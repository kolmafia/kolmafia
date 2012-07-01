/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

package net.java.dev.spellcast.utilities;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;
import javax.swing.MutableComboBoxModel;

/**
 * Lockable aspects of this class have been removed due to incompatibilities with Swing; synchronization between two
 * threads when one is the Swing thread turns out to have a lot of problems. It retains its original name for
 * convenience purposes only.
 */

public class LockableListModel
	extends AbstractListModel
	implements Cloneable, List, ListModel, ComboBoxModel, MutableComboBoxModel
{
	private static final ListElementFilter NO_FILTER = new ShowEverythingFilter();

	private boolean actionListenerFired = false;
	
	private ArrayList mirrorList;
	private ArrayList actualElements;
	private ArrayList visibleElements;

	protected Object selectedValue;
	protected ListElementFilter currentFilter;

	private boolean filtering = false;

	/**
	 * Constructs a new <code>LockableListModel</code>.
	 */

	public LockableListModel()
	{
		this.actualElements = new ArrayList();
		this.visibleElements = new ArrayList();

		this.selectedValue = null;
		this.currentFilter = LockableListModel.NO_FILTER;
		this.mirrorList = new ArrayList();
	}

	public LockableListModel( final Collection c )
	{
		this();
		this.addAll( c );
	}

	private LockableListModel( final LockableListModel l )
	{
		this( l, LockableListModel.NO_FILTER );
	}

	private LockableListModel( final LockableListModel l, final ListElementFilter f )
	{
		this.actualElements = l.actualElements;
		this.visibleElements = new ArrayList();

		this.selectedValue = null;
		this.currentFilter = f == null ? LockableListModel.NO_FILTER : f;

		synchronized ( l.mirrorList )
		{
			this.mirrorList = new ArrayList();
			l.mirrorList.add( new WeakReference( this ) );
		}

		if ( f == LockableListModel.NO_FILTER )
		{
			this.visibleElements.addAll( this.actualElements );
		}
		else if ( f == l.currentFilter )
		{
			this.visibleElements.addAll( l.visibleElements );
		}
		else
		{
			this.updateFilter( false );
		}
	}

	private LockableListModel getNextMirror( final Iterator it )
	{
		WeakReference ref;

		while ( it.hasNext() )
		{
			ref = (WeakReference) it.next();
			if ( ref.get() != null )
			{
				return (LockableListModel) ref.get();
			}

			it.remove();
		}

		return null;
	}

	public void sort()
	{
		synchronized ( this.actualElements )
		{
			Collections.sort( this.actualElements );
		}
		synchronized ( this.visibleElements )
		{
			Collections.sort( this.visibleElements );
		}
		this.fireContentsChanged( this, 0, this.visibleElements.size() - 1 );

		synchronized ( this.mirrorList )
		{
			LockableListModel mirror;
			Iterator it = this.mirrorList.iterator();

			while ( it.hasNext() )
			{
				mirror = this.getNextMirror( it );
				if ( mirror == null )
				{
					return;
				}

				Collections.sort( mirror.visibleElements );
				mirror.fireContentsChanged( this, 0, mirror.visibleElements.size() - 1 );
			}
		}
	}

	public void sort( final Comparator c )
	{
		synchronized ( this.actualElements )
		{
			Collections.sort( this.actualElements, c );
		}
		synchronized ( this.visibleElements )
		{
			Collections.sort( this.visibleElements, c );
		}
		this.fireContentsChanged( this, 0, this.visibleElements.size() - 1 );

		synchronized ( this.mirrorList )
		{
			LockableListModel mirror;
			Iterator it = this.mirrorList.iterator();

			while ( it.hasNext() )
			{
				mirror = this.getNextMirror( it );
				if ( mirror == null )
				{
					return;
				}

				Collections.sort( mirror.visibleElements, c );
				mirror.fireContentsChanged( this, 0, mirror.visibleElements.size() - 1 );
			}
		}
	}

	@Override
	public void fireContentsChanged( final Object source, final int index0, final int index1 )
	{
		if ( index0 >= 0 && index1 < 0 )
		{
			return;
		}
		if ( this.actionListenerFired || this.listenerList.getListenerCount() == 0 )
		{
			return;
		}

		this.actionListenerFired = true;
		super.fireContentsChanged( source, index0, index1 );
		this.actionListenerFired = false;
	}

	@Override
	public void fireIntervalAdded( final Object source, final int index0, final int index1 )
	{
		if ( this.actionListenerFired || this.listenerList.getListenerCount() == 0 )
		{
			return;
		}

		this.actionListenerFired = true;
		super.fireIntervalAdded( source, index0, index1 );
		this.actionListenerFired = false;
	}

	@Override
	public void fireIntervalRemoved( final Object source, final int index0, final int index1 )
	{
		if ( this.actionListenerFired || this.listenerList.getListenerCount() == 0 )
		{
			return;
		}

		this.actionListenerFired = true;
		super.fireIntervalRemoved( source, index0, index1 );
		this.actionListenerFired = false;
	}

	/**
	 * Please refer to {@link java.util.List#add(int,Object)} for more information regarding this function.
	 */

	public void add( final int index, final Object element )
	{
		if ( element == null )
		{
			return;
		}

		if ( this.currentFilter != LockableListModel.NO_FILTER || !this.mirrorList.isEmpty() )
		{
			this.updateFilter( false );
		}

		synchronized ( this.actualElements )
		{
			this.actualElements.add( index, element );
		}
		this.addVisibleElement( index, element );
	}

	private void addVisibleElement( final int index, final Object element )
	{
		this.addVisibleElement( this, index, element );

		synchronized ( this.mirrorList )
		{
			LockableListModel mirror;
			Iterator it = this.mirrorList.iterator();

			while ( it.hasNext() )
			{
				mirror = this.getNextMirror( it );
				if ( mirror == null )
				{
					return;
				}

				this.addVisibleElement( mirror, index, element );
			}
		}
	}

	private void addVisibleElement( final LockableListModel model, final int index, final Object element )
	{
		if ( !model.currentFilter.isVisible( element ) )
		{
			return;
		}

		int visibleIndex = model.computeVisibleIndex( index );
		synchronized ( model.visibleElements )
		{
			model.visibleElements.add( visibleIndex, element );
		}
		model.fireIntervalAdded( model, visibleIndex, visibleIndex );
	}

	/**
	 * Please refer to {@link java.util.List#add(Object)} for more information regarding this function.
	 */

	public boolean add( final Object o )
	{
		if ( o == null )
		{
			return false;
		}

		int originalSize = this.actualElements.size();
		this.add( originalSize, o );
		return originalSize != this.actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#addAll(Collection)} for more information regarding this function.
	 */

	public boolean addAll( final Collection c )
	{
		return this.addAll( this.actualElements.size(), c );
	}

	/**
	 * Please refer to {@link java.util.List#addAll(int,Collection)} for more information regarding this function.
	 */

	public boolean addAll( final int index, final Collection c )
	{
		boolean result;
		synchronized ( this.actualElements )
		{
			result = this.actualElements.addAll( index, c );
		}
		this.updateFilter( false );
		return result;
	}

	/**
	 * Please refer to {@link java.util.List#clear()} for more information regarding this function.
	 */

	public void clear()
	{
		synchronized ( this.actualElements )
		{
			this.actualElements.clear();
		}
		this.clearVisibleElements();
	}

	private void clearVisibleElements()
	{
		this.clearVisibleElements( this );

		synchronized ( this.mirrorList )
		{
			LockableListModel mirror;
			Iterator it = this.mirrorList.iterator();

			while ( it.hasNext() )
			{
				mirror = this.getNextMirror( it );
				if ( mirror == null )
				{
					return;
				}

				this.clearVisibleElements( mirror );
			}
		}
	}

	private void clearVisibleElements( final LockableListModel model )
	{
		int originalSize = model.visibleElements.size();

		if ( originalSize == 0 )
		{
			return;
		}

		synchronized ( model.visibleElements )
		{
			model.visibleElements.clear();
		}
		model.fireIntervalRemoved( model, 0, originalSize - 1 );
	}

	/**
	 * Please refer to {@link java.util.List#contains(Object)} for more information regarding this function.
	 */

	public boolean contains( final Object o )
	{
		return o == null ? false : this.actualElements.contains( o );
	}

	/**
	 * Please refer to {@link java.util.List#containsAll(Collection)} for more information regarding this function.
	 */

	public boolean containsAll( final Collection c )
	{
		return this.actualElements.containsAll( c );
	}

	/**
	 * Please refer to {@link java.util.List#equals(Object)} for more information regarding this function.
	 */

	@Override
	public boolean equals( final Object o )
	{
		return o instanceof LockableListModel ? this == o : this.actualElements.equals( o );
	}

	/**
	 * Please refer to {@link java.util.List#get(int)} for more information regarding this function.
	 */

	public Object get( final int index )
	{
		if ( index < 0 || index >= this.actualElements.size() )
		{
			return null;
		}

		return this.actualElements.get( index );
	}

	/**
	 * Please refer to {@link java.util.List#hashCode()} for more information regarding this function.
	 */

	@Override
	public int hashCode()
	{
		return this.actualElements.hashCode();
	}

	/**
	 * Please refer to {@link java.util.List#indexOf(Object)} for more information regarding this function.
	 */

	public int indexOf( final Object o )
	{
		return o == null ? -1 : this.actualElements.indexOf( o );
	}

	/**
	 * Please refer to {@link java.util.List#isEmpty()} for more information regarding this function.
	 */

	public boolean isEmpty()
	{
		return this.actualElements.isEmpty();
	}

	/**
	 * Internal class used to handle iterators. This is done to ensure that all applicable interface structures are
	 * notified whenever changes are made to the list elements.
	 */

	private class ListModelIterator
		implements ListIterator
	{
		private int nextIndex, previousIndex;
		private boolean isIncrementing;

		public ListModelIterator()
		{
			this( 0 );
		}

		public ListModelIterator( final int initialIndex )
		{
			this.nextIndex = 0;
			this.previousIndex = -1;
			this.isIncrementing = true;
		}

		public boolean hasPrevious()
		{
			return this.previousIndex > 0;
		}

		public boolean hasNext()
		{
			return this.nextIndex < LockableListModel.this.actualElements.size();
		}

		public Object next()
		{
			this.isIncrementing = true;
			Object nextObject = LockableListModel.this.get( this.nextIndex );
			++this.nextIndex;
			++this.previousIndex;
			return nextObject;
		}

		public Object previous()
		{
			this.isIncrementing = false;
			Object previousObject = LockableListModel.this.get( this.previousIndex );
			--this.nextIndex;
			--this.previousIndex;
			return previousObject;
		}

		public int nextIndex()
		{
			return this.nextIndex;
		}

		public int previousIndex()
		{
			return this.previousIndex;
		}

		public void add( final Object o )
		{
			LockableListModel.this.add( this.nextIndex, o );
			++this.nextIndex;
			++this.previousIndex;
		}

		public void remove()
		{
			if ( this.isIncrementing )
			{
				--this.nextIndex;
				--this.previousIndex;
				LockableListModel.this.remove( this.nextIndex );
			}
			else
			{
				++this.nextIndex;
				++this.previousIndex;
				LockableListModel.this.remove( this.previousIndex );
			}
		}

		public void set( final Object o )
		{
			LockableListModel.this.set( this.isIncrementing ? this.nextIndex - 1 : this.previousIndex + 1, o );
		}
	}

	/**
	 * Please refer to {@link java.util.List#iterator()} for more information regarding this function.
	 */

	public Iterator iterator()
	{
		return new ListModelIterator();
	}

	/**
	 * Please refer to {@link java.util.Vector#lastElement()} for more information regarding this function.
	 */

	public Object lastElement()
	{
		return this.actualElements.isEmpty() ? null : this.actualElements.get( this.actualElements.size() - 1 );
	}

	/**
	 * Please refer to {@link java.util.List#lastIndexOf(Object)} for more information regarding this function.
	 */

	public int lastIndexOf( final Object o )
	{
		return o == null ? -1 : this.indexOf( o );
	}

	/**
	 * Please refer to {@link java.util.List#listIterator()} for more information regarding this function.
	 */

	public ListIterator listIterator()
	{
		return new ListModelIterator();
	}

	/**
	 * Please refer to {@link java.util.List#listIterator(int)} for more information regarding this function.
	 */

	public ListIterator listIterator( final int index )
	{
		return new ListModelIterator( index );
	}

	/**
	 * Please refer to {@link java.util.List#remove(int)} for more information regarding this function.
	 */

	public Object remove( final int index )
	{
		if ( index < 0 || index >= this.actualElements.size() )
		{
			return null;
		}

		if ( this.currentFilter != LockableListModel.NO_FILTER || !this.mirrorList.isEmpty() )
		{
			this.updateFilter( false );
		}

		Object returnValue = this.actualElements.get( index );
		synchronized ( this.actualElements )
		{
			this.actualElements.remove( index );
		}
		this.removeVisibleElement( index, returnValue );

		return returnValue;
	}

	private void removeVisibleElement( final int index, final Object element )
	{
		this.removeVisibleElement( this, index, element );
		LockableListModel mirror;

		synchronized ( this.mirrorList )
		{
			Iterator it = this.mirrorList.iterator();

			while ( it.hasNext() )
			{
				mirror = this.getNextMirror( it );
				if ( mirror == null )
				{
					return;
				}

				this.removeVisibleElement( mirror, index, element );
			}
		}
	}

	private void removeVisibleElement( final LockableListModel model, final int index, final Object element )
	{
		if ( !model.currentFilter.isVisible( element ) )
		{
			return;
		}

		int visibleIndex = model.computeVisibleIndex( index );
		synchronized ( model.visibleElements )
		{
			model.visibleElements.remove( visibleIndex );
		}
		model.fireIntervalRemoved( model, visibleIndex, visibleIndex );
	}

	/**
	 * Please refer to {@link java.util.List#remove(Object)} for more information regarding this function.
	 */

	public boolean remove( final Object o )
	{
		return o == null ? false : this.remove( this.indexOf( o ) ) != null;
	}

	/**
	 * Please refer to {@link java.util.List#removeAll(Collection)} for more information regarding this function.
	 */

	public boolean removeAll( final Collection c )
	{
		int originalSize = this.actualElements.size();

		Object current;
		Iterator it = c.iterator();

		while ( it.hasNext() )
		{
			current = it.next();
			if ( current != null )
			{
				this.remove( current );
			}
		}

		return originalSize != this.actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#retainAll(Collection)} for more information regarding this function.
	 */

	public boolean retainAll( final Collection c )
	{
		int originalSize = this.actualElements.size();

		Iterator it = this.iterator();
		while ( it.hasNext() )
		{
			if ( !c.contains( it.next() ) )
			{
				it.remove();
			}
		}

		return originalSize != this.actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#set(int,Object)} for more information regarding this function.
	 */

	public Object set( final int index, final Object element )
	{
		if ( element == null )
		{
			return null;
		}

		if ( this.currentFilter != LockableListModel.NO_FILTER || !this.mirrorList.isEmpty() )
		{
			this.updateFilter( false );
		}

		Object returnValue = this.actualElements.set( index, element );
		this.setVisibleElement( index, element, returnValue );
		return returnValue;
	}

	private void setVisibleElement( final int index, final Object element, final Object originalValue )
	{
		this.setVisibleElement( this, index, element, originalValue );

		synchronized ( this.mirrorList )
		{
			LockableListModel mirror;
			Iterator it = this.mirrorList.iterator();

			while ( it.hasNext() )
			{
				mirror = this.getNextMirror( it );
				if ( mirror == null )
				{
					return;
				}

				this.setVisibleElement( mirror, index, element, originalValue );
			}
		}
	}

	private void setVisibleElement( final LockableListModel model, final int index, final Object element,
		final Object originalValue )
	{
		int visibleIndex = model.computeVisibleIndex( index );

		if ( originalValue != null && model.currentFilter.isVisible( originalValue ) )
		{
			if ( !model.currentFilter.isVisible( element ) )
			{
				synchronized ( model.visibleElements )
				{
					model.visibleElements.remove( visibleIndex );
				}
				model.fireIntervalRemoved( model, visibleIndex, visibleIndex );
			}
			else if ( visibleIndex == model.visibleElements.size() )
			{
				synchronized ( model.visibleElements )
				{
					model.visibleElements.add( visibleIndex, element );
				}
				model.fireIntervalAdded( model, visibleIndex, visibleIndex );
			}
			else
			{
				synchronized ( model.visibleElements )
				{
					model.visibleElements.set( visibleIndex, element );
				}
				model.fireContentsChanged( model, visibleIndex, visibleIndex );
			}
		}
		else if ( model.currentFilter.isVisible( element ) )
		{
			synchronized ( model.visibleElements )
			{
				model.visibleElements.add( visibleIndex, element );
			}
			model.fireIntervalAdded( model, visibleIndex, visibleIndex );
		}
	}

	/**
	 * Please refer to {@link java.util.List#size()} for more information regarding this function.
	 */

	public int size()
	{
		return this.actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#subList(int,int)} for more information regarding this function.
	 */

	public List subList( final int fromIndex, final int toIndex )
	{
		return this.actualElements.subList( fromIndex, toIndex );
	}

	/**
	 * Please refer to {@link java.util.List#toArray()} for more information regarding this function.
	 */

	public Object[] toArray()
	{
		return this.actualElements.toArray();
	}

	/**
	 * Please refer to {@link java.util.List#toArray(Object[])} for more information regarding this function.
	 */

	public Object[] toArray( final Object[] a )
	{
		return this.actualElements.toArray( a );
	}

	public void updateFilter( final boolean refresh )
	{
		this.updateSingleFilter( refresh );

		synchronized ( this.mirrorList )
		{
			LockableListModel mirror;
			Iterator it = this.mirrorList.iterator();

			while ( it.hasNext() )
			{
				mirror = this.getNextMirror( it );
				if ( mirror == null )
				{
					return;
				}

				mirror.updateSingleFilter( refresh );
			}
		}
	}

	private void updateSingleFilter( final boolean refresh )
	{
		Object element;
		int visibleIndex = 0;

		for ( int i = 0; i < this.actualElements.size(); ++i )
		{
			element = this.actualElements.get( i );

			if ( this.currentFilter.isVisible( element ) )
			{
				if ( visibleIndex == this.visibleElements.size() || this.visibleElements.get( visibleIndex ) != element )
				{
					synchronized ( this.visibleElements )
					{
						this.visibleElements.add( visibleIndex, element );
					}
					this.fireIntervalAdded( this, visibleIndex, visibleIndex );
				}

				++visibleIndex;
			}
			else if ( visibleIndex < this.visibleElements.size() && this.visibleElements.get( visibleIndex ) == element )
			{
				synchronized ( this.visibleElements )
				{
					this.visibleElements.remove( visibleIndex );
				}
				this.fireIntervalRemoved( this, visibleIndex, visibleIndex );
			}
		}

		if ( refresh )
		{
			this.fireContentsChanged( this, 0, this.visibleElements.size() - 1 );
		}
	}

	private int computeVisibleIndex( final int actualIndex )
	{
		if ( currentFilter == NO_FILTER )
		{
			return actualIndex;
		}

		int visibleIndex = 0;

		for ( int i = 0; i < actualIndex && visibleIndex < this.visibleElements.size(); ++i )
		{
			if ( this.actualElements.get( i ) == this.visibleElements.get( visibleIndex ) )
			{
				++visibleIndex;
			}
		}

		return visibleIndex;
	}

	/**
	 * Filters the current list using the provided filter.
	 */

	public void setFilter( final ListElementFilter newFilter )
	{
		this.currentFilter = newFilter == null ? LockableListModel.NO_FILTER : newFilter;
	}

	public int getIndexOf( final Object o )
	{
		return this.visibleElements.indexOf( o );
	}

	/**
	 * Please refer to {@link javax.swing.ListModel#getElementAt(int)} for more information regarding this function.
	 */

	public Object getElementAt( final int index )
	{
		return index < 0 || index >= this.visibleElements.size() ? null : this.visibleElements.get( index );
	}

	/**
	 * Please refer to {@link javax.swing.ListModel#getSize()} for more information regarding this function.
	 */

	public int getSize()
	{
		return this.visibleElements.size();
	}

	/**
	 * Please refer to {@link javax.swing.ComboBoxModel#getSelectedItem()} for more information regarding this function.
	 */

	public Object getSelectedItem()
	{
		return this.contains( this.selectedValue ) ? this.selectedValue : null;
	}

	/**
	 * Returns the index of the currently selected item in this <code>LockableListModel</code>. This is used
	 * primarily in cloning, to ensure that the same indices are being selected; however it may also be used to report
	 * the index of the currently selected item in testing a new object which uses a list model.
	 *
	 * @return the index of the currently selected item
	 */

	public int getSelectedIndex()
	{
		return this.visibleElements.indexOf( this.selectedValue );
	}

	/**
	 * Please refer to {@link javax.swing.ComboBoxModel#setSelectedItem(Object)} for more information regarding this
	 * function.
	 */

	public void setSelectedItem( final Object o )
	{
		this.selectedValue = o;
		this.fireContentsChanged( this, -1, -1 );
	}

	/**
	 * Sets the given index in this <code>LockableListModel</code> as the currently selected item. This is meant to be
	 * a complement to setSelectedItem(), and also functions to help in the cloning process.
	 */

	public void setSelectedIndex( final int index )
	{
		this.setSelectedItem( this.getElementAt( index ) );
	}

	/**
	 * Please refer to {@link javax.swing.MutableComboBoxModel#addElement(Object)} for more information regarding this
	 * function.
	 */

	public void addElement( final Object element )
	{
		this.add( element );
	}

	/**
	 * Please refer to {@link javax.swing.MutableComboBoxModel#insertElementAt(Object,int)} for more information
	 * regarding this function.
	 */

	public void insertElementAt( final Object element, final int index )
	{
		this.add( element );
	}

	/**
	 * Please refer to {@link javax.swing.MutableComboBoxModel#removeElement(Object)} for more information regarding
	 * this function.
	 */

	public void removeElement( final Object element )
	{
		this.remove( element );
	}

	/**
	 * Please refer to {@link javax.swing.MutableComboBoxModel#removeElementAt(int)} for more information regarding this
	 * function.
	 */

	public void removeElementAt( final int index )
	{
		this.remove( this.visibleElements.get( index ) );
	}

	/**
	 * Returns a deep copy of the data associated with this <code>LockableListModel</code>. Note that any subclasses
	 * must override this method in order to ensure that the object can be cast appropriately; note also that the
	 * listeners are not inherited by the clone copy, since this violates the principle of independence. If they are
	 * required, then the class using this model should add them using the functions provided in the
	 * <code>ListModel</code> interface. Note also that if an element added to the list does not implement the
	 * <code>Cloneable</code> interface, or implements it by causing it to fail by default, this method will not fail;
	 * it will add a reference to the object, in effect creating a shallow copy of it. Thus, retrieving an object using
	 * get() and modifying a field will result in both <code>LockableListModel</code> objects changing, in the same
	 * way retrieving an element from a cloned <code>ArrayList</code> will.
	 *
	 * @return a deep copy (exempting listeners) of this <code>LockableListModel</code>.
	 */

	@Override
	public Object clone()
	{
		LockableListModel cloneCopy;

		try
		{
			cloneCopy = (LockableListModel) super.clone();
		}
		catch ( CloneNotSupportedException e )
		{
			// Because none of the super classes support clone(), this means
			// that this method is overriding the one found in Object.  Thus,
			// this exception should never be thrown, unless one of the super
			// classes is re-written to throw the exception by default.

			throw new RuntimeException(
				"AbstractListModel or one of its superclasses was rewritten to throw CloneNotSupportedException by default, call to clone() was unsuccessful" );
		}

		cloneCopy.listenerList = new javax.swing.event.EventListenerList();

		cloneCopy.actualElements = new ArrayList();
		cloneCopy.actualElements.addAll( this.actualElements );

		cloneCopy.visibleElements = new ArrayList();
		cloneCopy.visibleElements.addAll( this.visibleElements );

		cloneCopy.mirrorList = new ArrayList();

		cloneCopy.currentFilter = this.currentFilter;
		cloneCopy.selectedValue = null;

		return cloneCopy;
	}

	/**
	 * Special class which allows you to filter elements inside of this list model.
	 */

	public static interface ListElementFilter
	{
		public boolean isVisible( Object element );
	}

	private static class ShowEverythingFilter
		implements ListElementFilter
	{
		public boolean isVisible( final Object element )
		{
			return true;
		}
	}

	/**
	 * Returns a mirror image of this <code>LockableListModel</code>. In essence, the object returned will be a clone
	 * of the original object. However, it has the additional feature of listening for changes to the
	 * <em>underlying data</em> of this <code>LockableListModel</code>. Note that this means any changes in
	 * selected indices will not be mirrored in the mirror image. Note that because this function modifies the listeners
	 * for this class, an asynchronous version is not available.
	 *
	 * @return a mirror image of this <code>LockableListModel</code>
	 */

	public LockableListModel getMirrorImage()
	{
		return new LockableListModel( this );
	}

	/**
	 * Returns a mirror image of this <code>LockableListModel</code>. In essence, the object returned will be a clone
	 * of the original object. However, it has the additional feature of listening for changes to the
	 * <em>underlying data</em> of this <code>LockableListModel</code>. Note that this means any changes in
	 * selected indices will not be mirrored in the mirror image. Note that because this function modifies the listeners
	 * for this class, an asynchronous version is not available.
	 *
	 * @return a mirror image of this <code>LockableListModel</code>
	 */

	public LockableListModel getMirrorImage( final ListElementFilter filter )
	{
		return new LockableListModel( this, filter );
	}

	/**
	 * Accessory method used to check the filtering status of the model.
	 *
	 *@return the filtering status of this <code>LockableListModel</code>
	 */
	public boolean isFiltering()
	{
		return this.filtering ;
	}
	
	/**
	 * Accessory method used to set the filtering status of the model.
	 *
	 */
	
	public void setFiltering( boolean isFiltering )
	{
		this.filtering = isFiltering;
	}
}
