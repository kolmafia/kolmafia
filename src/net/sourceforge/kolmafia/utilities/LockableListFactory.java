/**
 * Copyright (c) 2005-2016, KoLmafia development team
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
	 * Get an instance of LockableListModel if in a Swingless environment, else get a List
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
};