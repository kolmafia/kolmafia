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

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class DollHawkerRequest
	extends CoinMasterRequest
{
	public static final String master = "Dollhawker's Emporium";
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( DollHawkerRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( DollHawkerRequest.master );
	private static Map<String, Integer> itemRows = CoinmastersDatabase.getRows( DollHawkerRequest.master );

	public static final CoinmasterData DOLLHAWKER =
		new CoinmasterData(
			DollHawkerRequest.master,
			DollHawkerRequest.class,
			"shop.php?whichshop=elvishp2",
			"isotope",
			"You have 0 lunar isotopes",
			false,
			SpaaaceRequest.TOKEN_PATTERN,
			SpaaaceRequest.ISOTOPE,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			"buyitem",
			DollHawkerRequest.buyItems,
			DollHawkerRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			DollHawkerRequest.itemRows
			);

	public DollHawkerRequest()
	{
		super( DollHawkerRequest.DOLLHAWKER );
	}

	public DollHawkerRequest( final String action )
	{
		super( DollHawkerRequest.DOLLHAWKER, action );
	}

	public DollHawkerRequest( final String action, final int itemId, final int quantity )
	{
		super( DollHawkerRequest.DOLLHAWKER, action, itemId, quantity );
	}

	public DollHawkerRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public DollHawkerRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || urlString.indexOf( "whichshop=elvishp2" ) == -1 )
		{
			return false;
		}

		CoinmasterData data = DollHawkerRequest.DOLLHAWKER;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		return SpaaaceRequest.accessible();
	}

	@Override
	public void equip()
	{
		SpaaaceRequest.equip();
	}
}
