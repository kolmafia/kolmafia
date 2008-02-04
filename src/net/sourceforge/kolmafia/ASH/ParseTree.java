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

package net.sourceforge.kolmafia.ASH;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AreaCombatData;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.ItemManageFrame;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ByteArrayStream;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.KoLFrame;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ASH.Interpreter;
import net.sourceforge.kolmafia.ASH.Interpreter.AdvancedScriptException;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Monster;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.request.ChezSnooteeRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.DisplayCaseRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.MicroBreweryRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

public abstract class ParseTree
{
	private static StringBuffer concatenateBuffer = new StringBuffer();
	private static final GenericRequest VISITOR = new GenericRequest( "" );
	private static final RelayRequest RELAYER = new RelayRequest( false );

	private static final void indentLine( final PrintStream stream, final int indent )
	{
		if ( stream == null )
		{
			return;
		}

		for ( int i = 0; i < indent; ++i )
		{
			stream.print( "   " );
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
					if ( ( (ScriptVariableReference) refIterator.next() ).getType().equals( Interpreter.STRING_TYPE ) )
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
					else if ( !Interpreter.validCoercion( currentParam.getType(), currentValue.getType(), "parameter" ) )
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

			if ( !isExactMatch && source == Interpreter.existingFunctions && errorMessage != null )
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
				result = this.findFunction( Interpreter.existingFunctions, name, params, true );
			}
			if ( result == null )
			{
				result = this.findFunction( this.functions, name, params, false );
			}
			if ( result == null )
			{
				result = this.findFunction( Interpreter.existingFunctions, name, params, false );
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

		public ScriptValue execute()
		{
			// Yield control at the top of the scope to
			// allow other tasks to run and keyboard input -
			// especially the Escape key - to be accepted.

			// Unfortunately, the following does not work
			// Thread.yield();

			// ...but the following does.
			GenericRequest.delay(1);

			ScriptValue result = Interpreter.VOID_VALUE;
			Interpreter.traceIndent();

			ScriptCommand current;
			Iterator it = this.commands.iterator();

			while ( it.hasNext() )
			{
				current = (ScriptCommand) it.next();
				result = current.execute();

				// Abort processing now if command failed
				if ( !KoLmafia.permitsContinue() )
				{
					Interpreter.currentState = Interpreter.STATE_EXIT;
				}

				if ( result == null )
				{
					result = Interpreter.VOID_VALUE;
				}

				Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + result.toQuotedString() );
				switch ( Interpreter.currentState )
				{
				case Interpreter.STATE_RETURN:
				case Interpreter.STATE_BREAK:
				case Interpreter.STATE_CONTINUE:
				case Interpreter.STATE_EXIT:

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

		public void saveBindings()
		{
		}

		public void restoreBindings()
		{
		}

		public void printDisabledMessage()
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
					message.append( current.getValue().toStringValue().toString() );
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

		public abstract ScriptValue execute();

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

		public void saveBindings()
		{
			if ( this.scope == null )
			{
				return;
			}

			ArrayList values = new ArrayList();
			for ( int i = 0; i < this.scope.variables.size(); ++i )
			{
				values.add( ( (ScriptVariable) this.scope.variables.get( i ) ).getValue() );
			}

			this.callStack.push( values );
		}

		public void restoreBindings()
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

		public ScriptValue execute()
		{
			if ( StaticEntity.isDisabled( this.getName() ) )
			{
				this.printDisabledMessage();
				return this.getType().initialValue();
			}

			if ( this.scope == null )
			{
				throw new RuntimeException( "Calling undefined user function: " + this.getName() );
			}

			ScriptValue result = this.scope.execute();

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
		private final ScriptVariable[] variables;

		public ScriptExistingFunction( final String name, final ScriptType type, final ScriptType[] params )
		{
			this( name, type, params, null );
		}

		public ScriptExistingFunction( final String name, final ScriptType type, final ScriptType[] params,
			final String description )
		{
			super( name.toLowerCase(), type );
			this.description = description;

			this.variables = new ScriptVariable[ params.length ];
			Class[] args = new Class[ params.length ];

			for ( int i = 0; i < params.length; ++i )
			{
				this.variables[ i ] = new ScriptVariable( params[ i ] );
				this.variableReferences.addElement( new ScriptVariableReference( this.variables[ i ] ) );
				args[ i ] = ScriptVariable.class;
			}

			try
			{
				this.method = ScriptExistingFunction.class.getMethod( name, args );
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

		public ScriptValue execute()
		{
			if ( StaticEntity.isDisabled( this.getName() ) )
			{
				this.printDisabledMessage();
				return this.getType().initialValue();
			}

			if ( this.method == null )
			{
				throw new RuntimeException( "Internal error: no method for " + this.getName() );
			}

			try
			{
				// Invoke the method

				return (ScriptValue) this.method.invoke( this, this.variables );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				throw new AdvancedScriptException( e.getCause() == null ? e : e.getCause() );
			}
		}

		private ScriptValue continueValue()
		{
			return KoLmafia.permitsContinue() && !KoLmafia.hadPendingState() ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		// Basic utility functions which print information
		// or allow for easy testing.

		public ScriptValue enable( final ScriptVariable name )
		{
			StaticEntity.enable( name.toStringValue().toString().toLowerCase() );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue disable( final ScriptVariable name )
		{
			StaticEntity.disable( name.toStringValue().toString().toLowerCase() );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue user_confirm( final ScriptVariable message )
		{
			return KoLFrame.confirm( message.toStringValue().toString() ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue logprint( final ScriptVariable string )
		{
			String parameters = string.toStringValue().toString();

			parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.getSessionStream().println( "> " + parameters );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue print( final ScriptVariable string )
		{
			String parameters = string.toStringValue().toString();

			parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

			RequestLogger.printLine( parameters );
			RequestLogger.getSessionStream().println( "> " + parameters );

			return Interpreter.VOID_VALUE;
		}

		public ScriptValue print( final ScriptVariable string, final ScriptVariable color )
		{
			String parameters = string.toStringValue().toString();

			parameters = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( parameters, "\n" ), "\r" );
			parameters = StaticEntity.globalStringReplace( parameters, "<", "&lt;" );

			String colorString = color.toStringValue().toString();
			colorString = StaticEntity.globalStringDelete( StaticEntity.globalStringDelete( colorString, "\"" ), "<" );

			RequestLogger.printLine( "<font color=\"" + colorString + "\">" + parameters + "</font>" );
			RequestLogger.getSessionStream().println( " > " + parameters );

			return Interpreter.VOID_VALUE;
		}

		public ScriptValue print_html( final ScriptVariable string )
		{
			RequestLogger.printLine( string.toStringValue().toString() );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue abort()
		{
			RequestThread.declareWorldPeace();
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue abort( final ScriptVariable string )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, string.toStringValue().toString() );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue cli_execute( final ScriptVariable string )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( string.toStringValue().toString() );
			return this.continueValue();
		}

		private File getFile( String filename )
		{
			if ( filename.startsWith( "http" ) )
			{
				return null;
			}

			filename = filename.substring( filename.lastIndexOf( "/" ) + 1 );

			File f = new File( KoLConstants.SCRIPT_LOCATION, filename );
			if ( f.exists() )
			{
				return f;
			}

			f = new File( UtilityConstants.DATA_LOCATION, filename );
			if ( f.exists() )
			{
				return f;
			}

			f = new File( UtilityConstants.ROOT_LOCATION, filename );
			if ( f.exists() )
			{
				return f;
			}

			if ( filename.endsWith( ".dat" ) )
			{
				return this.getFile( filename.substring( 0, filename.length() - 4 ) + ".txt" );
			}

			return new File( KoLConstants.SCRIPT_LOCATION, filename );
		}

		private BufferedReader getReader( final String filename )
		{
			if ( filename.startsWith( "http" ) )
			{
				return DataUtilities.getReader( filename );
			}

			File input = this.getFile( filename );
			if ( input.exists() )
			{
				return DataUtilities.getReader( input );
			}

			BufferedReader reader = DataUtilities.getReader( "data", filename );
			return reader != null ? reader : DataUtilities.getReader( filename );
		}

		public ScriptValue load_html( final ScriptVariable string )
		{
			StringBuffer buffer = new StringBuffer();
			ScriptValue returnValue = new ScriptValue( Interpreter.BUFFER_TYPE, "", buffer );

			String location = string.toStringValue().toString();
			if ( !location.endsWith( ".htm" ) && !location.endsWith( ".html" ) )
			{
				return returnValue;
			}

			File input = this.getFile( location );
			if ( input == null || !input.exists() )
			{
				return returnValue;
			}

			ParseTree.VISITOR.loadResponseFromFile( input );
			if ( ParseTree.VISITOR.responseText != null )
			{
				buffer.append( ParseTree.VISITOR.responseText );
			}

			return returnValue;
		}

		public ScriptValue write( final ScriptVariable string )
		{
			if ( KoLmafiaASH.relayScript == null )
			{
				return Interpreter.VOID_VALUE;
			}

			KoLmafiaASH.serverReplyBuffer.append( string.toStringValue().toString() );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue writeln( final ScriptVariable string )
		{
			if ( KoLmafiaASH.relayScript == null )
			{
				return Interpreter.VOID_VALUE;
			}

			this.write( string );
			KoLmafiaASH.serverReplyBuffer.append( KoLConstants.LINE_BREAK );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue form_field( final ScriptVariable key )
		{
			if ( KoLmafiaASH.relayRequest == null )
			{
				return Interpreter.STRING_INIT;
			}

			String value = KoLmafiaASH.relayRequest.getFormField( key.toStringValue().toString() );
			return value == null ? Interpreter.STRING_INIT : new ScriptValue( value );
		}

		public ScriptValue visit_url()
		{
			StringBuffer buffer = new StringBuffer();
			ScriptValue returnValue = new ScriptValue( Interpreter.BUFFER_TYPE, "", buffer );

			RequestThread.postRequest( KoLmafiaASH.relayRequest );
			if ( KoLmafiaASH.relayRequest.responseText != null )
			{
				buffer.append( KoLmafiaASH.relayRequest.responseText );
			}

			return returnValue;
		}

		public ScriptValue visit_url( final ScriptVariable string )
		{
			return this.visit_url( string.toStringValue().toString() );
		}

		private ScriptValue visit_url( final String location )
		{
			StringBuffer buffer = new StringBuffer();
			ScriptValue returnValue = new ScriptValue( Interpreter.BUFFER_TYPE, "", buffer );

			ParseTree.VISITOR.constructURLString( location );
			if ( GenericRequest.shouldIgnore( ParseTree.VISITOR ) )
			{
				return returnValue;
			}

			if ( KoLmafiaASH.relayScript == null )
			{
				if ( ParseTree.VISITOR.getPath().equals( "fight.php" ) )
				{
					if ( FightRequest.getCurrentRound() == 0 )
					{
						return returnValue;
					}
				}

				RequestThread.postRequest( ParseTree.VISITOR );
				if ( ParseTree.VISITOR.responseText != null )
				{
					buffer.append( ParseTree.VISITOR.responseText );
					StaticEntity.externalUpdate( location, ParseTree.VISITOR.responseText );
				}
			}
			else
			{
				RequestThread.postRequest( ParseTree.RELAYER.constructURLString( location ) );
				if ( ParseTree.RELAYER.responseText != null )
				{
					buffer.append( ParseTree.RELAYER.responseText );
				}
			}

			return returnValue;
		}

		public ScriptValue wait( final ScriptVariable delay )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "wait " + delay.intValue() );
			return Interpreter.VOID_VALUE;
		}

		// Type conversion functions which allow conversion
		// of one data format to another.

		public ScriptValue to_string( final ScriptVariable val )
		{
			return val.toStringValue();
		}

		public ScriptValue to_boolean( final ScriptVariable val )
		{
			return val.toStringValue().toString().equals( "true" ) || val.intValue() != 0 ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue to_int( final ScriptVariable val )
		{
			return val.getValueType().equals( Interpreter.TYPE_STRING ) ? Interpreter.parseIntValue( val.toStringValue().toString() ) : new ScriptValue(
				val.intValue() );
		}

		public ScriptValue to_float( final ScriptVariable val )
		{
			return val.getValueType().equals( Interpreter.TYPE_STRING ) ? Interpreter.parseFloatValue( val.toStringValue().toString() ) : val.intValue() != 0 ? new ScriptValue(
				(float) val.intValue() ) : new ScriptValue( val.floatValue() );
		}

		public ScriptValue to_item( final ScriptVariable val )
		{
			return val.getValueType().equals( Interpreter.TYPE_INT ) ? Interpreter.makeItemValue( val.intValue() ) : Interpreter.parseItemValue( val.toStringValue().toString() );
		}

		public ScriptValue to_class( final ScriptVariable val )
		{
			return Interpreter.parseClassValue( val.toStringValue().toString() );
		}

		public ScriptValue to_stat( final ScriptVariable val )
		{
			return Interpreter.parseStatValue( val.toStringValue().toString() );
		}

		public ScriptValue to_skill( final ScriptVariable val )
		{
			return val.getValueType().equals( Interpreter.TYPE_INT ) ? Interpreter.makeSkillValue( val.intValue() ) : val.getValueType().equals(
				Interpreter.TYPE_EFFECT ) ? Interpreter.parseSkillValue( UneffectRequest.effectToSkill( val.toStringValue().toString() ) ) : Interpreter.parseSkillValue( val.toStringValue().toString() );
		}

		public ScriptValue to_effect( final ScriptVariable val )
		{
			return val.getValueType().equals( Interpreter.TYPE_INT ) ? Interpreter.makeEffectValue( val.intValue() ) : val.getValueType().equals(
				Interpreter.TYPE_SKILL ) ? Interpreter.parseEffectValue( UneffectRequest.skillToEffect( val.toStringValue().toString() ) ) : Interpreter.parseEffectValue( val.toStringValue().toString() );
		}

		public ScriptValue to_location( final ScriptVariable val )
		{
			return Interpreter.parseLocationValue( val.toStringValue().toString() );
		}

		public ScriptValue to_familiar( final ScriptVariable val )
		{
			return val.getValueType().equals( Interpreter.TYPE_INT ) ? Interpreter.makeFamiliarValue( val.intValue() ) : Interpreter.parseFamiliarValue( val.toStringValue().toString() );
		}

		public ScriptValue to_monster( final ScriptVariable val )
		{
			return Interpreter.parseMonsterValue( val.toStringValue().toString() );
		}

		public ScriptValue to_slot( final ScriptVariable item )
		{
			switch ( ItemDatabase.getConsumptionType( item.intValue() ) )
			{
			case KoLConstants.EQUIP_HAT:
				return Interpreter.parseSlotValue( "hat" );
			case KoLConstants.EQUIP_WEAPON:
				return Interpreter.parseSlotValue( "weapon" );
			case KoLConstants.EQUIP_OFFHAND:
				return Interpreter.parseSlotValue( "off-hand" );
			case KoLConstants.EQUIP_SHIRT:
				return Interpreter.parseSlotValue( "shirt" );
			case KoLConstants.EQUIP_PANTS:
				return Interpreter.parseSlotValue( "pants" );
			case KoLConstants.EQUIP_FAMILIAR:
				return Interpreter.parseSlotValue( "familiar" );
			case KoLConstants.EQUIP_ACCESSORY:
				return Interpreter.parseSlotValue( "acc1" );
			default:
				return Interpreter.parseSlotValue( "none" );
			}
		}

		public ScriptValue to_url( final ScriptVariable val )
		{
			KoLAdventure adventure = (KoLAdventure) val.rawValue();
			return new ScriptValue( adventure.getRequest().getURLString() );
		}

		// Functions related to daily information which get
		// updated usually once per day.

		public ScriptValue today_to_string()
		{
			return Interpreter.parseStringValue( KoLConstants.DAILY_FORMAT.format( new Date() ) );
		}

		public ScriptValue moon_phase()
		{
			return new ScriptValue( HolidayDatabase.getPhaseStep() );
		}

		public ScriptValue moon_light()
		{
			return new ScriptValue( HolidayDatabase.getMoonlight() );
		}

		public ScriptValue stat_bonus_today()
		{
			return KoLmafiaCLI.testConditional( "today is muscle day" ) ? Interpreter.parseStatValue( "muscle" ) : KoLmafiaCLI.testConditional( "today is myst day" ) ? Interpreter.parseStatValue( "mysticality" ) : KoLmafiaCLI.testConditional( "today is moxie day" ) ? Interpreter.parseStatValue( "moxie" ) : Interpreter.STAT_INIT;
		}

		public ScriptValue stat_bonus_tomorrow()
		{
			return KoLmafiaCLI.testConditional( "tomorrow is muscle day" ) ? Interpreter.parseStatValue( "muscle" ) : KoLmafiaCLI.testConditional( "tomorrow is myst day" ) ? Interpreter.parseStatValue( "mysticality" ) : KoLmafiaCLI.testConditional( "tomorrow is moxie day" ) ? Interpreter.parseStatValue( "moxie" ) : Interpreter.STAT_INIT;
		}

		public ScriptValue session_logs( final ScriptVariable dayCount )
		{
			return this.getSessionLogs( KoLCharacter.getUserName(), dayCount.intValue() );
		}

		public ScriptValue session_logs( final ScriptVariable player, final ScriptVariable dayCount )
		{
			return this.getSessionLogs( player.toStringValue().toString(), dayCount.intValue() );
		}

		private ScriptValue getSessionLogs( final String name, final int dayCount )
		{
			String[] files = new String[ dayCount ];

			Calendar timestamp = Calendar.getInstance();

			ScriptAggregateType type = new ScriptAggregateType( Interpreter.STRING_TYPE, files.length );
			ScriptArray value = new ScriptArray( type );

			String filename;
			BufferedReader reader;
			StringBuffer contents = new StringBuffer();

			for ( int i = 0; i < files.length; ++i )
			{
				contents.setLength( 0 );
				filename =
					StaticEntity.globalStringReplace( name, " ", "_" ) + "_" + KoLConstants.DAILY_FORMAT.format( timestamp.getTime() ) + ".txt";

				reader = KoLDatabase.getReader( new File( KoLConstants.SESSIONS_DIRECTORY, filename ) );
				timestamp.add( Calendar.DATE, -1 );

				if ( reader == null )
				{
					continue;
				}

				try
				{
					String line;

					while ( ( line = reader.readLine() ) != null )
					{
						contents.append( line );
						contents.append( KoLConstants.LINE_BREAK );
					}
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
				}

				value.aset( new ScriptValue( i ), Interpreter.parseStringValue( contents.toString() ) );
			}

			return value;
		}

		// Major functions related to adventuring and
		// item management.

		public ScriptValue adventure( final ScriptVariable count, final ScriptVariable loc )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "adventure " + count.intValue() + " " + loc.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue add_item_condition( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return Interpreter.VOID_VALUE;
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "conditions add " + count.intValue() + " " + item.toStringValue() );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue buy( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			AdventureResult itemToBuy = new AdventureResult( item.intValue(), 1 );
			int initialAmount = itemToBuy.getCount( KoLConstants.inventory );
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "buy " + count.intValue() + " " + item.toStringValue() );
			return initialAmount + count.intValue() == itemToBuy.getCount( KoLConstants.inventory ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue create( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "create " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue use( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "use " + count.intValue() + " " + item.toStringValue() );
			return UseItemRequest.lastUpdate.equals( "" ) ? this.continueValue() : Interpreter.FALSE_VALUE;
		}

		public ScriptValue eat( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "eat " + count.intValue() + " " + item.toStringValue() );
			return UseItemRequest.lastUpdate.equals( "" ) ? this.continueValue() : Interpreter.FALSE_VALUE;
		}

		public ScriptValue drink( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "drink " + count.intValue() + " " + item.toStringValue() );
			return UseItemRequest.lastUpdate.equals( "" ) ? this.continueValue() : Interpreter.FALSE_VALUE;
		}

		public ScriptValue put_closet( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "closet put " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue put_shop( final ScriptVariable price, final ScriptVariable limit, final ScriptVariable item )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mallsell " + item.toStringValue() + " @ " + price.intValue() + " limit " + limit.intValue() );
			return this.continueValue();
		}

		public ScriptValue put_stash( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "stash put " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue put_display( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "display put " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue take_closet( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "closet take " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue take_storage( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hagnk " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue take_display( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "display take " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue take_stash( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "stash take " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue autosell( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "sell " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue hermit( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hermit " + count.intValue() + " " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue retrieve_item( final ScriptVariable count, final ScriptVariable item )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			AdventureDatabase.retrieveItem( new AdventureResult( item.intValue(), count.intValue() ) );
			return this.continueValue();
		}

		// Major functions which provide item-related
		// information.

		public ScriptValue is_npc_item( final ScriptVariable item )
		{
			return NPCStoreDatabase.contains( ItemDatabase.getItemName( item.intValue() ), false ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue daily_special()
		{
			AdventureResult special =
				KoLCharacter.inMoxieSign() ? MicroBreweryRequest.getDailySpecial() : KoLCharacter.inMysticalitySign() ? ChezSnooteeRequest.getDailySpecial() : null;

			return special == null ? Interpreter.ITEM_INIT : Interpreter.parseItemValue( special.getName() );
		}

		public ScriptValue refresh_stash()
		{
			RequestThread.postRequest( new ClanStashRequest() );
			return this.continueValue();
		}

		public ScriptValue available_amount( final ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );

			int runningTotal = item.getCount( KoLConstants.inventory ) + item.getCount( KoLConstants.closet );

			for ( int i = 0; i <= KoLCharacter.FAMILIAR; ++i )
			{
				if ( KoLCharacter.getEquipment( i ).equals( item ) )
				{
					++runningTotal;
				}
			}

			if ( KoLCharacter.canInteract() )
			{
				runningTotal += item.getCount( KoLConstants.storage );
			}

			return new ScriptValue( runningTotal );
		}

		public ScriptValue item_amount( final ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLConstants.inventory ) );
		}

		public ScriptValue closet_amount( final ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLConstants.closet ) );
		}

		public ScriptValue creatable_amount( final ScriptVariable arg )
		{
			CreateItemRequest item = CreateItemRequest.getInstance( arg.intValue() );
			return new ScriptValue( item == null ? 0 : item.getQuantityPossible() );
		}

		public ScriptValue get_ingredients( final ScriptVariable item )
		{
			AdventureResult[] data = ConcoctionDatabase.getIngredients( item.intValue() );
			ScriptMap value = new ScriptMap( Interpreter.RESULT_TYPE );

			for ( int i = 0; i < data.length; ++i )
			{
				value.aset(
					Interpreter.parseItemValue( data[ i ].getName() ),
					Interpreter.parseIntValue( String.valueOf( data[ i ].getCount() ) ) );
			}

			return value;
		}

		public ScriptValue storage_amount( final ScriptVariable arg )
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLConstants.storage ) );
		}

		public ScriptValue display_amount( final ScriptVariable arg )
		{
			if ( KoLConstants.collection.isEmpty() )
			{
				RequestThread.postRequest( new DisplayCaseRequest() );
			}

			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( KoLConstants.collection ) );
		}

		public ScriptValue shop_amount( final ScriptVariable arg )
		{
			LockableListModel list = StoreManager.getSoldItemList();
			if ( list.isEmpty() )
			{
				RequestThread.postRequest( new ManageStoreRequest() );
			}

			SoldItem item = new SoldItem( arg.intValue(), 0, 0, 0, 0 );
			int index = list.indexOf( item );

			if ( index < 0 )
			{
				return new ScriptValue( 0 );
			}

			item = (SoldItem) list.get( index );
			return new ScriptValue( item.getQuantity() );
		}

		public ScriptValue stash_amount( final ScriptVariable arg )
		{
			List stash = ClanManager.getStash();
			if ( stash.isEmpty() )
			{
				RequestThread.postRequest( new ClanStashRequest() );
			}

			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( item.getCount( stash ) );
		}

		public ScriptValue pulls_remaining()
		{
			return new ScriptValue( ItemManageFrame.getPullsRemaining() );
		}

		public ScriptValue stills_available()
		{
			return new ScriptValue( KoLCharacter.getStillsAvailable() );
		}

		public ScriptValue have_mushroom_plot()
		{
			return new ScriptValue( MushroomManager.ownsPlot() );
		}

		// The following functions pertain to providing updated
		// information relating to the player.

		public ScriptValue refresh_status()
		{
			RequestThread.postRequest( CharPaneRequest.getInstance() );
			return this.continueValue();
		}

		public ScriptValue restore_hp( final ScriptVariable amount )
		{
			return new ScriptValue( StaticEntity.getClient().recoverHP( amount.intValue() ) );
		}

		public ScriptValue restore_mp( final ScriptVariable amount )
		{
			int desiredMP = amount.intValue();
			while ( !KoLmafia.refusesContinue() && desiredMP > KoLCharacter.getCurrentMP() )
			{
				StaticEntity.getClient().recoverMP( desiredMP );
			}
			return this.continueValue();
		}

		public ScriptValue my_name()
		{
			return new ScriptValue( KoLCharacter.getUserName() );
		}

		public ScriptValue my_id()
		{
			return new ScriptValue( KoLCharacter.getPlayerId() );
		}

		public ScriptValue my_hash()
		{
			return new ScriptValue( GenericRequest.passwordHash );
		}

		public ScriptValue in_muscle_sign()
		{
			return new ScriptValue( KoLCharacter.inMuscleSign() );
		}

		public ScriptValue in_mysticality_sign()
		{
			return new ScriptValue( KoLCharacter.inMysticalitySign() );
		}

		public ScriptValue in_moxie_sign()
		{
			return new ScriptValue( KoLCharacter.inMoxieSign() );
		}

		public ScriptValue in_bad_moon()
		{
			return new ScriptValue( KoLCharacter.inBadMoon() );
		}

		public ScriptValue my_class()
		{
			return Interpreter.makeClassValue( KoLCharacter.getClassType() );
		}

		public ScriptValue my_level()
		{
			return new ScriptValue( KoLCharacter.getLevel() );
		}

		public ScriptValue my_hp()
		{
			return new ScriptValue( KoLCharacter.getCurrentHP() );
		}

		public ScriptValue my_maxhp()
		{
			return new ScriptValue( KoLCharacter.getMaximumHP() );
		}

		public ScriptValue my_mp()
		{
			return new ScriptValue( KoLCharacter.getCurrentMP() );
		}

		public ScriptValue my_maxmp()
		{
			return new ScriptValue( KoLCharacter.getMaximumMP() );
		}

		public ScriptValue my_primestat()
		{
			int primeIndex = KoLCharacter.getPrimeIndex();
			return primeIndex == 0 ? Interpreter.parseStatValue( "muscle" ) : primeIndex == 1 ? Interpreter.parseStatValue( "mysticality" ) : Interpreter.parseStatValue( "moxie" );
		}

		public ScriptValue my_basestat( final ScriptVariable arg )
		{
			int stat = arg.intValue();

			if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "muscle" ) )
			{
				return new ScriptValue( KoLCharacter.getBaseMuscle() );
			}
			if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
			{
				return new ScriptValue( KoLCharacter.getBaseMysticality() );
			}
			if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "moxie" ) )
			{
				return new ScriptValue( KoLCharacter.getBaseMoxie() );
			}

			throw new RuntimeException( "Internal error: unknown stat" );
		}

		public ScriptValue my_buffedstat( final ScriptVariable arg )
		{
			int stat = arg.intValue();

			if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "muscle" ) )
			{
				return new ScriptValue( KoLCharacter.getAdjustedMuscle() );
			}
			if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
			{
				return new ScriptValue( KoLCharacter.getAdjustedMysticality() );
			}
			if ( Interpreter.STATS[ stat ].equalsIgnoreCase( "moxie" ) )
			{
				return new ScriptValue( KoLCharacter.getAdjustedMoxie() );
			}

			throw new RuntimeException( "Internal error: unknown stat" );
		}

		public ScriptValue my_meat()
		{
			return new ScriptValue( KoLCharacter.getAvailableMeat() );
		}

		public ScriptValue my_adventures()
		{
			return new ScriptValue( KoLCharacter.getAdventuresLeft() );
		}

		public ScriptValue my_turncount()
		{
			return new ScriptValue( KoLCharacter.getCurrentRun() );
		}

		public ScriptValue my_fullness()
		{
			return new ScriptValue( KoLCharacter.getFullness() );
		}

		public ScriptValue fullness_limit()
		{
			return new ScriptValue( KoLCharacter.getFullnessLimit() );
		}

		public ScriptValue my_inebriety()
		{
			return new ScriptValue( KoLCharacter.getInebriety() );
		}

		public ScriptValue inebriety_limit()
		{
			return new ScriptValue( KoLCharacter.getInebrietyLimit() );
		}

		public ScriptValue my_spleen_use()
		{
			return new ScriptValue( KoLCharacter.getSpleenUse() );
		}

		public ScriptValue spleen_limit()
		{
			return new ScriptValue( KoLCharacter.getSpleenLimit() );
		}

		public ScriptValue can_eat()
		{
			return new ScriptValue( KoLCharacter.canEat() );
		}

		public ScriptValue can_drink()
		{
			return new ScriptValue( KoLCharacter.canDrink() );
		}

		public ScriptValue turns_played()
		{
			return new ScriptValue( KoLCharacter.getCurrentRun() );
		}

		public ScriptValue can_interact()
		{
			return new ScriptValue( KoLCharacter.canInteract() );
		}

		public ScriptValue in_hardcore()
		{
			return new ScriptValue( KoLCharacter.isHardcore() );
		}

		// Basic skill and effect functions, including those used
		// in custom combat consult scripts.

		public ScriptValue have_skill( final ScriptVariable arg )
		{
			return new ScriptValue( KoLCharacter.hasSkill( arg.intValue() ) );
		}

		public ScriptValue mp_cost( final ScriptVariable skill )
		{
			return new ScriptValue( SkillDatabase.getMPConsumptionById( skill.intValue() ) );
		}

		public ScriptValue turns_per_cast( final ScriptVariable skill )
		{
			return new ScriptValue( SkillDatabase.getEffectDuration( skill.intValue() ) );
		}

		public ScriptValue have_effect( final ScriptVariable arg )
		{
			List potentialEffects = EffectDatabase.getMatchingNames( arg.toStringValue().toString() );
			AdventureResult effect =
				potentialEffects.isEmpty() ? null : new AdventureResult( (String) potentialEffects.get( 0 ), 0, true );
			return new ScriptValue( effect == null ? 0 : effect.getCount( KoLConstants.activeEffects ) );
		}

		public ScriptValue use_skill( final ScriptVariable count, final ScriptVariable skill )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( SkillDatabase.isCombat( skill.intValue() ) )
			{
				for ( int i = 0; i < count.intValue() && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
				{
					this.use_skill( skill );
				}

				return Interpreter.TRUE_VALUE;
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
		}

		public ScriptValue use_skill( final ScriptVariable skill )
		{
			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( SkillDatabase.isCombat( skill.intValue() ) )
			{
				return this.visit_url( "fight.php?action=skill&whichskill=" + skill.intValue() );
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast 1 " + skill.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate );
		}

		public ScriptValue use_skill( final ScriptVariable count, final ScriptVariable skill,
			final ScriptVariable target )
		{
			if ( count.intValue() <= 0 )
			{
				return this.continueValue();
			}

			// Just in case someone assumed that use_skill would also work
			// in combat, go ahead and allow it here.

			if ( SkillDatabase.isCombat( skill.intValue() ) )
			{
				for ( int i = 0; i < count.intValue() && FightRequest.INSTANCE.getAdventuresUsed() == 0; ++i )
				{
					this.use_skill( skill );
				}

				return Interpreter.TRUE_VALUE;
			}

			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() + " on " + target.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
		}

		public ScriptValue attack()
		{
			return this.visit_url( "fight.php?action=attack" );
		}

		public ScriptValue steal()
		{
			if ( !FightRequest.wonInitiative() )
			{
				return this.attack();
			}

			return this.visit_url( "fight.php?action=steal" );
		}

		public ScriptValue runaway()
		{
			return this.visit_url( "fight.php?action=runaway" );
		}

		public ScriptValue throw_item( final ScriptVariable item )
		{
			return this.visit_url( "fight.php?action=useitem&whichitem=" + item.intValue() );
		}

		public ScriptValue throw_items( final ScriptVariable item1, final ScriptVariable item2 )
		{
			return this.visit_url( "fight.php?action=useitem&whichitem=" + item1.intValue() + "&whichitem2=" + item2.intValue() );
		}

		public ScriptValue run_combat()
		{
			RequestThread.postRequest( FightRequest.INSTANCE );
			String response =
				KoLmafiaASH.relayScript == null ? FightRequest.lastResponseText : FightRequest.getNextTrackedRound();

			return new ScriptValue( Interpreter.BUFFER_TYPE, "", new StringBuffer( response == null ? "" : response ) );
		}

		// Equipment functions.

		public ScriptValue can_equip( final ScriptVariable item )
		{
			return new ScriptValue( EquipmentDatabase.canEquip( ItemDatabase.getItemName( item.intValue() ) ) );
		}

		public ScriptValue equip( final ScriptVariable item )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "equip " + item.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue equip( final ScriptVariable slot, final ScriptVariable item )
		{
			if ( item.getValue().equals( Interpreter.ITEM_INIT ) )
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "unequip " + slot.toStringValue() );
			}
			else
			{
				KoLmafiaCLI.DEFAULT_SHELL.executeLine( "equip " + slot.toStringValue() + " " + item.toStringValue() );
			}

			return this.continueValue();
		}

		public ScriptValue equipped_item( final ScriptVariable slot )
		{
			return Interpreter.makeItemValue( KoLCharacter.getEquipment( slot.intValue() ).getName() );
		}

		public ScriptValue have_equipped( final ScriptVariable item )
		{
			return KoLCharacter.hasEquipped( new AdventureResult( item.intValue(), 1 ) ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue outfit( final ScriptVariable outfit )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "outfit " + outfit.toStringValue().toString() );
			return this.continueValue();
		}

		public ScriptValue have_outfit( final ScriptVariable outfit )
		{
			SpecialOutfit so = KoLmafiaCLI.getMatchingOutfit( outfit.toStringValue().toString() );

			if ( so == null )
			{
				return Interpreter.FALSE_VALUE;
			}

			return EquipmentDatabase.hasOutfit( so.getOutfitId() ) ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue weapon_hands( final ScriptVariable item )
		{
			return new ScriptValue( EquipmentDatabase.getHands( item.intValue() ) );
		}

		public ScriptValue weapon_type( final ScriptVariable item )
		{
			String type = EquipmentDatabase.getType( item.intValue() );
			return new ScriptValue( type == null ? "unknown" : type );
		}

		public ScriptValue ranged_weapon( final ScriptVariable item )
		{
			return new ScriptValue( EquipmentDatabase.isRanged( item.intValue() ) );
		}

		public ScriptValue get_power( final ScriptVariable item )
		{
			return new ScriptValue( EquipmentDatabase.getPower( item.intValue() ) );
		}

		public ScriptValue my_familiar()
		{
			return Interpreter.makeFamiliarValue( KoLCharacter.getFamiliar().getId() );
		}

		public ScriptValue have_familiar( final ScriptVariable familiar )
		{
			return new ScriptValue( KoLCharacter.findFamiliar( familiar.toStringValue().toString() ) != null );
		}

		public ScriptValue use_familiar( final ScriptVariable familiar )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "familiar " + familiar.toStringValue() );
			return this.continueValue();
		}

		public ScriptValue familiar_equipment( final ScriptVariable familiar )
		{
			return Interpreter.parseItemValue( FamiliarDatabase.getFamiliarItem( familiar.intValue() ) );
		}

		public ScriptValue familiar_weight( final ScriptVariable familiar )
		{
			FamiliarData fam = KoLCharacter.findFamiliar( familiar.toStringValue().toString() );
			return new ScriptValue( fam == null ? 0 : fam.getWeight() );
		}

		// Random other functions related to current in-game
		// state, not directly tied to the character.

		public ScriptValue council()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "council" );
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue current_mcd()
		{
			return new ScriptValue( KoLCharacter.getSignedMLAdjustment() );
		}

		public ScriptValue change_mcd( final ScriptVariable level )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "mind-control " + level.intValue() );
			return this.continueValue();
		}

		public ScriptValue have_chef()
		{
			return new ScriptValue( KoLCharacter.hasChef() );
		}

		public ScriptValue have_bartender()
		{
			return new ScriptValue( KoLCharacter.hasBartender() );
		}

		// String parsing functions.

		public ScriptValue contains_text( final ScriptVariable source, final ScriptVariable search )
		{
			return new ScriptValue(
				source.toStringValue().toString().indexOf( search.toStringValue().toString() ) != -1 );
		}

		public ScriptValue extract_meat( final ScriptVariable string )
		{
			ArrayList data = new ArrayList();
			StaticEntity.getClient().processResults( string.toStringValue().toString(), data );

			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get( i );
				if ( result.getName().equals( AdventureResult.MEAT ) )
				{
					return new ScriptValue( result.getCount() );
				}
			}

			return new ScriptValue( 0 );
		}

		public ScriptValue extract_items( final ScriptVariable string )
		{
			ArrayList data = new ArrayList();
			StaticEntity.getClient().processResults( string.toStringValue().toString(), data );
			ScriptMap value = new ScriptMap( Interpreter.RESULT_TYPE );

			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get( i );
				if ( result.isItem() )
				{
					value.aset(
						Interpreter.parseItemValue( result.getName() ),
						Interpreter.parseIntValue( String.valueOf( result.getCount() ) ) );
				}
			}

			return value;
		}

		public ScriptValue length( final ScriptVariable string )
		{
			return new ScriptValue( string.toStringValue().toString().length() );
		}

		public ScriptValue index_of( final ScriptVariable source, final ScriptVariable search )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			return new ScriptValue( string.indexOf( substring ) );
		}

		public ScriptValue index_of( final ScriptVariable source, final ScriptVariable search,
			final ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.indexOf( substring, begin ) );
		}

		public ScriptValue last_index_of( final ScriptVariable source, final ScriptVariable search )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			return new ScriptValue( string.lastIndexOf( substring ) );
		}

		public ScriptValue last_index_of( final ScriptVariable source, final ScriptVariable search,
			final ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			String substring = search.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.lastIndexOf( substring, begin ) );
		}

		public ScriptValue substring( final ScriptVariable source, final ScriptVariable start )
		{
			String string = source.toStringValue().toString();
			int begin = start.intValue();
			return new ScriptValue( string.substring( begin ) );
		}

		public ScriptValue substring( final ScriptVariable source, final ScriptVariable start,
			final ScriptVariable finish )
		{
			String string = source.toStringValue().toString();
			int begin = start.intValue();
			int end = finish.intValue();
			return new ScriptValue( string.substring( begin, end ) );
		}

		public ScriptValue to_upper_case( final ScriptVariable string )
		{
			return Interpreter.parseStringValue( string.toStringValue().toString().toUpperCase() );
		}

		public ScriptValue to_lower_case( final ScriptVariable string )
		{
			return Interpreter.parseStringValue( string.toStringValue().toString().toLowerCase() );
		}

		public ScriptValue append( final ScriptVariable buffer, final ScriptVariable s )
		{
			StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
			current.append( s.toStringValue().toString() );
			return buffer.getValue();
		}

		public ScriptValue insert( final ScriptVariable buffer, final ScriptVariable index, final ScriptVariable s )
		{
			StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
			current.insert( index.intValue(), s.toStringValue().toString() );
			return buffer.getValue();
		}

		public ScriptValue replace( final ScriptVariable buffer, final ScriptVariable start, final ScriptVariable end,
			final ScriptVariable s )
		{
			StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
			current.replace( start.intValue(), end.intValue(), s.toStringValue().toString() );
			return buffer.getValue();
		}

		public ScriptValue delete( final ScriptVariable buffer, final ScriptVariable start, final ScriptVariable end )
		{
			StringBuffer current = (StringBuffer) buffer.getValue().rawValue();
			current.delete( start.intValue(), end.intValue() );
			return buffer.getValue();
		}

		public ScriptValue append_tail( final ScriptVariable matcher, final ScriptVariable current )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			StringBuffer buffer = (StringBuffer) current.getValue().rawValue();
			m.appendTail( buffer );
			return current.getValue();
		}

		public ScriptValue append_replacement( final ScriptVariable matcher, final ScriptVariable current,
			final ScriptVariable replacement )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			StringBuffer buffer = (StringBuffer) current.getValue().rawValue();
			m.appendReplacement( buffer, replacement.toStringValue().toString() );
			return matcher.getValue();
		}

		public ScriptValue create_matcher( final ScriptVariable pattern, final ScriptVariable string )
		{
			return new ScriptValue( Interpreter.MATCHER_TYPE, pattern.toStringValue().toString(), Pattern.compile(
				pattern.toStringValue().toString(), Pattern.DOTALL ).matcher( string.toStringValue().toString() ) );
		}

		public ScriptValue find( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return m.find() ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue start( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.start() );
		}

		public ScriptValue end( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.end() );
		}

		public ScriptValue group( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.group() );
		}

		public ScriptValue group( final ScriptVariable matcher, final ScriptVariable group )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.group( group.intValue() ) );
		}

		public ScriptValue group_count( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.groupCount() );
		}

		public ScriptValue replace_first( final ScriptVariable matcher, final ScriptVariable replacement )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.replaceFirst( replacement.toStringValue().toString() ) );
		}

		public ScriptValue replace_all( final ScriptVariable matcher, final ScriptVariable replacement )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			return new ScriptValue( m.replaceAll( replacement.toStringValue().toString() ) );
		}

		public ScriptValue reset( final ScriptVariable matcher )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			m.reset();
			return matcher.getValue();
		}

		public ScriptValue reset( final ScriptVariable matcher, final ScriptVariable input )
		{
			Matcher m = (Matcher) matcher.getValue().rawValue();
			m.reset( input.toStringValue().toString() );
			return matcher.getValue();
		}

		public ScriptValue replace_string( final ScriptVariable source, final ScriptVariable search,
			final ScriptVariable replace )
		{
			StringBuffer buffer;
			ScriptValue returnValue;

			if ( source.getValue().rawValue() instanceof StringBuffer )
			{
				buffer = (StringBuffer) source.getValue().rawValue();
				returnValue = source.getValue();
			}
			else
			{
				buffer = new StringBuffer( source.toStringValue().toString() );
				returnValue = new ScriptValue( Interpreter.BUFFER_TYPE, "", buffer );
			}

			StaticEntity.globalStringReplace(
				buffer, search.toStringValue().toString(), replace.toStringValue().toString() );
			return returnValue;
		}

		public ScriptValue split_string( final ScriptVariable string )
		{
			String[] pieces = string.toStringValue().toString().split( KoLConstants.LINE_BREAK );

			ScriptAggregateType type = new ScriptAggregateType( Interpreter.STRING_TYPE, pieces.length );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < pieces.length; ++i )
			{
				value.aset( new ScriptValue( i ), Interpreter.parseStringValue( pieces[ i ] ) );
			}

			return value;
		}

		public ScriptValue split_string( final ScriptVariable string, final ScriptVariable regex )
		{
			String[] pieces = string.toStringValue().toString().split( regex.toStringValue().toString() );

			ScriptAggregateType type = new ScriptAggregateType( Interpreter.STRING_TYPE, pieces.length );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < pieces.length; ++i )
			{
				value.aset( new ScriptValue( i ), Interpreter.parseStringValue( pieces[ i ] ) );
			}

			return value;
		}

		public ScriptValue group_string( final ScriptVariable string, final ScriptVariable regex )
		{
			Matcher userPatternMatcher =
				Pattern.compile( regex.toStringValue().toString() ).matcher( string.toStringValue().toString() );
			ScriptMap value = new ScriptMap( Interpreter.REGEX_GROUP_TYPE );

			int matchCount = 0;
			int groupCount = userPatternMatcher.groupCount();

			ScriptValue[] groupIndexes = new ScriptValue[ groupCount + 1 ];
			for ( int i = 0; i <= groupCount; ++i )
			{
				groupIndexes[ i ] = new ScriptValue( i );
			}

			ScriptValue matchIndex;
			ScriptCompositeValue slice;

			try
			{
				while ( userPatternMatcher.find() )
				{
					matchIndex = new ScriptValue( matchCount );
					slice = (ScriptCompositeValue) value.initialValue( matchIndex );

					value.aset( matchIndex, slice );
					for ( int i = 0; i <= groupCount; ++i )
					{
						slice.aset( groupIndexes[ i ], Interpreter.parseStringValue( userPatternMatcher.group( i ) ) );
					}

					++matchCount;
				}
			}
			catch ( Exception e )
			{
				// Because we're doing everything ourselves, this
				// error shouldn't get generated.  Print a stack
				// trace, just in case.

				StaticEntity.printStackTrace( e );
			}

			return value;
		}

		public ScriptValue chat_reply( final ScriptVariable string )
		{
			String recipient = ChatManager.lastBlueMessage();
			if ( !recipient.equals( "" ) )
			{
				RequestThread.postRequest( new ChatRequest( recipient, string.toStringValue().toString(), false ) );
			}

			return Interpreter.VOID_VALUE;
		}

		// Quest completion functions.

		public ScriptValue entryway()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "entryway" );
			return this.continueValue();
		}

		public ScriptValue hedgemaze()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "hedgemaze" );
			return this.continueValue();
		}

		public ScriptValue guardians()
		{
			int itemId = SorceressLairManager.fightTowerGuardians( true );
			return Interpreter.makeItemValue( itemId );
		}

		public ScriptValue chamber()
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "chamber" );
			return this.continueValue();
		}

		public ScriptValue tavern()
		{
			int result = StaticEntity.getClient().locateTavernFaucet();
			return new ScriptValue( KoLmafia.permitsContinue() ? result : -1 );
		}

		// Arithmetic utility functions.

		public ScriptValue random( final ScriptVariable arg )
		{
			int range = arg.intValue();
			if ( range < 2 )
			{
				throw new RuntimeException( "Random range must be at least 2" );
			}
			return new ScriptValue( KoLConstants.RNG.nextInt( range ) );
		}

		public ScriptValue round( final ScriptVariable arg )
		{
			return new ScriptValue( (int) Math.round( arg.floatValue() ) );
		}

		public ScriptValue truncate( final ScriptVariable arg )
		{
			return new ScriptValue( (int) arg.floatValue() );
		}

		public ScriptValue floor( final ScriptVariable arg )
		{
			return new ScriptValue( (int) Math.floor( arg.floatValue() ) );
		}

		public ScriptValue ceil( final ScriptVariable arg )
		{
			return new ScriptValue( (int) Math.ceil( arg.floatValue() ) );
		}

		public ScriptValue square_root( final ScriptVariable val )
		{
			return new ScriptValue( (float) Math.sqrt( val.floatValue() ) );
		}

		// Settings-type functions.

		public ScriptValue url_encode( final ScriptVariable arg )
			throws UnsupportedEncodingException
		{
			return new ScriptValue( URLEncoder.encode( arg.toStringValue().toString(), "UTF-8" ) );
		}

		public ScriptValue url_decode( final ScriptVariable arg )
			throws UnsupportedEncodingException
		{
			return new ScriptValue( URLDecoder.decode( arg.toStringValue().toString(), "UTF-8" ) );
		}

		public ScriptValue get_property( final ScriptVariable name )
		{
			String property = name.toStringValue().toString();
			return !Preferences.isUserEditable( property ) ? Interpreter.STRING_INIT : new ScriptValue(
				Preferences.getString( property ) );
		}

		public ScriptValue set_property( final ScriptVariable name, final ScriptVariable value )
		{
			// In order to avoid code duplication for combat
			// related settings, use the shell.

			KoLmafiaCLI.DEFAULT_SHELL.executeCommand(
				"set", name.toStringValue().toString() + "=" + value.toStringValue().toString() );
			return Interpreter.VOID_VALUE;
		}

		// Functions for aggregates.

		public ScriptValue count( final ScriptVariable arg )
		{
			return new ScriptValue( arg.getValue().count() );
		}

		public ScriptValue clear( final ScriptVariable arg )
		{
			arg.getValue().clear();
			return Interpreter.VOID_VALUE;
		}

		public ScriptValue file_to_map( final ScriptVariable var1, final ScriptVariable var2 )
		{
			String filename = var1.toStringValue().toString();
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue();
			return this.readMap( filename, map_variable, true );
		}

		public ScriptValue file_to_map( final ScriptVariable var1, final ScriptVariable var2, final ScriptVariable var3 )
		{
			String filename = var1.toStringValue().toString();
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var2.getValue();
			boolean compact = var3.intValue() == 1;
			return this.readMap( filename, map_variable, compact );
		}

		private ScriptValue readMap( final String filename, final ScriptCompositeValue result, final boolean compact )
		{
			BufferedReader reader = this.getReader( filename );
			if ( reader == null )
			{
				return Interpreter.FALSE_VALUE;
			}

			String[] data = null;
			result.clear();

			try
			{
				while ( ( data = KoLDatabase.readData( reader ) ) != null )
				{
					if ( data.length > 1 )
					{
						result.read( data, 0, compact );
					}
				}
			}
			catch ( Exception e )
			{
				// Okay, runtime error. Indicate that there was
				// a bad currentLine in the data file and print the
				// stack trace.

				StringBuffer buffer = new StringBuffer( "Invalid line in data file:" );
				if ( data != null )
				{
					buffer.append( KoLConstants.LINE_BREAK );

					for ( int i = 0; i < data.length; ++i )
					{
						buffer.append( '\t' );
						buffer.append( data[ i ] );
					}
				}

				StaticEntity.printStackTrace( e, buffer.toString() );
				return Interpreter.FALSE_VALUE;
			}

			try
			{
				reader.close();
			}
			catch ( Exception e )
			{
			}

			return Interpreter.TRUE_VALUE;
		}

		public ScriptValue map_to_file( final ScriptVariable var1, final ScriptVariable var2 )
		{
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue();
			String filename = var2.toStringValue().toString();
			return this.printMap( map_variable, filename, true );
		}

		public ScriptValue map_to_file( final ScriptVariable var1, final ScriptVariable var2, final ScriptVariable var3 )
		{
			ScriptCompositeValue map_variable = (ScriptCompositeValue) var1.getValue();
			String filename = var2.toStringValue().toString();
			boolean compact = var3.intValue() == 1;
			return this.printMap( map_variable, filename, compact );
		}

		private ScriptValue printMap( final ScriptCompositeValue map_variable, final String filename,
			final boolean compact )
		{
			if ( filename.startsWith( "http" ) )
			{
				return Interpreter.FALSE_VALUE;
			}

			PrintStream writer = null;
			File output = this.getFile( filename );

			if ( output == null )
			{
				return Interpreter.FALSE_VALUE;
			}

			writer = LogStream.openStream( output, true );
			map_variable.dump( writer, "", compact );
			writer.close();

			return Interpreter.TRUE_VALUE;
		}

		// Custom combat helper functions.

		public ScriptValue my_location()
		{
			String location = Preferences.getString( "lastAdventure" );
			return location.equals( "" ) ? Interpreter.parseLocationValue( "Rest" ) : Interpreter.parseLocationValue( location );
		}

		public ScriptValue get_monsters( final ScriptVariable location )
		{
			KoLAdventure adventure = (KoLAdventure) location.rawValue();
			AreaCombatData data = adventure.getAreaSummary();

			int monsterCount = data == null ? 0 : data.getMonsterCount();

			ScriptAggregateType type = new ScriptAggregateType( Interpreter.MONSTER_TYPE, monsterCount );
			ScriptArray value = new ScriptArray( type );

			for ( int i = 0; i < monsterCount; ++i )
			{
				value.aset( new ScriptValue( i ), Interpreter.parseMonsterValue( data.getMonster( i ).getName() ) );
			}

			return value;

		}

		public ScriptValue expected_damage()
		{
			// http://kol.coldfront.net/thekolwiki/index.php/Damage

			int baseValue =
				Math.max( 0, FightRequest.getMonsterAttack() - KoLCharacter.getAdjustedMoxie() ) + FightRequest.getMonsterAttack() / 4 - KoLCharacter.getDamageReduction();

			float damageAbsorb =
				1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
			float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() );
			return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
		}

		public ScriptValue expected_damage( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return Interpreter.ZERO_VALUE;
			}

			// http://kol.coldfront.net/thekolwiki/index.php/Damage

			int baseValue =
				Math.max( 0, monster.getAttack() - KoLCharacter.getAdjustedMoxie() ) + FightRequest.getMonsterAttack() / 4 - KoLCharacter.getDamageReduction();

			float damageAbsorb =
				1.0f - ( (float) Math.sqrt( Math.min( 1000, KoLCharacter.getDamageAbsorption() ) / 10.0f ) - 1.0f ) / 10.0f;
			float elementAbsorb = 1.0f - KoLCharacter.getElementalResistance( monster.getAttackElement() );
			return new ScriptValue( (int) Math.ceil( baseValue * damageAbsorb * elementAbsorb ) );
		}

		public ScriptValue monster_level_adjustment()
		{
			return new ScriptValue( KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue weight_adjustment()
		{
			return new ScriptValue( KoLCharacter.getFamiliarWeightAdjustment() );
		}

		public ScriptValue mana_cost_modifier()
		{
			return new ScriptValue( KoLCharacter.getManaCostAdjustment() );
		}

		public ScriptValue raw_damage_absorption()
		{
			return new ScriptValue( KoLCharacter.getDamageAbsorption() );
		}

		public ScriptValue damage_absorption_percent()
		{
			int raw = Math.min( 1000, KoLCharacter.getDamageAbsorption() );
			if ( raw == 0 )
			{
				return Interpreter.ZERO_FLOAT_VALUE;
			}

			// http://forums.kingdomofloathing.com/viewtopic.php?p=2016073
			// ( sqrt( raw / 10 ) - 1 ) / 10

			double percent = ( Math.sqrt( raw / 10.0 ) - 1.0 ) * 10.0;
			return new ScriptValue( (float) percent );
		}

		public ScriptValue damage_reduction()
		{
			return new ScriptValue( KoLCharacter.getDamageReduction() );
		}

		public ScriptValue elemental_resistance()
		{
			return new ScriptValue( KoLCharacter.getElementalResistance( FightRequest.getMonsterAttackElement() ) );
		}

		public ScriptValue elemental_resistance( final ScriptVariable arg )
		{
			if ( arg.getType().equals( Interpreter.TYPE_ELEMENT ) )
			{
				return new ScriptValue( KoLCharacter.getElementalResistance( arg.intValue() ) );
			}

			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return Interpreter.ZERO_VALUE;
			}

			return new ScriptValue( KoLCharacter.getElementalResistance( monster.getAttackElement() ) );
		}

		public ScriptValue combat_rate_modifier()
		{
			return new ScriptValue( KoLCharacter.getCombatRateAdjustment() );
		}

		public ScriptValue initiative_modifier()
		{
			return new ScriptValue( KoLCharacter.getInitiativeAdjustment() );
		}

		public ScriptValue experience_bonus()
		{
			return new ScriptValue( KoLCharacter.getExperienceAdjustment() );
		}

		public ScriptValue meat_drop_modifier()
		{
			return new ScriptValue( KoLCharacter.getMeatDropPercentAdjustment() );
		}

		public ScriptValue item_drop_modifier()
		{
			return new ScriptValue( KoLCharacter.getItemDropPercentAdjustment() );
		}

		public ScriptValue buffed_hit_stat()
		{
			int hitStat = KoLCharacter.getAdjustedHitStat();
			return new ScriptValue( hitStat );
		}

		public ScriptValue current_hit_stat()
		{
			return KoLCharacter.hitStat() == KoLConstants.MOXIE ? Interpreter.parseStatValue( "moxie" ) : Interpreter.parseStatValue( "muscle" );
		}

		public ScriptValue monster_element()
		{
			int element = FightRequest.getMonsterDefenseElement();
			return new ScriptValue( Interpreter.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
		}

		public ScriptValue monster_element( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return Interpreter.ELEMENT_INIT;
			}

			int element = monster.getDefenseElement();
			return new ScriptValue( Interpreter.ELEMENT_TYPE, element, MonsterDatabase.elementNames[ element ] );
		}

		public ScriptValue monster_attack()
		{
			return new ScriptValue( FightRequest.getMonsterAttack() );
		}

		public ScriptValue monster_attack( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return Interpreter.ZERO_VALUE;
			}

			return new ScriptValue( monster.getAttack() + KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue monster_defense()
		{
			return new ScriptValue( FightRequest.getMonsterDefense() );
		}

		public ScriptValue monster_defense( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return Interpreter.ZERO_VALUE;
			}

			return new ScriptValue( monster.getDefense() + KoLCharacter.getMonsterLevelAdjustment() );
		}

		public ScriptValue monster_hp()
		{
			return new ScriptValue( FightRequest.getMonsterHealth() );
		}

		public ScriptValue monster_hp( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			if ( monster == null )
			{
				return Interpreter.ZERO_VALUE;
			}

			return new ScriptValue( monster.getAdjustedHP( KoLCharacter.getMonsterLevelAdjustment() ) );
		}

		public ScriptValue item_drops()
		{
			Monster monster = FightRequest.getLastMonster();
			List data = monster == null ? new ArrayList() : monster.getItems();

			ScriptMap value = new ScriptMap( Interpreter.RESULT_TYPE );
			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get( i );
				value.aset(
					Interpreter.parseItemValue( result.getName() ),
					Interpreter.parseIntValue( String.valueOf( result.getCount() ) ) );
			}

			return value;
		}

		public ScriptValue item_drops( final ScriptVariable arg )
		{
			Monster monster = (Monster) arg.rawValue();
			List data = monster == null ? new ArrayList() : monster.getItems();

			ScriptMap value = new ScriptMap( Interpreter.RESULT_TYPE );
			AdventureResult result;

			for ( int i = 0; i < data.size(); ++i )
			{
				result = (AdventureResult) data.get( i );
				value.aset(
					Interpreter.parseItemValue( result.getName() ),
					Interpreter.parseIntValue( String.valueOf( result.getCount() ) ) );
			}

			return value;
		}

		public ScriptValue will_usually_dodge()
		{
			return FightRequest.willUsuallyDodge() ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue will_usually_miss()
		{
			return FightRequest.willUsuallyMiss() ? Interpreter.TRUE_VALUE : Interpreter.FALSE_VALUE;
		}

		public ScriptValue numeric_modifier( final ScriptVariable modifier )
		{
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( KoLCharacter.currentNumericModifier( mod ) );
		}

		public ScriptValue numeric_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( Modifiers.getNumericModifier( name, mod ) );
		}

		public ScriptValue boolean_modifier( final ScriptVariable modifier )
		{
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( KoLCharacter.currentBooleanModifier( mod ) );
		}

		public ScriptValue boolean_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( Modifiers.getBooleanModifier( name, mod ) );
		}

		public ScriptValue effect_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( Interpreter.parseEffectValue( Modifiers.getStringModifier( name, mod ) ) );
		}

		public ScriptValue class_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( Interpreter.parseClassValue( Modifiers.getStringModifier( name, mod ) ) );
		}

		public ScriptValue stat_modifier( final ScriptVariable arg, final ScriptVariable modifier )
		{
			String name = arg.toStringValue().toString();
			String mod = modifier.toStringValue().toString();
			return new ScriptValue( Interpreter.parseStatValue( Modifiers.getStringModifier( name, mod ) ) );
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

		public ScriptValue getValue()
		{
			if ( this.expression != null )
			{
				this.content = this.expression.execute();
			}

			return this.content;
		}

		public ScriptType getValueType()
		{
			return this.getValue().getType();
		}

		public Object rawValue()
		{
			return this.getValue().rawValue();
		}

		public int intValue()
		{
			return this.getValue().intValue();
		}

		public ScriptValue toStringValue()
		{
			return this.getValue().toStringValue();
		}

		public float floatValue()
		{
			return this.getValue().floatValue();
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

		public void setValue( final ScriptValue targetValue )
		{
			if ( this.getType().equals( targetValue.getType() ) )
			{
				this.content = targetValue;
				this.expression = null;
			}
			else if ( this.getType().equals( Interpreter.TYPE_STRING ) )
			{
				this.content = targetValue.toStringValue();
				this.expression = null;
			}
			else if ( this.getType().equals( Interpreter.TYPE_INT ) && targetValue.getType().equals(
				Interpreter.TYPE_FLOAT ) )
			{
				this.content = targetValue.toIntValue();
				this.expression = null;
			}
			else if ( this.getType().equals( Interpreter.TYPE_FLOAT ) && targetValue.getType().equals(
				Interpreter.TYPE_INT ) )
			{
				this.content = targetValue.toFloatValue();
				this.expression = null;
			}
			else if ( this.getType().equals( Interpreter.TYPE_ANY ) )
			{
				this.content = targetValue;
				this.expression = null;
			}
			else if ( this.getType().getBaseType().equals( Interpreter.TYPE_AGGREGATE ) && targetValue.getType().getBaseType().equals(
				Interpreter.TYPE_AGGREGATE ) )
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

		public ScriptValue execute()
		{
			return this.target.getValue();
		}

		public ScriptValue getValue()
		{
			return this.target.getValue();
		}

		public void forceValue( final ScriptValue targetValue )
		{
			this.target.forceValue( targetValue );
		}

		public void setValue( final ScriptValue targetValue )
		{
			this.target.setValue( targetValue );
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

		public ScriptValue execute()
		{
			return this.getValue();
		}

		// Evaluate all the indices and step through the slices.
		//
		// When done, this.slice has the final slice and this.index has
		// the final evaluated index.

		private boolean getSlice()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return false;
			}

			this.slice = (ScriptCompositeValue) this.target.getValue();
			this.index = null;

			Interpreter.traceIndent();
			Interpreter.trace( "AREF: " + this.slice.toString() );

			int count = this.indices.size();
			for ( int i = 0; i < count; ++i )
			{
				ScriptExpression exp = (ScriptExpression) this.indices.get( i );

				Interpreter.traceIndent();
				Interpreter.trace( "Key #" + ( i + 1 ) + ": " + exp.toQuotedString() );

				this.index = exp.execute();
				Interpreter.captureValue( this.index );
				if ( this.index == null )
				{
					this.index = Interpreter.VOID_VALUE;
				}

				Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + this.index.toQuotedString() );
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

		public ScriptValue getValue()
		{
			// Iterate through indices to final slice
			if ( this.getSlice() )
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

		public void setValue( final ScriptValue targetValue )
		{
			// Iterate through indices to final slice
			if ( this.getSlice() )
			{
				this.slice.aset( this.index, targetValue );
				Interpreter.traceIndent();
				Interpreter.trace( "ASET: " + targetValue.toQuotedString() );
				Interpreter.traceUnindent();
			}
		}

		public ScriptValue removeKey()
		{
			// Iterate through indices to final slice
			if ( this.getSlice() )
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

		public boolean contains( final ScriptValue index )
		{
			boolean result = false;
			// Iterate through indices to final slice
			if ( this.getSlice() )
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
		public ScriptValue execute()
		{
			return null;
		}

		public void print( final PrintStream stream, final int indent )
		{
			ParseTree.indentLine( stream, indent );
			stream.println( "<COMMAND " + this + ">" );
		}
	}

	public static class ScriptFlowControl
		extends ScriptCommand
	{
		int command;

		public ScriptFlowControl( final String command )
		{
			if ( command.equalsIgnoreCase( "break" ) )
			{
				this.command = Interpreter.COMMAND_BREAK;
			}
			else if ( command.equalsIgnoreCase( "continue" ) )
			{
				this.command = Interpreter.COMMAND_CONTINUE;
			}
			else if ( command.equalsIgnoreCase( "exit" ) )
			{
				this.command = Interpreter.COMMAND_EXIT;
			}
			else
			{
				throw new AdvancedScriptException( "Invalid command '" + command + "'" );
			}
		}

		public ScriptFlowControl( final int command )
		{
			this.command = command;
		}

		public String toString()
		{
			switch ( this.command )
			{
			case Interpreter.COMMAND_BREAK:
				return "break";
			case Interpreter.COMMAND_CONTINUE:
				return "continue";
			case Interpreter.COMMAND_EXIT:
				return "exit";
			}
			return "unknown command";
		}

		public ScriptValue execute()
		{
			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );
			Interpreter.traceUnindent();

			switch ( this.command )
			{
			case Interpreter.COMMAND_BREAK:
				Interpreter.currentState = Interpreter.STATE_BREAK;
				return Interpreter.VOID_VALUE;
			case Interpreter.COMMAND_CONTINUE:
				Interpreter.currentState = Interpreter.STATE_CONTINUE;
				return Interpreter.VOID_VALUE;
			case Interpreter.COMMAND_EXIT:
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			throw new RuntimeException( "Internal error: unknown ScriptCommand type" );
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
				if ( !Interpreter.validCoercion( expectedType, returnValue.getType(), "return" ) )
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
				return Interpreter.VOID_TYPE;
			}

			return this.returnValue.getType();
		}

		public ScriptExpression getExpression()
		{
			return this.returnValue;
		}

		public ScriptValue execute()
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

			ScriptValue result = this.returnValue.execute();
			Interpreter.captureValue( result );

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

			if ( this.expectedType.equals( Interpreter.TYPE_STRING ) )
			{
				return result.toStringValue();
			}

			if ( this.expectedType.equals( Interpreter.TYPE_FLOAT ) )
			{
				return result.toFloatValue();
			}

			if ( this.expectedType.equals( Interpreter.TYPE_INT ) )
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
			if ( !this.getType().equals( Interpreter.TYPE_VOID ) )
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
			if ( !condition.getType().equals( Interpreter.TYPE_BOOLEAN ) )
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

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );

			Interpreter.trace( "Test: " + this.condition );

			ScriptValue conditionResult = this.condition.execute();
			Interpreter.captureValue( conditionResult );

			Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + conditionResult );

			if ( conditionResult == null )
			{
				Interpreter.traceUnindent();
				return null;
			}

			if ( conditionResult.intValue() == 1 )
			{
				ScriptValue result = this.scope.execute();

				Interpreter.traceUnindent();

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
				{
					return result;
				}

				return Interpreter.TRUE_VALUE;
			}

			Interpreter.traceUnindent();
			return Interpreter.FALSE_VALUE;
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

		public ScriptValue execute()
		{
			ScriptValue result = super.execute();
			if ( Interpreter.currentState != Interpreter.STATE_NORMAL || result == Interpreter.TRUE_VALUE )
			{
				return result;
			}

			// Conditional failed. Move to else clauses

			Iterator it = this.elseLoops.iterator();
			ScriptConditional elseLoop;

			while ( it.hasNext() )
			{
				elseLoop = (ScriptConditional) it.next();
				result = elseLoop.execute();

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL || result == Interpreter.TRUE_VALUE )
				{
					return result;
				}
			}

			return Interpreter.FALSE_VALUE;
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

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( "else" );
			ScriptValue result = this.scope.execute();
			Interpreter.traceUnindent();

			if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
			{
				return result;
			}

			return Interpreter.TRUE_VALUE;
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

		public ScriptValue execute()
		{
			ScriptValue result = this.scope.execute();

			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
			}

			switch ( Interpreter.currentState )
			{
			case Interpreter.STATE_EXIT:
				return null;

			case Interpreter.STATE_BREAK:
				// Stay in state; subclass exits loop
				return Interpreter.VOID_VALUE;

			case Interpreter.STATE_RETURN:
				// Stay in state; subclass exits loop
				return result;

			case Interpreter.STATE_CONTINUE:
				// Done with this iteration
				Interpreter.currentState = Interpreter.STATE_NORMAL;
				return result;

			case Interpreter.STATE_NORMAL:
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

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			Interpreter.traceIndent();
			Interpreter.trace( this.toString() );

			// Evaluate the aggref to get the slice
			ScriptAggregateValue slice = (ScriptAggregateValue) this.aggregate.execute();
			Interpreter.captureValue( slice );
			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// Iterate over the slice with bound keyvar

			Iterator it = this.getReferences();
			return this.executeSlice( slice, it, (ScriptVariableReference) it.next() );

		}

		private ScriptValue executeSlice( final ScriptAggregateValue slice, final Iterator it,
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
				variable.setValue( key );

				Interpreter.trace( "Key #" + i + ": " + key );

				// If there are more indices to bind, recurse
				ScriptValue result;
				if ( nextVariable != null )
				{
					ScriptAggregateValue nextSlice = (ScriptAggregateValue) slice.aref( key );
					Interpreter.traceIndent();
					result = this.executeSlice( nextSlice, it, nextVariable );
				}
				else
				{
					// Otherwise, execute scope
					result = super.execute();
				}
				switch ( Interpreter.currentState )
				{
				case Interpreter.STATE_NORMAL:
					break;
				case Interpreter.STATE_BREAK:
					Interpreter.currentState = Interpreter.STATE_NORMAL;
					// Fall through
				default:
					Interpreter.traceUnindent();
					return result;
				}
			}

			Interpreter.traceUnindent();
			return Interpreter.VOID_VALUE;
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
			if ( !condition.getType().equals( Interpreter.TYPE_BOOLEAN ) )
			{
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean" );
			}

		}

		public ScriptExpression getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute()
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

				ScriptValue conditionResult = this.condition.execute();
				Interpreter.captureValue( conditionResult );

				Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + conditionResult );

				if ( conditionResult == null )
				{
					Interpreter.traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() != 1 )
				{
					break;
				}

				ScriptValue result = super.execute();

				if ( Interpreter.currentState == Interpreter.STATE_BREAK )
				{
					Interpreter.currentState = Interpreter.STATE_NORMAL;
					Interpreter.traceUnindent();
					return Interpreter.VOID_VALUE;
				}

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
				{
					Interpreter.traceUnindent();
					return result;
				}
			}

			Interpreter.traceUnindent();
			return Interpreter.VOID_VALUE;
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
			if ( !condition.getType().equals( Interpreter.TYPE_BOOLEAN ) )
			{
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean" );
			}

		}

		public ScriptExpression getCondition()
		{
			return this.condition;
		}

		public ScriptValue execute()
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
				ScriptValue result = super.execute();

				if ( Interpreter.currentState == Interpreter.STATE_BREAK )
				{
					Interpreter.currentState = Interpreter.STATE_NORMAL;
					Interpreter.traceUnindent();
					return Interpreter.VOID_VALUE;
				}

				if ( Interpreter.currentState != Interpreter.STATE_NORMAL )
				{
					Interpreter.traceUnindent();
					return result;
				}

				Interpreter.trace( "Test: " + this.condition );

				ScriptValue conditionResult = this.condition.execute();
				Interpreter.captureValue( conditionResult );

				Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + conditionResult );

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
			return Interpreter.VOID_VALUE;
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

		public ScriptValue execute()
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

			ScriptValue initialValue = this.initial.execute();
			Interpreter.captureValue( initialValue );

			Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + initialValue );

			if ( initialValue == null )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// Get the final value
			Interpreter.trace( "Last: " + this.last );

			ScriptValue lastValue = this.last.execute();
			Interpreter.captureValue( lastValue );

			Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + lastValue );

			if ( lastValue == null )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// Get the increment
			Interpreter.trace( "Increment: " + this.increment );

			ScriptValue incrementValue = this.increment.execute();
			Interpreter.captureValue( incrementValue );

			Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + incrementValue );

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
				this.variable.setValue( new ScriptValue( current ) );

				// Execute the scope
				ScriptValue result = super.execute();

				if ( Interpreter.currentState == Interpreter.STATE_BREAK )
				{
					Interpreter.currentState = Interpreter.STATE_NORMAL;
					Interpreter.traceUnindent();
					return Interpreter.VOID_VALUE;
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
			return Interpreter.VOID_VALUE;
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
			return Interpreter.VOID_TYPE;
		}

		public ScriptValue execute()
		{
			KoLmafiaCLI script = new KoLmafiaCLI( this.data.getByteArrayInputStream() );
			script.listenForCommands();
			return Interpreter.VOID_VALUE;
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

		public ScriptValue execute()
		{
			if ( !KoLmafia.permitsContinue() )
			{
				Interpreter.currentState = Interpreter.STATE_EXIT;
				return null;
			}

			// Save current variable bindings
			this.target.saveBindings();
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
					this.target.restoreBindings();
					throw new RuntimeException( "Internal error: illegal arguments" );
				}

				paramValue = (ScriptExpression) valIterator.next();

				Interpreter.trace( "Param #" + paramCount + ": " + paramValue.toQuotedString() );

				ScriptValue value = paramValue.execute();
				Interpreter.captureValue( value );
				if ( value == null )
				{
					value = Interpreter.VOID_VALUE;
				}

				Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + value.toQuotedString() );

				if ( Interpreter.currentState == Interpreter.STATE_EXIT )
				{
					this.target.restoreBindings();
					Interpreter.traceUnindent();
					return null;
				}

				// Bind parameter to new value
				if ( paramVarRef.getType().equals( Interpreter.TYPE_STRING ) )
				{
					paramVarRef.setValue( value.toStringValue() );
				}
				else if ( paramVarRef.getType().equals( Interpreter.TYPE_INT ) && paramValue.getType().equals(
					Interpreter.TYPE_FLOAT ) )
				{
					paramVarRef.setValue( value.toIntValue() );
				}
				else if ( paramVarRef.getType().equals( Interpreter.TYPE_FLOAT ) && paramValue.getType().equals(
					Interpreter.TYPE_INT ) )
				{
					paramVarRef.setValue( value.toFloatValue() );
				}
				else
				{
					paramVarRef.setValue( value );
				}
			}

			if ( valIterator.hasNext() )
			{
				this.target.restoreBindings();
				throw new RuntimeException( "Internal error: illegal arguments" );
			}

			Interpreter.trace( "Entering function " + this.target.getName() );
			ScriptValue result = this.target.execute();
			Interpreter.trace( "Function " + this.target.getName() + " returned: " + result );

			if ( Interpreter.currentState != Interpreter.STATE_EXIT )
			{
				Interpreter.currentState = Interpreter.STATE_NORMAL;
			}

			// Restore initial variable bindings
			this.target.restoreBindings();
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

			if ( rhs != null && !Interpreter.validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
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

		public ScriptValue execute()
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

				value = this.rhs.execute();
				Interpreter.captureValue( value );

				Interpreter.trace( "Set: " + value );
				Interpreter.traceUnindent();
			}

			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				return null;
			}

			if ( this.lhs.getType().equals( Interpreter.TYPE_STRING ) )
			{
				this.lhs.setValue( value.toStringValue() );
			}
			else if ( this.lhs.getType().equals( Interpreter.TYPE_INT ) )
			{
				this.lhs.setValue( value.toIntValue() );
			}
			else if ( this.lhs.getType().equals( Interpreter.TYPE_FLOAT ) )
			{
				this.lhs.setValue( value.toFloatValue() );
			}
			else
			{
				this.lhs.setValue( value );
			}

			return Interpreter.VOID_VALUE;
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
			case Interpreter.TYPE_VOID:
				return Interpreter.VOID_VALUE;
			case Interpreter.TYPE_BOOLEAN:
				return Interpreter.BOOLEAN_INIT;
			case Interpreter.TYPE_INT:
				return Interpreter.INT_INIT;
			case Interpreter.TYPE_FLOAT:
				return Interpreter.FLOAT_INIT;
			case Interpreter.TYPE_STRING:
				return Interpreter.STRING_INIT;
			case Interpreter.TYPE_BUFFER:
				return new ScriptValue( Interpreter.BUFFER_TYPE, "", new StringBuffer() );
			case Interpreter.TYPE_MATCHER:
				return new ScriptValue( Interpreter.MATCHER_TYPE, "", Pattern.compile( "" ).matcher( "" ) );

			case Interpreter.TYPE_ITEM:
				return Interpreter.ITEM_INIT;
			case Interpreter.TYPE_LOCATION:
				return Interpreter.LOCATION_INIT;
			case Interpreter.TYPE_CLASS:
				return Interpreter.CLASS_INIT;
			case Interpreter.TYPE_STAT:
				return Interpreter.STAT_INIT;
			case Interpreter.TYPE_SKILL:
				return Interpreter.SKILL_INIT;
			case Interpreter.TYPE_EFFECT:
				return Interpreter.EFFECT_INIT;
			case Interpreter.TYPE_FAMILIAR:
				return Interpreter.FAMILIAR_INIT;
			case Interpreter.TYPE_SLOT:
				return Interpreter.SLOT_INIT;
			case Interpreter.TYPE_MONSTER:
				return Interpreter.MONSTER_INIT;
			case Interpreter.TYPE_ELEMENT:
				return Interpreter.ELEMENT_INIT;
			}
			return null;
		}

		public ScriptValue parseValue( final String name )
		{
			switch ( this.type )
			{
			case Interpreter.TYPE_BOOLEAN:
				return Interpreter.parseBooleanValue( name );
			case Interpreter.TYPE_INT:
				return Interpreter.parseIntValue( name );
			case Interpreter.TYPE_FLOAT:
				return Interpreter.parseFloatValue( name );
			case Interpreter.TYPE_STRING:
				return Interpreter.parseStringValue( name );
			case Interpreter.TYPE_ITEM:
				return Interpreter.parseItemValue( name );
			case Interpreter.TYPE_LOCATION:
				return Interpreter.parseLocationValue( name );
			case Interpreter.TYPE_CLASS:
				return Interpreter.parseClassValue( name );
			case Interpreter.TYPE_STAT:
				return Interpreter.parseStatValue( name );
			case Interpreter.TYPE_SKILL:
				return Interpreter.parseSkillValue( name );
			case Interpreter.TYPE_EFFECT:
				return Interpreter.parseEffectValue( name );
			case Interpreter.TYPE_FAMILIAR:
				return Interpreter.parseFamiliarValue( name );
			case Interpreter.TYPE_SLOT:
				return Interpreter.parseSlotValue( name );
			case Interpreter.TYPE_MONSTER:
				return Interpreter.parseMonsterValue( name );
			case Interpreter.TYPE_ELEMENT:
				return Interpreter.parseElementValue( name );
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
			super( name, Interpreter.TYPE_TYPEDEF );
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
			super( "aggregate", Interpreter.TYPE_AGGREGATE );
			this.dataType = dataType;
			this.indexType = indexType;
			this.size = 0;
		}

		// Array
		public ScriptAggregateType( final ScriptType dataType, final int size )
		{
			super( "aggregate", Interpreter.TYPE_AGGREGATE );
			this.primitive = false;
			this.dataType = dataType;
			this.indexType = Interpreter.INT_TYPE;
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
			super( name, Interpreter.TYPE_RECORD );
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
			return Interpreter.STRING_TYPE;
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

			if ( type.equals( Interpreter.TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= this.fieldNames.length )
				{
					return null;
				}
				return this.fieldIndices[ index ];
			}

			if ( type.equals( Interpreter.TYPE_STRING ) )
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

			if ( type.equals( Interpreter.TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= this.fieldNames.length )
				{
					return -1;
				}
				return index;
			}

			if ( type.equals( Interpreter.TYPE_STRING ) )
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

		public ScriptValue execute()
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
			this.type = Interpreter.VOID_TYPE;
		}

		public ScriptValue( final int value )
		{
			this.type = Interpreter.INT_TYPE;
			this.contentInt = value;
		}

		public ScriptValue( final boolean value )
		{
			this.type = Interpreter.BOOLEAN_TYPE;
			this.contentInt = value ? 1 : 0;
		}

		public ScriptValue( final String value )
		{
			this.type = Interpreter.STRING_TYPE;
			this.contentString = value;
		}

		public ScriptValue( final float value )
		{
			this.type = Interpreter.FLOAT_TYPE;
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
			if ( this.type.equals( Interpreter.TYPE_FLOAT ) )
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
			if ( this.type.equals( Interpreter.TYPE_INT ) )
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

			if ( this.type.equals( Interpreter.TYPE_VOID ) )
			{
				return "void";
			}

			if ( this.contentString != null )
			{
				return this.contentString;
			}

			if ( this.type.equals( Interpreter.TYPE_BOOLEAN ) )
			{
				return String.valueOf( this.contentInt != 0 );
			}

			if ( this.type.equals( Interpreter.TYPE_FLOAT ) )
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

		public ScriptValue execute()
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

			if ( this.type == Interpreter.BOOLEAN_TYPE || this.type == Interpreter.INT_TYPE )
			{
				return this.contentInt < it.contentInt ? -1 : this.contentInt == it.contentInt ? 0 : 1;
			}

			if ( this.type == Interpreter.FLOAT_TYPE )
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
				key = type.getKey( Interpreter.parseValue( type.getIndexType(), data[ index ] ) );
			}
			else
			{
				key = type.getKey( Interpreter.parseValue( type.getIndexType(), "none" ) );
			}

			// If there's only a key and a value, parse the value
			// and store it in the composite

			if ( !( type.getDataType( key ) instanceof ScriptCompositeType ) )
			{
				this.aset( key, Interpreter.parseValue( type.getDataType( key ), data[ index + 1 ] ) );
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
			else if ( array[ index ].getType().equals( Interpreter.TYPE_STRING ) )
			{
				array[ index ] = val.toStringValue();
			}
			else if ( array[ index ].getType().equals( Interpreter.TYPE_INT ) && val.getType().equals(
				Interpreter.TYPE_FLOAT ) )
			{
				array[ index ] = val.toIntValue();
			}
			else if ( array[ index ].getType().equals( Interpreter.TYPE_FLOAT ) && val.getType().equals(
				Interpreter.TYPE_INT ) )
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
				if ( this.getDataType().equals( Interpreter.TYPE_STRING ) )
				{
					val = val.toStringValue();
				}
				else if ( this.getDataType().equals( Interpreter.TYPE_INT ) && val.getType().equals(
					Interpreter.TYPE_FLOAT ) )
				{
					val = val.toIntValue();
				}
				else if ( this.getDataType().equals( Interpreter.TYPE_FLOAT ) && val.getType().equals(
					Interpreter.TYPE_INT ) )
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
			else if ( array[ index ].getType().equals( Interpreter.TYPE_STRING ) )
			{
				array[ index ] = val.toStringValue();
			}
			else if ( array[ index ].getType().equals( Interpreter.TYPE_INT ) && val.getType().equals(
				Interpreter.TYPE_FLOAT ) )
			{
				array[ index ] = val.toIntValue();
			}
			else if ( array[ index ].getType().equals( Interpreter.TYPE_FLOAT ) && val.getType().equals(
				Interpreter.TYPE_INT ) )
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
					array[ offset ] = Interpreter.parseValue( valType, data[ index ] );
					index += 1;
				}
			}

			for ( int offset = size; offset < dataTypes.length; ++offset )
			{
				array[ offset ] = Interpreter.parseValue( dataTypes[ offset ], "none" );
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
			if ( this.oper.equals( "+" ) && ( leftType.equals( Interpreter.TYPE_STRING ) || rightType.equals( Interpreter.TYPE_STRING ) ) )
			{
				return Interpreter.STRING_TYPE;
			}

			// If it's not arithmetic, it's boolean
			if ( !this.oper.isArithmetic() )
			{
				return Interpreter.BOOLEAN_TYPE;
			}

			// Coerce int to float
			if ( leftType.equals( Interpreter.TYPE_FLOAT ) )
			{
				return Interpreter.FLOAT_TYPE;
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

		public ScriptValue execute()
		{
			return this.oper.applyTo( this.lhs, this.rhs );
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

		public ScriptValue applyTo( final ScriptExpression lhs, final ScriptExpression rhs )
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
				ScriptValue result = operand.removeKey();
				Interpreter.trace( "<- " + result );
				Interpreter.traceUnindent();
				return result;
			}

			Interpreter.traceIndent();
			Interpreter.trace( "Operand 1: " + lhs );

			ScriptValue leftValue = lhs.execute();
			Interpreter.captureValue( leftValue );
			if ( leftValue == null )
			{
				leftValue = Interpreter.VOID_VALUE;
			}
			Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + leftValue.toQuotedString() );
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
				if ( lhs.getType().equals( Interpreter.TYPE_INT ) )
				{
					result = new ScriptValue( 0 - leftValue.intValue() );
				}
				else if ( lhs.getType().equals( Interpreter.TYPE_FLOAT ) )
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
					Interpreter.trace( "<- " + Interpreter.TRUE_VALUE );
					Interpreter.traceUnindent();
					return Interpreter.TRUE_VALUE;
				}
				Interpreter.traceIndent();
				Interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute();
				Interpreter.captureValue( rightValue );
				if ( rightValue == null )
				{
					rightValue = Interpreter.VOID_VALUE;
				}
				Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + rightValue.toQuotedString() );
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
					Interpreter.trace( "<- " + Interpreter.FALSE_VALUE );
					return Interpreter.FALSE_VALUE;
				}
				Interpreter.traceIndent();
				Interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute();
				Interpreter.captureValue( rightValue );
				if ( rightValue == null )
				{
					rightValue = Interpreter.VOID_VALUE;
				}
				Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + rightValue.toQuotedString() );
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
			if ( !Interpreter.validCoercion( lhs.getType(), rhs.getType(), this.operator ) )
			{
				throw new RuntimeException( "Internal error: left hand side and right hand side do not correspond" );
			}

			// Special binary operator: <aggref> contains <any>
			if ( this.operator.equals( "contains" ) )
			{
				Interpreter.traceIndent();
				Interpreter.trace( "Operand 2: " + rhs );
				ScriptValue rightValue = rhs.execute();
				Interpreter.captureValue( rightValue );
				if ( rightValue == null )
				{
					rightValue = Interpreter.VOID_VALUE;
				}
				Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + rightValue.toQuotedString() );
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
			ScriptValue rightValue = rhs.execute();
			Interpreter.captureValue( rightValue );
			if ( rightValue == null )
			{
				rightValue = Interpreter.VOID_VALUE;
			}
			Interpreter.trace( "[" + Interpreter.executionStateString( Interpreter.currentState ) + "] <- " + rightValue.toQuotedString() );
			Interpreter.traceUnindent();
			if ( Interpreter.currentState == Interpreter.STATE_EXIT )
			{
				Interpreter.traceUnindent();
				return null;
			}

			// String operators
			if ( this.operator.equals( "+" ) )
			{
				if ( lhs.getType().equals( Interpreter.TYPE_STRING ) || rhs.getType().equals( Interpreter.TYPE_STRING ) )
				{
					ParseTree.concatenateBuffer.setLength( 0 );
					ParseTree.concatenateBuffer.append( leftValue.toStringValue().toString() );
					ParseTree.concatenateBuffer.append( rightValue.toStringValue().toString() );
					ScriptValue result = new ScriptValue( ParseTree.concatenateBuffer.toString() );
					Interpreter.trace( "<- " + result );
					Interpreter.traceUnindent();
					return result;
				}
			}

			if ( this.operator.equals( "=" ) || this.operator.equals( "==" ) )
			{
				if ( lhs.getType().equals( Interpreter.TYPE_STRING ) || lhs.getType().equals( Interpreter.TYPE_LOCATION ) || lhs.getType().equals(
					Interpreter.TYPE_MONSTER ) )
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
				if ( lhs.getType().equals( Interpreter.TYPE_STRING ) || lhs.getType().equals( Interpreter.TYPE_LOCATION ) || lhs.getType().equals(
					Interpreter.TYPE_MONSTER ) )
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

			if ( lhs.getType().equals( Interpreter.TYPE_FLOAT ) || rhs.getType().equals( Interpreter.TYPE_FLOAT ) )
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
