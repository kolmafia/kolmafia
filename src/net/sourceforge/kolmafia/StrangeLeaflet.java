/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JOptionPane;

public abstract class StrangeLeaflet extends StaticEntity
{
	private static final AdventureResult LEAFLET = new AdventureResult( 520, 1 );
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

	private static final String [] LOCATIONS =
	{
		"<b>In the House</b>",
		"<b>West of House</b>",
		"<b>North of the Field</b>",
		"<b>Forest Clearing</b>",
		"<b>Cave</b>",
		"<b>South Bank</b>",
		"<b>Forest</b>",
		"<b>On the other side of the forest maze...</b>",
		"<b>Halfway Up The Tree</b>",
		"<b>Tabletop</b>",
	};

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

	private static boolean mailbox;		// true if mailbox open
	private static boolean wornboots;	// true if we are wearing the boots

	private static boolean door;		// true if door is open
	private static boolean hedge;		// true if hedge is done
	private static boolean torch;		// true if torch is lit
	private static boolean serpent;		// true if serpent is done
	private static boolean chest;		// true if chest is done
	private static boolean fireplace;	// true if fireplace is lit
	private static String magic;		// non-null if magic invoked
	private static String exit;		// non-null if in maze
	private static boolean roadrunner;	// true if roadrunner is done
	private static boolean petunias;	// true if petunias are done
	private static boolean giant;		// true if giant is done

	public static void leafletNoMagic()
	{	robStrangeLeaflet( false );
	}

	public static void leafletWithMagic()
	{	robStrangeLeaflet( true );
	}

	public static void robStrangeLeaflet( boolean invokeMagic )
	{
		// Make sure the player has the Strange Leaflet.
		if ( !KoLCharacter.hasItem( LEAFLET ) )
		{
			if ( KoLCharacter.getLevel() >= 9 )
				DEFAULT_SHELL.executeLine( "council" );
			else
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You are too low level for that quest." );
				return;
			}
		}

		// Deduce location and status of items and actions
		// by initializing the leaflet variables.

		initialize();

		if ( !KoLmafia.permitsContinue() )
			return;

		// Solve the puzzles.

		// For completeness, get the leaflet
		getLeaflet();

		// Open the chest to get the house
		openChest();

		// Get the grue egg from the hole
		robHole();

		// Invoke the magic word, if the player wants to;
		// otherwise, retrieve the trophy in all cases.

		if ( !invokeMagic( invokeMagic ) )
		{
			KoLmafia.updateDisplay( "Serpent-slaying quest complete." );
			return;
		}

		// Get the ring
		getRing();

		String extra = trophy ? " (trophy available)" : ( magic != null ) ? " (magic invoked)" : "";
		KoLmafia.updateDisplay( "Strange leaflet completed" + extra + "." );

		if ( magic != null )
			KoLCharacter.updateStatus();
	}

	private static void initialize()
	{
		// We know nothing about the state of the objects.

		mailbox = false;
		door = false;
		hedge = false;
		torch = false;
		serpent = false;
		chest = false;
		fireplace = false;
		magic = null;
		exit = null;
		roadrunner = false;
		petunias = false;
		giant = false;

		// Find out where we are in the leaflet by using the "inv"
		// command -- this will return your current virtual inventory
		// and also shows your current location in the page title

		String response = executeCommand( "inv" );

		leaflet = response.indexOf( "A junk mail leaflet" ) != -1;
		sword = response.indexOf( "An ornate sword" ) != -1 &&
			response.indexOf( "hangs above the mantel" ) == -1;
		torch = response.indexOf( "A burning torch" ) != -1;
		stick = torch ||
			( response.indexOf( "A hefty stick" ) != -1 &&
			  response.indexOf( "lies on the ground" ) == -1 );
		boots = response.indexOf( "A pair of large rubber wading boots" ) != -1;
		wornboots = boots && response.indexOf( "boots (equipped)" ) != -1;
		parchment = response.indexOf( "A piece of parchment" ) != -1;
		egg = response.indexOf( "A jewel-encrusted egg" ) != -1;
		ruby = response.indexOf( "A fiery ruby" ) != -1;
		scroll = response.indexOf( "A rolled-up scroll" ) != -1;
		ring = response.indexOf( "A giant's pinky ring" ) != -1;
		trophy = response.indexOf( "A shiny bowling trophy" ) != -1;
	}

	private static void parseLocation( String response )
	{
		for ( location = 0; location < LOCATIONS.length; ++location )
			if ( response.indexOf( LOCATIONS[location] ) != -1 )
				break;

		// Assume no maze exit
		exit = null;

		switch ( location )
		{
		case HOUSE:
			fireplace = response.indexOf( "fireplace is lit" ) != -1;
			// You cannot close the door. "close door" =>
			// "You feel a sudden streak of malevolence and
			// decide to leave the door wide open. Serves
			// 'em right for not locking it."
			door = true;
			break;

		case FIELD:
			door = response.indexOf( "front door is closed" ) == -1;
			break;

		case PATH:
			hedge = response.indexOf( "thick hedge" ) == -1;
			break;

		case CLEARING:
			hedge = true;
			break;

		case CAVE:
			hedge = true;
			chest =	 response.indexOf( "empty treasure chest" ) != -1;
			serpent = response.indexOf( "dangerous-looking serpent" ) == -1;
			break;

		case BANK:
			fireplace = true;
			break;

		case FOREST:
			Matcher matcher = FOREST_PATTERN.matcher( response );
			if ( matcher.find() )
				exit = matcher.group(1);
			break;

		case BOTTOM:
			break;

		case TREE:
			roadrunner = response.indexOf( "large ruby in its beak" ) == -1;
			petunias =  response.indexOf( "scroll entangled in the flowers" ) == -1;
			break;

		case TABLE:
			giant = response.indexOf( "The Giant himself" ) == -1;
			break;

		default:
			KoLmafia.updateDisplay( ABORT_STATE, "Server-side change detected.  Script aborted." );
			break;
		}
	}

	private static void parseMantelpiece( String response )
	{
		if ( response.indexOf( "brass bowling trophy" ) != -1 )
			// "A brass bowling trophy sits on the mantelpiece."
			magic = null;
		else if ( response.indexOf( "carved driftwood bird" ) != -1 )
			// "A carved driftwood bird sits on the mantelpiece."
			magic = "plover";
		else if ( response.indexOf( "white house" ) != -1 )
			// "A ceramic model of a small white house sits on the mantelpiece."
			magic = "xyzzy";
		else if ( response.indexOf( "brick building" ) != -1 )
			// "A ceramic model of a brick building sits on the mantelpiece."
			magic = "plugh";
		else if ( response.indexOf( "model ship" ) != -1 )
			// "A model ship inside a bottle sits on the mantelpiece."
			magic = "yoho";
	}

	private static void parseMagic( String response )
	{
		// Bail if we didn't invoke a magic word.
		if ( magic == null)
			return;

		// Check for failures
		if ( response.indexOf( "That only works once." ) != -1 ||
		     // The player already invoked the correct word
		     response.indexOf( "send the plover over" ) != -1  ||
		     // "Red rover, red rover, send the plover over"
		     response.indexOf( "nothing happens" ) != -1 )
		     // "You chant the magic word, and nothing happens. You
		     // hear thunder rumbling in the distance..."
			magic = null;
	}

	private static void getLeaflet()
	{
		// Can't go back
		if ( location > BANK )
			return;

		if ( leaflet )
			return;

		KoLmafia.updateDisplay( "Retrieving mail..." );

		goTo( FIELD );

		// We can't tell if the mailbox is already open. But, there's
		// no harm in opening it twice.
		executeCommand( "open mailbox" );
		mailbox = true;

		executeCommand( "take leaflet" );
		leaflet = true;
	}

	private static void openChest()
	{
		// Can't go back
		if ( location > BANK )
			return;

		if ( chest )
			return;

		KoLmafia.updateDisplay( "Looking for treasure..." );

		goTo( CAVE );
		killSerpent();

		if ( !chest )
		{
			executeCommand( "open chest" );
			DEFAULT_SHELL.executeLine( "use Frobozz Real-Estate Company Instant House (TM)" );
		}
	}

	private static void robHole()
	{
		// Can't go back
		if ( location > BANK )
			return;

		KoLmafia.updateDisplay( "Hunting eggs..." );

		// We can't tell if we've already done this. But, there's no
		// harm in doing it twice.
		executeCommand( "look behind chest" );
		executeCommand( "look in hole" );
	}

	// Returns true if should proceed, false if should stop now
	private static boolean invokeMagic( boolean invokeMagic )
	{
		// Can't go back
		if ( location > BANK )
			return true;

		if ( trophy )
			return true;

		KoLmafia.updateDisplay( "Looking for knick-knacks..." );

		goTo( HOUSE );
		parseMantelpiece( executeCommand( "examine fireplace" ) );

		if ( magic == null )
		{
			executeCommand( "take trophy" );
			trophy = true;
			return true;
		}

		if ( !invokeMagic )
			return false;

		parseMagic( executeCommand( magic ) );
		return true;
	}

	private static void getRing()
	{
		if ( ring )
			return;

		if ( location < BOTTOM )
			goTo( BOTTOM );

		KoLmafia.updateDisplay( "Finding the giant..." );
		getScroll();

		if ( parchment && !KoLCharacter.hasSkill( "CLEESH" ) )
		{
			executeCommand( "GNUSTO CLEESH" );
			KoLCharacter.addAvailableSkill( UseSkillRequest.getInstance( "CLEESH" ) );
			parchment = false;
			scroll = false;
		}

		goTo( TABLE );

		if ( !giant )
			executeCommand( "CLEESH giant" );

		executeCommand( "take ring" );
		ring = true;
	}

	private static void getScroll()
	{
		if ( scroll )
			return;

		goTo( TREE );

		// If it's not in the bowl of petunias, we've gotten it and memorized it
		if ( petunias )
			return;

		getRuby();
		goTo( TREE );
		executeCommand( "throw ruby at petunias" );
		ruby = false;
		executeCommand( "read scroll" );
	}

	private static void getRuby()
	{
		if ( ruby )
			return;

		goTo( TREE );
		if ( !roadrunner )
		{
			if ( !egg )
				executeCommand( "take egg" );
			executeCommand( "throw egg at roadrunner" );
			egg = false;
		}

		goTo( BOTTOM );
		executeCommand( "move leaves" );
		ruby = true;
	}

	private static void goTo( int destination )
	{
		// If you've already reached your destination,
		// you do not need to move.

		if ( location == destination )
			return;

		// Otherwise, get necessary items and go where
		// you need to go.

		switch ( destination )
		{
		case HOUSE:

			switch ( location )
			{
			case BANK:
			case PATH:
			case CLEARING:
			case CAVE:
				goTo( FIELD );
				// Fall through
			case FIELD:
				openDoor();
				executeCommand( "east" );
				break;
			default:
				break;
			}

			break;

		case FIELD:

			switch ( location )
			{
			case HOUSE:
				executeCommand( "west" );
				break;
			case CLEARING:
			case CAVE:
				goTo( PATH );
				// Fall through
			case PATH:
				executeCommand( "south" );
				break;
			case BANK:
				executeCommand( "north" );
				break;
			default:
				break;
			}

			break;

		case PATH:

			switch ( location )
			{
			case HOUSE:
			case BANK:
				goTo( FIELD );
				// Fall through
			case FIELD:
				executeCommand( "north" );
				break;
			case CLEARING:
				executeCommand( "east" );
				break;
			case CAVE:
				executeCommand( "south" );
				break;
			default:
				break;
			}

			break;

		case CLEARING:

			cutHedge();
			goTo( PATH );
			executeCommand( "west" );
			break;

		case CAVE:

			getTorch();
			goTo( PATH );
			executeCommand( "north" );
			break;

		case BANK:

			wearBoots();
			goTo( FIELD );
			executeCommand( "south" );
			break;

		// From here on we've entered the maze and can't go back
		case FOREST:

			// No return
			if ( location > FOREST )
				return;
			goTo( BANK );
			executeCommand( "south" );
			executeCommand( "south" );
			break;

		case BOTTOM:

			switch ( location )
			{
			default:
				goTo( FOREST );
				// Fall through
			case FOREST:
				// Stumble around until we get out
				KoLmafia.updateDisplay( "Navigating the forest..." );
				while ( exit != null )
					executeCommand( exit );
				break;
			case TABLE:
				goTo( TREE );
				// Fall through
			case TREE:
				executeCommand( "down" );
				break;
			}

			break;

		case TREE:

			switch ( location )
			{
			case BOTTOM:
				executeCommand( "up" );
				break;
			case TABLE:
				executeCommand( "down" );
				break;
			}

			break;

		case TABLE:

			goTo( TREE );
			executeCommand( "up" );
			break;
		}
	}

	private static void getSword()
	{
		if ( sword )
			return;

		goTo( HOUSE );
		executeCommand( "take sword" );
		sword = true;
	}

	private static void getStick()
	{
		if ( stick || torch )
			return;

		goTo( PATH );
		executeCommand( "take stick" );
		stick = true;
	}

	private static void cutHedge()
	{
		if ( hedge )
			return;

		getSword();
		goTo( PATH );

		if ( !hedge )
			executeCommand( "cut hedge" );
	}

	private static void openDoor()
	{
		if ( door )
			return;

		goTo( FIELD );

		if ( !door )
			executeCommand( "open door" );
	}

	private static void getTorch()
	{
		if ( torch )
			return;

		cutHedge();

		if ( !stick )
			getStick();

		goTo( CLEARING );
		executeCommand( "light stick" );
		torch = true;
	}

	private static void killSerpent()
	{
		if ( serpent )
			return;

		goTo( CAVE );

		if ( !serpent )
			executeCommand( "kill serpent" );
	}

	private static void wearBoots()
	{
		if ( wornboots )
			return;

		getBoots();
		executeCommand( "wear boots" );
		wornboots = true;
	}

	private static void getBoots()
	{
		if ( boots )
			return;

		lightFire();
		executeCommand( "take boots" );
		boots = true;
	}

	private static void lightFire()
	{
		getTorch();
		goTo( HOUSE );

		if ( fireplace )
			return;

		if ( !parchment )
		{
			executeCommand( "examine fireplace" );
			executeCommand( "examine tinder" );
			parchment = true;
		}

		executeCommand( "light fireplace" );
		fireplace = true;
	}

	private static final KoLRequest LEAFLET_REQUEST = new KoLRequest( "leaflet.php?pwd" );

	private static String executeCommand( String command )
	{
		LEAFLET_REQUEST.addFormField( "command", command );
		LEAFLET_REQUEST.run();

		// Figure out where we are
		parseLocation( LEAFLET_REQUEST.responseText );

		// Let the caller look at the results, if desired
		return LEAFLET_REQUEST.responseText;
	}
}
