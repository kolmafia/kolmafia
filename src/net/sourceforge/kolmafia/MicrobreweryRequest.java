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

public class MicrobreweryRequest extends KoLRequest
{
	private static AdventureResult dailySpecial = null;
	private static final Pattern SPECIAL_PATTERN = Pattern.compile( "<input type=radio name=whichitem value=(\\d+)>", Pattern.DOTALL );

	private int price;
	private String itemName;
	private boolean isPurchase;

	public static final AdventureResult getDailySpecial()
	{
		if ( microbreweryItems.isEmpty() && KoLCharacter.inMoxieSign() )
			RequestThread.postRequest( new MicrobreweryRequest() );

		return dailySpecial;
	}

	public MicrobreweryRequest()
	{
		super( "brewery.php" );

		this.price = 0;
		this.itemName = null;
		this.isPurchase = false;
	}

	public MicrobreweryRequest( String name )
	{
		super( "brewery.php" );
		this.addFormField( "action", "Yep." );

		this.isPurchase = true;

		int itemId = 0;
		this.price = 0;

		// Parse item itemName and price

		switch ( microbreweryItems.indexOf( name ) )
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
		if ( !KoLCharacter.inMoxieSign() )
			return;

		if ( this.isPurchase )
		{
			if ( this.price == 0 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "The microbrewery doesn't sell that." );
				return;
			}

			if ( this.price > KoLCharacter.getAvailableMeat() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds." );
				return;
			}

			if ( !KoLCharacter.canDrink() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You can't drink. Why are you here?" );
				return;
			}
		}

		if ( this.itemName != null && !ConsumeItemRequest.allowBoozeConsumption( TradeableItemDatabase.getInebriety( this.itemName ) ) )
			return;

		KoLmafia.updateDisplay( "Visiting the micromicrobrewery..." );
		super.run();
	}

	public void processResults()
	{
		if ( this.isPurchase )
		{
			if ( this.responseText.indexOf( "You're way too drunk already." ) != -1 )
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
			KoLmafia.updateDisplay( "Drink purchased." );
			return;
		}

		microbreweryItems.clear();

		addBreweryItem( "Petite Porter", 50 );
		addBreweryItem( "Scrawny Stout", 75 );
		addBreweryItem( "Infinitesimal IPA", 100 );

		Matcher specialMatcher = SPECIAL_PATTERN.matcher( this.responseText );
		if ( specialMatcher.find() )
		{
			int itemId = StaticEntity.parseInt( specialMatcher.group(1) );
			dailySpecial = new AdventureResult( itemId, 1 );

			addBreweryItem( TradeableItemDatabase.getItemName( itemId ),
				Math.max( 1, TradeableItemDatabase.getPriceById( itemId ) ) * 3 );
		}

		ConcoctionsDatabase.getUsables().sort();
		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	private static final void addBreweryItem( String itemName, int price )
	{
		microbreweryItems.add( itemName );

		Concoction brew = new Concoction( itemName, price );
		ConcoctionsDatabase.getUsables().remove( brew );
		ConcoctionsDatabase.getUsables().add( brew );
	}

	public static final boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "brewery.php" ) )
			return false;

		Matcher idMatcher = SendMessageRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !idMatcher.find() )
			return true;

		int itemId = StaticEntity.parseInt( idMatcher.group(1) );
		String itemName = "";

		switch ( itemId )
		{
		case -1:
			itemName = "Petite Porter";
			break;

		case -2:
			itemName = "Scrawny Stout";
			break;

		case -3:
			itemName = "Infinitesimal IPA";
			break;

		default:
			itemName = TradeableItemDatabase.getItemName( itemId );
			break;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "drink 1 " + itemName );
		return true;
	}
}
