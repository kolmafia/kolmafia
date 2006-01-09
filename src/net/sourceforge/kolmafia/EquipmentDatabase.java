/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A static class which retrieves all the adventures currently
 * available to <code>KoLmafia</code>.
 */

public class EquipmentDatabase extends KoLDatabase
{
	private static int [] power = new int[ TradeableItemDatabase.ITEM_COUNT ];
	private static int [] hands = new int[ TradeableItemDatabase.ITEM_COUNT ];
	private static String [] requirement =  new String[ TradeableItemDatabase.ITEM_COUNT ];

	static
	{
		BufferedReader reader = getReader( "equipment.dat" );

		String [] data;
		int itemID;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 3 )
			{
				itemID = TradeableItemDatabase.getItemID( data[0] );

				if ( itemID != -1 )
				{
					power[ itemID ] = Integer.parseInt( data[1] );
					requirement[ itemID ] = data[2];
					hands[ itemID ] = ( data.length >= 4 ) ? Integer.parseInt( data[3] ) :
						TradeableItemDatabase.getConsumptionType( itemID ) == ConsumeItemRequest.EQUIP_WEAPON ? 1 : 0;
				}
			}
		}
	}

	public static boolean contains( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );
		return itemID != -1 && requirement[ itemID ] != null;
	}

	public static boolean canEquip( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 || requirement[ itemID ] == null )
			return false;

		if ( requirement[ itemID ].startsWith( "Mus:" ) )
			return KoLCharacter.getBaseMuscle() >= Integer.parseInt( requirement[ itemID ].substring(5) );

		if ( requirement[ itemID ].startsWith( "Mys:" ) )
			return KoLCharacter.getBaseMysticality() >= Integer.parseInt( requirement[ itemID ].substring(5) );

		if ( requirement[ itemID ].startsWith( "Mox:" ) )
			return KoLCharacter.getBaseMoxie() >= Integer.parseInt( requirement[ itemID ].substring(5) );

		return true;
	}

	public static int getPower( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return 0;

		return power[ itemID ];
	}

	public static int getHands( String itemName )
	{
		int itemID = TradeableItemDatabase.getItemID( itemName );

		if ( itemID == -1 )
			return 0;

		return hands[ itemID ];
	}
}
