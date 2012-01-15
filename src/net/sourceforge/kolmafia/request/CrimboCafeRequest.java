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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CrimboCafeRequest
	extends CafeRequest
{
	public static final boolean AVAILABLE = false;
	public static final String CAFEID = "8";
	public static final Object[][] MENU_DATA =
	{
		// Item, itemID, price
		{
			"Brussels Sprout Stir-Fry",
			new Integer( -79 ),
			new Integer( 50 ),
		},
		{
			"Carrot, Cabbage, and Kale Pizza",
			new Integer( -80 ),
			new Integer( 75 ),
		},
		{
			"Turnip and Rutabaga Pie",
			new Integer( -81 ),
			new Integer( 100 ),
		},
		{
			"Desert Island Iced Tea",
			new Integer( -84 ),
			new Integer( 100 ),
		},
		{
			"Jerkitini",
			new Integer( -83 ),
			new Integer( 100 ),
		},
		{
			"Horseradish-infused Vodka",
			new Integer( -82 ),
			new Integer( 100 ),
		},
	};

	private static final Object[] dataByName( final String name )
	{
		for ( int i = 0; i < CrimboCafeRequest.MENU_DATA.length; ++i )
		{
			Object [] data = CrimboCafeRequest.MENU_DATA[ i ];
			if ( name.equalsIgnoreCase( (String)data[ 0 ] ) )
			{
				return data;
			}
		}
		return null;
	}

	private static final Object[] dataByItemID( final int itemId )
	{
		for ( int i = 0; i < CrimboCafeRequest.MENU_DATA.length; ++i )
		{
			Object [] data = CrimboCafeRequest.MENU_DATA[ i ];
			if ( itemId == ((Integer)data[ 1 ]).intValue() )
			{
				return data;
			}
		}
		return null;
	}

	public final static String dataName( Object [] data )
	{
		return (String)data[ 0 ];
	}

	public final static int dataItemID( Object [] data )
	{
		return ((Integer)data[ 1 ]).intValue();
	}

	public final static int dataPrice( Object [] data )
	{
		return ((Integer)data[ 2 ]).intValue();
	}

	public CrimboCafeRequest( final String name )
	{
		super( "Crimbo Cafe", CrimboCafeRequest.CAFEID );

		int itemId = 0;
		int price = 0;

		Object [] data = dataByName( name );
		if ( data != null )
		{
			itemId = dataItemID( data );
			price = dataPrice( data );
		}

		this.setItem( name, itemId, price );
	}

	public static final boolean onMenu( final String name )
	{
		return KoLConstants.cafeItems.contains( name );
	}

	public static final void getMenu()
	{
		if ( !CrimboCafeRequest.AVAILABLE )
		{
			return;
		}
		KoLmafia.updateDisplay( "Visiting Crimbo Cafe..." );
		KoLConstants.cafeItems.clear();
		for ( int i = 0; i < CrimboCafeRequest.MENU_DATA.length; ++i )
		{
			Object [] data = CrimboCafeRequest.MENU_DATA[ i ];
			String name = CrimboCafeRequest.dataName( data );
			int price = CrimboCafeRequest.dataPrice( data );
			CafeRequest.addMenuItem( KoLConstants.cafeItems, name, price );
		}
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
		if ( !matcher.find() || !matcher.group( 1 ).equals( CrimboCafeRequest.CAFEID ) )
		{
			return false;
		}

		matcher = CafeRequest.ITEM_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int itemId = StringUtilities.parseInt( matcher.group( 1 ) );

		Object [] data = CrimboCafeRequest.dataByItemID( itemId );
		if ( data == null )
		{
			return false;
		}

		String itemName = CrimboCafeRequest.dataName( data);
		int price = CrimboCafeRequest.dataPrice( data );

		CafeRequest.registerItemUsage( itemName, price );
		return true;
	}
}
