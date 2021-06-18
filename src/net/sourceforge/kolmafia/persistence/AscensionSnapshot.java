/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.request.AscensionHistoryRequest;
import net.sourceforge.kolmafia.request.AscensionHistoryRequest.AscensionDataField;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

public class AscensionSnapshot
{
	public static final int NO_FILTER = 0;

	// It's likely this list can be refactored out of the code, with
	// AscensionPath existing.  Some day.
	public static final int UNKNOWN_PATH = -1;
	public static final int NOPATH = 998;
	public static final int TEETOTALER = 1;
	public static final int BOOZETAFARIAN = 2;
	public static final int OXYGENARIAN = 3;
	public static final int BAD_MOON = 999;
	public static final int BEES_HATE_YOU = 4;
	public static final int SURPRISING_FIST = 6;
	public static final int TRENDY = 7;
	public static final int AVATAR_OF_BORIS = 8;
	public static final int BUGBEAR_INVASION = 9;
	public static final int ZOMBIE_SLAYER = 10;
	public static final int CLASS_ACT = 11;
	public static final int AVATAR_OF_JARLSBERG = 12;
	public static final int BIG = 14;
	public static final int KOLHS = 15;
	public static final int CLASS_ACT_II = 16;
	public static final int AVATAR_OF_SNEAKY_PETE = 17;
	public static final int SLOW_AND_STEADY = 18;
	public static final int HEAVY_RAINS = 19;
	public static final int PICKY = 21;
	public static final int STANDARD = 22;
	public static final int ACTUALLY_ED_THE_UNDYING = 23;
	public static final int CRAZY_RANDOM_SUMMER = 24;
	public static final int COMMUNITY_SERVICE = 25;
	public static final int AVATAR_OF_WEST_OF_LOATHING = 26;
	public static final int THE_SOURCE = 27;
	public static final int NUCLEAR_AUTUMN = 28;
	public static final int GELATINOUS_NOOB = 29;
	public static final int LICENSE = 30;
	public static final int REPEAT = 31;
	public static final int POKEFAM = 32;
	public static final int GLOVER = 33;
	public static final int DISGUISES_DELIMIT = 34;
	public static final int DARK_GYFFTE = 35;
	public static final int CRAZY_RANDOM_SUMMER_TWO = 36;
	public static final int KINGDOM_OF_EXPLOATHING = 37;
	public static final int PATH_OF_THE_PLUMBER = 38;
	public static final int LOWKEY = 39;
	public static final int GREY_GOO = 40;
	public static final int QUANTUM = 42;

	public static final int UNKNOWN_CLASS = -1;
	public static final int SEAL_CLUBBER = 1;
	public static final int TURTLE_TAMER = 2;
	public static final int PASTAMANCER = 3;
	public static final int SAUCEROR = 4;
	public static final int DISCO_BANDIT = 5;
	public static final int ACCORDION_THIEF = 6;
	public static final int BORIS = 11;
	public static final int ZOMBIE_MASTER = 12;
	public static final int JARLSBERG = 14;
	public static final int SNEAKY_PETE = 15;
	public static final int ED = 17;
	public static final int COW_PUNCHER = 18;
	public static final int BEAN_SLINGER = 19;
	public static final int SNAKE_OILER = 20;
	public static final int NOOB = 23;
	public static final int VAMPYRE = 24;
	public static final int PLUMBER = 25;

	public static final int UNKNOWN_TYPE = -1;
	public static final int NORMAL = 1;
	public static final int HARDCORE = 2;
	public static final int CASUAL = 3;

	private static final Map<String, String> ascensionMap = new TreeMap<String, String>();
	private static final List<AscensionHistoryRequest> ascensionDataList = new ArrayList<AscensionHistoryRequest>();
	private static final List<AscensionDataField> softcoreAscensionList = new ArrayList<AscensionDataField>();
	private static final List<AscensionDataField> hardcoreAscensionList = new ArrayList<AscensionDataField>();
	private static final List<AscensionDataField> casualAscensionList = new ArrayList<AscensionDataField>();

	private static final Pattern LINK_PATTERN = Pattern.compile( "</?a[^>]+>" );

	public static final void clearCache()
	{
		// First, initialize all of the lists and
		// arrays which are used by the request.

		AscensionSnapshot.ascensionMap.clear();

		AscensionSnapshot.ascensionDataList.clear();
		AscensionSnapshot.softcoreAscensionList.clear();
		AscensionSnapshot.hardcoreAscensionList.clear();
		AscensionSnapshot.casualAscensionList.clear();
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

	public static final String getAscensionData( final int typeFilter, final int mostAscensionsBoardSize,
		final int mainBoardSize, final int classBoardSize, final int maxAge, final boolean playerMoreThanOnce,
		boolean localProfileLink )
	{
		AscensionSnapshot.initializeAscensionData();
		StringBuilder strbuf = new StringBuilder();

		strbuf.append( "<html><head>" );
		strbuf.append( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" );

		strbuf.append( "<title>" );

		switch ( typeFilter )
		{
		case AscensionSnapshot.NORMAL:
			strbuf.append( "Normal" );
			break;
		case AscensionSnapshot.HARDCORE:
			strbuf.append( "Hardcore" );
			break;
		case AscensionSnapshot.CASUAL:
			strbuf.append( "Casual" );
			break;
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
		strbuf.append( ( ( typeFilter == AscensionSnapshot.NORMAL ? (float) AscensionSnapshot.softcoreAscensionList.size() : 0.0f ) + AscensionSnapshot.hardcoreAscensionList.size() + AscensionSnapshot.casualAscensionList.size() ) / AscensionSnapshot.ascensionMap.size() );
		strbuf.append( "</h3></td></tr></table><br><br>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		// Next, the ascension leaderboards for most (numeric)
		// ascensions.

		strbuf.append( "<table width=500 cellspacing=0 cellpadding=0>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr><td style=\"color:white\" align=center bgcolor=blue><b>Most " );
		strbuf.append( typeFilter == AscensionSnapshot.NORMAL ? "Normal " : typeFilter == AscensionSnapshot.HARDCORE ? "Hardcore " : "Casual " );
		strbuf.append( "Ascensions</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid blue;\"><center><table>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<tr><td align=center><b>Player&nbsp;&nbsp;&nbsp;&nbsp;</b></td>" );
		strbuf.append( KoLConstants.LINE_BREAK );
		strbuf.append( "<td align=center><b>Ascensions</b></td></tr>" );
		strbuf.append( KoLConstants.LINE_BREAK );

		// Resort the lists, and print the results to the buffer
		// so that you have the "most ascensions" leaderboard.

		AscensionHistoryRequest.setComparator( typeFilter );
		Collections.sort( AscensionSnapshot.ascensionDataList );

		for ( int i = 0; i < AscensionSnapshot.ascensionDataList.size() && ( mostAscensionsBoardSize == 0 ? i < 20 : i < mostAscensionsBoardSize ); ++i )
		{
			String leader = AscensionSnapshot.ascensionDataList.get( i ).toString();

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

		if ( typeFilter != AscensionSnapshot.CASUAL )
		{
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.PATH_OF_THE_PLUMBER, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.KINGDOM_OF_EXPLOATHING, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.CRAZY_RANDOM_SUMMER_TWO, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.DARK_GYFFTE, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.DISGUISES_DELIMIT, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.GLOVER, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.POKEFAM, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.REPEAT, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.LICENSE, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.GELATINOUS_NOOB, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.NUCLEAR_AUTUMN, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.THE_SOURCE, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.AVATAR_OF_WEST_OF_LOATHING, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.COMMUNITY_SERVICE, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.CRAZY_RANDOM_SUMMER, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.ACTUALLY_ED_THE_UNDYING, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.STANDARD, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.PICKY, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.HEAVY_RAINS, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.SLOW_AND_STEADY, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.AVATAR_OF_SNEAKY_PETE, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.CLASS_ACT_II, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.KOLHS, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.BIG, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.AVATAR_OF_JARLSBERG, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.CLASS_ACT, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.ZOMBIE_SLAYER, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.BUGBEAR_INVASION, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.AVATAR_OF_BORIS, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.TRENDY, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.SURPRISING_FIST, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.BEES_HATE_YOU, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.BAD_MOON, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.OXYGENARIAN, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.TEETOTALER, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
				localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getPathedAscensionData(
				typeFilter, AscensionSnapshot.BOOZETAFARIAN, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
		}
		strbuf.append( AscensionSnapshot.getPathedAscensionData(
			typeFilter, AscensionSnapshot.NOPATH, mainBoardSize, classBoardSize, maxAge, playerMoreThanOnce,
			localProfileLink ) );
		strbuf.append( KoLConstants.LINE_BREAK );

		strbuf.append( "</center>" );
		return strbuf.toString();
	}

	public static final String getPathedAscensionData( final int typeFilter, final int pathFilter,
		final int mainBoardSize, final int classBoardSize, final int maxAge, final boolean playerMoreThanOnce,
		final boolean localProfileLink )
	{
		StringBuilder strbuf = new StringBuilder();

		// First, print the table showing the top ascenders
		// without a class-based filter.

		strbuf.append( AscensionSnapshot.getAscensionData(
			typeFilter, pathFilter, AscensionSnapshot.NO_FILTER, mainBoardSize, classBoardSize, maxAge,
			playerMoreThanOnce, localProfileLink ) );

		// Next, print the nifty disappearing link bar that
		// is used in the KoL leaderboard frame.

		strbuf.append( KoLConstants.LINE_BREAK );

		// Finally, add in all the breakdown tables, just like
		// in the KoL leaderboard frame, for class based paths.

		switch ( pathFilter )
		{
		case AscensionSnapshot.AVATAR_OF_BORIS:
		case AscensionSnapshot.ZOMBIE_SLAYER:
		case AscensionSnapshot.AVATAR_OF_JARLSBERG:
		case AscensionSnapshot.AVATAR_OF_SNEAKY_PETE:
		case AscensionSnapshot.ACTUALLY_ED_THE_UNDYING:
		case AscensionSnapshot.GELATINOUS_NOOB:
		case AscensionSnapshot.DARK_GYFFTE:
		case AscensionSnapshot.PATH_OF_THE_PLUMBER:
			break;
		case AscensionSnapshot.AVATAR_OF_WEST_OF_LOATHING:
			strbuf.append( "<br><a class=small href=\"javascript:void(0);\" onClick=\"javascript: var element = document.getElementById('sec" );
			strbuf.append( pathFilter );
			strbuf.append( "'); element.style.display = element.style.display == 'inline' ? 'none' : 'inline';\">" );
			strbuf.append( "hide/show records by class</a><div id=\"sec" );
			strbuf.append( pathFilter );
			strbuf.append( "\" style=\"display:none\"><br><br>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "<table><tr><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.COW_PUNCHER, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.BEAN_SLINGER, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td></tr><tr><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.SNAKE_OILER, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td></tr></table>" );
			break;
		default:
			strbuf.append( "<br><a class=small href=\"javascript:void(0);\" onClick=\"javascript: var element = document.getElementById('sec" );
			strbuf.append( pathFilter );
			strbuf.append( "'); element.style.display = element.style.display == 'inline' ? 'none' : 'inline';\">" );
			strbuf.append( "hide/show records by class</a><div id=\"sec" );
			strbuf.append( pathFilter );
			strbuf.append( "\" style=\"display:none\"><br><br>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "<table><tr><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.SEAL_CLUBBER, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.SAUCEROR, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td></tr><tr><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.TURTLE_TAMER, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.DISCO_BANDIT, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td></tr><tr><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.PASTAMANCER, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td><td valign=top>" );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( AscensionSnapshot.getAscensionData(
				typeFilter, pathFilter, AscensionSnapshot.ACCORDION_THIEF, mainBoardSize, classBoardSize, maxAge,
				playerMoreThanOnce, localProfileLink ) );
			strbuf.append( KoLConstants.LINE_BREAK );
			strbuf.append( "</td></tr></table>" );
			break;
		}

		// Close the disappearing section and return the complete
		// code for this path filter.

		strbuf.append( "</div><br><br>" );
		return strbuf.toString();
	}

	public static final String getAscensionData( final int typeFilter, final int pathFilter, final int classFilter,
		final int mainBoardSize, final int classBoardSize, final int maxAge, final boolean playerMoreThanOnce,
		boolean localProfileLink )
	{
		StringBuilder strbuf = new StringBuilder();

		AscensionDataField[] fields = null;

		switch ( typeFilter )
		{
		case AscensionSnapshot.NORMAL:
			fields = new AscensionDataField[ AscensionSnapshot.softcoreAscensionList.size() ];
			AscensionSnapshot.softcoreAscensionList.toArray( fields );
			break;
		case AscensionSnapshot.HARDCORE:
			fields = new AscensionDataField[ AscensionSnapshot.hardcoreAscensionList.size() ];
			AscensionSnapshot.hardcoreAscensionList.toArray( fields );
			break;
		case AscensionSnapshot.CASUAL:
			fields = new AscensionDataField[ AscensionSnapshot.casualAscensionList.size() ];
			AscensionSnapshot.casualAscensionList.toArray( fields );
			break;
		default:
			return "";
		}

		// First, retrieve all the ascensions which
		// satisfy the current filter so that the
		// total count can be displayed in the header.

		List<AscensionDataField> resultsList = new ArrayList<AscensionDataField>();

		for ( AscensionDataField field : fields )
		{
			if ( field.matchesFilter( typeFilter, pathFilter, classFilter, maxAge ) )
			{
				resultsList.add( field );
			}
		}

		// Next, retrieve only the top ten list so that
		// a maximum of ten elements are printed.

		List<AscensionDataField> leaderList = new ArrayList<AscensionDataField>();
		int leaderListSize =
			classFilter == AscensionSnapshot.NO_FILTER ? ( mainBoardSize == 0 ? 10 : mainBoardSize ) : classBoardSize == 0 ? 5 : classBoardSize;

		fields = new AscensionDataField[ resultsList.size() ];
		resultsList.toArray( fields );

		for ( int i = 0; i < fields.length && leaderList.size() < leaderListSize; ++i )
		{
			AscensionDataField field = fields[ i ];
			if ( !leaderList.contains( field ) || playerMoreThanOnce )
			{
				leaderList.add( field );
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

			strbuf.append( typeFilter == AscensionSnapshot.NORMAL ? "Normal " :
				       typeFilter == AscensionSnapshot.HARDCORE ? "Hardcore " :
				       "Casual " );
			strbuf.append( pathFilter == AscensionSnapshot.NO_FILTER ? "" :
				       pathFilter == AscensionSnapshot.NOPATH ? "No-Path " :
				       pathFilter == AscensionSnapshot.TEETOTALER ? "Teetotaler " :
				       pathFilter == AscensionSnapshot.BOOZETAFARIAN ? "Boozetafarian " :
				       pathFilter == AscensionSnapshot.OXYGENARIAN ? "Oxygenarian " : 
				       pathFilter == AscensionSnapshot.BAD_MOON ? "Bad Moon " : 
				       pathFilter == AscensionSnapshot.BEES_HATE_YOU ? "Bees Hate You " : 
				       pathFilter == AscensionSnapshot.SURPRISING_FIST ? "Way of the Surprising Fist " : 
				       pathFilter == AscensionSnapshot.TRENDY ? "Trendy " : 
				       pathFilter == AscensionSnapshot.AVATAR_OF_BORIS ? "Avatar of Boris " : 
				       pathFilter == AscensionSnapshot.BUGBEAR_INVASION ? "Bugbear Invasion " : 
				       pathFilter == AscensionSnapshot.ZOMBIE_SLAYER ? "Zombie Slayer " : 
				       pathFilter == AscensionSnapshot.CLASS_ACT ? "Class Act " : 
				       pathFilter == AscensionSnapshot.AVATAR_OF_JARLSBERG ? "Avatar of Jarlsberg " : 
				       pathFilter == AscensionSnapshot.BIG ? "BIG! " : 
				       pathFilter == AscensionSnapshot.KOLHS ? "KOLHS " : 
				       pathFilter == AscensionSnapshot.CLASS_ACT_II ? "Class Act II: A Class For Pigs " : 
				       pathFilter == AscensionSnapshot.AVATAR_OF_SNEAKY_PETE ? "Avatar of Sneaky Pete " : 
				       pathFilter == AscensionSnapshot.SLOW_AND_STEADY ? "Slow and Steady " : 
				       pathFilter == AscensionSnapshot.HEAVY_RAINS ? "Heavy Rains " : 
				       pathFilter == AscensionSnapshot.PICKY ? "Picky " : 
				       pathFilter == AscensionSnapshot.STANDARD ? "Standard " : 
				       pathFilter == AscensionSnapshot.ACTUALLY_ED_THE_UNDYING ? "Actually Ed the Undying " : 
				       pathFilter == AscensionSnapshot.CRAZY_RANDOM_SUMMER ? "One Crazy Random Summer " : 
				       pathFilter == AscensionSnapshot.COMMUNITY_SERVICE ? "Community Service " : 
				       pathFilter == AscensionSnapshot.AVATAR_OF_WEST_OF_LOATHING ? "Avatar of West of Loathing " : 
				       pathFilter == AscensionSnapshot.THE_SOURCE ? "The Source "  :
				       pathFilter == AscensionSnapshot.NUCLEAR_AUTUMN ? "Nuclear Autumn " :
				       pathFilter == AscensionSnapshot.GELATINOUS_NOOB ? "Gelatinous Noob " :
				       pathFilter == AscensionSnapshot.LICENSE ? "License to Adventure " :
				       pathFilter == AscensionSnapshot.REPEAT ? "Live. Ascend. Repeat. " :
				       pathFilter == AscensionSnapshot.POKEFAM ? "Pocket Familiars " :
				       pathFilter == AscensionSnapshot.GLOVER ? "G-Lover " :
				       pathFilter == AscensionSnapshot.DISGUISES_DELIMIT ? "Disguises Delimit " :
				       pathFilter == AscensionSnapshot.DARK_GYFFTE ? "Dark Gyffte " :
				       pathFilter == AscensionSnapshot.CRAZY_RANDOM_SUMMER_TWO ? "Two Crazy Random Summer " : 
				       pathFilter == AscensionSnapshot.KINGDOM_OF_EXPLOATHING ? "Kingdom of Exploathing " :
				       pathFilter == AscensionSnapshot.PATH_OF_THE_PLUMBER ? "Path of the Plumber " :
				       pathFilter == AscensionSnapshot.LOWKEY ? "Low Key Summer" :
				       "" );

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

		case COW_PUNCHER:
			strbuf.append( "Cow Puncher" );
			break;

		case BEAN_SLINGER:
			strbuf.append( "Bean Slinger" );
			break;

		case SNAKE_OILER:
			strbuf.append( "Snake Oiler" );
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

		for ( AscensionDataField field : leaderList )
		{
			String leader = field.toString();

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

	private static void initializeAscensionData()
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

		for ( String name : names )
		{
			AscensionHistoryRequest request =
				AscensionHistoryRequest.getInstance( name,
								     ContactManager.getPlayerId( name ),
								     AscensionSnapshot.ascensionMap.get( name ) );
			AscensionSnapshot.ascensionDataList.add( request );

			AscensionDataField[] fields = new AscensionDataField[ request.getAscensionData().size() ];
			request.getAscensionData().toArray( fields );

			for ( AscensionDataField field : fields )
			{
				if ( field.matchesFilter(
					AscensionSnapshot.NORMAL, AscensionSnapshot.NO_FILTER, AscensionSnapshot.NO_FILTER, 0 ) )
				{
					AscensionSnapshot.softcoreAscensionList.add( field );
				}
				else if ( field.matchesFilter(
					AscensionSnapshot.HARDCORE, AscensionSnapshot.NO_FILTER, AscensionSnapshot.NO_FILTER, 0 ) )
				{
					AscensionSnapshot.hardcoreAscensionList.add( field );
				}
				else
				{
					AscensionSnapshot.casualAscensionList.add( field );
				}
			}
		}

		// Now that you've retrieved all the data from all the
		// players, sort the lists for easier loading later.

		Collections.sort( AscensionSnapshot.softcoreAscensionList );
		Collections.sort( AscensionSnapshot.hardcoreAscensionList );
		Collections.sort( AscensionSnapshot.casualAscensionList );
	}
}
