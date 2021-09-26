/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
import java.util.List;
import java.util.Stack;

import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;

public class UserDefinedFunction
	extends Function
{
	private Scope scope;
	private final Stack<ArrayList<Value>> callStack;

	public UserDefinedFunction( final String name, final Type type, final List<VariableReference> variableReferences )
	{
		super( name, type, variableReferences );

		this.scope = null;
		this.callStack = new Stack<ArrayList<Value>>();
	}

	public void setScope( final Scope s )
	{
		this.scope = s;
	}

	public Scope getScope()
	{
		return this.scope;
	}

	private void saveBindings( AshRuntime interpreter )
	{
		if ( this.scope == null )
		{
			return;
		}

		ArrayList<Value> values = new ArrayList<Value>();

		for ( BasicScope next : this.scope.getScopes() )
		{
			for ( Variable current : next.getVariables() )
			{
				if ( !current.isStatic() )
				{
					values.add( current.getValue( interpreter ) );
				}
			}
		}

		this.callStack.push( values );
	}

	private void restoreBindings( AshRuntime interpreter )
	{
		if ( this.scope == null )
		{
			return;
		}

		ArrayList<Value> values = this.callStack.pop();
		int i = 0;

		for ( BasicScope next : this.scope.getScopes() )
		{
			for ( Variable current : next.getVariables() )
			{
				if ( !current.isStatic() )
				{
					current.forceValue( values.get( i++ ) );
				}
			}
		}
	}

	@Override
	public Value execute( final AshRuntime interpreter, Object[] values )
	{
		if ( StaticEntity.isDisabled( this.getName() ) )
		{
			this.printDisabledMessage( interpreter );
			return this.type.initialValue();
		}

		if ( this.scope == null )
		{
			throw interpreter.runtimeException( "Calling undefined user function: " + this.getName() );
		}

		// Save current variable bindings
		this.saveBindings( interpreter );

		// Bind values to variable references
		this.bindVariableReferences( interpreter, values );

		Value result = this.scope.execute( interpreter );

		// Restore initial variable bindings
		this.restoreBindings( interpreter );

		if ( result.getType().equals( this.type.getBaseType() ) )
		{
			return result;
		}

		return this.type.initialValue();
	}

	public boolean overridesLibraryFunction()
	{
		Function[] functions = RuntimeLibrary.functions.findFunctions( this.name );

		for ( Function function : functions )
		{
			if ( this.paramsMatch( function ) )
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		super.print( stream, indent );
		if ( this.scope != null )
		{
			this.scope.print( stream, indent + 1 );
		}
	}
}
