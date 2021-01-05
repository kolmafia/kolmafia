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
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class BoomBoxCommand
	extends AbstractCommand
{
	public BoomBoxCommand()
	{
		this.usage = " [giger | spooky | food | alive | dr | fists | damage | meat | silent | off | # ] - get the indicated buff";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !InventoryManager.hasItem( ItemPool.BOOMBOX ) )
		{
			KoLmafia.updateDisplay( "You need a SongBoom&trade; BoomBox first." );
			return;
		}
		int choice = StringUtilities.parseInt( parameters );
		if ( choice < 1 || choice > 6 )
		{
			if ( parameters.contains( "giger" ) || parameters.contains( "spooky" ) )
			{
				choice = 1;
			}
			else if ( parameters.contains( "food" ) )
			{
				choice = 2;
			}
			else if ( parameters.contains( "alive" ) || parameters.contains( "dr" ) )
			{
				choice = 3;
			}
			else if ( parameters.contains( "fists" ) || parameters.contains( "damage" ) )
			{
				choice = 4;
			}
			else if ( parameters.contains( "meat" ) )
			{
				choice = 5;
			}
			else if ( parameters.contains( "silent" ) || parameters.contains( "off" ) )
			{
				choice = 6;
			}
		}
		if ( choice < 1 || choice > 7 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, parameters + " is not a valid option." );
			return;
		}
		if ( choice == 1 && Preferences.getString( "boomBoxSong" ).equals( "Eye of the Giger" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already got Eye of the Giger playing." );
			return;
		}
		if ( choice == 2 && Preferences.getString( "boomBoxSong" ).equals( "Food Vibrations" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already got Food Vibrations playing." );
			return;
		}
		if ( choice == 3 && Preferences.getString( "boomBoxSong" ).equals( "Remainin' Alive" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already got Remainin' Alive playing." );
			return;
		}
		if ( choice == 4 && Preferences.getString( "boomBoxSong" ).equals( "These Fists Were Made for Punchin'" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already got These Fists Were Made for Punchin' playing." );
			return;
		}
		if ( choice == 5 && Preferences.getString( "boomBoxSong" ).equals( "Total Eclipse of Your Meat" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already got Total Eclipse of Your Meat playing." );
			return;
		}
		if ( choice == 6 && Preferences.getString( "boomBoxSong" ).equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already switched it off." );
			return;
		}
		int previousChoice = Preferences.getInteger( "choiceAdventure1312" );
		Preferences.setInteger( "choiceAdventure1312", choice );
		UseItemRequest useBoomBox = UseItemRequest.getInstance( ItemPool.BOOMBOX );
		RequestThread.postRequest( useBoomBox );
		Preferences.setInteger( "choiceAdventure1312", previousChoice );
	}
}
