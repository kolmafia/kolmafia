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
	protected static final String [] hermitItemNames = { "ten-leaf clover", "wooden figurine", "hot buttered roll", "banjo strings",
		"jabañero pepper", "fortune cookie", "golden twig", "ketchup", "catsup", "sweet rims", "dingy planks", "volleyball" };
	protected static final int [] hermitItemNumbers = { 24, 46, 47, 52, 55, 61, 66, 106, 107, 135, 140, 527 };

	protected static final String [] trapperItemNames = { "yak skin", "penguin skin", "hippopotamus skin" };
	protected static final int [] trapperItemNumbers = { 394, 393, 395 };

	protected boolean isLoggingIn;
	protected LoginRequest loginRequest;

	protected String password, sessionID, passwordHash;
	protected KoLCharacter characterData;
	protected KoLMessenger loathingChat;
	protected KoLMailManager loathingMail;

	private boolean disableMacro;
	protected KoLSettings settings;
	protected PrintStream logStream;
	protected PrintStream macroStream;
	protected boolean permitContinue;

	protected int [] initialStats;
	protected int [] fullStatGain;

	protected BuffBotHome buffBotHome;
	protected BuffBotManager buffBotManager;
	protected MPRestoreItemList mpRestoreItemList;
	protected CakeArenaManager cakeArenaManager;
	protected StoreManager storeManager;
	protected ClanManager clanManager;

	protected SortedListModel saveStateNames;
	protected List recentEffects;
	private LimitedSizeChatBuffer commandBuffer;

	private TreeMap seenPlayerIDs;
	private TreeMap seenPlayerNames;

	protected LockableListModel adventureList;
	protected SortedListModel tally, conditions;
	protected SortedListModel inventory, closet, usableItems, hunterItems, collection, storage;

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

		this.initialStats = new int[3];
		this.fullStatGain = new int[3];

		this.settings = new KoLSettings();
		this.saveStateNames = new SortedListModel();

		String [] currentNames = settings.getProperty( "saveState" ).split( "//" );
		for ( int i = 0; i < currentNames.length; ++i )
			saveStateNames.add( currentNames[i] );

		// This line is added to clear out data from previous
		// releases of KoLmafia - the extra disk access does
		// affect performance, but not significantly.

		storeSaveStates();
		deinitialize();

		seenPlayerIDs = new TreeMap();
		seenPlayerNames = new TreeMap();
		adventureList = new LockableListModel();
		commandBuffer = null;
	}

	public boolean isEnabled()
	{	return true;
	}

	public void setEnabled( boolean isEnabled )
	{
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public synchronized void updateDisplay( int state, String message )
	{
		if ( commandBuffer != null )
		{
			StringBuffer colorBuffer = new StringBuffer();
			if ( state == ERROR_STATE )
				colorBuffer.append( "<font color=red>" );
			else
				colorBuffer.append( "<font color=black>" );

			colorBuffer.append( message );
			colorBuffer.append( "</font><br>" );
			colorBuffer.append( System.getProperty( "line.separator" ) );

			commandBuffer.append( colorBuffer.toString() );
		}
	}

	public void setCommandBuffer( LimitedSizeChatBuffer commandBuffer )
	{	this.commandBuffer = commandBuffer;
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

		// Begin by loading the user-specific settings.
		this.settings = new KoLSettings( loginname );

		if ( this.characterData == null )
		{
			this.characterData = new KoLCharacter( loginname );
			this.mpRestoreItemList = new MPRestoreItemList( this );
			FamiliarData.setOwner( this.characterData );

			this.inventory = characterData.getInventory();
			this.usableItems = new SortedListModel();
			this.hunterItems = new SortedListModel();
			this.storage = new SortedListModel();
			this.collection = characterData.getCollection();
			this.closet = characterData.getCloset();
			this.recentEffects = new ArrayList();

			this.tally = new SortedListModel();
			this.conditions = new SortedListModel();

			resetSessionTally();
		}

		(new PasswordHashRequest( this )).run();

		adventureList.clear();
		adventureList.addAll( AdventureDatabase.getAsLockableListModel( this ) );

		if ( loginRequest != null )
			return;

		// Remove the password data; it doesn't need to be stored
		// in every single .kcs file.

		saveStateNames.clear();
		storeSaveStates();

		// Now!  Check to make sure the user didn't cancel
		// during any of the actual loading of elements

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		// Get current Moon Phases
		(new MoonPhaseRequest( this )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		if ( !isQuickLogin && settings.getProperty( "skipFamiliars" ).equals( "false" ) )
			(new FamiliarRequest( this )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		(new CharsheetRequest( this )).run();
		registerPlayer( loginname, String.valueOf( characterData.getUserID() ) );

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		(new CampgroundRequest( this )).run();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		if ( !isQuickLogin && settings.getProperty( "skipInventory" ).equals( "false" ) )
			(new EquipmentRequest( this )).run();

		resetSessionTally();
		applyRecentEffects();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		if ( getBreakfast )
			getBreakfast();

		if ( !permitContinue )
		{
			deinitialize();
			return;
		}

		this.isLoggingIn = false;
		this.loathingMail = new KoLMailManager( this );
		this.cakeArenaManager = new CakeArenaManager( this );
		this.storeManager = new StoreManager( this );
		this.clanManager = new ClanManager( this );
		this.permitContinue = true;
	}

	/**
	 * Utility method used to notify the client that it should attempt
	 * to retrieve breakfast.
	 */

	public void getBreakfast()
	{
		updateDisplay( DISABLED_STATE, "Retrieving breakfast..." );

		if ( characterData.hasToaster() )
			for ( int i = 0; i < 3 && permitContinue; ++i )
				(new CampgroundRequest( this, "toast" )).run();

		permitContinue = true;

		if ( characterData.hasArches() )
			(new CampgroundRequest( this, "arches" )).run();

		permitContinue = true;

		if ( characterData.canSummonNoodles() )
			(new UseSkillRequest( this, "Pastamastery", "", 3 )).run();

		permitContinue = true;

		if ( characterData.canSummonReagent() )
			(new UseSkillRequest( this, "Advanced Saucecrafting", "", 3 )).run();

		if ( characterData.canSummonShore() )
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

		deinitializeChat();
		setBuffBotActive( false );
		deinitializeLogStream();
		deinitializeMacroStream();
		permitContinue = false;
	}

	/**
	 * Used to reset the session tally to its original values.
	 */

	public void resetSessionTally()
	{
		tally.clear();

		initialStats[0] = KoLCharacter.calculateBasePoints( characterData.getTotalMuscle() );
		initialStats[1] = KoLCharacter.calculateBasePoints( characterData.getTotalMysticality() );
		initialStats[2] = KoLCharacter.calculateBasePoints( characterData.getTotalMoxie() );

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
	 * Utility method used to add an adventure result to the
	 * tally directly.  This is used whenever the nature of the
	 * result is already known and no additional parsing is needed.
	 *
	 * @param	result	Result to add to the running tally of adventure results
	 */

	public void processResult( AdventureResult result )
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
			AdventureResult.addResultToList( tally, result );
			if ( result.isItem() && TradeableItemDatabase.isUsable( resultName ) )
				AdventureResult.addResultToList( usableItems, result );
		}

		if ( characterData != null )
			characterData.processResult( result );

		// Now, if it's an actual stat gain, be sure to update the
		// list to reflect the current value of stats so far.

		if ( resultName.equals( AdventureResult.SUBSTATS ) && tally.size() >= 2 )
		{
			fullStatGain[0] = KoLCharacter.calculateBasePoints( characterData.getTotalMuscle() ) - initialStats[0];
			fullStatGain[1] = KoLCharacter.calculateBasePoints( characterData.getTotalMysticality() ) - initialStats[1];
			fullStatGain[2] = KoLCharacter.calculateBasePoints( characterData.getTotalMoxie() ) - initialStats[2];

			if ( tally.size() > 2 )
				tally.set( 2, new AdventureResult( AdventureResult.FULLSTATS, fullStatGain ) );
			else
				tally.add( new AdventureResult( AdventureResult.FULLSTATS, fullStatGain ) );
		}

		// Process the adventure result through the conditions
		// list, removing it if the condition is satisfied.

		if ( result.isItem() )
		{
			int conditionsIndex = conditions.indexOf( result );
			if ( conditionsIndex != -1 )
			{
				if ( result.getCount( conditions ) <= result.getCount() )
					conditions.remove( conditionsIndex );
				else
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
			AdventureResult.addResultToList( characterData.getEffects(), (AdventureResult) recentEffects.get(j) );

		recentEffects.clear();
		FamiliarData.updateWeightModifier();
	}

	/**
	 * Retrieves the character data for the character associated with the current
	 * gaming session.
	 *
	 * @return	The character data for this character
	 */

	public KoLCharacter getCharacterData()
	{	return characterData;
	}

	/**
	 * Retrieves the user ID for the character of the current gaming session.
	 * @return	The user ID of the current user
	 */

	public int getUserID()
	{	return characterData.getUserID();
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
	 * Retrieves the login name for this <code>KoLmafia</code> session.
	 * @return	The login name of the current user
	 */

	public String getLoginName()
	{	return (characterData == null) ? null : characterData.getUsername();
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
	 * Retrieves the character's inventory.
	 * @return	The character's inventory
	 */

	public SortedListModel getInventory()
	{	return inventory;
	}

	/**
	 * Retrieves the character's closet.
	 * @return	The character's closet
	 */

	public SortedListModel getCloset()
	{	return closet;
	}

	/**
	 * Retrieves the character's collection.
	 * @return	The character's collection
	 */

	public SortedListModel getCollection()
	{	return collection;
	}

	/**
	 * Retrieves the usable items in the character's inventory
	 * @return	The character's usable items
	 */

	public SortedListModel getUsableItems()
	{	return usableItems;
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
	 * Because inventory management is not yet implemented, this
	 * method always returns true.
	 *
	 * @return	<code>true</code>
	 */

	public boolean isLuckyCharacter()
	{	return inventory != null && inventory.contains( new AdventureResult( "ten-leaf clover", 0 ) );
	}

	/**
	 * Utility method called inbetween battles.  This method
	 * checks to see if the character's HP has dropped below
	 * the tolerance value, and autorecovers if it has (if
	 * the user has specified this in their settings).
	 */

	private void autoRecoverHP()
	{
		disableMacro = true;
		double autoRecover = Double.parseDouble( settings.getProperty( "hpAutoRecover" ) ) * (double) characterData.getMaximumHP();

		if ( (double) characterData.getCurrentHP() <= autoRecover )
		{
			try
			{
				int currentHP = -1;
				permitContinue = true;

				while ( permitContinue && characterData.getCurrentHP() <= autoRecover && currentHP != characterData.getCurrentHP() )
				{
					currentHP = characterData.getCurrentHP();
					updateDisplay( DISABLED_STATE, "Executing HP auto-recovery script..." );

					String scriptPath = settings.getProperty( "hpRecoveryScript" ) ;
					File autoRecoveryScript = new File( scriptPath );

					if ( autoRecoveryScript.exists() )
						(new KoLmafiaCLI( this, new FileInputStream( autoRecoveryScript ) )).listenForCommands();
					else
					{
						updateDisplay( ERROR_STATE, "Could not find HP auto-recovery script." );
						permitContinue = false;
						disableMacro = false;
						return;
					}
				}

				if ( currentHP == characterData.getCurrentHP() )
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
	 * Returns the list of mana restores being maintained
	 * by the current client.
	 */

	public MPRestoreItemList getMPRestoreItemList()
	{
		return mpRestoreItemList;
	}

	/**
	 * Returns the total number of mana restores currently
	 * available to the player.
	 */

	public int getRestoreCount()
	{
		int restoreCount = 0;
		String mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );

		for ( int i = 0; i < mpRestoreItemList.size(); ++i )
		{
			MPRestoreItemList.MPRestoreItem restorer = (MPRestoreItemList.MPRestoreItem) mpRestoreItemList.get(i);
			String itemName = restorer.toString();

			if ( mpRestoreSetting.indexOf( itemName ) != -1 )
				restoreCount += restorer.getItem().getCount( inventory );
		}

		return restoreCount;
	}

	/**
	 * Utility method called inbetween commands.  This method
	 * checks to see if the character's MP has dropped below
	 * the tolerance value, and autorecovers if it has (if
	 * the user has specified this in their settings).
	 */

	private void autoRecoverMP()
	{
		double mpNeeded = Double.parseDouble( settings.getProperty( "mpAutoRecover" ) ) * (double) characterData.getMaximumMP();

		if ( permitContinue )
			permitContinue = recoverMP( (int) mpNeeded );
	}

	/**
	 * Utility method which restores the character's current
	 * mana points to the given value.
	 */

	public boolean recoverMP( int mpNeeded )
	{
		if ( characterData.getCurrentMP() >= mpNeeded )
			return true;

		int previousMP = -1;
		String mpRestoreSetting = settings.getProperty( "buffBotMPRestore" );

		for ( int i = 0; i < mpRestoreItemList.size(); ++i )
		{
			MPRestoreItemList.MPRestoreItem restorer = (MPRestoreItemList.MPRestoreItem) mpRestoreItemList.get(i);
			String itemName = restorer.toString();

			if ( mpRestoreSetting.indexOf( itemName ) != -1 )
			{
				if ( itemName.equals( mpRestoreItemList.BEANBAG.toString() ) )
				{
					while ( characterData.getAdventuresLeft() > 0 && characterData.getCurrentMP() > previousMP )
					{
						previousMP = characterData.getCurrentMP();
 						restorer.recoverMP( mpNeeded );

 						if ( characterData.getCurrentMP() >= mpNeeded )
 							return true;

						if ( characterData.getCurrentMP() == previousMP )
						{
							updateDisplay( ERROR_STATE, "Detected no MP change.  Refreshing status to verify..." );
							(new CharsheetRequest( this )).run();
						}
 					}
				}
				else
				{
					AdventureResult item = new AdventureResult( itemName, 0 );
 					while ( inventory.contains( item ) && characterData.getCurrentMP() > previousMP )
 					{
 						previousMP = characterData.getCurrentMP();
 						restorer.recoverMP( mpNeeded );

 						if ( characterData.getCurrentMP() >= mpNeeded )
 							return true;

						if ( characterData.getCurrentMP() == previousMP )
						{
							updateDisplay( ERROR_STATE, "Detected no MP change.  Refreshing status to verify..." );
							(new CharsheetRequest( this )).run();
						}
 					}
				}
			}
		}

		updateDisplay( ERROR_STATE, "Unable to acquire enough MP!" );
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
			characterData.incrementFamilarWeight();

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
					parseResult( parsedResults.nextToken() );
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

				if ( characterData.getInebriety() > 19 )
				{
					if ( request instanceof KoLAdventure && !request.toString().startsWith( "Camp" ) )
						permitContinue = confirmDrunkenRequest();
					pulledOver = true;
				}

				// Finally, check to see if you have the appropriate
				// amount of hit points and mana points to continue.

				if ( request instanceof KoLAdventure && !request.toString().startsWith( "Camp" ) )
					autoRecoverHP();

				if ( (request instanceof KoLAdventure && !request.toString().startsWith( "Camp" )) || request instanceof UseSkillRequest )
					autoRecoverMP();

				// Check to see if there are any end conditions.  If
				// there are conditions, be sure that they are checked
				// during the iterations.

				boolean hasConditions = !conditions.isEmpty();

				// Begin the adventuring process, or the request execution
				// process (whichever is applicable).

				for ( int i = 1; permitContinue && iterationsRemaining > 0; ++i )
				{
					// Allow people to add conditions mid-request.

					hasConditions |= !conditions.isEmpty();

					// If the conditions existed and have been satisfied,
					// then you should stop.

					if ( hasConditions && conditions.isEmpty() )
					{
						updateDisplay( ENABLED_STATE, "Conditions satisfied." );
						return;
					}

					// Otherwise, disable the display and update the user
					// and the current request number.  Different requests
					// have different displays.  They are handled here.

					if ( request instanceof KoLAdventure || request instanceof HeroDonationRequest )
						updateDisplay( DISABLED_STATE, "Request " + i + " (" + request.toString() + ") in progress..." );

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

						if ( request instanceof KoLAdventure && characterData.getInebriety() > 19 && !pulledOver )
						{
							permitContinue = confirmDrunkenRequest();
							pulledOver = true;
						}
					}
					else if ( (request instanceof KoLAdventure && !request.toString().startsWith( "Camp" )) || request instanceof UseSkillRequest )
					{
						autoRecoverHP();
						autoRecoverMP();

						if ( permitContinue )
						{
							if ( request instanceof KoLAdventure && characterData.getInebriety() > 19 && !pulledOver )
							{
								permitContinue = confirmDrunkenRequest();
								pulledOver = true;
							}

							if ( permitContinue )
								--iterationsRemaining;
						}
					}
				}

				if ( permitContinue && iterations > 0 && iterationsRemaining <= 0 && !(request instanceof UseSkillRequest || request instanceof AutoSellRequest) )
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
	 * Makes a request which attempts to remove the given effect.
	 * This method should prompt the user to determine which effect
	 * the player would like to remove.
	 */

	protected abstract void makeUneffectRequest();

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit.
	 */

	protected abstract void makeHermitRequest();

	/**
	 * Makes a request to the trapper, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the trapper.
	 */

	protected abstract void makeTrapperRequest();

	/**
	 * Makes a request to the hunter, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hunter.
	 */

	protected abstract void makeHunterRequest();

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
	 * Returns the mailing client associated with the current
	 * session of KoLmafia.
	 *
	 * @return	The mailing client for the current session
	 */

	public KoLMailManager getMailManager()
	{	return isBuffBotActive() ? buffBotManager : loathingMail;
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
	 * Utility method used to initialize the buffbot and the
	 * buffbot logs.
	 */

	public void initializeBuffBot()
	{
		if ( buffBotHome == null )
			buffBotHome = new BuffBotHome(this);
	}

	/**
	 * Utility method used to deinitialize the buffbot and the
	 * buffbot logs.
	 */

	public void deinitializeBuffBot()
	{
		if ( buffBotHome != null )
		{
			buffBotHome.deinitialize();
			buffBotHome = null;
		}
	}

	/**
	 * Utility method used to retrieve the actual logger used
	 * for logging information related to the buffbut.
	 */

	public BuffBotHome getBuffBotLog()
	{	return buffBotHome;
	}

	/**
	 * Returns whether or not the buffbot is active.
	 * @return	<code>true</code> if the buffbot is active
	 */

	public boolean isBuffBotActive()
	{	return (buffBotHome == null) ? false : buffBotHome.isBuffBotActive();
	}

	/**
	 * Sets the active state of the buffbot.
	 * @param	isActive	The active state of the buffbot
	 */

	public void setBuffBotActive(boolean isActive)
	{	if (buffBotHome != null) buffBotHome.setBuffBotActive(isActive);
	}

	/**
	 * Sets the <code>BuffBotManager</code> used for managing the buffbot.
	 * @param	buffBotManager	The <code>BuffBotManager</code> to be used
	 */

	public void setBuffBotManager( BuffBotManager buffBotManager )
	{	this.buffBotManager = buffBotManager;
	}

	/**
	 * Retrieves the <code>BuffBotManager</code> used for managing the buffbot.
	 * @return	The <code>BuffBotManager</code> used for managing the buffbot
	 */

	public BuffBotManager getBuffBotManager()
	{	return buffBotManager;
	}

	/**
	 * Retrieves the <code>ClanManager</code> used for managing data relating
	 * to this player's clan.
	 * @return	The <code>ClanManager</code> used for managing the clan
	 */

	public ClanManager getClanManager()
	{	return clanManager;
	}

	/**
	 * Retrieves the <code>CakeArenaManager</code> used for managing requests
	 * to the cake-shaped arena.
	 */

	public CakeArenaManager getCakeArenaManager()
	{	return cakeArenaManager;
	}

	/**
	 * Retrieves the <code>StoreManager</code> used for managing data relating
	 * to the player's store.
	 */

	public StoreManager getStoreManager()
	{	return storeManager;
	}

	public LockableListModel getAdventureList()
	{	return adventureList;
	}

	public SortedListModel getSessionTally()
	{	return tally;
	}

	public SortedListModel getConditions()
	{	return conditions;
	}

	public void executeTimeInRequest()
	{
		if ( !isLoggingIn )
		{
			isLoggingIn = true;
			LoginRequest cachedLogin = loginRequest;

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

			// If the buffbot was active before, then
			// initialize it again.

			if ( buffBotManager != null )
			{
				initializeBuffBot();
				setBuffBotActive( true );
				buffBotManager.runBuffBot( Integer.MAX_VALUE );
			}

			updateDisplay( ENABLED_STATE, "Session timed in." );
		}
	}

	public void completeEntryway()
	{
		// Use the static method provided in the sorceress
		// lair to complete the entryway process.

		SorceressLair.setClient( this );
		SorceressLair.completeEntryway();
	}

	public void completeHedgeMaze()
	{
		// Use the static method provided in the sorceress
		// lair to complete the entryway process.

		SorceressLair.setClient( this );
		SorceressLair.completeHedgeMaze();
	}
}
