/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class Tr4pz0rRequest
	extends CoinMasterRequest
{
	public static String master = "L33t Tr4pz0r"; 
	public static LockableListModel buyItems = CoinmastersDatabase.getBuyItems( Tr4pz0rRequest.master );
	private static Map buyPrices = CoinmastersDatabase.getBuyPrices( Tr4pz0rRequest.master );

	// As you enter the Tr4pz0r's cabin, he leaps up from his table at the
	// sight of the 78 yeti furs draped across your back.

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "the sight of the ([\\d,]+|) ?yeti fur" );
	public static final AdventureResult YETI_FUR = ItemPool.get( ItemPool.YETI_FUR, 1 );
	public static final CoinmasterData L33T_TR4PZ0R =
		new CoinmasterData(
			Tr4pz0rRequest.master,
			Tr4pz0rRequest.class,
			"place.php?whichplace=mclargehuge&action=trappercabin",
			"yeti fur",
			"You ain't got no furs, son",
			false,
			Tr4pz0rRequest.TOKEN_PATTERN,
			Tr4pz0rRequest.YETI_FUR,
			null,
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"qty",
			CoinMasterRequest.QTY_PATTERN,
			"Yep.",
			Tr4pz0rRequest.buyItems,
			Tr4pz0rRequest.buyPrices,
			null,
			null,
			null,
			"max=on"
			);

	public Tr4pz0rRequest()
	{
		super( Tr4pz0rRequest.L33T_TR4PZ0R );
	}

	public Tr4pz0rRequest( final String action )
	{
		super( Tr4pz0rRequest.L33T_TR4PZ0R, action );
	}

	public Tr4pz0rRequest( final String action, final int itemId, final int quantity )
	{
		super( Tr4pz0rRequest.L33T_TR4PZ0R, action, itemId, quantity );
	}

	public Tr4pz0rRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public Tr4pz0rRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public Tr4pz0rRequest( final int itemId, final int quantity )
	{
		this( "Yep.", itemId, quantity );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		CoinMasterRequest.parseResponse( Tr4pz0rRequest.L33T_TR4PZ0R, urlString, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim place.php?whichplace=mclargehuge&action=trappercabin?action=Yep.
		if ( !urlString.startsWith( "place.php?whichplace=mclargehuge&action=trappercabin" ) )
		{
			return false;
		}

		CoinmasterData data = Tr4pz0rRequest.L33T_TR4PZ0R;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		if ( KoLCharacter.getLevel() < 8 )
		{
			return "You haven't even met the L33t Tr4pz0r yet";
		}
		if ( !KoLCharacter.getTr4pz0rQuestCompleted() )
		{
			return "You have unfinished business with the L33t Tr4pz0r";
		}
		return null;
	}
}
