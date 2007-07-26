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
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Modifiers extends KoLDatabase
{
	private static Map modifierMap = new TreeMap();
	private static ArrayList passiveSkills = new ArrayList();

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

	private static final Object [][] floatModifiers = {
		{ "Familiar Weight", "Weight",
		  Pattern.compile( "(.*) to Familiar Weight" ),
		  Pattern.compile( "Weight: ([+-]\\d+)" )
		},
		{ "Monster Level", "ML",
		  Pattern.compile( "(.*) to Monster Level" ),
		  Pattern.compile( "ML: ([+-]\\d+)" )
		},
		{ "Combat Rate", "Combat",
		  null,
		  Pattern.compile( "Combat: ([+-][\\d.]+)" )
		}, 
		{ "Initiative", "Init",
		  Pattern.compile( "Combat Initiative (.*)%" ),
		  Pattern.compile( "Init: ([+-][\\d.]+)" )
		},
		{ "Experience", "Exp",
		  Pattern.compile( "(.*) Stat.*Per Fight" ),
		  Pattern.compile( "Exp: ([+-][\\d.]+)" )
		},
		{ "Item Drop", "Item",
		  Pattern.compile( "(.*)% Item Drops from Monsters" ),
		  Pattern.compile( "Item: ([+-][\\d.]+)" )
		},
		{ "Meat Drop", "Meat",
		  Pattern.compile( "(.*)% Meat from Monsters" ),
		  Pattern.compile( "Meat: ([+-][\\d.]+)" )
		},
		{ "Damage Absorption", "DA",
		  Pattern.compile( "Damage Absorption (.*)" ),
		  Pattern.compile( "DA: ([+-]\\d+)" )
		},
		{ "Damage Reduction", "DR",
		  Pattern.compile( "Damage Reduction: (\\d+)" ),
		  Pattern.compile( "DR: (\\d+)" )
		},
		{ "Cold Resistance", "Cold",
		  null,
		  Pattern.compile( "Cold: ([+-]\\d+)" )
		},
		{ "Hot Resistance", "Hot",
		  null,
		  Pattern.compile( "Hot: ([+-]\\d+)" )
		},
		{ "Sleaze Resistance", "Sleaze",
		  null,
		  Pattern.compile( "Sleaze: ([+-]\\d+)" )
		},
		{ "Spooky Resistance", "Spooky",
		  null,
		  Pattern.compile( "Spooky: ([+-]\\d+)" )
		},
		{ "Stench Resistance", "Stench",
		  null,
		  Pattern.compile( "Stench: ([+-]\\d+)" )
		},
		{ "Mana Cost", "Mana",
		  Pattern.compile( "(.*) MP to use Skills" ),
		  Pattern.compile( "Mana: ([+-]\\d+)" )
		},
		{ "Moxie", "Mox",
		  Pattern.compile( "Moxie ([+-]\\d+)$" ),
		  Pattern.compile( "Mox: ([+-]\\d+)" )
		},
		{ "Moxie Percent", "Mox%",
		  Pattern.compile( "Moxie ([+-]\\d+)%" ),
		  Pattern.compile( "Mox%: ([+-]\\d+)" )
		},
		{ "Muscle", "Mus",
		  Pattern.compile( "Muscle ([+-]\\d+)$" ),
		  Pattern.compile( "Mus: ([+-]\\d+)" )
		},
		{ "Muscle Percent", "Mus%",
		  Pattern.compile( "Muscle ([+-]\\d+)%" ),
		  Pattern.compile( "Mus%: ([+-]\\d+)" )
		},
		{ "Mysticality", "Mys",
		  Pattern.compile( "Mysticality ([+-]\\d+)$" ),
		  Pattern.compile( "Mys: ([+-]\\d+)" )
		},
		{ "Mysticality Percent", "Mys%",
		  Pattern.compile( "Mysticality ([+-]\\d+)%" ),
		  Pattern.compile( "Mys%: ([+-]\\d+)" )
		},
	};

	public static final int FLOAT_MODIFIERS = floatModifiers.length;

	public static final int CLASS = 0;
	public static final int INTRINSIC_EFFECT = 1;

        private static final Object [][] stringModifiers = {
                { "Class", "Class",
                  null,
                  Pattern.compile( "Class: (\\w\\w)" )
                },
                { "Intrinsic Effect", "Intrinsic",
                  Pattern.compile( "Intrinsic effect: (.*)" ),
                  Pattern.compile( "Intrinsic: [^,]+" )
                },
	};

	public static final int STRING_MODIFIERS = stringModifiers.length;

	private static final String modifierName( Object [][] table, int index )
	{
		if ( index < 0 || index >= table.length )
			return null;
		return (String)table[index][0];
	};

	private static final String modifierTag( Object [][] table, int index )
	{
		if ( index < 0 || index >= table.length )
			return null;
		return (String)table[index][1];
	};

	private static final Pattern modifierDescPattern( Object [][] table, int index )
	{
		if ( index < 0 || index >= table.length )
			return null;
		return (Pattern)table[index][2];
	};

	private static final Pattern modifierTagPattern( Object [][] table, int index )
	{
		if ( index < 0 || index >= table.length )
			return null;
		return (Pattern)table[index][3];
	};

	private static final String COLD = modifierTag( floatModifiers, COLD_RESISTANCE ) + ": ";
	private static final String HOT = modifierTag( floatModifiers, HOT_RESISTANCE ) + ": ";
	private static final String SLEAZE = modifierTag( floatModifiers, SLEAZE_RESISTANCE ) + ": ";
	private static final String SPOOKY = modifierTag( floatModifiers, SPOOKY_RESISTANCE ) + ": ";
	private static final String STENCH = modifierTag( floatModifiers, STENCH_RESISTANCE ) + ": ";

	private static final String MOXIE = modifierTag( floatModifiers, MOX ) + ": ";
	private static final String MUSCLE = modifierTag( floatModifiers, MUS ) + ": ";
	private static final String MYSTICALITY = modifierTag( floatModifiers, MYS ) + ": ";

	private static final String MOXIE_PCT = modifierTag( floatModifiers, MOX_PCT ) + ": ";
	private static final String MUSCLE_PCT = modifierTag( floatModifiers, MUS_PCT ) + ": ";
	private static final String MYSTICALITY_PCT = modifierTag( floatModifiers, MYS_PCT ) + ": ";

	private static int findName( Object [][] table, String name )
	{
		for ( int i = 0; i < table.length; ++i )
			if ( name.equals( (String)table[i][0] ) )
				return i;
		return -1;
	};

        private float[] floats;
        private String[] strings;

	public Modifiers()
	{
		this.floats = new float[ FLOAT_MODIFIERS ];
		this.strings = new String[ STRING_MODIFIERS ];
		reset();
	};

        public void reset()
	{
		for ( int i = 0; i < this.floats.length; ++i )
			this.floats[i] = 0.0f;
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
		this.floats[index] += mod;
	};

	public void add( Modifiers mods )
	{
		if ( mods == null )
			return;

		// Make sure the modifiers apply to current class
		String type = mods.getString( CLASS );
		if ( type != null && !type.equals( KoLCharacter.getClassTypeAbbreviation() ) )
			return;

		float [] addition = mods.floats;

		for ( int i = 0; i < this.floats.length; ++i )
			if ( addition[i] != 0.0f )
				this.floats[i] += addition[i];

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
			return (Modifiers)modifier;

		if ( !( modifier instanceof String ) )
			return null;

		String string = (String) modifier;

		Modifiers newMods = new Modifiers();
		float [] newFloats = newMods.floats;
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

		modifierMap.put( name, newMods );
		return newMods;
	};

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

	// Parsing item enchantments into KoLmafia modifiers

	private static final Pattern DR_PATTERN = Pattern.compile( "Damage Reduction: <b>(\\d+)</b>" );

	public static String parseDamageReduction( String text )
	{
		Matcher matcher = DR_PATTERN.matcher( text );
		if (matcher.find() )
			return modifierTag( floatModifiers, DAMAGE_REDUCTION ) + ": " + matcher.group(1);

		return null;
	}

	private static final Pattern ALL_ATTR_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)$" );
	private static final Pattern ALL_ATTR_PCT_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)%$" );
	private static final Pattern CLASS_PATTERN = Pattern.compile( "Bonus for (.*) only" );
	private static final Pattern COMBAT_PATTERN = Pattern.compile( "Monsters will be (.*) attracted to you." );

	public static String parseModifier( String enchantment )
	{
		String result;

		// Search the float modifiers first

		result = parseModifier( floatModifiers, enchantment );
		if (result != null )
			return result;

		// Then the string modifiers

		result = parseModifier( stringModifiers, enchantment );
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
			String cls = "XX";
			if ( plural.equals( "Accordion Thieves" ) )
				cls = "AT";
			else if ( plural.equals( "Disco Bandits" ) )
				cls = "DB";
			else if ( plural.equals( "Pastamancers" ) )
				cls = "PA";
			else if ( plural.equals( "Saucerors" ) )
				cls = "SA";
			else if ( plural.equals( "Seal Clubbers" ) )
				cls = "SC";
			else if ( plural.equals( "Turtle Tamers" ) )
				cls = "TT";
			return modifierTag( stringModifiers, CLASS ) + ": " + cls;
		}

		matcher = COMBAT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return modifierTag( floatModifiers, COMBAT_RATE ) + ": " + ( matcher.group(1).equals( "more" ) ? "+5" : "-5" );

		if ( enchantment.indexOf( "Resistance" ) != -1 )
			return parseResistance( enchantment );

		return null;
	}

	private static String parseModifier( Object [][] table, String enchantment )
	{
		for ( int i = 0; i < table.length; ++i )
		{
			Pattern pattern = modifierDescPattern( table, i );
			if ( pattern == null )
				continue;

			Matcher matcher = pattern.matcher( enchantment );
			if ( matcher.find() )
				return modifierTag( table, i ) + ": " + matcher.group(1);
		}

		return null;
	}

	private static String parseResistance( String enchantment )
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
}
