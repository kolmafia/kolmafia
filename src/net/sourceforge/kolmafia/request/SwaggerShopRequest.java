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

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

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
			)
		{
			@Override
			public final int getBuyPrice( final int itemId )
			{
				switch ( itemId )
				{
				case ItemPool.BLACK_BARTS_BOOTY:
					return Preferences.getInteger( "blackBartsBootyCost" );
				case ItemPool.HOLIDAY_FUN_BOOK:
					return Preferences.getInteger( "holidayHalsBookCost" );
				case ItemPool.ANTAGONISTIC_SNOWMAN_KIT:
					return Preferences.getInteger( "antagonisticSnowmanKitCost" );
				case ItemPool.MAP_TO_KOKOMO:
					return Preferences.getInteger( "mapToKokomoCost" );
				case ItemPool.ESSENCE_OF_BEAR:
					return Preferences.getInteger( "essenceOfBearCost" );
				case ItemPool.MANUAL_OF_NUMBEROLOGY:
					return Preferences.getInteger( "manualOfNumberologyCost" );
				case ItemPool.ESSENCE_OF_ANNOYANCE:
					return Preferences.getInteger( "essenceOfAnnoyanceCost" );
				}

				return super.getBuyPrice( itemId );
			}
		};

	static
	{
		SWAGGER_SHOP.plural = "swagger";
	}

	public SwaggerShopRequest()
	{
		super( SwaggerShopRequest.SWAGGER_SHOP );
	}

	public SwaggerShopRequest( final boolean buying, final AdventureResult [] attachments )
	{
		super( SwaggerShopRequest.SWAGGER_SHOP, buying, attachments );
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
	// You've earned 0 swagger during bear season!
	// You've earned 0 swagger during a numeric season!

	private static final Pattern SEASON_PATTERN = Pattern.compile( "You've earned ([\\d,]+) swagger during (?:a |an |)(pirate|holiday|ice|drunken|bear|numeric) season" );

	// <tr><td><img style='vertical-align: middle' class=hand src='http://images.kingdomofloathing.com/itemimages/radio.gif' onclick='descitem(475026869)'></td><td valign=center><b><span onclick='descitem(475026869)'>Huggler Radio<span>&nbsp;&nbsp;&nbsp;&nbsp;</b></td><td><form style="padding:0;margin:0;"><input type="hidden" name="action" value="buy" /><input type="hidden" name="place" value="shop" /><input type="hidden" name="pwd" value="0c6efe5fe0c70235b340073785255041" /><input type="hidden" name="whichitem" value="5656" /><input type="submit" class="button" value="Buy (50 swagger)" /></form></td></tr>

	private static final Pattern ITEM_PATTERN =
		Pattern.compile( "<tr><td><img.*?onclick='descitem\\((.*?)\\)'.*?<b>(?:<[^>]*>)?([^<]*).*?</b>.*?name=\"whichitem\" value=\"(.*?)\".*?\\((.*?) swagger\\).*?</td></tr>", Pattern.DOTALL );

	private static final AdventureResult BLACK_BARTS_BOOTY = ItemPool.get( ItemPool.BLACK_BARTS_BOOTY, 1 );
	private static final AdventureResult HOLIDAY_FUN_BOOK = ItemPool.get( ItemPool.HOLIDAY_FUN_BOOK, 1 );
	private static final AdventureResult ANTAGONISTIC_SNOWMAN_KIT = ItemPool.get( ItemPool.ANTAGONISTIC_SNOWMAN_KIT, 1 );
	private static final AdventureResult MAP_TO_KOKOMO = ItemPool.get( ItemPool.MAP_TO_KOKOMO, 1 );
	private static final AdventureResult ESSENCE_OF_BEAR = ItemPool.get( ItemPool.ESSENCE_OF_BEAR, 1 );
	private static final AdventureResult MANUAL_OF_NUMBEROLOGY = ItemPool.get( ItemPool.MANUAL_OF_NUMBEROLOGY, 1 );
	private static final AdventureResult ESSENCE_OF_ANNOYANCE = ItemPool.get( ItemPool.ESSENCE_OF_ANNOYANCE, 1 );

	public static void parseResponse( final String urlString, final String responseText )
	{
		CoinmasterData data = SwaggerShopRequest.SWAGGER_SHOP;

		String action = GenericRequest.getAction( urlString );
		if ( action != null )
		{
			CoinMasterRequest.parseResponse( data, urlString, responseText );
			return;
		}

		// Learn new items by simply visiting the Swagger Shop
		// Refresh the Coin Master inventory every time we visit.

		LockableListModel<AdventureResult> items = SwaggerShopRequest.buyItems;
		Map prices = SwaggerShopRequest.buyPrices;
		items.clear();
		prices.clear();

		Matcher matcher = ITEM_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			String descId = matcher.group(1);
			String itemName = matcher.group(2);
			int itemId = StringUtilities.parseInt( matcher.group(3) );
			int price = StringUtilities.parseInt( matcher.group(4) );

			String match = ItemDatabase.getItemDataName( itemId );
			if ( match == null || !match.equals( itemName ) )
			{
				ItemDatabase.registerItem( itemId, itemName, descId );
			}

			// Add it to the Swagger Shop inventory
			AdventureResult item = ItemPool.get( itemId, PurchaseRequest.MAX_QUANTITY );
			items.add( item );
			prices.put( itemId, price );

			switch ( itemId )
			{
			case ItemPool.BLACK_BARTS_BOOTY:
				Preferences.setInteger( "blackBartsBootyCost", price );
				break;
			case ItemPool.HOLIDAY_FUN_BOOK:
				Preferences.setInteger( "holidayHalsBookCost", price );
				break;
			case ItemPool.ANTAGONISTIC_SNOWMAN_KIT:
				Preferences.setInteger( "antagonisticSnowmanKitCost", price );
				break;
			case ItemPool.MAP_TO_KOKOMO:
				Preferences.setInteger( "mapToKokomoCost", price );
				break;
			case ItemPool.ESSENCE_OF_BEAR:
				Preferences.setInteger( "essenceOfBearCost", price );
				break;
			case ItemPool.MANUAL_OF_NUMBEROLOGY:
				Preferences.setInteger( "manualOfNumberologyCost", price );
				break;
			case ItemPool.ESSENCE_OF_ANNOYANCE:
				Preferences.setInteger( "essenceOfAnnoyanceCost", price );
				break;
			}
		}

		// Find availability/cost of conditional items
		Preferences.setBoolean( "blackBartsBootyAvailable", items.contains( SwaggerShopRequest.BLACK_BARTS_BOOTY ) );
		Preferences.setBoolean( "holidayHalsBookAvailable", items.contains( SwaggerShopRequest.HOLIDAY_FUN_BOOK ) );
		Preferences.setBoolean( "antagonisticSnowmanKitAvailable", items.contains( SwaggerShopRequest.ANTAGONISTIC_SNOWMAN_KIT ) );
		Preferences.setBoolean( "mapToKokomoAvailable", items.contains( SwaggerShopRequest.MAP_TO_KOKOMO ) );
		Preferences.setBoolean( "essenceOfBearAvailable", items.contains( SwaggerShopRequest.ESSENCE_OF_BEAR ) );
		Preferences.setBoolean( "manualOfNumberologyAvailable", items.contains( SwaggerShopRequest.MANUAL_OF_NUMBEROLOGY ) );
		Preferences.setBoolean( "essenceOfAnnoyanceAvailable", items.contains( SwaggerShopRequest.ESSENCE_OF_ANNOYANCE ) );

		// Register the purchase requests, now that we know what is available
		data.registerPurchaseRequests();

		// Parse current swagger
		CoinMasterRequest.parseBalance( data, responseText );

		// If this is a special season, determine how much swagger has been found
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
			else if ( season.equals( "bear" ) )
			{
				Preferences.setInteger( "bearSwagger", seasonSwagger );
			}
			else if ( season.equals( "numeric" ) )
			{
				Preferences.setInteger( "numericSwagger", seasonSwagger );
			}
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		// We only claim peevpee.php?place=shop&action=buy
		if ( !urlString.startsWith( "peevpee.php" ) )
		{
			return false;
		}

		if ( !urlString.contains( "place=shop" ) && !urlString.contains( "action=buy" ) )
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
