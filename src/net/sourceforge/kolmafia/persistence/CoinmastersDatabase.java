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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
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
{
	// Map from Integer( itemId ) -> CoinMasterPurchaseRequest
	public static final Map<Integer, CoinMasterPurchaseRequest> COINMASTER_ITEMS = new HashMap<>();

	// Map from String -> LockableListModel
	public static final Map<String,LockableListModel<AdventureResult>> items = new TreeMap<>();

	// Map from String -> LockableListModel
	public static final Map<String,LockableListModel<AdventureResult>> buyItems = new TreeMap<>();

	// Map from String -> Map from Integer -> Integer
	public static final Map<String, Map<Integer, Integer>> buyPrices = new TreeMap<>();

	// Map from String -> LockableListModel
	public static final Map<String,LockableListModel<AdventureResult>> sellItems = new TreeMap<>();

	// Map from String -> Map from Integer -> Integer
	public static final Map<String, Map<Integer, Integer>> sellPrices = new TreeMap<>();

	// Map from String -> Map from Integer -> Integer
	public static final Map<String, Map<Integer, Integer>> itemRows = new TreeMap<>();

	public static final LockableListModel<AdventureResult> getItems( final String key )
	{
		return CoinmastersDatabase.items.get( key );
	}

	public static final LockableListModel<AdventureResult> getBuyItems( final String key )
	{
		return CoinmastersDatabase.buyItems.get( key );
	}

	public static final Map<Integer, Integer> getBuyPrices( final String key )
	{
		return CoinmastersDatabase.buyPrices.get( key );
	}

	public static final LockableListModel<AdventureResult> getSellItems( final String key )
	{
		return CoinmastersDatabase.sellItems.get( key );
	}

	public static final Map<Integer, Integer> getSellPrices( final String key )
	{
		return CoinmastersDatabase.sellPrices.get( key );
	}

	public static final Map<Integer, Integer> getRows( final String key )
	{
		return CoinmastersDatabase.itemRows.get( key );
	}

	public static final Map<Integer, Integer> getOrMakeRows( final String key )
	{
		return CoinmastersDatabase.getOrMakeMap( key, CoinmastersDatabase.itemRows );
	}

	public static final LockableListModel<AdventureResult> getNewList()
	{
		return new LockableListModel<AdventureResult>();
	}

	public static final Map<Integer, Integer> getNewMap()
	{
		return new TreeMap<Integer, Integer>();
	}

	private static LockableListModel<AdventureResult> getOrMakeList( final String key, final Map<String,LockableListModel<AdventureResult>> map )
	{
		LockableListModel<AdventureResult> retval = map.get( key );
		if ( retval == null )
		{
			retval = CoinmastersDatabase.getNewList();
			map.put( key, retval );
		}
		return retval;
	}

	private static Map<Integer, Integer> getOrMakeMap( final String key, final Map<String, Map<Integer, Integer>> map )
	{
		Map<Integer, Integer> retval = map.get( key );
		if ( retval == null )
		{
			retval = CoinmastersDatabase.getNewMap();
			map.put( key, retval );
		}
		return retval;
	}

	public static final Map<Integer, Integer> invert( final Map<Integer, Integer> map )
	{
		Map<Integer, Integer> retval = new TreeMap<>();
		for ( Entry<Integer, Integer> entry : map.entrySet() )
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
			if ( data.length < 4 )
			{
				continue;
			}

			String master = data[ 0 ];
			String type = data[ 1 ];
			int price = StringUtilities.parseInt( data[ 2 ] );
			Integer iprice = IntegerPool.get( price );
			AdventureResult item = AdventureResult.parseItem( data[ 3 ], true );
			Integer iitemId = IntegerPool.get( item.getItemId() );

			Integer row = null;
			if ( data.length > 4 )
			{
				String[] extra = data[ 4 ].split( "\\s,\\s" );
				for ( String extra1 : extra )
				{
					if ( extra1.startsWith( "ROW" ) )
					{
						row = IntegerPool.get( StringUtilities.parseInt( data[ 4 ].substring( 3 ) ) );
						Map<Integer, Integer> rowMap = CoinmastersDatabase.getOrMakeMap( master, CoinmastersDatabase.itemRows );
						rowMap.put( iitemId, row );
					}
				}
			}

			if ( type.equals( "buy" ) )
			{
				LockableListModel<AdventureResult> list = CoinmastersDatabase.getOrMakeList( master, CoinmastersDatabase.buyItems );
				list.add( item.getInstance( CoinmastersDatabase.purchaseLimit( iitemId ) ) );

				Map<Integer, Integer> map = CoinmastersDatabase.getOrMakeMap( master, CoinmastersDatabase.buyPrices );
				map.put( iitemId, iprice );
			}
			else if ( type.equals( "sell" ) )
			{
				LockableListModel<AdventureResult> list = CoinmastersDatabase.getOrMakeList( master, CoinmastersDatabase.sellItems );
				list.add( item );

				Map<Integer, Integer> map = CoinmastersDatabase.getOrMakeMap( master, CoinmastersDatabase.sellPrices );
				map.put( iitemId, iprice );
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

	private static int purchaseLimit( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.ZEPPELIN_TICKET:
		case ItemPool.TALES_OF_DREAD:
		case ItemPool.BRASS_DREAD_FLASK:
		case ItemPool.SILVER_DREAD_FLASK:
			return 1;
		}
		return PurchaseRequest.MAX_QUANTITY;
	}

	public static final int getPrice( final int itemId, final Map<Integer, Integer> prices )
	{
		if ( itemId == -1 )
		{
			return 0;
		}
		Integer price = prices.get( itemId );
		return ( price == null ) ? 0 : price.intValue();
	}

	public static final void clearPurchaseRequests( CoinmasterData data )
	{
		// Clear all purchase requests for a particular Coin Master
		Iterator<CoinMasterPurchaseRequest> it = CoinmastersDatabase.COINMASTER_ITEMS.values().iterator();
		while ( it.hasNext() )
		{
			CoinMasterPurchaseRequest request = it.next();
			if ( request.getData() == data )
			{
				it.remove();
			}
		}
	}

	public static final void registerPurchaseRequest( final CoinmasterData data, final AdventureResult item, final AdventureResult price )
	{
		int itemId = item.getItemId();
		int quantity = item.getCount();

		// Register a purchase request
		CoinMasterPurchaseRequest request = new CoinMasterPurchaseRequest( data, item, price );
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
			concoction.addIngredient( price );
		}

		// If we can create this only via a coin master trade, save request
		if ( concoction.getMixingMethod() == CraftingType.COINMASTER )
		{
			concoction.setPurchaseRequest( request );
		}
	}

	public static final CoinMasterPurchaseRequest getPurchaseRequest( final int itemId )
	{
		Integer id = IntegerPool.get( itemId );
		CoinMasterPurchaseRequest request = CoinmastersDatabase.COINMASTER_ITEMS.get( id );

		if ( request == null )
		{
			return null;
		}

		request.setLimit( request.affordableCount() );
		request.setCanPurchase();

		return request;
	}

	public static final boolean contains( final int itemId )
	{
		return CoinmastersDatabase.contains( itemId, true );
	}

	public static final boolean contains( final int itemId, boolean validate )
	{
		CoinMasterPurchaseRequest item = CoinmastersDatabase.getPurchaseRequest( itemId );
		return item != null && ( !validate || item.availableItem() );
	}
}
