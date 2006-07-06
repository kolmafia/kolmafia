/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.List;
import java.util.ArrayList;

/**
 * A static class which retrieves all the NPC stores currently
 * available to <code>KoLmafia</code>.
 */

public class NPCStoreDatabase extends KoLDatabase
{
	private static List [] storeTable;

	static
	{
		BufferedReader reader = getReader( "npcstores.dat" );

		storeTable = new ArrayList[5];
		for ( int i = 0; i < 5; ++i )
			storeTable[i] = new ArrayList();

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 4 )
			{
				for ( int i = 0; i < 4; ++i )
					storeTable[i].add( data[i] );

				storeTable[4].add( Integer.valueOf( data[2] ) );
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

	public static final MallPurchaseRequest getPurchaseRequest( String itemName )
	{
		Integer itemID = new Integer( TradeableItemDatabase.getItemID( itemName ) );
		int itemIndex = storeTable[4].indexOf( itemID );

		// If the item is not present in the NPC store table, then
		// the item is not available.

		if ( itemIndex == -1 )
			return null;

		// Check for whether or not the purchase can be made from a
		// guild store.  Store #1 is moxie classes, store #2 is for
		// mysticality classes, and store #3 is for muscle classes.

		String classType = KoLCharacter.getClassType();
		String storeID = (String) storeTable[0].get( itemIndex );
		MallPurchaseRequest itemRequest = new MallPurchaseRequest( client, (String) storeTable[1].get(itemIndex), storeID,
			StaticEntity.parseInt( (String) storeTable[2].get(itemIndex) ), StaticEntity.parseInt( (String) storeTable[3].get(itemIndex) ) );

		if ( storeID.equals( "1" ) )
		{
			itemRequest.setCanPurchase( classType.startsWith( "Di" ) || classType.startsWith( "Ac" ) );
		}

		else if ( storeID.equals( "2" ) )
		{
			itemRequest.setCanPurchase( classType.startsWith( "Pa" ) || classType.startsWith( "Sa" ) ||
				(classType.startsWith( "Ac" ) && KoLCharacter.getLevel() >= 9) );
		}

		else if ( storeID.equals( "3" ) )
		{
			itemRequest.setCanPurchase( classType.startsWith( "Se" ) || classType.startsWith( "Tu" ) ||
				(classType.startsWith( "Ac" ) && KoLCharacter.getLevel() >= 9) );
		}

		// If the person is trying to get one of the items from the bugbear
		// store, then the item is not available if they don't have the
		// bugbear outfit.  Of course, some items are available from the
		// Degrassi knoll bakery, so fallback on that when possible.

		else if ( storeID.equals( "b" ) )
		{
			if ( !EquipmentDatabase.hasOutfit( 1 ) )
			{
				itemIndex = storeTable[4].lastIndexOf( itemID );
				storeID = (String) storeTable[0].get( itemIndex );

				if ( storeID.equals( "b" ) )
				{
					itemRequest.setCanPurchase( false );
				}
				else
				{
					itemRequest = new MallPurchaseRequest( client, (String) storeTable[1].get(itemIndex), storeID,
						StaticEntity.parseInt( (String) storeTable[2].get(itemIndex) ), StaticEntity.parseInt( (String) storeTable[3].get(itemIndex) ) );

					itemRequest.setCanPurchase( KoLCharacter.inMuscleSign() );
				}
			}
		}

		// If the person is not in a muscle sign, then items from the
		// Degrassi Knoll are not available.

		else if ( (storeID.equals( "4" ) || storeID.equals( "5" )) )
			itemRequest.setCanPurchase( KoLCharacter.inMuscleSign() );

		// If the person is not in a mysticality sign, then items from the
		// Canadia Jewelers are not available.

		else if ( storeID.equals( "j" ) )
			itemRequest.setCanPurchase( KoLCharacter.inMysticalitySign() );

		// If the person is trying to get the items from the laboratory,
		// then the item is not available if they don't have the elite
		// guard uniform, and not available if out of Ronin.

		else if ( storeID.equals( "g" ) )
			itemRequest.setCanPurchase( EquipmentDatabase.hasOutfit( 5 ) );

		// If the person is trying to get one of the items from the hippy
		// store, then the item is not available if they don't have the
		// hippy outfit.

		else if ( storeID.equals( "h" ) )
			itemRequest.setCanPurchase( EquipmentDatabase.hasOutfit( 2 ) );

		// If it gets this far, then the item is definitely available
		// for purchase from the NPC store.

		return itemRequest;
	}

	public static final boolean contains( String itemName )
	{	return contains( itemName, true );
	}

	public static final boolean contains( String itemName, boolean validate )
	{
		MallPurchaseRequest itemRequest = getPurchaseRequest( itemName );
		return itemRequest == null ? false : validate ? itemRequest.canPurchase() : true;
	}
}
