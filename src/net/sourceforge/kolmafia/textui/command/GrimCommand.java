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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

public class GrimCommand
	extends AbstractCommand
{
	public GrimCommand()
	{
		this.usage = " init|hpmp|damage - get a Grim Brother buff";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( KoLCharacter.findFamiliar( FamiliarPool.GRIM_BROTHER ) == null )
		{
			KoLmafia.updateDisplay( "You don't have a Grim Brother" );
			return;
		}
		if ( Preferences.getBoolean( "_grimBuff" ) )
		{
			KoLmafia.updateDisplay( "You already received a Grim Brother effect today" );
			return;
		}

		int option = 0;
		if ( parameters.startsWith( "init" ) || parameters.startsWith( "soles" ) )
		{
			option = 1;
		}
		else if ( parameters.startsWith( "hpmp" ) || parameters.startsWith( "angry" ) )
		{
			option = 2;
		}
		else if ( parameters.startsWith( "damage" ) || parameters.startsWith( "grumpy" ) )
		{
			option = 3;
		}

		if ( option == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Pick a valid Grim Brother buff" );
			return;
		}

		RequestThread.postRequest( new GenericRequest( "familiar.php?action=chatgrim" ) );
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=835&option=" + option ) );
	}
}
