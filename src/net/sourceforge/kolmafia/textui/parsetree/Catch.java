/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.ScriptException;

public class Catch
	extends Value
{
	private final Command node;

	public Catch( final Command node  )
	{
		super( DataTypes.STRING_TYPE );
		this.node = node;
	}

	@Override
	public Value execute( final AshRuntime interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( ScriptRuntime.State.EXIT );
			return null;
		}

		interpreter.traceIndent();
		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Evaluating catch body" );
		}

		String errorMessage = "";
		Value scopeValue = null;

		try
		{
			KoLmafia.lastMessage = "";
			scopeValue = this.node.execute( interpreter );
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

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Returning '" + errorMessage + "'" );
		}

		interpreter.traceUnindent();

		// If user aborted or exited, don't catch it
		if ( interpreter.getState() == ScriptRuntime.State.EXIT )
		{
			return null;
		}

		return new Value( errorMessage );
	}

	@Override
	public boolean assertBarrier()
	{
		return this.node.assertBarrier();
	}

	@Override
	public boolean assertBreakable()
	{
		return this.node.assertBreakable();
	}

	@Override
	public String toString()
	{
		return "catch";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		AshRuntime.indentLine( stream, indent );
		stream.println( "<CATCH>" );
		this.node.print( stream, indent + 1 );
	}
}
