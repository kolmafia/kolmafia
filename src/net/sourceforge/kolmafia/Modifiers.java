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

	public static final int MODIFIER_COUNT = 21;

	private static final Pattern [] MODIFIER_PATTERNS = new Pattern [] {
		Pattern.compile( "Weight: ([+-]\\d+)" ),
		Pattern.compile( "ML: ([+-]\\d+)" ),
		Pattern.compile( "Combat: ([+-][\\d.]+)" ),
		Pattern.compile( "Init: ([+-][\\d.]+)" ),
		Pattern.compile( "Exp: ([+-][\\d.]+)" ),
		Pattern.compile( "Item: ([+-][\\d.]+)" ),
		Pattern.compile( "Meat: ([+-][\\d.]+)" ),
		Pattern.compile( "DA: ([+-]\\d+)" ),
		Pattern.compile( "DR: (\\d+)" ),
		Pattern.compile( "Cold: ([+-]\\d+)" ),
		Pattern.compile( "Hot: ([+-]\\d+)" ),
		Pattern.compile( "Sleaze: ([+-]\\d+)" ),
		Pattern.compile( "Spooky: ([+-]\\d+)" ),
		Pattern.compile( "Stench: ([+-]\\d+)" ),
		Pattern.compile( "Mana: ([+-]\\d+)" ),
		Pattern.compile( "Mox: ([+-]\\d+)" ),
		Pattern.compile( "Mox%: ([+-]\\d+)" ),
		Pattern.compile( "Mus: ([+-]\\d+)" ),
		Pattern.compile( "Mus%: ([+-]\\d+)" ),
		Pattern.compile( "Mys: ([+-]\\d+)" ),
		Pattern.compile( "Mys%: ([+-]\\d+)" ),
	};

	private static final Pattern [] STRING_PATTERNS = new Pattern [] {
		Pattern.compile( "Class: (\\w\\w)" ),
		Pattern.compile( "Intrinsic: [^,]+" ),
	};

        private float[] modifiers;

	public Modifiers()
	{
		this.modifiers = new float[ MODIFIER_COUNT ];
		reset();
	};

        public void reset()
	{
                for ( int i = 0; i < this.modifiers.length; ++i )
                        this.modifiers[i] = 0.0f;
	};

	public float get( int index )
	{
		if ( index < 0 || index >= this.modifiers.length )
			return 0.0f;

		return this.modifiers[index];
	};

	public boolean set( int index, double mod )
	{
		if ( index < 0 || index >= this.modifiers.length )
			return false;

		if ( this.modifiers[index] != mod )
		{
			this.modifiers[index] = (float)mod;
			return true;
		}
		return false;
	};

	public boolean set( Modifiers mods )
	{
		if ( mods == null )
			return false;

		float [] copy = mods.modifiers;
		boolean changed = false;

		for ( int index = 0; index < this.modifiers.length; ++index )
			if ( this.modifiers[index] != copy[index] )
			{
				this.modifiers[index] = copy[index];
				changed = true;
			}

		return changed;
	}

	public void add( int index, double mod )
	{
		if ( index < 0 || index >= this.modifiers.length )
			return;
		this.modifiers[index] += mod;
	};

	public void add( Modifiers mods )
	{
		if ( mods == null )
			return;

		float [] addition = mods.modifiers;

		for ( int i = 0; i < this.modifiers.length; ++i )
			if ( addition[i] != 0.0f )
				this.modifiers[i] += addition[i];
	}

	public static final Modifiers getModifiers( String name )
	{
		if ( name == null )
			return null;

		name = getCanonicalName( name );
		Object modifier = modifierMap.get( name );

		if ( modifier == null )
			return null;

		if ( modifier instanceof Modifiers )
			return (Modifiers)modifier;

		if ( !( modifier instanceof String ) )
			return null;

		Modifiers newMods = new Modifiers();
		float [] newModifiers = newMods.modifiers;

		for ( int i = 0; i < newModifiers.length; ++i )
		{
			Matcher effectMatcher = MODIFIER_PATTERNS[ i ].matcher( (String) modifier );
			newModifiers[i] = effectMatcher.find() ? Float.parseFloat( effectMatcher.group(1) ) : 0.0f;
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
			return "DR: " + matcher.group(1);

		return null;
	}

	private static final Pattern ALL_ATTR_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)$" );
	private static final Pattern ALL_ATTR_PCT_PATTERN = Pattern.compile( "^All Attributes ([+-]\\d+)%$" );
	private static final Pattern CLASS_PATTERN = Pattern.compile( "Bonus for (.*) only" );
	private static final Pattern COMBAT_PATTERN = Pattern.compile( "Monsters will be (.*) attracted to you." );
	private static final Pattern DA_PATTERN = Pattern.compile( "Damage Absorption (.*)" );
	private static final Pattern DR2_PATTERN = Pattern.compile( "Damage Reduction: (\\d+)" );
	private static final Pattern EXP_PATTERN = Pattern.compile( "(.*) Stat.*Per Fight" );
	private static final Pattern INIT_PATTERN = Pattern.compile( "Combat Initiative (.*)%" );
	private static final Pattern INTRINSIC_PATTERN = Pattern.compile( "Intrinsic effect: (.*)" );
	private static final Pattern ITEM_PATTERN = Pattern.compile( "(.*)% Item Drops from Monsters" );
	private static final Pattern MEAT_PATTERN = Pattern.compile( "(.*)% Meat from Monsters" );
	private static final Pattern ML_PATTERN = Pattern.compile( "(.*) to Monster Level" );
	private static final Pattern MOX_PATTERN = Pattern.compile( "Moxie ([+-]\\d+)$" );
	private static final Pattern MOX_PCT_PATTERN = Pattern.compile( "Moxie ([+-]\\d+)%" );
	private static final Pattern MUS_PATTERN = Pattern.compile( "Muscle ([+-]\\d+)$" );
	private static final Pattern MUS_PCT_PATTERN = Pattern.compile( "Muscle ([+-]\\d+)%" );
	private static final Pattern MYS_PATTERN = Pattern.compile( "Mysticality ([+-]\\d+)$" );
	private static final Pattern MYS_PCT_PATTERN = Pattern.compile( "Mysticality ([+-]\\d+)%" );
	private static final Pattern MP_PATTERN = Pattern.compile( "(.*) MP to use Skills" );
	private static final Pattern WEIGHT_PATTERN = Pattern.compile( "(.*) to Familiar Weight" );

	public static String parseModifier( String enchantment )
	{
		Matcher matcher;

		matcher = ALL_ATTR_PATTERN.matcher( enchantment );
		if ( matcher.find() )
                {
                        String mod = matcher.group(1);
			return "Mox: " + mod + ", Mus: " + mod + ", Mys: " + mod;
                }

		matcher = ALL_ATTR_PCT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
                {
                        String mod = matcher.group(1);
			return "Mox%: " + mod + ", Mus%: " + mod + ", Mys%: " + mod;
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
			return "Class: " + cls;
		}

		matcher = COMBAT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Combat: " + ( matcher.group(1).equals( "more" ) ? "+5" : "-5" );

		matcher = DA_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "DA: " + matcher.group(1);

		matcher = DR2_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "DR: " + matcher.group(1);

		matcher = EXP_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Exp: " + matcher.group(1);

		matcher = INIT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Init: " + matcher.group(1);

		matcher = INTRINSIC_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Intrinsic: " + matcher.group(1);

		matcher = ITEM_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Item: " + matcher.group(1);

		matcher = MEAT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Meat: " + matcher.group(1);

		matcher = MP_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Mana: " + matcher.group(1);

		matcher = ML_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "ML: " + matcher.group(1);

		matcher = MOX_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Mox: " + matcher.group(1);

		matcher = MOX_PCT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Mox%: " + matcher.group(1);

		matcher = MUS_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Mus: " + matcher.group(1);

		matcher = MUS_PCT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Mus%: " + matcher.group(1);

		matcher = MYS_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Mys: " + matcher.group(1);

		matcher = MYS_PCT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Mys%: " + matcher.group(1);

		matcher = WEIGHT_PATTERN.matcher( enchantment );
		if ( matcher.find() )
			return "Weight: " + matcher.group(1);

		if ( enchantment.indexOf( "Resistance" ) != -1 )
			return parseResistance( enchantment );

		return null;
	}

	private static String parseResistance( String enchantment )
	{
		int level = 0;

		if ( enchantment.indexOf( "Slight" ) != -1 )
			level = 10;
		else if ( enchantment.indexOf( "So-So" ) != -1 )
			level = 20;
		else if ( enchantment.indexOf( "Serious" ) != -1 )
			level = 30;
		else if ( enchantment.indexOf( "Superhuman" ) != -1 )
			level = 40;

		if ( enchantment.indexOf( "All Elements" ) != -1 )
			return "Cold: +" + level + ", Hot: +" + level + ", Sleaze: +" + level + ", Spooky: +" + level + ", Stench: +" + level;

		if ( enchantment.indexOf( "Cold" ) != -1 )
			return "Cold: +" + level;

		if ( enchantment.indexOf( "Hot" ) != -1 )
			return "Hot: +" + level;

		if ( enchantment.indexOf( "Sleaze" ) != -1 )
			return "Sleaze: +" + level;

		if ( enchantment.indexOf( "Spooky" ) != -1 )
			return "Spooky: +" + level;

		if ( enchantment.indexOf( "Stench" ) != -1 )
			return "Stench: +" + level;

		return null;
	}
}
