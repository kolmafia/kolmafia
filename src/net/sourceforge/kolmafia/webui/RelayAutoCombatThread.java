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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.webui;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.utilities.PauseObject;

public class RelayAutoCombatThread
	extends Thread
{
	private String desiredAction;
	private final PauseObject pauser = new PauseObject();

	public RelayAutoCombatThread()
	{
		super( "LocalRelayCombatThread" );
		this.setDaemon( true );
		this.start();
	}

	public void wake( final String desiredAction )
	{
		this.desiredAction = desiredAction;

		if ( !FightRequest.isTrackingFights() )
		{
			FightRequest.beginTrackingFights();
		}

		this.pauser.unpause();
	}

	@Override
	public void run()
	{
		while ( true )
		{
			this.pauser.pause();

			if ( this.desiredAction == null )
			{
				FightRequest.INSTANCE.run();
			}
			else
			{
				FightRequest.INSTANCE.runOnce( this.desiredAction );
			}

			FightRequest.stopTrackingFights();
		}
	}
}
