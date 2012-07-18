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

package net.sourceforge.kolmafia.objectpool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConcoctionPool
{
	private static final TreeMap<String, Concoction> map = new TreeMap<String, Concoction>();
	private static Collection<Concoction> values = null;
	private static final ConcoctionArray cache = new ConcoctionArray();

	static
	{
		// Pre-set concoctions for all items.

		int maxItemId = ItemDatabase.maxItemId();
		for ( int i = 1; i <= maxItemId; ++i )
		{
			String name = ItemDatabase.getItemName( i );

			// Skip non-existent items
			if ( name != null )
			{
				AdventureResult ar = ItemPool.get( i, 1 );
				Concoction c = new Concoction( ar, KoLConstants.NOCREATE );
				ConcoctionPool.set( c );
			}
		}
	}

	public static Concoction get( int itemId )
	{
		return ConcoctionPool.cache.get( itemId );
	}

	public static Concoction get( final String name )
	{
		String cname = StringUtilities.getCanonicalName( name );
		return ConcoctionPool.map.get( cname );
	}

	public static Concoction get( final AdventureResult ar )
	{
		int itemId = ar.getItemId();
		return itemId > 0 ? ConcoctionPool.get( itemId ) : ConcoctionPool.get( ar.getName() );
	}

	public static void set( final Concoction c )
	{
		String cname = StringUtilities.getCanonicalName( c.getName() );
		ConcoctionPool.map.put( cname, c );
		ConcoctionPool.values = null;

		int itemId = c.getItemId();
		if ( itemId > 0 )
		{
			ConcoctionPool.cache.set( itemId, c );
		}
	}

	public static Iterator<Concoction> iterator()
	{
		if ( ConcoctionPool.values == null )
		{
			ConcoctionPool.values = ConcoctionPool.map.values();
		}
		return ConcoctionPool.values.iterator();
	}

	/**
	 * Find a concoction made in a particular way that includes the
	 * specified ingredient
	 */

	public static final Concoction findConcoction( final int mixingMethod, final int itemId, final int used )
	{
		int count = ConcoctionPool.cache.size();

		for ( int i = 0; i < count; ++i )
		{
			Concoction concoction = ConcoctionPool.cache.get( i );
			if ( concoction == null || (concoction.getMixingMethod() & KoLConstants.CT_MASK) != mixingMethod )
			{
				continue;
			}

			AdventureResult[] ingredients = ConcoctionDatabase.getStandardIngredients( i );
			if ( ingredients == null )
			{
				continue;
			}

			for ( int j = 0; j < ingredients.length; ++j )
			{
                                AdventureResult ingredient = ingredients[ j ];
				if ( ingredient.getItemId() == itemId && ingredient.getCount() == used )
				{
					return concoction;
				}
			}
		}

		return null;
	}

	/**
	 * Internal class which functions exactly an array of concoctions,
	 * except it uses "sets" and "gets" like a list.
	 *
	 * This could be done with generics (Java 1.5) but is done like this so
	 * that we get backwards compatibility.
	 */

	private static class ConcoctionArray
	{
		private final ArrayList<Concoction> internalList = new ArrayList<Concoction>( ItemDatabase.maxItemId() );
		private int max = 0;

		public ConcoctionArray()
		{
			int max = ItemDatabase.maxItemId();
			for ( int i = 0; i <= max; ++i )
			{
				this.internalList.add( null );
			}
			this.max = max;
		}

		public Concoction get( final int index )
		{
			if ( index < 0 || index > this.max )
			{
				return null;
			}

			return this.internalList.get( index );
		}

		public void set( final int index, final Concoction value )
		{
			if ( index > this.max )
			{
				for ( int i = max + 1; i <= index; ++i )
				{
					this.internalList.add( null );
				}
				this.max = index;
			}
			this.internalList.set( index, value );
		}

		public int size()
		{
			return this.internalList.size();
		}
	}
}
