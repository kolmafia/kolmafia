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

package net.sourceforge.kolmafia.session;

import java.io.File;
import java.io.PrintStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class OceanManager
{
	private static final GenericRequest OCEAN_HANDLER = new GenericRequest( "ocean.php" );
	private static final Pattern OCEAN_PATTERN = Pattern.compile( "(\\d+),(\\d+)" );

	public static final void processOceanAdventure()
	{
		OceanManager.processOceanAdventure( OceanManager.OCEAN_HANDLER );
	}

	public static final void processOceanAdventure( final GenericRequest request )
	{
		String dest = Preferences.getString( "oceanDestination" );
		if ( dest.equals( "manual" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "Pick a course." );
			request.showInBrowser( true );
			return;
		}

		int lon = 0;
		int lat = 0;

		if ( dest.equals( "muscle" ) )
		{
			lon = 12;
			lat = 84;
		}
		else if ( dest.equals( "mysticality" ) )
		{
			lon = 3;
			lat = 35;
		}
		else if ( dest.equals( "moxie" ) )
		{
			lon = 13;
			lat = 91;
		}
		else if ( dest.equals( "sphere" ) )
		{
			lon = 59;
			lat = 10;
		}
		else if ( dest.equals( "plinth" ) )
		{
			lon = 63;
			lat = 29;
		}
		else if ( dest.indexOf( "," ) != -1 )
		{
			Matcher matcher = OceanManager.OCEAN_PATTERN.matcher( dest );
			if ( matcher.find() )
			{
				lon = StringUtilities.parseInt( matcher.group( 1 ) );
				lat = StringUtilities.parseInt( matcher.group( 2 ) );
			}
		}

		String action = Preferences.getString( "oceanAction" );
		boolean stop = action.equals( "stop" ) || action.equals( "savestop" );
		boolean show = action.equals( "show" ) || action.equals( "saveshow" );
		boolean save = action.equals( "savecontinue" ) || action.equals( "saveshow" ) || action.equals( "savestop" );

		while ( true )
		{
			if ( lon < 1 || lon > 242 || lat < 1 || lat > 100 )
			{
				// Pick a random destination
				lon = KoLConstants.RNG.nextInt( 242 ) + 1;
				lat = KoLConstants.RNG.nextInt( 100 ) + 1;
			}

			String coords = "Coordinates: " + lon + ", " + lat;
			RequestLogger.printLine( coords );
			RequestLogger.updateSessionLog( coords );

			// ocean.php?lon=10&lat=10
			request.constructURLString( "ocean.php" );
			request.clearDataFields();
			request.addFormField( "lon", String.valueOf( lon ) );
			request.addFormField( "lat", String.valueOf( lat ) );

			request.run();

			if ( save )
			{
				// Save the response Text
				File output = new File( KoLConstants.DATA_LOCATION, "ocean.html" );
				PrintStream writer = LogStream.openStream( output, false );

				// Trim to contain only HTML body
				int start = request.responseText.indexOf( "<body>" );
				int end = request.responseText.indexOf( "</body>" );
				String text = request.responseText.substring( start + 6, end );
				writer.println( text );
				writer.close();
			}

			if ( stop )
			{
				// Show result in browser and stop automation
				KoLmafia.updateDisplay( MafiaState.ABORT, "Stop" );
				request.showInBrowser( true );
				return;
			}

			if ( show )
			{
				// Show the response in the browser
				request.showInBrowser( true );
			}

			// And continue

			// The navigator says "Sorry, Cap'm, but we can't sail
			// to those coordinates, because that's where the
			// mainland is, and we've pretty much plundered the
			// mainland dry. Perhaps a more exotic locale is in
			// order?"

			if ( request.responseText.indexOf( "that's where the mainland is" ) == -1 )
			{
				return;
			}

			// Pick a different random destination
			lon = lat = 0;
		}
	}
}
