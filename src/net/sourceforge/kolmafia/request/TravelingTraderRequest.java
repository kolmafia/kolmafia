/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TravelingTraderRequest
	extends CoinMasterRequest
{
	public static final String master = "Traveling Trader"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getNewList();
	private static final Map buyPrices = CoinmastersDatabase.getNewMap();

	// traveler.php?action=For Gnomeregan!&whichitem=xxxx&quantity=1&tradeall=1&usehagnk=1&pwd
	private static AdventureResult item = ItemPool.get( ItemPool.TWINKLY_WAD, 1 );

	public static final CoinmasterData TRAVELER =
		new CoinmasterData(
			TravelingTraderRequest.master,
			TravelingTraderRequest.class,
			"traveler.php",
			"twinkly wad",
			null,
			false,
			null,
			TravelingTraderRequest.item,
			null,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"For Gnomeregan!",
			TravelingTraderRequest.buyItems,
			TravelingTraderRequest.buyPrices,
			null,
			null,
			"usehagnk=1",
			"tradeall=1",
			true,
			null
			);

	public TravelingTraderRequest()
	{
		super( TravelingTraderRequest.TRAVELER );
	}

	public TravelingTraderRequest( final String action )
	{
		super( TravelingTraderRequest.TRAVELER, action );
	}

	public TravelingTraderRequest( final String action, final int itemId, final int quantity )
	{
		super( TravelingTraderRequest.TRAVELER, action, itemId, quantity );
	}

	public TravelingTraderRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public TravelingTraderRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	@Override
	public void processResults()
	{
		if ( this.responseText.length() == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "The Traveling Trader is not in the Market Square." );
			return;
		}
		TravelingTraderRequest.parseResponse( this.getURLString(), this.responseText );
	}

	// The traveling trader is looking to acquire:<br><img class='hand
	// item' onclick='descitem(503220568);'
	// src='http://images.kingdomofloathing.com/itemimages/scwad.gif'>
	// <b>twinkly wads</b><br>

	private static final Pattern ACQUIRE_PATTERN = Pattern.compile( "The traveling trader is looking to acquire.*?descitem\\(([\\d]+)\\).*?<b>([^<]*)</b>" );

	// (You have <b>3,348</b> on you.)

	private static final Pattern INVENTORY_PATTERN = Pattern.compile( "\\(You have <b>([\\d,]*|none)</b> on you.\\)" );

        // You currently have <b>1,022</b> twinkly wads in Hagnk's Ancestral Storage

	private static final Pattern STORAGE_PATTERN = Pattern.compile( "You currently have <b>([\\d,]+)</b> (.*?) in Hagnk's Ancestral Storage", Pattern.DOTALL );

	// <tr><td><input type=radio name=whichitem value=4411
	// checked="checked"></td><td><a class=nounder
	// href='javascript:descitem(629749615);'> <img class='hand item'
	// src='http://images.kingdomofloathing.com/itemimages/music.gif'>
	// <b>Inigo's Incantation of Inspiration</b></a></td><td>100 twinkly
	// wads</td></tr

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=whichitem value=([\\d]+).*?>.*?descitem.*?([\\d]+).*?<b>([^<]*)</b></a></td><td>([\\d]+)", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "traveler.php" ) )
		{
			return;
		}

		// First, see what item he's trading for
		String descId = "";
		String plural1 = null;

		Matcher matcher = ACQUIRE_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			descId = matcher.group( 1 );
			plural1 = matcher.group( 2 );
		}

		int itemId = ItemDatabase.getItemIdFromDescription( descId );
		if ( itemId == -1 )
		{
			// He wants something we don't know about?!  We have no
			// way to register an item from just the descid, since
			// the item number is not in the description text.
			// ItemDatabase.registerItem( descId );
			return;
		}

		// We know the item. Set the item and token in the CoinmasterData

		CoinmasterData data = TravelingTraderRequest.TRAVELER;
		AdventureResult item = ItemPool.get( itemId, PurchaseRequest.MAX_QUANTITY );
		data.setItem( item );
		data.setToken( item.getName() );

		// Sanity check number of that item we have in inventory
		matcher = INVENTORY_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			String num = matcher.group( 1 );
			int num1 = num == null ? 0 :
				num.equals( "none" ) ? 0 :
				num.equals( "one" ) ? 1 :
				StringUtilities.parseInt( num );

			int icount = item.getCount( KoLConstants.inventory );
			int idelta = num1 - icount;
			if ( idelta != 0 )
			{
				AdventureResult result = new AdventureResult( itemId, idelta );
				AdventureResult.addResultToList( KoLConstants.inventory, result );
				AdventureResult.addResultToList( KoLConstants.tally, result );
			}
		}

		// Sanity check number of that item we have in storage
		String plural2 = null;

		matcher = STORAGE_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			String num = matcher.group( 1 );
			int num2 = num == null ? 0 :
				num.equals( "none" ) ? 0 :
				num.equals( "one" ) ? 1 :
				StringUtilities.parseInt( num );
			plural2 = matcher.group( 2 );

			int scount = item.getCount( KoLConstants.storage );
			int sdelta = num2 - scount;
			if ( sdelta != 0 )
			{
				AdventureResult result = new AdventureResult( itemId, sdelta );
				AdventureResult.addResultToList( KoLConstants.storage, result );
			}
		}

		// Refresh the coinmaster lists every time we visit.
		// Learn new trade items by simply visiting the Traveling Trader

		LockableListModel items = TravelingTraderRequest.buyItems;
		Map prices = TravelingTraderRequest.buyPrices;
		items.clear();
		prices.clear();

		matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group(1) );
			String desc = matcher.group(2);
			String name = matcher.group(3);
			int price = StringUtilities.parseInt( matcher.group(4) );

			String match = ItemDatabase.getItemDataName( itemId );
			if ( match == null || !match.equals( name ) )
			{
				ItemDatabase.registerItem( id, name, desc );
			}

			// Add it to the Traveling Trader inventory
			AdventureResult offering = ItemPool.get( id, 1 );
			String cname = StringUtilities.getCanonicalName( name );
			Integer iprice = IntegerPool.get( price );
			items.add( offering );
			prices.put( cname, iprice );
		}

		// Register the purchase requests, now that we know what is available
		data.registerPurchaseRequests();

		CoinMasterRequest.parseResponse( data, urlString, responseText );
	}

	public static String accessible()
	{
		return null;
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "traveler.php" ) )
		{
			return false;
		}

		CoinmasterData data = TravelingTraderRequest.TRAVELER;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
