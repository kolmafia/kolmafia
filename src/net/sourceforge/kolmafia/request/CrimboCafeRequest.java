/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CrimboCafeRequest
	extends CafeRequest
{
	public CrimboCafeRequest( final String name )
	{
		super( "Crimbo Cafe", "7" );

		int itemId = 0;
		int price = 0;

		switch ( KoLConstants.cafeItems.indexOf( name ) )
		{
		case 0:
			itemId = -73;
			price = 50;
			break;

		case 1:
			itemId = -74;
			price = 75;
			break;

		case 2:
			itemId = -75;
			price = 100;
			break;

		case 3:
			itemId = -76;
			price = 50;
			break;

		case 4:
			itemId = -77;
			price = 75;
			break;

		case 5:
			itemId = -78;
			price = 100;
			break;
		}

		this.setItem( name, itemId, price );
	}

	public static final boolean onMenu( final String name )
	{
		return KoLConstants.cafeItems.contains( name );
	}

	public static final void getMenu()
	{
		KoLmafia.updateDisplay( "Visiting Crimbo Cafe..." );
		KoLConstants.cafeItems.clear();
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "CRIMBCO Reconstituted Gruel", 50 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "New and Improved CRIMBCO Reconstituted Gruel", 75 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "CRIMBCO Deluxe Reconstituted Gruel with Simulated Raisins", 100 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "CRIMBCO Ribbon Candy Schnapps", 50 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "CRIMBCO Egg Substitute Nog Substitute", 75 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "CRIMBCO Extreme Braincracker Sour", 100 );
		ConcoctionDatabase.getUsables().sort();
		KoLmafia.updateDisplay( "Menu retrieved." );
	}

	public static final void reset()
	{
		CafeRequest.reset( KoLConstants.cafeItems );
	}

	public static final boolean registerRequest( final String urlString )
	{
		Matcher matcher = CafeRequest.CAFE_PATTERN.matcher( urlString );
		if ( !matcher.find() || !matcher.group( 1 ).equals( "7" ) )
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
		case -73:
			itemName = "CRIMBCO Reconstituted Gruel";
			price = 50;
			break;
		case -74:
			itemName = "New and Improved CRIMBCO Reconstituted Gruel";
			price = 75;
			break;
		case -75:
			itemName = "CRIMBCO Deluxe Reconstituted Gruel with Simulated Raisins";
			price = 100;
			break;
		case -76:
			itemName = "CRIMBCO Ribbon Candy Schnapps";
			price = 50;
			break;
		case -77:
			itemName = "CRIMBCO Egg Substitute Nog Substitute";
			price = 75;
			break;
		case -78:
			itemName = "CRIMBCO Extreme Braincracker Sour";
			price = 100;
			break;
		default:
			return false;
		}

		CafeRequest.registerItemUsage( itemName, price );
		return true;
	}
}
