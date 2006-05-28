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

import java.net.URL;
import java.net.URLEncoder;
import java.net.URLDecoder;

import java.awt.Component;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Properties;

import java.util.Arrays;
import java.math.BigInteger;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public abstract class KoLmafia implements KoLConstants
{
	static
	{
		System.setProperty( "com.apple.mrj.application.apple.menu.about.name", "KoLmafia" );
		System.setProperty( "com.apple.mrj.application.live-resize", "true" );
		System.setProperty( "com.apple.mrj.application.growbox.intrudes", "false" );

		JEditorPane.registerEditorKitForContentType( "text/html", "net.sourceforge.kolmafia.RequestEditorKit" );
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );
	}

	protected static PrintStream sessionStream = NullStream.INSTANCE;
	protected static PrintStream macroStream = NullStream.INSTANCE;
	protected static PrintStream debugStream = NullStream.INSTANCE;
	protected static LimitedSizeChatBuffer commandBuffer = new LimitedSizeChatBuffer( "KoLmafia: Graphical CLI", false );

	private static final String [] OVERRIDE_DATA =
	{
		"adventures.dat", "buffbots.dat", "buffs.dat", "classskills.dat", "combat.dat", "concoctions.dat",
		"equipment.dat", "familiars.dat", "itemdescs.dat", "monsters.dat", "npcstores.dat", "outfits.dat",
		"packages.dat", "statuseffects.dat", "tradeitems.dat", "zonelist.dat"
	};

	public static final String [][] BREAKFAST_SKILLS =
	{
		{ "Summon Snowcone", "1" },
		{ "Summon Hilarious Objects", "1" },
		{ "Advanced Saucecrafting", "3" },
		{ "Pastamastery", "3" },
		{ "Advanced Cocktailcrafting", "3" }
	};

	protected static final String [] trapperItemNames = { "yak skin", "penguin skin", "hippopotamus skin" };
	protected static final int [] trapperItemNumbers = { 394, 393, 395 };

	protected boolean isMakingRequest;
	protected KoLRequest currentRequest;
	protected LoginRequest cachedLogin;

	protected String password, sessionID, passwordHash;

	protected KoLSettings settings;
	protected Properties LOCAL_SETTINGS = new Properties();

	private String currentIterationString = "";
	protected int currentState = CONTINUE_STATE;

	protected int [] initialStats = new int[3];
	protected int [] fullStatGain = new int[3];

	protected SortedListModel saveStateNames = new SortedListModel();
	protected List recentEffects = new ArrayList();

	private static TreeMap seenPlayerIDs = new TreeMap();
	private static TreeMap seenPlayerNames = new TreeMap();
	protected SortedListModel contactList = new SortedListModel();

	protected SortedListModel tally = new SortedListModel();
	protected SortedListModel missingItems = new SortedListModel();
	protected SortedListModel hermitItems = new SortedListModel();
	protected SortedListModel hunterItems = new SortedListModel();

	protected LockableListModel restaurantItems = new LockableListModel();
	protected LockableListModel microbreweryItems = new LockableListModel();
	protected LockableListModel galaktikCures = new LockableListModel();

	protected boolean useDisjunction;
	protected SortedListModel conditions = new SortedListModel();
	protected LockableListModel adventureList = new LockableListModel();
	protected SortedListModel encounterList = new SortedListModel();

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafiaGUI</code>.
	 */

	public static void main( String [] args )
	{
		boolean useGUI = true;
		for ( int i = 0; i < args.length; ++i )
		{
			if ( args[i].equals( "--CLI" ) )
				useGUI = false;
			if ( args[i].equals( "--GUI" ) )
				useGUI = true;
		}

		if ( useGUI )
			KoLmafiaGUI.main( args );
		else
			KoLmafiaCLI.main( args );

		// All that completed, check to see if there is an auto-login
		// which should occur.

		String autoLogin = GLOBAL_SETTINGS.getProperty( "autoLogin" );
		if ( !autoLogin.equals( "" ) )
		{
			// Make sure that a password was stored for this
			// character (would fail otherwise):

			String password = StaticEntity.getClient().getSaveState( autoLogin );
			if ( password != null && !password.equals( "" ) )
				(new RequestThread( new LoginRequest( StaticEntity.getClient(), autoLogin, password ) )).start();
		}
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk.
	 */

	public KoLmafia()
	{
		this.useDisjunction = false;
		this.settings = GLOBAL_SETTINGS;

		String [] currentNames = GLOBAL_SETTINGS.getProperty( "saveState" ).split( "//" );
		for ( int i = 0; i < currentNames.length; ++i )
			saveStateNames.add( currentNames[i] );

		// This line is added to clear out data from previous
		// releases of KoLmafia - the extra disk access does
		// affect performance, but not significantly.

		storeSaveStates();

		// Also clear out any outdated data files -- this
		// includes everything except for the adventure table,
		// since changing that actually does something.

		String version = GLOBAL_SETTINGS.getProperty( "previousUpdateVersion" );

		if ( version == null || !version.equals( VERSION_NAME ) )
		{
			GLOBAL_SETTINGS.setProperty( "previousUpdateVersion", VERSION_NAME );
			for ( int i = 1; i < OVERRIDE_DATA.length; ++i )
			{
				File outdated = new File( "data/" + OVERRIDE_DATA[i] );
				if ( outdated.exists() )
					outdated.delete();
			}
		}
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( String message )
	{
		if ( !message.equals( "" ) )
			DEFAULT_SHELL.updateDisplay( CONTINUE_STATE, message );
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public void updateDisplay( int state, String message )
	{
		if ( message.equals( "" ) )
			return;

		if ( this.currentState != ABORT_STATE )
			this.currentState = state;

		debugStream.println( message );

		StringBuffer colorBuffer = new StringBuffer();
		if ( state == ERROR_STATE || state == ABORT_STATE )
			colorBuffer.append( "<font color=red>" );
		else
			colorBuffer.append( "<font color=black>" );

		colorBuffer.append( message.indexOf( LINE_BREAK ) != -1 ? ("<pre>" + message + "</pre>") : message );
		colorBuffer.append( "</font><br>" );
		colorBuffer.append( LINE_BREAK );

		LocalRelayServer.addStatusMessage( colorBuffer.toString() );
		commandBuffer.append( colorBuffer.toString() );
		updateDisplayPanels( state != CONTINUE_STATE ? state :
			message.endsWith( "..." ) ? CONTINUE_STATE : ENABLE_STATE, message );
	}

	public void updateDisplayPanels( int state, String message )
	{
		// Next, update all of the panels with the
		// desired update message.

		WeakReference [] references = new WeakReference[ existingPanels.size() ];
		existingPanels.toArray( references );

		for ( int i = 0; i < references.length; ++i )
		{
			if ( references[i].get() != null )
			{
				if ( references[i].get() instanceof KoLPanel )
					((KoLPanel) references[i].get()).setStatusMessage( state, message );

				((Component)references[i].get()).setEnabled( state != CONTINUE_STATE );
			}
		}

		// Finally, update all of the existing frames
		// with the appropriate state.

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			frames[i].updateDisplayState( state );

		if ( KoLDesktop.instanceExists() )
			KoLDesktop.getInstance().updateDisplayState( state );
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String username, String sessionID, boolean getBreakfast )
	{
		this.conditions.clear();

		// Initialize the variables to their initial
		// states to avoid null pointers getting thrown
		// all over the place

		if ( !permitsContinue() )
		{
			deinitialize();
			return;
		}

		this.sessionID = sessionID;
		this.settings = new KoLSettings( username );

		if ( GLOBAL_SETTINGS.getProperty( "keepSessionLogs" ).equals( "true" ) || settings.getProperty( "keepSessionLogs" ).equals( "true" ) )
		{
			settings.setProperty( "keepSessionLogs", "true" );
			KoLmafia.openSessionStream();
		}
		else
			KoLmafia.closeSessionStream();

		GLOBAL_SETTINGS.setProperty( "lastUsername", username );
		KoLCharacter.reset( username );

		this.refreshSession();

		if ( !permitsContinue() )
		{
			deinitialize();
			return;
		}

		// Always reset the auto-repair settings, because the
		// auto-repair situation is usually desired unless
		// the person is trying to explode boxes.

		if ( StaticEntity.getProperty( "autoRepairBoxes" ).equals( "false" ) )
			StaticEntity.setProperty( "autoRepairBoxes", String.valueOf( KoLCharacter.canInteract() ) );
		if ( StaticEntity.getProperty( "createWithoutBoxServants").equals( "false" ) )
			StaticEntity.setProperty( "createWithoutBoxServants", String.valueOf( KoLCharacter.isHardcore() ) );

		// If the password hash is non-null, then that means you
		// might be mid-transition.

		if ( getPasswordHash() != null && getPasswordHash().equals( "" ) )
			return;

		registerPlayer( username, String.valueOf( KoLCharacter.getUserID() ) );

		if ( getBreakfast )
		{
			String today = sdf.format( new Date() );
			String lastBreakfast = GLOBAL_SETTINGS.getProperty( "lastBreakfast." + username.toLowerCase() );

			if ( lastBreakfast == null || !lastBreakfast.equals( today ) )
			{
				String skillSetting = GLOBAL_SETTINGS.getProperty( "breakfast." + (KoLCharacter.isHardcore() ? "hardcore" : "softcore") );
				String scriptSetting = GLOBAL_SETTINGS.getProperty( "loginScript." + username.toLowerCase() );
				if ( scriptSetting != null && !scriptSetting.equals( "" ) )
					DEFAULT_SHELL.executeLine( scriptSetting );

				getBreakfast( true );
				if ( (skillSetting != null && !skillSetting.equals( "" )) || (scriptSetting != null && !scriptSetting.equals( "" )) )
					GLOBAL_SETTINGS.setProperty( "lastBreakfast." + username.toLowerCase(), today );
			}
		}
	}

	public void getBreakfast( boolean checkSettings )
	{
		if ( KoLCharacter.hasToaster() )
			for ( int i = 0; i < 3 && permitsContinue(); ++i )
				(new CampgroundRequest( this, "toast" )).run();

		if ( KoLCharacter.hasArches() )
			(new CampgroundRequest( this, "arches" )).run();

		String skillSetting = GLOBAL_SETTINGS.getProperty( "breakfast." + (KoLCharacter.isHardcore() ? "hardcore" : "softcore") );

		if ( skillSetting != null )
			for ( int i = 0; i < BREAKFAST_SKILLS.length; ++i )
				if ( (!checkSettings || skillSetting.indexOf( BREAKFAST_SKILLS[i][0] ) != -1) && KoLCharacter.hasSkill( BREAKFAST_SKILLS[i][0] ) )
					getBreakfast( BREAKFAST_SKILLS[i][0], Integer.parseInt( BREAKFAST_SKILLS[i][1] ) );
	}

	public void getBreakfast( String skillname, int standardCast )
	{	(new UseSkillRequest( this, skillname, "", standardCast )).run();
	}

	public final void refreshSession()
	{
		KoLCharacter.reset( KoLCharacter.getUsername() );

		KoLmafiaCLI.reset();
		KoLMailManager.reset();
		MushroomPlot.reset();
		StoreManager.reset();
		CakeArenaManager.reset();
		MuseumManager.reset();
		ClanManager.reset();

		this.hermitItems.clear();
		this.hermitItems.add( "banjo strings" );
		this.hermitItems.add( "catsup" );
		this.hermitItems.add( "digny planks" );
		this.hermitItems.add( "fortune cookie" );
		this.hermitItems.add( "golden twig" );
		this.hermitItems.add( "hot buttered roll" );
		this.hermitItems.add( "jaba\u00f1ero pepper" );
		this.hermitItems.add( "ketchup" );
		this.hermitItems.add( "sweet rims" );
		this.hermitItems.add( "volleyball" );
		this.hermitItems.add( "wooden figurine" );

		this.hunterItems.clear();
		this.restaurantItems.clear();
		this.microbreweryItems.clear();
		this.galaktikCures.clear();

		if ( !permitsContinue() )
			return;

		// Retrieve the list of outfits which are available to the
		// character.  Due to lots of bug reports, this is no longer
		// a skippable option.

		(new EquipmentRequest( this, EquipmentRequest.EQUIPMENT )).run();
		SpecialOutfit.deleteCheckpoint();

		if ( !permitsContinue() )
			return;

		// Retrieve the items which are available for consumption
		// and item creation.

		(new EquipmentRequest( this, EquipmentRequest.CLOSET )).run();

		if ( !permitsContinue() )
			return;

		// If the password hash is non-null, then that means you
		// might be mid-transition.

		if ( getPasswordHash() != null && getPasswordHash().equals( "" ) )
			return;

		// Retrieve the character sheet next -- because concoctions
		// are refreshed at the end, it's more important to do this
		// after so that you have updated dictionary data.

		(new CharsheetRequest( this )).run();

		if ( !permitsContinue() )
			return;

		// Update the player's account settings (including time-zone
		// and current autosell mode).

		(new AccountRequest( StaticEntity.getClient() )).run();

		if ( !permitsContinue() )
			return;

		// Get current moon phases

		(new MoonPhaseRequest( this )).run();

		if ( !permitsContinue() )
			return;

		// Retrieve campground data to see if the user is able to
		// cook, make drinks or make toast.

		updateDisplay( "Retrieving campground data..." );
		(new CampgroundRequest( this )).run();

		if ( !permitsContinue() )
			return;

		// Retrieve the list of familiars which are available to
		// the player, if they haven't opted to skip them.

		(new FamiliarRequest( this )).run();

		if ( !permitsContinue() )
			return;

		updateDisplay( "Retrieving contact list..." );
		(new ContactListRequest( this )).run();

		if ( !permitsContinue() )
			return;

		// Also update the contents of Hagnk's storage so that you
		// do not have to re-run it all the time.

		(new ItemStorageRequest( this )).run();

		if ( !permitsContinue() )
			return;

		DEFAULT_SHELL.updateDisplay( "Data refreshed." );

		resetSession();
		applyRecentEffects();

		ConcoctionsDatabase.getConcoctions().clear();
		KoLCharacter.recalculateAdjustments( false );
		KoLCharacter.refreshCalculatedLists();
	}

	/**
	 * Utility method used to notify the client that it should attempt
	 * to retrieve breakfast.
	 */

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		sessionID = null;
		passwordHash = null;

		closeDebugStream();
		closeMacroStream();
	}

	/**
	 * Used to reset the session tally to its original values.
	 */

	public void resetSession()
	{
		tally.clear();

		this.recentEffects.clear();
		this.missingItems.clear();

		this.encounterList.clear();
		this.adventureList.clear();

		initialStats[0] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() );
		initialStats[1] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() );
		initialStats[2] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() );

		fullStatGain[0] = 0;
		fullStatGain[1] = 0;
		fullStatGain[2] = 0;

		tally.add( new AdventureResult( AdventureResult.ADV ) );
		processResult( new AdventureResult( AdventureResult.MEAT ) );
		processResult( new AdventureResult( AdventureResult.SUBSTATS ) );
		processResult( new AdventureResult( AdventureResult.DIVIDER ) );
	}

	/**
	 * Utility method to parse an individual adventuring result.
	 * This method determines what the result actually was and
	 * adds it to the tally.
	 *
	 * @param	result	String to parse for the result
	 */

	public boolean parseResult( String result )
	{
		String trimResult = result.trim();

		// Because of the simplified parsing, there's a chance that
		// the "gain" acquired wasn't a subpoint (in other words, it
		// includes the word "a" or "some"), which causes a NFE or
		// possibly a ParseException to be thrown.  catch them and
		// do nothing (eventhough it's technically bad style).

		if ( trimResult.startsWith( "You gain a" ) || trimResult.startsWith( "You gain some" ) )
			return false;

		try
		{
			debugStream.println( "Parsing result: " + trimResult );
			return processResult( AdventureResult.parseResult( trimResult ) );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		return false;
	}

	public void parseItem( String result )
	{
		debugStream.println( "Parsing item: " + result );

		// We do the following in order to not get confused by:
		//
		// Frobozz Real-Estate Company Instant House (TM)
		// stone tablet (Sinister Strumming)
		// stone tablet (Squeezings of Woe)
		// stone tablet (Really Evil Rhythm)
		//
		// which otherwise cause an exception and a stack trace

		// Look for a verbatim match
		int itemID = TradeableItemDatabase.getItemID( result.trim() );
		if ( itemID != -1 )
		{
			processResult( new AdventureResult( itemID, 1 ) );
			return;
		 }

		// Remove parenthesized number and match again.
		StringTokenizer parsedItem = new StringTokenizer( result, "()" );
		String name = parsedItem.nextToken().trim();
		int count = 1;

		if ( parsedItem.hasMoreTokens() )
		{
			try
			{
				count = df.parse( parsedItem.nextToken() ).intValue();
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}
		}

		processResult( new AdventureResult( name, count, false ) );
	}

	public void parseEffect( String result )
	{
		debugStream.println( "Parsing effect: " + result );

		StringTokenizer parsedEffect = new StringTokenizer( result, "()" );
		String parsedEffectName = parsedEffect.nextToken().trim();
		String parsedDuration = parsedEffect.hasMoreTokens() ? parsedEffect.nextToken() : "1";

		try
		{
			processResult( new AdventureResult( parsedEffectName, df.parse( parsedDuration ).intValue(), true ) );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Utility method used to process a result.  By default, this
	 * method will also add an adventure result to the tally directly.
	 * This is used whenever the nature of the result is already known
	 * and no additional parsing is needed.
	 *
	 * @param	result	Result to add to the running tally of adventure results
	 */

	public boolean processResult( AdventureResult result )
	{	return processResult( result, true );
	}

	/**
	 * Utility method used to process a result, and the user wishes to
	 * specify whether or not the result should be added to the running
	 * tally.  This is used whenever the nature of the result is already
	 * known and no additional parsing is needed.
	 *
	 * @param	result	Result to add to the running tally of adventure results
	 * @param	shouldTally	Whether or not the result should be added to the running tally
	 */

	public boolean processResult( AdventureResult result, boolean shouldTally )
	{
		// This should not happen, but check just in case and
		// return if the result was null.

		if ( result == null )
			return false;

		debugStream.println( "Processing result: " + result );
		String resultName = result.getName();

		// This should not happen, but check just in case and
		// return if the result name was null.

		if ( resultName == null )
			return false;

		// Process the adventure result in this section; if
		// it's a status effect, then add it to the recent
		// effect list.  Otherwise, add it to the tally.

		if ( result.isStatusEffect() )
			AdventureResult.addResultToList( recentEffects, result );
		else if ( resultName.equals( AdventureResult.ADV ) && result.getCount() < 0 )
			AdventureResult.addResultToList( tally, result.getNegation() );

		else if ( result.isItem() || resultName.equals( AdventureResult.SUBSTATS ) || resultName.equals( AdventureResult.MEAT ) )
		{
			if ( shouldTally )
				AdventureResult.addResultToList( tally, result );
		}

		KoLCharacter.processResult( result );

		if ( !shouldTally )
			return false;

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		boolean shouldRefresh = false;

		if ( resultName.equals( AdventureResult.SUBSTATS ) && tally.size() >= 3 )
		{
			int currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() ) - initialStats[0];
			shouldRefresh |= fullStatGain[0] != currentTest;
			fullStatGain[0] = currentTest;

			currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() ) - initialStats[1];
			shouldRefresh |= fullStatGain[1] != currentTest;
			fullStatGain[1] = currentTest;

			currentTest = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() ) - initialStats[2];
			shouldRefresh |= fullStatGain[2] != currentTest;
			fullStatGain[2] = currentTest;

			if ( tally.size() > 3 )
				tally.set( 3, new AdventureResult( AdventureResult.FULLSTATS, fullStatGain ) );
			else
				tally.add( new AdventureResult( AdventureResult.FULLSTATS, fullStatGain ) );
		}

		// Process the adventure result through the conditions
		// list, removing it if the condition is satisfied.

		int conditionsIndex = conditions.indexOf( result );

		if ( !resultName.equals( AdventureResult.ADV ) && conditionsIndex != -1 )
		{
			if ( resultName.equals( AdventureResult.SUBSTATS ) )
			{
				// If the condition is a substat condition,
				// then zero out the appropriate count, if
				// applicable, and remove the substat condition
				// if the overall count dropped to zero.

				AdventureResult condition = (AdventureResult) conditions.get( conditionsIndex );

				int [] substats = new int[3];
				for ( int i = 0; i < 3; ++i )
					substats[i] = Math.max( 0, condition.getCount(i) - result.getCount(i) );

				condition = new AdventureResult( AdventureResult.SUBSTATS, substats );

				if ( condition.getCount() == 0 )
					conditions.remove( conditionsIndex );
				else
					conditions.set( conditionsIndex, condition );
			}
			else if ( result.getCount( conditions ) <= result.getCount() )
			{
				// If this results in the satisfaction of a
				// condition, then remove it.

				conditions.remove( conditionsIndex );
			}
			else
			{
				// Otherwise, this was a partial satisfaction
				// of a condition.  Decrement the count by the
				// negation of this result.

				AdventureResult.addResultToList( conditions, result.getNegation() );
			}
		}

		return shouldRefresh;
	}

	/**
	 * Adds the recent effects accumulated so far to the actual effects.
	 * This should be called after the previous effects were decremented,
	 * if adventuring took place.
	 */

	public void applyRecentEffects()
	{
		for ( int j = 0; j < recentEffects.size(); ++j )
			AdventureResult.addResultToList( KoLCharacter.getEffects(), (AdventureResult) recentEffects.get(j) );

		KoLCharacter.getEffects().sort();
		recentEffects.clear();
		KoLCharacter.recalculateAdjustments( false );
	}

	/**
	 * Returns the string form of the player ID associated
	 * with the given player name.
	 *
	 * @param	playerID	The ID of the player
	 * @return	The player's name if it has been seen, or null if it has not
	 *          yet appeared in the chat (not likely, but possible).
	 */

	public static String getPlayerName( String playerID )
	{	return (String) seenPlayerNames.get( playerID );
	}

	/**
	 * Returns the string form of the player ID associated
	 * with the given player name.
	 *
	 * @param	playerName	The name of the player
	 * @return	The player's ID if the player has been seen, or the player's name
	 *			with spaces replaced with underscores and other elements encoded
	 *			if the player's ID has not been seen.
	 */

	public static String getPlayerID( String playerName )
	{
		if ( playerName == null )
			return null;

		String playerID = (String) seenPlayerIDs.get( playerName.toLowerCase() );
		return playerID != null ? playerID : playerName;
	}

	/**
	 * Registers the given player name and player ID with
	 * KoLmafia's player name tracker.
	 *
	 * @param	playerName	The name of the player
	 * @param	playerID	The player ID associated with this player
	 */

	public static void registerPlayer( String playerName, String playerID )
	{
		if ( !seenPlayerIDs.containsKey( playerName.toLowerCase() ) )
		{
			seenPlayerIDs.put( playerName.toLowerCase(), playerID );
			seenPlayerNames.put( playerID, playerName );
		}
	}

	public void registerContact( String playerName, String playerID )
	{
		registerPlayer( playerName, playerID );
		if ( !contactList.contains( playerName ) )
			contactList.add( playerName );
	}

	/**
	 * Retrieves the session ID for this <code>KoLmafia</code> session.
	 * @return	The session ID of the current session
	 */

	public String getSessionID()
	{	return sessionID;
	}

	/**
	 * Stores the password hash for this <code>KoLmafia</code> session.
	 * @param	passwordHash	The password hash for this session
	 */

	public void setPasswordHash( String passwordHash )
	{	this.passwordHash = passwordHash;
	}

	/**
	 * Retrieves the password hash for this <code>KoLmafia</code> session.
	 * @return	The password hash of the current session
	 */

	public String getPasswordHash()
	{	return passwordHash;
	}

	/**
	 * Returns the character's contact list.
	 */

	public SortedListModel getContactList()
	{	return contactList;
	}

	/**
	 * Returns the list of items which are available from the hermit today.
	 */

	public SortedListModel getHermitItems()
	{	return hermitItems;
	}

	/**
	 * Returns the list of items which are available from the
	 * bounty hunter hunter today.
	 */

	public SortedListModel getBountyHunterItems()
	{	return hunterItems;
	}

	/**
	 * Returns the list of items which are available from
	 * Chez Snootee today.
	 */

	public LockableListModel getRestaurantItems()
	{	return restaurantItems;
	}

	/**
	 * Returns the list of items which are available from the
	 * Gnomish Micromicrobrewery today.
	 */

	public LockableListModel getMicrobreweryItems()
	{	return microbreweryItems;
	}

	/**
	 * Returns the list of cures which are currently available from
	 * Doc Galaktik
	 */

	public LockableListModel getGalaktikCures()
	{	return galaktikCures;
	}

	/**
	 * Returns whether or not the current user has a ten-leaf clover.
	 *
	 * @return	<code>true</code>
	 */

	public boolean isLuckyCharacter()
	{	return KoLCharacter.getInventory().contains( SewerRequest.CLOVER );
	}

	/**
	 * Utility method which ensures that the amount needed exists,
	 * and if not, calls the appropriate scripts to do so.
	 */

	private final boolean recover( int needed, String settingName, String currentName, String maximumName,
		String scriptProperty, String listProperty, Object [] techniques, Object [] fallbacks ) throws Exception
	{
		Object [] empty = new Object[0];
		Method currentMethod, maximumMethod;

		currentMethod = KoLCharacter.class.getMethod( currentName, new Class[0] );
		maximumMethod = KoLCharacter.class.getMethod( maximumName, new Class[0] );

		int initial = needed;
		int maximum = ((Number)maximumMethod.invoke( null, empty )).intValue();

		// First, check against the restore trigger to see if
		// any restoration needs to take place.

		double setting = Double.parseDouble( settings.getProperty( settingName ) );

		if ( !BuffBotHome.isBuffBotActive() )
		{
			needed = setting < 0 ? -1 : needed != 0 ? needed :
				(int) Math.max( setting * (double) maximum, (double) needed );

			if ( needed < 0 )
				return true;
		}

		int last = -1;
		int current = ((Number)currentMethod.invoke( null, empty )).intValue();

		// If a buffbot is currently running, only restore MP to
		// max when what you have is less than what you need.

		if ( BuffBotHome.isBuffBotActive() )
		{
			if ( current < needed )
				needed = maximum - 1;
		}
		else if ( needed >= maximum )
		{
			needed = maximum - 1;
		}

		if ( current >= needed )
			return true;

		// Next, check against the restore target to see how
		// far you need to go.

		int threshold = needed;
		setting = Double.parseDouble( settings.getProperty( settingName + "Target" ) );

		if ( initial == 0 )
			needed = (int) ( setting * (double) maximum );

		// First, attempt to recover using the appropriate script, if it exists.
		// This uses a lot of excessive reflection, but the idea is that it
		// checks the current value of the stat against the needed value of
		// the stat and makes sure that there's a change with every iteration.
		// If there is no change, it exists the loop.

		String scriptPath = settings.getProperty( scriptProperty ).trim();

		if ( !scriptPath.equals( "" ) )
		{
			while ( current < threshold && last != current && !refusesContinue() )
			{
				last = current;
				DEFAULT_SHELL.executeLine( scriptPath );
				current = ((Number)currentMethod.invoke( null, empty )).intValue();
			}
		}

		// If it gets this far, then you should attempt to recover
		// using the selected items.  This involves a few extra
		// reflection methods.

		String restoreSetting = settings.getProperty( listProperty ).trim();

		// Iterate through every single restore item, checking to
		// see if the settings wish to use this item.  If so, go ahead
		// and process the item's usage.

		String currentTechniqueName;

		for ( int i = 0; i < techniques.length && current < threshold; ++i )
		{
			currentTechniqueName = techniques[i].toString();
			if ( restoreSetting.indexOf( currentTechniqueName ) != -1 )
			{
				last = -1;

				while ( current < threshold && last != current && !refusesContinue() )
				{
					last = current;
					recoverOnce( techniques[i], currentTechniqueName, needed, false );
					current = ((Number)currentMethod.invoke( null, empty )).intValue();
				}
			}
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( current >= threshold && !refusesContinue() )
			return true;

		// Do the restoration, but without making any purchases
		// from NPC/PC stores.

		for ( int i = 0; i < fallbacks.length && current < threshold; ++i )
		{
			recoverOnce( fallbacks[i], fallbacks[i].toString(), needed, false );
			current = ((Number)currentMethod.invoke( null, empty )).intValue();
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( current >= threshold && !refusesContinue() )
			return true;

		// Now, last check -- go ahead and call the method which
		// invokes the fallback restores;

		for ( int i = 0; i < fallbacks.length && current < threshold; ++i )
		{
			recoverOnce( fallbacks[i], fallbacks[i].toString(), needed, true );
			current = ((Number)currentMethod.invoke( null, empty )).intValue();
		}

		// Fall-through check, just in case you've reached the
		// desired value.

		if ( current >= threshold && !refusesContinue() )
			return true;

		// If you failed to auto-recover and there are no settings,
		// make sure the user is aware of this.

		if ( scriptPath.equals( "" ) && restoreSetting.equals( "" ) )
			updateDisplay( ERROR_STATE, "No auto-restore settings found." );

		// Now you know for certain that you did not reach the
		// desired value.  There will be an error message that
		// is left over from previous attempts.

		this.currentState = ERROR_STATE;
		return false;
	}

	/**
	 * Utility method called inbetween battles.  This method
	 * checks to see if the character's HP has dropped below
	 * the tolerance value, and recovers if it has (if
	 * the user has specified this in their settings).
	 */

	protected final boolean recoverHP()
	{	return recoverHP( 0 );
	}

	public final boolean recoverHP( int recover )
	{
		try
		{
			return recover( recover, "hpAutoRecover", "getCurrentHP", "getMaximumHP",
				"hpRecoveryScript", "hpRestores", HPRestoreItemList.CONFIGURES, HPRestoreItemList.FALLBACKS );
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
	 * Utility method which uses the given recovery technique (not specified
	 * in a script) in order to restore.
	 */

	private final void recoverOnce( Object technique, String techniqueName, int needed, boolean isFallback )
	{
		// If the technique is an item, and the item is not readily available,
		// then don't bother with this item -- however, if it is the only item
		// present, then rethink it.

		if ( !ClassSkillsDatabase.contains( techniqueName ) )
		{
			AdventureResult item = new AdventureResult( techniqueName, 0 );
			if ( !KoLCharacter.getInventory().contains( item ) && !isFallback )
				return;
		}

		updateDisplay( "Recovering using " + techniqueName + "..." );

		if ( technique instanceof HPRestoreItemList.HPRestoreItem )
			((HPRestoreItemList.HPRestoreItem)technique).recoverHP( needed, isFallback );

		if ( technique instanceof MPRestoreItemList.MPRestoreItem )
			((MPRestoreItemList.MPRestoreItem)technique).recoverMP( needed, isFallback );
	}

	/**
	 * Returns the total number of mana restores currently
	 * available to the player.
	 */

	public int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = settings.getProperty( "mpRestores" );

		for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.CONFIGURES[i].toString() ) != -1 )
				restoreCount += MPRestoreItemList.CONFIGURES[i].getItem().getCount( KoLCharacter.getInventory() );

		for ( int i = 0; i < MPRestoreItemList.FALLBACKS.length; ++i )
			restoreCount += MPRestoreItemList.FALLBACKS[i].getItem().getCount( KoLCharacter.getInventory() );

		return restoreCount;
	}

	/**
	 * Utility method called inbetween commands.  This method
	 * checks to see if the character's MP has dropped below
	 * the tolerance value, and recovers if it has (if
	 * the user has specified this in their settings).
	 */

	protected final boolean recoverMP()
	{	return recoverMP( 0 );
	}

	/**
	 * Utility method which restores the character's current
	 * mana points above the given value.
	 */

	public final boolean recoverMP( int mpNeeded )
	{
		try
		{
			return recover( mpNeeded, "mpAutoRecover", "getCurrentMP", "getMaximumMP",
				"mpRecoveryScript", "mpRestores", MPRestoreItemList.CONFIGURES, MPRestoreItemList.FALLBACKS );
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
	 * Utility method used to process the results of any adventure
	 * in the Kingdom of Loathing.  This method searches for items,
	 * stat gains, and losses within the provided string.
	 *
	 * @param	results	The string containing the results of the adventure
	 * @return	<code>true</code> if any results existed
	 */

	public final boolean processResults( String results )
	{
		debugStream.println( "Processing results..." );

		if ( results.indexOf( "gains a pound" ) != -1 )
			KoLCharacter.incrementFamilarWeight();

		String plainTextResult = results.replaceAll( "<.*?>", "\n" );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, "\n" );
		String lastToken = null;

		Matcher damageMatcher = Pattern.compile( "you for ([\\d,]+) damage" ).matcher( plainTextResult );
		int lastDamageIndex = 0;

		while ( damageMatcher.find( lastDamageIndex ) )
		{
			lastDamageIndex = damageMatcher.end();
			String message = "You lose " + damageMatcher.group(1) + " hit points";

			DEFAULT_SHELL.printLine( message );
			parseResult( message );
		}

		damageMatcher = Pattern.compile( "You drop .*? ([\\d,]+) damage" ).matcher( plainTextResult );
		lastDamageIndex = 0;

		while ( damageMatcher.find( lastDamageIndex ) )
		{
			lastDamageIndex = damageMatcher.end();
			String message = "You lose " + damageMatcher.group(1) + " hit points";

			DEFAULT_SHELL.printLine( message );
			parseResult( message );
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
						DEFAULT_SHELL.printLine( acquisition + " " + item );
						parseItem( item );
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

						DEFAULT_SHELL.printLine( acquisition + " " + item );
						parseItem( itemName + " (" + countString + ")" );
					}
				}
				else
				{
					String effectName = parsedResults.nextToken();
					lastToken = parsedResults.nextToken();

					DEFAULT_SHELL.printLine( acquisition + " " + effectName + " " + lastToken );

					if ( lastToken.indexOf( "duration" ) == -1 )
					{
						parseEffect( effectName );
					}
					else
					{
						String duration = lastToken.substring( 11, lastToken.length() - 11 ).trim();
						parseEffect( effectName + " (" + duration + ")" );
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

				DEFAULT_SHELL.printLine( lastToken );

				requiresRefresh |= parseResult( lastToken.trim() );
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
			macroStream.print( KoLmafiaCLI.deriveCommand( request, iterations ) );
			forceContinue();

			// Handle the gym, which is the only adventure type
			// which needs to be specially handled.

			if ( request instanceof KoLAdventure )
			{
				KoLAdventure adventure = (KoLAdventure) request;
				if ( adventure.getRequest() == null )
				{
					(new ClanGymRequest( this, Integer.parseInt( adventure.getAdventureID() ), iterations )).run();
					return;
				}
			}

			int currentEffectCount = KoLCharacter.getEffects().size();

			boolean shouldRefreshStatus;

			// Otherwise, you're handling a standard adventure.  Be
			// sure to check to see if you're allowed to continue
			// after drunkenness.

			if ( KoLCharacter.isFallingDown() && request instanceof KoLAdventure && KoLCharacter.getInebriety() < 26 )
			{
				updateDisplay( ERROR_STATE, "You are too drunk to continue." );
				return;
			}

			// Check to see if there are any end conditions.  If
			// there are conditions, be sure that they are checked
			// during the iterations.

			int initialConditions = conditions.size();
			int remainingConditions = initialConditions;

			// If this is an adventure request, make sure that it
			// gets validated before running.

			if ( request instanceof KoLAdventure )
			{
				// Validate the adventure
				AdventureDatabase.validateAdventure( (KoLAdventure) request );
			}

			// Begin the adventuring process, or the request execution
			// process (whichever is applicable).

			int currentIteration = 0;
			boolean shouldEnableRefreshStatus = RequestFrame.isRefreshStatusEnabled();
			RequestFrame.setRefreshStatusEnabled( false );

			while ( permitsContinue() && ++currentIteration <= iterations )
			{
				// If the conditions existed and have been satisfied,
				// then you should stop.

				if ( conditions.size() < remainingConditions )
				{
					if ( conditions.size() == 0 || useDisjunction )
					{
						conditions.clear();
						remainingConditions = 0;
						break;
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
				else
					currentIterationString = "";

				if ( !(request instanceof KoLAdventure) )
					updateDisplay( currentIterationString );

				request.run();
				applyRecentEffects();

				// Decrement the counter to null out the increment
				// effect on the next iteration of the loop.

				if ( request instanceof KoLAdventure && permitsContinue() && ((KoLAdventure)request).getRequest().getAdventuresUsed() == 0 )
					--currentIteration;

				// Prevent drunkenness adventures from occurring by
				// testing inebriety levels after the request is run.

				if ( KoLCharacter.isFallingDown() && request instanceof KoLAdventure && KoLCharacter.getInebriety() < 26 )
				{
					updateDisplay( ERROR_STATE, "You are too drunk to continue." );
					return;
				}

				shouldRefreshStatus = currentEffectCount != KoLCharacter.getEffects().size();

				// One circumstance where you need a refresh is if
				// you gain/lose a status effect.

				shouldRefreshStatus |= currentEffectCount != KoLCharacter.getEffects().size();
				currentEffectCount = KoLCharacter.getEffects().size();

				// Another instance is if the player's equipment
				// results in recovery.

				shouldRefreshStatus |= request instanceof KoLAdventure && KoLCharacter.hasRecoveringEquipment();

				// However, if the request frame will refresh the
				// player's status, then do not refresh.

				shouldRefreshStatus &= !RequestFrame.willRefreshStatus();

				// If it turns out that you need to refresh the player's
				// status, go ahead and refresh it.

				if ( shouldRefreshStatus )
					CharpaneRequest.getInstance().run();

				// Account for the possibility that you could have run
				// out of adventures mid-request.

				if ( KoLCharacter.getAdventuresLeft() == 0 && request instanceof KoLAdventure )
					updateDisplay( PENDING_STATE, "Ran out of adventures." );
			}

			if ( shouldEnableRefreshStatus )
			{
				RequestFrame.setRefreshStatusEnabled( true );
				RequestFrame.refreshStatus();
			}

			currentIterationString = "";

			if ( currentState == PENDING_STATE )
			{
				// If we canceled the iteration without
				// generating a real error, permit
				// scripts to continue.

				forceContinue();
			}
			else if ( permitsContinue() )
			{
				// If you've completed the requests, make sure to update
				// the display.

				if ( request instanceof KoLAdventure && !conditions.isEmpty() )
					updateDisplay( ERROR_STATE, "Conditions not satisfied after " + (currentIteration - 1) +
						((currentIteration == 2) ? " request." : " requests.") );

				else if ( initialConditions != 0 && conditions.isEmpty() )
					updateDisplay( "Conditions satisfied after " + (currentIteration - 1) +
						((currentIteration == 2) ? " request." : " requests.") );

				else if ( !(request instanceof UseSkillRequest ) )
					updateDisplay( iterations > 1 ? "Requests completed." : "Request completed." );
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
		finally
		{
			System.gc();
		}
	}

	/**
	 * Removes the effects which are removed through a tiny house.
	 * This checks each status effect and checks the database to
	 * see if a tiny house will remove it.
	 */

	public void applyTinyHouseEffect()
	{
		Object [] effects = KoLCharacter.getEffects().toArray();
		AdventureResult currentEffect;

		for ( int i = effects.length - 1; i >= 0; --i )
		{
			currentEffect = (AdventureResult) effects[i];
			if ( StatusEffectDatabase.isTinyHouseClearable( currentEffect.getName() ) )
				KoLCharacter.getEffects().remove(i);
		}
	}

	/**
	 * Makes a request which attempts to remove the given effect.
	 */

	public abstract void makeUneffectRequest();

	/**
	 * Makes a request which attempts to zap a chosen item
	 */

	public abstract void makeZapRequest();

	/**
	 * Makes a request to the hermit in order to trade worthless
	 * items for more useful items.
	 */

	public abstract void makeHermitRequest();

	/**
	 * Makes a request to the trapper to trade yeti furs for
	 * other kinds of furs.
	 */

	public abstract void makeTrapperRequest();
	/**
	 * Makes a request to the hunter to trade today's bounty
	 * items in for meat.
	 */

	public abstract void makeHunterRequest();

	/**
	 * Makes a request to the untinkerer to untinker items
	 * into their component parts.
	 */

	public abstract void makeUntinkerRequest();

	/**
	 * Makes a request to set the mind control device to the desired value
	 */

	public abstract void makeMindControlRequest();

	/**
	 * Completes the infamous tavern quest.
	 */

	public int locateTavernFaucet()
	{
		int faucetRow = 0;
		int faucetColumn = 0;

		if ( KoLCharacter.getLevel() < 3 )
		{
			updateDisplay( ERROR_STATE, "You need to level up first." );
			return -1;
		}

		if ( KoLCharacter.getAdventuresLeft() < 25 )
		{
			updateDisplay( ERROR_STATE, "You need to have at least 25 adventures to find the faucet." );
			return -1;
		}

		(new KoLRequest( this, "council.php", true )).run();
		updateDisplay( "Searching for faucet..." );

		KoLAdventure adventure = new KoLAdventure( this, "", "rats.php", "", "Typical Tavern (Pre-Rat)" );
		adventure.run();

		ArrayList searchList = new ArrayList();
		for ( int i = 1; i <= 25; ++i )
			searchList.add( new Integer(i) );

		Integer searchIndex = new Integer(0);

		// Random guess instead of straightforward search
		// for the location of the faucet (lowers the chance
		// of bad results if the faucet is near the end).

		while ( !searchList.isEmpty() && KoLCharacter.getCurrentHP() > 0 &&
			(adventure.getRequest().responseText == null || adventure.getRequest().responseText.indexOf( "faucetoff" ) == -1) )
		{
			searchIndex = (Integer) searchList.get( RNG.nextInt( searchList.size() ) );
			searchList.remove( searchIndex );

			adventure.getRequest().addFormField( "where", searchIndex.toString() );
			adventure.run();
		}

		// If you successfully find the location of the
		// rat faucet, then you've got it.

		if ( permitsContinue() )
		{
			KoLCharacter.processResult( new AdventureResult( AdventureResult.ADV, 1 ) );
			faucetRow = (int) ((searchIndex.intValue() - 1) / 5) + 1;
			faucetColumn = (searchIndex.intValue() - 1) % 5 + 1;
			updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
			return ( faucetRow - 1 ) * 5 + (faucetColumn - 1);
		}

		return -1;
	}

	/**
	 * Trades items with the guardian of the goud.
	 */

	public void tradeGourdItems()
	{
		updateDisplay( "Determining items needed..." );
		KoLRequest request = new KoLRequest( this, "town_right.php?place=gourd", true );
		request.run();

		// For every class, it's the same -- the message reads, "Bring back"
		// and then the number of the item needed.  Compare how many you need
		// with how many you have.

		Matcher neededMatcher = Pattern.compile( "Bring back (\\d+)" ).matcher( request.responseText );
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

		int neededCount = neededMatcher.find() ? Integer.parseInt( neededMatcher.group(1) ) : 26;

		while ( neededCount <= 25 && neededCount <= item.getCount( KoLCharacter.getInventory() ) )
		{
			updateDisplay( "Giving up " + neededCount + " " + item.getName() + "s..." );
			request = new KoLRequest( this, "town_right.php?place=gourd&action=gourd", true );
			request.run();

			processResult( item.getInstance( 0 - neededCount++ ) );
		}

		int totalProvided = 0;
		for ( int i = 5; i < neededCount; ++i )
			totalProvided += i;

		updateDisplay( "Gourd trading complete (" + totalProvided + " " + item.getName() + "s given so far)." );
	}

	public void unlockGuildStore()
	{
		// Refresh the player's stats in order to get current
		// stat values to see if the quests can be completed.

		(new CharsheetRequest( this )).run();

		int baseStatValue = 0;
		int totalStatValue = 0;

		switch ( KoLCharacter.getPrimeIndex() )
		{
			case 0:
				baseStatValue = KoLCharacter.getBaseMuscle();
				totalStatValue = baseStatValue + KoLCharacter.getAdjustedMuscle();
				break;

			case 1:
				baseStatValue = KoLCharacter.getBaseMysticality();
				totalStatValue = baseStatValue + KoLCharacter.getAdjustedMysticality();
				break;

			case 2:
				baseStatValue = KoLCharacter.getBaseMoxie();
				totalStatValue = baseStatValue + KoLCharacter.getAdjustedMoxie();
				break;
		}

		// The wiki claims that your prime stats are somehow connected,
		// but the exact procedure is uncertain.  Therefore, just allow
		// the person to attempt to unlock their store, regardless of
		// their current stats.

		updateDisplay( "Entering guild challenge area..." );
		KoLRequest request = new KoLRequest( this, "guild.php?place=challenge", true );
		request.run();

		for ( int i = 1; i <= 4; ++i )
		{
			updateDisplay( "Completing guild task " + i + "..." );
			request = new KoLRequest( this, "guild.php?action=chal", true );
			request.run();
		}

		processResult( new AdventureResult( AdventureResult.ADV, -4 ) );

		if ( KoLCharacter.getLevel() >= 4 )
		{
			request = new KoLRequest( this, "guild.php?place=paco", true );
			request.run();
		}

		updateDisplay( "Guild store unlocked (maybe)." );
	}

	public void priceItemsAtLowestPrice()
	{
		(new StoreManageRequest( this )).run();

		// Now determine the desired prices on items.
		// If the value of an item is currently 100,
		// then remove the item from the store.

		StoreManager.SoldItem [] sold = new StoreManager.SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		int [] itemID = new int[ sold.length ];
		int [] prices = new int[ sold.length ];
		int [] limits = new int[ sold.length ];

		for ( int i = 0; i < sold.length; ++i )
		{
			limits[i] = sold[i].getLimit();
			itemID[i] = sold[i].getItemID();

			int minimumPrice = TradeableItemDatabase.getPriceByID( sold[i].getItemID() ) * 2;
			if ( sold[i].getPrice() == 999999999 && minimumPrice > 0 )
			{
				minimumPrice = Math.max( 100, minimumPrice );
				int desiredPrice = sold[i].getLowest() - (sold[i].getLowest() % 100);
				prices[i] = desiredPrice < minimumPrice ? minimumPrice : desiredPrice;
			}
			else
				prices[i] = sold[i].getPrice();
		}

		(new StoreManageRequest( this, itemID, prices, limits )).run();
		updateDisplay( "Repricing complete." );
	}

	/**
	 * Show an HTML string to the user
	 */

	public abstract void showHTML( String text, String title );

	/**
	 * Retrieves whether or not continuation of an adventure or request
	 * is permitted by the client, or by current circumstances in-game.
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public final boolean permitsContinue()
	{	return currentState == CONTINUE_STATE;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request
	 * will be denied by the client, regardless of continue state reset,
	 * until the display is enable (ie: in an abort state).
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public final boolean refusesContinue()
	{	return currentState == ABORT_STATE;
	}

	/**
	 * Forces a continue state.  This should only be called when
	 * there is no doubt that a continue should occur.
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public final void forceContinue()
	{	currentState = CONTINUE_STATE;
	}

	/**
	 * Initializes a stream for logging debugging information.  This
	 * method creates a <code>KoLmafia.log</code> file in the default
	 * directory if one does not exist, or appends to the existing
	 * log.  This method should only be invoked if the user wishes to
	 * assist in beta testing because the output is VERY verbose.
	 */

	public static final void openDebugStream()
	{
		// First, ensure that a log stream has not already been
		// initialized - this can be checked by observing what
		// class the current log stream is.

		if ( !(debugStream instanceof NullStream) )
			return;

		try
		{
			File f = new File( "KoLmafia.log" );

			if ( !f.exists() )
				f.createNewFile();

			debugStream = new LogStream( f );

			debugStream.println();
			debugStream.println();
			debugStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			debugStream.println( "            Beginning New Logging Session (" + VERSION_NAME + ")" );
			debugStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			debugStream.println( " Please note: do not post these logs in the KoLmafia thread.  If " );
			debugStream.println( " you would like us to look at the log, please instead email logs " );
			debugStream.println( " to holatuwol@hotmail.com using the subject [KoLmafia] Debug Log " );
			debugStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
			debugStream.println();
			debugStream.println();
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void closeDebugStream()
	{
		debugStream.close();
		debugStream = NullStream.INSTANCE;
	}

	/**
	 * Initializes a stream for logging debugging information.  This
	 * method creates a <code>KoLmafia.log</code> file in the default
	 * directory if one does not exist, or appends to the existing
	 * log.  This method should only be invoked if the user wishes to
	 * assist in beta testing because the output is VERY verbose.
	 */

	public static final void openSessionStream()
	{
		// First, ensure that a log stream has not already been
		// initialized - this can be checked by observing what
		// class the current log stream is.

		if ( !(sessionStream instanceof NullStream) )
			return;

		try
		{
			File f = new File( "logs/" + KoLCharacter.getUsername() + "_" +
				sdf.format( new Date() ) + ".txt" );

			if ( !f.getParentFile().exists() )
				f.mkdirs();

			if ( !f.exists() )
				f.createNewFile();

			sessionStream = new LogStream( f );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final void closeSessionStream()
	{
		sessionStream.close();
		sessionStream = NullStream.INSTANCE;
	}

	/**
	 * Retrieves the current settings for the current session.  Note
	 * that if this is invoked before initialization, this method
	 * will return the global settings.
	 *
	 * @return	The settings for the current session
	 */

	public final KoLSettings getSettings()
	{	return settings;
	}

	/**
	 * Retrieves the stream currently used for logging output for
	 * the URL/session logger.
	 */

	public static final PrintStream getSessionStream()
	{	return sessionStream;
	}

	/**
	 * Retrieves the stream currently used for logging debug output.
	 * @return	The stream used for debug output
	 */

	public static final PrintStream getDebugStream()
	{	return debugStream;
	}

	/**
	 * Initializes the macro recording stream.  This will only
	 * work if no macro streams are currently running.  If
	 * a call is made while a macro stream exists, this method
	 * does nothing.
	 *
	 * @param	filename	The name of the file to be created
	 */

	public static final void openMacroStream( String filename )
	{
		// First, ensure that a macro stream has not already been
		// initialized - this can be checked by observing what
		// class the current macro stream is.

		if ( !(macroStream instanceof NullStream) )
			return;

		try
		{
			File f = new File( filename );

			if ( !f.exists() )
			{
				f.getParentFile().mkdirs();
				f.createNewFile();
			}

			macroStream = new PrintStream( new FileOutputStream( f, false ) );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Retrieves the macro stream.
	 * @return	The macro stream associated with this client
	 */

	public static final PrintStream getMacroStream()
	{	return macroStream;
	}

	/**
	 * Deinitializes the macro stream.
	 */

	public static final void closeMacroStream()
	{
		macroStream.close();
		macroStream = NullStream.INSTANCE;
	}

	/**
	 * Utility method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public final void addSaveState( String username, String password )
	{
		try
		{
			if ( !saveStateNames.contains( username ) )
				saveStateNames.add( username );

			storeSaveStates();
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

			GLOBAL_SETTINGS.setProperty( "saveState." + username.toLowerCase(), (new BigInteger( encodedString.toString(), 36 )).toString( 10 ) );
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public void removeSaveState( String loginname )
	{
		if ( loginname == null )
			return;

		for ( int i = 0; i < saveStateNames.size(); ++i )
			if ( ((String)saveStateNames.get(i)).equalsIgnoreCase( loginname ) )
			{
				saveStateNames.remove( i );
				storeSaveStates();
				return;
			}
	}

	private final void storeSaveStates()
	{
		StringBuffer saveStateBuffer = new StringBuffer();
		String [] names = new String[ saveStateNames.size() ];
		saveStateNames.toArray( names );

		List lowerCaseNames = new ArrayList();
		for ( int i = 0; i < names.length; ++i )
		{
			if ( lowerCaseNames.contains( names[i].toLowerCase() ) )
			{
				saveStateNames.remove( names[i] );
				lowerCaseNames.remove( names[i].toLowerCase() );
			}

			lowerCaseNames.add( names[i].toLowerCase() );
		}

		if ( names.length != saveStateNames.size() )
		{
			names = new String[ saveStateNames.size() ];
			saveStateNames.toArray( names );
		}

		if ( names.length > 0 )
		{
			saveStateBuffer.append( names[0] );
			for ( int i = 1; i < names.length; ++i )
			{
				saveStateBuffer.append( "//" );
				saveStateBuffer.append( names[i] );
			}
		}

		GLOBAL_SETTINGS.setProperty( "saveState", saveStateBuffer.toString() );

		// Now, removing any passwords that were stored
		// which are no longer in the save state list

		String [] settingsArray = new String[ GLOBAL_SETTINGS.keySet().size() ];
		GLOBAL_SETTINGS.keySet().toArray( settingsArray );

		for ( int i = 0; i < settingsArray.length; ++i )
			if ( settingsArray[i].startsWith( "saveState." ) && !lowerCaseNames.contains( settingsArray[i].substring( 10 ) ) )
				GLOBAL_SETTINGS.remove( settingsArray[i] );
	}

	/**
	 * Utility method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public final String getSaveState( String loginname )
	{
		try
		{
			Object [] settingKeys = GLOBAL_SETTINGS.keySet().toArray();
			String password = null;
			String lowerCaseKey = "saveState." + loginname.toLowerCase();
			String currentKey;

			for ( int i = 0; i < settingKeys.length && password == null; ++i )
			{
				currentKey = (String) settingKeys[i];
				if ( currentKey.equals( lowerCaseKey ) )
					password = GLOBAL_SETTINGS.getProperty( currentKey );
			}

			if ( password == null )
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

	public SortedListModel getSessionTally()
	{	return tally;
	}

	public SortedListModel getConditions()
	{	return conditions;
	}

	public LockableListModel getAdventureList()
	{	return adventureList;
	}

	public LockableListModel getEncounterList()
	{	return encounterList;
	}

	public void executeTimeInRequest()
	{
		deinitialize();  forceContinue();
		updateDisplay( "Timing in session..." );

		cachedLogin.run();
		if ( getPasswordHash() == null )
			cachedLogin.run();

		while ( getPasswordHash() == null )
		{
			StaticEntity.executeCountdown( "Next login in ", 300 );
			cachedLogin.run();
		}

		updateDisplay( "Session timed in." );
	}

	public boolean checkRequirements( List requirements )
	{
		AdventureResult [] requirementsArray = new AdventureResult[ requirements.size() ];
		requirements.toArray( requirementsArray );

		int missingCount;
		missingItems.clear();

		// Check the items required for this quest,
		// retrieving any items which might be inside
		// of a closet somewhere.

		for ( int i = 0; i < requirementsArray.length; ++i )
		{
			if ( requirementsArray[i] == null )
				continue;

			missingCount = 0;

			if ( requirementsArray[i].isItem() )
			{
				AdventureDatabase.retrieveItem( requirementsArray[i] );
				missingCount = requirementsArray[i].getCount() - requirementsArray[i].getCount( KoLCharacter.getInventory() );
			}
			else if ( requirementsArray[i].isStatusEffect() )
			{
				// Status effects should be compared against
				// the status effects list.  This is used to
				// help people detect which effects they are
				// missing (like in PVP).

				missingCount = requirementsArray[i].getCount() - requirementsArray[i].getCount( KoLCharacter.getEffects() );
			}
			else if ( requirementsArray[i].getName().equals( AdventureResult.MEAT ) )
			{
				// Currency is compared against the amount
				// actually liquid -- amount in closet is
				// ignored in this case.

				missingCount = requirementsArray[i].getCount() - KoLCharacter.getAvailableMeat();
			}

			if ( missingCount > 0 )
			{
				// If there are any missing items, add
				// them to the list of needed items.

				missingItems.add( requirementsArray[i].getInstance( missingCount ) );
			}
		}

		// If there are any missing requirements
		// be sure to return false.  Otherwise,
		// you managed to get everything.

		return missingItems.isEmpty();
	}

	/**
	 * Utility method used to print a list to the given output
	 * stream.  If there's a need to print to the current output
	 * stream, simply pass the output stream to this method.
	 */

	protected void printList( List printing )
	{
		Object [] elements = new Object[ printing.size() ];
		printing.toArray( elements );

		for ( int i = 0; i < elements.length; ++i )
			DEFAULT_SHELL.printLine( elements[i].toString() );
	}

	/**
	 * Utility method used to purchase the given number of items
	 * from the mall using the given purchase requests.
	 */

	public void makePurchases( List results, Object [] purchases, int maxPurchases )
	{
		if ( purchases.length > 0 && purchases[0] instanceof MallPurchaseRequest )
			macroStream.print( "buy " + maxPurchases + " " + ((MallPurchaseRequest)purchases[0]).getItemName() );

		forceContinue();
		MallPurchaseRequest currentRequest;
		int purchaseCount = 0;

		for ( int i = 0; i < purchases.length && purchaseCount != maxPurchases && permitsContinue(); ++i )
		{
			if ( purchases[i] instanceof MallPurchaseRequest )
			{
				currentRequest = (MallPurchaseRequest) purchases[i];

				if ( !KoLCharacter.canInteract() && currentRequest.getQuantity() != MallPurchaseRequest.MAX_QUANTITY )
				{
					updateDisplay( ERROR_STATE, "You are not yet out of ronin." );
					return;
				}

				AdventureResult result = new AdventureResult( currentRequest.getItemName(), 0, false );

				// Keep track of how many of the item you had before
				// you run the purchase request

				int oldResultCount = result.getCount( KoLCharacter.getInventory() );
				int previousLimit = currentRequest.getLimit();

				currentRequest.setLimit( Math.min( previousLimit, maxPurchases - purchaseCount ) );
				currentRequest.run();

				// Calculate how many of the item you have now after
				// you run the purchase request

				int newResultCount = result.getCount( KoLCharacter.getInventory() );
				purchaseCount += newResultCount - oldResultCount;

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
			}
		}

		// With all that information parsed out, we should
		// refresh the lists at the very end.

		KoLCharacter.refreshCalculatedLists();

		if ( purchaseCount == maxPurchases || maxPurchases == Integer.MAX_VALUE )
			updateDisplay( "Purchases complete." );
		else
			updateDisplay( ERROR_STATE, "Desired purchase quantity not reached." );
	}

	/**
	 * Utility method used to register a given adventure in
	 * the running adventure summary.
	 */

	public void registerAdventure( KoLAdventure adventureLocation )
	{
		String adventureName = adventureLocation.getAdventureName();
		RegisteredEncounter lastAdventure = (RegisteredEncounter) adventureList.lastElement();

		if ( lastAdventure != null && lastAdventure.name.equals( adventureName ) )
		{
			++lastAdventure.encounterCount;

			// Manually set to force repainting in GUI
			adventureList.set( adventureList.size() - 1, lastAdventure );
		}
		else
			adventureList.add( new RegisteredEncounter( adventureName ) );
	}

	/**
	 * Utility method used to register a given encounter in
	 * the running adventure summary.
	 */

	public void registerEncounter( String encounterName )
	{
		encounterName = encounterName.toLowerCase().trim();

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

		encounterList.add( new RegisteredEncounter( encounterName ) );
	}

	private class RegisteredEncounter implements Comparable
	{
		private String name;
		private int encounterCount;

		public RegisteredEncounter( String name )
		{
			this.name = name;
			encounterCount = 1;
		}

		public String toString()
		{	return name + " (" + encounterCount + ")";
		}

		public int compareTo( Object o )
		{
			return !(o instanceof RegisteredEncounter) || o == null ? -1 :
				name.compareToIgnoreCase( ((RegisteredEncounter)o).name );
		}
	}

	public KoLRequest getCurrentRequest()
	{	return currentRequest;
	}

	public void setCurrentRequest( KoLRequest request)
	{	currentRequest = request;
	}

	public void setLocalProperty( String property, String value )
	{	LOCAL_SETTINGS.setProperty( property, value );
	}

	public void setLocalProperty( String property, boolean value )
	{	LOCAL_SETTINGS.setProperty( property, String.valueOf( value ) );
	}

	public void setLocalProperty( String property, int value )
	{	LOCAL_SETTINGS.setProperty( property, String.valueOf( value ) );
	}

	public String getLocalProperty( String property )
	{
		String value = LOCAL_SETTINGS.getProperty( property );
		return ( value == null) ? "" : value;
	}

	public boolean getLocalBooleanProperty( String property )
	{
		String value = LOCAL_SETTINGS.getProperty( property );
		return ( value == null) ? false : value.equals( "true" );
	}

	public int getLocalIntegerProperty( String property )
	{
		String value = LOCAL_SETTINGS.getProperty( property );
		return ( value == null) ? 0 : Integer.parseInt( value );
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
			targets[i] = getPlayerID( targets[i] ) == null ? targets[i] :
				getPlayerID( targets[i] );

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

		// Convert all the user IDs back to the
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
		DEFAULT_SHELL.updateDisplay( "Downloading override data files..." );

		try
		{
			for ( int i = 0; i < OVERRIDE_DATA.length; ++i )
			{
				BufferedReader reader = new BufferedReader( new InputStreamReader(
					(InputStream) (new URL( "http://kolmafia.sourceforge.net/data/" + OVERRIDE_DATA[i] )).getContent() ) );

				File output = new File( "data/" + OVERRIDE_DATA[i] );
				if ( output.exists() )
					output.delete();

				String line;
				PrintStream writer = new PrintStream( new FileOutputStream( output ) );

				while ( (line = reader.readLine()) != null )
					writer.println( line );

				writer.close();
			}
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Data file update failed" );
			return;
		}

		DEFAULT_SHELL.updateDisplay( "Download completed.  Please restart to complete the update." );
	}

	public void runBetweenBattleChecks()
	{
		// Before running the request, make sure you have enough
		// mana and health to continue.

		if ( !(getCurrentRequest() instanceof CampgroundRequest) )
		{
			String scriptPath = StaticEntity.getProperty( "betweenBattleScript" );
			if ( !scriptPath.equals( "" ) )
				DEFAULT_SHELL.executeLine( scriptPath );

			recoverHP();
			recoverMP();
		}

		if ( permitsContinue() )
		{
			updateDisplay( currentIterationString );
			currentIterationString = "";
		}
	}

	public void startRelayServer()
	{
		LocalRelayServer.startThread();

		// Wait for 5 seconds before giving up
		// on the relay server.

		for ( int i = 0; i < 50 && !LocalRelayServer.isRunning(); ++i )
			KoLRequest.delay( 100 );

		if ( !LocalRelayServer.isRunning() )
			return;

		// Even after the wait, sometimes, the
		// worker threads have not been filled.

		StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + (KoLRequest.isCompactMode ? "/main_c.html" : "/main.html") );
	}

	public void launchSimulator()
	{
		LocalRelayServer.startThread();

		// Wait for 5 seconds before giving up
		// on the relay server.

		for ( int i = 0; i < 50 && !LocalRelayServer.isRunning(); ++i )
			KoLRequest.delay( 100 );

		if ( !LocalRelayServer.isRunning() )
			return;

		// Even after the wait, sometimes, the
		// worker threads have not been filled.

		StaticEntity.openSystemBrowser( "http://127.0.0.1:" + LocalRelayServer.getPort() + "/KoLmafia/simulator/index.html" );
	}

	public void declareWorldPeace()
	{	DEFAULT_SHELL.updateDisplay( ABORT_STATE, "KoLmafia declares world peace." );
	}

	public boolean shouldMakeConflictingRequest()
	{	return currentRequest == null || !(currentRequest instanceof FightRequest) || currentRequest.getAdventuresUsed() == 1;
	}

	public void removeAllItemsFromStore()
	{
		(new StoreManageRequest( this )).run();

		// Now determine the desired prices on items.
		// If the value of an item is currently 100,
		// then remove the item from the store.

		StoreManager.SoldItem [] sold = new StoreManager.SoldItem[ StoreManager.getSoldItemList().size() ];
		StoreManager.getSoldItemList().toArray( sold );

		for ( int i = 0; i < sold.length && permitsContinue(); ++i )
			(new StoreManageRequest( this, sold[i].getItemID() )).run();

		updateDisplay( "Store emptying complete." );
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

		if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
			"Are you sure you'd like to host an end-of-run sale?", "MASSIVE SALE", JOptionPane.YES_NO_OPTION ) )
				return;

		// Find all tradeable items.  Tradeable items
		// are marked by an autosell value of nonzero.

		AdventureResult [] items = new AdventureResult[ KoLCharacter.getInventory().size() ];
		KoLCharacter.getInventory().toArray( items );

		ArrayList autosell = new ArrayList();
		ArrayList automall = new ArrayList();

		// Only place items in the mall which are not
		// sold in NPC stores -- everything else, make
		// sure you autosell.

		for ( int i = 0; i < items.length; ++i )
		{
			if ( TradeableItemDatabase.getPriceByID( items[i].getItemID() ) != 0 )
			{
				if ( NPCStoreDatabase.contains( items[i].getName() ) )
					autosell.add( items[i] );
				else
					automall.add( items[i] );
			}
		}

		// Now, place all the items in the mall at the
		// maximum possible price.  This allows KoLmafia
		// to determine the minimum price.

		if ( autosell.size() > 0 && permitsContinue() )
			(new AutoSellRequest( this, autosell.toArray(), AutoSellRequest.AUTOSELL )).run();

		if ( automall.size() > 0 && permitsContinue() )
			(new AutoSellRequest( this, automall.toArray(), AutoSellRequest.AUTOMALL )).run();

		// Now, remove all the items that you intended
		// to remove from the store due to pricing issues.

		if ( permitsContinue() )
			priceItemsAtLowestPrice();

		updateDisplay( "Undercutting sale complete." );
	}

	protected void handleAscension()
	{
		refreshSession();
		sessionStream.println();
		sessionStream.println();
		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		sessionStream.println( "           Beginning New Ascension           " );
		sessionStream.println( "=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=" );
		sessionStream.println();
		sessionStream.println();
	}
}
