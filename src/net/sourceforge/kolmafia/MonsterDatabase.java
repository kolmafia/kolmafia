/**
 * Copyright (c) 2006, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.util.Map;
import java.util.TreeMap;
import java.util.StringTokenizer;

import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;

public class MonsterDatabase extends KoLDatabase
{

	public static final Map MONSTERS = new TreeMap();

	// Elements
	public static final int NONE = 0;
	public static final int HEAT = 1;
	public static final int COLD = 2;
	public static final int STENCH = 3;
	public static final int SPOOKY = 4;
	public static final int SLEAZE = 5;

	static
	{
		refreshMonsterTable();
	}

	public static final void refreshMonsterTable()
	{
		MONSTERS.clear();

		BufferedReader reader = getReader( "monsters.dat" );
		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 2 )
			{
				Monster monster = registerMonster( data[0], data[1] );
				if ( monster != null )
					MONSTERS.put( data[0], monster );
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

	public static Monster findMonster ( String name )
	{	return (Monster)MONSTERS.get( name );
	}

	public static Monster registerMonster ( String name, String s )
	{
		Monster monster = findMonster( name );
		if ( monster != null )
			return monster;

		// parse parameters and make a new monster
		int HP = 0;
		double XP = 0;
		int attack = 0;
		int defense = 0;
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
						HP = Integer.parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "XP:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						XP = Double.parseDouble( value );
						continue;
					}
				}

				else if ( option.equals( "Atk:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						attack = Integer.parseInt( value );
						continue;
					}
				}

				else if ( option.equals( "Def:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						defense = Integer.parseInt( value );
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

				System.out.println( "Monster: \"" + name + "\": unknown option: " + option );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e, s );
			}

			return null;
		}

		return new Monster( name, HP, XP, attack, defense, attackElement, defenseElement );
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
		private int HP;
		private double XP;
		private int attack;
		private int defense;
		private int attackElement;
		private int defenseElement;

		public Monster( String name, int HP, double XP, int attack, int defense, int attackElement, int defenseElement )
		{
			this.name = name;
			this.HP = HP;
			this.XP = XP;
			this.attack = attack;
			this.defense = defense;
			this.attackElement = attackElement;
			this.defenseElement = defenseElement;
		}

		public String getName()
		{	return name;
		}

		public int getHP()
		{	return HP;
		}

		public double getXP()
		{	return XP;
		}

		public int getAttack()
		{	return attack;
		}

		public int getDefense()
		{	return defense;
		}

		public int getAttackElement()
		{	return attackElement;
		}

		public int getDefenseElement()
		{	return defenseElement;
		}
	}
}
