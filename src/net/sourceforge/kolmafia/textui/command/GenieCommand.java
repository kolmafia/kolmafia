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

import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenieRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class GenieCommand
	extends AbstractCommand
{
	public GenieCommand()
	{
		this.usage = " effect <effectname> | monster <monstername> | stat (mus|mys|mox|all) | meat | item (pony|pocket|shirt) | wish <wish> | freedom";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		String wish = "";
		if ( parameters.startsWith( "wish " ) )
		{
			wish = parameters.substring( 5 );
		}
		// meat
		else if ( parameters.equals( "meat" ) )
		{
			wish = "I was rich";
		}
		// item
			// pocket wish
			// pony
			// shirt
		else if ( parameters.startsWith( "item " ) )
		{
			parameters = parameters.substring( 5 );
			if ( parameters.startsWith( "pocket" ) )
			{
				if ( Preferences.getInteger( "_genieWishesUsed" ) == 3 || !InventoryManager.hasItem( ItemPool.GENIE_BOTTLE ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Don't use a pocket wish to make a pocket wish." );
					return;
				}
				wish = "for more wishes";
			}
			else if ( parameters.startsWith( "pony" ) )
			{
				wish = "for a pony";
			}
			else if ( parameters.startsWith( "shirt" ) )
			{
				wish = "for a blessed rustproof +2 gray dragon scale mail";
			}
		}
		// stat [mus|mys|mox|all]
		else if ( parameters.startsWith( "stat " ) )
		{
			parameters = parameters.substring( 5 );
			if ( parameters.startsWith( "mus" ) )
			{
				wish = "I was a little bit taller";
			}
			else if ( parameters.startsWith( "mys" ) )
			{
				wish = "I had a rabbit in a hat with a bat";
			}
			else if ( parameters.startsWith( "mox" ) )
			{
				wish = "I was a baller";
			}
			else if ( parameters.startsWith( "all" ) )
			{
				wish = "I was big";
			}
		}
		// freedom
		else if ( parameters.equals( "freedom" ) )
		{
			wish = "you were free";
		}
		// effect [name]
		else if ( parameters.startsWith( "effect " ) )
		{
			parameters = parameters.substring( 7 );
			List<String> effectNames = EffectDatabase.getMatchingNames( parameters );
			if ( effectNames.size() != 1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, parameters + "does not match exactly one effect" );
				return;
			}
			String name = effectNames.get( 0 );
			wish = "to be " + name;
		}
		// monster
		else if ( parameters.startsWith( "monster " ) )
		{
			parameters = parameters.substring( 8 );
			MonsterData monster = MonsterDatabase.findMonster( parameters, true, false );
			if ( monster == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, parameters + " does not match a monster." );
				return;
			}
			wish = "to fight " + parameters;
		}

		if ( wish != "" )
		{
			RequestThread.postRequest( new GenieRequest( wish ) );
		}
	}
}
