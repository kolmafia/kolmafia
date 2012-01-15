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

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.WineCellarRequest;

import net.sourceforge.kolmafia.session.InventoryManager;

public class SummonDemonCommand
	extends AbstractCommand
{
	public SummonDemonCommand()
	{
		this.usage = " <demonName> | <effect> | <location> | <number> - use the Summoning Chamber.";
	}

	public void run( final String cmd, final String parameters )
	{
		if ( parameters.length() == 0 )
		{
			return;
		}

		if ( Preferences.getBoolean( "demonSummoned" ) )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "You've already summoned a demon today." );
			return;
		}

		if ( !InventoryManager.retrieveItem( ItemPool.BLACK_CANDLE, 3 ) )
		{
			return;
		}

		if ( !InventoryManager.retrieveItem( ItemPool.EVIL_SCROLL ) )
		{
			return;
		}

		String demon = parameters;
		if ( Character.isDigit( parameters.charAt( 0 ) ) )
		{
			demon = Preferences.getString( "demonName" + parameters );
		}
		else
		{
			for ( int i = 0; i < KoLAdventure.DEMON_TYPES.length; ++i )
			{
				String location = KoLAdventure.DEMON_TYPES[ i ][ 0 ];
				if ( location != null && parameters.equalsIgnoreCase( location ) )
				{
					demon = Preferences.getString( "demonName" + ( i + 1 ) );
					break;
				}

				String effect = KoLAdventure.DEMON_TYPES[ i ][ 1 ];
				if ( effect != null && parameters.equalsIgnoreCase( effect ) )
				{
					demon = Preferences.getString( "demonName" + ( i + 1 ) );
					break;
				}

				String name = Preferences.getString( "demonName" + ( i + 1 ) );
				if ( parameters.equalsIgnoreCase( name ) )
				{
					demon = name;
					break;
				}
			}
		}

		if ( demon.equals( "" ) )
		{
			KoLmafia.updateDisplay(
				KoLConstants.ERROR_STATE, "You don't know the name of that demon." );
			return;
		}

		WineCellarRequest demonSummon = new WineCellarRequest( demon );

		KoLmafia.updateDisplay( "Summoning " + demon + "..." );
		RequestThread.postRequest( demonSummon );
	}
}
