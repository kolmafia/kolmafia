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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MacroTestCommand
	extends AbstractCommand
{
	public MacroTestCommand()
	{
		this.usage = " [monster] - turns on macro debug and generates a macro for the given monster";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		int index = 0;
		if ( parameters != null )
		{
			int val = parameters.indexOf( "index=" );
			if ( val != -1 )
			{
				index = StringUtilities.parseInt( parameters.substring( val + 6).trim() );
				parameters = parameters.substring( 0, val ).trim();
			}

			if ( parameters.length() > 0 )
			{
				MonsterStatusTracker.setNextMonsterName( parameters );
			}
		}
		
		try
		{
			Preferences.setBoolean( "macroDebug", true );
			FightRequest.setMacroPrefixLength( index );

			int lastComplexActionPrefix = 0;
			while ( true )
			{
				String macro = Macrofier.macrofy();
				int prefix = FightRequest.getMacroPrefixLength();

				if ( macro == null )
				{
					// Quit if final action in strategy is complex
					if ( lastComplexActionPrefix + 1 == prefix )
					{
						break;
					}
					lastComplexActionPrefix = prefix;
					FightRequest.setMacroPrefixLength( prefix + 1 );
					RequestLogger.printLine( "****action***" );
					RequestLogger.printLine();
					continue;
				}

				if ( prefix == 0 )
				{
					break;
				}
			}
		}
		finally
		{
			Preferences.setBoolean( "macroDebug", false );
		}
	}
}
