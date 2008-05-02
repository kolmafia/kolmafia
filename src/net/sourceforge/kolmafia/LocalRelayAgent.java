/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.TreeMap;

import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.session.ActionBarManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LeafletManager;
import net.sourceforge.kolmafia.session.ValhallaManager;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LocalRelayAgent
	extends Thread
{
	private static final LocalRelayCombatThread COMBAT_THREAD = new LocalRelayCombatThread();
	private static final TreeMap lastModified = new TreeMap();

	public static void reset()
	{
		LocalRelayAgent.lastModified.clear();
	}

	private final char[] data = new char[ 8096 ];
	private final StringBuffer buffer = new StringBuffer();
	private final PauseObject pauser = new PauseObject();

	private Socket socket = null;
	private BufferedReader reader;
	private PrintStream writer;

	private String path;
	private boolean isCheckingModified;
	private final RelayRequest request;

	public LocalRelayAgent( final int id )
	{
		this.request = new RelayRequest( true );
	}

	public boolean isWaiting()
	{
		return this.socket == null;
	}

	public void setSocket( final Socket socket )
	{
		this.socket = socket;
		this.pauser.unpause();
	}

	public void run()
	{
		while ( true )
		{
			if ( this.socket == null )
			{
				this.pauser.pause();
			}

			this.performRelay();
			this.closeRelay();
		}
	}

	public void performRelay()
	{
		if ( this.socket == null )
		{
			return;
		}

		this.path = null;
		this.reader = null;
		this.writer = null;

		try
		{
			if ( !this.readBrowserRequest() )
			{
				return;
			}

			this.readServerResponse();
			this.sendServerResponse();
		}
		catch ( IOException e )
		{
		}
	}

	public boolean readBrowserRequest()
		throws IOException
	{
		this.reader = new BufferedReader( new InputStreamReader( this.socket.getInputStream() ) );

		String requestLine = this.reader.readLine();
		
		if ( requestLine == null )
		{
			return false;
		}
		
		int spaceIndex = requestLine.indexOf( " " );

		this.path = requestLine.substring( spaceIndex, requestLine.lastIndexOf( " " ) ).trim();
		this.request.constructURLString( this.path );

		String currentLine;
		int contentLength = 0;

		while ( ( currentLine = this.reader.readLine() ) != null && !currentLine.equals( "" ) )
		{
			if ( currentLine.startsWith( "Referer: " ) )
			{
				String path = currentLine.substring( 9 );
				if ( !path.equals( "" ) && !path.startsWith( "http://127.0.0.1" ) )
				{
					RequestLogger.printLine( "Request from bogus referer ignored: " + path );
					return false;
				}
			}

			if ( currentLine.startsWith( "If-Modified-Since" ) )
			{
				this.isCheckingModified = true;
			}

			if ( currentLine.startsWith( "Content-Length" ) )
			{
				contentLength = StringUtilities.parseInt( currentLine.substring( 16 ) );
			}

			if ( currentLine.startsWith( "Cookie" ) && this.path.startsWith( "/inventory" ) )
			{
				String[] cookieList = currentLine.substring( 8 ).split( "\\s*;\\s*" );
				for ( int i = 0; i < cookieList.length; ++i )
				{
					if ( cookieList[ i ].startsWith( "inventory" ) )
					{
						GenericRequest.inventoryCookie = cookieList[ i ];
					}
				}
			}
		}

		if ( requestLine.startsWith( "POST" ) )
		{
			int current;
			int remaining = contentLength;

			while ( remaining > 0 )
			{
				current = this.reader.read( this.data );
				this.buffer.append( this.data, 0, current );
				remaining -= current;
			}

			this.request.addEncodedFormFields( this.buffer.toString() );
			this.buffer.setLength( 0 );
		}

		// Validate supplied password hashes
		String pwd = this.request.getFormField( "pwd" );

		// Other KoL pages might also use "phash"
		if ( pwd == null )
			pwd = this.request.getFormField( "phash" );

		// KoLmafia internal pages use only "pwd"
		if ( this.path.startsWith( "/KoLmafia" ) )
			return pwd != null;

		// All other pages need either no password hash
		// or a valid password hash.
		return pwd == null || pwd.equals( GenericRequest.passwordHash );
	}

	private boolean shouldSendNotModified()
	{
		if ( this.path.startsWith( "/images" ) )
		{
			return true;
		}

		if ( this.path.indexOf( "?" ) == -1 )
		{
			return false;
		}

		if ( !this.path.endsWith( ".js" ) && !this.path.endsWith( ".html" ) )
		{
			return false;
		}

		if ( LocalRelayAgent.lastModified.containsKey( this.path ) )
		{
			return true;
		}

		LocalRelayAgent.lastModified.put( this.path, Boolean.TRUE );
		return false;
	}

	private void readServerResponse()
		throws IOException
	{
		// If not requesting a server-side page, then it is safe
		// to assume that no changes have been made (save time).

		if ( this.isCheckingModified && this.shouldSendNotModified() )
		{
			this.request.pseudoResponse( "HTTP/1.1 304 Not Modified", "" );
			this.request.responseCode = 304;
			this.request.rawByteBuffer = this.request.responseText.getBytes( "UTF-8" );
		}

		if ( this.path.startsWith( "/charpane.php" ) )
		{
			int initialCount = KoLCharacter.getAdventuresLeft();
			this.request.run();

			if ( initialCount != KoLCharacter.getAdventuresLeft() && !KoLmafia.isRunningBetweenBattleChecks() && FightRequest.getCurrentRound() == 0 )
			{
				StaticEntity.getClient().runBetweenBattleChecks(
					false, Preferences.getBoolean( "relayMaintainsEffects" ),
					Preferences.getBoolean( "relayMaintainsHealth" ),
					Preferences.getBoolean( "relayMaintainsMana" ) );

				this.request.run();
			}
		}
		else if ( this.path.equals( "/fight.php?action=custom" ) )
		{
			LocalRelayAgent.COMBAT_THREAD.wake( null );
			this.request.pseudoResponse( "HTTP/1.1 302 Found", "/fight.php?action=script" );
		}
		else if ( this.path.equals( "/fight.php?action=script" ) )
		{
			String fightResponse = FightRequest.getNextTrackedRound();

			if ( FightRequest.isTrackingFights() )
			{
				fightResponse = KoLConstants.SCRIPT_PATTERN.matcher( fightResponse ).replaceAll( "" );
				this.request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
				this.request.headers.add( "Refresh: 1" );
			}
			else
			{
				this.request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
			}
		}
		else if ( this.path.equals( "/fight.php?action=abort" ) )
		{
			FightRequest.stopTrackingFights();
			this.request.pseudoResponse( "HTTP/1.1 200 OK", FightRequest.getNextTrackedRound() );
		}
		else if ( this.path.startsWith( "/fight.php?hotkey=" ) )
		{
			LocalRelayAgent.COMBAT_THREAD.wake( Preferences.getString( "combatHotkey" + this.request.getFormField( "hotkey" ) ) );
			this.request.pseudoResponse( "HTTP/1.1 302 Found", "/fight.php?action=script" );
		}
		else if ( this.path.equals( "/choice.php?action=auto" ) )
		{
			ChoiceManager.processChoiceAdventure( this.request );
		}
		else if ( this.path.equals( "/leaflet.php?action=auto" ) )
		{
			this.request.pseudoResponse( "HTTP/1.1 200 OK",
				LeafletManager.leafletWithMagic() );
		}
		else if ( this.path.startsWith( "/sidepane.php" ) )
		{
			this.request.pseudoResponse( "HTTP/1.1 200 OK", RequestEditorKit.getFeatureRichHTML(
				"charpane.php", CharPaneRequest.getLastResponse(), true ) );
		}
		else if ( this.path.startsWith( "/actionbar.php" ) )
		{
			ActionBarManager.updateJSONString( this.request );
		}
		else
		{
			this.request.run();

			if ( this.path.startsWith( "/valhalla.php" ) && this.request.responseCode == 302 )
			{
				if ( this.path.indexOf( "asctype=1" ) != -1 )
				{
					KoLmafia.resetCounters();
				}
				else
				{
					ValhallaManager.postAscension();
				}
			}
			else if ( this.path.endsWith( "noobmessage=true" ) )
			{
				if ( KoLCharacter.isHardcore() && Preferences.getBoolean( "lucreCoreLeaderboard" ) )
				{
					( new Thread( new SendMailRequest( "koldbot", "Started ascension." ) ) ).start();
				}
			}
		}

		if ( this.request.rawByteBuffer == null && this.request.responseText != null )
		{
			this.request.rawByteBuffer = this.request.responseText.getBytes( "UTF-8" );
		}
	}

	private void sendServerResponse()
		throws IOException
	{
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

	private void closeRelay()
	{
		try
		{
			if ( this.reader != null )
			{
				this.reader.close();
				this.reader = null;
			}
		}
		catch ( IOException e )
		{
			// The only time this happens is if the
			// input is already closed.  Ignore.
		}

		if ( this.writer != null )
		{
			this.writer.close();
			this.writer = null;
		}

		try
		{
			if ( this.socket != null )
			{
				this.socket.close();
				this.socket = null;
			}
		}
		catch ( IOException e )
		{
			// The only time this happens is if the
			// socket is already closed.  Ignore.
		}
	}
}
