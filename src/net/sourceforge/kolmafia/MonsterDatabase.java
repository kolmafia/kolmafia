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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class MonsterDatabase extends KoLDatabase
{
	private static final Map MONSTER_NAMES = new TreeMap();
	private static final Map MONSTER_DATA = new TreeMap();

	private static String [] MONSTER_STRINGS = null;

	// Elements
	public static final int NONE = 0;
	public static final int COLD = 1;
	public static final int HEAT = 2;
	public static final int SLEAZE = 3;
	public static final int SPOOKY = 4;
	public static final int STENCH = 5;

	public static final String [] elementNames =
	{
		"none", "cold", "hot", "sleaze", "spooky", "stench"
	};

	public static int elementNumber( String name )
	{
		for ( int i = 0; i < elementNames.length; ++i )
			if ( name.equals( elementNames[i] ) )
				return i;
		return -1;
	}

	static
	{
		refreshMonsterTable();
	}

	public static final void refreshMonsterTable()
	{
		MONSTER_DATA.clear();
		MONSTER_NAMES.clear();

		BufferedReader reader = getReader( "monsters.txt" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length >= 2 )
			{
				Monster monster = registerMonster( data[0], data[1] );
				if ( monster == null )
					continue;

				boolean bad = false;
				for ( int i = 2; i < data.length; ++i )
				{
					AdventureResult item = AdventureResult.parseResult( data[i] );
					if ( item != null )
					{
						monster.addItem( item );
						continue;
					}
					System.out.println( "Bad item for monster \"" + data[0] + "\": " + data[i] );
					bad = true;
				}

				if ( !bad )
				{
					monster.doneWithItems();
					MONSTER_DATA.put( data[0], monster );
					MONSTER_NAMES.put( CombatSettings.encounterKey( data[0], true ), data[0] );
				}
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

			printStackTrace( e );
		}
	}

	public static Monster findMonster( String name )
	{	return findMonster( name, true );
	}

	public static Monster findMonster( String name, boolean trySubstrings )
	{
		String keyName = CombatSettings.encounterKey( name, true );
		String realName = (String) MONSTER_NAMES.get( keyName );

		// If no monster with that name exists, maybe it's
		// one of those monsters with an alternate name.

		if ( realName != null )
			return (Monster) MONSTER_DATA.get( realName );

		if ( !trySubstrings )
			return null;

		if ( MONSTER_STRINGS == null )
		{
			MONSTER_STRINGS = new String[ MONSTER_NAMES.size() ];
			MONSTER_NAMES.keySet().toArray( MONSTER_STRINGS );
		}

		for ( int i = 0; i < MONSTER_STRINGS.length; ++i )
			if ( MONSTER_STRINGS[i].indexOf( keyName ) == 0 )
				return (Monster) MONSTER_DATA.get( MONSTER_NAMES.get( MONSTER_STRINGS[i] ) );

		for ( int i = 0; i < MONSTER_STRINGS.length; ++i )
			if ( substringMatches( MONSTER_STRINGS[i], keyName ) )
				return (Monster) MONSTER_DATA.get( MONSTER_NAMES.get( MONSTER_STRINGS[i] ) );

		return null;
	}

	public static Monster registerMonster( String name, String s )
	{
		Monster monster = findMonster( name, false );
		if ( monster != null )
			return monster;

		// parse parameters and make a new monster
		int health = 0;
		int attack = 0;
		int defense = 0;
		int initiative = 0;
		int minMeat = 0;
		int maxMeat = 0;
		int attackElement = NONE;
		int defenseElement = NONE;

		StringTokenizer tokens = new StringTokenizer( s, " " );
		while ( tokens.hasMoreTokens() )
		{
			String option = tokens.nextToken();
			String value;
			try
			{
				if ( option.equals( "HP:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						health = parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "Atk:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						attack = parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "Def:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						defense = parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "Init:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						initiative = parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "E:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int element = parseElement( value );
						if ( element != NONE )
						{
							attackElement = element;
							defenseElement = element;
							continue;
						}
					}
				}

				else if ( option.equals( "ED:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int element = parseElement( value );
						if ( element != NONE )
						{
							defenseElement = element;
							continue;
						}
					}
				}

				else if ( option.equals( "EA:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int element = parseElement( value );
						if ( element != NONE )
						{
							attackElement = element;
							continue;
						}
					}
				}

				else if ( option.equals( "Meat:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int dash = value.indexOf( "-" );
						if ( dash >= 0 )
						{
							minMeat = parseInt( value.substring( 0, dash ) );
							maxMeat = parseInt( value.substring( dash + 1 ) );
						}
						else
						{
							minMeat = parseInt( value );
							maxMeat = minMeat;
						}
						continue;
					}
				}

				System.out.println( "Monster: \"" + name + "\": unknown option: " + option );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				printStackTrace( e, s );
			}

			return null;
		}

		return new Monster( name, health, attack, defense, initiative, attackElement, defenseElement, minMeat, maxMeat );
	}

	private static int parseElement( String s )
	{
		if ( s.equals( "heat" ) )
			return HEAT;
		if ( s.equals( "cold" ) )
			return COLD;
		if ( s.equals( "stench" ) )
			return STENCH;
		if ( s.equals( "spooky" ) )
			return SPOOKY;
		if ( s.equals( "sleaze" ) )
			return SLEAZE;
		return NONE;
	}

	public static class Monster extends AdventureResult
	{
		private int health;
		private int attack;
		private int defense;
		private int initiative;
		private float statGain;
		private int attackElement;
		private int defenseElement;
		private int minMeat;
		private int maxMeat;

		private List items;
		private List pocketRates;

		public Monster( String name, int health, int attack, int defense, int initiative, int attackElement, int defenseElement, int minMeat, int maxMeat )
		{
			super( MONSTER_PRIORITY, name );

			this.health = health;
			this.attack = attack;
			this.defense = defense;
			this.initiative = initiative;
			this.statGain = ((attack + defense)) / 10.0f ;
			this.attackElement = attackElement;
			this.defenseElement = defenseElement;
			this.minMeat = minMeat;
			this.maxMeat = maxMeat;

			this.items = new ArrayList();
			this.pocketRates = new ArrayList();
		}

		public int getHP()
		{	return this.health;
		}

		public int getAdjustedHP( int ml )
		{	return this.health + ml;
		}

		public int getAttack()
		{	return this.attack;
		}

		public int getDefense()
		{	return this.defense;
		}

		public int getInitiative()
		{	return this.initiative;
		}

		public int getAttackElement()
		{	return this.attackElement;
		}

		public int getDefenseElement()
		{	return this.defenseElement;
		}

		public int getMinMeat()
		{	return this.minMeat;
		}

		public int getMaxMeat()
		{	return this.maxMeat;
		}

		public List getItems()
		{	return this.items;
		}

		public List getPocketRates()
		{	return this.pocketRates;
		}

		public boolean shouldSteal()
		{
			if ( !KoLCharacter.isMoxieClass() )
				return false;

			// If the player has an acceptable dodge rate or
			// then steal anything.

			if ( this.willUsuallyDodge( 0 ) )
				return this.shouldSteal( this.items );

			// Otherwise, only steal from monsters that drop
			// something on your conditions list.

			return this.shouldSteal( conditions );
		}

		private boolean shouldSteal( List checklist )
		{
			float dropModifier = AreaCombatData.getDropRateModifier();

			for ( int i = 0; i < checklist.size(); ++i )
				if ( this.shouldStealItem( (AdventureResult) checklist.get(i), dropModifier ) )
					return true;

			return false;
		}

		private boolean shouldStealItem( AdventureResult item, float dropModifier )
		{
			if ( !item.isItem() )
				return false;

			int itemIndex = this.items.indexOf( item );

			// If the monster drops this item, then return true
			// when the drop rate is less than 100%.

			if ( itemIndex != -1 )
			{
				item = (AdventureResult) this.items.get( itemIndex );
				return (item.getCount()) * dropModifier < 100.0f;
			}

			// If the item does not drop, check to see if maybe
			// the monster drops one of its ingredients.

			AdventureResult [] subitems = ConcoctionsDatabase.getStandardIngredients( item.getItemId() );
			if ( subitems.length < 2 )
				return false;

			for ( int i = 0; i < subitems.length; ++i )
				if ( this.shouldStealItem( subitems[i], dropModifier ) )
					return true;

			// The item doesn't drop the item or any of its
			// ingredients.

			return false;
		}

		public void addItem( AdventureResult item )
		{	this.items.add( item );
		}

		public void doneWithItems()
		{
			// Calculate the probability that an item will be yoinked
			// based on the integral provided by Buttons on the HCO forums.
			// http://forums.hardcoreoxygenation.com/viewtopic.php?t=3396

			float probability = 0.0f;
			float [] coefficients = new float[ this.items.size() ];

			for ( int i = 0; i < this.items.size(); ++i )
			{
				coefficients[0] = 1.0f;
				for ( int j = 1; j < coefficients.length; ++j )
					coefficients[j] = 0.0f;

				for ( int j = 0; j < this.items.size(); ++j )
				{
					probability = ((AdventureResult)this.items.get(j)).getCount() / 100.0f;

					if ( i == j )
					{
						for ( int k = 0; k < coefficients.length; ++k )
							coefficients[k] = coefficients[k] * probability;
					}
					else
					{
						for ( int k = coefficients.length - 1; k >= 1; --k )
							coefficients[k] = coefficients[k] - (probability * coefficients[k - 1]);
					}
				}

				probability = 0.0f;

				for ( int j = 0; j < coefficients.length; ++j )
					probability += coefficients[j] / ((j+1));

				this.pocketRates.add( new Float( probability ) );
			}
		}

		public float getExperience()
		{	return Math.max( 1.0f, this.statGain );
		}

		public float getAdjustedExperience( float modifier, int ml, FamiliarData familiar )
		{
			// Base stat gain = attack / 5 (not average of attack and defense)
			// Add constant stat gain from items, effects, and familiars
			// Add variable stat gain from familiars

			this.statGain = this.attack / 4.0f + modifier + sombreroAdjustment( this.attack + ml, familiar );
			return Math.max( 1.0f, this.statGain );
		}

		private static final int SOMBRERO = 18;

		private static float sombreroAdjustment( float ml, FamiliarData familiar )
		{
			if ( familiar.getId() != SOMBRERO )
				return 0.0f;

			// ( sqrt(ML) * weight * 3 ) / 100
			return (float) Math.round( Math.sqrt( ml - 4.0f ) * familiar.getModifiedWeight() / 100.0f );
		}

		public boolean willUsuallyMiss()
		{	return this.willUsuallyMiss( 0 );
		}

		public boolean willUsuallyDodge( int offenseModifier )
		{
			int ml = KoLCharacter.getMonsterLevelAdjustment() + offenseModifier;
			int dodgeRate = KoLCharacter.getAdjustedMoxie() - (this.attack + ml) - 6;
			return dodgeRate > 0;
		}

		public boolean willUsuallyMiss( int defenseModifier )
		{
			int ml = KoLCharacter.getMonsterLevelAdjustment() + defenseModifier;
			int hitStat = KoLCharacter.getAdjustedHitStat();

			return AreaCombatData.hitPercent( hitStat - ml, this.defense ) <= 50.0f;
		}
	}
}
