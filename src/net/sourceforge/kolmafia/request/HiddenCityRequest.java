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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HiddenCityRequest
	extends GenericRequest
{
	private static final Pattern WHICH_PATTERN = Pattern.compile( "which=([\\d,]+)" );
	private static final Pattern ROUND_PATTERN = Pattern.compile( "whichitem=([\\d,]+)" );

	private static int lastSquare = 0;

	private int square = 0;
	private String action;
	private int itemId = 0;

	public HiddenCityRequest()
	{
		super( "hiddencity.php");
	}

	public HiddenCityRequest( int square )
	{
		this();
		this.square = square;
	}

	public HiddenCityRequest( String action)
	{
		this();
		this.action = action;
	}

	public HiddenCityRequest( boolean temple )
	{
		this( "trisocket" );
	}

	public HiddenCityRequest( boolean altar, int itemId )
	{
		this( "roundthing" );
		this.itemId = itemId;
	}

	public int getAdventuresUsed()
	{
		return 1;
	}

	public void reconstructFields()
	{
		this.constructURLString( "hiddencity.php" );

		if ( this.action == null )
		{
			int square = HiddenCityRequest.recommendSquare( this.square );
			if ( square != 0 )
			{
				this.addFormField( "which", String.valueOf( square - 1 ) );
			}
		}
		else
		{
			this.addFormField( "action", this.action );
			if ( this.itemId != 0 )
			{
				this.addFormField( "whichitem", String.valueOf( this.itemId ) );
			}
		}
	}

	private static int recommendSquare( int square )
	{
		// If we are given a valid square to use, take it.
		if ( HiddenCityRequest.validSquare( square ) )
		{
			return square;
		}

		// Otherwise, get the current Hidden City Layout.
		String layout = HiddenCityRequest.hiddenCityLayout();

		if ( square == 0 )
		{
			// If there is an unexplored square, go there.
			square = HiddenCityRequest.firstUnexploredRuins( layout );
			if ( square > 0 )
			{
				return square;
			}

			// If all squares have been visited, pick an undefeated protector spirit
			square = HiddenCityRequest.firstProtectorSpirit( layout );
			if ( square > 0 )
			{
				return square;
			}
		}

		// If all squares have been visited, pick a normal square.
		square = HiddenCityRequest.firstNormalEncounter( layout );
		if ( square > 0 )
		{
			return square;
		}

		// This should not happen
		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Could not pick encounter square to visit." );
		return 0;
	}

	public void run()
	{
		this.reconstructFields();

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		super.run();
	}

	public static final void validateHiddenCity()
	{
		int lastAscension = Preferences.getInteger( "lastHiddenCityAscension" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastHiddenCityAscension", KoLCharacter.getAscensions() );
			Preferences.setString( "hiddenCityLayout", "0000000000000000000000000" );
			HiddenCityRequest.lastSquare = 0;
		}
	}

	public static final String hiddenCityLayout()
	{
		HiddenCityRequest.validateHiddenCity();
		String layout = Preferences.getString( "hiddenCityLayout" );
		if ( layout.length() != 25 )
		{
			layout = "0000000000000000000000000";
			Preferences.setString( "hiddenCityLayout", layout );
		}
		return layout;
	}

	private static boolean validSquare( int square )
	{
		return square >= 1 && square <= 25;
	}

	public static final int lastHiddenCitySquare()
	{
		return HiddenCityRequest.lastSquare;
	}

	public static final int firstUnexploredRuins()
	{
		String layout = HiddenCityRequest.hiddenCityLayout();
		return HiddenCityRequest.firstUnexploredRuins( layout );
	}

	private static final int firstUnexploredRuins( final String layout )
	{
		int square = layout.indexOf( "0" );
		// If there is no unexplored square, indexOf returns -1, we return 0
		return square + 1;
	}

	private static final int firstNormalEncounter( final String layout )
	{
		int square = layout.indexOf( "E" );
		// If there is no normal encounter square, indexOf returns -1, we return 0
		return square + 1;
	}

	private static final int firstProtectorSpirit( final String layout )
	{
		int square = layout.indexOf( "P" );
		// If there is no undefeated protector spirit, indexOf returns -1, we return 0
		return square + 1;
	}

	public void processResults()
	{
		if ( !this.getURLString().startsWith( "hiddencity.php" ) )
		{
			return;
		}

		HiddenCityRequest.parseResponse( this.getURLString(), this.responseText );

		int index = KoLAdventure.findAdventureFailure( this.responseText );
		if ( index >= 0 )
		{
			String failure = KoLAdventure.adventureFailureMessage( index );
			int severity = KoLAdventure.adventureFailureSeverity( index );
			KoLmafia.updateDisplay( severity, failure );
		}
	}

	public static final boolean parseResponse( final String location, final String responseText )
	{
		if ( location.equals( "hiddencity.php" ) )
		{
			HiddenCityRequest.parseCityMap( responseText );
			return true;
		}

		// You carefully socket the four triangular stones into their
		// places in the carving, and step back as the door slowly
		// slides to one side with a loud grinding noise.

		if ( responseText.indexOf( "socket the four triangular stones" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.TRIANGULAR_STONE, -4 );
			return true;
		}

		Matcher matcher = HiddenCityRequest.ROUND_PATTERN.matcher( location );
		if ( !matcher.find() )
		{
			// We simply visited a square
			HiddenCityRequest.identifySquare( location, responseText );
			return true;
		}

		int itemId = StringUtilities.parseInt( matcher.group( 1 ) );

		// You place the cracked stone sphere in the depression atop
		// the altar. You hear a click, and the sphere sinks into the
		// altar and disappears. There is a loud grinding noise, and a
		// niche opens on the front of the altar, containing an odd
		// triangular stone.

		// You place the object on the altar. There is a pregnant
		// pause, as though your offering is being carefully
		// considered. Eventually, it disappears into the altar, and
		// you feel a minor surge of power throughout your body. You
		// get the impression that it wasn't really what the god was
		// looking for, but that it was close enough to be worth
		// something.

		if ( responseText.indexOf( "sinks into the altar" ) != -1 ||
		     responseText.indexOf( "disappears into the altar" ) != -1 )
		{
			ResultProcessor.processItem( itemId, -1 );
			return true;
		}

		return false;
	}

	private static final Pattern MAP_PATTERN = Pattern.compile( "<a href='hiddencity.php\\?which=(\\d+)'[^>]*><img.*?hiddencity/map_([^.]+).gif[^>]*></a>" );

	private static final void parseCityMap( final String text )
	{
		HiddenCityRequest.validateHiddenCity();

		String oldLayout =  Preferences.getString( "hiddenCityLayout" );
		StringBuffer layout = new StringBuffer( oldLayout );

		Matcher matcher = HiddenCityRequest.MAP_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			int square = StringUtilities.parseInt( matcher.group(1) );

			if ( square < 0 || square >= 25 || layout.charAt( square ) != '0' )
			{
				continue;
			}

			String type = matcher.group(2);
			char code =
				type.startsWith( "ruins" ) ? 'E' :
				type.equals( "altar" ) ? 'R' :
				type.equals( "temple" ) ? 'T' :
				type.startsWith( "unruins" ) ? '0' :
				'0';

			layout.setCharAt( square, code );
		}

		String newLayout = layout.toString();

		if ( !oldLayout.equals( newLayout ) )
		{
			Preferences.setString( "hiddenCityLayout", newLayout );
		}
	}

	private static final void identifySquare( final String location, final String responseText )
	{
		int square = HiddenCityRequest.getSquare( location );
		if ( !HiddenCityRequest.validSquare( square ) )
		{
			return;
		}

		if ( responseText.indexOf( "Mansion House of the Black Friars" ) != -1 )
		{
			HiddenCityRequest.addHiddenCityLocation( square, 'T' );
		}
		else if ( responseText.indexOf( "An altar with a carving of a god of nature" ) != -1 )
		{
			HiddenCityRequest.addHiddenCityLocation( square, 'N' );
		}
		else if ( responseText.indexOf( "An altar with a carving of a god of lightning" ) != -1 )
		{
			HiddenCityRequest.addHiddenCityLocation( square, 'L' );
		}
		else if ( responseText.indexOf( "An altar with a carving of a god of water" ) != -1 )
		{
			HiddenCityRequest.addHiddenCityLocation( square, 'W' );
		}
		else if ( responseText.indexOf( "An altar with a carving of a god of fire" ) != -1 )
		{
			HiddenCityRequest.addHiddenCityLocation( square, 'F' );
		}
		else if ( responseText.indexOf( "Dr. Henry \"Dakota\" Fanning, Ph.D., R.I.P." ) != -1 )
		{
			HiddenCityRequest.addHiddenCityLocation( square, 'A' );
		}
		else if ( responseText.indexOf( "cleared that ancient protector spirit out" ) != -1 )
		{
			HiddenCityRequest.addHiddenCityLocation( square, 'D' );
		}
	}

	private static final int getSquare( final String urlString )
	{
		Matcher matcher = HiddenCityRequest.WHICH_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return 0;
		}

		return 1 + StringUtilities.parseInt( matcher.group( 1 ) );
	}

	private static final String currentAltar()
	{
		return HiddenCityRequest.currentAltar( HiddenCityRequest.lastSquare );
	}

	private static final String currentAltar( final int square )
	{
		if ( HiddenCityRequest.validSquare( square ) )
		{
			String layout = HiddenCityRequest.hiddenCityLayout();
			switch ( layout.charAt( square - 1 ) )
			{
			case 'N':
				return "Altar of Bulbazinalli";
			case 'L':
				return "Altar of Pikachutlotal";
			case 'W':
				return "Altar of Squirtlcthulli";
			case 'F':
				return "Altar of Charcoatl";
			}
		}
		return "Altar";
	}

	public static final void addHiddenCityLocation( final char value )
	{
		HiddenCityRequest.addHiddenCityLocation( HiddenCityRequest.lastSquare, value );
	}

	private static final void addHiddenCityLocation( final int square, final char value )
	{
		if ( !HiddenCityRequest.validSquare( square ) )
		{
			return;
		}

		// N (nature) - altar of Bulbazinalli
		// L (lightning) - altar of Pikachutlotal
		// W (water) - altar of Squirtlcthulli
		// F (fire) - altar of Charcoatl
		// R - unspecified altaR
		// P - protector spirit
		// D - defeated protector spirit
		// T - temple
		// E - encounter
		// A - Archaeologist
		// 0 - unidentified

		StringBuffer layout = new StringBuffer( HiddenCityRequest.hiddenCityLayout() );
		layout.setCharAt( square - 1, value );
		Preferences.setString( "hiddenCityLayout", layout.toString() );
	}

	private static final char getHiddenCityLocation()
	{
		return HiddenCityRequest.getHiddenCityLocation( HiddenCityRequest.lastSquare );
	}

	private static final char getHiddenCityLocation( final int square )
	{
		if ( !HiddenCityRequest.validSquare( square ) )
		{
			return '0';
		}

		String layout = HiddenCityRequest.hiddenCityLayout();
		return layout.charAt( square - 1 );
	}

	public static final String getHiddenCityLocationString( final String urlString )
	{
		if ( !urlString.startsWith( "hiddencity.php" ) )
		{
			return null;
		}

		int square = HiddenCityRequest.getSquare( urlString );
		if ( square == 0 )
		{
			return null;
		}

		HiddenCityRequest.lastSquare = square;
		return "Hidden City (Square " + square + ")";
	}

	public static final boolean recordToSession( final String urlString, final String redirect )
	{
		// If this wasn't a Hidden City request, nothing to do.
		if ( !urlString.startsWith( "hiddencity.php" ) )
		{
			return false;
		}

		// If request was not redirected, see if it is a special action
		// in a square or a simple visit. If the former, we recorded it
		// in registerRequest
		if ( urlString.equals( redirect ) )
		{
			return GenericRequest.getAction( urlString ) != null;
		}

		int square = HiddenCityRequest.getSquare( urlString );
		if ( !HiddenCityRequest.validSquare( square ) )
		{
			return false;
		}

		// If the request was redirected to an adventure, handle it and
		// let the caller record it
		if ( redirect.startsWith( "fight.php" ) ||
		     redirect.indexOf( "snarfblat=118" ) != -1 )
		{
			HiddenCityRequest.validateHiddenCity();
			if ( HiddenCityRequest.getHiddenCityLocation( square ) == '0' )
			{
				HiddenCityRequest.addHiddenCityLocation( square, 'E' );
			}

			return false;
		}

		return false;
	}

	// KoLAdventure claims all visits to hiddencity.php that do not include
	// an "action". If they are redirected to a fight or a noncombat
	// encounter, they get logged as "[123] Hidden City (Square 16)"
	// followed by an Encounter

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "hiddencity.php" ) )
		{
			return false;
		}

		// Reset layout the first time we visit the map.
		HiddenCityRequest.validateHiddenCity();

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		String message;
		if ( action.equals( "trisocket" ) )
		{
			message = "[" + KoLAdventure.getAdventureCount() + "] Hidden City (Smallish Temple)" + KoLConstants.LINE_BREAK + "Placing triangular stones into carving";
		}
		else if ( action.equals( "roundthing" ) )
		{
			Matcher matcher = HiddenCityRequest.ROUND_PATTERN.matcher( urlString );
			if ( !matcher.find() )
			{
				// We simply visited a square
				return true;
			}

			int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
			String name = ItemDatabase.getItemName( itemId );

			String altar = HiddenCityRequest.currentAltar();
			message = "[" + KoLAdventure.getAdventureCount() + "] Hidden City (" + altar + ")" + KoLConstants.LINE_BREAK + "Offering " + name + " at "+ altar;
		}
		else
		{
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
