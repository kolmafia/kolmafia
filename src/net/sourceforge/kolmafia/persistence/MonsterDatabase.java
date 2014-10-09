/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterDatabase
{
	private static final Map<String, MonsterData> MONSTER_DATA = new TreeMap<String, MonsterData>();
	private static String[] MONSTER_STRINGS = null;
	private static final Map<String, MonsterData> MONSTER_IMAGES = new TreeMap<String, MonsterData>();

	public enum Element
	{
		NONE( "none" ),
		COLD( "cold" ),
		HOT( "hot" ),
		SLEAZE( "sleaze" ),
		SPOOKY( "spooky" ),
		STENCH( "stench" ),
		SLIME( "slime" ),
		SUPERCOLD( "supercold" );

		private final String name;

		private Element( String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.name;
		}

		public static Element fromString( String text )
		{
			if ( text != null )
			{
				for ( Element elem : Element.values() )
				{
					if ( text.equals( elem.name ) )
					{
						return elem;
					}
				}
			}
			return Element.NONE;
		}
	}

	public enum Phylum
	{
		NONE( "none" ),
		BEAST( "beast" ),
		BUG( "bug" ),
		CONSTELLATION( "constellation" ),
		CONSTRUCT( "construct" ),
		DEMON( "demon" ),
		DUDE( "dude" ),
		ELEMENTAL( "elemental" ),
		ELF( "elf" ),
		FISH( "fish" ),
		GOBLIN( "goblin" ),
		HIPPY( "hippy" ),
		HOBO( "hobo" ),
		HUMANOID( "humanoid" ),
		HORROR( "horror" ),
		MER_KIN( "mer-kin" ),
		ORC( "orc" ),
		PENGUIN( "penguin" ),
		PIRATE( "pirate" ),
		PLANT( "plant" ),
		SLIME( "slime" ),
		UNDEAD( "undead" ),
		WEIRD( "weird" ),
		;

		private final String name;

		private Phylum( String name )
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return this.name;
		}
	}

	public static final String[] ELEMENT_ARRAY = new String[ Element.values().length ];
	public static final String[] PHYLUM_ARRAY = new String[ Phylum.values().length ];

	static
	{
		for ( int i = 0; i < Element.values().length; i++ )
		{
			ELEMENT_ARRAY[ i ] = Element.values()[i].toString();
		}

		for ( int i = 0; i < Phylum.values().length; i++ )
		{
			PHYLUM_ARRAY[ i ] = Phylum.values()[i].toString();
		}
	}

	public static final Element stringToElement( final String name )
	{
		for ( Element elem : Element.values() )
		{
			if ( name.equals( elem.toString() ) )
			{
				return elem;
			}
		}
		return Element.NONE;
	}

	public static final Phylum phylumNumber( final String name )
	{
		for ( Phylum phylum : Phylum.values() )
		{
			if ( name.equals( phylum.toString() ) )
			{
				return phylum;
			}
		}
		return Phylum.NONE;
	}

	public static final boolean elementalVulnerability( final Element element1, final Element element2 )
	{
		switch ( element1 )
		{
		case COLD:
			return element2 == Element.HOT || element2 == Element.SPOOKY;
		case HOT:
			return element2 == Element.SLEAZE || element2 == Element.STENCH;
		case SLEAZE:
			return element2 == Element.COLD || element2 == Element.SPOOKY;
		case SPOOKY:
			return element2 == Element.HOT || element2 == Element.STENCH;
		case STENCH:
			return element2 == Element.SLEAZE || element2 == Element.COLD;
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
		MonsterDatabase.MONSTER_IMAGES.clear();

		BufferedReader reader = FileUtilities.getVersionedReader( "monsters.txt", KoLConstants.MONSTERS_VERSION );
		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length < 1 )
			{
				continue;
			}

			String name = data[ 0 ];
			String image = data.length > 1 ? data[ 1 ] : "";
			String attributes = data.length > 2 ? data[ 2 ] : "";

			MonsterData monster = MonsterDatabase.registerMonster( name, image, attributes );
			if ( monster == null )
			{
				continue;
			}

			boolean bogus = false;

			for ( int i = 3; i < data.length; ++i )
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
				String keyName = CombatActionManager.encounterKey( data[ 0 ] );
				StringUtilities.registerPrepositions( keyName );
				MonsterDatabase.MONSTER_DATA.put( keyName, monster );
				if ( !image.equals( "" ) )
				{
					MonsterDatabase.MONSTER_IMAGES.put( image, monster );
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

			if ( left == -1 )
			{
				return null;
			}

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
	
	private synchronized static final void initializeMonsterStrings()
	{
		if ( MonsterDatabase.MONSTER_STRINGS == null )
		{
			String[] monsterData = new String[ MonsterDatabase.MONSTER_DATA.size() ];
			MonsterDatabase.MONSTER_DATA.keySet().toArray( monsterData );
			MonsterDatabase.MONSTER_STRINGS = monsterData;
		}
	}

	public static final MonsterData findMonster( final String name, boolean trySubstrings )
	{
		String keyName = CombatActionManager.encounterKey( name );
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

		initializeMonsterStrings();

		List<String> matchingNames = StringUtilities.getMatchingNames( MonsterDatabase.MONSTER_STRINGS, keyName );

		if ( matchingNames.size() != 1 )
		{
			return null;
		}

		return MonsterDatabase.MONSTER_DATA.get( matchingNames.get( 0 ) );
	}

	public static final MonsterData findMonsterByImage( final String image )
	{
		return MonsterDatabase.MONSTER_IMAGES.get( image );
	}

	// Register an unknown monster
	public static final MonsterData registerMonster( final String name )
	{
		MonsterData monster = MonsterDatabase.registerMonster( name, "", "" );
		MonsterDatabase.MONSTER_DATA.put( name, monster );
		return monster;
	}

	public static final Set entrySet()
	{
		return MonsterDatabase.MONSTER_DATA.entrySet();
	}

	public static final MonsterData registerMonster( final String name, final String image, final String s )
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
		int scale = Integer.MIN_VALUE;
		int cap = Integer.MAX_VALUE;
		int floor = Integer.MIN_VALUE;
		int mlMult = 1;
		int meat = 0;
		Element attackElement = Element.NONE;
		Element defenseElement = Element.NONE;
		Phylum phylum = Phylum.NONE;
		int poison = Integer.MAX_VALUE;
		boolean boss = false;
		EncounterType type = EncounterType.NONE;
		int physical = 0;

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

				else if ( option.equals( "Scale:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						scale = StringUtilities.parseInt( tokens.nextToken() );
						continue;
					}
					continue;
				}

				else if ( option.equals( "Cap:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						cap = StringUtilities.parseInt( tokens.nextToken() );
						continue;
					}
					continue;
				}

				else if ( option.equals( "Floor:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						floor = StringUtilities.parseInt( tokens.nextToken() );
						continue;
					}
					continue;
				}

				else if ( option.equals( "MLMult:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						mlMult = StringUtilities.parseInt( tokens.nextToken() );
						continue;
					}
					continue;
				}

				else if ( option.equals( "Phys:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						physical = StringUtilities.parseInt( tokens.nextToken() );
						continue;
					}
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
						Element element = MonsterDatabase.parseElement( value );
						if ( element != Element.NONE )
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
						Element element = MonsterDatabase.parseElement( value );
						if ( element != Element.NONE )
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
						Element element = MonsterDatabase.parseElement( value );
						if ( element != Element.NONE )
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
							int minMeat = StringUtilities.parseInt( value.substring( 0, dash ) );
							int maxMeat = StringUtilities.parseInt( value.substring( dash + 1 ) );
							meat = ( minMeat + maxMeat ) / 2;
						}
						else
						{
							meat = StringUtilities.parseInt( value );
						}
						continue;
					}
				}

				else if ( option.equals( "P:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						value = tokens.nextToken();
						Phylum num = MonsterDatabase.parsePhylum( value );
						if ( num != Phylum.NONE )
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

				else if ( option.equals( "WANDERER" ) )
				{
					type = EncounterType.WANDERER;
					continue;
				}

				else if ( option.equals( "ULTRARARE" ) )
				{
					type = EncounterType.ULTRARARE;
					continue;
				}

				else if ( option.equals( "SEMIRARE" ) )
				{
					type = EncounterType.SEMIRARE;
					continue;
				}

				else if ( option.equals( "SUPERLIKELY" ) )
				{
					type = EncounterType.SUPERLIKELY;
					continue;
				}

				else if ( option.equals( "FREE" ) )
				{
					type = EncounterType.FREE_COMBAT;
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

		monster = new MonsterData( name, health, attack, defense, initiative, experience,
					   scale, cap, floor, mlMult,
					   attackElement, defenseElement, physical,
					   meat,
					   phylum, poison, boss, type, image, s );
		return monster;
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

	private static final Element parseElement( final String s )
	{
		for ( Element elem : Element.values() )
		{
			if ( elem.toString().equals( s ) )
			{
				return elem;
			}
		}
		return Element.NONE;
	}

	private static final Phylum parsePhylum( final String s )
	{
		for ( Phylum phylum : Phylum.values() )
		{
			if ( phylum.toString().equals( s ) )
			{
				return phylum;
			}
		}
		return Phylum.NONE;
	}
}
