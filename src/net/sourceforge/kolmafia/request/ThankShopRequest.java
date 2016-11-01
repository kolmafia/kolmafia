/**
 * Copyright (c) 2005-2016, KoLmafia development team
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

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class ThankShopRequest
	extends CoinMasterRequest
{
	public static final String master = "A traveling Thanksgiving salesman";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( ThankShopRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( ThankShopRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( ThankShopRequest.master );
	private static final Pattern CASHEW_PATTERN = Pattern.compile( "([\\d,]+) cashews" );
	public static final AdventureResult CASHEW = ItemPool.get( ItemPool.CASHEW, 1 );

	public static final CoinmasterData CASHEW_STORE =
		new CoinmasterData(
			ThankShopRequest.master,
			"thankshop",
			ThankShopRequest.class,
			"cashew",
			null,
			false,
			ThankShopRequest.CASHEW_PATTERN,
			ThankShopRequest.CASHEW,
			null,
			ThankShopRequest.itemRows,
			"shop.php?whichshop=thankshop",
			"buyitem",
			ThankShopRequest.buyItems,
			ThankShopRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			);

	public ThankShopRequest()
	{
		super( ThankShopRequest.CASHEW_STORE );
	}

	public ThankShopRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( ThankShopRequest.CASHEW_STORE, buying, attachments );
	}

	public ThankShopRequest( final boolean buying, final AdventureResult attachment )
	{
		super( ThankShopRequest.CASHEW_STORE, buying, attachment );
	}

	public ThankShopRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( ThankShopRequest.CASHEW_STORE, buying, itemId, quantity );
	}

	@Override
	public void run()
	{
		if ( this.action != null )
		{
			this.addFormField( "pwd" );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		ThankShopRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=thankshop" ) )
		{
			return;
		}

		CoinmasterData data = ThankShopRequest.CASHEW_STORE;

		String action = GenericRequest.getAction( location );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, location, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=thankshop" ) )
		{
			return false;
		}

		CoinmasterData data = ThankShopRequest.CASHEW_STORE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
