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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BlackMarketRequest
	extends CoinMasterRequest
{
	public static final String master = "The Black Market";
	public static final AdventureResult TOKEN =  ItemPool.get( ItemPool.PRICELESS_DIAMOND, 1 );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) priceless diamond" );
	public static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BlackMarketRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BlackMarketRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( BlackMarketRequest.master );

	public static final CoinmasterData BLACK_MARKET =
		new CoinmasterData(
			BlackMarketRequest.master,
			"blackmarket",
			BlackMarketRequest.class,
			"priceless diamond",
			null,
			false,
			BlackMarketRequest.TOKEN_PATTERN,
			BlackMarketRequest.TOKEN,
			null,
			BlackMarketRequest.itemRows,
			"shop.php?whichshop=blackmarket",
			"buyitem",
			BlackMarketRequest.buyItems,
			BlackMarketRequest.buyPrices,
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

	public BlackMarketRequest()
	{
		super( BlackMarketRequest.BLACK_MARKET );
	}

	public BlackMarketRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( BlackMarketRequest.BLACK_MARKET, buying, attachments );
	}

	public BlackMarketRequest( final boolean buying, final AdventureResult attachment )
	{
		super( BlackMarketRequest.BLACK_MARKET, buying, attachment );
	}

	public BlackMarketRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( BlackMarketRequest.BLACK_MARKET, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		BlackMarketRequest.parseResponse( this.getURLString(), responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=blackmarket" ) )
		{
			return;
		}

		CoinmasterData data = BlackMarketRequest.BLACK_MARKET;
		int itemId = CoinMasterRequest.extractItemId( data, location );

		if ( itemId == -1 )
		{
			// Purchase for Meat or a simple visit
			CoinMasterRequest.parseBalance( data, responseText );
			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static String accessible()
	{
		if ( !QuestLogRequest.isBlackMarketAvailable() )
		{
			return "The Black Market is not currently available";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString, final boolean noMeat )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=blackmarket" ) )
		{
			return false;
		}

		Matcher m = GenericRequest.WHICHROW_PATTERN.matcher( urlString );
		if ( !m.find() )
		{
			// Just a visit
			return true;
		}

		CoinmasterData data = BlackMarketRequest.BLACK_MARKET;
		int itemId = CoinMasterRequest.extractItemId( data, urlString );

		if ( itemId == -1 )
		{
			// Presumably this is a purchase for Meat.
			// If we've already checked Meat, this is an unknown item
			if ( noMeat )
			{
				return false;
			}
			return NPCPurchaseRequest.registerShopRequest( urlString, true );
		}

		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
