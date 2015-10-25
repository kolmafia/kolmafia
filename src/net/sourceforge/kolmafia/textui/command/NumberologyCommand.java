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

import java.util.Map;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.NumberologyManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NumberologyCommand
	extends AbstractCommand
{
	public NumberologyCommand()
	{
		this.usage = "[?] [N] - list possible results from Calculate the Universe or submit a number";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !StringUtilities.isNumeric( parameters ) )
		{
			Map<Integer,Integer> results = NumberologyManager.numberologyResults();
			for ( Map.Entry<Integer,Integer> entry : results.entrySet() )
			{
				int result = entry.getKey();
				int seed = entry.getValue();
				String prize = NumberologyManager.prize( result );
				RequestLogger.printLine( "[" + result + "] Enter " + seed + " to get " + prize );
			}
			return;
		}

		int seed = StringUtilities.parseInt( parameters );

		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			int raw = NumberologyManager.rawNumberology( seed );
			int result = NumberologyManager.numberology( seed );
			String prize = NumberologyManager.prize( result );
			RequestLogger.printLine( seed + " -> " + raw + " (" + result + ") = " + prize );
			return;
		}

		RequestLogger.printLine( "Calculate with " + seed + " here" );
	}
}
