/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class RecordValue
	extends CompositeValue
{
	public RecordValue( final RecordType type )
	{
		super( type );

		Type[] DataTypes = type.getFieldTypes();
		int size = DataTypes.length;
		Value[] content = new Value[ size ];
		for ( int i = 0; i < size; ++i )
		{
			content[ i ] = DataTypes[ i ].initialValue();
		}
		this.content = content;
	}

	public RecordType getRecordType()
	{
		return (RecordType) this.type;
	}

	public Type getDataType( final Value key )
	{
		return ( (RecordType) this.type ).getDataType( key );
	}

	@Override
	public Value aref( final Value key, final Interpreter interpreter )
	{
		int index = ( (RecordType) this.type ).indexOf( key );
		if ( index < 0 )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		Value[] array = (Value[]) this.content;
		return array[ index ];
	}

	public Value aref( final int index, final Interpreter interpreter )
	{
		RecordType type = (RecordType) this.type;
		int size = type.fieldCount();
		if ( index < 0 || index >= size )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		Value[] array = (Value[]) this.content;
		return array[ index ];
	}

	@Override
	public void aset( final Value key, final Value val, final Interpreter interpreter )
	{
		int index = ( (RecordType) this.type ).indexOf( key );
		if ( index < 0 )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}

		this.aset( index, val, interpreter );
	}

	public void aset( final int index, final Value val, final Interpreter interpreter )
	{
		RecordType type = (RecordType) this.type;
		int size = type.fieldCount();
		if ( index < 0 || index >= size )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}

		Value[] array = (Value[]) this.content;

		if ( array[ index ].getType().equals( val.getType() ) )
		{
			array[ index ] = val;
		}
		else if ( array[ index ].getType().equals( DataTypes.TYPE_STRING ) )
		{
			array[ index ] = val.toStringValue();
		}
		else if ( array[ index ].getType().equals( DataTypes.TYPE_INT ) && val.getType().equals(
			DataTypes.TYPE_FLOAT ) )
		{
			array[ index ] = val.toIntValue();
		}
		else if ( array[ index ].getType().equals( DataTypes.TYPE_FLOAT ) && val.getType().equals(
			DataTypes.TYPE_INT ) )
		{
			array[ index ] = val.toFloatValue();
		}
		else
		{
			throw interpreter.runtimeException(
				"Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
		}
	}

	@Override
	public Value remove( final Value key, final Interpreter interpreter )
	{
		int index = ( (RecordType) this.type ).indexOf( key );
		if ( index < 0 )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		Value[] array = (Value[]) this.content;
		Value result = array[ index ];
		array[ index ] = this.getDataType( key ).initialValue();
		return result;
	}

	@Override
	public void clear()
	{
		Type[] DataTypes = ( (RecordType) this.type ).getFieldTypes();
		Value[] array = (Value[]) this.content;
		for ( int index = 0; index < array.length; ++index )
		{
			array[ index ] = DataTypes[ index ].initialValue();
		}
	}

	@Override
	public Value[] keys()
	{
		return ( (RecordType) this.type ).getFieldIndices();
	}

	@Override
	public void dump( final PrintStream writer, final String prefix, boolean compact )
	{
		if ( !compact || this.type.containsAggregate() )
		{
			super.dump( writer, prefix, compact );
			return;
		}

		writer.print( prefix );
		this.dumpValue( writer );
		writer.println();
	}

	@Override
	public void dumpValue( final PrintStream writer )
	{
		int size = ( (RecordType) this.type ).getFieldTypes().length;
		for ( int i = 0; i < size; ++i )
		{
			Value value = this.aref( i, null );
			if ( i > 0 )
			{
				writer.print( "\t" );
			}
			value.dumpValue( writer );
		}
	}

	@Override
	public int read( final String[] data, int index, boolean compact )
	{
		if ( !compact || this.type.containsAggregate() )
		{
			return super.read( data, index, compact );
		}

		Type[] types = ( (RecordType) this.type ).getFieldTypes();
		Value[] array = (Value[]) this.content;

		int size = Math.min( types.length, data.length - index );
		int first = index;

		// Consume remaining data values and store them
		for ( int offset = 0; offset < size; ++offset )
		{
			Type valType = types[ offset ];
			if ( valType instanceof RecordType )
			{
				RecordValue rec = (RecordValue) array[ offset ];
				index += rec.read( data, index, true );
			}
			else
			{
				array[ offset ] = DataTypes.parseValue( valType, data[ index ], true );
				index += 1;
			}
		}

		// assert index == data.length
		return index - first;
	}

	@Override
	public String toString()
	{
		return "record " + this.type.toString();
	}
}
