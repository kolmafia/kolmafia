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

import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CountersCommand
	extends AbstractCommand
{
	public CountersCommand()
	{
		this.usage = " [ clear | add <number> [<title> <img>] ] - show, clear, or add to current turn counters.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( parameters.equalsIgnoreCase( "clear" ) )
		{
			TurnCounter.clearCounters();
			return;
		}

		if ( parameters.startsWith( "deletehash " ) )
		{
			TurnCounter.deleteByHash( StringUtilities.parseInt( parameters.substring( 11 ) ) );
			return;
		}

		if ( parameters.startsWith( "add " ) )
		{
			String title = "Manual";
			String image = "watch.gif";
			parameters = parameters.substring( 4 ).trim();
			if ( parameters.endsWith( ".gif" ) )
			{
				int lastSpace = parameters.lastIndexOf( " " );
				image = parameters.substring( lastSpace + 1 );
				parameters = parameters.substring( 0, lastSpace + 1 ).trim();
			}
			int spacePos = parameters.indexOf( " " );
			if ( spacePos != -1 )
			{
				title = parameters.substring( spacePos + 1 );
				parameters = parameters.substring( 0, spacePos ).trim();
			}

			TurnCounter.startCounting( StringUtilities.parseInt( parameters ), title, image );
		}

		ShowDataCommand.show( "counters" );
	}
}
