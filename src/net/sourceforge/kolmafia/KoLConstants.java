/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
import java.util.Locale;
import java.util.Random;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JTextField;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

public interface KoLConstants
	extends UtilityConstants
{
	// Version information for the current version of KoLmafia.
	// Rendered in various locations and therefore made public.

	public static final String REVISION = null;
	public static final String VERSION_NAME = "KoLmafia v12.6";
	public static final String VERSION_DATE = "Released on September 17, 2008";

	// General constants used for calculations and formatting of
	// strings, as well as for string parsing.

	public static final Random RNG = new Random();
	public static final Font DEFAULT_FONT = ( new JTextField() ).getFont();
	public static final String LINE_BREAK = System.getProperty( "line.separator" );
	public static final Pattern LINE_BREAK_PATTERN = Pattern.compile( "\\s*[\r\n]+\\s*" );

	public static final Pattern ANYTAG_PATTERN = Pattern.compile( "<.*?>" );
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
	public static final DecimalFormat ROUNDED_MODIFIER_FORMAT =
		new DecimalFormat( "+#0.00;-#0.00", new DecimalFormatSymbols( Locale.US ) );

	public static final SimpleDateFormat DAILY_FORMAT = new SimpleDateFormat( "yyyyMMdd", Locale.US );
	public static final SimpleDateFormat WEEKLY_FORMAT = new SimpleDateFormat( "yyyyMM_'w'W", Locale.US );

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

	public static final String[][] FUNCTION_MENU = new String[][]
	{
		{ "Consumables", "inventory.php?which=1" },
		{ "Equipment", "inventory.php?which=2" },
		{ "Misc Items", "inventory.php?which=3" },
		{ "Auto Sell", "sellstuff.php" },
		{ "Character", "charsheet.php" },
		{ "Quest Log", "questlog.php" },
		{ "Skills", "skills.php" },
		{ "Read Messages", "messages.php" },
		{ "Account Menu", "account.php" },
		{ "Documentation", "doc.php?topic=home" },
		{ "KoL Forums", "http://forums.kingdomofloathing.com/" },
		{ "Store", "http://store.asymmetric.net/" }
	};

	public static final String[][] GOTO_MENU = new String[][]
	{
		{ "Main Map", "main.php" },
		{ "Seaside Town", "town.php" },
		{ "Spookyraven", "manor.php" },
		{ "Council", "council.php" },
		{ "Guild Hall", "guild.php" },
		{ "Clan Hall", "clan_hall.php" },
		{ "The Mall", "mall.php" },
		{ "Campground", "campground.php" },
		{ "Big Mountains", "mountains.php" },
		{ "Mt. McLargeHuge", "mclargehuge.php" },
		{ "Nearby Plains", "plains.php" },
		{ "Cobb's Knob", "knob.php" },
		{ "Defiled Cyrpt", "cyrpt.php" },
		{ "The Beanstalk", "beanstalk.php" },
		{ "Sorceress' Lair", "lair.php" },
		{ "Desert Beach", "beach.php" },
		{ "Distant Woods", "woods.php" },
		{ "Mysterious Island", "island.php" }
	};

	public static final String[][] FRAME_NAMES =
	{
		{ "Adventure", "AdventureFrame" },
		{ "Mini-Browser", "RequestFrame" },
		{ "Relay Server", "LocalRelayServer" },
		{ "Purchases", "MallSearchFrame" },
		{ "Graphical CLI", "CommandDisplayFrame" },
		{ "Player Status", "CharSheetFrame" },
		{ "Item Manager", "ItemManageFrame" },
		{ "Gear Changer", "GearChangeFrame" },
		{ "Store Manager", "StoreManageFrame" },
		{ "Coin Masters", "CoinmastersFrame" },
		{ "Museum Display", "MuseumFrame" },
		{ "Hall of Legends", "MeatManageFrame" },
		{ "Skill Casting", "SkillBuffFrame" },
		{ "Contact List", "ContactListFrame" },
		{ "Buffbot Manager", "BuffBotFrame" },
		{ "Purchase Buffs", "BuffRequestFrame" },
		{ "Flower Hunter", "FlowerHunterFrame" },
		{ "Mushroom Plot", "MushroomFrame" },
		{ "Familiar Trainer", "FamiliarTrainingFrame" },
		{ "IcePenguin Express", "MailboxFrame" },
		{ "Loathing Chat", "ChatManager" },
		{ "Recent Events", "RecentEventsFrame" },
		{ "Clan Management", "ClanManageFrame" },
		{ "Farmer's Almanac", "CalendarFrame" },
		{ "Internal Database", "DatabaseFrame" },
		{ "Coin Toss Game", "MoneyMakingGameFrame" },
		{ "Preferences", "OptionsFrame" }
	};


	// Scripting-related constants.  Used throughout KoLmafia in
	// order to ensure proper handling of scripts.

	public static final LockableListModel scripts = new LockableListModel();
	public static final LockableListModel bookmarks = new LockableListModel();

	public static final ArrayList disabledScripts = new ArrayList();

	public static final SortedListModel junkList = new SortedListModel();
	public static final SortedListModel singletonList = new SortedListModel();
	public static final SortedListModel mementoList = new SortedListModel();
	public static final SortedListModel profitableList = new SortedListModel();

	public static final String ATTACKS_DIRECTORY = "attacks/";
	public static final String BUFFBOT_DIRECTORY = "buffs/";
	public static final String CCS_DIRECTORY = "ccs/";
	public static final String CHATLOG_DIRECTORY = "chats/";
	public static final String PLOTS_DIRECTORY = "planting/";
	public static final String SCRIPT_DIRECTORY = "scripts/";
	public static final String SESSIONS_DIRECTORY = "sessions/";
	public static final String RELAY_DIRECTORY = "relay/";

	public static final File ATTACKS_LOCATION =
		new File( UtilityConstants.ROOT_LOCATION, KoLConstants.ATTACKS_DIRECTORY );
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
		"combats.txt",
		"concoctions.txt",
		"defaults.txt",
		"equipment.txt",
		"familiars.txt",
		"fullness.txt",
		"inebriety.txt",
		"itemdescs.txt",
		"modifiers.txt",
		"monsters.txt",
		"npcstores.txt",
		"outfits.txt",
		"packages.txt",
		"pulverize.txt",
		"spleenhit.txt",
		"statuseffects.txt",
		"tradeitems.txt",
		"zapgroups.txt",
		"zonelist.txt"
	};

	public static final int STATIONARY_BUTTON_COUNT = 3;

	// The current version number of each data file

	public static final int ADVENTURES_VERSION = 3;
	public static final int BUFFBOTS_VERSION = 1;
	public static final int CLASSSKILLS_VERSION = 2;
	public static final int COINMASTERS_VERSION = 1;
	public static final int COMBATS_VERSION = 1;
	public static final int CONCOCTIONS_VERSION = 1;
	public static final int DEFAULTS_VERSION = 1;
	public static final int EQUIPMENT_VERSION = 2;
	public static final int FAMILIARS_VERSION = 1;
	public static final int FULLNESS_VERSION = 1;
	public static final int INEBRIETY_VERSION = 1;
	public static final int ITEMDESCS_VERSION = 1;
	public static final int MODIFIERS_VERSION = 1;
	public static final int MONSTERS_VERSION = 1;
	public static final int NPCSTORES_VERSION = 1;
	public static final int OUTFITS_VERSION = 1;
	public static final int PACKAGES_VERSION = 1;
	public static final int PULVERIZE_VERSION = 1;
	public static final int SPLEENHIT_VERSION = 2;
	public static final int STATUSEFFECTS_VERSION = 2;
	public static final int TRADEITEMS_VERSION = 1;
	public static final int ZAPGROUPS_VERSION = 1;
	public static final int ZONELIST_VERSION = 1;

	// Different states of KoLmafia.  Used in order to determine
	// what is still permitted.

	public static final int ENABLE_STATE = 1;
	public static final int ERROR_STATE = 2;
	public static final int ABORT_STATE = 3;
	public static final int PENDING_STATE = 4;
	public static final int CONTINUE_STATE = 5;

	// Stats / Zodiac Sign categories

	public static final int NONE = 0;
	public static final int MUSCLE = 1;
	public static final int MYSTICALITY = 2;
	public static final int MOXIE = 3;
	public static final int BAD_MOON = 4;

	// Weapon Types

	public static final int MELEE = 1;
	public static final int RANGED = 2;

	// Item consumption types

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
	public static final int CONSUME_SPECIAL = 20;
	public static final int CONSUME_SPHERE = 21;
	public static final int CONSUME_FOOD_HELPER = 22;
	public static final int CONSUME_DRINK_HELPER = 23;
	public static final int CONSUME_STICKER = 24;

	public static final int CONSUME_HOBO = 98;
	public static final int CONSUME_GHOST = 99;

	// Item creation types

	public static final int METHOD_COUNT = 30;
	public static final int SUBCLASS = Integer.MAX_VALUE;

	public static final int NOCREATE = 0;
	public static final int COMBINE = 1;
	public static final int COOK = 2;
	public static final int MIX = 3;
	public static final int SMITH = 4;

	public static final int COOK_REAGENT = 5;
	public static final int COOK_PASTA = 6;
	public static final int MIX_SPECIAL = 7;

	public static final int JEWELRY = 8;
	public static final int STARCHART = 9;
	public static final int PIXEL = 10;
	public static final int ROLLING_PIN = 11;

	public static final int GNOME_TINKER = 12;
	public static final int SMITH_WEAPON = 13;
	public static final int SMITH_ARMOR = 14;

	public static final int CRIMBO05 = 15;
	public static final int CREATE_VIA_USE = 16;

	public static final int STILL_BOOZE = 17;
	public static final int STILL_MIXER = 18;
	public static final int MIX_SUPER = 19;

	public static final int CATALYST = 20;
	public static final int SUPER_REAGENT = 21;
	public static final int WOK = 22;
	public static final int MALUS = 23;

	public static final int CRIMBO06 = 24;

	public static final int EXPENSIVE_JEWELRY = 25;
	public static final int STAFF = 26;
	public static final int MULTI_USE = 27;
	public static final int SINGLE_USE = 28;

	public static final int CRIMBO07 = 29;

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
	public static final SortedListModel collection = new SortedListModel();
	public static final SortedListModel campground = new SortedListModel();
	public static final SortedListModel pulverizeQueue = new SortedListModel();

	public static final LockableListModel usableSkills = new LockableListModel();
	public static final LockableListModel summoningSkills = new LockableListModel();
	public static final LockableListModel remedySkills = new LockableListModel();
	public static final LockableListModel selfOnlySkills = new LockableListModel();
	public static final LockableListModel buffSkills = new LockableListModel();
	public static final LockableListModel availableSkills = new LockableListModel();
	public static final LockableListModel combatSkills = new LockableListModel();

	public static final LockableListModel activeEffects = new LockableListModel();
	public static final ArrayList recentEffects = new ArrayList();

	public static final HashMap seenPlayerIds = new HashMap();
	public static final HashMap seenPlayerNames = new HashMap();
	public static final SortedListModel contactList = new SortedListModel();

	public static final SortedListModel hermitItems = new SortedListModel();
	public static final SortedListModel trapperItems = new SortedListModel();
	public static final LockableListModel restaurantItems = new LockableListModel();
	public static final LockableListModel microbreweryItems = new LockableListModel();
	public static final LockableListModel kitchenItems = new LockableListModel();
	public static final LockableListModel cafeItems = new LockableListModel();

	public static final SortedListModel tally = new SortedListModel();
	public static final SortedListModel conditions = new SortedListModel();
	public static final LockableListModel adventureList = new LockableListModel();
	public static final SortedListModel encounterList = new SortedListModel();

	// Locations where session information is displayed for the user.
	// Include just the event history buffer and the command line buffer.

	public static final LimitedSizeChatBuffer commandBuffer = new LimitedSizeChatBuffer();
}
