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

import java.util.List;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;

import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.EveryCard;

public class PlayCommand
	extends AbstractCommand
{
	public PlayCommand()
	{
		this.usage = " random [PHYLUM] | CARDNAME - Play a random or specified card";
	}

	@Override
	public void run( final String cmd, String parameter )
	{
		EveryCard card = null;

		parameter = parameter.trim();

		if ( parameter.equals( "" ) )
		{
			KoLmafia.updateDisplay( "Play what?" );
			return;
		}

		if ( parameter.startsWith( "random" ) )
		{
			parameter = parameter.substring( 6 ).trim();
			
			if ( !parameter.equals( "" ) )
			{
				Phylum phylum = MonsterDatabase.phylumNumber( parameter );
				if ( phylum == Phylum.NONE )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "What kind of random monster is a " + parameter + "?" );
					return;
				}

				card = DeckOfEveryCardRequest.phylumToCard( phylum );
				if ( card == null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "'" + parameter + "' is not a known monster phylum" );
					return;
				}
			}
		}
		else
		{
			List<String> matchingNames = DeckOfEveryCardRequest.getMatchingNames( parameter );
			if ( matchingNames.size() == 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't know how to play " + parameter );
				return;
			}

			if ( matchingNames.size() > 1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "'" + parameter + "' is an ambiguous card name " );
				return;
			}

			String name = matchingNames.get( 0 );

			card = DeckOfEveryCardRequest.canonicalNameToCard( name );
			if ( card == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "I don't know how to play " + parameter );
				return;
			}
		}

		DeckOfEveryCardRequest request = card == null ? new DeckOfEveryCardRequest() : new DeckOfEveryCardRequest( card );

		RequestThread.postRequest( request );
	}
}
