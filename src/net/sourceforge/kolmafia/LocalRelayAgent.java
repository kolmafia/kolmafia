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
import java.io.PrintStream;

import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalRelayAgent extends Thread implements KoLConstants
{
	private static final CustomCombatThread CUSTOM_THREAD = new CustomCombatThread();
	static { CUSTOM_THREAD.start(); }

	private char [] data = new char[ 8096 ];
	private StringBuffer buffer = new StringBuffer();

	private Socket socket = null;
	private BufferedReader reader;
	private PrintStream writer;

	private String path;
	private boolean isCheckingModified;
	private LocalRelayRequest request;

	public LocalRelayAgent( int id )
	{	this.request = new LocalRelayRequest( true );
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

			if ( !this.readBrowserRequest() )
			{
				this.closeRelay();
				return;
			}

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

	public boolean readBrowserRequest() throws Exception
	{
		String requestLine = reader.readLine();
		int spaceIndex = requestLine.indexOf( " " );

		this.path = requestLine.substring( spaceIndex, requestLine.lastIndexOf( " " ) ).trim();
		this.request.constructURLString( this.path );

		String currentLine;
		boolean isValid = true;
		boolean hadReferrer = false;

		int contentLength = 0;

		while ( (currentLine = reader.readLine()) != null && !currentLine.equals( "" ) )
		{
			if ( currentLine.startsWith( "If-Modified-Since" ) )
				this.isCheckingModified = true;

			if ( currentLine.indexOf( "Referer" ) != -1 )
			{
				isValid = currentLine.indexOf( "http://127.0.0.1" ) != -1;
				hadReferrer = isValid;
			}

			if ( currentLine.startsWith( "Content-Length" ) )
				contentLength = StaticEntity.parseInt( currentLine.substring( 16 ) );

			if ( currentLine.startsWith( "Cookie" ) && this.path.startsWith( "/inventory" ) )
			{
				String [] cookieList = currentLine.substring(8).split( "\\s*;\\s*" );
				for ( int i = 0; i < cookieList.length; ++i )
				{
					if ( cookieList[i].startsWith( "inventory" ) )
						KoLRequest.inventoryCookie = cookieList[i];
				}
			}
		}


		if ( requestLine.startsWith( "GET" ) )
		{
			isValid = this.path.indexOf( "?" ) == -1 || hadReferrer || !this.path.startsWith( "/KoLmafia" );
		}
		else
		{
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

		return isValid;
	}

	private void readServerResponse() throws Exception
	{
		// If not requesting a server-side page, then it is safe
		// to assume that no changes have been made (save time).

		if ( this.isCheckingModified && request.getPath().startsWith( "images" ) )
		{
			this.request.pseudoResponse( "HTTP/1.1 304 Not Modified", "" );
			this.request.responseCode = 304;
		}

		if ( this.path.equals( "/fight.php?action=custom" ) )
		{
			if ( !FightRequest.isTrackingFights() )
				CUSTOM_THREAD.wake();

			this.request.pseudoResponse( "HTTP/1.1 302 Found", "/fight.php?action=script" );
		}
		else if ( this.path.equals( "/fight.php?action=script" ) )
		{
			String fightResponse = FightRequest.getNextTrackedRound();

			if ( FightRequest.isTrackingFights() )
			{
				fightResponse = SCRIPT_PATTERN.matcher( fightResponse ).replaceAll( "" );
				this.request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
				this.request.headers.add( "Refresh: 1" );
			}
			else
				this.request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
		}
		else if ( this.path.equals( "/fight.php?action=abort" ) )
		{
			FightRequest.stopTrackingFights();
			this.request.pseudoResponse( "HTTP/1.1 200 OK", FightRequest.getNextTrackedRound() );
		}
		else if ( this.path.equals( "/choice.php?action=auto" ) )
		{
			KoLRequest.processChoiceAdventure( this.request );
		}
		else if ( this.path.startsWith( "/tiles.php" ) )
		{
			AdventureRequest.handleDvoraksRevenge( this.request );
		}
		else if ( this.path.startsWith( "/charpane.php" ) )
		{
			int initialCount = KoLCharacter.getAdventuresLeft();
			this.request.run();

			if ( initialCount != KoLCharacter.getAdventuresLeft() && !KoLmafia.isRunningBetweenBattleChecks() && FightRequest.getCurrentRound() == 0 )
			{
				StaticEntity.getClient().runBetweenBattleChecks( false, KoLSettings.getBooleanProperty( "relayMaintainsEffects" ),
					KoLSettings.getBooleanProperty( "relayMaintainsHealth" ), KoLSettings.getBooleanProperty( "relayMaintainsMana" ) );

				this.request.run();
			}
		}
		else if ( this.path.startsWith( "/sidepane.php" ) )
		{
			this.request.pseudoResponse( "HTTP/1.1 200 OK", RequestEditorKit.getFeatureRichHTML( "charpane.php", CharpaneRequest.getLastResponse(), true ) );
		}
		else
		{
			if ( !this.request.hasNoResult() )
				while ( KoLmafia.isRunningBetweenBattleChecks() )
					KoLRequest.delay( 200 );

			this.request.run();

			if ( this.path.startsWith( "/valhalla.php" ) && this.request.responseCode == 302 )
			{
				if ( this.path.indexOf( "asctype=1" ) != -1 )
					KoLmafia.resetCounters();
				else
					StaticEntity.getClient().handleAscension();
			}
		}

		if ( this.request.rawByteBuffer == null && this.request.responseText != null )
			this.request.rawByteBuffer = this.request.responseText.getBytes( "UTF-8" );
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

	private static final class CustomCombatThread extends Thread
	{
		public CustomCombatThread()
		{	this.setDaemon( true );
		}

		public void wake()
		{
			FightRequest.beginTrackingFights();

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

				if ( !KoLSettings.getUserProperty( "battleAction" ).startsWith( "custom" ) )
					KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "set", "battleAction=custom" );

				FightRequest.INSTANCE.run();
			}
		}
	}
}
