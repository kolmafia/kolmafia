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

public class AggregateType
	extends CompositeType
{
	private final Type dataType;
	private final Type indexType;
	private final int size;

	// Map
	public AggregateType( final Type dataType, final Type indexType )
	{
		super( "aggregate", DataTypes.TYPE_AGGREGATE );
		this.dataType = dataType;
		this.indexType = indexType;
		this.size = 0;
	}

	// Array
	public AggregateType( final Type dataType, final int size )
	{
		super( "aggregate", DataTypes.TYPE_AGGREGATE );
		this.primitive = false;
		this.dataType = dataType;
		this.indexType = DataTypes.INT_TYPE;
		this.size = size;
	}

	@Override
	public Type getDataType()
	{
		return this.dataType;
	}

	@Override
	public Type getDataType( final Object key )
	{
		return this.dataType;
	}

	@Override
	public Type getIndexType()
	{
		return this.indexType;
	}

	public int getSize()
	{
		return this.size;
	}

	@Override
	public boolean equals( final Type o )
	{
		return o instanceof AggregateType && this.dataType.equals( ( (AggregateType) o ).dataType ) && this.indexType.equals( ( (AggregateType) o ).indexType );
	}

	@Override
	public Type simpleType()
	{
		if ( this.dataType instanceof AggregateType )
		{
			return this.dataType.simpleType();
		}
		return this.dataType;
	}

	@Override
	public String toString()
	{
		return this.simpleType().toString() + " [" + this.indexString() + "]";
	}

	public String indexString()
	{
		if ( this.dataType instanceof AggregateType )
		{
			String suffix = ", " + ( (AggregateType) this.dataType ).indexString();
			if ( this.size != 0 )
			{
				return this.size + suffix;
			}
			return this.indexType.toString() + suffix;
		}

		if ( this.size != 0 )
		{
			return String.valueOf( this.size );
		}
		return this.indexType.toString();
	}

	@Override
	public Value initialValue()
	{
		if ( this.size != 0 )
		{
			return new ArrayValue( this );
		}
		return new MapValue( this );
	}

	@Override
	public boolean containsAggregate()
	{
		return true;
	}
}
