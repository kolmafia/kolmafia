/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;

public class RestaurantRequest extends KoLRequest
{
	private static AdventureResult dailySpecial = null;
	private static final Pattern SPECIAL_PATTERN = Pattern.compile( "<input type=radio name=whichitem value=(\\d+)>", Pattern.DOTALL );

	private boolean isPurchase;
	private int price;

	public static final AdventureResult getDailySpecial()
	{
		if ( restaurantItems.isEmpty() && KoLCharacter.inMysticalitySign() )
			RequestThread.postRequest( new RestaurantRequest() );

		return dailySpecial;
	}

	public RestaurantRequest()
	{
		super( "restaurant.php" );
		this.isPurchase = false;
	}

	public RestaurantRequest( String name )
	{
		super( "restaurant.php" );
		this.addFormField( "action", "Yep." );

		this.isPurchase = true;

		int itemId = 0;
		this.price = 0;

		switch ( restaurantItems.indexOf( name ) )
		{
		case 0:
			itemId = -1;
			this.price = 50;
			break;

		case 1:
			itemId = -2;
			this.price = 75;
			break;

		case 2:
			itemId = -3;
			this.price = 100;
			break;

		case 3:
			itemId = TradeableItemDatabase.getItemId( name );
			this.price = Math.max( 1, TradeableItemDatabase.getPriceById( itemId ) ) * 3;
			break;
		}

		this.addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public void run()
	{
		if ( !KoLCharacter.inMysticalitySign() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't find the restaurant." );
			return;
		}

		if ( this.isPurchase )
		{
			if ( this.price == 0 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "The restaurant doesn't sell that." );
				return;
			}

			if ( this.price > KoLCharacter.getAvailableMeat() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds." );
				return;
			}

			if ( !KoLCharacter.canEat() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You can't eat. Why are you here?" );
				return;
			}
		}

		KoLmafia.updateDisplay( "Visiting the restaurant..." );
		super.run();
	}

	public void processResults()
	{
		if ( this.isPurchase )
		{
			if ( this.responseText.indexOf( "You are too full to eat that." ) != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Consumption limit reached." );
				return;
			}

			if ( this.responseText.indexOf( "You can't afford that item.") != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds." );
				return;
			}

			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - this.price ) );
			KoLmafia.updateDisplay( "Food purchased." );
			return;
		}

		restaurantItems.clear();

		addRestaurantItem( "Peche a la Frog", 50 );
		addRestaurantItem( "Au Jus Gezund Heit", 75 );
		addRestaurantItem( "Bouillabaise Coucher Avec Moi", 100 );

		Matcher specialMatcher = SPECIAL_PATTERN.matcher( this.responseText );
		if ( specialMatcher.find() )
		{
			int itemId = StaticEntity.parseInt( specialMatcher.group(1) );
			dailySpecial = new AdventureResult( itemId, 1 );

			addRestaurantItem( TradeableItemDatabase.getItemName( itemId ),
				Math.max( 1, TradeableItemDatabase.getPriceById( itemId ) ) * 3 );
		}

		ConcoctionsDatabase.getUsables().sort();
		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	private static final void addRestaurantItem( String itemName, int price )
	{
		restaurantItems.add( itemName );

		Concoction chez = new Concoction( itemName, price );
		ConcoctionsDatabase.getUsables().remove( chez );
		ConcoctionsDatabase.getUsables().add( chez );
	}

	public static final boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "restaurant.php" ) )
			return false;

		Matcher idMatcher = SendMessageRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !idMatcher.find() )
			return true;

		int fullness = 0;

		int itemId = StaticEntity.parseInt( idMatcher.group(1) );
		String itemName = "";

		switch ( itemId )
		{
		case -1:
			itemName = "Peche a la Frog";
			fullness = 3;
			break;

		case -2:
			itemName = "Au Jus Gezund Heit";
			fullness = 4;
			break;

		case -3:
			itemName = "Bouillabaise Coucher Avec Moi";
			fullness = 5;
			break;

		default:
			itemName = TradeableItemDatabase.getItemName( itemId );
			fullness = TradeableItemDatabase.getFullness( itemName );
			break;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "eat 1 " + itemName );

		if ( fullness > 0 && KoLCharacter.getFullness() + fullness <= KoLCharacter.getFullnessLimit() )
			KoLSettings.setUserProperty( "currentFullness", String.valueOf( KoLCharacter.getFullness() + fullness ) );

		return true;
	}
}
