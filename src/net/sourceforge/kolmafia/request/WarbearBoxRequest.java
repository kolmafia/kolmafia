/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

public class WarbearBoxRequest
	extends CoinMasterRequest
{
	public static final String master = "Warbear Black Box";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( WarbearBoxRequest.master );
	private static final Map<String, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( WarbearBoxRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( WarbearBoxRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) warbear whosit" );
	public static final AdventureResult WHOSIT = ItemPool.get( ItemPool.WARBEAR_WHOSIT, 1 );
	public static final AdventureResult BLACKBOX = ItemPool.get( ItemPool.WARBEAR_BLACK_BOX, 1 );
	public static final CoinmasterData WARBEARBOX =
		new CoinmasterData(
			WarbearBoxRequest.master,
			"warbear",
			WarbearBoxRequest.class,
			"shop.php?whichshop=warbear",
			"warbear whosit",
			null,
			false,
			WarbearBoxRequest.TOKEN_PATTERN,
			WarbearBoxRequest.WHOSIT,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			WarbearBoxRequest.buyItems,
			WarbearBoxRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			WarbearBoxRequest.itemRows
			);

	public WarbearBoxRequest()
	{
		super( WarbearBoxRequest.WARBEARBOX );
	}

	public WarbearBoxRequest( final String action )
	{
		super( WarbearBoxRequest.WARBEARBOX, action );
	}

	public WarbearBoxRequest( final String action, final AdventureResult [] attachments )
	{
		super( WarbearBoxRequest.WARBEARBOX, action, attachments );
	}

	public WarbearBoxRequest( final String action, final AdventureResult attachment )
	{
		super( WarbearBoxRequest.WARBEARBOX, action, attachment );
	}

	public WarbearBoxRequest( final String action, final int itemId, final int quantity )
	{
		super( WarbearBoxRequest.WARBEARBOX, action, itemId, quantity );
	}

	public WarbearBoxRequest( final String action, final int itemId )
	{
		super( WarbearBoxRequest.WARBEARBOX, action, itemId );
	}

	@Override
	public void processResults()
	{
		WarbearBoxRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=warbear" ) )
		{
			return;
		}

		CoinmasterData data = WarbearBoxRequest.WARBEARBOX;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=warbear" ) )
		{
			return false;
		}

		CoinmasterData data = WarbearBoxRequest.WARBEARBOX;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		int wand = WarbearBoxRequest.BLACKBOX.getCount( KoLConstants.inventory );
		if ( wand == 0 )
		{
			return "You don't have a warbear black box";
		}
		return null;
	}
}
