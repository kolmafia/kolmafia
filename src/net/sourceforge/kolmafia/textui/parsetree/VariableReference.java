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

import net.sourceforge.kolmafia.textui.Interpreter;

public class VariableReference
	extends Value
{
	public Variable target;

	public VariableReference( final Variable target )
	{
		this.target = target;
	}

	public VariableReference( final String varName, final BasicScope scope )
	{
		this.target = scope.findVariable( varName, true );
	}

	public boolean valid()
	{
		return this.target != null;
	}

	@Override
	public Type getType()
	{
		return this.target.getBaseType();
	}

	public String getName()
	{
		return this.target.getName();
	}

	public ValueList getIndices()
	{
		return null;
	}

	@Override
	public int compareTo( final Value o )
	{
		return this.target.getName().compareTo( ( (VariableReference) o ).target.getName() );
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		return this.target.getValue( interpreter );
	}

	public Value getValue( Interpreter interpreter )
	{
		return this.target.getValue( interpreter );
	}

	public void forceValue( final Value targetValue )
	{
		this.target.forceValue( targetValue );
	}

	public Value setValue( final Interpreter interpreter, final Value targetValue )
	{
		return this.setValue( interpreter, targetValue, null );
	}

	public Value setValue( Interpreter interpreter, final Value targetValue, final Operator oper )
	{
		Value newValue = targetValue;
		if ( oper != null )
		{
			Value currentValue = this.target.getValue( interpreter );
			newValue = oper.applyTo( interpreter, currentValue, targetValue );
		}
		if ( newValue != null )
		{
			this.target.setValue( interpreter, newValue );
		}
		return newValue;
	}

	@Override
	public String toString()
	{
		return this.target.getName();
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<VARREF> " + this.getName() );
	}
}
