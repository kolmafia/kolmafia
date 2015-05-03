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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.PvpManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PvpStealCommand
	extends AbstractCommand
{
	public PvpStealCommand()
	{
		this.usage = " [attacks] ( flowers | loot | fame) <stance> - commit random acts of PvP using the specified stance.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !PvpManager.checkStances() )
		{
			KoLmafia.updateDisplay( "Cannot determine valid stances" );
			return;
		}

		parameters = parameters.trim();
		
		if ( parameters.equals( "" ) )
		{
			for ( Integer option : PvpManager.optionToStance.keySet() )
			{
				RequestLogger.printLine( option + ": " + PvpManager.optionToStance.get( option ) ); 
			}
			return;
		}

		int attacks = 0;
		String mission = null;
		int stance = 0;

		int spaceIndex = parameters.indexOf( " " );

		if ( spaceIndex == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Must specify both mission and stance" );
			return;
		}

		String param = parameters.substring( 0, spaceIndex );
		parameters = parameters.substring( spaceIndex ).trim();

		if ( StringUtilities.isNumeric( param ) )
		{
			attacks = StringUtilities.parseInt( param );

			spaceIndex = parameters.indexOf( " " );
			if ( spaceIndex == -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Must specify both mission and stance" );
				return;
			}

			param = parameters.substring( 0, spaceIndex );
			parameters = parameters.substring( spaceIndex ).trim();
		}

		String missionType = param;

		if ( missionType.equals( "flowers" ) || missionType.equals( "fame" ) )
		{
			mission = missionType;
		}
		else if ( missionType.startsWith( "loot" ) )
		{
			if ( !KoLCharacter.canInteract() )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "You cannot attack for loot now." );
				return;
			}
			mission = "lootwhatever";
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to steal?" );
			return;
		}
		
		String stanceString = parameters;

		if ( StringUtilities.isNumeric( stanceString ) )
		{
			stance = StringUtilities.parseInt( stanceString );
			stanceString = PvpManager.findStance( stance );
			if ( stanceString == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, stance + " is not a valid stance" );
				return;
			}
		}
		else
		{
			// Find stance using fuzzy matching
			stance = PvpManager.findStance( stanceString );
			if ( stance < 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "\"" + stanceString + "\" does not uniquely match a currently known stance" );
				return;
			}
			stanceString = PvpManager.findStance( stance );
		}

		KoLmafia.updateDisplay( "Use " + ( attacks == 0 ? "all remaining" : String.valueOf( attacks ) ) + " PVP attacks to steal " +  missionType + " via " + stanceString );

		PvpManager.executePvpRequest( attacks, mission, stance );
	}
}
