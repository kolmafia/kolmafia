/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia.textui;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLConstants.ByteArrayStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser.AdvancedScriptException;
import net.sourceforge.kolmafia.request.GenericRequest;

public abstract class ParseTree
{
	private static void captureValue( final Interpreter interpreter, final ScriptValue value )
	{
		// We've just executed a command in a context that captures the
		// return value.

		if ( KoLmafia.refusesContinue() || value == null )
		{
			// User aborted
			Interpreter.currentState = Interpreter.STATE_EXIT;
			return;
		}

		// Even if an error occurred, since we captured the result,
		// permit further execution.

		Interpreter.currentState = Interpreter.STATE_NORMAL;
		KoLmafia.forceContinue();
	}

	private static final void indentLine( final PrintStream stream, final int indent )
	{
		if ( stream != null )
		{
			for ( int i = 0; i < indent; ++i )
			{
				stream.print( "	  " );
			}
		}
	}

	private static void printIndices( final ScriptExpressionList indices, final PrintStream stream, final int indent )
	{
		if ( indices == null )
		{
			return;
		}

		Iterator it = indices.iterator();
		while ( it.hasNext() )
		{
			ScriptExpression current = (ScriptExpression) it.next();
			ParseTree.indentLine( stream, indent );
			stream.println( "<KEY>" );
			current.print( stream, indent + 1 );
		}
	}

	public static class ScriptScope
	{
		ScriptFunctionList functions;
		ScriptVariableList variables;
		ScriptTypeList types;
		ScriptCommandList commands;
		ScriptScope parentScope;

		public ScriptScope( final ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( final ScriptCommand command, final ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.commands.addElement( command );
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptVariableList variables, final ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			if ( variables == null )
			{
				variables = new ScriptVariableList();
			}
			this.variables = variables;
			this.types = new ScriptTypeList();
			this.commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptFunctionList functions, ScriptVariableList variables, ScriptTypeList types )
		{
			if ( functions == null )
			{
				functions = new ScriptFunctionList();
			}
			this.functions = functions;
			if ( variables == null )
			{
				variables = new ScriptVariableList();
			}
			this.variables = variables;
			if ( types == null )
			{
				types = new ScriptTypeList();
			}
			this.types = types;
			this.commands = new ScriptCommandList();
			this.parentScope = null;
		}

		public ScriptScope getParentScope()
		{
			return this.parentScope;
		}

		public boolean addFunction( final ScriptFunction f )
		{
			return this.functions.addElement( f );
		}

		public boolean removeFunction( final ScriptFunction f )
		{
			return this.functions.removeElement( f );
		}

		public Iterator getFunctions()
		{
			return this.functions.iterator();
		}

		public boolean addVariable( final ScriptVariable v )
		{
			return this.variables.addElement( v );
		}

		public Iterator getVariables()
		{
			return this.variables.iterator();
		}

		public ScriptVariable findVariable( final String name )
		{
			return this.findVariable( name, false );
		}

		public ScriptVariable findVariable( final String name, final boolean recurse )
		{
			ScriptVariable current = this.variables.findVariable( name );
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

		public boolean addType( final ScriptType t )
		{
			return this.types.addElement( t );
		}

		public Iterator getTypes()
		{
			return this.types.iterator();
		}

		public ScriptType findType( final String name )
		{
			ScriptType current = this.types.findType( name );
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

		public void addCommand( final ScriptCommand c )
		{
			this.commands.addElement( c );
		}

		public boolean assertReturn()
		{
			int size = this.commands.size();
			if ( size == 0 )
			{
				return false;
			}
			if ( this.commands.get( size - 1 ) instanceof ScriptReturn )
			{
				return true;
			}
			return false;
		}

		private boolean isMatchingFunction( final ScriptUserDefinedFunction current, final ScriptUserDefinedFunction f )
		{
			// The existing function must be a forward
			// reference.  Thus, already-defined functions
			// need to be skipped.

			boolean avoidExactMatch = current.getScope() != null;

			// The types of the new function's parameters
			// must exactly match the types of the existing
			// function's parameters

			Iterator it1 = current.getReferences();
			Iterator it2 = f.getReferences();
			ScriptVariableReference p1, p2;

			int paramCount = 1;

			while ( it1.hasNext() && it2.hasNext() )
			{
				p1 = (ScriptVariableReference) it1.next();
				p2 = (ScriptVariableReference) it2.next();

				if ( !p1.getType().equals( p2.getType() ) )
				{
					return false;
				}

				++paramCount;
			}

			// There must be the same number of parameters

			if ( it1.hasNext() )
			{
				return false;
			}

			if ( it2.hasNext() )
			{
				return false;
			}

			// Unfortunately, if it's an exact match and you're
			// avoiding exact matches, you need to throw an
			// exception.

			if ( avoidExactMatch )
			{
				throw new AdvancedScriptException( "Function '" + f.getName() + "' defined multiple times" );
			}

			return true;
		}

		public ScriptUserDefinedFunction replaceFunction( final ScriptUserDefinedFunction f )
		{
			if ( f.getName().equals( "main" ) )
			{
				return f;
			}

			ScriptFunction[] options = this.functions.findFunctions( f.getName() );

			for ( int i = 0; i < options.length; ++i )
			{
				if ( options[ i ] instanceof ScriptUserDefinedFunction )
				{
					ScriptUserDefinedFunction existing = (ScriptUserDefinedFunction) options[ i ];
					if ( !this.isMatchingFunction( existing, f ) )
					{
						continue;
					}

					// Must use new definition's variables
					existing.setVariableReferences( f.getVariableReferences() );
					return existing;
				}
			}

			this.addFunction( f );
			return f;
		}

		public ScriptFunction findFunction( final String name, boolean hasParameters )
		{
			ScriptFunction[] functions = this.functions.findFunctions( name );

			int paramCount, stringCount;
			ScriptFunction bestMatch = null;

			for ( int i = 0; i < functions.length; ++i )
			{
				paramCount = 0;
				stringCount = 0;

				Iterator refIterator = functions[ i ].getReferences();

				while ( refIterator.hasNext() )
				{
					++paramCount;
					if ( ( (ScriptVariableReference) refIterator.next() ).getType().equals( DataTypes.STRING_TYPE ) )
					{
						++stringCount;
					}
				}

				if ( !hasParameters && paramCount == 0 )
				{
					return functions[ i ];
				}
				if ( hasParameters && paramCount == 1 )
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

		private ScriptFunction findFunction( final ScriptFunctionList source, final String name,
			final ScriptExpressionList params, boolean isExactMatch )
		{
			String errorMessage = null;

			ScriptFunction[] functions = source.findFunctions( name );

			// First, try to find an exact match on parameter types.
			// This allows strict matches to take precedence.

			for ( int i = 0; i < functions.length; ++i )
			{
				errorMessage = null;

				if ( params == null )
				{
					return functions[ i ];
				}

				Iterator refIterator = functions[ i ].getReferences();
				Iterator valIterator = params.getExpressions();

				ScriptVariableReference currentParam;
				ScriptExpression currentValue;
				int paramIndex = 1;

				while ( errorMessage == null && refIterator.hasNext() && valIterator.hasNext() )
				{
					currentParam = (ScriptVariableReference) refIterator.next();
					currentValue = (ScriptExpression) valIterator.next();

					if ( isExactMatch )
					{
						if ( currentParam.getType() != currentValue.getType() )
						{
							errorMessage =
								"Illegal parameter #" + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType();
						}
					}
					else if ( !Parser.validCoercion( currentParam.getType(), currentValue.getType(), "parameter" ) )
					{
						errorMessage =
							"Illegal parameter #" + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType();
					}

					++paramIndex;
				}

				if ( errorMessage == null && ( refIterator.hasNext() || valIterator.hasNext() ) )
				{
					errorMessage =
						"Illegal amount of parameters for function " + name + ", got " + params.size() + ", expected " + functions[ i ].getVariableReferences().size();
				}

				if ( errorMessage == null )
				{
					return functions[ i ];
				}
			}

			if ( !isExactMatch && this.parentScope != null )
			{
				return this.parentScope.findFunction( name, params );
			}

			if ( !isExactMatch && source == RuntimeLibrary.functions && errorMessage != null )
			{
				throw new AdvancedScriptException( errorMessage );
			}

			return null;
		}

		public ScriptFunction findFunction( final String name, final ScriptExpressionList params )
		{
			ScriptFunction result = this.findFunction( this.functions, name, params, true );

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

			// Just in case there's some people who don't want to edit
			// their scripts to use the new function format, check for
			// the old versions as well.

			if ( result == null )
			{
				if ( name.endsWith( "to_string" ) )
				{
					return this.findFunction( "to_string", params );
				}
				if ( name.endsWith( "to_boolean" ) )
				{
					return this.findFunction( "to_boolean", params );
				}
				if ( name.endsWith( "to_int" ) )
				{
					return this.findFunction( "to_int", params );
				}
				if ( name.endsWith( "to_float" ) )
				{
					return this.findFunction( "to_float", params );
				}
				if ( name.endsWith( "to_item" ) )
				{
					return this.findFunction( "to_item", params );
				}
				if ( name.endsWith( "to_class" ) )
				{
					return this.findFunction( "to_class", params );
				}
				if ( name.endsWith( "to_stat" ) )
				{
					return this.findFunction( "to_stat", params );
				}
				if ( name.endsWith( "to_skill" ) )
				{
					return this.findFunction( "to_skill", params );
				}
				if ( name.endsWith( "to_effect" ) )
				{
					return this.findFunction( "to_effect", params );
				}
				if ( name.endsWith( "to_location" ) )
				{
					return this.findFunction( "to_location", params );
				}
				if ( name.endsWith( "to_familiar" ) )
				{
					return this.findFunction( "to_familiar", params );
				}
				if ( name.endsWith( "to_monster" ) )
				{
					return this.findFunction( "to_monster", params );
				}
				if ( name.endsWith( "to_slot" ) )
				{
					return this.findFunction( "to_slot", params );
				}
				if ( name.endsWith( "to_url" ) )
				{
					return this.findFunction( "to_url", params );
				}
			}

			return result;
		}

		public Iterator getCommands()
		{
			return this.commands.iterator();
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			// Yield control at the top of the scope to
			// allow other tasks to run and keyboard input -
			// especially the Escape key - to be accepted.

			// Unfortunately, the following does not work
			// Thread.yield();

			// ...but the following does.
			GenericRequest.delay(1);

			ScriptValue result = DataTypes.VOID_VALUE;
			Interpreter.traceIndent();

			ScriptCommand current;
			Iterator it = this.commands.iterator();

			while ( it.hasNext() )
			{
				current = (ScriptCommand) it.next();
				result = current.execute( interpreter );

				// Abort processing now if command failed
				if ( !KoLmafia.permitsContinue() )
				{
					Interpreter.currentState = Interpreter.STATE_EXIT;
				}

				if ( result == null )
				{
					result = DataTypes.VOID_VALUE;
				}

				Interpreter.trace( "[" + Interpreter.currentState + "] <- " + result.toQuotedString() );

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
				{
					Interpreter.traceUnindent();
					return result;
				}
			}

			Interpreter.traceUnindent();
			return result;
		}

		public void print( final PrintStream stream, final int indent )
		{
			Iterator it;

			ParseTree.indentLine( stream, indent );
			stream.println( "<SCOPE>" );

			ParseTree.indentLine( stream, indent + 1 );
			stream.println( "<TYPES>" );

			it = this.getTypes();
			while ( it.hasNext() )
			{
				ScriptType currentType = (ScriptType) it.next();
				currentType.print( stream, indent + 2 );
			}

			ParseTree.indentLine( stream, indent + 1 );
			stream.println( "<VARIABLES>" );

			it = this.getVariables();
			while ( it.hasNext() )
			{
				ScriptVariable currentVar = (ScriptVariable) it.next();
				currentVar.print( stream, indent + 2 );
			}

			ParseTree.indentLine( stream, indent + 1 );
			stream.println( "<FUNCTIONS>" );

			it = this.getFunctions();
			while ( it.hasNext() )
			{
				ScriptFunction currentFunc = (ScriptFunction) it.next();
				currentFunc.print( stream, indent + 2 );
			}

			ParseTree.indentLine( stream, indent + 1 );
			stream.println( "<COMMANDS>" );

			it = this.getCommands();
			while ( it.hasNext() )
			{
				ScriptCommand currentCommand = (ScriptCommand) it.next();
				currentCommand.print( stream, indent + 2 );
			}
		}
	}

	public static class ScriptSymbol
		implements Comparable
	{
		public String name;

		public ScriptSymbol()
		{
		}

		public ScriptSymbol( final String name )
		{
			this.name = name;
		}

		public String getName()
		{
			return this.name;
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof ScriptSymbol ) )
			{
				throw new ClassCastException();
			}
			if ( this.name == null )
			{
				return 1;
			}
			return this.name.compareToIgnoreCase( ( (ScriptSymbol) o ).name );
		}
	}

	public static class ScriptSymbolTable
		extends Vector
	{
		public boolean addElement( final ScriptSymbol n )
		{
			if ( this.findSymbol( n.getName() ) != null )
			{
				return false;
			}

			super.addElement( n );
			return true;
		}

		public ScriptSymbol findSymbol( final String name )
		{
			ScriptSymbol currentSymbol = null;
			for ( int i = 0; i < this.size(); ++i )
			{
				currentSymbol = (ScriptSymbol) this.get( i );
				if ( currentSymbol.getName().equalsIgnoreCase( name ) )
				{
					return currentSymbol;
				}
			}

			return null;
		}
	}

	public static abstract class ScriptFunction
		extends ScriptSymbol
	{
		public ScriptType type;
		public ScriptVariableReferenceList variableReferences;

		public ScriptFunction( final String name, final ScriptType type,
			final ScriptVariableReferenceList variableReferences )
		{
			super( name );
			this.type = type;
			this.variableReferences = variableReferences;
		}

		public ScriptFunction( final String name, final ScriptType type )
		{
			this( name, type, new ScriptVariableReferenceList() );
		}

		public ScriptType getType()
		{
			return this.type;
		}

		public ScriptVariableReferenceList getVariableReferences()
		{
			return this.variableReferences;
		}

		public void setVariableReferences( final ScriptVariableReferenceList variableReferences )
		{
			this.variableReferences = variableReferences;
		}

		public Iterator getReferences()
		{
			return this.variableReferences.iterator();
		}

		public void saveBindings( Interpreter interpreter )
		{
		}

		public void restoreBindings( Interpreter interpreter )
		{
		}

		public void printDisabledMessage( Interpreter interpreter )
		{
			try
			{
				StringBuffer message = new StringBuffer( "Called disabled function: " );
				message.append( this.getName() );

				message.append( "(" );

				Iterator it = this.variableReferences.iterator();
				ScriptVariableReference current;

				for ( int i = 0; it.hasNext(); ++i )
				{
					current = (ScriptVariableReference) it.next();

					if ( i != 0 )
					{
						message.append( ',' );
					}

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

		public abstract ScriptValue execute( final Interpreter interpreter );

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<FUNC " + this.type + " " + this.getName() + ">" );

			Iterator it = this.getReferences();
			while ( it.hasNext() )
			{
				ScriptVariableReference current = (ScriptVariableReference) it.next();
				current.print( stream, indent + 1 );
			}
		}
	}

	public static class ScriptUserDefinedFunction
		extends ScriptFunction
	{
		private ScriptScope scope;
		private final Stack callStack;

		public ScriptUserDefinedFunction( final String name, final ScriptType type,
			final ScriptVariableReferenceList variableReferences )
		{
			super( name, type, variableReferences );

			this.scope = null;
			this.callStack = new Stack();
		}

		public void setScope( final ScriptScope s )
		{
			this.scope = s;
		}

		public ScriptScope getScope()
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
			for ( int i = 0; i < this.scope.variables.size(); ++i )
			{
				values.add( ( (ScriptVariable) this.scope.variables.get( i ) ).getValue( interpreter ) );
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
			for ( int i = 0; i < this.scope.variables.size(); ++i )
			{
				if ( !( ( (ScriptVariable) this.scope.variables.get( i ) ).getType() instanceof ScriptAggregateType ) )
				{
					( (ScriptVariable) this.scope.variables.get( i ) ).forceValue( (ScriptValue) values.get( i ) );
				}
			}
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( StaticEntity.isDisabled( this.getName() ) )
			{
				this.printDisabledMessage( interpreter );
				return this.getType().initialValue();
			}

			if ( this.scope == null )
			{
				throw new RuntimeException( "Calling undefined user function: " + this.getName() );
			}

			ScriptValue result = this.scope.execute( interpreter );

			if ( result.getType().equals( this.type ) )
			{
				return result;
			}

			return this.getType().initialValue();
		}

		public boolean assertReturn()
		{
			return this.scope.assertReturn();
		}

		public void print( final PrintStream stream, final int indent )
		{
			super.print( stream, indent );
			this.scope.print( stream, indent + 1 );
		}
	}

	public static class ScriptExistingFunction
		extends ScriptFunction
	{
		private Method method;
		private final String description;
		private final Object[] variables;

		public ScriptExistingFunction( final String name, final ScriptType type, final ScriptType[] params )
		{
			this( name, type, params, null );
		}

		public ScriptExistingFunction( final String name, final ScriptType type, final ScriptType[] params,
			final String description )
		{
			super( name.toLowerCase(), type );
			this.description = description;

			this.variables = new Object[ params.length + 1 ];
			Class[] args = new Class[ params.length + 1 ];

			args[0] = Interpreter.class;
			for ( int i = 0; i < params.length; ++i )
			{
				ScriptVariable variable = new ScriptVariable( params[ i ] );
				this.variableReferences.addElement( new ScriptVariableReference( variable ) );
				this.variables[ i + 1 ] = variable;
				args[ i + 1 ] = ScriptVariable.class;
			}

			try
			{
				this.method = RuntimeLibrary.findMethod( name, args );
			}
			catch ( Exception e )
			{
				// This should not happen; it denotes a coding
				// error that must be fixed before release.

				StaticEntity.printStackTrace( e, "No method found for built-in function: " + name );
			}
		}

		public String getDescription()
		{
			return this.description;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( StaticEntity.isDisabled( this.getName() ) )
			{
				this.printDisabledMessage( interpreter );
				return this.getType().initialValue();
			}

			if ( this.method == null )
			{
				throw new RuntimeException( "Internal error: no method for " + this.getName() );
			}

			try
			{
				// Invoke the method
				this.variables[0] = interpreter;
				return (ScriptValue) this.method.invoke( this, this.variables );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				throw new AdvancedScriptException( e.getCause() == null ? e : e.getCause() );
			}
		}
	}

	public static class ScriptFunctionList
		extends ScriptSymbolTable
	{
		public boolean addElement( final ScriptFunction n )
		{
			super.add( n );
			return true;
		}

		public ScriptFunction[] findFunctions( final String name )
		{
			ArrayList matches = new ArrayList();

			for ( int i = 0; i < this.size(); ++i )
			{
				if ( ( (ScriptFunction) this.get( i ) ).getName().equalsIgnoreCase( name ) )
				{
					matches.add( this.get( i ) );
				}
			}

			ScriptFunction[] matchArray = new ScriptFunction[ matches.size() ];
			matches.toArray( matchArray );
			return matchArray;
		}
	}

	public static class ScriptVariable
		extends ScriptSymbol
	{
		ScriptType type;
		ScriptValue content;
		ScriptExpression expression = null;

		public ScriptVariable( final ScriptType type )
		{
			super( null );
			this.type = type;
			this.content = new ScriptValue( type );
		}

		public ScriptVariable( final String name, final ScriptType type )
		{
			super( name );
			this.type = type;
			this.content = new ScriptValue( type );
		}

		public ScriptType getType()
		{
			return this.type;
		}

		public ScriptValue getValue( final Interpreter interpreter )
		{
			if ( this.expression != null )
			{
				this.content = this.expression.execute( interpreter );
			}

			return this.content;
		}

		public ScriptType getValueType( final Interpreter interpreter )
		{
			return this.getValue( interpreter ).getType();
		}

		public Object rawValue( final Interpreter interpreter )
		{
			return this.getValue( interpreter ).rawValue();
		}

		public int intValue( final Interpreter interpreter )
		{
			return this.getValue( interpreter ).intValue();
		}

		public ScriptValue toStringValue( final Interpreter interpreter )
		{
			return this.getValue( interpreter ).toStringValue();
		}

		public float floatValue( final Interpreter interpreter )
		{
			return this.getValue( interpreter ).floatValue();
		}

		public void setExpression( final ScriptExpression targetExpression )
		{
			this.expression = targetExpression;
		}

		public void forceValue( final ScriptValue targetValue )
		{
			this.content = targetValue;
			this.expression = null;
		}

		public void setValue( Interpreter interpreter, final ScriptValue targetValue )
		{
			if ( this.getType().equals( targetValue.getType() ) )
			{
				this.content = targetValue;
				this.expression = null;
			}
			else if ( this.getType().equals( DataTypes.TYPE_STRING ) )
			{
				this.content = targetValue.toStringValue();
				this.expression = null;
			}
			else if ( this.getType().equals( DataTypes.TYPE_INT ) && targetValue.getType().equals(
				DataTypes.TYPE_FLOAT ) )
			{
				this.content = targetValue.toIntValue();
				this.expression = null;
			}
			else if ( this.getType().equals( DataTypes.TYPE_FLOAT ) && targetValue.getType().equals(
				DataTypes.TYPE_INT ) )
			{
				this.content = targetValue.toFloatValue();
				this.expression = null;
			}
			else if ( this.getType().equals( DataTypes.TYPE_ANY ) )
			{
				this.content = targetValue;
				this.expression = null;
			}
			else if ( this.getType().getBaseType().equals( DataTypes.TYPE_AGGREGATE ) && targetValue.getType().getBaseType().equals(
				DataTypes.TYPE_AGGREGATE ) )
			{
				this.content = targetValue;
				this.expression = null;
			}
			else
			{
				throw new RuntimeException(
					"Internal error: Cannot assign " + targetValue.getType() + " to " + this.getType() );
			}
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<VAR " + this.getType() + " " + this.getName() + ">" );
		}
	}

	public static class ScriptVariableList
		extends ScriptSymbolTable
	{
		public boolean addElement( final ScriptVariable n )
		{
			return super.addElement( n );
		}

		public ScriptVariable findVariable( final String name )
		{
			return (ScriptVariable) super.findSymbol( name );
		}

		public Iterator getVariables()
		{
			return this.iterator();
		}
	}

	public static class ScriptVariableReference
		extends ScriptValue
	{
		public ScriptVariable target;

		public ScriptVariableReference( final ScriptVariable target )
		{
			this.target = target;
		}

		public ScriptVariableReference( final String varName, final ScriptScope scope )
		{
			this.target = scope.findVariable( varName, true );
		}

		public boolean valid()
		{
			return this.target != null;
		}

		public ScriptType getType()
		{
			return this.target.getType();
		}

		public String getName()
		{
			return this.target.getName();
		}

		public ScriptExpressionList getIndices()
		{
			return null;
		}

		public int compareTo( final Object o )
		{
			return this.target.getName().compareTo( ( (ScriptVariableReference) o ).target.getName() );
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			return this.target.getValue( interpreter );
		}

		public ScriptValue getValue( Interpreter interpreter )
		{
			return this.target.getValue( interpreter );
		}

		public void forceValue( final ScriptValue targetValue )
		{
			this.target.forceValue( targetValue );
		}

		public void setValue( Interpreter interpreter, final ScriptValue targetValue )
		{
			this.target.setValue( interpreter, targetValue );
		}

		public String toString()
		{
			return this.target.getName();
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<VARREF> " + this.getName() );
		}
	}

	public static class ScriptCompositeReference
		extends ScriptVariableReference
	{
		private final ScriptExpressionList indices;

		// Derived from indices: Final slice and index into it
		private ScriptCompositeValue slice;
		private ScriptValue index;

		public ScriptCompositeReference( final ScriptVariable target, final ScriptExpressionList indices )
		{
			super( target );
			this.indices = indices;
		}

		public ScriptType getType()
		{
			ScriptType type = this.target.getType().getBaseType();
			for ( int i = 0; i < this.indices.size(); ++i )
			{
				type = ( (ScriptCompositeType) type ).getDataType( this.indices.get( i ) ).getBaseType();
			}
			return type;
		}

		public String getName()
		{
			return this.target.getName() + "[]";
		}

		public ScriptExpressionList getIndices()
		{
			return this.indices;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
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
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return false;
			}

			this.slice = (ScriptCompositeValue) this.target.getValue( interpreter );
			this.index = null;

			Interpreter.traceIndent();
			Interpreter.trace( "AREF: " + this.slice.toString() );

			int count = this.indices.size();
			for ( int i = 0; i < count; ++i )
			{
				ScriptExpression exp = (ScriptExpression) this.indices.get( i );

				Interpreter.traceIndent();
				Interpreter.trace( "Key #" + ( i + 1 ) + ": " + exp.toQuotedString() );

				this.index = exp.execute( interpreter );
				ParseTree.captureValue( interpreter, this.index );
				if ( this.index == null )
				{
					this.index = DataTypes.VOID_VALUE;
				}

				Interpreter.trace( "[" + Interpreter.currentState + "] <- " + this.index.toQuotedString() );
				Interpreter.traceUnindent();

				if ( Interpreter.currentState == Interpreter.STATE_EXIT )
				{
					Interpreter.traceUnindent();
					return false;
				}

				// If this is the last index, stop now
				if ( i == count - 1 )
				{
					break;
				}

				ScriptCompositeValue result = (ScriptCompositeValue) this.slice.aref( this.index );

				// Create missing intermediate slices
				if ( result == null )
				{
					result = (ScriptCompositeValue) this.slice.initialValue( this.index );
					this.slice.aset( this.index, result );
				}

				this.slice = result;

				Interpreter.trace( "AREF <- " + this.slice.toString() );
			}

			Interpreter.traceUnindent();

			return true;
		}

		public ScriptValue getValue( final Interpreter interpreter )
		{
			// Iterate through indices to final slice
			if ( this.getSlice( interpreter ) )
			{
				ScriptValue result = this.slice.aref( this.index );

				if ( result == null )
				{
					result = this.slice.initialValue( this.index );
					this.slice.aset( this.index, result );
				}

				Interpreter.traceIndent();
				Interpreter.trace( "AREF <- " + result.toQuotedString() );
				Interpreter.traceUnindent();

				return result;
			}

			return null;
		}

		public void setValue( final Interpreter interpreter, final ScriptValue targetValue )
		{
			// Iterate through indices to final slice
			if ( this.getSlice( interpreter ) )
			{
				this.slice.aset( this.index, targetValue );
				Interpreter.traceIndent();
				Interpreter.trace( "ASET: " + targetValue.toQuotedString() );
				Interpreter.traceUnindent();
			}
		}

		public ScriptValue removeKey( final Interpreter interpreter )
		{
			// Iterate through indices to final slice
			if ( this.getSlice( interpreter ) )
			{
				ScriptValue result = this.slice.remove( this.index );
				if ( result == null )
				{
					result = this.slice.initialValue( this.index );
				}
				Interpreter.traceIndent();
				Interpreter.trace( "remove <- " + result.toQuotedString() );
				Interpreter.traceUnindent();
				return result;
			}
			return null;
		}

		public boolean contains( final Interpreter interpreter, final ScriptValue index )
		{
			boolean result = false;
			// Iterate through indices to final slice
			if ( this.getSlice( interpreter ) )
			{
				result = this.slice.aref( index ) != null;
			}
			Interpreter.traceIndent();
			Interpreter.trace( "contains <- " + result );
			Interpreter.traceUnindent();
			return result;
		}

		public String toString()
		{
			return this.target.getName() + "[]";
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<AGGREF " + this.getName() + ">" );
			ParseTree.printIndices( this.getIndices(), stream, indent + 1 );
		}
	}

	public static class ScriptVariableReferenceList
		extends ScriptList
	{
		public boolean addElement( final ScriptVariableReference n )
		{
			return super.addElement( n );
		}
	}

	public static abstract class ScriptCommand
	{
		public ScriptValue execute( final Interpreter interpreter )
		{
			return null;
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<COMMAND " + this + ">" );
		}
	}

	private static abstract class ScriptFlowControl
		extends ScriptCommand
	{
		String state;

		public ScriptFlowControl( final String state )
		{
			this.state = state;
		}

		public String toString()
		{
			return this.state;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );
			Interpreter.traceUnindent();
			Interpreter.currentState = this.state;
			return DataTypes.VOID_VALUE;
		}
	}

	public static class ScriptBreak
		extends ScriptFlowControl
	{
		public ScriptBreak()
		{
			super( Interpreter.STATE_BREAK );
		}
	}

	public static class ScriptContinue
		extends ScriptFlowControl
	{
		public ScriptContinue()
		{
			super( Interpreter.STATE_CONTINUE );
		}
	}

	public static class ScriptExit
		extends ScriptFlowControl
	{
		public ScriptExit()
		{
			super( Interpreter.STATE_EXIT );
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			super.execute( interpreter );
			return null;
		}
	}

	public static class ScriptCommandList
		extends ScriptList
	{
		public boolean addElement( final ScriptCommand n )
		{
			return super.addElement( n );
		}
	}

	public static class ScriptReturn
		extends ScriptCommand
	{
		private final ScriptExpression returnValue;
		private final ScriptType expectedType;

		public ScriptReturn( final ScriptExpression returnValue, final ScriptType expectedType )
		{
			this.returnValue = returnValue;

			if ( expectedType != null && returnValue != null )
			{
				if ( !Parser.validCoercion( expectedType, returnValue.getType(), "return" ) )
				{
					throw new AdvancedScriptException( "Cannot apply " + returnValue.getType() + " to " + expectedType );
				}
			}

			this.expectedType = expectedType;
		}

		public ScriptType getType()
		{
			if ( this.expectedType != null )
			{
				return this.expectedType;
			}

			if ( this.returnValue == null )
			{
				return DataTypes.VOID_TYPE;
			}

			return this.returnValue.getType();
		}

		public ScriptExpression getExpression()
		{
			return this.returnValue;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
			}

			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				return null;
			}

			Interpreter.currentState = Interpreter.STATE_RETURN;

			if ( this.returnValue == null )
			{
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( "Eval: " + this.returnValue );

			ScriptValue result = this.returnValue.execute( interpreter );
			ParseTree.captureValue( interpreter, result );

			Interpreter.trace( "Returning: " + result );
			Interpreter.traceUnindent();

			if ( result == null )
			{
				return null;
			}

			if ( Interpreter.currentState != Interpreter.STATE_EXIT )
			{
				Interpreter.currentState = Interpreter.STATE_RETURN;
			}

			if ( this.expectedType.equals( DataTypes.TYPE_STRING ) )
			{
				return result.toStringValue();
			}

			if ( this.expectedType.equals( DataTypes.TYPE_FLOAT ) )
			{
				return result.toFloatValue();
			}

			if ( this.expectedType.equals( DataTypes.TYPE_INT ) )
			{
				return result.toIntValue();
			}

			return result;
		}

		public String toString()
		{
			return "return " + this.returnValue;
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<RETURN " + this.getType() + ">" );
			if ( !this.getType().equals( DataTypes.TYPE_VOID ) )
			{
				this.returnValue.print( stream, indent + 1 );
			}
		}
	}

	public static class ScriptConditional
		extends ScriptCommand
	{
		public ScriptScope scope;
		private final ScriptExpression condition;

		public ScriptConditional( final ScriptScope scope, final ScriptExpression condition )
		{
			this.scope = scope;
			this.condition = condition;
			if ( !condition.getType().equals( DataTypes.TYPE_BOOLEAN ) )
			{
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean" );
			}
		}

		public ScriptScope getScope()
		{
			return this.scope;
		}

		public ScriptExpression getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );

			Interpreter.trace( "Test: " + this.condition );

			ScriptValue conditionResult = this.condition.execute( interpreter );
			ParseTree.captureValue( interpreter, conditionResult );

			Interpreter.trace( "[" + Interpreter.currentState + "] <- " + conditionResult );

			if ( conditionResult == null )
			{
				Interpreter.traceUnindent();
				return null;
			}

			if ( conditionResult.intValue() == 1 )
			{
				ScriptValue result = this.scope.execute( interpreter );

				Interpreter.traceUnindent();

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
				{
					return result;
				}

				return DataTypes.TRUE_VALUE;
			}

			Interpreter.traceUnindent();
			return DataTypes.FALSE_VALUE;
		}
	}

	public static class ScriptIf
		extends ScriptConditional
	{
		private final ArrayList elseLoops;

		public ScriptIf( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope, condition );
			this.elseLoops = new ArrayList();
		}

		public Iterator getElseLoops()
		{
			return this.elseLoops.iterator();
		}

		public void addElseLoop( final ScriptConditional elseLoop )
		{
			this.elseLoops.add( elseLoop );
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			ScriptValue result = super.execute( interpreter );
			if ( Interpreter.currentState != Interpreter.STATE_NORMAL || result == DataTypes.TRUE_VALUE )
			{
				return result;
			}

			// Conditional failed. Move to else clauses

			Iterator it = this.elseLoops.iterator();
			ScriptConditional elseLoop;

			while ( it.hasNext() )
			{
				elseLoop = (ScriptConditional) it.next();
				result = elseLoop.execute( interpreter );

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL || result == DataTypes.TRUE_VALUE )
				{
					return result;
				}
			}

			return DataTypes.FALSE_VALUE;
		}

		public String toString()
		{
			return "if";
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<IF>" );

			this.getCondition().print( stream, indent + 1 );
			this.getScope().print( stream, indent + 1 );

			Iterator it = this.getElseLoops();
			while ( it.hasNext() )
			{
				ScriptConditional currentElse = (ScriptConditional) it.next();
				currentElse.print( stream, indent );
			}
		}
	}

	public static class ScriptElseIf
		extends ScriptConditional
	{
		public ScriptElseIf( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope, condition );
		}

		public String toString()
		{
			return "else if";
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<ELSE IF>" );
			this.getCondition().print( stream, indent + 1 );
			this.getScope().print( stream, indent + 1 );
		}
	}

	public static class ScriptElse
		extends ScriptConditional
	{
		public ScriptElse( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope, condition );
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( "else" );
			ScriptValue result = this.scope.execute( interpreter );
			Interpreter.traceUnindent();

			if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
			{
				return result;
			}

			return DataTypes.TRUE_VALUE;
		}

		public String toString()
		{
			return "else";
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<ELSE>" );
			this.getScope().print( stream, indent + 1 );
		}
	}

	public static class ScriptLoop
		extends ScriptCommand
	{
		public ScriptScope scope;

		public ScriptLoop( final ScriptScope scope )
		{
			this.scope = scope;
		}

		public ScriptScope getScope()
		{
			return this.scope;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			ScriptValue result = this.scope.execute( interpreter );

			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
			}

			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				return null;
			}

			if ( Interpreter.currentState == Interpreter.STATE_BREAK )
			{
				// Stay in state; subclass exits loop
				return DataTypes.VOID_VALUE;
			}

			if ( Interpreter.currentState == Interpreter.STATE_CONTINUE )
			{
				// Done with this iteration
				Interpreter.currentState = Interpreter.STATE_NORMAL;
			}

			if ( Interpreter.currentState == Interpreter.STATE_RETURN )
			{
				// Stay in state; subclass exits loop
				return result;
			}

			return result;
		}
	}

	public static class ScriptForeach
		extends ScriptLoop
	{
		private final ScriptVariableReferenceList variableReferences;
		private final ScriptVariableReference aggregate;

		public ScriptForeach( final ScriptScope scope, final ScriptVariableReferenceList variableReferences,
			final ScriptVariableReference aggregate )
		{
			super( scope );
			this.variableReferences = variableReferences;
			this.aggregate = aggregate;
		}

		public ScriptVariableReferenceList getVariableReferences()
		{
			return this.variableReferences;
		}

		public Iterator getReferences()
		{
			return this.variableReferences.iterator();
		}

		public ScriptVariableReference getAggregate()
		{
			return this.aggregate;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );

			// Evaluate the aggref to get the slice
			ScriptAggregateValue slice = (ScriptAggregateValue) this.aggregate.execute( interpreter );
			ParseTree.captureValue( interpreter, slice );
			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// Iterate over the slice with bound keyvar

			Iterator it = this.getReferences();
			return this.executeSlice( interpreter, slice, it, (ScriptVariableReference) it.next() );
		}

		private ScriptValue executeSlice( final Interpreter interpreter, final ScriptAggregateValue slice, final Iterator it,
			final ScriptVariableReference variable )
		{
			// Get an array of keys for the slice
			ScriptValue[] keys = slice.keys();

			// Get the next key variable
			ScriptVariableReference nextVariable = it.hasNext() ? (ScriptVariableReference) it.next() : null;

			// While there are further keys
			for ( int i = 0; i < keys.length; ++i )
			{
				// Get current key
				ScriptValue key = keys[ i ];

				// Bind variable to key
				variable.setValue( interpreter, key );

				Interpreter.trace( "Key #" + i + ": " + key );

				// If there are more indices to bind, recurse
				ScriptValue result;
				if ( nextVariable != null )
				{
					ScriptAggregateValue nextSlice = (ScriptAggregateValue) slice.aref( key );
					Interpreter.traceIndent();
					result = this.executeSlice( interpreter, nextSlice, it, nextVariable );
				}
				else
				{
					// Otherwise, execute scope
					result = super.execute( interpreter );
				}

				if ( Interpreter.currentState == Interpreter.STATE_NORMAL )
				{
					continue;
				}

				if ( Interpreter.currentState == Interpreter.STATE_BREAK )
				{
					Interpreter.currentState = Interpreter.STATE_NORMAL;
				}

				Interpreter.traceUnindent();
				return result;
			}

			Interpreter.traceUnindent();
			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return "foreach";
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<FOREACH>" );

			Iterator it = this.getReferences();
			while ( it.hasNext() )
			{
				ScriptVariableReference current = (ScriptVariableReference) it.next();
				current.print( stream, indent + 1 );
			}

			this.getAggregate().print( stream, indent + 1 );
			this.getScope().print( stream, indent + 1 );
		}
	}

	public static class ScriptWhile
		extends ScriptLoop
	{
		private final ScriptExpression condition;

		public ScriptWhile( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope );
			this.condition = condition;
			if ( !condition.getType().equals( DataTypes.TYPE_BOOLEAN ) )
			{
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean" );
			}

		}

		public ScriptExpression getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );

			while ( true )
			{
				Interpreter.trace( "Test: " + this.condition );

				ScriptValue conditionResult = this.condition.execute( interpreter );
				ParseTree.captureValue( interpreter, conditionResult );

				Interpreter.trace( "[" + Interpreter.currentState + "] <- " + conditionResult );

				if ( conditionResult == null )
				{
					Interpreter.traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() != 1 )
				{
					break;
				}

				ScriptValue result = super.execute( interpreter );

				if ( Interpreter.currentState == Interpreter.STATE_BREAK )
				{
					Interpreter.currentState = Interpreter.STATE_NORMAL;
					Interpreter.traceUnindent();
					return DataTypes.VOID_VALUE;
				}

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
				{
					Interpreter.traceUnindent();
					return result;
				}
			}

			Interpreter.traceUnindent();
			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return "while";
		}

		public void print( final PrintStream stream, final int indent )
		{
                        ParseTree.indentLine( stream, indent );
                        stream.println( "<WHILE>" );
                        this.getCondition().print( stream, indent + 1 );
                        this.getScope().print( stream, indent + 1 );
                }

	}

	public static class ScriptRepeat
		extends ScriptLoop
	{
		private final ScriptExpression condition;

		public ScriptRepeat( final ScriptScope scope, final ScriptExpression condition )
		{
			super( scope );
			this.condition = condition;
			if ( !condition.getType().equals( DataTypes.TYPE_BOOLEAN ) )
			{
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean" );
			}

		}

		public ScriptExpression getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );

			while ( true )
			{
				ScriptValue result = super.execute( interpreter );

				if ( Interpreter.currentState == Interpreter.STATE_BREAK )
				{
					Interpreter.currentState = Interpreter.STATE_NORMAL;
					Interpreter.traceUnindent();
					return DataTypes.VOID_VALUE;
				}

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
				{
					Interpreter.traceUnindent();
					return result;
				}

				Interpreter.trace( "Test: " + this.condition );

				ScriptValue conditionResult = this.condition.execute( interpreter );
				ParseTree.captureValue( interpreter, conditionResult );

				Interpreter.trace( "[" + Interpreter.currentState + "] <- " + conditionResult );

				if ( conditionResult == null )
				{
					Interpreter.traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() == 1 )
				{
					break;
				}
			}

			Interpreter.traceUnindent();
			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return "repeat";
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<REPEAT>" );
			this.getScope().print( stream, indent + 1 );
			this.getCondition().print( stream, indent + 1 );
		}
	}

	public static class ScriptFor
		extends ScriptLoop
	{
		private final ScriptVariableReference variable;
		private final ScriptExpression initial;
		private final ScriptExpression last;
		private final ScriptExpression increment;
		private final int direction;

		public ScriptFor( final ScriptScope scope, final ScriptVariableReference variable,
			final ScriptExpression initial, final ScriptExpression last, final ScriptExpression increment,
			final int direction )
		{
			super( scope );
			this.variable = variable;
			this.initial = initial;
			this.last = last;
			this.increment = increment;
			this.direction = direction;
		}

		public ScriptVariableReference getVariable()
		{
			return this.variable;
		}

		public ScriptExpression getInitial()
		{
			return this.initial;
		}

		public ScriptExpression getLast()
		{
			return this.last;
		}

		public ScriptExpression getIncrement()
		{
			return this.increment;
		}

		public int getDirection()
		{
			return this.direction;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );

			// Get the initial value
			Interpreter.trace( "Initial: " + this.initial );

			ScriptValue initialValue = this.initial.execute( interpreter );
			ParseTree.captureValue( interpreter, initialValue );

			Interpreter.trace( "[" + Interpreter.currentState + "] <- " + initialValue );

			if ( initialValue == null )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// Get the final value
			Interpreter.trace( "Last: " + this.last );

			ScriptValue lastValue = this.last.execute( interpreter );
			ParseTree.captureValue( interpreter, lastValue );

			Interpreter.trace( "[" + Interpreter.currentState + "] <- " + lastValue );

			if ( lastValue == null )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// Get the increment
			Interpreter.trace( "Increment: " + this.increment );

			ScriptValue incrementValue = this.increment.execute( interpreter );
			ParseTree.captureValue( interpreter, incrementValue );

			Interpreter.trace( "[" + Interpreter.currentState + "] <- " + incrementValue );

			if ( incrementValue == null )
			{
				Interpreter.traceUnindent();
				return null;
			}

			int current = initialValue.intValue();
			int increment = incrementValue.intValue();
			int end = lastValue.intValue();

			boolean up = false;

			if ( this.direction > 0 )
			{
				up = true;
			}
			else if ( this.direction < 0 )
			{
				up = false;
			}
			else
			{
				up = current <= end;
			}

			if ( up && increment < 0 || !up && increment > 0 )
			{
				increment = -increment;
			}

			// Make sure the loop will eventually terminate

			if ( current != end && increment == 0 )
			{
				throw new AdvancedScriptException( "Start not equal to end and increment equals 0" );
			}

			while ( up && current <= end || !up && current >= end )
			{
				// Bind variable to current value
				this.variable.setValue( interpreter, new ScriptValue( current ) );

				// Execute the scope
				ScriptValue result = super.execute( interpreter );

				if ( Interpreter.currentState == Interpreter.STATE_BREAK )
				{
					Interpreter.currentState = Interpreter.STATE_NORMAL;
					Interpreter.traceUnindent();
					return DataTypes.VOID_VALUE;
				}

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
				{
					Interpreter.traceUnindent();
					return result;
				}

				// Calculate next value
				current += increment;
			}

			Interpreter.traceUnindent();
			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return "for";
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			int direction = this.getDirection();
			stream.println( "<FOR " + ( direction < 0 ? "downto" : direction > 0 ? "upto" : "to" ) + " >" );
			this.getVariable().print( stream, indent + 1 );
			this.getInitial().print( stream, indent + 1 );
			this.getLast().print( stream, indent + 1 );
			this.getIncrement().print( stream, indent + 1 );
			this.getScope().print( stream, indent + 1 );
		}
	}

	public static class ScriptBasicScript
		extends ScriptValue
	{
		private final ByteArrayStream data;

		public ScriptBasicScript( final ByteArrayStream data )
		{
			this.data = data;
		}

		public ScriptType getType()
		{
			return DataTypes.VOID_TYPE;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			KoLmafiaCLI script = new KoLmafiaCLI( this.data.getByteArrayInputStream() );
			script.listenForCommands();
			return DataTypes.VOID_VALUE;
		}
	}

	public static class ScriptCall
		extends ScriptValue
	{
		private final ScriptFunction target;
		private final ScriptExpressionList params;

		public ScriptCall( final String functionName, final ScriptScope scope, final ScriptExpressionList params )
		{
			this.target = this.findFunction( functionName, scope, params );
			if ( this.target == null )
			{
				throw new AdvancedScriptException( "Undefined reference '" + functionName + "'" );
			}
			this.params = params;
		}

		private ScriptFunction findFunction( final String name, final ScriptScope scope,
			final ScriptExpressionList params )
		{
			if ( scope == null )
			{
				return null;
			}

			return scope.findFunction( name, params );
		}

		public ScriptFunction getTarget()
		{
			return this.target;
		}

		public Iterator getExpressions()
		{
			return this.params.iterator();
		}

		public ScriptType getType()
		{
			return this.target.getType();
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			// Save current variable bindings
			this.target.saveBindings( interpreter );
			Interpreter.traceIndent();

			Iterator refIterator = this.target.getReferences();
			Iterator valIterator = this.params.getExpressions();

			ScriptVariableReference paramVarRef;
			ScriptExpression paramValue;

			int paramCount = 0;

			while ( refIterator.hasNext() )
			{
				paramVarRef = (ScriptVariableReference) refIterator.next();

				++paramCount;

				if ( !valIterator.hasNext() )
				{
					this.target.restoreBindings( interpreter );
					throw new RuntimeException( "Internal error: illegal arguments" );
				}

				paramValue = (ScriptExpression) valIterator.next();

				Interpreter.trace( "Param #" + paramCount + ": " + paramValue.toQuotedString() );

				ScriptValue value = paramValue.execute( interpreter );
				ParseTree.captureValue( interpreter, value );
				if ( value == null )
				{
					value = DataTypes.VOID_VALUE;
				}

				Interpreter.trace( "[" + Interpreter.currentState + "] <- " + value.toQuotedString() );

				if ( Interpreter.currentState == Interpreter.STATE_EXIT )
				{
					this.target.restoreBindings( interpreter );
					Interpreter.traceUnindent();
					return null;
				}

				// Bind parameter to new value
				if ( paramVarRef.getType().equals( DataTypes.TYPE_STRING ) )
				{
					paramVarRef.setValue( interpreter, value.toStringValue() );
				}
				else if ( paramVarRef.getType().equals( DataTypes.TYPE_INT ) && paramValue.getType().equals(
					DataTypes.TYPE_FLOAT ) )
				{
					paramVarRef.setValue( interpreter, value.toIntValue() );
				}
				else if ( paramVarRef.getType().equals( DataTypes.TYPE_FLOAT ) && paramValue.getType().equals(
					DataTypes.TYPE_INT ) )
				{
					paramVarRef.setValue( interpreter, value.toFloatValue() );
				}
				else
				{
					paramVarRef.setValue( interpreter, value );
				}
			}

			if ( valIterator.hasNext() )
			{
				this.target.restoreBindings( interpreter );
				throw new RuntimeException( "Internal error: illegal arguments" );
			}

			Interpreter.trace( "Entering function " + this.target.getName() );
			ScriptValue result = this.target.execute( interpreter );
			Interpreter.trace( "Function " + this.target.getName() + " returned: " + result );

			if ( Interpreter.currentState != Interpreter.STATE_EXIT )
			{
				Interpreter.currentState = Interpreter.STATE_NORMAL;
			}

			// Restore initial variable bindings
			this.target.restoreBindings( interpreter );
			Interpreter.traceUnindent();

			return result;
		}

		public String toString()
		{
			return this.target.getName() + "()";
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<CALL " + this.getTarget().getName() + ">" );

			Iterator it = this.getExpressions();
			while ( it.hasNext() )
			{
				ScriptExpression current = (ScriptExpression) it.next();
				current.print( stream, indent + 1 );
			}
		}
	}

	public static class ScriptAssignment
		extends ScriptCommand
	{
		private final ScriptVariableReference lhs;
		private final ScriptExpression rhs;

		public ScriptAssignment( final ScriptVariableReference lhs, final ScriptExpression rhs )
		{
			this.lhs = lhs;
			this.rhs = rhs;

			if ( rhs != null && !Parser.validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
			{
				throw new AdvancedScriptException(
					"Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() );
			}
		}

		public ScriptVariableReference getLeftHandSide()
		{
			return this.lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return this.rhs == null ? this.lhs.getType().initialValueExpression() : this.rhs;
		}

		public ScriptType getType()
		{
			return this.lhs.getType();
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			ScriptValue value;

			if ( this.rhs == null )
			{
				value = this.lhs.getType().initialValue();
			}
			else
			{
				Interpreter.traceIndent();
				Interpreter.trace( "Eval: " + this.rhs );

				value = this.rhs.execute( interpreter );
				ParseTree.captureValue( interpreter, value );

				Interpreter.trace( "Set: " + value );
				Interpreter.traceUnindent();
			}

			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				return null;
			}

			if ( this.lhs.getType().equals( DataTypes.TYPE_STRING ) )
			{
				this.lhs.setValue( interpreter, value.toStringValue() );
			}
			else if ( this.lhs.getType().equals( DataTypes.TYPE_INT ) )
			{
				this.lhs.setValue( interpreter, value.toIntValue() );
			}
			else if ( this.lhs.getType().equals( DataTypes.TYPE_FLOAT ) )
			{
				this.lhs.setValue( interpreter, value.toFloatValue() );
			}
			else
			{
				this.lhs.setValue( interpreter, value );
			}

			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return this.rhs == null ? this.lhs.getName() : this.lhs.getName() + " = " + this.rhs;
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<ASSIGN " + this.lhs.getName() + ">" );
			ScriptVariableReference lhs = this.getLeftHandSide();
			ParseTree.printIndices( lhs.getIndices(), stream, indent + 1 );
			this.getRightHandSide().print( stream, indent + 1 );
		}
	}

	public static class ScriptType
		extends ScriptSymbol
	{
		public boolean primitive;
		private final int type;

		public ScriptType( final String name, final int type )
		{
			super( name );
			this.primitive = true;
			this.type = type;
		}

		public int getType()
		{
			return this.type;
		}

		public ScriptType getBaseType()
		{
			return this;
		}

		public boolean isPrimitive()
		{
			return this.primitive;
		}

		public boolean equals( final ScriptType type )
		{
			return this.type == type.type;
		}

		public boolean equals( final int type )
		{
			return this.type == type;
		}

		public String toString()
		{
			return this.name;
		}

		public ScriptType simpleType()
		{
			return this;
		}

		public ScriptValue initialValue()
		{
			switch ( this.type )
			{
			case DataTypes.TYPE_VOID:
				return DataTypes.VOID_VALUE;
			case DataTypes.TYPE_BOOLEAN:
				return DataTypes.BOOLEAN_INIT;
			case DataTypes.TYPE_INT:
				return DataTypes.INT_INIT;
			case DataTypes.TYPE_FLOAT:
				return DataTypes.FLOAT_INIT;
			case DataTypes.TYPE_STRING:
				return DataTypes.STRING_INIT;
			case DataTypes.TYPE_BUFFER:
				return new ScriptValue( DataTypes.BUFFER_TYPE, "", new StringBuffer() );
			case DataTypes.TYPE_MATCHER:
				return new ScriptValue( DataTypes.MATCHER_TYPE, "", Pattern.compile( "" ).matcher( "" ) );

			case DataTypes.TYPE_ITEM:
				return DataTypes.ITEM_INIT;
			case DataTypes.TYPE_LOCATION:
				return DataTypes.LOCATION_INIT;
			case DataTypes.TYPE_CLASS:
				return DataTypes.CLASS_INIT;
			case DataTypes.TYPE_STAT:
				return DataTypes.STAT_INIT;
			case DataTypes.TYPE_SKILL:
				return DataTypes.SKILL_INIT;
			case DataTypes.TYPE_EFFECT:
				return DataTypes.EFFECT_INIT;
			case DataTypes.TYPE_FAMILIAR:
				return DataTypes.FAMILIAR_INIT;
			case DataTypes.TYPE_SLOT:
				return DataTypes.SLOT_INIT;
			case DataTypes.TYPE_MONSTER:
				return DataTypes.MONSTER_INIT;
			case DataTypes.TYPE_ELEMENT:
				return DataTypes.ELEMENT_INIT;
			}
			return null;
		}

		public ScriptValue parseValue( final String name )
		{
			switch ( this.type )
			{
			case DataTypes.TYPE_BOOLEAN:
				return DataTypes.parseBooleanValue( name );
			case DataTypes.TYPE_INT:
				return DataTypes.parseIntValue( name );
			case DataTypes.TYPE_FLOAT:
				return DataTypes.parseFloatValue( name );
			case DataTypes.TYPE_STRING:
				return DataTypes.parseStringValue( name );
			case DataTypes.TYPE_ITEM:
				return DataTypes.parseItemValue( name );
			case DataTypes.TYPE_LOCATION:
				return DataTypes.parseLocationValue( name );
			case DataTypes.TYPE_CLASS:
				return DataTypes.parseClassValue( name );
			case DataTypes.TYPE_STAT:
				return DataTypes.parseStatValue( name );
			case DataTypes.TYPE_SKILL:
				return DataTypes.parseSkillValue( name );
			case DataTypes.TYPE_EFFECT:
				return DataTypes.parseEffectValue( name );
			case DataTypes.TYPE_FAMILIAR:
				return DataTypes.parseFamiliarValue( name );
			case DataTypes.TYPE_SLOT:
				return DataTypes.parseSlotValue( name );
			case DataTypes.TYPE_MONSTER:
				return DataTypes.parseMonsterValue( name );
			case DataTypes.TYPE_ELEMENT:
				return DataTypes.parseElementValue( name );
			}
			return null;
		}

		public ScriptExpression initialValueExpression()
		{
			return this.initialValue();
		}

		public boolean containsAggregate()
		{
			return false;
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<TYPE " + this.name + ">" );
		}
	}

	public static class ScriptNamedType
		extends ScriptType
	{
		ScriptType base;

		public ScriptNamedType( final String name, final ScriptType base )
		{
			super( name, DataTypes.TYPE_TYPEDEF );
			this.base = base;
		}

		public ScriptType getBaseType()
		{
			return this.base.getBaseType();
		}

		public ScriptExpression initialValueExpression()
		{
			return new ScriptTypeInitializer( this.base.getBaseType() );
		}
	}

	public static class ScriptCompositeType
		extends ScriptType
	{
		public ScriptCompositeType( final String name, final int type )
		{
			super( name, type );
			this.primitive = false;
		}

		public ScriptType getIndexType()
		{
			return null;
		}

		public ScriptType getDataType()
		{
			return null;
		}

		public ScriptType getDataType( final Object key )
		{
			return null;
		}

		public ScriptValue getKey( final ScriptValue key )
		{
			return key;
		}

		public ScriptExpression initialValueExpression()
		{
			return new ScriptTypeInitializer( this );
		}
	}

	public static class ScriptAggregateType
		extends ScriptCompositeType
	{
		private final ScriptType dataType;
		private final ScriptType indexType;
		private final int size;

		// Map
		public ScriptAggregateType( final ScriptType dataType, final ScriptType indexType )
		{
			super( "aggregate", DataTypes.TYPE_AGGREGATE );
			this.dataType = dataType;
			this.indexType = indexType;
			this.size = 0;
		}

		// Array
		public ScriptAggregateType( final ScriptType dataType, final int size )
		{
			super( "aggregate", DataTypes.TYPE_AGGREGATE );
			this.primitive = false;
			this.dataType = dataType;
			this.indexType = DataTypes.INT_TYPE;
			this.size = size;
		}

		public ScriptType getDataType()
		{
			return this.dataType;
		}

		public ScriptType getDataType( final Object key )
		{
			return this.dataType;
		}

		public ScriptType getIndexType()
		{
			return this.indexType;
		}

		public int getSize()
		{
			return this.size;
		}

		public boolean equals( final ScriptType o )
		{
			return o instanceof ScriptAggregateType && this.dataType.equals( ( (ScriptAggregateType) o ).dataType ) && this.indexType.equals( ( (ScriptAggregateType) o ).indexType );
		}

		public ScriptType simpleType()
		{
			if ( this.dataType instanceof ScriptAggregateType )
			{
				return this.dataType.simpleType();
			}
			return this.dataType;
		}

		public String toString()
		{
			return this.simpleType().toString() + " [" + this.indexString() + "]";
		}

		public String indexString()
		{
			if ( this.dataType instanceof ScriptAggregateType )
			{
				String suffix = ", " + ( (ScriptAggregateType) this.dataType ).indexString();
				if ( this.size != 0 )
				{
					return this.size + suffix;
				}
				return this.indexType.toString() + suffix;
			}

			if ( this.size != 0 )
			{
				return String.valueOf( this.size );
			}
			return this.indexType.toString();
		}

		public ScriptValue initialValue()
		{
			if ( this.size != 0 )
			{
				return new ScriptArray( this );
			}
			return new ScriptMap( this );
		}

		public boolean containsAggregate()
		{
			return true;
		}
	}

	public static class ScriptRecordType
		extends ScriptCompositeType
	{
		private final String[] fieldNames;
		private final ScriptType[] fieldTypes;
		private final ScriptValue[] fieldIndices;

		public ScriptRecordType( final String name, final String[] fieldNames, final ScriptType[] fieldTypes )
		{
			super( name, DataTypes.TYPE_RECORD );
			if ( fieldNames.length != fieldTypes.length )
			{
				throw new AdvancedScriptException( "Internal error: wrong number of field types" );
			}

			this.fieldNames = fieldNames;
			this.fieldTypes = fieldTypes;

			// Build field index values.
			// These can be either integers or strings.
			//   Integers don't require a lookup
			//   Strings make debugging easier.

			this.fieldIndices = new ScriptValue[ fieldNames.length ];
			for ( int i = 0; i < fieldNames.length; ++i )
			{
				this.fieldIndices[ i ] = new ScriptValue( fieldNames[ i ] );
			}
		}

		public String[] getFieldNames()
		{
			return this.fieldNames;
		}

		public ScriptType[] getFieldTypes()
		{
			return this.fieldTypes;
		}

		public ScriptValue[] getFieldIndices()
		{
			return this.fieldIndices;
		}

		public int fieldCount()
		{
			return this.fieldTypes.length;
		}

		public ScriptType getIndexType()
		{
			return DataTypes.STRING_TYPE;
		}

		public ScriptType getDataType( final Object key )
		{
			if ( !( key instanceof ScriptValue ) )
			{
				throw new RuntimeException( "Internal error: key is not a ScriptValue" );
			}
			int index = this.indexOf( (ScriptValue) key );
			if ( index < 0 || index >= this.fieldTypes.length )
			{
				return null;
			}
			return this.fieldTypes[ index ];
		}

		public ScriptValue getFieldIndex( final String field )
		{
			String val = field.toLowerCase();
			for ( int index = 0; index < this.fieldNames.length; ++index )
			{
				if ( val.equals( this.fieldNames[ index ] ) )
				{
					return this.fieldIndices[ index ];
				}
			}
			return null;
		}

		public ScriptValue getKey( final ScriptValue key )
		{
			ScriptType type = key.getType();

			if ( type.equals( DataTypes.TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= this.fieldNames.length )
				{
					return null;
				}
				return this.fieldIndices[ index ];
			}

			if ( type.equals( DataTypes.TYPE_STRING ) )
			{
				String str = key.toString();
				for ( int index = 0; index < this.fieldNames.length; ++index )
				{
					if ( this.fieldNames[ index ].equals( str ) )
					{
						return this.fieldIndices[ index ];
					}
				}
				return null;
			}

			return null;
		}

		public int indexOf( final ScriptValue key )
		{
			ScriptType type = key.getType();

			if ( type.equals( DataTypes.TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= this.fieldNames.length )
				{
					return -1;
				}
				return index;
			}

			if ( type.equals( DataTypes.TYPE_STRING ) )
			{
				for ( int index = 0; index < this.fieldNames.length; ++index )
				{
					if ( key == this.fieldIndices[ index ] )
					{
						return index;
					}
				}
				return -1;
			}

			return -1;
		}

		public boolean equals( final ScriptType o )
		{
			return o instanceof ScriptRecordType && this.name == ( (ScriptRecordType) o ).name;
		}

		public ScriptType simpleType()
		{
			return this;
		}

		public String toString()
		{
			return this.name;
		}

		public ScriptValue initialValue()
		{
			return new ScriptRecord( this );
		}

		public boolean containsAggregate()
		{
			for ( int i = 0; i < this.fieldTypes.length; ++i )
			{
				if ( this.fieldTypes[ i ].containsAggregate() )
				{
					return true;
				}
			}
			return false;
		}
	}

	public static class ScriptTypeInitializer
		extends ScriptValue
	{
		public ScriptType type;

		public ScriptTypeInitializer( final ScriptType type )
		{
			this.type = type;
		}

		public ScriptType getType()
		{
			return this.type;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			return this.type.initialValue();
		}

		public String toString()
		{
			return "<initial value>";
		}
	}

	public static class ScriptTypeList
		extends ScriptSymbolTable
	{
		public boolean addElement( final ScriptType n )
		{
			return super.addElement( n );
		}

		public ScriptType findType( final String name )
		{
			return (ScriptType) super.findSymbol( name );
		}
	}

	public static class ScriptValue
		extends ScriptExpression
		implements Comparable
	{
		public ScriptType type;

		public int contentInt = 0;
		public float contentFloat = 0.0f;
		public String contentString = null;
		public Object content = null;

		public ScriptValue()
		{
			this.type = DataTypes.VOID_TYPE;
		}

		public ScriptValue( final int value )
		{
			this.type = DataTypes.INT_TYPE;
			this.contentInt = value;
		}

		public ScriptValue( final boolean value )
		{
			this.type = DataTypes.BOOLEAN_TYPE;
			this.contentInt = value ? 1 : 0;
		}

		public ScriptValue( final String value )
		{
			this.type = DataTypes.STRING_TYPE;
			this.contentString = value;
		}

		public ScriptValue( final float value )
		{
			this.type = DataTypes.FLOAT_TYPE;
			this.contentInt = (int) value;
			this.contentFloat = value;
		}

		public ScriptValue( final ScriptType type )
		{
			this.type = type;
		}

		public ScriptValue( final ScriptType type, final int contentInt, final String contentString )
		{
			this.type = type;
			this.contentInt = contentInt;
			this.contentString = contentString;
		}

		public ScriptValue( final ScriptType type, final String contentString, final Object content )
		{
			this.type = type;
			this.contentString = contentString;
			this.content = content;
		}

		public ScriptValue( final ScriptValue original )
		{
			this.type = original.type;
			this.contentInt = original.contentInt;
			this.contentString = original.contentString;
			this.content = original.content;
		}

		public ScriptValue toFloatValue()
		{
			if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
			{
				return this;
			}
			else
			{
				return new ScriptValue( (float) this.contentInt );
			}
		}

		public ScriptValue toIntValue()
		{
			if ( this.type.equals( DataTypes.TYPE_INT ) )
			{
				return this;
			}
			else
			{
				return new ScriptValue( (int) this.contentFloat );
			}
		}

		public ScriptType getType()
		{
			return this.type.getBaseType();
		}

		public String toString()
		{
			if ( this.content instanceof StringBuffer )
			{
				return ( (StringBuffer) this.content ).toString();
			}

			if ( this.type.equals( DataTypes.TYPE_VOID ) )
			{
				return "void";
			}

			if ( this.contentString != null )
			{
				return this.contentString;
			}

			if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
			{
				return String.valueOf( this.contentInt != 0 );
			}

			if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
			{
				return String.valueOf( this.contentFloat );
			}

			return String.valueOf( this.contentInt );
		}

		public String toQuotedString()
		{
			if ( this.contentString != null )
			{
				return "\"" + this.contentString + "\"";
			}
			return this.toString();
		}

		public ScriptValue toStringValue()
		{
			return new ScriptValue( this.toString() );
		}

		public Object rawValue()
		{
			return this.content;
		}

		public int intValue()
		{
			return this.contentInt;
		}

		public float floatValue()
		{
			return this.contentFloat;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			return this;
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof ScriptValue ) )
			{
				throw new ClassCastException();
			}

			ScriptValue it = (ScriptValue) o;

			if ( this.type == DataTypes.BOOLEAN_TYPE || this.type == DataTypes.INT_TYPE )
			{
				return this.contentInt < it.contentInt ? -1 : this.contentInt == it.contentInt ? 0 : 1;
			}

			if ( this.type == DataTypes.FLOAT_TYPE )
			{
				return this.contentFloat < it.contentFloat ? -1 : this.contentFloat == it.contentFloat ? 0 : 1;
			}

			if ( this.contentString != null )
			{
				return this.contentString.compareTo( it.contentString );
			}

			return -1;
		}

		public int count()
		{
			return 1;
		}

		public void clear()
		{
		}

		public boolean contains( final ScriptValue index )
		{
			return false;
		}

		public boolean equals( final Object o )
		{
			return o == null || !( o instanceof ScriptValue ) ? false : this.compareTo( (Comparable) o ) == 0;
		}

		public void dumpValue( final PrintStream writer )
		{
			writer.print( this.toStringValue().toString() );
		}

		public void dump( final PrintStream writer, final String prefix, final boolean compact )
		{
			writer.println( prefix + this.toStringValue().toString() );
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<VALUE " + this.getType() + " [" + this.toString() + "]>" );
		}
	}

	public static class ScriptCompositeValue
		extends ScriptValue
	{
		public ScriptCompositeValue( final ScriptCompositeType type )
		{
			super( type );
		}

		public ScriptCompositeType getCompositeType()
		{
			return (ScriptCompositeType) this.type;
		}

		public ScriptValue aref( final ScriptValue key )
		{
			return null;
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
		}

		public ScriptValue remove( final ScriptValue key )
		{
			return null;
		}

		public void clear()
		{
		}

		public ScriptValue[] keys()
		{
			return new ScriptValue[ 0 ];
		}

		public ScriptValue initialValue( final Object key )
		{
			return ( (ScriptCompositeType) this.type ).getDataType( key ).initialValue();
		}

		public void dump( final PrintStream writer, final String prefix, final boolean compact )
		{
			ScriptValue[] keys = this.keys();
			if ( keys.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < keys.length; ++i )
			{
				ScriptValue key = keys[ i ];
				ScriptValue value = this.aref( key );
				String first = prefix + key + "\t";
				value.dump( writer, first, compact );
			}
		}

		public void dumpValue( final PrintStream writer )
		{
		}

		// Returns number of fields consumed
		public int read( final String[] data, final int index, final boolean compact )
		{
			ScriptCompositeType type = (ScriptCompositeType) this.type;
			ScriptValue key = null;

			if ( index < data.length )
			{
				key = type.getKey( DataTypes.parseValue( type.getIndexType(), data[ index ] ) );
			}
			else
			{
				key = type.getKey( DataTypes.parseValue( type.getIndexType(), "none" ) );
			}

			// If there's only a key and a value, parse the value
			// and store it in the composite

			if ( !( type.getDataType( key ) instanceof ScriptCompositeType ) )
			{
				this.aset( key, DataTypes.parseValue( type.getDataType( key ), data[ index + 1 ] ) );
				return 2;
			}

			// Otherwise, recurse until we get the final slice
			ScriptCompositeValue slice = (ScriptCompositeValue) this.aref( key );

			// Create missing intermediate slice
			if ( slice == null )
			{
				slice = (ScriptCompositeValue) this.initialValue( key );
				this.aset( key, slice );
			}

			return slice.read( data, index + 1, compact ) + 1;
		}

		public String toString()
		{
			return "composite " + this.type.toString();
		}
	}

	public static class ScriptAggregateValue
		extends ScriptCompositeValue
	{
		public ScriptAggregateValue( final ScriptAggregateType type )
		{
			super( type );
		}

		public ScriptType getDataType()
		{
			return ( (ScriptAggregateType) this.type ).getDataType();
		}

		public int count()
		{
			return 0;
		}

		public boolean contains( final ScriptValue index )
		{
			return false;
		}

		public String toString()
		{
			return "aggregate " + this.type.toString();
		}
	}

	public static class ScriptArray
		extends ScriptAggregateValue
	{
		public ScriptArray( final ScriptAggregateType type )
		{
			super( type );

			int size = type.getSize();
			ScriptType dataType = type.getDataType();
			ScriptValue[] content = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				content[ i ] = dataType.initialValue();
			}
			this.content = content;
		}

		public ScriptValue aref( final ScriptValue index )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int i = index.intValue();
			if ( i < 0 || i > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}
			return array[ i ];
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}

			if ( array[ index ].getType().equals( val.getType() ) )
			{
				array[ index ] = val;
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_STRING ) )
			{
				array[ index ] = val.toStringValue();
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_INT ) && val.getType().equals(
				DataTypes.TYPE_FLOAT ) )
			{
				array[ index ] = val.toIntValue();
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_FLOAT ) && val.getType().equals(
				DataTypes.TYPE_INT ) )
			{
				array[ index ] = val.toFloatValue();
			}
			else
			{
				throw new RuntimeException(
					"Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
			}
		}

		public ScriptValue remove( final ScriptValue key )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}
			ScriptValue result = array[ index ];
			array[ index ] = this.getDataType().initialValue();
			return result;
		}

		public void clear()
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			for ( int index = 0; index < array.length; ++index )
			{
				array[ index ] = this.getDataType().initialValue();
			}
		}

		public int count()
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array.length;
		}

		public boolean contains( final ScriptValue key )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			return index >= 0 && index < array.length;
		}

		public ScriptValue[] keys()
		{
			int size = ( (ScriptValue[]) this.content ).length;
			ScriptValue[] result = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				result[ i ] = new ScriptValue( i );
			}
			return result;
		}
	}

	public static class ScriptMap
		extends ScriptAggregateValue
	{
		public ScriptMap( final ScriptAggregateType type )
		{
			super( type );
			this.content = new TreeMap();
		}

		public ScriptValue aref( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return (ScriptValue) map.get( key );
		}

		public void aset( final ScriptValue key, ScriptValue val )
		{
			TreeMap map = (TreeMap) this.content;

			if ( !this.getDataType().equals( val.getType() ) )
			{
				if ( this.getDataType().equals( DataTypes.TYPE_STRING ) )
				{
					val = val.toStringValue();
				}
				else if ( this.getDataType().equals( DataTypes.TYPE_INT ) && val.getType().equals(
					DataTypes.TYPE_FLOAT ) )
				{
					val = val.toIntValue();
				}
				else if ( this.getDataType().equals( DataTypes.TYPE_FLOAT ) && val.getType().equals(
					DataTypes.TYPE_INT ) )
				{
					val = val.toFloatValue();
				}
			}

			map.put( key, val );
		}

		public ScriptValue remove( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return (ScriptValue) map.remove( key );
		}

		public void clear()
		{
			TreeMap map = (TreeMap) this.content;
			map.clear();
		}

		public int count()
		{
			TreeMap map = (TreeMap) this.content;
			return map.size();
		}

		public boolean contains( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return map.containsKey( key );
		}

		public ScriptValue[] keys()
		{
			Set set = ( (TreeMap) this.content ).keySet();
			ScriptValue[] keys = new ScriptValue[ set.size() ];
			set.toArray( keys );
			return keys;
		}
	}

	public static class ScriptRecord
		extends ScriptCompositeValue
	{
		public ScriptRecord( final ScriptRecordType type )
		{
			super( type );

			ScriptType[] dataTypes = type.getFieldTypes();
			int size = dataTypes.length;
			ScriptValue[] content = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				content[ i ] = dataTypes[ i ].initialValue();
			}
			this.content = content;
		}

		public ScriptRecordType getRecordType()
		{
			return (ScriptRecordType) this.type;
		}

		public ScriptType getDataType( final ScriptValue key )
		{
			return ( (ScriptRecordType) this.type ).getDataType( key );
		}

		public ScriptValue aref( final ScriptValue key )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array[ index ];
		}

		public ScriptValue aref( final int index )
		{
			ScriptRecordType type = (ScriptRecordType) this.type;
			int size = type.fieldCount();
			if ( index < 0 || index >= size )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array[ index ];
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}

			this.aset( index, val );
		}

		public void aset( final int index, final ScriptValue val )
		{
			ScriptRecordType type = (ScriptRecordType) this.type;
			int size = type.fieldCount();
			if ( index < 0 || index >= size )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}

			ScriptValue[] array = (ScriptValue[]) this.content;

			if ( array[ index ].getType().equals( val.getType() ) )
			{
				array[ index ] = val;
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_STRING ) )
			{
				array[ index ] = val.toStringValue();
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_INT ) && val.getType().equals(
				DataTypes.TYPE_FLOAT ) )
			{
				array[ index ] = val.toIntValue();
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_FLOAT ) && val.getType().equals(
				DataTypes.TYPE_INT ) )
			{
				array[ index ] = val.toFloatValue();
			}
			else
			{
				throw new RuntimeException(
					"Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
			}
		}

		public ScriptValue remove( final ScriptValue key )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new RuntimeException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			ScriptValue result = array[ index ];
			array[ index ] = this.getDataType( key ).initialValue();
			return result;
		}

		public void clear()
		{
			ScriptType[] dataTypes = ( (ScriptRecordType) this.type ).getFieldTypes();
			ScriptValue[] array = (ScriptValue[]) this.content;
			for ( int index = 0; index < array.length; ++index )
			{
				array[ index ] = dataTypes[ index ].initialValue();
			}
		}

		public ScriptValue[] keys()
		{
			return ( (ScriptRecordType) this.type ).getFieldIndices();
		}

		public void dump( final PrintStream writer, final String prefix, boolean compact )
		{
			if ( !compact || this.type.containsAggregate() )
			{
				super.dump( writer, prefix, compact );
				return;
			}

			writer.print( prefix );
			this.dumpValue( writer );
			writer.println();
		}

		public void dumpValue( final PrintStream writer )
		{
			int size = ( (ScriptRecordType) this.type ).getFieldTypes().length;
			for ( int i = 0; i < size; ++i )
			{
				ScriptValue value = this.aref( i );
				if ( i > 0 )
				{
					writer.print( "\t" );
				}
				value.dumpValue( writer );
			}
		}

		public int read( final String[] data, int index, boolean compact )
		{
			if ( !compact || this.type.containsAggregate() )
			{
				return super.read( data, index, compact );
			}

			ScriptType[] dataTypes = ( (ScriptRecordType) this.type ).getFieldTypes();
			ScriptValue[] array = (ScriptValue[]) this.content;

			int size = Math.min( dataTypes.length, data.length - index );
			int first = index;

			// Consume remaining data values and store them
			for ( int offset = 0; offset < size; ++offset )
			{
				ScriptType valType = dataTypes[ offset ];
				if ( valType instanceof ScriptRecordType )
				{
					ScriptRecord rec = (ScriptRecord) array[ offset ];
					index += rec.read( data, index, true );
				}
				else
				{
					array[ offset ] = DataTypes.parseValue( valType, data[ index ] );
					index += 1;
				}
			}

			for ( int offset = size; offset < dataTypes.length; ++offset )
			{
				array[ offset ] = DataTypes.parseValue( dataTypes[ offset ], "none" );
			}

			// assert index == data.length
			return index - first;
		}

		public String toString()
		{
			return "record " + this.type.toString();
		}
	}

	public static class ScriptExpression
		extends ScriptCommand
	{
		ScriptExpression lhs;
		ScriptExpression rhs;
		ScriptOperator oper;

		public ScriptExpression()
		{
		}

		public ScriptExpression( final ScriptExpression lhs, final ScriptExpression rhs, final ScriptOperator oper )
		{
			this.lhs = lhs;
			this.rhs = rhs;
			this.oper = oper;
		}

		public ScriptType getType()
		{
			ScriptType leftType = this.lhs.getType();

			// Unary operators have no right hand side
			if ( this.rhs == null )
			{
				return leftType;
			}

			ScriptType rightType = this.rhs.getType();

			// String concatenation always yields a string
			if ( this.oper.equals( "+" ) && ( leftType.equals( DataTypes.TYPE_STRING ) || rightType.equals( DataTypes.TYPE_STRING ) ) )
			{
				return DataTypes.STRING_TYPE;
			}

			// If it's not arithmetic, it's boolean
			if ( !this.oper.isArithmetic() )
			{
				return DataTypes.BOOLEAN_TYPE;
			}

			// Coerce int to float
			if ( leftType.equals( DataTypes.TYPE_FLOAT ) )
			{
				return DataTypes.FLOAT_TYPE;
			}

			// Otherwise result is whatever is on right
			return rightType;
		}

		public ScriptExpression getLeftHandSide()
		{
			return this.lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return this.rhs;
		}

		public ScriptOperator getOperator()
		{
			return this.oper;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			return this.oper.applyTo( interpreter, this.lhs, this.rhs );
		}

		public String toString()
		{
			if ( this.rhs == null )
			{
				return this.oper.toString() + " " + this.lhs.toQuotedString();
			}
			return "( " + this.lhs.toQuotedString() + " " + this.oper.toString() + " " + this.rhs.toQuotedString() + " )";
		}

		public String toQuotedString()
		{
			return this.toString();
		}

		public void print( final PrintStream stream, final int indent )
		{
			this.getOperator().print( stream, indent );
			this.lhs.print( stream, indent + 1 );
			if ( this.rhs != null )
			{
				this.rhs.print( stream, indent + 1 );
			}
		}
	}

	public static class ScriptExpressionList
		extends ScriptList
	{
		public Iterator getExpressions()
		{
			return this.iterator();
		}
	}

	public static class ScriptOperator
	{
		String operator;

		public ScriptOperator( final String operator )
		{
			if ( operator == null )
			{
				throw new RuntimeException( "Internal error in ScriptOperator()" );
			}

			this.operator = operator;
		}

		public boolean equals( final String op )
		{
			return this.operator.equals( op );
		}

		public boolean precedes( final ScriptOperator oper )
		{
			return this.operStrength() > oper.operStrength();
		}

		private int operStrength()
		{
			if ( this.operator.equals( "!" ) || this.operator.equals( "contains" ) || this.operator.equals( "remove" ) )
			{
				return 7;
			}

			if ( this.operator.equals( "^" ) )
			{
				return 6;
			}

			if ( this.operator.equals( "*" ) || this.operator.equals( "/" ) || this.operator.equals( "%" ) )
			{
				return 5;
			}

			if ( this.operator.equals( "+" ) || this.operator.equals( "-" ) )
			{
				return 4;
			}

			if ( this.operator.equals( "<" ) || this.operator.equals( ">" ) || this.operator.equals( "<=" ) || this.operator.equals( ">=" ) )
			{
				return 3;
			}

			if ( this.operator.equals( "=" ) || this.operator.equals( "==" ) || this.operator.equals( "!=" ) )
			{
				return 2;
			}

			if ( this.operator.equals( "||" ) || this.operator.equals( "&&" ) )
			{
				return 1;
			}

			return -1;
		}

		public boolean isArithmetic()
		{
			return this.operator.equals( "+" ) || this.operator.equals( "-" ) || this.operator.equals( "*" ) || this.operator.equals( "^" ) || this.operator.equals( "/" ) || this.operator.equals( "%" );
		}

		public String toString()
		{
			return this.operator;
		}

		public ScriptValue applyTo( final Interpreter interpreter, final ScriptExpression lhs, final ScriptExpression rhs )
		{
			Interpreter.traceIndent();
			Interpreter.trace( "Operator: " + this.operator );

			// Unary operator with special evaluation of argument
			if ( this.operator.equals( "remove" ) )
			{
				ScriptCompositeReference operand = (ScriptCompositeReference) lhs;
				Interpreter.traceIndent();
				Interpreter.trace( "Operand: " + operand );
				Interpreter.traceUnindent();
				ScriptValue result = operand.removeKey( interpreter );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			Interpreter.traceIndent();
			Interpreter.trace( "Operand 1: " + lhs );

			ScriptValue leftValue = lhs.execute( interpreter );
			ParseTree.captureValue( interpreter, leftValue );
			if ( leftValue == null )
			{
				leftValue = DataTypes.VOID_VALUE;
			}
			Interpreter.trace( "[" + Interpreter.currentState + "] <- " + leftValue.toQuotedString() );
			Interpreter.traceUnindent();

			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// Unary Operators
			if ( this.operator.equals( "!" ) )
			{
				ScriptValue result = new ScriptValue( leftValue.intValue() == 0 );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "-" ) && rhs == null )
			{
				ScriptValue result = null;
				if ( lhs.getType().equals( DataTypes.TYPE_INT ) )
				{
					result = new ScriptValue( 0 - leftValue.intValue() );
				}
				else if ( lhs.getType().equals( DataTypes.TYPE_FLOAT ) )
				{
					result = new ScriptValue( 0.0f - leftValue.floatValue() );
				}
				else
				{
					throw new RuntimeException( "Unary minus can only be applied to numbers" );
				}
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			// Unknown operator
			if ( rhs == null )
			{
				throw new RuntimeException( "Internal error: missing right operand." );
			}

			// Binary operators with optional right values
			if ( this.operator.equals( "||" ) )
			{
				if ( leftValue.intValue() == 1 )
				{
					Interpreter.trace( "<- " + DataTypes.TRUE_VALUE );
					Interpreter.traceUnindent();
					return DataTypes.TRUE_VALUE;
				}
				Interpreter.traceIndent();
				Interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute( interpreter );
				ParseTree.captureValue( interpreter, rightValue );
				if ( rightValue == null )
				{
					rightValue = DataTypes.VOID_VALUE;
				}
				Interpreter.trace( "[" + Interpreter.currentState + "] <- " + rightValue.toQuotedString() );
				Interpreter.traceUnindent();
				if ( Interpreter.currentState == Interpreter.STATE_EXIT )
				{
					Interpreter.traceUnindent();
					return null;
				}
				Interpreter.trace( "<- " + rightValue );
				Interpreter.traceUnindent();
				return rightValue;
			}

			if ( this.operator.equals( "&&" ) )
			{
				if ( leftValue.intValue() == 0 )
				{
					Interpreter.traceUnindent();
					Interpreter.trace( "<- " + DataTypes.FALSE_VALUE );
					return DataTypes.FALSE_VALUE;
				}
				Interpreter.traceIndent();
				Interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute( interpreter );
				ParseTree.captureValue( interpreter, rightValue );
				if ( rightValue == null )
				{
					rightValue = DataTypes.VOID_VALUE;
				}
				Interpreter.trace( "[" + Interpreter.currentState + "] <- " + rightValue.toQuotedString() );
				Interpreter.traceUnindent();
				if ( Interpreter.currentState == Interpreter.STATE_EXIT )
				{
					Interpreter.traceUnindent();
					return null;
				}
				Interpreter.trace( "<- " + rightValue );
				Interpreter.traceUnindent();
				return rightValue;
			}

			// Ensure type compatibility of operands
			if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), this.operator ) )
			{
				throw new RuntimeException( "Internal error: left hand side and right hand side do not correspond" );
			}

			// Special binary operator: <aggref> contains <any>
			if ( this.operator.equals( "contains" ) )
			{
				Interpreter.traceIndent();
				Interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute( interpreter );
				ParseTree.captureValue( interpreter, rightValue );
				if ( rightValue == null )
				{
					rightValue = DataTypes.VOID_VALUE;
				}
				Interpreter.trace( "[" + Interpreter.currentState + "] <- " + rightValue.toQuotedString() );
				Interpreter.traceUnindent();
				if ( Interpreter.currentState == Interpreter.STATE_EXIT )
				{
					Interpreter.traceUnindent();
					return null;
				}
				ScriptValue result = new ScriptValue( leftValue.contains( rightValue ) );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			// Binary operators
			Interpreter.traceIndent();
			Interpreter.trace( "Operand 2: " + rhs );
			ScriptValue rightValue = rhs.execute( interpreter );
			ParseTree.captureValue( interpreter, rightValue );
			if ( rightValue == null )
			{
				rightValue = DataTypes.VOID_VALUE;
			}
			Interpreter.trace( "[" + Interpreter.currentState + "] <- " + rightValue.toQuotedString() );
			Interpreter.traceUnindent();
			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// String operators
			if ( this.operator.equals( "+" ) )
			{
				if ( lhs.getType().equals( DataTypes.TYPE_STRING ) || rhs.getType().equals( DataTypes.TYPE_STRING ) )
				{
					String string = leftValue.toStringValue().toString() + rightValue.toStringValue().toString();
					ScriptValue result = new ScriptValue( string );
					Interpreter.trace( "<- " + result );
					Interpreter.traceUnindent();
					return result;
				}
			}

			if ( this.operator.equals( "=" ) || this.operator.equals( "==" ) )
			{
				if ( lhs.getType().equals( DataTypes.TYPE_STRING ) || lhs.getType().equals( DataTypes.TYPE_LOCATION ) || lhs.getType().equals(
					DataTypes.TYPE_MONSTER ) )
				{
					ScriptValue result =
						new ScriptValue( leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
					Interpreter.trace( "<- " + result );
					Interpreter.traceUnindent();
					return result;
				}
			}

			if ( this.operator.equals( "!=" ) )
			{
				if ( lhs.getType().equals( DataTypes.TYPE_STRING ) || lhs.getType().equals( DataTypes.TYPE_LOCATION ) || lhs.getType().equals(
					DataTypes.TYPE_MONSTER ) )
				{
					ScriptValue result =
						new ScriptValue( !leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
					Interpreter.trace( "<- " + result );
					Interpreter.traceUnindent();
					return result;
				}
			}

			// Arithmetic operators
			boolean isInt;
			float lfloat = 0.0f, rfloat = 0.0f;
			int lint = 0, rint = 0;

			if ( lhs.getType().equals( DataTypes.TYPE_FLOAT ) || rhs.getType().equals( DataTypes.TYPE_FLOAT ) )
			{
				isInt = false;
				lfloat = leftValue.toFloatValue().floatValue();
				rfloat = rightValue.toFloatValue().floatValue();
			}
			else
			{
				isInt = true;
				lint = leftValue.intValue();
				rint = rightValue.intValue();
			}

			if ( this.operator.equals( "+" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint + rint ) : new ScriptValue( lfloat + rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "-" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint - rint ) : new ScriptValue( lfloat - rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "*" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint * rint ) : new ScriptValue( lfloat * rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "^" ) )
			{
				ScriptValue result =
					isInt ? new ScriptValue( (int) Math.pow( lint, rint ) ) : new ScriptValue( (float) Math.pow(
						lfloat, rfloat ) );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "/" ) )
			{
				ScriptValue result =
					isInt ? new ScriptValue( (float) lint / (float) rint ) : new ScriptValue( lfloat / rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "%" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint % rint ) : new ScriptValue( lfloat % rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "<" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint < rint ) : new ScriptValue( lfloat < rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( ">" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint > rint ) : new ScriptValue( lfloat > rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "<=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint <= rint ) : new ScriptValue( lfloat <= rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( ">=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint >= rint ) : new ScriptValue( lfloat >= rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "=" ) || this.operator.equals( "==" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint == rint ) : new ScriptValue( lfloat == rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "!=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint != rint ) : new ScriptValue( lfloat != rfloat );
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			// Unknown operator
			throw new RuntimeException( "Internal error: illegal operator \"" + this.operator + "\"" );
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<OPER " + this.operator + ">" );
		}
	}

	public static class ScriptList
		extends ArrayList
	{
		public boolean addElement( final Object n )
		{
			this.add( n );
			return true;
		}
	}
}
