/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.AdventureResult;

/**
 * Internal class which functions exactly an array of strings, except it uses "sets" and "gets" like a list. This
 * could be done with generics (Java 1.5) but is done like this so that we get backwards compatibility.
 */

public class AdventureResultArray
{
	private final ArrayList<AdventureResult> internalList = new ArrayList<AdventureResult>();

	public AdventureResult get( final int index )
	{
		return index < 0 || index >= this.internalList.size() ? null : this.internalList.get( index );
	}

	public void set( final int index, final AdventureResult value )
	{
		while ( index >= this.internalList.size() )
		{
			this.internalList.add( null );
		}

		this.internalList.set( index, value );
	}

	public void add( final AdventureResult s )
	{
		this.internalList.add( s );
	}

	public void clear()
	{
		this.internalList.clear();
	}

	public AdventureResult[] toArray()
	{
		AdventureResult[] array = new AdventureResult[ this.internalList.size() ];
		this.internalList.toArray( array );
		return array;
	}

	public int size()
	{
		return this.internalList.size();
	}

	public boolean isEmpty()
	{
		return this.internalList.size() == 0;
	}

	public boolean contains( AdventureResult ar )
	{
		return this.internalList.contains( ar );
	}
}
