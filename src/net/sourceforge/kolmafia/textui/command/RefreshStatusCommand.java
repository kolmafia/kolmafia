/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.StorageRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class RefreshStatusCommand
	extends AbstractCommand
{
	public RefreshStatusCommand()
	{
		this.usage = " all | status | equip | inv | storage | familiar | stickers | quests - resynchronize with KoL.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( parameters.equals( "all" ) )
		{
			KoLmafia.refreshSession();
			return;
		}
		else if ( parameters.equals( "status" ) || parameters.equals( "effects" ) )
		{
			ApiRequest.updateStatus();
		}
		else if ( parameters.equals( "gear" ) || parameters.startsWith( "equip" ) || parameters.equals( "outfit" ) )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.EQUIPMENT ) );
			parameters = "equip";
		}
		else if ( parameters.startsWith( "stick" ) )
		{
			RequestThread.postRequest( new EquipmentRequest( EquipmentRequest.BEDAZZLEMENTS ) );
			parameters = "equip";
		}
		else if ( parameters.startsWith( "inv" ) )
		{
			InventoryManager.refresh();
			return;
		}
		else if ( parameters.startsWith( "camp" ) )
		{
			RequestThread.postRequest( new CampgroundRequest() );
			return;
		}
		else if ( parameters.equals( "storage" ) )
		{
			StorageRequest.refresh();
			return;
		}
		else if ( parameters.startsWith( "familiar" ) || parameters.equals( "terrarium" ) )
		{
			parameters = "familiars";
			RequestThread.postRequest( new FamiliarRequest() );
		}
		else if ( parameters.equals( "quests" ) )
		{
			RequestThread.postRequest( new QuestLogRequest() );
			return;
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, parameters + " cannot be refreshed." );
			return;
		};

		ShowDataCommand.show( parameters );
	}
}
