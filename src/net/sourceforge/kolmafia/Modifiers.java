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
import java.util.TimeZone;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Modifiers extends KoLDatabase
{
	private static final Map modifierMap = new TreeMap();
	private static final ArrayList passiveSkills = new ArrayList();

	static
	{
		BufferedReader reader = getReader( "modifiers.txt" );
		String [] data;

		while ( (data = readData( reader )) != null )
                {
			if ( data.length != 2 )
                                continue;

                        String name = getCanonicalName( data[0] );
                        modifierMap.put( name, data[1] );
                }

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
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

	private static final Object [][] floatModifiers = {
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
		  Pattern.compile( "Damage Reduction: (\\d+)" )
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
		  Pattern.compile( "Melee Damage ([+-]\\d+)" ),
		  Pattern.compile( "Melee Damage: ([+-]\\d+)" )
		},
		{ "Ranged Damage",
		  Pattern.compile( "Ranged Damage ([+-]\\d+)" ),
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
	};

	public static final int FLOAT_MODIFIERS = floatModifiers.length;

	public static final int SOFTCORE = 0;
	public static final int SINGLE = 1;
	public static final int NEVER_FUMBLE = 2;
	public static final int WEAKENS = 3;

        private static final Object [][] booleanModifiers = {
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
	};

	public static final int BOOLEAN_MODIFIERS = booleanModifiers.length;

	public static final int CLASS = 0;
	public static final int INTRINSIC_EFFECT = 1;
	public static final int EQUALIZE = 2;

        private static final Object [][] stringModifiers = {
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

	public static final int STRING_MODIFIERS = stringModifiers.length;

	private static final String modifierName( Object [][] table, int index )
	{
		if ( index < 0 || index >= table.length )
			return null;
		return (String)table[index][0];
	};

	private static final Pattern modifierDescPattern( Object [][] table, int index )
	{
		if ( index < 0 || index >= table.length )
			return null;
		return (Pattern)table[index][1];
	};

	private static final Pattern modifierTagPattern( Object [][] table, int index )
	{
		if ( index < 0 || index >= table.length )
			return null;
		return (Pattern)table[index][2];
	};

	private static final String COLD = modifierName( floatModifiers, COLD_RESISTANCE ) + ": ";
	private static final String HOT = modifierName( floatModifiers, HOT_RESISTANCE ) + ": ";
	private static final String SLEAZE = modifierName( floatModifiers, SLEAZE_RESISTANCE ) + ": ";
	private static final String SPOOKY = modifierName( floatModifiers, SPOOKY_RESISTANCE ) + ": ";
	private static final String STENCH = modifierName( floatModifiers, STENCH_RESISTANCE ) + ": ";

	private static final String MOXIE = modifierName( floatModifiers, MOX ) + ": ";
	private static final String MUSCLE = modifierName( floatModifiers, MUS ) + ": ";
	private static final String MYSTICALITY = modifierName( floatModifiers, MYS ) + ": ";

	private static final String MOXIE_PCT = modifierName( floatModifiers, MOX_PCT ) + ": ";
	private static final String MUSCLE_PCT = modifierName( floatModifiers, MUS_PCT ) + ": ";
	private static final String MYSTICALITY_PCT = modifierName( floatModifiers, MYS_PCT ) + ": ";

	private static final String HP_TAG = modifierName( floatModifiers, HP ) + ": ";
	private static final String MP_TAG = modifierName( floatModifiers, MP ) + ": ";

	private static final String HP_REGEN_MIN_TAG = modifierName( floatModifiers, HP_REGEN_MIN ) + ": ";
	private static final String HP_REGEN_MAX_TAG = modifierName( floatModifiers, HP_REGEN_MAX ) + ": ";
	private static final String MP_REGEN_MIN_TAG = modifierName( floatModifiers, MP_REGEN_MIN ) + ": ";
	private static final String MP_REGEN_MAX_TAG = modifierName( floatModifiers, MP_REGEN_MAX ) + ": ";

	private static final int findName( Object [][] table, String name )
	{
		for ( int i = 0; i < table.length; ++i )
			if ( name.equals( (String)table[i][0] ) )
				return i;
		return -1;
	};

	private boolean variable;
	private float[] floats;
	private boolean[] booleans;
	private String[] strings;

	public Modifiers()
	{
		this.variable = false;
		this.floats = new float[ FLOAT_MODIFIERS ];
		this.booleans = new boolean[ BOOLEAN_MODIFIERS ];
		this.strings = new String[ STRING_MODIFIERS ];
		reset();
	};

        public void reset()
	{
		for ( int i = 0; i < this.floats.length; ++i )
			this.floats[i] = 0.0f;
		for ( int i = 0; i < this.booleans.length; ++i )
			this.booleans[i] = false;
		for ( int i = 0; i < this.strings.length; ++i )
			this.strings[i] = "";
	};

	public float get( int index )
	{
		if ( index < 0 || index >= this.floats.length )
			return 0.0f;

		return this.floats[index];
	};

	public float get( String name )
	{
		int index = findName( floatModifiers, name );
		if ( index < 0 || index >= this.floats.length )
			return 0.0f;

		return this.floats[index];
	};

	public boolean getBoolean( int index )
	{
		if ( index < 0 || index >= this.booleans.length )
			return false;

		return this.booleans[index];
	};

	public boolean getBoolean( String name )
	{
		int index = findName( booleanModifiers, name );
		if ( index < 0 || index >= this.booleans.length )
			return false;

		return this.booleans[index];
	};

	public String getString( int index )
	{
		if ( index < 0 || index >= this.strings.length )
			return "";

		return this.strings[index];
	};

	public String getString( String name )
	{
		int index = findName( stringModifiers, name );
		if ( index < 0 || index >= this.strings.length )
			return "";

		return this.strings[index];
	};

	public boolean set( int index, double mod )
	{
		if ( index < 0 || index >= this.floats.length )
			return false;

		if ( this.floats[index] != mod )
		{
			this.floats[index] = (float)mod;
			return true;
		}
		return false;
	};

	public boolean set( int index, boolean mod )
	{
		if ( index < 0 || index >= this.booleans.length )
			return false;

		if ( this.booleans[index] != mod )
		{
			this.booleans[index] = mod;
			return true;
		}
		return false;
	};

	public boolean set( int index, String mod )
	{
		if ( index < 0 || index >= this.strings.length )
			return false;

		if ( mod == null )
			mod = "";

		if ( !mod.equals( this.strings[index] ) )
		{
			this.strings[index] = mod;
			return true;
		}
		return false;
	};

	public boolean set( Modifiers mods )
	{
		if ( mods == null )
			return false;

		boolean changed = false;

		float [] copyFloats = mods.floats;
		for ( int index = 0; index < this.floats.length; ++index )
			if ( this.floats[index] != copyFloats[index] )
			{
				this.floats[index] = copyFloats[index];
				changed = true;
			}

		boolean [] copyBooleans = mods.booleans;
		for ( int index = 0; index < this.booleans.length; ++index )
			if ( this.booleans[index] != copyBooleans[index] )
			{
				this.booleans[index] = copyBooleans[index];
				changed = true;
			}

		String [] copyStrings = mods.strings;
		for ( int index = 0; index < this.strings.length; ++index )
			if ( !this.strings[index].equals( copyStrings[index] ) )
			{
				this.strings[index] = copyStrings[index];
				changed = true;
			}

		return changed;
	}

	public void add( int index, double mod )
	{
		if ( index < 0 || index >= this.floats.length )
			return;

		switch ( index )
		{
		case CRITICAL:
			// Critical hit modifier is maximum, not additive
			if ( mod > this.floats[index] )
				this.floats[index] = (float)mod;
			break;
		case MANA_COST:
			// Total Mana Cost reduction cannot exceed 3
			this.floats[index] += mod;
			if ( this.floats[index] < -3 )
				this.floats[index] = -3;
			break;
		case  MOX_PCT:
		case  MUS_PCT:
		case  MYS_PCT:
		case  HP_PCT:
		case  MP_PCT:
		case  SPELL_DAMAGE_PCT:
			// Percents are multiplicative
			double a = this.floats[index];
			double b = mod;
			double sum = ( ( a + 100.0 ) * ( b + 100.0 ) / 100.0 ) - 100.0;
			this.floats[index] = (float)sum;
			break;
		case COLD_RESISTANCE:
		case HOT_RESISTANCE:
		case SLEAZE_RESISTANCE:
		case SPOOKY_RESISTANCE:
		case STENCH_RESISTANCE:
			// If already at maximal resistance, keep it
			if ( this.floats[index] == 90 )
				break;
			// If new mod is maximal, set maximal
			if ( mod == 90 )
			{
				this.floats[index] = 90;
				break;
			}
			// Otherwise, cap at 60 or 80
			this.floats[index] = Math.min( this.floats[index] + (float)mod, KoLCharacter.isMysticalityClass() ? 80 : 60 );
			break;
		default:
			this.floats[index] += mod;
			break;
		}
	};

	public void add( Modifiers mods )
	{
		if ( mods == null )
			return;

		// Make sure the modifiers apply to current class
		String type = mods.getString( CLASS );
		if ( !type.equals( "" ) && !type.equals( KoLCharacter.getClassType() ) )
			return;

		// Add in the float modifiers

		float [] addition = mods.floats;

		for ( int i = 0; i < this.floats.length; ++i )
			if ( addition[i] != 0.0f )
				add( i, addition[i] );

		// OR in certain boolean modifiers

		booleans[ NEVER_FUMBLE ] |= mods.booleans[ NEVER_FUMBLE ];
		booleans[ WEAKENS ] |= mods.booleans[ WEAKENS ];

		// If the item provides an intrinsic effect, add it in
		add( getModifiers( mods.getString( INTRINSIC_EFFECT ) ) );
	}

	public static final Modifiers getModifiers( String name )
	{
		if ( name == null || name.equals( "" ) )
			return null;

		name = getCanonicalName( name );
		Object modifier = modifierMap.get( name );

		if ( modifier == null )
			return null;

		if ( modifier instanceof Modifiers )
		{
			Modifiers mods = (Modifiers)modifier;
			if ( mods.variable )
				mods.override( name );
			return mods;
		}

		if ( !( modifier instanceof String ) )
			return null;

		String string = (String) modifier;

		Modifiers newMods = new Modifiers();
		float [] newFloats = newMods.floats;
		boolean [] newBooleans = newMods.booleans;
		String [] newStrings = newMods.strings;

		for ( int i = 0; i < newFloats.length; ++i )
		{
			Pattern pattern = modifierTagPattern( floatModifiers, i );
			if ( pattern != null )
			{
				Matcher matcher = pattern.matcher( string );
				if ( matcher.find() )
				{
					newFloats[i] = Float.parseFloat ( matcher.group(1) );
					continue;
				}
			}
		}

		for ( int i = 0; i < newBooleans.length; ++i )
		{
			Pattern pattern = modifierTagPattern( booleanModifiers, i );
			if ( pattern != null )
			{
				Matcher matcher = pattern.matcher( string );
				if ( matcher.find() )
				{
					newBooleans[i] = true;
					continue;
				}
			}
		}

		for ( int i = 0; i < newStrings.length; ++i )
		{
			Pattern pattern = modifierTagPattern( stringModifiers, i );
			if ( pattern != null )
			{
				Matcher matcher = pattern.matcher( string );
				if ( matcher.find() )
				{
					newStrings[i] =	 matcher.group(1);
					continue;
				}
			}
		}

		newMods.variable = newMods.override( name );
		modifierMap.put( name, newMods );

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

	private boolean override( String name )
	{
		if ( name.equalsIgnoreCase( "Temporary Lycanthropy" ) )
		{
			set( MUS_PCT, MoonPhaseDatabase.getBloodEffect() );
			return true;
		}

		if ( name.equalsIgnoreCase( "Ur-Kel's Aria of Annoyance" ) )
		{
			set( MONSTER_LEVEL, 2 * KoLCharacter.getLevel() );
			return true;
		}

		int itemId = TradeableItemDatabase.getItemId( name );

		switch ( itemId )
		{
		case GALOSHES:
		case GAT:
		case GO_GO_BOOTS:
		case GREAVES:
			set( MOX_PCT, MoonPhaseDatabase.getGrimaciteEffect() );
			return true;

		case GAITERS:
		case GARTER:
		case GIRDLE:
		case GOGGLES:
			set( MYS_PCT, MoonPhaseDatabase.getGrimaciteEffect() );
			return true;

		case GASMASK:
		case GAUNTLETS:
		case GLAIVE:
		case GORGET:
			set( MUS_PCT, MoonPhaseDatabase.getGrimaciteEffect() );
			return true;

		case GUAYABERA:
		case GOWN:
			set( MONSTER_LEVEL, MoonPhaseDatabase.getGrimaciteEffect() );
			return true;

		case BAIO:
			int mod = MoonPhaseDatabase.getBaioEffect();
			set( MOX_PCT, mod );
			set( MUS_PCT, mod );
			set( MYS_PCT, mod );
			return true;

		case JEKYLLIN:
			int moonlight = MoonPhaseDatabase.getMoonlight();
			set( MOX, 9 - moonlight );
			set( MUS, 9 - moonlight );
			set( MYS, 9 - moonlight );
			set( ITEMDROP, 15 + moonlight * 5 );
			return true;

		case TUESDAYS_RUBY:
			// Reset to defaults

			set( MEATDROP, 0 );
			set( ITEMDROP, 0 );
			set( MOX_PCT, 0 );
			set( MUS_PCT, 0 );
			set( MYS_PCT, 0 );
			set( HP_REGEN_MIN, 0 );
			set( HP_REGEN_MAX, 0 );
			set( MP_REGEN_MIN, 0 );
			set( MP_REGEN_MAX, 0 );

			// Set modifiers depending on what day of the week it
			// is at the KoL servers

			Calendar KoL = Calendar.getInstance( TimeZone.getTimeZone( "GMT-7" ) );
			switch ( KoL.DAY_OF_WEEK )
			{
			case Calendar.SUNDAY:
				// +5% Meat from Monsters
				set( MEATDROP, 5 );
				break;
			case Calendar.MONDAY:
				// Muscle +5%
				set( MUS_PCT, 5 );
				break;
			case Calendar.TUESDAY:
				// Regenerate 3-7 MP per adventure
				set( MP_REGEN_MIN, 3 );
				set( MP_REGEN_MAX, 7 );
				break;
			case Calendar.WEDNESDAY:
				// +5% Mysticality
				set( MYS_PCT, 5 );
				break;
			case Calendar.THURSDAY:
				// +5% Item Drops from Monsters
				set( ITEMDROP, 5 );
				break;
			case Calendar.FRIDAY:
				// +5% Moxie
				set( MOX_PCT, 5 );
				break;
			case Calendar.SATURDAY:
				// Regenerate 3-7 HP per adventure
				set( HP_REGEN_MIN, 3 );
				set( HP_REGEN_MAX, 7 );
				break;
			}
			return true;
		}

		return false;
	};

	public static final float getNumericModifier( String name, String mod )
	{
		Modifiers mods = getModifiers( name );
		if ( mods == null )
			return 0.0f;
		return mods.get( mod );
	}

	public static final boolean getBooleanModifier( String name, String mod )
	{
		Modifiers mods = getModifiers( name );
		if ( mods == null )
			return false;
		return mods.getBoolean( mod );
	}

	public static final String getStringModifier( String name, String mod )
	{
		Modifiers mods = getModifiers( name );
		if ( mods == null )
			return "";
		return mods.getString( mod );
	}

	public void applyPassiveModifiers()
	{
		// You'd think this could be done at class initialization time,
		// but no: the ClassSkillsDatabase depends on the Mana Cost
		// modifier being set.

		if ( passiveSkills.isEmpty() )
		{
			Object [] keys = modifierMap.keySet().toArray();
			for ( int i = 0; i < keys.length; ++i )
			{
				String skill = (String)keys[i];
				if ( !ClassSkillsDatabase.contains( skill ) )
					continue;

				if ( ClassSkillsDatabase.getSkillType( ClassSkillsDatabase.getSkillId( skill ) ) == ClassSkillsDatabase.PASSIVE )
					passiveSkills.add( skill );
			}
		}

		for ( int i = 0; i < passiveSkills.size(); ++i )
		{
			String skill = (String) passiveSkills.get(i);
			if ( KoLCharacter.hasSkill( skill ) )
				add( getModifiers( skill ) );
		}

		// Varies according to level, somehow

		if ( KoLCharacter.hasSkill( "Skin of the Leatherback" ) )
			add( DAMAGE_REDUCTION, Math.max( (KoLCharacter.getLevel() >> 1) - 1, 1 ) );

		if ( KoLCharacter.getFamiliar().getId() == 38 && KoLCharacter.hasAmphibianSympathy() )
			add( FAMILIAR_WEIGHT, -10 );
	}

	public void applyFamiliarModifiers( FamiliarData familiar )
	{
		int familiarId = familiar.getId();
		int weight = familiar.getWeight() + (int)get( FAMILIAR_WEIGHT );

		if ( FamiliarsDatabase.isVolleyType( familiarId ) )
			add( EXPERIENCE, Math.sqrt( weight ) );

		if ( FamiliarsDatabase.isItemDropType( familiarId ) )
			add( ITEMDROP, weight * 2.5 );

		if ( FamiliarsDatabase.isMeatDropType( familiarId ) )
			add( MEATDROP, weight * 5 );

		switch ( familiarId )
		{
		case 72:
			// Exotic Parrot

			// Gives elemental resistances based on weight.
			// 1 level for every 4 lbs, applied in the order
			// Hot, Cold, Spooky, Stench, Sleaze.

			add( HOT_RESISTANCE, ( ( weight + 16 ) / 20 ) * 10 );
			add( COLD_RESISTANCE, ( ( weight + 12 ) / 20 ) * 10 );
			add( SPOOKY_RESISTANCE, ( ( weight + 8 ) / 20 ) * 10 );
			add( STENCH_RESISTANCE, ( ( weight + 4 ) / 20 ) * 10 );
			add( SLEAZE_RESISTANCE, ( ( weight + 0 ) / 20 ) * 10 );

			break;
		}
	}

	// Parsing item enchantments into KoLmafia modifiers

	private static final Pattern DR_PATTERN = Pattern.compile( "Damage Reduction: <b>(\\d+)</b>" );

	public static final String parseDamageReduction( String text )
	{
		Matcher matcher = DR_PATTERN.matcher( text );
		if (matcher.find() )
			return modifierName( floatModifiers, DAMAGE_REDUCTION ) + ": " + matcher.group(1);

		return null;
	}

	private static final Pattern SINGLE_PATTERN = Pattern.compile( "You may not equip more than one of this item at a time" );

	public static final String parseSingleEquip( String text )
	{
		Matcher matcher = SINGLE_PATTERN.matcher( text );
		if (matcher.find() )
			return modifierName( booleanModifiers, SINGLE );

		return null;
	}

	private static final Pattern ALL_ATTR_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)$" );
	private static final Pattern ALL_ATTR_PCT_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)%$" );
	private static final Pattern CLASS_PATTERN = Pattern.compile( "Bonus for (.*) only" );
	private static final Pattern COMBAT_PATTERN = Pattern.compile( "Monsters will be (.*) attracted to you." );
	private static final Pattern HP_MP_PATTERN = Pattern.compile( "^Maximum HP/MP ([+-]\\d+)$" );

	public static final String parseModifier( String enchantment )
	{
		String result;

		// Search the float modifiers first

		result = parseModifier( floatModifiers, enchantment, false );
		if (result != null )
			return result;

		// Then the boolean modifiers

		result = parseModifier( booleanModifiers, enchantment, false );
		if (result != null )
			return result;

		// Then the string modifiers

		result = parseModifier( stringModifiers, enchantment, true );
		if (result != null )
			return result;

		// Special handling needed

		Matcher matcher;

		matcher = ALL_ATTR_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group(1);
			return MOXIE + mod + ", " + MUSCLE + mod + ", " + MYSTICALITY + mod;
		}

		matcher = ALL_ATTR_PCT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group(1);
			return MOXIE_PCT + mod + ", " + MUSCLE_PCT + mod + ", " + MYSTICALITY_PCT + mod;
		}

		matcher = CLASS_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String plural = matcher.group(1);
			String cls = "none";
			if ( plural.equals( "Accordion Thieves" ) )
				cls = KoLCharacter.ACCORDION_THIEF;
			else if ( plural.equals( "Disco Bandits" ) )
				cls = KoLCharacter.DISCO_BANDIT;
			else if ( plural.equals( "Pastamancers" ) )
				cls = KoLCharacter.PASTAMANCER;
			else if ( plural.equals( "Saucerors" ) )
				cls = KoLCharacter.SAUCEROR;
			else if ( plural.equals( "Seal Clubbers" ) )
				cls = KoLCharacter.SEAL_CLUBBER;
			else if ( plural.equals( "Turtle Tamers" ) )
				cls = KoLCharacter.TURTLE_TAMER;
			return modifierName( stringModifiers, CLASS ) + ": \"" + cls + "\"";
		}

		matcher = COMBAT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return modifierName( floatModifiers, COMBAT_RATE ) + ": " + ( matcher.group(1).equals( "more" ) ? "+5" : "-5" );

		matcher = HP_MP_PATTERN.matcher( enchantment );
		if ( matcher.find() )
		{
			String mod = matcher.group(1);
			return HP_TAG + mod + ", " + MP_TAG + mod;
		}

		if ( enchantment.indexOf( "Regenerate" ) != -1 )
			return parseRegeneration( enchantment );

		if ( enchantment.indexOf( "Resistance" ) != -1 )
			return parseResistance( enchantment );

		return null;
	}

	private static final String parseModifier( Object [][] table, String enchantment, boolean quoted )
	{
		String quote = quoted ? "\"" : "" ;
		for ( int i = 0; i < table.length; ++i )
		{
			Pattern pattern = modifierDescPattern( table, i );
			if ( pattern == null )
				continue;

			Matcher matcher = pattern.matcher( enchantment );
			if ( matcher.find() )
			{
				if ( matcher.groupCount() == 0)
					return modifierName( table, i );
				return modifierName( table, i ) + ": " + quote + matcher.group(1) + quote;
			}
		}

		return null;
	}

	private static final Pattern REGEN_PATTERN = Pattern.compile( "Regenerate (\\d*)-?(\\d*)? ([HM]P)( and .*)? per [aA]dventure$" );

	private static final String parseRegeneration( String enchantment )
	{
		Matcher matcher = REGEN_PATTERN.matcher( enchantment );
		if ( !matcher.find() )
			return null;

		String min = matcher.group(1);
		String max = matcher.group(2) == null ? min : matcher.group(2);
		boolean hp = matcher.group(3).equals( "HP" );
		boolean both = matcher.group(4) != null;

		if ( max.equals( "" ) )
			max = min;

		if ( both )
			return	HP_REGEN_MIN_TAG + min + ", " +
				HP_REGEN_MAX_TAG + max + ", " +
				MP_REGEN_MIN_TAG + min + ", " +
				MP_REGEN_MAX_TAG + max;

		if ( hp )
			return	HP_REGEN_MIN_TAG + min + ", " +
				HP_REGEN_MAX_TAG + max;

		return	MP_REGEN_MIN_TAG + min + ", " +
			MP_REGEN_MAX_TAG + max;
	}

	private static final String parseResistance( String enchantment )
	{
		String level = "";

		if ( enchantment.indexOf( "Slight" ) != -1 )
			level = "+10";
		else if ( enchantment.indexOf( "So-So" ) != -1 )
			level = "+20";
		else if ( enchantment.indexOf( "Serious" ) != -1 )
			level = "+30";
		else if ( enchantment.indexOf( "Superhuman" ) != -1 )
			level = "+40";

		if ( enchantment.indexOf( "All Elements" ) != -1 )
			return COLD + level + ", " + HOT + level + ", " + SLEAZE + level + ", " + SPOOKY + level + ", " + STENCH + level;

		if ( enchantment.indexOf( "Cold" ) != -1 )
			return COLD + level;

		if ( enchantment.indexOf( "Hot" ) != -1 )
			return HOT + level;

		if ( enchantment.indexOf( "Sleaze" ) != -1 )
			return SLEAZE + level;

		if ( enchantment.indexOf( "Spooky" ) != -1 )
			return SPOOKY + level;

		if ( enchantment.indexOf( "Stench" ) != -1 )
			return STENCH + level;

		return null;
	}

	private static final boolean findModifier( Object [][] table, String tag )
	{
		for ( int i = 0; i < table.length; ++i )
		{
			Pattern pattern = modifierTagPattern( table, i );
			if ( pattern == null )
				continue;

			Matcher matcher = pattern.matcher( tag );
			if ( matcher.find() )
				return true;
		}
		return false;
	}

	public static final void checkModifiers()
	{
		Object [] keys = modifierMap.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			String name = (String)keys[i];
			Object modifier = modifierMap.get( name );

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
			String [] strings = ((String)modifier).split( ", " );
			for ( int j = 0; j < strings.length; ++j )
			{
				String mod = strings[j].trim();
				if ( findModifier( floatModifiers, mod ) )
					continue;
				if ( findModifier( booleanModifiers, mod ) )
					continue;
				if ( findModifier( stringModifiers, mod ) )
					continue;
				RequestLogger.printLine( "Key \"" + name + "\" has unknown modifier: \"" + mod + "\"" );
			}
		}
	}
}
