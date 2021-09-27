
package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * {@link net.java.dev.spellcast.utilities.LockableListModel} is used to hold data, but it depends on Swing.
 * For VMs without access to Swing, a List or {@link net.sourceforge.kolmafia.utilities.SortedList} is used
 * Based on forum post by xKiv: http://tiny.cc/b6kw8x
 * @author ajoshi
 */

public class LockableListFactory
{
	/**
	 * Get an instance of LockableListModel if in a Swing environment, else get a List
	 * @param E Class
	 * @return  LockableListModel<E> or an ArrayList if Swing cannot be loaded
	 */

	public static <E> List<E> getInstance( Class<?> E )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			return new LockableListModel<E>();
		}
		return new ArrayList<E>();
	}

	/**
	 * Get an instance of SortedListModel if in a Swingless environment, else get a SortedList
	 * @param E Class
	 * @return  SortedListModel<E> or an SortedList if Swing cannot be loaded
	 */

	public static <E> List<E> getSortedInstance( Class<?> E )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			return new SortedListModel<E>();
		}
		List i = new SortedList<E>();
		return i;
	}

	/**
	 * Calls {@link net.java.dev.spellcast.utilities.LockableListModel#getSize()} if possible,
	 * else returns list size
	 */

	public static int getsize( List<?> l )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			return ( (LockableListModel<?>) l ).getSize();
		}
		return l.size();
	}

	/**
	 * Calls {@link net.java.dev.spellcast.utilities.LockableListModel#getSelectedIndex()} if possible,
	 * else returns 0
	 */
	public static int getSelectedIndex( List<?> l )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			return ( (LockableListModel<?>) l ).getSelectedIndex();
		}
		return 0;
	}

	/**
	 * Calls {@link net.java.dev.spellcast.utilities.LockableListModel#setSelectedIndex()} if possible,
	 * else no p
	 */
	public static void setSelectedIndex( List<?> l, int index )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			( (LockableListModel<?>) l ).setSelectedIndex( index );
		}
	}

	/**
	 * Calls {@link net.java.dev.spellcast.utilities.LockableListModel#setSelectedItem()} if possible,
	 *  else no op
	 */
	public static void setSelectedItem( List l, Object selection )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			( (LockableListModel) l ).setSelectedItem( selection );
		}
		else
		{
			// noop
		}
	}

	/**
	 * Calls {@link net.java.dev.spellcast.utilities.LockableListModel#getMirror()} if possible,
	 * else returns the original list
	 */
	public static <T> List<T> getMirror( List<T> l )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			return ( (LockableListModel<T>) l ).getMirrorImage();
		}
		else
		{
			return l;
		}
	}

	/**
	 * Calls {@link net.java.dev.spellcast.utilities.LockableListModel#fireContentsChanged()} if possible,
	 * else no op
	 */
	public static void fireContentsChanged( List l, int index0, int index1 )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			( (LockableListModel) l ).fireContentsChanged( l, index0, index1 );
		}
		else
		{
			// noop
		}
	}

	/**
	 * Returns the last element in a list
	 */
	public static <E> E lastElement( List<E> l )
	{
		return l.isEmpty() ? null : l.get( l.size() - 1 );
	}

	/**
	 * Sorts the given list
	 */
	public static void sort( List l )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			( (LockableListModel) l ).sort();
		}
		else
		{
			if ( l instanceof SortedList )
			{
				( (SortedList) l ).sort();
			}
			else
			{
				synchronized ( l )
				{
					Collections.sort( l, null );
				}
			}
		}
	}

	public static <E> E getElementAt( List<E> l, int index )
	{
		if ( SwinglessUIUtils.isSwingAvailable )
		{
			return ( (LockableListModel<E>) l ).getElementAt( index );
		}
		return l.get( index );
	}
}
