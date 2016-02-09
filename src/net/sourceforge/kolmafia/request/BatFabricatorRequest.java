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

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.Limitmode;

public class BatFabricatorRequest
	extends CoinMasterRequest
{
	public static final String master = "Bat-Fabricator";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BatFabricatorRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BatFabricatorRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( BatFabricatorRequest.master );

	public static final AdventureResult METAL = ItemPool.get( ItemPool.HIGH_GRADE_METAL, 1 );
	public static final AdventureResult FIBERS = ItemPool.get( ItemPool.HIGH_TENSILE_STRENGTH_FIBERS, 1 );
	public static final AdventureResult EXPLOSIVES = ItemPool.get( ItemPool.HIGH_GRADE_EXPLOSIVES, 1 );

	public static final CoinmasterData BAT_FABRICATOR =
		new BatFabricatorCoinmasterData(
			BatFabricatorRequest.master,
			"Bat-Fabricator",
			BatFabricatorRequest.class,
			null,
			null,
			false,
			null,
			null,
			null,
			BatFabricatorRequest.itemRows,
			"shop.php?whichshop=batman_cave",
			"buyitem",
			BatFabricatorRequest.buyItems,
			BatFabricatorRequest.buyPrices,
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

	public BatFabricatorRequest()
	{
		super( BatFabricatorRequest.BAT_FABRICATOR );
	}

	public BatFabricatorRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( BatFabricatorRequest.BAT_FABRICATOR, buying, attachments );
	}

	public BatFabricatorRequest( final boolean buying, final AdventureResult attachment )
	{
		super( BatFabricatorRequest.BAT_FABRICATOR, buying, attachment );
	}

	public BatFabricatorRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( BatFabricatorRequest.BAT_FABRICATOR, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		BatFabricatorRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=batman_cave" ) )
		{
			return;
		}

		CoinmasterData data = BatFabricatorRequest.BAT_FABRICATOR;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=batman_cave" ) )
		{
			return false;
		}

		CoinmasterData data = BatFabricatorRequest.BAT_FABRICATOR;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( KoLCharacter.getLimitmode() != Limitmode.BATMAN )
		{
			return "Only Batfellow can use the Bat-Fabricator.";
		}
		// *** Only accessible if our current zone is Bat-Cave
		return null;
	}

	private static class BatFabricatorCoinmasterData
		extends CoinmasterData
	{
		public BatFabricatorCoinmasterData( 
			final String master,
			final String nickname,
			final Class requestClass,
			final String token,
			final String tokenTest,
			final boolean positiveTest,
			final Pattern tokenPattern,
			final AdventureResult item,
			final String property,
			final Map<Integer, Integer> itemRows,
			final String buyURL,
			final String buyAction,
			final LockableListModel<AdventureResult> buyItems,
			final Map<Integer, Integer> buyPrices,
			final String sellURL,
			final String sellAction,
			final LockableListModel<AdventureResult> sellItems,
			final Map<Integer, Integer> sellPrices,
			final String itemField,
			final Pattern itemPattern,
			final String countField,
			final Pattern countPattern,
			final String storageAction,
			final String tradeAllAction,
			final boolean canPurchase )
		{
			super( master, nickname, requestClass,
			       token, tokenTest, positiveTest, tokenPattern,
			       item, property, itemRows,
			       buyURL, buyAction, buyItems, buyPrices,
			       sellURL, sellAction, sellItems, sellPrices,
			       itemField, itemPattern,
			       countField, countPattern,
			       storageAction, tradeAllAction,
			       canPurchase );
		}

		@Override
		public AdventureResult itemBuyPrice( final int itemId )
		{
			// *** cost is 3 by default or 2 with Improved 3-D Bat-Printer
			int cost = 3;
			switch ( itemId )
			{
			case ItemPool.BAT_OOMERANG:
				return BatFabricatorRequest.METAL.getInstance( cost );
			case ItemPool.BAT_JUTE:
				return BatFabricatorRequest.FIBERS.getInstance( cost );
			case ItemPool.BAT_O_MITE:
				return BatFabricatorRequest.EXPLOSIVES.getInstance( cost );
			}
			return null;
		}
	}
}
