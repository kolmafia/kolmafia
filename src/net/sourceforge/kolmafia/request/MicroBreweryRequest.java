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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MicroBreweryRequest
	extends CafeRequest
{
	private static AdventureResult dailySpecial = null;
	private static final Pattern SPECIAL_PATTERN =
		Pattern.compile( "Today's Special:.*?<input type=radio name=whichitem value=(\\d+)>", Pattern.DOTALL );

	public static final AdventureResult getDailySpecial()
	{
		if ( KoLConstants.microbreweryItems.isEmpty() )
		{
			MicroBreweryRequest.getMenu();
		}

		return MicroBreweryRequest.dailySpecial;
	}

	public MicroBreweryRequest()
	{
		super( "The Gnomish Micromicrobrewery", "2" );
	}

	public MicroBreweryRequest( final String name )
	{
		super( "The Gnomish Micromicrobrewery", "2" );
		this.isPurchase = true;

		int itemId = 0;
		int price = 0;

		switch ( KoLConstants.microbreweryItems.indexOf( name ) )
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
			itemId = ItemDatabase.getItemId( name );
			price = Math.max( 1, Math.abs( ItemDatabase.getPriceById( itemId ) ) ) * 3;
			break;
		}

		this.setItem( name, itemId, price );
	}

	@Override
	public void run()
	{
		if ( !KoLCharacter.gnomadsAvailable() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't find " + this.name );
			return;
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.isPurchase )
		{
			super.processResults();
			return;
		}

		Matcher specialMatcher = MicroBreweryRequest.SPECIAL_PATTERN.matcher( this.responseText );
		if ( specialMatcher.find() )
		{
			int itemId = StringUtilities.parseInt( specialMatcher.group( 1 ) );
			MicroBreweryRequest.dailySpecial = new AdventureResult( itemId, 1 );

		}
	}

	public static final boolean onMenu( final String name )
	{
		return KoLConstants.microbreweryItems.contains( name );
	}

	public static final void getMenu()
	{
		if ( !KoLCharacter.gnomadsAvailable() || KoLCharacter.inZombiecore() )
		{
			return;
		}

		KoLConstants.microbreweryItems.clear();

		CafeRequest.addMenuItem( KoLConstants.microbreweryItems, "Petite Porter", 50 );
		CafeRequest.addMenuItem( KoLConstants.microbreweryItems, "Scrawny Stout", 75 );
		CafeRequest.addMenuItem( KoLConstants.microbreweryItems, "Infinitesimal IPA", 100 );

		RequestThread.postRequest( new MicroBreweryRequest() );

		if ( MicroBreweryRequest.dailySpecial != null )
		{
			int itemId = MicroBreweryRequest.dailySpecial.getItemId();
			String name = MicroBreweryRequest.dailySpecial.getName();
			int price = Math.max( 1, Math.abs( ItemDatabase.getPriceById( itemId ) ) ) * 3;
			CafeRequest.addMenuItem( KoLConstants.microbreweryItems, name, price );
		}

		ConcoctionDatabase.getUsables().sort();
		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	public static final void reset()
	{
		CafeRequest.reset( KoLConstants.microbreweryItems );
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher matcher = CafeRequest.CAFE_PATTERN.matcher( urlString );
		if ( !matcher.find() || !matcher.group( 1 ).equals( "2" ) )
		{
			return false;
		}

		matcher = CafeRequest.ITEM_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( matcher.group( 1 ) );
		String itemName;
		int price;

		switch ( itemId )
		{
		case -1:
			itemName = "Petite Porter";
			price = 50;
			break;
		case -2:
			itemName = "Scrawny Stout";
			price = 75;
			break;
		case -3:
			itemName = "Infinitesimal IPA";
			price = 100;
			break;
		default:
			itemName = ItemDatabase.getItemName( itemId );
			price = Math.max( 1, Math.abs( ItemDatabase.getPriceById( itemId ) ) ) * 3;
			break;
		}

		CafeRequest.registerItemUsage( itemName, price );
		return true;
	}
}
