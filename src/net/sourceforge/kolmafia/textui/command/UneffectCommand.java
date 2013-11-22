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

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.EffectDatabase;

import net.sourceforge.kolmafia.request.UneffectRequest;

public class UneffectCommand
	extends AbstractCommand
{
	public UneffectCommand()
	{
		this.usage = "[?] <effect> [, <effect>]... - remove effects using appropriate means.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.contains( "," ) )
		{
			// See if the whole parameter, comma and all,
			// matches an effect.
			if ( EffectDatabase.getEffectId( parameters.trim() ) == -1 )
			{
				// Nope. It is a list of effects. Assume that
				// none contain a comma.
				String[] effects = parameters.split( "\\s*,\\s*" );
				for ( int i = 0; i < effects.length; ++i )
				{
					this.run( "uneffect", effects[ i ] );
				}

				return;
			}
		}

		List matchingEffects = EffectDatabase.getMatchingNames( parameters.trim() );
		if ( matchingEffects.isEmpty() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Unknown effect: " + parameters );
			return;
		}

		if ( matchingEffects.size() > 1 )
		{
			// If there's only one shruggable buff on the list, then
			// that's probably the one the player wants.

			int shruggableCount = 0;

			String buffToCheck;
			AdventureResult buffToRemove = null;

			for ( int i = 0; i < matchingEffects.size(); ++i )
			{
				buffToCheck = (String) matchingEffects.get( i );
				if ( UneffectRequest.isShruggable( buffToCheck ) )
				{
					++shruggableCount;
					buffToRemove = new AdventureResult( buffToCheck, 1, true );
				}
			}

			if ( shruggableCount == 1 )
			{
				if ( KoLConstants.activeEffects.contains( buffToRemove ) )
				{
					if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
					{
						RequestLogger.printLine( buffToRemove.toString() );
						return;
					}

					RequestThread.postRequest( new UneffectRequest( buffToRemove ) );
				}

				return;
			}

			KoLmafia.updateDisplay( MafiaState.ERROR, "Ambiguous effect name: " + parameters );
			RequestLogger.printList( matchingEffects );

			return;
		}

		AdventureResult effect = new AdventureResult( (String) matchingEffects.get( 0 ), 1, true );

		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( effect.toString() );
			return;
		}

		if ( KoLConstants.activeEffects.contains( effect ) )
		{
			RequestThread.postRequest( new UneffectRequest( effect ) );
		}
	}
}
