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
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.RequestLogger;

public class PastaThrallCommand
	extends AbstractCommand
{
	public PastaThrallCommand()
	{
		this.usage = " - List status of pasta thralls.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		StringBuilder output = new StringBuilder();

		output.append( "<table border=2 cols=3>" );
		output.append( "<tr>" );
		output.append( "<th rowspan=3>Pasta Thrall</th>" );
		output.append( "<th>Name</th>" );
		output.append( "<th>Ability at Level 1</th>" );
		output.append( "</tr>" );
		output.append( "<tr>" );
		output.append( "<th>Level</th>" );
		output.append( "<th>Ability at Level 5</th>" );
		output.append( "</tr>" );
		output.append( "<tr>" );
		output.append( "<th>Current Modifiers</th>" );
		output.append( "<th>Ability at Level 10</th>" );
		output.append( "</tr>" );

		PastaThrallData[] thrallArray = new PastaThrallData[ KoLCharacter.pastaThralls.size() ];
		KoLCharacter.pastaThralls.toArray( thrallArray );

		for ( int i = 0; i < thrallArray.length; ++i )
		{
			PastaThrallData thrall = thrallArray[ i ];

			if ( thrall.equals( PastaThrallData.NO_THRALL ) )
			{
				continue;
			}

			output.append( "<tr>" );
			output.append( "<td rowspan=3>" );
			output.append( thrall.getType() );
			output.append( "</td>" );
			output.append( "<td>" );
			output.append( thrall.getName() );
			output.append( "</td>" );
			output.append( "<td>" );
			output.append( thrall.getLevel1Ability() );
			output.append( "</td>" );
			output.append( "</tr>" );

			output.append( "<tr>" );
			output.append( "<td>" );
			output.append( thrall.getLevel() );
			output.append( "</td>" );
			output.append( "<td>" );
			output.append( thrall.getLevel5Ability() );
			output.append( "</td>" );
			output.append( "</tr>" );

			output.append( "<tr>" );
			output.append( "<td>" );
			output.append( thrall.getCurrentModifiers() );
			output.append( "</td>" );
			output.append( "<td>" );
			output.append( thrall.getLevel10Ability() );
			output.append( "</td>" );
			output.append( "</tr>" );
		}

		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();

		output.setLength( 0 );
		PastaThrallData current = KoLCharacter.currentPastaThrall();
		if ( current == PastaThrallData.NO_THRALL )
		{
			output.append( "You do not currently have a bound pasta thrall" );
		}
		else
		{
			output.append( "You currently have a bound " );
			output.append( current.getType() );
			String name = current.getName();
			if ( !name.equals( "" ) )
			{
				output.append( " named " );
				output.append( name );
			}
		}

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
