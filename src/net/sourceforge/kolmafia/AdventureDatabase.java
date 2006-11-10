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
	public static final KoLRequest ZONE_VALIDATOR = new KoLRequest( "main.php", true );

	private static LockableListModel adventures = new LockableListModel();
	private static AdventureArray allAdventures = new AdventureArray();

	public static final ArrayList PARENT_LIST = new ArrayList();
	public static final TreeMap PARENT_ZONES = new TreeMap();
	public static final TreeMap ZONE_DESCRIPTIONS = new TreeMap();

	private static StringArray [] adventureTable = new StringArray[6];
	private static final TreeMap areaCombatData = new TreeMap();
	private static final TreeMap adventureLookup = new TreeMap();
	private static final TreeMap conditionLookup = new TreeMap();

	private static final TreeMap zoneValidations = new TreeMap();

	static
	{
		for ( int i = 0; i < 6; ++i )
			adventureTable[i] = new StringArray();

		AdventureDatabase.refreshZoneTable();
		AdventureDatabase.refreshAdventureTable();
		AdventureDatabase.refreshCombatsTable();
		AdventureDatabase.refreshAdventureList();
	}

	private static final AdventureResult SONAR = new AdventureResult( 563, 1 );
	public static final AdventureResult [] WOODS_ITEMS = new AdventureResult[12];
	static
	{
		for ( int i = 0; i < 12; ++i )
			WOODS_ITEMS[i] = new AdventureResult( i + 1, 1 );
	}

	public static class ChoiceAdventure
	{
		private String zone;
		private String setting;
		private String name;

		private String [] options;
		private String [] items;
		private String [][] spoilers;

		public ChoiceAdventure( String setting, String name, String [] options )
		{	this( "Unsorted", setting, name, options, null );
		}

		public ChoiceAdventure( String zone, String setting, String name, String [] options )
		{	this( zone, setting, name, options, null );
		}

		public ChoiceAdventure( String zone, String setting, String name, String [] options, String [] items )
		{
			this.zone = zone;
			this.setting = setting;
			this.name = name;
			this.options = options;
			this.items = items;

			if ( items != null )
				this.spoilers = new String [][] { { setting }, { name }, options, items };

			else
				this.spoilers = new String [][] { { setting }, { name }, options };
		}

		public String getZone()
		{	return zone;
		}

		public String getSetting()
		{	return setting;
		}

		public String getName()
		{	return name;
		}

		public String [] getItems()
		{	return items;
		}

		public String [] getOptions()
		{	return options;
		}

		public String [][] getSpoilers()
		{	return spoilers;
		}
	}

	public static final ChoiceAdventure [] CHOICE_ADVS =
	{
		// Lucky sewer options
		new ChoiceAdventure( "Town", "luckySewerAdventure", "Sewer Gnomes",
		  new String [] { "seal-clubbing club", "seal tooth", "helmet turtle", "scroll of turtle summoning", "pasta spoon", "ravioli hat",
		    "saucepan", "disco mask", "disco ball", "stolen accordion", "mariachi pants" } ),

		// Choice 1 is unknown

		// Denim Axes Examined
		new ChoiceAdventure( "Plains", "choiceAdventure2", "Palindome",
		  new String [] { "denim axe", "skip the adventure" },
		  new String [] { "499", "292" } ),

		// Choice 3 is The Oracle Will See You Now

		// Finger-Lickin'... Death.
		new ChoiceAdventure( "Beach", "choiceAdventure4", "South of the Border",
		  new String [] { "small meat boost", "try for poultrygeist", "skip the adventure" },
		  new String [] { null, "1164", null } ),

		// Heart of Very, Very Dark Darkness
		new ChoiceAdventure( "Woods", "choiceAdventure5", "Gravy Barrow",
		  new String [] { "use inexplicably glowing rock", "skip the adventure" } ),

		// Choice 6 is unknown

		// Choice 7 is How Depressing
		// Choice 8 is On the Verge of a Dirge

		// Choice 9 is the Giant Castle Chore Wheel: muscle position
		// Choice 10 is the Giant Castle Chore Wheel: mysticality position
		// Choice 11 is the Giant Castle Chore Wheel: map quest position
		// Choice 12 is the Giant Castle Chore Wheel: moxie position

		// Choice 13 is unknown

		// A Bard Day's Night
		new ChoiceAdventure( "Knob", "choiceAdventure14", "Knob Goblin Harem",
		  new String [] { "Knob goblin harem veil", "Knob goblin harem pants", "small meat boost", "complete the outfit" },
		  new String [] { "306", "305", null } ),

		// Yeti Nother Hippy
		new ChoiceAdventure( "Mountain", "choiceAdventure15", "eXtreme Slope 1",
		  new String [] { "eXtreme mittens", "eXtreme scarf", "small meat boost", "complete the outfit" },
		  new String [] { "399", "355", null } ),

		// Saint Beernard
		new ChoiceAdventure( "Mountain", "choiceAdventure16", "eXtreme Slope 2",
		  new String [] { "snowboarder pants", "eXtreme scarf", "small meat boost", "complete the outfit" },
		  new String [] { "356", "355", null } ),

		// Generic Teen Comedy
		new ChoiceAdventure( "Mountain", "choiceAdventure17", "eXtreme Slope 3",
		  new String [] { "eXtreme mittens", "snowboarder pants", "small meat boost", "complete the outfit" },
		  new String [] { "399", "356", null } ),

		// A Flat Miner
		new ChoiceAdventure( "Mountain", "choiceAdventure18", "Itznotyerzitz Mine 1",
		  new String [] { "miner's pants", "7-Foot Dwarven mattock", "small meat boost", "complete the outfit" },
		  new String [] { "361", "362", null } ),

		// 100% Legal
		new ChoiceAdventure( "Mountain", "choiceAdventure19", "Itznotyerzitz Mine 2",
		  new String [] { "miner's helmet", "miner's pants", "small meat boost", "complete the outfit" },
		  new String [] { "360", "361", null } ),

		// See You Next Fall
		new ChoiceAdventure( "Mountain", "choiceAdventure20", "Itznotyerzitz Mine 3",
		  new String [] { "miner's helmet", "7-Foot Dwarven mattock", "small meat boost", "complete the outfit" },
		  new String [] { "360", "362", null } ),

		// Under the Knife
		new ChoiceAdventure( "Town", "choiceAdventure21", "Sleazy Back Alley",
		  new String [] { "switch genders", "skip adventure" } ),

		// The Arrrbitrator
		new ChoiceAdventure( "Island", "choiceAdventure22", "Pirate's Cove 1",
		  new String [] { "eyepatch", "swashbuckling pants", "small meat boost", "complete the outfit" },
		  new String [] { "224", "402", null } ),

		// Barrie Me at Sea
		new ChoiceAdventure( "Island", "choiceAdventure23", "Pirate's Cove 2",
		  new String [] { "stuffed shoulder parrot", "swashbuckling pants", "small meat boost", "complete the outfit" },
		  new String [] { "403", "402", null } ),

		// Amatearrr Night
		new ChoiceAdventure( "Island", "choiceAdventure24", "Pirate's Cove 3",
		  new String [] { "stuffed shoulder parrot", "small meat boost", "eyepatch", "complete the outfit" },
		  new String [] { "403", null, "224" } ),

		// Ouch! You bump into a door!
		new ChoiceAdventure( "Town", "choiceAdventure25", "Dungeon of Doom",
		  new String [] { "magic lamp", "dead mimic", "skip adventure" },
		  new String [] { "1273", "1267", null } ),

		// Choice 26 is A Three-Tined Fork
		// Choice 27 is Footprints
		// Choice 28 is A Pair of Craters
		// Choice 29 is The Road Less Visible

		// Choices 30 - 39 are unknown

		// The Effervescent Fray
		new ChoiceAdventure( "Plains", "choiceAdventure40", "Cola Wars 1",
		  new String [] { "Cloaca-Cola fatigues", "Dyspepsi-Cola shield", "boost mysticality" },
		  new String [] { "1328", "1329", null } ),

		// Smells Like Team Spirit
		new ChoiceAdventure( "Plains", "choiceAdventure41", "Cola Wars 2",
		  new String [] { "Dyspepsi-Cola fatigues", "Cloaca-Cola helmet", "boost muscle" },
		  new String [] { "1330", "1331", null } ),

		// What is it Good For?
		new ChoiceAdventure( "Plains", "choiceAdventure42", "Cola Wars 3",
		  new String [] { "Dyspepsi-Cola helmet", "Cloaca-Cola shield", "boost moxie" },
		  new String [] { "1326", "1327", null } ),

		// Choices 43 - 44 are unknown

		// Choice 45 is Maps and Legends
		new ChoiceAdventure( "Woods", "choiceAdventure45", "Spooky Forest 1",
		  new String [] { "Spooky Temple map", "skip the adventure 1", "skip the adventure 2" },
		  new String [] { "74", null, null } ),

		// An Interesting Choice
		new ChoiceAdventure( "Woods", "choiceAdventure46", "Spooky Forest 2",
		  new String [] { "boost moxie", "boost muscle", "enter combat" } ),

		// Have a Heart
		new ChoiceAdventure( "Woods", "choiceAdventure47", "Spooky Forest 3",
		  new String [] { "bottle of used blood", "skip the adventure" },
		  new String [] { "1523", "1518" } ),

		// Choices 48 - 70 are violet fog adventures
		// Choice 71 is A Journey to the Center of Your Mind

		// Lording Over The Flies
		new ChoiceAdventure( "Island", "choiceAdventure72", "Frat House",
		  new String [] { "around the world", "skip the adventure" },
		  new String [] { "1634", "1633" } ),

		// Don't Fence Me In
		new ChoiceAdventure( "Woods", "choiceAdventure73", "Whitey's Grove 1",
		  new String [] { "boost muscle", "white picket fence", "piece of wedding cake" },
		  new String [] { null, "270", "262" } ),

		// The Only Thing About Him is the Way That He Walks
		new ChoiceAdventure( "Woods", "choiceAdventure74", "Whitey's Grove 2",
		  new String [] { "boost moxie", "boxed wine", "mullet wig" },
		  new String [] { null, "1005", "267" } ),

		// Rapido!
		new ChoiceAdventure( "Woods", "choiceAdventure75", "Whitey's Grove 3",
		  new String [] { "boost mysticality", "white lightning", "white collar" },
		  new String [] { null, "266", "1655" } ),

		// Junction in the Trunction
		new ChoiceAdventure( "Knob", "choiceAdventure76", "Knob Shaft",
		  new String [] { "cardboard ore", "styrofoam ore", "bubblewrap ore" },
		  new String [] { "1675", "1676", "1677" } ),

		// Choice 77 is Minnesota Incorporeals
		// Choice 78 is Broken
		// Choice 79 is A Hustle Here, a Hustle There
		// Choice 80 is Take a Look, it's in a Book!

		// One NightStand (simple white)
		new ChoiceAdventure( "Manor", "choiceAdventure82", "Haunted Bedroom 1",
		  new String [] { "old leather wallet", "boost muscle", "enter combat" },
		  new String [] { "1917", null, null } ),

		// One NightStand (mahogany)
		new ChoiceAdventure( "Manor", "choiceAdventure83", "Haunted Bedroom 2",
		  new String [] { "old coin purse", "enter combat", "quest item" },
		  new String [] { "1918", null, null } ),

		// One NightStand (ornate)
		new ChoiceAdventure( "Manor", "choiceAdventure84", "Haunted Bedroom 3",
		  new String [] { "small meat boost", "boost mysticality", "Lord Spookyraven's spectacles" },
		  new String [] { null, null, "1916" } ),

		// One NightStand (simple wooden)
		new ChoiceAdventure( "Manor", "choiceAdventure85", "Haunted Bedroom 4",
		  new String [] { "boost moxie (ballroom key step 1)", "empty drawer (ballroom key step 2)", "enter combat" } ),

		// Choice 86 is History is Fun!
		// Choice 87 is History is Fun!
		// Choice 88 is Naughty, Naughty

		// Tuesdays with Abhorrent Fiends -> mysticality
		// The Nether Planes on 350 Meat a Day -> moxie
		// Twisted, Curdled, Corrupt Energy and You -> myst class skill

		// Out in the Garden
		new ChoiceAdventure( "Manor", "choiceAdventure89", "Haunted Gallery",
		  new String [] { "Wolf Knight", "Snake Knight", "Dreams and Lights" } ),

		// Curtains
		new ChoiceAdventure( "Manor", "choiceAdventure90", "Haunted Ballroom",
		  new String [] { "Investigate Organ", "Watch Dancers", "Hide" } ),

		// Choice 91 is Louvre It or Leave It
		// Choices 92 - 102 are Escher print adventures
	};

	// We choose to not make some choice adventures configurable, but we
	// want to provide spoilers in the browser for them.

	public static final ChoiceAdventure [] CHOICE_ADV_SPOILERS =
	{
		// The Oracle Will See You Now
		new ChoiceAdventure( "choiceAdventure3", "Teleportitis",
		  new String [] { "skip the adventure", "randomly sink 100 meat", "make plus sign usable" } ),

		// How Depressing -> Self Explanatory
		// new ChoiceAdventure( "choiceAdventure7", "Gravy Barrow 2",
		//  new String [] { "use spooky glove", "skip the adventure" } ),

		// On the Verge of a Dirge -> Self Explanatory
		// new ChoiceAdventure( "choiceAdventure8", "Gravy Barrow 3",
		//  new String [] { "enter the chamber", "enter the chamber", "enter the chamber" } ),

		// Wheel In the Sky Keep on Turning: Muscle Position
		new ChoiceAdventure( "choiceAdventure9", "Castle Wheel",
		  new String [] { "Turn to mysticality", "Turn to moxie", "Leave at muscle" } ),

		// Wheel In the Sky Keep on Turning: Mysticality Position
		new ChoiceAdventure( "choiceAdventure10", "Castle Wheel",
		  new String [] { "Turn to Map Quest", "Turn to muscle", "Leave at mysticality" } ),

		// Wheel In the Sky Keep on Turning: Map Quest Position
		new ChoiceAdventure( "choiceAdventure11", "Castle Wheel",
		  new String [] { "Turn to moxie", "Turn to mysticality", "Leave at map quest" } ),

		// Wheel In the Sky Keep on Turning: Moxie Position
		new ChoiceAdventure( "choiceAdventure12", "Castle Wheel",
		  new String [] { "Turn to muscle", "Turn to map quest", "Leave at moxie" } ),

		// A Three-Tined Fork
		new ChoiceAdventure( "choiceAdventure26", "Spooky Forest",
		  new String [] { "muscle classes", "mysticality classes", "moxie classes" } ),

		// Footprints
		new ChoiceAdventure( "choiceAdventure27", "Spooky Forest",
		  new String [] { KoLCharacter.SEAL_CLUBBER, KoLCharacter.TURTLE_TAMER } ),

		// A Pair of Craters
		new ChoiceAdventure( "choiceAdventure28", "Spooky Forest",
		  new String [] { KoLCharacter.PASTAMANCER, KoLCharacter.SAUCEROR } ),

		// The Road Less Visible
		new ChoiceAdventure( "choiceAdventure29", "Spooky Forest",
		  new String [] { KoLCharacter.DISCO_BANDIT, KoLCharacter.ACCORDION_THIEF } ),

		// A Journey to the Center of Your Mind -> Self Explanatory

		// Minnesota Incorporeals
		new ChoiceAdventure( "choiceAdventure77", "Haunted Billiard Room",
		  new String [] { "boost moxie", "other options", "skip adventure" } ),

		// Broken
		new ChoiceAdventure( "choiceAdventure78", "Haunted Billiard Room",
		  new String [] { "other options", "boost muscle", "skip adventure" } ),

		// A Hustle Here, a Hustle There
		new ChoiceAdventure( "choiceAdventure79", "Haunted Billiard Room",
		  new String [] { "Spookyraven library key", "boost mysticality", "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceAdventure( "choiceAdventure80", "Haunted Library",
		  new String [] { "background history", "cooking recipe", "other options", "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceAdventure( "choiceAdventure81", "Haunted Library",
		  new String [] { "gallery quest", "cocktailcrafting recipe", "boost muscle", "skip adventure" } ),

		// History is Fun!
		new ChoiceAdventure( "choiceAdventure86", "Haunted Library",
		  new String [] { "Spookyraven Chapter 1", "Spookyraven Chapter 2", "Spookyraven Chapter 3" } ),

		// History is Fun!
		new ChoiceAdventure( "choiceAdventure87", "Haunted Library",
		  new String [] { "Spookyraven Chapter 4", "Spookyraven Chapter 5 (Gallery Quest)", "Spookyraven Chapter 6" } ),

		// Naughty, Naughty
		new ChoiceAdventure( "choiceAdventure88", "Haunted Library",
		  new String [] { "boost mysticality", "boost moxie", "Fettucini / Scarysauce" } ),

		// Louvre It or Leave It
		new ChoiceAdventure( "choiceAdventure91", "Haunted Gallery 2",
		  new String [] { "Enter the Drawing", "Pass on By" } ),
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

		// Curtains
		{ "choiceAdventure90", "3" },

		// Louvre It or Leave It
		{ "choiceAdventure91", "2" },
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
		if ( !ZONE_DESCRIPTIONS.isEmpty() )
			return;

		BufferedReader reader = getReader( "zonelist.dat" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				PARENT_ZONES.put( data[0], data[1] );

				if ( !PARENT_LIST.contains( data[1] ) )
					PARENT_LIST.add( data[1] );

				ZONE_DESCRIPTIONS.put( data[0], data[2] );

				if ( data.length > 3 )
				{
					ArrayList validationRequests = new ArrayList();
					for ( int i = 3; i < data.length; ++i )
						validationRequests.add( data[i] );

					zoneValidations.put( data[0], validationRequests );
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

				adventureTable[0].add( data[0] );
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

		for ( int i = 0; i < adventureTable[3].size(); ++i )
			addAdventure( getAdventure(i) );

		if ( StaticEntity.getBooleanProperty( "sortAdventures" ) )
			adventures.sort();
	}

	public static void refreshAdventureList( String desiredZone )
	{
		KoLAdventure location;
		adventures.clear();

		for ( int i = 0; i < adventureTable[3].size(); ++i )
		{
			location = allAdventures.get(i);
			if ( location.getParentZone().equals( desiredZone ) )
				adventures.add( location );
		}

		if ( StaticEntity.getBooleanProperty( "sortAdventures" ) )
			adventures.sort();
	}

	public static void addAdventure( KoLAdventure location )
	{
		adventures.add( location );
		allAdventures.add( location );
		adventureLookup.put( location.getRequest().getURLString(), location );
	}

	public static final boolean validateZone( String zoneName, String locationId )
	{
		if ( zoneName == null || locationId == null )
			return true;

		ArrayList validationRequests = (ArrayList) zoneValidations.get( zoneName );
		if ( validationRequests == null || validationRequests.isEmpty() )
			return true;

		boolean isValidZone = true;

		for ( int i = 1; isValidZone && i < validationRequests.size() - 1; ++i )
		{
			ZONE_VALIDATOR.constructURLString( (String) validationRequests.get(0) ).run();
			isValidZone &= ZONE_VALIDATOR.responseText != null &&
				ZONE_VALIDATOR.responseText.indexOf( (String) validationRequests.get(i) ) != -1;
		}

		ZONE_VALIDATOR.constructURLString( (String) validationRequests.get( validationRequests.size() - 1 ) ).run();

		// Special handling of the bat zone.

		if ( locationId.equals( "32" ) || locationId.equals( "33" ) || locationId.equals( "34" ) )
		{
			if ( locationId.equals( "32" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockleft.gif" ) == -1 )
				return true;

			if ( locationId.equals( "33" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockright.gif" ) == -1 )
				return true;

			if ( locationId.equals( "34" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockbottom.gif" ) == -1 )
				return true;

			int sonarCount = SONAR.getCount( inventory );
			int sonarToUse = 0;

			if ( ZONE_VALIDATOR.responseText.indexOf( "batrockleft.gif" ) != -1 )
				sonarToUse = 3;
			else if ( ZONE_VALIDATOR.responseText.indexOf( "batrockright.gif" ) != -1 )
				sonarToUse = 2;
			else if ( ZONE_VALIDATOR.responseText.indexOf( "batrockbottom.gif" ) != -1 )
				sonarToUse = 1;

			DEFAULT_SHELL.executeLine( "use " + Math.min( sonarToUse, sonarCount ) + " sonar-in-a-biscuit" );
			ZONE_VALIDATOR.run();

			return locationId.equals( "32" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockleft.gif" ) == -1 ||
				locationId.equals( "33" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockright.gif" ) == -1 ||
				locationId.equals( "34" ) && ZONE_VALIDATOR.responseText.indexOf( "batrockbottom.gif" ) == -1;
		}

		// Handle all others as normal.

		isValidZone &= ZONE_VALIDATOR.responseText != null;
		if ( isValidZone )
		{
			isValidZone &= ZONE_VALIDATOR.responseText.indexOf( "snarfblat=" + locationId ) != -1 ||
				ZONE_VALIDATOR.responseText.indexOf( "adv=" + locationId ) != -1 ||
				ZONE_VALIDATOR.responseText.indexOf( "name=snarfblat value=" + locationId ) != -1 ||
				ZONE_VALIDATOR.responseText.indexOf( "name=adv value=" + locationId ) != -1;
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

		if ( adventureURL.indexOf( "sewer.php" ) != -1 && adventureURL.indexOf( "doodit" ) != -1 )
			return (KoLAdventure) adventureLookup.get( "sewer.php?doodit=1" );

		return (KoLAdventure) adventureLookup.get( adventureURL );
	}

	public static KoLAdventure getAdventure( String adventureName )
	{
		if ( adventureLookup.isEmpty() )
			refreshAdventureList();

		if ( adventureName == null || adventureName.equals( "" ) )
			return null;

		return allAdventures.find( adventureName );

	}

	private static KoLAdventure getAdventure( int tableIndex )
	{
		return new KoLAdventure(
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

	public static String [][] choiceSpoilers( int choice )
	{
		String option = "choiceAdventure" + String.valueOf( choice );

		// See if this choice is controlled by user option
		for ( int i = 0; i < CHOICE_ADVS.length; ++i )
		{
			if ( CHOICE_ADVS[i].getSetting().equals( option ) )
				return CHOICE_ADVS[i].getSpoilers();
		}

		// Nope. See if we know this choice
		for ( int i = 0; i < CHOICE_ADV_SPOILERS.length; ++i )
		{
			if ( CHOICE_ADV_SPOILERS[i].getSetting().equals( option ) )
				return CHOICE_ADV_SPOILERS[i].getSpoilers();
		}

		// Nope. See if it's in the Violet Fog
		String [][] spoilers = VioletFog.choiceSpoilers( choice );
		if ( spoilers != null )
			return spoilers;

		// Nope. See if it's in the Louvre
		spoilers = Louvre.choiceSpoilers( choice );
		if ( spoilers != null )
			return spoilers;

		// Unknown choice
		return null;
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

		// See if it's a free movement in the Louvre
		if ( Louvre.freeAdventure( choice, decision ) )
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

	private static final void retrieveItem( ItemCreationRequest irequest, int missingCount )
	{
		if ( missingCount > 0 )
		{
			irequest.setQuantityNeeded( missingCount );
			irequest.run();
		}
	}

	public static final boolean retrieveItem( String itemName )
	{	return retrieveItem( new AdventureResult( itemName, 1, false ) );
	}

	public static final boolean retrieveItem( AdventureResult item )
	{
		try
		{
			int missingCount = item.getCount() - item.getCount( inventory );

			// If you already have enough of the given item, then
			// return from this method.

			if ( missingCount <= 0 )
				return true;

			// Next, if you have a piece of equipment, then
			// assume this might be a check on equipment.
			// in this case, return from this method.

			if ( KoLCharacter.hasEquipped( item ) )
			{
				DEFAULT_SHELL.executeLine( "unequip " + item.getName() );
				missingCount = item.getCount() - item.getCount( inventory );
			}

			if ( missingCount <= 0 )
				return true;

			// First, attempt to pull the item from the closet.
			// If this is successful, return from the method.

			missingCount = retrieveItem( "closet take", closet, item, missingCount );

			if ( missingCount <= 0 )
				return true;

			// Next, attempt to create the item from existing
			// ingredients (if possible).

			ItemCreationRequest creator = ItemCreationRequest.getInstance( item.getItemId() );
			if ( creator != null )
			{
				if ( ConcoctionsDatabase.hasAnyIngredient( item.getItemId() ) )
				{
					retrieveItem( creator, Math.min( missingCount, creator.getQuantityPossible() ) );
					missingCount = item.getCount() - item.getCount( inventory );

					if ( missingCount <= 0 )
						return true;
				}
			}

			// Next, hermit item retrieval is possible when
			// you have worthless items.  Use this method next.

			if ( hermitItems.contains( item ) )
			{
				int worthlessItemCount = HermitRequest.getWorthlessItemCount();
				if ( worthlessItemCount > 0 )
					(new HermitRequest( item.getItemId(), Math.min( worthlessItemCount, missingCount ) )).run();

				missingCount = item.getCount() - item.getCount( inventory );

				if ( missingCount <= 0 )
					return true;
			}

			// Next, attempt to pull the items out of storage,
			// if you are out of ronin.

			if ( KoLCharacter.canInteract() )
				missingCount = retrieveItem( "hagnk", storage, item, missingCount );

			if ( missingCount <= 0 )
				return true;

			// Try to purchase the item from the mall, if the
			// user wishes to autosatisfy through purchases,
			// and the item is not creatable through combines.

			int price = TradeableItemDatabase.getPriceById( item.getItemId() );

			boolean shouldUseMall = getBooleanProperty( "autoSatisfyWithMall" );
			boolean shouldUseStash = getBooleanProperty( "autoSatisfyWithStash" ) || getBooleanProperty( "showStashIngredients" );

			boolean shouldPurchase = price != 0 || item.getName().indexOf( "clover" ) != -1;
			boolean canUseNPCStore = getBooleanProperty( "autoSatisfyWithNPCs" ) && NPCStoreDatabase.contains( item.getName() );

			boolean shouldAutoSatisfyEarly = canUseNPCStore || !ConcoctionsDatabase.hasAnyIngredient( item.getItemId() );
			shouldAutoSatisfyEarly |= ConcoctionsDatabase.getMixingMethod( item.getItemId() ) == ItemCreationRequest.PIXEL;

			// See if the item can be retrieved from the clan stash.  If it can,
			// go ahead and pull as many items as possible from there.

			if ( shouldUseStash && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
			{
				if ( !ClanManager.isStashRetrieved() )
					(new ClanStashRequest()).run();

				missingCount = retrieveItem( "stash take", ClanManager.getStash(), item, missingCount );
				if ( missingCount <= 0 )
					return true;
			}

			// If the item should be bought early, go ahead and purchase it now,
			// after having checked the clan stash.

			if ( shouldPurchase && shouldAutoSatisfyEarly )
			{
				if ( canUseNPCStore || (KoLCharacter.canInteract() && shouldUseMall) )
					missingCount = retrieveItem( "buy", null, item, missingCount );
			}

			if ( missingCount <= 0 )
				return true;

			// If it's creatable, rather than seeing what main ingredient is missing,
			// show what sub-ingredients are missing; but only do this if it's not
			// clovers or dough, which causes infinite recursion.  Also don't do this
			// for white pixels, as that causes confusion.

			if ( creator != null && ConcoctionsDatabase.getMixingMethod( item.getItemId() ) != ItemCreationRequest.NOCREATE )
			{
				switch ( item.getItemId() )
				{
				case 24:	// ten-leaf clover
				case 196:	// disassembled clover
				case 159:	// wad of dough
				case 301:	// flat dough
				case 459:	// white pixel

					break;

				default:

					creator.setQuantityNeeded( missingCount );
					creator.run();

					return KoLCharacter.hasItem( item );
				}
			}

			// Try to purchase the item from the mall, if the user wishes to allow
			// purchases for item acquisition.

			if ( shouldPurchase && !shouldAutoSatisfyEarly )
			{
				if ( KoLCharacter.canInteract() && shouldUseMall )
					missingCount = retrieveItem( "buy", null, item, missingCount );
			}

			if ( missingCount <= 0 )
				return true;

			// If the item does not exist in sufficient quantities,
			// then notify the user that there aren't enough items
			// available to continue and cancel the request.

			KoLmafia.updateDisplay( "You need " + missingCount + " more " + item.getName() + " to continue." );
			return false;
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
			return false;
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
