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

// list-related imports
import java.util.List;
import java.util.Vector;
import java.util.ListIterator;
import java.util.Collection;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

// update components
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

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
	private Vector elements;
	private Object selectedValue;

	/**
	 * Constructs a new <code>LockableListModel</code>.
	 */

	public LockableListModel()
	{
		elements = new Vector();
		selectedValue = null;
	}

	public LockableListModel( Collection c )
	{
		this();
		addAll( c );
	}

	public void sort()
	{
		Collections.sort( elements );
		fireContentsChanged( this, 0, size() - 1 );
	}

	public void sort( Comparator c )
	{
		Collections.sort( elements, c );
		fireContentsChanged( this, 0, size() - 1 );
	}

	/**
	 * Please refer to {@link java.util.List#add(int,Object)} for more
	 * information regarding this function.
	 */

	public void add( int index, Object element )
	{
		if ( element == null )
			throw new IllegalArgumentException( "cannot add a null object to this list" );

		elements.add( index, element );
		fireIntervalAdded( this, index, index );
	}

	/**
	 * Please refer to {@link java.util.List#add(Object)} for more
	 * information regarding this function.
	 */

	public boolean add( Object o )
	{
		if ( o == null )
			return false;

		add( size(), o );
		return true;
	}

	/**
	 * Please refer to {@link java.util.List#addAll(Collection)} for more
	 * information regarding this function.
	 */

	public boolean addAll( Collection c )
	{
		if ( isEmpty() )
		{
			elements.addAll( c );
			fireIntervalAdded( this, 0, elements.size() - 1 );
			return true;
		}

		try
		{
			Iterator myIterator = c.iterator();
			while ( myIterator.hasNext() )
				if ( !add( myIterator.next() ) )
					return false;
			return true;
		}
		catch( IllegalArgumentException e )
		{
			return false;
		}
	}

	/**
	 * Please refer to {@link java.util.List#addAll(int,Collection)} for more
	 * information regarding this function.
	 */

	public boolean addAll( int index, Collection c )
	{
		try
		{
			Iterator myIterator = c.iterator();
			for ( int i = index; myIterator.hasNext(); ++i )
				add( i, myIterator.next() );
			return true;
		}
		catch( IllegalArgumentException e )
		{
			return false;
		}
	}

	/**
	 * Please refer to {@link java.util.List#clear()} for more
	 * information regarding this function.
	 */

	public void clear()
	{
		int lastIndex = size() - 1;

		// If the size of the list model is 0, then
		// there's nothing to do.  Avoid misfiring
		// the action listeners in this case.

		if ( lastIndex == -1 )
			return;

		elements.clear();
		fireIntervalRemoved( this, 0, lastIndex );
	}

	/**
	 * Please refer to {@link java.util.List#contains(Object)} for more
	 * information regarding this function.
	 */

	public boolean contains( Object o )
	{	return elements.contains( o );
	}

	/**
	 * Please refer to {@link java.util.List#containsAll(Collection)} for more
	 * information regarding this function.
	 */

	public boolean containsAll( Collection c )
	{	return elements.containsAll( c );
	}

	public boolean equals( Object o )
	{	return this == o;
	}

	/**
	 * Please refer to {@link java.util.List#get(int)} for more
	 * information regarding this function.
	 */

	public Object get( int index )
	{
		if ( index < 0 || index >= size() )
			return null;
		return elements.get( index );
	}

	/**
	 * Please refer to {@link java.util.List#hashCode()} for more
	 * information regarding this function.
	 */

	public int hashCode()
	{	return elements.hashCode();
	}

	/**
	 * Please refer to {@link java.util.List#indexOf(Object)} for more
	 * information regarding this function.
	 */

	public int indexOf( Object o )
	{	return elements.indexOf( o );
	}

	/**
	 * Please refer to {@link java.util.List#isEmpty()} for more
	 * information regarding this function.
	 */

	public boolean isEmpty()
	{	return elements.isEmpty();
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
		{	return nextIndex < LockableListModel.this.size();
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
	{	return elements.isEmpty() ? null : elements.lastElement();
	}

	/**
	 * Please refer to {@link java.util.List#lastIndexOf(Object)} for more
	 * information regarding this function.
	 */

	public int lastIndexOf( Object o )
	{	return elements.lastIndexOf( o );
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
		if ( index < 0 || index >= size() )
			return null;

		Object removedElement = elements.remove( index );
		if ( removedElement == null )
			return null;
		fireIntervalRemoved( this, index, index );
		return removedElement;
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
		Iterator myIterator = c.iterator();
		while ( myIterator.hasNext() )
			if ( !remove( myIterator.next() ) )
				return false;
		return true;
	}

	/**
	 * Please refer to {@link java.util.List#retainAll(Collection)} for more
	 * information regarding this function.
	 */

	public boolean retainAll( Collection c )
	{
		boolean hasChanged = false;
		Object [] elements = toArray();

		for ( int i = 0; i < elements.length; ++i )
		{
			if ( !c.contains( elements[i] ) )
			{
				remove( elements[i] );
				hasChanged = true;
			}
		}
		return hasChanged;
	}

	/**
	 * Please refer to {@link java.util.List#set(int,Object)} for more
	 * information regarding this function.
	 */

	public Object set( int index, Object element )
	{
		if ( element == null )
			throw new IllegalArgumentException( "cannot add a null object to this list" );

		Object originalElement = get( index );
		elements.set( index, element );
		fireContentsChanged( this, index, index );
		return originalElement;
	}

	/**
	 * Please refer to {@link java.util.List#size()} for more
	 * information regarding this function.
	 */

	public int size()
	{	return elements.size();
	}

	/**
	 * Please refer to {@link java.util.List#subList(int,int)} for more
	 * information regarding this function.
	 */

	public List subList( int fromIndex, int toIndex )
	{	return elements.subList( fromIndex, toIndex );
	}

	/**
	 * Please refer to {@link java.util.List#toArray()} for more
	 * information regarding this function.
	 */

	public Object [] toArray()
	{	return elements.toArray();
	}

	/**
	 * Please refer to {@link java.util.List#toArray(Object[])} for more
	 * information regarding this function.
	 */

	public Object [] toArray( Object[] a )
	{	return elements.toArray(a);
	}

	/**
	 * Please refer to {@link javax.swing.ListModel#getElementAt(int)} for more
	 * information regarding this function.
	 */

	public Object getElementAt( int index )
	{	return get( index );
	}

	/**
	 * Please refer to {@link javax.swing.ListModel#getSize()} for more
	 * information regarding this function.
	 */

	public int getSize()
	{	return size();
	}

    /**
     * Please refer to {@link javax.swing.ComboBoxModel#getSelectedItem()} for more
     * information regarding this function.
     */

    public Object getSelectedItem()
    {	return selectedValue;
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
	{	return indexOf( selectedValue );
	}

    /**
     * Please refer to {@link javax.swing.ComboBoxModel#setSelectedItem(Object)} for more
     * information regarding this function.
     */

	public void setSelectedItem( Object o )
	{	setSelectedIndex( o == null ? -1 : indexOf( o ) );
	}

	/**
	 * Sets the given index in this <code>LockableListModel</code> as the currently
	 * selected item.  This is meant to be a complement to setSelectedItem(), and also
	 * functions to help in the cloning process.
	 */

	public void setSelectedIndex( int index )
	{
		selectedValue = index == -1 ? null : get( index );
		fireContentsChanged( this, -1, -1 );
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
	{	add( index, element );
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
	{	remove( index );
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
	 * an element from a cloned <code>Vector</code> will.
	 *
	 * @return	a deep copy (exempting listeners) of this <code>LockableListModel</code>.
	 */

	public Object clone()
	{
		try
		{
			LockableListModel cloneCopy = (LockableListModel) super.clone();
			cloneCopy.listenerList = new javax.swing.event.EventListenerList();
			cloneCopy.elements = cloneList();
			cloneCopy.setSelectedIndex( getSelectedIndex() );
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
	 * Because <code>Vector</code> only creates a shallow copy of the objects,
	 * the one used as a data structure here must be cloned manually in order
	 * to satifsy the contract established by <code>clone()</code>.  However,
	 * the individual elements are known to be of class <code>Object</code>,
	 * and objects only force the clone() method to be protected.  Thus, in
	 * order to invoke clone() on each individual element, it must be done
	 * reflectively, which is the purpose of this private method.
	 *
	 * @return	as deep a copy of the object as can be obtained
	 */

	private Vector cloneList()
	{
		Vector clonedList = new Vector();
		java.lang.reflect.Method cloneMethod;  Object toClone;

		for ( int i = 0; i < size(); ++i )
			clonedList.add( attemptClone( get(i) ) );

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
	{
		LockableListModel mirrorImage = (LockableListModel) clone();
		addListDataListener( new MirrorImageListener( mirrorImage ) );
		return mirrorImage;
	}

	/**
	 * An internal listener class which is added whenever a mirror image is created.
	 * The <code>MirrorImageListener</code> will respond to any changes in the
	 * <code>LockableListModel</code> by changing the underlying data of the
	 * mirror image(s) of the <code>LockableListModel</code>.
	 */

	private class MirrorImageListener implements ListDataListener
	{
		private LockableListModel mirrorImage;

		/**
		 * Constructs a new <code>MirrorImageListener</code> which will respond
		 * to changes in the class it's listening on by making changes to the
		 * given mirror image.  Note that it does not check to ensure that the
		 * given object is truly a copy of the original; thus, before creating a
		 * listener, a mirror image must be created.
		 *
		 * @param	mirrorImage	the mirror image of this <code>LockableListModel</code>
		 */

		public MirrorImageListener( LockableListModel mirrorImage )
		{	this.mirrorImage = mirrorImage;
		}

		/**
		 * Called whenever contents have been added to the original list; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalAdded( ListDataEvent e )
		{
			if ( e.getType() == ListDataEvent.INTERVAL_ADDED && e.getSource() instanceof LockableListModel )
				intervalAdded( (LockableListModel) e.getSource(), e.getIndex0(), e.getIndex1() );
		}

		/**
		 * Indicates that the given list has added elements.  This function then
		 * proceeds to add the elements within the given index range to the mirror
		 * image currently being stored.
		 *
		 * @param	source	the list that has changed
		 * @param	index0	the lower index in the range
		 * @param	index1	the upper index in the range
		 */

		private void intervalAdded( LockableListModel source, int index0, int index1 )
		{
			if ( mirrorImage == null || source == null || index0 < 0 || index1 < 0 || index1 >= source.size() )
				return;

			for ( int i = index0; i <= index1; ++i )
				mirrorImage.add( source.get(i) );
		}

		/**
		 * Called whenever contents have been removed from the original list;
		 * a function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void intervalRemoved( ListDataEvent e )
		{
			if ( e.getType() == ListDataEvent.INTERVAL_REMOVED && e.getSource() instanceof LockableListModel )
				intervalRemoved( (LockableListModel) e.getSource(), e.getIndex0(), e.getIndex1() );
		}

		/**
		 * Indicates that the given list has removed elements.  This function then
		 * proceeds to remove the elements within the given index range from the mirror
		 * image currently being stored.
		 *
		 * @param	source	the list that has changed
		 * @param	index0	the lower index in the range
		 * @param	index1	the upper index in the range
		 */

		private void intervalRemoved( LockableListModel source, int index0, int index1 )
		{
			if ( mirrorImage == null || source == null || index0 < 0 || index1 < 0 || index1 >= mirrorImage.size() )
				return;

			mirrorImage.retainAll( source );
		}

		/**
		 * Called whenever contents in the original list have changed; a
		 * function required by every <code>ListDataListener</code>.
		 *
		 * @param	e	the <code>ListDataEvent</code> that triggered this function call
		 */

		public void contentsChanged( ListDataEvent e )
		{
			if ( e.getType() == ListDataEvent.CONTENTS_CHANGED && e.getSource() instanceof LockableListModel )
				contentsChanged( (LockableListModel) e.getSource(), e.getIndex0(), e.getIndex1() );
		}

		/**
		 * Indicates that the given list has changed its contents.  This function then
		 * proceeds to change the elements within the given index range in the mirror
		 * image currently being stored to match the ones in the original list.
		 *
		 * @param	source	the list that has changed
		 * @param	index0	the lower index in the range
		 * @param	index1	the upper index in the range
		 */

		private void contentsChanged( LockableListModel source, int index0, int index1 )
		{
			if ( mirrorImage == null || source == null || index1 < 0 )
				return;

			for ( int i = index0; i <= index1; ++i )
				mirrorImage.set( i, source.get(i) );
		}
	}
}