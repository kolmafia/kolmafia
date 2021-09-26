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

package net.sourceforge.kolmafia.textui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.textui.Parser.Line.Token;

import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.ArrayLiteral;
import net.sourceforge.kolmafia.textui.parsetree.Assignment;
import net.sourceforge.kolmafia.textui.parsetree.BasicScope;
import net.sourceforge.kolmafia.textui.parsetree.BasicScript;
import net.sourceforge.kolmafia.textui.parsetree.Catch;
import net.sourceforge.kolmafia.textui.parsetree.CompositeReference;
import net.sourceforge.kolmafia.textui.parsetree.Concatenate;
import net.sourceforge.kolmafia.textui.parsetree.Conditional;
import net.sourceforge.kolmafia.textui.parsetree.Else;
import net.sourceforge.kolmafia.textui.parsetree.ElseIf;
import net.sourceforge.kolmafia.textui.parsetree.ForEachLoop;
import net.sourceforge.kolmafia.textui.parsetree.ForLoop;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.Function.MatchType;
import net.sourceforge.kolmafia.textui.parsetree.FunctionCall;
import net.sourceforge.kolmafia.textui.parsetree.FunctionInvocation;
import net.sourceforge.kolmafia.textui.parsetree.FunctionReturn;
import net.sourceforge.kolmafia.textui.parsetree.If;
import net.sourceforge.kolmafia.textui.parsetree.IncDec;
import net.sourceforge.kolmafia.textui.parsetree.JavaForLoop;
import net.sourceforge.kolmafia.textui.parsetree.Loop;
import net.sourceforge.kolmafia.textui.parsetree.LoopBreak;
import net.sourceforge.kolmafia.textui.parsetree.LoopContinue;
import net.sourceforge.kolmafia.textui.parsetree.MapLiteral;
import net.sourceforge.kolmafia.textui.parsetree.Operation;
import net.sourceforge.kolmafia.textui.parsetree.Operator;
import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RepeatUntilLoop;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.ScriptExit;
import net.sourceforge.kolmafia.textui.parsetree.SortBy;
import net.sourceforge.kolmafia.textui.parsetree.StaticScope;
import net.sourceforge.kolmafia.textui.parsetree.Switch;
import net.sourceforge.kolmafia.textui.parsetree.SwitchScope;
import net.sourceforge.kolmafia.textui.parsetree.TernaryExpression;
import net.sourceforge.kolmafia.textui.parsetree.Try;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.TypeDef;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.VarArgType;
import net.sourceforge.kolmafia.textui.parsetree.Variable;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.textui.parsetree.WhileLoop;

import net.sourceforge.kolmafia.utilities.ByteArrayStream;
import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Parser
{
	public static final String APPROX = "\u2248";
	public static final String PRE_INCREMENT = "++X";
	public static final String PRE_DECREMENT = "--X";
	public static final String POST_INCREMENT = "X++";
	public static final String POST_DECREMENT = "X--";

	// Variables used during parsing

	private final String fileName;
	private final String shortFileName;
	private String scriptName;
	private final InputStream istream;

	private Line currentLine;
	private int currentIndex;
	private Token currentToken;

	private final Map<File, Long> imports;
	private Function mainMethod = null;
	private String notifyRecipient = null;

	public Parser()
	{
		this( null, null, null );
	}

	public Parser( final File scriptFile, final Map<File, Long> imports )
	{
		this( scriptFile, null, imports );
	}

	public Parser( final File scriptFile, final InputStream stream, final Map<File, Long> imports )
	{
		this.imports = imports != null ? imports : new TreeMap<>();

		this.istream = stream != null ? stream :
		               scriptFile != null ? DataUtilities.getInputStream( scriptFile ) :
		               null;

		if ( scriptFile != null )
		{
			this.fileName = scriptFile.getPath();
			this.shortFileName = this.fileName.substring( this.fileName.lastIndexOf( File.separator ) + 1 );

			if ( this.imports.isEmpty() )
			{
				this.imports.put( scriptFile, scriptFile.lastModified() );
			}
		}
		else
		{
			this.fileName = null;
			this.shortFileName = null;
		}

		if ( this.istream == null )
		{
			return;
		}

		try
		{
			final LineNumberReader commandStream = new LineNumberReader( new InputStreamReader( this.istream, StandardCharsets.UTF_8 ) );
			this.currentLine = new Line( commandStream );

			Line line = this.currentLine;
			while ( line.content != null )
			{
				line = new Line( commandStream, line );
			}

			// Move up to the first non-empty line
			while ( this.currentLine.content != null &&
			        this.currentLine.content.length() == 0 )
			{
				this.currentLine = this.currentLine.nextLine;
			}
			this.currentIndex = this.currentLine.offset;
		}
		catch ( Exception e )
		{
			// If any part of the initialization fails,
			// then throw an exception.

			throw this.parseException( this.fileName + " could not be accessed" );
		}
		finally
		{
			try
			{
				this.istream.close();
			}
			catch ( IOException e )
			{
			}
		}
	}

	public Scope parse()
	{
		if ( this.istream == null )
		{
			throw new RuntimeException( "Parser was not properly initialized before parsing was attempted" );
		}

		Scope scope = this.parseScope( null, null, null, Parser.getExistingFunctionScope(), false, false );

		if ( this.currentLine.nextLine != null )
		{
			throw this.parseException( "Script parsing error" );
		}

		return scope;
	}

	public String getFileName()
	{
		return this.fileName;
	}

	public String getShortFileName()
	{
		return this.shortFileName;
	}

	public String getScriptName()
	{
		return ( this.scriptName != null ) ?
		       this.scriptName :
		       this.shortFileName;
	}

	public int getLineNumber()
	{
		if ( this.istream == null )
		{
			return 0;
		}

		return this.currentLine.lineNumber;
	}

	public Map<File, Long> getImports()
	{
		return this.imports;
	}

	public Function getMainMethod()
	{
		return this.mainMethod;
	}

	public String getNotifyRecipient()
	{
		return this.notifyRecipient;
	}

	public static Scope getExistingFunctionScope()
	{
		return new Scope( RuntimeLibrary.functions, null, DataTypes.simpleTypes );
	}

	// **************** Parser *****************

	private static final HashSet<String> multiCharTokens = new HashSet<String>();
	private static final HashSet<String> reservedWords = new HashSet<String>();

	static
	{
		// Tokens
		multiCharTokens.add( "==" );
		multiCharTokens.add( "!=" );
		multiCharTokens.add( "<=" );
		multiCharTokens.add( ">=" );
		multiCharTokens.add( "||" );
		multiCharTokens.add( "&&" );
		multiCharTokens.add( "//" );
		multiCharTokens.add( "/*" );
		multiCharTokens.add( "<<" );
		multiCharTokens.add( ">>" );
		multiCharTokens.add( ">>>" );
		multiCharTokens.add( "++" );
		multiCharTokens.add( "--" );
		multiCharTokens.add( "**" );
		multiCharTokens.add( "+=" );
		multiCharTokens.add( "-=" );
		multiCharTokens.add( "*=" );
		multiCharTokens.add( "/=" );
		multiCharTokens.add( "%=" );
		multiCharTokens.add( "**=" );
		multiCharTokens.add( "&=" );
		multiCharTokens.add( "^=" );
		multiCharTokens.add( "|=" );
		multiCharTokens.add( "<<=" );
		multiCharTokens.add( ">>=" );
		multiCharTokens.add( ">>>=" );
		multiCharTokens.add( "..." );

		// Constants
		reservedWords.add( "true" );
		reservedWords.add( "false" );

		// Operators
		reservedWords.add( "contains" );
		reservedWords.add( "remove" );
		reservedWords.add( "new" );

		// Control flow
		reservedWords.add( "if" );
		reservedWords.add( "else" );
		reservedWords.add( "foreach" );
		reservedWords.add( "in" );
		reservedWords.add( "for" );
		reservedWords.add( "from" );
		reservedWords.add( "upto" );
		reservedWords.add( "downto" );
		reservedWords.add( "by" );
		reservedWords.add( "while" );
		reservedWords.add( "repeat" );
		reservedWords.add( "until" );
		reservedWords.add( "break" );
		reservedWords.add( "continue" );
		reservedWords.add( "return" );
		reservedWords.add( "exit" );
		reservedWords.add( "switch" );
		reservedWords.add( "case" );
		reservedWords.add( "default" );
		reservedWords.add( "try" );
		reservedWords.add( "catch" );
		reservedWords.add( "finally" );
		reservedWords.add( "static" );

		// Data types
		reservedWords.add( "void" );
		reservedWords.add( "boolean" );
		reservedWords.add( "int" );
		reservedWords.add( "float" );
		reservedWords.add( "string" );
		reservedWords.add( "buffer" );
		reservedWords.add( "matcher" );
		reservedWords.add( "aggregate" );

		reservedWords.add( "item" );
		reservedWords.add( "location" );
		reservedWords.add( "class" );
		reservedWords.add( "stat" );
		reservedWords.add( "skill" );
		reservedWords.add( "effect" );
		reservedWords.add( "familiar" );
		reservedWords.add( "slot" );
		reservedWords.add( "monster" );
		reservedWords.add( "element" );
		reservedWords.add( "coinmaster" );

		reservedWords.add( "record" );
		reservedWords.add( "typedef" );
	}

	private static boolean isReservedWord( final String name )
	{
		return name != null && Parser.reservedWords.contains( name.toLowerCase() );
	}

	public Scope importFile( final String fileName, final Scope scope )
	{
		List<File> matches = KoLmafiaCLI.findScriptFile( fileName );
		if ( matches.size() > 1 )
		{
			StringBuilder s = new StringBuilder();
			for ( File f : matches )
			{
				if ( s.length() > 0 )
					s.append( "; " );
				s.append( f.getPath() );
			}
			throw this.parseException( "too many matches for " + fileName + ": " + s );
		}
		if ( matches.size() == 0 )
		{
			throw this.parseException( fileName + " could not be found" );
		}

		File scriptFile = matches.get( 0 );

		if ( this.imports.containsKey( scriptFile ) )
		{
			return scope;
		}

		this.imports.put( scriptFile, scriptFile.lastModified() );

		Parser parser = new Parser( scriptFile, null, this.imports );
		Scope result = parser.parseScope( scope, null, null, scope.getParentScope(), false, false );
		if ( parser.currentLine.nextLine != null )
		{
			throw this.parseException( "Script parsing error" );
		}

		if ( parser.mainMethod != null )
		{	// Make imported script's main() available under a different name
			UserDefinedFunction f = new UserDefinedFunction(
				parser.mainMethod.getName() + "@" +
					parser.getScriptName().replace( ".ash", "" )
						.replaceAll( "[^a-zA-Z0-9]", "_" ),
				parser.mainMethod.getType(),
				parser.mainMethod.getVariableReferences() );
			f.setScope( ((UserDefinedFunction)parser.mainMethod).getScope() );
			result.addFunction( f );
		}

		return result;
	}

	private Scope parseCommandOrDeclaration( final Scope result, final Type expectedType )
	{
		Type t = this.parseType( result, true );

		// If there is no data type, it's a command of some sort
		if ( t == null )
		{
			ParseTreeNode c = this.parseCommand( expectedType, result, false, false, false );
			if ( c != null )
			{
				result.addCommand( c, this );
			}
			else
			{
				throw this.parseException( "command or declaration required" );
			}
		}
		else if ( this.parseVariables( t, result ) )
		{
			if ( this.currentToken().equals( ";" ) )
			{
				this.readToken(); //read ;
			}
			else
			{
				throw this.parseException( ";", this.currentToken() );
			}
		}
		else
		{
			//Found a type but no function or variable to tie it to
			throw this.parseException( "Type given but not used to declare anything" );
		}

		return result;
	}

	private Scope parseScope( final Scope startScope,
				  final Type expectedType,
				  final VariableList variables,
				  final BasicScope parentScope,
				  final boolean allowBreak,
				  final boolean allowContinue )
	{
		Scope result = startScope == null ? new Scope( variables, parentScope ) : startScope;
		return this.parseScope( result, expectedType, parentScope, allowBreak, allowContinue );
	}

	private Scope parseScope( Scope result,
				  final Type expectedType,
				  final BasicScope parentScope,
				  final boolean allowBreak,
				  final boolean allowContinue )
	{
		String importString;

		this.parseScriptName();
		this.parseNotify();
		this.parseSince();

		while ( ( importString = this.parseImport() ) != null )
		{
			result = this.importFile( importString, result );
		}

		while ( true )
		{
			if ( this.parseTypedef( result ) )
			{
				if ( this.currentToken().equals( ";" ) )
				{
					this.readToken(); //read ;
				}
				else
				{
					throw this.parseException( ";", this.currentToken() );
				}

				continue;
			}

			Type t = this.parseType( result, true );

			// If there is no data type, it's a command of some sort
			if ( t == null )
			{
				// See if it's a regular command
				ParseTreeNode c = this.parseCommand( expectedType, result, false, allowBreak, allowContinue );
				if ( c != null )
				{
					result.addCommand( c, this );
					continue;
				}

				// No type and no command -> done.
				break;
			}

			// If this is a new record definition, enter it
			if ( t.getType() == DataTypes.TYPE_RECORD && this.currentToken().equals( ";" ) )
			{
				this.readToken(); // read ;
				continue;
			}

			Function f = this.parseFunction( t, result );
			if ( f != null )
			{
				if ( f.getName().equalsIgnoreCase( "main" ) )
				{
					if ( parentScope.getParentScope() == null )
					{
						this.mainMethod = f;
					}
					else
					{
						throw this.parseException( "main method must appear at top level" );
					}
				}

				continue;
			}

			if ( this.parseVariables( t, result ) )
			{
				if ( this.currentToken().equals( ";" ) )
				{
					this.readToken(); //read ;
				}
				else
				{
					throw this.parseException( ";", this.currentToken() );
				}

				continue;
			}

			if ( ( t.getBaseType() instanceof AggregateType ) && this.currentToken().equals( "{" ) )
			{
				result.addCommand( this.parseAggregateLiteral( result, (AggregateType) t ), this );
			}
			else
			{
				//Found a type but no function or variable to tie it to
				throw this.parseException( "Type given but not used to declare anything" );
			}
		}

		return result;
	}

	private Type parseRecord( final BasicScope parentScope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "record" ) )
		{
			return null;
		}

		this.readToken(); // read record

		if ( this.currentToken().equals( ";" ) )
		{
			throw this.parseException( "Record name expected" );
		}

		// Allow anonymous records
		String recordName = null;

		if ( !this.currentToken().equals( "{" ) )
		{
			// Named record
			recordName = this.currentToken().content;

			if ( !this.parseIdentifier( recordName ) )
			{
				throw this.parseException( "Invalid record name '" + recordName + "'" );
			}
			else if ( Parser.isReservedWord( recordName ) )
			{
				throw this.parseException( "Reserved word '" + recordName + "' cannot be a record name" );
			}
			else if ( parentScope.findType( recordName ) != null )
			{
				throw this.parseException( "Record name '" + recordName + "' is already defined" );
			}

			this.readToken(); // read name
		}

		if ( this.currentToken().equals( "{" ) )
		{
			this.readToken(); // read {
		}
		else
		{
			throw this.parseException( "{", this.currentToken() );
		}

		// Loop collecting fields
		List<Type> fieldTypes = new ArrayList<Type>();
		List<String> fieldNames = new ArrayList<String>();

		while ( true )
		{
			if ( this.atEndOfFile() )
			{
				throw this.parseException( "}", this.currentToken() );
			}

			if ( this.currentToken().equals( "}" ) )
			{
				if ( fieldTypes.isEmpty() )
				{
					throw this.parseException( "Record field(s) expected" );
				}

				this.readToken(); // read }
				break;
			}

			// Get the field type
			Type fieldType = this.parseType( parentScope, true );
			if ( fieldType == null )
			{
				throw this.parseException( "Type name expected" );
			}

			if ( fieldType.getBaseType().equals( DataTypes.VOID_TYPE ) )
			{
				throw this.parseException( "Non-void field type expected" );
			}

			// Get the field name
			Token fieldName = this.currentToken();
			if ( fieldName.equals( ";" ) )
			{
				throw this.parseException( "Field name expected" );
			}
			else if ( !this.parseIdentifier( fieldName.content ) )
			{
				throw this.parseException( "Invalid field name '" + fieldName + "'" );
			}
			else if ( Parser.isReservedWord( fieldName.content ) )
			{
				throw this.parseException( "Reserved word '" + fieldName + "' cannot be used as a field name" );
			}
			else if ( fieldNames.contains( fieldName.content ) )
			{
				throw this.parseException( "Field name '" + fieldName + "' is already defined" );
			}
			else
			{
				this.readToken(); // read name
			}

			fieldTypes.add( fieldType );
			fieldNames.add( fieldName.content.toLowerCase() );

			if ( this.currentToken().equals( ";" ) )
			{
				this.readToken(); // read ;
			}
			else
			{
				throw this.parseException( ";", this.currentToken() );
			}
		}

		String[] fieldNameArray = new String[ fieldNames.size() ];
		Type[] fieldTypeArray = new Type[ fieldTypes.size() ];
		fieldNames.toArray( fieldNameArray );
		fieldTypes.toArray( fieldTypeArray );

		RecordType rec =
			new RecordType(
				recordName != null ? recordName :
					( "(anonymous record " + Integer.toHexString( Arrays.hashCode( fieldNameArray ) ) + ")" ),
				fieldNameArray, fieldTypeArray );

		if ( recordName != null )
		{
			// Enter into type table
			parentScope.addType( rec );
		}

		return rec;
	}

	private Function parseFunction( final Type functionType, final Scope parentScope )
	{
		if ( !this.parseIdentifier( this.currentToken().content ) )
		{
			return null;
		}

		if ( !"(".equals( this.nextToken() ) )
		{
			return null;
		}

		Token functionName = this.currentToken();

		if ( Parser.isReservedWord( functionName.content ) )
		{
			throw this.parseException( "Reserved word '" + functionName + "' cannot be used as a function name" );
		}

		this.readToken(); //read Function name
		this.readToken(); //read (

		VariableList paramList = new VariableList();
		List<VariableReference> variableReferences = new ArrayList<VariableReference>();
		boolean vararg = false;

		while ( true )
		{
			if ( this.currentToken().equals( ")" ) )
			{
				this.readToken(); //read )
				break;
			}
			Type paramType = this.parseType( parentScope, false );
			if ( paramType == null )
			{
				throw this.parseException( ")", this.currentToken() );
			}

			if ( this.currentToken().equals( "..." ) )
			{
				// We can only have a single vararg parameter
				if ( vararg )
				{
					throw this.parseException( "Only one vararg parameter is allowed" );
				}
				// Make an vararg type out of the previously parsed type.
				paramType = new VarArgType( paramType );

				this.readToken(); //read ...

				// Only one vararg is allowed
				vararg = true;
			}

			Variable param = this.parseVariable( paramType, null );
			if ( param == null )
			{
				throw this.parseException( "identifier", this.currentToken() );
			}

			if ( !paramList.add( param ) )
			{
				throw this.parseException( "Parameter " + param.getName() + " is already defined" );
			}

			if ( !this.currentToken().equals( ")" ) )
			{
				// The single vararg parameter must be the last one
				if ( vararg )
				{
					throw this.parseException( "The vararg parameter must be the last one" );
				}

				if ( this.currentToken().equals( "," ) )
				{
					this.readToken(); //read comma
				}
				else
				{
					throw this.parseException( ",", this.currentToken() );
				}
			}

			variableReferences.add( new VariableReference( param ) );
		}

		// Add the function to the parent scope before we parse the
		// function scope to allow recursion.

		UserDefinedFunction f = new UserDefinedFunction( functionName.content, functionType, variableReferences );

		if ( f.overridesLibraryFunction() )
		{
			throw this.overridesLibraryFunctionException( f );
		}

		UserDefinedFunction existing = parentScope.findFunction( f );

		if ( existing != null && existing.getScope() != null )
		{
			throw this.multiplyDefinedFunctionException( f );
		}

		if ( vararg )
		{
			Function clash = parentScope.findVarargClash( f );

			if ( clash != null )
			{
				throw this.varargClashException( f, clash );
			}
		}

		// Add new function or replace existing forward reference

		UserDefinedFunction result = parentScope.replaceFunction( existing, f );

		if ( this.currentToken().equals( ";" ) )
		{
			// Return forward reference
			this.readToken(); // ;
			return result;
		}

		Scope scope = this.parseBlockOrSingleCommand( functionType, paramList, parentScope, false, false, false );

		result.setScope( scope );
		if ( !result.assertBarrier() && !functionType.equals( DataTypes.TYPE_VOID ) )
		{
			throw this.parseException( "Missing return value" );
		}

		return result;
	}

	private boolean parseVariables( final Type t, final BasicScope parentScope )
	{
		while ( true )
		{
			Variable v = this.parseVariable( t, parentScope );
			if ( v == null )
			{
				return false;
			}

			if ( this.currentToken().equals( "," ) )
			{
				this.readToken(); //read ,
				continue;
			}

			return true;
		}
	}

	private Variable parseVariable( final Type t, final BasicScope scope )
	{
		if ( !this.parseIdentifier( this.currentToken().content ) )
		{
			return null;
		}

		Token variableName = this.currentToken();
		Variable result;

		if ( Parser.isReservedWord( variableName.content ) )
		{
			throw this.parseException( "Reserved word '" + variableName + "' cannot be a variable name" );
		}
		else if ( scope != null && scope.findVariable( variableName.content ) != null )
		{
			throw this.parseException( "Variable " + variableName + " is already defined" );
		}
		else
		{
			result = new Variable( variableName.content, t );
		}

		this.readToken(); // If parsing of Identifier succeeded, go to next token.
		// If we are parsing a parameter declaration, we are done
		if ( scope == null )
		{
			if ( this.currentToken().equals( "=" ) )
			{
				throw this.parseException( "Cannot initialize parameter " + variableName );
			}
			return result;
		}

		// Otherwise, we must initialize the variable.

		Value rhs;

		Type ltype = t.getBaseType();
		if ( this.currentToken().equals( "=" ) )
		{
			this.readToken(); // read =

			if ( this.currentToken().equals( "{" ) )
			{
				if ( ltype instanceof AggregateType )
				{
					rhs = this.parseAggregateLiteral( scope, (AggregateType) ltype );
				}
				else
				{
					throw this.parseException(
						"Cannot initialize " + variableName + " of type " + t + " with an aggregate literal" );
				}
			}
			else
			{
				rhs = this.parseExpression( scope );
			}

			if ( rhs != null )
			{
				rhs = this.autoCoerceValue( t, rhs, scope );
				if ( !Operator.validCoercion( ltype, rhs.getType(), "assign" ) )
				{
					throw this.parseException( "Cannot store " + rhs.getType() + " in " + variableName + " of type " + ltype );
				}
			}
			else
			{
				throw this.parseException( "Expression expected" );
			}
		}
		else if ( this.currentToken().equals( "{" ) && ltype instanceof AggregateType )
		{
			rhs = this.parseAggregateLiteral( scope, (AggregateType) ltype );
		}
		else
		{
			rhs = null;
		}

		scope.addVariable( result );
		VariableReference lhs = new VariableReference( variableName.content, scope );
		scope.addCommand( new Assignment( lhs, rhs ), this );

		return result;
	}

	private Value autoCoerceValue( Type ltype, Value rhs, final BasicScope scope )
	{
		// DataTypes.TYPE_ANY has no name
		if ( ltype == null || ltype.getName() == null)
		{
			return rhs;
		}

		// If the types are the same no coercion needed
		// A TypeDef or a RecordType match names for equal.
		Type rtype = rhs.getRawType();
		if ( ltype.equals( rtype ) )
		{
			return rhs;
		}

		// Look for a function:  LTYPE to_LTYPE( RTYPE )
		String name = "to_" + ltype.getName();
		List<Value> params = Collections.singletonList( rhs );

		// A typedef can overload a coercion function to a basic type or a typedef
		if ( ltype instanceof TypeDef || ltype instanceof RecordType )
		{
			Function target = scope.findFunction( name, params, MatchType.EXACT );
			if ( target != null && target.getType().equals( ltype ) )
			{
				return new FunctionCall( target, params, this );
			}
		}

		if ( ltype instanceof AggregateType )
		{
			return rhs;
		}

		if ( rtype instanceof TypeDef || rtype instanceof RecordType )
		{
			Function target = scope.findFunction( name, params, MatchType.EXACT );
			if ( target != null && target.getType().equals( ltype ) )
			{
				return new FunctionCall( target, params, this );
			}
		}

		// No overloaded coercions found for typedefs or records
		return rhs;
	}

	private List<Value> autoCoerceParameters( Function target, List<Value>params, BasicScope scope )
	{
		ListIterator<VariableReference> refIterator = target.getVariableReferences().listIterator();
		ListIterator<Value> valIterator = params.listIterator();
		VariableReference vararg = null;
		VarArgType varargType = null;

		while ( ( vararg != null || refIterator.hasNext() ) && valIterator.hasNext() )
		{
			// A VarArg parameter will consume all remaining values
			VariableReference currentParam = ( vararg != null ) ? vararg : refIterator.next();
			Type paramType = currentParam.getRawType();

			// If have found a vararg, remember it.
			if ( vararg == null && paramType instanceof VarArgType )
			{
				vararg = currentParam;
				varargType = ((VarArgType) paramType);
			}

			// If we are matching a vararg, coerce to data type
			if ( vararg != null )
			{
				paramType = varargType.getDataType();
			}

			Value currentValue = valIterator.next();
			Value coercedValue = this.autoCoerceValue( paramType, currentValue, scope );
			valIterator.set( coercedValue );
		}

		return params;
	}

	private boolean parseTypedef( final Scope parentScope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "typedef" ) )
		{
			return false;
		}
		this.readToken(); // read typedef

		Type t = this.parseType( parentScope, true );
		if ( t == null )
		{
			throw this.parseException( "Missing data type for typedef" );
		}

		Token typeName = this.currentToken();

		if ( typeName.equals( ";" ) )
		{
			throw this.parseException( "Type name expected" );
		}
		else if ( !this.parseIdentifier( typeName.content ) )
		{
			throw this.parseException( "Invalid type name '" + typeName + "'" );
		}
		else if ( Parser.isReservedWord( typeName.content ) )
		{
			throw this.parseException( "Reserved word '" + typeName + "' cannot be a type name" );
		}
		else
		{
			this.readToken(); // read name
		}

		Type existingType = parentScope.findType( typeName.content );
		if ( existingType != null )
		{
			if ( existingType.getBaseType().equals( t ) )
			{
				// It is OK to redefine a typedef with an equivalent type
				return true;
			}
				
			throw this.parseException( "Type name '" + typeName + "' is already defined" );
		}
		else
		{
			// Add the type to the type table
			TypeDef type = new TypeDef( typeName.content, t );
			parentScope.addType( type );
		}

		return true;
	}

	private ParseTreeNode parseCommand( final Type functionType, final BasicScope scope, final boolean noElse, boolean allowBreak, boolean allowContinue )
	{
		ParseTreeNode result;

		if ( this.currentToken().equalsIgnoreCase( "break" ) )
		{
			if ( allowBreak )
			{
				result = new LoopBreak();
			}
			else
			{
				throw this.parseException( "Encountered 'break' outside of loop" );
			}

			this.readToken(); //break
		}

		else if ( this.currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( allowContinue )
			{
				result = new LoopContinue();
			}
			else
			{
				throw this.parseException( "Encountered 'continue' outside of loop" );
			}

			this.readToken(); //continue
		}

		else if ( this.currentToken().equalsIgnoreCase( "exit" ) )
		{
			result = new ScriptExit();
			this.readToken(); //exit
		}

		else if ( ( result = this.parseReturn( functionType, scope ) ) != null )
		{
        }
		else if ( ( result = this.parseBasicScript() ) != null )
		{
			// basic_script doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseWhile( functionType, scope ) ) != null )
		{
			// while doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseForeach( functionType, scope ) ) != null )
		{
			// foreach doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseJavaFor( functionType, scope ) ) != null )
		{
			// for doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseFor( functionType, scope ) ) != null )
		{
			// for doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseRepeat( functionType, scope ) ) != null )
		{
        }
		else if ( ( result = this.parseSwitch( functionType, scope, allowContinue ) ) != null )
		{
			// switch doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseConditional( functionType, scope, noElse, allowBreak, allowContinue ) ) != null )
		{
			// loop doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseTry( functionType, scope, allowBreak, allowContinue ) ) != null )
		{
			// try doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseCatch( functionType, scope, allowBreak, allowContinue ) ) != null )
		{
			// standalone catch doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseStatic( functionType, scope ) ) != null )
		{
			// try doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseSort( scope ) ) != null )
		{
        }
		else if ( ( result = this.parseRemove( scope ) ) != null )
		{
        }
		else if ( ( result = this.parseBlock( functionType, null, scope, noElse, allowBreak, allowContinue ) ) != null )
		{
			// {} doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseValue( scope ) ) != null )
		{
        }
		else
		{
			return null;
		}

		if ( this.currentToken().equals( ";" ) )
		{
			this.readToken(); // ;
		}
		else
		{
			throw this.parseException( ";", this.currentToken() );
		}

		return result;
	}

	private Type parseType( final BasicScope scope, final boolean records )
	{
		if ( !this.parseIdentifier( this.currentToken().content ) )
		{
			return null;
		}

		Type valType = scope.findType( this.currentToken().content );
		if ( valType == null )
		{
			if ( records && this.currentToken().equalsIgnoreCase( "record" ) )
			{
				valType = this.parseRecord( scope );

				if ( valType == null )
				{
					return null;
				}

				if ( this.currentToken().equals( "[" ) )
				{
					return this.parseAggregateType( valType, scope );
				}

				return valType;
			}

			return null;
		}

		this.readToken();

		if ( this.currentToken().equals( "[" ) )
		{
			return this.parseAggregateType( valType, scope );
		}

		return valType;
	}

	/**
	 * Parses the content of an aggregate literal, e.g., `{1:true, 2:false, 3:false}`.
	 *
	 * <p>The presence of the opening bracket "{" is ALWAYS assumed when entering this method,
	 * and as such, MUST be checked before calling it. This method will never return null.
	 */
	private Value parseAggregateLiteral( final BasicScope scope, final AggregateType aggr )
	{
		this.readToken(); // read {

		Type index = aggr.getIndexType();
		Type data = aggr.getDataType();

		List<Value> keys = new ArrayList<Value>();
		List<Value> values = new ArrayList<Value>();

		// If index type is an int, it could be an array or a map
		boolean arrayAllowed = index.equals( DataTypes.INT_TYPE );

		// Assume it is a map.
		boolean isArray = false;

		while ( true )
		{
			if ( this.atEndOfFile() )
			{
				throw this.parseException( "}", this.currentToken() );
			}

			if ( this.currentToken().equals( "}" ) )
			{
				this.readToken(); // read }
				break;
			}

			Value lhs;

			// If we know we are reading an ArrayLiteral or haven't
			// yet ensured we are reading a MapLiteral, allow any
			// type of Value as the "key"
			Type dataType = data.getBaseType();
			if ( ( isArray || arrayAllowed ) && this.currentToken().equals( "{" ) && dataType instanceof AggregateType )
			{
				lhs = parseAggregateLiteral( scope, (AggregateType) dataType );
			}
			else
			{
				lhs = this.parseExpression( scope );
			}

			if ( lhs == null )
			{
				throw this.parseException( "Script parsing error" );
			}

			Token delim = this.currentToken();

			// If this could be an array and we haven't already
			// decided it is one, if the delimiter is a comma,
			// parse as an ArrayLiteral
			if ( arrayAllowed )
			{
				if ( delim.equals( "," ) || delim.equals( "}" ) )
				{
					isArray = true;
				}
				arrayAllowed = false;
			}

			if ( !delim.equals( ":" ) )
			{
				// If parsing an ArrayLiteral, accumulate only values
				if ( isArray )
				{
					// The value must have the correct data type
					lhs = this.autoCoerceValue( data, lhs, scope );
					if ( !Operator.validCoercion( dataType, lhs.getType(), "assign" ) )
					{
						throw this.parseException( "Invalid array literal" );
					}

					values.add( lhs );
				}
				else
				{
					throw this.parseException( ":", delim );
				}

				// Move on to the next value
				if ( delim.equals( "," ) )
				{
					this.readToken(); // read ;
				}
				else if ( !delim.equals( "}" ) )
				{
					throw this.parseException( "}", delim );
				}

				continue;
			}

			// We are parsing a MapLiteral
			this.readToken(); // read :

			if ( isArray )
			{
				// In order to reach this point without an error, we must have had a correct
				// array literal so far, meaning the index type is an integer, and what we saw before
				// the colon must have matched the aggregate's data type. Therefore, the next
				// question is: is the data type also an integer?

				if ( data.equals( DataTypes.INT_TYPE ) )
				{
					// If so, this is an int[int] aggregate. They could have done something like
					// {0, 1, 2, 3:3, 4:4, 5:5}
					throw this.parseException( "Cannot include keys when making an array literal" );
				}
				else
				{
					// If not, we can't tell why there's a colon here.
					throw this.parseException( ", or }", delim );
				}
			}

			Value rhs;
			if ( this.currentToken().equals( "{" ) && dataType instanceof AggregateType )
			{
				rhs = parseAggregateLiteral( scope, (AggregateType) dataType );
			}
			else
			{
				rhs = this.parseExpression( scope );
			}

			if ( rhs == null )
			{
				throw this.parseException( "Script parsing error" );
			}

			// Check that each type is valid via validCoercion
			lhs = this.autoCoerceValue( index, lhs, scope );
			rhs = this.autoCoerceValue( data, rhs, scope );
			if ( !Operator.validCoercion( index, lhs.getType(), "assign" ) ||
			     !Operator.validCoercion( data, rhs.getType(), "assign" ) )
			{
				throw this.parseException( "Invalid map literal" );
			}

			keys.add( lhs );
			values.add( rhs );

			// Move on to the next value
			if ( this.currentToken().equals( "," ) )
			{
				this.readToken(); // read ,
			}
			else if ( !this.currentToken().equals( "}" ) )
			{
				throw this.parseException( "}", this.currentToken() );
			}
		}

		if ( isArray )
		{
			int size = aggr.getSize ();
			if ( size > 0 && size < values.size() )
			{
				throw this.parseException( "Array has " + size + " elements but " + values.size() + " initializers." );
			}
		}

		return isArray ? new ArrayLiteral( aggr, values ) :  new MapLiteral( aggr, keys, values );
	}

	private Type parseAggregateType( Type dataType, final BasicScope scope )
	{
		Token separatorToken = this.currentToken();

		this.readToken(); // [ or ,

		Type indexType = null;
		int size = 0;

		if ( this.currentToken().equals( "]" ) )
		{
			if ( !separatorToken.equals( "[" ) )
			{
				throw this.parseException( "Missing index token" );
			}
		}
		else if ( this.readIntegerToken( this.currentToken().content ) )
		{
			size = StringUtilities.parseInt( this.currentToken().content );
			this.readToken(); // integer
		}
		else if ( this.parseIdentifier( this.currentToken().content ) )
		{
			indexType = scope.findType( this.currentToken().content );

			if ( indexType != null )
			{
				if ( !indexType.isPrimitive() )
				{
					throw this.parseException( "Index type '" + this.currentToken() + "' is not a primitive type" );
				}
			}
			else
			{
				throw this.parseException( "Invalid type name '" + this.currentToken() + "'" );
			}

			this.readToken(); // type name
		}
		else
		{
			throw this.parseException( "Missing index token" );
		}

		if ( this.currentToken().equals( "," ) ||
		     ( this.currentToken().equals( "]" ) &&
		       "[".equals( this.nextToken() ) ) )
		{
			if ( this.currentToken().equals( "]" ) )
			{
				this.readToken(); // ]
			}

			dataType = this.parseAggregateType( dataType, scope );
		}
		else if ( this.currentToken().equals( "]" ) )
		{
			this.readToken(); // ]
		}
		else
		{
			throw this.parseException( ", or ]", this.currentToken() );
		}

		return indexType != null ?
			new AggregateType( dataType, indexType ) :
			new AggregateType( dataType, size );
	}

	private boolean parseIdentifier( final String identifier )
	{
		if ( identifier == null )
		{
			return false;
		}

		if ( !Character.isLetter( identifier.charAt( 0 ) ) && identifier.charAt( 0 ) != '_' )
		{
			return false;
		}

		for ( int i = 1; i < identifier.length(); ++i )
		{
			if ( !Character.isLetterOrDigit( identifier.charAt( i ) ) && identifier.charAt( i ) != '_' )
			{
				return false;
			}
		}

		return true;
	}

	private boolean parseScopedIdentifier( final String identifier )
	{
		if ( identifier == null )
		{
			return false;
		}

		if ( !Character.isLetter( identifier.charAt( 0 ) ) && identifier.charAt( 0 ) != '_' )
		{
			return false;
		}

		for ( int i = 1; i < identifier.length(); ++i )
		{
			if ( !Character.isLetterOrDigit( identifier.charAt( i ) ) && identifier.charAt( i ) != '_'  && identifier.charAt( i ) != '@' )
			{
				return false;
			}
		}

		return true;
	}

	private FunctionReturn parseReturn( final Type expectedType, final BasicScope parentScope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "return" ) )
		{
			return null;
		}

		this.readToken(); //return

		if ( expectedType == null )
		{
			throw this.parseException( "Cannot return when outside of a function" );
		}

		if ( this.currentToken().equals( ";" ) )
		{
			if ( expectedType != null && !expectedType.equals( DataTypes.TYPE_VOID ) )
			{
				throw this.parseException( "Return needs " + expectedType + " value" );
			}

			return new FunctionReturn( null, DataTypes.VOID_TYPE );
		}

		if ( expectedType != null && expectedType.equals( DataTypes.TYPE_VOID ) )
		{
			throw this.parseException( "Cannot return a value from a void function" );
		}

		Value value = this.parseExpression( parentScope );

		if ( value != null )
		{
			value = this.autoCoerceValue( expectedType, value, parentScope );
		}
		else
		{
			throw this.parseException( "Expression expected" );
		}

		if ( expectedType != null && !Operator.validCoercion( expectedType, value.getType(), "return" ) )
		{
			throw this.parseException( "Cannot return " + value.getType() + " value from " + expectedType + " function" );
		}

		return new FunctionReturn( value, expectedType );
	}

	private Scope parseSingleCommandScope( final Type functionType,
	                                       final BasicScope parentScope,
	                                       final boolean noElse,
	                                       final boolean allowBreak,
	                                       final boolean allowContinue )
	{
		Scope result = new Scope( parentScope );

		ParseTreeNode command = this.parseCommand( functionType, parentScope, noElse, allowBreak, allowContinue );
		if ( command != null )
		{
			result.addCommand( command, this );
		}
		else
		{
			if ( this.currentToken().equals( ";" ) )
			{
				this.readToken(); // ;
			}
			else
			{
				throw this.parseException( ";", this.currentToken() );
			}
		}

		return result;
	}

	private Scope parseBlockOrSingleCommand( final Type functionType,
						 final VariableList variables,
						 final BasicScope parentScope,
						 final boolean noElse,
						 boolean allowBreak,
						 boolean allowContinue )
	{
		Scope scope = this.parseBlock( functionType, variables, parentScope, noElse, allowBreak, allowContinue );
		if ( scope != null )
		{
			return scope;
		}
		return this.parseSingleCommandScope( functionType, parentScope, noElse, allowBreak, allowContinue );
	}

	private Scope parseBlock( final Type functionType,
				  final VariableList variables,
				  final BasicScope parentScope,
				  final boolean noElse,
				  final boolean allowBreak,
				  final boolean allowContinue )
	{
		if ( !this.currentToken().equals( "{" ) )
		{
			return null;
		}

		this.readToken(); // {

		Scope scope = this.parseScope( null, functionType, variables, parentScope, allowBreak, allowContinue );

		if ( this.currentToken().equals( "}" ) )
		{
			this.readToken(); //read }
		}
		else
		{
			throw this.parseException( "}", this.currentToken() );
		}

		return scope;
	}

	private Conditional parseConditional( final Type functionType,
					      final BasicScope parentScope,
					      boolean noElse,
					      final boolean allowBreak,
					      final boolean allowContinue )
	{
		if ( !this.currentToken().equalsIgnoreCase( "if" ) )
		{
			return null;
		}

		this.readToken(); // if

		if ( this.currentToken().equals( "(" ) )
		{
			this.readToken(); // (
		}
		else
		{
			throw this.parseException( "(", this.currentToken() );
		}

		Value condition = this.parseExpression( parentScope );

		if ( this.currentToken().equals( ")" ) )
		{
			this.readToken(); // )
		}
		else
		{
			throw this.parseException( ")", this.currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw this.parseException( "\"if\" requires a boolean conditional expression" );
		}

		If result = null;
		boolean elseFound = false;
		boolean finalElse = false;

		do
		{
			Scope scope = parseBlockOrSingleCommand( functionType, null, parentScope, !elseFound, allowBreak, allowContinue );

			if ( result == null )
			{
				result = new If( scope, condition );
			}
			else if ( finalElse )
			{
				result.addElseLoop( new Else( scope, condition ) );
			}
			else
			{
				result.addElseLoop( new ElseIf( scope, condition ) );
			}

			if ( !noElse && this.currentToken().equalsIgnoreCase( "else" ) )
			{
				if ( finalElse )
				{
					throw this.parseException( "Else without if" );
				}

				this.readToken(); //else
				if ( this.currentToken().equalsIgnoreCase( "if" ) )
				{
					this.readToken(); //if

					if ( this.currentToken().equals( "(" ) )
					{
						this.readToken(); //(
					}
					else
					{
						throw this.parseException( "(", this.currentToken() );
					}

					condition = this.parseExpression( parentScope );

					if ( this.currentToken().equals( ")" ) )
					{
						this.readToken(); // )
					}
					else
					{
						throw this.parseException( ")", this.currentToken() );
					}

					if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
					{
						throw this.parseException( "\"if\" requires a boolean conditional expression" );
					}
				}
				else
				//else without condition
				{
					condition = DataTypes.TRUE_VALUE;
					finalElse = true;
				}

				elseFound = true;
				continue;
			}

			elseFound = false;
		}
		while ( elseFound );

		return result;
	}

	private BasicScript parseBasicScript()
	{
		if ( !this.currentToken().equalsIgnoreCase( "cli_execute" ) )
		{
			return null;
		}

		if ( !"{".equals( this.nextToken() ) )
		{
			return null;
		}

		this.readToken(); // cli_execute
		this.readToken(); // {

		ByteArrayStream ostream = new ByteArrayStream();

		while ( true )
		{
			if ( this.atEndOfFile() )
			{
				throw this.parseException( "}", this.currentToken() );
			}

			if ( this.currentToken().equals( "}" ) )
			{
				this.readToken(); // }
				break;
			}

			this.clearCurrentToken();

			final String line = this.restOfLine();

			try
			{
				ostream.write( line.getBytes() );
				ostream.write( KoLConstants.LINE_BREAK.getBytes() );
			}
			catch ( Exception e )
			{
				// Byte array output streams do not throw errors,
				// other than out of memory errors.

				StaticEntity.printStackTrace( e );
			}

			if ( line.length() > 0 )
			{
				this.currentLine.makeToken( line.length() );
			}
			this.currentLine = this.currentLine.nextLine;
			this.currentIndex = this.currentLine.offset;
		}

		return new BasicScript( ostream );
	}

	private Loop parseWhile( final Type functionType, final BasicScope parentScope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "while" ) )
		{
			return null;
		}

		this.readToken(); // while

		if ( this.currentToken().equals( "(" ) )
		{
			this.readToken(); // (
		}
		else
		{
			throw this.parseException( "(", this.currentToken() );
		}

		Value condition = this.parseExpression( parentScope );

		if ( this.currentToken().equals( ")" ) )
		{
			this.readToken(); // )
		}
		else
		{
			throw this.parseException( ")", this.currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw this.parseException( "\"while\" requires a boolean conditional expression" );
		}

		Scope scope = this.parseLoopScope( functionType, null, parentScope );

		return new WhileLoop( scope, condition );
	}

	private Loop parseRepeat( final Type functionType, final BasicScope parentScope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "repeat" ) )
		{
			return null;
		}

		this.readToken(); // repeat

		Scope scope = this.parseLoopScope( functionType, null, parentScope );

		if ( this.currentToken().equalsIgnoreCase( "until" ) )
		{
			this.readToken(); // until
		}
		else
		{
			throw this.parseException( "until", this.currentToken() );
		}

		if ( this.currentToken().equals( "(" ) )
		{
			this.readToken(); // (
		}
		else
		{
			throw this.parseException( "(", this.currentToken() );
		}

		Value condition = this.parseExpression( parentScope );

		if ( this.currentToken().equals( ")" ) )
		{
			this.readToken(); // )
		}
		else
		{
			throw this.parseException( ")", this.currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw this.parseException( "\"repeat\" requires a boolean conditional expression" );
		}

		return new RepeatUntilLoop( scope, condition );
	}

	private Switch parseSwitch( final Type functionType, final BasicScope parentScope, final boolean allowContinue )
	{
		if ( !this.currentToken().equalsIgnoreCase( "switch" ) )
		{
			return null;
		}

		this.readToken(); // switch

		if ( !this.currentToken().equals( "(" ) && !this.currentToken().equals( "{" ) )
		{
			throw this.parseException( "( or {", this.currentToken() );
		}

		Value condition = DataTypes.TRUE_VALUE;
		if ( this.currentToken().equals( "(" ) )
		{
			this.readToken(); // (

			condition = this.parseExpression( parentScope );

			if ( this.currentToken().equals( ")" ) )
			{
				this.readToken(); // )
			}
			else
			{
				throw this.parseException( ")", this.currentToken() );
			}

			if ( condition == null )
			{
				throw this.parseException( "\"switch ()\" requires an expression" );
			}
		}

		Type type = condition.getType();

		if ( this.currentToken().equals( "{" ) )
		{
			this.readToken(); // {
		}
		else
		{
			throw this.parseException( "{", this.currentToken() );
		}

		List<Value> tests = new ArrayList<Value>();
		List<Integer> indices = new ArrayList<Integer>();
		int defaultIndex = -1;

		SwitchScope scope = new SwitchScope( parentScope );
		int currentIndex = 0;
		Integer currentInteger = null;

		Map<Value, Integer> labels = new TreeMap<>();
		boolean constantLabels = true;

		while ( true )
		{
			if ( this.currentToken().equalsIgnoreCase( "case" ) )
			{
				this.readToken(); // case

				Value test = this.parseExpression( parentScope );

				if ( test == null )
				{
					throw this.parseException( "Case label needs to be followed by an expression" );
				}

				if ( this.currentToken().equals( ":" ) )
				{
					this.readToken(); // :
				}
				else
				{
					throw this.parseException( ":", this.currentToken() );
				}

				if ( !test.getType().equals( type ) )
				{
					throw this.parseException( "Switch conditional has type " + type + " but label expression has type " + test.getType() );
				}

				if ( currentInteger == null )
				{
					currentInteger = IntegerPool.get( currentIndex );
				}

				if ( test.getClass() == Value.class )
				{
					if ( labels.get( test ) != null )
					{
						throw this.parseException( "Duplicate case label: " + test );
					}
					labels.put( test, currentInteger );
				}
				else
				{
					constantLabels = false;
				}


				tests.add( test );
				indices.add( currentInteger );
				scope.resetBarrier();

				continue;
			}

			if ( this.currentToken().equalsIgnoreCase( "default" ) )
			{
				this.readToken(); // default

				if ( this.currentToken().equals( ":" ) )
				{
					this.readToken(); // :
				}
				else
				{
					throw this.parseException( ":", this.currentToken() );
				}

				if ( defaultIndex == -1 )
				{
					defaultIndex = currentIndex;
				}
				else
				{
					throw this.parseException( "Only one default label allowed in a switch statement" );
				}

				scope.resetBarrier();

				continue;
			}

			Type t = this.parseType( scope, true );

			// If there is no data type, it's a command of some sort
			if ( t == null )
			{
				// See if it's a regular command
				ParseTreeNode c = this.parseCommand( functionType, scope, false, true, allowContinue );
				if ( c != null )
				{
					scope.addCommand( c, this );
					currentIndex = scope.commandCount();
					currentInteger = null;
					continue;
				}

				// No type and no command -> done.
				break;
			}

			if ( !this.parseVariables( t, scope ) )
			{
				//Found a type but no function or variable to tie it to
				throw this.parseException( "Type given but not used to declare anything" );
			}

			if ( this.currentToken().equals( ";" ) )
			{
				this.readToken(); //read ;
			}
			else
			{
				throw this.parseException( ";", this.currentToken() );
			}

			currentIndex = scope.commandCount();
			currentInteger = null;
		}

		if ( this.currentToken().equals( "}" ) )
		{
			this.readToken(); // }
		}
		else
		{
			throw this.parseException( "}", this.currentToken() );
		}

		return new Switch( condition, tests, indices, defaultIndex, scope,
				   constantLabels ? labels : null );
	}

	private Try parseTry( final Type functionType, final BasicScope parentScope,
			      final boolean allowBreak, final boolean allowContinue )
	{
		if ( !this.currentToken().equalsIgnoreCase( "try" ) )
		{
			return null;
		}

		this.readToken(); // try

		Scope body = this.parseBlockOrSingleCommand( functionType, null, parentScope, false, allowBreak, allowContinue );

		// catch clauses would be parsed here

		Scope finalClause;

		if ( this.currentToken().equalsIgnoreCase( "finally" ) )
		{
			this.readToken(); // finally

			finalClause = this.parseBlockOrSingleCommand( functionType, null, body, false, allowBreak, allowContinue );
		}
		else
		{
			// this would not be an error if at least one catch was present
			throw this.parseException( "\"try\" without \"finally\" is pointless" );
		}

		return new Try( body, finalClause );
	}

	private Catch parseCatch( final Type functionType, final BasicScope parentScope,
				  final boolean allowBreak, final boolean allowContinue )
	{
		if ( !this.currentToken().equalsIgnoreCase( "catch" ) )
		{
			return null;
		}

		this.readToken(); // catch

		Scope body = this.parseBlockOrSingleCommand( functionType, null, parentScope, false, allowBreak, allowContinue );

		return new Catch( body );
	}

	private Catch parseCatchValue( final BasicScope parentScope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "catch" ) )
		{
			return null;
		}

		this.readToken(); // catch

		ParseTreeNode body = this.parseBlock( null, null, parentScope, true, false, false );
		if ( body == null )
		{
			Value value = this.parseExpression( parentScope );
			if ( value != null )
			{
				body = value;
			}
			else
			{
				throw this.parseException( "\"catch\" requires a block or an expression" );
			}
		}

		return new Catch( body );
	}

	private Scope parseStatic( final Type functionType, final BasicScope parentScope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "static" ) )
		{
			return null;
		}

		this.readToken(); // static

		Scope result = new StaticScope( parentScope );

		if ( this.currentToken().equals( "{" ) )
		{
			this.readToken(); //read {

			this.parseScope( result, functionType, parentScope, false, false );

			if ( this.currentToken().equals( "}" ) )
			{
				this.readToken(); //read }
			}
			else
			{
				throw this.parseException( "}", this.currentToken() );
			}
		}
		else	// body is a single call
		{
			this.parseCommandOrDeclaration( result, functionType );
		}

		return result;
	}

	private SortBy parseSort( final BasicScope parentScope )
	{
		// sort aggregate by expr

		if ( !this.currentToken().equalsIgnoreCase( "sort" ) )
		{
			return null;
		}

		if ( this.nextToken() == null ||
		     this.nextToken().equals( "(" ) ||
		     this.nextToken().equals( "=" ) )
		{	// it's a call to a function named sort(), or an assigment to
			// a variable named sort, not the sort statement.
			return null;
		}

		this.readToken(); // sort

		// Get an aggregate reference
		Value aggregate = this.parseVariableReference( parentScope );

		if ( !( aggregate instanceof VariableReference ) ||
                !( aggregate.getType().getBaseType() instanceof AggregateType ) )
		{
			throw this.parseException( "Aggregate reference expected" );
		}

		if ( this.currentToken().equalsIgnoreCase( "by" ) )
		{
			this.readToken();	// by
		}
		else
		{
			throw this.parseException( "by", this.currentToken() );
		}

		// Define key variables of appropriate type
		VariableList varList = new VariableList();
		AggregateType type = (AggregateType) aggregate.getType().getBaseType();
		Variable valuevar = new Variable( "value", type.getDataType() );
		varList.add( valuevar );
		Variable indexvar = new Variable( "index", type.getIndexType() );
		varList.add( indexvar );

		// Parse the key expression in a new scope containing 'index' and 'value'
		Scope scope = new Scope( varList, parentScope );
		Value expr = this.parseExpression( scope );

		if ( expr == null )
		{
			throw this.parseException( "Expression expected" );
		}

		return new SortBy( (VariableReference) aggregate, indexvar, valuevar, expr, this );
	}

	private Loop parseForeach( final Type functionType, final BasicScope parentScope )
	{
		// foreach key [, key ... ] in aggregate { scope }

		if ( !this.currentToken().equalsIgnoreCase( "foreach" ) )
		{
			return null;
		}

		this.readToken(); // foreach

		List<String> names = new ArrayList<String>();

		while ( true )
		{
			Token name = this.currentToken();

			if ( !this.parseIdentifier( name.content ) ||
			     // "foreach in aggregate" (i.e. no key)
			     name.equalsIgnoreCase( "in" ) &&
			     !"in".equalsIgnoreCase( this.nextToken() ) &&
			     !",".equals( this.nextToken() ) )
			{
				throw this.parseException( "Key variable name expected" );
			}
			else if ( Parser.isReservedWord( name.content ) )
			{
				throw this.parseException( "Reserved word '" + name + "' cannot be a key variable name" );
			}
			else if ( names.contains( name.content ) )
			{
				throw this.parseException( "Key variable '" + name + "' is already defined" );
			}
			else
			{
				names.add( name.content );
			}

			this.readToken(); // name

			if ( this.currentToken().equals( "," ) )
			{
				this.readToken(); // ,
				continue;
			}

			if ( this.currentToken().equalsIgnoreCase( "in" ) )
			{
				this.readToken(); // in
				break;
			}

			throw this.parseException( "in", this.currentToken() );
		}

		// Get an aggregate reference
		Value aggregate = this.parseValue( parentScope );

		if ( aggregate == null || !( aggregate.getType().getBaseType() instanceof AggregateType ) )
		{
			throw this.parseException( "Aggregate reference expected" );
		}

		// Define key variables of appropriate type
		VariableList varList = new VariableList();
		List<VariableReference> variableReferences = new ArrayList<VariableReference>();
		Type type = aggregate.getType().getBaseType();

		for ( String name : names )
		{
			Type itype;
			if ( type == null )
			{
				throw this.parseException( "Too many key variables specified" );
			}
			else if ( type instanceof AggregateType )
			{
				itype = ( (AggregateType) type ).getIndexType();
				type = ( (AggregateType) type ).getDataType();
			}
			else
			{	// Variable after all key vars holds the value instead
				itype = type;
				type = null;
			}

			Variable keyvar = new Variable( name, itype );
			varList.add( keyvar );
			variableReferences.add( new VariableReference( keyvar ) );
		}

		// Parse the scope with the list of keyVars
		Scope scope = this.parseLoopScope( functionType, varList, parentScope );

		// Add the foreach node with the list of varRefs
		return new ForEachLoop( scope, variableReferences, aggregate, this );
	}

	private Loop parseFor( final Type functionType, final BasicScope parentScope )
	{
		// for identifier from X [upto|downto|to|] Y [by Z]? {scope }

		if ( !this.currentToken().equalsIgnoreCase( "for" ) )
		{
			return null;
		}

		if ( !this.parseIdentifier( this.nextToken() ) )
		{
			return null;
		}

		this.readToken(); // for

		Token name = this.currentToken();

		if ( Parser.isReservedWord( name.content ) )
		{
			throw this.parseException( "Reserved word '" + name + "' cannot be an index variable name" );
		}
		else if ( parentScope.findVariable( name.content ) != null )
		{
			throw this.parseException( "Index variable '" + name + "' is already defined" );
		}

		this.readToken(); // name

		if ( this.currentToken().equalsIgnoreCase( "from" ) )
		{
			this.readToken(); // from
		}
		else
		{
			throw this.parseException( "from", this.currentToken() );
		}

		Value initial = this.parseExpression( parentScope );

		if ( initial == null )
		{
			throw this.parseException( "Expression for initial value expected" );
		}

		int direction = 0;

		if ( this.currentToken().equalsIgnoreCase( "upto" ) )
		{
			direction = 1;
		}
		else if ( this.currentToken().equalsIgnoreCase( "downto" ) )
		{
			direction = -1;
		}
		else if ( this.currentToken().equalsIgnoreCase( "to" ) )
		{
			direction = 0;
		}
		else
		{
			throw this.parseException( "to, upto, or downto", this.currentToken() );
		}

		this.readToken(); // upto/downto

		Value last = this.parseExpression( parentScope );

		if ( last == null )
		{
			throw this.parseException( "Expression for floor/ceiling value expected" );
		}

		Value increment = DataTypes.ONE_VALUE;
		if ( this.currentToken().equalsIgnoreCase( "by" ) )
		{
			this.readToken(); // by
			increment = this.parseExpression( parentScope );

			if ( increment == null )
			{
				throw this.parseException( "Expression for increment value expected" );
			}
		}

		// Create integer index variable
		Variable indexvar = new Variable( name.content, DataTypes.INT_TYPE );

		// Put index variable onto a list
		VariableList varList = new VariableList();
		varList.add( indexvar );

		Scope scope = this.parseLoopScope( functionType, varList, parentScope );

		return new ForLoop( scope, new VariableReference( indexvar ), initial, last, increment, direction, this );
	}

	private Loop parseJavaFor( final Type functionType, final BasicScope parentScope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "for" ) )
		{
			return null;
		}

		if ( !"(".equals( this.nextToken() ) )
		{
			return null;
		}

		this.readToken(); // for
		this.readToken(); // (

		// Parse variables and initializers

		Scope scope = new Scope( parentScope );
		List<Assignment> initializers = new ArrayList<Assignment>();

		// Parse each initializer in the context of scope, adding
		// variable to variable list in the scope, and saving
		// initialization expressions in initializers.

		while ( !this.currentToken().equals( ";" ) )
		{
			Type t = this.parseType( scope, true );

			Token name = this.currentToken();
			Variable variable;

			if ( !this.parseIdentifier( name.content ) || Parser.isReservedWord( name.content ) )
			{
				throw this.parseException( "Identifier required" );
			}

			// If there is no data type, it is using an existing variable
			if ( t == null )
			{
				variable = parentScope.findVariable( name.content );
				if ( variable == null )
				{
					throw this.parseException( "Unknown variable '" + name + "'" );
				}
				t = variable.getType();
			}
			else
			{
				if ( scope.findVariable( name.content, true ) != null )
				{
					throw this.parseException( "Variable '" + name + "' already defined" );
				}

				// Create variable and add it to the scope
				variable = new Variable( name.content, t );
				scope.addVariable( variable );
			}

			this.readToken(); // name

			VariableReference lhs = new VariableReference( name.content, scope );
			Value rhs = null;

			if ( this.currentToken().equals( "=" ) )
			{
				this.readToken(); // =

				rhs = this.parseExpression( scope );

				if ( rhs == null )
				{
					throw this.parseException( "Expression expected" );
				}

				Type ltype = t.getBaseType();
				rhs = this.autoCoerceValue( t, rhs, scope );
				Type rtype = rhs.getType();

				if ( !Operator.validCoercion( ltype, rtype, "assign" ) )
				{
					throw this.parseException( "Cannot store " + rtype + " in " + name + " of type " + ltype );
				}

			}

			Assignment initializer = new Assignment( lhs, rhs );

			initializers.add( initializer);

			if ( this.currentToken().equals( "," ) )
			{
				this.readToken(); // ,

				if ( this.currentToken().equals( ";" ) )
				{
					throw this.parseException( "Identifier expected" );
				}
			}
		}

		if ( this.currentToken().equals( ";" ) )
		{
			this.readToken(); // ;
		}
		else
		{
			throw this.parseException( ";", this.currentToken() );
		}

		// Parse condition in context of scope

		Value condition =
			( this.currentToken().equals( ";" ) ) ?
			DataTypes.TRUE_VALUE : this.parseExpression( scope );

		if ( this.currentToken().equals( ";" ) )
		{
			this.readToken(); // ;
		}
		else
		{
			throw this.parseException( ";", this.currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw this.parseException( "\"for\" requires a boolean conditional expression" );
		}

		// Parse incrementers in context of scope

		List<ParseTreeNode> incrementers = new ArrayList<ParseTreeNode>();

		while ( !this.atEndOfFile() && !this.currentToken().equals( ")" ) )
		{
			Value value = parsePreIncDec( scope );
			if ( value != null )
			{
				incrementers.add( value );
			}
			else
			{
				value = this.parseVariableReference( scope );
				if ( !( value instanceof VariableReference ) )
				{
					throw this.parseException( "Variable reference expected" );
				}

				VariableReference ref = (VariableReference) value;
				Value lhs = this.parsePostIncDec( ref );

				if ( lhs == ref )
				{
					Assignment incrementer = parseAssignment( scope, ref );

					if ( incrementer != null )
					{
						incrementers.add( incrementer );
					}
					else
					{
						throw this.parseException( "Variable '" + ref.getName() + "' not incremented" );
					}
				}
				else 
				{
					incrementers.add( lhs );
				}
			}

			if ( this.currentToken().equals( "," ) )
			{
				this.readToken(); // ,

				if ( this.atEndOfFile() || this.currentToken().equals( ")" ) )
				{
					throw this.parseException( "Identifier expected" );
				}
			}
		}

		if ( this.currentToken().equals( ")" ) )
		{
			this.readToken(); // )
		}
		else
		{
			throw this.parseException( ")", this.currentToken() );
		}

		// Parse scope body
		this.parseLoopScope( scope, functionType, parentScope );

		return new JavaForLoop( scope, initializers, condition, incrementers );
	}

	private Scope parseLoopScope( final Type functionType, final VariableList varList, final BasicScope parentScope )
	{
		return this.parseLoopScope( new Scope( varList, parentScope ), functionType, parentScope );
	}

	private Scope parseLoopScope( final Scope result, final Type functionType, final BasicScope parentScope )
	{
		if ( this.currentToken().equals( "{" ) )
		{
			// Scope is a block

			this.readToken(); // {

			this.parseScope( result, functionType, parentScope, true, true );

			if ( this.currentToken().equals( "}" ) )
			{
				this.readToken(); // }
			}
			else
			{
				throw this.parseException( "}", this.currentToken() );
			}
		}
		else
		{
			// Scope is a single command
			ParseTreeNode command = this.parseCommand( functionType, result, false, true, true );
			if ( command == null )
			{
				if ( this.currentToken().equals( ";" ) )
				{
					this.readToken(); // ;
				}
				else
				{
					throw this.parseException( ";", this.currentToken() );
				}
			}
			else
			{
				result.addCommand( command, this );
			}
		}

		return result;
	}

	private Value parseNewRecord( final BasicScope scope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "new" ) )
		{
			return null;
		}

		this.readToken();

		if ( !this.parseIdentifier( this.currentToken().content ) )
		{
			throw this.parseException( "Record name", this.currentToken() );
		}

		String name = this.currentToken().content;
		Type type = scope.findType( name );

		if ( !( type instanceof RecordType ) )
		{
			throw this.parseException( "'" + name + "' is not a record type" );
		}

		RecordType target = (RecordType) type;

		this.readToken(); //name

		List<Value> params = new ArrayList<>();
		String [] names = target.getFieldNames();
		Type [] types = target.getFieldTypes();
		int param = 0;

		if ( this.currentToken().equals( "(" ) )
		{
			this.readToken(); //(

			while ( true )
			{
				if ( this.atEndOfFile() )
				{
					throw this.parseException( ")", this.currentToken() );
				}

				if ( this.currentToken().equals( ")" ) )
				{
					this.readToken(); // )
					break;
				}

				Type currentType;
				String errorMessageFieldName = "";

				if ( param < types.length )
				{
					currentType = types[param];
					errorMessageFieldName = " (" + names[param] + ")";
				}
				else
				{
					throw this.parseException( "Too many field initializers for record " + name );
				}

				Type expected = currentType.getBaseType();
				Value val;

				if ( this.currentToken().equals( "," ) )
				{
					val = DataTypes.VOID_VALUE;
				}
				else if ( this.currentToken().equals( "{" ) && expected instanceof AggregateType )
				{
					val = this.parseAggregateLiteral( scope, (AggregateType) expected );
				}
				else
				{
					val = this.parseExpression( scope );
				}

				if ( val == null )
				{
					throw this.parseException( "Expression expected for field #" + ( param + 1 ) + errorMessageFieldName );
				}

				if ( val != DataTypes.VOID_VALUE )
				{
					val = this.autoCoerceValue( types[param], val, scope );
					Type given = val.getType();
					if ( !Operator.validCoercion( expected, given, "assign" ) )
					{
						throw this.parseException( given + " found when " + expected + " expected for field #" + ( param + 1 ) + errorMessageFieldName );
					}
				}

				params.add( val );
				param++;

				if ( this.currentToken().equals( "," ) )
				{
					this.readToken(); // ,
				}
				else if ( !this.currentToken().equals( ")" ) )
				{
					throw this.parseException( ", or )", this.currentToken() );
				}
			}
		}

		return target.initialValueExpression( params );
	}

	private Value parseCall( final BasicScope scope )
	{
		return this.parseCall( scope, null );
	}

	private Value parseCall( final BasicScope scope, final Value firstParam )
	{
		if ( !"(".equals( this.nextToken() ) )
		{
			return null;
		}

		if ( !this.parseScopedIdentifier( this.currentToken().content ) )
		{
			return null;
		}

		Token name = this.currentToken();
		this.readToken(); //name

		List<Value> params = this.parseParameters( scope, firstParam );
		Function target = scope.findFunction( name.content, params );

		if ( target != null )
		{
			params = this.autoCoerceParameters( target, params, scope );
		}
		else
		{
			throw this.undefinedFunctionException( name.content, params );
		}

		FunctionCall call = new FunctionCall( target, params, this );

		return parsePostCall( scope, call );
	}

	private List<Value> parseParameters( final BasicScope scope, final Value firstParam )
	{
		if ( !this.currentToken().equals( "(" ) )
		{
			return null;
		}

		this.readToken(); //(

		List<Value> params = new ArrayList<Value>();
		if ( firstParam != null )
		{
			params.add( firstParam );
		}

		while ( true )
		{
			if ( this.atEndOfFile() )
			{
				throw this.parseException( ")", this.currentToken() );
			}

			if ( this.currentToken().equals( ")" ) )
			{
				this.readToken(); // )
				break;
			}

			Value val = this.parseExpression( scope );
			if ( val != null )
			{
				params.add( val );
			}

			if ( this.atEndOfFile() )
			{
				throw this.parseException( ")", this.currentToken() );
			}

			if ( !this.currentToken().equals( "," ) )
			{
				if ( !this.currentToken().equals( ")" ) )
				{
					throw this.parseException( ")", this.currentToken() );
				}
				continue;
			}

			this.readToken(); // ,

			if ( this.atEndOfFile() )
			{
				throw this.parseException( "parameter", this.currentToken() );
			}

			if ( this.currentToken().equals( ")" ) )
			{
				throw this.parseException( "parameter", this.currentToken() );
			}
		}

		return params;
	}

	private Value parsePostCall( final BasicScope scope, FunctionCall call )
	{
		Value result = call;
		while ( result != null && this.currentToken().equals( "." ) )
		{
			Variable current = new Variable( result.getType() );
			current.setExpression( result );

			result = this.parseVariableReference( scope, new VariableReference( current ) );
		}

		return result;
	}

	private Value parseInvoke( final BasicScope scope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "call" ) )
		{
			return null;
		}

		this.readToken(); // call

		Type type = this.parseType( scope, false );

		// You can omit the type, but then this function invocation
		// cannot be used in an expression

		if ( type == null )
		{
			type = DataTypes.VOID_TYPE;
		}

		Token current = this.currentToken();
		Value name = null;

		if ( current.equals( "(" ) )
		{
			name = this.parseExpression( scope );
			if ( name == null || !name.getType().equals( DataTypes.STRING_TYPE ) )
			{
				throw this.parseException( "String expression expected for function name" );
			}
		}
		else
		{
			name = this.parseVariableReference( scope );

			if ( !( name instanceof VariableReference ) )
			{
				throw this.parseException( "Variable reference expected for function name" );
			}
		}

		List<Value> params;

		if ( this.currentToken().equals( "(" ) )
		{
			params = parseParameters( scope, null );
		}
		else
		{
			throw this.parseException( "(", this.currentToken() );
		}

		FunctionInvocation call = new FunctionInvocation( scope, type, name, params, this );

		return parsePostCall( scope, call );
	}

	private Assignment parseAssignment( final BasicScope scope, final VariableReference lhs )
	{
		Token operStr = this.currentToken();
		if ( !operStr.equals( "=" ) &&
		     !operStr.equals( "+=" ) &&
		     !operStr.equals( "-=" ) &&
		     !operStr.equals( "*=" ) &&
		     !operStr.equals( "/=" ) &&
		     !operStr.equals( "%=" ) &&
		     !operStr.equals( "**=" ) &&
		     !operStr.equals( "&=" ) &&
		     !operStr.equals( "^=" ) &&
		     !operStr.equals( "|=" ) &&
		     !operStr.equals( "<<=" ) &&
		     !operStr.equals( ">>=" ) &&
		     !operStr.equals( ">>>=" ) )
		{
			return null;
		}

		Type ltype = lhs.getType().getBaseType();
		boolean isAggregate = ( ltype instanceof AggregateType );

		if ( isAggregate && !operStr.equals( "=" ) )
		{
			throw this.parseException( "Cannot use '" + operStr + "' on an aggregate" );
		}

		Operator oper = new Operator( operStr.content, this );
		this.readToken(); // oper

		Value rhs;

		if ( this.currentToken().equals( "{" ) )
		{
			if ( isAggregate )
			{
				rhs = this.parseAggregateLiteral( scope, (AggregateType) ltype );
			}
			else
			{
				throw this.parseException( "Cannot use an aggregate literal for type " + lhs.getType() );
			}
		}
		else
		{
			rhs = this.parseExpression( scope );
		}

		if ( rhs == null )
		{
			throw this.parseException( "Expression expected" );
		}

		rhs = this.autoCoerceValue( lhs.getRawType(), rhs, scope );
		if ( !oper.validCoercion( lhs.getType(), rhs.getType() ) )
		{
			String error =
				oper.isLogical() ?
				( oper + " requires an integer or boolean expression and an integer or boolean variable reference" ) :
				oper.isInteger() ?
				( oper + " requires an integer expression and an integer variable reference" ) :
				( "Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() );
			throw this.parseException( error );
		}

		Operator op = operStr.equals( "=" ) ? null : new Operator( operStr.substring( 0, operStr.length() - 1 ), this );

		return new Assignment( lhs, rhs, op );
	}

	private Value parseRemove( final BasicScope scope )
	{
		if ( !this.currentToken().equalsIgnoreCase( "remove" ) )
		{
			return null;
		}

		Value lhs = this.parseExpression( scope );

		if ( lhs == null )
		{
			throw this.parseException( "Bad 'remove' statement" );
		}

		return lhs;
	}

	private Value parsePreIncDec( final BasicScope scope )
	{
		if ( this.nextToken() == null )
		{
			return null;
		}

		Value lhs = null;
		String operStr = null;

		// --[VariableReference]
		// ++[VariableReference]

		if ( !this.currentToken().equals( "++" ) &&
		     !this.currentToken().equals( "--" ) )
		{
			return null;
		}

		operStr = this.currentToken().equals( "++" ) ? Parser.PRE_INCREMENT : Parser.PRE_DECREMENT;
		this.readToken(); // oper

		lhs = this.parseVariableReference( scope );
		if ( lhs == null )
		{
			throw this.parseException( "Variable reference expected" );
		}

		int ltype = lhs.getType().getType();
		if ( ltype != DataTypes.TYPE_INT && ltype != DataTypes.TYPE_FLOAT )
		{
			throw this.parseException( operStr + " requires a numeric variable reference" );
		}

		Operator oper = new Operator( operStr, this );

		return new IncDec( (VariableReference) lhs, oper );
	}

	private Value parsePostIncDec( final VariableReference lhs )
	{
		// [VariableReference]++
		// [VariableReference]--

		if ( !this.currentToken().equals( "++" ) &&
		     !this.currentToken().equals( "--" ) )
		{
			return lhs;
		}

		String operStr = this.currentToken().equals( "++" ) ? Parser.POST_INCREMENT : Parser.POST_DECREMENT;

		int ltype = lhs.getType().getType();
		if ( ltype != DataTypes.TYPE_INT && ltype != DataTypes.TYPE_FLOAT )
		{
			throw this.parseException( operStr + " requires a numeric variable reference" );
		}

		this.readToken(); // oper

		Operator oper = new Operator( operStr, this );

		return new IncDec( lhs, oper );
	}

	private Value parseExpression( final BasicScope scope )
	{
		return this.parseExpression( scope, null );
	}

	private Value parseExpression( final BasicScope scope, final Operator previousOper )
	{
		if ( this.currentToken().equals( ";" ) )
		{
			return null;
		}

		Value lhs = null;
		Value rhs = null;
		Operator oper = null;

		Token operator = this.currentToken();
		if ( operator.equals( "!" ) )
		{
			this.readToken(); // !
			if ( ( lhs = this.parseValue( scope ) ) == null )
			{
				throw this.parseException( "Value expected" );
			}

			lhs = this.autoCoerceValue( DataTypes.BOOLEAN_TYPE, lhs, scope );
			lhs = new Operation( lhs, new Operator( operator.content, this ) );
			if ( lhs.getType() != DataTypes.BOOLEAN_TYPE )
			{
				throw this.parseException( "\"!\" operator requires a boolean value" );
			}
		}
		else if ( operator.equals( "~" ) )
		{
			this.readToken(); // ~
			if ( ( lhs = this.parseValue( scope ) ) == null )
			{
				throw this.parseException( "Value expected" );
			}

			lhs = new Operation( lhs, new Operator( operator.content, this ) );
			if ( lhs.getType() != DataTypes.INT_TYPE && lhs.getType() != DataTypes.BOOLEAN_TYPE )
			{
				throw this.parseException( "\"~\" operator requires an integer or boolean value" );
			}
		}
		else if ( operator.equals( "-" ) )
		{
			// See if it's a negative numeric constant
			if ( ( lhs = this.parseValue( scope ) ) == null )
			{
				// Nope. Unary minus.
				this.readToken(); // -
				if ( ( lhs = this.parseValue( scope ) ) == null )
				{
					throw this.parseException( "Value expected" );
				}

				lhs = new Operation( lhs, new Operator( operator.content, this ) );
			}
		}
		else if ( operator.equals( "remove" ) )
		{
			this.readToken(); // remove

			lhs = this.parseVariableReference( scope );
			if ( !( lhs instanceof CompositeReference ) )
			{
				throw this.parseException( "Aggregate reference expected" );
			}

			lhs = new Operation( lhs, new Operator( operator.content, this ) );
		}
		else if ( ( lhs = this.parseValue( scope ) ) == null )
		{
			return null;
		}

		do
		{
			oper = this.parseOperator( this.currentToken() );

			if ( oper == null )
			{
				return lhs;
			}

			if ( previousOper != null && !oper.precedes( previousOper ) )
			{
				return lhs;
			}

			if ( this.currentToken().equals( ":" ) )
			{
				return lhs;
			}

			if ( this.currentToken().equals( "?" ) )
			{
				this.readToken(); // ?

				Value conditional = lhs;

				if ( conditional.getType() != DataTypes.BOOLEAN_TYPE )
				{
					throw this.parseException(
						"Non-boolean expression " + conditional + " (" + conditional.getType() + ")" );
				}

				if ( ( lhs = this.parseExpression( scope, null ) ) == null )
				{
					throw this.parseException( "Value expected in left hand side" );
				}

				if ( this.currentToken().equals( ":" ) )
				{
					this.readToken(); // :
				}
				else
				{
					throw this.parseException( ":", this.currentToken() );
				}

				if ( ( rhs = this.parseExpression( scope, null ) ) == null )
				{
					throw this.parseException( "Value expected" );
				}

				if ( !oper.validCoercion( lhs.getType(), rhs.getType() ) )
				{
					throw this.parseException( "Cannot choose between " + lhs + " (" + lhs.getType() + ") and " + rhs + " (" + rhs.getType() + ")" );
				}

				lhs = new TernaryExpression( conditional, lhs, rhs );
			}
			else
			{
				this.readToken(); //operator

				if ( ( rhs = this.parseExpression( scope, oper ) ) == null )
				{
					throw this.parseException( "Value expected" );
				}


				Type ltype = lhs.getType();
				Type rtype = rhs.getType();

				if ( oper.equals( "+" ) && ( ltype.equals( DataTypes.TYPE_STRING ) || rtype.equals( DataTypes.TYPE_STRING ) ) )
				{
					// String concatenation
					if ( !ltype.equals( DataTypes.TYPE_STRING ) )
					{
						lhs = this.autoCoerceValue( DataTypes.STRING_TYPE, lhs, scope );
					}
					if ( !rtype.equals( DataTypes.TYPE_STRING ) )
					{
						rhs = this.autoCoerceValue( DataTypes.STRING_TYPE, rhs, scope );
					}
					if ( lhs instanceof Concatenate )
					{
						Concatenate conc = (Concatenate) lhs;
						conc.addString( rhs );
					}
					else
					{
						lhs = new Concatenate( lhs, rhs );
					}
				}
				else
				{
					rhs = this.autoCoerceValue( ltype, rhs, scope );
					if ( !oper.validCoercion( ltype, rhs.getType() ) )
					{
						throw this.parseException( "Cannot apply operator " + oper + " to " + lhs + " (" + lhs.getType() + ") and " + rhs + " (" + rhs.getType() + ")" );
					}
					lhs = new Operation( lhs, rhs, oper );
				}
			}
		}
		while ( true );
	}

	private Value parseValue( final BasicScope scope )
	{
		if ( this.currentToken().equals( ";" ) )
		{
			return null;
		}

		Value result = null;

		// Parse parenthesized expressions
		if ( this.currentToken().equals( "(" ) )
		{
			this.readToken(); // (

			result = this.parseExpression( scope );
			if ( this.currentToken().equals( ")" ) )
			{
				this.readToken(); // )
			}
			else
			{
				throw this.parseException( ")", this.currentToken() );
			}
		}

		// Parse constant values
		// true and false are reserved words

		else if ( this.currentToken().equalsIgnoreCase( "true" ) )
		{
			this.readToken();
			result = DataTypes.TRUE_VALUE;
		}

		else if ( this.currentToken().equalsIgnoreCase( "false" ) )
		{
			this.readToken();
			result = DataTypes.FALSE_VALUE;
		}

		else if ( this.currentToken().equals( "__FILE__" ) )
		{
			this.readToken();
			result = new Value( String.valueOf( this.shortFileName ) );
		}

		// numbers
		else if ( ( result = this.parseNumber() ) != null )
		{
        }

		else if ( ( result = this.parseString( scope ) ) != null )
		{
		}

		else if ( ( result = this.parseTypedConstant( scope ) ) != null )
		{
		}

		else if ( ( result = this.parseNewRecord( scope ) ) != null )
		{
		}

		else if ( ( result = this.parseCatchValue( scope ) ) != null )
		{
        }

		else if ( ( result = this.parsePreIncDec( scope ) ) != null )
		{
			return result;
		}

		else if ( ( result = this.parseInvoke( scope ) ) != null )
		{
        }

		else if ( ( result = this.parseCall( scope, null ) ) != null )
		{
        }

		else
		{
			Token anchor = this.currentToken();

			Type baseType = this.parseType( scope, false );
			if ( baseType != null && baseType.getBaseType() instanceof AggregateType )
			{
				if ( this.currentToken().equals( "{" ) )
				{
					result = this.parseAggregateLiteral( scope, (AggregateType) baseType.getBaseType() );
				}
				else
				{
					throw this.parseException( "{", this.currentToken() );
				}
			}
			else
			{
				if ( baseType != null )
				{
					this.rewindBackTo( anchor );
				}
				if ( ( result = this.parseVariableReference( scope ) ) != null )
				{
                }
			}
		}

		while ( result != null && ( this.currentToken().equals( "." ) || this.currentToken().equals( "[" ) ) )
		{
			Variable current = new Variable( result.getType() );
			current.setExpression( result );

			result = this.parseVariableReference( scope, new VariableReference( current ) );
		}

		if ( result instanceof VariableReference )
		{
			VariableReference ref = (VariableReference) result;
			Assignment value = this.parseAssignment( scope, ref );
			return ( value != null ) ? value : this.parsePostIncDec( ref );
		}

		return result;
	}

	private Value parseNumber()
	{
		int sign = 1;

		if ( this.currentToken().equals( "-" ) )
		{
			String next = this.nextToken();

			if ( next == null )
			{
				return null;
			}

			if ( !next.equals( "." ) && !this.readIntegerToken( next ) )
			{
				// Unary minus
				return null;
			}

			sign = -1;
			this.readToken(); // Read -
		}

		if ( this.currentToken().equals( "." ) )
		{
			this.readToken();
			Token fraction = this.currentToken();

			if ( this.readIntegerToken( fraction.content ) )
			{
				this.readToken(); // integer
			}
			else
			{
				throw this.parseException( "numeric value", fraction );
			}

			return new Value( sign * StringUtilities.parseDouble( "0." + fraction ) );
		}

		Token integer = this.currentToken();
		if ( !this.readIntegerToken( integer.content ) )
		{
			return null;
		}

		this.readToken(); // integer

		if ( this.currentToken().equals( "." ) )
		{
			String fraction = this.nextToken();
			if ( !this.readIntegerToken( fraction ) )
			{
				return new Value( sign * StringUtilities.parseLong( integer.content ) );
			}

			this.readToken(); // .
			this.readToken(); // fraction

			return new Value( sign * StringUtilities.parseDouble( integer + "." + fraction ) );
		}

		return new Value( sign * StringUtilities.parseLong( integer.content ) );
	}

	private boolean readIntegerToken( final String token )
	{
		if ( token == null )
		{
			return false;
		}

		for ( int i = 0; i < token.length(); ++i )
		{
			if ( !Character.isDigit( token.charAt( i ) ) )
			{
				return false;
			}
		}

		return true;
	}

	private Value parseString( final BasicScope scope )
	{
		if ( !this.currentToken().equals( "\"" ) &&
		     !this.currentToken().equals( "'" ) &&
		     !this.currentToken().equals( "`" ) )
		{
			return null;
		}

		this.clearCurrentToken();

		// Directly work with currentLine - ignore any "tokens" you meet until
		// the string is closed

		char startCharacter = this.restOfLine().charAt( 0 );
		char stopCharacter = startCharacter;
		boolean template = startCharacter == '`';

		Concatenate conc = null;
		StringBuilder resultString = new StringBuilder();
		for ( int i = 1; ; ++i )
		{
			final String line = this.restOfLine();

			if ( i == line.length() )
			{
				if ( i == 0 && this.currentIndex == this.currentLine.offset && this.currentLine.content != null )
				{
					// Empty lines are OK.
					this.currentLine = this.currentLine.nextLine;
					this.currentIndex = this.currentLine.offset;
					i = -1;
					continue;
				}

				// Plain strings can't span lines
				throw this.parseException( "No closing " + stopCharacter + " found" );
			}

			char ch = line.charAt( i );

			// Handle escape sequences
			if ( ch == '\\' )
			{
				i = this.parseEscapeSequence( resultString, i );
				continue;
			}

			// Handle template substitutions
			if ( template && ch == '{' )
			{
				// Move the current token to the expression
				this.currentToken = this.currentLine.makeToken( ++i );
				this.readToken(); // read the string so far, including the {

				Value rhs = this.parseExpression( scope );

				if ( rhs == null )
				{
					throw this.parseException( "Expression expected" );
				}

				// Set i to -1 so that it is set to zero by the loop, as the
				// currentLine has been shortened.
				i = -1;

				// Skip comments before the next token, look at what it is, then
				// discard said token.
				if ( this.currentToken().equals( "}" ) )
				{
					// Increment manually to not skip whitespace after the curly brace.
					++i; // }
				}
				else
				{
					throw this.parseException( "}", this.currentToken() );
				}

				this.clearCurrentToken();

				Value lhs = new Value( resultString.toString() );
				if ( conc == null )
				{
					conc = new Concatenate( lhs, rhs );
				}
				else
				{
					conc.addString( lhs );
					conc.addString( rhs );
				}
				
				resultString.setLength( 0 );
				continue;
			}

			if ( ch == stopCharacter )
			{
				this.currentToken = this.currentLine.makeToken( i + 1 ); //+ 1 to get rid of stop character token
				this.readToken();

				Value result = new Value( resultString.toString() );

				if ( conc == null )
				{
					return result;
				}
				else
				{
					conc.addString( result );
					return conc;
				}
			}
			resultString.append( ch );
		}
	}

	private int parseEscapeSequence( final StringBuilder resultString, int i )
	{
		final String line = this.restOfLine();

		if ( ++i == line.length() )
		{
			resultString.append( '\n' );
			this.currentLine.makeToken( i );
			this.currentLine = this.currentLine.nextLine;
			this.currentIndex = this.currentLine.offset;
			return -1;
		}

		char ch = line.charAt( i );

		switch ( ch )
		{
		case 'n':
			resultString.append( '\n' );
			break;

		case 'r':
			resultString.append( '\r' );
			break;

		case 't':
			resultString.append( '\t' );
			break;

		case 'x':
			try
			{
				int hex08 = Integer.parseInt( line.substring( i + 1, i + 3 ), 16 );
				resultString.append( (char) hex08 );
				i += 2;
			}
			catch ( IndexOutOfBoundsException | NumberFormatException e )
			{
				throw this.parseException( "Hexadecimal character escape requires 2 digits" );
			}
			break;

		case 'u':
			try
			{
				int hex16 = Integer.parseInt( line.substring( i + 1, i + 5 ), 16 );
				resultString.append( (char) hex16 );
				i += 4;
			}
			catch ( IndexOutOfBoundsException | NumberFormatException e )
			{
				throw this.parseException( "Unicode character escape requires 4 digits" );
			}
			break;

		default:
			if ( Character.isDigit( ch ) )
			{
				try
				{
					int octal = Integer.parseInt( line.substring( i, i + 3 ), 8 );
					resultString.append( (char) octal );
					i += 2;
					break;
				}
				catch ( IndexOutOfBoundsException | NumberFormatException e )
				{
					throw this.parseException( "Octal character escape requires 3 digits" );
				}
			}
			resultString.append( ch );
		}

		return i;
	}

	private Value parseLiteral( Type type, String element )
	{
		Value value = DataTypes.parseValue( type, element, false );
		if ( value == null )
		{
			throw this.parseException( "Bad " + type.toString() + " value: \"" + element + "\"" );
		}

		if ( !StringUtilities.isNumeric( element ) )
		{
			String fullName = value.toString();
			if ( !element.equalsIgnoreCase( fullName ) )
			{
				String s1 = CharacterEntities.escape( StringUtilities.globalStringReplace( element, ",", "\\," ).replaceAll("(?<= ) ", "\\\\ " ) );
				String s2 = CharacterEntities.escape( StringUtilities.globalStringReplace( fullName, ",", "\\," ).replaceAll("(?<= ) ", "\\\\ " ) );
				List<String> names = new ArrayList<String>();
				if ( type == DataTypes.ITEM_TYPE )
				{
					int itemId = (int)value.contentLong;
					String name = ItemDatabase.getItemName( itemId );
					int[] ids = ItemDatabase.getItemIds( name, 1, false );
					for ( int id : ids )
					{
						String s3 = "$item[[" + id + "]" + name + "]";
						names.add( s3 );
					}
				}
				else if ( type == DataTypes.EFFECT_TYPE )
				{
					int effectId = (int)value.contentLong;
					String name = EffectDatabase.getEffectName( effectId );
					int[] ids = EffectDatabase.getEffectIds( name, false );
					for ( int id : ids )
					{
						String s3 = "$effect[[" + id + "]" + name + "]";
						names.add( s3 );
					}
				}
				else if ( type == DataTypes.MONSTER_TYPE )
				{
					int monsterId = (int)value.contentLong;
					String name = MonsterDatabase.findMonsterById( monsterId ).getName();
					int[] ids = MonsterDatabase.getMonsterIds( name, false );
					for ( int id : ids )
					{
						String s3 = "$monster[[" + id + "]" + name + "]";
						names.add( s3 );
					}
				}
				else if ( type == DataTypes.SKILL_TYPE )
				{
					int skillId = (int)value.contentLong;
					String name = SkillDatabase.getSkillName( skillId );
					int[] ids = SkillDatabase.getSkillIds( name, false );
					for ( int id : ids )
					{
						String s3 = "$skill[[" + id + "]" + name + "]";
						names.add( s3 );
					}
				}

				if ( names.size() > 1 )
				{
					ScriptException ex = this.parseException2( "Multiple matches for \"" + s1 + "\"; using \"" + s2 + "\".",
										   "Clarify by using one of:" );
					RequestLogger.printLine( ex.getMessage() );
					for ( String name : names )
					{
						RequestLogger.printLine( name );
					}
				}
				else
				{
					ScriptException ex = this.parseException( "Changing \"" + s1 + "\" to \"" + s2 + "\" would get rid of this message." );
					RequestLogger.printLine( ex.getMessage() );
				}
			}
		}

		return value;
	}

	private Value parseTypedConstant( final BasicScope scope )
	{
		if ( !this.currentToken().equals( "$" ) )
		{
			return null;
		}

		this.readToken(); // read $

		Token name = this.currentToken();
		Type type = scope.findType( name.content );
		boolean plurals = false;

		if ( type == null )
		{
			StringBuilder buf = new StringBuilder( name.content );
			int length = name.length();

			if ( name.endsWith( "ies" ) )
			{
				buf.delete( length - 3, length );
				buf.insert( length - 3, "y" );
			}
			else if ( name.endsWith( "es" ) )
			{
				buf.delete( length - 2, length );
			}
			else if ( name.endsWith( "s" ) )
			{
				buf.deleteCharAt( length - 1 );
			}
			else if ( name.endsWith( "a" ) )
			{
				buf.deleteCharAt( length - 1 );
				buf.insert( length - 1, "um" );
			}
			else
			{
				throw this.parseException( "Unknown type " + name );
			}

			type = scope.findType( buf.toString() );

			plurals = true;
		}

		this.readToken();

		if ( type == null )
		{
			throw this.parseException( "Unknown type " + name );
		}

		if ( !type.isPrimitive() )
		{
			throw this.parseException( "Non-primitive type " + name );
		}

		if ( this.currentToken().equals( "[" ) )
		{
			this.readToken(); // read [
		}
		else
		{
			throw this.parseException( "[", this.currentToken() );
		}

		if ( plurals )
		{
			Value value = this.parsePluralConstant( scope, type );
			if ( value != null )
			{
				return value;	// explicit list of values
			}
			value = type.allValues();
			if ( value != null )
			{
				return value;	// implicit enumeration
			}
			throw this.parseException( "Can't enumerate all " + name );
		}

		StringBuilder resultString = new StringBuilder();

		int level = 1;
		for ( int i = 0; ; ++i )
		{
			final String line = this.restOfLine();

			if ( i == line.length() )
			{
				throw this.parseException( "No closing ] found" );
			}

			char c = line.charAt( i );
			if ( c == '\\' )
			{
				if ( ++i == line.length() )
				{
					throw this.parseException( "No closing ] found" );
				}

				resultString.append( line.charAt( i ) );
			}
			else if ( c == '[' )
			{
				level++;
				resultString.append( c );
			}
			else if ( c == ']' )
			{
				if ( --level > 0 )
				{
					resultString.append( c );
					continue;
				}

				if ( i > 0 )
				{
					this.currentLine.makeToken( i );
					this.currentIndex += i;
				}
				this.readToken(); // read ]
				String input = resultString.toString().trim();
				return parseLiteral( type, input );
			}
			else
			{
				resultString.append( c );
			}
		}
	}

	private PluralValue parsePluralConstant( final BasicScope scope, final Type type )
	{
		// Directly work with currentLine - ignore any "tokens" you meet until
		// the string is closed

		List<Value> list = new ArrayList<>();
		int level = 1;
		boolean slash = false;

		StringBuilder resultString = new StringBuilder();
		for ( int i = 0; ; ++i )
		{
			final String line = this.restOfLine();

			if ( i == line.length() )
			{
				if ( i > 0 )
				{
					this.currentLine.makeToken( i );
					this.currentIndex += i;
				}

				if ( slash )
				{
					slash = false;
					resultString.append( '/' );
				}

				if ( this.currentLine.content == null )
				{
					throw this.parseException( "No closing ] found" );
				}

				this.currentLine = this.currentLine.nextLine;
				this.currentIndex = this.currentLine.offset;
				i = -1;
				continue;
			}

			char ch = line.charAt( i );

			// Handle escape sequences
			if ( ch == '\\' )
			{
				i = this.parseEscapeSequence( resultString, i );
				continue;
			}

			// Potentially handle comments
			// If we've already seen a slash
			if ( slash )
			{
				slash = false;
				if ( ch == '/' )
				{
					this.currentLine.makeToken( i - 1 );
					this.currentIndex += i - 1;
					// Throw away the rest of the line
					this.currentLine.makeComment( this.restOfLine().length() );
					this.currentIndex += this.restOfLine().length();
					i = -1;
					continue;
				}
				resultString.append( '/' );
			}
			else if ( ch == '/' )
			{
				slash = true;
				continue;
			}

			// Allow start char without escaping
			if ( ch == '[' )
			{
				level++;
				resultString.append( ch );
				continue;
			}

			// Match non-initial start char
			if ( ch == ']' && --level > 0 )
			{
				resultString.append( ch );
				continue;
			}

			if ( ch != ']' && ch != ',' )
			{
				resultString.append( ch );
				continue;
			}

			// Add a new element to the list
			String element = resultString.toString().trim();
			resultString.setLength( 0 );
			if ( element.length() != 0 )
			{
				list.add( this.parseLiteral( type, element ) );
			}

			if ( ch == ']' )
			{
				if ( i > 0 )
				{
					this.currentLine.makeToken( i );
					this.currentIndex += i;
				}
				this.readToken(); // read ]
				if ( list.size() == 0 )
				{
					// Empty list - caller will interpret this specially
					return null;
				}
				return new PluralValue( type, list );
			}
		}
	}

	private Operator parseOperator( final Token oper )
	{
		if ( !this.isOperator( oper.content ) )
		{
			return null;
		}

		return new Operator( oper.content, this );
	}

	private boolean isOperator( final String oper )
	{
		return oper.equals( "!" ) ||
			oper.equals( "?" ) ||
			oper.equals( ":" ) ||
			oper.equals( "*" ) ||
			oper.equals( "**" ) ||
			oper.equals( "/" ) ||
			oper.equals( "%" ) ||
			oper.equals( "+" ) ||
			oper.equals( "-" ) ||
			oper.equals( "&" ) ||
			oper.equals( "^" ) ||
			oper.equals( "|" ) ||
			oper.equals( "~" ) ||
			oper.equals( "<<" ) ||
			oper.equals( ">>" ) ||
			oper.equals( ">>>" ) ||
			oper.equals( "<" ) ||
			oper.equals( ">" ) ||
			oper.equals( "<=" ) ||
			oper.equals( ">=" ) ||
			oper.equals( "==" ) ||
			oper.equals( Parser.APPROX ) ||
			oper.equals( "!=" ) ||
			oper.equals( "||" ) ||
			oper.equals( "&&" ) ||
			oper.equals( "contains" ) ||
			oper.equals( "remove" );
	}

	private Value parseVariableReference( final BasicScope scope )
	{
		if ( !this.parseIdentifier( this.currentToken().content ) )
		{
			return null;
		}

		Token name = this.currentToken();
		Variable var = scope.findVariable( name.content, true );

		if ( var == null )
		{
			throw this.parseException( "Unknown variable '" + name + "'" );
		}

		this.readToken(); // read name

		return this.parseVariableReference( scope, new VariableReference( var ) );
	}

	/**
	 * Look for an index/key, and return the corresponding data, expecting {@code var} to be a
	 * {@link AggregateType}/{@link RecordType}, e.g., {@code map.key}, {@code array[0]}.
	 *
	 * <p>May also return a {@link FunctionCall} if the chain ends with/is a function call,
	 * e.g., {@code var.function()}.
	 *
	 * <p>There may also be nothing, in which case the submitted variable reference is returned
	 * as is.
	 */
	private Value parseVariableReference( final BasicScope scope, final VariableReference var )
	{
		VariableReference current = var;
		Type type = var.getType();
		List<Value> indices = new ArrayList<Value>();

		boolean parseAggregate = this.currentToken().equals( "[" );

		while ( this.currentToken().equals( "[" ) ||
		        this.currentToken().equals( "." ) ||
		        parseAggregate && this.currentToken().equals( "," ) )
		{
			Value index;

			type = type.getBaseType();

			if ( this.currentToken().equals( "[" ) || this.currentToken().equals( "," ) )
			{
				this.readToken(); // read [ or ,
				parseAggregate = true;

				if ( !( type instanceof AggregateType ) )
				{
					if ( indices.isEmpty() )
					{
						throw this.parseException( "Variable '" + var.getName() + "' cannot be indexed" );
					}
					else
					{
						throw this.parseException( "Too many keys for '" + var.getName() + "'" );
					}
				}

				AggregateType atype = (AggregateType) type;
				index = this.parseExpression( scope );
				if ( index == null )
				{
					throw this.parseException( "Index for '" + current.getName() + "' expected" );
				}

				if ( !index.getType().getBaseType().equals( atype.getIndexType().getBaseType() ) )
				{
					throw this.parseException(
						"Index for '" + current.getName() + "' has wrong data type " + "(expected " + atype.getIndexType() + ", got " + index.getType() + ")" );
				}

				type = atype.getDataType();
			}
			else
			{
				this.readToken(); // read .

				// Maybe it's a function call with an implied "this" parameter.

				if ( "(".equals( this.nextToken() ) )
				{
					return this.parseCall( scope, current );
				}

				type = type.asProxy();
				if ( !( type instanceof RecordType ) )
				{
					throw this.parseException( "Record expected" );
				}

				RecordType rtype = (RecordType) type;

				Token field = this.currentToken();
				if ( this.parseIdentifier( field.content ) )
				{
					this.readToken(); // read name
				}
				else
				{
					throw this.parseException( "Field name expected" );
				}

				index = rtype.getFieldIndex( field.content );
				if ( index != null )
				{
					type = rtype.getDataType( index );
				}
				else
				{
					throw this.parseException( "Invalid field name '" + field + "'" );
				}
			}

			indices.add( index );

			if ( parseAggregate && this.currentToken().equals( "]" ) )
			{
				this.readToken(); // read ]
				parseAggregate = false;
			}

			current = new CompositeReference( current.target, indices, this );
		}

		if ( parseAggregate )
		{
			throw this.parseException( "]", this.currentToken() );
		}

		return current;
	}

	private String parseDirective( final String directive )
	{
		if ( !this.currentToken().equalsIgnoreCase( directive ) )
		{
			return null;
		}

		this.readToken(); //directive

		if ( this.atEndOfFile() )
		{
			throw this.parseException( "<", this.currentToken() );
		}

		// We called atEndOfFile(), which calls currentToken() to trim whitespace
		// and skip comments. Remove the resulting token.

		this.clearCurrentToken();

		String resultString = null;
		int endIndex = -1;
		final String line = this.restOfLine();
		final char firstChar = line.charAt( 0 );

		for ( char ch : new char[] { '<', '\'', '"' } )
		{
			if ( ch != firstChar )
			{
				continue;
			}

			if ( ch == '<' )
			{
				ch = '>';
			}

			endIndex = line.indexOf( ch, 1 );

			if ( endIndex == -1 )
			{
				throw this.parseException( "No closing " + ch + " found" );
			}

			resultString = line.substring( 1, endIndex );
			// +1 to include and get rid of '>', '\'' or '"' token
			this.currentToken = this.currentLine.makeToken( endIndex + 1 );
			this.readToken();

			break;
		}

		if ( endIndex == -1 )
		{
			endIndex = line.indexOf( ";" );

			if ( endIndex == -1 )
			{
				endIndex = line.length();
			}

			resultString = line.substring( 0, endIndex );
			this.currentToken = this.currentLine.makeToken( endIndex );
			this.readToken();
		}

		if ( this.currentToken().equals( ";" ) )
		{
			this.readToken(); //read ;
		}

		return resultString;
	}

	private void parseScriptName()
	{
		String resultString = this.parseDirective( "script" );
		if ( this.scriptName == null )
		{
			this.scriptName = resultString;
		}
	}

	private void parseNotify()
	{
		String resultString = this.parseDirective( "notify" );
		if ( this.notifyRecipient == null )
		{
			this.notifyRecipient = resultString;
		}
	}

	private void parseSince()
	{
		String revision = this.parseDirective( "since" );
		if ( revision != null )
		{
			// enforce "since" directives RIGHT NOW at parse time
			this.enforceSince( revision );
		}
	}

	private String parseImport()
	{
		return this.parseDirective( "import" );
	}

	// **************** Tokenizer *****************

	private static final char BOM = '\ufeff';

	/**
	 * Returns {@link #currentToken} if non-null. Otherwise, moves in front of
	 * the next non-comment token that we can find, before assigning it to
	 * {@link #currentToken} and returning it.
	 *
	 * <p>
	 * Never returns {@code null}.
	 */
	private Token currentToken()
	{
		// If we've already parsed a token, return it
		if ( this.currentToken != null )
		{
			return this.currentToken;
		}

		boolean inMultiLineComment = false;

		// Repeat until we get a token
		while ( true )
		{
			// at "end of file"
			if ( this.currentLine.content == null )
			{
				// will make an "end of file" token
				return this.currentToken = this.currentLine.makeToken( 0 );
			}

			final String restOfLine = this.restOfLine();

			if ( inMultiLineComment )
			{
				final int commentEnd = restOfLine.indexOf( "*/" );

				if ( commentEnd == -1 )
				{
					this.currentLine.makeComment( restOfLine.length() );

					this.currentLine = this.currentLine.nextLine;
					this.currentIndex = this.currentLine.offset;
				}
				else
				{
					this.currentToken = this.currentLine.makeComment( commentEnd + 2 );
					this.readToken();
					inMultiLineComment = false;
				}

				continue;
			}

			if ( restOfLine.length() == 0 )
			{
				this.currentLine = this.currentLine.nextLine;
				this.currentIndex = this.currentLine.offset;
				continue;
			}

			// "#" was "supposed" to start a whole-line comment, but a bad implementation made it
			// act just like "//"

			// "//" starts a comment which consumes the rest of the line
			if ( restOfLine.startsWith( "#" ) ||
			     restOfLine.startsWith( "//" ) )
			{
				this.currentLine.makeComment( restOfLine.length() );

				this.currentLine = this.currentLine.nextLine;
				this.currentIndex = this.currentLine.offset;
				continue;
			}

			// "/*" starts a comment which is terminated by "*/"
			if ( restOfLine.startsWith( "/*" ) )
			{
				final int commentEnd = restOfLine.indexOf( "*/", 2 );

				if ( commentEnd == -1 )
				{
					this.currentLine.makeComment( restOfLine.length() );

					this.currentLine = this.currentLine.nextLine;
					this.currentIndex = this.currentLine.offset;
					inMultiLineComment = true;
				}
				else
				{
					this.currentToken = this.currentLine.makeComment( commentEnd + 2 );
					this.readToken();
				}

				continue;
			}

			return this.currentToken = this.currentLine.makeToken( this.tokenLength( restOfLine ) );
		}
	}

	/**
	 * Calls {@link #currentToken()} to make sure we are currently in front of an unread
	 * token. Then, returns a string version of the next token that can be found after that.
	 *
	 * @return the content of the next token to come after the token we are currently in front of,
	 *         or {@code null} if we are at the end of the file.
	 */
	private String nextToken()
	{
		int offset = this.currentToken().restOfLineStart;
		Line line = this.currentLine;
		boolean inMultiLineComment = false;

		while ( true )
		{
			// at "end of file"
			if ( line.content == null )
			{
				return null;
			}

			final String restOfLine = line.substring( offset ).trim();

			if ( inMultiLineComment )
			{
				final int commentEnd = restOfLine.indexOf( "*/" );

				if ( commentEnd == -1 )
				{
					line = line.nextLine;
					offset = line.offset;
				}
				else
				{
					offset += commentEnd + 2;
					inMultiLineComment = false;
				}

				continue;
			}

			// "#" was "supposed" to start a whole-line comment, but a bad implementation made it
			// act just like "//"

			if ( restOfLine.length() == 0 ||
			     restOfLine.startsWith( "#" ) ||
			     restOfLine.startsWith( "//" ) )
			{
				line = line.nextLine;
				offset = line.offset;
				continue;
			}

			if ( restOfLine.startsWith( "/*" ) )
			{
				offset += 2;
				inMultiLineComment = true;
				continue;
			}

			return restOfLine.substring( 0, this.tokenLength( restOfLine ) );
		}
	}

	/**
	 * Forget every token up to {@code destinationToken}, so that we can resume parsing from there.
	 */
	private void rewindBackTo( final Token destinationToken )
	{
		this.currentToken();

		while ( this.currentToken != destinationToken )
		{
			this.currentLine.tokens.removeLast();

			while ( this.currentLine.tokens.isEmpty() )
			{
				// Don't do null checks. If previousLine is null, it means we never saw the
				// destination token, meaning we'd want to throw an error anyway.
				this.currentLine = this.currentLine.previousLine;
			}

			this.currentToken = this.currentLine.tokens.getLast();
			this.currentIndex = this.currentToken.offset;
		}
	}

	/**
	 * If we are not at the end of the file, null out
	 * {@link #currentToken} (allowing a new one to be gathered
	 * next time we call {@link #currentToken()}), and move
	 * {@link #currentIndex} forward.
	 */
	private void readToken()
	{
		// at "end of file"
		if ( this.currentToken().getLine().content == null )
		{
			return;
		}

		this.currentIndex = this.currentToken.restOfLineStart;
		this.currentToken = null;
	}

	/**
	 * If we have an unread token saved in {@link #currentToken}, null the field,
	 * and delete it from its {@link Line#tokens}, effectively forgetting that we saw it.
	 * <p>
	 * This method is made for parsing methods that manipulate lines character-by-character,
	 * and need to create Tokens of custom lengths.
	 */
	private void clearCurrentToken()
	{
		if ( this.currentToken != null )
		{
			this.currentToken = null;
			this.currentLine.tokens.removeLast();
		}
	}

	private int tokenLength( final String s )
	{
		int result;
		if ( s == null )
		{
			return 0;
		}

		for ( result = 0; result < s.length(); result++ )
		{
			if ( result + 3 < s.length() && this.tokenString( s.substring( result, result + 4 ) ) )
			{
				return result == 0 ? 4 : result;
			}

			if ( result + 2 < s.length() && this.tokenString( s.substring( result, result + 3 ) ) )
			{
				return result == 0 ? 3 : result;
			}

			if ( result + 1 < s.length() && this.tokenString( s.substring( result, result + 2 ) ) )
			{
				return result == 0 ? 2 : result;
			}

			if ( this.tokenChar( s.charAt( result ) ) )
			{
				return result == 0 ? 1 : result;
			}
		}

		return result; //== s.length()
	}

	private boolean tokenChar( char ch )
	{
		switch ( ch )
		{
		case ' ':
		case '\t':
		case '.':
		case ',':
		case '{':
		case '}':
		case '(':
		case ')':
		case '$':
		case '!':
		case '~':
		case '+':
		case '-':
		case '=':
		case '"':
		case '`':
		case '\'':
		case '*':
		case '/':
		case '%':
		case '|':
		case '^':
		case '&':
		case '[':
		case ']':
		case ';':
		case '<':
		case '>':
		case '?':
		case ':':
		case '\u2248':
			return true;
		}
		return false;
	}

	private boolean tokenString( final String s )
	{
		return Parser.multiCharTokens.contains( s );
	}

	/**
	 * Returns the content of {@link #currentLine} starting at {@link #currentIndex}.
	 */
	private String restOfLine()
	{
		return this.currentLine.substring( this.currentIndex );
	}

	/**
	 * Calls {@link #currentToken()} in order to skip any
	 * comment or whitespace we would be in front of,
	 * then return whether or not we reached the end
	 * of the file.
	 */
	private boolean atEndOfFile()
	{
		this.currentToken();

		return this.currentLine.content == null;
	}

	public final class Line
	{
		private final String content;
		private final int lineNumber;
		private final int offset;

		private final Deque<Token> tokens = new LinkedList<>();

		private final Line previousLine;
		/* Not made final to avoid a possible StackOverflowError. Do not modify. */
		private Line nextLine = null;

		private Line( final LineNumberReader commandStream )
		{
			this( commandStream, null );
		}

		private Line( final LineNumberReader commandStream, final Line previousLine )
		{
			this.previousLine = previousLine;
			if ( previousLine != null )
			{
				previousLine.nextLine = this;
			}

			int offset = 0;
			String line;

			try
			{
				line = commandStream.readLine();
			}
			catch ( IOException e )
			{
				// This should not happen. Therefore, print a stack trace for debug purposes.
				StaticEntity.printStackTrace( e );
				line = null;
			}

			if ( line == null )
			{
				// We are the "end of file" (or there was an IOException when reading)
				this.content = null;
				this.lineNumber = this.previousLine != null ? this.previousLine.lineNumber : 0;
				this.offset = this.previousLine != null ? this.previousLine.offset : 0;
				return;
			}

			// If the line starts with a Unicode BOM, remove it.
			if ( line.length() > 0 &&
			     line.charAt( 0 ) == Parser.BOM )
			{
				line = line.substring( 1 );
				offset += 1;
			}

			// Remove whitespace at front and end
			final String trimmed = line.trim();

			if ( !trimmed.isEmpty() )
			{
				// While the more "obvious" solution would be to use line.indexOf( trimmed ), we
				// know that the only difference between these strings is leading/trailing
				// whitespace.
				//
				// There are two cases:
				//  1. `trimmed` is empty, in which case `line` was entirely composed of whitespace.
				//  2. `trimmed` is non-empty. The first non-whitespace character in `line`
				//     indicates the start of `trimmed`.
				//
				// This is more efficient in that we don't need to confirm that the rest of
				// `trimmed` is present in `line`.

				final int ltrim = line.indexOf( trimmed.charAt( 0 ) );
				offset += ltrim;
			}

			line = trimmed;

			this.content = line;
			this.lineNumber = commandStream.getLineNumber();
			this.offset = offset;
		}

		private String substring( final int beginIndex )
		{
			if ( this.content == null )
			{
				return "";
			}

			// subtract "offset" from beginIndex, since we already removed it
			return this.content.substring( beginIndex - this.offset );
		}

		private Token makeToken( final int tokenLength )
		{
			final Token newToken = new Token( tokenLength );
			this.tokens.addLast( newToken );
			return newToken;
		}

		private Token makeComment( final int commentLength )
		{
			final Token newToken = new Comment( commentLength );
			this.tokens.addLast( newToken );
			return newToken;
		}

		@Override
		public String toString()
		{
			return this.content;
		}

		public class Token
		{
			final int offset;
			final String content;
			final String followingWhitespace;
			final int restOfLineStart;

			private Token( final int tokenLength )
			{
				if ( !Line.this.tokens.isEmpty() )
				{
					offset = Line.this.tokens.getLast().restOfLineStart;
				}
				else
				{
					offset = Line.this.offset;
				}

				final String lineRemainder;

				if ( Line.this.content == null )
				{
					// At end of file
					this.content = ";";
					// Going forward, we can just assume lineRemainder is an
					// empty string.
					lineRemainder = "";
				}
				else
				{
					final String lineRemainderWithToken = Line.this.substring( offset );

					this.content = lineRemainderWithToken.substring( 0, tokenLength );
					lineRemainder = lineRemainderWithToken.substring( tokenLength );
				}

				// As in Line(), this is more efficient than lineRemainder.indexOf( lineRemainder.trim() ).
				String trimmed = lineRemainder.trim();
				final int lTrim = trimmed.isEmpty() ? 0 : lineRemainder.indexOf( trimmed.charAt( 0 ) );

				this.followingWhitespace = lineRemainder.substring( 0, lTrim );

				this.restOfLineStart = offset + tokenLength + lTrim;
			}

			/** The Line in which this token exists */
			final Line getLine()
			{
				return Line.this;
			}

			public boolean equals( final String s )
			{
				return this.content.equals( s );
			}

			public boolean equalsIgnoreCase( final String s )
			{
				return this.content.equalsIgnoreCase( s );
			}

			public int length()
			{
				return this.content.length();
			}

			public String substring( final int beginIndex )
			{
				return this.content.substring( beginIndex );
			}

			public String substring( final int beginIndex, final int endIndex )
			{
				return this.content.substring( beginIndex, endIndex );
			}

			public boolean endsWith( final String suffix )
			{
				return this.content.endsWith( suffix );
			}

			@Override
			public String toString()
			{
				return this.content;
			}
		}

		private class Comment
			extends Token
		{
			private Comment( final int commentLength )
			{
				super( commentLength );
			}
		}
	}

	public List<Token> getTokens()
	{
		final List<Token> result = new LinkedList<>();

		Line line = this.currentLine;

		// Go back to the start
		while ( line != null &&
		        line.previousLine != null )
		{
			line = line.previousLine;
		}

		while ( line != null &&
		        line.content != null )
		{
			for ( final Token token : line.tokens )
			{
				result.add( token );
			}

			line = line.nextLine;
		}

		return result;
	}

	public List<String> getTokensContent()
	{
		return this.getTokens()
			.stream().map( token -> token.content ).collect( Collectors.toList() );
	}

	// **************** Parse errors *****************

	private ScriptException parseException( final String expected, final Token found )
	{
		String foundString = found.content;

		if ( found.getLine().content == null )
		{
			foundString = "end of file";
		}

		return this.parseException( "Expected " + expected + ", found " + foundString );
	}

	private ScriptException parseException( final String message )
	{
		return new ScriptException( message + " " + this.getLineAndFile() );
	}

	private ScriptException parseException2( final String message1, final String message2 )
	{
		return new ScriptException( message1 + " " + this.getLineAndFile() + " " + message2 );
	}

	private ScriptException undefinedFunctionException( final String name, final List<Value> params )
	{
		return this.parseException( Parser.undefinedFunctionMessage( name, params ) );
	}

	private ScriptException multiplyDefinedFunctionException( final Function f )
	{
        String buffer = "Function '" +
                f.getSignature() +
                "' defined multiple times.";
        return this.parseException( buffer );
	}

	private ScriptException overridesLibraryFunctionException( final Function f )
	{
        String buffer = "Function '" +
                f.getSignature() +
                "' overrides a library function.";
        return this.parseException( buffer );
	}

	private ScriptException varargClashException( final Function f, final Function clash )
	{
        String buffer = "Function '" +
                f.getSignature() +
                "' clashes with existing function '" +
                clash.getSignature() +
                "'.";
        return this.parseException( buffer );
	}

	public final ScriptException sinceException( String current, String target, boolean targetIsRevision )
	{
		String template;
		if ( targetIsRevision )
		{
			template = "'%s' requires revision r%s of kolmafia or higher (current: r%s).  Up-to-date builds can be found at https://ci.kolmafia.us/.";
		}
		else
		{
			template = "'%s' requires version %s of kolmafia or higher (current: %s).  Up-to-date builds can be found at https://ci.kolmafia.us/.";
		}

		return new ScriptException( String.format( template, this.shortFileName, target, current ) );
	}

	public static String undefinedFunctionMessage( final String name, final List<Value> params )
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append( "Function '" );
		Parser.appendFunctionCall( buffer, name, params );
		buffer.append( "' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts." );
		return buffer.toString();
	}

	private void enforceSince( String revision )
	{
		try
		{
			if ( revision.startsWith( "r" ) ) // revision
			{
				revision = revision.substring( 1 );
				int targetRevision = Integer.parseInt( revision );
				int currentRevision = StaticEntity.getRevision();
				if ( currentRevision < targetRevision )
				{
					throw this.sinceException( String.valueOf( currentRevision ), revision, true );
				}
			}
			else // version (or syntax error)
			{
				String [] target = revision.split( "\\." );
				if ( target.length != 2 )
				{
					throw this.parseException( "invalid 'since' format" );
				}

				int targetMajor = Integer.parseInt( target[ 0 ] );
				int targetMinor = Integer.parseInt( target[ 1 ] );

				// strip "KoLMafia v" from the front
				String currentVersion = StaticEntity.getVersion();
				currentVersion = currentVersion.substring( currentVersion.indexOf( "v" ) + 1 );

				// Strip " rxxxx" from end
				int rindex = currentVersion.indexOf( " r" );
				if ( rindex != -1 )
				{
					currentVersion = currentVersion.substring( 0, rindex );
				}

				String [] current = currentVersion.split( "\\." );
				int currentMajor = Integer.parseInt( current[ 0 ] );
				int currentMinor = Integer.parseInt( current[ 1 ] );

				if ( targetMajor > currentMajor || ( targetMajor == currentMajor && targetMinor > currentMinor ) )
				{
					throw this.sinceException( currentVersion, revision, false );
				}
			}
		}
		catch ( NumberFormatException e )
		{
			throw this.parseException( "invalid 'since' format" );
		}
	}

	public final void warning( final String msg )
	{
		RequestLogger.printLine( "WARNING: " + msg + " " + this.getLineAndFile() );
	}

	private static void appendFunctionCall( final StringBuilder buffer, final String name, final List<Value> params )
	{
		buffer.append( name );
		buffer.append( "(" );

		String sep = " ";
		for ( Value current : params )
		{
			buffer.append( sep );
			sep = ", ";
			buffer.append( current.getType() );
		}

		buffer.append( " )" );
	}

	private String getLineAndFile()
	{
		return Parser.getLineAndFile( this.shortFileName, this.getLineNumber() );
	}

	public static String getLineAndFile( final String fileName, final int lineNumber )
	{
		if ( fileName == null )
		{
			return "(" + Preferences.getString( "commandLineNamespace" ) + ")";
		}

		return "(" + fileName + ", line " + lineNumber + ")";
	}

	public static void printIndices( final List<Value> indices, final PrintStream stream, final int indent )
	{
		if ( indices == null )
		{
			return;
		}

		for ( Value current : indices )
		{
			AshRuntime.indentLine( stream, indent );
			stream.println( "<KEY>" );
			current.print( stream, indent + 1 );
		}
	}
}
