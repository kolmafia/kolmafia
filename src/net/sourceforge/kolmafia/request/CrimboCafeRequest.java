/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
		super( "Crimbo Cafe", "5" );

		int itemId = 0;
		int price = 0;

		switch ( KoLConstants.cafeItems.indexOf( name ) )
		{
		case 0:
			itemId = -4;
			price = 50;
			break;

		case 1:
			itemId = -5;
			price = 75;
			break;

		case 2:
			itemId = -6;
			price = 100;
			break;

		case 3:
			itemId = -9;
			price = 50;
			break;

		case 4:
			itemId = -8;
			price = 75;
			break;

		case 5:
			itemId = -7;
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
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "eggnogtini", 50 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "candycaine", 75 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "braincracker sweet", 100 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "gingerbread stir-fry", 50 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "candy cane pizza", 75 );
		CafeRequest.addMenuItem( KoLConstants.cafeItems, "gumdrop chow mein", 100 );
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
		if ( !matcher.find() || !matcher.group( 1 ).equals( "5" ) )
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
		case -4:
			itemName = "eggnogtini";
			price = 50;
			break;
		case -5:
			itemName = "candycaine";
			price = 75;
			break;
		case -6:
			itemName = "braincracker sweet";
			price = 100;
			break;
		case -9:
			itemName = "gingerbread stir-fry";
			price = 50;
			break;
		case -8:
			itemName = "candy cane pizza";
			price = 75;
			break;
		case -7:
			itemName = "gumdrop chow mein";
			price = 100;
			break;
		default:
			return false;
		}

		CafeRequest.registerItemUsage( itemName, price );
		return true;
	}
}
