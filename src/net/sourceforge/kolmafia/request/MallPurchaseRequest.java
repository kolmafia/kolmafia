/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MallPurchaseRequest
	extends PurchaseRequest
{
	private static final Pattern YIELD_PATTERN =
		Pattern.compile( "You may only buy ([\\d,]+) of this item per day from this store\\.You have already purchased ([\\d,]+)" );
	private static Pattern MALLSTOREID_PATTERN = Pattern.compile( "whichstore\\d?=(\\d+)" );
	private static Pattern MEAT_PATTERN = Pattern.compile( "You spent ([\\d,]+) Meat" );

	private final int shopId;

	private int itemSequenceCount = 0;	// for detecting renamed items

	/**
	 * Constructs a new <code>MallPurchaseRequest</code> with the given
	 * values. Note that the only value which can be modified at a later
	 * time is the quantity of items being purchases; all others are
	 * consistent through the time when the purchase is actually executed.
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

	public MallPurchaseRequest( final int itemId, final int quantity, final int shopId,
				    final String shopName, final int price, final int limit,
				    final boolean canPurchase )
	{
		this( new AdventureResult( itemId, 1 ), quantity, shopId, shopName, price, limit, canPurchase );
	}

	public MallPurchaseRequest( final int itemId, final int quantity, final int shopId,
		final String shopName, final int price, final int limit )
	{
		this( new AdventureResult( itemId, 1 ), quantity, shopId, shopName, price, limit, true );
	}

	public MallPurchaseRequest( final AdventureResult item, final int quantity, final int shopId,
		final String shopName, final int price, final int limit, final boolean canPurchase )
	{
		super( "mallstore.php" );

		this.isMallStore = true;
		this.hashField = "pwd";
		this.item = item;

		this.shopName = shopName;
		this.shopId = shopId;

		this.quantity = quantity;
		this.price = price;
		this.limit = Math.min( quantity, limit );
		this.canPurchase = canPurchase;

		this.addFormField( "whichstore", String.valueOf( shopId ) );
		this.addFormField( "buying", "1" );
		this.addFormField( "ajax", "1" );
		this.addFormField( "whichitem", MallPurchaseRequest.getStoreString( item.getItemId(), price ) );

		this.timestamp = System.currentTimeMillis();
	}

	public static final String getStoreString( final int itemId, final int price )
	{
		// whichitem=2272000000246

		StringBuffer whichItem = new StringBuffer();
		whichItem.append( itemId );

		int originalLength = whichItem.length();
		whichItem.append( price );

		while ( whichItem.length() < originalLength + 9 )
		{
			whichItem.insert( originalLength, '0' );
		}

		return whichItem.toString();
	}

	@Override
	public void run()
	{
		if ( this.shopId == KoLCharacter.getUserId() )
		{
			return;
		}

		this.itemSequenceCount = ResultProcessor.itemSequenceCount;
		this.addFormField( "quantity", String.valueOf( this.limit ) );

		super.run();
	}


	@Override
	public void processResults()
	{
		MallPurchaseRequest.parseResponse( this.getURLString(), this.responseText );

		int quantityAcquired = this.item.getCount( KoLConstants.inventory ) - this.initialCount;
		if ( quantityAcquired > 0 )
		{
			return;
		}

		if ( this.itemSequenceCount != ResultProcessor.itemSequenceCount )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR,
				"Wrong item received - possibly its name or plural has changed." );
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
			KoLmafia.updateDisplay( MafiaState.ERROR, "Not enough funds." );
			return;
		}

		// If you are on a player's ignore list, you can't buy from his store

		if ( this.responseText.indexOf( "That player will not sell to you" ) != -1 )
		{
			KoLmafia.updateDisplay( "You are on this shop's ignore list (#" + this.shopId + "). Skipping..." );
			RequestLogger.updateSessionLog( "You are on this shop's ignore list (#" + this.shopId + "). Skipping...");
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
					"<td valign=center><b>" + this.item.getName() + "</b> \\(([\\d,]+)\\) </td><td>([\\d,]+) Meat" ).matcher(
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
						this.item,
						Math.min( limit, this.quantity ),
						this.shopId, this.shopName,
						newPrice, Math.min( limit, this.quantity ),
						true ) ).run();
				}
				else
				{
					KoLmafia.updateDisplay( "Price switch detected (#" + this.shopId + "). Skipping..." );
				}
			}
			else
			{
				KoLmafia.updateDisplay( "Failed to yield. Skipping..." );
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
					this.item,
					limit - alreadyPurchased,
					this.shopId, this.shopName,
					this.price, limit, true ) ).run();
			}

			this.canPurchase = false;
			return;
		}
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "mallstore.php" ) )
		{
			return;
		}

		Matcher meatMatcher = MallPurchaseRequest.MEAT_PATTERN.matcher( responseText );
		if ( !meatMatcher.find() )
		{
			return;
		}

		int cost = StringUtilities.parseInt( meatMatcher.group( 1 ) );

		ResultProcessor.processMeat( -cost );
		KoLCharacter.updateStatus();
	}

	public static final boolean registerRequest( final String urlString )
	{
		// mallstore.php?whichstore=294980&buying=1&ajax=1&whichitem=2272000000246&quantity=9

		if ( !urlString.startsWith( "mallstore.php" ) )
		{
			return false;
		}

		Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}

		Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher( urlString );
		if ( !quantityMatcher.find() )
		{
			return true;
		}

		int quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );

		// whichitem=2272000000246
		// the last 9 characters of idString are the price, with leading zeros
		String idString = itemMatcher.group( 1 );
		int idStringLength = idString.length();
		String priceString = idString.substring(idStringLength - 9, idStringLength);
		idString = idString.substring( 0, idStringLength - 9 );

		// In a perfect world where I was not so lazy, I'd verify that
		// the price string was really an int and might find another
		// way to effectively strip leading zeros from the display

		int priceVal = StringUtilities.parseInt( priceString );
		int itemId = StringUtilities.parseInt( idString );
		String itemName = ItemDatabase.getItemName( itemId );

		// store ID is embedded in the URL.  Extract it and get
		// the store name for logging

		Matcher m = MallPurchaseRequest.MALLSTOREID_PATTERN.matcher(urlString);
		String storeName = m.find() ? m.group(1) : "a PC store";

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "buy " + quantity + " " + itemName + " for " + priceVal +
			" each from " + storeName + " on " + KoLConstants.DAILY_FORMAT.format( new Date() ) );

		return true;
	}
}
