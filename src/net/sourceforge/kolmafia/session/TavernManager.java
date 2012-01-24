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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.TavernRequest;

import net.sourceforge.kolmafia.swingui.CouncilFrame;

public class TavernManager
{
	/**
	 * Completes the infamous tavern quest.
	 */

	//  1<--2<--3<--4<--5
	//  v
	//  6   7-->8   9->10
	//  v   ^   v   ^   v
	// 11  12  13  14  15
	//  v       v   ^   v
	// 16  17  18->19  20
	//  v   ^ \
	// 21->22  23->24->25

	private static Integer [] searchOrder = {
		// new Integer(  5 ),
		new Integer(  4 ), new Integer(  3 ), new Integer(  2 ),
		new Integer(  1 ), new Integer(  6 ), new Integer( 11 ),
		new Integer( 16 ), new Integer( 21 ), new Integer( 22 ),
		new Integer( 17 ), new Integer( 23 ), new Integer( 24 ),
		new Integer( 25 ), new Integer( 12 ), new Integer(  7 ),
		new Integer(  8 ), new Integer( 13 ), new Integer( 18 ),
		new Integer( 19 ), new Integer( 14 ), new Integer(  9 ),
		new Integer( 10 ), new Integer( 15 ), new Integer( 20 ),
	};

	public static int locateTavernFaucet()
	{
		if ( KoLCharacter.getLevel() < 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need to level up first." );
			return -1;
		}

		// See if we've already found the faucet within KoLmafia
		TavernRequest.validateFaucetQuest();

		String layout = Preferences.getString( "tavernLayout" );

		int faucet = layout.indexOf( "3" );
		int baron = layout.indexOf( "4" );

		TavernManager.logSpecialSquares( faucet, baron );

		if ( faucet != -1 )
		{
			return faucet + 1;
		}

		// No. Go look for it.
		KoLmafia.updateDisplay( "Searching for faucet..." );

		// Make sure we have the quest from the council
		RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );

		// Make sure Bart Ender has given us access to the cellar
		RequestThread.postRequest( new GenericRequest( "tavern.php?place=barkeep" ) );
		// *** Should look at response and make sure we got there

		// Visit the tavern cellar to update the layout
		RequestThread.postRequest( new GenericRequest( "cellar.php" ) );

		// Refetch the current layout
		layout = Preferences.getString( "tavernLayout" );

		// Re-check faucet and baron
		faucet = layout.indexOf( "3" );
		baron = layout.indexOf( "4" );

		TavernManager.logSpecialSquares( faucet, baron );

		// See if we've already found the faucet outside KoLmafia
		if ( faucet != -1 )
		{
			return faucet + 1;
		}

		// If the faucet has not yet been found, then go through
		// the process of trying to locate it.

		AdventureRequest request = new AdventureRequest( "Typical Tavern Cellar", "cellar.php", "" );

		while ( faucet == -1 &&
			KoLmafia.permitsContinue() &&
			KoLCharacter.getCurrentHP() > 0 &&
			KoLCharacter.getAdventuresLeft() > 0 )
		{
			// The request will visit the next unexplored sport
			RequestThread.postRequest( request );
			faucet = Preferences.getString( "tavernLayout" ).indexOf( "3" );
		}

		if ( faucet == -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to find faucet." );
			return -1;
		}

		// Otherwise, you've found it!

		// Visit Bart Ender to claim reward
		RequestThread.postRequest( new GenericRequest( "tavern.php?place=barkeep" ) );

		// Notify the user that the faucet has been found.

		int row = faucet / 5 + 1;
		int column = faucet % 5 + 1;

		KoLmafia.updateDisplay( "Faucet found in row " + row + ", column " + column );

		return faucet + 1;
	}

	private static void logSpecialSquares( final int faucet, final int baron )
	{
		if ( faucet != -1 )
		{
			int faucetRow = faucet / 5 + 1;
			int faucetColumn = faucet % 5 + 1;

			KoLmafia.updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
		}

		if ( baron != -1 )
		{
			int baronRow = baron / 5 + 1;
			int baronColumn = baron % 5 + 1;

			KoLmafia.updateDisplay( "Baron found in row " + baronRow + ", column " + baronColumn );
		}
	}

	private static ArrayList getSearchList( final String layout )
	{
		ArrayList searchList = new ArrayList();

		for ( int i = 0; i < TavernManager.searchOrder.length; ++i )
		{
			searchList.add( TavernManager.searchOrder[ i ] );
		}

		for ( int i = layout.length() - 1; i >= 0; --i )
		{
			if ( layout.charAt( i )  == '0' )
			{
				continue;
			}

			// Remove explored square from searchlist
			int index = searchList.indexOf( new Integer( i + 1 ) );
			if ( index != -1 )
			{
				searchList.remove( index );
			}
		}

		return searchList;
	}

	public static int recommendSquare()
	{
		if ( KoLCharacter.getLevel() < 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need to level up first." );
			return 0;
		}

		// See if we've already found the faucet within KoLmafia
		TavernRequest.validateFaucetQuest();

		String layout = Preferences.getString( "tavernLayout" );
		int faucet = layout.indexOf( "3" );

		// See if any squares are unexplored
		if ( layout.indexOf( "0" ) == -1 )
		{
			// All squares are explored. Return the square with the faucet
			return faucet + 1;
		}

		// Some squares remain to be visited. Get a list of them in order.
		ArrayList searchList = TavernManager.getSearchList( layout );
		if ( searchList.size() == 0 )
		{
			// Should never happen
			return 0;
		}

		// Take the first square off of the list
		Integer searchIndex = (Integer) searchList.remove( 0 );

		// That's the square we will visit.
		return searchIndex.intValue();
	}
}
