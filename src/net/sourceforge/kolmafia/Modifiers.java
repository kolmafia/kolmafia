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
import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Modifiers extends KoLDatabase
{
	private static Map modifierMap = new TreeMap();
	private static ArrayList modifierSkills = new ArrayList();

	static
	{
		BufferedReader reader = getReader( "modifiers.txt" );
		String [] data;

		while ( (data = readData( reader )) != null )
			if ( data.length == 2 )
				modifierMap.put( getCanonicalName( data[0] ), data[1] );

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

	public static final int FAMILIAR_WEIGHT_MODIFIER = 0;
	public static final int MONSTER_LEVEL_MODIFIER = 1;
	public static final int COMBAT_RATE_MODIFIER = 2;
	public static final int INITIATIVE_MODIFIER = 3;
	public static final int EXPERIENCE_MODIFIER = 4;
	public static final int ITEMDROP_MODIFIER = 5;
	public static final int MEATDROP_MODIFIER = 6;
	public static final int DAMAGE_ABSORPTION_MODIFIER = 7;
	public static final int DAMAGE_REDUCTION_MODIFIER = 8;
	public static final int COLD_RESISTANCE_MODIFIER = 9;
	public static final int HOT_RESISTANCE_MODIFIER = 10;
	public static final int SLEAZE_RESISTANCE_MODIFIER = 11;
	public static final int SPOOKY_RESISTANCE_MODIFIER = 12;
	public static final int STENCH_RESISTANCE_MODIFIER = 13;
	public static final int MANA_COST_MODIFIER = 14;
	public static final int MOX_MODIFIER = 15;
	public static final int MOX_PCT_MODIFIER = 16;
	public static final int MUS_MODIFIER = 17;
	public static final int MUS_PCT_MODIFIER = 18;
	public static final int MYS_MODIFIER = 19;
	public static final int MYS_PCT_MODIFIER = 20;

	public static final int MODIFIERS = 21;

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

	private static final Pattern [] STRING_MODIFIER_PATTERNS = new Pattern [] {
		Pattern.compile( "Class: (\\w\\w)" ),
		Pattern.compile( "Intrinsic: [^,]+" ),
	};

	private static final Modifiers NO_MODIFIERS = new Modifiers();

        private float[] modifiers;

	public Modifiers()
	{
                this.modifiers = new float[ MODIFIERS];
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

	public static void applyPassiveModifiers( Modifiers mods )
	{
		if ( modifierSkills.isEmpty() )
		{
			Object [] keys = modifierMap.keySet().toArray();
			for ( int i = 0; i < keys.length; ++i )
			{
				if ( !ClassSkillsDatabase.contains( (String) keys[i] ) )
					continue;

				int skillId = ClassSkillsDatabase.getSkillId( (String) keys[i] );
				if ( ClassSkillsDatabase.getSkillType( skillId ) == ClassSkillsDatabase.PASSIVE )
					modifierSkills.add( keys[i] );

			}
		}

		for ( int i = 0; i < modifierSkills.size(); ++i )
		{
			String skill = (String) modifierSkills.get(i);
			if ( KoLCharacter.hasSkill( skill ) )
				mods.add( getModifiers( skill ) );
		}

		// Varies according to level, somehow

		if ( KoLCharacter.hasSkill( "Skin of the Leatherback" ) )
			mods.add( DAMAGE_REDUCTION_MODIFIER, Math.max( (KoLCharacter.getLevel() >> 1) - 1, 1 ) );

		if ( KoLCharacter.getFamiliar().getId() == 38 && KoLCharacter.hasAmphibianSympathy() )
			mods.add( FAMILIAR_WEIGHT_MODIFIER, -10 );
	}
}
