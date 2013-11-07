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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UseSkillCommand
	extends AbstractCommand
{
	public UseSkillCommand()
	{
		this.usage = "[?] [ [<count>] <skill> [on <player>] ] - list spells, or use one.";
	}

	@Override
	public void run( final String command, final String parameters )
	{
		if ( parameters.length() > 0 )
		{
			SpecialOutfit.createImplicitCheckpoint();
			UseSkillCommand.cast( parameters );
			SpecialOutfit.restoreImplicitCheckpoint();
			return;
		}
		ShowDataCommand.show( "skills" + ( command.equals( "cast" ) ? " cast" : "" ) );
	}

	private static void cast( final String parameters )
	{
		UseSkillCommand.cast( parameters, false );
	}

	public static boolean cast( final String parameters, boolean sim )
	{
		String[] buffs = parameters.split( "\\s*,\\s*" );

		for ( int i = 0; i < buffs.length; ++i )
		{
			String[] splitParameters = buffs[ i ].replaceFirst( " [oO][nN] ", " => " ).split( " => " );

			if ( splitParameters.length == 1 )
			{
				splitParameters = new String[ 2 ];
				splitParameters[ 0 ] = buffs[ i ];
				splitParameters[ 1 ] = null;
			}

			String[] buffParameters = AbstractCommand.splitCountAndName( splitParameters[ 0 ] );
			String buffCountString = buffParameters[ 0 ];
			String skillNameString = buffParameters[ 1 ];

			String skillName = SkillDatabase.getUsableKnownSkillName( skillNameString );
			if ( skillName == null )
			{
				if ( sim )
				{
					return false;
				}
				KoLmafia.updateDisplay( MafiaState.ERROR,
					"You don't have a skill uniquely matching \"" + parameters + "\"" );
				return false;
			}

			int buffCount = 1;

			if ( buffCountString != null && buffCountString.equals( "*" ) )
			{
				buffCount = 0;
			}
			else if ( buffCountString != null )
			{
				buffCount = StringUtilities.parseInt( buffCountString );
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( skillName + " (x" + buffCount + ")" );
				return true;
			}
			
			UseSkillRequest request =  UseSkillRequest.getInstance( skillName, splitParameters[ 1 ], buffCount );

			if ( sim )
			{
				return request.getMaximumCast() > 0;
			}

			RequestThread.postRequest( request );
		}
		return true;
	}
}
