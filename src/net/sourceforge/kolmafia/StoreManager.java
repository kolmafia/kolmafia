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
import java.util.Iterator;
import net.java.dev.spellcast.utilities.LockableListModel;

public class StoreManager implements KoLConstants
{
	private KoLmafia client;
	private LockableListModel soldItemList;

	public StoreManager( KoLmafia client )
	{
		this.client = client;
		soldItemList = new LockableListModel();
	}

	/**
	 * Registers an item inside of the store manager.  Note
	 * that this includes the price of the item and the
	 * limit which is used to sell the item.
	 */

	public void registerItem( int itemID, int price, int limit )
	{	soldItemList.add( new SoldItem( itemID, price, limit ) );
	}

	/**
	 * Returns a list of the items which are handled by
	 * the current store manager.  Note that this list
	 * may or may not be up-to-date.
	 */

	public LockableListModel getSoldItemList()
	{	return soldItemList;
	}

	/**
	 * Clears the list of items which are handled by the
	 * current store manager.  Should be called prior to
	 * running any store management requests.
	 */

	public void clear()
	{	soldItemList.clear();
	}

	/**
	 * Utility method used to search the mall for the
	 * given item.
	 */

	public void searchMall( String itemName, List priceSummary )
	{
		if ( itemName == null )
			return;

		ArrayList results = new ArrayList();
		(new SearchMallRequest( client, "\'\'" + itemName + "\'\'", 0, results )).run();

		TreeMap prices = new TreeMap();
		MallPurchaseRequest currentItem;
		Integer currentQuantity, currentPrice;

		Iterator i = results.iterator();
		while ( i.hasNext() )
		{
			currentItem = (MallPurchaseRequest) i.next();
			currentPrice = new Integer( currentItem.getPrice() );

			currentQuantity = (Integer) prices.get( currentPrice );
			if ( currentQuantity == null )
				prices.put( currentPrice, new Integer( currentItem.getQuantity() ) );
			else
				prices.put( currentPrice, new Integer( currentQuantity.intValue() + currentItem.getQuantity() ) );
		}

		priceSummary.clear();
		i = prices.keySet().iterator();

		while ( i.hasNext() )
		{
			currentPrice = (Integer) i.next();
			priceSummary.add( "  " + df.format( ((Integer)prices.get( currentPrice )).intValue() ) + " @ " +
				df.format( currentPrice.intValue() ) + " meat" );
		}
	}

	/**
	 * Utility method used to remove the given item from the
	 * player's store.
	 */

	public void takeItem( int itemID )
	{	(new StoreManageRequest( client, itemID )).run();
	}

	/**
	 * Internal immutable class used to hold a single instance
	 * of an item sold in a player's store.
	 */

	public static class SoldItem
	{
		private int itemID;
		private int price;
		private int limit;

		public SoldItem( int itemID, int price, int limit )
		{
			this.itemID = itemID;
			this.price = price;
			this.limit = limit;
		}

		public int getItemID()
		{	return itemID;
		}

		public int getPrice()
		{	return price;
		}

		public int getLimit()
		{	return limit;
		}
	}
}