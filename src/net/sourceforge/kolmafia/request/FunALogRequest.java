/**
 * Copyright (c) 2005-2018, KoLmafia development team
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
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

public class FunALogRequest
	extends CoinMasterRequest
{
	public static final String master = "PirateRealm Fun-a-Log";
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems(FunALogRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices(FunALogRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows(FunALogRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<b>You have ([\\d,]+) FunPoints?</b>" );
	public static final CoinmasterData FUN_A_LOG =
		new CoinmasterData(
			FunALogRequest.master,
			"Fun-a-Log",
			FunALogRequest.class,
			"FunPoint",
			"You have no FunPoints",
			false,
			FunALogRequest.TOKEN_PATTERN,
			null,
			"availableFunPoints",
			FunALogRequest.itemRows,
			"shop.php?whichshop=piraterealm",
			"buyitem",
			FunALogRequest.buyItems,
			FunALogRequest.buyPrices,
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

	public FunALogRequest()
	{
		super( FunALogRequest.FUN_A_LOG );
	}

	public FunALogRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super(FunALogRequest.FUN_A_LOG, buying, attachments );
	}

	public FunALogRequest( final boolean buying, final AdventureResult attachment )
	{
		super(FunALogRequest.FUN_A_LOG, buying, attachment );
	}

	public FunALogRequest( final boolean buying, final int itemId, final int quantity )
	{
		super(FunALogRequest.FUN_A_LOG, buying, itemId, quantity );
	}

	@Override
	public void processResults()
	{
		FunALogRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=piraterealm" ) )
		{
			return;
		}

		CoinmasterData data = FunALogRequest.FUN_A_LOG;

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
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=piraterealm" ) )
		{
			return false;
		}

		CoinmasterData data = FunALogRequest.FUN_A_LOG;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		return Preferences.getBoolean( "_prToday" ) || Preferences.getBoolean( "prAlways" ) ? null : "Need access to Pirate Realm";
	}
}
