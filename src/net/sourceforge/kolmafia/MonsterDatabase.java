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
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class MonsterDatabase extends KoLDatabase
{
	private static final Map MONSTER_NAMES = new TreeMap();
	private static final Map MONSTER_DATA = new TreeMap();

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
					MONSTER_DATA.put( data[0], monster );
					MONSTER_NAMES.put( CombatSettings.encounterKey( data[0] ), data[0] );
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
		String keyName = CombatSettings.encounterKey( name );
		String realName = (String) MONSTER_NAMES.get( keyName );

		// If no monster with that name exists, maybe it's
		// one of those monsters with an alternate name.

		if ( realName == null && trySubstrings )
		{
			Object [] possibleNames = MONSTER_NAMES.keySet().toArray();
			for ( int i = 0; realName == null && i < possibleNames.length; ++i )
				if ( ((String)possibleNames[i]).indexOf( keyName ) != -1 )
					realName = (String) MONSTER_NAMES.get( possibleNames[i] );
		}

		return realName == null ? null : (Monster) MONSTER_DATA.get( realName );
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

	public static class Monster
	{
		private String name;
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

		public Monster( String name, int health, int attack, int defense, int initiative, int attackElement, int defenseElement, int minMeat, int maxMeat )
		{
			this.name = name;
			this.health = health;
			this.attack = attack;
			this.defense = defense;
			this.initiative = initiative;
			this.statGain = ((float) (attack + defense)) / 10.0f ;
			this.attackElement = attackElement;
			this.defenseElement = defenseElement;
			this.minMeat = minMeat;
			this.maxMeat = maxMeat;
			this.items = new ArrayList();
		}

		public String getName()
		{	return name;
		}

		public int getHP()
		{	return health;
		}

		public int getAdjustedHP( int ml )
		{	return health + ml;
		}

		public int getAttack()
		{	return attack;
		}

		public int getDefense()
		{	return defense;
		}

		public int getInitiative()
		{	return initiative;
		}

		public int getAttackElement()
		{	return attackElement;
		}

		public int getDefenseElement()
		{	return defenseElement;
		}

		public int getMinMeat()
		{	return minMeat;
		}

		public int getMaxMeat()
		{	return maxMeat;
		}

		public List getItems()
		{	return items;
		}

		public boolean shouldSteal()
		{
			if ( !KoLCharacter.isMoxieClass() )
				return false;

			// If the player has an acceptable dodge rate, then steal anything.
			// Otherwise, only steal from monsters that are dropping something
			// on your conditions list if the applicable setting is present.

			if ( hasAcceptableDodgeRate( 0 ) )
				return shouldSteal( items );

			if ( !StaticEntity.getBooleanProperty( "autoRoninPickpocket" ) )
				return false;

			return shouldSteal( conditions );
		}

		private boolean shouldSteal( List checklist )
		{
			float dropModifier = AreaCombatData.getDropRateModifier();

			for ( int i = 0; i < checklist.size(); ++i )
				if ( shouldStealItem( (AdventureResult) checklist.get(i), dropModifier ) )
					return true;

			return false;
		}

		private boolean shouldStealItem( AdventureResult item, float dropModifier )
		{
			int itemIndex = items.indexOf( item );

			// If the monster drops this item, then return true
			// when the drop rate is less than 100%.

			if ( itemIndex != -1 )
			{
				item = (AdventureResult) items.get( itemIndex );
				return ((float)item.getCount()) * dropModifier < 100.0f;
			}

			// If the item does not drop, check to see if maybe
			// the monster drops one of its ingredients.

			AdventureResult [] subitems = ConcoctionsDatabase.getStandardIngredients( item.getItemId() );
			if ( subitems.length < 2 )
				return false;

			for ( int i = 0; i < subitems.length; ++i )
				if ( shouldStealItem( subitems[i], dropModifier ) )
					return true;

			// The item doesn't drop the item or any of its
			// ingredients.

			return false;
		}

		public void addItem( AdventureResult item )
		{	items.add( item );
		}

		public float getXP()
		{	return Math.max( 1.0f, statGain );
		}

		public float getAdjustedXP( float modifier, int ml, FamiliarData familiar )
		{
			// +1 ML adds +1 health, +1 Attack, +1 Defense
			// Monster statGain = ( attack + defense ) / 10
			float adjustedML = ((float) (attack + ml + defense + ml)) / 2.0f;
			statGain =  adjustedML / 5.0f;
			// Add constant statGain from items, effects, and familiars
			statGain += modifier;
			// Add variable statGain from familiars
			statGain += sombreroXPAdjustment( adjustedML, familiar );
			return Math.max( 1.0f, statGain );
		}

		private static final int SOMBRERO = 18;
		private static final float sombreroFactor = 3.0f / 100.0f;

		public static float sombreroXPAdjustment( float ml, FamiliarData familiar )
		{
			if ( familiar.getId() != SOMBRERO )
				return 0.0f;

			// ( sqrt(ML) * weight * 3 ) / 100
			return ((float) Math.sqrt( ml )) * (float)familiar.getModifiedWeight() * sombreroFactor;
		}

		public boolean willAlwaysMiss()
		{	return willAlwaysMiss( 0 );
		}

		public boolean hasAcceptableDodgeRate( int offenseModifier )
		{
			int ml = KoLCharacter.getMonsterLevelAdjustment() + offenseModifier;
			int dodgeRate = KoLCharacter.getAdjustedMoxie() - (attack + ml) - 6;
			return dodgeRate > 0;
		}

		public boolean willAlwaysMiss( int defenseModifier )
		{
			int ml = KoLCharacter.getMonsterLevelAdjustment() + defenseModifier;
			int hitstat;

			if ( KoLCharacter.rangedWeapon() )
				hitstat = KoLCharacter.getAdjustedMoxie() - ml;
			else if ( KoLCharacter.rigatoniActive() )
				hitstat = KoLCharacter.getAdjustedMysticality() - ml;
			else
				hitstat = KoLCharacter.getAdjustedMuscle() - ml;

			return AreaCombatData.hitPercent( hitstat, defense ) <= 0.0f;
		}
	}
}
