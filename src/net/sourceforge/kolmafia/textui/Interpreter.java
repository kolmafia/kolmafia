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
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.TreeMap;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.NullStream;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptType;
import net.sourceforge.kolmafia.textui.DataTypes.ScriptValue;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.Parser.AdvancedScriptException;
import net.sourceforge.kolmafia.textui.ParseTree;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptFunction;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptScope;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptVariableList;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptVariableReference;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.SendMailRequest;

public class Interpreter
{
	private Parser parser;
	private ScriptScope scope;

	// Variables used during execution

	public static final String STATE_NORMAL = "NORMAL";
	public static final String STATE_RETURN = "RETURN";
	public static final String STATE_BREAK = "BREAK";
	public static final String STATE_CONTINUE = "CONTINUE";
	public static final String STATE_EXIT = "EXIT";

	private static String lastImportString = "";

	private String currentState = Interpreter.STATE_NORMAL;
	private PrintStream traceStream = NullStream.INSTANCE;
	private int traceIndentation = 0;

	public Interpreter()
	{
		this.parser = new Parser();
		this.scope = new ScriptScope( new ScriptVariableList(), Parser.getExistingFunctionScope() );
	}

	private Interpreter( final Interpreter source, final File scriptFile )
	{
		this.parser = new Parser( scriptFile, null, source.getImports() );
		this.scope = source.scope;
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

	private static final String indentation = " " + " " + " ";
	public static final void indentLine( final PrintStream stream, final int indent )
	{
		if ( stream != null )
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
			this.printScope( this.scope );
			return true;
		}
		catch ( AdvancedScriptException e )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, e.getMessage() );
			return false;
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return false;
		}
	}

	public void execute( final String functionName, final String[] parameters )
	{
		// Before you do anything, validate the script, if one
		// is provided.	 One will not be provided in the event
		// that we are using a global namespace.

		if ( this == KoLmafiaASH.NAMESPACE_INTERPRETER )
		{
			String importString = Preferences.getString( "commandLineNamespace" );
			if ( importString.equals( "" ) )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, "No available namespace with function: " + functionName );
				return;
			}

			TreeMap imports = this.parser.getImports();
			boolean shouldRefresh = !Interpreter.lastImportString.equals( importString );

			if ( !shouldRefresh )
			{
				Iterator it = imports.keySet().iterator();

				while ( it.hasNext() && !shouldRefresh )
				{
					File file = (File) it.next();
					shouldRefresh = ( (Long) imports.get( file ) ).longValue() != file.lastModified();
				}
			}

			if ( shouldRefresh )
			{
				imports.clear();
				Interpreter.lastImportString = "";

				this.scope = new ScriptScope( new ScriptVariableList(), Parser.getExistingFunctionScope() );
				String[] importList = importString.split( "," );

				for ( int i = 0; i < importList.length; ++i )
				{
					this.parser.importFile( importList[ i ], this.scope );
				}
			}
		}

		String currentScript = this.getFileName() == null ? "<>" : "<" + this.getFileName() + ">";
		String notifyList = Preferences.getString( "previousNotifyList" );
		String notifyRecipient = this.parser.getNotifyRecipient();

		if ( notifyRecipient != null && notifyList.indexOf( currentScript ) == -1 )
		{
			Preferences.setString( "previousNotifyList", notifyList + currentScript );

			SendMailRequest notifier = new SendMailRequest( notifyRecipient, this );
			RequestThread.postRequest( notifier );
		}

		try
		{
			this.executeScope( this.scope, functionName, parameters );
		}
		catch ( AdvancedScriptException e )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, e.getMessage() );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "", true );
		}
	}

	private ScriptValue executeScope( final ScriptScope topScope, final String functionName, final String[] parameters )
	{
		ScriptFunction main;
		ScriptValue result = null;

		this.currentState = Interpreter.STATE_NORMAL;
		this.resetTracing();

		main =
			functionName.equals( "main" ) ? this.parser.getMainMethod() : topScope.findFunction( functionName, parameters != null );

		if ( main == null && !topScope.getCommands().hasNext() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to invoke " + functionName );
			return DataTypes.VOID_VALUE;
		}

		// First execute top-level commands;

		boolean executeTopLevel = this != KoLmafiaASH.NAMESPACE_INTERPRETER;

		if ( !executeTopLevel )
		{
			String importString = Preferences.getString( "commandLineNamespace" );
			executeTopLevel = !importString.equals( Interpreter.lastImportString );
			Interpreter.lastImportString = importString;
		}

		if ( executeTopLevel )
		{
			this.trace( "Executing top-level commands" );
			result = topScope.execute( this );
		}

		if ( this.currentState == Interpreter.STATE_EXIT )
		{
			return result;
		}

		// Now execute main function, if any
		if ( main != null )
		{
			this.trace( "Executing main function" );

			if ( !this.requestUserParams( main, parameters ) )
			{
				return null;
			}

			result = main.execute( this );
		}

		return result;
	}

	private boolean requestUserParams( final ScriptFunction targetFunction, final String[] parameters )
	{
		int args = parameters == null ? 0 : parameters.length;

		ScriptType lastType = null;
		ScriptVariableReference lastParam = null;

		int index = 0;

		Iterator it = targetFunction.getReferences();
		ScriptVariableReference param;

		while ( it.hasNext() )
		{
			param = (ScriptVariableReference) it.next();

			ScriptType type = param.getType();
			String name = param.getName();
			ScriptValue value = null;

			while ( value == null )
			{
				if ( type == DataTypes.VOID_TYPE )
				{
					value = DataTypes.VOID_VALUE;
					break;
				}

				String input = null;

				if ( index >= args )
				{
					input = DataTypes.promptForValue( type, name );
				}
				else
				{
					input = parameters[ index ];
				}

				// User declined to supply a parameter
				if ( input == null )
				{
					return false;
				}

				value = DataTypes.parseValue( type, input, false );
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

			param.setValue( this, value );

			lastType = type;
			lastParam = param;

			index++ ;
		}

		if ( index < args && lastParam != null )
		{
			StringBuffer inputs = new StringBuffer();
			for ( int i = index - 1; i < args; ++i )
			{
				inputs.append( parameters[ i ] + " " );
			}

			ScriptValue value = DataTypes.parseValue( lastType, inputs.toString().trim(), true );
			lastParam.setValue( this, value );
		}

		return true;
	}

	// **************** Debug printing *****************

	private void printScope( final ScriptScope scope )
	{
		if ( scope == null )
		{
			return;
		}

		PrintStream stream = this.traceStream;
		scope.print( stream, 0 );

		ScriptFunction mainMethod = this.parser.getMainMethod();
		if ( mainMethod != null )
		{
			this.indentLine( 1 );
			stream.println( "<MAIN>" );
			mainMethod.print( stream, 2 );
		}
	}

	// **************** Tracing *****************

	private final void indentLine( final int indent )
	{
		Interpreter.indentLine( this.traceStream, indent );
	}

	public final void resetTracing()
	{
		this.traceIndentation = 0;
		this.traceStream = RequestLogger.getDebugStream();
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
		this.indentLine( this.traceIndentation );
		this.traceStream.println( string );
	}
}
