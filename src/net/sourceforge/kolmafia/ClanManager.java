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

import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import java.awt.CardLayout;
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
import java.util.Collections;
import java.text.SimpleDateFormat;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

import edu.stanford.ejalbert.BrowserLauncher;
import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanManager extends StaticEntity
{
	private static final String STASH_ADD = "add";
	private static final String STASH_TAKE = "take";
	private static final String WAR_BATTLE = "warfare";
	private static final String CLAN_ACCEPT = "accept";
	private static final String CLAN_LEAVE = "leave";
	private static final String CLAN_BOOT = "boot";

	private static final String TIME_REGEX = "(\\d\\d/\\d\\d/\\d\\d, \\d\\d:\\d\\d[AP]M)";
	private static final SimpleDateFormat STASH_FORMAT = new SimpleDateFormat( "MM/dd/yy, hh:mma" );
	private static final SimpleDateFormat DIRECTORY_FORMAT = new SimpleDateFormat( "yyyyMM_'w'W" );

	private static String SNAPSHOT_DIRECTORY = "clan" + File.separator;

	private static String clanID;
	private static String clanName;

	private static boolean ranksRetrieved = false;
	private static Map profileMap = ClanSnapshotTable.getProfileMap();
	private static Map ascensionMap = AscensionSnapshotTable.getAscensionMap();
	private static Map stashMap = new TreeMap();
	private static List battleList = new ArrayList();

	private static LockableListModel rankList = new LockableListModel();
	private static LockableListModel stashContents = new LockableListModel();

	public static void reset()
	{
		ClanSnapshotTable.reset();
		AscensionSnapshotTable.reset();

		ranksRetrieved = false;
		profileMap.clear();
		ascensionMap.clear();
		stashMap.clear();
		battleList.clear();
		rankList.clear();
		stashContents.clear();
	}

	public static String getClanID()
	{	return clanID;
	}

	public static String getClanName()
	{	return clanName;
	}

	public static LockableListModel getStash()
	{	return stashContents;
	}

	public static LockableListModel getRankList()
	{
		if ( !ranksRetrieved )
		{
			(new RankListRequest( client )).run();
			ranksRetrieved = true;
		}

		return rankList;
	}

	private static class RankListRequest extends KoLRequest
	{
		public RankListRequest( KoLmafia client )
		{	super( client, "clan_members.php" );
		}

		public void run()
		{
			updateDisplay( DISABLE_STATE, "Retrieving list of ranks..." );
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

			updateDisplay( NORMAL_STATE, "List of ranks retrieved." );
		}
	}

	private static void retrieveClanData()
	{
		if ( profileMap.isEmpty() )
		{
			ClanMembersRequest cmr = new ClanMembersRequest( client );
			cmr.run();

			clanID = cmr.getClanID();
			clanName = cmr.getClanName();

			SNAPSHOT_DIRECTORY = "clan" + File.separator + clanID + File.separator + DIRECTORY_FORMAT.format( new Date() ) + File.separator;
			client.updateDisplay( NORMAL_STATE, "Clan data retrieved." );
		}
	}

	private static boolean retrieveMemberData( boolean retrieveProfileData, boolean retrieveAscensionData )
	{
		// First, determine how many member profiles need to be retrieved
		// before this happens.

		int requestsNeeded = 0;

		File profile, ascensionData;
		String currentName, currentProfile, currentAscensionData;

		Iterator nameIterator = profileMap.keySet().iterator();

		while ( nameIterator.hasNext() )
		{
			currentName = (String) nameIterator.next();

			currentProfile = (String) profileMap.get( currentName );
			currentAscensionData = (String) ascensionMap.get( currentName );

			profile = new File( SNAPSHOT_DIRECTORY + "profiles" + File.separator + client.getPlayerID( currentName ) + ".htm" );
			ascensionData = new File( SNAPSHOT_DIRECTORY + "ascensions" + File.separator + client.getPlayerID( currentName ) + ".htm" );

			if ( retrieveProfileData )
			{
				if ( currentProfile.equals( "" ) && !profile.exists() )
					++requestsNeeded;

				if ( currentProfile.equals( "" ) && profile.exists() )
					initializeProfile( currentName );
			}

			if ( retrieveAscensionData )
			{
				if ( currentAscensionData.equals( "" ) && !ascensionData.exists() )
					++requestsNeeded;

				if ( currentAscensionData.equals( "" ) && ascensionData.exists() )
					initializeAscensionData( currentName );
			}
		}

		// If all the member profiles have already been retrieved, then
		// you won't need to look up any profiles, so it takes no time.
		// No need to confirm with the user.  Therefore, return.

		if ( requestsNeeded == 0 )
			return true;

		StringBuffer message = new StringBuffer();
		message.append( profileMap.size() );
		message.append( " members are currently in your clan." );
		message.append( LINE_BREAK );

		// Server friendly requests take an additional 2 seconds per
		// request.  Because the estimate is that the server takes
		// 5 seconds per response, this effectively should double
		// the amount of time estimated (because you delay twice).

		message.append( "This process will take " );
		message.append( ((int)(requestsNeeded / (KoLRequest.isServerFriendly ? 4 : 10)) + 1) );
		message.append( " minutes to complete." );
		message.append( LINE_BREAK );

		message.append( "Are you sure you want to continue?" );
		message.append( LINE_BREAK );

		if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null, message.toString(), "Member list retrieved!", JOptionPane.YES_NO_OPTION ) )
			return false;

		// Now that it's known what the user wishes to continue,
		// you begin initializing all the data.

		client.updateDisplay( DISABLE_STATE, "Processing request..." );
		nameIterator = profileMap.keySet().iterator();

		// Create a special HTML file for each of the
		// players in the ClanSnapshotTable so that it can be
		// navigated at leisure.

		for ( int i = 1; nameIterator.hasNext() && client.permitsContinue(); ++i )
		{
			client.updateDisplay( DISABLE_STATE, "Examining member " + i + " of " + profileMap.size() + "..." );

			currentName = (String) nameIterator.next();
			currentProfile = (String) profileMap.get( currentName );
			currentAscensionData = (String) ascensionMap.get( currentName );

			if ( retrieveProfileData && currentProfile.equals( "" ) )
				initializeProfile( currentName );

			if ( retrieveAscensionData && currentAscensionData.equals( "" ) )
				initializeAscensionData( currentName );
		}

		return true;
	}

	private static void initializeProfile( String name )
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
					profileString.append( LINE_BREAK );
				}

				profileMap.put( name, profileString.toString() );
			}
			catch ( Exception e )
			{
				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );
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

			if ( !KoLRequest.isServerFriendly )
				KoLRequest.delay();

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

				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );

				return;
			}

		}
	}

	private static void initializeAscensionData( String name )
	{
		File ascension = new File( SNAPSHOT_DIRECTORY + "ascensions" + File.separator + client.getPlayerID( name ) + ".htm" );

		if ( ascension.exists() )
		{
			// In the event that the ascension has already been retrieved,
			// then load the data from disk.

			try
			{
				BufferedReader istream = new BufferedReader( new InputStreamReader( new FileInputStream( ascension ) ) );
				StringBuffer ascensionString = new StringBuffer();
				String currentLine;

				while ( (currentLine = istream.readLine()) != null )
				{
					ascensionString.append( currentLine );
					ascensionString.append( LINE_BREAK );
				}

				ascensionMap.put( name, ascensionString.toString() );
			}
			catch ( Exception e )
			{
				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );
				return;
			}
		}
		else
		{
			// Otherwise, run the request and pull the data from the
			// web server.

			AscensionDataRequest request = new AscensionDataRequest( client, name, client.getPlayerID( name ) );
			request.initialize();
			ascensionMap.put( name, request.responseText );

			// Manually add in a bit of lag so that it doesn't turn into
			// hammering the server for information.

			KoLRequest.delay();

			// To avoid retrieving the file again, store the intermediate
			// result in a local file.

			try
			{
				ascension.getParentFile().mkdirs();
				PrintStream ostream = new PrintStream( new FileOutputStream( ascension, true ), true );
				ostream.println( request.responseText );
				ostream.close();
			}
			catch ( Exception e )
			{
				client.updateDisplay( ERROR_STATE, "Failed to load cached ascension." );

				KoLmafia.getLogStream().println( e );
				e.printStackTrace( KoLmafia.getLogStream() );
				return;
			}

		}
	}

	public static void registerMember( String playerName, String level )
	{
		ClanSnapshotTable.registerMember( playerName, level );
		AscensionSnapshotTable.registerMember( playerName );
	}

	public static void unregisterMember( String playerID )
	{
		ClanSnapshotTable.unregisterMember( playerID );
		AscensionSnapshotTable.registerMember( playerID );
	}

	/**
	 * Attacks another clan in the Kingdom of Loathing.  This searches the list
	 * of clans for clans with goodie bags and prompts the user for which clan
	 * will be attacked.  If the user is not an administrator of the clan, or
	 * the clan has already attacked someone else in the last three hours, the
	 * user will be notified that an attack is not possible.
	 */

	public static void attackClan()
	{	(new ClanListRequest( client )).run();
	}

	private static class ClanListRequest extends KoLRequest
	{
		public ClanListRequest( KoLmafia client )
		{	super( client, "clan_attack.php" );
		}

		public void run()
		{
			client.updateDisplay( DISABLE_STATE, "Retrieving list of attackable clans..." );

			super.run();

			if ( responseCode != 200 )
				return;

			List enemyClans = new ArrayList();
			Matcher clanMatcher = Pattern.compile( "name=whichclan value=(\\d+)></td><td><b>(.*?)</td><td>(.*?)</td>" ).matcher( responseText );

			while ( clanMatcher.find() )
				enemyClans.add( new ClanAttackRequest( client, clanMatcher.group(1), clanMatcher.group(2), Integer.parseInt( clanMatcher.group(3) ) ) );

			if ( enemyClans.isEmpty() )
			{
				KoLRequest details = new KoLRequest( client, "clan_war.php" );
				details.run();

				Matcher nextMatcher = Pattern.compile( "<br>Your clan can attack again in (.*?)<p>" ).matcher( details.responseText );
				nextMatcher.find();

				JOptionPane.showMessageDialog( null, "Your clan can attack again in " + nextMatcher.group(1) );
				client.updateDisplay( ENABLE_STATE, "" );

				return;
			}

			Collections.sort( enemyClans );
			Object [] enemies = enemyClans.toArray();

			ClanAttackRequest enemy = (ClanAttackRequest) JOptionPane.showInputDialog( null,
				"Attack the following clan...", "Clans With Goodies", JOptionPane.INFORMATION_MESSAGE, null, enemies, enemies[0] );

			if ( enemy == null )
			{
				client.updateDisplay( ERROR_STATE, "" );
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
				client.updateDisplay( DISABLE_STATE, "Attacking " + name + "..." );

				super.run();

				// Theoretically, there should be a test for error state,
				// but because I'm lazy, that's not happening.

				client.updateDisplay( ENABLE_STATE, "Attack request processed." );
			}

			public String toString()
			{	return name + " (" + df.format( goodies ) + " " + (goodies == 1 ? "bag" : "bags") + ")";
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
	 * Takes a ClanSnapshotTable of clan member data for this clan.  The user will
	 * be prompted for the data they would like to include in this ClanSnapshotTable,
	 * including complete player profiles, favorite food, and any other
	 * data gathered by KoLmafia.  If the clan member list was not previously
	 * initialized, this method will also initialize that list.
	 */

	public static void takeSnapshot()
	{
		retrieveClanData();

		// If the file already exists, a ClanSnapshotTable cannot be taken.
		// Therefore, notify the user of this. :)

		File standardFile = new File( SNAPSHOT_DIRECTORY + "standard.htm" );
		File softcoreFile = new File( SNAPSHOT_DIRECTORY + "softcore.htm" );
		File hardcoreFile = new File( SNAPSHOT_DIRECTORY + "hardcore.htm" );

		if ( standardFile.exists() || softcoreFile.exists() || hardcoreFile.exists() )
		{
			JOptionPane.showMessageDialog( null, "You already created a snapshot this week." );
			return;
		}

		// Prompt the user to determine which settings they would
		// like during the clan ClanSnapshotTable process.

		JDialog dialog = new SnapshotOptionsDialog();
		dialog.pack();  dialog.setLocationRelativeTo( null );
		dialog.setVisible( true );
	}

	public static class SnapshotOptionsDialog extends JDialog
	{
		private JCheckBox [] optionBoxes;
		private final String [][] options =
		{
			{ "<td>Lv</td><td>Mus</td><td>Mys</td><td>Mox</td><td>Total</td>", "Stat points and power" },
			{ "<td>Title</td><td>Rank</td><td>Karma</td>", "Clan (title, rank, karma)" },
			{ "<td>PVP</td>", "Current PVP rankings" },
			{ "<td>Class</td>", "Current class" },
			{ "<td>Meat</td>", "Wealth accumulated" },
			{ "<td>Turns</td>", "Total turns used" },
			{ "<td>Food</td><td>Drink</td>", "Favorites (food, booze)" },
			{ "<td>Created</td>", "Account creation date" },
			{ "<td>Last Login</td>", "Last login date" },
			{ "<td>Ascensions</td>", "Ascension breakdown" }
		};

		public SnapshotOptionsDialog()
		{
			setTitle( "Clan Snapshot Settings" );
			getContentPane().setLayout( new BorderLayout() );
			getContentPane().add( new SnapshotOptionsPanel(), BorderLayout.CENTER );
		}

		/**
		 * This panel handles all of the things related to the clan
		 * ClanSnapshotTable.  For now, just a list of checkboxes to show
		 * which fields you want there.
		 */

		private class SnapshotOptionsPanel extends KoLPanel
		{
			public SnapshotOptionsPanel()
			{
				super( "confirm", "cancel", new Dimension( 340, 16 ), new Dimension( 20, 16 ) );
				VerifiableElement [] elements = new VerifiableElement[ options.length ];

				optionBoxes = new JCheckBox[ options.length ];
				for ( int i = 0; i < options.length; ++i )
					optionBoxes[i] = new JCheckBox();

				for ( int i = 0; i < options.length; ++i )
					elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

				setContent( elements, false );
				actionCancelled();
			}

			protected void actionConfirmed()
			{
				dispose();

				// Apply all the settings before generating the
				// needed clan ClanSnapshotTable.

				StringBuffer tableHeaderSetting = new StringBuffer();

				for ( int i = 0; i < options.length; ++i )
					if ( optionBoxes[i].isSelected() )
						tableHeaderSetting.append( options[i][0] );

				client.getSettings().setProperty( "clanRosterHeader", tableHeaderSetting.toString() );
				client.getSettings().saveSettings();

				// If initialization was unsuccessful, then there isn't
				// enough data to create a clan ClanSnapshotTable.

				File standardFile = new File( SNAPSHOT_DIRECTORY + "standard.htm" );
				File sortingScript = new File( SNAPSHOT_DIRECTORY + "sorttable.js" );
				File softcoreFile = new File( SNAPSHOT_DIRECTORY + "softcore.htm" );
				File hardcoreFile = new File( SNAPSHOT_DIRECTORY + "hardcore.htm" );

				String header = tableHeaderSetting.toString();

				boolean retrieveProfileData = header.indexOf( "<td>PVP</td>" ) != -1 || header.indexOf( "<td>Class</td>" ) != -1 ||
					header.indexOf( "<td>Meat</td>" ) != -1 || header.indexOf( "<td>Food</td>" ) != -1 || header.indexOf( "<td>Last Login</td>" ) != -1;

				boolean retrieveAscensionData = header.indexOf( "<td>Ascensions</td>" ) != -1;

				if ( !retrieveMemberData( retrieveProfileData, retrieveAscensionData ) )
				{
					client.updateDisplay( ERROR_STATE, "Initialization failed." );
					return;
				}

				standardFile.getParentFile().mkdirs();

				// Now, store the clan snapshot into the appropriate
				// data folder.

				try
				{
					PrintStream ostream;

					if ( !header.equals( "<td>Ascensions</td>" ) && !header.equals( "" ) )
					{
						client.updateDisplay( DISABLE_STATE, "Storing clan snapshot..." );

						ostream = new PrintStream( new FileOutputStream( standardFile, true ), true );
						ostream.println( ClanSnapshotTable.getStandardData() );
						ostream.close();

						String line;
						BufferedReader script = KoLDatabase.getReader( "sorttable.js" );
						ostream = new PrintStream( new FileOutputStream( sortingScript, true ), true );

						while ( (line = script.readLine()) != null )
							ostream.println( line );

						ostream.close();
					}

					if ( retrieveAscensionData )
					{
						client.updateDisplay( DISABLE_STATE, "Storing ascension snapshot..." );

						ostream = new PrintStream( new FileOutputStream( softcoreFile, true ), true );
						ostream.println( AscensionSnapshotTable.getAscensionData( true ) );
						ostream.close();

						ostream = new PrintStream( new FileOutputStream( hardcoreFile, true ), true );
						ostream.println( AscensionSnapshotTable.getAscensionData( false ) );
						ostream.close();
					}
				}
				catch ( Exception e )
				{
					client.updateDisplay( ERROR_STATE, "Clan snapshot generation failed." );

					KoLmafia.getLogStream().println( e );
					e.printStackTrace( KoLmafia.getLogStream() );
					return;
				}

				client.updateDisplay( ENABLE_STATE, "Snapshot generation completed." );

				try
				{
					// To make things less confusing, load the summary
					// file inside of the default browser after completion.

					if ( !header.equals( "<td>Ascensions</td>" ) && !header.equals( "" ) )
						BrowserLauncher.openURL( standardFile.toURL().toString() );

					if ( retrieveAscensionData )
					{
						BrowserLauncher.openURL( softcoreFile.toURL().toString() );
						BrowserLauncher.openURL( hardcoreFile.toURL().toString() );
					}
				}
				catch ( Exception e )
				{
					client.updateDisplay( ERROR_STATE, "Clan snapshot generation failed." );

					KoLmafia.getLogStream().println( e );
					e.printStackTrace( KoLmafia.getLogStream() );
					return;
				}
			}

			protected void actionCancelled()
			{
				String tableHeaderSetting = client.getSettings().getProperty( "clanRosterHeader" );
				for ( int i = 0; i < options.length; ++i )
					optionBoxes[i].setSelected( tableHeaderSetting.indexOf( options[i][0] ) != -1 );
			}
		}
	}

	/**
	 * Stores all of the transactions made in the clan stash.  This loads the existing
	 * clan stash log and updates it with all transactions made by every clan member.
	 * this format allows people to see WHO is using the stash, rather than just what
	 * is being done with the stash.
	 */

	public static void saveStashLog()
	{
		retrieveClanData();
		File file = new File( "clan" + File.separator + clanID + File.separator + "stashlog.htm" );

		try
		{
			String currentMember = "";

			List entryList;
			StashLogEntry entry;

			if ( file.exists() )
			{
				BufferedReader istream = new BufferedReader( new InputStreamReader( new FileInputStream( file ) ) );
				String line;

				boolean startReading = false;

				while ( (line = istream.readLine()) != null )
				{
					if ( startReading )
					{
						if ( line.startsWith( " " ) )
						{
							entryList = (List) stashMap.get( currentMember );
							if ( entryList == null )
							{
								entryList = new ArrayList();
								stashMap.put( currentMember, entryList );
							}

							entry = new StashLogEntry( line );
							if ( !entryList.contains( entry ) )
								entryList.add( entry );
						}
						else if ( line.length() > 0 && !line.startsWith( "<" ) )
							currentMember = line.substring( 0, line.length() - 1 );
					}
					else if ( line.equals( "<!-- Begin Stash Log: Do Not Modify Beyond This Point -->" ) );
				}

				istream.close();
			}

			client.updateDisplay( DISABLE_STATE, "Retrieving clan stash log..." );
			(new StashLogRequest( client )).run();
			client.updateDisplay( ENABLE_STATE, "Stash log retrieved." );

			file.delete();
			file.getParentFile().mkdirs();
			file.createNewFile();

			Iterator memberIterator = stashMap.keySet().iterator();
			PrintStream ostream = new PrintStream( new FileOutputStream( file, true ), true );
			Iterator entries;

			ostream.println( "<html><head>" );
			ostream.println( "<title>Clan Stash Log @ " + (new Date()).toString() + "</title>" );
			ostream.println( "<style><!--" );
			ostream.println();
			ostream.println( "\tbody { font-family: Verdana; font-size: 9pt }" );
			ostream.println();
			ostream.println( "\t." + STASH_ADD + " { color: green }" );
			ostream.println( "\t." + STASH_TAKE + " { color: olive }" );
			ostream.println( "\t." + WAR_BATTLE + " { color: orange }" );
			ostream.println( "\t." + CLAN_ACCEPT + " { color: blue }" );
			ostream.println( "\t." + CLAN_LEAVE + " { color: red }" );
			ostream.println( "\t." + CLAN_BOOT + " { color: red }" );
			ostream.println();
			ostream.println( "--></style></head>" );

			ostream.println();
			ostream.println( "<body>" );
			ostream.println();
			ostream.println( "<!-- Begin Stash Log: Do Not Modify Beyond This Point -->" );

			while ( memberIterator.hasNext() )
			{
				currentMember = (String) memberIterator.next();
				ostream.println( currentMember + ":" );

				entryList = (List) stashMap.get( currentMember );
				Collections.sort( entryList );

				entries = entryList.iterator();

				ostream.println( "<ul>" );
				while ( entries.hasNext() )
					ostream.println( entries.next().toString() );
				ostream.println( "</ul>" );

				ostream.println();
			}

			ostream.println( "</body></html>" );
			ostream.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace( System.err );
			throw new RuntimeException( "The file <" + file.getAbsolutePath() + "> could not be opened for writing" );
		}
	}

	private static class StashLogEntry implements Comparable
	{
		private String entryType;
		private Date timestamp;
		private String entry, stringform;

		public StashLogEntry( String entryType, Date timestamp, String entry )
		{
			this.entryType = entryType;
			this.timestamp = timestamp;
			this.entry = entry;

			this.stringform = "\t<li class=\"" + entryType + "\">" + STASH_FORMAT.format( timestamp ) + ": " + entry + "</li>";
		}

		public StashLogEntry( String stringform )
		{
			Matcher entryMatcher = Pattern.compile( "\t<li class=\"(.*?)\">(.*?): (.*?)</li>" ).matcher( stringform );
			entryMatcher.find();

			this.entryType = entryMatcher.group(1);

			try
			{
				this.timestamp = STASH_FORMAT.parse( entryMatcher.group(2) );
			}
			catch ( Exception e )
			{
				this.timestamp = new Date();
			}

			this.entry = entryMatcher.group(3);
			this.stringform = stringform;
		}

		public int compareTo( Object o )
		{
			return o == null || !(o instanceof StashLogEntry) ? -1 : timestamp.before( ((StashLogEntry)o).timestamp ) ? 1 :
				timestamp.after( ((StashLogEntry)o).timestamp ) ? -1 : 0;
		}

		public boolean equals( Object o )
		{
			return o == null || !(o instanceof StashLogEntry) ? false : stringform.equals( o.toString() );
		}

		public String toString()
		{	return stringform;
		}
	}

	private static class StashLogRequest extends KoLRequest
	{
		public StashLogRequest( KoLmafia client )
		{	super( client, "clan_log.php" );
		}

		public void run()
		{
			super.run();

			// First, process all additions to the clan stash.
			// These are designated with the word "added to".

			handleItems( true );

			// Next, process all the removals from the clan stash.
			// These are designated with the word "took from".

			handleItems( false );

			// Next, process all the clan warfare log entries.
			// Though grouping by player isn't very productive,
			// KoLmafia is meant to show a historic history, and
			// showing it by player may prove enlightening.

			handleBattles();

			// Now, handle all of the administrative-related
			// things in the clan.

			handleAdmin( CLAN_ACCEPT, "accepted", " into the clan", "accepted by " );
			handleAdmin( CLAN_LEAVE, "left the clan", "", "left clan" );
			handleAdmin( CLAN_BOOT, "booted", "", "booted by " );
		}

		private void handleItems( boolean parseAdditions )
		{
			String handleType = parseAdditions ? STASH_ADD : STASH_TAKE;

			String regex = parseAdditions ? TIME_REGEX + ": ([^<]*?) added ([\\d,]+) (.*?) to the Goodies Hoard" :
				TIME_REGEX + ": ([^<]*?) took ([\\d,]+) (.*?) from the Goodies Hoard";

			String suffixDescription = parseAdditions ? "added to stash" : "taken from stash";

			int lastItemID;
			int entryCount;

			List entryList;
			String currentMember;

			StashLogEntry entry;
			StringBuffer entryBuffer = new StringBuffer();
			Matcher entryMatcher = Pattern.compile( regex ).matcher( responseText );

			while ( entryMatcher.find() )
			{
				try
				{
					entryBuffer.setLength(0);
					currentMember = entryMatcher.group(2);

					if ( !stashMap.containsKey( currentMember ) )
						stashMap.put( currentMember, new ArrayList() );

					entryList = (List) stashMap.get( currentMember );
					entryCount = df.parse( entryMatcher.group(3) ).intValue();

					lastItemID = TradeableItemDatabase.getItemID( entryMatcher.group(4) );
					entryBuffer.append( (new AdventureResult( lastItemID, entryCount )).toString() );

					entryBuffer.append( " " );
					entryBuffer.append( suffixDescription );

					entry = new StashLogEntry( handleType, STASH_FORMAT.parse( entryMatcher.group(1) ), entryBuffer.toString() );
					if ( !entryList.contains( entry ) )
						entryList.add( entry );
				}
				catch ( Exception e )
				{
					// Should not happen, but catching the exception
					// anyway, just in case it does.

					System.out.println( e );
					e.printStackTrace();
				}
			}
		}

		private void handleBattles()
		{
			List entryList;
			String currentMember;

			StashLogEntry entry;
			Matcher entryMatcher = Pattern.compile( TIME_REGEX + ": ([^<]*?) launched an attack against (.*?)\\.<br>" ).matcher( responseText );

			while ( entryMatcher.find() )
			{
				try
				{
					currentMember = entryMatcher.group(2);
					if ( !stashMap.containsKey( currentMember ) )
						stashMap.put( currentMember, new ArrayList() );

					entryList = (List) stashMap.get( currentMember );
					entry = new StashLogEntry( WAR_BATTLE, STASH_FORMAT.parse( entryMatcher.group(1) ),
						"<i>" + entryMatcher.group(3) + "</i> attacked" );

					if ( !entryList.contains( entry ) )
						entryList.add( entry );
				}
				catch ( Exception e )
				{
					// Should not happen, but catching the exception
					// anyway, just in case it does.

					System.out.println( e );
					e.printStackTrace();
				}
			}
		}

		private void handleAdmin( String entryType, String searchString, String suffixString, String descriptionString )
		{
			String regex = TIME_REGEX + ": ([^<]*?) " + searchString + " (.*?)" + suffixString + "\\.<br>";

			List entryList;
			String currentMember;

			StashLogEntry entry;
			String entryString;
			Matcher entryMatcher = Pattern.compile( regex ).matcher( responseText );

			while ( entryMatcher.find() )
			{
				try
				{
					currentMember = entryMatcher.group( descriptionString.endsWith( " " ) ? 3 : 2 );
					if ( !stashMap.containsKey( currentMember ) )
						stashMap.put( currentMember, new ArrayList() );

					entryList = (List) stashMap.get( currentMember );
					entryString = descriptionString.endsWith( " " ) ? descriptionString + entryMatcher.group(2) : descriptionString;
					entry = new StashLogEntry( entryType, STASH_FORMAT.parse( entryMatcher.group(1) ), entryString );

					if ( !entryList.contains( entry ) )
						entryList.add( entry );
				}
				catch ( Exception e )
				{
					// Should not happen, but catching the exception
					// anyway, just in case it does.

					System.out.println( e );
					e.printStackTrace();
				}
			}
		}
	}

	public static void postMessage()
	{
		Object [] parameters = new Object[3];
		parameters[0] = client;
		parameters[1] = "Clan Board Post";
		parameters[2] = new MessagePostPanel( "post" );

		SwingUtilities.invokeLater( new CreateFrameRunnable( KoLPanelFrame.class, parameters ) );
	}

	public static void postAnnouncement()
	{
		Object [] parameters = new Object[3];
		parameters[0] = client;
		parameters[1] = "Clan Announcement";
		parameters[2] = new MessagePostPanel( "postannounce" );

		SwingUtilities.invokeLater( new CreateFrameRunnable( KoLPanelFrame.class, parameters ) );
	}

	private static class MessagePostPanel extends KoLPanel
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

	public static void getAnnouncements()
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

	public static void getMessageBoard()
	{
		Object [] parameters = new Object[3];
		parameters[0] = client;
		parameters[1] = "Clan Message Board";
		parameters[2] = new ClanMessageRequest( client, "clan_board.php" );

		SwingUtilities.invokeLater( new CreateFrameRunnable( RequestFrame.class, parameters ) );
	}

	private static class ClanMessageRequest extends KoLRequest
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

	public static String retrieveClanListAsCDL()
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

	public static void applyFilter( int matchType, int filterType, String filter )
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

				retrieveMemberData( true, false );
				break;
		}

		ClanSnapshotTable.applyFilter( matchType, filterType, filter );
	}
}