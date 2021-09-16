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

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WildfireCampRequest
	extends PlaceRequest
{
	// 5: Inferno, 4: Raging, 3: Burning, 2: Smouldering, 1: Smoking, 0: Clear
	private static final Map<KoLAdventure, Integer> FIRE_LEVEL = new HashMap<>();

	public WildfireCampRequest()
	{
		super( "wildfire_camp" );
	}

	public WildfireCampRequest( final String action )
	{
		super( "wildfire_camp", action );
	}

	@Override
	public void processResults()
	{
		WildfireCampRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );

		if ( action == null )
		{
			return;
		}

		switch ( action )
		{
		case "wildfire_rainbarrel":
			parseRainbarrel( responseText );
			return;
		case "wildfire_oldpump":
			parsePump( responseText );
			return;
		}
	}

	private static void parseRainbarrel( final String responseText )
	{
		Preferences.setBoolean( "_wildfireBarrelHarvested", true );

		if ( responseText.contains( "You collect" ) )
		{
			Preferences.setBoolean( "wildfireBarrelCaulked", responseText.contains( "You collect 150 water" ) );
		}
	}

	private static void parsePump( final String responseText )
	{
		if ( responseText.contains( "You collect" ) )
		{
			Preferences.setBoolean( "wildfirePumpGreased", responseText.contains( "You collect 50 water" ) );
		}
	}

	private static final Pattern CAPTAIN_ZONE = Pattern.compile( "<option.*?value=\"(\\d+)\">.*? \\(.*?: (\\d)\\)</option>" );
	public static void parseCaptain( final String responseText )
	{
		Matcher m = CAPTAIN_ZONE.matcher( responseText );

		FIRE_LEVEL.clear();

		while ( m.find() )
		{
			int locationId = StringUtilities.parseInt( m.group( 1 ) );
			int level = StringUtilities.parseInt( m.group( 2 ) );

			KoLAdventure location = AdventureDatabase.getAdventureByURL( "adventure.php?snarfblat=" + locationId );

			if ( location != null )
			{
				FIRE_LEVEL.put( location, level );
			}
		}
	}

	public static void refresh()
	{
		if ( KoLCharacter.inFirecore() )
		{
			RequestThread.postRequest( new WildfireCampRequest( "wildfire_captain" ) );
			RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1451&option=2" ) );
		}
	}

	public static int getFireLevel( final KoLAdventure location )
	{
		return FIRE_LEVEL.getOrDefault( location, 5 );
	}

	public static void reduceFireLevel( final KoLAdventure location )
	{
		if ( location == null )
		{
			return;
		}

		FIRE_LEVEL.put( location, Math.max( 0, getFireLevel( location ) - 1 ) );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "whichplace=wildfire_camp" ) )
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

		switch ( action )
		{
		case "wildfire_rainbarrel":
			message = "Harvesting the rain barrel";
			break;
		case "wildfire_oldpump":
			message = "[" + KoLAdventure.getAdventureCount() + "] Harvesting the water pump";
			break;
		case "wildfire_captain":
		case "wildfire_fracker":
		case "wildfire_cropster":
		case "wildfire_sprinklerjoe":
			break;
		default:
			return false;
		}

		if ( message != null )
		{
			RequestLogger.printLine();
			RequestLogger.printLine( message );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}
}
