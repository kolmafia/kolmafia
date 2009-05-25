/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.CustomItemDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FlaggedItems;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.AccountRequest;
import net.sourceforge.kolmafia.request.CafeRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CharSheetRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GourdRequest;
import net.sourceforge.kolmafia.request.GuildRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MindControlRequest;
import net.sourceforge.kolmafia.request.MoonPhaseRequest;
import net.sourceforge.kolmafia.request.PasswordHashRequest;
import net.sourceforge.kolmafia.request.PulverizeRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.request.SewerRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.ZapRequest;
import net.sourceforge.kolmafia.session.ActionBarManager;
import net.sourceforge.kolmafia.session.BreakfastManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.DisplayCaseManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.MailManager;
import net.sourceforge.kolmafia.session.MoodManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.ValhallaManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.LoginFrame;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;
import net.sourceforge.kolmafia.swingui.listener.LicenseDisplayListener;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class KoLmafia
{
	private static boolean isRefreshing = false;
	private static boolean isAdventuring = false;
	private static volatile String abortAfter = null;

	public static String lastMessage = "";

	static
	{
		System.setProperty( "sun.java2d.noddraw", "true" );
		System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "KoLmafia" );
		System.setProperty( "com.apple.mrj.application.live-resize", "true" );
		System.setProperty( "com.apple.mrj.application.growbox.intrudes", "false" );

		JEditorPane.registerEditorKitForContentType( "text/html", RequestEditorKit.class.getName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		System.setProperty( "http.referer", "www.kingdomofloathing.com" );
	}

	private static boolean hadPendingState = false;

	protected static String currentIterationString = "";
	public static int adventureGains = 0;
	public static boolean tookChoice = false;
	protected static boolean recoveryActive = false;

	public static boolean isMakingRequest = false;
	public static int continuationState = KoLConstants.CONTINUE_STATE;

	public static final int[] initialStats = new int[ 3 ];

	private static final Pattern TAVERN_PATTERN = Pattern.compile( "rats.php.*where=(\\d+)" );

	private static FileLock SESSION_HOLDER = null;
	private static FileChannel SESSION_CHANNEL = null;
	private static File SESSION_FILE = null;

	private static KoLAdventure currentAdventure;

	// Types of special encounters
	public static final String NONE = "0";
	public static final String STOP = "1";
	public static final String SEMIRARE = "2";
	public static final String GLYPH = "3";

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
		{ "You and the Cap'm Make it Hap'm", STOP },
		{ "This Adventure Bites", STOP },
		{ "It's Always Swordfish", STOP },
		{ "Granny, Does Your Dogfish Bite?", STOP },

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

		{ "The Sleeper Has Awakened", STOP },

		// Adventures that give semirares
		{ "A Menacing Phantom", SEMIRARE },
		{ "A Tight Squeeze", SEMIRARE },
		{ "All The Rave", SEMIRARE },
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
		{ "Two Sizes Too Small", SEMIRARE },
		{ "Yo Ho Ho and a Bottle of Whatever This Is", SEMIRARE },
		{ "You Can Top Our Desserts, But You Can't Beat Our Meats", SEMIRARE },
		{ "A Shark's Chum", SEMIRARE },

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
		boolean useGUI = true;

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
		KoLConstants.hermitItems.add( new AdventureResult( "figurine of an ancient seal", 1, false ) );
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

		Preferences.setInteger(
			"defaultLoginServer", 1 + KoLConstants.RNG.nextInt( GenericRequest.SERVER_COUNT ) );
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

		// Also clear out any outdated data files. Include the
		// adventure table, in case this is causing problems.

		String version = Preferences.getString( "previousUpdateVersion" );

		if ( version == null || !version.equals( KoLConstants.VERSION_NAME ) )
		{
			Preferences.setString( "previousUpdateVersion", KoLConstants.VERSION_NAME );
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
		// preferences. Always do this.

		String lookAndFeel = Preferences.getString( "swingLookAndFeel" );
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

		for ( int i = 0; i < installedLooks.length && !foundLookAndFeel; ++i )
		{
			foundLookAndFeel = installedLooks[ i ].equals( lookAndFeel );
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

		Preferences.setString( "swingLookAndFeel", lookAndFeel );

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
		Runtime.getRuntime().addShutdownHook( new ShutdownThread() );

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
			KoLmafiaCLI.initialize();
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

	private static final void deleteSimulator( final File location )
	{
		if ( location.isDirectory() )
		{
			File[] files = DataUtilities.listFiles( location );
			for ( int i = 0; i < files.length; ++i )
			{
				KoLmafia.deleteSimulator( files[ i ] );
			}
		}

		location.delete();
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object. All data fields are
	 * initialized to their default values, the global settings are loaded
	 * from disk.
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
			KoLmafia.updateDisplayState( state, message );
		}
	}

	private static final void updateDisplayState( final int state, final String message )
	{
		// Next, update all of the panels with the
		// desired update message.

		String unicodeMessage = StringUtilities.getEntityDecode( message, false );
		ActionPanel[] panels = StaticEntity.getExistingPanels();

		for ( int i = 0; i < panels.length; ++i )
		{
			if ( panels[ i ] instanceof GenericPanel )
			{
				( (GenericPanel) panels[ i ] ).setStatusMessage( unicodeMessage );
			}

			panels[ i ].setEnabled( state != KoLConstants.CONTINUE_STATE );
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

	public static final void enableDisplay()
	{
		KoLmafia.updateDisplayState(
			KoLmafia.continuationState == KoLConstants.ABORT_STATE || KoLmafia.continuationState == KoLConstants.ERROR_STATE ? KoLConstants.ERROR_STATE : KoLConstants.ENABLE_STATE,
			"" );
		KoLmafia.continuationState = KoLConstants.CONTINUE_STATE;
	}

	public final void login( final String name )
	{
		RequestThread.openRequestSequence();

		LoginRequest.isLoggingIn( true );
		this.initialize( name );
		LoginRequest.isLoggingIn( false );

		if ( Preferences.getBoolean( name, "getBreakfast" ) )
		{
			int today = HolidayDatabase.getPhaseStep();
			BreakfastManager.getBreakfast( Preferences.getInteger( "lastBreakfast" ) != today );
			Preferences.setInteger( "lastBreakfast", today );
		}

		// Also, do mushrooms, if a mushroom script has already
		// been setup by the user.

		if ( Preferences.getBoolean( "autoPlant" + ( KoLCharacter.canInteract() ? "Softcore" : "Hardcore" ) ) )
		{
			String currentLayout = Preferences.getString( "plantingScript" );
			if ( !currentLayout.equals( "" ) && KoLCharacter.inMuscleSign() && MushroomManager.ownsPlot() )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + KoLConstants.PLOTS_DIRECTORY + currentLayout + ".ash" );
			}
		}

		String scriptSetting = Preferences.getString( "loginScript" );
		if ( !scriptSetting.equals( "" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptSetting );
		}

		RequestThread.closeRequestSequence();
	}

	public final void timein( final String name )
	{
		// Save the current user settings to disk
		Preferences.reset( null );

                // Load the JSON string first, so we can use it, if necessary.
		ActionBarManager.loadJSONString();

                // Reload the current user's preferences
		Preferences.reset( name );

		// The password hash changes for each session
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

	/**
	 * Initializes the <code>KoLmafia</code> session. Called after the
	 * login has been confirmed to notify that the login was successful,
	 * the user-specific settings should be loaded, and the user can begin
	 * adventuring.
	 */

	public void initialize( final String username )
	{
                // Load the JSON string first, so we can use it, if necessary.
		ActionBarManager.loadJSONString();

		// Initialize the variables to their initial states to avoid
		// null pointers getting thrown all over the place

		KoLCharacter.reset( username );

		// Reset all per-player information

		MailManager.clearMailboxes();
		StoreManager.clearCache();
		DisplayCaseManager.clearCache();
		ClanManager.clearCache();
		ItemDatabase.reset();

		CampgroundRequest.reset();
		MushroomManager.reset();
		HermitRequest.reset();

		KoLmafia.updateDisplay( "Initializing session for " + username + "..." );
		Preferences.setString( "lastUsername", username );

		// Perform requests to read current character's data

		this.refreshSession();

		// Reset the session tally and encounter list

		this.resetSession();

		// Open the session log and indicate that we've logged in.

		RequestLogger.openSessionLog();

		if ( Preferences.getBoolean( "logStatusOnLogin" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeCommand( "log", "snapshot" );
		}

		// If the password hash is non-null, then that means you
		// might be mid-transition.

		if ( GenericRequest.passwordHash.equals( "" ) )
		{
			PasswordHashRequest request = new PasswordHashRequest( "lchat.php" );
			RequestThread.postRequest(  request );
			return;
		}

		KoLmafia.registerPlayer( username, String.valueOf( KoLCharacter.getUserId() ) );

		if ( Preferences.getString( "spadingData" ).length() > 10 )
		{
			KoLmafia.updateDisplay( "Some data has been collected that may be of interest " +
				"to others.  Please type `spade' to examine and submit or delete this data." );
		}
	}

	public static final void resetCounters()
	{
		Preferences.setInteger( "lastCounterDay", HolidayDatabase.getPhaseStep() );

		Preferences.setString( "barrelLayout", "?????????" );
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
		Preferences.setInteger( "seaodesFound", 0 );
		Preferences.setBoolean( "spiceMelangeUsed", false );
		Preferences.setInteger( "spookyPuttyCopiesMade", 0 );
		Preferences.setBoolean( "styxPixieVisited", false );
		Preferences.setBoolean( "telescopeLookedHigh", false );
		Preferences.setInteger( "tempuraSummons", 0 );
		Preferences.setInteger( "timesRested", 0 );
		Preferences.setInteger( "tomeSummons", 0 );
		
		Preferences.resetDailies();

		// Libram summoning skills now costs 1 MP again
		KoLConstants.summoningSkills.sort();
		KoLConstants.usableSkills.sort();
	}

	public void refreshSession()
	{
		this.refreshSessionData();

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

	private void refreshSessionData()
	{
		KoLmafia.isRefreshing = true;

		KoLmafia.updateDisplay( "Refreshing session data..." );

		// Load saved counters before any requests are made, since both
		// charpane and charsheet requests can set them.

		KoLCharacter.setCurrentRun( 0 );
		TurnCounter.loadCounters();

		// Get current moon phases

		RequestThread.postRequest( new MoonPhaseRequest() );
		KoLCharacter.setHoliday( HolidayDatabase.getHoliday() );

		// Retrieve the character sheet first. It's necessary to do
		// this before concoctions have a chance to get refreshed.

		RequestThread.postRequest( new CharSheetRequest() );

                // Now that we know the character's ascension count, reset
                // anything that depends on that.

		KoLCharacter.resetPerAscensionData();

		RequestThread.postRequest( new AccountRequest() );
		RequestThread.postRequest( new QuestLogRequest() );

		// Retrieve the items which are available for consumption
		// and item creation.

		RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.CLOSET ) );

		// If the password hash is not available, then that means you
		// might be mid-transition.

		if ( GenericRequest.passwordHash.equals( "" ) )
		{
			return;
		}

		// Retrieve the list of familiars which are available to
		// the player.

		RequestThread.postRequest( new FamiliarRequest() );

		// Retrieve campground data to see if the user is able to
		// cook or make drinks

		KoLmafia.updateDisplay( "Retrieving campground data..." );
		RequestThread.postRequest( new CampgroundRequest() );
		KoLCharacter.checkTelescope();

		if ( Preferences.getInteger( "lastEmptiedStorage" ) != KoLCharacter.getAscensions() )
		{
			RequestThread.postRequest( new StorageRequest() );
			CafeRequest.pullLARPCard();
		}

		RequestThread.postRequest( CharPaneRequest.getInstance() );

		KoLmafia.updateDisplay( "Session data refreshed." );

		KoLmafia.isRefreshing = false;
	}

	public static final boolean isRefreshing()
	{
		return KoLmafia.isRefreshing;
	}

	public static final void setIsRefreshing( final boolean isRefreshing )
	{
		KoLmafia.isRefreshing = isRefreshing;
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

	public boolean isSemirare( AdventureResult result )
	{
		switch ( result.getItemId() )
		{
		case ItemPool.ASCII_SHIRT:
		case ItemPool.RHINO_HORMONES:
		case ItemPool.MAGIC_SCROLL:
		case ItemPool.PIRATE_JUICE:
		case ItemPool.PET_SNACKS:
		case ItemPool.INHALER:
		case ItemPool.CYCLOPS_EYEDROPS:
		case ItemPool.SPINACH:
		case ItemPool.FIRE_FLOWER:
		case ItemPool.ICE_CUBE:
		case ItemPool.FAKE_BLOOD:
		case ItemPool.GUANEAU:
		case ItemPool.LARD:
		case ItemPool.MYSTIC_SHELL:
		case ItemPool.LIP_BALM:
		case ItemPool.ANTIFREEZE:
		case ItemPool.BLACK_EYEDROPS:
		case ItemPool.DOGSGOTNONOZ:
		case ItemPool.FLIPBOOK:
		case ItemPool.NEW_CLOACA_COLA:
		case ItemPool.MASSAGE_OIL:
		case ItemPool.POLTERGEIST:
		case ItemPool.TASTY_TART:
		case ItemPool.LUNCHBOX:
		case ItemPool.KNOB_PASTY:
		case ItemPool.KNOB_COFFEE:
		case ItemPool.SQUEEZE:
		case ItemPool.FISHYSOISSE:
		case ItemPool.LAMP_SHADE:
		case ItemPool.GARBAGE_JUICE:
		case ItemPool.LEWD_CARD:
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
	 * @return The player's name if it has been seen, or null if it has not
	 *         yet appeared in the chat (not likely, but possible).
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
	 * Returns the string form of the player Id associated with the given
	 * player name.
	 *
	 * @param playerName The name of the player
	 * @return The player's Id if the player has been seen, or the player's
	 *         name with spaces replaced with underscores and other elements
	 *         encoded if the player's Id has not been seen.
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
	 * Utility. The method which ensures that the amount needed exists, and if not, calls the appropriate scripts to do
	 * so.
	 */

	private final boolean recover( float desired, final String settingName, final String currentName,
		final String maximumName, final Object[] techniques )
		throws Exception
	{
		// First, check for beaten up, if the person has tongue as an
		// auto-heal option. This takes precedence over all other checks.

		String restoreSetting = Preferences.getString( settingName + "Items" ).trim().toLowerCase();

		// Next, check against the restore needed to see if
		// any restoration needs to take place.

		Object[] empty = new Object[ 0 ];
		Method currentMethod, maximumMethod;

		currentMethod = KoLCharacter.class.getMethod( currentName, new Class[ 0 ] );
		maximumMethod = KoLCharacter.class.getMethod( maximumName, new Class[ 0 ] );

		float setting = Preferences.getFloat( settingName );

		if ( setting < 0.0f && desired == 0 )
		{
			return true;
		}

		int current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

		// If you've already reached the desired value, don't
		// bother restoring.

		if ( desired != 0 && current >= desired )
		{
			return true;
		}

		int maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
		int needed = (int) Math.min( maximum, Math.max( desired, setting * maximum + 1.0f ) );

		if ( current >= needed )
		{
			return true;
		}

		// Next, check against the restore target to see how
		// far you need to go.

		setting = Preferences.getFloat( settingName + "Target" );
		desired = Math.min( maximum, Math.max( desired, setting * maximum ) );

		if ( BuffBotHome.isBuffBotActive() || desired > maximum )
		{
			desired = maximum;
		}

		// If it gets this far, then you should attempt to recover
		// using the selected items. This involves a few extra
		// reflection methods.

		String currentTechniqueName;

		// Determine all applicable items and skills for the restoration.
		// This is a little bit memory intensive, but it allows for a lot
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

		int last = -1;

		// Special handling of the Hidden Temple. Here, as
		// long as your health is above zero, you're okay.

		boolean isNonCombatHealthRestore =
			settingName.startsWith( "hp" ) && KoLmafia.isAdventuring && KoLmafia.currentAdventure.isNonCombatsOnly();

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

		HPRestoreItemList.setPurchaseBasedSort( false );
		MPRestoreItemList.setPurchaseBasedSort( false );

		// Next, use any available skills. This only applies to health
		// restoration, since no MP-using skill restores MP.

		if ( !possibleSkills.isEmpty() )
		{
			current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleSkills );

				do
				{
					last = current;
					currentTechniqueName = possibleSkills.get( indexToTry ).toString().toLowerCase();

					this.recoverOnce( possibleSkills.get( indexToTry ), currentTechniqueName, (int) desired, false );
					current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

					maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
					desired = Math.min( maximum, desired );
					needed = Math.min( maximum, needed );

					if ( last >= current )
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
				current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

				maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
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
			RequestThread.postRequest( new UseItemRequest( new AdventureResult( 231, 1 ) ) );
			return true;
		}

		// If things are still not restored, try looking for items you
		// don't have but can purchase.

		if ( !possibleItems.isEmpty() )
		{
			HPRestoreItemList.setPurchaseBasedSort( true );
			MPRestoreItemList.setPurchaseBasedSort( true );

			current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();
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
					current = ( (Number) currentMethod.invoke( null, empty ) ).intValue();

					maximum = ( (Number) maximumMethod.invoke( null, empty ) ).intValue();
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
			if ( Preferences.getBoolean( "removeMalignantEffects" ) )
			{
				MoodManager.removeMalignantEffects();
			}

			HPRestoreItemList.updateHealthRestored();
			if ( this.invokeRecoveryScript( "HP", recover ) )
			{
				return true;
			}
			return this.recover(
				recover, "hpAutoRecovery", "getCurrentHP", "getMaximumHP", HPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
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
		// If the technique is an item, and the item is not readily
		// available, then don't bother with this item -- however, if
		// it is the only item present, then rethink it.

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
	 * Returns the total number of mana restores currently available to the
	 * player.
	 */

	public static final int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = Preferences.getString( "mpAutoRecoveryItems" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
		{
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[ i ].toString().toLowerCase() ) != -1 )
			{
				AdventureResult item = MPRestoreItemList.CONFIGURES[ i ].getItem();
				if ( item != null )
				{
					restoreCount += item.getCount( KoLConstants.inventory );
				}
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
			MPRestoreItemList.updateManaRestored();
			if ( this.invokeRecoveryScript( "MP", mpNeeded ) )
			{
				return true;
			}
			return this.recover(
				mpNeeded, "mpAutoRecovery", "getCurrentMP", "getMaximumMP", MPRestoreItemList.CONFIGURES );
		}
		catch ( Exception e )
		{
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return false;
		}
	}
	
	private boolean invokeRecoveryScript( String type, int needed )
	{
		String scriptName = Preferences.getString( "recoveryScript" );
		if ( scriptName.length() == 0 )
		{
			return false;
		}
		Interpreter interpreter = KoLmafiaASH.getInterpreter(
			KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter != null )
		{
			Value v = interpreter.execute( "main", new String[]
			{
				type,
				String.valueOf( needed )
			} );
			return v != null && v.intValue() != 0;
		}
		return false;
	}

	public void makeRequest( final Runnable request )
	{
		this.makeRequest( request, 1 );
	}

	/**
	 * Makes the given request for the given number of iterations, or until
	 * continues are no longer possible, either through user cancellation
	 * or something occuring which prevents the requests from resuming.
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

				if ( KoLCharacter.getCurrentHP() == 0 )
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
			// a subroutine. In doing so, make sure your HP/MP
			// restore settings are scaled back down to current
			// levels, if they've been manipulated internally by

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
			// This should not happen. Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	private boolean handleConditions( final AdventureResult[] items, final CreateItemRequest[] creatables )
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

		return KoLConstants.conditions.isEmpty();
	}
	
	public static void abortAfter( String msg )
	{
		KoLmafia.abortAfter = msg;
	}

	private void executeRequest( final Runnable request, final int totalIterations, final boolean wasAdventuring )
	{
		KoLmafia.hadPendingState = false;

		// Begin the adventuring process, or the request execution
		// process (whichever is applicable).

		boolean isAdventure = request instanceof KoLAdventure;
		AdventureResult[] items = new AdventureResult[ KoLConstants.conditions.size() ];
		CreateItemRequest[] creatables = new CreateItemRequest[ KoLConstants.conditions.size() ];

		for ( int i = 0; i < KoLConstants.conditions.size(); ++i )
		{
			items[ i ] = (AdventureResult) KoLConstants.conditions.get( i );
			creatables[ i ] = CreateItemRequest.getInstance( items[ i ] );
		}

		// Turn on auto-attack in order to save server hits if the
		// player isn't using custom combat.

		KoLmafia.forceContinue();
		KoLmafia.abortAfter = null;

		int adventuresBeforeRequest;
		int currentIteration = 0;

		boolean checkBounty = false;
		AdventureResult bounty = null;

		if ( isAdventure && ( bounty = AdventureDatabase.currentBounty() ) != null )
		{
			AdventureResult ar = AdventureDatabase.getBounty( (KoLAdventure) request );
			checkBounty = ar != null && bounty.getItemId() == ar.getItemId();
		}

		while ( KoLmafia.permitsContinue() && ++currentIteration <= totalIterations )
		{
			adventuresBeforeRequest = KoLCharacter.getAdventuresLeft();
			KoLmafia.adventureGains = 0;
			KoLmafia.tookChoice = false;

			this.executeRequestOnce( request, wasAdventuring, currentIteration, totalIterations, items, creatables );

			if ( isAdventure && ( adventuresBeforeRequest + KoLmafia.adventureGains ) == KoLCharacter.getAdventuresLeft() )
			{
				--currentIteration;
			}

			if ( checkBounty && bounty.getCount( KoLConstants.inventory ) == bounty.getCount() )
			{
				RequestThread.postRequest( new CoinMasterRequest( "lucre" ) );
				checkBounty = false;
			}
		}

		if ( isAdventure )
		{
			AdventureFrame.updateRequestMeter( 1, 1 );
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
	}

	private void executeAdventureOnce( final KoLAdventure adventure, boolean wasAdventuring,
		final int currentIteration, final int totalIterations, final AdventureResult[] items,
		final CreateItemRequest[] creatables )
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

		if ( KoLmafia.abortAfter != null )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, KoLmafia.abortAfter );
			return;
		}

		if ( adventure.getRequest() instanceof SewerRequest )
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

			int remainingIterations = totalIterations - currentIteration + 1;
			int gumAmount = stopCount == 0 ? remainingIterations : Math.min( stopCount, remainingIterations );
			if ( !InventoryManager.hasItem( ItemPool.CHEWING_GUM ) &&
				!InventoryManager.retrieveItem( ItemPool.CHEWING_GUM, gumAmount ) )
			{
				return;
			}
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

		if ( this.handleConditions( items, creatables ) )
		{
			KoLmafia.updateDisplay(
				KoLConstants.PENDING_STATE, "Conditions satisfied after " + currentIteration + " adventures." );
			return;
		}
	}

	private void executeRequestOnce( final Runnable request, final boolean wasAdventuring, final int currentIteration,
		final int totalIterations, final AdventureResult[] items, final CreateItemRequest[] creatables )
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
				"Campground request " + currentIteration + " of " + totalIterations + " in progress...";
		}

		RequestLogger.printLine();
		RequestThread.postRequest( (GenericRequest) request );
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
		return InputFieldUtilities.input( message, list );
	}

	/**
	 * Makes a request to the hermit, looking for the given number of items. This method should prompt the user to
	 * determine which item to retrieve the hermit.
	 */

	public void makeHermitRequest()
	{
		AdventureResult clover = ItemPool.get( ItemPool.TEN_LEAF_CLOVER, 1 );
		if ( !KoLConstants.hermitItems.contains( clover ) )
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

		if ( selected == ItemPool.TEN_LEAF_CLOVER )
		{
			int cloverCount = selectedValue.getCount();

			if ( cloverCount <= maximumValue )
			{
				message = "(There are " + cloverCount + " clovers still available)";
				maximumValue = cloverCount;
			}
		}

		int tradeCount =
			InputFieldUtilities.getQuantity(
				"How many " + selectedValue.getName() + " to get?\n" + message, maximumValue, 1 );
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
			InputFieldUtilities.getQuantity(
				"How many " + selectedValue.getName() + " to get?\n" + message, maximumValue,
				maximumValue );

		if ( tradeCount == 0 )
		{
			return;
		}

		KoLmafia.updateDisplay( "Visiting the trapper..." );
		RequestThread.postRequest( new GenericRequest(
			"trapper.php?pwd&action=Yep.&whichitem=" + selected + "&qty=" + tradeCount ) );
	}

	/**
	 * Makes a request to the bounty hunter hunter, looking for the given
	 * number of items. This method should prompt the user to determine
	 * which item to retrieve the hermit.
	 */

	public void makeHunterRequest()
	{
		GenericRequest hunterRequest = new CoinMasterRequest( "lucre" );
		RequestThread.postRequest( hunterRequest );

		Matcher bountyMatcher = Pattern.compile( "name=whichitem value=(\\d+)" ).matcher( hunterRequest.responseText );

		LockableListModel bounties = new LockableListModel();
		while ( bountyMatcher.find() )
		{
			String item = ItemDatabase.getItemName( StringUtilities.parseInt( bountyMatcher.group( 1 ) ) );
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
			int bounty = Preferences.getInteger( "currentBountyItem" );
			if ( hunterRequest.responseText.indexOf( "already turned in a Bounty today" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You've already turned in a bounty today." );
				return;
			}

			if ( bounty > 0 )
			{
				AdventureFrame.updateSelectedAdventure( AdventureDatabase.getBountyLocation( bounty ) );
			}

			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're already on a bounty hunt." );

			return;
		}

		String selectedValue = (String) KoLmafia.getSelectedValue( "Time to collect bounties!", bounties );
		if ( selectedValue == null )
		{
			return;
		}

		String selection = selectedValue.substring( 0, selectedValue.indexOf( "(" ) - 1 );
		int itemId = ItemDatabase.getItemId( selection );
		RequestThread.postRequest( new CoinMasterRequest( "lucre", "takebounty", itemId ) );
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
			if ( itemId == ItemPool.MEAT_STACK )
			{
				continue;
			}

			// Otherwise, accept any COMBINE recipe
			if ( ConcoctionDatabase.getMixingMethod( currentItem ) == KoLConstants.COMBINE )
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

		int currentLevel = KoLCharacter.getMindControlLevel();

		String selectedLevel =
			(String) InputFieldUtilities.input( "Change monster annoyance from " + currentLevel + "?", levelArray );

		if ( selectedLevel == null )
		{
			return;
		}

		int setting = StringUtilities.parseInt( selectedLevel.split( " " )[ 1 ] );
		RequestThread.postRequest( new MindControlRequest( setting ) );
	}

	public void makeCampgroundRestRequest()
	{
		String turnCount = InputFieldUtilities.input( "Rest for how many turns?", "1" );
		if ( turnCount == null )
		{
			return;
		}

		this.makeRequest( new CampgroundRequest( "rest" ), StringUtilities.parseInt( turnCount ) );
	}

	public void makeClanSofaRequest()
	{
		String turnCount = InputFieldUtilities.input( "Sleep for how many turns?", "1" );
		if ( turnCount == null )
		{
			return;
		}

		ClanRumpusRequest request = new ClanRumpusRequest( ClanRumpusRequest.SOFA );
		request.setTurnCount( StringUtilities.parseInt( turnCount ) );
		RequestThread.postRequest( request );
	}

	public static final void validateFaucetQuest()
	{
		int lastAscension = Preferences.getInteger( "lastTavernAscension" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastTavernSquare", 0 );
			Preferences.setInteger( "lastTavernAscension", KoLCharacter.getAscensions() );
			Preferences.setString( "tavernLayout", "0000000000000000000000000" );
		}
	}

	public static final void preTavernVisit( final GenericRequest request )
	{
		KoLmafia.validateFaucetQuest();

		String urlString = request.getURLString();
		int square = KoLmafia.currentTavernSquare( urlString );
		if ( square == 0 )
		{
			return;
		}

		Preferences.setInteger( "lastTavernSquare", square );
	}

	public static final void postTavernVisit( final GenericRequest request )
	{
		if ( KoLCharacter.getAdventuresLeft() == 0 ||
			KoLCharacter.getCurrentHP() == 0 ||
			KoLCharacter.getInebriety() > KoLCharacter.getInebrietyLimit() )
		{
			return;
		}

		String urlString = request.getURLString();
		if ( urlString.startsWith( "fight.php" ) )
		{
			int square = Preferences.getInteger( "lastTavernSquare" );
			char replacement = request.responseText.indexOf( "Baron" ) != -1 ? '4' : '1';
			KoLmafia.addTavernLocation( square, replacement );
			return;
		}

		int square = KoLmafia.currentTavernSquare( urlString );
		if ( square == 0 )
		{
			return;
		}

		char replacement = '1';
		if ( request.responseText.indexOf( "faucetoff" ) != -1 )
		{
			replacement = '3';
		}
		else if ( request.responseText.indexOf( "You acquire" ) != -1 )
		{
			replacement = '2';
		}

		KoLmafia.addTavernLocation( square, replacement );
		Preferences.setInteger( "lastTavernSquare", square );
	}

	private static final int currentTavernSquare( final String urlString )
	{
		Matcher squareMatcher = KoLmafia.TAVERN_PATTERN.matcher( urlString );
		if ( !squareMatcher.find() )
		{
			return 0;
		}

		return StringUtilities.parseInt( squareMatcher.group( 1 ) );
	}

	private static final void addTavernLocation( final int square, final char value )
	{
		KoLmafia.validateFaucetQuest();
		StringBuffer layout = new StringBuffer( Preferences.getString( "tavernLayout" ) );
		layout.setCharAt( square - 1, value );
		Preferences.setString( "tavernLayout", layout.toString() );
	}

	/**
	 * Completes the infamous tavern quest.
	 */

	public int locateTavernFaucet()
	{
		// Determine which elements have already been checked
		// so you don't check through them again.

		ArrayList searchList = new ArrayList();

		for ( int i = 1; i <= 25; ++i )
		{
			searchList.add( new Integer( i ) );
		}

		KoLmafia.validateFaucetQuest();
		String visited = Preferences.getString( "tavernLayout" );
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
				int faucetRow = i / 5 + 1;
				int faucetColumn = i % 5 + 1;

				KoLmafia.updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
				return i + 1;
			}

			case '4':
			{
				int baronRow = i / 5 + 1;
				int baronColumn = i % 5 + 1;

				KoLmafia.updateDisplay( "Baron found in row " + baronRow + ", column " + baronColumn );
				return i + 1;
			}
			}
		}

		// If the faucet has not yet been found, then go through
		// the process of trying to locate it.

		if ( KoLCharacter.getLevel() < 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need to level up first." );
			return -1;
		}

		RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );

		KoLmafia.updateDisplay( "Searching for faucet..." );

		KoLAdventure adventure = new KoLAdventure( "Woods", "rats.php", "", "Typical Tavern (Pre-Rat)" );
		GenericRequest request = adventure.getRequest();

		RequestThread.postRequest( adventure );

		// Random guess instead of straightforward search
		// for the location of the faucet.

		boolean foundFaucet = false;
		Integer searchIndex = null;

		while ( !foundFaucet && searchList.size() > 0 && KoLmafia.permitsContinue() && KoLCharacter.getCurrentHP() > 0 && KoLCharacter.getAdventuresLeft() > 0 )
		{
			searchIndex = (Integer) searchList.remove( KoLConstants.RNG.nextInt( searchList.size() ) );
			request.addFormField( "where", searchIndex.toString() );
			RequestThread.postRequest( adventure );

			String response = request.responseText;
			foundFaucet = response != null && response.indexOf( "faucetoff" ) != -1;
		}

		if ( !foundFaucet )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to find faucet." );
			return -1;
		}

		// Otherwise, you've found it! So notify the user
		// that the faucet has been found.

		int faucetRow = ( searchIndex.intValue() - 1 ) / 5 + 1;
		int faucetColumn = ( searchIndex.intValue() - 1 ) % 5 + 1;

		KoLmafia.updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
		return searchIndex.intValue();
	}

	/**
	 * Trades items with the guardian of the goud.
	 */

	public void tradeGourdItems()
	{
		RequestThread.postRequest( new GourdRequest() );

		AdventureResult item = GourdRequest.gourdItem( 5 );
		int neededCount = Preferences.getInteger( "gourdItemCount" );

		GenericRequest gourdVisit = new GourdRequest( true );

		while ( neededCount <= 25 && neededCount <= item.getCount( KoLConstants.inventory ) )
		{
			RequestThread.postRequest( gourdVisit );
			neededCount++;
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
		GenericRequest guildVisit = new GuildRequest();

		// The wiki claims that your prime stats are somehow connected,
		// but the exact procedure is uncertain. Therefore, just allow
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
				success = stopAtPaco ? guildVisit.responseText.indexOf( "paco" ) != -1 :
					guildVisit.responseText.indexOf( "You've already beaten" ) != -1;
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
		RequestThread.postRequest( new ManageStoreRequest() );

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

			int minimumPrice = Math.max( 100, ItemDatabase.getPriceById( sold[ i ].getItemId() ) * 2 );
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

		RequestThread.postRequest( new ManageStoreRequest( itemId, prices, limits ) );
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

	public void makePurchases( final List results, final Object[] purchases, final int maxPurchases,
		final boolean isAutomated )
	{
		this.makePurchases( results, purchases, maxPurchases, isAutomated, 0 );
	}

	public void makePurchases( final List results, final Object[] purchases, final int maxPurchases,
		final boolean isAutomated, final int priceLimit )
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
		int currentPrice = currentRequest.getPrice();

		int itemId = currentRequest.getItemId();

		if ( itemId == ItemPool.TEN_LEAF_CLOVER && Preferences.getBoolean( "cloverProtectActive" ) )
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
			currentRequest = (MallPurchaseRequest) purchases[ i ];
			currentPrice = currentRequest.getPrice();

			if ( currentRequest.getQuantity() != MallPurchaseRequest.MAX_QUANTITY )
			{
				if ( !KoLCharacter.canInteract() || isAutomated && !Preferences.getBoolean( "autoSatisfyWithMall" ) )
				{
					continue;
				}
			}

			if ( ( priceLimit > 0 && currentPrice > priceLimit ) ||
				( isAutomated && currentPrice > Preferences.getInteger( "autoBuyPriceLimit" ) ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"Stopped purchasing " + currentRequest.getItemName() + " @ " + KoLConstants.COMMA_FORMAT.format( currentPrice ) + "." );

				RequestThread.closeRequestSequence();
				return;
			}

			// Keep track of how many of the item you had before
			// you run the purchase request

			previousLimit = currentRequest.getLimit();
			currentRequest.setLimit( Math.min( previousLimit, desiredCount - currentCount ) );
			RequestThread.postRequest( currentRequest );

			// Now that you have already made a purchase from the
			// store, remove the purchase from the list!

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

		RequestThread.closeRequestSequence();

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
		return KoLmafia.NONE;
	}

	public static final boolean isAutoStop( final String encounterName )
	{
		String encounterType = KoLmafia.encounterType( encounterName );
		return encounterType == KoLmafia.STOP || encounterType == KoLmafia.GLYPH;
	}
	
	// Used to ignore semirare monsters re-encountered via Spooky Putty
	private static boolean ignoreSemirare = false;
	
	public static void ignoreSemirare()
	{
		KoLmafia.ignoreSemirare = true;
	}

	public static void recognizeEncounter( final String encounterName )
	{
		String encounterType = KoLmafia.encounterType( encounterName );

		if ( encounterType == KoLmafia.SEMIRARE && !KoLmafia.ignoreSemirare )
		{
			KoLCharacter.registerSemirare();
			return;
		}
		KoLmafia.ignoreSemirare = false;

		if ( encounterType == KoLmafia.NONE )
		{
			return;
		}

		if ( encounterType == KoLmafia.STOP || encounterType == KoLmafia.GLYPH )
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

	public static void registerEncounter( String encounterName, final String encounterType, final String responseText )
	{
		encounterName = encounterName.trim();

		KoLmafia.handleSpecialEncounter( encounterName, responseText );
		KoLmafia.recognizeEncounter( encounterName );

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
			}
			return;
		}

		if ( encounterName.equalsIgnoreCase( "Step Up to the Table, Put the Ball in Play" ) )
		{
			if ( InventoryManager.hasItem( ItemPool.CARONCH_DENTURES ) )
			{
				ResultProcessor.processItem( ItemPool.CARONCH_DENTURES, -1 );
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

		// Parsing complete. Return the list of
		// unique targets.

		return targets;
	}

	public final void downloadAdventureOverride()
	{
		UtilityConstants.DATA_LOCATION.mkdirs();

		for ( int i = 0; i < KoLConstants.OVERRIDE_DATA.length; ++i )
		{
			if ( !downloadOverride( KoLConstants.OVERRIDE_DATA[ i ] ) )
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
			FileUtilities.getReader( "http://kolmafia.svn.sourceforge.net/viewvc/*checkout*/kolmafia/src/data/" + name );

		File output = new File( UtilityConstants.DATA_LOCATION, "temp.txt" );
		PrintStream writer = LogStream.openStream( output, true );

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
		RequestThread.enableDisplayIfSequenceComplete();
	}

	public static final boolean isRunningBetweenBattleChecks()
	{
		return KoLmafia.recoveryActive || MoodManager.isExecuting();
	}

	public boolean runThresholdChecks()
	{
		float autoStopValue = Preferences.getFloat( "autoAbortThreshold" );
		if ( autoStopValue >= 0.0f )
		{
			autoStopValue *= KoLCharacter.getMaximumHP();
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
			String scriptPath = Preferences.getString( "betweenBattleScript" );
			if ( !scriptPath.equals( "" ) )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptPath );
			}
		}

		SpecialOutfit.createImplicitCheckpoint();

		// Now, run the built-in behavior to take care of
		// any loose ends.

		if ( isMoodCheck )
		{
			MoodManager.execute();
		}

		if ( isHealthCheck )
		{
			this.recoverHP();
		}

		if ( isMoodCheck )
		{
			MoodManager.burnExtraMana( false );
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
		
		FightRequest.haveFought();	// reset flag
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

		PauseObject pauser = new PauseObject();

		for ( int i = 0; i < 50 && !LocalRelayServer.isRunning(); ++i )
		{
			pauser.pause( 200 );
		}

		if ( !LocalRelayServer.isRunning() )
		{
			return;
		}
	}

	public void openRelayBrowser()
	{
		if ( GenericRequest.isCompactMode )
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
			RelayRequest.setNextMain( location );
			this.openRelayBrowser();
		}
	}

	public void launchRadioKoL()
	{
		StaticEntity.openSystemBrowser( "http://209.9.238.5:8794/listen.pls" );
	}

	public void gc()
	{
		int mem1 = (int) ( Runtime.getRuntime().freeMemory() >> 10 );
		System.gc();
		int mem2 = (int) ( Runtime.getRuntime().freeMemory() >> 10 );
		RequestLogger.printLine( "Reclaimed " + ( mem2 - mem1 ) + " KB of memory" );
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
		String text = KoLmafia.whoisPlayer( player );
		return text != null && text.indexOf( "online" ) != -1;
	}

	public void removeAllItemsFromStore()
	{
		RequestThread.openRequestSequence();
		RequestThread.postRequest( new ManageStoreRequest() );

		// Now determine the desired prices on items.
		// If the value of an item is currently 100,
		// then remove the item from the store.

		SoldItem[] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		for ( int i = 0; i < sold.length && KoLmafia.permitsContinue(); ++i )
		{
			RequestThread.postRequest( new ManageStoreRequest( sold[ i ].getItemId() ) );
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

		if ( !InputFieldUtilities.confirm( "Are you sure you'd like to host an end-of-run sale?" ) )
		{
			return;
		}

		// Find all tradeable items. Tradeable items
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
			if ( items[ i ].getItemId() == ItemPool.MEAT_PASTE || items[ i ].getItemId() == ItemPool.MEAT_STACK || items[ i ].getItemId() == ItemPool.DENSE_STACK )
			{
				continue;
			}

			if ( !ItemDatabase.isTradeable( items[ i ].getItemId() ) )
			{
				continue;
			}

			if ( ItemDatabase.getPriceById( items[ i ].getItemId() ) <= 0 )
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
		// maximum possible price. This allows KoLmafia
		// to determine the minimum price.

		if ( autosell.size() > 0 && KoLmafia.permitsContinue() )
		{
			RequestThread.postRequest( new SellStuffRequest( autosell.toArray(), SellStuffRequest.AUTOSELL ) );
		}

		if ( automall.size() > 0 && KoLmafia.permitsContinue() )
		{
			RequestThread.postRequest( new SellStuffRequest( automall.toArray(), SellStuffRequest.AUTOMALL ) );
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

			if ( currentItem.getItemId() == ItemPool.MEAT_PASTE || currentItem.getItemId() == ItemPool.MEAT_STACK || currentItem.getItemId() == ItemPool.DENSE_STACK )
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
			RequestThread.postRequest( new SellStuffRequest( sellList.toArray(), SellStuffRequest.AUTOMALL ) );
		}

		RequestThread.closeRequestSequence();
	}

	public void makeJunkRemovalRequest()
	{
		int itemCount;
		AdventureResult currentItem;

		Object[] items = KoLConstants.junkList.toArray();

		// Before doing anything else, go through the list of items
		// which are traditionally used and use them. Also, if the item
		// can be untinkered, it's usually more beneficial to untinker
		// first.

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

		if ( closetList.size() > 0 )
		{
			RequestThread.postRequest( new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, closetList.toArray() ) );
		}

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

				if ( canUntinker && ConcoctionDatabase.getMixingMethod( currentItem ) == KoLConstants.COMBINE )
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
					RequestThread.postRequest( new UseItemRequest( currentItem.getInstance( itemCount ) ) );
					break;

				case 621: // Warm Subject gift certificate
					RequestThread.postRequest( new UseItemRequest( currentItem.getInstance( itemCount ) ) );
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
					switch ( ItemDatabase.getConsumptionType( currentItem.getItemId() ) )
					{
					case KoLConstants.EQUIP_HAT:
					case KoLConstants.EQUIP_PANTS:
					case KoLConstants.EQUIP_SHIRT:
					case KoLConstants.EQUIP_WEAPON:
					case KoLConstants.EQUIP_OFFHAND:

						if ( InventoryManager.hasItem( ItemPool.TENDER_HAMMER ) && itemPower >= 100 || hasMalusAccess && itemPower > 10 )
						{
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );
						}

						break;

					case KoLConstants.EQUIP_FAMILIAR:
					case KoLConstants.EQUIP_ACCESSORY:

						if ( InventoryManager.hasItem( ItemPool.TENDER_HAMMER ) )
						{
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );
						}

						break;

					default:

						if ( currentItem.getName().endsWith( "powder" ) || currentItem.getName().endsWith( "nuggets" ) )
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

			if ( currentItem.getItemId() == ItemPool.MEAT_PASTE )
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
			RequestThread.postRequest( new SellStuffRequest( sellList.toArray(), SellStuffRequest.AUTOSELL ) );
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

				if ( currentItem.getItemId() == ItemPool.MEAT_PASTE )
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
				RequestThread.postRequest( new SellStuffRequest( sellList.toArray(), SellStuffRequest.AUTOSELL ) );
			}
		}

		RequestThread.closeRequestSequence();
	}

	private static class UpdateCheckThread
		extends Thread
	{
		public UpdateCheckThread()
		{
			super( "UpdateCheckThread" );
		}

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
				StringBuffer contents = new StringBuffer();
				BufferedReader reader =
					FileUtilities.getReader( "http://sourceforge.net/export/rss2_projfiles.php?group_id=126572" );

				while ( ( line = reader.readLine() ) != null )
				{
					contents.append( line );
				}

				String string = contents.toString();

				if ( string.indexOf( "<title>SourceForge.net: Inactive feed (group: 126572)</title>" ) != -1 )
				{
					System.out.println( "Update check failed: inactive feed" );
					return;
				}

				Matcher updateMatcher =
					Pattern.compile( "<title>(KoLmafia [^<]*?) released [^<]*?</title>" ).matcher( string );
				if ( !updateMatcher.find() )
				{
					System.out.println( string );
					return;
				}

				String lastVersion = Preferences.getString( "lastRssVersion" );
				String currentVersion = updateMatcher.group( 1 );

				Preferences.setString( "lastRssVersion", currentVersion );

				if ( currentVersion.equals( KoLConstants.VERSION_NAME ) || currentVersion.equals( lastVersion ) )
				{
					return;
				}

				if ( InputFieldUtilities.confirm( "A new version of KoLmafia is now available.  Would you like to download it now?" ) )
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
		public ShutdownThread()
		{
			super( "ShutdownThread" );
		}

		public void run()
		{
			Preferences.reset( null );
			FlaggedItems.saveFlaggedItemList();
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
				// That means the file either doesn't exist or
				// the session holder was somehow closed.
				// Ignore and fall through.
			}
		}
	}

	public static void logout()
	{
		// Create login frame to ensure that there is an active frame.

		if ( !StaticEntity.isHeadless() )
		{
			GenericFrame.createDisplay( LoginFrame.class );
		}

		// Shut down main frame

		if ( KoLDesktop.instanceExists() )
		{
			KoLDesktop.getInstance().dispose();
		}

		// Close down any other active frames.	Since
		// there is at least one active, logout will
		// not be called again.

		Frame[] frames = Frame.getFrames();
		for ( int i = 0; i < frames.length; ++i )
		{
			if ( frames[ i ].getClass() != LoginFrame.class )
			{
				frames[ i ].dispose();
			}
		}

		// Execute the logout request.

		RequestThread.postRequest( new LogoutRequest() );
	}

	public static void quit()
	{
		logout();
		System.exit( 0 );
	}

	public static void about()
	{
		new LicenseDisplayListener().run();
	}

	public static void preferences()
	{
		KoLmafiaGUI.constructFrame( "OptionsFrame" );
	}
}
