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

public class NuggletCraftingRequest
	extends CoinMasterRequest
{
	public static final String master = "Topiary Nuggletcrafting";

	public static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( NuggletCraftingRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( NuggletCraftingRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( NuggletCraftingRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) topiary nugglet" );
	public static final AdventureResult TOPIARY_NUGGLET =  ItemPool.get( ItemPool.TOPIARY_NUGGLET, 1 );

	public static final CoinmasterData NUGGLETCRAFTING =
		new CoinmasterData(
			NuggletCraftingRequest.master,
			"NuggletCrafting",
			NuggletCraftingRequest.class,
			"topiary nugglet",
			"no topiary nugglets",
			false,
			NuggletCraftingRequest.TOKEN_PATTERN,
			NuggletCraftingRequest.TOPIARY_NUGGLET,
			null,
			NuggletCraftingRequest.itemRows,
			"shop.php?whichshop=topiary",
			"buyitem",
			NuggletCraftingRequest.buyItems,
			NuggletCraftingRequest.buyPrices,
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

	public NuggletCraftingRequest()
	{
		super( NuggletCraftingRequest.NUGGLETCRAFTING );
	}

	public NuggletCraftingRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( NuggletCraftingRequest.NUGGLETCRAFTING, buying, attachments );
	}

	public NuggletCraftingRequest( final boolean buying, final AdventureResult attachment )
	{
		super( NuggletCraftingRequest.NUGGLETCRAFTING, buying, attachment );
	}

	public NuggletCraftingRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( NuggletCraftingRequest.NUGGLETCRAFTING, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		NuggletCraftingRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "shop.php" ) || !location.contains( "whichshop=topiary" ) )
		{
			return;
		}

		CoinmasterData data = NuggletCraftingRequest.NUGGLETCRAFTING;

		String action = GenericRequest.getAction( location );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, location, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static String accessible()
	{
		if ( NuggletCraftingRequest.TOPIARY_NUGGLET.getCount( KoLConstants.inventory ) == 0 )
		{
			return "You do not have a topiary nugglet in inventory";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		// shop.php?pwd&whichshop=topiary
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=topiary" ) )
		{
			return false;
		}

		CoinmasterData data = NuggletCraftingRequest.NUGGLETCRAFTING;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
