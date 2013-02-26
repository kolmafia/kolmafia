/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

public class LostKeyManager
{
	public static final String[] goals =
	{
		"Lost Glasses",
		"Lost Comb",
		"Lost Pill Bottle"
	};

	private static final String[] steps =
	{
		"121111", // glasses
		"131212", // comb
		"131113", // pill bottle
	};

	public static final String autoKey( String decision, final int stepCount, final String responseText )
	{
		int goal = StringUtilities.parseInt( decision ) - 1;

		if ( ( goal < 0 || goal >= steps.length ) || ( stepCount < 0 || stepCount >= steps[ goal ].length() ) )
		{
			return "0";
		}

		decision = steps[ goal ].substring( stepCount, stepCount + 1 );
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

	private static final String currentGoalString()
	{
		int goal = Preferences.getInteger( "choiceAdventure594" );

		if ( goal < 1 || goal > goals.length )
		{
			return null;
		}

		return goals[ goal - 1 ];
	}

	public static final void addGoalButton( final StringBuffer buffer )
	{
		// Only add the goal button to the first choice
		if ( buffer.indexOf( "hotel next door" ) != -1 )
		{
			String goal = LostKeyManager.currentGoalString();
			if ( goal != null )
			{
				ChoiceManager.addGoalButton( buffer, goal );
			}
		}
	}
}
