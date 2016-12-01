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

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;

public class IncDec
	extends Value
{
	private final VariableReference lhs;
	private final Operator oper;

	public IncDec( final VariableReference lhs )
	{
		this.lhs = lhs;
		this.oper = null;
	}

	public IncDec( final VariableReference lhs, final Operator oper )
	{
		this.lhs = lhs;
		this.oper = oper;
	}

	public VariableReference getLeftHandSide()
	{
		return this.lhs;
	}

	public Type getType()
	{
		return this.lhs.getType();
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		String operStr = oper.operator;
		Value value;

		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Eval: " + this.lhs );
		}

		value = this.lhs.execute( interpreter );
		interpreter.captureValue( value );

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Orig: " + value );
		}

		Value newValue = this.oper.applyTo( interpreter, value );

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "New: " + newValue );
		}

		this.lhs.setValue( interpreter, newValue );

		interpreter.traceUnindent();

		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			return null;
		}

		return ( operStr == Parser.PRE_INCREMENT || operStr == Parser.PRE_DECREMENT ) ? newValue : value;
	}

	@Override
	public String toString()
	{
		String operStr = oper.operator;
		return  operStr == Parser.PRE_INCREMENT ? ( "++" + this.lhs.getName() ) :
			operStr == Parser.PRE_DECREMENT ? ( "--" + this.lhs.getName() ) :
			operStr == Parser.POST_INCREMENT ? ( this.lhs.getName() +  "++" ) :
			operStr == Parser.POST_DECREMENT ? ( this.lhs.getName() +  "--" ) :
			"";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		String operStr = oper.operator;
		String type =
			operStr == Parser.PRE_INCREMENT ? "PRE-INCREMENT" :
			operStr == Parser.PRE_DECREMENT ? "PRE-DECREMENT" :
			operStr == Parser.POST_INCREMENT ? "POST-INCREMENT" :
			operStr == Parser.POST_DECREMENT ? "POST-DECREMENT" :
			"UNKNOWN";
		Interpreter.indentLine( stream, indent );
		stream.println( "<" + type + " " + this.lhs.getName() + ">" );
		VariableReference lhs = this.getLeftHandSide();
		Parser.printIndices( lhs.getIndices(), stream, indent + 1 );
	}
}
