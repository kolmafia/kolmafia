/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.util.ArrayList;

public class NPCStoreDatabase extends KoLDatabase
{
	private static final ArrayList NPC_ITEMS = new ArrayList();

	private static final AdventureResult LAB_KEY = new AdventureResult( 339, 1 );

	static
	{
		BufferedReader reader = getReader( "npcstores.txt" );

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 4 )
			{
				NPC_ITEMS.add( new MallPurchaseRequest( data[0], data[1],
					TradeableItemDatabase.getItemId( data[2] ), parseInt( data[3] ) ) );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}
	}

	public static final MallPurchaseRequest getPurchaseRequest( String itemName )
	{
		int itemId = TradeableItemDatabase.getItemId( itemName );

		MallPurchaseRequest foundItem = null;
		MallPurchaseRequest currentItem = null;

		for ( int i = 0; i < NPC_ITEMS.size(); ++i )
		{
			currentItem = (MallPurchaseRequest) NPC_ITEMS.get(i);
			if ( currentItem.getItemId() != itemId )
				continue;

			foundItem = currentItem;
			if ( !canPurchase( foundItem.getStoreId(), foundItem.getShopName() ) )
				continue;

			foundItem.setCanPurchase( true );
			return foundItem;
		}

		if ( foundItem == null )
			return null;

		foundItem.setCanPurchase( false );
		return foundItem;
	}

	private static boolean canPurchase( String storeId, String shopName )
	{
		if ( storeId == null )
			return false;

		// Check for whether or not the purchase can be made from a
		// guild store.  Store #1 is moxie classes, store #2 is for
		// mysticality classes, and store #3 is for muscle classes.

		String classType = KoLCharacter.getClassType();

		if ( storeId.equals( "1" ) )
			return KoLCharacter.isMoxieClass();

		else if ( storeId.equals( "2" ) )
			return KoLCharacter.isMysticalityClass() || (classType.equals( KoLCharacter.ACCORDION_THIEF ) && KoLCharacter.getLevel() >= 9);

		else if ( storeId.equals( "3" ) )
			return KoLCharacter.isMuscleClass() || (classType.equals( KoLCharacter.ACCORDION_THIEF ) && KoLCharacter.getLevel() >= 9);

		// If the person is trying to get one of the items from the bugbear
		// store, then the item is not available if they don't have the
		// bugbear outfit.  Of course, some items are available from the
		// Degrassi knoll bakery, so fallback on that when possible.

		else if ( storeId.equals( "b" ) )
			return EquipmentDatabase.hasOutfit( 1 );

		// If the person is not in a muscle sign, then items from the
		// Degrassi Knoll are not available.

		else if ( (storeId.equals( "4" ) || storeId.equals( "5" )) )
			return KoLCharacter.inMuscleSign();

		// If the person is not in a mysticality sign, then items from the
		// Canadia Jewelers are not available.

		else if ( storeId.equals( "j" ) )
			return KoLCharacter.inMysticalitySign();

		// If the person is trying to get the items from the laboratory,
		// then the item is not available if they don't have the elite
		// guard uniform, and not available if out of Ronin.

		else if ( storeId.equals( "g" ) )
			return KoLCharacter.hasItem( LAB_KEY ) && EquipmentDatabase.hasOutfit( 5 );

		// If the person is trying to get one of the items from the hippy
		// store, then the item is not available if they don't have the
		// hippy outfit.

		else if ( storeId.equals( "h" ) )
		{
			int level = KoLCharacter.getLevel();

			if ( shopName.equals( "Hippy Store (Pre-War)" ) )
				return level < 12 && inventory.contains( KoLAdventure.DINGHY ) && EquipmentDatabase.hasOutfit( 2 );

			// Here, you insert any logic which is able to detect the
			// completion of the filthworm infestation and which outfit
			// was used to complete it.  But, for now, just assume that
			// the store is not accessible.

			if ( level < 12 || StaticEntity.getIntegerProperty( "lastFilthClearance" ) != KoLCharacter.getAscensions() )
				return false;

			if ( shopName.equals( "Hippy Store (Hippy)" ) )
				if ( !StaticEntity.getProperty( "currentHippyStore" ).equals( "hippy" ) )
					return false;

			if ( shopName.equals( "Hippy Store (Fratboy)" ) )
				if ( !StaticEntity.getProperty( "currentHippyStore" ).equals( "fratboy" ) )
					return false;

			return QuestLogRequest.finishedQuest( QuestLogRequest.ISLAND_WAR ) || EquipmentDatabase.hasOutfit( 32 );
		}

		// Check the quest log when determining whether the person has
		// access to the Citadel.

		else if ( storeId.equals( "w" ) )
			return QuestLogRequest.finishedQuest( "White Citadel" );

		// If it gets this far, then the item is definitely available
		// for purchase from the NPC store.

		return true;
	}

	public static final boolean contains( String itemName )
	{	return contains( itemName, true );
	}

	public static final int price( String itemName )
	{
		MallPurchaseRequest request =  getPurchaseRequest( itemName );
		return ( request == null ) ? 0 : request.getPrice();
	}

	public static final boolean contains( String itemName, boolean validate )
	{
		MallPurchaseRequest item = getPurchaseRequest( itemName );
		return item != null && (!validate || item.canPurchase());
	}
}
