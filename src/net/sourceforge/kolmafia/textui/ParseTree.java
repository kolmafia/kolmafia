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
import java.lang.IllegalAccessException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLConstants.ByteArrayStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.ParseNode;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptAggregateType;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptAggregateValue;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptCompositeType;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptCompositeValue;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptSymbol;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptSymbolTable;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptType;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptTypeList;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptValue;
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
			interpreter.setState( Interpreter.STATE_EXIT );
			return;
		}

		// Even if an error occurred, since we captured the result,
		// permit further execution.

		interpreter.setState( Interpreter.STATE_NORMAL );
		KoLmafia.forceContinue();
	}

	private static void printIndices( final ScriptValueList indices, final PrintStream stream, final int indent )
	{
		if ( indices == null )
		{
			return;
		}

		Iterator it = indices.iterator();
		while ( it.hasNext() )
		{
			ScriptValue current = (ScriptValue) it.next();
			Interpreter.indentLine( stream, indent );
			stream.println( "<KEY>" );
			current.print( stream, indent + 1 );
		}
	}

	public static class ScriptScope
		implements ParseNode
	{
		ScriptFunctionList functions;
		ScriptVariableList variables;
		ScriptTypeList types;
		ParseNodeList commands;
		ScriptScope parentScope;

		public ScriptScope( final ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.types = new ScriptTypeList();
			this.commands = new ParseNodeList();
			this.parentScope = parentScope;
		}

		public ScriptScope( final ParseNode command, final ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.types = new ScriptTypeList();
			this.commands = new ParseNodeList();
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
			this.commands = new ParseNodeList();
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
			this.commands = new ParseNodeList();
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

		public ScriptFunctionList getFunctionList()
		{
			return this.functions;
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

		public void addCommand( final ParseNode c )
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

		private boolean isMatchingFunction( final ScriptUserDefinedFunction existing, final ScriptUserDefinedFunction f )
		{
			// The types of the new function's parameters
			// must exactly match the types of the existing
			// function's parameters

			Iterator it1 = existing.getReferences();
			Iterator it2 = f.getReferences();

			while ( it1.hasNext() && it2.hasNext() )
			{
				ScriptVariableReference p1 = (ScriptVariableReference) it1.next();
				ScriptVariableReference p2 = (ScriptVariableReference) it2.next();

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

		public ScriptUserDefinedFunction findFunction( final ScriptUserDefinedFunction f )
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
					if ( this.isMatchingFunction( existing, f ) )
					{
						return existing;
					}
				}
			}

			return null;
		}

		public ScriptUserDefinedFunction replaceFunction( final ScriptUserDefinedFunction existing, final ScriptUserDefinedFunction f )
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
			interpreter.traceIndent();

			ParseNode current;
			Iterator it = this.commands.iterator();

			while ( it.hasNext() )
			{
				current = (ParseNode) it.next();
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
				ScriptType currentType = (ScriptType) it.next();
				currentType.print( stream, indent + 2 );
			}

			Interpreter.indentLine( stream, indent + 1 );
			stream.println( "<VARIABLES>" );

			it = this.getVariables();
			while ( it.hasNext() )
			{
				ScriptVariable currentVar = (ScriptVariable) it.next();
				currentVar.print( stream, indent + 2 );
			}

			Interpreter.indentLine( stream, indent + 1 );
			stream.println( "<FUNCTIONS>" );

			it = this.getFunctions();
			while ( it.hasNext() )
			{
				ScriptFunction currentFunc = (ScriptFunction) it.next();
				currentFunc.print( stream, indent + 2 );
			}

			Interpreter.indentLine( stream, indent + 1 );
			stream.println( "<COMMANDS>" );

			it = this.getCommands();
			while ( it.hasNext() )
			{
				ParseNode currentCommand = (ParseNode) it.next();
				currentCommand.print( stream, indent + 2 );
			}
		}
	}

	public static abstract class ScriptFunction
		extends ScriptSymbol
		implements ParseNode
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
				for ( int i = 0; it.hasNext(); ++i )
				{
					ScriptVariableReference current = (ScriptVariableReference) it.next();

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
			Interpreter.indentLine( stream, indent );
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
				throw new AdvancedScriptException( "Calling undefined user function: " + this.getName() );
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
		private final ScriptValue[] values;

		public ScriptExistingFunction( final String name, final ScriptType type, final ScriptType[] params )
		{
			this( name, type, params, null );
		}

		public ScriptExistingFunction( final String name, final ScriptType type, final ScriptType[] params,
			final String description )
		{
			super( name.toLowerCase(), type );
			this.description = description;

			this.values = new ScriptValue[ params.length ];
			Class[] args = new Class[ params.length ];

			for ( int i = 0; i < params.length; ++i )
			{
				ScriptVariable variable = new ScriptVariable( params[ i ] );
				this.variableReferences.addElement( new ScriptVariableReference( variable ) );
				args[ i ] = ScriptValue.class;
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
				throw new AdvancedScriptException( "Internal error: no method for " + this.getName() );
			}

			// Dereference variables and pass ScriptValues to function

			Iterator it = this.variableReferences.iterator();
			for ( int i = 0; it.hasNext(); ++i )
			{
				ScriptVariableReference current = (ScriptVariableReference) it.next();
				this.values[i] = current.getValue( interpreter );
			}

			try
			{
				// Invoke the method
				return (ScriptValue) this.method.invoke( this, this.values );
			}
			catch ( InvocationTargetException e )
			{
				// This is an error in the called method. Pass
				// it on up so that we'll print a stack trace.

				Throwable cause = e.getCause();
				if ( cause instanceof AdvancedScriptException )
				{
					// Pass up exceptions intentionally generated by library
					throw (AdvancedScriptException) cause;
				}
				throw new RuntimeException( cause );
			}
			catch ( IllegalAccessException e )
			{
				// This is not expected, but is an internal error in ASH
				throw new AdvancedScriptException( e );
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
		implements ParseNode
	{
		ScriptType type;
		ScriptValue content;
		ScriptValue expression = null;

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

		public void setExpression( final ScriptValue targetExpression )
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
				throw new AdvancedScriptException(
					"Internal error: Cannot assign " + targetValue.getType() + " to " + this.getType() );
			}
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			return getValue( interpreter );
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
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

		public ScriptValueList getIndices()
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
			Interpreter.indentLine( stream, indent );
			stream.println( "<VARREF> " + this.getName() );
		}
	}

	public static class ScriptCompositeReference
		extends ScriptVariableReference
	{
		private final ScriptValueList indices;

		// Derived from indices: Final slice and index into it
		private ScriptCompositeValue slice;
		private ScriptValue index;

		public ScriptCompositeReference( final ScriptVariable target, final ScriptValueList indices )
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

		public ScriptValueList getIndices()
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
				interpreter.setState( Interpreter.STATE_EXIT );
				return false;
			}

			this.slice = (ScriptCompositeValue) this.target.getValue( interpreter );
			this.index = null;

			interpreter.traceIndent();
			interpreter.trace( "AREF: " + this.slice.toString() );

			int count = this.indices.size();
			for ( int i = 0; i < count; ++i )
			{
				ScriptValue exp = (ScriptValue) this.indices.get( i );

				interpreter.traceIndent();
				interpreter.trace( "Key #" + ( i + 1 ) + ": " + exp.toQuotedString() );

				this.index = exp.execute( interpreter );
				ParseTree.captureValue( interpreter, this.index );
				if ( this.index == null )
				{
					this.index = DataTypes.VOID_VALUE;
				}

				interpreter.trace( "[" + interpreter.getState() + "] <- " + this.index.toQuotedString() );
				interpreter.traceUnindent();

				if ( interpreter.getState() == Interpreter.STATE_EXIT )
				{
					interpreter.traceUnindent();
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

				interpreter.trace( "AREF <- " + this.slice.toString() );
			}

			interpreter.traceUnindent();

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

				interpreter.traceIndent();
				interpreter.trace( "AREF <- " + result.toQuotedString() );
				interpreter.traceUnindent();

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
				interpreter.traceIndent();
				interpreter.trace( "ASET: " + targetValue.toQuotedString() );
				interpreter.traceUnindent();
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
				interpreter.traceIndent();
				interpreter.trace( "remove <- " + result.toQuotedString() );
				interpreter.traceUnindent();
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
			interpreter.traceIndent();
			interpreter.trace( "contains <- " + result );
			interpreter.traceUnindent();
			return result;
		}

		public String toString()
		{
			return this.target.getName() + "[]";
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
			stream.println( "<AGGREF " + this.getName() + ">" );
			ParseTree.printIndices( this.getIndices(), stream, indent + 1 );
		}
	}

	private static abstract class ScriptFlowControl
		implements ParseNode
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
			interpreter.traceIndent();
			interpreter.trace( this.toString() );
			interpreter.traceUnindent();
			interpreter.setState( this.state );
			return DataTypes.VOID_VALUE;
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
			stream.println( "<COMMAND " + this.state + ">" );
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

	public static class ScriptReturn
		implements ParseNode
	{
		private final ScriptValue returnValue;
		private final ScriptType expectedType;

		public ScriptReturn( final ScriptValue returnValue, final ScriptType expectedType )
		{
			this.returnValue = returnValue;
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

		public ScriptValue getExpression()
		{
			return this.returnValue;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				interpreter.setState( Interpreter.STATE_EXIT );
			}

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				return null;
			}

			interpreter.setState( Interpreter.STATE_RETURN );

			if ( this.returnValue == null )
			{
				return null;
			}

			interpreter.traceIndent();
			interpreter.trace( "Eval: " + this.returnValue );

			ScriptValue result = this.returnValue.execute( interpreter );
			ParseTree.captureValue( interpreter, result );

			interpreter.trace( "Returning: " + result );
			interpreter.traceUnindent();

			if ( result == null )
			{
				return null;
			}

			if ( interpreter.getState() != Interpreter.STATE_EXIT )
			{
				interpreter.setState( Interpreter.STATE_RETURN );
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
			Interpreter.indentLine( stream, indent );
			stream.println( "<RETURN " + this.getType() + ">" );
			if ( !this.getType().equals( DataTypes.TYPE_VOID ) )
			{
				this.returnValue.print( stream, indent + 1 );
			}
		}
	}

	public abstract static class ScriptConditional
		implements ParseNode
	{
		public ScriptScope scope;
		private final ScriptValue condition;

		public ScriptConditional( final ScriptScope scope, final ScriptValue condition )
		{
			this.scope = scope;
			this.condition = condition;
		}

		public ScriptScope getScope()
		{
			return this.scope;
		}

		public ScriptValue getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				interpreter.setState( Interpreter.STATE_EXIT );
				return null;
			}

			interpreter.traceIndent();
			interpreter.trace( this.toString() );

			interpreter.trace( "Test: " + this.condition );

			ScriptValue conditionResult = this.condition.execute( interpreter );
			ParseTree.captureValue( interpreter, conditionResult );

			interpreter.trace( "[" + interpreter.getState() + "] <- " + conditionResult );

			if ( conditionResult == null )
			{
				interpreter.traceUnindent();
				return null;
			}

			if ( conditionResult.intValue() == 1 )
			{
				ScriptValue result = this.scope.execute( interpreter );

				interpreter.traceUnindent();

				if ( interpreter.getState() != Interpreter.STATE_NORMAL )
				{
					return result;
				}

				return DataTypes.TRUE_VALUE;
			}

			interpreter.traceUnindent();
			return DataTypes.FALSE_VALUE;
		}
	}

	public static class ScriptIf
		extends ScriptConditional
	{
		private final ArrayList elseLoops;

		public ScriptIf( final ScriptScope scope, final ScriptValue condition )
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
			if ( interpreter.getState() != Interpreter.STATE_NORMAL || result == DataTypes.TRUE_VALUE )
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

				if ( interpreter.getState() != Interpreter.STATE_NORMAL || result == DataTypes.TRUE_VALUE )
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
			Interpreter.indentLine( stream, indent );
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
		public ScriptElseIf( final ScriptScope scope, final ScriptValue condition )
		{
			super( scope, condition );
		}

		public String toString()
		{
			return "else if";
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
			stream.println( "<ELSE IF>" );
			this.getCondition().print( stream, indent + 1 );
			this.getScope().print( stream, indent + 1 );
		}
	}

	public static class ScriptElse
		extends ScriptConditional
	{
		public ScriptElse( final ScriptScope scope, final ScriptValue condition )
		{
			super( scope, condition );
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				interpreter.setState( Interpreter.STATE_EXIT );
				return null;
			}

			interpreter.traceIndent();
			interpreter.trace( "else" );
			ScriptValue result = this.scope.execute( interpreter );
			interpreter.traceUnindent();

			if ( interpreter.getState() != Interpreter.STATE_NORMAL )
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
			Interpreter.indentLine( stream, indent );
			stream.println( "<ELSE>" );
			this.getScope().print( stream, indent + 1 );
		}
	}

	public abstract static class ScriptLoop
		implements ParseNode
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
				interpreter.setState( Interpreter.STATE_EXIT );
			}

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				return null;
			}

			if ( interpreter.getState() == Interpreter.STATE_BREAK )
			{
				// Stay in state; subclass exits loop
				return DataTypes.VOID_VALUE;
			}

			if ( interpreter.getState() == Interpreter.STATE_CONTINUE )
			{
				// Done with this iteration
				interpreter.setState( Interpreter.STATE_NORMAL );
			}

			if ( interpreter.getState() == Interpreter.STATE_RETURN )
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
				interpreter.setState( Interpreter.STATE_EXIT );
				return null;
			}

			interpreter.traceIndent();
			interpreter.trace( this.toString() );

			// Evaluate the aggref to get the slice
			ScriptAggregateValue slice = (ScriptAggregateValue) this.aggregate.execute( interpreter );
			ParseTree.captureValue( interpreter, slice );
			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
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

				interpreter.trace( "Key #" + i + ": " + key );

				// If there are more indices to bind, recurse
				ScriptValue result;
				if ( nextVariable != null )
				{
					ScriptAggregateValue nextSlice = (ScriptAggregateValue) slice.aref( key );
					interpreter.traceIndent();
					result = this.executeSlice( interpreter, nextSlice, it, nextVariable );
				}
				else
				{
					// Otherwise, execute scope
					result = super.execute( interpreter );
				}

				if ( interpreter.getState() == Interpreter.STATE_NORMAL )
				{
					continue;
				}

				if ( interpreter.getState() == Interpreter.STATE_BREAK )
				{
					interpreter.setState( Interpreter.STATE_NORMAL );
				}

				interpreter.traceUnindent();
				return result;
			}

			interpreter.traceUnindent();
			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return "foreach";
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
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
		private final ScriptValue condition;

		public ScriptWhile( final ScriptScope scope, final ScriptValue condition )
		{
			super( scope );
			this.condition = condition;
		}

		public ScriptValue getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				interpreter.setState( Interpreter.STATE_EXIT );
				return null;
			}

			interpreter.traceIndent();
			interpreter.trace( this.toString() );

			while ( true )
			{
				interpreter.trace( "Test: " + this.condition );

				ScriptValue conditionResult = this.condition.execute( interpreter );
				ParseTree.captureValue( interpreter, conditionResult );

				interpreter.trace( "[" + interpreter.getState() + "] <- " + conditionResult );

				if ( conditionResult == null )
				{
					interpreter.traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() != 1 )
				{
					break;
				}

				ScriptValue result = super.execute( interpreter );

				if ( interpreter.getState() == Interpreter.STATE_BREAK )
				{
					interpreter.setState( Interpreter.STATE_NORMAL );
					interpreter.traceUnindent();
					return DataTypes.VOID_VALUE;
				}

				if ( interpreter.getState() != Interpreter.STATE_NORMAL )
				{
					interpreter.traceUnindent();
					return result;
				}
			}

			interpreter.traceUnindent();
			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return "while";
		}

		public void print( final PrintStream stream, final int indent )
		{
                        Interpreter.indentLine( stream, indent );
                        stream.println( "<WHILE>" );
                        this.getCondition().print( stream, indent + 1 );
                        this.getScope().print( stream, indent + 1 );
                }

	}

	public static class ScriptRepeat
		extends ScriptLoop
	{
		private final ScriptValue condition;

		public ScriptRepeat( final ScriptScope scope, final ScriptValue condition )
		{
			super( scope );
			this.condition = condition;
		}

		public ScriptValue getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			if ( !KoLmafia.permitsContinue() )
			{
				interpreter.setState( Interpreter.STATE_EXIT );
				return null;
			}

			interpreter.traceIndent();
			interpreter.trace( this.toString() );

			while ( true )
			{
				ScriptValue result = super.execute( interpreter );

				if ( interpreter.getState() == Interpreter.STATE_BREAK )
				{
					interpreter.setState( Interpreter.STATE_NORMAL );
					interpreter.traceUnindent();
					return DataTypes.VOID_VALUE;
				}

				if ( interpreter.getState() != Interpreter.STATE_NORMAL )
				{
					interpreter.traceUnindent();
					return result;
				}

				interpreter.trace( "Test: " + this.condition );

				ScriptValue conditionResult = this.condition.execute( interpreter );
				ParseTree.captureValue( interpreter, conditionResult );

				interpreter.trace( "[" + interpreter.getState() + "] <- " + conditionResult );

				if ( conditionResult == null )
				{
					interpreter.traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() == 1 )
				{
					break;
				}
			}

			interpreter.traceUnindent();
			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return "repeat";
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
			stream.println( "<REPEAT>" );
			this.getScope().print( stream, indent + 1 );
			this.getCondition().print( stream, indent + 1 );
		}
	}

	public static class ScriptFor
		extends ScriptLoop
	{
		private final ScriptVariableReference variable;
		private final ScriptValue initial;
		private final ScriptValue last;
		private final ScriptValue increment;
		private final int direction;

		public ScriptFor( final ScriptScope scope, final ScriptVariableReference variable,
			final ScriptValue initial, final ScriptValue last, final ScriptValue increment,
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

		public ScriptValue getInitial()
		{
			return this.initial;
		}

		public ScriptValue getLast()
		{
			return this.last;
		}

		public ScriptValue getIncrement()
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
				interpreter.setState( Interpreter.STATE_EXIT );
				return null;
			}

			interpreter.traceIndent();
			interpreter.trace( this.toString() );

			// Get the initial value
			interpreter.trace( "Initial: " + this.initial );

			ScriptValue initialValue = this.initial.execute( interpreter );
			ParseTree.captureValue( interpreter, initialValue );

			interpreter.trace( "[" + interpreter.getState() + "] <- " + initialValue );

			if ( initialValue == null )
			{
				interpreter.traceUnindent();
				return null;
			}

			// Get the final value
			interpreter.trace( "Last: " + this.last );

			ScriptValue lastValue = this.last.execute( interpreter );
			ParseTree.captureValue( interpreter, lastValue );

			interpreter.trace( "[" + interpreter.getState() + "] <- " + lastValue );

			if ( lastValue == null )
			{
				interpreter.traceUnindent();
				return null;
			}

			// Get the increment
			interpreter.trace( "Increment: " + this.increment );

			ScriptValue incrementValue = this.increment.execute( interpreter );
			ParseTree.captureValue( interpreter, incrementValue );

			interpreter.trace( "[" + interpreter.getState() + "] <- " + incrementValue );

			if ( incrementValue == null )
			{
				interpreter.traceUnindent();
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

				if ( interpreter.getState() == Interpreter.STATE_BREAK )
				{
					interpreter.setState( Interpreter.STATE_NORMAL );
					interpreter.traceUnindent();
					return DataTypes.VOID_VALUE;
				}

				if ( interpreter.getState() != Interpreter.STATE_NORMAL )
				{
					interpreter.traceUnindent();
					return result;
				}

				// Calculate next value
				current += increment;
			}

			interpreter.traceUnindent();
			return DataTypes.VOID_VALUE;
		}

		public String toString()
		{
			return "for";
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
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
		implements ParseNode
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

		public void print( final PrintStream stream, final int indent )
		{
		}
	}

	public static class ScriptCall
		extends ScriptValue
	{
		private final ScriptFunction target;
		private final ScriptValueList params;

		public ScriptCall( final ScriptFunction target, final ScriptValueList params )
		{
			this.target = target;
			this.params = params;
		}

		public ScriptFunction getTarget()
		{
			return this.target;
		}

		public Iterator getValues()
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
				interpreter.setState( Interpreter.STATE_EXIT );
				return null;
			}

			// Save current variable bindings
			this.target.saveBindings( interpreter );
			interpreter.traceIndent();

			Iterator refIterator = this.target.getReferences();
			Iterator valIterator = this.params.getValues();

			ScriptVariableReference paramVarRef;
			ScriptValue paramValue;

			int paramCount = 0;

			while ( refIterator.hasNext() )
			{
				paramVarRef = (ScriptVariableReference) refIterator.next();

				++paramCount;

				if ( !valIterator.hasNext() )
				{
					this.target.restoreBindings( interpreter );
					throw new AdvancedScriptException( "Internal error: illegal arguments" );
				}

				paramValue = (ScriptValue) valIterator.next();

				interpreter.trace( "Param #" + paramCount + ": " + paramValue.toQuotedString() );

				ScriptValue value = paramValue.execute( interpreter );
				ParseTree.captureValue( interpreter, value );
				if ( value == null )
				{
					value = DataTypes.VOID_VALUE;
				}

				interpreter.trace( "[" + interpreter.getState() + "] <- " + value.toQuotedString() );

				if ( interpreter.getState() == Interpreter.STATE_EXIT )
				{
					this.target.restoreBindings( interpreter );
					interpreter.traceUnindent();
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
				throw new AdvancedScriptException( "Internal error: illegal arguments" );
			}

			interpreter.trace( "Entering function " + this.target.getName() );
			ScriptValue result = this.target.execute( interpreter );
			interpreter.trace( "Function " + this.target.getName() + " returned: " + result );

			if ( interpreter.getState() != Interpreter.STATE_EXIT )
			{
				interpreter.setState( Interpreter.STATE_NORMAL );
			}

			// Restore initial variable bindings
			this.target.restoreBindings( interpreter );
			interpreter.traceUnindent();

			return result;
		}

		public String toString()
		{
			return this.target.getName() + "()";
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
			stream.println( "<CALL " + this.getTarget().getName() + ">" );

			Iterator it = this.getValues();
			while ( it.hasNext() )
			{
				ScriptValue current = (ScriptValue) it.next();
				current.print( stream, indent + 1 );
			}
		}
	}

	public static class ScriptAssignment
		implements ParseNode
	{
		private final ScriptVariableReference lhs;
		private final ScriptValue rhs;

		public ScriptAssignment( final ScriptVariableReference lhs, final ScriptValue rhs )
		{
			this.lhs = lhs;
			this.rhs = rhs;
		}

		public ScriptVariableReference getLeftHandSide()
		{
			return this.lhs;
		}

		public ScriptValue getRightHandSide()
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
				interpreter.setState( Interpreter.STATE_EXIT );
				return null;
			}

			ScriptValue value;

			if ( this.rhs == null )
			{
				value = this.lhs.getType().initialValue();
			}
			else
			{
				interpreter.traceIndent();
				interpreter.trace( "Eval: " + this.rhs );

				value = this.rhs.execute( interpreter );
				ParseTree.captureValue( interpreter, value );

				interpreter.trace( "Set: " + value );
				interpreter.traceUnindent();
			}

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
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
			Interpreter.indentLine( stream, indent );
			stream.println( "<ASSIGN " + this.lhs.getName() + ">" );
			ScriptVariableReference lhs = this.getLeftHandSide();
			ParseTree.printIndices( lhs.getIndices(), stream, indent + 1 );
			this.getRightHandSide().print( stream, indent + 1 );
		}
	}

	public static class ScriptExpression
		extends ScriptValue
	{
		ScriptValue lhs;
		ScriptValue rhs;
		ScriptOperator oper;

		public ScriptExpression()
		{
		}

		public ScriptExpression( final ScriptValue lhs, final ScriptValue rhs, final ScriptOperator oper )
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

		public ScriptValue getLeftHandSide()
		{
			return this.lhs;
		}

		public ScriptValue getRightHandSide()
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

	public static class ScriptOperator
		implements ParseNode
	{
		String operator;

		public ScriptOperator( final String operator )
		{
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

		public ScriptValue applyTo( final Interpreter interpreter, final ScriptValue lhs, final ScriptValue rhs )
		{
			interpreter.traceIndent();
			interpreter.trace( "Operator: " + this.operator );

			// Unary operator with special evaluation of argument
			if ( this.operator.equals( "remove" ) )
			{
				ScriptCompositeReference operand = (ScriptCompositeReference) lhs;
				interpreter.traceIndent();
				interpreter.trace( "Operand: " + operand );
				interpreter.traceUnindent();
				ScriptValue result = operand.removeKey( interpreter );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			interpreter.traceIndent();
			interpreter.trace( "Operand 1: " + lhs );

			ScriptValue leftValue = lhs.execute( interpreter );
			ParseTree.captureValue( interpreter, leftValue );
			if ( leftValue == null )
			{
				leftValue = DataTypes.VOID_VALUE;
			}
			interpreter.trace( "[" + interpreter.getState() + "] <- " + leftValue.toQuotedString() );
			interpreter.traceUnindent();

			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			// Unary Operators
			if ( this.operator.equals( "!" ) )
			{
				ScriptValue result = new ScriptValue( leftValue.intValue() == 0 );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
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
					throw new AdvancedScriptException( "Internal error: Unary minus can only be applied to numbers" );
				}
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			// Unknown operator
			if ( rhs == null )
			{
				throw new AdvancedScriptException( "Internal error: missing right operand." );
			}

			// Binary operators with optional right values
			if ( this.operator.equals( "||" ) )
			{
				if ( leftValue.intValue() == 1 )
				{
					interpreter.trace( "<- " + DataTypes.TRUE_VALUE );
					interpreter.traceUnindent();
					return DataTypes.TRUE_VALUE;
				}
				interpreter.traceIndent();
				interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute( interpreter );
				ParseTree.captureValue( interpreter, rightValue );
				if ( rightValue == null )
				{
					rightValue = DataTypes.VOID_VALUE;
				}
				interpreter.trace( "[" + interpreter.getState() + "] <- " + rightValue.toQuotedString() );
				interpreter.traceUnindent();
				if ( interpreter.getState() == Interpreter.STATE_EXIT )
				{
					interpreter.traceUnindent();
					return null;
				}
				interpreter.trace( "<- " + rightValue );
				interpreter.traceUnindent();
				return rightValue;
			}

			if ( this.operator.equals( "&&" ) )
			{
				if ( leftValue.intValue() == 0 )
				{
					interpreter.traceUnindent();
					interpreter.trace( "<- " + DataTypes.FALSE_VALUE );
					return DataTypes.FALSE_VALUE;
				}
				interpreter.traceIndent();
				interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute( interpreter );
				ParseTree.captureValue( interpreter, rightValue );
				if ( rightValue == null )
				{
					rightValue = DataTypes.VOID_VALUE;
				}
				interpreter.trace( "[" + interpreter.getState() + "] <- " + rightValue.toQuotedString() );
				interpreter.traceUnindent();
				if ( interpreter.getState() == Interpreter.STATE_EXIT )
				{
					interpreter.traceUnindent();
					return null;
				}
				interpreter.trace( "<- " + rightValue );
				interpreter.traceUnindent();
				return rightValue;
			}

			// Ensure type compatibility of operands
			if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), this.operator ) )
			{
				throw new AdvancedScriptException( "Internal error: left hand side and right hand side do not correspond" );
			}

			// Special binary operator: <aggref> contains <any>
			if ( this.operator.equals( "contains" ) )
			{
				interpreter.traceIndent();
				interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute( interpreter );
				ParseTree.captureValue( interpreter, rightValue );
				if ( rightValue == null )
				{
					rightValue = DataTypes.VOID_VALUE;
				}
				interpreter.trace( "[" + interpreter.getState() + "] <- " + rightValue.toQuotedString() );
				interpreter.traceUnindent();
				if ( interpreter.getState() == Interpreter.STATE_EXIT )
				{
					interpreter.traceUnindent();
					return null;
				}
				ScriptValue result = new ScriptValue( leftValue.contains( rightValue ) );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			// Binary operators
			interpreter.traceIndent();
			interpreter.trace( "Operand 2: " + rhs );
			ScriptValue rightValue = rhs.execute( interpreter );
			ParseTree.captureValue( interpreter, rightValue );
			if ( rightValue == null )
			{
				rightValue = DataTypes.VOID_VALUE;
			}
			interpreter.trace( "[" + interpreter.getState() + "] <- " + rightValue.toQuotedString() );
			interpreter.traceUnindent();
			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}

			// String operators
			if ( this.operator.equals( "+" ) )
			{
				if ( lhs.getType().equals( DataTypes.TYPE_STRING ) || rhs.getType().equals( DataTypes.TYPE_STRING ) )
				{
					String string = leftValue.toStringValue().toString() + rightValue.toStringValue().toString();
					ScriptValue result = new ScriptValue( string );
					interpreter.trace( "<- " + result );
					interpreter.traceUnindent();
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
					interpreter.trace( "<- " + result );
					interpreter.traceUnindent();
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
					interpreter.trace( "<- " + result );
					interpreter.traceUnindent();
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
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "-" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint - rint ) : new ScriptValue( lfloat - rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "*" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint * rint ) : new ScriptValue( lfloat * rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "^" ) )
			{
				ScriptValue result =
					isInt ? new ScriptValue( (int) Math.pow( lint, rint ) ) : new ScriptValue( (float) Math.pow(
						lfloat, rfloat ) );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "/" ) )
			{
				ScriptValue result =
					isInt ? new ScriptValue( (float) lint / (float) rint ) : new ScriptValue( lfloat / rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "%" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint % rint ) : new ScriptValue( lfloat % rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "<" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint < rint ) : new ScriptValue( lfloat < rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( ">" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint > rint ) : new ScriptValue( lfloat > rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "<=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint <= rint ) : new ScriptValue( lfloat <= rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( ">=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint >= rint ) : new ScriptValue( lfloat >= rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "=" ) || this.operator.equals( "==" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint == rint ) : new ScriptValue( lfloat == rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			if ( this.operator.equals( "!=" ) )
			{
				ScriptValue result = isInt ? new ScriptValue( lint != rint ) : new ScriptValue( lfloat != rfloat );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}

			// Unknown operator
			throw new AdvancedScriptException( "Internal error: illegal operator \"" + this.operator + "\"" );
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			return null;
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
			stream.println( "<OPER " + this.operator + ">" );
		}
	}

	private static class ScriptList
		extends ArrayList
	{
		public boolean addElement( final Object n )
		{
			this.add( n );
			return true;
		}
	}

	private static class ParseNodeList
		extends ScriptList
	{
		public boolean addElement( final ParseNode n )
		{
			return super.addElement( n );
		}
	}

	public static class ScriptValueList
		extends ScriptList
	{
		public boolean addElement( final ScriptValue n )
		{
			return super.addElement( n );
		}

		public Iterator getValues()
		{
			return this.iterator();
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
}
