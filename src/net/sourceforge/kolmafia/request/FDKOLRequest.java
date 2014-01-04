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

public class FDKOLRequest
	extends CoinMasterRequest
{
	public static final String master = "FDKOL Tent";
	public static final AdventureResult FDKOL_TOKEN =  ItemPool.get( ItemPool.FDKOL_COMMENDATION, 1 );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "<td>([\\d,]+) FDKOL commendation" );
	public static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( FDKOLRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( FDKOLRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( FDKOLRequest.master );

	public static final CoinmasterData FDKOL =
		new CoinmasterData(
			FDKOLRequest.master,
			"FDKOL",
			FDKOLRequest.class,
			"shop.php?whichshop=fdkol",
			"FDKOL commendation",
			null,
			false,
			FDKOLRequest.TOKEN_PATTERN,
			FDKOLRequest.FDKOL_TOKEN,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			FDKOLRequest.buyItems,
			FDKOLRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			FDKOLRequest.itemRows
			);

	public FDKOLRequest()
	{
		super( FDKOLRequest.FDKOL );
	}

	public FDKOLRequest( final String action, final AdventureResult [] attachments )
	{
		super( FDKOLRequest.FDKOL, action, attachments );
	}

	public FDKOLRequest( final String action, final AdventureResult attachment )
	{
		super( FDKOLRequest.FDKOL, action, attachment );
	}

	public FDKOLRequest( final String action, final int itemId, final int quantity )
	{
		super( FDKOLRequest.FDKOL, action, itemId, quantity );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "shop.php" ) || !location.contains( "whichshop=fdkol" ) )
		{
			return;
		}

		CoinmasterData data = FDKOLRequest.FDKOL;

		Matcher m = TransferItemRequest.ITEMID_PATTERN.matcher( location );
		if ( !m.find() )
		{
			CoinMasterRequest.parseBalance( data, responseText );
			return;
		}

		int itemId = StringUtilities.parseInt( m.group( 1 ) );
		AdventureResult item = AdventureResult.findItem( itemId, data.getBuyItems() );
		if ( item == null )
		{
			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static String accessible()
	{
		if ( FDKOLRequest.FDKOL_TOKEN.getCount( KoLConstants.inventory ) == 0 )
		{
			return "You do not have an FDKOL commendation in inventory";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString, final boolean noMeat )
	{
		if ( urlString.startsWith( "inv_use.php" ) && urlString.contains( "whichitem=5707" ) )
		{
			// This is a simple visit to the FDKOL Requisitions Tent
			return true;
		}

		// shop.php?pwd&whichshop=fdkol
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=fdkol" ) )
		{
			return false;
		}

		Matcher m = TransferItemRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !m.find() )
		{
			// Just a visit
			return true;
		}

		CoinmasterData data = FDKOLRequest.FDKOL;
		int itemId = StringUtilities.parseInt( m.group( 1 ) );
		AdventureResult item = AdventureResult.findItem( itemId, data.getBuyItems() );
		if ( item == null )
		{
			// Presumably this is a purchase for Meat.
			// If we've already checked Meat, this is an unknown item
			if ( noMeat )
			{
				return false;
			}
			return NPCPurchaseRequest.registerShopRequest( urlString, true );
		}

		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
