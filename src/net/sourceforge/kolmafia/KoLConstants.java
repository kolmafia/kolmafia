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

import java.awt.Color;
import java.util.Vector;
import java.util.Random;
import java.util.Locale;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormatSymbols;

import java.io.File;
import javax.swing.JLabel;
import java.awt.Toolkit;

import java.util.TreeMap;
import java.util.ArrayList;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.java.dev.spellcast.utilities.LockableListModel;

public interface KoLConstants extends UtilityConstants
{
	// Version information for the current version of KoLmafia.
	// Rendered in various locations and therefore made public.

	public static final String VERSION_NAME = "KoLmafia v8.9";
	public static final String VERSION_DATE = "Released on September 22, 2006";

	// General constants used for calculations and formatting of
	// strings, as well as for string parsing.

	public static final Random RNG = new Random();
	public static final String LINE_BREAK = System.getProperty( "line.separator" );

	public static final DecimalFormat COMMA_FORMAT = new DecimalFormat( "#,##0", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat MODIFIER_FORMAT = new DecimalFormat( "+#0;-#0", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat FLOAT_FORMAT = new DecimalFormat( "#,##0.00", new DecimalFormatSymbols( Locale.US ) );
	public static final DecimalFormat ROUNDED_MODIFIER_FORMAT = new DecimalFormat( "+#0.00;-#0.00", new DecimalFormatSymbols( Locale.US ) );
	public static final SimpleDateFormat DATED_FILENAME_FORMAT = new SimpleDateFormat( "yyyyMMdd", Locale.US );

	// Generic constants which indicate null values.  Used in
	// order to preserve memory.

	public static final Class [] NOPARAMS = new Class[0];
	public static final JLabel BLANK_LABEL = new JLabel();

	// Constants which are used in order to do things inside of
	// the GUI.  Ensures that all GUI information can be accessed
	// at any time.

	public static final Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
	public static final LockableListModel existingFrames = new LockableListModel();
	public static final LockableListModel existingPanels = new LockableListModel();

	// Menus rendered in the relay browser and the KoLmafia mini-browser.
	// Ensures that the two menus always contain the same information.

	public static final String [][] FUNCTION_MENU = new String[][] {
		{ "Inventory", "inventory.php?which=1" },
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

	public static final String [][] GOTO_MENU = new String[][] {
		{ "Main Map", "main.php" },
		{ "Seaside Town", "town.php" },
		{ "Council", "council.php" },
		{ "Guild Hall", "guild.php" },
		{ "Clan Hall", "clan_hall.php" },
		{ "The Mall", "mall.php" },
		{ "Campground", "campground.php" },
		{ "Big Mountains", "mountains.php" },
		{ "Mt. McLargeHuge", "mclargehuge.php" },
		{ "Nearby Plains", "plains.php" },
		{ "The Beanstalk", "beanstalk.php" },
		{ "Sorceress' Lair", "lair.php" },
		{ "Desert Beach", "beach.php" },
		{ "Distant Woods", "woods.php" },
		{ "Mysterious Island", "island.php" }
	};

	// Scripting-related constants.  Used throughout KoLmafia in
	// order to ensure proper handling of scripts.

	public static final Vector commandQueue = new Vector();
	public static final LockableListModel scripts = new LockableListModel();
	public static final File SCRIPT_DIRECTORY = new File( "scripts" );
	public static final KoLmafiaCLI DEFAULT_SHELL = new KoLmafiaCLI( System.in );

	// Different states of KoLmafia.  Used in order to determine
	// what is still permitted.

	public static final int ENABLE_STATE   = 1;
	public static final int ERROR_STATE    = 2;
	public static final int ABORT_STATE    = 3;
	public static final int PENDING_STATE  = 4;
	public static final int CONTINUE_STATE = 5;

	// Colors which are used to handle the various KoLmafia states.
	// Used when changing the display.

	public static final Color ERROR_COLOR = new Color( 255, 192, 192 );
	public static final Color ENABLED_COLOR = new Color( 192, 255, 192 );
	public static final Color DISABLED_COLOR = null;

	// Constants which are useful, but not necessarily used very often.
	// Includes win game text.

	public static final String IMAGE_SERVER = "69.16.150.201";
	public static final String [][] WIN_GAME_TEXT = new String [][]
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
			"Moxie sign backdoor accessed.  Supertinkering The Ultimate Weapon...",
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
			"You touch your star starfish!  You surge with power!",
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
		}
	};

	// Variables which relate to a given session.  These are made
	// global in order to ensure that any element of KoLmafia can
	// access session-specific information.

	public static final SortedListModel saveStateNames = new SortedListModel();

	public static final SortedListModel inventory = new SortedListModel( AdventureResult.class );
	public static final SortedListModel closet = new SortedListModel( AdventureResult.class );
	public static final SortedListModel storage = new SortedListModel( AdventureResult.class );

	public static final SortedListModel collection = new SortedListModel( AdventureResult.class );
	public static final SortedListModel usables = new SortedListModel( AdventureResult.class );
	public static final SortedListModel sellables = new SortedListModel( AdventureResult.class );

	public static final SortedListModel usableSkills = new SortedListModel( UseSkillRequest.class );
	public static final LockableListModel availableSkills = new LockableListModel();
	public static final LockableListModel combatSkills = new LockableListModel();

	public static final LockableListModel activeEffects = new LockableListModel();
	public static final ArrayList recentEffects = new ArrayList();

	public static final TreeMap seenPlayerIDs = new TreeMap();
	public static final TreeMap seenPlayerNames = new TreeMap();
	public static final SortedListModel contactList = new SortedListModel();

	public static final SortedListModel tally = new SortedListModel();
	public static final SortedListModel missingItems = new SortedListModel();

	public static final SortedListModel hermitItems = new SortedListModel();
	public static final SortedListModel hunterItems = new SortedListModel();
	public static final String [] trapperItemNames = { "yak skin", "penguin skin", "hippopotamus skin" };
	public static final int [] trapperItemNumbers = { 394, 393, 395 };

	public static final LockableListModel restaurantItems = new LockableListModel();
	public static final LockableListModel microbreweryItems = new LockableListModel();

	public static final SortedListModel conditions = new SortedListModel();
	public static final LockableListModel adventureList = new LockableListModel();
	public static final SortedListModel encounterList = new SortedListModel();

	// Locations where session information is displayed for the user.
	// Include just the event history buffer and the command line buffer.

	public static final LockableListModel eventHistory = new LockableListModel();
	public static final LimitedSizeChatBuffer commandBuffer = new LimitedSizeChatBuffer( "KoLmafia: Graphical CLI", true, false );
}
