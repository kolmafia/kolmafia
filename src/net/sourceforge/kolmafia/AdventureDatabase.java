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

import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A static class which retrieves all the adventures currently
 * available to <code>KoLmafia</code>.
 */

public class AdventureDatabase extends KoLDatabase
{
	private static LockableListModel adventures = new LockableListModel();
	private static AdventureArray allAdventures = new AdventureArray();

	public static final Map ZONE_NAMES = new TreeMap();
	public static final Map ZONE_DESCRIPTIONS = new TreeMap();

	private static StringArray [] adventureTable = new StringArray[6];
	private static final Map areaCombatData = new TreeMap();
	private static final Map adventureLookup = new TreeMap();
	private static final Map conditionLookup = new TreeMap();

	private static final Map zoneValidations = new TreeMap();

	static
	{
		for ( int i = 0; i < 6; ++i )
			adventureTable[i] = new StringArray();

		AdventureDatabase.refreshZoneTable();
		AdventureDatabase.refreshAdventureTable();
		AdventureDatabase.refreshCombatsTable();
		AdventureDatabase.refreshAdventureList();
	}

	public static final String [][][] CHOICE_ADVS =
	{
		// Lucky sewer options
		{ { "luckySewerAdventure" }, { "Sewer Gnomes" },
		  { "seal-clubbing club", "seal tooth", "helmet turtle", "scroll of turtle summoning", "pasta spoon", "ravioli hat",
		    "saucepan", "disco mask", "disco ball", "stolen accordion", "mariachi pants" } },

		// Choice 1 is unknown

		// Denim Axes Examined
		{ { "choiceAdventure2" }, { "Palindome" },
		  { "Trade for a denim axe", "Keep your rubber axe" } },

		// The Oracle Will See You Now
		// (This adventure cannot be customized)
		// { { "choiceAdventure3" }, { "Teleportitis" },
		//  { "Leave the oracle", "Pay for a minor consultation", "Pay for a major consultation" } },

		// Finger-Lickin'... Death.
		{ { "choiceAdventure4" }, { "South of the Border" },
		  { "Bet on Tapajunta Del Maiz", "Bet on Cuerno De...  the other one", "Walk away in disgust" } },

		// Heart of Very, Very Dark Darkness
		{ { "choiceAdventure5" }, { "Gravy Barrow" },
		  { "Enter the cave", "Don't enter the cave" } },

		// Choice 6 is unknown

		// How Depressing
		// (This adventure cannot be customized)
		// { { "choiceAdventure7" }, { "Gravy Barrow 2" },
		//  { "Put your hand in the depression", "Leave the cave" } },

		// On the Verge of a Dirge
		// (This adventure cannot be customized)
		// { { "choiceAdventure8" }, { "Gravy Barrow 3" },
		//  { "Enter the chamber", "Enter the chamber", "Enter the chamber" } },

		// Choice 9 is the Giant Castle Chore Wheel: Muscle position
		// Choice 10 is the Giant Castle Chore Wheel: Mysticality position
		// Choice 11 is the Giant Castle Chore Wheel: Map Quest position
		// Choice 12 is the Giant Castle Chore Wheel: Moxie position

		// Choice 13 is unknown

		// A Bard Day's Night
		{ { "choiceAdventure14" }, { "Knob Goblin Harem" },
		  { "Knob goblin harem veil", "Knob goblin harem pants", "100 meat", "Complete the outfit" },
		  { "306", "305", null } },

		// Yeti Nother Hippy
		{ { "choiceAdventure15" }, { "eXtreme Slope 1" },
		  { "eXtreme mittens", "eXtreme scarf", "200 meat", "Complete the outfit" },
		  { "399", "355", null } },

		// Saint Beernard
		{ { "choiceAdventure16" }, { "eXtreme Slope 2" },
		  { "snowboarder pants", "eXtreme scarf", "200 meat", "Complete the outfit" },
		  { "356", "355", null } },

		// Generic Teen Comedy
		{ { "choiceAdventure17" }, { "eXtreme Slope 3" },
		  { "eXtreme mittens", "snowboarder pants", "200 meat", "Complete the outfit" },
		  { "399", "356", null } },

		// A Flat Miner
		{ { "choiceAdventure18" }, { "Itznotyerzitz Mine 1" },
		  { "miner's pants", "7-Foot Dwarven mattock", "100 meat", "Complete the outfit" },
		  { "361", "362", null } },

		// 100% Legal
		{ { "choiceAdventure19" }, { "Itznotyerzitz Mine 2" },
		  { "miner's helmet", "miner's pants", "100 meat", "Complete the outfit" },
		  { "360", "361", null } },

		// See You Next Fall
		{ { "choiceAdventure20" }, { "Itznotyerzitz Mine 3" },
		  { "miner's helmet", "7-Foot Dwarven mattock", "100 meat", "Complete the outfit" },
		  { "360", "362", null } },

		// Under the Knife
		{ { "choiceAdventure21" }, { "Sleazy Back Alley" },
		  { "Switch Genders", "Umm. No thanks" } },

		// The Arrrbitrator
		{ { "choiceAdventure22" }, { "Pirate's Cove 1" },
		  { "eyepatch", "swashbuckling pants", "100 meat", "Complete the outfit" },
		  { "224", "402", null } },

		// Barrie Me at Sea
		{ { "choiceAdventure23" }, { "Pirate's Cove 2" },
		  { "stuffed shoulder parrot", "swashbuckling pants", "100 meat", "Complete the outfit" },
		  { "403", "402", null } },

		// Amatearrr Night
		{ { "choiceAdventure24" }, { "Pirate's Cove 3" },
		  { "stuffed shoulder parrot", "100 meat", "eyepatch", "Complete the outfit" },
		  { "403", null, "224" } },

		// Ouch! You bump into a door!
		{ { "choiceAdventure25" }, { "Dungeon of Doom" },
		  { "Buy a magic lamp", "Buy some sort of cloak", "Leave without buying anything" } },

		// Choice 26 is A Three-Tined Fork
		// Choice 27 is Footprints
		// Choice 28 is A Pair of Craters
		// Choice 29 is The Road Less Visible

		// Choices 30 - 39 are unknown

		// The Effervescent Fray
		{ { "choiceAdventure40" }, { "Cola Wars 1" },
		  { "Cloaca-Cola fatigues", "Dyspepsi-Cola shield", "15 Mysticality" },
		  { "1328", "1329", null } },

		// Smells Like Team Spirit
		{ { "choiceAdventure41" }, { "Cola Wars 2" },
		  { "Dyspepsi-Cola fatigues", "Cloaca-Cola helmet", "15 Muscle" },
		  { "1330", "1331", null } },

		// What is it Good For?
		{ { "choiceAdventure42" }, { "Cola Wars 3" },
		  { "Dyspepsi-Cola helmet", "Cloaca-Cola shield", "15 Moxie" },
		  { "1326", "1327", null } },

		// Choices 43 - 44 are unknown

		// Maps and Legends
		{ { "choiceAdventure45" }, { "Spooky Forest 1" },
		  { "Spooky Temple Map", "Ignore the monolith", "Nothing" } },

		// An Interesting Choice
		{ { "choiceAdventure46" }, { "Spooky Forest 2" },
		  { "Moxie", "Muscle", "Fight" } },

		// Have a Heart
		{ { "choiceAdventure47" }, { "Spooky Forest 3" },
		  { "Trade for bottle of used blood", "Keep your hearts" } },

		// Choices 48 - 70 are violet fog adventures
		// Choice 71 is A Journey to the Center of Your Mind

		// Lording Over The Flies
		{ { "choiceAdventure72" }, { "Frat House" },
		  { "Trade for around the world", "Keep your flies" } },

		// Don't Fence Me In
		{ { "choiceAdventure73" }, { "Whitey's Grove 1" },
		  { "Muscle", "white picket fence", "piece of wedding cake" },
		  { null, "270", "262" } },

		// The Only Thing About Him is the Way That He Walks
		{ { "choiceAdventure74" }, { "Whitey's Grove 2" },
		  { "Moxie", "boxed wine", "mullet wig" },
		  { null, "1005", "267" } },

		// Rapido!
		{ { "choiceAdventure75" }, { "Whitey's Grove 3" },
		  { "Mysticality", "white lightning", "white collar" },
		  { null, "266", "1655" } }
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
		{ "choiceAdventure25", "3" },

		// Maps and Legends
		{ "choiceAdventure45", "2" },

		// Have a Heart
		{ "choiceAdventure47", "2" },

		// Lording Over The Flies
		{ "choiceAdventure72", "2" },
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

	// Some adventures don't actually cost a turn
	public static final String [] FREE_ADVENTURES =
	{
		"Rock-a-bye larva",
		"Cobb's Knob lab key"
	};

	public static final void refreshZoneTable()
	{
		if ( !ZONE_NAMES.isEmpty() )
			return;

		BufferedReader reader = getReader( "zonelist.dat" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				ZONE_NAMES.put( data[0], data[1] );
				ZONE_DESCRIPTIONS.put( data[0], data[2] );

				if ( data.length > 3 )
				{
					ArrayList validationRequests = new ArrayList();
					for ( int i = 3; i < data.length; ++i )
						validationRequests.add( data[i] );

					zoneValidations.put( data[1], validationRequests );
				}
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}
	}

	public static final void refreshAdventureTable()
	{
		BufferedReader reader = getReader( "adventures.dat" );

		for ( int i = 0; i < 4; ++i )
			adventureTable[i].clear();

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length > 5 )
			{
				if ( data[1].indexOf( "send" ) != -1 )
					continue;

				String zone = (String) ZONE_NAMES.get( data[0] );

				// Be defensive: user can supply a broken data file
				if ( zone == null )
				{
					System.out.println( "Bad adventure zone: " + data[0] );
					continue;
				}

				adventureTable[0].add( zone );
				for ( int i = 1; i < 6; ++i )
					adventureTable[i].add( data[i] );

				if ( data.length == 7 )
					conditionLookup.put( data[5], data[6] );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}
	}

	public static final void refreshCombatsTable()
	{
		areaCombatData.clear();

		BufferedReader reader = getReader( "combats.dat" );
		String [] data;

		String [] adventures = adventureTable[5].toArray();

		while ( (data = readData( reader )) != null )
		{
			if ( data.length > 1 )
			{
				if ( !validateAdventureArea( data[0], adventures ) )
				{
					System.out.println( "Invalid adventure area: \"" + data[0] + "\"" );
					continue;
				}

				int combats = parseInt( data[1] );
				if ( combats != 0 )
				{
					AreaCombatData combat = new AreaCombatData( combats );
					for ( int i = 2; i < data.length; ++i )
						combat.addMonster( data[i] );
					areaCombatData.put( data[0], combat );
				}
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}
	}

	public static void refreshAdventureList()
	{
		adventures.clear();
		allAdventures.clear();
		adventureLookup.clear();

		String [] excludedZones = getProperty( "zoneExcludeList" ).split( "," );
		if ( excludedZones.length == 1 && excludedZones[0].length() == 0 )
			excludedZones[0] = "-";

		for ( int i = 0; i < adventureTable[3].size(); ++i )
			addAdventure( excludedZones, getAdventure(i) );

		if ( getBooleanProperty( "sortAdventures" ) )
			adventures.sort();
	}

	public static void addAdventure( KoLAdventure location )
	{	addAdventure( null, location );
	}

	private static void addAdventure( String [] excludedZones, KoLAdventure location )
	{
		boolean shouldAdd = true;
		String zoneName = location.getZone();

		if ( excludedZones != null )
			for ( int j = 0; j < excludedZones.length && shouldAdd; ++j )
				if ( zoneName.equals( excludedZones[j] ) )
					shouldAdd = false;

		allAdventures.add( location );
		adventureLookup.put( location.getRequest().getURLString(), location );

		if ( shouldAdd )
			adventures.add( location );
	}

	public static final boolean validateZone( String zoneName, String locationID )
	{
		if ( zoneName == null || locationID == null )
			return true;

		ArrayList validationRequests = (ArrayList) zoneValidations.get( zoneName );
		if ( validationRequests == null || validationRequests.isEmpty() )
			return true;

		boolean isValidZone = true;

		for ( int i = 1; isValidZone && i < validationRequests.size() - 1; ++i )
		{
			KoLRequest request = new KoLRequest( getClient(), (String) validationRequests.get(0) );
			request.run();

			isValidZone &= request.responseText != null &&
				request.responseText.indexOf( (String) validationRequests.get(i) ) != -1;
		}

		KoLRequest request = new KoLRequest( getClient(), (String) validationRequests.get( validationRequests.size() - 1 ) );
		request.run();

		isValidZone &= request.responseText != null;
		if ( isValidZone )
		{
			isValidZone &= request.responseText.indexOf( "snarfblat=" + locationID ) != -1 ||
				request.responseText.indexOf( "adv=" + locationID ) != -1 ||
				request.responseText.indexOf( "name=snarfblat value=" + locationID ) != -1 ||
				request.responseText.indexOf( "name=adv value=" + locationID ) != -1;
		}

		return isValidZone;
	}

	public static final LockableListModel getAsLockableListModel()
	{
		if ( adventures.isEmpty() )
			refreshAdventureList();

		return adventures;
	}

	public static KoLAdventure getAdventureByURL( String adventureURL )
	{
		if ( adventureLookup.isEmpty() )
			refreshAdventureList();

		return (KoLAdventure) adventureLookup.get( adventureURL );
	}

	public static KoLAdventure getAdventure( String adventureName )
	{
		if ( adventureLookup.isEmpty() )
			refreshAdventureList();

		return allAdventures.find( adventureName );

	}

	private static KoLAdventure getAdventure( int tableIndex )
	{
		return new KoLAdventure( getClient(),
			adventureTable[0].get( tableIndex ), adventureTable[1].get( tableIndex ),
			adventureTable[2].get( tableIndex ), adventureTable[3].get( tableIndex ),
			adventureTable[4].get( tableIndex ), adventureTable[5].get( tableIndex ) );
	}

	public static String getCondition( KoLAdventure location )
	{
		if ( location == null )
			return "none";

		String condition = (String) conditionLookup.get( location.getAdventureName() );
		return condition == null ? "none" : condition;
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
		// See if it's a free movement in the violet fog
		if ( VioletFog.freeAdventure( choice, decision ) )
			return false;

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
				return parseInt( CHOICE_MEAT_COST[i][2] );
		return 0;
	}

	public static boolean freeAdventure( String text )
	{
		for ( int i = 0; i < FREE_ADVENTURES.length; ++i )
			if ( text.indexOf( FREE_ADVENTURES[i] ) != -1 )
				return true;
		return false;
	}

	private static boolean validateAdventureArea( String area, String [] areas )
	{
		for ( int i = 0; i < areas.length; ++i )
			if ( area.equals( areas[i] ) )
			     return true;
		return false;
	}

	public static AreaCombatData getAreaCombatData( String area )
	{
		// Strip out zone name if present
		int index = area.indexOf( ":" );
		if ( index != -1 )
			area = area.substring( index + 2 );

		// Get the combat data
		return (AreaCombatData) areaCombatData.get( area );
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
			DEFAULT_SHELL.executeLine( command + " " + retrieveCount + " " + item.getName() );
			return item.getCount() - item.getCount( inventory );
		}

		return missingCount;
	}

	/**
	 * Utility method which creates an item by invoking the
	 * appropriate CLI command.
	 */

	private static final void retrieveItem( ItemCreationRequest item, int missingCount )
	{
		int createCount = Math.min( missingCount, item.getCount( ConcoctionsDatabase.getConcoctions() ) );

		if ( createCount > 0 )
		{
			DEFAULT_SHELL.executeLine( "make " + createCount + " " + item.getName() );
			return;
		}
	}

	public static final void retrieveItem( String itemName )
	{	retrieveItem( new AdventureResult( itemName, 1, false ) );
	}

	public static final void retrieveItem( AdventureResult item )
	{
		try
		{
			int missingCount = item.getCount() - item.getCount( inventory );

			// If you already have enough of the given item, then
			// return from this method.

			if ( missingCount <= 0 )
				return;

			// Next, if you have a piece of equipment, then
			// assume this might be a check on equipment.
			// in this case, return from this method.

			if ( KoLCharacter.hasEquipped( item ) )
			{
				DEFAULT_SHELL.executeLine( "unequip " + item.getName() );
				missingCount = item.getCount() - item.getCount( inventory );
			}

			if ( missingCount <= 0 )
				return;

			// First, attempt to pull the item from the closet.
			// If this is successful, return from the method.

			missingCount = retrieveItem( "closet take", closet, item, missingCount );

			if ( missingCount <= 0 )
				return;

			// Next, attempt to create the item from existing
			// ingredients (if possible).

			ItemCreationRequest creator = ItemCreationRequest.getInstance( getClient(), item.getItemID(), missingCount );
			if ( creator != null )
			{
				if ( ConcoctionsDatabase.getMixingMethod( item.getItemID() ) == ItemCreationRequest.NOCREATE ||
					ConcoctionsDatabase.hasAnyIngredient( item.getItemID() ) )
				{
					retrieveItem( creator, missingCount );
					missingCount = item.getCount() - item.getCount( inventory );

					if ( missingCount <= 0 )
						return;
				}
			}

			// Next, hermit item retrieval is possible when
			// you have worthless items.  Use this method next.

			if ( hermitItems.contains( item.getName() ) )
			{
				int worthlessItemCount = HermitRequest.getWorthlessItemCount();
				if ( worthlessItemCount > 0 )
					(new HermitRequest( getClient(), item.getItemID(), Math.min( worthlessItemCount, missingCount ) )).run();

				missingCount = item.getCount() - item.getCount( inventory );

				if ( missingCount <= 0 )
					return;
			}

			// Next, attempt to pull the items out of storage,
			// if you are out of ronin.

			if ( KoLCharacter.canInteract() )
				missingCount = retrieveItem( "hagnk", storage, item, missingCount );

			if ( missingCount <= 0 )
				return;

			// Try to purchase the item from the mall, if the
			// user wishes to autosatisfy through purchases,
			// and the item is not creatable through combines.

			int price = TradeableItemDatabase.getPriceByID( item.getItemID() );

			boolean shouldUseMall = getBooleanProperty( "autoSatisfyWithMall" );
			boolean shouldUseStash = getBooleanProperty( "autoSatisfyWithStash" );

			boolean shouldPurchase = (price != 0 && price != -1) || item.getName().indexOf( "clover" ) != -1;
			boolean canUseNPCStore = getBooleanProperty( "autoSatisfyWithNPCs" ) && NPCStoreDatabase.contains( item.getName() );

			boolean shouldAutoSatisfyEarly = canUseNPCStore || !ConcoctionsDatabase.hasAnyIngredient( item.getItemID() ) || ConcoctionsDatabase.getMixingMethod( item.getItemID() ) == ItemCreationRequest.PIXEL;

			if ( shouldPurchase && shouldAutoSatisfyEarly )
			{
				// If you should auto-satisfy early, check the clan stash to see
				// if the item is available from there.

				if ( shouldUseStash && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
				{
					if ( !ClanManager.isStashRetrieved() )
						(new ClanStashRequest( getClient() )).run();

					missingCount = retrieveItem( "stash take", ClanManager.getStash(), item, missingCount );
					if ( missingCount <= 0 )
						return;
				}

				if ( canUseNPCStore || (KoLCharacter.canInteract() && shouldUseMall) )
					missingCount = retrieveItem( "buy", null, item, missingCount );
			}

			if ( missingCount <= 0 )
				return;

			// If it's creatable, rather than seeing what main ingredient is missing,
			// show what  sub-ingredients are missing; but only do this if it's not
			// clovers or dough, which causes infinite recursion.

			if ( creator != null )
			{
				switch ( item.getItemID() )
				{
					case 24:
					case 196:
					case 159:
					case 301:

						break;

					default:

						creator.setQuantityNeeded( missingCount );
						creator.run();
						return;
				}
			}

			// See if the item can be retrieved from the clan stash.  If it can,
			// go ahead and pull as many items as possible from there.

			if ( shouldUseStash && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
			{
				if ( !ClanManager.isStashRetrieved() )
					(new ClanStashRequest( getClient() )).run();

				missingCount = retrieveItem( "stash take", ClanManager.getStash(), item, missingCount );
				if ( missingCount <= 0 )
					return;
			}

			// Try to purchase the item from the mall, if the user wishes to allow
			// purchases for item acquisition.

			if ( shouldPurchase && !shouldAutoSatisfyEarly )
			{
				if ( KoLCharacter.canInteract() && shouldUseMall )
					missingCount = retrieveItem( "buy", null, item, missingCount );
			}

			if ( missingCount <= 0 )
				return;

			// If the item does not exist in sufficient quantities,
			// then notify the user that there aren't enough items
			// available to continue and cancel the request.

			KoLmafia.updateDisplay( ERROR_STATE, "You need " + missingCount + " more " + item.getName() + " to continue." );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}
	}

	private static class AdventureArray
	{
		private ArrayList nameList = new ArrayList();
		private ArrayList internalList = new ArrayList();

		public KoLAdventure get( int index )
		{
			if ( index < 0 || index > internalList.size() )
				return null;

			return (KoLAdventure) internalList.get( index );
		}

		public void add( KoLAdventure value )
		{
			nameList.add( getCanonicalName( value.getAdventureName() ) );
			internalList.add( value );
		}

		public KoLAdventure find( String adventureName )
		{
			int adventureIndex = -1;

			int length = -1;
			int startIndex = -1;
			int minimalLength = Integer.MAX_VALUE;
			int minimalStartIndex = Integer.MAX_VALUE;

			adventureName = getCanonicalName( adventureName );

			for ( int i = 0; i < size(); ++i )
			{
				startIndex = ((String)nameList.get(i)).indexOf( adventureName );
				length = ((String)nameList.get(i)).length();

				if ( startIndex != -1 )
				{
					if ( startIndex < minimalStartIndex || (startIndex == minimalStartIndex && length < minimalLength) )
					{
						adventureIndex = i;
						minimalLength = length;
						minimalStartIndex = startIndex;
					}
				}
			}

			return adventureIndex == -1 ? null : get( adventureIndex );

		}

		public void clear()
		{
			nameList.clear();
			internalList.clear();
		}

		public int size()
		{	return internalList.size();
		}
	}
}
