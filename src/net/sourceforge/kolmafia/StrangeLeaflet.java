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

public abstract class StrangeLeaflet extends StaticEntity
{
	private static final AdventureResult LEAFLET = new AdventureResult( 520, 1 );

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

	// Items we can pick up

	private static boolean leaflet;
	private static boolean sword;
	private static boolean stick;
	private static boolean boots;
	private static boolean wornboots;
	private static boolean parchment;
	private static boolean egg;
	private static boolean ruby;
	private static boolean scroll;
	private static boolean ring;
	private static boolean trophy;

	// Things we can manipulate

	private static boolean mailbox;
	private static boolean door;
	private static boolean hedge;
	private static boolean torch;
	private static boolean serpent;
	private static boolean chest;
	private static boolean fireplace;
	private static String magic;
	private static String exit;
	private static boolean roadrunner;
	private static boolean petunias;
	private static boolean giant;

	public static void robStrangeLeaflet()
	{
		// Make sure the player has the Strange Leaflet.
		if ( !KoLCharacter.hasItem( LEAFLET, false ) )
		{
			client.updateDisplay( ERROR_STATE, "You don't have a leaflet." );
			client.cancelRequest();
			return;
		}

		// Make sure it's in the inventory
		AdventureDatabase.retrieveItem( LEAFLET );

		// Deduce location and status of items and actions
		// by initializing the leaflet variables.

		initialize();

		if ( !client.permitsContinue() )
			return;

		// Solve the puzzles.

		// For completeness, get the leaflet
		getLeaflet();

		// Open the chest to get the house
		openChest();

		// Get the grue egg from the hole
		robHole();

		// Invoke the magic word
		invokeMagic();

		// Get the ring
		getRing();

		// Add new items to "Usable" list
		KoLCharacter.refreshCalculatedLists();

		String extra = trophy ? " (trophy available)" : ( magic != null ) ? " (magic invoked)" : "";
		client.updateDisplay( ENABLE_STATE, "Strange Leaflet robbed" + extra + "." );
	}

	private static void initialize()
	{
		// We know nothing about the state of the items,
		// so assume that we have nothing for now.

		leaflet = false;
		sword = false;
		stick = false;
		boots = false;
		wornboots = false;
		parchment = false;
		egg = false;
		ruby = false;
		scroll = false;
		ring = false;
		trophy = false;

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

		// Find out where we are in the leaflet by using
		// the "inv" command -- this will return your
		// current virtual inventory and will show you your
		// current location in the after-text.

		executeCommand( "inv" );
	}

	private static void parseLocation( String response )
	{
		for ( location = 0; location < LOCATIONS.length; ++location )
			if ( response.indexOf( LOCATIONS[location] ) != -1 )
				break;

		switch ( location )
		{
			case HOUSE:
				fireplace = response.indexOf( "fireplace is lit" ) != -1;
				door = true;
				parseMantelpiece( response );
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
				Matcher matcher = Pattern.compile( "Gaps in the dense, forbidding foliage lead (.*?)," ).matcher( response );
				if ( matcher.find() )
					exit = matcher.group(1);
				break;

			case BOTTOM:
				exit = null;
				break;

			case TREE:
				roadrunner = response.indexOf( "large ruby in its beak" ) != -1;
				petunias =  response.indexOf( "scroll entangled in the flowers" ) != -1;
				break;

			case TABLE:
				giant = response.indexOf( "The Giant himself" ) != -1;
				break;

			default:
				client.updateDisplay( ERROR_STATE, "I can't figure out where you are!" );
				client.cancelRequest();
				break;
		}
	}

	private static void parseMantelpiece( String response )
	{
		if ( response.indexOf( "brass bowling trophy" ) != -1 )
			magic = null;
		else if ( response.indexOf( "carved driftwood bird" ) != -1 )
			// "A carved driftwood bird sits on the mantelpiece."
			magic = "plover";
		else if ( response.indexOf( "white house" ) != -1 )
			magic = "xyzzy";
		else if ( response.indexOf( "brick building" ) != -1 )
			// "A ceramic model of a brick building sits on the mantelpiece."
			magic = "plugh";
		else if ( response.indexOf( "model ship" ) != -1 )
			magic = "yoho";
	}

	private static void getLeaflet()
	{
		// Can't go back
		if ( location > BANK )
			return;

		if ( leaflet )
			return;

		client.updateDisplay( NORMAL_STATE, "Retrieving mail..." );

		goTo( FIELD );

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

		client.updateDisplay( NORMAL_STATE, "Looking for treasure..." );

		goTo( CAVE );
		killSerpent();

		if ( !chest )
			executeCommand( "open chest" );
	}

	private static void robHole()
	{
		// Can't go back
		if ( location > BANK )
			return;

		client.updateDisplay( NORMAL_STATE, "Hunting eggs..." );
		executeCommand( "look behind chest" );
		executeCommand( "look in hole" );
	}

	private static void invokeMagic()
	{
		// Can't go back
		if ( location > BANK )
			return;

		if ( trophy )
			return;

		client.updateDisplay( NORMAL_STATE, "Invoking magic..." );

		goTo( HOUSE );
		executeCommand( "examine fireplace" );

		if ( magic == null )
		{
			executeCommand( "take trophy" );
			trophy = true;
			return;
		}

		executeCommand( magic );
	}

	private static void getRing()
	{
		if ( ring )
			return;

		client.updateDisplay( NORMAL_STATE, "Stealing ring..." );

		if ( location < BOTTOM )
			goTo( BOTTOM );

		getScroll();

		if ( parchment )
		{
			executeCommand( "GNUSTO CLEESH" );
			parchment = false;
			scroll = false;
		}

		goTo( TABLE );

		if ( giant )
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
		if ( !petunias )
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

		if ( roadrunner )
		{
			if ( !egg )
				executeCommand( "take egg" );
			executeCommand( "throw egg at roadrunner" );
			egg = false;
			roadrunner = false;
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
			{
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
			}

			case FIELD:
			{
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
			}

			case PATH:
			{
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
			}

			case CLEARING:
			{
				cutHedge();
				goTo( PATH );
				executeCommand( "west" );
				break;
			}

			case CAVE:
			{
				getTorch();
				goTo( PATH );
				executeCommand( "north" );
				break;
			}

			case BANK:
			{
				wearBoots();
				goTo( FIELD );
				executeCommand( "south" );
				break;
			}

			// From here on we've entered the maze and can't go back
			case FOREST:
			{
				// No return
				if ( location > FOREST )
					return;
				goTo( BANK );
				executeCommand( "south" );
				executeCommand( "south" );
				break;
			}

			case BOTTOM:
			{
				switch ( location )
				{
					default:
						goTo( FOREST );
						// Fall through
					case FOREST:
						// Stumble around until we get out
						while ( exit != null )
							executeCommand( exit );
						break;
					case TABLE:
						goTo( TREE);
						// Fall through
					case TREE:
						executeCommand( "down" );
						break;
				}
				break;
			}

			case TREE:
			{
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
			}

			case TABLE:
			{
				goTo( TREE);
				executeCommand( "up" );
				break;
			}
		}
	}

	private static void getSword()
	{
		if ( sword )
			return;

		goTo( HOUSE );

		if ( !sword )
			executeCommand( "take sword" );
	}

	private static void getStick()
	{
		if ( stick || torch )
			return;

		goTo( PATH );
		if ( !stick )
			executeCommand( "take stick" );
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

	private static void executeCommand( String command )
	{
		KoLRequest request = new KoLRequest( client, "leaflet.php", true );
		request.addFormField( "pwd", client.getPasswordHash() );
		request.addFormField( "command", command );
		request.run();

		// The inventory command is handled specially because
		// it resets the state of your items.

		if ( command.equals( "inv" ) )
		{
			leaflet = request.responseText.indexOf( "A junk mail leaflet" ) != -1;
			sword = request.responseText.indexOf( "An ornate sword" ) != -1;
			torch = request.responseText.indexOf( "A burning torch" ) != -1;
			stick = torch || request.responseText.indexOf( "A hefty stick" ) != -1;
			boots = request.responseText.indexOf( "A pair of large rubber wading boots" ) != -1;
			wornboots = boots && request.responseText.indexOf( "boots (equipped)" ) != -1;
			parchment = request.responseText.indexOf( "A piece of parchment" ) != -1;
			egg = request.responseText.indexOf( "A jewel-encrusted egg" ) != -1;
			ruby = request.responseText.indexOf( "A fiery ruby" ) != -1;
			scroll = request.responseText.indexOf( "A rolled-up scroll" ) != -1;
			ring = request.responseText.indexOf( "A giant's pinky ring" ) != -1;
			trophy = request.responseText.indexOf( "A shiny bowling trophy" ) != -1;
		}

		// Deduce status of items and objects based on your
		// current location.

		parseLocation( request.responseText );
	}
}
