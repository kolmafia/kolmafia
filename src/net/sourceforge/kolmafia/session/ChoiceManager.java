/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PasswordHashRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.TavernRequest;

import net.sourceforge.kolmafia.swingui.CouncilFrame;

import net.sourceforge.kolmafia.textui.command.ChoiceCommand;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.MemoriesDecorator;
import net.sourceforge.kolmafia.webui.RelayServer;

public abstract class ChoiceManager
{
	public static int lastChoice = 0;
	public static int lastDecision = 0;
	public static String lastResponseText = "";

	private static boolean initializeAfterChoice = false;

	private static final AdventureResult PAPAYA = ItemPool.get( ItemPool.PAPAYA, 1 );
	public static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice\"? value=\"?(\\d+)\"?" );
	public static final Pattern CHOICE2_PATTERN = Pattern.compile( "value='(\\d+)' name='whichchoice'" );
	// <a href="choice.php?whichchoice=537&pwd=&option=1">
	public static final Pattern CHOICE3_PATTERN = Pattern.compile( "choice.php\\?whichchoice=(\\d+)" );

	private static final Pattern URL_CHOICE_PATTERN = Pattern.compile( "whichchoice=(\\d+)" );
	private static final Pattern URL_OPTION_PATTERN = Pattern.compile( "option=(\\d+)" );
	private static final Pattern TATTOO_PATTERN = Pattern.compile( "otherimages/sigils/hobotat(\\d+).gif" );

	public static final GenericRequest CHOICE_HANDLER = new PasswordHashRequest( "choice.php" );

	private static final AdventureResult MAIDEN_EFFECT = new AdventureResult( "Dreams and Lights", 1, true );
	private static final AdventureResult BALLROOM_KEY = ItemPool.get( ItemPool.BALLROOM_KEY, 1 );

	private static final AdventureResult[] MISTRESS_ITEMS = new AdventureResult[]
	{
		ItemPool.get( ItemPool.CHINTZY_SEAL_PENDANT, 1 ),
		ItemPool.get( ItemPool.CHINTZY_TURTLE_BROOCH, 1 ),
		ItemPool.get( ItemPool.CHINTZY_NOODLE_RING, 1 ),
		ItemPool.get( ItemPool.CHINTZY_SAUCEPAN_EARRING, 1 ),
		ItemPool.get( ItemPool.CHINTZY_DISCO_BALL_PENDANT, 1 ),
		ItemPool.get( ItemPool.CHINTZY_ACCORDION_PIN, 1 ),
		ItemPool.get( ItemPool.ANTIQUE_HAND_MIRROR, 1 ),
	};

	private static final Pattern HELLEVATOR_PATTERN = Pattern.compile( "the (lobby|first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh) (button|floor)" );

	private static final String[] FLOORS = new String[]
	{
		"lobby",
		"first",
		"second",
		"third",
		"fourth",
		"fifth",
		"sixth",
		"seventh",
		"eighth",
		"ninth",
		"tenth",
		"eleventh",
	};

	public static class ChoiceAdventure
		implements Comparable
	{
		private final String zone;
		private final String setting;
		private final String name;

		private final String[] options;
		private final String[] items;
		private String[][] spoilers;

		public ChoiceAdventure( final String zone, final String setting, final String name, final String[] options )
		{
			this( zone, setting, name, options, null );
		}

		public ChoiceAdventure( final String zone, final String setting, final String name, final String[] options,
			final String[] items )
		{
			this.zone = (String) AdventureDatabase.PARENT_ZONES.get( zone );
			this.setting = setting;
			this.name = name;
			this.options = options;
			this.items = items;

			String[] settingArray = new String[] { setting };

			String[] nameArray = new String[] { name };

			if ( items != null )
			{
				this.spoilers = new String[][] { settingArray, nameArray, options, items };
			}
			else
			{
				this.spoilers = new String[][] { settingArray, nameArray, options };
			}
		}

		public String getZone()
		{
			return this.zone;
		}

		public String getSetting()
		{
			return this.setting;
		}

		public String getName()
		{
			return this.name;
		}

		public String[] getItems()
		{
			return this.items;
		}

		public String[] getOptions()
		{
			if ( this.options == null )
			{
				return ChoiceManager.dynamicChoiceOptions( this.setting );
			}
			return this.options;
		}

		public String[][] getSpoilers()
		{
			return this.spoilers;
		}

		public int compareTo( final Object o )
		{
			if ( ChoiceManager.choicesOrderedByName )
			{
				int result = this.name.compareToIgnoreCase( ( (ChoiceAdventure) o ).name );

				if ( result != 0 )
				{
					return result;
				}
			}

			int a = StringUtilities.parseInt( this.setting.substring( 15 ) );
			int b = StringUtilities.parseInt( ( (ChoiceAdventure) o ).setting.substring( 15 ) );

			return a - b;
		}
	}

	// A ChoiceSpoiler is a ChoiceAdventure that isn't user-configurable.
	// The zone is optional, since it doesn't appear in the choiceadv GUI.
	public static class ChoiceSpoiler
		extends ChoiceAdventure
	{
		public ChoiceSpoiler( final String setting, final String name, final String[] options )
		{
			super( "Unsorted", setting, name, options, null );
		}

		public ChoiceSpoiler( final String setting, final String name, final String[] options, final String[] items )
		{
			super( "Unsorted", setting, name, options, items );
		}

		public ChoiceSpoiler( final String zone, final String setting, final String name, final String[] options )
		{
			super( zone, setting, name, options, null );
		}

		public ChoiceSpoiler( final String zone, final String setting, final String name, final String[] options,
			final String[] items )
		{
			super( zone, setting, name, options, items );
		}
	}

	// NULLCHOICE is returned for failed lookups, so the caller doesn't have to do null checks.
	public static final ChoiceSpoiler NULLCHOICE = new ChoiceSpoiler( "none", "none", new String[]{} );

	private static boolean choicesOrderedByName = true;

	public static final void setChoiceOrdering( final boolean choicesOrderedByName )
	{
		ChoiceManager.choicesOrderedByName = choicesOrderedByName;
	}

	private static final Object[] CHOICE_DATA =
	{
		// Choice 1 is unknown

		// Denim Axes Examined
		new ChoiceSpoiler(
			"choiceAdventure2", "Palindome",
			new String[] { "denim axe", "skip adventure" },
			new String[] { "499", "292" } ),
		// Denim Axes Examined
		new Object[]{ new Integer(2), new Integer(1),
		  new AdventureResult( "rubber axe", -1 ) },

		// The Oracle Will See You Now
		new ChoiceSpoiler(
			"choiceAdventure3", "Teleportitis",
			new String[] { "skip adventure", "randomly sink 100 meat", "make plus sign usable" } ),

		// Finger-Lickin'... Death.
		new ChoiceAdventure(
			"Beach", "choiceAdventure4", "South of the Border",
			new String[] { "small meat boost", "try for poultrygeist", "skip adventure" },
			new String[] { null, "1164", null } ),
		// Finger-Lickin'... Death.
		new Object[]{ new Integer(4), new Integer(1),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },
		new Object[]{ new Integer(4), new Integer(2),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Heart of Very, Very Dark Darkness
		new ChoiceAdventure(
			"MusSign", "choiceAdventure5", "Gravy Barrow",
			new String[] { "fight the fairy queen", "skip adventure" } ),

		// Darker Than Dark
		new ChoiceSpoiler(
			"choiceAdventure6", "Gravy Barrow",
			new String[] { "get Beaten Up", "skip adventure" } ),

		// Choice 7 is How Depressing

		// On the Verge of a Dirge -> Self Explanatory
		new ChoiceSpoiler(
			"choiceAdventure8", "Gravy Barrow",
			new String[] { "enter the chamber", "enter the chamber", "enter the chamber" } ),

		// Wheel In the Sky Keep on Turning: Muscle Position
		new ChoiceSpoiler(
			"choiceAdventure9", "Castle Wheel",
			new String[] { "Turn to mysticality", "Turn to moxie", "Leave at muscle" } ),

		// Wheel In the Sky Keep on Turning: Mysticality Position
		new ChoiceSpoiler(
			"choiceAdventure10", "Castle Wheel",
			new String[] { "Turn to Map Quest", "Turn to muscle", "Leave at mysticality" } ),

		// Wheel In the Sky Keep on Turning: Map Quest Position
		new ChoiceSpoiler(
			"choiceAdventure11", "Castle Wheel",
			new String[] { "Turn to moxie", "Turn to mysticality", "Leave at map quest" } ),

		// Wheel In the Sky Keep on Turning: Moxie Position
		new ChoiceSpoiler(
			"choiceAdventure12", "Castle Wheel",
			new String[] { "Turn to muscle", "Turn to map quest", "Leave at moxie" } ),

		// Choice 13 is unknown

		// A Bard Day's Night
		new ChoiceAdventure(
			"Knob", "choiceAdventure14", "Cobb's Knob Harem",
			new String[] { "Knob goblin harem veil", "Knob goblin harem pants", "small meat boost", "complete the outfit" },
			new String[] { "306", "305", null } ),

		// Yeti Nother Hippy
		new ChoiceAdventure(
			"Mountain", "choiceAdventure15", "eXtreme Slope",
			new String[] { "eXtreme mittens", "eXtreme scarf", "small meat boost", "complete the outfit" },
			new String[] { "399", "355", null } ),

		// Saint Beernard
		new ChoiceAdventure(
			"Mountain", "choiceAdventure16", "eXtreme Slope",
			new String[] { "snowboarder pants", "eXtreme scarf", "small meat boost", "complete the outfit" },
			new String[] { "356", "355", null } ),

		// Generic Teen Comedy
		new ChoiceAdventure(
			"Mountain", "choiceAdventure17", "eXtreme Slope",
			new String[] { "eXtreme mittens", "snowboarder pants", "small meat boost", "complete the outfit" },
			new String[] { "399", "356", null } ),

		// A Flat Miner
		new ChoiceAdventure(
			"Mountain", "choiceAdventure18", "Itznotyerzitz Mine",
			new String[] { "miner's pants", "7-Foot Dwarven mattock", "small meat boost", "complete the outfit" },
			new String[] { "361", "362", null } ),

		// 100% Legal
		new ChoiceAdventure(
			"Mountain", "choiceAdventure19", "Itznotyerzitz Mine",
			new String[] { "miner's helmet", "miner's pants", "small meat boost", "complete the outfit" },
			new String[] { "360", "361", null } ),

		// See You Next Fall
		new ChoiceAdventure(
			"Mountain", "choiceAdventure20", "Itznotyerzitz Mine",
			new String[] { "miner's helmet", "7-Foot Dwarven mattock", "small meat boost", "complete the outfit" },
			new String[] { "360", "362", null } ),

		// Under the Knife
		new ChoiceAdventure(
			"Town", "choiceAdventure21", "Sleazy Back Alley",
			new String[] { "switch genders", "skip adventure" } ),
		// Under the Knife
		new Object[]{ new Integer(21), new Integer(1),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },

		// The Arrrbitrator
		new ChoiceAdventure(
			"Island", "choiceAdventure22", "Pirate's Cove",
			new String[] { "eyepatch", "swashbuckling pants", "small meat boost", "complete the outfit" },
			new String[] { "224", "402", null } ),

		// Barrie Me at Sea
		new ChoiceAdventure(
			"Island",
			"choiceAdventure23",
			"Pirate's Cove",
			new String[] { "stuffed shoulder parrot", "swashbuckling pants", "small meat boost", "complete the outfit" },
			new String[] { "403", "402", null } ),

		// Amatearrr Night
		new ChoiceAdventure(
			"Island", "choiceAdventure24", "Pirate's Cove",
			new String[] { "stuffed shoulder parrot", "small meat boost", "eyepatch", "complete the outfit" },
			new String[] { "403", null, "224" } ),

		// Ouch! You bump into a door!
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure25", "Dungeon of Doom",
			new String[] { "magic lamp", "dead mimic", "skip adventure" },
			new String[] { "1273", "1267", null } ),
		// Ouch! You bump into a door!
		new Object[]{ new Integer(25), new Integer(1),
		  new AdventureResult( AdventureResult.MEAT, -50 ) },
		new Object[]{ new Integer(25), new Integer(2),
		  new AdventureResult( AdventureResult.MEAT, -5000 ) },

		// A Three-Tined Fork
		new ChoiceSpoiler(
			"choiceAdventure26", "Spooky Forest",
			new String[] { "muscle classes", "mysticality classes", "moxie classes" } ),

		// Footprints
		new ChoiceSpoiler(
			"choiceAdventure27", "Spooky Forest",
			new String[] { KoLCharacter.SEAL_CLUBBER, KoLCharacter.TURTLE_TAMER } ),

		// A Pair of Craters
		new ChoiceSpoiler(
			"choiceAdventure28", "Spooky Forest",
			new String[] { KoLCharacter.PASTAMANCER, KoLCharacter.SAUCEROR } ),

		// The Road Less Visible
		new ChoiceSpoiler(
			"choiceAdventure29", "Spooky Forest",
			new String[] { KoLCharacter.DISCO_BANDIT, KoLCharacter.ACCORDION_THIEF } ),

		// Choices 30 - 39 are unknown

		// The Effervescent Fray
		new ChoiceAdventure(
			"Plains", "choiceAdventure40", "Cola Wars",
			new String[] { "Cloaca-Cola fatigues", "Dyspepsi-Cola shield", "mysticality substats" },
			new String[] { "1328", "1329", null } ),

		// Smells Like Team Spirit
		new ChoiceAdventure(
			"Plains", "choiceAdventure41", "Cola Wars",
			new String[] { "Dyspepsi-Cola fatigues", "Cloaca-Cola helmet", "muscle substats" },
			new String[] { "1330", "1331", null } ),

		// What is it Good For?
		new ChoiceAdventure(
			"Plains", "choiceAdventure42", "Cola Wars",
			new String[] { "Dyspepsi-Cola helmet", "Cloaca-Cola shield", "moxie substats" },
			new String[] { "1326", "1327", null } ),

		// Choices 43 - 44 are unknown

		// Maps and Legends
		new ChoiceSpoiler(
			"choiceAdventure45", "Spooky Forest",
			new String[] { "Spooky Temple map", "skip adventure", "skip adventure" },
			new String[] { "74", null, null } ),

		// An Interesting Choice
		new ChoiceAdventure(
			"Woods", "choiceAdventure46", "Spooky Forest Vampire",
			new String[] { "moxie substats", "muscle substats", "vampire heart" },
			new String[] { null, null, "1518" } ),

		// Have a Heart
		new ChoiceAdventure(
			"Woods", "choiceAdventure47", "Spooky Forest Vampire Hunter",
			 new String[] { "bottle of used blood", "skip adventure and keep vampire hearts" },
			new String[] { "1523", "1518" } ),
		// Have a Heart
		// This trades all vampire hearts for an equal number of
		// bottles of used blood.
		new Object[]{ new Integer(47), new Integer(1),
		  ItemPool.get( ItemPool.VAMPIRE_HEART, 1 ) },

		// Choices 48 - 70 are violet fog adventures
		// Choice 71 is A Journey to the Center of Your Mind

		// Lording Over The Flies
		new ChoiceAdventure(
			"Island", "choiceAdventure72", "Frat House", new String[] { "around the world", "skip adventure" },
			new String[] { "1634", "1633" } ),
		// Lording Over The Flies
		// This trades all Spanish flies for around the worlds,
		// in multiples of 5.  Excess flies are left in inventory.
		new Object[]{ new Integer(72), new Integer(1),
		  ItemPool.get( ItemPool.SPANISH_FLY, 5 ) },

		// Don't Fence Me In
		new ChoiceAdventure(
			"Woods", "choiceAdventure73", "Whitey's Grove",
			new String[] { "muscle substats", "white picket fence", "wedding cake, white rice 3x (+2x w/ rice bowl)" },
			new String[] { null, "270", "262" } ),

		// The Only Thing About Him is the Way That He Walks
		new ChoiceAdventure(
			"Woods", "choiceAdventure74", "Whitey's Grove",
			new String[] { "moxie substats", "boxed wine", "mullet wig" },
			new String[] { null, "1005", "267" } ),

		// Rapido!
		new ChoiceAdventure(
			"Woods", "choiceAdventure75", "Whitey's Grove",
			new String[] { "mysticality substats", "white lightning", "white collar" },
			new String[] { null, "266", "1655" } ),

		// Junction in the Trunction
		new ChoiceAdventure(
			"Knob", "choiceAdventure76", "Knob Shaft",
			new String[] { "cardboard ore", "styrofoam ore", "bubblewrap ore" },
			new String[] { "1675", "1676", "1677" } ),

		// Minnesota Incorporeals
		new ChoiceSpoiler(
			"choiceAdventure77", "Haunted Billiard Room",
			new String[] { "moxie substats", "other options", "skip adventure" } ),

		// Broken
		new ChoiceSpoiler(
			"choiceAdventure78", "Haunted Billiard Room",
			new String[] { "other options", "muscle substats", "skip adventure" } ),

		// A Hustle Here, a Hustle There
		new ChoiceSpoiler(
			"choiceAdventure79", "Haunted Billiard Room",
			new String[] { "Spookyraven library key", "mysticality substats", "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceSpoiler(
			"choiceAdventure80", "Haunted Library",
			new String[] { "background history", "cooking recipe", "other options", "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceSpoiler(
			"choiceAdventure81", "Haunted Library",
			new String[] { "gallery quest", "cocktailcrafting recipe", "muscle substats", "skip adventure" } ),

		// One NightStand (simple white)
		new ChoiceAdventure(
			"Manor2", "choiceAdventure82", "Haunted Bedroom",
			new String[] { "old leather wallet", "muscle substats", "enter combat" },
			new String[] { "1917", null, null } ),

		// One NightStand (mahogany)
		new ChoiceAdventure(
			"Manor2", "choiceAdventure83", "Haunted Bedroom",
			new String[] { "old coin purse", "enter combat", "quest item" },
			new String[] { "1918", null, null } ),

		// One NightStand (ornate)
		new ChoiceAdventure(
			"Manor2", "choiceAdventure84", "Haunted Bedroom",
			new String[] { "small meat boost", "mysticality substats", "Lord Spookyraven's spectacles" },
			new String[] { null, null, "1916" } ),

		// One NightStand (simple wooden)
		new ChoiceAdventure(
			"Manor2", "choiceAdventure85", "Haunted Bedroom",
			new String[] { "moxie (ballroom key step 1)", "empty drawer (ballroom key step 2)", "enter combat", "ballroom key and moxie", "ballroom key and combat" } ),

		// History is Fun!
		new ChoiceSpoiler(
			"choiceAdventure86", "Haunted Library",
			new String[] { "Spookyraven Chapter 1", "Spookyraven Chapter 2", "Spookyraven Chapter 3" } ),

		// History is Fun!
		new ChoiceSpoiler(
			"choiceAdventure87", "Haunted Library",
			new String[] { "Spookyraven Chapter 4", "Spookyraven Chapter 5 (Gallery Quest)", "Spookyraven Chapter 6" } ),

		// Naughty, Naughty
		new ChoiceSpoiler(
			"choiceAdventure88", "Haunted Library",
			new String[] { "mysticality substats", "moxie substats", "Fettucini / Scarysauce" } ),

		new ChoiceSpoiler(
			"choiceAdventure89", "Haunted Gallery",
			new String[] { "Wolf Knight", "Snake Knight", "Dreams and Lights" } ),

		// Curtains
		new ChoiceAdventure(
			"Manor2", "choiceAdventure90", "Haunted Ballroom",
			new String[] { "enter combat", "moxie substats", "skip adventure" } ),

		// Louvre It or Leave It
		new ChoiceSpoiler(
			"choiceAdventure91", "Haunted Gallery",
			new String[] { "Enter the Drawing", "skip adventure" } ),

		// Choices 92 - 104 are Escher print adventures

		// Having a Medicine Ball
		new ChoiceAdventure(
			"Manor2", "choiceAdventure105", "Haunted Bathroom",
			new String[] { "mysticality substats", "other options", "guy made of bees" } ),

		// Strung-Up Quartet
		new ChoiceAdventure(
			"Manor2", "choiceAdventure106", "Haunted Ballroom",
			new String[] { "increase monster level", "decrease combat frequency", "increase item drops", "disable song" } ),

		// Bad Medicine is What You Need
		new ChoiceAdventure(
			"Manor2", "choiceAdventure107", "Haunted Bathroom",
			new String[] { "antique bottle of cough syrup", "tube of hair oil", "bottle of ultravitamins", "skip adventure" },
			new String[] { "2086", "2087", "2085", null } ),

		// Aww, Craps
		new ChoiceAdventure(
			"Town", "choiceAdventure108", "Sleazy Back Alley",
			new String[] { "moxie substats", "meat and moxie", "random effect", "skip adventure" } ),

		// Dumpster Diving
		new ChoiceAdventure(
			"Town", "choiceAdventure109", "Sleazy Back Alley",
			new String[] { "enter combat", "meat and moxie", "Mad Train wine" },
			new String[] { null, null, "564" } ),

		// The Entertainer
		new ChoiceAdventure(
			"Town", "choiceAdventure110", "Sleazy Back Alley",
			new String[] { "moxie substats", "moxie and muscle", "small meat boost", "skip adventure" } ),

		// Malice in Chains
		new ChoiceAdventure(
			"Knob", "choiceAdventure111", "Outskirts of The Knob",
			new String[] { "muscle substats", "muscle substats", "enter combat" } ),

		// Please, Hammer
		new ChoiceAdventure(
			"Town", "choiceAdventure112", "Sleazy Back Alley",
			new String[] { "accept hammer quest", "reject quest", "muscle substats" } ),

		// Knob Goblin BBQ
		new ChoiceAdventure(
			"Knob", "choiceAdventure113", "Outskirts of The Knob",
			new String[] { "complete cake quest", "enter combat", "get a random item" } ),

		// The Baker's Dilemma
		new ChoiceAdventure(
			"Manor1", "choiceAdventure114", "Haunted Pantry",
			new String[] { "accept cake quest", "reject quest", "moxie and meat" } ),

		// Oh No, Hobo
		new ChoiceAdventure(
			"Manor1", "choiceAdventure115", "Haunted Pantry",
			new String[] { "enter combat", "Good Karma", "mysticality, moxie, and meat" } ),

		// The Singing Tree
		new ChoiceAdventure(
			"Manor1", "choiceAdventure116", "Haunted Pantry",
			new String[] { "mysticality substats", "moxie substats", "random effect", "skip adventure" } ),

		// Tresspasser
		new ChoiceAdventure(
			"Manor1", "choiceAdventure117", "Haunted Pantry",
			new String[] { "enter combat", "mysticality substats", "get a random item" } ),

		// When Rocks Attack
		new ChoiceAdventure(
			"Knob", "choiceAdventure118", "Outskirts of The Knob",
			new String[] { "accept unguent quest", "skip adventure" } ),

		// Choice 119 is Check It Out Now

		// Ennui is Wasted on the Young
		new ChoiceAdventure(
			"Knob", "choiceAdventure120", "Outskirts of The Knob",
			new String[] { "muscle and Pumped Up", "ice cold Sir Schlitz", "moxie and lemon", "skip adventure" },
			new String[] { null, "41", "332", null } ),

		// Choice 121 is Next Sunday, A.D.
		// Choice 122 is unknown

		// At Least It's Not Full Of Trash
		new ChoiceSpoiler(
			"choiceAdventure123", "Hidden Temple",
			new String[] { "lose HP", "Unlock Quest Puzzle", "lose HP" } ),

		// Choice 124 is unknown

		// No Visible Means of Support
		new ChoiceSpoiler(
			"choiceAdventure125", "Hidden Temple",
			new String[] { "lose HP", "lose HP", "Unlock Hidden City" } ),

		// Sun at Noon, Tan Us
		new ChoiceAdventure(
			"Plains", "choiceAdventure126", "Palindome",
			new String[] { "moxie", "chance of more moxie", "sunburned" } ),

		// No sir, away!  A papaya war is on!
		new ChoiceSpoiler(
			"choiceAdventure127", "Palindome",
			new String[] { "3 papayas", "trade 3 papayas for stats", "stats" },
			new String[] { "498", null, null } ),
		// No sir, away!  A papaya war is on!
		new Object[]{ new Integer(127), new Integer(2),
		  ItemPool.get( ItemPool.PAPAYA, -3 ) },

		// Choice 128 is unknown

		// Do Geese See God?
		new ChoiceSpoiler(
			"choiceAdventure129", "Palindome",
			new String[] { "photograph of God", "skip adventure" },
			new String[] { "2259", null } ),
		// Do Geese See God?
		new Object[]{ new Integer(129), new Integer(1),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Rod Nevada, Vendor
		new ChoiceSpoiler(
			"choiceAdventure130", "Palindome",
			new String[] { "hard rock candy", "skip adventure" },
			new String[] { "2260", null } ),
		// Rod Nevada, Vendor
		new Object[]{ new Integer(130), new Integer(1),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Choice 131 is Dr. Awkward

		// Let's Make a Deal!
		new ChoiceAdventure(
			"Beach", "choiceAdventure132", "Desert (Pre-Oasis)",
			new String[] { "broken carburetor", "Unlock Oasis" },
			new String[] { "2316", null } ),
		// Let's Make a Deal!
		new Object[]{ new Integer(132), new Integer(1),
		  new AdventureResult( AdventureResult.MEAT, -5000 ) },

		// Choice 133 is unknown

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure(
			"Pyramid", "choiceAdventure134", "The Middle Chamber",
			new String[] { "Turn the wheel", "skip adventure" } ),

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure(
			"Pyramid", "choiceAdventure135", "The Middle Chamber",
			new String[] { "Turn the wheel", "skip adventure" } ),

		// Peace Wants Love
		new ChoiceAdventure(
			"Island", "choiceAdventure136", "Hippy Camp",
			new String[] { "filthy corduroys", "filthy knitted dread sack", "small meat boost", "complete the outfit" },
			new String[] { "213", "214", null } ),

		// An Inconvenient Truth
		new ChoiceAdventure(
			"Island", "choiceAdventure137", "Hippy Camp",
			new String[] { "filthy knitted dread sack", "filthy corduroys", "small meat boost", "complete the outfit" },
			new String[] { "214", "213", null } ),

		// Purple Hazers
		new ChoiceAdventure(
			"Island", "choiceAdventure138", "Frat House",
			new String[] { "orcish cargo shorts", "Orcish baseball cap", "homoerotic frat-paddle", "complete the outfit" },
			new String[] { "240", "239", "241" } ),

		// Bait and Switch
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure139", "War Hippies",
			new String[] { "muscle substats", "ferret bait", "enter combat" },
			new String[] { null, "2041", null } ),

		// The Thin Tie-Dyed Line
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure140", "War Hippies",
			new String[] { "water pipe bombs", "moxie substats", "enter combat" },
			new String[] { "2348", null, null } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure141", "War Hippies",
			new String[] { "mysticality substats", "get some hippy food", "waste a turn" } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure142", "War Hippies",
			new String[] { "mysticality substats", "get some hippy food", "start the war" } ),

		// Catching Some Zetas
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure143", "War Fraternity",
			new String[] { "muscle substats", "sake bombs", "enter combat" },
			new String[] { null, "2067", null } ),

		// One Less Room Than In That Movie
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure144", "War Fraternity",
			new String[] { "moxie substats", "beer bombs", "enter combat" },
			new String[] { null, "2350", null } ),

		// Fratacombs
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure145", "War Fraternity",
			new String[] { "muscle substats", "get some frat food", "waste a turn" },
			new String[] { null, null, null } ),

		// Fratacombs
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure146", "War Fraternity",
			new String[] { "muscle substats", "get some frat food", "start the war" } ),

		// Cornered!
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure147", "Isle War Barn",
			new String[] { "Open The Granary (meat)", "Open The Bog (stench)", "Open The Pond (cold)" } ),

		// Cornered Again!
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure148", "Isle War Barn",
			new String[] { "Open The Back 40 (hot)", "Open The Family Plot (spooky)" } ),

		// ow Many Corners Does this Stupid Barn Have!?
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure149", "Isle War Barn",
			new String[] { "Open The Shady Thicket (booze)", "Open The Other Back 40 (sleaze)" } ),

		// Choice 150 is Another Adventure About BorderTown

		// Adventurer, $1.99
		new ChoiceAdventure(
			"Plains", "choiceAdventure151", "Fun House",
			new String[] { "fight the clownlord", "skip adventure" } ),

		// Lurking at the Threshold
		new ChoiceSpoiler(
			"choiceAdventure152", "Fun House",
			new String[] { "fight the clownlord", "skip adventure" } ),

		// Turn Your Head and Coffin
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure153", "Defiled Alcove",
			new String[] { "muscle substats", "small meat boost", "half-rotten brain", "skip adventure" },
			new String[] { null, null, "2562", null } ),

		// Choice 154 used to be Doublewide

		// Skull, Skull, Skull
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure155", "Defiled Nook",
			new String[] { "moxie substats", "small meat boost", "rusty bonesaw", "skip adventure" },
			new String[] { null, null, "2563", null } ),

		// Choice 156 used to be Pileup

		// Urning Your Keep
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure157", "Defiled Niche",
			new String[] { "mysticality substats", "plus-sized phylactery", "small meat boost", "skip adventure" },
			new String[] { null, "2564", null, null } ),

		// Choice 158 used to be Lich in the Niche
		// Choice 159 used to be Go Slow Past the Drawers
		// Choice 160 used to be Lunchtime

		// Choice 161 is Bureaucracy of the Damned

		// Between a Rock and Some Other Rocks
		new ChoiceSpoiler(
			"choiceAdventure162", "Goatlet",
			new String[] { "Open Goatlet", "skip adventure" } ),

		// Melvil Dewey Would Be Ashamed
		new ChoiceAdventure(
			"Manor1", "choiceAdventure163", "Haunted Library",
			new String[] { "Necrotelicomnicon", "Cookbook of the Damned", "Sinful Desires", "skip adventure" },
			new String[] { "2494", "2495", "2496", null } ),

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
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure164", "Pleasure Dome",
			new String[] { "muscle substats", "MP & Spirit of Alph", "enter combat" } ),

		// Beyond Any Measure
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure165", "Pleasure Dome",
			new String[] { "Rat-Faced -> Night Vision", "Bats in the Belfry -> Good with the Ladies", "mysticality       substats", "skip adventure" } ),

		// Death is a Boat
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure166", "Pleasure Dome",
			new String[] { "No Vertigo -> S.T.L.T.", "moxie substats", "Unusual Fashion Sense -> albatross necklace" },
			new String[] { "2652", null, "2659" } ),

		// It's a Fixer-Upper
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure167", "Moulder Mansion",
			new String[] { "enter combat", "mysticality substats", "HP & MP & Bats in the Belfry" } ),

		// Midst the Pallor of the Parlor
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure168", "Moulder Mansion",
			new String[] { "moxie substats", "Spirit of Alph -> Feelin' Philosophical", "Rat-Faced -> Unusual Fashion Sense" } ),

		// A Few Chintz Curtains, Some Throw Pillows, It
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure169", "Moulder Mansion",
			new String[] { "Night Vision -> flask of Amontillado", "muscle substats", "Dancing Prowess -> fancy ball mask" },
			new String[] { "2661", null, "2662" } ),

		// La Vie Boheme
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure170", "Rogue Windmill",
			new String[] { "HP & Rat-Faced", "enter combat", "moxie substats" } ),

		// Backstage at the Rogue Windmill
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure171", "Rogue Windmill",
			new String[] { "Bats in the Belfry -> No Vertigo", "muscle substats", "Spirit of Alph -> Dancing Prowess" } ),

		// Up in the Hippo Room
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure172", "Rogue Windmill",
			new String[] { "Good with the Ladies -> Can-Can skirt", "Feelin' Philosophical -> not-a-pipe", "mysticality substats" },
			new String[] { "2663", "2660", null } ),

		// Choice 173 is The Last Stand, Man
		// Choice 174 is The Last Stand, Bra
		// Choice 175-176 are unknown

		// The Blackberry Cobbler
		new ChoiceAdventure(
			"Woods", "choiceAdventure177", "Black Forest",
			new String[] { "blackberry slippers", "blackberry moccasins", "blackberry combat boots", "blackberry galoshes", "skip adventure" },
			new String[] { "2705", "2706", "2707", "4659", null } ),
		// The Blackberry Cobbler
		new Object[]{ new Integer(177), new Integer(1),
		  ItemPool.get( ItemPool.BLACKBERRY, -10 ) },
		new Object[]{ new Integer(177), new Integer(2),
		  ItemPool.get( ItemPool.BLACKBERRY, -10 ) },
		new Object[]{ new Integer(177), new Integer(3),
		  ItemPool.get( ItemPool.BLACKBERRY, -10 ) },
		new Object[]{ new Integer(177), new Integer(4),
		  ItemPool.get( ItemPool.BLACKBERRY, -10 ) },

		// Hammering the Armory
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure178", "Airship Shirt",
			new String[] { "bronze breastplate", "skip adventure" },
			new String[] { "2126", null } ),

		// Choice 179 is unknown

		// A Pre-War Dresser Drawer, Pa!
		new ChoiceAdventure(
			"Plains", "choiceAdventure180", "Palindome Shirt",
			new String[] { "Ye Olde Navy Fleece", "skip adventure" },
			new String[] { "2125", null } ),

		// Chieftain of the Flies
		new ChoiceAdventure(
			"Island", "choiceAdventure181", "Frat House (Stone Age)",
			new String[] { "around the world", "skip adventure" },
			new String[] { "1634", "1633" } ),
		// Chieftain of the Flies
		// This trades all Spanish flies for around the worlds,
		// in multiples of 5.  Excess flies are left in inventory.
		new Object[]{ new Integer(181), new Integer(1),
		  ItemPool.get( ItemPool.SPANISH_FLY, 5 ) },

		// Random Lack of an Encounter
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure182", "Fantasy Airship",
			new String[] { "enter combat", "Penultimate Fantasy chest", "stats" },
			new String[] { null, "604", null } ),

		// That Explains All The Eyepatches
		// Dynamically calculate options based on mainstat
		new ChoiceAdventure(
			"Island", "choiceAdventure184", "Barrrney's Barrr",
			null ),

		// Yes, You're a Rock Starrr
		new ChoiceAdventure(
			"Island", "choiceAdventure185", "Barrrney's Barrr",
			new String[] { "base booze", "mixed drinks", "stats" } ),

		// A Test of Testarrrsterone
		new ChoiceAdventure(
			"Island", "choiceAdventure186", "Barrrney's Barrr",
			new String[] { "stats", "drunkenness and stats", "moxie" } ),

		// Choice 187 is Arrr You Man Enough?

		// The Infiltrationist
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure188", "Frathouse Blueprints",
			new String[] { "frat boy ensemble", "mullet wig and briefcase", "frilly skirt and hot wings" } ),

		//  O Cap'm, My Cap'm
		new Object[]{ new Integer(189), new Integer(1),
		  new AdventureResult( AdventureResult.MEAT, -977 ) },

		// Choice 190 is unknown

		// Chatterboxing
		new ChoiceAdventure(
			"Island", "choiceAdventure191", "F'c'le",
			new String[] { "moxie substats", "use valuable trinket to banish, or lose hp", "muscle substats", "mysticality substats", "use valuable trinket to banish, or moxie", "use valuable trinket to banish, or muscle", "use valuable trinket to banish, or mysticality", "use valuable trinket to banish, or mainstat" } ),

		// Choice 192 is unknown
		// Choice 193 is Modular, Dude

		// Somewhat Higher and Mostly Dry
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure197", "A Maze of Sewer Tunnels",
			new String[] { "take the tunnel", "sewer gator", "turn the valve" } ),

		// Disgustin' Junction
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure198", "A Maze of Sewer Tunnels",
			new String[] { "take the tunnel", "giant zombie goldfish", "open the grate" } ),

		// The Former or the Ladder
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure199", "A Maze of Sewer Tunnels",
			new String[] { "take the tunnel", "C. H. U. M.", "head down the ladder" } ),

		// Enter The Hoboverlord
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure200", "Hobopolis Town Square",
			new String[] { "enter combat with Hodgman", "skip adventure" } ),

		// Home, Home in the Range
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure201", "Burnbarrel Blvd.",
			new String[] { "enter combat with Ol' Scratch", "skip adventure" } ),

		// Bumpity Bump Bump
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure202", "Exposure Esplanade",
			new String[] { "enter combat with Frosty", "skip adventure" } ),

		// Deep Enough to Dive
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure203", "The Heap",
			new String[] { "enter combat with Oscus", "skip adventure" } ),

		// Welcome To You!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure204", "The Ancient Hobo Burial Ground",
			new String[] { "enter combat with Zombo", "skip adventure" } ),

		// Van, Damn
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure205", "The Purple Light District",
			new String[] { "enter combat with Chester", "skip adventure" } ),

		// Getting Tired
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure206", "Burnbarrel Blvd.",
			new String[] { "start tirevalanche", "add tire to stack", "skip adventure" } ),

		// Hot Dog! I Mean... Door!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure207", "Burnbarrel Blvd.",
			new String[] { "increase hot hobos & get clan meat", "skip adventure" } ),

		// Ah, So That's Where They've All Gone
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure208", "The Ancient Hobo Burial Ground",
			new String[] { "increase spooky hobos & decrease stench", "skip adventure" } ),

		// Choice 209 is Timbarrrr!
		// Choice 210 is Stumped

		// Despite All Your Rage
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure211", "A Maze of Sewer Tunnels",
			new String[] { "gnaw through the bars" } ),

		// Choice 212 is also Despite All Your Rage, apparently after you've already
		// tried to wait for rescue?
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure212", "A Maze of Sewer Tunnels",
			new String[] { "gnaw through the bars" } ),

		// Piping Hot
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure213", "Burnbarrel Blvd.",
			new String[] { "increase sleaze hobos & decrease heat", "skip adventure" } ),

		// You vs. The Volcano
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure214", "The Heap",
			new String[] { "decrease stench hobos & increase stench", "skip adventure" } ),

		// Piping Cold
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure215", "Exposure Esplanade",
			new String[] { "decrease heat", "decrease sleaze hobos", "increase number of icicles" } ),

		// The Compostal Service
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure216", "The Heap",
			new String[] { "decrease stench & spooky", "skip adventure" } ),

		// There Goes Fritz!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure217", "Exposure Esplanade",
			new String[] { "yodel a little", "yodel a lot", "yodel your heart out" } ),

		// I Refuse!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure218", "The Heap",
			new String[] { "explore the junkpile", "skip adventure" } ),

		// The Furtivity of My City
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure219", "The Purple Light District",
			new String[] { "fight sleaze hobo", "increase stench", "increase sleaze hobos & get clan meat" } ),

		// Returning to the Tomb
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure220", "The Ancient Hobo Burial Ground",
			new String[] { "increase spooky hobos & get clan meat", "skip adventure" } ),

		// A Chiller Night
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure221", "The Ancient Hobo Burial Ground",
			new String[] { "study the dance moves", "dance with hobo zombies", "skip adventure" } ),

		// A Chiller Night (2)
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure222", "The Ancient Hobo Burial Ground",
			new String[] { "dance with hobo zombies", "skip adventure" } ),

		// Getting Clubbed
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure223", "The Purple Light District",
			new String[] { "try to get inside", "try to bamboozle the crowd", "try to flimflam the crowd" } ),

		// Exclusive!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure224", "The Purple Light District",
			new String[] { "fight sleaze hobo", "start barfight", "gain stats" } ),

		// Attention -- A Tent!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure225", "Hobopolis Town Square",
			new String[] { "perform on stage", "join the crowd", "skip adventure" } ),

		// Choice 226 is Here You Are, Up On Stage (use the same system as 211 & 212)
		// Choice 227 is Working the Crowd (use the same system as 211 & 212)

		// Choices 228 & 229 are unknown

		// Mind Yer Binder
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure230", "Hobopolis Town Square",
			new String[] { "hobo code binder", "skip adventure" },
			new String[] { "3220", null } ),
		// Mind Yer Binder
		new Object[]{ new Integer(230), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -30 ) },

		// Choices 231-271 are subchoices of Choice 272

		// Food, Glorious Food
		new ChoiceSpoiler(
			"choiceAdventure235", "Hobopolis Marketplace",
			new String[] { "muscle food", "mysticality food", "moxie food" } ),

		// Booze, Glorious Booze
		new ChoiceSpoiler(
			"choiceAdventure240", "Hobopolis Marketplace",
			new String[] { "muscle booze", "mysticality booze", "moxie booze" } ),

		// The Guy Who Carves Driftwood Animals
		new Object[]{ new Integer(247), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },

		// A Hattery
		new ChoiceSpoiler(
			"choiceAdventure250", "Hobopolis Marketplace",
			new String[] { "crumpled felt fedora", "battered old top-hat", "shapeless wide-brimmed hat" },
			new String[] { "3328", "3329", "3330" } ),
		// A Hattery
		new Object[]{ new Integer(250), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -250 ) },
		new Object[]{ new Integer(250), new Integer(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -150 ) },
		new Object[]{ new Integer(250), new Integer(3),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -200 ) },

		// A Pantry
		new ChoiceSpoiler(
			"choiceAdventure251", "Hobopolis Marketplace",
			new String[] { "mostly rat-hide leggings", "hobo dungarees", "old patched suit-pants" },
			new String[] { "3331", "3332", "3333" } ),
		// A Pantry
		new Object[]{ new Integer(251), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -200 ) },
		new Object[]{ new Integer(251), new Integer(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -150 ) },
		new Object[]{ new Integer(251), new Integer(3),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -250 ) },

		// Hobo Blanket Bingo
		new ChoiceSpoiler(
			"choiceAdventure252", "Hobopolis Marketplace",
			new String[] { "old soft shoes", "hobo stogie", "rope with some soap on it" },
			new String[] { "3140", "3334", "3335" } ),
		// Hobo Blanket Bingo
		new Object[]{ new Integer(252), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -250 ) },
		new Object[]{ new Integer(252), new Integer(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -200 ) },
		new Object[]{ new Integer(252), new Integer(3),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -150 ) },

		// Black-and-Blue-and-Decker
		new ChoiceSpoiler(
			"choiceAdventure255", "Hobopolis Marketplace",
			new String[] { "sharpened hubcap", "very large caltrop", "The Six-Pack of Pain" },
			new String[] { "3339", "3340", "3341" } ),
		// Black-and-Blue-and-Decker
		new Object[]{ new Integer(255), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },
		new Object[]{ new Integer(255), new Integer(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },
		new Object[]{ new Integer(255), new Integer(3),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },

		// Instru-mental
		new Object[]{ new Integer(258), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -99 ) },

		// We'll Make Great...
		new ChoiceSpoiler(
			"choiceAdventure259", "Hobopolis Marketplace",
			new String[] { "hobo monkey", "stats", "enter combat" } ),

		// Everybody's Got Something To Hide
		new Object[]{ new Integer(261), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -1000 ) },

		// Tanning Salon
		new ChoiceSpoiler(
			"choiceAdventure264", "Hobopolis Marketplace",
			new String[] { "20 adv of +50% moxie", "20 adv of +50% mysticality" } ),
		// Tanning Salon
		new Object[]{ new Integer(264), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },
		new Object[]{ new Integer(264), new Integer(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Let's All Go To The Movies
		new ChoiceSpoiler(
			"choiceAdventure267", "Hobopolis Marketplace",
			new String[] { "20 adv of +5 spooky resistance", "20 adv of +5 sleaze resistance" } ),
		// Let's All Go To The Movies
		new Object[]{ new Integer(267), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },
		new Object[]{ new Integer(267), new Integer(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// It's Fun To Stay There
		new ChoiceSpoiler(
			"choiceAdventure268", "Hobopolis Marketplace",
			new String[] { "20 adv of +5 stench resistance", "20 adv of +50% muscle" } ),
		// It's Fun To Stay There
		new Object[]{ new Integer(268), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },
		new Object[]{ new Integer(268), new Integer(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Marketplace Entrance
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure272", "Hobopolis Town Square",
			new String[] { "enter marketplace", "skip adventure" } ),

		// Piping Cold
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure273", "Exposure Esplanade",
			new String[] { "frozen banquet", "increase cold hobos & get clan meat", "skip adventure" },
			new String[] { "3338", null, null } ),

		// Choice 274 is Tattoo Redux, a subchoice of Choice 272 when
		// you've started a tattoo

		// Choice 275 is Triangle, Man, a subchoice of Choice 272 when
		// you've already purchased your class instrument
		// Triangle, Man
		new Object[]{ new Integer(275), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },

		// Choices 278-290 are llama lama gong related choices

		// The Gong Has Been Bung
		new ChoiceSpoiler(
			"choiceAdventure276", "Gong",
			new String[] { "3 adventures", "12 adventures", "15 adventures" } ),

		// Welcome Back!
		new ChoiceSpoiler(
			"choiceAdventure277", "Gong",
			new String[] { "finish journey", "also finish journey" } ),

		// Enter the Roach
		new ChoiceSpoiler(
			"choiceAdventure278", "Gong",
			new String[] { "muscle substats", "mysticality substats", "moxie substats" } ),

		// It's Nukyuhlur - the 'S' is Silent.
		new ChoiceSpoiler(
			"choiceAdventure279", "Gong",
			new String[] { "moxie substats", "muscle substats", "gain MP" } ),

		// Eek! Eek!
		new ChoiceSpoiler(
			"choiceAdventure280", "Gong",
			new String[] { "mysticality substats", "muscle substats", "gain MP" } ),

		// A Meta-Metamorphosis
		new ChoiceSpoiler(
			"choiceAdventure281", "Gong",
			new String[] { "moxie substats", "mysticality substats", "gain MP" } ),

		// You've Got Wings, But No Wingman
		new ChoiceSpoiler(
			"choiceAdventure282", "Gong",
			new String[] { "+30% muscle", "+10% all stats", "+30 ML" } ),

		// Time Enough at Last!
		new ChoiceSpoiler(
			"choiceAdventure283", "Gong",
			new String[] { "+30% muscle", "+10% all stats", "+50% item drops" } ),

		// Scavenger Is Your Middle Name
		new ChoiceSpoiler(
			"choiceAdventure284", "Gong",
			new String[] { "+30% muscle", "+50% item drops", "+30 ML" } ),

		// Bugging Out
		new ChoiceSpoiler(
			"choiceAdventure285", "Gong",
			new String[] { "+30% mysticality", "+30 ML", "+10% all stats" } ),

		// A Sweeping Generalization
		new ChoiceSpoiler(
			"choiceAdventure286", "Gong",
			new String[] { "+50% item drops", "+10% all stats", "+30% mysticality" } ),

		// In the Frigid Aire
		new ChoiceSpoiler(
			"choiceAdventure287", "Gong",
			new String[] { "+30 ML", "+30% mysticality", "+50% item drops" } ),

		// Our House
		new ChoiceSpoiler(
			"choiceAdventure288", "Gong",
			new String[] { "+30 ML", "+30% moxie", "+10% all stats" } ),

		// Workin' For The Man
		new ChoiceSpoiler(
			"choiceAdventure289", "Gong",
			new String[] { "+30 ML", "+30% moxie", "+50% item drops" } ),

		// The World's Not Fair
		new ChoiceSpoiler(
			"choiceAdventure290", "Gong",
			new String[] { "+30% moxie", "+10% all stats", "+50% item drops" } ),

		// A Tight Squeeze
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure291", "Burnbarrel Blvd.",
			new String[] { "jar of squeeze", "skip adventure" },
			new String[] { "3399", null } ),
		// A Tight Squeeze - jar of squeeze
		new Object[]{ new Integer(291), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Cold Comfort
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure292", "Exposure Esplanade",
			new String[] { "bowl of fishysoisse", "skip adventure" },
			new String[] { "3400", null } ),
		// Cold Comfort - bowl of fishysoisse
		new Object[]{ new Integer(292), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Flowers for You
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure293", "The Ancient Hobo Burial Ground",
			new String[] { "deadly lampshade", "skip adventure" },
			new String[] { "3401", null } ),
		// Flowers for You - deadly lampshade
		new Object[]{ new Integer(293), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Maybe It's a Sexy Snake!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure294", "The Purple Light District",
			new String[] { "lewd playing card", "skip adventure" },
			new String[] { "3403", null } ),
		// Maybe It's a Sexy Snake! - lewd playing card
		new Object[]{ new Integer(294), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Juicy!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure295", "The Heap",
			new String[] { "concentrated garbage juice", "skip adventure" },
			new String[] { "3402", null } ),
		// Juicy! - concentrated garbage juice
		new Object[]{ new Integer(295), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Choice 296 is Pop!

		// Gravy Fairy Ring
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure297", "Haiku Dungeon",
			new String[] { "mushrooms", "fairy gravy boat", "skip adventure" },
			new String[] { null, "80", null } ),

		// In the Shade
		new ChoiceAdventure(
			"The Sea", "choiceAdventure298", "An Octopus's Garden",
			new String[] { "plant seeds", "skip adventure" } ),

		// Down at the Hatch
		new ChoiceAdventure(
			"The Sea", "choiceAdventure299", "The Wreck of the Edgar Fitzsimmons",
			new String[] { "release creatures", "skip adventure" } ),

		// Choice 300 is Merry Crimbo!
		// Choice 301 is And to All a Good Night
		// Choice 302 is unknown
		// Choice 303 is You've Hit Bottom

		// A Vent Horizon
		new ChoiceAdventure(
			"The Sea", "choiceAdventure304", "The Marinara Trench",
			new String[] { "bubbling tempura batter", "skip adventure" },
			new String[] { "3681", null } ),
		// A Vent Horizon
		new Object[]{ new Integer(304), new Integer(1),
		  new AdventureResult( AdventureResult.MP, -200 ) },

		// There is Sauce at the Bottom of the Ocean
		new ChoiceAdventure(
			"The Sea", "choiceAdventure305", "The Marinara Trench",
			new String[] { "globe of Deep Sauce", "skip adventure" },
			new String[] { "3682", null } ),
		// There is Sauce at the Bottom of the Ocean
		new Object[]{ new Integer(305), new Integer(1),
		  ItemPool.get( ItemPool.MERKIN_PRESSUREGLOBE, -1 ) },

		// Choice 306 is [Grandpa Mine Choice]
		// Choice 307 is Ode to the Sea
		// Choice 308 is Boxing the Juke

		// Barback
		new ChoiceAdventure(
			"The Sea", "choiceAdventure309", "The Dive Bar",
			new String[] { "seaode", "skip adventure" },
			new String[] { "3773", null } ),

		// The Economist of Scales
		new ChoiceAdventure(
			"The Sea", "choiceAdventure310", "Madness Reef",
			new String[] { "rough fish scale", "pristine fish scale", "skip adventure" },
			new String[] { "3487", "3488", null } ),
		// The Economist of Scales
		// This trades 10 dull fish scales in.
		new Object[]{ new Integer(310), new Integer(1),
		  new AdventureResult( "dull fish scale", -10 ) },
		new Object[]{ new Integer(310), new Integer(2),
		  new AdventureResult( "rough fish scale", -10 ) },

		// Heavily Invested in Pun Futures
		new ChoiceAdventure(
			"The Sea", "choiceAdventure311", "Madness Reef",
			new String[] { "The Economist of Scales", "skip adventure" } ),

		// Choice 312 is unknown
		// Choice 313 is unknown
		// Choice 314 is unknown
		// Choice 315 is unknown
		// Choice 316 is unknown

		// Choice 317 is No Man, No Hole
		// Choice 318 is C'mere, Little Fella
		// Choice 319 is Turtles of the Universe
		// Choice 320 is A Rolling Turtle Gathers No Moss
		// Choice 321 is Boxed In
		// Choice 322 is Capital!

		// Choice 323 is unknown
		// Choice 324 is unknown
		// Choice 325 is unknown

		// Showdown
		new ChoiceAdventure(
			"Clan Basement", "choiceAdventure326", "The Slime Tube",
			new String[] { "enter combat with Mother Slime", "skip adventure" } ),

		// Choice 327 is Puttin' it on Wax
		// Choice 328 is Never Break the Chain
		// Choice 329 is Don't Be Alarmed, Now

		// A Shark's Chum
		new ChoiceAdventure(
			"Manor1", "choiceAdventure330", "Haunted Billiards Room",
			new String[] { "stats", "cube of billiard chalk" },
			new String[] { null, "3965" } ),

		// Choice 331 is Like That Time in Tortuga
		// Choice 332 is More eXtreme Than Usual
		// Choice 333 is Cleansing your Palette
		// Choice 334 is O Turtle Were Art Thou
		// Choice 335 is Blue Monday
		// Choice 336 is Jewel in the Rough

		// Engulfed!
		new ChoiceAdventure(
			"Clan Basement", "choiceAdventure337", "The Slime Tube",
			new String[] { "+1 rusty -> slime-covered item conversion", "raise area ML", "skip adventure" } ),

		// Choice 338 is Duel Nature
		// Choice 339 is Kick the Can
		// Choice 340 is Turtle in peril
		// Choice 341 is Nantucket Snapper
		// Choice 342 is The Horror...
		// Choice 343 is Turtles All The Way Around
		// Choice 344 is Silent Strolling
		// Choice 345 is Training Day

		// Choice 346 is Soup For You
		// Choice 347 is Yes, Soup For You
		// Choice 348 is Souped Up
		// Choice 349 is The Primordial Directive
		// Choice 350 is Soupercharged

		// Choice 351 is Beginner's Luck
		// Choice 352 is Savior Faire
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure352", "Seaside Megalopolis",
			new String[] { "Moxie -> Bad Reception Down Here", "Muscle -> A Diseased Procurer", "Mysticality -> Give it a Shot" }),

		// Bad Reception Down Here
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure353", "Seaside Megalopolis",
			new String[] { "Indigo Party Invitation", "Violet Hunt Invitation" },
			new String[] { "4060", "4061" } ),

		// You Can Never Be Too Rich or Too in the Future
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure354", "Seaside Megalopolis",
			new String[] { "Moxie", "Serenity" } ),

		// I'm on the Hunt, I'm After You
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure355", "Seaside Megalopolis",
			new String[] { "Stats", "Phairly Pheromonal" } ),

		// Choice 356 is A Diseased Procurer
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure356", "Seaside Megalopolis",
			new String[] { "Blue Milk Club Card", "Mecha Mayhem Club Card" },
			new String[] { "4062", "4063" } ),

		// Choice 357 is Painful, Circuitous Logic
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure357", "Seaside Megalopolis",
			new String[] { "Muscle", "Nano-juiced" } ),

		// Choice 358 is Brings All the Boys to the Blue Yard
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure358", "Seaside Megalopolis",
			new String[] { "Stats", "Dance Interpreter" } ),

		// Choice 359 is unknown

		// Choice 360 is Cavern Entrance

		// Give it a Shot
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure361", "Seaside Megalopolis",
			new String[] { "'Smuggler Shot First' Button", "Spacefleet Communicator Badge" },
			new String[] { "4064", "4065" } ),

		// Choice 362 is A Bridge Too Far
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure362", "Seaside Megalopolis",
			new String[] { "Stats", "Meatwise" } ),

		// Choice 363 is Does This Bug You? Does This Bug You?
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure363", "Seaside Megalopolis",
			new String[] { "Mysticality", "In the Saucestream" } ),

		// Choice 364 is 451 Degrees! Burning Down the House!
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure364", "Seaside Megalopolis",
			new String[] { "Moxie", "Supreme Being Glossary", "Muscle" },
			new String[] { null, "4073", null } ),

		// Choice 365 is None Shall Pass
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure364", "Seaside Megalopolis",
			new String[] { "Muscle", "multi-pass" },
			new String[] { null, "4074" } ),

		// Choice 366 is Entrance to the Forgotten City
		// Choice 367 is Ancient Temple (unlocked)
		// Choice 368 is City Center
		// Choice 369 is North Side of the City
		// Choice 370 is East Side of the City
		// Choice 371 is West Side of the City
		// Choice 372 is An Ancient Well
		// Choice 373 is Northern Gate
		// Choice 374 is An Ancient Tower
		// Choice 375 is Northern Abandoned Building
		// Choice 376 is Ancient Temple
		// Choice 377 is Southern Abandoned Building
		// Choice 378 is Storehouse
		// Choice 379 is Northern Building (Basement)
		// Choice 380 is Southern Building (Upstairs)
		// Choice 381 is Southern Building (Basement)
		// Choice 382 is Catacombs Entrance
		// Choice 383 is Catacombs Junction
		// Choice 384 is Catacombs Dead-End
		// Choice 385 is Sore of an Underground Lake
		// Choice 386 is Catacombs Machinery

		// Choice 387 is Time Isn't Holding Up; Time is a Doughnut
		// Choice 388 is Extra Savoir Faire
		// Choice 389 is The Unbearable Supremeness of Being
		// Choice 390 is A Winning Pass
		// Choice 391 is OMG KAWAIII
		// Choice 392 is The Elements of Surprise . . .

		// The Collector
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure393", "big bumboozer marble",
			new String[] { "1 of each marble -> 32768 Meat", "skip adventure" } ),

		// Choice 394 is Hellevator Music
		// Choice 395 is Rumble On

		// Don't Hold a Grudge
		new ChoiceAdventure(
			"Manor2", "choiceAdventure402", "Haunted Bathroom",
			new String[] { "muscle substats", "mysticality substats", "moxie substats" } ),

		// Picking Sides
		new ChoiceAdventure(
			"The Sea", "choiceAdventure403", "Skate Park",
			new String[] { "skate blade", "brand new key" },
			new String[] { "4213", "4206" } ),

		// Choice 409 is The Island Barracks
		//	1 = only option
		// Choice 410 is A Short Hallway
		//	1 = left, 2 = right, 3 = exit
		// Choice 411 is Hallway Left
		//	1 = kitchen, 2 = dining room, 3 = storeroom, 4 = exit
		// Choice 412 is Hallway Right
		//	1 = bedroom, 2 = library, 3 = parlour, 4 = exit
		// Choice 413 is Kitchen
		//	1 = cupboards, 2 = pantry, 3 = fridges, 4 = exit
		// Choice 414 is Dining Room
		//	1 = tables, 2 = sideboard, 3 = china cabinet, 4 = exit
		// Choice 415 is Store Room
		//	1 = crates, 2 = workbench, 3 = gun cabinet, 4 = exit
		// Choice 416 is Bedroom
		//	1 = beds, 2 = dressers, 3 = bathroom, 4 = exit
		// Choice 417 is Library
		//	1 = bookshelves, 2 = chairs, 3 = chess set, 4 = exit
		// Choice 418 is Parlour
		//	1 = pool table, 2 = bar, 3 = fireplace, 4 = exit

		// Choice 423 is A Wrenching Encounter
		// Choice 424 is Get Your Bolt On, Michael
		// Choice 425 is Taking a Proper Gander
		// Choice 426 is It's Electric, Boogie-oogie-oogie
		// Choice 427 is A Voice Crying in the Crimbo Factory
		// Choice 428 is Disguise the Limit
		// Choice 429 is Diagnosis: Hypnosis
		// Choice 430 is Secret Agent Penguin
		// Choice 431 is Zapatos Con Crete
		// Choice 432 is Don We Now Our Bright Apparel
		// Choice 433 is Everything is Illuminated?
		// Choice 435 is Season's Beatings
		// Choice 436 is unknown
		// Choice 437 is Flying In Circles

		// From Little Acorns...
		new Object[]{ new Integer(438), new Integer(1),
		  ItemPool.get( ItemPool.UNDERWORLD_ACORN, -1 ) },

		// Choice 439 is unknown
		// Choice 440 is Puttin' on the Wax
		// Choice 441 is The Mad Tea Party
		// Choice 442 is A Moment of Reflection
		// Choice 443 is Chess Puzzle

		// Choice 444 is The Field of Strawberries (Seal Clubber)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure444", "Reflection of Map (Seal Clubber)",
			new String[] { "walrus ice cream", "yellow matter custard" },
			new String[] { "4510", "4517" } ),

		// Choice 445 is The Field of Strawberries (Pastamancer)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure445", "Reflection of Map (Pastamancer)",
			new String[] { "eggman noodles", "yellow matter custard" },
			new String[] { "4512", "4517" } ),

		// Choice 446 is The Caucus Racetrack (Accordion Thief)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure446", "Reflection of Map (Accordion Thief)",
			new String[] { "missing wine", "delicious comfit?" },
			new String[] { "4516", "4518" } ),

		// Choice 447 is The Caucus Racetrack (Sauceror)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure447", "Reflection of Map (Sauceror)",
			new String[] { "Vial of <i>jus de larmes</i>", "delicious comfit?" },
			new String[] { "4513", "4518" } ),

		// Choice 448 is The Croquet Grounds (Turtle Tamer)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure448", "Reflection of Map (Turtle Tamer)",
			new String[] { "beautiful soup", "fight croqueteer" },
			new String[] { "4511", null } ),

		// Choice 449 is The Croquet Grounds (Disco Bandit)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure449", "Reflection of Map (Disco Bandit)",
			new String[] { "Lobster <i>qua</i> Grill", "fight croqueteer" },
			new String[] { "4515", null } ),

		// Choice 450 is The Duchess' Cottage

		// Typographical Clutter
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure451", "Greater-Than Sign",
			new String[] { "left parenthesis", "moxie, alternately lose then gain meat", "plus sign, then muscle", "mysticality substats", "get teleportitis" },
			new String[] { "4552", null, "818", null, null } ),

		// Leave a Message and I'll Call You Back
		new ChoiceAdventure(
			"Jacking", "choiceAdventure452", "Small-O-Fier",
			new String[] { "combat", "tiny fly glasses", "fruit" },
			new String[] { null, "4566", null } ),

		// Getting a Leg Up
		new ChoiceAdventure(
			"Jacking", "choiceAdventure453", "Small-O-Fier",
			new String[] { "combat", "stats", "hair of the calf" },
			new String[] { null, null, "4562" } ),

		// Just Like the Ocean Under the Moon
		new ChoiceAdventure(
			"Jacking", "choiceAdventure454", "Small-O-Fier",
			new String[] { "combat", "HP and MP" } ),

		// Double Trouble in the Stubble
		new ChoiceAdventure(
			"Jacking", "choiceAdventure455", "Small-O-Fier",
			new String[] { "stats", "quest item" } ),

		// Made it, Ma!  Top of the World!
		new ChoiceAdventure(
			"Jacking", "choiceAdventure456", "Huge-A-Ma-tron",
			new String[] { "combat", "Hurricane Force", "a dance upon the palate", "stats" },
			new String[] { null, null, "4568", null } ),

		// Choice 457 is Oh, No! Five-Oh!
		// Choice 458 is ... Grow Unspeakable Horrors
		// Choice 459 is unknown
		// Choice 460 is Space Trip (Bridge)
		// Choice 461 is Space Trip (Navigation)
		// Choice 462 is Space Trip (Diagnostics)
		// Choice 463 is Space Trip (Alpha Quadrant)
		// Choice 464 is Space Trip (Beta Quadrant)
		// Choice 465 is Space Trip (Planet)
		// Choice 466 is unknown
		// Choice 467 is Space Trip (Combat)
		// Choice 468 is Space Trip (Starbase Hub)
		// Choice 469 is Space Trip (General Store)
		// Choice 470 is Space Trip (Military Surplus Store)
		// Choice 471 is DemonStar
		// Choice 472 is Space Trip (Astrozorian Trade Vessel: Alpha)
		// Choice 473 is Space Trip (Murderbot Miner: first encounter)
		// Choice 474 is Space Trip (Slavers: Alpha)
		// Choice 475 is Space Trip (Astrozorian Trade Vessel: Beta)
		// Choice 476 is Space Trip (Astrozorian Trade Vessel: Gamma)
		// Choice 477 is Space Trip (Gamma Quadrant)
		// Choice 478 is Space Trip (The Source)
		// Choice 479 is Space Trip (Slavers: Beta)
		// Choice 480 is Space Trip (Scadian ship)
		// Choice 481 is Space Trip (Hipsterian ship)
		// Choice 482 is Space Trip (Slavers: Gamma)
		// Choice 483 is Space Trip (Scadian Homeworld)
		// Choice 484 is Space Trip (End)
		// Choice 485 is Fighters of Fighting
		// Choice 486 is Dungeon Fist!
		// Choice 487 is unknown
		// Choice 488 is Meteoid (Bridge)
		// Choice 489 is Meteoid (SpaceMall)
		// Choice 490 is Meteoid (Underground Complex)
		// Choice 491 is Meteoid (End)
		// Choice 492 is unknown
		// Choice 493 is unknown
		// Choice 494 is unknown
		// Choice 495 is unknown

		// Choice 496 is Crate Expectations
		// -> can skip if have +20 hot damage

		// Choice 497 is SHAFT!
		// Choice 498 is unknown
		// Choice 499 is unknown
		// Choice 500 is unknown
		// Choice 501 is unknown

		// Choice 502 is Arboreal Respite

		// The Road Less Traveled
		new ChoiceSpoiler(
			"choiceAdventure503", "Spooky Forest",
			new String[] { "gain some meat", "gain stakes or trade vampire hearts", "gain spooky sapling or trade bar skins" },
			new String[] { null, "1518", "70" } ),

		// Tree's Last Stand
		new ChoiceSpoiler(
			"choiceAdventure504", "Spooky Forest",
			new String[] { "bar skin", "bar skins", "buy spooky sapling", "skip adventure" },
			new String[] { "70", "70", "75", null } ),
		// Tree's Last Stand
		new Object[]{ new Integer(504), new Integer(1),
		  ItemPool.get( ItemPool.BAR_SKIN, -1 ) },
		new Object[]{ new Integer(504), new Integer(2),
		  ItemPool.get( ItemPool.BAR_SKIN, 1 ) },
		new Object[]{ new Integer(504), new Integer(3),
		  new AdventureResult( AdventureResult.MEAT, -100 ) },

		// Consciousness of a Stream
		new ChoiceSpoiler(
			"choiceAdventure505", "Spooky Forest",
			new String[] { "gain mosquito larva then 3 spooky mushrooms", "gain 300 meat & tree-holed coin then nothing", "fight a spooky vampire" },
			new String[] { "724", null, null } ),

		// Through Thicket and Thinnet
		new ChoiceSpoiler(
			"choiceAdventure506", "Spooky Forest",
			new String[] { "gain a starter item", "gain Spooky-Gro fertilizer", "gain spooky temple map" },
			new String[] { null, "76", "74" } ),

		// O Lith, Mon
		new ChoiceSpoiler(
			"choiceAdventure507", "Spooky Forest",
			new String[] { "gain Spooky Temple map", "skip adventure", "skip adventure" },
			new String[] { null, null, null } ),
		// O Lith, Mon
		new Object[]{ new Integer(507), new Integer(1),
		  ItemPool.get( ItemPool.TREE_HOLED_COIN, -1 ) },

		// Choice 508 is Pants-Gazing
		// Choice 509 is Of Course!
		// Choice 510 is Those Who Came Before You

		// If it's Tiny, is it Still a Mansion?
		new ChoiceAdventure(
			"Woods", "choiceAdventure511", "Typical Tavern",
			new String[] { "Baron von Ratsworth", "skip adventure" } ),

		// Hot and Cold Running Rats
		new ChoiceAdventure(
			"Woods", "choiceAdventure512", "Typical Tavern",
			new String[] { "fight", "skip adventure" } ),

		// Choice 513 is Staring Down the Barrel
		// -> can skip if have +20 cold damage
		// Choice 514 is 1984 Had Nothing on This Cellar
		// -> can skip if have +20 stench damage
		// Choice 515 is A Rat's Home...
		// -> can skip if have +20 spooky damage

		// Choice 516 is unknown
		// Choice 517 is Mr. Alarm, I Presarm

		// Clear and Present Danger
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure518", "Elf Alley",
			new String[] { "enter combat with Uncle Hobo", "skip adventure" } ),

		// What a Tosser
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure519", "Elf Alley",
			new String[] { "gift-a-pult", "skip adventure" },
			new String[] { "4852", null } ),
		// What a Tosser - gift-a-pult
		new Object[]{ new Integer(519), new Integer(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -50 ) },

		// Choice 520 is A Show-ho-ho-down
		// Choice 521 is A Wicked Buzz

		// Welcome to the Footlocker
		new ChoiceAdventure(
			"Knob", "choiceAdventure522", "Cobb's Knob Barracks",
			new String[] { "outfit piece or donut", "skip adventure" } ),

		// Death Rattlin'
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure523", "Defiled Cranny",
			new String[] { "small meat boost", "stats & HP & MP", "can of Ghuol-B-Gone&trade;", "fight swarm of ghuol whelps", "skip adventure" },
			new String[] { null, null, "2565", null, null } ),

		// Choice 524 is The Adventures of Lars the Cyberian
		// Choice 525 is Fiddling with a Puzzle
		// Choice 526 is unknown

		// Choice 527 is The Haert of Darkness
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure527", "Haert of the Cyrpt",
			new String[] { "fight the Bonerdagon", "skip adventure" } ),

		// Choice 528 is It Was Then That a Hideous Monster Carried You

		// A Swarm of Yeti-Mounted Skeletons
		new ChoiceAdventure(
			"Events", "choiceAdventure529", "Skeleton Swarm",
			new String[] { "Weapon Damage", "Spell Damage", "Ranged Damage" } ),

		// It Was Then That...  Aaaaaaaah!
		new ChoiceAdventure(
			"Mountain", "choiceAdventure530", "Icy Peak",
			new String[] { "hideous egg", "skip the adventure" },
			new String[] { "5106", null } ),

		// The Bonewall Is In
		new ChoiceAdventure(
			"Events", "choiceAdventure531", "Bonewall",
			new String[] { "Item Drop", "HP Bonus" } ),

		// You'll Sink His Battleship
		new ChoiceAdventure(
			"Events", "choiceAdventure532", "Battleship",
			new String[] { "Class Skills", "Accordion Thief Songs" } ),

		// Train, Train, Choo-Choo Train
		new ChoiceAdventure(
			"Events", "choiceAdventure533", "Supply Train",
			new String[] { "Meat Drop", "Pressure Penalty Modifiers" } ),

		// That's No Bone Moon...
		new ChoiceAdventure(
			"Events", "choiceAdventure534", "Bone Star",
			new String[] { "Torpedos", "Initiative", "Monster Level" },
			new String[] { "630", null, null } ),

		// Choice 535 is Deep Inside Ronald, Baby
		// Choice 536 is Deep Inside Grimace, Bow Chick-a Bow Bow
		// Choice 537 is Play Porko!
		// Choice 538 is Big-Time Generator
		// Choice 539 is An E.M.U. for Y.O.U.
		// Choice 540 is Big-Time Generator - game board
		// Choice 541 is unknown
		// Choice 542 is Now's Your Pants!  I Mean... Your Chance!
		// Choice 543 is Up In Their Grill
		// Choice 544 is A Sandwich Appears!

		// Behind Closed Doors
		new ChoiceAdventure(
			"Clan Basement", "choiceAdventure548", "Sorority House Necbromancer",
			new String[] { "enter combat with The Necbromancer", "skip adventure" } ),

		// Dark in the Attic
		new ChoiceSpoiler(
			"Clan Basement", "choiceAdventure549", "Dark in the Attic",
			new String[] { "staff guides", "ghost trap", "raise area ML", "lower area ML", "mass kill werewolves with silver shotgun shell" },
			new String[] { "5307", "5308", null, null, "5310" } ),

		// The Unliving Room
		new ChoiceSpoiler(
			"Clan Basement", "choiceAdventure550", "The Unliving Room",
			new String[] { "raise area ML", "lower area ML" , "mass kill zombies with chainsaw chain", "mass kill skeletons with funhouse mirror", "get costume item" },
			new String[] { null, null, "5309", "5311", null } ),

		// Debasement
		new ChoiceSpoiler(
			"Clan Basement", "choiceAdventure551", "Debasement",
			new String[] { "Prop Deportment", "mass kill vampires with plastic vampire fangs", "raise area ML", "lower area ML" } ),

		// Prop Deportment
		new ChoiceSpoiler(
			"Clan Basement", "choiceAdventure552", "Prop Deportment",
			new String[] { "chainsaw chain", "create a silver shotgun shell", "funhouse mirror" },
			new String[] { "5309", "5310", "5311" } ),

		// Relocked and Reloaded
		new ChoiceSpoiler(
			"Clan Basement", "choiceAdventure553", "Relocked and Reloaded",
			new String[] { "", "", "", "", "", "exit adventure" },
			new String[] { "2642", "2987", "4237", "1972", "4234", null } ),

		// Behind the Spooky Curtain
		new ChoiceSpoiler(
			"Clan Basement", "choiceAdventure554", "Behind the Spooky Curtain",
			new String[] { "staff guides, ghost trap, kill werewolves", "kill zombies, kill skeletons, costume item", "chainsaw chain, silver item, funhouse mirror, kill vampires" } ),

		// Gingerbread Homestead
		new ChoiceAdventure(
			"The Candy Diorama", "choiceAdventure557", "Gingerbread Homestead",
			new String[] { "get candies", "licorice root", "skip adventure or make a lollipop stick item" },
			new String[] { null, "5415", "5380" } ),

		// Tool Time
		new ChoiceAdventure(
			"The Candy Diorama", "choiceAdventure558", "Tool Time",
			new String[] { "sucker bucket", "sucker kabuto", "sucker hakama", "sucker tachi", "sucker scaffold", "skip adventure" },
			new String[] { "5426", "5428", "5427", "5429", "5430", null } ),

		// Fudge Mountain Breakdown
		new ChoiceAdventure(
			"The Candy Diorama", "choiceAdventure559", "Fudge Mountain Breakdown",
			new String[] { "fudge lily", "fight a swarm of fudgewasps or skip adventure", "frigid fudgepuck or skip adventure", "superheated fudge or skip adventure" },
			new String[] { "5436", null, "5439", "5437" } ),

		// Foreshadowing Demon!
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure560", "The Clumsiness Grove",
			new String[] { "head towards boss", "skip adventure" } ),

		// You Must Choose Your Destruction!
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure561", "The Clumsiness Grove",
			new String[] { "The Thorax", "The Bat in the Spats" } ),

		// Choice 562 is You're the Fudge Wizard Now, Dog

		// A Test of your Mettle
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure563", "The Clumsiness Grove",
			new String[] { "Fight Boss", "skip adventure" } ),

		// A Maelstrom of Trouble
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure564", "The Maelstrom of Lovers",
			new String[] { "head towards boss", "skip adventure" } ),

		// To Get Groped or Get Mugged?
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure565", "The Maelstrom of Lovers",
			new String[] { "The Terrible Pinch", "Thug 1 and Thug 2" } ),

		// A Choice to be Made
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure566", "The Maelstrom of Lovers",
			new String[] { "Fight Boss", "skip adventure" } ),

		// You May Be on Thin Ice
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure567", "The Glacier of Jerks",
			new String[] { "Fight Boss", "skip adventure" } ),

		// Some Sounds Most Unnerving
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure568", "The Glacier of Jerks",
			new String[] { "Mammon the Elephant", "The Large-Bellied Snitch" } ),

		// One More Demon to Slay
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure569", "The Glacier of Jerks",
			new String[] { "head towards boss", "skip adventure" } ),

		// Choice 571 is Your Minstrel Vamps
		// Choice 574 is The Minstrel Cycle Begins
	};

	public static final ChoiceAdventure[] CHOICE_ADVS;

	// We choose to not make some choice adventures configurable, but we
	// want to provide spoilers in the browser for them.

	public static final ChoiceAdventure[] CHOICE_ADV_SPOILERS;

	// Some choice adventures have options that cost meat or items

	public static final Object[][] CHOICE_COST;

	static
	{
		ArrayList choices = new ArrayList();
		ArrayList spoils = new ArrayList();
		ArrayList costs = new ArrayList();
		for ( int i = 0; i < CHOICE_DATA.length; ++i )
		{
			Object it = CHOICE_DATA[ i ];
			(it instanceof ChoiceSpoiler ? spoils
			: it instanceof ChoiceAdventure ? choices
			: costs).add( it );
		}
		CHOICE_ADVS = (ChoiceAdventure[]) choices.toArray( new ChoiceAdventure[ choices.size() ] );
		CHOICE_ADV_SPOILERS = (ChoiceAdventure[]) spoils.toArray( new ChoiceAdventure[ spoils.size() ] );
		CHOICE_COST = (Object[][]) costs.toArray( new Object[ costs.size() ][] );

		Arrays.sort( ChoiceManager.CHOICE_ADVS );
	}

	public static void initializeAfterChoice()
	{
		ChoiceManager.initializeAfterChoice = true;
	}

	private static final AdventureResult getCost( final int choice, final int decision )
	{
		for ( int i = 0; i < ChoiceManager.CHOICE_COST.length; ++i )
		{
			if ( choice == ((Integer)ChoiceManager.CHOICE_COST[ i ][ 0 ]).intValue() &&
			     decision == ((Integer)ChoiceManager.CHOICE_COST[ i ][ 1 ]).intValue() )
			{
				return (AdventureResult) ChoiceManager.CHOICE_COST[ i ][ 2 ];
			}
		}

		return null;
	}

	private static final void payCost( final int choice, final int decision )
	{
		AdventureResult cost = ChoiceManager.getCost( choice, decision );

		// No cost for this choice/decision
		if ( cost == null )
		{
			return;
		}

		int costCount = cost.getCount();

		// No cost for this choice/decision
		if ( costCount == 0 )
		{
			return;
		}

		if ( cost.isItem() )
		{
			int inventoryCount = cost.getCount( KoLConstants.inventory );
			// Make sure we have enough in inventory
			if ( costCount + inventoryCount < 0 )
			{
				return;
			}

			if ( costCount > 0 )
			{
				int multiplier = inventoryCount / costCount;
				cost = cost.getInstance( multiplier * costCount * -1 );
			}
		}
		else if ( cost.isMeat() )
		{
			int purseCount = KoLCharacter.getAvailableMeat();
			// Make sure we have enough in inventory
			if ( costCount + purseCount < 0 )
			{
				return;
			}
		}
		else if ( cost.isMP() )
		{
			int current = KoLCharacter.getCurrentMP();
			// Make sure we have enough mana
			if ( costCount + current < 0 )
			{
				return;
			}
		}
		else
		{
			return;
		}

		ResultProcessor.processResult( cost );
	}

	public static final void decorateChoice( final int choice, final StringBuffer buffer )
	{
		if ( choice >= 48 && choice <= 70 )
		{
			// Add "Go To Goal" button for the Violet Fog
			VioletFogManager.addGoalButton( buffer );
			return;
		}

		if ( choice >= 91 && choice <= 104 )
		{
			// Add "Go To Goal" button for the Louvre.
			LouvreManager.addGoalButton( buffer );
			return;
		}

		switch ( choice )
		{
		case 134:
		case 135:
			PyramidRequest.decorateChoice( choice, buffer );
			break;
		case 360:
			WumpusManager.decorate( buffer );
			break;
		case 392:
			MemoriesDecorator.decorateElements( choice, buffer );
			break;
		case 443:
			// Chess Puzzle
			RabbitHoleManager.decorateChessPuzzle( buffer );
			break;
		case 485:
			// Fighters of Fighting
			ArcadeRequest.decorateFightersOfFighting( buffer );
			break;
		case 486:
			// Dungeon Fist
			ArcadeRequest.decorateDungeonFist( buffer );
			break;

		case 537:
			// Play Porko!
		case 540:
			// Big-Time Generator
			SpaaaceRequest.decoratePorko( buffer );
			break;
		}
	}

	public static final String[][] choiceSpoilers( final int choice )
	{
		String[][] spoilers;

		// See if spoilers are dynamically generated
		spoilers = ChoiceManager.dynamicChoiceSpoilers( choice );
		if ( spoilers != null )
		{
			return spoilers;
		}

		// Nope. See if it's in the Violet Fog
		spoilers = VioletFogManager.choiceSpoilers( choice );
		if ( spoilers != null )
		{
			return spoilers;
		}

		// Nope. See if it's in the Louvre
		spoilers = LouvreManager.choiceSpoilers( choice );
		if ( spoilers != null )
		{
			return spoilers;
		}

		String option = "choiceAdventure" + String.valueOf( choice );

		// See if this choice is controlled by user option
		for ( int i = 0; i < ChoiceManager.CHOICE_ADVS.length; ++i )
		{
			if ( ChoiceManager.CHOICE_ADVS[ i ].getSetting().equals( option ) )
			{
				return ChoiceManager.CHOICE_ADVS[ i ].getSpoilers();
			}
		}

		// Nope. See if we know this choice
		for ( int i = 0; i < ChoiceManager.CHOICE_ADV_SPOILERS.length; ++i )
		{
			if ( ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getSetting().equals( option ) )
			{
				return ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getSpoilers();
			}
		}

		// Unknown choice
		return null;
	}

	private static final String[][] dynamicChoiceSpoilers( final int choice )
	{
		String[][] result;
		switch ( choice )
		{
		case 5:
			// Heart of Very, Very Dark Darkness
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "Spooky Gravy Barrow" );

		case 7:
			// How Depressing
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "Spooky Gravy Barrow" );

		case 85:
			// One NightStand (simple wooden)
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "Haunted Bedroom" );

		case 184:
			// That Explains All The Eyepatches

			result = ChoiceManager.dynamicChoiceSpoilers( 4, choice, "Barrrney's Barrr" );

			// Fill in items corresponding to choices
			for ( int i = 0; i < 3; ++i )
			{
				if ( result[ 2 ][ i ].startsWith( "shot of rotgut" ) )
				{
					result[ 3 ][ i ] = "2948";
				}
			}

			return result;

		case 187:
			// Arrr You Man Enough?
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "Barrrney's Barrr" );

		case 188:
			// The Infiltrationist
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "Orcish Frat House Blueprints" );

		case 272:
			// Marketplace Entrance
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "Hobo Marketplace" );

		case 298:
			// In the Shade
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "An Octopus's Garden" );

		case 304:
			// A Vent Horizon
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "The Marinara Trench" );

		case 305:
			// There is Sauce at the Bottom of the Ocean
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "The Marinara Trench" );

		case 309:
			// Barback
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "The Dive Bar" );

		case 360:
			// Wumpus Hunt
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "The Jungles of Ancient Loathing" );

		case 442:
			// A Moment of Reflection
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "Rabbit Hole" );

		case 522:
			// Welcome to the Footlocker

			result = ChoiceManager.dynamicChoiceSpoilers( 4, choice, "Welcome to the Footlocker" );

			// Fill in items corresponding to choices
			if ( result[ 2 ][ 0 ].indexOf( "polearm" ) != -1 )
			{
				result[ 3 ][ 0 ] = "310";
			}
			else if ( result[ 2 ][ 0 ].indexOf( "pants" ) != -1 )
			{
				result[ 3 ][ 0 ] = "309";
			}
			else if ( result[ 2 ][ 0 ].indexOf( "helm" ) != -1 )
			{
				result[ 3 ][ 0 ] = "308";
			}
			else
			{
				result[ 3 ][ 0 ] = "4941";
			}

			return result;

		case 502:
			// Arboreal Respite
			return ChoiceManager.dynamicChoiceSpoilers( 3, choice, "Arboreal Respite" );
		}
		return null;
	}

	private static final String[][] dynamicChoiceSpoilers( final int count, final int choice, final String name )
	{
		String[][] result = new String[ count ][];

		// The choice option is the first element
		result[ 0 ] = new String[ 1 ];
		result[ 0 ][ 0 ] = "choiceAdventure" + String.valueOf( choice );

		// The name of the choice is second element
		result[ 1 ] = new String[ 1 ];
		result[ 1 ][ 0 ] = name;

		// An array of choice spoilers is the third element
		result[ 2 ] = ChoiceManager.dynamicChoiceOptions( choice );

		if ( count > 3 )
		{
			// A parallel array of items is the fourth element
			// Caller will fill it in
			int r2len = result[ 2 ].length;
			result[ 3 ] = new String[ r2len ];

			// The code above was previously expressed more simply as:
			// result[ 3 ] = new String[ result[ 2 ].length ];
			// Unfortunately, TextWrangler 3.2's Java parser chokes on this, presumably
			// due to the square brackets nested inside the array initializer (a
			// construct found nowhere else in the mafia source).  The result is that
			// TextWrangler's jump-to-function popup could not see any methods defined
			// below this point in the file. - JRH
		}

		return result;
	}

	private static final String[] dynamicChoiceOptions( final int choice )
	{
		String[] result;
		switch ( choice )
		{
		case 5:
			// Heart of Very, Very Dark Darkness
			result = new String[ 2 ];

			boolean rock = InventoryManager.getCount( ItemPool.INEXPLICABLY_GLOWING_ROCK ) >= 1;

			result[ 0 ] = "You " + ( rock ? "" : "DON'T ") + " have an inexplicably glowing rock";
			result[ 1 ] = "skip adventure";

			return result;

		case 7:
			// How Depressing
			result = new String[ 2 ];

			boolean glove = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.SPOOKY_GLOVE, 1 ) );

			result[ 0 ] = "spooky glove " + ( glove ? "" : "NOT ") + "equipped";
			result[ 1 ] = "skip adventure";

			return result;

		case 85:
			// One NightStand (simple wooden)
			result = new String[ 5 ];

			boolean ballroom = Preferences.getInteger( "lastBallroomUnlock" ) == KoLCharacter.getAscensions();

			result[ 0 ] = "moxie" + ( ballroom ? "" : " and ballroom key step 1");
			result[ 1 ] = (ballroom && !KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) ? "ballroom key step 2" : "nothing");
			result[ 2 ] = "enter combat";
			result[ 3 ] = ballroom ? (!KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) ? "ballroom key step 2" : "moxie") : "moxie and ballroom key step 1";
			result[ 4 ] = ballroom ? (!KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) ? "ballroom key step 2" : "enter combat") : "moxie and ballroom key step 1";

			return result;

		case 184:
			// That Explains All The Eyepatches
			result = new String[ 6 ];

			// The choices are based on character class.

			int stat = KoLCharacter.getPrimeIndex() + 1;

			// Mus: combat, shot of rotgut (2948), drunkenness
			// Mys: drunkenness, shot of rotgut (2948), shot of rotgut (2948)
			// Mox: combat, drunkenness, shot of rotgut (2948)

			result[ 0 ] = stat == KoLConstants.MYSTICALITY ? "3 drunk and stats (varies by class)" : "enter combat (varies by class)";
			result[ 1 ] = stat == KoLConstants.MOXIE ? "3 drunk and stats (varies by class)" : "shot of rotgut (varies by class)";
			result[ 2 ] = stat == KoLConstants.MUSCLE ? "3 drunk and stats (varies by class)" : "shot of rotgut (varies by class)";
			result[ 3 ] = "always 3 drunk & stats";
			result[ 4 ] = "always shot of rotgut";
			result[ 5 ] = "combat (or rotgut if Myst class)";
			return result;

		case 187:
			// Arrr You Man Enough?

			result = new String[ 2 ];
			float odds = BeerPongRequest.pirateInsultOdds() * 100.0f;

			result[ 0 ] = KoLConstants.FLOAT_FORMAT.format( odds ) + "% chance of winning";
			result[ 1 ] = odds == 100.0f ? "Oh come on. Do it!" : "Try later";
			return result;

		case 188:
			// The Infiltrationist
			result = new String[ 3 ];

			// Attempt a frontal assault
			boolean ok1 = EquipmentManager.isWearingOutfit( 3 );
			result[ 0 ] = "Frat Boy Ensemble (" + ( ok1 ? "" : "NOT " ) + "equipped)";

			// Go in through the side door
			boolean ok2a = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.MULLET_WIG, 1 ) );
			boolean ok2b = InventoryManager.getCount( ItemPool.BRIEFCASE ) >= 1;
			result[ 1 ] = "mullet wig (" + ( ok2a ? "" : "NOT " ) + "equipped) + briefcase (" + ( ok2b ? "OK)" : "0 in inventory)" );

			// Catburgle
			boolean ok3a = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.FRILLY_SKIRT, 1 ) );
			int wings = InventoryManager.getCount( ItemPool.HOT_WING );
			result[ 2 ] = "frilly skirt (" + ( ok3a ? "" : "NOT " ) + " equipped) + 3 hot wings (" + wings + " in inventory)";

			return result;

		case 191:
			// Chatterboxing
			result = new String[ 4 ];

			int trinks = InventoryManager.getCount( ItemPool.VALUABLE_TRINKET );
			result[ 0 ] = "moxie substats";
			result[ 1 ] = trinks == 0 ? "lose hp (no valuable trinkets)" :
				"use valuable trinket to banish (" + trinks + " in inventory)";
			result[ 2 ] = "muscle substats";
			result[ 3 ] = "mysticality substats";

			return result;

		case 272:
			// Marketplace Entrance
			result = new String[ 2 ];

			int nickels = InventoryManager.getCount( ItemPool.HOBO_NICKEL );
			boolean binder = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.HOBO_CODE_BINDER, 1 ) );

			result[ 0 ] = nickels + " nickels, " + ( binder ? "" : "NO ") + " hobo code binder equipped";
			result[ 1 ] = "skip adventure";

			return result;

		case 298:
			// In the Shade
			result = new String[ 2 ];

			int seeds = InventoryManager.getCount( ItemPool.SEED_PACKET );
			int slime = InventoryManager.getCount( ItemPool.GREEN_SLIME );

			result[ 0 ] = seeds + " seed packets, " + slime + " globs of green slime";
			result[ 1 ] = "skip adventure";

			return result;

		case 304:
			// A Vent Horizon
			result = new String[ 2 ];

			int summons = 3 - Preferences.getInteger( "tempuraSummons" );

			result[ 0 ] = summons + " summons left today";
			result[ 1 ] = "skip adventure";

			return result;

		case 305:
			// There is Sauce at the Bottom of the Ocean
			result = new String[ 2 ];

			int globes = InventoryManager.getCount( ItemPool.MERKIN_PRESSUREGLOBE );

			result[ 0 ] = globes + " Mer-kin pressureglobes";
			result[ 1 ] = "skip adventure";

			return result;

		case 309:
			// Barback
			result = new String[ 2 ];

			int seaodes = 3 - Preferences.getInteger( "seaodesFound" );

			result[ 0 ] = seaodes + " more seodes available today";
			result[ 1 ] = "skip adventure";

			return result;

		case 360:
			// Wumpus Hunt
			return WumpusManager.dynamicChoiceOptions( ChoiceManager.lastResponseText );

		case 442:
			// A Moment of Reflection
			result = new String[ 6 ];
			int count = 0;
			if ( InventoryManager.getCount( ItemPool.BEAUTIFUL_SOUP ) > 0 )
			{
				++count;
			}
			if ( InventoryManager.getCount( ItemPool.LOBSTER_QUA_GRILL ) > 0 )
			{
				++count;
			}
			if ( InventoryManager.getCount( ItemPool.MISSING_WINE ) > 0 )
			{
				++count;
			}
			if ( InventoryManager.getCount( ItemPool.WALRUS_ICE_CREAM ) > 0 )
			{
				++count;
			}
			if ( InventoryManager.getCount( ItemPool.HUMPTY_DUMPLINGS ) > 0 )
			{
				++count;
			}
			result [ 0 ] = "Seal Clubber/Pastamancer item, or yellow matter custard";
			result [ 1 ] = "Sauceror/Accordion Thief item, or delicious comfit?";
			result [ 2 ] = "Disco Bandit/Turtle Tamer item, or fight croqueteer";
			result [ 3 ] = "you have " + count + "/5 of the items needed for an ittah bittah hookah";
			result [ 4 ] = "get a chess cookie";
			result [ 5 ] = "skip adventure";
			return result;

		case 522:
			// Welcome to the Footlocker
			result = new String[ 2 ];

			boolean havePolearm = ( InventoryManager.getCount( ItemPool.KNOB_GOBLIN_POLEARM ) > 0 );
			boolean havePants = ( InventoryManager.getCount( ItemPool.KNOB_GOBLIN_PANTS ) > 0 );
			boolean haveHelm = ( InventoryManager.getCount( ItemPool.KNOB_GOBLIN_HELM ) > 0 );

			result [ 0 ] = ( !havePolearm ? "knob goblin elite polearm" :
			                 !havePants ? "knob goblin elite pants" :
			                 !haveHelm ? "knob goblin elite helm" :
			                 "knob jelly donut" );
			result [ 1 ] = "skip adventure";
			return result;

		case 502:
			// Arboreal Respite

			// meet the vampire hunter, trade bar skins or gain a spooky sapling
			result = new String[ 3 ];

			int stakes = InventoryManager.getCount( ItemPool.WOODEN_STAKES );
			int hearts = InventoryManager.getCount( ItemPool.VAMPIRE_HEART );
			String hunterAction = ( stakes > 0 ? "and get wooden stakes" : "and trade " + hearts + " hearts" );

			int barskins = InventoryManager.getCount( ItemPool.BAR_SKIN );
			int saplings = InventoryManager.getCount( ItemPool.SPOOKY_SAPLING );

			result [ 0 ] = "gain some meat, meet the vampire hunter " + hunterAction + ", sell bar skins (" + barskins  + ") or buy a spooky sapling (" + saplings + ")";

			// gain mosquito larva, gain quest coin or gain a vampire heart
			boolean haveMap = InventoryManager.getCount( ItemPool.SPOOKY_MAP ) > 0;
			boolean haveCoin = InventoryManager.getCount( ItemPool.TREE_HOLED_COIN ) > 0;
			boolean getCoin = ( !haveCoin && !haveMap && !KoLCharacter.getTempleUnlocked() );
			String coinAction = ( getCoin ? "gain quest coin" : "skip adventure" );

			result [ 1 ] = "gain mosquito larva or spooky mushrooms, " + coinAction + ", get stats or fight a vampire";

			// gain a starter item, gain Spooky-Gro fertilizer or gain spooky temple map
			int fertalizer = InventoryManager.getCount( ItemPool.SPOOKY_FERTILIZER );
			String mapAction = ( haveCoin ? ", gain spooky temple map" : "" );

			result [ 2 ] = "gain a starter item, gain Spooky-Gro fertilizer (" + fertalizer + ")" + mapAction;

			return result;
		}
		return null;
	}

	public static final String[] dynamicChoiceOptions( final String option )
	{
		if ( !option.startsWith( "choiceAdventure" ) )
		{
			return null;
		}
		int choice = StringUtilities.parseInt( option.substring( 15 ) );
		return ChoiceManager.dynamicChoiceOptions( choice );
	}

	public static final String choiceSpoiler( final int choice, final int decision, final String[] spoilers )
	{
		switch ( choice )
		{
		case 105:
			// Having a Medicine Ball
			if ( decision == 2 )
			{
				KoLCharacter.ensureUpdatedGuyMadeOfBees();
				boolean defeated = Preferences.getBoolean( "guyMadeOfBeesDefeated" );
				if ( defeated )
				{
					return "guy made of bees: defeated";
				}
				return "guy made of bees: called " + Preferences.getString( "guyMadeOfBeesCount" ) + " times";
			}
			break;
		}
		return spoilers[ decision ];
	}

	public static final boolean processChoiceAdventure()
	{
		ChoiceManager.processChoiceAdventure( ChoiceManager.CHOICE_HANDLER, null );
		return ChoiceManager.CHOICE_HANDLER.containsUpdate;
	}

	public static final void processChoiceAdventure( int decision )
	{
		GenericRequest request = ChoiceManager.CHOICE_HANDLER;
		request.constructURLString( "choice.php" );
		request.addFormField( "whichchoice", String.valueOf( ChoiceManager.lastChoice ) );
		request.addFormField( "option", String.valueOf( decision ) );
		request.addFormField( "pwd", GenericRequest.passwordHash );

		request.run();
		ChoiceManager.processChoiceAdventure( request, request.responseText );
	}

	public static final void processChoiceAdventure( final GenericRequest request, final String responseText )
	{
		// You can no longer simply ignore a choice adventure.	One of
		// the options may have that effect, but we must at least run
		// choice.php to find out which choice it is.

		if ( responseText == null )
		{
			GoalManager.updateProgress( GoalManager.GOAL_CHOICE );
			request.constructURLString( "choice.php" );
			request.run();

			if ( request.responseCode == 302 )
			{
				return;
			}
		}
		else
		{
			request.responseText = responseText;
		}

		if ( GenericRequest.passwordHash.equals( "" ) )
		{
			return;
		}

		for ( int stepCount = 0; request.responseText.indexOf( "choice.php" ) != -1; ++stepCount )
		{
			Matcher choiceMatcher = ChoiceManager.CHOICE_PATTERN.matcher( request.responseText );

			if ( !choiceMatcher.find() )
			{
				// choice.php did not offer us any choices.
				// This would be a bug in KoL itself.
				// Bail now and let the user finish by hand.

				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Encountered choice adventure with no choices." );
				request.showInBrowser( true );
				return;
			}

			String whichchoice = choiceMatcher.group( 1 );
			int choice = StringUtilities.parseInt( whichchoice );

			// If this choice has special handling that can't be
			// handled by a single preference (extra fields, for
			// example), handle it elsewhere.

			if ( ChoiceManager.specialChoiceHandling( choice, request ) )
			{
				return;
			}

			String option = "choiceAdventure" + choice;
			String decision = Preferences.getString( option );

			// If this choice has special handling, convert to real
			// decision index

			decision = ChoiceManager.specialChoiceDecision( choice, option, decision, stepCount, request.responseText );

			// Let user handle the choice manually, if requested

			if ( decision.equals( "0" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Manual control requested for choice #" + choice );
				request.showInBrowser( true );
				ChoiceCommand.printChoices();
				return;
			}

			// Bail if no setting determines the decision

			if ( decision.equals( "" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Unsupported choice adventure #" + whichchoice );
				StaticEntity.printRequestData( request );
				request.showInBrowser( true );
				ChoiceCommand.printChoices();
				return;
			}

			// If user wants to complete an outfit, convert to real
			// decision index

			decision = ChoiceManager.pickOutfitChoice( option, decision );

			request.clearDataFields();
			request.addFormField( "whichchoice", whichchoice );
			request.addFormField( "option", decision );
			request.addFormField( "pwd", GenericRequest.passwordHash );

			request.run();
		}
	}

	public static final int getDecision( int choice, String responseText )
	{
		String option = "choiceAdventure" + choice;
		String decision = Preferences.getString( option );

		// If this choice has special handling, convert to real
		// decision index

		decision = ChoiceManager.specialChoiceDecision( choice, option, decision, Integer.MAX_VALUE, responseText );

		if ( decision.equals( "0" ) || decision.equals( "" ) )
		{	// Manual choice requested, or unsupported choice
			return 0;
		}

		// If user wants to complete an outfit, convert to real
		// decision index

		decision = ChoiceManager.pickOutfitChoice( option, decision );
		return StringUtilities.parseInt( decision );
	}

	public static final int getLastChoice()
	{
		return ChoiceManager.lastChoice;
	}

	public static final int getLastDecision()
	{
		return ChoiceManager.lastDecision;
	}

	public static final void preChoice( final GenericRequest request )
	{
		String choice = request.getFormField( "whichchoice" );
		String option = request.getFormField( "option" );

		if ( choice == null || option == null )
		{
			// Visiting a choice page but not yet making a decision
			ChoiceManager.lastChoice = 0;
			ChoiceManager.lastDecision = 0;
			ChoiceManager.lastResponseText = null;
			return;
		}

		// We are about to take a choice option
		ChoiceManager.lastChoice = StringUtilities.parseInt( choice );
		ChoiceManager.lastDecision = StringUtilities.parseInt( option );

		switch ( ChoiceManager.lastChoice )
		{
		// Wheel In the Sky Keep on Turning: Muscle Position
		case 9:
			Preferences.setString(
				"currentWheelPosition",
				ChoiceManager.lastDecision == 1 ? "mysticality" : ChoiceManager.lastDecision == 2 ? "moxie" : "muscle" );
			break;

		// Wheel In the Sky Keep on Turning: Mysticality Position
		case 10:
			Preferences.setString(
				"currentWheelPosition",
				ChoiceManager.lastDecision == 1 ? "map quest" : ChoiceManager.lastDecision == 2 ? "muscle" : "mysticality" );
			break;

		// Wheel In the Sky Keep on Turning: Map Quest Position
		case 11:
			Preferences.setString(
				"currentWheelPosition",
				ChoiceManager.lastDecision == 1 ? "moxie" : ChoiceManager.lastDecision == 2 ? "mysticality" : "map quest" );
			break;

		// Wheel In the Sky Keep on Turning: Moxie Position
		case 12:
			Preferences.setString(
				"currentWheelPosition",
				ChoiceManager.lastDecision == 1 ? "muscle" : ChoiceManager.lastDecision == 2 ? "map quest" : "moxie" );
			break;

		// Maidens: disambiguate the Knights
		case 89:
			AdventureRequest.setNameOverride( "Knight",
				ChoiceManager.lastDecision == 1 ? "Knight (Wolf)" : "Knight (Snake)" );
			break;

		// Strung-Up Quartet
		case 106:

			Preferences.setInteger( "lastQuartetAscension", KoLCharacter.getAscensions() );
			Preferences.setInteger( "lastQuartetRequest", ChoiceManager.lastDecision );

			if ( KoLCharacter.recalculateAdjustments() )
			{
				KoLCharacter.updateStatus();
			}

			break;

		// Wheel In the Pyramid, Keep on Turning
		case 134:
		case 135:
			if ( ChoiceManager.lastDecision == 1 )
			{
				PyramidRequest.advancePyramidPosition();
			}
			break;

		// Start the Island War Quest
		case 142:
		case 146:
			if ( ChoiceManager.lastDecision == 3 )
			{
				QuestDatabase.setQuestProgress( QuestDatabase.ISLAND_WAR, "step1" );
			}
			break;

		// The Gong Has Been Bung
		case 276:
			ResultProcessor.processItem( ItemPool.GONG, -1 );
			Preferences.setInteger( "moleTunnelLevel", 0 );
			Preferences.setInteger( "birdformCold", 0 );
			Preferences.setInteger( "birdformHot", 0 );
			Preferences.setInteger( "birdformRoc", 0 );
			Preferences.setInteger( "birdformSleaze", 0 );
			Preferences.setInteger( "birdformSpooky", 0 );
			Preferences.setInteger( "birdformStench", 0 );
			break;

		// An E.M.U. for Y.O.U.
		case 539:
			EquipmentManager.discardEquipment( ItemPool.SPOOKY_LITTLE_GIRL );
			break;
		}
	}

	private static final Pattern SKELETON_PATTERN = Pattern.compile( "You defeated <b>(\\d+)</b> skeletons" );
	public static void postChoice1( final GenericRequest request )
	{
		// Things that can or need to be done BEFORE processing results.
		// Remove spent items or meat here.

		if ( ChoiceManager.lastChoice == 0 )
		{
			// We are viewing the choice page for the first time.
			ChoiceManager.visitChoice( request );
			return;
		}

		String text = request.responseText;

		switch ( ChoiceManager.lastChoice )
		{
		case 188:
			// The Infiltrationist

			// Once you're inside the frat house, it's a simple
			// matter of making your way down to the basement and
			// retrieving Caronch's dentures from the frat boys'
			// ridiculous trophy case.

			if ( ChoiceManager.lastDecision == 3 &&
			     text.indexOf( "ridiculous trophy case" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.HOT_WING, -3 ) );
			}
			break;

		case 237:
			// Big Merv's Protein Shakes
		case 238:
			// Suddenly Salad!
		case 239:
			// Sizzling Weasel On a Stick
			if ( ChoiceManager.lastDecision == 1 && text.indexOf( "You gain" ) != -1 )
			{
				// You spend 20 hobo nickels
				AdventureResult cost = new AdventureResult( "hobo nickel", -20 );
				ResultProcessor.processResult( cost );

				// You gain 5 fullness
				Preferences.increment( "currentFullness", 5 );
			}
			break;

		case 242:
			// Arthur Finn's World-Record Homebrew Stout
		case 243:
			// Mad Jack's Corn Squeezery
		case 244:
			// Bathtub Jimmy's Gin Mill
			if ( ChoiceManager.lastDecision == 1 && text.indexOf( "You gain" ) != -1 )
			{
				// You spend 20 hobo nickels
				AdventureResult cost = new AdventureResult( "hobo nickel", -20 );
				ResultProcessor.processResult( cost );

				// You gain 5 drunkenness.  This will be set
				// when we refresh the charpane.
			}
			break;

		case 271:
			// Tattoo Shop
		case 274:
			// Tattoo Redux
			if ( ChoiceManager.lastDecision == 1 )
			{
				Matcher matcher = ChoiceManager.TATTOO_PATTERN.matcher( request.responseText );
				if ( matcher.find() )
				{
					int tattoo = StringUtilities.parseInt( matcher.group(1) );
					AdventureResult cost = new AdventureResult( "hobo nickel", -20 * tattoo );
					ResultProcessor.processResult( cost );
				}
			}
			break;

		case 298:
			// In the Shade

			// You carefully plant the packet of seeds, sprinkle it
			// with gooey green algae, wait a few days, and then
			// you reap what you sow. Sowed. Sew?

			if ( ChoiceManager.lastDecision == 1 && text.indexOf( "you reap what you sow" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.SEED_PACKET, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.GREEN_SLIME, -1 ) );
			}
			break;

		case 354:
			// You Can Never Be Too Rich or Too in the Future
			ResultProcessor.processResult( ItemPool.get( ItemPool.INDIGO_PARTY_INVITATION, -1 ) );
			break;

		case 355:
			// I'm on the Hunt, I'm After You
			ResultProcessor.processResult( ItemPool.get( ItemPool.VIOLET_HUNT_INVITATION, -1 ) );
			break;

		case 357:
			// Painful, Circuitous Logic
			ResultProcessor.processResult( ItemPool.get( ItemPool.MECHA_MAYHEM_CLUB_CARD, -1 ) );
			break;

		case 358:
			// Brings All the Boys to the Blue Yard
			ResultProcessor.processResult( ItemPool.get( ItemPool.BLUE_MILK_CLUB_CARD, -1 ) );
			break;

		case 362:
			// A Bridge Too Far
			ResultProcessor.processResult( ItemPool.get( ItemPool.SPACEFLEET_COMMUNICATOR_BADGE, -1 ) );
			break;

		case 363:
			// Does This Bug You? Does This Bug You?
			ResultProcessor.processResult( ItemPool.get( ItemPool.SMUGGLER_SHOT_FIRST_BADGE, -1 ) );
			break;

		case 373:
			// Choice 373 is Northern Gate

			// Krakrox plugged the small stone block he found in
			// the basement of the abandoned building into the hole
			// on the left side of the gate

			if ( text.indexOf( "Krakrox plugged the small stone block" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.SMALL_STONE_BLOCK, -1 ) );
			}

			// Krakrox plugged the little stone block he had found
			// in the belly of the giant snake into the hole on the
			// right side of the gate

			else if ( text.indexOf( "Krakrox plugged the little stone block" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.LITTLE_STONE_BLOCK, -1 ) );
			}
			break;

		case 376:
			// Choice 376 is Ancient Temple

			// You fit the two halves of the stone circle together,
			// and slot them into the depression on the door. After
			// a moment's pause, a low rumbling becomes audible,
			// and then the stone slab lowers into the ground. The
			// temple is now open to you, if you are ready to face
			// whatever lies inside.

			if ( text.indexOf( "two halves of the stone circle" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.HALF_STONE_CIRCLE, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.STONE_HALF_CIRCLE, -1 ) );
			}
			break;

		case 389:
			// Choice 389 is The Unbearable Supremeness of Being

			// "Of course I understand," Jill says, in fluent
			// English. "I learned your language in the past five
			// minutes. I know where the element is, but we'll have
			// to go offworld to get it. Meet me at the Desert
			// Beach Spaceport." And with that, she gives you a
			// kiss and scampers off. Homina-homina.

			if ( text.indexOf( "Homina-homina" ) != -1 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.SUPREME_BEING_GLOSSARY, -1 ) );
			}
			break;

		case 392:
			// Choice 392 is The Elements of Surprise . . .

			// And as the two of you walk toward the bed, you sense
			// your ancestral memories pulling you elsewhere, ever
			// elsewhere, because your ancestral memories are
			// total, absolute jerks.

			if ( text.indexOf( "total, absolute jerks" ) != -1 )
			{
				EquipmentManager.discardEquipment( ItemPool.RUBY_ROD );
				ResultProcessor.processResult( ItemPool.get( ItemPool.ESSENCE_OF_HEAT, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.ESSENCE_OF_KINK, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.ESSENCE_OF_COLD, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.ESSENCE_OF_STENCH, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.ESSENCE_OF_FRIGHT, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.ESSENCE_OF_CUTE, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.SECRET_FROM_THE_FUTURE, 1 ) );

			}
			break;

		case 393:
			// The Collector
			if ( ChoiceManager.lastDecision == 1 )
			{
				for ( int i = ItemPool.GREEN_PEAWEE_MARBLE; i <= ItemPool.BIG_BUMBOOZER_MARBLE; ++i )
				{
					ResultProcessor.processResult( ItemPool.get( i, -1 ) );
				}
			}
			break;

		case 394: {
			// Hellevator Music
			// Parse response
			Matcher matcher = HELLEVATOR_PATTERN.matcher( text );
			if ( !matcher.find() )
			{
				break;
			}
			String floor = matcher.group( 1 );
			for ( int mcd = 0; mcd < FLOORS.length; ++mcd )
			{
				if ( floor.equals( FLOORS[ mcd ] ) )
				{
					String message = "Setting monster level to " + mcd;
					RequestLogger.printLine( message );
					RequestLogger.updateSessionLog( message );
					break;
				}
			}
			break;
		}

		case 443:
			// Chess Puzzle
			if ( ChoiceManager.lastDecision == 1 )
			{
				// Option 1 is "Play"
				String location = request.getURLString();
				RabbitHoleManager.parseChessMove( location, text );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				// Option 2 is "Walk away from the board"
				ResultProcessor.processItem( ItemPool.REFLECTION_OF_MAP, 1 );
			}
			break;

		case 450:
			// The Duchess' Cottage
			if ( ChoiceManager.lastDecision == 1 &&
				text.indexOf( "Delectable and pulchritudinous!" ) != -1 )
			{	// Option 1 is Feed the Duchess
				ResultProcessor.processItem( ItemPool.BEAUTIFUL_SOUP, -1 );
				ResultProcessor.processItem( ItemPool.LOBSTER_QUA_GRILL, -1 );
				ResultProcessor.processItem( ItemPool.MISSING_WINE, -1 );
				ResultProcessor.processItem( ItemPool.WALRUS_ICE_CREAM, -1 );
				ResultProcessor.processItem( ItemPool.HUMPTY_DUMPLINGS, -1 );
			}
			break;

		case 457:
			// Oh, No! Five-Oh!
			int count = InventoryManager.getCount( ItemPool.ORQUETTES_PHONE_NUMBER );
			if ( ChoiceManager.lastDecision == 1 && count > 0 )
			{
				ResultProcessor.processItem( ItemPool.ORQUETTES_PHONE_NUMBER, -count );
				ResultProcessor.processItem( ItemPool.KEGGER_MAP, -1 );
			}

		case 460: case 461: case 462: case 463: case 464:
		case 465:           case 467: case 468: case 469:
		case 470:           case 472: case 473: case 474:
		case 475: case 476: case 477: case 478: case 479:
		case 480: case 481: case 482: case 483: case 484:
			// Space Trip
			ArcadeRequest.postChoiceSpaceTrip( request );
			break;

		case 471:
			// DemonStar
			ArcadeRequest.postChoiceDemonStar( request );
			break;

		case 485:
			// Fighters Of Fighting
			ArcadeRequest.postChoiceFightersOfFighting( request );
			break;

		case 486:
			// Dungeon Fist!
			ArcadeRequest.postChoiceDungeonFist( request );
			break;

		case 488: case 489: case 490: case 491:
			// Meteoid
			ArcadeRequest.postChoiceMeteoid( request );
			break;

		case 529:
		case 531:
		case 532:
		case 533:
		case 534:
			Matcher skeletonMatcher = SKELETON_PATTERN.matcher( text );
			if ( skeletonMatcher.find() )
			{
				String message = "You defeated " + skeletonMatcher.group(1) + " skeletons";
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
			}
			break;

		case 539:
			// Choice 539 is An E.M.U. for Y.O.U.
			EquipmentManager.discardEquipment( ItemPool.SPOOKY_LITTLE_GIRL );
			break;

		case 540:
			// Choice 540 is Big-Time Generator - game board
			//
			// Win:
			//
			// The generator starts to hum and the well above you
			// begins to spin, slowly at first, then faster and
			// faster. The humming becomes a deep, sternum-rattling
			// thrum, a sub-audio *WHOOMP WHOOMP WHOOMPWHOOMPWHOOMP.*
			// Brilliant blue light begins to fill the well, and
			// you feel like your bones are turning to either
			// powder, jelly, or jelly with powder in it.<p>Then
			// you fall through one of those glowy-circle
			// transporter things and end up back on Grimace, and
			// boy, are they glad to see you! You're not sure where
			// one gets ticker-tape after an alien invasion, but
			// they seem to have found some.
			//
			// Lose 3 times:
			//
			// Your E.M.U.'s getting pretty beaten up from all the
			// pinballing between obstacles, and you don't like
			// your chances of getting back to the surface if you
			// try again. You manage to blast out of the generator
			// well and land safely on the surface. After that,
			// though, the E.M.U. gives an all-over shudder, a sad
			// little servo whine, and falls apart.

			if ( text.indexOf( "WHOOMP" ) != -1 ||
			     text.indexOf( "a sad little servo whine" ) != -1 )
			{
				EquipmentManager.discardEquipment( ItemPool.EMU_UNIT );
			}
			break;

		case 542:
			// The Now's Your Pants!  I Mean... Your Chance!

			// Then you make your way back out of the Alley,
			// clutching your pants triumphantly and trying really
			// hard not to think about how oddly chilly it has
			// suddenly become.

			// When you steal your pants, they are unequipped, you
			// gain a "you acquire" message", and they appear in
			// inventory.
			//
			// Treat this is simply discarding the pants you are
			// wearing
			if ( text.indexOf( "oddly chilly" ) != -1 )
			{
				EquipmentManager.discardEquipment( EquipmentManager.getEquipment( EquipmentManager.PANTS ) );
			}
			break;

		case 559:
			// Fudge Mountain Breakdown
			if ( ChoiceManager.lastDecision == 2 )
			{
				if ( text.indexOf( "but nothing comes out" ) != -1 )
				{
					Preferences.setInteger( "_fudgeWaspFights", 3 );
				}
				else if ( text.indexOf( "trouble has taken a holiday" ) != -1 )
				{
					// The Advent Calendar hasn't been punched out enough to find fudgewasps yet
				}
				else
				{
					Preferences.increment( "_fudgeWaspFights", 1 );
				}
			}
			break;
		}

		// Certain choices cost meat or items when selected
		ChoiceManager.payCost( ChoiceManager.lastChoice, ChoiceManager.lastDecision );
	}

	public static void postChoice2( final GenericRequest request )
	{
		// Things that can or need to be done AFTER processing results.

		if ( ChoiceManager.lastChoice == 0 || ChoiceManager.lastDecision == 0 )
		{
			return;
		}

		String text = request.responseText;

		switch ( ChoiceManager.lastChoice )
		{
		case 7:
			// How Depressing

			if ( ChoiceManager.lastDecision == 1 )
			{
				EquipmentManager.discardEquipment( ItemPool.SPOOKY_GLOVE );
			}
			break;

		case 21:
			// Under the Knife
			if ( ChoiceManager.lastDecision == 1 &&
				text.indexOf( "anaesthetizes you" ) != -1 )
			{
				Preferences.increment( "sexChanges", 1 );
				Preferences.setBoolean( "_sexChanged", true );
				KoLCharacter.setGender( text.indexOf( "in more ways than one" ) != -1 ?
					KoLCharacter.FEMALE : KoLCharacter.MALE );
				ConcoctionDatabase.setRefreshNeeded( false );
			}
			break;

		case 48: case 49: case 50: case 51: case 52:
		case 53: case 54: case 55: case 56: case 57:
		case 58: case 59: case 60: case 61: case 62:
		case 63: case 64: case 65: case 66: case 67:
		case 68: case 69: case 70:
			// Choices in the Violet Fog
			VioletFogManager.mapChoice( ChoiceManager.lastChoice, ChoiceManager.lastDecision, text );
			break;

		case 81:
			// Take a Look, it's in a Book!
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setInteger( "lastGalleryUnlock", KoLCharacter.getAscensions() );
				break;
			}
			// fall through
		case 80:
			// Take a Look, it's in a Book!
			if ( ChoiceManager.lastDecision == 99 )
			{
				Preferences.setInteger( "lastSecondFloorUnlock", KoLCharacter.getAscensions() );
			}
			break;

		case 85:
			// One NightStand (simple wooden)
			if ( ChoiceManager.lastDecision == 1 && Preferences.getInteger( "lastBallroomUnlock" ) != KoLCharacter.getAscensions() )
			{
				Preferences.setInteger( "lastBallroomUnlock", KoLCharacter.getAscensions() );
			}
			break;

		case 92: case 93: case 94: case 95: case 96:
		case 97: case 98: case 99: case 100: case 101:
		case 102: case 103: case 104:
			// Choices in the Louvre
			LouvreManager.mapChoice( ChoiceManager.lastChoice, ChoiceManager.lastDecision, text );
			break;

		case 105:
			if ( ChoiceManager.lastDecision == 3 )
			{
				checkGuyMadeOfBees( request );
			}
			break;

		case 112:
			// Please, Hammer
			if ( ChoiceManager.lastDecision == 1 && KoLmafia.isAdventuring() )
			{
				InventoryManager.retrieveItem( ItemPool.get( ItemPool.HAROLDS_HAMMER, 1 ) );
			}
			break;
		case 123:
			// At least it's not full of trash
			if ( ChoiceManager.lastDecision == 1 && KoLmafia.isAdventuring() )
			{
				QuestDatabase.setQuestProgress( QuestDatabase.WORSHIP, "step1" );
			}
			break;
		case 125:
			// No visible means of support
			if ( ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring() )
			{
				QuestDatabase.setQuestProgress( QuestDatabase.WORSHIP, "step3" );
			}
			break;
			
		case 132:
			// Let's Make a Deal!
			if ( ChoiceManager.lastDecision == 1 && KoLmafia.isAdventuring() )
			{
				QuestDatabase.setQuestProgress( QuestDatabase.PYRAMID, "step1" );
			}
			break;

		case 162:
			// Between a Rock and Some Other Rocks
			if ( KoLmafia.isAdventuring() && !EquipmentManager.isWearingOutfit( 8 ) && !KoLConstants.activeEffects.contains( SorceressLairManager.EARTHEN_FIST ) )
			{
				CouncilFrame.unlockGoatlet();
			}
			break;

		case 197:
			// Somewhat Higher and Mostly Dry
		case 198:
			// Disgustin' Junction
		case 199:
			// The Former or the Ladder
			if ( ChoiceManager.lastDecision == 1 )
			{
				ChoiceManager.checkDungeonSewers( request );
			}
			break;

		case 200:
			// Enter The Hoboverlord
		case 201:
			// Home, Home in the Range
		case 202:
			// Bumpity Bump Bump
		case 203:
			// Deep Enough to Dive
		case 204:
			// Welcome To You!
		case 205:
			// Van, Damn

			// Stop for Hobopolis bosses
			if ( ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, ChoiceManager.hobopolisBossName( ChoiceManager.lastChoice ) + " waits for you." );
			}
			break;

		case 304:
			// A Vent Horizon

			// "You conjure some delicious batter from the core of
			// the thermal vent. It pops and sizzles as you stick
			// it in your sack."

			if ( text.indexOf( "pops and sizzles" ) != -1 )
			{
				Preferences.increment( "tempuraSummons", 1 );
			}
			break;

		case 309:
			// Barback

			// "You head down the tunnel into the cave, and manage
			// to find another seaode. Sweet! I mean... salty!"

			if ( text.indexOf( "salty!" ) != -1 )
			{
				Preferences.increment( "seaodesFound", 1 );
			}
			break;

		case 326:
			// Showdown

			if ( ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE,
					"Mother Slime waits for you." );
			}
			break;

		case 330:
			// A Shark's Chum
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.increment( "poolSharkCount", 1 );
			}
			break;

		case 360:
			WumpusManager.takeChoice( ChoiceManager.lastDecision, text );
			break;

		case 441:
			// The Mad Tea Party

			// I'm sorry, but there's a very strict dress code for
			// this party

			if ( ChoiceManager.lastDecision == 1  &&
			     text.indexOf( "very strict dress code" ) == -1 )
			{
				Preferences.setBoolean( "_madTeaParty", true );
			}
			break;

		case 442:
			// A Moment of Reflection
			if ( ChoiceManager.lastDecision == 5 )
			{
				// Option 5 is Chess Puzzle
				RabbitHoleManager.parseChessPuzzle( text );
			}

			if ( ChoiceManager.lastDecision != 6 )
			{
				// Option 6 does not consume the map. Others do.
				ResultProcessor.processItem( ItemPool.REFLECTION_OF_MAP, -1 );
			}
			break;
		case 517:
			// Mr. Alarm, I presarm
			QuestDatabase.setQuestIfBetter( QuestDatabase.PALINDOME, "step3" );
			break;

		case 518:
			// Clear and Present Danger

			// Stop for Hobopolis bosses
			if ( ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, ChoiceManager.hobopolisBossName( ChoiceManager.lastChoice ) + " waits for you." );
			}
			break;

		case 524:
			// The Adventures of Lars the Cyberian
			if ( text.indexOf( "Skullhead's Screw" ) != -1 )
			{
				// You lose the book if you receive the reward.
				// I don't know if that's always the result of
				// the same choice option
				ResultProcessor.processItem( ItemPool.LARS_THE_CYBERIAN, -1 );
			}
			break;

		case 548:
			// Behind Closed Doors
			if ( ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE,
					"The Necbromancer waits for you." );
			}
			break;

		case 549:
			// Dark in the Attic
			if ( text.indexOf( "The silver pellets tear through the sorority werewolves" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.SILVER_SHOTGUN_SHELL, -1 );
				RequestLogger.printLine( "You took care of a bunch of werewolves." );
			}
			else if ( text.indexOf( "quietly sneak away" ) != -1 )
			{
				RequestLogger.printLine( "You need a silver shotgun shell to kill werewolves." );
			}
			else if ( text.indexOf( "a loose shutter" ) != -1 )
			{
				RequestLogger.printLine( "All the werewolves have been defeated." );
			}
			else if ( text.indexOf( "crank up the volume on the boombox" ) != -1 )
			{
				RequestLogger.printLine( "You crank up the volume on the boombox." );
			}
			else if ( text.indexOf( "a firm counterclockwise twist" ) != -1 )
			{
				RequestLogger.printLine( "You crank down the volume on the boombox." );
			}
			break;

		case 550:
			// The Unliving Room
			if ( text.indexOf( "you pull out the chainsaw blades" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.CHAINSAW_CHAIN, -1 );
				RequestLogger.printLine( "You took out a bunch of zombies." );
			}
			else if ( text.indexOf( "a wet tearing noise" ) != -1 )
			{
				RequestLogger.printLine( "You need a chainsaw chain to kill zombies." );
			}
			else if ( text.indexOf( "a bloody tangle" ) != -1 )
			{
				RequestLogger.printLine( "All the zombies have been defeated." );
			}
			else if ( text.indexOf( "the skeletons collapse into piles of loose bones" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.FUNHOUSE_MIRROR, -1 );
				RequestLogger.printLine( "You made short work of some skeletons." );
			}
			else if ( text.indexOf( "couch in front of the door" ) != -1 )
			{
				RequestLogger.printLine( "You need a funhouse mirror to kill skeletons." );
			}
			else if ( text.indexOf( "just coats" ) != -1 )
			{
				RequestLogger.printLine( "All the skeletons have been defeated." );
			}
			else if ( text.indexOf( "close the windows" ) != -1 )
			{
				RequestLogger.printLine( "You close the windows." );
			}
			else if ( text.indexOf( "open the windows" ) != -1 )
			{
				RequestLogger.printLine( "You open the windows." );
			}
			break;

		case 551:
			// Debasement
			if ( text.indexOf( "the vampire girls shriek" ) != -1 )
			{
				RequestLogger.printLine( "You slew some vampires." );
			}
			else if ( text.indexOf( "gets back in her coffin" ) != -1 )
			{
				RequestLogger.printLine( "You need to equip plastic vampire fangs to kill vampires." );
			}
			else if ( text.indexOf( "they recognize you" ) != -1 )
			{
				RequestLogger.printLine( "You have already killed some vampires." );
			}
			else if ( text.indexOf( "crank up the fog machine" ) != -1 )
			{
				RequestLogger.printLine( "You crank up the fog machine." );
			}
			else if ( text.indexOf( "turn the fog machine way down" ) != -1 )
			{
				RequestLogger.printLine( "You crank down the fog machine." );
			}
			break;

		case 553:
			// Relocked and Reloaded
			if ( text.indexOf( "You melt" ) != -1 )
			{
				int item = 0;
				switch ( ChoiceManager.lastDecision )
				{
					case 1:
						item = ItemPool.MAXWELL_HAMMER;
						break;
					case 2:
						item = ItemPool.TONGUE_BRACELET;
						break;
					case 3:
						item = ItemPool.SILVER_CHEESE_SLICER;
						break;
					case 4:
						item = ItemPool.SILVER_SHRIMP_FORK;
						break;
					case 5:
						item = ItemPool.SILVER_PATE_KNIFE;
						break;
				}
				if ( item > 0 )
				{
					ResultProcessor.processItem( item, -1 );
				}
			}
			break;

		case 558:
			// Tool Time
			if ( text.indexOf( "You acquire an item" ) != -1 )
			{
				int amount = 3 + ChoiceManager.lastDecision;
				ResultProcessor.processItem( ItemPool.LOLLIPOP_STICK, -amount );
			}
			break;
		}

		if ( ChoiceManager.initializeAfterChoice && text.indexOf( "choice.php" ) == -1 )
		{
			Runnable initializeRunner = new Runnable()
			{
				public void run()
				{
					LoginManager.login( KoLCharacter.getUserName() );
					ChoiceManager.initializeAfterChoice = false;
				}
			};

			RequestThread.runInParallel( initializeRunner );
		}
	}

	public static void visitChoice( final GenericRequest request )
	{
		String responseText = request.responseText;
		Matcher matcher = ChoiceManager.CHOICE_PATTERN.matcher( responseText );
		boolean found = matcher.find();

		if ( !found )
		{
			matcher = ChoiceManager.CHOICE2_PATTERN.matcher( responseText );
			found = matcher.find();
		}

		if ( !found )
		{
			matcher = ChoiceManager.CHOICE3_PATTERN.matcher( responseText );
			found = matcher.find();
		}

		if ( !found )
		{
			// choice.php did not offer us any choices.
			// This would be a bug in KoL itself.
			return;
		}

		ChoiceManager.lastChoice = StringUtilities.parseInt( matcher.group( 1 ) );
		ChoiceManager.lastResponseText = responseText;

		switch ( ChoiceManager.lastChoice )
		{
		// Wheel In the Pyramid, Keep on Turning
		case 134:

			// Visiting this choice removes the carved wooden wheel
			// from your inventory.

			if ( InventoryManager.getCount( ItemPool.CARVED_WOODEN_WHEEL ) > 0 )
			{
				ResultProcessor.processItem( ItemPool.CARVED_WOODEN_WHEEL, -1 );
			}

			break;

		case 360:
			// Wumpus Hunt
			WumpusManager.visitChoice( responseText );
			break;

		case 460:
			// Space Trip
			ArcadeRequest.visitSpaceTripChoice( responseText );
			break;

		case 471:
			// DemonStar
			ArcadeRequest.visitDemonStarChoice( responseText );
			break;

		case 485:
			// Fighters Of Fighting
			ArcadeRequest.visitFightersOfFightingChoice( responseText );
			break;

		case 486:
			// DungeonFist!
			ArcadeRequest.visitDungeonFistChoice( responseText );
			break;

		case 488:
			// Meteoid
			ArcadeRequest.visitMeteoidChoice( responseText );
			break;

		case 496:
			// Crate Expectations
		case 509:
			// Of Course!
		case 510:
			// Those Who Came Before You
		case 511:
			// If it's Tiny, is it Still a Mansion?
		case 512:
			// Hot and Cold Running Rats
		case 513:
			//  Staring Down the Barrel
		case 514:
			// 1984 Had Nothing on This Cellar
		case 515:
			// A Rat's Home...
			TavernRequest.postTavernVisit( request );
			break;

		case 537:
			// Play Porko!
			SpaaaceRequest.visitPorkoChoice( responseText );
			break;

		case 540:
			// Big-Time Generator
			SpaaaceRequest.visitGeneratorChoice( responseText );
			break;
		}
	}

	private static void checkGuyMadeOfBees( final GenericRequest request )
	{
		KoLCharacter.ensureUpdatedGuyMadeOfBees();

		String text = request.responseText;
		String urlString = request.getPath();

		if ( urlString.startsWith( "fight.php" ) )
		{
			if ( text.indexOf( "guy made of bee pollen" ) != -1 )
			{
				// Record that we beat the guy made of bees.
				Preferences.setBoolean( "guyMadeOfBeesDefeated", true );
			}
		}
		else if ( urlString.startsWith( "choice.php" ) )
		{
			if ( text.indexOf( "that ship is sailed" ) != -1 )
			{
				// For some reason, we didn't notice when we
				// beat the guy made of bees. Record it now.
				Preferences.setBoolean( "guyMadeOfBeesDefeated", true );
			}
			else
			{
				// Increment the number of times we've
				// called the guy made of bees.
				Preferences.increment( "guyMadeOfBeesCount", 1, 5, true );
			}

		}
	}

	private static String hobopolisBossName( final int choice )
	{
		switch ( choice )
		{
		case 200:
			// Enter The Hoboverlord
			return "Hodgman";
		case 201:
			// Home, Home in the Range
			return "Ol' Scratch";
		case 202:
			// Bumpity Bump Bump
			return "Frosty";
		case 203:
			// Deep Enough to Dive
			return "Oscus";
		case 204:
			// Welcome To You!
			return "Zombo";
		case 205:
			// Van, Damn
			return "Chester";
		case 518:
			// Clear and Present Danger
			return "Uncle Hobo";
		}

		return "nobody";
	}

	private static void checkDungeonSewers( final GenericRequest request )
	{
		// Somewhat Higher and Mostly Dry
		// Disgustin' Junction
		// The Former or the Ladder

		String text = request.responseText;
		int explorations = 0;

		int dumplings = InventoryManager.getAccessibleCount( ItemPool.DUMPLINGS );
		int wads = InventoryManager.getAccessibleCount( ItemPool.SEWER_WAD );
		int oozeo = InventoryManager.getAccessibleCount( ItemPool.OOZE_O );
		int oil = InventoryManager.getAccessibleCount( ItemPool.OIL_OF_OILINESS );
		int umbrella = InventoryManager.getAccessibleCount( ItemPool.GATORSKIN_UMBRELLA );

		// You steel your nerves and descend into the darkened tunnel.
		if ( text.indexOf( "You steel your nerves and descend into the darkened tunnel." ) == -1 )
		{
			return;
		}

		// *** CODE TESTS ***

		// You flip through your code binder, and figure out that one
		// of the glyphs is code for 'shortcut', while the others are
		// the glyphs for 'longcut' and 'crewcut', respectively. You
		// head down the 'shortcut' tunnel.

		if ( text.indexOf( "'crewcut'" ) != -1 )
		{
			explorations += 1;
		}

		// You flip through your code binder, and gain a basic
		// understanding of the sign: "This ladder just goes in a big
		// circle. If you climb it you'll end up back where you
		// started." You continue down the tunnel, instead.

		if ( text.indexOf( "in a big circle" ) != -1 )
		{
			explorations += 3;
		}

		// You consult your binder and translate the glyphs -- one of
		// them says "This way to the Great Egress" and the other two
		// are just advertisements for Amalgamated Ladderage, Inc. You
		// head toward the Egress.

		if ( text.indexOf( "Amalgamated Ladderage" ) != -1 )
		{
			explorations += 5;
		}

		// *** ITEM TESTS ***

		// "How about these?" you ask, offering the fish some of your
		// unfortunate dumplings.
		if ( text.indexOf( "some of your unfortunate dumplings" ) != -1 )
		{
			// Remove unfortunate dumplings from inventory
			ResultProcessor.processItem( ItemPool.DUMPLINGS, -1 );
			++explorations;
			dumplings = InventoryManager.getAccessibleCount( ItemPool.DUMPLINGS );
			if ( dumplings <= 0 )
			{
				RequestLogger.printLine( "That was your last unfortunate dumplings." );
			}
		}

		// Before you can ask him what kind of tribute he wants, you
		// see his eyes light up at the sight of your sewer wad.
		if ( text.indexOf( "the sight of your sewer wad" ) != -1 )
		{
			// Remove sewer wad from inventory
			ResultProcessor.processItem( ItemPool.SEWER_WAD, -1 );
			++explorations;
			wads = InventoryManager.getAccessibleCount( ItemPool.SEWER_WAD );
			if ( wads <= 0 )
			{
				RequestLogger.printLine( "That was your last sewer wad." );
			}
		}

		// He finds a bottle of Ooze-O, and begins giggling madly. He
		// uncorks the bottle, takes a drink, and passes out in a heap.
		if ( text.indexOf( "He finds a bottle of Ooze-O" ) != -1 )
		{
			// Remove bottle of Ooze-O from inventory
			ResultProcessor.processItem( ItemPool.OOZE_O, -1 );
			++explorations;
			oozeo = InventoryManager.getAccessibleCount( ItemPool.OOZE_O );
			if ( oozeo <= 0 )
			{
				RequestLogger.printLine( "That was your last bottle of Ooze-O." );
			}
		}

		// You grunt and strain, but you can't manage to get between
		// the bars. In a flash of insight, you douse yourself with oil
		// of oiliness (it takes three whole bottles to cover your
		// entire body) and squeak through like a champagne cork. Only
		// without the bang, and you're not made out of cork, and
		// champagne doesn't usually smell like sewage. Anyway. You
		// continue down the tunnel.
		if ( text.indexOf( "it takes three whole bottles" ) != -1 )
		{
			// Remove 3 bottles of oil of oiliness from inventory
			ResultProcessor.processItem( ItemPool.OIL_OF_OILINESS, -3 );
			++explorations;
			oil = InventoryManager.getAccessibleCount( ItemPool.OIL_OF_OILINESS );
			if ( oil < 3 )
			{
				RequestLogger.printLine( "You have less than 3 bottles of oil of oiliness left." );
			}
		}

		// Fortunately, your gatorskin umbrella allows you to pass
		// beneath the sewagefall without incident. There's not much
		// left of the umbrella, though, and you discard it before
		// moving deeper into the tunnel.
		if ( text.indexOf( "your gatorskin umbrella allows you to pass" ) != -1 )
		{
			// Unequip gatorskin umbrella and discard it.

			++explorations;
			AdventureResult item = ItemPool.get( ItemPool.GATORSKIN_UMBRELLA, 1 );
			int slot = EquipmentManager.WEAPON;
			if ( KoLCharacter.hasEquipped( item, EquipmentManager.WEAPON ) )
			{
				slot = EquipmentManager.WEAPON;
			}
			else if ( KoLCharacter.hasEquipped( item, EquipmentManager.OFFHAND ) )
			{
				slot = EquipmentManager.OFFHAND;
			}

			EquipmentManager.setEquipment( slot, EquipmentRequest.UNEQUIP );

			AdventureResult.addResultToList( KoLConstants.inventory, item );
			ResultProcessor.processItem( ItemPool.GATORSKIN_UMBRELLA, -1 );
			umbrella = InventoryManager.getAccessibleCount( item );
			if ( umbrella > 0 )
			{
				RequestThread.postRequest( new EquipmentRequest( item, slot ) );
			}
			else
			{
				RequestLogger.printLine( "That was your last gatorskin umbrella." );
			}
		}

		// *** GRATE ***

		// Further into the sewer, you encounter a halfway-open grate
		// with a crank on the opposite side. What luck -- looks like
		// somebody else opened this grate from the other side!

		if ( text.indexOf( "somebody else opened this grate" ) != -1 )
		{
			explorations += 5;
		}

		// Now figure out how to say what happened. If the player wants
		// to stop if runs out of test items, generate an ERROR and
		// list the missing items in the status message. Otherwise,
		// simply tell how many explorations were accomplished.

		AdventureResult result = AdventureResult.tallyItem(
			"sewer tunnel explorations", explorations, false );
		AdventureResult.addResultToList( KoLConstants.tally, result );

		int state = KoLConstants.CONTINUE_STATE;
		String message = "+" + explorations + " Explorations";

		if ( Preferences.getBoolean( "requireSewerTestItems" ) )
		{
			String missing = "";
			String comma = "";

			if ( dumplings < 1 )
			{
				missing = missing + comma + "unfortunate dumplings";
				comma = ", ";
			}
			if ( wads < 1 )
			{
				missing = missing + comma + "sewer wad";
				comma = ", ";
			}
			if ( oozeo < 1 )
			{
				missing = missing + comma + "bottle of Ooze-O";
				comma = ", ";
			}
			if ( oil < 1 )
			{
				missing = missing + comma + "oil of oiliness";
				comma = ", ";
			}
			if ( umbrella < 1 )
			{
				missing = missing + comma + "gatorskin umbrella";
				comma = ", ";
			}
			if ( !missing.equals( "" ) )
			{
				state = KoLConstants.ERROR_STATE;
				message += ", NEED: " + missing;
			}
		}

		KoLmafia.updateDisplay( state, message );
	}

	private static final boolean specialChoiceHandling( final int choice, final GenericRequest request )
	{
		String decision = null;
		switch ( choice )
		{
		case 485:
			// Fighters of Fighting
			decision = ArcadeRequest.autoChoiceFightersOfFighting( request );
			break;
		}

		if ( decision == null )
		{
			return false;
		}

		request.addFormField( "whichchoice", String.valueOf( choice ) );
		request.addFormField( "option", decision );
		request.addFormField( "pwd", GenericRequest.passwordHash );
		request.run();

		ChoiceManager.lastResponseText = request.responseText;

		return true;
	}

	private static final String specialChoiceDecision( final int choice, final String option, final String decision, final int stepCount, final String responseText )
	{
		// A few choices have non-standard options: 0 is not Manual Control
		switch ( choice )
		{
		// Out in the Garden
		case 89:

			// Handle the maidens adventure in a less random
			// fashion that's actually useful.

			switch ( StringUtilities.parseInt( decision ) )
			{
			case 0:
				return String.valueOf( KoLConstants.RNG.nextInt( 2 ) + 1 );
			case 1:
			case 2:
				return decision;
			case 3:
				return KoLConstants.activeEffects.contains( ChoiceManager.MAIDEN_EFFECT ) ? String.valueOf( KoLConstants.RNG.nextInt( 2 ) + 1 ) : "3";
			case 4:
				return KoLConstants.activeEffects.contains( ChoiceManager.MAIDEN_EFFECT ) ? "1" : "3";
			case 5:
				return KoLConstants.activeEffects.contains( ChoiceManager.MAIDEN_EFFECT ) ? "2" : "3";
			}
			return decision;
		}

		// If the user wants manual control, let 'em have it.
		if ( decision.equals( "0" ) )
		{
			return decision;
		}

		// Otherwise, modify the decision based on character state
		switch ( choice )
		{
		// Heart of Very, Very Dark Darkness
		case 5:
			if ( InventoryManager.getCount( ItemPool.INEXPLICABLY_GLOWING_ROCK ) < 1 )
			{
				return "2";
			}
			return "1";

		// How Depressing
		case 7:
			if ( !KoLCharacter.hasEquipped( ItemPool.get( ItemPool.SPOOKY_GLOVE, 1 ) ) )
			{
				return "2";
			}
			return "1";

		// A Three-Tined Fork
		// Footprints
		case 26:
		case 27:

			// Check if we can satisfy one of user's conditions
			for ( int i = 0; i < 12; ++i )
			{
				if ( GoalManager.hasGoal( AdventureDatabase.WOODS_ITEMS[ i ] ) )
				{
					return choice == 26 ? String.valueOf( i / 4 + 1 ) : String.valueOf( i % 4 / 2 + 1 );
				}
			}

			return decision;

		case 48: case 49: case 50: case 51: case 52:
		case 53: case 54: case 55: case 56: case 57:
		case 58: case 59: case 60: case 61: case 62:
		case 63: case 64: case 65: case 66: case 67:
		case 68: case 69: case 70:

			// Choices in the Violet Fog
			if ( decision.equals( "" ) )
			{
				return VioletFogManager.handleChoice( choice );
			}

			return decision;

		// Take a Look, it's in a Book!
		case 81:

			// If we've already unlocked the gallery, try
			// to unlock the second floor.

			if ( decision.equals( "1" ) && Preferences.getInteger( "lastGalleryUnlock" ) == KoLCharacter.getAscensions() )
			{
				return "99";
			}
			// fall through

		// Take a Look, it's in a Book!
		case 80:

			// If we've already unlocked the second floor,
			// ignore this choice adventure.

			if ( decision.equals( "99" ) && Preferences.getInteger( "lastSecondFloorUnlock" ) == KoLCharacter.getAscensions() )
			{
				return "4";
			}
			return decision;

		// One NightStand (simple wooden)
		case 85:

			// If the player is looking for the ballroom key,
			// then update their preferences so that KoLmafia
			// automatically switches things for them.

			if ( GoalManager.hasGoal( ChoiceManager.BALLROOM_KEY ) )
			{
				if ( !KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) )
				{
					return (Preferences.getInteger( "lastBallroomUnlock" ) == KoLCharacter.getAscensions() ? "2" : "1");
				}
			}
			else if ( decision.equals( "4" ) )
			{
				if ( !KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) )
				{
					return (Preferences.getInteger( "lastBallroomUnlock" ) == KoLCharacter.getAscensions() ? "2" : "1");
				}
				return "1";
			}
			else if ( decision.equals( "5" ) )
			{
				if ( !KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) )
				{
					return (Preferences.getInteger( "lastBallroomUnlock" ) == KoLCharacter.getAscensions() ? "2" : "1");
				}
				return "3";
			}
			else
			{
				for ( int i = 0; i < ChoiceManager.MISTRESS_ITEMS.length; ++i )
				{
					if ( GoalManager.hasGoal( ChoiceManager.MISTRESS_ITEMS[ i ] ) )
					{
						return "3";
					}
				}
			}

			return decision;

		case 91:

			// Sometimes, the choice adventure for the louvre
			// loses track of whether to ignore the louvre or not.

			LouvreManager.resetDecisions();
			return Preferences.getInteger( "louvreGoal" ) != 0 ? "1" : "2";

		case 92: case 93: case 94: case 95: case 96:
		case 97: case 98: case 99: case 100: case 101:
		case 102: case 103: case 104:
			// Choices in the Louvre

			if ( decision.equals( "" ) )
			{
				return LouvreManager.handleChoice( choice, stepCount );
			}

			return decision;

		// No sir, away! A papaya war is on!
		case 127:
			switch ( StringUtilities.parseInt( decision ) )
			{
			case 1:
			case 2:
			case 3:
				return decision;
			case 4:
				return ChoiceManager.PAPAYA.getCount( KoLConstants.inventory ) >= 3 ? "2" : "1";
			case 5:
				return ChoiceManager.PAPAYA.getCount( KoLConstants.inventory ) >= 3 ? "2" : "3";
			}
			return decision;

		// Bureaucracy of the Damned
		case 161:
			// Check if we have all of Azazel's objects of evil
			for ( int i = 2566; i <= 2568; ++i )
			{
				AdventureResult item = new AdventureResult( i, 1 );
				if ( !KoLConstants.inventory.contains( item ) )
				{
					return "4";
				}
			}
			return "1";

		// Choice 162 is Between a Rock and Some Other Rocks
		case 162:

			// Auto-skip the goatlet adventure if you're not wearing
			// the mining outfit so it can be tried again later.

			return ( EquipmentManager.isWearingOutfit( 8 ) || KoLConstants.activeEffects.contains( SorceressLairManager.EARTHEN_FIST ) ) ? decision : "2";

		// That Explains All The Eyepatches
		case 184:
			switch ( KoLCharacter.getPrimeIndex() * 10 + StringUtilities.parseInt( decision ) )
			{
			// Options 4-6 are mapped to the actual class-specific options:
			// 4=drunk & stats, 5=rotgut, 6=combat (not available to Myst)
			// Mus
			case 04:
				return "3";
			case 05:
				return "2";
			case 06:
				return "1";
			// Mys
			case 14:
				return "1";
			case 15:
				return "2";
			case 16:
				return "3";
			// Mox
			case 24:
				return "2";
			case 25:
				return "3";
			case 26:
				return "1";
			}
			return decision;

		// Chatterboxing
		case 191:
			boolean trink = InventoryManager.getCount( ItemPool.VALUABLE_TRINKET ) > 0;
			switch ( StringUtilities.parseInt( decision ) )
			{
			case 5:	// banish or mox
				return trink ? "2" : "1";
			case 6:	// banish or mus
				return trink ? "2" : "3";
			case 7:	// banish or mys
				return trink ? "2" : "4";
			case 8:	// banish or mainstat
				if ( trink ) return "2";
				switch ( KoLCharacter.mainStat() )
				{
				case KoLConstants.MUSCLE:
					return "3";
				case KoLConstants.MYSTICALITY:
					return "4";
				case KoLConstants.MOXIE:
					return "1";
				default:
					return "0";
				}
			}
			return decision;

		// In the Shade
		case 298:
			if ( decision.equals( "1" ) )
			{
				int seeds = InventoryManager.getCount( ItemPool.SEED_PACKET );
				int slime = InventoryManager.getCount( ItemPool.GREEN_SLIME );
				if ( seeds < 1 || slime < 1 )
				{
					return "2";
				}
			}
			return decision;

		// A Vent Horizon
		case 304:

			// If we've already summoned three batters today or we
			// don't have enough MP, ignore this choice adventure.

			if ( decision.equals( "1" ) && ( Preferences.getInteger( "tempuraSummons" ) == 3 || KoLCharacter.getCurrentMP() < 200 ) )
			{
				return "2";
			}
			return decision;

		// There is Sauce at the Bottom of the Ocean
		case 305:

			// If we don't have a Mer-kin pressureglobe, ignore
			// this choice adventure.

			if ( decision.equals( "1" ) && InventoryManager.getCount( ItemPool.MERKIN_PRESSUREGLOBE ) < 1 )
			{
				return "2";
			}
			return decision;

		// Barback
		case 309:

			// If we've already found three seaodes today,
			// ignore this choice adventure.

			if ( decision.equals( "1" ) && Preferences.getInteger( "seaodesFound" ) == 3 )
			{
				return "2";
			}
			return decision;

		// Dungeon Fist!
		case 486:
			if ( !ChoiceManager.initializeAfterChoice )
			{	// Don't automate this if we logged in in the middle of the game -
				// the auto script isn't robust enough to handle arbitrary starting points.
				return ArcadeRequest.autoDungeonFist( stepCount );
			}
			return decision;

		// Arboreal Respite
		case 502:
			if ( decision.equals( "2" ) )
			{
				// mosquito larva, tree-holed coin, vampire
				if ( !Preferences.getString( "choiceAdventure505" ).equals( "2" ) )
				{
					return decision;
				}

				// We want a tree-holed coin. If we already
				// have one, get Spooky Temple Map instead
				if ( InventoryManager.getCount( ItemPool.TREE_HOLED_COIN ) > 0 )
				{
					return "3";
				}

				// We don't have a tree-holed coin. Either
				// obtain one or exit without consuming an
				// adventure
			}
			return decision;

		// Tree's Last Stand
		case 504:

			// If we have Bar Skins, sell them all
			if ( InventoryManager.getCount( ItemPool.BAR_SKIN ) > 1 )
			{
				return "2";
			}
			else if ( InventoryManager.getCount( ItemPool.BAR_SKIN ) > 0 )
			{
				return "1";
			}

			// If we don't have a Spooky Sapling, buy one
			// unless we've already unlocked the Hidden Temple
			//
			// We should buy one if it is on our conditions - i.e.,
			// the player is intentionally collecting them - but we
			// have to make sure that each purchased sapling
			// decrements the condition so we don't loop and buy
			// too many.

			if ( InventoryManager.getCount( ItemPool.SPOOKY_SAPLING ) == 0 &&
			     !KoLCharacter.getTempleUnlocked() &&
			     KoLCharacter.getAvailableMeat() >= 100 )
			{
				return "3";
			}

			// Otherwise, exit this choice
			return "4";

		// Dark in the Attic
		case 549:

			// Some choices appear depending on whether
			// the boombox is on or off

			// 1 - acquire staff guides
			// 2 - acquire ghost trap
			// 3 - turn on boombox (raise area ML)
			// 4 - turn off boombox (lower area ML)
			// 5 - mass kill werewolves

			boolean boomboxOn = responseText.indexOf( "sets your heart pounding and pulse racing" ) != -1;

			switch ( StringUtilities.parseInt( decision ) )
			{
			case 0 : // show in browser
			case 1 : // acquire staff guides
			case 2 : // acquire ghost trap
				return decision;
			case 3 : // mass kill werewolves with silver shotgun shell
				return "5";
			case 4 : // raise area ML, then acquire staff guides
				return !boomboxOn ? "3" : "1";
			case 5 : // raise area ML, then acquire ghost trap
				return !boomboxOn ? "3" : "2";
			case 6 : // raise area ML, then mass kill werewolves
				return !boomboxOn ? "3" : "5";
			case 7 : // raise area ML, then mass kill werewolves or ghost trap
				return !boomboxOn ? "3" :
				       InventoryManager.getCount( ItemPool.SILVER_SHOTGUN_SHELL ) > 0 ? "5" : "2";
			case 8 : // lower area ML, then acquire staff guides
				return boomboxOn ? "4" : "1";
			case 9 : // lower area ML, then acquire ghost trap
				return boomboxOn ? "4" : "2";
			case 10: // lower area ML, then mass kill werewolves
				return boomboxOn ? "4" : "5";
			case 11: // lower area ML, then mass kill werewolves or ghost trap
				return boomboxOn ? "4" :
				       InventoryManager.getCount( ItemPool.SILVER_SHOTGUN_SHELL ) > 0 ? "5" : "2";
			}
			return decision;

		// The Unliving Room
		case 550:

			// Some choices appear depending on whether
			// the windows are opened or closed

			// 1 - close the windows (raise area ML)
			// 2 - open the windows (lower area ML)
			// 3 - mass kill zombies
			// 4 - mass kill skeletons
			// 5 - get costume item

			boolean windowsClosed = responseText.indexOf( "covered all their windows" ) != -1;
			int chainsaw = InventoryManager.getCount( ItemPool.CHAINSAW_CHAIN );
			int mirror = InventoryManager.getCount( ItemPool.FUNHOUSE_MIRROR );

			switch ( StringUtilities.parseInt( decision ) )
			{
			case 0 : // show in browser
				return decision;
			case 1 : // mass kill zombies with chainsaw chain
				return "3";
			case 2 : // mass kill skeletons with funhouse mirror
				return "4";
			case 3 : // get costume item
				return "5";
			case 4 : // raise area ML, then mass kill zombies
				return !windowsClosed ? "1" : "3";
			case 5 : // raise area ML, then mass kill skeletons
				return !windowsClosed ? "1" : "4";
			case 6 : // raise area ML, then mass kill zombies/skeletons
				return !windowsClosed ? "1" :
				       chainsaw > mirror ? "3" : "4";
			case 7 : // raise area ML, then get costume item
				return !windowsClosed ? "1" : "5";
			case 8 : // lower area ML, then mass kill zombies
				return windowsClosed ? "2" : "3";
			case 9 : // lower area ML, then mass kill skeletons
				return windowsClosed ? "2" : "4";
			case 10: // lower area ML, then mass kill zombies/skeletons
				return windowsClosed ? "2" :
				       chainsaw > mirror ? "3" : "4";
			case 11: // lower area ML, then get costume item
				return windowsClosed ? "2" : "5";
			}
			return decision;

		// Debasement
		case 551:

			// Some choices appear depending on whether
			// the fog machine is on or off

			// 1 - Prop Deportment (choice adventure 552)
			// 2 - mass kill vampires
			// 3 - turn up the fog machine (raise area ML)
			// 4 - turn down the fog machine (lower area ML)

			boolean fogOn = responseText.indexOf( "white clouds of artificial fog" ) != -1;

			switch ( StringUtilities.parseInt( decision ) )
			{
			case 0: // show in browser
			case 1: // Prop Deportment
			case 2: // mass kill vampires with plastic vampire fangs
				return decision;
			case 3: // raise area ML, then Prop Deportment
				return fogOn ? "1" : "3";
			case 4: // raise area ML, then mass kill vampires
				return fogOn ? "2" : "3";
			case 5: // lower area ML, then Prop Deportment
				return fogOn ? "4" : "1";
			case 6: // lower area ML, then mass kill vampires
				return fogOn ? "4" : "2";
			}
			return decision;

		// Prop Deportment
		case 552:

			// Allow the user to let Mafia pick
			// which prop to get

			// 1 - chainsaw
			// 2 - Relocked and Reloaded
			// 3 - funhouse mirror
			// 4 - chainsaw chain OR funhouse mirror

			chainsaw = InventoryManager.getCount( ItemPool.CHAINSAW_CHAIN );
			mirror = InventoryManager.getCount( ItemPool.FUNHOUSE_MIRROR );

			switch ( StringUtilities.parseInt( decision ) )
			{
			case 0: // show in browser
			case 1: // chainsaw chain
			case 2: // Relocked and Reloaded
			case 3: // funhouse mirror
				return decision;
			case 4: // chainsaw chain OR funhouse mirror
				return chainsaw < mirror ? "1" : "3";
			}
			return decision;

		// Relocked and Reloaded
		case 553:

			// Choices appear depending on whether
			// you have the item to melt

			// 1 - Maxwell's Silver Hammer
			// 2 - silver tongue charrrm bracelet
			// 3 - silver cheese-slicer
			// 4 - silver shrimp fork
			// 5 - silver pat&eacute; knife
			// 6 - don't melt anything

			int item = 0;

			switch ( StringUtilities.parseInt( decision ) )
			{
			case 0: // show in browser
			case 6: // don't melt anything
				return decision;
			case 1: // melt Maxwell's Silver Hammer
				item = ItemPool.MAXWELL_HAMMER;
				break;
			case 2: // melt silver tongue charrrm bracelet
				item = ItemPool.TONGUE_BRACELET;
				break;
			case 3: // melt silver cheese-slicer
				item = ItemPool.SILVER_CHEESE_SLICER;
				break;
			case 4: // melt silver shrimp fork
				item = ItemPool.SILVER_SHRIMP_FORK;
				break;
			case 5: // melt silver pat&eacute; knife
				item = ItemPool.SILVER_PATE_KNIFE;
				break;
			}

			if ( item == 0 )
			{
				return "6";
			}
			return InventoryManager.getCount( item ) > 0 ? decision : "6";

		// Tool Time
		case 558:

			// Choices appear depending on whether
			// you have enough lollipop sticks

			// 1 - sucker bucket (4 lollipop sticks)
			// 2 - sucker kabuto (5 lollipop sticks)
			// 3 - sucker hakama (6 lollipop sticks)
			// 4 - sucker tachi (7 lollipop sticks)
			// 5 - sucker scaffold (8 lollipop sticks)
			// 6 - skip adventure

			if ( decision.equals( "0" ) || decision.equals( "6" ) )
			{
				return decision;
			}

			int amount = 3 + StringUtilities.parseInt( decision );
			return InventoryManager.getCount( ItemPool.LOLLIPOP_STICK ) >= amount ? decision : "6";
		}

		return decision;
	}

	private static final String pickOutfitChoice( final String option, final String decision )
	{
		// Find the options for the choice we've encountered

		boolean matchFound = false;
		String[] possibleDecisions = null;
		String[] possibleDecisionSpoilers = null;

		for ( int i = 0; i < ChoiceManager.CHOICE_ADVS.length && !matchFound; ++i )
		{
			if ( ChoiceManager.CHOICE_ADVS[ i ].getSetting().equals( option ) )
			{
				matchFound = true;
				possibleDecisions = ChoiceManager.CHOICE_ADVS[ i ].getItems();
				possibleDecisionSpoilers = ChoiceManager.CHOICE_ADVS[ i ].getOptions();
			}
		}

		for ( int i = 0; i < ChoiceManager.CHOICE_ADV_SPOILERS.length && !matchFound; ++i )
		{
			if ( ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getSetting().equals( option ) )
			{
				matchFound = true;
				possibleDecisions = ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getItems();
				possibleDecisionSpoilers = ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getOptions();
			}
		}

		// If it's not in the table (the castle wheel, for example) or
		// isn't an outfit completion choice, return the player's
		// chosen decision.

		if ( possibleDecisionSpoilers == null )
		{
			return decision.equals( "0" ) ? "1" : decision;
		}

		// Choose an item in the conditions first, if it's available.
		// This allows conditions to override existing choices.

		if ( possibleDecisions != null )
		{
			for ( int i = 0; i < possibleDecisions.length; ++i )
			{
				if ( possibleDecisions[ i ] == null )
				{
					continue;
				}

				AdventureResult item = new AdventureResult( StringUtilities.parseInt( possibleDecisions[ i ] ), 1 );
				if ( GoalManager.hasGoal( item ) )
				{
					return String.valueOf( i + 1 );
				}

				if ( possibleDecisions.length < StringUtilities.parseInt( decision ) && !InventoryManager.hasItem( item ) )
				{
					return String.valueOf( i + 1 );
				}
			}
		}

		if ( possibleDecisions == null )
		{
			return decision.equals( "0" ) ? "1" : decision;
		}

		// If this is an ignore decision, then go ahead and ignore
		// the choice adventure

		int decisionIndex = StringUtilities.parseInt( decision ) - 1;
		if ( possibleDecisions.length < possibleDecisionSpoilers.length && possibleDecisionSpoilers[ decisionIndex ].equals( "skip adventure" ) )
		{
			return decision;
		}

		// If no item is found in the conditions list, and the player
		// has a non-ignore decision, go ahead and use it.

		if ( !decision.equals( "0" ) && decisionIndex < possibleDecisions.length )
		{
			return decision;
		}

		// Choose a null choice if no conditions match what you're
		// trying to look for.

		for ( int i = 0; i < possibleDecisions.length; ++i )
		{
			if ( possibleDecisions[ i ] == null )
			{
				return String.valueOf( i + 1 );
			}
		}

		// If they have everything and it's an ignore choice, then use
		// the first choice no matter what.

		return "1";
	}

	public static final void addGoalButton( final StringBuffer buffer, final String goal )
	{
		// Insert a "Goal" button in-line
		String search = "<form name=choiceform1";
		int index = buffer.lastIndexOf( search );
		if ( index == -1 )
		{
			return;
		}

		// Build a "Goal" button
		StringBuffer button = new StringBuffer();
		String url = "/KoLmafia/specialCommand?cmd=choice-goal&pwd=" + GenericRequest.passwordHash;
		button.append( "<form name=goalform action='" + url + "' method=post>" );
		button.append( "<input class=button type=submit value=\"Go To Goal\">" );

		// Add the goal
		button.append( "<br><font size=-1>(" );
		button.append( goal );
		button.append( ")</font></form>" );

		// Insert it into the page
		buffer.insert( index, button );
	}

	public static final void gotoGoal()
	{
		String responseText = ChoiceManager.lastResponseText;
		GenericRequest request = ChoiceManager.CHOICE_HANDLER;
		ChoiceManager.processChoiceAdventure( request, responseText );

		StringBuffer buffer = new StringBuffer( request.responseText );
		RequestEditorKit.getFeatureRichHTML( request.getURLString(), buffer );
		StringUtilities.insertAfter( buffer, "<head>", RelayServer.getBase( "choice.php" ) );
		RelayRequest.specialCommandResponse = buffer.toString();
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		Matcher matcher = ChoiceManager.URL_CHOICE_PATTERN.matcher( urlString );
		if ( matcher.find() )
		{
			int choice = StringUtilities.parseInt ( matcher.group( 1 ) );
			switch ( choice )
			{
			case 443:
				// Chess Puzzle
				return RabbitHoleManager.registerChessboardRequest( urlString );
			case 460: case 461: case 462: case 463: case 464:
			case 465:	    case 467: case 468: case 469:
			case 470:	    case 472: case 473: case 474:
			case 475: case 476: case 477: case 478: case 479:
			case 480: case 481: case 482: case 483: case 484:
				// Space Trip
			case 471:
				// DemonStar
			case 485:
				// Fighters Of Fighting
			case 486:
				// Dungeon Fist!
			case 488: case 489: case 490: case 491:
				// Meteoid
				return true;
			}
			matcher = ChoiceManager.URL_OPTION_PATTERN.matcher( urlString );
			if ( matcher.find() )
			{
				int decision = StringUtilities.parseInt ( matcher.group( 1 ) );
				String desc = "unknown";
				String[][] possibleDecisions = ChoiceManager.choiceSpoilers( choice );
				if ( possibleDecisions != null && decision > 0 &&
					decision <= possibleDecisions[ 2 ].length )
				{
					desc = possibleDecisions[ 2 ][ decision - 1 ];
				}
				RequestLogger.updateSessionLog( "Took choice " + choice + "/" +
					decision + ": " + desc );
				// For now, leave the raw URL in the log in case some analysis
				// tool is relying on it.
				//return true;
			}
		}

		// By default, we log the url of any choice we take
		RequestLogger.updateSessionLog( urlString );

		return true;
	}
}
