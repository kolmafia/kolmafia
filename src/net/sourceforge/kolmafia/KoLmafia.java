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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class KoLmafia
{
	private static final String ADV_DBASE_FILE = "adventures.dat";

	private static List<String> MUS = new ArrayList<String>();
	private static List<String> MYS = new ArrayList<String>();
	private static List<String> MOX = new ArrayList<String>();

	static
	{
		MUS.add( "Beefiness" );  MUS.add( "Fortitude" );  MUS.add( "Muscleboundness" );  MUS.add( "Strengthliness" );  MUS.add( "Strongness" );
		MYS.add( "Enchantedness" );  MYS.add( "Magicalness" );  MYS.add( "Mysteriousness" );  MYS.add( "Wizardliness" );
		MOX.add( "Cheek" );  MOX.add( "Chutzpah" );  MOX.add( "Roguishness" );  MOX.add( "Sarcasm" );  MOX.add( "Smarm" );
	}

	private String loginname, password, sessionID, passwordHash;
	private KoLFrame activeFrame;

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
			StringTokenizer strtok = new StringTokenizer( advdata.readLine(), "\t" );
			String adventureID = strtok.nextToken();
			adventures.add( new KoLAdventure( this, adventureID, strtok.nextToken() ) );
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

		activeFrame = new AdventureFrame( this, adventures );
		activeFrame.pack();  activeFrame.setVisible( true );
	}

	public void acquireItem( String itemname )
	{
		StringTokenizer strtok = new StringTokenizer( itemname, "()" );

		String item = strtok.nextToken();
		int increase = strtok.hasMoreTokens() ? Integer.parseInt( strtok.nextToken() ) : 1;
	}

	public void modifyStat( int increase, String statname )
	{
		if ( MUS.contains( statname ) )
			;
		if ( MYS.contains( statname ) )
			;
		if ( MOX.contains( statname ) )
			;
	}

	public void updateAdventure( boolean isComplete, boolean permitContinue )
	{
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
}
