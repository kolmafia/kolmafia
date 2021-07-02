/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

import java.util.Map;

public class FireworksShopRequest
	extends CoinMasterRequest
{
	public static final String master = "Clan Underground Fireworks Shop";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( FireworksShopRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( FireworksShopRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( FireworksShopRequest.master );

	public static final CoinmasterData FIREWORKS_SHOP =
		new CoinmasterData(
			FireworksShopRequest.master,
			"fwshop",
			FireworksShopRequest.class,
			null,
			null,
			false,
			null,
			null,
			null,
			FireworksShopRequest.itemRows,
			"shop.php?whichshop=fwshop",
			"buyitem",
			FireworksShopRequest.buyItems,
			FireworksShopRequest.buyPrices,
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
			)
		{
			@Override
			public final boolean canBuyItem( final int itemId )
			{
				switch ( itemId )
				{
				case ItemPool.FEDORA_MOUNTED_FOUNTAIN:
				case ItemPool.PORKPIE_MOUNTED_POPPER:
				case ItemPool.SOMBRERO_MOUNTED_SPARKLER:
					return Preferences.getBoolean( "_fireworksShopHatBought" ) == false;
				case ItemPool.CATHERINE_WHEEL:
				case ItemPool.ROCKET_BOOTS:
				case ItemPool.OVERSIZED_SPARKLER:
					return Preferences.getBoolean( "_fireworksShopEquipmentBought" ) == false;
				}
				return super.canBuyItem( itemId );
			}
		};;

	public FireworksShopRequest()
	{
		super( FireworksShopRequest.FIREWORKS_SHOP );
	}

	public FireworksShopRequest( final String action )
	{
		super( FireworksShopRequest.FIREWORKS_SHOP, action );
	}

	public FireworksShopRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( FireworksShopRequest.FIREWORKS_SHOP, buying, attachments );
	}

	public FireworksShopRequest( final boolean buying, final AdventureResult attachment )
	{
		super( FireworksShopRequest.FIREWORKS_SHOP, buying, attachment );
	}

	public FireworksShopRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( FireworksShopRequest.FIREWORKS_SHOP, buying, itemId, quantity );
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
		FireworksShopRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=fwshop" ) )
		{
			return;
		}

		Preferences.setBoolean( "_fireworksShopHatBought", !responseText.contains( "<b>Dangerous Hats" ) );
		Preferences.setBoolean( "_fireworksShopEquipmentBought", !responseText.contains( "<b>Explosive Equipment" ) );

		CoinmasterData data = FireworksShopRequest.FIREWORKS_SHOP;
		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static String accessible()
	{
		if ( Preferences.getBoolean( "_fireworksShop" ) )
		{
			return "Your clan doesn't have a fireworks shop underneath its VIP lounge.";
		}

		return null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=fwshop" ) )
		{
			return false;
		}

		CoinmasterData data = FireworksShopRequest.FIREWORKS_SHOP;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
