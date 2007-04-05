/**
 * Copyright (c) 2005-2007, KoLmafia development team
 * http://sourceforge.net/
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
import java.awt.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.math.BigInteger;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import javax.swing.UIManager.LookAndFeelInfo;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
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

		CombatSettings.restoreDefaults();
		MoodSettings.restoreDefaults();
	}

	private static boolean hadPendingState = false;

	private static final String [] OVERRIDE_DATA =
	{
		"adventures.txt", "buffbots.txt", "classskills.txt", "combats.txt", "concoctions.txt",
		"equipment.txt", "familiars.txt", "fullness.txt", "inebriety.txt", "itemdescs.txt",
		"modifiers.txt", "monsters.txt", "npcstores.txt", "outfits.txt", "packages.txt",
		"spleenhit.txt", "statuseffects.txt", "tradeitems.txt", "zonelist.txt"
	};

	private static String currentIterationString = "";
	protected static boolean recoveryActive = false;

	public static boolean isMakingRequest = false;
	public static KoLRequest currentRequest = null;
	public static int continuationState = CONTINUE_STATE;

	public static int [] initialStats = new int[3];

	public static boolean executedLogin = false;
	public static boolean useDisjunction = false;

	private static final Pattern FUMBLE_PATTERN = Pattern.compile( "You drop your .*? on your .*?, doing ([\\d,]+) damage" );
	private static final Pattern STABBAT_PATTERN = Pattern.compile( " stabs you for ([\\d,]+) damage" );
	private static final Pattern CARBS_PATTERN = Pattern.compile( "some of your blood, to the tune of ([\\d,]+) damage" );
	private static final Pattern TAVERN_PATTERN = Pattern.compile( "where=(\\d+)" );
	private static final Pattern GOURD_PATTERN = Pattern.compile( "Bring back (\\d+)" );

	private static final AdventureResult CATNIP = new AdventureResult( 1486, 1 );
	private static final AdventureResult GLIDER = new AdventureResult( 1487, 1 );
	public static final AdventureResult SATCHEL = new AdventureResult( 1656, 1 );
	public static final AdventureResult NOVELTY_BUTTON = new AdventureResult( 2072, 1 );

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
	}

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafiaGUI</code>.
	 */

	public static void main( String [] args )
	{
		Runtime.getRuntime().addShutdownHook( new ShutdownThread() );

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
		hermitItems.add( new AdventureResult( "dingy planks", 1, false ) );
		hermitItems.add( new AdventureResult( "golden twig", 1, false ) );
		hermitItems.add( new AdventureResult( "hot buttered roll", 1, false ) );
		hermitItems.add( new AdventureResult( "jaba\u00f1ero pepper", 1, false ) );
		hermitItems.add( new AdventureResult( "ketchup", 1, false ) );
		hermitItems.add( new AdventureResult( "sweet rims", 1, false ) );
		hermitItems.add( new AdventureResult( "volleyball", 1, false ) );
		hermitItems.add( new AdventureResult( "wooden figurine", 1, false ) );

		// Change it so that it doesn't recognize daylight savings in order
		// to ensure different localizations work.

		TimeZone koltime = (TimeZone) TimeZone.getDefault().clone();
		koltime.setRawOffset( 1000 * 60 * 60 * -5 );
		DATED_FILENAME_FORMAT.setTimeZone( koltime );

		// Reload your settings and determine all the different users which
		// are present in your save state list.

		StaticEntity.reloadSettings( "" );
		KoLRequest.chooseRandomServer();

		StaticEntity.setProperty( "ignoreLoadBalancer", "false" );
		StaticEntity.setProperty( "relayBrowserOnly", "false" );

		String actualName;
		String [] pastUsers;

		String oldSaves = StaticEntity.getProperty( "saveState" );
		if ( !oldSaves.equals( "" ) )
		{
			pastUsers = oldSaves.split( "//" );
			for ( int i = 0; i < pastUsers.length; ++i )
			{
				actualName = StaticEntity.getGlobalProperty( pastUsers[i], "displayName" );
				if ( actualName.equals( "" ) && !pastUsers[i].equals( "" ) )
					StaticEntity.setGlobalProperty( pastUsers[i], "displayName", pastUsers[i] );
			}
		}

		pastUsers = StaticEntity.getPastUserList();

		for ( int i = 0; i < pastUsers.length; ++i )
		{
			actualName = StaticEntity.getGlobalProperty( pastUsers[i], "displayName" );
			if ( actualName.equals( "" ) )
				actualName = StaticEntity.globalStringReplace( pastUsers[i], "_", " " );

			saveStateNames.add( actualName );
		}

		// Also clear out any outdated data files.  Include the
		// adventure table, in case this is causing problems.

		String version = StaticEntity.getProperty( "previousUpdateVersion" );

		if ( version == null || !version.equals( VERSION_NAME ) )
		{
			StaticEntity.setProperty( "previousUpdateVersion", VERSION_NAME );
			for ( int i = 0; i < OVERRIDE_DATA.length; ++i )
			{
				File outdated = new File( DATA_LOCATION, OVERRIDE_DATA[i] );
				if ( outdated.exists() )
					outdated.delete();

				deleteSimulator( new File( "html/simulator" ) );
			}
		}

		if ( StaticEntity.getBooleanProperty( "useLargerFonts" ) )
			LimitedSizeChatBuffer.useLargerFonts();
		else
			LimitedSizeChatBuffer.useSmallerFonts();

		// Change the default look and feel to match the player's
		// preferences.  Always do this.

		String lookAndFeel = StaticEntity.getProperty( "swingLookAndFeel" );
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

		StaticEntity.setProperty( "swingLookAndFeel", lookAndFeel );
		System.setProperty( "spellcast.actionButtonsThreaded", StaticEntity.getProperty( "allowRequestQueueing" ) );

		if ( System.getProperty( "os.name" ).startsWith( "Win" ) || lookAndFeel.equals( UIManager.getCrossPlatformLookAndFeelClassName() ) )
		{
			UIManager.put( "ProgressBar.foreground", Color.black );
			UIManager.put( "ProgressBar.selectionForeground", Color.lightGray );

			UIManager.put( "ProgressBar.background", Color.lightGray );
			UIManager.put( "ProgressBar.selectionBackground", Color.black );
		}

		tab.CloseTabPaneEnhancedUI.selectedA = DataUtilities.toColor( StaticEntity.getProperty( "innerTabColor" ) );
		tab.CloseTabPaneEnhancedUI.selectedB = DataUtilities.toColor( StaticEntity.getProperty( "outerTabColor" ) );

		tab.CloseTabPaneEnhancedUI.notifiedA = DataUtilities.toColor( StaticEntity.getProperty( "innerChatColor" ) );
		tab.CloseTabPaneEnhancedUI.notifiedB = DataUtilities.toColor( StaticEntity.getProperty( "outerChatColor" ) );

		// Now run the main routines for each, so that
		// you have an interface.

		if ( useGUI )
			KoLmafiaGUI.main( args );
		else
			KoLmafiaCLI.main( args );

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

			DEFAULT_SHELL.executeLine( "call " + actualScript );
		}
		else if ( !useGUI )
		{
			// If you're not using the GUI thread, make sure that
			// you allow for an attempt to login.

			DEFAULT_SHELL.attemptLogin();
		}

		// Always read input from the command line when you're not
		// in GUI mode.

		if ( !useGUI )
			DEFAULT_SHELL.listenForCommands();
	}

	private static void deleteSimulator( File location )
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
	{	useDisjunction = false;
	}

	public static String getLastMessage()
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
		if ( continuationState == ABORT_STATE )
			return;

		if ( continuationState != PENDING_STATE )
			continuationState = state;

		RequestLogger.printLine( state, message );
		lastMessage = message;

		if ( !existingFrames.isEmpty() && message.indexOf( LINE_BREAK ) == -1 )
			updateDisplayState( state, RequestEditorKit.getStripped( message ) );
	}

	private static final void updateDisplayState( int state, String message )
	{
		// Next, update all of the panels with the
		// desired update message.

		WeakReference [] references = StaticEntity.getExistingPanels();
		for ( int i = 0; i < references.length; ++i )
		{
			if ( references[i].get() != null )
			{
				if ( references[i].get() instanceof KoLPanel && message != null && message.length() > 0 )
					((KoLPanel) references[i].get()).setStatusMessage( state, message );

				((Component)references[i].get()).setEnabled( state != CONTINUE_STATE );
			}
		}

		if ( message != null && message.length() > 0 )
			AdventureFrame.updateRequestMeter( message );

		KoLFrame [] frames = StaticEntity.getExistingFrames();
		for ( int i = 0; i < frames.length; ++i )
			frames[i].updateDisplayState( state );

		if ( KoLDesktop.instanceExists() )
			KoLDesktop.getInstance().updateDisplayState( state );
	}

	public static void enableDisplay()
	{
		updateDisplayState( continuationState == ABORT_STATE || continuationState == ERROR_STATE ? ERROR_STATE : ENABLE_STATE, "" );
		continuationState = CONTINUE_STATE;
	}

	public static boolean executedLogin()
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
		RequestThread.postRequest( CharpaneRequest.getInstance() );

		KoLmafia.updateDisplay( "Initializing session for " + username + "..." );

		StaticEntity.setProperty( "lastUsername", username );
		StaticEntity.reloadSettings( username );

		// Reset all per-player information when refreshing
		// your session via login.

		if ( originalName.equalsIgnoreCase( username ) )
		{
			KoLCharacter.reset();
		}
		else
		{
			KoLCharacter.reset( username );

			CombatSettings.restoreDefaults();
			MoodSettings.restoreDefaults();
			KoLMailManager.clearMailboxes();
			StoreManager.clearCache();
			MuseumManager.clearCache();
			ClanManager.clearCache();
		}

		// Now actually reset the session.

		refreshSession();
		RequestLogger.openSessionLog();
		resetSession();

		// If the password hash is non-null, then that means you
		// might be mid-transition.

		if ( KoLRequest.passwordHash != null && KoLRequest.passwordHash.equals( "" ) )
			return;

		int today = MoonPhaseDatabase.getPhaseStep();
		registerPlayer( username, String.valueOf( KoLCharacter.getUserId() ) );

		if ( StaticEntity.getGlobalProperty( username, "getBreakfast" ).equals( "true" ) )
		{
			if ( StaticEntity.getIntegerProperty( "lastBreakfast" ) != today )
			{
				StaticEntity.setProperty( "lastBreakfast", String.valueOf( today ) );
				getBreakfast( true );
			}
		}

		if ( StaticEntity.getIntegerProperty( "lastCounterDay" ) != today )
			resetCounters();

		// Also, do mushrooms, if a mushroom script has already
		// been setup by the user.

		if ( StaticEntity.getBooleanProperty( "autoPlant" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") ) )
		{
			String currentLayout = StaticEntity.getProperty( "plantingScript" );
			if ( !currentLayout.equals( "" ) && KoLCharacter.inMuscleSign() && MushroomPlot.ownsPlot() )
				DEFAULT_SHELL.executeLine( "call " + MushroomPlot.PLOT_DIRECTORY.getPath() + "/" + currentLayout + ".ash" );
		}
	}

	public static void resetCounters()
	{
		StaticEntity.setProperty( "lastCounterDay", String.valueOf( MoonPhaseDatabase.getPhaseStep() ) );
		StaticEntity.setProperty( "breakfastCompleted", "false" );

		StaticEntity.setProperty( "currentFullness", "0" );
		StaticEntity.setProperty( "currentSpleenUse", "0" );
		StaticEntity.setProperty( "currentPvpVictories", "" );

		StaticEntity.setProperty( "snowconeSummons", "0" );
		StaticEntity.setProperty( "grimoireSummons", "0" );
		StaticEntity.setProperty( "candyHeartSummons", "0" );

		StaticEntity.setProperty( "noodleSummons", "0" );
		StaticEntity.setProperty( "reagentSummons", "0" );
		StaticEntity.setProperty( "cocktailSummons", "0" );
	}

	public void getBreakfast( boolean checkSettings )
	{
		SpecialOutfit.createImplicitCheckpoint();

		if ( KoLCharacter.hasToaster() )
			for ( int i = 0; i < 3 && permitsContinue(); ++i )
				RequestThread.postRequest( new CampgroundRequest( "toast" ) );

		forceContinue();

		if ( KoLCharacter.hasArches() )
			RequestThread.postRequest( new CampgroundRequest( "arches" ) );

		forceContinue();

		if ( StaticEntity.getBooleanProperty( "visitRumpus" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") ) )
			RequestThread.postRequest( new ClanGymRequest( ClanGymRequest.SEARCH ) );

		forceContinue();

		castBreakfastSkills( checkSettings );

		forceContinue();

		SpecialOutfit.restoreImplicitCheckpoint();
	}

	public void castBreakfastSkills( boolean checkSettings )
	{	castBreakfastSkills( checkSettings, StaticEntity.getBooleanProperty( "loginRecovery" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") ) );
	}

	public boolean castBreakfastSkills( boolean checkSettings, boolean allowRestore )
	{
		if ( StaticEntity.getBooleanProperty( "breakfastCompleted" ) )
			return true;

		boolean shouldCast = false;
		boolean limitExceeded = true;

		String skillSetting = StaticEntity.getProperty( "breakfast" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") );
		boolean pathedSummons = StaticEntity.getBooleanProperty( "pathedSummons" + (KoLCharacter.isHardcore() ? "Hardcore" : "Softcore") );

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
					limitExceeded &= getBreakfast( UseSkillRequest.BREAKFAST_SKILLS[i], allowRestore );
			}
		}

		StaticEntity.setProperty( "breakfastCompleted", String.valueOf( limitExceeded ) );
		return limitExceeded;
	}

	public boolean getBreakfast( String skillName, boolean allowRestore )
	{
		UseSkillRequest summon = UseSkillRequest.getInstance( skillName );

		// Special handling for candy heart summoning.  This skill
		// can be extremely expensive, so rather than summoning to
		// your limit, designated by maximum MP, it merely swallows
		// all your current MP.

		if ( summon.getSkillId() == 18 )
		{
			summon.setBuffCount( 1 );
			while ( ClassSkillsDatabase.getMPConsumptionById( 18 ) <= KoLCharacter.getCurrentMP() )
				RequestThread.postRequest( summon );

			return true;
		}

		// For all other skills, if you don't need to cast them, then
		// skip this step.

		int maximumCast = summon.getMaximumCast();

		if ( maximumCast <= 0 )
			return true;

		int castCount = Math.min( maximumCast, allowRestore ? 5 :
			KoLCharacter.getCurrentMP() / ClassSkillsDatabase.getMPConsumptionById( ClassSkillsDatabase.getSkillId( skillName ) ) );

		if ( castCount == 0 )
			return false;

		summon.setBuffCount( castCount );
		RequestThread.postRequest( summon );

		return castCount == maximumCast;
	}

	public final void refreshSession()
	{
		isRefreshing = true;

		// Get current moon phases

		updateDisplay( "Refreshing session data..." );

		RequestThread.postRequest( new MoonPhaseRequest() );
		RequestThread.postRequest( new AccountRequest() );


		// Retrieve the character sheet first. It's necessary to do
		// this before concoctions have a chance to get refreshed.

		RequestThread.postRequest( new CharsheetRequest() );

		// Clear the violet fog path table and everything
		// else that changes on the player.

		VioletFog.reset();
		Louvre.reset();
		MushroomPlot.reset();

		// Retrieve the items which are available for consumption
		// and item creation.

		RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.CLOSET ) );

		// If the password hash is non-null, but is not available,
		// then that means you might be mid-transition.

		if ( KoLRequest.passwordHash != null && KoLRequest.passwordHash.equals( "" ) )
		{
			RequestThread.closeRequestSequence();
			return;
		}

		// Retrieve the list of familiars which are available to
		// the player.

		RequestThread.postRequest( new FamiliarRequest() );

		// Retrieve campground data to see if the user is able to
		// cook, make drinks or make toast.

		updateDisplay( "Retrieving campground data..." );

		RequestThread.postRequest( new CampgroundRequest() );
		RequestThread.postRequest( new ItemStorageRequest() );

		RequestThread.postRequest( CharpaneRequest.getInstance() );
		updateDisplay( "Session data refreshed." );

		isRefreshing = false;
	}

	public static boolean isRefreshing()
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
	 * Utilit.  This method to parse an individual adventuring result.
	 * Thi.  This method determines what the result actually was and
	 * adds it to the tally.
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

		return processResult( new AdventureResult( parsedEffectName, StaticEntity.parseInt( parsedDuration ), true ) );
	}

	/**
	 * Utilit.  This method used to process a result.  By default, this
	 .  This method will also add an adventure result to the tally directly.
	 * This is used whenever the nature of the result is already known
	 * and no additional parsing is needed.
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
			AdventureResult.addResultToList( tally, result.getNegation() );
		}
		else if ( result.isItem() || resultName.equals( AdventureResult.SUBSTATS ) || resultName.equals( AdventureResult.MEAT ) )
		{
			// If you gain a sock, you lose all the immateria

			if ( result.equals( KoLAdventure.SOCK ) && result.getCount() == 1 )
				for ( int i = 0; i < KoLAdventure.IMMATERIA.length; ++i )
					processResult( KoLAdventure.IMMATERIA[i] );

			AdventureResult.addResultToList( tally, result );
		}

		KoLCharacter.processResult( result );
		shouldRefresh |= result.getName().equals( AdventureResult.MEAT );

		// Process the adventure result through the conditions
		// list, removing it if the condition is satisfied.

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
				if ( result.getCount( inventory ) <= 0 )
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

	public static void applyEffects()
	{
		for ( int j = 0; j < recentEffects.size(); ++j )
			AdventureResult.addResultToList( activeEffects, (AdventureResult) recentEffects.get(j) );

		recentEffects.clear();
		activeEffects.sort();
	}

	/**
	 * Returns the string form of the player Id associated
	 * with the given player name.
	 *
	 * @param	playerId	The Id of the player
	 * @return	The player's name if it has been seen, or null if it has not
	 *          yet appeared in the chat (not likely, but possible).
	 */

	public static String getPlayerName( String playerId )
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

	public static String getPlayerId( String playerName )
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

	public static void registerPlayer( String playerName, String playerId )
	{
		playerName = playerName.replaceAll( "[^0-9A-Za-z_ ]", "" );
		String lowercase = playerName.toLowerCase();

		if ( !seenPlayerIds.containsKey( lowercase ) )
		{
			seenPlayerIds.put( lowercase, playerId );
			seenPlayerNames.put( playerId, playerName );
		}
	}

	public static void registerContact( String playerName, String playerId )
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
	 * Utilit.  This method which ensures that the amount needed exists,
	 * and if not, calls the appropriate scripts to do so.
	 */

	private final boolean recover( int desired, String settingName, String currentName, String maximumName, Object [] techniques ) throws Exception
	{
		// First, check for beaten up, if the person has tongue as an
		// auto-heal option.  This takes precedence over all other checks.

		String restoreSetting = StaticEntity.getProperty( settingName + "Items" ).trim().toLowerCase();

		if ( restoreSetting.indexOf( "tongue" ) != -1 && activeEffects.contains( KoLAdventure.BEATEN_UP ) )
		{
			if ( KoLCharacter.hasSkill( "Tongue of the Walrus" ) )
				RequestThread.postRequest( UseSkillRequest.getInstance( "Tongue of the Walrus", 1 ) );
			else if ( KoLCharacter.hasSkill( "Tongue of the Otter" ) )
				RequestThread.postRequest( UseSkillRequest.getInstance( "Tongue of the Otter", 1 ) );
		}

		// Next, check against the restore needed to see if
		// any restoration needs to take place.

		Object [] empty = new Object[0];
		Method currentMethod, maximumMethod;

		currentMethod = KoLCharacter.class.getMethod( currentName, new Class[0] );
		maximumMethod = KoLCharacter.class.getMethod( maximumName, new Class[0] );

		float setting = StaticEntity.getFloatProperty( settingName );

		if ( setting < 0.0f && desired == 0 )
			return true;

		int current = ((Number)currentMethod.invoke( null, empty )).intValue();

		// If you've already reached the desired value, don't
		// bother restoring.

		if ( desired != 0 && current >= desired )
			return true;

		int maximum = ((Number)maximumMethod.invoke( null, empty )).intValue();
		int needed = Math.min( maximum, Math.max( desired, (int) (setting * ((float) maximum)) ) );

		if ( desired == 0 && current > needed )
			return true;

		// Next, check against the restore target to see how
		// far you need to go.

		setting = StaticEntity.getFloatProperty( settingName + "Target" );
		desired = Math.min( maximum, Math.max( desired, (int) (setting * ((float) maximum)) ) );

		if ( BuffBotHome.isBuffBotActive() || desired > maximum )
			desired = maximum;

		if ( current >= desired )
			return true;

		// If it gets this far, then you should attempt to recover
		// using the selected items.  This involves a few extra
		// reflection methods.

		String currentTechniqueName;

		// Determine all applicable items and skills for the restoration.
		// This is a little bit memory intensive, but it allows for a lot
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
				if ( ((HPRestoreItem)techniques[i]).getItem() == null )
					possibleSkills.add( techniques[i] );
				else
					possibleItems.add( techniques[i] );
			}

			if ( techniques[i] instanceof MPRestoreItem )
			{
				if ( ((MPRestoreItem)techniques[i]).getItem() == null )
					possibleSkills.add( techniques[i] );
				else
					possibleItems.add( techniques[i] );
			}
		}

		if ( !possibleSkills.isEmpty() )
			Collections.sort( possibleSkills );

		int last = -1;

		// Iterate through every restore item which is already available
		// in the player's inventory.

		for ( int i = 0; i < possibleItems.size() && current < needed; ++i )
		{
			do
			{
				last = current;
				currentTechniqueName = possibleItems.get(i).toString().toLowerCase();

				recoverOnce( possibleItems.get(i), currentTechniqueName, desired, false );
				current = ((Number)currentMethod.invoke( null, empty )).intValue();

				maximum = ((Number)maximumMethod.invoke( null, empty )).intValue();
				desired = Math.min( maximum, desired );
				needed = Math.min( maximum, needed );
			}
			while ( last != current && current < needed );
		}

		if ( refusesContinue() )
			return false;

		// Next, move onto things which are not items (skills), and
		// prefer them over purchasing items.

		for ( int i = 0; i < possibleSkills.size() && current < needed; ++i )
		{
			do
			{
				last = current;
				currentTechniqueName = possibleSkills.get(i).toString().toLowerCase();

				recoverOnce( possibleSkills.get(i), currentTechniqueName, desired, true );
				current = ((Number)currentMethod.invoke( null, empty )).intValue();

				maximum = ((Number)maximumMethod.invoke( null, empty )).intValue();
				desired = Math.min( maximum, desired );
				needed = Math.min( maximum, needed );
			}
			while ( last != current && current < needed );
		}

		if ( refusesContinue() )
			return false;

		// If things are still not restored, try looking for items you
		// don't have.

		if ( StaticEntity.getBooleanProperty( "autoBuyRestores" ) )
		{
			for ( int i = 0; i < possibleItems.size() && current < needed; ++i )
			{
				do
				{
					last = current;
					currentTechniqueName = possibleItems.get(i).toString().toLowerCase();

					recoverOnce( possibleItems.get(i), currentTechniqueName, desired, true );
					current = ((Number)currentMethod.invoke( null, empty )).intValue();

					maximum = ((Number)maximumMethod.invoke( null, empty )).intValue();
					desired = Math.min( maximum, desired );
					needed = Math.min( maximum, needed );
				}
				while ( last != current && current < needed );
			}
		}
		else if ( current < needed )
		{
			updateDisplay( ERROR_STATE, "You ran out of restores" );
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
	 * Utilit.  This method called inbetween battles.  Thi.  This method
	 * checks to see if the character's HP has dropped below
	 * the tolerance value, and recovers if it has (if
	 * the user has specified this in their settings).
	 */

	public final boolean recoverHP()
	{	return recoverHP( 0 );
	}

	public final boolean recoverHP( int recover )
	{
		try
		{
			return recover( recover, "hpAutoRecovery", "getCurrentHP", "getMaximumHP", HPRestoreItemList.CONFIGURES );
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
	 * Utilit.  This method which uses the given recovery technique (not specified
	 * in a script) in order to restore.
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

	public static int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = StaticEntity.getProperty( "mpAutoRecoveryItems" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[i].toString().toLowerCase() ) != -1 )
				restoreCount += MPRestoreItemList.CONFIGURES[i].getItem().getCount( inventory );

		return restoreCount;
	}

	/**
	 * Utilit.  This method called inbetween commands.  Thi.  This method
	 * checks to see if the character's MP has dropped below
	 * the tolerance value, and recovers if it has (if
	 * the user has specified this in their settings).
	 */

	public final boolean recoverMP()
	{	return recoverMP( 0 );
	}

	/**
	 * Utilit.  This method which restores the character's current
	 * mana points above the given value.
	 */

	public final boolean recoverMP( int mpNeeded )
	{
		try
		{
			return recover( mpNeeded, "mpAutoRecovery", "getCurrentMP", "getMaximumMP", MPRestoreItemList.CONFIGURES );
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
	 * Utilit.  This method used to process the results of any adventure
	 * in the Kingdom of Loathing.  Thi.  This method searches for items,
	 * stat gains, and losses within the provided string.
	 *
	 * @param	results	The string containing the results of the adventure
	 * @return	<code>true</code> if any results existed
	 */

	public final boolean processResults( String results )
	{	return processResults( results, null );
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

				if ( StaticEntity.getBooleanProperty( "logGainMessages" ) )
					RequestLogger.updateSessionLog( message );

				parseResult( message );
			}

			damageMatcher = CARBS_PATTERN.matcher( plainTextResult );

			if ( damageMatcher.find() )
			{
				String message = "You lose " + damageMatcher.group(1) + " hit points";

				RequestLogger.printLine( message );

				if ( StaticEntity.getBooleanProperty( "logGainMessages" ) )
					RequestLogger.updateSessionLog( message );

				parseResult( message );
			}
		}

		if ( data == null )
		{
			damageMatcher = FUMBLE_PATTERN.matcher( plainTextResult );

			while ( damageMatcher.find() )
			{
				String message = "You lose " + damageMatcher.group(1) + " hit points";

				RequestLogger.printLine( message );

				if ( StaticEntity.getBooleanProperty( "logGainMessages" ) )
					RequestLogger.updateSessionLog( message );

				parseResult( message );
			}
		}

		boolean requiresRefresh = false;

		while ( parsedResults.hasMoreTokens() )
		{
			lastToken = parsedResults.nextToken();

			// Skip effect acquisition - it's followed by a boldface
			// which makes the parser think it's found an item.

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
							if ( StaticEntity.getBooleanProperty( "logAcquiredItems" ) )
								RequestLogger.updateSessionLog( acquisition + " " + item );
						}

						lastResult = parseItem( item );
						if ( data == null )
							processResult( lastResult );
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

						if ( StaticEntity.getBooleanProperty( "logAcquiredItems" ) )
							RequestLogger.updateSessionLog( acquisition + " " + item );

						lastResult = parseItem( itemName + " (" + countString + ")" );
						if ( data == null )
							processResult( lastResult );
						else
							AdventureResult.addResultToList( data, lastResult );
					}
				}
				else if ( data == null )
				{
					String effectName = parsedResults.nextToken();
					lastToken = parsedResults.nextToken();

					RequestLogger.printLine( acquisition + " " + effectName + " " + lastToken );

					if ( StaticEntity.getBooleanProperty( "logStatusEffects" ) )
						RequestLogger.updateSessionLog( acquisition + " " + effectName + " " + lastToken );

					if ( lastToken.indexOf( "duration" ) == -1 )
					{
						parseEffect( effectName );
					}
					else
					{
						String duration = lastToken.substring( 11, lastToken.length() - 11 ).trim();
						requiresRefresh |= parseEffect( effectName + " (" + duration + ")" );
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

				if ( !lastToken.startsWith( "You gain a" ) && !lastToken.startsWith( "You gain some" ) )
				{
					lastResult = parseResult( lastToken );
					if ( data == null )
					{
						processResult( lastResult );
						if ( lastResult.getName().equals( AdventureResult.SUBSTATS ) )
						{
							if ( StaticEntity.getBooleanProperty( "logStatGains" ) )
								RequestLogger.updateSessionLog( lastToken );
						}
						else if ( StaticEntity.getBooleanProperty( "logGainMessages" ) )
							RequestLogger.updateSessionLog( lastToken );

					}
					else if ( lastResult.getName().equals( AdventureResult.MEAT ) )
					{
						AdventureResult.addResultToList( data, lastResult );
						if ( StaticEntity.getBooleanProperty( "logGainMessages" ) )
							RequestLogger.updateSessionLog( lastToken );
					}
				}
			}
		}

		return requiresRefresh;
	}

	public void makeRequest( Runnable request )
	{	makeRequest( request, 1 );
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
				KoLAdventure adventure = (KoLAdventure) request;

				if ( adventure.getRequest() instanceof ClanGymRequest )
				{
					RequestThread.postRequest( ((ClanGymRequest)adventure.getRequest()).setTurnCount( iterations ) );
					return;
				}

				if ( adventure.getRequest() instanceof SewerRequest && !AdventureDatabase.retrieveItem( SewerRequest.GUM.getInstance( iterations ) ) )
					return;

				if ( !(adventure.getRequest() instanceof CampgroundRequest) && KoLCharacter.getCurrentHP() == 0 )
					recoverHP();

				if ( !KoLmafia.permitsContinue() )
					return;

				isAdventuring = true;
			}

			// Execute the request as initially intended by calling
			// a subroutine.  In doing so, make sure your HP/MP restore
			// settings are scaled back down to current levels, if they've
			// been manipulated internally by

			RequestThread.openRequestSequence();

			boolean restoreOutfit = false;
			RequestLogger.printLine();

			if ( request instanceof KoLAdventure )
			{
				restoreOutfit = true;
				SpecialOutfit.createImplicitCheckpoint();
			}

			executeRequest( request, iterations, wasAdventuring );

			if ( restoreOutfit )
				SpecialOutfit.restoreImplicitCheckpoint();

			RequestThread.closeRequestSequence();

			if ( request instanceof KoLAdventure && !wasAdventuring )
			{
				isAdventuring = false;
				runBetweenBattleChecks( false );
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
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

		int initialConditions = conditions.size();
		int remainingConditions = initialConditions;
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

		forceContinue();

		while ( permitsContinue() && ++currentIteration <= iterations )
		{
			// Account for the possibility that you could have run
			// out of adventures mid-request.

			if ( KoLCharacter.getAdventuresLeft() == 0 && request instanceof KoLAdventure )
			{
				iterations = currentIteration;
				break;
			}

			// See if you can create anything to satisfy your item
			// conditions, but only do so if it's an adventure.

			if ( request instanceof KoLAdventure )
			{
				boolean shouldCreate = false;

				for ( int i = 0; i < creatables.length && !shouldCreate; ++i )
					shouldCreate |= creatables[i] != null && creatables[i].getQuantityPossible() >= items[i].getCount();

				// In theory, you could do a real validation by doing a full
				// dependency search.  While that's technically better, it's
				// also not very useful.

				if ( !useDisjunction )
				{
					for ( int i = 0; i < creatables.length && shouldCreate; ++i )
						shouldCreate &= creatables[i] == null || creatables[i].getQuantityPossible() >= items[i].getCount();
				}

				// Create any items which are creatable.  If you're only doing
				// a disjunction of the conditions, stop after making one.

				for ( int i = creatables.length - 1; i >= 0; --i )
				{
					if ( creatables[i] != null && creatables[i].getQuantityPossible() >= items[i].getCount() )
					{
						creatables[i].setQuantityNeeded( items[i].getCount() );
						RequestThread.postRequest( creatables[i] );

						creatables[i] = null;
						shouldCreate = !useDisjunction;
					}
				}
			}

			// If the conditions existed and have been satisfied,
			// then you should stop.

			if ( conditions.size() < remainingConditions )
			{
				if ( conditions.size() == 0 || useDisjunction )
				{
					conditions.clear();
					updateDisplay( PENDING_STATE, "Conditions satisfied after " + (currentIteration - 1) +
						((currentIteration == 2) ? " request." : " requests.") );

					continue;
				}
			}

			remainingConditions = conditions.size();

			// Otherwise, disable the display and update the user
			// and the current request number.  Different requests
			// have different displays.  They are handled here.

			if ( request instanceof KoLAdventure && iterations > 1 )
				currentIterationString = "Request " + currentIteration + " of " + iterations + " (" + request.toString() + ") in progress...";
			else if ( request instanceof KoLAdventure )
				currentIterationString = "Visit to " + request.toString() + " in progress...";

			if ( refusesContinue() )
			{
				if ( request instanceof KoLAdventure && !wasAdventuring )
					AdventureFrame.updateRequestMeter( 1, 1 );

				return;
			}

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
				--currentIteration;

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
	}

	/**
	 * Makes a request which attempts to zap the chosen item
	 */

	public void makeZapRequest()
	{
		if ( KoLCharacter.getZapper() == null )
			return;

		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want to zap this item...", "Zzzzzzzzzap!", JOptionPane.INFORMATION_MESSAGE, null,
			inventory.toArray(), inventory.get(0) );

		if ( selectedValue == null )
			return;

		RequestThread.postRequest( new ZapRequest( (AdventureResult) selectedValue ) );
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  Thi.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	public void makeHermitRequest()
	{
		if ( !hermitItems.contains( SewerRequest.CLOVER ) )
			RequestThread.postRequest( new HermitRequest() );

		if ( !permitsContinue() )
			return;

		Object [] hermitItemArray = hermitItems.toArray();
		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the hermit...", "Mugging Hermit for...", JOptionPane.INFORMATION_MESSAGE, null,
			hermitItemArray, null );

		if ( selectedValue == null )
			return;

		int selected = ((AdventureResult)selectedValue).getItemId();

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
	 * Makes a request to the trapper, looking for the given number of
	 * items.  Thi.  This method should prompt the user to determine which
	 * item to retrieve from the trapper.
	 */

	public void makeTrapperRequest()
	{
		int furs = TrapperRequest.YETI_FUR.getCount( inventory );

		if ( furs == 0 )
		{
			updateDisplay( ERROR_STATE, "You don't have any yeti furs to trade." );
			return;
		}

		Object selectedValue = JOptionPane.showInputDialog(
			null, "I want this from the trapper...", "1337ing Trapper for...", JOptionPane.INFORMATION_MESSAGE, null,
			trapperItemNames, trapperItemNames[0] );

		if ( selectedValue == null )
			return;

		int selected = -1;
		for ( int i = 0; i < trapperItemNames.length; ++i )
			if ( selectedValue.equals( trapperItemNames[i] ) )
			{
				selected = trapperItemNumbers[i];
				break;
			}

		// Should not be possible...
		if ( selected == -1 )
			return;

		int tradeCount = KoLFrame.getQuantity( "How many " + selectedValue + " to get?", furs );
		if ( tradeCount == 0 )
			return;

		RequestThread.postRequest( new TrapperRequest( selected, tradeCount ) );
	}

	/**
	 * Makes a request to the hunter, looking to sell a given type of
	 * item.  Thi.  This method should prompt the user to determine which
	 * item to sell to the hunter.
	 */

	public void makeHunterRequest()
	{
		if ( hunterItems.isEmpty() )
			RequestThread.postRequest( new BountyHunterRequest() );

		Object [] hunterItemArray = hunterItems.toArray();

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "I want to sell this to the hunter...", "The Quilted Thicker Picker Upper!", JOptionPane.INFORMATION_MESSAGE, null,
			hunterItemArray, hunterItemArray[0] );

		if ( selectedValue == null )
			return;

		AdventureResult selected = new AdventureResult( selectedValue, 0, false );
		int available = selected.getCount( inventory );

		if ( available == 0 )
		{
			updateDisplay( ERROR_STATE, "You don't have any " + selectedValue + "." );
			return;
		}

		int tradeCount = KoLFrame.getQuantity( "How many " + selectedValue + " to sell?", available );
		if ( tradeCount == 0 )
			return;

		// If we're not selling all of the item, closet the rest
		if ( tradeCount < available )
		{
			Object [] items = new Object[1];
			items[0] = selected.getInstance( available - tradeCount );

			if ( permitsContinue() )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.INVENTORY_TO_CLOSET, items ) );

			if ( permitsContinue() )
				RequestThread.postRequest( new BountyHunterRequest( selected.getItemId() ) );

			if ( permitsContinue() )
				RequestThread.postRequest( new ItemStorageRequest( ItemStorageRequest.CLOSET_TO_INVENTORY, items ) );
		}
		else
			RequestThread.postRequest( new BountyHunterRequest( TradeableItemDatabase.getItemId( selectedValue ) ) );
	}

	/**
	 * Makes a request to Doc Galaktik, looking for a cure.
	 */

	public void makeGalaktikRequest()
	{
		Object [] cureArray = GalaktikRequest.retrieveCures().toArray();

		if ( cureArray.length == 0 )
		{
			updateDisplay( ERROR_STATE, "You don't need any cures." );
			return;
		}

		String selectedValue = (String) JOptionPane.showInputDialog(
			null, "Cure me, Doc!", "Doc Galaktik", JOptionPane.INFORMATION_MESSAGE, null,
			cureArray, cureArray[0] );

		if ( selectedValue == null )
			return;

		int type = 0;
		if ( selectedValue.indexOf( "HP" ) != -1 )
			type = GalaktikRequest.HP;
		else if ( selectedValue.indexOf( "MP" ) != -1 )
			type = GalaktikRequest.MP;
		else
			return;

		RequestThread.postRequest( new GalaktikRequest( type ) );
	}

	/**
	 * Makes a request to the hunter, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hunter.
	 */

	public void makeUntinkerRequest()
	{
		List untinkerItems = new ArrayList();

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

		Object [] untinkerItemArray = untinkerItems.toArray();
		Arrays.sort( untinkerItemArray );

		AdventureResult selectedValue = (AdventureResult) JOptionPane.showInputDialog(
			null, "I want to untinker an item...", "You can unscrew meat paste?", JOptionPane.INFORMATION_MESSAGE, null,
			untinkerItemArray, untinkerItemArray[0] );

		if ( selectedValue == null )
			return;

		RequestThread.postRequest( new UntinkerRequest( selectedValue.getItemId() ) );
	}

	/**
	 * Set the Canadian Mind Control device to selected setting.
	 */

	public void makeMindControlRequest()
	{
		String [] levelArray = new String[12];
		for ( int i = 0; i < 12; ++i )
			levelArray[i] = "Level " + i;

		String selectedLevel = (String) JOptionPane.showInputDialog(
			null, "Set the device to what level?", "Change mind control device from level " + KoLCharacter.getMindControlLevel(),
				JOptionPane.INFORMATION_MESSAGE, null, levelArray, levelArray[ KoLCharacter.getMindControlLevel() ] );

		if ( selectedLevel == null )
			return;

		RequestThread.postRequest( new MindControlRequest( StaticEntity.parseInt( selectedLevel.split( " " )[1] ) ) );
	}

	public void makeCampgroundRestRequest()
	{
		String turnCount = (String) JOptionPane.showInputDialog( null, "Rest for how many turns?", "1" );
		if ( turnCount == null )
			return;

		makeRequest( new CampgroundRequest( "rest" ), StaticEntity.parseInt( turnCount ) );
	}

	public void makeCampgroundRelaxRequest()
	{
		String turnCount = (String) JOptionPane.showInputDialog( null, "Relax for how many turns?", "1" );
		if ( turnCount == null )
			return;

		makeRequest( new CampgroundRequest( "relax" ), StaticEntity.parseInt( turnCount ) );
	}

	public void makeClanSofaRequest()
	{
		String turnCount = (String) JOptionPane.showInputDialog( null, "Sleep for how many turns?", "1" );
		if ( turnCount == null )
			return;

		makeRequest( new ClanGymRequest( ClanGymRequest.SOFA ), StaticEntity.parseInt( turnCount ) );
	}

	public static void validateFaucetQuest()
	{
		int lastAscension = StaticEntity.getIntegerProperty( "lastTavernAscension" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			StaticEntity.setProperty( "lastTavernSquare", "0" );
			StaticEntity.setProperty( "lastTavernAscension", String.valueOf( KoLCharacter.getAscensions() ) );
			StaticEntity.setProperty( "tavernLayout", "0000000000000000000000000" );
		}
	}

	public static void addTavernLocation( KoLRequest request )
	{
		validateFaucetQuest();
		if ( KoLCharacter.getAdventuresLeft() == 0 || KoLCharacter.getCurrentHP() == 0 )
			return;

		StringBuffer layout = new StringBuffer( StaticEntity.getProperty( "tavernLayout" ) );

		if ( request.getURLString().indexOf( "fight" ) != -1 )
		{
			int square = StaticEntity.getIntegerProperty( "lastTavernSquare" );
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
			StaticEntity.setProperty( "lastTavernSquare", String.valueOf( square ) );

			char replacement = '1';
			if ( request.responseText != null && request.responseText.indexOf( "faucetoff" ) != -1 )
				replacement = '3';
			else if ( request.responseText != null && request.responseText.indexOf( "You acquire" ) != -1 )
				replacement = '2';

			layout.setCharAt( square - 1, replacement );
		}

		StaticEntity.setProperty( "tavernLayout", layout.toString() );
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

		String visited = StaticEntity.getProperty( "tavernLayout" );
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

		DEFAULT_SHELL.executeLine( "council" );

		updateDisplay( "Searching for faucet..." );
		adventure.run();

		// Random guess instead of straightforward search
		// for the location of the faucet (lowers the chance
		// of bad results if the faucet is near the end).

		while ( KoLmafia.permitsContinue() && !foundFaucet && KoLCharacter.getCurrentHP() > 0 && KoLCharacter.getAdventuresLeft() > 0 )
		{
			searchIndex = (Integer) searchList.remove( RNG.nextInt( searchList.size() ) );

			adventure.getRequest().addFormField( "where", searchIndex.toString() );
			adventure.run();

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
		updateDisplay( "Determining items needed..." );

		KoLRequest request = new KoLRequest( "town_right.php?place=gourd", true );
		RequestThread.postRequest( request );

		// For every class, it's the same -- the message reads, "Bring back"
		// and then the number of the item needed.  Compare how many you need
		// with how many you have.

		Matcher neededMatcher = GOURD_PATTERN.matcher( request.responseText );
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

		while ( neededCount <= 25 && neededCount <= item.getCount( inventory ) )
		{
			updateDisplay( "Giving up " + neededCount + " " + item.getName() + "s..." );
			RequestThread.postRequest( request.constructURLString( "town_right.php?place=gourd&action=gourd" ) );
			processResult( item.getInstance( 0 - neededCount++ ) );
		}

		int totalProvided = 0;
		for ( int i = 5; i < neededCount; ++i )
			totalProvided += i;

		updateDisplay( "Gourd trading complete (" + totalProvided + " " + item.getName() + "s given so far)." );
	}

	public void unlockGuildStore()
	{	unlockGuildStore( false );
	}

	public void unlockGuildStore( boolean stopAtPaco )
	{
		// The wiki claims that your prime stats are somehow connected,
		// but the exact procedure is uncertain.  Therefore, just allow
		// the person to attempt to unlock their store, regardless of
		// their current stats.

		updateDisplay( "Entering guild challenge area..." );
		KoLRequest request = new KoLRequest( "guild.php?place=challenge", true );

		RequestThread.postRequest( request );

		boolean success = stopAtPaco ? request.responseText.indexOf( "paco" ) != -1 :
			request.responseText.indexOf( "store.php" ) != -1;

		updateDisplay( "Completing guild tasks..." );

		for ( int i = 0; i < 6 && !success && KoLCharacter.getAdventuresLeft() > 0 && permitsContinue(); ++i )
		{
			RequestThread.postRequest( request.constructURLString( "guild.php?action=chal" ) );

			if ( request.responseText != null )
			{
				success |= stopAtPaco ? request.responseText.indexOf( "paco" ) != -1 :
					request.responseText.indexOf( "You've already beaten" ) != -1;
			}
		}

		if ( success && KoLCharacter.getLevel() > 3 )
			RequestThread.postRequest( request.constructURLString( "guild.php?place=paco" ) );

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

			if ( sold[i].getPrice() == 999999999 )
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

	public abstract void showHTML( String text, String title );

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
	 * Utilit.  This method used to decode a saved password.
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

			StaticEntity.setGlobalProperty( username, "saveState", (new BigInteger( encodedString.toString(), 36 )).toString( 10 ) );
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

	public static void removeSaveState( String loginname )
	{
		if ( loginname == null )
			return;

		saveStateNames.remove( loginname );
		StaticEntity.removeGlobalProperty( loginname, "saveState" );
	}

	/**
	 * Utilit.  This method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public static final String getSaveState( String loginname )
	{
		try
		{
			String password = StaticEntity.getGlobalProperty( loginname, "saveState" );
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

	public static boolean checkRequirements( List requirements )
	{	return checkRequirements( requirements, true );
	}

	public static boolean checkRequirements( List requirements, boolean retrieveItem )
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

	public static void printList( List printing )
	{	printList( printing, RequestLogger.INSTANCE );
	}

	public static void printList( List printing, PrintStream ostream )
	{
		if ( printing == null || ostream == null )
			return;

		StringBuffer buffer = new StringBuffer();

		if ( printing != availableSkills )
		{
			for ( int i = 0; i < printing.size(); ++i )
			{
				if ( printing.get(i) == null )
					continue;

				buffer.append( printing.get(i).toString() );
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
	{	makePurchases( results, results.toArray(), count );
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
			{
				updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
				RequestThread.closeRequestSequence();
				return;
			}

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
	 * Utilit.  This method used to register a given adventure in
	 * the running adventure summary.
	 */

	public void registerAdventure( KoLAdventure adventureLocation )
	{
		String adventureName = adventureLocation.getAdventureName();
		if ( adventureName == null )
			return;

		recognizeEncounter( adventureName );
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

	public void recognizeEncounter( String encounterName )
	{
		if ( conditions.isEmpty() && stopEncounters.contains( encounterName ) )
		{
			RequestLogger.printLine();
			KoLmafia.updateDisplay( PENDING_STATE, encounterName );
			RequestLogger.printLine();

			RequestThread.enableDisplayIfSequenceComplete();
		}
	}

	/**
	 * Utilit.  This method used to register a given encounter in
	 * the running adventure summary.
	 */

	public void registerEncounter( String encounterName, String encounterType )
	{
		encounterName = encounterName.trim();
		recognizeEncounter( encounterName );

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
			processResult( CATNIP.getNegation() );
		if ( encounterName.equalsIgnoreCase( "Summer Holiday" ) && KoLCharacter.hasItem( GLIDER ) )
			processResult( GLIDER.getNegation() );

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
			encounterCount = 1;
		}

		public String toString()
		{	return "<html>" + stringform + " (" + encounterCount + ")</html>";
		}

		public int compareTo( Object o )
		{
			if ( !(o instanceof RegisteredEncounter) || o == null )
				return -1;

			if ( type == null || ((RegisteredEncounter)o).type == null || type.equals( ((RegisteredEncounter)o).type ) )
				return name.compareToIgnoreCase( ((RegisteredEncounter)o).name );

			return type.equals( "Combat" ) ? 1 : -1;
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
			BufferedReader reader = KoLDatabase.getReader(
				"http://svn.sourceforge.net/viewvc/*checkout*/kolmafia/src/data/" + OVERRIDE_DATA[i] );

			File output = new File( DATA_LOCATION, OVERRIDE_DATA[i] );

			try
			{
				String line;
				LogStream writer = LogStream.openStream( output, true );

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

				output.delete();
				RequestThread.closeRequestSequence();
				return;
			}
		}

		updateDisplay( "Please restart KoLmafia to complete the update." );
		RequestThread.enableDisplayIfSequenceComplete();
	}

	public static boolean isRunningBetweenBattleChecks()
	{	return recoveryActive || MoodSettings.isExecuting();
	}

	public static boolean runThresholdChecks()
	{
		float autoStopValue = StaticEntity.getFloatProperty( "hpThreshold" );
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
	{	runBetweenBattleChecks( isFullCheck, isFullCheck, isFullCheck, isFullCheck );
	}

	public void runBetweenBattleChecks( boolean isScriptCheck, boolean isMoodCheck, boolean isHealthCheck, boolean isManaCheck )
	{
		// Do not run between battle checks if you are in the middle
		// of your checks or if you have aborted.

		if ( recoveryActive || refusesContinue() || !runThresholdChecks() )
			return;

		// First, run the between battle script defined by the
		// user, which may make it so that none of the built
		// in behavior needs to run.

		RequestThread.openRequestSequence();

		if ( isScriptCheck )
		{
			String scriptPath = StaticEntity.getProperty( "betweenBattleScript" );

			if ( !scriptPath.equals( "" ) )
			{
				recoveryActive = true;
				DEFAULT_SHELL.executeLine( scriptPath );
				recoveryActive = false;
			}
		}

		SpecialOutfit.createImplicitCheckpoint();
		recoveryActive = true;

		// Now, run the built-in behavior to take care of
		// any loose ends.

		if ( isMoodCheck )
			MoodSettings.execute();

		if ( isHealthCheck || KoLCharacter.getCurrentHP() == 0 )
			recoverHP();

		if ( isManaCheck )
			recoverMP();

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
		startRelayServer();

		// Even after the wait, sometimes, the
		// worker threads have not been filled.

		String baseURL = "http://127.0.0.1:" + LocalRelayServer.getPort() + "/";

		if ( KoLRequest.sessionId == null )
			StaticEntity.openSystemBrowser( baseURL + "login.php" );
		else if ( KoLRequest.isCompactMode )
			StaticEntity.openSystemBrowser( baseURL + "main_c.html" );
		else
			StaticEntity.openSystemBrowser( baseURL + "main.html" );
	}

	public void launchRadioKoL()
	{
		if ( StaticEntity.getBooleanProperty( "useLowBandwidthRadio" ) )
			StaticEntity.openSystemBrowser( "http://209.9.238.5:8792/listen.pls" );
		else
			StaticEntity.openSystemBrowser( "http://209.9.238.5:8794/listen.pls" );
	}

	public void launchSimulator()
	{
		LocalRelayServer.startThread();

		// Wait for 5 seconds before giving up
		// on the relay server.

		for ( int i = 0; i < 50 && !LocalRelayServer.isRunning(); ++i )
			KoLRequest.delay( 500 );

		if ( !LocalRelayServer.isRunning() )
			return;

		// Even after the wait, sometimes, the
		// worker threads have not been filled.

		StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + "/KoLmafia/simulator/index.html" );
	}

	public static boolean isAdventuring()
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

		if ( JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog( null,
			"Are you sure you'd like to host an end-of-run sale?", "MASSIVE SALE", JOptionPane.YES_NO_OPTION ) )
				return;

		// Find all tradeable items.  Tradeable items
		// are marked by an autosell value of nonzero.

		RequestThread.openRequestSequence();
		makeJunkRemovalRequest();

		// Only place items in the mall which are not
		// sold in NPC stores and can be autosold.

		AdventureResult [] items = new AdventureResult[ inventory.size() ];
		inventory.toArray( items );

		ArrayList autosell = new ArrayList();
		ArrayList automall = new ArrayList();

		for ( int i = 0; i < items.length; ++i )
		{
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
			priceItemsAtLowestPrice();

		updateDisplay( "Undercutting sale complete." );
		RequestThread.closeRequestSequence();
	}

	public void makeJunkRemovalRequest()
	{
		int itemCount;
		AdventureResult currentItem;

		Object [] items = junkItemList.toArray();

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
				case 184: // briefcase
				case 533: // Gnollish toolbox
				case 553: // 31337 scroll
				case 604: // Penultimate fantasy chest
				case 831: // small box
				case 832: // large box
				case 1917: // old leather wallet
				case 1918: // old coin purse
					RequestThread.postRequest( new ConsumeItemRequest( currentItem.getInstance( itemCount ) ) );
					break;

				case 621: // Warm Subject gift certificate
					RequestThread.postRequest( new ConsumeItemRequest( currentItem.getInstance(1) ) );
					RequestThread.postRequest( new ConsumeItemRequest( currentItem.getInstance( itemCount - 1 ) ) );
					break;
				}
			}
		}
		while ( madeUntinkerRequest );

		// Now you've got all the items used up, go ahead and prepare to
		// pulverize strong equipment.

		int itemPower;

		if ( KoLCharacter.hasSkill( "Pulverize" ) && KoLCharacter.hasItem( ConcoctionsDatabase.HAMMER ) )
		{
			boolean hasMalusAccess = KoLCharacter.isMuscleClass();

			for ( int i = 0; i < items.length; ++i )
			{
				currentItem = (AdventureResult) items[i];

				if ( mementoList.contains( currentItem ) )
					continue;

				itemCount = currentItem.getCount( inventory );
				itemPower = EquipmentDatabase.getPower( currentItem.getItemId() );

				if ( itemCount > 0 && !NPCStoreDatabase.contains( currentItem.getName() ) )
				{
					switch ( TradeableItemDatabase.getConsumptionType( currentItem.getItemId() ) )
					{
					case EQUIP_HAT:
					case EQUIP_PANTS:
					case EQUIP_SHIRT:
					case EQUIP_WEAPON:
					case EQUIP_OFFHAND:

						if ( itemPower >= 100 || (hasMalusAccess && itemPower > 10) )
							RequestThread.postRequest( new PulverizeRequest( currentItem.getInstance( itemCount ) ) );

						break;

					case EQUIP_FAMILIAR:
					case EQUIP_ACCESSORY:
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

			if ( currentItem.getItemId() == MEAT_PASTE )
				continue;

			itemCount = currentItem.getCount( inventory );
			itemPower = EquipmentDatabase.getPower( currentItem.getItemId() );

			if ( itemCount > 0 )
				sellList.add( currentItem.getInstance( itemCount ) );
		}

		if ( !sellList.isEmpty() )
			RequestThread.postRequest( new AutoSellRequest( sellList.toArray(), AutoSellRequest.AUTOSELL ) );

		RequestThread.closeRequestSequence();
	}

	public void handleAscension()
	{
		StaticEntity.setProperty( "lastBreakfast", "-1" );
		StaticEntity.setProperty( "lastCouncilVisit", "0" );

		resetCounters();
		KoLCharacter.reset();

		LockableListModel familiars = KoLCharacter.getFamiliarList();
		for ( int i = 0; i < familiars.size(); ++i )
			((FamiliarData)familiars.get(i)).setItem( EquipmentRequest.UNEQUIP );

		conditions.clear();
		refreshSession();
		resetSession();
		enableDisplay();

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
	}

	private static class ShutdownThread extends Thread
	{
		public void run()
		{
			CustomItemDatabase.saveItemData();

			SystemTrayFrame.removeTrayIcon();
			LocalRelayServer.stop();

			RequestLogger.closeSessionLog();
			RequestLogger.closeDebugLog();
			RequestLogger.closeMirror();
		}
	}
}
