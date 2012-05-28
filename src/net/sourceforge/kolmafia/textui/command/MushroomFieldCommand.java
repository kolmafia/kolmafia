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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.session.MushroomManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MushroomFieldCommand
	extends AbstractCommand
{
	{
		this.usage = " [ plant <square> <type> | pick <square> | harvest ] - view or use your mushroom plot";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		String[] split = parameters.split( " " );
		String command = split[ 0 ];

		if ( command.equals( "plant" ) )
		{
			if ( split.length < 3 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: field plant square spore" );
				return;
			}

			String squareString = split[ 1 ];
			int square = StringUtilities.parseInt( squareString );

			// Skip past command and square
			parameters = parameters.substring( command.length() ).trim();
			parameters = parameters.substring( squareString.length() ).trim();

			if ( parameters.indexOf( "mushroom" ) == -1 )
			{
				parameters = parameters.trim() + " mushroom";
			}

			int spore = ItemFinder.getFirstMatchingItem( parameters ).getItemId();

			if ( spore == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unknown spore: " + parameters );
				return;
			}

			MushroomManager.plantMushroom( square, spore );
		}
		else if ( command.equals( "pick" ) )
		{
			if ( split.length < 2 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Syntax: field pick square" );
				return;
			}

			String squareString = split[ 1 ];

			int square = StringUtilities.parseInt( squareString );
			MushroomManager.pickMushroom( square, true );
		}
		else if ( command.equals( "harvest" ) )
		{
			MushroomManager.harvestMushrooms();
		}

		String plot = MushroomManager.getMushroomManager( false );

		if ( KoLmafia.permitsContinue() )
		{
			StringBuffer plotDetails = new StringBuffer();
			plotDetails.append( "Current:" );
			plotDetails.append( KoLConstants.LINE_BREAK );
			plotDetails.append( plot );
			plotDetails.append( KoLConstants.LINE_BREAK );
			plotDetails.append( "Forecast:" );
			plotDetails.append( KoLConstants.LINE_BREAK );
			plotDetails.append( MushroomManager.getForecastedPlot( false ) );
			plotDetails.append( KoLConstants.LINE_BREAK );
			RequestLogger.printLine( plotDetails.toString() );
		}
	}
}
