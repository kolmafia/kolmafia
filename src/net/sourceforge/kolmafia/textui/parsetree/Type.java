package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;

import java.util.regex.Pattern;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

public class Type
	extends Symbol
{
	public boolean primitive;
	private final int type;

	public Type( final String name, final int type )
	{
		super( name );
		this.primitive = true;
		this.type = type;
	}

	public int getType()
	{
		return this.type;
	}

	public Type getBaseType()
	{
		return this;
	}

	public boolean isPrimitive()
	{
		return this.primitive;
	}

	public boolean equals( final Type type )
	{
		return this.type == type.type;
	}

	public boolean equals( final int type )
	{
		return this.type == type;
	}

	public String toString()
	{
		return this.name;
	}

	public Type simpleType()
	{
		return this;
	}

	public Value initialValue()
	{
		switch ( this.type )
		{
		case DataTypes.TYPE_VOID:
			return DataTypes.VOID_VALUE;
		case DataTypes.TYPE_BOOLEAN:
			return DataTypes.BOOLEAN_INIT;
		case DataTypes.TYPE_INT:
			return DataTypes.INT_INIT;
		case DataTypes.TYPE_FLOAT:
			return DataTypes.FLOAT_INIT;
		case DataTypes.TYPE_STRING:
			return DataTypes.STRING_INIT;
		case DataTypes.TYPE_BUFFER:
			return new Value( DataTypes.BUFFER_TYPE, "", new StringBuffer() );
		case DataTypes.TYPE_MATCHER:
			return new Value( DataTypes.MATCHER_TYPE, "", Pattern.compile( "" ).matcher( "" ) );

		case DataTypes.TYPE_ITEM:
			return DataTypes.ITEM_INIT;
		case DataTypes.TYPE_LOCATION:
			return DataTypes.LOCATION_INIT;
		case DataTypes.TYPE_CLASS:
			return DataTypes.CLASS_INIT;
		case DataTypes.TYPE_STAT:
			return DataTypes.STAT_INIT;
		case DataTypes.TYPE_SKILL:
			return DataTypes.SKILL_INIT;
		case DataTypes.TYPE_EFFECT:
			return DataTypes.EFFECT_INIT;
		case DataTypes.TYPE_FAMILIAR:
			return DataTypes.FAMILIAR_INIT;
		case DataTypes.TYPE_SLOT:
			return DataTypes.SLOT_INIT;
		case DataTypes.TYPE_MONSTER:
			return DataTypes.MONSTER_INIT;
		case DataTypes.TYPE_ELEMENT:
			return DataTypes.ELEMENT_INIT;
		}
		return null;
	}

	public Value parseValue( final String name, final boolean returnDefault )
	{
		switch ( this.type )
		{
		case DataTypes.TYPE_BOOLEAN:
			return DataTypes.parseBooleanValue( name, returnDefault );
		case DataTypes.TYPE_INT:
			return DataTypes.parseIntValue( name );
		case DataTypes.TYPE_FLOAT:
			return DataTypes.parseFloatValue( name );
		case DataTypes.TYPE_STRING:
			return DataTypes.parseStringValue( name );
		case DataTypes.TYPE_ITEM:
			return DataTypes.parseItemValue( name, returnDefault );
		case DataTypes.TYPE_LOCATION:
			return DataTypes.parseLocationValue( name, returnDefault );
		case DataTypes.TYPE_CLASS:
			return DataTypes.parseClassValue( name, returnDefault );
		case DataTypes.TYPE_STAT:
			return DataTypes.parseStatValue( name, returnDefault );
		case DataTypes.TYPE_SKILL:
			return DataTypes.parseSkillValue( name, returnDefault );
		case DataTypes.TYPE_EFFECT:
			return DataTypes.parseEffectValue( name, returnDefault );
		case DataTypes.TYPE_FAMILIAR:
			return DataTypes.parseFamiliarValue( name, returnDefault );
		case DataTypes.TYPE_SLOT:
			return DataTypes.parseSlotValue( name, returnDefault );
		case DataTypes.TYPE_MONSTER:
			return DataTypes.parseMonsterValue( name, returnDefault );
		case DataTypes.TYPE_ELEMENT:
			return DataTypes.parseElementValue( name, returnDefault );
		}
		return null;
	}

	public Value initialValueExpression()
	{
		return new TypeInitializer( this );
	}

	public boolean containsAggregate()
	{
		return false;
	}

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<TYPE " + this.name + ">" );
	}
}
