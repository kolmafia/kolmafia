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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Collections;
import javax.swing.JOptionPane;

public class ClanManager implements KoLConstants
{
	private String SNAPSHOT_DIRECTORY;

	private KoLmafia client;
	private TreeMap profileMap;
	private TreeMap rosterMap;
	private TreeMap stashMap;

	public ClanManager( KoLmafia client )
	{
		this.client = client;
		this.profileMap = new TreeMap();
		this.rosterMap = new TreeMap();
		this.stashMap = new TreeMap();

		SNAPSHOT_DIRECTORY = "clan/";
	}

	public boolean initialize()
	{
		if ( profileMap.isEmpty() )
		{
			ClanMembersRequest cmr = new ClanMembersRequest( client );
			cmr.run();

			SNAPSHOT_DIRECTORY = "clan/" + cmr.getClanID() + "_" + new SimpleDateFormat( "yyyyMMdd" ).format( new Date() ) + "/";


			// With the clan member list retrieved, you make sure
			// the user wishes to spend the time to retrieve all
			// the information related to all clan members.

			if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
				profileMap.size() + " members are currently in your clan.\nThis process should take " +
				((int)(profileMap.size() / 15) + 1) + " minutes to complete.\nAre you sure you want to continue?",
				"Member list retrieved!", JOptionPane.YES_NO_OPTION ) )
					return false;

			// Now that it's known what the user wishes to continue,
			// you begin initializing all the data.

			client.updateDisplay( DISABLED_STATE, "Processing request..." );

			Iterator requestIterator = profileMap.values().iterator();
			for ( int i = 1; requestIterator.hasNext() && client.permitsContinue(); ++i )
			{
				client.updateDisplay( NOCHANGE, "Examining member " + i + " of " + profileMap.size() + "..." );
				((ProfileRequest) requestIterator.next()).run();

				// Manually add in a bit of lag so that it doesn't turn into
				// hammering the server for information.

				KoLRequest.delay( 2000 );
			}
		}

		return true;
	}

	public void registerMember( String playerName )
	{	profileMap.put( playerName.toLowerCase(), new ProfileRequest( client, playerName ) );
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
	 * Boots idle members from the clan.  The user will be prompted for
	 * the cutoff date relative to the current date.  Members whose last
	 * login date preceeds the cutoff date will be booted from the clan.
	 * If the clan member list was not previously initialized, this method
	 * will also initialize that list.
	 */

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

		Object currentMember;
		ProfileRequest memberLookup;

		Matcher lastonMatcher;
		List idleList = new ArrayList();

		Iterator memberIterator = profileMap.keySet().iterator();

		// In order to determine the last time the player
		// was idle, you need to manually examine each of
		// the player profiles for each player.

		for ( int i = 1; memberIterator.hasNext(); ++i )
		{
			currentMember = memberIterator.next();
			memberLookup = (ProfileRequest) profileMap.get( currentMember );
			if ( cutoffDate.after( memberLookup.getLastLogin() ) )
				idleList.add( currentMember );
		}

		// Now that all of the member profiles have been retrieved,
		// you show the user the complete list of profileMap.

		if ( idleList.isEmpty() )
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
				File file = new File( SNAPSHOT_DIRECTORY + "idlelist.txt" );

				try
				{
					file.getParentFile().mkdirs();
					PrintStream ostream = new PrintStream( new FileOutputStream( file, true ), true );

					for ( int i = 0; i < idleList.size(); ++i )
						ostream.println( idleList.get(i) );
				}
				catch ( Exception e )
				{
					throw new RuntimeException( "The file <" + file.getAbsolutePath() +
						"> could not be opened for writing" );
				}

				client.updateDisplay( ENABLED_STATE, "List of idle members saved to " + file.getAbsolutePath() );
			}
		}
	}

	private class ClanBootRequest extends KoLRequest
	{
		public ClanBootRequest( KoLmafia client, Object [] profileMap )
		{
			super( client, "clan_members.php" );

			addFormField( "pwd", client.getPasswordHash() );
			addFormField( "action", "modify" );
			addFormField( "begin", "0" );

			for ( int i = 0; i < profileMap.length; ++i )
				addFormField( "boot" + client.getPlayerID( (String) profileMap[i] ), "on" );
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
		// If initialization was unsuccessful, then don't
		// do anything.

		if ( !initialize() )
			return;

		File individualFile = new File( SNAPSHOT_DIRECTORY + "summary.htm" );

		// If the file already exists, a snapshot cannot be taken.
		// Therefore, notify the user of this. :)

		if ( individualFile.exists() )
		{
			JOptionPane.showMessageDialog( null, "You already created a snapshot today." );
			return;
		}

		// Next, retrieve a detailed copy of the clan
		// roster to complete initialization.

		if ( rosterMap.isEmpty() )
		{
			client.updateDisplay( DISABLED_STATE, "Retrieving additional data..." );
			(new DetailRosterRequest( client )).run();
			client.updateDisplay( DISABLED_STATE, "Storing clan snapshot..." );
		}

		// First create a file that contains a summary
		// (spreadsheet-style) of all the clan members;
		// imitate Ohayou's booze page for rendering.

		PrintStream ostream;

		String currentMember;
		ProfileRequest memberLookup;
		DetailRosterField rosterLookup;

		Iterator memberIterator = profileMap.keySet().iterator();

		try
		{
			individualFile.getParentFile().mkdirs();
			ostream = new PrintStream( new FileOutputStream( individualFile, true ), true );
			ostream.println( "<html><head><style> body, td { font-family: sans-serif; } </style></head><body>" );

			List classList = new ArrayList();
			List foodList = new ArrayList();
			List drinkList = new ArrayList();
			List rankList = new ArrayList();

			List pvpList = new ArrayList();
			List musList = new ArrayList();
			List mysList = new ArrayList();
			List moxList = new ArrayList();
			List powerList = new ArrayList();
			List karmaList = new ArrayList();

			while ( memberIterator.hasNext() )
			{
				currentMember = (String) memberIterator.next();
				memberLookup = (ProfileRequest) profileMap.get( currentMember );
				rosterLookup = (DetailRosterField) rosterMap.get( currentMember );

				classList.add( memberLookup.getClassType() );
				foodList.add( memberLookup.getFood() );
				drinkList.add( memberLookup.getDrink() );
				pvpList.add( memberLookup.getPvpRank() );

				rankList.add( rosterLookup.rank );

				musList.add( rosterLookup.mus );
				mysList.add( rosterLookup.mys );
				moxList.add( rosterLookup.mox );
				powerList.add( rosterLookup.power );
				karmaList.add( rosterLookup.karma );
			}

			Collections.sort( classList );
			Collections.sort( foodList );
			Collections.sort( drinkList );
			Collections.sort( rankList );

			ostream.println( "<table border=0 cellspacing=4 cellpadding=4><tr>" );
			ostream.println( "<td valign=top><b>Averages</b>:" );
			ostream.println( "<ul><li>PVP Rank: " + calculateAverage( pvpList ) + "</li>" );
			ostream.println( "<li>Muscle: " + calculateAverage( musList ) + "</li>" );
			ostream.println( "<li>Myst: " + calculateAverage( mysList ) + "</li>" );
			ostream.println( "<li>Moxie: " + calculateAverage( moxList ) + "</li>" );
			ostream.println( "<li>Power: " + calculateAverage( powerList ) + "</li>" );
			ostream.println( "<li>Karma: " + calculateAverage( karmaList ) + "</li>" );
			ostream.println( "</ul><b>Totals</b>:" );
			ostream.println( "<ul><li>Muscle: " + calculateTotal( musList ) + "</li>" );
			ostream.println( "<li>Myst: " + calculateTotal( mysList ) + "</li>" );
			ostream.println( "<li>Moxie: " + calculateTotal( moxList ) + "</li>" );
			ostream.println( "<li>Power: " + calculateTotal( powerList ) + "</li>" );
			ostream.println( "<li>Karma: " + calculateTotal( karmaList ) + "</li>" );
			ostream.println( "</ul></td>" );

			ostream.println( "<td valign=top><b>Class Breakdown</b>:" );
			printSummaryOfSummary( classList.iterator(), ostream );
			ostream.println( "</td><td valign=top><b>Rank Breakdown</b>:" );
			printSummaryOfSummary( rankList.iterator(), ostream );
			ostream.println( "</td><td valign=top><b>Food Breakdown</b>:" );
			printSummaryOfSummary( foodList.iterator(), ostream );
			ostream.println( "</td><td valign=top><b>Drink Breakdown</b>:" );
			printSummaryOfSummary( drinkList.iterator(), ostream );
			ostream.println( "</td></tr></table><br><br>" );

			ostream.println();
			ostream.println( "<table border=1 cellspacing=2 cellpadding=2>" );
			ostream.print( "<tr bgcolor=\"#000000\" style=\"color:#ffffff; font-weight: bold\">" );
			ostream.print( "<td>Name</td><td>Lv</td><td>Mus</td><td>Mys</td><td>Mox</td><td>Total</td>" );
			ostream.print( "<td>Title</td><td>Rank</td><td>Karma</td><td>PVP</td><td>Class</td>" );
			ostream.println( "<td>Food</td><td>Drink</td><td>Last Login</td></tr>" );

			memberIterator = profileMap.keySet().iterator();
			for ( int i = 1; memberIterator.hasNext(); ++i )
			{
				currentMember = (String) memberIterator.next();
				memberLookup = (ProfileRequest) profileMap.get( currentMember );

				ostream.print( "<tr><td><a href=\"profiles/" );
				ostream.print( client.getPlayerID( currentMember ) );
				ostream.print( ".htm\">" );
				ostream.print( client.getPlayerName( client.getPlayerID( currentMember ) ) );
				ostream.print( "</a></td><td>" );
				ostream.print( memberLookup.getPlayerLevel() );
				ostream.print( "</td>" );
				ostream.print( rosterMap.get( currentMember ).toString() );
				ostream.print( "<td>" );
				ostream.print( memberLookup.getPvpRank() );
				ostream.print( "</td><td>" );
				ostream.print( memberLookup.getClassType() );
				ostream.print( "</td><td>" );
				ostream.print( memberLookup.getFood() );
				ostream.print( "</td><td>" );
				ostream.print( memberLookup.getDrink() );
				ostream.print( "</td><td>" );
				ostream.print( memberLookup.getLastLoginAsString() );
				ostream.println( "</td></tr>" );
			}

			ostream.println( "</table></body></html>" );
			ostream.close();
		}
		catch ( Exception e )
		{
			throw new RuntimeException( "The file <" + individualFile.getAbsolutePath() +
				"> could not be opened for writing" );
		}

		// Create a special HTML file for each of the
		// players in the snapshot so that it can be
		// navigated at leisure.

		memberIterator = profileMap.keySet().iterator();
		for ( int i = 1; memberIterator.hasNext(); ++i )
		{
			currentMember = (String) memberIterator.next();
			memberLookup = (ProfileRequest) profileMap.get( currentMember );

			individualFile = new File( SNAPSHOT_DIRECTORY + "profiles/" + client.getPlayerID( currentMember ) + ".htm" );

			try
			{
				individualFile.getParentFile().mkdirs();
				ostream = new PrintStream( new FileOutputStream( individualFile, true ), true );
				ostream.println( memberLookup.responseText );
				ostream.close();
			}
			catch ( Exception e )
			{
				throw new RuntimeException( "The file <" + individualFile.getAbsolutePath() +
					"> could not be opened for writing" );
			}
		}

		client.updateDisplay( ENABLED_STATE, "Clan snapshot generation completed." );
	}

	private void printSummaryOfSummary( Iterator itemIterator, PrintStream ostream )
	{
		int maximumCount = 0;
		int currentCount = 0;
		Object currentItem = itemIterator.next();
		Object favorite = currentItem;
		Object nextItem;

		ostream.println( "<ul>" );

		while ( itemIterator.hasNext() )
		{
			++currentCount;
			nextItem = itemIterator.next();
			if ( !currentItem.equals( nextItem ) )
			{
				ostream.println( "<li>" + currentItem.toString() + ": " + currentCount + "</li>" );
				if ( currentCount > maximumCount )
				{
					maximumCount = currentCount;
					favorite = currentItem;
				}

				currentItem = nextItem;
				currentCount = 0;
			}
		}

		ostream.println( "<li>" + currentItem.toString() + ": " + currentCount + "</li>" );
		if ( currentCount > maximumCount )
			favorite = currentItem;

		ostream.println( "</ul><hr width=\"80%\"><b>Favorite</b>: " + favorite.toString() );
	}

	private int calculateTotal( List values )
	{
		int total = 0;
		String currentValue;

		for ( int i = 0; i < values.size(); ++i )
		{
			currentValue = (String) values.get(i);
			if ( !currentValue.startsWith( "&" ) )
				total += Integer.parseInt( (String) values.get(i) );
		}

		return total;
	}

	private int calculateAverage( List values )
	{
		int total = 0;
		String currentValue;
		int actualSize = values.size();

		for ( int i = 0; i < values.size(); ++i )
		{
			currentValue = (String) values.get(i);
			if ( currentValue.startsWith( "&" ) )
				--actualSize;
			else
				total += Integer.parseInt( (String) values.get(i) );
		}

		return actualSize == 0 ? 0 : total / actualSize;
	}

	private class DetailRosterRequest extends KoLRequest
	{
		public DetailRosterRequest( KoLmafia client )
		{	super( client, "clan_detailedroster.php" );
		}

		public void run()
		{
			super.run();

			DetailRosterField currentMember;
			Matcher rowMatcher = Pattern.compile( "<tr>(.*?)</tr>" ).matcher( responseText );

			rowMatcher.find( 0 );
			int lastRowIndex = rowMatcher.end();

			while ( rowMatcher.find( lastRowIndex ) )
			{
				lastRowIndex = rowMatcher.end();

				if ( !rowMatcher.group(1).equals( "<td height=4></td>" ) )
				{
					currentMember = new DetailRosterField( rowMatcher.group(1) );
					rosterMap.put( currentMember.getPlayerName().toLowerCase(), currentMember );
				}
			}
		}
	}

	private class DetailRosterField
	{
		private String playerName;
		private String stringForm;

		private String mus, mys, mox, power;
		private String title, rank, karma;

		public DetailRosterField( String tableRow )
		{
			int firstCellIndex = tableRow.indexOf( "</td>" );
			this.stringForm = tableRow.substring( firstCellIndex + 5 ).replaceAll( "<td></td>", "<td>&nbsp;</td>" );
			this.playerName = tableRow.substring( 4, firstCellIndex );

			Matcher dataMatcher = Pattern.compile( "<td.*?>(.*?)</td>" ).matcher( stringForm );

			dataMatcher.find();
			this.mus = dataMatcher.group(1);

			dataMatcher.find( dataMatcher.end() );
			this.mys = dataMatcher.group(1);
			dataMatcher.find( dataMatcher.end() );
			this.mox = dataMatcher.group(1);
			dataMatcher.find( dataMatcher.end() );
			this.power = dataMatcher.group(1);

			dataMatcher.find( dataMatcher.end() );
			this.title = dataMatcher.group(1);
			dataMatcher.find( dataMatcher.end() );
			this.rank = dataMatcher.group(1);
			dataMatcher.find( dataMatcher.end() );
			this.karma = dataMatcher.group(1);
		}

		public String toString()
		{	return stringForm;
		}

		public String getPlayerName()
		{	return playerName;
		}
	}

	/**
	 * Stores all of the transactions made in the clan stash.  This loads the existing
	 * clan stash log and updates it with all transactions made by every clan member.
	 * this format allows people to see WHO is using the stash, rather than just what
	 * is being done with the stash.
	 */

	public void saveStashLog()
	{
		Iterator memberIterator = stashMap.keySet().iterator();
		File file = new File( "clan/stashlog.txt" );

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
			throw new RuntimeException( "The file <" + file.getAbsolutePath() +
				"> could not be opened for writing" );
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

	/**
	 * Retrieves the clan announcements from the clan hall and displays
	 * them in a standard JFrame.
	 */

	public void getAnnouncements()
	{
		RequestFrame announcements = new RequestFrame( client, "Clan Announcements", new AnnouncementsRequest( client ) );
		announcements.pack();  announcements.setVisible( true );  announcements.requestFocus();

	}

	private class AnnouncementsRequest extends KoLRequest
	{
		public AnnouncementsRequest( KoLmafia client )
		{	super( client, "clan_hall.php" );
		}

		public void run()
		{
			super.run();

			responseText = responseText.substring( responseText.indexOf( "<b><p><center>Recent" ) ).replaceAll(
				"<br />" , "<br>" ).replaceAll( "</?t.*?>" , "\n" ).replaceAll( "<blockquote>", "<br>" ).replaceAll(
					"</blockquote>", "" ).replaceAll( "\n", "" ).replaceAll( "</?center>", "" ).replaceAll(
						"</?f.*?>", "" ).replaceAll( "</?p>", "<br><br>" );

			responseText = responseText.substring( responseText.indexOf( "<b>Date" ) );
		}
	}

	/**
	 * Retrieves the clan board posts from the clan hall and displays
	 * them in a standard JFrame.
	 */

	public void getMessageBoard()
	{
		RequestFrame clanboard = new RequestFrame( client, "Clan Message Board", new MessageBoardRequest( client ) );
		clanboard.pack();  clanboard.setVisible( true );  clanboard.requestFocus();

	}

	private class MessageBoardRequest extends KoLRequest
	{
		public MessageBoardRequest( KoLmafia client )
		{	super( client, "clan_board.php" );
		}

		public void run()
		{
			super.run();

			responseText = responseText.substring( responseText.indexOf( "<p><b><center>Clan" ) ).replaceAll(
				"<br />" , "<br>" ).replaceAll( "</?t.*?>" , "\n" ).replaceAll( "<blockquote>", "<br>" ).replaceAll(
					"</blockquote>", "" ).replaceAll( "\n", "" ).replaceAll( "</?center>", "" ).replaceAll(
						"</?f.*?>", "" ).replaceAll( "</?p>", "<br><br>" );

			responseText = responseText.substring( responseText.indexOf( "<b>Date" ) );
		}
	}
}