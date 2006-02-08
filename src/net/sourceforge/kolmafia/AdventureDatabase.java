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

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A static class which retrieves all the adventures currently
 * available to <code>KoLmafia</code>.
 */

public class AdventureDatabase extends KoLDatabase
{
	private static LockableListModel adventures = new LockableListModel();

	private static final AdventureResult CASINO = new AdventureResult( 40, 1 );
	private static final AdventureResult DINGHY = new AdventureResult( 141, 1 );
	private static final AdventureResult SOCK = new AdventureResult( 609, 1 );
	private static final AdventureResult ROWBOAT = new AdventureResult( 653, 1 );
	private static final AdventureResult BEAN = new AdventureResult( 186, 1 );

	public static final Map ZONE_NAMES = new TreeMap();
	public static final Map ZONE_DESCRIPTIONS = new TreeMap();

	static
	{
		BufferedReader reader = getReader( "zonelist.dat" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 3 )
			{
				ZONE_NAMES.put( data[0], data[1] );
				ZONE_DESCRIPTIONS.put( data[0], data[2] );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}

		AdventureDatabase.refreshTable();
	}

	public static final String [] CLOVER_ADVS =
	{
		"11",  // Cobb's Knob Outskirts
		"40",  // Cobb's Knob Kitchens
		"41",  // Cobb's Knob Treasury
		"42",  // Cobb's Knob Harem
		"16",  // The Haiku Dungeon
		"10",  // The Haunted Pantry
		"19",  // The Limerick Dungeon
		"70",  // The Casino Roulette Wheel
		"9",   // The Sleazy Back Alley
		"15",  // The Spooky Forest
		"17",  // The Hidden Temple
		"27",  // The Orcish Frat House (no disguise)
		"29",  // The Orcish Frat House (in disguise)
		"26",  // Hippy Camp (no disguise)
		"65",  // Hippy Camp (in disguise)
	};

	public static final String [][][] CHOICE_ADVS =
	{
		// Lucky sewer options
		{ { "luckySewerAdventure" }, { "Sewer Gnomes" },
		  { "seal-clubbing club", "seal tooth", "helmet turtle", "scroll of turtle summoning", "pasta spoon", "ravioli hat",
		    "saucepan", "disco mask", "disco ball", "stolen accordion", "mariachi pants" } },

		// Denim Axes Examined
		{ { "choiceAdventure2" }, { "Palindome" },
		  { "Trade for a denim axe", "Keep your rubber axe" } },

		// The Oracle Will See You Now
		{ { "choiceAdventure3" }, { "Teleportitis" },
		  { "Leave the oracle", "Pay for a minor consultation", "Pay for a major consultation" } },

		// Finger-Lickin'... Death.
		{ { "choiceAdventure4" }, { "South of the Border" },
		  { "Bet on Tapajunta Del Maiz", "Bet on Cuerno De...  the other one", "Walk away in disgust" } },

		// Heart of Very, Very Dark Darkness
		{ { "choiceAdventure5" }, { "Gravy Barrow 1" },
		  { "Enter the cave", "Don't enter the cave" } },

		// How Depressing
		{ { "choiceAdventure7" }, { "Gravy Barrow 2" },
		  { "Put your hand in the depression", "Leave" } },

		// On the Verge of a Dirge
		{ { "choiceAdventure8" }, { "Gravy Barrow 3" },
		  { "Enter the chamber", "Enter the chamber", "Enter the chamber" } },

                // A Bard Day's Night
		{ { "choiceAdventure14" }, { "Knob Goblin Harem" },
		  { "Knob goblin harem veil", "Knob goblin harem pants", "100 meat" } },

		// Yeti Nother Hippy
		{ { "choiceAdventure15" }, { "eXtreme Slope 1" },
		  { "eXtreme mittens", "eXtreme scarf", "75 meat" } },

		// Saint Beernard
		{ { "choiceAdventure16" }, { "eXtreme Slope 2" },
		  { "snowboarder pants", "eXtreme scarf", "75 meat" } },

		// Generic Teen Comedy
		{ { "choiceAdventure17" }, { "eXtreme Slope 3" },
		  { "eXtreme mittens", "snowboarder pants", "75 meat" } },

		// A Flat Miner
		{ { "choiceAdventure18" }, { "Itznotyerzitz Mine 1" },
		  { "miner's pants", "7-Foot Dwarven mattock", "100 meat" } },

		// 100% Legal
		{ { "choiceAdventure19" }, { "Itznotyerzitz Mine 2" },
		  { "miner's helmet", "miner's pants", "100 meat" } },

		// See You Next Fall
		{ { "choiceAdventure20" }, { "Itznotyerzitz Mine 3" },
		  { "miner's helmet", "7-Foot Dwarven mattock", "100 meat" } },

		// Under the Knife
		{ { "choiceAdventure21" }, { "Sleazy Back Alley" },
		  { "Switch Genders", "Umm. No thanks" } },

		// The Arrrbitrator
		{ { "choiceAdventure22" }, { "Pirate's Cove 1" },
		  { "eyepatch", "swashbuckling pants", "100 meat" } },

		// Barrie Me at Sea
		{ { "choiceAdventure23" }, { "Pirate's Cove 2" },
		  { "stuffed shoulder parrot", "swashbuckling pants", "100 meat" } },

		// Amatearrr Night
		{ { "choiceAdventure24" }, { "Pirate's Cove 3" },
		  { "stuffed shoulder parrot", "100 meat", "eyepatch" } },

		// Ouch! You bump into a door!
		{ { "choiceAdventure25" }, { "Dungeon of Doom" },
		  { "Buy a magic lamp", "Buy some sort of cloak", "Leave without buying anything" } },

		// The Effervescent Fray
		{ { "choiceAdventure40" }, { "Cola Wars 1" },
		  { "Cloaca-Cola fatigues", "Dyspepsi-Cola shield", "15 Mysticality" } },

		// Smells Like Team Spirit
		{ { "choiceAdventure41" }, { "Cola Wars 2" },
		  { "Dyspepsi-Cola fatigues", "Cloaca-Cola helmet", "15 Muscle" } },

		// What is it Good For?
		{ { "choiceAdventure42" }, { "Cola Wars 3" },
		  { "Dyspepsi-Cola helmet", "Cloaca-Cola shield", "15 Moxie" } }
	};

	// Some choice adventures have a choice that behaves as an "ignore"
	// setting: if you select it, no adventure is consumed.

	public static final String [][] IGNORABLE_CHOICES =
	{
		// Denim Axes Examined
		{ "choiceAdventure2", "2" },

		// The Oracle Will See You Now
		{ "choiceAdventure3", "3" },

		// Finger-Lickin'... Death.
		{ "choiceAdventure4", "3" },

		// Heart of Very, Very Dark Darkness
		{ "choiceAdventure5", "2" },

		// How Depressing
		{ "choiceAdventure7", "2" },

		// Wheel in the Clouds in the Sky, Keep on Turning
		{ "choiceAdventure9", "3" },
		{ "choiceAdventure10","3" },
		{ "choiceAdventure11", "3" },
		{ "choiceAdventure12", "3" },

		// Under the Knife
		{ "choiceAdventure21", "2" },

		// Ouch! You bump into a door!
		{ "choiceAdventure25", "3" }
	};

	// Some choice adventures have options that cost meat

	public static final String [][] CHOICE_MEAT_COST =
	{
		// Finger-Lickin'... Death.
		{ "choiceAdventure4", "1", "500" },
		{ "choiceAdventure4", "2", "500" },

		// Under the Knife
		{ "choiceAdventure21", "1", "500" },

		// Ouch! You bump into a door!
		{ "choiceAdventure25", "1", "50" },
		{ "choiceAdventure25", "2", "5000" }
	};

	private static List [] adventureTable;

	public static final void refreshTable()
	{
		BufferedReader reader = getReader( "adventures.dat", true );

		adventureTable = new ArrayList[4];
		for ( int i = 0; i < 4; ++i )
			adventureTable[i] = new ArrayList();

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 4 )
			{
				Object zone = ZONE_NAMES.get( data[0] );

				// Be defensive: user can supply a broken data file
				if ( zone == null)
				{
					System.out.println( "Bad adventure zone: " + data[0] );
					continue;
				}

				adventureTable[0].add( zone );
				for ( int i = 1; i < 4; ++i )
					adventureTable[i].add( data[i] );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	/**
	 * Returns the complete list of adventures available to the character
	 * based on the information provided by the given client.  Each element
	 * in this list is a <code>KoLAdventure</code> object.
	 */

	public static final LockableListModel getAsLockableListModel()
	{
		String [] zones = getProperty( "zoneExcludeList" ).split( "," );
		if ( zones.length == 1 && zones[0].length() == 0 )
			zones[0] = "-";

		boolean shouldAdd = true;
		String zoneName;

		adventures.clear();

		for ( int i = 0; i < adventureTable[1].size(); ++i )
		{
			shouldAdd = true;
			zoneName = (String) adventureTable[0].get(i);

			for ( int j = 0; j < zones.length && shouldAdd; ++j )
				if ( zoneName.equals( zones[j] ) )
					shouldAdd = false;

			if ( shouldAdd )
				adventures.add( new KoLAdventure( client, zoneName,
					(String) adventureTable[1].get(i), (String) adventureTable[2].get(i), (String) adventureTable[3].get(i) ) );
		}

		if ( getProperty( "sortAdventures" ).equals( "true" ) )
			java.util.Collections.sort( adventures );

		return adventures;
	}

	/**
	 * Returns the first adventure in the database which contains the given
	 * substring in part of its name.
	 */

	public static KoLAdventure getAdventure( String adventureName )
	{
		List adventureNames = adventureTable[3];

		for ( int i = 0; i < adventureNames.size(); ++i )
			if ( ((String) adventureNames.get(i)).toLowerCase().indexOf( adventureName.toLowerCase() ) != -1 )
				return new KoLAdventure( client, (String) adventureTable[0].get(i), (String) adventureTable[1].get(i),
					(String) adventureTable[2].get(i), (String) adventureTable[3].get(i) );

		return null;
	}

	/**
	 * Checks the map location of the given zone.  This is to ensure that
	 * KoLmafia arms any needed flags (such as for the beanstalk).
	 */

	public static void validateAdventure( KoLAdventure adventure )
	{
		KoLRequest request = null;

		String adventureID = adventure.getAdventureID();
		String zone = adventure.getZone();

		// The beach is unlocked provided the player has the meat car
		// accomplishment and a meatcar in inventory.

		if ( zone.equals( "Beach" ) )
		{
			// Make sure the car is in the inventory
			retrieveItem( ConcoctionsDatabase.CAR );

			if ( client.permitsContinue() )
			{
				// Sometimes, the player has just built the meatcar
				// and visited the council -- check the main map to
				// see if the beach is unlocked.

				client.updateDisplay( NORMAL_STATE, "Validating map location..." );
				request = new KoLRequest( client, "main.php" );
				request.run();

				if ( request.responseText.indexOf( "beach.php" ) == -1 )
				{
					client.updateDisplay( ERROR_STATE, "Beach is not yet unlocked." );
					client.cancelRequest();
					return;
				}

				request = null;
			}
		}

		else if ( zone.equals( "McLarge" ) )
		{
			client.updateDisplay( NORMAL_STATE, "Validating map location..." );
			// See if we can get to the location already
			request = new KoLRequest( client, "mclargehuge.php" );
			request.run();
			if ( request.responseText.indexOf( adventureID ) != -1 )
				return;

			// No. See if the trapper will give it to us
			request = new KoLRequest( client, "trapper.php" );
			request.run();

			// See if we can now get to the location
			request = new KoLRequest( client, "mclargehuge.php" );
		}

		// The casino is unlocked provided the player
		// has a casino pass in their inventory.

		else if ( zone.equals( "Casino" ) )
		{
			retrieveItem( CASINO );
		}

		// The island is unlocked provided the player
		// has a dingy dinghy in their inventory.

		else if ( zone.equals( "Island" ) )
		{
			retrieveItem( DINGHY );
		}

		// The Castle in the Clouds in the Sky is unlocked provided the
		// character has either a S.O.C.K. or an intragalactic rowboat

		else if ( adventureID.equals( "82" ) )
		{
			if ( KoLCharacter.hasItem( ROWBOAT, false ) )
			{
				retrieveItem( ROWBOAT );
				return;
			}
			retrieveItem( SOCK );
		}

		// The Hole in the Sky is unlocked provided the player has an
		// intragalactic rowboat in their inventory.

		else if ( adventureID.equals( "83" ) )
		{
			retrieveItem( ROWBOAT );
		}

		// The beanstalk is unlocked when the player
		// has planted a beanstalk -- but, the zone
		// needs to be armed first.

		else if ( adventureID.equals( "81" ) && !KoLCharacter.beanstalkArmed() )
		{
			// If the character has a S.O.C.K. or an intragalactic
			// rowboat, they can get to the airship
			if ( KoLCharacter.hasItem( SOCK, false ) || KoLCharacter.hasItem( ROWBOAT, false ) )
				return;

			request = new KoLRequest( client, "plains.php" );
			request.run();

			if ( request.responseText.indexOf( "beanstalk.php" ) == -1 )
			{
				// If not, check to see if the player has an enchanted
				// bean which can be used.  If they don't, then try to
				// find one through adventuring.

				if ( !KoLCharacter.hasItem( BEAN, false ) )
				{
					ArrayList temporary = new ArrayList();
					temporary.addAll( client.getConditions() );

					client.getConditions().clear();
					client.getConditions().add( BEAN );

					KoLAdventure beanbat = AdventureDatabase.getAdventure( "beanbat" );
					client.makeRequest( beanbat, KoLCharacter.getAdventuresLeft() );

					if ( !client.getConditions().isEmpty() )
					{
						client.updateDisplay( ERROR_STATE, "Unable to complete enchanted bean quest." );
						client.getConditions().clear();
						client.getConditions().addAll( temporary );
						return;
					}

					client.getConditions().clear();
					client.getConditions().addAll( temporary );
				}

				// Now that you've retrieved the bean, ensure that
				// it is in your inventory, and then use it.  Take
				// advantage of item consumption automatically doing
				// what's needed in grabbing the item.

				(new ConsumeItemRequest( client, BEAN )).run();
			}

			request = new KoLRequest( client, "beanstalk.php" );
			KoLCharacter.armBeanstalk();
		}

		// If you do not need to arm anything, then
		// return from this method.

		if ( request == null )
			return;

		client.updateDisplay( NORMAL_STATE, "Validating map location..." );
		request.run();

		// Now that the zone is armed, check to see
		// if the adventure is even available.  If
		// it's not, cancel the request before it's
		// even made to minimize server hits.

		if ( request.responseText.indexOf( adventure.getAdventureID() ) == -1 )
		{
			client.updateDisplay( ERROR_STATE, "This adventure is not yet unlocked." );
			client.cancelRequest();
			return;
		}
	}

	/**
	 * Utility method which retrieves an item by calling the
	 * given CLI command.
	 */

	private static final void retrieveItem( String command )
	{
		boolean shouldContinue = client.permitsContinue();
		DEFAULT_SHELL.executeLine( command );

		if ( !shouldContinue )
			client.cancelRequest();
	}

	/**
	 * Utility method which retrieves an item by calling a CLI
	 * command, which is constructed based on the parameters.
	 */

	private static final int retrieveItem( String command, LockableListModel source, AdventureResult item, int missingCount )
	{
		int retrieveCount = source == null ? missingCount : Math.min( missingCount, item.getCount( source ) );

		if ( retrieveCount > 0 )
		{
			retrieveItem( command + " " + retrieveCount + " " + item.getName() );
			return item.getCount() - item.getCount( KoLCharacter.getInventory() );
		}

		return missingCount;
	}

	/**
	 * Utility method which creates an item by invoking the
	 * appropriate CLI command.
	 */

	private static final void retrieveItem( ItemCreationRequest item, boolean validate, int missingCount )
	{
		int createCount = Math.min( missingCount, validate ? item.getCount( ConcoctionsDatabase.getConcoctions() ) : Integer.MAX_VALUE );

		if ( createCount > 0 )
		{
			retrieveItem( "make " + createCount + " " + item.getName() );
			return;
		}
	}

	public static synchronized final void retrieveItem( AdventureResult item )
	{
		try
		{
			int missingCount = item.getCount() - item.getCount( KoLCharacter.getInventory() );

			// If you already have enough of the given item, then
			// return from this method.

			if ( missingCount <= 0 )
				return;

			// First, attempt to pull the item from the closet.
			// If this is successful, return from the method.

			missingCount = retrieveItem( "closet take", KoLCharacter.getCloset(), item, missingCount );

			if ( missingCount <= 0 )
				return;

			// Next, attempt to create the item from existing
			// ingredients (if possible).

			ItemCreationRequest creator = ItemCreationRequest.getInstance( client, item.getItemID(), item.getCount() );
			if ( creator != null )
			{
				retrieveItem( creator, true, missingCount );
				missingCount = item.getCount() - item.getCount( KoLCharacter.getInventory() );

				if ( missingCount <= 0 )
					return;
			}

			// Next, if you have a piece of equipment, then attempt to
			// remove it.

			if ( KoLCharacter.hasEquipped( item ) )
			{
				retrieveItem( "unequip " + item.getName() );
				missingCount = item.getCount() - item.getCount( KoLCharacter.getInventory() );

				if ( missingCount <= 0 )
					return;
			}

			// Next, attempt to pull the items out of storage,
			// if you are out of ronin.

			if ( KoLCharacter.canInteract() )
				missingCount = retrieveItem( "hagnk", KoLCharacter.getStorage(), item, missingCount );

			if ( missingCount <= 0 )
				return;

			// Try to purchase the item from the mall, if the
			// user wishes to autosatisfy through purchases,
			// and the item is not made through combination.

			if ( client.permitsContinue() && getProperty( "autoSatisfyChecks" ).equals( "true" ) && (creator == null || ConcoctionsDatabase.getMixingMethod( creator.getItemID() ) != ItemCreationRequest.COMBINE) )
				missingCount = retrieveItem( "buy", null, item, missingCount );

			if ( missingCount <= 0 )
				return;

			// Finally, if it's creatable, rather than seeing
			// what main ingredient is missing, show what
			// sub-ingredients are missing.

			if ( creator != null )
			{
				retrieveItem( creator, false, missingCount );
				return;
			}

			// Try to purchase the item from the mall, if the
			// user wishes to autosatisfy through purchases.

			if ( client.permitsContinue() && getProperty( "autoSatisfyChecks" ).equals( "true" ) )
				missingCount = retrieveItem( "buy", null, item, missingCount );

			if ( missingCount <= 0 )
				return;

			// If the item does not exist in sufficient quantities,
			// then notify the client that there aren't enough items
			// available to continue and cancel the request.

			client.updateDisplay( ERROR_STATE, "You need " + missingCount + " more " + item.getName() + " to continue." );
			client.cancelRequest();
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	public static String ignoreChoiceOption( String choice )
	{
		for ( int i = 0; i < IGNORABLE_CHOICES.length; ++i )
			if ( choice.equals( IGNORABLE_CHOICES[i][0] ) )
				return IGNORABLE_CHOICES[i][1];
		return null;
	}

	public static boolean consumesAdventure( String choice, String decision )
	{
		for ( int i = 0; i < IGNORABLE_CHOICES.length; ++i )
			if ( choice.equals( IGNORABLE_CHOICES[i][0] ) )
				return !decision.equals( IGNORABLE_CHOICES[i][1] );
		return true;
	}

	public static int consumesMeat( String choice, String decision )
	{
		for ( int i = 0; i < CHOICE_MEAT_COST.length; ++i )
			if ( choice.equals( CHOICE_MEAT_COST[i][0] ) &&
			     decision.equals( CHOICE_MEAT_COST[i][1] ) )
				return Integer.parseInt( CHOICE_MEAT_COST[i][2] );
		return 0;
	}
}
