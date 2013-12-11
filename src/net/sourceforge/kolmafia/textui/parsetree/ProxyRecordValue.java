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

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.PastaThrallData;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Interpreter;

public class ProxyRecordValue
	extends RecordValue
{
	public ProxyRecordValue( final RecordType type, final Value obj )
	{
		super( type );

		this.contentLong = obj.contentLong;
		this.contentString = obj.contentString;
		this.content = obj.content;
	}

	@Override
	public Value aref( final Value key, final Interpreter interpreter )
	{
		int index = ( (RecordType) this.type ).indexOf( key );
		if ( index < 0 )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}
		return this.aref( index, interpreter );
	}

	@Override
	public Value aref( final int index, final Interpreter interpreter )
	{
		RecordType type = (RecordType) this.type;
		int size = type.fieldCount();
		if ( index < 0 || index >= size )
		{
			throw interpreter.runtimeException( "Internal error: field index out of bounds" );
		}

		Object rv;
		try
		{
			rv = this.getClass().getMethod( "get_" + type.getFieldNames()[ index ] ).invoke( this );
		}
		catch ( InvocationTargetException e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e.getCause() );
		}
		catch ( Exception e )
		{
			throw interpreter.runtimeException( "Unable to invoke attribute getter: " + e );
		}

		if ( rv == null )
		{
			return type.getFieldTypes()[ index ].initialValue();
		}

		if ( rv instanceof Value )
		{
			return (Value) rv;
		}

		if ( rv instanceof Integer )
		{
			return DataTypes.makeIntValue( ((Integer) rv).intValue() );
		}

		if ( rv instanceof Double )
		{
			return DataTypes.makeFloatValue( ((Double) rv).doubleValue() );
		}

		if ( rv instanceof String )
		{
			return new Value( rv.toString() );
		}

		if ( rv instanceof Boolean )
		{
			return DataTypes.makeBooleanValue( ((Boolean) rv).booleanValue() );
		}

		if ( rv instanceof CoinmasterData )
		{
			return DataTypes.makeCoinmasterValue( (CoinmasterData) rv );
		}

		throw interpreter.runtimeException( "Unable to convert attribute value of type: " + rv.getClass() );
	}

	@Override
	public void aset( final Value key, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public void aset( final int index, final Value val, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public Value remove( final Value key, final Interpreter interpreter )
	{
		throw interpreter.runtimeException( "Cannot assign to a proxy record field" );
	}

	@Override
	public void clear()
	{
	}

	/* Helper for building parallel arrays of field names & types */
	private static class RecordBuilder
	{
		private ArrayList<String> names;
		private ArrayList<Type> types;

		public RecordBuilder()
		{
			names = new ArrayList<String>();
			types = new ArrayList<Type>();
		}

		public RecordBuilder add( String name, Type type )
		{
			this.names.add( name.toLowerCase() );
			this.types.add( type );
			return this;
		}

		public RecordType finish( String name )
		{
			int len = this.names.size();
			return new RecordType( name,
				this.names.toArray( new String[len] ),
				this.types.toArray( new Type[len] ) );
		}
	}

	public static class ClassProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "primestat", DataTypes.STAT_TYPE )
			.finish( "class proxy" );

		public ClassProxy( Value obj )
		{
			super( _type, obj );
		}

		public Value get_primestat()
		{
			int primeIndex = KoLCharacter.getPrimeIndex( this.contentString );

			String name = AdventureResult.STAT_NAMES[ primeIndex ];

			return DataTypes.parseStatValue( name, true );
		}
	}

	public static class ItemProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "plural", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "smallimage", DataTypes.STRING_TYPE )
			.add( "levelreq", DataTypes.INT_TYPE )
			.add( "quality", DataTypes.STRING_TYPE )
			.add( "adventures", DataTypes.STRING_TYPE )
			.add( "muscle", DataTypes.STRING_TYPE )
			.add( "mysticality", DataTypes.STRING_TYPE )
			.add( "moxie", DataTypes.STRING_TYPE )
			.add( "fullness", DataTypes.INT_TYPE )
			.add( "inebriety", DataTypes.INT_TYPE )
			.add( "spleen", DataTypes.INT_TYPE )
			.add( "minhp", DataTypes.INT_TYPE )
			.add( "maxhp", DataTypes.INT_TYPE )
			.add( "minmp", DataTypes.INT_TYPE )
			.add( "maxmp", DataTypes.INT_TYPE )
			.add( "notes", DataTypes.STRING_TYPE )
			.add( "quest", DataTypes.BOOLEAN_TYPE )
			.add( "gift", DataTypes.BOOLEAN_TYPE )
			.add( "tradeable", DataTypes.BOOLEAN_TYPE )
			.add( "discardable", DataTypes.BOOLEAN_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "reusable", DataTypes.BOOLEAN_TYPE )
			.add( "usable", DataTypes.BOOLEAN_TYPE )
			.add( "multi", DataTypes.BOOLEAN_TYPE )
			.add( "fancy", DataTypes.BOOLEAN_TYPE )
			.add( "candy", DataTypes.BOOLEAN_TYPE )
			.add( "bounty", DataTypes.LOCATION_TYPE )
			.add( "bounty_count", DataTypes.INT_TYPE )
			.add( "seller", DataTypes.COINMASTER_TYPE )
			.add( "buyer", DataTypes.COINMASTER_TYPE )
			.add( "name_length", DataTypes.INT_TYPE )
			.finish( "item proxy" );

		public ItemProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_plural()
		{
			return ItemDatabase.getPluralName( this.contentString );
		}

		public String get_descid()
		{
			return ItemDatabase.getDescriptionId( this.contentString );
		}

		public String get_image()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getImage( id );
		}

		public String get_smallimage()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getSmallImage( id );
		}

		public Integer get_levelreq()
		{
			return ItemDatabase.getLevelReqByName( this.contentString );
		}

		public String get_quality()
		{
			return ItemDatabase.getQuality( this.contentString );
		}

		public String get_adventures()
		{
			return ItemDatabase.getAdvRangeByName( this.contentString );
		}

		public String get_muscle()
		{
			return ItemDatabase.getMuscleByName( this.contentString );
		}

		public String get_mysticality()
		{
			return ItemDatabase.getMysticalityByName( this.contentString );
		}

		public String get_moxie()
		{
			return ItemDatabase.getMoxieByName( this.contentString );
		}

		public int get_fullness()
		{
			return ItemDatabase.getFullness( this.contentString );
		}

		public int get_inebriety()
		{
			return ItemDatabase.getInebriety( this.contentString );
		}

		public int get_spleen()
		{
			return ItemDatabase.getSpleenHit( this.contentString );
		}

		public int get_minhp()
		{
			return ItemDatabase.getRestoreHPMin( this.contentString );
		}

		public int get_maxhp()
		{
			return ItemDatabase.getRestoreHPMax( this.contentString );
		}

		public int get_minmp()
		{
			return ItemDatabase.getRestoreMPMin( this.contentString );
		}

		public int get_maxmp()
		{
			return ItemDatabase.getRestoreMPMax( this.contentString );
		}

		public String get_notes()
		{
			return ItemDatabase.getNotes( this.contentString );
		}

		public boolean get_quest()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.isQuestItem( id );
		}

		public boolean get_gift()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.isGiftItem( id );
		}

		public boolean get_tradeable()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.isTradeable( id );
		}

		public boolean get_discardable()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.isDiscardable( id );
		}

		public boolean get_combat()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getAttribute( id, ItemDatabase.ATTR_COMBAT | ItemDatabase.ATTR_COMBAT_REUSABLE );
		}

		public boolean get_reusable()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getConsumptionType( id ) == KoLConstants.INFINITE_USES ||
				ItemDatabase.getAttribute( id, ItemDatabase.ATTR_REUSABLE | ItemDatabase.ATTR_COMBAT_REUSABLE );
		}

		public boolean get_usable()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.isUsable( id );
		}

		public boolean get_multi()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.isMultiUsable( id );
		}

		public boolean get_fancy()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.isFancyItem( id );
		}

		public boolean get_candy()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.isCandyItem( id );
		}

		public Value get_bounty()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			KoLAdventure adventure = AdventureDatabase.getBountyLocation( id );
			return adventure == null ?
			       DataTypes.LOCATION_INIT :
			       DataTypes.parseLocationValue( adventure.getAdventureName(), true );
		}

		public int get_bounty_count()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			AdventureResult bounty = AdventureDatabase.getBounty( id );
			return bounty == null ? 0 : bounty.getCount();
		}

		public CoinmasterData get_seller()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			String itemName = ItemDatabase.getItemName( id );
			return CoinmasterRegistry.findSeller( itemName );
		}

		public CoinmasterData get_buyer()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			String itemName = ItemDatabase.getItemName( id );
			return CoinmasterRegistry.findBuyer( itemName );
		}

		public int get_name_length()
		{
			int id = ItemDatabase.getItemId( this.contentString );
			return ItemDatabase.getNameLength( id );
		}
	}

	public static class FamiliarProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "hatchling", DataTypes.ITEM_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "name", DataTypes.STRING_TYPE )
			.add( "charges", DataTypes.INT_TYPE )
			.finish( "familiar proxy" );

		public FamiliarProxy( Value obj )
		{
			super( _type, obj );
		}

		public boolean get_combat()
		{
			return FamiliarDatabase.isCombatType( (int)this.contentLong );
		}

		public Value get_hatchling()
		{
			return DataTypes.makeItemValue(
				FamiliarDatabase.getFamiliarLarva( (int)this.contentLong ) );
		}

		public String get_image()
		{
			return FamiliarDatabase.getFamiliarImageLocation( (int)this.contentLong );
		}

		public String get_name()
		{
			FamiliarData fam = KoLCharacter.findFamiliar( this.contentString );
			return fam == null ? "" : fam.getName();
		}
		public int get_charges()
		{
			FamiliarData fam = KoLCharacter.findFamiliar( this.contentString );
			return fam == null ? 0 : fam.getCharges();
		}
	}

	public static class ThrallProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "id", DataTypes.INT_TYPE )
			.add( "name", DataTypes.STRING_TYPE )
			.add( "level", DataTypes.INT_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "tinyimage", DataTypes.STRING_TYPE )
			.add( "skill", DataTypes.SKILL_TYPE )
			.add( "current_modifiers", DataTypes.STRING_TYPE )
			.finish( "thrall proxy" );

		public ThrallProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_id()
		{
			Object [] data = (Object [])this.content;
			return data == null ? 0 : PastaThrallData.dataToId( data );
		}

		public String get_name()
		{
			PastaThrallData thrall = KoLCharacter.findPastaThrall( this.contentString );
			return thrall == null ? "" : thrall.getName();
		}

		public int get_level()
		{
			PastaThrallData thrall = KoLCharacter.findPastaThrall( this.contentString );
			return thrall == null ? 0 : thrall.getLevel();
		}

		public String get_image()
		{
			Object [] data = (Object [])this.content;
			return data == null ? "" : PastaThrallData.dataToImage( data );
		}

		public String get_tinyimage()
		{
			Object [] data = (Object [])this.content;
			return data == null ? "" : PastaThrallData.dataToTinyImage( data );
		}

		public Value get_skill()
		{
			Object [] data = (Object [])this.content;
			return DataTypes.makeSkillValue( data == null ? 0 : PastaThrallData.dataToSkillId( data ) );
		}

		public String get_current_modifiers()
		{
			PastaThrallData thrall = KoLCharacter.findPastaThrall( this.contentString );
			return thrall == null ? "" : thrall.getCurrentModifiers();
		}
	}

	public static class SkillProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "level", DataTypes.INT_TYPE )
			.add( "traincost", DataTypes.INT_TYPE )
			.add( "class", DataTypes.CLASS_TYPE )
			.add( "libram", DataTypes.BOOLEAN_TYPE )
			.add( "passive", DataTypes.BOOLEAN_TYPE )
			.add( "buff", DataTypes.BOOLEAN_TYPE )
			.add( "combat", DataTypes.BOOLEAN_TYPE )
			.add( "song", DataTypes.BOOLEAN_TYPE )
			.add( "expression", DataTypes.BOOLEAN_TYPE )
			.add( "permable", DataTypes.BOOLEAN_TYPE )
			.add( "dailylimit", DataTypes.INT_TYPE )
			.add( "timescast", DataTypes.INT_TYPE )
			.finish( "skill proxy" );

		public SkillProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_level()
		{
			return SkillDatabase.getSkillLevel( (int)this.contentLong );
		}

		public int get_traincost()
		{
			return SkillDatabase.getSkillPurchaseCost( (int)this.contentLong );
		}

		public Value get_class()
		{
			return DataTypes.parseClassValue(
				SkillDatabase.getSkillCategory( (int)this.contentLong ), true );
		}

		public boolean get_libram()
		{
			return SkillDatabase.isLibramSkill( (int)this.contentLong );
		}

		public boolean get_passive()
		{
			return SkillDatabase.isPassive( (int)this.contentLong );
		}

		public boolean get_buff()
		{
			return SkillDatabase.isBuff( (int)this.contentLong );
		}

		public boolean get_combat()
		{
			return SkillDatabase.isCombat( (int)this.contentLong );
		}

		public boolean get_song()
		{
			return SkillDatabase.isSong( (int)this.contentLong );
		}

		public boolean get_expression()
		{
			return SkillDatabase.isExpression( (int)this.contentLong );
		}

		public boolean get_permable()
		{
			return SkillDatabase.isPermable( (int)this.contentLong );
		}
		public int get_dailylimit()
		{
			return SkillDatabase.getMaxCasts( (int)this.contentLong );
		}
		public int get_timescast()
		{
			return SkillDatabase.getCasts( (int)this.contentLong );
		}
	}

	public static class EffectProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "default", DataTypes.STRING_TYPE )
			.add( "note", DataTypes.STRING_TYPE )
			.add( "all",
				new AggregateType( DataTypes.BOOLEAN_TYPE, DataTypes.STRING_TYPE ) )
			.add( "image", DataTypes.STRING_TYPE )
			.add( "descid", DataTypes.STRING_TYPE )
			.finish( "effect proxy" );

		public EffectProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_default()
		{
			return EffectDatabase.getDefaultAction( this.contentString );
		}

		public String get_note()
		{
			return EffectDatabase.getActionNote( this.contentString );
		}

		public Value get_all()
		{
			ArrayList<Value> rv = new ArrayList<Value>();
			Iterator i = EffectDatabase.getAllActions( this.contentString );
			while ( i.hasNext() )
			{
				rv.add( new Value( (String) i.next() ) );
			}
			return new PluralValue( DataTypes.STRING_TYPE, rv );
		}

		public String get_image()
		{
			return EffectDatabase.getImage( (int)this.contentLong );
		}

		public String get_descid()
		{
			return EffectDatabase.getDescriptionId( (int)this.contentLong );
		}
	}

	public static class LocationProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "nocombats", DataTypes.BOOLEAN_TYPE )
			.add( "zone", DataTypes.STRING_TYPE )
			.add( "parent", DataTypes.STRING_TYPE )
			.add( "parentdesc", DataTypes.STRING_TYPE )
			.add( "environment", DataTypes.STRING_TYPE )
			.add( "bounty", DataTypes.ITEM_TYPE )
			.add( "combat_queue", DataTypes.STRING_TYPE )
			.add( "noncombat_queue", DataTypes.STRING_TYPE )
			.add( "kisses", DataTypes.INT_TYPE )
			.finish( "location proxy" );

		public LocationProxy( Value obj )
		{
			super( _type, obj );
		}

		public boolean get_nocombats()
		{
			return ((KoLAdventure) this.content).isNonCombatsOnly();
		}

		public String get_zone()
		{
			return ((KoLAdventure) this.content).getZone();
		}

		public String get_parent()
		{
			return ((KoLAdventure) this.content).getParentZone();
		}

		public String get_parentdesc()
		{
			return ((KoLAdventure) this.content).getParentZoneDescription();
		}

		public String get_environment()
		{
			return ((KoLAdventure) this.content).getEnvironment();
		}

		public Value get_bounty()
		{
			AdventureResult bounty = AdventureDatabase.getBounty( (KoLAdventure) this.content );
			return bounty == null ?
			       DataTypes.ITEM_INIT :
			       DataTypes.parseItemValue( bounty.getName(), true );
		}

		public String get_combat_queue()
		{
			List<?> zoneQueue = AdventureQueueDatabase.getZoneQueue( (KoLAdventure) this.content );
			if ( zoneQueue == null )
			{
				return "";
			}

			StringBuilder builder = new StringBuilder();
			for ( Object ob : zoneQueue )
			{
				if ( ob == null )
					continue;

				if ( builder.length() > 0 )
					builder.append( "; " );

				builder.append( ob.toString() );
			}

			return builder.toString();
		}
		
		public String get_noncombat_queue()
		{
			List<?> zoneQueue = AdventureQueueDatabase.getZoneNoncombatQueue( (KoLAdventure) this.content );
			if ( zoneQueue == null )
			{
				return "";
			}

			StringBuilder builder = new StringBuilder();
			for ( Object ob : zoneQueue )
			{
				if ( ob == null )
					continue;

				if ( builder.length() > 0 )
					builder.append( "; " );

				builder.append( ob.toString() );
			}

			return builder.toString();
		}

		public int get_kisses()
		{
			return FightRequest.dreadKisses( (KoLAdventure)this.content );
		}
	}

	public static class MonsterProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "base_hp", DataTypes.INT_TYPE )
			.add( "base_attack", DataTypes.INT_TYPE )
			.add( "base_defense", DataTypes.INT_TYPE )
			.add( "raw_hp", DataTypes.INT_TYPE )
			.add( "raw_attack", DataTypes.INT_TYPE )
			.add( "raw_defense", DataTypes.INT_TYPE )
			.add( "base_initiative", DataTypes.INT_TYPE )
			.add( "raw_initiative", DataTypes.INT_TYPE )
			.add( "attack_element", DataTypes.ELEMENT_TYPE )
			.add( "defense_element", DataTypes.ELEMENT_TYPE )
			.add( "physical_resistance", DataTypes.INT_TYPE )
			.add( "min_meat", DataTypes.INT_TYPE )
			.add( "max_meat", DataTypes.INT_TYPE )
			.add( "base_mainstat_exp", DataTypes.FLOAT_TYPE )
			.add( "phylum", DataTypes.PHYLUM_TYPE )
			.add( "poison", DataTypes.EFFECT_TYPE )
			.add( "boss", DataTypes.BOOLEAN_TYPE )
			.add( "image", DataTypes.STRING_TYPE )
			.finish( "monster proxy" );

		public MonsterProxy( Value obj )
		{
			super( _type, obj );
		}

		public int get_base_hp()
		{
			return ((MonsterData) this.content).getHP();
		}

		public int get_base_attack()
		{
			return ((MonsterData) this.content).getAttack();
		}

		public int get_raw_hp()
		{
			return ((MonsterData) this.content).getRawHP();
		}

		public int get_raw_attack()
		{
			return ((MonsterData) this.content).getRawAttack();
		}

		public int get_raw_defense()
		{
			return ((MonsterData) this.content).getRawDefense();
		}

		public int get_base_defense()
		{
			return ((MonsterData) this.content).getDefense();
		}

		public int get_base_initiative()
		{
			return ((MonsterData) this.content).getInitiative();
		}

		public int get_raw_initiative()
		{
			return ((MonsterData) this.content).getRawInitiative();
		}

		public Value get_attack_element()
		{
			return DataTypes.parseElementValue(
				((MonsterData) this.content).getAttackElement().toString(),
				true );
		}

		public Value get_defense_element()
		{
			return DataTypes.parseElementValue(
				((MonsterData) this.content).getDefenseElement().toString(),
				true );
		}

		public int get_physical_resistance()
		{
			return ((MonsterData) this.content).getPhysicalResistance();
		}

		public int get_min_meat()
		{
			return ((MonsterData) this.content).getMinMeat();
		}

		public int get_max_meat()
		{
			return ((MonsterData) this.content).getMaxMeat();
		}

		public double get_base_mainstat_exp()
		{
			return ((MonsterData) this.content).getExperience();
		}

		public Value get_phylum()
		{
			return DataTypes.parsePhylumValue(
				((MonsterData) this.content).getPhylum().toString(),
				true );
		}

		public Value get_poison()
		{
			int poisonLevel = ((MonsterData) this.content).getPoison();
			String poisonName = poisonLevel == Integer.MAX_VALUE ?
				"none" :
				EffectDatabase.getEffectName( EffectDatabase.POISON_ID[ poisonLevel ] );
			return DataTypes.parseEffectValue( poisonName, true );
		}

		public boolean get_boss()
		{
			return ((MonsterData) this.content).isBoss();
		}

		public String get_image()
		{
			return ((MonsterData) this.content).getImage();
		}
	}

	public static class CoinmasterProxy
		extends ProxyRecordValue
	{
		public static RecordType _type = new RecordBuilder()
			.add( "token", DataTypes.STRING_TYPE )
			.add( "item", DataTypes.ITEM_TYPE )
			.add( "property", DataTypes.STRING_TYPE )
			.add( "available_tokens", DataTypes.INT_TYPE )
			.add( "buys", DataTypes.BOOLEAN_TYPE )
			.add( "sells", DataTypes.BOOLEAN_TYPE )
			.finish( "coinmaster proxy" );

		public CoinmasterProxy( Value obj )
		{
			super( _type, obj );
		}

		public String get_token()
		{
			return ((CoinmasterData) this.content).getToken();
		}

		public Value get_item()
		{
			CoinmasterData data = ((CoinmasterData) this.content);
			AdventureResult item = data.getItem();
			return item == null ?
			       DataTypes.ITEM_INIT :
			       DataTypes.parseItemValue( item.getName(), true );
		}

		public String get_property()
		{
			return ((CoinmasterData) this.content).getProperty();
		}

		public int get_available_tokens()
		{
			return ((CoinmasterData) this.content).availableTokens();
		}

		public boolean get_buys()
		{
			return ((CoinmasterData) this.content).getSellAction() != null;
		}

		public boolean get_sells()
		{
			return ((CoinmasterData) this.content).getBuyAction() != null;
		}
	}
}
