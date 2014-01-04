/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.LeafletRequest;

import net.sourceforge.kolmafia.swingui.CouncilFrame;

public abstract class LeafletManager
{
	private static final LeafletRequest LEAFLET_REQUEST = new LeafletRequest();

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

	private static final String[][] LOCATIONS =
	{
		{
			"<b>In the House</b>",
			"in the house",
		},
		{
			"<b>West of House</b>",
			"west of the house",
		},
		{
			"<b>North of the Field</b>",
			"north of the field",
		},
		{
			"<b>Forest Clearing</b>",
			"in the forest clearing",
		},
		{
			"<b>Cave</b>",
			"in the cave",
		},
		{
			"<b>South Bank</b>",
			"on the south bank",
		},
		{
			"<b>Forest</b>",
			"in the forest",
		},
		{
			"<b>On the other side of the forest maze...</b>",
			"past maze",
		},
		{
			"<b>Halfway Up The Tree</b>",
			"halfway up the tree",
		},
		{
			"<b>Tabletop</b>",
			"on the tabletop",
		}
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

	public static final String locationName()
	{
		String response = LeafletManager.executeCommand( "inv" );
		return LeafletManager.locationName( response );
	}

	public static final String leafletWithMagic()
	{
		return LeafletManager.robStrangeLeaflet( true );
	}

	public static final String leafletNoMagic()
	{
		return LeafletManager.robStrangeLeaflet( false );
	}

	public static final String robStrangeLeaflet( final boolean invokeMagic )
	{
		// Make sure the player has the Strange Leaflet.
		if ( !InventoryManager.hasItem( ItemPool.STRANGE_LEAFLET ) )
		{
			if ( KoLCharacter.getLevel() >= 9 )
			{
				RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You are too low level for that quest." );
				return CouncilFrame.COUNCIL_VISIT.responseText;
			}
		}

		try
		{
			// Deduce location and status of items and actions
			LeafletManager.initialize();

			// Solve the puzzles.
			LeafletManager.solveLeaflet( invokeMagic );
		}
		catch ( LeafletException e )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, e.getMessage() );
		}

		return LeafletManager.LEAFLET_REQUEST.responseText;
	}

	private static final void initialize()
		throws LeafletException
	{
		// We know nothing about the state of the objects.

		LeafletManager.door = false;
		LeafletManager.hedge = false;
		LeafletManager.torch = false;
		LeafletManager.serpent = false;
		LeafletManager.chest = false;
		LeafletManager.fireplace = false;
		LeafletManager.magic = null;
		LeafletManager.exit = null;
		LeafletManager.roadrunner = false;
		LeafletManager.petunias = false;
		LeafletManager.giant = false;

		// Find out where we are in the leaflet by using the "inv"
		// command -- this will return your current virtual inventory
		// and also shows your current location in the page title

		KoLmafia.updateDisplay( "Determining current leaflet progress..." );
		String response = LeafletManager.executeCommand( "inv" );

		LeafletManager.leaflet = response.indexOf( "A junk mail leaflet" ) != -1;
		LeafletManager.sword =
			response.indexOf( "An ornate sword" ) != -1 && response.indexOf( "hangs above the mantel" ) == -1;
		LeafletManager.torch = response.indexOf( "A burning torch" ) != -1;
		LeafletManager.stick =
			LeafletManager.torch || response.indexOf( "A hefty stick" ) != -1 && response.indexOf( "lies on the ground" ) == -1;
		LeafletManager.boots = response.indexOf( "A pair of large rubber wading boots" ) != -1;
		LeafletManager.wornboots = LeafletManager.boots && response.indexOf( "boots (equipped)" ) != -1;
		LeafletManager.parchment = response.indexOf( "A piece of parchment" ) != -1;
		LeafletManager.egg = response.indexOf( "A jewel-encrusted egg" ) != -1;
		LeafletManager.ruby = response.indexOf( "A fiery ruby" ) != -1;
		LeafletManager.scroll = response.indexOf( "A rolled-up scroll" ) != -1;
		LeafletManager.ring = response.indexOf( "A giant's pinky ring" ) != -1;
		LeafletManager.trophy = response.indexOf( "A shiny bowling trophy" ) != -1;
	}

	private static final void solveLeaflet( final boolean invokeMagic )
		throws LeafletException
	{
		// For completeness, get the leaflet
		LeafletManager.getLeaflet();

		// Open the chest to get the house
		LeafletManager.openChest();

		// Get the grue egg from the hole
		LeafletManager.robHole();

		// Invoke the magic word, if the player wants to;
		// otherwise, retrieve the trophy in all cases.

		if ( !LeafletManager.invokeMagic( invokeMagic ) )
		{
			KoLmafia.updateDisplay( "Serpent-slaying quest complete." );
			return;
		}

		// Get the ring
		LeafletManager.getRing();

		String extra =
			LeafletManager.trophy ? " (trophy available)" : LeafletManager.magic != null ? " (magic invoked)" : "";
		KoLmafia.updateDisplay( "Strange leaflet completed" + extra + "." );

		if ( LeafletManager.magic != null )
		{
			KoLCharacter.updateStatus();
		}
	}

	private static final String executeCommand( final String command )
	{
		LeafletManager.LEAFLET_REQUEST.setCommand( command );
		RequestThread.postRequest( LeafletManager.LEAFLET_REQUEST );

		// Figure out where we are
		LeafletManager.parseLocation( LeafletManager.LEAFLET_REQUEST.responseText );

		// Let the caller look at the results, if desired
		return LeafletManager.LEAFLET_REQUEST.responseText;
	}

	private static final int getLocation( final String response )
	{
		for ( int location = 0; location < LeafletManager.LOCATIONS.length; ++location )
		{
			String [] names = LeafletManager.LOCATIONS[ location ];
			if ( response.indexOf( names[0] ) != -1 )
			{
				return location;
			}
		}

		return -1;
	}

	private static final String locationName( final String response )
	{
                return LeafletManager.locationName( LeafletManager.getLocation( response ) );
	}

	private static final String locationName( final int location )
	{
                if ( location < 0 || location >= LeafletManager.LOCATIONS.length )
                {
                        return "Unknown";
                }

		return LeafletManager.LOCATIONS[ location ][ 1 ];
	}

	private static final void parseLocation( final String response )
	{
                // Find out where we are in the leaflet
		LeafletManager.location = LeafletManager.getLocation( response );

		// Assume no maze exit
		LeafletManager.exit = null;

		switch ( LeafletManager.location )
		{
		case HOUSE:
			LeafletManager.fireplace = response.indexOf( "fireplace is lit" ) != -1;
			// You cannot close the door. "close door" =>
			// "You feel a sudden streak of malevolence and
			// decide to leave the door wide open. Serves
			// 'em right for not locking it."
			LeafletManager.door = true;
			break;

		case FIELD:
			LeafletManager.door = response.indexOf( "front door is closed" ) == -1;
			break;

		case PATH:
			LeafletManager.hedge = response.indexOf( "thick hedge" ) == -1;
			break;

		case CLEARING:
			LeafletManager.hedge = true;
			break;

		case CAVE:
			LeafletManager.hedge = true;
			LeafletManager.chest = response.indexOf( "empty treasure chest" ) != -1;
			LeafletManager.serpent = response.indexOf( "dangerous-looking serpent" ) == -1;
			break;

		case BANK:
			LeafletManager.fireplace = true;
			break;

		case FOREST:
			Matcher matcher = LeafletManager.FOREST_PATTERN.matcher( response );
			if ( matcher.find() )
			{
				LeafletManager.exit = matcher.group( 1 );
			}
			break;

		case BOTTOM:
			break;

		case TREE:
			LeafletManager.roadrunner = response.indexOf( "large ruby in its beak" ) == -1;
			LeafletManager.petunias = response.indexOf( "scroll entangled in the flowers" ) == -1;
			break;

		case TABLE:
			LeafletManager.giant = response.indexOf( "The Giant himself" ) == -1;
			break;

		default:
			throw new LeafletException( "Server-side change detected. Script aborted." );
		}
	}

	private static final void parseMantelpiece( final String response )
	{
		if ( response.indexOf( "brass bowling trophy" ) != -1 )
		{
			// "A brass bowling trophy sits on the mantelpiece."
			LeafletManager.magic = null;
		}
		else if ( response.indexOf( "carved driftwood bird" ) != -1 )
		{
			// "A carved driftwood bird sits on the mantelpiece."
			LeafletManager.magic = "plover";
		}
		else if ( response.indexOf( "white house" ) != -1 )
		{
			// "A ceramic model of a small white house sits on the mantelpiece."
			LeafletManager.magic = "xyzzy";
		}
		else if ( response.indexOf( "brick building" ) != -1 )
		{
			// "A ceramic model of a brick building sits on the mantelpiece."
			LeafletManager.magic = "plugh";
		}
		else if ( response.indexOf( "model ship" ) != -1 )
		{
			// "A model ship inside a bottle sits on the mantelpiece."
			LeafletManager.magic = "yoho";
		}
	}

	private static final void parseMagic( final String response )
	{
		// Bail if we didn't invoke a magic word.
		if ( LeafletManager.magic == null )
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
			LeafletManager.magic = null;
		}
	}

	private static final void getLeaflet()
	{
		// Can't go back
		if ( LeafletManager.location > LeafletManager.BANK )
		{
			return;
		}

		if ( LeafletManager.leaflet )
		{
			return;
		}

		KoLmafia.updateDisplay( "Retrieving mail..." );

		LeafletManager.goTo( LeafletManager.FIELD );

		// We can't tell if the mailbox is already open. But, there's
		// no harm in opening it twice.
		LeafletManager.executeCommand( "open mailbox" );
		LeafletManager.executeCommand( "take leaflet" );
		LeafletManager.leaflet = true;
	}

	private static final void openChest()
	{
		// Can't go back
		if ( LeafletManager.location > LeafletManager.BANK )
		{
			return;
		}

		if ( LeafletManager.chest )
		{
			return;
		}

		KoLmafia.updateDisplay( "Looking for treasure..." );

		LeafletManager.goTo( LeafletManager.CAVE );
		LeafletManager.killSerpent();

		if ( !LeafletManager.chest )
		{
			LeafletManager.executeCommand( "open chest" );
		}
	}

	private static final void robHole()
	{
		// Can't go back
		if ( LeafletManager.location > LeafletManager.BANK )
		{
			return;
		}

		KoLmafia.updateDisplay( "Hunting eggs..." );

		// We can't tell if we've already done this. But, there's no
		// harm in doing it twice.
		LeafletManager.goTo( LeafletManager.CAVE );
		LeafletManager.executeCommand( "look behind chest" );
		LeafletManager.executeCommand( "look in hole" );
	}

	// Returns true if should proceed, false if should stop now
	private static final boolean invokeMagic( boolean invokeMagic )
	{
		// Can't go back
		if ( LeafletManager.location > LeafletManager.BANK )
		{
			return true;
		}

		if ( LeafletManager.trophy )
		{
			return true;
		}

		KoLmafia.updateDisplay( "Looking for knick-knacks..." );

		LeafletManager.goTo( LeafletManager.HOUSE );
		LeafletManager.parseMantelpiece( LeafletManager.executeCommand( "examine fireplace" ) );

		if ( LeafletManager.magic == null )
		{
			LeafletManager.executeCommand( "take trophy" );
			LeafletManager.trophy = true;
			return true;
		}

		if ( !invokeMagic )
		{
			return false;
		}

		LeafletManager.parseMagic( LeafletManager.executeCommand( LeafletManager.magic ) );
		return true;
	}

	private static final void getRing()
	{
		if ( LeafletManager.ring )
		{
			return;
		}

		if ( LeafletManager.location < LeafletManager.BOTTOM )
		{
			LeafletManager.goTo( LeafletManager.BOTTOM );
		}

		KoLmafia.updateDisplay( "Finding the giant..." );
		LeafletManager.getScroll();

		if ( LeafletManager.parchment && !KoLCharacter.hasSkill( "CLEESH" ) )
		{
			LeafletManager.executeCommand( "GNUSTO CLEESH" );
			ResponseTextParser.learnSkill( "CLEESH" );
			LeafletManager.parchment = false;
			LeafletManager.scroll = false;
		}

		LeafletManager.goTo( LeafletManager.TABLE );

		if ( !LeafletManager.giant )
		{
			LeafletManager.executeCommand( "CLEESH giant" );
		}

		LeafletManager.executeCommand( "take ring" );
		LeafletManager.ring = true;
	}

	private static final void getScroll()
	{
		if ( LeafletManager.scroll )
		{
			return;
		}

		LeafletManager.goTo( LeafletManager.TREE );

		// If it's not in the bowl of petunias, we've gotten it and memorized it
		if ( LeafletManager.petunias )
		{
			return;
		}

		LeafletManager.getRuby();
		LeafletManager.goTo( LeafletManager.TREE );
		LeafletManager.executeCommand( "throw ruby at petunias" );
		LeafletManager.ruby = false;
		LeafletManager.executeCommand( "read scroll" );
	}

	private static final void getRuby()
	{
		if ( LeafletManager.ruby )
		{
			return;
		}

		LeafletManager.goTo( LeafletManager.TREE );
		if ( !LeafletManager.roadrunner )
		{
			if ( !LeafletManager.egg )
			{
				LeafletManager.executeCommand( "take egg" );
			}
			LeafletManager.executeCommand( "throw egg at roadrunner" );
			LeafletManager.egg = false;
		}

		LeafletManager.goTo( LeafletManager.BOTTOM );
		LeafletManager.executeCommand( "move leaves" );
		LeafletManager.ruby = true;
	}

	private static final void goTo( final int destination )
	{
		// If you've already reached your destination,
		// you do not need to move.

		if ( LeafletManager.location == destination )
		{
			return;
		}

		// Otherwise, get necessary items and go where
		// you need to go.

		switch ( destination )
		{
		case HOUSE:

			switch ( LeafletManager.location )
			{
			case BANK:
			case PATH:
			case CLEARING:
			case CAVE:
				LeafletManager.goTo( LeafletManager.FIELD );
				// Fall through
			case FIELD:
				LeafletManager.openDoor();
				LeafletManager.executeCommand( "east" );
				break;
			default:
				break;
			}

			break;

		case FIELD:

			switch ( LeafletManager.location )
			{
			case HOUSE:
				LeafletManager.executeCommand( "west" );
				break;
			case CLEARING:
			case CAVE:
				LeafletManager.goTo( LeafletManager.PATH );
				// Fall through
			case PATH:
				LeafletManager.executeCommand( "south" );
				break;
			case BANK:
				LeafletManager.executeCommand( "north" );
				break;
			default:
				break;
			}

			break;

		case PATH:

			switch ( LeafletManager.location )
			{
			case HOUSE:
			case BANK:
				LeafletManager.goTo( LeafletManager.FIELD );
				// Fall through
			case FIELD:
				LeafletManager.executeCommand( "north" );
				break;
			case CLEARING:
				LeafletManager.executeCommand( "east" );
				break;
			case CAVE:
				LeafletManager.executeCommand( "south" );
				break;
			default:
				break;
			}

			break;

		case CLEARING:

			LeafletManager.cutHedge();
			LeafletManager.goTo( LeafletManager.PATH );
			LeafletManager.executeCommand( "west" );
			break;

		case CAVE:

			LeafletManager.getTorch();
			LeafletManager.goTo( LeafletManager.PATH );
			LeafletManager.executeCommand( "north" );
			break;

		case BANK:

			LeafletManager.wearBoots();
			LeafletManager.goTo( LeafletManager.FIELD );
			LeafletManager.executeCommand( "south" );
			break;

		// From here on we've entered the maze and can't go back
		case FOREST:

			// No return
			if ( LeafletManager.location > LeafletManager.FOREST )
			{
				return;
			}
			LeafletManager.goTo( LeafletManager.BANK );
			LeafletManager.executeCommand( "south" );
			LeafletManager.executeCommand( "south" );
			break;

		case BOTTOM:

			switch ( LeafletManager.location )
			{
			default:
				LeafletManager.goTo( LeafletManager.FOREST );
				// Fall through
			case FOREST:
				// Stumble around until we get out
				KoLmafia.updateDisplay( "Navigating the forest..." );
				while ( LeafletManager.exit != null )
				{
					LeafletManager.executeCommand( LeafletManager.exit );
				}
				break;
			case TABLE:
				LeafletManager.goTo( LeafletManager.TREE );
				// Fall through
			case TREE:
				LeafletManager.executeCommand( "down" );
				break;
			}

			break;

		case TREE:

			switch ( LeafletManager.location )
			{
			case BOTTOM:
				LeafletManager.executeCommand( "up" );
				break;
			case TABLE:
				LeafletManager.executeCommand( "down" );
				break;
			}

			break;

		case TABLE:

			LeafletManager.goTo( LeafletManager.TREE );
			LeafletManager.executeCommand( "up" );
			break;
		}
	}

	private static final void getSword()
	{
		if ( LeafletManager.sword )
		{
			return;
		}

		LeafletManager.goTo( LeafletManager.HOUSE );
		LeafletManager.executeCommand( "take sword" );
		LeafletManager.sword = true;
	}

	private static final void getStick()
	{
		if ( LeafletManager.stick || LeafletManager.torch )
		{
			return;
		}

		LeafletManager.goTo( LeafletManager.PATH );
		LeafletManager.executeCommand( "take stick" );
		LeafletManager.stick = true;
	}

	private static final void cutHedge()
	{
		if ( LeafletManager.hedge )
		{
			return;
		}

		LeafletManager.getSword();
		LeafletManager.goTo( LeafletManager.PATH );

		if ( !LeafletManager.hedge )
		{
			LeafletManager.executeCommand( "cut hedge" );
		}
	}

	private static final void openDoor()
	{
		if ( LeafletManager.door )
		{
			return;
		}

		LeafletManager.goTo( LeafletManager.FIELD );

		if ( !LeafletManager.door )
		{
			LeafletManager.executeCommand( "open door" );
		}
	}

	private static final void getTorch()
	{
		if ( LeafletManager.torch )
		{
			return;
		}

		LeafletManager.cutHedge();

		if ( !LeafletManager.stick )
		{
			LeafletManager.getStick();
		}

		LeafletManager.goTo( LeafletManager.CLEARING );
		LeafletManager.executeCommand( "light stick" );
		LeafletManager.torch = true;
	}

	private static final void killSerpent()
	{
		if ( LeafletManager.serpent )
		{
			return;
		}

		LeafletManager.goTo( LeafletManager.CAVE );

		if ( !LeafletManager.serpent )
		{
			LeafletManager.executeCommand( "kill serpent" );
		}
	}

	private static final void wearBoots()
	{
		if ( LeafletManager.wornboots )
		{
			return;
		}

		LeafletManager.getBoots();
		LeafletManager.executeCommand( "wear boots" );
		LeafletManager.wornboots = true;
	}

	private static final void getBoots()
	{
		if ( LeafletManager.boots )
		{
			return;
		}

		LeafletManager.lightFire();
		LeafletManager.executeCommand( "take boots" );
		LeafletManager.boots = true;
	}

	private static final void lightFire()
	{
		LeafletManager.getTorch();
		LeafletManager.goTo( LeafletManager.HOUSE );

		if ( LeafletManager.fireplace )
		{
			return;
		}

		if ( !LeafletManager.parchment )
		{
			LeafletManager.executeCommand( "examine fireplace" );
			LeafletManager.executeCommand( "examine tinder" );
			LeafletManager.parchment = true;
		}

		LeafletManager.executeCommand( "light fireplace" );
		LeafletManager.fireplace = true;
	}

	private static class LeafletException
                extends RuntimeException
	{
                public LeafletException( final String s )
		{
                        super( s == null ? "" : s );
                }
        }
}
