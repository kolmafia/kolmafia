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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.PrintStream;

import java.util.Date;
import java.util.Collections;
import java.text.SimpleDateFormat;

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
	private static final Pattern WAR_PATTERN = Pattern.compile( TIME_REGEX + ": ([^<]*?) launched an attack against (.*?)\\.<br>" );
	private static final Pattern LOGENTRY_PATTERN = Pattern.compile( "\t<li class=\"(.*?)\">(.*?): (.*?)</li>" );

	private static final Pattern RANK_PATTERN = Pattern.compile( "<select name=level.*?</select>" );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option.*?>(.*?)</option>" );

	private static final SimpleDateFormat STASH_FORMAT = new SimpleDateFormat( "MM/dd/yy, hh:mma", Locale.US );
	private static final SimpleDateFormat DIRECTORY_FORMAT = new SimpleDateFormat( "yyyyMM_'w'W", Locale.US );

	private static String SNAPSHOT_DIRECTORY = "clan/";

	private static String clanID;
	private static String clanName;

	private static boolean stashRetrieved = false;
	private static boolean ranksRetrieved = false;
	private static Map profileMap = ClanSnapshotTable.getProfileMap();
	private static Map ascensionMap = AscensionSnapshotTable.getAscensionMap();
	private static Map stashMap = new TreeMap();
	private static List battleList = new ArrayList();

	private static LockableListModel rankList = new LockableListModel();
	private static SortedListModel stashContents = new SortedListModel();

	public static void reset()
	{
		ClanSnapshotTable.reset();
		AscensionSnapshotTable.reset();

		stashRetrieved = false;
		ranksRetrieved = false;

		profileMap.clear();
		ascensionMap.clear();
		stashMap.clear();
		battleList.clear();
		rankList.clear();
		stashContents.clear();
	}

	public static void setStashRetrieved()
	{	stashRetrieved = true;
	}

	public static boolean isStashRetrieved()
	{	return stashRetrieved;
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
			(new RankListRequest()).run();
			ranksRetrieved = true;
		}

		return rankList;
	}

	private static class RankListRequest extends KoLRequest
	{
		public RankListRequest()
		{	super( "clan_members.php" );
		}

		public void run()
		{
			KoLmafia.updateDisplay( "Retrieving list of ranks..." );
			super.run();

			rankList.clear();
			Matcher ranklistMatcher = RANK_PATTERN.matcher( responseText );

			if ( ranklistMatcher.find() )
			{
				Matcher rankMatcher = OPTION_PATTERN.matcher( ranklistMatcher.group() );

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
			ClanMembersRequest cmr = new ClanMembersRequest();
			cmr.run();

			clanID = cmr.getClanID();
			clanName = cmr.getClanName();

			SNAPSHOT_DIRECTORY = "clan/" + clanID + "/" + DIRECTORY_FORMAT.format( new Date() ) + "/";
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

			profile = new File( SNAPSHOT_DIRECTORY + "profiles/" + KoLmafia.getPlayerID( names[i] ) + ".htm" );
			ascensionData = new File( SNAPSHOT_DIRECTORY + "ascensions/" + KoLmafia.getPlayerID( names[i] ) + ".htm" );

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
		File profile = new File( SNAPSHOT_DIRECTORY + "profiles/" + KoLmafia.getPlayerID( name ) + ".htm" );

		if ( profile.exists() )
		{
			// In the event that the profile has already been retrieved,
			// then load the data from disk.

			try
			{
				BufferedReader istream = KoLDatabase.getReader( profile );
				StringBuffer profileString = new StringBuffer();
				String currentLine;

				while ( (currentLine = istream.readLine()) != null )
				{
					profileString.append( currentLine );
					profileString.append( LINE_BREAK );
				}

				profileMap.put( name, profileString.toString() );
				istream.close();
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				printStackTrace( e, "Failed to load cached profile" );
				return;
			}
		}
		else
		{
			// Otherwise, run the request and pull the data from the
			// web server.

			ProfileRequest request = new ProfileRequest( name );
			request.initialize();

			String data = LINE_BREAK_PATTERN.matcher( COMMENT_PATTERN.matcher( STYLE_PATTERN.matcher( SCRIPT_PATTERN.matcher(
				request.responseText ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" );

			profileMap.put( name, data );

			// To avoid retrieving the file again, store the intermediate
			// result in a local file.

			try
			{
				profile.getParentFile().mkdirs();
				PrintStream ostream = new LogStream( profile );
				ostream.println( data );
				ostream.close();
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				printStackTrace( e, "Failed to load cached profile" );
				return;
			}

		}
	}

	private static void initializeAscensionData( String name )
	{
		File ascension = new File( SNAPSHOT_DIRECTORY + "ascensions/" + KoLmafia.getPlayerID( name ) + ".htm" );

		if ( ascension.exists() )
		{
			// In the event that the ascension has already been retrieved,
			// then load the data from disk.

			try
			{
				BufferedReader istream = KoLDatabase.getReader( ascension );
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

				printStackTrace( e, "Failed to load cached ascension history" );
				return;
			}
		}
		else
		{
			// Otherwise, run the request and pull the data from the
			// web server.

			AscensionDataRequest request = new AscensionDataRequest( name, KoLmafia.getPlayerID( name ) );
			request.initialize();

			String data = LINE_BREAK_PATTERN.matcher( COMMENT_PATTERN.matcher( STYLE_PATTERN.matcher( SCRIPT_PATTERN.matcher(
				request.responseText ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" );

			ascensionMap.put( name, data );

			// To avoid retrieving the file again, store the intermediate
			// result in a local file.

			try
			{
				ascension.getParentFile().mkdirs();
				PrintStream ostream = new LogStream( ascension );
				ostream.println( request.responseText );
				ostream.close();
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				printStackTrace( e, "Failed to load cached ascension history" );
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

	public static void takeSnapshot( int mostAscensionsBoardSize, int mainBoardSize, int classBoardSize, int maxAge, boolean playerMoreThanOnce, boolean localProfileLink )
	{
		retrieveClanData();

		File standardFile = new File( SNAPSHOT_DIRECTORY + "standard.htm" );
		File softcoreFile = new File( SNAPSHOT_DIRECTORY + "softcore.htm" );
		File hardcoreFile = new File( SNAPSHOT_DIRECTORY + "hardcore.htm" );
		File sortingScript = new File( SNAPSHOT_DIRECTORY + "sorttable.js" );

		String header = getProperty( "clanRosterHeader" );

		KoLRequest.delay( 1000 );
		standardFile.getParentFile().mkdirs();

		if ( standardFile != null && standardFile.exists() )
			standardFile.delete();
		if ( softcoreFile != null && softcoreFile.exists() )
			softcoreFile.delete();
		if ( hardcoreFile != null && hardcoreFile.exists() )
			hardcoreFile.delete();

		// If initialization was unsuccessful, then there isn't
		// enough data to create a clan ClanSnapshotTable.

		if ( !retrieveMemberData( true, true ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Initialization failed." );
			return;
		}

		// Now, store the clan snapshot into the appropriate
		// data folder.

		try
		{
			PrintStream ostream;

			KoLmafia.updateDisplay( "Storing clan snapshot..." );

			ostream = new LogStream( standardFile );
			ostream.println( ClanSnapshotTable.getStandardData( localProfileLink ) );
			ostream.close();

			String line;
			BufferedReader script = DataUtilities.getReader( "html", "sorttable.js" );
			ostream = new LogStream( sortingScript );

			while ( (line = script.readLine()) != null )
				ostream.println( line );

			ostream.close();

			KoLmafia.updateDisplay( "Storing ascension snapshot..." );

			ostream = new LogStream( softcoreFile );
			ostream.println( AscensionSnapshotTable.getAscensionData( true, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink ) );
			ostream.close();

			ostream = new LogStream( hardcoreFile );
			ostream.println( AscensionSnapshotTable.getAscensionData( false, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink ) );
			ostream.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e, "Clan snapshot generation failed" );
			return;
		}

		KoLmafia.updateDisplay( "Snapshot generation completed." );

		try
		{
			// To make things less confusing, load the summary
			// file inside of the default browser after completion.

			BrowserLauncher.openURL( standardFile.getAbsolutePath() );
			BrowserLauncher.openURL( softcoreFile.getAbsolutePath() );
			BrowserLauncher.openURL( hardcoreFile.getAbsolutePath() );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e, "Clan snapshot generation failed" );
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
		File file = new File( "clan/" + clanID + "/stashlog.htm" );

		try
		{
			List entryList = null;
			StashLogEntry entry = null;

			if ( file.exists() )
			{
				String currentMember = "";
				BufferedReader istream = KoLDatabase.getReader( file );
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
			(new StashLogRequest()).run();
			KoLmafia.updateDisplay( "Stash log retrieved." );

			file.getParentFile().mkdirs();
			file.createNewFile();

			String [] members = new String[ stashMap.keySet().size() ];
			stashMap.keySet().toArray( members );

			PrintStream ostream = new LogStream( file );
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

			printStackTrace( e, "Error opening <" + file.getAbsolutePath() + "> for output" );
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
			Matcher entryMatcher = LOGENTRY_PATTERN.matcher( stringform );
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

				printStackTrace( e );
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
		public StashLogRequest()
		{	super( "clan_log.php" );
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

			String regex = parseAdditions ? TIME_REGEX + ": ([^<]*?) added ([\\d,]+) (.*?)\\.<br>" :
				TIME_REGEX + ": ([^<]*?) took ([\\d,]+) (.*?)\\.<br>";

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
					entryCount = parseInt( entryMatcher.group(3) );

					lastItemID = TradeableItemDatabase.getItemID( entryMatcher.group(4), entryCount );
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

					printStackTrace( e );
				}
			}

			responseText = entryMatcher.replaceAll( "" );
		}

		private void handleBattles()
		{
			List entryList;
			String currentMember;

			StashLogEntry entry;
			Matcher entryMatcher = WAR_PATTERN.matcher( responseText );

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

					printStackTrace( e );
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

					printStackTrace( e );
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
