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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
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

/**
 * The main class for the <code>KoLmafia</code> package.  This
 * class encapsulates most of the data relevant to any given
 * session of <code>Kingdom of Loathing</code> and currently
 * functions as the blackboard in the architecture.  When data
 * listeners are implemented, it will continue to manage most
 * of the interactions.
 */

public class KoLmafiaASH
{
	/* Variables for Advanced Scripting */
	private int lineNumber = 0;

	public final static char[] tokenList = {' ', ',', '{', '}', '(', ')', '$', '!', '+', '-', '=', '"', '*', '/', '%', '[', ']', '!', ';'};
	public final static String[] multiCharTokenList = {"==", "!=", "<=", ">=", "||", "&&"};

	public static final int TYPE_VOID = 0;
	public static final int TYPE_BOOLEAN = 1;
	public static final int TYPE_INT = 2;
	public static final int TYPE_STRING = 3;

	public static final int TYPE_ITEM = 100;
	public static final int TYPE_ZODIAC = 101;
	public static final int TYPE_LOCATION = 102;
	public static final int TYPE_CLASS = 103;

	public static final int ZODIAC_WALLABY = 1;
	public static final int ZODIAC_MONGOOSE = 2;
	public static final int ZODIAC_VOLE = 3;
	public static final int ZODIAC_PLATYPUS = 4;
	public static final int ZODIAC_OPOSSUM = 5;
	public static final int ZODIAC_MARMOT = 6;
	public static final int ZODIAC_WOMBAT = 7;
	public static final int ZODIAC_BLENDER = 8;
	public static final int ZODIAC_PACKRAT = 9;
	public static final int ZODIAC_NONE = 10;

	public static final int CLASS_SEALCLUBBER = 1;
	public static final int CLASS_TURTLETAMER = 2;
	public static final int CLASS_PASTAMANCER = 3;
	public static final int CLASS_SAUCEROR = 4;
	public static final int CLASS_DISCOBANDIT = 5;
	public static final int CLASS_ACCORDIONTHIEF = 6;

	public static final int COMMAND_BREAK = 1;
	public static final int COMMAND_CONTINUE = 2;

	public static final int STATE_NORMAL = 1;
	public static final int STATE_RETURN = 2;
	public static final int STATE_BREAK = 3;
	public static final int STATE_CONTINUE = 4;
	public static final int STATE_EXIT = 5;

	public static int currentState = STATE_NORMAL;

	private static final String escapeString = "//";

	private static ScriptScope global;
	private static String line;
	private static String nextLine;

	public static LineNumberReader commandStream;

	public void execute( String parameters, KoLmafia scriptRequestor) throws IOException
	{
		commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( parameters ) ) );

		line = getNextLine();
		nextLine = getNextLine();

		try
		{
			global = parseScope( null, new ScriptVariableList(), ScriptExistingFunction.getExistingFunctionScope( scriptRequestor), false);
			if ( line != null )
				throw new AdvancedScriptException( "Script parsing error at line " + commandStream.getLineNumber());


			commandStream.close();

			printScope( global, 0 );

			currentState = STATE_NORMAL;
			ScriptValue result = executeGlobalScope( global);
			if(( result.getType().equals(TYPE_BOOLEAN)))
			{

				if( result.getIntValue() == 0)
					{
					KoLmafia.getLogStream().println( "Script failed!"); //failed
					scriptRequestor.updateDisplay( KoLmafia.NORMAL_STATE,  "Script failed!");
					return;
					}
				else
					{
					KoLmafia.getLogStream().println( "Script succeeded!"); //succes
					scriptRequestor.updateDisplay( KoLmafia.NORMAL_STATE,  "Script succeeded!");
					return;
					}
			}
			else
			{
				KoLmafia.getLogStream().println( "Script returned message " + result.toString());
				scriptRequestor.updateDisplay( KoLmafia.NORMAL_STATE,  "Script returned value " + result.toString());
				return;
			}

		}
		catch( AdvancedScriptException e)
		{
			commandStream.close();
			scriptRequestor.updateDisplay( KoLmafia.ERROR_STATE, e.getMessage() );
			scriptRequestor.cancelRequest();

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
				// and a non-blank line) or when you've reached EOF.

				line = commandStream.readLine();
			}
			while ( line != null && (line.trim().length() == 0 || line.startsWith( escapeString )) );

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




	private ScriptScope parseScope( ScriptType expectedType, ScriptVariableList variables, ScriptScope parentScope, boolean whileLoop) throws AdvancedScriptException
	{
		ScriptFunction f = null;
		ScriptVariable v = null;
		ScriptCommand c = null;
		ScriptType t = null;

		ScriptScope result = new ScriptScope( variables, parentScope);

		while( true)
		{
			if(( t = parseType()) == null)
			{
				if(( c = parseCommand( expectedType, result, false, whileLoop)) != null)
				{
					result.addCommand( c);
					continue;
				}
				else
				//No type and no call -> done.
					break;
			}
			if(( f = parseFunction( t, result)) != null)
			{
				if( !result.addFunction( f))
					throw new AdvancedScriptException( "Function " + f.getName() + " already defined at line " + commandStream.getLineNumber());
			}
			else if(( v = parseVariable( t)) != null)
			{
				if( !result.addVariable( v))
					throw new AdvancedScriptException( "Variable " + v.getName() + " already defined at line " + commandStream.getLineNumber());
				if( currentToken().equals(";"))
					readToken(); //read ;
				else
					throw new AdvancedScriptException( "';' Expected at line " + commandStream.getLineNumber());
			}
			else
				//Found a type but no function or variable to tie it to
				throw new AdvancedScriptException( "Script parse error at line " + commandStream.getLineNumber());
		}
		if( !result.assertReturn())
			{
			if( !( expectedType == null) && !expectedType.equals( TYPE_VOID) && !expectedType.equals(TYPE_BOOLEAN))
				throw new AdvancedScriptException( "Missing return value at line " + commandStream.getLineNumber());
			}
		return result;
	}

	private ScriptFunction parseFunction( ScriptType t, ScriptScope parentScope) throws AdvancedScriptException
	{
		String				functionName;
		ScriptFunction			result;
		ScriptType			paramType = null;
		ScriptVariable			param = null;
		ScriptVariable			paramNext = null;
		ScriptVariableList		paramList = null;
		ScriptVariableReference		paramRef = null;

		String lastParam = null;

		if( parseIdentifier( currentToken()))
			functionName = currentToken();
		else
			return null;
		if(( nextToken() == null) || (!nextToken().equals( "(")))
			return null;
		readToken(); //read Function name
		readToken(); //read (

		paramList = new ScriptVariableList();

		result = new ScriptFunction( functionName, t);
		while( !currentToken().equals( ")"))
			{
			if(( paramType = parseType()) == null)
				throw new AdvancedScriptException( " ')' Expected at line " + commandStream.getLineNumber());
			if(( param = parseVariable( paramType)) == null)
				throw new AdvancedScriptException( " Identifier expected at line " + commandStream.getLineNumber());
			if( !currentToken().equals( ")"))
				{
				if( !currentToken().equals( ","))
					throw new AdvancedScriptException( " ')' Expected at line " + commandStream.getLineNumber());
				readToken(); //read comma
				}
			paramRef = new ScriptVariableReference( param);
			result.addVariableReference( paramRef);
			if( !paramList.addElement( param))
				throw new AdvancedScriptException( "Variable " + param.getName() + " already defined at line " + commandStream.getLineNumber());
			}
		readToken(); //read )
		if( !currentToken().equals( "{")) //Scope is a single call
			{
			result.setScope( new ScriptScope( parseCommand( t, parentScope, false, false), parentScope));
			for( param = paramList.getFirstVariable(); param != null; param = paramNext)
				{
				paramNext = paramList.getNextVariable( param);
				lastParam = param.getName().toString();
				if( !result.getScope().addVariable( param))
					throw new AdvancedScriptException( "Variable " + param.getName() + " already defined at line " + commandStream.getLineNumber());
				}
			if( !result.getScope().assertReturn())
				throw new AdvancedScriptException( "Missing return value at line " + commandStream.getLineNumber());
			}
		else
			{
			readToken(); //read {
			result.setScope( parseScope( t, paramList, parentScope, false));
			if( !currentToken().equals( "}"))
				throw new AdvancedScriptException( " '}' Expected at line " + commandStream.getLineNumber());
			readToken(); //read }
			}

		return result;
	}

	private ScriptVariable parseVariable( ScriptType t)
	{
		ScriptVariable result;

		if( parseIdentifier( currentToken()))
			result = new ScriptVariable( currentToken(), t);
		else
			return null;
		readToken(); //If parsing of Identifier succeeded, go to next token.
		return result;
	}

	private ScriptCommand parseCommand( ScriptType functionType, ScriptScope scope, boolean noElse, boolean whileLoop) throws AdvancedScriptException
	{
		ScriptCommand result;

		if(( currentToken() != null) && ( currentToken().equals( "break")))
			{
			if( !whileLoop)
				throw new AdvancedScriptException( "break outside of loop at line " + commandStream.getLineNumber());
			result = new ScriptCommand( COMMAND_BREAK);
			readToken(); //break
			}

		else if(( currentToken() != null) && ( currentToken().equals( "continue")))
			{
			if( !whileLoop)
				throw new AdvancedScriptException( "break outside of loop at line " + commandStream.getLineNumber());
			result = new ScriptCommand( COMMAND_CONTINUE);
			readToken(); //continue
			}


		else if(( result = parseReturn( functionType, scope)) != null)
			;
		else if(( result = parseLoop( functionType, scope, noElse, whileLoop)) != null)
			//loop doesn't have a ; token
			return result;
		else if(( result = parseCall( scope)) != null)
			;
		else if(( result = parseAssignment( scope)) != null)
			;
		else
			return null;

		if(( currentToken() == null) || ( !currentToken().equals(";")))
			throw new AdvancedScriptException( "';' Expected at line " + commandStream.getLineNumber());
		readToken(); // ;

		return result;
	}

	private ScriptType parseType()
	{
		int type;

		if( currentToken() == null)
			return null;
		String typeString = currentToken();

		if( typeString.equals( "void"))
			type = TYPE_VOID;
		else if( typeString.equals( "boolean"))
			type = TYPE_BOOLEAN;
		else if( typeString.equals( "int"))
			type = TYPE_INT;
		else if( typeString.equals( "string"))
			type = TYPE_STRING;
		else if( typeString.equals( "item"))
			type = TYPE_ITEM;
		else if( typeString.equals( "zodiac"))
			type = TYPE_ZODIAC;
		else if( typeString.equals( "location"))
			type = TYPE_LOCATION;
		else if( typeString.equals( "class"))
			type = TYPE_CLASS;
		else
			return null;
		readToken();
		return new ScriptType( type);
	}

	private boolean parseIdentifier( String identifier)
	{
		if( !Character.isLetter( identifier.charAt( 0)) && (identifier.charAt( 0) != '_'))
	    	{
			return false;
		}
		for( int i = 1; i < identifier.length(); i++)
		{
			if( !Character.isLetterOrDigit( identifier.charAt( i)) && (identifier.charAt( i) != '_'))
    			{
				return false;
			}
		}


		return true;
	}



	private ScriptReturn parseReturn( ScriptType expectedType, ScriptScope parentScope) throws AdvancedScriptException
	{
			
		ScriptExpression expression = null;

		if(( currentToken() == null) || !( currentToken().equals( "return")))
			return null;
		readToken(); //return
		if(( currentToken() != null) && ( currentToken().equals(";")))
		{
			if(( expectedType != null) && expectedType.equals(TYPE_VOID))
			{
				return new ScriptReturn( null, new ScriptType( TYPE_VOID));
			}
			else
				throw new AdvancedScriptException( "Return needs value at line " + commandStream.getLineNumber());
		}
		else
		{
			if(( expression = parseExpression( parentScope)) == null)
				throw new AdvancedScriptException( "Expression expected at line " + commandStream.getLineNumber());
			return new ScriptReturn( expression, expectedType);
		}
	}


	private ScriptLoop parseLoop( ScriptType functionType, ScriptScope parentScope, boolean noElse, boolean loop) throws AdvancedScriptException
	{
		ScriptScope		scope;
		ScriptExpression	expression;
		ScriptLoop		result = null;
		ScriptLoop		currentLoop = null;
		ScriptCommand		command = null;
		boolean			repeat = false;
		boolean			elseFound = false;
		boolean			finalElse = false;

		if( currentToken() == null)
			return null;

		if( (currentToken().equals( "while") && ( repeat = true)) || currentToken().equals("if"))
		{

			if(( nextToken() == null) || ( !nextToken().equals("(")))
				throw new AdvancedScriptException( "'(' Expected at line " + commandStream.getLineNumber());
			readToken(); //if or while
			readToken(); //(
			expression = parseExpression( parentScope);
			if(( currentToken() == null) || ( !currentToken().equals(")")))
				throw new AdvancedScriptException( "')' Expected at line " + commandStream.getLineNumber());
			readToken(); //)

			do
			{
					

				if(( currentToken() == null) || ( !currentToken().equals( "{"))) //Scope is a single call
				{
					command = parseCommand( functionType, parentScope, !elseFound, (repeat || loop));
					scope = new ScriptScope( command, parentScope);
					if( result == null)
						result = new ScriptLoop( scope, expression, repeat);
				}
				else
				{
					readToken(); //read {
					scope = parseScope( functionType, null, parentScope, (repeat || loop));
					if(( currentToken() == null) || ( !currentToken().equals( "}")))
						throw new AdvancedScriptException( " '}' Expected at line " + commandStream.getLineNumber());
					readToken(); //read }
					if( result == null)
						result = new ScriptLoop( scope, expression, repeat);
					else
						result.addElseLoop( new ScriptLoop( scope, expression, false));
				}
				if( !repeat && !noElse && ( currentToken() != null) && currentToken().equals( "else"))
				{

					if( finalElse)
						throw new AdvancedScriptException( "Else without if at line " + commandStream.getLineNumber());

					if(( nextToken() != null) && nextToken().equals( "if"))
					{
						readToken(); //else
					readToken(); //if
						if(( currentToken() == null) || ( !currentToken().equals("(")))
							throw new AdvancedScriptException( "'(' Expected at line " + commandStream.getLineNumber());
						readToken(); //(
						expression = parseExpression( parentScope);
						if(( currentToken() == null) || ( !currentToken().equals(")")))
							throw new AdvancedScriptException( "')' Expected at line " + commandStream.getLineNumber());
						readToken(); //)
					}
					else //else without condition
					{
						readToken(); //else
						expression = new ScriptValue( new ScriptType( TYPE_BOOLEAN), 1);
						finalElse = true;
					}
					elseFound = true;
					continue;
				}
				elseFound = false;
			} while( elseFound);
		}
		else
			return null;
		return result;
	}

	private ScriptCall parseCall( ScriptScope scope) throws AdvancedScriptException
	{
		String				name = null;
		String				varName;
		ScriptCall			result;
		ScriptExpressionList		params;
		ScriptExpression		val;

		if(( nextToken() == null) || !nextToken().equals( "("))
			return null;

		if( parseIdentifier( currentToken()))
			name = currentToken();
		else
			return null;
		readToken(); //name
		readToken(); //(

		params = new ScriptExpressionList();
		while(( currentToken() != null) && (!currentToken().equals( ")")))
		{
			if(( val = parseExpression( scope)) != null)
			{
				params.addElement( val);
			}
			if( !currentToken().equals( ","))
			{
				if( !currentToken().equals( ")"))
					throw new AdvancedScriptException( "')' Expected at line " + commandStream.getLineNumber());
			}
			else
			{
				readToken();
				if( currentToken().equals( ")"))
					throw new AdvancedScriptException( "Parameter expected at line " + commandStream.getLineNumber());
			}
		}
		if( !currentToken().equals( ")"))
			throw new AdvancedScriptException( "')' Expected at line " + commandStream.getLineNumber());
		readToken(); //)
		result = new ScriptCall( name, scope, params);

		return result;
	}

	private ScriptAssignment parseAssignment( ScriptScope scope) throws AdvancedScriptException
	{
		String				name;
		ScriptVariableReference		leftHandSide;
		ScriptExpression		rightHandSide;

		if(( nextToken() == null) || ( !nextToken().equals( "=")))
			return null;

		if( parseIdentifier( currentToken()))
			name = currentToken();
		else
			return null;
		readToken(); //name
		readToken(); //=
		leftHandSide = new ScriptVariableReference( name, scope);
		rightHandSide = parseExpression( scope);
		return new ScriptAssignment( leftHandSide, rightHandSide);
	}

	private ScriptExpression parseExpression( ScriptScope scope) throws AdvancedScriptException
	{
		return parseExpression( scope, null);
	}
	private ScriptExpression parseExpression( ScriptScope scope, ScriptOperator previousOper) throws AdvancedScriptException
	{
		ScriptExpression	lhs = null;
		ScriptExpression	rhs = null;
		ScriptOperator		oper = null;

		if( currentToken() == null)
			return null;

		if(( !( currentToken() == null)) && currentToken().equals("!"))
			{
			readToken(); // !
			if(( lhs = parseValue( scope)) == null)
				throw new AdvancedScriptException( "Value expected at line " + commandStream.getLineNumber());
			lhs = new ScriptExpression( lhs, null, new ScriptOperator( "!"));
			}
		else
			{
			if(( lhs = parseValue( scope)) == null)
				return null;
			}

		do
		{
			oper = parseOperator( currentToken());
			
			if( oper == null)
			{
				return lhs;
			}

			if(( previousOper != null) && ( !oper.precedes( previousOper)))
			{
				return lhs;
			}

			readToken(); //operator

			rhs = parseExpression( scope, oper);
			lhs = new ScriptExpression( lhs, rhs, oper);
		} while( true);
		

		
	}

	private ScriptExpression parseValue( ScriptScope scope) throws AdvancedScriptException
	{
		ScriptExpression	result;
		int			i;

		if( currentToken() == null)
			return null;


		if( currentToken().equals("("))
			{
			readToken();// (
			result = parseExpression( scope);
			if(( currentToken() == null) || (!currentToken().equals(")")))
				throw new AdvancedScriptException( "')' Expected at line " + commandStream.getLineNumber());
			readToken();// )
			return result;
			}


		//Parse true and false first since they are reserved words.
		if( currentToken().equals( "true"))
			{
			readToken();
			return new ScriptValue( new ScriptType( TYPE_BOOLEAN), 1);
			}
		else if( currentToken().equals( "false"))
			{
			readToken();
			return new ScriptValue( new ScriptType( TYPE_BOOLEAN), 0);
			}

		else if(( result = parseCall( scope)) != null)
			return result;

		else if(( result = parseVariableReference( scope)) != null)
			return result;

		else if(( currentToken().charAt( 0) >= '0') && ( currentToken().charAt( 0) <= '9'))
		{
			int resultInt;

			for( resultInt = 0, i = 0; i < currentToken().length(); i++)
				{
				if( !(( currentToken().charAt(i) >= '0') && ( currentToken().charAt(i) <= '9')))
					throw new AdvancedScriptException( "Digits followed by non-digits at " + commandStream.getLineNumber());
				resultInt += ( resultInt * 10) + ( currentToken().charAt(i) - '0');
				}
			readToken(); //integer
			return new ScriptValue( new ScriptType( TYPE_INT), resultInt);
		}
		else if( currentToken().equals("\""))
		{
			//Directly work with line - ignore any "tokens" you meet until the string is closed
			for( i = 1; ; i++)
			{
				if( i == line.length())
				{
					throw new AdvancedScriptException( "No closing '\"' found at line " + commandStream.getLineNumber());
				}
				if( line.charAt(i) == '"')
				{
					String resultString = line.substring( 1, i);
					line = line.substring( i + 1); //+ 1 to get rid of '"' token
					return new ScriptValue( new ScriptType( TYPE_STRING), resultString);
				}
			}
		
		}
		else if( currentToken().equals( "$"))
		{
			ScriptType type;
			readToken();
				type = parseType();
			
			if( type == null)
				throw new AdvancedScriptException( "Unknown type " + currentToken() + " at line " + commandStream.getLineNumber());
			if( !currentToken().equals("["))
				throw new AdvancedScriptException( "'[' Expected at line " + commandStream.getLineNumber());
			for( i = 1; ; i++)
			{
				if( i == line.length())
				{
					throw new AdvancedScriptException( "No closing ']' found at line " + commandStream.getLineNumber());
				}
				if( line.charAt(i) == ']')
				{
					String resultString = line.substring( 1, i);
					line = line.substring( i + 1); //+1 to get rid of ']' token
					return new ScriptValue( type, resultString);
				}
			}
		}
		return null;
	}

	private ScriptOperator parseOperator( String oper)
	{
		if( oper == null)
			return null;
		if
		(
			oper.equals( "!") ||
			oper.equals( "*") || oper.equals( "/") || oper.equals( "%") ||
			oper.equals( "+") || oper.equals( "-") ||
			oper.equals( "<") || oper.equals( ">") || oper.equals( "<=") || oper.equals( ">=") ||
			oper.equals( "==") || oper.equals( "!=") || 
			oper.equals( "||") || oper.equals( "&&")
		)
		{
			return new ScriptOperator( oper);
		}
		else
			return null;
	}

	private ScriptVariableReference parseVariableReference( ScriptScope scope) throws AdvancedScriptException
	{
		ScriptVariableReference result = null;

		if( parseIdentifier( currentToken()))
		{
			String name = currentToken();
			result = new ScriptVariableReference( name, scope);

			readToken(); //name
			return result;
		}
		else
			return null;
	}

	private String currentToken()
	{
		fixLines();
		if( line == null)
			return null;
		return line.substring(0, tokenLength(line));
	}

	private String nextToken()
	{
		String result;

		fixLines();

		if( line == null)
			return null;
		if( tokenLength( line) < line.length())
			result = line.substring( tokenLength( line));
		else
			{
			if( nextLine == null)
				return null;
			return nextLine.substring(0, tokenLength(nextLine));
			}
		if( result.equals( ""))
			{
			if( nextLine == null)
				return null;
			return nextLine.substring(0, tokenLength(nextLine));
			}
		if( result.charAt(0) == ' ')
			result = result.substring( 1);

		return result.substring( 0, tokenLength( result));
	}

	private void readToken()
	{
		if( line == null)
			return;

		fixLines();

		line = line.substring( tokenLength( line));

	}

	private int tokenLength( String s)
	{
		int result;
		if( s == null)
			return 0;

		for( result = 0; result < s.length(); result++)
		{
			if(( result + 1 < s.length()) && tokenString( s.substring( result, result + 2)))
				{
				return result == 0 ? 2 : result;
				}

			if(( result < s.length()) && tokenString( s.substring( result, result + 1)))
				{
				return result == 0 ? 1 : result;
				}
		}
		return result; //== s.length()
	}

	private void fixLines()
	{
		if( line == null)
			return;

		while( line.equals( ""))
		{
			line = nextLine;
			nextLine = getNextLine();
			if( line == null)
				return;
		}
		while( line.charAt( 0) == ' ')
			line = line.substring( 1);

		if( nextLine == null)
			return;
		while( nextLine.equals( ""))
		{
			nextLine = getNextLine();
			if( nextLine == null)
				return;
		}
	}

	private boolean tokenString( String s)
	{
		if(s.length() == 1)
			{
			for(int i = 0; i < java.lang.reflect.Array.getLength( tokenList); i++)
				if( s.charAt( 0) == tokenList[i])
					return true;
			return false;
			}
		else
			{
			for(int i = 0; i < java.lang.reflect.Array.getLength( multiCharTokenList); i++)
				if( s.equals(multiCharTokenList[i]))
					return true;
			return false;
			}
	}
	

	private void printScope( ScriptScope scope, int indent)
	{
		ScriptVariable	currentVar;
		ScriptFunction	currentFunc;
		ScriptCommand	currentCommand;


		indentLine( indent);
		KoLmafia.getLogStream().println( "<SCOPE>");
		
		indentLine( indent + 1);
		KoLmafia.getLogStream().println( "<VARIABLES>");
		for( currentVar = scope.getFirstVariable(); currentVar != null; currentVar = scope.getNextVariable( currentVar))
			printVariable( currentVar, indent + 2);
		indentLine( indent + 1);
		KoLmafia.getLogStream().println( "<FUNCTIONS>");
		for( currentFunc = scope.getFirstFunction(); currentFunc != null; currentFunc = scope.getNextFunction( currentFunc))
			printFunction( currentFunc, indent + 2);
		indentLine( indent + 1);
		KoLmafia.getLogStream().println( "<COMMANDS>");
		for( currentCommand = scope.getFirstCommand(); currentCommand != null; currentCommand = scope.getNextCommand( currentCommand))
			printCommand( currentCommand, indent + 2);
	}

	private void printVariable( ScriptVariable var, int indent)
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<VAR " + var.getType().toString() + " " + var.getName().toString() + ">");
	}

	private void printFunction( ScriptFunction func, int indent)
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<FUNC " + func.getType().toString() + " " + func.getName().toString() + ">");
		for( ScriptVariableReference current = func.getFirstParam(); current != null; current = func.getNextParam( current))
			printVariableReference( current, indent + 1);
		printScope( func.getScope(), indent + 1);
	}

	private void printCommand( ScriptCommand command, int indent)
	{
		if( command instanceof ScriptReturn)
			printReturn( ( ScriptReturn) command, indent);
		else if( command instanceof ScriptLoop)
			printLoop( ( ScriptLoop) command, indent);
		else if( command instanceof ScriptCall)
			printCall( ( ScriptCall) command, indent);
		else if( command instanceof ScriptAssignment)
			printAssignment( ( ScriptAssignment) command, indent);
		else
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<COMMAND " + command.toString() + ">");
		}
	}

	private void printReturn( ScriptReturn ret, int indent)
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<RETURN " + ret.getType().toString() + ">");
		if( !ret.getType().equals(TYPE_VOID))
			printExpression( ret.getExpression(), indent + 1);
	}

	private void printLoop( ScriptLoop loop, int indent)
	{
		indentLine( indent);
		if( loop.repeats())
			KoLmafia.getLogStream().println( "<WHILE>");
		else
			KoLmafia.getLogStream().println( "<IF>");
		printExpression( loop.getCondition(), indent + 1);
		printScope( loop.getScope(), indent + 1);
		for( ScriptLoop currentElse = loop.getFirstElseLoop(); currentElse != null; currentElse = loop.getNextElseLoop( currentElse))
			printLoop( currentElse, indent + 1);
	}

	private void printCall( ScriptCall call, int indent)
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<CALL " + call.getTarget().getName().toString() + ">");
		for( ScriptExpression current = call.getFirstParam(); current != null; current = call.getNextParam( current))
			printExpression( current, indent + 1);
	}

	private void printAssignment( ScriptAssignment assignment, int indent)
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<ASSIGN " + assignment.getLeftHandSide().getName().toString() + ">");
		printExpression( assignment.getRightHandSide(), indent + 1);
		
	}

	private void printExpression( ScriptExpression expression, int indent)
	{
		if( expression instanceof ScriptValue)
			printValue(( ScriptValue) expression, indent);
		else
		{
			printOperator( expression.getOperator(), indent);
			printExpression( expression.getLeftHandSide(), indent + 1);
			if( expression.getRightHandSide() != null) // ! operator
				printExpression( expression.getRightHandSide(), indent + 1);
		}
	}

	public void printValue( ScriptValue value, int indent)
	{
		if( value instanceof ScriptVariableReference)
			printVariableReference((ScriptVariableReference) value, indent);
		else if( value instanceof ScriptCall)
			printCall((ScriptCall) value, indent);
		else
		{
			indentLine( indent);
			KoLmafia.getLogStream().println( "<VALUE " + value.getType().toString() + " [" + value.toString() + "]>");
		}
	}

	public void printOperator( ScriptOperator oper, int indent)
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<OPER " + oper.toString() + ">");
	}

	public void printVariableReference( ScriptVariableReference varRef, int indent)
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<VARREF> " + varRef.getName().toString());
	}

	private void indentLine( int indent)
	{
		for(int i = 0; i < indent; i++)
			KoLmafia.getLogStream().print( "   ");
	}


	private ScriptValue executeGlobalScope( ScriptScope globalScope) throws AdvancedScriptException
	{
		ScriptFunction	main;
		ScriptValue	result = null;
		String		functionName;

		main = globalScope.findFunction( "main", null);

		if( main == null)
		{
			if( globalScope.getFirstCommand() == null)
				throw new AdvancedScriptException( "No function main or command found.");
			result = globalScope.execute();
		}
		else
		{	
			result = main.execute();
		}

		return result;
	}


}

class ScriptScope extends ScriptListNode
{
	ScriptFunctionList	functions;
	ScriptVariableList	variables;
	ScriptCommandList	commands;
	ScriptScope		parentScope;

	public ScriptScope( ScriptScope parentScope)
	{
		functions = new ScriptFunctionList();
		variables = new ScriptVariableList();
		commands = new ScriptCommandList();
		this.parentScope = parentScope;
	}

	public ScriptScope( ScriptCommand command, ScriptScope parentScope)
	{
		functions = new ScriptFunctionList();
		variables = new ScriptVariableList();
		commands = new ScriptCommandList( command);
		this.parentScope = parentScope;
	}

	public ScriptScope( ScriptVariableList variables, ScriptScope parentScope)
	{
		functions = new ScriptFunctionList();
		if( variables == null)
			variables = new ScriptVariableList();
		this.variables = variables;
		commands = new ScriptCommandList();
		this.parentScope = parentScope;
	}

	public boolean addFunction( ScriptFunction f)
	{
		return functions.addElement( f);
	}

	public boolean addVariable( ScriptVariable v)
	{
		return variables.addElement( v);
	}

	public void addCommand( ScriptCommand c)
	{
		commands.addElement( c);
	}

	public ScriptScope getParentScope()
	{
		return parentScope;
	}

	public ScriptFunction getFirstFunction()
	{
		return ( ScriptFunction) functions.getFirstElement();
	}

	public ScriptFunction getNextFunction( ScriptFunction current)
	{
		return ( ScriptFunction) functions.getNextElement( current);
	}

	public ScriptVariable getFirstVariable()
	{
		return ( ScriptVariable) variables.getFirstElement();
	}

	public ScriptVariable getNextVariable( ScriptVariable current)
	{
		return ( ScriptVariable) variables.getNextElement( current);
	}

	public ScriptCommand getFirstCommand()
	{
		return ( ScriptCommand) commands.getFirstElement();
	}

	public ScriptCommand getNextCommand( ScriptCommand current)
	{
		return ( ScriptCommand) commands.getNextElement( current);
	}

	public boolean assertReturn()
	{
		ScriptCommand current, previous = null;

		for( current = getFirstCommand(); current != null; previous = current, current = getNextCommand( current))
			;
		if( previous == null)
			return false;
		if( !( previous instanceof ScriptReturn))
			return false;
		return true;
	}

	public ScriptFunction findFunction( String name, ScriptExpressionList params) throws AdvancedScriptException
	{


		ScriptFunction		current;
		ScriptVariableReference	currentParam;
		ScriptExpression	currentValue;
		int			paramIndex;

		for( current = getFirstFunction(); current != null; current = getNextFunction( current))
		{
			if( current.getName().equals( name))
			{
				if( params == null)
					return current;
				for
				(
					paramIndex = 1, currentParam = current.getFirstParam(), currentValue = (ScriptExpression) params.getFirstElement();
					(currentParam != null) && (currentValue != null);
					paramIndex++, currentParam = current.getNextParam( currentParam), currentValue = ( ScriptExpression) params.getNextElement( currentValue)
				)
				{
					if( !currentParam.getType().equals( currentValue.getType()))
						throw new AdvancedScriptException( "Illegal parameter " + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType() + " at line " + KoLmafiaASH.commandStream.getLineNumber());
				}
				if(( currentParam != null) || ( currentValue != null))
					throw new AdvancedScriptException( "Illegal amount of parameters for function " + name + " at line " + KoLmafiaASH.commandStream.getLineNumber());
				return current;
			}
		}
		if( parentScope != null)
			return parentScope.findFunction( name, params);
		return null;
	}

	public ScriptValue execute()
	{
		ScriptCommand	current;
		ScriptValue	result;

		for( current = getFirstCommand(); current != null; current = getNextCommand( current))
		{
			result = current.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_RETURN)
			{
				KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
				return result;
			}
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_BREAK)
			{
				throw new RuntimeException( "Internal error: break outside of loop");
			}
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_CONTINUE)
			{
				throw new RuntimeException( "Internal error: continue outside of loop");
			}
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
			{
				return null;
			}
		}
		try
		{
			return new ScriptValue( KoLmafiaASH.TYPE_VOID, 0);
		}
		catch( AdvancedScriptException e)
		{
			throw new RuntimeException( "AdvancedScriptException in execution - should occur only during parsing.");
		}
	}

}

class ScriptScopeList extends ScriptList
{
	public boolean addElement( ScriptListNode n)
	{
		return addElementSerial( n);
	}
}

class ScriptFunction extends ScriptListNode
{
	String					name;
	ScriptType				type;
	ScriptVariableReferenceList		variableReferences;
	ScriptScope				scope;

	public ScriptFunction()
	{
	}

	public ScriptFunction( String name, ScriptType type)
	{
		this.name = name;
		this.type = type;
		this.variableReferences = new ScriptVariableReferenceList();
		this.scope = null;
	}

	public void addVariableReference( ScriptVariableReference v)
	{
		variableReferences.addElement( v);
	}

	public void setScope( ScriptScope s)
	{
		scope = s;
	}

	public ScriptScope getScope()
	{
		return scope;
	}

	public int compareTo( Object o) throws ClassCastException
	{
		if(!(o instanceof ScriptFunction))
			throw new ClassCastException();
		return name.compareTo( (( ScriptFunction) o).name);
	}

	public String getName()
	{
		return name;
	}

	public ScriptVariableReference getFirstParam()
	{
		return (ScriptVariableReference) variableReferences.getFirstElement();
	}

	public ScriptVariableReference getNextParam( ScriptVariableReference current)
	{
		return (ScriptVariableReference) variableReferences.getNextElement( current);
	}

	public ScriptType getType()
	{
		return type;
	}

	public ScriptValue execute()
	{
		return scope.execute();
	}
}


class ScriptExistingFunction extends ScriptFunction
{
	KoLmafia		scriptRequestor;
	ScriptVariable[]	variables;

	public ScriptExistingFunction( String name, ScriptType type, ScriptType[] params, KoLmafia scriptRequestor)
	{
		super( name, type);

		this.scriptRequestor = scriptRequestor;

		variables = new ScriptVariable[ java.lang.reflect.Array.getLength( params)];

		for( int position = 0; position < java.lang.reflect.Array.getLength( params); position++)
		{
			variables[position] = new ScriptVariable( params[position]);
			variableReferences.addElement( new ScriptVariableReference( variables[position]));
		}
	}

	public static ScriptScope getExistingFunctionScope( KoLmafia scriptRequestor)
	{
		ScriptScope result;
		ScriptType[] params;

		result = new ScriptScope( null);

		params = new ScriptType[2];
		params[0] = new ScriptType( KoLmafiaASH.TYPE_INT);
		params[1] = new ScriptType( KoLmafiaASH.TYPE_LOCATION);
		result.addFunction( new ScriptExistingFunction( "adventure", new ScriptType( KoLmafiaASH.TYPE_BOOLEAN), params, scriptRequestor));

		params = new ScriptType[2];
		params[0] = new ScriptType( KoLmafiaASH.TYPE_INT);
		params[1] = new ScriptType( KoLmafiaASH.TYPE_ITEM);
		result.addFunction( new ScriptExistingFunction( "buy", new ScriptType( KoLmafiaASH.TYPE_BOOLEAN), params, scriptRequestor));

		params = new ScriptType[2];
		params[0] = new ScriptType( KoLmafiaASH.TYPE_INT);
		params[1] = new ScriptType( KoLmafiaASH.TYPE_ITEM);
		result.addFunction( new ScriptExistingFunction( "create", new ScriptType( KoLmafiaASH.TYPE_BOOLEAN), params, scriptRequestor));

		params = new ScriptType[2];
		params[0] = new ScriptType( KoLmafiaASH.TYPE_INT);
		params[1] = new ScriptType( KoLmafiaASH.TYPE_ITEM);
		result.addFunction( new ScriptExistingFunction( "use", new ScriptType( KoLmafiaASH.TYPE_BOOLEAN), params, scriptRequestor));

		params = new ScriptType[2];
		params[0] = new ScriptType( KoLmafiaASH.TYPE_INT);
		params[1] = new ScriptType( KoLmafiaASH.TYPE_ITEM);
		result.addFunction( new ScriptExistingFunction( "eat", new ScriptType( KoLmafiaASH.TYPE_BOOLEAN), params, scriptRequestor));

		params = new ScriptType[1];
		params[0] = new ScriptType( KoLmafiaASH.TYPE_ITEM);
		result.addFunction( new ScriptExistingFunction( "item_amount", new ScriptType( KoLmafiaASH.TYPE_INT), params, scriptRequestor));

		params = new ScriptType[1];
		params[0] = new ScriptType( KoLmafiaASH.TYPE_STRING);
		result.addFunction( new ScriptExistingFunction( "print", new ScriptType( KoLmafiaASH.TYPE_VOID), params, scriptRequestor));

		params = new ScriptType[0];
		result.addFunction( new ScriptExistingFunction( "my_zodiac", new ScriptType( KoLmafiaASH.TYPE_ZODIAC), params, scriptRequestor));

		params = new ScriptType[0];
		result.addFunction( new ScriptExistingFunction( "my_class", new ScriptType( KoLmafiaASH.TYPE_CLASS), params, scriptRequestor));

		params = new ScriptType[0];
		result.addFunction( new ScriptExistingFunction( "my_level", new ScriptType( KoLmafiaASH.TYPE_INT), params, scriptRequestor));

		params = new ScriptType[0];
		result.addFunction( new ScriptExistingFunction( "my_hp", new ScriptType( KoLmafiaASH.TYPE_INT), params, scriptRequestor));

		params = new ScriptType[0];
		result.addFunction( new ScriptExistingFunction( "my_maxhp", new ScriptType( KoLmafiaASH.TYPE_INT), params, scriptRequestor));

		params = new ScriptType[0];
		result.addFunction( new ScriptExistingFunction( "my_mp", new ScriptType( KoLmafiaASH.TYPE_INT), params, scriptRequestor));

		params = new ScriptType[0];
		result.addFunction( new ScriptExistingFunction( "my_maxmp", new ScriptType( KoLmafiaASH.TYPE_INT), params, scriptRequestor));

		return result;

	}

	public ScriptValue execute()
	{

		if( !scriptRequestor.permitsContinue())
		{
			KoLmafiaASH.currentState = KoLmafiaASH.STATE_EXIT;
			return null;
		}

		try
		{
			if( name.equals( "adventure"))
				return executeAdventureRequest( variables[0].getIntValue(), variables[1].getLocation());
			else if( name.equals( "buy"))
				return executeBuyRequest( variables[0].getIntValue(), variables[1].getIntValue());
			else if( name.equals( "create"))
				return executeCreateRequest( variables[0].getIntValue(), variables[1].getIntValue());
			else if( name.equals( "use") || name.equals( "eat"))
				return executeUseRequest( variables[0].getIntValue(), variables[1].getIntValue());
			else if( name.equals( "item_amount"))
				return executeItemAmountRequest( variables[0].getIntValue());
			else if( name.equals( "print"))
				return executePrintRequest( variables[0].getStringValue());
			else if( name.equals( "my_zodiac"))
				return executeZodiacRequest();
			else if( name.equals( "my_class"))
				return executeClassRequest();
			else if( name.equals( "my_level"))
				return executeLevelRequest();
			else if( name.equals( "my_hp"))
				return executeHPRequest();
			else if( name.equals( "my_maxhp"))
				return executeMaxHPRequest();
			else if( name.equals( "my_mp"))
				return executeMPRequest();
			else if( name.equals( "my_maxmp"))
				return executeMaxMPRequest();
			else
				throw new RuntimeException( "Internal error: unknown library function " + name);
		}
		catch( AdvancedScriptException e)
		{
			throw new RuntimeException( "AdvancedScriptException in execution - should occur only during parsing.");
		}
	}


	public ScriptValue executeAdventureRequest( int amount, KoLAdventure location) throws AdvancedScriptException
	{
		scriptRequestor.updateDisplay( KoLmafia.DISABLE_STATE, "Beginning " + amount + " turnips to " + location.toString() + "..." );
		scriptRequestor.makeRequest( location, amount );

		if ( scriptRequestor.permitsContinue())
			return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
		else
			return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
	}

	public ScriptValue executeBuyRequest( int amount, int itemID) throws AdvancedScriptException
	{
		ArrayList results;

		results = new ArrayList();

		(new SearchMallRequest( scriptRequestor, '\"' + TradeableItemDatabase.getItemName( itemID) + '\"', 0, results )).run();
		scriptRequestor.makePurchases( results, results.toArray(), amount );

		if ( scriptRequestor.permitsContinue())
			return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
		else
			return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
	}

	public ScriptValue executeCreateRequest( int amount, int itemID) throws AdvancedScriptException
	{
		ItemCreationRequest irequest = ItemCreationRequest.getInstance( scriptRequestor, itemID, amount );
		scriptRequestor.makeRequest( irequest, 1 );

		if ( scriptRequestor.permitsContinue() )
			{
			scriptRequestor.updateDisplay( KoLmafia.NORMAL_STATE, "Successfully created " + irequest.getQuantityNeeded() + " " + irequest.getName());
			return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
			}
		else
			return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);		
	}

	public ScriptValue executeUseRequest( int amount, int itemID) throws AdvancedScriptException
	{
		String itemName;
		int consumptionType;

		itemName = TradeableItemDatabase.getItemName( itemID);
		consumptionType = TradeableItemDatabase.getConsumptionType( itemID );

		if( consumptionType == ConsumeItemRequest.CONSUME_MULTIPLE || consumptionType == ConsumeItemRequest.CONSUME_RESTORE)
			scriptRequestor.makeRequest( new ConsumeItemRequest( scriptRequestor, new AdventureResult( itemName, amount, false ) ), 1 );
		else
			scriptRequestor.makeRequest( new ConsumeItemRequest( scriptRequestor, new AdventureResult( itemName, 1, false ) ), amount );

		if ( scriptRequestor.permitsContinue())
			return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
		else
			return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
	}

	public ScriptValue executeItemAmountRequest( int itemID) throws AdvancedScriptException
	{
		AdventureResult item;

		item = new AdventureResult( TradeableItemDatabase.getItemName( itemID), 0, false );

		return new ScriptValue( KoLmafiaASH.TYPE_INT, item.getCount( KoLCharacter.getInventory()));
	}

	public ScriptValue executePrintRequest( String message) throws AdvancedScriptException
	{
		scriptRequestor.updateDisplay( KoLmafia.DISABLE_STATE, message );

		return new ScriptValue( KoLmafiaASH.TYPE_VOID);
	}

	public ScriptValue executeZodiacRequest() throws AdvancedScriptException
	{
		return new ScriptValue( KoLmafiaASH.TYPE_ZODIAC, KoLCharacter.getSign());
	}

	public ScriptValue executeClassRequest() throws AdvancedScriptException
	{
		return new ScriptValue( KoLmafiaASH.TYPE_CLASS, KoLCharacter.getClassType());
	}

	public ScriptValue executeLevelRequest() throws AdvancedScriptException
	{
		return new ScriptValue( KoLmafiaASH.TYPE_INT, KoLCharacter.getLevel());
	}

	public ScriptValue executeHPRequest() throws AdvancedScriptException
	{
		return new ScriptValue( KoLmafiaASH.TYPE_INT, KoLCharacter.getCurrentHP());
	}

	public ScriptValue executeMaxHPRequest() throws AdvancedScriptException
	{
		return new ScriptValue( KoLmafiaASH.TYPE_INT, KoLCharacter.getMaximumHP());
	}

	public ScriptValue executeMPRequest() throws AdvancedScriptException
	{
		return new ScriptValue( KoLmafiaASH.TYPE_INT, KoLCharacter.getCurrentMP());
	}

	public ScriptValue executeMaxMPRequest() throws AdvancedScriptException
	{
		return new ScriptValue( KoLmafiaASH.TYPE_INT, KoLCharacter.getMaximumMP());
	}


}

class ScriptFunctionList extends ScriptList
{

}

class ScriptVariable extends ScriptListNode
{
	String		name;

	ScriptValue	content;

	public ScriptVariable( ScriptType type)
	{
		this.name = null;
		content = new ScriptValue( type);
	}


	public ScriptVariable( String name, ScriptType type)
	{
		this.name = name;
		content = new ScriptValue( type);
	}

	public int compareTo( Object o) throws ClassCastException
	{
		if(!(o instanceof ScriptVariable))
			throw new ClassCastException();
		if( name == null)
			return 1;
		return name.compareTo( (( ScriptVariable) o).name);

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

	public void setValue( ScriptValue targetValue)
	{
		if( !getType().equals( targetValue.getType()))
			throw new RuntimeException( "Internal error: Cannot assign " + targetValue.getType().toString() + " to " + getType().toString());
		content = targetValue;
	}
}

class ScriptVariableList extends ScriptList
{
	public ScriptVariable getFirstVariable()
	{
		return ( ScriptVariable) getFirstElement();
	}

	public ScriptVariable getNextVariable( ScriptVariable current)
	{
		return ( ScriptVariable) getNextElement( current);
	}
}

class ScriptVariableReference extends ScriptValue
{
	ScriptVariable target;

	public ScriptVariableReference( ScriptVariable target)
	{
		this.target = target;
	}

	public ScriptVariableReference( String varName, ScriptScope scope) throws AdvancedScriptException
	{
		target = findVariable( varName, scope);
	}

	private ScriptVariable findVariable( String name, ScriptScope scope) throws AdvancedScriptException
	{
		ScriptVariable current;

		do
		{
			for(current = scope.getFirstVariable(); current != null; current = scope.getNextVariable( current))
			{
				if( current.getName().equals( name))
					{
					return current;
					}
			}
			scope = scope.getParentScope();
		} while( scope != null);
		
		throw new AdvancedScriptException( "Undefined variable " + name + " at line " + KoLmafiaASH.commandStream.getLineNumber());
	}

	public ScriptType getType()
	{
		return target.getType();
	}

	public String getName()
	{
		return target.getName();
	}


	public int compareTo( Object o) throws ClassCastException
	{
		if(!(o instanceof ScriptVariableReference))
			throw new ClassCastException();
		return target.getName().compareTo( (( ScriptVariableReference) o).target.getName());

	}

	public ScriptValue execute()
	{
		return target.getValue();
	}

	public void setValue( ScriptValue targetValue)
	{
		target.setValue( targetValue);
	}
}

class ScriptVariableReferenceList extends ScriptList
{
	public boolean addElement( ScriptListNode n)
	{
		return addElementSerial( n);
	}
}

class ScriptCommand extends ScriptListNode
{
	int command;


	public ScriptCommand()
	{
		
	}

	public ScriptCommand( String command) throws AdvancedScriptException
	{
		if( command.equals( "break"))
			this.command = KoLmafiaASH.COMMAND_BREAK;
		else if( command.equals( "continue"))
			this.command = KoLmafiaASH.COMMAND_CONTINUE;
		else
			throw new AdvancedScriptException( command + " is not a command at line " + KoLmafiaASH.commandStream.getLineNumber());
	}

	public ScriptCommand( int command)
	{
		this.command = command;
	}

	public int compareTo( Object o) throws ClassCastException
	{
		if(!(o instanceof ScriptCommand))
			throw new ClassCastException();
		return 0;

	}

	public String toString()
	{
		if( this.command == KoLmafiaASH.COMMAND_BREAK)
			return "break";
		else if( this.command == KoLmafiaASH.COMMAND_CONTINUE)
			return "continue";
		return "<unknown command>";
	}

	public ScriptValue execute()
	{
		if( this.command == KoLmafiaASH.COMMAND_BREAK)
			{
			KoLmafiaASH.currentState = KoLmafiaASH.STATE_BREAK;
			return null;
			}
		else if( this.command == KoLmafiaASH.COMMAND_CONTINUE)
			{
			KoLmafiaASH.currentState = KoLmafiaASH.STATE_CONTINUE;
			return null;
			}
		throw new RuntimeException( "Internal error: unknown ScriptCommand type");
		
	}
}

class ScriptCommandList extends ScriptList
{

	public ScriptCommandList()
		{
		super();
		}

	public ScriptCommandList( ScriptCommand c)
		{
		super( c);
		}

	public boolean addElement( ScriptListNode n) //Command List has to remain in original order, so override addElement
	{
		return addElementSerial( n);
	}
}

class ScriptReturn extends ScriptCommand
{
	private ScriptExpression returnValue;

	public ScriptReturn( ScriptExpression returnValue, ScriptType expectedType) throws AdvancedScriptException
	{
		this.returnValue = returnValue;
		if( !( expectedType == null) && !returnValue.getType().equals( expectedType))
			throw new AdvancedScriptException( "Cannot apply " + returnValue.getType().toString() + " to " + expectedType.toString() + " at line " + KoLmafiaASH.commandStream.getLineNumber());
	}

	public ScriptType getType()
	{
		return returnValue.getType();
	}

	public ScriptExpression getExpression()
	{
		return returnValue;
	}

	public ScriptValue execute()
	{
		ScriptValue result;

		result = returnValue.execute();
		if( KoLmafiaASH.currentState != KoLmafiaASH.STATE_EXIT)
			KoLmafiaASH.currentState = KoLmafiaASH.STATE_RETURN;
		return result;
	}
}


class ScriptLoop extends ScriptCommand
{
	private boolean			repeat;
	private ScriptExpression	condition;
	private ScriptScope		scope;
	private ScriptLoopList		elseLoops;

	public ScriptLoop( ScriptScope scope, ScriptExpression condition, boolean repeat) throws AdvancedScriptException
	{
		this.scope = scope;
		this.condition = condition;
		if( !( condition.getType().equals( KoLmafiaASH.TYPE_BOOLEAN)))
			throw new AdvancedScriptException( "Cannot apply " + condition.getType().toString() + " to boolean at line " + KoLmafiaASH.commandStream.getLineNumber());
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
		return ( ScriptLoop) elseLoops.getFirstElement();
	}

	public ScriptLoop getNextElseLoop( ScriptLoop current)
	{
		return ( ScriptLoop) elseLoops.getNextElement( current);
	}


	public void addElseLoop( ScriptLoop elseLoop) throws AdvancedScriptException
	{
		if( repeat == true)
			throw new AdvancedScriptException( "Else without if at line " + KoLmafiaASH.commandStream.getLineNumber());
		elseLoops.addElement( elseLoop);
	}

	public ScriptValue execute()
	{
		ScriptValue result;
		while( condition.execute().getIntValue() == 1)
		{
			result = scope.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_BREAK)
			{
				if( repeat)
				{
					KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
					return null;
				}
				else
					return null;
			}				
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_CONTINUE)
			{
				if( !repeat)
					return null;
				else
					KoLmafiaASH.currentState = KoLmafiaASH.STATE_NORMAL;
			}
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_RETURN)
			{
				return result;
			}
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
			{
				return null;
			}
			if( !repeat)
				break;
		}
		for( ScriptLoop elseLoop = elseLoops.getFirstScriptLoop(); elseLoop != null; elseLoop = elseLoops.getNextScriptLoop( elseLoop))
			{
			result = elseLoop.execute();
			if( KoLmafiaASH.currentState != KoLmafiaASH.STATE_NORMAL)
				return result;
			}
		return null;
	}
}


class ScriptLoopList extends ScriptList
{
	public ScriptLoop getFirstScriptLoop()
	{
		return ( ScriptLoop) getFirstElement();
	}

	public ScriptLoop getNextScriptLoop( ScriptLoop current)
	{
		return ( ScriptLoop) getNextElement( current);
	}

	public boolean addElement( ScriptListNode n)
	{
		return addElementSerial( n);
	}
}

class ScriptCall extends ScriptValue
{
	private ScriptFunction				target;
	private ScriptExpressionList			params;

	public ScriptCall( String functionName, ScriptScope scope, ScriptExpressionList params) throws AdvancedScriptException
	{
		target = findFunction( functionName, scope, params);
		if( target == null)
			throw new AdvancedScriptException( "Undefined reference " + functionName + " at line " + KoLmafiaASH.commandStream.getLineNumber());
		this.params = params;
	}

	private ScriptFunction findFunction( String name, ScriptScope scope, ScriptExpressionList params) throws AdvancedScriptException
	{
		if( scope == null)
			return null;
		return scope.findFunction( name, params);
	}

	public ScriptFunction getTarget()
	{
		return target;
	}

	public ScriptExpression getFirstParam()
	{
		return ( ScriptExpression) params.getFirstElement();
	}

	public ScriptExpression getNextParam( ScriptExpression current)
	{
		return ( ScriptExpression) params.getNextElement( current);
	}

	public ScriptType getType()
	{
		return target.getType();
	}

	public ScriptValue execute()
	{
		ScriptVariableReference		paramVarRef;
		ScriptExpression		paramValue;
		for
		(
			paramVarRef = target.getFirstParam(), paramValue = params.getFirstExpression();
			paramVarRef != null;
			paramVarRef = target.getNextParam( paramVarRef), paramValue = params.getNextExpression( paramValue)
		)
		{
			if( paramVarRef == null)
				throw new RuntimeException( "Internal error: illegal arguments.");
			paramVarRef.setValue( paramValue.execute());
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
		}
		if( paramValue != null)
			throw new RuntimeException( "Internal error: illegal arguments.");

		return target.execute();
	}
}

class ScriptAssignment extends ScriptCommand
{
	private ScriptVariableReference	leftHandSide;
	private ScriptExpression	rightHandSide;

	public ScriptAssignment( ScriptVariableReference leftHandSide, ScriptExpression rightHandSide) throws AdvancedScriptException
	{
		this.leftHandSide = leftHandSide;
		this.rightHandSide = rightHandSide;
		if( !leftHandSide.getType().equals( rightHandSide.getType()))
			throw new AdvancedScriptException( "Cannot apply " + rightHandSide.getType().toString() + " to " + leftHandSide.toString() + " at line " + KoLmafiaASH.commandStream.getLineNumber());
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

	public ScriptValue execute()
	{
		leftHandSide.setValue( rightHandSide.execute());
		return null;
	}

}

class ScriptType
{
	int type;

	public ScriptType( int type)
	{
		this.type = type;
	}

	public boolean equals( ScriptType type)
	{
		if( this.type == type.type)
			return true;
		return false;
	}

	public boolean equals( int type)
	{
		if( this.type == type)
			return true;
		return false;
	}

	public String toString()
	{
		if( type == KoLmafiaASH.TYPE_VOID)
			return "void";
		if( type == KoLmafiaASH.TYPE_BOOLEAN)
			return "boolean";
		if( type == KoLmafiaASH.TYPE_INT)
			return "int";
		if( type == KoLmafiaASH.TYPE_STRING)
			return "string";
		if( type == KoLmafiaASH.TYPE_ITEM)
			return "item";
		if( type == KoLmafiaASH.TYPE_ZODIAC)
			return "zodiac";
		if( type == KoLmafiaASH.TYPE_LOCATION)
			return "location";
		if( type == KoLmafiaASH.TYPE_CLASS)
			return "class";
		return "<unknown type>";
	}

}

class ScriptValue extends ScriptExpression
{
	ScriptType type;

	int contentInt = 0;
	String contentString = null;
	Object content = null;

	public ScriptValue()
	{
		//stub constructor for subclasses
		//should not be called
	}

	public ScriptValue( int type) throws AdvancedScriptException
	{
		this.type = new ScriptType( type);
		fillContent();
	}

	public ScriptValue( ScriptType type)
	{
		this.type = type;
	}

	public ScriptValue( int type, int contentInt) throws AdvancedScriptException
	{
		this.type = new ScriptType( type);
		this.contentInt = contentInt;
		fillContent();
	}


	public ScriptValue( ScriptType type, int contentInt) throws AdvancedScriptException
	{
		this.type = type;
		this.contentInt = contentInt;
		fillContent();
	}


	public ScriptValue( int type, String contentString) throws AdvancedScriptException
	{
		this.type = new ScriptType( type);
		this.contentString = contentString;
		fillContent();
	}

	public ScriptValue( ScriptType type, String contentString) throws AdvancedScriptException
	{
		this.type = type;
		this.contentString = contentString;
		fillContent();
	}

	public ScriptValue( ScriptValue original)
	{
		this.type = original.type;
		this.contentInt = original.contentInt;
		this.contentString = original.contentString;
		this.content = original.content;
	}


	public int compareTo( Object o) throws ClassCastException
	{
		if(!(o instanceof ScriptValue))
			throw new ClassCastException();
		return 0;

	}

	public ScriptType getType()
	{
		return type;
	}

	public String toString()
	{
		if( contentString != null)
			return contentString;
		else
			return Integer.toString( contentInt);
	}

	public int getIntValue()
	{
		return contentInt;
	}

	public String getStringValue()
	{
		return contentString;
	}

	public ScriptValue execute()
	{
		return this;
	}

	public void fillContent() throws AdvancedScriptException
	{
		if( type.equals( KoLmafiaASH.TYPE_ITEM))
		{
			if(( contentInt = TradeableItemDatabase.getItemID( contentString)) == -1)
				throw new AdvancedScriptException( "Item" + contentString + " not found in database at line " + KoLmafiaASH.commandStream.getLineNumber());
		}
		else if( type.equals( KoLmafiaASH.TYPE_ZODIAC))
			{
			if( contentString.equalsIgnoreCase( "wallaby"))
				contentInt = KoLmafiaASH.ZODIAC_WALLABY;
			else if( contentString.equalsIgnoreCase( "mongoose"))
				contentInt = KoLmafiaASH.ZODIAC_MONGOOSE;
			else if( contentString.equalsIgnoreCase( "vole"))
				contentInt = KoLmafiaASH.ZODIAC_VOLE;
			else if( contentString.equalsIgnoreCase( "platypus"))
				contentInt = KoLmafiaASH.ZODIAC_PLATYPUS;
			else if( contentString.equalsIgnoreCase( "opossum"))
				contentInt = KoLmafiaASH.ZODIAC_OPOSSUM;
			else if( contentString.equalsIgnoreCase( "marmot"))
				contentInt = KoLmafiaASH.ZODIAC_MARMOT;
			else if( contentString.equalsIgnoreCase( "wombat"))
				contentInt = KoLmafiaASH.ZODIAC_WOMBAT;
			else if( contentString.equalsIgnoreCase( "blender"))
				contentInt = KoLmafiaASH.ZODIAC_BLENDER;
			else if( contentString.equalsIgnoreCase( "packrat"))
				contentInt = KoLmafiaASH.ZODIAC_PACKRAT;
			else if( contentString.equalsIgnoreCase( "none"))
				contentInt = KoLmafiaASH.ZODIAC_NONE;
			else
				throw new AdvancedScriptException( "Unknown zodiac " + contentString + " at line " + KoLmafiaASH.commandStream.getLineNumber());
			}
		else if( type.equals( KoLmafiaASH.TYPE_LOCATION))
		{
			if(( content = AdventureDatabase.getAdventure( contentString)) == null)
				throw new AdvancedScriptException( "Location " + contentString + " not found in database at line " + KoLmafiaASH.commandStream.getLineNumber());
		}
		else if( type.equals( KoLmafiaASH.TYPE_CLASS))
		{
			if( contentString.equalsIgnoreCase( "sealclubber") || contentString.equalsIgnoreCase( "seal clubber"))
				contentInt = KoLmafiaASH.CLASS_SEALCLUBBER;
			else if( contentString.equalsIgnoreCase( "turtletamer") || contentString.equalsIgnoreCase( "turtle tamer"))
				contentInt = KoLmafiaASH.CLASS_TURTLETAMER;
			else if( contentString.equalsIgnoreCase( "pastamancer"))
				contentInt = KoLmafiaASH.CLASS_PASTAMANCER;
			else if( contentString.equalsIgnoreCase( "sauceror"))
				contentInt = KoLmafiaASH.CLASS_SAUCEROR;
			else if( contentString.equalsIgnoreCase( "discobandit") || contentString.equalsIgnoreCase( "disco bandit"))
				contentInt = KoLmafiaASH.CLASS_DISCOBANDIT;
			else if( contentString.equalsIgnoreCase( "accordionthief") || contentString.equalsIgnoreCase( "accordion thief"))
				contentInt = KoLmafiaASH.CLASS_ACCORDIONTHIEF;
			else
				throw new AdvancedScriptException( "Unknown class " + contentString + " at line " + KoLmafiaASH.commandStream.getLineNumber());
		}
	}

	public KoLAdventure getLocation()
	{
		if( !type.equals( KoLmafiaASH.TYPE_LOCATION))
			throw new RuntimeException( "Internal error: getLocation() called on non-location");
		else
			return ( KoLAdventure) content;
	}
}

class ScriptExpression extends ScriptCommand
{
	ScriptExpression	lhs;
	ScriptExpression	rhs;
	ScriptOperator		oper;

	public ScriptExpression(ScriptExpression lhs, ScriptExpression rhs, ScriptOperator oper) throws AdvancedScriptException
	{
		this.lhs = lhs;
		this.rhs = rhs;
		if(( rhs != null) && !lhs.getType().equals( rhs.getType()))
			throw new AdvancedScriptException( "Cannot apply " + lhs.getType().toString() + " to " + rhs.getType().toString() + " at line " + KoLmafiaASH.commandStream.getLineNumber());
		this.oper = oper;
	}


	public ScriptExpression()
	{
		//stub constructor for subclasses
		//should not be called
	}

	public ScriptType getType()
	{
		if( oper.isBool())
			return new ScriptType( KoLmafiaASH.TYPE_BOOLEAN);
		else
			return lhs.getType();
		
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

	public ScriptValue execute()
	{
		try
		{
			return oper.applyTo(lhs, rhs);
		}
		catch( AdvancedScriptException e)
		{
			throw new RuntimeException( "AdvancedScriptException in execution - should occur only during parsing.");
		}
	}

}

class ScriptExpressionList extends ScriptList
{
	public ScriptExpression getFirstExpression()
	{
		return ( ScriptExpression) getFirstElement();
	}

	public ScriptExpression getNextExpression( ScriptExpression current)
	{
		return ( ScriptExpression) getNextElement( current);
	}
	

	public boolean addElement( ScriptListNode n) //Expression List has to remain in original order, so override addElement
	{
		return addElementSerial( n);
	}
}

class ScriptOperator
{
	String operString;

	public ScriptOperator( String oper)
	{
		if( oper == null)
			throw new RuntimeException( "Internal error in ScriptOperator()");
		operString = oper;
	}

	public boolean precedes( ScriptOperator oper)
	{
		return operStrength() > oper.operStrength();
	}

	private int operStrength()
	{
		if( operString.equals( "!"))
			return 6;
		if( operString.equals( "*") || operString.equals( "/") || operString.equals( "%"))
			return 5;
		else if( operString.equals( "+") || operString.equals( "-"))
			return 4;
		else if( operString.equals( "<") || operString.equals( ">") || operString.equals( "<=") || operString.equals( ">="))
			return 3;
		else if( operString.equals( "==") || operString.equals( "!="))
			return 2;
		else if( operString.equals( "||") || operString.equals( "&&"))
			return 1;
		else
			return -1;
	}

	public boolean isBool()
	{
		if
		(
			operString.equals( "*") || operString.equals( "/") || operString.equals( "%") ||
			operString.equals( "+") || operString.equals( "-")
		)
			return false;
		else
			return true;
	
	}

	public String toString()
	{
		return operString;
	}

	public ScriptValue applyTo( ScriptExpression lhs, ScriptExpression rhs) throws AdvancedScriptException
	{

		ScriptValue leftResult = lhs.execute();
		ScriptValue rightResult;

		if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
			return null;

		if(( rhs != null) && ( !rhs.getType().equals( lhs.getType()))) //double-check values
		{
			throw new RuntimeException( "Internal error: left hand side and right hand side do not correspond");
		}

		if( operString.equals( "!"))
		{
			if( leftResult.getIntValue() == 0)
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
			else
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
		}
		if( operString.equals( "*"))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			return new ScriptValue( KoLmafiaASH.TYPE_INT, leftResult.getIntValue() * rightResult.getIntValue());
		}
		if( operString.equals( "/"))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			return new ScriptValue( KoLmafiaASH.TYPE_INT, leftResult.getIntValue() / rightResult.getIntValue());
		}
		if( operString.equals( "%"))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			return new ScriptValue( KoLmafiaASH.TYPE_INT, leftResult.getIntValue() % rightResult.getIntValue());
		}
		if( operString.equals( "+"))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			if( lhs.getType().equals(KoLmafiaASH.TYPE_STRING))
				return new ScriptValue( KoLmafiaASH.TYPE_STRING, leftResult.getStringValue() + rightResult.getStringValue());
			else
				return new ScriptValue( KoLmafiaASH.TYPE_INT, leftResult.getIntValue() + rightResult.getIntValue());
		}
		if( operString.equals( "-"))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			return new ScriptValue( KoLmafiaASH.TYPE_INT, leftResult.getIntValue() - rightResult.getIntValue());
		}
		if( operString.equals( "<"))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			if( leftResult.getIntValue() < rightResult.getIntValue())
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
			else
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
		}
		if( operString.equals( ">"))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			if( leftResult.getIntValue() > rightResult.getIntValue())
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
			else
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
		}
		if( operString.equals( "<="))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			if( leftResult.getIntValue() <= rightResult.getIntValue())
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
			else
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
		}
		if( operString.equals( ">="))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			if( leftResult.getIntValue() >= rightResult.getIntValue())
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
			else
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
		}
		if( operString.equals( "=="))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			if( lhs.getType().equals(KoLmafiaASH.TYPE_INT) || lhs.getType().equals(KoLmafiaASH.TYPE_BOOLEAN))
			{
				if( leftResult.getIntValue() == rightResult.getIntValue())
					return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
				else
					return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
			}
			else
			{
				if( leftResult.getStringValue().equals( rightResult.getStringValue()))
					return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
				else
					return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);		
			}
		}
		if( operString.equals( "!="))
		{
			rightResult = rhs.execute();
			if( KoLmafiaASH.currentState == KoLmafiaASH.STATE_EXIT)
				return null;
			if( lhs.getType().equals(KoLmafiaASH.TYPE_INT) || lhs.getType().equals(KoLmafiaASH.TYPE_BOOLEAN))
			{
				if( leftResult.getIntValue() != rightResult.getIntValue())
					return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
				else
					return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
			}
			else
			{
				if( !leftResult.getStringValue().equals( rightResult.getStringValue()))
					return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
				else
					return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);		
			}
		}
		if( operString.equals( "||"))
		{
			if( leftResult.getIntValue() == 1)
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 1);
			else
				return rhs.execute();
		}
		if( operString.equals( "&&"))
		{
			if( leftResult.getIntValue() == 0)
				return new ScriptValue( KoLmafiaASH.TYPE_BOOLEAN, 0);
			else
				return rhs.execute();
		}
		throw new RuntimeException( "Internal error: illegal operator.");
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

	public void setNext( ScriptListNode node)
	{
		next = node;
	}

	public int compareTo( Object o) throws ClassCastException
	{
		if(!(o instanceof ScriptListNode))
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

	public ScriptList( ScriptListNode node)
	{
		firstNode = node;
	}

	public boolean addElement( ScriptListNode n)
	{
		ScriptListNode current;
		ScriptListNode previous = null;

		if( n.getNext() != null)
			throw new RuntimeException( "Internal error: Element already in list.");

		if( firstNode == null)
			{
			firstNode = n;
			return true;
			}
		for( current = firstNode; current != null; previous = current, current = current.getNext())
		{
			if( current.compareTo( n) <= 0)
				break;
		}
		if(( current != null) && ( current.compareTo( n) == 0))
		{
			return false;
		}
		if( previous == null) //Insert in front of very first element
		{
			firstNode = n;
			firstNode.setNext( current);
		}
		else
		{
			previous.setNext( n);
			n.setNext( current);
		}
		return true;
	}

	public boolean addElementSerial( ScriptListNode n) //Function for subclasses to override addElement with
	{
		ScriptListNode current;
		ScriptListNode previous = null;

		if( n.getNext() != null)
			throw new RuntimeException( "Internal error: Element already in list.");

		if( firstNode == null)
			{
			firstNode = n;
			return true;
			}

		for( current = firstNode; current != null; previous = current, current = current.getNext())
			;

		previous.setNext( n);
		return true;
	}


	public ScriptListNode getFirstElement()
	{
		return firstNode;
	}

	public ScriptListNode getNextElement( ScriptListNode n)
	{
		return n.getNext();
	}

}

class AdvancedScriptException extends Exception
{
	AdvancedScriptException( String s)
	{
		super( s);
	}
}
