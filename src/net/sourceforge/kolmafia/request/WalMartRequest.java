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

public class WalMartRequest
	extends CoinMasterRequest
{
	public static final String master = "Wal-Mart";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( WalMartRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( WalMartRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( WalMartRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Wal-Mart gift certificates" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.WALMART_GIFT_CERTIFICATE, 1 );
	public static final CoinmasterData WALMART =
		new CoinmasterData(
			WalMartRequest.master,
			"Wal-Mart",
			WalMartRequest.class,
			"Wal-Mart gift certificate",
			null,
			false,
			WalMartRequest.TOKEN_PATTERN,
			WalMartRequest.COIN,
			null,
			WalMartRequest.itemRows,
			"shop.php?whichshop=glaciest",
			"buyitem",
			WalMartRequest.buyItems,
			WalMartRequest.buyPrices,
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

	public WalMartRequest()
	{
		super( WalMartRequest.WALMART );
	}

	public WalMartRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( WalMartRequest.WALMART, buying, attachments );
	}

	public WalMartRequest( final boolean buying, final AdventureResult attachment )
	{
		super( WalMartRequest.WALMART, buying, attachment );
	}

	public WalMartRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( WalMartRequest.WALMART, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		WalMartRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=glaciest" ) )
		{
			return;
		}

		CoinmasterData data = WalMartRequest.WALMART;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=glaciest" ) )
		{
			return false;
		}

		CoinmasterData data = WalMartRequest.WALMART;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		return null;
	}
}
