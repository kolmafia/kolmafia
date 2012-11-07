/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CafeRequest
	extends GenericRequest
{
	protected static final Pattern CAFE_PATTERN = Pattern.compile( "cafe.php.*cafeid=(\\d*)", Pattern.DOTALL );
	protected static final Pattern ITEM_PATTERN = Pattern.compile( "whichitem=(-?\\d*)", Pattern.DOTALL );
	private static final LockableListModel existing = new LockableListModel();
	private static final AdventureResult LARP = ItemPool.get( ItemPool.LARP_MEMBERSHIP_CARD, 1 );
	private static final GenericRequest LARP_REQUEST = new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, new AdventureResult[] { CafeRequest.LARP } );

	protected String name = "";
	protected String itemName = null;
	protected boolean isPurchase = false;
	protected int price = 0;
	protected int fullness = 0;
	protected int inebriety = 0;

	public CafeRequest( final String name, final String cafeId )
	{
		super( "cafe.php" );
		this.addFormField( "cafeid", cafeId );
		this.name = name;
	}

	public static void pullLARPCard()
	{
		// You can only ever have a single LARP card.

		if ( LARP.getCount( KoLConstants.inventory ) > 0 )
		{
			return;
		}

		if ( LARP.getCount( KoLConstants.closet ) > 0 )
		{
			return;
		}

		// If you have a LARP card in storage, pull it.
		if ( KoLCharacter.canInteract() && LARP.getCount( KoLConstants.storage ) > 0 )
		{
			RequestThread.postRequest( LARP_REQUEST );
		}
	}

	public void setItem( final String itemName, final int itemId, final int price )
	{
		this.isPurchase = true;
		this.itemName = itemName;
		this.price = price;
		this.fullness = ItemDatabase.getFullness( itemName );
		this.inebriety = ItemDatabase.getInebriety( itemName );
		this.addFormField( "action", "CONSUME!" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public static int discountedPrice( int price )
	{
		int count = LARP.getCount( KoLConstants.inventory ) + LARP.getCount( KoLConstants.closet );

		if ( count > 0 )
		{
			price = (int) Math.ceil( 0.90f * (float) price );
		}

		return price;
	}

	@Override
	public void run()
	{
		if ( !this.isPurchase )
		{
			// Just visiting to peek at the menu
			KoLmafia.updateDisplay( "Visiting " + this.name + "..." );
			super.run();
			return;
		}

		if ( this.fullness > 0 && !KoLCharacter.canEat() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't eat. Why are you here?" );
			return;
		}

		if ( this.inebriety > 0 && !KoLCharacter.canDrink() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't drink. Why are you here?" );
			return;
		}

		if ( this.price == 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, this.name + " doesn't sell that." );
			return;
		}

		if ( this.price > KoLCharacter.getAvailableMeat() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient funds." );
			return;
		}

		if ( this.itemName == null )
		{
			return;
		}

		String advGain = ItemDatabase.getAdvRangeByName( this.itemName );
		int PvPGain = ItemDatabase.getPvPFights( this.itemName );
		if ( this.inebriety > 0 && !DrinkItemRequest.allowBoozeConsumption( this.inebriety, 1, advGain, PvPGain ) )
		{
			return;
		}

		KoLmafia.updateDisplay( "Purchasing " + this.itemName + " at the " + this.name + "..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		if ( !this.isPurchase )
		{
			return;
		}

		if ( this.responseText.indexOf( "This is not currently available to you." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Couldn't find " + this.name );
			return;
		}

		if ( this.responseText.indexOf( "You're way too drunk already." ) != -1 || this.responseText.indexOf( "You're too full to eat that." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Consumption limit reached." );
			return;
		}

		if ( this.responseText.indexOf( "You can't afford that item." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Insufficient funds." );
			return;
		}

		// If distention pill is active, no message will be printed, but it will take effect nonetheless.
		Preferences.setBoolean( "distentionPillActive", false );

		KoLmafia.updateDisplay( "Goodie purchased." );
	}

	protected static void addMenuItem( final LockableListModel menu, final String itemName, final int price )
	{
		menu.add( itemName );

		LockableListModel usables = ConcoctionDatabase.getUsables();
		Concoction item = new Concoction( itemName, price );
		int index = usables.indexOf( item );
		if ( index != -1 )
		{
			CafeRequest.existing.add( usables.remove( index ) );
		}
		else
		{
			CafeRequest.existing.add( null );
		}
		usables.add( item );
	}

	public static final void reset( final LockableListModel menu )
	{
		// Restore usable list with original concoction
		LockableListModel usables = ConcoctionDatabase.getUsables();
		for ( int i = 0; i < menu.size(); ++i )
		{
			String itemName = (String) menu.get( i );
			Concoction junk = new Concoction( itemName, -1 );
			usables.remove( junk );
			Object old = CafeRequest.existing.get( i );
			if ( old != null )
			{
				usables.add( old );
			}
		}
		menu.clear();
		CafeRequest.existing.clear();
	}

	public static boolean registerRequest( final String urlString )
	{
		Matcher matcher = CafeRequest.CAFE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return false;
		}

		matcher = CafeRequest.ITEM_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
		String itemName = ItemDatabase.getItemName( itemId );
		int price = ItemDatabase.getPriceById( itemId ) * 3;
		if ( price < 0 )
		{
			// Not a real item. Get price from the cafe...
			price = 0;
		}
		CafeRequest.registerItemUsage( itemName, price );
		return true;
	}

	public static final void registerItemUsage( final String itemName, int price )
	{
		int inebriety = ItemDatabase.getInebriety( itemName );
		String consume = inebriety > 0 ? "drink" : "eat";

		price = CafeRequest.discountedPrice( price );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Buy and " + consume + " 1 " + itemName + " for " + price + " Meat" );

		if ( inebriety > 0 )
		{
			return;
		}

		int fullness = ItemDatabase.getFullness( itemName );
		if ( fullness > 0 )
		{
			if ( KoLCharacter.getFullness() + fullness <= KoLCharacter.getFullnessLimit() )
			{
				Preferences.increment( "currentFullness", fullness );
			}
			return;
		}
	}
}
