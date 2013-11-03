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
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

public class FreeSnackRequest
	extends CoinMasterRequest
{
	public static final String master = "Game Shoppe Snacks"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( FreeSnackRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( FreeSnackRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) free snack voucher" );
	private static final Pattern SNACK_PATTERN = Pattern.compile( "whichsnack=(\\d+)" );
	public static final AdventureResult VOUCHER = ItemPool.get( ItemPool.SNACK_VOUCHER, 1 );

	public static final CoinmasterData FREESNACKS =
		new CoinmasterData(
			FreeSnackRequest.master,
			FreeSnackRequest.class,
			"gamestore.php",
			"snack voucher",
			"The teen glances at your snack voucher",
			true,
			FreeSnackRequest.TOKEN_PATTERN,
			FreeSnackRequest.VOUCHER,
			null,
			"whichsnack",
			FreeSnackRequest.SNACK_PATTERN,
			null,
			null,
			"buysnack",
			FreeSnackRequest.buyItems,
			FreeSnackRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			null
			);

	public FreeSnackRequest()
	{
		super( FreeSnackRequest.FREESNACKS );
	}

	public FreeSnackRequest( final String action )
	{
		super( FreeSnackRequest.FREESNACKS, action );
	}

	public FreeSnackRequest( final String action, final int itemId, final int quantity )
	{
		super( FreeSnackRequest.FREESNACKS, action, itemId, quantity );
	}

	public FreeSnackRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public FreeSnackRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
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
			CoinmastersFrame.externalUpdate();
		}
	}

	public static final void  buy( final int itemId, final int count )
	{
		RequestThread.postRequest( new FreeSnackRequest( "buysnack", itemId, count ) );
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
