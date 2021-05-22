/*
 * Copyright (c) 2005-2021, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * <p>
 * [1] Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * [2] Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 * [3] Neither the name "KoLmafia" nor the names of its contributors may
 * be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * <p>
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

package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.textui.DataTypes;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class LibraryFunctionStub
	extends AshStub
{
	private static final long serialVersionUID = 1L;

	public LibraryFunctionStub( ScriptRuntime controller, String ashFunctionName )
	{
		super( controller, ashFunctionName );
	}

	@Override
	protected FunctionList getAllFunctions()
	{
		return RuntimeLibrary.functions;
	}

	@Override
	protected Value execute( Function function, List<Value> ashArgs )
	{
		LibraryFunction ashFunction;
		if ( function instanceof LibraryFunction )
		{
			ashFunction = (LibraryFunction) function;
		}
		else
		{
			throw controller.runtimeException( Parser.undefinedFunctionMessage( ashFunctionName, ashArgs ) );
		}

		List<Object> ashArgsWithInterpreter = new ArrayList<>( ashArgs.size() + 1 );
		ashArgsWithInterpreter.add( controller );
		ashArgsWithInterpreter.addAll( ashArgs );

		return ashFunction.executeWithoutInterpreter( controller, ashArgsWithInterpreter.toArray() );
	}

	private final int findFunctionReference( Object[] args )
	{
		int index = -1;

		switch ( ashFunctionName )
		{
		case "adventure":
		case "adv1":
			index = 2;
			break;
		case "run_combat":
			index = 0;
			break;
		default: return index;
		}

		return ( args.length >= ( index + 1 ) && args[ index ] instanceof BaseFunction ) ? index : -1;
	}

	@Override
	public Object call( Context cx, Scriptable scope, Scriptable thisObj, Object[] args )
	{
		if ( ashFunctionName.equals( "to_string" ) && args.length == 0 )
		{
			// Special case, since we accidentally override JS's built-in toString.
			return "[runtime library]";
		}

		if ( ashFunctionName.equals( "buffer_to_file" ) && args.length > 0 && args[0] instanceof String )
		{
			// Manually convert string to buffer, since AshStub.call() cannot match a string argument to a
			// buffer parameter.
			args = args.clone();
			String str = (String) args[0];
			args[0] = new Value( DataTypes.BUFFER_TYPE, str, new StringBuffer( str ) );
		}

		String temporaryName = null;
		int functionReferenceArgIndex = findFunctionReference( args );
		if ( functionReferenceArgIndex >= 0 )
		{
			BaseFunction callback = (BaseFunction) args[ functionReferenceArgIndex ];
			temporaryName = callback.toString();
			ScriptableObject.defineProperty( scope, temporaryName, callback, ScriptableObject.DONTENUM);
			args[ functionReferenceArgIndex ] = temporaryName;
		}

		Object result = super.call( cx, scope, thisObj, args );

		if ( temporaryName != null )
		{
			ScriptableObject.deleteProperty( scope, temporaryName );
		}

		return result;
	}
}
