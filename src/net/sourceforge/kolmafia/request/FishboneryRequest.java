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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FishboneryRequest
	extends CoinMasterRequest
{
	public static final String master = "Freshwater Fishbonery";

	public static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( FishboneryRequest.master );
	private static final Map<String, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( FishboneryRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( FishboneryRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) freshwater fishbone" );
	public static final AdventureResult FRESHWATER_FISHBONE =  ItemPool.get( ItemPool.FRESHWATER_FISHBONE, 1 );

	public static final CoinmasterData FISHBONERY =
		new CoinmasterData(
			FishboneryRequest.master,
			"Fishbonery",
			FishboneryRequest.class,
			"shop.php?whichshop=fishbones",
			"freshwater fishbone",
			"no freshwater fishbones",
			false,
			FishboneryRequest.TOKEN_PATTERN,
			FishboneryRequest.FRESHWATER_FISHBONE,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			FishboneryRequest.buyItems,
			FishboneryRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			FishboneryRequest.itemRows
			);

	public FishboneryRequest()
	{
		super( FishboneryRequest.FISHBONERY );
	}

	public FishboneryRequest( final String action, final AdventureResult [] attachments )
	{
		super( FishboneryRequest.FISHBONERY, action, attachments );
	}

	public FishboneryRequest( final String action, final AdventureResult attachment )
	{
		super( FishboneryRequest.FISHBONERY, action, attachment );
	}

	public FishboneryRequest( final String action, final int itemId, final int quantity )
	{
		super( FishboneryRequest.FISHBONERY, action, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		FishboneryRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "shop.php" ) || !location.contains( "whichshop=fishbones" ) )
		{
			return;
		}

		CoinmasterData data = FishboneryRequest.FISHBONERY;

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
		if ( FishboneryRequest.FRESHWATER_FISHBONE.getCount( KoLConstants.inventory ) == 0 )
		{
			return "You do not have a freshwater fishbone in inventory";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( urlString.startsWith( "inv_use.php" ) && urlString.contains( "whichitem=7651" ) )
		{
			// This is a simple visit to the Fishbonery
			return true;
		}

		// shop.php?pwd&whichshop=fishbones
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=fishbones" ) )
		{
			return false;
		}

		CoinmasterData data = FishboneryRequest.FISHBONERY;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
