/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import java.util.Iterator;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

/**
 * Internal class which functions exactly an array of integers, except it uses "sets" and "gets" like a list.
 *
 * This could be done with generics (Java 1.5) but is done like this so that we
 * get backwards compatibility.
 *
 * News flash! Since we have moved to Java 1.5, we can use generics
 */

public class IntegerArray
	implements Iterable<Integer>
{
	private final ArrayList<Integer> internalList = new ArrayList<Integer>();

	public Iterator<Integer> iterator()
	{
		return this.internalList.iterator();
	}

	public void add( final int value )
	{
		this.set( this.internalList.size(), value );
	}

	public int get( final int index )
	{
		return index < 0 || index >= this.internalList.size() ? 0 : ( (Integer) this.internalList.get( index ) ).intValue();
	}

	public void set( final int index, final int value )
	{
		while ( index >= this.internalList.size() )
		{
			this.internalList.add( IntegerPool.get( 0 ) );
		}

		this.internalList.set( index, IntegerPool.get( value ) );
	}

	public int size()
	{
		return this.internalList.size();
	}

	public boolean contains( final int value )
	{
		return this.internalList.contains( IntegerPool.get( value ) );
	}

	public int[] toArray()
	{
		int[] array = new int[ this.internalList.size() ];
		Iterator<Integer> iterator = this.internalList.iterator();

		for ( int i = 0; i < array.length; ++i )
		{
			array[ i ] = ( (Integer) iterator.next() ).intValue();
		}

		return array;
	}
}
