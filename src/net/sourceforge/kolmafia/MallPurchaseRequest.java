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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JOptionPane;

/**
 * An extension of <code>KoLRequest</code> which handles the purchase of
 * items from the Mall of Loathing.
 */

public class MallPurchaseRequest extends KoLRequest implements Comparable
{
	private static final int BEER_SCHLITZ = 41;
	private static final int BEER_WILLER = 81;

	private boolean successful;
	private String itemName, shopName;
	private int itemID, shopID, quantity, price;
	private boolean isNPCStore;

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> which retrieves
	 * things from NPC stores.
	 */

	public MallPurchaseRequest( KoLmafia client, String storeName, String storeID, int itemID, int price )
	{
		super( client, "store.php" );

		addFormField( "whichstore", storeID );
		addFormField( "phash", client.getPasswordHash() );
		addFormField( "buying", "Yep." );
		addFormField( "whichitem", String.valueOf( itemID ) );

		this.itemName = TradeableItemDatabase.getItemName( itemID );
		this.shopName = storeName;
		this.itemID = itemID;
		this.shopID = 0;
		this.quantity = Integer.MAX_VALUE >> 1;
		this.price = price;
		this.isNPCStore = true;
	}

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> with the given values.
	 * Note that the only value which can be modified at a later time is the
	 * quantity of items being purchases; all others are consistent through
	 * the time when the purchase is actually executed.
	 *
	 * @param	client	The client to which this request reports errors
	 * @param	itemName	The name of the item to be purchased
	 * @param	itemID	The database ID for the item to be purchased
	 * @param	quantity	The quantity of items to be purchased
	 * @param	shopID	The integer identifier for the shop from which the item will be purchased
	 * @param	shopName	The name of the shop
	 * @param	price	The price at which the item will be purchased
	 */

	public MallPurchaseRequest( KoLmafia client, String itemName, int itemID, int quantity, int shopID, String shopName, int price )
	{
		super( client, "mallstore.php" );

		this.itemID = itemID;

		if ( TradeableItemDatabase.getItemName( itemID ) == null )
			TradeableItemDatabase.registerItem( itemID, itemName );

		this.itemName = itemID == 41 ? "ice-cold beer (Schlitz)" : itemID == 81 ? "ice-cold beer (Willer)" :
			TradeableItemDatabase.getItemName( this.itemID );

		this.shopID = shopID;
		this.shopName = shopName;
		this.quantity = quantity;
		this.price = price;
		this.isNPCStore = false;

		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "whichstore", String.valueOf( shopID ) );
		addFormField( "buying", "Yep." );

		// With the basic fields out of the way, you need to construct
		// the string representing the item you want to buy at the price
		// you wish to buy at.

		StringBuffer whichItem = new StringBuffer();
		whichItem.append( itemID );

		// First append the item ID.  Until the item database is done,
		// there's no way to look up the item.

		while ( whichItem.length() < 4 )
			whichItem.insert( 0, '0' );

		whichItem.append( price );

		while ( whichItem.length() < 13 )
			whichItem.insert( 4, '0' );

		addFormField( "whichitem", whichItem.toString() );
	}

	/**
	 * Retrieves the name of the item being purchased.
	 * @return	The name of the item being purchased
	 */

	public String getItemName()
	{	return itemName;
	}

	/**
	 * Retrieves the price of the item being purchased.
	 * @return	The price of the item being purchased
	 */

	public int getPrice()
	{	return price;
	}

	/**
	 * Retrieves the quantity of the item being purchased.
	 * @return	The quantity of the item being purchased
	 */

	public int getQuantity()
	{	return quantity;
	}

	/**
	 * Sets the maximum number of items that can be purchased through
	 * this request.
	 *
	 * @param	maximumQuantity	The maximum number of items to be purchased with this request
	 */

	public void setMaximumQuantity( int maximumQuantity )
	{	this.quantity = Math.min( maximumQuantity, this.quantity );
	}

	/**
	 * Converts this request into a readable string.  This is useful for
	 * debugging and as a temporary substitute for a list panel, in the
	 * event that a suitable list cell renderer has not been created.
	 */

	public String toString()
	{	return itemName + " (" + df.format( quantity ) + " @ " + df.format( price ) + "): " + shopName;
	}

	/**
	 * Executes the purchase request.  This calculates the number
	 * of items which will be purchased and adds it to the list.
	 * Note that it marks whether or not it's already been run
	 * to avoid problems with repeating the request.
	 */

	public void run()
	{
		if ( quantity < 1 )
			return;

		addFormField( isNPCStore ? "howmany" : "quantity", String.valueOf( quantity ) );
		this.successful = false;

		// If the item is not currently recognized, the user should
		// be notified that the purchases cannot be made because of that

		if ( itemID == -1 )
		{
			client.updateDisplay( ERROR_STATE, "Item not recognized by KoLmafia database." );
			client.cancelRequest();
			return;
		}

		updateDisplay( DISABLED_STATE, "Purchasing " + itemName.replaceAll( "&ntilde;", "ñ" ).replaceAll( "&trade;", "©" ) +
			" (" + df.format( quantity ) + " @ " + df.format( price ) + ")" );

		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// Once it reaches this point, you know that the
		// request was executed.  Begin parsing the reply.

		String result = replyContent.substring( replyContent.indexOf( "<center>" ), replyContent.indexOf( "</table>" ) );

		// One error is that the item price changed, or the item
		// is no longer available because someone was faster at
		// purchasing the item.  If that's the case, just return
		// without doing anything; nothing left to do.

		if ( replyContent.indexOf( "You can't afford" ) != -1 )
		{
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Not enough funds." );
			return;
		}

		// Another thing to search for is to see if the person
		// swapped the price on the item, or you got a "failed
		// to yield" message.  In that case, you may wish to
		// re-attempt the purchase.

		if ( replyContent.indexOf( "This store doesn't" ) != -1 || replyContent.indexOf( "failed to yield" ) != -1 )
		{
			Matcher itemChangedMatcher = Pattern.compile(
				"<td valign=center><b>" + itemName + "</b> \\(([\\d,]+)\\) </td><td>([\\d,]+) Meat" ).matcher( result );

			try
			{
				if ( itemChangedMatcher.find() )
				{
					int limit = df.parse( itemChangedMatcher.group(1) ).intValue();
					int newPrice = df.parse( itemChangedMatcher.group(2) ).intValue();

					// If the item exists at a lower or equivalent
					// price, then you should re-attempt the purchase
					// of the item.

					if ( price >= newPrice )
					{
						updateDisplay( NOCHANGE, "Failed to yield.  Attempting repurchase..." );
						(new MallPurchaseRequest( client, itemName, itemID, Math.min( limit, quantity ), shopID, shopName, newPrice )).run();
					}
					else
					{
						// In the event of a price switch, give the
						// player the option to report it.

						String alertMessage = "Store #" + shopID + " (" + shopName + ") has changed its price on " + itemName + ": " +
							df.format( price ) + " -> " + df.format( newPrice ) + ".  Would you like to continue onto the next store?";

						if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null, alertMessage, "Price switch detected", JOptionPane.YES_NO_OPTION ) )
						{
							client.cancelRequest();
							updateDisplay( ERROR_STATE, "#" + shopID + ": " + itemName + " @ " + df.format( price ) + " -> " + df.format( newPrice ) );
						}
					}
				}
				else
				{
					// If the item was not found, just make sure to
					// notify the user temporarily that the store
					// failed to yield the item.

					updateDisplay( NOCHANGE, "Failed to yield.  Skipping..." );
				}
			}
			catch ( Exception e )
			{
			}

			return;
		}

		// One error that might be encountered is that the user
		// already purchased the item; if that's the case, and
		// the user hasn't exhausted their limit, then make a
		// second request to the server containing the correct
		// number of items to buy.

		Matcher quantityMatcher = Pattern.compile(
			"You may only buy ([\\d,]+) of this item per day from this store\\.You have already purchased ([\\d,]+)" ).matcher( result );

		try
		{
			if ( quantityMatcher.find() )
			{
				int limit = df.parse( quantityMatcher.group(1) ).intValue();
				int alreadyPurchased = df.parse( quantityMatcher.group(2) ).intValue();

				(new MallPurchaseRequest( client, itemName, itemID, limit - alreadyPurchased, shopID, shopName, price )).run();
				return;
			}
		}
		catch ( Exception e )
		{
			// If an exception occurs, you may wish to report
			// it to the log stream and then return from the
			// run call, for future reference.

			e.printStackTrace( logStream );
			return;
		}

		// Otherwise, you managed to purchase something!  Here,
		// you report to the client whatever you gained.

		this.successful = true;
		AdventureResult searchItem = new AdventureResult( itemID, 0 );

		int itemIndex = client.getInventory().indexOf( searchItem );
		int beforeCount = ( itemIndex == -1 ) ? 0 : ((AdventureResult)client.getInventory().get(itemIndex)).getCount();

		processResults( result );

		itemIndex = client.getInventory().indexOf( searchItem );
		int afterCount = ( itemIndex == -1 ) ? 0 : ((AdventureResult)client.getInventory().get(itemIndex)).getCount();

		// Also report how much meat you lost in the purchase
		// so that gets updated in the session summary as well.

		client.processResult( new AdventureResult( AdventureResult.MEAT, -1 * price * (afterCount - beforeCount) ) );
	}

	public int compareTo( Object o )
	{
		return ( o == null || !( o instanceof MallPurchaseRequest ) ) ? 1 :
			compareTo( (MallPurchaseRequest) o );
	}

	public int compareTo( MallPurchaseRequest mpr )
	{	return price - mpr.price;
	}
}
