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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public abstract class StoreManager extends StaticEntity
{
	private static final int RECENT_FIRST = 1;
	private static final int OLDEST_FIRST = 2;
	private static final int GROUP_BY_NAME = 3;
	
	private static int currentSortType = RECENT_FIRST;

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

	/**
	 * Registers an item inside of the store manager.  Note
	 * that this includes the price of the item and the
	 * limit which is used to sell the item.
	 */

	public static SoldItem registerItem( int itemID, int quantity, int price, int limit, int lowest )
	{
		if ( price < 50000000 )
			potentialEarnings += (long) price * (long) quantity;

		SoldItem newItem = new SoldItem( itemID, quantity, price, limit, lowest );
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
	 * item ID.  This is useful for auto-adding at the
	 * existing price.
	 */

	public static int getPrice( int itemID )
	{
		int currentPrice = 999999999;
		for ( int i = 0; i < soldItemList.size(); ++i )
			if ( ((SoldItem)soldItemList.get(i)).getItemID() == itemID )
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
			switch ( currentSortType )
			{
				case RECENT_FIRST:
					currentSortType = OLDEST_FIRST;
					break;
				case OLDEST_FIRST:
					currentSortType = GROUP_BY_NAME;
					break;
				case GROUP_BY_NAME:
					currentSortType = RECENT_FIRST;
					break;
			}
		}
		
		// Because StoreLogEntry objects use the current
		// internal variable to decide how to sort, a simple
		// function call will suffice.
		
		Collections.sort( storeLog );
	}

	public static void update( String storeText, boolean isPriceManagement )
	{
		potentialEarnings = 0;
		ArrayList newItems = new ArrayList();

		if ( isPriceManagement )
		{
			int itemID, quantity, price, limit, lowest;

			// The item matcher here examines each row in the table
			// displayed in the price management page.

			Matcher priceMatcher = Pattern.compile( "<tr><td><b>(.*?)\\&nbsp;.*?<td>([\\d,]+)</td>.*?\"(\\d+)\" name=price(\\d+).*?value=\"(\\d+)\".*?<td>([\\d,]+)</td>" ).matcher( storeText );

			try
			{
				while ( priceMatcher.find() )
				{
					itemID = Integer.parseInt( priceMatcher.group(4) );
					if ( TradeableItemDatabase.getItemName( itemID ) == null )
						TradeableItemDatabase.registerItem( itemID, priceMatcher.group(1) );

					quantity = df.parse( priceMatcher.group(2) ).intValue();

					price = df.parse( priceMatcher.group(3) ).intValue();
					limit = df.parse( priceMatcher.group(5) ).intValue();
					lowest = df.parse( priceMatcher.group(6) ).intValue();

					// Now that all the data has been retrieved, register
					// the item that was discovered.

					newItems.add( registerItem( itemID, quantity, price, limit, lowest ) );
				}
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e );
			}
		}
		else
		{
			AdventureResult item;
			int itemID, price, limit;

			// The item matcher here examines each row in the table
			// displayed in the standard item-addition page.

			Matcher itemMatcher = Pattern.compile( "<tr><td><img src.*?></td><td>(.*?)</td><td>([\\d,]+)</td><td>(.*?)</td><td.*?(\\d+)" ).matcher( storeText );

			try
			{
				while ( itemMatcher.find() )
				{
					itemID = Integer.parseInt( itemMatcher.group(4) );
					if ( TradeableItemDatabase.getItemName( itemID ) == null )
					{
						String itemName = itemMatcher.group(1);
						if ( itemName.indexOf( "(" ) != -1 )
							itemName = itemName.substring( 0, itemName.indexOf( "(" ) ).trim();

						TradeableItemDatabase.registerItem( itemID, itemName );
					}

					// Remove parenthesized number and match again.
					StringTokenizer parsedItem = new StringTokenizer( itemMatcher.group(1), "()" );
					String name = parsedItem.nextToken().trim();
					int count = 1;

					if ( parsedItem.hasMoreTokens() )
					{
						try
						{
							count = df.parse( parsedItem.nextToken() ).intValue();
						}
						catch ( Exception e )
						{
							// This should not happen.  Therefore, print
							// a stack trace for debug purposes.
							
							StaticEntity.printStackTrace( e );
						}
					}
					
					item = new AdventureResult( name, count, false );
					price = df.parse( itemMatcher.group(2) ).intValue();

					// In this case, the limit could appear as "unlimited",
					// which equates to a limit of 0.

					limit = itemMatcher.group(3).startsWith( "<" ) ? 0 : df.parse( itemMatcher.group(3) ).intValue();

					// Now that all the data has been retrieved, register
					// the item that was discovered.

					newItems.add( registerItem( item.getItemID(), item.getCount(), price, limit, 0 ) );
				}
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e );
			}
		}

		soldItemList.retainAll( newItems );
		sortedSoldItemList.retainAll( newItems );
		Collections.sort( sortedSoldItemList );

		// Now, update the title of the store manage
		// frame to reflect the new price.

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			if ( frames[i] instanceof StoreManageFrame )
				frames[i].setTitle( "Store Manager (potential earnings: " + df.format( potentialEarnings ) + " meat)" );
	}

	public static void parseLog( String logText )
	{
		storeLog.clear();

		Matcher logMatcher = Pattern.compile( "<span.*?</span>" ).matcher( logText );
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
			
			switch ( currentSortType )
			{
				case RECENT_FIRST:
					return id - ((StoreLogEntry)o).id;
				case OLDEST_FIRST:
					return ((StoreLogEntry)o).id - id;
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

		(new SearchMallRequest( client, itemName, maximumResults, results, true )).run();
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
				prices.put( currentPrice, new Integer( currentQuantity.intValue() + resultsArray[i].getQuantity() ) );
		}

		Integer [] priceArray = new Integer[ prices.keySet().size() ];
		prices.keySet().toArray( priceArray );

		for ( int i = 0; i < priceArray.length; ++i )
			resultSummary.add( "  " + df.format( ((Integer)prices.get( priceArray[i] )).intValue() ) + " @ " + df.format( priceArray[i].intValue() ) + " meat" );
	}

	/**
	 * Internal immutable class used to hold a single instance
	 * of an item sold in a player's store.
	 */

	public static class SoldItem implements Comparable
	{
		private int itemID;
		private String itemName;
		private int quantity;
		private int price;
		private int limit;
		private int lowest;

		public SoldItem( int itemID, int quantity, int price, int limit, int lowest )
		{
			this.itemID = itemID;
			this.itemName = TradeableItemDatabase.getItemName( itemID );
			this.quantity = quantity;
			this.price = price;
			this.limit = limit;
			this.lowest = lowest;
		}

		public int getItemID()
		{	return itemID;
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
		{	return o != null && o instanceof SoldItem && ((SoldItem)o).itemID == itemID;
		}

		public int compareTo( Object o )
		{	return o == null || !(o instanceof SoldItem) ? -1 : price - ((SoldItem)o).price;
		}

		public String toString()
		{
			StringBuffer buffer = new StringBuffer();

			buffer.append( TradeableItemDatabase.getItemName( itemID ) );
			buffer.append( " (" );

			buffer.append( df.format( quantity ) );

			if ( limit < quantity )
			{
				buffer.append( " limit " );
				buffer.append( df.format( limit ) );
			}

			buffer.append( " @ " );
			buffer.append( df.format( price ) );
			buffer.append( ")" );

			return buffer.toString();

		}
	}
}
