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
import java.awt.Frame;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;

import java.math.BigInteger;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import apple.dts.samplecode.osxadapter.OSXAdapter;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.CustomItemDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.FlaggedItems;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest;
import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.CustomOutfitRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.MoonPhaseRequest;
import net.sourceforge.kolmafia.request.PasswordHashRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.session.ActionBarManager;
import net.sourceforge.kolmafia.session.BadMoonManager;
import net.sourceforge.kolmafia.session.ConsequenceManager;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LogoutManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.ValhallaManager;

import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;

import net.sourceforge.kolmafia.swingui.listener.LicenseDisplayListener;

import net.sourceforge.kolmafia.swingui.panel.GenericPanel;

import net.sourceforge.kolmafia.textui.Interpreter;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;
import net.sourceforge.kolmafia.webui.RelayServer;

public abstract class KoLmafia
{
	private static boolean isRefreshing = false;
	private static boolean isAdventuring = false;
	private static volatile String abortAfter = null;

	public static String lastMessage = " ";

	static
	{
		System.setProperty( "sun.java2d.noddraw", "true" );
		System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "KoLmafia" );
		System.setProperty( "com.apple.mrj.application.live-resize", "true" );
		System.setProperty( "com.apple.mrj.application.growbox.intrudes", "false" );

		JEditorPane.registerEditorKitForContentType( "text/html", RequestEditorKit.class.getName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
	}

	public static String currentIterationString = "";
	public static boolean tookChoice = false;
	public static boolean redoSkippedAdventures = true;

	public static boolean isMakingRequest = false;
	public static MafiaState continuationState = MafiaState.CONTINUE;
	public static MafiaState displayState = MafiaState.ENABLE;
	private static boolean allowDisplayUpdate = true;

	public static final int[] initialStats = new int[ 3 ];

	private static FileLock SESSION_HOLDER = null;
	private static FileChannel SESSION_CHANNEL = null;
	private static File SESSION_FILE = null;
	private static boolean SESSION_ENDING = false;

	public static KoLAdventure currentAdventure;
	public static String statDay;

	// Types of special encounters
	public static final String NONE = "0";
	public static final String STOP = "1";
	public static final String SEMIRARE = "2";
	public static final String GLYPH = "3";
	public static final String BADMOON = "4";

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
		// The following is unnecessary, since you can use "1 choice" as a goal
		{ "Mr. Alarm, I Presarm", STOP },
		{ "We'll All Be Flat", STOP },
		{ "You and the Cap'm Make it Hap'm", STOP },
		// The following is unnecessary, since you can use "blueprints" as a goal
		{ "This Adventure Bites", STOP },
		{ "It's Always Swordfish", STOP },
		{ "Granny, Does Your Dogfish Bite?", STOP },
		{ "Not a Micro Fish", STOP },
		{ "You've Hit Bottom", STOP },
		{ "Ode to the Sea", STOP },
		{ "Boxing the Juke", STOP },

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
		// 3-15.
		// Adventure in Arid, Extra-Dry Desert until you have
		// worm-riding hooks.

		// The following is unnecessary, since you can use "worm-riding hooks" as a goal
		{ "The Sleeper Has Awakened", STOP },

		// Axecore Clancy Adventures
		{ "Jackin' the Jukebox", STOP },
		{ "A Miner Variation", STOP },
		{ "Mercury Rising", STOP },
		{ "Don't You Know Who I Am?", STOP },

		// Adventures that give semirares
		{ "7-Foot Dwarf Foreman", SEMIRARE },
		{ "A Menacing Phantom", SEMIRARE },
		{ "A Shark's Chum", SEMIRARE },
		{ "A Tight Squeeze", SEMIRARE },
		{ "All The Rave", SEMIRARE },
		{ "Baa'baa'bu'ran", SEMIRARE },
		{ "Bad ASCII Art", SEMIRARE },
		{ "Blaaargh! Blaaargh!", SEMIRARE },
		{ "C. H. U. M. chieftain", SEMIRARE },
		{ "Cold Comfort", SEMIRARE },
		{ "Filth, Filth, and More Filth", SEMIRARE },
		{ "Flowers for You", SEMIRARE },
		{ "Hands On", SEMIRARE },
		{ "How Does He Smell?", SEMIRARE },
		{ "In the Still of the Alley", SEMIRARE },
		{ "It's The Only Way To Be Sure", SEMIRARE },
		{ "Juicy!", SEMIRARE },
		{ "Knob Goblin Elite Guard Captain", SEMIRARE },
		{ "Knob Goblin Embezzler", SEMIRARE },
		{ "Le Chauve-Souris du Parfum", SEMIRARE },
		{ "Like the Sunglasses, But Less Comfortable", SEMIRARE },
		{ "Lunchboxing", SEMIRARE },
		{ "Maybe It's a Sexy Snake!", SEMIRARE },
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
		{ "The Time This Fire", SEMIRARE },
		{ "Two Sizes Too Small", SEMIRARE },
		{ "What a Tosser", SEMIRARE },
		{ "Yo Ho Ho and a Bottle of Whatever This Is", SEMIRARE },
		{ "You Can Top Our Desserts, But You Can't Beat Our Meats", SEMIRARE },

		// Adventuring with the hobo code binder equipped - Glyph Adventures
		{ "A Funny Thing Happened On the Way", GLYPH },
		{ "Bacon Bacon Bacon", GLYPH },
		{ "Breakfast of Champions", GLYPH },
		{ "Elbereth? Who's Elbereth?", GLYPH },
		{ "For Sale By Squatter", GLYPH },
		{ "God Bless, Bra", GLYPH },
		{ "He's a Melancholy Drunk", GLYPH },
		{ "How Do I Shot Web?", GLYPH },
		{ "How Dry I Am", GLYPH },
		{ "It's In the Cards", GLYPH },
		{ "My Little Stowaway", GLYPH },
		{ "Not So Much With the Corncob Pipes, Either.", GLYPH },
		{ "Not a Standard-Issue Windowsill, Obviously", GLYPH },
		{ "Now You're a Hero", GLYPH },
		{ "Number 163", GLYPH },
		{ "Stumped", GLYPH },
		{ "They Gave at the Morgue", GLYPH },
		{ "They Hate Mimes, Too", GLYPH },
		{ "They Hate That", GLYPH },
		{ "Thud", GLYPH },
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

			PrintStream ostream = LogStream.openStream( KoLmafia.SESSION_FILE, true );
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
		System.out.println();
		System.out.println( StaticEntity.getVersion() );
		System.out.println( KoLConstants.VERSION_DATE );
		System.out.println();

		try
		{
			System.out.println( "Currently Running on " + System.getProperty( "os.name" ) );
			System.out.println( "Local Directory is " + KoLConstants.ROOT_LOCATION.getCanonicalPath() );
			System.out.println( "Using Java " + System.getProperty( "java.version" ) );
			System.out.println();
		}
		catch ( IOException e )
		{

		}

		boolean useGUI = true;

		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[ i ].equalsIgnoreCase( "--HELP" ) || args[ i ].equalsIgnoreCase( "/?" ) )
			{
				System.out.println( "An interface for the online adventure game, The Kingdom of Loathing." );
				System.out.println( "Please visit kolmafia.sourceforge.net for more information." );
				System.out.println();
				System.out.println( "KoLmafia [--Help] [--Version] [--CLI] [--GUI] script" );
				System.out.println();
				System.out.println( "  --Help        Display this message and exits." );
				System.out.println( "  --Version     Display the current version and exits." );
				System.out.println( "  --CLI         Run KoLmafia as a command line application." );
				System.out.println( "  --GUI         Run KoLmafia with a graphical user interface (Default)." );
				System.out.println( "  script        Specifies a script to call when starting KoLmafia." );

				System.exit( 0 );
			}
			else if ( args[ i ].equalsIgnoreCase( "--VERSION" ) )
			{
				System.exit( 0 );
			}
			else if ( args[ i ].equalsIgnoreCase( "--CLI" ) )
			{
				useGUI = false;
			}
			else if ( args[ i ].equalsIgnoreCase( "--GUI" ) )
			{
				useGUI = true;
			}
		}

		// All dates are presented as if the day began at rollover.

		TimeZone koltime = (TimeZone) TimeZone.getTimeZone( "GMT-0330" );

		KoLConstants.DAILY_FORMAT.setTimeZone( koltime );

		// Reload your settings and determine all the different users which
		// are present in your save state list.

		Preferences.setBoolean( "useDevProxyServer", false );
		Preferences.setBoolean( "relayBrowserOnly", false );

		String actualName;
		String[] pastUsers = StaticEntity.getPastUserList();

		for ( int i = 0; i < pastUsers.length; ++i )
		{
			if ( pastUsers[ i ].startsWith( "devster" ) )
			{
				continue;
			}

			actualName = Preferences.getString( pastUsers[ i ], "displayName" );
			if ( actualName.equals( "" ) )
			{
				actualName = StringUtilities.globalStringReplace( pastUsers[ i ], "_", " " );
			}

			KoLConstants.saveStateNames.add( actualName );
		}

		// Clear out any outdated data files.

		KoLmafia.checkDataOverrides();

		// Change the default look and feel to match the player's
		// preferences. Always do this.

		String defaultLookAndFeel;

		if ( System.getProperty( "os.name" ).startsWith( "Mac" ) || System.getProperty( "os.name" ).startsWith( "Win" ) )
		{
			defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
		}
		else
		{
			defaultLookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
		}

		String lookAndFeel = Preferences.getString( "swingLookAndFeel" );

		if ( lookAndFeel.equals( "" ) )
		{
			lookAndFeel = defaultLookAndFeel;
		}

		UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
		String[] installedLooks = new String[ installed.length ];

		for ( int i = 0; i < installedLooks.length; ++i )
		{
			installedLooks[ i ] = installed[ i ].getClassName();
		}

		boolean foundLookAndFeel = false;
		for ( int i = 0; i < installedLooks.length && !foundLookAndFeel; ++i )
		{
			foundLookAndFeel = installedLooks[ i ].equals( lookAndFeel );
		}

		if ( !foundLookAndFeel )
		{
			lookAndFeel = defaultLookAndFeel;
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

		OSXAdapter.setDockIconImage( JComponentUtilities.getImage( "limeglass.gif" ).getImage() );

		if ( System.getProperty( "os.name" ).startsWith( "Win" ) || lookAndFeel.equals( UIManager.getCrossPlatformLookAndFeelClassName() ) )
		{
			UIManager.put( "ProgressBar.foreground", Color.black );
			UIManager.put( "ProgressBar.selectionForeground", Color.lightGray );

			UIManager.put( "ProgressBar.background", Color.lightGray );
			UIManager.put( "ProgressBar.selectionBackground", Color.black );
		}

		tab.CloseTabPaneEnhancedUI.selectedA = DataUtilities.toColor( Preferences.getString( "innerTabColor" ) );
		tab.CloseTabPaneEnhancedUI.selectedB = DataUtilities.toColor( Preferences.getString( "outerTabColor" ) );

		tab.CloseTabPaneEnhancedUI.notifiedA = DataUtilities.toColor( Preferences.getString( "innerChatColor" ) );
		tab.CloseTabPaneEnhancedUI.notifiedB = DataUtilities.toColor( Preferences.getString( "outerChatColor" ) );

		if ( !KoLmafia.acquireFileLock( "1" ) && !KoLmafia.acquireFileLock( "2" ) )
		{
			System.exit( -1 );
		}

		FlaggedItems.initializeLists();

		// Create a script directory if necessary
		KoLConstants.SCRIPT_LOCATION.mkdirs();

		// Now run the main routines for each, so that
		// you have an interface.

		if ( useGUI )
		{
			KoLmafiaGUI.initialize();
		}
		else
		{
			StaticEntity.setClient( KoLmafiaCLI.DEFAULT_SHELL );
			RequestLogger.openStandard();
		}

		// Now, maybe the person wishes to run something
		// on startup, and they associated KoLmafia with
		// some non-ASH file extension. This will run it.

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

	private static final void checkDataOverrides()
	{
		String lastVersion = Preferences.getString( "previousUpdateVersion" );
		String currentVersion = StaticEntity.getVersion();

		int currentRevision = StaticEntity.getRevision();

		String message = null;

		if ( lastVersion == null || lastVersion.equals( "" ) )
		{
			message = "Clearing data overrides: initializing from " + currentVersion;
		}
		else if ( !lastVersion.equals( KoLConstants.VERSION_NAME ) )
		{
			message = "Clearing data overrides: upgrade from " + lastVersion + " to " + currentVersion;
		}

		// Save revision, just for fun, but do not clear override files
		// for minor version upgrades.

		Preferences.setString( "previousUpdateVersion", KoLConstants.VERSION_NAME );
		Preferences.setInteger( "previousUpdateRevision", currentRevision );

		if ( message == null )
		{
			return;
		}

		RequestLogger.printLine( message );

		for ( int i = 0; i < KoLConstants.OVERRIDE_DATA.length; ++i )
		{
			File outdated = new File( UtilityConstants.DATA_LOCATION, KoLConstants.OVERRIDE_DATA[ i ] );
			if ( outdated.exists() )
			{
				outdated.delete();
			}
		}
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object. All data fields are
	 * initialized to their default values, the global settings are loaded
	 * from disk.
	 */

	public KoLmafia()
	{
	}

	public static final boolean isSessionEnding()
	{
		return KoLmafia.SESSION_ENDING;
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
		KoLmafia.updateDisplay( MafiaState.CONTINUE, message );
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code> session.
	 */

	public static final void updateDisplay( final MafiaState state, final String message )
	{
		if ( KoLmafia.continuationState == MafiaState.ABORT && state != MafiaState.ABORT )
		{
			return;
		}

		if ( KoLmafia.continuationState != MafiaState.PENDING )
		{
			KoLmafia.continuationState = state;
		}

		RequestLogger.printLine( state, message );

		if ( KoLmafia.allowDisplayUpdate )
		{
			SystemTrayFrame.updateToolTip( message );
		}

		KoLmafia.lastMessage = message;

		if ( message.indexOf( KoLConstants.LINE_BREAK ) == -1 )
		{
			KoLmafia.updateDisplayState( state, message );
		}
	}

	private static final void updateDisplayState( final MafiaState state, final String message )
	{
		// Update all panels and frames with the message.

		if ( KoLmafia.allowDisplayUpdate )
		{
			String unicodeMessage = StringUtilities.getEntityDecode( message, false );
			ActionPanel[] panels = StaticEntity.getExistingPanels();

			for ( int i = 0; i < panels.length; ++i )
			{
				if ( panels[ i ] instanceof GenericPanel )
				{
					( (GenericPanel) panels[ i ] ).setStatusMessage( unicodeMessage );
				}

				panels[ i ].setEnabled( state != MafiaState.CONTINUE );
			}

			Frame [] frames = Frame.getFrames();
			for ( int i = 0; i < frames.length; ++i )
			{
				if ( frames[ i ] instanceof GenericFrame )
				{
					GenericFrame frame = (GenericFrame) frames[ i ];

					frame.setStatusMessage( unicodeMessage );
					frame.updateDisplayState( state );
				}
			}

			if ( KoLDesktop.instanceExists() )
			{
				KoLDesktop.getInstance().updateDisplayState( state );
			}
		}

		KoLmafia.displayState = state;
	}

	public static final void enableDisplay()
	{
		if ( KoLmafia.continuationState == MafiaState.ABORT || KoLmafia.continuationState == MafiaState.ERROR )
		{
			KoLmafia.updateDisplayState( MafiaState.ERROR, "" );
		}
		else
		{
			KoLmafia.updateDisplayState( MafiaState.ENABLE,	"" );
		}

		KoLmafia.continuationState = MafiaState.CONTINUE;
	}

	public static final void timein( final String name )
	{
		// Save the current user settings to disk
		Preferences.reset( null );

		// Load the JSON string first, so we can use it, if necessary.
		ActionBarManager.loadJSONString();

		// Reload the current user's preferences
		Preferences.reset( name );

		// The password hash changes for each session
		GenericRequest.passwordHash = "";
		PasswordHashRequest request = new PasswordHashRequest( "lchat.php" );
		RequestThread.postRequest(  request );

		// Just in case it's a new day...

		// Close existing session log and reopen it
		RequestLogger.closeSessionLog();
		RequestLogger.openSessionLog();

		// Get current moon phases
		RequestThread.postRequest( new MoonPhaseRequest() );
		KoLCharacter.setHoliday( HolidayDatabase.getHoliday() );
	}

	public static final void resetCounters()
	{
		Preferences.setInteger( "lastCounterDay", HolidayDatabase.getPhaseStep() );

		Preferences.setString( "barrelLayout", "?????????" );
		Preferences.setBoolean( "bootsCharged", false );
		Preferences.setBoolean( "breakfastCompleted", false );
		Preferences.setBoolean( "burrowgrubHiveUsed", false );
		Preferences.setInteger( "burrowgrubSummonsRemaining", 0 );
		Preferences.setInteger( "cocktailSummons", 0 );
		Preferences.setBoolean( "concertVisited", false );
		Preferences.setInteger( "currentFullness", 0 );
		Preferences.setInteger( "currentMojoFilters", 0 );
		Preferences.setInteger( "currentSpleenUse", 0 );
		Preferences.setString( "currentPvpVictories", "" );
		Preferences.setBoolean( "dailyDungeonDone", false );
		Preferences.setBoolean( "demonSummoned", false );
		Preferences.setBoolean( "expressCardUsed", false );
		Preferences.setInteger( "extraRolloverAdventures", 0 );
		Preferences.setBoolean( "friarsBlessingReceived", false );
		Preferences.setInteger( "grimoire1Summons", 0 );
		Preferences.setInteger( "grimoire2Summons", 0 );
		Preferences.setInteger( "grimoire3Summons", 0 );
		Preferences.setInteger( "lastBarrelSmashed", 0 );
		Preferences.setInteger( "libramSummons", 0 );
		Preferences.setBoolean( "libraryCardUsed", false );
		Preferences.setInteger( "noodleSummons", 0 );
		Preferences.setInteger( "nunsVisits", 0 );
		Preferences.setBoolean( "oscusSodaUsed", false );
		Preferences.setBoolean( "outrageousSombreroUsed", false );
		Preferences.setInteger( "pastamancerGhostSummons", 0 );
		Preferences.setInteger( "prismaticSummons", 0 );
		Preferences.setBoolean( "rageGlandVented", false );
		Preferences.setInteger( "reagentSummons", 0 );
		Preferences.setString( "romanticTarget", "" );
		Preferences.setInteger( "seaodesFound", 0 );
		Preferences.setBoolean( "spiceMelangeUsed", false );
		Preferences.setInteger( "spookyPuttyCopiesMade", 0 );
		Preferences.setBoolean( "styxPixieVisited", false );
		Preferences.setBoolean( "telescopeLookedHigh", false );
		Preferences.setInteger( "tempuraSummons", 0 );
		Preferences.setInteger( "timesRested", 0 );
		Preferences.setInteger( "tomeSummons", 0 );

		Preferences.resetDailies();
		ConsequenceManager.updateOneDesc();

		// Libram summoning skills now costs 1 MP again
		KoLConstants.summoningSkills.sort();
		KoLConstants.usableSkills.sort();

		// Remove Wandering Monster counters
		TurnCounter.stopCounting( "Romantic Monster window begin" );
		TurnCounter.stopCounting( "Romantic Monster window end" );
		TurnCounter.stopCounting( "Holiday Monster window begin" );
		TurnCounter.stopCounting( "Holiday Monster window end" );
	}

	public static void refreshSession()
	{
		KoLmafia.refreshSessionData();

		// Check to see if you need to reset the counters.

		boolean shouldResetCounters = false;

		int today = HolidayDatabase.getPhaseStep();

		if ( Preferences.getInteger( "lastCounterDay" ) != today )
		{
			shouldResetCounters = true;
		}

		int ascensions = KoLCharacter.getAscensions();
		int knownAscensions = Preferences.getInteger( "knownAscensions" );

		if ( ascensions != 0 && knownAscensions != -1 && knownAscensions != ascensions )
		{
			Preferences.setInteger( "knownAscensions", ascensions );
			ValhallaManager.resetPerAscensionCounters();
			shouldResetCounters = true;
		}
		else if ( knownAscensions == -1 )
		{
			Preferences.setInteger( "knownAscensions", ascensions );
		}

		if ( shouldResetCounters )
		{
			KoLmafia.resetCounters();
		}
	}

	private static void refreshSessionData()
	{
		KoLmafia.isRefreshing = true;

		KoLmafia.updateDisplay( "Refreshing session data..." );

		// Load saved counters before any requests are made, since both
		// charpane and charsheet requests can set them.

		CharPaneRequest.reset();
		KoLCharacter.setCurrentRun( 0 );
		TurnCounter.loadCounters();

		// Get current moon phases

		RequestThread.postRequest( new MoonPhaseRequest() );
		KoLCharacter.setHoliday( HolidayDatabase.getHoliday() );

		// Forget what is trendy
		TrendyRequest.reset();

		// Start out fetching the status using the KoL API. This
		// provides data from a lot of different standard pages

		// We are in Valhalla if this redirects to afterlife.php
		GenericRequest request = new ApiRequest( "status" );
		RequestThread.postRequest( request );
		if ( request.redirectLocation != null && request.redirectLocation.startsWith( "afterlife.php" ) )
		{
			// In Valhalla, parse the CharPane and abort further processing
			KoLmafia.updateDisplay( "Welcome to Valhalla!" );
			RequestThread.postRequest( new CharPaneRequest() );
			KoLmafia.isRefreshing = false;
			return;
		}

		// Retrieve the character sheet. It's necessary to do this
		// before concoctions have a chance to get refreshed.

		request = new CharSheetRequest();
		RequestThread.postRequest( request );

		// If you get redirected on the request for the character sheet,
		// don't make any more requests.

		if ( request.redirectLocation != null )
		{
			return;
		}

		// Now that we know the character's ascension count, reset
		// anything that depends on that.

		KoLCharacter.resetPerAscensionData();

		// Hermit items depend on character class
		HermitRequest.initialize();

		// Retrieve the contents of the closet and inventory.  We can
		// detect new items in either location, so let the Inventory
		// Manager control refreshing.
		InventoryManager.refresh();

		// Retrieve Custom Outfit list

		RequestThread.postRequest( new CustomOutfitRequest() );

		// Look at the Quest Log

		RequestThread.postRequest( new QuestLogRequest() );

		// if the Cyrpt quest is active, force evilometer refresh
		// (if we don't know evil levels already)
		if ( Preferences.getString( Quest.CYRPT.getPref() ).equals( QuestDatabase.STARTED )
			|| Preferences.getString( Quest.CYRPT.getPref() ).indexOf( "step" ) != -1 )
		{
			if ( Preferences.getInteger( "cyrptTotalEvilness" ) == 0 )
			{
				RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.EVILOMETER ) );
			}
		}

		// Retrieve the Terrarium

		RequestThread.postRequest( new FamiliarRequest() );

		// Retrieve campground data to see if the user has box servants
		// or a bookshelf

		KoLmafia.updateDisplay( "Retrieving campground data..." );
		CampgroundRequest.reset();
		RequestThread.postRequest( new CampgroundRequest( "inspectdwelling" ) );
		RequestThread.postRequest( new CampgroundRequest( "inspectkitchen" ) );
		KoLCharacter.checkTelescope();

		if ( Preferences.getInteger( "lastEmptiedStorage" ) != KoLCharacter.getAscensions() )
		{
			RequestThread.postRequest( new StorageRequest() );
			CafeRequest.pullLARPCard();
		}

		if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.KEYOTRON, 1 ) ) && Preferences.getInteger( "lastKeyotronUse" ) != KoLCharacter.getAscensions() )
		{
			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.KEYOTRON ) );
		}

		// If we have a Crown of Thrones available and it's not
		// equipped, see which familiar is sitting in it, if any.
		InventoryManager.checkCrownOfThrones();

		// Make sure that we know about the easy to see Golden Mr. A's, at least
		InventoryManager.countGoldenMrAccesories();

		KoLmafia.updateDisplay( "Session data refreshed." );

		KoLmafia.isRefreshing = false;

		ConcoctionDatabase.refreshConcoctions( true );

		// Visit lounge and report on whether you have a present waiting
		ClanLoungeRequest.visitLounge();
	}

	public static final boolean isRefreshing()
	{
		return KoLmafia.isRefreshing;
	}

	public static final void setIsRefreshing( final boolean isRefreshing )
	{
		KoLmafia.isRefreshing = isRefreshing;
	}

	public static final void resetAfterAvatar()
	{
		KoLmafia.isRefreshing = true;

		// Start out fetching the status using the KoL API. This
		// provides data from a lot of different standard pages

		GenericRequest request = new ApiRequest( "status" );
		RequestThread.postRequest( request );

		// Retrieve the character sheet. It's necessary to do this
		// before concoctions have a chance to get refreshed.

		// Clear skills first, since we no longer know Boris skills
		KoLCharacter.resetSkills();

		request = new CharSheetRequest();
		RequestThread.postRequest( request );

		// Clear preferences
		Preferences.setString( "banishingShoutMonsters", "" );

		// Hermit items depend on character class
		HermitRequest.initialize();

		// Retrieve the contents of the inventory, since quest items
		// may disappear.
		InventoryManager.refresh();

		// Retrieve the Terrarium

		RequestThread.postRequest( new FamiliarRequest() );

		// Retrieve the bookshelf
		RequestThread.postRequest( new CampgroundRequest( "bookshelf" ) );

		KoLmafia.isRefreshing = false;

		// Finally, update available concoctions
		ConcoctionDatabase.resetQueue();
		ConcoctionDatabase.refreshConcoctions( true );

		// Now we can finally run the player's kingLiberatedScript
		if ( KoLCharacter.kingLiberated() )
		{
			// Run a user-supplied script
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( Preferences.getString( "kingLiberatedScript" ) );
		}
	}

	/**
	 * Used to reset the session tally to its original values.
	 */

	public static void resetSession()
	{
		KoLConstants.encounterList.clear();
		KoLConstants.adventureList.clear();

		KoLmafia.initialStats[ 0 ] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() );
		KoLmafia.initialStats[ 1 ] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() );
		KoLmafia.initialStats[ 2 ] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() );

		AdventureResult.SESSION_FULLSTATS[ 0 ] = 0;
		AdventureResult.SESSION_FULLSTATS[ 1 ] = 0;
		AdventureResult.SESSION_FULLSTATS[ 2 ] = 0;

		AdventureResult.SESSION_SUBSTATS[ 0 ] = 0;
		AdventureResult.SESSION_SUBSTATS[ 1 ] = 0;
		AdventureResult.SESSION_SUBSTATS[ 2 ] = 0;

		KoLConstants.tally.clear();
		KoLConstants.tally.add( new AdventureResult( AdventureResult.ADV ) );
		KoLConstants.tally.add( new AdventureResult( AdventureResult.MEAT ) );
		KoLConstants.tally.add( AdventureResult.SESSION_SUBSTATS_RESULT );
		KoLConstants.tally.add( AdventureResult.SESSION_FULLSTATS_RESULT );
	}

	public static final void saveDataOverride()
	{
		if ( ItemDatabase.newItems )
		{
			ItemDatabase.writeTradeitems( new File( UtilityConstants.DATA_LOCATION, "tradeitems.txt" ) );
			ItemDatabase.writeItemdescs( new File( UtilityConstants.DATA_LOCATION, "itemdescs.txt" ) );
		}

		if ( EquipmentDatabase.newEquipment )
		{
			EquipmentDatabase.writeEquipment( new File( UtilityConstants.DATA_LOCATION, "equipment.txt" ) );
		}

		if ( EffectDatabase.newEffects )
		{
			EffectDatabase.writeEffects( new File( UtilityConstants.DATA_LOCATION, "statuseffects.txt" ) );
		}

		if ( ItemDatabase.newItems || EquipmentDatabase.newEquipment || EffectDatabase.newEffects)
		{
			Modifiers.writeModifiers( new File( UtilityConstants.DATA_LOCATION, "modifiers.txt" ) );
		}

		if ( FamiliarDatabase.newFamiliars )
		{
			FamiliarDatabase.writeFamiliars( new File( UtilityConstants.DATA_LOCATION, "familiars.txt" ) );
		}
	}

	/**
	 * Adds the recent effects accumulated so far to the actual effects. This should be called after the previous
	 * effects were decremented, if adventuring took place.
	 */

	public static final void applyEffects()
	{
		boolean concoctionRefreshNeeded = false;

		int oldCount = KoLConstants.activeEffects.size();

		for ( int j = 0; j < KoLConstants.recentEffects.size(); ++j )
		{
			AdventureResult effect = (AdventureResult) KoLConstants.recentEffects.get( j );
			AdventureResult.addResultToList( KoLConstants.activeEffects, effect );

			if ( effect.getName().equals( Effect.INIGO.effectName() ) )
			{
				concoctionRefreshNeeded = true;
			}
		}

		KoLConstants.recentEffects.clear();
		KoLConstants.activeEffects.sort();

		if ( oldCount != KoLConstants.activeEffects.size() )
		{
			KoLCharacter.updateStatus();
		}

		if ( concoctionRefreshNeeded )
		{
			ConcoctionDatabase.setRefreshNeeded( true );
		}
	}

	/**
	 * Makes the given request for the given number of iterations, or until
	 * continues are no longer possible, either through user cancellation
	 * or something occuring which prevents the requests from resuming.
	 *
	 * @param request The request made by the user
	 * @param iterations The number of times the request should be repeated
	 */

	public static void makeRequest( final Runnable request, final int iterations )
	{
		try
		{
			boolean wasAdventuring = KoLmafia.isAdventuring;

			if ( request instanceof KoLAdventure )
			{
				KoLmafia.currentAdventure = (KoLAdventure) request;

				if ( KoLmafia.currentAdventure.getRequest() instanceof ClanRumpusRequest )
				{
					RequestThread.postRequest( ( (ClanRumpusRequest) KoLmafia.currentAdventure.getRequest() ).setTurnCount( iterations ) );
					return;
				}
				if ( KoLmafia.currentAdventure.getRequest() instanceof RichardRequest )
				{
					RequestThread.postRequest( ( (RichardRequest) KoLmafia.currentAdventure.getRequest() ).setTurnCount( iterations ) );
					return;
				}

				if ( KoLCharacter.getCurrentHP() == 0 && !KoLmafia.currentAdventure.isNonCombatsOnly() )
				{
					RecoveryManager.recoverHP();
				}

				if ( !KoLmafia.permitsContinue() )
				{
					return;
				}

				KoLmafia.isAdventuring = true;
				if ( !wasAdventuring )
				{
					SpecialOutfit.createImplicitCheckpoint();
				}
			}

			KoLmafia.executeRequest( request, iterations, wasAdventuring );

			if ( request instanceof KoLAdventure && !wasAdventuring )
			{
				KoLmafia.isAdventuring = false;
				if ( RecoveryManager.isRecoveryPossible() )
				{
					RecoveryManager.runBetweenBattleChecks( false );
				}
				SpecialOutfit.restoreImplicitCheckpoint();
			}
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static boolean executeAfterAdventureScript()
	{
		return KoLmafia.executeScript( Preferences.getString( "afterAdventureScript" ) );
	}

	public static boolean executeBeforePVPScript()
	{
		return KoLmafia.executeScript( Preferences.getString( "beforePVPScript" ) );
	}

	public static boolean executeScript( final String scriptPath )
	{
		if ( !scriptPath.equals( "" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptPath );
			return true;
		}
		return false;
	}

	private static boolean handleConditions( final AdventureResult[] items, final CreateItemRequest[] creatables )
	{
		if ( items.length == 0 )
		{
			return false;
		}

		if ( !GoalManager.hasGoals() )
		{
			return true;
		}

		boolean shouldCreate = false;

		for ( int i = 0; i < creatables.length && !shouldCreate; ++i )
		{
			shouldCreate = creatables[ i ] != null && creatables[ i ].getQuantityPossible() >= items[ i ].getCount();
		}

		// In theory, you could do a real validation by doing a full
		// dependency search. While that's technically better, it's
		// also not very useful.

		for ( int i = 0; i < creatables.length && shouldCreate; ++i )
		{
			shouldCreate = creatables[ i ] == null || creatables[ i ].getQuantityPossible() >= items[ i ].getCount();
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

		return !GoalManager.hasGoals();
	}

	public static void abortAfter( String msg )
	{
		KoLmafia.abortAfter = msg;
	}

	private static void executeRequest( final Runnable request, final int totalIterations, final boolean wasAdventuring )
	{
		Interpreter.forgetPendingState();

		// Begin the adventuring process, or the request execution
		// process (whichever is applicable).

		boolean isAdventure = request instanceof KoLAdventure;

		List goals = new ArrayList( GoalManager.getGoals() );

		boolean deferConcoctionRefresh = true;

		AdventureResult[] items = new AdventureResult[ goals.size() ];
		CreateItemRequest[] creatables = new CreateItemRequest[ goals.size() ];

		for ( int i = 0; i < goals.size(); ++i )
		{
			items[ i ] = (AdventureResult) goals.get( i );
			creatables[ i ] = CreateItemRequest.getInstance( items[ i ] );

			if ( ConcoctionDatabase.getMixingMethod( items[ i ] ) != KoLConstants.NOCREATE )
			{
				deferConcoctionRefresh = false;
			}
		}

		KoLmafia.forceContinue();
		KoLmafia.abortAfter = null;

		boolean checkBounty = false;
		AdventureResult bounty = null;

		if ( isAdventure && ( bounty = AdventureDatabase.currentBounty() ) != null )
		{
			AdventureResult ar = AdventureDatabase.getBounty( (KoLAdventure) request );
			checkBounty = ar != null && bounty.getItemId() == ar.getItemId();
		}

		if ( deferConcoctionRefresh )
		{
			ConcoctionDatabase.deferRefresh( true );
		}

		int currentIteration = 0;

		while ( KoLmafia.permitsContinue() && ++currentIteration <= totalIterations )
		{
			int runBeforeRequest = KoLCharacter.getCurrentRun();
			KoLmafia.tookChoice = false;

			KoLmafia.executeRequestOnce( request, wasAdventuring, currentIteration, totalIterations, items, creatables );

			if ( isAdventure && KoLmafia.redoSkippedAdventures &&
			     runBeforeRequest == KoLCharacter.getCurrentRun() )
			{
				--currentIteration;
			}

			if ( checkBounty && bounty.getCount( KoLConstants.inventory ) == bounty.getCount() )
			{
				RequestThread.postRequest( new BountyHunterHunterRequest() );
				checkBounty = false;
			}
		}

		if ( deferConcoctionRefresh )
		{
			ConcoctionDatabase.deferRefresh( false );
		}


		if ( isAdventure )
		{
			AdventureFrame.updateRequestMeter( 1, 1 );
		}

		// If you've completed the requests, make sure to update
		// the display.

		if ( KoLmafia.permitsContinue() && RecoveryManager.isRecoveryPossible() )
		{
			if ( isAdventure && GoalManager.hasGoals() )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR,
					"Conditions not satisfied after " + ( currentIteration - 1 ) + ( currentIteration == 2 ? " adventure." : " adventures." ) );
			}
		}
		else if ( KoLmafia.continuationState == MafiaState.PENDING )
		{
			Interpreter.rememberPendingState();
			KoLmafia.forceContinue();
		}
	}

	private static void executeAdventureOnce( final KoLAdventure adventure, boolean wasAdventuring,
		final int currentIteration, final int totalIterations, final AdventureResult[] items,
		final CreateItemRequest[] creatables )
	{
		if ( KoLCharacter.getAdventuresLeft() == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.PENDING, "Ran out of adventures." );
			return;
		}

		if ( KoLmafia.handleConditions( items, creatables ) )
		{
			KoLmafia.updateDisplay(
				MafiaState.PENDING, "Conditions satisfied after " + currentIteration + " adventures." );
			return;
		}

		if ( KoLCharacter.isFallingDown() )
		{
			String holiday = HolidayDatabase.getHoliday();

			if ( adventure.getRequest().getPath().startsWith( "trickortreat" ) )
			{
				// You're allowed to trick or treat even when falling down drunk,
				// so ignore any problems in this case.
			}
			else if ( holiday.indexOf( "St. Sneaky Pete's Day" ) == -1 && holiday.indexOf( "Drunksgiving" ) == -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You are too drunk to continue." );
				return;
			}
			else if ( KoLCharacter.getInebriety() <= 25 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You are not drunk enough to continue." );
				return;
			}
		}

		if ( KoLmafia.abortAfter != null )
		{
			KoLmafia.updateDisplay( MafiaState.PENDING, KoLmafia.abortAfter );
			return;
		}

		// Otherwise, disable the display and update the user
		// and the current request number. Different requests
		// have different displays. They are handled here.

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

		KoLmafia.executeAfterAdventureScript();

		if ( KoLmafia.handleConditions( items, creatables ) )
		{
			KoLmafia.updateDisplay(
				MafiaState.PENDING, "Conditions satisfied after " + currentIteration + " adventures." );
			return;
		}
	}

	private static void executeRequestOnce( final Runnable request, final boolean wasAdventuring, final int currentIteration,
		final int totalIterations, final AdventureResult[] items, final CreateItemRequest[] creatables )
	{
		if ( request instanceof KoLAdventure )
		{
			KoLmafia.executeAdventureOnce(
				(KoLAdventure) request, wasAdventuring, currentIteration, totalIterations, items, creatables );
			return;
		}

		if ( request instanceof CampgroundRequest )
		{
			KoLmafia.updateDisplay( "Campground request " + currentIteration + " of " + totalIterations + " in progress..." );
		}

		RequestLogger.printLine();
		RequestThread.postRequest( (GenericRequest) request );
		RequestLogger.printLine();
	}

	public static void protectClovers()
	{
		if ( KoLCharacter.inBeecore() )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "closet", "put * ten-leaf clover" );
		}
		else
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "use", "* ten-leaf clover" );
		}
	}

	/**
	 * Show an HTML string to the user
	 */

	public abstract void showHTML( String location, String text );

	/**
	 * Retrieves whether or not continuation of an adventure or request is permitted by the or by current circumstances
	 * in-game.
	 *
	 * @return <code>true</code> if requests are allowed to continue
	 */

	public static final boolean permitsContinue()
	{
		return KoLmafia.continuationState == MafiaState.CONTINUE;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request will be denied by the regardless of continue
	 * state reset, until the display is enable (ie: in an abort state).
	 *
	 * @return <code>true</code> if requests are allowed to continue
	 */

	public static final boolean refusesContinue()
	{
		return KoLmafia.continuationState == MafiaState.ABORT;
	}

	/**
	 * Forces a continue state. This should only be called when there is no doubt that a continue should occur.
	 *
	 * @return <code>true</code> if requests are allowed to continue
	 */

	public static final void forceContinue()
	{
		KoLmafia.continuationState = MafiaState.CONTINUE;
	}

	/**
	 * Utility. This method used to decode a saved password. This should be called whenever a new password intends to be
	 * stored in the global file.
	 */

	public static final void addSaveState( final String username, final String password )
	{
		String utfString = StringUtilities.getURLEncode( password );

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
				encodedString.append( Integer.toHexString( currentCharacter ).toUpperCase() );
				break;
			}
		}

		Preferences.setString( username, "saveState", ( new BigInteger( encodedString.toString(), 36 ) ).toString( 10 ) );
		if ( !KoLConstants.saveStateNames.contains( username ) )
		{
			KoLConstants.saveStateNames.add( username );
		}
	}

	public static final void removeSaveState( final String loginname )
	{
		if ( loginname == null )
		{
			return;
		}

		KoLConstants.saveStateNames.remove( loginname );
		Preferences.setString( loginname, "saveState", "" );
	}

	/**
	 * Utility. The method used to decode a saved password. This should be called whenever a new password intends to be
	 * stored in the global file.
	 */

	public static final String getSaveState( final String loginname )
	{
		String password = Preferences.getString( loginname, "saveState" );
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

		return StringUtilities.getURLDecode( utfString.toString() );
	}

	public static final boolean checkRequirements( final List<AdventureResult> requirements )
	{
		return KoLmafia.checkRequirements( requirements, true );
	}

	public static final boolean checkRequirements( final List<AdventureResult> requirements, final boolean retrieveItem )
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
				InventoryManager.retrieveItem( requirementsArray[ i ] );
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
		// be sure to return false. Otherwise,
		// you managed to get everything.

		return requirements.isEmpty();
	}

	/**
	 * Utility method used to purchase the given number of items from the mall using the given purchase requests.
	 */

	public static void makePurchases( final List results, final Object[] purchases, final int maxPurchases,
		final boolean isAutomated )
	{
		KoLmafia.makePurchases( results, purchases, maxPurchases, isAutomated, 0 );
	}

	public static void makePurchases( final List results, final Object[] purchases, final int maxPurchases,
		final boolean isAutomated, final int priceLimit )
	{
		if ( purchases.length == 0 )
		{
			return;
		}

		for ( int i = 0; i < purchases.length; ++i )
		{
			if ( !( purchases[ i ] instanceof PurchaseRequest ) )
			{
				return;
			}
		}

		PurchaseRequest currentRequest = (PurchaseRequest) purchases[ 0 ];
		int currentPrice = currentRequest.getPrice();

		int itemId = currentRequest.getItemId();

		if ( itemId == ItemPool.TEN_LEAF_CLOVER && InventoryManager.cloverProtectionActive() && !KoLCharacter.inBeecore() )
		{
			// Our clovers will miraculously turn into disassembled
			// clovers as soon as they are bought.

			itemId = ItemPool.DISASSEMBLED_CLOVER;
		}

		AdventureResult itemToBuy = ItemPool.get( itemId, 0 );
		int initialCount = itemToBuy.getCount( KoLConstants.inventory );
		int currentCount = initialCount;
		int desiredCount = maxPurchases == Integer.MAX_VALUE ? Integer.MAX_VALUE : initialCount + maxPurchases;

		int previousLimit = 0;

		if ( Preferences.getInteger( "autoBuyPriceLimit" ) == 0 )
		{
			// this is probably due to an out-of-date defaults.txt
			Preferences.setInteger( "autoBuyPriceLimit", 20000 );
		}

		for ( int i = 0; i < purchases.length && currentCount < desiredCount && KoLmafia.permitsContinue(); ++i )
		{
			currentRequest = (PurchaseRequest) purchases[ i ];
			currentPrice = currentRequest.getPrice();

			if ( currentRequest.getQuantity() != PurchaseRequest.MAX_QUANTITY )
			{
				if ( !KoLCharacter.canInteract() || isAutomated && !Preferences.getBoolean( "autoSatisfyWithMall" ) )
				{
					continue;
				}
			}

			if ( ( priceLimit > 0 && currentPrice > priceLimit ) ||
				( isAutomated && currentPrice > Preferences.getInteger( "autoBuyPriceLimit" ) ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR,
					"Stopped purchasing " + currentRequest.getItemName() + " @ " + KoLConstants.COMMA_FORMAT.format( currentPrice ) + "." );

				return;
			}

			// Keep track of how many of the item you had before
			// you run the purchase request

			previousLimit = currentRequest.getLimit();
			currentRequest.setLimit( Math.min(
				KoLCharacter.getAvailableMeat() / currentPrice,
				Math.min( previousLimit, desiredCount - currentCount ) ) );
			RequestThread.postRequest( currentRequest );

			// Now that you have already made a purchase from the
			// store, remove the purchase from the list!

			if ( KoLmafia.permitsContinue() )
			{
				if ( currentRequest.getQuantity() == currentRequest.getLimit() )
				{
					results.remove( currentRequest );
				}
				else if ( currentRequest.getQuantity() == PurchaseRequest.MAX_QUANTITY )
				{
					currentRequest.setLimit( PurchaseRequest.MAX_QUANTITY );
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

		if ( currentCount >= desiredCount || maxPurchases == Integer.MAX_VALUE )
		{
			KoLmafia.updateDisplay( "Purchases complete." );
		}
		else
		{
			KoLmafia.updateDisplay( "Desired purchase quantity not reached (wanted " + maxPurchases + ", got " + ( currentCount - initialCount ) + ")" );
		}
	}

	/**
	 * Utility method used to register a given adventure in the running adventure summary.
	 */

	public void registerAdventure( final KoLAdventure adventureLocation )
	{
		KoLmafia.registerAdventure( adventureLocation.getAdventureName() );
	}

	public static void registerAdventure( final String adventureName )
	{
		if ( adventureName == null )
		{
			return;
		}

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

		if ( BadMoonManager.specialAdventure( encounterName ) )
		{
			return KoLmafia.BADMOON;
		}

		return KoLmafia.NONE;
	}

	public static final boolean isAutoStop( final String encounterName )
	{
		if ( encounterName.equals( "Under the Knife" ) && Preferences.getString( "choiceAdventure21" ).equals( "2" ) )
		{
			return false;
		}

		String encounterType = KoLmafia.encounterType( encounterName );
		return encounterType == KoLmafia.STOP ||
		       encounterType == KoLmafia.GLYPH ||
		       encounterType == KoLmafia.BADMOON;
	}

	// Used to ignore special monsters re-encountered via copying
	public static boolean ignoreSpecialMonsters = false;

	public static void ignoreSpecialMonsters()
	{
		KoLmafia.ignoreSpecialMonsters = true;
	}

	private static void recognizeEncounter( final String encounterName, final String responseText )
	{
		String encounterType = KoLmafia.encounterType( encounterName );

		// You stop for a moment to catch your breath, and possibly a
		// cold, and hear a wolf whistle from behind you. You spin
		// around and see <monster> that looks suspiciously like the
		// ones you shot with a love arrow earlier.

		if ( encounterType == KoLmafia.SEMIRARE &&
		     !KoLmafia.ignoreSpecialMonsters &&
		     responseText.indexOf( "hear a wolf whistle" ) == -1 )
		{
			KoLCharacter.registerSemirare();
			return;
		}

		if ( encounterType == KoLmafia.NONE )
		{
			return;
		}

		if ( encounterType == KoLmafia.BADMOON )
		{
			BadMoonManager.registerAdventure( encounterName );
		}

		if ( encounterType == KoLmafia.STOP || encounterType == KoLmafia.GLYPH || encounterType == KoLmafia.BADMOON )
		{
			GoalManager.checkAutoStop( encounterName );
		}
	}

	/**
	 * Utility. The method used to register a given encounter in the running adventure summary.
	 */

	public static void registerEncounter( String encounterName, final String encounterType, final String responseText )
	{
		encounterName = encounterName.trim();

		KoLmafia.handleSpecialEncounter( encounterName, responseText );
		KoLmafia.recognizeEncounter( encounterName, responseText );

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

		KoLConstants.encounterList.add( new RegisteredEncounter( encounterType, encounterName ) );
	}

	public static void handleSpecialEncounter( final String encounterName, final String responseText )
	{
		if ( encounterName.equalsIgnoreCase( "Cheetahs Never Lose" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.BAG_OF_CATNIP ) )
			{
				ResultProcessor.processItem( ItemPool.BAG_OF_CATNIP, -1 );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Summer Holiday" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.HANG_GLIDER ) )
			{
				ResultProcessor.processItem( ItemPool.HANG_GLIDER, -1 );
				QuestDatabase.setQuestProgress( Quest.CITADEL, "step5" );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Step Up to the Table, Put the Ball in Play" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.CARONCH_DENTURES ) )
			{
				ResultProcessor.processItem( ItemPool.CARONCH_DENTURES, -1 );
				QuestDatabase.setQuestIfBetter( Quest.PIRATE, "step4" );
			}

			if ( InventoryManager.hasItem( ItemPool.FRATHOUSE_BLUEPRINTS ) )
			{
				ResultProcessor.processItem( ItemPool.FRATHOUSE_BLUEPRINTS, -1 );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "No Colors Anymore" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.STONE_ROSE ) )
			{
				ResultProcessor.processItem( ItemPool.STONE_ROSE, -1 );
			}
			if ( InventoryManager.hasItem( ItemPool.BLACK_PAINT ) )
			{
				ResultProcessor.processItem( ItemPool.BLACK_PAINT, -1 );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Still No Colors Anymore" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.BLACK_PAINT ) )
			{
				ResultProcessor.processItem( ItemPool.BLACK_PAINT, -1 );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Granny, Does Your Dogfish Bite?" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.GRANDMAS_MAP ) )
			{
				ResultProcessor.processItem( ItemPool.GRANDMAS_MAP, -1 );
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Meat For Nothing and the Harem for Free" ) )
		{
			Preferences.setBoolean( "_treasuryEliteMeatCollected", true );
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Finally, the Payoff" ) )
		{
			Preferences.setBoolean( "_treasuryHaremMeatCollected", true );
			return;
		}
	}

	private static class RegisteredEncounter
		implements Comparable
	{
		private final String type;
		private final String name;
		private final String stringform;
		private int encounterCount;

		public RegisteredEncounter( final String type, final String name )
		{
			this.type = type;
			// The name is likely a substring of a page load, so storing it
			// as-is would keep the entire page in memory.
			this.name = new String( name );

			this.stringform = type == null ? name : type + ": " + name;
			this.encounterCount = 1;
		}

		@Override
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
				ContactManager.getPlayerId( targets[ i ] ) == null ? targets[ i ] : ContactManager.getPlayerId( targets[ i ] );
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
				ContactManager.getPlayerName( targets[ i ] ) == null ? targets[ i ] : ContactManager.getPlayerName( targets[ i ] );
		}

		// Sort the list one more time, this time
		// by player name.

		Arrays.sort( targets );

		// Parsing complete. Return the list of
		// unique targets.

		return targets;
	}

	public final void deleteAdventureOverride()
	{
		for ( int i = 0; i < KoLConstants.OVERRIDE_DATA.length; ++i )
		{
			File dest = new File( UtilityConstants.DATA_LOCATION,
				KoLConstants.OVERRIDE_DATA[ i ] );
			if ( dest.exists() )
			{
				dest.delete();
			}
		}

		KoLmafia.updateDisplay( "Please restart KoLmafia to complete the update." );
	}

	public static void gc()
	{
		int mem1 = (int) ( Runtime.getRuntime().freeMemory() >> 10 );
		System.gc();
		int mem2 = (int) ( Runtime.getRuntime().freeMemory() >> 10 );
		RequestLogger.printLine( "Reclaimed " + ( mem2 - mem1 ) + " KB of memory" );
	}

	public static final boolean isAdventuring()
	{
		return KoLmafia.isAdventuring;
	}

	public static String whoisPlayer( final String player )
	{
		GenericRequest request = new GenericRequest( "submitnewchat.php" );
		request.addFormField( "playerid", String.valueOf( KoLCharacter.getUserId() ) );
		request.addFormField( "pwd" );
		request.addFormField( "graf", "/whois " + player );

		RequestThread.postRequest( request );
		return request.responseText;
	}

	public static boolean isPlayerOnline( final String player )
	{
		// This player is currently online.
		// This player is currently online in channel clan.
		// This player is currently away from KoL in channel trade and listening to clan.
		String text = KoLmafia.whoisPlayer( player );
		return text != null && text.indexOf( "This player is currently" ) != -1;
	}

	private static class UpdateCheckThread
		extends Thread
	{
		public UpdateCheckThread()
		{
			super( "UpdateCheckThread" );
		}

		@Override
		public void run()
		{
			if ( KoLConstants.VERSION_NAME.startsWith( "KoLmafia r" ) )
			{
				return;
			}

			long lastUpdate = Long.parseLong( Preferences.getString( "lastRssUpdate" ) );
			if ( System.currentTimeMillis() - lastUpdate < 86400000L )
			{
				return;
			}

			try
			{
				String line;

				BufferedReader reader =
					FileUtilities.getReader( "http://kolmafia.svn.sourceforge.net/svnroot/kolmafia/src/net/sourceforge/kolmafia/KoLConstants.java" );

				String lastVersion = Preferences.getString( "lastRssVersion" );
				String currentVersion = null;

				while ( ( line = reader.readLine() ) != null )
				{
					if ( line.indexOf( "public static final String VERSION_NAME" ) != -1 )
					{
						int quote1 = line.indexOf( "\"" ) + 1;
						int quote2 = line.lastIndexOf( "\"" );

						currentVersion = line.substring( quote1, quote2 );
					}
				}

				reader.close();

				if ( currentVersion == null )
				{
					return;
				}

				Preferences.setString( "lastRssVersion", currentVersion );

				if ( currentVersion.equals( KoLConstants.VERSION_NAME ) || currentVersion.equals( lastVersion ) )
				{
					return;
				}

				if ( InputFieldUtilities.confirm( "A new version of KoLmafia is now available.  Would you like to download it now?" ) )
				{
					RelayLoader.openSystemBrowser( "http://sourceforge.net/projects/kolmafia/files/" );
				}
			}
			catch ( Exception e )
			{
			}
		}
	}

	public static void about()
	{
		new LicenseDisplayListener().run();
	}

	public static void quit()
	{
		if ( KoLmafia.SESSION_ENDING )
		{
			return;
		}

		KoLmafia.SESSION_ENDING = true;

		LogoutManager.prepare();

		QuitRunnable quitRunnable = new QuitRunnable();

		if ( SwingUtilities.isEventDispatchThread() )
		{
			KoLmafia.updateDisplay( "Logout in progress (interface will be unresponsive)..." );
			KoLmafia.allowDisplayUpdate = false;

			Thread quitThread = new Thread( quitRunnable );
			quitThread.start();

			try
			{
				quitThread.join();
			}
			catch ( InterruptedException e )
			{
			}
		}
		else
		{
			quitRunnable.run();
		}

		System.exit( 0 );
	}

	private static class QuitRunnable
		implements Runnable
	{
		public void run()
		{
			LogoutManager.logout();

			Preferences.reset( null );
			FlaggedItems.saveFlaggedItemList();
			CustomItemDatabase.saveItemData();

			RequestLogger.closeSessionLog();
			RequestLogger.closeDebugLog();
			RequestLogger.closeMirror();

			SystemTrayFrame.removeTrayIcon();
			RelayServer.stop();

			try
			{
				KoLmafia.SESSION_HOLDER.release();
				KoLmafia.SESSION_CHANNEL.close();
				KoLmafia.SESSION_FILE.delete();
			}
			catch ( Exception e )
			{
				// That means the file either doesn't exist or
				// the session holder was somehow closed.
				// Ignore and fall through.
			}
		}
	}

	public static void preferences()
	{
		KoLmafiaGUI.constructFrame( "OptionsFrame" );
	}

	public static void preferencesThreaded()
	{
		RequestThread.runInParallel( new PreferencesRunnable() );
	}

	private static class PreferencesRunnable
		implements Runnable
	{
		public void run()
		{
			KoLmafiaGUI.constructFrame( "OptionsFrame" );
		}
	}
}
