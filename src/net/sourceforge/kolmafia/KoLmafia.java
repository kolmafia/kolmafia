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
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

public class KoLmafia implements UtilityConstants
{
	private static final String ADV_DBASE_FILE = "adventures.dat";

	private String loginname, password, sessionID, passwordHash;
	private KoLFrame activeFrame;

	private KoLSettings settings;
	private PrintStream logStream;
	private boolean isLogging;
	private boolean permitContinue;

	private SortedListModel tally;

	public static void main( String [] args )
	{
		javax.swing.JFrame.setDefaultLookAndFeelDecorated( true );
    	KoLmafia session = new KoLmafia();
	}

	public KoLmafia()
	{
		loginname = null;
		sessionID = null;
		passwordHash = null;
		permitContinue = false;

		activeFrame = new LoginFrame( this );
		activeFrame.pack();  activeFrame.setVisible( true );
		activeFrame.requestFocus();

		settings = new KoLSettings();
		logStream = System.out;
		isLogging = false;
	}

	public KoLFrame getActiveFrame()
	{	return activeFrame;
	}

	public void initialize()
	{
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
		}
		catch ( IOException e )
		{
			// If an IOException is thrown, that means there was
			// a problem reading in the appropriate data file;
			// that means that no adventures can be done.  However,
			// the adventures data file should always be present.

			// The exception is strange enough that it won't be
			// handled at the current time.
		}

		activeFrame.setVisible( false );
		activeFrame.dispose();
		activeFrame = null;

		initializeTally();

		activeFrame = new AdventureFrame( this, adventures, tally );
		activeFrame.pack();  activeFrame.setVisible( true );
		activeFrame.requestFocus();
	}

	private void initializeTally()
	{
		tally = new SortedListModel();
		addToResultTally( new AdventureResult( AdventureResult.MEAT ) );
		addToResultTally( new AdventureResult( AdventureResult.SUBSTATS ) );
		addToResultTally( new AdventureResult( AdventureResult.DIVIDER ) );
	}

	private void resetTally()
	{
		// When reseting the tally, first remove all the items from
		// the list (which should appear after the stats, if the
		// tally is sorted) and then clear the stat gains.

		while ( tally.size() > 3 )
			tally.remove( 3 );

		for ( int i = 0; i < 3; ++i )
		{
			// Because the list won't update itself unless an
			// event is fired, tally also resets itself in order
			// to force the list model to fire the appropritate
			// change event.

			((AdventureResult)tally.get(i)).clear();
			tally.set( i, tally.get(i) );
		}
	}

	public void parseResult( String result )
	{
		// Because of the simplified parsing, there's a chance that
		// the "gain" acquired wasn't a subpoint (in other words, it
		// includes the word "a" or "some"), which causes a NFE or
		// possibly a ParseException to be thrown.  Catch them and
		// do nothing (eventhough it's technically bad style).

		try
		{
			addToResultTally( AdventureResult.parseResult( result ) );
		}
		catch ( Exception e )
		{
		}
	}

	public void addToResultTally( AdventureResult result )
	{
		int index = tally.indexOf( result );

		if ( index == -1 )
			tally.add( result );
		else
			tally.set( index, AdventureResult.add( result, (AdventureResult) tally.get( index ) ) );
	}

	public void updateAdventure( boolean isComplete, boolean permitContinue )
	{
		// For now, the isComplete variable has no meaning
		// because adventure usage is not being tracked.
		// However, permitContinue does - here, reset the
		// permitContinue to the value indicated here.

		this.permitContinue &= permitContinue;
	}

	public void setLoginName( String loginname )
	{	this.loginname = loginname;
	}

	public String getLoginName()
	{	return loginname;
	}

	public void setPassword( String password )
	{	this.password = password;
	}

	public String getPassword()
	{	return password;
	}

	public void setSessionID( String sessionID )
	{	this.sessionID = sessionID;
	}

	public String getSessionID()
	{	return sessionID;
	}

	public void setPasswordHash( String passwordHash )
	{	this.passwordHash = passwordHash;
	}

	public String getPasswordHash()
	{	return passwordHash;
	}

	public void makeRequest( Runnable request, int iterations )
	{
		try
		{
			permitContinue = true;
			int iterationsRemaining = iterations;

			for ( int i = 0; permitContinue && iterationsRemaining > 0; ++i, --iterationsRemaining )
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

	public void cancelRequest()
	{	this.permitContinue = false;
	}

	public boolean permitsContinue()
	{	return permitContinue;
	}

	public void initializeLogStream()
	{
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

	public KoLSettings getSettings()
	{	return settings;
	}

	public PrintStream getLogStream()
	{	return logStream;
	}

	public boolean isLogging()
	{	return isLogging;
	}
}
