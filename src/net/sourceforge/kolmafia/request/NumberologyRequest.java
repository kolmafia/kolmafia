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

package net.sourceforge.kolmafia.request;

import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.SkillPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.NumberologyManager;

public class NumberologyRequest
	extends GenericRequest
{
	public static final Pattern SEED_PATTERN = Pattern.compile( "num=([^&]*)" );

	private int seed = -1;

	public NumberologyRequest( int seed )
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1103" );
		this.addFormField( "option", "1" );
		this.addFormField( "num", String.valueOf( seed ) );
		this.seed = Math.abs( seed );
	}

	public static final int getSeed( final String urlString )
	{
		return GenericRequest.getNumericField( urlString, NumberologyRequest.SEED_PATTERN );
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( GenericRequest.abortIfInFightOrChoice() )
		{
			return;
		}

		// If you already cast Calculate the Universe today, punt
		if ( Preferences.getBoolean( "_universeCalculated" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You already Calculated the Universe today." );
			return;
		}

		// If the specified seed will get you "try again", punt
		int result = NumberologyManager.numberology( this.seed );
		String prize = NumberologyManager.numberologyPrize( result );
		if ( prize == NumberologyManager.TRY_AGAIN )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Seed " + this.seed + " will result in Try Again." );
			return;
		}

		GenericRequest skillRequest = new GenericRequest( "runskillz.php" );
		skillRequest.addFormField( "action", "Skillz" );
		skillRequest.addFormField( "whichskill", String.valueOf( SkillPool.CALCULATE_THE_UNIVERSE ) );
		skillRequest.addFormField( "ajax", "1" );

		// Run it via GET
		String URLString = skillRequest.getFullURLString();
		skillRequest.constructURLString( URLString, false );
		skillRequest.run();

		// See what KoL has to say about it.
		String responseText = skillRequest.responseText;

		if ( responseText.contains( "You don't have that skill" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't know how to Calculate the Universe" );
			return;
		}

		if ( responseText.contains( "You don't have enough" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need at least 1 MP to Calculate the Universe" );
			return;
		}

		if ( responseText.contains( "You can't use that skill again today" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You already Calculated the Universe today" );
			Preferences.setBoolean( "_universeCalculated", true );
			return;
		}

		if ( !responseText.contains( "whichchoice" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't Calculate the Universe" );
			return;
		}

		// You can only cast the skill once per day
		Preferences.setBoolean( "_universeCalculated", true );

		// Doing the Maths
		super.run();
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) )
		{
			return false;
		}

		int choice = ChoiceManager.extractChoiceFromURL( urlString );
		if ( choice != 1103 )
		{
			return false;
		}

		int seed = NumberologyRequest.getSeed( urlString );
		if ( seed == -1 )
		{
			return false;
		}

		String message = "numberology " + NumberologyManager.numberology( seed );

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
