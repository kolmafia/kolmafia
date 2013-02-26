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

package net.sourceforge.kolmafia.textui;

import java.util.List;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.TypeList;
import net.sourceforge.kolmafia.textui.parsetree.Value;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

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
	public static final int TYPE_COINMASTER = 110;
	public static final int TYPE_PHYLUM = 111;

	public static final int TYPE_STRICT_STRING = 1000;
	public static final int TYPE_AGGREGATE = 1001;
	public static final int TYPE_RECORD = 1002;
	public static final int TYPE_TYPEDEF = 1003;

	public static final String[] CLASSES =
	{
		"",
		KoLCharacter.SEAL_CLUBBER,
		KoLCharacter.TURTLE_TAMER,
		KoLCharacter.PASTAMANCER,
		KoLCharacter.SAUCEROR,
		KoLCharacter.DISCO_BANDIT,
		KoLCharacter.ACCORDION_THIEF,
		"",
		"",
		"",
		"",
		KoLCharacter.AVATAR_OF_BORIS,
		KoLCharacter.ZOMBIE_MASTER,
		KoLCharacter.AVATAR_OF_JARLSBERG
	};

	public static final Type ANY_TYPE = new Type( null, DataTypes.TYPE_ANY );
	public static final Type VOID_TYPE = new Type( "void", DataTypes.TYPE_VOID );
	public static final Type BOOLEAN_TYPE = new Type( "boolean", DataTypes.TYPE_BOOLEAN );
	public static final Type INT_TYPE = new Type( "int", DataTypes.TYPE_INT );
	public static final Type FLOAT_TYPE = new Type( "float", DataTypes.TYPE_FLOAT );
	public static final Type STRING_TYPE = new Type( "string", DataTypes.TYPE_STRING );
	public static final Type BUFFER_TYPE = new Type( "buffer", DataTypes.TYPE_BUFFER );
	public static final Type MATCHER_TYPE = new Type( "matcher", DataTypes.TYPE_MATCHER );

	public static final Type ITEM_TYPE = new Type( "item", DataTypes.TYPE_ITEM );
	public static final Type LOCATION_TYPE = new Type( "location", DataTypes.TYPE_LOCATION );
	public static final Type CLASS_TYPE = new Type( "class", DataTypes.TYPE_CLASS );
	public static final Type STAT_TYPE = new Type( "stat", DataTypes.TYPE_STAT );
	public static final Type SKILL_TYPE = new Type( "skill", DataTypes.TYPE_SKILL );
	public static final Type EFFECT_TYPE = new Type( "effect", DataTypes.TYPE_EFFECT );
	public static final Type FAMILIAR_TYPE = new Type( "familiar", DataTypes.TYPE_FAMILIAR );
	public static final Type SLOT_TYPE = new Type( "slot", DataTypes.TYPE_SLOT );
	public static final Type MONSTER_TYPE = new Type( "monster", DataTypes.TYPE_MONSTER );
	public static final Type ELEMENT_TYPE = new Type( "element", DataTypes.TYPE_ELEMENT );
	public static final Type COINMASTER_TYPE = new Type( "coinmaster", DataTypes.TYPE_COINMASTER );
	public static final Type PHYLUM_TYPE = new Type( "phylum", DataTypes.TYPE_PHYLUM );

	public static final Type STRICT_STRING_TYPE = new Type( "strict_string", DataTypes.TYPE_STRICT_STRING );
	public static final Type AGGREGATE_TYPE = new Type( "aggregate", DataTypes.TYPE_AGGREGATE );
	
	public static final AggregateType BOOLEAN_MAP_TYPE =
		new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE );

	public static final AggregateType RESULT_TYPE =
		new AggregateType( DataTypes.INT_TYPE, DataTypes.ITEM_TYPE );

	public static final AggregateType REGEX_GROUP_TYPE =
		new AggregateType(
			new AggregateType( DataTypes.STRING_TYPE, DataTypes.INT_TYPE ), DataTypes.INT_TYPE );

	// Common values

	public static final String[] BOOLEANS = { "true", "false" };

	public static final String[] STAT_ARRAY = new String[ Stat.values().length ];
	static
	{
		for ( int i = 0; i < Stat.values().length; i++ )
		{
			STAT_ARRAY[ i ] = Stat.values()[i].toString();
		}
	}

	public static final Value[] STAT_VALUES =
	{
		new Value( DataTypes.STAT_TYPE, Stat.MUSCLE.toString() ),
		new Value( DataTypes.STAT_TYPE, Stat.MYSTICALITY.toString() ),
		new Value( DataTypes.STAT_TYPE, Stat.MOXIE.toString() ),
		new Value( DataTypes.STAT_TYPE, Stat.SUBMUSCLE.toString() ),
		new Value( DataTypes.STAT_TYPE, Stat.SUBMYST.toString() ),
		new Value( DataTypes.STAT_TYPE, Stat.SUBMOXIE.toString() ),
	};

	public static final Value VOID_VALUE = new Value();
	public static final Value TRUE_VALUE = new Value( true );
	public static final Value FALSE_VALUE = new Value( false );
	public static final Value ZERO_VALUE = new Value( 0 );
	public static final Value ONE_VALUE = new Value( 1 );
	public static final Value ZERO_FLOAT_VALUE = new Value( 0.0 );
	public static final Value MUSCLE_VALUE = DataTypes.STAT_VALUES[0];
	public static final Value MYSTICALITY_VALUE = DataTypes.STAT_VALUES[1];
	public static final Value MOXIE_VALUE = DataTypes.STAT_VALUES[2];

	// Initial values for uninitialized variables

	// VOID_TYPE omitted since no variable can have that type
	public static final Value BOOLEAN_INIT = DataTypes.FALSE_VALUE;
	public static final Value INT_INIT = DataTypes.ZERO_VALUE;
	public static final Value FLOAT_INIT = DataTypes.ZERO_FLOAT_VALUE;
	public static final Value STRING_INIT = new Value( "" );

	public static final Value ITEM_INIT = new Value( DataTypes.ITEM_TYPE, -1, "none" );
	public static final Value LOCATION_INIT = new Value( DataTypes.LOCATION_TYPE, "none", (Object) null );
	public static final Value CLASS_INIT = new Value( DataTypes.CLASS_TYPE, -1, "none" );
	public static final Value STAT_INIT = new Value( DataTypes.STAT_TYPE, -1, "none" );
	public static final Value SKILL_INIT = new Value( DataTypes.SKILL_TYPE, -1, "none" );
	public static final Value EFFECT_INIT = new Value( DataTypes.EFFECT_TYPE, -1, "none" );
	public static final Value FAMILIAR_INIT = new Value( DataTypes.FAMILIAR_TYPE, -1, "none" );
	public static final Value SLOT_INIT = new Value( DataTypes.SLOT_TYPE, -1, "none" );
	public static final Value MONSTER_INIT = new Value( DataTypes.MONSTER_TYPE, "none", (Object) null );
	public static final Value ELEMENT_INIT = new Value( DataTypes.ELEMENT_TYPE, "none", (Object) null );
	public static final Value COINMASTER_INIT = new Value( DataTypes.COINMASTER_TYPE, "none", (Object) null );
	public static final Value PHYLUM_INIT = new Value( DataTypes.PHYLUM_TYPE, "none", (Object) null );

	public static final TypeList simpleTypes = new TypeList();

	static
	{
		simpleTypes.add( DataTypes.VOID_TYPE );
		simpleTypes.add( DataTypes.BOOLEAN_TYPE );
		simpleTypes.add( DataTypes.INT_TYPE );
		simpleTypes.add( DataTypes.FLOAT_TYPE );
		simpleTypes.add( DataTypes.STRING_TYPE );
		simpleTypes.add( DataTypes.BUFFER_TYPE );
		simpleTypes.add( DataTypes.MATCHER_TYPE );
		simpleTypes.add( DataTypes.AGGREGATE_TYPE );

		simpleTypes.add( DataTypes.ITEM_TYPE );
		simpleTypes.add( DataTypes.LOCATION_TYPE );
		simpleTypes.add( DataTypes.CLASS_TYPE );
		simpleTypes.add( DataTypes.STAT_TYPE );
		simpleTypes.add( DataTypes.SKILL_TYPE );
		simpleTypes.add( DataTypes.EFFECT_TYPE );
		simpleTypes.add( DataTypes.FAMILIAR_TYPE );
		simpleTypes.add( DataTypes.SLOT_TYPE );
		simpleTypes.add( DataTypes.MONSTER_TYPE );
		simpleTypes.add( DataTypes.ELEMENT_TYPE );
		simpleTypes.add( DataTypes.COINMASTER_TYPE );
		simpleTypes.add( DataTypes.PHYLUM_TYPE );
	}

	// For each simple data type X, we supply:
	// public static final ScriptValue parseXValue( String name );

	public static final Value parseBooleanValue( final String name, final boolean returnDefault )
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
			return makeBooleanValue( StringUtilities.parseInt( name ) );
		}

		return null;
	}

	public static final Value parseIntValue( final String name, final boolean returnDefault )
	{
		try
		{
			return new Value( StringUtilities.parseLong( name ) );
		}
		catch ( NumberFormatException e )
		{
			return returnDefault ? DataTypes.ZERO_VALUE : null;
		}
	}

	public static final Value parseFloatValue( final String name, final boolean returnDefault )
	{
		try
		{
			return new Value( StringUtilities.parseDouble( name ) );
		}
		catch ( NumberFormatException e )
		{
			return returnDefault ? DataTypes.ZERO_FLOAT_VALUE : null;
		}
	}

	public static final Value parseStringValue( final String name )
	{
		return new Value( name );
	}

	public static final Value parseItemValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.trim().equals( "" ) )
		{
			return returnDefault ? DataTypes.ITEM_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.ITEM_INIT;
		}

		// Allow for an item number to be specified
		// inside of the "item" construct.

		int itemId;
		
		if ( StringUtilities.isNumeric( name ) )
		{
			itemId = StringUtilities.parseInt( name );
			name = ItemDatabase.getItemDataName( itemId );

			if ( name == null  )
			{
				return returnDefault ? DataTypes.ITEM_INIT : null;
			}

			return new Value( DataTypes.ITEM_TYPE, itemId, name );
		}
		
		AdventureResult item = ItemFinder.getFirstMatchingItem( name, false );

		if ( item == null || item.getItemId() == -1 )
		{
			return returnDefault ? DataTypes.ITEM_INIT : null;
		}

		itemId = item.getItemId();
		name = ItemDatabase.getItemDataName( itemId );
		return new Value( DataTypes.ITEM_TYPE, itemId, name );
	}

	public static final Value parseLocationValue( final String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.LOCATION_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.LOCATION_INIT;
		}

		KoLAdventure content = AdventureDatabase.getAdventure( name );
		if ( content == null )
		{
			return returnDefault ? DataTypes.LOCATION_INIT : null;
		}

		return new Value( DataTypes.LOCATION_TYPE, content.getAdventureName(), (Object) content );
	}

	public static final Value parseLocationValue( final int adv, final boolean returnDefault )
	{
		if ( adv <= 0 )
		{
			return DataTypes.LOCATION_INIT;
		}

		KoLAdventure content = AdventureDatabase.getAdventureByURL(
			"adventure.php?snarfblat=" + adv );
		if ( content == null )
		{
			return returnDefault ? DataTypes.LOCATION_INIT : null;
		}

		return new Value( DataTypes.LOCATION_TYPE, content.getAdventureName(),
			(Object) content );
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

	public static final Value parseClassValue( final String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.CLASS_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.CLASS_INIT;
		}

		int num = DataTypes.classToInt( name );
		if ( num < 0 )
		{
			return returnDefault ? DataTypes.CLASS_INIT: null;
		}

		return new Value( DataTypes.CLASS_TYPE, num, DataTypes.CLASSES[ num ] );
	}

	public static final Value parseStatValue( final String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.STAT_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.STAT_INIT;
		}

		for ( int i = 0; i < DataTypes.STAT_VALUES.length; ++i )
		{
			if ( name.equalsIgnoreCase( DataTypes.STAT_VALUES[ i ].toString() ) )
			{
				return STAT_VALUES[ i ];
			}
		}

		return returnDefault ? DataTypes.STAT_INIT : null;
	}

	public static final Value parseSkillValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.SKILL_INIT : null;
		}

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
		name = SkillDatabase.getSkillDataName( num );
		return new Value( DataTypes.SKILL_TYPE, num, name );
	}

	public static final Value parseEffectValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.EFFECT_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.EFFECT_INIT;
		}

		AdventureResult effect = EffectDatabase.getFirstMatchingEffect( name, false );
		if ( effect == null )
		{
			return returnDefault ? DataTypes.EFFECT_INIT : null;
		}

		int num = EffectDatabase.getEffectId( effect.getName() );
		name = EffectDatabase.getEffectDataName( num );
		return new Value( DataTypes.EFFECT_TYPE, num, name );
	}

	public static final Value parseFamiliarValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.FAMILIAR_INIT : null;
		}

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
		return new Value( DataTypes.FAMILIAR_TYPE, num, name );
	}

	public static final Value parseSlotValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.SLOT_INIT : null;
		}

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
		return new Value( DataTypes.SLOT_TYPE, num, name );
	}

	public static final Value parseMonsterValue( final String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.MONSTER_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.MONSTER_INIT;
		}

		MonsterData monster = MonsterDatabase.findMonster( name, true );
		if ( monster == null )
		{
			return returnDefault ? DataTypes.MONSTER_INIT : null;
		}

		return new Value( DataTypes.MONSTER_TYPE, monster.getName(), monster );
	}

	public static final Value parseElementValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.ELEMENT_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.ELEMENT_INIT;
		}

		Element elem = MonsterDatabase.stringToElement( name );
		if ( elem == Element.NONE )
		{
			return returnDefault ? DataTypes.ELEMENT_INIT : null;
		}

		name = elem.toString();
		return new Value( DataTypes.ELEMENT_TYPE, name );
	}

	public static final Value parsePhylumValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.PHYLUM_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.PHYLUM_INIT;
		}

		Phylum phylum = MonsterDatabase.phylumNumber( name );
		if ( phylum == Phylum.NONE )
		{
			return returnDefault ? DataTypes.PHYLUM_INIT : null;
		}

		name = phylum.toString();
		return new Value( DataTypes.PHYLUM_TYPE, name );
	}

	public static final Value parseCoinmasterValue( String name, final boolean returnDefault )
	{
		if ( name == null || name.equals( "" ) )
		{
			return returnDefault ? DataTypes.COINMASTER_INIT : null;
		}

		if ( name.equalsIgnoreCase( "none" ) )
		{
			return DataTypes.COINMASTER_INIT;
		}

		CoinmasterData content = CoinmasterRegistry.findCoinmaster( name );
		if ( content == null )
		{
			return returnDefault ? DataTypes.COINMASTER_INIT : null;
		}

		return new Value( DataTypes.COINMASTER_TYPE, content.getMaster(), (Object) content );
	}

	public static final Value makeCoinmasterValue( final CoinmasterData data )
	{
		if ( data == null )
		{
			return DataTypes.COINMASTER_INIT;
		}

		return new Value( DataTypes.COINMASTER_TYPE, data.getMaster(), (Object) data );
	}

	public static final Value parseValue( final Type type, final String name, final boolean returnDefault )
	{
		return type.parseValue( name, returnDefault );
	}

	// For data types which map to integers, also supply:
	// public static final ScriptValue makeXValue( int num )

	public static final Value makeIntValue( final boolean val )
	{
		return val ? ONE_VALUE : ZERO_VALUE;
	}

	public static final Value makeIntValue( final long val )
	{
		return val == 0 ? ZERO_VALUE :
		       val == 1 ? ONE_VALUE :
		       new Value( val );
	}

	public static final Value makeFloatValue( final double val )
	{
		return val == 0.0 ? ZERO_FLOAT_VALUE : new Value( val );
	}

	public static final Value makeBooleanValue( final int num )
	{
		return makeBooleanValue( num != 0 );
	}

	public static final Value makeBooleanValue( final boolean value )
	{
		return value ? DataTypes.TRUE_VALUE : DataTypes.FALSE_VALUE;
	}

	public static final Value makeItemValue( final int num )
	{
		String name = ItemDatabase.getItemDataName( num );

		if ( name == null )
		{
			return DataTypes.ITEM_INIT;
		}

		return new Value( DataTypes.ITEM_TYPE, num, name );
	}

	public static final Value makeItemValue( String name )
	{
		int num = ItemDatabase.getItemId( name );

		if ( num == -1 )
		{
			return DataTypes.ITEM_INIT;
		}

		name = ItemDatabase.getItemDataName( num );
		return new Value( DataTypes.ITEM_TYPE, num, name );
	}

	public static final Value makeItemValue( final AdventureResult ar )
	{
		int num = ar.getItemId();
		String name = ItemDatabase.getItemDataName( num );
		return new Value( DataTypes.ITEM_TYPE, num, name );
	}

	public static final Value makeClassValue( final String name )
	{
		return new Value( DataTypes.CLASS_TYPE, DataTypes.classToInt( name ), name );
	}

	public static final Value makeSkillValue( final int num )
	{
		String name = SkillDatabase.getSkillDataName( num );
		if ( name == null )
		{
			return DataTypes.SKILL_INIT;
		}

		return new Value( DataTypes.SKILL_TYPE, num, name );
	}

	public static final Value makeEffectValue( final int num )
	{
		String name = EffectDatabase.getEffectDataName( num );
		if ( name == null )
		{
			return DataTypes.EFFECT_INIT;
		}
		return new Value( DataTypes.EFFECT_TYPE, num, name );
	}

	public static final Value makeFamiliarValue( final int num )
	{
		String name = FamiliarDatabase.getFamiliarName( num );
		if ( name == null )
		{
			return DataTypes.FAMILIAR_INIT;
		}
		return new Value( DataTypes.FAMILIAR_TYPE, num, name );
	}

	// Also supply:
	// public static final String promptForValue()

	public static String promptForValue( final Type type, final String name )
	{
		return DataTypes.promptForValue( type, "Please input a value for " + type + " " + name, name );
	}

	private static String promptForValue( final Type type, final String message, final String name )
	{
		switch ( type.getType() )
		{
		case TYPE_BOOLEAN:
			return (String) InputFieldUtilities.input( message, DataTypes.BOOLEANS );

		case TYPE_LOCATION: {
			LockableListModel inputs = AdventureDatabase.getAsLockableListModel();
			KoLAdventure initial = AdventureDatabase.getAdventure( Preferences.getString( "lastAdventure" ) );
			KoLAdventure value = (KoLAdventure) InputFieldUtilities.input( message, inputs, initial );
			return value == null ? null : value.getAdventureName();
		}

		case TYPE_SKILL: {
			Object [] inputs = SkillDatabase.getSkillsByType( SkillDatabase.CASTABLE ).toArray();
			UseSkillRequest value = (UseSkillRequest) InputFieldUtilities.input( message, inputs );
			return value == null ? null : value.getSkillName();
		}

		case TYPE_FAMILIAR: {
			Object [] inputs = KoLCharacter.getFamiliarList().toArray();
			FamiliarData initial = KoLCharacter.getFamiliar();
			FamiliarData value = (FamiliarData) InputFieldUtilities.input( message, inputs, initial );
			return value == null ? null : value.getRace();
		}

		case TYPE_SLOT:
			return (String) InputFieldUtilities.input( message, EquipmentRequest.slotNames );

		case TYPE_ELEMENT:
			return (String) InputFieldUtilities.input( message, MonsterDatabase.ELEMENT_ARRAY );

		case TYPE_COINMASTER:
			return (String) InputFieldUtilities.input( message, CoinmasterRegistry.MASTERS );

		case TYPE_PHYLUM:
			return (String) InputFieldUtilities.input( message, MonsterDatabase.PHYLUM_ARRAY );

		case TYPE_CLASS:
			return (String) InputFieldUtilities.input( message, DataTypes.CLASSES );

		case TYPE_STAT:
			return (String) InputFieldUtilities.input( message, DataTypes.STAT_ARRAY );

		case TYPE_INT:
		case TYPE_FLOAT:
		case TYPE_STRING:
		case TYPE_ITEM:
		case TYPE_EFFECT:
		case TYPE_MONSTER:
			return InputFieldUtilities.input( message );

		default:
			throw new ScriptException( "Internal error: Illegal type for main() parameter" );
		}
	}
}
