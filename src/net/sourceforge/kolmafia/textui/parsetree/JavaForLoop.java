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

import java.util.List;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class JavaForLoop
	extends Loop
{
	private final List<Assignment> initializers;
	private final Value condition;
	private final List<Command> incrementers;

	public JavaForLoop( final Scope scope,
			    final List<Assignment> initializers,
			    final Value condition,
			    final List<Command> incrementers )
	{
		super( scope );
		this.initializers = initializers;
		this.condition = condition;
		this.incrementers = incrementers;
	}

	public Value getCondition()
	{
		return this.condition;
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
			interpreter.trace( this.toString() );
		}

		// For all variable references, bind to initial value
		for ( Assignment initializer : initializers )
		{
			if ( ScriptRuntime.isTracing() )
			{
				interpreter.trace( "Initialize: " + initializer.getLeftHandSide() );
			}

			Value value = initializer.execute( interpreter );
			interpreter.captureValue( value );

			if ( value == null )
			{
				value = DataTypes.VOID_VALUE;
			}

			if ( ScriptRuntime.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + value );
			}

			if ( interpreter.getState() == ScriptRuntime.State.EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}
		}

		while ( true )
		{
			// Test the exit condition
			if ( ScriptRuntime.isTracing() )
			{
				interpreter.trace( "Test: " + this.condition );
			}

			Value conditionResult = this.condition.execute( interpreter );
			interpreter.captureValue( conditionResult );

			if ( ScriptRuntime.isTracing() )
			{
				interpreter.trace( "[" + interpreter.getState() + "] <- " + conditionResult );
			}

			if ( conditionResult == null )
			{
				interpreter.traceUnindent();
				return null;
			}

			if ( conditionResult.intValue() != 1 )
			{
				break;
			}

			// Execute the loop body
			Value result = super.execute( interpreter );

			if ( interpreter.getState() == ScriptRuntime.State.BREAK )
			{
				interpreter.setState( ScriptRuntime.State.NORMAL );
				interpreter.traceUnindent();
				return DataTypes.VOID_VALUE;
			}

			if ( interpreter.getState() != ScriptRuntime.State.NORMAL )
			{
				interpreter.traceUnindent();
				return result;
			}

			// Execute incrementers
			for ( Command incrementer : this.incrementers )
			{
				Value iresult = incrementer.execute( interpreter );

				// Abort processing now if command failed
				if ( !KoLmafia.permitsContinue() )
				{
					interpreter.setState( ScriptRuntime.State.EXIT );
				}

				if ( ScriptRuntime.isTracing() )
				{
					interpreter.trace( "[" + interpreter.getState() + "] <- " + iresult.toQuotedString() );
				}

				if ( interpreter.getState() != ScriptRuntime.State.NORMAL )
				{
					interpreter.traceUnindent();
					return DataTypes.VOID_VALUE;
				}
			}
		}

		interpreter.traceUnindent();
		return DataTypes.VOID_VALUE;
	}

	@Override
	public String toString()
	{
		return "for";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		AshRuntime.indentLine( stream, indent );
		stream.println( "<FOR>" );

		for ( Assignment initializer : initializers )
		{
			initializer.print( stream, indent + 1 );
		}
		this.getCondition().print( stream, indent + 1 );
		for ( Command incrementer : this.incrementers )
		{
			AshRuntime.indentLine( stream, indent + 1 );
			stream.println( "<ITERATE>" );
			incrementer.print( stream, indent + 2 );
		}
		this.getScope().print( stream, indent + 1 );
	}
}
