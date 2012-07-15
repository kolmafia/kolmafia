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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;

import net.sourceforge.kolmafia.request.CoinMasterPurchaseRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;

import net.sourceforge.kolmafia.swingui.StoreManageFrame;

import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class StoreManager
{
	private static final Pattern LOGSPAN_PATTERN = Pattern.compile( "<span.*?</span>" );
	private static final Pattern ADDER_PATTERN =
		Pattern.compile( "<tr><td><img src.*?></td><td>(.*?)( *\\((\\d*)\\))?</td><td>([\\d,]+)</td><td>(.*?)</td><td.*?(\\d+)" );
	private static final Pattern PRICER_PATTERN =
		Pattern.compile( "<tr><td><b>(?!Item:)(.*?)&nbsp;.*?<td>([\\d,]+)</td>.*?\"(\\d+)\" name=price\\[(\\d+).*?value=\"(\\d+)\".*?<td>([\\d,]+)</td>" );

	private static final int RECENT_FIRST = 1;
	private static final int OLDEST_FIRST = 2;
	private static final int GROUP_BY_NAME = 3;

	private static int currentLogSort = StoreManager.RECENT_FIRST;
	private static boolean sortItemsByName = false;
	private static long potentialEarnings = 0;

	private static final LockableListModel storeLog = new LockableListModel();
	private static final LockableListModel soldItemList = new LockableListModel();
	private static final LockableListModel sortedSoldItemList = new LockableListModel();

	private static final IntegerArray mallPrices = new IntegerArray();
	private static final LinkedHashMap<Integer, ArrayList<PurchaseRequest>> mallSearches = new LinkedHashMap<Integer, ArrayList<PurchaseRequest>>();
	private static final PauseObject pauser = new PauseObject();

	public static boolean soldItemsRetrieved = false;

	public static final void clearCache()
	{
		StoreManager.soldItemsRetrieved = false;
		StoreManager.storeLog.clear();
		StoreManageFrame.cancelTableEditing();
		StoreManager.soldItemList.clear();
		StoreManager.sortedSoldItemList.clear();
		StoreManager.potentialEarnings = 0;
	}

	public static final long getPotentialEarnings()
	{
		return StoreManager.potentialEarnings;
	}

	/**
	 * Registers an item inside of the store manager. Note that this includes the price of the item and the limit which
	 * is used to sell the item.
	 */

	public static final SoldItem registerItem( final int itemId, final int quantity, final int price, final int limit,
		final int lowest )
	{
		if ( price < 50000000 )
		{
			StoreManager.potentialEarnings += (long) price * (long) quantity;
		}

		SoldItem newItem = new SoldItem( itemId, quantity, price, limit, lowest );
		int itemIndex = StoreManager.soldItemList.indexOf( newItem );

		// If the item is brand-new, just return it

		if ( itemIndex == -1 )
		{
			return newItem;
		}

		// If the item already exists, check it against the one which
		// already exists in the list.	If there are any changes,
		// update.

		SoldItem oldItem = (SoldItem) StoreManager.soldItemList.get( itemIndex );

		if ( oldItem.getQuantity() != quantity ||
		     oldItem.getPrice() != price ||
		     oldItem.getLimit() != limit ||
		     lowest != 0 && oldItem.getLowest() != lowest )
		{
			return newItem;
		}

		return oldItem;
	}

	/**
	 * Returns the current price of the item with the given item Id. This is useful for auto-adding at the existing
	 * price.
	 */

	public static final int getPrice( final int itemId )
	{
		int currentPrice = 999999999;
		for ( int i = 0; i < StoreManager.soldItemList.size(); ++i )
		{
			if ( ( (SoldItem) StoreManager.soldItemList.get( i ) ).getItemId() == itemId )
			{
				currentPrice = ( (SoldItem) StoreManager.soldItemList.get( i ) ).getPrice();
			}
		}

		return currentPrice;
	}

	public static final LockableListModel getSoldItemList()
	{
		return StoreManager.soldItemList;
	}

	public static final LockableListModel getSortedSoldItemList()
	{
		return StoreManager.sortedSoldItemList;
	}

	public static final LockableListModel getStoreLog()
	{
		return StoreManager.storeLog;
	}

	public static final void sortStoreLog( final boolean cycleSortType )
	{
		if ( cycleSortType )
		{
			switch ( StoreManager.currentLogSort )
			{
			case RECENT_FIRST:
				StoreManager.currentLogSort = StoreManager.OLDEST_FIRST;
				break;
			case OLDEST_FIRST:
				StoreManager.currentLogSort = StoreManager.GROUP_BY_NAME;
				break;
			case GROUP_BY_NAME:
				StoreManager.currentLogSort = StoreManager.RECENT_FIRST;
				break;
			}
		}

		// Because StoreLogEntry objects use the current
		// internal variable to decide how to sort, a simple
		// function call will suffice.

		StoreManager.storeLog.sort();
	}

	public static final void update( final String storeText, final boolean isPriceManagement )
	{
		StoreManager.potentialEarnings = 0;
		ArrayList<SoldItem> newItems = new ArrayList<SoldItem>();

		if ( isPriceManagement )
		{
			int itemId, quantity, price, limit, lowest;

			// The item matcher here examines each row in the table
			// displayed in the price management page.

			Matcher priceMatcher = StoreManager.PRICER_PATTERN.matcher( storeText );

			while ( priceMatcher.find() )
			{
				itemId = StringUtilities.parseInt( priceMatcher.group( 4 ) );
				if ( ItemDatabase.getItemName( itemId ) == null )
				{
					// Do not register new items discovered in your store,
					// since the descid is not available
					//
					// ItemDatabase.registerItem( itemId, priceMatcher.group( 1 ) );
					continue;
				}

				quantity = StringUtilities.parseInt( priceMatcher.group( 2 ) );

				price = StringUtilities.parseInt( priceMatcher.group( 3 ) );
				limit = StringUtilities.parseInt( priceMatcher.group( 5 ) );
				lowest = StringUtilities.parseInt( priceMatcher.group( 6 ) );

				// Now that all the data has been retrieved, register
				// the item that was discovered.

				newItems.add( StoreManager.registerItem( itemId, quantity, price, limit, lowest ) );
			}
		}
		else
		{
			AdventureResult item;
			int itemId, price, limit;

			// The item matcher here examines each row in the table
			// displayed in the standard item-addition page.

			Matcher itemMatcher = StoreManager.ADDER_PATTERN.matcher( storeText );

			while ( itemMatcher.find() )
			{
				itemId = StringUtilities.parseInt( itemMatcher.group( 6 ) );
				if ( ItemDatabase.getItemName( itemId ) == null )
				{
					// Do not register new items discovered in your store,
					// since the descid is not available
					//
					// ItemDatabase.registerItem( itemId, itemMatcher.group( 1 ) );
					continue;
				}

				int count = itemMatcher.group(2) == null ? 1 : StringUtilities.parseInt( itemMatcher.group(3) );

				// Register using item ID, since the name might have changed
				item = new AdventureResult( itemId, count );
				price = StringUtilities.parseInt( itemMatcher.group( 4 ) );

				// In this case, the limit could appear as
				// "unlimited", which equates to a limit of 0.

				limit = itemMatcher.group( 5 ).startsWith( "<" ) ? 0 : StringUtilities.parseInt( itemMatcher.group( 5 ) );

				// Now that all the data has been retrieved,
				// register the item that was discovered.

				newItems.add( StoreManager.registerItem( item.getItemId(), item.getCount(), price, limit, 0 ) );
			}
		}

		StoreManageFrame.cancelTableEditing();

		StoreManager.sortItemsByName = true;
		Collections.sort( newItems );
		StoreManager.soldItemList.clear();
		StoreManager.soldItemList.addAll( newItems );

		StoreManager.sortItemsByName = false;
		Collections.sort( newItems );
		StoreManager.sortedSoldItemList.clear();
		StoreManager.sortedSoldItemList.addAll( newItems );

		StoreManager.soldItemsRetrieved = true;

		// Now, update the title of the store manage
		// frame to reflect the new price.

		StoreManageFrame.updateEarnings( StoreManager.potentialEarnings );
	}

	public static final void parseLog( final String logText )
	{
		StoreManager.storeLog.clear();
		ArrayList<Comparable<StoreLogEntry>> currentLog = new ArrayList<Comparable<StoreLogEntry>>();

		Matcher logMatcher = StoreManager.LOGSPAN_PATTERN.matcher( logText );
		if ( logMatcher.find() )
		{
			if ( logMatcher.group().indexOf( "<br>" ) == -1 )
			{
				return;
			}

			String entryString;
			StoreLogEntry entry;
			String[] entries = logMatcher.group().split( "<br>" );

			for ( int i = 0; i < entries.length - 1; ++i )
			{
				entryString = KoLConstants.ANYTAG_PATTERN.matcher( entries[ i ] ).replaceAll( "" );
				entry = new StoreLogEntry( entries.length - i - 1, entryString );
				currentLog.add( entry );
			}

			StoreManager.storeLog.addAll( currentLog );
			StoreManager.sortStoreLog( false );
		}
	}

	private static class StoreLogEntry
		implements Comparable<StoreLogEntry>
	{
		private final int id;
		private final String text;
		private final String stringForm;

		public StoreLogEntry( final int id, final String text )
		{
			this.id = id;

			String[] pieces = text.split( " " );
			this.text = text.substring( pieces[ 0 ].length() + pieces[ 1 ].length() + 2 );
			this.stringForm = id + ": " + text;
		}

		@Override
		public String toString()
		{
			return this.stringForm;
		}

		public int compareTo( final StoreLogEntry o )
		{
			if ( o == null  )
			{
				return -1;
			}

			switch ( StoreManager.currentLogSort )
			{
			case RECENT_FIRST:
				return o.id - this.id;
			case OLDEST_FIRST:
				return this.id - o.id;
			case GROUP_BY_NAME:
				return this.text.compareToIgnoreCase( o.text );
			default:
				return -1;
			}
		}
	}

	public static final void flushCache()
	{
		long t0, t1;
		t1 = System.currentTimeMillis();
		t0 = t1 - 15 * 1000;

		Iterator<ArrayList<PurchaseRequest>> i = StoreManager.mallSearches.values().iterator();
		while ( i.hasNext() )
		{
			ArrayList<PurchaseRequest> search = i.next();
			if ( search == null || search.size() == 0 )
			{
				i.remove();
				continue;
			}
			long t = search.get( 0 ).getTimestamp();
			if ( t < t0 || t > t1 )
			{
				i.remove();
				continue;
			}
			break;
		}
	}

	/**
	 * Utility method used to search the mall for a specific item.
	 */

	public static final ArrayList<PurchaseRequest> searchMall( final AdventureResult item )
	{
		int itemId = item.getItemId();
		if ( itemId <= 0 )
		{
			// This should not happen.
			return new ArrayList<PurchaseRequest>();
		}

		Integer id = IntegerPool.get( itemId );
		String name = ItemDatabase.getItemDataName( id );

		StoreManager.flushCache();

		ArrayList<PurchaseRequest> results = StoreManager.mallSearches.get( id );
		if ( results != null && results.size() > 0 )
		{
			KoLmafia.updateDisplay( "Using cached search results for " + name + "..." );
			return results;
		}

		results = new ArrayList<PurchaseRequest>();

		StoreManager.searchMall( "\"" + name + "\"", results, 10, false );

		// Flush CoinMasterPurchaseRequests
		Iterator<PurchaseRequest> it = results.iterator();
		while ( it.hasNext() )
		{
			if ( it.next() instanceof CoinMasterPurchaseRequest )
			{
				it.remove();
			}
		}

		if ( KoLmafia.permitsContinue() )
		{
			StoreManager.mallSearches.put( id, results );
		}

		return results;
	}

	/**
	 * Utility method used to search the mall for a search string
	 */

	public static final void searchMall( final String searchString, final List resultSummary, final int maximumResults, boolean toString )
	{
		resultSummary.clear();

		if ( searchString == null )
		{
			return;
		}

		ArrayList results = new ArrayList();

		// With the search string properly formatted, issue
		// the search request.

		RequestThread.postRequest( new MallSearchRequest(
			MallSearchRequest.getSearchString( searchString ),
			maximumResults, results, true ) );

		if ( !toString )
		{
			resultSummary.addAll( results );
			return;
		}

		PurchaseRequest[] resultsArray = new PurchaseRequest[ results.size() ];
		results.toArray( resultsArray );

		TreeMap<Integer, Integer> prices = new TreeMap<Integer, Integer>();
		Integer currentQuantity, currentPrice;

		for ( int i = 0; i < resultsArray.length; ++i )
		{
			currentPrice = IntegerPool.get( resultsArray[ i ].getPrice() );
			currentQuantity = prices.get( currentPrice );

			if ( currentQuantity == null )
			{
				prices.put( currentPrice, IntegerPool.get( resultsArray[ i ].getLimit() ) );
			}
			else
			{
				prices.put( currentPrice, IntegerPool.get( currentQuantity.intValue() + resultsArray[ i ].getLimit() ) );
			}
		}

		Integer[] priceArray = new Integer[ prices.size() ];
		prices.keySet().toArray( priceArray );

		for ( int i = 0; i < priceArray.length; ++i )
		{
			resultSummary.add( "  " + KoLConstants.COMMA_FORMAT.format( prices.get( priceArray[ i ] ).intValue() ) + " @ " + KoLConstants.COMMA_FORMAT.format( priceArray[ i ].intValue() ) + " meat" );
		}
	}

	public static final void updateMallPrice( final AdventureResult item, final ArrayList results )
	{
		if ( item.getItemId() < 1 )
		{
			return;
		}
		int price = -1;
		int qty = 5;
		Iterator i = results.iterator();
		while ( i.hasNext() )
		{
			PurchaseRequest req = (PurchaseRequest) i.next();
			if ( !req.canPurchaseIgnoringMeat() ) continue;
			price = req.getPrice();
			qty -= req.getLimit();
			if ( qty <= 0 )
			{
				break;
			}
		}
		StoreManager.mallPrices.set( item.getItemId(), price );
		if ( price > 0 )
		{
			MallPriceDatabase.recordPrice( item.getItemId(), price );
		}
	}

	public static final synchronized int getMallPrice( final AdventureResult item )
	{
		StoreManager.flushCache();
		if ( item.getItemId() < 1 ||
		     ( !ItemDatabase.isTradeable( item.getItemId() ) && !NPCStoreDatabase.contains( item.getName(), true ) ) )
		{
			return 0;
		}
		if ( StoreManager.mallPrices.get( item.getItemId() ) == 0 )
		{
			ArrayList results = StoreManager.searchMall( item );
			StoreManager.updateMallPrice( item, results );
		}
		return StoreManager.mallPrices.get( item.getItemId() );
	}

	public static int getMallPrice( AdventureResult item, float maxAge )
	{
		int id = item.getItemId();
		int price = MallPriceDatabase.getPrice( id );
		if ( price <= 0 || MallPriceDatabase.getAge( id ) > maxAge )
		{
			price = StoreManager.getMallPrice( item );
		}
		return price;
	}

	/**
	 * Internal immutable class used to hold a single instance of an item sold in a player's store.
	 */

	public static class SoldItem
		extends Vector<Object>
		implements Comparable<Object>
	{
		private final int itemId;
		private final String itemName;
		private final int quantity;
		private final int price;
		private final int limit;
		private final int lowest;

		public SoldItem( final int itemId, final int quantity, final int price, final int limit, final int lowest )
		{
			this.itemId = itemId;
			this.itemName = ItemDatabase.getItemDataName( itemId );
			this.quantity = quantity;
			this.price = price;
			this.limit = limit;
			this.lowest = lowest;

			super.add( this.itemName );
			super.add( IntegerPool.get( price ) );
			super.add( IntegerPool.get( lowest ) );
			super.add( IntegerPool.get( quantity ) );
			super.add( IntegerPool.get( limit ) );
		}

		public int getItemId()
		{
			return this.itemId;
		}

		public String getItemName()
		{
			return this.itemName;
		}

		public int getQuantity()
		{
			return this.quantity;
		}

		public int getPrice()
		{
			return this.price;
		}

		public int getLimit()
		{
			return this.limit;
		}

		public int getLowest()
		{
			return this.lowest;
		}

		@Override
		public synchronized boolean equals( final Object o )
		{
			return o != null && o instanceof SoldItem && ( (SoldItem) o ).itemId == this.itemId;
		}

		public int compareTo( final Object o )
		{
			if ( o == null || !( o instanceof SoldItem ) )
			{
				return -1;
			}

			if ( this.price != 999999999 && ( (SoldItem) o ).price == 999999999 )
			{
				return -1;
			}

			if ( this.price == 999999999 && ( (SoldItem) o ).price != 999999999 )
			{
				return 1;
			}

			if ( this.price == 999999999 && ( (SoldItem) o ).price == 999999999 )
			{
				return this.itemName.compareToIgnoreCase( ( (SoldItem) o ).itemName );
			}

			return StoreManager.sortItemsByName ? this.itemName.compareToIgnoreCase( ( (SoldItem) o ).itemName ) : this.price - ( (SoldItem) o ).price;
		}

		@Override
		public synchronized String toString()
		{
			StringBuilder buffer = new StringBuilder();

			buffer.append( ItemDatabase.getItemName( this.itemId ) );
			buffer.append( " (" );

			buffer.append( KoLConstants.COMMA_FORMAT.format( this.quantity ) );

			if ( this.limit < this.quantity )
			{
				buffer.append( " limit " );
				buffer.append( KoLConstants.COMMA_FORMAT.format( this.limit ) );
			}

			buffer.append( " @ " );
			buffer.append( KoLConstants.COMMA_FORMAT.format( this.price ) );
			buffer.append( ")" );

			return buffer.toString();

		}
	}

	public static void priceItemsAtLowestPrice( boolean avoidMinPrice )
	{
		RequestThread.postRequest( new ManageStoreRequest() );

		SoldItem[] sold = new SoldItem[ StoreManager.soldItemList.size() ];
		StoreManager.soldItemList.toArray( sold );

		int[] itemId = new int[ sold.length ];
		int[] prices = new int[ sold.length ];
		int[] limits = new int[ sold.length ];

		// Now determine the desired prices on items.

		for ( int i = 0; i < sold.length; ++i )
		{
			itemId[ i ] = sold[ i ].getItemId();
			limits[ i ] = sold[ i ].getLimit();

			int minimumPrice = Math.max( 100, Math.abs( ItemDatabase.getPriceById( sold[ i ].getItemId() ) ) * 2 );
			int desiredPrice = Math.max( minimumPrice, sold[ i ].getLowest() - sold[ i ].getLowest() % 100 );

			if ( sold[ i ].getPrice() == 999999999 && ( !avoidMinPrice || desiredPrice > minimumPrice ) )
			{
				prices[ i ] = desiredPrice;
			}
			else
			{
				prices[ i ] = sold[ i ].getPrice();
			}
		}

		RequestThread.postRequest( new ManageStoreRequest( itemId, prices, limits ) );
		KoLmafia.updateDisplay( "Repricing complete." );
	}
}
