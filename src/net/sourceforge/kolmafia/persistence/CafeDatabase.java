/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CafeDatabase
{
	static class InverseIntegerOrder implements Comparator<Integer>
	{
		@Override
		public int compare(Integer o1, Integer o2)
		{
			return o2.compareTo(o1);
		}
	}

	private static final Map<Integer, String> cafeFood = new TreeMap<Integer, String>( new InverseIntegerOrder() );
	private static final Map<Integer, String> cafeBooze = new TreeMap<Integer, String>( new InverseIntegerOrder() );

	// Map from item name to descid, since ItemDatabase can't help with these
	private static final Map<String, String> nameToDescId = new TreeMap<String, String>();;

	static
	{
		CafeDatabase.readCafeData( "cafe_booze.txt",  KoLConstants.CAFE_BOOZE_VERSION, CafeDatabase.cafeBooze );
		CafeDatabase.readCafeData( "cafe_food.txt",  KoLConstants.CAFE_FOOD_VERSION, CafeDatabase.cafeFood );
	}

	private static void readCafeData( String filename, int version, Map<Integer, String> map )
	{
		BufferedReader reader = FileUtilities.getVersionedReader( filename, version );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			CafeDatabase.saveCafeItem( data, map );
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
		}
	}

	public static final String boozeDescId( int id )
	{
		return String.valueOf( id ) + "_booze";
	}

	public static final String foodDescId( int id )
	{
		return String.valueOf( id ) + "_food";
	}

	public static String getCafeBoozeName( int id )
	{
		return cafeBooze.get( id );
	}

	public static String getCafeFoodName( int id )
	{
		return cafeFood.get( id );
	}

	public static final Set<Integer> cafeBoozeKeySet()
	{
		return CafeDatabase.cafeBooze.keySet();
	}

	public static final Set<Entry<Integer, String>> cafeBoozeEntrySet()
	{
		return CafeDatabase.cafeBooze.entrySet();
	}

	public static final Set<Integer> cafeFoodKeySet()
	{
		return CafeDatabase.cafeFood.keySet();
	}

	public static final Set<Entry<Integer, String>> cafeFoodEntrySet()
	{
		return CafeDatabase.cafeFood.entrySet();
	}

	public static final String nameToDescId( final String name )
	{
		String descId = nameToDescId.get( name );
		if ( descId != null )
		{
			return descId;
		}
		// Some cafes offer real items. Look them up in ItemDatabase
		int id = ItemDatabase.getItemId( name, 1, false );
		return (id == -1 ) ? null : ItemDatabase.getDescriptionId( id );
	}

	private static final void saveCafeItem( String[] data, Map<Integer, String> map )
	{
		if ( data.length < 2 )
			return;

		String id = data[ 0 ];
		int itemId = StringUtilities.parseInt( id );
		String name = data[ 1 ];
		map.put( itemId, name  );
		String descId = ( map == cafeBooze ) ? boozeDescId( itemId ) : foodDescId( itemId );
		nameToDescId.put( name, descId );
	}
}
