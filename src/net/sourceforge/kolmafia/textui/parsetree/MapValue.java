/**
 * Copyright (c) 2005-2020, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.parsetree;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;

public class MapValue
	extends AggregateValue
{
	public MapValue( final AggregateType type )
	{
		super( type );
		this.content = new TreeMap<Value,Value>();
	}

	public MapValue( final AggregateType type, boolean caseInsensitive )
	{
		super( type );
		this.content =
			caseInsensitive ?
			new TreeMap<Value,Value>( Value.ignoreCaseComparator ) :
			new TreeMap<Value,Value>();
	}

	public MapValue( final AggregateType type, Map<?, ?> value )
	{
		super( type );
		this.content = value;
	}

	@SuppressWarnings("unchecked") 
	private Map<Value,Value> getMap()
	{
		return (Map<Value,Value>) this.content;
	}

	@Override
	public Value aref( final Value key, final AshRuntime interpreter )
	{
		Map<Value,Value> map = this.getMap();
		return map.get( key );
	}

	@Override
	public void aset( final Value key, Value val, final AshRuntime interpreter )
	{
		Map<Value,Value> map = this.getMap();

		Type dataType = this.getDataType();
		Type baseType = dataType.getBaseType();
		Type valType = val.getType();

		if ( baseType.equals( valType ) )
		{
			map.put( key, val );
		}
		else if ( baseType.equals( DataTypes.TYPE_STRING ) )
		{
			map.put( key, val.toStringValue() );
		}
		else if ( baseType.equals( DataTypes.TYPE_INT ) &&
			  valType.equals( DataTypes.TYPE_FLOAT ) )
		{
			map.put( key, val.toIntValue() );
		}
		else if ( baseType.equals( DataTypes.TYPE_FLOAT ) &&
			  valType.equals( DataTypes.TYPE_INT ) )
		{
			map.put( key, val.toFloatValue() );
		}
		else
		{
			throw interpreter.runtimeException( "Internal error: Cannot assign " + valType + " to " + baseType );
		}
	}

	@Override
	public Value remove( final Value key, final AshRuntime interpreter )
	{
		// Look through all active foreach loops since they are
		// implemented via iterators and you must use that iterator's
		// remove method on the current element only.
		for ( int i = interpreter.iterators.size() - 3; i >= 0; i -= 3 )
		{
			AggregateValue slice = (AggregateValue) interpreter.iterators.get( i + 1 );
			if ( slice != this )
			{
				continue;
			}
			Value keyValue = (Value) interpreter.iterators.get( i );
			if ( !key.equals( keyValue ) )
			{
				throw interpreter.runtimeException( "Removing non-current key within foreach" );
			}

			// This is removing the current element of a foreach iterator.
			// That works.

			// Return the current value
			Value rv = this.aref( key, interpreter );

			@SuppressWarnings("unchecked") 
			Iterator<Value> it = (Iterator<Value>) interpreter.iterators.get( i + 2 );
			it.remove();

			// NULL-out the key associated with this iterator in
			// case remove is used more than once on the same key
			interpreter.iterators.set( i, null );

			return rv;
		}
		
		Map<Value,Value> map = this.getMap();
		return map.remove( key );
	}

	@Override
	public void clear()
	{
		Map<Value,Value> map = this.getMap();
		map.clear();
	}

	@Override
	public int count()
	{
		Map<Value,Value> map = this.getMap();
		return map.size();
	}

	@Override
	public boolean contains( final Value key )
	{
		Map<Value,Value> map = this.getMap();
		return map.containsKey( key );
	}

	@Override
	public Value[] keys()
	{
		Set<Value> set = this.getMap().keySet();
		Value[] keys = new Value[ set.size() ];
		set.toArray( keys );
		return keys;
	}

	@Override
	public Iterator<Value> iterator()
	{
		Set<Value> set = this.getMap().keySet();
		return set.iterator();
	}
}
