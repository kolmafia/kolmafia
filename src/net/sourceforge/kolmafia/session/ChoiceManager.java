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

import java.util.ArrayList;
import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.ArcadeRequest;
import net.sourceforge.kolmafia.request.BeerPongRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PasswordHashRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.textui.command.ChoiceCommand;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.MemoriesDecorator;

public abstract class ChoiceManager
{
	public static final GenericRequest CHOICE_HANDLER = new PasswordHashRequest( "choice.php" );

	public static boolean handlingChoice = false;
	private static int lastChoice = 0;
	private static int lastDecision = 0;
	public static String lastResponseText = "";
	private static int skillUses = 0;
	private static boolean canWalkAway;

	private enum PostChoiceAction {
		NONE,
		INITIALIZE,
		ASCEND;
	};

	private static PostChoiceAction action = PostChoiceAction.NONE;

	private static final Pattern [] CHOICE_PATTERNS =
	{
		Pattern.compile( "name=['\"]?whichchoice['\"]? value=['\"]?(\\d+)['\"]?" ),
		Pattern.compile( "value=['\"]?(\\d+)['\"]? name=['\"]?whichchoice['\"]?" ),
		Pattern.compile( "choice.php\\?whichchoice=(\\d+)" ),
	};

	public static int currentChoice()
	{
		return ChoiceManager.handlingChoice ? ChoiceManager.lastChoice : 0;
	}

	public static int extractChoice( final String responseText )
	{
		for ( int i = 0; i < ChoiceManager.CHOICE_PATTERNS.length; ++i )
		{
			Matcher matcher = CHOICE_PATTERNS[i].matcher( responseText );
			if ( matcher.find() )
			{
				return StringUtilities.parseInt( matcher.group( 1 ) );
			}
		}

		return 0;
	}

	private static final Pattern URL_CHOICE_PATTERN = Pattern.compile( "whichchoice=(\\d+)" );
	private static final Pattern URL_OPTION_PATTERN = Pattern.compile( "(?<!force)option=(\\d+)" );
	private static final Pattern TATTOO_PATTERN = Pattern.compile( "otherimages/sigils/hobotat(\\d+).gif" );
	private static final Pattern REANIMATOR_ARM_PATTERN = Pattern.compile( "(\\d+) arms??<br>" );
	private static final Pattern REANIMATOR_LEG_PATTERN = Pattern.compile( "(\\d+) legs??<br>" );
	private static final Pattern REANIMATOR_SKULL_PATTERN = Pattern.compile( "(\\d+) skulls??<br>" );
	private static final Pattern REANIMATOR_WEIRDPART_PATTERN = Pattern.compile( "(\\d+) weird random parts??<br>" );
	private static final Pattern REANIMATOR_WING_PATTERN = Pattern.compile( "(\\d+) wings??<br>" );
	private static final Pattern CHAMBER_PATTERN = Pattern.compile( "Chamber <b>#(\\d+)</b>" );
	private static final Pattern YEARBOOK_TARGET_PATTERN = Pattern.compile( "<b>Results:</b>.*?<b>(.*?)</b>" );
	private static final Pattern UNPERM_PATTERN = Pattern.compile( "Turning (.+)(?: \\(HP\\)) into (\\d+) karma." );
	private static final Pattern ICEHOUSE_PATTERN = Pattern.compile( "perfectly-preserved (.*?), right" );
	private static final Pattern CINDERELLA_TIME_PATTERN = Pattern.compile( "<i>It is (\\d+) minute(?:s) to midnight.</i>" );
	private static final Pattern CINDERELLA_SCORE_PATTERN = Pattern.compile( "score (?:is now|was) <b>(\\d+)</b>" );
	private static final Pattern RUMPLE_MATERIAL_PATTERN = Pattern.compile( "alt=\"(.*?)\"></td><td valign=center>(\\d+)<" );
	private static final Pattern MOTORBIKE_TIRES_PATTERN = Pattern.compile( "<b>Tires:</b> (.*?)?\\(" );
	private static final Pattern MOTORBIKE_GASTANK_PATTERN = Pattern.compile( "<b>Gas Tank:</b> (.*?)?\\(");
	private static final Pattern MOTORBIKE_HEADLIGHT_PATTERN = Pattern.compile( "<b>Headlight:</b> (.*?)?\\(" );
	private static final Pattern MOTORBIKE_COWLING_PATTERN = Pattern.compile( "<b>Cowling:</b> (.*?)?\\(" );
	private static final Pattern MOTORBIKE_MUFFLER_PATTERN = Pattern.compile( "<b>Muffler:</b> (.*?)?\\(" );
	private static final Pattern MOTORBIKE_SEAT_PATTERN = Pattern.compile( "<b>Seat:</b> (.*?)?\\(" );
	private static final Pattern POOL_SKILL_PATTERN = Pattern.compile( "(\\d+) Pool Skill</b>" );

	public static final Pattern DECISION_BUTTON_PATTERN = Pattern.compile( "<input type=hidden name=option value=(\\d+)><input class=button type=submit value=\"(.*?)\">" );

	private static final AdventureResult PAPAYA = ItemPool.get( ItemPool.PAPAYA, 1 );
	private static final AdventureResult MAIDEN_EFFECT = new AdventureResult( "Dreams and Lights", 1, true );
	private static final AdventureResult BALLROOM_KEY = ItemPool.get( ItemPool.BALLROOM_KEY, 1 );
	private static final AdventureResult MODEL_AIRSHIP = ItemPool.get( ItemPool.MODEL_AIRSHIP, 1 );
 
	private static final AdventureResult CURSE1_EFFECT = new AdventureResult( "Once-Cursed", 1, true );
	private static final AdventureResult CURSE2_EFFECT = new AdventureResult( "Twice-Cursed", 1, true );
	private static final AdventureResult CURSE3_EFFECT = new AdventureResult( "Thrice-Cursed", 1, true );
	private static final AdventureResult MCCLUSKY_FILE = ItemPool.get( ItemPool.MCCLUSKY_FILE, 1 );
	private static final AdventureResult MCCLUSKY_FILE_PAGE5 = ItemPool.get( ItemPool.MCCLUSKY_FILE_PAGE5, 1 );
	private static final AdventureResult BINDER_CLIP = ItemPool.get( ItemPool.BINDER_CLIP, 1 );
	private static final AdventureResult STONE_TRIANGLE = ItemPool.get( ItemPool.STONE_TRIANGLE, 1 );
	
	private static final AdventureResult JOCK_EFFECT = new AdventureResult( "Jamming with the Jocks", 1, true );
	private static final AdventureResult NERD_EFFECT = new AdventureResult( "Nerd is the Word", 1, true );
	private static final AdventureResult GREASER_EFFECT = new AdventureResult( "Greaser Lightnin'", 1, true );
	
	// Dreadsylvania items and effects
	private static final AdventureResult MOON_AMBER_NECKLACE = ItemPool.get( ItemPool.MOON_AMBER_NECKLACE, 1 );
	private static final AdventureResult BLOODY_KIWITINI = ItemPool.get( ItemPool.BLOODY_KIWITINI, 1 );
	private static final AdventureResult KIWITINI_EFFECT = new AdventureResult( "First Blood Kiwi", 1, true );
	private static final AdventureResult AUDITORS_BADGE = ItemPool.get( ItemPool.AUDITORS_BADGE, 1 );
	private static final AdventureResult WEEDY_SKIRT = ItemPool.get( ItemPool.WEEDY_SKIRT, 1 );
	private static final AdventureResult GHOST_SHAWL = ItemPool.get( ItemPool.GHOST_SHAWL, 1 );
	private static final AdventureResult SHEPHERDS_PIE = ItemPool.get( ItemPool.SHEPHERDS_PIE, 1 );
	private static final AdventureResult PIE_EFFECT = new AdventureResult( "Shepherd's Breath", 1, true );
	private static final AdventureResult MAKESHIFT_TURBAN = ItemPool.get( ItemPool.MAKESHIFT_TURBAN, 1 );
	private static final AdventureResult TEMPORARY_BLINDNESS = new AdventureResult( "Temporary Blindness", 1, true );

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

	private static final String[][] OLD_MAN_PSYCHOSIS_SPOILERS =
	{
		{	"Draw a Monster with a Crayon",	"-1 Crayon, Add Cray-Kin" }, { "Build a Bubble Mountain", "+3 crew, -8-10 bubbles" },
		{	"Ask Mom for More Bath Toys", "+2 crayons, +8-11 bubbles" }, { "Draw a Bunch of Coconuts with Crayons", "Block Ferocious roc, -2 crayons" },
		{	"Splash in the Water", "Add Bristled Man-O-War" }, { "Draw a Big Storm Cloud on the Shower Wall", "Block Deadly Hydra, -3 crayons" },
		{	"Knock an Action Figure Overboard", "+20-23 bubbles, -1 crew" }, { "Submerge Some Bubbles", "Block giant man-eating shark, -16 bubbles" },
		{	"Turn on the Shower Wand", "Add Deadly Hydra" }, { "Dump Bubble Bottle and Turn on the Faucet", "+13-19 bubbles" },
		{	"Put the Toy Boat on the Side of the Tub", "+4 crayon, -1 crew" }, { "Cover the Ship in Bubbles", "Block fearsome giant squid, -13-20 bubbles" },
		{	"Pull the Drain Plug", "-8 crew, -3 crayons, -17 bubbles, increase NC rate" }, { "Open a New Bathtub Crayon Box", "+3 crayons" },
		{	"Sing a Bathtime Tune", "+3 crayons, +16 bubbles, -2 crew" }, { "Surround Bubbles with Crayons", "+5 crew, -6-16 bubbles, -2 crayons" },
	};

	public static class Option
	{
		private final String name;
		private final int option;
		private final AdventureResult item;

		public Option( final String name )
		{
			this( name, 0, null );
		}

		public Option( final String name, final int option )
		{
			this( name, option, null );
		}

		public Option( final String name, final String item )
		{
			this( name, 0, item );
		}

		public Option( final String name, final int option, final String item )
		{
			this.name = name;
			this.option = option;
			this.item = item != null ? new AdventureResult( item ) : null;
		}

		public String getName()
		{
			return this.name;
		}

		public int getOption()
		{
			return this.option;
		}

		public int getDecision( final int def )
		{
			return this.option == 0 ? def : this.option;
		}

		public AdventureResult getItem()
		{
			return this.item;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	public static class ChoiceAdventure
		implements Comparable<ChoiceAdventure>
	{
		private final int choice;
		private final String zone;
		private final String setting;
		private final String name;
		private final int ordering;

		private final Object[] options;
		private Object[][] spoilers;

		public ChoiceAdventure( final String zone, final String setting, final String name, final Object[] options )
		{
			this( zone, setting, name, options, 0 );
		}

		public ChoiceAdventure( final String zone, final String setting, final String name, final Object[] options, final int ordering )
		{
			this.zone = zone;
			this.setting = setting;
			this.choice = setting.equals( "none" ) ? 0 : StringUtilities.parseInt( setting.substring( 15 ) );
			this.name = name;
			this.options = options;
			this.spoilers = new Object[][] { new String[] { setting }, new String[] { name }, options };
			this.ordering = ordering;
		}

		public int getChoice()
		{
			return this.choice;
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

		public Object[] getOptions()
		{
			return  ( this.options == null ) ?
				ChoiceManager.dynamicChoiceOptions( this.setting ) :
				this.options;
		}

		public Object[][] getSpoilers()
		{
			return this.spoilers;
		}

		public int compareTo( final ChoiceAdventure o )
		{
			// Choices can have a specified relative ordering
			// within zone regardless of name or choice number
			if ( this.ordering != o.ordering )
			{
				return this.ordering - o.ordering;
			}

			if ( ChoiceManager.choicesOrderedByName )
			{
				int result = this.name.compareToIgnoreCase( o.name );

				if ( result != 0 )
				{
					return result;
				}
			}

			return this.choice - o.choice;
		}
	}

	// A ChoiceSpoiler is a ChoiceAdventure that isn't user-configurable.
	// The zone is optional, since it doesn't appear in the choiceadv GUI.
	public static class ChoiceSpoiler
		extends ChoiceAdventure
	{
		public ChoiceSpoiler( final String setting, final String name, final Object[] options )
		{
			super( "Unsorted", setting, name, options );
		}

		public ChoiceSpoiler( final String zone, final String setting, final String name, final Object[] options )
		{
			super( zone, setting, name, options );
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
			new Object[] { new Option( "denim axe", "denim axe" ),
				       new Option( "skip adventure", "rubber axe" ) } ),
		// Denim Axes Examined
		new Object[]{ IntegerPool.get(2), IntegerPool.get(1),
		  new AdventureResult( "rubber axe", -1 ) },

		// The Oracle Will See You Now
		new ChoiceSpoiler(
			"choiceAdventure3", "Teleportitis",
			new Object[] { "skip adventure", "randomly sink 100 meat", "make plus sign usable" } ),

		// Finger-Lickin'... Death.
		new ChoiceAdventure(
			"Beach", "choiceAdventure4", "South of the Border",
			new Object[] { "small meat boost",
				       new Option( "try for poultrygeist", "poultrygeist" ),
				       "skip adventure" } ),
		// Finger-Lickin'... Death.
		new Object[]{ IntegerPool.get(4), IntegerPool.get(1),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },
		new Object[]{ IntegerPool.get(4), IntegerPool.get(2),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Heart of Very, Very Dark Darkness
		new ChoiceAdventure(
			"MusSign", "choiceAdventure5", "Gravy Barrow",
			new Object[] { "fight the fairy queen", "skip adventure" } ),

		// Darker Than Dark
		new ChoiceSpoiler(
			"choiceAdventure6", "Gravy Barrow",
			new Object[] { "get Beaten Up", "skip adventure" } ),

		// Choice 7 is How Depressing

		// On the Verge of a Dirge -> Self Explanatory
		new ChoiceSpoiler(
			"choiceAdventure8", "Gravy Barrow",
			new Object[] { "enter the chamber", "enter the chamber", "enter the chamber" } ),

		// Wheel In the Sky Keep on Turning: Muscle Position
		new ChoiceSpoiler(
			"choiceAdventure9", "Castle Wheel",
			new Object[] { "Turn to mysticality", "Turn to moxie", "Leave at muscle" } ),

		// Wheel In the Sky Keep on Turning: Mysticality Position
		new ChoiceSpoiler(
			"choiceAdventure10", "Castle Wheel",
			new Object[] { "Turn to Map Quest", "Turn to muscle", "Leave at mysticality" } ),

		// Wheel In the Sky Keep on Turning: Map Quest Position
		new ChoiceSpoiler(
			"choiceAdventure11", "Castle Wheel",
			new Object[] { "Turn to moxie", "Turn to mysticality", "Leave at map quest" } ),

		// Wheel In the Sky Keep on Turning: Moxie Position
		new ChoiceSpoiler(
			"choiceAdventure12", "Castle Wheel",
			new Object[] { "Turn to muscle", "Turn to map quest", "Leave at moxie" } ),

		// Choice 13 is unknown

		// A Bard Day's Night
		new ChoiceAdventure(
			"Knob", "choiceAdventure14", "Cobb's Knob Harem",
			new Object[] { new Option( "Knob goblin harem veil", "Knob goblin harem veil" ),
				       new Option( "Knob goblin harem pants", "Knob goblin harem pants" ),
				       "small meat boost", "complete the outfit" } ),

		// Yeti Nother Hippy
		new ChoiceAdventure(
			"McLarge", "choiceAdventure15", "eXtreme Slope",
			new Object[] { new Option( "eXtreme mittens", "eXtreme mittens" ),
				       new Option( "eXtreme scarf", "eXtreme scarf" ),
				       "small meat boost", "complete the outfit" } ),

		// Saint Beernard
		new ChoiceAdventure(
			"McLarge", "choiceAdventure16", "eXtreme Slope",
			new Object[] { new Option( "snowboarder pants", "snowboarder pants" ),
				       new Option( "eXtreme scarf", "eXtreme scarf" ),
				       "small meat boost", "complete the outfit" } ),

		// Generic Teen Comedy
		new ChoiceAdventure(
			"McLarge", "choiceAdventure17", "eXtreme Slope",
			new Object[] { new Option( "eXtreme mittens", "eXtreme mittens" ),
				       new Option( "snowboarder pants", "snowboarder pants" ),
				       "small meat boost", "complete the outfit" } ),

		// A Flat Miner
		new ChoiceAdventure(
			"McLarge", "choiceAdventure18", "Itznotyerzitz Mine",
			new Object[] { new Option( "miner's pants", "miner's pants" ),
				       new Option( "7-Foot Dwarven mattock", "7-Foot Dwarven mattock" ),
				       "small meat boost", "complete the outfit" } ),

		// 100% Legal
		new ChoiceAdventure(
			"McLarge", "choiceAdventure19", "Itznotyerzitz Mine",
			new Object[] { new Option( "miner's helmet", "miner's helmet" ),
				       new Option( "miner's pants", "miner's pants" ),
				       "small meat boost", "complete the outfit" } ),

		// See You Next Fall
		new ChoiceAdventure(
			"McLarge", "choiceAdventure20", "Itznotyerzitz Mine",
			new Object[] { new Option( "miner's helmet", "miner's helmet" ),
				       new Option( "7-Foot Dwarven mattock", "7-Foot Dwarven mattock" ),
				       "small meat boost", "complete the outfit" } ),

		// Under the Knife
		new ChoiceAdventure(
			"Town", "choiceAdventure21", "Sleazy Back Alley",
			new Object[] { "switch genders", "skip adventure" } ),
		// Under the Knife
		new Object[]{ IntegerPool.get(21), IntegerPool.get(1),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },

		// The Arrrbitrator
		new ChoiceAdventure(
			"Island", "choiceAdventure22", "Pirate's Cove",
			new Object[] { new Option( "eyepatch", "eyepatch" ),
				       new Option( "swashbuckling pants", "swashbuckling pants" ),
				       "small meat boost", "complete the outfit" } ),

		// Barrie Me at Sea
		new ChoiceAdventure(
			"Island",
			"choiceAdventure23",
			"Pirate's Cove",
			new Object[] { new Option( "stuffed shoulder parrot", "stuffed shoulder parrot" ),
				       new Option( "swashbuckling pants", "swashbuckling pants" ),
				       "small meat boost", "complete the outfit" } ),

		// Amatearrr Night
		new ChoiceAdventure(
			"Island", "choiceAdventure24", "Pirate's Cove",
			new Object[] { new Option( "stuffed shoulder parrot", "stuffed shoulder parrot" ),
				       "small meat boost",
				       new Option( "eyepatch", "eyepatch" ),
				       "complete the outfit" } ),

		// Ouch! You bump into a door!
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure25", "Dungeon of Doom",
			new Object[] { new Option( "magic lamp", "magic lamp" ),
				       new Option( "dead mimic", "dead mimic" ),
				       "skip adventure" } ),
		// Ouch! You bump into a door!
		new Object[]{ IntegerPool.get(25), IntegerPool.get(1),
		  new AdventureResult( AdventureResult.MEAT, -50 ) },
		new Object[]{ IntegerPool.get(25), IntegerPool.get(2),
		  new AdventureResult( AdventureResult.MEAT, -5000 ) },

		// A Three-Tined Fork
		new ChoiceSpoiler(
			"Woods", "choiceAdventure26", "Spooky Forest",
			new Object[] { "muscle classes", "mysticality classes", "moxie classes" } ),

		// Footprints
		new ChoiceSpoiler(
			"Woods", "choiceAdventure27", "Spooky Forest",
			new Object[] { KoLCharacter.SEAL_CLUBBER, KoLCharacter.TURTLE_TAMER } ),

		// A Pair of Craters
		new ChoiceSpoiler(
			"Woods", "choiceAdventure28", "Spooky Forest",
			new Object[] { KoLCharacter.PASTAMANCER, KoLCharacter.SAUCEROR } ),

		// The Road Less Visible
		new ChoiceSpoiler(
			"Woods", "choiceAdventure29", "Spooky Forest",
			new Object[] { KoLCharacter.DISCO_BANDIT, KoLCharacter.ACCORDION_THIEF } ),

		// Choices 30 - 39 are unknown

		// The Effervescent Fray
		new ChoiceAdventure(
			"Plains", "choiceAdventure40", "Cola Wars",
			new Object[] { new Option( "Cloaca-Cola fatigues", "Cloaca-Cola fatigues" ),
				       new Option( "Dyspepsi-Cola shield", "Dyspepsi-Cola shield" ),
				       "mysticality substats" } ),

		// Smells Like Team Spirit
		new ChoiceAdventure(
			"Plains", "choiceAdventure41", "Cola Wars",
			new Object[] { new Option( "Dyspepsi-Cola fatigues", "Dyspepsi-Cola fatigues" ),
				       new Option( "Cloaca-Cola helmet", "Cloaca-Cola helmet" ),
				       "muscle substats" } ),

		// What is it Good For?
		new ChoiceAdventure(
			"Plains", "choiceAdventure42", "Cola Wars",
			new Object[] { new Option( "Dyspepsi-Cola helmet", "Dyspepsi-Cola helmet" ),
				       new Option( "Cloaca-Cola shield", "Cloaca-Cola shield" ),
				       "moxie substats" } ),

		// Choices 43 - 44 are unknown

		// Maps and Legends
		new ChoiceSpoiler(
			"Woods", "choiceAdventure45", "Spooky Forest",
			new Object[] { new Option( "Spooky Temple map", "Spooky Temple map" ),
				       "skip adventure", "skip adventure" } ),

		// An Interesting Choice
		new ChoiceAdventure(
			"Woods", "choiceAdventure46", "Spooky Forest Vampire",
			new Object[] { "moxie substats", "muscle substats",
				       new Option( "vampire heart", "vampire heart" ) } ),

		// Have a Heart
		new ChoiceAdventure(
			"Woods", "choiceAdventure47", "Spooky Forest Vampire Hunter",
			new Object[] { new Option( "bottle of used blood", "bottle of used blood" ),
				       new Option( "skip adventure and keep vampire hearts", "vampire heart" ) } ),
		// Have a Heart
		// This trades all vampire hearts for an equal number of
		// bottles of used blood.
		new Object[]{ IntegerPool.get(47), IntegerPool.get(1),
		  ItemPool.get( ItemPool.VAMPIRE_HEART, 1 ) },

		// Choices 48 - 70 are violet fog adventures
		// Choice 71 is A Journey to the Center of Your Mind

		// Lording Over The Flies
		new ChoiceAdventure(
			"Island", "choiceAdventure72", "Frat House",
			new Object[] { new Option( "around the world", "around the world" ),
				       new Option( "skip adventure", "Spanish fly" ) } ),
		// Lording Over The Flies
		// This trades all Spanish flies for around the worlds,
		// in multiples of 5.  Excess flies are left in inventory.
		new Object[]{ IntegerPool.get(72), IntegerPool.get(1),
		  ItemPool.get( ItemPool.SPANISH_FLY, 5 ) },

		// Don't Fence Me In
		new ChoiceAdventure(
			"Woods", "choiceAdventure73", "Whitey's Grove",
			new Object[] { "muscle substats",
				       new Option( "white picket fence", "white picket fence" ),
				       new Option( "wedding cake, white rice 3x (+2x w/ rice bowl)", "piece of wedding cake" ) } ),

		// The Only Thing About Him is the Way That He Walks
		new ChoiceAdventure(
			"Woods", "choiceAdventure74", "Whitey's Grove",
			new Object[] { "moxie substats",
				       new Option( "boxed wine", "boxed wine" ),
				       new Option( "mullet wig", "mullet wig" ) } ),

		// Rapido!
		new ChoiceAdventure(
			"Woods", "choiceAdventure75", "Whitey's Grove",
			new Object[] { "mysticality substats",
				       new Option( "white lightning", "white lightning" ),
				       new Option( "white collar", "white collar" ) } ),

		// Junction in the Trunction
		new ChoiceAdventure(
			"Knob", "choiceAdventure76", "Knob Shaft",
			new Object[] { new Option( "cardboard ore", "cardboard ore" ),
				       new Option( "styrofoam ore", "styrofoam ore" ),
				       new Option( "bubblewrap ore", "bubblewrap ore" ) } ),

		// Minnesota Incorporeals
		new ChoiceSpoiler(
			"choiceAdventure77", "Haunted Billiard Room",
			new Object[] { "moxie substats", "other options", "skip adventure" } ),

		// Broken
		new ChoiceSpoiler(
			"choiceAdventure78", "Haunted Billiard Room",
			new Object[] { "other options", "muscle substats", "skip adventure" } ),

		// A Hustle Here, a Hustle There
		new ChoiceSpoiler(
			"choiceAdventure79", "Haunted Billiard Room",
			new Object[] { "Spookyraven library key", "mysticality substats", "skip adventure" } ),


		// History is Fun!
		new ChoiceSpoiler(
			"choiceAdventure86", "Haunted Library",
			new Object[] { "Spookyraven Chapter 1", "Spookyraven Chapter 2", "Spookyraven Chapter 3" } ),

		// History is Fun!
		new ChoiceSpoiler(
			"choiceAdventure87", "Haunted Library",
			new Object[] { "Spookyraven Chapter 4", "Spookyraven Chapter 5 (Gallery Quest)", "Spookyraven Chapter 6" } ),

		// Naughty, Naughty
		new ChoiceSpoiler(
			"choiceAdventure88", "Haunted Library",
			new Object[] { "mysticality substats", "moxie substats", "Fettucini / Scarysauce" } ),

		new ChoiceSpoiler(
			"choiceAdventure89", "Haunted Gallery",
			new Object[] { "Wolf Knight", "Snake Knight", "Dreams and Lights", "skip adventure" } ),

		// Curtains
		new ChoiceAdventure(
			"Manor2", "choiceAdventure90", "Haunted Ballroom",
			new Object[] { "enter combat", "moxie substats", "skip adventure" } ),

		// Having a Medicine Ball
		new ChoiceAdventure(
			"Manor2", "choiceAdventure105", "Haunted Bathroom",
			new Object[] { "mysticality substats", "other options", "guy made of bees" } ),

		// Strung-Up Quartet
		new ChoiceAdventure(
			"Manor2", "choiceAdventure106", "Haunted Ballroom",
			new Object[] { "increase monster level", "decrease combat frequency", "increase item drops", "disable song" } ),

		// Bad Medicine is What You Need
		new ChoiceAdventure(
			"Manor2", "choiceAdventure107", "Haunted Bathroom",
			new Object[] { new Option( "antique bottle of cough syrup", "antique bottle of cough syrup" ),
				       new Option(  "tube of hair oil",	 "tube of hair oil" ),
				       new Option( "bottle of ultravitamins", "bottle of ultravitamins" ),
				       "skip adventure" } ),

		// Aww, Craps
		new ChoiceAdventure(
			"Town", "choiceAdventure108", "Sleazy Back Alley",
			new Object[] { "moxie substats", "meat and moxie", "random effect", "skip adventure" } ),

		// Dumpster Diving
		new ChoiceAdventure(
			"Town", "choiceAdventure109", "Sleazy Back Alley",
			new Object[] { "enter combat", "meat and moxie",
				       new Option( "Mad Train wine", "Mad Train wine" ) } ),

		// The Entertainer
		new ChoiceAdventure(
			"Town", "choiceAdventure110", "Sleazy Back Alley",
			new Object[] { "moxie substats", "moxie and muscle", "small meat boost", "skip adventure" } ),

		// Malice in Chains
		new ChoiceAdventure(
			"Knob", "choiceAdventure111", "Outskirts of The Knob",
			new Object[] { "muscle substats", "muscle substats", "enter combat" } ),

		// Please, Hammer
		new ChoiceAdventure(
			"Town", "choiceAdventure112", "Sleazy Back Alley",
			new Object[] { "accept hammer quest", "reject quest", "muscle substats" } ),

		// Knob Goblin BBQ
		new ChoiceAdventure(
			"Knob", "choiceAdventure113", "Outskirts of The Knob",
			new Object[] { "complete cake quest", "enter combat", "get a random item" } ),

		// The Baker's Dilemma
		new ChoiceAdventure(
			"Manor1", "choiceAdventure114", "Haunted Pantry",
			new Object[] { "accept cake quest", "reject quest", "moxie and meat" } ),

		// Oh No, Hobo
		new ChoiceAdventure(
			"Manor1", "choiceAdventure115", "Haunted Pantry",
			new Object[] { "enter combat", "Good Karma", "mysticality, moxie, and meat" } ),

		// The Singing Tree
		new ChoiceAdventure(
			"Manor1", "choiceAdventure116", "Haunted Pantry",
			new Object[] { "mysticality substats", "moxie substats", "random effect", "skip adventure" } ),

		// Tresspasser
		new ChoiceAdventure(
			"Manor1", "choiceAdventure117", "Haunted Pantry",
			new Object[] { "enter combat", "mysticality substats", "get a random item" } ),

		// When Rocks Attack
		new ChoiceAdventure(
			"Knob", "choiceAdventure118", "Outskirts of The Knob",
			new Object[] { "accept unguent quest", "skip adventure" } ),

		// Choice 119 is Check It Out Now

		// Ennui is Wasted on the Young
		new ChoiceAdventure(
			"Knob", "choiceAdventure120", "Outskirts of The Knob",
			new Object[] { "muscle and Pumped Up",
				       new Option( "ice cold Sir Schlitz", "ice cold Sir Schlitz" ),
				       new Option( "moxie and lemon", "lemon" ),
				       "skip adventure" } ),

		// Choice 121 is Next Sunday, A.D.
		// Choice 122 is unknown

		// At Least It's Not Full Of Trash
		new ChoiceSpoiler(
			"choiceAdventure123", "Hidden Temple",
			new Object[] { "lose HP", "Unlock Quest Puzzle", "lose HP" } ),

		// Choice 124 is unknown

		// No Visible Means of Support
		new ChoiceSpoiler(
			"choiceAdventure125", "Hidden Temple",
			new Object[] { "lose HP", "lose HP", "Unlock Hidden City" } ),

		// Sun at Noon, Tan Us
		new ChoiceAdventure(
			"Plains", "choiceAdventure126", "Palindome",
			new Object[] { "moxie", "chance of more moxie", "sunburned" } ),

		// No sir, away!  A papaya war is on!
		new ChoiceSpoiler(
			"Plains", "choiceAdventure127", "Palindome",
			new Object[] { new Option( "3 papayas", "papaya" ),
				       "trade 3 papayas for stats", "stats" } ),
		// No sir, away!  A papaya war is on!
		new Object[]{ IntegerPool.get(127), IntegerPool.get(2),
		  ItemPool.get( ItemPool.PAPAYA, -3 ) },

		// Choice 128 is unknown

		// Do Geese See God?
		new ChoiceSpoiler(
			"Plains", "choiceAdventure129", "Palindome",
			new Object[] { new Option( "photograph of God", "photograph of God" ),
				       "skip adventure" } ),
		// Do Geese See God?
		new Object[]{ IntegerPool.get(129), IntegerPool.get(1),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Let's Make a Deal!
		new ChoiceAdventure(
			"Beach", "choiceAdventure132", "Desert (Pre-Oasis)",
			new Object[] { new Option( "broken carburetor", "broken carburetor" ),
				       "Unlock Oasis" } ),
		// Let's Make a Deal!
		new Object[]{ IntegerPool.get(132), IntegerPool.get(1),
		  new AdventureResult( AdventureResult.MEAT, -5000 ) },

		// Choice 133 is unknown

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure(
			"Pyramid", "choiceAdventure134", "The Middle Chamber",
			new Object[] { "Turn the wheel", "skip adventure" } ),

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure(
			"Pyramid", "choiceAdventure135", "The Middle Chamber",
			new Object[] { "Turn the wheel", "skip adventure" } ),

		// Peace Wants Love
		new ChoiceAdventure(
			"Island", "choiceAdventure136", "Hippy Camp",
			new Object[] { new Option( "filthy corduroys", "filthy corduroys" ),
				       new Option( "filthy knitted dread sack", "filthy knitted dread sack" ),
				       "small meat boost", "complete the outfit" } ),

		// An Inconvenient Truth
		new ChoiceAdventure(
			"Island", "choiceAdventure137", "Hippy Camp",
			new Object[] { new Option( "filthy knitted dread sack", "filthy knitted dread sack" ),
				       new Option( "filthy corduroys", "filthy corduroys" ),
				       "small meat boost", "complete the outfit" } ),

		// Purple Hazers
		new ChoiceAdventure(
			"Island", "choiceAdventure138", "Frat House",
			new Object[] { new Option( "Orcish cargo shorts", "Orcish cargo shorts" ),
				       new Option( "Orcish baseball cap", "Orcish baseball cap" ),
				       new Option( "homoerotic frat-paddle", "homoerotic frat-paddle" ),
				       "complete the outfit" } ),

		// Bait and Switch
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure139", "War Hippies",
			new Object[] { "muscle substats",
				       new Option( "ferret bait", "ferret bait" ),
				       "enter combat" } ),

		// The Thin Tie-Dyed Line
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure140", "War Hippies",
			new Object[] { new Option( "water pipe bombs", "water pipe bomb" ),
				       "moxie substats", "enter combat" } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure141", "War Hippies",
			new Object[] { "mysticality substats", "get some hippy food", "waste a turn" } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure142", "War Hippies",
			new Object[] { "mysticality substats", "get some hippy food", "start the war" } ),

		// Catching Some Zetas
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure143", "War Fraternity",
			new Object[] { "muscle substats",
				       new Option( "sake bombs", "sake bomb" ),
				       "enter combat" } ),

		// One Less Room Than In That Movie
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure144", "War Fraternity",
			new Object[] { "moxie substats",
				       new Option( "beer bombs", "beer bomb" ),
				       "enter combat" } ),

		// Fratacombs
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure145", "War Fraternity",
			new Object[] { "muscle substats", "get some frat food", "waste a turn" } ),

		// Fratacombs
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure146", "War Fraternity",
			new Object[] { "muscle substats", "get some frat food", "start the war" } ),

		// Cornered!
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure147", "Isle War Barn",
			new Object[] { "Open The Granary (meat)", "Open The Bog (stench)", "Open The Pond (cold)" } ),

		// Cornered Again!
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure148", "Isle War Barn",
			new Object[] { "Open The Back 40 (hot)", "Open The Family Plot (spooky)" } ),

		// How Many Corners Does this Stupid Barn Have!?
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure149", "Isle War Barn",
			new Object[] { "Open The Shady Thicket (booze)", "Open The Other Back 40 (sleaze)" } ),

		// Choice 150 is Another Adventure About BorderTown

		// Adventurer, $1.99
		new ChoiceAdventure(
			"Plains", "choiceAdventure151", "Fun House",
			new Object[] { "fight the clownlord", "skip adventure" } ),

		// Lurking at the Threshold
		new ChoiceSpoiler(
			"Plains", "choiceAdventure152", "Fun House",
			new Object[] { "fight the clownlord", "skip adventure" } ),

		// Turn Your Head and Coffin
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure153", "Defiled Alcove",
			new Object[] { "muscle substats", "small meat boost",
				       new Option( "half-rotten brain", "half-rotten brain" ),
				       "skip adventure" } ),

		// Choice 154 used to be Doublewide

		// Skull, Skull, Skull
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure155", "Defiled Nook",
			new Object[] { "moxie substats", "small meat boost",
				       new Option( "rusty bonesaw", "rusty bonesaw" ),
				       new Option( "debonair deboner", "debonair deboner" ),
				       "skip adventure" } ),

		// Choice 156 used to be Pileup

		// Urning Your Keep
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure157", "Defiled Niche",
			new Object[] { "mysticality substats",
				       new Option( "plus-sized phylactery", "plus-sized phylactery" ),
				       "small meat boost", "skip adventure" } ),

		// Choice 158 used to be Lich in the Niche
		// Choice 159 used to be Go Slow Past the Drawers
		// Choice 160 used to be Lunchtime

		// Choice 161 is Bureaucracy of the Damned

		// Between a Rock and Some Other Rocks
		new ChoiceSpoiler(
			"choiceAdventure162", "Goatlet",
			new Object[] { "Open Goatlet", "skip adventure" } ),

		// Melvil Dewey Would Be Ashamed
		new ChoiceAdventure(
			"Manor1", "choiceAdventure163", "Haunted Library",
			new Object[] { new Option( "Necrotelicomnicon", "Necrotelicomnicon" ),
				       new Option( "Cookbook of the Damned", "Cookbook of the Damned" ),
				       new Option( "Sinful Desires", "Sinful Desires" ),
				       "skip adventure" } ),

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
			new Object[] { "muscle substats", "MP & Spirit of Alph", "enter combat" } ),

		// Beyond Any Measure
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure165", "Pleasure Dome",
			new Object[] { "Rat-Faced -> Night Vision", "Bats in the Belfry -> Good with the Ladies", "mysticality	     substats", "skip adventure" } ),

		// Death is a Boat
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure166", "Pleasure Dome",
			new Object[] { new Option( "No Vertigo -> S.T.L.T.", "S.T.L.T." ),
				       "moxie substats",
				       new Option( "Unusual Fashion Sense -> albatross necklace", "albatross necklace" ) } ),

		// It's a Fixer-Upper
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure167", "Moulder Mansion",
			new Object[] { "enter combat", "mysticality substats", "HP & MP & Bats in the Belfry" } ),

		// Midst the Pallor of the Parlor
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure168", "Moulder Mansion",
			new Object[] { "moxie substats", "Spirit of Alph -> Feelin' Philosophical", "Rat-Faced -> Unusual Fashion Sense" } ),

		// A Few Chintz Curtains, Some Throw Pillows, It
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure169", "Moulder Mansion",
			new Object[] { new Option( "Night Vision -> flask of Amontillado", "flask of Amontillado" ),
				       "muscle substats",
				       new Option( "Dancing Prowess -> fancy ball mask", "fancy ball mask" ) } ),

		// La Vie Boheme
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure170", "Rogue Windmill",
			new Object[] { "HP & Rat-Faced", "enter combat", "moxie substats" } ),

		// Backstage at the Rogue Windmill
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure171", "Rogue Windmill",
			new Object[] { "Bats in the Belfry -> No Vertigo", "muscle substats", "Spirit of Alph -> Dancing Prowess" } ),

		// Up in the Hippo Room
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure172", "Rogue Windmill",
			new Object[] { new Option( "Good with the Ladies -> Can-Can skirt", "Can-Can skirt" ),
				       new Option( "Feelin' Philosophical -> not-a-pipe", "not-a-pipe" ),
				       "mysticality substats" } ),

		// Choice 173 is The Last Stand, Man
		// Choice 174 is The Last Stand, Bra
		// Choice 175-176 are unknown

		// Choice 177 was The Blackberry Cobbler

		// Hammering the Armory
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure178", "Fantasy Airship Shirt",
			new Object[] { new Option( "bronze breastplate", "bronze breastplate" ),
				       "skip adventure" } ),

		// Choice 179 is unknown

		// A Pre-War Dresser Drawer, Pa!
		new ChoiceAdventure(
			"Plains", "choiceAdventure180", "Palindome Shirt",
			new Object[] { new Option( "Ye Olde Navy Fleece","Ye Olde Navy Fleece" ),
				       "skip adventure" } ),

		// Chieftain of the Flies
		new ChoiceAdventure(
			"Island", "choiceAdventure181", "Frat House (Stone Age)",
			new Object[] { new Option( "around the world", "around the world" ),
				       new Option( "skip adventure", "Spanish fly" ) } ),
		// Chieftain of the Flies
		// This trades all Spanish flies for around the worlds,
		// in multiples of 5.  Excess flies are left in inventory.
		new Object[]{ IntegerPool.get(181), IntegerPool.get(1),
		  ItemPool.get( ItemPool.SPANISH_FLY, 5 ) },

		// Random Lack of an Encounter
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure182", "Fantasy Airship",
			new Object[] { "enter combat",
				       new Option( "Penultimate Fantasy chest", "Penultimate Fantasy chest" ),
				       "stats",
				       new Option( "model airship and combat", "model airship" ),
				       new Option( "model airship and chest", "model airship" ),
				       new Option( "model airship and stats", "model airship" ) } ),

		// That Explains All The Eyepatches
		// Dynamically calculate options based on mainstat
		new ChoiceAdventure(
			"Island", "choiceAdventure184", "Barrrney's Barrr",
			null ),

		// Yes, You're a Rock Starrr
		new ChoiceAdventure(
			"Island", "choiceAdventure185", "Barrrney's Barrr",
			null ),

		// A Test of Testarrrsterone
		new ChoiceAdventure(
			"Island", "choiceAdventure186", "Barrrney's Barrr",
			new Object[] { "stats", "drunkenness and stats", "moxie" } ),

		// Choice 187 is Arrr You Man Enough?

		// The Infiltrationist
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure188", "Frathouse Blueprints",
			new Object[] { "frat boy ensemble", "mullet wig and briefcase", "frilly skirt and hot wings" } ),

		//  O Cap'm, My Cap'm
		new Object[]{ IntegerPool.get(189), IntegerPool.get(1),
		  new AdventureResult( AdventureResult.MEAT, -977 ) },

		// Choice 190 is unknown

		// Chatterboxing
		new ChoiceAdventure(
			"Island", "choiceAdventure191", "F'c'le",
			new Object[] { "moxie substats",
				       "use valuable trinket to banish, or lose hp",
				       "muscle substats",
				       "mysticality substats",
				       "use valuable trinket to banish, or moxie",
				       "use valuable trinket to banish, or muscle",
				       "use valuable trinket to banish, or mysticality",
				       "use valuable trinket to banish, or mainstat" } ),
		new Object[]{ IntegerPool.get(191), IntegerPool.get(2),
		  ItemPool.get( ItemPool.VALUABLE_TRINKET, -1 ) },

		// Choice 192 is unknown
		// Choice 193 is Modular, Dude

		// Somewhat Higher and Mostly Dry
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure197", "A Maze of Sewer Tunnels",
			new Object[] { "take the tunnel", "sewer gator", "turn the valve" } ),

		// Disgustin' Junction
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure198", "A Maze of Sewer Tunnels",
			new Object[] { "take the tunnel", "giant zombie goldfish", "open the grate" } ),

		// The Former or the Ladder
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure199", "A Maze of Sewer Tunnels",
			new Object[] { "take the tunnel", "C. H. U. M.", "head down the ladder" } ),

		// Enter The Hoboverlord
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure200", "Hobopolis Town Square",
			new Object[] { "enter combat with Hodgman", "skip adventure" } ),

		// Home, Home in the Range
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure201", "Burnbarrel Blvd.",
			new Object[] { "enter combat with Ol' Scratch", "skip adventure" } ),

		// Bumpity Bump Bump
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure202", "Exposure Esplanade",
			new Object[] { "enter combat with Frosty", "skip adventure" } ),

		// Deep Enough to Dive
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure203", "The Heap",
			new Object[] { "enter combat with Oscus", "skip adventure" } ),

		// Welcome To You!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure204", "The Ancient Hobo Burial Ground",
			new Object[] { "enter combat with Zombo", "skip adventure" } ),

		// Van, Damn
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure205", "The Purple Light District",
			new Object[] { "enter combat with Chester", "skip adventure" } ),

		// Getting Tired
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure206", "Burnbarrel Blvd.",
			new Object[] { "start tirevalanche", "add tire to stack", "skip adventure" } ),

		// Hot Dog! I Mean... Door!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure207", "Burnbarrel Blvd.",
			new Object[] { "increase hot hobos & get clan meat", "skip adventure" } ),

		// Ah, So That's Where They've All Gone
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure208", "The Ancient Hobo Burial Ground",
			new Object[] { "increase spooky hobos & decrease stench", "skip adventure" } ),

		// Choice 209 is Timbarrrr!
		// Choice 210 is Stumped

		// Despite All Your Rage
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure211", "A Maze of Sewer Tunnels",
			new Object[] { "gnaw through the bars" } ),

		// Choice 212 is also Despite All Your Rage, apparently after you've already
		// tried to wait for rescue?
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure212", "A Maze of Sewer Tunnels",
			new Object[] { "gnaw through the bars" } ),

		// Piping Hot
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure213", "Burnbarrel Blvd.",
			new Object[] { "increase sleaze hobos & decrease heat", "skip adventure" } ),

		// You vs. The Volcano
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure214", "The Heap",
			new Object[] { "decrease stench hobos & increase stench", "skip adventure" } ),

		// Piping Cold
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure215", "Exposure Esplanade",
			new Object[] { "decrease heat", "decrease sleaze hobos", "increase number of icicles" } ),

		// The Compostal Service
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure216", "The Heap",
			new Object[] { "decrease stench & spooky", "skip adventure" } ),

		// There Goes Fritz!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure217", "Exposure Esplanade",
			new Object[] { "yodel a little", "yodel a lot", "yodel your heart out" } ),

		// I Refuse!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure218", "The Heap",
			new Object[] { "explore the junkpile", "skip adventure" } ),

		// The Furtivity of My City
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure219", "The Purple Light District",
			new Object[] { "fight sleaze hobo", "increase stench", "increase sleaze hobos & get clan meat" } ),

		// Returning to the Tomb
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure220", "The Ancient Hobo Burial Ground",
			new Object[] { "increase spooky hobos & get clan meat", "skip adventure" } ),

		// A Chiller Night
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure221", "The Ancient Hobo Burial Ground",
			new Object[] { "study the dance moves", "dance with hobo zombies", "skip adventure" } ),

		// A Chiller Night (2)
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure222", "The Ancient Hobo Burial Ground",
			new Object[] { "dance with hobo zombies", "skip adventure" } ),

		// Getting Clubbed
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure223", "The Purple Light District",
			new Object[] { "try to get inside", "try to bamboozle the crowd", "try to flimflam the crowd" } ),

		// Exclusive!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure224", "The Purple Light District",
			new Object[] { "fight sleaze hobo", "start barfight", "gain stats" } ),

		// Attention -- A Tent!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure225", "Hobopolis Town Square",
			new Object[] { "perform on stage", "join the crowd", "skip adventure" } ),

		// Choice 226 is Here You Are, Up On Stage (use the same system as 211 & 212)
		// Choice 227 is Working the Crowd (use the same system as 211 & 212)

		// Choices 228 & 229 are unknown

		// Mind Yer Binder
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure230", "Hobopolis Town Square",
			new Object[] { new Option( "hobo code binder", "hobo code binder" ),
				       "skip adventure" } ),
		// Mind Yer Binder
		new Object[]{ IntegerPool.get(230), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -30 ) },

		// Choices 231-271 are subchoices of Choice 272

		// Food, Glorious Food
		new ChoiceSpoiler(
			"choiceAdventure235", "Hobopolis Marketplace",
			new Object[] { "muscle food", "mysticality food", "moxie food" } ),

		// Booze, Glorious Booze
		new ChoiceSpoiler(
			"choiceAdventure240", "Hobopolis Marketplace",
			new Object[] { "muscle booze", "mysticality booze", "moxie booze" } ),

		// The Guy Who Carves Driftwood Animals
		new Object[]{ IntegerPool.get(247), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },

		// A Hattery
		new ChoiceSpoiler(
			"choiceAdventure250", "Hobopolis Marketplace",
			new Object[] { new Option( "crumpled felt fedora", "crumpled felt fedora" ),
				       new Option( "battered old top-hat", "battered old top-hat" ),
				       new Option( "shapeless wide-brimmed hat", "shapeless wide-brimmed hat" ) } ),
		// A Hattery
		new Object[]{ IntegerPool.get(250), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -250 ) },
		new Object[]{ IntegerPool.get(250), IntegerPool.get(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -150 ) },
		new Object[]{ IntegerPool.get(250), IntegerPool.get(3),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -200 ) },

		// A Pantry
		new ChoiceSpoiler(
			"choiceAdventure251", "Hobopolis Marketplace",
			new Object[] { new Option( "mostly rat-hide leggings", "mostly rat-hide leggings" ),
				       new Option( "hobo dungarees", "hobo dungarees" ),
				       new Option( "old patched suit-pants", "old patched suit-pants" ) } ),
		// A Pantry
		new Object[]{ IntegerPool.get(251), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -200 ) },
		new Object[]{ IntegerPool.get(251), IntegerPool.get(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -150 ) },
		new Object[]{ IntegerPool.get(251), IntegerPool.get(3),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -250 ) },

		// Hobo Blanket Bingo
		new ChoiceSpoiler(
			"choiceAdventure252", "Hobopolis Marketplace",
			new Object[] { new Option( "old soft shoes", "old soft shoes" ),
				       new Option( "hobo stogie", "hobo stogie" ),
				       new Option( "rope with some soap on it", "rope with some soap on it" ) } ),
		// Hobo Blanket Bingo
		new Object[]{ IntegerPool.get(252), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -250 ) },
		new Object[]{ IntegerPool.get(252), IntegerPool.get(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -200 ) },
		new Object[]{ IntegerPool.get(252), IntegerPool.get(3),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -150 ) },

		// Black-and-Blue-and-Decker
		new ChoiceSpoiler(
			"choiceAdventure255", "Hobopolis Marketplace",
			new Object[] { new Option( "sharpened hubcap", "sharpened hubcap" ),
				       new Option( "very large caltrop", "very large caltrop" ),
				       new Option( "The Six-Pack of Pain", "The Six-Pack of Pain" ) } ),
		// Black-and-Blue-and-Decker
		new Object[]{ IntegerPool.get(255), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },
		new Object[]{ IntegerPool.get(255), IntegerPool.get(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },
		new Object[]{ IntegerPool.get(255), IntegerPool.get(3),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },

		// Instru-mental
		new Object[]{ IntegerPool.get(258), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -99 ) },

		// We'll Make Great...
		new ChoiceSpoiler(
			"choiceAdventure259", "Hobopolis Marketplace",
			new Object[] { "hobo monkey", "stats", "enter combat" } ),

		// Everybody's Got Something To Hide
		new Object[]{ IntegerPool.get(261), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -1000 ) },

		// Tanning Salon
		new ChoiceSpoiler(
			"choiceAdventure264", "Hobopolis Marketplace",
			new Object[] { "20 adv of +50% moxie", "20 adv of +50% mysticality" } ),
		// Tanning Salon
		new Object[]{ IntegerPool.get(264), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },
		new Object[]{ IntegerPool.get(264), IntegerPool.get(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Let's All Go To The Movies
		new ChoiceSpoiler(
			"choiceAdventure267", "Hobopolis Marketplace",
			new Object[] { "20 adv of +5 spooky resistance", "20 adv of +5 sleaze resistance" } ),
		// Let's All Go To The Movies
		new Object[]{ IntegerPool.get(267), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },
		new Object[]{ IntegerPool.get(267), IntegerPool.get(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// It's Fun To Stay There
		new ChoiceSpoiler(
			"choiceAdventure268", "Hobopolis Marketplace",
			new Object[] { "20 adv of +5 stench resistance", "20 adv of +50% muscle" } ),
		// It's Fun To Stay There
		new Object[]{ IntegerPool.get(268), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },
		new Object[]{ IntegerPool.get(268), IntegerPool.get(2),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Marketplace Entrance
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure272", "Hobopolis Town Square",
			new Object[] { "enter marketplace", "skip adventure" } ),

		// Piping Cold
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure273", "Exposure Esplanade",
			new Object[] { new Option( "frozen banquet", "frozen banquet" ),
				       "increase cold hobos & get clan meat", "skip adventure" } ),

		// Choice 274 is Tattoo Redux, a subchoice of Choice 272 when
		// you've started a tattoo

		// Choice 275 is Triangle, Man, a subchoice of Choice 272 when
		// you've already purchased your class instrument
		// Triangle, Man
		new Object[]{ IntegerPool.get(275), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -10 ) },

		// Choices 278-290 are llama lama gong related choices

		// The Gong Has Been Bung
		new ChoiceSpoiler(
			"choiceAdventure276", "Gong",
			new Object[] { "3 adventures", "12 adventures", "15 adventures" } ),

		// Welcome Back!
		new ChoiceSpoiler(
			"choiceAdventure277", "Gong",
			new Object[] { "finish journey", "also finish journey" } ),

		// Enter the Roach
		new ChoiceSpoiler(
			"choiceAdventure278", "Gong",
			new Object[] { "muscle substats", "mysticality substats", "moxie substats" } ),

		// It's Nukyuhlur - the 'S' is Silent.
		new ChoiceSpoiler(
			"choiceAdventure279", "Gong",
			new Object[] { "moxie substats", "muscle substats", "gain MP" } ),

		// Eek! Eek!
		new ChoiceSpoiler(
			"choiceAdventure280", "Gong",
			new Object[] { "mysticality substats", "muscle substats", "gain MP" } ),

		// A Meta-Metamorphosis
		new ChoiceSpoiler(
			"choiceAdventure281", "Gong",
			new Object[] { "moxie substats", "mysticality substats", "gain MP" } ),

		// You've Got Wings, But No Wingman
		new ChoiceSpoiler(
			"choiceAdventure282", "Gong",
			new Object[] { "+30% muscle", "+10% all stats", "+30 ML" } ),

		// Time Enough at Last!
		new ChoiceSpoiler(
			"choiceAdventure283", "Gong",
			new Object[] { "+30% muscle", "+10% all stats", "+50% item drops" } ),

		// Scavenger Is Your Middle Name
		new ChoiceSpoiler(
			"choiceAdventure284", "Gong",
			new Object[] { "+30% muscle", "+50% item drops", "+30 ML" } ),

		// Bugging Out
		new ChoiceSpoiler(
			"choiceAdventure285", "Gong",
			new Object[] { "+30% mysticality", "+30 ML", "+10% all stats" } ),

		// A Sweeping Generalization
		new ChoiceSpoiler(
			"choiceAdventure286", "Gong",
			new Object[] { "+50% item drops", "+10% all stats", "+30% mysticality" } ),

		// In the Frigid Aire
		new ChoiceSpoiler(
			"choiceAdventure287", "Gong",
			new Object[] { "+30 ML", "+30% mysticality", "+50% item drops" } ),

		// Our House
		new ChoiceSpoiler(
			"choiceAdventure288", "Gong",
			new Object[] { "+30 ML", "+30% moxie", "+10% all stats" } ),

		// Workin' For The Man
		new ChoiceSpoiler(
			"choiceAdventure289", "Gong",
			new Object[] { "+30 ML", "+30% moxie", "+50% item drops" } ),

		// The World's Not Fair
		new ChoiceSpoiler(
			"choiceAdventure290", "Gong",
			new Object[] { "+30% moxie", "+10% all stats", "+50% item drops" } ),

		// A Tight Squeeze
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure291", "Burnbarrel Blvd.",
			new Object[] { new Option( "jar of squeeze", "jar of squeeze" ),
				       "skip adventure" } ),
		// A Tight Squeeze - jar of squeeze
		new Object[]{ IntegerPool.get(291), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Cold Comfort
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure292", "Exposure Esplanade",
			new Object[] { new Option( "bowl of fishysoisse", "bowl of fishysoisse" ),
				       "skip adventure" } ),
		// Cold Comfort - bowl of fishysoisse
		new Object[]{ IntegerPool.get(292), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Flowers for You
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure293", "The Ancient Hobo Burial Ground",
			new Object[] { new Option( "deadly lampshade", "deadly lampshade" ),
				       "skip adventure" } ),
		// Flowers for You - deadly lampshade
		new Object[]{ IntegerPool.get(293), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Maybe It's a Sexy Snake!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure294", "The Purple Light District",
			new Object[] { new Option( "lewd playing card", "lewd playing card" ),
				       "skip adventure" } ),
		// Maybe It's a Sexy Snake! - lewd playing card
		new Object[]{ IntegerPool.get(294), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Juicy!
		new ChoiceAdventure(
			"Hobopolis", "choiceAdventure295", "The Heap",
			new Object[] { new Option( "concentrated garbage juice", "concentrated garbage juice" ),
				       "skip adventure" } ),
		// Juicy! - concentrated garbage juice
		new Object[]{ IntegerPool.get(295), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -5 ) },

		// Choice 296 is Pop!

		// Gravy Fairy Ring
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure297", "Haiku Dungeon",
			new Object[] { "mushrooms",
				       new Option( "fairy gravy boat", "fairy gravy boat" ),
				       "skip adventure" } ),

		// In the Shade
		new ChoiceAdventure(
			"The Sea", "choiceAdventure298", "An Octopus's Garden",
			new Object[] { "plant seeds", "skip adventure" } ),

		// Down at the Hatch
		new ChoiceAdventure(
			"The Sea", "choiceAdventure299", "The Wreck of the Edgar Fitzsimmons",
			new Object[] { "release creatures", "skip adventure" } ),

		// Choice 300 is Merry Crimbo!
		// Choice 301 is And to All a Good Night
		// Choice 302 is You've Hit Bottom (Sauceror)
		// Choice 303 is You've Hit Bottom (Pastamancer)

		// A Vent Horizon
		new ChoiceAdventure(
			"The Sea", "choiceAdventure304", "The Marinara Trench",
			new Object[] { new Option( "bubbling tempura batter", "bubbling tempura batter" ),
				       "skip adventure" } ),
		// A Vent Horizon
		new Object[]{ IntegerPool.get(304), IntegerPool.get(1),
		  new AdventureResult( AdventureResult.MP, -200 ) },

		// There is Sauce at the Bottom of the Ocean
		new ChoiceAdventure(
			"The Sea", "choiceAdventure305", "The Marinara Trench",
			new Object[] { new Option( "globe of Deep Sauce", "globe of Deep Sauce" ),
				       "skip adventure" } ),
		// There is Sauce at the Bottom of the Ocean
		new Object[]{ IntegerPool.get(305), IntegerPool.get(1),
		  ItemPool.get( ItemPool.MERKIN_PRESSUREGLOBE, -1 ) },

		// Choice 306 is [Grandpa Mine Choice]
		// Choice 307 is Ode to the Sea
		// Choice 308 is Boxing the Juke

		// Barback
		new ChoiceAdventure(
			"The Sea", "choiceAdventure309", "The Dive Bar",
			new Object[] { new Option( "seaode", "seaode" ),
				       "skip adventure" } ),

		// The Economist of Scales
		new ChoiceAdventure(
			"The Sea", "choiceAdventure310", "Madness Reef",
			new Object[] { new Option( "get 1 rough fish scale", 1, "rough fish scale" ),
				       new Option( "get 1 pristine fish scale", 2, "pristine fish scale" ),
				       new Option( "get multiple rough fish scales", 4, "rough fish scale" ),
				       new Option( "get multiple pristine fish scales", 5, "pristine fish scale" ),
				       new Option( "skip adventure", 6 )  } ),
		// The Economist of Scales
		// This trades 10 dull fish scales in.
		new Object[]{ IntegerPool.get(310), IntegerPool.get(1),
		  ItemPool.get( ItemPool.DULL_FISH_SCALE, -10 ) },
		new Object[]{ IntegerPool.get(310), IntegerPool.get(2),
		  ItemPool.get( ItemPool.ROUGH_FISH_SCALE, -10 ) },
		new Object[]{ IntegerPool.get(310), IntegerPool.get(4),
		  ItemPool.get( ItemPool.DULL_FISH_SCALE, 10 ) },
		new Object[]{ IntegerPool.get(310), IntegerPool.get(5),
		  ItemPool.get( ItemPool.ROUGH_FISH_SCALE, 10 ) },

		// Heavily Invested in Pun Futures
		new ChoiceAdventure(
			"The Sea", "choiceAdventure311", "Madness Reef",
			new Object[] { "The Economist of Scales", "skip adventure" } ),

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
			new Object[] { "enter combat with Mother Slime", "skip adventure" } ),

		// Choice 327 is Puttin' it on Wax
		// Choice 328 is Never Break the Chain
		// Choice 329 is Don't Be Alarmed, Now

		// A Shark's Chum
		new ChoiceAdventure(
			"Manor1", "choiceAdventure330", "Haunted Billiards Room",
			new Object[] { "stats and pool skill",
				       new Option( "cube of billiard chalk", "cube of billiard chalk" ) } ),

		// Choice 331 is Like That Time in Tortuga
		// Choice 332 is More eXtreme Than Usual
		// Choice 333 is Cleansing your Palette
		// Choice 334 is O Turtle Were Art Thou
		// Choice 335 is Blue Monday
		// Choice 336 is Jewel in the Rough

		// Engulfed!
		new ChoiceAdventure(
			"Clan Basement", "choiceAdventure337", "The Slime Tube",
			new Object[] { "+1 rusty -> slime-covered item conversion", "raise area ML", "skip adventure" } ),

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

		// The Primordial Directive
		new ChoiceAdventure(
			"Memories", "choiceAdventure349", "The Primordial Soup",
			new Object[] { "swim upwards", "swim in circles", "swim downwards" }),

		// Soupercharged
		new ChoiceAdventure(
			"Memories", "choiceAdventure350", "The Primordial Soup",
			new Object[] { "Fight Cyrus", "skip adventure" }),

		// Choice 351 is Beginner's Luck

		// Savior Faire
		new ChoiceAdventure(
			"Memories", "choiceAdventure352", "Seaside Megalopolis",
			new Object[] { "Moxie -> Bad Reception Down Here", "Muscle -> A Diseased Procurer", "Mysticality -> Give it a Shot" }),

		// Bad Reception Down Here
		new ChoiceAdventure(
			"Memories", "choiceAdventure353", "Seaside Megalopolis",
			new Object[] { new Option( "Indigo Party Invitation", "Indigo Party Invitation" ),
				       new Option( "Violet Hunt Invitation", "Violet Hunt Invitation" ) } ),

		// You Can Never Be Too Rich or Too in the Future
		new ChoiceAdventure(
			"Memories", "choiceAdventure354", "Seaside Megalopolis",
			new Object[] { "Moxie", "Serenity" } ),

		// I'm on the Hunt, I'm After You
		new ChoiceAdventure(
			"Memories", "choiceAdventure355", "Seaside Megalopolis",
			new Object[] { "Stats", "Phairly Pheromonal" } ),

		// A Diseased Procurer
		new ChoiceAdventure(
			"Memories", "choiceAdventure356", "Seaside Megalopolis",
			new Object[] { new Option( "Blue Milk Club Card", "Blue Milk Club Card" ),
				       new Option( "Mecha Mayhem Club Card", "Mecha Mayhem Club Card" ) } ),

		// Painful, Circuitous Logic
		new ChoiceAdventure(
			"Memories", "choiceAdventure357", "Seaside Megalopolis",
			new Object[] { "Muscle", "Nano-juiced" } ),

		// Brings All the Boys to the Blue Yard
		new ChoiceAdventure(
			"Memories", "choiceAdventure358", "Seaside Megalopolis",
			new Object[] { "Stats", "Dance Interpreter" } ),

		// Choice 359 is unknown

		// Choice 360 is Cavern Entrance

		// Give it a Shot
		new ChoiceAdventure(
			"Memories", "choiceAdventure361", "Seaside Megalopolis",
			new Object[] { new Option( "'Smuggler Shot First' Button", "'Smuggler Shot First' Button" ),
				       new Option( "Spacefleet Communicator Badge", "Spacefleet Communicator Badge" ) } ),

		// A Bridge Too Far
		new ChoiceAdventure(
			"Memories", "choiceAdventure362", "Seaside Megalopolis",
			new Object[] { "Stats", "Meatwise" } ),

		// Does This Bug You? Does This Bug You?
		new ChoiceAdventure(
			"Memories", "choiceAdventure363", "Seaside Megalopolis",
			new Object[] { "Mysticality", "In the Saucestream" } ),

		// 451 Degrees! Burning Down the House!
		new ChoiceAdventure(
			"Memories", "choiceAdventure364", "Seaside Megalopolis",
			new Object[] { "Moxie",
				       new Option( "Supreme Being Glossary", "Supreme Being Glossary" ),
				       "Muscle" } ),

		// None Shall Pass
		new ChoiceAdventure(
			"Memories", "choiceAdventure365", "Seaside Megalopolis",
			new Object[] { "Muscle",
				       new Option( "multi-pass", "multi-pass" ) } ),

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

		// Ancient Temple
		new ChoiceAdventure(
			"Memories", "choiceAdventure376", "The Jungles of Ancient Loathing",
			new Object[] { "Enter the Temple", "leave" } ),

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
			new Object[] { "1 of each marble -> 32768 Meat", "skip adventure" } ),

		// Choice 394 is Hellevator Music
		// Choice 395 is Rumble On

		// Woolly Scaly Bully
		new ChoiceAdventure(
			"The Sea", "choiceAdventure396", "Mer-kin Elementary School",
			new Object[] { "lose HP", "lose HP", "unlock janitor's closet" } ),

		// Bored of Education
		new ChoiceAdventure(
			"The Sea", "choiceAdventure397", "Mer-kin Elementary School",
			new Object[] { "lose HP", "unlock the bathrooms", "lose HP" } ),

		// A Mer-kin Graffiti
		new ChoiceAdventure(
			"The Sea", "choiceAdventure398", "Mer-kin Elementary School",
			new Object[] { "unlock teacher's lounge", "lose HP", "lose HP"	} ),

		// The Case of the Closet
		new ChoiceAdventure(
			"The Sea", "choiceAdventure399", "Mer-kin Elementary School",
			new Object[] { "fight a Mer-kin monitor",
				       new Option( "Mer-kin sawdust", "Mer-kin sawdust" ) } ),

		// No Rest for the Room
		new ChoiceAdventure(
			"The Sea", "choiceAdventure400", "Mer-kin Elementary School",
			new Object[] { "fight a Mer-kin teacher",
				       new Option( "Mer-kin cancerstick", "Mer-kin cancerstick" ) } ),

		// Raising Cane
		new ChoiceAdventure(
			"The Sea", "choiceAdventure401", "Mer-kin Elementary School",
			new Object[] { "fight a Mer-kin punisher ",
				       new Option( "Mer-kin wordquiz", "Mer-kin wordquiz" ) } ),

		// Don't Hold a Grudge
		new ChoiceAdventure(
			"Manor2", "choiceAdventure402", "Haunted Bathroom",
			new Object[] { "muscle substats", "mysticality substats", "moxie substats" } ),

		// Picking Sides
		new ChoiceAdventure(
			"The Sea", "choiceAdventure403", "Skate Park",
			new Object[] { new Option( "skate blade", "skate blade" ),
				       new Option( "brand new key", "brand new key" ) } ),

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
		new Object[]{ IntegerPool.get(438), IntegerPool.get(1),
		  ItemPool.get( ItemPool.UNDERWORLD_ACORN, -1 ) },

		// Choice 439 is unknown
		// Choice 440 is Puttin' on the Wax
		// Choice 441 is The Mad Tea Party
		// Choice 442 is A Moment of Reflection
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure442", "A Moment of Reflection",
			new Object[] { "Seal Clubber/Pastamancer/custard", "Accordion Thief/Sauceror/comfit", "Turtle Tamer/Disco Bandit/croqueteer", "Ittah bittah hookah", "Chessboard", "nothing" } ),

		// Choice 443 is Chess Puzzle

		// Choice 444 is The Field of Strawberries (Seal Clubber)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure444", "Reflection of Map (Seal Clubber)",
			new Object[] { new Option( "walrus ice cream", "walrus ice cream" ),
				       new Option( "yellow matter custard", "yellow matter custard" ) } ),

		// Choice 445 is The Field of Strawberries (Pastamancer)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure445", "Reflection of Map (Pastamancer)",
			new Object[] { new Option( "eggman noodles", "eggman noodles" ),
				       new Option( "yellow matter custard", "yellow matter custard" ) } ),

		// Choice 446 is The Caucus Racetrack (Accordion Thief)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure446", "Reflection of Map (Accordion Thief)",
			new Object[] { new Option( "missing wine", "missing wine" ),
				       new Option( "delicious comfit?", "delicious comfit?" ) } ),

		// Choice 447 is The Caucus Racetrack (Sauceror)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure447", "Reflection of Map (Sauceror)",
			new Object[] { new Option( "Vial of <i>jus de larmes</i>", "Vial of <i>jus de larmes</i>" ),
				       new Option( "delicious comfit?", "delicious comfit?" ) } ),

		// Choice 448 is The Croquet Grounds (Turtle Tamer)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure448", "Reflection of Map (Turtle Tamer)",
			new Object[] { new Option( "beautiful soup", "beautiful soup" ),
				       "fight croqueteer" } ),

		// Choice 449 is The Croquet Grounds (Disco Bandit)
		new ChoiceAdventure(
			"RabbitHole", "choiceAdventure449", "Reflection of Map (Disco Bandit)",
			new Object[] { new Option( "Lobster <i>qua</i> Grill", "Lobster <i>qua</i> Grill" ),
				       "fight croqueteer" } ),

		// Choice 450 is The Duchess' Cottage

		// Typographical Clutter
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure451", "Greater-Than Sign",
			new Object[] { new Option( "left parenthesis", "left parenthesis" ),
				       "moxie, alternately lose then gain meat",
				       new Option( "plus sign, then muscle", "plus sign" ),
				       "mysticality substats",
				       "get teleportitis" } ),

		// Leave a Message and I'll Call You Back
		new ChoiceAdventure(
			"Jacking", "choiceAdventure452", "Small-O-Fier",
			new Object[] { "combat",
				       new Option( "tiny fly glasses", "tiny fly glasses" ),
				       "fruit" } ),

		// Getting a Leg Up
		new ChoiceAdventure(
			"Jacking", "choiceAdventure453", "Small-O-Fier",
			new Object[] { "combat", "stats",
				       new Option( "hair of the calf", "hair of the calf" ) } ),

		// Just Like the Ocean Under the Moon
		new ChoiceAdventure(
			"Jacking", "choiceAdventure454", "Small-O-Fier",
			new Object[] { "combat", "HP and MP" } ),

		// Double Trouble in the Stubble
		new ChoiceAdventure(
			"Jacking", "choiceAdventure455", "Small-O-Fier",
			new Object[] { "stats", "quest item" } ),

		// Made it, Ma!	 Top of the World!
		new ChoiceAdventure(
			"Jacking", "choiceAdventure456", "Huge-A-Ma-tron",
			new Object[] { "combat", "Hurricane Force",
				       new Option( "a dance upon the palate", "a dance upon the palate" ),
				       "stats" } ),

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
			new Object[] { "gain some meat",
				       new Option( "gain stakes or trade vampire hearts", "wooden stakes" ),
				       new Option( "gain spooky sapling or trade bar skins", "spooky sapling" ) } ),

		// Tree's Last Stand
		new ChoiceSpoiler(
			"choiceAdventure504", "Spooky Forest",
			new Object[] { new Option( "bar skin", "bar skin" ),
				       new Option( "bar skins", "bar skin" ),
				       new Option( "buy spooky sapling", "spooky sapling" ),
				       "skip adventure" } ),
		// Tree's Last Stand
		new Object[]{ IntegerPool.get(504), IntegerPool.get(1),
		  ItemPool.get( ItemPool.BAR_SKIN, -1 ) },
		new Object[]{ IntegerPool.get(504), IntegerPool.get(2),
		  ItemPool.get( ItemPool.BAR_SKIN, 1 ) },
		new Object[]{ IntegerPool.get(504), IntegerPool.get(3),
		  new AdventureResult( AdventureResult.MEAT, -100 ) },

		// Consciousness of a Stream
		new ChoiceSpoiler(
			"choiceAdventure505", "Spooky Forest",
			new Object[] { new Option( "gain mosquito larva then 3 spooky mushrooms", "mosquito larva" ),
				       "gain 300 meat & tree-holed coin then nothing",
				       "fight a spooky vampire" } ),

		// Through Thicket and Thinnet
		new ChoiceSpoiler(
			"choiceAdventure506", "Spooky Forest",
			new Object[] { "gain a starter item",
				       new Option( "gain Spooky-Gro fertilizer", "Spooky-Gro fertilizer" ),
				       new Option( "gain spooky temple map", "spooky temple map" ) } ),

		// O Lith, Mon
		new ChoiceSpoiler(
			"choiceAdventure507", "Spooky Forest",
			new Object[] { "gain Spooky Temple map", "skip adventure", "skip adventure" } ),
		// O Lith, Mon
		new Object[]{ IntegerPool.get(507), IntegerPool.get(1),
		  ItemPool.get( ItemPool.TREE_HOLED_COIN, -1 ) },

		// Choice 508 is Pants-Gazing
		// Choice 509 is Of Course!
		// Choice 510 is Those Who Came Before You

		// If it's Tiny, is it Still a Mansion?
		new ChoiceAdventure(
			"Woods", "choiceAdventure511", "Typical Tavern",
			new Object[] { "Baron von Ratsworth", "skip adventure" } ),

		// Hot and Cold Running Rats
		new ChoiceAdventure(
			"Woods", "choiceAdventure512", "Typical Tavern",
			new Object[] { "fight", "skip adventure" } ),

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
			"Crimbo10", "choiceAdventure518", "Elf Alley",
			new Object[] { "enter combat with Uncle Hobo", "skip adventure" } ),

		// What a Tosser
		new ChoiceAdventure(
			"Crimbo10", "choiceAdventure519", "Elf Alley",
			new Object[] { new Option( "gift-a-pult", "gift-a-pult" ),
				       "skip adventure" } ),
		// What a Tosser - gift-a-pult
		new Object[]{ IntegerPool.get(519), IntegerPool.get(1),
		  ItemPool.get( ItemPool.HOBO_NICKEL, -50 ) },

		// Choice 520 is A Show-ho-ho-down
		// Choice 521 is A Wicked Buzz

		// Welcome to the Footlocker
		new ChoiceAdventure(
			"Knob", "choiceAdventure522", "Cobb's Knob Barracks",
			new Object[] { "outfit piece or donut", "skip adventure" } ),

		// Death Rattlin'
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure523", "Defiled Cranny",
			new Object[] { "small meat boost",
				       "stats & HP & MP",
				       new Option( "can of Ghuol-B-Gone&trade;", "can of Ghuol-B-Gone&trade;" ),
				       "fight swarm of ghuol whelps",
				       "skip adventure" } ),

		// Choice 524 is The Adventures of Lars the Cyberian
		// Choice 525 is Fiddling with a Puzzle
		// Choice 526 is unknown

		// Choice 527 is The Haert of Darkness
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure527", "Haert of the Cyrpt",
			new Object[] { "fight the Bonerdagon", "skip adventure" } ),

		// Choice 528 is It Was Then That a Hideous Monster Carried You

		// A Swarm of Yeti-Mounted Skeletons
		new ChoiceAdventure(
			"Events", "choiceAdventure529", "Skeleton Swarm",
			new Object[] { "Weapon Damage", "Spell Damage", "Ranged Damage" } ),

		// It Was Then That...	Aaaaaaaah!
		new ChoiceAdventure(
			"Events", "choiceAdventure530", "Icy Peak",
			new Object[] { new Option( "hideous egg", "hideous egg" ),
				       "skip the adventure" } ),

		// The Bonewall Is In
		new ChoiceAdventure(
			"Events", "choiceAdventure531", "Bonewall",
			new Object[] { "Item Drop", "HP Bonus" } ),

		// You'll Sink His Battleship
		new ChoiceAdventure(
			"Events", "choiceAdventure532", "Battleship",
			new Object[] { "Class Skills", "Accordion Thief Songs" } ),

		// Train, Train, Choo-Choo Train
		new ChoiceAdventure(
			"Events", "choiceAdventure533", "Supply Train",
			new Object[] { "Meat Drop", "Pressure Penalty Modifiers" } ),

		// That's No Bone Moon...
		new ChoiceAdventure(
			"Events", "choiceAdventure534", "Bone Star",
			new Object[] { new Option( "Torpedos", "photoprotoneutron torpedo" ),
				       "Initiative",
				       "Monster Level" } ),

		// Deep Inside Ronald, Baby
		new ChoiceAdventure( "Spaaace", "choiceAdventure535", "Deep Inside Ronald",
			SafetyShelterManager.RonaldGoals ),

		// Deep Inside Grimace, Bow Chick-a Bow Bow
		new ChoiceAdventure( "Spaaace", "choiceAdventure536", "Deep Inside Grimace",
			SafetyShelterManager.GrimaceGoals ),

		// Choice 537 is Play Porko!
		// Choice 538 is Big-Time Generator
		// Choice 539 is An E.M.U. for Y.O.U.
		// Choice 540 is Big-Time Generator - game board
		// Choice 541 is unknown
		// Choice 542 is Now's Your Pants!  I Mean... Your Chance!
		// Choice 543 is Up In Their Grill
		// Choice 544 is A Sandwich Appears!
		// Choice 545 is unknown

		// Interview With You
		new ChoiceAdventure( "Item-Driven", "choiceAdventure546", "Interview With You",
			VampOutManager.VampOutGoals ),

		// Behind Closed Doors
		new ChoiceAdventure(
			"Events", "choiceAdventure548", "Sorority House Necbromancer",
			new Object[] { "enter combat with The Necbromancer", "skip adventure" } ),

		// Dark in the Attic
		new ChoiceSpoiler(
			"Events", "choiceAdventure549", "Dark in the Attic",
			new Object[] { new Option( "staff guides", "Haunted Sorority House staff guide" ),
				       new Option( "ghost trap", "ghost trap" ),
				       "raise area ML",
				       "lower area ML",
				       new Option( "mass kill werewolves with silver shotgun shell", "silver shotgun shell" ) } ),

		// The Unliving Room
		new ChoiceSpoiler(
			"Events", "choiceAdventure550", "The Unliving Room",
			new Object[] { "raise area ML",
				       "lower area ML",
				       new Option( "mass kill zombies with chainsaw chain", "chainsaw chain" ),
				       new Option( "mass kill skeletons with funhouse mirror", "funhouse mirror" ),
				       "get costume item" } ),

		// Debasement
		new ChoiceSpoiler(
			"Events", "choiceAdventure551", "Debasement",
			new Object[] { "Prop Deportment",
				       "mass kill vampires with plastic vampire fangs",
				       "raise area ML",
				       "lower area ML" } ),

		// Prop Deportment
		new ChoiceSpoiler(
			"Events", "choiceAdventure552", "Prop Deportment",
			new Object[] { new Option( "chainsaw chain", "chainsaw chain" ),
				       new Option( "create a silver shotgun shell", "silver shotgun shell" ),
				       new Option( "funhouse mirror", "funhouse mirror" ) } ),

		// Relocked and Reloaded
		new ChoiceSpoiler(
			"Events", "choiceAdventure553", "Relocked and Reloaded",
			new Object[] { new Option( "", "Maxwell's Silver hammer" ),
				       new Option( "", "silver tongue charrrm bracelet" ),
				       new Option( "", "silver cheese-slicer" ),
				       new Option( "", "silver shrimp fork" ),
				       new Option( "", "silver pat&eacute; knife" ),
				       "exit adventure" } ),

		// Behind the Spooky Curtain
		new ChoiceSpoiler(
			"Events", "choiceAdventure554", "Behind the Spooky Curtain",
			new Object[] { "staff guides, ghost trap, kill werewolves", "kill zombies, kill skeletons, costume item", "chainsaw chain, silver item, funhouse mirror, kill vampires" } ),

		// More Locker Than Morlock
		new ChoiceAdventure(
			"McLarge", "choiceAdventure556", "Itznotyerzitz Mine",
			new Object[] { "get an outfit piece", "skip adventure" } ),

		// Gingerbread Homestead
		new ChoiceAdventure(
			"The Candy Diorama", "choiceAdventure557", "Gingerbread Homestead",
			new Object[] { "get candies",
				       new Option( "licorice root", "licorice root" ),
				       new Option( "skip adventure or make a lollipop stick item", "lollipop stick" ) } ),

		// Tool Time
		new ChoiceAdventure(
			"The Candy Diorama", "choiceAdventure558", "Tool Time",
			new Object[] { new Option( "sucker bucket", "sucker bucket" ),
				       new Option( "sucker kabuto", "sucker kabuto" ),
				       new Option( "sucker hakama", "sucker hakama" ),
				       new Option( "sucker tachi", "sucker tachi" ),
				       new Option( "sucker scaffold", "sucker scaffold" ),
				       "skip adventure" } ),

		// Fudge Mountain Breakdown
		new ChoiceAdventure(
			"The Candy Diorama", "choiceAdventure559", "Fudge Mountain Breakdown",
			new Object[] { new Option( "fudge lily", "fudge lily" ),
				       "fight a swarm of fudgewasps or skip adventure",
				       new Option( "frigid fudgepuck or skip adventure", "frigid fudgepuck" ),
				       new Option( "superheated fudge or skip adventure", "superheated fudge" ) } ),

		// Foreshadowing Demon!
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure560", "The Clumsiness Grove",
			new Object[] { "head towards boss", "skip adventure" } ),

		// You Must Choose Your Destruction!
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure561", "The Clumsiness Grove",
			new Object[] { "The Thorax", "The Bat in the Spats" } ),

		// Choice 562 is You're the Fudge Wizard Now, Dog

		// A Test of your Mettle
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure563", "The Clumsiness Grove",
			new Object[] { "Fight Boss", "skip adventure" } ),

		// A Maelstrom of Trouble
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure564", "The Maelstrom of Lovers",
			new Object[] { "head towards boss", "skip adventure" } ),

		// To Get Groped or Get Mugged?
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure565", "The Maelstrom of Lovers",
			new Object[] { "The Terrible Pinch", "Thug 1 and Thug 2" } ),

		// A Choice to be Made
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure566", "The Maelstrom of Lovers",
			new Object[] { "Fight Boss", "skip adventure" } ),

		// You May Be on Thin Ice
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure567", "The Glacier of Jerks",
			new Object[] { "Fight Boss", "skip adventure" } ),

		// Some Sounds Most Unnerving
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure568", "The Glacier of Jerks",
			new Object[] { "Mammon the Elephant", "The Large-Bellied Snitch" } ),

		// One More Demon to Slay
		new ChoiceAdventure(
			"Suburbs", "choiceAdventure569", "The Glacier of Jerks",
			new Object[] { "head towards boss", "skip adventure" } ),

		// Choice 571 is Your Minstrel Vamps
		// Choice 572 is Your Minstrel Clamps
		// Choice 573 is Your Minstrel Stamps
		// Choice 574 is The Minstrel Cycle Begins

		// Duffel on the Double
		new ChoiceAdventure(
			"McLarge", "choiceAdventure575", "eXtreme Slope",
			new Object[] { "get an outfit piece",
				       new Option( "jar of frostigkraut", "jar of frostigkraut" ),
				       "skip adventure" } ),

		// Choice 576 is Your Minstrel Camps
		// Choice 577 is Your Minstrel Scamp
		// Choice 578 is End of the Boris Road

		// Such Great Heights
		new ChoiceAdventure(
			"Woods", "choiceAdventure579", "Hidden Temple Heights",
			new Object[] { "mysticality substats",
				       new Option( "Nostril of the Serpent then skip adventure", "Nostril of the Serpent" ),
				       "gain 3 adv then skip adventure" } ),

		// Choice 580 is The Hidden Heart of the Hidden Temple (4 variations)

		// Such Great Depths
		new ChoiceAdventure(
			"Woods", "choiceAdventure581", "Hidden Temple Depths",
			new Object[] { new Option( "glowing fungus", "glowing fungus" ),
				       "+15 mus/mys/mox then skip adventure", "fight clan of cave bars" } ),

		// Fitting In
		new ChoiceAdventure(
			"Woods", "choiceAdventure582", "Hidden Temple",
			new Object[] { "Such Great Heights", "heart of the Hidden Temple", "Such Great Depths" } ),

		// Confusing Buttons
		new ChoiceSpoiler(
			"Woods", "choiceAdventure583", "Hidden Temple",
			new Object[] { "Press a random button" } ),

		// Unconfusing Buttons
		new ChoiceAdventure(
			"Woods", "choiceAdventure584", "Hidden Temple",
			new Object[] { "Hidden Temple (Stone) - muscle substats",
				       "Hidden Temple (Sun) - gain ancient calendar fragment",
				       "Hidden Temple (Gargoyle) - MP",
				       "Hidden Temple (Pikachutlotal) - Hidden City unlock" } ),

		// A Lost Room
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure594", "Lost Key",
			new Object[] { new Option( "lost glasses", "lost glasses" ),
				       new Option( "lost comb", "lost comb" ),
				       new Option( "lost pill bottle", "lost pill bottle" ) } ),

		// Choice 585 is Screwing Around!
		// Choice 586 is All We Are Is Radio Huggler

		// Choice 588 is Machines!
		// Choice 589 is Autopsy Auturvy
		// Choice 590 is Not Alone In The Dark

		// Fire! I... have made... fire!
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure595", "CSA fire-starting kit",
			new Object[] { "pvp fights", "hp/mp regen" } ),

		// Choice 596 is Dawn of the D'oh

		// Cake Shaped Arena
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure597", "Reagnimated Gnome",
			new Object[] { new Option( "gnomish swimmer's ears (underwater)", "gnomish swimmer's ears" ),
				       new Option( "gnomish coal miner's lung (block)", "gnomish coal miner's lung" ),
				       new Option( "gnomish tennis elbow (damage)", "gnomish tennis elbow" ),
				       new Option( "gnomish housemaid's kgnee (gain advs)", "gnomish housemaid's kgnee" ),
				       new Option( "gnomish athlete's foot (delevel)", "gnomish athlete's foot" ) } ),

		// Choice 598 is Recruitment Jive
		// Choice 599 is A Zombie Master's Bait
		// Choice 600 is Summon Minion
		// Choice 601 is Summon Horde
		// Choice 602 is Behind the Gash

		// Skeletons and The Closet
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure603", "Skeleton",
			new Object[] { "warrior (dmg, delevel)", "cleric (hot dmg, hp)", "wizard (cold dmg, mp)", "rogue (dmg, meat)", "buddy (delevel, exp)",	"ignore this adventure" } ),

		// Choice 604 is unknown
		// Choice 605 is Welcome to the Great Overlook Lodge
		// Choice 606 is Lost in the Great Overlook Lodge
		// Choice 607 is Room 237
		// Choice 608 is Go Check It Out!
		// Choice 609 is There's Always Music In the Air
		// Choice 610 is To Catch a Killer
		// Choice 611 is The Horror... (A-Boo Peak)
		// Choice 612 is Behind the world there is a door...
		// Choice 613 is Behind the door there is a fog
		// Choice 614 is Near the fog there is an... anvil?
		// Choice 615 is unknown

		// Choice 616 is He Is the Arm, and He Sounds Like This
		// Choice 617 is Now It's Dark
		// Choice 618 is Cabin Fever
		// Choice 619 is To Meet a Gourd
		// Choice 620 is A Blow Is Struck!
		// Choice 621 is Hold the Line!
		// Choice 622 is The Moment of Truth
		// Choice 623 is Return To the Fray!
		// Choice 624 is Returning to Action
		// Choice 625 is The Table
		// Choice 626 is Super Crimboman Crimbo Type is Go!
		// Choice 627 is unknown
		// Choice 628 is unknown
		// Choice 629 is unknown
		// Choice 630 is unknown
		// Choice 631 is unknown
		// Choice 632 is unknown
		// Choice 633 is ChibiBuddy&trade;
		// Choice 634 is Goodbye Fnord
		// Choice 635 is unknown
		// Choice 636 is unknown
		// Choice 637 is unknown
		// Choice 638 is unknown
		// Choice 639 is unknown
		// Choice 640 is unknown
		// Choice 641 is Stupid Pipes.
		// Choice 642 is You're Freaking Kidding Me
		// Choice 643 is Great. A Stupid Door. What Next?
		// Choice 644 is Snakes.
		// Choice 645 is So... Many... Skulls...
		// Choice 646 is Oh No... A Door...
		// Choice 647 is A Stupid Dummy. Also, a Straw Man.
		// Choice 648 is Slings and Arrows
		// Choice 649 is A Door. Figures.
		// Choice 650 is This Is Your Life. Your Horrible, Horrible Life.
		// Choice 651 is The Wall of Wailing
		// Choice 652 is A Door. Too Soon...
		// Choice 653 is unknown
		// Choice 654 is Courier? I don't even...
		// Choice 655 is They Have a Fight, Triangle Loses
		// Choice 656 is Wheels Within Wheel

		// You Grind 16 Rats, and Whaddya Get?
		new ChoiceAdventure(
			"Psychoses", "choiceAdventure657", "Chinatown Tenement",
			new Object[] { "Fight Boss", "skip adventure" } ),

		// Choice 658 is Debasement
		// Choice 659 is How Does a Floating Platform Even Work?
		// Choice 660 is It's a Place Where Books Are Free
		// Choice 661 is Sphinx For the Memories
		// Choice 662 is Think or Thwim
		// Choice 663 is When You're a Stranger
		// Choice 664 is unknown
		// Choice 665 is A Gracious Maze
		// Choice 666 is unknown
		// Choice 667 is unknown
		// Choice 668 is unknown

		// The Fast and the Furry-ous
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure669", "Basement Furry",
			new Object[] { "Open Ground Floor with titanium umbrella, otherwise Neckbeard Choice", "200 Moxie substats",
				"???", "skip adventure and guarantees this adventure will reoccur" } ),

		// You Don't Mess Around with Gym
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure670", "Basement Fitness",
			new Object[] { new Option( "massive dumbbell, then skip adventure", "massive dumbbell" ),
				       "Muscle stats",
				       "Items",
				       "Open Ground Floor with amulet, otherwise skip" } ),

		// Out in the Open Source
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure671", "Basement Neckbeard",
			new Object[] { new Option( "With massive dumbbell, open Ground Floor, otherwise skip adventure", "massive dumbbell" ),
				       "200 Mysticality substats",
				       "O'RLY manual, open sauce",
				       "Fitness Choice" } ),

		// There's No Ability Like Possibility
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure672", "Ground Possibility",
			new Object[] { "3 random items", "Nothing Is Impossible", "skip adventure" } ),

		// Putting Off Is Off-Putting
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure673", "Ground Procrastination",
			new Object[] { new Option( "very overdue library book, then skip adventure", "very overdue library book" ),
				       "Trash-Wrapped", "skip adventure" } ),

		// Huzzah!
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure674", "Ground Renaissance",
			new Object[] { new Option( "pewter claymore, then skip adventure", "pewter claymore" ),
				       "Pretending to Pretend", "skip adventure" } ),

		// Melon Collie and the Infinite Lameness
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure675", "Top Goth",
			new Object[] { "Fight a Goth Giant",
				       new Option( "complete quest", "drum 'n' bass 'n' drum 'n' bass record" ),
				       new Option( "3 thin black candles", "thin black candle" ),
				       "Steampunk Choice" } ),

		// Flavor of a Raver
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure676", "Top Raver",
			new Object[] { "Fight a Raver Giant",
				       "Restore 1000 hp & mp",
				       new Option( "drum 'n' bass 'n' drum 'n' bass record, then skip adventure", "drum 'n' bass 'n' drum 'n' bass record" ),
				       "Punk Rock Choice" } ),

		// Copper Feel
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure677", "Top Steampunk",
			new Object[] { new Option( "With model airship, complete quest, otherwise fight Steampunk Giant", "model airship" ),
				       new Option( "steam-powered model rocketship, then skip adventure", "steam-powered model rocketship" ),
				       new Option( "brass gear", "brass gear" ),
				       "Goth Choice" } ),

		// Yeah, You're for Me, Punk Rock Giant
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure678", "Top Punk Rock",
			new Object[] { "Wearing mohawk wig, turn wheel, otherwise fight Punk Rock Giant",
				       "500 meat",
				       "Steampunk Choice",
				       "Raver Choice" } ),

		// Choice 679 is Keep On Turnin' the Wheel in the Sky
		// Choice 680 is Are you a Man or a Mouse?
		// Choice 681 is F-F-Fantastic!
		// Choice 682 is Now Leaving Jarlsberg, Population You

		// Choice 686 is Of Might and Magic

		// Choice 689 is The Final Chest
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure689", "Daily Dungeon",
			new Object[] { "Get fat loot token" } ),

		// The First Chest Isn't the Deepest.
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure690", "Daily Dungeon",
			new Object[] { "Get item", "Skip to 8th chamber, no turn spent", "Skip to 6th chamber, no turn spent" } ),

		// Second Chest
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure691", "Daily Dungeon",
			new Object[] { "Get item", "Skip to 13th chamber, no turn spent", "Skip to 11th chamber, no turn spent" } ),

		// Choice 692 is I Wanna Be a Door
				
		// It's Almost Certainly a Trap
		new ChoiceAdventure(
			"Dungeon", "choiceAdventure693", "Daily Dungeon",
			new Object[] { "Suffer elemental damage, get stats", "Avoid trap, no turn spent", "Leave, no turn spent" } ),

		// Choice 695 is A Drawer of Chests

		// Choice 701 is Ators Gonna Ate
		new ChoiceAdventure(
			"The Sea", "choiceAdventure701", "Mer-kin Gymnasium",
			new Object[] { "get an item", "skip adventure" } ),

		// Choice 703 is Mer-kin dreadscroll
		// Choice 704 is Playing the Catalog Card
		// Choice 705 is Halls Passing in the Night
		new ChoiceAdventure(
			"The Sea", "choiceAdventure705", "Mer-kin Elementary School",
			new Object[] { "fight a Mer-kin spectre",
				       new Option( "Mer-kin sawdust", "Mer-kin sawdust" ),
				       new Option( "Mer-kin cancerstick", "Mer-kin cancerstick" ),
				       new Option( "Mer-kin wordquiz", "Mer-kin wordquiz" ) } ),

		//     Shub-Jigguwatt (Violence) path
		// Choice 706 is In The Temple of Violence, Shine Like Thunder
		// Choice 707 is Flex Your Pecs in the Narthex
		// Choice 708 is Don't Falter at the Altar
		// Choice 709 is You Beat Shub to a Stub, Bub

		//     Yog-Urt (Hatred) path
		// Choice 710 is They've Got Fun and Games
		// Choice 711 is They've Got Everything You Want
		// Choice 712 is Honey, They Know the Names
		// Choice 713 is You Brought Her To Her Kn-kn-kn-kn-knees, Knees.

		//     Dad Sea Monkee (Loathing) path
		// Choice 714 is An Unguarded Door (1)
		// Choice 715 is Life in the Stillness
		// Choice 716 is An Unguarded Door (2)
		// Choice 717 is Over. Over Now.

		// The Cabin in the Dreadsylvanian Woods
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure721", "Cabin",
			new Object[] { new Option( "learn shortcut", 5),
				       new Option( "skip adventure", 6 ) },
			1 ),

		// Choice 722 is The Kitchen in the Woods
		// Choice 723 is What Lies Beneath (the Cabin)
		// Choice 724 is Where it's Attic

		// Tallest Tree in the Forest
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure725", "Tallest Tree",
			new Object[] { new Option( "learn shortcut", 5),
				       new Option( "skip adventure", 6 ) },
			2 ),

		// Choice 726 is Top of the Tree, Ma!
		// Choice 727 is All Along the Watchtower
		// Choice 728 is Treebasing

		// Below the Roots
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure729", "Burrows",
			new Object[] { new Option( "learn shortcut", 5),
				       new Option( "skip adventure", 6 ) },
			3 ),

		// Choice 730 is Hot Coals
		// Choice 731 is The Heart of the Matter
		// Choice 732 is Once Midden, Twice Shy

		// Dreadsylvanian Village Square
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure733", "Village Square",
			new Object[] { new Option( "learn shortcut", 5),
				       new Option( "skip adventure", 6 ) },
			4 ),

		// Choice 734 is Fright School
		// Choice 735 is Smith, Black as Night
		// Choice 736 is Gallows

		// The Even More Dreadful Part of Town
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure737", "Skid Row",
			new Object[] { new Option( "learn shortcut", 5),
				       new Option( "skip adventure", 6 ) },
			5 ),

		// Choice 738 is A Dreadful Smell
		// Choice 739 is The Tinker's. Damn.
		// Choice 740 is Eight, Nine, Tenement

		// The Old Duke's Estate
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure741", "Old Duke's Estate",
			new Object[] { new Option( "learn shortcut", 5),
				       new Option( "skip adventure", 6 ) },
			6 ),

		// Choice 742 is The Plot Thickens
		// Choice 743 is No Quarter
		// Choice 744 is The Master Suite -- Sweet!

		// This Hall is Really Great
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure745", "Great Hall",
			new Object[] { new Option( "learn shortcut", 5),
				       new Option( "skip adventure", 6 ) },
			8 ),

		// Choice 746 is The Belle of the Ballroom
		// Choice 747 is Cold Storage
		// Choice 748 is Dining In (the Castle)

		// Tower Most Tall
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure749", "Tower",
			new Object[] { new Option( "learn shortcut", 5 ),
				       new Option( "skip adventure", 6 ) },
			7 ),

		// Choice 750 is Working in the Lab, Late One Night
		// Choice 751 is Among the Quaint and Curious Tomes.
		// Choice 752 is In The Boudoir

		// The Dreadsylvanian Dungeon
		new ChoiceAdventure(
			"Dreadsylvania", "choiceAdventure753", "Dungeons",
			new Object[] { new Option( "learn shortcut", 5),
				       new Option( "skip adventure", 6 ) },
			9 ),

		// Choice 754 is Live from Dungeon Prison
		// Choice 755 is The Hot Bowels
		// Choice 756 is Among the Fungus

		// Choice 757 is ???

		// Choice 758 is End of the Path
		// Choice 759 is You're About to Fight City Hall
		// Choice 760 is Holding Court
		// Choice 761 is Staring Upwards...
		// Choice 762 is Try New Extra-Strength Anvil
		// Choice 763 is ???
		// Choice 764 is The Machine
		// Choice 765 is Hello Gallows
		// Choice 766 is ???
		// Choice 767 is Tales of Dread

		// Choice 768 is The Littlest Identity Crisis
		// Choice 771 is It Was All a Horrible, Horrible Dream
		
		// Choice 772 is Saved by the Bell 
		// Choice 774 is Opening up the Folder Holder
		// Choice 780 is Action Elevator
		// Choice 781 is Earthbound and Down
		// Choice 783 is Water You Dune
		// Choice 784 is You, M. D.
		// Choice 785 is Air Apparent
		// Choice 786 is Working Holiday
		// Choice 787 is Fire when Ready
		// Choice 788 is Life is Like a Cherry of Bowls
		// Choice 789 is Where Does The Lone Ranger Take His Garbagester?
		// Choice 791 is Legend of the Temple in the Hidden City

		// Choice 793 is Welcome to The Shore, Inc.
		new ChoiceAdventure(
			"Beach", "choiceAdventure793", "The Shore",
			new Object[] { "Muscle Vacation",
				       "Mysticality Vacation",
				       "Moxie Vacation" } ),
		
		// Choice 794 is Once More Unto the Junk
		new ChoiceAdventure(
			"Woods", "choiceAdventure794", "The Old Landfill",
			new Object[] { "The Bathroom of Ten Men",
				       "The Den of Iquity",
				       "Let's Workshop This a Little" } ),
		// Choice 795 is The Bathroom of Ten Men
		new ChoiceAdventure(
			"Woods", "choiceAdventure795", "The Bathroom of Ten Men",
			new Object[] { new Option( "old claw-foot bathtub", "old claw-foot bathtub" ),
				       "fight junksprite",
				       "make lots of noise" } ),
		// Choice 796 is The Den of Iquity
		new ChoiceAdventure(
			"Woods", "choiceAdventure796", "The Den of Iquity",
			new Object[] { "make lots of noise",
				       new Option( "old clothesline pole", "old clothesline pole" ),
				       new Option( "tangle of copper wire", "tangle of copper wire" ) } ),
		// Choice 797 is Let's Workshop This a Little
		new ChoiceAdventure(
			"Woods", "choiceAdventure797", "Let's Workshop This a Little",
			new Object[] { new Option( "Junk-Bond", "Junk-Bond" ),
				       "make lots of noise",
				       new Option( "antique cigar sign", "antique cigar sign" ) } ),
					   
		// Choice 801 is A Reanimated Conversation

		// Choice 803 is Behind the Music.  Literally.
		new ChoiceAdventure(
			"Events", "choiceAdventure803", "The Space Odyssey Discotheque",
			new Object[] { new Option( "gain 2-3 horoscopes", 1 ),
				       new Option( "find interesting room", 3 ),
				       new Option( "investigate interesting room", 4 ),
				       new Option( "investigate trap door", 5 ),
				       new Option( "investigate elevator", 6 ) } ),

		// Choice 804 is Trick or Treat!
		// Choice 805 is A Sietch in Time

		// Choice 808 is Silence at Last.
		new ChoiceAdventure(
			"Events", "choiceAdventure808", "The Spirit World",
			new Object[] { new Option( "gain spirit bed piece" ),
						new Option( "fight spirit alarm clock" ) } ),

		// Choice 809 is Uncle Crimbo's Trailer
		// Choice 810 is K.R.A.M.P.U.S. facility

		// Choice 813 is What Warbears Are Good For
		new ChoiceAdventure(
			"Crimbo13", "choiceAdventure813", "Warbear Fortress (First Level)",
			new Object[] { "Open K.R.A.M.P.U.S. facility" } ),
		
		// Choice 822 is The Prince's Ball (In the Restroom)
		// Choice 823 is The Prince's Ball (On the Dance Floor)
		// Choice 824 is The Prince's Ball (In the Kitchen)
		// Choice 825 is The Prince's Ball (On the Balcony)
		// Choice 826 is The Prince's Ball (In the Lounge)
		// Choice 827 is The Prince's Ball (At the Canap&eacute;s Table)

		// Choice 829 is We All Wear Masks

		// Choice 830 is Cooldown
		new ChoiceAdventure(
			"Skid Row", "choiceAdventure830", "Cooldown",
			new Object[] { "+Wolf Offence or +Wolf Defence",
				       "+Wolf Elemental Attacks or +Rabbit",
				       "Improved Howling! or +Wolf Lung Capacity",
				       new Option( "Leave", 6 ) } ),
		// Choice 832 is Shower Power
		new ChoiceAdventure(
			"Skid Row", "choiceAdventure832", "Shower Power",
			new Object[] { "+Wolf Offence",
				       "+Wolf Defence" } ),
		// Choice 833 is Vendie, Vidi, Vici
		new ChoiceAdventure(
			"Skid Row", "choiceAdventure833", "Vendie, Vidi, Vici",
			new Object[] { "+Wolf Elemental Attacks",
				       "+Rabbit" } ),
		// Choice 834 is Back Room Dealings
		new ChoiceAdventure(
			"Skid Row", "choiceAdventure834", "Back Room Dealings",
			new Object[] { new Option( "Improved Howling!", 2 ),
				       new Option( "+Wolf Lung Capacity", 3 ) } ),

		// Choice 835 is Barely Tales
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure835", "Grim Brother",
			new Object[] { "30 turns of +20 initiative",
				       "30 turns of +20 max HP, +10 max MP",
				       "30 turns of +10 Weapon Damage, +20 Spell Damage" } ),

		// Choice 836 is Adventures Who Live in Ice Houses...
		
		// Choice 837 is On Purple Pond
		new ChoiceAdventure(
			"The Candy Witch and the Relentless Child Thieves", "choiceAdventure837", "On Purple Pond",
			new Object[] { "find out the two children not invading",
				       "+1 Moat",
				       "gain Candy" } ),
		// Choice 838 is General Mill
		new ChoiceAdventure(
			"The Candy Witch and the Relentless Child Thieves", "choiceAdventure838", "General Mill",
			new Object[] { "+1 Moat",
				       "gain Candy" } ),
		// Choice 839 is On The Sounds of the Undergrounds
		new ChoiceAdventure(
			"The Candy Witch and the Relentless Child Thieves", "choiceAdventure839", "The Sounds of the Undergrounds",
			new Object[] { "learn what the first two waves will be",
				       "+1 Minefield Strength",
				       "gain Candy" } ),
		// Choice 840 is Hop on Rock Pops
		new ChoiceAdventure(
			"The Candy Witch and the Relentless Child Thieves", "choiceAdventure840", "Hop on Rock Pops",
			new Object[] { "+1 Minefield Strength",
				       "gain Candy" } ),
		// Choice 841 is Building, Structure, Edifice
		new ChoiceAdventure(
			"The Candy Witch and the Relentless Child Thieves", "choiceAdventure841", "Building, Structure, Edifice",
			new Object[] { "increase candy in another location",
				       "+2 Random Defense",
				       "gain Candy" } ),
		// Choice 842 is The Gingerbread Warehouse
		new ChoiceAdventure(
			"The Candy Witch and the Relentless Child Thieves", "choiceAdventure842", "The Gingerbread Warehouse",
			new Object[] { "+1 Wall Strength",
				       "+1 Poison Jar",
				       "+1 Anti-Aircraft Turret",
				       "gain Candy" } ),

		// Choice 844 is The Portal to Horrible Parents
		// Choice 845 is Rumpelstiltskin's Workshop
		// Choice 846 is Bartering for the Future of Innocent Children
		// Choice 847 is Pick Your Poison
		// Choice 848 is Where the Magic Happens
		// Choice 850 is World of Bartercraft

		// Choice 851 is Shen Copperhead, Nightclub Owner
		// Choice 852 is Shen Copperhead, Jerk
		// Choice 853 is Shen Copperhead, Huge Jerk
		// Choice 854 is Shen Copperhead, World's Biggest Jerk
		// Choice 855 is Behind the 'Stache
		new ChoiceAdventure(
			"Town", "choiceAdventure855", "Behind the 'Stache",
			new Object[] { "don't take initial damage in fights",
				       "can get priceless diamond",
				       "can make Flamin' Whatshisname",
				       "get 4-5 random items" } ),
		// Choice 856 is This Looks Like a Good Bush for an Ambush
		new ChoiceAdventure(
			"The Red Zeppelin's Mooring", "choiceAdventure856", "This Looks Like a Good Bush for an Ambush",
			new Object[] { "scare protestors (more with lynyrd gear)",
				       "skip adventure" } ),
		// Choice 857 is Bench Warrant
		new ChoiceAdventure(
			"The Red Zeppelin's Mooring", "choiceAdventure857", "Bench Warrant",
			new Object[] { "creep protestors (more with sleaze damage/sleaze spell damage)",
				       "skip adventure" } ),
		// Choice 858 is Fire Up Above
		new ChoiceAdventure(
			"The Red Zeppelin's Mooring", "choiceAdventure858", "Fire Up Above",
			new Object[] { "set fire to protestors (more with Flamin' Whatshisname)",
				       "skip adventure" } ),
		// Choice 866 is Methinks the Protesters Doth Protest Too Little
		new ChoiceAdventure(
			"The Red Zeppelin's Mooring", "choiceAdventure866", "Methinks the Protesters Doth Protest Too Little",
			new Object[] { "scare protestors (more with lynyrd gear)",
				       "creep protestors (more with sleaze damage/sleaze spell damage)",
				       "set fire to protestors (more with Flamin' Whatshisname)" } ),

		// Rod Nevada, Vendor
		new ChoiceSpoiler(
			"Plains", "choiceAdventure873", "The Palindome",
			new Object[] { new Option( "photograph of a red nugget", "photograph of a red nugget" ),
				       "skip adventure" } ),
		// Rod Nevada, Vendor
		new Object[]{ IntegerPool.get(873), IntegerPool.get(1),
		  new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Welcome To Our ool Table
		new ChoiceAdventure(
			"Manor1", "choiceAdventure875", "Pool Table",
			new Object[] { "try to beat ghost",
				       "improve pool skill",
				       "skip" } ),

		// One Simple Nightstand
		new ChoiceAdventure(
			"Manor2", "choiceAdventure876", "One Simple Nightstand",
			new Object[] { new Option( "old leather wallet", 1 ),
				       new Option( "muscle substats", 2 ),
				       new Option( "muscle substats (with ghost key)", 3 ),
				       new Option( "skip", 6 ) } ),

		// One Mahogany Nightstand
		new ChoiceAdventure(
			"Manor2", "choiceAdventure877", "One Mahogany Nightstand",
			new Object[] { new Option( "old coin purse or half a memo", 1 ),
				       new Option( "take damage", 2 ),
				       new Option( "quest item", 3 ),
				       new Option( "gain more meat (with ghost key)", 4 ),
				       new Option( "skip", 6 ) } ),

		// One Ornate Nightstand
		new ChoiceAdventure(
			"Manor2", "choiceAdventure878", "One Ornate Nightstand",
			new Object[] { new Option( "small meat boost", 1 ),
				       new Option( "mysticality substats", 2 ),
				       new Option( "Lord Spookyraven's spectacles", 3 ),
				       new Option( "disposable instant camera", 4 ),
				       new Option( "mysticality substats (with ghost key)", 5 ),
				       new Option( "skip", 6 ) } ),

		// One Rustic Nightstand
		new ChoiceAdventure(
			"Manor2", "choiceAdventure879", "One Rustic Nightstand",
			new Object[] { new Option( "moxie", 1 ),
				       new Option( "empty drawer", 2 ),
				       new Option( "enter combat with mistress (once only)", 3 ),
				       new Option( "Engorged Sausages and You or ballroom key and moxie", 4 ),
				       new Option( "moxie substats (with ghost key)", 5 ),
					   new Option( "skip", 6 )} ),

		// One Elegant Nightstand
		new ChoiceAdventure(
			"Manor2", "choiceAdventure880", "One Elegant Nightstand",
			new Object[] { new Option( "Lady Spookyraven's finest gown (once only)", 1 ),
				       new Option( "elegant nightstick", 2 ),
				       new Option( "stats (with ghost key)", 3 ),
				       new Option( "skip", 6 ) } ),

		// Off the Rack
		new ChoiceAdventure(
			"Manor2", "choiceAdventure882", "Bathroom Towel",
			new Object[] { "get towel",
				       "skip" } ),

		// Take a Look, it's in a Book!
		new ChoiceSpoiler(
			"choiceAdventure888", "Haunted Library",
			new Object[] { "background history",
				       "cooking recipe",
				       "other options",
				       "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceSpoiler(
			"choiceAdventure889", "Haunted Library",
			new Object[] { "gallery quest",
				       "cocktailcrafting recipe",
				       "muscle substats",
				       "skip adventure" } ),

		// Choice 890 is Lights Out in the Storage Room
		// Choice 891 is Lights Out in the Laundry Room
		// Choice 892 is Lights Out in the Bathroom
		// Choice 893 is Lights Out in the Kitchen
		// Choice 894 is Lights Out in the Library
		// Choice 895 is Lights Out in the Ballroom
		// Choice 896 is Lights Out in the Gallery
		// Choice 897 is Lights Out in the Bedroom
		// Choice 898 is Lights Out in the Nursery
		// Choice 899 is Lights Out in the Conservatory
		// Choice 900 is Lights Out in the Billiards Room
		// Choice 901 is Lights Out in the Wine Cellar
		// Choice 902 is Lights Out in the Boiler Room
		// Choice 903 is Lights Out in the Laboratory

		// Choices 904-913 are Escher print adventures

		// Louvre It or Leave It
		new ChoiceSpoiler(
			"choiceAdventure914", "Haunted Gallery",
			new Object[] { "Enter the Drawing", "skip adventure" } ),

		// Choice 918 is Yachtzee!
		new ChoiceAdventure(
			"Spring Break Beach", "choiceAdventure918", "Yachtzee!",
			new Object[] { "get cocktail ingredients (sometimes Ultimate Mind Destroyer)",
				       "get 5k meat and random item",
				       "get Beach Bucks" } ),

		// Choice 919 is Break Time!
		new ChoiceAdventure(
			"Spring Break Beach", "choiceAdventure919", "Break Time!",
			new Object[] { "get Beach Bucks",
				       "+15ML on Sundaes",
				       "+15ML on Burgers",
				       "+15ML on Cocktails",
				       "reset ML on monsters",
				       "leave without using a turn" } ),

		// Choice 920 is Eraser
		new ChoiceAdventure(
			"Item-Driven", "choiceAdventure920", "Eraser",
			new Object[] { "reset Buff Jimmy quests",
				       "reset Taco Dan quests",
				       "reset Broden quests",
				       "don't use it" } ),

		// Choice 923 is All Over the Map
		new ChoiceAdventure(
			"Woods", "choiceAdventure923", "Black Forest",
			new Object[] { "fight blackberry bush or visit cobbler",
				       "visit blacksmith",
				       "visit black gold mine",
				       "visit black church" } ),

		// Choice 924 is You Found Your Thrill
		new ChoiceAdventure(
			"Woods", "choiceAdventure924", "Blackberry",
			new Object[] { "fight blackberry bush",
				       "visit cobbler" } ),

		// Choice 925 is The Blackest Smith
		new ChoiceAdventure(
			"Woods", "choiceAdventure925", "Blacksmith",
			new Object[] { new Option( "get black sword", 1 ),
				       new Option( "get black shield", 2 ),
				       new Option( "get black helmet", 3 ),
				       new Option( "get black greaves", 4 ),
				       new Option( "return to main choice", 6 ) } ),

		// Choice 926 is Be Mine
		new ChoiceAdventure(
			"Woods", "choiceAdventure926", "Black Gold Mine",
			new Object[] { new Option( "get black gold", 1 ),
				       new Option( "get Texas tea", 2 ),
				       new Option( "get Black Lung effect", 3 ),
				       new Option( "return to main choice", 6 ) } ),

		// Choice 927 is Sunday Black Sunday
		new ChoiceAdventure(
			"Woods", "choiceAdventure927", "Black Church",
			new Object[] { new Option( "get 13 turns of Salsa Satanica or beaten up", 1 ),
				       new Option( "get black kettle drum", 2 ),
				       new Option( "return to main choice", 6 ) } ),

		// Choice 928 is The Blackberry Cobbler
		new ChoiceAdventure(
			"Woods", "choiceAdventure928", "Blackberry Cobbler",
			new Object[] { new Option( "get blackberry slippers", 1 ),
				       new Option( "get blackberry moccasins", 2 ),
				       new Option( "get blackberry combat boots", 3 ),
				       new Option( "get blackberry galoshes", 4 ),
				       new Option( "return to main choice", 6 ) } ),

		// Choice 935 is Lost in Space... Ship
		// Choice 936 is The Nerve Center
		// Choice 937 is The Spacement
		// Choice 938 is The Ship's Kitchen
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
		ChoiceManager.action = PostChoiceAction.INITIALIZE;
		GenericRequest request = ChoiceManager.CHOICE_HANDLER;
		request.constructURLString( "choice.php" );
		request.run();
		RequestLogger.printLine( "Encounter: " + Preferences.getString( "lastEncounter" ) );
		ChoiceCommand.printChoices();
	}

	public static void ascendAfterChoice()
	{
		ChoiceManager.action = PostChoiceAction.ASCEND;
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

		if ( choice >= 904 && choice <= 913 )
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

		case 535:
			// Add "Go To Goal" button for a Safety Shelter Map
			SafetyShelterManager.addRonaldGoalButton( buffer );
			break;

		case 536:
			// Add "Go To Goal" button for a Safety Shelter Map
			SafetyShelterManager.addGrimaceGoalButton( buffer );
			break;

		case 537:
			// Play Porko!
		case 540:
			// Big-Time Generator
			SpaaaceRequest.decoratePorko( buffer );
			break;

		case 546:
			// Add "Go To Goal" button for Interview With You
			VampOutManager.addGoalButton( buffer );
			break;

		case 594:
			// Add "Go To Goal" button for a Lost Room
			LostKeyManager.addGoalButton( buffer );
			break;

		case 665:
			// Add "Solve" button for A Gracious Maze
			GameproManager.addGoalButton( buffer );
			break;

		case 703:
			// Load the options of the dreadscroll with the correct responses
			DreadScrollManager.decorate( buffer );
			break;

		case 872:
			ChoiceManager.decorateDrawnOnward( buffer );
			break;
		}
	}

	private static final Pattern PHOTO_PATTERN = Pattern.compile( "<select name=\"(.*?)\".*?</select>" );

	public static final void decorateDrawnOnward( final StringBuffer buffer )
	{
		Matcher matcher = ChoiceManager.PHOTO_PATTERN.matcher( buffer.toString() );
		while ( matcher.find() )
		{
			String photo = matcher.group( 1 );
			String find = matcher.group( 0 );
			String replace = null;
			if ( photo.equals( "photo1" ) )
			{
				if ( find.contains( "2259" ) )
				{
					replace = StringUtilities.singleStringReplace( find, "<option value=\"2259\">", "<option value=\"2259\" selected>" );
				}
			}
			else if ( photo.equals( "photo2" ) )
			{
				if ( find.contains( "7264" ) )
				{
					replace = StringUtilities.singleStringReplace( find, "<option value=\"7264\">", "<option value=\"7264\" selected>" );
				}
			}
			else if ( photo.equals( "photo3" ) )
			{
				if ( find.contains( "7263" ) )
				{
					replace = StringUtilities.singleStringReplace( find, "<option value=\"7263\">", "<option value=\"7263\" selected>" );
				}
			}
			else if ( photo.equals( "photo4" ) )
			{
				if ( find.contains( "7265" ) )
				{
					replace = StringUtilities.singleStringReplace( find, "<option value=\"7265\">", "<option value=\"7265\" selected>" );
				}
			}

			if ( replace != null )
			{
				StringUtilities.singleStringReplace( buffer, find, replace );
			}
		}
	}

	public static final Object[][] choiceSpoilers( final int choice )
	{
		Object[][] spoilers;

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

		// Nope. See if it's a Safety Shelter Map
		if ( choice == 535 || choice == 536 )
		{
			return null;
		}

		// Nope. See if it's Interview with you.
		if ( choice == 546 )
		{
			return null;
		}

		// See if it's A Lost Room
		if ( choice == 594 )
		{
			return null;
		}
		// See if this choice is controlled by user option
		for ( int i = 0; i < ChoiceManager.CHOICE_ADVS.length; ++i )
		{
			ChoiceAdventure choiceAdventure = ChoiceManager.CHOICE_ADVS[ i ];
			if ( choiceAdventure.getChoice() == choice )
			{
				return choiceAdventure.getSpoilers();
			}
		}

		// Nope. See if we know this choice
		for ( int i = 0; i < ChoiceManager.CHOICE_ADV_SPOILERS.length; ++i )
		{
			ChoiceAdventure choiceAdventure = ChoiceManager.CHOICE_ADV_SPOILERS[ i ];
			if ( choiceAdventure.getChoice() == choice )
			{
				return choiceAdventure.getSpoilers();
			}
		}

		// Unknown choice
		return null;
	}

	private static final Object[][] dynamicChoiceSpoilers( final int choice )
	{
		switch ( choice )
		{
		case 5:
			// Heart of Very, Very Dark Darkness
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Spooky Gravy Burrow" );

		case 7:
			// How Depressing
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Spooky Gravy Burrow" );

		case 85:
			// One NightStand (simple wooden)

			return ChoiceManager.dynamicChoiceSpoilers( choice, "Haunted Bedroom" );

		case 184:
			// That Explains All The Eyepatches

			return ChoiceManager.dynamicChoiceSpoilers( choice, "Barrrney's Barrr" );

		case 185:
			// Yes, You're a Rock Starrr
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Barrrney's Barrr" );

		case 187:
			// Arrr You Man Enough?
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Barrrney's Barrr" );

		case 188:
			// The Infiltrationist
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Orcish Frat House Blueprints" );

		case 272:
			// Marketplace Entrance
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Hobo Marketplace" );

		case 298:
			// In the Shade
			return ChoiceManager.dynamicChoiceSpoilers( choice, "An Octopus's Garden" );

		case 304:
			// A Vent Horizon
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Marinara Trench" );

		case 305:
			// There is Sauce at the Bottom of the Ocean
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Marinara Trench" );

		case 309:
			// Barback
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Dive Bar" );

		case 360:
			// Wumpus Hunt
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Jungles of Ancient Loathing" );

		case 410:
		case 411:
		case 412:
		case 413:
		case 414:
		case 415:
		case 416:
		case 417:
		case 418:
			// The Barracks
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Barracks" );

		case 442:
			// A Moment of Reflection
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Rabbit Hole" );

		case 522:
			// Welcome to the Footlocker
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Welcome to the Footlocker" );

		case 502:
			// Arboreal Respite
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Arboreal Respite" );

		case 579:
			// Such Great Heights
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Such Great Heights" );

		case 580:
			// The Hidden Heart of the Hidden Temple
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Hidden Heart of the Hidden Temple" );

		case 581:
			// Such Great Depths
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Such Great Depths" );

		case 582:
			// Fitting In
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Fitting In" );

		case 606:
			// Lost in the Great Overlook Lodge
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Lost in the Great Overlook Lodge" );

		case 611:
			// The Horror...(A-Boo Peak)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Horror..." );

		case 636:
		case 637:
		case 638:
		case 639:
			// Old Man psychoses
			return ChoiceManager.dynamicChoiceSpoilers( choice, "First Mate's Log Entry" );

		case 641:
			// Stupid Pipes. (Mystic's psychoses)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Stupid Pipes." );

		case 642:
			// You're Freaking Kidding Me (Mystic's psychoses)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "You're Freaking Kidding Me" );

		case 644:
			// Snakes. (Mystic's psychoses)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Snakes." );

		case 645:
			// So... Many... Skulls... (Mystic's psychoses)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "So... Many... Skulls..." );

		case 647:
			// A Stupid Dummy. Also, a Straw Man. (Mystic's psychoses)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "A Stupid Dummy. Also, a Straw Man." );

		case 648:
			// Slings and Arrows (Mystic's psychoses)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Slings and Arrows" );

		case 650:
			// This Is Your Life. Your Horrible, Horrible Life. (Mystic's psychoses)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "This Is Your Life. Your Horrible, Horrible Life." );

		case 651:
			// The Wall of Wailing (Mystic's psychoses)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Wall of Wailing" );

		case 669:
			// The Fast and the Furry-ous
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Fast and the Furry-ous" );

		case 670:
			// You Don't Mess Around with Gym
			return ChoiceManager.dynamicChoiceSpoilers( choice, "You Don't Mess Around with Gym" );

		case 678:
			// Yeah, You're for Me, Punk Rock Giant
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Yeah, You're for Me, Punk Rock Giant" );

		case 692:
			// I Wanna Be a Door
			return ChoiceManager.dynamicChoiceSpoilers( choice, "I Wanna Be a Door" );

		case 700:
			// Delirium in the Cafeterium
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Delirium in the Cafeterium" );
		
		case 721:
			// The Cabin in the Dreadsylvanian Woods
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Cabin in the Dreadsylvanian Woods" );

		case 722:
			// The Kitchen in the Woods
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Kitchen in the Woods" );

		case 723:
			// What Lies Beneath (the Cabin)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "What Lies Beneath (the Cabin)" );

		case 724:
			// Where it's Attic
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Where it's Attic" );

		case 725:
			// Tallest Tree in the Forest
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Tallest Tree in the Forest" );

		case 726:
			// Top of the Tree, Ma!
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Top of the Tree, Ma!" );

		case 727:
			// All Along the Watchtower
			return ChoiceManager.dynamicChoiceSpoilers( choice, "All Along the Watchtower" );

		case 728:
			// Treebasing
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Treebasing" );

		case 729:
			// Below the Roots
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Below the Roots" );

		case 730:
			// Hot Coals
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Hot Coals" );

		case 731:
			// The Heart of the Matter
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Heart of the Matter" );

		case 732:
			// Once Midden, Twice Shy
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Once Midden, Twice Shy" );

		case 733:
			// Dreadsylvanian Village Square
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Dreadsylvanian Village Square" );

		case 734:
			// Fright School
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Fright School" );

		case 735:
			// Smith, Black as Night
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Smith, Black as Night" );

		case 736:
			// Gallows
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Gallows" );

		case 737:
			// The Even More Dreadful Part of Town
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Even More Dreadful Part of Town" );

		case 738:
			// A Dreadful Smell
			return ChoiceManager.dynamicChoiceSpoilers( choice, "A Dreadful Smell" );

		case 739:
			// The Tinker's. Damn.
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Tinker's. Damn." );

		case 740:
			// Eight, Nine, Tenement
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Eight, Nine, Tenement" );

		case 741:
			// The Old Duke's Estate
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Old Duke's Estate" );

		case 742:
			// The Plot Thickens
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Plot Thickens" );

		case 743:
			// No Quarter
			return ChoiceManager.dynamicChoiceSpoilers( choice, "No Quarter" );

		case 744:
			// The Master Suite -- Sweet!
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Master Suite -- Sweet!" );

		case 745:
			// This Hall is Really Great
			return ChoiceManager.dynamicChoiceSpoilers( choice, "This Hall is Really Great" );

		case 746:
			// The Belle of the Ballroom
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Belle of the Ballroom" );

		case 747:
			// Cold Storage
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Cold Storage" );

		case 748:
			// Dining In (the Castle)
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Dining In (the Castle)" );

		case 749:
			// Tower Most Tall
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Tower Most Tall" );

		case 750:
			// Working in the Lab, Late One Night
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Working in the Lab, Late One Night" );

		case 751:
			// Among the Quaint and Curious Tomes.
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Among the Quaint and Curious Tomes." );

		case 752:
			// In The Boudoir
			return ChoiceManager.dynamicChoiceSpoilers( choice, "In The Boudoir" );

		case 753:
			// The Dreadsylvanian Dungeon
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Dreadsylvanian Dungeon" );

		case 754:
			// Live from Dungeon Prison
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Live from Dungeon Prison" );

		case 755:
			// The Hot Bowels
			return ChoiceManager.dynamicChoiceSpoilers( choice, "The Hot Bowels" );

		case 756:
			// Among the Fungus
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Among the Fungus" );

		case 758:
			// End of the Path
			return ChoiceManager.dynamicChoiceSpoilers( choice, "End of the Path" );

		case 759:
			// You're About to Fight City Hall
			return ChoiceManager.dynamicChoiceSpoilers( choice, "You're About to Fight City Hall" );

		case 760:
			// Holding Court
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Holding Court" );

		// Choice 761 is Staring Upwards...
		// Choice 762 is Try New Extra-Strength Anvil
		// Choice 764 is The Machine
		// Choice 765 is Hello Gallows

		case 772:
			// Saved by the Bell
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Saved by the Bell");
		
		case 780:
			// Action Elevator
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Action Elevator");
		
		case 781:
			// Earthbound and Down
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Earthbound and Down");
		
		case 783:
			// Water You Dune
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Water You Dune");
		
		case 784:
			// You, M. D.
			return ChoiceManager.dynamicChoiceSpoilers( choice, "You, M. D.");
		
		case 785:
			// Air Apparent
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Air Apparent");
		
		case 786:
			// Working Holiday
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Working Holiday");
		
		case 787:
			// Fire when Ready
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Fire when Ready");
			
		case 788:
			// Life is Like a Cherry of Bowls
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Life is Like a Cherry of Bowls");
			
		case 789:
			// Where Does The Lone Ranger Take His Garbagester?
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Where Does The Lone Ranger Take His Garbagester?");
			
		case 791:
			// Legend of the Temple in the Hidden City
			return ChoiceManager.dynamicChoiceSpoilers( choice, "Legend of the Temple in the Hidden City");

		case 801:
			// A Reanimated Conversation
			return ChoiceManager.dynamicChoiceSpoilers( choice, "A Reanimated Conversation" );
		}
			
		return null;
	}

	private static final Object[][] dynamicChoiceSpoilers( final int choice, final String name )
	{
		Object[][] result = new Object[ 3 ][];

		// The choice option is the first element
		result[ 0 ] = new String[ 1 ];
		result[ 0 ][ 0 ] = "choiceAdventure" + String.valueOf( choice );

		// The name of the choice is second element
		result[ 1 ] = new String[ 1 ];
		result[ 1 ][ 0 ] = name;

		// An array of choice spoilers is the third element
		result[ 2 ] = ChoiceManager.dynamicChoiceOptions( choice );

		return result;
	}

	private static final Object[] dynamicChoiceOptions( final int choice )
	{
		Object[] result;
		switch ( choice )
		{
		case 5:
			// Heart of Very, Very Dark Darkness
			result = new Object[ 2 ];

			boolean rock = InventoryManager.getCount( ItemPool.INEXPLICABLY_GLOWING_ROCK ) >= 1;

			result[ 0 ] = "You " + ( rock ? "" : "DON'T ") + " have an inexplicably glowing rock";
			result[ 1 ] = "skip adventure";

			return result;

		case 7:
			// How Depressing
			result = new Object[ 2 ];

			boolean glove = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.SPOOKY_GLOVE, 1 ) );

			result[ 0 ] = "spooky glove " + ( glove ? "" : "NOT ") + "equipped";
			result[ 1 ] = "skip adventure";

			return result;

		case 85:
			// One NightStand (simple wooden)
			result = new Object[ 4 ];

			boolean ballroom = Preferences.getInteger( "lastBallroomUnlock" ) == KoLCharacter.getAscensions();

			result[ 0 ] = ballroom ? "moxie " : "moxie and ballroom key step 1";
			result[ 1 ] = (ballroom && !KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) ? "ballroom key step 2" : "nothing");
			result[ 2 ] = "enter combat";
			result[ 3 ] = new Option( "Engorged Sausages and You", "Engorged Sausages and You" );

			return result;

		case 184:
			// That Explains All The Eyepatches
			result = new Object[ 6 ];

			// The choices are based on character class.
			// Mus: combat, shot of rotgut (2948), drunkenness
			// Mys: drunkenness, shot of rotgut (2948), shot of rotgut (2948)
			// Mox: combat, drunkenness, shot of rotgut (2948)

			result[ 0 ] = KoLCharacter.isMysticalityClass() ? "3 drunk and stats (varies by class)" : "enter combat (varies by class)";
			result[ 1 ] = KoLCharacter.isMoxieClass() ?
				"3 drunk and stats (varies by class)" :
				new Option( "shot of rotgut (varies by class)", "shot of rotgut" );
			result[ 2 ] = KoLCharacter.isMuscleClass() ?
				"3 drunk and stats (varies by class)" :
				new Option( "shot of rotgut (varies by class)", "shot of rotgut" );
			result[ 3 ] = "always 3 drunk & stats";
			result[ 4 ] = "always shot of rotgut";
			result[ 5 ] = "combat (or rotgut if Myst class)";
			return result;

		case 185:
			// Yes, You're a Rock Starrr
			result = new Object[ 3 ];

			int drunk = KoLCharacter.getInebriety();

			// 0 drunk: base booze, mixed booze, fight
			// More than 0 drunk: base booze, mixed booze, stats

			result[ 0 ] = "base booze";
			result[ 1 ] = "mixed booze";
			result[ 2 ] = drunk == 0 ? "combat" : "stats";
			return result;

		case 187:
			// Arrr You Man Enough?

			result = new Object[ 2 ];
			float odds = BeerPongRequest.pirateInsultOdds() * 100.0f;

			result[ 0 ] = KoLConstants.FLOAT_FORMAT.format( odds ) + "% chance of winning";
			result[ 1 ] = odds == 100.0f ? "Oh come on. Do it!" : "Try later";
			return result;

		case 188:
			// The Infiltrationist
			result = new Object[ 3 ];

			// Attempt a frontal assault
			boolean ok1 = EquipmentManager.isWearingOutfit( OutfitPool.FRAT_OUTFIT );
			result[ 0 ] = "Frat Boy Ensemble (" + ( ok1 ? "" : "NOT " ) + "equipped)";

			// Go in through the side door
			boolean ok2a = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.MULLET_WIG, 1 ) );
			boolean ok2b = InventoryManager.getCount( ItemPool.BRIEFCASE ) >= 1;
			result[ 1 ] = "mullet wig (" + ( ok2a ? "" : "NOT " ) + "equipped) + briefcase (" + ( ok2b ? "OK)" : "0 in inventory)" );

			// Catburgle
			boolean ok3a = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.FRILLY_SKIRT, 1 ) );
			int wings = InventoryManager.getCount( ItemPool.HOT_WING );
			result[ 2 ] = "frilly skirt (" + ( ok3a ? "" : "NOT " ) + "equipped) + 3 hot wings (" + wings + " in inventory)";

			return result;

		case 191:
			// Chatterboxing
			result = new Object[ 4 ];

			int trinks = InventoryManager.getCount( ItemPool.VALUABLE_TRINKET );
			result[ 0 ] = "moxie substats";
			result[ 1 ] = trinks == 0 ? "lose hp (no valuable trinkets)" :
				"use valuable trinket to banish (" + trinks + " in inventory)";
			result[ 2 ] = "muscle substats";
			result[ 3 ] = "mysticality substats";

			return result;

		case 272:
			// Marketplace Entrance
			result = new Object[ 2 ];

			int nickels = InventoryManager.getCount( ItemPool.HOBO_NICKEL );
			boolean binder = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.HOBO_CODE_BINDER, 1 ) );

			result[ 0 ] = nickels + " nickels, " + ( binder ? "" : "NO ") + " hobo code binder equipped";
			result[ 1 ] = "skip adventure";

			return result;

		case 298:
			// In the Shade
			result = new Object[ 2 ];

			int seeds = InventoryManager.getCount( ItemPool.SEED_PACKET );
			int slime = InventoryManager.getCount( ItemPool.GREEN_SLIME );

			result[ 0 ] = seeds + " seed packets, " + slime + " globs of green slime";
			result[ 1 ] = "skip adventure";

			return result;

		case 304:
			// A Vent Horizon
			result = new Object[ 2 ];

			int summons = 3 - Preferences.getInteger( "tempuraSummons" );

			result[ 0 ] = summons + " summons left today";
			result[ 1 ] = "skip adventure";

			return result;

		case 305:
			// There is Sauce at the Bottom of the Ocean
			result = new Object[ 2 ];

			int globes = InventoryManager.getCount( ItemPool.MERKIN_PRESSUREGLOBE );

			result[ 0 ] = globes + " Mer-kin pressureglobes";
			result[ 1 ] = "skip adventure";

			return result;

		case 309:
			// Barback
			result = new Object[ 2 ];

			int seaodes = 3 - Preferences.getInteger( "seaodesFound" );

			result[ 0 ] = seaodes + " more seodes available today";
			result[ 1 ] = "skip adventure";

			return result;

		case 360:
			// Wumpus Hunt
			return WumpusManager.dynamicChoiceOptions( ChoiceManager.lastResponseText );

		case 410:
		case 411:
		case 412:
		case 413:
		case 414:
		case 415:
		case 416:
		case 417:
		case 418:
			// The Barracks
			result = HaciendaManager.getSpoilers( choice );
			return result;
			
		case 442:
			// A Moment of Reflection
			result = new Object[ 6 ];
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
			result[ 0 ] = "Seal Clubber/Pastamancer item, or yellow matter custard";
			result[ 1 ] = "Sauceror/Accordion Thief item, or delicious comfit?";
			result[ 2 ] = "Disco Bandit/Turtle Tamer item, or fight croqueteer";
			result[ 3 ] = "you have " + count + "/5 of the items needed for an ittah bittah hookah";
			result[ 4 ] = "get a chess cookie";
			result[ 5 ] = "skip adventure";
			return result;

		case 502:
			// Arboreal Respite
			result = new Object[ 3 ];

			// meet the vampire hunter, trade bar skins or gain a spooky sapling
			int stakes = InventoryManager.getCount( ItemPool.WOODEN_STAKES );
			int hearts = InventoryManager.getCount( ItemPool.VAMPIRE_HEART );
			String hunterAction = ( stakes > 0 ? "and get wooden stakes" : "and trade " + hearts + " hearts" );

			int barskins = InventoryManager.getCount( ItemPool.BAR_SKIN );
			int saplings = InventoryManager.getCount( ItemPool.SPOOKY_SAPLING );

			result[ 0 ] = "gain some meat, meet the vampire hunter " + hunterAction + ", sell bar skins (" + barskins  + ") or buy a spooky sapling (" + saplings + ")";

			// gain mosquito larva, gain quest coin or gain a vampire heart
			boolean haveMap = InventoryManager.getCount( ItemPool.SPOOKY_MAP ) > 0;
			boolean haveCoin = InventoryManager.getCount( ItemPool.TREE_HOLED_COIN ) > 0;
			boolean getCoin = ( !haveCoin && !haveMap && !KoLCharacter.getTempleUnlocked() );
			String coinAction = ( getCoin ? "gain quest coin" : "skip adventure" );

			result[ 1 ] = "gain mosquito larva or spooky mushrooms, " + coinAction + ", get stats or fight a vampire";

			// gain a starter item, gain Spooky-Gro fertilizer or gain spooky temple map
			int fertilizer = InventoryManager.getCount( ItemPool.SPOOKY_FERTILIZER );
			String mapAction = ( haveCoin ? ", gain spooky temple map" : "" );

			result[ 2 ] = "gain a starter item, gain Spooky-Gro fertilizer (" + fertilizer + ")" + mapAction;

			return result;

		case 522:
			// Welcome to the Footlocker
			result = new Object[ 2 ];

			boolean havePolearm = ( InventoryManager.getCount( ItemPool.KNOB_GOBLIN_POLEARM ) > 0 ||
			                        InventoryManager.getEquippedCount( ItemPool.KNOB_GOBLIN_POLEARM ) > 0 );
			boolean havePants = ( InventoryManager.getCount( ItemPool.KNOB_GOBLIN_PANTS ) > 0 ||
			                      InventoryManager.getEquippedCount( ItemPool.KNOB_GOBLIN_PANTS ) > 0 );
			boolean haveHelm = ( InventoryManager.getCount( ItemPool.KNOB_GOBLIN_HELM ) > 0 ||
			                     InventoryManager.getEquippedCount( ItemPool.KNOB_GOBLIN_HELM ) > 0 );

			result[ 0 ] =
				!havePolearm ? new Option( "knob goblin elite polearm", "knob goblin elite polearm" ) :
				!havePants ? new Option( "knob goblin elite pants", "knob goblin elite pants" ) :
				!haveHelm ? new Option( "knob goblin elite helm", "knob goblin elite helm" ) :
				new Option( "knob jelly donut", "knob jelly donut" );
			result[ 1 ] = "skip adventure";
			return result;

		case 579:
			// Such Great Heights
			result = new Object[ 3 ];

			boolean haveNostril = ( InventoryManager.getCount( ItemPool.NOSTRIL_OF_THE_SERPENT ) > 0 );
			boolean gainNostril = ( !haveNostril && Preferences.getInteger( "lastTempleButtonsUnlock" ) != KoLCharacter.getAscensions() );
			boolean templeAdvs = ( Preferences.getInteger( "lastTempleAdventures" ) == KoLCharacter.getAscensions() );

			result[ 0 ] = "mysticality substats";
			result[ 1 ] = ( gainNostril ? "gain the Nostril of the Serpent" : "skip adventure" );
			result[ 2 ] = ( templeAdvs ? "skip adventure" : "gain 3 adventures" );
			return result;

		case 580:
			// The Hidden Heart of the Hidden Temple
			result = new Object[ 3 ];

			haveNostril = ( InventoryManager.getCount( ItemPool.NOSTRIL_OF_THE_SERPENT ) > 0 );
			boolean buttonsUnconfused = ( Preferences.getInteger( "lastTempleButtonsUnlock" ) == KoLCharacter.getAscensions() );
			
			if ( ChoiceManager.lastResponseText.contains( "door_stone.gif" ) )
			{
				result[ 0 ] = "muscle substats";
				result[ 1 ] = ( buttonsUnconfused || haveNostril ? "choose Hidden Heart adventure" : "randomise Hidden Heart adventure" );
				result[ 2 ] = "moxie substats and 5 turns of Somewhat poisoned";
			}
			else if ( ChoiceManager.lastResponseText.contains( "door_sun.gif" ) )
			{
				result[ 0 ] = "gain ancient calendar fragment";
				result[ 1 ] = ( buttonsUnconfused || haveNostril ? "choose Hidden Heart adventure" : "randomise Hidden Heart adventure" );
				result[ 2 ] = "moxie substats and 5 turns of Somewhat poisoned";
			}
			else if ( ChoiceManager.lastResponseText.contains( "door_gargoyle.gif" ) )
			{
				result[ 0 ] = "gain mana";
				result[ 1 ] = ( buttonsUnconfused || haveNostril ? "choose Hidden Heart adventure" : "randomise Hidden Heart adventure" );
				result[ 2 ] = "moxie substats and 5 turns of Somewhat poisoned";
			}
			else if ( ChoiceManager.lastResponseText.contains( "door_pikachu.gif" ) )
			{
				result[ 0 ] = "unlock Hidden City";
				result[ 1 ] = ( buttonsUnconfused || haveNostril ? "choose Hidden Heart adventure" : "randomise Hidden Heart adventure" );
				result[ 2 ] = "moxie substats and 5 turns of Somewhat poisoned";
			}
			
			return result;

		case 581:
			// Such Great Depths
			result = new Object[ 3 ];

			int fungus = InventoryManager.getCount( ItemPool.GLOWING_FUNGUS );

			result[ 0 ] = "gain a glowing fungus (" + fungus + ")";
			result[ 1 ] = ( Preferences.getBoolean( "_templeHiddenPower" ) ? "skip adventure" : "5 advs of +15 mus/mys/mox" );
			result[ 2 ] = "fight clan of cave bars";
			return result;

		case 582:
			// Fitting In
			result = new Object[ 3 ];

			// mysticality substats, gain the Nostril of the Serpent or gain 3 adventures
			haveNostril = ( InventoryManager.getCount( ItemPool.NOSTRIL_OF_THE_SERPENT ) > 0 );
			gainNostril = ( !haveNostril && Preferences.getInteger( "lastTempleButtonsUnlock" ) != KoLCharacter.getAscensions() );
			String nostrilAction = ( gainNostril ? "gain the Nostril of the Serpent" : "skip adventure" );

			templeAdvs = ( Preferences.getInteger( "lastTempleAdventures" ) == KoLCharacter.getAscensions() );
			String advAction = ( templeAdvs ? "skip adventure" : "gain 3 adventures" );

			result[ 0 ] = "mysticality substats, " + nostrilAction + " or " + advAction;

			// Hidden Heart of the Hidden Temple
			result[ 1 ] = "Hidden Heart of the Hidden Temple";

			// gain glowing fungus, gain Hidden Power or fight a clan of cave bars
			String powerAction = ( Preferences.getBoolean( "_templeHiddenPower" ) ? "skip adventure" : "Hidden Power" );

			result[ 2 ] = "gain a glowing fungus, " + powerAction + " or fight a clan of cave bars";

			return result;

		case 606:
			// Lost in the Great Overlook Lodge
			result = new Object[ 6 ];

			result[ 0 ] = "need +4 stench resist, have " + KoLCharacter.getElementalResistanceLevels( Element.STENCH );

			// annoyingly, the item drop check does not take into account fairy (or other sidekick) bonus.
			// This is just a one-off implementation, but should be standardized somewhere in Modifiers
			// if kol adds more things like this.
			double bonus = 0;
			// Check for familiars
			if ( !KoLCharacter.getFamiliar().equals( FamiliarData.NO_FAMILIAR ) )
			{
				bonus = Modifiers.getNumericModifier( KoLCharacter.getFamiliar(), "Item Drop" );
			}
			// Check for Clancy
			else if ( KoLCharacter.getCurrentInstrument() != null &&
				KoLCharacter.getCurrentInstrument().equals( CharPaneRequest.LUTE ) )
			{
				int weight = 5 * KoLCharacter.getMinstrelLevel();
				bonus = Math.sqrt( 55 * weight ) + weight - 3;
			}
			// Check for Eggman
			else if ( KoLCharacter.getCompanion() == Companion.EGGMAN )
			{
				bonus = KoLCharacter.hasSkill( "Working Lunch" ) ? 75 : 50;
			}

			result[ 1 ] =
				"need +50% item drop, have " + Math.round( KoLCharacter.getItemDropPercentAdjustment() +
				KoLCharacter.currentNumericModifier( Modifiers.FOODDROP ) - bonus ) + "%";
			result[ 2 ] = new Option( "need jar of oil", "jar of oil" );
			result[ 3 ] = "need +40% init, have " + KoLCharacter.getInitiativeAdjustment() + "%";
			result[ 4 ] = null; //why is there a missing button 5?
			result[ 5 ] = "flee";

			return result;

		case 611:
			// The Horror... (A-Boo Peak)
			result = new Object[ 2 ];
			result[ 0 ] = ChoiceManager.booPeakDamage();
			result[ 1 ] = "Flee";
			return result;

		case 636:
		case 637:
		case 638:
		case 639:
			// Old Man psychosis choice adventures are randomized and may not include all elements.
			return oldManPsychosisSpoilers();

		case 641:
			// Stupid Pipes. (Mystic's psychoses)
			result = new Object[ 3 ];
			{
				StringBuilder buffer = new StringBuilder();
				int resistance = KoLCharacter.getElementalResistanceLevels( Element.HOT );
				int damage = (int)( 2.50 * (100.0 - KoLCharacter.elementalResistanceByLevel( resistance ) ) );
				int hp = KoLCharacter.getCurrentHP();
				buffer.append( "take " );
				buffer.append( String.valueOf( damage ) );
				buffer.append( " hot damage, current HP = " );
				buffer.append( String.valueOf( hp ) );
				buffer.append( ", current hot resistance = " );
				buffer.append( String.valueOf( resistance ) );
				result[ 0 ] = buffer.toString();
			}
			result[ 1 ] = "flickering pixel";
			result[ 2 ] = "skip adventure";
			return result;

		case 642:
			// You're Freaking Kidding Me (Mystic's psychoses)
			result = new Object[ 3 ];
			{
				StringBuilder buffer = new StringBuilder();
				buffer.append( "50 buffed Muscle/Mysticality/Moxie required, have " );
				buffer.append( String.valueOf( KoLCharacter.getAdjustedMuscle() ) );
				buffer.append( "/" );
				buffer.append( String.valueOf( KoLCharacter.getAdjustedMysticality() ) );
				buffer.append( "/" );
				buffer.append( String.valueOf( KoLCharacter.getAdjustedMoxie() ) );
				result[ 0 ] = buffer.toString();
			}
			result[ 1 ] = "flickering pixel";
			result[ 2 ] = "skip adventure";
			return result;

		case 644:
			// Snakes. (Mystic's psychoses)
			result = new Object[ 3 ];
			{
				StringBuilder buffer = new StringBuilder();
				buffer.append( "50 buffed Moxie required, have " );
				buffer.append( String.valueOf( KoLCharacter.getAdjustedMoxie() ) );
				result[ 0 ] = buffer.toString();
			}
			result[ 1 ] = "flickering pixel";
			result[ 2 ] = "skip adventure";
			return result;

		case 645:
			// So... Many... Skulls... (Mystic's psychoses)
			result = new Object[ 3 ];
			{
				StringBuilder buffer = new StringBuilder();
				int resistance = KoLCharacter.getElementalResistanceLevels( Element.SPOOKY );
				int damage = (int)( 2.50 * (100.0 - KoLCharacter.elementalResistanceByLevel( resistance ) ) );
				int hp = KoLCharacter.getCurrentHP();
				buffer.append( "take " );
				buffer.append( String.valueOf( damage ) );
				buffer.append( " spooky damage, current HP = " );
				buffer.append( String.valueOf( hp ) );
				buffer.append( ", current spooky resistance = " );
				buffer.append( String.valueOf( resistance ) );
				result[ 0 ] = buffer.toString();
			}
			result[ 1 ] = "flickering pixel";
			result[ 2 ] = "skip adventure";
			return result;

		case 647:
			// A Stupid Dummy. Also, a Straw Man. (Mystic's psychoses)
			result = new Object[ 3 ];
			{
				StringBuilder buffer = new StringBuilder();
				String current = String.valueOf( KoLCharacter.currentBonusDamage() );
				buffer.append( "100 weapon damage required" );
				result[ 0 ] = buffer.toString();
			}
			result[ 1 ] = "flickering pixel";
			result[ 2 ] = "skip adventure";
			return result;

		case 648:
			// Slings and Arrows (Mystic's psychoses)
			result = new Object[ 3 ];
			{
				StringBuilder buffer = new StringBuilder();
				buffer.append( "101 HP required, have " );
				buffer.append( String.valueOf( KoLCharacter.getCurrentHP() ) );
				result[ 0 ] = buffer.toString();
			}
			result[ 1 ] = "flickering pixel";
			result[ 2 ] = "skip adventure";
			return result;

		case 650:
			// This Is Your Life. Your Horrible, Horrible Life. (Mystic's psychoses)
			result = new Object[ 3 ];
			{
				StringBuilder buffer = new StringBuilder();
				buffer.append( "101 MP required, have " );
				buffer.append( String.valueOf( KoLCharacter.getCurrentMP() ) );
				result[ 0 ] = buffer.toString();
			}
			result[ 1 ] = "flickering pixel";
			result[ 2 ] = "skip adventure";
			return result;

		case 651:
			// The Wall of Wailing (Mystic's psychoses)
			result = new Object[ 3 ];
			{
				StringBuilder buffer = new StringBuilder();
				buffer.append( "10 prismatic damage required, have " );
				buffer.append( String.valueOf( KoLCharacter.currentPrismaticDamage() ) );
				result[ 0 ] = buffer.toString();
			}
			result[ 1 ] = "flickering pixel";
			result[ 2 ] = "skip adventure";
			return result;

		case 669:
			// The Fast and the Furry-ous
			result = new Object[ 4 ];
			result[ 0 ] =
				KoLCharacter.hasEquipped( ItemPool.get( ItemPool.TITANIUM_UMBRELLA, 1 ) ) ?
				"open Ground Floor (titanium umbrella equipped)" :
				"Neckbeard Choice (titanium umbrella not equipped)";
			result[ 1 ] = "200 Moxie substats";
			result[ 2 ] = "";
			result[ 3 ] = "skip adventure and guarantees this adventure will reoccur";
			return result;

		case 670:
			// You Don't Mess Around with Gym
			result = new Object[ 5 ];
			result[ 0 ] = "massive dumbbell, then skip adventure";
			result[ 1 ] = "200 Muscle substats";
			result[ 2 ] = "pec oil, giant jar of protein powder, Squat-Thrust Magazine";
			result[ 3 ] =
				KoLCharacter.hasEquipped( ItemPool.get( ItemPool.EXTREME_AMULET, 1 ) ) ?
				"open Ground Floor (amulet equipped)" :
				"skip adventure (amulet not equipped)";
			result[ 4 ] = "skip adventure and guarantees this adventure will reoccur";
			return result;

		case 678:
			// Yeah, You're for Me, Punk Rock Giant
			result = new Object[ 4 ];
			result[ 0 ] =
				KoLCharacter.hasEquipped( ItemPool.get( ItemPool.MOHAWK_WIG, 1 ) ) ?
				"Finish quest (mohawk wig equipped)" :
				"Fight Punk Rock Giant (mohawk wig not equipped)";
			result[ 1 ] = "500 meat";
			result[ 2 ] = "Steampunk Choice";
			result[ 3 ] = "Raver Choice";
			return result;

		case 692:
			// I Wanna Be a Door
			result = new String[ 9 ];
			result[ 0 ] = "suffer trap effects";
			result[ 1 ] = "unlock door with key, no turn spent";
			result[ 2 ] = "pick lock with lockpicks, no turn spent";
			result[ 3 ] = KoLCharacter.getAdjustedMuscle() >= 30 ? "bypass trap with muscle" : "suffer trap effects";
			result[ 4 ] = KoLCharacter.getAdjustedMysticality() >= 30 ? "bypass trap with mysticality" : "suffer trap effects";
			result[ 5 ] = KoLCharacter.getAdjustedMoxie() >= 30 ? "bypass trap with moxie" : "suffer trap effects";
			result[ 6 ] = "open door with card, no turn spent";
			result[ 7 ] = "leave, no turn spent";
			return result;
			
		case 700:
			// Delirium in the Cafeteria
			result = new String[ 9 ];
			result[ 0 ] = KoLConstants.activeEffects.contains( ChoiceManager.JOCK_EFFECT ) ? "Gain stats" : "Lose HP";
			result[ 1 ] = KoLConstants.activeEffects.contains( ChoiceManager.NERD_EFFECT ) ? "Gain stats" : "Lose HP";
			result[ 2 ] = KoLConstants.activeEffects.contains( ChoiceManager.GREASER_EFFECT ) ? "Gain stats" : "Lose HP";
			return result;
			
		case 721:
		{
			// The Cabin in the Dreadsylvanian Woods

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			buffer.append( "dread tarragon" );
			if ( KoLCharacter.isMuscleClass() )
			{
				buffer.append( ", old dry bone (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.OLD_DRY_BONE ) ) );
				buffer.append( ") -> bone flour" );
			}
			buffer.append( ", -stench" );
			result[ 0 ] = buffer.toString();	// The Kitchen

			buffer.setLength( 0 );
			buffer.append( "Freddies" );
			buffer.append( ", Bored Stiff (+100 spooky damage)" );
			buffer.append( ", replica key (" );
			buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.REPLICA_KEY ) ) );
			buffer.append( ") -> Dreadsylvanian auditor's badge" );
			buffer.append( ", wax banana (" );
			buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.WAX_BANANA ) ) );
			buffer.append( ") -> complicated lock impression" );
			result[ 1 ] = buffer.toString();	// The Cellar

			buffer.setLength( 0 );
			ChoiceManager.lockSpoiler( buffer );
			buffer.append( "-spooky" );
			if ( KoLCharacter.getClassType() == KoLCharacter.ACCORDION_THIEF )
			{
				buffer.append( " + intricate music box parts" );
			}
			buffer.append( ", fewer werewolves" );
			buffer.append( ", fewer vampires" );
			buffer.append( ", +Moxie" );
			result[ 2 ] = buffer.toString();	// The Attic (locked)

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil1" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 722:
			// The Kitchen in the Woods
			result = new Object[ 6 ];
			result[ 0 ] = "dread tarragon";
			result[ 1 ] = "old dry bone (" + String.valueOf( InventoryManager.getCount( ItemPool.OLD_DRY_BONE ) ) + ") -> bone flour";
			result[ 2 ] = "-stench";
			result[ 5 ] = "Return to The Cabin";
			return result;

		case 723:
			// What Lies Beneath (the Cabin)
			result = new Object[ 6 ];
			result[ 0 ] = "Freddies";
			result[ 1 ] = "Bored Stiff (+100 spooky damage)";
			result[ 2 ] = "replica key (" + String.valueOf( InventoryManager.getCount( ItemPool.REPLICA_KEY ) ) + ") -> Dreadsylvanian auditor's badge";
			result[ 3 ] = "wax banana (" + String.valueOf( InventoryManager.getCount( ItemPool.WAX_BANANA ) ) + ") -> complicated lock impression";
			result[ 5 ] = "Return to The Cabin";
			return result;

		case 724:
			// Where it's Attic
			result = new Object[ 6 ];
			result[ 0 ] = "-spooky" + ( KoLCharacter.getClassType() == KoLCharacter.ACCORDION_THIEF  ? " + intricate music box parts" : "" );
			result[ 1 ] = "fewer werewolves";
			result[ 2 ] = "fewer vampires";
			result[ 3 ] = "+Moxie";
			result[ 5 ] = "Return to The Cabin";
			return result;

		case 725:
		{
			// Tallest Tree in the Forest

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			if ( KoLCharacter.isMuscleClass() )
			{
				buffer.append( "drop blood kiwi" );
				buffer.append( ", -sleaze" );
				buffer.append( ", moon-amber" );
			}
			else
			{
				buffer.append( "unavailable (Muscle class only)" );
			}
			result[ 0 ] = buffer.toString();	// Climb tree (muscle only)

			buffer.setLength( 0 );
			ChoiceManager.lockSpoiler( buffer );
			buffer.append( "fewer ghosts" );
			buffer.append( ", Freddies" );
			buffer.append( ", +Muscle" );
			result[ 1 ] = buffer.toString();	// Fire Tower (locked)

			buffer.setLength( 0 );
			buffer.append( "blood kiwi (from above)" );
			buffer.append( ", Dreadsylvanian seed pod" );
			if ( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.FOLDER_HOLDER, 1 ) ) )
			{
				buffer.append( ", folder (owl)" );
			}

			result[ 2 ] = buffer.toString();	// Base of tree

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil2" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 726:
			// Top of the Tree, Ma!
			result = new Object[ 6 ];
			result[ 0 ] = "drop blood kiwi";
			result[ 1 ] = "-sleaze";
			result[ 2 ] = "moon-amber";
			result[ 5 ] = "Return to The Tallest Tree";
			return result;

		case 727:
			// All Along the Watchtower
			result = new Object[ 6 ];
			result[ 0 ] = "fewer ghosts";
			result[ 1 ] = "Freddies";
			result[ 2 ] = "+Muscle";
			result[ 5 ] = "Return to The Tallest Tree";
			return result;

		case 728:
			// Treebasing
			result = new Object[ 6 ];
			result[ 0 ] = "blood kiwi (from above)";
			result[ 1 ] = "Dreadsylvanian seed pod";
			result[ 2 ] = "folder (owl)";
			result[ 5 ] = "Return to The Tallest Tree";
			return result;

		case 729:
		{
			// Below the Roots

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			buffer.append( "-hot" );
			buffer.append( ", Dragged Through the Coals (+100 hot damage)" );
			buffer.append( ", old ball and chain (" );
			buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.OLD_BALL_AND_CHAIN ) ) );
			buffer.append( ") -> cool iron ingot" );
			result[ 0 ] = buffer.toString();	// Hot

			buffer.setLength( 0 );
			buffer.append( "-cold" );
			buffer.append( ", +Mysticality" );
			buffer.append( ", Nature's Bounty (+300 max HP)" );
			result[ 1 ] = buffer.toString();	// Cold

			buffer.setLength( 0 );
			buffer.append( "fewer bugbears" );
			buffer.append( ", Freddies" );
			result[ 2 ] = buffer.toString();	// Smelly

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil3" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 730:
			// Hot Coals
			result = new Object[ 6 ];
			result[ 0 ] = "-hot";
			result[ 1 ] = "Dragged Through the Coals (+100 hot damage)";
			result[ 2 ] = "old ball and chain (" + String.valueOf( InventoryManager.getCount( ItemPool.OLD_BALL_AND_CHAIN ) ) + ") -> cool iron ingot";
			result[ 5 ] = "Return to The Burrows";
			return result;

		case 731:
			// The Heart of the Matter
			result = new Object[ 6 ];
			result[ 0 ] = "-cold";
			result[ 1 ] = "+Mysticality";
			result[ 2 ] = "Nature's Bounty (+300 max HP)";
			result[ 5 ] = "Return to The Burrows";
			return result;

		case 732:
			// Once Midden, Twice Shy
			result = new Object[ 6 ];
			result[ 0 ] = "fewer bugbears";
			result[ 1 ] = "Freddies";
			result[ 5 ] = "Return to The Burrows";
			return result;

		case 733:
		{
			// Dreadsylvanian Village Square

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			ChoiceManager.lockSpoiler( buffer );
			buffer.append( "fewer ghosts" );
			buffer.append( ", ghost pencil" );
			buffer.append( ", +Mysticality" );
			result[ 0 ] = buffer.toString();	// Schoolhouse (locked)

			buffer.setLength( 0 );
			buffer.append( "-cold" );
			buffer.append( ", Freddies" );
			if ( InventoryManager.getCount( ItemPool.HOTHAMMER ) > 0 )
			{
				buffer.append( ", cool iron ingot (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.COOL_IRON_INGOT ) ) );
				buffer.append( ") + warm fur (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.WARM_FUR ) ) );
				buffer.append( ") -> cooling iron equipment" );
			}
			result[ 1 ] = buffer.toString();	// Blacksmith

			buffer.setLength( 0 );
			buffer.append( "-spooky" );
			buffer.append( ", gain " );
			String item =
				KoLCharacter.isMuscleClass() ? "hangman's hood" :
				KoLCharacter.isMysticalityClass() ? "cursed ring finger ring" :
				KoLCharacter.isMoxieClass() ? "Dreadsylvanian clockwork key" :
				"nothing";
			buffer.append( item );
			buffer.append( " with help of clannie" );
			buffer.append( " or help clannie gain an item" );
			result[ 2 ] = buffer.toString();	// Gallows

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil4" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 734:
			// Fright School
			result = new Object[ 6 ];
			result[ 0 ] = "fewer ghosts";
			result[ 1 ] = "ghost pencil";
			result[ 2 ] = "+Mysticality";
			result[ 5 ] = "Return to The Village Square";
			return result;

		case 735:
			// Smith, Black as Night
			result = new Object[ 6 ];
			result[ 0 ] = "-cold";
			result[ 1 ] = "Freddies";
			result[ 2 ] =
				"cool iron ingot (" +
				String.valueOf( InventoryManager.getCount( ItemPool.COOL_IRON_INGOT ) ) +
				") + warm fur (" +
				String.valueOf( InventoryManager.getCount( ItemPool.WARM_FUR ) ) +
				") -> cooling iron equipment";
			result[ 5 ] = "Return to The Village Square";
			return result;

		case 736:
			// Gallows
			result = new Object[ 6 ];
			result[ 0 ] = "-spooky ";
			result[ 1 ] =
				"gain " +
				( KoLCharacter.isMuscleClass() ? "hangman's hood" :
				  KoLCharacter.isMysticalityClass() ? "cursed ring finger ring" :
				  KoLCharacter.isMoxieClass() ? "Dreadsylvanian clockwork key" :
				  "nothing" ) +
				" with help of clannie";
			result[ 3 ] = "help clannie gain an item";
			result[ 5 ] = "Return to The Village Square";
			return result;

		case 737:
		{
			// The Even More Dreadful Part of Town

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			buffer.append( "-stench" );
			buffer.append( ", Sewer-Drenched (+100 stench damage)" );
			result[ 0 ] = buffer.toString();	// Sewers

			buffer.setLength( 0 );
			buffer.append( "fewer skeletons" );
			buffer.append( ", -sleaze" );
			buffer.append( ", +Muscle" );
			result[ 1 ] = buffer.toString();	// Tenement

			buffer.setLength( 0 );
			if ( KoLCharacter.isMoxieClass() )
			{
				buffer.append( "Freddies" );
				buffer.append( ", lock impression (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.WAX_LOCK_IMPRESSION ) ) );
				buffer.append( ") + music box parts (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.INTRICATE_MUSIC_BOX_PARTS ) ) );
				buffer.append( ") -> replica key" );
				buffer.append( ", moon-amber (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.MOON_AMBER ) ) );
				buffer.append( ") -> polished moon-amber" );
				buffer.append( ", 3 music box parts (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.INTRICATE_MUSIC_BOX_PARTS ) ) );
				buffer.append( ") + clockwork key (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY ) ) );
				buffer.append( ") -> mechanical songbird" );
				buffer.append( ", 3 lengths of old fuse" );
			}
			else
			{
				buffer.append( "unavailable (Moxie class only)" );
			}
			result[ 2 ] = buffer.toString();	// Ticking Shack (moxie only)

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil5" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 738:
			// A Dreadful Smell
			result = new Object[ 6 ];
			result[ 0 ] = "-stench";
			result[ 1 ] = "Sewer-Drenched (+100 stench damage)";
			result[ 5 ] = "Return to Skid Row";
			return result;

		case 739:
			// The Tinker's. Damn.
			result = new Object[ 6 ];
			result[ 0 ] = "Freddies";
			result[ 1 ] =
				"lock impression (" +
				String.valueOf( InventoryManager.getCount( ItemPool.WAX_LOCK_IMPRESSION ) ) +
				") + music box parts (" +
				String.valueOf( InventoryManager.getCount( ItemPool.INTRICATE_MUSIC_BOX_PARTS ) ) +
				") -> replica key";
			result[ 2 ] =
				"moon-amber (" +
				String.valueOf( InventoryManager.getCount( ItemPool.MOON_AMBER ) ) +
				") -> polished moon-amber";
			result[ 3 ] =
				"3 music box parts (" +
				String.valueOf( InventoryManager.getCount( ItemPool.INTRICATE_MUSIC_BOX_PARTS ) ) +
				") + clockwork key (" +
				String.valueOf( InventoryManager.getCount( ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY ) ) +
				") -> mechanical songbird";
			result[ 4 ] = "3 lengths of old fuse";
			result[ 5 ] = "Return to Skid Row";
			return result;

		case 740:
			// Eight, Nine, Tenement
			result = new Object[ 6 ];
			result[ 0 ] = "fewer skeletons";
			result[ 1 ] = "-sleaze";
			result[ 2 ] = "+Muscle";
			result[ 5 ] = "Return to Skid Row";
			return result;

		case 741:
		{
			// The Old Duke's Estate

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			buffer.append( "fewer zombies" );
			buffer.append( ", Freddies" );
			buffer.append( ", Fifty Ways to Bereave Your Lover (+100 sleaze damage)" );
			result[ 0 ] = buffer.toString();	// Cemetery

			buffer.setLength( 0 );
			buffer.append( "-hot" );
			if ( KoLCharacter.isMysticalityClass() )
			{
				buffer.append( ", dread tarragon (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.DREAD_TARRAGON ) ) );
				buffer.append( ") + dreadful roast (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.DREADFUL_ROAST ) ) );
				buffer.append( ") + bone flour (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.BONE_FLOUR ) ) );
				buffer.append( ") + stinking agaricus (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.STINKING_AGARICUS ) ) );
				buffer.append( ") -> Dreadsylvanian shepherd's pie" );
			}
			buffer.append( ", +Moxie" );
			result[ 1 ] = buffer.toString();	// Servants' Quarters

			buffer.setLength( 0 );
			ChoiceManager.lockSpoiler( buffer );
			buffer.append( "fewer werewolves" );
			buffer.append( ", eau de mort" );
			buffer.append( ", 10 ghost thread (" );
			buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.GHOST_THREAD ) ) );
			buffer.append( ") -> ghost shawl" );
			result[ 2 ] = buffer.toString();	// Master Suite (locked)

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil6" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 742:
			// The Plot Thickens
			result = new Object[ 6 ];
			result[ 0 ] = "fewer zombies";
			result[ 1 ] = "Freddies";
			result[ 2 ] = "Fifty Ways to Bereave Your Lover (+100 sleaze damage)";
			result[ 5 ] = "Return to The Old Duke's Estate";
			return result;

		case 743:
			// No Quarter
			result = new Object[ 6 ];
			result[ 0 ] = "-hot";
			result[ 1 ] =
				"dread tarragon (" +
				String.valueOf( InventoryManager.getCount( ItemPool.DREAD_TARRAGON ) ) +
				") + dreadful roast (" +
				String.valueOf( InventoryManager.getCount( ItemPool.DREADFUL_ROAST ) ) +
				") + bone flour (" +
				String.valueOf( InventoryManager.getCount( ItemPool.BONE_FLOUR ) ) +
				") + stinking agaricus (" +
				String.valueOf( InventoryManager.getCount( ItemPool.STINKING_AGARICUS ) ) +
				") -> Dreadsylvanian shepherd's pie";
			result[ 2 ] = "+Moxie";
			result[ 5 ] = "Return to The Old Duke's Estate";
			return result;

		case 744:
			// The Master Suite -- Sweet!
			result = new Object[ 6 ];
			result[ 0 ] = "fewer werewolves";
			result[ 1 ] = "eau de mort";
			result[ 2 ] =
				"10 ghost thread (" +
				String.valueOf( InventoryManager.getCount( ItemPool.GHOST_THREAD ) ) +
				") -> ghost shawl";
			result[ 5 ] = "Return to The Old Duke's Estate";
			return result;

		case 745:
		{
			// This Hall is Really Great

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			ChoiceManager.lockSpoiler( buffer );
			buffer.append( "fewer vampires" );
			buffer.append( ", " );
			if ( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.MUDDY_SKIRT, 1 ) ) )
			{
				buffer.append( "equipped muddy skirt -> weedy skirt and " );
			}
			else if ( InventoryManager.getCount( ItemPool.MUDDY_SKIRT ) > 0 )
			{
				buffer.append( "(muddy skirt in inventory but not equipped) " );
			}
			buffer.append( "+Moxie" );
			result[ 0 ] = buffer.toString();	// Ballroom (locked)

			buffer.setLength( 0 );
			buffer.append( "-cold" );
			buffer.append( ", Staying Frosty (+100 cold damage)" );
			result[ 1 ] = buffer.toString();	// Kitchen

			buffer.setLength( 0 );
			buffer.append( "dreadful roast" );
			buffer.append( ", -stench" );
			if ( KoLCharacter.isMysticalityClass() )
			{
				buffer.append( ", wax banana" );
			}
			result[ 2 ] = buffer.toString();	// Dining Room

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil7" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 746:
			// The Belle of the Ballroom
			result = new Object[ 6 ];
			result[ 0 ] = "fewer vampires";
			result[ 1 ] =
				( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.MUDDY_SKIRT, 1 ) ) ?
				  "equipped muddy skirt -> weedy skirt and " :
				  InventoryManager.getCount( ItemPool.MUDDY_SKIRT ) > 0 ?
				  "(muddy skirt in inventory but not equipped) " :
				  "" ) +
				"+Moxie";
			result[ 5 ] = "Return to The Great Hall";
			return result;

		case 747:
			// Cold Storage
			result = new Object[ 6 ];
			result[ 0 ] = "-cold";
			result[ 1 ] = "Staying Frosty (+100 cold damage)";
			result[ 5 ] = "Return to The Great Hall";
			return result;

		case 748:
			// Dining In (the Castle)
			result = new Object[ 6 ];
			result[ 0 ] = "dreadful roast";
			result[ 1 ] = "-stench";
			result[ 2 ] = "wax banana";
			result[ 5 ] = "Return to The Great Hall";
			return result;

		case 749:
		{
			// Tower Most Tall

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			ChoiceManager.lockSpoiler( buffer );
			buffer.append( "fewer bugbears" );
			buffer.append( ", fewer zombies" );
			buffer.append( ", visit The Machine" );
			if ( KoLCharacter.isMoxieClass() )
			{
				buffer.append( ", blood kiwi (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.BLOOD_KIWI ) ) );
				buffer.append( ") + eau de mort (" );
				buffer.append( String.valueOf( InventoryManager.getCount( ItemPool.EAU_DE_MORT ) ) );
				buffer.append( ") -> bloody kiwitini" );
			}
			result[ 0 ] = buffer.toString();	// Laboratory (locked)

			buffer.setLength( 0 );
			if ( KoLCharacter.isMysticalityClass() )
			{
				buffer.append( "fewer skeletons" );
				buffer.append( ", +Mysticality" );
				buffer.append( ", learn recipe for moon-amber necklace" );
			}
			else
			{
				buffer.append( "unavailable (Mysticality class only)" );
			}
			result[ 1 ] = buffer.toString();	// Books (mysticality only)

			buffer.setLength( 0 );
			buffer.append( "-sleaze" );
			buffer.append( ", Freddies" );
			buffer.append( ", Magically Fingered (+150 max MP, 40-50 MP regen)" );
			result[ 2 ] = buffer.toString();	// Bedroom

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil8" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 750:
			// Working in the Lab, Late One Night
			result = new Object[ 6 ];
			result[ 0 ] = "fewer bugbears";
			result[ 1 ] = "fewer zombies";
			result[ 2 ] = "visit The Machine";
			result[ 3 ] =
				"blood kiwi (" +
				String.valueOf( InventoryManager.getCount( ItemPool.BLOOD_KIWI ) ) +
				") + eau de mort (" +
				String.valueOf( InventoryManager.getCount( ItemPool.EAU_DE_MORT ) ) +
				") -> bloody kiwitini";
			result[ 5 ] = "Return to The Tower";
			return result;

		case 751:
			// Among the Quaint and Curious Tomes.
			result = new Object[ 6 ];
			result[ 0 ] = "fewer skeletons";
			result[ 1 ] = "+Mysticality";
			result[ 2 ] = "learn recipe for moon-amber necklace";
			result[ 5 ] = "Return to The Tower";
			return result;

		case 752:
			// In The Boudoir
			result = new Object[ 6 ];
			result[ 0 ] = "-sleaze";
			result[ 1 ] = "Freddies";
			result[ 2 ] = "Magically Fingered (+150 max MP, 40-50 MP regen)";
			result[ 5 ] = "Return to The Tower";
			return result;

		case 753:
		{
			// The Dreadsylvanian Dungeon

			result = new Object[ 6 ];

			StringBuilder buffer = new StringBuilder();
			buffer.append( "-spooky" );
			buffer.append( ", +Muscle" );
			buffer.append( ", +MP" );
			result[ 0 ] = buffer.toString();	// Prison

			buffer.setLength( 0 );
			buffer.append( "-hot" );
			buffer.append( ", Freddies" );
			buffer.append( ", +Muscle/Mysticality/Moxie" );
			result[ 1 ] = buffer.toString();	// Boiler Room

			buffer.setLength( 0 );
			buffer.append( "stinking agaricus" );
			buffer.append( ", Spore-wreathed (reduce enemy defense by 20%)" );
			result[ 2 ] = buffer.toString();	// Guard room

			result[ 4 ] = ChoiceManager.shortcutSpoiler( "ghostPencil9" );
			result[ 5 ] = "Leave this noncombat";
			return result;
		}

		case 754:
			// Live from Dungeon Prison
			result = new Object[ 6 ];
			result[ 0 ] = "-spooky";
			result[ 1 ] = "+Muscle";
			result[ 2 ] = "+MP";
			result[ 5 ] = "Return to The Dungeons";
			return result;

		case 755:
			// The Hot Bowels
			result = new Object[ 6 ];
			result[ 0 ] = "-hot";
			result[ 1 ] = "Freddies";
			result[ 2 ] = "+Muscle/Mysticality/Moxie";
			result[ 5 ] = "Return to The Dungeons";
			return result;

		case 756:
			// Among the Fungus
			result = new Object[ 6 ];
			result[ 0 ] = "stinking agaricus";
			result[ 1 ] = "Spore-wreathed (reduce enemy defense by 20%)";
			result[ 5 ] = "Return to The Dungeons";
			return result;

		case 758:
		{
			// End of the Path

			StringBuilder buffer = new StringBuilder();
			boolean necklaceEquipped = KoLCharacter.hasEquipped( ChoiceManager.MOON_AMBER_NECKLACE );
			boolean necklaceAvailable = InventoryManager.getCount( ChoiceManager.MOON_AMBER_NECKLACE ) > 0;
			boolean hasKiwiEffect = KoLConstants.activeEffects.contains( ChoiceManager.KIWITINI_EFFECT );
			boolean isBlind =
				KoLConstants.activeEffects.contains( ChoiceManager.TEMPORARY_BLINDNESS ) ||
				KoLCharacter.hasEquipped( ChoiceManager.MAKESHIFT_TURBAN );
			boolean kiwitiniAvailable = InventoryManager.getCount( ChoiceManager.BLOODY_KIWITINI ) > 0;

			buffer.append( necklaceEquipped ? "moon-amber necklace equipped" :
				       necklaceAvailable ? "moon-amber necklace NOT equipped but in inventory" :
				       "moon-amber necklace neither equipped nor available" );
			buffer.append( " / " );
			buffer.append( hasKiwiEffect ? ( isBlind ? "First Blood Kiwi and blind" : "First Blood Kiwi but NOT blind" ) :
				       kiwitiniAvailable ? "bloody kiwitini in inventory" :
				       "First Blood Kiwi neither active nor available" );

			result = new Object[ 2 ];
			result[ 0 ] = buffer.toString();
			result[ 1 ] = "Run away";
			return result;
		}

		case 759:
		{
			// You're About to Fight City Hall

			StringBuilder buffer = new StringBuilder();
			boolean badgeEquipped = KoLCharacter.hasEquipped( ChoiceManager.AUDITORS_BADGE );
			boolean badgeAvailable = InventoryManager.getCount( ChoiceManager.AUDITORS_BADGE ) > 0;
			boolean skirtEquipped = KoLCharacter.hasEquipped( ChoiceManager.WEEDY_SKIRT );
			boolean skirtAvailable = InventoryManager.getCount( ChoiceManager.WEEDY_SKIRT ) > 0;

			buffer.append( badgeEquipped ? "Dreadsylvanian auditor's badge equipped" :
				       badgeAvailable ? "Dreadsylvanian auditor's badge NOT equipped but in inventory" :
				       "Dreadsylvanian auditor's badge neither equipped nor available" );
			buffer.append( " / " );
			buffer.append( skirtEquipped ? "weedy skirt equipped" :
				       skirtAvailable ? "weedy skirt NOT equipped but in inventory" :
				       "weedy skirt neither equipped nor available" );

			result = new Object[ 2 ];
			result[ 0 ] = buffer.toString();
			result[ 1 ] = "Run away";
			return result;
		}

		case 760:
		{
			// Holding Court

			StringBuilder buffer = new StringBuilder();
			boolean shawlEquipped = KoLCharacter.hasEquipped( ChoiceManager.GHOST_SHAWL );
			boolean shawlAvailable = InventoryManager.getCount( ChoiceManager.GHOST_SHAWL ) > 0;
			boolean hasPieEffect = KoLConstants.activeEffects.contains( ChoiceManager.PIE_EFFECT );
			boolean pieAvailable = InventoryManager.getCount( ChoiceManager.SHEPHERDS_PIE ) > 0;

			buffer.append( shawlEquipped ? "ghost shawl equipped" :
				       shawlAvailable ? "ghost shawl NOT equipped but in inventory" :
				       "ghost shawl neither equipped nor available" );
			buffer.append( " / " );
			buffer.append( hasPieEffect ? "Shepherd's Breath active" :
				       pieAvailable ? "Dreadsylvanian shepherd's pie in inventory" :
				       "Shepherd's Breath neither active nor available" );

			result = new Object[ 2 ];
			result[ 0 ] = buffer.toString();
			result[ 1 ] = "Run away";
			return result;
		}

		case 772:
		{
			// Saved by the Bell
			StringBuilder buffer = new StringBuilder();
			
			buffer.append( "Get " );
			buffer.append( String.valueOf( ( Preferences.getInteger( "kolhsTotalSchoolSpirited" ) + 1 ) * 10 ) );
			buffer.append( " turns of School Spirited (+100% Meat drop, +50% Item drop)" );
			
			// If you reach this encounter and Mafia things you've not spend 40 adventures in KOL High school, correct this
			Preferences.setInteger( "_kolhsAdventures", 40 );
			
			result = new String[ 10 ];
			result[ 0 ] = Preferences.getBoolean( "_kolhsSchoolSpirited" ) ? "Already got School Spirited today" : buffer.toString();
			result[ 1 ] = Preferences.getBoolean( "_kolhsPoeticallyLicenced" ) ? "Already got Poetically Licenced today" :
				"50 turns of Poetically Licenced (+20% Myst, -20% Muscle, +2 Myst stats/fight, +10% Spell damage)";
			result[ 2 ] = InventoryManager.getCount( ItemPool.YEARBOOK_CAMERA ) > 0 
				|| KoLCharacter.hasEquipped( ItemPool.get( ItemPool.YEARBOOK_CAMERA, 1 ) ) ? "Turn in yesterday's photo (if you have it)" : "Get Yearbook Camera";
			result[ 3 ] = Preferences.getBoolean( "_kolhsCutButNotDried" ) ? "Already got Cut But Not Dried today" :
				"50 turns of Cut But Not Dried (+20% Muscle, -20% Moxie, +2 Muscle stats/fight, +10% Weapon damage)";
			result[ 4 ] = Preferences.getBoolean( "_kolhsIsskayLikeAnAshtray" ) ? "Already got Isskay Like An Ashtray today" :
				"50 turns of Isskay Like An Ashtray (+20% Moxie, -20% Myst, +2 Moxie stats/fight, +10% Pickpocket chance)";
			result[ 5 ] = "Make items";
			result[ 6 ] = "Make items";
			result[ 7 ] = "Make items";
			result[ 9 ] = "Leave";
			return result;
		}

		case 780:
		{
			// Action Elevator
			
			int hiddenApartmentProgress = Preferences.getInteger( "hiddenApartmentProgress" );
			boolean hasOnceCursed = KoLConstants.activeEffects.contains( ChoiceManager.CURSE1_EFFECT );
			boolean hasTwiceCursed = KoLConstants.activeEffects.contains( ChoiceManager.CURSE2_EFFECT );
			boolean hasThriceCursed = KoLConstants.activeEffects.contains( ChoiceManager.CURSE3_EFFECT );
			boolean pygmyLawyersRelocated = Preferences.getInteger( "relocatePygmyLawyer" ) == KoLCharacter.getAscensions();
			
			result = new String[ 6 ];
			result[ 0 ] = ( hiddenApartmentProgress >= 7 ? "penthouse empty" :
					hasThriceCursed ? "Fight ancient protector spirit" :
					"Need Thrice-Cursed to fight ancient protector spirit" );
			result[ 1 ] = ( hasThriceCursed ? "Increase Thrice-Cursed" :
					hasTwiceCursed ? "Get Thrice-Cursed" :
					hasOnceCursed ? "Get Twice-Cursed" :
					"Get Once-Cursed" );
			result[ 2 ] = ( pygmyLawyersRelocated ? "Waste adventure" : "Relocate pygmy witch lawyers to Hidden Park" );
			result[ 5 ] = "skip adventure";
			return result;
 		}
		
		case 781:
			// Earthbound and Down
			result = new String[ 6 ];
			result[ 0 ] = "Unlock Hidden Apartment Building";
			result[ 1 ] = "Get stone triangle";
			result[ 2 ] = "Get Blessing of Bulbazinalli";
			result[ 5 ] = "skip adventure";
			return result;

		case 783:
			// Water You Dune
			result = new String[ 6 ];
			result[ 0 ] = "Unlock Hidden Hospital";
			result[ 1 ] = "Get stone triangle";
			result[ 2 ] = "Get Blessing of Squirtlcthulli";
			result[ 5 ] = "skip adventure";
			return result;
		
		case 784:
			// You, M. D.
			result = new String[ 6 ];
			result[ 0 ] = "Fight ancient protector spirit";
			result[ 5 ] = "skip adventure";
			return result;

		case 785:
			// Air Apparent
			result = new String[ 6 ];
			result[ 0 ] = "Unlock Hidden Office Building";
			result[ 1 ] = "Get stone triangle";
			result[ 2 ] = "Get Blessing of Pikachutlotal";
			result[ 5 ] = "skip adventure";
			return result;

		case 786:
		{
			// Working Holiday
			
			int hiddenOfficeProgress = Preferences.getInteger( "hiddenOfficeProgress" );
			boolean hasBossUnlock = hiddenOfficeProgress >= 6;
			boolean hasMcCluskyFile = InventoryManager.getCount( ChoiceManager.MCCLUSKY_FILE ) > 0;
			boolean hasBinderClip = InventoryManager.getCount( ChoiceManager.BINDER_CLIP ) > 0;
			
			result = new String[ 6 ];
			result[ 0 ] = ( hiddenOfficeProgress >= 7 ? "office empty" :
					hasMcCluskyFile || hasBossUnlock ? "Fight ancient protector spirit" :
					"Need McClusky File (complete) to fight ancient protector spirit" );
			result[ 1 ] = ( hasBinderClip || hasMcCluskyFile || hasBossUnlock ) ? "Get random item" : "Get boring binder clip";
			result[ 2 ] = "Fight pygmy witch accountant";
			result[ 5 ] = "skip adventure";
			return result;		
		}
			
		case 787:
			// Fire when Ready
			result = new String[ 6 ];
			result[ 0 ] = "Unlock Hidden Bowling Alley";
			result[ 1 ] = "Get stone triangle";
			result[ 2 ] = "Get Blessing of Charcoatl";
			result[ 5 ] = "skip adventure";
			return result;

		case 788:
		{
			// Life is Like a Cherry of Bowls
			int hiddenBowlingAlleyProgress = Preferences.getInteger( "hiddenBowlingAlleyProgress" );

			StringBuilder buffer = new StringBuilder();
			buffer.append( "Get stats, on 5th visit, fight ancient protector spirit (");
			buffer.append( String.valueOf( 6 - hiddenBowlingAlleyProgress ) );
			buffer.append( " visit" );
			if ( hiddenBowlingAlleyProgress < 5 )
			{
				buffer.append( "s" );
			}
			buffer.append( " left" );

			result = new String[ 6 ];
			result[ 0 ] = ( hiddenBowlingAlleyProgress > 6 ? "Get stats" :
					hiddenBowlingAlleyProgress == 6 ? "fight ancient protector spirit" :
					buffer.toString() );
			result[ 5 ] = "skip adventure";
			return result;
		}

		case 789:
		{
			boolean pygmyJanitorsRelocated = Preferences.getInteger( "relocatePygmyJanitor" ) == KoLCharacter.getAscensions();

			// Where Does The Lone Ranger Take His Garbagester?
			result = new String[ 6 ];
			result[ 0 ] = "Get random items";
			result[ 1 ] = ( pygmyJanitorsRelocated ? "Waste adventure" : "Relocate pygmy janitors to Hidden Park" );
			result[ 5 ] = "skip adventure";
			return result;
		}

		case 791:
		{
			// Legend of the Temple in the Hidden City
			
			int stoneTriangles = InventoryManager.getCount( ChoiceManager.STONE_TRIANGLE );
			StringBuilder buffer = new StringBuilder();

			buffer.append( "Need 4 stone triangles to fight Protector Spectre (");
			buffer.append( String.valueOf( stoneTriangles ) );
			buffer.append( ")" );
			
			result = new String[ 6 ];
			result[ 0 ] = ( stoneTriangles == 4 ? "fight Protector Spectre": buffer.toString() );
			result[ 5 ] = "skip adventure";
			return result;
		}

		case 801:
		
			// A Reanimated Conversation
			result = new String[ 7 ];
			result[ 0 ] = "skulls increase meat drops";
			result[ 1 ] = "arms deal extra damage";
			result[ 2 ] = "legs increase item drops";
			result[ 3 ] = "wings sometimes delevel at start of combat";
			result[ 4 ] = "weird parts sometimes block enemy attacks";
			result[ 5 ] = "get rid of all collected parts";
			result[ 6 ] = "no changes";
			return result;

		}
		return null;
	}

	private static final String shortcutSpoiler( final String setting )
	{
		return Preferences.getBoolean( setting ) ?
			"shortcut KNOWN" :
			"learn shortcut";
	}

	private static final void lockSpoiler( StringBuilder buffer )
	{
		buffer.append( "possibly locked," );
		if ( InventoryManager.getCount( ItemPool.DREADSYLVANIAN_SKELETON_KEY ) == 0 )
		{
			buffer.append( " no" );
		}
		buffer.append( " key in inventory: " );
	}

	private static final Object[] dynamicChoiceOptions( final String option )
	{
		if ( !option.startsWith( "choiceAdventure" ) )
		{
			return null;
		}
		int choice = StringUtilities.parseInt( option.substring( 15 ) );
		return ChoiceManager.dynamicChoiceOptions( choice );
	}

	public static final Object choiceSpoiler( final int choice, final int decision, final Object[] spoilers )
	{
		switch ( choice )
		{
		case 105:
			// Having a Medicine Ball
			if ( decision == 3 )
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
		case 182:
			if ( decision == 4 )
			{
				return "model airship";
			}
			break;
		case 793:
			if ( decision == 4 )
			{
				return "gift shop";
			}
			break;
		}

		// Iterate through the spoilers and find the one corresponding to the decision
		for ( int i = 0; i < spoilers.length; ++i )
		{
			Object spoiler = spoilers[i];

			// If this is an Option object, it may specify the option
			if ( spoiler instanceof Option )
			{
				int option = ((Option)spoiler).getOption();
				if ( option == decision )
				{
					return spoiler;
				}
				if ( option != 0 )
				{
					continue;
				}
				// option of 0 means use positional index
			}

			// If we get here, match positionalindex
			if ( ( i + 1 ) == decision )
			{
				return spoiler;
			}
		}

		// If we get here, we ran out of spoilers.
		return null;
	}

	public static final void processRedirectedChoiceAdventure( final String redirectLocation )
	{
		ChoiceManager.processChoiceAdventure( ChoiceManager.CHOICE_HANDLER, redirectLocation, null );
	}

	public static final void processChoiceAdventure( final String responseText )
	{
		ChoiceManager.processChoiceAdventure( ChoiceManager.CHOICE_HANDLER, "choice.php", responseText );
	}

	public static final void processChoiceAdventure( int decision )
	{
		GenericRequest request = ChoiceManager.CHOICE_HANDLER;

		request.constructURLString( "choice.php" );
		request.addFormField( "whichchoice", String.valueOf( ChoiceManager.lastChoice ) );
		request.addFormField( "option", String.valueOf( decision ) );
		request.addFormField( "pwd", GenericRequest.passwordHash );
		request.run();

		ChoiceManager.processChoiceAdventure( request, "choice.php", request.responseText );
	}

	public static final void processChoiceAdventure( final GenericRequest request, final String initialURL, final String responseText )
	{
		// You can no longer simply ignore a choice adventure.  One of
		// the options may have that effect, but we must at least run
		// choice.php to find out which choice it is.

		// Get rid of extra fields - like "action=auto"
		request.constructURLString( initialURL );

		if ( responseText == null )
		{
			GoalManager.updateProgress( GoalManager.GOAL_CHOICE );
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

		for ( int stepCount = 0; request.responseText.indexOf( "action=choice.php" ) != -1; ++stepCount )
		{
			request.clearDataFields();

			int choice = ChoiceManager.extractChoice( request.responseText );
			if ( choice == 0 )
			{
				// choice.php did not offer us any choices.
				// This would be a bug in KoL itself.
				// Bail now and let the user finish by hand.

				KoLmafia.updateDisplay( MafiaState.ABORT, "Encountered choice adventure with no choices." );
				request.showInBrowser( true );
				return;
			}

			// If this choice has special handling that can't be
			// handled by a single preference (extra fields, for
			// example), handle it elsewhere.

			if ( ChoiceManager.specialChoiceHandling( choice, request ) )
			{
				return;
			}

			String option = "choiceAdventure" + choice;
			String decision = Preferences.getString( option );

			// If choice zero is not "Manual Control", adjust it to an actual choice

			decision = ChoiceManager.specialChoiceDecision1( choice, decision, stepCount, request.responseText );

			// If one of the decisions will satisfy a goal, take it

			decision = ChoiceManager.pickGoalChoice( option, decision );

			// If this choice has special handling based on
			// character state, convert to real decision index

			decision = ChoiceManager.specialChoiceDecision2( choice, decision, stepCount, request.responseText );

			// Let user handle the choice manually, if requested

			if ( decision.equals( "0" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Manual control requested for choice #" + choice );
				ChoiceCommand.printChoices();
				request.showInBrowser( true );
				return;
			}

			// Bail if no setting determines the decision

			if ( decision.equals( "" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Unsupported choice adventure #" + choice );
				ChoiceCommand.logChoices();
				request.showInBrowser( true );
				return;
			}

			// Make sure that KoL currently allows the chosen choice

			if ( !ChoiceCommand.optionAvailable( decision, request.responseText ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Requested choice (" + decision + ") for choice #" + choice + " is not currently available." );
				ChoiceCommand.printChoices();
				request.showInBrowser( true );
				return;
			}

			request.addFormField( "whichchoice", String.valueOf( choice ) );
			request.addFormField( "option", decision );
			request.addFormField( "pwd", GenericRequest.passwordHash );

			request.run();
		}
	}

	public static final int getDecision( int choice, String responseText )
	{
		String option = "choiceAdventure" + choice;
		String decision = Preferences.getString( option );

		// If choice decision is not "Manual Control", adjust it to an actual option

		decision = ChoiceManager.specialChoiceDecision1( choice, decision, Integer.MAX_VALUE, responseText );

		// If one of the decisions will satisfy a goal, take it

		decision = ChoiceManager.pickGoalChoice( option, decision );

		// If this choice has special handling based on
		// character state, convert to real decision index

		decision = ChoiceManager.specialChoiceDecision2( choice, decision, Integer.MAX_VALUE, responseText );

		// Currently unavailable decision, manual choice requested, or unsupported choice
		if ( decision.equals( "0" ) || decision.equals( "" ) || !ChoiceCommand.optionAvailable( decision, responseText ) )
		{
			return 0;
		}

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
		ChoiceManager.handlingChoice = true;

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
			PyramidRequest.setPyramidWheelPlaced();
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
				QuestDatabase.setQuestProgress( Quest.ISLAND_WAR, "step1" );
				Preferences.setString( "warProgress", "started" );
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

		// The Horror...
		case 611:
			if ( ChoiceManager.lastDecision == 2 ) // Flee
			{
				// To find which step we're on, look at the responseText from the _previous_ request.  This should still be in lastResponseText.
				int level = ChoiceManager.findBooPeakLevel( ChoiceManager.findChoiceDecisionText( 1, ChoiceManager.lastResponseText ) ) - 1;
				if ( level < 1 )
					break;
				// We took 1 off the level since we didn't actually complete the level represented by that button.
				// The formula is now progress = n*(n+1), where n is the number of levels completed.
				// 0 (flee immediately): 0, breaks above
				// 1: 2
				// 2: 6
				// 3: 12
				// 4: 20
				// 5 NOT handled here.  see postChoice1.
				Preferences.decrement( "booPeakProgress", level * ( level + 1 ), 0 );
			}
			break;

		// Behind the world there is a door...
		case 612:
			TurnCounter.stopCounting( "Silent Invasion window begin" );
			TurnCounter.stopCounting( "Silent Invasion window end" );
			TurnCounter.startCounting( 35, "Silent Invasion window begin loc=*", "lparen.gif" );
			TurnCounter.startCounting( 40, "Silent Invasion window end loc=*", "rparen.gif" );
			break;

		case 794:
			ResultProcessor.removeItem( ItemPool.FUNKY_JUNK_KEY );
			break;

		}
	}

	private static final Pattern SKELETON_PATTERN = Pattern.compile( "You defeated <b>(\\d+)</b> skeletons" );
	private static final Pattern FOG_PATTERN = Pattern.compile( "<font.*?><b>(.*?)</b></font>" );

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

		String urlString = request.getURLString();
		ChoiceManager.lastResponseText = request.responseText;
		String text = ChoiceManager.lastResponseText;

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

		case 191:
			// Chatterboxing
			if ( ChoiceManager.lastDecision == 2 && text.contains( "find a valuable trinket that looks promising" ) )
			{
				BanishManager.banishMonster( "chatty pirate", "chatterboxing" );
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

		case 413:
		case 414:
		case 415:
		case 416:
		case 417:
		case 418:
			HaciendaManager.parseRoom( ChoiceManager.lastChoice, ChoiceManager.lastDecision, text );
			break;

		case 440:
			// Puttin' on the Wax
			if ( ChoiceManager.lastDecision == 1 )
			{
				HaciendaManager.parseRecording( urlString, text );
			}
			break;

		case 443:
			// Chess Puzzle
			if ( ChoiceManager.lastDecision == 1 )
			{
				// Option 1 is "Play"
				RabbitHoleManager.parseChessMove( urlString, text );
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
			ArcadeRequest.postChoiceSpaceTrip( request, ChoiceManager.lastChoice, ChoiceManager.lastDecision );
			break;

		case 471:
			// DemonStar
			ArcadeRequest.postChoiceDemonStar( request, ChoiceManager.lastDecision );
			break;

		case 485:
			// Fighters Of Fighting
			ArcadeRequest.postChoiceFightersOfFighting( request, ChoiceManager.lastDecision );
			break;

		case 486:
			// Dungeon Fist!
			ArcadeRequest.postChoiceDungeonFist( request, ChoiceManager.lastDecision );
			break;

		case 488: case 489: case 490: case 491:
			// Meteoid
			ArcadeRequest.postChoiceMeteoid( request, ChoiceManager.lastChoice, ChoiceManager.lastDecision );
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
				QuestDatabase.setQuestIfBetter( Quest.GENERATOR, QuestDatabase.FINISHED );
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

		case 546:
			// Interview With You
			VampOutManager.postChoiceVampOut( text );
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

		case 588:
			// Machines!
			if ( text.contains( "The batbugbears around you start acting weird" ) )
			{
				BugbearManager.clearShipZone( "Sonar" );
			}
			break;

		case 589:
			// Autopsy Auturvy
			// The tweezers you used dissolve in the caustic fluid. Rats.
			if ( text.indexOf( "dissolve in the caustic fluid" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.AUTOPSY_TWEEZERS, -1 );
			}
			return;

		case 599:
			// A Zombie Master's Bait
			if ( request.getFormField( "quantity" ) == null )
			{
				return;
			}

			AdventureResult brain;
			if ( ChoiceManager.lastDecision == 1 )
			{
				brain = ItemPool.get( ItemPool.CRAPPY_BRAIN, 1 );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				brain = ItemPool.get( ItemPool.DECENT_BRAIN, 1 );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				brain = ItemPool.get( ItemPool.GOOD_BRAIN, 1 );
			}
			else if ( ChoiceManager.lastDecision == 4 )
			{
				brain = ItemPool.get( ItemPool.BOSS_BRAIN, 1 );
			}
			else
			{
				return;
			}

			int quantity = StringUtilities.parseInt( request.getFormField( "quantity" ) );
			int inventoryCount = brain.getCount( KoLConstants.inventory );
			brain = brain.getInstance( -1 * Math.min( quantity, inventoryCount ) );

			ResultProcessor.processResult( brain );

			return;

		case 603:
			// Skeletons and The Closet
			if ( ChoiceManager.lastDecision != 6 )
			{
				ResultProcessor.removeItem( ItemPool.SKELETON );
			}
			return;

		case 607:
			// Room 237
			// Twin Peak first choice
			if ( text.contains( "You take a moment to steel your nerves." ) )
			{
				int prefval = Preferences.getInteger( "twinPeakProgress" );
				prefval |= 1;
				Preferences.setInteger( "twinPeakProgress", prefval );
			}
			return;

		case 608:
			// Go Check It Out!
			// Twin Peak second choice
			if ( text.contains( "All work and no play" ) )
			{
				int prefval = Preferences.getInteger( "twinPeakProgress" );
				prefval |= 2;
				Preferences.setInteger( "twinPeakProgress", prefval );
			}
			return;

		case 611:
			// The Horror...
			// We need to detect if the choiceadv chain was completed OR we got beaten up.
			// Fleeing is handled in preChoice
			if ( text.contains( "You drop the book, trying to scrub" ) ||
				text.contains( "Well, it wasn't easy, and it wasn't pleasant" ) ||
				text.contains( "You survey the deserted battle site" ) ||
				text.contains( "You toss the map aside with a smile" ) ||
				text.contains( "Unlike your mom, it wasn't easy" ) ||
				text.contains( "You finally cleared all the ghosts from the square" ) ||
				text.contains( "You made it through the battle site" ) ||
				text.contains( "Man, those Space Tourists are hardcore" ) ||
				text.contains( "You stagger to your feet outside Pigherpes" ) ||
				text.contains( "You clear out of the battle site, still a little" ) )
			{
				Preferences.decrement( "booPeakProgress", 30, 0 );
			}
			else if ( text.contains( "That's all the horror you can take" ) ) // AKA beaten up
			{
				Preferences.decrement( "booPeakProgress", 2, 0 );
			}

		case 614:
			// Near the fog there is an... anvil?
			if ( text.indexOf( "You acquire" ) == -1 )
			{
				return;
			}
			int souls =
				ChoiceManager.lastDecision == 1 ? 3 :
				ChoiceManager.lastDecision == 2 ? 11 :
				ChoiceManager.lastDecision == 3 ? 23 :
				ChoiceManager.lastDecision == 4 ? 37 :
				0;
			if ( souls > 0 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.MIME_SOUL_FRAGMENT, 0 - souls ) );
			}
			return;

		case 616:
			// He Is the Arm, and He Sounds Like This
			// Twin Peak third choice
			if ( text.contains( "You attempt to mingle" ) )
			{
				int prefval = Preferences.getInteger( "twinPeakProgress" );
				prefval |= 4;
				Preferences.setInteger( "twinPeakProgress", prefval );
				ResultProcessor.processResult( ItemPool.get( ItemPool.JAR_OF_OIL, -1 ) );
			}
			return;

		case 617:
			// Now It's Dark
			// Twin Peak fourth choice
			if ( text.contains( "When the lights come back" ) )
			{
				// the other three must be completed at this point.
				Preferences.setInteger( "twinPeakProgress", 15 );
			}
			return;

		case 669:
		case 670:
		case 671:
			// All New Area Unlocked messages unlock the Ground Floor but check for it specifically in case future changes unlock areas with message.
			if ( text.contains( "New Area Unlocked" ) && text.contains( "The Ground Floor" ) )
			{
				Preferences.setInteger( "lastCastleGroundUnlock", KoLCharacter.getAscensions() );
			}
			break;			

		case 689:
			// The Final Reward
			if ( text.contains( "claim your rightful reward" ) )
			{
				// Daily Dungeon Complete
				Preferences.setBoolean( "dailyDungeonDone", true );
				Preferences.setInteger( "_lastDailyDungeonRoom", 15 );
			}
			return;

		case 690:
		case 691:
			// The First Chest Isn't the Deepest and Second Chest
			if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.increment( "_lastDailyDungeonRoom", 3 );
			}
			else
			{
				Preferences.increment( "_lastDailyDungeonRoom", 1 );
			}
			return;

		case 692:
			// I Wanna Be a Door
			if ( text.contains( "key breaks off in the lock" ) )
			{
				// Unfortunately, the key breaks off in the lock.
				ResultProcessor.processItem( ItemPool.SKELETON_KEY, -1 );
			}
			if ( ChoiceManager.lastDecision != 8 )
			{
				Preferences.increment( "_lastDailyDungeonRoom", 1 );
			}
			return;

		case 693:
			// It's Almost Certainly a Trap
			if ( ChoiceManager.lastDecision != 3 )
			{
				Preferences.increment( "_lastDailyDungeonRoom", 1 );
			}
			return;

		case 699:
			// Lumber-related Pun
			if ( text.contains( "hand him the branch" ) )
			{
				// Marty's eyes widen when you hand him the
				// branch from the Great Tree.
				ResultProcessor.processItem( ItemPool.GREAT_TREE_BRANCH, -1 );
			}
			else if ( text.contains( "hand him the rust" ) )
			{
				// At first Marty looks disappointed when you
				// hand him the rust-spotted, rotten-handled
				// axe, but after a closer inspection he gives
				// an impressed whistle.
				ResultProcessor.processItem( ItemPool.PHIL_BUNION_AXE, -1 );
			}
			else if ( text.contains( "hand him the bouquet" ) )
			{
				// Marty looks delighted when you hand him the
				// bouquet of swamp roses.
				ResultProcessor.processItem( ItemPool.SWAMP_ROSE_BOUQUET, -1 );
			}
			return;

		case 700:
			// Delirium in the Cafeterium
			Preferences.increment( "_kolhsAdventures", 1 );
			return;

		case 703:
			// Mer-kin dreadscroll
			if ( text.contains( "I guess you're the Mer-kin High Priest now" ) )
			{
				Preferences.setString( "merkinQuestPath", "scholar" );
				ResultProcessor.processItem( ItemPool.DREADSCROLL, -1 );
				return;
			}
			return;

		case 709:
			// You Beat Shub to a Stub, Bub
		case 713:
			// You Brought Her To Her Kn-kn-kn-kn-knees, Knees.
		case 717:
			// Over. Over Now.
			Preferences.setString( "merkinQuestPath", "done" );
			return;

		case 720:
			// The Florist Friar's Cottage
			FloristRequest.parseResponse( urlString , text );
			return;

		case 721:
			// The Cabin in the Dreadsylvanian Woods
			if ( ChoiceManager.lastDecision == 3 )
			{
				// Try the Attic

				// You use your skeleton key to unlock the padlock on the attic trap door.
				// Then you use your legs to climb the ladder into the attic.
				// Then you use your stupidity to lose the skeleton key.  Crap.

				if ( text.contains( "lose the skeleton key" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}
			return;

		case 722:
			// The Kitchen in the Woods
			if ( ChoiceManager.lastDecision == 2 )
			{
				// Screw around with the flour mill
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.OLD_DRY_BONE, -1 ) );
				}
			}
			return;


		case 723:
			// What Lies Beneath (the Cabin)
			if ( ChoiceManager.lastDecision == 3 )
			{
				// Check out the lockbox
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.REPLICA_KEY, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 4 )
			{
				// Stick a wax banana in the lock
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.WAX_BANANA, -1 ) );
				}
			}
			return;

		case 725:
			// Tallest Tree in the Forest
			if ( ChoiceManager.lastDecision == 2 )
			{
				// Check out the fire tower

				// You climb the rope ladder and use your skeleton key to
				// unlock the padlock on the door leading into the little room
				// at the top of the watchtower. Then you accidentally drop
				// your skeleton key and lose it in a pile of leaves. Rats.

				if ( text.contains( "you accidentally drop your skeleton key" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}

			return;

		case 729:
			// Below the Roots
			if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}
			return;

		case 730:
			// Hot Coals
			if ( ChoiceManager.lastDecision == 3 )
			{
				// Melt down an old ball and chain
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.OLD_BALL_AND_CHAIN, -1 ) );
				}
			}
			return;

		case 733:
			// Dreadsylvanian Village Square
			if ( ChoiceManager.lastDecision == 1 )
			{
				// The schoolhouse

				// You try the door of the schoolhouse, but it's locked. You
				// try your skeleton key in the lock, but it works. I mean and
				// it works. But it breaks. That was the but.

				if ( text.contains( "But it breaks" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}
			return;

		case 737:
			// The Even More Dreadful Part of Town
			if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}
			return;

		case 739:
			// The Tinker's. Damn.
			if ( ChoiceManager.lastDecision == 2 )
			{
				// Make a key using the wax lock impression
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.WAX_LOCK_IMPRESSION, -1 ) );
					ResultProcessor.processResult( ItemPool.get( ItemPool.INTRICATE_MUSIC_BOX_PARTS, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				// Polish the moon-amber
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.MOON_AMBER, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 4 )
			{
				// Assemble a clockwork bird
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREADSYLVANIAN_CLOCKWORK_KEY, -1 ) );
					ResultProcessor.processResult( ItemPool.get( ItemPool.INTRICATE_MUSIC_BOX_PARTS, -3 ) );
				}
			}
			return;

		case 741:
			// The Old Duke's Estate
			if ( ChoiceManager.lastDecision == 3 )
			{
				// Make your way to the master suite

				// You find the door to the old Duke's master bedroom and
				// unlock it with your skeleton key.

				if ( text.contains( "unlock it with your skeleton key" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}
			return;

		case 743:
			// No Quarter
			if ( ChoiceManager.lastDecision == 2 )
			{
				// Make a shepherd's pie
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREAD_TARRAGON, -1 ) );
					ResultProcessor.processResult( ItemPool.get( ItemPool.BONE_FLOUR, -1 ) );
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREADFUL_ROAST, -1 ) );
					ResultProcessor.processResult( ItemPool.get( ItemPool.STINKING_AGARICUS, -1 ) );
				}
			}
			return;

		case 744:
			// The Master Suite -- Sweet!
			if ( ChoiceManager.lastDecision == 3 )
			{
				// Mess with the loom
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_THREAD, -10 ) );
				}
			}
			return;

		case 745:
			// This Hall is Really Great
			if ( ChoiceManager.lastDecision == 1 )
			{
				// Head to the ballroom

				// You unlock the door to the ballroom with your skeleton
				// key. You open the door, and are so impressed by the site of
				// the elegant ballroom that you drop the key down a nearby
				// laundry chute.

				if ( text.contains( "you drop the key" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}
			return;

		case 746:
			// The Belle of the Ballroom
			if ( ChoiceManager.lastDecision == 2 )
			{
				// Trip the light fantastic

				// You twirl around on the dance floor to music only you can
				// hear, your muddy skirt whirling around you filthily. You get
				// so caught up in the twirling that you drop your seed pod. It
				// breaks open, spreading weed seeds all over your skirt, which
				// immediately take root and grow.

				if ( text.contains( "spreading weed seeds all over your skirt" ) )
				{
					EquipmentManager.discardEquipment( ItemPool.MUDDY_SKIRT );
					EquipmentManager.setEquipment( EquipmentManager.PANTS, ItemPool.get( ItemPool.WEEDY_SKIRT, 1 ) );
				}
			}
			return;

		case 749:
			// Tower Most Tall
			if ( ChoiceManager.lastDecision == 1 )
			{
				// Go to the laboratory

				// You use your skeleton key to unlock the door to the
				// laboratory. Unfortunately, the lock is electrified, and it
				// incinerates the key shortly afterwards.

				if ( text.contains( "it incinerates the key" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.DREADSYLVANIAN_SKELETON_KEY, -1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}
			return;

		case 750:
			// Working in the Lab, Late One Night
			if ( ChoiceManager.lastDecision == 4 )
			{
				// Use the still
				if ( text.contains( "You acquire" ) )
				{
					ResultProcessor.processResult( ItemPool.get( ItemPool.BLOOD_KIWI, -1 ) );
					ResultProcessor.processResult( ItemPool.get( ItemPool.EAU_DE_MORT, -1 ) );
				}
			}
			return;

		case 753:
			// The Dreadsylvanian Dungeon
			if ( ChoiceManager.lastDecision == 5 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.GHOST_PENCIL, -1 ) );
			}
			return;

		case 762:
			// Try New Extra-Strength Anvil
			// All of the options that craft something use the same ingredients
			if ( text.contains( "You acquire" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.COOL_IRON_INGOT, -1 ) );
				ResultProcessor.processResult( ItemPool.get( ItemPool.WARM_FUR, -1 ) );
			}
			return;

		case 772:
			// Saved by the Bell
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setBoolean( "_kolhsSchoolSpirited", true );
				Preferences.increment( "kolhsTotalSchoolSpirited", 1 );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setBoolean( "_kolhsPoeticallyLicenced", true );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				// You walk into the Yearbook Club and collar the kid with all
				// the camera equipment from yesterday. "Let me check your
				// memory card," he says, plugging the camera into a computer.
				// "Yup! You got it! Nice work. Here's your reward -- a nice
				// new accessory for that camera! If you're interested, now we
				// need a picture of a <b>monster</b>. You up for it?"
				//
				// You walk back into the Yearbook Club, a little tentatively.
				// "All right! Let's see what you've got!" the camera kid
				// says, and plugs your camera into a computer. "Aw, man, you
				// didn't get it? Well, I'll give you another chance.  If you
				// can still get us a picture of a <b>monster</b> and bring it
				// in tomorrow, you're still in the Club."
				//
				// You poke your head into the Yearbook Club room, but the
				// camera kid's packing up all the equipment and putting it
				// away. "Sorry, gotta go," he says, "but remember, you've
				// gotta get a picture of a <b>monster</b> for tomorrow, all
				// right? We're counting on you."

				if ( text.contains( "You got it!" ) )
				{
					Preferences.setString( "yearbookCameraTarget", "" );
					Preferences.setBoolean( "yearbookCameraPending", false );
					Preferences.increment( "yearbookCameraUpgrades", 1, 20, false );
					if ( KoLCharacter.getAscensions() != Preferences.getInteger( "lastYearbookCameraAscension" ) )
					{
						Preferences.setInteger( "lastYearbookCameraAscension", KoLCharacter.getAscensions() );
						Preferences.increment( "yearbookCameraAscensions", 1, 20, false );
					}
				}

				Matcher matcher = YEARBOOK_TARGET_PATTERN.matcher( text );
				if ( matcher.find() )
				{
					Preferences.setString( "yearbookCameraTarget", matcher.group( 1 ) );
				}
			}
			else if ( ChoiceManager.lastDecision == 4 )
			{
				Preferences.setBoolean( "_kolhsCutButNotDried", true );
			}
			else if ( ChoiceManager.lastDecision == 5 )
			{
				Preferences.setBoolean( "_kolhsIsskayLikeAnAshtray", true );
			}
			if ( ChoiceManager.lastDecision != 8 )
			{
				Preferences.increment( "_kolhsSavedByTheBell", 1 );
			}
			return;

		case 780:
			// Action Elevator
			if ( ChoiceManager.lastDecision == 1 && text.contains( "penthouse is empty now" ) )
			{
				if ( Preferences.getInteger( "hiddenApartmentProgress" ) < 7 )
				{
					Preferences.setInteger( "hiddenApartmentProgress", 7 );
				}
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				Preferences.setInteger( "relocatePygmyLawyer", KoLCharacter.getAscensions() );
			}
			return;

		case 781:
			// Earthbound and Down
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setInteger( "hiddenApartmentProgress", 1 );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.MOSS_COVERED_STONE_SPHERE, -1 ) );
				Preferences.setInteger( "hiddenApartmentProgress", 8 );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.SIX_BALL, -1 ) );
			}
			return;

		case 783:
			// Water You Dune
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setInteger( "hiddenHospitalProgress", 1 );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.DRIPPING_STONE_SPHERE, -1 ) );
				Preferences.setInteger( "hiddenHospitalProgress", 8 );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.TWO_BALL, -1 ) );
			}
			return;

		case 785:
			// Air Apparent
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setInteger( "hiddenOfficeProgress", 1 );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.CRACKLING_STONE_SPHERE, -1 ) );
				Preferences.setInteger( "hiddenOfficeProgress", 8 );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.ONE_BALL, -1 ) );
			}
			return;

		case 786:
			// Working Holiday
			if ( ChoiceManager.lastDecision == 1 && text.contains( "boss's office is empty" ) )
			{
				if ( Preferences.getInteger( "hiddenOfficeProgress" ) < 7 )
				{
					Preferences.setInteger( "hiddenOfficeProgress", 7 );
				}
			}
			// if you don't get the expected binder clip, don't have one, and don't have a mcclusky file, you must have unlocked the boss at least
			else if ( ChoiceManager.lastDecision == 2 && !text.contains( "boring binder clip" ) && 
				InventoryManager.getCount( ChoiceManager.MCCLUSKY_FILE ) == 0 && InventoryManager.getCount( ChoiceManager.BINDER_CLIP ) == 0 &&
				Preferences.getInteger( "hiddenOfficeProgress" ) < 6 )
			{
				Preferences.setInteger( "hiddenOfficeProgress", 6 );
			}
			return;

		case 787:
			// Fire when Ready
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setInteger( "hiddenBowlingAlleyProgress", 1 );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.SCORCHED_STONE_SPHERE, -1 ) );
				Preferences.setInteger( "hiddenBowlingAlleyProgress", 8 );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.FIVE_BALL, -1 ) );
			}
			return;

		case 788:
			// Life is Like a Cherry of Bowls
			if ( ChoiceManager.lastDecision == 1 && text.contains( "without a frustrated ghost to torment" ) )
			{
				if ( Preferences.getInteger( "hiddenBowlingAlleyProgress" ) < 7 )
				{
					Preferences.setInteger( "hiddenBowlingAlleyProgress", 7 );
				}
			}
			if ( ChoiceManager.lastDecision == 1 )
			{
				ResultProcessor.removeItem( ItemPool.BOWLING_BALL );
				int bowlCount = Preferences.getInteger( "hiddenBowlingAlleyProgress" );
				if ( bowlCount < 6 )
				{
					Preferences.setInteger( "hiddenBowlingAlleyProgress" , ( bowlCount < 2 ? 2 : bowlCount + 1 ) );
				}
			}
			return;

		case 789:
			// Where Does The Lone Ranger Take His Garbagester?
			if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setInteger( "relocatePygmyJanitor", KoLCharacter.getAscensions() );
			}
			return;

		case 801:
			// A Reanimated Conversation
			if ( ChoiceManager.lastDecision == 6 )
			{
				Preferences.setInteger( "reanimatorArms", 0 );
				Preferences.setInteger( "reanimatorLegs", 0 );
				Preferences.setInteger( "reanimatorSkulls", 0 );
				Preferences.setInteger( "reanimatorWeirdParts", 0 );
				Preferences.setInteger( "reanimatorWings", 0 );
			}
			return;		

		case 805:
			// A Sietch in Time
			int gnasirProgress = Preferences.getInteger( "gnasirProgress" );

			// Annoyingly, the option numbers change as you turn
			// things in. Therefore, we must look at response text

			if ( text.contains( "give the stone rose to Gnasir" ) )
			{
				ResultProcessor.removeItem( ItemPool.STONE_ROSE );
				gnasirProgress |= 1;
				Preferences.setInteger( "gnasirProgress", gnasirProgress );
			}
			else if ( text.contains( "hold up the bucket of black paint" ) )
			{
				ResultProcessor.removeItem( ItemPool.BLACK_PAINT );
				gnasirProgress |= 2;
				Preferences.setInteger( "gnasirProgress", gnasirProgress );
			}
			else if ( text.contains( "hand Gnasir the glass jar" ) )
			{
				ResultProcessor.removeItem( ItemPool.KILLING_JAR );
				gnasirProgress |= 4;
				Preferences.setInteger( "gnasirProgress", gnasirProgress );
			}
			// You hand him the pages, and he shuffles them into their correct order and inspects them carefully. 
			else if ( text.contains( "hand him the pages" ) )
			{
				ResultProcessor.processItem( ItemPool.WORM_RIDING_MANUAL_PAGE, -15 );
				gnasirProgress |= 8;
				Preferences.setInteger( "gnasirProgress", gnasirProgress );
			}
			return;

		case 812:
			if ( ChoiceManager.lastDecision == 1 )
			{
				Matcher matcher = ChoiceManager.UNPERM_PATTERN.matcher( ChoiceManager.lastResponseText );
				if ( matcher.find() )
				{
					KoLCharacter.removeAvailableSkill( matcher.group( 1 ) );
					Preferences.increment( "bankedKarma", Integer.parseInt( matcher.group( 2 ) ) );
				}
			}
			return;
		case 821:
			// LP-ROM burner
			if ( ChoiceManager.lastDecision == 1 )
			{
				HaciendaManager.parseRecording( urlString, text );
			}
			break;

		case 835:
			// Barely Tales
			if ( ChoiceManager.lastDecision != 0 )
			{
				Preferences.setBoolean( "_grimBuff", true );
			}
			break;

		case 836:
			// Adventures Who Live in Ice Houses...
			if ( ChoiceManager.lastDecision == 1 )
			{
				BanishManager.removeBanishByBanisher( "ice house" );
			}
			break;

		case 852:
			// Shen Copperhead, Jerk
		case 853:
			// Shen Copperhead, Huge Jerk
		case 854:
			// Shen Copperhead, World's Biggest Jerk

			// You will have exactly one of these items to ger rid of
			ResultProcessor.removeItem( ItemPool.FIRST_PIZZA );
			ResultProcessor.removeItem( ItemPool.LACROSSE_STICK );
			ResultProcessor.removeItem( ItemPool.EYE_OF_THE_STARS );
			ResultProcessor.removeItem( ItemPool.STANKARA_STONE );
			ResultProcessor.removeItem( ItemPool.MURPHYS_FLAG );
			ResultProcessor.removeItem( ItemPool.SHIELD_OF_BROOK );
			break;

		case 890:
			// Lights Out in the Storage Room
			if ( text.contains( "BUT AIN'T NO ONE CAN GET A STAIN OUT LIKE OLD AGNES!" ) )
			{
				Preferences.setString( "nextSpookyravenElizabethRoom", "The Haunted Laundry Room" );
			}
			break;

		case 891:
			// Lights Out in the Laundry Room
			if ( text.contains( "DO YOU SEE THE STAIN UPON MY TOWEL?" ) )
			{
				Preferences.setString( "nextSpookyravenElizabethRoom", "The Haunted Bathroom" );
			}
			break;

		case 892:
			// Lights Out in the Bathroom
			if ( text.contains( "THE STAIN HAS BEEN LIFTED" ) )
			{
				Preferences.setString( "nextSpookyravenElizabethRoom", "The Haunted Kitchen" );
			}
			break;

		case 893:
			// Lights Out in the Kitchen
			if ( text.contains( "If You Give a Demon a Brownie" ) )
			{
				Preferences.setString( "nextSpookyravenElizabethRoom", "The Haunted Library" );
			}
			break;

		case 894:
			// Lights Out in the Library
			if ( text.contains( "If You Give a Demon a Brownie" ) )
			{
				Preferences.setString( "nextSpookyravenElizabethRoom", "The Haunted Ballroom" );
			}
			break;

		case 895:
			// Lights Out in the Ballroom
			if ( text.contains( "The Flowerbed of Unearthly Delights" ) )
			{
				Preferences.setString( "nextSpookyravenElizabethRoom", "The Haunted Gallery" );
			}
			break;

		case 896:
			// Lights Out in the Gallery

			// The correct option leads to a combat with Elizabeth.
			// If you win, we will set "nextSpookyravenElizabethRoom" to "none"
			break;

		case 897:
			// Lights Out in the Bedroom
			if ( text.contains( "restock his medical kit in the nursery" ) )
			{
				Preferences.setString( "nextSpookyravenStephenRoom", "The Haunted Nursery" );
			}
			break;

		case 898:
			// Lights Out in the Nursery
			if ( text.contains( "This afternoon we're burying Crumbles" ) )
			{
				Preferences.setString( "nextSpookyravenStephenRoom", "The Haunted Conservatory" );
			}
			break;

		case 899:
			// Lights Out in the Conservatory
			if ( text.contains( "an engraved portrait of Crumbles" ) )
			{
				Preferences.setString( "nextSpookyravenStephenRoom", "The Haunted Billiards Room" );
			}
			break;

		case 900:
			// Lights Out in the Billiards Room
			if ( text.contains( "The wolf head has a particularly nasty expression on its face" ) )
			{
				Preferences.setString( "nextSpookyravenStephenRoom", "The Haunted Wine Cellar" );
			}
			break;

		case 901:
			// Lights Out in the Wine Cellar
			if ( text.contains( "Crumbles II (Wolf)" ) )
			{
				Preferences.setString( "nextSpookyravenStephenRoom", "The Haunted Boiler Room" );
			}
			break;

		case 902:
			// Lights Out in the Boiler Room
			if ( text.contains( "CRUMBLES II" ) )
			{
				Preferences.setString( "nextSpookyravenStephenRoom", "The Haunted Laboratory" );
			}
			break;

		case 903:
			// Lights Out in the Laboratory

			// The correct option leads to a combat with Stephen.
			// If you win, we will set "nextSpookyravenStephenRoom" to "none"
			break;

		case 915:
			// Choice 915 is Et Tu, Buff Jimmy?
			if ( text.contains( "skinny mushroom girls" ) )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_MUSHROOM, QuestDatabase.STARTED );
			}
			else if ( text.contains( "But here's a few Beach Bucks as a token of my changes in gratitude" ) )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_MUSHROOM, QuestDatabase.FINISHED );
				ResultProcessor.processItem( ItemPool.PENCIL_THIN_MUSHROOM, -10 );
			}
			else if ( text.contains( "not really into moving out of this hammock" ) )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_CHEESEBURGER, QuestDatabase.STARTED );
				Preferences.setInteger( "buffJimmyIngredients", 0 );
			}
			else if ( text.contains( "So I'll just give you some Beach Bucks instead" ) )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_CHEESEBURGER, QuestDatabase.FINISHED );
				Preferences.setInteger( "buffJimmyIngredients", 0 );
			}
			else if ( text.contains( "sons of sons of sailors are" ) )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_SALT, QuestDatabase.STARTED );
			}
			else if ( text.contains( "So here's some Beach Bucks instead" ) )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_SALT, QuestDatabase.FINISHED );
				ResultProcessor.processItem( ItemPool.SAILOR_SALT, -50 );
			}
			break;
		case 916:
			// Choice 916 is Taco Dan's Taco Stand's Taco Dan
			if ( text.contains( "find those receipts" ) )
			{
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_AUDIT, QuestDatabase.STARTED );
			}
			else if ( text.contains( "Here's a little Taco Dan's Taco Stand gratitude for ya" ) )
			{
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_AUDIT, QuestDatabase.FINISHED );
				ResultProcessor.processItem( ItemPool.TACO_DAN_RECEIPT, -10 );
			}
			else if ( text.contains( "fill it up with as many cocktail drippings" ) )
			{
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_COCKTAIL, QuestDatabase.STARTED );
				Preferences.setInteger( "tacoDanCocktailSauce", 0 );
			}
			else if ( text.contains( "sample of Taco Dan's Taco Stand's Tacoriffic Cocktail Sauce" ) )
			{
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_COCKTAIL, QuestDatabase.FINISHED );
				Preferences.setInteger( "tacoDanCocktailSauce", 0 );
				ResultProcessor.processItem( ItemPool.TACO_DAN_SAUCE_BOTTLE, -1 );
			}
			else if ( text.contains( "get enough taco fish" ) )
			{
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_FISH, QuestDatabase.STARTED );
				Preferences.setInteger( "tacoDanFishMeat", 0 );
			}
			else if ( text.contains( "batch of those Taco Dan's Taco Stand's Taco Fish Tacos" ) )
			{
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_FISH, QuestDatabase.FINISHED );
				Preferences.setInteger( "tacoDanFishMeat", 0 );
			}
			break;
		case 917:
			// Choice 917 is Do You Even Brogurt
			if ( text.contains( "need about ten shots of it" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BRODEN_BACTERIA, QuestDatabase.STARTED );
				Preferences.setInteger( "brodenBacteria", 0 );
			}
			else if ( text.contains( "YOLO cup to spit the bacteria into" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BRODEN_BACTERIA, QuestDatabase.FINISHED );
				Preferences.setInteger( "brodenBacteria", 0 );
			}
			else if ( text.contains( "loan you my sprinkle shaker to fill up" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BRODEN_SPRINKLES, QuestDatabase.STARTED );
				Preferences.setInteger( "brodenSprinkles", 0 );
			}
			else if ( text.contains( "can sell some deluxe brogurts" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BRODEN_SPRINKLES, QuestDatabase.FINISHED );
				Preferences.setInteger( "brodenSprinkles", 0 );
				ResultProcessor.processItem( ItemPool.SPRINKLE_SHAKER, -1 );
			}
			else if ( text.contains( "There were like fifteen of these guys" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BRODEN_DEBT, QuestDatabase.STARTED );
			}
			else if ( text.contains( "And they all had broupons, huh" ) )
			{
				QuestDatabase.setQuestProgress( Quest.BRODEN_DEBT, QuestDatabase.FINISHED );
				ResultProcessor.processItem( ItemPool.BROUPON, -15 );
			}
			break;
		case 918:
			// Yachtzee!
			if ( ChoiceManager.lastDecision == 3 && text.contains( "You open the captain's door" ) )
			{
				int beads = Math.min( InventoryManager.getCount( ItemPool.MOIST_BEADS ), 100 );
				ResultProcessor.processResult( ItemPool.get( ItemPool.MOIST_BEADS, -beads ) );
			}
			break;
		case 919:
			// Choice 919 is Break Time!
			if ( ChoiceManager.lastDecision == 1 )
			{
				if ( text.contains( "You've already thoroughly" ) )
				{
					Preferences.setInteger( "_sloppyDinerBeachBucks", 4 );
				}
				else
				{
					Preferences.increment( "_sloppyDinerBeachBucks", 1 );
				}
			}
			break;
		case 920:
			// Choice 920 is Eraser
			if ( ChoiceManager.lastDecision == 1 )
			{
				QuestDatabase.setQuestProgress( Quest.JIMMY_MUSHROOM, QuestDatabase.UNSTARTED );
				QuestDatabase.setQuestProgress( Quest.JIMMY_CHEESEBURGER, QuestDatabase.UNSTARTED );
				QuestDatabase.setQuestProgress( Quest.JIMMY_SALT, QuestDatabase.UNSTARTED );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_AUDIT, QuestDatabase.UNSTARTED );
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_COCKTAIL, QuestDatabase.UNSTARTED );
				QuestDatabase.setQuestProgress( Quest.TACO_DAN_FISH, QuestDatabase.UNSTARTED );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				QuestDatabase.setQuestProgress( Quest.BRODEN_BACTERIA, QuestDatabase.UNSTARTED );
				QuestDatabase.setQuestProgress( Quest.BRODEN_SPRINKLES, QuestDatabase.UNSTARTED );
				QuestDatabase.setQuestProgress( Quest.BRODEN_DEBT, QuestDatabase.UNSTARTED );
			}
			if ( ChoiceManager.lastDecision != 4 )
			{
				ResultProcessor.processItem( ItemPool.MIND_DESTROYER, -1 );
			}
			break;
		}

		// Certain choices cost meat or items when selected
		ChoiceManager.payCost( ChoiceManager.lastChoice, ChoiceManager.lastDecision );
	}

	// <td align="center" valign="middle"><a href="choice.php?whichchoice=810&option=1&slot=7&pwd=xxx" style="text-decoration:none"><img alt='Toybot (Level 3)' title='Toybot (Level 3)' border=0 src='http://images.kingdomofloathing.com/otherimages/crimbotown/krampus_toybot.gif' /></a></td>
	private static final Pattern URL_SLOT_PATTERN = Pattern.compile( "slot=(\\d+)" );
	private static final Pattern BOT_PATTERN = Pattern.compile( "<td.*?<img alt='([^']*)'.*?</td>" );

	private static String findKRAMPUSBot( final String urlString, final String responseText )
	{
		Matcher slotMatcher = ChoiceManager.URL_SLOT_PATTERN.matcher( urlString );
		if ( !slotMatcher.find() )
		{
			return null;
		}
		String slotString = slotMatcher.group( 0 );
		Matcher botMatcher = ChoiceManager.BOT_PATTERN.matcher( responseText );
		while ( botMatcher.find() )
		{
			if ( botMatcher.group( 0 ).contains( slotString ) )
			{
				return botMatcher.group( 1 );
			}
		}
		return null;
	}

	public static void postChoice2( final GenericRequest request )
	{
		// Things that can or need to be done AFTER processing results.

		String text = request.responseText;
		ChoiceManager.handlingChoice = text.contains( "choice.php" );

		if ( ChoiceManager.lastChoice == 0 || ChoiceManager.lastDecision == 0 )
		{
			// This was a visit
			return;
		}

		switch ( ChoiceManager.lastChoice )
		{
		case 3:
			// The Oracle Will See You Now
			if ( text.contains( "actually a book" ) )
			{
				Preferences.setInteger( "lastPlusSignUnlock", KoLCharacter.getAscensions() );
			}
			break;
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

		case 73:
			// Don't Fence Me In
			if ( ChoiceManager.lastDecision == 3 )
			{
				if ( text.contains( "you pick" ) || text.contains( "you manage" ) )
				{
					Preferences.increment( "_whiteRiceDrops", 1 );
				}
			}
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

		case 89:
			if ( ChoiceManager.lastDecision == 4 )
			{
				TurnCounter.startCounting( 10, "Garden Banished loc=*", "wolfshield.gif" );
			}
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
			if ( ChoiceManager.lastDecision == 2  )
			{
				QuestDatabase.setQuestProgress( Quest.WORSHIP, "step1" );
			}
			break;
		case 125:
			// No visible means of support
			if ( ChoiceManager.lastDecision == 3  )
			{
				QuestDatabase.setQuestProgress( Quest.WORSHIP, "step3" );
			}
			break;

		case 132:
			// Let's Make a Deal!
			if ( ChoiceManager.lastDecision == 2 )
			{
				QuestDatabase.setQuestProgress( Quest.PYRAMID, "step1" );
			}
			break;

		case 162:
			// Between a Rock and Some Other Rocks
			if ( KoLmafia.isAdventuring() && !EquipmentManager.isWearingOutfit( OutfitPool.MINING_OUTFIT ) && !KoLConstants.activeEffects.contains( SorceressLairManager.EARTHEN_FIST ) )
			{
				QuestManager.unlockGoatlet();
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
				KoLmafia.updateDisplay( MafiaState.PENDING, ChoiceManager.hobopolisBossName( ChoiceManager.lastChoice ) + " waits for you." );
			}
			break;

		case 299:
			// Down at the Hatch
			if ( ChoiceManager.lastDecision == 2 )
			{
				// The first time you take option 2, you
				// release Big Brother. Subsequent times, you
				// release other creatures.
				Preferences.setBoolean( "bigBrotherRescued", true );
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
				KoLmafia.updateDisplay( MafiaState.ABORT,
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
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, "step3" );
			break;

		case 518:
			// Clear and Present Danger

			// Stop for Hobopolis bosses
			if ( ChoiceManager.lastDecision == 2 && KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, ChoiceManager.hobopolisBossName( ChoiceManager.lastChoice ) + " waits for you." );
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
				KoLmafia.updateDisplay( MafiaState.ABORT,
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

		case 578:
			// End of the Boris Road
			ChoiceManager.handleAfterAvatar();
			break;

		case 579:
			// Such Great Heights
			if ( ChoiceManager.lastDecision == 3 && Preferences.getInteger( "lastTempleAdventures" ) != KoLCharacter.getAscensions())
			{
				Preferences.setInteger( "lastTempleAdventures", KoLCharacter.getAscensions() );
			}
			break;

		case 581:
			// Such Great Depths
			if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setBoolean( "_templeHiddenPower", true );
			}
			break;

		case 584:
			// Unconfusing Buttons
			if ( Preferences.getInteger( "lastTempleButtonsUnlock" ) != KoLCharacter.getAscensions() )
			{
				Preferences.setInteger( "lastTempleButtonsUnlock", KoLCharacter.getAscensions() );
			}
			if ( InventoryManager.getCount( ItemPool.NOSTRIL_OF_THE_SERPENT ) > 0 )
			{
				ResultProcessor.processItem( ItemPool.NOSTRIL_OF_THE_SERPENT, -1 );
			}
			break;

		case 595:
			// Fire! I... have made... fire!
			Preferences.setBoolean( "_fireStartingKitUsed", true );
			break;

		case 602:
			// Behind the Gash
			// This is a multi-part choice adventure, and we only want to handle the last choice
			if ( text.contains( "you shout into the blackness" ) )
			{
				ChoiceManager.handleAfterAvatar();
			}
			break;

		case 613:
			// Behind the door there is a fog
			if ( ChoiceManager.lastDecision == 1 )
			{
				Matcher fogMatcher = FOG_PATTERN.matcher( text );
				if ( fogMatcher.find() )
				{
					String message = "Message: \"" + fogMatcher.group(1) + "\"";
					RequestLogger.printLine( message );
					RequestLogger.updateSessionLog( message );
				}
			}
			break;

		case 633:
			// ChibiBuddy&trade;
			if ( ChoiceManager.lastDecision == 1 )
			{
				ResultProcessor.processItem( ItemPool.CHIBIBUDDY_OFF, -1 );
				ResultProcessor.processItem( ItemPool.CHIBIBUDDY_ON, 1 );
			}
			break;

		case 641:
			// Stupid Pipes.
			if ( ChoiceManager.lastDecision == 2 && text.contains( "flickering pixel" ) )
			{
				Preferences.setBoolean( "flickeringPixel1", true );
			}
			break;

		case 642:
			// You're Freaking Kidding Me
			if ( ChoiceManager.lastDecision == 2 && text.contains( "flickering pixel" ) )
			{
				Preferences.setBoolean( "flickeringPixel2", true );
			}
			break;

		case 644:
			// Snakes.
			if ( ChoiceManager.lastDecision == 2 && text.contains( "flickering pixel" ) )
			{
				Preferences.setBoolean( "flickeringPixel3", true );
			}
			break;

		case 645:
			// So... Many... Skulls...
			if ( ChoiceManager.lastDecision == 2 && text.contains( "flickering pixel" ) )
			{
				Preferences.setBoolean( "flickeringPixel4", true );
			}
			break;

		case 647:
			// A Stupid Dummy. Also, a Straw Man.
			if ( ChoiceManager.lastDecision == 2 && text.contains( "flickering pixel" ) )
			{
				Preferences.setBoolean( "flickeringPixel5", true );
			}
			break;

		case 648:
			// Slings and Arrows
			if ( ChoiceManager.lastDecision == 2 && text.contains( "flickering pixel" ) )
			{
				Preferences.setBoolean( "flickeringPixel6", true );
			}
			break;

		case 650:
			// This Is Your Life. Your Horrible, Horrible Life.
			if ( ChoiceManager.lastDecision == 2 && text.contains( "flickering pixel" ) )
			{
				Preferences.setBoolean( "flickeringPixel7", true );
			}
			break;

		case 651:
			// The Wall of Wailing
			if ( ChoiceManager.lastDecision == 2 && text.contains( "flickering pixel" ) )
			{
				Preferences.setBoolean( "flickeringPixel8", true );
			}
			break;

		case 677:
			// Copper Feel
			if ( ChoiceManager.lastDecision == 1 )
			{
				ResultProcessor.removeItem( ItemPool.MODEL_AIRSHIP );
			}
			break;

		case 682:
			// Now Leaving Jarlsberg, Population You
			ChoiceManager.handleAfterAvatar();
			break;

		case 704:
			// Playing the Catalog Card
			DreadScrollManager.handleLibrary( text );
			break;

		case 774:
			// Opening up the Folder Holder

			// Choice 1 is adding a folder.
			if ( ChoiceManager.lastDecision == 1 && text.contains( "You carefully place your new folder in the holder" ) )
			{
				// Figure out which one it was from the URL
				String id = request.getFormField( "folder" );
				AdventureResult folder = EquipmentRequest.idToFolder( id );
				ResultProcessor.removeItem( folder.getItemId() );
			}

			// Choice 2 is removing a folder. Since the folder is
			// destroyed, it does not go back to inventory.

			// Set all folder slots from the response text
			EquipmentRequest.parseFolders( text );
			break;

		case 786:
			if ( ChoiceManager.lastDecision == 2 && Preferences.getBoolean( "autoCraft" ) && text.contains( "boring binder clip" ) &&
			     InventoryManager.getCount( ItemPool.MCCLUSKY_FILE_PAGE5 ) == 1 )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.BINDER_CLIP ) );
			}
			break;
			
		case 810:
			if ( ChoiceManager.lastDecision == 2 )
			{
				ResultProcessor.processItem( ItemPool.WARBEAR_WHOSIT, -100 );
			}
			else if ( ChoiceManager.lastDecision == 4 && text.contains( "You upgrade the robot!" ) )
			{
				String bot = ChoiceManager.findKRAMPUSBot( request.getURLString(), text );
				int cost =
					( bot == null ) ? 0 :
					bot.contains( "Level 2" ) ? 250 :
					bot.contains( "Level 3" ) ? 500 :
					0;
				if ( cost != 0 )
				{
					ResultProcessor.processItem( ItemPool.WARBEAR_WHOSIT, -cost );
				}
			}
			break;

		case 822:
		case 823:
		case 824:
		case 825:
		case 826:
		case 827:
			// The Prince's Ball
			if ( ChoiceManager.parseCinderellaTime() == false )
			{
				Preferences.decrement( "cinderellaMinutesToMidnight" );
			}
			Matcher matcher = ChoiceManager.CINDERELLA_SCORE_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				int score = StringUtilities.parseInt( matcher.group( 1 ) );
				if ( score != -1 )
				{
					Preferences.setInteger( "cinderellaScore", score );
				}
			}
			if ( text.contains( "Your final score was" ) )
			{
				Preferences.setInteger( "cinderellaMinutesToMidnight", 0 );
				Preferences.setString( "grimstoneMaskPath", "" );
			}
			break;
			
		case 829:
			// We all wear masks
			if ( ChoiceManager.lastDecision != 6 )
			{
				ResultProcessor.processItem( ItemPool.GRIMSTONE_MASK, -1 );
				Preferences.setInteger( "cinderellaMinutesToMidnight", 0 );
			}
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setInteger( "cinderellaMinutesToMidnight", 30 );
				Preferences.setInteger( "cinderellaScore", 0 );
				Preferences.setString( "grimstoneMaskPath", "stepmother" );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setString( "grimstoneMaskPath", "wolf" );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setString( "grimstoneMaskPath", "witch" );
			}
			else if ( ChoiceManager.lastDecision == 4 )
			{
				// We lose all Rumpelstiltskin ingredients
				int straw = InventoryManager.getCount( ItemPool.STRAW );
				int leather = InventoryManager.getCount( ItemPool.LEATHER );
				int clay = InventoryManager.getCount( ItemPool.CLAY );
				int filling = InventoryManager.getCount( ItemPool.FILLING );
				int parchment = InventoryManager.getCount( ItemPool.PARCHMENT );
				int glass = InventoryManager.getCount( ItemPool.GLASS );
				if ( straw > 0 )
				{
					ResultProcessor.processItem( ItemPool.STRAW, -straw );
				}
				if ( leather > 0 )
				{
					ResultProcessor.processItem( ItemPool.LEATHER, -leather );
				}
				if ( clay > 0 )
				{
					ResultProcessor.processItem( ItemPool.CLAY, -clay );
				}
				if ( filling > 0 )
				{
					ResultProcessor.processItem( ItemPool.FILLING, -filling );
				}
				if ( parchment > 0 )
				{
					ResultProcessor.processItem( ItemPool.PARCHMENT, -parchment );
				}
				if ( glass > 0 )
				{
					ResultProcessor.processItem( ItemPool.GLASS, -glass );
				}
				// Reset score
				Preferences.setInteger( "rumpelstiltskinTurnsUsed", 0 );
				Preferences.setInteger( "rumpelstiltskinKidsRescued", 0 );
				Preferences.setString( "grimstoneMaskPath", "gnome" );
			}
			else if ( ChoiceManager.lastDecision == 5 )
			{
				Preferences.setString( "grimstoneMaskPath", "hare" );
			}
			break;
	
		case 844:
			// The Portal to Horrible Parents
			if ( ChoiceManager.lastDecision == 1 )
			{
				RumpleManager.spyOnParents( text );
			}
			break;
	
		case 846:
			// Bartering for the Future of Innocent Children
			RumpleManager.pickParent( text );
			break;

		case 847:
			// Pick Your Poison
			RumpleManager.pickSin( text );
			break;
	
		case 848:
			// Where the Magic Happens
			if ( ChoiceManager.lastDecision != 4 )
			{
				RumpleManager.recordTrade( text );
			}
			break;
		
		case 860:
			// Another Tired Retread
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setString( "peteMotorbikeTires", "Racing Slicks" );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setString( "peteMotorbikeTires", "Spiky Tires" );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				Preferences.setString( "peteMotorbikeTires", "Snow Tires" );
			}
			break;
		case 861:
			// Station of the Gas
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setString( "peteMotorbikeGasTank", "Large Capacity Tank" );
				Preferences.setInteger( "lastDesertUnlock", KoLCharacter.getAscensions() );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setString( "peteMotorbikeGasTank", "Extra-Buoyant Tank" );
				Preferences.setInteger( "lastIslandUnlock", KoLCharacter.getAscensions() );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				Preferences.setString( "peteMotorbikeGasTank", "Nitro-Burnin' Funny Tank" );
			}
			break;
		case 862:
			// Me and Cinderella Put It All Together
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setString( "peteMotorbikeHeadlight", "Ultrabright Yellow Bulb" );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setString( "peteMotorbikeHeadlight", "Party Bulb" );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				Preferences.setString( "peteMotorbikeHeadlight", "Blacklight Bulb" );
			}
			break;
		case 863:
			// Endowing the Cowling
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setString( "peteMotorbikeCowling", "Ghost Vacuum" );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setString( "peteMotorbikeCowling", "Rocket Launcher" );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				Preferences.setString( "peteMotorbikeCowling", "Sweepy Red Light" );
			}
			break;
		case 864:
			// Diving into the Mufflers
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setString( "peteMotorbikeMuffler", "Extra-Loud Muffler" );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setString( "peteMotorbikeMuffler", "Extra-Quiet Muffler" );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				Preferences.setString( "peteMotorbikeMuffler", "Extra-Smelly Muffler" );
			}
			break;
		case 865:
			// Ayy, Sit on It
			if ( ChoiceManager.lastDecision == 1 )
			{
				Preferences.setString( "peteMotorbikeSeat", "Massage Seat" );
			}
			else if ( ChoiceManager.lastDecision == 2 )
			{
				Preferences.setString( "peteMotorbikeSeat", "Deep Seat Cushions" );
			}
			else if ( ChoiceManager.lastDecision == 3 )
			{
				Preferences.setString( "peteMotorbikeSeat", "Sissy Bar" );
			}
			break;

		case 869:
			// End of Pete Road
			ChoiceManager.handleAfterAvatar();
			break;

		case 875:
			// Welcome To Our ool Table
			Matcher poolSkillMatcher = ChoiceManager.POOL_SKILL_PATTERN.matcher( text );
			if ( poolSkillMatcher.find() )
			{
				Preferences.increment( "poolSkill", StringUtilities.parseInt( poolSkillMatcher.group( 1 ) ) );
			}
			break;

		case 928:
			// The Blackberry Cobbler
			if ( ChoiceManager.lastDecision != 6 )
			{
				ResultProcessor.processItem( ItemPool.BLACKBERRY, -3 );
			}
		}

		if ( text.contains( "choice.php" ) )
		{
			ChoiceManager.visitChoice( request );
			return;
		}

		PostChoiceAction action = ChoiceManager.action;
		if ( action != PostChoiceAction.NONE )
		{
			ChoiceManager.action = PostChoiceAction.NONE;
			switch ( action )
			{
			case INITIALIZE:
				LoginManager.login( KoLCharacter.getUserName() );
				break;
			case ASCEND:
				ValhallaManager.postAscension();
				break;
			}
		}
	}

	private static void handleAfterAvatar()
	{
		String newClass = "Unknown";
			switch ( ChoiceManager.lastDecision )
			{
			case 1:
				newClass = "Seal Clubber";
				break;
			case 2:
				newClass = "Turtle Tamer";
				break;
			case 3:
				newClass = "Pastamancer";
				break;
			case 4:
				newClass = "Sauceror";
				break;
			case 5:
				newClass = "Disco Bandit";
				break;
			case 6:
				newClass = "Accordion Thief";
				break;
			}

		StringBuilder buffer = new StringBuilder();
		buffer.append( "Now walking on the " );
		buffer.append( newClass );
		buffer.append( " road." );

		String message = buffer.toString();

		KoLmafia.updateDisplay( message );
		RequestLogger.updateSessionLog( message );

		KoLmafia.resetAfterAvatar();
	}

	private static void visitChoice( final GenericRequest request )
	{
		ChoiceManager.lastChoice = ChoiceManager.extractChoice( request.responseText );
		ChoiceManager.lastResponseText = request.responseText;

		if ( ChoiceManager.lastChoice == 0 )
		{
			// choice.php did not offer us any choices.
			// This would be a bug in KoL itself.
			return;
		}

		ChoiceManager.setCanWalkAway( ChoiceManager.lastChoice );

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
			WumpusManager.visitChoice( ChoiceManager.lastResponseText );
			break;

		case 440:
			// Puttin' on the Wax
			HaciendaManager.preRecording( ChoiceManager.lastResponseText );
			break;

		case 460:
			// Space Trip
			ArcadeRequest.visitSpaceTripChoice( ChoiceManager.lastResponseText );
			break;

		case 471:
			// DemonStar
			ArcadeRequest.visitDemonStarChoice( ChoiceManager.lastResponseText );
			break;

		case 485:
			// Fighters Of Fighting
			ArcadeRequest.visitFightersOfFightingChoice( ChoiceManager.lastResponseText );
			break;

		case 486:
			// DungeonFist!
			ArcadeRequest.visitDungeonFistChoice( ChoiceManager.lastResponseText );
			break;

		case 488:
			// Meteoid
			ArcadeRequest.visitMeteoidChoice( ChoiceManager.lastResponseText );
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
			SpaaaceRequest.visitPorkoChoice( ChoiceManager.lastResponseText );
			break;

		case 540:
			// Big-Time Generator
			SpaaaceRequest.visitGeneratorChoice( ChoiceManager.lastResponseText );
			break;

		case 570:
			GameproManager.parseGameproMagazine( ChoiceManager.lastResponseText );
			break;

		case 641:
			// Stupid Pipes.
			if ( !ChoiceManager.lastResponseText.contains( "Dive Down" ) && 
			     KoLCharacter.getElementalResistanceLevels( Element.HOT ) >= 25 )
			{
				Preferences.setBoolean( "flickeringPixel1", true );
			}
			break;

		case 642:
			// You're Freaking Kidding Me
			if ( !ChoiceManager.lastResponseText.contains( "Wait a minute..." ) &&
			     KoLCharacter.getAdjustedMuscle() >= 500 && 
			     KoLCharacter.getAdjustedMysticality() >= 500 &&
			     KoLCharacter.getAdjustedMoxie() >= 500 )
			{
				Preferences.setBoolean( "flickeringPixel2", true );
			}
			break;

		case 644:
			// Snakes.
			if ( !ChoiceManager.lastResponseText.contains( "Tie the snakes in a knot." ) &&
			     KoLCharacter.getAdjustedMoxie() >= 300 )
			{
				Preferences.setBoolean( "flickeringPixel3", true );
			}
			break;

		case 645:
			// So... Many... Skulls...
			if ( !ChoiceManager.lastResponseText.contains( "You fear no evil" ) &&
			     KoLCharacter.getElementalResistanceLevels( Element.SPOOKY ) >= 25 )
			{
				Preferences.setBoolean( "flickeringPixel4", true );
			}
			break;

		case 647:
			// A Stupid Dummy. Also, a Straw Man.

			// *** unspaded
			if ( !ChoiceManager.lastResponseText.contains( "Graaaaaaaaargh!" ) &&
			     KoLCharacter.currentBonusDamage() >= 1000 )
			{
				Preferences.setBoolean( "flickeringPixel5", true );
			}
			break;

		case 648:
			// Slings and Arrows
			// *** Yes, there supposed to be two spaces there.
			if ( !ChoiceManager.lastResponseText.contains( "Arrows?  Ha." ) &&
			     KoLCharacter.getCurrentHP() >= 1000 )
			{
				Preferences.setBoolean( "flickeringPixel6", true );
			}
			break;

		case 650:
			// This Is Your Life. Your Horrible, Horrible Life.
			if ( !ChoiceManager.lastResponseText.contains( "Then watch it again with the commentary on!" ) &&
			     KoLCharacter.getCurrentMP() >= 1000 )
			{
				Preferences.setBoolean( "flickeringPixel7", true );
			}
			break;

		case 651:
			// The Wall of Wailing
			if ( !ChoiceManager.lastResponseText.contains( "Make the tide resist you" ) &&
			     KoLCharacter.currentPrismaticDamage() >= 60 )
			{
				Preferences.setBoolean( "flickeringPixel8", true );
			}

			break;

		case 658:
			// Debasement
			ResultProcessor.processItem( ItemPool.GOLD_PIECE, -30 );
			break;

		case 689:
			// The Final Reward
			Preferences.setInteger( "_lastDailyDungeonRoom", 14 );
			break;

		case 690:
			// The First Chest Isn't the Deepest
			Preferences.setInteger( "_lastDailyDungeonRoom", 4 );
			break;

		case 691:
			// Second Chest
			Preferences.setInteger( "_lastDailyDungeonRoom", 9 );
			break;

		case 692:
		case 693:
			// I Wanna Be a Door and It's Almost Certainly a Trap
			Matcher chamberMatcher = ChoiceManager.CHAMBER_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( chamberMatcher.find() )
			{
				int round = StringUtilities.parseInt( chamberMatcher.group( 1 ) );
				Preferences.setInteger( "_lastDailyDungeonRoom", round - 1 );
			}
			break;

		case 705:
			// Halls Passing in the Night
			ResultProcessor.processItem( ItemPool.MERKIN_HALLPASS, -1 );
			break;

		case 764:
			// The Machine

			// You approach The Machine, and notice that the
			// capacitor you're carrying fits perfectly into an
			// obviously empty socket on the base of it. You plug
			// it in, and The Machine whirs ominously to life.......

			if ( ChoiceManager.lastResponseText.contains( "You plug it in" ) )
			{
				ResultProcessor.processResult( ItemPool.get( ItemPool.SKULL_CAPACITOR, -1 ) );
			}
			break;

		case 774:
			// Opening up the Folder Holder

			String option = request.getFormField( "forceoption" );
			if ( option != null )
			{
				ChoiceManager.lastDecision = StringUtilities.parseInt( option );
			}
			break;
			
		case 801:
		{
			// A Reanimated Conversation
			Matcher matcher = ChoiceManager.REANIMATOR_ARM_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setInteger( "reanimatorArms", StringUtilities.parseInt( matcher.group(1) ) );
			}
			else
			{
				Preferences.setInteger( "reanimatorArms", 0 );
			}
			matcher = ChoiceManager.REANIMATOR_LEG_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setInteger( "reanimatorLegs", StringUtilities.parseInt( matcher.group(1) ) );
			}
			else
			{
				Preferences.setInteger( "reanimatorLegs", 0 );
			}
			matcher = ChoiceManager.REANIMATOR_SKULL_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setInteger( "reanimatorSkulls", StringUtilities.parseInt( matcher.group(1) ) );
			}
			else
			{
				Preferences.setInteger( "reanimatorSkulls", 0 );
			}
			matcher = ChoiceManager.REANIMATOR_WEIRDPART_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setInteger( "reanimatorWeirdParts", StringUtilities.parseInt( matcher.group(1) ) );
			}
			else
			{
				Preferences.setInteger( "reanimatorWeirdParts", 0 );
			}
			matcher = ChoiceManager.REANIMATOR_WING_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setInteger( "reanimatorWings", StringUtilities.parseInt( matcher.group(1) ) );
			}
			else
			{
				Preferences.setInteger( "reanimatorWings", 0 );
			}
			break;
		}

		case 836:
		{
			// Adventures Who Live in Ice Houses...
			Matcher matcher = ChoiceManager.ICEHOUSE_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				String icehouseMonster = matcher.group( 1 ).toLowerCase();
				String knownBanishes = Preferences.getString( "banishedMonsters" );
				if ( !knownBanishes.contains( icehouseMonster ) )
				{
					// If not already known to be banished, add it
					BanishManager.banishMonster( icehouseMonster, "ice house" );
				}
			}
			break;
		}

		case 822:
		case 823:
		case 824:
		case 825:
		case 826:
		case 827:
			// The Prince's Ball
			ChoiceManager.parseCinderellaTime();
			Preferences.setString( "grimstoneMaskPath", "stepmother" );
			break;

		case 848:
		case 849:
		case 850:
		{
			// Where the Magic Happens & The Practice & World of Bartercraft
			Preferences.setString( "grimstoneMaskPath", "gnome" );
			// Update remaining materials
			Matcher matcher = ChoiceManager.RUMPLE_MATERIAL_PATTERN.matcher( ChoiceManager.lastResponseText );
			while ( matcher.find() )
			{
				String material = matcher.group( 1 );
				int number = StringUtilities.parseInt( matcher.group( 2 ) );
				if ( material.equals( "straw" ) )
				{
					int straw = InventoryManager.getCount( ItemPool.STRAW );
					if ( straw != number )
					{
						ResultProcessor.processItem( ItemPool.STRAW, number - straw );
					}
				}
				else if ( material.equals( "leather" ) )
				{
					int leather = InventoryManager.getCount( ItemPool.LEATHER );
					if ( leather != number )
					{
						ResultProcessor.processItem( ItemPool.LEATHER, number - leather );
					}
				}
				else if ( material.equals( "clay" ) )
				{
					int clay = InventoryManager.getCount( ItemPool.CLAY );
					if ( clay != number )
					{
						ResultProcessor.processItem( ItemPool.CLAY, number - clay );
					}
				}
				else if ( material.equals( "filling" ) )
				{
					int filling = InventoryManager.getCount( ItemPool.FILLING );
					if ( filling != number )
					{
						ResultProcessor.processItem( ItemPool.FILLING, number - filling );
					}
				}
				else if ( material.equals( "parchment" ) )
				{
					int parchment = InventoryManager.getCount( ItemPool.PARCHMENT );
					if ( parchment != number )
					{
						ResultProcessor.processItem( ItemPool.PARCHMENT, number - parchment );
					}
				}
				else if ( material.equals( "glass" ) )
				{
					int glass = InventoryManager.getCount( ItemPool.GLASS );
					if ( glass != number )
					{
						ResultProcessor.processItem( ItemPool.GLASS, number - glass );
					}
				}
			}
			break;
		}

		case 871:
		{
			// inspecting Motorbike
			Matcher matcher = ChoiceManager.MOTORBIKE_TIRES_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setString( "peteMotorbikeTires", matcher.group( 1 ).trim() );
			}
			matcher = ChoiceManager.MOTORBIKE_GASTANK_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setString( "peteMotorbikeGasTank", matcher.group( 1 ).trim() );
				if ( Preferences.getString( "peteMotorbikeGasTank" ).equals( "Large Capacity Tank" ) )
				{
					Preferences.setInteger( "lastDesertUnlock", KoLCharacter.getAscensions() );
				}
				else if ( Preferences.getString( "peteMotorbikeGasTank" ).equals( "Extra-Buoyant Tank" ) )
				{
					Preferences.setInteger( "lastIslandUnlock", KoLCharacter.getAscensions() );
				}
			}
			matcher = ChoiceManager.MOTORBIKE_HEADLIGHT_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setString( "peteMotorbikeHeadlight", matcher.group( 1 ).trim() );
			}
			matcher = ChoiceManager.MOTORBIKE_COWLING_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setString( "peteMotorbikeCowling", matcher.group( 1 ).trim() );
			}
			matcher = ChoiceManager.MOTORBIKE_MUFFLER_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setString( "peteMotorbikeMuffler", matcher.group( 1 ).trim() );
			}
			matcher = ChoiceManager.MOTORBIKE_SEAT_PATTERN.matcher( ChoiceManager.lastResponseText );
			if ( matcher.find() )
			{
				Preferences.setString( "peteMotorbikeSeat", matcher.group( 1 ).trim() );
			}
			break;
		}
		
		case 890: // Lights Out in the Storage Room
		case 891: // Lights Out in the Laundry Room
		case 892: // Lights Out in the Bathroom
		case 893: // Lights Out in the Kitchen
		case 894: // Lights Out in the Library
		case 895: // Lights Out in the Ballroom
		case 896: // Lights Out in the Gallery
		case 897: // Lights Out in the Bedroom
		case 898: // Lights Out in the Nursery
		case 899: // Lights Out in the Conservatory
		case 900: // Lights Out in the Billiards Room
		case 901: // Lights Out in the Wine Cellar
		case 902: // Lights Out in the Boiler Room
		case 903: // Lights Out in the Laboratory
			// Remove the counter if it exists so a new one can be made
			// as soon as the next adventure is started
			TurnCounter.stopCounting( "Spookyraven Lights Out" );
			break;
		}
	}

	private static String booPeakDamage()
	{
		int damageTaken = 0;
		int diff = 0;
		String decisionText = ChoiceManager.findChoiceDecisionText( 1, ChoiceManager.lastResponseText );

		int booPeakLevel = ChoiceManager.findBooPeakLevel( decisionText );

		if ( booPeakLevel < 1 )
			return "";

		switch ( booPeakLevel )
		{
		case 1:
			// actual base damage is 13
			damageTaken = 30;
			diff = 17;
			break;
		case 2:
			// actual base damage is 25
			damageTaken = 30;
			diff = 5;
			break;
		case 3:
			damageTaken = 50;
			break;
		case 4:
			damageTaken = 125;
			break;
		case 5:
			damageTaken = 250;
			break;
		}

		double spookyDamage = KoLConstants.activeEffects.contains( EffectPool.get( Effect.SPOOKYFORM ) ) ? 1.0 :
			  Math.max( damageTaken * ( 100.0 - KoLCharacter.elementalResistanceByLevel( KoLCharacter.getElementalResistanceLevels( Element.SPOOKY ) ) ) / 100.0 - diff, 1 );
		if ( KoLConstants.activeEffects.contains( EffectPool.get( Effect.COLDFORM ) ) || KoLConstants.activeEffects.contains( EffectPool.get( Effect.SLEAZEFORM ) ) )
		{
			spookyDamage *= 2;
		}

		double coldDamage = KoLConstants.activeEffects.contains( EffectPool.get( Effect.COLDFORM ) ) ? 1.0 :
			  Math.max( damageTaken * ( 100.0 - KoLCharacter.elementalResistanceByLevel( KoLCharacter.getElementalResistanceLevels( Element.COLD ) ) ) / 100.0 - diff, 1 );
		if ( KoLConstants.activeEffects.contains( EffectPool.get( Effect.SLEAZEFORM ) ) || KoLConstants.activeEffects.contains( EffectPool.get( Effect.STENCHFORM ) ) )
		{
			coldDamage *= 2;
		}
		return ( (int) Math.ceil( spookyDamage ) ) + " spooky damage, " + ( (int) Math.ceil( coldDamage ) ) + " cold damage";
	}

	private static int findBooPeakLevel( String decisionText )
	{
		if ( decisionText == null )
		{
			return 0;
		}
		if ( decisionText.equals( "Ask the Question" ) || decisionText.equals( "Talk to the Ghosts" ) ||
			decisionText.equals( "I Wanna Know What Love Is" ) || decisionText.equals( "Tap Him on the Back" ) ||
			decisionText.equals( "Avert Your Eyes" ) || decisionText.equals( "Approach a Raider" ) ||
			decisionText.equals( "Approach the Argument" ) || decisionText.equals( "Approach the Ghost" ) ||
			decisionText.equals( "Approach the Accountant Ghost" ) || decisionText.equals( "Ask if He's Lost" ) )
		{
			return 1;
		}
		else if ( decisionText.equals( "Enter the Crypt" ) ||
			decisionText.equals( "Try to Talk Some Sense into Them" ) ||
			decisionText.equals( "Put Your Two Cents In" ) || decisionText.equals( "Talk to the Ghost" ) ||
			decisionText.equals( "Tell Them What Werewolves Are" ) || decisionText.equals( "Scream in Terror" ) ||
			decisionText.equals( "Check out the Duel" ) || decisionText.equals( "Watch the Fight" ) ||
			decisionText.equals( "Approach and Reproach" ) || decisionText.equals( "Talk Back to the Robot" ) )
		{
			return 2;
		}
		else if ( decisionText.equals( "Go down the Steps" ) || decisionText.equals( "Make a Suggestion" ) ||
			decisionText.equals( "Tell Them About True Love" ) || decisionText.equals( "Scold the Ghost" ) ||
			decisionText.equals( "Examine the Pipe" ) || decisionText.equals( "Say What?" ) ||
			decisionText.equals( "Listen to the Lesson" ) || decisionText.equals( "Listen in on the Discussion" ) ||
			decisionText.equals( "Point out the Malefactors" ) || decisionText.equals( "Ask for Information" ) )
		{
			return 3;
		}
		else if ( decisionText.equals( "Hurl Some Spells of Your Own" ) || decisionText.equals( "Take Command" ) ||
			decisionText.equals( "Lose Your Patience" ) || decisionText.equals( "Fail to Stifle a Sneeze" ) ||
			decisionText.equals( "Ask for Help" ) ||
			decisionText.equals( "Ask How Duskwalker Basketball Is Played, Against Your Better Judgement" ) ||
			decisionText.equals( "Knights in White Armor, Never Reaching an End" ) ||
			decisionText.equals( "Own up to It" ) || decisionText.equals( "Approach the Poor Waifs" ) ||
			decisionText.equals( "Look Behind You" ) )
		{
			return 4;
		}
		else if ( decisionText.equals( "Read the Book" ) || decisionText.equals( "Join the Conversation" ) ||
			decisionText.equals( "Speak of the Pompatus of Love" ) || decisionText.equals( "Ask What's Going On" ) ||
			decisionText.equals( "Interrupt the Rally" ) || decisionText.equals( "Ask What She's Doing Up There" ) ||
			decisionText.equals( "Point Out an Unfortunate Fact" ) || decisionText.equals( "Try to Talk Sense" ) ||
			decisionText.equals( "Ask for Directional Guidance" ) || decisionText.equals( "What?" ) )
		{
			return 5;
		}

		return 0;
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

		MafiaState state = MafiaState.CONTINUE;
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
				state = MafiaState.ERROR;
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

		case 600:
			// Summon Minion
			if ( ChoiceManager.skillUses > 0 )
			{
				// Add the quantity field here and let the decision get added later
				request.addFormField( "quantity", String.valueOf( ChoiceManager.skillUses ) );
			}
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

	private static final String specialChoiceDecision1( final int choice, String decision, final int stepCount, final String responseText )
	{
		// A few choices have non-standard options: 0 is not Manual Control
		switch ( choice )
		{
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
			case 6:
				return "4";
			}
			return decision;

		// Dungeon Fist!
		case 486:
			if ( ChoiceManager.action == PostChoiceAction.NONE )
			{	// Don't automate this if we logged in in the middle of the game -
				// the auto script isn't robust enough to handle arbitrary starting points.
				return ArcadeRequest.autoDungeonFist( stepCount, responseText );
			}
			return decision;

		// Interview With You
		case 546:
			if ( ChoiceManager.action == PostChoiceAction.NONE )
			{	// Don't automate this if we logged in in the middle of the game -
				// the auto script isn't robust enough to handle arbitrary starting points.
				return VampOutManager.autoVampOut( StringUtilities.parseInt( decision ), stepCount, responseText );
			}
			return "0";

		// Summon Minion is a skill
		case 600:
			if ( ChoiceManager.skillUses > 0 )
			{
				ChoiceManager.skillUses = 0;
				return "1";
			}
			return "2";

		// Summon Horde is a skill
		case 601:
			if ( ChoiceManager.skillUses > 0 )
			{
				// This skill has to be done 1 cast at a time
				ChoiceManager.skillUses--;
				return "1";
			}
			return "2";

		case 665:
			if ( ChoiceManager.action == PostChoiceAction.NONE )
			{
				return GameproManager.autoSolve( stepCount );
			}
			return "0";

		case 904: case 905: case 906: case 907: case 908:
		case 909: case 910: case 911: case 912: case 913:
			// Choices in the Louvre

			if ( decision.equals( "" ) )
			{
				return LouvreManager.handleChoice( choice, stepCount );
			}

			return decision;
		}

		return decision;
	}

	private static final String specialChoiceDecision2( final int choice, String decision, final int stepCount, final String responseText )
	{
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

			boolean sausagesAvailable = responseText != null && responseText.contains( "Check under the nightstand" );

			// If the player is looking for the ballroom key and
			// doesn't have it, then update their decision so that
			// KoLmafia automatically goes for it
			if ( GoalManager.hasGoal( ChoiceManager.BALLROOM_KEY ) && !KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) )
			{
				// Always get the sausage book if it is available.
				decision = "4";
			}

			// If the player wants the sausage book and it is
			// available, take it.
			if ( decision.equals( "4" ) )
			{
				return
					sausagesAvailable ? "4" :
					KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) ? "1" :
					Preferences.getInteger( "lastBallroomUnlock" ) == KoLCharacter.getAscensions() ? "2" :
					"1";
			}

			// Otherwise, if the player is specifically looking for
			// things obtained from the combat, fight!
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

		// Skull, Skull, Skull
		case 155:
			// Option 4 - "Check the shiny object" - is not always available.
			if ( decision.equals( "4" ) && !responseText.contains( "Check the shiny object" ) )
			{
				return "5";
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

			// If you are wearing the outfit, have Worldpunch, or
			// are in Axecore, take the appropriate decision.
			// Otherwise, auto-skip the goatlet adventure so it can
			// be tried again later.

			return decision.equals( "2" ) ? "2" :
				EquipmentManager.isWearingOutfit( OutfitPool.MINING_OUTFIT ) ? "1" :
				KoLCharacter.inFistcore() && KoLConstants.activeEffects.contains( SorceressLairManager.EARTHEN_FIST )  ? "1" :
				KoLCharacter.inAxecore() ? "3" :
				"2";
			//Random Lack of an Encounter
		case 182:

			// If the player is looking for the model airship,
			// then update their preferences so that KoLmafia
			// automatically switches things for them.
			int option4Mask = ( responseText.contains( "Gallivant down to the head" ) ? 1 : 0 ) << 2;

			if ( option4Mask > 0 && GoalManager.hasGoal( ChoiceManager.MODEL_AIRSHIP ) )
			{
				return "4";
			}
			if ( Integer.parseInt( decision ) < 4 )
				return decision;

			return ( option4Mask & Integer.parseInt( decision ) ) > 0 ? "4"
				: String.valueOf( Integer.parseInt( decision ) - 3 );
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
				case MUSCLE:
					return "3";
				case MYSTICALITY:
					return "4";
				case MOXIE:
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

		case 535:
			if ( ChoiceManager.action == PostChoiceAction.NONE )
			{	// Don't automate this if we logged in in the middle of the game -
				// the auto script isn't robust enough to handle arbitrary starting points.
				return SafetyShelterManager.autoRonald( decision, stepCount, responseText );
			}
			return "0";

		case 536:
			if ( ChoiceManager.action == PostChoiceAction.NONE )
			{	// Don't automate this if we logged in in the middle of the game -
				// the auto script isn't robust enough to handle arbitrary starting points.
				return SafetyShelterManager.autoGrimace( decision, stepCount, responseText );
			}
			return "0";

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

		// Duffel on the Double
		case 575:
			// Option 2 - "Dig deeper" - is not always available.
			if ( decision.equals( "2" ) && !responseText.contains( "Dig deeper" ) )
			{
				return "3";
			}
			return decision;

		case 594:
			if ( ChoiceManager.action == PostChoiceAction.NONE )
			{	// Don't automate this if we logged in in the middle of the game -
				// the auto script isn't robust enough to handle arbitrary starting points.
				return LostKeyManager.autoKey( decision, stepCount, responseText );
			}
			return "0";

		case 678:
			// Option 3 isn't always available, but decision to take isn't clear if it's selected, so show in browser
			if ( decision.equals( "3" ) && !responseText.contains( "Check behind the trash can" ) )
			{
				return "0";
			}
			return decision;
			
		case 690:
			// The First Chest Isn't the Deepest.
		case 691:
			// Second Chest

			// *** These are chests in the daily dungeon.

			// If you have a Ring of Detect Boring Doors equipped,
			// "go through the boring door"

			return decision;

		case 692:
			// I Wanna Be a Door

			// *** This is the locked door in the daily dungeon.

			// If you have a Platinum Yendorian Express Card, use it.
			// Otherwise, if you have pick-o-matic lockpicks, use them
			// Otherwise, if you have a skeleton key, use it.

			if ( decision.equals( "11" ) )
			{
				if ( InventoryManager.getCount( ItemPool.EXPRESS_CARD ) > 0 )
				{
					return "7";
				}
				else if ( InventoryManager.getCount( ItemPool.PICKOMATIC_LOCKPICKS ) > 0 )
				{
					return "3";
				}
				else if ( InventoryManager.getCount( ItemPool.SKELETON_KEY ) > 0 )
				{
					return "2";
				}
				else
				{
					// Cannot unlock door
					return "0";
				}
			}
			
			// Use highest stat to try to pass door
			if ( decision.equals( "12" ) )
			{
				int buffedMuscle = KoLCharacter.getAdjustedMuscle();
				int buffedMysticality = KoLCharacter.getAdjustedMysticality();
				int buffedMoxie = KoLCharacter.getAdjustedMoxie();
				
				if ( buffedMuscle >= buffedMysticality && buffedMuscle >= buffedMoxie )
				{
					return "4";
				}
				else if ( buffedMysticality >= buffedMuscle && buffedMysticality >= buffedMoxie )
				{
					return "5";
				}
				else
				{
					return "6";
				}
			}
			return decision;

		case 693:
			// It's Almost Certainly a Trap

			// *** This is a trap in the daily dungeon.

			// If you have an eleven-foot pole, use it.

			return decision;

		// Delirium in the Cafeterium
		case 700:
			if ( decision.equals( "1" ) )
			{
				return ( KoLConstants.activeEffects.contains( ChoiceManager.JOCK_EFFECT ) ? "1" :
					KoLConstants.activeEffects.contains( ChoiceManager.NERD_EFFECT ) ? "2" : "3" );
			}
			return decision;
			
		// Halls Passing in the Night 
		case 705:
			// Option 2-4 aren't always available, but decision to take isn't clear if it's selected, so show in browser
			if ( decision.equals( "2" ) && !responseText.contains( "Go to the janitor's closet" ) )
			{
				return "0";
			}
			if ( decision.equals( "3" ) && !responseText.contains( "Head to the bathroom" ) )
			{
				return "0";
			}
			if ( decision.equals( "4" ) && !responseText.contains( "Check out the teacher's lounge" ) )
			{
				return "0";
			}
			return decision;
			
		// The Cabin in the Dreadsylvanian Woods
		case 721:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil1" ) ) )
			{
				return "6";
			}
			return decision;

		// Tallest Tree in the Forest
		case 725:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil2" ) ) )
			{
				return "6";
			}
			return decision;

		// Below the Roots
		case 729:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil3" ) ) )
			{
				return "6";
			}
			return decision;

		// Dreadsylvanian Village Square
		case 733:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil4" ) ) )
			{
				return "6";
			}
			return decision;

		// The Even More Dreadful Part of Town
		case 737:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil5" ) ) )
			{
				return "6";
			}
			return decision;

		// The Old Duke's Estate
		case 741:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil6" ) ) )
			{
				return "6";
			}
			return decision;

		// This Hall is Really Great
		case 745:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil7" ) ) )
			{
				return "6";
			}
			return decision;

		// Tower Most Tall
		case 749:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil8" ) ) )
			{
				return "6";
			}
			return decision;

		// The Dreadsylvanian Dungeon
		case 753:
			// Option 5 - "Use a ghost pencil" - is not always available.
			// Even if it is, if you already have this shortcut, skip it
			if ( decision.equals( "5" ) &&
			     ( !responseText.contains( "Use a ghost pencil" ) ||
			       Preferences.getBoolean( "ghostPencil9" ) ) )
			{
				return "6";
			}
			return decision;

		// Action Elevator
		case 780:
			// If Boss dead, skip, else if thrice-cursed, fight spirit, if not, get cursed.
			if ( decision.equals( "1" ) )
			{
				return ( Preferences.getInteger( "hiddenApartmentProgress" ) >= 7 ? "6" :
							KoLConstants.activeEffects.contains( ChoiceManager.CURSE3_EFFECT ) ? "1" : "2" );
			}
			// Only relocate pygmy lawyers once, then leave
			if ( decision.equals( "3" ) )
			{
				return ( Preferences.getInteger( "relocatePygmyLawyer" ) == KoLCharacter.getAscensions() ? "6" : "3" );
			}
			return decision;
			
		// Earthbound and Down
		case 781:
		{
			// Option 1 and 2 are not always available. Take appropriate one if option to 
			// take action is selected. If not,leave.
			if ( decision.equals( "1" ) )
			{
				int hiddenApartmentProgress = Preferences.getInteger( "hiddenApartmentProgress" );
				return ( hiddenApartmentProgress == 7 ? "2" : hiddenApartmentProgress < 1 ? "1" : "6" );
			}
			return decision;
		}
		
		// Water You Dune
		case 783:
		{
			// Option 1 and 2 are not always available. Take appropriate one if option to 
			// take action is selected. If not, leave.
			if ( decision.equals( "1" ) )
			{
				int hiddenHospitalProgress = Preferences.getInteger( "hiddenHospitalProgress" );
				return ( hiddenHospitalProgress == 7 ? "2" : hiddenHospitalProgress < 1 ? "1" : "6" );
			}
			return decision;
		}
		
		// Air Apparent
		case 785:
		{
			// Option 1 and 2 are not always available. Take appropriate one if option to 
			// take action is selected. If not, leave.
			if ( decision.equals( "1" ) )
			{
				int hiddenOfficeProgress = Preferences.getInteger( "hiddenOfficeProgress" );
				return ( hiddenOfficeProgress == 7 ? "2" : hiddenOfficeProgress < 1 ? "1" : "6" );
			}
			return decision;
		}
		
		// Working Holiday
		case 786:
		{
			// If boss dead, fight accountant, fight boss if available, if not, get binder clip if you lack it, if not, fight accountant if you still need file
			if ( decision.equals( "1" ) )
			{
				int hiddenOfficeProgress = Preferences.getInteger( "hiddenOfficeProgress" );
				boolean hasMcCluskyFile = InventoryManager.getCount( ChoiceManager.MCCLUSKY_FILE ) > 0;
				boolean hasMcCluskyFilePage5 = InventoryManager.getCount( ChoiceManager.MCCLUSKY_FILE_PAGE5 ) > 0;
				boolean hasBinderClip = InventoryManager.getCount( ChoiceManager.BINDER_CLIP ) > 0;
				return ( hiddenOfficeProgress >= 7 ? "3" :
						hasMcCluskyFile ? "1" :
						!hasBinderClip ? "2" : 
						!hasMcCluskyFilePage5 ? "3" : "0" );
			}
			return decision;
		}
		
		// Fire when Ready
		case 787:
		{
			// Option 1 and 2 are not always available. Take appropriate one if option to 
			// take action is selected. If not, leave.
			if ( decision.equals( "1" ) )
			{
				int hiddenBowlingAlleyProgress = Preferences.getInteger( "hiddenBowlingAlleyProgress" );
				return ( hiddenBowlingAlleyProgress == 7 ? "2" : hiddenBowlingAlleyProgress < 1 ? "1" : "6" );
			}
			return decision;
		}
		
		// Where Does The Lone Ranger Take His Garbagester?
		case 789:
			// Only relocate pygmy janitors once, then get random items
			if ( decision.equals( "2" ) && 
				Preferences.getInteger( "relocatePygmyJanitors" ) == KoLCharacter.getAscensions() )
			{
				return "1";
			}
			return decision;

		// Legend of the Temple in the Hidden City
		case 791:
		{
			// Leave if not enough triangles to fight spectre
			int stoneTriangles = InventoryManager.getCount( ChoiceManager.STONE_TRIANGLE );
			if ( decision.equals( "1" ) && stoneTriangles < 4 )
			{
				return "6";
			}
			return decision;
		}
	
		// Silence at last
		case 808:
			// Abort if you want to fight spirit alarm clock but it isn't available.
			if ( decision.equals( "2" ) && !responseText.contains( "nightstand wasn't here before" ) )
			{
				return "0";
			}
			return decision;

		case 914:

			// Sometimes, the choice adventure for the louvre
			// loses track of whether to ignore the louvre or not.

			LouvreManager.resetDecisions();
			return Preferences.getInteger( "louvreGoal" ) != 0 ? "1" : "2";

		// Break Time!
		case 919:
			// Abort if you have plundered the register too many times today
			if ( decision.equals( "1" ) && responseText.contains( "You've already thoroughly" ) )
			{
				return "6";
			}
			return decision;
		
		}

		return decision;
	}

	public static final Object findOption( final Object[] options, final int decision )
	{
		for ( int i = 0; i < options.length; ++i )
		{
			Object obj = options[ i ];
			if ( obj instanceof Option )
			{
				Option opt = (Option)obj;
				if ( opt.getDecision( i + 1 ) == decision )
				{
					return obj;
				}
			}
			else if ( obj instanceof String )
			{
				if ( (i + 1 ) == decision )
				{
					return obj;
				}
			}
		}

		return null;
	}

	private static final String pickGoalChoice( final String option, final String decision )
	{
		// If the user wants manual control, let 'em have it.
		if ( decision.equals( "0" ) )
		{
			return decision;
		}

		// Find the options for the choice we've encountered

		Object[] options = null;

		for ( int i = 0; i < ChoiceManager.CHOICE_ADVS.length && options == null; ++i )
		{
			ChoiceAdventure choiceAdventure = ChoiceManager.CHOICE_ADVS[ i ];
			if ( choiceAdventure.getSetting().equals( option ) )
			{
				options = choiceAdventure.getOptions();
				break;
			}
		}

		for ( int i = 0; i < ChoiceManager.CHOICE_ADV_SPOILERS.length && options == null; ++i )
		{
			ChoiceAdventure choiceAdventure = ChoiceManager.CHOICE_ADV_SPOILERS[ i ];
			if ( choiceAdventure.getSetting().equals( option ) )
			{
				options = choiceAdventure.getOptions();
				break;
			}
		}

		// If it's not in the table, return the player's chosen decision.

		if ( options == null )
		{
			return decision;
		}

		// Choose an item in the conditions first, if it's available.
		// This allows conditions to override existing choices.

		boolean items = false;
		for ( int i = 0; i < options.length; ++i )
		{
			Object obj = options[ i ];
			if ( !( obj instanceof Option ) )
			{
				continue;
			}

			Option opt = (Option)obj;
			AdventureResult item = opt.getItem();
			if ( item == null )
			{
				continue;
			}

			if ( GoalManager.hasGoal( item ) )
			{
				return String.valueOf( opt.getDecision( i + 1 ) );
			}

			items = true;
		}

		// If none of the options have an associated item, nothing to do.
		if ( !items )
		{
			return decision;
		}

		// Find the spoiler corresponding to the chosen decision
		Object chosen = ChoiceManager.findOption( options, StringUtilities.parseInt( decision ) );

		// If the player doesn't want to "complete the outfit", nothing to do
		if ( chosen == null || !chosen.toString().equals( "complete the outfit" ) )
		{
			return decision;
		}

		// Pick an item that the player doesn't have yet
		for ( int i = 0; i < options.length; ++i )
		{
			Object obj = options[ i ];
			if ( !( obj instanceof Option ) )
			{
				continue;
			}

			Option opt = (Option)obj;
			AdventureResult item = opt.getItem();
			if ( item == null )
			{
				continue;
			}

			if ( !InventoryManager.hasItem( item ) )
			{
				return String.valueOf( opt.getDecision( i + 1 ) );
			}
		}

		// If they have everything, then just return choice 1
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
		button.append( "<form name=goalform action='" ).append( url ).append( "' method=post>" );
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
		ChoiceManager.processChoiceAdventure( request, "choice.php", responseText );

		StringBuffer buffer = new StringBuffer( request.responseText );
		RequestEditorKit.getFeatureRichHTML( request.getURLString(), buffer );
		RelayRequest.specialCommandResponse = buffer.toString();
		RelayRequest.specialCommandIsAdventure = true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		if ( urlString.equals( "choice.php" ) )
		{
			// Continuing after a multi-fight.
			// Handle those when the real choice comes up.
			return true;
		}

		Matcher matcher = ChoiceManager.URL_CHOICE_PATTERN.matcher( urlString );
		int choice = 0;
		int decision = 0;
		if ( matcher.find() )
		{
			choice = StringUtilities.parseInt( matcher.group( 1 ) );
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
			case 535:	// Deep Inside Ronald
			case 536:	// Deep Inside Grimace
				return true;
			case 546:	// Interview With You
				return true;
			}
			matcher = ChoiceManager.URL_OPTION_PATTERN.matcher( urlString );
			if ( matcher.find() )
			{
				decision = StringUtilities.parseInt( matcher.group( 1 ) );
				Object[][] spoilers = ChoiceManager.choiceSpoilers( choice );
				String desc =
					spoilers == null ?
					"unknown" :
					ChoiceManager.choiceSpoiler( choice, decision, spoilers[ 2 ] ).toString();
				RequestLogger.updateSessionLog( "Took choice " + choice + "/" + decision + ": " + desc );
				// For now, leave the raw URL in the log in case some analysis
				// tool is relying on it.
				//return true;
			}
		}

		if ( choice == 0 && decision == 0 )
		{
			// forecoption=0 will redirect to the real choice.
			// Don't bother logging it.
			return true;
		}

		// By default, we log the url of any choice we take
		RequestLogger.updateSessionLog( urlString );

		return true;
	}

	public static final void registerDeferredChoice( final int choice, final String encounter )
	{
		// If we couldn't find an encounter, do nothing
		if ( encounter == null )
		{
			return;
		}

		switch ( choice )
		{
		case 620:	// A Blow Is Struck!
		case 621:	// Hold the Line!
		case 622:	// The Moment of Truth
		case 634:	// Goodbye Fnord
			// These all arise out of a multifight, rather than by
			// visiting a location.
			RequestLogger.registerLastLocation();
			break;
		}
	}

	public static final String findChoiceDecisionIndex( final String text, final String responseText )
	{
		Matcher matcher = ChoiceManager.DECISION_BUTTON_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String decisionText = matcher.group( 2 );

			if ( decisionText.indexOf( text ) != -1 )
			{
				return StringUtilities.getEntityDecode( matcher.group( 1 ) );
			}
		}

		return "0";
	}

	public static final String findChoiceDecisionText( final int index, final String responseText )
	{
		Matcher matcher = ChoiceManager.DECISION_BUTTON_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int decisionIndex = Integer.parseInt( matcher.group( 1 ) );

			if ( decisionIndex == index )
			{
				return matcher.group( 2 );
			}
		}

		return null;
	}

	public static final void setSkillUses( int uses )
	{
		// Used for casting skills that lead to a choice adventure
		ChoiceManager.skillUses = uses;
	}

	private static String[] oldManPsychosisSpoilers()
	{
		Matcher matcher = ChoiceManager.DECISION_BUTTON_PATTERN.matcher( ChoiceManager.lastResponseText );

		String[][] buttons = new String[ 4 ][ 2 ];
		int i = 0;
		while ( matcher.find() )
		{
			buttons[ i ][ 0 ] = matcher.group( 1 );
			buttons[ i ][ 1 ] = matcher.group( 2 );
			++i;
		}

		// we need to return a string array with len=4 - even if there are buttons missing
		// the missing buttons are just "hidden" and thus the later buttons have the appropriate form field
		// i.e. button 2 may be the first button.

		// As it turns out, I think all this cancels out and they could just be implemented as standard choice adventures,
		// since the buttons are not actually randomized, they are consistent within the four choice adventures that make up the 10 log entry non-combats.
		// Ah well.  Leavin' it here.
		String[] spoilers = new String[ 4 ];

		for ( int j = 0; j < spoilers.length; j++ )
		{
			for ( String[] s : OLD_MAN_PSYCHOSIS_SPOILERS )
			{
				if ( s[ 0 ].equals( buttons[ j ][ 1 ] ) )
				{
					spoilers[ Integer.parseInt( buttons[ j ][ 0 ] ) - 1 ] = s[ 1 ]; // button 1 text should be in index 0, 2 -> 1, etc.
					break;
				}
			}
		}

		return spoilers;
	}

	private static boolean parseCinderellaTime()
	{
		Matcher matcher = ChoiceManager.CINDERELLA_TIME_PATTERN.matcher( ChoiceManager.lastResponseText );
		while ( matcher.find() )
		{
			int time = StringUtilities.parseInt( matcher.group( 1 ) );
			if ( time != -1 )
			{
				Preferences.setInteger( "cinderellaMinutesToMidnight", time );
				return true;
			}
		}
		return false;
	}
	
	public static boolean canWalkAway()
	{
		return ChoiceManager.canWalkAway;
	}

	private static void setCanWalkAway( final int choice )
	{
		ChoiceManager.canWalkAway = ChoiceManager.canWalkFromChoice( choice );
	}

	public static boolean canWalkFromChoice( int choice )
	{
		switch ( choice )
		{
		case 570: // GameInformPowerDailyPro Walkthru
		case 603: // Skeletons and The Closet
		case 627: // ChibiBuddy&trade; (on)
		case 633: // ChibiBuddy&trade; (off)
		case 664: // The Crackpot Mystic's Shed
		case 720: // The Florist Friar's Cottage
		case 767: // Tales of Dread
		case 770: // The Institute for Canadian Studies
		case 774: // Opening up the Folder Holder
		case 792: // The Degrassi Knoll Gym
		case 793: // Welcome to The Shore, Inc.
		case 801: // A Reanimated Conversation
		case 804: // Trick or Treat!
		case 810: // K.R.A.M.P.U.S. facility
		case 812: // The Unpermery
		case 821: // LP-ROM burner
		case 835: // Barely Tales
		case 836: // Adventures Who Live in Ice Houses...
		case 844: // The Portal to Horrible Parents
		case 845: // Rumpelstiltskin's Workshop
		case 859: // Upping your grade
		case 867: // Sneaky Peterskills
		case 870: // Hair Today
		case 871: // inspecting Motorbike
		case 872: // Drawn Onward
			return true;

		default:
			return false;
		}
	}
}
