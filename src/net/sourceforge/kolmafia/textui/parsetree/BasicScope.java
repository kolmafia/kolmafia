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

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;

import net.sourceforge.kolmafia.utilities.PauseObject;

public abstract class BasicScope
	extends ParseTreeNode
{
	private final PauseObject pauser = new PauseObject();
	private static long nextPause = System.currentTimeMillis();
	
	protected static final int BARRIER_NONE = 0;	// no return, etc. yet
	protected static final int BARRIER_SEEN = 1;	// just seen
	protected static final int BARRIER_PAST = 2;	// already warned about dead code	

	protected TypeList types;
	protected VariableList variables;
	protected FunctionList functions;
	protected BasicScope parentScope;
	protected ArrayList nestedScopes;
	boolean executed;

	public BasicScope( FunctionList functions, VariableList variables, TypeList types, BasicScope parentScope )
	{
		this.functions = ( functions == null ) ? new FunctionList() : functions;
		this.types = ( types == null ) ? new TypeList() : types;
		this.variables = ( variables == null ) ? new VariableList() : variables;
		this.parentScope = parentScope;
		this.nestedScopes = new ArrayList();
		this.nestedScopes.add( this );
		while ( parentScope != null )
		{
			parentScope.nestedScopes.add( this );
			parentScope = parentScope.parentScope;
		}
		this.executed = false;
	}

	public BasicScope( VariableList variables, final BasicScope parentScope )
	{
		this( null, variables, null, parentScope );
	}

	public BasicScope( final BasicScope parentScope )
	{
		this( null, null, null, parentScope );
	}

	public BasicScope getParentScope()
	{
		return this.parentScope;
	}

	public Iterator getTypes()
	{
		return this.types.iterator();
	}

	public boolean addType( final Type t )
	{
		return this.types.add( t );
	}

	public Type findType( final String name )
	{
		Type current = this.types.find( name );
		if ( current != null )
		{
			return current;
		}
		if ( this.parentScope != null )
		{
			return this.parentScope.findType( name );
		}
		return null;
	}

	public Iterator getScopes()
	{
		return this.nestedScopes.iterator();
	}

	public Iterator getVariables()
	{
		return this.variables.iterator();
	}

	public boolean addVariable( final Variable v )
	{
		return this.variables.add( v );
	}

	public Variable findVariable( final String name )
	{
		return this.findVariable( name, false );
	}

	public Variable findVariable( final String name, final boolean recurse )
	{
		Variable current = this.variables.find( name );
		if ( current != null )
		{
			return current;
		}
		if ( recurse && this.parentScope != null )
		{
			return this.parentScope.findVariable( name, true );
		}
		return null;
	}

	public Iterator getFunctions()
	{
		return this.functions.iterator();
	}

	public FunctionList getFunctionList()
	{
		return this.functions;
	}

	public boolean addFunction( final Function f )
	{
		return this.functions.add( f );
	}

	public boolean removeFunction( final Function f )
	{
		return this.functions.remove( f );
	}

	public Function findFunction( final String name, final ValueList params )
	{
		Function result = this.findFunction( this.functions, name, params, true );

		if ( result == null )
		{
			result = this.findFunction( RuntimeLibrary.functions, name, params, true );
		}

		if ( result == null )
		{
			result = this.findFunction( this.functions, name, params, false );
		}

		if ( result == null )
		{
			result = this.findFunction( RuntimeLibrary.functions, name, params, false );
		}

		return result;
	}

	private Function findFunction( final FunctionList source, final String name, final ValueList params, boolean exact )
	{
		Function[] functions = source.findFunctions( name );

		for ( int i = 0; i < functions.length; ++i )
		{
			Function function = functions[ i ];
			if ( function.paramsMatch( params, exact ) )
			{
				return function;
			}
		}

		if ( !exact && this.parentScope != null )
		{
			return this.parentScope.findFunction( name, params );
		}

		return null;
	}

	private boolean isMatchingFunction( final UserDefinedFunction existing, final UserDefinedFunction f )
	{
		// The types of the new function's parameters
		// must exactly match the types of the existing
		// function's parameters

		Iterator it1 = existing.getReferences();
		Iterator it2 = f.getReferences();

		while ( it1.hasNext() && it2.hasNext() )
		{
			VariableReference p1 = (VariableReference) it1.next();
			VariableReference p2 = (VariableReference) it2.next();

			if ( !p1.getType().equals( p2.getType() ) )
			{
				return false;
			}
		}

		// There must be the same number of parameters

		if ( it1.hasNext() || it2.hasNext() )
		{
			return false;
		}

		return true;
	}

	public UserDefinedFunction findFunction( final UserDefinedFunction f )
	{
		if ( f.getName().equals( "main" ) )
		{
			return f;
		}

		Function[] options = this.functions.findFunctions( f.getName() );

		for ( int i = 0; i < options.length; ++i )
		{
			if ( options[ i ] instanceof UserDefinedFunction )
			{
				UserDefinedFunction existing = (UserDefinedFunction) options[ i ];
				if ( this.isMatchingFunction( existing, f ) )
				{
					return existing;
				}
			}
		}

		return null;
	}

	public UserDefinedFunction replaceFunction( final UserDefinedFunction existing, final UserDefinedFunction f )
	{
		if ( f.getName().equals( "main" ) )
		{
			return f;
		}

		if ( existing != null )
		{
			// Must use new definition's variables

			existing.setVariableReferences( f.getVariableReferences() );
			return existing;
		}

		this.addFunction( f );
		return f;
	}

	public Function findFunction( final String name, boolean hasParameters )
	{
		Function function = findFunction( name, this.functions, hasParameters );
		
		if ( function != null )
		{
			return function;
		}
		
		function = findFunction( name, RuntimeLibrary.functions, hasParameters );

		return function;
	}
	
	public Function findFunction( final String name, final FunctionList functionList, final boolean hasParameters )
	{
		Function[] functions = functionList.findFunctions( name );
		
		if ( functions.length == 0 )
		{
			return null;
		}

		boolean isAmbiguous = false;
		int minParamCount = Integer.MAX_VALUE;
		Function bestMatch = null;

		for ( int i = 0; i < functions.length; ++i )
		{
			int paramCount = 0;
			boolean isSingleString = false;

			Iterator refIterator = functions[ i ].getReferences();

			if ( refIterator.hasNext() )
			{
				paramCount = 1;
				VariableReference reference = (VariableReference) refIterator.next();
				
				if ( reference.getType().equals( DataTypes.STRING_TYPE ) )
				{
					isSingleString = true;
				}
			}
			
			while ( refIterator.hasNext() )
			{
				++paramCount;
				isSingleString = false;
				refIterator.next();
			}
			
			if ( paramCount == 0 )
			{
				if ( !hasParameters )
				{
					return functions[ i ];
				}
			}
			else if ( hasParameters && paramCount == 1 )
			{
				if ( isSingleString )
				{
					return functions[ i ];
				}
				
				if ( minParamCount == 1 )
				{
					isAmbiguous = true;
				}
				
				bestMatch = functions[ i ];
				minParamCount = 1;
			}
			else
			{
				if ( paramCount < minParamCount )
				{
					bestMatch = functions[ i ];
					minParamCount = paramCount;
					isAmbiguous = false;
				}
				else if ( minParamCount == paramCount )
				{
					isAmbiguous = true;
				}				
			}
		}
		
		if ( isAmbiguous )
		{
			return null;
		}

		return bestMatch;
	}

	public void print( final PrintStream stream, final int indent )
	{
		Iterator it;

		Interpreter.indentLine( stream, indent );
		stream.println( "<SCOPE>" );

		Interpreter.indentLine( stream, indent + 1 );
		stream.println( "<TYPES>" );

		it = this.getTypes();
		while ( it.hasNext() )
		{
			Type currentType = (Type) it.next();
			currentType.print( stream, indent + 2 );
		}

		Interpreter.indentLine( stream, indent + 1 );
		stream.println( "<VARIABLES>" );

		it = this.getVariables();
		while ( it.hasNext() )
		{
			Variable currentVar = (Variable) it.next();
			currentVar.print( stream, indent + 2 );
		}

		Interpreter.indentLine( stream, indent + 1 );
		stream.println( "<FUNCTIONS>" );

		it = this.getFunctions();
		while ( it.hasNext() )
		{
			Function currentFunc = (Function) it.next();
			currentFunc.print( stream, indent + 2 );
		}

		Interpreter.indentLine( stream, indent + 1 );
		stream.println( "<COMMANDS>" );

		it = this.getCommands();
		while ( it.hasNext() )
		{
			ParseTreeNode currentCommand = (ParseTreeNode) it.next();
			currentCommand.print( stream, indent + 2 );
		}
	}

	public Value execute( final Interpreter interpreter )
	{
		// Yield control at the top of the scope to
		// allow other tasks to run and keyboard input -
		// especially the Escape key - to be accepted.

		// Unfortunately, the following does not work
		// Thread.yield();

		// ...but the following does.
		long t = System.currentTimeMillis();
		if ( t >= BasicScope.nextPause )
		{
			BasicScope.nextPause = t + 100L;
			this.pauser.pause( 1 );
		}

		try
		{
			Value result = DataTypes.VOID_VALUE;
			interpreter.traceIndent();

			Iterator it = this.getCommands();
			while ( it.hasNext() )
			{
				ParseTreeNode current = (ParseTreeNode) it.next();
				result = current.execute( interpreter );

				// Abort processing now if command failed
				if ( !KoLmafia.permitsContinue() )
				{
					interpreter.setState( Interpreter.STATE_EXIT );
				}

				if ( result == null )
				{
					result = DataTypes.VOID_VALUE;
				}

				if ( interpreter.isTracing() )
				{
					interpreter.trace( "[" + interpreter.getState() + "] <- " + result.toQuotedString() );
				}

				if ( interpreter.getState() != Interpreter.STATE_NORMAL )
				{
					break;
				}
			}

			interpreter.traceUnindent();
			return result;
		}
		finally
		{
			this.executed = true;
		}
	}

	public abstract void addCommand( final ParseTreeNode c, final Parser p );

	public abstract Iterator getCommands();
}
