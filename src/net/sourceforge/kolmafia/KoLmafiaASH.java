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
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.DataUtilities;


//Parameter value requests
import javax.swing.JOptionPane;

//Arrays
import java.lang.reflect.Array;

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
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

	public static final String [] zodiacs = { "none", "wallaby", "mongoose", "vole", "platypus", "opossum", "marmot", "wombat", "blender", "packrat" };
	public static final String [] classes = { "seal clubber", "turtle tamer", "pastamancer", "sauceror", "disco bandit", "accordion thief" };
	public static final String [] stats = { "muscle", "mysticality", "moxie" };
	public static final String [] booleans = { "true", "false" };

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

	public String fileName;
	public LineNumberReader commandStream;

	public void execute( File scriptFile ) throws IOException
	{
		commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( scriptFile ) ) );
		this.fileName = scriptFile.getPath();

		line = getNextLine();
		lineNumber = commandStream.getLineNumber();
		nextLine = getNextLine();

		try
		{
			global = parseScope( null, new ScriptVariableList(), getExistingFunctionScope(), false );

			if ( line != null )
				throw new AdvancedScriptException( "Script parsing error " + getLineAndFile() );

			commandStream.close();
			printScope( global, 0 );

			currentState = STATE_NORMAL;
			ScriptValue result = executeGlobalScope( global );
			if ( result.getType().equals( TYPE_BOOLEAN ) )
			{
				if ( result.getIntValue() == 0 )
				{
					DEFAULT_SHELL.printLine( "Script failed!" );
					return;
				}
				else
				{
					DEFAULT_SHELL.printLine( "Script succeeded!" );
					return;
				}
			}
			else
			{
				DEFAULT_SHELL.printLine(  "Script returned value " + result.toString() );
				return;
			}

		}
		catch( AdvancedScriptException e )
		{
			commandStream.close();
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, e.getMessage() );

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
			return;
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
			// If an IOException occurs during the parsing of the
			// command, you should exit from the command with an
			// error state after printing the stack trace.

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();

			return null;
		}
	}


	private ScriptScope parseScope( ScriptType expectedType, ScriptVariableList variables, ScriptScope parentScope, boolean whileLoop ) throws AdvancedScriptException
	{
		return parseScope( null, expectedType, variables, parentScope, whileLoop );
	}

	private ScriptScope parseFile( String fileName, ScriptScope startScope, ScriptScope parentScope ) throws AdvancedScriptException, java.io.FileNotFoundException
	{
		ScriptScope result;
		this.fileName = fileName;

		File scriptFile = new File( "scripts" + File.separator + fileName );
		if ( !scriptFile.exists() )
			scriptFile = new File( "scripts" + File.separator + fileName + ".ash" );

		commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( scriptFile ) ) );

		line = getNextLine();
		lineNumber = commandStream.getLineNumber();
		nextLine = getNextLine();

		result = parseScope( startScope, null, new ScriptVariableList(), parentScope, false );

		try
		{
			commandStream.close();
		}
		catch( IOException e )
		{
			throw new RuntimeException( "Internal Error: IOException occurred on attempt to close file." );
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
			if ( currentToken().equalsIgnoreCase( ";" ) )
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
					if ( startScope == null ) //only upper level scope may be directly executed.
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
			else if ( (v = parseVariable( t )) != null )
			{
				if ( !result.addVariable( v ) )
					throw new AdvancedScriptException( "Variable " + v.getName() + " already defined " + getLineAndFile() );
				if ( currentToken().equalsIgnoreCase( ";" ) )
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
		String functionName;
		ScriptFunction result;
		ScriptType paramType = null;
		ScriptVariable param = null;
		ScriptVariable paramNext = null;
		ScriptVariableList paramList = null;
		ScriptVariableReference paramRef = null;

		if ( parseIdentifier( currentToken() ) )
			functionName = currentToken();
		else
			return null;

		if ( nextToken() == null || !nextToken().equalsIgnoreCase( "(" ) )
			return null;

		readToken(); //read Function name
		readToken(); //read (

		paramList = new ScriptVariableList();

		result = new ScriptFunction( functionName, t );
		while ( !currentToken().equalsIgnoreCase( ")" ) )
		{
			if ( (paramType = parseType()) == null )
				throw new AdvancedScriptException( " ')' Expected " + getLineAndFile() );

			if ( (param = parseVariable( paramType )) == null )
				throw new AdvancedScriptException( " Identifier expected " + getLineAndFile() );

			if ( !currentToken().equalsIgnoreCase( ")" ) )
			{
				if ( !currentToken().equalsIgnoreCase( "," ) )
					throw new AdvancedScriptException( " ')' Expected " + getLineAndFile() );

				readToken(); //read comma
			}
			paramRef = new ScriptVariableReference( param );
			result.addVariableReference( paramRef );
			if ( !paramList.addElement( param ) )
				throw new AdvancedScriptException( "Variable " + param.getName() + " already defined " + getLineAndFile() );
		}

		readToken(); //read )

		if ( !currentToken().equalsIgnoreCase( "{" ) ) //Scope is a single call
		{
			result.setScope( new ScriptScope( parseCommand( t, parentScope, false, false ), parentScope ) );

			for ( param = paramList.getFirstVariable(); param != null; param = paramNext )
			{
				paramNext = paramList.getNextVariable( param );
				if ( !result.getScope().addVariable( param ) )
					throw new AdvancedScriptException( "Variable " + param.getName() + " already defined " + getLineAndFile() );
			}

			if ( !result.getScope().assertReturn() )
				throw new AdvancedScriptException( "Missing return value " + getLineAndFile() );
		}
		else
		{
			readToken(); //read {
			result.setScope( parseScope( t, paramList, parentScope, false ) );
			if ( !currentToken().equalsIgnoreCase( "}" ) )
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

	private ScriptVariable parseVariable( ScriptType t )
	{
		ScriptVariable result;

		if ( parseIdentifier( currentToken() ) )
			result = new ScriptVariable( currentToken(), t );
		else
			return null;

		readToken(); // If parsing of Identifier succeeded, go to next token.
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
				throw new AdvancedScriptException( "break outside of loop " + getLineAndFile() );

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

		if ( currentToken() == null || !currentToken().equalsIgnoreCase( ";" ) )
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
		else
			return null;

		readToken();
		return new ScriptType( type );
	}

	private boolean parseIdentifier( String identifier )
	{
		if ( !Character.isLetter( identifier.charAt( 0 ) ) && (identifier.charAt( 0 ) != '_' ) )
			return false;

		for ( int i = 1; i < identifier.length(); i++ )
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

		if ( currentToken() != null && currentToken().equalsIgnoreCase( ";" ) )
		{
			if ( expectedType != null && expectedType.equals( TYPE_VOID ) )
				return new ScriptReturn( null, new ScriptType( TYPE_VOID ) );

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
			if ( nextToken() == null || !nextToken().equalsIgnoreCase( "(" ) )
				throw new AdvancedScriptException( "'(' Expected " + getLineAndFile() );

			readToken(); //if or while
			readToken(); //(

			expression = parseExpression( parentScope );
			if ( currentToken() == null || !currentToken().equalsIgnoreCase( ")" ) )
				throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

			readToken(); // )

			do
			{
				if ( currentToken() == null || !currentToken().equalsIgnoreCase( "{" ) ) //Scope is a single call
				{
					command = parseCommand( functionType, parentScope, !elseFound, (repeat || loop) );
					scope = new ScriptScope( command, parentScope );
					if ( result == null )
						result = new ScriptLoop( scope, expression, repeat );
				}
				else
				{
					readToken(); //read {
					scope = parseScope( functionType, null, parentScope, (repeat || loop ) );

					if ( currentToken() == null || !currentToken().equalsIgnoreCase( "}" ) )
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

						if ( currentToken() == null || !currentToken().equalsIgnoreCase( "(" ) )
							throw new AdvancedScriptException( "'(' Expected " + getLineAndFile() );

						readToken(); //(
						expression = parseExpression( parentScope );

						if ( currentToken() == null || !currentToken().equalsIgnoreCase( ")" ) )
							throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

						readToken(); // )
					}
					else //else without condition
					{
						readToken(); //else
						expression = new ScriptValue( new ScriptType( TYPE_BOOLEAN ), 1 );
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
		String name = null;
		String varName;
		ScriptCall result;
		ScriptExpressionList params;
		ScriptExpression val;

		if ( nextToken() == null || !nextToken().equalsIgnoreCase( "(" ) )
			return null;

		if ( parseIdentifier( currentToken() ) )
			name = currentToken();
		else
			return null;

		readToken(); //name
		readToken(); //(

		params = new ScriptExpressionList();
		while ( currentToken() != null && !currentToken().equalsIgnoreCase( ")" ) )
		{
			if ( (val = parseExpression( scope )) != null )
			{
				params.addElement( val );
			}
			if ( !currentToken().equalsIgnoreCase( "," ) )
			{
				if ( !currentToken().equalsIgnoreCase( ")" ) )
				{
					throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );
				}
			}
			else
			{
				readToken();
				if ( currentToken().equalsIgnoreCase( ")" ) )
					throw new AdvancedScriptException( "Parameter expected " + getLineAndFile() );
			}
		}

		if ( !currentToken().equalsIgnoreCase( ")" ) )
			throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

		readToken(); // )
		result = new ScriptCall( name, scope, params );

		return result;
	}

	private ScriptAssignment parseAssignment( ScriptScope scope ) throws AdvancedScriptException
	{
		String name;
		ScriptVariableReference leftHandSide;
		ScriptExpression rightHandSide;

		if ( nextToken() == null || !nextToken().equalsIgnoreCase( "=" ) )
			return null;

		if ( parseIdentifier( currentToken() ) )
			name = currentToken();
		else
			return null;

		readToken(); //name
		readToken(); //=

		leftHandSide = new ScriptVariableReference( name, scope );
		rightHandSide = parseExpression( scope );
		return new ScriptAssignment( leftHandSide, rightHandSide );
	}

	private ScriptExpression parseExpression( ScriptScope scope ) throws AdvancedScriptException
	{
		return parseExpression( scope, null );
	}

	private ScriptExpression parseExpression( ScriptScope scope, ScriptOperator previousOper ) throws AdvancedScriptException
	{
		ScriptExpression lhs = null;
		ScriptExpression rhs = null;
		ScriptOperator oper = null;

		if ( currentToken() == null )
			return null;

		if ( currentToken() != null && currentToken().equalsIgnoreCase( "!" ) )
		{
			readToken(); // !
			if ( (lhs = parseValue( scope )) == null )
				throw new AdvancedScriptException( "Value expected " + getLineAndFile() );
			lhs = new ScriptExpression( lhs, null, new ScriptOperator( "!" ) );
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
		int i;

		if ( currentToken() == null )
			return null;


		if ( currentToken().equalsIgnoreCase( "(" ) )
		{
			readToken();// (
			result = parseExpression( scope );
			if ( currentToken() == null || !currentToken().equalsIgnoreCase( ")" ) )
				throw new AdvancedScriptException( "')' Expected " + getLineAndFile() );

			readToken();// )
			return result;
		}

		//Parse true and false first since they are reserved words.
		if ( currentToken().equalsIgnoreCase( "true" ) )
		{
			readToken();
			return new ScriptValue( new ScriptType( TYPE_BOOLEAN ), 1 );
		}
		else if ( currentToken().equalsIgnoreCase( "false" ) )
		{
			readToken();
			return new ScriptValue( new ScriptType( TYPE_BOOLEAN ), 0 );
		}

		else if ( (result = parseCall( scope )) != null )
			return result;

		else if ( (result = parseVariableReference( scope )) != null )
			return result;

		else if ( Character.isDigit( currentToken().charAt( 0 ) ) || currentToken().charAt( 0 ) == '-' )
		{
			int resultInt;

			boolean negative = false;

			i = 0;

			if ( currentToken().charAt( 0 ) == '-' )
			{
				negative = true;
				i = 1;
			}

			for ( resultInt = 0; i < currentToken().length(); i++ )
			{
				if ( !Character.isDigit( currentToken().charAt(i) ) )
				{
					if ( currentToken().charAt(i) == '.' )
					{
						return parseFloat();
					}
					else
						throw new AdvancedScriptException( "Failed to parse numeric value " + getLineAndFile() );
				}
				resultInt = ( resultInt * 10 ) + ( currentToken().charAt(i ) - '0' );
			}
			if ( negative )
				resultInt = resultInt * -1;

			readToken(); //integer
			return new ScriptValue( new ScriptType( TYPE_INT ), resultInt );
		}
		else if ( currentToken().equalsIgnoreCase( "\"" ) )
		{
			//Directly work with line - ignore any "tokens" you meet until the string is closed
			String resultString = "";
			for ( i = 1; ; i++ )
			{
				if ( i == line.length() )
				{
					throw new AdvancedScriptException( "No closing '\"' found " + getLineAndFile() );
				}
				else if ( line.charAt(i ) == '\\' )
				{
					resultString = resultString + line.charAt( ++i );
				}
				else if ( line.charAt(i ) == '"' )
				{
					line = line.substring( i + 1 ); //+ 1 to get rid of '"' token
					return new ScriptValue( new ScriptType( TYPE_STRING ), resultString );
				}
				else
				{
					resultString = resultString + line.charAt( i );
				}
			}

		}
		else if ( currentToken().equalsIgnoreCase( "$" ) )
		{
			ScriptType type;
			readToken();
			type = parseType();

			if ( type == null )
				throw new AdvancedScriptException( "Unknown type " + currentToken() + " " + getLineAndFile() );
			if ( !currentToken().equalsIgnoreCase( "[" ) )
				throw new AdvancedScriptException( "'[' Expected " + getLineAndFile() );

			String resultString = "";
			for ( i = 1; ; i++ )
			{
				if ( i == line.length() )
				{
					throw new AdvancedScriptException( "No closing ']' found " + getLineAndFile() );
				}
				else if ( line.charAt(i ) == '\\' )
				{
					resultString = resultString + line.charAt( ++i );
				}
				else if ( line.charAt(i ) == ']' )
				{
					line = line.substring( i + 1 ); //+1 to get rid of ']' token
					return new ScriptValue( type, resultString );
				}
				else
				{
					resultString = resultString + line.charAt( i );
				}

			}
		}
		return null;
	}

	private ScriptValue parseFloat() throws AdvancedScriptException
	{
		try
		{
			float result;

			result = Float.parseFloat( currentToken() );
			readToken(); //float
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
			oper.equalsIgnoreCase( "!" ) ||
			oper.equalsIgnoreCase( "*" ) || oper.equalsIgnoreCase( "/" ) || oper.equalsIgnoreCase( "%" ) ||
			oper.equalsIgnoreCase( "+" ) || oper.equalsIgnoreCase( "-" ) ||
			oper.equalsIgnoreCase( "<" ) || oper.equalsIgnoreCase( ">" ) || oper.equalsIgnoreCase( "<=" ) || oper.equalsIgnoreCase( ">=" ) ||
			oper.equalsIgnoreCase( "==" ) || oper.equalsIgnoreCase( "!=" ) ||
			oper.equalsIgnoreCase( "||" ) || oper.equalsIgnoreCase( "&&" )
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

		if ( !currentToken().equalsIgnoreCase( "<" ) )
			throw new AdvancedScriptException( "'<' Expected " + getLineAndFile() );
		for ( i = 1; ; i++ )
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

		if ( result.equalsIgnoreCase( "" ) )
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

		while ( line.equalsIgnoreCase( "" ) )
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

		while ( nextLine.equalsIgnoreCase( "" ) )
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
			for ( int i = 0; i < java.lang.reflect.Array.getLength( tokenList ); i++ )
				if ( s.charAt( 0 ) == tokenList[i] )
					return true;
			return false;
		}
		else
		{
			for ( int i = 0; i < java.lang.reflect.Array.getLength( multiCharTokenList ); i++ )
				if ( s.equalsIgnoreCase( multiCharTokenList[i] ) )
					return true;
			return false;
		}
	}


	private void printScope( ScriptScope scope, int indent )
	{
		ScriptVariable currentVar;
		ScriptFunction currentFunc;
		ScriptCommand currentCommand;

		indentLine( indent );
		KoLmafia.getLogStream().println( "<SCOPE>" );

		indentLine( indent + 1 );
		KoLmafia.getLogStream().println( "<VARIABLES>" );
		for ( currentVar = scope.getFirstVariable(); currentVar != null; currentVar = scope.getNextVariable( currentVar ) )
			printVariable( currentVar, indent + 2 );
		indentLine( indent + 1 );
		KoLmafia.getLogStream().println( "<FUNCTIONS>" );
		for ( currentFunc = scope.getFirstFunction(); currentFunc != null; currentFunc = scope.getNextFunction( currentFunc ) )
			printFunction( currentFunc, indent + 2 );
		indentLine( indent + 1 );
		KoLmafia.getLogStream().println( "<COMMANDS>" );
		for ( currentCommand = scope.getFirstCommand(); currentCommand != null; currentCommand = scope.getNextCommand( currentCommand ) )
			printCommand( currentCommand, indent + 2 );
	}

	private void printVariable( ScriptVariable var, int indent )
	{
		indentLine( indent );
		KoLmafia.getLogStream().println( "<VAR " + var.getType().toString() + " " + var.getName().toString() + ">" );
	}

	private void printFunction( ScriptFunction func, int indent )
	{
		indentLine( indent );
		KoLmafia.getLogStream().println( "<FUNC " + func.getType().toString() + " " + func.getName().toString() + ">" );
		for ( ScriptVariableReference current = func.getFirstParam(); current != null; current = func.getNextParam( current ) )
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
			KoLmafia.getLogStream().println( "<COMMAND " + command.toString() + ">" );
		}
	}

	private void printReturn( ScriptReturn ret, int indent )
	{
		indentLine( indent );
		KoLmafia.getLogStream().println( "<RETURN " + ret.getType().toString() + ">" );
		if ( !ret.getType().equals( TYPE_VOID ) )
			printExpression( ret.getExpression(), indent + 1 );
	}

	private void printLoop( ScriptLoop loop, int indent )
	{
		indentLine( indent );
		if ( loop.repeats() )
			KoLmafia.getLogStream().println( "<WHILE>" );
		else
			KoLmafia.getLogStream().println( "<IF>" );
		printExpression( loop.getCondition(), indent + 1 );
		printScope( loop.getScope(), indent + 1 );
		for ( ScriptLoop currentElse = loop.getFirstElseLoop(); currentElse != null; currentElse = loop.getNextElseLoop( currentElse ) )
			printLoop( currentElse, indent + 1 );
	}

	private void printCall( ScriptCall call, int indent )
	{
		indentLine( indent );
		KoLmafia.getLogStream().println( "<CALL " + call.getTarget().getName().toString() + ">" );
		for ( ScriptExpression current = call.getFirstParam(); current != null; current = call.getNextParam( current ) )
			printExpression( current, indent + 1 );
	}

	private void printAssignment( ScriptAssignment assignment, int indent )
	{
		indentLine( indent );
		KoLmafia.getLogStream().println( "<ASSIGN " + assignment.getLeftHandSide().getName().toString() + ">" );
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
			KoLmafia.getLogStream().println( "<VALUE " + value.getType().toString() + " [" + value.toString() + "]>" );
		}
	}

	public void printOperator( ScriptOperator oper, int indent )
	{
		indentLine( indent );
		KoLmafia.getLogStream().println( "<OPER " + oper.toString() + ">" );
	}

	public void printVariableReference( ScriptVariableReference varRef, int indent )
	{
		indentLine( indent );
		KoLmafia.getLogStream().println( "<VARREF> " + varRef.getName().toString() );
	}

	private void indentLine( int indent )
	{
		for (int i = 0; i < indent; i++ )
			KoLmafia.getLogStream().print( "   " );
	}


	private ScriptValue executeGlobalScope( ScriptScope globalScope ) throws AdvancedScriptException
	{
		ScriptFunction		main;
		ScriptValue		result = null;
		String			resultString;

		main = globalScope.findFunction( "main", null );



		if ( main == null )
		{
			if ( globalScope.getFirstCommand() == null )
				throw new AdvancedScriptException( "No function main or command found." );
			result = globalScope.execute();
		}
		else
		{
			requestUserParams( main );
			result = main.execute();
		}



		return result;
	}






	private void requestUserParams( ScriptFunction targetFunction ) throws AdvancedScriptException
	{
		ScriptVariableReference	param;
		String			resultString;


		for ( param = targetFunction.getFirstParam(); param != null; param = targetFunction.getNextParam( param ) )
		{
			if ( param.getType().equals( TYPE_ZODIAC ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType().toString() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					zodiacs,
					zodiacs[0]
				 );
				param.setValue( new ScriptValue( TYPE_ZODIAC, resultString ) );
			}
			else if ( param.getType().equals( TYPE_CLASS ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType().toString() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					classes,
					classes[0]
				 );
				param.setValue( new ScriptValue( TYPE_CLASS, resultString ) );
			}
			else if ( param.getType().equals( TYPE_STAT ) )
			{
				resultString = ( String ) JOptionPane.showInputDialog
				(
					null,
					"Please input a value for " + param.getType().toString() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					stats,
					stats[0]
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
				param.getType().equals( TYPE_FAMILIAR )
			)
			{
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType().toString() + " " + param.getName() );
				param.setValue( new ScriptValue( param.getType(), resultString ) );
			}
			else if ( param.getType().equals( TYPE_INT ) )
			{
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType().toString() + " " + param.getName() );
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
				resultString = JOptionPane.showInputDialog( "Please input a value for " + param.getType().toString() + " " + param.getName() );
				try
				{
					param.setValue( new ScriptValue( TYPE_FLOAT, Float.parseFloat( resultString ) ) );
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
					"Please input a value for " + param.getType().toString() + " " + param.getName(),
					"Input Variable",
					JOptionPane.INFORMATION_MESSAGE,
					null,
					booleans,
					booleans[0]
				 );
				if ( resultString.equalsIgnoreCase( "true" ) )
					param.setValue( new ScriptValue( TYPE_BOOLEAN, 1 ) );
				else if ( resultString.equalsIgnoreCase( "false" ) )
					param.setValue( new ScriptValue( TYPE_BOOLEAN, 0 ) );
				else
					throw new RuntimeException( "Internal error: Illegal value for boolean" );
			}
			else if ( param.getType().equals( TYPE_VOID ) )
			{
				param.setValue( new ScriptValue( TYPE_VOID ) );
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

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_LOCATION ) };
		result.addFunction( new ScriptExistingFunction( "adventure", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "buy", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "create", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "use", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "eat", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "item_amount", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		params[0] = new ScriptType( TYPE_ITEM );
		result.addFunction( new ScriptExistingFunction( "closet_amount", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		params[0] = new ScriptType( TYPE_ITEM );
		result.addFunction( new ScriptExistingFunction( "museum_amount", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		params[0] = new ScriptType( TYPE_ITEM );
		result.addFunction( new ScriptExistingFunction( "shop_amount", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "storage_amount", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "stash_amount", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "put_closet", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "put_shop", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "put_stash", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "take_closet", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "take_storage", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "sell_item", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_STRING ) };
		result.addFunction( new ScriptExistingFunction( "print", new ScriptType( TYPE_VOID ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_zodiac", new ScriptType( TYPE_ZODIAC ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_class", new ScriptType( TYPE_CLASS ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_level", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_hp", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_maxhp", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_mp", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_maxmp", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_STAT ) };
		result.addFunction( new ScriptExistingFunction( "my_basestat", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_STAT ) };
		result.addFunction( new ScriptExistingFunction( "my_buffedstat", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_meat", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_closetmeat", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_adventures", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_inebriety", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_SKILL ) };
		result.addFunction( new ScriptExistingFunction( "have_skill", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_EFFECT ) };
		result.addFunction( new ScriptExistingFunction( "have_effect", new ScriptType( TYPE_INT ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_SKILL ) };
		result.addFunction( new ScriptExistingFunction( "use_skill", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "add_item_condition", new ScriptType( TYPE_VOID ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "can_eat", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "can_drink", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "can_interact", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "trade_hermit", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "trade_bounty_hunter", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "trade_trapper", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "equip", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "unequip", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_familiar", new ScriptType( TYPE_FAMILIAR ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_FAMILIAR ) };
		result.addFunction( new ScriptExistingFunction( "equip_familiar", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "council", new ScriptType( TYPE_VOID ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ) };
		result.addFunction( new ScriptExistingFunction( "mind_control", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "my_name", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "have_chef", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "have_bartender", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_STRING ) };
		result.addFunction( new ScriptExistingFunction( "cli_execute", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "bounty_hunter_wants", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_BOOLEAN ) };
		result.addFunction( new ScriptExistingFunction( "boolean_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ) };
		result.addFunction( new ScriptExistingFunction( "int_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_FLOAT ) };
		result.addFunction( new ScriptExistingFunction( "float_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "item_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_ZODIAC ) };
		result.addFunction( new ScriptExistingFunction( "zodiac_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_LOCATION ) };
		result.addFunction( new ScriptExistingFunction( "location_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_CLASS ) };
		result.addFunction( new ScriptExistingFunction( "class_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_STAT ) };
		result.addFunction( new ScriptExistingFunction( "stat_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_SKILL ) };
		result.addFunction( new ScriptExistingFunction( "skill_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_EFFECT ) };
		result.addFunction( new ScriptExistingFunction( "effect_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_FAMILIAR ) };
		result.addFunction( new ScriptExistingFunction( "familiar_to_string", new ScriptType( TYPE_STRING ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ) };
		result.addFunction( new ScriptExistingFunction( "wait", new ScriptType( TYPE_VOID ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "entryway", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "hedgemaze", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "guardians", new ScriptType( TYPE_ITEM ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "chamber", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "nemesis", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "guild", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "gourd", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] {};
		result.addFunction( new ScriptExistingFunction( "tavern", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_STRING ) };
		result.addFunction( new ScriptExistingFunction( "train_familiar", new ScriptType( TYPE_BOOLEAN ), params ) );

		params = new ScriptType[] { new ScriptType( TYPE_INT ), new ScriptType( TYPE_ITEM ) };
		result.addFunction( new ScriptExistingFunction( "retrieve_item", new ScriptType( TYPE_BOOLEAN ), params ) );

		return result;
	}

	class ScriptScope extends ScriptListNode
	{
		ScriptFunctionList	functions;
		ScriptVariableList	variables;
		ScriptCommandList	commands;
		ScriptScope		parentScope;

		public ScriptScope( ScriptScope parentScope )
		{
			functions = new ScriptFunctionList();
			variables = new ScriptVariableList();
			commands = new ScriptCommandList();
			this.parentScope = parentScope;
		}

		public ScriptScope( ScriptCommand command, ScriptScope parentScope )
		{
			functions = new ScriptFunctionList();
			variables = new ScriptVariableList();
			commands = new ScriptCommandList( command );
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

		public boolean addFunction( ScriptFunction f )
		{
			return functions.addElement( f );
		}

		public boolean addVariable( ScriptVariable v )
		{
			return variables.addElement( v );
		}

		public void addCommand( ScriptCommand c )
		{
			commands.addElement( c );
		}

		public ScriptScope getParentScope()
		{
			return parentScope;
		}

		public ScriptFunction getFirstFunction()
		{
			return ( ScriptFunction ) functions.getFirstElement();
		}

		public ScriptFunction getNextFunction( ScriptFunction current )
		{
			return ( ScriptFunction ) functions.getNextElement( current );
		}

		public ScriptVariable getFirstVariable()
		{
			return ( ScriptVariable ) variables.getFirstElement();
		}

		public ScriptVariable getNextVariable( ScriptVariable current )
		{
			return ( ScriptVariable ) variables.getNextElement( current );
		}

		public ScriptCommand getFirstCommand()
		{
			return ( ScriptCommand ) commands.getFirstElement();
		}

		public ScriptCommand getNextCommand( ScriptCommand current )
		{
			return ( ScriptCommand ) commands.getNextElement( current );
		}

		public boolean assertReturn()
		{
			ScriptCommand current, previous = null;

			for ( current = getFirstCommand(); current != null; previous = current, current = getNextCommand( current ) )
				;
			if ( previous == null )
				return false;
			if ( !( previous instanceof ScriptReturn ) )
				return false;
			return true;
		}

		public ScriptFunction findFunction( String name, ScriptExpressionList params ) throws AdvancedScriptException
		{
			ScriptFunction current;
			ScriptVariableReference currentParam;
			ScriptExpression currentValue;
			int paramIndex;

			for ( current = getFirstFunction(); current != null; current = getNextFunction( current ) )
			{
				if ( current.getName().equalsIgnoreCase( name ) )
				{
					if ( params == null )
						return current;
					for
					(
						paramIndex = 1, currentParam = current.getFirstParam(), currentValue = (ScriptExpression ) params.getFirstElement();
						(currentParam != null ) && (currentValue != null );
						paramIndex++, currentParam = current.getNextParam( currentParam ), currentValue = ( ScriptExpression ) params.getNextElement( currentValue )
					 )
					{
						if ( !currentParam.getType().equals( currentValue.getType() ) )
						{
							if ( currentParam.getType().equals( TYPE_FLOAT ) && currentValue.getType().equals( TYPE_INT ) )
								; //do nothing
							else if ( currentParam.getType().equals( TYPE_INT ) && currentValue.getType().equals( TYPE_FLOAT ) )
								; //do nothing
							else
								throw new AdvancedScriptException( "Illegal parameter " + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType() + " " + getLineAndFile() );
						}
					}

					if ( currentParam != null || currentValue != null )
						throw new AdvancedScriptException( "Illegal amount of parameters for function " + name + " " + getLineAndFile() );

					return current;
				}
			}
			if ( parentScope != null )
				return parentScope.findFunction( name, params );
			return null;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptCommand current;
			ScriptValue result;

			for ( current = getFirstCommand(); current != null; current = getNextCommand( current ) )
			{
				result = current.execute();
				if ( currentState == STATE_RETURN )
				{
					return result;
				}
				if ( currentState == STATE_BREAK )
				{
					return null;
				}
				if ( currentState == STATE_CONTINUE )
				{
					return null;
				}
				if ( currentState == STATE_EXIT )
				{
					return null;
				}
			}
			try
			{
				return new ScriptValue( TYPE_VOID, 0 );
			}
			catch( AdvancedScriptException e )
			{
				throw new RuntimeException( "AdvancedScriptException in execution - should occur only during parsing." );
			}
		}

	}

	class ScriptScopeList extends ScriptList
	{
		public boolean addElement( ScriptListNode n )
		{
			return addElementSerial( n );
		}
	}

	class ScriptFunction extends ScriptListNode
	{
		String name;
		ScriptType type;
		ScriptVariableReferenceList variableReferences;
		ScriptScope scope;

		public ScriptFunction()
		{
		}

		public ScriptFunction( String name, ScriptType type )
		{
			this.name = name;
			this.type = type;
			this.variableReferences = new ScriptVariableReferenceList();
			this.scope = null;
		}

		public void addVariableReference( ScriptVariableReference v )
		{
			variableReferences.addElement( v );
		}

		public void setScope( ScriptScope s )
		{
			scope = s;
		}

		public ScriptScope getScope()
		{
			return scope;
		}

		public int compareTo( Object o ) throws ClassCastException
		{
			if (!(o instanceof ScriptFunction ) )
				throw new ClassCastException();
			return name.compareTo( ((ScriptFunction)o).name );
		}

		public String getName()
		{
			return name;
		}

		public ScriptVariableReference getFirstParam()
		{
			return (ScriptVariableReference ) variableReferences.getFirstElement();
		}

		public ScriptVariableReference getNextParam( ScriptVariableReference current )
		{
			return (ScriptVariableReference ) variableReferences.getNextElement( current );
		}

		public ScriptType getType()
		{
			return type;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptValue result = scope.execute();
			if( currentState != STATE_EXIT)
				currentState = STATE_NORMAL;
			return result;
		}

		public boolean assertReturn()
		{
			return scope.assertReturn();
		}
	}


	class ScriptExistingFunction extends ScriptFunction
	{
		ScriptVariable [] variables;

		public ScriptExistingFunction( String name, ScriptType type, ScriptType [] params )
		{
			super( name, type );

			variables = new ScriptVariable[ java.lang.reflect.Array.getLength( params )];

			for ( int position = 0; position < java.lang.reflect.Array.getLength( params ); position++ )
			{
				variables[position] = new ScriptVariable( params[position] );
				variableReferences.addElement( new ScriptVariableReference( variables[position] ) );
			}
		}


		public ScriptValue execute()
		{
			try
			{
				if ( !client.permitsContinue() )
				{
					currentState = STATE_EXIT;
					return null;
				}

				if ( name.equalsIgnoreCase( "adventure" ) )
					return executeAdventureRequest( variables[0].getIntValue(), variables[1].getLocation() );
				else if ( name.equalsIgnoreCase( "buy" ) )
					return executeBuyRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "create" ) )
					return executeCreateRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "use" ) || name.equalsIgnoreCase( "eat" ) )
					return executeUseRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "item_amount" ) )
					return executeItemAmountRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "closet_amount" ) )
					return executeClosetAmountRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "museum_amount" ) )
					return executeMuseumAmountRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "shop_amount" ) )
					return executeShopAmountRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "storage_amount" ) )
					return executeStorageAmountRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "stash_amount" ) )
					return executeStashAmountRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "put_closet" ) )
					return executePutClosetRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "put_shop" ) )
					return executePutShopRequest( variables[0].getIntValue(), variables[1].getIntValue(), variables[2].getIntValue() );
				else if ( name.equalsIgnoreCase( "put_stash" ) )
					return executePutStashRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "take_closet" ) )
					return executeTakeClosetRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "take_storage" ) )
					return executeTakeStorageRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "sell_item" ) )
					return executeSellItemRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "print" ) )
					return executePrintRequest( variables[0].getStringValue() );
				else if ( name.equalsIgnoreCase( "my_zodiac" ) )
					return executeZodiacRequest();
				else if ( name.equalsIgnoreCase( "my_class" ) )
					return executeClassRequest();
				else if ( name.equalsIgnoreCase( "my_level" ) )
					return executeLevelRequest();
				else if ( name.equalsIgnoreCase( "my_hp" ) )
					return executeHPRequest();
				else if ( name.equalsIgnoreCase( "my_maxhp" ) )
					return executeMaxHPRequest();
				else if ( name.equalsIgnoreCase( "my_mp" ) )
					return executeMPRequest();
				else if ( name.equalsIgnoreCase( "my_maxmp" ) )
					return executeMaxMPRequest();
				else if ( name.equalsIgnoreCase( "my_basestat" ) )
					return executeBaseStatRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "my_buffedstat" ) )
					return executeBuffedStatRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "my_meat" ) )
					return executeMeatRequest();
				else if ( name.equalsIgnoreCase( "my_closetmeat" ) )
					return executeClosetMeatRequest();
				else if ( name.equalsIgnoreCase( "my_adventures" ) )
					return executeAdventuresRequest();
				else if ( name.equalsIgnoreCase( "my_inebriety" ) )
					return executeInebrietyRequest();
				else if ( name.equalsIgnoreCase( "have_skill" ) )
					return executeHaveSkillRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "have_effect" ) )
					return executeHaveEffectRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "use_skill" ) )
					return executeUseSkillRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "add_item_condition" ) )
					return executeItemConditionRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "can_eat" ) )
					return executeCanEatRequest();
				else if ( name.equalsIgnoreCase( "can_drink" ) )
					return executeCanDrinkRequest();
				else if ( name.equalsIgnoreCase( "can_interact" ) )
					return executeCanInteractRequest();
				else if ( name.equalsIgnoreCase( "trade_hermit" ) )
					return executeHermitRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else if ( name.equalsIgnoreCase( "trade_bounty_hunter" ) )
					return executeBountyHunterRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "trade_trapper" ) )
					return executeTrapperRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "equip" ) )
					return executeEquipRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "unequip" ) )
					return executeUnequipRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "my_familiar" ) )
					return executeMyFamiliarRequest();
				else if ( name.equalsIgnoreCase( "equip_familiar" ) )
					return executeEquipFamiliarRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "council" ) )
					return executeCouncilRequest();
				else if ( name.equalsIgnoreCase( "mind_control" ) )
					return executeMindControlRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "my_name" ) )
					return executeNameRequest();
				else if ( name.equalsIgnoreCase( "have_chef" ) )
					return executeChefRequest();
				else if ( name.equalsIgnoreCase( "have_bartender" ) )
					return executeBartenderRequest();
				else if ( name.equalsIgnoreCase( "cli_execute" ) )
					return executeCLIExecuteRequest( variables[0].getStringValue() );
				else if ( name.equalsIgnoreCase( "bounty_hunter_wants" ) )
					return executeBountyHunterWantsRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "boolean_to_string" ) )
					return executeBooleanToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "int_to_string" ) )
					return executeIntToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "float_to_string" ) )
					return executeFloatToStringRequest( variables[0].getFloatValue() );
				else if ( name.equalsIgnoreCase( "item_to_string" ) )
					return executeItemToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "zodiac_to_string" ) )
					return executeZodiacToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "location_to_string" ) )
					return executeLocationToStringRequest( variables[0].getLocation() );
				else if ( name.equalsIgnoreCase( "class_to_string" ) )
					return executeClassToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "stat_to_string" ) )
					return executeStatToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "skill_to_string" ) )
					return executeSkillToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "effect_to_string" ) )
					return executeEffectToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "familiar_to_string" ) )
					return executeFamiliarToStringRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "wait" ) )
					return executeWaitRequest( variables[0].getIntValue() );
				else if ( name.equalsIgnoreCase( "entryway" ) )
					return executeEntrywayRequest();
				else if ( name.equalsIgnoreCase( "hedgemaze" ) )
					return executeHedgemazeRequest();
				else if ( name.equalsIgnoreCase( "guardians" ) )
					return executeGuardiansRequest();
				else if ( name.equalsIgnoreCase( "chamber" ) )
					return executeChamberRequest();
				else if ( name.equalsIgnoreCase( "nemesis" ) )
					return executeNemesisRequest();
				else if ( name.equalsIgnoreCase( "guild" ) )
					return executeGuildRequest();
				else if ( name.equalsIgnoreCase( "gourd" ) )
					return executeGourdRequest();
				else if ( name.equalsIgnoreCase( "tavern" ) )
					return executeTavernRequest();
				else if ( name.equalsIgnoreCase( "train_familiar" ) )
					return executeTrainFamiliarRequest( variables[0].getIntValue(), variables[1].getStringValue() );
				else if ( name.equalsIgnoreCase( "retrieve_item" ) )
					return executeRetrieveItemRequest( variables[0].getIntValue(), variables[1].getIntValue() );
				else
					throw new RuntimeException( "Internal error: unknown library function " + name );
			}
			catch( AdvancedScriptException e )
			{
				throw new RuntimeException( "AdvancedScriptException in execution - should occur only during parsing." );
			}
		}


		private ScriptValue executeAdventureRequest( int amount, KoLAdventure location ) throws AdvancedScriptException
		{
			DEFAULT_SHELL.updateDisplay( "Beginning " + amount + " turnips to " + location.toString() + "..." );
			client.makeRequest( location, amount );

			if ( client.permitsContinue() )
				return new ScriptValue( TYPE_BOOLEAN, 1 );
			else
				return new ScriptValue( TYPE_BOOLEAN, 0 );
		}

		private ScriptValue executeBuyRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "buy " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeCreateRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "create " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeUseRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "use " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeItemAmountRequest( int itemID ) throws AdvancedScriptException
		{
			AdventureResult item;

			if( (itemID == -1))
				return new ScriptValue( TYPE_INT, 0);
			item = new AdventureResult( TradeableItemDatabase.getItemName( itemID ), 0, false );

			return new ScriptValue( TYPE_INT, item.getCount( KoLCharacter.getInventory() ) );
		}

		private ScriptValue executeClosetAmountRequest( int itemID ) throws AdvancedScriptException
		{
			AdventureResult item;

			if( (itemID == -1))
				return new ScriptValue( TYPE_INT, 0);
			item = new AdventureResult( TradeableItemDatabase.getItemName( itemID ), 0, false );

			return new ScriptValue( TYPE_INT, item.getCount( KoLCharacter.getCloset() ) );
		}

		private ScriptValue executeMuseumAmountRequest( int itemID ) throws AdvancedScriptException
		{
			if ( itemID == -1 )
				return new ScriptValue( TYPE_INT, 0);

			// Make sure you have the most up-to-date
			// data on the museum.

			if ( KoLCharacter.getCollection().isEmpty() )
				(new MuseumRequest( client )).run();

			AdventureResult item = new AdventureResult( TradeableItemDatabase.getItemName( itemID ), 0, false );
			return new ScriptValue( TYPE_INT, item.getCount( KoLCharacter.getCollection() ) );
		}

		private ScriptValue executeShopAmountRequest( int itemID ) throws AdvancedScriptException
		{
			if ( itemID == -1 )
				return new ScriptValue( TYPE_INT, 0);

			(new StoreManageRequest( client )).run(); //refresh store inventory

			LockableListModel list = StoreManager.getSoldItemList();
			StoreManager.SoldItem item = new StoreManager.SoldItem( itemID, 0, 0, 0, 0 );
			int index = list.indexOf( item );

			if ( index < 0 )
				return new ScriptValue( TYPE_INT, 0 );

			item = (StoreManager.SoldItem)list.get( index );
			return new ScriptValue( TYPE_INT, item.getQuantity() );
		}

		private ScriptValue executeStorageAmountRequest( int itemID ) throws AdvancedScriptException
		{
			AdventureResult item;

			if( (itemID == -1))
				return new ScriptValue( TYPE_INT, 0);
			item = new AdventureResult( TradeableItemDatabase.getItemName( itemID ), 0, false );

			return new ScriptValue( TYPE_INT, item.getCount( KoLCharacter.getStorage() ) );
		}

		private ScriptValue executeStashAmountRequest( int itemID ) throws AdvancedScriptException
		{
			AdventureResult item;

			if( (itemID == -1))
				return new ScriptValue( TYPE_INT, 0);
			new ClanStashRequest( client ).run(); //refresh clan stash
			item = new AdventureResult( TradeableItemDatabase.getItemName( itemID ), 0, false );

			return new ScriptValue( TYPE_INT, item.getCount( ClanManager.getStash() ) );
		}

		private ScriptValue executePutClosetRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "closet put " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executePutShopRequest( int price, int limit, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "mallsell " + TradeableItemDatabase.getItemName( itemID ) + " " + price + " " + limit );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executePutStashRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "stash put " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeTakeClosetRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "closet take " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeTakeStorageRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "hagnk " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeSellItemRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "sell " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executePrintRequest( String message ) throws AdvancedScriptException
		{
			DEFAULT_SHELL.updateDisplay( message );
			return new ScriptValue( TYPE_VOID );
		}

		private ScriptValue executeZodiacRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_ZODIAC, KoLCharacter.getSign() );
		}

		private ScriptValue executeClassRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_CLASS, KoLCharacter.getClassType() );
		}

		private ScriptValue executeLevelRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getLevel() );
		}

		private ScriptValue executeHPRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getCurrentHP() );
		}

		private ScriptValue executeMaxHPRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getMaximumHP() );
		}

		private ScriptValue executeMPRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getCurrentMP() );
		}

		private ScriptValue executeMaxMPRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getMaximumMP() );
		}

		private ScriptValue executeBaseStatRequest( int stat ) throws AdvancedScriptException
		{
			if ( stats[stat].equalsIgnoreCase( "muscle" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getBaseMuscle() );
			else if ( stats[stat].equalsIgnoreCase( "mysticality" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getBaseMysticality() );
			else if ( stats[stat].equalsIgnoreCase( "moxie" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getBaseMoxie() );
			else
				throw new RuntimeException( "Internal Error: unknown stat" );
		}

		private ScriptValue executeBuffedStatRequest( int stat ) throws AdvancedScriptException
		{
			if ( stats[stat].equalsIgnoreCase( "muscle" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getAdjustedMuscle() );
			else if ( stats[stat].equalsIgnoreCase( "mysticality" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getAdjustedMysticality() );
			else if ( stats[stat].equalsIgnoreCase( "moxie" ) )
				return new ScriptValue( TYPE_INT, KoLCharacter.getAdjustedMoxie() );
			else
				throw new RuntimeException( "Internal Error: unknown stat" );
		}

		private ScriptValue executeMeatRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getAvailableMeat() );
		}

		private ScriptValue executeClosetMeatRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getClosetMeat() );
		}

		private ScriptValue executeAdventuresRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getAdventuresLeft() );
		}

		private ScriptValue executeInebrietyRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_INT, KoLCharacter.getInebriety() );
		}

		private ScriptValue executeHaveSkillRequest( int skillID ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_BOOLEAN, KoLCharacter.hasSkill( skillID ) ? 1 : 0 );
		}

		private ScriptValue executeHaveEffectRequest( int effectID ) throws AdvancedScriptException
		{
			List potentialEffects;
			AdventureResult effect;

			potentialEffects = StatusEffectDatabase.getMatchingNames( StatusEffectDatabase.getEffectName( effectID ) );
			if ( potentialEffects.isEmpty() )
				effect = null;
			else
				effect = new AdventureResult(  (String) potentialEffects.get(0), 0, true );
			return new ScriptValue( TYPE_INT, effect == null ? 0 : effect.getCount( KoLCharacter.getEffects() ) );
		}

		private ScriptValue executeUseSkillRequest( int amount, int skillID ) throws AdvancedScriptException
		{
			client.makeRequest( new UseSkillRequest( client, ClassSkillsDatabase.getSkillName( skillID ), null, amount ), 1 );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeItemConditionRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "conditions add " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_VOID );
		}

		private ScriptValue executeCanEatRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_BOOLEAN, KoLCharacter.canEat() ? 1 : 0 );
		}

		private ScriptValue executeCanDrinkRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_BOOLEAN, KoLCharacter.canDrink() ? 1 : 0 );
		}

		private ScriptValue executeCanInteractRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_BOOLEAN, KoLCharacter.canInteract() ? 1 : 0 );
		}

		private ScriptValue executeHermitRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "hermit " + amount + " " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeBountyHunterRequest( int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "hunter " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeTrapperRequest( int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "trapper " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeEquipRequest( int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "equip " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeUnequipRequest( int itemID ) throws AdvancedScriptException
		{
			if( !(itemID == -1))
				DEFAULT_SHELL.executeLine( "unequip " + TradeableItemDatabase.getItemName( itemID ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeMyFamiliarRequest() throws AdvancedScriptException
		{
			if ( KoLCharacter.getFamiliar().getID() == -1 )
				return new ScriptValue( TYPE_FAMILIAR, "none" );
			else
				return new ScriptValue( TYPE_FAMILIAR, KoLCharacter.getFamiliar().getRace() );
		}

		private ScriptValue executeEquipFamiliarRequest( int familiarID ) throws AdvancedScriptException
		{
			if( familiarID == -1 )
			{
				client.makeRequest( new FamiliarRequest( client, FamiliarData.NO_FAMILIAR ), 1 );
			}
			else
			{
				DEFAULT_SHELL.executeLine( "familiar " + FamiliarsDatabase.getFamiliarName( familiarID ) );
			}
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );

		}

		private ScriptValue executeCouncilRequest() throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "council" );
			return new ScriptValue( TYPE_VOID );
		}

		private ScriptValue executeMindControlRequest( int setting ) throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "mind-control " + setting );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeNameRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, KoLCharacter.getUsername());
		}

		private ScriptValue executeChefRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_BOOLEAN, KoLCharacter.hasChef() ? 1 : 0);
		}

		private ScriptValue executeBartenderRequest() throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_BOOLEAN, KoLCharacter.hasBartender() ? 1 : 0);
		}

		private ScriptValue executeCLIExecuteRequest( String s ) throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( s );

			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeBountyHunterWantsRequest( int itemID ) throws AdvancedScriptException
		{
			if( ( itemID == -1 ) )
				return new ScriptValue( TYPE_BOOLEAN, 1 );
			if ( StaticEntity.getClient().hunterItems.isEmpty() )
				(new BountyHunterRequest( StaticEntity.getClient() )).run();

			for ( int i = 0; i < StaticEntity.getClient().hunterItems.size(); ++i )
				if ( ((String)StaticEntity.getClient().hunterItems.get(i)).equalsIgnoreCase( TradeableItemDatabase.getItemName( itemID ) ))
					return new ScriptValue( TYPE_BOOLEAN, 1 );

			return new ScriptValue( TYPE_BOOLEAN, 0 );
		}

		private ScriptValue executeBooleanToStringRequest( int value ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, value == 1 ? "true" : "false" );
		}

		private ScriptValue executeIntToStringRequest( int value ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, Integer.toString( value ) );
		}

		private ScriptValue executeFloatToStringRequest( double value ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, Double.toString( value ) );
		}

		private ScriptValue executeItemToStringRequest( int itemID ) throws AdvancedScriptException
		{
			if( itemID == -1)
				return new ScriptValue( TYPE_STRING, "none" );
			return new ScriptValue( TYPE_STRING, TradeableItemDatabase.getItemName( itemID ) );
		}

		private ScriptValue executeZodiacToStringRequest( int zodiacID ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, zodiacs[zodiacID] );
		}

		private ScriptValue executeLocationToStringRequest( KoLAdventure location ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, location.toString() );
		}

		private ScriptValue executeClassToStringRequest( int classID ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, classes[classID] );
		}

		private ScriptValue executeStatToStringRequest( int statID ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, stats[statID] );
		}

		private ScriptValue executeSkillToStringRequest( int skillID ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, ClassSkillsDatabase.getSkillName( skillID ) );
		}

		private ScriptValue executeEffectToStringRequest( int effectID ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, StatusEffectDatabase.getEffectName( effectID ) );
		}

		private ScriptValue executeFamiliarToStringRequest( int familiarID ) throws AdvancedScriptException
		{
			return new ScriptValue( TYPE_STRING, FamiliarsDatabase.getFamiliarName( familiarID ) );
		}

		private ScriptValue executeWaitRequest( int seconds ) throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "wait " + seconds );
			return new ScriptValue( TYPE_VOID );
		}

		private ScriptValue executeEntrywayRequest() throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "entryway" );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeHedgemazeRequest() throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "hedgemaze" );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeGuardiansRequest() throws AdvancedScriptException
		{
			int itemID;

			itemID = SorceressLair.fightTowerGuardians();
			return new ScriptValue( TYPE_ITEM, itemID == -1 ? "none" : TradeableItemDatabase.getItemName( itemID ) );
		}

		private ScriptValue executeChamberRequest() throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "chamber" );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeNemesisRequest() throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "nemesis" );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeGuildRequest() throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "guild" );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeGourdRequest() throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "gourd" );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeTavernRequest() throws AdvancedScriptException
		{
			int result;

			result = StaticEntity.getClient().locateTavernFaucet();
			return new ScriptValue( TYPE_INT, client.permitsContinue() ? result : -1 );
		}

		private ScriptValue executeTrainFamiliarRequest( int amount, String trainType ) throws AdvancedScriptException
		{
			DEFAULT_SHELL.executeLine( "train " + trainType + " " + amount );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}

		private ScriptValue executeRetrieveItemRequest( int amount, int itemID ) throws AdvancedScriptException
		{
			AdventureDatabase.retrieveItem( new AdventureResult( itemID, amount ) );
			return new ScriptValue( TYPE_BOOLEAN, client.permitsContinue() ? 1 : 0 );
		}
	}

	class ScriptFunctionList extends ScriptList
	{
	}

	class ScriptVariable extends ScriptListNode
	{
		String name;

		ScriptValue	content;

		public ScriptVariable( ScriptType type )
		{
			this.name = null;
			content = new ScriptValue( type );
		}


		public ScriptVariable( String name, ScriptType type )
		{
			this.name = name;
			content = new ScriptValue( type );
		}

		public int compareTo( Object o ) throws ClassCastException
		{
			if (!(o instanceof ScriptVariable ) )
				throw new ClassCastException();
			if ( name == null )
				return 1;
			return name.compareTo( ((ScriptVariable)o).name );

		}

		public ScriptType getType()
		{
			return content.getType();
		}

		public String getName()
		{
			return name;
		}

		public ScriptValue getValue()
		{
			return content;
		}

		public int getIntValue()
		{
			return content.getIntValue();
		}

		public String getStringValue()
		{
			return content.getStringValue();
		}

		public KoLAdventure getLocation()
		{
			return content.getLocation();
		}

		public double getFloatValue()
		{
			return content.getFloatValue();
		}

		public void setValue( ScriptValue targetValue ) throws AdvancedScriptException
		{
			if ( !getType().equals( targetValue.getType() ) )
			{
				if ( getType().equals( TYPE_INT ) && targetValue.getType().equals( TYPE_FLOAT ) )
				{
					content = targetValue.toInt();
				}
				else if ( getType().equals( TYPE_FLOAT ) && targetValue.getType().equals( TYPE_INT ) )
				{
					content = targetValue.toFloat();
				}
				else
					throw new RuntimeException( "Internal error: Cannot assign " + targetValue.getType().toString() + " to " + getType().toString() );
			}
			content = targetValue;
		}
	}

	class ScriptVariableList extends ScriptList
	{
		public ScriptVariable getFirstVariable()
		{
			return ( ScriptVariable ) getFirstElement();
		}

		public ScriptVariable getNextVariable( ScriptVariable current )
		{
			return ( ScriptVariable ) getNextElement( current );
		}
	}

	class ScriptVariableReference extends ScriptValue
	{
		ScriptVariable target;

		public ScriptVariableReference( ScriptVariable target )
		{
			this.target = target;
		}

		public ScriptVariableReference( String varName, ScriptScope scope ) throws AdvancedScriptException
		{
			target = findVariable( varName, scope );
		}

		private ScriptVariable findVariable( String name, ScriptScope scope ) throws AdvancedScriptException
		{
			ScriptVariable current;

			do
			{
				for (current = scope.getFirstVariable(); current != null; current = scope.getNextVariable( current ) )
				{
					if ( current.getName().equalsIgnoreCase( name ) )
						{
						return current;
						}
				}
				scope = scope.getParentScope();
			} while ( scope != null );

			throw new AdvancedScriptException( "Undefined variable " + name + " " + getLineAndFile() );
		}

		public ScriptType getType()
		{
			return target.getType();
		}

		public String getName()
		{
			return target.getName();
		}


		public int compareTo( Object o ) throws ClassCastException
		{
			if (!(o instanceof ScriptVariableReference ) )
				throw new ClassCastException();
			return target.getName().compareTo( ((ScriptVariableReference)o).target.getName() );

		}

		public ScriptValue execute()
		{
			return target.getValue();
		}

		public void setValue( ScriptValue targetValue ) throws AdvancedScriptException
		{
			target.setValue( targetValue );
		}
	}

	class ScriptVariableReferenceList extends ScriptList
	{
		public boolean addElement( ScriptListNode n )
		{
			return addElementSerial( n );
		}
	}

	class ScriptCommand extends ScriptListNode
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
		{
			this.command = command;
		}

		public int compareTo( Object o ) throws ClassCastException
		{
			if (!(o instanceof ScriptCommand ) )
				throw new ClassCastException();
			return 0;

		}

		public String toString()
		{
			if ( this.command == COMMAND_BREAK )
				return "break";
			else if ( this.command == COMMAND_CONTINUE )
				return "continue";
			else if ( this.command == COMMAND_EXIT )
				return "exit";
			return "<unknown command>";
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( this.command == COMMAND_BREAK )
			{
				currentState = STATE_BREAK;
				return null;
			}
			else if ( this.command == COMMAND_CONTINUE )
			{
				currentState = STATE_CONTINUE;
				return null;
			}
			else if ( this.command == COMMAND_EXIT )
			{
				currentState = STATE_EXIT;
				return null;
			}
			throw new RuntimeException( "Internal error: unknown ScriptCommand type" );

		}
	}

	class ScriptCommandList extends ScriptList
	{

		public ScriptCommandList()
		{
		}

		public ScriptCommandList( ScriptCommand c )
		{
			super( c );
		}

		public boolean addElement( ScriptListNode n ) //Command List has to remain in original order, so override addElement
		{
			return addElementSerial( n );
		}
	}

	class ScriptReturn extends ScriptCommand
	{
		private ScriptExpression returnValue;
		private ScriptType expectedType;

		public ScriptReturn( ScriptExpression returnValue, ScriptType expectedType ) throws AdvancedScriptException
		{
			this.returnValue = returnValue;
			if ( !( expectedType == null ) && !(returnValue == null ) && !returnValue.getType().equals( expectedType ) )
			{
				if ( returnValue.getType().equals( TYPE_INT ) && expectedType.equals( TYPE_FLOAT ) )
					;
				else if ( returnValue.getType().equals( TYPE_FLOAT ) && expectedType.equals( TYPE_INT ) )
					;
				else
					throw new AdvancedScriptException( "Cannot apply " + returnValue.getType().toString() + " to " + expectedType.toString() + " " + getLineAndFile() );
			}
			this.expectedType = expectedType;
		}

		public ScriptType getType()
		{
			if ( expectedType != null )
				return expectedType;
			if ( returnValue == null )
				return new ScriptType( TYPE_VOID );
			return returnValue.getType();
		}

		public ScriptExpression getExpression()
		{
			return returnValue;
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptValue result;

			if ( currentState != STATE_EXIT )
				currentState = STATE_RETURN;

			if ( returnValue == null )
				return null;

			result = returnValue.execute();
			if ( result == null )
				return null;
			if ( returnValue.getType().equals( TYPE_INT ) && expectedType != null && expectedType.equals( TYPE_FLOAT ) )
				return result.toFloat();
			if ( returnValue.getType().equals( TYPE_FLOAT ) && expectedType != null && expectedType.equals( TYPE_INT ) )
				return result.toInt();
			return result;
		}
	}


	class ScriptLoop extends ScriptCommand
	{
		private boolean repeat;
		private ScriptExpression condition;
		private ScriptScope scope;
		private ScriptLoopList elseLoops;

		public ScriptLoop( ScriptScope scope, ScriptExpression condition, boolean repeat ) throws AdvancedScriptException
		{
			this.scope = scope;
			this.condition = condition;
			if ( !( condition.getType().equals( TYPE_BOOLEAN ) ) )
				throw new AdvancedScriptException( "Cannot apply " + condition.getType().toString() + " to boolean " + getLineAndFile() );
			this.repeat = repeat;
			elseLoops = new ScriptLoopList();
		}

		public boolean repeats()
		{
			return repeat;
		}

		public ScriptExpression getCondition()
		{
			return condition;
		}

		public ScriptScope getScope()
		{
			return scope;
		}

		public ScriptLoop getFirstElseLoop()
		{
			return ( ScriptLoop ) elseLoops.getFirstElement();
		}

		public ScriptLoop getNextElseLoop( ScriptLoop current )
		{
			return ( ScriptLoop ) elseLoops.getNextElement( current );
		}


		public void addElseLoop( ScriptLoop elseLoop ) throws AdvancedScriptException
		{
			if ( repeat == true )
				throw new AdvancedScriptException( "Else without if " + getLineAndFile() );
			elseLoops.addElement( elseLoop );
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptValue result;
			boolean conditionMet = (condition.execute().getIntValue() == 1 );
			DEFAULT_SHELL.updateDisplay( CONTINUE_STATE, "" );

			if ( conditionMet )
			{
				do
				{
					result = scope.execute();
					if ( currentState == STATE_BREAK )
					{
						if ( repeat )
							currentState = STATE_NORMAL;

						return null;
					}
					if ( currentState == STATE_CONTINUE )
					{
						if ( !repeat )
							return null;

						currentState = STATE_NORMAL;
					}
					if ( currentState == STATE_RETURN )
					{
						return result;
					}
					if ( currentState == STATE_EXIT )
					{
						return null;
					}
					if ( !repeat )
						break;
				}
				while ( condition.execute().getIntValue() == 1 );
			}
			else
			{
				for ( ScriptLoop elseLoop = elseLoops.getFirstScriptLoop(); elseLoop != null; elseLoop = elseLoops.getNextScriptLoop( elseLoop ) )
				{
					result = elseLoop.execute();
					if ( currentState != STATE_NORMAL )
						return result;
				}
			}

			return null;
		}
	}


	class ScriptLoopList extends ScriptList
	{
		public ScriptLoop getFirstScriptLoop()
		{
			return ( ScriptLoop ) getFirstElement();
		}

		public ScriptLoop getNextScriptLoop( ScriptLoop current )
		{
			return ( ScriptLoop ) getNextElement( current );
		}

		public boolean addElement( ScriptListNode n )
		{
			return addElementSerial( n );
		}
	}

	class ScriptCall extends ScriptValue
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
		{
			return target;
		}

		public ScriptExpression getFirstParam()
		{
			return ( ScriptExpression ) params.getFirstElement();
		}

		public ScriptExpression getNextParam( ScriptExpression current )
		{
			return ( ScriptExpression ) params.getNextElement( current );
		}

		public ScriptType getType()
		{
			return target.getType();
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			ScriptVariableReference		paramVarRef;
			ScriptExpression		paramValue;
			for
			(
				paramVarRef = target.getFirstParam(), paramValue = params.getFirstExpression();
				paramVarRef != null;
				paramVarRef = target.getNextParam( paramVarRef ), paramValue = params.getNextExpression( paramValue )
			 )
			{
				if ( paramVarRef == null )
					throw new RuntimeException( "Internal error: illegal arguments." );
				paramVarRef.setValue( paramValue.execute() );
				if ( currentState == STATE_EXIT )
					return null;
			}
			if ( paramValue != null )
				throw new RuntimeException( "Internal error: illegal arguments." );

			return target.execute();
		}
	}

	class ScriptAssignment extends ScriptCommand
	{
		private ScriptVariableReference leftHandSide;
		private ScriptExpression rightHandSide;

		public ScriptAssignment( ScriptVariableReference leftHandSide, ScriptExpression rightHandSide ) throws AdvancedScriptException
		{
			this.leftHandSide = leftHandSide;
			this.rightHandSide = rightHandSide;
			if ( !leftHandSide.getType().equals( rightHandSide.getType() ) )
			{
				if ( leftHandSide.getType().equals( TYPE_INT ) && rightHandSide.getType().equals( TYPE_FLOAT ) )
					;
				else if ( leftHandSide.getType().equals( TYPE_FLOAT ) && rightHandSide.getType().equals( TYPE_INT ) )
					;
				else
					throw new AdvancedScriptException( "Cannot apply " + rightHandSide.getType().toString() + " to " + leftHandSide.toString() + " " + getLineAndFile() );
			}
		}

		public ScriptVariableReference getLeftHandSide()
		{
			return leftHandSide;
		}

		public ScriptExpression getRightHandSide()
		{
			return rightHandSide;
		}

		public ScriptType getType()
		{
			return leftHandSide.getType();
		}

		public ScriptValue execute() throws AdvancedScriptException
		{
			if ( leftHandSide.getType().equals( TYPE_INT ) && rightHandSide.getType().equals( TYPE_FLOAT ) )
				leftHandSide.setValue( rightHandSide.execute().toInt() );
			else if ( leftHandSide.getType().equals( TYPE_FLOAT ) && rightHandSide.getType().equals( TYPE_INT ) )
				leftHandSide.setValue( rightHandSide.execute().toFloat() );
			else
				leftHandSide.setValue( rightHandSide.execute() );
			return null;
		}

	}

	class ScriptType
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
			if( type == TYPE_EFFECT )
				return "effect";
			if( type == TYPE_FAMILIAR )
				return "familiar";
			return "unknown type";
		}

	}

	class ScriptValue extends ScriptExpression
	{
		ScriptType type;

		int contentInt = 0;
		float contentFloat = 0.0f;
		String contentString = null;
		Object content = null;

		public ScriptValue()
		{
			//stub constructor for subclasses
			//should not be called
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
		{
			this.type = new ScriptType( type );
			this.contentInt = contentInt;
			fillContent();
		}


		public ScriptValue( ScriptType type, int contentInt ) throws AdvancedScriptException
		{
			this.type = type;
			this.contentInt = contentInt;
			fillContent();
		}


		public ScriptValue( int type, String contentString ) throws AdvancedScriptException
		{
			this.type = new ScriptType( type );
			this.contentString = contentString;
			fillContent();
		}

		public ScriptValue( ScriptType type, String contentString ) throws AdvancedScriptException
		{
			this.type = type;
			this.contentString = contentString;
			fillContent();
		}

		public ScriptValue( int type, float content ) throws AdvancedScriptException
		{
			if ( type != TYPE_FLOAT )
				throw new AdvancedScriptException( "Internal error: cannot assign float value to non-float" );
			this.type = new ScriptType( TYPE_FLOAT );
			this.contentFloat = content;
		}

		public ScriptValue( ScriptValue original )
		{
			this.type = original.type;
			this.contentInt = original.contentInt;
			this.contentString = original.contentString;
			this.content = original.content;
		}

		public int compareTo( Object o ) throws ClassCastException
		{
			if (!(o instanceof ScriptValue ) )
				throw new ClassCastException();
			return 0;

		}

		public ScriptValue toFloat() throws AdvancedScriptException
		{
			if ( type.equals( TYPE_FLOAT ) )
				return this;
			else
				return new ScriptValue( TYPE_FLOAT, (float ) contentInt );
		}

		public ScriptValue toInt() throws AdvancedScriptException
		{
			if ( type.equals( TYPE_INT ) )
				return this;
			else
				return new ScriptValue( TYPE_INT, (int ) contentFloat );
		}

		public ScriptType getType()
		{
			return type;
		}

		public String toString()
		{
			if ( contentString != null )
				return contentString;
			else
				return Integer.toString( contentInt );
		}

		public int getIntValue()
		{
			return contentInt;
		}

		public String getStringValue()
		{
			return contentString;
		}

		public float getFloatValue()
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
				if( contentString.equalsIgnoreCase( "none" ))
				{
					contentInt = -1;
					return;
				}

				// Allow for an item number to be specified inside
				// of the "item" construct.

				for ( int i = 0; i < contentString.length(); ++i )
				{
					if ( !Character.isDigit( contentString.charAt(i) ) )
					{
						// If you get an actual item number, then store it inside
						// of contentInt and return from the method.

						if ( (contentInt = TradeableItemDatabase.getItemID( contentString )) != -1 )
							return;

						// Otherwise, throw an AdvancedScriptException so that
						// an unsuccessful parse happens before the script gets
						// executed (consistent with paradigm).

						throw new AdvancedScriptException( "Item " + contentString + " not found in database " + getLineAndFile() );
					}
				}

				// Since it is numeric, parse the integer value
				// and store it inside of the contentInt.

				contentInt = Integer.parseInt( contentString );
				return;
			}
			else if ( type.equals( TYPE_ZODIAC ) )
			{
				for ( int i = 0; ; i++ )
				{
					if ( i == Array.getLength( zodiacs ) )
						throw new AdvancedScriptException( "Unknown zodiac " + contentString + " " + getLineAndFile() );
					if ( contentString.equalsIgnoreCase( zodiacs[i] ) )
					{
						contentInt = i;
						break;
					}
				}
			}
			else if ( type.equals( TYPE_LOCATION ) )
			{
				if ( (content = AdventureDatabase.getAdventure( contentString )) == null )
					throw new AdvancedScriptException( "Location " + contentString + " not found in database " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_CLASS ) )
			{
				for ( int i = 0; i < classes.length; i++ )
				{
					if ( contentString.equalsIgnoreCase( classes[i] ) )
					{
						contentInt = i;
						return;
					}
				}

				throw new AdvancedScriptException( "Unknown class " + contentString + " " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_STAT ) )
			{
				for ( int i = 0; i < stats.length; i++ )
				{
					if ( contentString.equalsIgnoreCase( stats[i] ) )
					{
						contentInt = i;
						return;
					}
				}

				throw new AdvancedScriptException( "Unknown stat " + contentString + " " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_SKILL ) )
			{
				if ( (contentInt = ClassSkillsDatabase.getSkillID( contentString )) == -1 )
					throw new AdvancedScriptException( "Skill " + contentString + " not found in database " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_EFFECT ) )
			{
				if ( (contentInt = StatusEffectDatabase.getEffectID( contentString )) == -1 )
					throw new AdvancedScriptException( "Effect " + contentString + " not found in database " + getLineAndFile() );
			}
			else if ( type.equals( TYPE_FAMILIAR ) )
			{
				if ( contentString.equalsIgnoreCase( "none" ) )
					contentInt = -1;
				else if ( (contentInt = FamiliarsDatabase.getFamiliarID( contentString )) == -1 )
					throw new AdvancedScriptException( "Familiar " + contentString + " not found in database " + getLineAndFile() );
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

	class ScriptExpression extends ScriptCommand
	{
		ScriptExpression lhs;
		ScriptExpression rhs;
		ScriptOperator oper;

		public ScriptExpression(ScriptExpression lhs, ScriptExpression rhs, ScriptOperator oper ) throws AdvancedScriptException
		{
			this.lhs = lhs;
			this.rhs = rhs;
			if (( rhs != null ) && !lhs.getType().equals( rhs.getType() ) )
			{
				if ( lhs.getType().equals( TYPE_INT ) && rhs.getType().equals( TYPE_FLOAT ) )
					;
				else if ( lhs.getType().equals( TYPE_FLOAT ) && rhs.getType().equals( TYPE_INT ) )
					;
				else
					throw new AdvancedScriptException( "Cannot apply " + lhs.getType().toString() + " to " + rhs.getType().toString() + " " + getLineAndFile() );
			}
			this.oper = oper;
		}


		public ScriptExpression()
		{
			//stub constructor for subclasses
			//should not be called
		}

		public ScriptType getType()
		{
			if ( oper.isBool() )
				return new ScriptType( TYPE_BOOLEAN );
			if ( lhs.getType().equals( TYPE_FLOAT ) ) // int (oper ) float evaluates to float.
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
				return oper.applyTo(lhs, rhs );
			}
			catch( AdvancedScriptException e )
			{
				throw new RuntimeException( "AdvancedScriptException in execution - should occur only during parsing." );
			}
		}

	}

	class ScriptExpressionList extends ScriptList
	{
		public ScriptExpression getFirstExpression()
		{
			return (ScriptExpression) getFirstElement();
		}

		public ScriptExpression getNextExpression( ScriptExpression current )
		{
			return (ScriptExpression) getNextElement( current );
		}


		public boolean addElement( ScriptListNode n ) //Expression List has to remain in original order, so override addElement
		{
			return addElementSerial( n );
		}
	}

	class ScriptOperator
	{
		String operString;

		public ScriptOperator( String oper )
		{
			if ( oper == null )
				throw new RuntimeException( "Internal error in ScriptOperator()" );
			operString = oper;
		}

		public boolean precedes( ScriptOperator oper )
		{
			return operStrength() > oper.operStrength();
		}

		private int operStrength()
		{
			if ( operString.equalsIgnoreCase( "!" ) )
				return 6;
			if ( operString.equalsIgnoreCase( "*" ) || operString.equalsIgnoreCase( "/" ) || operString.equalsIgnoreCase( "%" ) )
				return 5;
			else if ( operString.equalsIgnoreCase( "+" ) || operString.equalsIgnoreCase( "-" ) )
				return 4;
			else if ( operString.equalsIgnoreCase( "<" ) || operString.equalsIgnoreCase( ">" ) || operString.equalsIgnoreCase( "<=" ) || operString.equalsIgnoreCase( ">=" ) )
				return 3;
			else if ( operString.equalsIgnoreCase( "==" ) || operString.equalsIgnoreCase( "!=" ) )
				return 2;
			else if ( operString.equalsIgnoreCase( "||" ) || operString.equalsIgnoreCase( "&&" ) )
				return 1;
			else
				return -1;
		}

		public boolean isBool()
		{
			if
			(
				operString.equalsIgnoreCase( "*" ) || operString.equalsIgnoreCase( "/" ) || operString.equalsIgnoreCase( "%" ) ||
				operString.equalsIgnoreCase( "+" ) || operString.equalsIgnoreCase( "-" )
			 )
				return false;
			else
				return true;

		}

		public String toString()
		{
			return operString;
		}

		public ScriptValue applyTo( ScriptExpression lhs, ScriptExpression rhs ) throws AdvancedScriptException
		{

			ScriptValue leftResult = lhs.execute();
			ScriptValue rightResult;

			if ( currentState == STATE_EXIT )
				return null;

			if ( rhs != null && !rhs.getType().equals( lhs.getType() ) ) //float-check values
			{
				if ( lhs.getType().equals( TYPE_INT ) && rhs.getType().equals( TYPE_FLOAT ) )
					;
				else if ( lhs.getType().equals( TYPE_FLOAT ) && rhs.getType().equals( TYPE_INT ) )
					;
				else
					throw new RuntimeException( "Internal error: left hand side and right hand side do not correspond" );
			}

			if ( operString.equalsIgnoreCase( "!" ) )
			{
				if ( leftResult.getIntValue() == 0 )
					return new ScriptValue( TYPE_BOOLEAN, 1 );
				else
					return new ScriptValue( TYPE_BOOLEAN, 0 );
			}
			if ( operString.equalsIgnoreCase( "*" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
					return new ScriptValue( TYPE_FLOAT, leftResult.toFloat().getFloatValue() * rightResult.toFloat().getFloatValue() );
				else
					return new ScriptValue( TYPE_INT, leftResult.getIntValue() * rightResult.getIntValue() );
			}
			if ( operString.equalsIgnoreCase( "/" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
					return new ScriptValue( TYPE_FLOAT, leftResult.toFloat().getFloatValue() / rightResult.toFloat().getFloatValue() );
				else
					return new ScriptValue( TYPE_INT, leftResult.getIntValue() / rightResult.getIntValue() );
			}
			if ( operString.equalsIgnoreCase( "%" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
					return new ScriptValue( TYPE_FLOAT, leftResult.toFloat().getFloatValue() % rightResult.toFloat().getFloatValue() );
				else
					return new ScriptValue( TYPE_INT, leftResult.getIntValue() % rightResult.getIntValue() );
			}
			if ( operString.equalsIgnoreCase( "+" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_STRING ) )
					return new ScriptValue( TYPE_STRING, leftResult.getStringValue() + rightResult.getStringValue() );
				else
				{
					if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
						return new ScriptValue( TYPE_FLOAT, leftResult.toFloat().getFloatValue() + rightResult.toFloat().getFloatValue() );
					else
						return new ScriptValue( TYPE_INT, leftResult.getIntValue() + rightResult.getIntValue() );
				}
			}
			if ( operString.equalsIgnoreCase( "-" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
					return new ScriptValue( TYPE_FLOAT, leftResult.toFloat().getFloatValue() - rightResult.toFloat().getFloatValue() );
				else
					return new ScriptValue( TYPE_INT, leftResult.getIntValue() - rightResult.getIntValue() );
			}
			if ( operString.equalsIgnoreCase( "<" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
				{
					if ( leftResult.toFloat().getFloatValue() < rightResult.toFloat().getFloatValue() )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
				else
				{
					if ( leftResult.getIntValue() < rightResult.getIntValue() )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
			}
			if ( operString.equalsIgnoreCase( ">" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
				{
					if ( leftResult.toFloat().getFloatValue() > rightResult.toFloat().getFloatValue() )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
				else
				{
					if ( leftResult.getIntValue() > rightResult.getIntValue() )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
			}
			if ( operString.equalsIgnoreCase( "<=" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
				{
					if ( leftResult.toFloat().getFloatValue() <= rightResult.toFloat().getFloatValue() )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
				else
				{
					if ( leftResult.getIntValue() <= rightResult.getIntValue() )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
			}
			if ( operString.equalsIgnoreCase( ">=" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
				{
					if ( leftResult.toFloat().getFloatValue() >= rightResult.toFloat().getFloatValue() )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
				else
				{
					if ( leftResult.getIntValue() >= rightResult.getIntValue() )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
			}
			if ( operString.equalsIgnoreCase( "==" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if
				(
					lhs.getType().equals( TYPE_INT ) ||
					lhs.getType().equals( TYPE_FLOAT ) ||
					lhs.getType().equals( TYPE_BOOLEAN ) ||
					lhs.getType().equals( TYPE_ITEM ) ||
					lhs.getType().equals( TYPE_ZODIAC ) ||
					lhs.getType().equals( TYPE_CLASS ) ||
					lhs.getType().equals( TYPE_SKILL ) ||
					lhs.getType().equals( TYPE_EFFECT ) ||
					lhs.getType().equals( TYPE_STAT ) ||
					lhs.getType().equals( TYPE_FAMILIAR )
				 )
				{
					if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
					{
						if ( leftResult.toFloat().getFloatValue() == rightResult.toFloat().getFloatValue() )
							return new ScriptValue( TYPE_BOOLEAN, 1 );
						else
							return new ScriptValue( TYPE_BOOLEAN, 0 );
					}
					else
					{
						if ( leftResult.getIntValue() == rightResult.getIntValue() )
							return new ScriptValue( TYPE_BOOLEAN, 1 );
						else
							return new ScriptValue( TYPE_BOOLEAN, 0 );
					}
				}
				else
				{
					if ( leftResult.getStringValue().equalsIgnoreCase( rightResult.getStringValue() ) )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
			}
			if ( operString.equalsIgnoreCase( "!=" ) )
			{
				rightResult = rhs.execute();
				if ( currentState == STATE_EXIT )
					return null;
				if
				(
					lhs.getType().equals( TYPE_INT ) ||
					lhs.getType().equals( TYPE_FLOAT ) ||
					lhs.getType().equals( TYPE_BOOLEAN ) ||
					lhs.getType().equals( TYPE_ITEM ) ||
					lhs.getType().equals( TYPE_ZODIAC ) ||
					lhs.getType().equals( TYPE_CLASS ) ||
					lhs.getType().equals( TYPE_SKILL ) ||
					lhs.getType().equals( TYPE_EFFECT ) ||
					lhs.getType().equals( TYPE_STAT ) ||
					lhs.getType().equals( TYPE_FAMILIAR )
				 )
				{
					if ( lhs.getType().equals( TYPE_FLOAT ) || rhs.getType().equals( TYPE_FLOAT ) )
					{
						if ( leftResult.toFloat().getFloatValue() != rightResult.toFloat().getFloatValue() )
							return new ScriptValue( TYPE_BOOLEAN, 1 );
						else
							return new ScriptValue( TYPE_BOOLEAN, 0 );
					}
					else
					{
						if ( leftResult.getIntValue() != rightResult.getIntValue() )
							return new ScriptValue( TYPE_BOOLEAN, 1 );
						else
							return new ScriptValue( TYPE_BOOLEAN, 0 );
					}
				}
				else
				{
					if ( !leftResult.getStringValue().equalsIgnoreCase( rightResult.getStringValue() ) )
						return new ScriptValue( TYPE_BOOLEAN, 1 );
					else
						return new ScriptValue( TYPE_BOOLEAN, 0 );
				}
			}
			if ( operString.equalsIgnoreCase( "||" ) )
			{
				if ( leftResult.getIntValue() == 1 )
					return new ScriptValue( TYPE_BOOLEAN, 1 );
				else
					return rhs.execute();
			}
			if ( operString.equalsIgnoreCase( "&&" ) )
			{
				if ( leftResult.getIntValue() == 0 )
					return new ScriptValue( TYPE_BOOLEAN, 0 );
				else
					return rhs.execute();
			}
			throw new RuntimeException( "Internal error: illegal operator." );
		}
	}


	class ScriptListNode implements Comparable
	{
		ScriptListNode next = null;

		public ScriptListNode()
		{
		}

		public ScriptListNode getNext()
		{
			return next;
		}

		public void setNext( ScriptListNode node )
		{
			next = node;
		}

		public int compareTo( Object o ) throws ClassCastException
		{
			if (!(o instanceof ScriptListNode ) )
				throw new ClassCastException();

			return 0; //This should not happen since each extending class overrides this function

		}

	}

	class ScriptList
	{
		ScriptListNode firstNode;

		public ScriptList()
		{
			firstNode = null;
		}

		public ScriptList( ScriptListNode node )
		{
			firstNode = node;
		}

		public boolean addElement( ScriptListNode n )
		{
			ScriptListNode current;
			ScriptListNode previous = null;

			if ( firstNode == null )
				{
				firstNode = n;
				n.setNext( null );
				return true;
				}
			for ( current = firstNode; current != null; previous = current, current = current.getNext() )
			{
				if ( current.compareTo( n ) <= 0 )
					break;
			}
			if ( current != null && current.compareTo( n ) == 0 )
			{
				return false;
			}
			if ( previous == null ) //Insert in front of very first element
			{
				firstNode = n;
				firstNode.setNext( current );
			}
			else
			{
				previous.setNext( n );
				n.setNext( current );
			}
			return true;
		}

		public boolean addElementSerial( ScriptListNode n ) //Function for subclasses to override addElement with
		{
			ScriptListNode current;
			ScriptListNode previous = null;

			if ( firstNode == null )
			{
				firstNode = n;
				return true;
			}

			for ( current = firstNode; current != null; previous = current, current = current.getNext() )
				;

			previous.setNext( n );
			return true;
		}


		public ScriptListNode getFirstElement()
		{
			return firstNode;
		}

		public ScriptListNode getNextElement( ScriptListNode n )
		{
			return n.getNext();
		}

	}

	class AdvancedScriptException extends Exception
	{
		AdvancedScriptException( String s )
		{
			super( s );
		}
	}
}
