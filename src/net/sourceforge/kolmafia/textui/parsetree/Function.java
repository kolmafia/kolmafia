/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
import java.util.List;

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;

public abstract class Function
	extends Symbol
{
	protected Type type;
	protected List<VariableReference> variableReferences;
	private String signature;

	public Function( final String name, final Type type, final List<VariableReference> variableReferences )
	{
		super( name );
		this.type = type;
		this.variableReferences = variableReferences;
	}

	public Function( final String name, final Type type )
	{
		this( name, type, new ArrayList<VariableReference>() );
	}

	public Type getType()
	{
		return this.type;
	}

	public List<VariableReference> getVariableReferences()
	{
		return this.variableReferences;
	}

	public void setVariableReferences( final List<VariableReference> variableReferences )
	{
		this.variableReferences = variableReferences;
	}
	
	public String getSignature()
	{
		if ( this.signature == null )
		{
			StringBuffer buf = new StringBuffer();
			// Since you can't usefully have multiple overloads with the
			// same parameter types but different return types, including
			// the return type in the signature isn't very useful.
			//buf.append( this.type );
			//buf.append( " " );
			buf.append( this.name );
			buf.append( "(" );
		
			String sep = "";
			for ( VariableReference current : this.variableReferences )
			{
				buf.append( sep );
				sep = ", ";
				Type paramType = current.getType();
				buf.append( paramType );
			}
			
			buf.append( ")" );
			this.signature = buf.toString();
		}
		return this.signature;
	}

	public boolean paramsMatch( final Function that, boolean exact )
	{
		// The types of the other function's parameters must exactly
		// match the types of this function's parameters

		Iterator<VariableReference> it1 = this.variableReferences.iterator();
		Iterator<VariableReference> it2 = that.variableReferences.iterator();

		while ( it1.hasNext() && it2.hasNext() )
		{
			Type p1Type = it1.next().getType();
			Type p2Type = it2.next().getType();

			if ( p1Type.equals( p2Type ) )
			{
				continue;
			}

			if ( !exact && Parser.validCoercion( p1Type, p2Type, "parameter" ) )
			{
				continue;
			}

			return false;
		}

		// There must be the same number of parameters

		if ( it1.hasNext() || it2.hasNext() )
		{
			return false;
		}

		return true;
	}

	public boolean paramsMatch( final List<Value> params, boolean exact )
	{
		if ( params == null )
		{
			return true;
		}

		Iterator<VariableReference> refIterator = this.variableReferences.iterator();
		Iterator<Value> valIterator = params.iterator();

		while ( refIterator.hasNext() && valIterator.hasNext() )
		{
			Type paramType = refIterator.next().getType();
			Type valueType = valIterator.next().getType();

			if ( paramType == valueType )
			{
				continue;
			}

			if ( !exact && Parser.validCoercion( paramType, valueType, "parameter" ) )
			{
				continue;
			}

			return false;
		}

		if ( refIterator.hasNext() || valIterator.hasNext() )
		{
			return false;
		}

		return true;
	}

	public void printDisabledMessage( Interpreter interpreter )
	{
		try
		{
			StringBuffer message = new StringBuffer( "Called disabled function: " );
			message.append( this.getName() );

			message.append( "(" );

			String sep = "";
			for ( VariableReference current : this.variableReferences )
			{
				message.append( sep );
				sep = ",";
				message.append( ' ' );
				message.append( current.getValue( interpreter ).toStringValue().toString() );
			}

			message.append( " )" );
			RequestLogger.printLine( message.toString() );
		}
		catch ( Exception e )
		{
			// If it fails, don't print the disabled message.
			// Which means, exiting here is okay.
		}
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		// Dereference variables and pass Values to function
		Object[] values = new Object[ this.variableReferences.size() + 1];
		values[ 0 ] = interpreter;

		int index = 1;
		for ( VariableReference current : this.variableReferences )
		{
			values[ index++ ] = current.getValue( interpreter );
		}

		return this.execute( interpreter, values );
	}

	public abstract Value execute( final Interpreter interpreter, Object[] values );

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<FUNC " + this.type + " " + this.getName() + ">" );

		for ( VariableReference current : this.variableReferences )
		{
			current.print( stream, indent + 1 );
		}
	}
}
