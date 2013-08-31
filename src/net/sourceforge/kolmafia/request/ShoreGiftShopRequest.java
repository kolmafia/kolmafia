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

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class ShoreGiftShopRequest
	extends CoinMasterRequest
{
	public static final String master = "The Shore, Inc. Gift Shop"; 

	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( ShoreGiftShopRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( ShoreGiftShopRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( ShoreGiftShopRequest.master );
	private static final Pattern SCRIP_PATTERN = Pattern.compile( "(\\d+) Shore Inc. Ship Trip Scrip" );
	public static final AdventureResult SHIP_TRIP_SCRIP = ItemPool.get( ItemPool.SHIP_TRIP_SCRIP, 1 );

	public static final CoinmasterData SHORE_GIFT_SHOP =
		new CoinmasterData(
			ShoreGiftShopRequest.master,
			ShoreGiftShopRequest.class,
			"shop.php?whichshop=shore",
			"Shore Inc. Ship Trip Scrip",
			"no Shore Inc. Ship Trip Scrip",
			false,
			ShoreGiftShopRequest.SCRIP_PATTERN,
			ShoreGiftShopRequest.SHIP_TRIP_SCRIP,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			ShoreGiftShopRequest.buyItems,
			ShoreGiftShopRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			ShoreGiftShopRequest.itemRows
			);

	public ShoreGiftShopRequest()
	{
		super( ShoreGiftShopRequest.SHORE_GIFT_SHOP );
	}

	public ShoreGiftShopRequest( final String action )
	{
		super( ShoreGiftShopRequest.SHORE_GIFT_SHOP, action );
		this.addFormField( "pwd" );
	}

	public ShoreGiftShopRequest( final String action, final int itemId, final int quantity )
	{
		super( ShoreGiftShopRequest.SHORE_GIFT_SHOP, action, itemId, quantity );
		this.addFormField( "pwd" );
	}

	public ShoreGiftShopRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public ShoreGiftShopRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	@Override
	public void processResults()
	{
		ShoreGiftShopRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = ShoreGiftShopRequest.SHORE_GIFT_SHOP;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.contains( "whichshop=shore" ) )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
			}

			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.contains( "whichshop=shore" ) )
		{
			return false;
		}

		CoinmasterData data = ShoreGiftShopRequest.SHORE_GIFT_SHOP;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
