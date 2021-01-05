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

import java.util.Map;

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public class RubeeRequest
	extends CoinMasterRequest
{
	public static final String master = "FantasyRealm Rubee&trade; Store";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems(RubeeRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices(RubeeRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows(RubeeRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Rubees&trade;" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.RUBEE, 1 );
	public static final CoinmasterData RUBEE =
		new CoinmasterData(
			RubeeRequest.master,
			"FantasyRealm Store",
			RubeeRequest.class,
			"Rubee&trade;",
			null,
			false,
			RubeeRequest.TOKEN_PATTERN,
			RubeeRequest.COIN,
			null,
			RubeeRequest.itemRows,
			"shop.php?whichshop=fantasyrealm",
			"buyitem",
			RubeeRequest.buyItems,
			RubeeRequest.buyPrices,
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

	public RubeeRequest()
	{
		super(RubeeRequest.RUBEE );
	}

	public RubeeRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super(RubeeRequest.RUBEE, buying, attachments );
	}

	public RubeeRequest( final boolean buying, final AdventureResult attachment )
	{
		super(RubeeRequest.RUBEE, buying, attachment );
	}

	public RubeeRequest( final boolean buying, final int itemId, final int quantity )
	{
		super(RubeeRequest.RUBEE, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		RubeeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=fantasyrealm" ) )
		{
			return;
		}

		CoinmasterData data = RubeeRequest.RUBEE;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=fantasyrealm" ) )
		{
			return false;
		}

		CoinmasterData data = RubeeRequest.RUBEE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		return Preferences.getBoolean( "_frToday" ) || Preferences.getBoolean( "frAlways" ) ? null : "Need access to Fantasy Realm";
	}

	public void equip()
	{
		if ( !KoLCharacter.hasEquipped( ItemPool.FANTASY_REALM_GEM ) )
		{
			EquipmentRequest request = new EquipmentRequest( ItemPool.get( ItemPool.FANTASY_REALM_GEM, 1 ), EquipmentManager.ACCESSORY3 );
			RequestThread.postRequest( request );
		}
	}
}
