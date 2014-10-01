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

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TicketCounterRequest
	extends CoinMasterRequest
{
	public static final String master = "Arcade Ticket Counter"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( TicketCounterRequest.master );
	private static final Map<String, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( TicketCounterRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( TicketCounterRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You currently have ([\\d,]+) Game Grid redemption ticket" );
	public static final AdventureResult TICKET = ItemPool.get( ItemPool.GG_TICKET, 1 );
	public static final CoinmasterData TICKET_COUNTER =
		new CoinmasterData(
			TicketCounterRequest.master,
			"arcade",
			TicketCounterRequest.class,
			"shop.php?whichshop=arcade",
			"ticket",
			"You currently have no Game Grid redemption tickets",
			false,
			TicketCounterRequest.TOKEN_PATTERN,
			TicketCounterRequest.TICKET,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			TicketCounterRequest.buyItems,
			TicketCounterRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			TicketCounterRequest.itemRows
			);

	public TicketCounterRequest()
	{
		super( TicketCounterRequest.TICKET_COUNTER );
	}

	public TicketCounterRequest( final String action )
	{
		super( TicketCounterRequest.TICKET_COUNTER, action );
	}

	public TicketCounterRequest( final String action, final AdventureResult [] attachments )
	{
		super( TicketCounterRequest.TICKET_COUNTER, action, attachments );
	}

	public TicketCounterRequest( final String action, final AdventureResult attachment )
	{
		super( TicketCounterRequest.TICKET_COUNTER, action, attachment );
	}

	public TicketCounterRequest( final String action, final int itemId, final int quantity )
	{
		super( TicketCounterRequest.TICKET_COUNTER, action, itemId, quantity );
	}

	public TicketCounterRequest( final String action, final int itemId )
	{
		super( TicketCounterRequest.TICKET_COUNTER, action, itemId );
	}

	@Override
	public void processResults()
	{
		TicketCounterRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern ITEM_PATTERN = Pattern.compile( "<tr rel=\"(\\d+)\".*?descitem\\((\\d+).*?<b>(.*?)</b>" );

	private static final int[] unlockables =
	{
		ItemPool.SINISTER_DEMON_MASK,
		ItemPool.CHAMPION_BELT,
		ItemPool.SPACE_TRIP_HEADPHONES,
		ItemPool.METEOID_ICE_BEAM,
		ItemPool.DUNGEON_FIST_GAUNTLET,
	};

	public static boolean parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( "whichshop=arcade" ) )
		{
			return false;
		}
		// Learn new trade items by simply visiting Arcade
		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			int id = StringUtilities.parseInt( matcher.group(1) );
			for ( int i = 0; i < TicketCounterRequest.unlockables.length; i++ )
			{
				if ( id == TicketCounterRequest.unlockables[i] )
				{
					Preferences.setBoolean( "lockedItem" + id, false );
					break;
				}
			}
			String desc = matcher.group(2);
			String name = matcher.group(3);
			String data = ItemDatabase.getItemDataName( id );
			// String price = matcher.group(4);
			if ( data == null || !data.equals( name ) )
			{
				ItemDatabase.registerItem( id, name, desc );
			}
		}

		CoinMasterRequest.parseResponse( TicketCounterRequest.TICKET_COUNTER, urlString, responseText );

		return true;
	}

	public static String accessible()
	{
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim arcade.php?action=redeem
		if ( !urlString.contains( "whichshop=arcade" ) )
		{
			return false;
		}

		CoinmasterData data = TicketCounterRequest.TICKET_COUNTER;
		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
