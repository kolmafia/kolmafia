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

package net.sourceforge.kolmafia;

import java.io.File;
import java.util.Iterator;
import java.util.TreeMap;

import net.sourceforge.kolmafia.ASH.Interpreter;
import net.sourceforge.kolmafia.request.RelayRequest;

public abstract class KoLmafiaASH
{
	private static final TreeMap relayScriptMap = new TreeMap();

	public static Interpreter relayScript = null;
	public static final RelayRequest relayRequest = new RelayRequest( false );
	public static final StringBuffer serverReplyBuffer = new StringBuffer();

	private static final TreeMap TIMESTAMPS = new TreeMap();
	private static final TreeMap INTERPRETERS = new TreeMap();

	public static final Interpreter NAMESPACE_INTERPRETER = new Interpreter();

	public static final boolean getClientHTML( final RelayRequest request )
	{
		String script = request.getPath();

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
		KoLmafiaASH.relayScriptMap.put( request.getPath(), toExecute );
		return toExecute.exists() && KoLmafiaASH.getClientHTML( request, toExecute );
	}

	private static final boolean getClientHTML( final RelayRequest request, final File toExecute )
	{
		synchronized ( KoLmafiaASH.serverReplyBuffer )
		{
			KoLmafiaASH.relayScript = KoLmafiaASH.getInterpreter( (File) toExecute );
			if ( KoLmafiaASH.relayScript == null )
			{
				return false;
			}

			KoLmafiaASH.serverReplyBuffer.setLength( 0 );
			KoLmafiaASH.relayRequest.constructURLString( request.getURLString() );

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
				request.pseudoResponse( "HTTP/1.1 200 OK", KoLmafiaASH.serverReplyBuffer.toString() );
			}

			KoLmafiaASH.relayScript = null;
			return KoLmafiaASH.serverReplyBuffer.length() != 0;
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
			Iterator it = imports.keySet().iterator();

			while ( it.hasNext() && !createInterpreter )
			{
				File file = (File) it.next();
				createInterpreter = ( (Long) imports.get( file ) ).longValue() != file.lastModified();
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
}
