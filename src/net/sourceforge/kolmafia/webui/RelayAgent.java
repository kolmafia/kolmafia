/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.webui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.request.RelayRequest;

import net.sourceforge.kolmafia.session.ActionBarManager;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.LeafletManager;
import net.sourceforge.kolmafia.session.ValhallaManager;

import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RelayAgent
	extends Thread
{
	private static final RelayAutoCombatThread COMBAT_THREAD = new RelayAutoCombatThread();
	private static final Map lastModified = new HashMap();

	private static GenericRequest errorRequest = null;
	private static String errorRequestPath = null;

	public static void reset()
	{
		RelayAgent.lastModified.clear();
	}

	public static void setErrorRequest( GenericRequest errorRequest )
	{
		RelayAgent.errorRequest = errorRequest;
		RelayAgent.errorRequestPath = "/" + errorRequest.getPath();
	}

	private final char[] data = new char[ 8192 ];
	private final StringBuffer buffer = new StringBuffer();
	private final PauseObject pauser = new PauseObject();

	private Socket socket = null;
	private BufferedReader reader;
	private PrintStream writer;

	private String path;
	private String requestMethod;
	private boolean isCheckingModified;
	private final RelayRequest request;

	public RelayAgent( final int id )
	{
		super( "LocalRelayAgent" + id );
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

			try
			{
				this.performRelay();
			}
			finally
			{
				this.closeRelay();
			}
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
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Horrible relay failure" );
		}
	}

	public boolean readBrowserRequest()
		throws IOException
	{
		boolean debugging = RequestLogger.isDebugging() && Preferences.getBoolean( "logBrowserInteractions" );

		this.reader = new BufferedReader( new InputStreamReader( this.socket.getInputStream() ) );

		String requestLine = this.reader.readLine();

		if ( debugging )
		{
			RequestLogger.updateDebugLog( "-----From Browser-----" );
			RequestLogger.updateDebugLog( requestLine );
		}

		if ( requestLine == null )
		{
			return false;
		}

		int spaceIndex = requestLine.indexOf( " " );

		this.requestMethod = requestLine.substring( 0, spaceIndex );
		boolean usePostMethod = this.requestMethod.equals( "POST" );
		this.path = requestLine.substring( spaceIndex + 1, requestLine.lastIndexOf( " " ) );
		if ( this.path.startsWith( "//" ) )
		{
			// A current KoL bug causes URLs to gain an unnecessary
			// leading slash after certain chat right-click
			// commands are used.
			this.path = this.path.substring( 1 );
		}

		this.request.constructURLString( this.path, usePostMethod );

		String currentLine;
		int contentLength = 0;

		while ( ( currentLine = this.reader.readLine() ) != null && !currentLine.equals( "" ) )
		{
			if ( debugging )
			{
				RequestLogger.updateDebugLog( currentLine );
			}

			if ( currentLine.startsWith( "Referer: " ) )
			{
				String referer = currentLine.substring( 9 );
				if ( !referer.equals( "" ) )
				{
					boolean validHost = true;

					if ( referer.startsWith( "http://" ) )
					{
						int endHostIndex = referer.indexOf( ':', 7 );

						if ( endHostIndex == -1 )
						{
							endHostIndex = referer.indexOf( '/', 7 );
						}

						if ( endHostIndex == -1 )
						{
							validHost = false;
						}
						else
						{
							String refererHost = referer.substring( 7, endHostIndex );

							if ( validRefererHosts.contains( refererHost ) )
							{
								// Do nothing
							}
							else if ( invalidRefererHosts.contains( refererHost ) )
							{
								validHost = false;
							}
							else if ( InetAddress.getByName( refererHost ).isLoopbackAddress() )
							{
								validRefererHosts.add( refererHost );
							}
							else
							{
								invalidRefererHosts.add( refererHost );
								validHost = false;
							}
						}
					}
					else
					{
						validHost = false;
					}

					if ( !validHost )
					{
						RequestLogger.printLine( "Request from bogus referer ignored" );
						RequestLogger.printLine( "Path: \"" + path + "\"" );
						RequestLogger.printLine( "Referer: \"" + referer + "\"" );
						return false;
					}
				}

				continue;
			}

			if ( currentLine.startsWith( "If-Modified-Since" ) )
			{
				this.isCheckingModified = true;
				continue;
			}

			if ( currentLine.startsWith( "Content-Length" ) )
			{
				contentLength = StringUtilities.parseInt( currentLine.substring( 16 ) );
				continue;
			}

			if ( currentLine.startsWith( "User-Agent" ) )
			{
				GenericRequest.saveUserAgent( currentLine.substring( 12 ) );
				continue;
			}

			if ( currentLine.startsWith( "Cookie" ) )
			{
				if ( this.path.startsWith( "/inventory" ) )
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
				continue;
			}
		}

		if ( requestMethod.equals( "POST" ) )
		{
			int remaining = contentLength;

			while ( remaining > 0 )
			{
				int current = this.reader.read( this.data );
				this.buffer.append( this.data, 0, current );
				remaining -= current;
			}

			String fields = this.buffer.toString();
			this.buffer.setLength( 0 );

			if ( debugging )
			{
				RequestLogger.updateDebugLog( fields );
			}

			this.request.addFormFields( fields, true );
		}

		if ( debugging )
		{
			RequestLogger.updateDebugLog( "----------" );
		}

		// Validate supplied password hashes
		String pwd = this.request.getFormField( "pwd" );
		if ( pwd == null )
		{
			// KoLmafia internal pages use only "pwd"
			if ( this.path.startsWith( "/KoLmafia" ) )
			{
				RequestLogger.printLine( "Missing password hash" );
				RequestLogger.printLine( "Path: \"" + this.path + "\"" );
				return false;
			}
			pwd = this.request.getFormField( "phash" );
		}

		// All other pages need either no password hash
		// or a valid password hash.
		if ( pwd != null && !pwd.equals( GenericRequest.passwordHash ) )
		{
			RequestLogger.printLine( "Password hash mismatch" );
			RequestLogger.printLine( "Path: \"" + this.path + "\"" );
			return false;
		}

		return true;
	}

	private boolean shouldSendNotModified()
	{
		if ( this.path.startsWith( "/images" ) )
		{
			return true;
		}

		if ( this.path.indexOf( "?" ) != -1 )
		{
			return false;
		}

		if ( !this.path.endsWith( ".js" ) && !this.path.endsWith( ".html" ) )
		{
			return false;
		}

		if ( RelayAgent.lastModified.containsKey( this.path ) )
		{
			return true;
		}

		RelayAgent.lastModified.put( this.path, Boolean.TRUE );
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

			return;
		}

		if ( errorRequest != null )
		{
			if ( this.path.startsWith( "/main.php" ) )
			{
				this.request.pseudoResponse( "HTTP/1.1 302 Found", RelayAgent.errorRequestPath );
				return;
			}

			if ( this.path.equals( RelayAgent.errorRequestPath ) )
			{
				this.request.pseudoResponse( "HTTP/1.1 200 OK", errorRequest.responseText );
				this.request.formatResponse();

				RelayAgent.errorRequest = null;
				RelayAgent.errorRequestPath = null;

				return;
			}
		}

		if ( this.path.equals( "/fight.php?action=custom" ) )
		{
			RelayAgent.COMBAT_THREAD.wake( null );
			this.request.pseudoResponse( "HTTP/1.1 302 Found", "/fight.php?action=script" );
		}
		else if ( this.path.equals( "/fight.php?action=script" ) )
		{
			String fightResponse = FightRequest.getNextTrackedRound();
			if ( FightRequest.isTrackingFights() )
			{
				fightResponse = KoLConstants.SCRIPT_PATTERN.matcher( fightResponse ).replaceAll( "" );
				this.request.headers.add( "Refresh: 1" );
			}
			this.request.pseudoResponse( "HTTP/1.1 200 OK", fightResponse );
		}
		else if ( this.path.equals( "/fight.php?action=abort" ) )
		{
			FightRequest.stopTrackingFights();
			this.request.pseudoResponse( "HTTP/1.1 200 OK", FightRequest.getNextTrackedRound() );
		}
		else if ( this.path.startsWith( "/fight.php?hotkey=" ) )
		{
			String hotkey = this.request.getFormField( "hotkey" );

			if ( hotkey.equals( "11" ) )
			{
				RelayAgent.COMBAT_THREAD.wake( null );
			}
			else
			{
				RelayAgent.COMBAT_THREAD.wake( Preferences.getString( "combatHotkey" + hotkey ) );
			}

			this.request.pseudoResponse( "HTTP/1.1 302 Found", "/fight.php?action=script" );
		}
		else if ( this.path.equals( "/choice.php?action=auto" ) )
		{
			ChoiceManager.processChoiceAdventure( this.request, ChoiceManager.lastResponseText );
		}
		else if ( this.path.equals( "/leaflet.php?action=auto" ) )
		{
			this.request.pseudoResponse( "HTTP/1.1 200 OK",
				LeafletManager.leafletWithMagic() );
		}
		else if ( this.path.startsWith( "/loggedout.php" ) )
		{
			this.request.pseudoResponse( "HTTP/1.1 200 OK", LogoutRequest.getLastResponse() );
		}
		else if ( this.path.startsWith( "/actionbar.php" ) )
		{
			ActionBarManager.updateJSONString( this.request );
		}
		else
		{
			RequestThread.postRequest( this.request );

			if ( this.path.startsWith( "/afterlife.php" ) && this.request.responseCode == 302 )
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
		}
	}

	private void sendServerResponse()
		throws IOException
	{
		if ( this.request.rawByteBuffer == null )
		{
			if ( this.request.responseText == null )
			{
				return;
			}

			this.request.rawByteBuffer = this.request.responseText.getBytes( "UTF-8" );
		}

		this.writer = new PrintStream( this.socket.getOutputStream(), false );
		this.writer.println( this.request.statusLine );
		this.request.printHeaders( this.writer );
		this.writer.println();
		this.writer.write( this.request.rawByteBuffer );
		this.writer.flush();

		if ( !RequestLogger.isDebugging() )
		{
			return;
		}

		boolean interactions = Preferences.getBoolean( "logBrowserInteractions" );

		if ( interactions )
		{
			RequestLogger.updateDebugLog( "-----To Browser-----" );
			RequestLogger.updateDebugLog( this.request.statusLine );
			this.request.printHeaders( RequestLogger.getDebugStream() );
		}

		if ( Preferences.getBoolean( "logDecoratedResponses" ) )
		{
			String text = this.request.responseText;
			if ( !Preferences.getBoolean( "logReadableHTML" ) )
			{
				text = KoLConstants.LINE_BREAK_PATTERN.matcher( text ).replaceAll( "" );
			}
			RequestLogger.updateDebugLog( text );
		}

		if ( interactions )
		{
			RequestLogger.updateDebugLog( "----------" );
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

	private static Set validRefererHosts = new HashSet();
	private static Set invalidRefererHosts = new HashSet();
}
