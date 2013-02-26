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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class ArrayValue
	extends AggregateValue
{
	public ArrayValue( final AggregateType type )
	{
		super( type );

		int size = type.getSize();
		Type dataType = type.getDataType();
		Value[] content = new Value[ size ];
		for ( int i = 0; i < size; ++i )
		{
			content[ i ] = dataType.initialValue();
		}
		this.content = content;
	}

	@Override
	public Value aref( final Value key, final Interpreter interpreter )
	{
		Value[] array = (Value[]) this.content;
		int index = (int) key.intValue();
		if ( index < 0 || index >= array.length )
		{
			throw interpreter.runtimeException( "Array index out of bounds" );
		}
		return array[ index ];
	}

	@Override
	public void aset( final Value key, final Value val, final Interpreter interpreter )
	{
		Value[] array = (Value[]) this.content;
		int index = (int) key.intValue();
		if ( index < 0 || index >= array.length )
		{
			throw interpreter.runtimeException( "Array index out of bounds" );
		}

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
		Value[] array = (Value[]) this.content;
		int index = (int) key.intValue();
		if ( index < 0 || index >= array.length )
		{
			throw interpreter.runtimeException( "Array index out of bounds" );
		}
		Value result = array[ index ];
		array[ index ] = this.getDataType().initialValue();
		return result;
	}

	@Override
	public void clear()
	{
		Value[] array = (Value[]) this.content;
		for ( int i = 0; i < array.length; ++i )
		{
			array[ i ] = this.getDataType().initialValue();
		}
	}

	@Override
	public int count()
	{
		Value[] array = (Value[]) this.content;
		return array.length;
	}

	@Override
	public boolean contains( final Value key )
	{
		Value[] array = (Value[]) this.content;
		int index = (int) key.intValue();
		return index >= 0 && index < array.length;
	}

	@Override
	public Value[] keys()
	{
		int size = ( (Value[]) this.content ).length;
		Value[] result = new Value[ size ];
		for ( int i = 0; i < size; ++i )
		{
			result[ i ] = new Value( i );
		}
		return result;
	}

	@Override
	public String toJSON()
	{
		StringBuffer buffer = new StringBuffer();

		buffer.append( "[ " );

		Value[] array = (Value[]) this.content;

		for ( int i = 0; i < array.length; ++i )
		{
			if ( i > 0 )
			{
				buffer.append( ", " );
			}

			buffer.append( array[ i ].toJSON() );
		}

		buffer.append( " ]" );

		return buffer.toString();
	}
}
