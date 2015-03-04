/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SwaggerShopRequest
	extends CoinMasterRequest
{
	public static final String master = "The Swagger Shop"; 
	private static final LockableListModel<AdventureResult> buyItems = CoinmastersDatabase.getBuyItems( SwaggerShopRequest.master );
	private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices( SwaggerShopRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) swagger" );

	public static final CoinmasterData SWAGGER_SHOP =
		new CoinmasterData(
			SwaggerShopRequest.master,
			"swagger",
			SwaggerShopRequest.class,
			"swagger",
			"You have 0 swagger",
			false,
			SwaggerShopRequest.TOKEN_PATTERN,
			null,
			"availableSwagger",
			null,
			"peevpee.php?place=shop",
			"buy",
			SwaggerShopRequest.buyItems,
			SwaggerShopRequest.buyPrices,
			null,
			null,
			null,
			null,
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			null,
			null,
			null,
			null,
			true
			);

	static
	{
		SWAGGER_SHOP.plural = "swagger";
	}

	public SwaggerShopRequest()
	{
		super( SwaggerShopRequest.SWAGGER_SHOP );
	}

	public SwaggerShopRequest( final boolean buying, final AdventureResult attachment )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, buying, attachment );
	}

	public SwaggerShopRequest( final boolean buying, final int itemId, final int quantity )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, buying, itemId, quantity );
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

	// You've earned 600 swagger during a pirate season, yarrr.
	// You've earned 2 swagger during a holiday season, fun!
	// You've earned 0 swagger during an ice season, brrrr!
	// You've earned 152 swagger during a drunken season!
	private static final Pattern SEASON_PATTERN = Pattern.compile( "You've earned ([\\d,]+) swagger during an? (pirate|holiday|ice|drunken) season" );

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = SwaggerShopRequest.SWAGGER_SHOP;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			// Parse current swagger
			CoinMasterRequest.parseBalance( data, responseText );

			// Determine how much swagger has been found during a special season
			Matcher seasonMatcher = SwaggerShopRequest.SEASON_PATTERN.matcher( responseText );
			if ( seasonMatcher.find() )
			{
				int seasonSwagger = StringUtilities.parseInt( seasonMatcher.group( 1 ) );
				String season = seasonMatcher.group( 2 );
				if ( season.equals( "pirate" ) )
				{
					Preferences.setInteger( "pirateSwagger", seasonSwagger );
				}
				else if ( season.equals( "holiday" ) )
				{
					Preferences.setInteger( "holidaySwagger", seasonSwagger );
				}
				else if ( season.equals( "ice" ) )
				{
					Preferences.setInteger( "iceSwagger", seasonSwagger );
				}
				else if ( season.equals( "drunken" ) )
				{
					Preferences.setInteger( "drunkenSwagger", seasonSwagger );
				}
				Preferences.setBoolean( "blackBartsBootyAvailable", responseText.contains( "Black Bart's Booty" ) );
				Preferences.setBoolean( "holidayHalsBookAvailable", responseText.contains( "Holiday Hal's Happy-Time Fun Book!" ) );
				Preferences.setBoolean( "antagonisticSnowmanKitAvailable", responseText.contains( "Antagonistic Snowman Kit" ) );
				Preferences.setBoolean( "mapToKokomoAvailable", responseText.contains( "Map to Kokomo" ) );
			}

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
