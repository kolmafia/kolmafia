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
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.ConcoctionsDatabase.Concoction;

public class RestaurantRequest extends CafeRequest
{
	private static AdventureResult dailySpecial = null;
	private static final Pattern SPECIAL_PATTERN = Pattern.compile( "<input type=radio name=whichitem value=(\\d+)>", Pattern.DOTALL );

	public static final AdventureResult getDailySpecial()
	{
		if ( restaurantItems.isEmpty() )
			getMenu();

		return dailySpecial;
	}

	public RestaurantRequest()
	{	super( "Chez Snoot&eacute;e", "1" );
	}

	public RestaurantRequest( String name )
	{
		super( "Chez Snoot&eacute;e", "1" );
		this.isPurchase = true;

		int itemId = 0;
		int price = 0;

		switch ( restaurantItems.indexOf( name ) )
		{
		case 0:
			itemId = -1;
			price = 50;
			break;

		case 1:
			itemId = -2;
			price = 75;
			break;

		case 2:
			itemId = -3;
			price = 100;
			break;

		case 3:
			itemId = TradeableItemDatabase.getItemId( name );
			price = Math.max( 1, TradeableItemDatabase.getPriceById( itemId ) ) * 3;
			break;
		}

		setItem( name, itemId, price );
	}

	public void run()
	{
		if ( !KoLCharacter.inMysticalitySign() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't find " + this.name );
			return;
		}

		super.run();
	}

	public void processResults()
	{
		if ( this.isPurchase )
		{
			super.processResults();
			return;
		}

		Matcher specialMatcher = SPECIAL_PATTERN.matcher( this.responseText );
		if ( specialMatcher.find() )
		{
			int itemId = StaticEntity.parseInt( specialMatcher.group(1) );
			dailySpecial = new AdventureResult( itemId, 1 );

		}
	}

	public static final void getMenu()
	{
		if ( !KoLCharacter.inMysticalitySign() )
			return;

		restaurantItems.clear();

		CafeRequest.addMenuItem( restaurantItems, "Peche a la Frog", 50 );
		CafeRequest.addMenuItem( restaurantItems, "As Jus Gezund Heit", 75 );
		CafeRequest.addMenuItem( restaurantItems, "Bouillabaise Coucher Avec Moi", 100 );

		RequestThread.postRequest( new RestaurantRequest() );

		int itemId = dailySpecial.getItemId();
		String name = dailySpecial.getName();
		int price = Math.max( 1, TradeableItemDatabase.getPriceById( itemId ) ) * 3;
		CafeRequest.addMenuItem( restaurantItems, name, price );

		ConcoctionsDatabase.getUsables().sort();
		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	public static final void reset()
	{	CafeRequest.reset( restaurantItems );
	}

	public static final boolean registerRequest( String urlString )
	{
		Matcher matcher = CafeRequest.CAFE_PATTERN.matcher( urlString );
		if ( !matcher.find() || !matcher.group(1).equals( "1" ) )
			return false;

		int itemId = StaticEntity.parseInt( matcher.group(2) );
		String itemName;
		int price;

		switch ( itemId )
		{
		case -1:
			itemName = "Peche a la Frog";
			price = 50;
			break;
		case -2:
			itemName = "As Jus Gezund Heit";
			price = 75;
			break;
		case -3:
			itemName = "Bouillabaise Coucher Avec Moi";
			price = 100;
			break;
		default:
			itemName = TradeableItemDatabase.getItemName( itemId );
			price = Math.max( 1, TradeableItemDatabase.getPriceById( itemId ) ) * 3;
			break;
		}

		CafeRequest.registerItemUsage( itemName, price );
		return true;
	}
}
