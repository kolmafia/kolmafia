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

	public static void unregisterMember( String playerID )
	{
		ProfileRequest [] filterArray = new ProfileRequest[ filterList.size() ];
		filterList.toArray( filterArray );

		for ( int i = 0; i < filterArray.length; ++i )
		{
			String lowerCaseName = filterArray[i].getPlayerName().toLowerCase();

			if ( filterArray[i].getPlayerID().equals( playerID ) )
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
		return ProfileRequest.getInstance( name, KoLmafia.getPlayerID( name ), (String) levelMap.get(name),
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

		StringBuffer strbuf = new StringBuffer();

		strbuf.append( "<html><head>" );
		strbuf.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" );

		strbuf.append( "<title>Clan Snapshot for " );
		strbuf.append( ClanManager.getClanName() );
		strbuf.append( " (" );
		strbuf.append( new Date() );
		strbuf.append( ")</title>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<script language=\"Javascript\" src=\"sorttable.js\"></script>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<style> body, td { font-family: sans-serif; } </style></head><body>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<center><h2>" );
		strbuf.append( ClanManager.getClanName() );
		strbuf.append( " (#" );
		strbuf.append( ClanManager.getClanID() );
		strbuf.append( ")</h2></center>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( getStandardSummary() );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<br><br><table class=\"sortable\" id=\"details\" border=0 cellspacing=6 cellpadding=6>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<tr style=\"font-weight: bold\"><td align=center>Name</td><td align=center>User ID</td>" );

		strbuf.append( getRosterHeader() );
		strbuf.append( LINE_BREAK );

		String [] members = new String[ profileMap.keySet().size() ];
		profileMap.keySet().toArray( members );

		for ( int i = 0; i < members.length; ++i )
		{
			strbuf.append( getMemberDetail( members[i], localProfileLink ) );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "</table></body></html>" );
		strbuf.append( LINE_BREAK );

		return strbuf.toString();
	}

	private static String getStandardSummary()
	{
		String header = getRosterHeader();
		StringBuffer strbuf = new StringBuffer();

		ArrayList classList = new ArrayList();
		ArrayList foodList = new ArrayList();
		ArrayList drinkList = new ArrayList();

		ArrayList rankList = new ArrayList();
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
		String [] members = new String[ profileMap.keySet().size() ];
		profileMap.keySet().toArray( members );

		for ( int i = 0; i < members.length; ++i )
		{
			memberLookup = getProfile( members[i] );

			if ( header.indexOf( "<td>Class</td>" ) != -1 )
				classList.add( memberLookup.getClassType() );

			if ( header.indexOf( "<td>Food</td>" ) != -1 )
				foodList.add( memberLookup.getFood() );

			if ( header.indexOf( "<td>Drink</td>" ) != -1 )
				drinkList.add( memberLookup.getDrink() );

			if ( header.indexOf( "<td>Meat</td>" ) != -1 )
				meatList.add( memberLookup.getCurrentMeat() );

			if ( header.indexOf( "<td>Turns</td>" ) != -1 )
				turnsList.add( memberLookup.getCurrentRun() );

			if ( header.indexOf( "<td>PVP</td>" ) != -1 )
				pvpList.add( memberLookup.getPvpRank() );

			rankList.add( memberLookup.getRank() );

			musList.add( memberLookup.getMuscle() );
			mysList.add( memberLookup.getMysticism() );
			moxList.add( memberLookup.getMoxie() );
			powerList.add( memberLookup.getPower() );
			karmaList.add( memberLookup.getKarma() );
		}

		Collections.sort( classList );
		Collections.sort( foodList );
		Collections.sort( drinkList );
		Collections.sort( rankList );

		strbuf.append( "<table border=0 cellspacing=10 cellpadding=10><tr>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<td valign=top>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<b>Averages</b>:<ul>" );
		strbuf.append( LINE_BREAK );

		if ( header.indexOf( "<td>PVP</td>" ) != -1 )
		{
			strbuf.append( "<li><nobr>PVP Rank: " + COMMA_FORMAT.format( calculateAverage( pvpList ) ) + "</nobr></li>" );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "<li><nobr>Muscle: " + COMMA_FORMAT.format( calculateAverage( musList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Myst: " + COMMA_FORMAT.format( calculateAverage( mysList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Moxie: " + COMMA_FORMAT.format( calculateAverage( moxList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Power: " + COMMA_FORMAT.format( calculateAverage( powerList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Karma: " + COMMA_FORMAT.format( calculateAverage( karmaList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		if ( header.indexOf( "<td>Meat</td>" ) != -1 )
		{
			strbuf.append( "<li><nobr>Meat: " + COMMA_FORMAT.format( calculateAverage( meatList ) ) + "</nobr></li>" );
			strbuf.append( LINE_BREAK );
		}

		if ( header.indexOf( "<td>Turns</td>" ) != -1 )
		{
			strbuf.append( "<li><nobr>Turns: " + COMMA_FORMAT.format( calculateAverage( turnsList ) ) + "</nobr></li>" );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "</ul>" );
		strbuf.append( LINE_BREAK );
		strbuf.append( "<b>Totals</b>:<ul>" );
		strbuf.append( LINE_BREAK );

		if ( header.indexOf( "<td>PVP</td>" ) != -1 )
		{
			strbuf.append( "<li><nobr>PVP Rank: " + COMMA_FORMAT.format( calculateTotal( pvpList ) ) + "</nobr></li>" );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "<li><nobr>Muscle: " + COMMA_FORMAT.format( calculateTotal( musList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Myst: " + COMMA_FORMAT.format( calculateTotal( mysList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Moxie: " + COMMA_FORMAT.format( calculateTotal( moxList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Power: " + COMMA_FORMAT.format( calculateTotal( powerList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		strbuf.append( "<li><nobr>Karma: " + COMMA_FORMAT.format( calculateTotal( karmaList ) ) + "</nobr></li>" );
		strbuf.append( LINE_BREAK );

		if ( header.indexOf( "<td>Meat</td>" ) != -1 )
		{
			strbuf.append( "<li><nobr>Meat: " + COMMA_FORMAT.format( calculateTotal( meatList ) ) + "</nobr></li>" );
			strbuf.append( LINE_BREAK );
		}

		if ( header.indexOf( "<td>Turns</td>" ) != -1 )
		{
			strbuf.append( "<li><nobr>Turns: " + COMMA_FORMAT.format( calculateTotal( turnsList ) ) + "</nobr></li>" );
			strbuf.append( LINE_BREAK );
		}

		strbuf.append( "</ul></td>" );
		strbuf.append( LINE_BREAK );

		if ( header.indexOf( "<td>Class</td>" ) != -1 )
		{
			strbuf.append( "<td valign=top><b>Class Breakdown</b>:" );
			strbuf.append( getBreakdown( classList ) );

			strbuf.append( LINE_BREAK );

			strbuf.append( "<br><b>Rank Breakdown</b>:" );
			strbuf.append( getBreakdown( rankList ) );
			strbuf.append( "</td>" );
		}

		if ( header.indexOf( "<td>Food</td>" ) != -1 )
		{
			strbuf.append( "<td valign=top><b>Food Breakdown</b>:" );
			strbuf.append( getBreakdown( foodList ) );
			strbuf.append( "</td>" );
		}

		if ( header.indexOf( "<td>Drink</td>" ) != -1 )
		{
			strbuf.append( "<td valign=top><b>Drink Breakdown</b>:" );
			strbuf.append( getBreakdown( drinkList ) );
			strbuf.append( "</td>" );
		}

		strbuf.append( "</tr></table>" );
		strbuf.append( LINE_BREAK );

		return strbuf.toString();
	}

	private static String getMemberDetail( String memberName, boolean localProfileLink )
	{
		ProfileRequest memberLookup = getProfile( memberName );
		StringBuffer strbuf = new StringBuffer();

		// No matter what happens, you need to make sure
		// to print the player's name first.

		strbuf.append( "<tr><td>" );

		if ( localProfileLink )
		{
			strbuf.append( "<a href=\"profiles/" );
			strbuf.append( KoLmafia.getPlayerID( memberName ) );
			strbuf.append( ".htm\">" );
		}

		strbuf.append( KoLmafia.getPlayerName( KoLmafia.getPlayerID( memberName ) ) );

		if ( localProfileLink )
			strbuf.append( "</a>" );

		strbuf.append( "</td><td>" );
		strbuf.append( KoLmafia.getPlayerID( memberName ) );

		// Each of these are printed, pending on what
		// fields are desired in this particular table.

		String [] header = getRosterHeader().split( "(</?td>)+" );

		for ( int i = 0; i < header.length; ++i )
		{
			if ( header[i].equals( "Lv" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getPlayerLevel() );
			}

			if ( header[i].equals( "Mus" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( COMMA_FORMAT.format( memberLookup.getMuscle().intValue() ) );
			}

			if ( header[i].equals( "Mys" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( COMMA_FORMAT.format( memberLookup.getMysticism().intValue() ) );
			}

			if ( header[i].equals( "Mox" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( COMMA_FORMAT.format( memberLookup.getMoxie().intValue() ) );
			}

			if ( header[i].equals( "Total" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( COMMA_FORMAT.format( memberLookup.getPower().intValue() ) );
			}

			if ( header[i].equals( "Title" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getTitle() );
			}

			if ( header[i].equals( "Rank" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getRank() );
			}

			if ( header[i].equals( "Karma" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( COMMA_FORMAT.format( memberLookup.getKarma().intValue() ) );
			}

			if ( header[i].equals( "PVP" ) )
			{
				strbuf.append( "</td><td>" );

				int rank = memberLookup.getPvpRank().intValue();
				strbuf.append( rank == 0 ? "&nbsp;" : COMMA_FORMAT.format( rank ) );
			}

			if ( header[i].equals( "Class" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getClassType() );
			}

			if ( header[i].equals( "Meat" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( COMMA_FORMAT.format( memberLookup.getCurrentMeat().intValue() ) );
			}

			if ( header[i].equals( "Turns" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( COMMA_FORMAT.format( memberLookup.getCurrentRun().intValue() ) );
			}

			if ( header[i].equals( "Food" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getFood() );
			}

			if ( header[i].equals( "Drink" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getDrink() );
			}

			if ( header[i].equals( "Created" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getCreationAsString() );
			}

			if ( header[i].equals( "Last Login" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getLastLoginAsString() );
			}

			if ( header[i].equals( "Ascensions" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( COMMA_FORMAT.format( memberLookup.getAscensionCount().intValue() ) );
			}

			if ( header[i].equals( "Last Ascension" ) )
			{
				AscensionDataRequest request = AscensionDataRequest.getInstance( memberName, KoLmafia.getPlayerID( memberName ),
					(String) AscensionSnapshotTable.getAscensionMap().get( memberName ) );

				List ascensions = request.getAscensionData();

				strbuf.append( "</td><td>" );
				strbuf.append( ((AscensionDataRequest.AscensionDataField)ascensions.get( ascensions.size() - 1 )).getDateAsString() );
			}

			if ( header[i].equals( "Path" ) )
			{
				strbuf.append( "</td><td>" );
				strbuf.append( memberLookup.getRestriction() );
			}
		}

		strbuf.append( "</td></tr>" );
		return strbuf.toString();
	}

	public static String getRosterHeader()
	{	return getProperty( "clanRosterHeader" );
	}

	public static final String getDefaultHeader()
	{
		return "<td>Lv</td><td>Mus</td><td>Mys</td><td>Mox</td><td>Total</td>" +
			"<td>Title</td><td>Rank</td><td>Karma</td>" +
			"<td>Class</td><td>Path</td><td>Turns</td><td>Meat</td>" +
			"<td>PVP</td><td>Food</td><td>Drink</td>" +
			"<td>Created</td><td>Last Login</td>";
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
