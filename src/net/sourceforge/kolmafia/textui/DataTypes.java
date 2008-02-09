/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia.textui;

import java.io.PrintStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLFrame;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser.AdvancedScriptException;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Monster;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

public class DataTypes
{
	public static final int TYPE_ANY = 0;
	public static final int TYPE_VOID = 1;
	public static final int TYPE_BOOLEAN = 2;
	public static final int TYPE_INT = 3;
	public static final int TYPE_FLOAT = 4;
	public static final int TYPE_STRING = 5;
	public static final int TYPE_BUFFER = 6;
	public static final int TYPE_MATCHER = 7;

	public static final int TYPE_ITEM = 100;
	public static final int TYPE_LOCATION = 101;
	public static final int TYPE_CLASS = 102;
	public static final int TYPE_STAT = 103;
	public static final int TYPE_SKILL = 104;
	public static final int TYPE_EFFECT = 105;
	public static final int TYPE_FAMILIAR = 106;
	public static final int TYPE_SLOT = 107;
	public static final int TYPE_MONSTER = 108;
	public static final int TYPE_ELEMENT = 109;

	public static final int TYPE_AGGREGATE = 1000;
	public static final int TYPE_RECORD = 1001;
	public static final int TYPE_TYPEDEF = 1002;

	public static final String[] CLASSES =
	{
		KoLCharacter.SEAL_CLUBBER,
		KoLCharacter.TURTLE_TAMER,
		KoLCharacter.PASTAMANCER,
		KoLCharacter.SAUCEROR,
		KoLCharacter.DISCO_BANDIT,
		KoLCharacter.ACCORDION_THIEF
	};

	public static final ScriptType ANY_TYPE = new ScriptType( "any", DataTypes.TYPE_ANY );
	public static final ScriptType VOID_TYPE = new ScriptType( "void", DataTypes.TYPE_VOID );
	public static final ScriptType BOOLEAN_TYPE = new ScriptType( "boolean", DataTypes.TYPE_BOOLEAN );
	public static final ScriptType INT_TYPE = new ScriptType( "int", DataTypes.TYPE_INT );
	public static final ScriptType FLOAT_TYPE = new ScriptType( "float", DataTypes.TYPE_FLOAT );
	public static final ScriptType STRING_TYPE = new ScriptType( "string", DataTypes.TYPE_STRING );
	public static final ScriptType BUFFER_TYPE = new ScriptType( "buffer", DataTypes.TYPE_BUFFER );
	public static final ScriptType MATCHER_TYPE = new ScriptType( "matcher", DataTypes.TYPE_MATCHER );

	public static final ScriptType ITEM_TYPE = new ScriptType( "item", DataTypes.TYPE_ITEM );
	public static final ScriptType LOCATION_TYPE = new ScriptType( "location", DataTypes.TYPE_LOCATION );
	public static final ScriptType CLASS_TYPE = new ScriptType( "class", DataTypes.TYPE_CLASS );
	public static final ScriptType STAT_TYPE = new ScriptType( "stat", DataTypes.TYPE_STAT );
	public static final ScriptType SKILL_TYPE = new ScriptType( "skill", DataTypes.TYPE_SKILL );
	public static final ScriptType EFFECT_TYPE = new ScriptType( "effect", DataTypes.TYPE_EFFECT );
	public static final ScriptType FAMILIAR_TYPE = new ScriptType( "familiar", DataTypes.TYPE_FAMILIAR );
	public static final ScriptType SLOT_TYPE = new ScriptType( "slot", DataTypes.TYPE_SLOT );
	public static final ScriptType MONSTER_TYPE = new ScriptType( "monster", DataTypes.TYPE_MONSTER );
	public static final ScriptType ELEMENT_TYPE = new ScriptType( "element", DataTypes.TYPE_ELEMENT );

	public static final ScriptType AGGREGATE_TYPE = new ScriptType( "aggregate", DataTypes.TYPE_AGGREGATE );
	public static final ScriptAggregateType RESULT_TYPE =
		new ScriptAggregateType( DataTypes.INT_TYPE, DataTypes.ITEM_TYPE );
	public static final ScriptAggregateType REGEX_GROUP_TYPE =
		new ScriptAggregateType(
			new ScriptAggregateType( DataTypes.STRING_TYPE, DataTypes.INT_TYPE ), DataTypes.INT_TYPE );

	// Common values

	public static final String[] BOOLEANS = { "true", "false" };
	public static final String[] STATS = { "Muscle", "Mysticality", "Moxie" };

        public static final ScriptValue[] STAT_VALUES =
        {
                new ScriptValue( DataTypes.STAT_TYPE, KoLConstants.MUSCLE, DataTypes.STATS[ 0 ] ),
                new ScriptValue( DataTypes.STAT_TYPE, KoLConstants.MYSTICALITY, DataTypes.STATS[ 1 ] ),
                new ScriptValue( DataTypes.STAT_TYPE, KoLConstants.MOXIE, DataTypes.STATS[ 2 ] ),
        };

	public static final ScriptValue VOID_VALUE = new ScriptValue();
	public static final ScriptValue TRUE_VALUE = new ScriptValue( true );
	public static final ScriptValue FALSE_VALUE = new ScriptValue( false );
	public static final ScriptValue ZERO_VALUE = new ScriptValue( 0 );
	public static final ScriptValue ONE_VALUE = new ScriptValue( 1 );
	public static final ScriptValue ZERO_FLOAT_VALUE = new ScriptValue( 0.0f );
	public static final ScriptValue MUSCLE_VALUE = DataTypes.STAT_VALUES[0];
	public static final ScriptValue MYSTICALITY_VALUE = DataTypes.STAT_VALUES[1];
	public static final ScriptValue MOXIE_VALUE = DataTypes.STAT_VALUES[2];

	// Initial values for uninitialized variables

	// VOID_TYPE omitted since no variable can have that type
	public static final ScriptValue BOOLEAN_INIT = DataTypes.FALSE_VALUE;
	public static final ScriptValue INT_INIT = DataTypes.ZERO_VALUE;
	public static final ScriptValue FLOAT_INIT = DataTypes.ZERO_FLOAT_VALUE;
	public static final ScriptValue STRING_INIT = new ScriptValue( "" );

	public static final ScriptValue ITEM_INIT = new ScriptValue( DataTypes.ITEM_TYPE, -1, "none" );
	public static final ScriptValue LOCATION_INIT = new ScriptValue( DataTypes.LOCATION_TYPE, "none", (Object) null );
	public static final ScriptValue CLASS_INIT = new ScriptValue( DataTypes.CLASS_TYPE, -1, "none" );
	public static final ScriptValue STAT_INIT = new ScriptValue( DataTypes.STAT_TYPE, -1, "none" );
	public static final ScriptValue SKILL_INIT = new ScriptValue( DataTypes.SKILL_TYPE, -1, "none" );
	public static final ScriptValue EFFECT_INIT = new ScriptValue( DataTypes.EFFECT_TYPE, -1, "none" );
	public static final ScriptValue FAMILIAR_INIT = new ScriptValue( DataTypes.FAMILIAR_TYPE, -1, "none" );
	public static final ScriptValue SLOT_INIT = new ScriptValue( DataTypes.SLOT_TYPE, -1, "none" );
	public static final ScriptValue MONSTER_INIT = new ScriptValue( DataTypes.MONSTER_TYPE, "none", (Object) null );
	public static final ScriptValue ELEMENT_INIT = new ScriptValue( DataTypes.ELEMENT_TYPE, "none", (Object) null );

	public static final ScriptTypeList simpleTypes = new ScriptTypeList();

	static
	{
		simpleTypes.addElement( DataTypes.VOID_TYPE );
		simpleTypes.addElement( DataTypes.BOOLEAN_TYPE );
		simpleTypes.addElement( DataTypes.INT_TYPE );
		simpleTypes.addElement( DataTypes.FLOAT_TYPE );
		simpleTypes.addElement( DataTypes.STRING_TYPE );
		simpleTypes.addElement( DataTypes.BUFFER_TYPE );
		simpleTypes.addElement( DataTypes.MATCHER_TYPE );

		simpleTypes.addElement( DataTypes.ITEM_TYPE );
		simpleTypes.addElement( DataTypes.LOCATION_TYPE );
		simpleTypes.addElement( DataTypes.CLASS_TYPE );
		simpleTypes.addElement( DataTypes.STAT_TYPE );
		simpleTypes.addElement( DataTypes.SKILL_TYPE );
		simpleTypes.addElement( DataTypes.EFFECT_TYPE );
		simpleTypes.addElement( DataTypes.FAMILIAR_TYPE );
		simpleTypes.addElement( DataTypes.SLOT_TYPE );
		simpleTypes.addElement( DataTypes.MONSTER_TYPE );
		simpleTypes.addElement( DataTypes.ELEMENT_TYPE );
	}

	public static final ScriptValue booleanValue( final boolean value )
	{
		return value ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	// For each simple data type X, we supply:
	// public static final ScriptValue parseXValue( String name );

	public static final ScriptValue parseBooleanValue( final String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "true" ) )
		{
			return DataTypes.TRUE_VALUE;
		}
		if ( name.equalsIgnoreCase( "false" ) )
		{
			return DataTypes.FALSE_VALUE;
		}

		if ( returnDefault )
		{
			return booleanValue( StaticEntity.parseInt( name ) == 0 );
		}

		return null;
	}

	public static final ScriptValue parseIntValue( final String name )
		throws NumberFormatException
	{
		return new ScriptValue( StaticEntity.parseInt( name ) );
	}

	public static final ScriptValue parseFloatValue( final String name )
		throws NumberFormatException
	{
		return new ScriptValue( StaticEntity.parseFloat( name ) );
	}

	public static final ScriptValue parseStringValue( final String name )
	{
		return new ScriptValue( name );
	}

	public static final ScriptValue parseItemValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.ITEM_INIT;
		}

		// Allow for an item number to be specified
		// inside of the "item" construct.

		int itemId;
		for ( int i = 0; i < name.length(); ++i )
		{
			if ( !Character.isDigit( name.charAt( i ) ) )
			{
				AdventureResult item = KoLmafiaCLI.getFirstMatchingItem( name, false );

				if ( item == null )
				{
					return returnDefault ? DataTypes.ITEM_INIT : null;
				}

				itemId = item.getItemId();
				name = ItemDatabase.getItemName( itemId );
				return new ScriptValue( DataTypes.ITEM_TYPE, itemId, name );
			}
		}

		// Since it is numeric, parse the integer value
		// and store it inside of the contentInt.

		itemId = StaticEntity.parseInt( name );
		name = ItemDatabase.getItemName( itemId );
		return new ScriptValue( DataTypes.ITEM_TYPE, itemId, name );
	}

	public static final ScriptValue parseLocationValue( final String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.LOCATION_INIT;
		}

		KoLAdventure content = AdventureDatabase.getAdventure( name );
		if ( content == null )
		{
			return returnDefault ? DataTypes.LOCATION_INIT : null;
		}

		return new ScriptValue( DataTypes.LOCATION_TYPE, name, (Object) content );
	}

	public static final int classToInt( final String name )
	{
		for ( int i = 0; i < DataTypes.CLASSES.length; ++i )
		{
			if ( name.equalsIgnoreCase( DataTypes.CLASSES[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	public static final ScriptValue parseClassValue( final String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) || name.equals( "" ) )
		{
			return DataTypes.CLASS_INIT;
		}

		int num = DataTypes.classToInt( name );
		if ( num < 0 )
		{
			return returnDefault ? DataTypes.CLASS_INIT: null;
		}

		return new ScriptValue( DataTypes.CLASS_TYPE, num, DataTypes.CLASSES[ num ] );
	}

	public static final int statToInt( final String name )
	{
		for ( int i = 0; i < DataTypes.STATS.length; ++i )
		{
			if ( name.equalsIgnoreCase( DataTypes.STATS[ i ] ) )
			{
				return STAT_VALUES[ i ].intValue();
			}
		}
		return -1;
	}

	public static final ScriptValue parseStatValue( final String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.STAT_INIT;
		}

		for ( int i = 0; i < DataTypes.STATS.length; ++i )
		{
			if ( name.equalsIgnoreCase( DataTypes.STATS[ i ] ) )
			{
				return STAT_VALUES[ i ];
			}
		}

		return returnDefault ? DataTypes.STAT_INIT : null;
	}

	public static final ScriptValue parseSkillValue( String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.SKILL_INIT;
		}

		List skills = SkillDatabase.getMatchingNames( name );

		if ( skills.isEmpty() )
		{
			return returnDefault ? DataTypes.SKILL_INIT : null;
		}

		int num = SkillDatabase.getSkillId( (String) skills.get( 0 ) );
		name = SkillDatabase.getSkillName( num );
		return new ScriptValue( DataTypes.SKILL_TYPE, num, name );
	}

	public static final ScriptValue parseEffectValue( String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) || name.equals( "" ) )
		{
			return DataTypes.EFFECT_INIT;
		}

		AdventureResult effect = KoLmafiaCLI.getFirstMatchingEffect( name );
		if ( effect == null )
		{
			return returnDefault ? DataTypes.EFFECT_INIT : null;
		}

		int num = EffectDatabase.getEffectId( effect.getName() );
		name = EffectDatabase.getEffectName( num );
		return new ScriptValue( DataTypes.EFFECT_TYPE, num, name );
	}

	public static final ScriptValue parseFamiliarValue( String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.FAMILIAR_INIT;
		}

		int num = FamiliarDatabase.getFamiliarId( name );
		if ( num == -1 )
		{
			return returnDefault ? DataTypes.FAMILIAR_INIT : null;
		}

		name = FamiliarDatabase.getFamiliarName( num );
		return new ScriptValue( DataTypes.FAMILIAR_TYPE, num, name );
	}

	public static final ScriptValue parseSlotValue( String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.SLOT_INIT;
		}

		int num = EquipmentRequest.slotNumber( name );
		if ( num == -1 )
		{
			return returnDefault ? DataTypes.SLOT_INIT : null;
		}

		name = EquipmentRequest.slotNames[ num ];
		return new ScriptValue( DataTypes.SLOT_TYPE, num, name );
	}

	public static final ScriptValue parseMonsterValue( final String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.MONSTER_INIT;
		}

		Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
		{
			return returnDefault ? DataTypes.MONSTER_INIT : null;
		}

		return new ScriptValue( DataTypes.MONSTER_TYPE, monster.getName(), (Object) monster );
	}

	public static final ScriptValue parseElementValue( String name, final boolean returnDefault )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.ELEMENT_INIT;
		}

		int num = MonsterDatabase.elementNumber( name );
		if ( num == -1 )
		{
			return returnDefault ? DataTypes.ELEMENT_INIT : null;
		}

		name = MonsterDatabase.elementNames[ num ];
		return new ScriptValue( DataTypes.ELEMENT_TYPE, num, name );
	}

	public static final ScriptValue parseValue( final ScriptType type, final String name, final boolean returnDefault )
	{
		return type.parseValue( name, returnDefault );
	}

	// For data types which map to integers, also supply:
	//
	// public static final ScriptValue makeXValue( int num )
	//     throws nothing.

	public static final ScriptValue makeItemValue( final int num )
	{
		String name = ItemDatabase.getItemName( num );

		if ( name == null )
		{
			return DataTypes.ITEM_INIT;
		}

		return new ScriptValue( DataTypes.ITEM_TYPE, num, name );
	}

	public static final ScriptValue makeItemValue( final String name )
	{
		int num = ItemDatabase.getItemId( name );

		if ( num == -1 )
		{
			return DataTypes.ITEM_INIT;
		}

		return new ScriptValue( DataTypes.ITEM_TYPE, num, name );
	}

	public static final ScriptValue makeClassValue( final String name )
	{
		return new ScriptValue( DataTypes.CLASS_TYPE, DataTypes.classToInt( name ), name );
	}

	public static final ScriptValue makeSkillValue( final int num )
	{
		String name = SkillDatabase.getSkillName( num );
		if ( name == null )
		{
			return DataTypes.SKILL_INIT;
		}

		return new ScriptValue( DataTypes.SKILL_TYPE, num, name );
	}

	public static final ScriptValue makeEffectValue( final int num )
	{
		String name = EffectDatabase.getEffectName( num );
		if ( name == null )
		{
			return DataTypes.EFFECT_INIT;
		}
		return new ScriptValue( DataTypes.EFFECT_TYPE, num, name );
	}

	public static final ScriptValue makeFamiliarValue( final int num )
	{
		String name = FamiliarDatabase.getFamiliarName( num );
		if ( name == null )
		{
			return DataTypes.FAMILIAR_INIT;
		}
		return new ScriptValue( DataTypes.FAMILIAR_TYPE, num, name );
	}

	public static String promptForValue( final ScriptType type, final String name )
	{
		return DataTypes.promptForValue( type, "Please input a value for " + type + " " + name, name );
	}

	private static String promptForValue( final ScriptType type, final String message, final String name )
	{
		switch ( type.getType() )
		{
		case TYPE_BOOLEAN:
			return (String) KoLFrame.input( message, DataTypes.BOOLEANS );

		case TYPE_LOCATION:
			return (String) ( (KoLAdventure) KoLFrame.input(
				message, AdventureDatabase.getAsLockableListModel().toArray(),
				AdventureDatabase.getAdventure( Preferences.getString( "lastAdventure" ) ) ) ).getAdventureName();

		case TYPE_SKILL:
			return (String) ( (UseSkillRequest) KoLFrame.input( message, SkillDatabase.getSkillsByType(
				SkillDatabase.CASTABLE ).toArray() ) ).getSkillName();

		case TYPE_FAMILIAR:
			return ( (FamiliarData) KoLFrame.input(
				message, KoLCharacter.getFamiliarList().toArray(), KoLCharacter.getFamiliar() ) ).getRace();

		case TYPE_SLOT:
			return (String) KoLFrame.input( message, EquipmentRequest.slotNames );

		case TYPE_ELEMENT:
			return (String) KoLFrame.input( message, MonsterDatabase.elementNames );

		case TYPE_CLASS:
			return (String) KoLFrame.input( message, DataTypes.CLASSES );

		case TYPE_STAT:
			return (String) KoLFrame.input( message, DataTypes.STATS );

		case TYPE_INT:
		case TYPE_FLOAT:
		case TYPE_STRING:
		case TYPE_ITEM:
		case TYPE_EFFECT:
		case TYPE_MONSTER:
			return KoLFrame.input( message );

		default:
			throw new AdvancedScriptException( "Internal error: Illegal type for main() parameter" );
		}
	}

	// **************** ScriptSymbol *****************

	public static class ScriptSymbol
		implements Comparable
	{
		public String name;

		public ScriptSymbol()
		{
		}

		public ScriptSymbol( final String name )
		{
			this.name = name;
		}

		public String getName()
		{
			return this.name;
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof ScriptSymbol ) )
			{
				throw new ClassCastException();
			}
			if ( this.name == null )
			{
				return 1;
			}
			return this.name.compareToIgnoreCase( ( (ScriptSymbol) o ).name );
		}
	}

	public static class ScriptSymbolTable
		extends Vector
	{
		public boolean addElement( final ScriptSymbol n )
		{
			if ( this.findSymbol( n.getName() ) != null )
			{
				return false;
			}

			super.addElement( n );
			return true;
		}

		public ScriptSymbol findSymbol( final String name )
		{
			ScriptSymbol currentSymbol = null;
			for ( int i = 0; i < this.size(); ++i )
			{
				currentSymbol = (ScriptSymbol) this.get( i );
				if ( currentSymbol.getName().equalsIgnoreCase( name ) )
				{
					return currentSymbol;
				}
			}

			return null;
		}
	}

	// **************** ScriptType *****************

	public static class ScriptType
		extends ScriptSymbol
	{
		public boolean primitive;
		private final int type;

		public ScriptType( final String name, final int type )
		{
			super( name );
			this.primitive = true;
			this.type = type;
		}

		public int getType()
		{
			return this.type;
		}

		public ScriptType getBaseType()
		{
			return this;
		}

		public boolean isPrimitive()
		{
			return this.primitive;
		}

		public boolean equals( final ScriptType type )
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

		public ScriptType simpleType()
		{
			return this;
		}

		public ScriptValue initialValue()
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
				return new ScriptValue( DataTypes.BUFFER_TYPE, "", new StringBuffer() );
			case DataTypes.TYPE_MATCHER:
				return new ScriptValue( DataTypes.MATCHER_TYPE, "", Pattern.compile( "" ).matcher( "" ) );

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

		public ScriptValue parseValue( final String name, final boolean returnDefault )
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

		public ScriptValue initialValueExpression()
		{
			return new ScriptTypeInitializer( this );
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

	public static class ScriptNamedType
		extends ScriptType
	{
		ScriptType base;

		public ScriptNamedType( final String name, final ScriptType base )
		{
			super( name, DataTypes.TYPE_TYPEDEF );
			this.base = base;
		}

		public ScriptType getBaseType()
		{
			return this.base.getBaseType();
		}

		public ScriptValue initialValueExpression()
		{
			return new ScriptTypeInitializer( this.base.getBaseType() );
		}
	}

	public static class ScriptCompositeType
		extends ScriptType
	{
		public ScriptCompositeType( final String name, final int type )
		{
			super( name, type );
			this.primitive = false;
		}

		public ScriptType getIndexType()
		{
			return null;
		}

		public ScriptType getDataType()
		{
			return null;
		}

		public ScriptType getDataType( final Object key )
		{
			return null;
		}

		public ScriptValue getKey( final ScriptValue key )
		{
			return key;
		}

		public ScriptValue initialValueExpression()
		{
			return new ScriptTypeInitializer( this );
		}
	}

	public static class ScriptAggregateType
		extends ScriptCompositeType
	{
		private final ScriptType dataType;
		private final ScriptType indexType;
		private final int size;

		// Map
		public ScriptAggregateType( final ScriptType dataType, final ScriptType indexType )
		{
			super( "aggregate", DataTypes.TYPE_AGGREGATE );
			this.dataType = dataType;
			this.indexType = indexType;
			this.size = 0;
		}

		// Array
		public ScriptAggregateType( final ScriptType dataType, final int size )
		{
			super( "aggregate", DataTypes.TYPE_AGGREGATE );
			this.primitive = false;
			this.dataType = dataType;
			this.indexType = DataTypes.INT_TYPE;
			this.size = size;
		}

		public ScriptType getDataType()
		{
			return this.dataType;
		}

		public ScriptType getDataType( final Object key )
		{
			return this.dataType;
		}

		public ScriptType getIndexType()
		{
			return this.indexType;
		}

		public int getSize()
		{
			return this.size;
		}

		public boolean equals( final ScriptType o )
		{
			return o instanceof ScriptAggregateType && this.dataType.equals( ( (ScriptAggregateType) o ).dataType ) && this.indexType.equals( ( (ScriptAggregateType) o ).indexType );
		}

		public ScriptType simpleType()
		{
			if ( this.dataType instanceof ScriptAggregateType )
			{
				return this.dataType.simpleType();
			}
			return this.dataType;
		}

		public String toString()
		{
			return this.simpleType().toString() + " [" + this.indexString() + "]";
		}

		public String indexString()
		{
			if ( this.dataType instanceof ScriptAggregateType )
			{
				String suffix = ", " + ( (ScriptAggregateType) this.dataType ).indexString();
				if ( this.size != 0 )
				{
					return this.size + suffix;
				}
				return this.indexType.toString() + suffix;
			}

			if ( this.size != 0 )
			{
				return String.valueOf( this.size );
			}
			return this.indexType.toString();
		}

		public ScriptValue initialValue()
		{
			if ( this.size != 0 )
			{
				return new ScriptArray( this );
			}
			return new ScriptMap( this );
		}

		public boolean containsAggregate()
		{
			return true;
		}
	}

	public static class ScriptRecordType
		extends ScriptCompositeType
	{
		private final String[] fieldNames;
		private final ScriptType[] fieldTypes;
		private final ScriptValue[] fieldIndices;

		public ScriptRecordType( final String name, final String[] fieldNames, final ScriptType[] fieldTypes )
		{
			super( name, DataTypes.TYPE_RECORD );

			this.fieldNames = fieldNames;
			this.fieldTypes = fieldTypes;

			// Build field index values.
			// These can be either integers or strings.
			//   Integers don't require a lookup
			//   Strings make debugging easier.

			this.fieldIndices = new ScriptValue[ fieldNames.length ];
			for ( int i = 0; i < fieldNames.length; ++i )
			{
				this.fieldIndices[ i ] = new ScriptValue( fieldNames[ i ] );
			}
		}

		public String[] getFieldNames()
		{
			return this.fieldNames;
		}

		public ScriptType[] getFieldTypes()
		{
			return this.fieldTypes;
		}

		public ScriptValue[] getFieldIndices()
		{
			return this.fieldIndices;
		}

		public int fieldCount()
		{
			return this.fieldTypes.length;
		}

		public ScriptType getIndexType()
		{
			return DataTypes.STRING_TYPE;
		}

		public ScriptType getDataType( final Object key )
		{
			if ( !( key instanceof ScriptValue ) )
			{
				throw new AdvancedScriptException( "Internal error: key is not a ScriptValue" );
			}
			int index = this.indexOf( (ScriptValue) key );
			if ( index < 0 || index >= this.fieldTypes.length )
			{
				return null;
			}
			return this.fieldTypes[ index ];
		}

		public ScriptValue getFieldIndex( final String field )
		{
			String val = field.toLowerCase();
			for ( int index = 0; index < this.fieldNames.length; ++index )
			{
				if ( val.equals( this.fieldNames[ index ] ) )
				{
					return this.fieldIndices[ index ];
				}
			}
			return null;
		}

		public ScriptValue getKey( final ScriptValue key )
		{
			ScriptType type = key.getType();

			if ( type.equals( DataTypes.TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= this.fieldNames.length )
				{
					return null;
				}
				return this.fieldIndices[ index ];
			}

			if ( type.equals( DataTypes.TYPE_STRING ) )
			{
				String str = key.toString();
				for ( int index = 0; index < this.fieldNames.length; ++index )
				{
					if ( this.fieldNames[ index ].equals( str ) )
					{
						return this.fieldIndices[ index ];
					}
				}
				return null;
			}

			return null;
		}

		public int indexOf( final ScriptValue key )
		{
			ScriptType type = key.getType();

			if ( type.equals( DataTypes.TYPE_INT ) )
			{
				int index = key.intValue();
				if ( index < 0 || index >= this.fieldNames.length )
				{
					return -1;
				}
				return index;
			}

			if ( type.equals( DataTypes.TYPE_STRING ) )
			{
				for ( int index = 0; index < this.fieldNames.length; ++index )
				{
					if ( key == this.fieldIndices[ index ] )
					{
						return index;
					}
				}
				return -1;
			}

			return -1;
		}

		public boolean equals( final ScriptType o )
		{
			return o instanceof ScriptRecordType && this.name == ( (ScriptRecordType) o ).name;
		}

		public ScriptType simpleType()
		{
			return this;
		}

		public String toString()
		{
			return this.name;
		}

		public ScriptValue initialValue()
		{
			return new ScriptRecord( this );
		}

		public boolean containsAggregate()
		{
			for ( int i = 0; i < this.fieldTypes.length; ++i )
			{
				if ( this.fieldTypes[ i ].containsAggregate() )
				{
					return true;
				}
			}
			return false;
		}
	}

	public static class ScriptTypeInitializer
		extends ScriptValue
	{
		public ScriptType type;

		public ScriptTypeInitializer( final ScriptType type )
		{
			this.type = type;
		}

		public ScriptType getType()
		{
			return this.type;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			return this.type.initialValue();
		}

		public String toString()
		{
			return "<initial value>";
		}
	}

	public static class ScriptTypeList
		extends ScriptSymbolTable
	{
		public boolean addElement( final ScriptType n )
		{
			return super.addElement( n );
		}

		public ScriptType findType( final String name )
		{
			return (ScriptType) super.findSymbol( name );
		}
	}

	// **************** ScriptValue *****************

	public static abstract class ScriptCommand
	{
		public ScriptValue execute( final Interpreter interpreter )
		{
			return null;
		}

		public void print( final PrintStream stream, final int indent )
		{
		}
	}

	public static class ScriptValue
		extends ScriptCommand
		implements Comparable
	{
		public ScriptType type;

		public int contentInt = 0;
		public float contentFloat = 0.0f;
		public String contentString = null;
		public Object content = null;

		public ScriptValue()
		{
			this.type = DataTypes.VOID_TYPE;
		}

		public ScriptValue( final int value )
		{
			this.type = DataTypes.INT_TYPE;
			this.contentInt = value;
		}

		public ScriptValue( final boolean value )
		{
			this.type = DataTypes.BOOLEAN_TYPE;
			this.contentInt = value ? 1 : 0;
		}

		public ScriptValue( final String value )
		{
			this.type = DataTypes.STRING_TYPE;
			this.contentString = value;
		}

		public ScriptValue( final float value )
		{
			this.type = DataTypes.FLOAT_TYPE;
			this.contentInt = (int) value;
			this.contentFloat = value;
		}

		public ScriptValue( final ScriptType type )
		{
			this.type = type;
		}

		public ScriptValue( final ScriptType type, final int contentInt, final String contentString )
		{
			this.type = type;
			this.contentInt = contentInt;
			this.contentString = contentString;
		}

		public ScriptValue( final ScriptType type, final String contentString, final Object content )
		{
			this.type = type;
			this.contentString = contentString;
			this.content = content;
		}

		public ScriptValue( final ScriptValue original )
		{
			this.type = original.type;
			this.contentInt = original.contentInt;
			this.contentString = original.contentString;
			this.content = original.content;
		}

		public ScriptValue toFloatValue()
		{
			if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
			{
				return this;
			}
			else
			{
				return new ScriptValue( (float) this.contentInt );
			}
		}

		public ScriptValue toIntValue()
		{
			if ( this.type.equals( DataTypes.TYPE_INT ) )
			{
				return this;
			}
			else
			{
				return new ScriptValue( (int) this.contentFloat );
			}
		}

		public ScriptType getType()
		{
			return this.type.getBaseType();
		}

		public String toString()
		{
			if ( this.content instanceof StringBuffer )
			{
				return ( (StringBuffer) this.content ).toString();
			}

			if ( this.type.equals( DataTypes.TYPE_VOID ) )
			{
				return "void";
			}

			if ( this.contentString != null )
			{
				return this.contentString;
			}

			if ( this.type.equals( DataTypes.TYPE_BOOLEAN ) )
			{
				return String.valueOf( this.contentInt != 0 );
			}

			if ( this.type.equals( DataTypes.TYPE_FLOAT ) )
			{
				return String.valueOf( this.contentFloat );
			}

			return String.valueOf( this.contentInt );
		}

		public String toQuotedString()
		{
			if ( this.contentString != null )
			{
				return "\"" + this.contentString + "\"";
			}
			return this.toString();
		}

		public ScriptValue toStringValue()
		{
			return new ScriptValue( this.toString() );
		}

		public Object rawValue()
		{
			return this.content;
		}

		public int intValue()
		{
			return this.contentInt;
		}

		public float floatValue()
		{
			return this.contentFloat;
		}

		public ScriptValue execute( final Interpreter interpreter )
		{
			return this;
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof ScriptValue ) )
			{
				throw new ClassCastException();
			}

			ScriptValue it = (ScriptValue) o;

			if ( this.type == DataTypes.BOOLEAN_TYPE || this.type == DataTypes.INT_TYPE )
			{
				return this.contentInt < it.contentInt ? -1 : this.contentInt == it.contentInt ? 0 : 1;
			}

			if ( this.type == DataTypes.FLOAT_TYPE )
			{
				return this.contentFloat < it.contentFloat ? -1 : this.contentFloat == it.contentFloat ? 0 : 1;
			}

			if ( this.contentString != null )
			{
				return this.contentString.compareTo( it.contentString );
			}

			return -1;
		}

		public int count()
		{
			return 1;
		}

		public void clear()
		{
		}

		public boolean contains( final ScriptValue index )
		{
			return false;
		}

		public boolean equals( final Object o )
		{
			return o == null || !( o instanceof ScriptValue ) ? false : this.compareTo( (Comparable) o ) == 0;
		}

		public void dumpValue( final PrintStream writer )
		{
			writer.print( this.toStringValue().toString() );
		}

		public void dump( final PrintStream writer, final String prefix, final boolean compact )
		{
			writer.println( prefix + this.toStringValue().toString() );
		}

		public void print( final PrintStream stream, final int indent )
		{
			Interpreter.indentLine( stream, indent );
			stream.println( "<VALUE " + this.getType() + " [" + this.toString() + "]>" );
		}
	}

	public static class ScriptCompositeValue
		extends ScriptValue
	{
		public ScriptCompositeValue( final ScriptCompositeType type )
		{
			super( type );
		}

		public ScriptCompositeType getCompositeType()
		{
			return (ScriptCompositeType) this.type;
		}

		public ScriptValue aref( final ScriptValue key )
		{
			return null;
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
		}

		public ScriptValue remove( final ScriptValue key )
		{
			return null;
		}

		public void clear()
		{
		}

		public ScriptValue[] keys()
		{
			return new ScriptValue[ 0 ];
		}

		public ScriptValue initialValue( final Object key )
		{
			return ( (ScriptCompositeType) this.type ).getDataType( key ).initialValue();
		}

		public void dump( final PrintStream writer, final String prefix, final boolean compact )
		{
			ScriptValue[] keys = this.keys();
			if ( keys.length == 0 )
			{
				return;
			}

			for ( int i = 0; i < keys.length; ++i )
			{
				ScriptValue key = keys[ i ];
				ScriptValue value = this.aref( key );
				String first = prefix + key + "\t";
				value.dump( writer, first, compact );
			}
		}

		public void dumpValue( final PrintStream writer )
		{
		}

		// Returns number of fields consumed
		public int read( final String[] data, final int index, final boolean compact )
		{
			ScriptCompositeType type = (ScriptCompositeType) this.type;
			ScriptValue key = null;

			if ( index < data.length )
			{
				key = type.getKey( DataTypes.parseValue( type.getIndexType(), data[ index ], true ) );
			}
			else
			{
				key = type.getKey( DataTypes.parseValue( type.getIndexType(), "none", true ) );
			}

			// If there's only a key and a value, parse the value
			// and store it in the composite

			if ( !( type.getDataType( key ) instanceof ScriptCompositeType ) )
			{
				this.aset( key, DataTypes.parseValue( type.getDataType( key ), data[ index + 1 ], true ) );
				return 2;
			}

			// Otherwise, recurse until we get the final slice
			ScriptCompositeValue slice = (ScriptCompositeValue) this.aref( key );

			// Create missing intermediate slice
			if ( slice == null )
			{
				slice = (ScriptCompositeValue) this.initialValue( key );
				this.aset( key, slice );
			}

			return slice.read( data, index + 1, compact ) + 1;
		}

		public String toString()
		{
			return "composite " + this.type.toString();
		}
	}

	public static class ScriptAggregateValue
		extends ScriptCompositeValue
	{
		public ScriptAggregateValue( final ScriptAggregateType type )
		{
			super( type );
		}

		public ScriptType getDataType()
		{
			return ( (ScriptAggregateType) this.type ).getDataType();
		}

		public int count()
		{
			return 0;
		}

		public boolean contains( final ScriptValue index )
		{
			return false;
		}

		public String toString()
		{
			return "aggregate " + this.type.toString();
		}
	}

	public static class ScriptArray
		extends ScriptAggregateValue
	{
		public ScriptArray( final ScriptAggregateType type )
		{
			super( type );

			int size = type.getSize();
			ScriptType dataType = type.getDataType();
			ScriptValue[] content = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				content[ i ] = dataType.initialValue();
			}
			this.content = content;
		}

		public ScriptValue aref( final ScriptValue index )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int i = index.intValue();
			if ( i < 0 || i > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}
			return array[ i ];
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}

			if ( array[ index ].getType().equals( val.getType() ) )
			{
				array[ index ] = val;
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_STRING ) )
			{
				array[ index ] = val.toStringValue();
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_INT ) && val.getType().equals(
				DataTypes.TYPE_FLOAT ) )
			{
				array[ index ] = val.toIntValue();
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_FLOAT ) && val.getType().equals(
				DataTypes.TYPE_INT ) )
			{
				array[ index ] = val.toFloatValue();
			}
			else
			{
				throw new AdvancedScriptException(
					"Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
			}
		}

		public ScriptValue remove( final ScriptValue key )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			if ( index < 0 || index > array.length )
			{
				throw new AdvancedScriptException( "Array index out of bounds" );
			}
			ScriptValue result = array[ index ];
			array[ index ] = this.getDataType().initialValue();
			return result;
		}

		public void clear()
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			for ( int index = 0; index < array.length; ++index )
			{
				array[ index ] = this.getDataType().initialValue();
			}
		}

		public int count()
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array.length;
		}

		public boolean contains( final ScriptValue key )
		{
			ScriptValue[] array = (ScriptValue[]) this.content;
			int index = key.intValue();
			return index >= 0 && index < array.length;
		}

		public ScriptValue[] keys()
		{
			int size = ( (ScriptValue[]) this.content ).length;
			ScriptValue[] result = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				result[ i ] = new ScriptValue( i );
			}
			return result;
		}
	}

	public static class ScriptMap
		extends ScriptAggregateValue
	{
		public ScriptMap( final ScriptAggregateType type )
		{
			super( type );
			this.content = new TreeMap();
		}

		public ScriptValue aref( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return (ScriptValue) map.get( key );
		}

		public void aset( final ScriptValue key, ScriptValue val )
		{
			TreeMap map = (TreeMap) this.content;

			if ( !this.getDataType().equals( val.getType() ) )
			{
				if ( this.getDataType().equals( DataTypes.TYPE_STRING ) )
				{
					val = val.toStringValue();
				}
				else if ( this.getDataType().equals( DataTypes.TYPE_INT ) && val.getType().equals(
					DataTypes.TYPE_FLOAT ) )
				{
					val = val.toIntValue();
				}
				else if ( this.getDataType().equals( DataTypes.TYPE_FLOAT ) && val.getType().equals(
					DataTypes.TYPE_INT ) )
				{
					val = val.toFloatValue();
				}
			}

			map.put( key, val );
		}

		public ScriptValue remove( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return (ScriptValue) map.remove( key );
		}

		public void clear()
		{
			TreeMap map = (TreeMap) this.content;
			map.clear();
		}

		public int count()
		{
			TreeMap map = (TreeMap) this.content;
			return map.size();
		}

		public boolean contains( final ScriptValue key )
		{
			TreeMap map = (TreeMap) this.content;
			return map.containsKey( key );
		}

		public ScriptValue[] keys()
		{
			Set set = ( (TreeMap) this.content ).keySet();
			ScriptValue[] keys = new ScriptValue[ set.size() ];
			set.toArray( keys );
			return keys;
		}
	}

	public static class ScriptRecord
		extends ScriptCompositeValue
	{
		public ScriptRecord( final ScriptRecordType type )
		{
			super( type );

			ScriptType[] dataTypes = type.getFieldTypes();
			int size = dataTypes.length;
			ScriptValue[] content = new ScriptValue[ size ];
			for ( int i = 0; i < size; ++i )
			{
				content[ i ] = dataTypes[ i ].initialValue();
			}
			this.content = content;
		}

		public ScriptRecordType getRecordType()
		{
			return (ScriptRecordType) this.type;
		}

		public ScriptType getDataType( final ScriptValue key )
		{
			return ( (ScriptRecordType) this.type ).getDataType( key );
		}

		public ScriptValue aref( final ScriptValue key )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new AdvancedScriptException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array[ index ];
		}

		public ScriptValue aref( final int index )
		{
			ScriptRecordType type = (ScriptRecordType) this.type;
			int size = type.fieldCount();
			if ( index < 0 || index >= size )
			{
				throw new AdvancedScriptException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			return array[ index ];
		}

		public void aset( final ScriptValue key, final ScriptValue val )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new AdvancedScriptException( "Internal error: field index out of bounds" );
			}

			this.aset( index, val );
		}

		public void aset( final int index, final ScriptValue val )
		{
			ScriptRecordType type = (ScriptRecordType) this.type;
			int size = type.fieldCount();
			if ( index < 0 || index >= size )
			{
				throw new AdvancedScriptException( "Internal error: field index out of bounds" );
			}

			ScriptValue[] array = (ScriptValue[]) this.content;

			if ( array[ index ].getType().equals( val.getType() ) )
			{
				array[ index ] = val;
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_STRING ) )
			{
				array[ index ] = val.toStringValue();
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_INT ) && val.getType().equals(
				DataTypes.TYPE_FLOAT ) )
			{
				array[ index ] = val.toIntValue();
			}
			else if ( array[ index ].getType().equals( DataTypes.TYPE_FLOAT ) && val.getType().equals(
				DataTypes.TYPE_INT ) )
			{
				array[ index ] = val.toFloatValue();
			}
			else
			{
				throw new AdvancedScriptException(
					"Internal error: Cannot assign " + val.getType() + " to " + array[ index ].getType() );
			}
		}

		public ScriptValue remove( final ScriptValue key )
		{
			int index = ( (ScriptRecordType) this.type ).indexOf( key );
			if ( index < 0 )
			{
				throw new AdvancedScriptException( "Internal error: field index out of bounds" );
			}
			ScriptValue[] array = (ScriptValue[]) this.content;
			ScriptValue result = array[ index ];
			array[ index ] = this.getDataType( key ).initialValue();
			return result;
		}

		public void clear()
		{
			ScriptType[] dataTypes = ( (ScriptRecordType) this.type ).getFieldTypes();
			ScriptValue[] array = (ScriptValue[]) this.content;
			for ( int index = 0; index < array.length; ++index )
			{
				array[ index ] = dataTypes[ index ].initialValue();
			}
		}

		public ScriptValue[] keys()
		{
			return ( (ScriptRecordType) this.type ).getFieldIndices();
		}

		public void dump( final PrintStream writer, final String prefix, boolean compact )
		{
			if ( !compact || this.type.containsAggregate() )
			{
				super.dump( writer, prefix, compact );
				return;
			}

			writer.print( prefix );
			this.dumpValue( writer );
			writer.println();
		}

		public void dumpValue( final PrintStream writer )
		{
			int size = ( (ScriptRecordType) this.type ).getFieldTypes().length;
			for ( int i = 0; i < size; ++i )
			{
				ScriptValue value = this.aref( i );
				if ( i > 0 )
				{
					writer.print( "\t" );
				}
				value.dumpValue( writer );
			}
		}

		public int read( final String[] data, int index, boolean compact )
		{
			if ( !compact || this.type.containsAggregate() )
			{
				return super.read( data, index, compact );
			}

			ScriptType[] dataTypes = ( (ScriptRecordType) this.type ).getFieldTypes();
			ScriptValue[] array = (ScriptValue[]) this.content;

			int size = Math.min( dataTypes.length, data.length - index );
			int first = index;

			// Consume remaining data values and store them
			for ( int offset = 0; offset < size; ++offset )
			{
				ScriptType valType = dataTypes[ offset ];
				if ( valType instanceof ScriptRecordType )
				{
					ScriptRecord rec = (ScriptRecord) array[ offset ];
					index += rec.read( data, index, true );
				}
				else
				{
					array[ offset ] = DataTypes.parseValue( valType, data[ index ], true );
					index += 1;
				}
			}

			for ( int offset = size; offset < dataTypes.length; ++offset )
			{
				array[ offset ] = DataTypes.parseValue( dataTypes[ offset ], "none", true );
			}

			// assert index == data.length
			return index - first;
		}

		public String toString()
		{
			return "record " + this.type.toString();
		}
	}
}
