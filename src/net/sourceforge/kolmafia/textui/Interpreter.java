/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import java.io.InputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.TreeMap;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;

import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.ValueList;
import net.sourceforge.kolmafia.textui.parsetree.VariableList;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;

import net.sourceforge.kolmafia.utilities.CharacterEntities;
import net.sourceforge.kolmafia.utilities.NullStream;

public class Interpreter
{
	protected Parser parser;
	protected Scope scope;

	// Variables used during execution

	public static final String STATE_NORMAL = "NORMAL";
	public static final String STATE_RETURN = "RETURN";
	public static final String STATE_BREAK = "BREAK";
	public static final String STATE_CONTINUE = "CONTINUE";
	public static final String STATE_EXIT = "EXIT";

	private static Stack interpreterStack = new Stack();

	private String currentState = Interpreter.STATE_NORMAL;
	private int traceIndentation = 0;
	public Profiler profiler;

	// key, then aggregate, then iterator for every active foreach loop
	public ArrayList iterators = new ArrayList();

	// For use in runtime error messages
	private String fileName;
	private int lineNumber;

	// For use in LibraryFunction return values
	private boolean hadPendingState;

	// For use by RuntimeLibrary's CLI command batching feature
	LinkedHashMap<String, StringBuffer> batched;

	// For use in ASH relay scripts
	private RelayRequest relayRequest = null;
	private StringBuffer serverReplyBuffer = null;

	// GLOBAL control of tracing
	private static PrintStream traceStream = NullStream.INSTANCE;

	public static boolean isTracing()
	{
		return Interpreter.traceStream != NullStream.INSTANCE;
	}

	public static final void openTraceStream()
	{
		Interpreter.traceStream =
			RequestLogger.openStream( "ASH_" + KoLConstants.DAILY_FORMAT.format( new Date() ) + ".txt", Interpreter.traceStream, true );
	}

	public static final void closeTraceStream()
	{
		RequestLogger.closeStream( Interpreter.traceStream );
		Interpreter.traceStream = NullStream.INSTANCE;
	}

	public Interpreter()
	{
		this.parser = new Parser();
		this.scope = new Scope( new VariableList(), Parser.getExistingFunctionScope() );
		this.hadPendingState = false;
	}

	private Interpreter( final Interpreter source, final File scriptFile )
	{
		this.parser = new Parser( scriptFile, null, source.getImports() );
		this.scope = source.scope;
		this.hadPendingState = false;
	}

	public void initializeRelayScript( final RelayRequest request )
	{
		this.relayRequest = request;
		if ( this.serverReplyBuffer == null )
		{
			this.serverReplyBuffer = new StringBuffer();
		}
		else
		{
			this.serverReplyBuffer.setLength( 0 );
		}

		// Allow a relay script to execute regardless of error state
		KoLmafia.forceContinue();
	}

	public RelayRequest getRelayRequest()
	{
		return this.relayRequest;
	}

	public StringBuffer getServerReplyBuffer()
	{
		return this.serverReplyBuffer;
	}

	public void finishRelayScript()
	{
		this.relayRequest = null;
		this.serverReplyBuffer = null;
	}

	public void cloneRelayScript( final Interpreter caller )
	{
		this.finishRelayScript();
		if ( caller != null )
		{
			this.relayRequest = caller.getRelayRequest();
			this.serverReplyBuffer = caller.getServerReplyBuffer();
		}
	}

	public Parser getParser()
	{
		return this.parser;
	}

	public String getFileName()
	{
		return this.parser.getFileName();
	}

	public TreeMap getImports()
	{
		return this.parser.getImports();
	}

	public Iterator getFunctions()
	{
		return this.scope.getFunctions();
	}

	public String getState()
	{
		return this.currentState;
	}

	public void setState( final String state )
	{
		this.currentState = state;
	}

	public static void rememberPendingState()
	{
		if ( Interpreter.interpreterStack.isEmpty() )
		{
			return;
		}

		Interpreter current = (Interpreter) Interpreter.interpreterStack.peek();

		current.hadPendingState = true;
	}

	public static void forgetPendingState()
	{
		if ( Interpreter.interpreterStack.isEmpty() )
		{
			return;
		}

		Interpreter current = (Interpreter) Interpreter.interpreterStack.peek();

		current.hadPendingState = false;
	}

	public static boolean getContinueValue()
	{
		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		if ( Interpreter.interpreterStack.isEmpty() )
		{
			return true;
		}

		Interpreter current = (Interpreter) Interpreter.interpreterStack.peek();

		return !current.hadPendingState;
	}

	public void setLineAndFile( final String fileName, final int lineNumber )
	{
		this.fileName = fileName;
		this.lineNumber = lineNumber;
	}

	private static final String indentation = " " + " " + " ";
	public static final void indentLine( final PrintStream stream, final int indent )
	{
		if ( stream != null && stream != NullStream.INSTANCE )
		{
			for ( int i = 0; i < indent; ++i )
			{
				stream.print( indentation );
			}
		}
	}

	// **************** Parsing and execution *****************

	public boolean validate( final File scriptFile, final InputStream stream )
	{
		try
		{
			this.parser = new Parser( scriptFile, stream, null );
			this.scope = parser.parse();
			this.resetTracing();
			if ( Interpreter.isTracing() )
			{
				this.printScope( this.scope );
			}
			return true;
		}
		catch ( ScriptException e )
		{
			String message = CharacterEntities.escape( e.getMessage() );
			KoLmafia.updateDisplay( MafiaState.ERROR, message );
			return false;
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	public Value execute( final String functionName, final String[] parameters )
	{
		String currentScript = this.getFileName() == null ? "<>" : "<" + this.getFileName() + ">";
		String notifyList = Preferences.getString( "previousNotifyList" );
		String notifyRecipient = this.parser.getNotifyRecipient();

		if ( notifyRecipient != null && notifyList.indexOf( currentScript ) == -1 )
		{
			Preferences.setString( "previousNotifyList", notifyList + currentScript );

			SendMailRequest notifier = new SendMailRequest( notifyRecipient, this );
			RequestThread.postRequest( notifier );
		}

		return this.execute( functionName, parameters, true );
	}

	public Value execute( final String functionName, final String[] parameters, final boolean executeTopLevel )
	{
		try
		{
			return this.executeScope( this.scope, functionName, parameters, executeTopLevel );
		}
		catch ( ScriptException e )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, e.getMessage() );
		}
		catch ( StackOverflowError e )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Stack overflow during ASH script: " + Parser.getLineAndFile( this.fileName, this.lineNumber ) );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "", true );
			KoLmafia.updateDisplay( MafiaState.ERROR, "Script execution aborted (" + e.getMessage() + "): " + Parser.getLineAndFile( this.fileName, this.lineNumber ) );
		}
		return DataTypes.VOID_VALUE;
	}

	private Value executeScope( final Scope topScope, final String functionName, final String[] parameters,
				    final boolean executeTopLevel )
	{
		Function main;
		Value result = null;

		Interpreter.interpreterStack.push( this );

		this.currentState = Interpreter.STATE_NORMAL;
		this.resetTracing();

		if ( functionName.equals( "main" ) )
		{
			main = this.parser.getMainMethod();
		}
		else
		{
			main = topScope.findFunction( functionName, parameters != null );

			if ( main == null && !topScope.getCommands().hasNext() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to invoke " + functionName );
				return DataTypes.VOID_VALUE;
			}
		}

		// First execute top-level commands;

		if ( executeTopLevel )
		{
			if ( Interpreter.isTracing() )
			{
				this.trace( "Executing top-level commands" );
			}
			result = topScope.execute( this );
		}

		if ( this.currentState == Interpreter.STATE_EXIT )
		{
			return result;
		}

		// Now execute main function, if any
		if ( main != null )
		{
			if ( Interpreter.isTracing() )
			{
				this.trace( "Executing main function" );
			}

			Object[] values = new Object[ main.variableReferences.size() + 1];
			values[ 0 ] = this;
			
			if ( !this.requestUserParams( main, parameters, values ) )
			{
				return null;
			}

			result = main.execute( this, values );
		}

		Interpreter.interpreterStack.pop();

		return result;
	}

	private boolean requestUserParams( final Function targetFunction, final String[] parameters, Object[] values )
	{
		int args = parameters == null ? 0 : parameters.length;
		Iterator it = targetFunction.getReferences();
		Type type = null;
		int index = 0;

		while ( it.hasNext() )
		{
			VariableReference param = (VariableReference) it.next();
			type = param.getType();

			String name = param.getName();
			Value value = null;

			while ( value == null )
			{
				if ( type == DataTypes.VOID_TYPE )
				{
					value = DataTypes.VOID_VALUE;
					break;
				}

				String input = ( index >= args ) ?
					DataTypes.promptForValue( type, name ) :
					parameters[ index ];

				// User declined to supply a parameter
				if ( input == null )
				{
					return false;
				}

				try
				{
					value = DataTypes.parseValue( type, input, false );
				}
				catch ( Exception e )
				{
					value = null;
				}

				if ( value == null )
				{
					RequestLogger.printLine( "Bad " + type.toString() + " value: \"" + input + "\"" );

					// Punt if parameter came from the CLI
					if ( index < args )
					{
						return false;
					}
				}
			}

			values[ ++index ] = value;
		}

		if ( index < args && type != null )
		{
			StringBuilder inputs = new StringBuilder();
			for ( int i = index - 1; i < args; ++i )
			{
				inputs.append( parameters[ i ] );
				inputs.append( " " );
			}

			Value value = DataTypes.parseValue( type, inputs.toString().trim(), true );
			values[ index ] = value;
		}

		return true;
	}

	// **************** Debug printing *****************

	private void printScope( final Scope scope )
	{
		if ( scope == null )
		{
			return;
		}

		PrintStream stream = this.traceStream;
		scope.print( stream, 0 );

		Function mainMethod = this.parser.getMainMethod();
		if ( mainMethod != null )
		{
			this.indentLine( 1 );
			stream.println( "<MAIN>" );
			mainMethod.print( stream, 2 );
		}
	}

	// **************** Tracing *****************

	public final void resetTracing()
	{
		this.traceIndentation = 0;
		this.traceStream = Interpreter.traceStream;
	}

	private final void indentLine( final int indent )
	{
		if ( this.isTracing() )
		{
			Interpreter.indentLine( this.traceStream, indent );
		}
	}

	public final void traceIndent()
	{
		this.traceIndentation++ ;
	}

	public final void traceUnindent()
	{
		this.traceIndentation-- ;
	}

	public final void trace( final String string )
	{
		if ( Interpreter.isTracing() )
		{
			this.indentLine( this.traceIndentation );
			this.traceStream.println( string );
		}
	}

	public final void captureValue( final Value value )
	{
		// We've just executed a command in a context that captures the
		// return value.

		if ( KoLmafia.refusesContinue() || value == null )
		{
			// User aborted
			this.setState( STATE_EXIT );
			return;
		}

		// Even if an error occurred, since we captured the result,
		// permit further execution.

		this.setState( STATE_NORMAL );
		KoLmafia.forceContinue();
	}

	public final ScriptException runtimeException( final String message )
	{
		return Interpreter.runtimeException( message, this.fileName, this.lineNumber );
	}

	public static final ScriptException runtimeException( final String message, final String fileName, final int lineNumber )
	{
		return new ScriptException( message + " " + Parser.getLineAndFile( fileName, lineNumber ) );
	}

	public final ScriptException undefinedFunctionException( final String name, final ValueList params )
	{
		return this.runtimeException( Parser.undefinedFunctionMessage( name, params ) );
	}
}
