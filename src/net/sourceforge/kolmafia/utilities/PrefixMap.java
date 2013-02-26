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

import java.util.NoSuchElementException;
import java.util.TreeMap;

/*
This is a minimal implementation of a variant SortedMap (currently based on a
TreeMap), mapping Strings to Objects, with two distinct kinds of keys:
* putExact() creates a key that has to exactly match, as usual.
* putPrefix() creates a key that will also be matched by any longer string
that startsWith the key.

Performance should be equal to a TreeMap with twice as many keys, in other
words lookup operations should take only a constant amount of extra time. 
It's definitely faster than repeatedly chopping off the last character of a
string and trying the lookup again.

Null values are allowed, however keys that map to null cannot be
distinguished from non-existent keys.

There are some limitations on keys that can be inserted into the map, which
shouldn't present any difficulties for the expected usage of human-typed
keywords:
* The last character of a key cannot be the null character.
* The last character of a key cannot be greater than or equal to
PREFIX_SUFFIX, which is currently set to Unicode code point 0xFFFF.  This may
prove to be a problem if Java ever gains full support for characters outside
of the Basic Multilingual Plane.

There are currently some limitations on the order in which keys can be
inserted and removed.  None of these are fundamentally impossible to
overcome, however they'd make it much harder to extract a list of unique keys
from the map, which seems a more important capability.
* If both a prefix key, and a longer key (of either type) that matches that
prefix are to be added to the map, the shorter prefix MUST be added first. 
This is not currently checked; the expected symptom is that keys that should
match the prefix, but are alphabetically after the longer key, will return
null.  One important consequence of this: if you want to add the empty string
as a prefix key, it has to be the very first key added.
* Exact keys can always be safely removed from the map.  Prefix keys can only
be removed if no longer key that matches that prefix has been added to the
map.
*/

public class PrefixMap
	extends TreeMap<String, Object>
{
	// Return values for getKeyType
	public static final int NOT_A_KEY = 0;
	public static final int EXACT_KEY = 1;
	public static final int PREFIX_KEY = 2;

	private static final String EXACT_SUFFIX = "\u0000";
	private static final String PREFIX_SUFFIX = "\uFFFF";

	public PrefixMap()
	{
		super();
	}
	
	public Object get( String key )
	{
		try
		{
			return super.get( super.headMap( key + EXACT_SUFFIX ).lastKey() );
		}
		catch ( NoSuchElementException e )
		{
			return null;
		}
	}
	
	// This returns the value that would be found if the key didn't exist.
	private Object getBelow( String key )
	{
		try
		{
			return super.get( super.headMap( key ).lastKey() );
		}
		catch ( NoSuchElementException e )
		{
			return null;
		}
	}

	public void putExact( String key, Object value )
	{
		this.putRange( key, key + EXACT_SUFFIX, value );
	}

	public void putPrefix( String key, Object value )
	{
		this.putRange( key, key + PREFIX_SUFFIX, value );
	}

	private void putRange( String startKey, String endKey, Object value )
	{
		super.put( startKey, value );
		super.put( endKey, this.getBelow( startKey ) );
	}
	
	public Object remove( String key )
	{
		super.remove( key + EXACT_SUFFIX );
		super.remove( key + PREFIX_SUFFIX );
		return super.remove( key );
	}
	
	public int getKeyType( String key )
	{
		if ( key.endsWith( EXACT_SUFFIX ) || key.endsWith( PREFIX_SUFFIX ) )
		{
			return NOT_A_KEY;
		}
		if ( super.containsKey( key + EXACT_SUFFIX ) )
		{
			return EXACT_KEY;
		}
		return PREFIX_KEY;
	}
	
	// The following methods, inherited from TreeMap, should be safe to call
	// on a PrefixMap (but not necessarily very useful):
	//
	// clear()
	// clone() 
	// comparator() - will always return null.
	// containsValue(Object value) - horribly inefficient, of course.
	// entrySet(), keySet() - but you must use getKeyType on each returned key, to tell whether
	//	it's a real key (and of which type), rather than an implementation artifact.  Also,
	//	don't use setValue() on any Map.Entry you get from entrySet().
	// firstKey, headMap, lastKey, subMap, tailMap - probably not useful.
	// size() - but the return value is twice the number of keys.
	// values() - will include lots of null values, in addition to actual mapped values.

	// The following TreeMap methods cannot be used with PrefixMap:
	//
	// All constructors that specify a Comparator (normal string ordering is assumed).
	// All comstructors that load initial values (which key type would they be?).
	// containsKey( Object key ) - harmless, but the result is meaningless.
	// put( Object key, Object value ) - must use putExact or putPrefix instead.
	// putAll( Map map ) - would need some way to indicate which type of keys to use
}
