/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.request.AscensionHistoryRequest;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest.AscensionDataField;
import net.sourceforge.kolmafia.request.ClanMembersRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ProfileSnapshot
	extends KoLDatabase
{
	public static final int EXACT_MATCH = 0;
	public static final int BELOW_MATCH = -1;
	public static final int ABOVE_MATCH = 1;

	public static final int NAME_FILTER = 0;
	public static final int LEVEL_FILTER = 1;
	public static final int PVP_FILTER = 2;
	public static final int CLASS_FILTER = 3;
	public static final int KARMA_FILTER = 4;

	public static final int LOGIN_FILTER = 5;

	public static final String[] FILTER_NAMES =
	{
		"Player name",
		"Current level",
		"Antihippy rank",
		"Character class",
		"Accumulated karma",
		"Number of days idle"
	};

	private static final Map levelMap = new TreeMap();
	private static final Map profileMap = new TreeMap();
	private static final Map rosterMap = new TreeMap();

	private static final LockableListModel filterList = new LockableListModel();
	private static final ClanMembersRequest request = new ClanMembersRequest( true );

	public static final void clearCache()
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		ProfileSnapshot.levelMap.clear();
		ProfileSnapshot.profileMap.clear();
		ProfileSnapshot.rosterMap.clear();
		ProfileSnapshot.filterList.clear();
	}

	public static final Map<String, String> getProfileMap()
	{
		return ProfileSnapshot.profileMap;
	}

	public static final LockableListModel getFilteredList()
	{
		return ProfileSnapshot.filterList;
	}

	public static final void registerMember( final String playerName, final String level )
	{
		String lowerCaseName = playerName.toLowerCase();

		ProfileSnapshot.levelMap.put( lowerCaseName, level );
		ProfileSnapshot.profileMap.put( lowerCaseName, "" );
	}

	public static final void unregisterMember( final String playerId )
	{
		ProfileRequest[] filterArray = new ProfileRequest[ ProfileSnapshot.filterList.size() ];
		ProfileSnapshot.filterList.toArray( filterArray );

		for ( int i = 0; i < filterArray.length; ++i )
		{
			String lowerCaseName = filterArray[ i ].getPlayerName().toLowerCase();

			if ( filterArray[ i ].getPlayerId().equals( playerId ) )
			{
				ProfileSnapshot.filterList.remove( i );

				ProfileSnapshot.levelMap.remove( lowerCaseName );
				ProfileSnapshot.profileMap.remove( lowerCaseName );
				ProfileSnapshot.rosterMap.remove( lowerCaseName );
			}
		}
	}

	public static final void applyFilter( final int matchType, final int filterType, final String filter )
	{
		// First, if you haven't retrieved a detailed
		// roster for the clan, do so.

		if ( ProfileSnapshot.request.responseText == null )
		{
			RequestThread.postRequest( ProfileSnapshot.request );
		}

		ProfileSnapshot.filterList.clear();
		ArrayList interimList = new ArrayList();

		String[] names = new String[ ProfileSnapshot.profileMap.size() ];
		ProfileSnapshot.profileMap.keySet().toArray( names );

		try
		{
			// If the comparison value matches the type desired,
			// add the element to the list.

			for ( int i = 0; i < names.length; ++i )
			{
				if ( ProfileSnapshot.compare( filterType, names[ i ], filter ) == matchType )
				{
					interimList.add( ProfileSnapshot.getProfile( names[ i ] ) );
				}
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		KoLmafia.updateDisplay( "Rendering list (KoLmafia may temporary lock)..." );
		ProfileSnapshot.filterList.addAll( interimList );
		KoLmafia.updateDisplay( "Search results rendered." );
	}

	private static final ProfileRequest getProfile( final String name )
	{
		return ProfileRequest.getInstance(
			name, ContactManager.getPlayerId( name ), (String) ProfileSnapshot.levelMap.get( name ),
			(String) ProfileSnapshot.profileMap.get( name ), (String) ProfileSnapshot.rosterMap.get( name ) );
	}

	private static final int compare( final int filterType, final String name, final String filter )
	{
		int compareValue = 0;
		ProfileRequest request = ProfileSnapshot.getProfile( name );

		try
		{
			switch ( filterType )
			{
			case NAME_FILTER:
				compareValue = request.getPlayerName().compareToIgnoreCase( filter );
				break;

			case LEVEL_FILTER:
				compareValue = request.getPlayerLevel().intValue() - StringUtilities.parseInt( filter );
				break;

			case PVP_FILTER:
				compareValue = request.getPvpRank().intValue() - StringUtilities.parseInt( filter );
				break;

			case CLASS_FILTER:
				compareValue = request.getClassType().compareToIgnoreCase( filter );
				break;

			case KARMA_FILTER:
				compareValue = request.getKarma().intValue() - StringUtilities.parseInt( filter );
				break;

			case LOGIN_FILTER:

				int daysIdle = StringUtilities.parseInt( filter );
				long millisecondsIdle = 86400000L * daysIdle;
				Date cutoffDate = new Date( System.currentTimeMillis() - millisecondsIdle );

				compareValue =
					request.getLastLogin().after( cutoffDate ) ? -1 : request.getLastLogin().before( cutoffDate ) ? 1 : 0;
				break;
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		return compareValue < 0 ? -1 : compareValue > 0 ? 1 : 0;
	}

	public static final String getStandardData( final boolean localProfileLink )
	{
		// First, if you haven't retrieved a detailed
		// roster for the clan, do so.

		if ( ProfileSnapshot.request.responseText == null )
		{
			RequestThread.postRequest( ProfileSnapshot.request );
		}

		String[] members = new String[ ProfileSnapshot.profileMap.size() ];
		ProfileSnapshot.profileMap.keySet().toArray( members );

		StringBuffer strbuf = new StringBuffer();

		strbuf.append( "<html><head>" );
		strbuf.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" );

		strbuf.append( "<title>Clan Snapshot for " );
		strbuf.append( ClanManager.getClanName() );

		strbuf.append( ", Clan #" );
		strbuf.append( ClanManager.getClanId() );
		strbuf.append( " (" );
		strbuf.append( new Date() );
		strbuf.append( ")</title>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<script language=\"Javascript\" src=\"sorttable.js\"></script>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<style> body, td { font-family: sans-serif; } </style></head>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<body>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<h2>" );
		strbuf.append( ClanManager.getClanName() );
		strbuf.append( "</h2>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		ArrayList rankList = new ArrayList();

		ProfileRequest memberLookup;
		for ( int i = 0; i < members.length; ++i )
		{
			memberLookup = ProfileSnapshot.getProfile( members[ i ] );
			rankList.add( memberLookup.getRank() );
		}

		Collections.sort( rankList );
		strbuf.append( KoLDatabase.getBreakdown( rankList ) );

		strbuf.append( "<center><br><br><table class=\"sortable\" id=\"overview\" border=0 cellspacing=0 cellpadding=10>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr style=\"font-weight: bold\"><td>Name</td>" );

		strbuf.append( ProfileSnapshot.getOverviewHeader() );
		strbuf.append( KoLConstants.LINE_BREAK );

		for ( int i = 0; i < members.length; ++i )
		{
			strbuf.append( ProfileSnapshot.getOverviewDetail( members[ i ], localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
		}

		strbuf.append( "</table>" );

		strbuf.append( "<br><br><hr width=80%><br><br>" );

		strbuf.append( ProfileSnapshot.getStatsSummary( members ) );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<br><br><table class=\"sortable\" id=\"stats\" border=0 cellspacing=0 cellpadding=10>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr style=\"font-weight: bold\"><td>Name</td>" );

		strbuf.append( ProfileSnapshot.getStatsHeader() );
		strbuf.append( KoLConstants.LINE_BREAK );

		for ( int i = 0; i < members.length; ++i )
		{
			strbuf.append( ProfileSnapshot.getStatsDetail( members[ i ], localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
		}

		strbuf.append( "</table>" );

		strbuf.append( "<br><br><hr width=80%><br><br>" );

		strbuf.append( ProfileSnapshot.getSocialSummary( members ) );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<br><br><table class=\"sortable\" id=\"social\" border=0 cellspacing=0 cellpadding=10>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr style=\"font-weight: bold\"><td>Name</td>" );

		strbuf.append( ProfileSnapshot.getSocialHeader() );
		strbuf.append( KoLConstants.LINE_BREAK );

		for ( int i = 0; i < members.length; ++i )
		{
			strbuf.append( ProfileSnapshot.getSocialDetail( members[ i ], localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
		}

		strbuf.append( "</table>" );

		strbuf.append( "</center></body></html>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		return strbuf.toString();
	}

	private static final String getOverviewDetail( final String memberName, final boolean localProfileLink )
	{
		ProfileRequest memberLookup = ProfileSnapshot.getProfile( memberName );
		StringBuffer strbuf = new StringBuffer();

		// No matter what happens, you need to make sure
		// to print the player's name first.

		strbuf.append( "<tr><td>" );

		if ( localProfileLink )
		{
			strbuf.append( "<a href=\"profiles/" );
			strbuf.append( ClanManager.getURLName( memberName ) );
			strbuf.append( "\">" );
		}

		strbuf.append( ContactManager.getPlayerName( ContactManager.getPlayerId( memberName ) ) );

		if ( localProfileLink )
		{
			strbuf.append( "</a>" );
		}

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.isHardcore() ? "HC" : "SC" );

		String restriction = memberLookup.getRestriction();

		if ( restriction == null )
		{
			strbuf.append( "Astral" );
		}
		else if ( restriction.startsWith( "Boo" ) )
		{
			strbuf.append( "B" );
		}
		else if ( restriction.startsWith( "Tee" ) )
		{
			strbuf.append( "T" );
		}
		else if ( restriction.startsWith( "Oxy" ) )
		{
			strbuf.append( "O" );
		}
		else
		{
			strbuf.append( "NP" );
		}

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getClassType() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getPlayerLevel() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( KoLConstants.COMMA_FORMAT.format( memberLookup.getCurrentRun() == null ? 0 : memberLookup.getCurrentRun().intValue() ) );

		AscensionHistoryRequest request =
			AscensionHistoryRequest.getInstance(
				memberName, ContactManager.getPlayerId( memberName ), (String) AscensionSnapshot.getAscensionMap().get(
					memberName ) );

		List ascensions = request.getAscensionData();

		strbuf.append( "</td><td align=center>" );
		if ( ascensions.isEmpty() )
		{
			strbuf.append( memberLookup.getCreationAsString() );
		}
		else
		{
			strbuf.append( ( (AscensionDataField) ascensions.get( ascensions.size() - 1 ) ).getDateAsString() );
		}

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getLastLoginAsString() );

		strbuf.append( "</td></tr>" );
		return strbuf.toString();
	}

	private static final String getOverviewHeader()
	{
		return "<td>Path</td><td align=center>Class</td><td align=center>Lvl</td>" + "<td align=center>Turns</td><td align=center>Ascended</td><td align=center>Logged In</td>";
	}

	private static final String getStatsSummary( final String[] members )
	{
		StringBuffer strbuf = new StringBuffer();

		ArrayList classList = new ArrayList();
		ArrayList powerList = new ArrayList();
		ArrayList karmaList = new ArrayList();

		ArrayList meatList = new ArrayList();
		ArrayList turnsList = new ArrayList();
		ArrayList pvpList = new ArrayList();

		ArrayList musList = new ArrayList();
		ArrayList mysList = new ArrayList();
		ArrayList moxList = new ArrayList();

		// Iterate through the list of clan members
		// and populate the lists.

		ProfileRequest memberLookup;

		for ( int i = 0; i < members.length; ++i )
		{
			memberLookup = ProfileSnapshot.getProfile( members[ i ] );

			classList.add( memberLookup.getClassType() );
			meatList.add( memberLookup.getCurrentMeat() );
			turnsList.add( memberLookup.getTurnsPlayed() );
			pvpList.add( memberLookup.getPvpRank() );

			musList.add( memberLookup.getMuscle() );
			mysList.add( memberLookup.getMysticism() );
			moxList.add( memberLookup.getMoxie() );
			powerList.add( memberLookup.getPower() );
			karmaList.add( memberLookup.getKarma() );
		}

		Collections.sort( classList );

		strbuf.append( "<table border=0 cellspacing=10 cellpadding=10><tr>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<td valign=top>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<center><b>Averages</b></center><ul>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>PVP Rank: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateAverage( pvpList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Muscle: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateAverage( musList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Myst: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateAverage( mysList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Moxie: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateAverage( moxList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Power: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateAverage( powerList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Turns: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateAverage( turnsList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "</ul></td>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<td valign=top>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<center><b>Totals</b></center><ul>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>PVP Rank: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateTotal( pvpList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Muscle: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateTotal( musList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Myst: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateTotal( mysList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Moxie: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateTotal( moxList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Power: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateTotal( powerList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<li><nobr>Turns: " + KoLConstants.COMMA_FORMAT.format( KoLDatabase.calculateTotal( turnsList ) ) + "</nobr></li>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "</ul></td>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<td valign=top><center><b>Class Breakdown</b></center>" );
		strbuf.append( KoLDatabase.getBreakdown( classList ) );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "</tr></table>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		return strbuf.toString();
	}

	private static final String getStatsDetail( final String memberName, final boolean localProfileLink )
	{
		ProfileRequest memberLookup = ProfileSnapshot.getProfile( memberName );
		StringBuffer strbuf = new StringBuffer();

		// No matter what happens, you need to make sure
		// to print the player's name first.

		strbuf.append( "<tr><td>" );

		if ( localProfileLink )
		{
			strbuf.append( "<a href=\"profiles/" );
			strbuf.append( ClanManager.getURLName( memberName ) );
			strbuf.append( "\">" );
		}

		strbuf.append( ContactManager.getPlayerName( ContactManager.getPlayerId( memberName ) ) );

		if ( localProfileLink )
		{
			strbuf.append( "</a>" );
		}

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.isHardcore() ? "HC" : "SC" );

		String restriction = memberLookup.getRestriction();

		if ( restriction == null )
		{
			strbuf.append( "Astral" );
		}
		else if ( restriction.startsWith( "Boo" ) )
		{
			strbuf.append( "B" );
		}
		else if ( restriction.startsWith( "Tee" ) )
		{
			strbuf.append( "T" );
		}
		else if ( restriction.startsWith( "Oxy" ) )
		{
			strbuf.append( "O" );
		}
		else
		{
			strbuf.append( "NP" );
		}

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getClassType() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getPlayerLevel() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getPvpRank() == null ? "&nbsp;" : KoLConstants.COMMA_FORMAT.format( memberLookup.getPvpRank().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getMuscle() == null ? "0" : KoLConstants.COMMA_FORMAT.format( memberLookup.getMuscle().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getMysticism() == null ? "0" : KoLConstants.COMMA_FORMAT.format( memberLookup.getMysticism().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getMoxie() == null ? "0" : KoLConstants.COMMA_FORMAT.format( memberLookup.getMoxie().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getTurnsPlayed() == null ? "0" : KoLConstants.COMMA_FORMAT.format( memberLookup.getTurnsPlayed().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( KoLConstants.COMMA_FORMAT.format( memberLookup.getAscensionCount() == null ? 0 : memberLookup.getAscensionCount().intValue() ) );

		strbuf.append( "</td></tr>" );
		return strbuf.toString();
	}

	private static final String getStatsHeader()
	{
		return "<td align=center>Path</td><td align=center>Class</td><td align=center>Lv</td><td>PvP</td><td align=center>Mus</td><td align=center>Mys</td><td align=center>Mox</td><td align=center>Total Turns</td><td align=center>Asc</td>";
	}

	private static final String getSocialSummary( final String[] members )
	{
		StringBuffer strbuf = new StringBuffer();

		ArrayList foodList = new ArrayList();
		ArrayList drinkList = new ArrayList();

		ProfileRequest memberLookup;

		for ( int i = 0; i < members.length; ++i )
		{
			memberLookup = ProfileSnapshot.getProfile( members[ i ] );

			foodList.add( memberLookup.getFood() );
			drinkList.add( memberLookup.getDrink() );
		}

		Collections.sort( foodList );
		Collections.sort( drinkList );

		strbuf.append( "<table border=0 cellspacing=10 cellpadding=10><tr>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<td valign=top><center><b>Food Breakdown</b></center>" );
		strbuf.append( KoLDatabase.getBreakdown( foodList ) );
		strbuf.append( "</td>" );

		strbuf.append( "<td valign=top><center><b>Drink Breakdown</b></center>" );
		strbuf.append( KoLDatabase.getBreakdown( drinkList ) );
		strbuf.append( "</td></tr></table>" );

		return strbuf.toString();
	}

	private static final String getSocialDetail( final String memberName, final boolean localProfileLink )
	{
		ProfileRequest memberLookup = ProfileSnapshot.getProfile( memberName );
		StringBuffer strbuf = new StringBuffer();

		// No matter what happens, you need to make sure
		// to print the player's name first.

		strbuf.append( "<tr><td>" );

		if ( localProfileLink )
		{
			strbuf.append( "<a href=\"profiles/" );
			strbuf.append( ClanManager.getURLName( memberName ) );
			strbuf.append( "\">" );
		}

		strbuf.append( ContactManager.getPlayerName( ContactManager.getPlayerId( memberName ) ) );

		if ( localProfileLink )
		{
			strbuf.append( "</a>" );
		}

		strbuf.append( "</td><td>" );
		strbuf.append( memberLookup.getRank() );

		strbuf.append( "</td><td>" );
		strbuf.append( memberLookup.getFood() );

		strbuf.append( "</td><td>" );
		strbuf.append( memberLookup.getDrink() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getCreationAsString() );

		strbuf.append( "</td></tr>" );
		return strbuf.toString();
	}

	private static final String getSocialHeader()
	{
		return "<td>Rank</td><td>Favorite Food</td><td>Favorite Drink</td><td>Created</td>";
	}

	public static final void addToRoster( final String name, final String row )
	{
		ProfileSnapshot.rosterMap.put( name, row );
	}
}
