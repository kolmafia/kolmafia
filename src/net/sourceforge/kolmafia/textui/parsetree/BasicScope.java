/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.utilities.PauseObject;

public abstract class BasicScope
	implements ParseTreeNode
{
	private final PauseObject pauser = new PauseObject();

	protected TypeList types;
	protected VariableList variables;
	protected FunctionList functions;
	protected BasicScope parentScope;

	public BasicScope( FunctionList functions, VariableList variables, TypeList types, final BasicScope parentScope )
	{
		this.functions = ( functions == null ) ? new FunctionList() : functions;
		this.types = ( types == null ) ? new TypeList() : types;
		this.variables = ( variables == null ) ? new VariableList() : variables;
		this.parentScope = parentScope;
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
		Function[] functions = this.functions.findFunctions( name );

		int paramCount, stringCount;
		Function bestMatch = null;

		for ( int i = 0; i < functions.length; ++i )
		{
			paramCount = 0;
			stringCount = 0;

			Iterator refIterator = functions[ i ].getReferences();

			while ( refIterator.hasNext() )
			{
				++paramCount;
				if ( ( (VariableReference) refIterator.next() ).getType().equals( DataTypes.STRING_TYPE ) )
				{
					++stringCount;
				}
			}

			if ( !hasParameters && paramCount == 0 )
			{
				return functions[ i ];
			}
			if ( hasParameters && paramCount >= 1 )
			{
				if ( stringCount == 1 )
				{
					return functions[ i ];
				}
				else if ( bestMatch != null )
				{
					return null;
				}
				else
				{
					bestMatch = functions[ i ];
				}
			}
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
		this.pauser.pause(1);

		Value result = DataTypes.VOID_VALUE;
		interpreter.traceIndent();

		ParseTreeNode current;
		Iterator it = this.getCommands();

		while ( it.hasNext() )
		{
			current = (ParseTreeNode) it.next();
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

			interpreter.trace( "[" + interpreter.getState() + "] <- " + result.toQuotedString() );

			if ( interpreter.getState() != Interpreter.STATE_NORMAL )
			{
				interpreter.traceUnindent();
				return result;
			}
		}

		interpreter.traceUnindent();
		return result;
	}

	public abstract void addCommand( final ParseTreeNode c );
	public abstract Iterator getCommands();
}
