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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.textui.Interpreter;

public class UserDefinedFunction
	extends Function
{
	private Scope scope;
	private final Stack callStack;

	public UserDefinedFunction( final String name, final Type type,
		final VariableReferenceList variableReferences )
	{
		super( name, type, variableReferences );

		this.scope = null;
		this.callStack = new Stack();
	}

	public void setScope( final Scope s )
	{
		this.scope = s;
	}

	public Scope getScope()
	{
		return this.scope;
	}

	public void saveBindings( Interpreter interpreter )
	{
		if ( this.scope == null )
		{
			return;
		}
		
		ArrayList values = new ArrayList();

		Iterator scopes = this.scope.getScopes();
		while ( scopes.hasNext() )
		{
			Iterator variables = ((BasicScope) scopes.next()).getVariables();
	
			while ( variables.hasNext() )
			{
				Variable current = (Variable) variables.next();
				values.add( current.getValue( interpreter ) );
			}
		}
		this.callStack.push( values );
	}

	public void restoreBindings( Interpreter interpreter )
	{
		if ( this.scope == null )
		{
			return;
		}

		ArrayList values = (ArrayList) this.callStack.pop();
		int i = 0;

		Iterator scopes = this.scope.getScopes();
		while ( scopes.hasNext() )
		{
			Iterator variables = ((BasicScope) scopes.next()).getVariables();
	
			while ( variables.hasNext() )
			{
				Variable current = (Variable) variables.next();
				current.forceValue( (Value) values.get( i++ ) );
			}
		}
	}

	public Value execute( final Interpreter interpreter )
	{
		if ( StaticEntity.isDisabled( this.getName() ) )
		{
			this.printDisabledMessage( interpreter );
			return this.getType().initialValue();
		}

		if ( this.scope == null )
		{
			throw interpreter.runtimeException( "Calling undefined user function: " + this.getName() );
		}

		Value result = this.scope.execute( interpreter );

		if ( result.getType().equals( this.type.getBaseType() ) )
		{
			return result;
		}

		return this.getType().initialValue();
	}

	public boolean assertBarrier()
	{
		return this.scope.assertBarrier();
	}

	public void print( final PrintStream stream, final int indent )
	{
		super.print( stream, indent );
		if ( this.scope != null )
		{
			this.scope.print( stream, indent + 1 );
		}
	}
}
