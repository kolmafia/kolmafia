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
import java.io.IOException;
import java.io.PrintStream;

import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalRelayAgent extends Thread
{
	private static final CustomCombatThread CUSTOM_THREAD = new CustomCombatThread();
	static { CUSTOM_THREAD.start(); }

	private static final Pattern INVENTORY_COOKIE_PATTERN = Pattern.compile( "inventory=(\\d+)" );

	private Socket socket = null;

	private BufferedReader reader;
	private PrintStream writer;

	private String path;
	private boolean isCheckingModified;
	private LocalRelayRequest request;

	public LocalRelayAgent( int id )
	{
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

	public void sendHeaders( PrintStream printStream, LocalRelayRequest request ) throws IOException
	{
		String header = null;
		String lowercase = null;

		if ( request.contentType == null )
			request.contentType = "text/html";

		for ( int i = 0; (header = request.getHeader( i )) != null; ++i )
		{
			lowercase = header.toLowerCase();

			if ( lowercase.startsWith( "content-type" ) )
				continue;

			if ( lowercase.startsWith( "content-length" ) )
				continue;

			if ( lowercase.startsWith( "cache-control" ) )
				continue;

			if ( lowercase.startsWith( "pragma" ) )
				continue;

			if ( lowercase.startsWith( "connection" ) )
				continue;

			if ( lowercase.startsWith( "transfer-encoding" ) )
				continue;

			if ( lowercase.startsWith( "set-cookie" ) )
				continue;

			if ( lowercase.startsWith( "http-referer" ) )
				continue;

			if ( !lowercase.equals( "" ) )
				printStream.println( header );
		}

		if ( request.responseCode == 200 )
		{
			if ( request.contentType.equals( "text/html" ) )
				printStream.println( "Content-Type: text/html; charset=UTF-8" );
			else
				printStream.println( "Content-Type: " + request.contentType );

			printStream.println( "Content-Length: " + request.rawByteBuffer.length );

			if ( request.formURLString.indexOf( ".php" ) != -1 )
			{
				printStream.println( "Cache-Control: no-cache, must-revalidate" );
				printStream.println( "Pragma: no-cache" );
			}
		}

		printStream.println( "Connection: close" );
	}

	public void performRelay()
	{
		if ( this.socket == null )
			return;

		this.path = null;
		this.reader = null;
		this.writer = null;
		this.request = null;

		try
		{
			this.reader = KoLDatabase.getReader( this.socket.getInputStream() );

			this.readBrowserRequest();
			this.readServerResponse();

			this.writer = new PrintStream( this.socket.getOutputStream(), true, "UTF-8" );

			if ( this.request.rawByteBuffer != null )
			{
				this.writer.println( this.request.statusLine );
				this.sendHeaders( this.writer, this.request );

				this.writer.println();
				this.writer.write( this.request.rawByteBuffer );
			}
		}
		catch ( Exception e )
		{
		}

		this.closeRelay( this.socket, this.reader, this.writer );
	}

	public void readBrowserRequest() throws Exception
	{
		String line = null;
		String method = "GET";
		int contentLength = 0;

		if ( (line = this.reader.readLine()) == null )
			return;

		int spaceIndex = line.indexOf( " " );
		if ( spaceIndex == -1 )
			return;

		method = line.trim().substring( 0, spaceIndex );
		int lastSpaceIndex = line.lastIndexOf( " " );

		this.path = line.substring( spaceIndex, lastSpaceIndex ).trim();
		this.request = new LocalRelayRequest( this.path );

		this.isCheckingModified = false;

		int colonIndex = 0;
		String [] tokens = new String[2];

		while ( (line = this.reader.readLine()) != null && line.trim().length() != 0 )
		{
			colonIndex = line.indexOf( ": " );
			if ( colonIndex == -1 )
				continue;

			tokens[0] = line.substring( 0, colonIndex );
			tokens[1] = line.substring( colonIndex + 2 );

			if ( tokens[0].equals( "Content-Length" ) )
				contentLength = StaticEntity.parseInt( tokens[1].trim() );

			if ( tokens[0].equals( "Cookie" ) )
			{
				// Let's find out what kind of cookie the browser is trying
				// to tell KoLmafia about.

				Matcher inventoryMatcher = INVENTORY_COOKIE_PATTERN.matcher( tokens[1] );
				if ( inventoryMatcher.find() )
					StaticEntity.setProperty( "visibleBrowserInventory", "inventory=" + inventoryMatcher.group(1) );
			}

			if ( tokens[0].equals( "If-Modified-Since" ) )
				this.isCheckingModified = true;
		}

		if ( method.equals( "POST" ) && contentLength > 0 )
		{
			char [] data = new char[ contentLength ];
			this.reader.read( data, 0, contentLength );

			this.request.addEncodedFormFields( new String( data ) );
		}
	}

	private void readServerResponse() throws Exception
	{
		// If not requesting a server-side page, then it is safe
		// to assume that no changes have been made (save time).

		if ( this.isCheckingModified && !this.request.contentType.startsWith( "text" ) )
		{
			this.request.pseudoResponse( "HTTP/1.1 304 Not Modified", "" );
			this.request.responseCode = 304;
		}
		else if ( this.path.indexOf( "fight.php" ) != -1 && (FightRequest.isTrackingFights() || this.path.indexOf( "action=script" ) != -1) )
		{
			if ( !FightRequest.isTrackingFights() )
			{
				StaticEntity.setProperty( "battleAction", "custom combat script" );

				FightRequest.beginTrackingFights();
				CUSTOM_THREAD.wake();
			}

			String fightResponse = FightRequest.getNextTrackedRound();

			if ( fightResponse == null )
			{
				this.request.pseudoResponse( "HTTP/1.1 404 Not Found", "" );
			}
			else
			{
				if ( FightRequest.isTrackingFights() )
				{
					fightResponse = StaticEntity.singleStringDelete( fightResponse, "top.charpane.location.href=\"charpane.php\";" );
					fightResponse = StaticEntity.singleStringDelete( fightResponse, "src=\"http://images.kingdomofloathing.com/scripts/window.js\"" );
					fightResponse = StaticEntity.singleStringDelete( fightResponse, "src=\"http://images.kingdomofloathing.com/scripts/core.js\"" );

					fightResponse = StaticEntity.singleStringReplace( fightResponse, "</html>",
						"<script language=\"Javascript\"> function continueAutomatedFight() { document.location.reload(); return 0; } setTimeout( continueAutomatedFight, 400 ); </script></html>" );
				}

				this.request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
			}
		}
		else if ( this.path.indexOf( "charpane.php" ) != -1 )
		{
			if ( FightRequest.getActualRound() == 0 )
			{
				StaticEntity.getClient().runBetweenBattleChecks( false, StaticEntity.getBooleanProperty( "relayMaintainsMoods" ),
					StaticEntity.getBooleanProperty( "relayMaintainsHealth" ), StaticEntity.getBooleanProperty( "relayMaintainsMana" ) );
			}

			this.request.run();
		}
		else if ( this.path.indexOf( "tiles.php" ) != -1 )
		{
			AdventureRequest.handleDvoraksRevenge( this.request );
		}
		else
		{
			this.request.run();

			if ( this.path.indexOf( "valhalla.php" ) != -1 && this.request.responseCode == 302 )
			{
				StaticEntity.getClient().handleAscension();

				this.request.constructURLString( this.request.redirectLocation );
				this.request.constructURLString( "mtnoob.php?action=toot" ).run();

				this.request.responseText = StaticEntity.singleStringReplace( this.request.responseText, "</html>",
					"<script language=\"Javascript\"><!-- menupane.document.location.reload(); charpane.document.location.reload(); --></script></html>" );
			}
		}

		if ( this.request.rawByteBuffer == null && this.request.responseText != null )
			this.request.rawByteBuffer = this.request.responseText.getBytes( "UTF-8" );
	}

	private void closeRelay( Socket socket, BufferedReader reader, PrintStream writer )
	{
		try
		{
			if ( reader != null )
				reader.close();
		}
		catch ( Exception e )
		{
			// The only time this happens is if the
			// print stream is already closed.  Ignore.
		}

		try
		{
			if ( writer != null )
				writer.close();
		}
		catch ( Exception e )
		{
			// The only time this happens is if the
			// print stream is already closed.  Ignore.
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
