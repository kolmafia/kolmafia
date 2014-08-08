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

public class BuffJimmyRequest
	extends CoinMasterRequest
{
	public static final String master = "Buff Jimmy's Souvenir Shop";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( BuffJimmyRequest.master );
	private static final Map<String, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( BuffJimmyRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( BuffJimmyRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) Beach Bucks" );
	public static final AdventureResult COIN = ItemPool.get( ItemPool.BEACH_BUCK, 1 );
	public static final CoinmasterData BUFF_JIMMY =
		new CoinmasterData(
			BuffJimmyRequest.master,
			"buff_jimmy",
			BuffJimmyRequest.class,
			"shop.php?whichshop=sbb_jimmy",
			"Beach Buck",
			null,
			false,
			BuffJimmyRequest.TOKEN_PATTERN,
			BuffJimmyRequest.COIN,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			BuffJimmyRequest.buyItems,
			BuffJimmyRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			BuffJimmyRequest.itemRows
			);

	public BuffJimmyRequest()
	{
		super( BuffJimmyRequest.BUFF_JIMMY );
	}

	public BuffJimmyRequest( final String action )
	{
		super( BuffJimmyRequest.BUFF_JIMMY, action );
	}

	public BuffJimmyRequest( final String action, final AdventureResult [] attachments )
	{
		super( BuffJimmyRequest.BUFF_JIMMY, action, attachments );
	}

	public BuffJimmyRequest( final String action, final AdventureResult attachment )
	{
		super( BuffJimmyRequest.BUFF_JIMMY, action, attachment );
	}

	public BuffJimmyRequest( final String action, final int itemId, final int quantity )
	{
		super( BuffJimmyRequest.BUFF_JIMMY, action, itemId, quantity );
	}

	public BuffJimmyRequest( final String action, final int itemId )
	{
		super( BuffJimmyRequest.BUFF_JIMMY, action, itemId );
	}

	@Override
	public void processResults()
	{
		BuffJimmyRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=sbb_jimmy" ) )
		{
			return;
		}

		CoinmasterData data = BuffJimmyRequest.BUFF_JIMMY;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=sbb_jimmy" ) )
		{
			return false;
		}

		CoinmasterData data = BuffJimmyRequest.BUFF_JIMMY;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		// Not yet implemented
		return null;
	}
}
