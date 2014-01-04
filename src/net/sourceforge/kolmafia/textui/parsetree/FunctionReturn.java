/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

public class FunctionReturn
	extends ParseTreeNode
{
	private final Value returnValue;
	private final Type expectedType;

	public FunctionReturn( final Value returnValue, final Type expectedType )
	{
		this.returnValue = returnValue;
		this.expectedType = expectedType;
	}

	public Type getType()
	{
		if ( this.expectedType != null )
		{
			return this.expectedType;
		}

		if ( this.returnValue == null )
		{
			return DataTypes.VOID_TYPE;
		}

		return this.returnValue.getType();
	}

	public Value getExpression()
	{
		return this.returnValue;
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			interpreter.setState( Interpreter.STATE_EXIT );
		}

		if ( interpreter.getState() == Interpreter.STATE_EXIT )
		{
			return null;
		}

		if ( this.returnValue == null )
		{
			interpreter.setState( Interpreter.STATE_RETURN );
			return null;
		}

		interpreter.traceIndent();
		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Eval: " + this.returnValue );
		}

		Value result = this.returnValue.execute( interpreter );
		interpreter.captureValue( result );

		if ( interpreter.isTracing() )
		{
			interpreter.trace( "Returning: " + result );
		}
		interpreter.traceUnindent();

		if ( result == null )
		{
			return null;
		}

		if ( interpreter.getState() != Interpreter.STATE_EXIT )
		{
			interpreter.setState( Interpreter.STATE_RETURN );
		}
		
		if ( this.expectedType == null )
		{
			return result;
		}

		if ( this.expectedType.equals( DataTypes.TYPE_STRING ) )
		{
			return result.toStringValue();
		}

		if ( this.expectedType.equals( DataTypes.TYPE_FLOAT ) )
		{
			return result.toFloatValue();
		}

		if ( this.expectedType.equals( DataTypes.TYPE_INT ) )
		{
			return result.toIntValue();
		}

		return result;
	}

	@Override
	public String toString()
	{
		return "return " + this.returnValue;
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<RETURN " + this.getType() + ">" );
		if ( !this.getType().equals( DataTypes.TYPE_VOID ) )
		{
			this.returnValue.print( stream, indent + 1 );
		}
	}
	
	@Override
	public boolean assertBarrier()
	{
		return true;
	}
}
