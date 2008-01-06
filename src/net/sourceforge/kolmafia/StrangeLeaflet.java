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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StrangeLeaflet
	extends StaticEntity
{
	private static final AdventureResult LEAFLET = new AdventureResult( 520, 1 );
	private static final AdventureResult FROBOZZ = new AdventureResult( 526, 1 );
	private static final Pattern FOREST_PATTERN = Pattern.compile( "Gaps in the dense, forbidding foliage lead (.*?)," );

	// This script assumes that the leaflet can be in any state; the player
	// can open the leaflet, move around, and manipulate objects in any way
	// desired, and then invoke this script, which will detect what has been
	// done and carry on from there to complete everything still possible.
	//
	// Needless to say, the player could have done things that prohibit
	// earlier actions: lighting the fireplace precludes recovering the
	// parchment, and entering the forest maze precludes doing anything
	// from the first half of the map, for example.

	// Things to be checked/confirmed:
	//
	// 1) The brass bowling trophy, model ship, white house, and carved
	//    driftwood bird have all been seen and confirmed by me. The brick
	//    building has been reported, but not actually successfully used,
	//    yet, by this script.

	// There are ten locations within the Strange Leaflet:

	//                 Cave
	//                   ^
	//                   | N/S
	//          E/W      v
	// Clearing <-> Forest Path
	//                   ^
	//                   | N/S
	//                   v      E/W
	//               Open Field <-> Inside House
	//                   ^
	//                   | N/S
	//                   v
	//               South Bank           Table top
	//                   |                     ^
	//                   | S + S               | U/D
	//                   v                     v
	//               Forest Maze         Middle of tree
	//                   |                     ^
	//         N/S/E/W   |                     | U/D
	//                   |                     v
	//                   +-------------> Bottom of tree

	private static final int HOUSE = 0;
	private static final int FIELD = 1;
	private static final int PATH = 2;
	private static final int CLEARING = 3;
	private static final int CAVE = 4;
	private static final int BANK = 5;
	private static final int FOREST = 6;
	private static final int BOTTOM = 7;
	private static final int TREE = 8;
	private static final int TABLE = 9;

	// Strings that unambiguously identify current location

	private static final String[] LOCATIONS =
		{ "<b>In the House</b>", "<b>West of House</b>", "<b>North of the Field</b>", "<b>Forest Clearing</b>", "<b>Cave</b>", "<b>South Bank</b>", "<b>Forest</b>", "<b>On the other side of the forest maze...</b>", "<b>Halfway Up The Tree</b>", "<b>Tabletop</b>", };

	// Current location

	private static int location;

	// Items we can pick up: true if in inventory, false otherwise

	private static boolean leaflet;
	private static boolean sword;
	private static boolean stick;
	private static boolean boots;
	private static boolean parchment;
	private static boolean egg;
	private static boolean ruby;
	private static boolean scroll;
	private static boolean ring;
	private static boolean trophy;

	// Things we can manipulate

	private static boolean wornboots; // true if we are wearing the boots

	private static boolean door; // true if door is open
	private static boolean hedge; // true if hedge is done
	private static boolean torch; // true if torch is lit
	private static boolean serpent; // true if serpent is done
	private static boolean chest; // true if chest is done
	private static boolean fireplace; // true if fireplace is lit
	private static String magic; // non-null if magic invoked
	private static String exit; // non-null if in maze
	private static boolean roadrunner; // true if roadrunner is done
	private static boolean petunias; // true if petunias are done
	private static boolean giant; // true if giant is done

	public static final void leafletNoMagic()
	{
		StrangeLeaflet.robStrangeLeaflet( false );
	}

	public static final void leafletWithMagic()
	{
		StrangeLeaflet.robStrangeLeaflet( true );
	}

	public static final void robStrangeLeaflet( final boolean invokeMagic )
	{
		// Make sure the player has the Strange Leaflet.
		if ( !KoLCharacter.hasItem( StrangeLeaflet.LEAFLET ) )
		{
			if ( KoLCharacter.getLevel() >= 9 )
			{
				RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );
			}
			else
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You are too low level for that quest." );
				return;
			}
		}

		// Deduce location and status of items and actions
		// by initializing the leaflet variables.

		KoLmafia.updateDisplay( "Determining current leaflet progress..." );
		StrangeLeaflet.initialize();

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Solve the puzzles.

		// For completeness, get the leaflet
		StrangeLeaflet.getLeaflet();

		// Open the chest to get the house
		StrangeLeaflet.openChest();

		// Get the grue egg from the hole
		StrangeLeaflet.robHole();

		// Invoke the magic word, if the player wants to;
		// otherwise, retrieve the trophy in all cases.

		if ( !StrangeLeaflet.invokeMagic( invokeMagic ) )
		{
			KoLmafia.updateDisplay( "Serpent-slaying quest complete." );
			return;
		}

		// Get the ring
		StrangeLeaflet.getRing();

		String extra =
			StrangeLeaflet.trophy ? " (trophy available)" : StrangeLeaflet.magic != null ? " (magic invoked)" : "";
		KoLmafia.updateDisplay( "Strange leaflet completed" + extra + "." );

		if ( StrangeLeaflet.magic != null )
		{
			KoLCharacter.updateStatus();
		}
	}

	private static final void initialize()
	{
		// We know nothing about the state of the objects.

		StrangeLeaflet.door = false;
		StrangeLeaflet.hedge = false;
		StrangeLeaflet.torch = false;
		StrangeLeaflet.serpent = false;
		StrangeLeaflet.chest = false;
		StrangeLeaflet.fireplace = false;
		StrangeLeaflet.magic = null;
		StrangeLeaflet.exit = null;
		StrangeLeaflet.roadrunner = false;
		StrangeLeaflet.petunias = false;
		StrangeLeaflet.giant = false;

		// Find out where we are in the leaflet by using the "inv"
		// command -- this will return your current virtual inventory
		// and also shows your current location in the page title

		String response = StrangeLeaflet.executeCommand( "inv" );

		StrangeLeaflet.leaflet = response.indexOf( "A junk mail leaflet" ) != -1;
		StrangeLeaflet.sword =
			response.indexOf( "An ornate sword" ) != -1 && response.indexOf( "hangs above the mantel" ) == -1;
		StrangeLeaflet.torch = response.indexOf( "A burning torch" ) != -1;
		StrangeLeaflet.stick =
			StrangeLeaflet.torch || response.indexOf( "A hefty stick" ) != -1 && response.indexOf( "lies on the ground" ) == -1;
		StrangeLeaflet.boots = response.indexOf( "A pair of large rubber wading boots" ) != -1;
		StrangeLeaflet.wornboots = StrangeLeaflet.boots && response.indexOf( "boots (equipped)" ) != -1;
		StrangeLeaflet.parchment = response.indexOf( "A piece of parchment" ) != -1;
		StrangeLeaflet.egg = response.indexOf( "A jewel-encrusted egg" ) != -1;
		StrangeLeaflet.ruby = response.indexOf( "A fiery ruby" ) != -1;
		StrangeLeaflet.scroll = response.indexOf( "A rolled-up scroll" ) != -1;
		StrangeLeaflet.ring = response.indexOf( "A giant's pinky ring" ) != -1;
		StrangeLeaflet.trophy = response.indexOf( "A shiny bowling trophy" ) != -1;
	}

	private static final void parseLocation( final String response )
	{
		for ( StrangeLeaflet.location = 0; StrangeLeaflet.location < StrangeLeaflet.LOCATIONS.length; ++StrangeLeaflet.location )
		{
			if ( response.indexOf( StrangeLeaflet.LOCATIONS[ StrangeLeaflet.location ] ) != -1 )
			{
				break;
			}
		}

		// Assume no maze exit
		StrangeLeaflet.exit = null;

		switch ( StrangeLeaflet.location )
		{
		case HOUSE:
			StrangeLeaflet.fireplace = response.indexOf( "fireplace is lit" ) != -1;
			// You cannot close the door. "close door" =>
			// "You feel a sudden streak of malevolence and
			// decide to leave the door wide open. Serves
			// 'em right for not locking it."
			StrangeLeaflet.door = true;
			break;

		case FIELD:
			StrangeLeaflet.door = response.indexOf( "front door is closed" ) == -1;
			break;

		case PATH:
			StrangeLeaflet.hedge = response.indexOf( "thick hedge" ) == -1;
			break;

		case CLEARING:
			StrangeLeaflet.hedge = true;
			break;

		case CAVE:
			StrangeLeaflet.hedge = true;
			StrangeLeaflet.chest = response.indexOf( "empty treasure chest" ) != -1;
			StrangeLeaflet.serpent = response.indexOf( "dangerous-looking serpent" ) == -1;
			break;

		case BANK:
			StrangeLeaflet.fireplace = true;
			break;

		case FOREST:
			Matcher matcher = StrangeLeaflet.FOREST_PATTERN.matcher( response );
			if ( matcher.find() )
			{
				StrangeLeaflet.exit = matcher.group( 1 );
			}
			break;

		case BOTTOM:
			break;

		case TREE:
			StrangeLeaflet.roadrunner = response.indexOf( "large ruby in its beak" ) == -1;
			StrangeLeaflet.petunias = response.indexOf( "scroll entangled in the flowers" ) == -1;
			break;

		case TABLE:
			StrangeLeaflet.giant = response.indexOf( "The Giant himself" ) == -1;
			break;

		default:
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Server-side change detected.  Script aborted." );
			break;
		}
	}

	private static final void parseMantelpiece( final String response )
	{
		if ( response.indexOf( "brass bowling trophy" ) != -1 )
		{
			// "A brass bowling trophy sits on the mantelpiece."
			StrangeLeaflet.magic = null;
		}
		else if ( response.indexOf( "carved driftwood bird" ) != -1 )
		{
			// "A carved driftwood bird sits on the mantelpiece."
			StrangeLeaflet.magic = "plover";
		}
		else if ( response.indexOf( "white house" ) != -1 )
		{
			// "A ceramic model of a small white house sits on the mantelpiece."
			StrangeLeaflet.magic = "xyzzy";
		}
		else if ( response.indexOf( "brick building" ) != -1 )
		{
			// "A ceramic model of a brick building sits on the mantelpiece."
			StrangeLeaflet.magic = "plugh";
		}
		else if ( response.indexOf( "model ship" ) != -1 )
		{
			// "A model ship inside a bottle sits on the mantelpiece."
			StrangeLeaflet.magic = "yoho";
		}
	}

	private static final void parseMagic( final String response )
	{
		// Bail if we didn't invoke a magic word.
		if ( StrangeLeaflet.magic == null )
		{
			return;
		}

		// Check for failures
		if ( response.indexOf( "That only works once." ) != -1 ||
		// The player already invoked the correct word
		response.indexOf( "send the plover over" ) != -1 ||
		// "Red rover, red rover, send the plover over"
		response.indexOf( "nothing happens" ) != -1 )
		{
			// "You chant the magic word, and nothing happens. You
			// hear thunder rumbling in the distance..."
			StrangeLeaflet.magic = null;
		}
	}

	private static final void getLeaflet()
	{
		// Can't go back
		if ( StrangeLeaflet.location > StrangeLeaflet.BANK )
		{
			return;
		}

		if ( StrangeLeaflet.leaflet )
		{
			return;
		}

		KoLmafia.updateDisplay( "Retrieving mail..." );

		StrangeLeaflet.goTo( StrangeLeaflet.FIELD );

		// We can't tell if the mailbox is already open. But, there's
		// no harm in opening it twice.
		StrangeLeaflet.executeCommand( "open mailbox" );
		StrangeLeaflet.executeCommand( "take leaflet" );
		StrangeLeaflet.leaflet = true;
	}

	private static final void openChest()
	{
		// Can't go back
		if ( StrangeLeaflet.location > StrangeLeaflet.BANK )
		{
			return;
		}

		if ( StrangeLeaflet.chest )
		{
			return;
		}

		KoLmafia.updateDisplay( "Looking for treasure..." );

		StrangeLeaflet.goTo( StrangeLeaflet.CAVE );
		StrangeLeaflet.killSerpent();

		if ( !StrangeLeaflet.chest )
		{
			StrangeLeaflet.executeCommand( "open chest" );
			RequestThread.postRequest( new ConsumeItemRequest( StrangeLeaflet.FROBOZZ ) );
		}
	}

	private static final void robHole()
	{
		// Can't go back
		if ( StrangeLeaflet.location > StrangeLeaflet.BANK )
		{
			return;
		}

		KoLmafia.updateDisplay( "Hunting eggs..." );

		// We can't tell if we've already done this. But, there's no
		// harm in doing it twice.
		StrangeLeaflet.executeCommand( "look behind chest" );
		StrangeLeaflet.executeCommand( "look in hole" );
	}

	// Returns true if should proceed, false if should stop now
	private static final boolean invokeMagic( boolean invokeMagic )
	{
		// Can't go back
		if ( StrangeLeaflet.location > StrangeLeaflet.BANK )
		{
			return true;
		}

		if ( StrangeLeaflet.trophy )
		{
			return true;
		}

		KoLmafia.updateDisplay( "Looking for knick-knacks..." );

		StrangeLeaflet.goTo( StrangeLeaflet.HOUSE );
		StrangeLeaflet.parseMantelpiece( StrangeLeaflet.executeCommand( "examine fireplace" ) );

		if ( StrangeLeaflet.magic == null )
		{
			StrangeLeaflet.executeCommand( "take trophy" );
			StrangeLeaflet.trophy = true;
			return true;
		}

		if ( !invokeMagic )
		{
			return false;
		}

		StrangeLeaflet.parseMagic( StrangeLeaflet.executeCommand( StrangeLeaflet.magic ) );
		return true;
	}

	private static final void getRing()
	{
		if ( StrangeLeaflet.ring )
		{
			return;
		}

		if ( StrangeLeaflet.location < StrangeLeaflet.BOTTOM )
		{
			StrangeLeaflet.goTo( StrangeLeaflet.BOTTOM );
		}

		KoLmafia.updateDisplay( "Finding the giant..." );
		StrangeLeaflet.getScroll();

		if ( StrangeLeaflet.parchment && !KoLCharacter.hasSkill( "CLEESH" ) )
		{
			StrangeLeaflet.executeCommand( "GNUSTO CLEESH" );
			KoLCharacter.addAvailableSkill( "CLEESH" );
			StrangeLeaflet.parchment = false;
			StrangeLeaflet.scroll = false;
		}

		StrangeLeaflet.goTo( StrangeLeaflet.TABLE );

		if ( !StrangeLeaflet.giant )
		{
			StrangeLeaflet.executeCommand( "CLEESH giant" );
		}

		StrangeLeaflet.executeCommand( "take ring" );
		StrangeLeaflet.ring = true;
	}

	private static final void getScroll()
	{
		if ( StrangeLeaflet.scroll )
		{
			return;
		}

		StrangeLeaflet.goTo( StrangeLeaflet.TREE );

		// If it's not in the bowl of petunias, we've gotten it and memorized it
		if ( StrangeLeaflet.petunias )
		{
			return;
		}

		StrangeLeaflet.getRuby();
		StrangeLeaflet.goTo( StrangeLeaflet.TREE );
		StrangeLeaflet.executeCommand( "throw ruby at petunias" );
		StrangeLeaflet.ruby = false;
		StrangeLeaflet.executeCommand( "read scroll" );
	}

	private static final void getRuby()
	{
		if ( StrangeLeaflet.ruby )
		{
			return;
		}

		StrangeLeaflet.goTo( StrangeLeaflet.TREE );
		if ( !StrangeLeaflet.roadrunner )
		{
			if ( !StrangeLeaflet.egg )
			{
				StrangeLeaflet.executeCommand( "take egg" );
			}
			StrangeLeaflet.executeCommand( "throw egg at roadrunner" );
			StrangeLeaflet.egg = false;
		}

		StrangeLeaflet.goTo( StrangeLeaflet.BOTTOM );
		StrangeLeaflet.executeCommand( "move leaves" );
		StrangeLeaflet.ruby = true;
	}

	private static final void goTo( final int destination )
	{
		// If you've already reached your destination,
		// you do not need to move.

		if ( StrangeLeaflet.location == destination )
		{
			return;
		}

		// Otherwise, get necessary items and go where
		// you need to go.

		switch ( destination )
		{
		case HOUSE:

			switch ( StrangeLeaflet.location )
			{
			case BANK:
			case PATH:
			case CLEARING:
			case CAVE:
				StrangeLeaflet.goTo( StrangeLeaflet.FIELD );
				// Fall through
			case FIELD:
				StrangeLeaflet.openDoor();
				StrangeLeaflet.executeCommand( "east" );
				break;
			default:
				break;
			}

			break;

		case FIELD:

			switch ( StrangeLeaflet.location )
			{
			case HOUSE:
				StrangeLeaflet.executeCommand( "west" );
				break;
			case CLEARING:
			case CAVE:
				StrangeLeaflet.goTo( StrangeLeaflet.PATH );
				// Fall through
			case PATH:
				StrangeLeaflet.executeCommand( "south" );
				break;
			case BANK:
				StrangeLeaflet.executeCommand( "north" );
				break;
			default:
				break;
			}

			break;

		case PATH:

			switch ( StrangeLeaflet.location )
			{
			case HOUSE:
			case BANK:
				StrangeLeaflet.goTo( StrangeLeaflet.FIELD );
				// Fall through
			case FIELD:
				StrangeLeaflet.executeCommand( "north" );
				break;
			case CLEARING:
				StrangeLeaflet.executeCommand( "east" );
				break;
			case CAVE:
				StrangeLeaflet.executeCommand( "south" );
				break;
			default:
				break;
			}

			break;

		case CLEARING:

			StrangeLeaflet.cutHedge();
			StrangeLeaflet.goTo( StrangeLeaflet.PATH );
			StrangeLeaflet.executeCommand( "west" );
			break;

		case CAVE:

			StrangeLeaflet.getTorch();
			StrangeLeaflet.goTo( StrangeLeaflet.PATH );
			StrangeLeaflet.executeCommand( "north" );
			break;

		case BANK:

			StrangeLeaflet.wearBoots();
			StrangeLeaflet.goTo( StrangeLeaflet.FIELD );
			StrangeLeaflet.executeCommand( "south" );
			break;

		// From here on we've entered the maze and can't go back
		case FOREST:

			// No return
			if ( StrangeLeaflet.location > StrangeLeaflet.FOREST )
			{
				return;
			}
			StrangeLeaflet.goTo( StrangeLeaflet.BANK );
			StrangeLeaflet.executeCommand( "south" );
			StrangeLeaflet.executeCommand( "south" );
			break;

		case BOTTOM:

			switch ( StrangeLeaflet.location )
			{
			default:
				StrangeLeaflet.goTo( StrangeLeaflet.FOREST );
				// Fall through
			case FOREST:
				// Stumble around until we get out
				KoLmafia.updateDisplay( "Navigating the forest..." );
				while ( StrangeLeaflet.exit != null )
				{
					StrangeLeaflet.executeCommand( StrangeLeaflet.exit );
				}
				break;
			case TABLE:
				StrangeLeaflet.goTo( StrangeLeaflet.TREE );
				// Fall through
			case TREE:
				StrangeLeaflet.executeCommand( "down" );
				break;
			}

			break;

		case TREE:

			switch ( StrangeLeaflet.location )
			{
			case BOTTOM:
				StrangeLeaflet.executeCommand( "up" );
				break;
			case TABLE:
				StrangeLeaflet.executeCommand( "down" );
				break;
			}

			break;

		case TABLE:

			StrangeLeaflet.goTo( StrangeLeaflet.TREE );
			StrangeLeaflet.executeCommand( "up" );
			break;
		}
	}

	private static final void getSword()
	{
		if ( StrangeLeaflet.sword )
		{
			return;
		}

		StrangeLeaflet.goTo( StrangeLeaflet.HOUSE );
		StrangeLeaflet.executeCommand( "take sword" );
		StrangeLeaflet.sword = true;
	}

	private static final void getStick()
	{
		if ( StrangeLeaflet.stick || StrangeLeaflet.torch )
		{
			return;
		}

		StrangeLeaflet.goTo( StrangeLeaflet.PATH );
		StrangeLeaflet.executeCommand( "take stick" );
		StrangeLeaflet.stick = true;
	}

	private static final void cutHedge()
	{
		if ( StrangeLeaflet.hedge )
		{
			return;
		}

		StrangeLeaflet.getSword();
		StrangeLeaflet.goTo( StrangeLeaflet.PATH );

		if ( !StrangeLeaflet.hedge )
		{
			StrangeLeaflet.executeCommand( "cut hedge" );
		}
	}

	private static final void openDoor()
	{
		if ( StrangeLeaflet.door )
		{
			return;
		}

		StrangeLeaflet.goTo( StrangeLeaflet.FIELD );

		if ( !StrangeLeaflet.door )
		{
			StrangeLeaflet.executeCommand( "open door" );
		}
	}

	private static final void getTorch()
	{
		if ( StrangeLeaflet.torch )
		{
			return;
		}

		StrangeLeaflet.cutHedge();

		if ( !StrangeLeaflet.stick )
		{
			StrangeLeaflet.getStick();
		}

		StrangeLeaflet.goTo( StrangeLeaflet.CLEARING );
		StrangeLeaflet.executeCommand( "light stick" );
		StrangeLeaflet.torch = true;
	}

	private static final void killSerpent()
	{
		if ( StrangeLeaflet.serpent )
		{
			return;
		}

		StrangeLeaflet.goTo( StrangeLeaflet.CAVE );

		if ( !StrangeLeaflet.serpent )
		{
			StrangeLeaflet.executeCommand( "kill serpent" );
		}
	}

	private static final void wearBoots()
	{
		if ( StrangeLeaflet.wornboots )
		{
			return;
		}

		StrangeLeaflet.getBoots();
		StrangeLeaflet.executeCommand( "wear boots" );
		StrangeLeaflet.wornboots = true;
	}

	private static final void getBoots()
	{
		if ( StrangeLeaflet.boots )
		{
			return;
		}

		StrangeLeaflet.lightFire();
		StrangeLeaflet.executeCommand( "take boots" );
		StrangeLeaflet.boots = true;
	}

	private static final void lightFire()
	{
		StrangeLeaflet.getTorch();
		StrangeLeaflet.goTo( StrangeLeaflet.HOUSE );

		if ( StrangeLeaflet.fireplace )
		{
			return;
		}

		if ( !StrangeLeaflet.parchment )
		{
			StrangeLeaflet.executeCommand( "examine fireplace" );
			StrangeLeaflet.executeCommand( "examine tinder" );
			StrangeLeaflet.parchment = true;
		}

		StrangeLeaflet.executeCommand( "light fireplace" );
		StrangeLeaflet.fireplace = true;
	}

	private static final KoLRequest LEAFLET_REQUEST = new KoLRequest( "leaflet.php" );

	private static final String executeCommand( final String command )
	{
		KoLRequest request = StrangeLeaflet.LEAFLET_REQUEST;
		request.clearDataFields();
		request.addFormField( "pwd" );
		request.addFormField( "command", command );
		RequestThread.postRequest( request );

		// Figure out where we are
		StrangeLeaflet.parseLocation( request.responseText );

		// Let the caller look at the results, if desired
		return request.responseText;
	}
}
