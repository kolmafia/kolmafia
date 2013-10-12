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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

public abstract class PurchaseRequest
	extends GenericRequest
	implements Comparable<PurchaseRequest>
{
	public static final int MAX_QUANTITY = 16777215;

	protected static boolean usePriceComparison;
	protected String hashField;

	protected String shopName;
	public boolean isMallStore;

	protected AdventureResult item;
	protected int quantity;
	protected int price;
	protected int limit;

	protected boolean canPurchase;
	protected long timestamp;

	protected int initialCount;		// for detecting partial yields

	public PurchaseRequest( final String location )
	{
		super( location );
	}

	@Override
	public String getHashField()
	{
		return this.hashField;
	}

	public AdventureResult getItem()
	{
		return this.item;
	}

	public int getItemId()
	{
		return this.item.getItemId();
	}

	public String getItemName()
	{
		return this.item.getName();
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
	 * Retrieves the price of the item being purchased.
	 *
	 * @return The price of the item being purchased
	 */

	public int getPrice()
	{
		return this.price;
	}

	public String getPriceString()
	{
		return KoLConstants.COMMA_FORMAT.format( this.getPrice() );
	}

	public AdventureResult getCost()
	{
		return new AdventureResult( AdventureResult.MEAT, this.price );
	}

	public String getCurrency( final int count )
	{
		return "Meat";
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
	 * Converts this request into a readable string.
	 */

	@Override
	public String toString()
	{
		StringBuilder buffer = new StringBuilder();
		String color = this.color();

		buffer.append( "<html>" );
		if ( color != null )
		{
			buffer.append( "<nobr style=\"color:" );
			buffer.append( color );
			buffer.append( "\">" );
		}

		buffer.append( this.item.getName() );
		buffer.append( " (" );

		if ( this.quantity == PurchaseRequest.MAX_QUANTITY )
		{
			buffer.append( "unlimited" );
		}
		else if ( this.quantity < 0 )
		{
			buffer.append( "unknown" );
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
		buffer.append( this.getPriceString() );
		buffer.append( "): " );
		buffer.append( this.shopName );

		if ( color != null )
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

	public void setCanPurchase()
	{
		this.setCanPurchase( KoLCharacter.getAvailableMeat() >= this.price );
	}

	public boolean canPurchase()
	{
		return this.canPurchase && KoLCharacter.getAvailableMeat() >= this.price;
	}

	public String color()
	{
		return this.canPurchase && KoLCharacter.getAvailableMeat() >= this.price ? null : "gray";
	}

	public boolean canPurchaseIgnoringMeat()
	{
		return this.canPurchase;
	}

	public int affordableCount()
	{
		return KoLCharacter.getAvailableMeat() / this.price;
	}

	public boolean isAccessible()
	{
		return true;
	}

	/**
	 * Executes the purchase request. This calculates the number of items
	 * which will be purchased and adds it to the list. Note that it marks
	 * whether or not it's already been run to avoid problems with
	 * repeating the request.
	 */

	@Override
	public void run()
	{
		if ( this.limit < 1 || !this.canPurchase() )
		{
			return;
		}

		// If the item is not currently recognized, notify user. This
		// should never happen, since we automatically detect and
		// register new items in mall stores and in NPC stores.

		if ( this.item.getItemId() == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Item not present in KoLmafia database." );
			return;
		}

		// Make sure we have enough Meat to buy what we want.

		if ( KoLCharacter.getAvailableMeat() < this.limit * this.price )
		{
			return;
		}

		// Make sure we are wearing the appropriate outfit, if necessary

		if ( !this.ensureProperAttire() )
		{
			return;
		}

		// Now that we're ready, make the purchase!

		KoLmafia.updateDisplay( "Purchasing " + this.item.getName() + " (" + KoLConstants.COMMA_FORMAT.format( this.limit ) + " @ " + this.getPriceString() + ")..." );

		this.initialCount = this.item.getCount( KoLConstants.inventory );

		super.run();
	}

	public static final void setUsePriceComparison( final boolean usePriceComparison )
	{
		PurchaseRequest.usePriceComparison = usePriceComparison;
	}

	public int compareTo( final PurchaseRequest pr )
	{
		if ( pr == null )
		{
			return -1;
		}
		if ( !PurchaseRequest.usePriceComparison )
		{
			int nameComparison = this.item.getName().compareToIgnoreCase( pr.item.getName() );
			if ( nameComparison != 0 )
			{
				return nameComparison;
			}
		}

		if ( this.price != pr.price )
		{
			return this.price - pr.price;
		}

		if ( !this.isMallStore && pr.isMallStore )
		{
			return KoLCharacter.isHardcore() ? -1 : 1;
		}

		if ( this.isMallStore && !pr.isMallStore )
		{
			return KoLCharacter.isHardcore() ? 1 : -1;
		}

		// limit is how many items you can actually buy
		// sort high to low on limit

		if ( this.limit != pr.limit )
		{
			return pr.limit - this.limit;
		}

		// If limits are equal but quantity is not, one or the other
		// stores has an artificial limit. Reward those that don't do
		// that by sorting low to high on quantity.

		if ( this.quantity != pr.quantity )
		{
			return this.quantity - pr.quantity;
		}

		return this.shopName.compareToIgnoreCase( pr.shopName );
	}

	public boolean ensureProperAttire()
	{
		return true;
	}

	@Override
	public boolean equals( final Object o )
	{
		return o == null || !( o instanceof PurchaseRequest ) ? false : this.shopName.equals( ( (PurchaseRequest) o ).shopName )
				&& this.item.getItemId() == ( (PurchaseRequest) o ).item.getItemId();
	}

	@Override
	public int hashCode()
	{
		int hash = 0;
		hash += this.shopName != null ? this.shopName.hashCode() : 0;
		hash += this.item != null ? this.item.hashCode() : 0;
		return hash;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( urlString.startsWith( "mallstore.php" ) )
		{
			return MallPurchaseRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "store.php" ) ||
		     urlString.startsWith( "galaktik.php" ) ||
		     urlString.startsWith( "town_giftshop.php" ) )
		{
			return NPCPurchaseRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "shop.php" ) )
		{
			return NPCPurchaseRequest.registerShopRequest( urlString, false );
		}

		return false;
	}
}
