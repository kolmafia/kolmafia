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

import java.io.File;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.WumpusManager;

import net.sourceforge.kolmafia.utilities.ByteBufferUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class WumpusCommand
	extends AbstractCommand
{
	public WumpusCommand()
	{
		this.usage = " status - Display status of last wumpus cave.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] tokens = parameters.split( "\\s+" );
		if ( tokens.length < 1 )
		{
			return;
		}

		String option = tokens[ 0 ];

		if ( option.equals( "status" ) )
		{
			WumpusManager.printStatus();
			return;
		}

		if ( option.equals( "code" ) )
		{
			String code = WumpusManager.getWumpinatorCode();
			RequestLogger.printLine( code );
			return;
		}

		if ( option.equals( "reset" ) )
		{
			WumpusManager.reset();
			return;
		}

		if ( option.equals( "replay" ) )
		{
			if ( tokens.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Replay from what file?" );
				return;
			}

			String fileName = tokens[ 1 ];
			File file = new File( KoLConstants.DATA_LOCATION, fileName );

			if ( !file.exists() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "File " + file + " does not exist" );
				return;
			}
			
			byte[] bytes = ByteBufferUtilities.read( file );
			String text = StringUtilities.getEncodedString( bytes, "UTF-8" );

			KoLmafia.updateDisplay( "Read " + KoLConstants.COMMA_FORMAT.format( bytes.length ) +
						" bytes into a " + KoLConstants.COMMA_FORMAT.format( text.length() ) +
						" character string" );

			String[] lines = text.split( "\\n" );

			// Start with fresh cave data
			WumpusManager.reset();

			int index = 0;
			while ( index < lines.length )
			{
				String encounter = null, exits = null, sounds = null;
				String line = lines[ index++ ];

				// Find the next encounter
				if ( !line.startsWith( "Encounter:" ) )
				{
					continue;
				}

				encounter = line;

				// Look at all lines up to next encounter, collecting exits and sounds
				while ( index < lines.length )
				{
					line = lines[ index];
					if ( line.startsWith( "Encounter:" ) )
					{
						break;
					}

					index += 1;
					
					if ( line.startsWith( "Exits:" ) )
					{
						exits = line;
					}
					else if ( line.startsWith( "Sounds:" ) )
					{
						sounds = line;
					}
				}

				RequestLogger.printLine( encounter );
				String responseText = WumpusManager.reconstructResponseText( encounter, exits, sounds );
				if ( responseText != null )
				{
					WumpusManager.visitChoice( responseText );
					StringBuilder buffer = new StringBuilder();
					buffer.append( "Wumpinator: " );
					buffer.append( "<a href=\"" ) ;
					buffer.append( WumpusManager.getWumpinatorURL() );
					buffer.append( "\">" );
					buffer.append( "&lt;click here&gt;" );
					buffer.append( "</a>" );
					RequestLogger.printLine( buffer.toString() );
				}
			}
		}
	}
}
