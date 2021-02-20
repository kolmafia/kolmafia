/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.*;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScrapheapRequest
	extends PlaceRequest
{
	private static final Pattern CHRONOLITH_COST = Pattern.compile( "title=\"\\((\\d+) Energy\\)\"" );
	private static final Pattern ENERGY_GAIN = Pattern.compile( "You gain (\\d+) Energy." );
	private static final Pattern STATBOT_COST = Pattern.compile( "Current upgrade cost: <b>(\\d+) energy</b>" );
	private static final Pattern CONFIGURATION = Pattern.compile( "robot/(left|right|top|bottom|body)(\\d+).png\"" );
	private static final Pattern CPU_UPGRADE_INSTALLED = Pattern.compile( "value=\"([a-z_]+)\"[^\\(]+\\(already installed\\)" );

	public ScrapheapRequest()
	{
		super( "scrapheap" );
	}

	public ScrapheapRequest( final String action )
	{
		super( "scrapheap", action );
	}

	public static void refresh()
	{
		if ( KoLCharacter.inRobocore() )
		{
			RequestThread.postRequest( new ScrapheapRequest() );
		}
	}

	@Override
	public void processResults()
	{
		ScrapheapRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );

		if ( action == null || action.startsWith( "sh_chrono" ) )
		{
			parseChronolith( responseText );
			return;
		}

		if ( action.startsWith( "sh_getpower" ) )
		{
			parseCollectEnergy( responseText );
		}
	}

	private static void parseChronolith( final String responseText )
	{
		Matcher m = CHRONOLITH_COST.matcher( responseText );

		if ( m.find() )
		{
			int cost = StringUtilities.parseInt( m.group( 1 ) );
			Preferences.setInteger( "_chronolithActivations", cost - 10 );
		}
	}

	private static void parseCollectEnergy( final String responseText )
	{
		Matcher m = ENERGY_GAIN.matcher( responseText );

		if ( m.find() )
		{
			// Progression went 25, 21, 18, 13, 11, 10, 8, 6 ,6 ,5?
			// int gain = StringUtilities.parseInt( m.group( 1 ) );
			Preferences.increment( "_energyCollected" );
		}
	}

	public static void parseStatbotCost( final String responseText )
	{
		Matcher m = STATBOT_COST.matcher( responseText );

		if ( m.find() )
		{
			int cost = StringUtilities.parseInt( m.group( 1 ) );
			Preferences.setInteger( "statbotUses", cost - 10 );
		}
	}

	public static void parseConfiguration( final String text )
	{
		Matcher m = CONFIGURATION.matcher( text );

		while ( m.find() )
		{
			String section = m.group( 1 );
			int config = StringUtilities.parseInt( m.group( 2 ) );
			Preferences.setInteger( "youRobot" + StringUtilities.toTitleCase( section ), config );
		}
	}

	public static void parseCPUUpgrades( final String text )
	{
		ArrayList<String> cpuUpgrades = new ArrayList<>();
		Matcher m = CPU_UPGRADE_INSTALLED.matcher( text );

		while ( m.find() )
		{
			cpuUpgrades.add( m.group( 1 ) );
		}

		Preferences.setString( "youRobotCPUUpgrades", String.join( ",", cpuUpgrades ) );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "whichplace=scrapheap" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			// Nothing to log for simple visits
			return true;
		}

		String message = null;

		if ( action.startsWith( "sh_chrono" ) )
		{
			message = "Activating the Chronolith";
		}
		if ( action.startsWith( "sh_upgrade" ) )
		{
			return true;
		}
		if ( action.startsWith( "sh_getpower" ) )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Collecting energy";
		}
		if ( action.startsWith( "sh_scrounge" ) )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Scavenging scrap";
		}
		else if ( action.startsWith( "sh_configure" ) )
		{
			return true;
		}

		if ( message == null )
		{
			// Log URL for anything else
			return false;
		}

		RequestLogger.printLine();
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
