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

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.ScriptException;

public class Variable
	extends Symbol
{
	Type type;
	Value content;
	Value expression = null;
	boolean isStatic = false;

	public Variable( final Type type )
	{
		super( null );
		this.type = type;
		this.content = new Value( type );
	}

	public Variable( final String name, final Type type )
	{
		super( name );
		this.type = type;
		this.content = new Value( type );
	}

	public Type getType()
	{
		return this.type;
	}

	public Type getBaseType()
	{
		return this.type.getBaseType();
	}

	public boolean isStatic()
	{
		return this.isStatic;
	}

	public void markStatic()
	{
		this.isStatic = true;
	}

	public Value getValue( final Interpreter interpreter )
	{
		if ( this.expression != null )
		{
			this.content = this.expression.execute( interpreter );
		}

		return this.content;
	}

	public Type getValueType( final Interpreter interpreter )
	{
		return this.getValue( interpreter ).getType();
	}

	public Object rawValue( final Interpreter interpreter )
	{
		return this.getValue( interpreter ).rawValue();
	}

	public long intValue( final Interpreter interpreter )
	{
		return this.getValue( interpreter ).intValue();
	}

	public Value toStringValue( final Interpreter interpreter )
	{
		return this.getValue( interpreter ).toStringValue();
	}

	public double floatValue( final Interpreter interpreter )
	{
		return this.getValue( interpreter ).floatValue();
	}

	public void setExpression( final Value targetExpression )
	{
		this.expression = targetExpression;
	}

	public void forceValue( final Value targetValue )
	{
		this.content = targetValue;
		this.expression = null;
	}

	public void setValue( Interpreter interpreter, final Value targetValue )
	{
		if ( this.getBaseType().equals( targetValue.getType() ) )
		{
			this.content = targetValue;
			this.expression = null;
		}
		else if ( this.getBaseType().equals( DataTypes.TYPE_STRICT_STRING ) || this.getBaseType().equals( DataTypes.TYPE_STRING ) )
		{
			this.content = targetValue.toStringValue();
			this.expression = null;
		}
		else if ( this.getBaseType().equals( DataTypes.TYPE_INT ) && targetValue.getType().equals( DataTypes.TYPE_FLOAT ) )
		{
			this.content = targetValue.toIntValue();
			this.expression = null;
		}
		else if ( this.getBaseType().equals( DataTypes.TYPE_FLOAT ) && targetValue.getType().equals( DataTypes.TYPE_INT ) )
		{
			this.content = targetValue.toFloatValue();
			this.expression = null;
		}
		else
		{
			throw new ScriptException(
				"Internal error: Cannot assign " + targetValue.getType() + " to " + this.getType() );
		}
	}

	@Override
	public Value execute( final Interpreter interpreter )
	{
		return getValue( interpreter );
	}

	@Override
	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<VAR " + this.getType() + " " + this.getName() + ">" );
	}
}
