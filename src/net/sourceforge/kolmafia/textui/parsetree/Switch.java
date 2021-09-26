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
import java.util.Map;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class Switch
	extends Command
{
	private final Value condition;
	private final Value [] tests;
	private final Integer [] offsets;
	private final int defaultIndex;
	private final SwitchScope scope;
	private final Map<Value, Integer> labels;

	public Switch( final Value condition, final List<Value> tests, final List<Integer> offsets, final int defaultIndex, final SwitchScope scope, final Map<Value, Integer> labels )
	{
		this.condition = condition;
		this.tests = tests.toArray( new Value[tests.size()] );
		this.offsets = offsets.toArray(new Integer[offsets.size()] );
		this.defaultIndex = defaultIndex;
		this.scope = scope;
		this.labels = labels;
	}

	public Value getCondition()
	{
		return this.condition;
	}

	public SwitchScope getScope()
	{
		return this.scope;
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

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "Value: " + this.condition );
		}

		Value value = this.condition.execute( interpreter );
		interpreter.captureValue( value );

		if ( ScriptRuntime.isTracing() )
		{
			interpreter.trace( "[" + interpreter.getState() + "] <- " + value );
		}

		if ( value == null )
		{
			interpreter.traceUnindent();
			return null;
		}

		int offset = this.defaultIndex;

		if ( labels != null )
		{
			Integer mapped = labels.get( value );
			if ( mapped != null )
			{
				offset = mapped.intValue();
			}
		}
		else
		{
			for ( int index = 0; index < tests.length; ++index )
			{
				Value test = tests[index];
				if ( ScriptRuntime.isTracing() )
				{
					interpreter.trace( "test: " + test );
				}

				Value result = test.execute( interpreter );
				interpreter.captureValue( result );

				if ( ScriptRuntime.isTracing() )
				{
					interpreter.trace( "[" + interpreter.getState() + "] <- " + result );
				}

				if ( result == null )
				{
					interpreter.traceUnindent();
					return null;
				}

				if ( result.equals( value ) )
				{
					offset = offsets[ index ].intValue();
					break;
				}
			}
		}

		if ( offset >= 0 && offset < this.scope.commandCount() )
		{
			this.scope.setOffset( offset );
			Value result = this.scope.execute( interpreter );

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
		}

		interpreter.traceUnindent();
		return DataTypes.VOID_VALUE;
	}

	@Override
	public String toString()
	{
		return "switch";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		AshRuntime.indentLine( stream, indent );
		stream.println( "<SWITCH" + (labels != null ? " (OPTIMIZED)" : "" ) + ">" );
		this.getCondition().print( stream, indent + 1 );
		this.getScope().print( stream, indent + 1, tests, offsets, defaultIndex );
	}

	@Override
	public boolean assertBarrier()
	{
		return this.defaultIndex != -1 &&
			this.scope.assertBarrier() &&
			!this.scope.assertBreakable();
	}
}
