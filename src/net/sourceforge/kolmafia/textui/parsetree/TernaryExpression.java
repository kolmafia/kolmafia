/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class TernaryExpression
	extends Expression
{
	Value conditional;

	public TernaryExpression( final Value conditional, final Value lhs, final Value rhs )
	{
		this.conditional = conditional;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public Type getType()
	{
		// Ternary expressions have no real operator
		return this.lhs.getType();
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Operator: ?:" );
		}

		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Condition: " + conditional );
		}
		Value conditionResult = this.conditional.execute( interpreter );
		interpreter.captureValue( conditionResult );

		if ( conditionResult == null )
		{
			conditionResult = DataTypes.VOID_VALUE;
		}
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "[" + interpreter.getState() + "] <- " + conditionResult.toQuotedString() );
		}
		interpreter.traceUnindent();

		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			interpreter.traceUnindent();
			return null;
		}

		Value expression;
		String tag;

		if ( conditionResult.intValue() != 0 )
		{
			expression = lhs;
			tag = "True value: ";
		}
		else
		{
			expression = rhs;
			tag = "False value: ";
		}

		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( tag + expression );
		}

		Value executeResult = expression.execute( interpreter );

		if ( executeResult == null )
		{
			executeResult = DataTypes.VOID_VALUE;
		}

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "[" + interpreter.getState() + "] <- " + executeResult.toQuotedString() );
		}
		interpreter.traceUnindent();

		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			interpreter.traceUnindent();
			return null;
		}

		if ( Operator.isStringLike( this.lhs.getType() ) != Operator.isStringLike( this.rhs.getType() ) )
		{
			executeResult = executeResult.toStringValue();
		}
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "<- " + executeResult );
		}
		interpreter.traceUnindent();

		return executeResult;
	}

	@Override
	public String toString()
	{
		return "( " + this.conditional.toQuotedString() + " ? " + this.lhs.toQuotedString() + " : " + this.rhs.toQuotedString() + " )";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<OPER ?:>" );
		this.conditional.print( stream, indent + 1 );
		super.print( stream, indent );
	}
}
