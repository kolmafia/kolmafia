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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.request.BeerPongRequest;

public class PirateInsultsCommand
	extends AbstractCommand
{
	{
		this.usage = " - list the pirate insult comebacks you know.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		KoLCharacter.ensureUpdatedPirateInsults();

		RequestLogger.printLine();
		RequestLogger.printLine( "Known insults:" );

		int count = 0;
		for ( int i = 1; i <= BeerPongRequest.VALID_PIRATE_INSULTS; ++i )
		{
			String retort = BeerPongRequest.knownPirateRetort( i );
			if ( retort != null )
			{
				if ( count == 0 )
				{
					RequestLogger.printLine();
				}
				count += 1;

				RequestLogger.printLine( retort );
			}
		}

		float odds = BeerPongRequest.pirateInsultOdds( count ) * 100.0f;

		if ( count == 0 )
		{
			RequestLogger.printLine( "None." );
		}

		RequestLogger.printLine();
		RequestLogger.printLine( "Since you know " + count + " insult" + ( count == 1 ? "" : "s" ) + ", you have a " + KoLConstants.FLOAT_FORMAT.format( odds ) + "% chance of winning at Insult Beer Pong." );
	}
}
