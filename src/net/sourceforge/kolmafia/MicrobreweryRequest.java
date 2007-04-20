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
	private static final Pattern COST_PATTERN = Pattern.compile( "(.*?) \\((\\d*) Meat\\)" );
	private static final Pattern AVAILABLE_PATTERN = Pattern.compile( "<td>([^<]*? \\(.*? Meat\\))</td>" );

	private int price;
	private String itemName;
	private boolean isPurchase;

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
		addFormField( "action", "Yep." );

		this.isPurchase = true;
		this.price = 0;

		// Parse item itemName and price
		Matcher itemMatcher = COST_PATTERN.matcher( name );
		int itemId = 0;

		if ( itemMatcher.find() )
		{
			this.itemName = itemMatcher.group(1);
			this.price = Integer.parseInt ( itemMatcher.group(2) );

			// Get the menu the microbrewery offers today
			if ( microbreweryItems.isEmpty() )
				(new MicrobreweryRequest()).run();

			// Find the item in the menu
			for ( int i = 0; i < 3; i++ )
				if ( microbreweryItems.get(i).equals( name ) )
					itemId = -1 - i;

			if ( itemId == 0 )
				itemId = TradeableItemDatabase.getItemId( itemName );
		}

		addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public void run()
	{
		if ( !KoLCharacter.inMoxieSign() )
			return;

		if ( isPurchase )
		{
			if ( price == 0 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "The micromicromicrobrewery doesn't sell that." );
				return;
			}

			if ( price > KoLCharacter.getAvailableMeat() )
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

		if ( itemName != null && !ConsumeItemRequest.allowBoozeConsumption( TradeableItemDatabase.getInebriety( itemName ) ) )
			return;

		KoLmafia.updateDisplay( "Visiting the micromicrobrewery..." );
		super.run();
	}

	public void processResults()
	{
		if ( isPurchase )
		{
			if ( responseText.indexOf( "You're way too drunk already." ) != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Consumption limit reached." );
				return;
			}

			if ( responseText.indexOf( "You can't afford that item.") != -1 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds." );
				return;
			}

			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - price ) );
			KoLmafia.updateDisplay( "Drink purchased." );
			return;
		}

		Matcher purchaseMatcher = AVAILABLE_PATTERN.matcher( responseText );
		while ( purchaseMatcher.find() )
		{
			String itemName = purchaseMatcher.group(1);
			microbreweryItems.add( itemName );

			Matcher itemMatcher = COST_PATTERN.matcher( itemName );
			itemMatcher.find();

			Concoction brew = new Concoction( itemMatcher.group(1), StaticEntity.parseInt( itemMatcher.group(2) ) );
			ConcoctionsDatabase.getUsables().remove( brew );
			ConcoctionsDatabase.getUsables().add( brew );
		}

		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	public static boolean registerRequest( String urlString )
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
