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
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;

import java.util.Calendar;
import javax.swing.JEditorPane;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.DataUtilities;
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

public class KoLmafia implements UtilityConstants
{
	private static final String ADV_DBASE_FILE = "adventures.dat";

	private String loginname, password, sessionID, passwordHash;
	private KoLFrame activeFrame;
	private ChatBuffer loathingChat;

	private KoLSettings settings;
	private PrintStream logStream;
	private boolean permitContinue;

	private SortedListModel tally;

	/**
	 * The main method.  Currently, it instantiates a single instance
	 * of the <code>KoLmafia</code> client after setting the default
	 * look and feel of all <code>JFrame</code> objects to decorated.
	 */

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );
    	KoLmafia session = new KoLmafia();
	}

	/**
	 * Constructs a new <code>KoLmafia</code> object.  All data fields
	 * are initialized to their default values, the global settings
	 * are loaded from disk, and a <code>LoginFrame</code> is created
	 * to allow the user to login.
	 */

	public KoLmafia()
	{	deinitialize();
	}

	/**
	 * Returns the currently active frame in the <code>KoLmafia</code>
	 * session.  This frame is often used in display updates, and is
	 * therefore made accessible.
	 *
	 * @return	the currently active frame in this <code>KoLmafia</code> session
	 */

	public KoLFrame getActiveFrame()
	{	return activeFrame;
	}

	/**
	 * Initializes the <code>KoLmafia</code> session.  Called after
	 * the login has been confirmed to notify the client that the
	 * login was successful, the user-specific settings should be
	 * loaded, and the user can begin adventuring.
	 */

	public void initialize( String loginname, String passwordHash, String sessionID )
	{
		// Store the initialized variables
		this.loginname = loginname;
		this.passwordHash = passwordHash;
		this.sessionID = sessionID;

		// Begin by loading the user-specific settings.
		logStream.println( "Loading user settings for " + loginname + "..." );
		settings = new KoLSettings( loginname );

		logStream.println( "Settings successfully loaded.  Reading adventure database..." );
		BufferedReader advdata = DataUtilities.getReaderForSharedDataFile( ADV_DBASE_FILE );
		LockableListModel adventures = new LockableListModel();

		try
		{
			String line;
			while ( (line = advdata.readLine()) != null )
			{
				StringTokenizer strtok = new StringTokenizer( line, "\t" );
				if ( strtok.countTokens() == 3 )
					adventures.add( new KoLAdventure( this, strtok.nextToken(), strtok.nextToken(), strtok.nextToken() ) );
			}

			logStream.println( "Adventure database loaded successfully." );
		}
		catch ( IOException e )
		{
			// If an IOException is thrown, that means there was
			// a problem reading in the appropriate data file;
			// that means that no adventures can be done.  However,
			// the adventures data file should always be present.

			// The exception is strange enough that it won't be
			// handled at the current time.

			logStream.println( "I/O error in reading adventure database.  Continuing anyway." );
		}

		activeFrame.setVisible( false );
		activeFrame.dispose();
		activeFrame = null;

		tally = new SortedListModel();
		addToResultTally( new AdventureResult( AdventureResult.MEAT ) );
		addToResultTally( new AdventureResult( AdventureResult.SUBSTATS ) );
		addToResultTally( new AdventureResult( AdventureResult.DIVIDER ) );

		activeFrame = new AdventureFrame( this, adventures, tally );
		activeFrame.pack();  activeFrame.setVisible( true );
		activeFrame.requestFocus();
	}

	/**
	 * Deinitializes the <code>KoLmafia</code> session.  Called after
	 * the user has logged out.  Re-displays the <code>LoginFrame</code>
	 * and sets all the values to their defaults.
	 */

	public void deinitialize()
	{
		loginname = null;
		sessionID = null;
		passwordHash = null;
		permitContinue = false;

		settings = new KoLSettings();
		logStream = new NullStream();

		activeFrame = null;
		activeFrame = new LoginFrame( this );
		activeFrame.pack();  activeFrame.setVisible( true );
		activeFrame.requestFocus();
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

		if ( result.indexOf( "point" ) != -1 )
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
		int index = tally.indexOf( result );

		if ( index == -1 )
			tally.add( result );
		else
			tally.set( index, AdventureResult.add( result, (AdventureResult) tally.get( index ) ) );
	}

	/**
	 * Utility method used to notify the client that an adventure
	 * condition has been updated.  It is used to indicate whether
	 * or not an adventure was completed successfully, and whether
	 * or not another adventure is possible.
	 *
	 * @param	isComplete	<code>true</code> if the adventure was successfully completed
	 * @param	permitContinue	<code>true</code> if another adventure is possible
	 */

	public void updateAdventure( boolean isComplete, boolean permitContinue )
	{
		// For now, the isComplete variable has no meaning
		// because adventure usage is not being tracked.
		// However, permitContinue does - here, reset the
		// permitContinue to the value indicated here.

		this.permitContinue &= permitContinue;
	}

	/**
	 * Retrieves the login name for this <code>KoLmafia</code> session.
	 * @return	the login name of the current user
	 */

	public String getLoginName()
	{	return loginname;
	}

	/**
	 * Retrieves the session ID for this <code>KoLmafia</code> session.
	 * @return	the session ID of the current session
	 */

	public String getSessionID()
	{	return sessionID;
	}

	/**
	 * Retrieves the password hash for this <code>KoLmafia</code> session.
	 * @return	the password hash of the current session
	 */

	public String getPasswordHash()
	{	return passwordHash;
	}

	/**
	 * Returns whether or not the current user has a ten-leaf clover.
	 * Because inventory management is not yet implemented, this
	 * method always returns true.
	 *
	 * @return	<code>true</code>
	 */

	public boolean isLuckyCharacter()
	{	return true;
	}

	/**
	 * Makes the given request for the given number of iterations,
	 * or until continues are no longer possible, either through
	 * user cancellation or something occuring which prevents the
	 * requests from resuming.  Because this method does not create
	 * new threads, any GUI invoking this method should create a
	 * separate thread for calling it.
	 *
	 * @param	request	The request made by the user
	 * @param	iterations	The number of times the request should be repeated
	 */

	public void makeRequest( Runnable request, int iterations )
	{
		try
		{
			permitContinue = true;
			int iterationsRemaining = iterations;

			for ( int i = 1; permitContinue && iterationsRemaining > 0; ++i, --iterationsRemaining )
			{
				activeFrame.updateDisplay( KoLFrame.DISABLED_STATE, "Request " + i + " in progress..." );
				request.run();
			}

			permitContinue = false;
			if ( iterationsRemaining <= 0 )
				activeFrame.updateDisplay( KoLFrame.ENABLED_STATE, "Requests completed!" );
		}
		catch ( RuntimeException e )
		{
			// In the event that an exception occurs during the
			// request processing, catch it here, print it to
			// the logger (whatever it may be), and notify the
			// user that an error was encountered.

			logStream.println( e );
			activeFrame.updateDisplay( KoLFrame.ENABLED_STATE, "Unexpected error." );
		}
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
	 * Initializes the chat buffer with the provided chat pane.
	 * Note that the chat refresher will also be initialized
	 * by calling this method; to stop the chat refresher, call
	 * the <code>deinitializeChat()</code> method.
	 */

	public void initializeChat( JEditorPane chatDisplay )
	{
		loathingChat = new ChatBuffer( loginname + ": Started " +
			Calendar.getInstance().getTime().toString() );

		loathingChat.setChatDisplay( chatDisplay );
	}

	/**
	 * De-initializes the chat.  This closes any existing logging
	 * activity occurring within the chat and disables future
	 * chat refresher requests.  In order to re-initialize the
	 * chat, please call the <code>initializeChat()</code> method.
	 */

	public void deinitializeChat()
	{
		loathingChat.closeActiveLogFile();
		loathingChat = null;
	}

	/**
	 * Retrieves the chat buffer currently used for storing and
	 * saving the currently running chat.
	 *
	 * @return	The current chat buffer
	 */

	public ChatBuffer getChatBuffer()
	{	return loathingChat;
	}
}
