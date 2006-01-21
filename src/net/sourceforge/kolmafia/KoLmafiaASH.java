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

	public static final int COMMAND_BREAK = 1;
	public static final int COMMAND_CONTINUE = 2;
	private static final String escapeString = "//";

	private static ScriptScope global;
	private static String line;
	private static String nextLine;

	public static LineNumberReader commandStream;

	public void execute( String parameters ) throws IOException
	{
		commandStream = new LineNumberReader( new InputStreamReader( new FileInputStream( parameters ) ) );

		line = getNextLine();
		nextLine = getNextLine();
		global = parseScope( new ScriptType(TYPE_BOOLEAN), new ScriptVariableList(), null, false);

		if ( line != null )
			throw new RuntimeException( "Script parsing error at line " + getLineNumber());

		printScope( global, 0 );

		//Execute code here

		return;
	}

	private ScriptScope parseScope( ScriptType expectedType, ScriptVariableList variables, ScriptScope parentScope, boolean whileLoop )
	{
		ScriptFunction f = null;
		ScriptVariable v = null;
		ScriptCommand c = null;
		ScriptType t = null;

		ScriptScope result = new ScriptScope( variables, parentScope);

		while( true)
		{
			if(( t = parseType()) == null)
				if(( c = parseCommand( expectedType, result, false, whileLoop)) != null)
					{
					result.addCommand( c);
					continue;
					}
				else
					//No type and no call -> done.
					break;

			if(( f = parseFunction( t, result)) != null)
				{
				result.addFunction( f);
				}
			else if(( v = parseVariable( t)) != null)
				{
				result.addVariable( v);
				if( currentToken().equals(";"))
					readToken(); //read ;
				else
					throw new RuntimeException( "';' Expected at line " + getLineNumber());
				}
			else
				//Found a type but no function or variable to tie it to
				throw new RuntimeException( "Script parse error at line " + getLineNumber());
		};
		if( !result.assertReturn())
			{
			if( expectedType.equals( TYPE_VOID))
				result.addCommand( new ScriptReturn( null, new ScriptType( TYPE_VOID)));
			else if( expectedType.equals( TYPE_BOOLEAN))
				result.addCommand( new ScriptReturn( new ScriptValue( new ScriptType( TYPE_BOOLEAN), 1), new ScriptType( TYPE_BOOLEAN)));
			else
				throw new RuntimeException( "Missing return value at line " + getLineNumber());
			}
		return result;
	}

	private ScriptFunction parseFunction( ScriptType t, ScriptScope parentScope)
	{
		Identifier			functionName;
		ScriptFunction			result;
		ScriptType			paramType = null;
		ScriptVariable			param = null;
		ScriptVariableList		paramList = null;
		ScriptVariableReference		paramRef = null;

		String lastParam = null;

		try
		{
			functionName = new Identifier( currentToken());
			if(( nextToken() == null) || (!nextToken().equals( "(")))
				return null;
			readToken(); //read Function name
			readToken(); //read (

			paramList = new ScriptVariableList();

			result = new ScriptFunction( functionName, t);
			while( !currentToken().equals( ")"))
				{
				if(( paramType = parseType()) == null)
					throw new RuntimeException( " ')' Expected at line " + getLineNumber());
				if(( param = parseVariable( paramType)) == null)
					throw new RuntimeException( " Identifier expected at line " + getLineNumber());
				if( !currentToken().equals( ")"))
					{
					if( !currentToken().equals( ","))
						throw new RuntimeException( " ')' Expected at line " + getLineNumber());
					readToken(); //read comma
					}
				paramRef = new ScriptVariableReference( param);
				result.addVariableReference( paramRef);
				paramList.addElement( param);
				}
			readToken(); //read )
			if( !currentToken().equals( "{")) //Scope is a single call
				{
				result.setScope( new ScriptScope( parseCommand( t, parentScope, false, false), parentScope));
				for( param = paramList.getFirstVariable(); param != null; param = paramList.getNextVariable( param))
					{
					lastParam = param.getName().toString();
					result.getScope().addVariable( new ScriptVariable( param));
					}
				if( !result.getScope().assertReturn())
					throw new RuntimeException( "Missing return value at line " + getLineNumber());
				}
			else
				{
				readToken(); //read {
				result.setScope( parseScope( t, paramList, parentScope, false));
				if( !currentToken().equals( "}"))
					throw new RuntimeException( " '}' Expected at line " + getLineNumber());
				readToken(); //read }
				}
		}
		catch( RuntimeException e)
		{
			return null;
		}
		return result;
	}

	private ScriptVariable parseVariable( ScriptType t) throws RuntimeException
	{
		ScriptVariable result;

		result = new ScriptVariable( new Identifier( currentToken()), t);
		readToken(); //If creation of Identifier succeeded, go to next token.
		return result;
	}

	private ScriptCommand parseCommand( ScriptType functionType, ScriptScope scope, boolean noElse, boolean whileLoop) throws RuntimeException
	{
		ScriptCommand result;

		if(( currentToken() != null) && ( currentToken().equals( "break")))
			{
			if( !whileLoop)
				throw new RuntimeException( "break outside of loop at line " + getLineNumber());
			result = new ScriptCommand( COMMAND_BREAK);
			readToken(); //break
			}

		else if(( currentToken() != null) && ( currentToken().equals( "continue")))
			{
			if( !whileLoop)
				throw new RuntimeException( "break outside of loop at line " + getLineNumber());
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
			throw new RuntimeException( "';' Expected at line " + getLineNumber());
		readToken(); // ;

		return result;
	}

	private ScriptType parseType()
	{
		if( currentToken() == null)
			return null;
		String type = currentToken();
		ScriptType result;
		try
		{
			result = new ScriptType( type);
			readToken();
			return result;
		}
		catch( RuntimeException e)
		{
			return null;
		}

	}

	private ScriptReturn parseReturn( ScriptType expectedType, ScriptScope parentScope ) throws RuntimeException
	{

		ScriptExpression expression = null;

		if(( currentToken() == null) || !( currentToken().equals( "return")))
			return null;
		readToken(); //return
		if(( currentToken() != null) && ( currentToken().equals(";")))
		{
			if( expectedType.equals(TYPE_VOID))
			{
				return new ScriptReturn( null, new ScriptType( TYPE_VOID));
			}
			else
				throw new RuntimeException( "Return needs value at line " + getLineNumber());
		}
		else
		{
			if(( expression = parseExpression( parentScope)) == null)
				throw new RuntimeException( "Expression expected at line " + getLineNumber());
			return new ScriptReturn( expression, expectedType);
		}
	}


	private ScriptLoop parseLoop( ScriptType functionType, ScriptScope parentScope, boolean noElse, boolean loop ) throws RuntimeException
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
				throw new RuntimeException( "'(' Expected at line " + getLineNumber());
			readToken(); //if or while
			readToken(); //(
			expression = parseExpression( parentScope);
			if(( currentToken() == null) || ( !currentToken().equals(")")))
				throw new RuntimeException( "')' Expected at line " + getLineNumber());
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
						throw new RuntimeException( " '}' Expected at line " + getLineNumber());
					readToken(); //read }
					if( result == null)
						result = new ScriptLoop( scope, expression, repeat);
					else
						result.addElseLoop( new ScriptLoop( scope, expression, false));
				}
				if( !repeat && !noElse && ( currentToken() != null) && currentToken().equals( "else"))
				{

					if( finalElse)
						throw new RuntimeException( "Else without if at line " + getLineNumber());

					if(( nextToken() != null) && nextToken().equals( "if"))
					{
						readToken(); //else
						readToken(); //if
						if(( currentToken() == null) || ( !currentToken().equals("(")))
							throw new RuntimeException( "'(' Expected at line " + getLineNumber());
						readToken(); //(
						expression = parseExpression( parentScope);
						if(( currentToken() == null) || ( !currentToken().equals(")")))
							throw new RuntimeException( "')' Expected at line " + getLineNumber());
						readToken(); //)
					}
					else //else without condition
					{
						readToken(); //else
						expression = new ScriptValue( new ScriptType(TYPE_BOOLEAN), 1);
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

	private ScriptCall parseCall( ScriptScope scope ) throws RuntimeException
	{
		Identifier			name = null;
		Identifier			varName;
		ScriptCall			result;
		ScriptExpressionList		params;
		ScriptExpression		val;

		if(( nextToken() == null) || !nextToken().equals( "("))
			return null;
		try
		{
			name = new Identifier( currentToken());
		}
		catch(RuntimeException e)
		{
			return null;
		}
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
					throw new RuntimeException( "')' Expected at line " + getLineNumber());
			}
			else
			{
				readToken();
				if( currentToken().equals( ")"))
					throw new RuntimeException( "Parameter expected at line " + getLineNumber());
			}
		}
		if( !currentToken().equals( ")"))
			throw new RuntimeException( "')' Expected at line " + getLineNumber());
		readToken(); //)
		result = new ScriptCall( name, scope, params);

		return result;
	}

	private ScriptAssignment parseAssignment( ScriptScope scope) throws RuntimeException
	{
		Identifier			name;
		ScriptVariableReference		leftHandSide;
		ScriptExpression		rightHandSide;

		if(( nextToken() == null) || ( !nextToken().equals( "=")))
			return null;

		try
		{
			name = new Identifier( currentToken());
		}
		catch(RuntimeException e)
		{
			return null;
		}
		readToken(); //name
		readToken(); //=
		leftHandSide = new ScriptVariableReference( name, scope);
		rightHandSide = parseExpression( scope);
		return new ScriptAssignment( leftHandSide, rightHandSide);
	}

	private ScriptExpression parseExpression( ScriptScope scope) throws RuntimeException
	{
		return parseExpression( scope, null);
	}
	private ScriptExpression parseExpression( ScriptScope scope, ScriptOperator previousOper) throws RuntimeException
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
				throw new RuntimeException( "Value expected at line " + getLineNumber());
			lhs = new ScriptExpression( lhs, null, new ScriptOperator( "!"));
			}
		else
			{
			if(( lhs = parseValue( scope)) == null)
				return null;
			}

		do
		{
			try
			{
				oper = new ScriptOperator(currentToken());
			}
			catch( RuntimeException e)
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

	private ScriptExpression parseValue( ScriptScope scope) throws RuntimeException
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
				throw new RuntimeException( "')' Expected at line " + getLineNumber());
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
					throw new RuntimeException( "Digits followed by non-digits at " + getLineNumber());
				resultInt += ( resultInt * 10) + ( currentToken().charAt(i) - '0');
				}
			readToken(); //integer
			return new ScriptValue( new ScriptType( "int"), resultInt);
		}
		else if( currentToken().equals("\""))
		{
			//Directly work with line - ignore any "tokens" you meet until the string is closed
			for( i = 1; ; i++)
			{
				if( i == line.length())
				{
					throw new RuntimeException( "No closing '\"' found at line " + getLineNumber());
				}
				if( line.charAt(i) == '"')
				{
					String resultString = line.substring( 1, i);
					line = line.substring( i + 1); //+ 1 to get rid of '"' token
					return new ScriptValue( new ScriptType( "string"), resultString);
				}
			}

		}
		else if( currentToken().equals( "$"))
		{
			ScriptType type;
			readToken();
			try
			{
				type = new ScriptType( currentToken());
			}
			catch( RuntimeException e)
			{
				throw new RuntimeException( "Unknown type " + currentToken() + " at line " + getLineNumber());
			}
			readToken(); //type
			if( !currentToken().equals("["))
				throw new RuntimeException( "'[' Expected at line " + getLineNumber());
			for( i = 1; ; i++)
			{
				if( i == line.length())
				{
					throw new RuntimeException( "No closing ']' found at line " + getLineNumber());
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

	private ScriptOperator parseOperator()
	{
		ScriptOperator result;
		if( currentToken() == null)
			return null;
		try
		{
			result = new ScriptOperator( currentToken());
		}
		catch( RuntimeException e)
		{
			return null;
		}
		readToken(); //operator
		return result;
	}

	private ScriptVariableReference parseVariableReference( ScriptScope scope) throws RuntimeException
	{
		ScriptVariableReference result = null;

		if( currentToken() == null)
			return null;

		try
		{
			Identifier name = new Identifier( currentToken());
			result = new ScriptVariableReference( name, scope);

			readToken();
			return result;
		}
		catch( RuntimeException e)
		{
			return null;
		}
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


	private void printScope( ScriptScope scope, int indent) throws RuntimeException
	{
		ScriptVariable	currentVar;
		ScriptFunction	currentFunc;
		ScriptCommand	currentCommand;


		indentLine( indent);
		try
		{
			KoLmafia.getLogStream().println( "<SCOPE " + scope.getType() + ">");
		}
		catch( RuntimeException e)
		{
			KoLmafia.getLogStream().println( "<SCOPE (no return)>");
		}
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

	private void printFunction( ScriptFunction func, int indent) throws RuntimeException
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<FUNC " + func.getType().toString() + " " + func.getName().toString() + ">");
		for( ScriptVariableReference current = func.getFirstParam(); current != null; current = func.getNextParam( current))
			printVariableReference( current, indent + 1);
		printScope( func.getScope(), indent + 1);
	}

	private void printCommand( ScriptCommand command, int indent) throws RuntimeException
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

	private void printReturn( ScriptReturn ret, int indent) throws RuntimeException
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<RETURN " + ret.getType().toString() + ">");
		if( !ret.getType().equals(TYPE_VOID))
			printExpression( ret.getExpression(), indent + 1);
	}

	private void printLoop( ScriptLoop loop, int indent) throws RuntimeException
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

	private void printCall( ScriptCall call, int indent) throws RuntimeException
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<CALL " + call.getTarget().getName().toString() + ">");
		for( ScriptExpression current = call.getFirstParam(); current != null; current = call.getNextParam( current))
			printExpression( current, indent + 1);
	}

	private void printAssignment( ScriptAssignment assignment, int indent) throws RuntimeException
	{
		indentLine( indent);
		KoLmafia.getLogStream().println( "<ASSIGN " + assignment.getLeftHandSide().getName().toString() + ">");
		printExpression( assignment.getRightHandSide(), indent + 1);

	}

	private void printExpression( ScriptExpression expression, int indent) throws RuntimeException
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

	public void printValue( ScriptValue value, int indent) throws RuntimeException
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
				lineNumber++;
			}
			while ( line != null && line.trim().length() == 0 );

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

	private int getLineNumber()
	{
		return lineNumber - 1; //Parser saves one extra line for look-ahead
	}

	private class ScriptScope extends ScriptListNode
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

		public void addFunction( ScriptFunction f) throws RuntimeException
		{
			functions.addElement( f);
		}

		public void addVariable( ScriptVariable v) throws RuntimeException
		{
			variables.addElement( v);
		}

		public void addCommand( ScriptCommand c) throws RuntimeException
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

		public ScriptType getType() throws RuntimeException
		{
			ScriptCommand current = null;
			ScriptCommand previous = null;

			for( current = getFirstCommand(); current != null; previous = current, current = getNextCommand( current))
				;
			if( previous == null)
				throw new RuntimeException( "Missing return!");
			if( !( previous instanceof ScriptReturn))
				throw new RuntimeException( "Missing return!");
			return ((ScriptReturn) previous).getType();
		}

	}

	private class ScriptScopeList extends ScriptList
	{
		public void addElement( ScriptListNode n) throws RuntimeException
		{
			addElementSerial( n);
		}
	}

	private class ScriptFunction extends ScriptListNode
	{
		Identifier				name;
		ScriptType				type;
		ScriptVariableReferenceList		variables;
		ScriptScope				scope;

		public ScriptFunction( Identifier name, ScriptType type)
		{
			this.name = name;
			this.type = type;
			this.variables = new ScriptVariableReferenceList();
			this.scope = null;
		}

		public void addVariableReference( ScriptVariableReference v) throws RuntimeException
		{
			variables.addElement( v);
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

		public Identifier getName()
		{
			return name;
		}

		public ScriptVariableReference getFirstParam()
		{
			return (ScriptVariableReference) variables.getFirstElement();
		}

		public ScriptVariableReference getNextParam( ScriptVariableReference current)
		{
			return (ScriptVariableReference) variables.getNextElement( current);
		}

		public ScriptType getType()
		{
			return type;
		}
	}

	private class ScriptFunctionList extends ScriptList
	{

	}

	private class ScriptVariable extends ScriptListNode
	{
		Identifier name;
		ScriptType type;

		int contentInt;
		String contentString;

		public ScriptVariable( Identifier name, ScriptType type)
		{
			this.name = name;
			this.type = type;
			this.contentInt = 0;
			this.contentString = "";
		}

		public ScriptVariable( ScriptVariable original)
		{
			name = original.name;
			type = original.type;
			setNext( null);
		}

		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof ScriptVariable))
				throw new ClassCastException();
			return name.compareTo( (( ScriptVariable) o).name);

		}

		public ScriptType getType()
		{
			return type;
		}

		public Identifier getName()
		{
			return name;
		}
	}

	private class ScriptVariableList extends ScriptList
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

	private class ScriptVariableReference extends ScriptValue
	{
		ScriptVariable target;

		public ScriptVariableReference( ScriptVariable target) throws RuntimeException
		{
			this.target = target;
			if( !target.getType().equals( getType()))
				throw new RuntimeException( "Cannot apply " + target.getType().toString() + " to " + getType().toString() + " at line " + getLineNumber());
		}

		public ScriptVariableReference( Identifier varName, ScriptScope scope) throws RuntimeException
		{
			target = findVariable( varName, scope);
		}

		private ScriptVariable findVariable( Identifier name, ScriptScope scope) throws RuntimeException
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

			throw new RuntimeException( "Undefined variable " + name + " at line " + getLineNumber());
		}

		public ScriptType getType()
		{
			return target.getType();
		}

		public Identifier getName()
		{
			return target.getName();
		}


		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof ScriptVariableReference))
				throw new ClassCastException();
			return target.getName().compareTo( (( ScriptVariableReference) o).target.getName());

		}

	}

	private class ScriptVariableReferenceList extends ScriptList
	{
		public void addElement( ScriptListNode n) throws RuntimeException
		{
			addElementSerial( n);
		}
	}

	private class ScriptCommand extends ScriptListNode
	{
		int command;


		public ScriptCommand()
		{

		}

		public ScriptCommand( String command) throws RuntimeException
		{
			if( command.equals( "break"))
				this.command = COMMAND_BREAK;
			else if( command.equals( "continue"))
				this.command = COMMAND_CONTINUE;
			else
				throw new RuntimeException( command + " is not a command at line " + getLineNumber());
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
			if( this.command == COMMAND_BREAK)
				return "break";
			else if( this.command == COMMAND_CONTINUE)
				return "continue";
			return "<unknown command>";
		}
	}

	private class ScriptCommandList extends ScriptList
	{

		public ScriptCommandList()
			{
			super();
			}

		public ScriptCommandList( ScriptCommand c)
			{
			super( c);
			}

		public void addElement( ScriptListNode n) throws RuntimeException //Call List has to remain in original order, so override addElement
		{
			addElementSerial( n);
		}
	}

	private class ScriptReturn extends ScriptCommand
	{
		private ScriptExpression returnValue;

		public ScriptReturn( ScriptExpression returnValue, ScriptType expectedType) throws RuntimeException
		{
			this.returnValue = returnValue;
			if( !returnValue.getType().equals( expectedType))
				throw new RuntimeException( "Cannot apply " + returnValue.getType().toString() + " to " + expectedType.toString() + " at line " + getLineNumber());
		}

		public ScriptType getType()
		{
			return returnValue.getType();
		}

		public ScriptExpression getExpression()
		{
			return returnValue;
		}
	}


	private class ScriptLoop extends ScriptCommand
	{
		private boolean			repeat;
		private ScriptExpression	condition;
		private ScriptScope		scope;
		private ScriptLoopList		elseLoops;

		public ScriptLoop( ScriptScope scope, ScriptExpression condition, boolean repeat) throws RuntimeException
		{
			this.scope = scope;
			this.condition = condition;
			if( !( condition.getType().equals( TYPE_BOOLEAN)))
				throw new RuntimeException( "Cannot apply " + condition.getType().toString() + " to boolean at line " + getLineNumber());
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


		public void addElseLoop( ScriptLoop elseLoop) throws RuntimeException
		{
			if( repeat == true)
				throw new RuntimeException( "Else without if at line " + getLineNumber());
			elseLoops.addElement( elseLoop);
		}
	}


	private class ScriptLoopList extends ScriptList
	{

		public void addElement( ScriptListNode n) throws RuntimeException
		{
			addElementSerial( n);
		}
	}

	private class ScriptCall extends ScriptValue
	{
		private ScriptFunction				target;
		private ScriptExpressionList			params;

		public ScriptCall( Identifier functionName, ScriptScope scope, ScriptExpressionList params) throws RuntimeException
		{
			target = findFunction( functionName, scope, params);
			this.params = params;
		}

		private ScriptFunction findFunction( Identifier name, ScriptScope scope, ScriptExpressionList params) throws RuntimeException
		{
			ScriptFunction		current;
			ScriptVariableReference	currentParam;
			ScriptExpression	currentValue;
			int			paramIndex;

			if( scope == null)
				throw new RuntimeException( "Undefined reference " + name + " at line " + getLineNumber());
			do
			{
				for( current = scope.getFirstFunction(); current != null; current = scope.getNextFunction( current))
				{
					if( current.getName().equals( name))
					{
						for
						(
							paramIndex = 1, currentParam = current.getFirstParam(), currentValue = (ScriptExpression) params.getFirstElement();
							(currentParam != null) && (currentValue != null);
							paramIndex++, currentParam = current.getNextParam( currentParam), currentValue = ( ScriptExpression) params.getNextElement( currentValue)
						)
						{
							if( !currentParam.getType().equals( currentValue.getType()))
								throw new RuntimeException( "Illegal parameter " + paramIndex + " for function " + name + ", got " + currentValue.getType() + ", need " + currentParam.getType() + " at line " + getLineNumber());
						}
						if(( currentParam != null) || ( currentValue != null))
							throw new RuntimeException( "Illegal amount of parameters for function " + name + " at line " + getLineNumber());
						return current;
					}
				}
				scope = scope.getParentScope();
			} while( scope != null);
			throw new RuntimeException( "Undefined reference " + name + " at line " + getLineNumber());
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
	}

	private class ScriptAssignment extends ScriptCommand
	{
		private ScriptVariableReference	leftHandSide;
		private ScriptExpression	rightHandSide;

		public ScriptAssignment( ScriptVariableReference leftHandSide, ScriptExpression rightHandSide) throws RuntimeException
		{
			this.leftHandSide = leftHandSide;
			this.rightHandSide = rightHandSide;
			if( !leftHandSide.getType().equals( rightHandSide.getType()))
				throw new RuntimeException( "Cannot apply " + rightHandSide.getType().toString() + " to " + leftHandSide.toString() + " at line " + getLineNumber());
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
	}

	private class ScriptType
	{
		int type;
		public ScriptType( String s) throws RuntimeException
		{
			if( s.equals( "void"))
				type = TYPE_VOID;
			else if( s.equals( "boolean"))
				type = TYPE_BOOLEAN;
			else if( s.equals( "int"))
				type = TYPE_INT;
			else if( s.equals( "string"))
				type = TYPE_STRING;
			else if( s.equals( "item"))
				type = TYPE_ITEM;
			else if( s.equals( "zodiac"))
				type = TYPE_ZODIAC;
			else if( s.equals( "location"))
				type = TYPE_LOCATION;
			else
				throw new RuntimeException( "Wrong type identifier " + s + " at line " + getLineNumber());
		}

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
			if( type == TYPE_VOID)
				return "void";
			if( type == TYPE_BOOLEAN)
				return "boolean";
			if( type == TYPE_INT)
				return "int";
			if( type == TYPE_STRING)
				return "string";
			if( type == TYPE_ITEM)
				return "item";
			if( type == TYPE_ZODIAC)
				return "zodiac";
			if( type == TYPE_LOCATION)
				return "location";
			return "<unknown type>";
		}

	}

	private class ScriptValue extends ScriptExpression
	{
		ScriptType type;

		int contentInt;
		String contentString;

		public ScriptValue()
		{
			//stub constructor for subclasses
			//should not be called
		}


		public ScriptValue( ScriptType type, int contentInt)
		{
			this.type = type;
			this.contentInt = contentInt;
			contentString = null;
		}

		public ScriptValue( ScriptType type, String contentString)
		{
			this.type = type;
			this.contentString = contentString;
			contentInt = 0;
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
	}

	private class ScriptExpression extends ScriptCommand
	{
		ScriptExpression	lhs;
		ScriptExpression	rhs;
		ScriptOperator		oper;

		public ScriptExpression(ScriptExpression lhs, ScriptExpression rhs, ScriptOperator oper) throws RuntimeException
		{
			this.lhs = lhs;
			this.rhs = rhs;
			if(( rhs != null) && !lhs.getType().equals( rhs.getType()))
				throw new RuntimeException( "Cannot apply " + lhs.getType().toString() + " to " + rhs.getType().toString() + " at line " + getLineNumber());
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
				return new ScriptType( TYPE_BOOLEAN);
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
	}

	private class ScriptExpressionList extends ScriptList
	{
		public void addElement( ScriptListNode n) throws RuntimeException //Call List has to remain in original order, so override addElement
		{
			addElementSerial( n);
		}
	}

	private class ScriptOperator
	{
		String operString;

		public ScriptOperator( String oper) throws RuntimeException
		{
			if( oper == null)
				throw new RuntimeException( "Internal error in ScriptOperator()");
			else if
			(
				oper.equals( "!") ||
				oper.equals( "*") || oper.equals( "/") || oper.equals( "%") ||
				oper.equals( "+") || oper.equals( "-") ||
				oper.equals( "<") || oper.equals( ">") || oper.equals( "<=") || oper.equals( ">=") ||
				oper.equals( "==") || oper.equals( "!=") ||
				oper.equals( "||") || oper.equals( "&&")
			)
			{
				operString = oper;
			}
			else
				throw new RuntimeException( "Illegal operator " + oper + " at line " + getLineNumber());
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
	}


	private class ScriptListNode implements Comparable
	{
		ScriptListNode next;

		public ScriptListNode()
		{
			this.next = null;
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

	private class ScriptList
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

		public void addElement( ScriptListNode n) throws RuntimeException
		{
			ScriptListNode current;
			ScriptListNode previous = null;

			if( n.getNext() != null)
				throw new RuntimeException( "Internal error: Element already in list.");

			if( firstNode == null)
				{
				firstNode = n;
				return;
				}
			for( current = firstNode; current != null; previous = current, current = current.getNext())
			{
				if( current.compareTo( n) <= 0)
					break;
			}
			if( current == null)
				previous.setNext( n);
			else
			{
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
			}
		}

		public void addElementSerial( ScriptListNode n) throws RuntimeException //Function for subclasses to override addElement with
		{
			ScriptListNode current;
			ScriptListNode previous = null;

			if( n.getNext() != null)
				throw new RuntimeException( "Internal error: Element already in list.");

			if( firstNode == null)
				{
				firstNode = n;
				return;
				}
			for( current = firstNode; current != null; previous = current, current = current.getNext())
				;
			previous.setNext( n);

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

	private class Identifier extends ScriptListNode
	{

		private StringBuffer s;

		public Identifier ( Identifier identifier)
		{
			s = new StringBuffer();
			s = identifier.s;
		}

		Identifier( String start) throws RuntimeException
		{

			if( start.length() < 1)
				throw new RuntimeException( "Invalid Identifier - Identifier cannot be length 0. At line " + getLineNumber());

			if( !Character.isLetter( start.charAt( 0)) && (start.charAt( 0) != '_'))
				{
				throw new RuntimeException( "Invalid Identifier - Must start with a letter. At line " + getLineNumber());
			}
			s = new StringBuffer();
			s.append(start.charAt(0));
			for( int i = 1; i < start.length(); i++)
			{
				if( !Character.isLetterOrDigit( start.charAt( i)) && (start.charAt( i) != '_'))
					{
					throw new RuntimeException( "Invalid Identifier at position " + i + ": not a letter or digit. At line " + getLineNumber());
				}
				s.append(start.charAt(i));
			}
		}

		public void init( char c)
		{
			s = new StringBuffer();
			s.append( c);
			}

		public void append( char c)
		{
			s.append( c);
			}

		public char charAt (int index)
		{
			return s.charAt( index);
		}

		public boolean equals(Object o)
		{
			if(!(o instanceof Identifier))
				return false;

			return (s.toString().equals((( Identifier) o).s.toString()));
		}

		public int compareTo( Object o) throws ClassCastException
		{
			if(!(o instanceof Identifier))
				throw new ClassCastException();

			return (s.toString().compareTo((( Identifier) o).s.toString()));
		}


		public Object clone()
		{
			return new Identifier(this);
		}

		public String toString()
		{
			return s.toString();
		}
	}
}
