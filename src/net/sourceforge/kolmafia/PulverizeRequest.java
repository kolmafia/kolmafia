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

public class PulverizeRequest extends KoLRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "smashitem=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );

	private AdventureResult item;

	public PulverizeRequest( AdventureResult item )
	{
		super( "smith.php" );
		this.addFormField( "action", "pulverize" );
		this.addFormField( "pwd" );

		this.item = item;
		this.addFormField( "smashitem", String.valueOf( item.getItemId() ) );
		this.addFormField( "quantity", String.valueOf( item.getCount() ) );

		// 1 to confirm smashing untradables
		this.addFormField( "conftrade", "1" );
	}

	public void useMalus( String itemName )
	{
		if ( itemName == null || !TradeableItemDatabase.contains( itemName ) )
			return;

		int itemId = TradeableItemDatabase.getItemId( itemName );
		AdventureResult [] ingredients = ConcoctionsDatabase.getIngredients( itemId );

		if ( ingredients == null || ingredients.length == 0 )
			return;

		int amountNeeded = ingredients[0].getCount( inventory ) / 5;
		if ( amountNeeded == 0 )
			return;

		ItemCreationRequest icr = ItemCreationRequest.getInstance( itemId );
		if ( icr == null )
			return;

		icr.setQuantityNeeded( amountNeeded );
		icr.run();
	}

	public void run()
	{
		if ( StaticEntity.getBooleanProperty( "mementoListActive" ) && mementoList.contains( this.item ) )
			return;

		if ( item.getCount( inventory ) == item.getCount() )
		{
			if ( !postRoninJunkList.contains( item ) )
				postRoninJunkList.add( item );

			if ( !KoLCharacter.canInteract() && !preRoninJunkList.contains( item ) )
				preRoninJunkList.add( item );
		}

		switch ( TradeableItemDatabase.getConsumptionType( this.item.getItemId() ) )
		{
		case EQUIP_ACCESSORY:
		case EQUIP_HAT:
		case EQUIP_PANTS:
		case EQUIP_SHIRT:
		case EQUIP_WEAPON:
		case EQUIP_OFFHAND:
			break;

		default:

			if ( !KoLCharacter.isMuscleClass() || !KoLCharacter.hasSkill( "Pulverize" ) )
			{
			}
			if ( this.item.getName().endsWith( "powder" ) )
			{
				this.useMalus( StaticEntity.singleStringReplace( this.item.getName(), "powder", "nugget" ) );
				this.useMalus( StaticEntity.singleStringReplace( this.item.getName(), "powder", "wad" ) );
			}
			else if ( this.item.getName().endsWith( "nugget" ) )
			{
				this.useMalus( StaticEntity.singleStringReplace( this.item.getName(), "nugget", "wad" ) );
			}

			return;
		}

		if ( !KoLCharacter.hasSkill( "Pulverize" ) )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't know how to pulverize objects." );
			return;
		}

		if ( !AdventureDatabase.retrieveItem( ConcoctionsDatabase.HAMMER ) )
			return;

		if ( this.item.getCount( inventory ) < this.item.getCount() )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have a " + this.item.getName() + "." );
			return;
		}

		KoLmafia.updateDisplay( "Pulverizing " + this.item.getName() + "..." );
		super.run();
	}

	public void processResults()
	{
		// "That's too important to pulverize."
		// "That's not something you can pulverize."

		if ( this.responseText.indexOf( "too important to pulverize" ) != -1 || this.responseText.indexOf( "not something you can pulverize" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "The " + this.item.getName() + " could not be smashed." );
			StaticEntity.getClient().processResult( this.item );
			return;
		}

		// Remove old item and notify the user of success.
		KoLmafia.updateDisplay( this.item + " smashed." );
	}

	public static boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "smith.php" ) || urlString.indexOf( "action=pulverize" ) == -1 )
			return false;

		Matcher itemMatcher = ITEMID_PATTERN.matcher( urlString );
		Matcher quantityMatcher = QUANTITY_PATTERN.matcher( urlString );

		if ( itemMatcher.find() && quantityMatcher.find() )
		{
			int itemId = StaticEntity.parseInt( itemMatcher.group(1) );
			String name = TradeableItemDatabase.getItemName( itemId );

			if ( name == null )
				return true;

			int quantity = StaticEntity.parseInt( quantityMatcher.group(1) );

			StaticEntity.getClient().processResult( new AdventureResult( itemId, 0 - quantity ) );
			RequestLogger.updateSessionLog( "pulverize " + quantity + " " + name );
		}

		return true;
	}
}
