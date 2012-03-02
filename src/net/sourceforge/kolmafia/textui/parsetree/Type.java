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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import java.util.regex.Pattern;

import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.KoLAdventure;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class Type
	extends Symbol
{
	public boolean primitive;
	private final int type;
	private Value allValues = null;

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
	
	public Type asProxy()
	{
		if ( this == DataTypes.CLASS_TYPE )
		{
			return ProxyRecordValue.ClassProxy._type;
		}
		if ( this == DataTypes.ITEM_TYPE )
		{
			return ProxyRecordValue.ItemProxy._type;
		}
		if ( this == DataTypes.FAMILIAR_TYPE )
		{
			return ProxyRecordValue.FamiliarProxy._type;
		}
		if ( this == DataTypes.SKILL_TYPE )
		{
			return ProxyRecordValue.SkillProxy._type;
		}
		if ( this == DataTypes.EFFECT_TYPE )
		{
			return ProxyRecordValue.EffectProxy._type;
		}
		if ( this == DataTypes.LOCATION_TYPE )
		{
			return ProxyRecordValue.LocationProxy._type;
		}
		if ( this == DataTypes.MONSTER_TYPE )
		{
			return ProxyRecordValue.MonsterProxy._type;
		}
		if ( this == DataTypes.COINMASTER_TYPE )
		{
			return ProxyRecordValue.CoinmasterProxy._type;
		}
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
		case DataTypes.TYPE_COINMASTER:
			return DataTypes.COINMASTER_INIT;
		case DataTypes.TYPE_PHYLUM:
			return DataTypes.PHYLUM_INIT;
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
			return DataTypes.parseIntValue( name, returnDefault );
		case DataTypes.TYPE_FLOAT:
			return DataTypes.parseFloatValue( name, returnDefault );
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
		case DataTypes.TYPE_COINMASTER:
			return DataTypes.parseCoinmasterValue( name, returnDefault );
		case DataTypes.TYPE_PHYLUM:
			return DataTypes.parsePhylumValue( name, returnDefault );
		}
		return null;
	}

	public Value allValues()
	{
		if ( this.allValues != null ) return this.allValues;
		
		ArrayList list = new ArrayList();
		switch ( this.type )
		{
		case DataTypes.TYPE_BOOLEAN:
			this.addValues( list, DataTypes.BOOLEANS );
			break;
		case DataTypes.TYPE_ITEM:
			int limit = ItemDatabase.maxItemId();
			for ( int i = 1; i <= limit; ++i )
			{
				if ( i != 13 && ItemDatabase.getItemDataName( i ) != null )
				{
					list.add( DataTypes.makeItemValue( i ) );
				}
			}
			break;
		case DataTypes.TYPE_LOCATION:
			this.addValues( list, AdventureDatabase.getAsLockableListModel() );
			break;
		case DataTypes.TYPE_CLASS:
			this.addValues( list, DataTypes.CLASSES );
			break;
		case DataTypes.TYPE_STAT:
			this.addValues( list, DataTypes.STATS, 0, 3 );
			break;
		case DataTypes.TYPE_SKILL:
			this.addValues( list, SkillDatabase.entrySet() );
			break;
		case DataTypes.TYPE_EFFECT:
			this.addValues( list, EffectDatabase.entrySet() );
			break;
		case DataTypes.TYPE_FAMILIAR:
			this.addValues( list, FamiliarDatabase.entrySet() );
			break;
		case DataTypes.TYPE_SLOT:
			this.addValues( list, EquipmentRequest.slotNames );
			break;
		case DataTypes.TYPE_MONSTER:
			this.addValues( list, MonsterDatabase.entrySet() );
			break;
		case DataTypes.TYPE_ELEMENT:
			this.addValues( list, MonsterDatabase.elementNames, 1, -1 );
			break;
		case DataTypes.TYPE_COINMASTER:
			this.addValues( list, CoinmasterRegistry.MASTERS );
			break;
		case DataTypes.TYPE_PHYLUM:
			this.addValues( list, MonsterDatabase.phylumNames, 1, -1 );
			break;
		default:
			return null;
		}
		this.allValues = new PluralValue( this, list );
		return this.allValues;
	}
	
	private void addValues( ArrayList results, String[] values )
	{
		this.addValues( results, values, 0, -1 );
	}

	private void addValues( ArrayList results, String[] values, int start, int stop )
	{
		if ( stop == -1 ) stop = values.length;
		for ( int i = start; i < stop; ++i )
		{
			Value v = this.parseValue( values[ i ], false );
			if ( v != null ) results.add( v );
		}
	}

	private void addValues( ArrayList results, Collection values )
	{
		Iterator i = values.iterator();
		while ( i.hasNext() )
		{
			Object o = i.next();
			if ( o instanceof Map.Entry )
			{	// Some of the database entrySet() methods return
				// Integer:String mappings, others String:<something>.
				// Attempt to handle either transparently.
				Map.Entry e = (Map.Entry) o;
				o = e.getKey();
				if ( !(o instanceof String) )
				{
					o = e.getValue();
				}
			}
			if ( o instanceof KoLAdventure )
			{	// KoLAdventure.toString() returns "zone: location",
				// which isn't parseable as an ASH location.
				o = ((KoLAdventure) o).getAdventureName();
			}
			Value v = this.parseValue( o.toString(), false );
			if ( v != null ) results.add( v );
		}
	}

	public Value initialValueExpression()
	{
		return new TypeInitializer( this );
	}

	public boolean containsAggregate()
	{
		return false;
	}
	
	public Value execute( final Interpreter interpreter )
	{
		return null;
	}

	public void print( final PrintStream stream, final int indent )
	{
		Interpreter.indentLine( stream, indent );
		stream.println( "<TYPE " + this.name + ">" );
	}
}
