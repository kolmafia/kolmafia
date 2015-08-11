/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.EncounterManager.EncounterType;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterDatabase
{
	private static final Map<String, MonsterData> MONSTER_DATA = new TreeMap<String, MonsterData>();
	private static final Map<String, MonsterData> LEET_MONSTER_DATA = new TreeMap<String, MonsterData>();
	private static String[] MONSTER_STRINGS = null;
	private static final Map<String, MonsterData> MONSTER_IMAGES = new TreeMap<String, MonsterData>();
	private static final Map<Integer, MonsterData> MONSTER_IDS = new TreeMap<Integer, MonsterData>();

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
			String idString = data.length > 1 ? data[ 1 ] : "";
			String imageString = data.length > 2 ? data[ 2 ] : "";
			String attributes = data.length > 3 ? data[ 3 ] : "";

			int id = StringUtilities.isNumeric( idString ) ? StringUtilities.parseInt( idString ) : 0;
			String [] images = imageString.split( "\\s*,\\s*" );

			MonsterData monster = MonsterDatabase.registerMonster( name, id, images, attributes );
			if ( monster == null )
			{
				continue;
			}

			boolean bogus = false;

			for ( int i = 4; i < data.length; ++i )
			{
				String itemString = data[ i ];
				AdventureResult item = MonsterDatabase.parseItem( itemString );
				if ( item == null || item.getItemId() == -1 || item.getName() == null )
				{
					RequestLogger.printLine( "Bad item for monster \"" + name + "\": " + itemString );
					bogus = true;
					continue;
				}

				monster.addItem( item );
			}

			if ( !bogus )
			{
				monster.doneWithItems();

				// "dummy" monsters are KoL monsters names that
				// we always disambiguate into other monsters.
				// We need them only for 1337 name translation

				if ( !monster.isDummy() )
				{
					String keyName = CombatActionManager.encounterKey( name );
					StringUtilities.registerPrepositions( keyName );
					MonsterDatabase.MONSTER_DATA.put( keyName, monster );
					for ( String image : images )
					{
						MonsterDatabase.MONSTER_IMAGES.put( image, monster );
					}
					MonsterDatabase.registerMonsterId( id, name, monster );
				}

				MonsterDatabase.LEET_MONSTER_DATA.put( StringUtilities.leetify( name ), monster );
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

		int itemId = ItemDatabase.getItemId( name );
		if ( itemId == -1 )
		{
			return ItemPool.get( data, (int)'0' );
		}

		return ItemPool.get( itemId, (count << 16) | prefix );
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

	public static final MonsterData findMonsterById( final int id )
	{
		return MonsterDatabase.MONSTER_IDS.get( id );
	}

	public static final String translateLeetMonsterName( final String leetName )
	{
		MonsterData monster = MonsterDatabase.LEET_MONSTER_DATA.get( leetName );
		return monster == null ? leetName : monster.getName();
	}

	// Register an unknown monster
	public static final MonsterData registerMonster( final String name )
	{
		MonsterData monster = MonsterDatabase.registerMonster( name, 0, new String[0], "" );
		MonsterDatabase.MONSTER_DATA.put( name, monster );
		MonsterDatabase.LEET_MONSTER_DATA.put( StringUtilities.leetify( name ), monster );
		return monster;
	}

	// Register an unknown monster from Manuel
	public static final MonsterData registerMonster( final String name, final int id, final String image )
	{
		String[] images = { image };
		MonsterData monster = MonsterDatabase.registerMonster( name, id, images, "" );
		MonsterDatabase.registerMonsterId( id, name, monster );
		return monster;
	}

	private static final void registerMonsterId( final int id, final String name, final MonsterData monster )
	{
		if ( id != 0 )
		{
			MonsterData old = MonsterDatabase.MONSTER_IDS.get( id );
			if ( old == null )
			{
				MonsterDatabase.MONSTER_IDS.put( id, monster );
			}
			else
			{
				RequestLogger.printLine( "Duplicate monster ID " + id + " : (" + old.getName() + "," + name + ")" );
			}
		}
	}

	public static final Set entrySet()
	{
		return MonsterDatabase.MONSTER_DATA.entrySet();
	}

	public static final Set<Integer> idKeySet()
	{
		return MonsterDatabase.MONSTER_IDS.keySet();
	}

	public static final MonsterData registerMonster( final String name, final int id, final String[] images, final String attributes )
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
		Object scale = null;
		Object cap = null;
		int floor = Integer.MIN_VALUE;
		Object mlMult = null;
		int meat = 0;
		Element attackElement = Element.NONE;
		Element defenseElement = Element.NONE;
		Phylum phylum = Phylum.NONE;
		int poison = Integer.MAX_VALUE;
		boolean boss = false;
		boolean dummy = false;
		EncounterType type = EncounterType.NONE;
		int physical = 0;
		String manuelName = null;

		StringTokenizer tokens = new StringTokenizer( attributes, " " );
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
					scale = parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Cap:" ) )
				{
					cap = parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Floor:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						floor = StringUtilities.parseInt( tokens.nextToken() );
					}
					continue;
				}

				else if ( option.equals( "MLMult:" ) )
				{
					mlMult = parseNumeric( tokens );
					continue;
				}

				else if ( option.equals( "Phys:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						physical = StringUtilities.parseInt( tokens.nextToken() );
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
						}
					}
					continue;
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
						}
					}
					continue;
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
						}
					}
					continue;
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
					}
					continue;
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
						}
					}
					continue;
				}

				else if ( option.equals( "Manuel:" ) )
				{
					if ( tokens.hasMoreTokens() )
					{
						manuelName = parseString( tokens.nextToken(), tokens );
					}
					continue;
				}

				else if ( option.startsWith( "\"" ) )
				{
					String string = parseString( option, tokens );
					poison = EffectDatabase.getPoisonLevel( string );
					if ( poison == Integer.MAX_VALUE )
					{
						RequestLogger.printLine( "Monster: \"" + name + "\": unknown poison type: " + string );
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

				else if ( option.equals( "DUMMY" ) )
				{
					dummy = true;
					continue;
				}

				RequestLogger.printLine( "Monster: \"" + name + "\": unknown option: " + option );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, attributes );
			}

			return null;
		}

		monster = new MonsterData( name, id,
					   health, attack, defense,
					   initiative, experience,
					   scale, cap, floor, mlMult,
					   attackElement, defenseElement,
					   physical,
					   meat, phylum, poison,
					   boss, dummy, type,
					   images, manuelName,
					   attributes );

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

	private static final String parseString( String token, StringTokenizer tokens )
	{
		if ( !token.startsWith( "\"" ) )
		{
			return "";
		}

		StringBuffer temp = new StringBuffer( token );
		while ( !token.endsWith( "\"" ) && tokens.hasMoreTokens() )
		{
			token = tokens.nextToken();
			temp.append( ' ' );
			temp.append( token );
		}

		// Remove initial and final quote
		temp.deleteCharAt( 0 );
		temp.deleteCharAt( temp.length() - 1 );

		return temp.toString();
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

	public static final boolean contains( final String name )
	{
		return MonsterDatabase.findMonster( name, false ) != null;
	}
}
