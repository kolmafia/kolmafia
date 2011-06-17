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
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

public class LunarLunchRequest
	extends CoinMasterRequest
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) lunar isotope" );
	public static final CoinmasterData LUNAR_LUNCH =
		new CoinmasterData(
			"Lunar Lunch-o-Mat",
			"spaaace.php?place=shop3",
			"isotope",
			"You have 0 lunar isotopes",
			false,
			LunarLunchRequest.TOKEN_PATTERN,
			CoinmastersFrame.ISOTOPE,
			"availableIsotopes",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"buy",
			CoinmastersDatabase.getIsotope3Items(),
			CoinmastersDatabase.isotope3BuyPrices(),
			null,
			null
			);

	public LunarLunchRequest()
	{
		super( LunarLunchRequest.LUNAR_LUNCH );
	}

	public LunarLunchRequest( final String action )
	{
		super( LunarLunchRequest.LUNAR_LUNCH, action );
	}

	public LunarLunchRequest( final String action, final int itemId, final int quantity )
	{
		super( LunarLunchRequest.LUNAR_LUNCH, action, itemId, quantity );
	}

	public LunarLunchRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public LunarLunchRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public static final void buy( final int itemId, final int count )
	{
		RequestThread.postRequest( new LunarLunchRequest( "buy", itemId, count ) );
	}
}
