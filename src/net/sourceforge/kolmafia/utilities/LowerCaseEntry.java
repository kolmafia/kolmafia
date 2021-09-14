/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import net.java.dev.spellcast.utilities.LockableListModel;

public class LowerCaseEntry<K, V>
	implements Entry<K, V>
{
	private final Entry<K, V> original;
	private final K key;
	private V value;
	private String pairString, lowercase;

	public LowerCaseEntry( final Entry<K, V> original )
	{
		this.original = original;

		this.key = original.getKey();
		this.value = original.getValue();

		this.pairString = this.value + " (" + this.key + ")";
		this.lowercase = this.value.toString().toLowerCase();
	}

	public LowerCaseEntry( final K key, final V value )
	{
		this.original = null;

		this.key = key;
		this.value = value;

		this.pairString = this.value + " (" + this.key + ")";
		this.lowercase = this.value.toString().toLowerCase();
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( o instanceof Entry )
		{
			Entry<?, ?> entry = (Entry<?, ?>) o;

			return this.key.equals( entry.getKey() ) && this.value.equals( entry.getValue() );
		}

		return false;
	}

	public K getKey()
	{
		return this.key;
	}

	public V getValue()
	{
		return this.value;
	}

	@Override
	public String toString()
	{
		return this.pairString;
	}

	@Override
	public int hashCode()
	{
		return this.key.hashCode();
	}

	public V setValue( final V newValue )
	{
		V returnValue = this.value;
		this.value = newValue;

		if ( this.original != null )
		{
			this.original.setValue( newValue );
		}

		this.pairString = this.value + " (" + this.key + ")";
		this.lowercase = this.value.toString().toLowerCase();

		return returnValue;
	}

	public String getLowerCase()
	{
		return this.lowercase;
	}

	public static final <K, V> LockableListModel<LowerCaseEntry<K, V>> createListModel( final Set<Entry<K, V>> entries )
	{
		LockableListModel<LowerCaseEntry<K, V>> model = new LockableListModel<>();

		Iterator<Entry<K, V>> it = entries.iterator();
		while ( it.hasNext() )
		{
			model.add( new LowerCaseEntry<>( it.next() ) );
		}

		return model;
	}
}
