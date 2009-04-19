/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import net.sourceforge.kolmafia.textui.Parser;

public class Operator
	implements ParseTreeNode
{
	String operator;

	// For runtime error messages
	String fileName;
	int lineNumber;

	public Operator( final String operator, final Parser parser )
	{
		this.operator = operator;
		this.fileName = parser.getShortFileName();
		this.lineNumber = parser.getLineNumber();
	}

	public boolean equals( final String op )
	{
		return this.operator.equals( op );
	}

	public boolean precedes( final Operator oper )
	{
		return this.operStrength() > oper.operStrength();
	}

	private int operStrength()
	{
		if ( this.operator.equals( "!" ) || this.operator.equals( "contains" ) || this.operator.equals( "remove" ) )
		{
			return 7;
		}

		if ( this.operator.equals( "^" ) )
		{
			return 6;
		}

		if ( this.operator.equals( "*" ) || this.operator.equals( "/" ) || this.operator.equals( "%" ) )
		{
			return 5;
		}

		if ( this.operator.equals( "+" ) || this.operator.equals( "-" ) )
		{
			return 4;
		}

		if ( this.operator.equals( "<" ) || this.operator.equals( ">" ) || this.operator.equals( "<=" ) || this.operator.equals( ">=" ) )
		{
			return 3;
		}

		if ( this.operator.equals( "==" ) || this.operator.equals( "!=" ) )
		{
			return 2;
		}

		if ( this.operator.equals( "||" ) || this.operator.equals( "&&" ) )
		{
			return 1;
		}

		return -1;
	}

	public boolean isArithmetic()
	{
		return this.operator.equals( "+" ) || this.operator.equals( "-" ) || this.operator.equals( "*" ) || this.operator.equals( "^" ) || this.operator.equals( "/" ) || this.operator.equals( "%" );
	}

	public String toString()
	{
		return this.operator;
	}

	public Value applyTo( final Interpreter interpreter, final Value lhs, final Value rhs )
	{
		interpreter.traceIndent();
		interpreter.trace( "Operator: " + this.operator );

		// Unary operator with special evaluation of argument
		if ( this.operator.equals( "remove" ) )
		{
			CompositeReference operand = (CompositeReference) lhs;
			interpreter.traceIndent();
			interpreter.trace( "Operand: " + operand );
			interpreter.traceUnindent();
			Value result = operand.removeKey( interpreter );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		interpreter.traceIndent();
		interpreter.trace( "Operand 1: " + lhs );

		Value leftValue = lhs.execute( interpreter );
		interpreter.captureValue( leftValue );
		if ( leftValue == null )
		{
			leftValue = DataTypes.VOID_VALUE;
		}
		interpreter.trace( "[" + interpreter.getState() + "] <- " + leftValue.toQuotedString() );
		interpreter.traceUnindent();

		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			interpreter.traceUnindent();
			return null;
		}

		// Unary Operators
		if ( this.operator.equals( "!" ) )
		{
			Value result = new Value( leftValue.intValue() == 0 );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "-" ) && rhs == null )
		{
			Value result = null;
			if ( lhs.getType().equals( DataTypes.TYPE_INT ) )
			{
				result = new Value( 0 - leftValue.intValue() );
			}
			else if ( lhs.getType().equals( DataTypes.TYPE_FLOAT ) )
			{
				result = new Value( 0.0f - leftValue.floatValue() );
			}
			else
			{
				throw Interpreter.runtimeException( "Internal error: Unary minus can only be applied to numbers", this.fileName, this.lineNumber );
			}
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		// Unknown operator
		if ( rhs == null )
		{
			throw interpreter.runtimeException( "Internal error: missing right operand.", this.fileName, this.lineNumber );
		}

		// Binary operators with optional right values
		if ( this.operator.equals( "||" ) )
		{
			if ( leftValue.intValue() == 1 )
			{
				interpreter.trace( "<- " + DataTypes.TRUE_VALUE );
				interpreter.traceUnindent();
				return DataTypes.TRUE_VALUE;
			}
			interpreter.traceIndent();
			interpreter.trace( "Operand 2: " + rhs );
			Value rightValue = rhs.execute( interpreter );
			interpreter.captureValue( rightValue );
			if ( rightValue == null )
			{
				rightValue = DataTypes.VOID_VALUE;
			}
			interpreter.trace( "[" + interpreter.getState() + "] <- " + rightValue.toQuotedString() );
			interpreter.traceUnindent();
			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}
			interpreter.trace( "<- " + rightValue );
			interpreter.traceUnindent();
			return rightValue;
		}

		if ( this.operator.equals( "&&" ) )
		{
			if ( leftValue.intValue() == 0 )
			{
				interpreter.traceUnindent();
				interpreter.trace( "<- " + DataTypes.FALSE_VALUE );
				return DataTypes.FALSE_VALUE;
			}
			interpreter.traceIndent();
			interpreter.trace( "Operand 2: " + rhs );
			Value rightValue = rhs.execute( interpreter );
			interpreter.captureValue( rightValue );
			if ( rightValue == null )
			{
				rightValue = DataTypes.VOID_VALUE;
			}
			interpreter.trace( "[" + interpreter.getState() + "] <- " + rightValue.toQuotedString() );
			interpreter.traceUnindent();
			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}
			interpreter.trace( "<- " + rightValue );
			interpreter.traceUnindent();
			return rightValue;
		}

		// Ensure type compatibility of operands
		if ( !Parser.validCoercion( lhs.getType(), rhs.getType(), this.operator ) )
		{
			throw interpreter.runtimeException( "Internal error: left hand side and right hand side do not correspond", this.fileName, this.lineNumber );
		}

		// Special binary operator: <aggref> contains <any>
		if ( this.operator.equals( "contains" ) )
		{
			interpreter.traceIndent();
			interpreter.trace( "Operand 2: " + rhs );
			Value rightValue = rhs.execute( interpreter );
			interpreter.captureValue( rightValue );
			if ( rightValue == null )
			{
				rightValue = DataTypes.VOID_VALUE;
			}
			interpreter.trace( "[" + interpreter.getState() + "] <- " + rightValue.toQuotedString() );
			interpreter.traceUnindent();
			if ( interpreter.getState() == Interpreter.STATE_EXIT )
			{
				interpreter.traceUnindent();
				return null;
			}
			Value result = new Value( leftValue.contains( rightValue ) );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		// Binary operators
		interpreter.traceIndent();
		interpreter.trace( "Operand 2: " + rhs );
		Value rightValue = rhs.execute( interpreter );
		interpreter.captureValue( rightValue );
		if ( rightValue == null )
		{
			rightValue = DataTypes.VOID_VALUE;
		}
		interpreter.trace( "[" + interpreter.getState() + "] <- " + rightValue.toQuotedString() );
		interpreter.traceUnindent();
		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			interpreter.traceUnindent();
			return null;
		}

		// String operators
		if ( this.operator.equals( "+" ) )
		{
			if ( lhs.getType().equals( DataTypes.TYPE_STRING ) || rhs.getType().equals( DataTypes.TYPE_STRING ) )
			{
				String string = leftValue.toStringValue().toString() + rightValue.toStringValue().toString();
				Value result = new Value( string );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}
		}

		if ( this.operator.equals( "==" ) )
		{
			if ( lhs.getType().equals( DataTypes.TYPE_STRING ) || lhs.getType().equals( DataTypes.TYPE_LOCATION ) || lhs.getType().equals(
				DataTypes.TYPE_MONSTER ) )
			{
				Value result =
					new Value( leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}
		}

		if ( this.operator.equals( "!=" ) )
		{
			if ( lhs.getType().equals( DataTypes.TYPE_STRING ) || lhs.getType().equals( DataTypes.TYPE_LOCATION ) || lhs.getType().equals(
				DataTypes.TYPE_MONSTER ) )
			{
				Value result =
					new Value( !leftValue.toString().equalsIgnoreCase( rightValue.toString() ) );
				interpreter.trace( "<- " + result );
				interpreter.traceUnindent();
				return result;
			}
		}

		// Arithmetic operators
		boolean isInt;
		float lfloat = 0.0f, rfloat = 0.0f;
		int lint = 0, rint = 0;

		if ( lhs.getType().equals( DataTypes.TYPE_FLOAT ) || rhs.getType().equals( DataTypes.TYPE_FLOAT ) )
		{
			isInt = false;
			lfloat = leftValue.toFloatValue().floatValue();
			rfloat = rightValue.toFloatValue().floatValue();
		}
		else
		{
			isInt = true;
			lint = leftValue.intValue();
			rint = rightValue.intValue();
		}

		if ( this.operator.equals( "+" ) )
		{
			Value result = isInt ? new Value( lint + rint ) : new Value( lfloat + rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "-" ) )
		{
			Value result = isInt ? new Value( lint - rint ) : new Value( lfloat - rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "*" ) )
		{
			Value result = isInt ? new Value( lint * rint ) : new Value( lfloat * rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "^" ) )
		{
			Value result = isInt ? new Value( (int) Math.pow( lint, rint ) ) : new Value( (float) Math.pow( lfloat, rfloat ) );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "/" ) )
		{
			if ( isInt ? rint == 0 : rfloat == 0.0f )
			{
				throw interpreter.runtimeException( "Division by zero", this.fileName, this.lineNumber );
			}
			Value result = isInt ? new Value( lint / rint ) : new Value( lfloat / rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "%" ) )
		{
			if ( isInt ? rint == 0 : rfloat == 0.0f )
			{
				throw interpreter.runtimeException( "Division by zero", this.fileName, this.lineNumber );
			}
			Value result = isInt ? new Value( lint % rint ) : new Value( lfloat % rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "<" ) )
		{
			Value result = isInt ? new Value( lint < rint ) : new Value( lfloat < rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( ">" ) )
		{
			Value result = isInt ? new Value( lint > rint ) : new Value( lfloat > rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "<=" ) )
		{
			Value result = isInt ? new Value( lint <= rint ) : new Value( lfloat <= rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( ">=" ) )
		{
			Value result = isInt ? new Value( lint >= rint ) : new Value( lfloat >= rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "==" ) )
		{
			Value result = isInt ? new Value( lint == rint ) : new Value( lfloat == rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		if ( this.operator.equals( "!=" ) )
		{
			Value result = isInt ? new Value( lint != rint ) : new Value( lfloat != rfloat );
			interpreter.trace( "<- " + result );
			interpreter.traceUnindent();
			return result;
		}

		// Unknown operator
		throw interpreter.runtimeException( "Internal error: illegal operator \"" + this.operator + "\"", this.fileName, this.lineNumber );
	}

	public Value execute( final Interpreter interpreter )
	{
		return null;
	}

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<OPER " + this.operator + ">" );
	}
}
