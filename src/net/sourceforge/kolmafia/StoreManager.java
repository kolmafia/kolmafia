/**
 * Copyright (c) 2005-2006, KoLmafia development team
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

public abstract class StoreManager extends StaticEntity
{
	private static final Pattern LOGSPAN_PATTERN = Pattern.compile( "<span.*?</span>" );
	private static final Pattern ADDER_PATTERN = Pattern.compile( "<tr><td><img src.*?></td><td>(.*?)</td><td>([\\d,]+)</td><td>(.*?)</td><td.*?(\\d+)" );
	private static final Pattern PRICER_PATTERN = Pattern.compile( "<tr><td><b>(.*?)\\&nbsp;.*?<td>([\\d,]+)</td>.*?\"(\\d+)\" name=price(\\d+).*?value=\"(\\d+)\".*?<td>([\\d,]+)</td>" );

	private static final int RECENT_FIRST = 1;
	private static final int OLDEST_FIRST = 2;
	private static final int GROUP_BY_NAME = 3;

	private static int currentLogSort = RECENT_FIRST;
	private static boolean sortItemsByName = false;

	private static long potentialEarnings = 0;
	private static LockableListModel storeLog = new LockableListModel();
	private static LockableListModel soldItemList = new LockableListModel();
	private static LockableListModel sortedSoldItemList = new LockableListModel();

	public static void reset()
	{
		potentialEarnings = 0;
		soldItemList.clear();
		sortedSoldItemList.clear();
	}

	public static long getPotentialEarnings()
	{	return potentialEarnings;
	}

	/**
	 * Registers an item inside of the store manager.  Note
	 * that this includes the price of the item and the
	 * limit which is used to sell the item.
	 */

	public static SoldItem registerItem( int itemId, int quantity, int price, int limit, int lowest )
	{
		if ( price < 50000000 )
			potentialEarnings += (long) price * (long) quantity;

		SoldItem newItem = new SoldItem( itemId, quantity, price, limit, lowest );
		int itemIndex = soldItemList.indexOf( newItem );

		// If the item is brand-new, just add it to the
		// list of sold items.

		if ( itemIndex == -1 )
		{
			soldItemList.add( newItem );
			sortedSoldItemList.add( newItem );
		}
		else
		{
			// If the item already exists, check it against the
			// one which already exists in the list.  If there
			// are any changes, update.

			SoldItem oldItem = (SoldItem) soldItemList.get( itemIndex );

			if ( oldItem.getQuantity() != newItem.getQuantity() || oldItem.getPrice() != newItem.getPrice() || oldItem.getLimit() != newItem.getLimit() || oldItem.getLowest() != newItem.getLowest() )
			{
				soldItemList.set( itemIndex, newItem );
				sortedSoldItemList.set( sortedSoldItemList.indexOf( newItem ), newItem );
			}
		}

		return newItem;
	}

	/**
	 * Returns the current price of the item with the given
	 * item Id.  This is useful for auto-adding at the
	 * existing price.
	 */

	public static int getPrice( int itemId )
	{
		int currentPrice = 999999999;
		for ( int i = 0; i < soldItemList.size(); ++i )
			if ( ((SoldItem)soldItemList.get(i)).getItemId() == itemId )
				currentPrice = ((SoldItem)soldItemList.get(i)).getPrice();

		return currentPrice;
	}

	public static LockableListModel getSoldItemList()
	{	return soldItemList;
	}

	public static LockableListModel getSortedSoldItemList()
	{	return sortedSoldItemList;
	}

	public static LockableListModel getStoreLog()
	{	return storeLog;
	}

	public static void sortStoreLog( boolean cycleSortType )
	{
		if ( cycleSortType )
		{
			switch ( currentLogSort )
			{
			case RECENT_FIRST:
				currentLogSort = OLDEST_FIRST;
				break;
			case OLDEST_FIRST:
				currentLogSort = GROUP_BY_NAME;
				break;
			case GROUP_BY_NAME:
				currentLogSort = RECENT_FIRST;
				break;
			}
		}

		// Because StoreLogEntry objects use the current
		// internal variable to decide how to sort, a simple
		// function call will suffice.

		storeLog.sort();
	}

	public static void update( String storeText, boolean isPriceManagement )
	{
		potentialEarnings = 0;
		ArrayList newItems = new ArrayList();

		if ( isPriceManagement )
		{
			int itemId, quantity, price, limit, lowest;

			// The item matcher here examines each row in the table
			// displayed in the price management page.

			Matcher priceMatcher = PRICER_PATTERN.matcher( storeText );

			while ( priceMatcher.find() )
			{
				itemId = parseInt( priceMatcher.group(4) );
				if ( TradeableItemDatabase.getItemName( itemId ) == null )
					TradeableItemDatabase.registerItem( itemId, priceMatcher.group(1) );

				quantity = parseInt( priceMatcher.group(2) );

				price = parseInt( priceMatcher.group(3) );
				limit = parseInt( priceMatcher.group(5) );
				lowest = parseInt( priceMatcher.group(6) );

				// Now that all the data has been retrieved, register
				// the item that was discovered.

				newItems.add( registerItem( itemId, quantity, price, limit, lowest ) );
			}
		}
		else
		{
			AdventureResult item;
			int itemId, price, limit;

			// The item matcher here examines each row in the table
			// displayed in the standard item-addition page.

			Matcher itemMatcher = ADDER_PATTERN.matcher( storeText );

			while ( itemMatcher.find() )
			{
				itemId = parseInt( itemMatcher.group(4) );
				if ( TradeableItemDatabase.getItemName( itemId ) == null )
				{
					String itemName = itemMatcher.group(1);
					if ( itemName.indexOf( "(" ) != -1 )
						itemName = itemName.substring( 0, itemName.indexOf( "(" ) ).trim();

					TradeableItemDatabase.registerItem( itemId, itemName );
				}

				// Remove parenthesized number and match again.
				StringTokenizer parsedItem = new StringTokenizer( itemMatcher.group(1), "()" );
				String name = parsedItem.nextToken().trim();
				int count = 1;

				if ( parsedItem.hasMoreTokens() )
					count = parseInt( parsedItem.nextToken() );

				item = new AdventureResult( name, count, false );
				price = parseInt( itemMatcher.group(2) );

				// In this case, the limit could appear as "unlimited",
				// which equates to a limit of 0.

				limit = itemMatcher.group(3).startsWith( "<" ) ? 0 : parseInt( itemMatcher.group(3) );

				// Now that all the data has been retrieved, register
				// the item that was discovered.

				newItems.add( registerItem( item.getItemId(), item.getCount(), price, limit, 0 ) );
			}
		}

		soldItemList.retainAll( newItems );
		sortedSoldItemList.retainAll( newItems );

		sortItemsByName = true;
		soldItemList.sort();

		sortItemsByName = false;
		sortedSoldItemList.sort();

		// Now, update the title of the store manage
		// frame to reflect the new price.

		StoreManageFrame.updateEarnings( potentialEarnings );
	}

	public static void parseLog( String logText )
	{
		storeLog.clear();

		Matcher logMatcher = LOGSPAN_PATTERN.matcher( logText );
		if ( logMatcher.find() )
		{
			if ( logMatcher.group().indexOf( "<br>" ) == -1 )
				return;

			String [] entries = logMatcher.group().split( "<br>" );

			for ( int i = 0; i < entries.length - 1; ++i )
				storeLog.add( new StoreLogEntry( entries.length - i - 1, entries[i].replaceAll( "<.*?>", "" ) ) );

			sortStoreLog( false );
		}
	}

	private static class StoreLogEntry implements Comparable
	{
		private int id;
		private String text;
		private String stringForm;

		public StoreLogEntry( int id, String text )
		{
			this.id = id;

			String [] pieces = text.split( " " );
			this.text = text.substring( pieces[0].length() + pieces[1].length() + 2 );
			this.stringForm = id + ": " + text;
		}

		public String toString()
		{	return stringForm;
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof StoreLogEntry) )
				return -1;

			switch ( currentLogSort )
			{
			case RECENT_FIRST:
				return ((StoreLogEntry)o).id - id;
			case OLDEST_FIRST:
				return id - ((StoreLogEntry)o).id;
			case GROUP_BY_NAME:
				return text.compareToIgnoreCase( ((StoreLogEntry)o).text );
			default:
				return -1;
			}
		}
	}

	/**
	 * Utility method used to search the mall for the
	 * given item.
	 */

	public static void searchMall( String itemName, List resultSummary, int maximumResults, boolean toString )
	{
		resultSummary.clear();
		if ( itemName == null )
			return;

		ArrayList results = new ArrayList();

		// With the item name properly formatted, issue
		// the search request.

		(new SearchMallRequest( SearchMallRequest.getItemName( itemName ), maximumResults, results, true )).run();
		if ( !toString )
		{
			resultSummary.addAll( results );
			return;
		}

		MallPurchaseRequest [] resultsArray = new MallPurchaseRequest[ results.size() ];
		results.toArray( resultsArray );

		TreeMap prices = new TreeMap();
		Integer currentQuantity, currentPrice;

		for ( int i = 0; i < resultsArray.length; ++i )
		{
			currentPrice = new Integer( resultsArray[i].getPrice() );
			currentQuantity = (Integer) prices.get( currentPrice );

			if ( currentQuantity == null )
				prices.put( currentPrice, new Integer( resultsArray[i].getLimit() ) );
			else
				prices.put( currentPrice, new Integer( currentQuantity.intValue() + resultsArray[i].getLimit() ) );
		}

		Integer [] priceArray = new Integer[ prices.keySet().size() ];
		prices.keySet().toArray( priceArray );

		for ( int i = 0; i < priceArray.length; ++i )
			resultSummary.add( "  " + COMMA_FORMAT.format( ((Integer)prices.get( priceArray[i] )).intValue() ) + " @ " + COMMA_FORMAT.format( priceArray[i].intValue() ) + " meat" );
	}

	/**
	 * Internal immutable class used to hold a single instance
	 * of an item sold in a player's store.
	 */

	public static class SoldItem extends Vector implements Comparable
	{
		private int itemId;
		private String itemName;
		private int quantity;
		private int price;
		private int limit;
		private int lowest;

		public SoldItem( int itemId, int quantity, int price, int limit, int lowest )
		{
			this.itemId = itemId;
			this.itemName = TradeableItemDatabase.getItemName( itemId );
			this.quantity = quantity;
			this.price = price;
			this.limit = limit;
			this.lowest = lowest;

			super.add( new AdventureResult( itemId, quantity ) );
			super.add( new Integer( price ) );
			super.add( new Integer( lowest ) );
			super.add( new Integer( quantity ) );
			super.add( limit != 0 ? Boolean.TRUE : Boolean.FALSE );
		}

		public int getItemId()
		{	return itemId;
		}

		public String getItemName()
		{	return itemName;
		}

		public int getQuantity()
		{	return quantity;
		}

		public int getPrice()
		{	return price;
		}

		public int getLimit()
		{	return limit;
		}

		public int getLowest()
		{	return lowest;
		}

		public boolean equals( Object o )
		{	return o != null && o instanceof SoldItem && ((SoldItem)o).itemId == itemId;
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof SoldItem) )
				return -1;

			if ( price != 999999999 && ((SoldItem)o).price == 999999999 )
				return -1;

			if ( price == 999999999 && ((SoldItem)o).price != 999999999 )
				return 1;

			if ( price == 999999999 && ((SoldItem)o).price == 999999999 )
				return itemName.compareToIgnoreCase( ((SoldItem)o).itemName );

			return sortItemsByName ? itemName.compareToIgnoreCase( ((SoldItem)o).itemName ) : price - ((SoldItem)o).price;
		}

		public String toString()
		{
			StringBuffer buffer = new StringBuffer();

			buffer.append( TradeableItemDatabase.getItemName( itemId ) );
			buffer.append( " (" );

			buffer.append( COMMA_FORMAT.format( quantity ) );

			if ( limit < quantity )
			{
				buffer.append( " limit " );
				buffer.append( COMMA_FORMAT.format( limit ) );
			}

			buffer.append( " @ " );
			buffer.append( COMMA_FORMAT.format( price ) );
			buffer.append( ")" );

			return buffer.toString();

		}
	}
}
