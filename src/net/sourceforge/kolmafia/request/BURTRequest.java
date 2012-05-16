/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BURTRequest
	extends CoinMasterRequest
{
	public static final String master = "Bugbear Token"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( BURTRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( BURTRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) BURT" );
	public static final AdventureResult BURT_TOKEN = ItemPool.get( ItemPool.BURT, 1 );
	private static final Pattern TOBUY_PATTERN = Pattern.compile( "itemquantity=(\\d+)" );
	public static final CoinmasterData BURT =
		new CoinmasterData(
			BURTRequest.master,
			BURTRequest.class,
			"inv_use.php?whichitem=5683&ajax=1",
			"BURT",
			null,
			false,
			BURTRequest.TOKEN_PATTERN,
			BURTRequest.BURT_TOKEN,
			null,
			"itemquantity",
			BURTRequest.TOBUY_PATTERN,
			null,
			null,
			null,
			BURTRequest.buyItems,
			BURTRequest.buyPrices,
			null,
			null
			);

	private static String lastURL = null;

	public BURTRequest()
	{
		super( BURTRequest.BURT );
	}

	public BURTRequest( final String action )
	{
		super( BURTRequest.BURT, action );
	}

	public BURTRequest( final String action, final int itemId, final int quantity )
	{
		super( BURTRequest.BURT, action, itemId, quantity );
		this.addFormField( "doit", "69" );
	}

	public BURTRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public BURTRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public void processResults()
	{
		BURTRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = BURTRequest.BURT;

		// If you don't have enough commendations, you are redirected to inventory.php
		if ( responseText.indexOf( "You don't have enough BURTs" ) == -1 )
		{
			// inv_use.php?whichitem=5683&pwd&&itemquantity=xxx
			CoinMasterRequest.completePurchase( data, location );
		}

		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		// inv_use.php?whichitem=5683&pwd&itemquantity=xxx
		if ( !urlString.startsWith( "inv_use.php" ) || urlString.indexOf( "whichitem=5683" ) == -1 )
		{
			return false;
		}

		// Save URL. If request fails, we are redirected to inventory.php
		BURTRequest.lastURL = urlString;

		if ( urlString.indexOf( "itemquantity" ) != -1 )
		{
			CoinmasterData data = BURTRequest.BURT;
			CoinMasterRequest.registerRequest( data, urlString );
		}

		return true;
	}

	public static String accessible()
	{
		int BURTs = BURTRequest.BURT_TOKEN.getCount( KoLConstants.inventory );
		if ( BURTs == 0 )
		{
			return "You don't have any BURTs";
		}
		return null;
	}
}
