/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

// input and output
import java.io.LineNumberReader;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.NumberFormatException;

// utility imports
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.DataUtilities;

//Parameter value requests
import javax.swing.JOptionPane;

/**
 * The main private class for the <code>KoLmafia</code> package.  This
 * private class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public class KoLmafiaASH extends StaticEntity
{
	/* Variables for Advanced Scripting */
	public final static char [] tokenList = { ' ', ',', '{', '}', '(', ')', '$', '!', '+', '-', '=', '"', '*', '/', '%', '[', ']', '!', ';', '<', '>' };
	public final static String [] multiCharTokenList = { "==", "!=", "<=", ">=", "||", "&&" };

	public static final int TYPE_VOID = 0;
	public static final int TYPE_BOOLEAN = 1;
	public static final int TYPE_INT = 2;
	public static final int TYPE_FLOAT = 3;
	public static final int TYPE_STRING = 4;

	public static final int TYPE_ITEM = 100;
	public static final int TYPE_ZODIAC = 101;
	public static final int TYPE_LOCATION = 102;
	public static final int TYPE_CLASS = 103;
	public static final int TYPE_STAT = 104;
	public static final int TYPE_SKILL = 105;
	public static final int TYPE_EFFECT = 106;
	public static final int TYPE_FAMILIAR = 107;
	public static final int TYPE_SLOT = 108;
	public static final int TYPE_MONSTER = 109;

	public static final String [] ZODIACS = { "none", "wallaby", "mongoose", "vole", "platypus", "opossum", "marmot", "wombat", "blender", "packrat" };
	public static final String [] CLASSES = { "seal clubber", "turtle tamer", "pastamancer", "sauceror", "disco bandit", "accordion thief" };
	public static final String [] STATS = { "muscle", "mysticality", "moxie" };
	public static final String [] BOOLEANS = { "true", "false" };

	public static final int COMMAND_BREAK = 1;
	public static final int COMMAND_CONTINUE = 2;
	public static final int COMMAND_EXIT = 3;

	public static final int STATE_NORMAL = 1;
	public static final int STATE_RETURN = 2;
	public static final int STATE_BREAK = 3;
	public static final int STATE_CONTINUE = 4;
	public static final int STATE_EXIT = 5;

	public int currentState = STATE_NORMAL;

	private static final String escapeString = "//";

	private ScriptScope global;
	private String line;
	private String nextLine;
	private int lineNumber;

	private static ArrayList imports = new ArrayList();
	public String fileName;
	public LineNumberReader commandStream;

	private static final ScriptType VOID_TYPE = new ScriptType( TYPE_VOID );
	private static final ScriptType BOOLEAN_TYPE = new ScriptType( TYPE_BOOLEAN );
	private static final ScriptType INT_TYPE = new ScriptType( TYPE_INT );
	private static final ScriptType FLOAT_TYPE = new ScriptType( TYPE_FLOAT );
	private static final ScriptType STRING_TYPE = new ScriptType( TYPE_STRING );

	private static final ScriptType ITEM_TYPE = new ScriptType( TYPE_ITEM );
	private static final ScriptType ZODIAC_TYPE = new ScriptType( TYPE_ZODIAC );
	private static final ScriptType LOCATION_TYPE = new ScriptType( TYPE_LOCATION );
	private static final ScriptType CLASS_TYPE = new ScriptType( TYPE_CLASS );
	private static final ScriptType STAT_TYPE = new ScriptType( TYPE_STAT );
	private static final ScriptType SKILL_TYPE = new ScriptType( TYPE_SKILL );
	private static final ScriptType EFFECT_TYPE = new ScriptType( TYPE_EFFECT );
	private static final ScriptType FAMILIAR_TYPE = new ScriptType( TYPE_FAMILIAR );
	private static final ScriptType SLOT_TYPE = new ScriptType( TYPE_SLOT );
	private static final ScriptType MONSTER_TYPE = new ScriptType( TYPE_MONSTER );

	private ScriptValue VOID_VALUE = new ScriptValue();
	private ScriptValue TRUE_VALUE = new ScriptValue( true );
	private ScriptValue FALSE_VALUE = new ScriptValue( false );


	// **************** Tracing *****************

	private static boolean tracing = true;
	private static int traceIndentation = 0;

	private static void resetTracing()
	{
		traceIndentation = 0;
	}

	private static void traceIndent()
	{	traceIndentation++;
	}

	private static void traceUnindent()
	{	traceIndentation--;
	}

	private static void trace( String string )
	{
		if ( tracing )
		{
			indentLine( traceIndentation );
			KoLmafia.getDebugStream().println( string );
		}
	}

	private static String executionStateString( int state )
	{
		switch ( state )
		{
		case STATE_NORMAL:
			return "NORMAL";
		case STATE_RETURN:
			return "RETURN";
		case STATE_BREAK:
			return "BREAK";
		case STATE_CONTINUE:
			return "CONTINUE";
		case STATE_EXIT:
			return "EXIT";
		}

		return String.valueOf(state);
	}

	// **************** Parsing *****************

	public void validate( File scriptFile ) throws IOException
	{
		this.commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( scriptFile ) ) );
		this.fileName = scriptFile.getPath();
		this.imports.clear();

		this.line = getNextLine();
		this.lineNumber = commandStream.getLineNumber();
		this.nextLine = getNextLine();

		try
		{
			this.global = parseScope( null, null, new ScriptVariableList(), getExistingFunctionScope(), false );

			if ( this.line != null )
				throw new AdvancedScriptException( "Script parsing error " + getLineAndFile() );

			this.commandStream.close();
			printScope( global, 0 );
		}
		catch ( AdvancedScriptException e )
		{
			this.commandStream.close();
			this.commandStream = null;
			printStackTrace( e, e.getMessage() );
		}
	}
	
	public void execute( File scriptFile ) throws IOException
	{
		// Before you do anything, validate the script.
		validate( scriptFile );

		if ( this.commandStream == null )
			return;
		
		try
		{
			ScriptValue result = executeGlobalScope( global );

			if ( !client.permitsContinue() || result == null || result.getType() == null )
			{
				DEFAULT_SHELL.printLine( "Script aborted!" );
				return;
			}
			
			if ( result.getType().equals( TYPE_VOID ) )
				DEFAULT_SHELL.printLine( !client.permitsContinue() ? "Script failed!" : "Script succeeded!" );
			else if ( result.getType().equals( TYPE_BOOLEAN ) )
				DEFAULT_SHELL.printLine( result.intValue() == 0 ? "Script failed!" : "Script succeeded!" );
			else if ( result.getType().equals( TYPE_STRING ) )
				DEFAULT_SHELL.printLine( result.toString() );
			else
				DEFAULT_SHELL.printLine(  "Script returned value " + result );

		}
		catch ( AdvancedScriptException e )
		{
			printStackTrace( e, e.getMessage() );
		}
		catch ( RuntimeException e )
		{
			// If it's an exception resulting from
			// a premature abort, which causes void
			// values to be return, ignore.
			
			if ( !e.getMessage().startsWith( "Cannot" ) )
				printStackTrace( e, e.getMessage() );				
		}
	}


	private String getNextLine()
	{
		try
		{
			String line;

			do
			{
				// Read a line from input, and break out of the do-while
				// loop when you've read a valid line (which is a non-comment
				// and a non-blank line ) or when you've reached EOF.

				line = commandStream.readLine();
			}
			while ( line != null && (line.trim().length() == 0 || line.trim().startsWith( "#" ) || line.trim().startsWith( "//" ) || line.trim().startsWith( "\'" )) );

			// You will either have reached the end of file, or you will
			// have a valid line -- return it.

			return line == null ? null : line.trim();
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			printStackTrace( e );
			return null;
		}
	}


	private ScriptScope parseFile( String fileName, ScriptScope startScope, ScriptScope parentScope ) throws AdvancedScriptException, java.io.FileNotFoundException
	{
		ScriptScope result;
		this.fileName = fileName;

		File scriptFile = new File( "scripts" + File.separator + fileName );
		if ( !scriptFile.exists() )
			scriptFile = new File( "scripts" + File.separator + fileName + ".ash" );

		if ( scriptFile.exists() )
		{
			String name = scriptFile.toString();
			if ( imports.contains( name ) )
				return startScope;
			imports.add( name );
		}

		commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( scriptFile ) ) );

		line = getNextLine();
		lineNumber = commandStream.getLineNumber();
		nextLine = getNextLine();

		result = parseScope( startScope, null, new ScriptVariableList(), parentScope, false );

		try
		{
			commandStream.close();
		}
		catch ( IOException e )
		{
		}

		if ( line != null )
			throw new AdvancedScriptException( "Script parsing error " + getLineAndFile() );

		return result;
	}

	private ScriptScope parseScope( ScriptScope startScope, ScriptType expectedType, ScriptVariableList variables, ScriptScope parentScope, boolean whileLoop ) throws AdvancedScriptException
	{
		ScriptFunction f = null;
		ScriptVariable v = null;
		ScriptCommand c = null;
		ScriptType t = null;
		ScriptScope result;
		String importString;

		result = startScope == null ? new ScriptScope( variables, parentScope ) : startScope;

		while ( (importString = parseImport()) != null )
		{
			if ( currentToken().equals( ";" ) )
				readToken(); //read ;
			else
				throw new AdvancedScriptException( "';' Expected " + getLineAndFile() );
			try
			{
				result = new KoLmafiaASH().parseFile( importString, result, parentScope );
			}
			catch( java.io.FileNotFoundException e )
			{
				throw new AdvancedScriptException( "File " + importString + " not found " + getLineAndFile() );
			}
		}

		while ( true )
		{
			if ( (t = parseType()) == null )
			{
				if ( (c = parseCommand( expectedType, result, false, whileLoop )) != null )
				{
					result.addCommand( c );

					continue;
				}
				else
				//No type and no command -> done.
					break;
			}
			if ( (f = parseFunction( t, result )) != null )
			{
				if ( !f.getName().equalsIgnoreCase( "main" ) || startScope == null ) //only upper level scope may define main.
					if ( !result.addFunction( f ) )
						throw new AdvancedScriptException( "Function " + f.getName() + " already defined " + getLineAndFile() );
			}
			else if ( (v = parseVariable( t, result )) != null )
			{
				if ( currentToken().equals( ";" ) )
					readToken(); //read ;
				else
					throw new AdvancedScriptException( "';' Expected " + getLineAndFile() );
			}
			else
				//Found a type but no function or variable to tie it to
				throw new AdvancedScriptException( "Script parse error " + getLineAndFile() );
		}

		return result;
	}

	private ScriptFunction parseFunction( ScriptType t, ScriptScope parentScope ) throws AdvancedScriptException
	{
		if ( !parseIdentifier( currentToken() ) )
			return null;

		String functionName = currentToken();

		if ( nextToken() == null || !nextToken().equals( "(" ) )
			return null;

		readToken(); //read Function name
		readToken(); //read (

		ScriptFunction result = new ScriptFunction( functionName, t );
		ScriptVariableList paramList = new ScriptVariableList();

		while ( !currentToken().equals( ")" ) )
		{
			ScriptType paramType = parseType();
			if (paramType == null )
				throw new AdvancedScriptException( " ')' Expected " + getLineAndFile() );

			ScriptVariable param = parseVariable( paramType, null );
			if ( param == null )
				throw new AdvancedScriptException( " Identifier expected " + getLineAndFile() );

			if ( !paramList.addElement( param ) )
				throw new AdvancedScriptException( "Variable " + param.getName() + " already defined " + getLineAndFile() );

			if ( !currentToken().equals( ")" ) )
			{
				if ( !currentToken().equals( "," ) )
					throw new AdvancedScriptException( " ')' Expected " + getLineAndFile() );

				readToken(); //read comma
			}

			ScriptVariableReference paramRef = new ScriptVariableReference( param );
			result.addVariableReference( paramRef );
		}

		readToken(); //read )

		if ( !currentToken().equals( "{" ) ) //Scope is a single call
		{
			result.setScope( new ScriptScope( parseCommand( t, parentScope, false, false ), parentScope ) );

			for ( ScriptVariable param = paramList.getFirstVariable(); param != null; param = paramList.getNextVariable() )
			{
				if ( !result.getScope().addVariable( param ) )
					throw new AdvancedScriptException( "Variable " + param.getName() + " already defined " + getLineAndFile() );
			}

			if ( !result.getScope().assertReturn() )
				throw new AdvancedScriptException( "Missing return value " + getLineAndFile() );
		}
		else
		{
			readToken(); //read {
			result.setScope( parseScope( null, t, paramList, parentScope, false ) );
			if ( !currentToken().equals( "}" ) )
				throw new AdvancedScriptException( " '}' Expected " + getLineAndFile() );
			readToken(); //read }
		}

		if ( !result.assertReturn() )
		{
			if ( !( t == null ) && !t.equals( TYPE_VOID ) && !t.equals( TYPE_BOOLEAN ) )
				throw new AdvancedScriptException( "Missing return value " + getLineAndFile() );
		}

		return result;
	}

	private ScriptVariable parseVariable( ScriptType t, ScriptScope scope ) throws AdvancedScriptException
	{
		if ( !parseIdentifier( currentToken() ) )
			return null;

		ScriptVariable result = new ScriptVariable( currentToken(), t );
		if ( scope != null && !scope.addVariable( result ) )
			throw new AdvancedScriptException( "Variable " + result.getName() + " already defined " + getLineAndFile() );

		readToken(); // If parsing of Identifier succeeded, go to next token.
		
		if ( scope != null && currentToken().equals( "=" ) )
		{
			readToken(); // Eat the equals sign

			ScriptVariableReference lhs = new ScriptVariableReference( result.getName(), scope );
			ScriptExpression rhs = parseExpression( scope );
			scope.addCommand( new ScriptAssignment( lhs, rhs ) );
		}

		return result;
	}

	private ScriptCommand parseCommand( ScriptType functionType, ScriptScope scope, boolean noElse, boolean whileLoop ) throws AdvancedScriptException
	{
		ScriptCommand result;

		if ( currentToken() == null )
			return null;

		if ( currentToken().equalsIgnoreCase( "break" ) )
		{
			if ( !whileLoop )
				throw new AdvancedScriptException( "break outside of loop " + getLineAndFile() );

			result = new ScriptCommand( COMMAND_BREAK );
			readToken(); //break
		}

		else if ( currentToken().equalsIgnoreCase( "continue" ) )
		{
			if ( !whileLoop )
				throw new AdvancedScriptException( "continue outside of loop " + getLineAndFile() );

			result = new ScriptCommand( COMMAND_CONTINUE );
			readToken(); //continue
		}

		else if ( currentToken().equalsIgnoreCase( "exit" ) )
		{
			result = new ScriptCommand( COMMAND_EXIT );
			readToken(); //exit
		}


		else if ( (result = parseReturn( functionType, scope )) != null )
			;
		else if ( (result = parseLoop( functionType, scope, noElse, whileLoop )) != null )
			//loop doesn't have a ; token
			return result;
		else if ( (result = parseCall( scope )) != null )
			;
		else if ( (result = parseAssignment( scope )) != null )
			;
		else
			return null;

		if ( currentToken() == null || !currentToken().equals( ";" ) )
			throw new AdvancedScriptException( "';' Expected " + getLineAndFile() );

		readToken(); // ;
		return result;
	}

	private ScriptType parseType()
	{
		int type;

		if ( currentToken() == null )
			return null;

		String typeString = currentToken();

		if ( typeString.equalsIgnoreCase( "void" ) )
			type = TYPE_VOID;
		else if ( typeString.equalsIgnoreCase( "boolean" ) )
			type = TYPE_BOOLEAN;
		else if ( typeString.equalsIgnoreCase( "int" ) )
			type = TYPE_INT;
		else if ( typeString.equalsIgnoreCase( "float" ) )
			type = TYPE_FLOAT;
		else if ( typeString.equalsIgnoreCase( "string" ) )
			type = TYPE_STRING;
		else if ( typeString.equalsIgnoreCase( "item" ) )
			type = TYPE_ITEM;
		else if ( typeString.equalsIgnoreCase( "zodiac" ) )
			type = TYPE_ZODIAC;
		else if ( typeString.equalsIgnoreCase( "location" ) )
			type = TYPE_LOCATION;
		else if ( typeString.equalsIgnoreCase( "class" ) )
			type = TYPE_CLASS;
		else if ( typeString.equalsIgnoreCase( "stat" ) )
			type = TYPE_STAT;
		else if ( typeString.equalsIgnoreCase( "skill" ) )
			type = TYPE_SKILL;
		else if ( typeString.equalsIgnoreCase( "effect" ) )
			type = TYPE_EFFECT;
		else if ( typeString.equalsIgnoreCase( "familiar" ) )
			type = TYPE_FAMILIAR;
		else if ( typeString.equalsIgnoreCase( "slot" ) )
			type = TYPE_SLOT;
		else if ( typeString.equalsIgnoreCase( "monster" ) )
			type = TYPE_MONSTER;
		else
			return null;

		readToken();
		return new ScriptType( type );
	}

	private boolean parseIdentifier( String identifier )
	{
		if ( !Character.isLetter( identifier.charAt( 0 ) ) && (identifier.charAt( 0 ) != '_' ) )
			return false;

		for ( int i = 1; i < identifier.length(); ++i )
			if ( !Character.isLetterOrDigit( identifier.charAt( i ) ) && (identifier.charAt( i ) != '_' ) )
				return false;

		return true;
	}

	private ScriptReturn parseReturn( ScriptType expectedType, ScriptScope parentScope ) throws AdvancedScriptException
	{
		ScriptExpression expression = null;

		if ( currentToken() == null || !currentToken().equalsIgnoreCase( "return" ) )
			return null;

		readToken(); //return

		if ( currentToken() != null && currentToken().equals( ";" ) )
		{
			if ( expectedType != null && expectedType.equals( TYPE_VOID ) )
				return new ScriptReturn( null, VOID_TYPE );

			throw new AdvancedScriptException( "Return needs value " + getLineAndFile() );
		}
		else
		{
			if ( (expression = parseExpression( parentScope )) == null )
				throw new AdvancedScriptException( "Expression expected " + getLineAndFile() );

			return new ScriptReturn( expression, expectedType );
		}
	}


	private ScriptLoop parseLoop( ScriptType functionType, ScriptScope parentScope, boolean noElse, boolean loop ) throws AdvancedScriptException
	{
		ScriptScope scope;
		ScriptExpression expression;
		ScriptLoop result = null;
		ScriptLoop currentLoop = null;
		ScriptCommand command = null;
		boolean repeat = false;
		boolean elseFound = false;
		boolean finalElse = false;

		if ( currentToken() == null )
			return null;

		if ( (currentToken().equalsIgnoreCase( "while" ) && ( repeat = true )) || currentToken().equalsIgnoreCase( "if" ) )
		{
			if ( nextToken() == null || !nextToken().equals( "(" ) )
				throw new AdvancedScriptException( "'(' Expected " + getLineAndFile() );

			readToken(); //if or while
			readToken(); //(

			expression = parseExpression( parentScope );
			if ( currentToken() == null || !currentToken().equals( ")" ) )
				throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

			readToken(); // )

			do
			{
				if ( currentToken() == null || !currentToken().equals( "{" ) ) //Scope is a single call
				{
					command = parseCommand( functionType, parentScope, !elseFound, (repeat || loop) );
					scope = new ScriptScope( command, parentScope );
					if ( result == null )
						result = new ScriptLoop( scope, expression, repeat );
				}
				else
				{
					readToken(); //read {
					scope = parseScope( null, functionType, null, parentScope, (repeat || loop ) );

					if ( currentToken() == null || !currentToken().equals( "}" ) )
						throw new AdvancedScriptException( " '}' Expected " + getLineAndFile() );

					readToken(); //read }
					if ( result == null )
						result = new ScriptLoop( scope, expression, repeat );
					else
						result.addElseLoop( new ScriptLoop( scope, expression, false ) );
				}
				if ( !repeat && !noElse && currentToken() != null && currentToken().equalsIgnoreCase( "else" ) )
				{

					if ( finalElse )
						throw new AdvancedScriptException( "Else without if " + getLineAndFile() );

					if ( nextToken() != null && nextToken().equalsIgnoreCase( "if" ) )
					{
						readToken(); //else
						readToken(); //if

						if ( currentToken() == null || !currentToken().equals( "(" ) )
							throw new AdvancedScriptException( "'(' Expected " + getLineAndFile() );

						readToken(); //(
						expression = parseExpression( parentScope );

						if ( currentToken() == null || !currentToken().equals( ")" ) )
							throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

						readToken(); // )
					}
					else //else without condition
					{
						readToken(); //else
						expression = TRUE_VALUE;
						finalElse = true;
					}

					elseFound = true;
					continue;
				}

				elseFound = false;
			}
			while ( elseFound );
		}
		else
			return null;

		return result;
	}

	private ScriptCall parseCall( ScriptScope scope ) throws AdvancedScriptException
	{
		if ( nextToken() == null || !nextToken().equals( "(" ) )
			return null;

		if ( !parseIdentifier( currentToken() ) )
			return null;

		String name = currentToken();

		readToken(); //name
		readToken(); //(

		ScriptExpressionList params = new ScriptExpressionList();
		while ( currentToken() != null && !currentToken().equals( ")" ) )
		{
			ScriptExpression val = parseExpression( scope );
			if ( val != null )
				params.addElement( val );

			if ( !currentToken().equals( "," ) )
			{
				if ( !currentToken().equals( ")" ) )
					throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );
			}
			else
			{
				readToken();
				if ( currentToken().equals( ")" ) )
					throw new AdvancedScriptException( "Parameter expected " + getLineAndFile() );
			}
		}

		if ( !currentToken().equals( ")" ) )
			throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

		readToken(); // )

		ScriptCall result = new ScriptCall( name, scope, params );
		return result;
	}

	private ScriptAssignment parseAssignment( ScriptScope scope ) throws AdvancedScriptException
	{
		if ( nextToken() == null || !nextToken().equals( "=" ) )
			return null;

		if ( !parseIdentifier( currentToken() ) )
			return null;

		String name = currentToken();

		readToken(); //name
		readToken(); //=

		ScriptVariableReference lhs = new ScriptVariableReference( name, scope );
		ScriptExpression rhs = parseExpression( scope );
		return new ScriptAssignment( lhs, rhs );
	}

	private ScriptExpression parseExpression( ScriptScope scope ) throws AdvancedScriptException
	{
		return parseExpression( scope, null );
	}

	private ScriptExpression parseExpression( ScriptScope scope, ScriptOperator previousOper ) throws AdvancedScriptException
	{
		if ( currentToken() == null )
			return null;

		ScriptExpression lhs = null;
		ScriptExpression rhs = null;
		ScriptOperator oper = null;

		if ( currentToken().equals( "!" )  || currentToken().equals( "-" ) )
		{
			String operator = currentToken();
			readToken(); // !
			if ( (lhs = parseValue( scope )) == null )
				throw new AdvancedScriptException( "Value expected " + getLineAndFile() );

			lhs = new ScriptExpression( lhs, null, new ScriptOperator( operator ) );
		}
		else
		{
			if ( (lhs = parseValue( scope )) == null )
				return null;
		}

		do
		{
			oper = parseOperator( currentToken() );

			if ( oper == null )
				return lhs;

			if ( previousOper != null && !oper.precedes( previousOper ) )
				return lhs;

			readToken(); //operator

			rhs = parseExpression( scope, oper );
			lhs = new ScriptExpression( lhs, rhs, oper );
		}
		while ( true );
	}

	private ScriptExpression parseValue( ScriptScope scope ) throws AdvancedScriptException
	{
		ScriptExpression result;

		if ( currentToken() == null )
			return null;

		if ( currentToken().equals( "(" ) )
		{
			readToken();// (
			result = parseExpression( scope );
			if ( currentToken() == null || !currentToken().equals( ")" ) )
				throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

			readToken();// )
			return result;
		}

		//Parse true and false first since they are reserved words.
		if ( currentToken().equalsIgnoreCase( "true" ) )
		{
			readToken();
			return new ScriptValue( true );
		}

		if ( currentToken().equalsIgnoreCase( "false" ) )
		{
			readToken();
			return new ScriptValue( false );
		}

		if ( (result = parseCall( scope )) != null )
			return result;

		if ( (result = parseVariableReference( scope )) != null )
			return result;

		if ( Character.isDigit( currentToken().charAt( 0 ) ) || currentToken().charAt( 0 ) == '-' )
		{
			for ( int i = 0; i < currentToken().length(); ++i )
			{
				if ( !Character.isDigit( currentToken().charAt( i ) ) )
				{
					if ( i == 0 && currentToken().charAt(i) == '-' )
						continue;

					if ( currentToken().charAt(i) == '.' )
						return parseDouble();

					throw new AdvancedScriptException( "Failed to parse numeric value " + getLineAndFile() );
				}
			}

			int resultInt = Integer.parseInt( currentToken() );
			readToken(); // integer

			return new ScriptValue( INT_TYPE, resultInt );
		}
		else if ( currentToken().equals( "\"" ) )
		{
			// Directly work with line - ignore any "tokens" you meet until the string is closed

			StringBuffer resultString = new StringBuffer();
			for ( int i = 1; ; ++i )
			{
				if ( i == line.length() )
				{
					throw new AdvancedScriptException( "No closing '\"' found " + getLineAndFile() );
				}
				else if ( line.charAt( i ) == '\\' )
				{
					resultString.append( line.charAt( ++i ) );
				}
				else if ( line.charAt( i ) == '"' )
				{
					line = line.substring( i + 1 ); //+ 1 to get rid of '"' token
					return new ScriptValue( STRING_TYPE, resultString.toString() );
				}
				else
				{
					resultString.append( line.charAt( i ) );
				}
			}

		}
		else if ( currentToken().equals( "$" ) )
		{
			ScriptType type;
			readToken();
			type = parseType();

			if ( type == null )
				throw new AdvancedScriptException( "Unknown type " + currentToken() + " " + getLineAndFile() );
			if ( !currentToken().equals( "[" ) )
				throw new AdvancedScriptException( "'[' Expected " + getLineAndFile() );

			StringBuffer resultString = new StringBuffer();

			for ( int i = 1; ; ++i )
			{
				if ( i == line.length() )
				{
					throw new AdvancedScriptException( "No closing ']' found " + getLineAndFile() );
				}
				else if ( line.charAt( i ) == '\\' )
				{
					resultString.append( line.charAt( ++i ) );
				}
				else if ( line.charAt( i ) == ']' )
				{
					line = line.substring( i + 1 ); //+1 to get rid of ']' token
					return new ScriptValue( type, resultString.toString());
				}
				else
				{
					resultString.append( line.charAt( i ) );
				}

			}
		}

		return null;
	}

	private ScriptValue parseDouble() throws AdvancedScriptException
	{
		try
		{
			double result;

			result = Double.parseDouble( currentToken() );
			readToken(); //double
			return new ScriptValue( TYPE_FLOAT, result );
		}
		catch( NumberFormatException e )
		{
			throw new AdvancedScriptException( "Failed to parse numeric value " + getLineAndFile() );
		}
	}

	private ScriptOperator parseOperator( String oper )
	{
		if ( oper == null )
			return null;
		if
		(
			oper.equals( "!" ) ||
			oper.equals( "*" ) || oper.equals( "/" ) || oper.equals( "%" ) ||
			oper.equals( "+" ) || oper.equals( "-" ) ||
			oper.equals( "<" ) || oper.equals( ">" ) || oper.equals( "<=" ) || oper.equals( ">=" ) ||
			oper.equals( "==" ) || oper.equals( "!=" ) ||
			oper.equals( "||" ) || oper.equals( "&&" )
		 )
		{
			return new ScriptOperator( oper );
		}
		else
			return null;
	}

	private ScriptVariableReference parseVariableReference( ScriptScope scope ) throws AdvancedScriptException
	{
		ScriptVariableReference result = null;

		if ( parseIdentifier( currentToken() ) )
		{
			String name = currentToken();
			result = new ScriptVariableReference( name, scope );

			readToken(); //name
			return result;
		}
		else
			return null;
	}

	private String parseImport() throws AdvancedScriptException
	{
		int i;

		if ( !currentToken().equalsIgnoreCase( "import" ) )
			return null;
		readToken(); //import

		if ( !currentToken().equals( "<" ) )
			throw new AdvancedScriptException( "'<' Expected " + getLineAndFile() );
		for ( i = 1; ; ++i )
		{
			if ( i == line.length() )
			{
				throw new AdvancedScriptException( "No closing '>' found " + getLineAndFile() );
			}
			if ( line.charAt(i ) == '>' )
			{
				String resultString = line.substring( 1, i );
				line = line.substring( i + 1 ); //+1 to get rid of '>' token
				return resultString;
			}
		}

	}

	private String currentToken()
	{
		fixLines();
		if ( line == null )
			return null;
		return line.substring( 0, tokenLength( line ) );
	}

	private String nextToken()
	{
		fixLines();

		if ( line == null )
			return null;

		if ( tokenLength( line ) >= line.length() )
		{
			if ( nextLine == null )
				return null;

			return nextLine.substring( 0, tokenLength( nextLine ) ).trim();
		}

		String result = line.substring( tokenLength( line ) ).trim();

		if ( result.equals( "" ) )
		{
			if ( nextLine == null )
				return null;

			return nextLine.substring( 0, tokenLength( nextLine ) );
		}

		return result.substring( 0, tokenLength( result ) );
	}

	private void readToken()
	{
		if ( line == null )
			return;

		fixLines();
		line = line.substring( tokenLength( line ) );
	}

	private int tokenLength( String s )
	{
		int result;
		if ( s == null )
			return 0;

		for ( result = 0; result < s.length(); result++ )
		{
			if ( result + 1 < s.length() && tokenString( s.substring( result, result + 2 ) ) )
				return result == 0 ? 2 : result;

			if ( result < s.length() && tokenString( s.substring( result, result + 1 ) ) )
				return result == 0 ? 1 : result;
		}

		return result; //== s.length()
	}

	private void fixLines()
	{
		if ( line == null )
			return;

		while ( line.equals( "" ) )
		{
			line = nextLine;
			lineNumber = commandStream.getLineNumber();
			nextLine = getNextLine();

			if ( line == null )
				return;
		}

		line = line.trim();

		if ( nextLine == null )
			return;

		while ( nextLine.equals( "" ) )
		{
			nextLine = getNextLine();
			if ( nextLine == null )
				return;
		}

		nextLine = nextLine.trim();
	}

	private boolean tokenString( String s )
	{
		if ( s.length() == 1 )
		{
			for ( int i = 0; i < tokenList.length; ++i )
				if ( s.charAt( 0 ) == tokenList[i] )
					return true;
			return false;
		}
		else
		{
			for ( int i = 0; i < multiCharTokenList.length; ++i )
				if ( s.equalsIgnoreCase( multiCharTokenList[i] ) )
					return true;
			return false;
		}
	}

	// **************** Debug printing *****************

	private void printScope( ScriptScope scope, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<SCOPE>" );

		indentLine( indent + 1 );
		KoLmafia.getDebugStream().println( "<VARIABLES>" );
		for ( ScriptVariable currentVar = scope.getFirstVariable(); currentVar != null; currentVar = scope.getNextVariable() )
			printVariable( currentVar, indent + 2 );

		indentLine( indent + 1 );
		KoLmafia.getDebugStream().println( "<FUNCTIONS>" );
		for ( ScriptFunction currentFunc = scope.getFirstFunction(); currentFunc != null; currentFunc = scope.getNextFunction() )
			printFunction( currentFunc, indent + 2 );

		indentLine( indent + 1 );
		KoLmafia.getDebugStream().println( "<COMMANDS>" );
		for ( ScriptCommand currentCommand = scope.getFirstCommand(); currentCommand != null; currentCommand = scope.getNextCommand() )
			printCommand( currentCommand, indent + 2 );
	}

	private void printVariable( ScriptVariable var, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<VAR " + var.getType() + " " + var.getName() + ">" );
	}

	private void printFunction( ScriptFunction func, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<FUNC " + func.getType() + " " + func.getName() + ">" );
		for ( ScriptVariableReference current = func.getFirstParam(); current != null; current = func.getNextParam() )
			printVariableReference( current, indent + 1 );
		printScope( func.getScope(), indent + 1 );
	}

	private void printCommand( ScriptCommand command, int indent )
	{
		if ( command instanceof ScriptReturn )
			printReturn( ( ScriptReturn ) command, indent );
		else if ( command instanceof ScriptLoop )
			printLoop( ( ScriptLoop ) command, indent );
		else if ( command instanceof ScriptCall )
			printCall( ( ScriptCall ) command, indent );
		else if ( command instanceof ScriptAssignment )
			printAssignment( ( ScriptAssignment ) command, indent );
		else
		{
			indentLine( indent );
			KoLmafia.getDebugStream().println( "<COMMAND " + command + ">" );
		}
	}

	private void printReturn( ScriptReturn ret, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<RETURN " + ret.getType() + ">" );
		if ( !ret.getType().equals( TYPE_VOID ) )
			printExpression( ret.getExpression(), indent + 1 );
	}

	private void printLoop( ScriptLoop loop, int indent )
	{
		indentLine( indent );
		if ( loop.repeats() )
			KoLmafia.getDebugStream().println( "<WHILE>" );
		else
			KoLmafia.getDebugStream().println( "<IF>" );
		printExpression( loop.getCondition(), indent + 1 );
		printScope( loop.getScope(), indent + 1 );
		for ( ScriptLoop currentElse = loop.getFirstElseLoop(); currentElse != null; currentElse = loop.getNextElseLoop() )
			printLoop( currentElse, indent + 1 );
	}

	private void printCall( ScriptCall call, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<CALL " + call.getTarget().getName() + ">" );
		for ( ScriptExpression current = call.getFirstParam(); current != null; current = call.getNextParam() )
			printExpression( current, indent + 1 );
	}

	private void printAssignment( ScriptAssignment assignment, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<ASSIGN " + assignment.getLeftHandSide().getName() + ">" );
		printExpression( assignment.getRightHandSide(), indent + 1 );

	}

	private void printExpression( ScriptExpression expression, int indent )
	{
		if ( expression instanceof ScriptValue )
			printValue( (ScriptValue) expression, indent );
		else
		{
			printOperator( expression.getOperator(), indent );
			printExpression( expression.getLeftHandSide(), indent + 1 );
			if ( expression.getRightHandSide() != null ) // ! operator
				printExpression( expression.getRightHandSide(), indent + 1 );
		}
	}

	public void printValue( ScriptValue value, int indent )
	{
		if ( value instanceof ScriptVariableReference )
			printVariableReference( (ScriptVariableReference) value, indent );
		else if ( value instanceof ScriptCall )
			printCall( (ScriptCall) value, indent );
		else
		{
			indentLine( indent );
			KoLmafia.getDebugStream().println( "<VALUE " + value.getType() + " [" + value + "]>" );
		}
	}

	public void printOperator( ScriptOperator oper, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<OPER " + oper + ">" );
	}

	public void printVariableReference( ScriptVariableReference varRef, int indent )
	{
		indentLine( indent );
		KoLmafia.getDebugStream().println( "<VARREF> " + varRef.getName() );
	}

	private static void indentLine( int indent )
	{
		for ( int i = 0; i < indent; ++i )
			KoLmafia.getDebugStream().print( "   " );
	}

	// **************** Execution *****************

	private void captureValue( ScriptValue value )
	{
		// We've just executed a command in a context that captures the
		// return value.

		if ( client.refusesContinue() || value == null )
		{
			// User aborted
			currentState = STATE_EXIT;
			return;
		}

		// Even if an error occurred, since we captured the result,
		// permit further execution.
		currentState = STATE_NORMAL;
		client.forceContinue();
	}

	private ScriptValue executeGlobalScope( ScriptScope globalScope ) throws AdvancedScriptException
	{
		ScriptFunction main;
		ScriptValue result = null;
		String resultString;

		currentState = STATE_NORMAL;
		resetTracing();

		main = globalScope.findFunction( "main", null );

		if ( main == null && globalScope.getFirstCommand() == null )
			throw new AdvancedScriptException( "No commands or main function found." );

		// First execute top-level commands;
		trace( "Executing top-level commands" );

		result = globalScope.execute();
		if ( currentState == STATE_EXIT )
			return result;

		// Now execute main function, if any
		if ( main != null )
		{
			trace( "Executing main function" );
			requestUserParams( main );
			result = main.execute();
		}

		return result;
	}

	private void requestUserParams( ScriptFunction targetFunction ) throws AdvancedScriptException
	{
		ScriptVariableReference	param;
		String resultString;

		for ( param = targetFunction.getFirstParam(); param != null; param = targetFunction.getNextParam() )
		{
			if ( param.getType().equals( TYPE_ZODIAC ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					ZODIACS,
					ZODIACS[0]
				 );
				param.setValue( new ScriptValue( TYPE_ZODIAC, resultString ) );
			}
			else if ( param.getType().equals( TYPE_CLASS ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					CLASSES,
					CLASSES[0]
				 );
				param.setValue( new ScriptValue( TYPE_CLASS, resultString ) );
			}
			else if ( param.getType().equals( TYPE_STAT ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					STATS,
					STATS[0]
				 );
				param.setValue( new ScriptValue( TYPE_STAT, resultString ) );
			}
			else if
			(
				param.getType().equals( TYPE_ITEM ) ||
				param.getType().equals( TYPE_LOCATION ) ||
				param.getType().equals( TYPE_STRING ) ||
				param.getType().equals( TYPE_SKILL ) ||
				param.getType().equals( TYPE_EFFECT ) ||
				param.getType().equals( TYPE_FAMILIAR ) ||
				param.getType().equals( TYPE_SLOT ) ||
				param.getType().equals( TYPE_MONSTER )
			)
			{
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType() + " " + param.getName() );
				param.setValue( new ScriptValue( param.getType(), resultString ) );
			}
			else if ( param.getType().equals( TYPE_INT ) )
			{
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType() + " " + param.getName() );
				try
				{
					param.setValue( new ScriptValue( TYPE_INT, Integer.parseInt( resultString ) ) );
				}
				catch( NumberFormatException e )
				{
					throw new AdvancedScriptException( "Incorrect value for integer." );
				}
			}
			else if ( param.getType().equals( TYPE_FLOAT ) )
			{
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType() + " " + param.getName() );
				try
				{
					param.setValue( new ScriptValue( TYPE_FLOAT, Double.parseDouble( resultString ) ) );
				}
				catch( NumberFormatException e )
				{
					throw new AdvancedScriptException( "Incorrect value for float." );
				}
			}
			else if ( param.getType().equals( TYPE_BOOLEAN ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					BOOLEANS,
					BOOLEANS[0]
				 );
				if ( resultString.equalsIgnoreCase( "true" ) )
					param.setValue( new ScriptValue( true ) );
				else if ( resultString.equalsIgnoreCase( "false" ) )
					param.setValue( new ScriptValue( false ) );
				else
					throw new RuntimeException( "Internal error: Illegal value for boolean" );
			}
			else if ( param.getType().equals( TYPE_VOID ) )
			{
				param.setValue( VOID_VALUE );
			}
			else
				throw new RuntimeException( "Internal error: Illegal type for main() parameter" );
		}
	}

	public String getLineAndFile()
	{
		return "at line " + lineNumber + " in file " + fileName;
	}


	public ScriptScope getExistingFunctionScope()
	{
		ScriptScope result;
		ScriptType [] params;

		result = new ScriptScope( null );

		// Include all the to_string and to_int methods first
		// so they're easier to add to later.
		
		params = new ScriptType[] { BOOLEAN_TYPE };
		result.addFunction( new ScriptExistingFunction( "boolean_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "int_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addFunction( new ScriptExistingFunction( "float_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "item_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { ZODIAC_TYPE };
		result.addFunction( new ScriptExistingFunction( "zodiac_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { LOCATION_TYPE };
		result.addFunction( new ScriptExistingFunction( "location_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { CLASS_TYPE };
		result.addFunction( new ScriptExistingFunction( "class_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { STAT_TYPE };
		result.addFunction( new ScriptExistingFunction( "stat_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addFunction( new ScriptExistingFunction( "skill_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { EFFECT_TYPE };
		result.addFunction( new ScriptExistingFunction( "effect_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addFunction( new ScriptExistingFunction( "familiar_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addFunction( new ScriptExistingFunction( "slot_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addFunction( new ScriptExistingFunction( "monster_to_string", STRING_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "int_to_item", ITEM_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "int_to_skill", SKILL_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "int_to_effect", EFFECT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "int_to_familiar", FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "int_to_slot", SLOT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "item_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addFunction( new ScriptExistingFunction( "skill_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { EFFECT_TYPE };
		result.addFunction( new ScriptExistingFunction( "effect_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addFunction( new ScriptExistingFunction( "familiar_to_int", INT_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addFunction( new ScriptExistingFunction( "slot_to_int", INT_TYPE, params ) );

		// Begin the functions which are documented in the KoLmafia
		// Advanced Script Handling manual.
		
		params = new ScriptType[] { INT_TYPE, LOCATION_TYPE };
		result.addFunction( new ScriptExistingFunction( "adventure", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "buy", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "create", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "use", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "eat", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "drink", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "item_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		params[0] = ITEM_TYPE;
		result.addFunction( new ScriptExistingFunction( "closet_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		params[0] = ITEM_TYPE;
		result.addFunction( new ScriptExistingFunction( "museum_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		params[0] = ITEM_TYPE;
		result.addFunction( new ScriptExistingFunction( "shop_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "storage_amount", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "refresh_stash", VOID_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "stash_amount", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "creatable_amount", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "put_closet", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "put_shop", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "put_stash", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "put_display", BOOLEAN_TYPE, params ) );
		
		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "take_closet", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "take_storage", BOOLEAN_TYPE, params ) );
		
		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "take_display", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "sell_item", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addFunction( new ScriptExistingFunction( "print", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_name", STRING_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_zodiac", ZODIAC_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_class", CLASS_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_level", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_hp", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_maxhp", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_mp", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_maxmp", INT_TYPE, params ) );

		params = new ScriptType[] { STAT_TYPE };
		result.addFunction( new ScriptExistingFunction( "my_basestat", INT_TYPE, params ) );

		params = new ScriptType[] { STAT_TYPE };
		result.addFunction( new ScriptExistingFunction( "my_buffedstat", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_meat", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_closetmeat", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_adventures", INT_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_inebriety", INT_TYPE, params ) );

		params = new ScriptType[] { SKILL_TYPE };
		result.addFunction( new ScriptExistingFunction( "have_skill", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { EFFECT_TYPE };
		result.addFunction( new ScriptExistingFunction( "have_effect", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, SKILL_TYPE };
		result.addFunction( new ScriptExistingFunction( "use_skill", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "add_item_condition", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "can_eat", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "can_drink", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "can_interact", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "trade_hermit", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "trade_bounty_hunter", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "trade_trapper", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "equip", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "equip_slot", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "unequip", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addFunction( new ScriptExistingFunction( "unequip_slot", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { SLOT_TYPE };
		result.addFunction( new ScriptExistingFunction( "current_equipment", ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_familiar", FAMILIAR_TYPE, params ) );

		params = new ScriptType[] { FAMILIAR_TYPE };
		result.addFunction( new ScriptExistingFunction( "equip_familiar", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addFunction( new ScriptExistingFunction( "monster_base_attack", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addFunction( new ScriptExistingFunction( "monster_base_defense", INT_TYPE, params ) );

		params = new ScriptType[] { MONSTER_TYPE };
		result.addFunction( new ScriptExistingFunction( "monster_base_HP", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "weapon_hands", INT_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "ranged_weapon", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "council", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "current_mind_control_level", INT_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "mind_control", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "have_chef", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "have_bartender", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { STRING_TYPE };
		result.addFunction( new ScriptExistingFunction( "cli_execute", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "bounty_hunter_wants", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "wait", VOID_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "entryway", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "hedgemaze", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "guardians", ITEM_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "chamber", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "nemesis", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "guild", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "gourd", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "tavern", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, STRING_TYPE };
		result.addFunction( new ScriptExistingFunction( "train_familiar", BOOLEAN_TYPE, params ) );

		params = new ScriptType[] { INT_TYPE, ITEM_TYPE };
		result.addFunction( new ScriptExistingFunction( "retrieve_item", BOOLEAN_TYPE, params ) );

		// Arithmetic utility functions
		params = new ScriptType[] { INT_TYPE };
		result.addFunction( new ScriptExistingFunction( "random", INT_TYPE, params ) );

		// Float-to-int conversion functions
		params = new ScriptType[] { FLOAT_TYPE };
		result.addFunction( new ScriptExistingFunction( "round", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addFunction( new ScriptExistingFunction( "truncate", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addFunction( new ScriptExistingFunction( "floor", INT_TYPE, params ) );

		params = new ScriptType[] { FLOAT_TYPE };
		result.addFunction( new ScriptExistingFunction( "ceil", INT_TYPE, params ) );

		return result;
	}

	private class ScriptScope
	{
		ScriptFunctionList	functions;
		ScriptVariableList	variables;
		ScriptCommandList	commands;
		ScriptScope		parentScope;

		public ScriptScope( ScriptScope parentScope )
		{
			this.functions = new ScriptFunctionList();
			this.variables = new ScriptVariableList();
			this.commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptCommand command, ScriptScope parentScope )
		{
			functions = new ScriptFunctionList();
			variables = new ScriptVariableList();
			commands = new ScriptCommandList();
			commands.addElement( command );
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptVariableList variables, ScriptScope parentScope )
		{
			functions = new ScriptFunctionList();
			if ( variables == null )
				variables = new ScriptVariableList();
			this.variables = variables;
			commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope getParentScope()
		{	return parentScope;
		}

		public boolean addFunction( ScriptFunction f )
		{	return functions.addElement( f );
		}

		public ScriptFunction getFirstFunction()
		{	return (ScriptFunction)functions.getFirstElement();
		}

		public ScriptFunction getNextFunction()
		{	return (ScriptFunction)functions.getNextElement();
		}

		public boolean addVariable( ScriptVariable v )
		{	return variables.addElement( v );
		}

		public ScriptVariable getFirstVariable()
		{	return (ScriptVariable)variables.getFirstElement();
		}

		public ScriptVariable getNextVariable()
		{	return (ScriptVariable)variables.getNextElement();
		}

		public ScriptVariable findVariable( String name )
		{	return variables.findVariable( name );
		}

		public void addCommand( ScriptCommand c )
		{	commands.addElement( c );
		}

		public ScriptCommand getFirstCommand()
		{	return (ScriptCommand)commands.getFirstElement();
		}

		public ScriptCommand getNextCommand()
		{	return (ScriptCommand)commands.getNextElement();
		}

		public boolean assertReturn()
		{
			int size = commands.size();
			if ( size == 0 )
				return false;
			if ( commands.get( size - 1 ) instanceof ScriptReturn )
				return true;
			return false;
		}

		public ScriptFunction findFunction( String name, ScriptExpressionList params ) throws AdvancedScriptException
		{
			ScriptFunction current = functions.findFunction( name );
			if ( current != null )
			{
				if ( params == null )
					return current;

				ScriptVariableReference currentParam = current.getFirstParam();
				ScriptExpression currentValue = (ScriptExpression) params.getFirstElement();
				int paramIndex = 1;

				while ( currentParam != null && currentValue != null )
				{
					if ( !currentParam.getType().equals( currentValue.getType() ) )
					{
						boolean validParameter = false;

						// float-check values
						validParameter |= currentParam.getType().equals( TYPE_FLOAT ) && currentValue.getType().equals( TYPE_INT );
						validParameter |= currentParam.getType().equals( TYPE_INT ) && currentValue.getType().equals( TYPE_FLOAT );

						// string operations are valid
						validParameter |= currentParam.getType().equals( TYPE_STRING );

						if ( !validParameter )
							throw new AdvancedScriptException( "Illegal parameter " + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType() + " " + getLineAndFile() );
					}

					++paramIndex;
					currentParam = current.getNextParam( );
					currentValue = (ScriptExpression)params.getNextElement();
				}

				if ( currentParam != null || currentValue != null )
					throw new AdvancedScriptException( "Illegal amount of parameters for function " + name + " " + getLineAndFile() );

				return current;
			}

			if ( parentScope != null )
				return parentScope.findFunction( name, params );

			return null;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptCommand current;
			ScriptValue result = null;

			traceIndent();
			for ( current = getFirstCommand(); current != null; current = getNextCommand() )
			{
				trace( "Command: " + current );

				result = current.execute();

				// Abort processing now if command failed
				if ( !client.permitsContinue() )
					currentState = STATE_EXIT;

				trace( "[" + executionStateString( currentState ) + "] <- " + result );

				switch ( currentState )
				{
					case STATE_RETURN:
					case STATE_BREAK:
					case STATE_CONTINUE:
					case STATE_EXIT:

						traceUnindent();
						return result;
				}
			}

			traceUnindent();
			return result;
		}
	}

	private class ScriptScopeList extends ScriptList
	{
		public boolean addElement( ScriptScope n )
		{	return super.addElement( n );
		}
	}

	private class ScriptSymbol implements Comparable
	{
		protected String name;

		public ScriptSymbol()
		{
		}

		public ScriptSymbol( String name )
		{	this.name = name;
		}

		public String getName()
		{	return name;
		}

		public int compareTo( Object o )
		{
			if ( !( o instanceof ScriptSymbol ) )
				throw new ClassCastException();
			if ( name == null)
				return 1;
			return name.compareToIgnoreCase( ((ScriptSymbol)o).name );
		}
	}

	private class ScriptSymbolTable extends SortedListModel
	{
		private int searchIndex = -1;

		public boolean addElement( ScriptSymbol n )
		{
			if ( findSymbol( n.getName() ) != null )
			     return false;

			add( n );
			return true;
		}

		ScriptSymbol findSymbol( String name )
		{
			for ( int i = 0; i < size(); ++i )
			{
				ScriptSymbol symbol = (ScriptSymbol)get( i );
				if ( name.equalsIgnoreCase( symbol.getName() ) )
					return symbol;
			}

			return null;
		}

		public ScriptSymbol getFirstElement()
		{
			searchIndex = -1;
			return getNextElement();
		}

		public ScriptSymbol getNextElement()
		{
			if ( ++searchIndex >= size() )
				return null;
			return (ScriptSymbol)get( searchIndex );
		}

		public ScriptSymbol getNextElement( ScriptSymbol n )
		{
			searchIndex = indexOf( n );
			if ( searchIndex == -1 )
				return null;
			return getNextElement();
		}
	}

	private class ScriptFunction extends ScriptSymbol
	{
		ScriptType type;
		ScriptVariableReferenceList variableReferences;
		ScriptScope scope;

		public ScriptFunction()
		{
		}

		public ScriptFunction( String name, ScriptType type )
		{
			super( name );
			this.type = type;
			this.variableReferences = new ScriptVariableReferenceList();
			this.scope = null;
		}

		public void addVariableReference( ScriptVariableReference v )
		{	variableReferences.addElement( v );
		}

		public void setScope( ScriptScope s )
		{	scope = s;
		}

		public ScriptScope getScope()
		{	return scope;
		}

		public ScriptVariableReference getFirstParam()
		{	return (ScriptVariableReference)variableReferences.getFirstElement();
		}

		public ScriptVariableReference getNextParam()
		{	return (ScriptVariableReference)variableReferences.getNextElement();
		}

		public ScriptType getType()
		{	return type;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptValue result = scope.execute();

			if ( currentState != STATE_EXIT )
				currentState = STATE_NORMAL;

			return result;
		}

		public boolean assertReturn()
		{	return scope.assertReturn();
		}
	}

	private class ScriptExistingFunction extends ScriptFunction
	{
		private Method method;
		private ScriptVariable [] variables;

		public ScriptExistingFunction( String name, ScriptType type, ScriptType [] params )
		{
			super( name.toLowerCase(), type );

			variables = new ScriptVariable[ params.length ];
			Class [] args = new Class[ params.length ];

			for ( int i = 0; i < params.length; ++i )
			{
				variables[i] = new ScriptVariable( params[i] );
				variableReferences.addElement( new ScriptVariableReference( variables[i] ) );
				args[i] = ScriptVariable.class;
			}

			try
			{
				this.method = getClass().getMethod( name, args );
			}
			catch ( Exception e )
			{
				// This should not happen; it denotes a coding
				// error that must be fixed before release. So,
				// simply print the bogus function to stdout
				System.out.println( "No method found for built-in function: " + name );
			}
		}

		public ScriptValue execute()
		{
			if ( method == null )
				throw new RuntimeException( "Internal error: no method for " + getName() );

			try
			{
				// Invoke the method
				return (ScriptValue)method.invoke(this, variables);
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				printStackTrace( e, "Exception during call to " + getName() );
				return null;
			}
		}

		// Here are all the methods for built-in ASH functions

		public ScriptValue boolean_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue int_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue float_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue item_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue zodiac_to_string( ScriptVariable val ) 
		{	return val.toStringValue();
		}

		public ScriptValue location_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue class_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue stat_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue skill_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue effect_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue familiar_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue slot_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue monster_to_string( ScriptVariable val )
		{	return val.toStringValue();
		}

		public ScriptValue item_to_int( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, val.intValue() );
		}

		public ScriptValue int_to_item( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_ITEM, val.intValue() );
		}

		public ScriptValue skill_to_int( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, val.intValue() );
		}

		public ScriptValue int_to_skill( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_SKILL, val.intValue() );
		}

		public ScriptValue effect_to_int( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, val.intValue() );
		}

		public ScriptValue int_to_effect( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_EFFECT, val.intValue() );
		}

		public ScriptValue familiar_to_int( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, val.intValue() );
		}

		public ScriptValue int_to_familiar( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_FAMILIAR, val.intValue() );
		}

		public ScriptValue slot_to_int( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, val.intValue() );
		}

		public ScriptValue int_to_slot( ScriptVariable val ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_SLOT, val.intValue() );
		}

		// Begin the functions which are documented in the KoLmafia
		// Advanced Script Handling manual.

		public ScriptValue adventure( ScriptVariable count, ScriptVariable loc )
		{
			DEFAULT_SHELL.executeLine( "adventure " + count.intValue() + " " + loc.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue buy( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "buy " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue create( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "create " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue use( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "use " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue eat( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "use " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue drink( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "use " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue item_amount( ScriptVariable arg ) throws AdvancedScriptException
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( TYPE_INT, item.getCount( KoLCharacter.getInventory() ) );
		}

		public ScriptValue closet_amount( ScriptVariable arg ) throws AdvancedScriptException
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( TYPE_INT, item.getCount( KoLCharacter.getCloset() ) );
		}

		public ScriptValue museum_amount( ScriptVariable arg ) throws AdvancedScriptException
		{
			if ( KoLCharacter.getCollection().isEmpty() )
				(new MuseumRequest( client )).run();
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( TYPE_INT, item.getCount( KoLCharacter.getCollection() ) );
		}

		public ScriptValue shop_amount( ScriptVariable arg ) throws AdvancedScriptException
		{
			(new StoreManageRequest( client )).run();

			LockableListModel list = StoreManager.getSoldItemList();
			StoreManager.SoldItem item = new StoreManager.SoldItem( arg.intValue(), 0, 0, 0, 0 );
			int index = list.indexOf( item );

			if ( index < 0 )
				return new ScriptValue( TYPE_INT, 0 );

			item = (StoreManager.SoldItem) list.get( index );
			return new ScriptValue( TYPE_INT, item.getQuantity() );
		}

		public ScriptValue storage_amount( ScriptVariable arg ) throws AdvancedScriptException
		{
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( TYPE_INT, item.getCount( KoLCharacter.getStorage() ) );
		}

		public ScriptValue refresh_stash()
		{
			(new ClanStashRequest( client )).run();
			return VOID_VALUE;
		}

		public ScriptValue stash_amount( ScriptVariable arg ) throws AdvancedScriptException
		{
			List stash = ClanManager.getStash();
			if ( stash.size() == 0 )
				(new ClanStashRequest( client )).run();
			AdventureResult item = new AdventureResult( arg.intValue(), 0 );
			return new ScriptValue( TYPE_INT, item.getCount( stash ) );
		}

		public ScriptValue creatable_amount( ScriptVariable arg ) throws AdvancedScriptException
		{
			ConcoctionsDatabase.refreshConcoctions();
			ItemCreationRequest item = ItemCreationRequest.getInstance( client, arg.intValue(), 0 );
			return new ScriptValue( TYPE_INT, item.getCount( ConcoctionsDatabase.getConcoctions() ) );
		}

		public ScriptValue put_closet( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "closet put " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue put_shop( ScriptVariable count, ScriptVariable price, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "mallsell " + item.toStringValue() + " " + count.intValue() + " " + price.intValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue put_stash( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "stash put " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}
			
		public ScriptValue put_display( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "display put " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue take_closet( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "closet take " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue take_storage( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "hagnk " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue take_display( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "display take " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}
			
		public ScriptValue sell_item( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "sell " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue print( ScriptVariable string )
		{
			DEFAULT_SHELL.updateDisplay( string.toStringValue().toString() );
			return VOID_VALUE;
		}

		public ScriptValue my_name()
		{	return new ScriptValue( KoLCharacter.getUsername() );
		}

		public ScriptValue my_zodiac() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_ZODIAC, KoLCharacter.getSign() );
		}

		public ScriptValue my_class() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_CLASS, KoLCharacter.getClassType() );
		}

		public ScriptValue my_level() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getLevel() );
		}

		public ScriptValue my_hp() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getCurrentHP() );
		}

		public ScriptValue my_maxhp() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getMaximumHP() );
		}

		public ScriptValue my_mp() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getCurrentMP() );
		}

		public ScriptValue my_maxmp() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getMaximumMP() );
		}

		public ScriptValue my_basestat( ScriptVariable arg ) throws AdvancedScriptException
		{
			int stat = arg.intValue();

			if ( STATS[ stat ].equalsIgnoreCase( "muscle" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getBaseMuscle() );
			if ( STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getBaseMysticality() );
			if ( STATS[ stat ].equalsIgnoreCase( "moxie" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getBaseMoxie() );

			throw new RuntimeException( "Internal error: unknown stat" );
		}

		public ScriptValue my_buffedstat( ScriptVariable arg ) throws AdvancedScriptException
		{
			int stat = arg.intValue();

			if ( STATS[ stat ].equalsIgnoreCase( "muscle" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getAdjustedMuscle() );
			if ( STATS[ stat ].equalsIgnoreCase( "mysticality" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getAdjustedMysticality() );
			if ( STATS[ stat ].equalsIgnoreCase( "moxie" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getAdjustedMoxie() );

			throw new RuntimeException( "Internal error: unknown stat" );
		}

		public ScriptValue my_meat() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getAvailableMeat() );
		}

		public ScriptValue my_closetmeat() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getClosetMeat() );
		}

		public ScriptValue my_adventures() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getAdventuresLeft() );
		}

		public ScriptValue my_inebriety() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, KoLCharacter.getInebriety() );
		}

		public ScriptValue my_familiar() throws AdvancedScriptException
		{	return new ScriptValue( TYPE_FAMILIAR, KoLCharacter.getFamiliar().getID() == -1 ? "none" : KoLCharacter.getFamiliar().getRace() );
		}

		public ScriptValue have_effect( ScriptVariable arg ) throws AdvancedScriptException
		{
			List potentialEffects = StatusEffectDatabase.getMatchingNames( arg.toStringValue().toString() );
			AdventureResult effect = potentialEffects.isEmpty() ? null : new AdventureResult( (String) potentialEffects.get(0), 0, true );
			return new ScriptValue( TYPE_INT, effect == null ? 0 : effect.getCount( KoLCharacter.getEffects() ) );
		}

		public ScriptValue have_skill( ScriptVariable arg )
		{	return new ScriptValue( KoLCharacter.hasSkill( arg.intValue() ) );
		}

		public ScriptValue use_skill( ScriptVariable count, ScriptVariable skill )
		{
			DEFAULT_SHELL.executeLine( "cast " + count.intValue() + " " + skill.toStringValue() );
			return new ScriptValue( UseSkillRequest.lastUpdate.equals( "" ) );
		}

		public ScriptValue add_item_condition( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "conditions add " + count.intValue() + " " + item.toStringValue() );
			return VOID_VALUE;
		}

		public ScriptValue can_eat()
		{	return new ScriptValue( KoLCharacter.canEat() );
		}

		public ScriptValue can_drink()
		{	return new ScriptValue( KoLCharacter.canDrink() );
		}

		public ScriptValue can_interact()
		{	return new ScriptValue( KoLCharacter.canInteract() );
		}

		public ScriptValue trade_hermit( ScriptVariable count, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "hermit " + count.intValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue bounty_hunter_wants( ScriptVariable item )
		{
			String itemName = item.toStringValue().toString();

			if ( client.hunterItems.isEmpty() )
				(new BountyHunterRequest( client )).run();

			for ( int i = 0; i < client.hunterItems.size(); ++i )
				if ( ((String)client.hunterItems.get(i)).equalsIgnoreCase( itemName ) )
					return TRUE_VALUE;

			return FALSE_VALUE;
		}

		public ScriptValue trade_bounty_hunter( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "hunter " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue trade_trapper( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "trapper " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue equip( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "equip " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue equip_slot( ScriptVariable slot, ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "equip " + slot.toStringValue() + " " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue unequip( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "unequip " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue unequip_slot( ScriptVariable item )
		{
			DEFAULT_SHELL.executeLine( "unequip " + item.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue current_equipment( ScriptVariable slot ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_ITEM, KoLCharacter.getCurrentEquipmentName( slot.intValue() ) );
		}

		public ScriptValue monster_base_attack( ScriptVariable arg ) throws AdvancedScriptException
		{
			MonsterDatabase.Monster monster = (MonsterDatabase.Monster)(arg.rawValue());
			return new ScriptValue( TYPE_INT, monster.getAttack() );
		}

		public ScriptValue monster_base_defense( ScriptVariable arg ) throws AdvancedScriptException
		{
			MonsterDatabase.Monster monster = (MonsterDatabase.Monster)(arg.rawValue());

			return new ScriptValue( TYPE_INT, monster.getDefense() );
		}

		public ScriptValue monster_base_HP( ScriptVariable arg ) throws AdvancedScriptException
		{
			MonsterDatabase.Monster monster = (MonsterDatabase.Monster)(arg.rawValue());

			return new ScriptValue( TYPE_INT, monster.getHP() );
		}

		public ScriptValue weapon_hands( ScriptVariable item ) throws AdvancedScriptException
		{	return new ScriptValue( TYPE_INT, EquipmentDatabase.getHands( item.intValue() ) );
		}

		public ScriptValue ranged_weapon( ScriptVariable item )
		{	return new ScriptValue( EquipmentDatabase.isRanged( variables[0].intValue() ) );
		}

		public ScriptValue equip_familiar( ScriptVariable familiar )
		{
			DEFAULT_SHELL.executeLine( "familiar " + familiar.toStringValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue council()
		{
			DEFAULT_SHELL.executeLine( "council" );
			return VOID_VALUE;
		}

		public ScriptValue current_mind_control_level() throws AdvancedScriptException
		{	return new ScriptValue( INT_TYPE, KoLCharacter.getMindControlLevel() );
		}

		public ScriptValue mind_control( ScriptVariable level )
		{
			DEFAULT_SHELL.executeLine( "mind-control " + level.intValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue have_chef()
		{	return new ScriptValue( KoLCharacter.hasChef() );
		}

		public ScriptValue have_bartender()
		{	return new ScriptValue( KoLCharacter.hasBartender() );
		}

		public ScriptValue cli_execute( ScriptVariable string )
		{
			DEFAULT_SHELL.executeLine( string.toStringValue().toString() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue wait( ScriptVariable delay )
		{
			DEFAULT_SHELL.executeLine( "wait " + delay.intValue() );
			return VOID_VALUE;
		}

		public ScriptValue entryway()
		{
			DEFAULT_SHELL.executeLine( "entryway" );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue hedgemaze()
		{
			DEFAULT_SHELL.executeLine( "hedgemaze" );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue guardians() throws AdvancedScriptException
		{
			int itemID = SorceressLair.fightTowerGuardians();
			return new ScriptValue( TYPE_ITEM, itemID == -1 ? "none" : TradeableItemDatabase.getItemName( itemID ) );
		}

		public ScriptValue chamber()
		{
			DEFAULT_SHELL.executeLine( "chamber" );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue nemesis()
		{
			DEFAULT_SHELL.executeLine( "nemesis" );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue guild()
		{
			DEFAULT_SHELL.executeLine( "guild" );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue gourd()
		{
			DEFAULT_SHELL.executeLine( "gourd" );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue tavern() throws AdvancedScriptException
		{
			int result = client.locateTavernFaucet();
			return new ScriptValue( TYPE_INT, client.permitsContinue() ? result : -1 );
		}

		public ScriptValue train_familiar( ScriptVariable weight, ScriptVariable familiar )
		{
			DEFAULT_SHELL.executeLine( "train " + familiar.toStringValue() + " " + weight.intValue() );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue retrieve_item( ScriptVariable count, ScriptVariable item )
		{
			AdventureDatabase.retrieveItem( new AdventureResult( item.intValue(), count.intValue() ) );
			return new ScriptValue( client.permitsContinue() );
		}

		public ScriptValue random( ScriptVariable arg ) throws AdvancedScriptException
		{
			int range = arg.intValue();
			if ( range < 2 )
				throw new RuntimeException( "Random range must be at least 2" );
			return new ScriptValue( INT_TYPE, RNG.nextInt( range ) );
		}

		public ScriptValue round( ScriptVariable arg ) throws AdvancedScriptException
		{	return new ScriptValue( INT_TYPE, (int)Math.round( arg.floatValue() ) );
		}

		public ScriptValue truncate( ScriptVariable arg ) throws AdvancedScriptException
		{	return new ScriptValue( INT_TYPE, (int)variables[0].floatValue() );
		}

		public ScriptValue floor( ScriptVariable arg ) throws AdvancedScriptException
		{	return new ScriptValue( INT_TYPE, (int)Math.floor( variables[0].floatValue() ) );
		}

		public ScriptValue ceil( ScriptVariable arg ) throws AdvancedScriptException
		{	return new ScriptValue( INT_TYPE, (int)Math.ceil( variables[0].floatValue() ) );
		}
	}

	private class ScriptFunctionList extends ScriptSymbolTable
	{
		public boolean addElement( ScriptFunction n )
		{	return super.addElement( n );
		}

		public ScriptFunction findFunction( String name )
		{	return (ScriptFunction)super.findSymbol( name );
		}
	}

	private class ScriptVariable extends ScriptSymbol
	{
		ScriptValue	content;

		public ScriptVariable( ScriptType type )
		{
			super( null );
			content = new ScriptValue( type );
		}

		public ScriptVariable( String name, ScriptType type )
		{
			super( name );
			content = new ScriptValue( type );
		}

		public ScriptType getType()
		{	return content.getType();
		}

		public ScriptValue getValue()
		{	return content;
		}

		public Object rawValue()
		{	return content.rawValue();
		}

		public int intValue()
		{	return content.intValue();
		}

		public ScriptValue toStringValue()
		{	return content.toStringValue();
		}

		public KoLAdventure getLocation()
		{	return content.getLocation();
		}

		public double floatValue()
		{	return content.floatValue();
		}

		public void setValue( ScriptValue targetValue ) throws AdvancedScriptException
		{
			if ( getType().equals( targetValue.getType() ) )
			{
				content = targetValue;
			}
			else if ( getType().equals( TYPE_STRING ) )
			{
				content = targetValue.toStringValue();
			}
			else if ( getType().equals( TYPE_INT ) && targetValue.getType().equals( TYPE_FLOAT ) )
			{
				content = targetValue.toIntValue();
			}
			else if ( getType().equals( TYPE_FLOAT ) && targetValue.getType().equals( TYPE_INT ) )
			{
				content = targetValue.toFloatValue();
			}
			else
			{
				throw new RuntimeException( "Internal error: Cannot assign " + targetValue.getType() + " to " + getType() );
			}
		}
	}

	private class ScriptVariableList extends ScriptSymbolTable
	{
		public boolean addElement( ScriptVariable n )
		{	return super.addElement( n );
		}

		public ScriptVariable findVariable( String name )
		{	return (ScriptVariable)super.findSymbol( name );
		}

		public ScriptVariable getFirstVariable()
		{	return ( ScriptVariable)getFirstElement();
		}

		public ScriptVariable getNextVariable()
		{	return ( ScriptVariable)getNextElement();
		}
	}

	private class ScriptVariableReference extends ScriptValue
	{
		ScriptVariable target;

		public ScriptVariableReference( ScriptVariable target )
		{	this.target = target;
		}

		public ScriptVariableReference( String varName, ScriptScope scope ) throws AdvancedScriptException
		{	target = findVariable( varName, scope );
		}

		private ScriptVariable findVariable( String name, ScriptScope scope ) throws AdvancedScriptException
		{
			while ( scope != null )
			{
				ScriptVariable current = scope.findVariable( name );
				if ( current != null )
					return current;
				scope = scope.getParentScope();
			}

			throw new AdvancedScriptException( "Undefined variable " + name + " " + getLineAndFile() );
		}

		public ScriptType getType()
		{	return target.getType();
		}

		public String getName()
		{	return target.getName();
		}

		public int compareTo( Object o )
		{	return target.getName().compareTo( ((ScriptVariableReference)o).target.getName() );
		}

		public ScriptValue execute()
		{	return target.getValue();
		}

		public void setValue( ScriptValue targetValue ) throws AdvancedScriptException
		{	target.setValue( targetValue );
		}

		public String toString()
		{	return target.getName();
		}
	}

	private class ScriptVariableReferenceList extends ScriptList
	{
		public boolean addElement( ScriptVariableReference n )
		{	return super.addElement( n );
		}
	}

	private class ScriptCommand
	{
		int command;

		public ScriptCommand()
		{
		}

		public ScriptCommand( String command ) throws AdvancedScriptException
		{
			if ( command.equalsIgnoreCase( "break" ) )
				this.command = COMMAND_BREAK;
			else if ( command.equalsIgnoreCase( "continue" ) )
				this.command = COMMAND_CONTINUE;
			else if ( command.equalsIgnoreCase( "exit" ) )
				this.command = COMMAND_EXIT;
			else
				throw new AdvancedScriptException( command + " is not a command " + getLineAndFile() );
		}

		public ScriptCommand( int command )
		{	this.command = command;
		}

		public String toString()
		{
			switch ( this.command)
			{
			case COMMAND_BREAK:
				return "break";
			case COMMAND_CONTINUE:
				return "continue";
			case COMMAND_EXIT:
				return "exit";
			}
			return "<unknown command>";
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			traceIndent();
			trace( toString() );
			traceUnindent();

			switch ( this.command)
			{
			case COMMAND_BREAK:
				currentState = STATE_BREAK;
				return VOID_VALUE;
			case COMMAND_CONTINUE:
				currentState = STATE_CONTINUE;
				return VOID_VALUE;
			case COMMAND_EXIT:
				currentState = STATE_EXIT;
				return null;
			}

			throw new RuntimeException( "Internal error: unknown ScriptCommand type" );

		}
	}

	private class ScriptCommandList extends ScriptList
	{
		public boolean addElement( ScriptCommand n )
		{	return super.addElement( n );
		}
	}

	private class ScriptReturn extends ScriptCommand
	{
		private ScriptExpression returnValue;
		private ScriptType expectedType;

		public ScriptReturn( ScriptExpression returnValue, ScriptType expectedType ) throws AdvancedScriptException
		{
			this.returnValue = returnValue;

			if ( expectedType != null && returnValue != null && !returnValue.getType().equals( expectedType ) )
			{
				boolean validReturn = false;

				// float-check values
				validReturn |= expectedType.equals( TYPE_INT ) && returnValue.getType().equals( TYPE_FLOAT );
				validReturn |= expectedType.equals( TYPE_FLOAT ) && returnValue.getType().equals( TYPE_INT );

				// string operations are valid
				validReturn |= expectedType.equals( TYPE_STRING );

				if ( !validReturn )
					throw new AdvancedScriptException( "Cannot apply " + returnValue.getType() + " to " + expectedType + " " + getLineAndFile() );
			}

			this.expectedType = expectedType;
		}

		public ScriptType getType()
		{
			if ( expectedType != null )
				return expectedType;

			if ( returnValue == null )
				return VOID_TYPE;

			return returnValue.getType();
		}

		public ScriptExpression getExpression()
		{	return returnValue;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( !client.permitsContinue() )
				currentState = STATE_EXIT;

			if ( currentState == STATE_EXIT )
				return null;

			currentState = STATE_RETURN;

			if ( returnValue == null )
				return null;

			traceIndent();
			trace( "Eval: " + returnValue );

			ScriptValue result = returnValue.execute();
			captureValue( result );

			trace( "Returning: " + result );
                        traceUnindent();

			if ( currentState != STATE_EXIT )
				currentState = STATE_RETURN;

			if ( result == null )
				return null;

			if ( expectedType.equals( TYPE_STRING ) )
				return result.toStringValue();

			if ( expectedType.equals( TYPE_FLOAT ) )
				return result.toFloatValue();

			if ( expectedType.equals( TYPE_INT ) )
				return result.toIntValue();

			return result;
		}

		public String toString()
		{	return "return " + returnValue;
		}
	}


	private class ScriptLoop extends ScriptCommand
	{
		private boolean repeat;
		private ScriptExpression condition;
		private ScriptScope scope;
		private ScriptLoopList elseLoops;

		public ScriptLoop( ScriptScope scope, ScriptExpression condition, boolean repeat ) throws AdvancedScriptException
		{
			this.scope = scope;
			this.condition = condition;
			if ( !condition.getType().equals( TYPE_BOOLEAN ) )
				throw new AdvancedScriptException( "Cannot apply " + condition.getType() + " to boolean " + getLineAndFile() );

			this.repeat = repeat;
			elseLoops = new ScriptLoopList();
		}

		public boolean repeats()
		{	return repeat;
		}

		public ScriptExpression getCondition()
		{	return condition;
		}

		public ScriptScope getScope()
		{	return scope;
		}

		public ScriptLoop getFirstElseLoop()
		{	return ( ScriptLoop )elseLoops.getFirstElement();
		}

		public ScriptLoop getNextElseLoop()
		{	return ( ScriptLoop )elseLoops.getNextElement();
		}

		public void addElseLoop( ScriptLoop elseLoop ) throws AdvancedScriptException
		{
			if ( repeat == true )
				throw new AdvancedScriptException( "Else without if " + getLineAndFile() );
			elseLoops.addElement( elseLoop );
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( !client.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( this.toString() );

			boolean executed = false;

			while ( !executed || repeat  )
			{
				trace( "Test: " + condition );

				ScriptValue conditionResult = condition.execute();
				captureValue( conditionResult );

				trace( "[" + executionStateString( currentState ) + "] <- " + conditionResult );

				if (  conditionResult == null )
				{
					traceUnindent();
					return null;
				}

				if ( conditionResult.intValue() != 1 )
					break;

				// The condition was satisfied at least once
				executed = true;

				ScriptValue result = scope.execute();

				if ( !client.permitsContinue() )
					currentState = STATE_EXIT;

				if ( currentState == STATE_BREAK )
				{
					if ( repeat )
						currentState = STATE_NORMAL;

					traceUnindent();
					return VOID_VALUE;
				}

				if ( currentState == STATE_CONTINUE )
				{
					if ( !repeat )
					{
						traceUnindent();
						return VOID_VALUE;
					}

					currentState = STATE_NORMAL;
				}

				if ( currentState == STATE_RETURN )
				{
					traceUnindent();
					return result;
				}

				if ( currentState == STATE_EXIT )
				{
					traceUnindent();
					return null;
				}
			}

			if ( executed )
			{
				traceUnindent();
				return VOID_VALUE;
			}

			// Conditional failed. Move to else clauses
			for ( ScriptLoop elseLoop = elseLoops.getFirstScriptLoop(); elseLoop != null; elseLoop = elseLoops.getNextScriptLoop() )
			{
				ScriptValue result = elseLoop.execute();
				if ( currentState != STATE_NORMAL )
				{
					traceUnindent();
					return result;
				}
			}

			traceUnindent();
			return VOID_VALUE;
		}

		public String toString()
		{	return repeat ? "while" : "if";
		}
	}


	private class ScriptLoopList extends ScriptList
	{
		public boolean addElement( ScriptLoop n )
		{	return super.addElement( n );
		}

		public ScriptLoop getFirstScriptLoop()
		{	return (ScriptLoop)getFirstElement();
		}

		public ScriptLoop getNextScriptLoop()
		{	return (ScriptLoop)getNextElement();
		}
	}

	private class ScriptCall extends ScriptValue
	{
		private ScriptFunction target;
		private ScriptExpressionList params;

		public ScriptCall( String functionName, ScriptScope scope, ScriptExpressionList params ) throws AdvancedScriptException
		{
			target = findFunction( functionName, scope, params );
			if ( target == null )
				throw new AdvancedScriptException( "Undefined reference " + functionName + " " + getLineAndFile() );
			this.params = params;
		}

		private ScriptFunction findFunction( String name, ScriptScope scope, ScriptExpressionList params ) throws AdvancedScriptException
		{
			if ( scope == null )
				return null;
			return scope.findFunction( name, params );
		}

		public ScriptFunction getTarget()
		{	return target;
		}

		public ScriptExpression getFirstParam()
		{	return ( ScriptExpression )params.getFirstElement();
		}

		public ScriptExpression getNextParam()
		{	return ( ScriptExpression )params.getNextElement();
		}

		public ScriptType getType()
		{	return target.getType();
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( !client.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			ScriptVariableReference paramVarRef = target.getFirstParam();
			ScriptExpression paramValue = params.getFirstExpression();

			traceIndent();
			int paramCount = 0;
			while ( paramVarRef != null )
			{
				++paramCount;
				if ( paramValue == null )
					throw new RuntimeException( "Internal error: illegal arguments" );

				trace( "Param #" + paramCount + ": " + paramValue );

				ScriptValue value = paramValue.execute();
				captureValue( value );

				trace( "[" + executionStateString( currentState ) + "] <- " + value );

				if ( currentState == STATE_EXIT )
				{
					traceUnindent();
					return null;
				}

				if ( paramVarRef.getType().equals( TYPE_INT ) && paramValue.getType().equals( TYPE_FLOAT ) )
					paramVarRef.setValue( value.toIntValue() );
				else if ( paramVarRef.getType().equals( TYPE_FLOAT ) && paramValue.getType().equals( TYPE_INT ) )
					paramVarRef.setValue( value.toFloatValue() );
				else if ( paramVarRef.getType().equals( TYPE_STRING ) )
					paramVarRef.setValue( value.toStringValue() );
				else
					paramVarRef.setValue( value );

				paramVarRef = target.getNextParam();
				paramValue = params.getNextExpression();
			}

			if ( paramValue != null )
				throw new RuntimeException( "Internal error: illegal arguments" );

			trace( "Entering function " + target.getName() );
			ScriptValue result = target.execute();

			trace( "Function " + target.getName() + " returned: " + result );
			traceUnindent();

			return result;
		}

		public String toString()
		{	return target.getName() + "()";
		}
	}

	private class ScriptAssignment extends ScriptCommand
	{
		private ScriptVariableReference lhs;
		private ScriptExpression rhs;

		public ScriptAssignment( ScriptVariableReference lhs, ScriptExpression rhs ) throws AdvancedScriptException
		{
			this.lhs = lhs;
			this.rhs = rhs;
			
			if ( !lhs.getType().equals( rhs.getType() ) )
			{
				boolean validOperation = false;

				// float-check values
				validOperation |= lhs.getType().equals( TYPE_INT ) && rhs.getType().equals( TYPE_FLOAT );
				validOperation |= lhs.getType().equals( TYPE_FLOAT ) && rhs.getType().equals( TYPE_INT );

				// string operations are valid
				validOperation |= lhs.getType().equals( TYPE_STRING );

				if ( !validOperation )
					throw new AdvancedScriptException( "Cannot apply " + rhs.getType() + " to " + lhs + " " + getLineAndFile() );
			}
		}

		public ScriptVariableReference getLeftHandSide()
		{
			return lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return rhs;
		}

		public ScriptType getType()
		{
			return lhs.getType();
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( !client.permitsContinue() )
			{
				currentState = STATE_EXIT;
				return null;
			}

			traceIndent();
			trace( "Eval: " + rhs );

			ScriptValue value = rhs.execute();
			captureValue( value );

			trace( "Set: " + value );
			traceUnindent();

			if ( currentState == STATE_EXIT )
				return null;

			if ( lhs.getType().equals( TYPE_INT ) && rhs.getType().equals( TYPE_FLOAT ) )
				lhs.setValue( value.toIntValue() );
			else if ( lhs.getType().equals( TYPE_FLOAT ) && rhs.getType().equals( TYPE_INT ) )
				lhs.setValue( value.toFloatValue() );
			else if ( lhs.getType().equals( TYPE_STRING ) )
				lhs.setValue( value.toStringValue() );
			else
				lhs.setValue( value );

			return VOID_VALUE;
		}

		public String toString()
		{	return lhs.getName() + " = " + rhs;
		}
	}

	private static class ScriptType
	{
		int type;

		public ScriptType( int type )
		{
			this.type = type;
		}

		public boolean equals( ScriptType type )
		{
			if ( this.type == type.type )
				return true;
			return false;
		}

		public boolean equals( int type )
		{
			if ( this.type == type )
				return true;
			return false;
		}

		public String toString()
		{
			if ( type == TYPE_VOID )
				return "void";
			if ( type == TYPE_BOOLEAN )
				return "boolean";
			if ( type == TYPE_INT )
				return "int";
			if ( type == TYPE_FLOAT )
				return "float";
			if ( type == TYPE_STRING )
				return "string";
			if ( type == TYPE_ITEM )
				return "item";
			if ( type == TYPE_ZODIAC )
				return "zodiac";
			if ( type == TYPE_LOCATION )
				return "location";
			if ( type == TYPE_CLASS )
				return "class";
			if ( type == TYPE_STAT )
				return "stat";
			if ( type == TYPE_SKILL )
				return "skill";
			if ( type == TYPE_EFFECT )
				return "effect";
			if ( type == TYPE_FAMILIAR )
				return "familiar";
			if ( type == TYPE_SLOT )
				return "slot";
			if ( type == TYPE_MONSTER )
				return "monster";
			return "unknown type";
		}
	}

	private class ScriptValue extends ScriptExpression
	{
		ScriptType type;

		int contentInt = 0;
		double contentFloat = 0.0;
		String contentString = null;
		Object content = null;

		public ScriptValue()
		{	this.type = VOID_TYPE;
		}

		public ScriptValue( boolean value )
		{
			this.type = BOOLEAN_TYPE;
			this.contentInt = value ? 1 : 0;
		}

		public ScriptValue( String value )
		{
			this.type = STRING_TYPE;
			this.contentString = value;
		}

		public ScriptValue( double content )
		{
			this.type = FLOAT_TYPE;
			this.contentFloat = content;
		}

		public ScriptValue( int type ) throws AdvancedScriptException
		{
			this.type = new ScriptType( type );
			fillContent();
		}

		public ScriptValue( ScriptType type )
		{
			this.type = type;
		}

		public ScriptValue( int type, int contentInt ) throws AdvancedScriptException
		{	this( new ScriptType( type ), contentInt );
		}

		public ScriptValue( ScriptType type, int contentInt ) throws AdvancedScriptException
		{
			this.type = type;
			this.contentInt = contentInt;
			fillContent();
		}


		public ScriptValue( int type, String contentString ) throws AdvancedScriptException
		{	this( new ScriptType( type ), contentString );
		}

		public ScriptValue( ScriptType type, String contentString ) throws AdvancedScriptException
		{
			this.type = type;
			this.contentString = contentString;
			fillContent();
		}

		public ScriptValue( int type, double content ) throws AdvancedScriptException
		{
			if ( type != TYPE_FLOAT )
				throw new AdvancedScriptException( "Internal error: cannot assign float value to non-float" );
			this.type = FLOAT_TYPE;
			this.contentFloat = content;
		}

		public ScriptValue( ScriptValue original )
		{
			this.type = original.type;
			this.contentInt = original.contentInt;
			this.contentString = original.contentString;
			this.content = original.content;
		}

		public ScriptValue toFloatValue() throws AdvancedScriptException
		{
			if ( type.equals( TYPE_FLOAT ) )
				return this;
			else
				return new ScriptValue( TYPE_FLOAT, (double) contentInt );
		}

		public ScriptValue toIntValue() throws AdvancedScriptException
		{
			if ( type.equals( TYPE_INT ) )
				return this;
			else
				return new ScriptValue( TYPE_INT, (int) contentFloat );
		}

		public ScriptType getType()
		{
			return type;
		}

		public String toString()
		{
			if ( type.equals( TYPE_VOID ) )
				return "<void>";

			if ( contentString != null )
				return contentString;
			
			if ( type.equals( TYPE_BOOLEAN ) )
				return String.valueOf( contentInt != 0 );

			if ( type.equals( TYPE_FLOAT ) )
				return String.valueOf( contentFloat );

			return String.valueOf( contentInt );
		}

		public ScriptValue toStringValue()
		{
			return new ScriptValue( toString() );
		}

		public Object rawValue()
		{
			return content;
		}

		public int intValue()
		{
			return contentInt;
		}

		public double floatValue()
		{
			return contentFloat;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			return this;
		}

		public void fillContent() throws AdvancedScriptException
		{
			if ( type.equals( TYPE_ITEM ) )
			{
				if ( contentString == null )
				{
					contentString = TradeableItemDatabase.getItemName( contentInt );
					if ( contentString == null )
					{
						contentString = "none";
						contentInt = -1;
					}
					return;
				}

				if ( contentString.equalsIgnoreCase( "none" ) )
				{
					contentInt = -1;
					return;
				}

				// Allow for an item number to be specified
				// inside of the "item" construct.

				for ( int i = 0; i < contentString.length(); ++i )
				{
					// If you get an actual item number, then store it inside
					// of contentInt and return from the method.  But, in this
					// case, we're testing if it's not an item number -- use
					// substring matching to make it user-friendlier.

					if ( !Character.isDigit( contentString.charAt(i) ) )
					{
						AdventureResult item = DEFAULT_SHELL.getFirstMatchingItem( contentString, KoLmafiaCLI.NOWHERE );

						// Otherwise, throw an AdvancedScriptException so that
						// an unsuccessful parse happens before the script gets
						// executed (consistent with paradigm).

						if ( item == null )
							throw new AdvancedScriptException( "Item " + contentString + " not found in database " + getLineAndFile() );

						contentInt = item.getItemID();
						contentString = TradeableItemDatabase.getItemName( contentInt );
						return;
					}
				}

				// Since it is numeric, parse the integer value
				// and store it inside of the contentInt.

				contentInt = Integer.parseInt( contentString );
				contentString = TradeableItemDatabase.getItemName( contentInt );
				return;
			}
			else if ( type.equals( TYPE_ZODIAC ) )
			{
				for ( int i = 0; i < ZODIACS.length; ++i )
				{
					if ( contentString.equalsIgnoreCase( ZODIACS[i] ) )
					{
						contentInt = i;
						return;
					}
				}

				throw new AdvancedScriptException( "Unknown zodiac " + contentString + " " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_LOCATION ) )
			{
				if ( (content = AdventureDatabase.getAdventure( contentString )) == null )
					throw new AdvancedScriptException( "Location " + contentString + " not found in database " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_CLASS ) )
			{
				for ( int i = 0; i < CLASSES.length; ++i )
				{
					if ( contentString.equalsIgnoreCase( CLASSES[i] ) )
					{
						contentInt = i;
						return;
					}
				}

				throw new AdvancedScriptException( "Unknown private class " + contentString + " " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_STAT ) )
			{
				for ( int i = 0; i < STATS.length; ++i )
				{
					if ( contentString.equalsIgnoreCase( STATS[i] ) )
					{
						contentInt = i;
						return;
					}
				}

				throw new AdvancedScriptException( "Unknown stat " + contentString + " " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_SKILL ) )
			{
				if ( contentString == null )
				{
					contentString = ClassSkillsDatabase.getSkillName( contentInt );
					if ( contentString == null )
					{
						contentString = "none";
						contentInt = -1;
					}
					return;
				}

				List skills = ClassSkillsDatabase.getMatchingNames( contentString );

				if ( skills.isEmpty() )
					throw new AdvancedScriptException( "Skill " + contentString + " not found in database " + getLineAndFile() );

				contentInt = ClassSkillsDatabase.getSkillID( (String) skills.get(0) );
				contentString = ClassSkillsDatabase.getSkillName( contentInt );
			}
			else if ( type.equals( TYPE_EFFECT ) )
			{
				if ( contentString == null )
				{
					contentString = StatusEffectDatabase.getEffectName( contentInt );
					if ( contentString == null )
					{
						contentString = "none";
						contentInt = -1;
					}
					return;
				}
				
				AdventureResult effect = DEFAULT_SHELL.getFirstMatchingEffect( contentString );
				if ( effect == null )
					throw new AdvancedScriptException( "Effect " + contentString + " not found in database " + getLineAndFile() );

				contentInt = StatusEffectDatabase.getEffectID( effect.getName() );
				contentString = StatusEffectDatabase.getEffectName( contentInt );
			}
			else if ( type.equals( TYPE_FAMILIAR ) )
			{
				if ( contentString == null )
				{
					contentString = FamiliarsDatabase.getFamiliarName( contentInt );
					if ( contentString == null )
					{
						contentString = "none";
						contentInt = -1;
					}
					return;
				}

				if ( contentString.equalsIgnoreCase( "none" ) )
					contentInt = -1;
				else if ( (contentInt = FamiliarsDatabase.getFamiliarID( contentString )) == -1 )
					throw new AdvancedScriptException( "Familiar " + contentString + " not found in database " + getLineAndFile() );

				contentString = FamiliarsDatabase.getFamiliarName( contentInt );
			}
			else if ( type.equals( TYPE_SLOT ) )
			{
				if ( contentString == null )
				{
					if ( contentInt < 0 || contentInt >= EquipmentRequest.slotNames.length ) 
						throw new AdvancedScriptException( "Bad slot number " + getLineAndFile() );
					contentString = EquipmentRequest.slotNames[contentInt];
					return;
				}

				contentInt = EquipmentRequest.slotNumber( contentString );
				if ( contentInt == -1 )
					throw new AdvancedScriptException( "Bad slot name " + contentString + getLineAndFile() );
			}
			else if ( type.equals( TYPE_MONSTER ) )
			{
				MonsterDatabase.Monster monster = MonsterDatabase.findMonster( contentString );
				if ( monster == null )
					throw new AdvancedScriptException( "Bad monster name " + contentString + getLineAndFile() );
				content = monster;
			}
		}

		public KoLAdventure getLocation()
		{
			if ( !type.equals( TYPE_LOCATION ) )
				throw new RuntimeException( "Internal error: getLocation() called on non-location" );
			else
				return ( KoLAdventure ) content;
		}
	}

	private class ScriptExpression extends ScriptCommand
	{
		ScriptExpression lhs;
		ScriptExpression rhs;
		ScriptOperator oper;

		public ScriptExpression()
		{
		}

		public ScriptExpression( ScriptExpression lhs, ScriptExpression rhs, ScriptOperator oper ) throws AdvancedScriptException
		{
			this.lhs = lhs;
			this.rhs = rhs;

			if ( rhs != null && !lhs.getType().equals( rhs.getType() ) )
			{
				boolean validOperation = false;

				// float-check values
				validOperation |= lhs.getType().equals( TYPE_INT ) && rhs.getType().equals( TYPE_FLOAT );
				validOperation |= lhs.getType().equals( TYPE_FLOAT ) && rhs.getType().equals( TYPE_INT );

				// string operations are valid
				validOperation |= lhs.getType().equals( TYPE_STRING ) || rhs.getType().equals( TYPE_STRING );

				if ( !validOperation )
					throw new AdvancedScriptException( "Cannot apply " + rhs.getType() + " to " + lhs + " " + getLineAndFile() );
			}

			this.oper = oper;
		}

		public ScriptType getType()
		{
			// Unary operators have no right hand side
			if ( rhs == null )
				return lhs.getType();

			if ( oper.isBool() )
				return BOOLEAN_TYPE;

			// Anything concatenated with a string yields a string
			if ( lhs.getType().equals( TYPE_STRING ) || rhs.getType().equals( TYPE_STRING ) && oper.equals( "+" ) )
				return STRING_TYPE;
			if ( lhs.getType().equals( TYPE_FLOAT ) ) // int ( oper ) float evaluates to float.
				return lhs.getType();
			return rhs.getType();
		}

		public ScriptExpression getLeftHandSide()
		{
			return lhs;
		}

		public ScriptExpression getRightHandSide()
		{
			return rhs;
		}

		public ScriptOperator getOperator()
		{
			return oper;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			try
			{
				return oper.applyTo( lhs, rhs );
			}
			catch( AdvancedScriptException e )
			{
				throw new RuntimeException( "AdvancedScriptException in execution - should occur only during parsing." );
			}
		}

		public String toString()
		{	return lhs + " " + oper.toString() + " " + rhs;
		}
	}

	private class ScriptExpressionList extends ScriptList
	{
		public boolean addElement( ScriptExpression n )
		{	return super.addElement( n );
		}

		public ScriptExpression getFirstExpression()
		{	return (ScriptExpression)getFirstElement();
		}

		public ScriptExpression getNextExpression()
		{	return (ScriptExpression)getNextElement();
		}
	}

	private class ScriptOperator
	{
		String operator;

		public ScriptOperator( String operator )
		{
			if ( operator == null )
				throw new RuntimeException( "Internal error in ScriptOperator()" );

			this.operator = operator;
		}

		public boolean precedes( ScriptOperator oper )
		{
			return operStrength() > oper.operStrength();
		}

		private int operStrength()
		{
			if ( operator.equals( "!" ) )
				return 6;

			if ( operator.equals( "*" ) || operator.equals( "/" ) || operator.equals( "%" ) )
				return 5;

			if ( operator.equals( "+" ) || operator.equals( "-" ) )
				return 4;

			if ( operator.equals( "<" ) || operator.equals( ">" ) || operator.equals( "<=" ) || operator.equals( ">=" ) )
				return 3;

			if ( operator.equals( "==" ) || operator.equals( "!=" ) )
				return 2;

			if ( operator.equals( "||" ) || operator.equals( "&&" ) )
				return 1;

			return -1;
		}

		public boolean isBool()
		{	return !operator.equals( "*" ) && !operator.equals( "/" ) && !operator.equals( "%" ) && !operator.equals( "+" ) && !operator.equals( "-" );
		}

		public String toString()
		{	return operator;
		}

		public ScriptValue applyTo( ScriptExpression lhs, ScriptExpression rhs ) throws AdvancedScriptException
		{
			ScriptValue leftValue = lhs.execute();
			captureValue( leftValue );
			if ( currentState == STATE_EXIT )
				return null;

			// Unary Operators
			if ( operator.equals( "!" ) )
				return new ScriptValue( leftValue.intValue() == 0 );

			if ( operator.equals( "-" ) && rhs == null )
			{
				if ( lhs.getType().equals( TYPE_INT ) )
					return new ScriptValue( INT_TYPE, 0 - leftValue.intValue() );
				if ( lhs.getType().equals( TYPE_FLOAT ) )
					return new ScriptValue( 0.0 - leftValue.floatValue() );
				throw new RuntimeException( "Unary minus can only be applied to numbers" );
			}

			// Unknown operator
			if ( rhs == null )
				throw new RuntimeException( "Internal error: missing right operand." );

			// Binary operators with optional right values
			if ( operator.equals( "||" ) )
			{
				if ( leftValue.intValue() == 1 )
					return TRUE_VALUE;
				ScriptValue rightValue = rhs.execute();
				captureValue( rightValue );
				if ( currentState == STATE_EXIT )
					return null;
				return rightValue;
			}
			if ( operator.equals( "&&" ) )
			{
				if ( leftValue.intValue() == 0 )
					return FALSE_VALUE;
				ScriptValue rightValue = rhs.execute();
				captureValue( rightValue);
				if ( currentState == STATE_EXIT )
					return null;
				return rightValue;
			}

			// Ensure type compatibility of operands
			if ( !rhs.getType().equals( lhs.getType() ) )
			{
				boolean validOperation = false;

				// float-check values
				validOperation |= lhs.getType().equals( TYPE_INT ) && rhs.getType().equals( TYPE_FLOAT );
				validOperation |= lhs.getType().equals( TYPE_FLOAT ) && rhs.getType().equals( TYPE_INT );

				// string operations are valid
				validOperation |= (lhs.getType().equals( TYPE_STRING ) || rhs.getType().equals( TYPE_STRING )) && operator.equals( "+" );

				if ( !validOperation )
					throw new RuntimeException( "Internal error: left hand side and right hand side do not correspond" );
			}

			// Binary operators
			ScriptValue rightValue = rhs.execute();
			captureValue( rightValue );
			if ( currentState == STATE_EXIT )
				return null;

			// String operators
			if ( operator.equals( "+" ) )
			{
				if ( lhs.getType().equals( TYPE_STRING ) || rhs.getType().equals( TYPE_STRING ) )
					return new ScriptValue( TYPE_STRING, leftValue.toStringValue().toString() + rightValue.toStringValue().toString() );
			}

			if ( operator.equals( "==" ) )
			{
				if ( lhs.getType().equals( TYPE_STRING ) ||
				     lhs.getType().equals( TYPE_LOCATION ) ||
				     lhs.getType().equals( TYPE_MONSTER ) )
					return new ScriptValue( leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
			}

			if ( operator.equals( "!=" ) )
			{
				if ( lhs.getType().equals( TYPE_STRING ) ||
				     lhs.getType().equals( TYPE_LOCATION ) ||
				     lhs.getType().equals( TYPE_MONSTER ) )
					return new ScriptValue( !leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
			}

			// Arithmetic operators
			boolean isInt;
			double lfloat = 0.0, rfloat = 0.0;
			int lint = 0, rint = 0;

			if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
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

			if ( operator.equals( "+" ) )
			{
				if ( isInt )
					return new ScriptValue( TYPE_INT, lint + rint );
				return new ScriptValue( lfloat + rfloat );
			}

			if ( operator.equals( "-" ) )
			{
				if ( isInt )
					return new ScriptValue( TYPE_INT, lint - rint );
				return new ScriptValue( lfloat - rfloat );
			}

			if ( operator.equals( "*" ) )
			{
				if ( isInt )
					return new ScriptValue( TYPE_INT, lint * rint );
				return new ScriptValue( lfloat * rfloat );
			}

			if ( operator.equals( "/" ) )
			{
				if ( isInt )
					return new ScriptValue( TYPE_INT, lint / rint );
				return new ScriptValue( lfloat / rfloat );
			}

			if ( operator.equals( "%" ) )
			{
				if ( isInt )
					return new ScriptValue( TYPE_INT, lint % rint );
				return new ScriptValue( lfloat % rfloat );
			}

			if ( operator.equals( "<" ) )
			{
				if ( isInt )
					return new ScriptValue( lint < rint );
				return new ScriptValue( lfloat < rfloat );
			}

			if ( operator.equals( ">" ) )
			{
				if ( isInt )
					return new ScriptValue( lint > rint );
				return new ScriptValue( lfloat > rfloat );
			}

			if ( operator.equals( "<=" ) )
			{
				if ( isInt )
					return new ScriptValue( lint <= rint );
				return new ScriptValue( lfloat <= rfloat );
			}

			if ( operator.equals( ">=" ) )
			{
				if ( isInt )
					return new ScriptValue( lint >= rint );
				return new ScriptValue( lfloat >= rfloat );
			}

			if ( operator.equals( "==" ) )
			{
				if ( isInt )
					return new ScriptValue( lint == rint );
				return new ScriptValue( lfloat == rfloat );
			}

			if ( operator.equals( "!=" ) )
			{
				if ( isInt )
					return new ScriptValue( lint != rint );
				return new ScriptValue( lfloat != rfloat );
			}

			// Unknown operator
			throw new RuntimeException( "Internal error: illegal operator." );
		}
	}

	private class ScriptList extends ArrayList
	{
		private int searchIndex = -1;

		public boolean addElement( Object n )
		{
			add( n );
			return true;
		}

		public Object getFirstElement()
		{
			searchIndex = -1;
			return getNextElement();
		}

		public Object getNextElement()
		{
			if ( ++searchIndex >= size() )
				return null;
			return get( searchIndex );
		}

		public Object getNextElement( Object n )
		{
			searchIndex = indexOf( n );
			if ( searchIndex == -1 )
				return null;
			return getNextElement();
		}
	}

	private class AdvancedScriptException extends Exception
	{
		AdvancedScriptException( String s )
		{
			super( s );
		}
	}
}
