/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.CoinMasterPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmastersDatabase
	extends KoLDatabase
{
	// Map from Integer( itemId ) -> CoinMasterPurchaseRequest
	public static final HashMap<Integer, CoinMasterPurchaseRequest> COINMASTER_ITEMS = new HashMap<Integer, CoinMasterPurchaseRequest>();

	// Map from String -> LockableListModel
	public static final TreeMap<String,LockableListModel<AdventureResult>> items = new TreeMap();

	// Map from String -> LockableListModel
	public static final TreeMap<String,LockableListModel<AdventureResult>> buyItems = new TreeMap();

	// Map from String -> Map from String -> Integer
	public static final TreeMap<String, Map<String, Integer>> buyPrices = new TreeMap<String, Map<String, Integer>>();

	// Map from String -> Map from String -> Integer
	public static final TreeMap<String, Map<String, Integer>> sellPrices = new TreeMap<String, Map<String, Integer>>();

	// Map from String -> Map from String -> Integer
	public static final TreeMap<String, Map<String, Integer>> itemRows = new TreeMap<String, Map<String, Integer>>();

	public static final LockableListModel<AdventureResult> getItems( final String key )
	{
		return CoinmastersDatabase.items.get( key );
	}

	public static final LockableListModel<AdventureResult> getBuyItems( final String key )
	{
		return CoinmastersDatabase.buyItems.get( key );
	}

	public static final Map<String, Integer> getBuyPrices( final String key )
	{
		return CoinmastersDatabase.buyPrices.get( key );
	}

	public static final Map<String, Integer> getSellPrices( final String key )
	{
		return CoinmastersDatabase.sellPrices.get( key );
	}

	public static final Map<String, Integer> getRows( final String key )
	{
		return CoinmastersDatabase.itemRows.get( key );
	}

	public static final LockableListModel<AdventureResult> getNewList()
	{
		return new LockableListModel<AdventureResult>();
	}

	public static final Map<String, Integer> getNewMap()
	{
		return new TreeMap<String, Integer>();
	}

	private static final LockableListModel<AdventureResult> getOrMakeList( final String key, final Map<String,LockableListModel<AdventureResult>> map )
	{
		LockableListModel<AdventureResult> retval = map.get( key );
		if ( retval == null )
		{
			retval = CoinmastersDatabase.getNewList();
			map.put( key, retval );
		}
		return retval;
	}

	private static final Map<String, Integer> getOrMakeMap( final String key, final Map<String, Map<String, Integer>> map )
	{
		Map<String, Integer> retval = (Map<String, Integer>) map.get( key );
		if ( retval == null )
		{
			retval = CoinmastersDatabase.getNewMap();
			map.put( key, retval );
		}
		return retval;
	}

	public static final Map<Integer, String> invert( final Map<String, Integer> map )
	{
		Map<Integer, String> retval = new TreeMap<Integer, String>();
		for ( Entry<String, Integer> entry : map.entrySet() )
		{
			retval.put(entry.getValue(), entry.getKey());
		}
		return retval;
	}

	static
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "coinmasters.txt", KoLConstants.COINMASTERS_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 2 )
			{
				continue;
			}

			String master = data[ 0 ];

			// Special categories
			if ( data.length == 2 )
			{
				String rname = data[ 1 ];
				String name = StringUtilities.getCanonicalName( rname );
				AdventureResult item = new AdventureResult( name, PurchaseRequest.MAX_QUANTITY, false );
				LockableListModel<AdventureResult> list = CoinmastersDatabase.getOrMakeList( master, CoinmastersDatabase.items );
				list.add( item );
				continue;
			}

			if ( data.length < 4 )
			{
				continue;
			}

			String type = data[ 1 ];
			int price = StringUtilities.parseInt( data[ 2 ] );
			Integer iprice = IntegerPool.get( price );
			String rname = data[ 3 ];
			String name = StringUtilities.getCanonicalName( rname );
			AdventureResult item = new AdventureResult( name, PurchaseRequest.MAX_QUANTITY, false );

			if ( type.equals( "buy" ) )
			{
				LockableListModel<AdventureResult> list = CoinmastersDatabase.getOrMakeList( master, CoinmastersDatabase.buyItems );
				Map map = CoinmastersDatabase.getOrMakeMap( master, CoinmastersDatabase.buyPrices );
				list.add( item );
				map.put( name, iprice );
			}
			else if ( type.equals( "sell" ) )
			{
				Map<String, Integer> map = CoinmastersDatabase.getOrMakeMap( master, CoinmastersDatabase.sellPrices );
				map.put( name, iprice );
			}
			
			Integer row = null;
			if ( data.length > 4 )
			{
				if ( data[ 4 ].startsWith( "ROW" ) )
				{
					row = IntegerPool.get( StringUtilities.parseInt( data[ 4 ].substring( 3 ) ) );
					Map<String, Integer> rowMap = CoinmastersDatabase.getOrMakeMap( master, CoinmastersDatabase.itemRows );
					rowMap.put( name, row );
				}
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

	public static final int getPrice( final String name, final Map prices )
	{
		if ( name == null )
		{
			return 0;
		}
		Integer price = (Integer) prices.get( StringUtilities.getCanonicalName( name ) );
		return ( price == null ) ? 0 : price.intValue();
	}

	public static final boolean availableItem( final String name )
	{
		if ( name.equals( "a crimbo carol, ch. 1" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.SEAL_CLUBBER );
		}
		if ( name.equals( "a crimbo carol, ch. 2" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.TURTLE_TAMER );
		}
		if ( name.equals( "a crimbo carol, ch. 3" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.PASTAMANCER );
		}
		if ( name.equals( "a crimbo carol, ch. 4" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR );
		}
		if ( name.equals( "a crimbo carol, ch. 5" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.DISCO_BANDIT );
		}
		if ( name.equals( "a crimbo carol, ch. 6" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.ACCORDION_THIEF );
		}
		if ( name.equals( "a. w. o. l. tattoo #1" ) )
		{
			return KoLCharacter.AWOLtattoo == 1;
		}
		if ( name.equals( "a. w. o. l. tattoo #2" ) )
		{
			return KoLCharacter.AWOLtattoo == 2;
		}
		if ( name.equals( "a. w. o. l. tattoo #3" ) )
		{
			return KoLCharacter.AWOLtattoo == 3;
		}
		if ( name.equals( "a. w. o. l. tattoo #4" ) )
		{
			return KoLCharacter.AWOLtattoo == 4;
		}
		if ( name.equals( "a. w. o. l. tattoo #5" ) )
		{
			return KoLCharacter.AWOLtattoo == 5;
		}

		return true;
	}

	public static final void clearPurchaseRequests( CoinmasterData data )
	{
		// Clear all purchase requests for a particular Coin Master
		Iterator it = CoinmastersDatabase.COINMASTER_ITEMS.values().iterator();
		while ( it.hasNext() )
		{
			CoinMasterPurchaseRequest request = (CoinMasterPurchaseRequest) it.next();
			if ( request.getData() == data )
			{
				it.remove();
			}
		}
	}

	public static final void registerPurchaseRequest( CoinmasterData data, int itemId, int price, int quantity )
	{
		// Register a purchase request
		CoinMasterPurchaseRequest request = new CoinMasterPurchaseRequest( data, itemId, price, quantity );
		CoinmastersDatabase.COINMASTER_ITEMS.put( IntegerPool.get( itemId ), request );

		// Register this in the Concoction for the item

		// Special case: ten-leaf-clovers are limited
		if ( itemId == ItemPool.TEN_LEAF_CLOVER )
		{
			return;
		}

		Concoction concoction = ConcoctionPool.get( itemId );
		if ( concoction == null )
		{
			return;
		}

		// If we can create it any other way, prefer that method
		if ( concoction.getMixingMethod() == CraftingType.NOCREATE )
		{
			concoction.setMixingMethod( CraftingType.COINMASTER );
			concoction.addIngredient( request.getCost() );
		}

		// If we can create this only via a coin master trade, save request
		if ( concoction.getMixingMethod() == CraftingType.COINMASTER )
		{
			concoction.setPurchaseRequest( request );
		}
	}

	public static final CoinMasterPurchaseRequest getPurchaseRequest( final String itemName )
	{
		Integer id = IntegerPool.get( ItemDatabase.getItemId( itemName, 1, false ) );
		CoinMasterPurchaseRequest request =  (CoinMasterPurchaseRequest) CoinmastersDatabase.COINMASTER_ITEMS.get(  id );

		if ( request == null )
		{
			return null;
		}

		request.setLimit( request.affordableCount() );
		request.setCanPurchase();

		return request;
	}

	public static final boolean contains( final String itemName )
	{
		return CoinmastersDatabase.contains( itemName, true );
	}

	public static final int price( final String itemName )
	{
		PurchaseRequest request = CoinmastersDatabase.getPurchaseRequest( itemName );
		return request == null ? 0 : request.getPrice();
	}

	public static final boolean contains( final String itemName, boolean validate )
	{
		PurchaseRequest item = CoinmastersDatabase.getPurchaseRequest( itemName );
		return item != null && ( !validate || item.canPurchase() );
	}
}
