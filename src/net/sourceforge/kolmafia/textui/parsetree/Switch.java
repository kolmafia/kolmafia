/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Map;

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;


public class Switch
	extends ParseTreeNode
{
	private final Value condition;
	private final Value [] tests;
	private final Integer [] offsets;
	private final int defaultIndex;
	private final SwitchScope scope;
	private final Map labels;

	public Switch( final Value condition, final ArrayList tests, final ArrayList offsets, final int defaultIndex, final SwitchScope scope, final Map labels )
	{
		this.condition = condition;
		this.tests = (Value[])tests.toArray( new Value[tests.size()] );
		this.offsets = (Integer[])offsets.toArray(new Integer[offsets.size()] );
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
	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( this.toString() );
		}

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Value: " + this.condition );
		}

		Value value = this.condition.execute( interpreter );
		interpreter.captureValue( value );

		if ( interpreter.isTracing() )
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
			Integer mapped = (Integer)labels.get( value );
			if ( mapped != null )
			{
				offset = mapped.intValue();
			}
		}
		else
		{
			for ( int index = 0; index < tests.length; ++index )
			{
				Value test = tests[ index ];
				if ( interpreter.isTracing() )
				{
					interpreter.trace( "test: " + test );
				}

				Value result = test.execute( interpreter );
				interpreter.captureValue( result );

				if ( interpreter.isTracing() )
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

			if ( interpreter.getState() == Interpreter.STATE_BREAK )
			{
				interpreter.setState( Interpreter.STATE_NORMAL );
				interpreter.traceUnindent();
				return DataTypes.VOID_VALUE;
			}

			if ( interpreter.getState() != Interpreter.STATE_NORMAL )
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
		Interpreter.indentLine( stream, indent );
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
