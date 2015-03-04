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
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class FreeSnackRequest
	extends CoinMasterRequest
{
	public static final String master = "Game Shoppe Snacks"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( FreeSnackRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( FreeSnackRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) free snack voucher" );
	private static final Pattern SNACK_PATTERN = Pattern.compile( "whichsnack=(\\d+)" );
	public static final AdventureResult VOUCHER = ItemPool.get( ItemPool.SNACK_VOUCHER, 1 );

	public static final CoinmasterData FREESNACKS =
		new CoinmasterData(
			FreeSnackRequest.master,
			"snacks",
			FreeSnackRequest.class,
			"snack voucher",
			"The teen glances at your snack voucher",
			true,
			FreeSnackRequest.TOKEN_PATTERN,
			FreeSnackRequest.VOUCHER,
			null,
			null,
			"gamestore.php",
			"buysnack",
			FreeSnackRequest.buyItems,
			FreeSnackRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichsnack",
			FreeSnackRequest.SNACK_PATTERN,
			null,
			null,
			null,
			null,
			true
			);

	public FreeSnackRequest()
	{
		super( FreeSnackRequest.FREESNACKS );
	}

	public FreeSnackRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( FreeSnackRequest.FREESNACKS, buying, attachments );
	}

	public FreeSnackRequest( final boolean buying, final AdventureResult attachment )
	{
		super( FreeSnackRequest.FREESNACKS, buying, attachment );
	}

	public FreeSnackRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( FreeSnackRequest.FREESNACKS, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		GameShoppeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseFreeSnackVisit( final String location, final String responseText )
	{
		if ( responseText.indexOf( "You acquire" ) != -1 )
		{
			CoinmasterData data = FreeSnackRequest.FREESNACKS;
			CoinMasterRequest.completePurchase( data, location );
		}
	}

	public static final void buy( final int itemId, final int count )
	{
		RequestThread.postRequest( new FreeSnackRequest( true, itemId, count ) );
	}

	public static String accessible()
	{
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim gamestore.php?action=buysnack
		if ( !urlString.startsWith( "gamestore.php" ) )
		{
			return false;
		}

		CoinmasterData data = FreeSnackRequest.FREESNACKS;
		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
