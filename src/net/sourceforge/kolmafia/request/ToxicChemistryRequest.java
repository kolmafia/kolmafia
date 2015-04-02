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

public class ToxicChemistryRequest
	extends CoinMasterRequest
{
	public static final String master = "Toxic Chemistry";

	public static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( ToxicChemistryRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( ToxicChemistryRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( ToxicChemistryRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) toxic globule" );
	public static final AdventureResult TOXIC_GLOBULE =  ItemPool.get( ItemPool.TOXIC_GLOBULE, 1 );

	public static final CoinmasterData TOXIC_CHEMISTRY =
		new CoinmasterData(
			ToxicChemistryRequest.master,
			"ToxicChemistry",
			ToxicChemistryRequest.class,
			"toxic globule",
			"no toxic globules",
			false,
			ToxicChemistryRequest.TOKEN_PATTERN,
			ToxicChemistryRequest.TOXIC_GLOBULE,
			null,
			ToxicChemistryRequest.itemRows,
			"shop.php?whichshop=toxic",
			"buyitem",
			ToxicChemistryRequest.buyItems,
			ToxicChemistryRequest.buyPrices,
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

	public ToxicChemistryRequest()
	{
		super( ToxicChemistryRequest.TOXIC_CHEMISTRY );
	}

	public ToxicChemistryRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( ToxicChemistryRequest.TOXIC_CHEMISTRY, buying, attachments );
	}

	public ToxicChemistryRequest( final boolean buying, final AdventureResult attachment )
	{
		super( ToxicChemistryRequest.TOXIC_CHEMISTRY, buying, attachment );
	}

	public ToxicChemistryRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( ToxicChemistryRequest.TOXIC_CHEMISTRY, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		ToxicChemistryRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "shop.php" ) || !location.contains( "whichshop=toxic" ) )
		{
			return;
		}

		CoinmasterData data = ToxicChemistryRequest.TOXIC_CHEMISTRY;

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
		if ( ToxicChemistryRequest.TOXIC_GLOBULE.getCount( KoLConstants.inventory ) == 0 )
		{
			return "You do not have a toxic globule in inventory";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		// shop.php?pwd&whichshop=toxic
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=toxic" ) )
		{
			return false;
		}

		CoinmasterData data = ToxicChemistryRequest.TOXIC_CHEMISTRY;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
