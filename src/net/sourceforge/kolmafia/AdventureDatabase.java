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
		"72",  // The Casino Money Making Game
		"9",   // The Sleazy Back Alley
		"15",  // The Spooky Forest
		"17",  // The Hidden Temple
		"29"   // The Orcish Frat House (in disguise)
	};

	public static final String [] SEWER_OPTIONS =
	{ "seal-clubbing club", "seal tooth", "helmet turtle", "scroll of turtle summoning", "pasta spoon", "ravioli hat",
	  "saucepan", "spices", "disco mask", "disco ball", "stolen accordion", "mariachi pants", "worthless trinket" };

	public static final String [][][] CHOICE_ADVS =
	{
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

	static
	{
		BufferedReader reader = getReader( "adventures.dat" );
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
			if ( !KoLCharacter.hasAccomplishment( KoLCharacter.MEATCAR ) )
			{
				// Sometimes, the player has just built the meatcar
				// and visited the council -- check the main map to
				// see if the beach is unlocked.

				client.updateDisplay( DISABLE_STATE, "Validating map location..." );
				request = new KoLRequest( client, "main.php" );
				request.run();

				if ( request.responseText.indexOf( "beach.php" ) == -1 )
				{
					client.updateDisplay( ERROR_STATE, "Beach is not yet unlocked." );
					client.cancelRequest();
					return;
				}

				KoLCharacter.addAccomplishment( KoLCharacter.MEATCAR );
				request = null;
			}

			// Make sure the car is in the inventory
			retrieveItem( ConcoctionsDatabase.CAR );
		}

		else if ( zone.equals( "McLarge" ) )
		{
			// For Mt. McLargeHuge, all you need to do is visit
			// the trapper in order to arm the location.

			if ( !KoLCharacter.hasAccomplishment( KoLCharacter.ICY_PEAK ) )
			{
				client.updateDisplay( DISABLE_STATE, "Validating map location..." );

				request = new KoLRequest( client, "trapper.php" );
				request.run();

				request = new KoLRequest( client, "mclargehuge.php" );
			}
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
		// player has a S.O.C.K. or intragalactic rowboat in their
		// inventory.

		else if ( adventureID.equals( "82" ) )
		{
			// If he has a rowboat in the closet, fetch it.
			if ( ROWBOAT.getCount( KoLCharacter.getCloset() ) > 0 )
				retrieveItem( ROWBOAT );

			// If he doesn't have a rowboat in inventory, he must
			// have a S.O.C.K.
			else if ( ROWBOAT.getCount( KoLCharacter.getInventory() ) == 0 )
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
			// If the player has either the S.O.C.K. or the
			// rowboat, we deduce that the beanstalk is unlocked

			if ( ROWBOAT.getCount( KoLCharacter.getInventory() ) == 0 &&
			     ROWBOAT.getCount( KoLCharacter.getCloset() ) == 0 &&
			     SOCK.getCount( KoLCharacter.getInventory() ) == 0 &&
			     SOCK.getCount( KoLCharacter.getCloset() ) == 0 )
			{
				if ( !KoLCharacter.hasAccomplishment( KoLCharacter.BEANSTALK ) )
				{
					// Sometimes, the player has just used
					// the enchanted bean, and therefore
					// does not have the accomplishment.
					// Check the map, and if the beanstalk
					// is available, go ahead and update
					// the accomplishments.

					client.updateDisplay( DISABLE_STATE, "Validating map location..." );
					request = new KoLRequest( client, "plains.php" );
					request.run();

					if ( request.responseText.indexOf( "beanstalk.php" ) == -1 )
					{
						client.updateDisplay( ERROR_STATE, "Beanstalk is not yet unlocked." );
						client.cancelRequest();
						return;
					}

					KoLCharacter.addAccomplishment( KoLCharacter.BEANSTALK );
				}

				request = new KoLRequest( client, "beanstalk.php" );
				KoLCharacter.armBeanstalk();
			}
		}

		// If you do not need to arm anything, then
		// return from this method.

		if ( request == null )
			return;

		client.updateDisplay( DISABLE_STATE, "Validating map location..." );
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

	public static final void retrieveItem( AdventureResult item )
	{
		int missingCount = item.getCount() - item.getCount( KoLCharacter.getInventory() );

		// If there are already enough items in the player's
		// inventory, then return.

		if ( missingCount <= 0 )
			return;

		String itemName = item.getName();
		int closetCount = item.getCount( KoLCharacter.getCloset() );

		// If there are some items which are sitting in the
		// player's closet, then pull them out.

		if ( closetCount > 0 )
		{
			AdventureResult [] itemArray = new AdventureResult[1];
			itemArray[0] = item.getInstance( Math.min( missingCount, closetCount ) );
			(new ItemStorageRequest( client, ItemStorageRequest.CLOSET_TO_INVENTORY, itemArray )).run();
		}

		missingCount = item.getCount() - item.getCount( KoLCharacter.getInventory() );

		// If there are now enough items in the player's inventory
		// after pulling them from the closet, then return.

		if ( missingCount <= 0 )
			return;

		// Now, check to see if the items can be created from the
		// ingredients in the player's inventory.

		ItemCreationRequest creation = ItemCreationRequest.getInstance( client, item.getItemID(), missingCount );
		if ( creation != null )
		{
			int creationCount = creation.getCount( ConcoctionsDatabase.getConcoctions() );

			// If the item can be created, then go ahead and try
			// to create the item.

			if ( creationCount > 0 )
				ItemCreationRequest.getInstance( client, item.getItemID(), Math.min( creationCount, missingCount ) ).run();
		}

		missingCount = item.getCount() - item.getCount( KoLCharacter.getInventory() );

		if ( missingCount <= 0 )
			return;

		// If the character is ready to interact with other players,
		// then they have infinite pulls.  Go ahead and pull the item
		// that's needed from storage.

		if ( KoLCharacter.canInteract() )
		{
			int storageCount = item.getCount( KoLCharacter.getStorage() );

			// Execute the needed command to pull the item from storage,
			// rather than attempt to instantiate a new storage request.

			if ( storageCount > 0 )
			{
				try
				{
					KoLmafiaCLI purchaser = new KoLmafiaCLI( client, System.in );
					purchaser.executeLine( "hagnk " + Math.min( storageCount, missingCount ) + " " + item.getName() );
				}
				catch ( Exception e )
				{
					// This should not happen, so go
					// ahead and ignore it.
				}
			}
		}

		missingCount = item.getCount() - item.getCount( KoLCharacter.getInventory() );

		if ( missingCount <= 0 )
			return;

		// If all else fails, attempt to make a purchase from the
		// mall.  Note that this is only possible if the character
		// can interact with other players.

		if ( KoLCharacter.canInteract() || NPCStoreDatabase.contains( item.getName() ) )
		{
			try
			{
				KoLmafiaCLI purchaser = new KoLmafiaCLI( client, System.in );
				purchaser.executeLine( "buy " + missingCount + " " + item.getName() );
			}
			catch ( Exception e )
			{
				// This should not happen, so go
				// ahead and ignore it.
			}
		}

		missingCount = item.getCount() - item.getCount( KoLCharacter.getInventory() );

		if ( missingCount <= 0 )
			return;

		// If the item does not exist in sufficient quantities,
		// then notify the client that there aren't enough items
		// available to continue adventuring.

		client.updateDisplay( ERROR_STATE, "You need " + missingCount + " more " + itemName + " to continue." );
		client.cancelRequest();
		return;
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
