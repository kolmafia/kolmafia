/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SnowsuitCommand
	extends AbstractCommand
{
	public static final String[][] DECORATION =
	{
		{	"eyebrows", "1"	},
		{	"smirk", "2"	},
		{	"nose", "3"	},
		{	"goatee", "4"	},
		{	"hat", "5"	},
	};

	public SnowsuitCommand()
	{
		this.usage = "[?] <decoration> - decorate Snowsuit (and equip it if unequipped)";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		String currentDecoration = Preferences.getString( "snowsuit" );

		if ( parameters.length() == 0 )
		{
			KoLmafia.updateDisplay( "Current decoration on Snowsuit is " + currentDecoration );			
			return;
		}

		String decoration = parameters;
		String choice = "0";

		for ( int i = 0; i < SnowsuitCommand.DECORATION.length; i++ )
		{
			if ( decoration.equalsIgnoreCase( SnowsuitCommand.DECORATION[ i ][ 0 ] ) )
			{
				choice = SnowsuitCommand.DECORATION[ i ][ 1 ];
				decoration = SnowsuitCommand.DECORATION[ i ][ 0 ];
				break;
			}
		}

		if ( choice.equals( "0" ) )
		{
			KoLmafia.updateDisplay( "Decoration " + decoration + " not recognised. Valid values are eyebrows, goatee, hat, nose and smirk" );
			return;
		}

		if ( EquipmentManager.getEquipment( EquipmentManager.FAMILIAR ).getItemId() != ItemPool.SNOW_SUIT )
		{
			AdventureResult snowsuit = ItemPool.get( ItemPool.SNOW_SUIT );
			RequestThread.postRequest( new EquipmentRequest( snowsuit, EquipmentManager.FAMILIAR ) );
		}

		if ( decoration.equalsIgnoreCase( currentDecoration ) )
		{
			KoLmafia.updateDisplay( "Decoration " + decoration + " already equipped." );
			return;
		}

		if ( KoLmafia.permitsContinue() )
		{
			RequestThread.postRequest( new GenericRequest( "inventory.php?action=decorate" ) );
		}
		if ( KoLmafia.permitsContinue() )
		{
			RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=640&option=" + choice ) );
		}
		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( "Snowsuit decorated with " + decoration + "." );
		}
	}
}
