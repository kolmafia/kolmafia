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

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BURTRequest
	extends CoinMasterRequest
{
	public static final String master = "Bugbear Token"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( BURTRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( BURTRequest.master );
	private static final Map itemByPrice = CoinmastersDatabase.invert( BURTRequest.buyPrices );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) BURT" );
	public static final AdventureResult BURT_TOKEN = ItemPool.get( ItemPool.BURT, 1 );
	private static final Pattern TOBUY_PATTERN = Pattern.compile( "itemquantity=(\\d+)" );
	public static final CoinmasterData BURT =
		new CoinmasterData(
			BURTRequest.master,
			"BURT",
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
			null,
			null,
			null,
			true,
			null
			);

	static
	{
		BURT.plural = "BURTs";
	}

	private static int priceToItemId( final int price )
	{
		String itemName = (String) BURTRequest.itemByPrice.get( IntegerPool.get( price ) );
		return ItemDatabase.getItemId( itemName );
	}

	private static int itemIdToPrice( final int itemId )
	{
		CoinmasterData data = BURTRequest.BURT;
		return data.getBuyPrice( itemId );
	}

	private static String lastURL = null;

	public BURTRequest()
	{
		super( BURTRequest.BURT );
	}

	public BURTRequest( final String action )
	{
		super( BURTRequest.BURT, action );
	}

	public BURTRequest( final String action, final AdventureResult [] attachments )
	{
		super( BURTRequest.BURT, action, attachments );
	}

	public BURTRequest( final String action, final AdventureResult attachment )
	{
		super( BURTRequest.BURT, action, attachment );
	}

	public BURTRequest( final String action, final int itemId, final int quantity )
	{
		super( BURTRequest.BURT, action, itemId, quantity );
	}

	public BURTRequest( final String action, final int itemId )
	{
		super( BURTRequest.BURT, action, itemId );
	}

	@Override
	public void setItem( final AdventureResult item )
	{
		// The item field is the buy price; the number of BURTS spent
		String itemField = this.data.getItemField();
		int itemId = item.getItemId();
		this.addFormField( itemField, String.valueOf( BURTRequest.itemIdToPrice( itemId ) ) );
	}

	@Override
	public void processResults()
	{
		BURTRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( String location, final String responseText )
	{
		if ( BURTRequest.lastURL == null )
		{
			return;
		}

		location = BURTRequest.lastURL;
		BURTRequest.lastURL = null;
			
		CoinmasterData data = BURTRequest.BURT;

		// If you don't have enough BURTs, you are redirected to inventory.php
		if ( responseText.indexOf( "You don't have enough BURTs" ) == -1 )
		{
			// inv_use.php?whichitem=5683&pwd&itemquantity=xxx
			Matcher itemMatcher = data.getItemMatcher( location );
			if ( itemMatcher.find() )
			{
				int price = StringUtilities.parseInt( itemMatcher.group( 1 ) );
				int itemId = BURTRequest.priceToItemId( price );
				CoinMasterRequest.completePurchase( data, itemId, 1, false );
			}
			return;
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

		CoinmasterData data = BURTRequest.BURT;
		Matcher itemMatcher = data.getItemMatcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}

		int price = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		int itemId = BURTRequest.priceToItemId( price );
		if ( itemId != -1 )
		{
			CoinMasterRequest.buyStuff( data, itemId, 1, false );
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
