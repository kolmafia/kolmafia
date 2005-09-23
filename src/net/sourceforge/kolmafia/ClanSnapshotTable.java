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
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.io.File;

import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanSnapshotTable extends KoLDatabase
{
	public static final int EXACT_MATCH = 0;
	public static final int BELOW_MATCH = -1;
	public static final int ABOVE_MATCH = 1;

	public static final int NAME_FILTER = 1;
	public static final int ID_FILTER = 2;
	public static final int LV_FILTER = 3;
	public static final int MUS_FILTER = 4;
	public static final int MYS_FILTER = 5;
	public static final int MOX_FILTER = 6;
	public static final int POWER_FILTER = 7;
	public static final int PVP_FILTER = 8;
	public static final int CLASS_FILTER = 9;
	public static final int RANK_FILTER = 10;
	public static final int KARMA_FILTER = 11;
	public static final int MEAT_FILTER = 12;
	public static final int TURN_FILTER = 13;
	public static final int LOGIN_FILTER = 14;
	public static final int ASCENSION_FILTER = 15;

	private KoLmafia client;
	private String clanID;
	private String clanName;

	private Map levelMap;
	private Map profileMap;
	private Map rosterMap;

	private LockableListModel filterList;
	private DetailRosterRequest request;

	public ClanSnapshotTable( KoLmafia client )
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		this.client = client;
		this.levelMap = new TreeMap();
		this.profileMap = new TreeMap();
		this.rosterMap = new TreeMap();

		this.filterList = new LockableListModel();

		// Next, retrieve a detailed copy of the clan
		// roster to complete initialization.

		request = new DetailRosterRequest( client );
	}

	public Map getProfileMap()
	{	return profileMap;
	}

	public void setClanID( String clanID )
	{	this.clanID = clanID;
	}

	public void setClanName( String clanName )
	{	this.clanName = clanName;
	}

	public LockableListModel getFilteredList()
	{	return filterList;
	}

	public void registerMember( String playerName, String level )
	{
		String lowerCaseName = playerName.toLowerCase();

		levelMap.put( lowerCaseName, level );
		profileMap.put( lowerCaseName, "" );
	}

	public void unregisterMember( String playerID )
	{
		ProfileRequest [] filterArray = new ProfileRequest[ filterList.size() ];
		filterList.toArray( filterArray );

		for ( int i = 0; i < filterArray.length; ++i )
		{
			if ( filterArray[i].getPlayerID().equals( playerID ) )
			{
				String lowerCaseName = filterArray[i].getPlayerName().toLowerCase();

				filterList.remove(i);

				levelMap.remove( lowerCaseName );
				profileMap.remove( lowerCaseName );
				rosterMap.remove( lowerCaseName );
			}
		}
	}

	public void applyFilter( int matchType, int filterType, String filter )
	{
		// First, if you haven't retrieved a detailed
		// roster for the clan, do so.

		if ( request.responseText == null )
			request.run();

		filterList.clear();
		List interimList = new ArrayList();

		String currentName;
		Iterator nameIterator = profileMap.keySet().iterator();

		try
		{
			// If the comparison value matches the type desired,
			// add the element to the list.

			while ( nameIterator.hasNext() )
			{
				currentName = (String) nameIterator.next();
				if ( compare( filterType, currentName, filter ) == matchType )
					interimList.add( getProfile( currentName ) );
			}
		}
		catch ( Exception e )
		{
			// An exception shouldn't occur during the parsing
			// process, unless the user did not enter a valid
			// numeric string.  In this case, nothing is added,
			// which is exactly what's wanted.
		}

		client.updateDisplay( DISABLED_STATE, "Rendering list (KoLmafia may temporary lock)..." );
		filterList.addAll( interimList );
		client.updateDisplay( ENABLED_STATE, "Search results rendered." );
	}

	private ProfileRequest getProfile( String name )
	{	return ProfileRequest.getInstance( name, (String) levelMap.get(name), (String) profileMap.get(name), (String) rosterMap.get(name) );
	}

	private int compare( int filterType, String name, String filter )
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

				case ID_FILTER:
					compareValue = Integer.parseInt( request.getPlayerID() ) - df.parse( filter ).intValue();
					break;

				case LV_FILTER:
					compareValue = request.getPlayerLevel().intValue() - df.parse( filter ).intValue();
					break;

				case MUS_FILTER:
					compareValue = request.getMuscle().intValue() - df.parse( filter ).intValue();
					break;

				case MYS_FILTER:
					compareValue = request.getMysticism().intValue() - df.parse( filter ).intValue();
					break;

				case MOX_FILTER:
					compareValue = request.getMoxie().intValue() - df.parse( filter ).intValue();
					break;

				case POWER_FILTER:
					compareValue = request.getPower().intValue() - df.parse( filter ).intValue();
					break;

				case PVP_FILTER:
					compareValue = request.getPvpRank().intValue() - df.parse( filter ).intValue();
					break;

				case CLASS_FILTER:
					compareValue = request.getClassType().compareToIgnoreCase( filter );
					break;

				case RANK_FILTER:
					compareValue = request.getRank().compareToIgnoreCase( filter );
					break;

				case KARMA_FILTER:
					compareValue = request.getKarma().intValue() - df.parse( filter ).intValue();
					break;

				case MEAT_FILTER:
					compareValue = request.getCurrentMeat().intValue() - df.parse( filter ).intValue();
					break;

				case TURN_FILTER:
					compareValue = request.getTurnsPlayed().intValue() - df.parse( filter ).intValue();
					break;

				case LOGIN_FILTER:

					int daysIdle = df.parse( filter ).intValue();
					long millisecondsIdle = 86400000L * daysIdle;
					Date cutoffDate = new Date( System.currentTimeMillis() - millisecondsIdle );

					compareValue = request.getLastLogin().after( cutoffDate ) ? -1 : request.getLastLogin().before( cutoffDate ) ? 1 : 0;
					break;

				case ASCENSION_FILTER:
					compareValue = request.getAscensionCount().intValue() - df.parse( filter ).intValue();
					break;
			}
		}
		catch ( Exception e )
		{
		}

		return compareValue < 0 ? -1 : compareValue > 0 ? 1 : 0;
	}

	public String getStandardData()
	{
		// First, if you haven't retrieved a detailed
		// roster for the clan, do so.

		if ( request.responseText == null )
			request.run();

		StringBuffer strbuf = new StringBuffer();

		strbuf.append( "<html><head><title>Clan Snapshot for " );
		strbuf.append( clanName );
		strbuf.append( " (" );
		strbuf.append( new Date() );
		strbuf.append( ")</title>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		strbuf.append( "<style> body, td { font-family: sans-serif; } </style></head><body>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<center><h2>" );
		strbuf.append( clanName );
		strbuf.append( " (#" );
		strbuf.append( clanID );
		strbuf.append( ")</h2></center>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		strbuf.append( getStandardSummary() );
		strbuf.append( System.getProperty( "line.separator" ) );

		strbuf.append( "<br><br><table border=1 cellspacing=4 cellpadding=4>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<tr bgcolor=\"#000000\" style=\"color:#ffffff; font-weight: bold\"><td>Name</td>" );

		strbuf.append( getRosterHeader().replaceFirst( "<td>Ascensions</td>", "" ) );
		strbuf.append( System.getProperty( "line.separator" ) );

		Iterator memberIterator = profileMap.keySet().iterator();
		for ( int i = 1; memberIterator.hasNext(); ++i )
		{
			strbuf.append( getMemberDetail( (String) memberIterator.next() ) );
			strbuf.append( System.getProperty( "line.separator" ) );
		}

		strbuf.append( "</table></body></html>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		return strbuf.toString();
	}

	private String getStandardSummary()
	{
		String header = getRosterHeader();
		StringBuffer strbuf = new StringBuffer();

		List classList = new ArrayList();
		List foodList = new ArrayList();
		List drinkList = new ArrayList();

		List rankList = new ArrayList();
		List powerList = new ArrayList();
		List karmaList = new ArrayList();

		List meatList = new ArrayList();
		List turnsList = new ArrayList();
		List pvpList = new ArrayList();

		List musList = new ArrayList();
		List mysList = new ArrayList();
		List moxList = new ArrayList();

		List ascensionsList = new ArrayList();

		// Iterate through the list of clan members
		// and populate the lists.

		String currentMember;
		ProfileRequest memberLookup;
		Iterator memberIterator = profileMap.keySet().iterator();

		while ( memberIterator.hasNext() )
		{

			currentMember = (String) memberIterator.next();
			memberLookup = getProfile( currentMember );

			if ( header.indexOf( "<td>Class</td>" ) != -1 )
			{
				classList.add( memberLookup.getClassType() );
				if ( memberLookup.getClassType() == null )
				{
					System.out.println( memberLookup.responseText );
					System.exit(0);
				}
			}

			if ( header.indexOf( "<td>Food</td>" ) != -1 )
				foodList.add( memberLookup.getFood() );

			if ( header.indexOf( "<td>Drink</td>" ) != -1 )
				drinkList.add( memberLookup.getDrink() );

			if ( header.indexOf( "<td>Meat</td>" ) != -1 )
				meatList.add( memberLookup.getCurrentMeat() );

			if ( header.indexOf( "<td>Turns</td>" ) != -1 )
				turnsList.add( memberLookup.getTurnsPlayed() );

			if ( header.indexOf( "<td>PVP</td>" ) != -1 )
				pvpList.add( memberLookup.getPvpRank() );

			rankList.add( memberLookup.getRank() );

			musList.add( memberLookup.getMuscle() );
			mysList.add( memberLookup.getMysticism() );
			moxList.add( memberLookup.getMoxie() );
			powerList.add( memberLookup.getPower() );
			karmaList.add( memberLookup.getKarma() );

			if ( header.indexOf( "<td>Ascensions</td>" ) != -1 )
				ascensionsList.add( memberLookup.getAscensionCount() );
		}

		Collections.sort( classList );
		Collections.sort( foodList );
		Collections.sort( drinkList );
		Collections.sort( rankList );
		Collections.sort( ascensionsList );

		strbuf.append( "<table border=0 cellspacing=4 cellpadding=4><tr>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<td valign=top>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<b>Averages</b>:<ul>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		if ( header.indexOf( "<td>PVP</td>" ) != -1 )
			strbuf.append( "<li>PVP Rank: " + df.format( calculateAverage( pvpList ) ) + "</li>" );

		strbuf.append( "<li>Muscle: " + df.format( calculateAverage( musList ) ) + "</li>" );
		strbuf.append( "<li>Myst: " + df.format( calculateAverage( mysList ) ) + "</li>" );
		strbuf.append( "<li>Moxie: " + df.format( calculateAverage( moxList ) ) + "</li>" );
		strbuf.append( "<li>Power: " + df.format( calculateAverage( powerList ) ) + "</li>" );
		strbuf.append( "<li>Karma: " + df.format( calculateAverage( karmaList ) ) + "</li>" );

		if ( header.indexOf( "<td>Meat</td>" ) != -1 )
			strbuf.append( "<li>Meat: " + df.format( calculateAverage( meatList ) ) + "</li>" );

		if ( header.indexOf( "<td>Turns</td>" ) != -1 )
			strbuf.append( "<li>Turns: " + df.format( calculateAverage( turnsList ) ) + "</li>" );

		strbuf.append( "</ul>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<b>Totals</b>:<ul>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		if ( header.indexOf( "<td>PVP</td>" ) != -1 )
			strbuf.append( "<li>PVP Rank: " + df.format( calculateTotal( pvpList ) ) + "</li>" );

		strbuf.append( "<li>Muscle: " + df.format( calculateTotal( musList ) ) + "</li>" );
		strbuf.append( "<li>Myst: " + df.format( calculateTotal( mysList ) ) + "</li>" );
		strbuf.append( "<li>Moxie: " + df.format( calculateTotal( moxList ) ) + "</li>" );
		strbuf.append( "<li>Power: " + df.format( calculateTotal( powerList ) ) + "</li>" );
		strbuf.append( "<li>Karma: " + df.format( calculateTotal( karmaList ) ) + "</li>" );

		if ( header.indexOf( "<td>Meat</td>" ) != -1 )
			strbuf.append( "<li>Meat: " + df.format( calculateTotal( meatList ) ) + "</li>" );

		if ( header.indexOf( "<td>Turns</td>" ) != -1 )
			strbuf.append( "<li>Turns: " + df.format( calculateTotal( turnsList ) ) + "</li>" );

		strbuf.append( "</ul></td>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		if ( header.indexOf( "<td>Class</td>" ) != -1 )
		{
			strbuf.append( "<td valign=top><b>Class Breakdown</b>:" );
			strbuf.append( getBreakdown( classList.iterator() ) );
			strbuf.append( "</td>" );
		}

		strbuf.append( "<td valign=top><b>Rank Breakdown</b>:" );
		strbuf.append( getBreakdown( rankList.iterator() ) );
		strbuf.append( "</td>" );

		if ( header.indexOf( "<td>Food</td>" ) != -1 )
		{
			strbuf.append( "<td valign=top><b>Food Breakdown</b>:" );
			strbuf.append( getBreakdown( foodList.iterator() ) );
			strbuf.append( "</td>" );
		}

		if ( header.indexOf( "<td>Drink</td>" ) != -1 )
		{
			strbuf.append( "<td valign=top><b>Drink Breakdown</b>:" );
			strbuf.append( getBreakdown( drinkList.iterator() ) );
			strbuf.append( "</td>" );
		}

		if ( header.indexOf( "<td>Ascensions</td>" ) != -1 )
		{
			strbuf.append( "<td valign=top><b>Ascension Breakdown</b>:" );
			strbuf.append( getBreakdown( ascensionsList.iterator() ) );
			strbuf.append( "</td>" );
		}

		strbuf.append( "</tr></table>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		return strbuf.toString();
	}

	private String getMemberDetail( String memberName )
	{
		ProfileRequest memberLookup = getProfile( memberName );
		StringBuffer strbuf = new StringBuffer();

		// No matter what happens, you need to make sure
		// to print the player's name first.

		strbuf.append( "<tr><td><a href=\"profiles/" );
		strbuf.append( client.getPlayerID( memberName ) );
		strbuf.append( ".htm\">" );
		strbuf.append( client.getPlayerName( client.getPlayerID( memberName ) ) );
		strbuf.append( "</a>" );

		// Each of these are printed, pending on what
		// fields are desired in this particular table.

		String header = getRosterHeader();

		if ( header.indexOf( "<td>Lv</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getPlayerLevel() );
		}

		if ( header.indexOf( "<td>Mus</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getMuscle().intValue() ) );
		}

		if ( header.indexOf( "<td>Mys</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getMysticism().intValue() ) );
		}

		if ( header.indexOf( "<td>Mox</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getMoxie().intValue() ) );
		}

		if ( header.indexOf( "<td>Total</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getPower().intValue() ) );
		}

		if ( header.indexOf( "<td>Title</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getTitle() );
		}

		if ( header.indexOf( "<td>Rank</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getRank() );
		}

		if ( header.indexOf( "<td>Karma</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getKarma().intValue() ) );
		}

		if ( header.indexOf( "<td>PVP</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getPvpRank().intValue() ) );
		}

		if ( header.indexOf( "<td>Class</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getClassType() );
		}

		if ( header.indexOf( "<td>Meat</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getCurrentMeat().intValue() ) );
		}

		if ( header.indexOf( "<td>Turns</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getTurnsPlayed().intValue() ) );
		}

		if ( header.indexOf( "<td>Food</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getFood() );
		}

		if ( header.indexOf( "<td>Drink</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getDrink() );
		}

		if ( header.indexOf( "<td>Last Login</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getLastLoginAsString() );
		}

		if ( header.indexOf( "<td>Ascensions</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( df.format( memberLookup.getAscensionCount().intValue() ) );
		}

		strbuf.append( "</td></tr>" );
		return strbuf.toString();
	}

	public String getRosterHeader()
	{	return getProperty( "clanRosterHeader" );
	}

	public static final String getDefaultHeader()
	{
		return "<td>Lv</td><td>Mus</td><td>Mys</td><td>Mox</td><td>Total</td>" +
			"<td>Title</td><td>Rank</td><td>Karma</td><td>PVP</td><td>Class</td>" +
				"<td>Food</td><td>Drink</td><td>Last Login</td>";
	}

	private class DetailRosterRequest extends KoLRequest
	{
		public DetailRosterRequest( KoLmafia client )
		{	super( client, "clan_detailedroster.php" );
		}

		public void run()
		{
			updateDisplay( DISABLED_STATE, "Retrieving detailed roster..." );
			super.run();

			Matcher rowMatcher = Pattern.compile( "<tr>(.*?)</tr>" ).matcher( responseText.substring( responseText.indexOf( "clan_detailedroster.php" ) ) );
			rowMatcher.find();

			String currentRow;
			String currentName;
			Matcher dataMatcher;
			ProfileRequest request;

			Pattern cellPattern = Pattern.compile( "<td.*?>(.*?)</td>" );

			int lastRowIndex = 0;
			while ( rowMatcher.find( lastRowIndex ) )
			{
				lastRowIndex = rowMatcher.end();
				currentRow = rowMatcher.group(1);

				if ( !currentRow.equals( "<td height=4></td>" ) )
				{
					dataMatcher = cellPattern.matcher( currentRow );

					// The name of the player occurs in the first
					// field of the table.  Use this to index the
					// roster map.

					dataMatcher.find();
					currentName = dataMatcher.group(1).toLowerCase();
					rosterMap.put( currentName, currentRow );
				}
			}

			updateDisplay( ENABLED_STATE, "Detail roster retrieved." );
		}
	}
}
