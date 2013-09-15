/**
 * Copyright (c) 2005-2013, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *	  notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *	  notice, this list of conditions and the following disclaimer in
 *	  the documentation and/or other materials provided with the
 *	  distribution.
 *  [3] Neither the name "KoLmafia" nor the names of
 *	  its contributors may be used to endorse or promote products
 *	  derived from this software without specific prior written
 *	  permission.
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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.FileUtilities;

public class RelayServer
	implements Runnable
{
	public static final ArrayList<RelayAgent> agentThreads = new ArrayList<RelayAgent>();

	private static long lastStatusMessage = 0;
	private static Thread relayThread = null;

	private ServerSocket serverSocket = null;
	private static int port = 60080;
	private static boolean listening = false;
	private static boolean updateStatus = false;

	private static final RelayServer INSTANCE = new RelayServer();
	private static final StringBuffer statusMessages = new StringBuffer();

	private RelayServer()
	{
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "basics.css" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "basement.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "basics.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "chat.html" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "cli.html" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "combatfilter.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "hotkeys.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "onfocus.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "palinshelves.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "sorttable.js" );
		FileUtilities.loadLibrary( KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, "macrohelper.js" );

		Preferences.setString( "lastRelayUpdate", StaticEntity.getVersion() );
	}

	public static final void updateStatus()
	{
		RelayServer.updateStatus = true;
	}

	public static final void startThread()
	{
		if ( RelayServer.relayThread != null )
		{
			return;
		}

		RelayServer.relayThread = new Thread( RelayServer.INSTANCE,
			"LocalRelayServer" );
		RelayServer.relayThread.start();
	}

	public static final int getPort()
	{
		return RelayServer.port;
	}

	public static final boolean isRunning()
	{
		return RelayServer.listening;
	}

	public static final void stop()
	{
		RelayServer.listening = false;
	}

	public void run()
	{
		RelayServer.port = 60080;
		while ( !this.openServerSocket() )
		{
			if ( RelayServer.port <= 60089 )
			{
				++RelayServer.port;
			}
			else
			{
				KoLmafia.quit();
			}
		}

		RelayServer.listening = true;

		while ( RelayServer.listening )
		{
			try
			{
				this.dispatchAgent( this.serverSocket.accept() );
			}
			catch ( Exception e )
			{
				// If an exception occurs here, that means
				// someone closed the thread; just reset
				// the listening state and fall through.

				RelayServer.listening = false;
			}
		}

		this.closeAgents();

		try
		{
			if ( this.serverSocket != null )
			{
				this.serverSocket.close();
			}
		}
		catch ( Exception e )
		{
			// The end result of a socket closing
			// should not throw an exception, but
			// if it does, the socket closes.
		}

		this.serverSocket = null;
		RelayServer.relayThread = null;
	}

	private synchronized boolean openServerSocket()
	{
		try
		{
			if ( Preferences.getBoolean( "relayAllowRemoteAccess" ) )
			{
				this.serverSocket = new ServerSocket( RelayServer.port, 25 );
			}
			else
			{
				this.serverSocket = new ServerSocket( RelayServer.port, 25, InetAddress.getByName( "127.0.0.1" ) );
			}

			return true;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	private synchronized void closeAgents()
	{
		while ( !RelayServer.agentThreads.isEmpty() )
		{
			RelayAgent agent = (RelayAgent) RelayServer.agentThreads.remove( 0 );
			agent.setSocket( null );
		}
	}

	private synchronized void dispatchAgent( final Socket socket )
	{
		RelayAgent agent = null;

		for ( int i = 0; i < RelayServer.agentThreads.size(); ++i )
		{
			agent = (RelayAgent) RelayServer.agentThreads.get( i );

			if ( agent.isWaiting() )
			{
				agent.setSocket( socket );
				return;
			}
		}

		this.createAgent( socket );
	}

	private synchronized void createAgent( final Socket socket )
	{
		RelayAgent agent = new RelayAgent( RelayServer.agentThreads.size() );
		agent.setSocket( socket );

		RelayServer.agentThreads.add( agent );
		agent.start();
	}

	public static final void addStatusMessage( final String message )
	{
		if ( System.currentTimeMillis() - RelayServer.lastStatusMessage < 4000 )
		{
			RelayServer.statusMessages.append( message );
		}
	}

	public static final String getNewStatusMessages()
	{
		if ( RelayServer.updateStatus )
		{
			RelayServer.updateStatus = false;
			RelayServer.statusMessages.append( "<!-- REFRESH -->" );
		}

		String newMessages = RelayServer.statusMessages.toString();
		RelayServer.statusMessages.setLength( 0 );

		RelayServer.lastStatusMessage = System.currentTimeMillis();
		return newMessages;
	}
}
