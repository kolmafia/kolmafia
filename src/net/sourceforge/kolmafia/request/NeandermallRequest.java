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

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

public class NeandermallRequest
	extends CoinMasterRequest
{
	public static final String master = "The Neandermall"; 

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( NeandermallRequest.master );
	private static final Map<String, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( NeandermallRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( NeandermallRequest.master );
	private static final Pattern CHRONER_PATTERN = Pattern.compile( "([\\d,]+) Chroner" );
	public static final AdventureResult CHRONER = ItemPool.get( ItemPool.CHRONER, 1 );

	public static final CoinmasterData NEANDERMALL =
		new CoinmasterData(
			NeandermallRequest.master,
			"caveshop",
			NeandermallRequest.class,
			"shop.php?whichshop=caveshop",
			"Chroner",
			"no Chroners",
			false,
			NeandermallRequest.CHRONER_PATTERN,
			NeandermallRequest.CHRONER,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			NeandermallRequest.buyItems,
			NeandermallRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			NeandermallRequest.itemRows
			);

	public NeandermallRequest()
	{
		super( NeandermallRequest.NEANDERMALL );
	}

	public NeandermallRequest( final String action )
	{
		super( NeandermallRequest.NEANDERMALL, action );
	}

	public NeandermallRequest( final String action, final AdventureResult [] attachments )
	{
		super( NeandermallRequest.NEANDERMALL, action, attachments );
	}

	public NeandermallRequest( final String action, final AdventureResult attachment )
	{
		super( NeandermallRequest.NEANDERMALL, action, attachment );
	}

	public NeandermallRequest( final String action, final int itemId, final int quantity )
	{
		super( NeandermallRequest.NEANDERMALL, action, itemId, quantity );
	}

	public NeandermallRequest( final String action, final int itemId )
	{
		super( NeandermallRequest.NEANDERMALL, action, itemId );
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
		NeandermallRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=caveshop" ) )
		{
			return;
		}

		CoinmasterData data = NeandermallRequest.NEANDERMALL;

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
		if ( !Preferences.getBoolean( "timeTowerAvailable" ) )
		{
			return "You can't get to the Neandermall";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=caveshop" ) )
		{
			return false;
		}

		CoinmasterData data = NeandermallRequest.NEANDERMALL;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
