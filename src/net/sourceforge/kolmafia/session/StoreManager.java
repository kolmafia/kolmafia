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

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.StoreManageFrame;

import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

public abstract class StoreManager
	extends StaticEntity
{
	private static final Pattern LOGSPAN_PATTERN = Pattern.compile( "<span.*?</span>" );
	private static final Pattern ADDER_PATTERN =
		Pattern.compile( "<tr><td><img src.*?></td><td>(.*?)</td><td>([\\d,]+)</td><td>(.*?)</td><td.*?(\\d+)" );
	private static final Pattern PRICER_PATTERN =
		Pattern.compile( "<tr><td><b>([^<]*?)\\&nbsp;.*?<td>([\\d,]+)</td>.*?\"(\\d+)\" name=price(\\d+).*?value=\"(\\d+)\".*?<td>([\\d,]+)</td>" );

	private static final int RECENT_FIRST = 1;
	private static final int OLDEST_FIRST = 2;
	private static final int GROUP_BY_NAME = 3;

	private static int currentLogSort = StoreManager.RECENT_FIRST;
	private static boolean sortItemsByName = false;
	private static long potentialEarnings = 0;

	private static final LockableListModel storeLog = new LockableListModel();
	private static final LockableListModel soldItemList = new LockableListModel();
	private static final LockableListModel sortedSoldItemList = new LockableListModel();

	public static final void clearCache()
	{
		StoreManager.potentialEarnings = 0;

		StoreManager.storeLog.clear();
		StoreManager.soldItemList.clear();
		StoreManager.sortedSoldItemList.clear();
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

		// If the item is brand-new, just add it to the
		// list of sold items.

		if ( itemIndex == -1 )
		{
			StoreManager.soldItemList.add( newItem );
			StoreManager.sortedSoldItemList.add( newItem );
		}
		else
		{
			// If the item already exists, check it against the
			// one which already exists in the list.  If there
			// are any changes, update.

			SoldItem oldItem = (SoldItem) StoreManager.soldItemList.get( itemIndex );

			if ( oldItem.getQuantity() != quantity || oldItem.getPrice() != price || oldItem.getLimit() != limit || lowest != 0 && oldItem.getLowest() != lowest )
			{
				StoreManager.soldItemList.set( itemIndex, newItem );
				StoreManager.sortedSoldItemList.set( StoreManager.sortedSoldItemList.indexOf( newItem ), newItem );
			}
		}

		return newItem;
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
		ArrayList newItems = new ArrayList();

		if ( isPriceManagement )
		{
			int itemId, quantity, price, limit, lowest;

			// The item matcher here examines each row in the table
			// displayed in the price management page.

			Matcher priceMatcher = StoreManager.PRICER_PATTERN.matcher( storeText );

			while ( priceMatcher.find() )
			{
				itemId = StaticEntity.parseInt( priceMatcher.group( 4 ) );
				if ( ItemDatabase.getItemName( itemId ) == null )
				{
					ItemDatabase.registerItem( itemId, priceMatcher.group( 1 ) );
				}

				quantity = StaticEntity.parseInt( priceMatcher.group( 2 ) );

				price = StaticEntity.parseInt( priceMatcher.group( 3 ) );
				limit = StaticEntity.parseInt( priceMatcher.group( 5 ) );
				lowest = StaticEntity.parseInt( priceMatcher.group( 6 ) );

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
				itemId = StaticEntity.parseInt( itemMatcher.group( 4 ) );
				if ( ItemDatabase.getItemName( itemId ) == null )
				{
					String itemName = itemMatcher.group( 1 );
					if ( itemName.indexOf( "(" ) != -1 )
					{
						itemName = itemName.substring( 0, itemName.indexOf( "(" ) ).trim();
					}

					ItemDatabase.registerItem( itemId, itemName );
				}

				// Remove parenthesized number and match again.
				StringTokenizer parsedItem = new StringTokenizer( itemMatcher.group( 1 ), "()" );
				String name = parsedItem.nextToken().trim();
				int count = 1;

				if ( parsedItem.hasMoreTokens() )
				{
					count = StaticEntity.parseInt( parsedItem.nextToken() );
				}

				item = new AdventureResult( name, count, false );
				price = StaticEntity.parseInt( itemMatcher.group( 2 ) );

				// In this case, the limit could appear as "unlimited",
				// which equates to a limit of 0.

				limit = itemMatcher.group( 3 ).startsWith( "<" ) ? 0 : StaticEntity.parseInt( itemMatcher.group( 3 ) );

				// Now that all the data has been retrieved, register
				// the item that was discovered.

				newItems.add( StoreManager.registerItem( item.getItemId(), item.getCount(), price, limit, 0 ) );
			}
		}

		StoreManager.soldItemList.retainAll( newItems );
		StoreManager.sortedSoldItemList.retainAll( newItems );

		StoreManager.sortItemsByName = true;
		StoreManager.soldItemList.sort();

		StoreManager.sortItemsByName = false;
		StoreManager.sortedSoldItemList.sort();

		// Now, update the title of the store manage
		// frame to reflect the new price.

		StoreManageFrame.updateEarnings( StoreManager.potentialEarnings );
	}

	public static final void parseLog( final String logText )
	{
		StoreManager.storeLog.clear();

		Matcher logMatcher = StoreManager.LOGSPAN_PATTERN.matcher( logText );
		if ( logMatcher.find() )
		{
			if ( logMatcher.group().indexOf( "<br>" ) == -1 )
			{
				return;
			}

			String[] entries = logMatcher.group().split( "<br>" );

			for ( int i = 0; i < entries.length - 1; ++i )
			{
				StoreManager.storeLog.add( new StoreLogEntry( entries.length - i - 1, entries[ i ].replaceAll(
					"<.*?>", "" ) ) );
			}

			StoreManager.sortStoreLog( false );
		}
	}

	private static class StoreLogEntry
		implements Comparable
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

		public String toString()
		{
			return this.stringForm;
		}

		public int compareTo( final Object o )
		{
			if ( o == null || !( o instanceof StoreLogEntry ) )
			{
				return -1;
			}

			switch ( StoreManager.currentLogSort )
			{
			case RECENT_FIRST:
				return ( (StoreLogEntry) o ).id - this.id;
			case OLDEST_FIRST:
				return this.id - ( (StoreLogEntry) o ).id;
			case GROUP_BY_NAME:
				return this.text.compareToIgnoreCase( ( (StoreLogEntry) o ).text );
			default:
				return -1;
			}
		}
	}

	public static final ArrayList searchMall( final String itemName )
	{
		ArrayList results = new ArrayList();
		StoreManager.searchMall( itemName, results, 10, false );
		return results;
	}

	/**
	 * Utility method used to search the mall for the given item.
	 */

	public static final void searchMall( final String itemName, final List resultSummary, final int maximumResults,
		boolean toString )
	{
		resultSummary.clear();
		if ( itemName == null )
		{
			return;
		}

		ArrayList results = new ArrayList();

		// With the item name properly formatted, issue
		// the search request.

		RequestThread.postRequest( new MallSearchRequest( itemName, maximumResults, results, true ) );

		if ( !toString )
		{
			resultSummary.addAll( results );
			return;
		}

		MallPurchaseRequest[] resultsArray = new MallPurchaseRequest[ results.size() ];
		results.toArray( resultsArray );

		TreeMap prices = new TreeMap();
		Integer currentQuantity, currentPrice;

		for ( int i = 0; i < resultsArray.length; ++i )
		{
			currentPrice = new Integer( resultsArray[ i ].getPrice() );
			currentQuantity = (Integer) prices.get( currentPrice );

			if ( currentQuantity == null )
			{
				prices.put( currentPrice, new Integer( resultsArray[ i ].getLimit() ) );
			}
			else
			{
				prices.put( currentPrice, new Integer( currentQuantity.intValue() + resultsArray[ i ].getLimit() ) );
			}
		}

		Integer[] priceArray = new Integer[ prices.size() ];
		prices.keySet().toArray( priceArray );

		for ( int i = 0; i < priceArray.length; ++i )
		{
			resultSummary.add( "  " + KoLConstants.COMMA_FORMAT.format( ( (Integer) prices.get( priceArray[ i ] ) ).intValue() ) + " @ " + KoLConstants.COMMA_FORMAT.format( priceArray[ i ].intValue() ) + " meat" );
		}
	}

	/**
	 * Internal immutable class used to hold a single instance of an item sold in a player's store.
	 */

	public static class SoldItem
		extends Vector
		implements Comparable
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
			this.itemName = ItemDatabase.getItemName( itemId );
			this.quantity = quantity;
			this.price = price;
			this.limit = limit;
			this.lowest = lowest;

			super.add( this.itemName );
			super.add( new Integer( price ) );
			super.add( new Integer( lowest ) );
			super.add( new Integer( quantity ) );
			super.add( limit != 0 ? Boolean.TRUE : Boolean.FALSE );
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

		public boolean equals( final Object o )
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

		public String toString()
		{
			StringBuffer buffer = new StringBuffer();

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
}
