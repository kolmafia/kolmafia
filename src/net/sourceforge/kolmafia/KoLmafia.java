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
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public abstract class KoLmafia implements UtilityConstants
{
	protected static final String [] hermitItemNames = { "ten-leaf clover", "wooden figurine", "hot buttered roll", "banjo strings",
		"jabañero pepper", "fortune cookie", "golden twig", "ketchup", "catsup", "sweet rims", "dingy planks", "volleyball" };
	protected static final int [] hermitItemNumbers = { 24, 46, 47, 52, 55, 61, 66, 106, 107, 135, 140, 527 };

	protected boolean isLoggingIn;
	protected String password, sessionID, passwordHash;
	protected KoLCharacter characterData;
	protected KoLMessenger loathingChat;

	protected KoLSettings settings;
	protected PrintStream logStream;
	protected boolean permitContinue;

	protected List recentEffects;
	protected SortedListModel tally;
	protected LockableListModel inventory, closet, usableItems;

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafiaGUI</code>.
	 */

	public static void main( String [] args )
	{	KoLmafiaGUI.main( args );
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk.
	 */

	public KoLmafia()
	{
		this.isLoggingIn = true;
		deinitialize();
	}

	/**
	 * Updates the currently active display in the <code>KoLmafia</code>
	 * session.
	 */

	public abstract void updateDisplay( int state, String message );
	public abstract void requestFocus();

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String loginname, String sessionID, boolean getBreakfast )
	{
		// Store the initialized variables
		this.sessionID = sessionID;

		if ( !permitContinue )
		{
			this.sessionID = null;
			this.permitContinue = true;
			return;
		}

		(new PasswordHashRequest( this )).run();

		if ( !permitContinue )
		{
			this.sessionID = null;
			this.permitContinue = true;
			return;
		}

		characterData = new KoLCharacter( loginname );
		recentEffects = new ArrayList();

		if ( settings.getProperty( "skipCharacterData" ) == null )
		{
			(new CharsheetRequest( this )).run();
			(new CampgroundRequest( this )).run();
		}

		if ( !permitContinue )
		{
			this.sessionID = null;
			this.permitContinue = true;
			return;
		}

		inventory = characterData.getInventory();
		closet = characterData.getCloset();

		usableItems = new SortedListModel();

		if ( settings.getProperty( "skipInventory" ) == null )
			(new EquipmentRequest( this )).run();

		if ( !permitContinue )
		{
			this.sessionID = null;
			this.permitContinue = true;
			return;
		}

		if ( settings.getProperty( "skipFamiliarData" ) == null )
			(new FamiliarRequest( this )).run();

		// Begin by loading the user-specific settings.
		logStream.println( "Loading user settings for " + loginname + "..." );
		settings = new KoLSettings( loginname );

		tally = new SortedListModel();
		addToResultTally( new AdventureResult( AdventureResult.HP, characterData.getCurrentHP() ) );
		addToResultTally( new AdventureResult( AdventureResult.MP, characterData.getCurrentMP() ) );
		addToResultTally( new AdventureResult( AdventureResult.ADV, characterData.getAdventuresLeft() ) );
		addToResultTally( new AdventureResult( AdventureResult.DRUNK, characterData.getInebriety() ) );
		addToResultTally( new AdventureResult( AdventureResult.SPACER, 0 ) );
		addToResultTally( new AdventureResult( AdventureResult.MEAT ) );
		addToResultTally( new AdventureResult( AdventureResult.SUBSTATS ) );
		addToResultTally( new AdventureResult( AdventureResult.DIVIDER ) );

		applyRecentEffects();

		if ( getBreakfast )
		{
			updateDisplay( KoLFrame.NOCHANGE_STATE, "Retrieving breakfast..." );

			if ( characterData.hasToaster() )
				for ( int i = 0; i < 3 && permitContinue; ++i )
					(new CampgroundRequest( this, "toast" )).run();

			permitContinue = true;

			if ( characterData.hasArches() )
				(new CampgroundRequest( this, "arches" )).run();

			if ( characterData.canSummonNoodles() )
				(new UseSkillRequest( this, "Pastamastery", "", 3 )).run();
			if ( characterData.canSummonReagent() )
				(new UseSkillRequest( this, "Advanced Saucecrafting", "", 3 )).run();
		}

		this.isLoggingIn = false;
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.
	 */

	public void deinitialize()
	{
		sessionID = null;
		passwordHash = null;
		permitContinue = false;

		settings = new KoLSettings();
		deinitializeChat();
		deinitializeLogStream();
		this.permitContinue = true;
	}

	/**
	 * Utility method to parse an individual adventuring result.
	 * This method determines what the result actually was and
	 * adds it to the tally.  Note that at the current time, it
	 * will ignore anything with the word "points".
	 *
	 * @param	result	String to parse for the result
	 */

	public void parseResult( String result )
	{
		// Because of the simplified parsing, there's a chance that
		// the "gain" acquired wasn't a subpoint (in other words, it
		// includes the word "a" or "some"), which causes a NFE or
		// possibly a ParseException to be thrown.  Catch them and
		// do nothing (eventhough it's technically bad style).

		if ( result.startsWith( "You gain a" ) || result.startsWith( "You gain some" ) )
			return;

		try
		{
			logStream.println( "Parsing adventure result:\n\t" + result );
			addToResultTally( AdventureResult.parseResult( result ) );
		}
		catch ( Exception e )
		{
			logStream.println( e );
		}
	}

	/**
	 * Utility method used to add an adventure result to the
	 * tally directly.  This is used whenever the nature of the
	 * result is already known and no additional parsing is needed.
	 *
	 * @param	result	Result to add to the running tally of adventure results
	 */

	public void addToResultTally( AdventureResult result )
	{
		String resultName = result.getName();
		if ( result.isStatusEffect() )
			recentEffects.add( result );
		else
			AdventureResult.addResultToList( tally, result, characterData.getBaseMaxHP(), characterData.getBaseMaxMP() );

		if ( result.isItem() && TradeableItemDatabase.contains( resultName ) )
		{
			AdventureResult.addResultToList( inventory, result );
			if ( TradeableItemDatabase.isUsable( resultName ) )
				AdventureResult.addResultToList( usableItems, result );
		}

		if ( resultName.equals( AdventureResult.ADV ) )
			AdventureResult.reduceTally( characterData.getEffects(), result.getCount() );

		// Also update the character data's information related to
		// stats; for now, only drunkenness matters since the pane
		// won't be automatically updated during changes, but the
		// current drunkenness level is used for drunkenness tracking

		if ( tally.size() > 4 && resultName.equals( AdventureResult.DRUNK ) )
			characterData.setInebriety( ((AdventureResult)tally.get( 3 )).getCount() );
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

	public LockableListModel getInventory()
	{	return inventory;
	}

	/**
	 * Retrieves the character's closet.
	 * @return	The character's closet
	 */

	public LockableListModel getCloset()
	{	return closet;
	}

	/**
	 * Retrieves the usable items in the character's inventory
	 * @return	The character's usable items
	 */

	public LockableListModel getUsableItems()
	{	return usableItems;
	}

	/**
	 * Returns whether or not the current user has a ten-leaf clover.
	 * Because inventory management is not yet implemented, this
	 * method always returns true.
	 *
	 * @return	<code>true</code>
	 */

	public boolean isLuckyCharacter()
	{	return inventory.contains( new AdventureResult( "ten-leaf clover", 0 ) );
	}

	/**
	 * Makes the given request for the given number of iterations,
	 * or until continues are no longer possible, either through
	 * user cancellation or something occuring which prevents the
	 * requests from resuming.  Overriding classes should make use
	 * of the <code>permitContinue</code> variable to determine
	 * if one of the requests has suggested that the user stop.
	 *
	 * @param	request	The request made by the user
	 * @param	iterations	The number of times the request should be repeated
	 */

	public void makeRequest( Runnable request, int iterations )
	{
		try
		{
			this.permitContinue = true;
			int iterationsRemaining = iterations;

			if ( request.toString().equals( "The Hermitage" ) )
				makeHermitRequest( iterations );
			else if ( request.toString().startsWith( "Gym" ) )
				(new ClanGymRequest( this, Integer.parseInt( ((KoLAdventure)request).getAdventureID() ), iterations )).run();
			else
			{
				if ( request instanceof KoLAdventure && request.toString().indexOf( "Campground" ) == -1 && characterData.getInebriety() > 19 )
					permitContinue = confirmDrunkenRequest();

				for ( int i = 1; permitContinue && iterationsRemaining > 0; ++i )
				{
					updateDisplay( KoLFrame.DISABLED_STATE, "Request " + i + " in progress..." );
					request.run();
					applyRecentEffects();

					// Make sure you only decrement iterations if the
					// continue was permitted.  This resolves the issue
					// of incorrectly updating the client if something
					// occurred on the last iteration.

					if ( permitContinue )
						--iterationsRemaining;
				}

				if ( permitContinue && iterationsRemaining <= 0 )
					updateDisplay( KoLFrame.ENABLED_STATE, "Requests completed!" );
			}
		}
		catch ( RuntimeException e )
		{
			// In the event that an exception occurs during the
			// request processing, catch it here, print it to
			// the logger (whatever it may be), and notify the
			// user that an error was encountered.

			logStream.println( e );
			updateDisplay( KoLFrame.ENABLED_STATE, "Unexpected error." );
		}

		this.permitContinue = true;
	}

	/**
	 * Makes a request to the hermit, looking for the given number of
	 * items.  This method should prompt the user to determine which
	 * item to retrieve the hermit, if no default has been specified
	 * in the user settings.
	 *
	 * @param	itemCount	The number of items to request
	 */

	protected abstract void makeHermitRequest( int tradeCount );

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

		if ( logStream instanceof LogStream )
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
		loathingChat = new KoLMessenger( this );
		loathingChat.initialize();
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
	 * Returns whether or not the client is current in a login state.
	 * While the client is in a login state, only login-related
	 * activities should be permitted.
	 */

	public boolean inLoginState()
	{	return isLoggingIn;
	}
}
