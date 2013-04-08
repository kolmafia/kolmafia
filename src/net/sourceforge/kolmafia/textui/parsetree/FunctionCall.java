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

import java.util.Iterator;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.Profiler;

public class FunctionCall
	extends Value
{
	protected Function target;
	protected final ValueList params;
	protected final String fileName;
	protected final int lineNumber;

	public FunctionCall( final Function target, final ValueList params, final Parser parser )
	{
		this.target = target;
		this.params = params;
		this.fileName = parser.getShortFileName();
		this.lineNumber = parser.getLineNumber();
	}

	public Function getTarget()
	{
		return this.target;
	}

	public Iterator getValues()
	{
		return this.params.iterator();
	}

	@Override
	public Type getType()
	{
		return this.target.getType();
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		interpreter.traceIndent();

		Iterator valIterator = this.params.iterator();
		Object[] values = new Object[ params.size() + 1 ];
		values[ 0 ] = interpreter;

		int paramCount = 1;

		while ( valIterator.hasNext() )
		{
			Value paramValue = (Value) valIterator.next();

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Param #" + paramCount + ": " + paramValue.toQuotedString() );
			}

			Value value = paramValue.execute( interpreter );
			interpreter.captureValue( value );
			if ( value == null )
			{
				value = DataTypes.VOID_VALUE;
			}

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + value.toQuotedString() );
			}

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			values[ paramCount++ ] = value;
		}

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Entering function " + this.target.getName() );
		}

		interpreter.setLineAndFile( this.fileName, this.lineNumber );

		Value result;
		Profiler prev = interpreter.profiler;
		if ( prev != null )
		{
			long t0 = System.nanoTime();
			prev.net += t0 - prev.net0;
			Profiler curr = Profiler.create( this.target.getSignature() );
			curr.net0 = t0;
			interpreter.profiler = curr;

			result = this.target.execute( interpreter, values );

			long t1 = System.nanoTime();
			prev.net0 = t1;
			interpreter.profiler = prev;
			curr.total = t1 - t0;
			curr.net += t1 - curr.net0;
			curr.finish();
		}
		else
		{
			result = this.target.execute( interpreter, values );
		}

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Function " + this.target.getName() + " returned: " + result );
		}

		if ( interpreter.getState() != Interpreter.STATE_EXIT )
		{
			interpreter.setState( Interpreter.STATE_NORMAL );
		}

		interpreter.traceUnindent();

		return result;
	}

	@Override
	public String toString()
	{
		return this.target.getName() + "()";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<CALL " + this.getTarget().getName() + ">" );

		Iterator it = this.getValues();
		while ( it.hasNext() )
		{
			Value current = (Value) it.next();
			current.print( stream, indent + 1 );
		}
	}
}
