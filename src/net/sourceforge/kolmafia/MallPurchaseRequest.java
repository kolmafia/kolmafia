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

/**
 * An extension of <code>KoLRequest</code> which handles the purchase of
 * items from the Mall of Loathing.
 */

public class MallPurchaseRequest extends KoLRequest
{
	private boolean wasExecuted;
	private String itemName, shopName;
	private int quantity, price;

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> with the given values.
	 * Note that the only value which can be modified at a later time is the
	 * quantity of items being purchases; all others are consistent through
	 * the time when the purchase is actually executed.
	 *
	 * @param	client	The client to which this request reports errors
	 * @param	itemName	The name of the item to be purchased
	 * @param	quantity	The quantity of items to be purchased
	 * @param	shopID	The integer identifier for the shop from which the item will be purchased
	 * @param	shopName	The name of the shop
	 * @param	price	The price at which the item will be purchased
	 */

	public MallPurchaseRequest( KoLmafia client, String itemName, int quantity, int shopID, String shopName, int price )
	{
		super( client, "mallstore.php" );

		this.itemName = itemName;
		this.shopName = shopName;
		this.quantity = quantity;
		this.price = price;

		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "whichstore", "" + shopID );
		addFormField( "buying", "Yep." );

		// With the basic fields out of the way, you need to construct
		// the string representing the item you want to buy at the price
		// you wish to buy at.

		StringBuffer whichItem = new StringBuffer();
		whichItem.append( TradeableItemDatabase.getItemID( itemName ) );

		// First append the item ID.  Until the item database is done,
		// there's no way to look up the item.

		while ( whichItem.length() < 3 )
			whichItem.insert( 0, '0' );

		whichItem.append( price );

		while ( whichItem.length() < 12 )
			whichItem.insert( 3, '0' );

		addFormField( "whichitem", whichItem.toString() );
	}

	/**
	 * Converts this request into a readable string.  This is useful for
	 * debugging and as a temporary substitute for a list panel, in the
	 * event that a suitable list cell renderer has not been created.
	 */

	public String toString()
	{	return itemName + " (" + quantity + " @ " + price + "): " + shopName;
	}

	/**
	 * Accessor method to set the quantity of items to be purchased to the
	 * given value.  This is useful when the user does not want to purchase
	 * the maximum number of items available to them.
	 *
	 * @param	quantity	The number of items to be purchased
	 */

	public void setQuantity( int quantity )
	{	this.quantity = quantity;
	}

	/**
	 * Accessor method which returns the total number of items which
	 * will be purchased when this <code>MallPurchaseRequest</code>
	 * is finally run.
	 */

	public int getQuantity()
	{	return quantity;
	}

	/**
	 * Accessor method which returns the total cost of running this
	 * purchase request.
	 *
	 * @return	The total cost of running this <code>MallPurchaseRequest</code>
	 */

	public int getRequestCost()
	{	return quantity * price;
	}

	/**
	 * Executes the purchase request.  This calculates the number
	 * of items which will be purchased and adds it to the list.
	 * Note that it marks whether or not it's already been run
	 * to avoid problems with repeating the request.
	 */

	public void run()
	{
		if ( wasExecuted )
			return;

		addFormField( "quantity", "" + quantity );
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// Once it reaches this point, you know that the
		// request was executed.  Therefore, reset state
		// variables and begin parsing the reply.

		if ( replyContent.indexOf( "acquire" ) != -1 )
		{
			// One error is that the item price changed, or the item
			// is no longer available because someone was faster at
			// purchasing the item.  If that's the case, just return
			// without doing anything; nothing left to do.



			// One error that might be encountered is that the user
			// already purchased the item; if that's the case, and
			// the user hasn't exhausted their limit, then make a
			// second request to the server containing the correct
			// number of items to buy.
		}
		else
		{
			// Otherwise, you managed to purchase something!  Here,
			// you report to the client whatever you gained.
		}

		wasExecuted = true;
	}
}
