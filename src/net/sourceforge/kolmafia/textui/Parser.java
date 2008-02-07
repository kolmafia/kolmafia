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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ByteArrayStream;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptAggregateType;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptCommand;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptNamedType;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptRecord;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptRecordType;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptSymbol;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptSymbolTable;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptType;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptTypeInitializer;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptTypeList;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptValue;
import net.sourceforge.kolmafia.textui.ParseTree;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptAssignment;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptBasicScript;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptBreak;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptCall;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptCompositeReference;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptConditional;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptContinue;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptElse;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptElseIf;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptExistingFunction;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptExit;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptExpression;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptFor;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptForeach;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptFunction;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptFunctionList;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptIf;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptList;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptOperator;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptRepeat;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptReturn;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptScope;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptUserDefinedFunction;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptValueList;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptVariable;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptVariableList;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptVariableReference;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptVariableReferenceList;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptWhile;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

public class Parser
{
	// Variables used during parsing

	private static Parser currentAnalysis = null;

	private String fileName;
	private InputStream istream;

	private LineNumberReader commandStream;
	private String currentLine;
	private String nextLine;
	private int lineNumber;

	private String fullLine;

	private TreeMap imports = new TreeMap();
	private ScriptFunction mainMethod = null;
	private String notifyRecipient = null;

	public Parser( final File scriptFile, final InputStream stream, final TreeMap imports )
	{
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

		if ( imports != null )
		{
			this.imports = imports;
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

			throw new AdvancedScriptException( this.fileName + " could not be accessed" );
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

	public ScriptScope parse()
	{
		Parser previousAnalysis = Parser.currentAnalysis;
                ScriptScope scope = null;

		try
		{
			Parser.currentAnalysis = this;
			scope = this.parseScope( null, null, new ScriptVariableList(), Parser.getExistingFunctionScope(), false );

			if ( this.currentLine != null )
			{
				throw new AdvancedScriptException( "Script parsing error" );
			}
		}
		finally
		{
			Parser.currentAnalysis = previousAnalysis;
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

	public ScriptFunction getMainMethod()
	{
		return this.mainMethod;
	}

	public String getNotifyRecipient()
	{
		return this.notifyRecipient;
	}

	public static ScriptScope getExistingFunctionScope()
	{
		return new ScriptScope( RuntimeLibrary.functions, null, DataTypes.simpleTypes );
	}

	// **************** Parser *****************

	public static final char[] tokenList =
		{ ' ', '.', ',', '{', '}', '(', ')', '$', '!', '+', '-', '=', '"', '\'', '*', '^', '/', '%', '[', ']', '!', ';', '<', '>' };
	public static final String[] multiCharTokenList = { "==", "!=", "<=", ">=", "||", "&&", "/*", "*/" };

	// Feature control;
	// disabled until and if we choose to document the feature

	private static final boolean arrays = false;

	private static final ScriptSymbolTable reservedWords = new ScriptSymbolTable();

	static
	{
		// Constants
		reservedWords.addElement( new ScriptSymbol( "true" ) );
		reservedWords.addElement( new ScriptSymbol( "false" ) );

		// Operators
		reservedWords.addElement( new ScriptSymbol( "contains" ) );
		reservedWords.addElement( new ScriptSymbol( "remove" ) );

		// Control flow
		reservedWords.addElement( new ScriptSymbol( "if" ) );
		reservedWords.addElement( new ScriptSymbol( "else" ) );
		reservedWords.addElement( new ScriptSymbol( "foreach" ) );
		reservedWords.addElement( new ScriptSymbol( "in" ) );
		reservedWords.addElement( new ScriptSymbol( "for" ) );
		reservedWords.addElement( new ScriptSymbol( "from" ) );
		reservedWords.addElement( new ScriptSymbol( "upto" ) );
		reservedWords.addElement( new ScriptSymbol( "downto" ) );
		reservedWords.addElement( new ScriptSymbol( "by" ) );
		reservedWords.addElement( new ScriptSymbol( "while" ) );
		reservedWords.addElement( new ScriptSymbol( "repeat" ) );
		reservedWords.addElement( new ScriptSymbol( "until" ) );
		reservedWords.addElement( new ScriptSymbol( "break" ) );
		reservedWords.addElement( new ScriptSymbol( "continue" ) );
		reservedWords.addElement( new ScriptSymbol( "return" ) );
		reservedWords.addElement( new ScriptSymbol( "exit" ) );

		// Data types
		reservedWords.addElement( new ScriptSymbol( "void" ) );
		reservedWords.addElement( new ScriptSymbol( "boolean" ) );
		reservedWords.addElement( new ScriptSymbol( "int" ) );
		reservedWords.addElement( new ScriptSymbol( "float" ) );
		reservedWords.addElement( new ScriptSymbol( "string" ) );
		reservedWords.addElement( new ScriptSymbol( "buffer" ) );
		reservedWords.addElement( new ScriptSymbol( "matcher" ) );

		reservedWords.addElement( new ScriptSymbol( "item" ) );
		reservedWords.addElement( new ScriptSymbol( "location" ) );
		reservedWords.addElement( new ScriptSymbol( "class" ) );
		reservedWords.addElement( new ScriptSymbol( "stat" ) );
		reservedWords.addElement( new ScriptSymbol( "skill" ) );
		reservedWords.addElement( new ScriptSymbol( "effect" ) );
		reservedWords.addElement( new ScriptSymbol( "familiar" ) );
		reservedWords.addElement( new ScriptSymbol( "slot" ) );
		reservedWords.addElement( new ScriptSymbol( "monster" ) );
		reservedWords.addElement( new ScriptSymbol( "element" ) );

		reservedWords.addElement( new ScriptSymbol( "record" ) );
		reservedWords.addElement( new ScriptSymbol( "typedef" ) );
	}

	private static final boolean isReservedWord( final String name )
	{
		return Parser.reservedWords.findSymbol( name ) != null;
	}

	public ScriptScope importFile( final String fileName, final ScriptScope scope )
	{
		File scriptFile = KoLmafiaCLI.findScriptFile( fileName );
		if ( scriptFile == null )
		{
			throw new AdvancedScriptException( fileName + " could not be found" );
		}

		if ( this.imports.containsKey( scriptFile ) )
		{
			return scope;
		}

		AdvancedScriptException error = null;

		ScriptScope result = scope;

		Parser previousAnalysis = Parser.currentAnalysis;

		try
		{
			Parser parser = new Parser( scriptFile, null, this.imports );
			Parser.currentAnalysis = parser;
			result = parser.parseScope( scope, null, new ScriptVariableList(), scope.getParentScope(), false );
		}
		catch ( Exception e )
		{
			error = new AdvancedScriptException( e );
		}
		finally
		{
			Parser.currentAnalysis.disconnect();
		}

		if ( error == null && Parser.currentAnalysis.currentLine != null )
		{
			error = new AdvancedScriptException( "Script parsing error" );
		}

		Parser.currentAnalysis = previousAnalysis;

		if ( error != null )
		{
			throw error;
		}

		this.imports.put( scriptFile, new Long( scriptFile.lastModified() ) );
		this.mainMethod = null;

		return result;
	}

	private ScriptScope parseScope( final ScriptScope startScope, final ScriptType expectedType,
		final ScriptVariableList variables, final ScriptScope parentScope, final boolean whileLoop )
	{
		ScriptScope result;
		String importString;

		result = startScope == null ? new ScriptScope( variables, parentScope ) : startScope;
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

			ScriptType t = this.parseType( result, true, true );

			// If there is no data type, it's a command of some sort
			if ( t == null )
			{
				// See if it's a regular command
				ScriptCommand c = this.parseCommand( expectedType, result, false, whileLoop );
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

			ScriptFunction f = this.parseFunction( t, result );
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
			throw new AdvancedScriptException( "Script parse error" );
		}

		return result;
	}

	private ScriptType parseRecord( final ScriptScope parentScope )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "record" ) )
		{
			return null;
		}

		this.readToken(); // read record

		if ( this.currentToken() == null )
		{
			throw new AdvancedScriptException( "Record name expected" );
		}

		// Allow anonymous records
		String recordName = null;

		if ( !this.currentToken().equals( "{" ) )
		{
			// Named record
			recordName = this.currentToken();

			if ( !this.parseIdentifier( recordName ) )
			{
				throw new AdvancedScriptException( "Invalid record name '" + recordName + "'" );
			}

			if ( Parser.isReservedWord( recordName ) )
			{
				throw new AdvancedScriptException( "Reserved word '" + recordName + "' cannot be a record name" );
			}

			if ( parentScope.findType( recordName ) != null )
			{
				throw new AdvancedScriptException( "Record name '" + recordName + "' is already defined" );
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
			ScriptType fieldType = this.parseType( parentScope, true, true );
			if ( fieldType == null )
			{
				throw new AdvancedScriptException( "Type name expected" );
			}

			// Get the field name
			String fieldName = this.currentToken();
			if ( fieldName == null )
			{
				throw new AdvancedScriptException( "Field name expected" );
			}

			if ( !this.parseIdentifier( fieldName ) )
			{
				throw new AdvancedScriptException( "Invalid field name '" + fieldName + "'" );
			}

			if ( Parser.isReservedWord( fieldName ) )
			{
				throw new AdvancedScriptException( "Reserved word '" + fieldName + "' cannot be used as a field name" );
			}

			if ( fieldNames.contains( fieldName ) )
			{
				throw new AdvancedScriptException( "Field name '" + fieldName + "' is already defined" );
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
		ScriptType[] fieldTypeArray = new ScriptType[ fieldTypes.size() ];
		fieldNames.toArray( fieldNameArray );
		fieldTypes.toArray( fieldTypeArray );

		ScriptRecordType rec =
			new ScriptRecordType(
				recordName == null ? "(anonymous record)" : recordName, fieldNameArray, fieldTypeArray );

		if ( recordName != null )
		{
			// Enter into type table
			parentScope.addType( rec );
		}

		return rec;
	}

	private ScriptFunction parseFunction( final ScriptType functionType, final ScriptScope parentScope )
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
			throw new AdvancedScriptException( "Reserved word '" + functionName + "' cannot be used as a function name" );
		}

		this.readToken(); //read Function name
		this.readToken(); //read (

		ScriptVariableList paramList = new ScriptVariableList();
		ScriptVariableReferenceList variableReferences = new ScriptVariableReferenceList();

		while ( !this.currentToken().equals( ")" ) )
		{
			ScriptType paramType = this.parseType( parentScope, true, false );
			if ( paramType == null )
			{
				this.parseError( ")", this.currentToken() );
			}

			ScriptVariable param = this.parseVariable( paramType, null );
			if ( param == null )
			{
				this.parseError( "identifier", this.currentToken() );
			}

			if ( !paramList.addElement( param ) )
			{
				throw new AdvancedScriptException( "Variable " + param.getName() + " is already defined" );
			}

			if ( !this.currentToken().equals( ")" ) )
			{
				if ( !this.currentToken().equals( "," ) )
				{
					this.parseError( ")", this.currentToken() );
				}

				this.readToken(); //read comma
			}

			variableReferences.addElement( new ScriptVariableReference( param ) );
		}

		this.readToken(); //read )

		// Add the function to the parent scope before we parse the
		// function scope to allow recursion. Replace an existing
		// forward reference.

		ScriptUserDefinedFunction result =
			parentScope.replaceFunction( new ScriptUserDefinedFunction( functionName, functionType, variableReferences ) );
		if ( this.currentToken() != null && this.currentToken().equals( ";" ) )
		{
			// Yes. Return forward reference
			this.readToken(); // ;
			return result;
		}

		ScriptScope scope;
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
			scope = new ScriptScope( paramList, parentScope );
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
			throw new AdvancedScriptException( "Missing return value" );
		}

		return result;
	}

	private boolean parseVariables( final ScriptType t, final ScriptScope parentScope )
	{
		while ( true )
		{
			ScriptVariable v = this.parseVariable( t, parentScope );
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

	private ScriptVariable parseVariable( final ScriptType t, final ScriptScope scope )
	{
		if ( !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		String variableName = this.currentToken();
		if ( Parser.isReservedWord( variableName ) )
		{
			throw new AdvancedScriptException( "Reserved word '" + variableName + "' cannot be a variable name" );
		}

		ScriptVariable result = new ScriptVariable( variableName, t );
		if ( scope != null && !scope.addVariable( result ) )
		{
			throw new AdvancedScriptException( "Variable " + result.getName() + " is already defined" );
		}

		this.readToken(); // If parsing of Identifier succeeded, go to next token.
		// If we are parsing a parameter declaration, we are done
		if ( scope == null )
		{
			if ( this.currentToken().equals( "=" ) )
			{
				throw new AdvancedScriptException( "Cannot initialize parameter " + result.getName() );
			}
			return result;
		}

		// Otherwise, we must initialize the variable.

		ScriptVariableReference lhs = new ScriptVariableReference( result.getName(), scope );
		ScriptValue rhs;

		if ( this.currentToken().equals( "=" ) )
		{
			this.readToken(); // Eat the equals sign
			rhs = this.parseExpression( scope );

			if ( rhs == null )
			{
				throw new AdvancedScriptException( "Expression expected" );
			}

			if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
			{
				throw new AdvancedScriptException(
					"Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() );
			}
		}
		else
		{
			rhs = null;
		}

		scope.addCommand( new ScriptAssignment( lhs, rhs ) );
		return result;
	}

	private boolean parseTypedef( final ScriptScope parentScope )
	{
		if ( this.currentToken() == null || !this.currentToken().equalsIgnoreCase( "typedef" ) )
		{
			return false;
		}
		this.readToken(); // read typedef

		ScriptType t = this.parseType( parentScope, true, true );
		if ( t == null )
		{
			throw new AdvancedScriptException( "Missing data type for typedef" );
		}

		String typeName = this.currentToken();

		if ( !this.parseIdentifier( typeName ) )
		{
			throw new AdvancedScriptException( "Invalid type name '" + typeName + "'" );
		}

		if ( Parser.isReservedWord( typeName ) )
		{
			throw new AdvancedScriptException( "Reserved word '" + typeName + "' cannot be a type name" );
		}

		if ( parentScope.findType( typeName ) != null )
		{
			throw new AdvancedScriptException( "Type name '" + typeName + "' is already defined" );
		}

		this.readToken(); // read name

		// Add the type to the type table
		ScriptNamedType type = new ScriptNamedType( typeName, t );
		parentScope.addType( type );

		return true;
	}

	private ScriptCommand parseCommand( final ScriptType functionType, final ScriptScope scope, final boolean noElse,
		boolean whileLoop )
	{
		ScriptCommand result;

		if ( this.currentToken() == null )
		{
			return null;
		}

		if ( this.currentToken().equalsIgnoreCase( "break" ) )
		{
			if ( !whileLoop )
			{
				throw new AdvancedScriptException( "Encountered 'break' outside of loop" );
			}

			result = new ScriptBreak();
			this.readToken(); //break
		}

		else if ( this.currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( !whileLoop )
			{
				throw new AdvancedScriptException( "Encountered 'continue' outside of loop" );
			}

			result = new ScriptContinue();
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

	private ScriptType parseType( final ScriptScope scope, final boolean aggregates, final boolean records )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		ScriptType valType = scope.findType( this.currentToken() );
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

	private ScriptType parseAggregateType( final ScriptType dataType, final ScriptScope scope )
	{
		this.readToken(); // [ or ,
		if ( this.currentToken() == null )
		{
			throw new AdvancedScriptException( "Missing index token" );
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
					return new ScriptAggregateType( this.parseAggregateType( dataType, scope ), size );
				}

				return new ScriptAggregateType( dataType, size );
			}

			if ( this.currentToken().equals( "," ) )
			{
				return new ScriptAggregateType( this.parseAggregateType( dataType, scope ), size );
			}

			this.parseError( "]", this.currentToken() );
		}

		ScriptType indexType = scope.findType( this.currentToken() );
		if ( indexType == null )
		{
			throw new AdvancedScriptException( "Invalid type name '" + this.currentToken() + "'" );
		}

		if ( !indexType.isPrimitive() )
		{
			throw new AdvancedScriptException( "Index type '" + this.currentToken() + "' is not a primitive type" );
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
				return new ScriptAggregateType( this.parseAggregateType( dataType, scope ), indexType );
			}

			return new ScriptAggregateType( dataType, indexType );
		}

		if ( this.currentToken().equals( "," ) )
		{
			return new ScriptAggregateType( this.parseAggregateType( dataType, scope ), indexType );
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

	private ScriptReturn parseReturn( final ScriptType expectedType, final ScriptScope parentScope )
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
				return new ScriptReturn( null, DataTypes.VOID_TYPE );
			}

			throw new AdvancedScriptException( "Return needs " + expectedType + " value" );
		}
		else
		{
			if ( expectedType != null && expectedType.equals( DataTypes.TYPE_VOID ) )
			{
				throw new AdvancedScriptException( "Cannot return a value from a void function" );
			}

			ScriptValue value = this.parseExpression( parentScope );
		
			if ( value == null )
			{
				throw new AdvancedScriptException( "Expression expected" );
			}

			if ( !Parser.validCoercion( expectedType, value.getType(), "return" ) )
			{
				throw new AdvancedScriptException( "Cannot return " + value.getType() + " value from " + expectedType + " function");
			}

			return new ScriptReturn( value, expectedType );
		}
	}

	private ScriptConditional parseConditional( final ScriptType functionType, final ScriptScope parentScope,
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

		ScriptValue condition = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( condition.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw new AdvancedScriptException( "\"if\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		ScriptIf result = null;
		boolean elseFound = false;
		boolean finalElse = false;

		do
		{
			ScriptScope scope;

			if ( this.currentToken() == null || !this.currentToken().equals( "{" ) ) //Scope is a single call
			{
				ScriptCommand command = this.parseCommand( functionType, parentScope, !elseFound, loop );
				scope = new ScriptScope( command, parentScope );
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
				result = new ScriptIf( scope, condition );
			}
			else if ( finalElse )
			{
				result.addElseLoop( new ScriptElse( scope, condition ) );
			}
			else
			{
				result.addElseLoop( new ScriptElseIf( scope, condition ) );
			}

			if ( !noElse && this.currentToken() != null && this.currentToken().equalsIgnoreCase( "else" ) )
			{
				if ( finalElse )
				{
					throw new AdvancedScriptException( "Else without if" );
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

					if ( condition.getType() != DataTypes.BOOLEAN_TYPE )
					{
						throw new AdvancedScriptException( "\"if\" requires a boolean conditional expression" );
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

	private ScriptBasicScript parseBasicScript()
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

		return new ScriptBasicScript( ostream );
	}

	private ScriptWhile parseWhile( final ScriptType functionType, final ScriptScope parentScope )
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

		ScriptValue expression = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( expression.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw new AdvancedScriptException( "\"while\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		ScriptScope scope = this.parseLoopScope( functionType, null, parentScope );

		return new ScriptWhile( scope, expression );
	}

	private ScriptRepeat parseRepeat( final ScriptType functionType, final ScriptScope parentScope )
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

		ScriptScope scope = this.parseLoopScope( functionType, null, parentScope );
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

		ScriptValue expression = this.parseExpression( parentScope );
		if ( this.currentToken() == null || !this.currentToken().equals( ")" ) )
		{
			this.parseError( ")", this.currentToken() );
		}

		if ( expression.getType() != DataTypes.BOOLEAN_TYPE )
		{
			throw new AdvancedScriptException( "\"repeat\" requires a boolean conditional expression" );
		}

		this.readToken(); // )

		return new ScriptRepeat( scope, expression );
	}

	private ScriptForeach parseForeach( final ScriptType functionType, final ScriptScope parentScope )
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
				throw new AdvancedScriptException( "Key variable name expected" );
			}

			if ( parentScope.findVariable( name ) != null )
			{
				throw new AdvancedScriptException( "Key variable '" + name + "' is already defined" );
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
		ScriptValue aggregate = this.parseVariableReference( parentScope );

		if ( aggregate == null || !( aggregate instanceof ScriptVariableReference ) || !( aggregate.getType().getBaseType() instanceof ScriptAggregateType ) )
		{
			throw new AdvancedScriptException( "Aggregate reference expected" );
		}

		// Define key variables of appropriate type
		ScriptVariableList varList = new ScriptVariableList();
		ScriptVariableReferenceList variableReferences = new ScriptVariableReferenceList();
		ScriptType type = aggregate.getType().getBaseType();

		for ( int i = 0; i < names.size(); ++i )
		{
			if ( !( type instanceof ScriptAggregateType ) )
			{
				throw new AdvancedScriptException( "Too many key variables specified" );
			}

			ScriptType itype = ( (ScriptAggregateType) type ).getIndexType();
			ScriptVariable keyvar = new ScriptVariable( (String) names.get( i ), itype );
			varList.addElement( keyvar );
			variableReferences.addElement( new ScriptVariableReference( keyvar ) );
			type = ( (ScriptAggregateType) type ).getDataType();
		}

		// Parse the scope with the list of keyVars
		ScriptScope scope = this.parseLoopScope( functionType, varList, parentScope );

		// Add the foreach node with the list of varRefs
		return new ScriptForeach( scope, variableReferences, (ScriptVariableReference) aggregate );
	}

	private ScriptFor parseFor( final ScriptType functionType, final ScriptScope parentScope )
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
			throw new AdvancedScriptException( "Index variable '" + name + "' is already defined" );
		}

		this.readToken(); // for
		this.readToken(); // name

		if ( !this.currentToken().equalsIgnoreCase( "from" ) )
		{
			this.parseError( "from", this.currentToken() );
		}

		this.readToken(); // from

		ScriptValue initial = this.parseExpression( parentScope );

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

		ScriptValue last = this.parseExpression( parentScope );

		ScriptValue increment = DataTypes.ONE_VALUE;
		if ( this.currentToken().equalsIgnoreCase( "by" ) )
		{
			this.readToken(); // by
			increment = this.parseExpression( parentScope );
		}

		// Create integer index variable
		ScriptVariable indexvar = new ScriptVariable( name, DataTypes.INT_TYPE );

		// Put index variable onto a list
		ScriptVariableList varList = new ScriptVariableList();
		varList.addElement( indexvar );

		ScriptScope scope = this.parseLoopScope( functionType, varList, parentScope );

		return new ScriptFor( scope, new ScriptVariableReference( indexvar ), initial, last, increment, direction );
	}

	private ScriptScope parseLoopScope( final ScriptType functionType, final ScriptVariableList varList,
		final ScriptScope parentScope )
	{
		ScriptScope scope;

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
			scope = new ScriptScope( varList, parentScope );
			scope.addCommand( this.parseCommand( functionType, scope, false, true ) );
		}

		return scope;
	}

	private ScriptValue parseCall( final ScriptScope scope )
	{
		return this.parseCall( scope, null );
	}

	private ScriptValue parseCall( final ScriptScope scope, final ScriptValue firstParam )
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

		ScriptValueList params = new ScriptValueList();
		if ( firstParam != null )
		{
			params.addElement( firstParam );
		}

		while ( this.currentToken() != null && !this.currentToken().equals( ")" ) )
		{
			ScriptValue val = this.parseExpression( scope );
			if ( val != null )
			{
				params.addElement( val );
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

		ScriptCall call = new ScriptCall( name, scope, params );
		if ( call.getTarget() == null )
		{
			throw new AdvancedScriptException( "Undefined reference '" + name + "'" );
		}

		ScriptValue result = call;
		while ( result != null && this.currentToken() != null && this.currentToken().equals( "." ) )
		{
			ScriptVariable current = new ScriptVariable( result.getType() );
			current.setExpression( result );

			result = this.parseVariableReference( scope, current );
		}

		return result;
	}

	private ScriptCommand parseAssignment( final ScriptScope scope )
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

		ScriptValue lhs = this.parseVariableReference( scope );
		if ( lhs instanceof ScriptCall )
		{
			return lhs;
		}

		if ( lhs == null || !( lhs instanceof ScriptVariableReference ) )
		{
			throw new AdvancedScriptException( "Variable reference expected" );
		}

		if ( !this.currentToken().equals( "=" ) )
		{
			return null;
		}

		this.readToken(); //=

		ScriptValue rhs = this.parseExpression( scope );

		if ( rhs == null )
		{
			throw new AdvancedScriptException( "Internal error" );
		}

		if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), "assign" ) )
		{
			throw new AdvancedScriptException(
				"Cannot store " + rhs.getType() + " in " + lhs + " of type " + lhs.getType() );
		}

		return new ScriptAssignment( (ScriptVariableReference) lhs, rhs );
	}

	private ScriptValue parseRemove( final ScriptScope scope )
	{
		if ( this.currentToken() == null || !this.currentToken().equals( "remove" ) )
		{
			return null;
		}

		ScriptValue lhs = this.parseExpression( scope );

		if ( lhs == null )
		{
			throw new AdvancedScriptException( "Bad 'remove' statement" );
		}

		return lhs;
	}

	private ScriptValue parseExpression( final ScriptScope scope )
	{
		return this.parseExpression( scope, null );
	}

	private ScriptValue parseExpression( final ScriptScope scope, final ScriptOperator previousOper )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		ScriptValue lhs = null;
		ScriptValue rhs = null;
		ScriptOperator oper = null;

		if ( this.currentToken().equals( "!" ) )
		{
			String operator = this.currentToken();
			this.readToken(); // !
			if ( ( lhs = this.parseValue( scope ) ) == null )
			{
				throw new AdvancedScriptException( "Value expected" );
			}

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
			if ( lhs.getType() != DataTypes.BOOLEAN_TYPE )
			{
				throw new AdvancedScriptException( "\"!\" operator requires a boolean value" );
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
				throw new AdvancedScriptException( "Value expected" );
			}

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
		}
		else if ( this.currentToken().equals( "remove" ) )
		{
			String operator = this.currentToken();
			this.readToken(); // remove

			lhs = this.parseVariableReference( scope );
			if ( lhs == null || !( lhs instanceof ScriptCompositeReference ) )
			{
				throw new AdvancedScriptException( "Aggregate reference expected" );
			}

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
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
				throw new AdvancedScriptException( "Value expected" );
			}

			if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), oper.toString() ) )
			{
				throw new AdvancedScriptException(
					"Cannot apply operator " + oper + " to " + lhs + " (" + lhs.getType() + ") and " + rhs + " (" + rhs.getType() + ")" );
			}

			lhs = new ScriptExpression( lhs, rhs, oper );
		}
		while ( true );
	}

	private ScriptValue parseValue( final ScriptScope scope )
	{
		if ( this.currentToken() == null )
		{
			return null;
		}

		ScriptValue result = null;

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

		ScriptVariable current;
		while ( result != null && this.currentToken() != null && this.currentToken().equals( "." ) )
		{
			current = new ScriptVariable( result.getType() );
			current.setExpression( result );

			result = this.parseVariableReference( scope, current );
		}

		return result;
	}

	private ScriptValue parseNumber()
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
			return new ScriptValue( sign * StaticEntity.parseFloat( "0." + fraction ) );
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
				return new ScriptValue( sign * StaticEntity.parseInt( integer ) );
			}

			this.readToken(); // .
			this.readToken(); // fraction

			return new ScriptValue( sign * StaticEntity.parseFloat( integer + "." + fraction ) );
		}

		return new ScriptValue( sign * StaticEntity.parseInt( integer ) );
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

	private ScriptValue parseString()
	{
		// Directly work with currentLine - ignore any "tokens" you meet until
		// the string is closed

		StringBuffer resultString = new StringBuffer();
		char startCharacter = this.currentLine.charAt( 0 );

		for ( int i = 1;; ++i )
		{
			if ( i == this.currentLine.length() )
			{
				throw new AdvancedScriptException( "No closing \" found" );
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
				return new ScriptValue( resultString.toString() );
			}
			else
			{
				resultString.append( this.currentLine.charAt( i ) );
			}
		}
	}

	private ScriptValue parseTypedConstant( final ScriptScope scope )
	{
		this.readToken(); // read $

		String name = this.currentToken();
		ScriptType type = this.parseType( scope, false, false );
		if ( type == null || !type.isPrimitive() )
		{
			throw new AdvancedScriptException( "Unknown type " + name );
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
				throw new AdvancedScriptException( "No closing ] found" );
			}
			else if ( this.currentLine.charAt( i ) == '\\' )
			{
				resultString.append( this.currentLine.charAt( ++i ) );
			}
			else if ( this.currentLine.charAt( i ) == ']' )
			{
				this.currentLine = this.currentLine.substring( i + 1 ); //+1 to get rid of ']' token
				String input = resultString.toString().trim();
				ScriptValue value = DataTypes.parseValue( type, input, false );
				if ( value == null )
				{
					throw new AdvancedScriptException( "Bad " + type.toString() + " value: \"" + input + "\"" );
				}
				return value;
			}
			else
			{
				resultString.append( this.currentLine.charAt( i ) );
			}
		}
	}

	private ScriptOperator parseOperator( final String oper )
	{
		if ( oper == null || !this.isOperator( oper ) )
		{
			return null;
		}

		return new ScriptOperator( oper );
	}

	private boolean isOperator( final String oper )
	{
		return  oper.equals( "!" ) ||
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

	private ScriptValue parseVariableReference( final ScriptScope scope )
	{
		if ( this.currentToken() == null || !this.parseIdentifier( this.currentToken() ) )
		{
			return null;
		}

		String name = this.currentToken();
		ScriptVariable var = scope.findVariable( name, true );

		if ( var == null )
		{
			throw new AdvancedScriptException( "Unknown variable '" + name + "'" );
		}

		this.readToken(); // read name

		if ( this.currentToken() == null || !this.currentToken().equals( "[" ) && !this.currentToken().equals( "." ) )
		{
			return new ScriptVariableReference( var );
		}

		return this.parseVariableReference( scope, var );
	}

	private ScriptValue parseVariableReference( final ScriptScope scope, final ScriptVariable var )
	{
		ScriptType type = var.getType();
		ScriptValueList indices = new ScriptValueList();

		boolean parseAggregate = this.currentToken().equals( "[" );

		while ( this.currentToken() != null && ( this.currentToken().equals( "[" ) || this.currentToken().equals( "." ) || parseAggregate && this.currentToken().equals(
			"," ) ) )
		{
			ScriptValue index;

			type = type.getBaseType();

			if ( this.currentToken().equals( "[" ) || this.currentToken().equals( "," ) )
			{
				this.readToken(); // read [ or . or ,
				parseAggregate = true;

				if ( !( type instanceof ScriptAggregateType ) )
				{
					if ( indices.isEmpty() )
					{
						throw new AdvancedScriptException( "Variable '" + var.getName() + "' cannot be indexed" );
					}
					else
					{
						throw new AdvancedScriptException( "Too many keys for '" + var.getName() + "'" );
					}
				}

				ScriptAggregateType atype = (ScriptAggregateType) type;
				index = this.parseExpression( scope );
				if ( index == null )
				{
					throw new AdvancedScriptException( "Index for '" + var.getName() + "' expected" );
				}

				if ( !index.getType().equals( atype.getIndexType() ) )
				{
					throw new AdvancedScriptException(
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
						scope, indices.isEmpty() ? new ScriptVariableReference( var ) : new ScriptCompositeReference(
							var, indices ) );
				}

				if ( !( type instanceof ScriptRecordType ) )
				{
					throw new AdvancedScriptException( "Record expected" );
				}

				ScriptRecordType rtype = (ScriptRecordType) type;

				String field = this.currentToken();
				if ( field == null || !this.parseIdentifier( field ) )
				{
					throw new AdvancedScriptException( "Field name expected" );
				}

				index = rtype.getFieldIndex( field );
				if ( index == null )
				{
					throw new AdvancedScriptException( "Invalid field name '" + field + "'" );
				}
				this.readToken(); // read name
				type = rtype.getDataType( index );
			}

			indices.addElement( index );

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

		return new ScriptCompositeReference( var, indices );
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
			throw new AdvancedScriptException( "No closing > found" );
		}

		if ( startIndex == -1 )
		{
			startIndex = this.currentLine.indexOf( "\"" );
			endIndex = this.currentLine.indexOf( "\"", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
			{
				throw new AdvancedScriptException( "No closing \" found" );
			}
		}

		if ( startIndex == -1 )
		{
			startIndex = this.currentLine.indexOf( "\'" );
			endIndex = this.currentLine.indexOf( "\'", startIndex + 1 );

			if ( startIndex != -1 && endIndex == -1 )
			{
				throw new AdvancedScriptException( "No closing \' found" );
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

	public static final boolean validCoercion( ScriptType lhs, ScriptType rhs, final String oper )
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
			return lhs.getType() == DataTypes.TYPE_AGGREGATE && ( (ScriptAggregateType) lhs ).getIndexType().equals(
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

	private void parseError( final String expected, final String actual )
	{
		throw new AdvancedScriptException( "Expected " + expected + ", found " + actual );
	}

	private static String getCurrentLineAndFile()
	{
		if ( Parser.currentAnalysis == null )
		{
			return "";
		}

		return Parser.currentAnalysis.getLineAndFile();
	}

	private String getLineAndFile()
	{
		if ( this.fileName == null )
		{
			return "(" + Preferences.getString( "commandLineNamespace" ) + ")";
		}

		String partialName = this.fileName.substring( this.fileName.lastIndexOf( File.separator ) + 1 );
		return "(" + partialName + ", line " + this.lineNumber + ")";
	}

	public static class AdvancedScriptException
		extends RuntimeException
	{
		AdvancedScriptException( final Throwable t )
		{
			this( t.getMessage() == null ? "" : t.getMessage() + " " + Parser.getCurrentLineAndFile() );
		}

		AdvancedScriptException( final String s )
		{
			super( s == null ? "" : s + " " + Parser.getCurrentLineAndFile() );
		}
	}
}
