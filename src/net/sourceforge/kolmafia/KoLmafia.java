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

import java.io.IOException;
import java.io.BufferedReader;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class KoLmafia
{
	private static final String ADV_DBASE_FILE = "adventures.dat";

	private String loginname, password, sessionID, passwordHash;
	private KoLFrame activeFrame;

	private Runnable currentRequest;
	private boolean permitContinue;
	private int currentIteration, iterationsRemaining;

	private LockableListModel tally;

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

		this.iterationsRemaining = 0;
		this.permitContinue = false;

		activeFrame = new LoginFrame( this );
		activeFrame.pack();  activeFrame.setVisible( true );
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
	}

	private void initializeTally()
	{
		tally = new SortedListModel();
		addToTally( new AdventureResult( AdventureResult.MEAT ) );
		addToTally( new AdventureResult( AdventureResult.MUS ) );
		addToTally( new AdventureResult( AdventureResult.MYS ) );
		addToTally( new AdventureResult( AdventureResult.MOX ) );
		addToTally( new AdventureResult( AdventureResult.DIVIDER ) );
	}

	public void acquireItem( String itemname )
	{
		// Because of the simplified parsing, there's a chance that
		// the "item" acquired was a stat point (which should not
		// be added at all).

		if ( itemname.endsWith( "point!" ) )
			return;

		StringTokenizer strtok = new StringTokenizer( itemname, "()" );
		addToTally( new AdventureResult( strtok.nextToken().trim(),
			strtok.hasMoreTokens() ? Integer.parseInt( strtok.nextToken() ) : 1 ) );
	}

	public void modifyStat( int increase, String statname )
	{
		if ( statname.equals( AdventureResult.MEAT ) )
			addToTally( new AdventureResult( AdventureResult.MEAT, increase ) );

		else if ( AdventureResult.MUS_SUBSTAT.contains( statname ) )
			addToTally( new AdventureResult( AdventureResult.MUS, increase ) );

		else if ( AdventureResult.MYS_SUBSTAT.contains( statname ) )
			addToTally( new AdventureResult( AdventureResult.MYS, increase ) );

		else if ( AdventureResult.MOX_SUBSTAT.contains( statname ) )
			addToTally( new AdventureResult( AdventureResult.MOX, increase ) );
	}

	private void addToTally( AdventureResult result )
	{
		int index = tally.indexOf( result );

		if ( index == -1 )
			tally.add( result );
		else
			tally.set( index, AdventureResult.add( result, (AdventureResult) tally.get( index ) ) );
	}

	public void updateAdventure( boolean isComplete, boolean permitContinue )
	{
		if ( isComplete )
		{
			++currentIteration;
			--iterationsRemaining;
		}

		// Adventuring will be permitted only if the user
		// has not hit cancel, the adventure result claims
		// it's okay to continue, and iterations remain.

		if ( this.permitContinue && permitContinue && iterationsRemaining > 0 )
		{
			activeFrame.updateDisplay( KoLFrame.ADVENTURING_STATE, "Request " + currentIteration + " in progress..." );
			currentRequest.run();
		}
		else if ( iterationsRemaining <= 0 )
			activeFrame.updateDisplay( KoLFrame.LOGGED_IN_STATE, "Requests completed!" );
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
		this.currentIteration = 1;
		this.iterationsRemaining = iterations;
		this.currentRequest = request;
		this.permitContinue = true;
		request.run();
	}

	public void cancelRequest()
	{	this.permitContinue = false;
	}

	public boolean permitsContinue()
	{	return permitContinue;
	}
}
