/**
 * Copyright (c) 2005-2020, KoLmafia development team
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

package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Interpreter.InterpreterState;
import net.sourceforge.kolmafia.textui.ScriptException;

public class CatchValue
        extends Value
{
	private final Value value;

	public CatchValue( final Value value )
	{
		this.value = value;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( InterpreterState.EXIT );
			return null;
		}

		interpreter.traceIndent();
		if ( Interpreter.isTracing() )
		{
			interpreter.trace( "Evaluating catch value" );
		}

		String errorMessage = "";
		Value scopeValue = null;

		try
		{
			KoLmafia.lastMessage = "";
			scopeValue = this.value.execute( interpreter );
		}
		catch ( ScriptException se )
		{
			errorMessage = "SCRIPT: " + se.getMessage();
		}
		catch ( Exception e )
		{
			errorMessage = "JAVA: " + e.getMessage();
		}

		// We may have thrown and caught an error within the catch block.
		// Return message only if currently cannot continue.
		if ( errorMessage.equals( "" ) && !KoLmafia.permitsContinue() )
		{
			// Capture the value, permitting continuation
			errorMessage = "CAPTURE: " + KoLmafia.lastMessage;
			interpreter.captureValue( scopeValue );
		}

		if ( Interpreter.isTracing() )
		{
			interpreter.trace( "Returning '" + errorMessage + "'" );
		}

		interpreter.traceUnindent();

		// If user aborted or exited, don't catch it
		if ( interpreter.getState() == InterpreterState.EXIT )
		{
			return null;
		}

		return new Value( errorMessage );
	}

	@Override
	public String toString()
	{
	    return "catch " + this.value.toString() ;
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<CATCH>" );
		this.value.print( stream, indent + 1 );
	}
}
