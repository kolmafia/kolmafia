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

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

public class LunarLunchRequest
	extends CoinMasterRequest
{
	public static final String master = "Lunar Lunch-o-Mat"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( LunarLunchRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( LunarLunchRequest.master );

	public static final CoinmasterData LUNAR_LUNCH =
		new CoinmasterData(
			LunarLunchRequest.master,
			LunarLunchRequest.class,
			"spaaace.php?place=shop3",
			"isotope",
			"You have 0 lunar isotopes",
			false,
			SpaaaceRequest.TOKEN_PATTERN,
			SpaaaceRequest.ISOTOPE,
			null,
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"buy",
			LunarLunchRequest.buyItems,
			LunarLunchRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
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

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "spaaace.php" ) || urlString.indexOf( "place=shop3" ) == -1 )
		{
			return false;
		}

		CoinmasterData data = LunarLunchRequest.LUNAR_LUNCH;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( !Preferences.getString( Quest.GENERATOR.getPref() ).equals( QuestDatabase.FINISHED ) )
		{
			return "You need to repair the Elves' Shield Generator to shop at the Lunar Lunch-o-Mat.";
		}
		return SpaaaceRequest.accessible();
	}

	@Override
	public void equip()
	{
		SpaaaceRequest.equip();
	}
}
