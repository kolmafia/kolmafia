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
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

public class CrimboCartelRequest
	extends CoinMasterRequest
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You currently have <b>([\\d,]+)</b> Crimbux" );
	public static final CoinmasterData CRIMBO_CARTEL =
		new CoinmasterData(
			"Crimbo Cartel",
			"crimbo09.php",
			"Crimbuck",
			"You do not currently have any Crimbux",
			false,
			CrimboCartelRequest.TOKEN_PATTERN,
			CoinmastersFrame.CRIMBUCK,
			"availableCrimbux",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"howmany",
			CoinMasterRequest.HOWMANY_PATTERN,
			"buygift",
			CoinmastersDatabase.getCrimbuckItems(),
			CoinmastersDatabase.crimbuckBuyPrices(),
			null,
			null
			);

	public CrimboCartelRequest()
	{
		super( CrimboCartelRequest.CRIMBO_CARTEL );
	}

	public CrimboCartelRequest( final String action )
	{
		super( CrimboCartelRequest.CRIMBO_CARTEL, action );
	}

	public CrimboCartelRequest( final String action, final int itemId, final int quantity )
	{
		super( CrimboCartelRequest.CRIMBO_CARTEL, action, itemId, quantity );
	}

	public CrimboCartelRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public CrimboCartelRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public static void parseCrimboCartelVisit( final String location, final String responseText )
	{
		CoinmasterData data = CrimboCartelRequest.CRIMBO_CARTEL;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "place=store" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
				CoinmastersFrame.externalUpdate();
			}

			return;
		}

		if ( !action.equals( "buygift" ) )
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
		// We only claim crimbo09.php?action=buygift
		if ( !urlString.startsWith( "crimbo09.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		if ( !action.equals( "buygift" ) )
		{
			return false;
		}

		CoinmasterData data = CrimboCartelRequest.CRIMBO_CARTEL;
		CoinMasterRequest.buyStuff( data, urlString );
		return true;
	}
}
