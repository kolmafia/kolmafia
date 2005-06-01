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
import java.util.TreeMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;

import net.java.dev.spellcast.utilities.LockableListModel;

public class ClanSnapshotTable implements KoLConstants
{
	public static final int EXACT_MATCH = 0;
	public static final int BELOW_MATCH = -1;
	public static final int ABOVE_MATCH = 1;

	public static final int LV_FILTER = 1;
	public static final int MUS_FILTER = 2;
	public static final int MYS_FILTER = 3;
	public static final int MOX_FILTER = 4;
	public static final int POWER_FILTER = 5;
	public static final int PVP_FILTER = 6;
	public static final int CLASS_FILTER = 7;
	public static final int RANK_FILTER = 8;
	public static final int KARMA_FILTER = 9;
	public static final int MEAT_FILTER = 10;
	public static final int TURN_FILTER = 11;
	public static final int LOGIN_FILTER = 12;

	private KoLmafia client;
	private String clanID;
	private String clanName;
	private TreeMap profileMap;
	private LockableListModel filterList;

	public ClanSnapshotTable( KoLmafia client, String clanID, String clanName, TreeMap profileMap )
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		this.client = client;
		this.clanID = clanID;
		this.clanName = clanName;
		this.profileMap = profileMap;
		this.filterList = new LockableListModel();

		// Next, retrieve a detailed copy of the clan
		// roster to complete initialization.

		(new DetailRosterRequest( client )).run();
	}

	public LockableListModel getFilteredList()
	{	return filterList;
	}

	public void applyFilter( int matchType, int filterType, String filter )
	{
		filterList.clear();
		filterList.addAll( profileMap.values() );

		// This is where the filter gets applied, but for
		// now, filtering isn't implemented so just return
		// the entire list of members.

		ProfileRequest [] profileArray = new ProfileRequest[ filterList.size() ];
		filterList.toArray( profileArray );

		int compareValue = 0;
		try
		{
			for ( int i = profileArray.length - 1; i >= 0; --i )
			{
				switch ( filterType )
				{
					case LV_FILTER:
						compareValue = profileArray[i].getPlayerLevel() - df.parse( filter ).intValue();
						break;

					case MUS_FILTER:
						compareValue = Integer.parseInt( profileArray[i].getMuscle() ) - df.parse( filter ).intValue();
						break;

					case MYS_FILTER:
						compareValue = Integer.parseInt( profileArray[i].getMysticism() ) - df.parse( filter ).intValue();
						break;

					case MOX_FILTER:
						compareValue = Integer.parseInt( profileArray[i].getMoxie() ) - df.parse( filter ).intValue();
						break;

					case POWER_FILTER:
						compareValue = Integer.parseInt( profileArray[i].getPower() ) - df.parse( filter ).intValue();
						break;

					case PVP_FILTER:
						compareValue = Integer.parseInt( profileArray[i].getPvpRank().startsWith( "&" ) ? "0" : profileArray[i].getPvpRank() ) -
							df.parse( (String) filter ).intValue();
						break;

					case CLASS_FILTER:
						compareValue = profileArray[i].getClassType().compareToIgnoreCase( filter );
						break;

					case RANK_FILTER:
						compareValue = profileArray[i].getRank().compareToIgnoreCase( filter );
						break;

					case KARMA_FILTER:
						compareValue = Integer.parseInt( profileArray[i].getKarma() ) - df.parse( filter ).intValue();
						break;

					case MEAT_FILTER:
						compareValue = profileArray[i].getCurrentMeat() - df.parse( filter ).intValue();
						break;

					case TURN_FILTER:
						compareValue = profileArray[i].getTurnsPlayed() - df.parse( filter ).intValue();
						break;

					case LOGIN_FILTER:

						try
						{
							int daysIdle = df.parse( filter ).intValue();
							long millisecondsIdle = 86400000L * daysIdle;
							Date cutoffDate = new Date( System.currentTimeMillis() - millisecondsIdle );

							compareValue = profileArray[i].getLastLogin().after( cutoffDate ) ? -1 :
								 profileArray[i].getLastLogin().before( cutoffDate ) ? 1 : 0;
						}
						catch ( Exception e )
						{
						}

						break;
				}

				compareValue = compareValue < 0 ? -1 : compareValue > 0 ? 1 : 0;

				// If the comparison value does not match the match
				// type desired, remove the element from the list

				if ( compareValue != matchType )
					filterList.remove( i );
			}
		}
		catch ( Exception e )
		{
			// An exception shouldn't occur during the parsing
			// process, unless the user did not enter a valid
			// numeric string.  In this case, nothing is added,
			// which is exactly what's wanted.
		}
	}

	public String toString()
	{
		StringBuffer strbuf = new StringBuffer();

		strbuf.append( "<html><head><style> body, td { font-family: sans-serif; } </style></head><body>" );
		strbuf.append( "<center><h1>" + clanName + " (#" + clanID + ")</h1></center>" );

		strbuf.append( getSummary() );
		strbuf.append( "<br><br>" );

		strbuf.append( "<table>" );
		strbuf.append( "<tr bgcolor=\"#000000\" style=\"color:#ffffff; font-weight: bold\"><td>Name</td>" );
		strbuf.append( getHeader() );

		Iterator memberIterator = profileMap.keySet().iterator();
		for ( int i = 1; memberIterator.hasNext(); ++i )
			strbuf.append( getMemberDetail( (String) memberIterator.next() ) );

		strbuf.append( "</table></body></html>" );
		return strbuf.toString();
	}

	private String getSummary()
	{
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

		// Once that's complete, iterate through the
		// clan members and initialize the lists.

		String currentMember;
		ProfileRequest memberLookup;
		Iterator memberIterator = profileMap.keySet().iterator();

		while ( memberIterator.hasNext() )
		{
			currentMember = (String) memberIterator.next();
			memberLookup = (ProfileRequest) profileMap.get( currentMember );

			classList.add( memberLookup.getClassType() );
			foodList.add( memberLookup.getFood() );
			drinkList.add( memberLookup.getDrink() );

			meatList.add( String.valueOf( memberLookup.getCurrentMeat() ) );
			turnsList.add( String.valueOf( memberLookup.getTurnsPlayed() ) );

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

		strbuf.append( "<table border=0 cellspacing=4 cellpadding=4><tr>" );
		strbuf.append( "<td valign=top><b>Averages</b>:" );
		strbuf.append( "<ul><li>PVP Rank: " + df.format( calculateAverage( pvpList ) ) + "</li>" );
		strbuf.append( "<li>Muscle: " + df.format( calculateAverage( musList ) ) + "</li>" );
		strbuf.append( "<li>Myst: " + df.format( calculateAverage( mysList ) ) + "</li>" );
		strbuf.append( "<li>Moxie: " + df.format( calculateAverage( moxList ) ) + "</li>" );
		strbuf.append( "<li>Power: " + df.format( calculateAverage( powerList ) ) + "</li>" );
		strbuf.append( "<li>Karma: " + df.format( calculateAverage( karmaList ) ) + "</li>" );
		strbuf.append( "<li>Meat: " + df.format( calculateAverage( meatList ) ) + "</li>" );
		strbuf.append( "<li>Turns: " + df.format( calculateAverage( turnsList ) ) + "</li>" );
		strbuf.append( "</ul><b>Totals</b>:" );
		strbuf.append( "<ul><li>Muscle: " + df.format( calculateTotal( musList ) ) + "</li>" );
		strbuf.append( "<li>Myst: " + df.format( calculateTotal( mysList ) ) + "</li>" );
		strbuf.append( "<li>Moxie: " + df.format( calculateTotal( moxList ) ) + "</li>" );
		strbuf.append( "<li>Power: " + df.format( calculateTotal( powerList ) ) + "</li>" );
		strbuf.append( "<li>Karma: " + df.format( calculateTotal( karmaList ) ) + "</li>" );
		strbuf.append( "<li>Meat: " + df.format( calculateTotal( meatList ) ) + "</li>" );
		strbuf.append( "<li>Turns: " + df.format( calculateTotal( turnsList ) ) + "</li>" );
		strbuf.append( "</ul></td>" );

		strbuf.append( "<td valign=top><b>Class Breakdown</b>:" );
		strbuf.append( getBreakdown( classList.iterator() ) );
		strbuf.append( "</td><td valign=top><b>Rank Breakdown</b>:" );
		strbuf.append( getBreakdown( rankList.iterator() ) );
		strbuf.append( "</td><td valign=top><b>Food Breakdown</b>:" );
		strbuf.append( getBreakdown( foodList.iterator() ) );
		strbuf.append( "</td><td valign=top><b>Drink Breakdown</b>:" );
		strbuf.append( getBreakdown( drinkList.iterator() ) );

		strbuf.append( "</td></tr></table>" );

		return strbuf.toString();
	}

	private String getBreakdown( Iterator itemIterator )
	{
		StringBuffer strbuf = new StringBuffer();

		int maximumCount = 0;
		int currentCount = 0;
		Object currentItem = itemIterator.next();
		Object favorite = currentItem;
		Object nextItem;

		strbuf.append( "<ul>" );

		while ( itemIterator.hasNext() )
		{
			++currentCount;
			nextItem = itemIterator.next();
			if ( !currentItem.equals( nextItem ) )
			{
				strbuf.append( "<li>" + currentItem.toString() + ": " + currentCount + "</li>" );
				if ( currentCount > maximumCount )
				{
					maximumCount = currentCount;
					favorite = currentItem;
				}

				currentItem = nextItem;
				currentCount = 0;
			}
		}

		strbuf.append( "<li>" + currentItem.toString() + ": " + (currentCount + 1) + "</li>" );
		if ( currentCount > maximumCount )
			favorite = currentItem;

		strbuf.append( "</ul><hr width=\"80%\"><b>Favorite</b>: " + favorite.toString() );

		return strbuf.toString();
	}

	private int calculateTotal( List values )
	{
		int total = 0;
		String currentValue;

		for ( int i = 0; i < values.size(); ++i )
		{
			currentValue = (String) values.get(i);
			if ( !currentValue.startsWith( "&" ) )
				total += Integer.parseInt( (String) values.get(i) );
		}

		return total;
	}

	private int calculateAverage( List values )
	{
		int total = 0;
		String currentValue;
		int actualSize = values.size();

		for ( int i = 0; i < values.size(); ++i )
		{
			currentValue = (String) values.get(i);
			if ( currentValue.startsWith( "&" ) )
				--actualSize;
			else
				total += Integer.parseInt( (String) values.get(i) );
		}

		return actualSize == 0 ? 0 : total / actualSize;
	}

	private String getMemberDetail( String memberName )
	{
		ProfileRequest memberLookup = (ProfileRequest) profileMap.get( memberName );
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

		String header = getHeader();

		if ( header.indexOf( "<td>Lv</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getPlayerLevel() );
		}

		if ( header.indexOf( "<td>Mus</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getMuscle() );
		}

		if ( header.indexOf( "<td>Mys</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getMysticism() );
		}

		if ( header.indexOf( "<td>Mox</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getMoxie() );
		}

		if ( header.indexOf( "<td>Total</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getPower() );
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
			strbuf.append( memberLookup.getKarma() );
		}

		if ( header.indexOf( "<td>PVP</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getPvpRank() );
		}

		if ( header.indexOf( "<td>Class</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getClassType() );
		}

		if ( header.indexOf( "<td>Meat</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getCurrentMeat() );
		}

		if ( header.indexOf( "<td>Turns</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( memberLookup.getTurnsPlayed() );
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

		strbuf.append( "</td></tr>" );
		return strbuf.toString();
	}

	private String getHeader()
	{
		String tableHeaderSetting = client.getSettings().getProperty( "clanRosterHeader" );
		return tableHeaderSetting == null ? ClanSnapshotTable.getDefaultHeader() : tableHeaderSetting;
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
			ProfileRequest currentRequest;

			Pattern cellPattern = Pattern.compile( "<td.*?>(.*?)</td>" );

			int lastRowIndex = rowMatcher.end();
			while ( rowMatcher.find( lastRowIndex ) )
			{
				lastRowIndex = rowMatcher.end();
				currentRow = rowMatcher.group(1);

				if ( !currentRow.equals( "<td height=4></td>" ) )
				{
					dataMatcher = cellPattern.matcher( currentRow );

					dataMatcher.find();
					currentName = dataMatcher.group(1);

					currentRequest = (ProfileRequest) profileMap.get( currentName.toLowerCase() );

					dataMatcher.find( dataMatcher.end() );
					currentRequest.setMuscle( dataMatcher.group(1) );
					dataMatcher.find( dataMatcher.end() );
					currentRequest.setMysticism( dataMatcher.group(1) );
					dataMatcher.find( dataMatcher.end() );
					currentRequest.setMoxie( dataMatcher.group(1) );

					dataMatcher.find();
					dataMatcher.find( dataMatcher.end() );
					currentRequest.setTitle( dataMatcher.group(1) );
					dataMatcher.find( dataMatcher.end() );
					currentRequest.setRank( dataMatcher.group(1) );
					dataMatcher.find( dataMatcher.end() );
					currentRequest.setKarma( dataMatcher.group(1) );
				}
			}
		}
	}
}
