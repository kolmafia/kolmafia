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

import java.util.List;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;

public class ArrayLiteral
	extends AggregateLiteral
{
	private final List<Value> values;

        public ArrayLiteral( AggregateType type, final List<Value> values )
	{
		super( new AggregateType( type ) );
		this.values = values;

		type = (AggregateType)this.getType();
		int size = type.getSize();

		// If size == -1, we are creating a map.
		// Unexpected, but it will work
		if ( size < 0 )
		{
			return;
		}

		// If size == 0, we are creating an array whose size is specified
		// by the number of values. Change the size in the type.
		if ( size == 0 )
		{
			type.setSize( values.size() );
			return;
		}

		// If size > 0, we are creating an array whose size is known at
		// compile time. If the count of values is <= that size, all is
		// well.  But if not, still no problem, since we will only store
		// the correct number of values.
	}

	@Override
	public Value execute( final AshRuntime interpreter )
	{
		AggregateType type = (AggregateType)this.type;

		this.aggr = (AggregateValue)this.type.initialValue();

		int index = 0;
		int size = type.getSize();
		for ( Value val : this.values )
		{
			if ( size >= 0 && index >= size )
			{
				break;
			}

			Value key = DataTypes.makeIntValue( index++ );
			this.aggr.aset( key, val.execute( interpreter ) );
		}

		return this.aggr;
	}

	@Override
	public int count()
	{
		if ( this.aggr != null )
		{
			return this.aggr.count();
		}
		return this.values.size();
	}
}
