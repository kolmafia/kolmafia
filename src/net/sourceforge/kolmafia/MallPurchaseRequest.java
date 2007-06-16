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

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MallPurchaseRequest extends KoLRequest implements Comparable
{
	private static final AdventureResult TROUSERS = new AdventureResult( 1792, 1 );
	private static final Pattern YIELD_PATTERN = Pattern.compile( "You may only buy ([\\d,]+) of this item per day from this store\\.You have already purchased ([\\d,]+)" );

	private static boolean usePriceComparison;

	// In order to prevent overflows from happening, make
	// it so that the maximum quantity is 10 million

	private String itemName, shopName;
	private int itemId, shopId, quantity, price, limit;

	private AdventureResult item;
	private int initialCount;

	private boolean isNPCStore;
	private String npcStoreId;

	private boolean canPurchase;
	public static final int MAX_QUANTITY = 10000000;

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> which retrieves
	 * things from NPC stores.
	 */

	public MallPurchaseRequest( String storeName, String storeId, int itemId, int price )
	{
		super( storeId.indexOf( "." ) == -1 ? "store.php" : storeId );

		if ( storeId.indexOf( "." ) == -1 )
		{
			this.addFormField( "whichstore", storeId );
			this.addFormField( "phash" );
			this.addFormField( "buying", "Yep." );
		}
		else if ( storeId.equals( "galaktik.php" ) )
		{
			// Annoying special case.
			this.addFormField( "action", "buyitem" );
			this.addFormField( "pwd" );
		}
		else
		{
			this.addFormField( "action", "buy" );
			this.addFormField( "pwd" );
		}

		this.addFormField( "whichitem", String.valueOf( itemId ) );

		this.itemName = TradeableItemDatabase.getItemName( itemId );
		this.shopName = storeName;
		this.itemId = itemId;
		this.shopId = 0;
		this.quantity = MAX_QUANTITY;
		this.limit = this.quantity;
		this.price = price;

		this.item = new AdventureResult( this.itemId, 1 );

		this.isNPCStore = true;
		this.npcStoreId = storeId;
		this.canPurchase = true;
	}

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> with the given values.
	 * Note that the only value which can be modified at a later time is the
	 * quantity of items being purchases; all others are consistent through
	 * the time when the purchase is actually executed.
	 *
	 * @param	itemName	The name of the item to be purchased
	 * @param	itemId	The database Id for the item to be purchased
	 * @param	quantity	The quantity of items to be purchased
	 * @param	shopId	The integer identifier for the shop from which the item will be purchased
	 * @param	shopName	The name of the shop
	 * @param	price	The price at which the item will be purchased
	 * @param	limit	The maximum number of items that can be purchased per day
	 * @param	canPurchase	Whether or not this purchase request is possible
	 */

	public MallPurchaseRequest( String itemName, int itemId, int quantity, int shopId, String shopName, int price, int limit, boolean canPurchase )
	{
		super( "mallstore.php" );

		this.itemId = itemId;

		if ( TradeableItemDatabase.getItemName( itemId ) == null && itemName != null )
			TradeableItemDatabase.registerItem( itemId, itemName );

		this.itemName = TradeableItemDatabase.getItemName( this.itemId );
		if ( this.itemName == null )
			this.itemName = "(unknown)";

		this.shopId = shopId;
		this.shopName = shopName;
		this.quantity = quantity;
		this.price = price;
		this.limit = Math.min( quantity, limit );
		this.isNPCStore = false;
		this.canPurchase = canPurchase;

		this.addFormField( "pwd" );
		this.addFormField( "whichstore", String.valueOf( shopId ) );
		this.addFormField( "buying", "Yep." );

		this.addFormField( "whichitem", getStoreString( itemId, price ) );

		this.item = new AdventureResult( this.itemId, 1 );
	}

	public static String getStoreString( int itemId, int price )
	{
		// With the basic fields out of the way, you need to construct
		// the string representing the item you want to buy at the price
		// you wish to buy at.

		StringBuffer whichItem = new StringBuffer();
		whichItem.append( itemId );

		// First append the item Id.  Until the item database is done,
		// there's no way to look up the item.

		int originalLength = whichItem.length();
		whichItem.append( price );

		while ( whichItem.length() < originalLength + 9 )
			whichItem.insert( originalLength, '0' );

		return whichItem.toString();
	}

	public int getItemId()
	{	return this.itemId;
	}

	public String getStoreId()
	{	return this.isNPCStore ? this.npcStoreId : String.valueOf( this.shopId );
	}

	/**
	 * Retrieves the name of the item being purchased.
	 * @return	The name of the item being purchased
	 */

	public String getItemName()
	{	return this.itemName;
	}

	/**
	 * Retrieves the price of the item being purchased.
	 * @return	The price of the item being purchased
	 */

	public int getPrice()
	{	return !this.isNPCStore || !KoLCharacter.getEquipment( KoLCharacter.PANTS ).equals( TROUSERS ) ? this.price : (int) (this.price * 0.95f);
	}

	/**
	 * Retrieves the quantity of the item available in the store.
	 * @return	The quantity of the item in the store
	 */

	public int getQuantity()
	{	return this.quantity;
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
	{	return this.limit;
	}

	/**
	 * Sets the maximum number of items that can be purchased through
	 * this request.
	 *
	 * @param	limit	The maximum number of items to be purchased with this request
	 */

	public void setLimit( int limit )
	{	this.limit = Math.min( this.quantity, limit );
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
			if ( !this.canPurchase() )
				buffer.append( "<font color=gray>" );
		}

		buffer.append( this.itemName );
		buffer.append( " (" );

		if ( this.quantity == MAX_QUANTITY )
			buffer.append( "unlimited" );
		else
		{
			buffer.append( COMMA_FORMAT.format( this.quantity ) );

			if ( this.limit < this.quantity || !this.canPurchase() )
			{
				buffer.append( " limit " );
				buffer.append( COMMA_FORMAT.format( this.limit ) );
			}
		}

		buffer.append( " @ " );
		buffer.append( COMMA_FORMAT.format( this.price ) );
		buffer.append( "): " );
		buffer.append( this.shopName );

		if ( !existingFrames.isEmpty() )
		{
			if ( !this.canPurchase() )
				buffer.append( "</font>" );

			buffer.append( "</nobr></html>" );
		}

		return buffer.toString();
	}

	public void setCanPurchase( boolean canPurchase )
	{	this.canPurchase = canPurchase;
	}

	public boolean canPurchase()
	{	return this.canPurchase && KoLCharacter.getAvailableMeat() >= this.price;
	}

	/**
	 * Executes the purchase request.  This calculates the number
	 * of items which will be purchased and adds it to the list.
	 * Note that it marks whether or not it's already been run
	 * to avoid problems with repeating the request.
	 */

	public void run()
	{
		if ( this.limit < 1 || !this.canPurchase() || this.shopId == KoLCharacter.getUserId() )
			return;

		this.addFormField( this.isNPCStore ? "howmany" : "quantity", String.valueOf( this.limit ) );

		// If the item is not currently recognized, the user should
		// be notified that the purchases cannot be made because of that

		if ( this.itemId == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Item not recognized by KoLmafia database." );
			return;
		}

		// Check to make sure that the person is wearing the appropriate
		// outfit for making the purchase.

		if ( KoLCharacter.getAvailableMeat() < this.limit * this.price )
			return;

		if ( !this.ensureProperAttire() )
			return;

		// Now that everything's ensured, go ahead and execute the
		// actual purchase request.

		KoLmafia.updateDisplay( "Purchasing " + TradeableItemDatabase.getItemName( this.itemId ) +
			" (" + COMMA_FORMAT.format( this.limit ) + " @ " + COMMA_FORMAT.format( this.getPrice() ) + ")..." );

		this.initialCount = this.item.getCount( inventory );
		super.run();
	}

	public int compareTo( Object o )
	{
		return ( o == null || !( o instanceof MallPurchaseRequest ) ) ? 1 :
			this.compareTo( (MallPurchaseRequest) o );
	}

	public static void setUsePriceComparison( boolean usePriceComparison )
	{	MallPurchaseRequest.usePriceComparison = usePriceComparison;
	}

	public int compareTo( MallPurchaseRequest mpr )
	{
		if ( !usePriceComparison )
		{
			int nameComparison = this.itemName.compareToIgnoreCase( mpr.itemName );
			if ( nameComparison != 0 )
				return nameComparison;
		}

		if ( this.price != mpr.price )
			return this.price - mpr.price;

		if ( this.isNPCStore && !mpr.isNPCStore )
			return KoLCharacter.isHardcore() ? -1 : 1;

		if ( !this.isNPCStore && mpr.isNPCStore )
			return KoLCharacter.isHardcore() ? 1 : -1;

		if ( this.quantity != mpr.quantity )
			return mpr.quantity - this.quantity;

		return this.shopName.compareToIgnoreCase( mpr.shopName );
	}

	public boolean ensureProperAttire()
	{
		if ( !this.isNPCStore )
			return true;

		int neededOutfit = 0;

		if ( this.npcStoreId.equals( "b" ) )
		{
			neededOutfit = 1;
		}
		else if ( this.npcStoreId.equals( "g" ) )
		{
			neededOutfit = 5;
		}
		else if ( this.npcStoreId.equals( "h" ) )
		{
			neededOutfit = 2;
		}
		else
		{
			// Maybe you can put on some Travoltan Trousers to decrease the
			// cost of the purchase.

			if ( !KoLCharacter.canInteract() && !KoLCharacter.isHardcore() && inventory.contains( TROUSERS ) )
				(new EquipmentRequest( TROUSERS, KoLCharacter.PANTS )).run();

			return true;
		}

		// Only switch outfits if the person is not
		// currently wearing the outfit.

		if ( EquipmentDatabase.isWearingOutfit( neededOutfit ) )
			return true;

		if ( !EquipmentDatabase.hasOutfit( neededOutfit ) )
			return false;

		(new EquipmentRequest( EquipmentDatabase.getOutfit( neededOutfit ) )).run();
		return true;
	}

	public void processResults()
	{
		int quantityAcquired = this.item.getCount( inventory ) - this.initialCount;
		if ( quantityAcquired > 0 )
		{
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -1 * this.getPrice() * quantityAcquired ) );
			KoLCharacter.updateStatus();

			return;
		}

		int startIndex = this.responseText.indexOf( "<center>" );
		int stopIndex = this.responseText.indexOf( "</table>" );

		if ( startIndex == -1 || stopIndex == -1 )
		{
			KoLmafia.updateDisplay( "Store unavailable.  Skipping..." );
			return;
		}

		String result = this.responseText.substring( startIndex, stopIndex );

		// One error is that the item price changed, or the item
		// is no longer available because someone was faster at
		// purchasing the item.  If that's the case, just return
		// without doing anything; nothing left to do.

		if ( this.responseText.indexOf( "You can't afford" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Not enough funds." );
			return;
		}

		// Another thing to search for is to see if the person
		// swapped the price on the item, or you got a "failed
		// to yield" message.  In that case, you may wish to
		// re-attempt the purchase.

		if ( this.responseText.indexOf( "This store doesn't" ) != -1 || this.responseText.indexOf( "failed to yield" ) != -1 )
		{
			Matcher itemChangedMatcher = Pattern.compile( "<td valign=center><b>" + this.itemName + "</b> \\(([\\d,]+)\\) </td><td>([\\d,]+) Meat" ).matcher( result );

			if ( itemChangedMatcher.find() )
			{
				int limit = StaticEntity.parseInt( itemChangedMatcher.group(1) );
				int newPrice = StaticEntity.parseInt( itemChangedMatcher.group(2) );

				// If the item exists at a lower or equivalent
				// price, then you should re-attempt the purchase
				// of the item.

				if ( this.price >= newPrice )
				{
					KoLmafia.updateDisplay( "Failed to yield.  Attempting repurchase..." );
					(new MallPurchaseRequest( this.itemName, this.itemId, Math.min( limit, this.quantity ), this.shopId, this.shopName, newPrice, Math.min( limit, this.quantity ), true )).run();
				}
				else
				{
					// In the event of a price switch, give the
					// player the option to report it.

					KoLmafia.updateDisplay( "Price switch detected (#" + this.shopId + ").  Skipping..." );
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

		Matcher quantityMatcher = YIELD_PATTERN.matcher( result );

		if ( quantityMatcher.find() )
		{
			int limit = StaticEntity.parseInt( quantityMatcher.group(1) );
			int alreadyPurchased = StaticEntity.parseInt( quantityMatcher.group(2) );

			if ( limit != alreadyPurchased )
				(new MallPurchaseRequest( this.itemName, this.itemId, limit - alreadyPurchased, this.shopId, this.shopName, this.price, limit, true )).run();

			this.canPurchase = false;
			return;
		}
	}

	public boolean equals( Object o )
	{
		return o == null || !(o instanceof MallPurchaseRequest) ? false :
			this.shopName.equals( ((MallPurchaseRequest)o).shopName ) && this.itemId == ((MallPurchaseRequest)o).itemId;
	}

	public static boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "mallstore.php" ) && !urlString.startsWith( "store.php" ) && !urlString.startsWith( "galaktik.php" ) && !urlString.startsWith( "town_giftshop.php" ) )
			return false;

		String itemName = null;

		Matcher quantityMatcher = null;

		if ( urlString.startsWith( "mall" ) )
			quantityMatcher = SendMessageRequest.QUANTITY_PATTERN.matcher( urlString );
		else
			quantityMatcher = SendMessageRequest.HOWMANY_PATTERN.matcher( urlString );

		if ( !quantityMatcher.find() )
			return true;

		int quantity = StaticEntity.parseInt( quantityMatcher.group(1) );
		if ( quantity == 0 )
			quantity = 1;

		Matcher itemMatcher = SendMessageRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
			return true;

		String idString = itemMatcher.group(1);
		if ( urlString.startsWith( "mall" ) )
			idString = idString.substring( 0, idString.length() - 9 );

		int itemId = StaticEntity.parseInt( idString );
		itemName = TradeableItemDatabase.getItemName( itemId );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "buy " + quantity + " " + itemName );
		return true;
	}
}
