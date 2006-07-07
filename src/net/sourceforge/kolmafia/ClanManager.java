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
import java.util.Map;
import java.util.TreeMap;
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
import javax.swing.JOptionPane;

import edu.stanford.ejalbert.BrowserLauncher;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanManager extends StaticEntity
{
	private static final String STASH_ADD = "add";
	private static final String STASH_TAKE = "take";
	private static final String WAR_BATTLE = "warfare";

	private static final String CLAN_WHITELIST = "whitelist";
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
	private static SortedListModel stashContents = new SortedListModel( AdventureResult.class );

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

	public static SortedListModel getStash()
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
			KoLmafia.updateDisplay( "Retrieving list of ranks..." );
			super.run();

			rankList.clear();
			Matcher ranklistMatcher = Pattern.compile( "<select name=level.*?</select>" ).matcher( responseText );

			if ( ranklistMatcher.find() )
			{
				Matcher rankMatcher = Pattern.compile( "<option.*?>(.*?)</option>" ).matcher( ranklistMatcher.group() );

				while ( rankMatcher.find() )
					rankList.add( rankMatcher.group(1).toLowerCase() );
			}

			KoLmafia.updateDisplay( "List of ranks retrieved." );
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
			KoLmafia.updateDisplay( "Clan data retrieved." );
		}
	}

	private static boolean retrieveMemberData( boolean retrieveProfileData, boolean retrieveAscensionData )
	{
		// First, determine how many member profiles need to be retrieved
		// before this happens.

		int requestsNeeded = 0;
		File profile, ascensionData;
		String currentProfile, currentAscensionData;

		String [] names = new String[ profileMap.keySet().size() ];
		profileMap.keySet().toArray( names );

		for ( int i = 0; i < names.length; ++i )
		{
			currentProfile = (String) profileMap.get( names[i] );
			currentAscensionData = (String) ascensionMap.get( names[i] );

			profile = new File( SNAPSHOT_DIRECTORY + "profiles" + File.separator + KoLmafia.getPlayerID( names[i] ) + ".htm" );
			ascensionData = new File( SNAPSHOT_DIRECTORY + "ascensions" + File.separator + KoLmafia.getPlayerID( names[i] ) + ".htm" );

			if ( retrieveProfileData )
			{
				if ( currentProfile.equals( "" ) && !profile.exists() )
					++requestsNeeded;

				if ( currentProfile.equals( "" ) && profile.exists() )
					initializeProfile( names[i] );
			}

			if ( retrieveAscensionData )
			{
				if ( currentAscensionData.equals( "" ) && !ascensionData.exists() )
					++requestsNeeded;

				if ( currentAscensionData.equals( "" ) && ascensionData.exists() )
					initializeAscensionData( names[i] );
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

		KoLmafia.updateDisplay( "Processing request..." );

		// Create a special HTML file for each of the
		// players in the ClanSnapshotTable so that it can be
		// navigated at leisure.

		for ( int i = 0; i < names.length && KoLmafia.permitsContinue(); ++i )
		{
			KoLmafia.updateDisplay( "Examining member " + (i+1) + " of " + names.length + "..." );

			currentProfile = (String) profileMap.get( names[i] );
			currentAscensionData = (String) ascensionMap.get( names[i] );

			if ( retrieveProfileData && currentProfile.equals( "" ) )
				initializeProfile( names[i] );

			if ( retrieveAscensionData && currentAscensionData.equals( "" ) )
				initializeAscensionData( names[i] );
		}

		return true;
	}

	private static void initializeProfile( String name )
	{
		File profile = new File( SNAPSHOT_DIRECTORY + "profiles" + File.separator + KoLmafia.getPlayerID( name ) + ".htm" );

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
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, "Failed to load cached profile" );
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
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, "Failed to load cached profile" );
				return;
			}

		}
	}

	private static void initializeAscensionData( String name )
	{
		File ascension = new File( SNAPSHOT_DIRECTORY + "ascensions" + File.separator + KoLmafia.getPlayerID( name ) + ".htm" );

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
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, "Failed to load cached ascension history" );
				return;
			}
		}
		else
		{
			// Otherwise, run the request and pull the data from the
			// web server.

			AscensionDataRequest request = new AscensionDataRequest( client, name, KoLmafia.getPlayerID( name ) );
			request.initialize();
			ascensionMap.put( name, request.responseText );

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
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, "Failed to load cached ascension history" );
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
	 * Takes a ClanSnapshotTable of clan member data for this clan.  The user will
	 * be prompted for the data they would like to include in this ClanSnapshotTable,
	 * including complete player profiles, favorite food, and any other
	 * data gathered by KoLmafia.  If the clan member list was not previously
	 * initialized, this method will also initialize that list.
	 */

	public static void takeSnapshot( int mostAscensionsBoardSize, int mainBoardSize, int classBoardSize, int maxAge, boolean playerMoreThanOnce )
	{
		retrieveClanData();

		// If the file already exists, a ClanSnapshotTable cannot be taken.
		// Therefore, notify the user of this. :)

		File standardFile;
		File softcoreFile;
		File hardcoreFile;
		File sortingScript;


		String header = getProperty( "clanRosterHeader" ).toString();

		boolean generateSnapshot = !header.equals( "" ) && !header.equals( "<td>Ascensions</td>" );



		if( mostAscensionsBoardSize == 0 && mainBoardSize == 0 && classBoardSize == 0 && maxAge == 0 && playerMoreThanOnce == false)
		{
			standardFile = new File( SNAPSHOT_DIRECTORY + "standard.htm" );
			softcoreFile = new File( SNAPSHOT_DIRECTORY + "softcore.htm" );
			hardcoreFile = new File( SNAPSHOT_DIRECTORY + "hardcore.htm" );
			sortingScript = new File( SNAPSHOT_DIRECTORY + "sorttable.js" );

			if (standardFile.exists() || softcoreFile.exists() || hardcoreFile.exists() )
			{
				JOptionPane.showMessageDialog( null, "You already created a snapshot this week." );
				return;
			}

		}
		else
		{
			standardFile = new File( SNAPSHOT_DIRECTORY + "standard.htm" );
			if( playerMoreThanOnce)
			{
				softcoreFile = new File( SNAPSHOT_DIRECTORY + "s" + mostAscensionsBoardSize + "-" + mainBoardSize + "-" + classBoardSize + "-" + maxAge + "y.htm" );
				hardcoreFile = new File( SNAPSHOT_DIRECTORY + "h" + mostAscensionsBoardSize + "-" + mainBoardSize + "-" + classBoardSize + "-" + maxAge + "y.htm" );
			}
			else
			{
				softcoreFile = new File( SNAPSHOT_DIRECTORY + "s" + mostAscensionsBoardSize + "-" + mainBoardSize + "-" + classBoardSize + "-" + maxAge + "n.htm" );
				hardcoreFile = new File( SNAPSHOT_DIRECTORY + "h" + mostAscensionsBoardSize + "-" + mainBoardSize + "-" + classBoardSize + "-" + maxAge + "n.htm" );
			}
			sortingScript = new File( SNAPSHOT_DIRECTORY + "sorttable.js" );

			if (softcoreFile.exists() || hardcoreFile.exists() )
			{
				JOptionPane.showMessageDialog( null, "You already created such a snapshot this week." );
				return;
			}
			generateSnapshot = false;

		}

		// If initialization was unsuccessful, then there isn't
		// enough data to create a clan ClanSnapshotTable.

		boolean retrieveProfileData = header.indexOf( "<td>PVP</td>" ) != -1 || header.indexOf( "<td>Path</td>" ) != -1 || header.indexOf( "<td>Class</td>" ) != -1 ||
			header.indexOf( "<td>Meat</td>" ) != -1 || header.indexOf( "<td>Food</td>" ) != -1 || header.indexOf( "<td>Last Login</td>" ) != -1;

		boolean retrieveAscensionData = header.indexOf( "<td>Ascensions</td>" ) != -1;

		if ( !retrieveMemberData( retrieveProfileData, retrieveAscensionData ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Initialization failed." );
			return;
		}
		if( standardFile != null)
			standardFile.getParentFile().mkdirs();


		// Now, store the clan snapshot into the appropriate
		// data folder.

		try
		{
			PrintStream ostream;

			if ( generateSnapshot )
			{
				KoLmafia.updateDisplay( "Storing clan snapshot..." );

				ostream = new PrintStream( new FileOutputStream( standardFile, true ), true );
				ostream.println( ClanSnapshotTable.getStandardData() );
				ostream.close();

				String line;
				BufferedReader script = DataUtilities.getReader( "html", "sorttable.js" );
				ostream = new PrintStream( new FileOutputStream( sortingScript, true ), true );

				while ( (line = script.readLine()) != null )
					ostream.println( line );

				ostream.close();
			}

			if ( retrieveAscensionData )
			{
				KoLmafia.updateDisplay( "Storing ascension snapshot..." );

				ostream = new PrintStream( new FileOutputStream( softcoreFile, true ), true );
				ostream.println( AscensionSnapshotTable.getAscensionData( true, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce ) );
				ostream.close();

				ostream = new PrintStream( new FileOutputStream( hardcoreFile, true ), true );
				ostream.println( AscensionSnapshotTable.getAscensionData( false, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce ) );
				ostream.close();
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Clan snapshot generation failed" );
			return;
		}

		KoLmafia.updateDisplay( "Snapshot generation completed." );

		try
		{
			// To make things less confusing, load the summary
			// file inside of the default browser after completion.

			if ( generateSnapshot )
				BrowserLauncher.openURL( standardFile.toURL().toString() );

			if ( retrieveAscensionData )
			{
				BrowserLauncher.openURL( softcoreFile.toURL().toString() );
				BrowserLauncher.openURL( hardcoreFile.toURL().toString() );
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Clan snapshot generation failed" );
			return;
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
			List entryList = null;
			StashLogEntry entry = null;

			if ( file.exists() )
			{
				String currentMember = "";
				BufferedReader istream = new BufferedReader( new InputStreamReader( new FileInputStream( file ) ) );
				String line;

				boolean startReading = false;

				while ( (line = istream.readLine()) != null )
				{
					if ( startReading )
					{
						if ( line.startsWith( " " ) )
						{
							currentMember = line.substring( 1, line.length() - 1 );
							entryList = (List) stashMap.get( currentMember );
							if ( entryList == null )
							{
								entryList = new ArrayList();
								stashMap.put( currentMember, entryList );
							}
						}
						else if ( line.length() > 0 && !line.startsWith( "<" ) )
						{
							entry = new StashLogEntry( line );
							if ( !entryList.contains( entry ) )
								entryList.add( entry );
						}
					}
					else if ( line.equals( "<!-- Begin Stash Log: Do Not Modify Beyond This Point -->" ) )
						startReading = true;
				}

				istream.close();
				file.delete();
			}

			KoLmafia.updateDisplay( "Retrieving clan stash log..." );
			(new StashLogRequest( client )).run();
			KoLmafia.updateDisplay( "Stash log retrieved." );

			file.getParentFile().mkdirs();
			file.createNewFile();

			String [] members = new String[ stashMap.keySet().size() ];
			stashMap.keySet().toArray( members );

			PrintStream ostream = new PrintStream( new FileOutputStream( file, true ), true );
			Object [] entries;

			ostream.println( "<html><head>" );
			ostream.println( "<title>Clan Stash Log @ " + (new Date()).toString() + "</title>" );
			ostream.println( "<style><!--" );
			ostream.println();
			ostream.println( "\tbody { font-family: Verdana; font-size: 9pt }" );
			ostream.println();
			ostream.println( "\t." + STASH_ADD + " { color: green }" );
			ostream.println( "\t." + STASH_TAKE + " { color: olive }" );
			ostream.println( "\t." + WAR_BATTLE + " { color: orange }" );
			ostream.println( "\t." + CLAN_WHITELIST + " { color: blue }" );
			ostream.println( "\t." + CLAN_ACCEPT + " { color: blue }" );
			ostream.println( "\t." + CLAN_LEAVE + " { color: red }" );
			ostream.println( "\t." + CLAN_BOOT + " { color: red }" );
			ostream.println();
			ostream.println( "--></style></head>" );

			ostream.println();
			ostream.println( "<body>" );
			ostream.println();
			ostream.println( "<!-- Begin Stash Log: Do Not Modify Beyond This Point -->" );

			for ( int i = 0; i < members.length; ++i )
			{
				ostream.println( " " + members[i] + ":" );

				entryList = (List) stashMap.get( members[i] );
				Collections.sort( entryList );
				entries = entryList.toArray();

				ostream.println( "<ul>" );
				for ( int j = 0; j < entries.length; ++j )
					ostream.println( entries[j].toString() );
				ostream.println( "</ul>" );

				ostream.println();
			}

			ostream.println( "</body></html>" );
			ostream.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error opening <" + file.getAbsolutePath() + "> for output" );
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
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
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

			handleAdmin( CLAN_WHITELIST, "was accepted into the clan \\(whitelist\\)", "", "auto-accepted through whitelist" );
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
					currentMember = entryMatcher.group(2).trim();

					if ( !stashMap.containsKey( currentMember ) )
						stashMap.put( currentMember, new ArrayList() );

					entryList = (List) stashMap.get( currentMember );
					entryCount = StaticEntity.parseInt( entryMatcher.group(3) );

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
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e );
				}
			}

			responseText = entryMatcher.replaceAll( "" );
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
					currentMember = entryMatcher.group(2).trim();
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
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e );
				}
			}

			responseText = entryMatcher.replaceAll( "" );
		}

		private void handleAdmin( String entryType, String searchString, String suffixString, String descriptionString )
		{
			String regex = TIME_REGEX + ": ([^<]*?) " + searchString + "(.*?)" + suffixString + "\\.?<br>";

			List entryList;
			String currentMember;

			StashLogEntry entry;
			String entryString;
			Matcher entryMatcher = Pattern.compile( regex ).matcher( responseText );

			while ( entryMatcher.find() )
			{
				try
				{
					currentMember = entryMatcher.group( descriptionString.endsWith( " " ) ? 3 : 2 ).trim();
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
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.

					StaticEntity.printStackTrace( e );
				}
			}

			responseText = entryMatcher.replaceAll( "" );
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
		String [] members = new String[ profileMap.keySet().size() ];
		profileMap.keySet().toArray( members );

		if ( members.length > 0 )
			clanCDL.append( members[0] );

		for ( int i = 1; i < members.length; ++i )
		{
			clanCDL.append( ", " );
			clanCDL.append( members[i] );
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
			case ClanSnapshotTable.LEVEL_FILTER:
			case ClanSnapshotTable.KARMA_FILTER:

				break;

			default:

				retrieveMemberData( true, false );
				break;
		}

		ClanSnapshotTable.applyFilter( matchType, filterType, filter );
	}
}
