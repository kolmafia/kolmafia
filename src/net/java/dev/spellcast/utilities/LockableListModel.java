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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

import javax.swing.event.EventListenerList;

/**
 * <p>Lockable aspects of this class have been removed due to incompatibilities with Swing;
 * synchronization between two threads when one is the Swing thread turns out to have a lot
 * of problems.  It retains its original name for convenience purposes only.  The original
 * methods only retain their lock properties if you synchronize upon the object lock, which
 * may be more trouble than they're worth.</p>
 *
 * <p>In addition to this assertion, the <code>LockableListModel</code> also provides the
 * ability to create a <i>mirror image</i>: namely, another <code>LockableListModel</code>
 * that changes whenever the data in <tt>this</tt> <code>LockableListModel</code> changes
 * but does not respond to object-selection changes.  This makes it so that multiple copies
 * of data can be maintained - synchronization of data, in a sense.</p>
 */

public class LockableListModel extends javax.swing.AbstractListModel
	implements Cloneable, java.util.List, javax.swing.ListModel, javax.swing.ComboBoxModel, javax.swing.MutableComboBoxModel
{
	private static final ListElementFilter NO_FILTER = new ListElementFilter();

	private ArrayList mirrorList;
	private ArrayList actualElements;
	private ArrayList visibleElements;

	protected Object selectedValue;
	protected ListElementFilter currentFilter;

	/**
	 * Constructs a new <code>LockableListModel</code>.
	 */

	public LockableListModel()
	{
		actualElements = new ArrayList();
		visibleElements = new ArrayList();

		selectedValue = null;
		currentFilter = NO_FILTER;
		mirrorList = new ArrayList();
	}

	public LockableListModel( Collection c )
	{
		this();
		addAll( c );
	}

	public void sort()
	{
		Collections.sort( visibleElements );
		Collections.sort( actualElements );

		fireContentsChanged( this, 0, getSize() - 1 );
	}

	public void sort( Comparator c )
	{
		Collections.sort( visibleElements, c );
		Collections.sort( actualElements, c );

		fireContentsChanged( this, 0, getSize() - 1 );
	}

	public void fireContentsChanged( Object source, int index0, int index1 )
	{
		if ( listenerList.getListenerCount() == 0 )
			return;

		super.fireContentsChanged( source, index0, index1 );
	}

	public void fireIntervalAdded( Object source, int index0, int index1 )
	{
		if ( listenerList.getListenerCount() == 0 )
			return;

		super.fireIntervalAdded( source, index0, index1 );
	}

	public void fireIntervalRemoved( Object source, int index0, int index1 )
	{
		if ( listenerList.getListenerCount() == 0 )
			return;

		super.fireIntervalRemoved( source, index0, index1 );
	}

	/**
	 * Please refer to {@link java.util.List#add(int,Object)} for more
	 * information regarding this function.
	 */

	public void add( int index, Object element )
	{
		actualElements.add( index, element );
		addVisibleElement( index, element );
	}

	private void addVisibleElement( int index, Object element )
	{
		addVisibleElement( this, index, element );
		for ( int i = 0; i < mirrorList.size(); ++i )
			addVisibleElement( (LockableListModel) mirrorList.get(i), index, element );
	}

	private static void addVisibleElement( LockableListModel model, int index, Object element )
	{
		if ( model.currentFilter.isVisible( element ) )
		{
			int visibleIndex = model.computeVisibleIndex( index );
			model.visibleElements.add( visibleIndex, element );
			model.fireIntervalAdded( model, visibleIndex, visibleIndex );
		}
	}

	/**
	 * Please refer to {@link java.util.List#add(Object)} for more
	 * information regarding this function.
	 */

	public boolean add( Object o )
	{
		int originalSize = actualElements.size();
		add( originalSize, o );
		return originalSize != actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#addAll(Collection)} for more
	 * information regarding this function.
	 */

	public boolean addAll( Collection c )
	{	return addAll( actualElements.size(), c );
	}

	/**
	 * Please refer to {@link java.util.List#addAll(int,Collection)} for more
	 * information regarding this function.
	 */

	public boolean addAll( int index, Collection c )
	{
		if ( isEmpty() )
		{
			if ( c.isEmpty() )
				return false;

			actualElements.addAll( 0, c );
			for ( int i = 0; i < actualElements.size(); ++i )
				addVisibleElement( i, actualElements.get(i) );

			return true;
		}

		Object currentItem;
		int currentIndex = index;
		Iterator myIterator = c.iterator();
		int originalSize = actualElements.size();

		while ( myIterator.hasNext() )
		{
			currentItem = myIterator.next();
			if ( currentItem != null )
				add( currentIndex++, currentItem );
		}

		return originalSize != actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#clear()} for more
	 * information regarding this function.
	 */

	public void clear()
	{
		actualElements.clear();
		clearVisibleElements();
	}

	private void clearVisibleElements()
	{
		clearVisibleElements( this );
		for ( int i = 0; i < mirrorList.size(); ++i )
			clearVisibleElements( (LockableListModel) mirrorList.get(i) );
	}

	private static void clearVisibleElements( LockableListModel model )
	{
		int originalSize = model.getSize();
		model.visibleElements.clear();

		if ( originalSize != 0 )
			model.fireIntervalRemoved( model, 0, originalSize - 1 );
	}

	/**
	 * Please refer to {@link java.util.List#contains(Object)} for more
	 * information regarding this function.
	 */

	public boolean contains( Object o )
	{	return actualElements.contains( o );
	}

	/**
	 * Please refer to {@link java.util.List#containsAll(Collection)} for more
	 * information regarding this function.
	 */

	public boolean containsAll( Collection c )
	{	return actualElements.containsAll( c );
	}

	/**
	 * Please refer to {@link java.util.List#equals(Object)} for more
	 * information regarding this function.
	 */

	public boolean equals( Object o )
	{	return o instanceof LockableListModel ? this == o : actualElements.equals( o );
	}

	/**
	 * Please refer to {@link java.util.List#get(int)} for more
	 * information regarding this function.
	 */

	public Object get( int index )
	{
		if ( index < 0 || index >= actualElements.size() )
			return null;

		return actualElements.get( index );
	}

	/**
	 * Please refer to {@link java.util.List#hashCode()} for more
	 * information regarding this function.
	 */

	public int hashCode()
	{	return actualElements.hashCode();
	}

	/**
	 * Please refer to {@link java.util.List#indexOf(Object)} for more
	 * information regarding this function.
	 */

	public int indexOf( Object o )
	{	return actualElements.indexOf( o );
	}

	/**
	 * Please refer to {@link java.util.List#isEmpty()} for more
	 * information regarding this function.
	 */

	public boolean isEmpty()
	{	return actualElements.isEmpty();
	}

	/**
	 * Internal class used to handle iterators.  This is done to
	 * ensure that all applicable interface structures are notified
	 * whenever changes are made to the list elements.
	 */

	private class ListModelIterator implements ListIterator
	{
		private int nextIndex, previousIndex;
		private boolean isIncrementing;

		public ListModelIterator()
		{	this( 0 );
		}

		public ListModelIterator( int initialIndex )
		{
			nextIndex = 0;
			previousIndex = -1;
			isIncrementing = true;
		}

		public boolean hasPrevious()
		{	return previousIndex > 0;
		}

		public boolean hasNext()
		{	return nextIndex < LockableListModel.this.actualElements.size();
		}

		public Object next()
		{
			isIncrementing = true;
			Object nextObject = LockableListModel.this.get( nextIndex );
			++nextIndex;  ++previousIndex;
			return nextObject;
		}

		public Object previous()
		{
			isIncrementing = false;
			Object previousObject = LockableListModel.this.get( previousIndex );
			--nextIndex;  --previousIndex;
			return previousObject;
		}

		public int nextIndex()
		{	return nextIndex;
		}

		public int previousIndex()
		{	return previousIndex;
		}

		public void add( Object o )
		{
			LockableListModel.this.add( nextIndex, o );
			++nextIndex;  ++previousIndex;
		}

		public void remove()
		{
			if ( isIncrementing )
			{
				--nextIndex;  --previousIndex;
				LockableListModel.this.remove( nextIndex );
			}
			else
			{
				++nextIndex;  ++previousIndex;
				LockableListModel.this.remove( previousIndex );
			}
		}

		public void set( Object o )
		{
			LockableListModel.this.set( isIncrementing ?
				nextIndex - 1 : previousIndex + 1, o );
		}
	}

	/**
	 * Please refer to {@link java.util.List#iterator()} for more
	 * information regarding this function.
	 */

	public Iterator iterator()
	{	return new ListModelIterator();
	}

	/**
	 * Please refer to {@link java.util.Vector#lastElement()} for more
	 * information regarding this function.
	 */

	public Object lastElement()
	{	return actualElements.isEmpty() ? null : actualElements.get( actualElements.size() - 1 );
	}

	/**
	 * Please refer to {@link java.util.List#lastIndexOf(Object)} for more
	 * information regarding this function.
	 */

	public int lastIndexOf( Object o )
	{	return indexOf( o );
	}

	/**
	 * Please refer to {@link java.util.List#listIterator()} for more
	 * information regarding this function.
	 */

	public ListIterator listIterator()
	{	return new ListModelIterator();
	}

	/**
	 * Please refer to {@link java.util.List#listIterator(int)} for more
	 * information regarding this function.
	 */

	public ListIterator listIterator( int index )
	{	return new ListModelIterator( index );
	}


	/**
	 * Please refer to {@link java.util.List#remove(int)} for more
	 * information regarding this function.
	 */

	public Object remove( int index )
	{
		if ( index < 0 || index >= actualElements.size() )
			return null;

		Object returnValue = actualElements.remove( index );
		removeVisibleElement( index, returnValue );

		return returnValue;
	}

	private void removeVisibleElement( int index, Object element )
	{
		removeVisibleElement( this, index, element );
		for ( int i = 0; i < mirrorList.size(); ++i )
			removeVisibleElement( (LockableListModel) mirrorList.get(i), index, element );
	}

	private static void removeVisibleElement( LockableListModel model, int index, Object element )
	{
		if ( model.currentFilter.isVisible( element ) )
		{
			int visibleIndex = model.computeVisibleIndex( index );
			model.visibleElements.remove( visibleIndex );
			model.fireIntervalRemoved( model, visibleIndex, visibleIndex );
		}
	}

	/**
	 * Please refer to {@link java.util.List#remove(Object)} for more
	 * information regarding this function.
	 */

	public boolean remove( Object o )
	{	return remove( indexOf( o ) ) != null;
	}

	/**
	 * Please refer to {@link java.util.List#removeAll(Collection)} for more
	 * information regarding this function.
	 */

	public boolean removeAll( Collection c )
	{
		int originalSize = actualElements.size();

		Iterator it = c.iterator();
		while ( it.hasNext() )
			remove( it.next() );

		return originalSize != actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#retainAll(Collection)} for more
	 * information regarding this function.
	 */

	public boolean retainAll( Collection c )
	{
		int originalSize = actualElements.size();

		Iterator it = iterator();
		while ( it.hasNext() )
			if ( !c.contains( it.next() ) )
				it.remove();

		return originalSize != actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#set(int,Object)} for more
	 * information regarding this function.
	 */

	public Object set( int index, Object element )
	{
		if ( element == null )
			return null;

		Object returnValue = actualElements.set( index, element );
		setVisibleElement( index, element, returnValue );

		return returnValue;
	}

	private void setVisibleElement( int index, Object element, Object originalValue )
	{
		setVisibleElement( this, index, element, originalValue );
		for ( int i = 0; i < mirrorList.size(); ++i )
			setVisibleElement( (LockableListModel) mirrorList.get(i), index, element, originalValue );
	}

	private static void setVisibleElement( LockableListModel model, int index, Object element, Object originalValue )
	{
		int visibleIndex = model.computeVisibleIndex( index );

		if ( originalValue != null && model.currentFilter.isVisible( originalValue ) )
		{
			if ( !model.currentFilter.isVisible( element ) )
			{
				model.visibleElements.remove( visibleIndex );
				model.fireIntervalRemoved( model, visibleIndex, visibleIndex );
			}
			else if ( visibleIndex == model.visibleElements.size() )
			{
				model.visibleElements.add( visibleIndex, element );
				model.fireIntervalAdded( model, visibleIndex, visibleIndex );
			}
			else
			{
				model.visibleElements.set( visibleIndex, element );
				model.fireContentsChanged( model, visibleIndex, visibleIndex );
			}
		}
		else if ( model.currentFilter.isVisible( element ) )
		{
			model.visibleElements.add( visibleIndex, element );
			model.fireIntervalAdded( model, visibleIndex, visibleIndex );
		}
	}

	/**
	 * Please refer to {@link java.util.List#size()} for more
	 * information regarding this function.
	 */

	public int size()
	{	return actualElements.size();
	}

	/**
	 * Please refer to {@link java.util.List#subList(int,int)} for more
	 * information regarding this function.
	 */

	public List subList( int fromIndex, int toIndex )
	{	return actualElements.subList( fromIndex, toIndex );
	}

	/**
	 * Please refer to {@link java.util.List#toArray()} for more
	 * information regarding this function.
	 */

	public Object [] toArray()
	{	return actualElements.toArray();
	}

	/**
	 * Please refer to {@link java.util.List#toArray(Object[])} for more
	 * information regarding this function.
	 */

	public Object [] toArray( Object[] a )
	{	return actualElements.toArray(a);
	}

	public void applyListFilters()
	{
		applyListFilter( this.currentFilter );

		LockableListModel model;
		for ( int i = 0; i < mirrorList.size(); ++i )
		{
			model = (LockableListModel) mirrorList.get(i);
			model.applyListFilter( model.currentFilter );
		}
	}

	/**
	 * Filters the current list using the provided filter.
	 */

	public void applyListFilter( ListElementFilter newFilter )
	{
		Object element;
		visibleElements.clear();

		for ( int i = 0; i < actualElements.size(); ++i )
		{
			element = actualElements.get(i);
			if ( newFilter.isVisible( element ) )
				visibleElements.add( element );
		}

		this.currentFilter = newFilter;
		fireContentsChanged( this, 0, actualElements.size() );
	}

	private int computeVisibleIndex( int actualIndex )
	{
		int visibleIndex = actualIndex;
		for ( int i = 0; i < actualIndex; ++i )
			if ( !currentFilter.isVisible( actualElements.get(i) ) )
				--visibleIndex;

		return visibleIndex;
	}

	private int computeActualIndex( int visibleIndex )
	{
		int actualIndex = visibleIndex;
		for ( int i = 0; i <= actualIndex; ++i )
			if ( !currentFilter.isVisible( actualElements.get(i) ) )
				++actualIndex;

		return actualIndex;
	}

	/**
	 * Please refer to {@link javax.swing.ListModel#getElementAt(int)} for more
	 * information regarding this function.
	 */

	public Object getElementAt( int index )
	{	return index < 0 || index >= visibleElements.size() ? null : visibleElements.get( index );
	}

	/**
	 * Please refer to {@link javax.swing.ListModel#getSize()} for more
	 * information regarding this function.
	 */

	public int getSize()
	{	return visibleElements.size();
	}

    /**
     * Please refer to {@link javax.swing.ComboBoxModel#getSelectedItem()} for more
     * information regarding this function.
     */

    public Object getSelectedItem()
    {	return contains( selectedValue ) ? selectedValue : null;
	}

	/**
	 * Returns the index of the currently selected item in this <code>LockableListModel</code>.
	 * This is used primarily in cloning, to ensure that the same indices are being selected;
	 * however it may also be used to report the index of the currently selected item in testing
	 * a new object which uses a list model.
	 *
	 * @return	the index of the currently selected item
	 */

	public int getSelectedIndex()
	{	return visibleElements.indexOf( selectedValue );
	}

    /**
     * Please refer to {@link javax.swing.ComboBoxModel#setSelectedItem(Object)} for more
     * information regarding this function.
     */

	public void setSelectedItem( Object o )
	{
		if ( o == selectedValue )
			return;

		selectedValue = o;

		if ( selectedValue != null && contains( selectedValue ) )
			fireContentsChanged( this, -1, -1 );
	}

	/**
	 * Sets the given index in this <code>LockableListModel</code> as the currently
	 * selected item.  This is meant to be a complement to setSelectedItem(), and also
	 * functions to help in the cloning process.
	 */

	public void setSelectedIndex( int index )
	{	setSelectedItem( getElementAt( index ) );
	}

    /**
     * Please refer to {@link javax.swing.MutableComboBoxModel#addElement(Object)} for more
     * information regarding this function.
     */

	public void addElement( Object element )
	{	add( element );
	}

    /**
     * Please refer to {@link javax.swing.MutableComboBoxModel#insertElementAt(Object,int)} for more
     * information regarding this function.
     */

	public void insertElementAt( Object element, int index )
	{	add( element );
	}

    /**
     * Please refer to {@link javax.swing.MutableComboBoxModel#removeElement(Object)} for more
     * information regarding this function.
     */

	public void removeElement( Object element )
	{	remove( element );
	}

    /**
     * Please refer to {@link javax.swing.MutableComboBoxModel#removeElementAt(int)} for more
     * information regarding this function.
     */

	public void removeElementAt( int index )
	{	remove( computeActualIndex( index ) );
	}

	/**
	 * Returns a deep copy of the data associated with this <code>LockableListModel</code>.
	 * Note that any subclasses must override this method in order to ensure that the object can
	 * be cast appropriately; note also that the listeners are not inherited by the clone copy,
	 * since this violates the principle of independence.  If they are required, then the
	 * class using this model should add them using the functions provided in the <code>ListModel</code>
	 * interface.  Note also that if an element added to the list does not implement the
	 * <code>Cloneable</code> interface, or implements it by causing it to fail by default, this
	 * method will not fail; it will add a reference to the object, in effect creating a shallow
	 * copy of it.  Thus, retrieving an object using get() and modifying a field will result
	 * in both <code>LockableListModel</code> objects changing, in the same way retrieving
	 * an element from a cloned <code>ArrayList</code> will.
	 *
	 * @return	a deep copy (exempting listeners) of this <code>LockableListModel</code>.
	 */

	public Object clone()
	{
		try
		{
			LockableListModel cloneCopy = (LockableListModel) super.clone();
			cloneCopy.listenerList = new javax.swing.event.EventListenerList();

			cloneCopy.actualElements = cloneList( actualElements );
			cloneCopy.visibleElements = cloneList( visibleElements );

			cloneCopy.currentFilter = this.currentFilter;
			cloneCopy.selectedValue = null;

			return cloneCopy;
		}
		catch ( CloneNotSupportedException e )
		{
			// Because none of the super classes support clone(), this means
			// that this method is overriding the one found in Object.  Thus,
			// this exception should never be thrown, unless one of the super
			// classes is re-written to throw the exception by default.
			throw new RuntimeException( "AbstractListModel or one of its superclasses was rewritten to throw CloneNotSupportedException by default, call to clone() was unsuccessful" );
		}
	}

	/**
	 * Because <code>ArrayList</code> only creates a shallow copy of the objects,
	 * the one used as a data structure here must be cloned manually in order
	 * to satifsy the contract established by <code>clone()</code>.  However,
	 * the individual elements are known to be of class <code>Object</code>,
	 * and objects only force the clone() method to be protected.  Thus, in
	 * order to invoke clone() on each individual element, it must be done
	 * reflectively, which is the purpose of this private method.
	 *
	 * @return	as deep a copy of the object as can be obtained
	 */

	private ArrayList cloneList( ArrayList listToClone )
	{
		ArrayList clonedList = new ArrayList();

		for ( int i = 0; i < listToClone.size(); ++i )
			clonedList.add( attemptClone( listToClone.get(i) ) );

		return clonedList;
	}

	/**
	 * A private function which attempts to clone the object, if
	 * and only if the object implements the <code>Cloneable</code>
	 * interface.  If the object provided does not implement the
	 * <code>Cloneable</code> interface, or the clone() method is
	 * protected (as it is in class <code>Object</code>), then the
	 * original object is returned.
	 *
	 * @param	o	the object to be cloned
	 * @return	a copy of the object, either shallow or deep, pending
	 *			whether the original object was intended to be able to
	 *			be deep-copied
	 */

	private static Object attemptClone( Object o )
	{
		if ( !( o instanceof Cloneable ) )
			return o;

		java.lang.reflect.Method cloneMethod;

		try
		{
			// The function clone() has no parameters; this implementation
			// is implementation and specification dependent, and is used
			// with the traditional rule about null for a parameter list in
			// Class and Method indicating a zero-length parameter list.
			cloneMethod = o.getClass().getDeclaredMethod( "clone", null );
		}
		catch ( SecurityException e )
		{
			// If the methods of this function cannot be accessed
			// because it is protected, then it cannot be called
			// from this context - the original object should be
			// returned in this case.
			return o;
		}
		catch ( NoSuchMethodException e )
		{
			// This exception should never be thrown because all
			// objects have the clone() function.  If it is thrown,
			// then the clone() method was somehow deleted from
			// class Object (lack of backwards compatibility).
			// In this case, the original object should be returned.
			return o;
		}

		try
		{
			// The function clone() has no parameters; this implementation
			// is implementation and specification dependent, and is used
			// with the traditional rule about null for a parameter list in
			// Class and Method indicating a zero-length parameter list.
			return cloneMethod.invoke( o, null );
		}
		catch ( IllegalAccessException e )
		{
			// This exception should not occur, since the SecurityException
			// would have been thrown in the cases where this would have occurred.
			// But, if it does happen to occur *after* the SecurityException
			// caught all the instances, then something is wrong.
			throw new InternalError("accessible clone() method exists, but IllegalAccessException thrown");
		}
		catch ( IllegalArgumentException e )
		{
			// This exception should not occur, since the NoSuchMethodException
			// would have been thrown in the cases where this would have occurred.
			// But, if it does happen to occur *after* the NoSuchMethodException
			// caught all the instances, then something is wrong.
			throw new InternalError("accessible clone() method exists, but IllegalArgumentException thrown when no arguments are provided");
		}
		catch ( java.lang.reflect.InvocationTargetException e )
		{
			// The only exception normally thrown by the clone() operation is
			// the CloneNotSupportedException.  If this is thrown by the element,
			// then it is known that even if it implements the Cloneable interface,
			// it throws the exception by default - return the original object.
			return o;
		}
	}

	/**
	 * Special class which allows you to filter elements inside of
	 * this list model.
	 */

	public static class ListElementFilter
	{
		public boolean isVisible( Object element )
		{	return true;
		}
	}

	/**
	 * Returns a mirror image of this <code>LockableListModel</code>.  In essence,
	 * the object returned will be a clone of the original object.  However, it has
	 * the additional feature of listening for changes to the <em>underlying data</em> of this
	 * <code>LockableListModel</code>.  Note that this means any changes in selected
	 * indices will not be mirrored in the mirror image.  Note that because this function
	 * modifies the listeners for this class, an asynchronous version is not available.
	 *
	 * @return	a mirror image of this <code>LockableListModel</code>
	 */

	public LockableListModel getMirrorImage()
	{	return getMirrorImage( NO_FILTER );
	}

	/**
	 * Returns a mirror image of this <code>LockableListModel</code>.  In essence,
	 * the object returned will be a clone of the original object.  However, it has
	 * the additional feature of listening for changes to the <em>underlying data</em> of this
	 * <code>LockableListModel</code>.  Note that this means any changes in selected
	 * indices will not be mirrored in the mirror image.  Note that because this function
	 * modifies the listeners for this class, an asynchronous version is not available.
	 *
	 * @return	a mirror image of this <code>LockableListModel</code>
	 */

	public LockableListModel getMirrorImage( ListElementFilter filter )
	{
		LockableListModel mirrorImage = new LockableListModel();

		mirrorList.add( mirrorImage );
		mirrorImage.actualElements = this.actualElements;
		mirrorImage.applyListFilter( filter );

		return mirrorImage;
	}
}
