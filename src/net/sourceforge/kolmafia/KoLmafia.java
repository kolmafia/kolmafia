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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.math.BigInteger;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
	protected static LimitedSizeChatBuffer commandBuffer;

	protected static final String [] hermitItemNames = { "ten-leaf clover", "wooden figurine", "hot buttered roll", "banjo strings",
		"jabañero pepper", "fortune cookie", "golden twig", "ketchup", "catsup", "sweet rims", "dingy planks", "volleyball" };
	protected static final int [] hermitItemNumbers = { 24, 46, 47, 52, 55, 61, 66, 106, 107, 135, 140, 527 };

	protected static final String [] trapperItemNames = { "yak skin", "penguin skin", "hippopotamus skin" };
	protected static final int [] trapperItemNumbers = { 394, 393, 395 };

	protected static final AdventureResult CLOVER = new AdventureResult( 24, 1 );

	protected boolean isLoggingIn;
	protected LoginRequest loginRequest;

	protected String password, sessionID, passwordHash;

	private boolean disableMacro;
	protected KoLSettings settings;
	protected PrintStream logStream;
	protected PrintStream macroStream;

	protected int currentState;
	protected boolean permitContinue;

	protected int [] initialStats;
	protected int [] fullStatGain;

	protected KoLMessenger loathingChat;
	protected ClanManager clanManager;

	protected SortedListModel saveStateNames;
	protected List recentEffects;

	private TreeMap seenPlayerIDs;
	private TreeMap seenPlayerNames;

	private int pullsRemaining;
	protected LockableListModel adventureList;
	protected SortedListModel tally, missingItems;
	protected SortedListModel usableItems, sellableItems, hunterItems, storage;

	protected boolean useDisjunction;
	protected SortedListModel conditions;

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
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk.
	 */

	public KoLmafia()
	{
		this.isLoggingIn = true;
		this.useDisjunction = false;

		this.initialStats = new int[3];
		this.fullStatGain = new int[3];

		this.settings = GLOBAL_SETTINGS;
		this.saveStateNames = new SortedListModel();

		String [] currentNames = settings.getProperty( "saveState" ).split( "//" );
		for ( int i = 0; i < currentNames.length; ++i )
			saveStateNames.add( currentNames[i] );

		// This line is added to clear out data from previous
		// releases of KoLmafia - the extra disk access does
		// affect performance, but not significantly.

		storeSaveStates();
		deinitialize();

		deinitializeLogStream();
		deinitializeMacroStream();

		seenPlayerIDs = new TreeMap();
		seenPlayerNames = new TreeMap();
		adventureList = new LockableListModel();
		conditions = new SortedListModel();
		missingItems = new SortedListModel();
		commandBuffer = null;
	}

	public boolean isEnabled()
	{	return true;
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public synchronized void updateDisplay( int state, String message )
	{
		this.currentState = state;

		if ( commandBuffer != null )
		{
			StringBuffer colorBuffer = new StringBuffer();
			if ( state == ERROR_STATE || state == CANCELLED_STATE )
				colorBuffer.append( "<font color=red>" );
			else
				colorBuffer.append( "<font color=black>" );

			colorBuffer.append( message.indexOf( LINE_BREAK ) != -1 ? ("<pre>" + message + "</pre>") : message );
			colorBuffer.append( "</font><br>" );
			colorBuffer.append( LINE_BREAK );

			commandBuffer.append( colorBuffer.toString() );
		}
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String loginname, String sessionID, boolean getBreakfast, boolean isQuickLogin )
	{
		// Initialize the variables to their initial
		// states to avoid null pointers getting thrown
		// all over the place

		this.sessionID = sessionID;

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		if ( KoLCharacter.getUsername().equals( "" ) )
		{
			this.pullsRemaining = -1;
			FamiliarData.reset();

			KoLCharacter.reset( loginname );
			CharpaneRequest.reset();
			MushroomPlot.reset();
			StoreManager.reset();
			CakeArenaManager.reset();
			MuseumManager.reset();

			this.conditions.clear();
			this.missingItems.clear();

			this.usableItems = new SortedListModel();
			this.sellableItems = new SortedListModel();
			this.hunterItems = new SortedListModel();
			this.storage = new SortedListModel();
			this.recentEffects = new ArrayList();

			this.tally = new SortedListModel();
			resetSessionTally();
		}

		// Get the password hash for the session

		(new PasswordHashRequest( this )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		// Reset the adventure list to use the
		// new password hash

		adventureList.clear();
		adventureList.addAll( AdventureDatabase.getAsLockableListModel() );

		// Get current moon phases

		(new MoonPhaseRequest( this )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		// Retrieve the player data -- just in
		// case adventures or HP changed.

		(new CharsheetRequest( this )).run();
		registerPlayer( loginname, String.valueOf( KoLCharacter.getUserID() ) );

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		// If this is a time-in request, this is all the
		// data that is actually required.

		if ( loginRequest != null )
			return;

		// Retrieve campground data to see if the user is able to
		// cook, make drinks or make toast.

		updateDisplay( DISABLED_STATE, "Retrieving campground data..." );
		(new CampgroundRequest( this )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		// Retrieve the list of familiars which are available to
		// the player, if they haven't opted to skip them.

		if ( !isQuickLogin && settings.getProperty( "skipFamiliars" ).equals( "false" ) )
			(new FamiliarRequest( this )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		// Retrieve the list of outfits which are available to the
		// character, should this be something they want.

		if ( !isQuickLogin && settings.getProperty( "skipOutfits" ).equals( "false" ) )
			(new EquipmentRequest( this, EquipmentRequest.EQUIPMENT )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		// Retrieve the items which are available for consumption
		// and item creation.

		if ( !isQuickLogin && settings.getProperty( "skipInventory" ).equals( "false" ) )
			(new EquipmentRequest( this, EquipmentRequest.CLOSET )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		resetSessionTally();
		applyRecentEffects();

		// Retrieve breakfast if the option to retrieve breakfast
		// was previously selected.

		if ( getBreakfast )
			getBreakfast();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		this.isLoggingIn = false;
		this.settings = new KoLSettings( loginname );
		this.clanManager = new ClanManager( this );
		this.permitContinue = true;

		// There's a possibility that the adventure list settings
		// for global use are different from the list settings for
		// individual use.  Recompile the adventure list.

		adventureList.clear();
		adventureList.addAll( AdventureDatabase.getAsLockableListModel() );
		MPRestoreItemList.reset();
	}

	/**
	 * Utility method used to notify the client that it should attempt
	 * to retrieve breakfast.
	 */

	public void getBreakfast()
	{
		updateDisplay( DISABLED_STATE, "Retrieving breakfast..." );

		if ( KoLCharacter.hasToaster() )
			for ( int i = 0; i < 3 && permitContinue; ++i )
				(new CampgroundRequest( this, "toast" )).run();

		permitContinue = true;

		if ( KoLCharacter.hasArches() )
			(new CampgroundRequest( this, "arches" )).run();

		permitContinue = true;

		if ( KoLCharacter.canSummonNoodles() )
			(new UseSkillRequest( this, "Pastamastery", "", 3 )).run();

		permitContinue = true;

		if ( KoLCharacter.canSummonReagent() )
			(new UseSkillRequest( this, "Advanced Saucecrafting", "", 3 )).run();

		permitContinue = true;

		if ( KoLCharacter.canSummonShore() )
			(new UseSkillRequest( this, "Advanced Cocktailcrafting", "", 3 )).run();

		permitContinue = true;
		updateDisplay( ENABLED_STATE, "Breakfast retrieved." );
	}

	/**
	 * Requests daily buffs from Clan Otori's standard buffbots.
	 */

	public void pwnClanOtori()
	{
		String todaySetting = sdf.format( new Date() );

		if ( settings.getProperty( "lastOtoriRequest" ).equals( todaySetting ) )
		{
			updateDisplay( ERROR_STATE, "Sorry, Otori can only be pwned once a day." );
			return;
		}

		settings.setProperty( "lastOtoriRequest", todaySetting );
		updateDisplay( DISABLED_STATE, "Pwning Clan Otori..." );

		(new GreenMessageRequest( this, "79826", "I really didn't want to do this...", new AdventureResult( AdventureResult.MEAT, 1 ) )).run();
		(new GreenMessageRequest( this, "79826", "The hermit made me do it!", new AdventureResult( AdventureResult.MEAT, 2 ) )).run();
		(new GreenMessageRequest( this, "79826", "Or was it Toot Oriole?  -hic-", new AdventureResult( AdventureResult.MEAT, 3 ) )).run();

		(new GreenMessageRequest( this, "121179", "I'm flipping you upside-down, turtle boy.", new AdventureResult( AdventureResult.MEAT, 1 ) )).run();

		(new GreenMessageRequest( this, "246325", "Mint says, \"World domination, baby!\"", new AdventureResult( AdventureResult.MEAT, 1 ) )).run();
		(new GreenMessageRequest( this, "246325", "Oh, and KoLmafia says, \"Your shoes are mine, [censored]!\"", new AdventureResult( AdventureResult.MEAT, 2 ) )).run();
		(new GreenMessageRequest( this, "246325", "But you're a bot, too, so you won't see this.  Sadness.", new AdventureResult( AdventureResult.MEAT, 3 ) )).run();

		permitContinue = true;
		updateDisplay( ENABLED_STATE, "Pwning of Clan Otori complete." );
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		sessionID = null;
		passwordHash = null;
		loginRequest = null;
		permitContinue = false;
	}

	/**
	 * Used to reset the session tally to its original values.
	 */

	public void resetSessionTally()
	{
		tally.clear();

		initialStats[0] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() );
		initialStats[1] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() );
		initialStats[2] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() );

		fullStatGain[0] = 0;
		fullStatGain[1] = 0;
		fullStatGain[2] = 0;

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

	public void parseResult( String result )
	{
		String trimResult = result.trim();

		// Because of the simplified parsing, there's a chance that
		// the "gain" acquired wasn't a subpoint (in other words, it
		// includes the word "a" or "some"), which causes a NFE or
		// possibly a ParseException to be thrown.  Catch them and
		// do nothing (eventhough it's technically bad style).

		if ( trimResult.startsWith( "You gain a" ) || trimResult.startsWith( "You gain some" ) )
			return;

		try
		{
			if ( logStream != null )
				logStream.println( "Parsing result: " + trimResult );

			processResult( AdventureResult.parseResult( trimResult ) );
		}
		catch ( Exception e )
		{
			logStream.println( e );
			e.printStackTrace( logStream );
		}
	}

	public void parseEffect( String result )
	{
		if ( logStream != null )
			logStream.println( "Parsing effect: " + result );

		StringTokenizer parsedEffect = new StringTokenizer( result, "()" );
		String parsedEffectName = parsedEffect.nextToken().trim();
		String parsedDuration = parsedEffect.hasMoreTokens() ? parsedEffect.nextToken() : "1";

		try
		{
			processResult( new AdventureResult( parsedEffectName, df.parse( parsedDuration ).intValue(), true ) );
		}
		catch ( Exception e )
		{
			logStream.println( e );
			e.printStackTrace( logStream );
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

	public void processResult( AdventureResult result )
	{	processResult( result, true );
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

	public void processResult( AdventureResult result, boolean shouldTally )
	{
		// This should not happen, but check just in case and
		// return if the result was null.

		if ( result == null )
			return;

		if ( logStream != null )
			logStream.println( "Processing result: " + result );

		String resultName = result.getName();

		// This should not happen, but check just in case and
		// return if the result name was null.

		if ( resultName == null )
			return;

		// Process the adventure result in this section; if
		// it's a status effect, then add it to the recent
		// effect list.  Otherwise, add it to the tally.

		if ( result.isStatusEffect() )
			AdventureResult.addResultToList( recentEffects, result );
		else if ( result.isItem() || resultName.equals( AdventureResult.SUBSTATS ) || resultName.equals( AdventureResult.MEAT ) )
		{
			if ( shouldTally )
				AdventureResult.addResultToList( tally, result );

			if ( result.isItem() && TradeableItemDatabase.isUsable( resultName ) )
			{
				AdventureResult.addResultToList( usableItems, result );
				if ( result.getCount( usableItems ) < 0 )
					usableItems.remove( usableItems.indexOf( result ) );
			}
			if ( result.isItem() && TradeableItemDatabase.getPriceByID( result.getItemID() ) != 0 )
			{
				AdventureResult.addResultToList( sellableItems, result );
				if ( result.getCount( sellableItems ) < 0 )
					sellableItems.remove( sellableItems.indexOf( result ) );
			}
		}

		KoLCharacter.processResult( result );

		if ( !shouldTally )
			return;

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		if ( resultName.equals( AdventureResult.SUBSTATS ) && tally.size() >= 2 )
		{
			fullStatGain[0] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMuscle() ) - initialStats[0];
			fullStatGain[1] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMysticality() ) - initialStats[1];
			fullStatGain[2] = KoLCharacter.calculateBasePoints( KoLCharacter.getTotalMoxie() ) - initialStats[2];

			if ( tally.size() > 2 )
				tally.set( 2, new AdventureResult( AdventureResult.FULLSTATS, fullStatGain ) );
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

		recentEffects.clear();
		FamiliarData.updateWeightModifier();
	}

	/**
	 * Returns the string form of the player ID associated
	 * with the given player name.
	 *
	 * @param	playerID	The ID of the player
	 * @return	The player's name if it has been seen, or null if it has not
	 *          yet appeared in the chat (not likely, but possible).
	 */

	public String getPlayerName( String playerID )
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

	public String getPlayerID( String playerName )
	{
		if ( playerName == null )
			return null;

		String playerID = (String) seenPlayerIDs.get( playerName.toLowerCase() );
		return playerID != null ? playerID : playerName.replaceAll( " ", "_" );
	}

	/**
	 * Registers the given player name and player ID with
	 * KoLmafia's player name tracker.
	 *
	 * @param	playerName	The name of the player
	 * @param	playerID	The player ID associated with this player
	 */

	public void registerPlayer( String playerName, String playerID )
	{
		if ( !seenPlayerIDs.containsKey( playerName.toLowerCase() ) )
		{
			seenPlayerIDs.put( playerName.toLowerCase(), playerID );
			seenPlayerNames.put( playerID, playerName );
		}
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
	 * Retrieves the usable items in the character's inventory
	 * @return	The character's usable items
	 */

	public SortedListModel getUsableItems()
	{	return usableItems;
	}

	/**
	 * Retrieves the sellable items in the character's inventory
	 * @return	The character's sellable items
	 */

	public SortedListModel getSellableItems()
	{	return sellableItems;
	}

	/**
	 * Returns the list of items which are available from the
	 * bounty hunter hunter today.
	 */

	public SortedListModel getBountyHunterItems()
	{	return hunterItems;
	}

	/**
	 * Retrieves the character's storage contents.
	 * @return	The character's items in storage
	 */

	public SortedListModel getStorage()
	{	return storage;
	}

	/**
	 * Returns whether or not the current user has a ten-leaf clover.
	 *
	 * @return	<code>true</code>
	 */

	public boolean isLuckyCharacter()
	{	return KoLCharacter.getInventory().contains( CLOVER );
	}

	/**
	 * Utility method called inbetween battles.  This method
	 * checks to see if the character's HP has dropped below
	 * the tolerance value, and autorecovers if it has (if
	 * the user has specified this in their settings).
	 */

	protected final void autoRecoverHP()
	{
		double autoRecover = Double.parseDouble( settings.getProperty( "hpAutoRecover" ) ) * (double) KoLCharacter.getMaximumHP();
		autoRecoverHP( (int) autoRecover );
	}

	/**
	 * Internal method to recover HP above to specified threshold.
	 * If the threshold is equal to or greater than your maximum
	 * hit points
	 */

	protected final void autoRecoverHP( int autoRecover )
	{
		if ( KoLCharacter.getCurrentHP() <= autoRecover )
		{
			try
			{
				int currentHP = -1;
				permitContinue = true;

				while ( permitContinue && KoLCharacter.getCurrentHP() <= autoRecover &&
					KoLCharacter.getCurrentHP() < KoLCharacter.getMaximumHP() && currentHP != KoLCharacter.getCurrentHP() )
				{
					currentHP = KoLCharacter.getCurrentHP();
					updateDisplay( DISABLED_STATE, "Executing HP auto-recovery script..." );

					String scriptPath = settings.getProperty( "hpRecoveryScript" ) ;
					File autoRecoveryScript = new File( scriptPath );

					if ( autoRecoveryScript.exists() )
					{
						disableMacro = true;
						(new KoLmafiaCLI( this, new FileInputStream( autoRecoveryScript ) )).listenForCommands();
					}
					else
					{
						updateDisplay( ERROR_STATE, "Could not find HP auto-recovery script." );
						permitContinue = false;
						disableMacro = false;
						return;
					}
				}

				if ( currentHP == KoLCharacter.getCurrentHP() )
				{
					updateDisplay( ERROR_STATE, "Auto-recovery script failed to restore HP." );
					permitContinue = false;
					disableMacro = false;
					return;
				}
			}
			catch ( Exception e )
			{
				updateDisplay( ERROR_STATE, "Could not find HP auto-recovery script." );
				permitContinue = false;
				disableMacro = false;
				return;
			}
		}

		disableMacro = false;
	}

	/**
	 * Returns the total number of mana restores currently
	 * available to the player.
	 */

	public int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );

		for ( int i = 0; i < MPRestoreItemList.size(); ++i )
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.get(i).toString() ) != -1 )
				restoreCount += MPRestoreItemList.get(i).getItem().getCount( KoLCharacter.getInventory() );

		return restoreCount;
	}

	/**
	 * Utility method called inbetween commands.  This method
	 * checks to see if the character's MP has dropped below
	 * the tolerance value, and autorecovers if it has (if
	 * the user has specified this in their settings).
	 */

	protected final void autoRecoverMP()
	{
		double mpNeeded = Double.parseDouble( settings.getProperty( "mpAutoRecover" ) ) * (double) KoLCharacter.getMaximumMP();
		recoverMP( (int) mpNeeded );
	}

	/**
	 * Utility method which restores the character's current
	 * mana points above the given value.
	 */

	public boolean recoverMP( int mpNeeded )
	{
		if ( KoLCharacter.getCurrentMP() >= mpNeeded )
			return true;

		int previousMP = -1;
		disableMacro = true;

		String mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );

		for ( int i = 0; i < MPRestoreItemList.size(); ++i )
		{
			if ( mpRestoreSetting.indexOf( MPRestoreItemList.get(i).toString() ) != -1 )
			{
				if ( MPRestoreItemList.get(i) == MPRestoreItemList.BEANBAG || MPRestoreItemList.get(i) == MPRestoreItemList.HOUSE )
				{
					while ( KoLCharacter.getAdventuresLeft() > 0 &&
						KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() && KoLCharacter.getCurrentMP() > previousMP )
					{
						previousMP = KoLCharacter.getCurrentMP();
 						MPRestoreItemList.get(i).recoverMP( mpNeeded );

 						if ( KoLCharacter.getCurrentMP() >= mpNeeded )
 						{
							disableMacro = false;
							permitContinue = true;
 							return true;
						}

						if ( KoLCharacter.getCurrentMP() == previousMP )
						{
							updateDisplay( ERROR_STATE, "Detected no MP change.  Refreshing status to verify..." );
							(new CharsheetRequest( this )).run();
						}
 					}
				}
				else
				{
					AdventureResult item = new AdventureResult( MPRestoreItemList.get(i).toString(), 0 );
 					while ( KoLCharacter.getInventory().contains( item ) &&
						KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() && KoLCharacter.getCurrentMP() > previousMP )
 					{
 						previousMP = KoLCharacter.getCurrentMP();
 						MPRestoreItemList.get(i).recoverMP( mpNeeded );

 						if ( KoLCharacter.getCurrentMP() >= mpNeeded )
 						{
							disableMacro = false;
							permitContinue = true;
							return true;
						}

						if ( KoLCharacter.getCurrentMP() == previousMP )
						{
							updateDisplay( ERROR_STATE, "Detected no MP change.  Refreshing status to verify..." );
							(new CharsheetRequest( this )).run();
						}
 					}
				}
			}
		}

		updateDisplay( ERROR_STATE, "Unable to acquire enough MP!" );
		disableMacro = false;
		permitContinue = false;
		return false;
	}

	/**
	 * Utility method used to process the results of any adventure
	 * in the Kingdom of Loathing.  This method searches for items,
	 * stat gains, and losses within the provided string.
	 *
	 * @param	results	The string containing the results of the adventure
	 */

	public void processResults( String results )
	{
		logStream.println( "Processing results..." );

		if ( results.indexOf( "gains a pound!</b>" ) != -1 )
			KoLCharacter.incrementFamilarWeight();

		String plainTextResult = results.replaceAll( "<.*?>", "\n" );
		StringTokenizer parsedResults = new StringTokenizer( plainTextResult, "\n" );
		String lastToken = null;

		Matcher damageMatcher = Pattern.compile( "you for ([\\d,]+) damage" ).matcher( plainTextResult );
		int lastDamageIndex = 0;

		while ( damageMatcher.find( lastDamageIndex ) )
		{
			lastDamageIndex = damageMatcher.end();
			parseResult( "You lose " + damageMatcher.group(1) + " hit points" );
		}

		damageMatcher = Pattern.compile( "You drop .*? ([\\d,]+) damage" ).matcher( plainTextResult );
		lastDamageIndex = 0;

		while ( damageMatcher.find( lastDamageIndex ) )
		{
			lastDamageIndex = damageMatcher.end();
			parseResult( "You lose " + damageMatcher.group(1) + " hit points" );
		}

		while ( parsedResults.hasMoreTokens() )
		{
			lastToken = parsedResults.nextToken();

			// Skip effect acquisition - it's followed by a boldface
			// which makes the parser think it's found an item.

			if ( lastToken.startsWith( "You acquire" ) )
			{
				if ( lastToken.indexOf( "effect" ) == -1 )
				{
					String acquisition = parsedResults.nextToken();

					try
					{
						if ( lastToken.indexOf( "an item" ) != -1 )
						{
							// If the item is in the standard adventure
							// result format (before Jick's change), then
							// make sure that the item is processed.

							processResult( AdventureResult.parseResult( acquisition ) );
						}
						else
						{
							// The name of the item follows the number
							// that appears after the first index.

							String countString = acquisition.split( " " )[0];
							String itemName = acquisition.substring( acquisition.indexOf( " " ) ).trim();

							boolean isNumeric = true;
							for ( int i = 0; isNumeric && i < countString.length(); ++i )
								isNumeric &= Character.isDigit( countString.charAt(i) );

							processResult( new AdventureResult( itemName, isNumeric ? df.parse( countString ).intValue() : 1 ) );
						}
					}
					catch ( Exception e )
					{
						// Because the number is always the first token
						// listed, this should not happen.
					}
				}
				else
				{
					String effect = parsedResults.nextToken();
					lastToken = parsedResults.nextToken();

					if ( lastToken.indexOf( "duration" ) == -1 )
						parseEffect( effect );
					else
					{
						String duration = lastToken.substring( 11, lastToken.length() - 11 ).trim();
						parseEffect( effect + " (" + duration + ")" );
					}
				}
			}
			else if ( (lastToken.startsWith( "You gain" ) || lastToken.startsWith( "You lose " )) )
				parseResult( lastToken.indexOf( "." ) == -1 ? lastToken : lastToken.substring( 0, lastToken.indexOf( "." ) ) );
		}
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
			this.permitContinue = true;
			boolean pulledOver = false;
			boolean shouldRefreshStatus;

			int currentEffectCount = KoLCharacter.getEffects().size();
			int iterationsRemaining = iterations;

			// If you're currently recording commands, be sure to
			// record the current command to the macro stream.

			if ( !this.disableMacro )
				macroStream.print( KoLmafiaCLI.deriveCommand( request, iterations ) );

			// Handle the special adventures, which include the
			// hermitage, trapper, bounty hunter, and gym.

			if ( request.toString().equals( "The Hermitage" ) )
				makeHermitRequest();
			else if ( request.toString().equals( "The 1337 Trapper" ) )
				makeTrapperRequest();
			else if ( request.toString().equals( "The Bounty Hunter" ) )
				makeHunterRequest();
			else if ( request.toString().startsWith( "Gym" ) )
				(new ClanGymRequest( this, Integer.parseInt( ((KoLAdventure)request).getAdventureID() ), iterations )).run();
			else
			{
				// Otherwise, you're handling a standard adventure.  Be
				// sure to check to see if you're allowed to continue
				// after drunkenness.

				if ( KoLCharacter.getInebriety() > 19 )
				{
					if ( request instanceof KoLAdventure && !((KoLAdventure)request).getZone().equals( "Camp" ) )
						permitContinue = confirmDrunkenRequest();
					pulledOver = true;
				}

				// Finally, check to see if you have the appropriate
				// amount of hit points and mana points to continue.

				if ( currentState != CANCELLED_STATE )
				{
					if ( request instanceof KoLAdventure && !request.toString().startsWith( "Camp" ) )
						autoRecoverHP();

					if ( (request instanceof KoLAdventure && !request.toString().startsWith( "Camp" )) || request instanceof UseSkillRequest )
						autoRecoverMP();
				}

				// Check to see if there are any end conditions.  If
				// there are conditions, be sure that they are checked
				// during the iterations.

				int remainingConditions = conditions.size();

				// If this is an adventure request, make sure that it
				// gets validated before running.

				if ( request instanceof KoLAdventure )
					AdventureDatabase.validateAdventure( (KoLAdventure) request );

				// Begin the adventuring process, or the request execution
				// process (whichever is applicable).

				for ( int i = 1; permitContinue && iterationsRemaining > 0; ++i )
				{
					// If the conditions existed and have been satisfied,
					// then you should stop.

					if ( conditions.size() < remainingConditions )
					{
						if ( conditions.size() == 0 || useDisjunction )
						{
							updateDisplay( ENABLED_STATE, "Conditions satisfied." );
							return;
						}
					}

					remainingConditions = conditions.size();

					// Otherwise, disable the display and update the user
					// and the current request number.  Different requests
					// have different displays.  They are handled here.

					if ( request instanceof KoLAdventure )
						updateDisplay( DISABLED_STATE, "Request " + i + " of " + iterations + " (" + request.toString() + ") in progress..." );

					else if ( request instanceof ConsumeItemRequest )
					{
						int consumptionType = ((ConsumeItemRequest)request).getConsumptionType();
						String useTypeAsString = (consumptionType == ConsumeItemRequest.CONSUME_EAT) ? "Eating" :
							(consumptionType == ConsumeItemRequest.CONSUME_DRINK) ? "Drinking" : "Using";

						if ( iterations == 1 )
							updateDisplay( DISABLED_STATE, useTypeAsString + " " + ((ConsumeItemRequest)request).getItemUsed().toString() + "..." );
						else
							updateDisplay( DISABLED_STATE, useTypeAsString + " " + ((ConsumeItemRequest)request).getItemUsed().getName() + " (" + i + " of " + iterations + ")..." );
					}

					request.run();
					applyRecentEffects();

					// Make sure you only decrement iterations if the
					// continue was permitted.  This resolves the issue
					// of incorrectly updating the client if something
					// occurred on the last iteration.

					if ( permitContinue )
					{
						--iterationsRemaining;

						if ( request instanceof KoLAdventure && !request.toString().startsWith( "Camp" ) )
							autoRecoverHP();

						if ( (request instanceof KoLAdventure && !request.toString().startsWith( "Camp" )) || request instanceof UseSkillRequest )
							autoRecoverMP();

						if ( request instanceof KoLAdventure && KoLCharacter.getInebriety() > 19 && !pulledOver )
						{
							permitContinue = confirmDrunkenRequest();
							pulledOver = true;
						}
					}
					else if ( (request instanceof KoLAdventure && !request.toString().startsWith( "Camp" )) || request instanceof UseSkillRequest )
					{
						if ( currentState != CANCELLED_STATE )
						{
							autoRecoverHP();
							autoRecoverMP();
						}

						if ( permitContinue )
						{
							if ( request instanceof KoLAdventure && KoLCharacter.getInebriety() > 19 && !pulledOver )
							{
								permitContinue = confirmDrunkenRequest();
								pulledOver = true;
							}

							if ( permitContinue )
								--iterationsRemaining;
						}
					}

					shouldRefreshStatus = currentEffectCount != KoLCharacter.getEffects().size();

					// If this is a KoLRequest request, make sure to process
					// any applicable adventure usage.

					if ( request instanceof KoLRequest )
						processResult( new AdventureResult( AdventureResult.ADV, 0 - ((KoLRequest)request).getAdventuresUsed() ) );

					shouldRefreshStatus |= currentEffectCount != KoLCharacter.getEffects().size();
					currentEffectCount = KoLCharacter.getEffects().size();

					// If it turns out that you need to refresh the player's
					// status, go ahead and refresh it.

					if ( shouldRefreshStatus )
						(new CharpaneRequest( this )).run();
				}

				// If you've completed the request, make sure to update
				// the display.

				if ( permitContinue && iterations > 0 && iterationsRemaining <= 0 &&
					!(request instanceof UseSkillRequest || request instanceof AutoSellRequest || request instanceof HeroDonationRequest) )
						updateDisplay( ENABLED_STATE, "Requests completed!" );
			}
		}
		catch ( RuntimeException e )
		{
			// In the event that an exception occurs during the
			// request processing, catch it here, print it to
			// the logger (whatever it may be), and notify the
			// user that an error was encountered.

			logStream.println( e );
			e.printStackTrace( logStream );
			updateDisplay( ERROR_STATE, "Unexpected error." );
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
	 * Confirms whether or not the user wants to make a drunken
	 * request.  This should be called before doing requests when
	 * the user is in an inebrieted state.
	 *
	 * @return	<code>true</code> if the user wishes to adventure drunk
	 */

	protected abstract boolean confirmDrunkenRequest();

	/**
	 * For requests that do not use the client's "makeRequest()"
	 * method, this method is used to reset the continue state.
	 */

	public void resetContinueState()
	{	this.permitContinue = true;
	}

	/**
	 * Cancels the user's current request.  Note that if there are
	 * no requests running, this method does nothing.
	 */

	public void cancelRequest()
	{	this.permitContinue = false;
	}

	/**
	 * Retrieves whether or not continuation of an adventure or request
	 * is permitted by the client, or by current circumstances in-game.
	 *
	 * @return	<code>true</code> if requests are allowed to continue
	 */

	public boolean permitsContinue()
	{	return permitContinue;
	}

	/**
	 * Initializes a stream for logging debugging information.  This
	 * method creates a <code>KoLmafia.log</code> file in the default
	 * data directory if one does not exist, or appends to the existing
	 * log.  This method should only be invoked if the user wishes to
	 * assist in beta testing because the output is VERY verbose.
	 */

	public void initializeLogStream()
	{
		// First, ensure that a log stream has not already been
		// initialized - this can be checked by observing what
		// class the current log stream is.

		if ( !(logStream instanceof NullStream) )
			return;

		try
		{
			File f = new File( DATA_DIRECTORY + "KoLmafia.log" );

			if ( !f.exists() )
				f.createNewFile();

			logStream = new LogStream( f );
		}
		catch ( IOException e )
		{
			// This should not happen, unless the user
			// security settings are too high to allow
			// programs to write output; therefore,
			// pretend for now that everything works.
		}
	}

	/**
	 * De-initializes the log stream.  This method should only
	 * be called when the user wishes to stop logging the session.
	 */

	public void deinitializeLogStream()
	{
		if ( logStream != null )
			logStream.close();
		logStream = new NullStream();
	}

	/**
	 * Retrieves the current settings for the current session.  Note
	 * that if this is invoked before initialization, this method
	 * will return the global settings.
	 *
	 * @return	The settings for the current session
	 */

	public KoLSettings getSettings()
	{	return settings;
	}

	/**
	 * Retrieves the stream currently used for logging debug output.
	 * @return	The stream used for debug output
	 */

	public PrintStream getLogStream()
	{	return logStream;
	}

	/**
	 * Returns the messaging client associated with the current
	 * session of KoLmafia.
	 *
	 * @return	The messaging client for the current session
	 */

	public KoLMessenger getMessenger()
	{	return loathingChat;
	}

	/**
	 * Initializes the chat buffer with the provided chat pane.
	 * Note that the chat refresher will also be initialized
	 * by calling this method; to stop the chat refresher, call
	 * the <code>deinitializeChat()</code> method.
	 */

	public void initializeChat()
	{
		if ( loathingChat == null )
		{
			loathingChat = new KoLMessenger( this );
			loathingChat.initialize();
		}
	}

	/**
	 * De-initializes the chat.  This closes any existing logging
	 * activity occurring within the chat and disables future
	 * chat refresher requests.  In order to re-initialize the
	 * chat, please call the <code>initializeChat()</code> method.
	 */

	public void deinitializeChat()
	{
		if ( loathingChat != null )
		{
			loathingChat.dispose();
			loathingChat = null;
		}
	}

	/**
	 * Initializes the macro recording stream.  This will only
	 * work if no macro streams are currently running.  If
	 * a call is made while a macro stream exists, this method
	 * does nothing.
	 *
	 * @param	filename	The name of the file to be created
	 */

	public void initializeMacroStream( String filename )
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
			// This should not happen, unless the user
			// security settings are too high to allow
			// programs to write output; therefore,
			// pretend for now that everything works.
		}
	}

	/**
	 * Retrieves the macro stream.
	 * @return	The macro stream associated with this client
	 */

	public PrintStream getMacroStream()
	{	return macroStream;
	}

	/**
	 * Deinitializes the macro stream.
	 */

	public void deinitializeMacroStream()
	{
		if ( macroStream != null )
			macroStream.close();
		macroStream = new NullStream();
	}

	/**
	 * Returns whether or not the client is current in a login state.
	 * While the client is in a login state, only login-related
	 * activities should be permitted.
	 */

	public boolean inLoginState()
	{	return isLoggingIn;
	}

	/**
	 * Utility method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public void addSaveState( String loginname, String password )
	{
		try
		{
			if ( !saveStateNames.contains( loginname ) )
				saveStateNames.add( loginname );

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

			settings.setProperty( "saveState." + loginname.toLowerCase(), (new BigInteger( encodedString.toString(), 36 )).toString( 10 ) );
			settings.saveSettings();
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// UTF-8 is a very generic encoding scheme; this
			// exception should never be thrown.  But if it
			// is, just ignore it for now.  Better exception
			// handling when it becomes necessary.
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

	private void storeSaveStates()
	{
		StringBuffer saveStateBuffer = new StringBuffer();
		Iterator nameIterator = saveStateNames.iterator();

		if ( nameIterator.hasNext() )
		{
			saveStateBuffer.append( nameIterator.next() );
			while ( nameIterator.hasNext() )
			{
				saveStateBuffer.append( "//" );
				saveStateBuffer.append( nameIterator.next() );
			}
			settings.setProperty( "saveState", saveStateBuffer.toString() );
		}
		else
			settings.setProperty( "saveState", "" );

		// Now, removing any passwords that were stored
		// which are no longer in the save state list

		String currentKey;
		Object [] settingsArray = settings.keySet().toArray();

		nameIterator = saveStateNames.iterator();
		List lowerCaseNames = new ArrayList();

		while ( nameIterator.hasNext() )
			lowerCaseNames.add( ((String)nameIterator.next()).toLowerCase() );

		for ( int i = 0; i < settingsArray.length; ++i )
		{
			currentKey = (String) settingsArray[i];
			if ( currentKey.startsWith( "saveState." ) && !lowerCaseNames.contains( currentKey.substring( 10 ) ) )
				settings.remove( currentKey );
		}

		settings.saveSettings();
	}

	/**
	 * Utility method used to decode a saved password.
	 * This should be called whenever a new password
	 * intends to be stored in the global file.
	 */

	public String getSaveState( String loginname )
	{
		try
		{
			Object [] settingKeys = settings.keySet().toArray();
			String password = null;
			String lowerCaseKey = "saveState." + loginname.toLowerCase();
			String currentKey;

			for ( int i = 0; i < settingKeys.length && password == null; ++i )
			{
				currentKey = (String) settingKeys[i];
				if ( currentKey.equals( lowerCaseKey ) )
					password = settings.getProperty( currentKey );
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
			// UTF-8 is a very generic encoding scheme; this
			// exception should never be thrown.  But if it
			// is, just ignore it for now.  Better exception
			// handling when it becomes necessary.

			return null;
		}
	}

	/**
	 * Retrieves the <code>ClanManager</code> used for managing data relating
	 * to this player's clan.
	 * @return	The <code>ClanManager</code> used for managing the clan
	 */

	public ClanManager getClanManager()
	{	return clanManager;
	}

	public SortedListModel getSessionTally()
	{	return tally;
	}

	public SortedListModel getConditions()
	{	return conditions;
	}

	public synchronized void executeTimeInRequest()
	{
		// If the client is permitted to continue,
		// then the session has already timed in.

		if ( permitContinue )
		{
			updateDisplay( ERROR_STATE, "No timeout detected." );
			return;
		}

		isLoggingIn = true;
		LoginRequest cachedLogin = loginRequest;

		boolean previouslyActive = BuffBotHome.isBuffBotActive();

		deinitialize();
		updateDisplay( DISABLED_STATE, "Timing in session..." );

		// Two quick login attempts to force
		// a timeout of the other session and
		// re-request another session.

		cachedLogin.run();

		if ( isLoggingIn )
			cachedLogin.run();

		// Wait 5 minutes inbetween each attempt
		// to re-login to Kingdom of Loathing,
		// because if the above two failed, that
		// means it's nightly maintenance.

		while ( isLoggingIn )
		{
			KoLRequest.delay( 300000 );
			cachedLogin.run();
		}

		// Refresh the character data after a
		// successful login.

		(new CharsheetRequest( KoLmafia.this )).run();

		BuffBotHome.setBuffBotActive( previouslyActive );
		updateDisplay( ENABLED_STATE, "Session timed in." );
	}

	public void robStrangeLeaflet()
	{
		// Use the static method provided in the Strange Leaflet
		StrangeLeaflet.robStrangeLeaflet();
	}

	public void faceNemesis()
	{
		// Use the static method provided to face your Nemesis
		Nemesis.faceNemesis();
	}

	public void completeEntryway()
	{
		// Use the static method provided in the sorceress
		// lair to complete the entryway process.

		SorceressLair.completeEntryway();
	}

	public void completeHedgeMaze()
	{
		// Use the static method provided in the sorceress
		// lair to pass through the hedge maze

		SorceressLair.completeHedgeMaze();
	}

	public void fightTowerGuardians()
	{
		// Use the static method provided in the sorceress
		// lair to fight all the tower guardians

		SorceressLair.fightTowerGuardians();
	}

	public void completeSorceressChamber()
	{
		// Use the static method provided in the sorceress
		// lair to complete the Sorceress's Chamber

		SorceressLair.completeSorceressChamber();
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

			AdventureDatabase.retrieveItem( requirementsArray[i] );
			missingCount = requirementsArray[i].getCount() - requirementsArray[i].getCount( KoLCharacter.getInventory() );

			if ( missingCount > 0 )
			{
				missingItems.add( requirementsArray[i].getInstance( missingCount ) );
				// Allow later requirements to be created.
				// We'll cancel the request again later.
				resetContinueState();
			}
		}

		// If there are any missing requirements
		// be sure to return false.

		if ( !missingItems.isEmpty() )
		{
			updateDisplay( ERROR_STATE, "Insufficient items to continue." );
			printList( missingItems );
			cancelRequest();
			return false;
		}

		updateDisplay( ENABLED_STATE, "Requirements met." );
		return true;
	}

	/**
	 * Utility method used to print a list to the given output
	 * stream.  If there's a need to print to the current output
	 * stream, simply pass the output stream to this method.
	 */

	protected abstract void printList( List printing );

	/**
	 * Utility method used to purchase the given number of items
	 * from the mall using the given purchase requests.
	 */

	public int makePurchases( List results, Object [] purchases, int maxPurchases )
	{
		MallPurchaseRequest currentRequest;
		permitContinue = true;

		for ( int i = 0; i < purchases.length && maxPurchases > 0 && permitContinue; ++i )
		{
			if ( purchases[i] instanceof MallPurchaseRequest )
			{
				currentRequest = (MallPurchaseRequest) purchases[i];

				// Keep track of how many of the item you had before
				// you run the purchase request

				AdventureResult oldResult = new AdventureResult( currentRequest.getItemName(), 0 );
				int oldResultIndex = KoLCharacter.getInventory().indexOf( oldResult );

				if ( oldResultIndex != -1 )
					oldResult = (AdventureResult) KoLCharacter.getInventory().get( oldResultIndex );

				currentRequest.setLimit( maxPurchases );
				currentRequest.run();

				// Calculate how many of the item you have now after
				// you run the purchase request

				int newResultIndex = KoLCharacter.getInventory().indexOf( oldResult );
				if ( newResultIndex != -1 )
				{
					AdventureResult newResult = (AdventureResult) KoLCharacter.getInventory().get( newResultIndex );
					maxPurchases -= newResult.getCount() - oldResult.getCount();
				}

				// Remove the purchase from the list!  Because you
				// have already made a purchase from the store

				if ( permitContinue )
					results.remove( purchases[i] );
			}
		}

		if ( maxPurchases == 0 )
			updateDisplay( ENABLED_STATE, "Purchases complete." );
		else
			updateDisplay( ERROR_STATE, "Purchases aborted.  Unexpected error." );

		// Return the number of purchases still remaining
		// after running the request.

		return maxPurchases;
	}
}
