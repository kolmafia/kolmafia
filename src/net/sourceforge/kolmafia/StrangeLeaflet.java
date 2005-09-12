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

public class StrangeLeaflet implements KoLConstants
{
	private static KoLmafia client;

	private static final AdventureResult LEAFLET = new AdventureResult( 520, 1 );
	private static final AdventureResult FROBOZZ = new AdventureResult( 526, 1 );

	// There are five locations within the Strange Leaflet:

	//                 Cave
	//                   ^
	//                   | N/S
	//          E/W      v
	// Clearing <-> Forest Path
	//                   ^
	//                   | N/S
	//                   v      E/W
	//               Open Field <-> Inside House

	private static final int HOUSE = 0;
	private static final int FIELD = 1;
	private static final int PATH = 2;
	private static final int CLEARING = 3;
	private static final int CAVE = 4;

	// Strings that unambiguously identify current location

	private static final String [] LOCATIONS =
	{
		"in the house",
		"open field",
		"on a forest path",
		"in a clearing",
		"dark and dank cave"
	};

	// Current location

	private static int location;

	// Item status

	private static final int UNKNOWN = -1;
	private static final int NO = 0;
	private static final int YES = 1;

	// Items we can pick up

	private static int sword;
	private static int stick;

	// Things we can manipulate

	private static int door;
	private static int hedge;
	private static int torch;
	private static int serpent;
	private static int chest;

	public static void setClient( KoLmafia client )
	{
		StrangeLeaflet.client = client;
	}

	public static void robStrangeLeaflet()
	{
		KoLRequest request;

		// See if there is anything left to do.
		request = new KoLRequest( client, "charsheet.php", true );
		request.run();

		if ( request.responseText.indexOf( "You have found everything there is to find in the Strange Leaflet." ) != -1 )
		{
			client.updateDisplay( ERROR_STATE, "You have nothing left to do in the leaflet." );
			client.cancelRequest();
			return;
		}
		// Make sure he has the Strange Leaflet

		if ( LEAFLET.getCount( client.getInventory() ) < 1 )
		{
			// Not in his inventory. Is it in the closet?
			if ( LEAFLET.getCount( client.getCloset() ) < 1 )
			{
				client.updateDisplay( ERROR_STATE, "You don't have a leaflet." );
				client.cancelRequest();
				return;
			}

			// Yes. Pull it out.

			AdventureResult [] leaflet = new AdventureResult[1];
			leaflet[0] = LEAFLET.getInstance( 1 );
			(new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, leaflet )).run();
		}

		// Deduce location and status of items and actions.

		initialize();
		if ( !client.permitsContinue() )
			return;

		// Solve the puzzles

		openChest();
		robHole();
		invokeMagic();

		client.updateDisplay( ENABLED_STATE, "Strange Leaflet robbed." );
	}

	private static void initialize()
	{
		// Find out where we are in the leaflet

		KoLRequest request = new KoLRequest( client, "text.php", true );
		request.addFormField( "justgothere", "yes" );
		request.run();

		// We know nothing about the state of the items
		sword = UNKNOWN;
		stick = UNKNOWN;
		door = UNKNOWN;
		hedge = UNKNOWN;
		torch = UNKNOWN;
		serpent = UNKNOWN;
		chest = UNKNOWN;

		// See what we can deduce from the response text
		parseLocation( request.responseText );
	}

	private static void parseLocation( String response)
	{
		for ( location = 0 ; location < LOCATIONS.length; ++location )
			if ( response.indexOf( LOCATIONS[location] ) != -1 )
				break;

		switch ( location )
		{
		case HOUSE:
			sword = response.indexOf( "ornate sword" ) != -1 ? NO : YES;
			door = YES;
			break;

		case FIELD:
			door = response.indexOf( "front door is closed" ) != -1 ? NO : YES;
			break;

		case PATH:
			stick = response.indexOf( "hefty stick" ) != -1 ? NO : YES;
			hedge = response.indexOf( "thick hedge" ) != -1 ? NO : YES;
			if ( hedge == NO )
			{
				torch = NO;
				serpent = NO;
				chest = NO;
			}
			else
			{
				door = YES;
				sword = YES;
			}
			break;

		case CLEARING:
			sword = YES;
			door = YES;
			hedge = YES;
			break;

		case  CAVE:
			sword = YES;
			stick = YES;
			door = YES;
			hedge = YES;
			torch = YES;
			chest =	 response.indexOf( "empty treasure chest" ) == -1 ? NO : YES;
			serpent = response.indexOf( "dangerous-looking serpent" ) != -1 ? NO : YES;
			break;

		default:
			client.updateDisplay( ERROR_STATE, "I can't figure out where you are!" );
			client.cancelRequest();
			break;
		}
	}

	private static void openChest()
	{
		if (chest == YES)
			return;

		client.updateDisplay( DISABLED_STATE, "Looking for treasure..." );

		goTo( CAVE );
		killSerpent();
		if (chest != YES)
		{
			executeCommand( "open chest" );

			// Can't parse "Frobozz Real-Estate Company Instant House (TM)". Do it by hand.
			client.processResult( FROBOZZ );
		}
	}

	private static void robHole()
	{
		client.updateDisplay( DISABLED_STATE, "Hunting eggs..." );
		executeCommand( "look behind chest" );
		executeCommand( "look in hole" );
	}

	private static void invokeMagic()
	{
		client.updateDisplay( DISABLED_STATE, "Invoking magic..." );
		executeCommand( "plugh" );
	}

	private static void goTo( int destination )
	{
		// Bail immediately if we're already there
	       if ( location == destination )
			return;

	       KoLRequest request;

	       // Otherwise, get necessary items and go where you need to go
	       switch ( destination )
	       {
	       case HOUSE:
		       switch ( location )
		       {
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
		       default:
			       break;
		       }
		       break;

	       case PATH:
		       switch ( location )
		       {
		       case HOUSE:
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
	       }
	}

	private static void getSword()
	{
		if (sword == YES)
			return;

		goTo( HOUSE );
		if (sword != YES )
			executeCommand( "take sword" );
	}

	private static void getStick()
	{
		if (stick == YES)
			return;

		goTo( PATH );
		if (stick != YES)
			executeCommand( "take stick" );
	}

	private static void cutHedge()
	{
		if (hedge == YES)
                        return;

                getSword();
                goTo( PATH );
		if (hedge != YES)
			executeCommand( "cut hedge" );
	}

	private static void openDoor()
	{
		if (door == YES)
                        return;

                goTo( FIELD );
		if (door != YES)
                        executeCommand( "open door" );
	}

	private static void getTorch()
	{
		if (torch == YES)
			return;

		cutHedge();
		getStick();
		goTo( CLEARING );
		executeCommand( "light stick" );
		torch = YES;
	}

	private static void killSerpent()
	{
		if (serpent == YES)
			return;

		goTo( CAVE );
		if (serpent != YES)
			executeCommand( "kill serpent" );
	}

	private static void executeCommand( String command )
	{
		KoLRequest request = new KoLRequest( client, "text.php", true );
		request.addFormField( "pwd", client.getPasswordHash() );
		request.addFormField( "command", command );
		request.run();
		client.processResults( request.responseText );

		// Deduce status of items and objects
		parseLocation( request.responseText );
	}
}
