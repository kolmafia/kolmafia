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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.ejalbert.BrowserLauncher;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class ClanManager extends StaticEntity
{
	private static final Pattern WHITELIST_PATTERN = Pattern.compile( "<b>([^<]+)</b> \\(#(\\d+)\\)" );

	private static String snapshotFolder = "clan/";
	private static String clanId = null;
	private static String clanName = null;
	private static boolean stashRetrieved = false;
	private static boolean ranksRetrieved = false;

	private static final ArrayList currentMembers = new ArrayList();
	private static final ArrayList whiteListMembers = new ArrayList();

	private static final Map profileMap = ClanSnapshotTable.getProfileMap();
	private static final Map ascensionMap = AscensionSnapshotTable.getAscensionMap();
	private static final Map titleMap = new TreeMap();

	private static final List battleList = new ArrayList();

	private static final LockableListModel rankList = new LockableListModel();
	private static final SortedListModel stashContents = new SortedListModel();

	public static final void clearCache()
	{
		ClanSnapshotTable.clearCache();
		AscensionSnapshotTable.clearCache();

		stashRetrieved = false;
		ranksRetrieved = false;

		profileMap.clear();
		ascensionMap.clear();
		battleList.clear();
		rankList.clear();
		stashContents.clear();

		clanId = null;
		clanName = null;
	}

	public static final void setStashRetrieved()
	{	stashRetrieved = true;
	}

	public static final boolean isStashRetrieved()
	{	return stashRetrieved;
	}

	public static final String getClanId()
	{
		retrieveClanId();
		return clanId;
	}

	public static final String getClanName()
	{
		retrieveClanId();
		return clanName;
	}

	public static final SortedListModel getStash()
	{	return stashContents;
	}

	public static final LockableListModel getRankList()
	{
		if ( !ranksRetrieved )
		{
			RequestThread.postRequest( new ClanRankListRequest( rankList ) );
			ranksRetrieved = true;
		}

		return rankList;
	}

	private static final void retrieveClanData()
	{
		if ( KoLmafia.isAdventuring() )
			return;

		if ( !profileMap.isEmpty() )
			return;

		retrieveClanId();
		snapshotFolder = "clan/" + clanId + "/" + WEEKLY_FORMAT.format( new Date() ) + "/";
		KoLmafia.updateDisplay( "Clan data retrieved." );

		KoLRequest whiteListFinder = new KoLRequest( "clan_whitelist.php" );
		whiteListFinder.run();

		String currentName;
		Matcher whiteListMatcher = WHITELIST_PATTERN.matcher( whiteListFinder.responseText );
		while ( whiteListMatcher.find() )
		{
			currentName = whiteListMatcher.group(1);
			KoLmafia.registerPlayer( currentName, whiteListMatcher.group(2) );

			currentName = currentName.toLowerCase();
			if ( !currentMembers.contains( currentName ) )
				whiteListMembers.add( currentName );
		}

		Collections.sort( currentMembers );
		Collections.sort( whiteListMembers );
	}

	public static final void resetClanId()
	{
		clanId = null;
		clanName = null;
	}

	private static final void retrieveClanId()
	{
		if ( clanId != null )
			return;

		ClanMembersRequest cmr = new ClanMembersRequest();
		RequestThread.postRequest( cmr );

		clanId = cmr.getClanId();
		clanName = cmr.getClanName();
	}

	private static final boolean retrieveMemberData( boolean retrieveProfileData, boolean retrieveAscensionData )
	{
		// First, determine how many member profiles need to be retrieved
		// before this happens.

		int requestsNeeded = 0;

		String filename;
		File profile, ascensionData;
		String currentProfile, currentAscensionData;

		String [] names = new String[ profileMap.size() ];
		profileMap.keySet().toArray( names );

		RequestThread.openRequestSequence();

		for ( int i = 0; i < names.length; ++i )
		{
			KoLmafia.updateDisplay( "Cache data lookup for member " + (i+1) + " of " + names.length + "..." );

			currentProfile = (String) profileMap.get( names[i] );
			currentAscensionData = (String) ascensionMap.get( names[i] );

			filename = getFileName( names[i] );
			profile = new File( ROOT_LOCATION, snapshotFolder + "profiles/" + filename );
			ascensionData = new File( ROOT_LOCATION, snapshotFolder + "ascensions/" + filename );

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

		RequestThread.closeRequestSequence();

		// If all the member profiles have already been retrieved, then
		// you won't need to look up any profiles, so it takes no time.
		// No need to confirm with the user.  Therefore, return.

		if ( requestsNeeded == 0 )
			return true;

		// Now that it's known what the user wishes to continue,
		// you begin initializing all the data.

		RequestThread.openRequestSequence();

		// Create a special HTML file for each of the
		// players in the ClanSnapshotTable so that it can be
		// navigated at leisure.

		for ( int i = 0; i < names.length && KoLmafia.permitsContinue(); ++i )
		{
			KoLmafia.updateDisplay( "Loading profile for member " + (i+1) + " of " + names.length + "..." );

			currentProfile = (String) profileMap.get( names[i] );
			currentAscensionData = (String) ascensionMap.get( names[i] );

			if ( retrieveProfileData && currentProfile.equals( "" ) )
				initializeProfile( names[i] );

			if ( retrieveAscensionData && currentAscensionData.equals( "" ) )
				initializeAscensionData( names[i] );
		}

		RequestThread.closeRequestSequence();
		return true;
	}

	public static final String getURLName( String name )
	{	return KoLSettings.baseUserName( name ) + "_(%23" + KoLmafia.getPlayerId( name ) + ")" + ".htm";
	}

	public static final String getFileName( String name )
	{	return KoLSettings.baseUserName( name ) + "_(#" + KoLmafia.getPlayerId( name ) + ")" + ".htm";
	}

	private static final void initializeProfile( String name )
	{
		File profile = new File( ROOT_LOCATION, snapshotFolder + "profiles/" + getFileName( name ) );

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

	private static final void initializeAscensionData( String name )
	{
		File ascension = new File( ROOT_LOCATION, snapshotFolder + "ascensions/" + getFileName( name ) );

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
	
	public static String getTitle( String name )
	{	return (String) titleMap.get( name.toLowerCase() );
	}

	public static final void registerMember( String name, String level, String title )
	{
		String lowercase = name.toLowerCase();

		if ( !currentMembers.contains( lowercase ) )
			currentMembers.add( lowercase );
		if ( !whiteListMembers.contains( lowercase ) )
			whiteListMembers.add( lowercase );

		ClanSnapshotTable.registerMember( name, level );
		AscensionSnapshotTable.registerMember( name );

		titleMap.put( lowercase, title );
	}

	public static final void unregisterMember( String playerId )
	{
		String lowercase = KoLmafia.getPlayerName( playerId ).toLowerCase();

		currentMembers.remove( lowercase );
		whiteListMembers.remove( lowercase );

		ClanSnapshotTable.unregisterMember( playerId );
		AscensionSnapshotTable.unregisterMember( playerId );
	}

	/**
	 * Takes a ClanSnapshotTable of clan member data for this clan.  The user will
	 * be prompted for the data they would like to include in this ClanSnapshotTable,
	 * including complete player profiles, favorite food, and any other
	 * data gathered by KoLmafia.  If the clan member list was not previously
	 * initialized, this method will also initialize that list.
	 */

	public static final void takeSnapshot( int mostAscensionsBoardSize, int mainBoardSize, int classBoardSize, int maxAge, boolean playerMoreThanOnce, boolean localProfileLink )
	{
		retrieveClanData();

		File standardFile = new File( ROOT_LOCATION, snapshotFolder + "standard.htm" );
		File softcoreFile = new File( ROOT_LOCATION, snapshotFolder + "softcore.htm" );
		File hardcoreFile = new File( ROOT_LOCATION, snapshotFolder + "hardcore.htm" );
		File sortingScript = new File( ROOT_LOCATION, snapshotFolder + "sorttable.js" );

		// If initialization was unsuccessful, then there isn't
		// enough data to create a clan ClanSnapshotTable.

		if ( !retrieveMemberData( true, true ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Initialization failed." );
			return;
		}

		// Now, store the clan snapshot into the appropriate
		// data folder.

		RequestThread.openRequestSequence();
		KoLmafia.updateDisplay( "Storing clan snapshot..." );

		try
		{
			PrintStream ostream = LogStream.openStream( standardFile, true );
			ostream.println( ClanSnapshotTable.getStandardData( localProfileLink ) );
			ostream.close();

			String line;
			BufferedReader script = DataUtilities.getReader( "relay", "sorttable.js" );

			ostream = LogStream.openStream( sortingScript, true );
			while ( (line = script.readLine()) != null )
				ostream.println( line );

			ostream.close();

			KoLmafia.updateDisplay( "Storing ascension snapshot..." );

			ostream = LogStream.openStream( softcoreFile, true );
			ostream.println( AscensionSnapshotTable.getAscensionData( true, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink ) );
			ostream.close();

			ostream = LogStream.openStream( hardcoreFile, true );
			ostream.println( AscensionSnapshotTable.getAscensionData( false, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce, localProfileLink ) );
			ostream.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		KoLmafia.updateDisplay( "Snapshot generation completed." );
		RequestThread.closeRequestSequence();

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

	public static final void saveStashLog()
	{
		retrieveClanData();
		RequestThread.postRequest( new ClanStashLogRequest() );
	}

	/**
	 * Retrieves the clan membership in the form of a
	 * list object.
	 */

	public static final List getWhiteList()
	{
		retrieveClanData();
		return whiteListMembers;
	}

	public static final boolean isMember( String memberName )
	{
		retrieveClanData();
		return Collections.binarySearch( whiteListMembers, memberName.toLowerCase() ) != -1;
	}

	public static final void applyFilter( int matchType, int filterType, String filter )
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
		}

		ClanSnapshotTable.applyFilter( matchType, filterType, filter );
	}
}
