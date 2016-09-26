/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatManager;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AscensionSnapshot;
import net.sourceforge.kolmafia.persistence.ProfileSnapshot;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AscensionHistoryRequest;
import net.sourceforge.kolmafia.request.ClanLogRequest;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.ClanMembersRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

import net.sourceforge.kolmafia.webui.RelayLoader;

public abstract class ClanManager
{
	private static final Pattern WHITELIST_PATTERN = Pattern.compile( "<b>([^<]+)</b> \\(#(\\d+)\\)" );

	private static String snapshotFolder = "clan/";
	private static int clanId = 0;
	private static String clanName = null;
	public static boolean stashRetrieved = false;
	private static boolean ranksRetrieved = false;

	private static final ArrayList<String> currentMembers = new ArrayList<String>();
	private static final ArrayList<String> whiteListMembers = new ArrayList<String>();

	private static final Map<String, String> profileMap = ProfileSnapshot.getProfileMap();
	private static final Map<String, String> ascensionMap = AscensionSnapshot.getAscensionMap();
	private static final Map<String, String> titleMap = new HashMap<String, String>();

	private static final List battleList = new ArrayList();

	private static final LockableListModel<String> rankList = new LockableListModel<String>();
	private static final SortedListModel<AdventureResult> stashContents = new SortedListModel<AdventureResult>();

	private static final AdventureResult HOT_DOG_STAND = ItemPool.get( ItemPool.CLAN_HOT_DOG_STAND, 1 );
	private static final AdventureResult FLOUNDRY = ItemPool.get( ItemPool.CLAN_FLOUNDRY, 1 );

	public static final void clearCache()
	{
		ProfileSnapshot.clearCache();
		AscensionSnapshot.clearCache();
		ChatManager.resetClanMessages();
		ClanLoungeRequest.resetHotdogs();
		ClanLoungeRequest.resetSpeakeasy();
		FightRequest.resetKisses();

		ClanManager.clanId = 0;
		ClanManager.clanName = null;
		ClanManager.stashRetrieved = false;
		ClanManager.ranksRetrieved = false;

		ClanManager.currentMembers.clear();
		ClanManager.whiteListMembers.clear();

		ClanManager.profileMap.clear();
		ClanManager.ascensionMap.clear();
		ClanManager.titleMap.clear();
		ClanManager.battleList.clear();
		ClanManager.rankList.clear();
		ClanManager.stashContents.clear();

		KoLConstants.clanLounge.clear();
		KoLConstants.clanRumpus.clear();
	}

	public static final void resetClanId()
	{
		ClanManager.clanId = 0;
		ClanManager.clanName = null;
		KoLCharacter.setClan( false );
	}

	private static final void retrieveClanIdAndName()
	{
		if ( ClanManager.clanId != 0 )
		{
			return;
		}

		ProfileRequest request = new ProfileRequest( KoLCharacter.getUserName() );
		RequestThread.postRequest( request );
	}

	public static final int getClanId()
	{
		ClanManager.retrieveClanIdAndName();
		return ClanManager.clanId;
	}

	public static final String getClanName( boolean update )
	{
		if ( update )
		{
			ClanManager.retrieveClanIdAndName();
		}

		return ClanManager.clanName;
	}

	public static final void setClanId( int id )
	{
		ClanManager.clanId = id;
	}

	public static final void changeClan( final int clanId, String clanName )
	{
		// Drop all saved clan information
		ClanManager.clearCache();

		// Save new clan information
		ClanManager.clanId = clanId;
		ClanManager.clanName = clanName;

		// Visit lounge and rumpus room to see what is there
		if ( clanName != null && !clanName.equals( "" ) )
		{
			RequestLogger.printLine( "You are currently a member of " + clanName );

			// Visit lounge and check hotdog stand and speakeasy.
			// Have to visit second floor in addition, as both default to ground floor if not present.
			ClanLoungeRequest.visitLoungeFloor2();
			if ( KoLConstants.clanLounge.contains( HOT_DOG_STAND ) )
			{
				ClanLoungeRequest.visitLounge( ClanLoungeRequest.HOT_DOG_STAND );
			}
			ClanLoungeRequest.visitLounge( ClanLoungeRequest.SPEAKEASY );
			if ( KoLConstants.clanLounge.contains( FLOUNDRY ) )
			{
				ClanLoungeRequest.visitLounge( ClanLoungeRequest.FLOUNDRY );
			}

			RequestThread.postRequest( new ClanRumpusRequest( RequestType.SEARCH ) );
		}
		else
		{
			RequestLogger.printLine( "You are not currently a member of a clan." );
			KoLConstants.clanLounge.clear();
		}
	}

	public static final void setClanName( String name )
	{
		boolean changed =
			( name == null ) ?
			ClanManager.clanName != null :
			( ClanManager.clanName != null ) ?
			!ClanManager.clanName.equals( name ) :
			true;

		KoLCharacter.setClan( name != null );
		if ( changed )
		{
			ClanManager.changeClan( ClanManager.clanId, name );
		}
	}

	public static final boolean isStashRetrieved()
	{
		return ClanManager.stashRetrieved;
	}

	public static final void setStashRetrieved()
	{
		ClanManager.stashRetrieved = true;
	}

	public static final LockableListModel<AdventureResult> getStash()
	{
		return ClanManager.stashContents;
	}

	public static final LockableListModel<String> getRankList()
	{
		if ( !ClanManager.ranksRetrieved )
		{
			RequestThread.postRequest( new ClanMembersRequest( ClanManager.rankList ) );
			ClanManager.ranksRetrieved = true;
		}

		return ClanManager.rankList;
	}

	private static final void updateCurrentMembers()
	{
		ClanManager.currentMembers.clear();

		ClanMembersRequest cmr = new ClanMembersRequest( false );
		RequestThread.postRequest( cmr );
		Collections.sort( ClanManager.currentMembers );

		ClanManager.snapshotFolder =
			"clan/" + ClanManager.clanId + "/" + KoLConstants.WEEKLY_FORMAT.format( new Date() ) + "/";

		KoLmafia.updateDisplay( "Clan data retrieved." );
	}

	private static final void updateWhiteList()
	{
		ClanManager.whiteListMembers.clear();

		GenericRequest whiteListFinder = new GenericRequest( "clan_office.php" );
		RequestThread.postRequest( whiteListFinder );

		if ( whiteListFinder.responseText == null || !whiteListFinder.responseText.contains( "clan_whitelist.php" ) )
		{
			return;
		}

		whiteListFinder = new GenericRequest( "clan_whitelist.php" );
		RequestThread.postRequest( whiteListFinder );

		Matcher whiteListMatcher = ClanManager.WHITELIST_PATTERN.matcher( whiteListFinder.responseText );
		while ( whiteListMatcher.find() )
		{
			String currentName = whiteListMatcher.group( 1 );
			ContactManager.registerPlayerId( currentName, whiteListMatcher.group( 2 ) );

			currentName = currentName.toLowerCase();
			if ( !ClanManager.currentMembers.contains( currentName ) )
			{
				ClanManager.whiteListMembers.add( currentName );
			}
		}

		Collections.sort( ClanManager.whiteListMembers );
	}

	private static final void retrieveClanData()
	{
		if ( KoLmafia.isAdventuring() )
		{
			return;
		}

		if ( !ClanManager.profileMap.isEmpty() )
		{
			return;
		}

		ClanManager.retrieveClanIdAndName();
		ClanManager.updateCurrentMembers();
		ClanManager.updateWhiteList();
	}

	private static final boolean retrieveMemberData( final boolean retrieveProfileData,
		final boolean retrieveAscensionData )
	{
		// First, determine how many member profiles need to be retrieved
		// before this happens.

		int requestsNeeded = 0;

		String filename;
		File profile, ascensionData;
		String currentProfile, currentAscensionData;

		String[] names = new String[ ClanManager.profileMap.size() ];
		ClanManager.profileMap.keySet().toArray( names );

		for ( int i = 0; i < names.length; ++i )
		{
			KoLmafia.updateDisplay( "Cache data lookup for member " + ( i + 1 ) + " of " + names.length + "..." );

			currentProfile = (String) ClanManager.profileMap.get( names[ i ] );
			currentAscensionData = (String) ClanManager.ascensionMap.get( names[ i ] );

			filename = ClanManager.getFileName( names[ i ] );
			profile = new File( KoLConstants.ROOT_LOCATION, ClanManager.snapshotFolder + "profiles/" + filename );
			ascensionData =
				new File( KoLConstants.ROOT_LOCATION, ClanManager.snapshotFolder + "ascensions/" + filename );

			if ( retrieveProfileData )
			{
				if ( currentProfile.equals( "" ) && !profile.exists() )
				{
					++requestsNeeded;
				}

				if ( currentProfile.equals( "" ) && profile.exists() )
				{
					ClanManager.initializeProfile( names[ i ] );
				}
			}

			if ( retrieveAscensionData )
			{
				if ( currentAscensionData.equals( "" ) && !ascensionData.exists() )
				{
					++requestsNeeded;
				}

				if ( currentAscensionData.equals( "" ) && ascensionData.exists() )
				{
					ClanManager.initializeAscensionData( names[ i ] );
				}
			}
		}

		// If all the member profiles have already been retrieved, then
		// you won't need to look up any profiles, so it takes no time.
		// No need to confirm with the user.  Therefore, return.

		if ( requestsNeeded == 0 )
		{
			return true;
		}

		// Now that it's known what the user wishes to continue,
		// you begin initializing all the data.

		// Create a special HTML file for each of the
		// players in the ProfileSnapshot so that it can be
		// navigated at leisure.

		for ( int i = 0; i < names.length && KoLmafia.permitsContinue(); ++i )
		{
			KoLmafia.updateDisplay( "Loading profile for member " + ( i + 1 ) + " of " + names.length + "..." );

			currentProfile = (String) ClanManager.profileMap.get( names[ i ] );
			currentAscensionData = (String) ClanManager.ascensionMap.get( names[ i ] );

			if ( retrieveProfileData && currentProfile.equals( "" ) )
			{
				ClanManager.initializeProfile( names[ i ] );
			}

			if ( retrieveAscensionData && currentAscensionData.equals( "" ) )
			{
				ClanManager.initializeAscensionData( names[ i ] );
			}
		}

		return true;
	}

	public static final String getURLName( final String name )
	{
		return Preferences.baseUserName( name ) + "_(%23" + ContactManager.getPlayerId( name ) + ")" + ".htm";
	}

	public static final String getFileName( final String name )
	{
		return Preferences.baseUserName( name ) + "_(#" + ContactManager.getPlayerId( name ) + ")" + ".htm";
	}

	private static final void initializeProfile( final String name )
	{
		File profile =
			new File(
				KoLConstants.ROOT_LOCATION,
				ClanManager.snapshotFolder + "profiles/" + ClanManager.getFileName( name ) );

		if ( profile.exists() )
		{
			try
			{
				BufferedReader istream = FileUtilities.getReader( profile );
				StringBuilder profileString = new StringBuilder();
				String currentLine;

				while ( ( currentLine = istream.readLine() ) != null )
				{
					profileString.append( currentLine );
					profileString.append( KoLConstants.LINE_BREAK );
				}

				ClanManager.profileMap.put( name.toLowerCase(), profileString.toString() );
				istream.close();
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

			ProfileRequest request = new ProfileRequest( name );
			request.initialize();

			String data =
				KoLConstants.LINE_BREAK_PATTERN.matcher(
					KoLConstants.COMMENT_PATTERN.matcher(
						KoLConstants.STYLE_PATTERN.matcher(
							KoLConstants.SCRIPT_PATTERN.matcher( request.responseText ).replaceAll( "" ) ).replaceAll(
							"" ) ).replaceAll( "" ) ).replaceAll( "" ).replaceAll(
					"ascensionhistory.php\\?back=other&who=" + ContactManager.getPlayerId( name ),
					"../ascensions/" + ClanManager.getURLName( name ) );

			ClanManager.profileMap.put( name, data );

			// To avoid retrieving the file again, store the intermediate
			// result in a local file.

			PrintStream ostream = LogStream.openStream( profile, true );
			ostream.println( data );
			ostream.close();
		}
	}

	private static final void initializeAscensionData( final String name )
	{
		File ascension =
			new File(
				KoLConstants.ROOT_LOCATION,
				ClanManager.snapshotFolder + "ascensions/" + ClanManager.getFileName( name ) );

		if ( ascension.exists() )
		{
			try
			{
				BufferedReader istream = FileUtilities.getReader( ascension );
				StringBuilder ascensionString = new StringBuilder();
				String currentLine;

				while ( ( currentLine = istream.readLine() ) != null )
				{
					ascensionString.append( currentLine );
					ascensionString.append( KoLConstants.LINE_BREAK );
				}

				ClanManager.ascensionMap.put( name, ascensionString.toString() );
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

			AscensionHistoryRequest request = new AscensionHistoryRequest( name, ContactManager.getPlayerId( name ) );
			request.initialize();

			String data =
				KoLConstants.LINE_BREAK_PATTERN.matcher(
					KoLConstants.COMMENT_PATTERN.matcher(
						KoLConstants.STYLE_PATTERN.matcher(
							KoLConstants.SCRIPT_PATTERN.matcher( request.responseText ).replaceAll( "" ) ).replaceAll(
							"" ) ).replaceAll( "" ) ).replaceAll( "" ).replaceAll(
					"<a href=\"charsheet.php\">", "<a href=../profiles/" + ClanManager.getURLName( name ) );

			ClanManager.ascensionMap.put( name, data );

			// To avoid retrieving the file again, store the intermediate
			// result in a local file.

			PrintStream ostream = LogStream.openStream( ascension, true );
			ostream.println( data );
			ostream.close();
		}
	}

	public static String getTitle( final String name )
	{
		return (String) ClanManager.titleMap.get( name.toLowerCase() );
	}

	public static final void registerMember( final String name, final String level, final String title )
	{
		String lowercase = name.toLowerCase();

		if ( !ClanManager.currentMembers.contains( lowercase ) )
		{
			ClanManager.currentMembers.add( lowercase );
		}
		if ( !ClanManager.whiteListMembers.contains( lowercase ) )
		{
			ClanManager.whiteListMembers.add( lowercase );
		}

		ProfileSnapshot.registerMember( name, level );
		AscensionSnapshot.registerMember( name );

		ClanManager.titleMap.put( lowercase, title );
	}

	public static final void unregisterMember( final String playerId )
	{
		String lowercase = ContactManager.getPlayerName( playerId ).toLowerCase();

		ClanManager.currentMembers.remove( lowercase );
		ClanManager.whiteListMembers.remove( lowercase );

		ProfileSnapshot.unregisterMember( playerId );
		AscensionSnapshot.unregisterMember( playerId );
	}

	/**
	 * Takes a ProfileSnapshot of clan member data for this clan. The user will be prompted for the data they would
	 * like to include in this ProfileSnapshot, including complete player profiles, favorite food, and any other data
	 * gathered by KoLmafia. If the clan member list was not previously initialized, this method will also initialize
	 * that list.
	 */

	public static final void takeSnapshot( final int mostAscensionsBoardSize, final int mainBoardSize,
		final int classBoardSize, final int maxAge, final boolean playerMoreThanOnce, final boolean localProfileLink )
	{
		ClanManager.retrieveClanData();

		File standardFile = new File( KoLConstants.ROOT_LOCATION, ClanManager.snapshotFolder + "standard.htm" );
		File softcoreFile = new File( KoLConstants.ROOT_LOCATION, ClanManager.snapshotFolder + "softcore.htm" );
		File hardcoreFile = new File( KoLConstants.ROOT_LOCATION, ClanManager.snapshotFolder + "hardcore.htm" );
		File casualFile = new File( KoLConstants.ROOT_LOCATION, ClanManager.snapshotFolder + "casual.htm" );
		File sortingScript = new File( KoLConstants.ROOT_LOCATION, ClanManager.snapshotFolder + KoLConstants.SORTTABLE_JS );

		// If initialization was unsuccessful, then there isn't
		// enough data to create a clan ProfileSnapshot.

		if ( !ClanManager.retrieveMemberData( true, true ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Initialization failed." );
			return;
		}

		// Now, store the clan snapshot into the appropriate
		// data folder.

		KoLmafia.updateDisplay( "Storing clan snapshot..." );

		try
		{
			PrintStream ostream = LogStream.openStream( standardFile, true );
			ostream.println( ProfileSnapshot.getStandardData( localProfileLink ) );
			ostream.close();

			String line;
			BufferedReader script = DataUtilities.getReader( "relay", KoLConstants.SORTTABLE_JS );

			ostream = LogStream.openStream( sortingScript, true );
			while ( ( line = script.readLine() ) != null )
			{
				ostream.println( line );
			}

			ostream.close();

			KoLmafia.updateDisplay( "Storing ascension snapshot..." );

			ostream = LogStream.openStream( softcoreFile, true );
			ostream.println( AscensionSnapshot.getAscensionData(
				AscensionSnapshot.NORMAL, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			ostream.close();

			ostream = LogStream.openStream( hardcoreFile, true );
			ostream.println( AscensionSnapshot.getAscensionData(
				AscensionSnapshot.HARDCORE, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			ostream.close();

			ostream = LogStream.openStream( casualFile, true );
			ostream.println( AscensionSnapshot.getAscensionData(
				AscensionSnapshot.CASUAL, mostAscensionsBoardSize, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			ostream.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}

		KoLmafia.updateDisplay( "Snapshot generation completed." );

		// To make things less confusing, load the summary
		// file inside of the default browser after completion.

		RelayLoader.openSystemBrowser( standardFile );
		RelayLoader.openSystemBrowser( softcoreFile );
		RelayLoader.openSystemBrowser( hardcoreFile );
		RelayLoader.openSystemBrowser( casualFile );
	}

	/**
	 * Stores all of the transactions made in the clan stash. This loads the existing clan stash log and updates it with
	 * all transactions made by every clan member. this format allows people to see WHO is using the stash, rather than
	 * just what is being done with the stash.
	 */

	public static final void saveStashLog()
	{
		ClanManager.retrieveClanData();
		RequestThread.postRequest( new ClanLogRequest() );
	}

	/**
	 * Retrieves the clan membership in the form of a list object.
	 */

	public static final List<String> getWhiteList()
	{
		ClanManager.retrieveClanData();
		return ClanManager.whiteListMembers;
	}

	public static final boolean isCurrentMember( final String memberName )
	{
		// If we are busy, we can't go and fetch the member list.
		if ( KoLmafia.isAdventuring() )
		{
			return false;
		}

		// Force an update every time for this, since players can come and go asynchronously.
		ClanManager.updateCurrentMembers();
		return Collections.binarySearch( ClanManager.currentMembers, memberName.toLowerCase() ) > -1;
	}

	public static final boolean isMember( final String memberName )
	{
		ClanManager.retrieveClanData();
		return Collections.binarySearch( ClanManager.whiteListMembers, memberName.toLowerCase() ) > -1;
	}

	public static final void applyFilter( final int matchType, final int filterType, final String filter )
	{
		ClanManager.retrieveClanData();

		// Certain filter types do not require the player profiles
		// to be looked up.  These can be processed immediately,
		// without prompting the user for confirmation.

		switch ( filterType )
		{
		case ProfileSnapshot.NAME_FILTER:
		case ProfileSnapshot.LEVEL_FILTER:
		case ProfileSnapshot.KARMA_FILTER:

			break;

		default:

			ClanManager.retrieveMemberData( true, false );
		}

		ProfileSnapshot.applyFilter( matchType, filterType, filter );
	}
}
