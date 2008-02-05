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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.TreeMap;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ASH.DataTypes;
import net.sourceforge.kolmafia.ASH.Parser;
import net.sourceforge.kolmafia.ASH.Parser.AdvancedScriptException;
import net.sourceforge.kolmafia.ASH.ParseTree;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptExistingFunction;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptFunction;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptScope;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptType;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptValue;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptVariableList;
import net.sourceforge.kolmafia.ASH.ParseTree.ScriptVariableReference;
import net.sourceforge.kolmafia.ASH.RuntimeLibrary;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

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

	public static String currentState = Interpreter.STATE_NORMAL;
	public static boolean isExecuting = false;
	private static String lastImportString = "";

	public Interpreter()
	{
		this.scope = new ScriptScope( new ScriptVariableList(), Parser.getExistingFunctionScope() );
	}

	private Interpreter( final Interpreter source, final File scriptFile )
	{
		this.scope = source.scope;
		this.parser = new Parser( scriptFile, null, source.getImports() );
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

	// **************** Parsing and execution *****************

	public boolean validate( final File scriptFile, final InputStream stream )
	{
		try
		{
			this.parser = new Parser( scriptFile, stream, null );
			return this.validate();
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

	private boolean validate()
	{
                this.scope = parser.parse();
                this.printScope( this.scope );
		return true;
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
			boolean wasExecuting = Interpreter.isExecuting;

			Interpreter.isExecuting = true;
			this.executeScope( this.scope, functionName, parameters );
			Interpreter.isExecuting = wasExecuting;
		}
		catch ( AdvancedScriptException e )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, e.getMessage() );
			return;
		}
		catch ( RuntimeException e )
		{
			// If it's an exception resulting from
			// a premature abort, which causes void
			// values to be returned, ignore.

			StaticEntity.printStackTrace( e );
		}
	}

	private ScriptValue executeScope( final ScriptScope topScope, final String functionName, final String[] parameters )
	{
		ScriptFunction main;
		ScriptValue result = null;

		Interpreter.currentState = Interpreter.STATE_NORMAL;
		Interpreter.resetTracing();

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
			Interpreter.trace( "Executing top-level commands" );
			result = topScope.execute();
		}

		if ( Interpreter.currentState == Interpreter.STATE_EXIT )
		{
			return result;
		}

		// Now execute main function, if any
		if ( main != null )
		{
			Interpreter.trace( "Executing main function" );

			if ( !this.requestUserParams( main, parameters ) )
			{
				return null;
			}

			result = main.execute();
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

				try
				{
					value = DataTypes.parseValue( type, input );
				}
				catch ( AdvancedScriptException e )
				{
					RequestLogger.printLine( e.getMessage() );

					// Punt if parameter came from the CLI
					if ( index < args )
					{
						return false;
					}
				}
			}

			param.setValue( value );

			lastType = type;
			lastParam = param;

			index++ ;
		}

		if ( index < args )
		{
			StringBuffer inputs = new StringBuffer();
			for ( int i = index - 1; i < args; ++i )
			{
				inputs.append( parameters[ i ] + " " );
			}

			ScriptValue value = DataTypes.parseValue( lastType, inputs.toString().trim() );
			lastParam.setValue( value );
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

		PrintStream stream = RequestLogger.getDebugStream();
		scope.print( stream, 0 );

		ScriptFunction mainMethod = this.parser.getMainMethod();
		if ( mainMethod != null )
		{
			Interpreter.indentLine( 1 );
			stream.println( "<MAIN>" );
			mainMethod.print( stream, 2 );
		}
	}

	public void showUserFunctions( final String filter )
	{
		this.showFunctions( this.scope.getFunctions(), filter.toLowerCase() );
	}

	public void showExistingFunctions( final String filter )
	{
		this.showFunctions( RuntimeLibrary.functions.iterator(), filter.toLowerCase() );
	}

	private void showFunctions( final Iterator it, final String filter )
	{
		ScriptFunction func;

		if ( !it.hasNext() )
		{
			RequestLogger.printLine( "No functions in your current namespace." );
			return;
		}

		boolean hasDescription = false;

		while ( it.hasNext() )
		{
			func = (ScriptFunction) it.next();
			hasDescription =
				func instanceof ScriptExistingFunction && ( (ScriptExistingFunction) func ).getDescription() != null;

			boolean matches = filter.equals( "" );
			matches |= func.getName().toLowerCase().indexOf( filter ) != -1;

			Iterator it2 = func.getReferences();
			matches |=
				it2.hasNext() && ( (ScriptVariableReference) it2.next() ).getType().toString().indexOf( filter ) != -1;

			if ( !matches )
			{
				continue;
			}

			StringBuffer description = new StringBuffer();

			if ( hasDescription )
			{
				description.append( "<b>" );
			}

			description.append( func.getType() );
			description.append( " " );
			description.append( func.getName() );
			description.append( "( " );

			it2 = func.getReferences();
			ScriptVariableReference var;

			while ( it2.hasNext() )
			{
				var = (ScriptVariableReference) it2.next();
				description.append( var.getType() );

				if ( var.getName() != null )
				{
					description.append( " " );
					description.append( var.getName() );
				}

				if ( it2.hasNext() )
				{
					description.append( ", " );
				}
			}

			description.append( " )" );

			if ( hasDescription )
			{
				description.append( "</b><br>" );
				description.append( ( (ScriptExistingFunction) func ).getDescription() );
				description.append( "<br>" );
			}

			RequestLogger.printLine( description.toString() );

		}
	}

	// **************** Tracing *****************

	private static final void indentLine( final int indent )
	{
		for ( int i = 0; i < indent; ++i )
		{
			RequestLogger.getDebugStream().print( "   " );
		}
	}

	private static int traceIndentation = 0;

	public static final void resetTracing()
	{
		Interpreter.traceIndentation = 0;
	}

	public static final void traceIndent()
	{
		Interpreter.traceIndentation++ ;
	}

	public static final void traceUnindent()
	{
		Interpreter.traceIndentation-- ;
	}

	public static final void trace( final String string )
	{
		Interpreter.indentLine( Interpreter.traceIndentation );
		RequestLogger.updateDebugLog( string );
	}
}
