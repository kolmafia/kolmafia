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

	private static final int EXPLORE = 1;
	private static final int FAUCET = 2;
	private static final int BARON = 3;
	private static final int FIGHT_BARON = 4;

	private static int overrideSquare = -1;

	public static int exploreTavern()
	{
		return TavernManager.exploreTavern( TavernManager.EXPLORE );
	}

	public static int locateTavernFaucet()
	{
		return TavernManager.exploreTavern( TavernManager.FAUCET );
	}

	public static int locateBaron()
	{
		return TavernManager.exploreTavern( TavernManager.BARON );
	}

	public static int fightBaron()
	{
		return TavernManager.exploreTavern( TavernManager.FIGHT_BARON );
	}

	private static int exploreTavern( final int goal )
	{
		if ( KoLCharacter.getLevel() < 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need to level up first." );
			return -1;
		}

		// See if we've already found our goal within KoLmafia
		TavernRequest.validateFaucetQuest();

		String layout = Preferences.getString( "tavernLayout" );

		int faucet = layout.indexOf( "3" );
		if ( goal == TavernManager.FAUCET && faucet != -1 )
		{
			TavernManager.logFaucetSquare( faucet );
			return faucet + 1;
		}

		int baron = layout.indexOf( "4" );
		if ( goal == TavernManager.BARON && baron != -1 )
		{
			TavernManager.logBaronSquare( baron );
			return baron + 1;
		}

		int unexplored = layout.indexOf( "0" );
		if ( goal == TavernManager.EXPLORE && unexplored == -1 )
		{
			TavernManager.logFaucetSquare( faucet );
			TavernManager.logBaronSquare( baron );
			KoLmafia.updateDisplay( "Entire cellar explored" );
			return 0;
		}

		// No. Go look for it.
		String message =
			goal == TavernManager.FAUCET ?
			"Searching for faucet..." :
			goal == TavernManager.BARON ?
			"Searching for Baron von Ratsworth..." :
			goal == TavernManager.FIGHT_BARON ?
			"Going after Baron von Ratsworth..." :
			"Exploring rest of cellar...";

		KoLmafia.updateDisplay( message );

		// Make sure we have the quest from the council
		RequestThread.postRequest( CouncilFrame.COUNCIL_VISIT );

		// Make sure Bart Ender has given us access to the cellar
		RequestThread.postRequest( new GenericRequest( "tavern.php?place=barkeep" ) );
		// *** Should look at response and make sure we got there

		// Visit the tavern cellar to update the layout
		RequestThread.postRequest( new GenericRequest( "cellar.php" ) );

		// Refetch the current layout
		layout = Preferences.getString( "tavernLayout" );

		// See if we've already found the goal outside KoLmafia
		faucet = layout.indexOf( "3" );
		if ( goal == TavernManager.FAUCET && faucet != -1 )
		{
			TavernManager.logFaucetSquare( faucet );
			return faucet + 1;
		}

		baron = layout.indexOf( "4" );
		if ( goal == TavernManager.BARON && baron != -1	 )
		{
			TavernManager.logBaronSquare( baron );
			return baron + 1;
		}

		unexplored = layout.indexOf( "0" );
		if ( goal == TavernManager.EXPLORE && unexplored == -1 )
		{
			TavernManager.logFaucetSquare( faucet );
			TavernManager.logBaronSquare( baron );
			KoLmafia.updateDisplay( "Entire cellar explored" );
			return 0;
		}

		// If the goal has not yet been found, then explore
		AdventureRequest request = new AdventureRequest( "Typical Tavern Cellar", "cellar.php", "" );

		// Remember if we have already found the faucet
		boolean hadFaucet = faucet != -1;

		// Reset Baron's choice to automatically skip him the first time we find him
		int oldBaronSetting = Preferences.getInteger( "choiceAdventure511" );
		if ( oldBaronSetting != 2 )
		{
			Preferences.setInteger( "choiceAdventure511", 2 );
		}

		while ( ( goal == TavernManager.FAUCET && faucet == -1 ||
			  goal == TavernManager.BARON && baron == -1 ||
			  goal == TavernManager.FIGHT_BARON && baron == -1 ||
			  goal == TavernManager.EXPLORE && unexplored != -1 ) &&
			KoLmafia.permitsContinue() &&
			KoLCharacter.getCurrentHP() > 0 &&
			KoLCharacter.getAdventuresLeft() > 0 )
		{
			// TavernRequest will visit the next unexplored square
			RequestThread.postRequest( request );

			// See what we discovered
			faucet = Preferences.getString( "tavernLayout" ).indexOf( "3" );
			baron = Preferences.getString( "tavernLayout" ).indexOf( "4" );
			unexplored = Preferences.getString( "tavernLayout" ).indexOf( "0" );

			// If we just found the faucet for the first time, visit Bart Ender to claim reward
			if ( !hadFaucet && faucet != -1 )
			{
				RequestThread.postRequest( new GenericRequest( "tavern.php?place=barkeep" ) );
				hadFaucet = true;
			}
		}

		// Restore Baron choice option setting.
		if ( oldBaronSetting != 2 )
		{
			Preferences.setInteger( "choiceAdventure511", oldBaronSetting );
		}

		if ( goal == TavernManager.FAUCET )
		{
			if ( faucet == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to find faucet." );
				return -1;
			}

			// Notify the user that the faucet has been found.
			TavernManager.logFaucetSquare( faucet );
			return faucet + 1;
		}

		if ( goal == TavernManager.BARON )
		{
			if ( baron == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to find baron." );
				return -1;
			}

			// Notify the user that the baron has been found.
			TavernManager.logBaronSquare( baron );
			return baron + 1;
		}

		if ( goal == TavernManager.FIGHT_BARON )
		{
			if ( baron == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to find baron." );
				return -1;
			}

			// Go fight Baron von Ratsworth
			if ( oldBaronSetting != 1 )
			{
				Preferences.setInteger( "choiceAdventure511", 1 );
			}

			TavernManager.logBaronSquare( baron );
			TavernManager.overrideSquare = baron;
			RequestThread.postRequest( request );
			TavernManager.overrideSquare = -1;

			if ( oldBaronSetting != 1 )
			{
				Preferences.setInteger( "choiceAdventure511", oldBaronSetting );
			}

			return baron + 1;
		}

		// Otherwise, we are exploring the rest of the tavern
		if ( goal == TavernManager.EXPLORE )
		{
			if ( unexplored != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to finish exploring cellar." );
				return -1;
			}

			// Notify the user that we are done
			TavernManager.logFaucetSquare( faucet );
			TavernManager.logBaronSquare( baron );
			KoLmafia.updateDisplay( "Done exploring." );
			return 0;
		}

		// What were we doing?
		return -1;
	}

	private static void logFaucetSquare( final int faucet )
	{
		if ( faucet != -1 )
		{
			int faucetRow = faucet / 5 + 1;
			int faucetColumn = faucet % 5 + 1;

			KoLmafia.updateDisplay( "Faucet found in row " + faucetRow + ", column " + faucetColumn );
		}
	}

	private static void logBaronSquare( final int baron )
	{
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
		if ( TavernManager.overrideSquare >= 0 )
		{
			return TavernManager.overrideSquare + 1;
		}

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
