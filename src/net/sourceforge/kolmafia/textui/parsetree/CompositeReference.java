/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

public class CompositeReference
	extends VariableReference
{
	private final ValueList indices;

	// Derived from indices: Final slice and index into it
	private CompositeValue slice;
	private Value index;

	// For runtime error messages
	String fileName;
	int lineNumber;

	public CompositeReference( final Variable target, final ValueList indices, final Parser parser )
	{
		super( target );
		this.indices = indices;
		this.fileName = parser.getShortFileName();
		this.lineNumber = parser.getLineNumber();
	}

	@Override
	public Type getType()
	{
		Type type = this.target.getType().getBaseType();
		Iterator it = this.indices.iterator();

		while ( it.hasNext() )
		{
			type = ( (CompositeType) type.asProxy() ).getDataType( it.next() ).getBaseType();
		}
		return type;
	}

	@Override
	public String getName()
	{
		return this.target.getName() + "[]";
	}

	@Override
	public ValueList getIndices()
	{
		return this.indices;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		interpreter.setLineAndFile( this.fileName, this.lineNumber );
		return this.getValue( interpreter );
	}

	// Evaluate all the indices and step through the slices.
	//
	// When done, this.slice has the final slice and this.index has
	// the final evaluated index.

	private boolean getSlice( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return false;
		}

		this.slice = (CompositeValue) Value.asProxy( this.target.getValue( interpreter ) );
		this.index = null;

		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "AREF: " + this.slice.toString() );
		}

		Iterator it = this.indices.iterator();

		for ( int i = 0; it.hasNext(); ++i )
		{
			Value exp = (Value) it.next();

			interpreter.traceIndent();
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Key #" + ( i + 1 ) + ": " + exp.toQuotedString() );
			}

			this.index = exp.execute( interpreter );
			interpreter.captureValue( this.index );
			if ( this.index == null )
			{
				this.index = DataTypes.VOID_VALUE;
			}

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + this.index.toQuotedString() );
			}
			interpreter.traceUnindent();

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return false;
			}

			if ( it.hasNext() )
			{
				CompositeValue result = (CompositeValue) Value.asProxy(
					this.slice.aref( this.index, interpreter ) );

				// Create missing intermediate slices
				if ( result == null )
				{	// ...but don't actually save a proxy in the parent object
					Value temp = this.slice.initialValue( this.index );
					this.slice.aset( this.index, temp, interpreter );
					result = (CompositeValue) Value.asProxy( temp );
				}

				this.slice = result;

				if ( interpreter.isTracing() )
				{
					interpreter.trace( "AREF <- " + this.slice.toString() );
				}
			}
		}

		interpreter.traceUnindent();

		return true;
	}

	@Override
	public synchronized Value getValue( final Interpreter interpreter )
	{
		interpreter.setLineAndFile( this.fileName, this.lineNumber );
		// Iterate through indices to final slice
		if ( this.getSlice( interpreter ) )
		{
			Value result = this.slice.aref( this.index, interpreter );

			if ( result == null )
			{
				result = this.slice.initialValue( this.index );
			}

			interpreter.traceIndent();
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "AREF <- " + result.toQuotedString() );
			}
			interpreter.traceUnindent();

			return result;
		}

		return null;
	}

	@Override
	public synchronized Value setValue( Interpreter interpreter, final Value targetValue, final Operator oper )
	{
		interpreter.setLineAndFile( this.fileName, this.lineNumber );
		// Iterate through indices to final slice
		if ( this.getSlice( interpreter ) )
		{
			Value newValue = targetValue;

			interpreter.traceIndent();

			if ( oper != null )
			{
				Value currentValue = this.slice.aref( this.index, interpreter );

				if ( currentValue == null )
				{
					currentValue = this.slice.initialValue( this.index );
					this.slice.aset( this.index, currentValue, interpreter );
				}

				if ( interpreter.isTracing() )
				{
					interpreter.trace( "AREF <- " + currentValue.toQuotedString() );
				}

				newValue = oper.applyTo( interpreter, currentValue, targetValue );
			}

			this.slice.aset( this.index, newValue, interpreter );

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "ASET: " + newValue.toQuotedString() );
			}

			interpreter.traceUnindent();

			return newValue;
		}

		return null;
	}

	public synchronized Value removeKey( final Interpreter interpreter )
	{
		interpreter.setLineAndFile( this.fileName, this.lineNumber );
		// Iterate through indices to final slice
		if ( this.getSlice( interpreter ) )
		{
			Value result = this.slice.remove( this.index, interpreter );
			if ( result == null )
			{
				result = this.slice.initialValue( this.index );
			}
			interpreter.traceIndent();
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "remove <- " + result.toQuotedString() );
			}
			interpreter.traceUnindent();
			return result;
		}
		return null;
	}

	public boolean contains( final Interpreter interpreter, final Value index )
	{
		interpreter.setLineAndFile( this.fileName, this.lineNumber );
		boolean result = false;
		// Iterate through indices to final slice
		if ( this.getSlice( interpreter ) )
		{
			result = this.slice.aref( index, interpreter ) != null;
		}
		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "contains <- " + result );
		}
		interpreter.traceUnindent();
		return result;
	}

	@Override
	public String toString()
	{
		return this.target.getName() + "[]";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<AGGREF " + this.getName() + ">" );
		Parser.printIndices( this.getIndices(), stream, indent + 1 );
	}
}
