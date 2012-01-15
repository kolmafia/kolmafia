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

import net.sourceforge.kolmafia.request.ClanLoungeRequest;

public class PoolCommand
	extends AbstractCommand
{
	public PoolCommand()
	{
		this.usage = " type [,type [,type]] - play pool games in your clan's VIP lounge";
	}

	public void run( final String cmd, String parameters )
	{
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "What stance do you wish to take?" );
			return;
		}

		String[] split = parameters.split( "," );
		if ( split.length < 1 || split.length > 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Specify from 1 to 3 pool games" );
			return;
		}

		int [] option = new int[ split.length ];
		for ( int i = 0; i < split.length; ++i )
		{
			String tag = split[i].trim();
			option[i] = ClanLoungeRequest.findPoolGame( tag );
			if ( option[i] == 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "I don't understand what a '" + tag + "' pool game is." );
				return;
			}
		}

		for ( int i = 0; i < option.length; ++i )
		{
			RequestThread.postRequest( new ClanLoungeRequest( ClanLoungeRequest.POOL_TABLE, option[i] ) );
		}
	}
}
