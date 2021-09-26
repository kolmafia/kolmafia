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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class Try
	extends Command
{
	private final Scope body, finalClause;

	public Try( final Scope body, final Scope finalClause )
	{
		this.body = body;
		this.finalClause = finalClause;
	}

	@Override
	public Value execute( final AshRuntime interpreter )
	{
		// We can't catch script ABORTs
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( ScriptRuntime.State.EXIT );
			return null;
		}

		Value result = DataTypes.VOID_VALUE;
		interpreter.traceIndent();
		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Entering try body" );
		}

		try
		{
			result = this.body.execute( interpreter );
		}
		catch ( Exception e )
		{
			// *** Here is where we wuld look at catch blocks and find which, if any
			// *** will handle this exception.
			//
			// If no catch block swallows this error, propagate it upwards
			interpreter.traceUnindent();
			throw e;
		}
		finally
		{
			if ( this.finalClause != null )
			{
				ScriptRuntime.State oldState = interpreter.getState();
				boolean userAborted = StaticEntity.userAborted;
				MafiaState continuationState = StaticEntity.getContinuationState();

				KoLmafia.forceContinue();
				interpreter.setState( ScriptRuntime.State.NORMAL );

				if ( ScriptRuntime.isTracing() )
				{
					interpreter.trace( "Entering finally, saved state: " + oldState );
				}

				this.finalClause.execute( interpreter );

				// Unless the finally block aborted, restore previous state
				if ( !KoLmafia.refusesContinue() )
				{
					interpreter.setState( oldState );
					StaticEntity.setContinuationState( continuationState );
					StaticEntity.userAborted = userAborted;
				}
			}
		}

		interpreter.traceUnindent();
		return result;
	}

	@Override
	public boolean assertBarrier()
	{
		return this.body.assertBarrier() || this.finalClause.assertBarrier();
	}

	@Override
	public boolean assertBreakable()
	{
		return this.body.assertBreakable() || this.finalClause.assertBreakable();
	}

	@Override
	public String toString()
	{
		return "try";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		AshRuntime.indentLine( stream, indent );
		stream.println( "<TRY>" );

		this.body.print( stream, indent + 1 );

		if ( this.finalClause != null )
		{
			AshRuntime.indentLine( stream, indent );
			stream.println( "<FINALLY>" );

			this.finalClause.print( stream, indent + 1 );
		}
	}
}
