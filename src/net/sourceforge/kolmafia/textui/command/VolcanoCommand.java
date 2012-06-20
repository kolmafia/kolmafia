/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.request.VolcanoIslandRequest;

import net.sourceforge.kolmafia.session.VolcanoMazeManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class VolcanoCommand
	extends AbstractCommand
{
	public VolcanoCommand()
	{
		this.usage = " visit | solve | map [n] | platforms | jump | move row col | movep row col - play in the lava maze.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "slime" ) )
		{
			VolcanoIslandRequest.getSlime();
			return;
		}

		if ( command.equals( "clear" ) )
		{
			VolcanoMazeManager.clear();
			return;
		}

		if ( command.equals( "solve" ) )
		{
			VolcanoMazeManager.solve();
			return;
		}

		if ( command.equals( "visit" ) )
		{
			VolcanoMazeManager.visit();
			return;
		}

		if ( command.equals( "platforms" ) )
		{
			VolcanoMazeManager.platforms();
			return;
		}

		if ( command.equals( "jump" ) )
		{
			VolcanoMazeManager.jump();
			return;
		}

		if ( command.equals( "move" ) || command.equals( "movep" ) )
		{
			if ( split.length != 3 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Specify x y coordinate to jump to" );
				return;
			}
			int x = VolcanoCommand.getCell( split[1] );
			if ( x < 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Coordinate 'x' must be between 0 and 12" );
				return;
			}
			int y = VolcanoCommand.getCell( split[2] );
			if ( y < 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Coordinate 'y' must be between 0 and 12" );
				return;
			}
			VolcanoMazeManager.move( x, y, command.equals( "movep" ) );
			return;
		}

		if ( command.equals( "map" ) )
		{
			if ( split.length == 1 )
			{
				VolcanoMazeManager.displayMap();
				return;
			}
			int map = VolcanoCommand.getMap( split[1] );
			if ( map < 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Map # must be between 1 and 5" );
				return;
			}
			VolcanoMazeManager.displayMap( map );
			return;
		}

		if ( command.equals( "test" ) )
		{
			if ( split.length != 4 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Specify map x y" );
				return;
			}
			int map = VolcanoCommand.getMap( split[1] );
			if ( map < 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Map # must be between 1 and 5" );
				return;
			}
			int x = VolcanoCommand.getCell( split[2] );
			if ( x < 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Coordinate 'x' must be between 0 and 12" );
				return;
			}
			int y = VolcanoCommand.getCell( split[3] );
			if ( y < 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Coordinate 'y' must be between 0 and 12" );
				return;
			}
			VolcanoMazeManager.test( map, x, y );
			return;
		}

		KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to do in the volcano?" );
	}

	private static int getCell( final String str )
	{
		if ( !StringUtilities.isNumeric( str ) )
		{
			return -1;
		}
		int cell = StringUtilities.parseInt( str );
		if ( cell < 0 || cell > 12 )
		{
			return -1;
		}
		return cell;
	}

	private static int getMap( final String str )
	{
		if ( !StringUtilities.isNumeric( str ) )
		{
			return -1;
		}
		int map = StringUtilities.parseInt( str );
		if ( map < 1 || map > 5 )
		{
			return -1;
		}
		return map;
	}
}
