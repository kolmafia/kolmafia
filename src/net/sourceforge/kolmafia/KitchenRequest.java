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

public class KitchenRequest extends KoLRequest
{
	private int price;
	private String itemName;

	public KitchenRequest( String name )
	{
		super( "cafe.php" );
		this.addFormField( "cafeid", "3" );
		this.addFormField( "pwd" );
		this.addFormField( "action", "CONSUME!" );

		this.itemName = name;
		int itemId = TradeableItemDatabase.getItemId( name );
		this.price = Math.max( 1, TradeableItemDatabase.getPriceById( itemId ) ) * 3;

		this.addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public void run()
	{
		if ( !KoLCharacter.inBadMoon() )
			return;

		if ( this.price == 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Hell's Kitchen doesn't sell that." );
			return;
		}

		if ( this.price > KoLCharacter.getAvailableMeat() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds." );
			return;
		}

		if ( this.itemName == null )
			return;

		if ( this.itemName.equals( "Imp Ale" ) && !ConsumeItemRequest.allowBoozeConsumption( TradeableItemDatabase.getInebriety( this.itemName ) ) )
			return;

		KoLmafia.updateDisplay( "Visiting Hell's Kitchen..." );
		super.run();
	}

	public void processResults()
	{
		if ( this.responseText.indexOf( "This is not currently available to you.") != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Couldn't find the kitchen." );
			return;
		}

		if ( this.responseText.indexOf( "You're way too drunk already." ) != -1 ||
		     this.responseText.indexOf( "You're too full to eat that." ) != -1 )
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
		KoLmafia.updateDisplay( "Goodie purchased." );
	}

	public static final void getMenu()
	{
		kitchenItems.clear();

		addKitchenItem( "Jumbo Dr. Lucifer", 150 );
		addKitchenItem( "Brimstone Chicken Sandwich", 300 );
		addKitchenItem( "Lord of the Flies-sized fries", 300 );
		addKitchenItem( "Double Bacon Beelzeburger", 300 );
		addKitchenItem( "Imp Ale", 75 );

		ConcoctionsDatabase.getUsables().sort();
		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	private static final void addKitchenItem( String itemName, int price )
	{
		kitchenItems.add( itemName );

		Concoction junk = new Concoction( itemName, price );
		ConcoctionsDatabase.getUsables().remove( junk );
		ConcoctionsDatabase.getUsables().add( junk );
	}

	public static final boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "cafe.php" ) )
			return false;

		Matcher idMatcher = SendMessageRequest.ITEMID_PATTERN.matcher( urlString );
		if ( !idMatcher.find() )
			return true;

		int itemId = StaticEntity.parseInt( idMatcher.group(1) );
		String itemName = TradeableItemDatabase.getItemName( itemId );
		boolean booze = itemName.equals( "Imp Ale" );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( ( booze ? "drink" : "eat" ) + " 1 " + itemName );


		if ( !booze )
		{
			int fullness = TradeableItemDatabase.getFullness( itemName );
			if ( fullness > 0 && KoLCharacter.getFullness() + fullness <= KoLCharacter.getFullnessLimit() )
				KoLSettings.setUserProperty( "currentFullness", String.valueOf( KoLCharacter.getFullness() + fullness ) );
		}

		return true;
	}
}
