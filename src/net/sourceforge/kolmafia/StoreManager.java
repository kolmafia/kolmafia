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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

public abstract class StoreManager extends StaticEntity
{
	private static LockableListModel soldItemList = new LockableListModel();

	public static void reset()
	{	soldItemList.clear();
	}

	/**
	 * Registers an item inside of the store manager.  Note
	 * that this includes the price of the item and the
	 * limit which is used to sell the item.
	 */

	public static void registerItem( int itemID, int quantity, int price, int limit, int lowest )
	{	soldItemList.add( new SoldItem( itemID, quantity, price, limit, lowest ) );
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

	/**
	 * Returns a list of the items which are handled by
	 * the current store manager.  Note that this list
	 * may or may not be up-to-date.
	 */

	public static LockableListModel getSoldItemList()
	{	return soldItemList;
	}

	public static synchronized void update( String storeText, boolean isPriceManagement )
	{
		soldItemList.clear();

		// Now that the item list has been cleared, begin
		// parsing the store text.

		if ( isPriceManagement )
		{
			int itemID, quantity, price, limit, lowest;

			// The item matcher here examines each row in the table
			// displayed in the price management page.

			Matcher priceMatcher = Pattern.compile( "<tr>.*?<td>([\\d,]+)</td>.*?\"(\\d+)\" name=price(\\d+).*?value=\"(\\d+)\".*?<td>([\\d,]+)</td>" ).matcher( storeText );

			try
			{
				while ( priceMatcher.find() )
				{
					itemID = Integer.parseInt( priceMatcher.group(3) );
					quantity = df.parse( priceMatcher.group(1) ).intValue();

					price = df.parse( priceMatcher.group(2) ).intValue();
					limit = df.parse( priceMatcher.group(4) ).intValue();
					lowest = df.parse( priceMatcher.group(5) ).intValue();

					// Now that all the data has been retrieved, register
					// the item that was discovered.

					registerItem( itemID, quantity, price, limit, lowest );
				}
			}
			catch ( Exception e )
			{
				// Because of the way the regular expressions are
				// set up, this should not happen.
			}
		}
		else
		{
			AdventureResult item;
			int price, limit;

			// The item matcher here examines each row in the table
			// displayed in the standard item-addition page.

			Matcher itemMatcher = Pattern.compile( "<tr><td><img src.*?></td><td>(.*?)</td><td>([\\d,]+)</td><td>(.*?)</td>" ).matcher( storeText );

			try
			{
				while ( itemMatcher.find() )
				{
					item = AdventureResult.parseResult( itemMatcher.group(1) );
					price = df.parse( itemMatcher.group(2) ).intValue();

					// In this case, the limit could appear as "unlimited",
					// which equates to a limit of 0.

					limit = itemMatcher.group(3).startsWith( "<" ) ? 0 : df.parse( itemMatcher.group(3) ).intValue();

					// Now that all the data has been retrieved, register
					// the item that was discovered.

					registerItem( item.getItemID(), item.getCount(), price, limit, 0 );
				}
			}
			catch ( Exception e )
			{
				// Because of the way the regular expressions are
				// set up, this should not happen.
			}
		}
	}

	/**
	 * Utility method used to search the mall for the
	 * given item.
	 */

	public static void searchMall( String itemName, List priceSummary )
	{	(new MallSearchThread( itemName, priceSummary )).start();
	}

	private static class MallSearchThread extends DaemonThread
	{
		private String itemName;
		private List priceSummary;

		public MallSearchThread( String itemName, List priceSummary )
		{
			this.itemName = itemName;
			this.priceSummary = priceSummary;
		}

		public void run()
		{
			priceSummary.clear();
			if ( itemName == null )
				return;

			ArrayList results = new ArrayList();

			// For items with the n-tilde character, a
			// perfect match is available if you use the
			// substring consisting of everything after
			// the ntilde;

			if ( itemName.indexOf( "\u00f1" ) != -1 )
				itemName = itemName.substring( itemName.indexOf( "\u00f1" ) + 1 );

			// For items with the trademark character, a
			// perfect match is available if you use the
			// substring consisting of everything before
			// the trademark character

			else if ( itemName.indexOf( "\u2122" ) != -1 )
				itemName = itemName.substring( 0, itemName.indexOf( "\u2122" ) );

			else if ( itemName.indexOf( "\u00e9" ) != -1 )
				itemName = itemName.substring( 0, itemName.indexOf( "\u00e9" ) );

			// All items with double quotes can be matched
			// by searching on everything before the double

			else if ( itemName.indexOf( "\"" ) != -1 )
				itemName = itemName.substring( 0, itemName.indexOf( "\"" ) );

			// In all other cases, an exact match is only
			// available if you enclose the item name in
			// double quotes.

			else
				itemName = "\"" + itemName + "\"";

			// With the item name properly formatted, issue
			// the search request.

			(new SearchMallRequest( client, itemName, 0, results, true )).run();

			MallPurchaseRequest [] resultsArray = new MallPurchaseRequest[ results.size() ];
			results.toArray( resultsArray );

			for ( int i = 0; i < resultsArray.length; ++i )
			{
				if ( resultsArray[i].getQuantity() == resultsArray[i].getLimit() )
					priceSummary.add( "  " + df.format( resultsArray[i].getQuantity() ) + " @ " + df.format( resultsArray[i].getPrice() ) + " meat" );
				else
					priceSummary.add( "  " + df.format( resultsArray[i].getQuantity() ) + "(limit " + df.format( resultsArray[i].getLimit() ) + ") @ " + df.format( resultsArray[i].getPrice() ) );
			}

			client.updateDisplay( ENABLE_STATE, "" );
		}
	}

	/**
	 * Utility method used to remove the given item from the
	 * player's store.
	 */

	public static void takeItem( int itemID )
	{	(new RequestThread( new StoreManageRequest( client, itemID ) )).start();
	}

	/**
	 * Internal immutable class used to hold a single instance
	 * of an item sold in a player's store.
	 */

	public static class SoldItem
	{
		private int itemID;
		private int quantity;
		private int price;
		private int limit;
		private int lowest;

		public SoldItem( int itemID, int quantity, int price, int limit, int lowest )
		{
			this.itemID = itemID;
			this.quantity = quantity;
			this.price = price;
			this.limit = limit;
			this.lowest = lowest;
		}

		public int getItemID()
		{	return itemID;
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
	}
}
