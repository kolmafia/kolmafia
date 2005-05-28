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

public class ClanSnapshotTable implements KoLConstants
{
	private KoLmafia client;
	private String clanID;
	private String clanName;
	private TreeMap profileMap;
	private TreeMap rosterMap;

	public ClanSnapshotTable( KoLmafia client, String clanID, String clanName, TreeMap profileMap )
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		this.client = client;
		this.clanID = clanID;
		this.clanName = clanName;
		this.profileMap = profileMap;

		rosterMap = new TreeMap();

		// Next, retrieve a detailed copy of the clan
		// roster to complete initialization.

		(new DetailRosterRequest( client )).run();
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
		DetailRosterField rosterLookup;
		Iterator memberIterator = profileMap.keySet().iterator();

		while ( memberIterator.hasNext() )
		{
			currentMember = (String) memberIterator.next();
			memberLookup = (ProfileRequest) profileMap.get( currentMember );
			rosterLookup = (DetailRosterField) rosterMap.get( currentMember );

			classList.add( memberLookup.getClassType() );
			foodList.add( memberLookup.getFood() );
			drinkList.add( memberLookup.getDrink() );

			meatList.add( String.valueOf( memberLookup.getCurrentMeat() ) );
			turnsList.add( String.valueOf( memberLookup.getTurnsPlayed() ) );

			pvpList.add( memberLookup.getPvpRank() );
			rankList.add( rosterLookup.rank );

			musList.add( rosterLookup.mus );
			mysList.add( rosterLookup.mys );
			moxList.add( rosterLookup.mox );
			powerList.add( rosterLookup.power );
			karmaList.add( rosterLookup.karma );
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
		DetailRosterField rosterLookup = (DetailRosterField) rosterMap.get( memberName );

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
			strbuf.append( rosterLookup.mus );
		}

		if ( header.indexOf( "<td>Mys</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( rosterLookup.mys );
		}

		if ( header.indexOf( "<td>Mox</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( rosterLookup.mox );
		}

		if ( header.indexOf( "<td>Total</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( rosterLookup.power );
		}

		if ( header.indexOf( "<td>Title</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( rosterLookup.title );
		}

		if ( header.indexOf( "<td>Rank</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( rosterLookup.rank );
		}

		if ( header.indexOf( "<td>Karma</td>" ) != -1 )
		{
			strbuf.append( "</td><td>" );
			strbuf.append( rosterLookup.karma );
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
		return client.getSettings().getProperty( "clanRosterHeader" ) != null ? client.getSettings().getProperty( "clanRosterHeader" ) :
			"<td>Lv</td><td>Mus</td><td>Mys</td><td>Mox</td><td>Total</td><td>Title</td><td>Rank</td><td>Karma</td><td>PVP</td><td>Class</td><td>Food</td><td>Drink</td><td>Last Login</td>";
	}

	private class DetailRosterRequest extends KoLRequest
	{
		public DetailRosterRequest( KoLmafia client )
		{	super( client, "clan_detailedroster.php" );
		}

		public void run()
		{
			super.run();

			DetailRosterField currentMember;
			Matcher rowMatcher = Pattern.compile( "<tr>(.*?)</tr>" ).matcher( responseText );

			rowMatcher.find( 0 );
			int lastRowIndex = rowMatcher.end();

			while ( rowMatcher.find( lastRowIndex ) )
			{
				lastRowIndex = rowMatcher.end();

				if ( !rowMatcher.group(1).equals( "<td height=4></td>" ) )
				{
					currentMember = new DetailRosterField( rowMatcher.group(1) );
					rosterMap.put( currentMember.name.toLowerCase(), currentMember );
				}
			}
		}
	}

	private class DetailRosterField
	{
		private String name;
		private String mus, mys, mox, power;
		private String title, rank, karma;

		public DetailRosterField( String tableRow )
		{
			int firstCellIndex = tableRow.indexOf( "</td>" );
			this.name = tableRow.substring( 4, firstCellIndex );

			Matcher dataMatcher = Pattern.compile( "<td.*?>(.*?)</td>" ).matcher( tableRow.substring( firstCellIndex ) );

			dataMatcher.find();
			this.mus = dataMatcher.group(1);

			dataMatcher.find( dataMatcher.end() );
			this.mys = dataMatcher.group(1);
			dataMatcher.find( dataMatcher.end() );
			this.mox = dataMatcher.group(1);
			dataMatcher.find( dataMatcher.end() );
			this.power = dataMatcher.group(1);

			dataMatcher.find( dataMatcher.end() );
			this.title = dataMatcher.group(1);
			dataMatcher.find( dataMatcher.end() );
			this.rank = dataMatcher.group(1);
			dataMatcher.find( dataMatcher.end() );
			this.karma = dataMatcher.group(1);
		}
	}
}
