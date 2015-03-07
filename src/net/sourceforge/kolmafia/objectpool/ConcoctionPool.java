/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

package net.sourceforge.kolmafia.objectpool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConcoctionPool
{
	// Canonical Name -> Concoction
	// *** since item names can be duplicated, this only has last entered name
	private static final Map<String, Concoction> names = new TreeMap<String, Concoction>();

	// ItemID -> Concoction
	private static final Map<Integer,Concoction> items = new TreeMap<Integer,Concoction>();

	// Name -> Concoction
	private static final Map<String, Concoction> nonitems = new TreeMap<String, Concoction>();

	// All concoctions
	private static Collection<Concoction> values = null;
	private static final Map<Integer, Integer> rowCache = new TreeMap<Integer, Integer>();

	static
	{
		// Pre-set concoctions for all items.
		int maxItemId = ItemDatabase.maxItemId();
		for ( int i = 1; i <= maxItemId; ++i )
		{
			// Skip non-existent items
			if ( ItemDatabase.getItemName( i ) != null )
			{
				AdventureResult ar = ItemPool.get( i, 1 );
				Concoction c = new Concoction( ar, CraftingType.NOCREATE );
				ConcoctionPool.set( c );
			}
		}
	}

	public static Concoction get( int itemId )
	{
		return ConcoctionPool.items.get( itemId );
	}

	public static Concoction get( final String name )
	{
		// *** item names can be duplicated.
		// *** why is this canonical?
		String cname = StringUtilities.getCanonicalName( name );
		return ConcoctionPool.names.get( cname );
	}

	public static Concoction get( final AdventureResult ar )
	{
		int itemId = ar.getItemId();
		return itemId > 0 ? ConcoctionPool.items.get( itemId ) : ConcoctionPool.nonitems.get( ar.getName() );
	}

	public static void set( final Concoction c )
	{
		String name = c.getName();

		// *** item names can be duplicated.
		// *** why is this canonical?
		String cname = StringUtilities.getCanonicalName( name );
		ConcoctionPool.names.put( cname, c );

		int itemId = c.getItemId();

		if ( itemId > 0 )
		{
			ConcoctionPool.items.put( itemId, c );
		}
		else
		{
			ConcoctionPool.nonitems.put( name, c );
		}

		int row = c.getRow();

		if ( row > 0 )
		{
			if ( ConcoctionPool.rowCache.containsKey( row ) )
			{
				RequestLogger.printLine( "Duplicate row for item " + itemId );
			}
			ConcoctionPool.rowCache.put( row, itemId );
		}

		// Rebuild values next time it is needed
		ConcoctionPool.values = null;
	}

	public static int idToRow( int itemId )
	{
		for ( Entry<Integer, Integer> entry : ConcoctionPool.rowCache.entrySet() )
		{
			if ( itemId == entry.getValue() )
			{
				return entry.getKey();
			}
		}
		return -1;
	}

	public static int rowToId( int row )
	{
		if ( ConcoctionPool.rowCache.containsKey( row ) )
		{
			return ConcoctionPool.rowCache.get( row );
		}
		return -1;
	}

	public static Collection<Concoction> concoctions()
	{
		if ( ConcoctionPool.values == null )
		{
			ConcoctionPool.values = new ArrayList<Concoction>();
			ConcoctionPool.values.addAll( ConcoctionPool.items.values() );
			ConcoctionPool.values.addAll( ConcoctionPool.nonitems.values() );
		}
		return ConcoctionPool.values;
	}

	/**
	 * Find a concoction made in a particular way that includes the
	 * specified ingredient
	 */

	public static final Concoction findConcoction( final CraftingType mixingMethod, final int itemId, final int used )
	{
		for ( Concoction item : ConcoctionPool.concoctions() )
		{
			if ( item.getMixingMethod() != mixingMethod )
			{
				continue;
			}

			AdventureResult[] ingredients = ConcoctionDatabase.getStandardIngredients( item.concoction.getItemId() );
			if ( ingredients == null )
			{
				continue;
			}

			for ( AdventureResult ingredient : ingredients )
			{
				if ( ingredient.getItemId() == itemId && ingredient.getCount() == used )
				{
					return item;
				}
			}
		}

		return null;
	}

	public static final Concoction findConcoction( final AdventureResult[] ingredients )
	{
		for ( Concoction item : ConcoctionPool.concoctions() )
		{
			if ( item.hasIngredients( ingredients ) )
			{
				return item;
			}
		}

		return null;
	}

}
