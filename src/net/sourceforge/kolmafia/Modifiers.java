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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Modifiers
	extends KoLDatabase
{
	private static final Map modifiersByName = new TreeMap();
	private static final HashMap familiarEffectByName = new HashMap();
	private static final ArrayList passiveSkills = new ArrayList();
	public static String currentLocation = "";
	public static String currentZone = "";
	public static String currentFamiliar = "";
	private static float currentWeight = 0.0f;

	private static final Pattern FAMILIAR_EFFECT_PATTERN =
		Pattern.compile( "Familiar Effect: \"(.*?)\"" );
	private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN =
		Pattern.compile( "([\\d.]+)\\s*x\\s*(Volley|Somb|Lep|Fairy)" );
	private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT = "$2: $1 ";
	private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN2 =
		Pattern.compile( "cap ([\\d.]+)" );
	private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2 = "Familiar Weight Cap: $1 ";

	static
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "modifiers.txt", KoLConstants.MODIFIERS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length != 2 )
			{
				continue;
			}

			String name = StringUtilities.getCanonicalName( data[ 0 ] );
			if ( Modifiers.modifiersByName.containsKey( name ) )
			{
				KoLmafia.updateDisplay( "Duplicate modifiers for: " + name );
			}
			Modifiers.modifiersByName.put( name, data[ 1 ] );
			
			Matcher matcher = FAMILIAR_EFFECT_PATTERN.matcher( data[ 1 ] );
			if ( matcher.find() )
			{
				Modifiers.familiarEffectByName.put( name, matcher.group( 1 ) );
				String effect = matcher.group( 1 );
				matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN.matcher( effect );
				if ( matcher.find() )
				{
					effect = matcher.replaceAll( FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT );
				}
				matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN2.matcher( effect );
				if ( matcher.find() )
				{
					effect = matcher.replaceAll( FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2 );
				}
                                Modifiers.modifiersByName.put( "fameq:" + name, effect );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final int FAMILIAR_WEIGHT = 0;
	public static final int MONSTER_LEVEL = 1;
	public static final int COMBAT_RATE = 2;
	public static final int INITIATIVE = 3;
	public static final int EXPERIENCE = 4;
	public static final int ITEMDROP = 5;
	public static final int MEATDROP = 6;
	public static final int DAMAGE_ABSORPTION = 7;
	public static final int DAMAGE_REDUCTION = 8;
	public static final int COLD_RESISTANCE = 9;
	public static final int HOT_RESISTANCE = 10;
	public static final int SLEAZE_RESISTANCE = 11;
	public static final int SPOOKY_RESISTANCE = 12;
	public static final int STENCH_RESISTANCE = 13;
	public static final int MANA_COST = 14;
	public static final int MOX = 15;
	public static final int MOX_PCT = 16;
	public static final int MUS = 17;
	public static final int MUS_PCT = 18;
	public static final int MYS = 19;
	public static final int MYS_PCT = 20;
	public static final int HP = 21;
	public static final int HP_PCT = 22;
	public static final int MP = 23;
	public static final int MP_PCT = 24;
	public static final int WEAPON_DAMAGE = 25;
	public static final int RANGED_DAMAGE = 26;
	public static final int SPELL_DAMAGE = 27;
	public static final int SPELL_DAMAGE_PCT = 28;
	public static final int COLD_DAMAGE = 29;
	public static final int HOT_DAMAGE = 30;
	public static final int SLEAZE_DAMAGE = 31;
	public static final int SPOOKY_DAMAGE = 32;
	public static final int STENCH_DAMAGE = 33;
	public static final int COLD_SPELL_DAMAGE = 34;
	public static final int HOT_SPELL_DAMAGE = 35;
	public static final int SLEAZE_SPELL_DAMAGE = 36;
	public static final int SPOOKY_SPELL_DAMAGE = 37;
	public static final int STENCH_SPELL_DAMAGE = 38;
	public static final int CRITICAL = 39;
	public static final int FUMBLE = 40;
	public static final int HP_REGEN_MIN = 41;
	public static final int HP_REGEN_MAX = 42;
	public static final int MP_REGEN_MIN = 43;
	public static final int MP_REGEN_MAX = 44;
	public static final int ADVENTURES = 45;
	public static final int FAMILIAR_WEIGHT_PCT = 46;
	public static final int WEAPON_DAMAGE_PCT = 47;
	public static final int RANGED_DAMAGE_PCT = 48;
	public static final int STACKABLE_MANA_COST = 49;
	public static final int HOBO_POWER = 50;
	public static final int BASE_RESTING_HP = 51;
	public static final int RESTING_HP_PCT = 52;
	public static final int BONUS_RESTING_HP = 53;
	public static final int BASE_RESTING_MP = 54;
	public static final int RESTING_MP_PCT = 55;
	public static final int BONUS_RESTING_MP = 56;
	public static final int CRITICAL_PCT = 57;
	public static final int PVP_FIGHTS = 58;
	public static final int VOLLEYBALL_WEIGHT = 59;
	public static final int SOMBRERO_WEIGHT = 60;
	public static final int LEPRECHAUN_WEIGHT = 61;
	public static final int FAIRY_WEIGHT = 62;
	public static final int MEATDROP_PENALTY = 63;
	public static final int HIDDEN_FAMILIAR_WEIGHT = 64;
	public static final int ITEMDROP_PENALTY = 65;
	public static final int INITIATIVE_PENALTY = 66;
	public static final int FOODDROP = 67;
	public static final int BOOZEDROP = 68;
	public static final int HATDROP = 69;
	public static final int WEAPONDROP = 70;
	public static final int OFFHANDDROP = 71;
	public static final int SHIRTDROP = 72;
	public static final int PANTSDROP = 73;
	public static final int ACCESSORYDROP = 74;
	public static final int VOLLEYBALL_EFFECTIVENESS = 75;
	public static final int SOMBRERO_EFFECTIVENESS = 76;
	public static final int LEPRECHAUN_EFFECTIVENESS = 77;
	public static final int FAIRY_EFFECTIVENESS = 78;
	public static final int FAMILIAR_WEIGHT_CAP = 79;
	public static final int SLIME_RESISTANCE = 80;
	public static final int SLIME_HATES_IT = 81;
	
	public static final String EXPR = "(?:([+-]?[\\d.]+)|\\[([^]]+)\\])";

	private static final Object[][] floatModifiers =
	{
		{ "Familiar Weight",
		  Pattern.compile( "([+-]\\d+) to Familiar Weight" ),
		  Pattern.compile( "Familiar Weight: " + EXPR )
		},
		{ "Monster Level",
		  Pattern.compile( "([+-]\\d+) to Monster Level" ),
		  Pattern.compile( "Monster Level: " + EXPR )
		},
		{ "Combat Rate",
		  null,
		  Pattern.compile( "Combat Rate: " + EXPR )
		},
		{ "Initiative",
		  new Object[] {
			Pattern.compile( "Combat Initiative ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Combat Initiative" ),
		  },
		  Pattern.compile( "Initiative: " + EXPR )
		},
		{ "Experience",
		  Pattern.compile( "([+-]\\d+) Stat.*Per Fight" ),
		  Pattern.compile( "Experience: " + EXPR )
		},
		{ "Item Drop",
		  Pattern.compile( "([+-]\\d+)% Item Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Item Drop: " + EXPR )
		},
		{ "Meat Drop",
		  Pattern.compile( "([+-]\\d+)% Meat from Monsters" ),
		  Pattern.compile( "Meat Drop: " + EXPR )
		},
		{ "Damage Absorption",
		  Pattern.compile( "Damage Absorption ([+-]\\d+)" ),
		  Pattern.compile( "Damage Absorption: " + EXPR )
		},
		{ "Damage Reduction",
		  Pattern.compile( "Damage Reduction: ([+-]?\\d+)" ),
		  Pattern.compile( "Damage Reduction: " + EXPR )
		},
		{ "Cold Resistance",
		  null,
		  Pattern.compile( "Cold Resistance: " + EXPR )
		},
		{ "Hot Resistance",
		  null,
		  Pattern.compile( "Hot Resistance: " + EXPR )
		},
		{ "Sleaze Resistance",
		  null,
		  Pattern.compile( "Sleaze Resistance: " + EXPR )
		},
		{ "Spooky Resistance",
		  null,
		  Pattern.compile( "Spooky Resistance: " + EXPR )
		},
		{ "Stench Resistance",
		  null,
		  Pattern.compile( "Stench Resistance: " + EXPR )
		},
		{ "Mana Cost",
		  Pattern.compile( "([+-]\\d+) MP to use Skills" ),
		  Pattern.compile( "Mana Cost: " + EXPR )
		},
		{ "Moxie",
		  new Object[] {
			Pattern.compile( "Moxie ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Moxie$" ),
		  },
		  Pattern.compile( "Moxie: " + EXPR )
		},
		{ "Moxie Percent",
		  new Object[] {
			Pattern.compile( "Moxie ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Moxie" ),
		  },
		  Pattern.compile( "Moxie Percent: " + EXPR )
		},
		{ "Muscle",
		  new Object[] {
			Pattern.compile( "Muscle ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Muscle$" ),
		  },
		  Pattern.compile( "Muscle: " + EXPR )
		},
		{ "Muscle Percent",
		  new Object[] {
			Pattern.compile( "Muscle ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Muscle" ),
		  },
		  Pattern.compile( "Muscle Percent: " + EXPR )
		},
		{ "Mysticality",
		  new Object[] {
			Pattern.compile( "Mysticality ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Mysticality$" ),
		  },
		  Pattern.compile( "Mysticality: " + EXPR )
		},
		{ "Mysticality Percent",
		  new Object[] {
			Pattern.compile( "Mysticality ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Mysticality" ),
		  },
		  Pattern.compile( "Mysticality Percent: " + EXPR )
		},
		{ "Maximum HP",
		  Pattern.compile( "Maximum HP ([+-]\\d+)$" ),
		  Pattern.compile( "Maximum HP: " + EXPR )
		},
		{ "Maximum HP Percent",
		  null,
		  Pattern.compile( "Maximum HP Percent: " + EXPR )
		},
		{ "Maximum MP",
		  Pattern.compile( "Maximum MP ([+-]\\d+)$" ),
		  Pattern.compile( "Maximum MP: " + EXPR )
		},
		{ "Maximum MP Percent",
		  null,
		  Pattern.compile( "Maximum MP Percent: " + EXPR )
		},
		{ "Weapon Damage",
		  new Object[] {
			Pattern.compile( "Weapon Damage ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Weapon Damage" ),
		  },
		  Pattern.compile( "Weapon Damage: " + EXPR )
		},
		{ "Ranged Damage",
		  new Object[] {
			Pattern.compile( "Ranged Damage ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Ranged Damage" ),
		  },
		  Pattern.compile( "Ranged Damage: " + EXPR )
		},
		{ "Spell Damage",
		  new Object[] {
			Pattern.compile( "Spell Damage ([+-]\\d+)$" ),
			Pattern.compile( "([+-]\\d+) Spell Damage" ),
		  },
		  Pattern.compile( "(?:^|, )Spell Damage: " + EXPR )
		},
		{ "Spell Damage Percent",
		  new Object[] {
			Pattern.compile( "Spell Damage ([+-]\\d+)%" ),
			Pattern.compile( "([+-]\\d+)% Spell Damage" ),
		  },
		  Pattern.compile( "Spell Damage Percent: " + EXPR )
		},
		{ "Cold Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=blue>Cold Damage</font>" ),
		  Pattern.compile( "Cold Damage: " + EXPR )
		},
		{ "Hot Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=red>Hot Damage</font>" ),
		  Pattern.compile( "Hot Damage: " + EXPR )
		},
		{ "Sleaze Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=blueviolet>Sleaze Damage</font>" ),
		  Pattern.compile( "Sleaze Damage: " + EXPR )
		},
		{ "Spooky Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=gray>Spooky Damage</font>" ),
		  Pattern.compile( "Spooky Damage: " + EXPR )
		},
		{ "Stench Damage",
		  Pattern.compile( "^([+-]\\d+) <font color=green>Stench Damage</font>" ),
		  Pattern.compile( "Stench Damage: " + EXPR )
		},
		{ "Cold Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=blue>Cold Spells</font>" ),
		  Pattern.compile( "Cold Spell Damage: " + EXPR )
		},
		{ "Hot Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=red>Hot Spells</font>" ),
		  Pattern.compile( "Hot Spell Damage: " + EXPR )
		},
		{ "Sleaze Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=blueviolet>Sleaze Spells</font>" ),
		  Pattern.compile( "Sleaze Spell Damage: " + EXPR )
		},
		{ "Spooky Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=gray>Spooky Spells</font>" ),
		  Pattern.compile( "Spooky Spell Damage: " + EXPR )
		},
		{ "Stench Spell Damage",
		  Pattern.compile( "^([+-]\\d+) (Damage )?to <font color=green>Stench Spells</font>" ),
		  Pattern.compile( "Stench Spell Damage: " + EXPR )
		},
		{ "Critical",
		  Pattern.compile( "(\\d+)x chance of Critical Hit" ),
		  Pattern.compile( "Critical: " + EXPR )
		},
		{ "Fumble",
		  Pattern.compile( "(\\d+)x chance of Fumble" ),
		  Pattern.compile( "Fumble: " + EXPR )
		},
		{ "HP Regen Min",
		  null,
		  Pattern.compile( "HP Regen Min: " + EXPR )
		},
		{ "HP Regen Max",
		  null,
		  Pattern.compile( "HP Regen Max: " + EXPR )
		},
		{ "MP Regen Min",
		  null,
		  Pattern.compile( "MP Regen Min: " + EXPR )
		},
		{ "MP Regen Max",
		  null,
		  Pattern.compile( "MP Regen Max: " + EXPR )
		},
		{ "Adventures",
		  Pattern.compile( "([+-]\\d+) Adventure\\(s\\) per day when equipped" ),
		  Pattern.compile( "Adventures: " + EXPR )
		},
		{ "Familiar Weight Percent",
		  Pattern.compile( "([+-]\\d+)% Familiar Weight" ),
		  Pattern.compile( "Familiar Weight Percent: " + EXPR )
		},
		{ "Weapon Damage Percent",
		  Pattern.compile( "Weapon Damage ([+-]\\d+)%" ),
		  Pattern.compile( "Weapon Damage Percent: " + EXPR )
		},
		{ "Ranged Damage Percent",
		  Pattern.compile( "Ranged Damage ([+-]\\d+)%" ),
		  Pattern.compile( "Ranged Damage Percent: " + EXPR )
		},
		{ "Stackable Mana Cost",
		  Pattern.compile( "([+-]\\d+) MP to use Skills" ),
		  Pattern.compile( "Mana Cost \\(stackable\\): " + EXPR )
		},
		{ "Hobo Power",
		  Pattern.compile( "([+-]\\d+) Hobo Power" ),
		  Pattern.compile( "Hobo Power: " + EXPR )
		},
		{ "Base Resting HP",
		  null,
		  Pattern.compile( "Base Resting HP: " + EXPR )
		},
		{ "Resting HP Percent",
		  null,
		  Pattern.compile( "Resting HP Percent: " + EXPR )
		},
		{ "Bonus Resting HP",
		  null,
		  Pattern.compile( "Bonus Resting HP: " + EXPR )
		},
		{ "Base Resting MP",
		  null,
		  Pattern.compile( "Base Resting MP: " + EXPR )
		},
		{ "Resting MP Percent",
		  null,
		  Pattern.compile( "Resting MP Percent: " + EXPR )
		},
		{ "Bonus Resting MP",
		  null,
		  Pattern.compile( "Bonus Resting MP: " + EXPR )
		},
		{ "Critical Hit Percent",
		  Pattern.compile( "([+-]\\d+)% chance of Critical Hit" ),
		  Pattern.compile( "Critical Hit Percent: " + EXPR )
		},
		{ "PvP Fights",
		  Pattern.compile( "([+-]\\d+) PvP fight\\(s\\) per day when equipped" ),
		  Pattern.compile( "PvP Fights: " + EXPR )
		},
		{ "Volleyball",
		  null,
		  Pattern.compile( "Volley(?:ball)?: " + EXPR )
		},
		{ "Sombrero",
		  null,
		  Pattern.compile( "Somb(?:rero)?: " + EXPR )
		},
		{ "Leprechaun",
		  null,
		  Pattern.compile( "Lep(?:rechaun)?: " + EXPR )
		},
		{ "Fairy",
		  null,
		  Pattern.compile( "Fairy: " + EXPR )
		},
		{ "Meat Drop Penalty",
		  null,
		  Pattern.compile( "Meat Drop Penalty: " + EXPR )
		},
		{ "Hidden Familiar Weight",
		  null,
		  Pattern.compile( "Familiar Weight \\(hidden\\): " + EXPR )
		},
		{ "Item Drop Penalty",
		  null,
		  Pattern.compile( "Item Drop Penalty: " + EXPR )
		},
		{ "Initiative Penalty",
		  null,
		  Pattern.compile( "Initiative Penalty: " + EXPR )
		},
		{ "Food Drop",
		  Pattern.compile( "([+-]\\d+)% Food Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Food Drop: " + EXPR )
		},
		{ "Booze Drop",
		  Pattern.compile( "([+-]\\d+)% Booze Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Booze Drop: " + EXPR )
		},
		{ "Hat Drop",
		  Pattern.compile( "([+-]\\d+)% Hat(?:/Pants)? Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Hat Drop: " + EXPR )
		},
		{ "Weapon Drop",
		  Pattern.compile( "([+-]\\d+)% Weapon Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Weapon Drop: " + EXPR )
		},
		{ "Offhand Drop",
		  Pattern.compile( "([+-]\\d+)% Off-[Hh]and Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Offhand Drop: " + EXPR )
		},
		{ "Shirt Drop",
		  Pattern.compile( "([+-]\\d+)% Shirt Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Shirt Drop: " + EXPR )
		},
		{ "Pants Drop",
		  Pattern.compile( "([+-]\\d+)% (?:Hat/)?Pants Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Pants Drop: " + EXPR )
		},
		{ "Accessory Drop",
		  Pattern.compile( "([+-]\\d+)% Accessory Drops? [Ff]rom Monsters$" ),
		  Pattern.compile( "Accessory Drop: " + EXPR )
		},
		{ "Volleyball Effectiveness",
		  null,
		  Pattern.compile( "Volleyball Effectiveness: " + EXPR )
		},
		{ "Sombrero Effectiveness",
		  null,
		  Pattern.compile( "Sombrero Effectiveness: " + EXPR )
		},
		{ "Leprechaun Effectiveness",
		  null,
		  Pattern.compile( "Leprechaun Effectiveness: " + EXPR )
		},
		{ "Fairy Effectiveness",
		  null,
		  Pattern.compile( "Fairy Effectiveness: " + EXPR )
		},
		{ "Familiar Weight Cap",
		  null,
		  Pattern.compile( "Familiar Weight Cap: " + EXPR )
		},
		{ "Slime Resistance",
		  null,
		  Pattern.compile( "Slime Resistance: " + EXPR )
		},
		{ "Slime Hates It",
		  Pattern.compile( "Slime( Really)? Hates (It|You)" ),
		  Pattern.compile( "Slime Hates It: " + EXPR )
		},
	};

	public static final int FLOAT_MODIFIERS = Modifiers.floatModifiers.length;

	public static final int SOFTCORE = 0;
	public static final int SINGLE = 1;
	public static final int NEVER_FUMBLE = 2;
	public static final int WEAKENS = 3;
	public static final int FREE_PULL = 4;
	public static final int VARIABLE = 5;
	public static final int NONSTACKABLE_WATCH = 6;

	private static final Object[][] booleanModifiers =
	{
		{ "Softcore Only",
		  Pattern.compile( "This item cannot be equipped while in Hardcore" ),
		  Pattern.compile( "Softcore Only" )
		},
		{ "Single Equip",
		  null,
		  Pattern.compile( "Single Equip" )
		},
		{ "Never Fumble",
		  Pattern.compile( "Never Fumble" ),
		  Pattern.compile( "Never Fumble" )
		},
		{ "Weakens Monster",
		  Pattern.compile( "Successful hit weakens opponent" ),
		  Pattern.compile( "Weakens Monster" )
		},
		{ "Free Pull",
		  null,
		  Pattern.compile( "Free Pull" )
		},
		{ "Variable",
		  null,
		  null
		},
		{ "Nonstackable Watch",
		  null,
		  Pattern.compile( "Nonstackable Watch" )
		},
	};

	public static final int BOOLEAN_MODIFIERS = Modifiers.booleanModifiers.length;

	public static final int CLASS = 0;
	public static final int INTRINSIC_EFFECT = 1;
	public static final int EQUALIZE = 2;
	public static final int WIKI_NAME = 3;
	public static final int MODIFIERS = 4;

	private static final Object[][] stringModifiers =
	{
		{ "Class",
		  null,
		  Pattern.compile( "Class: \"(.*?)\"" )
		},
		{ "Intrinsic Effect",
		  Pattern.compile( "Intrinsic effect: (.*)" ),
		  Pattern.compile( "Intrinsic Effect: \"(.*?)\"" )
		},
		{ "Equalize",
		  null,
		  Pattern.compile( "Equalize: \"(.*?)\"" )
		},
		{ "Wiki Name",
		  null,
		  Pattern.compile( "Wiki Name: \"(.*?)\"" )
		},
		{ "Modifiers",
		  null,
		  Pattern.compile( "^(none)$" )
		},
	};

	public static final int STRING_MODIFIERS = Modifiers.stringModifiers.length;
	
	// Clownosity behaves differently from any other current modifiers -
	// multiples of the same item do not contribute any more towards it,
	// even if their other attributes do stack.  Treat it as a special case
	// for now, rather than creating a 4th class of modifier types.
	// Currently, there are 19 distinct points of Clownosity available, so
	// bits in an int will work to represent them.
	
	private static final Pattern CLOWNOSITY_PATTERN =
		Pattern.compile( "Clownosity: ([+-]\\d+)" );
	private static int clownosityMask = 1;
	
	public static final Iterator getAllModifiers()
	{
		return Modifiers.modifiersByName.keySet().iterator();
	}
	
	public static final void overrideModifier( String name, String value )
	{
		name = StringUtilities.getCanonicalName( name );
		Modifiers.modifiersByName.put( name, value );
	}

	public static final String getModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.floatModifiers, index );
	}

	public static final String getBooleanModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.booleanModifiers, index );
	}

	public static final String getStringModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.stringModifiers, index );
	}

	private static final String modifierName( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return (String) table[ index ][ 0 ];
	};

	private static final Object modifierDescPattern( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return table[ index ][ 1 ];
	};

	private static final Pattern modifierTagPattern( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return (Pattern) table[ index ][ 2 ];
	};

	private static final String COLD =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.COLD_RESISTANCE ) + ": ";
	private static final String HOT =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.HOT_RESISTANCE ) + ": ";
	private static final String SLEAZE =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.SLEAZE_RESISTANCE ) + ": ";
	private static final String SPOOKY =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.SPOOKY_RESISTANCE ) + ": ";
	private static final String STENCH =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.STENCH_RESISTANCE ) + ": ";

	private static final String MOXIE = Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MOX ) + ": ";
	private static final String MUSCLE = Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MUS ) + ": ";
	private static final String MYSTICALITY = Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MYS ) + ": ";

	private static final String MOXIE_PCT =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MOX_PCT ) + ": ";
	private static final String MUSCLE_PCT =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MUS_PCT ) + ": ";
	private static final String MYSTICALITY_PCT =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MYS_PCT ) + ": ";

	private static final String HP_TAG = Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.HP ) + ": ";
	private static final String MP_TAG = Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MP ) + ": ";

	private static final String HP_REGEN_MIN_TAG =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.HP_REGEN_MIN ) + ": ";
	private static final String HP_REGEN_MAX_TAG =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.HP_REGEN_MAX ) + ": ";
	private static final String MP_REGEN_MIN_TAG =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MP_REGEN_MIN ) + ": ";
	private static final String MP_REGEN_MAX_TAG =
		Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.MP_REGEN_MAX ) + ": ";

	public static int elementalResistance( final int element )
	{
		switch ( element )
		{
		case MonsterDatabase.COLD:
			return Modifiers.COLD_RESISTANCE;
		case MonsterDatabase.HEAT:
			return Modifiers.HOT_RESISTANCE;
		case MonsterDatabase.SLEAZE:
			return Modifiers.SLEAZE_RESISTANCE;
		case MonsterDatabase.SPOOKY:
			return Modifiers.SPOOKY_RESISTANCE;
		case MonsterDatabase.STENCH:
			return Modifiers.STENCH_RESISTANCE;
		}
		return -1;
	}

	public static ArrayList getPotentialChanges( final int index )
	{
		ArrayList available = new ArrayList();

		Modifiers currentTest;
		Object[] check = Modifiers.modifiersByName.keySet().toArray();

		boolean hasEffect;
		AdventureResult currentEffect;

		for ( int i = 0; i < check.length; ++i )
		{
			if ( !EffectDatabase.contains( (String) check[ i ] ) )
			{
				continue;
			}

			currentTest = Modifiers.getModifiers( (String) check[ i ] );
			float value = ( (Modifiers) currentTest ).get( index );

			if ( value == 0.0f )
			{
				continue;
			}

			currentEffect = new AdventureResult( (String) check[ i ], 1, true );
			hasEffect = KoLConstants.activeEffects.contains( currentEffect );

			if ( value > 0.0f && !hasEffect )
			{
				available.add( currentEffect );
			}
			else if ( value < 0.0f && hasEffect )
			{
				available.add( currentEffect );
			}
		}

		return available;
	}

	private static final int findName( final Object[][] table, final String name )
	{
		for ( int i = 0; i < table.length; ++i )
		{
			if ( name.equalsIgnoreCase( (String) table[ i ][ 0 ] ) )
			{
				return i;
			}
		}
		return -1;
	};

	private String name;
	private boolean variable;
	private final float[] floats;
	private final boolean[] booleans;
	private final String[] strings;
	private Expression[] expressions;
	private int clownosity;

	public Modifiers()
	{
		this.variable = false;
		this.floats = new float[ Modifiers.FLOAT_MODIFIERS ];
		this.booleans = new boolean[ Modifiers.BOOLEAN_MODIFIERS ];
		this.strings = new String[ Modifiers.STRING_MODIFIERS ];
		this.reset();
	};

	public void reset()
	{
		for ( int i = 0; i < this.floats.length; ++i )
		{
			this.floats[ i ] = 0.0f;
		}
		for ( int i = 0; i < this.booleans.length; ++i )
		{
			this.booleans[ i ] = false;
		}
		for ( int i = 0; i < this.strings.length; ++i )
		{
			this.strings[ i ] = "";
		}
		this.expressions = null;
		this.clownosity = 0;
	};

	public float get( final int index )
	{
		if ( index < 0 || index >= this.floats.length )
		{
			return 0.0f;
		}

		return this.floats[ index ];
	};

	public float get( final String name )
	{
		int index = Modifiers.findName( Modifiers.floatModifiers, name );
		if ( index < 0 || index >= this.floats.length )
		{
			return 0.0f;
		}

		return this.floats[ index ];
	};

	public boolean getBoolean( final int index )
	{
		if ( index < 0 || index >= this.booleans.length )
		{
			return false;
		}

		return this.booleans[ index ];
	};

	public boolean getBoolean( final String name )
	{
		int index = Modifiers.findName( Modifiers.booleanModifiers, name );
		if ( index < 0 || index >= this.booleans.length )
		{
			return false;
		}

		return this.booleans[ index ];
	};

	public String getString( final int index )
	{
		if ( index < 0 || index >= this.strings.length )
		{
			return "";
		}

		return this.strings[ index ];
	};

	public String getString( final String name )
	{
		int index = Modifiers.findName( Modifiers.stringModifiers, name );
		if ( index < 0 || index >= this.strings.length )
		{
			return "";
		}

		return this.strings[ index ];
	};
	
	public int getClownosity()
	{
		int n = this.clownosity;
		// Count the bits:
		n = ((n & 0xAAAAAAAA) >>>  1) + (n & 0x55555555);
		n = ((n & 0xCCCCCCCC) >>>  2) + (n & 0x33333333);
		n = ((n & 0xF0F0F0F0) >>>  4) + (n & 0x0F0F0F0F);
		n = ((n & 0xFF00FF00) >>>  8) + (n & 0x00FF00FF);
		n = ((n & 0xFFFF0000) >>> 16) + (n & 0x0000FFFF);
		return n;
	}

	public boolean set( final int index, final double mod )
	{
		if ( index < 0 || index >= this.floats.length )
		{
			return false;
		}

		if ( this.floats[ index ] != mod )
		{
			this.floats[ index ] = (float) mod;
			return true;
		}
		return false;
	};

	public boolean set( final int index, final boolean mod )
	{
		if ( index < 0 || index >= this.booleans.length )
		{
			return false;
		}

		if ( this.booleans[ index ] != mod )
		{
			this.booleans[ index ] = mod;
			return true;
		}
		return false;
	};

	public boolean set( final int index, String mod )
	{
		if ( index < 0 || index >= this.strings.length )
		{
			return false;
		}

		if ( mod == null )
		{
			mod = "";
		}

		if ( !mod.equals( this.strings[ index ] ) )
		{
			this.strings[ index ] = mod;
			return true;
		}
		return false;
	};

	public boolean set( final Modifiers mods )
	{
		if ( mods == null )
		{
			return false;
		}

		boolean changed = false;

		float[] copyFloats = mods.floats;
		for ( int index = 0; index < this.floats.length; ++index )
		{
			if ( this.floats[ index ] != copyFloats[ index ] )
			{
				this.floats[ index ] = copyFloats[ index ];
				changed = true;
			}
		}

		boolean[] copyBooleans = mods.booleans;
		for ( int index = 0; index < this.booleans.length; ++index )
		{
			if ( this.booleans[ index ] != copyBooleans[ index ] )
			{
				this.booleans[ index ] = copyBooleans[ index ];
				changed = true;
			}
		}

		String[] copyStrings = mods.strings;
		for ( int index = 0; index < this.strings.length; ++index )
		{
			if ( !this.strings[ index ].equals( copyStrings[ index ] ) )
			{
				this.strings[ index ] = copyStrings[ index ];
				changed = true;
			}
		}
		
		if ( this.clownosity != mods.clownosity )
		{
			this.clownosity = mods.clownosity;
			changed = true;
		}

		return changed;
	}

	public void add( final int index, final double mod, final String desc )
	{
		if ( index < 0 || index >= this.floats.length )
		{
			return;
		}

		switch ( index )
		{
		case CRITICAL:
			// Critical hit modifier is maximum, not additive
			if ( mod > this.floats[ index ] )
			{
				this.floats[ index ] = (float) mod;
			}
			break;
		case MANA_COST:
			// Total Mana Cost reduction cannot exceed 3
			this.floats[ index ] += mod;
			if ( this.floats[ index ] < -3 )
			{
				this.floats[ index ] = -3;
			}
			break;
		default:
			this.floats[ index ] += mod;
			break;
		}
	};

	public void add( final Modifiers mods )
	{
		if ( mods == null )
		{
			return;
		}

		// Make sure the modifiers apply to current class
		String type = mods.getString( Modifiers.CLASS );
		if ( !type.equals( "" ) && !type.equals( KoLCharacter.getClassType() ) )
		{
			return;
		}
		
		String name = mods.name;

		// Add in the float modifiers

		float[] addition = mods.floats;

		for ( int i = 0; i < this.floats.length; ++i )
		{
			if ( addition[ i ] != 0.0f )
			{
				if ( i == Modifiers.ADVENTURES &&
					mods.booleans[ Modifiers.NONSTACKABLE_WATCH ] &&
					this.booleans[ Modifiers.NONSTACKABLE_WATCH ] )
				{
					continue;
				}
				this.add( i, addition[ i ], name );
			}
		}

		// OR in certain boolean modifiers

		this.booleans[ Modifiers.NEVER_FUMBLE ] |= mods.booleans[ Modifiers.NEVER_FUMBLE ];
		this.booleans[ Modifiers.WEAKENS ] |= mods.booleans[ Modifiers.WEAKENS ];
		this.booleans[ Modifiers.VARIABLE ] |= mods.booleans[ Modifiers.VARIABLE ];
		this.booleans[ Modifiers.NONSTACKABLE_WATCH ] |= mods.booleans[ Modifiers.NONSTACKABLE_WATCH ];
		
		this.clownosity |= mods.clownosity;
	}

	public static final Modifiers getModifiers( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return null;
		}

		name = StringUtilities.getCanonicalName( name );
		Object modifier = Modifiers.modifiersByName.get( name );

		if ( modifier == null )
		{
			return null;
		}

		if ( modifier instanceof Modifiers )
		{
			Modifiers mods = (Modifiers) modifier;
			if ( mods.variable )
			{
				mods.override( name );
			}
			return mods;
		}

		if ( !( modifier instanceof String ) )
		{
			return null;
		}

		String string = (String) modifier;

		Modifiers newMods = new Modifiers();
		newMods.name = name;
		float[] newFloats = newMods.floats;
		boolean[] newBooleans = newMods.booleans;
		String[] newStrings = newMods.strings;

		for ( int i = 0; i < newFloats.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.floatModifiers, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( string );
			if ( !matcher.find() )
			{
				continue;
			}

			if ( matcher.group( 1 ) != null )
			{
				newFloats[ i ] = Float.parseFloat( matcher.group( 1 ) );
			}
			else
			{
				if ( newMods.expressions == null )
				{
					newMods.expressions = new Expression[ Modifiers.FLOAT_MODIFIERS ];
				}
				newMods.expressions[ i ] = new Expression( matcher.group( 2 ),
									   name );
			}
		}

		for ( int i = 0; i < newBooleans.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.booleanModifiers, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( string );
			if ( !matcher.find() )
			{
				continue;
			}

			newBooleans[ i ] = true;
		}

		for ( int i = 0; i < newStrings.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.stringModifiers, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( string );
			if ( !matcher.find() )
			{
				continue;
			}

			newStrings[ i ] = matcher.group( 1 );
		}

		newStrings[ Modifiers.MODIFIERS ] = string;
		
		Matcher matcher = CLOWNOSITY_PATTERN.matcher( string );
		if ( matcher.find() )
		{
			switch ( StringUtilities.parseInt( matcher.group( 1 ) ) )
			{	// Assign each item with Clownosity its own bit or two in the value
			case 1:
				newMods.clownosity = clownosityMask;
				clownosityMask <<= 1;
				break;
				
			case 2:
				newMods.clownosity = clownosityMask | (clownosityMask << 1);
				clownosityMask <<= 2;
				break;
			
			default:
				// invalid Clownosity, just ignore it for now
			}
		}

		newMods.variable = newMods.override( name );
		newMods.booleans[ VARIABLE ] = newMods.variable || name.startsWith( "loc:" ) ||
			name.startsWith( "zone:" );
		Modifiers.modifiersByName.put( name, newMods );

		return newMods;
	};

	// Items that modify based on day of week
	private static final int TUESDAYS_RUBY = 2604;

	private boolean override( final String name )
	{
		if ( this.expressions != null )
		{
			for ( int i = 0; i < this.expressions.length; ++i )
			{
				Expression expr = this.expressions[ i ];
				if ( expr != null )
				{
					this.floats[ i ] = expr.eval();
				}
			}
			return true;
		}

		int itemId = ItemDatabase.getItemId( name );

		switch ( itemId )
		{
		case TUESDAYS_RUBY:
			// Reset to defaults

			this.set( Modifiers.MEATDROP, 0 );
			this.set( Modifiers.ITEMDROP, 0 );
			this.set( Modifiers.MOX_PCT, 0 );
			this.set( Modifiers.MUS_PCT, 0 );
			this.set( Modifiers.MYS_PCT, 0 );
			this.set( Modifiers.HP_REGEN_MIN, 0 );
			this.set( Modifiers.HP_REGEN_MAX, 0 );
			this.set( Modifiers.MP_REGEN_MIN, 0 );
			this.set( Modifiers.MP_REGEN_MAX, 0 );

			// Set modifiers depending on what day of the week it
			// is at the KoL servers

			Calendar date = Calendar.getInstance( TimeZone.getTimeZone( "GMT-7" ) );
			switch ( date.get( Calendar.DAY_OF_WEEK ) )
			{
			case Calendar.SUNDAY:
				// +5% Meat from Monsters
				this.set( Modifiers.MEATDROP, 5 );
				break;
			case Calendar.MONDAY:
				// Muscle +5%
				this.set( Modifiers.MUS_PCT, 5 );
				break;
			case Calendar.TUESDAY:
				// Regenerate 3-7 MP per adventure
				this.set( Modifiers.MP_REGEN_MIN, 3 );
				this.set( Modifiers.MP_REGEN_MAX, 7 );
				break;
			case Calendar.WEDNESDAY:
				// +5% Mysticality
				this.set( Modifiers.MYS_PCT, 5 );
				break;
			case Calendar.THURSDAY:
				// +5% Item Drops from Monsters
				this.set( Modifiers.ITEMDROP, 5 );
				break;
			case Calendar.FRIDAY:
				// +5% Moxie
				this.set( Modifiers.MOX_PCT, 5 );
				break;
			case Calendar.SATURDAY:
				// Regenerate 3-7 HP per adventure
				this.set( Modifiers.HP_REGEN_MIN, 3 );
				this.set( Modifiers.HP_REGEN_MAX, 7 );
				break;
			}
			return true;
		}

		return false;
	};

	public static final float getNumericModifier( final String name, final String mod )
	{
		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			return 0.0f;
		}
		return mods.get( mod );
	}

	public static final float getNumericModifier( final FamiliarData fam, final String mod, final int passedWeight, final AdventureResult item ) {
		int familiarId = fam != null ? fam.getId() : -1;
		if ( familiarId == -1 ) { return 0.0f; }
		Modifiers tempMods = new Modifiers();
		tempMods.setFamiliar( fam );
		if ( familiarId != 82 )
		{ // Mad Hatrack ... hats do not give their normal modifiers (should i be checking the item is a hat?)
			tempMods.add( Modifiers.getModifiers( item.getName() ) );
		}
		int weight = passedWeight + (int) tempMods.get( Modifiers.FAMILIAR_WEIGHT ) + (int) tempMods.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
		float percent = tempMods.get( Modifiers.FAMILIAR_WEIGHT_PCT ) / 100.0f;
		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * percent );
		}
		tempMods.lookupFamiliarModifiers( fam, weight, item );
		return tempMods.get( mod );
	}

	public static final boolean getBooleanModifier( final String name, final String mod )
	{
		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			return false;
		}
		return mods.getBoolean( mod );
	}

	public static final String getStringModifier( final String name, final String mod )
	{
		Modifiers mods = Modifiers.getModifiers( name );
		if ( mods == null )
		{
			return "";
		}
		return mods.getString( mod );
	}

	public void applyPassiveModifiers()
	{
		// You'd think this could be done at class initialization time,
		// but no: the SkillDatabase depends on the Mana Cost
		// modifier being set.

		if ( Modifiers.passiveSkills.isEmpty() )
		{
			Object[] keys = Modifiers.modifiersByName.keySet().toArray();
			for ( int i = 0; i < keys.length; ++i )
			{
				String skill = (String) keys[ i ];
				if ( !SkillDatabase.contains( skill ) )
				{
					continue;
				}

				if ( SkillDatabase.getSkillType( SkillDatabase.getSkillId( skill ) ) == SkillDatabase.PASSIVE )
				{
					Modifiers.passiveSkills.add( skill );
				}
			}
		}

		for ( int i = 0; i < Modifiers.passiveSkills.size(); ++i )
		{
			String skill = (String) Modifiers.passiveSkills.get( i );
			if ( KoLCharacter.hasSkill( skill ) )
			{
				this.add( Modifiers.getModifiers( skill ) );
			}
		}

		if ( KoLCharacter.getFamiliar().getId() == FamiliarPool.DODECAPEDE && KoLCharacter.hasAmphibianSympathy() )
		{
			this.add( Modifiers.FAMILIAR_WEIGHT, -10, "dodecapede sympathy" );
		}
	}

	public void applyFamiliarModifiers( final FamiliarData familiar )
	{
		int weight = familiar.getWeight() + (int) this.get( Modifiers.FAMILIAR_WEIGHT ) +
			(int) this.get( Modifiers.HIDDEN_FAMILIAR_WEIGHT );
		float percent = this.get( Modifiers.FAMILIAR_WEIGHT_PCT ) / 100.0f;
		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * percent );
		}

		weight = Math.max( 1, weight );
		AdventureResult famItem = EquipmentManager.getEquipment( EquipmentManager.FAMILIAR );
		this.lookupFamiliarModifiers( familiar, weight, famItem );
	}

	public void lookupFamiliarModifiers( final FamiliarData familiar, int weight, final AdventureResult famItem ) {
		int familiarId = familiar.getId();
		weight = Math.max( 1, weight );
		Modifiers.currentWeight = weight;
		
		this.add( Modifiers.getModifiers( "fam:" + familiar.getRace() ) );
		if ( famItem != null )
		{
			this.add( Modifiers.getModifiers( "fameq:" + famItem.getName() ) );
		}

		int cap = (int)this.get( Modifiers.FAMILIAR_WEIGHT_CAP );
		int cappedWeight = ( cap == 0 ) ? weight : Math.min( weight, cap );

		double effective = cappedWeight * this.get( Modifiers.VOLLEYBALL_WEIGHT );
		if ( effective == 0.0 && FamiliarDatabase.isVolleyType( familiarId ) )
		{
			effective = weight;
		}
		if ( effective != 0.0 )
		{
			double factor = this.get( Modifiers.VOLLEYBALL_EFFECTIVENESS );
			if ( factor == 0.0 ) factor = 1.0;
			this.add( Modifiers.EXPERIENCE, factor * Math.sqrt( effective ), "Volleyball" );
		}

		effective = cappedWeight * this.get( Modifiers.SOMBRERO_WEIGHT );
		if ( effective == 0.0 && FamiliarDatabase.isSombreroType( familiarId ) )
		{
			effective = weight;
		}
		if ( effective != 0.0 )
		{	// NIY
			double factor = this.get( Modifiers.SOMBRERO_EFFECTIVENESS );
			if ( factor == 0.0 ) factor = 1.0;
			this.add( Modifiers.EXPERIENCE, factor * 0.0, "Sombrero" );
		}

		effective = cappedWeight * this.get( Modifiers.LEPRECHAUN_WEIGHT );
		if ( effective == 0.0 && FamiliarDatabase.isMeatDropType( familiarId ) )
		{
			effective = weight;
		}
		if ( effective != 0.0 )
		{
			double factor = this.get( Modifiers.LEPRECHAUN_EFFECTIVENESS );
			if ( factor == 0.0 ) factor = 1.0;
			this.add( Modifiers.MEATDROP, factor * (Math.sqrt( 220 * effective ) + 2 * effective - 6),
				"Leprechaun" );
		}

		effective = cappedWeight * this.get( Modifiers.FAIRY_WEIGHT );
		if ( effective == 0.0 && FamiliarDatabase.isFairyType( familiarId ) )
		{
			effective = weight;
		}
		if ( effective != 0.0 )
		{
			double factor = this.get( Modifiers.FAIRY_EFFECTIVENESS );
			if ( factor == 0.0 ) factor = 1.0;
			this.add( Modifiers.ITEMDROP, factor * (Math.sqrt( 55 * effective ) + effective - 3),
				"Fairy" );
		}

		switch ( familiarId )
		{
		case FamiliarPool.HATRACK:
			if ( famItem == EquipmentRequest.UNEQUIP )
			{
				this.add( Modifiers.HATDROP, 50.0, "naked hatrack" );
			}
			break;
		}
	}
	
	public static final String getFamiliarEffect( final String itemName )
	{
		return (String) Modifiers.familiarEffectByName.get( 
			StringUtilities.getCanonicalName( itemName ) );
	}


	// Parsing item enchantments into KoLmafia modifiers

	private static final Pattern DR_PATTERN = Pattern.compile( "Damage Reduction: (<b>)?([+-]?\\d+)(</b>)?" );

	public static final String parseDamageReduction( final String text )
	{
		Matcher matcher = Modifiers.DR_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.DAMAGE_REDUCTION ) + ": " + matcher.group( 2 );
		}

		return null;
	}

	private static final Pattern SINGLE_PATTERN =
		Pattern.compile( "You may not equip more than one of this item at a time" );

	public static final String parseSingleEquip( final String text )
	{
		Matcher matcher = Modifiers.SINGLE_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierName( Modifiers.booleanModifiers, Modifiers.SINGLE );
		}

		return null;
	}

	private static final Pattern SOFTCORE_PATTERN =
		Pattern.compile( "This item cannot be equipped while in Hardcore" );

	public static final String parseSoftcoreOnly( final String text )
	{
		Matcher matcher = Modifiers.SOFTCORE_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierName( Modifiers.booleanModifiers, Modifiers.SOFTCORE );
		}

		return null;
	}

	private static final Pattern FREE_PULL_PATTERN =
		Pattern.compile( "Free pull from Hagnk's" );

	public static final String parseFreePull( final String text )
	{
		Matcher matcher = Modifiers.FREE_PULL_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierName( Modifiers.booleanModifiers, Modifiers.FREE_PULL );
		}

		return null;
	}

	private static final Pattern ALL_ATTR_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)$" );
	private static final Pattern ALL_ATTR_PCT_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)%$" );
	private static final Pattern CLASS_PATTERN = Pattern.compile( "Bonus&nbsp;for&nbsp;(.*)&nbsp;only" );
	private static final Pattern COMBAT_PATTERN = Pattern.compile( "Monsters will be (.*) attracted to you" );
	private static final Pattern HP_MP_PATTERN = Pattern.compile( "^Maximum HP/MP ([+-]\\d+)$" );

	public static final String parseModifier( final String enchantment )
	{
		String result;

		// Search the float modifiers first

		result = Modifiers.parseModifier( Modifiers.floatModifiers, enchantment, false );
		if ( result != null )
		{
			return result;
		}

		// Then the boolean modifiers

		result = Modifiers.parseModifier( Modifiers.booleanModifiers, enchantment, false );
		if ( result != null )
		{
			return result;
		}

		// Then the string modifiers

		result = Modifiers.parseModifier( Modifiers.stringModifiers, enchantment, true );
		if ( result != null )
		{
			return result;
		}

		// Special handling needed

		Matcher matcher;

		matcher = Modifiers.ALL_ATTR_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group( 1 );
			return Modifiers.MUSCLE + mod + ", " + Modifiers.MYSTICALITY + mod + ", " + Modifiers.MOXIE + mod;
		}

		matcher = Modifiers.ALL_ATTR_PCT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group( 1 );
			return Modifiers.MUSCLE_PCT + mod + ", " + Modifiers.MYSTICALITY_PCT + mod + ", " + Modifiers.MOXIE_PCT + mod;
		}

		matcher = Modifiers.CLASS_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String plural = matcher.group( 1 );
			String cls = "none";
			if ( plural.equals( "Accordion&nbsp;Thieves" ) )
			{
				cls = KoLCharacter.ACCORDION_THIEF;
			}
			else if ( plural.equals( "Disco&nbsp;Bandits" ) )
			{
				cls = KoLCharacter.DISCO_BANDIT;
			}
			else if ( plural.equals( "Pastamancers" ) )
			{
				cls = KoLCharacter.PASTAMANCER;
			}
			else if ( plural.equals( "Saucerors" ) )
			{
				cls = KoLCharacter.SAUCEROR;
			}
			else if ( plural.equals( "Seal&nbsp;Clubbers" ) )
			{
				cls = KoLCharacter.SEAL_CLUBBER;
			}
			else if ( plural.equals( "Turtle&nbsp; Tamers" ) )
			{
				cls = KoLCharacter.TURTLE_TAMER;
			}
			return Modifiers.modifierName( Modifiers.stringModifiers, Modifiers.CLASS ) + ": \"" + cls + "\"";
		}

		matcher = Modifiers.COMBAT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			return Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.COMBAT_RATE ) + ": " + ( matcher.group( 1 ).equals( "more" ) ? "+5" : "-5" );
		}

		matcher = Modifiers.HP_MP_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group( 1 );
			return Modifiers.HP_TAG + mod + ", " + Modifiers.MP_TAG + mod;
		}

		if ( enchantment.indexOf( "Regenerate" ) != -1 )
		{
			return Modifiers.parseRegeneration( enchantment );
		}

		if ( enchantment.indexOf( "Resistance" ) != -1 )
		{
			return Modifiers.parseResistance( enchantment );
		}

		return null;
	}

	private static final String parseModifier( final Object[][] table, final String enchantment, final boolean quoted )
	{
		String quote = quoted ? "\"" : "";
		for ( int i = 0; i < table.length; ++i )
		{
			Object object = Modifiers.modifierDescPattern( table, i );
			if ( object == null )
			{
				continue;
			}

			Object [] patterns;

			if ( object instanceof Pattern )
			{
				patterns = new Pattern[1];
				patterns[0] = (Pattern) object;
			}
			else
			{
				patterns = (Object[]) object;
			}

			for ( int j = 0; j < patterns.length; ++j )
			{
				Pattern pattern = (Pattern) patterns[ j ];
				Matcher matcher = pattern.matcher( enchantment );
				if ( !matcher.find() )
				{
					continue;
				}

				if ( matcher.groupCount() == 0 )
				{
					return Modifiers.modifierName( table, i );
				}
				return Modifiers.modifierName( table, i ) + ": " + quote + matcher.group( 1 ) + quote;
			}
		}

		return null;
	}

	private static final Pattern REGEN_PATTERN =
		Pattern.compile( "Regenerate (\\d*)-?(\\d*)? ([HM]P)( and .*)? per [aA]dventure$" );

	private static final String parseRegeneration( final String enchantment )
	{
		Matcher matcher = Modifiers.REGEN_PATTERN.matcher( enchantment );
		if ( !matcher.find() )
		{
			return null;
		}

		String min = matcher.group( 1 );
		String max = matcher.group( 2 ) == null ? min : matcher.group( 2 );
		boolean hp = matcher.group( 3 ).equals( "HP" );
		boolean both = matcher.group( 4 ) != null;

		if ( max.equals( "" ) )
		{
			max = min;
		}

		if ( both )
		{
			return Modifiers.HP_REGEN_MIN_TAG + min + ", " + Modifiers.HP_REGEN_MAX_TAG + max + ", " + Modifiers.MP_REGEN_MIN_TAG + min + ", " + Modifiers.MP_REGEN_MAX_TAG + max;
		}

		if ( hp )
		{
			return Modifiers.HP_REGEN_MIN_TAG + min + ", " + Modifiers.HP_REGEN_MAX_TAG + max;
		}

		return Modifiers.MP_REGEN_MIN_TAG + min + ", " + Modifiers.MP_REGEN_MAX_TAG + max;
	}

	private static final String parseResistance( final String enchantment )
	{
		String level = "";

		if ( enchantment.indexOf( "Slight" ) != -1 )
		{
			level = "+1";
		}
		else if ( enchantment.indexOf( "So-So" ) != -1 )
		{
			level = "+2";
		}
		else if ( enchantment.indexOf( "Serious" ) != -1 )
		{
			level = "+3";
		}
		else if ( enchantment.indexOf( "Stupendous" ) != -1 )
		{
			level = "+4";
		}
		else if ( enchantment.indexOf( "Superhuman" ) != -1 )
		{
			level = "+5";
		}
		else if ( enchantment.indexOf( "Sublime" ) != -1 )
		{
			level = "+9";
		}

		if ( enchantment.indexOf( "All Elements" ) != -1 )
		{
			return Modifiers.COLD + level + ", " + Modifiers.HOT + level + ", " + Modifiers.SLEAZE + level + ", " + Modifiers.SPOOKY + level + ", " + Modifiers.STENCH + level;
		}

		if ( enchantment.indexOf( "Cold" ) != -1 )
		{
			return Modifiers.COLD + level;
		}

		if ( enchantment.indexOf( "Hot" ) != -1 )
		{
			return Modifiers.HOT + level;
		}

		if ( enchantment.indexOf( "Sleaze" ) != -1 )
		{
			return Modifiers.SLEAZE + level;
		}

		if ( enchantment.indexOf( "Spooky" ) != -1 )
		{
			return Modifiers.SPOOKY + level;
		}

		if ( enchantment.indexOf( "Stench" ) != -1 )
		{
			return Modifiers.STENCH + level;
		}

		return null;
	}

	private static final boolean findModifier( final Object[][] table, final String tag )
	{
		for ( int i = 0; i < table.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( table, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( tag );
			if ( matcher.find() )
			{
				return true;
			}
		}
		return false;
	}

	public static final void checkModifiers()
	{
		Object[] keys = Modifiers.modifiersByName.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String) keys[ i ];
			Object modifier = Modifiers.modifiersByName.get( name );

			if ( modifier == null )
			{
				RequestLogger.printLine( "Key \"" + name + "\" has no modifiers" );
				continue;
			}

			if ( modifier instanceof Modifiers )
			{
				modifier = ((Modifiers) modifier).getString( Modifiers.MODIFIERS );
			}

			if ( !( modifier instanceof String ) )
			{
				RequestLogger.printLine( "Key \"" + name + "\" has bogus modifiers of class " + modifier.getClass().toString() );
				continue;
			}

			// It's a string. Check all modifiers.
			// Familiar Effect has to be special-cased since its parameter contains commas.
			String[] strings = ( (String) modifier ).replaceFirst(
				"(, )?Familiar Effect: \"[^\"]+\"" , "" ).split( ", " );
			for ( int j = 0; j < strings.length; ++j )
			{
				String mod = strings[ j ].trim();
				if ( mod.equals( "" ) )
				{
					continue;
				}
				if ( Modifiers.findModifier( Modifiers.floatModifiers, mod ) )
				{
					continue;
				}
				if ( Modifiers.findModifier( Modifiers.booleanModifiers, mod ) )
				{
					continue;
				}
				if ( Modifiers.findModifier( Modifiers.stringModifiers, mod ) )
				{
					continue;
				}
				if ( mod.startsWith( "Clownosity:" ) )
				{
					continue;
				}
				if ( name.startsWith( "fameq:" ) )
				{
					continue;	// these may contain freeform text
				}
				RequestLogger.printLine( "Key \"" + name + "\" has unknown modifier: \"" + mod + "\"" );
			}
		}
	}
	
	private static class Expression
	{
		private float[] stack;	// also holds numeric literals
		private int sp = 0;
		private String bytecode;
		private AdventureResult effect;
		private String loc, zone, fam, pref;
		private String text;
		private static final Pattern NUM_PATTERN = Pattern.compile( "([+-]?[\\d.]+)(.*)" );
		
		public Expression( String text, String name )
		{
			if ( text.indexOf( "T" ) != -1 )
			{
				this.effect = new AdventureResult( name, 0, true );
			}
			this.stack = new float[10];
			this.text = text;
			this.bytecode = expr() + "r";
			if ( this.text.length() > 0 )
			{
				KoLmafia.updateDisplay( "Modifier syntax error: expected end, found "
					+ this.text );
			}
			this.text = null;
		}
		
		public float eval()
		{
			String bytecode = this.bytecode;
			float[] s = this.stack;
			int sp = this.sp;
			int pc = 0;
			float v = 0.0f;
			while ( true )
			{
				switch ( bytecode.charAt( pc++ ) )
				{
				case 'r':
					return s[ --sp ];
				
				case '^':
					v = (float) Math.pow( s[ --sp ], s[ --sp ] );
					break;
				
				case '*':
					v = s[ --sp ] * s[ --sp ];
					break;
				case '/':
					v = s[ --sp ] / s[ --sp ];
					break;
				
				case '+':
					v = s[ --sp ] + s[ --sp ];
					break;
				case '-':
					v = s[ --sp ] - s[ --sp ];
					break;
				
				case 'c':
					v = (float) Math.ceil( s[ --sp ] );
					break;
				case 'f':
					v = (float) Math.floor( s[ --sp ] );
					break;
				case 's':
					v = (float) Math.sqrt( s[ --sp ] );
					break;

				case 'l':
					v = Modifiers.currentLocation.indexOf( this.loc ) == -1 ? 0.0f : 1.0f;
					break;
				case 'z':
					v = Modifiers.currentZone.indexOf( this.zone ) == -1 ? 0.0f : 1.0f;
					break;
				case 'w':
					v = Modifiers.currentFamiliar.indexOf( this.fam ) == -1 ? 0.0f : 1.0f;
					break;
				case 'p':
					v = Preferences.getFloat( this.pref );
					break;
					
				case 'm':
					v = Math.min( s[ --sp ], s[ --sp ] );
					break;
				case 'x':
					v = Math.max( s[ --sp ], s[ --sp ] );
					break;
				
				case '0':
					v = s[ 0 ];
					break;
				case '1':
					v = s[ 1 ];
					break;
				case '2':
					v = s[ 2 ];
					break;
				case '3':
					v = s[ 3 ];
					break;
				case '4':
					v = s[ 4 ];
					break;
				
				case 'A':
					v = 0;
					break;
				case 'B':
					v = HolidayDatabase.getBloodEffect();
					break;
				case 'C':
					v = 0;
					break;
				case 'D':
					v = KoLCharacter.getInebriety();
					break;
				case 'E':
					v = 0;
					break;
				case 'F':
					v = KoLCharacter.getFullness();
					break;
				case 'G':
					v = HolidayDatabase.getGrimaciteEffect() / 10.0f;
					break;
				case 'H':
					v = KoLCharacter.getHoboPower();
					break;
				case 'I':
					v = 0;
					break;
				case 'J':
					v = HolidayDatabase.getHoliday().equals( "Festival of Jarlsberg" ) ?
						1.0f : 0.0f;
					break;
				case 'K':
					v = 0;
					break;
				case 'L':
					v = KoLCharacter.getLevel();
					break;
				case 'M':
					v = HolidayDatabase.getMoonlight();
					break;
				case 'N':
					v = 0;
					break;
				case 'O':
					v = 0;
					break;
				case 'P':
					v = 0;
					break;
				case 'Q':
					v = 0;
					break;
				case 'R':
					v = 0;
					break;
				case 'S':
					v = KoLCharacter.getSpleenUse();
					break;
				case 'T':
					v = Math.max( 1, this.effect.getCount( KoLConstants.activeEffects ) );
					break;
				case 'U':
					v = KoLCharacter.getTelescopeUpgrades();
					break;
				case 'V':
					v = 0;
					break;
				case 'W':
					v = Modifiers.currentWeight;
					break;
				case 'X':
					v = KoLCharacter.getGender();
					break;
				case 'Y':
					v = 0;
					break;
				case 'Z':
					v = 0;
					break;
				default:
					KoLmafia.updateDisplay( "Modifier bytecode invalid at " + pc + ": "
						+ this.bytecode );
				}
				s[ sp++ ] = v;
			}
		}
		
		private void expect( String token )
		{
			if ( this.text.startsWith( token ) )
			{
				this.text = this.text.substring( token.length() );
				return;
			}
			KoLmafia.updateDisplay( "Modifier syntax error: expected " + token +
				", found " + this.text );
		}
		
		private String until( String token )
		{
			int pos = this.text.indexOf( token );
			if ( pos == -1 )
			{
				KoLmafia.updateDisplay( "Modifier syntax error: expected " + token +
					", found " + this.text );
				return "";
			}
			String rv = this.text.substring( 0, pos );
			this.text = this.text.substring( pos + token.length() );
			return rv;
		}
		
		private boolean optional( String token )
		{
			if ( this.text.startsWith( token ) )
			{
				this.text = this.text.substring( token.length() );
				return true;
			}
			return false;
		}

		private char optional( String token1, String token2 )
		{
			if ( this.text.startsWith( token1 ) )
			{
				this.text = this.text.substring( token1.length() );
				return token1.charAt( 0 );
			}
			if ( this.text.startsWith( token2 ) )
			{
				this.text = this.text.substring( token2.length() );
				return token2.charAt( 0 );
			}
			return '\0';
		}
		
		private String expr()
		{
			String rv = this.term();
			while ( true ) switch ( this.optional( "+", "-" ) )
			{
			case '+':
				rv = this.term() + rv + "+";
				break;
			case '-':
				rv = this.term() + rv + "-";
				break;
			default:
				return rv;			
			}
		}
		
		private String term()
		{
			String rv = this.factor();
			while ( true ) switch ( this.optional( "*", "/" ) )
			{
			case '*':
				rv = this.factor() + rv + "*";
				break;
			case '/':
				rv = this.factor() + rv + "/";
				break;
			default:
				return rv;			
			}
		}
		
		private String factor()
		{
			String rv = this.value();
			while ( this.optional( "^" ) )
			{
				rv = this.value() + rv + "^";
			}
			return rv;
		}
		
		private String value()
		{
			String rv;
			if ( this.optional( "(" ) )
			{
				rv = this.expr();
				this.expect( ")" );
				return rv;
			}
			if ( this.optional( "ceil(" ) )
			{
				rv = this.expr();
				this.expect( ")" );
				return rv + "c";
			}
			if ( this.optional( "floor(" ) )
			{
				rv = this.expr();
				this.expect( ")" );
				return rv + "f";
			}
			if ( this.optional( "sqrt(" ) )
			{
				rv = this.expr();
				this.expect( ")" );
				return rv + "s";
			}
			if ( this.optional( "min(" ) )
			{
				rv = this.expr();
				this.expect( "," );
				rv = rv + this.expr() + "m";
				this.expect( ")" );
				return rv;
			}
			if ( this.optional( "max(" ) )
			{
				rv = this.expr();
				this.expect( "," );
				rv = rv + this.expr() + "x";
				this.expect( ")" );
				return rv;
			}
			if ( this.optional( "loc(" ) )
			{
				this.loc = this.until( ")" ).toLowerCase();
				return "l";
			}
			if ( this.optional( "zone(" ) )
			{
				this.zone = this.until( ")" ).toLowerCase();
				return "z";
			}
			if ( this.optional( "fam(" ) )
			{
				this.fam = this.until( ")" ).toLowerCase();
				return "w";
			}
			if ( this.optional( "pref(" ) )
			{
				this.pref = this.until( ")" );
				return "p";
			}
			if ( this.text.length() == 0 )
			{
				KoLmafia.updateDisplay( "Modifier syntax error: unexpected end of expr" );
				return "0";	
			}
			rv = this.text.substring( 0, 1 );
			if ( rv.charAt( 0 ) >= 'A' && rv.charAt( 0 ) <= 'Z' )
			{
				this.text = this.text.substring( 1 );
				return rv;
			}
			Matcher m = NUM_PATTERN.matcher( this.text );
			if ( m.matches() )
			{
				this.stack[ this.sp++ ] = Float.parseFloat( m.group( 1 ) );
				this.text = m.group( 2 );
				return String.valueOf( (char)( '0' + this.sp - 1 ) );
			}
			KoLmafia.updateDisplay( "Modifier syntax error: can't understand " + this.text );
			this.text = "";
			return "0";	
		}
	}
	
	public static void setLocation( KoLAdventure location )
	{
		Modifiers.currentLocation = location.getAdventureName().toLowerCase();
		Modifiers.currentZone = location.getZone().toLowerCase();
	}

	public static void setFamiliar( FamiliarData fam )
	{
		Modifiers.currentFamiliar = fam == null ? "" : fam.getRace().toLowerCase();
	}
}
