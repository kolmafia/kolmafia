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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.DefaultListCellRenderer;

/**
 * An extension of <code>KoLRequest</code> which handles the purchase of
 * items from the Mall of Loathing.
 */

public class MallPurchaseRequest extends KoLRequest implements Comparable
{
	// In order to prevent overflows from happening, make
	// it so that the maximum quantity is 10 million

	private String itemName, shopName;
	private int itemID, shopID, quantity, price, limit;

	private boolean isNPCStore;
	private String npcStoreID;

	private boolean canPurchase;
	public static final int MAX_QUANTITY = 10000000;

	private boolean attireChanged = false;

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> which retrieves
	 * things from NPC stores.
	 */

	public MallPurchaseRequest( KoLmafia client, String storeName, String storeID, int itemID, int price )
	{
		super( client, storeID.indexOf( "." ) == -1 ? "store.php" : storeID );

		if ( storeID.indexOf( "." ) == -1 )
		{
			addFormField( "whichstore", storeID );
			addFormField( "phash" );
			addFormField( "buying", "Yep." );
		}
		else if ( storeID.equals( "galaktik.php" ) )
		{
			// Annoying special case.
			addFormField( "action", "buyitem" );
			addFormField( "pwd" );
		}
		else
		{
			addFormField( "action", "buy" );
			addFormField( "pwd" );
		}

		addFormField( "whichitem", String.valueOf( itemID ) );

		this.itemName = TradeableItemDatabase.getItemName( itemID );
		this.shopName = storeName;
		this.itemID = itemID;
		this.shopID = 0;
		this.quantity = MAX_QUANTITY;
		this.limit = quantity;
		this.price = price;

		this.isNPCStore = true;
		this.npcStoreID = storeID;
		this.canPurchase = true;
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

	public MallPurchaseRequest( KoLmafia client, String itemName, int itemID, int quantity, int shopID, String shopName, int price, int limit, boolean canPurchase )
	{
		super( client, "mallstore.php" );

		this.itemID = itemID;

		if ( TradeableItemDatabase.getItemName( itemID ) == null )
			TradeableItemDatabase.registerItem( itemID, itemName );

		this.itemName = TradeableItemDatabase.getItemName( this.itemID );

		this.shopID = shopID;
		this.shopName = shopName;
		this.quantity = quantity;
		this.price = price;
		this.limit = Math.min( quantity, limit );
		this.isNPCStore = false;
		this.canPurchase = canPurchase;

		addFormField( "pwd" );
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
	 * Retrieves the quantity of the item available in the store.
	 * @return	The quantity of the item in the store
	 */

	public int getQuantity()
	{	return quantity;
	}

	/**
	 * Sets the quantity of the item available in the store.
	 * @param	quantity	The quantity of the item available in the store.
	 */

	public void setQuantity( int quantity )
	{	this.quantity = quantity;
	}

	/**
	 * Retrieves the quantity of the item being purchased.
	 * @return	The quantity of the item being purchased
	 */

	public int getLimit()
	{	return limit;
	}

	/**
	 * Sets the maximum number of items that can be purchased through
	 * this request.
	 *
	 * @param	limit	The maximum number of items to be purchased with this request
	 */

	public void setLimit( int limit )
	{	this.limit = Math.min( quantity, limit );
	}

	/**
	 * Converts this request into a readable string.  This is useful for
	 * debugging and as a temporary substitute for a list panel, in the
	 * event that a suitable list cell renderer has not been created.
	 */

	public String toString()
	{
		StringBuffer buffer = new StringBuffer();

		if ( !existingFrames.isEmpty() )
		{
			buffer.append( "<html><nobr>" );
			if ( !canPurchase )
				buffer.append( "<font color=gray>" );
		}

		buffer.append( itemName );
		buffer.append( " (" );

		if ( quantity == MAX_QUANTITY )
			buffer.append( "unlimited" );
		else
		{
			buffer.append( COMMA_FORMAT.format( quantity ) );

			if ( limit < quantity || !canPurchase )
			{
				buffer.append( " limit " );
				buffer.append( COMMA_FORMAT.format( limit ) );
			}
		}

		buffer.append( " @ " );
		buffer.append( COMMA_FORMAT.format( price ) );
		buffer.append( "): " );
		buffer.append( shopName );

		if ( !existingFrames.isEmpty() )
		{
			if ( !canPurchase )
				buffer.append( "</font>" );

			buffer.append( "</nobr></html>" );
		}

		return buffer.toString();
	}

	public void setCanPurchase( boolean canPurchase )
	{	this.canPurchase = canPurchase;
	}

	/**
	 * Executes the purchase request.  This calculates the number
	 * of items which will be purchased and adds it to the list.
	 * Note that it marks whether or not it's already been run
	 * to avoid problems with repeating the request.
	 */

	public void run()
	{
		if ( limit < 1 || !canPurchase )
			return;

		addFormField( isNPCStore ? "howmany" : "quantity", String.valueOf( limit ) );

		// If the item is not currently recognized, the user should
		// be notified that the purchases cannot be made because of that

		if ( itemID == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Item not recognized by KoLmafia database." );
			return;
		}

		// Check to make sure that the person is wearing the appropriate
		// outfit for making the purchase.

		ensureProperAttire();

		// Now that everything's ensured, go ahead and execute the
		// actual purchase request.

		KoLmafia.updateDisplay( "Purchasing " + TradeableItemDatabase.getItemName( itemID ) + " (" + COMMA_FORMAT.format( limit ) + " @ " + COMMA_FORMAT.format( price ) + ")..." );
		super.run();

		if ( attireChanged )
			SpecialOutfit.restoreCheckpoint();
	}

	public int compareTo( Object o )
	{
		return ( o == null || !( o instanceof MallPurchaseRequest ) ) ? 1 :
			compareTo( (MallPurchaseRequest) o );
	}

	public int compareTo( MallPurchaseRequest mpr )
	{	return price - mpr.price;
	}

	private void ensureProperAttire()
	{
		if ( !isNPCStore )
			return;

		int neededOutfit = 0;

		if ( npcStoreID.equals( "b" ) )
			neededOutfit = 1;

		if ( npcStoreID.equals( "g" ) )
			neededOutfit = 5;

		if ( npcStoreID.equals( "h" ) )
			neededOutfit = 2;

		if ( neededOutfit == 0 )
			return;

		// Only switch outfits if the person is not
		// currently wearing the outfit.

		if ( !EquipmentDatabase.isWearingOutfit( neededOutfit ) )
		{
			SpecialOutfit.createCheckpoint();
			(new EquipmentRequest( client, EquipmentDatabase.getOutfit( neededOutfit ) )).run();
			attireChanged = true;
		}
	}

	protected void processResults()
	{
		int startIndex = responseText.indexOf( "<center>" );
		int stopIndex = responseText.indexOf( "</table>" );

		if ( startIndex == -1 || stopIndex == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Unexpected result.  Are you sure this store is available?" );
			return;
		}

		String result = responseText.substring( startIndex, stopIndex );

		// One error is that the item price changed, or the item
		// is no longer available because someone was faster at
		// purchasing the item.  If that's the case, just return
		// without doing anything; nothing left to do.

		if ( responseText.indexOf( "You can't afford" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Not enough funds." );
			return;
		}

		// Another thing to search for is to see if the person
		// swapped the price on the item, or you got a "failed
		// to yield" message.  In that case, you may wish to
		// re-attempt the purchase.

		if ( responseText.indexOf( "This store doesn't" ) != -1 || responseText.indexOf( "failed to yield" ) != -1 )
		{
			Matcher itemChangedMatcher = Pattern.compile(
				"<td valign=center><b>" + itemName + "</b> \\(([\\d,]+)\\) </td><td>([\\d,]+) Meat" ).matcher( result );

			if ( itemChangedMatcher.find() )
			{
				int limit = StaticEntity.parseInt( itemChangedMatcher.group(1) );
				int newPrice = StaticEntity.parseInt( itemChangedMatcher.group(2) );

				// If the item exists at a lower or equivalent
				// price, then you should re-attempt the purchase
				// of the item.

				if ( price >= newPrice )
				{
					KoLmafia.updateDisplay( "Failed to yield.  Attempting repurchase..." );
					(new MallPurchaseRequest( client, itemName, itemID, Math.min( limit, quantity ), shopID, shopName, newPrice, Math.min( limit, quantity ), true )).run();
				}
				else
				{
					// In the event of a price switch, give the
					// player the option to report it.

					KoLmafia.updateDisplay( "Price switch detected (#" + shopID + ").  Skipping..." );
				}
			}
			else
			{
				// If the item was not found, just make sure to
				// notify the user temporarily that the store
				// failed to yield the item.

				KoLmafia.updateDisplay( "Failed to yield.  Skipping..." );
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

		if ( quantityMatcher.find() )
		{
			int limit = StaticEntity.parseInt( quantityMatcher.group(1) );
			int alreadyPurchased = StaticEntity.parseInt( quantityMatcher.group(2) );

			if ( limit != alreadyPurchased )
				(new MallPurchaseRequest( client, itemName, itemID, limit - alreadyPurchased, shopID, shopName, price, limit, true )).run();

			canPurchase = false;
			return;
		}

		// Otherwise, you managed to purchase something!  Here,
		// you report to the client whatever you gained.

		int quantityAcquired = responseText.indexOf( "You acquire an item: <b>" ) != -1 ? 1 : 0;
		for ( int i = limit; i > 0 && quantityAcquired == 0; --i )
			if ( responseText.indexOf( "acquire <b>" + COMMA_FORMAT.format( i ) ) != -1 )
				quantityAcquired = i;

		client.processResult( new AdventureResult( itemID, quantityAcquired ) );
		client.processResult( new AdventureResult( AdventureResult.MEAT, -1 * price * quantityAcquired ) );

		KoLCharacter.updateStatus();
		RequestFrame.refreshStatus();
	}

	public String getCommandForm( int iterations )
	{	return "buy " + limit + " " + itemName;
	}
}
