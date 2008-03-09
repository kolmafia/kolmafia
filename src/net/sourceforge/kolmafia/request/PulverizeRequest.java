/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class PulverizeRequest
	extends GenericRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "smashitem=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );

	private AdventureResult item;

	public PulverizeRequest( final AdventureResult item )
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

	public void useMalus( final String itemName )
	{
		if ( itemName == null || !ItemDatabase.contains( itemName ) )
		{
			return;
		}

		int itemId = ItemDatabase.getItemId( itemName );
		AdventureResult[] ingredients = ConcoctionDatabase.getIngredients( itemId );

		if ( ingredients == null || ingredients.length == 0 )
		{
			return;
		}

		int amountNeeded = ingredients[ 0 ].getCount( KoLConstants.inventory ) / 5;
		if ( amountNeeded == 0 )
		{
			return;
		}

		CreateItemRequest icr = CreateItemRequest.getInstance( itemId );
		if ( icr == null )
		{
			return;
		}

		icr.setQuantityNeeded( amountNeeded );
		icr.run();
	}

	public void run()
	{
		if ( Preferences.getBoolean( "mementoListActive" ) && KoLConstants.mementoList.contains( this.item ) )
		{
			return;
		}

		if ( this.item.getCount( KoLConstants.inventory ) == this.item.getCount() && !KoLConstants.junkList.contains( this.item ) )
		{
			KoLConstants.junkList.add( this.item );
		}

		if ( KoLConstants.singletonList.contains( this.item ) && !KoLConstants.closet.contains( this.item ) )
		{
			this.item = this.item.getInstance( this.item.getCount() - 1 );
			this.addFormField( "quantity", String.valueOf( this.item.getCount() ) );
		}

		switch ( ItemDatabase.getConsumptionType( this.item.getItemId() ) )
		{
		case KoLConstants.EQUIP_ACCESSORY:
		case KoLConstants.EQUIP_HAT:
		case KoLConstants.EQUIP_PANTS:
		case KoLConstants.EQUIP_SHIRT:
		case KoLConstants.EQUIP_WEAPON:
		case KoLConstants.EQUIP_OFFHAND:
			break;

		default:

			if ( !KoLCharacter.isMuscleClass() || !KoLCharacter.hasSkill( "Pulverize" ) )
			{
			}
			if ( this.item.getName().endsWith( "powder" ) )
			{
				this.useMalus( StringUtilities.singleStringReplace( this.item.getName(), "powder", "nugget" ) );
				this.useMalus( StringUtilities.singleStringReplace( this.item.getName(), "powder", "wad" ) );
			}
			else if ( this.item.getName().endsWith( "nugget" ) )
			{
				this.useMalus( StringUtilities.singleStringReplace( this.item.getName(), "nugget", "wad" ) );
			}

			return;
		}

		if ( !KoLCharacter.hasSkill( "Pulverize" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't know how to pulverize objects." );
			return;
		}

		if ( !InventoryManager.retrieveItem( ItemPool.TENDER_HAMMER ) )
		{
			return;
		}

		if ( this.item.getCount( KoLConstants.inventory ) < this.item.getCount() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a " + this.item.getName() + "." );
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
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "The " + this.item.getName() + " could not be smashed." );
			ResultProcessor.processResult( this.item );
			return;
		}

		// Remove old item and notify the user of success.
		KoLmafia.updateDisplay( this.item + " smashed." );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "smith.php" ) || urlString.indexOf( "action=pulverize" ) == -1 )
		{
			return false;
		}

		Matcher itemMatcher = PulverizeRequest.ITEMID_PATTERN.matcher( urlString );
		Matcher quantityMatcher = PulverizeRequest.QUANTITY_PATTERN.matcher( urlString );

		if ( itemMatcher.find() && quantityMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
			String name = ItemDatabase.getItemName( itemId );

			if ( name == null )
			{
				return true;
			}

			int quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );

			ResultProcessor.processResult( new AdventureResult( itemId, 0 - quantity ) );
			RequestLogger.updateSessionLog( "pulverize " + quantity + " " + name );
		}

		return true;
	}
}
