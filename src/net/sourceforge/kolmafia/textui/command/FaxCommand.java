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
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.ClanLoungeRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class FaxCommand
	extends AbstractCommand
{
	public FaxCommand()
	{
		this.usage = " send | put | receive | get - use the fax machine in your clan's VIP lounge";
	}

	@Override
	public void run( final String cmd, String parameter )
	{
		parameter = parameter.trim();
		if ( parameter.equals( "" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "What do you want to do with the fax machine?" );
			return;
		}

		int option = ClanLoungeRequest.findFaxOption( parameter );
		if ( option == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "I don't understand what it means to '" + parameter + "' a fax." );
			return;
		}

		boolean hasPhotocopy = InventoryManager.hasItem( ItemPool.PHOTOCOPIED_MONSTER );
		if ( option == 1 && !hasPhotocopy )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You cannot send a fax without a photocopied monster in your inventory" );
			return;
		}
		if ( option == 2 && hasPhotocopy )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You cannot receive a fax with a photocopied monster in your inventory" );
			return;
		}

		RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.FAX_MACHINE, option ) );
	}
}
