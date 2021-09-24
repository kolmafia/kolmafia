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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptException;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class CompositeValue
	extends Value
{
	public CompositeValue( final CompositeType type )
	{
		super( type );
	}

	public CompositeType getCompositeType()
	{
		return (CompositeType) this.type;
	}

	public Value aref( final Value key )
	{
		return this.aref( key, null );
	}

	public abstract Value aref( final Value key, final AshRuntime interpreter );

	public void aset( final Value key, final Value val )
	{
		this.aset( key, val, null );
	}

	public abstract void aset( final Value key, final Value val, final AshRuntime interpreter );

	public abstract Value remove( final Value key, final AshRuntime interpreter );

	@Override
	public abstract void clear();

	public abstract Value[] keys();

	public Iterator<Value> iterator()
	{
		return Arrays.asList( this.keys() ).iterator();
	}

	public Value initialValue( final Object key )
	{
		return ( (CompositeType) this.type ).getDataType( key ).initialValue();
	}

	@Override
	public void dump( final PrintStream writer, final String prefix, final boolean compact )
	{
		Value[] keys = this.keys();
		if ( keys.length == 0 )
		{
			return;
		}

		for ( int i = 0; i < keys.length; ++i )
		{
			Value key = keys[ i ];
			Value value = this.aref( key );
			String first = prefix + key.dumpValue() + "\t";
			value.dump( writer, first, compact );
		}
	}

	@Override
	public void dumpValue( final PrintStream writer )
	{
	}

	// Returns number of fields consumed
	public int read( final String[] data, final int index, final boolean compact, final String filename, final int line )
	{
		CompositeType type = (CompositeType) this.type;
		Type indexType = type.getIndexType();
		String keyString = ( index < data.length ) ? data[index] : "none";
		Value key = type.getKey( Value.readValue( indexType, keyString, filename, line ) );
		if ( key == null )
		{
			throw new ScriptException( "Invalid key in data file: " + keyString );
		}

		Type dataType = type.getDataType( key ).getBaseType();

		// If data is a zero-length array, read all remaining fields
		// into it and set the length of the array appropriately.

		if ( dataType instanceof AggregateType )
		{
			AggregateType atype = (AggregateType)dataType;
			if ( atype.getSize() == 0 )
			{
				Type dtype = atype.getDataType();
				ArrayList<Value> values = new ArrayList<Value>();
				for ( int i = index + 1; i < data.length; i++ )
				{
					values.add( Value.readValue( dtype, data[ i ], filename, line ) );
				}
				this.aset( key, new ArrayValue( new AggregateType( atype ), values ) );
				return data.length - index;
			}
		}

		// If the data is another composite, recurse until we get the
		// final slice

		if ( dataType instanceof CompositeType )
		{
			CompositeValue slice = (CompositeValue) this.aref( key );

			// Create missing intermediate slice
			if ( slice == null )
			{
				slice = (CompositeValue) this.initialValue( key );
				this.aset( key, slice );
			}

			return slice.read( data, index + 1, compact, filename, line ) + 1;
		}
		
		// Parse the value and store it in the composite

		Value value =
			index < data.length - 1 ?
			Value.readValue( dataType, data[ index + 1 ], filename, line ) :
			dataType.initialValue();

		this.aset( key, value );
		return 2;
	}

	@Override
	public Object toJSON()
		throws JSONException
	{
		JSONObject obj = new JSONObject();

		Value[] keys = this.keys();

		for ( int i = 0; i < keys.length; ++i )
		{
			String key = keys[ i ].toString();
			Object value = this.aref( keys[ i ] ).toJSON();
			obj.put( key, value );
		}

		return obj;
	}
}
