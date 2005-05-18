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
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Date;
import javax.swing.JOptionPane;
import java.text.SimpleDateFormat;
import java.util.Collections;

public class ClanManager implements KoLConstants
{
	private KoLmafia client;
	private TreeMap memberData;

	public ClanManager( KoLmafia client )
	{
		this.client = client;
		this.memberData = new TreeMap();
	}

	public boolean initialize()
	{
		if ( memberData.isEmpty() )
		{
			ClanMembersRequest cmr = new ClanMembersRequest( client );
			cmr.run();

			// With the clan member list retrieved, you make sure
			// the user wishes to spend the time to retrieve all
			// the information related to all clan members.

			boolean continueProcessing = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog( null,
				memberData.size() + " members are currently in your clan.\nThis process should take " +
				((int)(memberData.size() / 15) + 1) + " minutes to complete.\nAre you sure you want to continue?",
				"Member list retrieved!", JOptionPane.YES_NO_OPTION );

			if ( !continueProcessing )
				return false;

			// Now that it's known what the user wishes to continue,
			// you begin initializing all the data.

			client.updateDisplay( DISABLED_STATE, "Processing request..." );

			Iterator requestIterator = memberData.values().iterator();
			for ( int i = 1; requestIterator.hasNext() && client.permitsContinue(); ++i )
			{
				client.updateDisplay( NOCHANGE, "Examining member " + i + " of " + memberData.size() + "..." );
				((ProfileRequest) requestIterator.next()).run();
			}
		}

		return true;
	}

	public void registerMember( String playerName )
	{	memberData.put( playerName, new ProfileRequest( client, playerName ) );
	}

	public void bootIdleMembers()
	{
		// If initialization was unsuccessful, then don't
		// do anything.

		if ( !initialize() )
			return;

		// It has been confirmed that the user wishes to
		// continue.  Now, you need to retrieve the cutoff
		// horizon the user wishes to use.

		Date cutoffDate;

		try
		{
			int daysIdle = df.parse( JOptionPane.showInputDialog( "How many days since last login?", "30" ) ).intValue();
			long millisecondsIdle = 86400000L * daysIdle;
			cutoffDate = new Date( System.currentTimeMillis() - millisecondsIdle );
		}
		catch ( Exception e )
		{
			// If user doesn't enter an integer, then just return
			// without doing anything

			return;
		}

		Pattern lastonPattern = Pattern.compile( "<b>Last Login:</b> (.*?)<br>" );

		Object currentMember;
		ProfileRequest memberLookup;

		Iterator memberIterator = memberData.keySet().iterator();
		SimpleDateFormat sdf = new SimpleDateFormat( "MMMM d, yyyy" );

		Matcher lastonMatcher;
		List idleList = new ArrayList();

		// In order to determine the last time the player
		// was idle, you need to manually examine each of
		// the player profiles for each player.

		for ( int i = 1; memberIterator.hasNext() && client.permitsContinue(); ++i )
		{
			currentMember = memberIterator.next();
			memberLookup = (ProfileRequest) memberData.get( currentMember );
			lastonMatcher = lastonPattern.matcher( memberLookup.getCleanHTML() );
			lastonMatcher.find();

			try
			{
				if ( cutoffDate.after( sdf.parse( lastonMatcher.group(1) ) ) )
					idleList.add( currentMember );
			}
			catch ( Exception e )
			{
			}

			// Manually add in a bit of lag so that it doesn't turn into
			// hammering the server for information.

			KoLRequest.delay( 2000 );
		}

		// Now that all of the member profiles have been retrieved,
		// you show the user the complete list of memberData.

		if ( idleList.size() == 0 )
			JOptionPane.showMessageDialog( null, "No idle accounts detected!" );
		else
		{
			Collections.sort( idleList );
			Object selectedValue = JOptionPane.showInputDialog( null, idleList.size() + " idle members:",
				"Idle hands!", JOptionPane.INFORMATION_MESSAGE, null, idleList.toArray(), idleList.get(0) );

			// Now, you need to determine what to do with the data;
			// sometimes, it shows too many players, and the person
			// wishes to retain some and cancels the process.

			if ( selectedValue != null )
			{
				(new ClanBootRequest( client, idleList.toArray() )).run();
				client.updateDisplay( ENABLED_STATE, "Idle members have been booted." );
			}
			else
			{
				File file = new File( "data/" + "IdleMembers.txt" );

				try
				{
					file.getParentFile().mkdirs();
					PrintStream ostream = new PrintStream( new FileOutputStream( file, true ), true );

					for ( int i = 0; i < idleList.size(); ++i )
						ostream.println( idleList.get(i) );
				}
				catch ( Exception e )
				{	throw new RuntimeException( "The file <" + file.getAbsolutePath() + "> could not be opened for writing" );
				}

				client.updateDisplay( ENABLED_STATE, "List of idle members saved to " + file.getAbsolutePath() );
			}
		}
	}

	private class ClanBootRequest extends KoLRequest
	{
		public ClanBootRequest( KoLmafia client, Object [] memberData )
		{
			super( client, "clan_members.php" );

			addFormField( "pwd", client.getPasswordHash() );
			addFormField( "action", "modify" );
			addFormField( "begin", "0" );

			for ( int i = 0; i < memberData.length; ++i )
				addFormField( "boot" + client.getPlayerID( (String) memberData[i] ), "on" );
		}
	}

	public void attackClan()
	{	(new ClanListRequest( client )).run();
	}

	private class ClanListRequest extends KoLRequest
	{
		public ClanListRequest( KoLmafia client )
		{	super( client, "clan_attack.php" );
		}

		public void run()
		{
			client.updateDisplay( DISABLED_STATE, "Retrieving list of attackable clans..." );

			super.run();

			if ( isErrorState )
				return;

			List enemyClans = new ArrayList();
			Matcher clanMatcher = Pattern.compile( "name=whichclan value=(\\d+)></td><td><b>(.*?)</td><td>(.*?)</td>" ).matcher( responseText );
			int lastMatchIndex = 0;

			while ( clanMatcher.find( lastMatchIndex ) )
			{
				lastMatchIndex = clanMatcher.end();
				enemyClans.add( new ClanAttackRequest( client, clanMatcher.group(1), clanMatcher.group(2), Integer.parseInt( clanMatcher.group(3) ) ) );
			}

			if ( enemyClans.isEmpty() )
			{
				JOptionPane.showMessageDialog( null, "Sorry, you cannot attack a clan at this time." );
				return;
			}

			Collections.sort( enemyClans );
			Object [] enemies = enemyClans.toArray();

			ClanAttackRequest enemy = (ClanAttackRequest) JOptionPane.showInputDialog( null,
				"Attack the following clan...", "Clans With Goodies", JOptionPane.INFORMATION_MESSAGE, null, enemies, enemies[0] );

			if ( enemy == null )
			{
				client.updateDisplay( ENABLED_STATE, "" );
				return;
			}

			enemy.run();
		}

		private class ClanAttackRequest extends KoLRequest implements Comparable
		{
			private String name;
			private int goodies;

			public ClanAttackRequest( KoLmafia client, String id, String name, int goodies )
			{
				super( client, "clan_attack.php" );
				addFormField( "whichclan", id );

				this.name = name;
				this.goodies = goodies;
			}

			public void run()
			{
				client.updateDisplay( DISABLED_STATE, "Attacking " + name + "..." );

				super.run();

				// Theoretically, there should be a test for error state,
				// but because I'm lazy, that's not happening.

				client.updateDisplay( ENABLED_STATE, "Attack request processed." );
			}

			public String toString()
			{	return name + " (" + goodies + " " + (goodies == 1 ? "bag" : "bags") + ")";
			}

			public int compareTo( Object o )
			{	return o == null || !(o instanceof ClanAttackRequest) ? -1 : compareTo( (ClanAttackRequest) o );
			}

			public int compareTo( ClanAttackRequest car )
			{
				int goodiesDifference = car.goodies - goodies;
				return goodiesDifference != 0 ? goodiesDifference : name.compareToIgnoreCase( car.name );
			}
		}

	}
}