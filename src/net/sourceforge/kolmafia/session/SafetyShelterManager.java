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

package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SafetyShelterManager
{	
	public static final String[] RonaldGoals =
	{
		"E.M.U. rocket thrusters",
		"Spell Transfer Complete",
		"E.M.U. joystick",
		"elven medi-pack & magi-pack",
		"Overstimulated",
		"Simulation Stimulation",
	};

	private static final String[] RonaldScript =
	{
		"11211",	// E.M.U. rocket thrusters
		"1122",		// Spell Transfer Complete
		"12211",	// E.M.U. joystick
		"12221",	// elven medi-pack & magi-pack
		"1321",		// Overstimulated
		"1322",		// Simulation Stimulation
	};

	public static final String[] GrimaceGoals =
	{
		"distention pill",
		"synthetic dog hair pill",
		"Heal Thy Nanoself",
		"E.M.U. harness",
		"elven hardtack & squeeze",
		"E.M.U. Helmet",
	};

	private static final String[] GrimaceScript =
	{
		"1121",		// distention pill
		"1122",		// synthetic dog hair pill
		"1211",		// Heal Thy Nanoself
		"12121",	// E.M.U. harness
		"13211",	// elven hardtack & squeeze
		"12221",	// E.M.U. Helmet
	};

	public static final String autoRonald( final String decision, final int stepCount, final String responseText )
	{
		return SafetyShelterManager.autoShelter( decision, stepCount, responseText, RonaldScript );
	}

	public static final String autoGrimace( final String decision, final int stepCount, final String responseText )
	{
		return SafetyShelterManager.autoShelter( decision, stepCount, responseText, GrimaceScript );
	}

	public static final String autoShelter( String decision, final int stepCount, final String responseText, final String [] script )
	{
		int goal = StringUtilities.parseInt( decision ) - 1;

		if ( ( goal < 0 || goal >= script.length ) ||
		     ( stepCount < 0 || stepCount >= script[ goal ].length() ) )
		{
			return "0";
		}

		decision = script[ goal ].substring( stepCount, stepCount + 1 );
		String action = ChoiceManager.findChoiceDecisionText( Integer.parseInt( decision ), responseText );
		if ( action != null )
		{
			logText( "Action: " + action );
		}

		return decision;
	}

	private static final void logText( final String text )
	{
		RequestLogger.printLine( text );
		RequestLogger.updateSessionLog( text );
	}

	public static final void addRonaldGoalButton( final StringBuffer buffer )
	{
		// Only add the goal button to the first choice
		if ( buffer.indexOf( "Take a Look Around" ) == -1 )
		{
			return;
		}

		int goal = Preferences.getInteger( "choiceAdventure535" );
		if ( goal < 1 || goal > RonaldGoals.length )
		{
			return;
		}

		ChoiceManager.addGoalButton( buffer, RonaldGoals[ goal - 1 ] );
	}

	public static final void addGrimaceGoalButton( final StringBuffer buffer )
	{
		// Only add the goal button to the first choice
		if ( buffer.indexOf( "Down the Hatch!" ) == -1 )
		{
			return;
		}

		int goal = Preferences.getInteger( "choiceAdventure536" );
		if ( goal < 1 || goal > GrimaceGoals.length )
		{
			return;
		}

		ChoiceManager.addGoalButton( buffer, GrimaceGoals[ goal - 1 ] );
	}
}
