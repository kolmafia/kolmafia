/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

public class CoinMasterPurchaseRequest
	extends PurchaseRequest
{
	private CoinmasterData data;
	private AdventureResult cost;
	private String priceString;
	private CoinMasterRequest request;

	/**
	 * Constructs a new <code>CoinMasterPurchaseRequest</code> which retrieves things from Coin Masters.
	 */

	public CoinMasterPurchaseRequest( final CoinmasterData data, final AdventureResult item, final AdventureResult price )
	{
		super( "" );		// We do not run this request itself

		this.shopName = data.getMaster();
		this.isMallStore = false;
		this.item = item.getInstance( 1 );
		this.price = price.getCount();
		this.quantity = item.getCount();

		this.limit = this.quantity;
		this.canPurchase = true;

		this.timestamp = 0L;

		this.data = data;
		this.cost = price;
		this.priceString = KoLConstants.COMMA_FORMAT.format( this.price ) + " " + price.getPluralName( this.price );
		this.request = data.getRequest( true, new AdventureResult[] { this.item } );
	}

	public CoinmasterData getData()
	{
		return this.data;
	}

	@Override
	public String getPriceString()
	{
		return this.priceString;
	}

	@Override
	public AdventureResult getCost()
	{
		return this.cost;
	}

	@Override
	public String getCurrency( final long count )
	{
		return this.cost.getPluralName( this.price );
	}

	public int getTokenItemId()
	{
		return this.cost.getItemId();
	}

	@Override
	public int affordableCount()
	{
		int tokens = this.data.affordableTokens( this.cost );
		int price = this.price;
		return price == 0 ? 0 : tokens / price;
	}

	@Override
	public boolean canPurchase()
	{
		return this.canPurchase && this.data.isAccessible() && this.affordableCount() > 0;
	}

	@Override
	public void setCanPurchase()
	{
		this.setCanPurchase( this.data.isAccessible() && this.affordableCount() > 0 );
	}

	@Override
	public String color()
	{
		return this.canPurchase && this.affordableCount() > 0 ? null : "gray";
	}

	@Override
	public boolean isAccessible()
	{
		return this.data.isAccessible();
	}

	@Override
	public void run()
	{
		if ( this.request == null )
		{
			return;
		}

		if ( this.limit < 1 )
		{
			return;
		}

		// Make sure we have enough tokens to buy what we want.
		if ( this.data.availableTokens( this.cost ) < this.limit * this.price )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't afford that." );
			return;
		}

		// Make sure the Coin Master is accessible
		String message = this.data.accessible();
		if ( message != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, message );
			return;
		}

		// Now that we're ready, make the purchase!

		KoLmafia.updateDisplay( "Purchasing " + this.item.getName() + " (" + KoLConstants.COMMA_FORMAT.format( this.limit ) + " @ " + this.getPriceString() + ")..." );

		this.initialCount = this.item.getCount( KoLConstants.inventory );
		this.request.setQuantity( this.limit );

		RequestThread.postRequest( this.request );
	}
}
