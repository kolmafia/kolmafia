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

package net.sourceforge.kolmafia;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;

import java.io.File;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Random;

import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JTextField;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.chat.StyledChatBuffer;

import net.sourceforge.kolmafia.swingui.menu.ScriptMRUList;

public interface KoLConstants
	extends UtilityConstants
{
	// Version information for the current version of KoLmafia.
	// Rendered in various locations and therefore made public.

	public static final String VERSION_NAME = "KoLmafia v15.7";
	public static final String VERSION_DATE = "Released on October 13, 2012";
	public static final boolean RELEASED = true;
	public static final String REVISION = null;

	// General constants used for calculations and formatting of
	// strings, as well as for string parsing.

	public static final Random RNG = new Random();
	public static final Font DEFAULT_FONT = ( new JTextField() ).getFont();
	public static final String LINE_BREAK = System.getProperty( "line.separator" );
	public static final Pattern LINE_BREAK_PATTERN = Pattern.compile( "\\s*[\r\n]+\\s*" );

	public static final Pattern ANYTAG_PATTERN = Pattern.compile( "<.*?>" );
	public static final Pattern ANYTAG_BUT_ITALIC_PATTERN = Pattern.compile( "<(?!i>)(?!/i>).*?>" );
	public static final Pattern SCRIPT_PATTERN = Pattern.compile( "<script.*?</script>", Pattern.DOTALL );
	public static final Pattern STYLE_PATTERN = Pattern.compile( "<style.*?</style>", Pattern.DOTALL );
	public static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--.*?-->", Pattern.DOTALL );

	public static final DecimalFormat COMMA_FORMAT =
		new DecimalFormat( "#,##0;-#,##0", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat MODIFIER_FORMAT =
		new DecimalFormat( "+#0;-#0", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat SINGLE_PRECISION_FORMAT =
		new DecimalFormat( "+#,##0.0;-#,##0.0", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat FLOAT_FORMAT =
		new DecimalFormat( "#,##0.00;-#,##0.00", new DecimalFormatSymbols( Locale.US ) );
	// About 45 places needed to show the smallest denorm float.
	public static final DecimalFormat NONSCIENTIFIC_FORMAT =
		new DecimalFormat( "#0.0############################################", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat ROUNDED_MODIFIER_FORMAT =
		new DecimalFormat( "+#0.00;-#0.00", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat CHAT_LASTSEEN_FORMAT = new DecimalFormat( "0000000000" );

	public static final SimpleDateFormat DAILY_FORMAT = new SimpleDateFormat( "yyyyMMdd", Locale.US );
	public static final SimpleDateFormat WEEKLY_FORMAT = new SimpleDateFormat( "yyyyMM_'w'W", Locale.US );
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat( "HH:mm:ss z", Locale.US );

	// Generic constants which indicate null values.  Used in
	// order to preserve memory.

	public static final Class[] NOPARAMS = new Class[ 0 ];
	public static final JLabel BLANK_LABEL = new JLabel();

	// Constants which are used in order to do things inside of
	// the GUI.  Ensures that all GUI information can be accessed
	// at any time.

	public static final Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
	public static final LockableListModel existingFrames = new LockableListModel();

	// Menus rendered in the relay browser and the KoLmafia mini-browser.
	// Ensures that the two menus always contain the same information.

	// Additional navigation destinations for convenience

	public static final String[][] GOTO_MENU = new String[][]
	{
		{
			"Spookyraven",
			"manor.php"
		},
		{
			"Council",
			"council.php"
		},
		{
			"Guild Hall",
			"guild.php"
		},
		{
			"Mt. McLargeHuge",
			"place.php?whichplace=mclargehuge"
		},
		{
			"Cobb's Knob",
			"cobbsknob.php"
		},
		{
			"Defiled Cyrpt",
			"cyrpt.php"
		},
		{
			"The Beanstalk",
			"beanstalk.php"
		},
		{
			"The Sea",
			"thesea.php"
		},
	};

	public static final String[][] FRAME_NAMES =
	{
		{
			"Adventure",
			"AdventureFrame"
		},
		{
			"Mini-Browser",
			"RequestFrame"
		},
		{
			"Relay Server",
			"LocalRelayServer"
		},
		{
			"Purchases",
			"MallSearchFrame"
		},
		{
			"Graphical CLI",
			"CommandDisplayFrame"
		},
		{
			"Daily Deeds",
			"CharSheetFrame"
		},
		{
			"Item Manager",
			"ItemManageFrame"
		},
		{
			"Gear Changer",
			"GearChangeFrame"
		},
		{
			"Store Manager",
			"StoreManageFrame"
		},
		{
			"Coin Masters",
			"CoinmastersFrame"
		},
		{
			"Museum Display",
			"MuseumFrame"
		},
		{
			"Hall of Legends",
			"MeatManageFrame"
		},
		{
			"Skill Casting",
			"SkillBuffFrame"
		},
		{
			"Contact List",
			"ContactListFrame"
		},
		{
			"Buffbot Manager",
			"BuffBotFrame"
		},
		{
			"Purchase Buffs",
			"BuffRequestFrame"
		},
		{
			"Request a Fax",
			"FaxRequestFrame"
		},
		{
			"Mushroom Plot",
			"MushroomFrame"
		},
		{
			"Familiar Trainer",
			"FamiliarTrainingFrame"
		},
		{
			"Loathing Chat",
			"ChatManager"
		},
		{
			"Clan Management",
			"ClanManageFrame"
		},
		{
			"Farmer's Almanac",
			"CalendarFrame"
		},
		{
			"Internal Database",
			"DatabaseFrame"
		},
		{
			"Coin Toss Game",
			"MoneyMakingGameFrame"
		},
		{
			"Preferences",
			"OptionsFrame"
		},
		{
			"Modifier Maximizer",
			"MaximizerFrame"
		}
	};

	// Scripting-related constants.  Used throughout KoLmafia in
	// order to ensure proper handling of scripts.

	public static final LockableListModel scripts = new LockableListModel();
	public static final LockableListModel bookmarks = new LockableListModel();

	public static final ArrayList<String> disabledScripts = new ArrayList<String>();
	public static final ScriptMRUList scriptMList = 
			  new ScriptMRUList( "scriptMRUList", "scriptMRULength" );
	public static final ScriptMRUList maximizerMList =
			  new ScriptMRUList( "maximizerMRUList", "maximizerMRULength" );

	public static final SortedListModel junkList = new SortedListModel();
	public static final SortedListModel singletonList = new SortedListModel();
	public static final SortedListModel mementoList = new SortedListModel();
	public static final SortedListModel profitableList = new SortedListModel();

	public static final String BUFFBOT_DIRECTORY = "buffs/";
	public static final String CCS_DIRECTORY = "ccs/";
	public static final String CHATLOG_DIRECTORY = "chats/";
	public static final String PLOTS_DIRECTORY = "planting/";
	public static final String SCRIPT_DIRECTORY = "scripts/";
	public static final String SESSIONS_DIRECTORY = "sessions/";
	public static final String RELAY_DIRECTORY = "relay/";

	public static final File BUFFBOT_LOCATION =
		new File( UtilityConstants.ROOT_LOCATION, KoLConstants.BUFFBOT_DIRECTORY );
	public static final File CCS_LOCATION = new File( UtilityConstants.ROOT_LOCATION, KoLConstants.CCS_DIRECTORY );
	public static final File CHATLOG_LOCATION =
		new File( UtilityConstants.ROOT_LOCATION, KoLConstants.CHATLOG_DIRECTORY );
	public static final File PLOTS_LOCATION = new File( UtilityConstants.ROOT_LOCATION, KoLConstants.PLOTS_DIRECTORY );
	public static final File SCRIPT_LOCATION = new File( UtilityConstants.ROOT_LOCATION, KoLConstants.SCRIPT_DIRECTORY );
	public static final File SESSIONS_LOCATION =
		new File( UtilityConstants.ROOT_LOCATION, KoLConstants.SESSIONS_DIRECTORY );
	public static final File RELAY_LOCATION = new File( UtilityConstants.ROOT_LOCATION, KoLConstants.RELAY_DIRECTORY );

	// All data files that can be overridden

	public static final String[] OVERRIDE_DATA =
	{
		"adventures.txt",
		"buffbots.txt",
		"classskills.txt",
		"coinmasters.txt",
		"combats.txt",
		"concoctions.txt",
		"consequences.txt",
		"defaults.txt",
		"equipment.txt",
		"familiars.txt",
		"foldgroups.txt",
		"fullness.txt",
		"inebriety.txt",
		"items.txt",
		"mallprices.txt",
		"modifiers.txt",
		"monsters.txt",
		"nonfilling.txt",
		"npcstores.txt",
		"outfits.txt",
		"packages.txt",
		"pulverize.txt",
		"spleenhit.txt",
		"statuseffects.txt",
		"zapgroups.txt",
		"zonelist.txt"
	};

	// The current version number of each data file

	public static final int ADVENTURES_VERSION = 4;
	public static final int BUFFBOTS_VERSION = 1;
	public static final int CLASSSKILLS_VERSION = 3;
	public static final int COINMASTERS_VERSION = 2;
	public static final int COMBATS_VERSION = 1;
	public static final int CONCOCTIONS_VERSION = 3;
	public static final int CONSEQUENCES_VERSION = 1;
	public static final int DEFAULTS_VERSION = 1;
	public static final int EQUIPMENT_VERSION = 2;
	public static final int FAMILIARS_VERSION = 2;
	public static final int FOLDGROUPS_VERSION = 1;
	public static final int FULLNESS_VERSION = 2;
	public static final int INEBRIETY_VERSION = 2;
	public static final int ITEMS_VERSION = 1;
	public static final int MODIFIERS_VERSION = 2;
	public static final int MONSTERS_VERSION = 4;
	public static final int NONFILLING_VERSION = 1;
	public static final int NPCSTORES_VERSION = 1;
	public static final int OUTFITS_VERSION = 1;
	public static final int PACKAGES_VERSION = 1;
	public static final int PULVERIZE_VERSION = 2;
	public static final int QUESTSCOUNCIL_VERSION = 1;
	public static final int QUESTSLOG_VERSION = 1;
	public static final int SPLEENHIT_VERSION = 3;
	public static final int STATUSEFFECTS_VERSION = 3;
	public static final int ZAPGROUPS_VERSION = 1;
	public static final int ZONELIST_VERSION = 1;

	// mallprices.txt can be updated by typing a filename, so give
	// it a distinct version number in case the user types the name
	// of another one of the data files.
	public static final int MALLPRICES_VERSION = 0xF00D5;

	// Different states of KoLmafia.  Used in order to determine
	// what is still permitted.
	public enum MafiaState {
		ENABLE,
		ERROR,
		ABORT,
		PENDING,
		CONTINUE;
	};

	public enum Stat
	{
		MUSCLE( "Muscle" ),
		MYSTICALITY( "Mysticality" ),
		MOXIE( "Moxie" ),
		SUBMUSCLE( "SubMuscle" ),
		SUBMYST( "SubMysticality" ),
		SUBMOXIE( "SubMoxie" ),
		NONE( "None" );

		private final String name;

		private Stat( String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	public enum ZodiacType
	{
		NONE,
		MUSCLE,
		MYSTICALITY,
		MOXIE,
		BAD_MOON
	}

	public enum ZodiacZone
	{
		KNOLL,
		CANADIA,
		GNOMADS,
		NONE
	}

	public enum WeaponType
	{
		MELEE,
		RANGED,
		NONE
	}

	// Mystical Book Types
	public enum BookType
	{
		TOME,
		GRIMOIRE,
		LIBRAM
	}

	public static final int NO_CONSUME = 0;
	public static final int CONSUME_EAT = 1;
	public static final int CONSUME_DRINK = 2;
	public static final int CONSUME_USE = 3;
	public static final int CONSUME_MULTIPLE = 4;
	public static final int GROW_FAMILIAR = 5;
	public static final int CONSUME_ZAP = 6;
	public static final int EQUIP_FAMILIAR = 7;
	public static final int EQUIP_ACCESSORY = 8;
	public static final int EQUIP_HAT = 9;
	public static final int EQUIP_PANTS = 10;
	public static final int EQUIP_SHIRT = 11;
	public static final int EQUIP_WEAPON = 12;
	public static final int EQUIP_OFFHAND = 13;
	public static final int MP_RESTORE = 14;
	public static final int MESSAGE_DISPLAY = 15;
	public static final int HP_RESTORE = 16;
	public static final int HPMP_RESTORE = 17;
	public static final int INFINITE_USES = 18;
	public static final int EQUIP_CONTAINER = 19;
	public static final int CONSUME_SPHERE = 20;
	public static final int CONSUME_FOOD_HELPER = 21;
	public static final int CONSUME_DRINK_HELPER = 22;
	public static final int CONSUME_STICKER = 23;
	public static final int CONSUME_GUARDIAN = 24;

	public static final int CONSUME_MIMIC = 96;
	public static final int CONSUME_SLIME = 97;
	public static final int CONSUME_HOBO = 98;
	public static final int CONSUME_GHOST = 99;

	// Anatomy of a mixingMethod:

	// The low-order bits (masked by CT_MASK) contain one of the item
	// creation types below.  This has to be sufficient to determine the
	// crafting URL or subclass that will be used, and the adventure cost
	// of creation.
	//
	// CR_xxx bit flags (masked by CR_MASK) express other creation
	// requirements: necessary skills, equipment, player state, etc.
	//
	// CF_xxx bits express other information about the item's creation
	// (yield, desirability, etc.), but do not affect whether it's
	// possible.

	// Item creation types

	public static final int CT_MASK = 0x000000FF;
	public static final int METHOD_COUNT = 29;
	public static final int SUBCLASS = 255;

	public static final int NOCREATE = 0;

	// Items anybody can create using meat paste or The Plunger
	public static final int COMBINE = 1;
	// Items created with an E-Z Cook Oven or Dramatic Range
	public static final int COOK = 2;
	// Items created with a Shaker or Cocktailcrafting Kit
	public static final int MIX = 3;
	// Items anybody can create with a tenderizing hammer or via Innabox
	public static final int SMITH = 4;
	// Items that can only be created with a tenderizing hammer, not via Innabox
	public static final int SSMITH = 5;
	// Items requiring access to Nash Crosby's Still
	public static final int STILL_BOOZE = 6;
	public static final int STILL_MIXER = 7;
	// Items requiring access to the Wok of Ages
	public static final int WOK = 8;
	// Items requiring access to the Malus of Forethought
	public static final int MALUS = 9;
	// Items created with jewelry-making pliers
	public static final int JEWELRY = 10;
	// Items anybody can create with starcharts, stars, and lines
	public static final int STARCHART = 11;
	// Items anybody can create with pixels
	public static final int PIXEL = 12;
	// Items created with a rolling pin or and an unrolling pin
	public static final int ROLLING_PIN = 13;
	// Items requiring access to the Gnome supertinker
	public static final int GNOME_TINKER = 14;
	// Items requiring access to Roderic the Staffcrafter
	public static final int STAFF = 15;
	// Items anybody can create with a sushi-rolling mat
	public static final int SUSHI = 16;
	// Items created by single (or multi) using a single item.
	// Extra ingredients might also be consumed.
	// Multi-using more than one of the item creates multiple results.
	public static final int SINGLE_USE = 17;
	// Items created by multi-using specific numbers of a single item.
	// Extra ingredients might also be consumed.
	// You must create multiple result items one at a time.
	public static final int MULTI_USE = 18;
	// Items formerly creatable in Crimbo Town during Crimbo 2005
	public static final int CRIMBO05 = 19;
	// Items formerly creatable in Crimbo Town during Crimbo 2006
	public static final int CRIMBO06 = 20;
	// Items formerly creatable in Crimbo Town during Crimbo 2007
	public static final int CRIMBO07 = 21;
	// Items anybody can create by folding a sugar sheet
	public static final int SUGAR_FOLDING = 22;
	// Items requiring access to Phineas
	public static final int PHINEAS = 23;
	// Items created with an Dramatic Range
	public static final int COOK_FANCY = 24;
	// Items created with a Cocktailcrafting Kit
	public static final int MIX_FANCY = 25;
	// Un-untinkerable Amazing Ideas
	public static final int ACOMBINE = 26;
	// Coinmaster purchase
	public static final int COINMASTER = 27;
	// Tome of Clip Art summons
	public static final int CLIPART = 28;

	// Item creation requirement flags

	public static final int CR_MASK = 0x0FFFFF00;
	// Character gender (for kilt vs. skirt)
	public static final int CR_MALE = 0x00000100;
	public static final int CR_FEMALE = 0x00000200;
	// Holiday-only
	public static final int CR_SSPD = 0x00000400;
	// Requires tenderizing hammer (implied for SMITH & SSMITH)
	public static final int CR_HAMMER = 0x00000800;
	// Requires depleted Grimacite hammer
	public static final int CR_GRIMACITE = 0x00001000;
	// Requires Torso Awaregness
	public static final int CR_TORSO = 0x00002000;
	// Requires Super-Advanced Meatsmithing
	public static final int CR_WEAPON = 0x00004000;
	// Requires Armorcraftiness
	public static final int CR_ARMOR = 0x00008000;
	// Requires Really Expensive Jewerlycrafting
	public static final int CR_EXPENSIVE = 0x00010000;
	// Requires Advanced Saucecrafting
	public static final int CR_REAGENT = 0x00020000;
	// Requires The Way of Sauce
	public static final int CR_WAY = 0x00040000;
	// Requires Deep Saucery
	public static final int CR_DEEP = 0x00080000;
	// Requires Pastamastery
	public static final int CR_PASTA = 0x00100000;
	// Requires Tempuramancy
	public static final int CR_TEMPURA = 0x00200000;
	// Requires Advanced Cocktailcrafting
	public static final int CR_AC = 0x00400000;
	// Requires Superhuman Cocktailcrafting
	public static final int CR_SHC = 0x00800000;
	// Requires Salacious Cocktailcrafting
	public static final int CR_SALACIOUS = 0x01000000;
	// Not on Bees Hate You path
	public static final int CR_NOBEE = 0x02000000;
	//	public static final int CR_ = 0x04000000;
	//	public static final int CR_ = 0x08000000;

	// Item creation information flags

	//	public static final int CF_ = 0x10000000;
	// Saucerors make 3 of this item at a time
	public static final int CF_SX3 = 0x20000000;
	// Recipe unexpectedly does not appear in Discoveries
	public static final int CF_NODISCOVERY = 0x40000000;
	// Recipe should never be used automatically
	public static final int CF_MANUAL = 0x80000000;

	// Colors which are used to handle the various KoLmafia states.
	// Used when changing the display.

	public static final Color ERROR_COLOR = new Color( 255, 192, 192 );
	public static final Color ENABLED_COLOR = new Color( 192, 255, 192 );
	public static final Color DISABLED_COLOR = null;

	// Constants which are useful, but not necessarily used very often.
	// Includes win game text.

	public static final String DEFAULT_KMAIL = "Keep the contents of this message top-sekrit, ultra hush-hush.";

	public static final String[][] WIN_GAME_TEXT = new String[][]
	{
		{
			"Petitioning the Seaside Town Council for automatic game completion...",
			"The Seaside Town Council has rejected your petition.  Game incomplete.",
			"You reject the Seaside Town's decision.  Fighting the council...",
			"You have been defeated by the Seaside Town Council."
		},

		{
			"You enter the super-secret code into the Strange Leaflet...",
			"Your ruby W and heavy D fuse to form the mysterious R!",
			"Moxie sign backdoor accessed.	Supertinkering The Ultimate Weapon...",
			"Supertinkering complete.  Executing tower script...",
			"Your RNG spawns an enraged cow on Floors 1-6."
		},

		{
			"You win the game. What, you were expecting more?",
			"You are now standing in an open field to the west of the Kingdom.",
			"You hear a gurgling ocean to the south, and a path leads north into Valhalla.",
			"What now, Adventurer?"
		},

		{
			"You touch your star starfish!	You surge with power!",
			"Accessing tower backdoor.  Fighting Naughty Sorceress...",
			"Connection timed out during post.  Retrying...",
			"Connection timed out during reply.  Retrying...",
			"Star power expired.  You slink away, dejected and defeated."
		},

		{
			"You raise your metallic A to the sky. Victory is yours!",
			"Original game concept by Jick (Asymmetric Publications).",
			"Co-written by Mr. Skullhead, Riff, and the /dev team.",
			"Special thanks to: the Mods, the Ascension testers, and you.",
			"We present you a new quest, which is the same thing, only harder.",
			"Segmentation fault.  Core dumped."
		},

		{
			"Unlocking secret Arizona trail script...",
			"Crossing first obstacle, admiring landmarks...",
			"Path set to oxygenarian, familiar pace set to grue-ing...",
			"You have died from KoLera."
		},

		{
			"Challenging Naughty Sorceress to a game of chess...",
			"Naughty Sorceress accepts, and invites you to go first.  Calculating move...",
			"Timeout in findBestMove(), making random move: &#9817;f2-f3",
			"Naughty Sorceress moves: &#9823;e7-e5",
			"Timeout in findBestMove(), making random move: &#9817;g2-g4",
			"Naughty Sorceress moves: &#9819;d8-h4",
			"Checkmate."
		},
	};

	// Variables which relate to a given session.  These are made
	// global in order to ensure that any element of KoLmafia can
	// access session-specific information.

	public static final SortedListModel saveStateNames = new SortedListModel();

	public static final SortedListModel inventory = new SortedListModel();
	public static final SortedListModel closet = new SortedListModel();
	public static final SortedListModel storage = new SortedListModel();
	public static final SortedListModel freepulls = new SortedListModel();
	public static final SortedListModel collection = new SortedListModel();
	public static final SortedListModel campground = new SortedListModel();
	public static final SortedListModel pulverizeQueue = new SortedListModel();

	public static final LockableListModel usableSkills = new LockableListModel();
	public static final LockableListModel summoningSkills = new LockableListModel();
	public static final LockableListModel remedySkills = new LockableListModel();
	public static final LockableListModel selfOnlySkills = new LockableListModel();
	public static final LockableListModel buffSkills = new LockableListModel();
	public static final LockableListModel availableSkills = new LockableListModel();
	public static final IdentityHashMap availableSkillsMap = new IdentityHashMap();
	public static final LockableListModel availableConditionalSkills = new LockableListModel();
	public static final IdentityHashMap availableConditionalSkillsMap = new IdentityHashMap();
	public static final LockableListModel permedSkills = new LockableListModel();
	public static final LockableListModel combatSkills = new LockableListModel();

	public static final LockableListModel activeEffects = new LockableListModel();
	public static final ArrayList<AdventureResult> recentEffects = new ArrayList<AdventureResult>();

	public static final LockableListModel hermitItems = new LockableListModel();
	public static final LockableListModel restaurantItems = new LockableListModel();
	public static final LockableListModel microbreweryItems = new LockableListModel();
	public static final LockableListModel kitchenItems = new LockableListModel();
	public static final LockableListModel cafeItems = new LockableListModel();

	public static final SortedListModel tally = new SortedListModel();
	public static final LockableListModel adventureList = new LockableListModel();
	public static final SortedListModel encounterList = new SortedListModel();

	// Locations where session information is displayed for the user.
	// Include just the event history buffer and the command line buffer.

	public static final StyledChatBuffer commandBuffer = new StyledChatBuffer( "", "blue", false );
}
