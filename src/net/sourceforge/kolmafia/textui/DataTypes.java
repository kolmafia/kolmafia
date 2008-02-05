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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLFrame;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.textui.Parser.AdvancedScriptException;
import net.sourceforge.kolmafia.textui.ParseTree;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptAggregateType;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptType;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptTypeInitializer;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptTypeList;
import net.sourceforge.kolmafia.textui.ParseTree.ScriptValue;
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
	public static final String[] STATS = { "Muscle", "Mysticality", "Moxie" };
	public static final String[] BOOLEANS = { "true", "false" };

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

	public static final ScriptValue VOID_VALUE = new ScriptValue();
	public static final ScriptValue TRUE_VALUE = new ScriptValue( true );
	public static final ScriptValue FALSE_VALUE = new ScriptValue( false );
	public static final ScriptValue ZERO_VALUE = new ScriptValue( 0 );
	public static final ScriptValue ONE_VALUE = new ScriptValue( 1 );
	public static final ScriptValue ZERO_FLOAT_VALUE = new ScriptValue( 0.0f );

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

	// For each simple data type X, we supply:
	// public static final ScriptValue parseXValue( String name );

	public static final ScriptValue parseBooleanValue( final String name )
	{
		if ( name.equalsIgnoreCase( "true" ) )
		{
			return DataTypes.TRUE_VALUE;
		}
		if ( name.equalsIgnoreCase( "false" ) )
		{
			return DataTypes.FALSE_VALUE;
		}

		if ( Interpreter.isExecuting )
		{
			return StaticEntity.parseInt( name ) == 0 ? DataTypes.FALSE_VALUE : DataTypes.TRUE_VALUE;
		}

		throw new AdvancedScriptException( "Can't interpret '" + name + "' as a boolean" );
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

	public static final ScriptValue parseItemValue( String name )
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
					if ( Interpreter.isExecuting )
					{
						return DataTypes.ITEM_INIT;
					}

					throw new AdvancedScriptException( "Item " + name + " not found in database" );
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

	public static final ScriptValue parseLocationValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.LOCATION_INIT;
		}

		KoLAdventure content = AdventureDatabase.getAdventure( name );
		if ( content == null )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.LOCATION_INIT;
			}

			throw new AdvancedScriptException( "Location " + name + " not found in database" );
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

	public static final ScriptValue parseClassValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) || name.equals( "" ) )
		{
			return DataTypes.CLASS_INIT;
		}

		int num = DataTypes.classToInt( name );
		if ( num < 0 )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.CLASS_INIT;
			}

			throw new AdvancedScriptException( "Unknown class " + name );
		}

		return new ScriptValue( DataTypes.CLASS_TYPE, num, DataTypes.CLASSES[ num ] );
	}

	public static final int statToInt( final String name )
	{
		for ( int i = 0; i < DataTypes.STATS.length; ++i )
		{
			if ( name.equalsIgnoreCase( DataTypes.STATS[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	public static final ScriptValue parseStatValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.STAT_INIT;
		}

		int num = DataTypes.statToInt( name );
		if ( num < 0 )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.STAT_INIT;
			}

			throw new AdvancedScriptException( "Unknown stat " + name );
		}

		return new ScriptValue( DataTypes.STAT_TYPE, num, DataTypes.STATS[ num ] );
	}

	public static final ScriptValue parseSkillValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.SKILL_INIT;
		}

		List skills = SkillDatabase.getMatchingNames( name );

		if ( skills.isEmpty() )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.SKILL_INIT;
			}

			throw new AdvancedScriptException( "Skill " + name + " not found in database" );
		}

		int num = SkillDatabase.getSkillId( (String) skills.get( 0 ) );
		name = SkillDatabase.getSkillName( num );
		return new ScriptValue( DataTypes.SKILL_TYPE, num, name );
	}

	public static final ScriptValue parseEffectValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) || name.equals( "" ) )
		{
			return DataTypes.EFFECT_INIT;
		}

		AdventureResult effect = KoLmafiaCLI.getFirstMatchingEffect( name );
		if ( effect == null )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.EFFECT_INIT;
			}

			throw new AdvancedScriptException( "Effect " + name + " not found in database" );
		}

		int num = EffectDatabase.getEffectId( effect.getName() );
		name = EffectDatabase.getEffectName( num );
		return new ScriptValue( DataTypes.EFFECT_TYPE, num, name );
	}

	public static final ScriptValue parseFamiliarValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.FAMILIAR_INIT;
		}

		int num = FamiliarDatabase.getFamiliarId( name );
		if ( num == -1 )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.FAMILIAR_INIT;
			}

			throw new AdvancedScriptException( "Familiar " + name + " not found in database" );
		}

		name = FamiliarDatabase.getFamiliarName( num );
		return new ScriptValue( DataTypes.FAMILIAR_TYPE, num, name );
	}

	public static final ScriptValue parseSlotValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.SLOT_INIT;
		}

		int num = EquipmentRequest.slotNumber( name );
		if ( num == -1 )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.SLOT_INIT;
			}

			throw new AdvancedScriptException( "Bad slot name " + name );
		}

		name = EquipmentRequest.slotNames[ num ];
		return new ScriptValue( DataTypes.SLOT_TYPE, num, name );
	}

	public static final ScriptValue parseMonsterValue( final String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.MONSTER_INIT;
		}

		Monster monster = MonsterDatabase.findMonster( name );
		if ( monster == null )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.MONSTER_INIT;
			}

			throw new AdvancedScriptException( "Bad monster name " + name );
		}

		return new ScriptValue( DataTypes.MONSTER_TYPE, monster.getName(), (Object) monster );
	}

	public static final ScriptValue parseElementValue( String name )
	{
		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.ELEMENT_INIT;
		}

		int num = MonsterDatabase.elementNumber( name );
		if ( num == -1 )
		{
			if ( Interpreter.isExecuting )
			{
				return DataTypes.ELEMENT_INIT;
			}

			throw new AdvancedScriptException( "Bad element name " + name );
		}

		name = MonsterDatabase.elementNames[ num ];
		return new ScriptValue( DataTypes.ELEMENT_TYPE, num, name );
	}

	public static final ScriptValue parseValue( final ScriptType type, final String name )
	{
		return type.parseValue( name );
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
			throw new RuntimeException( "Internal error: Illegal type for main() parameter" );
		}
	}
}
