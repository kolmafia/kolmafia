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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.HashMultimap;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NPCStoreDatabase
	extends KoLDatabase
{
	private static final HashMultimap NPC_ITEMS = new HashMultimap();
	private static final AdventureResult LAB_KEY = new AdventureResult( 339, 1 );
	private static final AdventureResult RABBIT_HOLE = new AdventureResult( "Down the Rabbit Hole", 1, true );
	private static final Map storeNameById = new TreeMap();

	static
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "npcstores.txt", KoLConstants.NPCSTORES_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length != 4 )
			{
				continue;
			}

			String storeName = new String( data[0] );
			String storeId = new String( data[1] );
			String itemName = data[ 2 ];
			int itemId = ItemDatabase.getItemId( itemName );
			int price = StringUtilities.parseInt( data[ 3 ] );
			NPCStoreDatabase.storeNameById.put( storeId, storeName );
			NPCStoreDatabase.NPC_ITEMS.put( itemId, new NPCPurchaseRequest( storeName, storeId, itemId, price ) );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final String getStoreName( final String storeId )
	{
		return (String) NPCStoreDatabase.storeNameById.get( storeId );
	}

	public static final PurchaseRequest getPurchaseRequest( final String itemName )
	{
		int itemId = ItemDatabase.getItemId( itemName, 1, false );

		NPCPurchaseRequest foundItem = null;

		ArrayList items = NPCStoreDatabase.NPC_ITEMS.get( itemId );
		if ( items == null )
		{
			return null;
		}
		for ( Iterator i = items.iterator(); i.hasNext(); )
		{
			foundItem = (NPCPurchaseRequest) i.next();

			if ( !NPCStoreDatabase.canPurchase( foundItem.getStoreId(), foundItem.getShopName(),
				itemName ) )
			{
				continue;
			}

			foundItem.setCanPurchase( true );
			return foundItem;
		}

		if ( foundItem == null )
		{
			return null;
		}

		foundItem.setCanPurchase( false );
		return foundItem;
	}

	private static final boolean canPurchase( final String storeId, final String shopName,
		final String itemName )
	{
		if ( storeId == null )
		{
			return false;
		}

		// Check for whether or not the purchase can be made from a
		// guild store.  Store #1 is moxie classes, store #2 is for
		// mysticality classes, and store #3 is for muscle classes.

		String classType = KoLCharacter.getClassType();

		if ( storeId.equals( "1" ) )
		{
			return KoLCharacter.isMoxieClass() &&
				KoLCharacter.getGuildStoreOpen();
		}
		else if ( storeId.equals( "2" ) )
		{
			return (KoLCharacter.isMysticalityClass() || classType.equals( KoLCharacter.ACCORDION_THIEF ) && KoLCharacter.getLevel() >= 9) &&
				KoLCharacter.getGuildStoreOpen();
		}
		else if ( storeId.equals( "3" ) )
		{
			return ( ( KoLCharacter.isMuscleClass() && !KoLCharacter.isAvatarOfBoris() ) ||
				 ( classType.equals( KoLCharacter.ACCORDION_THIEF ) && KoLCharacter.getLevel() >= 9 ) ) &&
				KoLCharacter.getGuildStoreOpen();
		}
		else if ( storeId.equals( "a" ) )
		{
			// You can only get to the Tweedleporium if you can go
			// Down the Rabbit Hole
			return KoLConstants.activeEffects.contains( NPCStoreDatabase.RABBIT_HOLE );
		}
		else if ( storeId.equals( "b" ) )
		{
			return EquipmentManager.hasOutfit( 1 );
		}
		else if ( storeId.equals( "r" ) )
		{
			boolean available;
			if ( shopName.equals( "Barrrtleby's Barrrgain Books" ) )
			{
				available = !KoLCharacter.inBeecore();
			}
			else if ( shopName.equals( "Barrrtleby's Barrrgain Books (Bees Hate You)" ) )
			{
				available = KoLCharacter.inBeecore();
			}
			else
			{
				// What is this?
				return false;
			}

			if ( !available )
			{
				return false;
			}

			if ( Preferences.getInteger( "lastPirateEphemeraReset" ) == KoLCharacter.getAscensions()
				&& !Preferences.getString( "lastPirateEphemera" ).equalsIgnoreCase( itemName ) )
			{
				if ( NPCPurchaseRequest.PIRATE_EPHEMERA_PATTERN.matcher( itemName ).matches() )
				{
					return false;
				}
			}
			return EquipmentManager.hasOutfit( 9 ) ||
				InventoryManager.hasItem( ItemPool.PIRATE_FLEDGES );
		}
		else if ( storeId.equals( "4" ) || storeId.equals( "5" ) )
		{
			return KoLCharacter.knollAvailable();
		}
		else if ( storeId.equals( "j" ) )
		{
			return KoLCharacter.canadiaAvailable();
		}
		else if ( storeId.equals( "k" ) )
		{
			return KoLCharacter.getDispensaryOpen();
		}
		else if ( storeId.equals( "h" ) )
		{
			int level = KoLCharacter.getLevel();

			if ( shopName.equals( "Hippy Store (Pre-War)" ) )
			{
				if ( !InventoryManager.hasItem( ItemPool.DINGHY_DINGY ) || !EquipmentManager.hasOutfit( 2 ) )
				{
					return false;
				}

				if ( Preferences.getInteger( "lastFilthClearance" ) == KoLCharacter.getAscensions() )
				{
					return false;
				}

				if ( level < 12 )
				{
					return true;
				}

				return QuestLogRequest.isHippyStoreAvailable();
			}

			// Here, you insert any logic which is able to detect
			// the completion of the filthworm infestation and
			// which outfit was used to complete it.

			if ( Preferences.getInteger( "lastFilthClearance" ) != KoLCharacter.getAscensions() )
			{
				return false;
			}

			int outfit = 0;
			if ( shopName.equals( "Hippy Store (Hippy)" ) )
			{
				if ( !Preferences.getString( "currentHippyStore" ).equals( "hippy" ) )
				{
					return false;
				}

				outfit = 32;	// War Hippy Fatigues
			}

			else if ( shopName.equals( "Hippy Store (Fratboy)" ) )
			{
				if ( !Preferences.getString( "currentHippyStore" ).equals( "fratboy" ) )
				{
					return false;
				}

				outfit = 33;	// Frat Warrior Fatigues
			}

			else
			{
				// What is this?
				return false;
			}

			return QuestLogRequest.isHippyStoreAvailable() || EquipmentManager.hasOutfit( outfit );
		}

		// Check the quest log when determining if you've used the
		// black market map.

		else if ( storeId.equals( "l" ) )
		{
			return QuestLogRequest.isBlackMarketAvailable();
		}
		else if ( storeId.equals( "n" ) )
		{
			return KoLCharacter.gnomadsAvailable();
		}
		else if ( storeId.equals( "w" ) )
		{
			return QuestLogRequest.isWhiteCitadelAvailable();
		}
		else if ( storeId.equals( "y" ) )
		{
			return KoLCharacter.inBadMoon();
		}
		else if ( shopName.equals( "Gift Shop" ) )
		{
			return !KoLCharacter.inBadMoon();
		}

		// If it gets this far, then the item is definitely available
		// for purchase from the NPC store.

		return true;
	}

	public static final boolean contains( final String itemName )
	{
		return NPCStoreDatabase.contains( itemName, true );
	}

	public static final int price( final String itemName )
	{
		PurchaseRequest request = NPCStoreDatabase.getPurchaseRequest( itemName );
		return request == null ? 0 : request.getPrice();
	}

	public static final boolean contains( final String itemName, boolean validate )
	{
		PurchaseRequest item = NPCStoreDatabase.getPurchaseRequest( itemName );
		return item != null && ( !validate || item.canPurchaseIgnoringMeat() );
	}
}
