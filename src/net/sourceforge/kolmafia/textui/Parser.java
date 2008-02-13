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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.KoLConstants.ByteArrayStream;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.Assignment;
import net.sourceforge.kolmafia.textui.parsetree.BasicScript;
import net.sourceforge.kolmafia.textui.parsetree.CompositeReference;
import net.sourceforge.kolmafia.textui.parsetree.Conditional;
import net.sourceforge.kolmafia.textui.parsetree.Else;
import net.sourceforge.kolmafia.textui.parsetree.ElseIf;
import net.sourceforge.kolmafia.textui.parsetree.Expression;
import net.sourceforge.kolmafia.textui.parsetree.ForEachLoop;
import net.sourceforge.kolmafia.textui.parsetree.ForLoop;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionCall;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.If;
import net.sourceforge.kolmafia.textui.parsetree.LoopBreak;
import net.sourceforge.kolmafia.textui.parsetree.LoopContinue;
import net.sourceforge.kolmafia.textui.parsetree.Operator;
import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RepeatUntilLoop;
import net.sourceforge.kolmafia.textui.parsetree.FunctionReturn;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.ScriptExit;
import net.sourceforge.kolmafia.textui.parsetree.Symbol;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.TypeDef;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.ValueList;
import net.sourceforge.kolmafia.textui.parsetree.Variable;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.textui.parsetree.VariableReferenceList;
import net.sourceforge.kolmafia.textui.parsetree.WhileLoop;

public class Parser
{
	// Variables used during parsing

	private String fileName;
	private InputStream istream;

	private LineNumberReader commandStream;
	private String currentLine;
	private String nextLine;
	private int lineNumber;

	private String fullLine;

	private TreeMap imports;
	private Function mainMethod = null;
	private String notifyRecipient = null;

	public Parser()
	{
		this( null, null, null );
	}

	public Parser( final File scriptFile, final InputStream stream, final TreeMap imports )
	{
		this.imports = ( imports != null ) ? imports : new TreeMap();

		if ( scriptFile != null )
		{
			this.fileName = scriptFile.getPath();
			this.istream = DataUtilities.getInputStream( scriptFile );
		}
		else if ( stream != null )
		{
			this.fileName = null;
			this.istream = stream;
		}
		else
		{
			this.fileName = null;
			this.istream = null;
			return;
		}

		try
		{
			this.commandStream = new LineNumberReader( new InputStreamReader( istream ) );
			this.currentLine = this.getNextLine();
			this.lineNumber = this.commandStream.getLineNumber();
			this.nextLine = this.getNextLine();
		}
		catch ( Exception e )
		{
			// If any part of the initialization fails,
			// then throw an exception.

			parseError( this.fileName + " could not be accessed" );
		}
	}

	private void disconnect()
	{
		try
		{
			this.commandStream = null;
			this.istream.close();
		}
		catch ( IOException e )
		{
		}
	}

	public Scope parse()
	{
		Scope scope = null;

		try
		{
			scope = this.parseScope( null, null, new VariableList(), Parser.getExistingFunctionScope(), false );

			if ( this.currentLine != null )
			{
				parseError( "Script parsing error" );
			}
		}
		finally
		{
			this.disconnect();
		}

		return scope;
	}

	public String getFileName()
	{
		return this.fileName;
	}

	public TreeMap getImports()
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

	public static final char[] tokenList =
		{ ' ', '.', ',', '{', '}', '(', ')', '$', '!', '+', '-', '=', '"', '\'', '*', '^', '/', '%', '[', ']', '!', ';', '<', '>' };
	public static final String[] multiCharTokenList = { "==", "!=", "<=", ">=", "||", "&&", "/*", "*/" };

	// Feature control;
	// disabled until and if we choose to document the feature

	private static final boolean arrays = false;

	private static final ArrayList reservedWords = new ArrayList();

	static
	{
		// Constants
		reservedWords.add( "true" );
		reservedWords.add( "false" );

		// Operators
		reservedWords.add( "contains" );
		reservedWords.add( "remove" );

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

		// Data types
		reservedWords.add( "void" );
		reservedWords.add( "boolean" );
		reservedWords.add( "int" );
		reservedWords.add( "float" );
		reservedWords.add( "string" );
		reservedWords.add( "buffer" );
		reservedWords.add( "matcher" );

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

		reservedWords.add( "record" );
		reservedWords.add( "typedef" );
	}

	private static final boolean isReservedWord( final String name )
	{
		return Parser.reservedWords.contains( name.toLowerCase() );
	}

	public Scope importFile( final String fileName, final Scope scope )
	{
		File scriptFile = KoLmafiaCLI.findScriptFile( fileName );
		if ( scriptFile == null )
		{
			parseError( fileName + " could not be found" );
		}

		if ( this.imports.containsKey( scriptFile ) )
		{
			return scope;
		}

		Scope result = scope;
		Parser parser = null;

		try
		{
			parser = new Parser( scriptFile, null, this.imports );
			result = parser.parseScope( scope, null, new VariableList(), scope.getParentScope(), false );
			if ( parser.currentLine != null )
			{
				parseError( "Script parsing error" );
			}
		}
		finally
		{
			if ( parser != null )
			{
				parser.disconnect();
			}
		}

		this.imports.put( scriptFile, new Long( scriptFile.lastModified() ) );
		this.mainMethod = null;

		return result;
	}

	private Scope parseScope( final Scope startScope, final Type expectedType,
		final VariableList variables, final Scope parentScope, final boolean whileLoop )
	{
		Scope result;
		String importString;

		result = startScope == null ? new Scope( variables, parentScope ) : startScope;
		this.parseNotify();

		while ( ( importString = this.parseImport() ) != null )
		{
			result = this.importFile( importString, result );
		}

		while ( true )
		{
			if ( this.parseTypedef( result ) )
			{
				if ( !this.currentToken().equals( ";" ) )
				{
					this.parseError( ";", this.currentToken() );
				}

				this.readToken(); //read ;
				continue;
			}

			Type t = this.parseType( result, true, true );

			// If there is no data type, it's a command of some sort
			if ( t == null )
			{
				// See if it's a regular command
				ParseTreeNode c = this.parseCommand( expectedType, result, false, whileLoop );
				if ( c != null )
				{
					result.addCommand( c );

					continue;
				}

				// No type and no command -> done.
				break;
			}

			// If this is a new record definition, enter it
			if ( t.getType() == DataTypes.TYPE_RECORD && this.currentToken() != null && this.currentToken().equals(
				";" ) )
			{
				this.readToken(); // read ;
				continue;
			}

			Function f = this.parseFunction( t, result );
			if ( f != null )
			{
				if ( f.getName().equalsIgnoreCase( "main" ) )
				{
					this.mainMethod = f;
				}

				continue;
			}

			if ( this.parseVariables( t, result ) )
			{
				if ( !this.currentToken().equals( ";" ) )
				{
					this.parseError( ";", this.currentToken() );
				}

				this.readToken(); //read ;
				continue;
			}

			//Found a type but no function or variable to tie it to
			parseError( "Script parse error" );
		}

		return result;
	}

	private Type parseRecord( final Scope parentScope )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "record" ) )
		{
			return null;
		}

		this.readToken(); // read record

		if ( this.currentToken() == null )
		{
			parseError( "Record name expected" );
		}

		// Allow anonymous records
		String recordName = null;

		if ( !this.currentToken().equals( "{" ) )
		{
			// Named record
			recordName = this.currentToken();

			if ( !this.parseIdentifier( recordName ) )
			{
				parseError( "Invalid record name '" + recordName + "'" );
			}

			if ( Parser.isReservedWord( recordName ) )
			{
				parseError( "Reserved word '" + recordName + "' cannot be a record name" );
			}

			if ( parentScope.findType( recordName ) != null )
			{
				parseError( "Record name '" + recordName + "' is already defined" );
			}

			this.readToken(); // read name
		}

		if ( this.currentToken() == null || !this.currentToken().equals( "{" ) )
		{
			this.parseError( "{", this.currentToken() );
		}

		this.readToken(); // read {

		// Loop collecting fields
		ArrayList fieldTypes = new ArrayList();
		ArrayList fieldNames = new ArrayList();

		while ( true )
		{
			// Get the field type
			Type fieldType = this.parseType( parentScope, true, true );
			if ( fieldType == null )
			{
				parseError( "Type name expected" );
			}

			// Get the field name
			String fieldName = this.currentToken();
			if ( fieldName == null )
			{
				parseError( "Field name expected" );
			}

			if ( !this.parseIdentifier( fieldName ) )
			{
				parseError( "Invalid field name '" + fieldName + "'" );
			}

			if ( Parser.isReservedWord( fieldName ) )
			{
				parseError( "Reserved word '" + fieldName + "' cannot be used as a field name" );
			}

			if ( fieldNames.contains( fieldName ) )
			{
				parseError( "Field name '" + fieldName + "' is already defined" );
			}

			this.readToken(); // read name

			if ( this.currentToken() == null || !this.currentToken().equals( ";" ) )
			{
				this.parseError( ";", this.currentToken() );
			}

			this.readToken(); // read ;

			fieldTypes.add( fieldType );
			fieldNames.add( fieldName.toLowerCase() );

			if ( this.currentToken() == null )
			{
				this.parseError( "}", "EOF" );
			}

			if ( this.currentToken().equals( "}" ) )
			{
				break;
			}
		}

		this.readToken(); // read }

		String[] fieldNameArray = new String[ fieldNames.size() ];
		Type[] fieldTypeArray = new Type[ fieldTypes.size() ];
		fieldNames.toArray( fieldNameArray );
		fieldTypes.toArray( fieldTypeArray );

		RecordType rec =
			new RecordType(
				recordName == null ? "(anonymous record)" : recordName, fieldNameArray, fieldTypeArray );

		if ( recordName != null )
		{
			// Enter into type table
			parentScope.addType( rec );
		}

		return rec;
	}

	private Function parseFunction( final Type functionType, final Scope parentScope )
	{
		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			return null;
		}

		String functionName = this.currentToken();

		if ( Parser.isReservedWord( functionName ) )
		{
			parseError( "Reserved word '" + functionName + "' cannot be used as a function name" );
		}

		this.readToken(); //read Function name
		this.readToken(); //read (

		VariableList paramList = new VariableList();
		VariableReferenceList variableReferences = new VariableReferenceList();

		while ( !this.currentToken().equals( ")" ) )
		{
			Type paramType = this.parseType( parentScope, true, false );
			if ( paramType == null )
			{
				this.parseError( ")", this.currentToken() );
			}

			Variable param = this.parseVariable( paramType, null );
			if ( param == null )
			{
				this.parseError( "identifier", this.currentToken() );
			}

			if ( !paramList.add( param ) )
			{
				parseError( "Variable " + param.getName() + " is already defined" );
			}

			if ( !this.currentToken().equals( ")" ) )
			{
				if ( !this.currentToken().equals( "," ) )
				{
					this.parseError( ")", this.currentToken() );
				}

				this.readToken(); //read comma
			}

			variableReferences.add( new VariableReference( param ) );
		}

		this.readToken(); //read )

		// Add the function to the parent scope before we parse the
		// function scope to allow recursion.

		UserDefinedFunction f = new UserDefinedFunction( functionName, functionType, variableReferences );
		UserDefinedFunction existing = parentScope.findFunction( f );

		if ( existing != null && existing.getScope() != null )
		{
			parseError( "Function '" + functionName + "' defined multiple times" );
		}

		// Add new function or replace existing forward reference

		UserDefinedFunction result = parentScope.replaceFunction( existing, f );

		if ( this.currentToken() != null && this.currentToken().equals( ";" ) )
		{
			// Return forward reference
			this.readToken(); // ;
			return result;
		}

		Scope scope;
		if ( this.currentToken() != null && this.currentToken().equals( "{" ) )
		{
			// Scope is a block

			this.readToken(); // {

			scope = this.parseScope( null, functionType, paramList, parentScope, false );
			if ( this.currentToken() == null || !this.currentToken().equals( "}" ) )
			{
				this.parseError( "}", this.currentToken() );
			}

			this.readToken(); // }
		}
		else
		{
			// Scope is a single command
			scope = new Scope( paramList, parentScope );
			scope.addCommand( this.parseCommand( functionType, parentScope, false, false ) );
		}

		result.setScope( scope );
		if ( !result.assertReturn() && !functionType.equals( DataTypes.TYPE_VOID )
		// The following clause can't be correct. I think it
		// depends on the various conditional & loop constructs
		// returning a boolean. Or something. But without it,
		// existing scripts break. Aargh!
		&& !functionType.equals( DataTypes.TYPE_BOOLEAN ) )
		{
			parseError( "Missing return value" );
		}

		return result;
	}

	private boolean parseVariables( final Type t, final Scope parentScope )
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

	private Variable parseVariable( final Type t, final Scope scope )
	{
		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		String variableName = this.currentToken();
		if ( Parser.isReservedWord( variableName ) )
		{
			parseError( "Reserved word '" + variableName + "' cannot be a variable name" );
		}

		Variable result = new Variable( variableName, t );
		if ( scope != null && !scope.addVariable( result ) )
		{
			parseError( "Variable " + result.getName() + " is already defined" );
		}

		this.readToken(); // If parsing of Identifier succeeded, go to next token.
		// If we are parsing a parameter declaration, we are done
		if ( scope == null )
		{
			if ( this.currentToken().equals( "=" ) )
			{
				parseError( "Cannot initialize parameter " + result.getName() );
			}
			return result;
		}

		// Otherwise, we must initialize the variable.

		VariableReference lhs = new VariableReference( result.getName(), scope );
		Value rhs;

		if ( this.currentToken().equals( "=" ) )
		{
			this.readToken(); // Eat the equals sign
			rhs = this.parseExpression( scope );

			if ( rhs == null )
			{
				parseError( "Expression expected" );
			}

			if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
			{
				parseError(
					"Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() );
			}
		}
		else
		{
			rhs = null;
		}

		scope.addCommand( new Assignment( lhs, rhs ) );
		return result;
	}

	private boolean parseTypedef( final Scope parentScope )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "typedef" ) )
		{
			return false;
		}
		this.readToken(); // read typedef

		Type t = this.parseType( parentScope, true, true );
		if ( t == null )
		{
			parseError( "Missing data type for typedef" );
		}

		String typeName = this.currentToken();

		if ( !this.parseIdentifier( typeName ) )
		{
			parseError( "Invalid type name '" + typeName + "'" );
		}

		if ( Parser.isReservedWord( typeName ) )
		{
			parseError( "Reserved word '" + typeName + "' cannot be a type name" );
		}

		if ( parentScope.findType( typeName ) != null )
		{
			parseError( "Type name '" + typeName + "' is already defined" );
		}

		this.readToken(); // read name

		// Add the type to the type table
		TypeDef type = new TypeDef( typeName, t );
		parentScope.addType( type );

		return true;
	}

	private ParseTreeNode parseCommand( final Type functionType, final Scope scope, final boolean noElse,
		boolean whileLoop )
	{
		ParseTreeNode result;

		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( this.currentToken().equalsIgnoreCase( "break" ) )
		{
			if ( !whileLoop )
			{
				parseError( "Encountered 'break' outside of loop" );
			}

			result = new LoopBreak();
			this.readToken(); //break
		}

		else if ( this.currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( !whileLoop )
			{
				parseError( "Encountered 'continue' outside of loop" );
			}

			result = new LoopContinue();
			this.readToken(); //continue
		}

		else if ( this.currentToken().equalsIgnoreCase( "exit" ) )
		{
			result = new ScriptExit();
			this.readToken(); //exit
		}

		else if ( ( result = this.parseReturn( functionType, scope ) ) != null )
		{
			;
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
		else if ( ( result = this.parseFor( functionType, scope ) ) != null )
		{
			// for doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseRepeat( functionType, scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseConditional( functionType, scope, noElse, whileLoop ) ) != null )
		{
			// loop doesn't have a ; token
			return result;
		}
		else if ( ( result = this.parseCall( scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseAssignment( scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseRemove( scope ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseValue( scope ) ) != null )
		{
			;
		}
		else
		{
			return null;
		}

		if ( this.currentToken() == null || !this.currentToken().equals( ";" ) )
		{
			this.parseError( ";", this.currentToken() );
		}

		this.readToken(); // ;
		return result;
	}

	private Type parseType( final Scope scope, final boolean aggregates, final boolean records )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		Type valType = scope.findType( this.currentToken() );
		if ( valType == null )
		{
			if ( records && this.currentToken().equalsIgnoreCase( "record" ) )
			{
				valType = this.parseRecord( scope );

				if ( valType == null )
				{
					return null;
				}

				if ( aggregates && this.currentToken().equals( "[" ) )
				{
					return this.parseAggregateType( valType, scope );
				}

				return valType;
			}

			return null;
		}

		this.readToken();

		if ( aggregates && this.currentToken().equals( "[" ) )
		{
			return this.parseAggregateType( valType, scope );
		}

		return valType;
	}

	private Type parseAggregateType( final Type dataType, final Scope scope )
	{
		this.readToken(); // [ or ,
		if ( this.currentToken() == null )
		{
			parseError( "Missing index token" );
		}

		if ( Parser.arrays && this.readIntegerToken( this.currentToken() ) )
		{
			int size = StaticEntity.parseInt( this.currentToken() );
			this.readToken(); // integer

			if ( this.currentToken() == null )
			{
				this.parseError( "]", this.currentToken() );
			}

			if ( this.currentToken().equals( "]" ) )
			{
				this.readToken(); // ]

				if ( this.currentToken().equals( "[" ) )
				{
					return new AggregateType( this.parseAggregateType( dataType, scope ), size );
				}

				return new AggregateType( dataType, size );
			}

			if ( this.currentToken().equals( "," ) )
			{
				return new AggregateType( this.parseAggregateType( dataType, scope ), size );
			}

			this.parseError( "]", this.currentToken() );
		}

		Type indexType = scope.findType( this.currentToken() );
		if ( indexType == null )
		{
			parseError( "Invalid type name '" + this.currentToken() + "'" );
		}

		if ( !indexType.isPrimitive() )
		{
			parseError( "Index type '" + this.currentToken() + "' is not a primitive type" );
		}

		this.readToken(); // type name
		if ( this.currentToken() == null )
		{
			this.parseError( "]", this.currentToken() );
		}

		if ( this.currentToken().equals( "]" ) )
		{
			this.readToken(); // ]

			if ( this.currentToken().equals( "[" ) )
			{
				return new AggregateType( this.parseAggregateType( dataType, scope ), indexType );
			}

			return new AggregateType( dataType, indexType );
		}

		if ( this.currentToken().equals( "," ) )
		{
			return new AggregateType( this.parseAggregateType( dataType, scope ), indexType );
		}

		this.parseError( ", or ]", this.currentToken() );
		return null;
	}

	private boolean parseIdentifier( final String identifier )
	{
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

	private FunctionReturn parseReturn( final Type expectedType, final Scope parentScope )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "return" ) )
		{
			return null;
		}

		this.readToken(); //return

		if ( this.currentToken() != null && this.currentToken().equals( ";" ) )
		{
			if ( expectedType != null && expectedType.equals( DataTypes.TYPE_VOID ) )
			{
				return new FunctionReturn( null, DataTypes.VOID_TYPE );
			}

			parseError( "Return needs " + expectedType + " value" );
			return null;
		}
		else
		{
			if ( expectedType != null && expectedType.equals( DataTypes.TYPE_VOID ) )
			{
				parseError( "Cannot return a value from a void function" );
			}

			Value value = this.parseExpression( parentScope );

			if ( value == null )
			{
				parseError( "Expression expected" );
			}

			if ( !Parser.validCoercion( expectedType, value.getType(), "return" ) )
			{
				parseError( "Cannot return " + value.getType() + " value from " + expectedType + " function");
			}

			return new FunctionReturn( value, expectedType );
		}
	}

	private Conditional parseConditional( final Type functionType, final Scope parentScope,
		boolean noElse, final boolean loop )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "if" ) )
		{
			return null;
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			this.parseError( "(", this.nextToken() );
		}

		this.readToken(); // if
		this.readToken(); // (

		Value condition = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			parseError( "\"if\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		If result = null;
		boolean elseFound = false;
		boolean finalElse = false;

		do
		{
			Scope scope;

			if ( this.currentToken() == null || !this.currentToken().equals( "{" ) ) //Scope is a single call
			{
				ParseTreeNode command = this.parseCommand( functionType, parentScope, !elseFound, loop );
				scope = new Scope( command, parentScope );
			}
			else
			{
				this.readToken(); //read {
				scope = this.parseScope( null, functionType, null, parentScope, loop );

				if ( this.currentToken() == null || !this.currentToken().equals( "}" ) )
				{
					this.parseError( "}", this.currentToken() );
				}

				this.readToken(); //read }
			}

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

			if ( !noElse && this.currentToken() != null && this.currentToken().equalsIgnoreCase( "else" ) )
			{
				if ( finalElse )
				{
					parseError( "Else without if" );
				}

				if ( this.nextToken() != null && this.nextToken().equalsIgnoreCase( "if" ) )
				{
					this.readToken(); //else
					this.readToken(); //if

					if ( this.currentToken() == null || !this.currentToken().equals( "(" ) )
					{
						this.parseError( "(", this.currentToken() );
					}

					this.readToken(); //(
					condition = this.parseExpression( parentScope );

					if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
					{
						this.parseError( ")", this.currentToken() );
					}

					if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
					{
						parseError( "\"if\" requires a boolean conditional expression" );
					}

					this.readToken(); // )
				}
				else
				//else without condition
				{
					this.readToken(); //else
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
		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "cli_execute" ) )
		{
			return null;
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "{" ) )
		{
			return null;
		}

		this.readToken(); // while
		this.readToken(); // {

		ByteArrayStream ostream = new ByteArrayStream();

		while ( this.currentToken() != null && !this.currentToken().equals( "}" ) )
		{
			try
			{
				ostream.write( this.currentLine.getBytes() );
				ostream.write( KoLConstants.LINE_BREAK.getBytes() );
			}
			catch ( Exception e )
			{
				// Byte array output streams do not throw errors,
				// other than out of memory errors.

				StaticEntity.printStackTrace( e );
			}

			this.currentLine = "";
			this.fixLines();
		}

		if ( this.currentToken() == null )
		{
			this.parseError( "}", this.currentToken() );
		}

		this.readToken(); // }

		return new BasicScript( ostream );
	}

	private WhileLoop parseWhile( final Type functionType, final Scope parentScope )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "while" ) )
		{
			return null;
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			this.parseError( "(", this.nextToken() );
		}

		this.readToken(); // while
		this.readToken(); // (

		Value condition = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			parseError( "\"while\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		Scope scope = this.parseLoopScope( functionType, null, parentScope );

		return new WhileLoop( scope, condition );
	}

	private RepeatUntilLoop parseRepeat( final Type functionType, final Scope parentScope )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "repeat" ) )
		{
			return null;
		}

		this.readToken(); // repeat

		Scope scope = this.parseLoopScope( functionType, null, parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( "until" ) )
		{
			this.parseError( "until", this.currentToken() );
		}

		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			this.parseError( "(", this.nextToken() );
		}

		this.readToken(); // until
		this.readToken(); // (

		Value condition = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( condition == null || condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			parseError( "\"repeat\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		return new RepeatUntilLoop( scope, condition );
	}

	private ForEachLoop parseForeach( final Type functionType, final Scope parentScope )
	{
		// foreach key [, key ... ] in aggregate { scope }

		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "foreach" ) )
		{
			return null;
		}

		this.readToken(); // foreach

		ArrayList names = new ArrayList();

		while ( true )
		{
			String name = this.currentToken();

			if ( !this.parseIdentifier( name ) )
			{
				parseError( "Key variable name expected" );
			}

			if ( parentScope.findVariable( name ) != null )
			{
				parseError( "Key variable '" + name + "' is already defined" );
			}

			names.add( name );
			this.readToken(); // name

			if ( this.currentToken() != null )
			{
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
			}

			this.parseError( "in", this.currentToken() );
		}

		// Get an aggregate reference
		Value aggregate = this.parseVariableReference( parentScope );

		if ( aggregate == null || !( aggregate instanceof VariableReference ) || !( aggregate.getType().getBaseType() instanceof AggregateType ) )
		{
			parseError( "Aggregate reference expected" );
		}

		// Define key variables of appropriate type
		VariableList varList = new VariableList();
		VariableReferenceList variableReferences = new VariableReferenceList();
		Type type = aggregate.getType().getBaseType();

		for ( int i = 0; i < names.size(); ++i )
		{
			if ( !( type instanceof AggregateType ) )
			{
				parseError( "Too many key variables specified" );
			}

			Type itype = ( (AggregateType) type ).getIndexType();
			Variable keyvar = new Variable( (String) names.get( i ), itype );
			varList.add( keyvar );
			variableReferences.add( new VariableReference( keyvar ) );
			type = ( (AggregateType) type ).getDataType();
		}

		// Parse the scope with the list of keyVars
		Scope scope = this.parseLoopScope( functionType, varList, parentScope );

		// Add the foreach node with the list of varRefs
		return new ForEachLoop( scope, variableReferences, (VariableReference) aggregate );
	}

	private ForLoop parseFor( final Type functionType, final Scope parentScope )
	{
		// foreach key in aggregate {scope }

		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( !this.currentToken().equalsIgnoreCase( "for" ) )
		{
			return null;
		}

		String name = this.nextToken();

		if ( !this.parseIdentifier( name ) )
		{
			return null;
		}

		if ( parentScope.findVariable( name ) != null )
		{
			parseError( "Index variable '" + name + "' is already defined" );
		}

		this.readToken(); // for
		this.readToken(); // name

		if ( !this.currentToken().equalsIgnoreCase( "from" ) )
		{
			this.parseError( "from", this.currentToken() );
		}

		this.readToken(); // from

		Value initial = this.parseExpression( parentScope );

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
			this.parseError( "to, upto, or downto", this.currentToken() );
		}

		this.readToken(); // upto/downto

		Value last = this.parseExpression( parentScope );

		Value increment = DataTypes.ONE_VALUE;
		if ( this.currentToken().equalsIgnoreCase( "by" ) )
		{
			this.readToken(); // by
			increment = this.parseExpression( parentScope );
		}

		// Create integer index variable
		Variable indexvar = new Variable( name, DataTypes.INT_TYPE );

		// Put index variable onto a list
		VariableList varList = new VariableList();
		varList.add( indexvar );

		Scope scope = this.parseLoopScope( functionType, varList, parentScope );

		return new ForLoop( scope, new VariableReference( indexvar ), initial, last, increment, direction );
	}

	private Scope parseLoopScope( final Type functionType, final VariableList varList,
		final Scope parentScope )
	{
		Scope scope;

		if ( this.currentToken() != null && this.currentToken().equals( "{" ) )
		{
			// Scope is a block

			this.readToken(); // {

			scope = this.parseScope( null, functionType, varList, parentScope, true );
			if ( this.currentToken() == null || !this.currentToken().equals( "}" ) )
			{
				this.parseError( "}", this.currentToken() );
			}

			this.readToken(); // }
		}
		else
		{
			// Scope is a single command
			scope = new Scope( varList, parentScope );
			scope.addCommand( this.parseCommand( functionType, scope, false, true ) );
		}

		return scope;
	}

	private Value parseCall( final Scope scope )
	{
		return this.parseCall( scope, null );
	}

	private Value parseCall( final Scope scope, final Value firstParam )
	{
		if ( this.nextToken() == null || !this.nextToken().equals( "(" ) )
		{
			return null;
		}

		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		String name = this.currentToken();

		this.readToken(); //name
		this.readToken(); //(

		ValueList params = new ValueList();
		if ( firstParam != null )
		{
			params.add( firstParam );
		}

		while ( this.currentToken() != null && !this.currentToken().equals( ")" ) )
		{
			Value val = this.parseExpression( scope );
			if ( val != null )
			{
				params.add( val );
			}

			if ( !this.currentToken().equals( "," ) )
			{
				if ( !this.currentToken().equals( ")" ) )
				{
					this.parseError( ")", this.currentToken() );
				}
			}
			else
			{
				this.readToken();
				if ( this.currentToken().equals( ")" ) )
				{
					this.parseError( "parameter", this.currentToken() );
				}
			}
		}

		if ( !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		this.readToken(); // )

		Function target = this.findFunction( scope, name, params );
		if ( target == null )
		{
			parseError( "Undefined reference to function '" + name + "'" );
		}
		FunctionCall call = new FunctionCall( target, params );
		Value result = call;
		while ( result != null && this.currentToken() != null && this.currentToken().equals( "." ) )
		{
			Variable current = new Variable( result.getType() );
			current.setExpression( result );

			result = this.parseVariableReference( scope, current );
		}

		return result;
	}

        private final Function findFunction( final Scope scope, final String name, final ValueList params )
	{
                Function result = this.findFunction( scope, scope.getFunctionList(), name, params, true );

                if ( result == null )
                {
                        result = this.findFunction( scope, RuntimeLibrary.functions, name, params, true );
                }
                if ( result == null )
                {
                        result = this.findFunction( scope, scope.getFunctionList(), name, params, false );
                }
                if ( result == null )
                {
                        result = this.findFunction( scope, RuntimeLibrary.functions, name, params, false );
                }

                // Just in case there's some people who don't want to edit
                // their scripts to use the new function format, check for
                // the old versions as well.

                if ( result == null )
                {
                        if ( name.endsWith( "to_string" ) )
                        {
                                return this.findFunction( scope, "to_string", params );
                        }
                        if ( name.endsWith( "to_boolean" ) )
                        {
                                return this.findFunction( scope, "to_boolean", params );
                        }
                        if ( name.endsWith( "to_int" ) )
                        {
                                return this.findFunction( scope, "to_int", params );
                        }
                        if ( name.endsWith( "to_float" ) )
                        {
                                return this.findFunction( scope, "to_float", params );
                        }
                        if ( name.endsWith( "to_item" ) )
                        {
                                return this.findFunction( scope, "to_item", params );
                        }
                        if ( name.endsWith( "to_class" ) )
                        {
                                return this.findFunction( scope, "to_class", params );
                        }
                        if ( name.endsWith( "to_stat" ) )
                        {
                                return this.findFunction( scope, "to_stat", params );
                        }
                        if ( name.endsWith( "to_skill" ) )
                        {
                                return this.findFunction( scope, "to_skill", params );
                        }
                        if ( name.endsWith( "to_effect" ) )
                        {
                                return this.findFunction( scope, "to_effect", params );
                        }
                        if ( name.endsWith( "to_location" ) )
                        {
                                return this.findFunction( scope, "to_location", params );
                        }
                        if ( name.endsWith( "to_familiar" ) )
                        {
                                return this.findFunction( scope, "to_familiar", params );
                        }
                        if ( name.endsWith( "to_monster" ) )
                        {
                                return this.findFunction( scope, "to_monster", params );
                        }
                        if ( name.endsWith( "to_slot" ) )
                        {
                                return this.findFunction( scope, "to_slot", params );
                        }
                        if ( name.endsWith( "to_url" ) )
                        {
                                return this.findFunction( scope, "to_url", params );
                        }
                }

		return result;
        }

	private final Function findFunction( final Scope scope, final FunctionList source,
						   final String name, final ValueList params,
						   boolean isExactMatch )
	{
		String errorMessage = null;

		Function[] functions = source.findFunctions( name );

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
			Iterator valIterator = params.iterator();

			VariableReference currentParam;
			Value currentValue;
			int paramIndex = 1;

			while ( errorMessage == null && refIterator.hasNext() && valIterator.hasNext() )
			{
				currentParam = (VariableReference) refIterator.next();
				currentValue = (Value) valIterator.next();

				if ( isExactMatch )
				{
					if ( currentParam.getType() != currentValue.getType() )
					{
						errorMessage =
							"Illegal parameter #" + paramIndex + " for function " + name + ". Got " + currentValue.getType() + ", need " + currentParam.getType();
					}
				}
				else if ( !Parser.validCoercion( currentParam.getType(), currentValue.getType(), "parameter" ) )
				{
					errorMessage =
						"Illegal parameter #" + paramIndex + " for function " + name + ". Got " + currentValue.getType() + ", need " + currentParam.getType();
				}

				++paramIndex;
			}

			if ( errorMessage == null && ( refIterator.hasNext() || valIterator.hasNext() ) )
			{
				errorMessage =
					"Illegal amount of parameters for function " + name + ". Got " + params.size() + ", expected " + functions[ i ].getVariableReferences().size();
			}

			if ( errorMessage == null )
			{
				return functions[ i ];
			}
		}

		if ( !isExactMatch && scope.getParentScope() != null )
		{
			return findFunction( scope.getParentScope(), name, params );
		}

		if ( !isExactMatch && source == RuntimeLibrary.functions && errorMessage != null )
		{
			parseError( errorMessage );
		}

		return null;
	}

	private ParseTreeNode parseAssignment( final Scope scope )
	{
		if ( this.nextToken() == null )
		{
			return null;
		}

		if ( !this.nextToken().equals( "=" ) && !this.nextToken().equals( "[" ) && !this.nextToken().equals( "." ) )
		{
			return null;
		}

		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		Value lhs = this.parseVariableReference( scope );
		if ( lhs instanceof FunctionCall )
		{
			return lhs;
		}

		if ( lhs == null || !( lhs instanceof VariableReference ) )
		{
			parseError( "Variable reference expected" );
		}

		if ( !this.currentToken().equals( "=" ) )
		{
			return null;
		}

		this.readToken(); //=

		Value rhs = this.parseExpression( scope );

		if ( rhs == null )
		{
			parseError( "Internal error" );
		}

		if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
		{
			parseError(
				"Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() );
		}

		return new Assignment( (VariableReference) lhs, rhs );
	}

	private Value parseRemove( final Scope scope )
	{
		if ( this.currentToken() == null || !this.currentToken().equals( "remove" ) )
		{
			return null;
		}

		Value lhs = this.parseExpression( scope );

		if ( lhs == null )
		{
			parseError( "Bad 'remove' statement" );
		}

		return lhs;
	}

	private Value parseExpression( final Scope scope )
	{
		return this.parseExpression( scope, null );
	}

	private Value parseExpression( final Scope scope, final Operator previousOper )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		Value lhs = null;
		Value rhs = null;
		Operator oper = null;

		if ( this.currentToken().equals( "!" ) )
		{
			String operator = this.currentToken();
			this.readToken(); // !
			if ( ( lhs = this.parseValue( scope ) ) == null )
			{
				parseError( "Value expected" );
			}

			lhs = new Expression( lhs, null, new Operator( operator ) );
			if ( lhs.getType() != DataTypes.BOOLEAN_TYPE )
			{
				parseError( "\"!\" operator requires a boolean value" );
			}
		}
		else if ( this.currentToken().equals( "-" ) )
		{
			// See if it's a negative numeric constant
			if ( ( lhs = this.parseValue( scope ) ) != null )
			{
				return lhs;
			}

			// Nope. Must be unary minus.
			String operator = this.currentToken();
			this.readToken(); // !
			if ( ( lhs = this.parseValue( scope ) ) == null )
			{
				parseError( "Value expected" );
			}

			lhs = new Expression( lhs, null, new Operator( operator ) );
		}
		else if ( this.currentToken().equals( "remove" ) )
		{
			String operator = this.currentToken();
			this.readToken(); // remove

			lhs = this.parseVariableReference( scope );
			if ( lhs == null || !( lhs instanceof CompositeReference ) )
			{
				parseError( "Aggregate reference expected" );
			}

			lhs = new Expression( lhs, null, new Operator( operator ) );
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

			this.readToken(); //operator

			if ( ( rhs = this.parseExpression( scope, oper ) ) == null )
			{
				parseError( "Value expected" );
			}

			if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), oper.toString() ) )
			{
				parseError(
					"Cannot apply operator " + oper + " to " + lhs + " (" + lhs.getType() + ") and " + rhs + " (" + rhs.getType() + ")" );
			}

			lhs = new Expression( lhs, rhs, oper );
		}
		while ( true );
	}

	private Value parseValue( final Scope scope )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		Value result = null;

		// Parse parenthesized expressions
		if ( this.currentToken().equals( "(" ) )
		{
			this.readToken(); // (

			result = this.parseExpression( scope );
			if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
			{
				this.parseError( ")", this.currentToken() );
			}

			this.readToken(); // )
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

		// numbers
		else if ( ( result = this.parseNumber() ) != null )
		{
			;
		}
		else if ( this.currentToken().equals( "\"" ) || this.currentToken().equals( "\'" ) )
		{
			result = this.parseString();
		}
		else if ( this.currentToken().equals( "$" ) )
		{
			result = this.parseTypedConstant( scope );
		}
		else if ( ( result = this.parseCall( scope, result ) ) != null )
		{
			;
		}
		else if ( ( result = this.parseVariableReference( scope ) ) != null )
		{
			;
		}

		Variable current;
		while ( result != null && this.currentToken() != null && this.currentToken().equals( "." ) )
		{
			current = new Variable( result.getType() );
			current.setExpression( result );

			result = this.parseVariableReference( scope, current );
		}

		return result;
	}

	private Value parseNumber()
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

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
			String fraction = this.currentToken();

			if ( !this.readIntegerToken( fraction ) )
			{
				this.parseError( "numeric value", fraction );
			}

			this.readToken(); // integer
			return new Value( sign * StaticEntity.parseFloat( "0." + fraction ) );
		}

		String integer = this.currentToken();
		if ( !this.readIntegerToken( integer ) )
		{
			return null;
		}

		this.readToken(); // integer

		if ( this.currentToken().equals( "." ) )
		{
			String fraction = this.nextToken();
			if ( !this.readIntegerToken( fraction ) )
			{
				return new Value( sign * StaticEntity.parseInt( integer ) );
			}

			this.readToken(); // .
			this.readToken(); // fraction

			return new Value( sign * StaticEntity.parseFloat( integer + "." + fraction ) );
		}

		return new Value( sign * StaticEntity.parseInt( integer ) );
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

	private Value parseString()
	{
		// Directly work with currentLine - ignore any "tokens" you meet until
		// the string is closed

		StringBuffer resultString = new StringBuffer();
		char startCharacter = this.currentLine.charAt( 0 );

		for ( int i = 1;; ++i )
		{
			if ( i == this.currentLine.length() )
			{
				parseError( "No closing \" found" );
			}

			if ( this.currentLine.charAt( i ) == '\\' )
			{
				char ch = this.currentLine.charAt( ++i );

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

				case '\\':
				case '\'':
				case '\"':
					resultString.append( ch );
					break;

				case 'x':
					BigInteger hex08 = new BigInteger( this.currentLine.substring( i + 1, i + 3 ), 16 );
					resultString.append( (char) hex08.intValue() );
					i += 2;
					break;

				case 'u':
					BigInteger hex16 = new BigInteger( this.currentLine.substring( i + 1, i + 5 ), 16 );
					resultString.append( (char) hex16.intValue() );
					i += 4;
					break;

				default:
					if ( Character.isDigit( ch ) )
					{
						BigInteger octal = new BigInteger( this.currentLine.substring( i, i + 3 ), 8 );
						resultString.append( (char) octal.intValue() );
						i += 2;
					}
				}
			}
			else if ( this.currentLine.charAt( i ) == startCharacter )
			{
				this.currentLine = this.currentLine.substring( i + 1 ); //+ 1 to get rid of '"' token
				return new Value( resultString.toString() );
			}
			else
			{
				resultString.append( this.currentLine.charAt( i ) );
			}
		}
	}

	private Value parseTypedConstant( final Scope scope )
	{
		this.readToken(); // read $

		String name = this.currentToken();
		Type type = this.parseType( scope, false, false );
		if ( type == null || !type.isPrimitive() )
		{
			parseError( "Unknown type " + name );
		}

		if ( !this.currentToken().equals( "[" ) )
		{
			this.parseError( "[", this.currentToken() );
		}

		StringBuffer resultString = new StringBuffer();

		for ( int i = 1;; ++i )
		{
			if ( i == this.currentLine.length() )
			{
				parseError( "No closing ] found" );
			}
			else if ( this.currentLine.charAt( i ) == '\\' )
			{
				resultString.append( this.currentLine.charAt( ++i ) );
			}
			else if ( this.currentLine.charAt( i ) == ']' )
			{
				this.currentLine = this.currentLine.substring( i + 1 ); //+1 to get rid of ']' token
				String input = resultString.toString().trim();
				Value value = DataTypes.parseValue( type, input, false );
				if ( value == null )
				{
					parseError( "Bad " + type.toString() + " value: \"" + input + "\"" );
				}
				return value;
			}
			else
			{
				resultString.append( this.currentLine.charAt( i ) );
			}
		}
	}

	private Operator parseOperator( final String oper )
	{
		if ( oper == null || !this.isOperator( oper ) )
		{
			return null;
		}

		return new Operator( oper );
	}

	private boolean isOperator( final String oper )
	{
		return oper.equals( "!" ) ||
			oper.equals( "*" ) ||
			oper.equals( "^" ) ||
			oper.equals( "/" ) ||
			oper.equals( "%" ) ||
			oper.equals( "+" ) ||
			oper.equals( "-" ) ||
			oper.equals( "<" ) ||
			oper.equals( ">" ) ||
			oper.equals( "<=" ) ||
			oper.equals( ">=" ) ||
			oper.equals( "=" ) ||
			oper.equals( "==" ) ||
			oper.equals( "!=" ) ||
			oper.equals( "||" ) ||
			oper.equals( "&&" ) ||
			oper.equals( "contains" ) ||
			oper.equals( "remove" );
	}

	private Value parseVariableReference( final Scope scope )
	{
		if ( this.currentToken() == null || !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		String name = this.currentToken();
		Variable var = scope.findVariable( name, true );

		if ( var == null )
		{
			parseError( "Unknown variable '" + name + "'" );
		}

		this.readToken(); // read name

		if ( this.currentToken() == null || !this.currentToken().equals( "[" ) && !this.currentToken().equals( "." ) )
		{
			return new VariableReference( var );
		}

		return this.parseVariableReference( scope, var );
	}

	private Value parseVariableReference( final Scope scope, final Variable var )
	{
		Type type = var.getType();
		ValueList indices = new ValueList();

		boolean parseAggregate = this.currentToken().equals( "[" );

		while ( this.currentToken() != null && ( this.currentToken().equals( "[" ) || this.currentToken().equals( "." ) || parseAggregate && this.currentToken().equals(
			"," ) ) )
		{
			Value index;

			type = type.getBaseType();

			if ( this.currentToken().equals( "[" ) || this.currentToken().equals( "," ) )
			{
				this.readToken(); // read [ or . or ,
				parseAggregate = true;

				if ( !( type instanceof AggregateType ) )
				{
					if ( indices.isEmpty() )
					{
						parseError( "Variable '" + var.getName() + "' cannot be indexed" );
					}
					else
					{
						parseError( "Too many keys for '" + var.getName() + "'" );
					}
				}

				AggregateType atype = (AggregateType) type;
				index = this.parseExpression( scope );
				if ( index == null )
				{
					parseError( "Index for '" + var.getName() + "' expected" );
				}

				if ( !index.getType().equals( atype.getIndexType() ) )
				{
					parseError(
						"Index for '" + var.getName() + "' has wrong data type " + "(expected " + atype.getIndexType() + ", got " + index.getType() + ")" );
				}

				type = atype.getDataType();
			}
			else
			{
				this.readToken(); // read [ or . or ,

				// Maybe it's a function call with an implied "this" parameter.

				if ( this.nextToken().equals( "(" ) )
				{
					return this.parseCall(
						scope, indices.isEmpty() ? new VariableReference( var ) : new CompositeReference(
							var, indices ) );
				}

				if ( !( type instanceof RecordType ) )
				{
					parseError( "Record expected" );
				}

				RecordType rtype = (RecordType) type;

				String field = this.currentToken();
				if ( field == null || !this.parseIdentifier( field ) )
				{
					parseError( "Field name expected" );
				}

				index = rtype.getFieldIndex( field );
				if ( index == null )
				{
					parseError( "Invalid field name '" + field + "'" );
				}
				this.readToken(); // read name
				type = rtype.getDataType( index );
			}

			indices.add( index );

			if ( parseAggregate && this.currentToken() != null )
			{
				if ( this.currentToken().equals( "]" ) )
				{
					this.readToken(); // read ]
					parseAggregate = false;
				}
			}
		}

		if ( parseAggregate )
		{
			this.parseError( this.currentToken(), "]" );
		}

		return new CompositeReference( var, indices );
	}

	private String parseDirective( final String directive )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( directive ) )
		{
			return null;
		}

		this.readToken(); //directive

		if ( this.currentToken() == null )
		{
			this.parseError( "<", this.currentToken() );
		}

		int startIndex = this.currentLine.indexOf( "<" );
		int endIndex = this.currentLine.indexOf( ">" );

		if ( startIndex != -1 && endIndex == -1 )
		{
			parseError( "No closing > found" );
		}

		if ( startIndex == -1 )
		{
			startIndex = this.currentLine.indexOf( "\"" );
			endIndex = this.currentLine.indexOf( "\"", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
			{
				parseError( "No closing \" found" );
			}
		}

		if ( startIndex == -1 )
		{
			startIndex = this.currentLine.indexOf( "\'" );
			endIndex = this.currentLine.indexOf( "\'", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
			{
				parseError( "No closing \' found" );
			}
		}

		if ( endIndex == -1 )
		{
			endIndex = this.currentLine.indexOf( ";" );
			if ( endIndex == -1 )
			{
				endIndex = this.currentLine.length();
			}
		}

		String resultString = this.currentLine.substring( startIndex + 1, endIndex );
		this.currentLine = this.currentLine.substring( endIndex );

		if ( this.currentToken().equals( ">" ) || this.currentToken().equals( "\"" ) || this.currentToken().equals(
			"\'" ) )
		{
			this.readToken(); //get rid of '>' or '"' token
		}

		if ( this.currentToken().equals( ";" ) )
		{
			this.readToken(); //read ;
		}

		return resultString;
	}

	private void parseNotify()
	{
		String resultString = this.parseDirective( "notify" );
		if ( this.notifyRecipient == null )
		{
			this.notifyRecipient = resultString;
		}
	}

	private String parseImport()
	{
		return this.parseDirective( "import" );
	}

	public static final boolean validCoercion( Type lhs, Type rhs, final String oper )
	{
		// Resolve aliases

		lhs = lhs.getBaseType();
		rhs = rhs.getBaseType();

		// "oper" is either a standard operator or is a special name:
		//
		// "parameter" - value used as a function parameter
		//	lhs = parameter type, rhs = expression type
		//
		// "return" - value returned as function value
		//	lhs = function return type, rhs = expression type
		//
		// "assign" - value
		//	lhs = variable type, rhs = expression type

		// The "contains" operator requires an aggregate on the left
		// and the correct index type on the right.

		if ( oper.equals( "contains" ) )
		{
			return lhs.getType() == DataTypes.TYPE_AGGREGATE && ( (AggregateType) lhs ).getIndexType().equals(
				rhs );
		}

		// If the types are equal, no coercion is necessary
		if ( lhs.equals( rhs ) )
		{
			return true;
		}

		if ( lhs.equals( DataTypes.TYPE_ANY ) && rhs.getType() != DataTypes.TYPE_AGGREGATE )
		{
			return true;
		}

		// Anything coerces to a string
		if ( lhs.equals( DataTypes.TYPE_STRING ) )
		{
			return true;
		}

		// Anything coerces to a string for concatenation
		if ( oper.equals( "+" ) && rhs.equals( DataTypes.TYPE_STRING ) )
		{
			return true;
		}

		// Int coerces to float
		if ( lhs.equals( DataTypes.TYPE_INT ) && rhs.equals( DataTypes.TYPE_FLOAT ) )
		{
			return true;
		}

		if ( lhs.equals( DataTypes.TYPE_FLOAT ) && rhs.equals( DataTypes.TYPE_INT ) )
		{
			return true;
		}

		return false;
	}

	// **************** Tokenizer *****************

	private String getNextLine()
	{
		try
		{
			do
			{
				// Read a line from input, and break out of the
				// do-while loop when you've read a valid line

				this.fullLine = this.commandStream.readLine();

				// Return null at end of file
				if ( this.fullLine == null )
				{
					return null;
				}

				// Remove whitespace at front and end
				this.fullLine = this.fullLine.trim();
			}
			while ( this.fullLine.length() == 0 );

			// Found valid currentLine - return it

			return this.fullLine;
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
			return null;
		}
	}

	private String currentToken()
	{
		this.fixLines();
		if ( this.currentLine == null )
		{
			return null;
		}

		while ( this.currentLine.startsWith( "#" ) || this.currentLine.startsWith( "//" ) )
		{
			this.currentLine = "";

			this.fixLines();
			if ( this.currentLine == null )
			{
				return null;
			}
		}

		if ( !this.currentLine.trim().equals( "/*" ) )
		{
			return this.currentLine.substring( 0, this.tokenLength( this.currentLine ) );
		}

		while ( this.currentLine != null && !this.currentLine.trim().equals( "*/" ) )
		{
			this.currentLine = "";
			this.fixLines();
		}

		if ( this.currentLine == null )
		{
			return null;
		}

		this.currentLine = "";
		return this.currentToken();
	}

	private String nextToken()
	{
		this.fixLines();

		if ( this.currentLine == null )
		{
			return null;
		}

		if ( this.tokenLength( this.currentLine ) >= this.currentLine.length() )
		{
			if ( this.nextLine == null )
			{
				return null;
			}

			return this.nextLine.substring( 0, this.tokenLength( this.nextLine ) ).trim();
		}

		String result = this.currentLine.substring( this.tokenLength( this.currentLine ) ).trim();

		if ( result.equals( "" ) )
		{
			if ( this.nextLine == null )
			{
				return null;
			}

			return this.nextLine.substring( 0, this.tokenLength( this.nextLine ) );
		}

		return result.substring( 0, this.tokenLength( result ) );
	}

	private void readToken()
	{
		this.fixLines();

		if ( this.currentLine == null )
		{
			return;
		}

		this.currentLine = this.currentLine.substring( this.tokenLength( this.currentLine ) );
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
			if ( result + 1 < s.length() && this.tokenString( s.substring( result, result + 2 ) ) )
			{
				return result == 0 ? 2 : result;
			}

			if ( result < s.length() && this.tokenString( s.substring( result, result + 1 ) ) )
			{
				return result == 0 ? 1 : result;
			}
		}

		return result; //== s.length()
	}

	private void fixLines()
	{
		if ( this.currentLine == null )
		{
			return;
		}

		while ( this.currentLine.equals( "" ) )
		{
			this.currentLine = this.nextLine;
			this.lineNumber = this.commandStream.getLineNumber();
			this.nextLine = this.getNextLine();

			if ( this.currentLine == null )
			{
				return;
			}
		}

		this.currentLine = this.currentLine.trim();

		if ( this.nextLine == null )
		{
			return;
		}

		while ( this.nextLine.equals( "" ) )
		{
			this.nextLine = this.getNextLine();
			if ( this.nextLine == null )
			{
				return;
			}
		}

		this.nextLine = this.nextLine.trim();
	}

	private boolean tokenString( final String s )
	{
		if ( s.length() == 1 )
		{
			for ( int i = 0; i < Parser.tokenList.length; ++i )
			{
				if ( s.charAt( 0 ) == Parser.tokenList[ i ] )
				{
					return true;
				}
			}
			return false;
		}
		else
		{
			for ( int i = 0; i < Parser.multiCharTokenList.length; ++i )
			{
				if ( s.equalsIgnoreCase( Parser.multiCharTokenList[ i ] ) )
				{
					return true;
				}
			}

			return false;
		}
	}

	// **************** Parse errors *****************

	private final void parseError( final String expected, final String actual )
	{
		throw this.parseException( "Expected " + expected + ", found " + actual );
	}

	private final void parseError( final String message )
	{
		throw this.parseException( message );
	}

	private final ScriptException parseException( final String message )
	{
		return new ScriptException( message + " " + this.getLineAndFile() );
	}

	private final String getLineAndFile()
	{
		if ( this.fileName == null )
		{
			return "(" + Preferences.getString( "commandLineNamespace" ) + ")";
		}

		String partialName = this.fileName.substring( this.fileName.lastIndexOf( File.separator ) + 1 );
		return "(" + partialName + ", line " + this.lineNumber + ")";
	}

	public static void printIndices( final ValueList indices, final PrintStream stream, final int indent )
	{
		if ( indices == null )
		{
			return;
		}

		Iterator it = indices.iterator();
		while ( it.hasNext() )
		{
			Value current = (Value) it.next();
			Interpreter.indentLine( stream, indent );
			stream.println( "<KEY>" );
			current.print( stream, indent + 1 );
		}
	}
}
