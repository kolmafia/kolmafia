/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ClanFortuneRequest;
import net.sourceforge.kolmafia.request.ClanFortuneRequest.Buff;

public class FortuneCommand
	extends AbstractCommand
{
	public FortuneCommand()
	{
		this.usage = " - buff mus|mys|mox|familiar|meat|item [word1 word2 word3] | <playername> [word1 word2 word3] - get a buff or an item, "
		           + "using preference-defined words if none are specified."
				   + "\nIf playername has spaces, cannot specify words, and does not support playernames with 3 spaces";
	}
	
	@Override
	public void run( final String cmd, String parameters )
	{
		// Check that your clan has a fortune teller
		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What do you want to request from the clan fortune teller?" );
			return;
		}

		String[] params = parameters.split( "\\s" );

		if ( params[0].equals( "buff" ) || params[0].equals( "effect" ) )
		{
			if ( Preferences.getBoolean( "_clanFortuneBuffUsed" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You already received a buff from the clan fortune teller." );
				return;
			}
			String buffname = params[1];
			Buff buff;
			// get a buff
			if ( buffname.startsWith( "susie" ) || buffname.startsWith( "fam" ) )
			{
				buff = Buff.FAMILIAR;
			}
			else if ( buffname.startsWith( "hagnk" ) || buffname.startsWith( "item" ) )
			{
				buff = Buff.ITEM;
			}
			else if ( buffname.startsWith( "meat" ) )
			{
				buff = Buff.MEAT;
			}
			else if ( buffname.startsWith( "gunther" ) || buffname.startsWith( "mus" ) )
			{
				buff = Buff.MUSCLE;
			}
			else if ( buffname.startsWith( "gorgonzola" ) || buffname.startsWith( "mys" ) )
			{
				buff = Buff.MYSTICALITY;
			}
			else if ( buffname.startsWith( "shifty" ) || buffname.startsWith( "mox" ) )
			{
				buff = Buff.MOXIE;
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "That isn't a valid buff." );
				return;
			}
			
			if ( params.length == 2 )
			{
				RequestThread.postRequest( new ClanFortuneRequest( buff ) );
			}
			else if ( params.length == 5 )
			{
				RequestThread.postRequest( new ClanFortuneRequest( buff, params[2], params[3], params[4] ) );
			}
			else
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You need to choose all 3 words or none of the words for your compatibility test." );
				return;
			}
		}
		else
		{
			if ( Preferences.getInteger( "_clanFortuneConsultUses" ) == 3 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You already consulted with a clanmate 3 times today." );
				return;
			}
			if ( params.length == 4 )
			{
				RequestThread.postRequest( new ClanFortuneRequest( params[0], params[1], params[2], params[3] ) );
			}
			else
			{
				// If not 4 parameters, assume a name with spaces
				RequestThread.postRequest( new ClanFortuneRequest( parameters ) );
			}
		}
	}
}
