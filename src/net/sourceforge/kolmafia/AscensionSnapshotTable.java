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

public class AscensionSnapshotTable extends KoLDatabase
{
	public static final int NO_FILTER = 0;

	public static final int NOPATH = 1;
	public static final int TEETOTALER = 2;
	public static final int BOOZETAFARIAN = 3;
	public static final int OXYGENARIAN = 4;

	public static final int SEAL_CLUBBER = 1;
	public static final int TURTLE_TAMER = 2;
	public static final int PASTAMANCER = 3;
	public static final int SAUCEROR = 4;
	public static final int DISCO_BANDIT = 5;
	public static final int ACCORDION_THIEF = 6;

	private KoLmafia client;
	private String clanID;
	private String clanName;
	private Map ascensionMap;

	private List softcoreAscensionList;
	private List hardcoreAscensionList;

	public AscensionSnapshotTable( KoLmafia client )
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		this.client = client;
		this.clanID = clanID;
		this.clanName = clanName;
		this.ascensionMap = new TreeMap();

		this.softcoreAscensionList = new ArrayList();
		this.hardcoreAscensionList = new ArrayList();
	}

	public void registerMember( String playerName )
	{
		String lowerCaseName = playerName.toLowerCase();
		ascensionMap.put( lowerCaseName, "" );
	}

	public void unregisterMember( String playerID )
	{
		String lowerCaseName = client.getPlayerName( playerID ).toLowerCase();
		ascensionMap.remove( lowerCaseName );
	}

	public void setClanID( String clanID )
	{	this.clanID = clanID;
	}

	public void setClanName( String clanName )
	{	this.clanName = clanName;
	}

	public Map getAscensionMap()
	{	return ascensionMap;
	}

	public String getAscensionData( boolean isSoftcore )
	{
		initializeAscensionData();
		StringBuffer strbuf = new StringBuffer();

		strbuf.append( "<html><head><title>" );

		if ( isSoftcore )
			strbuf.append( "Softcore" );
		else
			strbuf.append( "Hardcore" );

		strbuf.append( " Ascension Data for " );
		strbuf.append( clanName );
		strbuf.append( " (" );
		strbuf.append( new Date() );
		strbuf.append( ")</title>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		strbuf.append( "<style> body, td { font-family: sans-serif; } </style></head><body>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<center><table cellspacing=0 cellpadding=0><tr><td align=center><h2><u>" );
		strbuf.append( clanName );
		strbuf.append( " (#" );
		strbuf.append( clanID );
		strbuf.append( ")</u></h2></td></tr>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		// Right below the name of the clan, write the average
		// number of this kind of ascension.

		strbuf.append( "<tr><td align=center><h3>Avg: " );
		strbuf.append( ((isSoftcore ? (double)softcoreAscensionList.size() : 0.0) + (double)hardcoreAscensionList.size()) / (double)ascensionMap.size() );
		strbuf.append( "</h3></td></tr></table><br><br><br><br>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		strbuf.append( getAscensionData( isSoftcore, NO_FILTER ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, OXYGENARIAN ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, TEETOTALER ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, BOOZETAFARIAN ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, NOPATH ) );
		strbuf.append( System.getProperty( "line.separator" ) );

		strbuf.append( "</center>" );
		return strbuf.toString();
	}

	public String getAscensionData( boolean isSoftcore, int pathFilter )
	{
		StringBuffer strbuf = new StringBuffer();

		// First, print the table showing the top ascenders
		// without a class-based filter.

		strbuf.append( getAscensionData( isSoftcore, pathFilter, NO_FILTER ) );

		// Next, print the nifty disappearing link bar that
		// is used in the KoL leaderboard frame.

		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<br><a class=small href=\"javascript:void(0);\" onClick=\"javascript: var element = document.getElementById('sec" );
		strbuf.append( pathFilter );
		strbuf.append( "'); element.style.display = element.style.display == 'inline' ? 'none' : 'inline';\">" );
		strbuf.append( "hide/show records by class</a><div id=\"sec" );
		strbuf.append( pathFilter );
		strbuf.append( "\" style=\"display:none\">" );

		// Finally, add in all the breakdown tables, just like
		// in the KoL leaderboard frame.

		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<table><tr><td valign=top>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, pathFilter, SEAL_CLUBBER ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "</td><td valign=top>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, pathFilter, SAUCEROR ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "</td></tr><tr><td valign=top>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, pathFilter, TURTLE_TAMER ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "</td><td valign=top>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, pathFilter, DISCO_BANDIT ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "</td></tr><tr><td valign=top>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, pathFilter, PASTAMANCER ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "</td><td valign=top>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( getAscensionData( isSoftcore, pathFilter, ACCORDION_THIEF ) );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "</td></tr></table>" );

		// Close the disappearing section and return the complete
		// code for this path filter.

		strbuf.append( "</div><br><br>" );
		return strbuf.toString();
	}

	public String getAscensionData( boolean isSoftcore, int pathFilter, int classFilter )
	{
		StringBuffer strbuf = new StringBuffer();

		Iterator fieldIterator;
		AscensionDataRequest.AscensionDataField currentField;

		// First, retrieve all the ascensions which
		// satisfy the current filter so that the
		// total count can be displayed in the header.

		List resultsList = new ArrayList();
		fieldIterator = (isSoftcore ? softcoreAscensionList : hardcoreAscensionList).iterator();

		while ( fieldIterator.hasNext() )
		{
			currentField = (AscensionDataRequest.AscensionDataField) fieldIterator.next();
			if ( currentField.matchesFilter( isSoftcore, pathFilter, classFilter ) )
				resultsList.add( currentField );
		}

		// Next, retrieve only the top ten list so that
		// a maximum of ten elements are printed.

		List leaderList = new ArrayList();
		int leaderListSize = classFilter == NO_FILTER ? 10 : 5;

		fieldIterator = resultsList.iterator();

		while ( fieldIterator.hasNext() && leaderList.size() < leaderListSize )
		{
			currentField = (AscensionDataRequest.AscensionDataField) fieldIterator.next();
			if ( !leaderList.contains( currentField ) )
				leaderList.add( currentField );
		}

		// Now that the data has been retrieved, go ahead
		// and print the table header data.

		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<table width=500 cellspacing=0 cellpadding=0>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<tr><td style=\"color:white\" align=center bgcolor=blue><b>" );

		switch ( classFilter )
		{
			case NO_FILTER:
				strbuf.append( "Fastest " );

				strbuf.append( isSoftcore ? "Normal " : "Hardcore " );
				strbuf.append( pathFilter == NO_FILTER ? "" : pathFilter == NOPATH ? "No-Path " :
					pathFilter == TEETOTALER ? "Teetotaler " : pathFilter == BOOZETAFARIAN ? "Boozetafarian " : " Oxygenarian " );

				strbuf.append( "Ascensions (Out of " );
				strbuf.append( resultsList.size() );
				strbuf.append( ")" );
				break;

			case SEAL_CLUBBER:
				strbuf.append( "Seal Clubber" );
				break;

			case TURTLE_TAMER:
				strbuf.append( "Turtle Tamer" );
				break;

			case PASTAMANCER:
				strbuf.append( "Pastamancer" );
				break;

			case SAUCEROR:
				strbuf.append( "Sauceror" );
				break;

			case DISCO_BANDIT:
				strbuf.append( "Disco Bandit" );
				break;

			case ACCORDION_THIEF:
				strbuf.append( "Accordion Thief" );
				break;
		}

		strbuf.append( "</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<tr><td align=center><b>Player&nbsp;&nbsp;&nbsp;&nbsp;</b></td>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<td align=center><b>Days</b></td>" );
		strbuf.append( System.getProperty( "line.separator" ) );
		strbuf.append( "<td align=center><b>Adventures</b></td></tr>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		// Now, print the actual table data inside, using
		// the top ten list.

		for ( int i = 0; i < leaderListSize && i < leaderList.size(); ++i )
		{
			strbuf.append( leaderList.get(i).toString() );
			strbuf.append( System.getProperty( "line.separator" ) );
		}

		strbuf.append( "</table></td></tr></table>" );
		strbuf.append( System.getProperty( "line.separator" ) );

		return strbuf.toString();
	}

	private void initializeAscensionData()
	{
		// If the ascension lists have already been initialized,
		// then return from this method call.

		if ( !softcoreAscensionList.isEmpty() && !hardcoreAscensionList.isEmpty() )
			return;

		// If the lists are not initialized, then go ahead and
		// load the appropriate data into them.

		String currentName;
		Iterator nameIterator = ascensionMap.keySet().iterator();

		AscensionDataRequest.AscensionDataField field;
		Iterator ascensionIterator;

		while ( nameIterator.hasNext() )
		{
			currentName = (String) nameIterator.next();
			ascensionIterator = AscensionDataRequest.getInstance( currentName, client.getPlayerID( currentName ),
				(String) ascensionMap.get( currentName ) ).getAscensionData().iterator();

			while ( ascensionIterator.hasNext() )
			{
				field = (AscensionDataRequest.AscensionDataField) ascensionIterator.next();

				if ( field.matchesFilter( true, NO_FILTER, NO_FILTER ) )
					softcoreAscensionList.add( field );
				else
					hardcoreAscensionList.add( field );
			}
		}

		// Now that you've retrieved all the data from all the
		// players, sort the lists for easier loading later.

		Collections.sort( softcoreAscensionList );
		Collections.sort( hardcoreAscensionList );
	}
}