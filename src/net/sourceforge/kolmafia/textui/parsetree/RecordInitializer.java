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

import java.util.Iterator;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class RecordInitializer
	extends TypeInitializer
{
        ValueList params;

	public RecordInitializer( final RecordType type, ValueList params )
	{
		super( type );
                this.params = params;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		RecordType type = (RecordType) this.type;
		RecordValue record = (RecordValue) type.initialValue();
		Value[] content = (Value []) record.rawValue();

		Iterator iterator = this.params.iterator();
		int fieldCount = 0;

		interpreter.traceIndent();

		while ( iterator.hasNext() )
		{
			Value fieldValue = (Value) iterator.next();

			if ( fieldValue == DataTypes.VOID_VALUE )
			{
				fieldCount++;
				continue;
			}

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Field #" + (fieldCount + 1) + ": " + fieldValue.toQuotedString() );
			}

			Value value = fieldValue.execute( interpreter );
			interpreter.captureValue( value );
			if ( value == null )
			{
				value = DataTypes.VOID_VALUE;
			}

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + value.toQuotedString() );
			}

			content[ fieldCount ] = value;
			fieldCount++;
		}

		interpreter.traceUnindent();

		return record;
	}

	@Override
	public String toString()
	{
		return "<" + this.type + " initializer>";
	}
}
