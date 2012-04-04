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

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptException;

public class RecordType
	extends CompositeType
{
	private final String[] fieldNames;
	private final Type[] fieldTypes;
	private final Value[] fieldIndices;

	public RecordType( final String name, final String[] fieldNames, final Type[] fieldTypes )
	{
		super( name, DataTypes.TYPE_RECORD );

		this.fieldNames = fieldNames;
		this.fieldTypes = fieldTypes;

		// Build field index values.
		// These can be either integers or strings.
		//   Integers don't require a lookup
		//   Strings make debugging easier.

		this.fieldIndices = new Value[ fieldNames.length ];
		for ( int i = 0; i < fieldNames.length; ++i )
		{
			this.fieldIndices[ i ] = new Value( fieldNames[ i ] );
		}
	}

	public String[] getFieldNames()
	{
		return this.fieldNames;
	}

	public Type[] getFieldTypes()
	{
		return this.fieldTypes;
	}

	public Value[] getFieldIndices()
	{
		return this.fieldIndices;
	}

	public int fieldCount()
	{
		return this.fieldTypes.length;
	}

	public Type getIndexType()
	{
		return DataTypes.STRING_TYPE;
	}

	public Type getDataType( final Object key )
	{
		if ( !( key instanceof Value ) )
		{
			throw new ScriptException( "Internal error: key is not a Value" );
		}
		int index = this.indexOf( (Value) key );
		if ( index < 0 || index >= this.fieldTypes.length )
		{
			return null;
		}
		return this.fieldTypes[ index ];
	}

	public Value getFieldIndex( final String field )
	{
		String val = field.toLowerCase();
		for ( int index = 0; index < this.fieldNames.length; ++index )
		{
			if ( val.equals( this.fieldNames[ index ] ) )
			{
				return this.fieldIndices[ index ];
			}
		}
		return null;
	}

	public Value getKey( final Value key )
	{
		Type type = key.getType();

		if ( type.equals( DataTypes.TYPE_INT ) )
		{
			int index = key.intValue();
			if ( index < 0 || index >= this.fieldNames.length )
			{
				return null;
			}
			return this.fieldIndices[ index ];
		}

		if ( type.equals( DataTypes.TYPE_STRING ) )
		{
			String str = key.toString();
			for ( int index = 0; index < this.fieldNames.length; ++index )
			{
				if ( this.fieldNames[ index ].equals( str ) )
				{
					return this.fieldIndices[ index ];
				}
			}
			return null;
		}

		return null;
	}

	public int indexOf( final Value key )
	{
		Type type = key.getType();

		if ( type.equals( DataTypes.TYPE_INT ) )
		{
			int index = key.intValue();
			if ( index < 0 || index >= this.fieldNames.length )
			{
				return -1;
			}
			return index;
		}

		if ( type.equals( DataTypes.TYPE_STRING ) )
		{
			for ( int index = 0; index < this.fieldNames.length; ++index )
			{
				if ( key == this.fieldIndices[ index ] )
				{
					return index;
				}
			}
			return -1;
		}

		return -1;
	}

	public boolean equals( final Type o )
	{
		return o instanceof RecordType && this.name.equals( ( (RecordType) o ).name );
	}

	public Type simpleType()
	{
		return this;
	}

	public String toString()
	{
		return this.name;
	}

	public Value initialValue()
	{
		return new RecordValue( this );
	}

	public Value initialValueExpression( ValueList params )
	{
		if ( params.isEmpty() )
		{
			return new TypeInitializer( this );
		}

		return new RecordInitializer( this, params );
	}

	public boolean containsAggregate()
	{
		for ( int i = 0; i < this.fieldTypes.length; ++i )
		{
			if ( this.fieldTypes[ i ].containsAggregate() )
			{
				return true;
			}
		}
		return false;
	}
}
