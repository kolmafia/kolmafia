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
			int result = this.name.compareToIgnoreCase( ((ChoiceAdventure)o).name );
			return result != 0 ? result : this.setting.compareToIgnoreCase( ((ChoiceAdventure)o).setting );
		}
	}

	// Lucky sewer options
	public static final ChoiceAdventure LUCKY_SEWER =
		new ChoiceAdventure( "Town", "luckySewerAdventure", "Sewer Gnomes",
		  new String [] { "seal-clubbing club", "seal tooth", "helmet turtle", "turtle totem", "pasta spoon", "ravioli hat",
		    "saucepan", "disco mask", "disco ball", "stolen accordion", "mariachi pants" } );


	public static final ChoiceAdventure [] CHOICE_ADVS =
	{
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
		new ChoiceAdventure( "Woods", "choiceAdventure45", "Spooky Forest",
		  new String [] { "Spooky Temple map", "skip the adventure", "skip the adventure" },
		  new String [] { "74", null, null } ),

		// An Interesting Choice
		new ChoiceAdventure( "Woods", "choiceAdventure46", "Spooky Forest",
		  new String [] { "moxie substats", "muscle substats", "enter combat" } ),

		// Have a Heart
		new ChoiceAdventure( "Woods", "choiceAdventure47", "Spooky Forest",
		  new String [] { "bottle of used blood", "skip the adventure" },
		  new String [] { "1523", "1518" } ),

		// Choices 48 - 70 are violet fog adventures
		// Choice 71 is A Journey to the Center of Your Mind

		// Lording Over The Flies
		new ChoiceAdventure( "Island", "choiceAdventure72", "Frat House",
		  new String [] { "around the world", "skip the adventure" },
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
		new ChoiceAdventure( "Manor", "choiceAdventure82", "Haunted Bedroom",
		  new String [] { "old leather wallet", "muscle substats", "enter combat" },
		  new String [] { "1917", null, null } ),

		// One NightStand (mahogany)
		new ChoiceAdventure( "Manor", "choiceAdventure83", "Haunted Bedroom",
		  new String [] { "old coin purse", "enter combat", "quest item" },
		  new String [] { "1918", null, null } ),

		// One NightStand (ornate)
		new ChoiceAdventure( "Manor", "choiceAdventure84", "Haunted Bedroom",
		  new String [] { "small meat boost", "mysticality substats", "Lord Spookyraven's spectacles" },
		  new String [] { null, null, "1916" } ),

		// One NightStand (simple wooden)
		new ChoiceAdventure( "Manor", "choiceAdventure85", "Haunted Bedroom",
		  new String [] { "moxie (ballroom key step 1)", "empty drawer (ballroom key step 2)", "enter combat" } ),

		// Choice 86 is History is Fun!
		// Choice 87 is History is Fun!
		// Choice 88 is Naughty, Naughty
		// Choice 89 is Out in the Garden

		// Curtains
		new ChoiceAdventure( "Manor", "choiceAdventure90", "Haunted Ballroom",
		  new String [] { "enter combat", "moxie substats", "skip adventure" } ),

		// Choice 91 is Louvre It or Leave It
		// Choices 92 - 104 are Escher print adventures

		// Having a Medicine Ball
		new ChoiceAdventure( "Manor", "choiceAdventure105", "Haunted Bathroom",
		  new String [] { "moxie substats", "other options", "guy made of bees" } ),

		// Strung-Up Quartet
		new ChoiceAdventure( "Manor", "choiceAdventure106", "Haunted Ballroom",
		  new String [] { "increase monster level", "decrease combat frequency", "increase item drops", "skip adventure" } ),

		// Bad Medicine is What You Need
		new ChoiceAdventure( "Manor", "choiceAdventure107", "Haunted Bathroom",
		  new String [] { "antique bottle of cough syrup", "tube of hair oil", "bottle of ultravitamins", "take nothing" },
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
		new ChoiceAdventure( "Knob", "choiceAdventure111", "Outskirts of The Knob",
		  new String [] { "muscle substats", "muscle substats", "enter combat" } ),

		// Please, Hammer
		new ChoiceAdventure( "Town", "choiceAdventure112", "Sleazy Back Alley",
		  new String [] { "accept hammer quest", "reject quest", "muscle substats" } ),

		// Knob Goblin BBQ
		new ChoiceAdventure( "Knob", "choiceAdventure113", "Outskirts of The Knob",
		  new String [] { "complete cake quest", "enter combat", "get a random item" } ),

		// The Baker's Dilemma
		new ChoiceAdventure( "Manor", "choiceAdventure114", "Haunted Pantry",
		  new String [] { "accept cake quest", "reject quest", "moxie and meat" } ),

		// Oh No, Hobo
		new ChoiceAdventure( "Manor", "choiceAdventure115", "Haunted Pantry",
		  new String [] { "enter combat", "Good Karma", "mysticality, moxie, and meat" } ),

		// The Singing Tree
		new ChoiceAdventure( "Manor", "choiceAdventure116", "Haunted Pantry",
		  new String [] { "mysticality substats", "moxie substats", "random effect", "skip adventure" } ),

		// Tresspasser
		new ChoiceAdventure( "Manor", "choiceAdventure117", "Haunted Pantry",
		  new String [] { "enter combat", "mysticality substats", "get a random item" } ),

		// When Rocks Attack
		new ChoiceAdventure( "Knob", "choiceAdventure118", "Outskirts of The Knob",
		  new String [] { "accept unguent quest", "skip adventure" } ),

		// Choice 119 is unknown

		// Ennui is Wasted on the Young
		new ChoiceAdventure( "Knob", "choiceAdventure120", "Outskirts of The Knob",
		  new String [] { "muscle and Pumped Up", "ice cold Sir Schlitz", "moxie and lemon", "skip adventure" },
		  new String [] { null, "41", "332", null } ),

                // At Least It's Not Full Of Trash
		new ChoiceAdventure( "Woods", "choiceAdventure123", "Hidden Temple",
		  new String [] { "lose HP", "Unlock Quest Puzzle", "lose HP" },
		  new String [] { null, null, null } ),

                // No Visible Means of Support
		new ChoiceAdventure( "Woods", "choiceAdventure125", "Hidden Temple",
		  new String [] { "lose HP", "lose HP", "Unlock Hidden City" },
		  new String [] { null, null, null } ),

		// Sun at Noon, Tan Us
		new ChoiceAdventure( "Plains", "choiceAdventure126", "Palindome",
		  new String [] { "moxie", "chance of more moxie", "sunburned" } ),

		// No sir, away!  A papaya war is on!
		new ChoiceAdventure( "Plains", "choiceAdventure127", "Palindome",
		  new String [] { "3 papayas", "trade 3 papayas for stats", "stats" },
		  new String [] { "498", null, null } ),

		// Do Geese See God?
		new ChoiceAdventure( "Plains", "choiceAdventure129", "Palindome",
		  new String [] { "photograph of God", "skip the adventure" },
		  new String [] { "2259", null } ),

		// Rod Nevada, Vendor
		new ChoiceAdventure( "Plains", "choiceAdventure130", "Palindome",
		  new String [] { "hard rock candy", "skip the adventure" },
		  new String [] { "2260", null } ),

		// Choice 131 is Dr. Awkward
		// All choices lead to combat

		// Let's Make a Deal!
		new ChoiceAdventure( "Beach", "choiceAdventure132", "The Arid Extra-Dry Desert",
		  new String [] { "broken carburetor", "Unlock Oasis" },
		  new String [] { "2316", null } ),

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure( "Pyramid", "choiceAdventure134", "The Upper Chamber",
		  new String [] { "", "" },
		  new String [] { null, null } ),

		// Bait and Switch
		new ChoiceAdventure( "Island", "choiceAdventure139", "Hippy Camp",
		  new String [] { "muscle substats", "ferret bait", "enter combat" },
		  new String [] { null, "2041", null } ),

		// The Thin Tie-Dyed Line
		new ChoiceAdventure( "Island", "choiceAdventure140", "Hippy Camp",
		  new String [] { "water pipe bombs", "moxie substats", "enter combat" },
		  new String [] { "2348", null, null } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure( "Island", "choiceAdventure141", "Hippy Camp",
		  new String [] { "mysticality substats", "seal-clubbing club", "waste a turn" },
		  new String [] { null, "1", null } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure( "Island", "choiceAdventure142", "Hippy Camp",
		  new String [] { "mysticality substats", "seal-clubbing club", "Open Extended Island" },
		  new String [] { null, "1", null } ),

		// Cornered!
		new ChoiceAdventure( "Island", "choiceAdventure147", "McMillicancuddy's Barn",
		  new String [] { "Open The Granary", "Open The Bog", "Open The Pond" },
		  new String [] { null, null, null } ),

		// Cornered Again!
		new ChoiceAdventure( "Island", "choiceAdventure148", "McMillicancuddy's Barn",
		  new String [] { "Open The Back 40", "Open The Family Plot" },
		  new String [] { null, null } ),

		// ow Many Corners Does this Stupid Barn Have!?
		new ChoiceAdventure( "Island", "choiceAdventure149", "McMillicancuddy's Barn",
		  new String [] { "Open The Shady Thicket", "Open The Other Back 40" },
		  new String [] { null, null } ),

		// Choice 150 is Another Adventure About BorderTown

		// Adventurer, $1.99
		new ChoiceAdventure( "Plains", "choiceAdventure151", "Fun House",
		  new String [] { "push the nose", "skip the adventure" },
		  new String [] { null, null } ),

		// Lurking at the Threshold
		new ChoiceAdventure( "Plains", "choiceAdventure152", "Fun House",
		  new String [] { "open the door", "skip the adventure" },
		  new String [] { null, null } ),

		// Turn Your Head and Coffin
		new ChoiceAdventure( "Cyrpt", "choiceAdventure153", "Defiled Alcove",
		  new String [] { "muscle substats", "small meat boost", "half-rotten brain", "skip the adventure" },
		  new String [] { null, null, "2562", null } ),

		// Doublewide
		new ChoiceAdventure( "Cyrpt", "choiceAdventure154", "Defiled Alcove",
		  new String [] { "enter combat", "skip the adventure" },
		  new String [] { null, null } ),

		// Skull, Skull, Skull
		new ChoiceAdventure( "Cyrpt", "choiceAdventure155", "Defiled Nook",
		  new String [] { "moxie substats", "small meat boost", "rusty bonesaw", "skip the adventure" },
		  new String [] { null, null, "2563", null } ),

		// Pileup
		new ChoiceAdventure( "Cyrpt", "choiceAdventure156", "Defiled Nook",
		  new String [] { "enter combat", "skip the adventure" },
		  new String [] { null, null } ),

		// Urning Your Keep
		new ChoiceAdventure( "Cyrpt", "choiceAdventure157", "Defiled Niche",
		  new String [] { "mysticality substats", "plus-sized phylactery", "small meat gain", "skip the adventure" },
		  new String [] { null, "2564", null, null } ),

		// Lich in the Niche
		new ChoiceAdventure( "Cyrpt", "choiceAdventure158", "Defiled Niche",
		  new String [] { "enter combat", "skip the adventure" },
		  new String [] { null, null } ),

		// Go Slow Past the Drawers
		new ChoiceAdventure( "Cyrpt", "choiceAdventure159", "Defiled Cranny",
		  new String [] { "small meat boost", "stats & HP & MP", "can of Ghuol-B-Gone&trade;", "skip the adventure" },
		  new String [] { null, null, "2565", null } ),

		// Lunchtime
		new ChoiceAdventure( "Cyrpt", "choiceAdventure160", "Defiled Cranny",
		  new String [] { "enter combat", "skip the adventure" },
		  new String [] { null, null } ),

		// Bureaucracy of the Damned
		new ChoiceAdventure( "Woods", "choiceAdventure161", "Deep Fat Friar's Gate",
		  new String [] { "Quest", "Quest", "Quest", "skip the adventure" },
		  new String [] { null, null, null, null } ),

		// Between a Rock and Some Other Rocks
		new ChoiceAdventure( "Mountain", "choiceAdventure162", "Goatlet",
		  new String [] { "Open Goatlet", "skip adventure" },
		  new String [] { null, null } ),

		// Melvil Dewey Would Be Ashamed
		new ChoiceAdventure( "Manor", "choiceAdventure163", "Haunted Library",
		  new String [] { "Necrotelicomnicon", "Cookbook of the Damned", "Sinful Desires", "skip adventure" },
		  new String [] { "2494", "2495", "2496", null } ),

		// The Worm Wood choices always come in order

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
		new ChoiceAdventure( "Worm Wood", "choiceAdventure164", "Stately Pleasure Dome",
		  new String [] { "muscle substats", "MP & Spirit of Alph", "enter combat" },
		  new String [] { null, null, null } ),

		// Beyond Any Measure
		new ChoiceAdventure( "Worm Wood", "choiceAdventure165", "Stately Pleasure Dome",
		  new String [] { "Rat-Faced -> Night Vision", "Bats in the Belfry -> Good with the Ladies", "mysticality substats", "skip the adventure" },
		  new String [] { null, null, null, null } ),

		// Death is a Boat
		new ChoiceAdventure( "Worm Wood", "choiceAdventure166", "Stately Pleasure Dome",
		  new String [] { "No Vertigo -> S.T.L.T.", "moxie substats", "Unusual Fashion Sense -> albatross necklace" },
		  new String [] { "2652", null, "2659" } ),

		// It's a Fixer-Upper
		new ChoiceAdventure( "Worm Wood", "choiceAdventure167", "Mouldering Mansion",
		  new String [] { "enter combat", "mysticality substats", "HP & MP & Bats in the Belfry" },
		  new String [] { null, null, null } ),

		// Midst the Pallor of the Parlor
		new ChoiceAdventure( "Worm Wood", "choiceAdventure168", "Mouldering Mansion",
		  new String [] { "moxie substats", "Spirit of Alph -> Feelin' Philosophical", "Rat-Faced -> Unusual Fashion Sense" },
		  new String [] { null, null, null } ),

		// A Few Chintz Curtains, Some Throw Pillows, It
		new ChoiceAdventure( "Worm Wood", "choiceAdventure169", "Mouldering Mansion",
		  new String [] { "Night Vision -> flask of Amontillado", "muscle substats", "Dancing Prowess -> fancy ball mask" },
		  new String [] { "2661", null, "2662" } ),

		// La Vie Boheme
		new ChoiceAdventure( "Worm Wood", "choiceAdventure170", "Rogue Windmill",
		  new String [] { "HP & Rat-Faced", "enter combat", "moxie substats" },
		  new String [] { null, null, null } ),

		// Backstage at the Rogue Windmill
		new ChoiceAdventure( "Worm Wood", "choiceAdventure171", "Rogue Windmill",
		  new String [] { "Bats in the Belfry -> No Vertigo", "muscle substats", "Spirit of Alph -> Dancing Prowess" },
		  new String [] { null, null, null } ),

		// Up in the Hippo Room
		new ChoiceAdventure( "Worm Wood", "choiceAdventure172", "Rogue Windmill",
		  new String [] { "Good with the Ladies -> Can-Can skirt", "Feelin' Philosophical -> not-a-pipe", "mysticality substats" },
		  new String [] { "2663", "2660", null } ),

		// The Blackberry Cobbler
		new ChoiceAdventure( "Woods", "choiceAdventure177", "Black Forest",
		  new String [] { "blackberry slippers", "blackberry moccasins", "blackberry combat boots", "skip the adventure" },
		  new String [] { "2705", "2706", "2707", null } ),

		// Hammering the Armory
		new ChoiceAdventure( "Beanstalk", "choiceAdventure178", "Penultimate Fantasy Airship",
		  new String [] { "bronze breastplate", "skip the adventure" },
		  new String [] { "2126", null } ),
	};

	static
	{
		Arrays.sort( CHOICE_ADVS );
	}

	// We choose to not make some choice adventures configurable, but we
	// want to provide spoilers in the browser for them.

	public static final ChoiceAdventure [] CHOICE_ADV_SPOILERS =
	{
		// The Oracle Will See You Now
		new ChoiceAdventure( "choiceAdventure3", "Teleportitis",
		  new String [] { "skip the adventure", "randomly sink 100 meat", "make plus sign usable" } ),

		// Darker Than Dark
		new ChoiceAdventure( "choiceAdventure6", "Gravy Barrow",
		  new String [] { "no spooky glove", "skip the adventure" } ),

		// How Depressing -> Self Explanatory
		new ChoiceAdventure( "choiceAdventure7", "Gravy Barrow",
		  new String [] { "use spooky glove", "skip the adventure" } ),

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

		// Take a Look, it's in a Book!
		{ "choiceAdventure80", "4" },
		{ "choiceAdventure81", "4" },

		// Curtains
		{ "choiceAdventure90", "3" },

		// Louvre It or Leave It
		{ "choiceAdventure91", "2" },

		// Strung-Up Quartet
		{ "choiceAdventure106", "4" },

		// Bad Medicine is What You Need
		{ "choiceAdventure107", "4" },

		// Aww, Craps
		{ "choiceAdventure108", "4" },

		// Aww, Craps
		{ "choiceAdventure110", "4" },

		// The Singing Tree
		{ "choiceAdventure116", "4" },

		// The Singing Tree
		{ "choiceAdventure116", "2" },

		// Ennui is Wasted on the Young
		{ "choiceAdventure120", "4" },

		// Do Geese See God?
		{ "choiceAdventure129", "2" },

		// Rod Nevada, Vendor
		{ "choiceAdventure130", "2" },

		// Wheel In the Pyramid, Keep on Turning
		{ "choiceAdventure134", "2" },

		// Adventurer, $1.99
		{ "choiceAdventure151", "2" },

		// Lurking at the Threshold
		{ "choiceAdventure152", "2" },

		// Turn Your Head and Coffin
		{ "choiceAdventure153", "4" },

		// Doublewide
		{ "choiceAdventure154", "2" },

		// Skull, Skull, Skull
		{ "choiceAdventure155", "4" },

		// Pileup
		{ "choiceAdventure156", "2" },

		// Urning Your Keep
		{ "choiceAdventure157", "4" },

		// Lich in the Niche
		{ "choiceAdventure158", "2" },

		// Go Slow Past the Drawers
		{ "choiceAdventure159", "4" },

		// Lunchtime
		{ "choiceAdventure160", "2" },

		// Bureaucracy of the Damned
		{ "choiceAdventure161", "4" },

		// Between a Rock and Some Other Rocks
		{ "choiceAdventure162", "2" },

		// Melvil Dewey Would Be Ashamed
		{ "choiceAdventure163", "4" },

		// Beyond Any Measure
		{ "choiceAdventure165", "4" },

		// The Blackberry Cobbler
		{ "choiceAdventure177", "4" },

		// Hammering the Armory
		{ "choiceAdventure178", "2" },
	};

	// Some choice adventures have options that cost meat

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
		{ "47", "1", new AdventureResult( "bottle of used blood", 0 ) },

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

				if ( data.length == 5 )
					conditionLookup.put( data[3], data[4] );
				else
					conditionLookup.put( data[3], "none" );
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
					System.out.println( "Invalid adventure area: \"" + data[0] + "\"" );
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

	public static void refreshAdventureList()
	{
		adventures.clear();
		allAdventures.clear();
		adventureLookup.clear();

		for ( int i = 0; i < adventureTable[3].size(); ++i )
			addAdventure( getAdventure(i) );
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
	}

	public static void addAdventure( KoLAdventure location )
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

		ArrayList validationRequests = (ArrayList) zoneValidations.get( zoneName );
		if ( validationRequests == null || validationRequests.isEmpty() )
			return true;

		boolean isValidZone = true;

		for ( int i = 1; isValidZone && i < validationRequests.size() - 1; ++i )
		{
			RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( (String) validationRequests.get(0) ) );
			isValidZone &= ZONE_VALIDATOR.responseText != null &&
				ZONE_VALIDATOR.responseText.indexOf( (String) validationRequests.get(i) ) != -1;
		}

		RequestThread.postRequest( ZONE_VALIDATOR.constructURLString( (String) validationRequests.get( validationRequests.size() - 1 ) ) );

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

			RequestThread.postRequest( new ConsumeItemRequest( SONAR.getInstance( Math.min( sonarToUse, sonarCount ) ) ) );
			RequestThread.postRequest( ZONE_VALIDATOR );

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

		if ( adventureURL.startsWith( "/" ) )
			adventureURL = adventureURL.substring(1);

		KoLAdventure location = (KoLAdventure) adventureLookup.get( adventureURL );
		return location == null || location.getRequest() instanceof ClanGymRequest ? null : location;
	}

	public static KoLAdventure getAdventure( String adventureName )
	{
		if ( adventureLookup.isEmpty() )
			refreshAdventureList();

		if ( adventureName == null || adventureName.equals( "" ) )
			return null;

		return allAdventures.find( adventureName.equalsIgnoreCase( "sewer" ) ? "unlucky sewer" : adventureName );

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

		if ( !condition.equals( "castle items" ) )
			return condition;

		return (KoLCharacter.isHardcore() ? "2 choiceadv" : "1 choiceadv") + ", 1 thin black candle, 1 heavy D";
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

	public static AdventureResult getCost( String choice, String decision )
	{
		for ( int i = 0; i < CHOICE_COST.length; ++i )
			if ( choice.equals( CHOICE_COST[i][0] ) && decision.equals( CHOICE_COST[i][1] ) )
				return (AdventureResult) CHOICE_COST[i][2];

		return null;
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
		int missingCount = item.getCount() - item.getCount( inventory );

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

		// Try to purchase the item from the mall, if the
		// user wishes to autosatisfy through purchases,
		// and the item is not creatable through combines.

		int itemId = item.getItemId();
		int price = TradeableItemDatabase.getPriceById( itemId );

		boolean shouldUseMall = force || getBooleanProperty( "autoSatisfyWithMall" );
		boolean shouldUseStash = getBooleanProperty( "autoSatisfyWithStash" );

		boolean shouldPurchase = price != 0 || item.getName().indexOf( "clover" ) != -1;
		boolean canUseNPCStore = NPCStoreDatabase.contains( item.getName() );
		canUseNPCStore &= force || getBooleanProperty( "autoSatisfyWithNPCs" );

		int mixingMethod = ConcoctionsDatabase.getMixingMethod( itemId );
		boolean shouldCreate, shouldAutoSatisfyEarly;

		switch ( itemId )
		{
		case MEAT_PASTE:
		case MEAT_STACK:
		case DENSE_STACK:
			shouldCreate = true;
			break;

		default:
			shouldCreate = ConcoctionsDatabase.isPermittedMethod( mixingMethod );
			break;
		}

		ItemCreationRequest creator = shouldCreate ? ItemCreationRequest.getInstance( itemId ) : null;
		shouldCreate &= creator != null;

		switch ( mixingMethod )
		{
		case NOCREATE:
		case STARCHART:
		case PIXEL:
			shouldAutoSatisfyEarly = true;
			break;

		default:
			shouldAutoSatisfyEarly = !ConcoctionsDatabase.hasAnyIngredient( itemId );
			break;
		}

		// First, attempt to pull the item from the closet.
		// If this is successful, return from the method.

		AdventureResult [] items = new AdventureResult [] { item };
		items[0] = item.getInstance( Math.min( item.getCount( closet ), missingCount ) );

		if ( items[0].getCount() > 0 )
		{
			RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.CLOSET_TO_INVENTORY, items ) );
			missingCount = item.getCount() - item.getCount( inventory );

			if ( missingCount <= 0 )
				return true;
		}

		if ( missingCount <= 0 )
			return true;

		// Next, attempt to pull the items out of storage,
		// if you are out of ronin.

		if ( KoLCharacter.canInteract() )
		{
			items[0] = item.getInstance( Math.min( item.getCount( storage ), missingCount ) );
			if ( items[0].getCount() > 0 )
			{
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.STORAGE_TO_INVENTORY, items ) );
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

			items[0] = item.getInstance( Math.min( item.getCount( ClanManager.getStash() ), missingCount ) );
			if ( items[0].getCount() > 0 )
			{
				RequestThread.postRequest( new ClanStashRequest( items, ClanStashRequest.STASH_TO_ITEMS ) );
				missingCount = item.getCount() - item.getCount( inventory );

				if ( missingCount <= 0 )
					return true;
			}
		}

		// Next, attempt to create the item from existing
		// ingredients (if possible).

		if ( shouldCreate && creator.getQuantityPossible() > 0 )
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

		// If the item should be bought early, go ahead and purchase it now,
		// after having checked the clan stash.

		if ( shouldPurchase && shouldAutoSatisfyEarly )
		{
			if ( canUseNPCStore || (KoLCharacter.canInteract() && shouldUseMall) )
			{
				StaticEntity.getClient().makePurchases( StoreManager.searchMall( item.getName() ), missingCount );
				missingCount = item.getCount() - item.getCount( inventory );

				if ( missingCount <= 0 )
					return true;
			}
		}

		switch ( mixingMethod )
		{
		// Subingredients for star charts, pixels and malus ingredients can get
		// very expensive. Therefore, skip over them in this step.

		case NOCREATE:
		case STARCHART:
		case PIXEL:
		case MALUS:
		case ROLLING_PIN:
		case CLOVER:
		case STAFF:

			break;

		// If it's creatable, and you have at least one ingredient, see if you
		// can make it via recursion.

		default:

			if ( shouldCreate && ConcoctionsDatabase.hasAnyIngredient( itemId ) )
			{
				creator.setQuantityNeeded( missingCount );
				RequestThread.postRequest( creator );
				return KoLCharacter.hasItem( item );
			}
		}

		// Try to purchase the item from the mall, if the user wishes to allow
		// purchases for item acquisition.

		if ( shouldPurchase && !shouldAutoSatisfyEarly )
		{
			if ( KoLCharacter.canInteract() && shouldUseMall )
			{
				StaticEntity.getClient().makePurchases( StoreManager.searchMall( item.getName() ), missingCount );
				missingCount = item.getCount() - item.getCount( inventory );

				if ( missingCount <= 0 )
					return true;
			}
		}

		switch ( mixingMethod )
		{
		// Subingredients for star charts, pixels and malus ingredients can get
		// very expensive. Therefore, skip over them in this step.

		case NOCREATE:
		case STARCHART:
		case PIXEL:
		case MALUS:
		case ROLLING_PIN:
		case CLOVER:
		case STAFF:
		case MULTI_USE:

			break;

		// If it's creatable, and you have at least one ingredient, see if you
		// can make it via recursion.

		default:

			if ( shouldCreate )
			{
				creator.setQuantityNeeded( missingCount );
				RequestThread.postRequest( creator );
				return KoLCharacter.hasItem( item );
			}
		}

		// If the item does not exist in sufficient quantities,
		// then notify the user that there aren't enough items
		// available to continue and cancel the request.

		KoLmafia.updateDisplay( ERROR_STATE, "You need " + missingCount + " more " + item.getName() + " to continue." );
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

			adventureName = getCanonicalName( adventureName );

			// First, try substring matches when attempting to
			// find the adventure location.

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( ((String)this.nameList.get(i)).equals( adventureName ) )
					return this.get( i );

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
