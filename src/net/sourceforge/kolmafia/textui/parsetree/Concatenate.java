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

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Iterator;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class Concatenate
	extends Expression
{
	private final ArrayList<Value> strings;

	public Concatenate( final Value lhs, final Value rhs )
	{
		this.strings = new ArrayList<Value>();
		strings.add( lhs );
		strings.add( rhs );
	}

	@Override
	public Type getType()
	{
		return DataTypes.STRING_TYPE;
	}

	public void addString( final Value string )
	{
		strings.add( string );
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Concatenate:" );
		}

		StringBuilder buffer = new StringBuilder();

		Iterator<Value> it = this.strings.iterator();
		int count = 0;

		while ( it.hasNext() )
		{
			Value arg = it.next();

			interpreter.traceIndent();
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Arg " + (++count) + ": " + arg );
			}

			Value value = arg.execute( interpreter );
			interpreter.captureValue( value );
			if ( value == null )
			{
				value = DataTypes.VOID_VALUE;
			}

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + value.toQuotedString() );
			}
			interpreter.traceUnindent();

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			String string = value.toStringValue().toString();
			buffer.append( string );
		}

		Value result = new Value( buffer.toString() );

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "<- " + result );
		}

		interpreter.traceUnindent();
		return result;
	}

	@Override
	public String toString()
	{
		StringBuilder output = new StringBuilder( "(" );
		Iterator<Value> it = this.strings.iterator();
		int count = 0;

		while ( it.hasNext() )
		{
			if ( count++ > 0 )
			{
				output.append( " + " );
			}
			Value string = it.next();
			output.append( string.toQuotedString() );

		}

		output.append( ")" );
		return output.toString();
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<CONCATENATE>" );
		Iterator<Value> it = this.strings.iterator();
		while ( it.hasNext() )
		{
			Value string = it.next();
			string.print( stream, indent + 1 );
		}
	}
}
