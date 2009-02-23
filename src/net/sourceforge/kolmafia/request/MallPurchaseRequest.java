/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MallPurchaseRequest
	extends GenericRequest
	implements Comparable
{
	private static final AdventureResult TROUSERS = new AdventureResult( 1792, 1 );
	private static final Pattern YIELD_PATTERN =
		Pattern.compile( "You may only buy ([\\d,]+) of this item per day from this store\\.You have already purchased ([\\d,]+)" );
	public static final Pattern PIRATE_EPHEMERA_PATTERN =
		Pattern.compile( "pirate (?:brochure|pamphlet|tract)" );

	private static Pattern MALLSTOREID_PATTERN = Pattern.compile( "whichstore\\d?=(\\d+)" );
	private static Pattern NPCSTOREID_PATTERN = Pattern.compile( "whichstore=([^&]*)" );

	private static boolean usePriceComparison;

	// In order to prevent overflows from happening, make
	// it so that the maximum quantity is 10 million

	private String itemName;
	final String shopName;
	private final int itemId, shopId;
	private int quantity;
	final int price;
	private int limit;

	private final AdventureResult item;
	private int initialCount;

	private String hashField;
	private boolean isNPCStore;
	private String npcStoreId;

	private boolean canPurchase;
	public static final int MAX_QUANTITY = 16777215;
	private int itemSequenceCount = 0;	// for detecting renamed items
	private long timestamp;

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> which retrieves things from NPC stores.
	 */

	public MallPurchaseRequest( final String storeName, final String storeId, final int itemId, final int price )
	{
		super( storeId.indexOf( "." ) == -1 ? "store.php" : storeId );

		this.hashField = "pwd";

		if ( storeId.indexOf( "." ) == -1 )
		{
			this.addFormField( "whichstore", storeId );
			this.addFormField( "buying", "Yep." );
			this.hashField = "phash";
		}
		else if ( storeId.equals( "galaktik.php" ) )
		{
			// Annoying special case.
			this.addFormField( "action", "buyitem" );
		}
		else
		{
			this.addFormField( "action", "buy" );
		}

		this.addFormField( "whichitem", String.valueOf( itemId ) );

		this.itemName = ItemDatabase.getItemName( itemId );
		this.shopName = storeName;
		this.itemId = itemId;
		this.shopId = 0;
		this.quantity = MallPurchaseRequest.MAX_QUANTITY;
		this.limit = this.quantity;
		this.price = price;

		this.item = new AdventureResult( this.itemId, 1 );

		this.isNPCStore = true;
		this.npcStoreId = storeId;
		this.canPurchase = true;
		this.timestamp = 0L;
	}

	public String getHashField()
	{
		return this.hashField;
	}

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> with the given values. Note that the only value which can be
	 * modified at a later time is the quantity of items being purchases; all others are consistent through the time
	 * when the purchase is actually executed.
	 *
	 * @param itemName The name of the item to be purchased
	 * @param itemId The database Id for the item to be purchased
	 * @param quantity The quantity of items to be purchased
	 * @param shopId The integer identifier for the shop from which the item will be purchased
	 * @param shopName The name of the shop
	 * @param price The price at which the item will be purchased
	 * @param limit The maximum number of items that can be purchased per day
	 * @param canPurchase Whether or not this purchase request is possible
	 */

	public MallPurchaseRequest( final String itemName, final int itemId, final int quantity, final int shopId,
		final String shopName, final int price, final int limit, final boolean canPurchase )
	{
		super( "mallstore.php" );

		this.itemId = itemId;

		if ( ItemDatabase.getItemName( itemId ) == null && itemName != null )
		{
			ItemDatabase.registerItem( itemId, itemName );
		}

		this.itemName = ItemDatabase.getItemName( this.itemId );
		if ( this.itemName == null )
		{
			this.itemName = "(unknown)";
		}

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

		this.addFormField( "whichitem", MallPurchaseRequest.getStoreString( itemId, price ) );

		this.item = new AdventureResult( this.itemId, 1 );
		this.timestamp = new Date().getTime();
	}

	public static final String getStoreString( final int itemId, final int price )
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
		{
			whichItem.insert( originalLength, '0' );
		}

		return whichItem.toString();
	}

	public int getItemId()
	{
		return this.itemId;
	}

	public String getStoreId()
	{
		return this.isNPCStore ? this.npcStoreId : String.valueOf( this.shopId );
	}

	public String getShopName()
	{
		return this.shopName;
	}

	public long getTimestamp()
	{
		return this.timestamp;
	}

	/**
	 * Retrieves the name of the item being purchased.
	 *
	 * @return The name of the item being purchased
	 */

	public String getItemName()
	{
		return this.itemName;
	}

	/**
	 * Retrieves the price of the item being purchased.
	 *
	 * @return The price of the item being purchased
	 */

	public int getPrice()
	{
		return !this.isNPCStore || !EquipmentManager.getEquipment( EquipmentManager.PANTS ).equals(
			MallPurchaseRequest.TROUSERS ) ? this.price : (int) ( this.price * 0.95f );
	}

	/**
	 * Retrieves the quantity of the item available in the store.
	 *
	 * @return The quantity of the item in the store
	 */

	public int getQuantity()
	{
		return this.quantity;
	}

	/**
	 * Sets the quantity of the item available in the store.
	 *
	 * @param quantity The quantity of the item available in the store.
	 */

	public void setQuantity( final int quantity )
	{
		this.quantity = quantity;
	}

	/**
	 * Retrieves the quantity of the item being purchased.
	 *
	 * @return The quantity of the item being purchased
	 */

	public int getLimit()
	{
		return this.limit;
	}

	/**
	 * Sets the maximum number of items that can be purchased through this request.
	 *
	 * @param limit The maximum number of items to be purchased with this request
	 */

	public void setLimit( final int limit )
	{
		this.limit = Math.min( this.quantity, limit );
	}

	/**
	 * Converts this request into a readable string. This is useful for debugging and as a temporary substitute for a
	 * list panel, in the event that a suitable list cell renderer has not been created.
	 */

	public String toString()
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append( "<html><nobr>" );
		if ( !this.canPurchase() )
		{
			buffer.append( "<font color=gray>" );
		}

		buffer.append( this.itemName );
		buffer.append( " (" );

		if ( this.quantity == MallPurchaseRequest.MAX_QUANTITY )
		{
			buffer.append( "unlimited" );
		}
		else
		{
			buffer.append( KoLConstants.COMMA_FORMAT.format( this.quantity ) );

			if ( this.limit < this.quantity || !this.canPurchase() )
			{
				buffer.append( " limit " );
				buffer.append( KoLConstants.COMMA_FORMAT.format( this.limit ) );
			}
		}

		buffer.append( " @ " );
		buffer.append( KoLConstants.COMMA_FORMAT.format( this.price ) );
		buffer.append( "): " );
		buffer.append( this.shopName );

		if ( !this.canPurchase() )
		{
			buffer.append( "</font>" );
		}

		buffer.append( "</nobr></html>" );

		return buffer.toString();
	}

	public void setCanPurchase( final boolean canPurchase )
	{
		this.canPurchase = canPurchase;
	}

	public boolean canPurchase()
	{
		return this.canPurchase && KoLCharacter.getAvailableMeat() >= this.price;
	}

	/**
	 * Executes the purchase request. This calculates the number of items which will be purchased and adds it to the
	 * list. Note that it marks whether or not it's already been run to avoid problems with repeating the request.
	 */

	public void run()
	{
		if ( this.limit < 1 || !this.canPurchase() || this.shopId == KoLCharacter.getUserId() )
		{
			return;
		}

		this.addFormField( this.isNPCStore ? "howmany" : "quantity", String.valueOf( this.limit ) );

		// If the item is not currently recognized, the user should
		// be notified that the purchases cannot be made because of that

		if ( this.itemId == -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Item not recognized by KoLmafia database." );
			return;
		}

		// Check to make sure that the person is wearing the appropriate
		// outfit for making the purchase.

		if ( KoLCharacter.getAvailableMeat() < this.limit * this.price )
		{
			return;
		}

		if ( !this.ensureProperAttire() )
		{
			return;
		}

		// Now that everything's ensured, go ahead and execute the
		// actual purchase request.

		KoLmafia.updateDisplay( "Purchasing " + ItemDatabase.getItemName( this.itemId ) + " (" + KoLConstants.COMMA_FORMAT.format( this.limit ) + " @ " + KoLConstants.COMMA_FORMAT.format( this.getPrice() ) + ")..." );

		this.initialCount = this.item.getCount( KoLConstants.inventory );
		this.itemSequenceCount = ResultProcessor.itemSequenceCount;
		super.run();
	}

	public int compareTo( final Object o )
	{
		return o == null || !( o instanceof MallPurchaseRequest ) ? 1 : this.compareTo( (MallPurchaseRequest) o );
	}

	public static final void setUsePriceComparison( final boolean usePriceComparison )
	{
		MallPurchaseRequest.usePriceComparison = usePriceComparison;
	}

	public int compareTo( final MallPurchaseRequest mpr )
	{
		if ( !MallPurchaseRequest.usePriceComparison )
		{
			int nameComparison = this.itemName.compareToIgnoreCase( mpr.itemName );
			if ( nameComparison != 0 )
			{
				return nameComparison;
			}
		}

		if ( this.price != mpr.price )
		{
			return this.price - mpr.price;
		}

		if ( this.isNPCStore && !mpr.isNPCStore )
		{
			return KoLCharacter.isHardcore() ? -1 : 1;
		}

		if ( !this.isNPCStore && mpr.isNPCStore )
		{
			return KoLCharacter.isHardcore() ? 1 : -1;
		}

		if ( this.quantity != mpr.quantity )
		{
			return mpr.quantity - this.quantity;
		}

		return this.shopName.compareToIgnoreCase( mpr.shopName );
	}

	public boolean ensureProperAttire()
	{
		if ( !this.isNPCStore )
		{
			return true;
		}

		int neededOutfit = 0;

		if ( this.npcStoreId.equals( "b" ) )
		{
			neededOutfit = 1;
		}
		else if ( this.npcStoreId.equals( "g" ) )
		{
			neededOutfit = 5;
		}
		else if ( this.npcStoreId.equals( "r" ) )
		{
			if ( !KoLCharacter.hasEquipped( ItemPool.get( ItemPool.PIRATE_FLEDGES, 1 ) ) )
			{
				neededOutfit = 9;
			}
		}
		else if ( this.npcStoreId.equals( "h" ) )
		{
			if ( this.shopName.equals( "Hippy Store (Pre-War)" ) )
			{
				neededOutfit = 2;
			}
			else if ( QuestLogRequest.isHippyStoreAvailable() )
			{
				neededOutfit = 0;
			}
			else if ( this.shopName.equals( "Hippy Store (Hippy)" ) )
			{
				neededOutfit = 32;
			}
			else if ( this.shopName.equals( "Hippy Store (Fratboy)" ) )
			{
				neededOutfit = 33;
			}
		}

		if ( neededOutfit == 0 )
		{
			// Maybe you can put on some Travoltan Trousers to
			// decrease the cost of the purchase.

			if ( !KoLCharacter.canInteract() &&
			     !KoLCharacter.isHardcore() &&
			     KoLConstants.inventory.contains( MallPurchaseRequest.TROUSERS ) )
			{
				( new EquipmentRequest( MallPurchaseRequest.TROUSERS, EquipmentManager.PANTS ) ).run();
			}

			return true;
		}

		// Only switch outfits if the person is not
		// currently wearing the outfit.

		if ( EquipmentManager.isWearingOutfit( neededOutfit ) )
		{
			return true;
		}

		if ( !EquipmentManager.hasOutfit( neededOutfit ) )
		{
			return false;
		}

		( new EquipmentRequest( EquipmentDatabase.getOutfit( neededOutfit ) ) ).run();
		return true;
	}

	public void processResults()
	{
		MallPurchaseRequest.parseResponse( this.getURLString(), this.responseText );

		int quantityAcquired = this.item.getCount( KoLConstants.inventory ) - this.initialCount;
		if ( quantityAcquired > 0 )
		{
			ResultProcessor.processResult(
				new AdventureResult( AdventureResult.MEAT, -1 * this.getPrice() * quantityAcquired ) );
			KoLCharacter.updateStatus();

			return;
		}
		
		if ( this.itemSequenceCount != ResultProcessor.itemSequenceCount )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
				"Wrong item received - possibly its name has changed." );
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
		// purchasing the item.	 If that's the case, just return
		// without doing anything; nothing left to do.

		if ( this.responseText.indexOf( "You can't afford" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Not enough funds." );
			return;
		}

		// Another thing to search for is to see if the person
		// swapped the price on the item, or you got a "failed
		// to yield" message.  In that case, you may wish to
		// re-attempt the purchase.

		if ( this.responseText.indexOf( "This store doesn't" ) != -1 || this.responseText.indexOf( "failed to yield" ) != -1 )
		{
			Matcher itemChangedMatcher =
				Pattern.compile(
					"<td valign=center><b>" + this.itemName + "</b> \\(([\\d,]+)\\) </td><td>([\\d,]+) Meat" ).matcher(
					result );

			if ( itemChangedMatcher.find() )
			{
				int limit = StringUtilities.parseInt( itemChangedMatcher.group( 1 ) );
				int newPrice = StringUtilities.parseInt( itemChangedMatcher.group( 2 ) );

				// If the item exists at a lower or equivalent
				// price, then you should re-attempt the purchase
				// of the item.

				if ( this.price >= newPrice )
				{
					KoLmafia.updateDisplay( "Failed to yield.  Attempting repurchase..." );
					( new MallPurchaseRequest(
						this.itemName, this.itemId, Math.min( limit, this.quantity ), this.shopId, this.shopName,
						newPrice, Math.min( limit, this.quantity ), true ) ).run();
				}
				else
				{
					KoLmafia.updateDisplay( "Price switch detected (#" + this.shopId + ").	Skipping..." );
				}
			}
			else
			{
				KoLmafia.updateDisplay( "Failed to yield.  Skipping..." );
			}

			return;
		}

		// One error that might be encountered is that the user
		// already purchased the item; if that's the case, and
		// the user hasn't exhausted their limit, then make a
		// second request to the server containing the correct
		// number of items to buy.

		Matcher quantityMatcher = MallPurchaseRequest.YIELD_PATTERN.matcher( result );

		if ( quantityMatcher.find() )
		{
			int limit = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			int alreadyPurchased = StringUtilities.parseInt( quantityMatcher.group( 2 ) );

			if ( limit != alreadyPurchased )
			{
				( new MallPurchaseRequest(
					this.itemName, this.itemId, limit - alreadyPurchased, this.shopId, this.shopName, this.price,
					limit, true ) ).run();
			}

			this.canPurchase = false;
			return;
		}
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "store.php" ) )
		{
			return;
		}

		Matcher m = MallPurchaseRequest.NPCSTOREID_PATTERN.matcher(urlString);
		if ( !m.find() )
		{
			return;
		}

		String storeId = m.group(1);

		if ( storeId.equals( "r" ) )
		{
			m = PIRATE_EPHEMERA_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				Preferences.setInteger( "lastPirateEphemeraReset", KoLCharacter.getAscensions() );
				Preferences.setString( "lastPirateEphemera", m.group( 0 ) );
			}
		}
	}

	public boolean equals( final Object o )
	{
		return o == null || !( o instanceof MallPurchaseRequest ) ? false : this.shopName.equals( ( (MallPurchaseRequest) o ).shopName ) && this.itemId == ( (MallPurchaseRequest) o ).itemId;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "mallstore.php" ) && !urlString.startsWith( "store.php" ) && !urlString.startsWith( "galaktik.php" ) && !urlString.startsWith( "town_giftshop.php" ) )
		{
			return false;
		}

		String priceString = null;
		int priceVal = 0;
		boolean isMall = false;
		Matcher quantityMatcher = null;

		if ( urlString.startsWith( "mall" ) )
		{
			isMall = true;
			quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher( urlString );
		}
		else
		{
			quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher( urlString );
		}

		if ( !quantityMatcher.find() )
		{
			return true;
		}

		String quantityString = quantityMatcher.group( 1 );

		int quantity = 0;

		if ( quantityString.length() < 8 )
		{
			quantity = Math.min( MallPurchaseRequest.MAX_QUANTITY, StringUtilities.parseInt( quantityString ) );
		}

		if ( quantity == 0 )
		{
			quantity = 1;
		}

		Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}

		String idString = itemMatcher.group( 1 );
		String storeName;

		if ( isMall )
		{
			// the last 9 characters of idString are the price,
			// with leading zeros
			int idStringLength = idString.length();
			priceString = idString.substring(idStringLength - 9, idStringLength);
			idString = idString.substring( 0, idStringLength - 9 );

			// store ID is embedded in the URL.  Extract it and get
			// the store name for logging

			Matcher m = MallPurchaseRequest.MALLSTOREID_PATTERN.matcher(urlString);
			storeName = m.find() ? m.group(1) : "a PC store";
		}
		else
		{
			Matcher m = MallPurchaseRequest.NPCSTOREID_PATTERN.matcher(urlString);
			String storeId = m.find() ? NPCStoreDatabase.getStoreName( m.group(1) ) : null;
			storeName = storeId != null ? storeId : "an NPC Store";
		}

		// in a perfect world where I was not so lazy, I'd verify that
		// the price string was really an int and might find another
		// way to effectively strip leading zeros from the display

		priceVal = StringUtilities.parseInt( priceString);
		int itemId = StringUtilities.parseInt( idString );
		String itemName = ItemDatabase.getItemName( itemId );

		RequestLogger.updateSessionLog();
		if (isMall)
		{
			RequestLogger.updateSessionLog( "buy " + quantity + " " + itemName + " for " + priceVal +
			" each from " + storeName + " on " + KoLConstants.DAILY_FORMAT.format( new Date() ) );
		}
		else
		{
			RequestLogger.updateSessionLog( "buy " + quantity + " " + itemName + " at market price from " + storeName);
		}
		return true;
	}
}
