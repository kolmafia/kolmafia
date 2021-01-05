/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import java.util.Map;
import java.util.regex.Pattern;

public class Crimbo20BoozeRequest
	extends CoinMasterRequest
{
	public static final String master = "Elf Booze Drive";

	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( Crimbo20BoozeRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( Crimbo20BoozeRequest.master );
	private static Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows( Crimbo20BoozeRequest.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "([\\d,]+) (boxes of )?donated booze" );
	public static final AdventureResult TOKEN = ItemPool.get( ItemPool.DONATED_BOOZE, 1 );

	public static final CoinmasterData CRIMBO20BOOZE =
		new CoinmasterData(
			Crimbo20BoozeRequest.master,
			"crimbo20booze",
			Crimbo20BoozeRequest.class,
			"donated booze",
			"no boxes of donated booze",
			false,
			Crimbo20BoozeRequest.TOKEN_PATTERN,
			Crimbo20BoozeRequest.TOKEN,
			null,
			Crimbo20BoozeRequest.itemRows,
			"shop.php?whichshop=crimbo20booze",
			"buyitem",
			Crimbo20BoozeRequest.buyItems,
			Crimbo20BoozeRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichrow",
			GenericRequest.WHICHROW_PATTERN,
			"quantity",
			GenericRequest.QUANTITY_PATTERN,
			null,
			null,
			true
			)
		{
			@Override
			public final boolean canBuyItem( final int itemId )
			{
				switch ( itemId )
				{
					case ItemPool.BOOZE_DRIVE_BUTTON:
					case ItemPool.BOOZE_MAILING_LIST:
						AdventureResult item = ItemPool.get( itemId );
						return item.getCount( KoLConstants.closet ) + item.getCount( KoLConstants.inventory ) == 0;
				}
				return super.canBuyItem( itemId );
			}
		};

	public Crimbo20BoozeRequest()
	{
		super(Crimbo20BoozeRequest.CRIMBO20BOOZE );
	}

	public Crimbo20BoozeRequest(final boolean buying, final AdventureResult [] attachments )
	{
		super(Crimbo20BoozeRequest.CRIMBO20BOOZE, buying, attachments );
	}

	public Crimbo20BoozeRequest(final boolean buying, final AdventureResult attachment )
	{
		super(Crimbo20BoozeRequest.CRIMBO20BOOZE, buying, attachment );
	}

	public Crimbo20BoozeRequest(final boolean buying, final int itemId, final int quantity )
	{
		super(Crimbo20BoozeRequest.CRIMBO20BOOZE, buying, itemId, quantity );
	}

	@Override
	public void run()
	{
		if ( this.action != null )
		{
			this.addFormField( "pwd" );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		Crimbo20BoozeRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		if ( !location.contains( "whichshop=crimbo20booze" ) )
		{
			return;
		}

		CoinmasterData data = Crimbo20BoozeRequest.CRIMBO20BOOZE;

		String action = GenericRequest.getAction( location );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, location, responseText );
			return;
		}

		// Parse current coin balances
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static String accessible()
	{
		// Change after it closes
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "shop.php" ) || !urlString.contains( "whichshop=crimbo20booze" ) )
		{
			return false;
		}

		CoinmasterData data = Crimbo20BoozeRequest.CRIMBO20BOOZE;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}
}
