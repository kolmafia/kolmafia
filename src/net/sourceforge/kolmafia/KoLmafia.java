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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.math.BigInteger;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.UIManager;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.HPRestoreItemList.HPRestoreItem;
import net.sourceforge.kolmafia.MPRestoreItemList.MPRestoreItem;
import net.sourceforge.kolmafia.StoreManager.SoldItem;

public abstract class KoLmafia implements KoLConstants
{
	private static boolean isRefreshing = false;
	private static boolean isAdventuring = false;

	public static String lastMessage = "";

	static
	{
		System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "KoLmafia" );
		System.setProperty( "com.apple.mrj.application.live-resize", "true" );
		System.setProperty( "com.apple.mrj.application.growbox.intrudes", "false" );

		RequestPane.registerEditorKitForContentType( "text/html", RequestEditorKit.class.getName() );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
		System.setProperty( "http.referer", "www.kingdomofloathing.com" );
	}

	private static boolean hadPendingState = false;

	private static final String [] OVERRIDE_DATA =
	{
		"adventures.txt", "buffbots.txt", "classskills.txt", "combats.txt", "concoctions.txt",
		"equipment.txt", "familiars.txt", "fullness.txt", "inebriety.txt", "itemdescs.txt",
		"modifiers.txt", "monsters.txt", "npcstores.txt", "outfits.txt", "packages.txt",
		"spleenhit.txt", "statuseffects.txt", "tradeitems.txt", "zonelist.txt"
	};

	protected static String currentIterationString = "";
	protected static boolean recoveryActive = false;

	public static boolean isMakingRequest = false;
	public static KoLRequest currentRequest = null;
	public static int continuationState = CONTINUE_STATE;

	public static final int [] initialStats = new int[3];

	public static boolean executedLogin = false;

	private static final Pattern FUMBLE_PATTERN = Pattern.compile( "You drop your .*? on your .*?, doing ([\\d,]+) damage" );
	private static final Pattern STABBAT_PATTERN = Pattern.compile( " stabs you for ([\\d,]+) damage" );
	private static final Pattern CARBS_PATTERN = Pattern.compile( "some of your blood, to the tune of ([\\d,]+) damage" );
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

	private static FileLock SESSION_HOLDER = null;
	private static FileChannel SESSION_CHANNEL = null;
	private static File SESSION_FILE = null;

	private static KoLAdventure currentAdventure;

	private static final ArrayList stopEncounters = new ArrayList();
	static
	{
		stopEncounters.add( "History is Fun!" );
		stopEncounters.add( "It's A Sign!" );
		stopEncounters.add( "The Manor in Which You're Accustomed" );
		stopEncounters.add( "Under the Knife" );
		stopEncounters.add( "The Oracle Will See You Now" );
		stopEncounters.add( "A Grave Situation" );
		stopEncounters.add( "Take a Dusty Look!" );
		stopEncounters.add( "Drawn Onward" );
		stopEncounters.add( "Mr. Alarm" );
		stopEncounters.add( "We'll All Be Flat" );

				// Adventures that start the Around the World Quest

		stopEncounters.add( "I Just Wanna Fly" );
		stopEncounters.add( "Me Just Want Fly" );

		// Adventure in the Arid, Extra-Dry desert until you find the
		// Oasis

		stopEncounters.add( "Let's Make a Deal!" );

		// Get Ultra-hydrated and adventure in the Arid, Extra-Dry
		// desert until you are given the task to find a stone rose.

		stopEncounters.add( "A Sietch in Time" );

		// Adventure in Oasis until you have a stone rose and a drum
		// machine. Buy black paint.

		// Come back to the Arid, Extra-Dry Desert and adventure until
		// you are tasked to find the missing pages from the
		// worm-riding manual.

		stopEncounters.add( "Walk Without Rhythm" );

		// Adventure in Oasis until you have worm-riding manual pages
		// 3-15 Adventure in Arid, Extra-Dry Desert until you have
		// worm-riding hooks.

		// Adventures that give demon names

		stopEncounters.add( "Hoom Hah" );
		stopEncounters.add( "Every Seashell Has a Story to Tell If You're Listening" );
		stopEncounters.add( "Leavesdropping" );
		stopEncounters.add( "These Pipes... Aren't Clean!" );
	}

	private static final boolean acquireFileLock( String suffix )
	{
		try
		{
			SESSION_FILE = new File( SESSIONS_LOCATION, "active_session." + suffix );

			if ( SESSION_FILE.exists() )
			{
				SESSION_CHANNEL = new RandomAccessFile( SESSION_FILE, "rw" ).getChannel();
				SESSION_HOLDER = SESSION_CHANNEL.tryLock();
				return SESSION_HOLDER != null;
			}

			LogStream ostream = LogStream.openStream( SESSION_FILE, true );
			ostream.println( VERSION_NAME );
			ostream.close();

			SESSION_CHANNEL = new RandomAccessFile( SESSION_FILE, "rw" ).getChannel();
			SESSION_HOLDER = SESSION_CHANNEL.lock();
			return true;
		}
		catch ( Exception e )
		{
			return false;
		}
	}

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafiaGUI</code>.
	 */

	public static final void main( String [] args )
	{
		boolean useGUI = true;
		System.setProperty( "http.agent", VERSION_NAME );

		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[i].equals( "--CLI" ) )
				useGUI = false;
			if ( args[i].equals( "--GUI" ) )
				useGUI = true;
		}

		hermitItems.add( new AdventureResult( "banjo strings", 1, false ) );
		hermitItems.add( new AdventureResult( "catsup", 1, false ) );
		hermitItems.add( new AdventureResult( "chisel", 1, false ) );
		hermitItems.add( new AdventureResult( "dingy planks", 1, false ) );
		hermitItems.add( new AdventureResult( "golden twig", 1, false ) );
		hermitItems.add( new AdventureResult( "hot buttered roll", 1, false ) );
		hermitItems.add( new AdventureResult( "jaba\u00f1ero pepper", 1, false ) );
		hermitItems.add( new AdventureResult( "ketchup", 1, false ) );
		hermitItems.add( new AdventureResult( "petrified noodles", 1, false ) );
		hermitItems.add( new AdventureResult( "seal tooth", 1, false ) );
		hermitItems.add( new AdventureResult( "sweet rims", 1, false ) );
		hermitItems.add( new AdventureResult( "volleyball", 1, false ) );
		hermitItems.add( new AdventureResult( "wooden figurine", 1, false ) );

		trapperItems.add( new AdventureResult( 394, 1 ) );
		trapperItems.add( new AdventureResult( 393, 1 ) );
		trapperItems.add( new AdventureResult( 395, 1 ) );

		// Change it so that it doesn't recognize daylight savings in order
		// to ensure different localizations work.

		TimeZone koltime = (TimeZone) TimeZone.getDefault().clone();
		koltime.setRawOffset( 1000 * 60 * -270 );
		DATED_FILENAME_FORMAT.setTimeZone( koltime );

		// Reload your settings and determine all the different users which
		// are present in your save state list.

		KoLSettings.setUserProperty( "defaultLoginServer", String.valueOf( 1 + RNG.nextInt( KoLRequest.SERVER_COUNT ) ) );
		KoLSettings.setUserProperty( "relayBrowserOnly", "false" );

		String actualName;

		String [] pastUsers = StaticEntity.getPastUserList();

		for ( int i = 0; i < pastUsers.length; ++i )
		{
			if ( pastUsers[i].startsWith( "devster" ) )
				continue;

			actualName = KoLSettings.getGlobalProperty( pastUsers[i], "displayName" );
			if ( actualName.equals( "" ) )
				actualName = StaticEntity.globalStringReplace( pastUsers[i], "_", " " );

			saveStateNames.add( actualName );
		}

		// Also clear out any outdated data files.  Include the
		// adventure table, in case this is causing problems.

		String version = KoLSettings.getUserProperty( "previousUpdateVersion" );

		if ( version == null || !version.equals( VERSION_NAME ) )
		{
			KoLSettings.setUserProperty( "previousUpdateVersion", VERSION_NAME );
			for ( int i = 0; i < OVERRIDE_DATA.length; ++i )
			{
				File outdated = new File( DATA_LOCATION, OVERRIDE_DATA[i] );
				if ( outdated.exists() )
					outdated.delete();

				deleteSimulator( new File( RELAY_LOCATION, "simulator" ) );
			}
		}

		LimitedSizeChatBuffer.updateFontSize();

		// Change the default look and feel to match the player's
		// preferences.  Always do this.

		String lookAndFeel = KoLSettings.getUserProperty( "swingLookAndFeel" );
		boolean foundLookAndFeel = false;

		if ( lookAndFeel.equals( "" ) )
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) || System.getProperty( "os.name" ).startsWith( "Win" ) )
				lookAndFeel = UIManager.getSystemLookAndFeelClassName();
			else
				lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
		}

		UIManager.LookAndFeelInfo [] installed = UIManager.getInstalledLookAndFeels();
		String [] installedLooks = new String[ installed.length ];

		for ( int i = 0; i < installedLooks.length; ++i )
			installedLooks[i] = installed[i].getClassName();

		for ( int i = 0; i < installedLooks.length; ++i )
			foundLookAndFeel |= installedLooks[i].equals( lookAndFeel );

		if ( !foundLookAndFeel )
		{
			if ( System.getProperty( "os.name" ).startsWith( "Mac" ) || System.getProperty( "os.name" ).startsWith( "Win" ) )
				lookAndFeel = UIManager.getSystemLookAndFeelClassName();
			else
				lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();

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
			SystemTrayFrame.addTrayIcon();

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

		if ( !acquireFileLock( "1" ) && !acquireFileLock( "2" ) )
			System.exit(-1);

		KoLSettings.initializeLists();
		Runtime.getRuntime().addShutdownHook( new ShutdownThread() );

		// Now run the main routines for each, so that
		// you have an interface.

		if ( useGUI )
			KoLmafiaGUI.initialize();
		else
			KoLmafiaCLI.initialize();

		// Now, maybe the person wishes to run something
		// on startup, and they associated KoLmafia with
		// some non-ASH file extension.  This will run it.

		StringBuffer initialScript = new StringBuffer();

		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[i].equalsIgnoreCase( "--CLI" ) )
				continue;

			initialScript.append( args[i] );
			initialScript.append( " " );
		}

		if ( initialScript.length() != 0 )
		{
			String actualScript = initialScript.toString().trim();
			if ( actualScript.startsWith( "script=" ) )
				actualScript = actualScript.substring( 7 );

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + actualScript );
		}
		else if ( !useGUI )
		{
			KoLmafiaCLI.DEFAULT_SHELL.attemptLogin( "" );
		}

		// Always read input from the command line when you're not
		// in GUI mode.

		if ( !useGUI )
			KoLmafiaCLI.DEFAULT_SHELL.listenForCommands();
	}

	private static final void deleteSimulator( File location )
	{
		if ( location.isDirectory() )
		{
			File [] files = location.listFiles();
			for ( int i = 0; i < files.length; ++i )
				deleteSimulator( files[i] );
		}

		location.delete();
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk.
	 */

	public KoLmafia()
	{
	}

	public static final String getLastMessage()
	{	return lastMessage;
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public static final void updateDisplay( String message )
	{	updateDisplay( CONTINUE_STATE, message );
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public static final void updateDisplay( int state, String message )
	{
		if ( continuationState == ABORT_STATE && state != ABORT_STATE )
			return;

		if ( continuationState != PENDING_STATE )
			continuationState = state;

		RequestLogger.printLine( state, message );
		SystemTrayFrame.updateToolTip( message );
		lastMessage = message;

		if ( message.indexOf( LINE_BREAK ) == -1 )
			updateDisplayState( state, RequestEditorKit.getStripped( message ) );
	}

	private static final void updateDisplayState( int state, String message )
	{
		// Next, update all of the panels with the
		// desired update message.

		ActionPanel [] panels = StaticEntity.getExistingPanels();

		for ( int i = 0; i < panels.length; ++i )
		{
			if ( panels[i] instanceof KoLPanel )
				((KoLPanel)panels[i]).setStatusMessage( message );

			panels[i].setEnabled( state != CONTINUE_STATE );
		}

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		for ( int i = 0; i < frames.length; ++i )
		{
			frames[i].setStatusMessage( message );
			frames[i].updateDisplayState( state );
		}

		if ( KoLDesktop.instanceExists() )
			KoLDesktop.getInstance().updateDisplayState( state );
	}

	public static final void enableDisplay()
	{
		updateDisplayState( continuationState == ABORT_STATE || continuationState == ERROR_STATE ? ERROR_STATE : ENABLE_STATE, "" );
		continuationState = CONTINUE_STATE;
	}

	public static final boolean executedLogin()
	{	return executedLogin;
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify thethat the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String username )
	{
		String originalName = KoLCharacter.getUserName();

		// Initialize the variables to their initial
		// states to avoid null pointers getting thrown
		// all over the place

		executedLogin = true;

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

		// If the password hash is non-null, then that means you
		// might be mid-transition.

		if ( KoLRequest.passwordHash != null && KoLRequest.passwordHash.equals( "" ) )
			return;

		int today = MoonPhaseDatabase.getPhaseStep();
		if ( KoLSettings.getIntegerProperty( "lastCounterDay" ) != today )
			resetCounters();

		registerPlayer( username, String.valueOf( KoLCharacter.getUserId() ) );

		if ( KoLSettings.getGlobalProperty( username, "getBreakfast" ).equals( "true" ) )
		{
			this.getBreakfast( true, KoLSettings.getIntegerProperty( "lastBreakfast" ) != today );
			KoLSettings.setUserProperty( "lastBreakfast", String.valueOf( today ) );
		}

		// Also, do mushrooms, if a mushroom script has already
		// been setup by the user.

		if ( KoLSettings.getBooleanProperty( "autoPlant" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") ) )
		{
			String currentLayout = KoLSettings.getUserProperty( "plantingScript" );
			if ( !currentLayout.equals( "" ) && KoLCharacter.inMuscleSign() && MushroomPlot.ownsPlot() )
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + PLOTS_DIRECTORY + currentLayout + ".ash" );
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

		// Summon Candy Heart now costs 1 MP again
		usableSkills.sort();
	}

	public void getBreakfast( boolean checkSettings, boolean checkCampground )
	{
		if ( checkCampground )
		{
			if ( KoLCharacter.hasToaster() )
			{
				for ( int i = 0; i < 3 && permitsContinue(); ++i )
					RequestThread.postRequest( new CampgroundRequest( "toast" ) );

				forceContinue();
			}

			if ( KoLCharacter.hasArches() )
			{
				RequestThread.postRequest( new CampgroundRequest( "arches" ) );
				forceContinue();
			}

			if ( KoLSettings.getBooleanProperty( "visitRumpus" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") ) )
			{
				RequestThread.postRequest( new ClanGymRequest( ClanGymRequest.SEARCH ) );
				forceContinue();
			}

			if ( KoLSettings.getBooleanProperty( "readManual" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") ) )
			{
				AdventureResult manual = KoLCharacter.isMuscleClass() ? MANUAL_1 : KoLCharacter.isMysticalityClass() ? MANUAL_2 : MANUAL_3;
				if ( KoLCharacter.hasItem( manual ) )
					RequestThread.postRequest( new ConsumeItemRequest( manual ) );

				forceContinue();
			}

			if ( KoLSettings.getBooleanProperty( "grabClovers" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") ) )
			{
				if ( HermitRequest.getWorthlessItemCount() > 0 )
					KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hermit * ten-leaf clover" );

				forceContinue();
			}

			if ( KoLSettings.getIntegerProperty( "lastFilthClearance" ) == KoLCharacter.getAscensions() )
			{
				KoLmafia.updateDisplay( "Collecting cut of hippy profits..." );
				RequestThread.postRequest( new KoLRequest( "store.php?whichstore=h" ) );
				forceContinue();
			}
		}

		SpecialOutfit.createImplicitCheckpoint();
		this.castBreakfastSkills( checkSettings, 0 );
		SpecialOutfit.restoreImplicitCheckpoint();
		forceContinue();
	}

	public void castBreakfastSkills( boolean checkSettings, int manaRemaining )
	{
		this.castBreakfastSkills( checkSettings,
			KoLSettings.getBooleanProperty( "loginRecovery" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") ), manaRemaining );
	}

	public boolean castBreakfastSkills( boolean checkSettings, boolean allowRestore, int manaRemaining )
	{
		if ( KoLSettings.getBooleanProperty( "breakfastCompleted" ) )
			return true;

		boolean shouldCast = false;
		boolean limitExceeded = true;

		String skillSetting = KoLSettings.getUserProperty( "breakfast" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") );
		boolean pathedSummons = KoLSettings.getBooleanProperty( "pathedSummons" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") );

		if ( skillSetting != null )
		{
			for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
			{
				shouldCast = !checkSettings || skillSetting.indexOf( UseSkillRequest.BREAKFAST_SKILLS[i] ) != -1;
				shouldCast &= KoLCharacter.hasSkill( UseSkillRequest.BREAKFAST_SKILLS[i] );

				if ( checkSettings && pathedSummons )
				{
					if ( UseSkillRequest.BREAKFAST_SKILLS[i].equals( "Pastamastery" ) && !KoLCharacter.canEat() )
						shouldCast = false;
					if ( UseSkillRequest.BREAKFAST_SKILLS[i].equals( "Advanced Cocktailcrafting" ) && !KoLCharacter.canDrink() )
						shouldCast = false;
				}

				if ( shouldCast )
					limitExceeded &= this.getBreakfast( UseSkillRequest.BREAKFAST_SKILLS[i], allowRestore, manaRemaining );
			}
		}

		KoLSettings.setUserProperty( "breakfastCompleted", String.valueOf( limitExceeded ) );
		return limitExceeded;
	}

	public boolean getBreakfast( String skillName, boolean allowRestore, int manaRemaining )
	{
		UseSkillRequest summon = UseSkillRequest.getInstance( skillName );

		// Special handling for candy heart summoning.  This skill
		// can be extremely expensive, so rather than summoning to
		// your limit, designated by maximum MP, it merely swallows
		// all your current MP.

		if ( summon.getSkillId() == 18 )
		{
			summon.setBuffCount( 1 );

			while ( ClassSkillsDatabase.getMPConsumptionById( 18 ) <= KoLCharacter.getCurrentMP() - manaRemaining )
				RequestThread.postRequest( summon );

			return true;
		}

		// For all other skills, if you don't need to cast them, then
		// skip this step.

		int maximumCast = summon.getMaximumCast();

		if ( maximumCast <= 0 )
			return true;

		int castCount = Math.min( maximumCast, allowRestore ? 5 :
			(KoLCharacter.getCurrentMP() - manaRemaining) / ClassSkillsDatabase.getMPConsumptionById( ClassSkillsDatabase.getSkillId( skillName ) ) );

		if ( castCount == 0 )
			return false;

		summon.setBuffCount( castCount );
		RequestThread.postRequest( summon );

		return castCount == maximumCast;
	}

	public void refreshSession()
	{	this.refreshSession( true );
	}

	public void refreshSession( boolean getQuestLog )
	{
		isRefreshing = true;

		// Get current moon phases

		updateDisplay( "Refreshing session data..." );

		RequestThread.postRequest( new MoonPhaseRequest() );
		KoLCharacter.setHoliday( MoonPhaseDatabase.getHoliday( new Date() ) );

		// Retrieve the character sheet first. It's necessary to do
		// this before concoctions have a chance to get refreshed.

		RequestThread.postRequest( new CharsheetRequest() );

		if ( getQuestLog )
			RequestThread.postRequest( new QuestLogRequest() );

		// Clear the violet fog path table and everything
		// else that changes on the player.

		VioletFog.reset();
		Louvre.reset();
		MushroomPlot.reset();
		TradeableItemDatabase.getDustyBottles();

		// Retrieve the items which are available for consumption
		// and item creation.

		if ( getQuestLog )
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.CLOSET ) );

		// If the password hash is non-null, but is not available,
		// then that means you might be mid-transition.

		if ( KoLRequest.passwordHash != null && KoLRequest.passwordHash.equals( "" ) )
			return;

		// Retrieve the list of familiars which are available to
		// the player.

		RequestThread.postRequest( new FamiliarRequest() );

		// Retrieve campground data to see if the user is able to
		// cook, make drinks or make toast.

		if ( getQuestLog )
		{
			updateDisplay( "Retrieving campground data..." );
			RequestThread.postRequest( new CampgroundRequest() );
		}

		if ( KoLSettings.getIntegerProperty( "lastEmptiedStorage" ) != KoLCharacter.getAscensions() )
		{
			RequestThread.postRequest( new ItemStorageRequest() );
			if ( storage.isEmpty() )
				KoLSettings.setUserProperty( "lastEmptiedStorage", String.valueOf( KoLCharacter.getAscensions() ) );
		}

		RequestThread.postRequest( CharpaneRequest.getInstance() );
		StaticEntity.loadCounters();

		updateDisplay( "Session data refreshed." );

		isRefreshing = false;
	}

	public static final boolean isRefreshing()
	{	return isRefreshing;
	}

	/**
	 * Used to reset the session tally to its original values.
	 */

	public void resetSession()
	{
		encounterList.clear();
		adventureList.clear();

		initialStats[0] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() );
		initialStats[1] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() );
		initialStats[2] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() );

		AdventureResult.SESSION_FULLSTATS[0] = 0;
		AdventureResult.SESSION_FULLSTATS[1] = 0;
		AdventureResult.SESSION_FULLSTATS[2] = 0;

		tally.clear();
		tally.add( new AdventureResult( AdventureResult.ADV ) );
		tally.add( new AdventureResult( AdventureResult.MEAT ) );
		tally.add( AdventureResult.SESSION_SUBSTATS_RESULT );
		tally.add( AdventureResult.SESSION_FULLSTATS_RESULT );
	}

	/**
	 * Utility.  The method to parse an individual adventuring result.
	 * This method determines what the result actually was and adds it to
	 * the tally.
	 *
	 * @param	result	String to parse for the result
	 */

	public AdventureResult parseResult( String result )
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

	private AdventureResult parseItem( String result )
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
			return new AdventureResult( itemId, 1 );

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

	private boolean parseEffect( String result )
	{
		RequestLogger.updateDebugLog( "Parsing effect: " + result );

		StringTokenizer parsedEffect = new StringTokenizer( result, "()" );
		String parsedEffectName = parsedEffect.nextToken().trim();
		String parsedDuration = parsedEffect.hasMoreTokens() ? parsedEffect.nextToken() : "1";

		return this.processResult( new AdventureResult( parsedEffectName, StaticEntity.parseInt( parsedDuration ), true ) );
	}

	/**
	 * Utility.  The method used to process a result.  By default, this
	 * method will also add an adventure result to the tally directly.
	 * This is used whenever the nature of the result is already known and
	 * no additional parsing is needed.
	 *
	 * @param	result	Result to add to the running tally of adventure results
	 */

	public boolean processResult( AdventureResult result )
	{
		// This should not happen, but check just in case and
		// return if the result was null.

		if ( result == null )
			return false;

		RequestLogger.updateDebugLog( "Processing result: " + result );

		String resultName = result.getName();
		boolean shouldRefresh = false;

		// Process the adventure result in this section; if
		// it's a status effect, then add it to the recent
		// effect list.  Otherwise, add it to the tally.

		if ( result.isStatusEffect() )
		{
			shouldRefresh |= !activeEffects.contains( result );
			AdventureResult.addResultToList( recentEffects, result );
		}
		else if ( resultName.equals( AdventureResult.ADV ) && result.getCount() < 0 )
		{
			StaticEntity.saveCounters();
			AdventureResult.addResultToList( tally, result.getNegation() );
		}
		else if ( result.isItem() || resultName.equals( AdventureResult.SUBSTATS ) || resultName.equals( AdventureResult.MEAT ) )
		{
			// If you gain a sock, you lose all the immateria

			if ( result.equals( KoLAdventure.SOCK ) && result.getCount() == 1 )
				for ( int i = 0; i < KoLAdventure.IMMATERIA.length; ++i )
					this.processResult( KoLAdventure.IMMATERIA[i] );

			AdventureResult.addResultToList( tally, result );
		}

		KoLCharacter.processResult( result );
		shouldRefresh |= result.getName().equals( AdventureResult.MEAT );

		// Process the adventure result through the conditions
		// list, removing it if the condition is satisfied.

		if ( result.isItem() && HermitRequest.isWorthlessItem( result.getItemId() ) )
			result = HermitRequest.WORTHLESS_ITEM.getInstance( result.getCount() );

		int conditionIndex = conditions.indexOf( result );

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
					if ( AdventureResult.CONDITION_SUBSTATS[i] == 0 )
						continue;

					AdventureResult.CONDITION_SUBSTATS[i] = Math.max( 0, AdventureResult.CONDITION_SUBSTATS[i] - result.getCount(i) );
				}

				if ( AdventureResult.CONDITION_SUBSTATS_RESULT.getCount() == 0 )
					conditions.remove( conditionIndex );
				else
					conditions.fireContentsChanged( conditions, conditionIndex, conditionIndex );
			}
			else
			{
				// Otherwise, this was a partial satisfaction
				// of a condition.  Decrement the count by the
				// negation of this result.

				AdventureResult.addResultToList( conditions, result.getNegation() );
				if ( result.getCount( conditions ) <= 0 )
					conditions.remove( result );
			}
		}

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		if ( resultName.equals( AdventureResult.SUBSTATS ) && tally.size() >= 3 )
		{
			int currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() ) - initialStats[0];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[0] != currentTest;
			AdventureResult.SESSION_FULLSTATS[0] = currentTest;

			currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() ) - initialStats[1];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[1] != currentTest;
			AdventureResult.SESSION_FULLSTATS[1] = currentTest;

			currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() ) - initialStats[2];
			shouldRefresh |= AdventureResult.SESSION_FULLSTATS[2] != currentTest;
			AdventureResult.SESSION_FULLSTATS[2] = currentTest;

			if ( tally.size() > 3 )
				tally.fireContentsChanged( tally, 3, 3 );
		}

		return shouldRefresh;
	}

	/**
	 * Adds the recent effects accumulated so far to the actual effects.
	 * This should be called after the previous effects were decremented,
	 * if adventuring took place.
	 */

	public static final void applyEffects()
	{
		int oldCount = activeEffects.size();

		for ( int j = 0; j < recentEffects.size(); ++j )
			AdventureResult.addResultToList( activeEffects, (AdventureResult) recentEffects.get(j) );

		recentEffects.clear();
		activeEffects.sort();

		if ( oldCount != activeEffects.size() )
			KoLCharacter.updateStatus();
	}

	/**
	 * Returns the string form of the player Id associated
	 * with the given player name.
	 *
	 * @param	playerId	The Id of the player
	 * @return	The player's name if it has been seen, or null if it has not
	 *          yet appeared in the chat (not likely, but possible).
	 */

	public static final String getPlayerName( String playerId )
	{
		if ( playerId == null )
			return null;

		String playerName = (String) seenPlayerNames.get( playerId );
		return playerName != null ? playerName : playerId;
	}

	/**
	 * Returns the string form of the player Id associated
	 * with the given player name.
	 *
	 * @param	playerName	The name of the player
	 * @return	The player's Id if the player has been seen, or the player's name
	 *			with spaces replaced with underscores and other elements encoded
	 *			if the player's Id has not been seen.
	 */

	public static final String getPlayerId( String playerName )
	{
		if ( playerName == null )
			return null;

		String playerId = (String) seenPlayerIds.get( playerName.toLowerCase() );
		return playerId != null ? playerId : playerName;
	}

	/**
	 * Registers the given player name and player Id with
	 * KoLmafia's player name tracker.
	 *
	 * @param	playerName	The name of the player
	 * @param	playerId	The player Id associated with this player
	 */

	public static final void registerPlayer( String playerName, String playerId )
	{
		playerName = playerName.replaceAll( "[^0-9A-Za-z_ ]", "" );
		String lowercase = playerName.toLowerCase();

		if ( lowercase.equals( "modwarning" ) || seenPlayerIds.containsKey( lowercase ) )
			return;

		seenPlayerIds.put( lowercase, playerId );
		seenPlayerNames.put( playerId, playerName );
	}

	public static final void registerContact( String playerName, String playerId )
	{
		playerName = playerName.toLowerCase().replaceAll( "[^0-9A-Za-z_ ]", "" );
		registerPlayer( playerName, playerId );
		if ( !contactList.contains( playerName ) )
			contactList.add( playerName.toLowerCase() );
	}

	/**
	 * Returns whether or not the current user has a ten-leaf clover.
	 *
	 * @return	<code>true</code>
	 */

	public boolean isLuckyCharacter()
	{	return inventory.contains( SewerRequest.CLOVER );
	}

	/**
	 * Utility. The method which ensures that the amount needed exists,
	 * and if not, calls the appropriate scripts to do so.
	 */

	private final boolean recover( float desired, String settingName, String currentName, String maximumName, Object [] techniques ) throws Exception
	{
		// First, check for beaten up, if the person has tongue as an
		// auto-heal option.  This takes precedence over all other checks.

		String restoreSetting = KoLSettings.getUserProperty( settingName + "Items" ).trim().toLowerCase();

		// Next, check against the restore needed to see if
		// any restoration needs to take place.

		Object [] empty = new Object[0];
		Method currentMethod, maximumMethod;

		currentMethod = KoLCharacter.class.getMethod( currentName, new Class[0] );
		maximumMethod = KoLCharacter.class.getMethod( maximumName, new Class[0] );

		float setting = KoLSettings.getFloatProperty( settingName );

		if ( setting < 0.0f && desired == 0 )
			return true;

		float current = ((Number)currentMethod.invoke( null, empty )).floatValue();

		// If you've already reached the desired value, don't
		// bother restoring.

		if ( desired != 0 && current >= desired )
			return true;

		float maximum = ((Number)maximumMethod.invoke( null, empty )).floatValue();
		float needed = Math.min( maximum, Math.max( desired, setting * maximum + 1.0f ) );

		if ( current >= needed )
			return true;

		// Next, check against the restore target to see how
		// far you need to go.

		setting = KoLSettings.getFloatProperty( settingName + "Target" );
		desired = Math.min( maximum, Math.max( desired, setting * maximum ) );

		if ( BuffBotHome.isBuffBotActive() || desired > maximum )
			desired = maximum;

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
			currentTechniqueName = techniques[i].toString().toLowerCase();
			if ( restoreSetting.indexOf( currentTechniqueName ) == -1 )
				continue;

			if ( techniques[i] instanceof HPRestoreItem )
			{
				if ( ((HPRestoreItem)techniques[i]).isSkill() )
					possibleSkills.add( techniques[i] );
				else
					possibleItems.add( techniques[i] );
			}

			if ( techniques[i] instanceof MPRestoreItem )
			{
				if ( ((MPRestoreItem)techniques[i]).isSkill() )
					possibleSkills.add( techniques[i] );
				else
					possibleItems.add( techniques[i] );
			}
		}

		float last = -1;

		// Special handling of the Hidden Temple.  Here, as
		// long as your health is above zero, you're okay.

		boolean isNonCombatHealthRestore = settingName.startsWith( "hp" ) && isAdventuring() && currentAdventure.isNonCombatsOnly();

		if ( isNonCombatHealthRestore )
		{
			if ( KoLCharacter.getCurrentHP() > 0 )
				return true;

			needed = 1;
			desired = 1;
		}

		// Consider clearing beaten up if your restoration settings
		// include the appropriate items.

		if ( settingName.startsWith( "hp" ) && KoLSettings.getBooleanProperty( "completeHealthRestore" ) )
		{
			MoodSettings.fixMaximumHealth( restoreSetting );
			current = KoLCharacter.getCurrentHP();
			needed = Math.max( needed, Math.min( desired, current + 1 ) );
		}

		if ( current >= needed )
			return true;

		for ( int i = 0; i < possibleSkills.size(); ++i )
		{
			if ( possibleSkills.get(i) instanceof HPRestoreItem )
				((HPRestoreItem)possibleSkills.get(i)).updateHealthPerUse();
			else
				((MPRestoreItem)possibleSkills.get(i)).updateManaPerUse();
		}

		for ( int i = 0; i < possibleItems.size(); ++i )
		{
			if ( possibleItems.get(i) instanceof HPRestoreItem )
				((HPRestoreItem)possibleItems.get(i)).updateHealthPerUse();
			else
				((MPRestoreItem)possibleItems.get(i)).updateManaPerUse();
		}

		HPRestoreItemList.setPurchaseBasedSort( false );
		MPRestoreItemList.setPurchaseBasedSort( false );

		// Next, use any available skills.  This only applies to health
		// restoration, since no MP-using skill restores MP.

		if ( !possibleSkills.isEmpty() )
		{
			current = ((Number)currentMethod.invoke( null, empty )).floatValue();

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleSkills );

				do
				{
					last = current;
					currentTechniqueName = possibleSkills.get(indexToTry).toString().toLowerCase();

					this.recoverOnce( possibleSkills.get(indexToTry), currentTechniqueName, (int) desired, false );
					current = ((Number)currentMethod.invoke( null, empty )).floatValue();

					maximum = ((Number)maximumMethod.invoke( null, empty )).floatValue();
					desired = Math.min( maximum, desired );
					needed = Math.min( maximum, needed );

					if ( last == current )
						++indexToTry;
				}
				while ( indexToTry < possibleSkills.size() && current < needed );
			}

			if ( refusesContinue() )
				return false;
		}

		// Iterate through every restore item which is already available
		// in the player's inventory.

		Collections.sort( possibleItems );

		for ( int i = 0; i < possibleItems.size() && current < needed; ++i )
		{
			do
			{
				last = current;
				currentTechniqueName = possibleItems.get(i).toString().toLowerCase();

				this.recoverOnce( possibleItems.get(i), currentTechniqueName, (int) desired, false );
				current = ((Number)currentMethod.invoke( null, empty )).floatValue();

				maximum = ((Number)maximumMethod.invoke( null, empty )).floatValue();
				desired = Math.min( maximum, desired );
				needed = Math.min( maximum, needed );
			}
			while ( last != current && current < needed );
		}

		if ( refusesContinue() )
			return false;

		// For areas that are all noncombats, then you can go ahead
		// and heal using only unguent.

		if ( isNonCombatHealthRestore && KoLSettings.getBooleanProperty( "autoBuyRestores" ) && KoLCharacter.getAvailableMeat() >= 30 )
		{
			RequestThread.postRequest( new ConsumeItemRequest( new AdventureResult( 231, 1 ) ) );
			return true;
		}

		// If things are still not restored, try looking for items you
		// don't have but can purchase.

		if ( !possibleItems.isEmpty() && KoLSettings.getBooleanProperty( "autoBuyRestores" ) )
		{
			HPRestoreItemList.setPurchaseBasedSort( true );
			MPRestoreItemList.setPurchaseBasedSort( true );

			current = ((Number)currentMethod.invoke( null, empty )).floatValue();
			last = -1;

			while ( last != current && current < needed )
			{
				int indexToTry = 0;
				Collections.sort( possibleItems );

				do
				{
					last = current;
					currentTechniqueName = possibleItems.get(indexToTry).toString().toLowerCase();

					this.recoverOnce( possibleItems.get(indexToTry), currentTechniqueName, (int) desired, true );
					current = ((Number)currentMethod.invoke( null, empty )).floatValue();

					maximum = ((Number)maximumMethod.invoke( null, empty )).floatValue();
					desired = Math.min( maximum, desired );

					if ( last == current )
						++indexToTry;
				}
				while ( indexToTry < possibleItems.size() && current < needed );
			}

			HPRestoreItemList.setPurchaseBasedSort( false );
			MPRestoreItemList.setPurchaseBasedSort( false );
		}
		else if ( current < needed )
		{
			updateDisplay( ERROR_STATE, "You ran out of restores." );
			return false;
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( refusesContinue() )
			return false;

		if ( current < needed )
		{
			updateDisplay( ERROR_STATE, "Autorecovery failed." );
			return false;
		}

		return true;
	}

	/**
	 * Utility.  The method called in between battles. This method checks
	 * to see if the character's HP has dropped below the tolerance value,
	 * and recovers if it has (if the user has specified this in their
	 * settings).
	 */

	public final boolean recoverHP()
	{	return this.recoverHP( 0 );
	}

	public final boolean recoverHP( int recover )
	{
		try
		{
			return this.recover( recover, "hpAutoRecovery", "getCurrentHP", "getMaximumHP", HPRestoreItemList.CONFIGURES );
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
	 * Utility. The method which uses the given recovery technique (not
	 * specified in a script) in order to restore.
	 */

	private final void recoverOnce( Object technique, String techniqueName, int needed, boolean purchase )
	{
		// If the technique is an item, and the item is not readily available,
		// then don't bother with this item -- however, if it is the only item
		// present, then rethink it.

		if ( technique instanceof HPRestoreItem )
			((HPRestoreItem)technique).recoverHP( needed, purchase );

		if ( technique instanceof MPRestoreItem )
			((MPRestoreItem)technique).recoverMP( needed, purchase );
	}

	/**
	 * Returns the total number of mana restores currently
	 * available to the player.
	 */

	public static final int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = KoLSettings.getUserProperty( "mpAutoRecoveryItems" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[i].toString().toLowerCase() ) != -1 )
				restoreCount += MPRestoreItemList.CONFIGURES[i].getItem().getCount( inventory );

		return restoreCount;
	}

	/**
	 * Utility. The method called in between commands.  This method checks
	 * to see if the character's MP has dropped below the tolerance value,
	 * and recovers if it has (if the user has specified this in their
	 * settings).
	 */

	public final boolean recoverMP()
	{	return this.recoverMP( 0 );
	}

	/**
	 * Utility. The method which restores the character's current mana
	 * points above the given value.
	 */

	public final boolean recoverMP( int mpNeeded )
	{
		try
		{
			return this.recover( mpNeeded, "mpAutoRecovery", "getCurrentMP", "getMaximumMP", MPRestoreItemList.CONFIGURES );
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
	 * Utility. The method used to process the results of any adventure in
	 * the Kingdom of Loathing This method searches for items, stat gains,
	 * and losses within the provided string.
	 *
	 * @param	results	The string containing the results of the adventure
	 * @return	<code>true</code> if any results existed
	 */

	public final boolean processResults( String results )
	{	return this.processResults( results, null );
	}

	public final boolean processResults( String results, ArrayList data )
	{
		if ( data == null )
			RequestLogger.updateDebugLog( "Processing results..." );

		if ( data == null && results.indexOf( "gains a pound" ) != -1 )
		{
			KoLCharacter.incrementFamilarWeight();

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "familiar " + KoLCharacter.getFamiliar() );
			RequestLogger.updateSessionLog();
		}

		String plainTextResult = results.replaceAll( "<.*?>", LINE_BREAK );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, LINE_BREAK );
		String lastToken = null;

		Matcher damageMatcher = null;
		AdventureResult lastResult;

		if ( data == null && KoLCharacter.isUsingStabBat() )
		{
			damageMatcher = STABBAT_PATTERN.matcher( plainTextResult );

			if ( damageMatcher.find() )
			{
				String message = "You lose " + damageMatcher.group(1) + " hit points";

				RequestLogger.printLine( message );

				if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
					RequestLogger.updateSessionLog( message );

				this.parseResult( message );
			}

			damageMatcher = CARBS_PATTERN.matcher( plainTextResult );

			if ( damageMatcher.find() )
			{
				String message = "You lose " + damageMatcher.group(1) + " hit points";

				RequestLogger.printLine( message );

				if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
					RequestLogger.updateSessionLog( message );

				this.parseResult( message );
			}
		}

		if ( data == null )
		{
			damageMatcher = FUMBLE_PATTERN.matcher( plainTextResult );

			while ( damageMatcher.find() )
			{
				String message = "You lose " + damageMatcher.group(1) + " hit points";

				RequestLogger.printLine( message );

				if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
					RequestLogger.updateSessionLog( message );

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
				continue;

			if ( lastToken.startsWith( "You acquire" ) )
			{
				String acquisition = lastToken;

				if ( lastToken.indexOf( "effect" ) == -1 )
				{
					String item = parsedResults.nextToken();

					if ( lastToken.indexOf( "an item" ) != -1 )
					{
						if ( data == null )
						{
							RequestLogger.printLine( acquisition + " " + item );
							if ( KoLSettings.getBooleanProperty( "logAcquiredItems" ) )
								RequestLogger.updateSessionLog( acquisition + " " + item );
						}

						lastResult = this.parseItem( item );
						if ( data == null )
							this.processResult( lastResult );
						else
							AdventureResult.addResultToList( data, lastResult );
					}
					else
					{
						// The name of the item follows the number
						// that appears after the first index.

						String countString = item.split( " " )[0];
						int spaceIndex = item.indexOf( " " );

						String itemName = spaceIndex == -1 ? item : item.substring( spaceIndex ).trim();
						boolean isNumeric = spaceIndex != -1;

						for ( int i = 0; isNumeric && i < countString.length(); ++i )
							isNumeric &= Character.isDigit( countString.charAt(i) ) || countString.charAt(i) == ',';

						if ( !isNumeric )
							countString = "1";
						else if ( itemName.equals( "evil golden arches" ) )
							itemName = "evil golden arch";

						RequestLogger.printLine( acquisition + " " + item );

						if ( KoLSettings.getBooleanProperty( "logAcquiredItems" ) )
							RequestLogger.updateSessionLog( acquisition + " " + item );

						lastResult = this.parseItem( itemName + " (" + countString + ")" );
						if ( data == null )
							this.processResult( lastResult );
						else
							AdventureResult.addResultToList( data, lastResult );
					}
				}
				else if ( data == null )
				{
					String effectName = parsedResults.nextToken();
					lastToken = parsedResults.nextToken();

					RequestLogger.printLine( acquisition + " " + effectName + " " + lastToken );

					if ( KoLSettings.getBooleanProperty( "logStatusEffects" ) )
						RequestLogger.updateSessionLog( acquisition + " " + effectName + " " + lastToken );

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
			}
			else if ( (lastToken.startsWith( "You gain" ) || lastToken.startsWith( "You lose " )) )
			{
				int periodIndex = lastToken.indexOf( "." );
				if ( periodIndex != -1 )
					lastToken = lastToken.substring( 0, periodIndex );

				int parenIndex = lastToken.indexOf( "(" );
				if ( parenIndex != -1 )
					lastToken = lastToken.substring( 0, parenIndex );

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
								RequestLogger.updateSessionLog( lastToken );
						}
						else if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
							RequestLogger.updateSessionLog( lastToken );

					}
					else if ( lastResult.getName().equals( AdventureResult.MEAT ) )
					{
						AdventureResult.addResultToList( data, lastResult );
						if ( KoLSettings.getBooleanProperty( "logGainMessages" ) )
							RequestLogger.updateSessionLog( lastToken );
					}
				}
			}
			else if ( lastToken.startsWith( "You discard" ) )
			{
				Matcher matcher = DISCARD_PATTERN.matcher( lastToken );
				if ( matcher.find() )
				{
					AdventureResult item = new AdventureResult( matcher.group(1), -1, false );
					AdventureResult.addResultToList( inventory, item );
					AdventureResult.addResultToList( tally, item );
				}
			}
		}

		applyEffects();
		return requiresRefresh;
	}

	public void makeRequest( Runnable request )
	{	this.makeRequest( request, 1 );
	}

	/**
	 * Makes the given request for the given number of iterations,
	 * or until continues are no longer possible, either through
	 * user cancellation or something occuring which prevents the
	 * requests from resuming.
	 *
	 * @param	request	The request made by the user
	 * @param	iterations	The number of times the request should be repeated
	 */

	public void makeRequest( Runnable request, int iterations )
	{
		try
		{
			// Before anything happens, make sure that you are in
			// in a valid continuation state.

			boolean wasAdventuring = isAdventuring;

			// Handle the gym, which is the only adventure type
			// which needs to be specially handled.

			if ( request instanceof KoLAdventure )
			{
				currentAdventure = (KoLAdventure) request;

				if ( currentAdventure.getRequest() instanceof ClanGymRequest )
				{
					RequestThread.postRequest( ((ClanGymRequest)currentAdventure.getRequest()).setTurnCount( iterations ) );
					return;
				}

				if ( !(currentAdventure.getRequest() instanceof CampgroundRequest) && KoLCharacter.getCurrentHP() == 0 )
					this.recoverHP();

				if ( !KoLmafia.permitsContinue() )
					return;

				isAdventuring = true;
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
				isAdventuring = false;
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

	private boolean handleConditions( AdventureResult [] items, ItemCreationRequest [] creatables  )
	{
		if ( conditions.isEmpty() )
			return true;

		boolean shouldCreate = false;

		for ( int i = 0; i < creatables.length && !shouldCreate; ++i )
			shouldCreate |= creatables[i] != null && creatables[i].getQuantityPossible() >= items[i].getCount();

		// In theory, you could do a real validation by doing a full
		// dependency search.  While that's technically better, it's
		// also not very useful.

		for ( int i = 0; i < creatables.length && shouldCreate; ++i )
			shouldCreate &= creatables[i] == null || creatables[i].getQuantityPossible() >= items[i].getCount();

		// Create any items which are creatable.

		if ( shouldCreate )
		{
			for ( int i = creatables.length - 1; i >= 0; --i )
			{
				if ( creatables[i] != null && creatables[i].getQuantityPossible() >= items[i].getCount() )
				{
					creatables[i].setQuantityNeeded( items[i].getCount() );
					RequestThread.postRequest( creatables[i] );
					creatables[i] = null;
				}
			}
		}

		// If the conditions existed and have been satisfied,
		// then you should stop.

		return conditions.isEmpty();
	}

	private void executeRequest( Runnable request, int iterations, boolean wasAdventuring )
	{
		hadPendingState = false;

		boolean isCheckExempt = !(request instanceof KoLAdventure) || ((KoLAdventure)request).getRequest() instanceof CampgroundRequest ||
			KoLCharacter.getInebriety() > 25 || ((KoLAdventure)request).getZone().equals( "Holiday" );

		if ( KoLCharacter.isFallingDown() && !isCheckExempt )
		{
			updateDisplay( ERROR_STATE, "You are too drunk to continue." );
			return;
		}

		// Check to see if there are any end conditions.  If
		// there are conditions, be sure that they are checked
		// during the iterations.

		boolean hadConditions = !conditions.isEmpty();
		int adventuresBeforeRequest = 0;

		// Begin the adventuring process, or the request execution
		// process (whichever is applicable).

		int currentIteration = 0;

		AdventureResult [] items = new AdventureResult[ conditions.size() ];
		ItemCreationRequest [] creatables = new ItemCreationRequest[ conditions.size() ];

		for ( int i = 0; i < conditions.size(); ++i )
		{
			items[i] = (AdventureResult) conditions.get(i);
			creatables[i] = ItemCreationRequest.getInstance( items[i].getItemId() );
		}

		// Turn on auto-attack in order to save server hits if the
		// player isn't using custom combat.

		forceContinue();
		int currentIterationCount = 0;

		while ( permitsContinue() && ++currentIteration <= iterations )
		{
			if ( currentIterationCount > 4 )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "Internal error.  Please restart KoLmafia." );
				break;
			}

			// Account for the possibility that you could have run
			// out of adventures mid-request.

			if ( KoLCharacter.getAdventuresLeft() == 0 && request instanceof KoLAdventure )
				break;

			// See if you can create anything to satisfy your item
			// conditions, but only do so if it's an adventure.

			if ( request instanceof KoLAdventure )
			{
				if ( hadConditions && this.handleConditions( items, creatables ) )
				{
					conditions.clear();

					int bountyItem = KoLSettings.getIntegerProperty( "currentBountyItem" );
					if ( bountyItem != 0 && AdventureDatabase.getBountyLocation( TradeableItemDatabase.getItemName( bountyItem ) ) == request )
						RequestThread.postRequest( new KoLRequest( "bhh.php" ) );

					updateDisplay( PENDING_STATE, "Conditions satisfied after " + (currentIteration - 1) +
						((currentIteration == 2) ? " request." : " requests.") );

					break;
				}

				if ( ((KoLAdventure)request).getRequest() instanceof SewerRequest )
				{
					if ( SewerRequest.GUM.getCount( inventory ) == 0 )
					{
						int stopCount = 0;
						AdventureResult currentCondition;
						for ( int i = 0; i < conditions.size(); ++i )
						{
							currentCondition = (AdventureResult) conditions.get(i);
							if ( currentCondition.isItem() )
								stopCount += currentCondition.getCount();
						}

						int gumAmount = stopCount == 0 ? iterations : Math.min( stopCount, iterations );
						if ( !AdventureDatabase.retrieveItem( SewerRequest.GUM.getInstance( gumAmount ) ) )
							return;
					}
				}

				// Otherwise, disable the display and update the user
				// and the current request number.  Different requests
				// have different displays.  They are handled here.

				if ( iterations > 1 )
					currentIterationString = "Request " + currentIteration + " of " + iterations + " (" + request.toString() + ") in progress...";
				else
					currentIterationString = "Visit to " + request.toString() + " in progress...";
			}
			else if ( request instanceof CampgroundRequest )
			{
				currentIterationString = "Canpground request " + currentIteration + " of " + iterations + " in progress...";
			}

			if ( refusesContinue() )
				break;

			adventuresBeforeRequest = KoLCharacter.getAdventuresLeft();

			if ( request instanceof KoLAdventure && !wasAdventuring )
				AdventureFrame.updateRequestMeter( currentIteration - 1, iterations );

			RequestLogger.printLine();

			if ( request instanceof KoLRequest )
				RequestThread.postRequest( (KoLRequest) request );
			else if ( request instanceof KoLAdventure )
				RequestThread.postRequest( (KoLAdventure) request );
			else
				request.run();

			RequestLogger.printLine();

			// Decrement the counter to null out the increment
			// effect on the next iteration of the loop.

			if ( request instanceof KoLAdventure && adventuresBeforeRequest == KoLCharacter.getAdventuresLeft() )
			{
				--currentIteration;
				++currentIterationCount;
			}
			else
				currentIterationCount = 0;

			// Prevent drunkenness adventures from occurring by
			// testing inebriety levels after the request is run.

			if ( KoLCharacter.isFallingDown() && !isCheckExempt )
			{
				updateDisplay( ERROR_STATE, "You are too drunk to continue." );
				return;
			}
		}

		if ( request instanceof KoLAdventure )
			currentIterationString = "";

		if ( request instanceof KoLAdventure )
			AdventureFrame.updateRequestMeter( 1, 1 );

		// If you've completed the requests, make sure to update
		// the display.

		if ( permitsContinue() && !isRunningBetweenBattleChecks() )
		{
			if ( request instanceof KoLAdventure && !conditions.isEmpty() )
			{
				updateDisplay( ERROR_STATE, "Conditions not satisfied after " + (currentIteration - 1) +
					((currentIteration == 2) ? " adventure." : " adventures.") );
			}
		}
		else if ( continuationState == PENDING_STATE )
		{
			hadPendingState = true;
			forceContinue();
		}

		if ( request instanceof KoLAdventure )
			KoLRequest.printTotalDelay();
	}

	/**
	 * Makes a request which attempts to zap the chosen item
	 */

	public void makeZapRequest()
	{
		if ( KoLCharacter.getZapper() == null )
			return;

		AdventureResult selectedValue = (AdventureResult) getSelectedValue( "Let's explodey my wand!", inventory );
		if ( selectedValue == null )
			return;

		RequestThread.postRequest( new ZapRequest( selectedValue ) );
	}

	private static final Object getSelectedValue( String message, LockableListModel list )
	{	return KoLFrame.input( message, list );
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	public void makeHermitRequest()
	{
		if ( !hermitItems.contains( SewerRequest.CLOVER ) )
			RequestThread.postRequest( new HermitRequest() );

		if ( !permitsContinue() )
			return;

		AdventureResult selectedValue = (AdventureResult) getSelectedValue( "I have worthless items!", hermitItems );
		if ( selectedValue == null )
			return;

		int selected = selectedValue.getItemId();

		String message = "(You have " + HermitRequest.getWorthlessItemCount() + " worthless items)";
		int maximumValue = HermitRequest.getWorthlessItemCount();

		if ( selected == SewerRequest.CLOVER.getItemId() )
		{
			int cloverCount = ((AdventureResult)selectedValue).getCount();

			if ( cloverCount <= maximumValue )
			{
				message = "(There are " + cloverCount + " clovers still available)";
				maximumValue = cloverCount;
			}
		}

		int tradeCount = KoLFrame.getQuantity( "How many " + ((AdventureResult)selectedValue).getName() + " to get?\n" + message, maximumValue, 1 );
		if ( tradeCount == 0 )
			return;

		RequestThread.postRequest( new HermitRequest( selected, tradeCount ) );
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	public void makeTrapperRequest()
	{
		AdventureResult selectedValue = (AdventureResult) getSelectedValue( "I want skins!", trapperItems );
		if ( selectedValue == null )
			return;

		int selected = selectedValue.getItemId();
		int maximumValue = CouncilFrame.YETI_FUR.getCount( inventory );

		String message = "(You have " + maximumValue + " furs available)";
		int tradeCount = KoLFrame.getQuantity( "How many " + ((AdventureResult)selectedValue).getName() + " to get?\n" + message, maximumValue, maximumValue );
		if ( tradeCount == 0 )
			return;

		KoLmafia.updateDisplay( "Visiting the trapper..." );
		RequestThread.postRequest( new KoLRequest( "trapper.php?pwd&action=Yep.&whichitem=" + selected + "&qty=" + tradeCount ) );
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	public void makeHunterRequest()
	{
		KoLRequest hunterRequest = new KoLRequest( "bhh.php" );
		RequestThread.postRequest( hunterRequest );

		Matcher bountyMatcher = Pattern.compile( "name=whichitem value=(\\d+)" ).matcher( hunterRequest.responseText );

		LockableListModel bounties = new LockableListModel();
		while ( bountyMatcher.find() )
		{
			String item = TradeableItemDatabase.getItemName( StaticEntity.parseInt( bountyMatcher.group(1) ) );
			if ( item == null )
				continue;

			KoLAdventure location = AdventureDatabase.getBountyLocation( item );
			if ( location == null )
				continue;

			bounties.add( item + " (" + location.getAdventureName() + ")" );
		}

		if ( bounties.isEmpty() )
		{
			int bounty = KoLSettings.getIntegerProperty( "currentBountyItem" );
			if ( bounty == 0 )
			{
				updateDisplay( ERROR_STATE, "You're already on a bounty hunt." );
			}
			else
			{
				AdventureFrame.updateSelectedAdventure( AdventureDatabase.getBountyLocation(
					TradeableItemDatabase.getItemName( bounty ) ) );
			}

			return;
		}

		String selectedValue = (String) getSelectedValue( "Time to collect bounties!", bounties );
		if ( selectedValue == null )
			return;

		int itemId = TradeableItemDatabase.getItemId( selectedValue.substring( 0, selectedValue.lastIndexOf( "(" ) - 1 ) );
		RequestThread.postRequest( new KoLRequest( "bhh.php?pwd&action=takebounty&whichitem=" + itemId ) );
	}

	/**
	 * Makes a request to the hunter, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hunter.
	 */

	public void makeUntinkerRequest()
	{
		SortedListModel untinkerItems = new SortedListModel();

		for ( int i = 0; i < inventory.size(); ++i )
		{
			AdventureResult currentItem = (AdventureResult) inventory.get(i);
			int itemId = currentItem.getItemId();

			// Ignore silly fairy gravy + meat from yesterday recipe
			if ( itemId == MEAT_STACK )
				continue;

			// Otherwise, accept any COMBINE recipe
			if ( ConcoctionsDatabase.getMixingMethod( itemId ) == COMBINE )
				untinkerItems.add( currentItem );
		}

		if ( untinkerItems.isEmpty() )
		{
			updateDisplay( ERROR_STATE, "You don't have any untinkerable items." );
			return;
		}

		AdventureResult selectedValue = (AdventureResult) getSelectedValue( "You can unscrew meat paste?", untinkerItems );
		if ( selectedValue == null )
			return;

		RequestThread.postRequest( new UntinkerRequest( selectedValue.getItemId() ) );
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void makeMindControlRequest()
	{
		int maxLevel = 0;

		if ( KoLCharacter.inMysticalitySign() )
			maxLevel = 11;
		else if ( KoLCharacter.inMuscleSign() )
			maxLevel = 10;
		else if ( KoLCharacter.inMoxieSign() )
			maxLevel = 10;
		else
			return;

		String [] levelArray = new String[ maxLevel + 1 ];

		for ( int i = 0; i <= maxLevel; ++i )
			levelArray[i] = "Level " + i;

		int currentLevel = KoLCharacter.getSignedMLAdjustment();

		String selectedLevel = (String) KoLFrame.input( "Change monster annoyance from " + currentLevel + "?", levelArray );
		if ( selectedLevel == null )
			return;

		int setting = StaticEntity.parseInt( selectedLevel.split( " " )[1] );

		if ( KoLCharacter.inMysticalitySign() )
			RequestThread.postRequest( new MindControlRequest( setting ) );
		else if ( KoLCharacter.inMuscleSign() )
			RequestThread.postRequest( new DetunedRadioRequest( setting ) );
		else if ( KoLCharacter.inMoxieSign() )
			RequestThread.postRequest( new AnnoyotronRequest( setting ) );
	}

	public void makeCampgroundRestRequest()
	{
		String turnCount = (String) KoLFrame.input( "Rest for how many turns?", "1" );
		if ( turnCount == null )
			return;

		this.makeRequest( new CampgroundRequest( "rest" ), StaticEntity.parseInt( turnCount ) );
	}

	public void makeCampgroundRelaxRequest()
	{
		String turnCount = (String) KoLFrame.input( "Relax for how many turns?", "1" );
		if ( turnCount == null )
			return;

		this.makeRequest( new CampgroundRequest( "relax" ), StaticEntity.parseInt( turnCount ) );
	}

	public void makeClanSofaRequest()
	{
		String turnCount = (String) KoLFrame.input( "Sleep for how many turns?", "1" );
		if ( turnCount == null )
			return;

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

	public static final void addTavernLocation( KoLRequest request )
	{
		validateFaucetQuest();
		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getCurrentHP() == 0 )
			return;

		StringBuffer layout = new StringBuffer( KoLSettings.getUserProperty( "tavernLayout" ) );

		if ( request.getURLString().indexOf( "fight" ) != -1 )
		{
			int square = KoLSettings.getIntegerProperty( "lastTavernSquare" );
			if ( request.responseText != null )
				layout.setCharAt( square - 1, request.responseText.indexOf( "Baron" ) != -1 ? '4' : '1' );
		}
		else
		{
			String urlString = request.getURLString();
			if ( urlString.indexOf( "charpane" ) != -1 || urlString.indexOf( "chat" ) != -1 || urlString.equals( "rats.php" ) )
				return;

			Matcher squareMatcher = TAVERN_PATTERN.matcher( urlString );
			if ( !squareMatcher.find() )
				return;

			// Handle fighting rats.  If this was done through
			// the mini-browser, you'll have response text; else,
			// the response text will be null.

			int square = StaticEntity.parseInt( squareMatcher.group(1) );
			KoLSettings.setUserProperty( "lastTavernSquare", String.valueOf( square ) );

			char replacement = '1';
			if ( request.responseText != null && request.responseText.indexOf( "faucetoff" ) != -1 )
				replacement = '3';
			else if ( request.responseText != null && request.responseText.indexOf( "You acquire" ) != -1 )
				replacement = '2';

			layout.setCharAt( square - 1, replacement );
		}

		KoLSettings.setUserProperty( "tavernLayout", layout.toString() );
	}

	/**
	 * Completes the infamous tavern quest.
	 */

	public int locateTavernFaucet()
	{
		validateFaucetQuest();

		// Determine which elements have already been checked
		// so you don't check through them again.

		ArrayList searchList = new ArrayList();
		Integer searchIndex = null;

		for ( int i = 1; i <= 25; ++i )
			searchList.add( new Integer(i) );

		String visited = KoLSettings.getUserProperty( "tavernLayout" );
		for ( int i = visited.length() - 1; i >= 0; --i )
		{
			switch ( visited.charAt(i) )
			{
				case '0':
					break;
				case '1':
				case '2':
					searchList.remove(i);
					break;

				case '3':
				{
					int faucetRow = ((int) (i / 5)) + 1;
					int faucetColumn = i % 5 + 1;

					updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
					return i + 1;
				}

				case '4':
				{
					int baronRow = ((int) (i / 5)) + 1;
					int baronColumn = i % 5 + 1;

					updateDisplay( "Baron found in row " + baronRow + ", column " + baronColumn );
					return i + 1;
				}
			}
		}

		// If the faucet has not yet been found, then go through
		// the process of trying to locate it.

		KoLAdventure adventure = new KoLAdventure( "", "0", "0", "rats.php", "", "Typical Tavern (Pre-Rat)" );
		boolean foundFaucet = searchList.size() < 2;

		if ( KoLCharacter.getLevel() < 3 )
		{
			updateDisplay( ERROR_STATE, "You need to level up first." );
			return -1;
		}

		CouncilFrame.COUNCIL_VISIT.run();

		updateDisplay( "Searching for faucet..." );
		RequestThread.postRequest( adventure );

		// Random guess instead of straightforward search
		// for the location of the faucet (lowers the chance
		// of bad results if the faucet is near the end).

		while ( KoLmafia.permitsContinue() && !foundFaucet && KoLCharacter.getCurrentHP() > 0 && KoLCharacter.getAdventuresLeft() > 0 )
		{
			searchIndex = (Integer) searchList.remove( RNG.nextInt( searchList.size() ) );

			adventure.getRequest().addFormField( "where", searchIndex.toString() );
			RequestThread.postRequest( adventure );

			foundFaucet = adventure.getRequest().responseText != null &&
				adventure.getRequest().responseText.indexOf( "faucetoff" ) != -1;
		}

		// If you have not yet found the faucet, be sure
		// to set the settings so that your next attempt
		// does not repeat located squares.

		if ( !foundFaucet )
		{
			updateDisplay( ERROR_STATE, "Unable to find faucet." );
			return -1;
		}

		// Otherwise, you've found it!  So notify the user
		// that the faucet has been found.

		int faucetRow = (int) ((searchIndex.intValue() - 1) / 5) + 1;
		int faucetColumn = (searchIndex.intValue() - 1) % 5 + 1;

		updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
		return searchIndex.intValue();
	}

	/**
	 * Trades items with the guardian of the goud.
	 */

	public void tradeGourdItems()
	{
		KoLRequest gourdVisit = new KoLRequest( "town_right.php?place=gourd" );

		updateDisplay( "Determining items needed..." );
		RequestThread.postRequest( gourdVisit );

		// For every class, it's the same -- the message reads, "Bring back"
		// and then the number of the item needed.  Compare how many you need
		// with how many you have.

		Matcher neededMatcher = GOURD_PATTERN.matcher( gourdVisit.responseText );
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

		int neededCount = neededMatcher.find() ? StaticEntity.parseInt( neededMatcher.group(1) ) : 26;
		gourdVisit.addFormField( "action=gourd" );

		while ( neededCount <= 25 && neededCount <= item.getCount( inventory ) )
		{
			updateDisplay( "Giving up " + neededCount + " " + item.getName() + "s..." );
			RequestThread.postRequest( gourdVisit );
			this.processResult( item.getInstance( 0 - neededCount++ ) );
		}

		int totalProvided = 0;
		for ( int i = 5; i < neededCount; ++i )
			totalProvided += i;

		updateDisplay( "Gourd trading complete (" + totalProvided + " " + item.getName() + "s given so far)." );
	}

	public void unlockGuildStore()
	{	this.unlockGuildStore( false );
	}

	public void unlockGuildStore( boolean stopAtPaco )
	{
		KoLRequest guildVisit = new KoLRequest( "town_right.php?place=gourd" );

		// The wiki claims that your prime stats are somehow connected,
		// but the exact procedure is uncertain.  Therefore, just allow
		// the person to attempt to unlock their store, regardless of
		// their current stats.

		updateDisplay( "Entering guild challenge area..." );
		RequestThread.postRequest( guildVisit.constructURLString( "guild.php?place=challenge" ) );

		boolean success = stopAtPaco ? guildVisit.responseText.indexOf( "paco" ) != -1 :
			guildVisit.responseText.indexOf( "store.php" ) != -1;

		guildVisit.constructURLString( "guild.php?action=chal" );
		updateDisplay( "Completing guild tasks..." );

		for ( int i = 0; i < 6 && !success && KoLCharacter.getAdventuresLeft() > 0 && permitsContinue(); ++i )
		{
			RequestThread.postRequest( guildVisit );

			if ( guildVisit.responseText != null )
			{
				success |= stopAtPaco ? guildVisit.responseText.indexOf( "paco" ) != -1 :
					guildVisit.responseText.indexOf( "You've already beaten" ) != -1;
			}
		}

		if ( success )
			RequestThread.postRequest( guildVisit.constructURLString( "guild.php?place=paco" ) );

		if ( success && stopAtPaco )
			updateDisplay( "You have unlocked the guild meatcar quest." );
		else if ( success )
			updateDisplay( "Guild store successfully unlocked." );
		else
			updateDisplay( "Guild store was not unlocked." );
	}

	public void priceItemsAtLowestPrice()
	{
		RequestThread.openRequestSequence();
		RequestThread.postRequest( new StoreManageRequest() );

		SoldItem [] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		int [] itemId = new int[ sold.length ];
		int [] prices = new int[ sold.length ];
		int [] limits = new int[ sold.length ];

		// Now determine the desired prices on items.

		for ( int i = 0; i < sold.length; ++i )
		{
			itemId[i] = sold[i].getItemId();
			limits[i] = sold[i].getLimit();

			int minimumPrice = Math.max( 100, TradeableItemDatabase.getPriceById( sold[i].getItemId() ) * 2 );
			int desiredPrice = Math.max( minimumPrice, sold[i].getLowest() - sold[i].getLowest() % 100 );

			if ( sold[i].getPrice() == 999999999 && desiredPrice > 100 )
				prices[i] = desiredPrice;
			else
				prices[i] = sold[i].getPrice();
		}

		RequestThread.postRequest( new StoreManageRequest( itemId, prices, limits ) );
		updateDisplay( "Repricing complete." );
		RequestThread.closeRequestSequence();
	}

	/**
	 * Show an HTML string to the user
	 */

	public abstract void showHTML( String location, String text );

	public static final boolean hadPendingState()
	{	return hadPendingState;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request
	 * is permitted by the or by current circumstances in-game.
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public static final boolean permitsContinue()
	{	return continuationState == CONTINUE_STATE;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request
	 * will be denied by the regardless of continue state reset,
	 * until the display is enable (ie: in an abort state).
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public static final boolean refusesContinue()
	{	return continuationState == ABORT_STATE;
	}

	/**
	 * Forces a continue state.  This should only be called when
	 * there is no doubt that a continue should occur.
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public static final void forceContinue()
	{	continuationState = CONTINUE_STATE;
	}

	/**
	 * Utility. This method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public static final void addSaveState( String username, String password )
	{
		try
		{
			String utfString = URLEncoder.encode( password, "UTF-8" );

			StringBuffer encodedString = new StringBuffer();
			char currentCharacter;
			for ( int i = 0; i < utfString.length(); ++i )
			{
				currentCharacter = utfString.charAt(i);
				switch ( currentCharacter )
				{
				case '-':  encodedString.append( "2D" );  break;
				case '.':  encodedString.append( "2E" );  break;
				case '*':  encodedString.append( "2A" );  break;
				case '_':  encodedString.append( "5F" );  break;
				case '+':  encodedString.append( "20" );  break;

				case '%':
					encodedString.append( utfString.charAt( ++i ) );
					encodedString.append( utfString.charAt( ++i ) );
					break;

				default:
					encodedString.append( Integer.toHexString( (int) currentCharacter ).toUpperCase() );
					break;
				}
			}

			KoLSettings.setGlobalProperty( username, "saveState", (new BigInteger( encodedString.toString(), 36 )).toString( 10 ) );
			if ( !saveStateNames.contains( username ) )
				saveStateNames.add( username );
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void removeSaveState( String loginname )
	{
		if ( loginname == null )
			return;

		saveStateNames.remove( loginname );
		KoLSettings.setGlobalProperty( loginname, "saveState", "" );
	}

	/**
	 * Utility. The method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public static final String getSaveState( String loginname )
	{
		try
		{
			String password = KoLSettings.getGlobalProperty( loginname, "saveState" );
			if ( password == null || password.length() == 0 || password.indexOf( "/" ) != -1 )
				return null;

			String hexString = (new BigInteger( password, 10 )).toString( 36 );
			StringBuffer utfString = new StringBuffer();
			for ( int i = 0; i < hexString.length(); ++i )
			{
				utfString.append( '%' );
				utfString.append( hexString.charAt(i) );
				utfString.append( hexString.charAt(++i) );
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

	public static final boolean checkRequirements( List requirements )
	{	return checkRequirements( requirements, true );
	}

	public static final boolean checkRequirements( List requirements, boolean retrieveItem )
	{
		AdventureResult [] requirementsArray = new AdventureResult[ requirements.size() ];
		requirements.toArray( requirementsArray );

		int actualCount = 0;

		// Check the items required for this quest,
		// retrieving any items which might be inside
		// of a closet somewhere.

		for ( int i = 0; i < requirementsArray.length; ++i )
		{
			if ( requirementsArray[i] == null )
				continue;

			if ( requirementsArray[i].isItem() && retrieveItem )
				AdventureDatabase.retrieveItem( requirementsArray[i] );

			if ( requirementsArray[i].isItem() )
			{
				// Items are validated against the amount
				// currently in inventory.

				actualCount = requirementsArray[i].getCount( inventory );
			}
			else if ( requirementsArray[i].isStatusEffect() )
			{
				// Status effects should be compared against
				// the status effects list.

				actualCount = requirementsArray[i].getCount( activeEffects );
			}
			else if ( requirementsArray[i].getName().equals( AdventureResult.MEAT ) )
			{
				// Currency is compared against the amount
				// actually liquid.

				actualCount = KoLCharacter.getAvailableMeat();
			}

			if ( actualCount >= requirementsArray[i].getCount() )
				requirements.remove( requirementsArray[i] );
			else if ( actualCount > 0 )
				AdventureResult.addResultToList( requirements, requirementsArray[i].getInstance( 0 - actualCount ) );
		}

		// If there are any missing requirements
		// be sure to return false.  Otherwise,
		// you managed to get everything.

		return requirements.isEmpty();
	}

	public static final void printList( List printing )
	{	printList( printing, RequestLogger.INSTANCE );
	}

	public static final void printList( List printing, PrintStream ostream )
	{
		if ( printing == null || ostream == null )
			return;

		StringBuffer buffer = new StringBuffer();

		if ( printing != availableSkills )
		{
			Object current;
			for ( int i = 0; i < printing.size(); ++i )
			{
				current = printing.get(i);
				if ( current == null )
					continue;

				buffer.append( current.toString() );
				buffer.append( LINE_BREAK );
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

		buffer.setLength(0);
		ClassSkillsDatabase.generateSkillList( buffer, true );
		commandBuffer.append( buffer.toString() );
	}

	public void makePurchases( List results, int count )
	{	this.makePurchases( results, results.toArray(), count );
	}

	/**
	 * Utility method used to purchase the given number of items
	 * from the mall using the given purchase requests.
	 */

	public void makePurchases( List results, Object [] purchases, int maxPurchases )
	{
		if ( purchases.length == 0 )
			return;

		for ( int i = 0; i < purchases.length; ++i )
			if ( !(purchases[i] instanceof MallPurchaseRequest) )
				return;

		RequestThread.openRequestSequence();

		MallPurchaseRequest currentRequest = (MallPurchaseRequest) purchases[0];
		AdventureResult itemToBuy = new AdventureResult( currentRequest.getItemId(), 0 );

		int initialCount = itemToBuy.getCount( inventory );
		int currentCount = initialCount;
		int desiredCount = maxPurchases == Integer.MAX_VALUE ? Integer.MAX_VALUE : initialCount + maxPurchases;

		int previousLimit = 0;

		for ( int i = 0; i < purchases.length && currentCount < desiredCount && permitsContinue(); ++i )
		{
			currentRequest = (MallPurchaseRequest) purchases[i];

			if ( !KoLCharacter.canInteract() && currentRequest.getQuantity() != MallPurchaseRequest.MAX_QUANTITY )
				continue;

			// Keep track of how many of the item you had before
			// you run the purchase request

			previousLimit = currentRequest.getLimit();
			currentRequest.setLimit( Math.min( previousLimit, desiredCount - currentCount ) );
			RequestThread.postRequest( currentRequest );

			// Remove the purchase from the list!  Because you
			// have already made a purchase from the store

			if ( permitsContinue() )
			{
				if ( currentRequest.getQuantity() == currentRequest.getLimit() )
					results.remove( currentRequest );
				else if ( currentRequest.getQuantity() == MallPurchaseRequest.MAX_QUANTITY )
					currentRequest.setLimit( MallPurchaseRequest.MAX_QUANTITY );
				else
				{
					if ( currentRequest.getLimit() == previousLimit )
						currentRequest.setCanPurchase( false );

					currentRequest.setQuantity( currentRequest.getQuantity() - currentRequest.getLimit() );
					currentRequest.setLimit( previousLimit );
				}
			}
			else
				currentRequest.setLimit( previousLimit );

			// Now update how many you actually have for the next
			// iteration of the loop.

			currentCount = itemToBuy.getCount( inventory );
		}

		// With all that information parsed out, we should
		// refresh the lists at the very end.

		if ( itemToBuy.getCount( inventory ) >= desiredCount || maxPurchases == Integer.MAX_VALUE )
			updateDisplay( "Purchases complete." );
		else
			updateDisplay( "Desired purchase quantity not reached (wanted " + maxPurchases + ", got " +
				(currentCount - initialCount) + ")" );

		RequestThread.closeRequestSequence();
	}

	/**
	 * Utility. The method used to register a given adventure in the
	 * running adventure summary.
	 */

	public void registerAdventure( KoLAdventure adventureLocation )
	{
		String adventureName = adventureLocation.getAdventureName();
		if ( adventureName == null )
			return;

		this.recognizeEncounter( adventureName );
		RegisteredEncounter previousAdventure = (RegisteredEncounter) adventureList.lastElement();

		if ( previousAdventure != null && previousAdventure.name.equals( adventureName ) )
		{
			++previousAdventure.encounterCount;
			adventureList.set( adventureList.size() - 1, previousAdventure );
		}
		else
		{
			adventureList.add( new RegisteredEncounter( null, adventureName ) );
		}
	}

	public static final boolean isAutoStop( String encounterName )
	{	return stopEncounters.contains( encounterName );
	}

	public void recognizeEncounter( String encounterName )
	{
		if ( conditions.isEmpty() && isAutoStop( encounterName ) )
		{
			RequestLogger.printLine();
			KoLmafia.updateDisplay( PENDING_STATE, encounterName );
			RequestLogger.printLine();

			RequestThread.enableDisplayIfSequenceComplete();
		}
	}

	/**
	 * Utility. The method used to register a given encounter in the
	 * running adventure summary.
	 */

	public void registerEncounter( String encounterName, String encounterType )
	{
		encounterName = encounterName.trim();
		this.recognizeEncounter( encounterName );

		RegisteredEncounter [] encounters = new RegisteredEncounter[ encounterList.size() ];
		encounterList.toArray( encounters );

		for ( int i = 0; i < encounters.length; ++i )
		{
			if ( encounters[i].name.equals( encounterName ) )
			{
				++encounters[i].encounterCount;

				// Manually set to force repainting in GUI
				encounterList.set( i, encounters[i] );
				return;
			}
		}

		if ( encounterName.equalsIgnoreCase( "Cheetahs Never Lose" ) && KoLCharacter.hasItem( CATNIP ) )
			this.processResult( CATNIP.getNegation() );
		if ( encounterName.equalsIgnoreCase( "Summer Holiday" ) && KoLCharacter.hasItem( GLIDER ) )
			this.processResult( GLIDER.getNegation() );

		encounterList.add( new RegisteredEncounter( encounterType, encounterName ) );
	}

	private class RegisteredEncounter implements Comparable
	{
		private String type;
		private String name;
		private String stringform;
		private int encounterCount;

		public RegisteredEncounter( String type, String name )
		{
			this.type = type;
			this.name = name;

			this.stringform = type == null ? name : type + ": " + name;
			this.encounterCount = 1;
		}

		public String toString()
		{	return "<html>" + this.stringform + " (" + this.encounterCount + ")</html>";
		}

		public int compareTo( Object o )
		{
			if ( !(o instanceof RegisteredEncounter) || o == null )
				return -1;

			if ( this.type == null || ((RegisteredEncounter)o).type == null || this.type.equals( ((RegisteredEncounter)o).type ) )
				return this.name.compareToIgnoreCase( ((RegisteredEncounter)o).name );

			return this.type.equals( "Combat" ) ? 1 : -1;
		}
	}

	public KoLRequest getCurrentRequest()
	{	return currentRequest;
	}

	public void setCurrentRequest( KoLRequest request)
	{	currentRequest = request;
	}

	public final String [] extractTargets( String targetList )
	{
		// If there are no targets in the list, then
		// return absolutely nothing.

		if ( targetList == null || targetList.trim().length() == 0 )
			return new String[0];

		// Otherwise, split the list of targets, and
		// determine who all the unique targets are.

		String [] targets = targetList.trim().split( "\\s*,\\s*" );
		for ( int i = 0; i < targets.length; ++i )
			targets[i] = getPlayerId( targets[i] ) == null ? targets[i] :
				getPlayerId( targets[i] );

		// Sort the list in order to increase the
		// speed of duplicate detection.

		Arrays.sort( targets );

		// Determine who all the duplicates are.

		int uniqueListSize = targets.length;
		for ( int i = 1; i < targets.length; ++i )
		{
			if ( targets[i].equals( targets[ i - 1 ] ) )
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
			String [] uniqueList = new String[ uniqueListSize ];
			for ( int i = 0; i < targets.length; ++i )
				if ( targets[i] != null )
					uniqueList[ addedCount++ ] = targets[i];

			targets = uniqueList;
		}

		// Convert all the user Ids back to the
		// original player names so that the results
		// are easy to understand for the user.

		for ( int i = 0; i < targets.length; ++i )
			targets[i] = getPlayerName( targets[i] ) == null ? targets[i] :
				getPlayerName( targets[i] );

		// Sort the list one more time, this time
		// by player name.

		Arrays.sort( targets );

		// Parsing complete.  Return the list of
		// unique targets.

		return targets;
	}

	public final void downloadAdventureOverride()
	{
		for ( int i = 0; i < OVERRIDE_DATA.length; ++i )
		{
			updateDisplay( "Downloading " + OVERRIDE_DATA[i] + "..." );

			BufferedReader reader = KoLDatabase.getReader(
				"http://kolmafia.svn.sourceforge.net/viewvc/*checkout*/kolmafia/src/data/" + OVERRIDE_DATA[i] );

			File output = new File( DATA_LOCATION, OVERRIDE_DATA[i] );
			LogStream writer = LogStream.openStream( output, true );

			try
			{
				String line;

				while ( (line = reader.readLine()) != null )
					writer.println( line );

				writer.close();
				reader.close();
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				updateDisplay( ERROR_STATE, "Subversion service access failed for " + OVERRIDE_DATA[i] + "." );
				e.printStackTrace();

				writer.close();
				output.delete();

				RequestThread.closeRequestSequence();
				return;
			}
		}

		updateDisplay( "Please restart KoLmafia to complete the update." );
		RequestThread.enableDisplayIfSequenceComplete();
	}

	public static final boolean isRunningBetweenBattleChecks()
	{	return recoveryActive || MoodSettings.isExecuting();
	}

	public boolean runThresholdChecks()
	{
		float autoStopValue = KoLSettings.getFloatProperty( "autoAbortThreshold" );
		if ( autoStopValue >= 0.0f )
		{
			autoStopValue *= ((float) KoLCharacter.getMaximumHP());
			if ( KoLCharacter.getCurrentHP() <= autoStopValue )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "Health fell below " + ((int)autoStopValue) + ". Auto-abort triggered." );
				return false;
			}
		}

		return true;
	}

	public void runBetweenBattleChecks( boolean isFullCheck )
	{	this.runBetweenBattleChecks( isFullCheck, isFullCheck, true, isFullCheck );
	}

	public void runBetweenBattleChecks( boolean isScriptCheck, boolean isMoodCheck, boolean isHealthCheck, boolean isManaCheck )
	{
		// Do not run between battle checks if you are in the middle
		// of your checks or if you have aborted.

		if ( recoveryActive || refusesContinue() )
			return;

		// First, run the between battle script defined by the
		// user, which may make it so that none of the built
		// in behavior needs to run.

		RequestThread.openRequestSequence();
		recoveryActive = true;

		if ( isScriptCheck )
		{
			String scriptPath = KoLSettings.getUserProperty( "betweenBattleScript" );
			if ( !scriptPath.equals( "" ) )
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( scriptPath );
		}

		recoveryActive = false;
		SpecialOutfit.createImplicitCheckpoint();
		recoveryActive = true;

		// Now, run the built-in behavior to take care of
		// any loose ends.

		if ( isMoodCheck )
			MoodSettings.execute();

		if ( isHealthCheck )
			this.recoverHP();

		if ( isMoodCheck )
			MoodSettings.burnExtraMana( false );

		if ( isManaCheck )
			this.recoverMP();

		recoveryActive = false;
		SpecialOutfit.restoreImplicitCheckpoint();
		RequestThread.closeRequestSequence();

		if ( KoLCharacter.getCurrentHP() == 0 )
			updateDisplay( ABORT_STATE, "Insufficient health to continue (auto-abort triggered)." );

		if ( permitsContinue() && currentIterationString.length() > 0 )
		{
			RequestLogger.printLine();
			updateDisplay( currentIterationString );
			currentIterationString = "";
		}
	}

	public void startRelayServer()
	{
		if ( LocalRelayServer.isRunning() )
			return;

		LocalRelayServer.startThread();

		// Wait for 5 seconds before giving up
		// on the relay server.

		for ( int i = 0; i < 50 && !LocalRelayServer.isRunning(); ++i )
			KoLRequest.delay( 500 );

		if ( !LocalRelayServer.isRunning() )
			return;
	}

	public void openRelayBrowser()
	{
		if ( KoLRequest.sessionId == null )
			openRelayBrowser( "login.php", false );
		else if ( KoLRequest.isCompactMode )
			openRelayBrowser( "main_c.html", false );
		else
			openRelayBrowser( "main.html", false );
	}

	public void openRelayBrowser( String location )
	{	openRelayBrowser( location, false );
	}

	public void openRelayBrowser( String location, boolean forceMain )
	{
		this.startRelayServer();

		if ( !forceMain )
		{
			StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + "/" + location );
		}
		else
		{
			LocalRelayRequest.setNextMain( location );
			openRelayBrowser();
		}
	}

	public void launchRadioKoL()
	{
		if ( KoLSettings.getBooleanProperty( "useLowBandwidthRadio" ) )
			StaticEntity.openSystemBrowser( "http://209.9.238.5:8792/listen.pls" );
		else
			StaticEntity.openSystemBrowser( "http://209.9.238.5:8794/listen.pls" );
	}

	public void launchSimulator()
	{
		this.startRelayServer();
		StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + "/simulator/index.html" );
	}

	public static final boolean isAdventuring()
	{	return isAdventuring;
	}

	public void removeAllItemsFromStore()
	{
		RequestThread.openRequestSequence();
		RequestThread.postRequest( new StoreManageRequest() );

		// Now determine the desired prices on items.
		// If the value of an item is currently 100,
		// then remove the item from the store.

		SoldItem [] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		for ( int i = 0; i < sold.length && permitsContinue(); ++i )
			RequestThread.postRequest( new StoreManageRequest( sold[i].getItemId() ) );

		updateDisplay( "Store emptying complete." );
		RequestThread.closeRequestSequence();
	}

	/**
	 * Hosts a massive sale on the items currently in your store.
	 * Utilizes the "minimum meat" principle.
	 */

	public void makeEndOfRunSaleRequest()
	{
		if ( !KoLCharacter.canInteract() )
		{
			updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
			return;
		}

		if ( !KoLFrame.confirm( "Are you sure you'd like to host an end-of-run sale?" ) )
			return;

		// Find all tradeable items.  Tradeable items
		// are marked by an autosell value of nonzero.

		RequestThread.openRequestSequence();

		// Only place items in the mall which are not
		// sold in NPC stores and can be autosold.

		AdventureResult [] items = new AdventureResult[ inventory.size() ];
		inventory.toArray( items );

		ArrayList autosell = new ArrayList();
		ArrayList automall = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			if ( items[i].getItemId() == MEAT_PASTE || items[i].getItemId() == MEAT_STACK || items[i].getItemId() == DENSE_STACK )
				continue;

			if ( !TradeableItemDatabase.isTradeable( items[i].getItemId() ) )
				continue;

			if ( TradeableItemDatabase.getPriceById( items[i].getItemId() ) <= 0 )
				continue;

			if ( NPCStoreDatabase.contains( items[i].getName(), false ) )
				autosell.add( items[i] );
			else
				automall.add( items[i] );
		}

		// Now, place all the items in the mall at the
		// maximum possible price.  This allows KoLmafia
		// to determine the minimum price.

		if ( autosell.size() > 0 && permitsContinue() )
			RequestThread.postRequest( new AutoSellRequest( autosell.toArray(), AutoSellRequest.AUTOSELL ) );

		if ( automall.size() > 0 && permitsContinue() )
			RequestThread.postRequest( new AutoSellRequest( automall.toArray(), AutoSellRequest.AUTOMALL ) );

		// Now, remove all the items that you intended
		// to remove from the store due to pricing issues.

		if ( permitsContinue() )
			this.priceItemsAtLowestPrice();

		updateDisplay( "Undercutting sale complete." );
		RequestThread.closeRequestSequence();
	}

	public void makeAutoMallRequest()
	{
		// Now you've got all the items used up, go ahead and prepare to
		// sell anything that's left.

		int itemCount;

		AdventureResult currentItem;
		Object [] items = profitableList.toArray();

		ArrayList sellList = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = (AdventureResult) items[i];

			if ( mementoList.contains( currentItem ) )
				continue;

			if ( currentItem.getItemId() == MEAT_PASTE || currentItem.getItemId() == MEAT_STACK || currentItem.getItemId() == DENSE_STACK )
				continue;

			itemCount = currentItem.getCount( inventory );

			if ( itemCount > 0 )
				sellList.add( currentItem.getInstance( itemCount ) );
		}

		if ( !sellList.isEmpty() )
			RequestThread.postRequest( new AutoSellRequest( sellList.toArray(), AutoSellRequest.AUTOMALL ) );

		RequestThread.closeRequestSequence();
	}

	public void makeJunkRemovalRequest()
	{
		int itemCount;
		AdventureResult currentItem;

		Object [] items = junkList.toArray();

		// Before doing anything else, go through the list of items which are
		// traditionally used and use them.  Also, if the item can be untinkered,
		// it's usually more beneficial to untinker first.

		boolean madeUntinkerRequest = false;
		boolean canUntinker = UntinkerRequest.canUntinker();

		RequestThread.openRequestSequence();

		do
		{
			madeUntinkerRequest = false;

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[i];
				itemCount = currentItem.getCount( inventory );

				if ( itemCount == 0 )
					continue;

				if ( canUntinker && ConcoctionsDatabase.getMixingMethod( currentItem.getItemId() ) == COMBINE )
				{
					RequestThread.postRequest( new UntinkerRequest( currentItem.getItemId() ) );
					madeUntinkerRequest = true;
					continue;
				}

				switch ( currentItem.getItemId() )
				{
				case 184:	// briefcase
				case 533:	// Gnollish toolbox
				case 553:	// 31337 scroll
				case 604:	// Penultimate fantasy chest
				case 831:	// small box
				case 832:	// large box
				case 1768:	// Gnomish toolbox
				case 1917:	// old leather wallet
				case 1918:	// old coin purse
				case 2057:	// black pension check
				case 2058:	// black picnic basket
				case 2511:	// Frat Army FGF
				case 2512:	// Hippy Army MPE
				case 2536:	// canopic jar
				case 2612:	// ancient vinyl coin purse
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
				currentItem = (AdventureResult) items[i];

				if ( mementoList.contains( currentItem ) )
					continue;

				if ( currentItem.getName().startsWith( "antique" ) )
					continue;

				itemCount = currentItem.getCount( inventory );
				if ( !KoLCharacter.canInteract() && singletonList.contains( currentItem ) && !closet.contains( currentItem ) )
					--itemCount;

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

						if ( KoLCharacter.hasItem( ConcoctionsDatabase.HAMMER ) && itemPower >= 100 || (hasMalusAccess && itemPower > 10) )
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );

						break;

					case EQUIP_FAMILIAR:
					case EQUIP_ACCESSORY:

						if ( KoLCharacter.hasItem( ConcoctionsDatabase.HAMMER ) )
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );

						break;

					default:

						if ( currentItem.getName().endsWith( "powder" ) || currentItem.getName().endsWith( "nugget" ) )
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );

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
			currentItem = (AdventureResult) items[i];

			if ( mementoList.contains( currentItem ) )
				continue;

			if ( !KoLCharacter.canInteract() && singletonList.contains( currentItem ) && !closet.contains( currentItem ) )
				continue;

			if ( currentItem.getItemId() == MEAT_PASTE )
				continue;

			itemCount = currentItem.getCount( inventory );
			if ( itemCount > 0 )
				sellList.add( currentItem.getInstance( itemCount ) );
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
				currentItem = (AdventureResult) items[i];

				if ( mementoList.contains( currentItem ) || !singletonList.contains( currentItem ) )
					continue;

				if ( currentItem.getItemId() == MEAT_PASTE )
					continue;

				itemCount = currentItem.getCount( inventory ) - 1;
				if ( itemCount > 0 )
					sellList.add( currentItem.getInstance( itemCount ) );
			}

			if ( !sellList.isEmpty() )
				RequestThread.postRequest( new AutoSellRequest( sellList.toArray(), AutoSellRequest.AUTOSELL ) );
		}

		RequestThread.closeRequestSequence();
	}

	public void handleAscension()
	{
		RequestThread.openRequestSequence();
		KoLSettings.setUserProperty( "lastBreakfast", "-1" );

		resetCounters();
		KoLCharacter.reset();

		this.refreshSession( false );
		this.resetSession();
		conditions.clear();

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
			sessionStream.print( "Hardcore " );
		else
			sessionStream.print( "Softcore " );

		if ( KoLCharacter.canEat() && KoLCharacter.canDrink() )
			sessionStream.print( "No-Path " );
		else if ( KoLCharacter.canEat() )
			sessionStream.print( "Teetotaler " );
		else if ( KoLCharacter.canDrink() )
			sessionStream.print( "Boozetafarian " );
		else
			sessionStream.print( "Oxygenarian " );

		sessionStream.println( KoLCharacter.getClassType() );
		sessionStream.println();
		sessionStream.println();

		printList( availableSkills, sessionStream );
		sessionStream.println();
		sessionStream.println();
		sessionStream.println();

		RequestThread.closeRequestSequence();
	}

	private static class ShutdownThread extends Thread
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
				SESSION_HOLDER.release();
				SESSION_CHANNEL.close();
				SESSION_FILE.delete();
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
