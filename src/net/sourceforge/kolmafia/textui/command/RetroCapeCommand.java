/**
 * Copyright (c) 2005-2020, KoLmafia development team
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
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.session.InventoryManager;

public class RetroCapeCommand
	extends AbstractCommand
{
	public RetroCapeCommand()
	{
		this.usage = " [muscle | mysticality | moxie | vampire | heck | robot] [hold | thrill | kiss | kill]";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !InventoryManager.hasItem( ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You need a knock-off retro superhero cape" );
			return;
		}

		parameters = parameters.trim();

		String[] params = parameters.split( " " );

		Integer superhero = null;
		Integer washingInstruction = null;

		for ( String param : params )
		{
			if ( param.contains( "mus" ) || param.equals( "vampire" ) )
			{
				superhero = 1;
			}
			else if ( param.contains( "mys" ) || param.equals( "heck" ) )
			{
				superhero = 2;
			}
			else if ( param.contains( "mox" ) || param.equals( "robot" ) )
			{
				superhero = 3;
			}
			else if ( param.equals( "hold" ) )
			{
				washingInstruction = 2;
			}
			else if ( param.equals( "thrill" ) )
			{
				washingInstruction = 3;
			}
			else if ( param.equals( "kiss" ) )
			{
				washingInstruction = 4;
			}
			else if ( param.equals( "kill" ) )
			{
				washingInstruction = 5;
			}
		}

		KoLmafia.updateDisplay( "Reconfiguring retro cape" );

		GenericRequest request = new GenericRequest( "inventory.php?action=hmtmkmkm", false );
		RequestThread.postRequest( request );

		if ( washingInstruction != null )
		{
			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1437" );
			request.addFormField( "option", Integer.toString( washingInstruction ) );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );
		}

		if ( superhero != null )
		{
			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1437" );
			request.addFormField( "option", "1" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );	

			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1438" );
			request.addFormField( "option", Integer.toString( superhero ) );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );

			request = new GenericRequest( "choice.php" );
			request.addFormField( "whichchoice", "1438" );
			request.addFormField( "option", "4" );
			request.addFormField( "pwd", GenericRequest.passwordHash );
			RequestThread.postRequest( request );
		}

		request = new GenericRequest( "choice.php" );
		request.addFormField( "whichchoice", "1437" );
		request.addFormField( "option", "6" );
		request.addFormField( "pwd", GenericRequest.passwordHash );
		RequestThread.postRequest( request );
	}
}
