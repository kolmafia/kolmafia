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

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.UIManager;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.HPRestoreItemList.HPRestoreItem;
import net.sourceforge.kolmafia.MPRestoreItemList.MPRestoreItem;
import net.sourceforge.kolmafia.StoreManager.SoldItem;

public abstract class KoLmafia
	implements KoLConstants
{
	private static boolean isRefreshing = false;
	private static boolean isAdventuring = false;

	public static String lastMessage = "";

	static
	{
		System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "KoLmafia" );
		System.setProperty( "com.apple.mrj.application.live-resize", "true" );
		System.setProperty( "com.apple.mrj.application.growbox.intrudes", "false" );

		JEditorPane.registerEditorKitForContentType( "text/html", RequestEditorKit.class.getName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		System.setProperty( "http.referer", "www.kingdomofloathing.com" );
	}

	private static boolean hadPendingState = false;

	protected static String currentIterationString = "";
	protected static boolean recoveryActive = false;

	public static boolean isMakingRequest = false;
	public static int continuationState = KoLConstants.CONTINUE_STATE;

	public static final int[] initialStats = new int[ 3 ];

	public static boolean executedLogin = false;

	private static final Pattern FUMBLE_PATTERN =
		Pattern.compile( "You drop your .*? on your .*?, doing ([\\d,]+) damage" );
	private static final Pattern STABBAT_PATTERN = Pattern.compile( " stabs you for ([\\d,]+) damage" );
	private static final Pattern CARBS_PATTERN =
		Pattern.compile( "some of your blood, to the tune of ([\\d,]+) damage" );
	private static final Pattern TAVERN_PATTERN = Pattern.compile( "where=(\\d+)" );
	private static final Pattern GOURD_PATTERN = Pattern.compile( "Bring back (\\d+)" );
	private static final Pattern DISCARD_PATTERN = Pattern.compile( "You discard your (.*?)\\." );

	private static final AdventureResult CATNIP = new AdventureResult( 1486, 1 );
	private static final AdventureResult GLIDER = new AdventureResult( 1487, 1 );
	public static final AdventureResult SATCHEL = new AdventureResult( 1656, 1 );
	public static final AdventureResult NOVELTY_BUTTON = new AdventureResult( 2072, 1 );

	private static final AdventureResult MANUAL_1 = new AdventureResult( 2280, 1 );
	private static final AdventureResult MANUAL_2 = new AdventureResult( 2281, 1 );
	private static final AdventureResult MANUAL_3 = new AdventureResult( 2282, 1 );

	private static final AdventureResult [] CRIMBO_TOYS =
	{
		new AdventureResult( 3092, 1 ),		// handmade hobby horse
		new AdventureResult( 3093, 1 ),		// ball-in-a-cup
		new AdventureResult( 3094, 1 ),		// set of jacks
	};

	private static final int SOCK = 609;
	private static final int LUCRE = 2098;

	// Steel items
	private static final int LASAGNA = 2742;
	private static final int MARGARITA = 2743;
	private static final int AIR_FRESHENER = 2744;

	/* Gnome postal items */
	private static final int RED_PAPER_CLIP = 2289;
	private static final int REALLY_BIG_TINY_HOUSE = 2290;
	private static final int NONESSENTIAL_AMULET = 2291;
	private static final int WHITE_WINE_VINIGARETTE = 2292;
	private static final int CUP_OF_STRONG_TEA = 2293;
	private static final int CURIOUSLY_SHINY_AX = 2294;
	private static final int MARINATED_STAKES = 2295;
	private static final int KNOB_BUTTER = 2296;
	private static final int VIAL_OF_ECTOPLASM = 2297;
	private static final int BOOCK_OF_MAGIKS = 2298;
	private static final int EZ_PLAY_HARMONICA_BOOK = 2299;
	private static final int FINGERLESS_HOBO_GLOVES = 2300;
	private static final int CHOMSKYS_COMICS = 2301;
	private static final int GNOME_DEMODULIZER = 2848;

	// Semi-rares
	private static final int ASCII_SHIRT = 2121;
	private static final int RHINO_HORMONES = 2419;
	private static final int MAGIC_SCROLL = 2420;
	private static final int PIRATE_JUICE = 2421;
	private static final int PET_SNACKS = 2422;
	private static final int INHALER = 2423;
	private static final int CYCLOPS_EYEDROPS = 2424;
	private static final int SPINACH = 2425;
	private static final int FIRE_FLOWER = 2426;
	private static final int ICE_CUBE = 2427;
	private static final int FAKE_BLOOD = 2428;
	private static final int GUANEAU = 2429;
	private static final int LARD = 2430;
	private static final int MYTIC_SHELL = 2431;
	private static final int LIP_BALM = 2432;
	private static final int ANTIFREEZE = 2433;
	private static final int BLACK_EYEDROPS = 2434;
	private static final int DOGSGOTNONOZ = 2435;
	private static final int FLIPBOOK = 2436;
	private static final int NEW_CLOACA_COLA = 2437;
	private static final int MASSAGE_OIL = 2438;
	private static final int POLTERGEIST = 2439;
	private static final int TASTY_TART = 2591;
	private static final int LUNCHBOX = 2592;
	private static final int KNOB_PASTY = 2593;
	private static final int KNOB_COFFEE = 2594;

	private static FileLock SESSION_HOLDER = null;
	private static FileChannel SESSION_CHANNEL = null;
	private static File SESSION_FILE = null;

	private static KoLAdventure currentAdventure;

	// Types of special encounters
	public static final String NONE = "0";
	public static final String STOP = "1";
	public static final String SEMIRARE = "2";

	public static final String[][] SPECIAL_ENCOUNTERS =
	{
		{ "History is Fun!", STOP },
		{ "It's A Sign!", STOP },
		{ "The Manor in Which You're Accustomed", STOP },
		{ "Under the Knife", STOP },
		{ "The Oracle Will See You Now", STOP },
		{ "A Grave Situation", STOP },
		{ "Take a Dusty Look!", STOP },
		{ "Drawn Onward", STOP },
		{ "Mr. Alarm", STOP },
		{ "We'll All Be Flat", STOP },
		{ "It's Always Swordfish", STOP },

		// Adventures that start the Around the World Quest

		{ "I Just Wanna Fly", STOP },
		{ "Me Just Want Fly", STOP },

		// Adventure in the Arid, Extra-Dry desert until you find the
		// Oasis

		{ "Let's Make a Deal!", STOP },

		// Get Ultra-hydrated and adventure in the Arid, Extra-Dry
		// desert until you are given the task to find a stone rose.

		{ "A Sietch in Time", STOP },

		// Adventure in Oasis until you have a stone rose and a drum
		// machine. Buy black paint.

		// Come back to the Arid, Extra-Dry Desert and adventure until
		// you are tasked to find the missing pages from the
		// worm-riding manual.

		{ "Walk Without Rhythm", STOP },

		// Adventure in Oasis until you have worm-riding manual pages
		// 3-15 Adventure in Arid, Extra-Dry Desert until you have
		// worm-riding hooks.

		// Adventures that give semirares
		{ "A Menacing Phantom", SEMIRARE },
		{ "All The Rave", SEMIRARE },
		{ "Bad ASCII Art", SEMIRARE },
		{ "Blaaargh! Blaaargh!", SEMIRARE },
		{ "Filth, Filth, and More Filth", SEMIRARE },
		{ "Hands On", SEMIRARE },
		{ "How Does He Smell?", SEMIRARE },
		{ "In the Still of the Alley", SEMIRARE },
		{ "It's The Only Way To Be Sure", SEMIRARE },
		{ "Knob Goblin Elite Guard Captain", SEMIRARE },
		{ "Knob Goblin Embezzler", SEMIRARE },
		{ "Le Chauve-Souris du Parfum", SEMIRARE },
		{ "Like the Sunglasses, But Less Comfortable", SEMIRARE },
		{ "Lunchboxing", SEMIRARE },
		{ "Monty of County Crisco", SEMIRARE },
		{ "Natural Selection", SEMIRARE },
		{ "Not Quite as Cold as Ice", SEMIRARE },
		{ "Play Misty For Me", SEMIRARE },
		{ "Prior to Always", SEMIRARE },
		{ "Rokay, Raggy!", SEMIRARE },
		{ "Sand in the Vaseline", SEMIRARE },
		{ "Some Bricks Do, In Fact, Hang in the Air", SEMIRARE },
		{ "The Bleary-Eyed Cyclops", SEMIRARE },
		{ "The Latest Sorcerous Developments", SEMIRARE },
		{ "The Pilsbury Doughjerk", SEMIRARE },
		{ "Two Sizes Too Small", SEMIRARE },
		{ "Yo Ho Ho and a Bottle of Whatever This Is", SEMIRARE },
		{ "You Can Top Our Desserts, But You Can't Beat Our Meats", SEMIRARE },
	};

	private static final boolean acquireFileLock( final String suffix )
	{
		try
		{
			KoLmafia.SESSION_FILE = new File( KoLConstants.SESSIONS_LOCATION, "active_session." + suffix );

			if ( KoLmafia.SESSION_FILE.exists() )
			{
				KoLmafia.SESSION_CHANNEL = new RandomAccessFile( KoLmafia.SESSION_FILE, "rw" ).getChannel();
				KoLmafia.SESSION_HOLDER = KoLmafia.SESSION_CHANNEL.tryLock();
				return KoLmafia.SESSION_HOLDER != null;
			}

			LogStream ostream = LogStream.openStream( KoLmafia.SESSION_FILE, true );
			ostream.println( KoLConstants.VERSION_NAME );
			ostream.close();

			KoLmafia.SESSION_CHANNEL = new RandomAccessFile( KoLmafia.SESSION_FILE, "rw" ).getChannel();
			KoLmafia.SESSION_HOLDER = KoLmafia.SESSION_CHANNEL.lock();
			return true;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	/**
	 * The main method. Currently, it instantiates a single instance of the <code>KoLmafiaGUI</code>.
	 */

	public static final void main( final String[] args )
	{
		boolean useGUI = true;
		System.setProperty( "http.agent", KoLRequest.getUserAgent() );

		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[ i ].equals( "--CLI" ) )
			{
				useGUI = false;
			}
			if ( args[ i ].equals( "--GUI" ) )
			{
				useGUI = true;
			}
		}

		KoLConstants.hermitItems.add( new AdventureResult( "banjo strings", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "catsup", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "chisel", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "dingy planks", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "golden twig", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "hot buttered roll", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "jaba\u00f1ero pepper", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "ketchup", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "petrified noodles", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "seal tooth", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "sweet rims", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "volleyball", 1, false ) );
		KoLConstants.hermitItems.add( new AdventureResult( "wooden figurine", 1, false ) );

		KoLConstants.trapperItems.add( new AdventureResult( 394, 1 ) );
		KoLConstants.trapperItems.add( new AdventureResult( 393, 1 ) );
		KoLConstants.trapperItems.add( new AdventureResult( 395, 1 ) );

		// Change it so that it doesn't recognize daylight savings in order
		// to ensure different localizations work.

		TimeZone koltime = (TimeZone) TimeZone.getDefault().clone();
		koltime.setRawOffset( 1000 * 60 * -270 );
		KoLConstants.DAILY_FORMAT.setTimeZone( koltime );

		// Reload your settings and determine all the different users which
		// are present in your save state list.

		KoLSettings.setUserProperty(
			"defaultLoginServer", String.valueOf( 1 + KoLConstants.RNG.nextInt( KoLRequest.SERVER_COUNT ) ) );
		KoLSettings.setUserProperty( "relayBrowserOnly", "false" );

		String actualName;
		String[] pastUsers = StaticEntity.getPastUserList();

		for ( int i = 0; i < pastUsers.length; ++i )
		{
			if ( pastUsers[ i ].startsWith( "devster" ) )
			{
				continue;
			}

			actualName = KoLSettings.getGlobalProperty( pastUsers[ i ], "displayName" );
			if ( actualName.equals( "" ) )
			{
				actualName = StaticEntity.globalStringReplace( pastUsers[ i ], "_", " " );
			}

			KoLConstants.saveStateNames.add( actualName );
		}

		// Also clear out any outdated data files.  Include the
		// adventure table, in case this is causing problems.

		String version = KoLSettings.getUserProperty( "previousUpdateVersion" );

		if ( version == null || !version.equals( KoLConstants.VERSION_NAME ) )
		{
			KoLSettings.setUserProperty( "previousUpdateVersion", KoLConstants.VERSION_NAME );
			for ( int i = 0; i < KoLConstants.OVERRIDE_DATA.length; ++i )
			{
				File outdated = new File( UtilityConstants.DATA_LOCATION, KoLConstants.OVERRIDE_DATA[ i ] );
				if ( outdated.exists() )
				{
					outdated.delete();
				}

				KoLmafia.deleteSimulator( new File( KoLConstants.RELAY_LOCATION, "simulator" ) );
			}
		}

		LimitedSizeChatBuffer.updateFontSize();

		// Change the default look and feel to match the player's
		// preferences.  Always do this.

		String lookAndFeel = KoLSettings.getUserProperty( "swingLookAndFeel" );
		boolean foundLookAndFeel = false;

		if ( lookAndFeel.equals( "" ) )
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) || System.getProperty( "os.name" ).startsWith(
				"Win" ) )
			{
				lookAndFeel = UIManager.getSystemLookAndFeelClassName();
			}
			else
			{
				lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
			}
		}

		UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
		String[] installedLooks = new String[ installed.length ];

		for ( int i = 0; i < installedLooks.length; ++i )
		{
			installedLooks[ i ] = installed[ i ].getClassName();
		}

		for ( int i = 0; i < installedLooks.length; ++i )
		{
			foundLookAndFeel |= installedLooks[ i ].equals( lookAndFeel );
		}

		if ( !foundLookAndFeel )
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) || System.getProperty( "os.name" ).startsWith(
				"Win" ) )
			{
				lookAndFeel = UIManager.getSystemLookAndFeelClassName();
			}
			else
			{
				lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
			}

			foundLookAndFeel = true;
		}

		try
		{
			UIManager.setLookAndFeel( lookAndFeel );
			JFrame.setDefaultLookAndFeelDecorated( System.getProperty( "os.name" ).startsWith( "Mac" ) );
		}
		catch ( Exception e )
		{
			// Should not happen, as we checked to see if
			// the look and feel was installed first.

			JFrame.setDefaultLookAndFeelDecorated( true );
		}

		if ( StaticEntity.usesSystemTray() )
		{
			SystemTrayFrame.addTrayIcon();
		}

		KoLSettings.setUserProperty( "swingLookAndFeel", lookAndFeel );

		if ( System.getProperty( "os.name" ).startsWith( "Win" ) || lookAndFeel.equals( UIManager.getCrossPlatformLookAndFeelClassName() ) )
		{
			UIManager.put( "ProgressBar.foreground", Color.black );
			UIManager.put( "ProgressBar.selectionForeground", Color.lightGray );

			UIManager.put( "ProgressBar.background", Color.lightGray );
			UIManager.put( "ProgressBar.selectionBackground", Color.black );
		}

		tab.CloseTabPaneEnhancedUI.selectedA = DataUtilities.toColor( KoLSettings.getUserProperty( "innerTabColor" ) );
		tab.CloseTabPaneEnhancedUI.selectedB = DataUtilities.toColor( KoLSettings.getUserProperty( "outerTabColor" ) );

		tab.CloseTabPaneEnhancedUI.notifiedA = DataUtilities.toColor( KoLSettings.getUserProperty( "innerChatColor" ) );
		tab.CloseTabPaneEnhancedUI.notifiedB = DataUtilities.toColor( KoLSettings.getUserProperty( "outerChatColor" ) );

		if ( !KoLmafia.acquireFileLock( "1" ) && !KoLmafia.acquireFileLock( "2" ) )
		{
			System.exit( -1 );
		}

		KoLSettings.initializeLists();
		Runtime.getRuntime().addShutdownHook( new ShutdownThread() );

		// Now run the main routines for each, so that
		// you have an interface.

		if ( useGUI )
		{
			KoLmafiaGUI.initialize();
		}
		else
		{
			KoLmafiaCLI.initialize();
		}

		// Now, maybe the person wishes to run something
		// on startup, and they associated KoLmafia with
		// some non-ASH file extension.  This will run it.

		StringBuffer initialScript = new StringBuffer();

		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[ i ].equalsIgnoreCase( "--CLI" ) )
			{
				continue;
			}

			initialScript.append( args[ i ] );
			initialScript.append( " " );
		}

		if ( initialScript.length() != 0 )
		{
			String actualScript = initialScript.toString().trim();
			if ( actualScript.startsWith( "script=" ) )
			{
				actualScript = actualScript.substring( 7 );
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + actualScript );
		}
		else if ( !useGUI )
		{
			KoLmafiaCLI.DEFAULT_SHELL.attemptLogin( "" );
		}

		// Check for KoLmafia updates in a separate thread
		// so as to allow for continued execution.

		( new UpdateCheckThread() ).start();

		// Always read input from the command line when you're not
		// in GUI mode.

		if ( !useGUI )
		{
			KoLmafiaCLI.DEFAULT_SHELL.listenForCommands();
		}
	}

	private static final void deleteSimulator( final File location )
	{
		if ( location.isDirectory() )
		{
			File[] files = location.listFiles();
			for ( int i = 0; i < files.length; ++i )
			{
				KoLmafia.deleteSimulator( files[ i ] );
			}
		}

		location.delete();
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object. All data fields are initialized to their default values, the
	 * global settings are loaded from disk.
	 */

	public KoLmafia()
	{
	}

	public static final String getLastMessage()
	{
		return KoLmafia.lastMessage;
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code> session.
	 */

	public static final void updateDisplay( final String message )
	{
		KoLmafia.updateDisplay( KoLConstants.CONTINUE_STATE, message );
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code> session.
	 */

	public static final void updateDisplay( final int state, final String message )
	{
		if ( KoLmafia.continuationState == KoLConstants.ABORT_STATE && state != KoLConstants.ABORT_STATE )
		{
			return;
		}

		if ( KoLmafia.continuationState != KoLConstants.PENDING_STATE )
		{
			KoLmafia.continuationState = state;
		}

		RequestLogger.printLine( state, message );
		SystemTrayFrame.updateToolTip( message );
		KoLmafia.lastMessage = message;

		if ( message.indexOf( KoLConstants.LINE_BREAK ) == -1 )
		{
			KoLmafia.updateDisplayState( state, RequestEditorKit.getStripped( message ) );
		}
	}

	private static final void updateDisplayState( final int state, final String message )
	{
		// Next, update all of the panels with the
		// desired update message.

		ActionPanel[] panels = StaticEntity.getExistingPanels();

		for ( int i = 0; i < panels.length; ++i )
		{
			if ( panels[ i ] instanceof KoLPanel )
			{
				( (KoLPanel) panels[ i ] ).setStatusMessage( message );
			}

			panels[ i ].setEnabled( state != KoLConstants.CONTINUE_STATE );
		}

		KoLFrame[] frames = StaticEntity.getExistingFrames();
		for ( int i = 0; i < frames.length; ++i )
		{
			frames[ i ].setStatusMessage( message );
			frames[ i ].updateDisplayState( state );
		}

		if ( KoLDesktop.instanceExists() )
		{
			KoLDesktop.getInstance().updateDisplayState( state );
		}
	}

	public static final void enableDisplay()
	{
		KoLmafia.updateDisplayState(
			KoLmafia.continuationState == KoLConstants.ABORT_STATE || KoLmafia.continuationState == KoLConstants.ERROR_STATE ? KoLConstants.ERROR_STATE : KoLConstants.ENABLE_STATE,
			"" );
		KoLmafia.continuationState = KoLConstants.CONTINUE_STATE;
	}

	public static final boolean executedLogin()
	{
		return KoLmafia.executedLogin;
	}

	/**
	 * Initializes the <code>KoLmafia</code> session. Called after the login has been confirmed to notify thethat the
	 * login was successful, the user-specific settings should be loaded, and the user can begin adventuring.
	 */

	public void initialize( final String username )
	{
		String originalName = KoLCharacter.getUserName();

		// Initialize the variables to their initial
		// states to avoid null pointers getting thrown
		// all over the place

		KoLmafia.executedLogin = true;

		KoLmafia.updateDisplay( "Initializing session for " + username + "..." );
		KoLSettings.setUserProperty( "lastUsername", username );

		// Reset all per-player information when refreshing
		// your session via login.

		KoLCharacter.reset( username );

		KoLMailManager.clearMailboxes();
		StoreManager.clearCache();
		MuseumManager.clearCache();
		ClanManager.clearCache();

		// Now actually reset the session.

		this.refreshSession();
		RequestLogger.openSessionLog();
		this.resetSession();

		if ( KoLSettings.getBooleanProperty( "logStatusOnLogin" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "log", "snapshot" );
		}

		// If the password hash is non-null, then that means you
		// might be mid-transition.

		if ( KoLRequest.passwordHash != null && KoLRequest.passwordHash.equals( "" ) )
		{
			return;
		}

		int today = MoonPhaseDatabase.getPhaseStep();
		if ( KoLSettings.getIntegerProperty( "lastCounterDay" ) != today )
		{
			KoLmafia.resetCounters();
		}

		KoLmafia.registerPlayer( username, String.valueOf( KoLCharacter.getUserId() ) );

		if ( KoLSettings.getGlobalProperty( username, "getBreakfast" ).equals( "true" ) )
		{
			this.getBreakfast( true, KoLSettings.getIntegerProperty( "lastBreakfast" ) != today );
			KoLSettings.setUserProperty( "lastBreakfast", String.valueOf( today ) );
		}

		// Also, do mushrooms, if a mushroom script has already
		// been setup by the user.

		if ( KoLSettings.getBooleanProperty( "autoPlant" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			String currentLayout = KoLSettings.getUserProperty( "plantingScript" );
			if ( !currentLayout.equals( "" ) && KoLCharacter.inMuscleSign() && MushroomPlot.ownsPlot() )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + KoLConstants.PLOTS_DIRECTORY + currentLayout + ".ash" );
			}
		}

		String scriptSetting = KoLSettings.getUserProperty( "loginScript" );
		if ( !scriptSetting.equals( "" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptSetting );
		}
	}

	public static final void resetCounters()
	{
		KoLSettings.setUserProperty( "lastCounterDay", String.valueOf( MoonPhaseDatabase.getPhaseStep() ) );
		KoLSettings.setUserProperty( "breakfastCompleted", "false" );

		KoLSettings.setUserProperty( "expressCardUsed", "false" );
		KoLSettings.setUserProperty( "currentFullness", "0" );
		KoLSettings.setUserProperty( "currentMojoFilters", "0" );
		KoLSettings.setUserProperty( "currentSpleenUse", "0" );
		KoLSettings.setUserProperty( "currentPvpVictories", "" );

		KoLSettings.setUserProperty( "snowconeSummons", "0" );
		KoLSettings.setUserProperty( "grimoireSummons", "0" );
		KoLSettings.setUserProperty( "candyHeartSummons", "0" );

		KoLSettings.setUserProperty( "noodleSummons", "0" );
		KoLSettings.setUserProperty( "reagentSummons", "0" );
		KoLSettings.setUserProperty( "cocktailSummons", "0" );

		// Libram summoning skills now costs 1 MP again
		KoLConstants.summoningSkills.sort();
		KoLConstants.usableSkills.sort();
	}

	public static final void resetPerAscensionCounters()
	{
		KoLSettings.setUserProperty( "currentBountyItem", "0" );
		KoLSettings.setUserProperty( "currentHippyStore", "none" );
		KoLSettings.setUserProperty( "currentWheelPosition", "muscle" );
		KoLSettings.setUserProperty( "fratboysDefeated", "0" );
		KoLSettings.setUserProperty( "guyMadeOfBeesCount", "0" );
		KoLSettings.setUserProperty( "guyMadeOfBeesDefeated", "false" );
		KoLSettings.setUserProperty( "hippiesDefeated", "0" );
		KoLSettings.setUserProperty( "trapperOre", "chrome" );
	}

	public void getBreakfast( final boolean checkSettings, final boolean checkCampground )
	{
		if ( checkCampground )
		{
			if ( KoLCharacter.hasToaster() )
			{
				for ( int i = 0; i < 3 && KoLmafia.permitsContinue(); ++i )
				{
					RequestThread.postRequest( new CampgroundRequest( "toast" ) );
				}

				KoLmafia.forceContinue();
			}

			if ( KoLCharacter.hasArches() )
			{
				RequestThread.postRequest( new CampgroundRequest( "arches" ) );
				KoLmafia.forceContinue();
			}

			if ( KoLSettings.getBooleanProperty( "visitRumpus" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
			{
				RequestThread.postRequest( new ClanGymRequest( ClanGymRequest.SEARCH ) );
				KoLmafia.forceContinue();
			}

			if ( KoLSettings.getBooleanProperty( "readManual" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
			{
				AdventureResult manual =
					KoLCharacter.isMuscleClass() ? KoLmafia.MANUAL_1 : KoLCharacter.isMysticalityClass() ? KoLmafia.MANUAL_2 : KoLmafia.MANUAL_3;
				if ( KoLCharacter.hasItem( manual ) )
				{
					RequestThread.postRequest( new ConsumeItemRequest( manual ) );
				}

				KoLmafia.forceContinue();
			}

			if ( KoLSettings.getBooleanProperty( "useCrimboToys" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
			{
				for ( int i = 0; i < CRIMBO_TOYS.length; ++i )
				{
					AdventureResult toy = CRIMBO_TOYS[i];
					if ( KoLCharacter.hasItem( toy ) )
					{
						RequestThread.postRequest( new ConsumeItemRequest( toy ) );
						KoLmafia.forceContinue();
					}
				}
			}

			if ( KoLSettings.getBooleanProperty( "grabClovers" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
			{
				if ( HermitRequest.getWorthlessItemCount() > 0 )
				{
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hermit * ten-leaf clover" );
				}

				KoLmafia.forceContinue();
			}

			if ( KoLSettings.getIntegerProperty( "lastFilthClearance" ) == KoLCharacter.getAscensions() )
			{
				KoLmafia.updateDisplay( "Collecting cut of hippy profits..." );
				RequestThread.postRequest( new KoLRequest( "store.php?whichstore=h" ) );
				KoLmafia.forceContinue();
			}
		}

		SpecialOutfit.createImplicitCheckpoint();
		this.castBreakfastSkills( checkSettings, 0 );
		SpecialOutfit.restoreImplicitCheckpoint();
		KoLmafia.forceContinue();
	}

	public void castBreakfastSkills( final boolean checkSettings, final int manaRemaining )
	{
		this.castBreakfastSkills(
			checkSettings,
			KoLSettings.getBooleanProperty( "loginRecovery" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ),
			manaRemaining );
	}

	public boolean castBreakfastSkills( boolean checkSettings, final boolean allowRestore, final int manaRemaining )
	{
		if ( KoLSettings.getBooleanProperty( "breakfastCompleted" ) )
		{
			return true;
		}

		boolean shouldCast = false;
		boolean limitExceeded = true;

		String skillSetting =
			KoLSettings.getUserProperty( "breakfast" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );
		boolean pathedSummons =
			KoLSettings.getBooleanProperty( "pathedSummons" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) );

		if ( skillSetting != null )
		{
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				shouldCast = !checkSettings || skillSetting.indexOf( UseSkillRequest.BREAKFAST_SKILLS[ i ] ) != -1;
				shouldCast &= KoLCharacter.hasSkill( UseSkillRequest.BREAKFAST_SKILLS[ i ] );

				if ( checkSettings && pathedSummons )
				{
					if ( UseSkillRequest.BREAKFAST_SKILLS[ i ].equals( "Pastamastery" ) && !KoLCharacter.canEat() )
					{
						shouldCast = false;
					}
					if ( UseSkillRequest.BREAKFAST_SKILLS[ i ].equals( "Advanced Cocktailcrafting" ) && !KoLCharacter.canDrink() )
					{
						shouldCast = false;
					}
				}

				if ( shouldCast )
				{
					limitExceeded &=
						this.getBreakfast( UseSkillRequest.BREAKFAST_SKILLS[ i ], allowRestore, manaRemaining );
				}
			}
		}

		KoLSettings.setUserProperty( "breakfastCompleted", String.valueOf( limitExceeded ) );
		return limitExceeded;
	}

	public boolean getBreakfast( final String skillName, final boolean allowRestore, final int manaRemaining )
	{
		UseSkillRequest summon = UseSkillRequest.getInstance( skillName );
		// For all other skills, if you don't need to cast them, then
		// skip this step.

		int maximumCast = summon.getMaximumCast();

		if ( maximumCast <= 0 )
		{
			return true;
		}

		int castCount =
			Math.min(
				maximumCast,
				allowRestore ? 5 : ( KoLCharacter.getCurrentMP() - manaRemaining ) / ClassSkillsDatabase.getMPConsumptionById( ClassSkillsDatabase.getSkillId( skillName ) ) );

		if ( castCount == 0 )
		{
			return false;
		}

		summon.setBuffCount( castCount );
		RequestThread.postRequest( summon );

		return castCount == maximumCast;
	}

	public void refreshSession()
	{
		this.refreshSession( true );
	}

	public void refreshSession( final boolean getQuestLog )
	{
		KoLmafia.isRefreshing = true;

		// Get current moon phases

		KoLmafia.updateDisplay( "Refreshing session data..." );

		RequestThread.postRequest( new MoonPhaseRequest() );
		KoLCharacter.setHoliday( MoonPhaseDatabase.getHoliday( new Date() ) );

		// Retrieve the character sheet first. It's necessary to do
		// this before concoctions have a chance to get refreshed.

		RequestThread.postRequest( new CharsheetRequest() );

		if ( getQuestLog )
		{
			RequestThread.postRequest( new AccountRequest() );
			RequestThread.postRequest( new QuestLogRequest() );
		}

		// Clear the violet fog path table and everything
		// else that changes on the player.

		VioletFog.reset();
		Louvre.reset();
		MushroomPlot.reset();
		HermitRequest.resetClovers();
		TradeableItemDatabase.getDustyBottles();

		// Retrieve the items which are available for consumption
		// and item creation.

		if ( getQuestLog )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.CLOSET ) );
		}

		// If the password hash is non-null, but is not available,
		// then that means you might be mid-transition.

		if ( KoLRequest.passwordHash != null && KoLRequest.passwordHash.equals( "" ) )
		{
			return;
		}

		// Retrieve the list of familiars which are available to
		// the player.

		RequestThread.postRequest( new FamiliarRequest() );

		// Retrieve campground data to see if the user is able to
		// cook, make drinks or make toast.

		if ( getQuestLog )
		{
			KoLmafia.updateDisplay( "Retrieving campground data..." );
			RequestThread.postRequest( new CampgroundRequest() );
			KoLCharacter.checkTelescope();
		}

		if ( !KoLCharacter.inBadMoon() && KoLSettings.getIntegerProperty( "lastEmptiedStorage" ) != KoLCharacter.getAscensions() )
		{
			RequestThread.postRequest( new ItemStorageRequest() );
			if ( KoLConstants.storage.isEmpty() )
			{
				KoLSettings.setUserProperty( "lastEmptiedStorage", String.valueOf( KoLCharacter.getAscensions() ) );
			}
		}

		// Charpane can set counters, so load saved counters first.
		StaticEntity.loadCounters();
		RequestThread.postRequest( CharpaneRequest.getInstance() );

		KoLmafia.updateDisplay( "Session data refreshed." );

		KoLmafia.isRefreshing = false;
	}

	public static final boolean isRefreshing()
	{
		return KoLmafia.isRefreshing;
	}

	/**
	 * Used to reset the session tally to its original values.
	 */

	public void resetSession()
	{
		KoLConstants.encounterList.clear();
		KoLConstants.adventureList.clear();

		KoLmafia.initialStats[ 0 ] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() );
		KoLmafia.initialStats[ 1 ] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() );
		KoLmafia.initialStats[ 2 ] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() );

		AdventureResult.SESSION_FULLSTATS[ 0 ] = 0;
		AdventureResult.SESSION_FULLSTATS[ 1 ] = 0;
		AdventureResult.SESSION_FULLSTATS[ 2 ] = 0;

		KoLConstants.tally.clear();
		KoLConstants.tally.add( new AdventureResult( AdventureResult.ADV ) );
		KoLConstants.tally.add( new AdventureResult( AdventureResult.MEAT ) );
		KoLConstants.tally.add( AdventureResult.SESSION_SUBSTATS_RESULT );
		KoLConstants.tally.add( AdventureResult.SESSION_FULLSTATS_RESULT );
	}

	/**
	 * Utility. The method to parse an individual adventuring result. This method determines what the result actually
	 * was and adds it to the tally.
	 *
	 * @param result String to parse for the result
	 */

	public AdventureResult parseResult( final String result )
	{
		String trimResult = result.trim();
		RequestLogger.updateDebugLog( "Parsing result: " + trimResult );

		try
		{
			return AdventureResult.parseResult( trimResult );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	private AdventureResult parseItem( final String result )
	{
		RequestLogger.updateDebugLog( "Parsing item: " + result );

		// We do the following in order to not get confused by:
		//
		// Frobozz Real-Estate Company Instant House (TM)
		// stone tablet (Sinister Strumming)
		// stone tablet (Squeezings of Woe)
		// stone tablet (Really Evil Rhythm)
		//
		// which otherwise cause an exception and a stack trace

		// Look for a verbatim match
		int itemId = TradeableItemDatabase.getItemId( result.trim() );
		if ( itemId > 0 )
		{
			return new AdventureResult( itemId, 1 );
		}

		// Remove parenthesized number and match again.
		String name = result;
		int count = 1;

		int index = result.lastIndexOf( " (" );
		if ( index != -1 )
		{
			name = result.substring( 0, index );
			count = StaticEntity.parseInt( result.substring( index ) );
		}

		return new AdventureResult( name, count, false );
	}

	private boolean parseEffect( final String result )
	{
		RequestLogger.updateDebugLog( "Parsing effect: " + result );

		StringTokenizer parsedEffect = new StringTokenizer( result, "()" );
		String parsedEffectName = parsedEffect.nextToken().trim();
		String parsedDuration = parsedEffect.hasMoreTokens() ? parsedEffect.nextToken() : "1";

		return this.processResult( new AdventureResult( parsedEffectName, StaticEntity.parseInt( parsedDuration ), true ) );
	}

	/**
	 * Utility. The method used to process a result. By default, this method will also add an adventure result to the
	 * tally directly. This is used whenever the nature of the result is already known and no additional parsing is
	 * needed.
	 *
	 * @param result Result to add to the running tally of adventure results
	 */

	public boolean processResult( AdventureResult result )
	{
		// This should not happen, but check just in case and
		// return if the result was null.

		if ( result == null )
		{
			return false;
		}

		RequestLogger.updateDebugLog( "Processing result: " + result );

		String resultName = result.getName();
		boolean shouldRefresh = false;

		// Process the adventure result in this section; if
		// it's a status effect, then add it to the recent
		// effect list.  Otherwise, add it to the tally.

		if ( result.isStatusEffect() )
		{
			shouldRefresh |= !KoLConstants.activeEffects.contains( result );
			AdventureResult.addResultToList( KoLConstants.recentEffects, result );
		}
		else if ( resultName.equals( AdventureResult.ADV ) && result.getCount() < 0 )
		{
			StaticEntity.saveCounters();
			AdventureResult.addResultToList( KoLConstants.tally, result.getNegation() );
		}
		else if ( result.isItem() )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
		else if ( resultName.equals( AdventureResult.SUBSTATS ) )
		{
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
		else if ( resultName.equals( AdventureResult.MEAT ) )
		{
			KoLAdventure location = KoLAdventure.lastVisitedLocation();
			if ( location != null && location.getAdventureId().equals( "126" ) && FightRequest.getCurrentRound() == 0 )
			{
				BigIsland.addNunneryMeat( result );
				return false;
			}

			AdventureResult.addResultToList( KoLConstants.tally, result );
		}

		KoLCharacter.processResult( result );

		if ( result.isItem() )
		{
			// Do special processing when you get certain items
			this.gainItem( result );
		}

		shouldRefresh |= result.getName().equals( AdventureResult.MEAT );

		// Process the adventure result through the conditions
		// list, removing it if the condition is satisfied.

		if ( result.isItem() && HermitRequest.isWorthlessItem( result.getItemId() ) )
		{
			result = HermitRequest.WORTHLESS_ITEM.getInstance( result.getCount() );
		}

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		if ( resultName.equals( AdventureResult.SUBSTATS ) && KoLConstants.tally.size() >= 3 )
		{
			int currentTest =
				KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() ) - KoLmafia.initialStats[ 0 ];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[ 0 ] != currentTest;
			AdventureResult.SESSION_FULLSTATS[ 0 ] = currentTest;

			currentTest =
				KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() ) - KoLmafia.initialStats[ 1 ];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[ 1 ] != currentTest;
			AdventureResult.SESSION_FULLSTATS[ 1 ] = currentTest;

			currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() ) - KoLmafia.initialStats[ 2 ];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[ 2 ] != currentTest;
			AdventureResult.SESSION_FULLSTATS[ 2 ] = currentTest;

			if ( KoLConstants.tally.size() > 3 )
			{
				KoLConstants.tally.fireContentsChanged( KoLConstants.tally, 3, 3 );
			}
		}

		int conditionIndex = KoLConstants.conditions.indexOf( result );

		if ( conditionIndex != -1 )
		{
			if ( resultName.equals( AdventureResult.SUBSTATS ) )
			{
				// If the condition is a substat condition,
				// then zero out the appropriate count, if
				// applicable, and remove the substat condition
				// if the overall count dropped to zero.

				for ( int i = 0; i < 3; ++i )
				{
					if ( AdventureResult.CONDITION_SUBSTATS[ i ] == 0 )
					{
						continue;
					}

					AdventureResult.CONDITION_SUBSTATS[ i ] =
						Math.max( 0, AdventureResult.CONDITION_SUBSTATS[ i ] - result.getCount( i ) );
				}

				if ( AdventureResult.CONDITION_SUBSTATS_RESULT.getCount() == 0 )
				{
					KoLConstants.conditions.remove( conditionIndex );
				}
				else
				{
					KoLConstants.conditions.fireContentsChanged(
						KoLConstants.conditions, conditionIndex, conditionIndex );
				}
			}
			else
			{
				// Otherwise, this was a partial satisfaction
				// of a condition.  Decrement the count by the
				// negation of this result.

				AdventureResult condition = (AdventureResult) KoLConstants.conditions.get( conditionIndex );
				condition = condition.getInstance( condition.getCount() - result.getCount() );

				if ( condition.getCount() <= 0 )
				{
					KoLConstants.conditions.remove( conditionIndex );
				}
				else
				{
					KoLConstants.conditions.set( conditionIndex, condition );
				}
			}
		}

		return shouldRefresh;
	}

	private void gainItem( AdventureResult result )
	{
		switch ( result.getItemId() )
		{
		case KoLmafia.LUCRE:
			CoinmastersFrame.externalUpdate();
			break;

		case KoLmafia.SOCK:
			// If you get a S.O.C.K., you lose all the Immateria
			if ( result.getCount() == 1 )
			{
				for ( int i = 0; i < KoLAdventure.IMMATERIA.length; ++i )
				{
					this.processResult( KoLAdventure.IMMATERIA[ i ] );
				}
			}
			break;

		case KoLmafia.LASAGNA:
		case KoLmafia.MARGARITA:
		case KoLmafia.AIR_FRESHENER:
			// When you get a steel item, you lose Azazel's items
			if ( result.getCount() == 1 )
			{
				for ( int i = 0; i < KoLAdventure.AZAZEL.length; ++i )
				{
					this.processResult( KoLAdventure.AZAZEL[ i ] );
				}
			}
			break;
		
		/* These update the session results for the item swapping in the
		 * Gnome's Going Postal quest.
		 */

		case KoLmafia.REALLY_BIG_TINY_HOUSE:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.RED_PAPER_CLIP );
			}
			break;
		case KoLmafia.NONESSENTIAL_AMULET:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.REALLY_BIG_TINY_HOUSE );
			}
			break;
				
		case KoLmafia.WHITE_WINE_VINIGARETTE:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.NONESSENTIAL_AMULET );
			}
			break;
		case KoLmafia.CURIOUSLY_SHINY_AX:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.WHITE_WINE_VINIGARETTE );
			}
			break;
		case KoLmafia.CUP_OF_STRONG_TEA:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.CURIOUSLY_SHINY_AX );
			}
			break;
		case KoLmafia.MARINATED_STAKES:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.CUP_OF_STRONG_TEA );
			}
			break;
		case KoLmafia.KNOB_BUTTER:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.MARINATED_STAKES );
			}
			break;
		case KoLmafia.VIAL_OF_ECTOPLASM:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.KNOB_BUTTER );
			}
			break;
		case KoLmafia.BOOCK_OF_MAGIKS:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.VIAL_OF_ECTOPLASM );
			}
			break;
		case KoLmafia.EZ_PLAY_HARMONICA_BOOK:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.BOOCK_OF_MAGIKS );
			}
			break;
		case KoLmafia.FINGERLESS_HOBO_GLOVES:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.EZ_PLAY_HARMONICA_BOOK );
			}
			break;
		case KoLmafia.CHOMSKYS_COMICS:
			if ( result.getCount() == 1 )
			{
				this.processResult( KoLAdventure.FINGERLESS_HOBO_GLOVES );
			}
			break;
			
		case KoLmafia.GNOME_DEMODULIZER:
			if ( result.getCount() == 1 && KoLConstants.inventory.contains( KoLAdventure.CHOMSKYS_COMICS ) )
			{
				this.processResult( KoLAdventure.CHOMSKYS_COMICS );
			}
			break;
		}
	}

	public boolean isSemirare( AdventureResult result )
	{
		switch ( result.getItemId() )
		{
		case ASCII_SHIRT:
		case RHINO_HORMONES:
		case MAGIC_SCROLL:
		case PIRATE_JUICE:
		case PET_SNACKS:
		case INHALER:
		case CYCLOPS_EYEDROPS:
		case SPINACH:
		case FIRE_FLOWER:
		case ICE_CUBE:
		case FAKE_BLOOD:
		case GUANEAU:
		case LARD:
		case MYTIC_SHELL:
		case LIP_BALM:
		case ANTIFREEZE:
		case BLACK_EYEDROPS:
		case DOGSGOTNONOZ:
		case FLIPBOOK:
		case NEW_CLOACA_COLA:
		case MASSAGE_OIL:
		case POLTERGEIST:
		case TASTY_TART:
		case LUNCHBOX:
		case KNOB_PASTY:
		case KNOB_COFFEE:
                        return true;
		}
                return false;
	}

	/**
	 * Adds the recent effects accumulated so far to the actual effects. This should be called after the previous
	 * effects were decremented, if adventuring took place.
	 */

	public static final void applyEffects()
	{
		int oldCount = KoLConstants.activeEffects.size();

		for ( int j = 0; j < KoLConstants.recentEffects.size(); ++j )
		{
			AdventureResult.addResultToList(
				KoLConstants.activeEffects, (AdventureResult) KoLConstants.recentEffects.get( j ) );
		}

		KoLConstants.recentEffects.clear();
		KoLConstants.activeEffects.sort();

		if ( oldCount != KoLConstants.activeEffects.size() )
		{
			KoLCharacter.updateStatus();
		}
	}

	/**
	 * Returns the string form of the player Id associated with the given player name.
	 *
	 * @param playerId The Id of the player
	 * @return The player's name if it has been seen, or null if it has not yet appeared in the chat (not likely, but
	 *         possible).
	 */

	public static final String getPlayerName( final String playerId )
	{
		if ( playerId == null )
		{
			return null;
		}

		String playerName = (String) KoLConstants.seenPlayerNames.get( playerId );
		return playerName != null ? playerName : playerId;
	}

	/**
	 * Returns the string form of the player Id associated with the given player name.
	 *
	 * @param playerName The name of the player
	 * @return The player's Id if the player has been seen, or the player's name with spaces replaced with underscores
	 *         and other elements encoded if the player's Id has not been seen.
	 */

	public static final String getPlayerId( final String playerName )
	{
		if ( playerName == null )
		{
			return null;
		}

		String playerId = (String) KoLConstants.seenPlayerIds.get( playerName.toLowerCase() );
		return playerId != null ? playerId : playerName;
	}

	/**
	 * Registers the given player name and player Id with KoLmafia's player name tracker.
	 *
	 * @param playerName The name of the player
	 * @param playerId The player Id associated with this player
	 */

	public static final void registerPlayer( String playerName, final String playerId )
	{
		playerName = playerName.replaceAll( "[^0-9A-Za-z_ ]", "" );
		String lowercase = playerName.toLowerCase();

		if ( lowercase.equals( "modwarning" ) || KoLConstants.seenPlayerIds.containsKey( lowercase ) )
		{
			return;
		}

		KoLConstants.seenPlayerIds.put( lowercase, playerId );
		KoLConstants.seenPlayerNames.put( playerId, playerName );
	}

	public static final void registerContact( String playerName, final String playerId )
	{
		playerName = playerName.toLowerCase().replaceAll( "[^0-9A-Za-z_ ]", "" );
		KoLmafia.registerPlayer( playerName, playerId );
		if ( !KoLConstants.contactList.contains( playerName ) )
		{
			KoLConstants.contactList.add( playerName.toLowerCase() );
		}
	}

	/**
	 * Returns whether or not the current user has a ten-leaf clover.
	 *
	 * @return <code>true</code>
	 */

	public boolean isLuckyCharacter()
	{
		return KoLConstants.inventory.contains( SewerRequest.CLOVER );
	}

	/**
	 * Utility. The method which ensures that the amount needed exists, and if not, calls the appropriate scripts to do
	 * so.
	 */

	private final boolean recover( float desired, final String settingName, final String currentName,
		final String maximumName, final Object[] techniques )
		throws Exception
	{
		// First, check for beaten up, if the person has tongue as an
		// auto-heal option.  This takes precedence over all other checks.

		String restoreSetting = KoLSettings.getUserProperty( settingName + "Items" ).trim().toLowerCase();

		// Next, check against the restore needed to see if
		// any restoration needs to take place.

		Object[] empty = new Object[ 0 ];
		Method currentMethod, maximumMethod;

		currentMethod = KoLCharacter.class.getMethod( currentName, new Class[ 0 ] );
		maximumMethod = KoLCharacter.class.getMethod( maximumName, new Class[ 0 ] );

		float setting = KoLSettings.getFloatProperty( settingName );

		if ( setting < 0.0f && desired == 0 )
		{
			return true;
		}

		float current = ( (Number) currentMethod.invoke( null, empty ) ).floatValue();

		// If you've already reached the desired value, don't
		// bother restoring.

		if ( desired != 0 && current >= desired )
		{
			return true;
		}

		float maximum = ( (Number) maximumMethod.invoke( null, empty ) ).floatValue();
		float needed = Math.min( maximum, Math.max( desired, setting * maximum + 1.0f ) );

		if ( current >= needed )
		{
			return true;
		}

		// Next, check against the restore target to see how
		// far you need to go.

		setting = KoLSettings.getFloatProperty( settingName + "Target" );
		desired = Math.min( maximum, Math.max( desired, setting * maximum ) );

		if ( BuffBotHome.isBuffBotActive() || desired > maximum )
		{
			desired = maximum;
		}

		// If it gets this far, then you should attempt to recover
		// using the selected items.  This involves a few extra
		// reflection methods.

		String currentTechniqueName;

		// Determine all applicable items and skills for the restoration.
		// This is a little bit memory floatensive, but it allows for a lot
		// more flexibility.

		ArrayList possibleItems = new ArrayList();
		ArrayList possibleSkills = new ArrayList();

		for ( int i = 0; i < techniques.length; ++i )
		{
			currentTechniqueName = techniques[ i ].toString().toLowerCase();
			if ( restoreSetting.indexOf( currentTechniqueName ) == -1 )
			{
				continue;
			}

			if ( techniques[ i ] instanceof HPRestoreItem )
			{
				if ( ( (HPRestoreItem) techniques[ i ] ).isSkill() )
				{
					possibleSkills.add( techniques[ i ] );
				}
				else
				{
					possibleItems.add( techniques[ i ] );
				}
			}

			if ( techniques[ i ] instanceof MPRestoreItem )
			{
				if ( ( (MPRestoreItem) techniques[ i ] ).isSkill() )
				{
					possibleSkills.add( techniques[ i ] );
				}
				else
				{
					possibleItems.add( techniques[ i ] );
				}
			}
		}

		float last = -1;

		// Special handling of the Hidden Temple.  Here, as
		// long as your health is above zero, you're okay.

		boolean isNonCombatHealthRestore =
			settingName.startsWith( "hp" ) && KoLmafia.isAdventuring() && KoLmafia.currentAdventure.isNonCombatsOnly();

		if ( isNonCombatHealthRestore )
		{
			if ( KoLCharacter.getCurrentHP() > 0 )
			{
				return true;
			}

			needed = 1;
			desired = 1;
		}

		// Consider clearing beaten up if your restoration settings
		// include the appropriate items.

		if ( current >= needed )
		{
			return true;
		}

		for ( int i = 0; i < possibleSkills.size(); ++i )
		{
			if ( possibleSkills.get( i ) instanceof HPRestoreItem )
			{
				( (HPRestoreItem) possibleSkills.get( i ) ).updateHealthPerUse();
			}
			else
			{
				( (MPRestoreItem) possibleSkills.get( i ) ).updateManaPerUse();
			}
		}

		for ( int i = 0; i < possibleItems.size(); ++i )
		{
			if ( possibleItems.get( i ) instanceof HPRestoreItem )
			{
				( (HPRestoreItem) possibleItems.get( i ) ).updateHealthPerUse();
			}
			else
			{
				( (MPRestoreItem) possibleItems.get( i ) ).updateManaPerUse();
			}
		}

		HPRestoreItemList.setPurchaseBasedSort( false );
		MPRestoreItemList.setPurchaseBasedSort( false );

		// Next, use any available skills.  This only applies to health
		// restoration, since no MP-using skill restores MP.

		if ( !possibleSkills.isEmpty() )
		{
			current = ( (Number) currentMethod.invoke( null, empty ) ).floatValue();

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleSkills );

				do
				{
					last = current;
					currentTechniqueName = possibleSkills.get( indexToTry ).toString().toLowerCase();

					this.recoverOnce( possibleSkills.get( indexToTry ), currentTechniqueName, (int) desired, false );
					current = ( (Number) currentMethod.invoke( null, empty ) ).floatValue();

					maximum = ( (Number) maximumMethod.invoke( null, empty ) ).floatValue();
					desired = Math.min( maximum, desired );
					needed = Math.min( maximum, needed );

					if ( last == current )
					{
						++indexToTry;
					}
				}
				while ( indexToTry < possibleSkills.size() && current < needed );
			}

			if ( KoLmafia.refusesContinue() )
			{
				return false;
			}
		}

		// Iterate through every restore item which is already available
		// in the player's inventory.

		Collections.sort( possibleItems );

		for ( int i = 0; i < possibleItems.size() && current < needed; ++i )
		{
			do
			{
				last = current;
				currentTechniqueName = possibleItems.get( i ).toString().toLowerCase();

				this.recoverOnce( possibleItems.get( i ), currentTechniqueName, (int) desired, false );
				current = ( (Number) currentMethod.invoke( null, empty ) ).floatValue();

				maximum = ( (Number) maximumMethod.invoke( null, empty ) ).floatValue();
				desired = Math.min( maximum, desired );
				needed = Math.min( maximum, needed );
			}
			while ( last != current && current < needed );
		}

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		// For areas that are all noncombats, then you can go ahead
		// and heal using only unguent.

		if ( isNonCombatHealthRestore && KoLCharacter.getAvailableMeat() >= 30 )
		{
			RequestThread.postRequest( new ConsumeItemRequest( new AdventureResult( 231, 1 ) ) );
			return true;
		}

		// If things are still not restored, try looking for items you
		// don't have but can purchase.

		if ( !possibleItems.isEmpty() )
		{
			HPRestoreItemList.setPurchaseBasedSort( true );
			MPRestoreItemList.setPurchaseBasedSort( true );

			current = ( (Number) currentMethod.invoke( null, empty ) ).floatValue();
			last = -1;

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleItems );

				do
				{
					last = current;
					currentTechniqueName = possibleItems.get( indexToTry ).toString().toLowerCase();

					this.recoverOnce( possibleItems.get( indexToTry ), currentTechniqueName, (int) desired, true );
					current = ( (Number) currentMethod.invoke( null, empty ) ).floatValue();

					maximum = ( (Number) maximumMethod.invoke( null, empty ) ).floatValue();
					desired = Math.min( maximum, desired );

					if ( last == current )
					{
						++indexToTry;
					}
				}
				while ( indexToTry < possibleItems.size() && current < needed );
			}

			HPRestoreItemList.setPurchaseBasedSort( false );
			MPRestoreItemList.setPurchaseBasedSort( false );
		}
		else if ( current < needed )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You ran out of restores." );
			return false;
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( KoLmafia.refusesContinue() )
		{
			return false;
		}

		if ( current < needed )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Autorecovery failed." );
			return false;
		}

		return true;
	}

	/**
	 * Utility. The method called in between battles. This method checks to see if the character's HP has dropped below
	 * the tolerance value, and recovers if it has (if the user has specified this in their settings).
	 */

	public final boolean recoverHP()
	{
		return this.recoverHP( 0 );
	}

	public final boolean recoverHP( final int recover )
	{
		try
		{
			if ( KoLSettings.getBooleanProperty( "removeMalignantEffects" ) )
			{
				MoodSettings.removeMalignantEffects();
			}

			return this.recover(
				recover, "hpAutoRecovery", "getCurrentHP", "getMaximumHP", HPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Utility. The method which uses the given recovery technique (not specified in a script) in order to restore.
	 */

	private final void recoverOnce( final Object technique, final String techniqueName, final int needed,
		final boolean purchase )
	{
		// If the technique is an item, and the item is not readily available,
		// then don't bother with this item -- however, if it is the only item
		// present, then rethink it.

		if ( technique instanceof HPRestoreItem )
		{
			( (HPRestoreItem) technique ).recoverHP( needed, purchase );
		}

		if ( technique instanceof MPRestoreItem )
		{
			( (MPRestoreItem) technique ).recoverMP( needed, purchase );
		}
	}

	/**
	 * Returns the total number of mana restores currently available to the player.
	 */

	public static final int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = KoLSettings.getUserProperty( "mpAutoRecoveryItems" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
		{
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) != -1 )
			{
				restoreCount += MPRestoreItemList.CONFIGURES[ i ].getItem().getCount( KoLConstants.inventory );
			}
		}

		return restoreCount;
	}

	/**
	 * Utility. The method called in between commands. This method checks to see if the character's MP has dropped below
	 * the tolerance value, and recovers if it has (if the user has specified this in their settings).
	 */

	public final boolean recoverMP()
	{
		return this.recoverMP( 0 );
	}

	/**
	 * Utility. The method which restores the character's current mana points above the given value.
	 */

	public final boolean recoverMP( final int mpNeeded )
	{
		try
		{
			return this.recover(
				mpNeeded, "mpAutoRecovery", "getCurrentMP", "getMaximumMP", MPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	/**
	 * Utility. The method used to process the results of any adventure in the Kingdom of Loathing This method searches
	 * for items, stat gains, and losses within the provided string.
	 *
	 * @param results The string containing the results of the adventure
	 * @return <code>true</code> if any results existed
	 */

	public final boolean processResults( final String results )
	{
		return this.processResults( results, null );
	}

	public final boolean processResults( final String results, final ArrayList data )
	{
		if ( data == null )
		{
			RequestLogger.updateDebugLog( "Processing results..." );
		}

		if ( data == null && results.indexOf( "gains a pound" ) != -1 )
		{
			KoLCharacter.incrementFamilarWeight();

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "familiar " + KoLCharacter.getFamiliar() );
			RequestLogger.updateSessionLog();
		}

		String plainTextResult = results.replaceAll( "<.*?>", KoLConstants.LINE_BREAK );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, KoLConstants.LINE_BREAK );
		String lastToken = null;

		Matcher damageMatcher = null;
		AdventureResult lastResult;

		if ( data == null && KoLCharacter.isUsingStabBat() )
		{
			damageMatcher = KoLmafia.STABBAT_PATTERN.matcher( plainTextResult );

			if ( damageMatcher.find() )
			{
				String message = "You lose " + damageMatcher.group( 1 ) + " hit points";

				RequestLogger.printLine( message );

				if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
				{
					RequestLogger.updateSessionLog( message );
				}

				this.parseResult( message );
			}

			damageMatcher = KoLmafia.CARBS_PATTERN.matcher( plainTextResult );

			if ( damageMatcher.find() )
			{
				String message = "You lose " + damageMatcher.group( 1 ) + " hit points";

				RequestLogger.printLine( message );

				if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
				{
					RequestLogger.updateSessionLog( message );
				}

				this.parseResult( message );
			}
		}

		if ( data == null )
		{
			damageMatcher = KoLmafia.FUMBLE_PATTERN.matcher( plainTextResult );

			while ( damageMatcher.find() )
			{
				String message = "You lose " + damageMatcher.group( 1 ) + " hit points";

				RequestLogger.printLine( message );

				if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
				{
					RequestLogger.updateSessionLog( message );
				}

				this.parseResult( message );
			}
		}

		boolean requiresRefresh = false;

		while ( parsedResults.hasMoreTokens() )
		{
			lastToken = parsedResults.nextToken();

			// Skip effect acquisition - it's followed by a boldface
			// which makes the parser think it's found an item.

			if ( lastToken.indexOf( "You acquire a skill" ) != -1 || lastToken.indexOf( "You gain a skill" ) != -1 )
			{
				continue;
			}

			String acquisition = lastToken.trim();
			// The following only under Can Has Cyborger. Sigh.
			if ( acquisition.startsWith( "O hai, I made dis" ) )
			{
				acquisition = "You acquire an item:";
			}

			if ( acquisition.startsWith( "You acquire" ) )
			{
				if ( acquisition.indexOf( "effect" ) == -1 )
				{
					String item = parsedResults.nextToken();

					if ( acquisition.indexOf( "an item" ) != -1 )
					{
						if ( data == null )
						{
							RequestLogger.printLine( acquisition + " " + item );
							if ( KoLSettings.getBooleanProperty( "logAcquiredItems" ) )
							{
								RequestLogger.updateSessionLog( acquisition + " " + item );
							}
						}

						lastResult = this.parseItem( item );
						if ( data == null )
						{
							this.processResult( lastResult );
						}
						else
						{
							AdventureResult.addResultToList( data, lastResult );
						}
					}
					else
					{
						// The name of the item follows the number
						// that appears after the first index.

						String countString = item.split( " " )[ 0 ];
						int spaceIndex = item.indexOf( " " );

						String itemName = spaceIndex == -1 ? item : item.substring( spaceIndex ).trim();
						boolean isNumeric = spaceIndex != -1;

						for ( int i = 0; isNumeric && i < countString.length(); ++i )
						{
							isNumeric &= Character.isDigit( countString.charAt( i ) ) || countString.charAt( i ) == ',';
						}

						if ( !isNumeric )
						{
							countString = "1";
						}
						else if ( itemName.equals( "evil golden arches" ) )
						{
							itemName = "evil golden arch";
						}

						RequestLogger.printLine( acquisition + " " + item );

						if ( KoLSettings.getBooleanProperty( "logAcquiredItems" ) )
						{
							RequestLogger.updateSessionLog( acquisition + " " + item );
						}

						lastResult = this.parseItem( itemName + " (" + countString + ")" );
						if ( data == null )
						{
							this.processResult( lastResult );
						}
						else
						{
							AdventureResult.addResultToList( data, lastResult );
						}
					}
				}
				else if ( data == null )
				{
					String effectName = parsedResults.nextToken();
					lastToken = parsedResults.nextToken();

					RequestLogger.printLine( acquisition + " " + effectName + " " + lastToken );

					if ( KoLSettings.getBooleanProperty( "logStatusEffects" ) )
					{
						RequestLogger.updateSessionLog( acquisition + " " + effectName + " " + lastToken );
					}

					if ( lastToken.indexOf( "duration" ) == -1 )
					{
						this.parseEffect( effectName );
					}
					else
					{
						String duration = lastToken.substring( 11, lastToken.length() - 11 ).trim();
						requiresRefresh |= this.parseEffect( effectName + " (" + duration + ")" );
					}
				}
				continue;
			}

			// The following only under Can Has Cyborger
			if ( lastToken.startsWith( "You gets" ) )
			{
				lastToken = "You gain" + lastToken.substring( 8 );
			}
			else if ( lastToken.startsWith( "You can has" ) )
			{
				lastToken = "You gain" + lastToken.substring( 11 );
			}

			if ( lastToken.startsWith( "You gain" ) || lastToken.startsWith( "You lose " ) )
			{
				int periodIndex = lastToken.indexOf( "." );
				if ( periodIndex != -1 )
				{
					lastToken = lastToken.substring( 0, periodIndex );
				}

				int parenIndex = lastToken.indexOf( "(" );
				if ( parenIndex != -1 )
				{
					lastToken = lastToken.substring( 0, parenIndex );
				}

				lastToken = lastToken.trim();

				if ( data == null && lastToken.indexOf( "level" ) == -1 )
				{
					RequestLogger.printLine( lastToken );
				}

				// Because of the simplified parsing, there's a chance that
				// the "gain" acquired wasn't a subpoint (in other words, it
				// includes the word "a" or "some"), which causes a NFE or
				// possibly a ParseException to be thrown.  catch them and
				// do nothing (eventhough it's technically bad style).

				if ( lastToken.startsWith( "You gain a" ) || lastToken.startsWith( "You gain some" ) )
				{
					requiresRefresh = true;
				}
				else
				{
					lastResult = this.parseResult( lastToken );
					if ( data == null )
					{
						this.processResult( lastResult );
						if ( lastResult.getName().equals( AdventureResult.SUBSTATS ) )
						{
							if ( KoLSettings.getBooleanProperty( "logStatGains" ) )
							{
								RequestLogger.updateSessionLog( lastToken );
							}
						}
						else if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
						{
							RequestLogger.updateSessionLog( lastToken );
						}

					}
					else if ( lastResult.getName().equals( AdventureResult.MEAT ) )
					{
						AdventureResult.addResultToList( data, lastResult );
						if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
						{
							RequestLogger.updateSessionLog( lastToken );
						}
					}
				}
				continue;
			}

			if ( lastToken.startsWith( "You discard" ) )
			{
				Matcher matcher = KoLmafia.DISCARD_PATTERN.matcher( lastToken );
				if ( matcher.find() )
				{
					AdventureResult item = new AdventureResult( matcher.group( 1 ), -1, false );
					AdventureResult.addResultToList( KoLConstants.inventory, item );
					AdventureResult.addResultToList( KoLConstants.tally, item );
				}
			}
		}

		KoLmafia.applyEffects();
		return requiresRefresh;
	}

	public void makeRequest( final Runnable request )
	{
		this.makeRequest( request, 1 );
	}

	/**
	 * Makes the given request for the given number of iterations, or until continues are no longer possible, either
	 * through user cancellation or something occuring which prevents the requests from resuming.
	 *
	 * @param request The request made by the user
	 * @param iterations The number of times the request should be repeated
	 */

	public void makeRequest( final Runnable request, final int iterations )
	{
		try
		{
			// Before anything happens, make sure that you are in
			// in a valid continuation state.

			boolean wasAdventuring = KoLmafia.isAdventuring;

			// Handle the gym, which is the only adventure type
			// which needs to be specially handled.

			if ( request instanceof KoLAdventure )
			{
				KoLmafia.currentAdventure = (KoLAdventure) request;

				if ( KoLmafia.currentAdventure.getRequest() instanceof ClanGymRequest )
				{
					RequestThread.postRequest( ( (ClanGymRequest) KoLmafia.currentAdventure.getRequest() ).setTurnCount( iterations ) );
					return;
				}

				if ( !( KoLmafia.currentAdventure.getRequest() instanceof CampgroundRequest ) && KoLCharacter.getCurrentHP() == 0 )
				{
					this.recoverHP();
				}

				if ( !KoLmafia.permitsContinue() )
				{
					return;
				}

				KoLmafia.isAdventuring = true;
				SpecialOutfit.createImplicitCheckpoint();
			}

			// Execute the request as initially intended by calling
			// a subroutine.  In doing so, make sure your HP/MP restore
			// settings are scaled back down to current levels, if they've
			// been manipulated internally by

			RequestThread.openRequestSequence();
			this.executeRequest( request, iterations, wasAdventuring );
			RequestThread.closeRequestSequence();

			if ( request instanceof KoLAdventure && !wasAdventuring )
			{
				KoLmafia.isAdventuring = false;
				this.runBetweenBattleChecks( false );
				SpecialOutfit.restoreImplicitCheckpoint();
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private boolean handleConditions( final AdventureResult[] items, final ItemCreationRequest[] creatables )
	{
		if ( items.length == 0 )
		{
			return false;
		}

		if ( KoLConstants.conditions.isEmpty() )
		{
			return true;
		}

		boolean shouldCreate = false;

		for ( int i = 0; i < creatables.length && !shouldCreate; ++i )
		{
			shouldCreate |= creatables[ i ] != null && creatables[ i ].getQuantityPossible() >= items[ i ].getCount();
		}

		// In theory, you could do a real validation by doing a full
		// dependency search.  While that's technically better, it's
		// also not very useful.

		for ( int i = 0; i < creatables.length && shouldCreate; ++i )
		{
			shouldCreate &= creatables[ i ] == null || creatables[ i ].getQuantityPossible() >= items[ i ].getCount();
		}

		// Create any items which are creatable.

		if ( shouldCreate )
		{
			for ( int i = creatables.length - 1; i >= 0; --i )
			{
				if ( creatables[ i ] != null && creatables[ i ].getQuantityPossible() >= items[ i ].getCount() )
				{
					creatables[ i ].setQuantityNeeded( items[ i ].getCount() );
					RequestThread.postRequest( creatables[ i ] );
					creatables[ i ] = null;
				}
			}
		}

		// If the conditions existed and have been satisfied,
		// then you should stop.

		return KoLConstants.conditions.isEmpty();
	}

	private void executeRequest( final Runnable request, final int totalIterations, final boolean wasAdventuring )
	{
		KoLmafia.hadPendingState = false;

		// Begin the adventuring process, or the request execution
		// process (whichever is applicable).

		boolean isAdventure = request instanceof KoLAdventure;
		AdventureResult[] items = new AdventureResult[ KoLConstants.conditions.size() ];
		ItemCreationRequest[] creatables = new ItemCreationRequest[ KoLConstants.conditions.size() ];

		for ( int i = 0; i < KoLConstants.conditions.size(); ++i )
		{
			items[ i ] = (AdventureResult) KoLConstants.conditions.get( i );
			creatables[ i ] = ItemCreationRequest.getInstance( items[ i ].getItemId() );
		}

		// Turn on auto-attack in order to save server hits if the
		// player isn't using custom combat.

		KoLmafia.forceContinue();

		int adventuresBeforeRequest;
		int currentIteration = 0;

		while ( KoLmafia.permitsContinue() && ++currentIteration <= totalIterations )
		{
			adventuresBeforeRequest = KoLCharacter.getAdventuresLeft();
			this.executeRequestOnce( request, wasAdventuring, currentIteration, totalIterations, items, creatables );

			if ( isAdventure && adventuresBeforeRequest == KoLCharacter.getAdventuresLeft() )
			{
				--currentIteration;
				KoLRequest.delay( 5000 );
			}
		}

		if ( isAdventure )
		{
			AdventureFrame.updateRequestMeter( 1, 1 );

			int bountyItem = KoLSettings.getIntegerProperty( "currentBountyItem" );
			if ( bountyItem != 0 && AdventureDatabase.getBountyLocation( bountyItem ) == request )
			{
				RequestThread.postRequest( new CoinmasterRequest( "lucre" ) );
			}
		}

		// If you've completed the requests, make sure to update
		// the display.

		if ( KoLmafia.permitsContinue() && !KoLmafia.isRunningBetweenBattleChecks() )
		{
			if ( isAdventure && !KoLConstants.conditions.isEmpty() )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE,
					"Conditions not satisfied after " + ( currentIteration - 1 ) + ( currentIteration == 2 ? " adventure." : " adventures." ) );
			}
		}
		else if ( KoLmafia.continuationState == KoLConstants.PENDING_STATE )
		{
			KoLmafia.hadPendingState = true;
			KoLmafia.forceContinue();
		}

		if ( isAdventure )
		{
			KoLRequest.printTotalDelay();
		}
	}

	private void executeAdventureOnce( final KoLAdventure adventure, boolean wasAdventuring,
		final int currentIteration, final int totalIterations, final AdventureResult[] items,
		final ItemCreationRequest[] creatables )
	{
		if ( KoLCharacter.getAdventuresLeft() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Ran out of adventures." );
			return;
		}

		if ( this.handleConditions( items, creatables ) )
		{
			KoLmafia.updateDisplay(
				KoLConstants.PENDING_STATE, "Conditions satisfied after " + currentIteration + " adventures." );
			return;
		}

		if ( KoLCharacter.isFallingDown() && KoLCharacter.getInebriety() <= 25 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You are too drunk to continue." );
			return;
		}

		if ( adventure.getRequest() instanceof SewerRequest )
		{
			if ( SewerRequest.GUM.getCount( KoLConstants.inventory ) == 0 )
			{
				int stopCount = 0;
				AdventureResult currentCondition;
				for ( int i = 0; i < KoLConstants.conditions.size(); ++i )
				{
					currentCondition = (AdventureResult) KoLConstants.conditions.get( i );
					if ( currentCondition.isItem() )
					{
						stopCount += currentCondition.getCount();
					}
				}

				int gumAmount = stopCount == 0 ? totalIterations : Math.min( stopCount, totalIterations );
				if ( !AdventureDatabase.retrieveItem( SewerRequest.GUM.getInstance( gumAmount ) ) )
				{
					return;
				}
			}
		}

		// Otherwise, disable the display and update the user
		// and the current request number.  Different requests
		// have different displays.  They are handled here.

		if ( totalIterations > 1 )
		{
			KoLmafia.currentIterationString =
				"Request " + currentIteration + " of " + totalIterations + " (" + adventure.toString() + ") in progress...";
		}
		else
		{
			KoLmafia.currentIterationString = "Visit to " + adventure.toString() + " in progress...";
		}

		if ( !wasAdventuring )
		{
			AdventureFrame.updateRequestMeter( currentIteration - 1, totalIterations );
		}

		RequestLogger.printLine();
		RequestThread.postRequest( adventure );
		RequestLogger.printLine();

		KoLmafia.currentIterationString = "";

		if ( this.handleConditions( items, creatables ) )
		{
			KoLmafia.updateDisplay(
				KoLConstants.PENDING_STATE, "Conditions satisfied after " + currentIteration + " adventures." );
			return;
		}
	}

	private void executeRequestOnce( final Runnable request, final boolean wasAdventuring, final int currentIteration,
		final int totalIterations, final AdventureResult[] items, final ItemCreationRequest[] creatables )
	{
		if ( request instanceof KoLAdventure )
		{
			this.executeAdventureOnce(
				(KoLAdventure) request, wasAdventuring, currentIteration, totalIterations, items, creatables );
			return;
		}

		if ( request instanceof CampgroundRequest )
		{
			KoLmafia.currentIterationString =
				"Canpground request " + currentIteration + " of " + totalIterations + " in progress...";
		}

		RequestLogger.printLine();
		RequestThread.postRequest( (KoLRequest) request );
		RequestLogger.printLine();
	}

	/**
	 * Makes a request which attempts to zap the chosen item
	 */

	public void makeZapRequest()
	{
		if ( KoLCharacter.getZapper() == null )
		{
			return;
		}

		AdventureResult selectedValue =
			(AdventureResult) KoLmafia.getSelectedValue( "Let's explodey my wand!", ZapRequest.getZappableItems() );
		if ( selectedValue == null )
		{
			return;
		}

		RequestThread.postRequest( new ZapRequest( selectedValue ) );
	}

	private static final Object getSelectedValue( final String message, final LockableListModel list )
	{
		return KoLFrame.input( message, list );
	}

	/**
	 * Makes a request to the hermit, looking for the given number of items. This method should prompt the user to
	 * determine which item to retrieve the hermit.
	 */

	public void makeHermitRequest()
	{
		if ( !KoLConstants.hermitItems.contains( SewerRequest.CLOVER ) )
		{
			RequestThread.postRequest( new HermitRequest() );
		}

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		AdventureResult selectedValue =
			(AdventureResult) KoLmafia.getSelectedValue( "I have worthless items!", KoLConstants.hermitItems );
		if ( selectedValue == null )
		{
			return;
		}

		int selected = selectedValue.getItemId();

		String message = "(You have " + HermitRequest.getWorthlessItemCount() + " worthless items)";
		int maximumValue = HermitRequest.getWorthlessItemCount();

		if ( selected == SewerRequest.CLOVER.getItemId() )
		{
			int cloverCount = ( (AdventureResult) selectedValue ).getCount();

			if ( cloverCount <= maximumValue )
			{
				message = "(There are " + cloverCount + " clovers still available)";
				maximumValue = cloverCount;
			}
		}

		int tradeCount =
			KoLFrame.getQuantity(
				"How many " + ( (AdventureResult) selectedValue ).getName() + " to get?\n" + message, maximumValue, 1 );
		if ( tradeCount == 0 )
		{
			return;
		}

		RequestThread.postRequest( new HermitRequest( selected, tradeCount ) );
	}

	/**
	 * Makes a request to the hermit, looking for the given number of items. This method should prompt the user to
	 * determine which item to retrieve the hermit.
	 */

	public void makeTrapperRequest()
	{
		AdventureResult selectedValue =
			(AdventureResult) KoLmafia.getSelectedValue( "I want skins!", KoLConstants.trapperItems );
		if ( selectedValue == null )
		{
			return;
		}

		int selected = selectedValue.getItemId();
		int maximumValue = CouncilFrame.YETI_FUR.getCount( KoLConstants.inventory );

		String message = "(You have " + maximumValue + " furs available)";
		int tradeCount =
			KoLFrame.getQuantity(
				"How many " + ( (AdventureResult) selectedValue ).getName() + " to get?\n" + message, maximumValue,
				maximumValue );
		if ( tradeCount == 0 )
		{
			return;
		}

		KoLmafia.updateDisplay( "Visiting the trapper..." );
		RequestThread.postRequest( new KoLRequest(
			"trapper.php?pwd&action=Yep.&whichitem=" + selected + "&qty=" + tradeCount ) );
	}

	/**
	 * Makes a request to the bounty hunter hunter, looking for the given
	 * number of items. This method should prompt the user to determine
	 * which item to retrieve the hermit.
	 */

	public void makeHunterRequest()
	{
		KoLRequest hunterRequest = new CoinmasterRequest( "lucre" );
		RequestThread.postRequest( hunterRequest );

		Matcher bountyMatcher = Pattern.compile( "name=whichitem value=(\\d+)" ).matcher( hunterRequest.responseText );

		LockableListModel bounties = new LockableListModel();
		while ( bountyMatcher.find() )
		{
			String item = TradeableItemDatabase.getItemName( StaticEntity.parseInt( bountyMatcher.group( 1 ) ) );
			if ( item == null )
			{
				continue;
			}

			KoLAdventure location = AdventureDatabase.getBountyLocation( item );
			if ( location == null )
			{
				continue;
			}

			bounties.add( item + " (" + location.getAdventureName() + ")" );
		}

		if ( bounties.isEmpty() )
		{
			int bounty = KoLSettings.getIntegerProperty( "currentBountyItem" );
			if ( bounty == 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're already on a bounty hunt." );
			}
			else
			{
				AdventureFrame.updateSelectedAdventure( AdventureDatabase.getBountyLocation( bounty ) );
			}

			return;
		}

		String selectedValue = (String) KoLmafia.getSelectedValue( "Time to collect bounties!", bounties );
		if ( selectedValue == null )
		{
			return;
		}

		String selection = selectedValue.substring( 0, selectedValue.lastIndexOf( "(" ) - 1 );
		int itemId = TradeableItemDatabase.getItemId( selection );
		RequestThread.postRequest( new CoinmasterRequest( "lucre", "takebounty", itemId ) );
	}

	/**
	 * Makes a request to the hunter, looking for the given number of items. This method should prompt the user to
	 * determine which item to retrieve the hunter.
	 */

	public void makeUntinkerRequest()
	{
		SortedListModel untinkerItems = new SortedListModel();

		for ( int i = 0; i < KoLConstants.inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) KoLConstants.inventory.get( i );
			int itemId = currentItem.getItemId();

			// Ignore silly fairy gravy + meat from yesterday recipe
			if ( itemId == KoLConstants.MEAT_STACK )
			{
				continue;
			}

			// Otherwise, accept any COMBINE recipe
			if ( ConcoctionsDatabase.getMixingMethod( itemId ) == KoLConstants.COMBINE )
			{
				untinkerItems.add( currentItem );
			}
		}

		if ( untinkerItems.isEmpty() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have any untinkerable items." );
			return;
		}

		AdventureResult selectedValue =
			(AdventureResult) KoLmafia.getSelectedValue( "You can unscrew meat paste?", untinkerItems );
		if ( selectedValue == null )
		{
			return;
		}

		RequestThread.postRequest( new UntinkerRequest( selectedValue.getItemId() ) );
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void makeMindControlRequest()
	{
		int maxLevel = 0;

		if ( KoLCharacter.inMysticalitySign() )
		{
			maxLevel = 11;
		}
		else if ( KoLCharacter.inMuscleSign() )
		{
			maxLevel = 10;
		}
		else if ( KoLCharacter.inMoxieSign() )
		{
			maxLevel = 10;
		}
		else
		{
			return;
		}

		String[] levelArray = new String[ maxLevel + 1 ];

		for ( int i = 0; i <= maxLevel; ++i )
		{
			levelArray[ i ] = "Level " + i;
		}

		int currentLevel = KoLCharacter.getSignedMLAdjustment();

		String selectedLevel =
			(String) KoLFrame.input( "Change monster annoyance from " + currentLevel + "?", levelArray );
		if ( selectedLevel == null )
		{
			return;
		}

		int setting = StaticEntity.parseInt( selectedLevel.split( " " )[ 1 ] );
		RequestThread.postRequest( new MindControlRequest( setting ) );
	}

	public void makeCampgroundRestRequest()
	{
		String turnCount = (String) KoLFrame.input( "Rest for how many turns?", "1" );
		if ( turnCount == null )
		{
			return;
		}

		this.makeRequest( new CampgroundRequest( "rest" ), StaticEntity.parseInt( turnCount ) );
	}

	public void makeCampgroundRelaxRequest()
	{
		String turnCount = (String) KoLFrame.input( "Relax for how many turns?", "1" );
		if ( turnCount == null )
		{
			return;
		}

		this.makeRequest( new CampgroundRequest( "relax" ), StaticEntity.parseInt( turnCount ) );
	}

	public void makeClanSofaRequest()
	{
		String turnCount = (String) KoLFrame.input( "Sleep for how many turns?", "1" );
		if ( turnCount == null )
		{
			return;
		}

		ClanGymRequest request = new ClanGymRequest( ClanGymRequest.SOFA );
		request.setTurnCount( StaticEntity.parseInt( turnCount ) );
		RequestThread.postRequest( request );
	}

	public static final void validateFaucetQuest()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastTavernAscension" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			KoLSettings.setUserProperty( "lastTavernSquare", "0" );
			KoLSettings.setUserProperty( "lastTavernAscension", String.valueOf( KoLCharacter.getAscensions() ) );
			KoLSettings.setUserProperty( "tavernLayout", "0000000000000000000000000" );
		}
	}

	public static final void addTavernLocation( final KoLRequest request )
	{
		KoLmafia.validateFaucetQuest();
		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getCurrentHP() == 0 )
		{
			return;
		}

		StringBuffer layout = new StringBuffer( KoLSettings.getUserProperty( "tavernLayout" ) );

		if ( request.getURLString().indexOf( "fight" ) != -1 )
		{
			int square = KoLSettings.getIntegerProperty( "lastTavernSquare" );
			if ( request.responseText != null )
			{
				layout.setCharAt( square - 1, request.responseText.indexOf( "Baron" ) != -1 ? '4' : '1' );
			}
		}
		else
		{
			String urlString = request.getURLString();
			if ( urlString.indexOf( "charpane" ) != -1 || urlString.indexOf( "chat" ) != -1 || urlString.equals( "rats.php" ) )
			{
				return;
			}

			Matcher squareMatcher = KoLmafia.TAVERN_PATTERN.matcher( urlString );
			if ( !squareMatcher.find() )
			{
				return;
			}

			// Handle fighting rats.  If this was done through
			// the mini-browser, you'll have response text; else,
			// the response text will be null.

			int square = StaticEntity.parseInt( squareMatcher.group( 1 ) );
			KoLSettings.setUserProperty( "lastTavernSquare", String.valueOf( square ) );

			char replacement = '1';
			if ( request.responseText != null && request.responseText.indexOf( "faucetoff" ) != -1 )
			{
				replacement = '3';
			}
			else if ( request.responseText != null && request.responseText.indexOf( "You acquire" ) != -1 )
			{
				replacement = '2';
			}

			layout.setCharAt( square - 1, replacement );
		}

		KoLSettings.setUserProperty( "tavernLayout", layout.toString() );
	}

	/**
	 * Completes the infamous tavern quest.
	 */

	public int locateTavernFaucet()
	{
		KoLmafia.validateFaucetQuest();

		// Determine which elements have already been checked
		// so you don't check through them again.

		ArrayList searchList = new ArrayList();
		Integer searchIndex = null;

		for ( int i = 1; i <= 25; ++i )
		{
			searchList.add( new Integer( i ) );
		}

		String visited = KoLSettings.getUserProperty( "tavernLayout" );
		for ( int i = visited.length() - 1; i >= 0; --i )
		{
			switch ( visited.charAt( i ) )
			{
			case '0':
				break;
			case '1':
			case '2':
				searchList.remove( i );
				break;

			case '3':
			{
				int faucetRow = (int) ( i / 5 ) + 1;
				int faucetColumn = i % 5 + 1;

				KoLmafia.updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
				return i + 1;
			}

			case '4':
			{
				int baronRow = (int) ( i / 5 ) + 1;
				int baronColumn = i % 5 + 1;

				KoLmafia.updateDisplay( "Baron found in row " + baronRow + ", column " + baronColumn );
				return i + 1;
			}
			}
		}

		// If the faucet has not yet been found, then go through
		// the process of trying to locate it.

		KoLAdventure adventure = new KoLAdventure( "Woods", "rats.php", "", "Typical Tavern (Pre-Rat)" );
		boolean foundFaucet = searchList.size() < 2;

		if ( KoLCharacter.getLevel() < 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need to level up first." );
			return -1;
		}

		RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );

		KoLmafia.updateDisplay( "Searching for faucet..." );
		RequestThread.postRequest( adventure );

		// Random guess instead of straightforward search
		// for the location of the faucet (lowers the chance
		// of bad results if the faucet is near the end).

		while ( KoLmafia.permitsContinue() && !foundFaucet && KoLCharacter.getCurrentHP() > 0 && KoLCharacter.getAdventuresLeft() > 0 )
		{
			searchIndex = (Integer) searchList.remove( KoLConstants.RNG.nextInt( searchList.size() ) );

			adventure.getRequest().addFormField( "where", searchIndex.toString() );
			RequestThread.postRequest( adventure );

			foundFaucet =
				adventure.getRequest().responseText != null && adventure.getRequest().responseText.indexOf( "faucetoff" ) != -1;
		}

		// If you have not yet found the faucet, be sure
		// to set the settings so that your next attempt
		// does not repeat located squares.

		if ( !foundFaucet )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to find faucet." );
			return -1;
		}

		// Otherwise, you've found it!  So notify the user
		// that the faucet has been found.

		int faucetRow = (int) ( ( searchIndex.intValue() - 1 ) / 5 ) + 1;
		int faucetColumn = ( searchIndex.intValue() - 1 ) % 5 + 1;

		KoLmafia.updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
		return searchIndex.intValue();
	}

	/**
	 * Trades items with the guardian of the goud.
	 */

	public void tradeGourdItems()
	{
		KoLRequest gourdVisit = new KoLRequest( "town_right.php?place=gourd" );

		KoLmafia.updateDisplay( "Determining items needed..." );
		RequestThread.postRequest( gourdVisit );

		// For every class, it's the same -- the message reads, "Bring back"
		// and then the number of the item needed.  Compare how many you need
		// with how many you have.

		Matcher neededMatcher = KoLmafia.GOURD_PATTERN.matcher( gourdVisit.responseText );
		AdventureResult item;

		switch ( KoLCharacter.getPrimeIndex() )
		{
		case 0:
			item = new AdventureResult( 747, 5 );
			break;
		case 1:
			item = new AdventureResult( 559, 5 );
			break;
		default:
			item = new AdventureResult( 27, 5 );
		}

		int neededCount = neededMatcher.find() ? StaticEntity.parseInt( neededMatcher.group( 1 ) ) : 26;
		gourdVisit.addFormField( "action=gourd" );

		while ( neededCount <= 25 && neededCount <= item.getCount( KoLConstants.inventory ) )
		{
			KoLmafia.updateDisplay( "Giving up " + neededCount + " " + item.getName() + "s..." );
			RequestThread.postRequest( gourdVisit );
			this.processResult( item.getInstance( 0 - neededCount++ ) );
		}

		int totalProvided = 0;
		for ( int i = 5; i < neededCount; ++i )
		{
			totalProvided += i;
		}

		KoLmafia.updateDisplay( "Gourd trading complete (" + totalProvided + " " + item.getName() + "s given so far)." );
	}

	public void unlockGuildStore()
	{
		this.unlockGuildStore( false );
	}

	public void unlockGuildStore( final boolean stopAtPaco )
	{
		KoLRequest guildVisit = new KoLRequest( "town_right.php?place=gourd" );

		// The wiki claims that your prime stats are somehow connected,
		// but the exact procedure is uncertain.  Therefore, just allow
		// the person to attempt to unlock their store, regardless of
		// their current stats.

		KoLmafia.updateDisplay( "Entering guild challenge area..." );
		RequestThread.postRequest( guildVisit.constructURLString( "guild.php?place=challenge" ) );

		boolean success =
			stopAtPaco ? guildVisit.responseText.indexOf( "paco" ) != -1 : guildVisit.responseText.indexOf( "store.php" ) != -1;

		guildVisit.constructURLString( "guild.php?action=chal" );
		KoLmafia.updateDisplay( "Completing guild tasks..." );

		for ( int i = 0; i < 6 && !success && KoLCharacter.getAdventuresLeft() > 0 && KoLmafia.permitsContinue(); ++i )
		{
			RequestThread.postRequest( guildVisit );

			if ( guildVisit.responseText != null )
			{
				success |=
					stopAtPaco ? guildVisit.responseText.indexOf( "paco" ) != -1 : guildVisit.responseText.indexOf( "You've already beaten" ) != -1;
			}
		}

		if ( success )
		{
			RequestThread.postRequest( guildVisit.constructURLString( "guild.php?place=paco" ) );
		}

		if ( success && stopAtPaco )
		{
			KoLmafia.updateDisplay( "You have unlocked the guild meatcar quest." );
		}
		else if ( success )
		{
			KoLmafia.updateDisplay( "Guild store successfully unlocked." );
		}
		else
		{
			KoLmafia.updateDisplay( "Guild store was not unlocked." );
		}
	}

	public void priceItemsAtLowestPrice( boolean avoidMinPrice )
	{
		RequestThread.openRequestSequence();
		RequestThread.postRequest( new StoreManageRequest() );

		SoldItem[] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		int[] itemId = new int[ sold.length ];
		int[] prices = new int[ sold.length ];
		int[] limits = new int[ sold.length ];

		// Now determine the desired prices on items.

		for ( int i = 0; i < sold.length; ++i )
		{
			itemId[ i ] = sold[ i ].getItemId();
			limits[ i ] = sold[ i ].getLimit();

			int minimumPrice = Math.max( 100, TradeableItemDatabase.getPriceById( sold[ i ].getItemId() ) * 2 );
			int desiredPrice = Math.max( minimumPrice, sold[ i ].getLowest() - sold[ i ].getLowest() % 100 );

			if ( sold[ i ].getPrice() == 999999999 && ( !avoidMinPrice || desiredPrice > minimumPrice ) )
			{
				prices[ i ] = desiredPrice;
			}
			else
			{
				prices[ i ] = sold[ i ].getPrice();
			}
		}

		RequestThread.postRequest( new StoreManageRequest( itemId, prices, limits ) );
		KoLmafia.updateDisplay( "Repricing complete." );
		RequestThread.closeRequestSequence();
	}

	/**
	 * Show an HTML string to the user
	 */

	public abstract void showHTML( String location, String text );

	public static final boolean hadPendingState()
	{
		return KoLmafia.hadPendingState;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request is permitted by the or by current circumstances
	 * in-game.
	 *
	 * @return <code>true</code> if requests are allowed to continue
	 */

	public static final boolean permitsContinue()
	{
		return KoLmafia.continuationState == KoLConstants.CONTINUE_STATE;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request will be denied by the regardless of continue
	 * state reset, until the display is enable (ie: in an abort state).
	 *
	 * @return <code>true</code> if requests are allowed to continue
	 */

	public static final boolean refusesContinue()
	{
		return KoLmafia.continuationState == KoLConstants.ABORT_STATE;
	}

	/**
	 * Forces a continue state. This should only be called when there is no doubt that a continue should occur.
	 *
	 * @return <code>true</code> if requests are allowed to continue
	 */

	public static final void forceContinue()
	{
		KoLmafia.continuationState = KoLConstants.CONTINUE_STATE;
	}

	/**
	 * Utility. This method used to decode a saved password. This should be called whenever a new password intends to be
	 * stored in the global file.
	 */

	public static final void addSaveState( final String username, final String password )
	{
		try
		{
			String utfString = URLEncoder.encode( password, "UTF-8" );

			StringBuffer encodedString = new StringBuffer();
			char currentCharacter;
			for ( int i = 0; i < utfString.length(); ++i )
			{
				currentCharacter = utfString.charAt( i );
				switch ( currentCharacter )
				{
				case '-':
					encodedString.append( "2D" );
					break;
				case '.':
					encodedString.append( "2E" );
					break;
				case '*':
					encodedString.append( "2A" );
					break;
				case '_':
					encodedString.append( "5F" );
					break;
				case '+':
					encodedString.append( "20" );
					break;

				case '%':
					encodedString.append( utfString.charAt( ++i ) );
					encodedString.append( utfString.charAt( ++i ) );
					break;

				default:
					encodedString.append( Integer.toHexString( (int) currentCharacter ).toUpperCase() );
					break;
				}
			}

			KoLSettings.setGlobalProperty(
				username, "saveState", ( new BigInteger( encodedString.toString(), 36 ) ).toString( 10 ) );
			if ( !KoLConstants.saveStateNames.contains( username ) )
			{
				KoLConstants.saveStateNames.add( username );
			}
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void removeSaveState( final String loginname )
	{
		if ( loginname == null )
		{
			return;
		}

		KoLConstants.saveStateNames.remove( loginname );
		KoLSettings.setGlobalProperty( loginname, "saveState", "" );
	}

	/**
	 * Utility. The method used to decode a saved password. This should be called whenever a new password intends to be
	 * stored in the global file.
	 */

	public static final String getSaveState( final String loginname )
	{
		try
		{
			String password = KoLSettings.getGlobalProperty( loginname, "saveState" );
			if ( password == null || password.length() == 0 || password.indexOf( "/" ) != -1 )
			{
				return null;
			}

			String hexString = ( new BigInteger( password, 10 ) ).toString( 36 );
			StringBuffer utfString = new StringBuffer();
			for ( int i = 0; i < hexString.length(); ++i )
			{
				utfString.append( '%' );
				utfString.append( hexString.charAt( i ) );
				utfString.append( hexString.charAt( ++i ) );
			}

			return URLDecoder.decode( utfString.toString(), "UTF-8" );
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	public static final boolean checkRequirements( final List requirements )
	{
		return KoLmafia.checkRequirements( requirements, true );
	}

	public static final boolean checkRequirements( final List requirements, final boolean retrieveItem )
	{
		AdventureResult[] requirementsArray = new AdventureResult[ requirements.size() ];
		requirements.toArray( requirementsArray );

		int actualCount = 0;

		// Check the items required for this quest,
		// retrieving any items which might be inside
		// of a closet somewhere.

		for ( int i = 0; i < requirementsArray.length; ++i )
		{
			if ( requirementsArray[ i ] == null )
			{
				continue;
			}

			if ( requirementsArray[ i ].isItem() && retrieveItem )
			{
				AdventureDatabase.retrieveItem( requirementsArray[ i ] );
			}

			if ( requirementsArray[ i ].isItem() )
			{
				actualCount = requirementsArray[ i ].getCount( KoLConstants.inventory );
			}
			else if ( requirementsArray[ i ].isStatusEffect() )
			{
				actualCount = requirementsArray[ i ].getCount( KoLConstants.activeEffects );
			}
			else if ( requirementsArray[ i ].getName().equals( AdventureResult.MEAT ) )
			{
				actualCount = KoLCharacter.getAvailableMeat();
			}

			if ( actualCount >= requirementsArray[ i ].getCount() )
			{
				requirements.remove( requirementsArray[ i ] );
			}
			else if ( actualCount > 0 )
			{
				AdventureResult.addResultToList( requirements, requirementsArray[ i ].getInstance( 0 - actualCount ) );
			}
		}

		// If there are any missing requirements
		// be sure to return false.  Otherwise,
		// you managed to get everything.

		return requirements.isEmpty();
	}

	public static final void printList( final List printing )
	{
		KoLmafia.printList( printing, RequestLogger.INSTANCE );
	}

	public static final void printList( final List printing, final PrintStream ostream )
	{
		if ( printing == null || ostream == null )
		{
			return;
		}

		StringBuffer buffer = new StringBuffer();

		if ( printing != KoLConstants.availableSkills )
		{
			Object current;
			for ( int i = 0; i < printing.size(); ++i )
			{
				current = printing.get( i );
				if ( current == null )
				{
					continue;
				}

				buffer.append( current.toString() );
				buffer.append( KoLConstants.LINE_BREAK );
			}

			ostream.println( buffer.toString() );
			return;
		}

		ClassSkillsDatabase.generateSkillList( buffer, false );

		if ( ostream != RequestLogger.INSTANCE )
		{
			ostream.println( buffer.toString() );
			return;
		}

		RequestLogger.printLine( buffer.toString(), false );

		buffer.setLength( 0 );
		ClassSkillsDatabase.generateSkillList( buffer, true );
		KoLConstants.commandBuffer.append( buffer.toString() );
	}

	/**
	 * Utility method used to purchase the given number of items from the mall using the given purchase requests.
	 */

	public void makePurchases( final List results, final Object[] purchases, final int maxPurchases,
		final boolean isAutomated )
	{
		if ( purchases.length == 0 )
		{
			return;
		}

		for ( int i = 0; i < purchases.length; ++i )
		{
			if ( !( purchases[ i ] instanceof MallPurchaseRequest ) )
			{
				return;
			}
		}

		RequestThread.openRequestSequence();
		MallPurchaseRequest currentRequest = (MallPurchaseRequest) purchases[ 0 ];
		AdventureResult itemToBuy = new AdventureResult( currentRequest.getItemId(), 0 );

		int initialCount = itemToBuy.getCount( KoLConstants.inventory );
		int currentCount = initialCount;
		int desiredCount = maxPurchases == Integer.MAX_VALUE ? Integer.MAX_VALUE : initialCount + maxPurchases;

		int previousLimit = 0;

		int currentPrice = currentRequest.getPrice();
		int priceLimit =
			purchases.length < 3 ? Integer.MAX_VALUE : ( (MallPurchaseRequest) purchases[ 2 ] ).getPrice() * 2;

		for ( int i = 0; i < purchases.length && currentCount < desiredCount && KoLmafia.permitsContinue(); ++i )
		{
			currentRequest = (MallPurchaseRequest) purchases[ i ];
			currentPrice = currentRequest.getPrice();

			if ( currentRequest.getQuantity() != MallPurchaseRequest.MAX_QUANTITY )
			{
				if ( !KoLCharacter.canInteract() || isAutomated && !KoLSettings.getBooleanProperty( "autoSatisfyWithMall" ) )
				{
					continue;
				}
			}

			if ( isAutomated )
			{
				if ( currentPrice >= priceLimit )
				{
					KoLmafia.updateDisplay( "Stopped purchasing " + currentRequest.getItemName() + " @ " + KoLConstants.COMMA_FORMAT.format( currentPrice ) + "." );
				}
			}

			// Keep track of how many of the item you had before
			// you run the purchase request

			previousLimit = currentRequest.getLimit();
			currentRequest.setLimit( Math.min( previousLimit, desiredCount - currentCount ) );
			RequestThread.postRequest( currentRequest );

			// Remove the purchase from the list!  Because you
			// have already made a purchase from the store

			if ( KoLmafia.permitsContinue() )
			{
				if ( currentRequest.getQuantity() == currentRequest.getLimit() )
				{
					results.remove( currentRequest );
				}
				else if ( currentRequest.getQuantity() == MallPurchaseRequest.MAX_QUANTITY )
				{
					currentRequest.setLimit( MallPurchaseRequest.MAX_QUANTITY );
				}
				else
				{
					if ( currentRequest.getLimit() == previousLimit )
					{
						currentRequest.setCanPurchase( false );
					}

					currentRequest.setQuantity( currentRequest.getQuantity() - currentRequest.getLimit() );
					currentRequest.setLimit( previousLimit );
				}
			}
			else
			{
				currentRequest.setLimit( previousLimit );
			}

			// Now update how many you actually have for the next
			// iteration of the loop.

			currentCount = itemToBuy.getCount( KoLConstants.inventory );
		}

		// With all that information parsed out, we should
		// refresh the lists at the very end.

		if ( itemToBuy.getCount( KoLConstants.inventory ) >= desiredCount || maxPurchases == Integer.MAX_VALUE )
		{
			KoLmafia.updateDisplay( "Purchases complete." );
		}
		else
		{
			KoLmafia.updateDisplay( "Desired purchase quantity not reached (wanted " + maxPurchases + ", got " + ( currentCount - initialCount ) + ")" );
		}

		RequestThread.closeRequestSequence();
	}

	/**
	 * Utility method used to register a given adventure in the running adventure summary.
	 */

	public void registerAdventure( final KoLAdventure adventureLocation )
	{
		String adventureName = adventureLocation.getAdventureName();
		if ( adventureName == null )
		{
			return;
		}

		this.recognizeEncounter( adventureName );
		RegisteredEncounter previousAdventure = (RegisteredEncounter) KoLConstants.adventureList.lastElement();

		if ( previousAdventure != null && previousAdventure.name.equals( adventureName ) )
		{
			++previousAdventure.encounterCount;
			KoLConstants.adventureList.set( KoLConstants.adventureList.size() - 1, previousAdventure );
		}
		else
		{
			KoLConstants.adventureList.add( new RegisteredEncounter( null, adventureName ) );
		}
	}

	public static final String encounterType( final String encounterName )
	{
		for ( int i = 0; i < KoLmafia.SPECIAL_ENCOUNTERS.length; ++i )
		{
			if ( encounterName.equalsIgnoreCase( KoLmafia.SPECIAL_ENCOUNTERS[ i ][ 0 ] ) )
			{
				return KoLmafia.SPECIAL_ENCOUNTERS[ i ][ 1 ];
			}
		}
		return KoLmafia.NONE;
	}

	public static final boolean isAutoStop( final String encounterName )
	{
		String encounterType = KoLmafia.encounterType( encounterName );
		return encounterType == KoLmafia.STOP;
	}

	public void recognizeEncounter( final String encounterName )
	{
		String encounterType = KoLmafia.encounterType( encounterName );

		if ( encounterType == KoLmafia.NONE )
		{
			return;
		}

		if ( encounterType == KoLmafia.SEMIRARE )
		{
			StaticEntity.stopCounting( "Fortune Cookie" );
			return;
		}

		if ( encounterType == KoLmafia.STOP )
		{
			RequestLogger.printLine();

			if ( KoLConstants.conditions.isEmpty() )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, encounterName );
				RequestLogger.printLine();
			}
			else
			{
				KoLmafia.updateDisplay( encounterName );
				KoLmafia.updateDisplay( "There are still unsatisfied conditions." );
			}

			RequestThread.enableDisplayIfSequenceComplete();
		}
	}

	/**
	 * Utility. The method used to register a given encounter in the running adventure summary.
	 */

	public void registerEncounter( String encounterName, final String encounterType )
	{
		encounterName = encounterName.trim();
		this.recognizeEncounter( encounterName );

		RegisteredEncounter[] encounters = new RegisteredEncounter[ KoLConstants.encounterList.size() ];
		KoLConstants.encounterList.toArray( encounters );

		for ( int i = 0; i < encounters.length; ++i )
		{
			if ( encounters[ i ].name.equals( encounterName ) )
			{
				++encounters[ i ].encounterCount;

				// Manually set to force repainting in GUI
				KoLConstants.encounterList.set( i, encounters[ i ] );
				return;
			}
		}

		if ( encounterName.equalsIgnoreCase( "Cheetahs Never Lose" ) && KoLCharacter.hasItem( KoLmafia.CATNIP ) )
		{
			this.processResult( KoLmafia.CATNIP.getNegation() );
		}
		if ( encounterName.equalsIgnoreCase( "Summer Holiday" ) && KoLCharacter.hasItem( KoLmafia.GLIDER ) )
		{
			this.processResult( KoLmafia.GLIDER.getNegation() );
		}

		KoLConstants.encounterList.add( new RegisteredEncounter( encounterType, encounterName ) );
	}

	private class RegisteredEncounter
		implements Comparable
	{
		private final String type;
		private final String name;
		private final String stringform;
		private int encounterCount;

		public RegisteredEncounter( final String type, final String name )
		{
			this.type = type;
			this.name = name;

			this.stringform = type == null ? name : type + ": " + name;
			this.encounterCount = 1;
		}

		public String toString()
		{
			return "<html>" + this.stringform + " (" + this.encounterCount + ")</html>";
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof RegisteredEncounter ) || o == null )
			{
				return -1;
			}

			if ( this.type == null || ( (RegisteredEncounter) o ).type == null || this.type.equals( ( (RegisteredEncounter) o ).type ) )
			{
				return this.name.compareToIgnoreCase( ( (RegisteredEncounter) o ).name );
			}

			return this.type.equals( "Combat" ) ? 1 : -1;
		}
	}

	public final String[] extractTargets( final String targetList )
	{
		// If there are no targets in the list, then
		// return absolutely nothing.

		if ( targetList == null || targetList.trim().length() == 0 )
		{
			return new String[ 0 ];
		}

		// Otherwise, split the list of targets, and
		// determine who all the unique targets are.

		String[] targets = targetList.trim().split( "\\s*,\\s*" );
		for ( int i = 0; i < targets.length; ++i )
		{
			targets[ i ] =
				KoLmafia.getPlayerId( targets[ i ] ) == null ? targets[ i ] : KoLmafia.getPlayerId( targets[ i ] );
		}

		// Sort the list in order to increase the
		// speed of duplicate detection.

		Arrays.sort( targets );

		// Determine who all the duplicates are.

		int uniqueListSize = targets.length;
		for ( int i = 1; i < targets.length; ++i )
		{
			if ( targets[ i ].equals( targets[ i - 1 ] ) )
			{
				targets[ i - 1 ] = null;
				--uniqueListSize;
			}
		}

		// Now, create the list of unique targets;
		// if the list has the same size as the original,
		// you can skip this step.

		if ( uniqueListSize != targets.length )
		{
			int addedCount = 0;
			String[] uniqueList = new String[ uniqueListSize ];
			for ( int i = 0; i < targets.length; ++i )
			{
				if ( targets[ i ] != null )
				{
					uniqueList[ addedCount++ ] = targets[ i ];
				}
			}

			targets = uniqueList;
		}

		// Convert all the user Ids back to the
		// original player names so that the results
		// are easy to understand for the user.

		for ( int i = 0; i < targets.length; ++i )
		{
			targets[ i ] =
				KoLmafia.getPlayerName( targets[ i ] ) == null ? targets[ i ] : KoLmafia.getPlayerName( targets[ i ] );
		}

		// Sort the list one more time, this time
		// by player name.

		Arrays.sort( targets );

		// Parsing complete.  Return the list of
		// unique targets.

		return targets;
	}

	public final void downloadAdventureOverride()
	{
		for ( int i = 0; i < KoLmafia.OVERRIDE_DATA.length; ++i )
		{
			if ( !downloadOverride( KoLmafia.OVERRIDE_DATA[ i ] ) )
			{
				RequestThread.closeRequestSequence();
				return;
			}
		}

		KoLmafia.updateDisplay( "Please restart KoLmafia to complete the update." );
		RequestThread.enableDisplayIfSequenceComplete();
	}

	private final boolean downloadOverride( String name )
	{
		KoLmafia.updateDisplay( "Downloading " + name + "..." );

		BufferedReader reader =
			KoLDatabase.getReader( "http://kolmafia.svn.sourceforge.net/viewvc/*checkout*/kolmafia/src/data/" + name );

		File output = new File( UtilityConstants.DATA_LOCATION, "temp.txt" );
		LogStream writer = LogStream.openStream( output, true );

		String line;

		while ( true )
		{
			try
			{
				line = reader.readLine();
			}
			catch ( IOException e )
			{
				StaticEntity.printStackTrace( e );
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE,
					"IO error reading from subversion service for " + name + "." );
				writer.close();
				output.delete();
				return false;
			}

			if ( line == null )
				break;

                        writer.println( line );
		}

		try
		{
			reader.close();
		}
		catch ( IOException e )
		{
		}

		writer.close();

		// File successfully downloaded.
		// Delete existing copy, if any,

		File dest = new File( UtilityConstants.DATA_LOCATION, name );
		if ( dest.exists() )
		{
			dest.delete();
		}

		// Rename temp file to desired file
		output.renameTo( dest );

		return true;
	}

	public static final boolean isRunningBetweenBattleChecks()
	{
		return KoLmafia.recoveryActive || MoodSettings.isExecuting();
	}

	public boolean runThresholdChecks()
	{
		float autoStopValue = KoLSettings.getFloatProperty( "autoAbortThreshold" );
		if ( autoStopValue >= 0.0f )
		{
			autoStopValue *= (float) KoLCharacter.getMaximumHP();
			if ( KoLCharacter.getCurrentHP() <= autoStopValue )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ABORT_STATE, "Health fell below " + (int) autoStopValue + ". Auto-abort triggered." );
				return false;
			}
		}

		return true;
	}

	public void runBetweenBattleChecks( final boolean isFullCheck )
	{
		this.runBetweenBattleChecks( isFullCheck, isFullCheck, true, isFullCheck );
	}

	public void runBetweenBattleChecks( final boolean isScriptCheck, final boolean isMoodCheck,
		final boolean isHealthCheck, final boolean isManaCheck )
	{
		// Do not run between battle checks if you are in the middle
		// of your checks or if you have aborted.

		if ( KoLmafia.recoveryActive || KoLmafia.refusesContinue() )
		{
			return;
		}

		// First, run the between battle script defined by the
		// user, which may make it so that none of the built
		// in behavior needs to run.

		RequestThread.openRequestSequence();
		KoLmafia.recoveryActive = true;

		if ( isScriptCheck )
		{
			String scriptPath = KoLSettings.getUserProperty( "betweenBattleScript" );
			if ( !scriptPath.equals( "" ) )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptPath );
			}
		}

		KoLmafia.recoveryActive = false;
		SpecialOutfit.createImplicitCheckpoint();
		KoLmafia.recoveryActive = true;

		// Now, run the built-in behavior to take care of
		// any loose ends.

		if ( isMoodCheck )
		{
			MoodSettings.execute();
		}

		if ( isHealthCheck )
		{
			this.recoverHP();
		}

		if ( isMoodCheck )
		{
			MoodSettings.burnExtraMana( false );
		}

		if ( isManaCheck )
		{
			this.recoverMP();
		}

		KoLmafia.recoveryActive = false;
		SpecialOutfit.restoreImplicitCheckpoint();
		RequestThread.closeRequestSequence();

		if ( KoLCharacter.getCurrentHP() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Insufficient health to continue (auto-abort triggered)." );
		}

		if ( KoLmafia.permitsContinue() && KoLmafia.currentIterationString.length() > 0 )
		{
			RequestLogger.printLine();
			KoLmafia.updateDisplay( KoLmafia.currentIterationString );
			KoLmafia.currentIterationString = "";
		}
	}

	public void startRelayServer()
	{
		if ( LocalRelayServer.isRunning() )
		{
			return;
		}

		LocalRelayServer.startThread();

		// Wait for 5 seconds before giving up
		// on the relay server.

		for ( int i = 0; i < 50 && !LocalRelayServer.isRunning(); ++i )
		{
			KoLRequest.delay( 500 );
		}

		if ( !LocalRelayServer.isRunning() )
		{
			return;
		}
	}

	public void openRelayBrowser()
	{
		if ( KoLRequest.isCompactMode )
		{
			this.openRelayBrowser( "main_c.html", false );
		}
		else
		{
			this.openRelayBrowser( "main.html", false );
		}
	}

	public void openRelayBrowser( final String location )
	{
		this.openRelayBrowser( location, false );
	}

	public void openRelayBrowser( final String location, boolean forceMain )
	{
		this.startRelayServer();

		if ( !forceMain )
		{
			StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + "/" + location );
		}
		else
		{
			LocalRelayRequest.setNextMain( location );
			this.openRelayBrowser();
		}
	}

	public void launchRadioKoL()
	{
		StaticEntity.openSystemBrowser( "http://209.9.238.5:8794/listen.pls" );
	}

	public void launchSimulator()
	{
		this.startRelayServer();
		StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + "/simulator/index.html" );
	}

	public static final boolean isAdventuring()
	{
		return KoLmafia.isAdventuring;
	}

	public void removeAllItemsFromStore()
	{
		RequestThread.openRequestSequence();
		RequestThread.postRequest( new StoreManageRequest() );

		// Now determine the desired prices on items.
		// If the value of an item is currently 100,
		// then remove the item from the store.

		SoldItem[] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		for ( int i = 0; i < sold.length && KoLmafia.permitsContinue(); ++i )
		{
			RequestThread.postRequest( new StoreManageRequest( sold[ i ].getItemId() ) );
		}

		KoLmafia.updateDisplay( "Store emptying complete." );
		RequestThread.closeRequestSequence();
	}

	/**
	 * Hosts a massive sale on the items currently in your store. Utilizes the "minimum meat" principle.
	 */

	public void makeEndOfRunSaleRequest( final boolean avoidMinPrice )
	{
		if ( !KoLCharacter.canInteract() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You are not yet out of ronin." );
			return;
		}

		if ( !KoLFrame.confirm( "Are you sure you'd like to host an end-of-run sale?" ) )
		{
			return;
		}

		// Find all tradeable items.  Tradeable items
		// are marked by an autosell value of nonzero.

		RequestThread.openRequestSequence();

		// Only place items in the mall which are not
		// sold in NPC stores and can be autosold.

		AdventureResult[] items = new AdventureResult[ KoLConstants.inventory.size() ];
		KoLConstants.inventory.toArray( items );

		ArrayList autosell = new ArrayList();
		ArrayList automall = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[ i ].getItemId() == KoLConstants.MEAT_PASTE || items[ i ].getItemId() == KoLConstants.MEAT_STACK || items[ i ].getItemId() == KoLConstants.DENSE_STACK )
			{
				continue;
			}

			if ( !TradeableItemDatabase.isTradeable( items[ i ].getItemId() ) )
			{
				continue;
			}

			if ( TradeableItemDatabase.getPriceById( items[ i ].getItemId() ) <= 0 )
			{
				continue;
			}

			if ( NPCStoreDatabase.contains( items[ i ].getName(), false ) )
			{
				autosell.add( items[ i ] );
			}
			else
			{
				automall.add( items[ i ] );
			}
		}

		// Now, place all the items in the mall at the
		// maximum possible price.  This allows KoLmafia
		// to determine the minimum price.

		if ( autosell.size() > 0 && KoLmafia.permitsContinue() )
		{
			RequestThread.postRequest( new AutoSellRequest( autosell.toArray(), AutoSellRequest.AUTOSELL ) );
		}

		if ( automall.size() > 0 && KoLmafia.permitsContinue() )
		{
			RequestThread.postRequest( new AutoSellRequest( automall.toArray(), AutoSellRequest.AUTOMALL ) );
		}

		// Now, remove all the items that you intended
		// to remove from the store due to pricing issues.

		if ( KoLmafia.permitsContinue() )
		{
			this.priceItemsAtLowestPrice( avoidMinPrice );
		}

		KoLmafia.updateDisplay( "Undercutting sale complete." );
		RequestThread.closeRequestSequence();
	}

	public void makeAutoMallRequest()
	{
		// Now you've got all the items used up, go ahead and prepare to
		// sell anything that's left.

		int itemCount;

		AdventureResult currentItem;
		Object[] items = KoLConstants.profitableList.toArray();

		ArrayList sellList = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = (AdventureResult) items[ i ];

			if ( KoLConstants.mementoList.contains( currentItem ) )
			{
				continue;
			}

			if ( currentItem.getItemId() == KoLConstants.MEAT_PASTE || currentItem.getItemId() == KoLConstants.MEAT_STACK || currentItem.getItemId() == KoLConstants.DENSE_STACK )
			{
				continue;
			}

			itemCount = currentItem.getCount( KoLConstants.inventory );

			if ( itemCount > 0 )
			{
				sellList.add( currentItem.getInstance( itemCount ) );
			}
		}

		if ( !sellList.isEmpty() )
		{
			RequestThread.postRequest( new AutoSellRequest( sellList.toArray(), AutoSellRequest.AUTOMALL ) );
		}

		RequestThread.closeRequestSequence();
	}

	public void makeJunkRemovalRequest()
	{
		int itemCount;
		AdventureResult currentItem;

		Object[] items = KoLConstants.junkList.toArray();

		// Before doing anything else, go through the list of items which are
		// traditionally used and use them.  Also, if the item can be untinkered,
		// it's usually more beneficial to untinker first.

		boolean madeUntinkerRequest = false;
		boolean canUntinker = UntinkerRequest.canUntinker();

		RequestThread.openRequestSequence();
		ArrayList closetList = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			if ( !KoLConstants.singletonList.contains( items[ i ] ) || KoLConstants.closet.contains( items[ i ] ) )
			{
				continue;
			}

			if ( KoLConstants.inventory.contains( items[ i ] ) )
			{
				closetList.add( ( (AdventureResult) items[ i ] ).getInstance( 1 ) );
			}
		}

		RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, closetList.toArray() ) );

		do
		{
			madeUntinkerRequest = false;

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[ i ];
				itemCount = currentItem.getCount( KoLConstants.inventory );

				if ( itemCount == 0 )
				{
					continue;
				}

				if ( canUntinker && ConcoctionsDatabase.getMixingMethod( currentItem.getItemId() ) == KoLConstants.COMBINE )
				{
					RequestThread.postRequest( new UntinkerRequest( currentItem.getItemId() ) );
					madeUntinkerRequest = true;
					continue;
				}

				switch ( currentItem.getItemId() )
				{
				case 184: // briefcase
				case 533: // Gnollish toolbox
				case 553: // 31337 scroll
				case 604: // Penultimate fantasy chest
				case 831: // small box
				case 832: // large box
				case 1768: // Gnomish toolbox
				case 1917: // old leather wallet
				case 1918: // old coin purse
				case 2057: // black pension check
				case 2058: // black picnic basket
				case 2511: // Frat Army FGF
				case 2512: // Hippy Army MPE
				case 2536: // canopic jar
				case 2612: // ancient vinyl coin purse
					RequestThread.postRequest( new ConsumeItemRequest( currentItem.getInstance( itemCount ) ) );
					break;

				case 621: // Warm Subject gift certificate
					RequestThread.postRequest( new ConsumeItemRequest( currentItem.getInstance( itemCount ) ) );
					break;
				}
			}
		}
		while ( madeUntinkerRequest );

		// Now you've got all the items used up, go ahead and prepare to
		// pulverize strong equipment.

		int itemPower;

		if ( KoLCharacter.hasSkill( "Pulverize" ) )
		{
			boolean hasMalusAccess = KoLCharacter.isMuscleClass();

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[ i ];

				if ( KoLConstants.mementoList.contains( currentItem ) )
				{
					continue;
				}

				if ( currentItem.getName().startsWith( "antique" ) )
				{
					continue;
				}

				itemCount = currentItem.getCount( KoLConstants.inventory );
				itemPower = EquipmentDatabase.getPower( currentItem.getItemId() );

				if ( itemCount > 0 && !NPCStoreDatabase.contains( currentItem.getName(), false ) )
				{
					switch ( TradeableItemDatabase.getConsumptionType( currentItem.getItemId() ) )
					{
					case EQUIP_HAT:
					case EQUIP_PANTS:
					case EQUIP_SHIRT:
					case EQUIP_WEAPON:
					case EQUIP_OFFHAND:

						if ( KoLCharacter.hasItem( ConcoctionsDatabase.HAMMER ) && itemPower >= 100 || hasMalusAccess && itemPower > 10 )
						{
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );
						}

						break;

					case EQUIP_FAMILIAR:
					case EQUIP_ACCESSORY:

						if ( KoLCharacter.hasItem( ConcoctionsDatabase.HAMMER ) )
						{
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );
						}

						break;

					default:

						if ( currentItem.getName().endsWith( "powder" ) || currentItem.getName().endsWith( "nugget" ) )
						{
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );
						}

						break;
					}
				}
			}
		}

		// Now you've got all the items used up, go ahead and prepare to
		// sell anything that's left.

		ArrayList sellList = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = (AdventureResult) items[ i ];

			if ( KoLConstants.mementoList.contains( currentItem ) )
			{
				continue;
			}

			if ( currentItem.getItemId() == KoLConstants.MEAT_PASTE )
			{
				continue;
			}

			itemCount = currentItem.getCount( KoLConstants.inventory );
			if ( itemCount > 0 )
			{
				sellList.add( currentItem.getInstance( itemCount ) );
			}
		}

		if ( !sellList.isEmpty() )
		{
			RequestThread.postRequest( new AutoSellRequest( sellList.toArray(), AutoSellRequest.AUTOSELL ) );
			sellList.clear();
		}

		if ( !KoLCharacter.canInteract() )
		{
			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[ i ];

				if ( KoLConstants.mementoList.contains( currentItem ) )
				{
					continue;
				}

				if ( currentItem.getItemId() == KoLConstants.MEAT_PASTE )
				{
					continue;
				}

				itemCount = currentItem.getCount( KoLConstants.inventory ) - 1;
				if ( itemCount > 0 )
				{
					sellList.add( currentItem.getInstance( itemCount ) );
				}
			}

			if ( !sellList.isEmpty() )
			{
				RequestThread.postRequest( new AutoSellRequest( sellList.toArray(), AutoSellRequest.AUTOSELL ) );
			}
		}

		RequestThread.closeRequestSequence();
	}

	public void handleAscension()
	{
		RequestThread.openRequestSequence();
		KoLSettings.setUserProperty( "lastBreakfast", "-1" );

		KoLmafia.resetCounters();
		KoLmafia.resetPerAscensionCounters();
		KoLCharacter.reset();

		this.refreshSession( false );
		this.resetSession();
		KoLConstants.conditions.clear();

		// Based on your class, you get some basic
		// items once you ascend.

		String type = KoLCharacter.getClassType();
		if ( type.equals( KoLCharacter.SEAL_CLUBBER ) )
		{
			KoLCharacter.processResult( new AdventureResult( "seal-skull helmet", 1, false ), false );
			KoLCharacter.processResult( new AdventureResult( "seal-clubbing club", 1, false ), false );
		}
		else if ( type.equals( KoLCharacter.TURTLE_TAMER ) )
		{
			KoLCharacter.processResult( new AdventureResult( "helmet turtle", 1, false ), false );
			KoLCharacter.processResult( new AdventureResult( "turtle totem", 1, false ), false );
		}
		else if ( type.equals( KoLCharacter.PASTAMANCER ) )
		{
			KoLCharacter.processResult( new AdventureResult( "pasta spoon", 1, false ), false );
			KoLCharacter.processResult( new AdventureResult( "ravioli hat", 1, false ), false );
		}
		else if ( type.equals( KoLCharacter.SAUCEROR ) )
		{
			KoLCharacter.processResult( new AdventureResult( "saucepan", 1, false ), false );
			KoLCharacter.processResult( new AdventureResult( "spices", 1, false ), false );
		}
		else if ( type.equals( KoLCharacter.DISCO_BANDIT ) )
		{
			KoLCharacter.processResult( new AdventureResult( "disco ball", 1, false ), false );
			KoLCharacter.processResult( new AdventureResult( "disco mask", 1, false ), false );
		}
		else if ( type.equals( KoLCharacter.ACCORDION_THIEF ) )
		{
			KoLCharacter.processResult( new AdventureResult( "mariachi pants", 1, false ), false );
			KoLCharacter.processResult( new AdventureResult( "stolen accordion", 1, false ), false );
		}

		// Note the information in the session log
		// for recording purposes.

		MoodSettings.setMood( "apathetic" );
		PrintStream sessionStream = RequestLogger.getSessionStream();

		sessionStream.println();
		sessionStream.println();
		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		sessionStream.println( "           Beginning New Ascension           " );
		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		sessionStream.println();

		sessionStream.println( "Ascension #" + KoLCharacter.getAscensions() + ":" );

		if ( KoLCharacter.isHardcore() )
		{
			sessionStream.print( "Hardcore " );
		}
		else
		{
			sessionStream.print( "Softcore " );
		}

		if ( KoLCharacter.canEat() && KoLCharacter.canDrink() )
		{
			sessionStream.print( "No-Path " );
		}
		else if ( KoLCharacter.canEat() )
		{
			sessionStream.print( "Teetotaler " );
		}
		else if ( KoLCharacter.canDrink() )
		{
			sessionStream.print( "Boozetafarian " );
		}
		else
		{
			sessionStream.print( "Oxygenarian " );
		}

		sessionStream.println( KoLCharacter.getClassType() );
		sessionStream.println();
		sessionStream.println();

		KoLmafia.printList( KoLConstants.availableSkills, sessionStream );
		sessionStream.println();

		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );

		sessionStream.println();
		sessionStream.println();

		RequestThread.closeRequestSequence();
	}

	private static class UpdateCheckThread
		extends Thread
	{
		public void run()
		{
			if ( KoLConstants.VERSION_NAME.startsWith( "KoLmafia r" ) )
			{
				return;
			}

			long lastUpdate = Long.parseLong( KoLSettings.getUserProperty( "lastRssUpdate" ) );
			if ( System.currentTimeMillis() - lastUpdate < 86400000L )
			{
				return;
			}

			try
			{
				String line;
				StringBuffer contents = new StringBuffer();
				BufferedReader reader =
					KoLDatabase.getReader( "http://sourceforge.net/export/rss2_projfiles.php?group_id=126572" );

				while ( ( line = reader.readLine() ) != null )
				{
					contents.append( line );
				}

				Matcher updateMatcher =
					Pattern.compile( "<title>(KoLmafia [^<]*?) released [^<]*?</title>" ).matcher( contents.toString() );
				if ( !updateMatcher.find() )
				{
					System.out.println( contents );
					return;
				}

				String lastVersion = KoLSettings.getUserProperty( "lastRssVersion" );
				String currentVersion = updateMatcher.group( 1 );

				KoLSettings.setUserProperty( "lastRssVersion", currentVersion );

				if ( currentVersion.equals( KoLConstants.VERSION_NAME ) || currentVersion.equals( lastVersion ) )
				{
					return;
				}

				if ( KoLFrame.confirm( "A new version of KoLmafia is now available.  Would you like to download it now?" ) )
				{
					StaticEntity.openSystemBrowser( "https://sourceforge.net/project/showfiles.php?group_id=126572" );
				}
			}
			catch ( Exception e )
			{
			}
		}
	}

	private static class ShutdownThread
		extends Thread
	{
		public void run()
		{
			KoLSettings.saveFlaggedItemList();
			CustomItemDatabase.saveItemData();

			RequestLogger.closeSessionLog();
			RequestLogger.closeDebugLog();
			RequestLogger.closeMirror();

			SystemTrayFrame.removeTrayIcon();
			LocalRelayServer.stop();

			try
			{
				KoLmafia.SESSION_HOLDER.release();
				KoLmafia.SESSION_CHANNEL.close();
				KoLmafia.SESSION_FILE.delete();
			}
			catch ( Exception e )
			{
				// That means the file either doesn't exist or the
				// session holder was somehow closed.  Ignore and
				// fall through.
			}
		}
	}
}
