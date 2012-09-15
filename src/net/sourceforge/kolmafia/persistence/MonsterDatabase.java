/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.combat.CombatActionManager;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterDatabase
{
	private static final Map<String, MonsterData> MONSTER_DATA = new TreeMap<String, MonsterData>();
	private static String[] MONSTER_STRINGS = null;

	// Elements
	public static final int NONE = 0;
	public static final int COLD = 1;
	public static final int HEAT = 2;
	public static final int SLEAZE = 3;
	public static final int SPOOKY = 4;
	public static final int STENCH = 5;
	public static final int SLIME = 6;

	public static final String[] elementNames =
	{
		"none",
		"cold",
		"hot",
		"sleaze",
		"spooky",
		"stench",
		"slime"
	};

	// Phila
	public static final int BEAST = 1;
	public static final int BUG = 2;
	public static final int CONSTELLATION = 3;
	public static final int CRIMBO  = 4;
	public static final int DEMIHUMAN = 5;
	public static final int DEMON = 6;
	public static final int ELEMENTAL = 7;
	public static final int FISH = 8;
	public static final int GOBLIN = 9;
	public static final int HIPPY = 10;
	public static final int HOBO = 11;
	public static final int HUMANOID = 12;
	public static final int HORROR = 13;
	public static final int MER_KIN = 14;
	public static final int OBJECT = 15;
	public static final int ORC = 16;
	public static final int PENGUIN = 17;
	public static final int PIRATE = 18;
	public static final int PLANT = 19;
	public static final int SLIMES = 20;
	public static final int STRANGE = 21;
	public static final int UNDEAD = 22;

	public static final String[] phylumNames =
	{
		"none",
		"beast",
		"bug",
		"constellation",
		"crimbo",
		"demihuman",
		"demon",
		"elemental",
		"fish",
		"goblin",
		"hippy",
		"hobo",
		"humanoid",
		"horror",
		"mer-kin",
		"object",
		"orc",
		"penguin",
		"pirate",
		"plant",
		"slime",
		"strange",
		"undead",
	};

	public static final int elementNumber( final String name )
	{
		for ( int i = 0; i < MonsterDatabase.elementNames.length; ++i )
		{
			if ( name.equals( MonsterDatabase.elementNames[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	public static final int phylumNumber( final String name )
	{
		for ( int i = 0; i < MonsterDatabase.phylumNames.length; ++i )
		{
			if ( name.equals( MonsterDatabase.phylumNames[ i ] ) )
			{
				return i;
			}
		}
		return -1;
	}

	public static final boolean elementalVulnerability( final int element1, final int element2 )
	{
		switch ( element1 )
		{
		case COLD:
			return element2 == MonsterDatabase.HEAT || element2 == MonsterDatabase.SPOOKY;
		case HEAT:
			return element2 == MonsterDatabase.SLEAZE || element2 == MonsterDatabase.STENCH;
		case SLEAZE:
			return element2 == MonsterDatabase.COLD || element2 == MonsterDatabase.SPOOKY;
		case SPOOKY:
			return element2 == MonsterDatabase.HEAT || element2 == MonsterDatabase.STENCH;
		case STENCH:
			return element2 == MonsterDatabase.SLEAZE || element2 == MonsterDatabase.COLD;
		}
		return false;
	}

	static
	{
		MonsterDatabase.refreshMonsterTable();
	}

	public static final void refreshMonsterTable()
	{
		MonsterDatabase.MONSTER_DATA.clear();

		BufferedReader reader = FileUtilities.getVersionedReader( "monsters.txt", KoLConstants.MONSTERS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			MonsterData monster = null;

			if ( data.length >= 2 )
			{
				monster = MonsterDatabase.registerMonster( data[ 0 ], data[ 1 ] );
			}

			if ( monster == null )
			{
				continue;
			}

			boolean bogus = false;

			for ( int i = 2; i < data.length; ++i )
			{
				AdventureResult item = MonsterDatabase.parseItem( data[ i ] );
				if ( item == null || item.getItemId() == -1 || item.getName() == null )
				{
					RequestLogger.printLine( "Bad item for monster \"" + data[ 0 ] + "\": " + data[ i ] );
					bogus = true;
					continue;
				}

				monster.addItem( item );
			}

			if ( !bogus )
			{
				monster.doneWithItems();
				String keyName = CombatActionManager.encounterKey( data[ 0 ], true );
				StringUtilities.registerPrepositions( keyName );
				MonsterDatabase.MONSTER_DATA.put( keyName, monster );
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

	private static final AdventureResult parseItem( final String data )
	{
		String name = data;
		int count = 0;
		String countString;
		char prefix = '0';

		// Remove quantity and flag
		if ( name.endsWith( ")" ) )
		{
			int left = name.lastIndexOf( " (" );

			countString = name.substring( left + 2, name.length() - 1 );

			if ( !Character.isDigit( countString.charAt( 0 ) ) )
			{
				countString = countString.substring( 1 );
			}

			count =	 StringUtilities.parseInt( countString );
			prefix = name.charAt( left + 2 );
			name = name.substring( 0, left );
		}

		// Convert item numbers to names
		if ( name.startsWith( "[" ) )
		{
			int end = name.indexOf( "]" );
			int itemId = StringUtilities.parseInt( name.substring( 1, end ) );
			name = ItemDatabase.getItemName( itemId );
		}
		else if ( ItemDatabase.getItemId( name, 1 ) == -1 )
		{
			return new AdventureResult( data, (int)'0' );
		}

		return new AdventureResult( name, (count << 16) | prefix );
	}

	public static final MonsterData findMonster( final String name, boolean trySubstrings )
	{
		String keyName = CombatActionManager.encounterKey( name, true );
		MonsterData match = (MonsterData) MonsterDatabase.MONSTER_DATA.get( keyName );

		// If no monster with that name exists, maybe it's
		// one of those monsters with an alternate name.

		if ( match != null )
		{
			return match;
		}

		if ( !trySubstrings )
		{
			return null;
		}

		if ( MonsterDatabase.MONSTER_STRINGS == null )
		{
			MonsterDatabase.MONSTER_STRINGS = new String[ MonsterDatabase.MONSTER_DATA.size() ];
			MonsterDatabase.MONSTER_DATA.keySet().toArray( MonsterDatabase.MONSTER_STRINGS );
		}

		List<String> matchingNames = StringUtilities.getMatchingNames( MonsterDatabase.MONSTER_STRINGS, keyName );

		if ( matchingNames.size() != 1 )
		{
			return null;
		}

		return (MonsterData) MonsterDatabase.MONSTER_DATA.get( matchingNames.get( 0 ) );
	}

	// Register an unknown monster
	public static final MonsterData registerMonster( final String name )
	{
		MonsterData monster = MonsterDatabase.registerMonster( name, "" );
		MonsterDatabase.MONSTER_DATA.put( name, monster );
		return monster;
	}

	public static final Set entrySet()
	{
		return MonsterDatabase.MONSTER_DATA.entrySet();
	}

	public static final MonsterData registerMonster( final String name, final String s )
	{
		MonsterData monster = MonsterDatabase.findMonster( name, false );
		if ( monster != null )
		{
			return monster;
		}

		// parse parameters and make a new monster
		Object health = null;
		Object attack = null;
		Object defense = null;
		Object initiative = null;
		Object experience = null;
		int minMeat = 0;
		int maxMeat = 0;
		int attackElement = MonsterDatabase.NONE;
		int defenseElement = MonsterDatabase.NONE;
		int phylum = MonsterDatabase.NONE;
		int poison = Integer.MAX_VALUE;
		boolean boss = false;

		StringTokenizer tokens = new StringTokenizer( s, " " );
		while ( tokens.hasMoreTokens() )
		{
			String option = tokens.nextToken();
			String value;
			try
			{
				if ( option.equals( "HP:" ) )
				{
					health = parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Atk:" ) )
				{
					attack = parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Def:" ) )
				{
					defense = parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Init:" ) )
				{
					initiative = parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Exp:" ) )
				{
					experience = parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Phys:" ) )
				{
					/* physical = */ parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Item:" ) )
				{
					/* itemBlock = */ parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Skill:" ) )
				{
					/* skillBlock = */ parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Spell:" ) )
				{
					/* spellBlock = */ parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "E:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int element = MonsterDatabase.parseElement( value );
						if ( element != MonsterDatabase.NONE )
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
						int element = MonsterDatabase.parseElement( value );
						if ( element != MonsterDatabase.NONE )
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
						int element = MonsterDatabase.parseElement( value );
						if ( element != MonsterDatabase.NONE )
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
							minMeat = StringUtilities.parseInt( value.substring( 0, dash ) );
							maxMeat = StringUtilities.parseInt( value.substring( dash + 1 ) );
						}
						else
						{
							minMeat = StringUtilities.parseInt( value );
							maxMeat = minMeat;
						}
						continue;
					}
				}

				else if ( option.equals( "P:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						int num = MonsterDatabase.parsePhylum( value );
						if ( num != MonsterDatabase.NONE )
						{
							phylum = num;
							continue;
						}
					}
				}

				else if ( option.startsWith( "\"" ) )
				{
					StringBuffer temp = new StringBuffer( option );
					while ( !option.endsWith( "\"" ) && tokens.hasMoreTokens() )
					{
						option = tokens.nextToken();
						temp.append( ' ' );
						temp.append( option );
					}
					poison = EffectDatabase.getPoisonLevel( temp.toString() );
					if ( poison == Integer.MAX_VALUE )
					{
						RequestLogger.printLine( "Monster: \"" + name + "\": unknown poison type: " + temp );
					}
					continue;
				}

				else if ( option.equals( "BOSS" ) )
				{
					boss = true;
					continue;
				}

				RequestLogger.printLine( "Monster: \"" + name + "\": unknown option: " + option );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, s );
			}

			return null;
		}

		return new MonsterData( name, health, attack, defense, initiative, experience,
			attackElement, defenseElement, minMeat, maxMeat, phylum, poison, boss );
	}

	private static final Object parseNumeric( StringTokenizer tokens )
	{
		if ( !tokens.hasMoreTokens() )
		{
			return null;
		}
		String value = tokens.nextToken();
		if ( !value.startsWith( "[" ) )
		{
			return IntegerPool.get( StringUtilities.parseInt( value ) );
		}
		// Must paste the entire expression back together, since we're
		// splitting the tokens on spaces.
		StringBuilder temp = new StringBuilder( value );
		while ( !value.endsWith( "]" ) && tokens.hasMoreTokens() )
		{
			value = tokens.nextToken();
			temp.append( ' ' );
			temp.append( value );
		}
		return temp.substring( 1, temp.length() - 1 );
	}

	private static final int parseElement( final String s )
	{
		if ( s.equals( "heat" ) )
		{
			return MonsterDatabase.HEAT;
		}
		if ( s.equals( "cold" ) )
		{
			return MonsterDatabase.COLD;
		}
		if ( s.equals( "stench" ) )
		{
			return MonsterDatabase.STENCH;
		}
		if ( s.equals( "spooky" ) )
		{
			return MonsterDatabase.SPOOKY;
		}
		if ( s.equals( "sleaze" ) )
		{
			return MonsterDatabase.SLEAZE;
		}
		if ( s.equals( "slime" ) )
		{
			return MonsterDatabase.SLIME;
		}
		return MonsterDatabase.NONE;
	}

	private static final int parsePhylum( final String s )
	{
		if ( s.equals( "beast" ) )
		{
			return MonsterDatabase.BEAST;
		}
		if ( s.equals( "bug" ) )
		{
			return MonsterDatabase.BUG;
		}
		if ( s.equals( "constellation" ) )
		{
			return MonsterDatabase.CONSTELLATION;
		}
		if ( s.equals( "crimbo" ) )
		{
			return MonsterDatabase.CRIMBO ;
		}
		if ( s.equals( "demihuman" ) )
		{
			return MonsterDatabase.DEMIHUMAN;
		}
		if ( s.equals( "demon" ) )
		{
			return MonsterDatabase.DEMON;
		}
		if ( s.equals( "elemental" ) )
		{
			return MonsterDatabase.ELEMENTAL;
		}
		if ( s.equals( "fish" ) )
		{
			return MonsterDatabase.FISH;
		}
		if ( s.equals( "goblin" ) )
		{
			return MonsterDatabase.GOBLIN;
		}
		if ( s.equals( "hippy" ) )
		{
			return MonsterDatabase.HIPPY;
		}
		if ( s.equals( "hobo" ) )
		{
			return MonsterDatabase.HOBO;
		}
		if ( s.equals( "humanoid" ) )
		{
			return MonsterDatabase.HUMANOID;
		}
		if ( s.equals( "horror" ) )
		{
			return MonsterDatabase.HORROR;
		}
		if ( s.equals( "mer-kin" ) )
		{
			return MonsterDatabase.MER_KIN;
		}
		if ( s.equals( "object" ) )
		{
			return MonsterDatabase.OBJECT;
		}
		if ( s.equals( "orc" ) )
		{
			return MonsterDatabase.ORC;
		}
		if ( s.equals( "penguin" ) )
		{
			return MonsterDatabase.PENGUIN;
		}
		if ( s.equals( "pirate" ) )
		{
			return MonsterDatabase.PIRATE;
		}
		if ( s.equals( "plant" ) )
		{
			return MonsterDatabase.PLANT;
		}
		if ( s.equals( "slime" ) )
		{
			return MonsterDatabase.SLIMES;
		}
		if ( s.equals( "strange" ) )
		{
			return MonsterDatabase.STRANGE;
		}
		if ( s.equals( "undead" ) )
		{
			return MonsterDatabase.UNDEAD;
		}
		return MonsterDatabase.NONE;
	}
}
