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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * An extension of the {@link net.java.dev.spellcast.utilities.LockableListModel} which maintains
 * elements in ascending order, where elements can only be added and replaced if they do not
 * disturb the sorted property of the <code>List</code>.  The <code>SortedListModel</code> adds
 * the additional restriction that, if an element <tt>e2</tt> is added to the list after another
 * element <tt>e1</tt> is added, and <tt>e1.equals(e2)</tt> returns <tt>true</tt>, then <tt>e2</tt>
 * must appear in the list after <tt>e1</tt>.
 */

public class SortedListModel extends LockableListModel
{
	private static final int FIRST_INDEX = 0;
	private static final int LAST_INDEX  = -1;
	private static final int INSERTION   = -2;

	private Class associatedClass;

	/**
	 * Constructs a new <code>SortedListModel</code>.  In essence, all this
	 * class does is call the constructor for the <code>LockableListModel</code>
	 * with the class object associated with <code>java.lang.Comparable</code>.
	 */

	public SortedListModel()
	{	this( Comparable.class );
	}

	/**
	 * Constructs a specialized list, where all objects inserted
	 * into the list are guaranteed to be an instance of the class
	 * or interface noted.
	 *
	 * @param	className	the fully-qualified name of the appropriate class
	 */

	public SortedListModel( String className )
	{
		try
		{	associatedClass = Class.forName( className );
		}
		catch ( ClassNotFoundException e )
		{	throw new IllegalArgumentException( className + " does not exist" );
		}

		if ( !Comparable.class.isAssignableFrom( associatedClass ) )
			throw new IllegalArgumentException( associatedClass.getName() + " does not implement Comparable" );
	}

	/**
	 * Constructs a new <code>SortedListModel</code> where all the elements are of
	 * the given type.  Note that the given class name must indicate a class that
	 * implements <code>java.lang.Comparable</code>.
	 *
	 * @param	associatedClass	the class object indicative of the elements to be inserted
	 */

	public SortedListModel( Class associatedClass )
	{
		this.associatedClass = associatedClass;
		if ( !Comparable.class.isAssignableFrom( associatedClass ) )
			throw new IllegalArgumentException( associatedClass.getName() + " does not implement Comparable" );
	}

    /**
     * Please refer to {@link java.util.List#add(int,Object)} for more
     * information regarding this function.
     */

	public void add( int index, Object element )
	{
		boolean needsSort = ( index > 0 && ((Comparable)element).compareTo( get( index - 1 ) ) < 0 ) ||
			( index < size() && ((Comparable)element).compareTo( get( index ) ) > 0 );

		super.add( index, element );

		if ( needsSort )
			java.util.Collections.sort( this );
	}

    /**
     * Please refer to {@link java.util.List#add(Object)} for more
     * information regarding this function.
     */

	public boolean add( Object o )
	{
		try
		{
			add( indexOf( 0, size() - 1, (Comparable)o, INSERTION ), o );
			return true;
		}
		catch ( IllegalArgumentException e1 )
		{	return false;
		}
		catch ( ClassCastException e2 )
		{	return false;
		}
	}

	public boolean addAll( Collection c )
	{
		if ( isEmpty() )
		{
			ArrayList interimList = new ArrayList();
			interimList.addAll( c );
			Collections.sort( interimList );
			return super.addAll( interimList );
		}

		return super.addAll( c );
	}

    /**
     * Please refer to {@link java.util.List#indexOf(Object)} for more
     * information regarding this function.
     */

	public int indexOf( Object o )
	{
		if ( !associatedClass.isInstance( o ) )
			return -1;
		return indexOf( 0, size() - 1, (Comparable)o, FIRST_INDEX );
	}

    /**
     * Please refer to {@link java.util.List#indexOf(Object)} for more information
     * regarding this function.  However, note also that a <code>SortedListModel</code>
     * maintains that if an element <tt>e2</tt> is added to the list after another
     * element <tt>e1</tt> is added, and <tt>e1.equals(e2)</tt> returns <tt>true</tt>,
     * then <tt>e2</tt> must appear in the list after <tt>e1</tt>.  Thus, the
     * calculation of the last index of an object can be done in <code>log( n )</code>
     * time by locating the correct index for the insertion of the object, which is
     * done in <code>log( n )</code> time, and checking the index immediately previous
     * to it.
     */

	public int lastIndexOf( Object o )
	{
		if ( !associatedClass.isInstance( o ) )
			return -1;

		return indexOf( 0, size() - 1, (Comparable)o, LAST_INDEX );
	}

    /**
     * Please refer to {@link java.util.List#set(int,Object)} for more
     * information regarding this function.
     */

	public Object set( int index, Object element )
	{
		boolean needsSort = ( index > 0 && ((Comparable)element).compareTo( get( index - 1 ) ) < 0 ) ||
			( index + 1 < size() && ((Comparable)element).compareTo( get( index + 1 ) ) > 0 );

		Object oldvalue = super.set( index, element );

		if ( needsSort )
			java.util.Collections.sort( this );

		return oldvalue;
	}

	/**
	 * A helper function which calculates the index of an element using
	 * binary search.  In most cases, the difference is minimal, since
	 * most <code>ListModel</code> objects are fairly small.  However,
	 * in the event that there are multiple <code>SortedListModel</code>
	 * objects of respectable size, having good performance is ideal.
	 */

	private int indexOf( int beginIndex, int endIndex, Comparable element, int whichIndexOf )
	{
		// if the binary search has been terminated, return -1
		int totalListSize = size();

		if ( beginIndex < 0 || beginIndex > endIndex || endIndex >= totalListSize || beginIndex + whichIndexOf >= totalListSize )
			return whichIndexOf == INSERTION ? beginIndex : -1;

		// calculate the halfway point and compare the element with the
		// element located at the halfway point - note that in locating
		// the last index of, the value is rounded up to avoid an infinite
		// recursive loop

		int halfwayIndex = beginIndex + endIndex;
		if ( whichIndexOf == LAST_INDEX || whichIndexOf == INSERTION )
			++halfwayIndex;
		halfwayIndex >>= 1;

		Comparable halfwayElement = (Comparable) get( halfwayIndex );
		int compareResult = halfwayElement.compareTo( element );

		// if the element in the middle is larger than the element being checked,
		// then it is known that the element is smaller than the middle element,
		// so it must preceed the middle element

		if ( compareResult > 0 )
			return indexOf( beginIndex, halfwayIndex - 1, element, whichIndexOf );

		// if the element in the middle is smaller than the element being checked,
		// then it is known that the element is larger than the middle element, so
		// it must succeed the middle element

		if ( compareResult < 0 )
			return indexOf( halfwayIndex + 1, endIndex, element, whichIndexOf );

		// if the element in the middle is equal to the element being checked,
		// then it is known that you have located at least one occurrence of the
		// object; if the range has not yet been narrowed completely, continue
		// to narrow the range - locate the last index of the element by looking
		// in the second half, and locate all others by looking in the first half

		if ( beginIndex != endIndex )
		{
			if ( whichIndexOf == LAST_INDEX || whichIndexOf == INSERTION )
				return indexOf( halfwayIndex, endIndex, element, whichIndexOf );
			return indexOf( beginIndex, halfwayIndex, element, whichIndexOf );
		}

		// if the range has been narrowed completely, then you have either found
		// the first or last occurrence, pending the value of [whichIndexOf] used;
		// if searching for the last index, then the index found is guaranteed
		// to be valid, so simply return it

		if ( whichIndexOf == LAST_INDEX )
			return halfwayIndex;

		// by the principle of the SortedListModel, the insertion index, if an
		// element is found, will be immediately after the last instance of it

		if ( whichIndexOf == INSERTION )
			return halfwayIndex + 1;

		// in all other cases, one must ensure that the [whichIndexOf]-th element
		// exists, and this is done by checking that the element at the [whichIndexOf]
		// position relative to the current checking point exists and is equal to the
		// element being located

		return element.equals( get( halfwayIndex + whichIndexOf ) ) ?
			halfwayIndex + whichIndexOf : -1;
	}

	public Object clone()
	{
		SortedListModel cloneCopy = (SortedListModel) super.clone();
		cloneCopy.associatedClass = associatedClass;
		return cloneCopy;
	}
}