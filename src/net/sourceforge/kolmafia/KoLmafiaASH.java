/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.io.File;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.RelayRequest;

import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.NamespaceInterpreter;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;

import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;

public abstract class KoLmafiaASH
{
	private static final HashMap relayScriptMap = new HashMap();

	public static Interpreter relayScript = null;
	public static final RelayRequest relayRequest = new RelayRequest( false );
	public static final StringBuffer serverReplyBuffer = new StringBuffer();

	private static final HashMap TIMESTAMPS = new HashMap();
	private static final HashMap INTERPRETERS = new HashMap();

	public static final Interpreter NAMESPACE_INTERPRETER = new NamespaceInterpreter();

	public static final boolean getClientHTML( final RelayRequest request )
	{
		String script = Preferences.getString( "masterRelayOverride" );

		if ( script.length() == 0 || request.getPath().startsWith( "relay_" ) )
		{
			script = request.getBasePath();
		}

		if ( KoLmafiaASH.relayScriptMap.containsKey( script ) )
		{
			File toExecute = (File) KoLmafiaASH.relayScriptMap.get( script );
			return toExecute.exists() && KoLmafiaASH.getClientHTML( request, toExecute );
		}

		if ( !script.endsWith( ".ash" ) )
		{
			if ( !script.endsWith( ".php" ) )
			{
				return false;
			}

			script = script.substring( 0, script.length() - 4 ) + ".ash";
		}

		File toExecute = new File( KoLConstants.RELAY_LOCATION, script );
		KoLmafiaASH.relayScriptMap.put( script, toExecute );
		return toExecute.exists() && KoLmafiaASH.getClientHTML( request, toExecute );
	}

	private static final boolean getClientHTML( final RelayRequest request, final File toExecute )
	{
		synchronized ( KoLmafiaASH.serverReplyBuffer )
		{
			KoLmafiaASH.relayScript = KoLmafiaASH.getInterpreter( toExecute );
			if ( KoLmafiaASH.relayScript == null )
			{
				return false;
			}

			KoLmafiaASH.serverReplyBuffer.setLength( 0 );

			KoLmafiaASH.relayRequest.cloneURLString( request );

			KoLmafiaASH.relayScript.execute( "main", null );

			if ( KoLmafiaASH.serverReplyBuffer.length() == 0 )
			{
				if ( KoLmafiaASH.relayRequest.responseText != null && KoLmafiaASH.relayRequest.responseText.length() != 0 )
				{
					KoLmafiaASH.serverReplyBuffer.append( KoLmafiaASH.relayRequest.responseText );
				}
			}

			if ( KoLmafiaASH.serverReplyBuffer.length() != 0 )
			{
				String response = KoLmafiaASH.serverReplyBuffer.toString();
				request.pseudoResponse( "HTTP/1.1 200 OK", response );
			}

			KoLmafiaASH.relayScript = null;

			return KoLmafiaASH.serverReplyBuffer.length() != 0;
		}
	}

	public static final String getScriptHTML( final Interpreter interp, final String serverFunc, final String path )
	{
		synchronized ( KoLmafiaASH.serverReplyBuffer )
		{
			KoLmafiaASH.relayScript = interp;
			KoLmafiaASH.serverReplyBuffer.setLength( 0 );

			KoLmafiaASH.relayRequest.constructURLString( path );

			String script = Preferences.getString( "masterRelayOverride" );

			if ( script.length() == 0 || KoLmafiaASH.relayRequest.getPath().startsWith( "relay_" ) )
			{
				script = KoLmafiaASH.relayRequest.getBasePath();
			}

			int slashpos = script.lastIndexOf( "/" );
			interp.execute( serverFunc, new String[] {
				script.substring( slashpos + 1 ) }, false );

			if ( KoLmafiaASH.serverReplyBuffer.length() == 0 )
			{
				KoLmafiaASH.serverReplyBuffer.append( "<html><body>Script failed to write any output!</body></html>" );
			}

			KoLmafiaASH.relayScript = null;

			return KoLmafiaASH.serverReplyBuffer.toString();
		}
	}

	public static final Interpreter getInterpreter( final File toExecute )
	{
		if ( toExecute == null )
		{
			return null;
		}

		Interpreter interpreter;
		boolean createInterpreter = !KoLmafiaASH.TIMESTAMPS.containsKey( toExecute );

		if ( !createInterpreter )
		{
			createInterpreter =
				( (Long) KoLmafiaASH.TIMESTAMPS.get( toExecute ) ).longValue() != toExecute.lastModified();
		}

		if ( !createInterpreter )
		{
			interpreter = (Interpreter) KoLmafiaASH.INTERPRETERS.get( toExecute );
			TreeMap imports = interpreter.getImports();

			Iterator it = imports.entrySet().iterator();

			while ( it.hasNext() && !createInterpreter )
			{
				Entry entry = (Entry) it.next();
				File file = (File) entry.getKey();
				createInterpreter = ( (Long) entry.getValue() ).longValue() != file.lastModified();
			}
		}

		if ( createInterpreter )
		{
			KoLmafiaASH.TIMESTAMPS.clear();
			interpreter = new Interpreter();

			if ( !interpreter.validate( toExecute, null ) )
			{
				return null;
			}

			KoLmafiaASH.TIMESTAMPS.put( toExecute, new Long( toExecute.lastModified() ) );
			KoLmafiaASH.INTERPRETERS.put( toExecute, interpreter );
		}

		return (Interpreter) KoLmafiaASH.INTERPRETERS.get( toExecute );
	}

	public static void showUserFunctions( final Interpreter interpreter, final String filter )
	{
		KoLmafiaASH.showFunctions( interpreter.getFunctions(), filter.toLowerCase() );
	}

	public static void showExistingFunctions( final String filter )
	{
		KoLmafiaASH.showFunctions( RuntimeLibrary.getFunctions(), filter.toLowerCase() );
	}

	private static void showFunctions( final Iterator it, final String filter )
	{
		Function func;

		if ( !it.hasNext() )
		{
			RequestLogger.printLine( "No functions in your current namespace." );
			return;
		}

		boolean hasDescription = false;

		while ( it.hasNext() )
		{
			func = (Function) it.next();
			hasDescription =
				func instanceof LibraryFunction && ( (LibraryFunction) func ).getDescription() != null;

			boolean matches = filter.equals( "" );

			if ( !matches )
			{
				matches = func.getName().toLowerCase().indexOf( filter ) != -1;
			}

			Iterator it2 = func.getReferences();

			if ( !matches )
			{
				matches =
					it2.hasNext() && ( (VariableReference) it2.next() ).getType().toString().indexOf( filter ) != -1;
			}

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
			VariableReference var;

			while ( it2.hasNext() )
			{
				var = (VariableReference) it2.next();
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
				description.append( ( (LibraryFunction) func ).getDescription() );
				description.append( "<br>" );
			}

			RequestLogger.printLine( description.toString() );

		}
	}
}
