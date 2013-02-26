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

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class AltarOfBonesRequest
	extends CoinMasterRequest
{
	public static final String master = "Altar of Bones"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( AltarOfBonesRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( AltarOfBonesRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+).*?bone chips" );
	public static final AdventureResult BONE_CHIPS = ItemPool.get( ItemPool.BONE_CHIPS, 1 );
	public static final CoinmasterData ALTAR_OF_BONES =
		new CoinmasterData(
			AltarOfBonesRequest.master,
			AltarOfBonesRequest.class,
			"bone_altar.php",
			"bone chips",
			"You have no bone chips",
			false,
			AltarOfBonesRequest.TOKEN_PATTERN,
			AltarOfBonesRequest.BONE_CHIPS,
			null,
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			null,
			null,
			"buy",
			AltarOfBonesRequest.buyItems,
			AltarOfBonesRequest.buyPrices,
			null,
			null
			);

	public AltarOfBonesRequest()
	{
		super( AltarOfBonesRequest.ALTAR_OF_BONES );
	}

	public AltarOfBonesRequest( final String action )
	{
		super( AltarOfBonesRequest.ALTAR_OF_BONES, action );
	}

	public AltarOfBonesRequest( final String action, final int itemId, final int quantity )
	{
		super( AltarOfBonesRequest.ALTAR_OF_BONES, action, itemId, quantity );
	}

	public AltarOfBonesRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public AltarOfBonesRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public static void parseResponse( final String urlString, final String responseText )
	{
		CoinMasterRequest.parseResponse( AltarOfBonesRequest.ALTAR_OF_BONES, urlString, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim bone_altar.php?action=buy
		if ( !urlString.startsWith( "bone_altar.php" ) )
		{
			return false;
		}

		CoinmasterData data = AltarOfBonesRequest.ALTAR_OF_BONES;
		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		return "The Altar of Bones is not available";
	}
}
