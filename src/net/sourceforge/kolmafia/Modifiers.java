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
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Modifiers
	extends KoLDatabase
{
	private static final Map modifiersByName = new TreeMap();
	private static final HashMap familiarEffectByName = new HashMap();
	private static final ArrayList passiveSkills = new ArrayList();

	private static final Pattern FAMILIAR_EFFECT_PATTERN =
		Pattern.compile( "Familiar Effect: \"(.*?)\"" );

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
			Modifiers.modifiersByName.put( name, data[ 1 ] );
			
			Matcher matcher = FAMILIAR_EFFECT_PATTERN.matcher( data[ 1 ] );
			if ( matcher.find() )
			{
				Modifiers.familiarEffectByName.put( name, matcher.group( 1 ) );
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
	public static final int MELEE_DAMAGE = 25;
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
	public static final int MELEE_DAMAGE_PCT = 47;
	public static final int RANGED_DAMAGE_PCT = 48;
	public static final int STACKABLE_MANA_COST = 49;
	public static final int HOBO_POWER = 50;
	public static final int BASE_RESTING_HP = 51;
	public static final int RESTING_HP_PCT = 52;
	public static final int BONUS_RESTING_HP = 53;
	public static final int BASE_RESTING_MP = 54;
	public static final int RESTING_MP_PCT = 55;
	public static final int BONUS_RESTING_MP = 56;

	private static final Object[][] floatModifiers =
	{
		{ "Familiar Weight",
		  Pattern.compile( "(.*) to Familiar Weight" ),
		  Pattern.compile( "Familiar Weight: ([+-]\\d+)" )
		},
		{ "Monster Level",
		  Pattern.compile( "(.*) to Monster Level" ),
		  Pattern.compile( "Monster Level: ([+-]\\d+)" )
		},
		{ "Combat Rate",
		  null,
		  Pattern.compile( "Combat Rate: ([+-][\\d.]+)" )
		},
		{ "Initiative",
		  Pattern.compile( "Combat Initiative (.*)%" ),
		  Pattern.compile( "Initiative: ([+-][\\d.]+)" )
		},
		{ "Experience",
		  Pattern.compile( "(.*) Stat.*Per Fight" ),
		  Pattern.compile( "Experience: ([+-][\\d.]+)" )
		},
		{ "Item Drop",
		  Pattern.compile( "(.*)% Item Drops? from Monsters" ),
		  Pattern.compile( "Item Drop: ([+-][\\d.]+)" )
		},
		{ "Meat Drop",
		  Pattern.compile( "(.*)% Meat from Monsters" ),
		  Pattern.compile( "Meat Drop: ([+-][\\d.]+)" )
		},
		{ "Damage Absorption",
		  Pattern.compile( "Damage Absorption (.*)" ),
		  Pattern.compile( "Damage Absorption: ([+-]\\d+)" )
		},
		{ "Damage Reduction",
		  Pattern.compile( "Damage Reduction: (\\d+)" ),
		  Pattern.compile( "Damage Reduction: ([+-]?\\d+)" )
		},
		{ "Cold Resistance",
		  null,
		  Pattern.compile( "Cold Resistance: ([+-]\\d+)" )
		},
		{ "Hot Resistance",
		  null,
		  Pattern.compile( "Hot Resistance: ([+-]\\d+)" )
		},
		{ "Sleaze Resistance",
		  null,
		  Pattern.compile( "Sleaze Resistance: ([+-]\\d+)" )
		},
		{ "Spooky Resistance",
		  null,
		  Pattern.compile( "Spooky Resistance: ([+-]\\d+)" )
		},
		{ "Stench Resistance",
		  null,
		  Pattern.compile( "Stench Resistance: ([+-]\\d+)" )
		},
		{ "Mana Cost",
		  Pattern.compile( "(.*) MP to use Skills" ),
		  Pattern.compile( "Mana Cost: ([+-]\\d+)" )
		},
		{ "Moxie",
		  Pattern.compile( "Moxie ([+-]\\d+)$" ),
		  Pattern.compile( "Moxie: ([+-]\\d+)" )
		},
		{ "Moxie Percent",
		  Pattern.compile( "Moxie ([+-]\\d+)%" ),
		  Pattern.compile( "Moxie Percent: ([+-]\\d+)" )
		},
		{ "Muscle",
		  Pattern.compile( "Muscle ([+-]\\d+)$" ),
		  Pattern.compile( "Muscle: ([+-]\\d+)" )
		},
		{ "Muscle Percent",
		  Pattern.compile( "Muscle ([+-]\\d+)%" ),
		  Pattern.compile( "Muscle Percent: ([+-]\\d+)" )
		},
		{ "Mysticality",
		  Pattern.compile( "Mysticality ([+-]\\d+)$" ),
		  Pattern.compile( "Mysticality: ([+-]\\d+)" )
		},
		{ "Mysticality Percent",
		  Pattern.compile( "Mysticality ([+-]\\d+)%" ),
		  Pattern.compile( "Mysticality Percent: ([+-]\\d+)" )
		},
		{ "Maximum HP",
		  Pattern.compile( "Maximum HP ([+-]\\d+)$" ),
		  Pattern.compile( "Maximum HP: ([+-]\\d+)" )
		},
		{ "Maximum HP Percent",
		  null,
		  Pattern.compile( "Maximum HP Percent: ([+-]\\d+)" )
		},
		{ "Maximum MP",
		  Pattern.compile( "Maximum MP ([+-]\\d+)$" ),
		  Pattern.compile( "Maximum MP: ([+-]\\d+)" )
		},
		{ "Maximum MP Percent",
		  null,
		  Pattern.compile( "Maximum MP Percent: ([+-]\\d+)" )
		},
		{ "Melee Damage",
		  Pattern.compile( "Melee Damage ([+-]\\d+)$" ),
		  Pattern.compile( "Melee Damage: ([+-]\\d+)" )
		},
		{ "Ranged Damage",
		  Pattern.compile( "Ranged Damage ([+-]\\d+)$" ),
		  Pattern.compile( "Ranged Damage: ([+-]\\d+)" )
		},
		{ "Spell Damage",
		  Pattern.compile( "Spell Damage ([+-]\\d+)$" ),
		  Pattern.compile( "Spell Damage: ([+-]\\d+)" )
		},
		{ "Spell Damage Percent",
		  Pattern.compile( "Spell Damage ([+-]\\d+)%" ),
		  Pattern.compile( "Spell Damage Percent: ([+-]\\d+)" )
		},
		{ "Cold Damage",
		  Pattern.compile( "([+-]\\d+) <font color=blue>Cold Damage</font>" ),
		  Pattern.compile( "Cold Damage: ([+-]\\d+)" )
		},
		{ "Hot Damage",
		  Pattern.compile( "([+-]\\d+) <font color=red>Hot Damage</font>" ),
		  Pattern.compile( "Hot Damage: ([+-]\\d+)" )
		},
		{ "Sleaze Damage",
		  Pattern.compile( "([+-]\\d+) <font color=blueviolet>Sleaze Damage</font>" ),
		  Pattern.compile( "Sleaze Damage: ([+-]\\d+)" )
		},
		{ "Spooky Damage",
		  Pattern.compile( "([+-]\\d+) <font color=gray>Spooky Damage</font>" ),
		  Pattern.compile( "Spooky Damage: ([+-]\\d+)" )
		},
		{ "Stench Damage",
		  Pattern.compile( "([+-]\\d+) <font color=green>Stench Damage</font>" ),
		  Pattern.compile( "Stench Damage: ([+-]\\d+)" )
		},
		{ "Cold Spell Damage",
		  Pattern.compile( "([+-]\\d+) Damage to <font color=blue>Cold Spells</font>" ),
		  Pattern.compile( "Cold Spell Damage: ([+-]\\d+)" )
		},
		{ "Hot Spell Damage",
		  Pattern.compile( "([+-]\\d+) Damage to <font color=red>Hot Spells</font>" ),
		  Pattern.compile( "Hot Spell Damage: ([+-]\\d+)" )
		},
		{ "Sleaze Spell Damage",
		  Pattern.compile( "([+-]\\d+) Damage to <font color=blueviolet>Sleaze Spells</font>" ),
		  Pattern.compile( "Sleaze Spell Damage: ([+-]\\d+)" )
		},
		{ "Spooky Spell Damage",
		  Pattern.compile( "([+-]\\d+) Damage to <font color=gray>Spooky Spells</font>" ),
		  Pattern.compile( "Spooky Spell Damage: ([+-]\\d+)" )
		},
		{ "Stench Spell Damage",
		  Pattern.compile( "([+-]\\d+) Damage to <font color=green>Stench Spells</font>" ),
		  Pattern.compile( "Stench Spell Damage: ([+-]\\d+)" )
		},
		{ "Critical",
		  Pattern.compile( "(\\d+)x chance of Critical Hit" ),
		  Pattern.compile( "Critical: (\\d+)" )
		},
		{ "Fumble",
		  Pattern.compile( "(\\d+)x chance of Fumble" ),
		  Pattern.compile( "Fumble: (\\d+)" )
		},
		{ "HP Regen Min",
		  null,
		  Pattern.compile( "HP Regen Min: (\\d+)" )
		},
		{ "HP Regen Max",
		  null,
		  Pattern.compile( "HP Regen Max: (\\d+)" )
		},
		{ "MP Regen Min",
		  null,
		  Pattern.compile( "MP Regen Min: (\\d+)" )
		},
		{ "MP Regen Max",
		  null,
		  Pattern.compile( "MP Regen Max: (\\d+)" )
		},
		{ "Adventures",
		  Pattern.compile( "([+-]\\d+) Adventure\\(s\\) per day when equipped" ),
		  Pattern.compile( "Adventures: ([+]\\d+)" )
		},
		{ "Familiar Weight Percent",
		  null,
		  Pattern.compile( "Familiar Weight Percent: ([+-]\\d+)" )
		},
		{ "Melee Damage Percent",
		  Pattern.compile( "Melee Damage ([+-]\\d+)%" ),
		  Pattern.compile( "Melee Damage Percent: ([+-]\\d+)" )
		},
		{ "Ranged Damage Percent",
		  Pattern.compile( "Ranged Damage ([+-]\\d+)%" ),
		  Pattern.compile( "Ranged Damage Percent: ([+-]\\d+)" )
		},
		{ "Stackable Mana Cost",
		  Pattern.compile( "(.*) MP to use Skills" ),
		  Pattern.compile( "Mana Cost \\(stackable\\): ([+-]\\d+)" )
		},
		{ "Hobo Power",
		  Pattern.compile( "(.*) Hobo Power" ),
		  Pattern.compile( "Hobo Power: ([+-]\\d+)" )
		},
		{ "Base Resting HP",
		  null,
		  Pattern.compile( "Base Resting HP: ([+-]\\d+)" )
		},
		{ "Resting HP Percent",
		  null,
		  Pattern.compile( "Resting HP Percent: ([+-]\\d+)" )
		},
		{ "Bonus Resting HP",
		  null,
		  Pattern.compile( "Bonus Resting HP: ([+-]\\d+)" )
		},
		{ "Base Resting MP",
		  null,
		  Pattern.compile( "Base Resting MP: ([+-]\\d+)" )
		},
		{ "Resting MP Percent",
		  null,
		  Pattern.compile( "Resting MP Percent: ([+-]\\d+)" )
		},
		{ "Bonus Resting MP",
		  null,
		  Pattern.compile( "Bonus Resting MP: ([+-]\\d+)" )
		},
	};

	public static final int FLOAT_MODIFIERS = Modifiers.floatModifiers.length;

	public static final int SOFTCORE = 0;
	public static final int SINGLE = 1;
	public static final int NEVER_FUMBLE = 2;
	public static final int WEAKENS = 3;
	public static final int HOBO_POWERED = 4;

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
		{ "Hobo Powered",
		  Pattern.compile( "Converts Hobo Power to" ),
		  Pattern.compile( "Hobo Powered" )
		},
	};

	public static final int BOOLEAN_MODIFIERS = Modifiers.booleanModifiers.length;

	public static final int CLASS = 0;
	public static final int INTRINSIC_EFFECT = 1;
	public static final int EQUALIZE = 2;

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
	};

	public static final int STRING_MODIFIERS = Modifiers.stringModifiers.length;
	
	// Clownosity behaves differently from any other current modifiers - multiples of the
	// same item do not contribute any more towards it, even if their other attributes do
	// stack.  Treat it as a special case for now, rather than creating a 4th class of
	// modifier types.  Currently, there are 19 distinct points of Clownosity available,
	// so bits in an int will work to represent them.
	
	private static final Pattern CLOWNOSITY_PATTERN =
		Pattern.compile( "Clownosity: ([+-]\\d+)" );
	private static int clownosityMask = 1;

	public static final String getModifierName( final int index )
	{
		return Modifiers.modifierName( Modifiers.floatModifiers, index );
	}

	private static final String modifierName( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return (String) table[ index ][ 0 ];
	};

	private static final Pattern modifierDescPattern( final Object[][] table, final int index )
	{
		if ( index < 0 || index >= table.length )
		{
			return null;
		}
		return (Pattern) table[ index ][ 1 ];
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
			if ( name.equals( (String) table[ i ][ 0 ] ) )
			{
				return i;
			}
		}
		return -1;
	};

	private boolean variable;
	private final float[] floats;
	private final boolean[] booleans;
	private final String[] strings;
	private float[] hoboFactors;
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
		this.hoboFactors = null;
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

	public void add( final int index, final double mod )
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
		case MOX_PCT:
		case MUS_PCT:
		case MYS_PCT:
		case HP_PCT:
		case MP_PCT:
		case SPELL_DAMAGE_PCT:
		case FAMILIAR_WEIGHT_PCT:
			double a = this.floats[ index ];
			double b = mod;
			double sum = a + b;
			// Negative percents are multiplicative
			if ( b < 0 )
			{
				sum = ( a + 100.0 ) * ( b + 100.0 ) / 100.0 - 100.0;
			}
			this.floats[ index ] = (float) sum;
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

		// Add in the float modifiers

		float[] addition = mods.floats;

		for ( int i = 0; i < this.floats.length; ++i )
		{
			if ( addition[ i ] != 0.0f )
			{
				this.add( i, addition[ i ] );
			}
		}

		// OR in certain boolean modifiers

		if ( !this.booleans[ Modifiers.NEVER_FUMBLE ] )
		{
			this.booleans[ Modifiers.NEVER_FUMBLE ] = mods.booleans[ Modifiers.NEVER_FUMBLE ];
		}

		if ( !this.booleans[ Modifiers.WEAKENS ] )
		{
			this.booleans[ Modifiers.WEAKENS ] = mods.booleans[ Modifiers.WEAKENS ];
		}
		
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
		float[] newFloats = newMods.floats;
		boolean[] newBooleans = newMods.booleans;
		String[] newStrings = newMods.strings;

		for ( int i = 0; i < newFloats.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.floatModifiers, i );
			if ( pattern != null )
			{
				Matcher matcher = pattern.matcher( string );
				if ( matcher.find() )
				{
					newFloats[ i ] = Float.parseFloat( matcher.group( 1 ) );
					continue;
				}
			}
		}

		for ( int i = 0; i < newBooleans.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.booleanModifiers, i );
			if ( pattern != null )
			{
				Matcher matcher = pattern.matcher( string );
				if ( matcher.find() )
				{
					newBooleans[ i ] = true;
					continue;
				}
			}
		}

		for ( int i = 0; i < newStrings.length; ++i )
		{
			Pattern pattern = Modifiers.modifierTagPattern( Modifiers.stringModifiers, i );
			if ( pattern != null )
			{
				Matcher matcher = pattern.matcher( string );
				if ( matcher.find() )
				{
					newStrings[ i ] = matcher.group( 1 );
					continue;
				}
			}
		}
		
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
		Modifiers.modifiersByName.put( name, newMods );

		return newMods;
	};

	// Items that modify based on moon signs
	private static final int BAIO = 877;
	private static final int JEKYLLIN = 1291;

	private static final int GOGGLES = 1540;
	private static final int GLAIVE = 1541;
	private static final int GREAVES = 1542;
	private static final int GARTER = 1543;
	private static final int GALOSHES = 1544;
	private static final int GORGET = 1545;
	private static final int GUAYABERA = 1546;

	private static final int GASMASK = 2819;
	private static final int GAT = 2820;
	private static final int GAITERS = 2821;
	private static final int GAUNTLETS = 2822;
	private static final int GO_GO_BOOTS = 2823;
	private static final int GIRDLE = 2824;
	private static final int GOWN = 2825;

	// Items that modify based on day of week
	private static final int TUESDAYS_RUBY = 2604;

	// Items that modify based on character level
	private static final int PILGRIM_SHIELD = 2090;

	// Items that modify based on holiday
	private static final int PARTY_HAT = 2945;

	// Effects that modify based on remaining duration
	private static final AdventureResult MALLOWED_OUT = new AdventureResult( "Mallowed Out", 0, true );

	private boolean override( final String name )
	{
		if ( this.booleans[ HOBO_POWERED ] )
		{
			if ( this.hoboFactors == null )
			{
				this.hoboFactors = (float []) this.floats.clone();
				// Any values >= 200 will be treated as conversion factors
				// (divided by 1000) from Hobo Power to the modifier.
			}
			float hoboPower = KoLCharacter.getHoboPower();
			for ( int i = 0; i < this.floats.length; ++i )
			{
				float factor = this.hoboFactors[ i ];
				if ( factor >= 200.0f )
				{
					this.floats[ i ] = factor * hoboPower / 1000.0f;
				}
			}
			return true;
		}
		
		if ( name.equalsIgnoreCase( "Temporary Lycanthropy" ) )
		{
			this.set( Modifiers.MUS_PCT, HolidayDatabase.getBloodEffect() );
			return true;
		}

		if ( name.equalsIgnoreCase( "Ur-Kel's Aria of Annoyance" ) )
		{
			this.set( Modifiers.MONSTER_LEVEL, 2 * KoLCharacter.getLevel() );
			return true;
		}

		if ( name.equalsIgnoreCase( "Starry-Eyed" ) )
		{
			int mod = 5 * KoLCharacter.getTelescopeUpgrades();
			this.set( Modifiers.MUS_PCT, mod );
			this.set( Modifiers.MYS_PCT, mod );
			this.set( Modifiers.MOX_PCT, mod );
			return true;
		}

		if ( name.equalsIgnoreCase( "Mallowed Out" ) )
		{
			int mod = 5 * Modifiers.MALLOWED_OUT.getCount( KoLConstants.activeEffects );
			this.set( Modifiers.MUS_PCT, mod );
			this.set( Modifiers.MYS_PCT, mod );
			this.set( Modifiers.MOX_PCT, mod );
			return true;
		}

		int itemId = ItemDatabase.getItemId( name );

		switch ( itemId )
		{
		case GALOSHES:
		case GAT:
		case GO_GO_BOOTS:
		case GREAVES:
			this.set( Modifiers.MOX_PCT, HolidayDatabase.getGrimaciteEffect() );
			return true;

		case GAITERS:
		case GARTER:
		case GIRDLE:
		case GOGGLES:
			this.set( Modifiers.MYS_PCT, HolidayDatabase.getGrimaciteEffect() );
			return true;

		case GASMASK:
		case GAUNTLETS:
		case GLAIVE:
		case GORGET:
			this.set( Modifiers.MUS_PCT, HolidayDatabase.getGrimaciteEffect() );
			return true;

		case GUAYABERA:
		case GOWN:
			this.set( Modifiers.MONSTER_LEVEL, HolidayDatabase.getGrimaciteEffect() );
			return true;

		case BAIO:
			int mod = HolidayDatabase.getBaioEffect();
			this.set( Modifiers.MOX_PCT, mod );
			this.set( Modifiers.MUS_PCT, mod );
			this.set( Modifiers.MYS_PCT, mod );
			return true;

		case JEKYLLIN:
			int moonlight = HolidayDatabase.getMoonlight();
			this.set( Modifiers.MOX, 9 - moonlight );
			this.set( Modifiers.MUS, 9 - moonlight );
			this.set( Modifiers.MYS, 9 - moonlight );
			this.set( Modifiers.ITEMDROP, 15 + moonlight * 5 );
			return true;

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

		case PILGRIM_SHIELD:
			this.set( Modifiers.DAMAGE_REDUCTION, KoLCharacter.getLevel() );
			return true;

		case PARTY_HAT:
			// Reset to defaults
			this.set( Modifiers.MP_REGEN_MIN, 0 );
			this.set( Modifiers.MP_REGEN_MAX, 0 );

			// Party hat is special on the Festival of Jarlsberg
			if ( HolidayDatabase.getHoliday().equals( "Festival of Jarlsberg" ) )
			{
				this.set( Modifiers.MP_REGEN_MIN, 3 );
				this.set( Modifiers.MP_REGEN_MAX, 5 );
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

		// Varies according to level, somehow

		if ( KoLCharacter.hasSkill( "Skin of the Leatherback" ) )
		{
			this.add( Modifiers.DAMAGE_REDUCTION, Math.max( ( KoLCharacter.getLevel() >> 1 ) - 1, 1 ) );
		}

		if ( KoLCharacter.getFamiliar().getId() == 38 && KoLCharacter.hasAmphibianSympathy() )
		{
			this.add( Modifiers.FAMILIAR_WEIGHT, -10 );
		}
	}

	private static final double dropFamiliarExponent = 1.0 / Math.sqrt( 2 );
	private static final double puppyFactor = Math.sqrt( 1.5 );
	private static final double heavyFamiliarFactor = 10.0 / 300.0;

	public void applyFamiliarModifiers( final FamiliarData familiar )
	{
		int familiarId = familiar.getId();
		int weight = familiar.getWeight() + (int) this.get( Modifiers.FAMILIAR_WEIGHT );
		float percent = this.get( Modifiers.FAMILIAR_WEIGHT_PCT ) / 100.0f;

		if ( percent != 0.0f )
		{
			weight = (int) Math.floor( weight + weight * percent );
		}

		weight = Math.max( 1, weight );

		if ( FamiliarDatabase.isVolleyType( familiarId ) )
		{
			this.add( Modifiers.EXPERIENCE, Math.sqrt( weight ) );
		}

		if ( FamiliarDatabase.isMeatDropType( familiarId ) )
		{
			// A Leprechaun provides 100% at 20 lbs.

			// Starwed's formula seems to accurately model a 1-20
			// lb. leprechaun

			// http://jick-nerfed.us/forums/viewtopic.php?t=3872
			// ( .05 * x ) ** ( 1 / sqrt(2) )

			double mod = weight >= 20 ? 1.0 : Math.pow( weight * 0.05, Modifiers.dropFamiliarExponent );
			if ( weight > 20 )
			{
				mod += ( weight - 20 ) * Modifiers.heavyFamiliarFactor;
			}

			this.add( Modifiers.MEATDROP, 100.0 * mod );
		}

		boolean fairy = FamiliarDatabase.isFairyType( familiarId );
		double itemWeight = weight;

		if ( FamiliarDatabase.isPuppyType( familiarId ) )
		{
			// A Jumpsuited Hound dog is a fairy at sqrt( 1.5 )
			// weight.

			itemWeight = weight * puppyFactor;
			fairy = true;
		}

		if ( fairy )
		{
			// A Gravy Fairy provides 50% at 20 lbs.

			// Starwed has formula which is decent for a 1-20
			// lb. fairy, but which gives 52% at 20 lbs.

			// http://jick-nerfed.us/forums/viewtopic.php?p=56342
			// ( .02 * x ) ** ( 1 / sqrt(2) )

			// However, spading on the AFH forum indicates that
			// a Leprechaun gives about (10/3)% pound above 20.
			// and a Fairy gives about (5/3)% pound above 20.

			// http://afh.s4.bizhat.com/viewtopic.php?t=712&mforum=afh

			// For mathematical elegance, we're going to assume
			// that a Fairy is exactly half as effective as a
			// Leprechaun.

			double mod = itemWeight >= 20 ? 1.0 : Math.pow( itemWeight * 0.05, Modifiers.dropFamiliarExponent );
			if ( itemWeight > 20 )
			{
				mod += ( itemWeight - 20 ) * Modifiers.heavyFamiliarFactor;
			}

			this.add( Modifiers.ITEMDROP, 50.0 * mod );
		}

		switch ( familiarId )
		{
		case 69:
			// Jumpsuited Hound Dog This is an item drop familiar
			// but the formula is not spaded yet.
			// It also increases combat frequency:
			//   min (5, floor (weight / 6 ) )
			int combat = weight / 6;
			if ( combat > 5 )
				combat = 5;
			this.add( Modifiers.COMBAT_RATE, combat );
			break;

		case 72:
			// Exotic Parrot

			// Gives elemental resistances based on weight.
			// 1 level for every 4 lbs, applied in the order
			// Hot, Cold, Spooky, Stench, Sleaze.

			this.add( Modifiers.HOT_RESISTANCE, ( weight + 19 ) / 20 );
			this.add( Modifiers.COLD_RESISTANCE, ( weight + 15 ) / 20 );
			this.add( Modifiers.SPOOKY_RESISTANCE, ( weight + 11 ) / 20 );
			this.add( Modifiers.STENCH_RESISTANCE, ( weight + 7 ) / 20 );
			this.add( Modifiers.SLEAZE_RESISTANCE, ( weight + 3 ) / 20 );

			break;
		}
	}
	
	public static final String getFamiliarEffect( final String itemName )
	{
		return (String) Modifiers.familiarEffectByName.get( 
			StringUtilities.getCanonicalName( itemName ) );
	}


	// Parsing item enchantments into KoLmafia modifiers

	private static final Pattern DR_PATTERN = Pattern.compile( "Damage Reduction: <b>(\\d+)</b>" );

	public static final String parseDamageReduction( final String text )
	{
		Matcher matcher = Modifiers.DR_PATTERN.matcher( text );
		if ( matcher.find() )
		{
			return Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.DAMAGE_REDUCTION ) + ": " + matcher.group( 1 );
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

	private static final Pattern ALL_ATTR_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)$" );
	private static final Pattern ALL_ATTR_PCT_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)%$" );
	private static final Pattern CLASS_PATTERN = Pattern.compile( "Bonus for (.*) only" );
	private static final Pattern COMBAT_PATTERN = Pattern.compile( "Monsters will be (.*) attracted to you." );
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
			return Modifiers.MOXIE + mod + ", " + Modifiers.MUSCLE + mod + ", " + Modifiers.MYSTICALITY + mod;
		}

		matcher = Modifiers.ALL_ATTR_PCT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group( 1 );
			return Modifiers.MOXIE_PCT + mod + ", " + Modifiers.MUSCLE_PCT + mod + ", " + Modifiers.MYSTICALITY_PCT + mod;
		}

		matcher = Modifiers.CLASS_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String plural = matcher.group( 1 );
			String cls = "none";
			if ( plural.equals( "Accordion Thieves" ) )
			{
				cls = KoLCharacter.ACCORDION_THIEF;
			}
			else if ( plural.equals( "Disco Bandits" ) )
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
			else if ( plural.equals( "Seal Clubbers" ) )
			{
				cls = KoLCharacter.SEAL_CLUBBER;
			}
			else if ( plural.equals( "Turtle Tamers" ) )
			{
				cls = KoLCharacter.TURTLE_TAMER;
			}
			return Modifiers.modifierName( Modifiers.stringModifiers, Modifiers.CLASS ) + ": \"" + cls + "\"";
		}

		matcher = Modifiers.COMBAT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			return Modifiers.modifierName( Modifiers.floatModifiers, Modifiers.COMBAT_RATE ) + ": " + ( matcher.group(
				1 ).equals( "more" ) ? "+5" : "-5" );
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
			Pattern pattern = Modifiers.modifierDescPattern( table, i );
			if ( pattern == null )
			{
				continue;
			}

			Matcher matcher = pattern.matcher( enchantment );
			if ( matcher.find() )
			{
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
		else if ( enchantment.indexOf( "Sublime" ) != -1 )
		{
			level = "+4";
		}
		else if ( enchantment.indexOf( "Superhuman" ) != -1 )
		{
			level = "+5";
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
				RequestLogger.printLine( "Key \"" + name + "\" already parsed. Skipping." );
				continue;
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
				RequestLogger.printLine( "Key \"" + name + "\" has unknown modifier: \"" + mod + "\"" );
			}
		}
	}
}
