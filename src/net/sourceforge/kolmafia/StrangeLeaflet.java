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
import javax.swing.JOptionPane;

public abstract class StrangeLeaflet extends StaticEntity
{
	private static final AdventureResult LEAFLET = new AdventureResult( 520, 1 );

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

	// Items we can pick up

	private static boolean sword;
	private static boolean stick;

	// Things we can manipulate

	private static boolean door;
	private static boolean hedge;
	private static boolean torch;
	private static boolean serpent;
	private static boolean chest;

	public static void robStrangeLeaflet()
	{	robStrangeLeaflet( false );
	}

	public static void robStrangeLeaflet( boolean invokeMagic )
	{

		// If the player has never ascended, then they're going
		// to have to do it all by hand.

		if ( KoLCharacter.getAscensions() < 1 )
		{
			client.updateDisplay( ERROR_STATE, "Sorry, you've never ascended." );
			client.cancelRequest();
			return;
		}

		// Make sure the player has the Strange Leaflet.
		// The item will be located inside of the inventory

		if ( LEAFLET.getCount( KoLCharacter.getInventory() ) < 1 )
		{
			// Not in the inventory. Is it in the closet?
			if ( LEAFLET.getCount( KoLCharacter.getCloset() ) < 1 )
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

		// Deduce location and status of items and actions
		// by initializing the leaflet variables.

		initialize();

		if ( !client.permitsContinue() )
			return;

		// Solve the puzzles.  In order, you are trying to
		// open the chest, take the grue egg from the hole
		// behind the chest, and use the "magic phrase".

		openChest();
		robHole();

		if ( client instanceof KoLmafiaGUI )
			invokeMagic = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
				"Would you like to invoke the \"magic words\" today?", "You know you want to!", JOptionPane.YES_NO_OPTION );

		if ( invokeMagic )
			invokeMagic();

		// Add new items to "Usable" list
		KoLCharacter.refreshCalculatedLists();
		client.updateDisplay( ENABLE_STATE, "Strange Leaflet robbed." );
	}

	private static void initialize()
	{
		// We know nothing about the state of the items,
		// so assume that we have nothing for now.

		sword = false;
		stick = false;
		door = false;
		hedge = false;
		torch = false;
		serpent = false;
		chest = false;

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
				sword = response.indexOf( "ornate sword" ) == -1;
				door = true;
				break;

			case FIELD:
				door = response.indexOf( "front door is closed" ) == -1;
				break;

			case PATH:
				stick = response.indexOf( "hefty stick" ) == -1;
				hedge = response.indexOf( "thick hedge" ) == -1;

				if ( !hedge )
				{
					torch = false;
					serpent = false;
					chest = false;
				}
				else
				{
					door = true;
					sword = true;
				}
				break;

			case CLEARING:
				sword = true;
				door = true;
				hedge = true;
				break;

			case CAVE:
				sword = true;
				stick = true;
				door = true;
				hedge = true;
				torch = true;
				chest =	 response.indexOf( "empty treasure chest" ) != -1;
				serpent = response.indexOf( "dangerous-looking serpent" ) == -1;
				break;

			default:
				client.updateDisplay( ERROR_STATE, "I can't figure out where you are!" );
				client.cancelRequest();
				break;
		}
	}

	private static void openChest()
	{
		if ( chest )
			return;

		client.updateDisplay( DISABLE_STATE, "Looking for treasure..." );

		goTo( CAVE );
		killSerpent();

		if ( !chest )
			executeCommand( "open chest" );
	}

	private static void robHole()
	{
		client.updateDisplay( DISABLE_STATE, "Hunting eggs..." );
		executeCommand( "look behind chest" );
		executeCommand( "look in hole" );
	}

	private static void invokeMagic()
	{
		client.updateDisplay( DISABLE_STATE, "Invoking magic..." );
		executeCommand( "plugh" );
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

	private static void executeCommand( String command )
	{
		KoLRequest request = new KoLRequest( client, "text.php", true );
		request.addFormField( "pwd", client.getPasswordHash() );
		request.addFormField( "command", command );
		request.run();

		// The inventory command is handled specially because
		// it resets the state of your items.  For now, only
		// the strings for the sword and torch are known;
		// later, check for the hefty stick text.

		if ( command.equals( "inv" ) )
		{
			sword = request.responseText.indexOf( "An ornate sword" ) != -1;
			torch = request.responseText.indexOf( "A burning torch" ) != -1;
			stick = torch || request.responseText.indexOf( "A hefty stick" ) != -1;
		}

		// Deduce status of items and objects based on your
		// current location.

		parseLocation( request.responseText );
	}
}
