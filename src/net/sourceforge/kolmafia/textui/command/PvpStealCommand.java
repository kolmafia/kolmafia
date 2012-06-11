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

import net.sourceforge.kolmafia.session.PvpManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PvpStealCommand
	extends AbstractCommand
{
	public PvpStealCommand()
	{
		this.usage = " [attacks] ( flowers | loot | fame) [muscle|myst|moxie|ballyhoo] - commit random acts of PvP [using the specified stance].";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		String[] params = parameters.split( " " );
		
		int count = params.length;

		if ( count == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Steal what?" );
			return;
		}

		int offset = 0;

		int attacks = 0;
		if ( StringUtilities.isNumeric( params[ 0 ] ) )
		{
			attacks = StringUtilities.parseInt( params[ 0 ] );
			offset += 1;
		}

		if ( count == offset )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Steal " + attacks + " what?" );
			return;
		}

		String missionType = params[ offset ];
		String mission;

		if ( missionType.equals( "flowers" ) || missionType.equals( "fame" ) )
		{
			mission = missionType;
		}
		else if ( missionType.startsWith( "loot" ) )
		{
			mission = "lootwhatever";
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "What do you want to steal?" );
			return;
		}

		offset += 1;
		
		String stanceString = "best stat";
		int stance = 0;
		
		if ( count > offset )
		{
			stanceString = params[ offset ];
			if ( stanceString.equals( "muscle" ) )
			{
				stance = PvpManager.MUSCLE_STANCE;
			}
			else if ( stanceString.equals( "myst" ) )
			{
				stance = PvpManager.MYST_STANCE;
			}
			else if ( stanceString.equals( "moxie" ) )
			{
				stance = PvpManager.MOXIE_STANCE;
			}
			else if ( stanceString.equals( "ballyhoo" ) )
			{
				stance = PvpManager.BALLYHOO_STANCE;
			}
		}

		KoLmafia.updateDisplay( "Use " + ( attacks == 0 ? "all remaining" : String.valueOf( attacks ) ) + " PVP attacks to steal " +  missionType + " via " + stanceString );

		PvpManager.executePvpRequest( attacks, mission, stance );
	}
}
