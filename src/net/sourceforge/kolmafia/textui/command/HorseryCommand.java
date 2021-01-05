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

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class HorseryCommand
	extends AbstractCommand
{
	public HorseryCommand()
	{
		this.usage = " [init | -combat | stat | resist | regen | meat | random | spooky | normal | dark | crazy | pale | # ] - get the indicated horse";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !Preferences.getBoolean( "horseryAvailable" ) )
		{
			KoLmafia.updateDisplay( "You need a horsery first." );
			return;
		}
		int choice = StringUtilities.parseInt( parameters );
		if ( choice < 1 || choice > 4 )
		{
			if ( parameters.contains( "init" ) || parameters.contains( "regen" ) || parameters.startsWith( "normal" ) )
			{
				choice = 1;
			}
			else if ( parameters.contains( "-combat" ) || parameters.contains( "meat" ) || parameters.startsWith( "dark" ) )
			{
				choice = 2;
			}
			else if ( parameters.contains( "stat" ) || parameters.contains( "random" ) || parameters.startsWith( "crazy" ) )
			{
				choice = 3;
			}
			else if ( parameters.contains( "resist" ) || parameters.contains( "spooky" ) || parameters.startsWith( "pale" ) )
			{
				choice = 4;
			}
		}
		if ( choice < 1 || choice > 7 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, parameters + " is not a valid option." );
			return;
		}
		if ( choice == 1 && Preferences.getString( "_horsery" ).equals( "normal horse" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already have the normal horse." );
			return;
		}
		if ( choice == 2 && Preferences.getString( "_horsery" ).equals( "dark horse" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already have the dark horse." );
			return;
		}
		if ( choice == 3 && Preferences.getString( "_horsery" ).equals( "crazy horse" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already have the crazy horse." );
			return;
		}
		if ( choice == 4 && Preferences.getString( "_horsery" ).equals( "pale horse" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already have the pale horse." );
			return;
		}
		RequestThread.postRequest( new GenericRequest( "place.php?whichplace=town_right&action=town_horsery" ) );
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1266&option=" + choice ) );

	}
}
