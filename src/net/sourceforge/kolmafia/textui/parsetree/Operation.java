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

public class Operation
	extends Expression
{
	Operator oper;

	public Operation( final Value lhs, final Value rhs, final Operator oper )
	{
		this.lhs = lhs;
		this.rhs = rhs;
		this.oper = oper;
	}

	public Operation( final Value lhs, final Operator oper )
	{
		this.lhs = lhs;
		this.rhs = null;
		this.oper = oper;
	}

	@Override
	public Type getType()
	{
		Type leftType = this.lhs.getType();

		// Unary operators have no right hand side
		if ( this.rhs == null )
		{
			return leftType;
		}

		Type rightType = this.rhs.getType();

		// String concatenation always yields a string
		if ( this.oper.equals( "+" ) && ( leftType.equals( DataTypes.TYPE_STRING ) || rightType.equals( DataTypes.TYPE_STRING ) ) )
		{
			return DataTypes.STRING_TYPE;
		}

		// If it's an integer operator, must be integers
		if ( this.oper.isInteger() )
		{
			return DataTypes.INT_TYPE;
		}

		// If it's a logical operator, must be both integers or both
		// booleans
		if ( this.oper.isLogical() )
		{
			return leftType;
		}

		// If it's not arithmetic, it's boolean
		if ( !this.oper.isArithmetic() )
		{
			return DataTypes.BOOLEAN_TYPE;
		}

		// Coerce int to float
		if ( leftType.equals( DataTypes.TYPE_FLOAT ) )
		{
			return DataTypes.FLOAT_TYPE;
		}

		// Otherwise result is whatever is on right
		return rightType;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		return this.oper.applyTo( interpreter, this.lhs, this.rhs );
	}

	@Override
	public String toString()
	{
		if ( this.rhs == null )
		{
			return this.oper.toString() + " " + this.lhs.toQuotedString();
		}

		return "( " + this.lhs.toQuotedString() + " " + this.oper.toString() + " " + this.rhs.toQuotedString() + " )";
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		this.oper.print( stream, indent );
		super.print( stream, indent );
	}
}
