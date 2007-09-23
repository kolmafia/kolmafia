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

import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.LockableListModel;

public class AdventureDatabase extends KoLDatabase
{
	private static final KoLRequest FAMEQUIP_CHANGER = new KoLRequest( "familiar.php?pwd&action=unequip" );
	private static final LockableListModel adventures = new LockableListModel();
	private static final AdventureArray allAdventures = new AdventureArray();

	public static final ArrayList PARENT_LIST = new ArrayList();
	public static final TreeMap PARENT_ZONES = new TreeMap();
	public static final TreeMap ZONE_DESCRIPTIONS = new TreeMap();

	private static final StringArray [] adventureTable = new StringArray[6];
	private static final TreeMap areaCombatData = new TreeMap();
	private static final TreeMap adventureLookup = new TreeMap();

	private static final StringArray conditionsById = new StringArray();
	static
	{
		conditionsById.set( 15, "1 mosquito larva" ); // Spooky Forest
		conditionsById.set( 16, "1 fairy gravy" ); // Haiku Dungeon
		conditionsById.set( 17, "3 choiceadv" );  // Hidden Temple
		conditionsById.set( 18, "1 gnollish toolbox, 1 tires" );  // Degrassi Knoll
		conditionsById.set( 19, "1 dungeoneer's dungarees" );  // Limerick Dungeon
		conditionsById.set( 21, "1 smart skull" );  // Pre-Cyrpt Cemetary
		conditionsById.set( 22, "1 disembodied brain" );  // Fernswarthy's Tower
		conditionsById.set( 25, "1 beer goggles" );  // Typical Tavern
		conditionsById.set( 26, "outfit" );  // Hippy Camp
		conditionsById.set( 27, "outfit" );  // Frat House
		conditionsById.set( 30, "1 Pine-Fresh air freshener" );  // Bat Hole Entryway
		conditionsById.set( 31, "3 sonar-in-a-biscuit, 1 broken skull" );  // Guano Junction
		conditionsById.set( 33, "1 enchanted bean" );  // Beanbat Chamber
		conditionsById.set( 38, "1 plus sign" );  // Enormous Greater-Than Sign
		conditionsById.set( 39, "1 dead mimic" );  // Dungeons of Doom
		conditionsById.set( 41, "outfit" );  // Knob Goblin Treasury
		conditionsById.set( 42, "outfit, 1 Knob Goblin perfume" );  // Knob Goblin Harem
		conditionsById.set( 47, "1 annoying pitchfork" );  // Bugbear Pens
		conditionsById.set( 48, "1 inexplicably glowing rock" );  // Spooky Gravy Barrow
		conditionsById.set( 60, "6 goat cheese" );  // Goatlet
		conditionsById.set( 61, "outfit" );  // Itznotyerzitz Mine
		conditionsById.set( 62, "1 frigid ninja stars" );  // Ninja Snowmen
		conditionsById.set( 63, "outfit" );  // eXtreme Slope
		conditionsById.set( 66, "outfit" );  // Pirate Cove
		conditionsById.set( 67, "1 abridged dictionary" );  // Pirate Cove in Disguise
		conditionsById.set( 73, "1 digital key" );  // 8-Bit Realm
		conditionsById.set( 75, "1 wussiness potion, 1 ruby W, 1 dodecagram" );  // Friar's Quest 1
		conditionsById.set( 76, "1 box of birthday candles" );  // Friar's Quest 2
		conditionsById.set( 77, "1 eldritch butterknife" );  // Friar's Quest 3
		conditionsById.set( 80, "1 64735 scroll, 1 lowercase N" );  // Valley Beyond Orc Chasm
		conditionsById.set( 81, "1 metallic A, 1 S.O.C.K." );  // Fantasy Airship
		conditionsById.set( 82, "2 choiceadv, castle map items, 1 heavy D" );  // Giant's Castle
		conditionsById.set( 83, "1 Richard's star key, 1 star hat, 1 star crossbow" );  // Hole in the Sky
		conditionsById.set( 85, "outfit" );  // Cola Battlefield
		conditionsById.set( 100, "1 bird rib, 1 lion oil" );  // Whitey's Grove
		conditionsById.set( 102, "1 chef's hat" );  // Haunted Kitchen
		conditionsById.set( 103, "1 Spookyraven gallery key" ); // Haunted Conservatory
		conditionsById.set( 105, "1 pool cue, 1 handful of hand chalk" );  // Haunted Billiards
		conditionsById.set( 107, "1 fancy bath salts" );  // Haunted Bathroom
		conditionsById.set( 108, "1 Lord Spookyraven's spectacles" );  // Haunted Bedroom
		conditionsById.set( 111, "1 sunken eyes, 1 broken wings, 1 black market map" );  // Black Forest
		conditionsById.set( 112, "1 spider web" );  // Sleazy Back Alley
		conditionsById.set( 113, "1 razor-sharp can lid" );  // Haunted Pantry
		conditionsById.set( 114, "1 chef's hat" );  // Knob Outskirts
		conditionsById.set( 119, "1 stunt nuts, 1 I Love Me Vol I" );  // Palindome
		conditionsById.set( 127, "1 filthworm hatchling scent gland" ); // The Hatching Chamber
		conditionsById.set( 128, "1 filthworm drone scent gland" ); // The Feeding Chamber
		conditionsById.set( 129, "1 filthworm royal guard scent gland" ); // The Guards Chamber
		conditionsById.set( 151, "1 choiceadv" );  // Stately Pleasure Dome
		conditionsById.set( 152, "1 choiceadv" );  // Mouldering Mansion
		conditionsById.set( 153, "1 choiceadv" );  // Rogue Windmill
	}

	private static final StringArray bountiesById = new StringArray();
	static
	{
		// First set of bounties
		bountiesById.set( 15, "3 triffid bark" ); // Spooky Forest
		bountiesById.set( 18, "6 oily rag" );  // Degrassi Knoll
		bountiesById.set( 20, "20 empty greasepaint tube" );  // Fun House
		bountiesById.set( 22, "7 wilted lettuce" );  // Fernswarthy's Tower
		bountiesById.set( 30, "14 pink bat eye" );  // Bat Hole Entryway
		bountiesById.set( 112, "13 hobo gristle" );  // Sleazy Back Alley
		bountiesById.set( 113, "13 shredded can label" );  // Haunted Pantry
		bountiesById.set( 114, "8 bloodstained briquette" );  // Knob Outskirts

		// Second set of bounties
		bountiesById.set( 26, "11 greasy dreadlock" );  // Hippy Camp
		bountiesById.set( 27, "34 empty aftershave bottle" );  // Frat House
		bountiesById.set( 41, "17 bundle of receipts" );  // Knob Goblin Treasury
		bountiesById.set( 45, "4 callused fingerbone" );  // South of the Border
		bountiesById.set( 50, "40 broken petri dish" );  // Knob Goblin Laboratory
		bountiesById.set( 66, "5 vial of pirate sweat" );  // Pirate Cove
		bountiesById.set( 100, "8 white lint" );  // Whitey's Grove
		bountiesById.set( 39, "6 worthless piece of yellow glass" );  // Dungeon's of Doom

		// Third set of bounties
		bountiesById.set( 60, "40 billy idol" );  // Goatlet
		bountiesById.set( 62, "40 coal button" );  // Ninja Snowmen
		bountiesById.set( 80, "5 sammich crust" );  // Valley Beyond Orc Chasm
		bountiesById.set( 81, "5 burned-out arcanodiode" );  // Fantasy Airship
		bountiesById.set( 82, "5 discarded pacifier" );  // Giant's Castle
		bountiesById.set( 83, "6 sticky stardust" );  // Hole in the Sky
		bountiesById.set( 106, "11 non-Euclidean hoof" );  // Haunted Gallery
		bountiesById.set( 120, "20 disintegrating cork" );  // Haunted Wine Cellar
	}

	private static final TreeMap locationByBounty = new TreeMap();
	static
	{
		for ( int i = 0; i < 6; ++i )
			adventureTable[i] = new StringArray();

		AdventureDatabase.refreshZoneTable();
		AdventureDatabase.refreshAdventureTable();
		AdventureDatabase.refreshCombatsTable();
		AdventureDatabase.refreshAdventureList();
	}

	public static final AdventureResult [] WOODS_ITEMS = new AdventureResult[12];
	static
	{
		for ( int i = 0; i < 12; ++i )
			WOODS_ITEMS[i] = new AdventureResult( i + 1, 1 );
	}

	public static class ChoiceAdventure implements Comparable
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

		public ChoiceAdventure( String setting, String name, String [] options, String [] items )
		{	this( "Unsorted", setting, name, options, items );
		}

		public ChoiceAdventure( String zone, String setting, String name, String [] options )
		{	this( zone, setting, name, options, null );
		}

		public ChoiceAdventure( String zone, String setting, String name, String [] options, String [] items )
		{
			this.zone = (String) PARENT_ZONES.get( zone );
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
		{	return this.zone;
		}

		public String getSetting()
		{	return this.setting;
		}

		public String getName()
		{	return this.name;
		}

		public String [] getItems()
		{	return this.items;
		}

		public String [] getOptions()
		{	return this.options;
		}

		public String [][] getSpoilers()
		{	return this.spoilers;
		}

		public int compareTo( Object o )
		{
			if ( choicesOrderedByName )
			{
				int result = this.name.compareToIgnoreCase( ((ChoiceAdventure)o).name );
				if ( result != 0 )
					return result;
			}

			int a = StaticEntity.parseInt( this.setting.substring(15) );
			int b = StaticEntity.parseInt( ((ChoiceAdventure)o).setting.substring(15) );

			return a - b;
		}
	}

	private static boolean choicesOrderedByName = true;

	public static final void setChoiceOrdering( boolean choicesOrderedByName )
	{	AdventureDatabase.choicesOrderedByName = choicesOrderedByName;
	}

	// Lucky sewer options
	public static final ChoiceAdventure LUCKY_SEWER =
		new ChoiceAdventure( "Town", "luckySewerAdventure", "Sewer Gnomes",
		  new String [] { "seal-clubbing club", "seal tooth", "helmet turtle", "turtle totem", "pasta spoon", "ravioli hat",
			"saucepan", "disco mask", "disco ball", "stolen accordion", "mariachi pants" } );


	public static final ChoiceAdventure [] CHOICE_ADVS =
	{
		// Choice 1 is unknown
		// Choice 2 is Denim Axes Examined
		// Choice 3 is The Oracle Will See You Now

		// Finger-Lickin'... Death.
		new ChoiceAdventure( "Beach", "choiceAdventure4", "South of the Border",
		  new String [] { "small meat boost", "try for poultrygeist", "skip adventure" },
		  new String [] { null, "1164", null } ),

		// Heart of Very, Very Dark Darkness
		new ChoiceAdventure( "Woods", "choiceAdventure5", "Gravy Barrow",
		  new String [] { "fight the fairy queen", "skip adventure" } ),

		// Choice 6 is Darker Than Dark
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
		new ChoiceAdventure( "Mountain", "choiceAdventure15", "eXtreme Slope",
		  new String [] { "eXtreme mittens", "eXtreme scarf", "small meat boost", "complete the outfit" },
		  new String [] { "399", "355", null } ),

		// Saint Beernard
		new ChoiceAdventure( "Mountain", "choiceAdventure16", "eXtreme Slope",
		  new String [] { "snowboarder pants", "eXtreme scarf", "small meat boost", "complete the outfit" },
		  new String [] { "356", "355", null } ),

		// Generic Teen Comedy
		new ChoiceAdventure( "Mountain", "choiceAdventure17", "eXtreme Slope",
		  new String [] { "eXtreme mittens", "snowboarder pants", "small meat boost", "complete the outfit" },
		  new String [] { "399", "356", null } ),

		// A Flat Miner
		new ChoiceAdventure( "Mountain", "choiceAdventure18", "Itznotyerzitz Mine",
		  new String [] { "miner's pants", "7-Foot Dwarven mattock", "small meat boost", "complete the outfit" },
		  new String [] { "361", "362", null } ),

		// 100% Legal
		new ChoiceAdventure( "Mountain", "choiceAdventure19", "Itznotyerzitz Mine",
		  new String [] { "miner's helmet", "miner's pants", "small meat boost", "complete the outfit" },
		  new String [] { "360", "361", null } ),

		// See You Next Fall
		new ChoiceAdventure( "Mountain", "choiceAdventure20", "Itznotyerzitz Mine",
		  new String [] { "miner's helmet", "7-Foot Dwarven mattock", "small meat boost", "complete the outfit" },
		  new String [] { "360", "362", null } ),

		// Under the Knife
		new ChoiceAdventure( "Town", "choiceAdventure21", "Sleazy Back Alley",
		  new String [] { "switch genders", "skip adventure" } ),

		// The Arrrbitrator
		new ChoiceAdventure( "Island", "choiceAdventure22", "Pirate's Cove",
		  new String [] { "eyepatch", "swashbuckling pants", "small meat boost", "complete the outfit" },
		  new String [] { "224", "402", null } ),

		// Barrie Me at Sea
		new ChoiceAdventure( "Island", "choiceAdventure23", "Pirate's Cove",
		  new String [] { "stuffed shoulder parrot", "swashbuckling pants", "small meat boost", "complete the outfit" },
		  new String [] { "403", "402", null } ),

		// Amatearrr Night
		new ChoiceAdventure( "Island", "choiceAdventure24", "Pirate's Cove",
		  new String [] { "stuffed shoulder parrot", "small meat boost", "eyepatch", "complete the outfit" },
		  new String [] { "403", null, "224" } ),

		// Choice 25 is Ouch! You bump into a door!
		// Choice 26 is A Three-Tined Fork
		// Choice 27 is Footprints
		// Choice 28 is A Pair of Craters
		// Choice 29 is The Road Less Visible

		// Choices 30 - 39 are unknown

		// The Effervescent Fray
		new ChoiceAdventure( "Plains", "choiceAdventure40", "Cola Wars",
		  new String [] { "Cloaca-Cola fatigues", "Dyspepsi-Cola shield", "mysticality substats" },
		  new String [] { "1328", "1329", null } ),

		// Smells Like Team Spirit
		new ChoiceAdventure( "Plains", "choiceAdventure41", "Cola Wars",
		  new String [] { "Dyspepsi-Cola fatigues", "Cloaca-Cola helmet", "muscle substats" },
		  new String [] { "1330", "1331", null } ),

		// What is it Good For?
		new ChoiceAdventure( "Plains", "choiceAdventure42", "Cola Wars",
		  new String [] { "Dyspepsi-Cola helmet", "Cloaca-Cola shield", "moxie substats" },
		  new String [] { "1326", "1327", null } ),

		// Choices 43 - 44 are unknown
		// Choice 45 is Maps and Legends

		// An Interesting Choice
		new ChoiceAdventure( "choiceAdventure46", "Spooky Forest",
		  new String [] { "moxie substats", "muscle substats", "vampire heart" },
		  new String [] { null, null, "1518" } ),

		// Have a Heart
		new ChoiceAdventure( "Woods", "choiceAdventure47", "Spooky Forest",
		  new String [] { "bottle of used blood", "skip adventure" },
		  new String [] { "1523", "1518" } ),

		// Choices 48 - 70 are violet fog adventures
		// Choice 71 is A Journey to the Center of Your Mind

		// Lording Over The Flies
		new ChoiceAdventure( "Island", "choiceAdventure72", "Frat House",
		  new String [] { "around the world", "skip adventure" },
		  new String [] { "1634", "1633" } ),

		// Don't Fence Me In
		new ChoiceAdventure( "Woods", "choiceAdventure73", "Whitey's Grove",
		  new String [] { "muscle substats", "white picket fence", "piece of wedding cake" },
		  new String [] { null, "270", "262" } ),

		// The Only Thing About Him is the Way That He Walks
		new ChoiceAdventure( "Woods", "choiceAdventure74", "Whitey's Grove",
		  new String [] { "moxie substats", "boxed wine", "mullet wig" },
		  new String [] { null, "1005", "267" } ),

		// Rapido!
		new ChoiceAdventure( "Woods", "choiceAdventure75", "Whitey's Grove",
		  new String [] { "mysticality substats", "white lightning", "white collar" },
		  new String [] { null, "266", "1655" } ),

		// Junction in the Trunction
		new ChoiceAdventure( "Knob", "choiceAdventure76", "Knob Shaft",
		  new String [] { "cardboard ore", "styrofoam ore", "bubblewrap ore" },
		  new String [] { "1675", "1676", "1677" } ),

		// Choice 77 is Minnesota Incorporeals
		// Choice 78 is Broken
		// Choice 79 is A Hustle Here, a Hustle There
		// Choice 80 is Take a Look, it's in a Book!
		// Choice 81 is Take a Look, it's in a Book!

		// One NightStand (simple white)
		new ChoiceAdventure( "Manor2", "choiceAdventure82", "Haunted Bedroom",
		  new String [] { "old leather wallet", "muscle substats", "enter combat" },
		  new String [] { "1917", null, null } ),

		// One NightStand (mahogany)
		new ChoiceAdventure( "Manor2", "choiceAdventure83", "Haunted Bedroom",
		  new String [] { "old coin purse", "enter combat", "quest item" },
		  new String [] { "1918", null, null } ),

		// One NightStand (ornate)
		new ChoiceAdventure( "Manor2", "choiceAdventure84", "Haunted Bedroom",
		  new String [] { "small meat boost", "mysticality substats", "Lord Spookyraven's spectacles" },
		  new String [] { null, null, "1916" } ),

		// One NightStand (simple wooden)
		new ChoiceAdventure( "Manor2", "choiceAdventure85", "Haunted Bedroom",
		  new String [] { "moxie (ballroom key step 1)", "empty drawer (ballroom key step 2)", "enter combat" } ),

		// Choice 86 is History is Fun!
		// Choice 87 is History is Fun!
		// Choice 88 is Naughty, Naughty
		// Choice 89 is Out in the Garden

		// Curtains
		new ChoiceAdventure( "Manor2", "choiceAdventure90", "Haunted Ballroom",
		  new String [] { "enter combat", "moxie substats", "skip adventure" } ),

		// Choice 91 is Louvre It or Leave It
		// Choices 92 - 104 are Escher print adventures

		// Having a Medicine Ball
		new ChoiceAdventure( "Manor2", "choiceAdventure105", "Haunted Bathroom",
		  new String [] { "moxie substats", "other options", "guy made of bees" } ),

		// Strung-Up Quartet
		new ChoiceAdventure( "Manor2", "choiceAdventure106", "Haunted Ballroom",
		  new String [] { "increase monster level", "decrease combat frequency", "increase item drops", "skip adventure" } ),

		// Bad Medicine is What You Need
		new ChoiceAdventure( "Manor2", "choiceAdventure107", "Haunted Bathroom",
		  new String [] { "antique bottle of cough syrup", "tube of hair oil", "bottle of ultravitamins", "skip adventure" },
		  new String [] { "2086", "2087", "2085", null } ),

		// Aww, Craps
		new ChoiceAdventure( "Town", "choiceAdventure108", "Sleazy Back Alley",
		  new String [] { "moxie substats", "meat and moxie", "random effect", "skip adventure" } ),

		// Dumpster Diving
		new ChoiceAdventure( "Town", "choiceAdventure109", "Sleazy Back Alley",
		  new String [] { "enter combat", "meat and moxie", "Mad Train wine" },
		  new String [] { null, null, "564" } ),

		// The Entertainer
		new ChoiceAdventure( "Town", "choiceAdventure110", "Sleazy Back Alley",
		  new String [] { "moxie substats", "moxie and muscle", "small meat boost", "skip adventure" },
		  new String [] { null, null, null, null } ),

		// Malice in Chains
		new ChoiceAdventure( "Knob", "choiceAdventure111", "Knob Outskirts",
		  new String [] { "muscle substats", "muscle substats", "enter combat" } ),

		// Please, Hammer
		new ChoiceAdventure( "Town", "choiceAdventure112", "Sleazy Back Alley",
		  new String [] { "accept hammer quest", "reject quest", "muscle substats" } ),

		// Knob Goblin BBQ
		new ChoiceAdventure( "Knob", "choiceAdventure113", "Knob Outskirts",
		  new String [] { "complete cake quest", "enter combat", "get a random item" } ),

		// The Baker's Dilemma
		new ChoiceAdventure( "Manor1", "choiceAdventure114", "Haunted Pantry",
		  new String [] { "accept cake quest", "reject quest", "moxie and meat" } ),

		// Oh No, Hobo
		new ChoiceAdventure( "Manor1", "choiceAdventure115", "Haunted Pantry",
		  new String [] { "enter combat", "Good Karma", "mysticality, moxie, and meat" } ),

		// The Singing Tree
		new ChoiceAdventure( "Manor1", "choiceAdventure116", "Haunted Pantry",
		  new String [] { "mysticality substats", "moxie substats", "random effect", "skip adventure" } ),

		// Tresspasser
		new ChoiceAdventure( "Manor1", "choiceAdventure117", "Haunted Pantry",
		  new String [] { "enter combat", "mysticality substats", "get a random item" } ),

		// When Rocks Attack
		new ChoiceAdventure( "Knob", "choiceAdventure118", "Knob Outskirts",
		  new String [] { "accept unguent quest", "skip adventure" } ),

		// Choice 118 is When Rocks Attack
		// Choice 119 is unknown

		// Ennui is Wasted on the Young
		new ChoiceAdventure( "Knob", "choiceAdventure120", "Knob Outskirts",
		  new String [] { "muscle and Pumped Up", "ice cold Sir Schlitz", "moxie and lemon", "skip adventure" },
		  new String [] { null, "41", "332", null } ),

		// Choice 121 is Next Sunday, A.D.
		// Choice 122 is unknown
		// Choice 123 is At Least It's Not Full Of Trash
		// Choice 124 is unknown
		// Choice 125 is No Visible Means of Support

		// Sun at Noon, Tan Us
		new ChoiceAdventure( "Plains", "choiceAdventure126", "Palindome",
		  new String [] { "moxie", "chance of more moxie", "sunburned" } ),

		// No sir, away!  A papaya war is on!
		new ChoiceAdventure( "Plains", "choiceAdventure127", "Palindome",
		  new String [] { "3 papayas", "trade 3 papayas for stats", "stats" },
		  new String [] { "498", null, null } ),

		// Choice 128 is unknown
		// Choice 129 is Do Geese See God?
		// Choice 130 is Rod Nevada, Vendor
		// Choice 131 is Dr. Awkward

		// Let's Make a Deal!
		new ChoiceAdventure( "Beach", "choiceAdventure132", "Desert (Pre-Oasis)",
		  new String [] { "broken carburetor", "Unlock Oasis" },
		  new String [] { "2316", null } ),

		// Choice 133 is unknown

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure( "Pyramid", "choiceAdventure134", "The Upper Chamber",
		  new String [] { "Turn the wheel", "skip adventure" },
		  new String [] { null, null } ),

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure( "Pyramid", "choiceAdventure135", "The Upper Chamber",
		  new String [] { "Turn the wheel", "skip adventure" },
		  new String [] { null, null } ),

		// Peace Wants Love
		new ChoiceAdventure( "Island", "choiceAdventure136", "Hippy Camp",
		  new String [] { "filthy corduroys", "filthy knitted dread sack", "small meat boost", "complete the outfit" },
		  new String [] { "213", "214", null } ),

		// An Inconvenient Truth
		new ChoiceAdventure( "Island", "choiceAdventure137", "Hippy Camp",
		  new String [] { "filthy knitted dread sack", "filthy corduroys", "small meat boost", "complete the outfit" },
		  new String [] { "214", "213", null } ),

		// Purple Hazers
		new ChoiceAdventure( "Island", "choiceAdventure138", "Frat House",
		  new String [] { "orcish cargo shorts", "Orcish baseball cap", "homoerotic frat-paddle", "complete the outfit" },
		  new String [] { "240", "239", "241" } ),

		// Bait and Switch
		new ChoiceAdventure( "IsleWar", "choiceAdventure139", "War Hippies",
		  new String [] { "muscle substats", "ferret bait", "enter combat" },
		  new String [] { null, "2041", null } ),

		// The Thin Tie-Dyed Line
		new ChoiceAdventure( "IsleWar", "choiceAdventure140", "War Hippies",
		  new String [] { "water pipe bombs", "moxie substats", "enter combat" },
		  new String [] { "2348", null, null } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure( "IsleWar", "choiceAdventure141", "War Hippies",
		  new String [] { "mysticality substats", "get some hippy food", "waste a turn" },
		  new String [] { null, null, null } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure( "IsleWar", "choiceAdventure142", "War Hippies",
		  new String [] { "mysticality substats", "get some hippy food", "start the war" },
		  new String [] { null, null, null } ),

		// Catching Some Zetas
		new ChoiceAdventure( "IsleWar", "choiceAdventure143", "War Fraternity",
		  new String [] { "muscle substats", "sake bombs", "enter combat" },
		  new String [] { null, "2067", null } ),

		// One Less Room Than In That Movie
		new ChoiceAdventure( "IsleWar", "choiceAdventure144", "War Fraternity",
		  new String [] { "moxie substats", "beer bombs", "enter combat" },
		  new String [] { null, "2350", null } ),

		// Fratacombs
		new ChoiceAdventure( "IsleWar", "choiceAdventure145", "War Fraternity",
		  new String [] { "muscle substats", "get some frat food", "waste a turn" },
		  new String [] { null, null, null } ),

		// Fratacombs
		new ChoiceAdventure( "IsleWar", "choiceAdventure146", "War Fraternity",
		  new String [] { "muscle substats", "get some frat food", "start the war" },
		  new String [] { null, null, null } ),

		// Cornered!
		new ChoiceAdventure( "IsleWar", "choiceAdventure147", "Isle War Barn",
		  new String [] { "Open The Granary (meat)", "Open The Bog (stench)", "Open The Pond (cold)" },
		  new String [] { null, null, null } ),

		// Cornered Again!
		new ChoiceAdventure( "IsleWar", "choiceAdventure148", "Isle War Barn",
		  new String [] { "Open The Back 40 (hot)", "Open The Family Plot (spooky)" },
		  new String [] { null, null } ),

		// ow Many Corners Does this Stupid Barn Have!?
		new ChoiceAdventure( "IsleWar", "choiceAdventure149", "Isle War Barn",
		  new String [] { "Open The Shady Thicket (booze)", "Open The Other Back 40 (sleaze)" },
		  new String [] { null, null } ),

		// Choice 150 is Another Adventure About BorderTown

		// Adventurer, $1.99
		new ChoiceAdventure( "Plains", "choiceAdventure151", "Fun House",
		  new String [] { "fight the clownlord", "skip adventure" },
		  new String [] { null, null } ),

		// Choice 152 is Lurking at the Threshold
		// Choices 154, 156, 158, 160 are the cyrpt bosses

		// Turn Your Head and Coffin
		new ChoiceAdventure( "Cyrpt", "choiceAdventure153", "Defiled Alcove",
		  new String [] { "muscle substats", "small meat boost", "half-rotten brain", "skip adventure" },
		  new String [] { null, null, "2562", null } ),

		// Skull, Skull, Skull
		new ChoiceAdventure( "Cyrpt", "choiceAdventure155", "Defiled Nook",
		  new String [] { "moxie substats", "small meat boost", "rusty bonesaw", "skip adventure" },
		  new String [] { null, null, "2563", null } ),

		// Urning Your Keep
		new ChoiceAdventure( "Cyrpt", "choiceAdventure157", "Defiled Niche",
		  new String [] { "mysticality substats", "plus-sized phylactery", "small meat gain", "skip adventure" },
		  new String [] { null, "2564", null, null } ),

		// Go Slow Past the Drawers
		new ChoiceAdventure( "Cyrpt", "choiceAdventure159", "Defiled Cranny",
		  new String [] { "small meat boost", "stats & HP & MP", "can of Ghuol-B-Gone&trade;", "skip adventure" },
		  new String [] { null, null, "2565", null } ),

		// Choice 161 is Bureaucracy of the Damned
		// Choice 162 is Between a Rock and Some Other Rocks

		// Melvil Dewey Would Be Ashamed
		new ChoiceAdventure( "Manor1", "choiceAdventure163", "Haunted Library",
		  new String [] { "Necrotelicomnicon", "Cookbook of the Damned", "Sinful Desires", "skip adventure" },
		  new String [] { "2494", "2495", "2496", null } ),

		// The Wormwood choices always come in order

		// 1: 164, 167, 170
		// 2: 165, 168, 171
		// 3: 166, 169, 172

		// Some first-round choices give you an effect for five turns:

		// 164/2 -> Spirit of Alph
		// 167/3 -> Bats in the Belfry
		// 170/1 -> Rat-Faced

		// First-round effects modify some second round options and
		// give you a second effect for five rounds. If you do not have
		// the appropriate first-round effect, these second-round
		// options do not consume an adventure.

		// 165/1 + Rat-Faced -> Night Vision
		// 165/2 + Bats in the Belfry -> Good with the Ladies
		// 168/2 + Spirit of Alph -> Feelin' Philosophical
		// 168/2 + Rat-Faced -> Unusual Fashion Sense
		// 171/1 + Bats in the Belfry -> No Vertigo
		// 171/3 + Spirit of Alph -> Dancing Prowess

		// Second-round effects modify some third round options and
		// give you an item. If you do not have the appropriate
		// second-round effect, most of these third-round options do
		// not consume an adventure.

		// 166/1 + No Vertigo -> S.T.L.T.
		// 166/3 + Unusual Fashion Sense -> albatross necklace
		// 169/1 + Night Vision -> flask of Amontillado
		// 169/3 + Dancing Prowess -> fancy ball mask
		// 172/1 + Good with the Ladies -> Can-Can skirt
		// 172/1 -> combat
		// 172/2 + Feelin' Philosophical -> not-a-pipe

		// Down by the Riverside
		new ChoiceAdventure( "Wormwood", "choiceAdventure164", "Pleasure Dome",
		  new String [] { "muscle substats", "MP & Spirit of Alph", "enter combat" },
		  new String [] { null, null, null } ),

		// Beyond Any Measure
		new ChoiceAdventure( "Wormwood", "choiceAdventure165", "Pleasure Dome",
		  new String [] { "Rat-Faced -> Night Vision", "Bats in the Belfry -> Good with the Ladies", "mysticality substats", "skip adventure" },
		  new String [] { null, null, null, null } ),

		// Death is a Boat
		new ChoiceAdventure( "Wormwood", "choiceAdventure166", "Pleasure Dome",
		  new String [] { "No Vertigo -> S.T.L.T.", "moxie substats", "Unusual Fashion Sense -> albatross necklace" },
		  new String [] { "2652", null, "2659" } ),

		// It's a Fixer-Upper
		new ChoiceAdventure( "Wormwood", "choiceAdventure167", "Moulder Mansion",
		  new String [] { "enter combat", "mysticality substats", "HP & MP & Bats in the Belfry" },
		  new String [] { null, null, null } ),

		// Midst the Pallor of the Parlor
		new ChoiceAdventure( "Wormwood", "choiceAdventure168", "Moulder Mansion",
		  new String [] { "moxie substats", "Spirit of Alph -> Feelin' Philosophical", "Rat-Faced -> Unusual Fashion Sense" },
		  new String [] { null, null, null } ),

		// A Few Chintz Curtains, Some Throw Pillows, It
		new ChoiceAdventure( "Wormwood", "choiceAdventure169", "Moulder Mansion",
		  new String [] { "Night Vision -> flask of Amontillado", "muscle substats", "Dancing Prowess -> fancy ball mask" },
		  new String [] { "2661", null, "2662" } ),

		// La Vie Boheme
		new ChoiceAdventure( "Wormwood", "choiceAdventure170", "Rogue Windmill",
		  new String [] { "HP & Rat-Faced", "enter combat", "moxie substats" },
		  new String [] { null, null, null } ),

		// Backstage at the Rogue Windmill
		new ChoiceAdventure( "Wormwood", "choiceAdventure171", "Rogue Windmill",
		  new String [] { "Bats in the Belfry -> No Vertigo", "muscle substats", "Spirit of Alph -> Dancing Prowess" },
		  new String [] { null, null, null } ),

		// Up in the Hippo Room
		new ChoiceAdventure( "Wormwood", "choiceAdventure172", "Rogue Windmill",
		  new String [] { "Good with the Ladies -> Can-Can skirt", "Feelin' Philosophical -> not-a-pipe", "mysticality substats" },
		  new String [] { "2663", "2660", null } ),

		// Choice 173-176 are unknown

		// The Blackberry Cobbler
		new ChoiceAdventure( "Woods", "choiceAdventure177", "Black Forest",
		  new String [] { "blackberry slippers", "blackberry moccasins", "blackberry combat boots", "skip adventure" },
		  new String [] { "2705", "2706", "2707", null } ),

		// Hammering the Armory
		new ChoiceAdventure( "Beanstalk", "choiceAdventure178", "Airship Shirt",
		  new String [] { "bronze breastplate", "skip adventure" },
		  new String [] { "2126", null } ),

		// A Pre-War Dresser Drawer, Pa!
		new ChoiceAdventure( "Plains", "choiceAdventure180", "Palindome Shirt",
		  new String [] { "Ye Olde Navy Fleece", "skip adventure" },
		  new String [] { "2125", null } ),

		// Chieftain of the Flies
		new ChoiceAdventure( "Island", "choiceAdventure181", "Post-War Frat House",
		  new String [] { "around the world", "skip adventure" },
		  new String [] { "1634", "1633" } ),
	};

	static
	{
		Arrays.sort( CHOICE_ADVS );
	}

	// We choose to not make some choice adventures configurable, but we
	// want to provide spoilers in the browser for them.

	public static final ChoiceAdventure [] CHOICE_ADV_SPOILERS =
	{
		// Denim Axes Examined
		new ChoiceAdventure( "choiceAdventure2", "Palindome",
		  new String [] { "denim axe", "skip adventure" },
		  new String [] { "499", "292" } ),

		// The Oracle Will See You Now
		new ChoiceAdventure( "choiceAdventure3", "Teleportitis",
		  new String [] { "skip adventure", "randomly sink 100 meat", "make plus sign usable" } ),

		// Darker Than Dark
		new ChoiceAdventure( "choiceAdventure6", "Gravy Barrow",
		  new String [] { "fight the fairy queen", "skip adventure" } ),

		// How Depressing -> Self Explanatory
		new ChoiceAdventure( "choiceAdventure7", "Gravy Barrow",
		  new String [] { "fight the fairy queen", "skip adventure" } ),

		// On the Verge of a Dirge -> Self Explanatory
		new ChoiceAdventure( "choiceAdventure8", "Gravy Barrow",
		  new String [] { "enter the chamber", "enter the chamber", "enter the chamber" } ),

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

		// Ouch! You bump into a door!
		new ChoiceAdventure( "choiceAdventure25", "Dungeon of Doom",
		  new String [] { "magic lamp", "dead mimic", "skip adventure" },
		  new String [] { "1273", "1267", null } ),

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

		// Maps and Legends
		new ChoiceAdventure( "choiceAdventure45", "Spooky Forest",
		  new String [] { "Spooky Temple map", "skip adventure", "skip adventure" },
		  new String [] { "74", null, null } ),

		// A Journey to the Center of Your Mind -> Self Explanatory

		// Minnesota Incorporeals
		new ChoiceAdventure( "choiceAdventure77", "Haunted Billiard Room",
		  new String [] { "moxie substats", "other options", "skip adventure" } ),

		// Broken
		new ChoiceAdventure( "choiceAdventure78", "Haunted Billiard Room",
		  new String [] { "other options", "muscle substats", "skip adventure" } ),

		// A Hustle Here, a Hustle There
		new ChoiceAdventure( "choiceAdventure79", "Haunted Billiard Room",
		  new String [] { "Spookyraven library key", "mysticality substats", "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceAdventure( "choiceAdventure80", "Haunted Library",
		  new String [] { "background history", "cooking recipe", "other options", "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceAdventure( "choiceAdventure81", "Haunted Library",
		  new String [] { "gallery quest", "cocktailcrafting recipe", "muscle substats", "skip adventure" } ),

		// History is Fun!
		new ChoiceAdventure( "choiceAdventure86", "Haunted Library",
		  new String [] { "Spookyraven Chapter 1", "Spookyraven Chapter 2", "Spookyraven Chapter 3" } ),

		// History is Fun!
		new ChoiceAdventure( "choiceAdventure87", "Haunted Library",
		  new String [] { "Spookyraven Chapter 4", "Spookyraven Chapter 5 (Gallery Quest)", "Spookyraven Chapter 6" } ),

		// Naughty, Naughty
		new ChoiceAdventure( "choiceAdventure88", "Haunted Library",
		  new String [] { "mysticality substats", "moxie substats", "Fettucini / Scarysauce" } ),

		new ChoiceAdventure( "choiceAdventure89", "Haunted Gallery",
		  new String [] { "Wolf Knight", "Snake Knight", "Dreams and Lights" } ),

		// Louvre It or Leave It
		new ChoiceAdventure( "choiceAdventure91", "Haunted Gallery",
		  new String [] { "Enter the Drawing", "skip adventure" } ),

		// At Least It's Not Full Of Trash
		new ChoiceAdventure( "choiceAdventure123", "Hidden Temple",
		  new String [] { "lose HP", "Unlock Quest Puzzle", "lose HP" } ),

		// No Visible Means of Support
		new ChoiceAdventure( "choiceAdventure125", "Hidden Temple",
		  new String [] { "lose HP", "lose HP", "Unlock Hidden City" } ),

		// Do Geese See God?
		new ChoiceAdventure( "choiceAdventure129", "Palindome",
		  new String [] { "photograph of God", "skip adventure" },
		  new String [] { "2259", null } ),

		// Rod Nevada, Vendor
		new ChoiceAdventure( "choiceAdventure130", "Palindome",
		  new String [] { "hard rock candy", "skip adventure" },
		  new String [] { "2260", null } ),

		// Lurking at the Threshold
		new ChoiceAdventure( "choiceAdventure152", "Fun House",
		  new String [] { "fight the clownlord", "skip adventure" } ),

		// Doublewide
		new ChoiceAdventure( "choiceAdventure154", "Defiled Alcove",
		  new String [] { "fight conjoined zmombie", "skip adventure" } ),

		// Pileup
		new ChoiceAdventure( "choiceAdventure156", "Defiled Nook",
		  new String [] { "fight giant skeelton", "skip adventure" } ),

		// Lich in the Niche
		new ChoiceAdventure( "choiceAdventure158", "Defiled Niche",
		  new String [] { "fight gargantulihc", "skip adventure" } ),

		// Lunchtime
		new ChoiceAdventure( "choiceAdventure160", "Defiled Cranny",
		  new String [] { "fight huge ghuol", "skip adventure" } ),

		// Between a Rock and Some Other Rocks
		new ChoiceAdventure( "choiceAdventure162", "Goatlet",
		  new String [] { "Open Goatlet", "skip adventure" } ),

	};

	// Some choice adventures have options that cost meat or items

	public static final Object [][] CHOICE_COST =
	{
		// Denim Axes Examined
		{ "2", "1", new AdventureResult( "rubber axe", -1 ) },

		// Finger-Lickin'... Death.
		{ "4", "1", new AdventureResult( AdventureResult.MEAT, -500 ) },
		{ "4", "2", new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Under the Knife
		{ "21", "1", new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Ouch! You bump into a door!
		{ "25", "1", new AdventureResult( AdventureResult.MEAT, -50 ) },
		{ "25", "2", new AdventureResult( AdventureResult.MEAT, -5000 ) },

		// Have a Heart
		// This trades all vampire hearts for an equal number of
		// bottles of used blood.
		{ "47", "1", new AdventureResult( "vampire heart", 0 ) },

		// Lording Over The Flies
		// This trades 5 Spanish flies for around the worlds.
		// Excess flies are left in inventory.
		{ "72", "1", new AdventureResult( "Spanish fly", -5 ) },

		// No sir, away!  A papaya war is on!
		{ "127", "2", new AdventureResult( "papaya", -3 ) },

		// Do Geese See God?
		{ "129", "1", new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Rod Nevada, Vendor
		{ "130", "1", new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Let's Make a Deal!
		{ "132", "1", new AdventureResult( AdventureResult.MEAT, -5000 ) },

		// The Blackberry Cobbler
		{ "177", "1", new AdventureResult( "blackberry", -10 ) },
		{ "177", "2", new AdventureResult( "blackberry", -10 ) },
		{ "177", "3", new AdventureResult( "blackberry", -10 ) },

		// Chieftain of the Flies
		// This trades 5 Spanish flies for around the worlds.
		// Excess flies are left in inventory.
		{ "181", "1", new AdventureResult( "Spanish fly", -5 ) },
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

		BufferedReader reader = getReader( "zonelist.txt" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				PARENT_ZONES.put( data[0], data[1] );

				if ( !PARENT_LIST.contains( data[1] ) )
					PARENT_LIST.add( data[1] );

				ZONE_DESCRIPTIONS.put( data[0], data[2] );
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
		BufferedReader reader = getReader( "adventures.txt" );

		for ( int i = 0; i < 4; ++i )
			adventureTable[i].clear();

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length > 3 )
			{
				if ( data[1].indexOf( "send" ) != -1 )
					continue;

				adventureTable[0].add( data[0] );

				String [] requirements = data[1].split( "/" );
				adventureTable[1].add( requirements[0] );
				adventureTable[2].add( requirements[1] );

				String [] location = data[2].split( "=" );
				adventureTable[3].add( location[0] + ".php" );
				adventureTable[4].add( location[1] );

				adventureTable[5].add( data[3] );
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

		BufferedReader reader = getReader( "combats.txt" );
		String [] data;

		String [] adventures = adventureTable[5].toArray();

		while ( (data = readData( reader )) != null )
		{
			if ( data.length > 1 )
			{
				if ( !validateAdventureArea( data[0], adventures ) )
				{
					RequestLogger.printLine( "Invalid adventure area: \"" + data[0] + "\"" );
					continue;
				}

				int combats = parseInt( data[1] );
				// There can be an ultra-rare monster even if
				// there are no other combats
				AreaCombatData combat = new AreaCombatData( combats );
				for ( int i = 2; i < data.length; ++i )
					combat.addMonster( data[i] );
				areaCombatData.put( data[0], combat );
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

	public static final void refreshAdventureList()
	{
		adventures.clear();
		allAdventures.clear();
		adventureLookup.clear();

		for ( int i = 0; i < adventureTable[3].size(); ++i )
			addAdventure( getAdventure(i) );

		for ( int i = 0; i < bountiesById.size(); ++i )
		{
			String bounty = bountiesById.get(i);
			if ( bounty == null || bounty.equals( "" ) )
				continue;

			bounty = getCanonicalName( bounty.substring( bounty.indexOf( " " ) + 1 ) );
			locationByBounty.put( bounty, getAdventureByURL( "adventure.php?snarfblat=" + i ) );
		}
	}

	public static final KoLAdventure getBountyLocation( String item )
	{	return item == null ? null : (KoLAdventure) locationByBounty.get( getCanonicalName( item ) );
	}

	public static final void refreshAdventureList( String desiredZone )
	{
		KoLAdventure location;
		adventures.clear();

		for ( int i = 0; i < adventureTable[3].size(); ++i )
		{
			location = allAdventures.get(i);
			if ( location.getParentZone().equals( desiredZone ) )
				adventures.add( location );
		}
	}

	public static final void addAdventure( KoLAdventure location )
	{
		adventures.add( location );
		allAdventures.add( location );

		String url = location.getRequest().getURLString();

		adventureLookup.put( url, location );
		adventureLookup.put( singleStringReplace( url, "snarfblat=", "adv=" ), location );
		adventureLookup.put( url + "&override=on", location );
	}

	public static final boolean validateZone( String zoneName, String locationId )
	{
		if ( zoneName == null || locationId == null )
			return true;

		// Special handling of the bat zone.


		return true;
	}

	public static final LockableListModel getAsLockableListModel()
	{
		if ( adventures.isEmpty() )
			refreshAdventureList();

		return adventures;
	}

	public static final KoLAdventure getAdventureByURL( String adventureURL )
	{
		if ( adventureLookup.isEmpty() )
			refreshAdventureList();

		if ( adventureURL.startsWith( "/" ) )
			adventureURL = adventureURL.substring(1);

		if ( adventureURL.startsWith( "basement.php" ) )
			return null;

		if ( adventureURL.startsWith( "sewer.php" ) )
			return adventureURL.indexOf( "doodit" ) == -1 ?
				(KoLAdventure) adventureLookup.get( "sewer.php" ) : (KoLAdventure) adventureLookup.get( "sewer.php?doodit=1" );

		int subAdventureIndex = adventureURL.indexOf( "&subsnarfblat");
		if ( subAdventureIndex != -1 )
			adventureURL = adventureURL.substring( 0, subAdventureIndex );

		KoLAdventure location = (KoLAdventure) adventureLookup.get( adventureURL );
		return location == null || location.getRequest() instanceof ClanGymRequest ? null : location;
	}

	public static final KoLAdventure getAdventure( String adventureName )
	{
		if ( adventureLookup.isEmpty() )
			refreshAdventureList();

		if ( adventureName == null || adventureName.equals( "" ) )
			return null;

		return allAdventures.find( adventureName.equalsIgnoreCase( "sewer" ) ? "unlucky sewer" : adventureName );

	}

	private static final KoLAdventure getAdventure( int tableIndex )
	{
		return new KoLAdventure(
			adventureTable[0].get( tableIndex ), adventureTable[1].get( tableIndex ),
			adventureTable[2].get( tableIndex ), adventureTable[3].get( tableIndex ),
			adventureTable[4].get( tableIndex ), adventureTable[5].get( tableIndex ) );
	}

	public static final String getDefaultConditions( KoLAdventure adventure )
	{
		if ( adventure == null )
			return "none";

		if ( !adventure.getFormSource().startsWith( "adventure" ) )
		{
			if ( adventure.getFormSource().startsWith( "sewer" ) )
				return "2 worthless item";

			return "none";
		}

		int adventureId = StaticEntity.parseInt( adventure.getAdventureId() );
		int bountyId = KoLSettings.getIntegerProperty( "currentBountyItem" );

		if ( bountyId != 0 )
		{
			String bounty = bountiesById.get( adventureId );
			if ( !bounty.equals( "" ) && TradeableItemDatabase.getItemId( bounty.substring( bounty.indexOf( " " ) ).trim() ) == bountyId )
				return bounty;
		}

		String conditions = conditionsById.get( adventureId );
		if ( conditions != null && !conditions.equals( "" ) )
			return conditions;
		return "none";
	}

	public static final String [][] choiceSpoilers( int choice )
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

	public static final AdventureResult getCost( String choice, String decision )
	{
		for ( int i = 0; i < CHOICE_COST.length; ++i )
			if ( choice.equals( CHOICE_COST[i][0] ) && decision.equals( CHOICE_COST[i][1] ) )
				return (AdventureResult) CHOICE_COST[i][2];

		return null;
	}

	public static final boolean freeAdventure( String text )
	{
		for ( int i = 0; i < FREE_ADVENTURES.length; ++i )
			if ( text.indexOf( FREE_ADVENTURES[i] ) != -1 )
				return true;
		return false;
	}

	private static final boolean validateAdventureArea( String area, String [] areas )
	{
		for ( int i = 0; i < areas.length; ++i )
			if ( area.equals( areas[i] ) )
				 return true;
		return false;
	}

	public static final AreaCombatData getAreaCombatData( String area )
	{
		// Strip out zone name if present
		int index = area.indexOf( ":" );
		if ( index != -1 )
			area = area.substring( index + 2 );

		// Get the combat data
		return (AreaCombatData) areaCombatData.get( area );
	}

	public static final boolean retrieveItem( String itemName )
	{	return retrieveItem( new AdventureResult( itemName, 1, false ), false );
	}

	public static final boolean retrieveItem( AdventureResult item )
	{	return retrieveItem( item, false );
	}

	public static final boolean retrieveItem( AdventureResult item, boolean force )
	{
		RequestThread.openRequestSequence();
		boolean result = acquireItem( item, force );
		RequestThread.closeRequestSequence();

		return result;
	}

	private static final boolean acquireItem( AdventureResult item, boolean force )
	{
		int itemId = item.getItemId();

		int missingCount = item.getCount() -
			(itemId == HermitRequest.WORTHLESS_ITEM.getItemId() ? HermitRequest.getWorthlessItemCount() : item.getCount( inventory ));

		// If you already have enough of the given item, then
		// return from this method.

		if ( missingCount <= 0 )
			return true;

		for ( int i = KoLCharacter.HAT; i <= KoLCharacter.FAMILIAR; ++i )
		{
			if ( KoLCharacter.getEquipment( i ).equals( item ) )
			{
				RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.UNEQUIP, i ) );
				--missingCount;
			}
		}

		if ( missingCount <= 0 )
			return true;

		for ( int i = 0; i < KoLCharacter.getFamiliarList().size(); ++i )
		{
			FamiliarData current = (FamiliarData) KoLCharacter.getFamiliarList().get(i);

			if ( current.getItem() != null && current.getItem().equals( item ) )
			{
				KoLmafia.updateDisplay( "Stealing " + item.getName() + " from " + current.getName() + " the " + current.getRace() + "..." );
				FAMEQUIP_CHANGER.addFormField( "famid", String.valueOf( current.getId() ) );
				RequestThread.postRequest( FAMEQUIP_CHANGER );

				--missingCount;

				if ( missingCount <= 0 )
					return true;
			}
		}

		// First, handle worthless items by traveling to
		// the sewer for as many adventures as needed.

		if ( itemId == HermitRequest.WORTHLESS_ITEM.getItemId() )
		{
			ArrayList temporary = new ArrayList();
			temporary.addAll( conditions );
			conditions.clear();

			conditions.add( item.getInstance( missingCount ) );
			StaticEntity.getClient().makeRequest( getAdventureByURL( "sewer.php" ), KoLCharacter.getAdventuresLeft() );

			if ( !conditions.isEmpty() )
				KoLmafia.updateDisplay( ABORT_STATE, "Unable to acquire " + item.getCount() + " worthless items." );

			conditions.clear();
			conditions.addAll( temporary );

			return HermitRequest.getWorthlessItemCount() >= item.getCount();
		}

		// Try to purchase the item from the mall, if the
		// user wishes to autosatisfy through purchases,
		// and the item is not creatable through combines.

		int price = TradeableItemDatabase.getPriceById( itemId );

		boolean shouldUseMall = force || KoLSettings.getBooleanProperty( "autoSatisfyWithMall" ) &&
			((price > 0 || item.getName().indexOf( "clover" ) != -1) && KoLCharacter.canInteract() && TradeableItemDatabase.isTradeable( itemId ));

		boolean shouldUseStash = KoLSettings.getBooleanProperty( "autoSatisfyWithStash" );
		boolean shouldUseNPCStore = NPCStoreDatabase.contains( item.getName() ) && (force || KoLSettings.getBooleanProperty( "autoSatisfyWithNPCs" ));

		int mixingMethod = ConcoctionsDatabase.getMixingMethod( itemId );
		ItemCreationRequest creator = ItemCreationRequest.getInstance( itemId );

		if ( mixingMethod == COOK_REAGENT && KoLCharacter.canInteract() && !KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR ) )
			creator = null;

		// First, attempt to pull the item from the closet.
		// If this is successful, return from the method.

		int itemCount = item.getCount( closet );

		if ( itemCount > 0 )
		{
			RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.CLOSET_TO_INVENTORY,
				new AdventureResult [] { item.getInstance( Math.min( itemCount, missingCount ) ) } ) );
			missingCount = item.getCount() - item.getCount( inventory );

			if ( missingCount <= 0 )
				return true;
		}

		// Next, attempt to pull the items out of storage,
		// if you are out of ronin.

		if ( KoLCharacter.canInteract() && !KoLCharacter.inBadMoon() )
		{
			itemCount = item.getCount( storage );

			if ( itemCount > 0 )
			{
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.STORAGE_TO_INVENTORY,
					new AdventureResult [] { item.getInstance( itemCount ) } ) );

				missingCount = item.getCount() - item.getCount( inventory );
				if ( missingCount <= 0 )
					return true;
			}
		}

		// See if the item can be retrieved from the clan stash.  If it can,
		// go ahead and pull as many items as possible from there.

		if ( shouldUseStash && KoLCharacter.canInteract() && KoLCharacter.hasClan() )
		{
			if ( !ClanManager.isStashRetrieved() )
				RequestThread.postRequest( new ClanStashRequest() );

			itemCount = item.getCount( ClanManager.getStash() );
			if ( itemCount > 0 )
			{
				RequestThread.postRequest( new ClanStashRequest(
					new AdventureResult [] { item.getInstance( Math.min( itemCount, getPurchaseCount( itemId, missingCount ) ) ) },
					ClanStashRequest.STASH_TO_ITEMS ) );

				missingCount = item.getCount() - item.getCount( inventory );

				if ( missingCount <= 0 )
					return true;
			}
		}

		// Next, attempt to create the item from existing
		// ingredients (if possible).

		if ( creator != null && creator.getQuantityPossible() > 0 )
		{
			creator.setQuantityNeeded( Math.min( missingCount, creator.getQuantityPossible() ) );
			RequestThread.postRequest( creator );
			missingCount = item.getCount() - item.getCount( inventory );

			if ( missingCount <= 0 )
				return true;
		}

		// Next, hermit item retrieval is possible when
		// you have worthless items.  Use this method next.

		if ( hermitItems.contains( item ) )
		{
			int worthlessItemCount = HermitRequest.getWorthlessItemCount();
			if ( worthlessItemCount > 0 )
			{
				RequestThread.postRequest( new HermitRequest( itemId, Math.min( worthlessItemCount, missingCount ) ) );
				missingCount = item.getCount() - item.getCount( inventory );

				if ( missingCount <= 0 )
					return true;
			}
		}

		if ( trapperItems.contains( item ) )
		{
			int furCount = CouncilFrame.YETI_FUR.getCount( inventory );
			if ( furCount > 0 )
			{
				KoLmafia.updateDisplay( "Visiting the trapper..." );
				RequestThread.postRequest( new KoLRequest( "trapper.php?pwd&action=Yep.&whichitem=" + itemId + "&qty=" +
					Math.min( missingCount, furCount ) ) );

				missingCount = item.getCount() - item.getCount( inventory );
				if ( missingCount <= 0 )
					return true;
			}
		}

		// If the item should be bought early, go ahead and purchase it now,
		// after having checked the clan stash.

		if ( shouldUseNPCStore || (shouldUseMall && !hasAnyIngredient( itemId )) )
		{
			StaticEntity.getClient().makePurchases( StoreManager.searchMall( item.getName() ), getPurchaseCount( itemId, missingCount ) );
			missingCount = item.getCount() - item.getCount( inventory );

			if ( missingCount <= 0 )
				return true;
		}

		switch ( mixingMethod )
		{
		// Subingredients for star charts, pixels and malus ingredients can get
		// very expensive. Therefore, skip over them in this step.

		case NOCREATE:
		case STARCHART:
		case PIXEL:
		case MALUS:
		case STAFF:
		case MULTI_USE:

			break;

		// If it's creatable, and you have at least one ingredient, see if you
		// can make it via recursion.

		default:

			if ( creator != null && itemId != ConcoctionsDatabase.WAD_DOUGH && itemId != SewerRequest.DISASSEMBLED_CLOVER )
			{
				creator.setQuantityNeeded( missingCount );
				RequestThread.postRequest( creator );

				missingCount = item.getCount() - item.getCount( inventory );

				if ( missingCount <= 0 )
					return true;
			}
		}

		if ( shouldUseMall || isRestorePurchase( itemId ) )
		{
			StaticEntity.getClient().makePurchases( StoreManager.searchMall( item.getName() ), getPurchaseCount( itemId, missingCount ) );
			missingCount = item.getCount() - item.getCount( inventory );

			if ( missingCount <= 0 )
				return true;
		}

		// If the item does not exist in sufficient quantities,
		// then notify the user that there aren't enough items
		// available to continue and cancel the request.

		KoLmafia.updateDisplay( ERROR_STATE, "You need " + missingCount + " more " + item.getName() + " to continue." );
		return false;
	}

	private static boolean isRestorePurchase( int itemId )
	{
		switch ( itemId )
		{
		case 588: // soft green echo eyedrop antidote
		case 592: // tiny house
		case 595: // scroll of drastic healing
			return true;

		default:
			return false;
		}
	}

	private static int getPurchaseCount( int itemId, int missingCount )
	{
		if ( !KoLCharacter.canInteract() )
			return missingCount;

		return isRestorePurchase( itemId ) ? Math.max( 20, missingCount ) : missingCount;
	}

	private static final boolean hasAnyIngredient( int itemId )
	{
		if ( itemId < 0 )
			return false;

		switch ( itemId )
		{
		case MEAT_PASTE:
			return KoLCharacter.getAvailableMeat() >= 10;
		case MEAT_STACK:
			return KoLCharacter.getAvailableMeat() >= 100;
		case DENSE_STACK:
			return KoLCharacter.getAvailableMeat() >= 1000;
		}

		int mixingMethod = ConcoctionsDatabase.getMixingMethod( itemId );
		AdventureResult [] ingredients = ConcoctionsDatabase.getStandardIngredients( itemId );

		for ( int i = 0; i < ingredients.length; ++i )
		{
			// An item is immediately available if it is in your inventory,
			// in your closet, or you have the ingredients for a substep.

			if ( inventory.contains( ingredients[i] ) || closet.contains( ingredients[i] ) )
				return true;

			if ( hasAnyIngredient( ingredients[i].getItemId() ) )
				return true;
		}

		return false;
	}

	private static class AdventureArray
	{
		private ArrayList nameList = new ArrayList();
		private ArrayList internalList = new ArrayList();

		public KoLAdventure get( int index )
		{
			if ( index < 0 || index > this.internalList.size() )
				return null;

			return (KoLAdventure) this.internalList.get( index );
		}

		public void add( KoLAdventure value )
		{
			this.nameList.add( getCanonicalName( value.getAdventureName() ) );
			this.internalList.add( value );
		}

		public KoLAdventure find( String adventureName )
		{
			int matchCount = 0;
			int adventureIndex = -1;

			adventureName = getCanonicalName( CombatSettings.encounterKey( adventureName ) );

			// First, prefer adventures which start with the
			// provided substring.  That failing, report all
			// matches.

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( ((String)this.nameList.get(i)).equals( adventureName ) )
					return this.get( i );

				if ( ((String) this.nameList.get(i)).startsWith( adventureName ) )
				{
					++matchCount;
					adventureIndex = i;
				}
			}

			if ( matchCount > 1 )
			{
				for ( int i = 0; i < this.size(); ++i )
					if ( ((String)this.nameList.get(i)).startsWith( adventureName ) )
						RequestLogger.printLine( (String) this.nameList.get(i) );

				KoLmafia.updateDisplay( ERROR_STATE, "Multiple matches against " + adventureName + "." );
				return null;
			}

			if ( matchCount == 1 )
				return this.get( adventureIndex );

			// Next, try substring matches when attempting to
			// find the adventure location.  That failing,
			// report all matches.

			matchCount = 0;

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( ((String)this.nameList.get(i)).indexOf( adventureName ) != -1 )
				{
					++matchCount;
					adventureIndex = i;
				}
			}

			if ( matchCount > 1 )
			{
				for ( int i = 0; i < this.size(); ++i )
					if ( ((String)this.nameList.get(i)).indexOf( adventureName ) != -1 )
						RequestLogger.printLine( (String) this.nameList.get(i) );

				KoLmafia.updateDisplay( ERROR_STATE, "Multiple matches against " + adventureName + "." );
				return null;
			}

			if ( matchCount == 1 )
				return this.get( adventureIndex );

			// Next, try to do fuzzy matching.  If it matches
			// exactly one area, use it.  Otherwise, if it
			// matches more than once, do nothing.

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( fuzzyMatches( (String) this.nameList.get(i), adventureName ) )
				{
					++matchCount;
					adventureIndex = i;
				}
			}

			if ( matchCount > 1 )
			{
				for ( int i = 0; i < this.size(); ++i )
					if ( fuzzyMatches( (String) this.nameList.get(i), adventureName ) )
						RequestLogger.printLine( (String) this.nameList.get(i) );

				KoLmafia.updateDisplay( ERROR_STATE, "Multiple matches against " + adventureName + "." );
				return null;
			}

			if ( matchCount == 1 )
				return this.get( adventureIndex );

			return null;
		}

		public void clear()
		{
			this.nameList.clear();
			this.internalList.clear();
		}

		public int size()
		{	return this.internalList.size();
		}
	}
}
