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

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

public class FlickerCommand
	extends AbstractCommand
{
	public static final String[][] PIXELS =
	{
		{
			"flickeringPixel1",
			"Anger",
			"Stupid Pipes",
			"25 hot resistance",
		},
		{
			"flickeringPixel2",
			"Anger",
			"You're Freaking Kidding Me",
			"500 buffed Muscle/Mysticality/Moxie",
		},
		{
			"flickeringPixel3",
			"Fear",
			"Snakes",
			"300 buffed Moxie",
		},
		{
			"flickeringPixel4",
			"Fear",
			"So... Many... Skulls...",
			"25 spooky resistance",
		},
		{
			"flickeringPixel5",
			"Doubt",
			"A Stupid Dummy",
			"+300 bonus damage",
		},
		{
			"flickeringPixel6",
			"Doubt",
			"Slings and Arrows",
			"1000 HP",
		},
		{
			"flickeringPixel7",
			"Regret",
			"This Is Your Life",
			"1000 MP",
		},
		{
			"flickeringPixel8",
			"Regret",
			"The Wall of Wailing",
			"60 prismatic damage",
		},
	};

	public FlickerCommand()
	{
		this.usage = " - List status of flickering pixels.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		StringBuilder output = new StringBuilder();

		output.append( "<table border=2 cols=5>" );
		output.append( "<tr>" );
		output.append( "<th>#</th>" );
		output.append( "<th>Location</th>" );
		output.append( "<th>Choice</th>" );
		output.append( "<th>Requirement</th>" );
		output.append( "<th>Status</th>" );
		output.append( "</tr>" );

		for ( int i = 0; i < FlickerCommand.PIXELS.length; ++i )
		{
			String [] data = FlickerCommand.PIXELS[ i ];

			output.append( "<tr>" );

			output.append( "<td>" );
			output.append( String.valueOf( i + 1 ) );
			output.append( "</td>" );

			output.append( "<td>" );
			output.append( data[1] );
			output.append( "</td>" );

			output.append( "<td>" );
			output.append( data[2] );
			output.append( "</td>" );

			output.append( "<td>" );
			output.append( data[3] );
			output.append( "</td>" );

			output.append( "<td>" );
			output.append( Preferences.getBoolean( data[0] ) ? "have" : "NEED" );
			output.append( "</td>" );

			output.append( "</tr>" );
		}

		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
