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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;

public abstract class TowerDoorManager
{
	// Items for the tower doorway
	private static final AdventureResult UNIVERSAL_KEY = ItemPool.get( ItemPool.UNIVERSAL_KEY, 1 );

	private static class Lock
	{
		final AdventureResult key;	// The key that fits the lock
		final String action;		// The action name for the lock
		final boolean special;		// True if normal retrieve_item will not work
						// to get the key in Kingdom of Exploathing

		public Lock( int itemId, String action, boolean special )
		{
			this.key = ItemPool.get( itemId, 1 );
			this.action = action;
			this.special = special;
		}
	}

	private static final Lock[] LOCK_DATA =
	{
		new Lock( ItemPool.BORIS_KEY, "ns_lock1", true ),
		new Lock( ItemPool.JARLSBERG_KEY, "ns_lock2", true ),
		new Lock( ItemPool.SNEAKY_PETE_KEY, "ns_lock3", true ),
		new Lock( ItemPool.STAR_KEY, "ns_lock4", false ),
		new Lock( ItemPool.DIGITAL_KEY, "ns_lock5", true ),
		new Lock( ItemPool.SKELETON_KEY, "ns_lock6", false ),
	};

	public static AdventureResult actionToKey( final String action )
	{
		for ( Lock lock : TowerDoorManager.LOCK_DATA )
		{
			if ( action.equals( lock.action ) )
			{
				return lock.key;
			}
		}
		return null;
	}

	public static String keyToAction( final AdventureResult key )
	{
		return TowerDoorManager.keyToAction( key.getName() );
	}

	public static String keyToAction( final String keyName )
	{
		for ( Lock lock : TowerDoorManager.LOCK_DATA )
		{
			if ( keyName.equals( lock.key.getName() ) )
			{
				return lock.action;
			}
		}
		return null;
	}

	public static void parseTowerDoorResponse( final String action, final String responseText )
	{
		if ( action == null || action.equals( "" ) )
		{
			TowerDoorManager.parseTowerDoor( responseText );
			return;
		}

		if ( action.equals( "ns_doorknob" ) )
		{
			// You turn the knob and the door vanishes. I guess it was made out of the same material as those weird lock plates.
			if ( responseText.contains( "You turn the knob and the door vanishes" ) )
			{
				QuestDatabase.setQuestProgress( Quest.FINAL, "step6" );
			}
			return;
		}

		AdventureResult key = TowerDoorManager.actionToKey( action );

		if ( key == null )
		{
			return;
		}

		AdventureResult item =
			responseText.contains( "universal key" ) ?
			TowerDoorManager.UNIVERSAL_KEY :
			key;

		// You place Boris's key in the lock and turn it. You hear a
		// jolly bellowing in the distance as the lock vanishes, along
		// with the metal plate it was attached to. Huh.

		// You put Jarlsberg's key in the lock and turn it. You hear a
		// nasal, sort of annoying laugh in the distance as the lock
		// vanishes in a puff of rotten-egg-smelling smoke.

		// You put the key in the lock and hear the roar of a
		// motorcycle behind you. By the time you turn around to check
		// out the cool motorcycle guy he's gone, but when you turn
		// back to the lock it is <i>also</i> gone.

		// You put the key in and turn it. There is a flash of
		// brilliant starlight accompanied by a competent but not
		// exceptional drum solo, and when both have faded, the lock is
		// gone.

		// You put the skeleton key in the lock and turn it. The key,
		// the lock, and the metal plate the lock is attached to all
		// crumble to dust. And rust, in the case of the metal.

		// You put the digital key in the lock and turn it. A familiar
		// sequence of eight tones plays as the lock disappears.

		if ( responseText.contains( "the lock vanishes" ) ||
		     responseText.contains( "turn back to the lock" ) ||
		     responseText.contains( "the lock is gone" ) ||
		     responseText.contains( "crumble to dust" ) ||
		     responseText.contains( "the lock disappears" ) )
		{
			ResultProcessor.processResult( item.getNegation() );
			String keys = Preferences.getString( "nsTowerDoorKeysUsed" );
			Preferences.setString( "nsTowerDoorKeysUsed", keys + ( keys.equals( "" ) ? "" : "," ) + key.getDataName() );
		}
	}

	public static void parseTowerDoor( String responseText )
	{
		// Based on which locks are absent, deduce which keys have been used.

		StringBuilder buffer = new StringBuilder();

		for ( Lock lock : TowerDoorManager.LOCK_DATA )
		{
			if ( !responseText.contains( lock.action ) )
			{
				if ( buffer.length() > 0 )
				{
					buffer.append( "," );
				}
				buffer.append( lock.key.getDataName() );
			}
		}

		Preferences.setString( "nsTowerDoorKeysUsed", buffer.toString() );
	}

	public static boolean registerTowerDoorRequest( final String urlString )
	{
		String message = null;

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			String prefix = "[" + KoLAdventure.getAdventureCount() + "] ";
			RequestLogger.printLine();
			RequestLogger.updateSessionLog();
			message = prefix + "Tower Door";
		}
		else
		{
			message =
				action.equals( "ns_lock1" ) ? "Tower Door: Boris's lock" :
				action.equals( "ns_lock2" ) ? "Tower Door: Jarlsberg's lock" :
				action.equals( "ns_lock3" ) ? "Tower Door: Sneaky Pete's lock" :
				action.equals( "ns_lock4" ) ? "Tower Door: star lock" :
				action.equals( "ns_lock5" ) ? "Tower Door: digital lock" :
				action.equals( "ns_lock6" ) ? "Tower Door: skeleton lock" :
				action.equals( "ns_doorknob" ) ? "Tower Door: doorknob" :
				null;
		}

		if ( message == null )
		{
			return true;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}

	public static final void towerDoorScript()
	{
		// Is the Tower Door open? Go look at the tower.
		RequestThread.postRequest( new PlaceRequest( "nstower" ) );

		String status = Quest.FINAL.getStatus();
		if ( !status.equals( "step5" ) )
		{
			String message =
				status.equals( QuestDatabase.UNSTARTED ) ?
				"You haven't been given the quest to fight the Sorceress!" :
				QuestDatabase.isQuestLaterThan( status, "step5" ) ?
				"You have already opened the Tower Door." :
				"You haven't reached the Tower Door yet.";

			KoLmafia.updateDisplay( MafiaState.ERROR, message );
			return;
		}

		// Look at the door to decide what remains to be done
		RequestThread.postRequest( new PlaceRequest( "nstower_door" ) );

		String keys = Preferences.getString( "nsTowerDoorKeysUsed" );

		ArrayList<Lock> needed = new ArrayList<Lock>();
		for ( Lock lock : TowerDoorManager.LOCK_DATA )
		{
			if ( !keys.contains( lock.key.getName() ) )
			{
				needed.add( lock );
			}
		}

		// If we have any locks left to open, acquire the correct key and unlock them
		if ( needed.size() > 0 )
		{
			// First acquire all needed keys
			CoinmasterData coinmaster = CoinmasterRegistry.findCoinmaster( "Cosmic Ray's Bazaar" );
			boolean exploathing = KoLCharacter.isKingdomOfExploathing();
			for ( Lock lock : needed )
			{
				AdventureResult key = lock.key;
				boolean have = InventoryManager.hasItem( key );
				if ( !have && lock.special && exploathing )
				{
					// We have to get this from Cosmic Ray's Bazaar
					AdventureResult[] itemList = new AdventureResult[1];
					itemList[0] = key;
					CoinMasterRequest request = coinmaster.getRequest( true, itemList );
					RequestThread.postRequest( request );
					have = InventoryManager.hasItem( key );
				}
				else
				{
					// If we have the key, move it to inventory.
					// Otherwise, acquire it.
					have = InventoryManager.retrieveItem( key );
				}
				if ( !have )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to acquire " + key );
					return;
				}
			}

			// Then unlock each lock
			for ( Lock lock : needed )
			{
				RequestThread.postRequest( new PlaceRequest( "nstower_door", lock.action ) );
				keys = Preferences.getString( "nsTowerDoorKeysUsed" );
				if ( !keys.contains( lock.key.getName() ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Failed to open lock using " + lock.key );
					return;
				}
			}
		}

		// Now turn the doorknob
		RequestThread.postRequest( new PlaceRequest( "nstower_door", "ns_doorknob", true ) );

		status = Quest.FINAL.getStatus();
		if ( status.equals( "step6" ) )
		{
			KoLmafia.updateDisplay( "Tower Door open!" );
		}
	}
}
