/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class PokemporiumRequest
	extends CoinMasterRequest
{
	public static final String master = "The Pok&eacute;mporium";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( PokemporiumRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( PokemporiumRequest.master );
	private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( PokemporiumRequest.master );
	private static final Pattern POKEDOLLAR_PATTERN = Pattern.compile( "([\\d,]+) 1,960 pok&eacute;dollar bills" );
	public static final AdventureResult POKEDOLLAR = new AdventureResult( ItemPool.POKEDOLLAR_BILLS, 1, false ) {
			@Override
			public String getPluralName( int price )
			{
				return price == 1 ? "pok&eacute;dollar bill" : "pok&eacute;dollar bills";
			}
		};

	public static final CoinmasterData POKEMPORIUM =
		new CoinmasterData(
			PokemporiumRequest.master,
			"pokefam",
			PokemporiumRequest.class,
			"pok&eacute;dollar bills",
			"no pok&eacute;dollar bills",
			false,
			PokemporiumRequest.POKEDOLLAR_PATTERN,
			PokemporiumRequest.POKEDOLLAR,
			null,
			PokemporiumRequest.itemRows,
			"shop.php?whichshop=pokefam",
			"buyitem",
			PokemporiumRequest.buyItems,
			PokemporiumRequest.buyPrices,
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
			)
		{
			@Override
			public final boolean canBuyItem( final int itemId )
			{
				return KoLCharacter.inPokefam();
			}
		};

	static
	{
		POKEMPORIUM.plural = "pok&eacute;dollar bills";
	}

    public PokemporiumRequest()
	{
		super(PokemporiumRequest.POKEMPORIUM );
	}

	public PokemporiumRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super(PokemporiumRequest.POKEMPORIUM, buying, attachments );
	}

	public PokemporiumRequest( final boolean buying, final AdventureResult attachment )
	{
		super(PokemporiumRequest.POKEMPORIUM, buying, attachment );
	}

	public PokemporiumRequest( final boolean buying, final int itemId, final int quantity )
	{
		super(PokemporiumRequest.POKEMPORIUM, buying, itemId, quantity );
	}

	@Override
	public void run()
	{
		if ( this.action != null )
		{
			this.addFormField( "pwd" );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		PokemporiumRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=pokefam" ) )
		{
			return;
		}

		CoinmasterData data = PokemporiumRequest.POKEMPORIUM;

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
		// Change after it closes
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=pokefam" ) )
		{
			return false;
		}

		CoinmasterData data = PokemporiumRequest.POKEMPORIUM;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
