/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GuildRequest;

public class GuildUnlockManager
{
	public static void unlockGuild()
	{
		// Don't even try in Axecore
		if ( KoLCharacter.inAxecore() )
		{
			KoLmafia.updateDisplay( "Boris needed no guild. Why would you?" );
			return;
		}

		// See if we've already unlocked the Guild
		if ( KoLCharacter.getGuildStoreOpen() )
		{
			KoLmafia.updateDisplay( "Guild already unlocked." );
			return;
		}

		KoLmafia.updateDisplay( "Speaking to guild master..." );
		GuildRequest master = new GuildRequest( "challenge" );
		RequestThread.postRequest( master );

		if ( KoLCharacter.getGuildStoreOpen() )
		{
			KoLmafia.updateDisplay( "Guild already unlocked." );
			return;
		}

		KoLmafia.updateDisplay( "Completing guild task..." );

		Stat stat = KoLCharacter.mainStat();

		String locationName;
		String snarfblat;
		String choice;
		AdventureResult item;

		switch ( stat )
		{
		case MUSCLE:
			locationName = "Outskirts of The Knob";
			snarfblat = AdventurePool.OUTSKIRTS_OF_THE_KNOB_ID;
			choice = "543";
			item = ItemPool.get( ItemPool.BIG_KNOB_SAUSAGE, 1 );
			break;
		case MYSTICALITY:
			locationName = "Haunted Pantry";
			snarfblat = AdventurePool.HAUNTED_PANTRY_ID;
			choice = "544";
			item = ItemPool.get( ItemPool.EXORCISED_SANDWICH, 1 );
			break;
		case MOXIE:
			locationName = "Sleazy Back Alley";
			snarfblat = AdventurePool.SLEAZY_BACK_ALLEY_ID;
			choice = "542";
			item = EquipmentManager.getEquipment( EquipmentManager.PANTS );
			if ( item == EquipmentRequest.UNEQUIP )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Put on some pants and try again." );
				return;
			}
			break;
		default:
			KoLmafia.updateDisplay( MafiaState.ERROR, "What class are you?" );
			return;
		}

		// Make an adventure request
		AdventureRequest request = new AdventureRequest( locationName, "adventure.php", snarfblat );

		// Remember how many of the goal item we already have
		int initialCount = item.getCount( KoLConstants.inventory );
		int currentCount = initialCount;

		// Enable the choice adventure
		String setting = "choiceAdventure" + choice;
		int oldChoice = Preferences.getInteger( setting );
		Preferences.setInteger( setting, 1 );

		while ( KoLmafia.permitsContinue() &&
			KoLCharacter.getCurrentHP() > 0 &&
			KoLCharacter.getAdventuresLeft() > 0 &&
			currentCount == initialCount )
		{
			// Visit the adventure location
			RequestThread.postRequest( request );

			// See if the desired item is in inventory
			currentCount = item.getCount( KoLConstants.inventory );
		}

		// Restore the choice adventure setting
		Preferences.setInteger( setting, oldChoice );

		// Put on your pants back on
		if ( stat == Stat.MOXIE )
		{
			KoLmafia.updateDisplay( "Putting your pants back on..." );
			RequestThread.postRequest( new EquipmentRequest( item ) );
		}

		// See if we achieved our goal
		if ( currentCount == initialCount )
		{
			KoLmafia.updateDisplay( "Guild was not unlocked." );
			return;
		}

		KoLmafia.updateDisplay( "Speaking to guild master again..." );
		RequestThread.postRequest( master );

		KoLmafia.updateDisplay( "Getting meatcar quest..." );
		GuildRequest paco = new GuildRequest( "paco" );
		RequestThread.postRequest( paco );

		KoLmafia.updateDisplay( "Guild successfully unlocked." );
	}
}
