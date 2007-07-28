/**
 * Copyright (c) 2005-2007, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;

import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalRelayAgent extends Thread implements KoLConstants
{
	private static final CustomCombatThread CUSTOM_THREAD = new CustomCombatThread();
	static { CUSTOM_THREAD.start(); }

	char [] data = new char[ 8096 ];
	StringBuffer buffer = new StringBuffer();

	private static final Pattern INVENTORY_COOKIE_PATTERN = Pattern.compile( "inventory=(\\d+)" );

	private Socket socket = null;
	private BufferedReader reader;
	private PrintStream writer;

	private String path;
	private boolean isCheckingModified;
	private LocalRelayRequest request;

	public LocalRelayAgent( int id )
	{	this.request = new LocalRelayRequest();
	}

	boolean isWaiting()
	{	return this.socket == null;
	}

	void setSocket( Socket socket )
	{
		this.socket = socket;

		synchronized ( this )
		{	this.notify();
		}
	}

	public void run()
	{
		while ( true )
		{
			if ( this.socket == null )
			{
				// Wait indefinitely for a client.  Exception
				// handling is probably not the best way to
				// handle this, but for now, it will do.

				try
				{
					synchronized ( this )
					{	this.wait();
					}
				}
				catch ( InterruptedException e )
				{
					// We expect this to happen only when we are
					// interrupted.  Fall through.
				}
			}

			if ( this.socket != null )
				this.performRelay();

			this.socket = null;
		}
	}

	public void performRelay()
	{
		if ( this.socket == null )
			return;

		this.path = null;
		this.reader = null;
		this.writer = null;

		try
		{
			this.reader = new BufferedReader( new InputStreamReader( this.socket.getInputStream() ) );

			this.readBrowserRequest();
			this.readServerResponse();

			if ( this.request.rawByteBuffer != null )
			{
				this.writer = new PrintStream( this.socket.getOutputStream(), false );
				this.writer.println( this.request.statusLine );
				this.request.printHeaders( this.writer );

				this.writer.println();
				this.writer.write( this.request.rawByteBuffer );
				this.writer.flush();
			}
		}
		catch ( Exception e )
		{
		}

		this.closeRelay();
	}

	public void readBrowserRequest() throws Exception
	{
		String requestLine = reader.readLine();
		int spaceIndex = requestLine.indexOf( " " );

		this.path = requestLine.substring( spaceIndex, requestLine.lastIndexOf( " " ) ).trim();
		this.request.constructURLString( this.path );

		String currentLine;

		if ( requestLine.startsWith( "GET" ) )
		{
			while ( (currentLine = reader.readLine()) != null && !currentLine.equals( "" ) )
			{
				if ( currentLine.startsWith( "If-Modified-Since" ) )
					this.isCheckingModified = true;

				if ( currentLine.startsWith( "Cookie" ) && this.path.startsWith( "/inventory.php" ) )
				{
					Matcher inventoryMatcher = INVENTORY_COOKIE_PATTERN.matcher( currentLine );
					if ( inventoryMatcher.find() )
						StaticEntity.setProperty( "visibleBrowserInventory", "inventory=" + inventoryMatcher.group(1) );
				}
			}
		}
		else
		{
			int contentLength = 0;

			while ( (currentLine = reader.readLine()) != null && !currentLine.equals( "" ) )
				if ( currentLine.startsWith( "Content-Length" ) )
					contentLength = StaticEntity.parseInt( currentLine.substring( 16 ) );

			int current;
			int remaining = contentLength;

			while ( remaining > 0 )
			{
				current = reader.read( data );
				buffer.append( data, 0, current );
				remaining -= current;
			}

			this.request.addEncodedFormFields( buffer.toString() );
			buffer.setLength(0);
		}
	}

	private void readServerResponse() throws Exception
	{
		// If not requesting a server-side page, then it is safe
		// to assume that no changes have been made (save time).

		if ( this.isCheckingModified && !this.request.contentType.equals( "text/html" ) )
		{
			this.request.pseudoResponse( "HTTP/1.1 304 Not Modified", "" );
			this.request.responseCode = 304;
		}
		else if ( this.path.equals( "/fight.php?action=script" ) )
		{
			if ( !FightRequest.isTrackingFights() )
				CUSTOM_THREAD.wake();

			String fightResponse = FightRequest.getNextTrackedRound();

			if ( fightResponse == null )
			{
				this.request.pseudoResponse( "HTTP/1.1 200 OK", "Timeout." );
			}
			else
			{
				if ( FightRequest.isTrackingFights() )
				{
					this.request.pseudoResponse(  "HTTP/1.1 200 OK", StaticEntity.singleStringDelete( fightResponse, "top.charpane.location.href=\"charpane.php\";" ) );
					this.request.headers.add( "Refresh: 0" );
				}
				else
					this.request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
			}
		}
		else if ( this.path.startsWith( "/tiles.php" ) )
		{
			AdventureRequest.handleDvoraksRevenge( this.request );
		}
		else
		{
			this.request.run();

			if ( this.path.startsWith( "/valhalla.php" ) && this.request.responseCode == 302 )
			{
				StaticEntity.getClient().handleAscension();

				this.request.constructURLString( this.request.redirectLocation );
				this.request.constructURLString( "mtnoob.php?action=toot" ).run();

				this.request.responseText = StaticEntity.singleStringReplace( this.request.responseText, "</html>",
					"<script language=\"Javascript\"><!-- menupane.document.location.reload(); charpane.document.location.reload(); --></script></html>" );
			}
		}

		if ( this.request.rawByteBuffer == null && this.request.responseText != null )
			this.request.rawByteBuffer = this.request.responseText.getBytes();
	}

	private void closeRelay()
	{
		try
		{
			if ( reader != null )
				reader.close();
		}
		catch ( Exception e )
		{
			// The only time this happens is if the
			// print bstream is already closed.  Ignore.
		}

		try
		{
			if ( writer != null )
				writer.close();
		}
		catch ( Exception e )
		{
			// The only time this happens is if the
			// print bstream is already closed.  Ignore.
		}

		try
		{
			if ( socket != null )
				socket.close();
		}
		catch ( Exception e )
		{
			// The only time this happens is if the
			// socket is already closed.  Ignore.
		}

	}

	private static class CustomCombatThread extends Thread
	{
		public CustomCombatThread()
		{	this.setDaemon( true );
		}

		public void wake()
		{
			FightRequest.beginTrackingFights();

			if ( !StaticEntity.getProperty( "battleAction" ).startsWith( "custom" ) )
				DEFAULT_SHELL.executeCommand( "set", "battleAction=custom" );

			synchronized ( this )
			{	this.notify();
			}
		}

		public void run()
		{
			while ( true )
			{
				try
				{
					synchronized ( this )
					{	this.wait();
					}
				}
				catch ( InterruptedException e )
				{
					// We expect this to happen only when we are
					// interrupted.  Fall through.
				}

				FightRequest.INSTANCE.run();
			}
		}
	}
}
