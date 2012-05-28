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

import java.util.Collection;

/**
 * An extension of the {@link net.java.dev.spellcast.utilities.LockableListModel} which maintains elements in ascending
 * order, where elements can only be added and replaced if they do not disturb the sorted property of the
 * <code>List</code>.
 */

public class SortedListModel
	extends LockableListModel
{
	/**
	 * Constructs a new <code>SortedListModel</code>. In essence, all this class does is call the constructor for the
	 * <code>LockableListModel</code> with the class object associated with <code>java.lang.Comparable</code>.
	 */

	public SortedListModel()
	{
	}

	/**
	 * Please refer to {@link java.util.List#add(int,Object)} for more information regarding this function. Note that if
	 * the position is invalid (ie: it does not result in a sorted property), the element will be successfully added,
	 * but to a different position.
	 */

	@Override
	public void add( final int index, final Object element )
	{
		if ( element == null )
		{
			return;
		}

		this.add( element );
	}

	/**
	 * Please refer to {@link java.util.List#add(Object)} for more information regarding this function.
	 */

	@Override
	public boolean add( final Object o )
	{
		if ( o == null )
		{
			return false;
		}

		try
		{
			super.add( this.insertionIndexOf( 0, this.size() - 1, (Comparable) o ), o );
			return true;
		}
		catch ( IllegalArgumentException e1 )
		{
			return false;
		}
		catch ( ClassCastException e2 )
		{
			return false;
		}
	}

	/**
	 * Please refer to {@link java.util.List#addAll(int,Collection)} for more information regarding this function.
	 */

	@Override
	public boolean addAll( final int index, final Collection c )
	{
		boolean result = super.addAll( index, c );
		
		if ( !result )
		{
			return false;
		}
		
		this.sort();
		return true;
	}

	/**
	 * Please refer to {@link java.util.List#indexOf(Object)} for more information regarding this function.
	 */

	@Override
	public int indexOf( final Object o )
	{
		return o == null ? -1 : this.normalIndexOf( 0, this.size() - 1, (Comparable) o );
	}

	/**
	 * Please refer to {@link java.util.List#contains(Object)} for more information regarding this function.
	 */

	@Override
	public boolean contains( final Object o )
	{
		return this.indexOf( o ) != -1;
	}

	/**
	 * A helper function which calculates the index of an element using binary search. In most cases, the difference is
	 * minimal, since most <code>ListModel</code> objects are fairly small. However, in the event that there are
	 * multiple <code>SortedListModel</code> objects of respectable size, having good performance is ideal.
	 */

	private int normalIndexOf( int beginIndex, int endIndex, final Comparable element )
	{
		int compareResult;

		while ( true )
		{
			if ( beginIndex == endIndex )
			{
				compareResult = this.compare( element, (Comparable) this.get( beginIndex ) );
				return compareResult == 0 ? beginIndex : -1;
			}
	
			if ( beginIndex > endIndex )
			{
				return -1;
			}
	
			// calculate the halfway point and compare the element with the
			// element located at the halfway point - note that in locating
			// the last index of, the value is rounded up to avoid an infinite
			// recursive loop
	
			int halfwayIndex = beginIndex + endIndex >> 1;
			compareResult = this.compare( (Comparable) this.get( halfwayIndex ), element );
	
			// if the element in the middle is larger than the element being checked,
			// then it is known that the element is smaller than the middle element,
			// so it must preceed the middle element
	
			if ( compareResult > 0 )
			{
				endIndex = halfwayIndex - 1;
				continue;
			}
	
			// if the element in the middle is smaller than the element being checked,
			// then it is known that the element is larger than the middle element, so
			// it must succeed the middle element
	
			if ( compareResult < 0 )
			{
				beginIndex = halfwayIndex + 1;
				continue;
			}
	
			// if the element in the middle is equal to the element being checked,
			// then it is known that you have located at least one occurrence of the
			// object; because duplicates are not allowed, return the halfway point
	
			return halfwayIndex;
		}
	}

	private int insertionIndexOf( int beginIndex, int endIndex, final Comparable element )
	{
		int compareResult;

		while ( true )
		{
			if ( beginIndex == endIndex )
			{
				compareResult = this.compare( element, (Comparable) this.get( beginIndex ) );
				return compareResult < 0 ? beginIndex : beginIndex + 1;
			}
	
			if ( beginIndex > endIndex )
			{
				return beginIndex;
			}
	
			// calculate the halfway point and compare the element with the
			// element located at the halfway point - note that in locating
			// the last index of, the value is rounded up to avoid an infinite
			// recursive loop
	
			int halfwayIndex = beginIndex + endIndex >> 1;
			compareResult = this.compare( (Comparable) this.get( halfwayIndex ), element );
	
			// if the element in the middle is larger than the element being checked,
			// then it is known that the element is smaller than the middle element,
			// so it must preceed the middle element
	
			if ( compareResult > 0 )
			{
				endIndex = halfwayIndex - 1;
				continue;
			}
	
			// if the element in the middle is smaller than the element being checked,
			// then it is known that the element is larger than the middle element, so
			// it must succeed the middle element
	
			if ( compareResult < 0 )
			{
				beginIndex = halfwayIndex + 1;
				continue;
			}
	
			// if the element in the middle is equal to the element being checked,
			// then it is known that you have located at least one occurrence of the
			// object; because duplicates are not allowed, return the halfway point
	
			return halfwayIndex + 1;
		}
	}

	private int compare( final Comparable left, final Comparable right )
	{
		return left instanceof String || right instanceof String ?
			left.toString().compareToIgnoreCase( right.toString() ) : left.compareTo( right );
	}
}
