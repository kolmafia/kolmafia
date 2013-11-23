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

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class SwaggerShopRequest
	extends CoinMasterRequest
{
	public static final String master = "The Swagger Shop"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( SwaggerShopRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( SwaggerShopRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) swagger" );

	public static final CoinmasterData SWAGGER_SHOP =
		new CoinmasterData(
			SwaggerShopRequest.master,
			"swagger",
			SwaggerShopRequest.class,
			"peevpee.php?place=shop",
			"swagger",
			"You have 0 swagger",
			false,
			SwaggerShopRequest.TOKEN_PATTERN,
			null,
			"availableSwagger",
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			null,
			null,
			"buy",
			SwaggerShopRequest.buyItems,
			SwaggerShopRequest.buyPrices,
			null,
			null,
			null,
			null,
			true,
			null
			);

	static
	{
		SWAGGER_SHOP.plural = "swagger";
	}

	public SwaggerShopRequest()
	{
		super( SwaggerShopRequest.SWAGGER_SHOP );
	}

	public SwaggerShopRequest( final String action )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, action );
	}

	public SwaggerShopRequest( final String action, final AdventureResult attachment )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, action, attachment );
	}

	public SwaggerShopRequest( final String action, final int itemId, final int quantity )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, action, itemId, quantity );
	}

	public SwaggerShopRequest( final String action, final int itemId )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, action, itemId );
	}

	@Override
	public void run()
	{
		if ( this.action != null ) {
			if ( KoLCharacter.isHardcore() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't spend your swagger in Hardcore." );
				return;
			}

			if ( KoLCharacter.inRonin() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't spend your swagger until you get out of Ronin." );
				return;
			}
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		SwaggerShopRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = SwaggerShopRequest.SWAGGER_SHOP;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			// Parse current swagger
			CoinMasterRequest.parseBalance( data, responseText );
			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim peevpee.php?place=shop&action=buy
		if ( !urlString.startsWith( "peevpee.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "place=shop" ) == -1 && urlString.indexOf( "action=buy" ) == -1 )
		{
			return false;
		}

		CoinmasterData data = SwaggerShopRequest.SWAGGER_SHOP;
		return CoinMasterRequest.registerRequest( data, urlString, true );
	}

	public static String accessible()
	{
		if ( KoLCharacter.isHardcore() || KoLCharacter.inRonin() )
		{
			return "Characters in Hardcore or Ronin cannot redeem Swagger";
		}
		return null;
	}
}
