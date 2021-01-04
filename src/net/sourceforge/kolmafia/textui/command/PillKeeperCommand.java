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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.session.InventoryManager;

public class PillKeeperCommand
	extends AbstractCommand
{
	public PillKeeperCommand()
	{
		this.usage = " [free] explode | extend | noncombat | element | stat | familiar | semirare | random";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !InventoryManager.hasItem( ItemPool.PILL_KEEPER ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need an Eight Days a Week Pill Keeper" );
			return;
		}

		boolean freePillUsed = Preferences.getBoolean( "_freePillKeeperUsed" );
		if ( ( KoLCharacter.getSpleenLimit() - KoLCharacter.getSpleenUse() ) < 3 && freePillUsed )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your spleen has been abused enough today" );
			return;
		}

		if ( parameters.contains( "free" ) && freePillUsed )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Free pill keeper use already spent" );
			return;
		}

		int choice = 0;
		String pilltext = "";
		if ( parameters.contains( "exp" ) )
		{
			choice = 1;
			pilltext = "Monday - Explodinall";
			if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.EVERYTHING_LOOKS_YELLOW ) ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Everything already looks yellow" );
				return;
			}
		}
		else if ( parameters.contains( "ext" ) )
		{
			choice = 2;
			pilltext = "Tuesday - Extendicillin";
		}
		else if ( parameters.contains( "non" ) )
		{
			choice = 3;
			pilltext = "Wednesday - Sneakisol";
		}
		else if ( parameters.contains( "ele" ) )
		{
			choice = 4;
			pilltext = "Thursday - Rainbowolin";
		}
		else if ( parameters.contains( "sta" ) )
		{
			choice = 5;
			pilltext = "Friday - Hulkien";
		}
		else if ( parameters.contains( "fam" ) )
		{
			choice = 6;
			pilltext = "Saturday - Fidoxene";
		}
		else if ( parameters.contains( "sem" ) )
		{
			choice = 7;
			pilltext = "Sunday - Surprise Me";
		}
		else if ( parameters.contains( "ran" ) )
		{
			choice = 8;
			pilltext = "Funday - Telecybin";
		}
		if ( choice == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid choice" );
			return;
		}

		KoLmafia.updateDisplay( "Taking pills for " + pilltext );

		GenericRequest request = new GenericRequest( "main.php?eowkeeper=1", false );
		RequestThread.postRequest( request );

		request = new GenericRequest( "choice.php" );
		request.addFormField( "whichchoice", "1395" );
		request.addFormField( "option", Integer.toString( choice ) );
		request.addFormField( "pwd", GenericRequest.passwordHash );
		RequestThread.postRequest( request );
	}
}
