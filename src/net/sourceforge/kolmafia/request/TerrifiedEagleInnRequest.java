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
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TerrifiedEagleInnRequest
	extends CoinMasterRequest
{
	public static final String master = "The Terrified Eagle Inn"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( TerrifiedEagleInnRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( TerrifiedEagleInnRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( TerrifiedEagleInnRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>(\\w+) Freddy Kruegerand(?:s?)</td>" );
	public static final AdventureResult KRUEGERAND = ItemPool.get( ItemPool.KRUEGERAND, 1 );
	public static final CoinmasterData TERRIFIED_EAGLE_INN =
		new CoinmasterData(
			TerrifiedEagleInnRequest.master,
			"dreadsylvania",
			TerrifiedEagleInnRequest.class,
			"shop.php?whichshop=dv",
			"Freddy Kruegerand",
			null,
			false,
			TerrifiedEagleInnRequest.TOKEN_PATTERN,
			TerrifiedEagleInnRequest.KRUEGERAND,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			TerrifiedEagleInnRequest.buyItems,
			TerrifiedEagleInnRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			TerrifiedEagleInnRequest.itemRows
			);

	public TerrifiedEagleInnRequest()
	{
		super( TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN );
	}

	public TerrifiedEagleInnRequest( final String action )
	{
		super( TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN, action );
	}

	public TerrifiedEagleInnRequest( final String action, final AdventureResult [] attachments )
	{
		super( TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN, action, attachments );
	}

	public TerrifiedEagleInnRequest( final String action, final AdventureResult attachment )
	{
		super( TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN, action, attachment );
	}

	public TerrifiedEagleInnRequest( final String action, final int itemId, final int quantity )
	{
		super( TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN, action, itemId, quantity );
	}

	public TerrifiedEagleInnRequest( final String action, final int itemId )
	{
		super( TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN, action, itemId );
	}

	@Override
	public void processResults()
	{
		TerrifiedEagleInnRequest.parseResponse( this.getURLString(), this.responseText );
	}


	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "name=whichrow value=(\\d*).*?<a onClick='javascript:descitem\\((\\d+)\\)'><b>(.*?)</b>.*?</a>.*?<b>([,\\d]*)</b>" );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=dv" ) )
		{
			return;
		}

		CoinmasterData data = TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
			return;
		}

		// Debug: look for new items every time we visit the shop
		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String row = matcher.group(1);
			String descId = matcher.group(2);
			String itemName = matcher.group(3);
			int price = StringUtilities.parseInt( matcher.group(4) );

			String match = ItemDatabase.getItemName( descId );
			if ( match == null )
			{
				// Unfortunately, there is no itemId in the table.
				// ItemDatabase.registerItem( itemId, itemName, descId );

				// Print what goes in coinmasters.txt
				StringBuilder printMe= new StringBuilder();
				printMe.append( KoLConstants.LINE_BREAK );
				printMe.append( TerrifiedEagleInnRequest.master );	
				printMe.append( "\tbuy\t" );
				printMe.append( String.valueOf( price ) );
				printMe.append( "\t" );
				printMe.append( itemName );
				printMe.append( "\tROW" );
				printMe.append( row );
				String message = printMe.toString();
				RequestLogger.printLine( message );
				RequestLogger.updateSessionLog( message );
			}
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static String accessible()
	{
		return null;
	}

	public static boolean registerRequest( final String urlString )
	{
		// shop.php?pwd&whichshop=dv
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=dv" ) )
		{
			return false;
		}

		CoinmasterData data = TerrifiedEagleInnRequest.TERRIFIED_EAGLE_INN;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
