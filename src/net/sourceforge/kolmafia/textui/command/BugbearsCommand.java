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

import net.sourceforge.kolmafia.session.BugbearManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BugbearsCommand
	extends AbstractCommand
{
	public BugbearsCommand()
	{
		this.usage = " - List progress of bugbear hunting.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		StringBuilder output = new StringBuilder();

		output.append( "<table border=2 cols=3>" );
		output.append( "<tr>" );
		output.append( "<th rowspan=2>Mothership Zone</th>" );
		output.append( "<th rowspan=2>Status</th>" );
		output.append( "<th rowspan=2>Bugbear</th>" );
		output.append( "<th>Location 1</th>" );
		output.append( "</tr>" );
		output.append( "<tr>" );
		output.append( "<th>Location 2</th>" );
		output.append( "</tr>" );

		for ( int i = 0; i < BugbearManager.BUGBEAR_DATA.length; ++i )
		{
			Object[] data = BugbearManager.BUGBEAR_DATA[ i ];

			output.append( "<tr>" );
			output.append( "<td rowspan=2>" );
			output.append( BugbearManager.dataToShipZone( data ) );
			output.append( "</td>" );

			String setting = BugbearManager.dataToStatusSetting( data );
			String status = Preferences.getString( setting );
			String value =
				StringUtilities.isNumeric( status ) ?
				( status + "/" + String.valueOf( BugbearManager.dataToLevel( data ) * 3 ) ) :
				status;

			output.append( "<td rowspan=2>" );
			output.append( value );
			output.append( "</td>" );

			output.append( "<td rowspan=2>" );
			output.append( BugbearManager.dataToBugbear( data ) );
			output.append( "</td>" );

			output.append( "<td>" );
			output.append( BugbearManager.dataToBugbearZone1( data ) );
			output.append( "</td>" );
			output.append( "</tr>" );

			output.append( "<tr>" );
			output.append( "<td>" );
			output.append( BugbearManager.dataToBugbearZone2( data ) );
			output.append( "</td>" );
			output.append( "</tr>" );
		}

		output.append( "</table>" );

		RequestLogger.printLine( output.toString() );
		RequestLogger.printLine();
	}
}
