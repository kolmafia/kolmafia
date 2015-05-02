/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.request.PeeVPeeRequest;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PvpManager
{
	// The following is no longer guaranteed; Season 19 lets you use any mini as a stance
	public static final int MUSCLE_STANCE = IntegerPool.get( 1 );
	public static final int MYST_STANCE = IntegerPool.get( 2 );
	public static final int MOXIE_STANCE = IntegerPool.get( 3 );
	public static final int BALLYHOO_STANCE = IntegerPool.get( 4 );

	// The current mapping of stances
	public static final TreeMap<Integer,String> optionToStance = new TreeMap<Integer,String>();
	public static final TreeMap<String,Integer> stanceToOption = new TreeMap<String,Integer>();
	public static boolean stancesKnown = false;

	// <select name="stance"><option value="0" >Bear Hugs All Around</option><option value="1" selected>Beary Famous</option><option value="2" >Barely Dressed</option><option value="3" >Basket Reaver</option><option value="4" >Polar Envy</option><option value="5" >Maul Power</option><option value="6" >Grave Robbery</option><option value="7" >Most Things Eaten</option><option value="8" >Hibernation Ready</option><option value="9" >Visiting the Cousins</option><option value="10" >Northern Digestion</option><option value="11" >Most Murderous</option></select>

	private static final Pattern STANCE_DROPDOWN_PATTERN = Pattern.compile( "<select name=\"stance\">.*?</select>", Pattern.DOTALL );
	private static final Pattern STANCE_OPTION_PATTERN = Pattern.compile( "<option value=\"([\\d]*)\" (?:selected)?>(.*?)</option>" );

	public static final boolean checkStances()
	{
		if ( !PvpManager.stancesKnown )
		{
			PeeVPeeRequest request = new PeeVPeeRequest( "fight" );
			RequestThread.postRequest( request );
		}
		return PvpManager.stancesKnown;
	}

	public static final void parseStances( final String responseText )
	{
		Matcher stanceMatcher = PvpManager.STANCE_DROPDOWN_PATTERN.matcher( responseText );
		if ( !stanceMatcher.find() )
		{
			return;
		}

		String stances = stanceMatcher.group( 0 );

		PvpManager.optionToStance.clear();
		PvpManager.stanceToOption.clear();

		Matcher optionsMatcher = PvpManager.STANCE_OPTION_PATTERN.matcher( stances );
		while ( optionsMatcher.find() )
		{
			int option = StringUtilities.parseInt( optionsMatcher.group( 1 ) );
			String stance = optionsMatcher.group( 2 );
			PvpManager.optionToStance.put( option, stance );
			PvpManager.stanceToOption.put( stance, option );
		}

		PvpManager.stancesKnown = true;
	}

	public static final int findStance( final String stanceName )
	{
		// *** Could do fuzzy matching here
		Integer stance = PvpManager.stanceToOption.get( stanceName );
		return stance == null ? -1 : stance.intValue();
	}

	public static final String findStance( final int stance )
	{
		return PvpManager.optionToStance.get( stance );
	}

	private static boolean checkHippyStone()
	{
		if ( !KoLCharacter.getHippyStoneBroken() )
		{
			if ( !InputFieldUtilities.confirm( "Would you like to break your hippy stone?" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "This feature is not available to hippies." );
				return false;
			}
			new GenericRequest( "campground.php?confirm=on&smashstone=Yep." ).run();
			return KoLCharacter.getHippyStoneBroken();
		}
		return true;
	}

	private static int pickStance()
	{
		if ( KoLCharacter.getAdjustedMuscle() >= KoLCharacter.getAdjustedMysticality() &&
		     KoLCharacter.getAdjustedMuscle() >= KoLCharacter.getAdjustedMoxie() )
		{
			return PvpManager.MUSCLE_STANCE;
		}

		if ( KoLCharacter.getAdjustedMysticality() >= KoLCharacter.getAdjustedMuscle() &&
		     KoLCharacter.getAdjustedMysticality() >= KoLCharacter.getAdjustedMoxie() )
		{
			return PvpManager.MYST_STANCE;
		}

		return PvpManager.MOXIE_STANCE;
	}

	public static void executePvpRequest( final int attacks, final String mission, final int stance )
	{
		if ( !PvpManager.checkHippyStone() )
		{
			return;
		}

		PeeVPeeRequest request = new PeeVPeeRequest( "", stance, mission );
		
		int availableFights = KoLCharacter.getAttacksLeft();
		int totalFights = ( attacks > availableFights || attacks == 0 ) ? availableFights : attacks;
		int fightsCompleted = 0;

		while ( fightsCompleted++ < totalFights )
		{
			// Execute the beforePVPScript to change equipment, get
			// buffs, whatever.
			KoLmafia.executeBeforePVPScript();

			// If the beforePVPScript aborts, stop before initiating a fight
			if ( KoLmafia.refusesContinue() )
			{
				break;
			}

			KoLmafia.updateDisplay( "Attack " + fightsCompleted + " of " + totalFights );
			RequestThread.postRequest( request );

			// If he wants to abort the command, honor it
			if ( KoLmafia.refusesContinue() )
			{
				break;
			}

			KoLmafia.forceContinue();
		}

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "You have " + KoLCharacter.getAttacksLeft() + " attacks remaining." );
		}
	}

	public static final void executePvpRequest( final ProfileRequest[] targets, final PeeVPeeRequest request )
	{
		if ( !PvpManager.checkHippyStone() )
		{
			return;
		}
		
		for ( int i = 0; i < targets.length && KoLmafia.permitsContinue() && KoLCharacter.getAttacksLeft() > 0; ++i )
		{
			if ( targets[ i ] == null )
			{
				continue;
			}

			if ( Preferences.getString( "currentPvpVictories" ).indexOf( targets[ i ].getPlayerName() ) != -1 )
			{
				continue;
			}

			if ( targets[ i ].getPlayerName().toLowerCase().startsWith( "devster" ) )
			{
				continue;
			}

			// Execute the beforePVPScript to change equipment, get buffs, whatever.
			KoLmafia.executeBeforePVPScript();

			// Choose current "best" stance
			// *** this is broken, as of Season 19
			request.addFormField( "stance", String.valueOf( PvpManager.pickStance() ) );

			KoLmafia.updateDisplay( "Attacking " + targets[ i ].getPlayerName() + "..." );
			request.setTarget( targets[ i ].getPlayerName() );
			request.setTargetType( "0" );
			RequestThread.postRequest( request );

			if ( request.responseText.indexOf( "lost some dignity in the attempt" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You lost to " + targets[ i ].getPlayerName() + "." );
			}
		}
	}
}
