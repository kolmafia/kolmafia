/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BeachCombRequest;
import net.sourceforge.kolmafia.request.BeachCombRequest.BeachCombCommand;

import net.sourceforge.kolmafia.session.BeachManager;
import net.sourceforge.kolmafia.session.BeachManager.BeachHead;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BeachCommand
	extends AbstractCommand
{
	public BeachCommand()
	{
		this.usage = " common, head DESC, print, visit, random, wander MINUTES, comb ROW, COL, exit";
	}
	
	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " +" );
		String command = split[ 0 ];

		if ( command.equals( "print" ) )
		{
			printBeachLayout();
			return;
		}

		if ( command.equals( "common" ) )
		{
			boolean visited = BeachCombRequest.visitIfNecessary();
			if ( KoLmafia.permitsContinue() )
			{
				try
				{
					( new BeachCombRequest( BeachCombCommand.COMMON ) ).run();
				}
				finally
				{
					BeachCombRequest.exitIfNecessary( visited );
				}
			}
			return;
		}

		if ( command.equals( "head" ) )
		{
			// *** parse BeachHead
			// 
			// beach head NUM
			// beach head EFFECT
			// beach head KEYWORD

			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Choose which beach head to comb: NUM, EFFECT, or KEYWORD." );
				return;
			}

			BeachHead head = null;
			String str;

			if ( split.length == 2 )
			{
				str = split[ 1 ];
				if ( StringUtilities.isNumeric( str ) )
				{
					int num = StringUtilities.parseInt( str );
					head = BeachManager.idToBeachHead.get( num );
					if ( head == null )
					{
						KoLmafia.updateDisplay( MafiaState.ERROR, "'" + num + "' is not a valid beach head number." );
						return;
					}
				}
				else
				{
					// Look for an effect with no spaces...
					head = BeachManager.effectToBeachHead.get( str );

					if ( head == null )
					{
						List<String> matchingNames = StringUtilities.getMatchingNames( BeachManager.beachHeadDescArray, str );
						if ( matchingNames.size() == 0 )
						{
							KoLmafia.updateDisplay( MafiaState.ERROR, "Which beach head is " + str + "?" );
							return;
						}

						if ( matchingNames.size() > 1 )
						{
							KoLmafia.updateDisplay( MafiaState.ERROR, "'" + str + "' is an ambiguous beach head" );
							return;
						}

						head = BeachManager.descToBeachHead.get( matchingNames.get( 0 ) );
						// If the name fuzzy matches the descs, it is a valid beach head
					}
				}
			}
			else
			{
				int index = parameters.indexOf( " " );
				str = parameters.substring( index ).trim();
				head = BeachManager.effectToBeachHead.get( str );
				if ( head == null )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "'" + str + "' doesn't identify a beach head to comb" );
					return;
				}
			}

			boolean visited = BeachCombRequest.visitIfNecessary();
			if ( KoLmafia.permitsContinue() )
			{
				try
				{
					( new BeachCombRequest( head ) ).run();
				}
				finally
				{
					BeachCombRequest.exitIfNecessary( visited );
				}
			}
			return;
		}

		// The following commands let you manipulate the comb, one command at a time
		if ( command.equals( "visit" ) )
		{
			( new BeachCombRequest() ).run();
			if ( Preferences.getBoolean( "_beachCombing" ) )
			{
				printBeachLayout();
			}
			return;
		}

		if ( command.equals( "exit" ) )
		{
			( new BeachCombRequest( BeachCombCommand.EXIT ) ).run();
			return;
		}

		if ( command.equals( "random" ) )
		{
			boolean visited = BeachCombRequest.visitIfNecessary();
			if ( KoLmafia.permitsContinue() )
			{
				( new BeachCombRequest( BeachCombCommand.RANDOM ) ).run();
				if ( Preferences.getBoolean( "_beachCombing" ) )
				{
					printBeachLayout();
				}
			}
			return;
		}

		if ( command.equals( "wander" ) )
		{
			// Parse minutes
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Choose how far down the beach to wander." );
				return;
			}

			String minutesString = split[ 1];
			if ( !StringUtilities.isNumeric( minutesString ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "'" + minutesString + " must be a number." );
				return;
			}

			int minutes = StringUtilities.parseInt( minutesString );

			boolean visited = BeachCombRequest.visitIfNecessary();
			if ( KoLmafia.permitsContinue() )
			{
				( new BeachCombRequest( minutes ) ).run();
				if ( Preferences.getBoolean( "_beachCombing" ) )
				{
					printBeachLayout();
				}
			}
			return;
		}

		if ( command.equals( "comb" ) )
		{
			if ( !Preferences.getBoolean( "_beachCombing" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Visit a square on the beach before you comb it." );
				return;
			}

			// Parse rows and col
			if ( split.length < 3 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Choose row and column to comb." );
				return;
			}

			String rowString = split[ 1 ];
			if ( !StringUtilities.isNumeric( rowString ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "'" + rowString + " must be a number." );
				return;
			}
			int row = StringUtilities.parseInt( rowString );

			String colString = split[ 2 ];
			if ( !StringUtilities.isNumeric( colString ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "'" + colString + " must be a number." );
				return;
			}
			int col = StringUtilities.parseInt( colString );

			( new BeachCombRequest( row, col ) ).run();

			return;
		}
	}

	private void printBeachLayout()
	{
		RequestLogger.printLine( "Beach at " + Preferences.getInteger( "_beachMinutes" ) );
		RequestLogger.printLine( "" );

        // Get current (or most recent) beach layout
		Map<Integer, String> layout = BeachManager.getBeachLayout();

		// It is sorted from lowest to highest row. I.e., closest to
		// farthest from waves. We want to display it in the opposite
		// order. Push rows on stack and then pop them off
		Stack<Entry<Integer, String>> stack = new Stack<Entry<Integer, String>>();
		for ( Entry<Integer, String> entry : layout.entrySet() )
		{
			stack.push( entry );
		}
		while ( !stack.empty() )
		{
			Entry<Integer, String> entry = stack.pop();
			RequestLogger.printLine( entry.getKey() + ": " + entry.getValue() );
		}
	}
}
