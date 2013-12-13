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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.RestoresDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RestoresCommand
	extends AbstractCommand
{
	public RestoresCommand()
	{
		this.usage = " - List details of restores.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "" ) )
		{
			command = "available";
		}
		if ( !command.equals( "all" ) && !command.equals( "available" ) && !command.equals( "obtainable" ) )
		{
			KoLmafia.updateDisplay( "Valid parameters are all, available or obtainable" );
			return;
		}

		String[][] restoreData = RestoresDatabase.getRestoreData( command );

		StringBuilder output = new StringBuilder();

		if ( restoreData != null )
		{
			output.append( "<table border=2 cols=7>" );
			output.append( "<tr>" );
			output.append( "<th>Restore</th>" );
			output.append( "<th>Type</th>" );
			output.append( "<th>HP</th>" );
			output.append( "<th>MP</th>" );
			output.append( "<th>Adv cost</th>" );
			output.append( "<th>Uses left</th>" );
			output.append( "<th>Notes</th>" );
			output.append( "</tr>" );

			for ( String[] restore : restoreData )
			{
				if ( restore[ 0 ] != null )
				{
					output.append( "<tr>" );

					for ( int i = 0 ; i < 7 ; i++ )
					{
						output.append( "<td>" );
						output.append( restore[i] );
						output.append( "</td>" );
					}
					output.append( "</tr>" );
				}
			}

			output.append( "</table>" );
		}
		else
		{
			output.append( "No restore details" );
		}

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
