/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.ejalbert.BrowserLauncher;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class ClanManager extends StaticEntity
{
	private static String SNAPSHOT_DIRECTORY = "clan/";

	private static String clanId;
	private static String clanName;

	private static boolean stashRetrieved = false;
	private static boolean ranksRetrieved = false;
	private static Map profileMap = ClanSnapshotTable.getProfileMap();
	private static Map ascensionMap = AscensionSnapshotTable.getAscensionMap();
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

	public static String getClanId()
	{	return clanId;
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
			RequestThread.postRequest( new ClanRankListRequest( rankList ) );
			ranksRetrieved = true;
		}

		return rankList;
	}

	private static void retrieveClanData()
	{
		if ( KoLmafia.isAdventuring() )
			return;

		if ( profileMap.isEmpty() )
		{
			ClanMembersRequest cmr = new ClanMembersRequest();
			RequestThread.postRequest( cmr );

			clanId = cmr.getClanId();
			clanName = cmr.getClanName();

			SNAPSHOT_DIRECTORY = "clan/" + clanId + "/" + WEEKLY_FORMAT.format( new Date() ) + "/";
			KoLmafia.updateDisplay( "Clan data retrieved." );
		}
	}

	private static boolean retrieveMemberData( boolean retrieveProfileData, boolean retrieveAscensionData )
	{
		// First, determine how many member profiles need to be retrieved
		// before this happens.

		int requestsNeeded = 0;

		String filename;
		File profile, ascensionData;
		String currentProfile, currentAscensionData;

		String [] names = new String[ profileMap.size() ];
		profileMap.keySet().toArray( names );

		for ( int i = 0; i < names.length; ++i )
		{
			currentProfile = (String) profileMap.get( names[i] );
			currentAscensionData = (String) ascensionMap.get( names[i] );

			filename = getFileName( names[i] );
			profile = new File( SNAPSHOT_DIRECTORY + "profiles/" + filename );
			ascensionData = new File( SNAPSHOT_DIRECTORY + "ascensions/" + filename );

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

	public static String getURLName( String name )
	{	return KoLCharacter.baseUserName( name ) + "_(%23" + KoLmafia.getPlayerId( name ) + ")" + ".htm";
	}

	public static String getFileName( String name )
	{	return KoLCharacter.baseUserName( name ) + "_(#" + KoLmafia.getPlayerId( name ) + ")" + ".htm";
	}

	private static void initializeProfile( String name )
	{
		File profile = new File( SNAPSHOT_DIRECTORY + "profiles/" + getFileName( name ) );

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

				profileMap.put( name.toLowerCase(), profileString.toString() );
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
				request.responseText ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" ).replaceAll(
					"ascensionhistory.php\\?back=other&who=" + KoLmafia.getPlayerId( name ), "../ascensions/" + getURLName( name ) );

			profileMap.put( name, data );

			// To avoid retrieving the file again, store the intermediate
			// result in a local file.

			PrintStream ostream = LogStream.openStream( profile, true );
			ostream.println( data );
			ostream.close();
		}
	}

	private static void initializeAscensionData( String name )
	{
		File ascension = new File( SNAPSHOT_DIRECTORY + "ascensions/" + getFileName( name ) );

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

			AscensionDataRequest request = new AscensionDataRequest( name, KoLmafia.getPlayerId( name ) );
			request.initialize();

			String data = LINE_BREAK_PATTERN.matcher( COMMENT_PATTERN.matcher( STYLE_PATTERN.matcher( SCRIPT_PATTERN.matcher(
				request.responseText ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" ) ).replaceAll( "" ).replaceAll(
				"<a href=\"charsheet.php\">", "<a href=../profiles/" + getURLName( name ) );

			ascensionMap.put( name, data );

			// To avoid retrieving the file again, store the intermediate
			// result in a local file.

			PrintStream ostream = LogStream.openStream( ascension, true );
			ostream.println( data );
			ostream.close();
		}
	}

	public static void registerMember( String playerName, String level )
	{
		ClanSnapshotTable.registerMember( playerName, level );
		AscensionSnapshotTable.registerMember( playerName );
	}

	public static void unregisterMember( String playerId )
	{
		ClanSnapshotTable.unregisterMember( playerId );
		AscensionSnapshotTable.registerMember( playerId );
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

		// If initialization was unsuccessful, then there isn't
		// enough data to create a clan ClanSnapshotTable.

		if ( !retrieveMemberData( true, true ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Initialization failed." );
			return;
		}

		// Now, store the clan snapshot into the appropriate
		// data folder.

		KoLmafia.updateDisplay( "Storing clan snapshot..." );

		PrintStream ostream = LogStream.openStream( standardFile, true );
		ostream.println( ClanSnapshotTable.getStandardData( localProfileLink ) );
		ostream.close();

		String line;
		BufferedReader script = DataUtilities.getReader( "html", "sorttable.js" );

		try
		{
			ostream = LogStream.openStream( sortingScript, true );
			while ( (line = script.readLine()) != null )
				ostream.println( line );

			ostream.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		KoLmafia.updateDisplay( "Storing ascension snapshot..." );

		ostream = LogStream.openStream( softcoreFile, true );
		ostream.println( AscensionSnapshotTable.getAscensionData( true, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink ) );
		ostream.close();

		ostream = LogStream.openStream( hardcoreFile, true );
		ostream.println( AscensionSnapshotTable.getAscensionData( false, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink ) );
		ostream.close();

		KoLmafia.updateDisplay( "Snapshot generation completed." );

		// To make things less confusing, load the summary
		// file inside of the default browser after completion.

		BrowserLauncher.openURL( standardFile.getAbsolutePath() );
		BrowserLauncher.openURL( softcoreFile.getAbsolutePath() );
		BrowserLauncher.openURL( hardcoreFile.getAbsolutePath() );
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
		RequestThread.postRequest( new ClanStashLogRequest() );
	}

	public static boolean isMember( String memberName )
	{
		retrieveClanData();
		Iterator it = profileMap.keySet().iterator();

		while ( it.hasNext() )
			if ( memberName.equalsIgnoreCase( (String) it.next() ) )
				return true;

		return false;
	}

	/**
	 * Retrieves the clan membership in the form of a
	 * list object.
	 */

	public static String [] retrieveClanList()
	{
		retrieveClanData();

		String [] members = new String[ profileMap.size() ];
		profileMap.keySet().toArray( members );

		return members;
	}

	/**
	 * Retrieves the clan membership in the form of a
	 * CDL (comma-delimited list)
	 */

	public static String retrieveClanListAsCDL()
	{
		String [] members = retrieveClanList();
		StringBuffer clanCDL = new StringBuffer();

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
