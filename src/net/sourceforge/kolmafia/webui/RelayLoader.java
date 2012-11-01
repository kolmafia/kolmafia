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

import com.centerkey.BareBonesBrowserLaunch;

import java.io.File;
import java.io.IOException;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.PauseObject;

public class RelayLoader
	extends Thread
{
	public static String currentBrowser = null;

	private final String location;

	protected RelayLoader( final String location, final boolean isRelayLocation )
	{
		super( "RelayLoader@" + location );

		if ( isRelayLocation )
		{
			StringBuffer locationBuffer = new StringBuffer();

			if ( !location.startsWith( "/" ) )
			{
				locationBuffer.append( "/" );
			}

			if ( location.endsWith( "main.php" ) )
			{
				locationBuffer.append( "game.php" );
			}
			else
			{
				locationBuffer.append( location );
			}

			this.location = locationBuffer.toString();
		}
		else
		{
			this.location = location;
		}
	}

	@Override
	public void run()
	{
		String preferredBrowser = Preferences.getString( "preferredWebBrowser" );

		String location = this.location;

		if ( location.startsWith( "/" ) )
		{
			RelayLoader.startRelayServer();

			// Wait for 5 seconds before giving up
			// on the relay server.

			PauseObject pauser = new PauseObject();

			for ( int i = 0; i < 50 && !RelayServer.isRunning(); ++i )
			{
				pauser.pause( 200 );
			}

			location = "http://127.0.0.1:" + RelayServer.getPort() + this.location;
		}

		BareBonesBrowserLaunch.openURL( preferredBrowser, location );
	}

	public static final synchronized void startRelayServer()
	{
		if ( RelayServer.isRunning() )
		{
			return;
		}

		RelayServer.startThread();
	}

	public static final void openRelayBrowser()
	{
		KoLmafia.forceContinue();
		openSystemBrowser( "game.php", true );
	}

	public static final void openSystemBrowser( final File file )
	{
		try
		{
			String location = file.getCanonicalPath();

			RelayLoader.openSystemBrowser( location, false );
		}
		catch ( IOException e )
		{
		}
	}

	public static final void openSystemBrowser( final String location )
	{
		boolean isRelayLocation = !location.startsWith( "http://" ) && !location.startsWith( "https://" );

		RelayLoader.openSystemBrowser( location, isRelayLocation );
	}

	public static final void openSystemBrowser( final String location, boolean isRelayLocation )
	{
		new RelayLoader( location, isRelayLocation ).start();
	}

}
