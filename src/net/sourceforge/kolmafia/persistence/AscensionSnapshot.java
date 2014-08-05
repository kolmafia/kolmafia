/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;

import net.sourceforge.kolmafia.request.AscensionHistoryRequest;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest.AscensionDataField;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

public class AscensionSnapshot
	extends KoLDatabase
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

	private static final Map<String, String> ascensionMap = new TreeMap<String, String>();
	private static final List ascensionDataList = new ArrayList();
	private static final List softcoreAscensionList = new ArrayList();
	private static final List hardcoreAscensionList = new ArrayList();

	private static final Pattern LINK_PATTERN = Pattern.compile( "</?a[^>]+>" );

	public static final void clearCache()
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		AscensionSnapshot.ascensionMap.clear();

		AscensionSnapshot.ascensionDataList.clear();
		AscensionSnapshot.softcoreAscensionList.clear();
		AscensionSnapshot.hardcoreAscensionList.clear();
	}

	public static final void registerMember( final String playerName )
	{
		String lowerCaseName = playerName.toLowerCase();
		AscensionSnapshot.ascensionMap.put( lowerCaseName, "" );
	}

	public static final void unregisterMember( final String playerId )
	{
		String lowerCaseName = ContactManager.getPlayerName( playerId ).toLowerCase();
		AscensionSnapshot.ascensionMap.remove( lowerCaseName );
	}

	public static final Map<String, String> getAscensionMap()
	{
		return AscensionSnapshot.ascensionMap;
	}

	public static final String getAscensionData( final boolean isSoftcore, final int mostAscensionsBoardSize,
		final int mainBoardSize, final int classBoardSize, final int maxAge, final boolean playerMoreThanOnce,
		boolean localProfileLink )
	{
		AscensionSnapshot.initializeAscensionData();
		StringBuffer strbuf = new StringBuffer();

		strbuf.append( "<html><head>" );
		strbuf.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" );

		strbuf.append( "<title>" );

		if ( isSoftcore )
		{
			strbuf.append( "Softcore" );
		}
		else
		{
			strbuf.append( "Hardcore" );
		}

		String clanName = ClanManager.getClanName( true );

		strbuf.append( " Ascension Data for " );
		strbuf.append( clanName );
		strbuf.append( " (" );
		strbuf.append( new Date() );
		strbuf.append( ")</title>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "<style> body, td { font-family: sans-serif; } </style></head><body>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<center><table cellspacing=0 cellpadding=0><tr><td align=center><h2><u>" );
		strbuf.append( clanName );
		strbuf.append( " (#" );
		strbuf.append( ClanManager.getClanId() );
		strbuf.append( ")</u></h2></td></tr>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		// Right below the name of the clan, write the average
		// number of this kind of ascension.

		strbuf.append( "<tr><td align=center><h3>Avg: " );
		strbuf.append( ( ( isSoftcore ? (float) AscensionSnapshot.softcoreAscensionList.size() : 0.0f ) + AscensionSnapshot.hardcoreAscensionList.size() ) / AscensionSnapshot.ascensionMap.size() );
		strbuf.append( "</h3></td></tr></table><br><br>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		// Next, the ascension leaderboards for most (numeric)
		// ascensions.

		strbuf.append( "<table width=500 cellspacing=0 cellpadding=0>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr><td style=\"color:white\" align=center bgcolor=blue><b>Most " );
		strbuf.append( isSoftcore ? "Normal " : "Hardcore " );
		strbuf.append( "Ascensions</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr><td align=center><b>Player&nbsp;&nbsp;&nbsp;&nbsp;</b></td>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<td align=center><b>Ascensions</b></td></tr>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		// Resort the lists, and print the results to the buffer
		// so that you have the "most ascensions" leaderboard.

		AscensionHistoryRequest.setComparator( isSoftcore );
		Collections.sort( AscensionSnapshot.ascensionDataList );

		String leader;

		for ( int i = 0; i < AscensionSnapshot.ascensionDataList.size() && ( mostAscensionsBoardSize == 0 ? i < 20 : i < mostAscensionsBoardSize ); ++i )
		{
			leader = AscensionSnapshot.ascensionDataList.get( i ).toString();

			if ( !localProfileLink )
			{
				leader = AscensionSnapshot.LINK_PATTERN.matcher( leader ).replaceAll( "" );
			}

			strbuf.append( leader );
			strbuf.append( KoLConstants.LINE_BREAK );
		}

		strbuf.append( "</table></td></tr></table><br><br>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		// Finally, the ascension leaderboards for fastest
		// ascension speed.  Do this for all paths individually.

		strbuf.append( AscensionSnapshot.getPathedAscensionData(
			isSoftcore, AscensionSnapshot.OXYGENARIAN, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
			localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getPathedAscensionData(
			isSoftcore, AscensionSnapshot.TEETOTALER, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
			localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getPathedAscensionData(
			isSoftcore, AscensionSnapshot.BOOZETAFARIAN, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getPathedAscensionData(
			isSoftcore, AscensionSnapshot.NOPATH, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
			localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "</center>" );
		return strbuf.toString();
	}

	public static final String getPathedAscensionData( final boolean isSoftcore, final int pathFilter,
		final int mainBoardSize, final int classBoardSize, final int maxAge, final boolean playerMoreThanOnce,
		final boolean localProfileLink )
	{
		StringBuffer strbuf = new StringBuffer();

		// First, print the table showing the top ascenders
		// without a class-based filter.

		strbuf.append( AscensionSnapshot.getAscensionData(
			isSoftcore, pathFilter, AscensionSnapshot.NO_FILTER, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );

		// Next, print the nifty disappearing link bar that
		// is used in the KoL leaderboard frame.

		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<br><a class=small href=\"javascript:void(0);\" onClick=\"javascript: var element = document.getElementById('sec" );
		strbuf.append( pathFilter );
		strbuf.append( "'); element.style.display = element.style.display == 'inline' ? 'none' : 'inline';\">" );
		strbuf.append( "hide/show records by class</a><div id=\"sec" );
		strbuf.append( pathFilter );
		strbuf.append( "\" style=\"display:none\"><br><br>" );

		// Finally, add in all the breakdown tables, just like
		// in the KoL leaderboard frame.

		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<table><tr><td valign=top>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getAscensionData(
			isSoftcore, pathFilter, AscensionSnapshot.SEAL_CLUBBER, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "</td><td valign=top>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getAscensionData(
			isSoftcore, pathFilter, AscensionSnapshot.SAUCEROR, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "</td></tr><tr><td valign=top>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getAscensionData(
			isSoftcore, pathFilter, AscensionSnapshot.TURTLE_TAMER, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "</td><td valign=top>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getAscensionData(
			isSoftcore, pathFilter, AscensionSnapshot.DISCO_BANDIT, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "</td></tr><tr><td valign=top>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getAscensionData(
			isSoftcore, pathFilter, AscensionSnapshot.PASTAMANCER, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "</td><td valign=top>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( AscensionSnapshot.getAscensionData(
			isSoftcore, pathFilter, AscensionSnapshot.ACCORDION_THIEF, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "</td></tr></table>" );

		// Close the disappearing section and return the complete
		// code for this path filter.

		strbuf.append( "</div><br><br>" );
		return strbuf.toString();
	}

	public static final String getAscensionData( final boolean isSoftcore, final int pathFilter, final int classFilter,
		final int mainBoardSize, final int classBoardSize, final int maxAge, final boolean playerMoreThanOnce,
		boolean localProfileLink )
	{
		StringBuffer strbuf = new StringBuffer();

		AscensionDataField[] fields;

		if ( isSoftcore )
		{
			fields = new AscensionDataField[ AscensionSnapshot.softcoreAscensionList.size() ];
			AscensionSnapshot.softcoreAscensionList.toArray( fields );
		}
		else
		{
			fields = new AscensionDataField[ AscensionSnapshot.hardcoreAscensionList.size() ];
			AscensionSnapshot.hardcoreAscensionList.toArray( fields );
		}

		// First, retrieve all the ascensions which
		// satisfy the current filter so that the
		// total count can be displayed in the header.

		List resultsList = new ArrayList();

		for ( int i = 0; i < fields.length; ++i )
		{
			if ( fields[ i ].matchesFilter( isSoftcore, pathFilter, classFilter, maxAge ) )
			{
				resultsList.add( fields[ i ] );
			}
		}

		// Next, retrieve only the top ten list so that
		// a maximum of ten elements are printed.

		List leaderList = new ArrayList();
		int leaderListSize =
			classFilter == AscensionSnapshot.NO_FILTER ? ( mainBoardSize == 0 ? 10 : mainBoardSize ) : classBoardSize == 0 ? 5 : classBoardSize;

		fields = new AscensionDataField[ resultsList.size() ];
		resultsList.toArray( fields );

		for ( int i = 0; i < fields.length && leaderList.size() < leaderListSize; ++i )
		{
			if ( !leaderList.contains( fields[ i ] ) || playerMoreThanOnce )
			{
				leaderList.add( fields[ i ] );
			}
		}

		// Now that the data has been retrieved, go ahead
		// and print the table header data.

		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<table width=500 cellspacing=0 cellpadding=0>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr><td style=\"color:white\" align=center bgcolor=blue><b>" );

		switch ( classFilter )
		{
		case NO_FILTER:
			strbuf.append( "Fastest " );

			strbuf.append( isSoftcore ? "Normal " : "Hardcore " );
			strbuf.append( pathFilter == AscensionSnapshot.NO_FILTER ? "" : pathFilter == AscensionSnapshot.NOPATH ? "No-Path " : pathFilter == AscensionSnapshot.TEETOTALER ? "Teetotaler " : pathFilter == AscensionSnapshot.BOOZETAFARIAN ? "Boozetafarian " : " Oxygenarian " );

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
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr><td align=center><b>Player&nbsp;&nbsp;&nbsp;&nbsp;</b></td>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<td align=center><b>Days</b></td>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<td align=center><b>Adventures</b></td></tr>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		// Now, print the actual table data inside, using
		// the top ten list.

		String leader;

		for ( int i = 0; i < leaderListSize && i < leaderList.size(); ++i )
		{
			leader = leaderList.get( i ).toString();

			if ( !localProfileLink )
			{
				leader = AscensionSnapshot.LINK_PATTERN.matcher( leader ).replaceAll( "" );
			}

			strbuf.append( leader );
			strbuf.append( KoLConstants.LINE_BREAK );
		}

		strbuf.append( "</table></td></tr></table>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		return strbuf.toString();
	}

	private static final void initializeAscensionData()
	{
		// If the ascension lists have already been initialized,
		// then return from this method call.

		if ( !AscensionSnapshot.ascensionDataList.isEmpty() )
		{
			return;
		}

		// If the lists are not initialized, then go ahead and
		// load the appropriate data into them.

		String[] names = new String[ AscensionSnapshot.ascensionMap.size() ];
		AscensionSnapshot.ascensionMap.keySet().toArray( names );

		AscensionHistoryRequest request;
		AscensionDataField[] fields;

		for ( int i = 0; i < names.length; ++i )
		{
			request =
				AscensionHistoryRequest.getInstance(
					names[ i ], ContactManager.getPlayerId( names[ i ] ),
					(String) AscensionSnapshot.ascensionMap.get( names[ i ] ) );
			AscensionSnapshot.ascensionDataList.add( request );

			fields = new AscensionDataField[ request.getAscensionData().size() ];
			request.getAscensionData().toArray( fields );

			for ( int j = 0; j < fields.length; ++j )
			{
				if ( fields[ j ].matchesFilter(
					true, AscensionSnapshot.NO_FILTER, AscensionSnapshot.NO_FILTER, 0 ) )
				{
					AscensionSnapshot.softcoreAscensionList.add( fields[ j ] );
				}
				else
				{
					AscensionSnapshot.hardcoreAscensionList.add( fields[ j ] );
				}
			}
		}

		// Now that you've retrieved all the data from all the
		// players, sort the lists for easier loading later.

		Collections.sort( AscensionSnapshot.softcoreAscensionList );
		Collections.sort( AscensionSnapshot.hardcoreAscensionList );
	}
}
