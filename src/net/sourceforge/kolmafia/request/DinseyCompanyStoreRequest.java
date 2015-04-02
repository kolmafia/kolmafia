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
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

public class DinseyCompanyStoreRequest
	extends CoinMasterRequest
{
	public static final String master = "The Dinsey Company Store";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( DinseyCompanyStoreRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( DinseyCompanyStoreRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( DinseyCompanyStoreRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) FunFunds" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.FUNFUNDS, 1 );
	public static final CoinmasterData DINSEY_COMPANY_STORE =
		new CoinmasterData(
			DinseyCompanyStoreRequest.master,
			"DinsyStore",
			DinseyCompanyStoreRequest.class,
			"FunFunds",
			null,
			false,
			DinseyCompanyStoreRequest.TOKEN_PATTERN,
			DinseyCompanyStoreRequest.COIN,
			null,
			DinseyCompanyStoreRequest.itemRows,
			"shop.php?whichshop=landfillstore",
			"buyitem",
			DinseyCompanyStoreRequest.buyItems,
			DinseyCompanyStoreRequest.buyPrices,
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

	static
	{
		DINSEY_COMPANY_STORE.plural = "FunFunds";
	}

	public DinseyCompanyStoreRequest()
	{
		super( DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE );
	}

	public DinseyCompanyStoreRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE, buying, attachments );
	}

	public DinseyCompanyStoreRequest( final boolean buying, final AdventureResult attachment )
	{
		super( DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE, buying, attachment );
	}

	public DinseyCompanyStoreRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		DinseyCompanyStoreRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=landfillstore" ) )
		{
			return;
		}

		CoinmasterData data = DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=landfillstore" ) )
		{
			return false;
		}

		CoinmasterData data = DinseyCompanyStoreRequest.DINSEY_COMPANY_STORE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		return null;
	}
}
