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

public class NPCStoreDatabase
	extends KoLDatabase
{
	private static final ArrayList NPC_ITEMS = new ArrayList();
	private static final AdventureResult LAB_KEY = new AdventureResult( 339, 1 );

	static
	{
		BufferedReader reader = KoLDatabase.getVersionedReader( "npcstores.txt", KoLConstants.NPCSTORES_VERSION );

		String[] data;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length == 4 )
			{
				NPCStoreDatabase.NPC_ITEMS.add( new MallPurchaseRequest(
					data[ 0 ], data[ 1 ], TradeableItemDatabase.getItemId( data[ 2 ] ),
					StaticEntity.parseInt( data[ 3 ] ) ) );
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

			StaticEntity.printStackTrace( e );
		}
	}

	public static final MallPurchaseRequest getPurchaseRequest( final String itemName )
	{
		int itemId = TradeableItemDatabase.getItemId( itemName );

		MallPurchaseRequest foundItem = null;
		MallPurchaseRequest currentItem = null;

		for ( int i = 0; i < NPCStoreDatabase.NPC_ITEMS.size(); ++i )
		{
			currentItem = (MallPurchaseRequest) NPCStoreDatabase.NPC_ITEMS.get( i );
			if ( currentItem.getItemId() != itemId )
			{
				continue;
			}

			foundItem = currentItem;
			if ( !NPCStoreDatabase.canPurchase( foundItem.getStoreId(), foundItem.getShopName() ) )
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

	private static final boolean canPurchase( final String storeId, final String shopName )
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
			return KoLCharacter.isMoxieClass();
		}
		else if ( storeId.equals( "2" ) )
		{
			return KoLCharacter.isMysticalityClass() || classType.equals( KoLCharacter.ACCORDION_THIEF ) && KoLCharacter.getLevel() >= 9;
		}
		else if ( storeId.equals( "3" ) )
		{
			return KoLCharacter.isMuscleClass() || classType.equals( KoLCharacter.ACCORDION_THIEF ) && KoLCharacter.getLevel() >= 9;
		}
		else if ( storeId.equals( "b" ) )
		{
			return EquipmentDatabase.hasOutfit( 1 );
		}
		else if ( storeId.equals( "r" ) )
		{
			return EquipmentDatabase.hasOutfit( 9 );
		}
		else if ( storeId.equals( "4" ) || storeId.equals( "5" ) )
		{
			return KoLCharacter.inMuscleSign();
		}
		else if ( storeId.equals( "j" ) )
		{
			return KoLCharacter.inMysticalitySign();
		}
		else if ( storeId.equals( "g" ) )
		{
			return KoLCharacter.hasItem( NPCStoreDatabase.LAB_KEY ) && EquipmentDatabase.hasOutfit( 5 );
		}
		else if ( storeId.equals( "h" ) )
		{
			int level = KoLCharacter.getLevel();

			if ( shopName.equals( "Hippy Store (Pre-War)" ) )
			{
				if ( !KoLConstants.inventory.contains( KoLAdventure.DINGHY ) || !EquipmentDatabase.hasOutfit( 2 ) )
				{
					return false;
				}

				if ( level < 12 )
				{
					return true;
				}

				if ( KoLSettings.getIntegerProperty( "lastFilthClearance" ) == KoLCharacter.getAscensions() )
				{
					return false;
				}

				return QuestLogRequest.isHippyStoreAvailable();
			}

			// Here, you insert any logic which is able to detect the
			// completion of the filthworm infestation and which outfit
			// was used to complete it.

			if ( level < 12 || KoLSettings.getIntegerProperty( "lastFilthClearance" ) != KoLCharacter.getAscensions() )
			{
				return false;
			}

			if ( shopName.equals( "Hippy Store (Hippy)" ) )
			{
				if ( !KoLSettings.getUserProperty( "currentHippyStore" ).equals( "hippy" ) )
				{
					return false;
				}
			}

			if ( shopName.equals( "Hippy Store (Fratboy)" ) )
			{
				if ( !KoLSettings.getUserProperty( "currentHippyStore" ).equals( "fratboy" ) )
				{
					return false;
				}
			}

			return QuestLogRequest.isHippyStoreAvailable() || EquipmentDatabase.hasOutfit( 32 );
		}

		// Check the quest log when determining if you've used the
		// black market map.

		else if ( storeId.equals( "l" ) )
		{
			return QuestLogRequest.isBlackMarketAvailable();
		}
		else if ( storeId.equals( "n" ) )
		{
			return KoLCharacter.inMoxieSign();
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
		MallPurchaseRequest request = NPCStoreDatabase.getPurchaseRequest( itemName );
		return request == null ? 0 : request.getPrice();
	}

	public static final boolean contains( final String itemName, boolean validate )
	{
		MallPurchaseRequest item = NPCStoreDatabase.getPurchaseRequest( itemName );
		return item != null && ( !validate || item.canPurchase() );
	}
}
