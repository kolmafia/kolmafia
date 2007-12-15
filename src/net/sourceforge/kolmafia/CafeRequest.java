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

public class CafeRequest extends KoLRequest
{
	protected static final Pattern CAFE_PATTERN = Pattern.compile( "cafe.php.*cafeid=(\\d*).*whichitem=(-?\\d*)", Pattern.DOTALL );
	private static final LockableListModel existing = new LockableListModel();
	protected String name = "";
	protected String itemName = null;
	protected boolean isPurchase = false;
	protected int price = 0;
	protected int fullness = 0;
	protected int inebriety = 0;

	public CafeRequest( String name, String cafeId )
	{
		super( "cafe.php" );
		this.addFormField( "cafeid", cafeId );
		this.name = name;
	}

	public void setItem( String itemName, int itemId, int price )
	{
		this.isPurchase = true;
		this.itemName = itemName;
		this.price = price;
		this.fullness = TradeableItemDatabase.getFullness( itemName );
		this.inebriety = TradeableItemDatabase.getInebriety( itemName );
		this.addFormField( "pwd" );
		this.addFormField( "action", "CONSUME!" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public void run()
	{
		if ( !isPurchase )
		{
			// Just visiting to peek at the menu
			KoLmafia.updateDisplay( "Visiting " + name + "..." );
			super.run();
			return;
		}

		if ( fullness > 0 && !KoLCharacter.canEat() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't eat. Why are you here?" );
			return;
		}

		if ( inebriety > 0 && !KoLCharacter.canDrink() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't drink. Why are you here?" );
			return;
		}

		if ( this.price == 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, name + " doesn't sell that." );
			return;
		}

		if ( this.price > KoLCharacter.getAvailableMeat() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Insufficient funds." );
			return;
		}

		if ( this.itemName == null )
			return;

		if (  inebriety > 0 && !ConsumeItemRequest.allowBoozeConsumption( inebriety ) )
			return;

		KoLmafia.updateDisplay( "Visiting " + name + "..." );
		super.run();
	}

	public void processResults()
	{
		if ( !isPurchase )
			return;

		if ( this.responseText.indexOf( "This is not currently available to you.") != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Couldn't find " + name );
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

		KoLmafia.updateDisplay( "Goodie purchased." );
	}

	protected static void addMenuItem( LockableListModel menu, String itemName, int price )
	{
		menu.add( itemName );

		LockableListModel usables = ConcoctionsDatabase.getUsables();
		Concoction item = new Concoction( itemName, price );
		int index = usables.indexOf( item );
		if ( index != -1 )
			existing.add( usables.remove( index ) );
		else
			existing.add( null );
		usables.add( item );
	}

	public static final void reset( LockableListModel menu )
	{
		// Restore usable list with original concoction
		for ( int i = 0; i < menu.size(); ++i )
		{
			String itemName = (String)menu.get(i);
			Concoction junk = new Concoction( itemName, -1 );
			ConcoctionsDatabase.getUsables().remove( junk );
			Object old = existing.get(i);
			if ( old != null )
				ConcoctionsDatabase.getUsables().add( old );
		}
		menu.clear();
		existing.clear();
	}

	public static boolean registerRequest( String urlString )
	{
		Matcher matcher = CAFE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
			return false;

		int itemId = StaticEntity.parseInt( matcher.group(2) );
		String itemName = TradeableItemDatabase.getItemName( itemId );
		int price = Math.max( 1, TradeableItemDatabase.getPriceById( itemId ) ) * 3;
		registerItemUsage( itemName, price );
		return true;
	}

	public static final void registerItemUsage( String itemName, int price )
	{
		int inebriety = TradeableItemDatabase.getInebriety( itemName );
		String consume = ( inebriety > 0 ) ? "drink" : "eat";

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Buy and " + consume + " 1 " + itemName + " for " + price + " Meat" );

		StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - price ) );

		if ( inebriety > 0 )
		{
			return;
		}

		int fullness = TradeableItemDatabase.getFullness( itemName );
		if ( fullness > 0 )
		{
			if ( KoLCharacter.getFullness() + fullness <= KoLCharacter.getFullnessLimit() )
				KoLSettings.setUserProperty( "currentFullness", String.valueOf( KoLCharacter.getFullness() + fullness ) );
			return;
		}
	}
}
