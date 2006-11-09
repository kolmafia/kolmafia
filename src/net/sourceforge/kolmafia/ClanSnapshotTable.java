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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanSnapshotTable extends KoLDatabase
{
	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>(.*?)</tr>" );
	private static final Pattern CELL_PATTERN = Pattern.compile( "<td.*?>(.*?)</td>" );

	public static final int EXACT_MATCH = 0;
	public static final int BELOW_MATCH = -1;
	public static final int ABOVE_MATCH = 1;

	public static final int NAME_FILTER = 0;
	public static final int LEVEL_FILTER = 1;
	public static final int PVP_FILTER = 2;
	public static final int CLASS_FILTER = 3;
	public static final int KARMA_FILTER = 4;

	public static final int LOGIN_FILTER = 5;

	public static final String [] FILTER_NAMES =
	{	"Player name", "Current level", "Antihippy rank", "Character class", "Accumulated karma", "Number of days idle"
	};

	private static Map levelMap = new TreeMap();
	private static Map profileMap = new TreeMap();
	private static Map rosterMap = new TreeMap();

	private static LockableListModel filterList = new LockableListModel();
	private static DetailRosterRequest request = null;

	public static void reset()
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		levelMap.clear();
		profileMap.clear();
		rosterMap.clear();
		filterList.clear();

		// Next, retrieve a detailed copy of the clan
		// roster to complete initialization.

		request = new DetailRosterRequest();
	}

	public static Map getProfileMap()
	{	return profileMap;
	}

	public static LockableListModel getFilteredList()
	{	return filterList;
	}

	public static void registerMember( String playerName, String level )
	{
		String lowerCaseName = playerName.toLowerCase();

		levelMap.put( lowerCaseName, level );
		profileMap.put( lowerCaseName, "" );
	}

	public static void unregisterMember( String playerId )
	{
		ProfileRequest [] filterArray = new ProfileRequest[ filterList.size() ];
		filterList.toArray( filterArray );

		for ( int i = 0; i < filterArray.length; ++i )
		{
			String lowerCaseName = filterArray[i].getPlayerName().toLowerCase();

			if ( filterArray[i].getPlayerId().equals( playerId ) )
			{
				filterList.remove(i);

				levelMap.remove( lowerCaseName );
				profileMap.remove( lowerCaseName );
				rosterMap.remove( lowerCaseName );
			}
		}
	}

	public static void applyFilter( int matchType, int filterType, String filter )
	{
		// First, if you haven't retrieved a detailed
		// roster for the clan, do so.

		if ( request.responseText == null )
			request.run();

		filterList.clear();
		ArrayList interimList = new ArrayList();

		String [] names = new String[ profileMap.keySet().size() ];
		profileMap.keySet().toArray( names );

		try
		{
			// If the comparison value matches the type desired,
			// add the element to the list.

			for ( int i = 0; i < names.length; ++i )
				if ( compare( filterType, names[i], filter ) == matchType )
					interimList.add( getProfile( names[i] ) );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}

		KoLmafia.updateDisplay( "Rendering list (KoLmafia may temporary lock)..." );
		filterList.addAll( interimList );
		KoLmafia.updateDisplay( "Search results rendered." );
	}

	private static ProfileRequest getProfile( String name )
	{
		return ProfileRequest.getInstance( name, KoLmafia.getPlayerId( name ), (String) levelMap.get(name),
			(String) profileMap.get(name), (String) rosterMap.get(name) );
	}

	private static int compare( int filterType, String name, String filter )
	{
		int compareValue = 0;
		ProfileRequest request = getProfile( name );

		try
		{
			switch ( filterType )
			{
			case NAME_FILTER:
				compareValue = request.getPlayerName().compareToIgnoreCase( filter );
				break;

			case LEVEL_FILTER:
				compareValue = request.getPlayerLevel().intValue() - parseInt( filter );
				break;

			case PVP_FILTER:
				compareValue = request.getPvpRank().intValue() - parseInt( filter );
				break;

			case CLASS_FILTER:
				compareValue = request.getClassType().compareToIgnoreCase( filter );
				break;

			case KARMA_FILTER:
				compareValue = request.getKarma().intValue() - parseInt( filter );
				break;

			case LOGIN_FILTER:

				int daysIdle = parseInt( filter );
				long millisecondsIdle = 86400000L * daysIdle;
				Date cutoffDate = new Date( System.currentTimeMillis() - millisecondsIdle );

				compareValue = request.getLastLogin().after( cutoffDate ) ? -1 : request.getLastLogin().before( cutoffDate ) ? 1 : 0;
				break;
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}

		return compareValue < 0 ? -1 : compareValue > 0 ? 1 : 0;
	}

	public static String getStandardData( boolean localProfileLink )
	{
		// First, if you haven't retrieved a detailed
		// roster for the clan, do so.

		if ( request.responseText == null )
			request.run();

		String [] members = new String[ profileMap.keySet().size() ];
		profileMap.keySet().toArray( members );

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
		strbuf.append( LINE_BREAK );

		strbuf.append( "<script language=\"Javascript\" src=\"sorttable.js\"></script>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<style> body, td { font-family: sans-serif; } </style></head>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<body>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<h2>" );
		strbuf.append( ClanManager.getClanName() );
		strbuf.append( "</h2>" );
		strbuf.append( LINE_BREAK );

		ArrayList rankList = new ArrayList();

		ProfileRequest memberLookup;
		for ( int i = 0; i < members.length; ++i )
		{
			memberLookup = getProfile( members[i] );
			rankList.add( memberLookup.getRank() );
		}

		Collections.sort( rankList );
		strbuf.append( getBreakdown( rankList ) );

		strbuf.append( "<center><br><br><table class=\"sortable\" id=\"overview\" border=0 cellspacing=0 cellpadding=10>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<tr style=\"font-weight: bold\"><td>Name</td>" );

		strbuf.append( getOverviewHeader() );
		strbuf.append( LINE_BREAK );

		for ( int i = 0; i < members.length; ++i )
		{
			strbuf.append( getOverviewDetail( members[i], localProfileLink ) );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "</table>" );

		strbuf.append( "<br><br><hr width=80%><br><br>" );

		strbuf.append( getStatsSummary( members ) );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<br><br><table class=\"sortable\" id=\"stats\" border=0 cellspacing=0 cellpadding=10>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<tr style=\"font-weight: bold\"><td>Name</td>" );

		strbuf.append( getStatsHeader() );
		strbuf.append( LINE_BREAK );

		for ( int i = 0; i < members.length; ++i )
		{
			strbuf.append( getStatsDetail( members[i], localProfileLink ) );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "</table>" );

		strbuf.append( "<br><br><hr width=80%><br><br>" );

		strbuf.append( getSocialSummary( members ) );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<br><br><table class=\"sortable\" id=\"social\" border=0 cellspacing=0 cellpadding=10>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<tr style=\"font-weight: bold\"><td>Name</td>" );

		strbuf.append( getSocialHeader() );
		strbuf.append( LINE_BREAK );

		for ( int i = 0; i < members.length; ++i )
		{
			strbuf.append( getSocialDetail( members[i], localProfileLink ) );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "</table>" );

		strbuf.append( "</center></body></html>" );
		strbuf.append( LINE_BREAK );

		return strbuf.toString();
	}

	private static String getOverviewDetail( String memberName, boolean localProfileLink )
	{
		ProfileRequest memberLookup = getProfile( memberName );
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

		strbuf.append( KoLmafia.getPlayerName( KoLmafia.getPlayerId( memberName ) ) );

		if ( localProfileLink )
			strbuf.append( "</a>" );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.isHardcore() ? "HC" : "SC" );

		String restriction = memberLookup.getRestriction();

		if ( restriction == null )
			strbuf.append( "Astral" );
		else if ( restriction.startsWith( "Boo" ) )
			strbuf.append( "B" );
		else if ( restriction.startsWith( "Tee" ) )
			strbuf.append( "T" );
		else if ( restriction.startsWith( "Oxy" ) )
			strbuf.append( "O" );
		else
			strbuf.append( "NP" );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getClassType() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getPlayerLevel() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( COMMA_FORMAT.format( memberLookup.getCurrentRun() == null ? 0 : memberLookup.getCurrentRun().intValue() ) );

		AscensionDataRequest request = AscensionDataRequest.getInstance( memberName, KoLmafia.getPlayerId( memberName ),
			(String) AscensionSnapshotTable.getAscensionMap().get( memberName ) );

		List ascensions = request.getAscensionData();

		strbuf.append( "</td><td align=center>" );
		if ( ascensions.isEmpty() )
			strbuf.append( memberLookup.getCreationAsString() );
		else
			strbuf.append( ((AscensionDataRequest.AscensionDataField)ascensions.get( ascensions.size() - 1 )).getDateAsString() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getLastLoginAsString() );

		strbuf.append( "</td></tr>" );
		return strbuf.toString();
	}

	private static String getOverviewHeader()
	{
		return "<td>Path</td><td align=center>Class</td><td align=center>Lvl</td>" +
			"<td align=center>Turns</td><td align=center>Ascended</td><td align=center>Logged In</td>";
	}

	private static String getStatsSummary( String [] members )
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
			memberLookup = getProfile( members[i] );

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
		strbuf.append( LINE_BREAK );
		strbuf.append( "<td valign=top>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<center><b>Averages</b></center><ul>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>PVP Rank: " + COMMA_FORMAT.format( calculateAverage( pvpList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Muscle: " + COMMA_FORMAT.format( calculateAverage( musList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Myst: " + COMMA_FORMAT.format( calculateAverage( mysList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Moxie: " + COMMA_FORMAT.format( calculateAverage( moxList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Power: " + COMMA_FORMAT.format( calculateAverage( powerList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Turns: " + COMMA_FORMAT.format( calculateAverage( turnsList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "</ul></td>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<td valign=top>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<center><b>Totals</b></center><ul>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>PVP Rank: " + COMMA_FORMAT.format( calculateTotal( pvpList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Muscle: " + COMMA_FORMAT.format( calculateTotal( musList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Myst: " + COMMA_FORMAT.format( calculateTotal( mysList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Moxie: " + COMMA_FORMAT.format( calculateTotal( moxList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Power: " + COMMA_FORMAT.format( calculateTotal( powerList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Turns: " + COMMA_FORMAT.format( calculateTotal( turnsList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "</ul></td>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<td valign=top><center><b>Class Breakdown</b></center>" );
		strbuf.append( getBreakdown( classList ) );
		strbuf.append( LINE_BREAK );

		strbuf.append( "</tr></table>" );
		strbuf.append( LINE_BREAK );

		return strbuf.toString();
	}

	private static String getStatsDetail( String memberName, boolean localProfileLink )
	{
		ProfileRequest memberLookup = getProfile( memberName );
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

		strbuf.append( KoLmafia.getPlayerName( KoLmafia.getPlayerId( memberName ) ) );

		if ( localProfileLink )
			strbuf.append( "</a>" );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.isHardcore() ? "HC" : "SC" );

		String restriction = memberLookup.getRestriction();

		if ( restriction == null )
			strbuf.append( "Astral" );
		else if ( restriction.startsWith( "Boo" ) )
			strbuf.append( "B" );
		else if ( restriction.startsWith( "Tee" ) )
			strbuf.append( "T" );
		else if ( restriction.startsWith( "Oxy" ) )
			strbuf.append( "O" );
		else
			strbuf.append( "NP" );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getClassType() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getPlayerLevel() );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getPvpRank() == null ? "&nbsp;" : COMMA_FORMAT.format( memberLookup.getPvpRank().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getMuscle() == null ? "0" : COMMA_FORMAT.format( memberLookup.getMuscle().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getMysticism() == null ? "0" : COMMA_FORMAT.format( memberLookup.getMysticism().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getMoxie() == null ? "0" : COMMA_FORMAT.format( memberLookup.getMoxie().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( memberLookup.getTurnsPlayed() == null ? "0" : COMMA_FORMAT.format( memberLookup.getTurnsPlayed().intValue() ) );

		strbuf.append( "</td><td align=center>" );
		strbuf.append( COMMA_FORMAT.format( memberLookup.getAscensionCount() == null ? 0 : memberLookup.getAscensionCount().intValue() ) );

		strbuf.append( "</td></tr>" );
		return strbuf.toString();
	}

	private static String getStatsHeader()
	{
		return "<td align=center>Path</td><td align=center>Class</td><td align=center>Lv</td><td>PvP</td><td align=center>Mus</td><td align=center>Mys</td><td align=center>Mox</td><td align=center>Total Turns</td><td align=center>Asc</td>";
	}

	private static String getSocialSummary( String [] members )
	{
		StringBuffer strbuf = new StringBuffer();

		ArrayList foodList = new ArrayList();
		ArrayList drinkList = new ArrayList();

		ProfileRequest memberLookup;

		for ( int i = 0; i < members.length; ++i )
		{
			memberLookup = getProfile( members[i] );

			foodList.add( memberLookup.getFood() );
			drinkList.add( memberLookup.getDrink() );
		}

		Collections.sort( foodList );
		Collections.sort( drinkList );

		strbuf.append( "<table border=0 cellspacing=10 cellpadding=10><tr>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<td valign=top><center><b>Food Breakdown</b></center>" );
		strbuf.append( getBreakdown( foodList ) );
		strbuf.append( "</td>" );

		strbuf.append( "<td valign=top><center><b>Drink Breakdown</b></center>" );
		strbuf.append( getBreakdown( drinkList ) );
		strbuf.append( "</td></tr></table>" );

		return strbuf.toString();
	}

	private static String getSocialDetail( String memberName, boolean localProfileLink )
	{
		ProfileRequest memberLookup = getProfile( memberName );
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

		strbuf.append( KoLmafia.getPlayerName( KoLmafia.getPlayerId( memberName ) ) );

		if ( localProfileLink )
			strbuf.append( "</a>" );

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

	private static String getSocialHeader()
	{
		return "<td>Rank</td><td>Favorite Food</td><td>Favorite Drink</td><td>Created</td>";
	}

	private static class DetailRosterRequest extends KoLRequest
	{
		public DetailRosterRequest()
		{	super( "clan_detailedroster.php" );
		}

		public void run()
		{
			KoLmafia.updateDisplay( "Retrieving detailed roster..." );
			super.run();

			Matcher rowMatcher = ROW_PATTERN.matcher( responseText.substring( responseText.indexOf( "clan_detailedroster.php" ) ) );
			rowMatcher.find();

			String currentRow;
			String currentName;
			Matcher dataMatcher;

			int lastRowIndex = 0;
			while ( rowMatcher.find( lastRowIndex ) )
			{
				lastRowIndex = rowMatcher.end();
				currentRow = rowMatcher.group(1);

				if ( !currentRow.equals( "<td height=4></td>" ) )
				{
					dataMatcher = CELL_PATTERN.matcher( currentRow );

					// The name of the player occurs in the first
					// field of the table.  Use this to index the
					// roster map.

					dataMatcher.find();
					currentName = dataMatcher.group(1).toLowerCase();
					rosterMap.put( currentName, currentRow );
				}
			}

			KoLmafia.updateDisplay( "Detail roster retrieved." );
		}
	}
}
