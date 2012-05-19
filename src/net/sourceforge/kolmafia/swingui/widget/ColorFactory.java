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

package net.sourceforge.kolmafia.swingui.widget;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

public final class ColorFactory
{
	public static String getItemColor( AdventureResult ar )
	{
		String color = null;
		if ( Preferences.getBoolean( "mementoListActive" ) && KoLConstants.mementoList.contains( ar ) )
		{
			color = "olive";
		}
		else if ( KoLConstants.junkList.contains( ar ) )
		{
			color = "gray";
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
		if ( KoLConstants.junkList.contains( icr.createdItem ) )
		{
			color = "gray";
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
		boolean meetsRequirement = ItemDatabase.meetsLevelRequirement( name );
		String color = null;

		if ( !meetsRequirement )
		{
			color = "#c0c0c0";
		}
		else
		{
			color = ColorFactory.getQualityColor( name );
		}
		return color;
	}

	public static String getStorageColor( AdventureResult ar )
	{
		String color = null;
		String name = ar.getName();
		if ( !ItemDatabase.meetsLevelRequirement( name ) || !EquipmentManager.canEquip( name ) )
		{
			color = "gray";
		}
		else
		{
			color = ColorFactory.getQualityColor( name );
		}
		return color;
	}

	public static final String getQualityColor( final String name )
	{
		String quality = ItemDatabase.getQuality( name );
		if ( quality == ItemDatabase.CRAPPY )
		{
			return "#999999";
		}
		else if ( quality == ItemDatabase.GOOD )
		{
			return "green";
		}
		else if ( quality == ItemDatabase.AWESOME )
		{
			return "blue";
		}
		else if ( quality == ItemDatabase.EPIC )
		{
			return "#8a2be2";
		}
		return null;
	}
}
