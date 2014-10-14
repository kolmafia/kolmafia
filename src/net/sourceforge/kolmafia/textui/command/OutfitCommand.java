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

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public class OutfitCommand
	extends AbstractCommand
{
	public OutfitCommand()
	{
		this.usage = " [list <filter>] | save <name> | checkpoint | <name> - list, save, restore, or change outfits.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.startsWith( "list" ) )
		{
			ShowDataCommand.show( "outfits " + parameters.substring( 4 ).trim() );
			return;
		}
		else if ( parameters.startsWith( "save " ) )
		{
			RequestThread.postRequest( new EquipmentRequest( parameters.substring( 4 ).trim() ) );
			return;
		}
		else if ( parameters.length() == 0 )
		{
			ShowDataCommand.show( "outfits" );
			return;
		}
		else if ( parameters.equalsIgnoreCase( "checkpoint" ) )
		{
			SpecialOutfit.restoreExplicitCheckpoint();
			return;
		}

		SpecialOutfit intendedOutfit = EquipmentManager.getMatchingOutfit( parameters );

		if ( intendedOutfit == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "No outfit found matching: " + parameters );
			return;
		}

		if ( intendedOutfit != SpecialOutfit.PREVIOUS_OUTFIT && !EquipmentManager.retrieveOutfit( intendedOutfit ) )
		{
			return;
		}

		RequestThread.postRequest( new EquipmentRequest( intendedOutfit ) );
	}
}
