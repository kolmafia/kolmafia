/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.widget;

import java.util.HashMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.objectpool.Concoction;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

public final class ColorFactory
{
	private final static HashMap<String, String> colorPrefMap = new HashMap<String, String>();

	static
	{
		String rawPref = Preferences.getString( "textColors" );
		String[] splitPref = rawPref.split( "\\|" );

		for ( int i = 0; i < splitPref.length; ++i )
		{
			String[] it = splitPref[ i ].split( ":" );
			if ( it.length == 2 )
			{
				colorPrefMap.put( it[ 0 ], it[ 1 ] );
			}
		}
	}

	public static String getItemColor( AdventureResult ar )
	{
		String color = null;

		color = checkOptionalColors( ar.getItemId() );

		if ( color != null )
		{
			return color;
		}

		if ( Preferences.getBoolean( "mementoListActive" ) && KoLConstants.mementoList.contains( ar ) )
		{
			color = getMementoColor();
		}
		else if ( KoLConstants.junkList.contains( ar ) )
		{
			color = getJunkColor();
		}
		else
		{
			color = ColorFactory.getQualityColor( ar.getName() );
		}
		return color;
	}

	public static String getCreationColor( CreateItemRequest icr )
	{
		return ColorFactory.getCreationColor( icr, false );
	}

	public static String getCreationColor( CreateItemRequest icr, boolean isEquipment )
	{
		String color = null;

		color = checkOptionalColors( icr.getItemId() );

		if ( color != null )
		{
			return color;
		}

		if ( KoLConstants.junkList.contains( icr.createdItem ) )
		{
			color = getJunkColor();
		}
		else if ( !isEquipment )
		{
			color = ColorFactory.getQualityColor( icr.getName() );
		}
		return color;
	}

	public static String getConcoctionColor( Concoction item )
	{
		String name = item.getName();

		String color = checkOptionalColors( item.getItemId() );

		if ( color != null )
		{
			return color;
		}

		return	ItemDatabase.meetsLevelRequirement( name ) ?
			ColorFactory.getQualityColor( name ) :
			getNotAvailableColor();
	}

	public static String getStorageColor( AdventureResult ar )
	{
		String color = null;
		String name = ar.getName();

		color = checkOptionalColors( ar.getItemId() );

		if ( color != null )
		{
			return color;
		}

		if ( !ItemDatabase.meetsLevelRequirement( name ) || !EquipmentManager.canEquip( name ) )
		{
			color = getNotAvailableColor();
		}
		else
		{
			color = ColorFactory.getQualityColor( name );
		}
		return color;
	}

	public static final String getQualityColor( final String name )
	{
		String pref;
		String color = null;
		String quality = ItemDatabase.getQuality( name );

		if ( quality == null )
		{
			return null;
		}
		if ( quality.equals( ItemDatabase.CRAPPY ) )
		{
			pref = checkPref( "crappy" );
			color = pref != null ? pref : "#999999";
		}
		else if ( quality.equals( ItemDatabase.DECENT ) )
		{
			pref = checkPref( "decent" );
			color = pref;
		}
		else if ( quality.equals( ItemDatabase.GOOD ) )
		{
			pref = checkPref( "good" );
			color = pref != null ? pref : "green";
		}
		else if ( quality.equals( ItemDatabase.AWESOME ) )
		{
			pref = checkPref( "awesome" );
			color = pref != null ? pref : "blue";
		}
		else if ( quality.equals( ItemDatabase.EPIC ) )
		{
			pref = checkPref( "epic" );
			color = pref != null ? pref : "#8a2be2";
		}
		return color;
	}

	private static String getJunkColor()
	{
		String pref = checkPref( "junk" );
		if ( pref == null )
		{
			return "gray";
		}
		return pref;
	}

	private static String getMementoColor()
	{
		String pref = checkPref( "memento" );
		if ( pref == null )
		{
			return "olive";
		}
		return pref;
	}

	private static String getNotAvailableColor()
	{
		String pref = checkPref( "notavailable" );
		if ( pref == null )
		{
			return "gray";
		}
		return pref;
	}

	private static String getQuestColor()
	{
		return checkPref( "quest" );
	}

	private static String getNotTradeableColor()
	{
		return checkPref( "nontradeable" );
	}

	private static String getGiftColor()
	{
		return checkPref( "gift" );
	}

	private static String checkOptionalColors( int itemId )
	{
		if ( ItemDatabase.isGiftable( itemId ) && !ItemDatabase.isTradeable( itemId ) )
		{
			// gift items
			String it = getGiftColor();
			if ( it != null )
				return it;
		}
		if ( ItemDatabase.isQuestItem( itemId ) )
		{
			// quest items
			String it = getQuestColor();
			if ( it != null )
				return it;
		}
		if ( !ItemDatabase.isTradeable( itemId ) )
		{
			String it = getNotTradeableColor();
			if ( it != null )
				return it;
		}

		return null;
	}

	private static String checkPref( String pref )
	{
		Object it = colorPrefMap.get( pref );
		if ( it != null )
		{
			return it.toString();
		}
		return null;
	}
}
