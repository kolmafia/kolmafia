/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

public class CoinMasterPurchaseRequest
	extends PurchaseRequest
{
	private CoinmasterData data;
	private CoinMasterRequest request;
	private String priceString;
	private AdventureResult cost;

	/**
	 * Constructs a new <code>CoinMasterPurchaseRequest</code> which retrieves things from Coin Masters.
	 */

	public CoinMasterPurchaseRequest( final CoinmasterData data, final int itemId, final int price, final int quantity )
	{
		super( "" );		// We do not run this request itself

		this.data = data;

		this.isMallStore = false;
		this.item = new AdventureResult( itemId, 1 );

		this.shopName = data.getMaster();

		this.quantity = quantity;
		this.price = price;

		AdventureResult item = data.getItem();
		String token = item != null ? item.getName() : data.getToken();
		String name = ( price != 1 ) ? data.getPluralToken() : token;
		this.priceString = KoLConstants.COMMA_FORMAT.format( this.price ) + " " + name;
		this.cost = AdventureResult.tallyItem( token, price, true );

		this.limit = this.quantity;
		this.canPurchase = true;

		this.timestamp = 0L;

		this.request = CoinMasterRequest.getRequest( data, data.getBuyAction(), this.item );
	}

	public CoinmasterData getData()
	{
		return this.data;
	}

	public String getPriceString()
	{
		return this.priceString;
	}

	public AdventureResult getCost()
	{
		return this.cost;
	}

	public String getCurrency( final int count )
	{
		String name = ( count != 1 ) ? data.getPluralToken() : data.getToken();
		return name;
	}

	public int getTokenItemId()
	{
		AdventureResult item = data.getItem();
		return item != null ? item.getItemId() : -1;
	}

	public int affordableCount()
	{
		int tokens = this.data.affordableTokens();
		int price = this.price;
		return tokens / price;
	}

	public boolean canPurchase()
	{
		CoinmasterData data = this.data;
		int tokens = data.affordableTokens();
		int price = this.price;
		return this.canPurchase && price <= tokens;
	}

	public void setCanPurchase()
	{
		CoinmasterData data = this.data;

		// If the Coin Master is "accessible" - which is up to the Coin
		// Master to determine - we can purchase from it.
		if ( CoinMasterRequest.accessible( data ) != null )
		{
			this.setCanPurchase( false );
			return;
		}

		// See if we can afford the items
		int tokens = data.affordableTokens();
		int price = this.price;
		this.setCanPurchase( tokens >= price );
	}

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
		if ( this.data.availableTokens() < this.limit * this.price )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't afford that." );
			return;
		}

		// Make sure the Coin Master is accessible
		String message = CoinMasterRequest.accessible( this.data );
		if ( message != null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, message );
			return;
		}

		// Now that we're ready, make the purchase!

		KoLmafia.updateDisplay( "Purchasing " + this.item.getName() + " (" + KoLConstants.COMMA_FORMAT.format( this.limit ) + " @ " + this.getPriceString() + ")..." );

		this.initialCount = this.item.getCount( KoLConstants.inventory );
		this.request.setQuantity( this.limit );

		RequestThread.postRequest( this.request );
	}
}
