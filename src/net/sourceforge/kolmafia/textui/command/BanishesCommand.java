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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.BanishManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BanishesCommand
	extends AbstractCommand
{
	public BanishesCommand()
	{
		this.usage = " - List status of banishes.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[][] banishData = BanishManager.getBanishData();

		StringBuilder output = new StringBuilder();

		if ( banishData != null )
		{
			output.append( "<table border=2 cols=4>" );
			output.append( "<tr>" );
			output.append( "<th>Monsters Banished</th>" );
			output.append( "<th>Banished By</th>" );
			output.append( "<th>On Turn</th>" );
			output.append( "<th>Turns Left</th>" );
			output.append( "</tr>" );

			for ( String[] banish : banishData )
			{
				output.append( "<tr>" );

				for ( int i = 0 ; i < 4 ; i++ )
				{
					output.append( "<td>" );
					output.append( banish[i] );
					output.append( "</td>" );
				}
				output.append( "</tr>" );
			}

			output.append( "</table>" );
		}
		else
		{
			output.append( "No current banishes" );
		}

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
