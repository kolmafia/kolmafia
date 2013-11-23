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

import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class TicketCounterRequest
	extends CoinMasterRequest
{
	public static final String master = "Arcade Ticket Counter"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( TicketCounterRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( TicketCounterRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You currently have ([\\d,]+) Game Grid redemption ticket" );
	public static final AdventureResult TICKET = ItemPool.get( ItemPool.GG_TICKET, 1 );
	public static final CoinmasterData TICKET_COUNTER =
		new CoinmasterData(
			TicketCounterRequest.master,
			TicketCounterRequest.class,
			"arcade.php",
			"ticket",
			"You currently have no Game Grid redemption tickets",
			false,
			TicketCounterRequest.TOKEN_PATTERN,
			TicketCounterRequest.TICKET,
			null,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"redeem",
			TicketCounterRequest.buyItems,
			TicketCounterRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			null
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

	public static boolean parseResponse( final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "action=redeem" ) != -1 )
		{
			CoinMasterRequest.parseResponse( TicketCounterRequest.TICKET_COUNTER, urlString, responseText );
			return true;
		}

		return false;
	}

	public static final void buy( final int itemId, final int count )
	{
		RequestThread.postRequest( new TicketCounterRequest( "redeem", itemId, count ) );
	}

	public static String accessible()
	{
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim arcade.php?action=redeem
		if ( !urlString.startsWith( "arcade.php" ) )
		{
			return false;
		}

		CoinmasterData data = TicketCounterRequest.TICKET_COUNTER;
		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
