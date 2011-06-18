/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

public class BigBrotherRequest
	extends CoinMasterRequest
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? sand dollar" );

	public static final CoinmasterData BIG_BROTHER =
		new CoinmasterData(
			"Big Brother",
			"monkeycastle.php?who=2",
			"sand dollar",
			"You haven't got any sand dollars",
			false,
			BigBrotherRequest.TOKEN_PATTERN,
			CoinmastersFrame.SAND_DOLLAR,
			"availableSandDollars",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"buyitem",
			CoinmastersDatabase.getSandDollarItems(),
			CoinmastersDatabase.sandDollarBuyPrices(),
			null,
			null
			);

	public BigBrotherRequest()
	{
		super( BigBrotherRequest.BIG_BROTHER );
	}

	public BigBrotherRequest( final String action )
	{
		super( BigBrotherRequest.BIG_BROTHER, action );
	}

	public BigBrotherRequest( final String action, final int itemId, final int quantity )
	{
		super( BigBrotherRequest.BIG_BROTHER, action, itemId, quantity );
	}

	public BigBrotherRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public BigBrotherRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public static void parseVisit( final String location, final String responseText )
	{
		CoinmasterData data = BigBrotherRequest.BIG_BROTHER;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "who=2" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
				CoinmastersFrame.externalUpdate();
			}

			return;
		}

		if ( !action.equals( "buyitem" ) )
		{
			return;
		}

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			CoinMasterRequest.refundPurchase( data, location );
		}

		CoinMasterRequest.parseBalance( data, responseText );
		CoinmastersFrame.externalUpdate();
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim monkeycastle.php?action=buyitem
		if ( !urlString.startsWith( "monkeycastle.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			if ( urlString.indexOf( "who=2" ) != -1 )
			{
				// Simple visit
				RequestLogger.updateSessionLog( "Visiting Big Brother" );
				return true;
			}

			return false;
		}

		if ( !action.equals( "buyitem" ) )
		{
			return false;
		}

		CoinmasterData data = BigBrotherRequest.BIG_BROTHER;
		CoinMasterRequest.buyStuff( data, urlString );
		return true;
	}
}
