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

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class Crimbo14Request
	extends CoinMasterRequest
{
	public static final String master = "Crimbo 2014"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( Crimbo14Request.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( Crimbo14Request.master );
	private static final LockableListModel<AdventureResult> sellItems = CoinmastersDatabase.getSellItems( Crimbo14Request.master );
	private static final Map<Integer, Integer> sellPrices = CoinmastersDatabase.getSellPrices( Crimbo14Request.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>(no|[\\d,]) Crimbo Credit", Pattern.DOTALL );
	public static final AdventureResult CRIMBO_CREDIT = ItemPool.get( ItemPool.CRIMBO_CREDIT, 1 );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( Crimbo14Request.master );
	public static final CoinmasterData CRIMBO14 =
		new CoinmasterData(
			Crimbo14Request.master,
			"crimbo14",
			Crimbo14Request.class,
			"Crimbo Credit",
			null,
			false,
			Crimbo14Request.TOKEN_PATTERN,
			Crimbo14Request.CRIMBO_CREDIT,
			null,
			Crimbo14Request.itemRows,
			"shop.php?whichshop=crimbo14",
			"buyitem",
			Crimbo14Request.buyItems,
			Crimbo14Request.buyPrices,
			"shop.php?whichshop=crimbo14turnin",
			"buyitem",
			Crimbo14Request.sellItems,
			Crimbo14Request.sellPrices,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			);

	public Crimbo14Request()
	{
		super( Crimbo14Request.CRIMBO14 );
	}

	public Crimbo14Request( final boolean buying, final AdventureResult [] attachments )
	{
		super( Crimbo14Request.CRIMBO14, buying, attachments );
	}

	public Crimbo14Request( final boolean buying, final AdventureResult attachment )
	{
		super( Crimbo14Request.CRIMBO14, buying, attachment );
	}

	public Crimbo14Request( final boolean buying, final int itemId, final int quantity )
	{
		super( Crimbo14Request.CRIMBO14, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		Crimbo14Request.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=crimbo14" ) )
		{
			return;
		}

		CoinmasterData data = Crimbo14Request.CRIMBO14;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=crimbo14" ) )
		{
			return false;
		}

		CoinmasterData data = Crimbo14Request.CRIMBO14;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		return null;
	}
}
