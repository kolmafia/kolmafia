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

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MummeryRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class MummeryCommand
	extends AbstractCommand
{
	public MummeryCommand()
	{
		this.usage = " [muscle | myst | moxie | hp | mp | item | meat | # ] - put the indicated costume on your familiar";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !KoLConstants.inventory.contains( ItemPool.get( ItemPool.MUMMING_TRUNK, 1 ) ) )
		{
			KoLmafia.updateDisplay( "You need a mumming trunk first." );
			return;
		}
		if ( KoLCharacter.currentFamiliar == FamiliarData.NO_FAMILIAR )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need to have a familiar to put a costume on." );
			return;
		}
		int choice = StringUtilities.parseInt( parameters );
		if ( choice < 1 || choice > 7 )
		{
			if ( parameters.contains( "meat" ) )
			{
				choice = 1;
			}
			else if ( parameters.contains( "mp" ) )
			{
				choice = 2;
			}
			else if ( parameters.contains( "mus" ) )
			{
				choice = 3;
			}
			else if ( parameters.contains( "item" ) )
			{
				choice = 4;
			}
			else if ( parameters.contains( "mys" ) )
			{
				choice = 5;
			}
			else if ( parameters.contains( "hp" ) )
			{
				choice = 6;
			}
			else if ( parameters.contains( "mox" ) )
			{
				choice = 7;
			}
		}
		if ( choice < 1 || choice > 7 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, parameters + " is not a valid option." );
			return;
		}
		if ( Preferences.getString( "_mummeryUses" ).contains( String.valueOf( choice ) ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already applied the " + parameters + " costume today." );
			return;
		}
		RequestThread.postRequest( new MummeryRequest( choice ) );

	}
}
