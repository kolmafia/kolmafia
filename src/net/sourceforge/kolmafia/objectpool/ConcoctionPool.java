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
