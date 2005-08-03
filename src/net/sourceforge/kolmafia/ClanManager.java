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

import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Collections;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanManager implements KoLConstants
{
	private String SNAPSHOT_DIRECTORY;

	private KoLmafia client;
	private String clanID;
	private String clanName;

	private ClanSnapshotTable snapshot;

	private Map profileMap;
	private Map stashMap;

	private LockableListModel rankList;
	private LockableListModel stashContents;

	public ClanManager( KoLmafia client )
	{
		this.client = client;
		SNAPSHOT_DIRECTORY = "clan" + File.separator;

		this.snapshot = new ClanSnapshotTable( client );
		this.profileMap = snapshot.getProfileMap();

		this.stashMap = new TreeMap();
		this.rankList = new LockableListModel();
		this.stashContents = new LockableListModel();
	}

	public LockableListModel getStash()
	{	return stashContents;
	}

	public LockableListModel getRankList()
	{
		if ( rankList.isEmpty() )
			(new RankListRequest( client )).run();
		return rankList;
	}

	private class RankListRequest extends KoLRequest
	{
		public RankListRequest( KoLmafia client )
		{	super( client, "clan_members.php" );
		}

		public void run()
		{
			updateDisplay( DISABLED_STATE, "Retrieving list of ranks..." );
			super.run();

			rankList.clear();
			Matcher ranklistMatcher = Pattern.compile( "<select.*?</select>" ).matcher( responseText );

			if ( ranklistMatcher.find() )
			{
				Matcher rankMatcher = Pattern.compile( "<option.*?>(.*?)</option>" ).matcher( ranklistMatcher.group() );
				int lastMatchIndex = 0;

				while ( rankMatcher.find( lastMatchIndex ) )
				{
					lastMatchIndex = rankMatcher.end();
					rankList.add( rankMatcher.group(1) );
				}
			}

			updateDisplay( ENABLED_STATE, "List of ranks retrieved." );
		}
	}

	private void retrieveClanData()
	{
		if ( profileMap.isEmpty() )
		{
			ClanMembersRequest cmr = new ClanMembersRequest( client );
			cmr.run();

			this.clanID = cmr.getClanID();
			snapshot.setClanID( this.clanID );

			this.clanName = cmr.getClanName();
			snapshot.setClanName( this.clanName );

			SNAPSHOT_DIRECTORY = "clan" + File.separator + clanID + "_" + sdf.format( new Date() ) + File.separator;
			client.updateDisplay( ENABLED_STATE, "Clan data retrieved." );
		}
	}

	private boolean retrieveMemberData()
	{
		// First, determine how many member profiles need to be retrieved
		// before this happens.

		int profilesNeeded = 0;

		File profile;  String currentName;  String currentProfile;
		Iterator nameIterator = profileMap.keySet().iterator();

		while ( nameIterator.hasNext() )
		{
			currentName = (String) nameIterator.next();
			currentProfile = (String) profileMap.get( currentName );

			profile = new File( SNAPSHOT_DIRECTORY + "profiles" + File.separator + client.getPlayerID( currentName ) + ".htm" );

			if ( currentProfile.equals( "" ) && !profile.exists() )
				++profilesNeeded;

			if ( currentProfile.equals( "" ) && profile.exists() )
				initializeProfile( currentName );
		}

		// If all the member profiles have already been retrieved, then
		// you won't need to look up any profiles, so it takes no time.
		// No need to confirm with the user.  Therefore, return.

		if ( profilesNeeded == 0 )
			return true;

		if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
			profileMap.size() + " members are currently in your clan.\nThis process will take " +
			((int)(profilesNeeded / 10) + 1) + " minutes to complete.\nAre you sure you want to continue?",
			"Member list retrieved!", JOptionPane.YES_NO_OPTION ) )
				return false;

		// Now that it's known what the user wishes to continue,
		// you begin initializing all the data.

		client.updateDisplay( DISABLED_STATE, "Processing request..." );
		nameIterator = profileMap.keySet().iterator();

		// Create a special HTML file for each of the
		// players in the snapshot so that it can be
		// navigated at leisure.

		for ( int i = 1; nameIterator.hasNext() && client.permitsContinue(); ++i )
		{
			client.updateDisplay( DISABLED_STATE, "Examining member " + i + " of " + profileMap.size() + "..." );

			currentName = (String) nameIterator.next();
			currentProfile = (String) profileMap.get( currentName );

			if ( currentProfile.equals( "" ) )
				initializeProfile( currentName );
		}

		return true;
	}

	private void initializeProfile( String name )
	{
		File profile = new File( SNAPSHOT_DIRECTORY + "profiles" + File.separator + client.getPlayerID( name ) + ".htm" );

		if ( profile.exists() )
		{
			// In the event that the profile has already been retrieved,
			// then load the data from disk.

			try
			{
				BufferedReader istream = new BufferedReader( new InputStreamReader( new FileInputStream( profile ) ) );
				StringBuffer profileString = new StringBuffer();
				String currentLine;

				while ( (currentLine = istream.readLine()) != null )
				{
					profileString.append( currentLine );
					profileString.append( System.getProperty( "line.separator" ) );
				}

				profileMap.put( name, profileString.toString() );
			}
			catch ( Exception e )
			{
				e.printStackTrace( client.getLogStream() );
				e.printStackTrace( System.err );
				return;
			}
		}
		else
		{
			// Otherwise, run the request and pull the data from the
			// web server.

			ProfileRequest request = new ProfileRequest( client, name );
			request.initialize();
			profileMap.put( name, request.responseText );

			// Manually add in a bit of lag so that it doesn't turn into
			// hammering the server for information.

			KoLRequest.delay( 500 );

			// To avoid retrieving the file again, store the intermediate
			// result in a local file.

			try
			{
				profile.getParentFile().mkdirs();
				PrintStream ostream = new PrintStream( new FileOutputStream( profile, true ), true );
				ostream.println( request.responseText );
				ostream.close();
			}
			catch ( Exception e )
			{
				client.updateDisplay( ERROR_STATE, "Failed to load cached profile." );
				e.printStackTrace( client.getLogStream() );
				e.printStackTrace( System.err );
				return;
			}

		}
	}

	public void registerMember( String playerName, String level )
	{	snapshot.registerMember( playerName, level );
	}

	public void unregisterMember( String playerID )
	{	snapshot.unregisterMember( playerID );
	}

	/**
	 * Attacks another clan in the Kingdom of Loathing.  This searches the list
	 * of clans for clans with goodie bags and prompts the user for which clan
	 * will be attacked.  If the user is not an administrator of the clan, or
	 * the clan has already attacked someone else in the last three hours, the
	 * user will be notified that an attack is not possible.
	 */

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
				client.updateDisplay( ENABLED_STATE, "" );
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

	/**
	 * Takes a snapshot of clan member data for this clan.  The user will
	 * be prompted for the data they would like to include in this snapshot,
	 * including complete player profiles, favorite food, and any other
	 * data gathered by KoLmafia.  If the clan member list was not previously
	 * initialized, this method will also initialize that list.
	 */

	public void takeSnapshot()
	{
		retrieveClanData();

		// If the file already exists, a snapshot cannot be taken.
		// Therefore, notify the user of this. :)

		File summaryFile = new File( SNAPSHOT_DIRECTORY + "summary.htm" );

		if ( summaryFile.exists() )
		{
			JOptionPane.showMessageDialog( null, "You already created a snapshot today." );
			return;
		}

		// If initialization was unsuccessful, then there isn't
		// enough data to create a clan snapshot.

		String header = snapshot.getHeader();

		if ( header.indexOf( "<td>PVP</td>" ) != -1 || header.indexOf( "<td>Class</td>" ) != -1 || header.indexOf( "<td>Meat</td>" ) != -1 ||
			header.indexOf( "<td>Turns</td>" ) != -1 || header.indexOf( "<td>Food</td>" ) != -1 || header.indexOf( "<td>Drink</td>" ) != -1 ||
				header.indexOf( "<td>Last Login</td>" ) != -1 || header.indexOf( "<td>Ascensions</td>" ) != -1 )
		{
			if ( !retrieveMemberData() )
			{
				client.updateDisplay( ERROR_STATE, "Initialization failed." );
				return;
			}
		}

		// Now, store the clan snapshot into the appropriate
		// data folder.

		try
		{
			summaryFile.getParentFile().mkdirs();
			client.updateDisplay( DISABLED_STATE, "Storing clan snapshot..." );

			PrintStream ostream = new PrintStream( new FileOutputStream( summaryFile, true ), true );
			ostream.println( snapshot.toString() );
			ostream.close();
		}
		catch ( Exception e )
		{
			client.updateDisplay( ERROR_STATE, "Clan snapshot generation failed." );
			e.printStackTrace( client.getLogStream() );
			e.printStackTrace( System.err );
			return;
		}

		client.updateDisplay( ENABLED_STATE, "Clan snapshot generation completed." );
	}

	/**
	 * Stores all of the transactions made in the clan stash.  This loads the existing
	 * clan stash log and updates it with all transactions made by every clan member.
	 * this format allows people to see WHO is using the stash, rather than just what
	 * is being done with the stash.
	 */

	public void saveStashLog()
	{
		File file = new File( "clan/stashlog_" + clanID + ".txt" );

		try
		{
			String currentMember = "";
			List entryList;

			if ( file.exists() )
			{
				BufferedReader istream = new BufferedReader( new InputStreamReader( new FileInputStream( file ) ) );
				String line;

				while ( (line = istream.readLine()) != null )
				{
					if ( line.startsWith( " " ) )
					{
						entryList = (List) stashMap.get( currentMember );
						if ( entryList == null )
						{
							entryList = new ArrayList();
							stashMap.put( currentMember, entryList );
						}

						if ( !entryList.contains( line ) )
							entryList.add( line );
					}
					else
						currentMember = line.substring( 0, line.length() - 1 );
				}

				istream.close();
			}

			client.updateDisplay( DISABLED_STATE, "Retrieving clan stash log..." );
			(new StashLogRequest( client )).run();
			client.updateDisplay( ENABLED_STATE, "Stash log retrieved." );

			file.delete();
			file.getParentFile().mkdirs();
			file.createNewFile();

			Iterator memberIterator = stashMap.keySet().iterator();
			PrintStream ostream = new PrintStream( new FileOutputStream( file, true ), true );
			Iterator withdrawals;

			while ( memberIterator.hasNext() )
			{
				currentMember = (String) memberIterator.next();
				ostream.println( currentMember + ":" );

				withdrawals = ((List) stashMap.get( currentMember )).iterator();
				while ( withdrawals.hasNext() )
					ostream.println( withdrawals.next().toString() );

				ostream.println();
			}

			ostream.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace( System.err );
			throw new RuntimeException( "The file <" + file.getAbsolutePath() + "> could not be opened for writing" );
		}
	}

	private class StashLogRequest extends KoLRequest
	{
		public StashLogRequest( KoLmafia client )
		{	super( client, "clan_log.php" );
		}

		public void run()
		{
			super.run();

			String lastEntry;
			List lastEntryList;
			String currentMember;
			int lastEntryIndex = 0;

			Matcher entryMatcher = Pattern.compile(
				"<option value=\"(\\d+)\">(.*?): (.*?) (gave|took) an item: (.*?)</option>" ).matcher( responseText );

			while ( entryMatcher.find( lastEntryIndex ) )
			{
				lastEntryIndex = entryMatcher.end();
				currentMember = entryMatcher.group(3);

				lastEntry = (entryMatcher.group(4).equals( "gave" ) ? " [ D_" : " [W_") +
					entryMatcher.group(1) + "] " + entryMatcher.group(2) + ": " + entryMatcher.group(5);

				lastEntryList = (List) stashMap.get( currentMember );

				if ( lastEntryList == null )
				{
					lastEntryList = new ArrayList();
					stashMap.put( currentMember, lastEntryList );
				}

				if ( !lastEntryList.contains( lastEntry ) )
					lastEntryList.add( lastEntry );
			}

			entryMatcher = Pattern.compile(
				"<option value=\"(\\d+)\">(.*?): (.*?) took an item: (.*?)</option>" ).matcher( responseText );

			while ( entryMatcher.find( lastEntryIndex ) )
			{
				lastEntryIndex = entryMatcher.end();

				lastEntry = " [ W_" + entryMatcher.group(1) + "] " + entryMatcher.group(2) + ": " + entryMatcher.group(4);
				lastEntryList = (List) stashMap.get( entryMatcher.group(3) );

				if ( lastEntryList == null )
					lastEntryList = new ArrayList();

				if ( !lastEntryList.contains( lastEntry ) )
					lastEntryList.add( lastEntry );
			}
		}
	}

	public void postMessage()
	{
		Object [] parameters = new Object[3];
		parameters[0] = client;
		parameters[1] = "Clan Board Post";
		parameters[2] = new MessagePostPanel( "post" );

		SwingUtilities.invokeLater( new CreateFrameRunnable( KoLPanelFrame.class, parameters ) );
	}

	public void postAnnouncement()
	{
		Object [] parameters = new Object[3];
		parameters[0] = client;
		parameters[1] = "Clan Announcement";
		parameters[2] = new MessagePostPanel( "postannounce" );

		SwingUtilities.invokeLater( new CreateFrameRunnable( KoLPanelFrame.class, parameters ) );
	}

	private class MessagePostPanel extends KoLPanel
	{
		private String action;
		private JTextArea messageEntry;

		public MessagePostPanel( String action )
		{
			super( "post", "clear", new Dimension( 1, 20 ), new Dimension( 300, 20 ) );
			this.action = action;

			messageEntry = new JTextArea( 8, 32 );
			messageEntry.setLineWrap( true );
			messageEntry.setWrapStyleWord( true );
			JScrollPane scrollArea = new JScrollPane( messageEntry,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "", scrollArea );
			setContent( elements );
		}

		public void actionConfirmed()
		{
			KoLRequest postRequest = new KoLRequest( client, "clan_board.php" );
			postRequest.addFormField( "action", action );
			postRequest.addFormField( "message", messageEntry.getText() );

			(new RequestThread( postRequest )).start();
		}

		public void actionCancelled()
		{	messageEntry.setText( "" );
		}
	}

	/**
	 * Retrieves the clan announcements from the clan hall and displays
	 * them in a standard JFrame.
	 */

	public void getAnnouncements()
	{
		Object [] parameters = new Object[3];
		parameters[0] = client;
		parameters[1] = "Clan Announcements";
		parameters[2] = new ClanMessageRequest( client, "clan_hall.php" );

		SwingUtilities.invokeLater( new CreateFrameRunnable( RequestFrame.class, parameters ) );
	}

	/**
	 * Retrieves the clan board posts from the clan hall and displays
	 * them in a standard JFrame.
	 */

	public void getMessageBoard()
	{
		Object [] parameters = new Object[3];
		parameters[0] = client;
		parameters[1] = "Clan Message Board";
		parameters[2] = new ClanMessageRequest( client, "clan_board.php" );

		SwingUtilities.invokeLater( new CreateFrameRunnable( RequestFrame.class, parameters ) );
	}

	private class ClanMessageRequest extends KoLRequest
	{
		public ClanMessageRequest( KoLmafia client, String location )
		{	super( client, location );
		}

		public void run()
		{
			super.run();

			int startMessageIndex = responseText.indexOf( "<p><b><center>" );
			if ( startMessageIndex == -1 )
				startMessageIndex = responseText.indexOf( "<b><p><center>" );

			// After running the request, filter out the extraneous
			// HTML in the response text.

			responseText = responseText.substring( startMessageIndex ).replaceAll(
					"<br />" , "<br>" ).replaceAll( "</?t.*?>" , "\n" ).replaceAll( "<blockquote>", "<br>" ).replaceAll(
						"</blockquote>", "" ).replaceAll( "\n", "" ).replaceAll( "</?center>", "" ).replaceAll(
							"</?f.*?>", "" ).replaceAll( "</?p>", "<br><br>" );

			responseText = responseText.substring( responseText.indexOf( "<b>Date" ) );
		}
	}

	/**
	 * Retrieves the clan membership in the form of a
	 * CDL (comma-delimited list)
	 */

	public String retrieveClanListAsCDL()
	{
		retrieveClanData();

		StringBuffer clanCDL = new StringBuffer();
		Iterator memberIterator = profileMap.keySet().iterator();

		for ( int i = 1; memberIterator.hasNext(); ++i )
		{
			if ( i != 1 )  clanCDL.append( ", " );
			clanCDL.append( (String) memberIterator.next() );
		}

		return clanCDL.toString();
	}

	public LockableListModel getFilteredList()
	{
		return snapshot != null ? snapshot.getFilteredList() : new LockableListModel();
	}

	public void applyFilter( int matchType, int filterType, String filter )
	{
		retrieveClanData();

		// Certain filter types do not require the player profiles
		// to be looked up.  These can be processed immediately,
		// without prompting the user for confirmation.

		switch ( filterType )
		{
			case ClanSnapshotTable.NAME_FILTER:
			case ClanSnapshotTable.ID_FILTER:
			case ClanSnapshotTable.LV_FILTER:
			case ClanSnapshotTable.RANK_FILTER:
			case ClanSnapshotTable.KARMA_FILTER:

				break;

			default:

				retrieveMemberData();
				break;
		}

		snapshot.applyFilter( matchType, filterType, filter );
	}

}