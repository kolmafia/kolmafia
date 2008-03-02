/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class ConcoctionPool
{
	private static final ConcoctionArray concoctionCache = new ConcoctionArray();

	public static int count()
	{
		return concoctionCache.size();
	}

	public static Concoction get( int itemId )
	{
		return concoctionCache.get( itemId );
	}

	public static void set( int itemId, Concoction c )
	{
		concoctionCache.set( itemId, c );
	}

	/**
	 * Find a concoction made in a particular way that includes the specified ingredient
	 */

	public static final int findConcoction( final int mixingMethod, final int itemId )
	{
		int count = concoctionCache.size();
		AdventureResult ingredient = ItemPool.get( itemId, 1 );

		for ( int i = 0; i < count; ++i )
		{
			Concoction concoction = concoctionCache.get( i );
			if ( concoction == null || concoction.getMixingMethod() != mixingMethod )
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
				if ( ingredients[ j ].equals( ingredient ) )
				{
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Internal class which functions exactly an array of concoctions, except it uses "sets" and "gets" like a list.
	 * This could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
	 */

	private static class ConcoctionArray
	{
		private final ArrayList internalList = new ArrayList();

		public ConcoctionArray()
		{
			int maxItemId = ItemDatabase.maxItemId();
			for ( int i = 0; i <= maxItemId; ++i )
			{
				this.internalList.add( new Concoction(
					ItemDatabase.getItemName( i ) == null ? null : ItemPool.get( i, 1 ),
					KoLConstants.NOCREATE ) );
			}
		}

		public Concoction get( final int index )
		{
			if ( index < 0 )
			{
				return null;
			}

			return (Concoction) this.internalList.get( index );
		}

		public void set( final int index, final Concoction value )
		{
			this.internalList.set( index, value );
		}

		public int size()
		{
			return this.internalList.size();
		}
	}
}
