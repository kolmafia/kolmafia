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

import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser;

public class Assignment
	extends ParseTreeNode
{
	private final VariableReference lhs;
	private final Value rhs;
	private final Operator oper;

	public Assignment( final VariableReference lhs, final Value rhs )
	{
		this.lhs = lhs;
		this.rhs = rhs;
		this.oper = null;
	}

	public Assignment( final VariableReference lhs, final Value rhs, final Operator oper )
	{
		this.lhs = lhs;
		this.rhs = rhs;
		this.oper = oper;
	}

	public VariableReference getLeftHandSide()
	{
		return this.lhs;
	}

	public Value getRightHandSide()
	{
		return this.rhs == null ? this.lhs.getType().initialValueExpression() : this.rhs;
	}

	public Type getType()
	{
		return this.lhs.getType();
	}

	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
			return null;
		}

		Value value;

		if ( this.rhs == null )
		{
			value = this.lhs.getType().initialValue();
		}
		else
		{
			interpreter.traceIndent();
			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Eval: " + this.rhs );
			}

			value = this.rhs.execute( interpreter );
			interpreter.captureValue( value );

			if ( interpreter.isTracing() )
			{
				interpreter.trace( "Set: " + value );
			}
			interpreter.traceUnindent();
		}

		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			return null;
		}

		Value newValue;
		if ( this.lhs.getType().equals( DataTypes.TYPE_STRING ) )
		{
			newValue = this.lhs.setValue( interpreter, value.toStringValue(), oper );
		}
		else if ( this.lhs.getType().equals( DataTypes.TYPE_INT ) )
		{
			newValue = this.lhs.setValue( interpreter, value.toIntValue(), oper );
		}
		else if ( this.lhs.getType().equals( DataTypes.TYPE_FLOAT ) )
		{
			newValue = this.lhs.setValue( interpreter, value.toFloatValue(), oper );
		}
		else if ( this.lhs.getType().equals( DataTypes.TYPE_BOOLEAN ) )
		{
			newValue = this.lhs.setValue( interpreter, value.toBooleanValue(), oper );
		}
		else
		{
			newValue = this.lhs.setValue( interpreter, value );
		}

		return newValue;
	}

	public String toString()
	{
		return this.rhs == null ? this.lhs.getName() : this.lhs.getName() + " = " + this.rhs;
	}

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<ASSIGN " + this.lhs.getName() + ">" );
		VariableReference lhs = this.getLeftHandSide();
		Parser.printIndices( lhs.getIndices(), stream, indent + 1 );
		if ( this.oper != null )
		{
			oper.print( stream, indent + 1 );
		}
		this.getRightHandSide().print( stream, indent + 1 );
	}
}
