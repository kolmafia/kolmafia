/**
 * Copyright (c) 2005-2020, KoLmafia development team
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
import java.io.File;
import java.io.PrintStream;

import java.util.Map;
import java.util.TreeMap;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PocketDatabase
{
	public enum PocketType
	{
		UNKNOWN( "Unknown" ),
		STATS( "Stats" ),
		MONSTER( "Monster" ),
		EFFECT( "Effect", "an effect" ),
		BUFF( "Buff", "an accordion buff" ),
		ELEMENT( "Element", "an elemental resistance effect" ),
		JOKE( "Joke" ),
		CANDY1( "Candy1", "a type 1 candy effect" ),
		CANDY2( "Candy2", "a type 2 candy effect" ),
		CHIPS1( "Chips1", "a potato chip effect" ),
		GUM1( "Gum1", "a gum effect" ),
		LENS1( "Lens1", "a contact lens effect" ),
		NEEDLE1( "Needle1", "a needles effect" ),
		TEETH1( "Teeth1", "a teeth effect" ),
		CANDY( "Candy", "2 candy effects" ),
		CHIPS( "Chips", "2 potato chip effects" ),
		GUM( "Gum", "2 gum effects" ),
		LENS( "Lens", "2 contact lens effects" ),
		NEEDLE( "Needle", "2 needles effects" ),
		TEETH( "Teeth", "2 teeth effects" ),
		ITEM( "Item", "an item" ),	
		ITEM2( "Item2", "two items" ),	
		AVATAR( "Avatar", "an avatar potion" ),
		BELL( "Bell", "a desk bell" ),
		BOOZE( "Booze" ),
		CASH( "Cash", "an item that is usable for meat" ),
		CHESS( "Chess", "a chess piece" ),
		CHOCO( "Choco", "some chocolate" ),
		FOOD( "Food" ),
		FRUIT( "Fruit" ),
		OYSTER( "Oyster", "an oyster egg" ),
		POTION( "Potion", "a potion" ),
		YEG( "Yeg", "an item from Yeg's Motel" ),
		SCRAP( "Scrap", "part of demon name" ),
		POEM( "Poem", "an encrypted line of a poem" ),
		MEAT( "Meat", "Meat and a puzzle clue" );

		private final String tag;
		private final String name;

		private PocketType( String tag )
		{
			this.tag = tag;
			this.name = tag.toLowerCase();
		}

		private PocketType( String tag, String name )
		{
			this.tag = tag;
			this.name = name;
		}

		public String getTag()
		{
			return this.tag;
		}

		@Override
		public String toString()
		{
			return this.name;
		}

		public static PocketType fromTag( String tag )
		{
			if ( tag != null )
			{
				for ( PocketType type : PocketType.values() )
				{
					if ( tag.equals( type.tag ) )
					{
						return type;
					}
				}
			}
			return null;
		}
	}

	public static class Pocket
	{
		protected final Integer pocket;
		protected final PocketType type;

		public Pocket( int pocket, PocketType type )
		{
			this.pocket = pocket;
			this.type = type;
		}

		public Integer getPocket()
		{
			return this.pocket;
		}

		public PocketType getType()
		{
			return this.type;
		}

		@Override
		public String toString()
		{
			return this.type.toString();
		}
	}

	public static class StatsPocket
		extends Pocket
	{
		private final int muscle;
		private final int mysticality;
		private final int moxie;

		public StatsPocket( int pocket, int muscle, int mysticality, int moxie )
		{
			super( pocket, PocketType.STATS );
			this.muscle = muscle;
			this.mysticality = mysticality;
			this.moxie = moxie;
		}

		public int getMuscle()
		{
			return this.muscle;
		}

		public int getMysticality()
		{
			return this.mysticality;
		}

		public int getMoxie()
		{
			return this.moxie;
		}

		@Override
		public String toString()
		{
			return "stats: " + this.muscle + "/" + this.mysticality + "/" + this.moxie;
		}
	}

	public static class MonsterPocket
		extends Pocket
	{
		private final MonsterData monster;

		public MonsterPocket( int pocket, MonsterData monster )
		{
			super( pocket, PocketType.MONSTER );
			this.monster = monster;
		}

		public MonsterData getMonster()
		{
			return this.monster;
		}

		@Override
		public String toString()
		{
			return "a " + this.monster.getName();
		}
	}

	public static class MeatPocket
		extends Pocket
	{
		private final int meat;
		private final String text;

		public MeatPocket( int pocket, int meat, String text )
		{
			super( pocket, PocketType.MEAT );
			this.meat = meat;
			this.text = text;
		}

		public int getMeat()
		{
			return this.meat;
		}

		public String getText()
		{
			return this.text;
		}

		@Override
		public String toString()
		{
			return String.valueOf( meat ) + " Meat and a clue: " + this.text;
		}
	}

	public static class PoemPocket
		extends Pocket
	{
		private final String text;

		public PoemPocket( int pocket, String text )
		{
			super( pocket, PocketType.POEM );
			this.text = text;
		}

		public String getText()
		{
			return this.text;
		}

		@Override
		public String toString()
		{
			return "an encrypted line of a poem: " + this.text;
		}
	}

	public static class ScrapPocket
		extends Pocket
	{
		private final int scrap;

		public ScrapPocket( int pocket, int scrap )
		{
			super( pocket, PocketType.SCRAP );
			this.scrap = scrap;
		}

		public int getScrap()
		{
			return this.scrap;
		}

		@Override
		public String toString()
		{
			return "part #" + this.scrap + " of a demon name";
		}
	}

	public static class OneItemPocket
		extends Pocket
	{
		protected final AdventureResult item1;

		public OneItemPocket( int pocket, PocketType type, AdventureResult item1 )
		{
			super( pocket, type );
			this.item1 = item1;
		}

		public AdventureResult getItem()
		{
			return this.item1;
		}

		@Override
		public String toString()
		{
			String name = this.item1.getName();
			int count = this.item1.getCount();
			return this.type.toString() + ": " + name + ( count == 1  ? "" : " (" + count + ")" );
		}
	}

	public static class TwoItemPocket
		extends OneItemPocket
	{
		protected final AdventureResult item2;

		public TwoItemPocket( int pocket, PocketType type, AdventureResult item1, AdventureResult item2 )
		{
			super( pocket, type, item1 );
			this.item2 = item2;
		}

		public AdventureResult getItem2()
		{
			return this.item2;
		}

		@Override
		public String toString()
		{
			String name1 = this.item1.getName();
			int count1 = this.item1.getCount();
			String name2 = this.item2.getName();
			int count2 = this.item2.getCount();
			return "two items: " + name1 + ( count1 == 1  ? "" : " (" + count1 + ")" ) + " and " + name2 + ( count2 == 1  ? "" : " (" + count2 + ")" );
		}
	}

	public static class OneEffectPocket
		extends Pocket
	{
		protected final AdventureResult effect1;

		public OneEffectPocket( int pocket, PocketType type, AdventureResult effect1 )
		{
			super( pocket, type );
			this.effect1 = effect1;
		}

		public AdventureResult getEffect1()
		{
			return this.effect1;
		}

		protected static final String normalizeEffectName( AdventureResult effect )
		{
			String name = effect.getName();
			int num = effect.getEffectId();
			int[] effectIds = EffectDatabase.getEffectIds( name, false );
			if ( effectIds != null && effectIds.length > 1 )
			{
				name = "[" + String.valueOf( num ) + "]" + name;
			}
			return name;
		}		

		@Override
		public String toString()
		{
			return this.type.toString() + ": " + this.normalizeEffectName( this.effect1 ) + " (" + this.effect1.getCount() + ")";
		}
	}

	public static class TwoEffectPocket
		extends OneEffectPocket
	{
		protected final AdventureResult effect2;

		public TwoEffectPocket( int pocket, PocketType type, AdventureResult effect1, AdventureResult effect2 )
		{
			super( pocket, type, effect1 );
			this.effect2 = effect2;
		}

		public AdventureResult getEffect2()
		{
			return this.effect2;
		}

		@Override
		public String toString()
		{
			return this.type.toString() + ": " + this.normalizeEffectName( this.effect1 ) + " (" + this.effect1.getCount() + ") and " + this.normalizeEffectName( this.effect2 ) + " (" + this.effect2.getCount() + ")";
		}
	}

	private static final AdventureResult JOKE_MAD = EffectPool.get( EffectPool.JOKE_MAD, 20 );

	public static class JokePocket
		extends OneEffectPocket
	{
		protected final String joke;

		public JokePocket( int pocket, String joke )
		{
			super( pocket, PocketType.JOKE, JOKE_MAD );
			this.joke = joke;
		}

		public String getJoke()
		{
			return this.joke;
		}

		@Override
		public String toString()
		{
			return "an annoying (20 turns of Joke-Mad) joke: " + this.joke;
		}
	}

	// Here are the data structures for retrieving pocket data

	public static final Map<Integer, Pocket> allPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> uncategorizedPockets = new TreeMap<>();

	public static final Map<Integer, Pocket> statsPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> monsterPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> oneEffectPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> buffPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> elementPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> candy1Pockets = new TreeMap<>();
	public static final Map<Integer, Pocket> candy2Pockets = new TreeMap<>();
	public static final Map<Integer, Pocket> chips1Pockets = new TreeMap<>();
	public static final Map<Integer, Pocket> gum1Pockets = new TreeMap<>();
	public static final Map<Integer, Pocket> lens1Pockets = new TreeMap<>();
	public static final Map<Integer, Pocket> needle1Pockets = new TreeMap<>();
	public static final Map<Integer, Pocket> teeth1Pockets = new TreeMap<>();
	public static final Map<Integer, Pocket> candyPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> chipsPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> gumPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> lensPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> needlePockets = new TreeMap<>();
	public static final Map<Integer, Pocket> teethPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> oneItemPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> twoItemPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> avatarPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> bellPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> boozePockets = new TreeMap<>();
	public static final Map<Integer, Pocket> cashPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> chessPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> chocoPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> foodPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> fruitPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> oysterPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> potionPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> yegPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> jokePockets = new TreeMap<>();
	public static final Map<Integer, Pocket> meatPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> poemPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> scrapPockets = new TreeMap<>();
	public static final Map<Integer, Pocket> unknownPockets = new TreeMap<>();

	static
	{
		PocketDatabase.reset();
		RequestLogger.printLine( "Pockets loaded: " + allPockets.size() );

		/*
		if ( uncategorizedPockets.size() > 0 )
		{
			RequestLogger.printLine( "Uncategorized pockets: " + uncategorizedPockets.size() );
		}

		RequestLogger.printLine( "Stats pockets: " + statsPockets.size() );

		RequestLogger.printLine( "Monster pockets: " + monsterPockets.size() );

		RequestLogger.printLine( "One Effect pockets: " + oneEffectPockets.size() );
		RequestLogger.printLine( "Accordion Buff pockets: " + buffPockets.size() );
		RequestLogger.printLine( "Elemental Resistance pockets: " + elementPockets.size() );
		RequestLogger.printLine( "Candy1 pockets: " + candy1Pockets.size() );
		RequestLogger.printLine( "Candy2 pockets: " + candy2Pockets.size() );
		RequestLogger.printLine( "Chips1 pockets: " + chips1Pockets.size() );
		RequestLogger.printLine( "Gum1 pockets: " + gum1Pockets.size() );
		RequestLogger.printLine( "Lens1 pockets: " + lens1Pockets.size() );
		RequestLogger.printLine( "Needle1 pockets: " + needle1Pockets.size() );
		RequestLogger.printLine( "Teeth1 pockets: " + teeth1Pockets.size() );
		RequestLogger.printLine( "Candy pockets: " + candyPockets.size() );
		RequestLogger.printLine( "Chips pockets: " + chipsPockets.size() );
		RequestLogger.printLine( "Gum pockets: " + gumPockets.size() );
		RequestLogger.printLine( "Lens pockets: " + lensPockets.size() );
		RequestLogger.printLine( "Needle pockets: " + needlePockets.size() );
		RequestLogger.printLine( "Teeth pockets: " + teethPockets.size() );

		RequestLogger.printLine( "One Item pockets: " + oneItemPockets.size() );
		RequestLogger.printLine( "Two Item pockets: " + twoItemPockets.size() );
		RequestLogger.printLine( "Avatar Potion Pockets: " + avatarPockets.size() );
		RequestLogger.printLine( "Desk Bell pockets: " + bellPockets.size() );
		RequestLogger.printLine( "Booze pockets: " + boozePockets.size() );
		RequestLogger.printLine( "Cash pockets: " + cashPockets.size() );
		RequestLogger.printLine( "Chess Piece pockets: " + chessPockets.size() );
		RequestLogger.printLine( "Chocolate pockets: " + chocoPockets.size() );
		RequestLogger.printLine( "Food pockets: " + foodPockets.size() );
		RequestLogger.printLine( "Fruit pockets: " + fruitPockets.size() );
		RequestLogger.printLine( "Oyster Egg pockets: " + oysterPockets.size() );
		RequestLogger.printLine( "Potion pockets: " + potionPockets.size() );
		RequestLogger.printLine( "Yeg's Motel item pockets: " + yegPockets.size() );

		RequestLogger.printLine( "Joke pockets: " + jokePockets.size() );
		RequestLogger.printLine( "Meat pockets: " + meatPockets.size() );
		RequestLogger.printLine( "Poem pockets: " + poemPockets.size() );
		RequestLogger.printLine( "Scrap pockets: " + scrapPockets.size() );
		RequestLogger.printLine( "Unknown pockets: " + unknownPockets.size() );
		*/
	}

	private static void reset()
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "cultshorts.txt", KoLConstants.CULTSHORTS_VERSION );
		String[] data;
		boolean error = false;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length >= 2 )
			{
				int pocketId = StringUtilities.parseInt( data[ 0 ] );
				if ( pocketId < 1 || pocketId > 666 )
				{
					RequestLogger.printLine( "Bogus pocket number: " + pocketId );
					error = true;
					continue;
				}

				String tag = data[ 1 ];
				PocketType type = PocketType.fromTag( tag );
				if ( type == null )
				{
					RequestLogger.printLine( "Pocket " + pocketId + " has bogus pocket type: " + tag );
					error = true;
					continue;
				}

				Pocket pocket = parsePocketData( pocketId, type, data );
				if ( pocket == null )
				{
					error = true;
					continue;
				}

				if ( !PocketDatabase.addToDatabase( pocket ) )
				{
					error = true;
				}
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			error = true;
		}

		if ( error )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Error loading pocket database." );
		}
	}

	private static Pocket parsePocketData( int pocketId, PocketType type, String[] data )
	{
		switch ( type )
		{
		case STATS:
		{
			if ( data.length < 5 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " does not have muscle, mysticality, and moxie" );
				return null;
			}
			String muscleString = data[ 2 ];
			if ( !StringUtilities.isNumeric( muscleString ) )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has bad muscle value: " + muscleString );
				return null;
			}
			int muscle = StringUtilities.parseInt( muscleString );
			String mysticalityString = data[ 3 ];
			if ( !StringUtilities.isNumeric( mysticalityString ) )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has bad mysticality value: " + mysticalityString );
				return null;
			}
			int mysticality = StringUtilities.parseInt( mysticalityString );
			String moxieString = data[ 4 ];
			if ( !StringUtilities.isNumeric( moxieString ) )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has bad moxie value: " + moxieString );
				return null;
			}
			int moxie = StringUtilities.parseInt( moxieString );
			return new StatsPocket( pocketId, muscle, mysticality, moxie );
		}
		case MONSTER:
		{
			if ( data.length < 3 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " does not have a monster name" );
				return null;
			}
			String name = data[ 2 ];
			MonsterData monster = MonsterDatabase.findMonster( name );
			if ( monster == null )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has unknown monster name: " + name );
				return null;
			}
			return new MonsterPocket( pocketId, monster );
		}
		case EFFECT:
		case BUFF:
		case ELEMENT:
		case CANDY1:
		case CANDY2:
		case CHIPS1:
		case GUM1:
		case LENS1:
		case NEEDLE1:
		case TEETH1:
		{
			if ( data.length < 3 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " does not have an effect" );
				return null;
			}
			String effect1String = data[ 2 ];
			AdventureResult effect1 = PocketDatabase.parseEffect( effect1String );
			if ( effect1 == null )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has unknown effect: " + effect1String );
				return null;
			}
			if ( effect1.getCount() == 0 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has effect duration 0: " + effect1String );
			}
			return new OneEffectPocket( pocketId, type, effect1 );
		}
		case CANDY:
		case CHIPS:
		case GUM:
		case LENS:
		case NEEDLE:
		case TEETH:
		{
			if ( data.length < 4 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " does not have two effects" );
				return null;
			}
			String effect1String = data[ 2 ];
			AdventureResult effect1 = PocketDatabase.parseEffect( effect1String );
			if ( effect1 == null )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has unknown effect: " + effect1String );
				return null;
			}
			if ( effect1.getCount() == 0 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has effect duration 0: " + effect1String );
			}
			String effect2String = data[ 3 ];
			AdventureResult effect2 = PocketDatabase.parseEffect( effect2String );
			if ( effect2 == null )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has unknown effect: " + effect2String );
				return null;
			}
			if ( effect2.getCount() == 0 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has effect duration 0: " + effect2String );
			}
			return new TwoEffectPocket( pocketId, type, effect1, effect2 );
		}
		case ITEM:
		case AVATAR:
		case BELL:
		case BOOZE:
		case CASH:
		case CHESS:
		case CHOCO:
		case FOOD:
		case FRUIT:
		case OYSTER:
		case POTION:
		case YEG:
		{
			if ( data.length < 3 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " does not have an item" );
				return null;
			}
			String item1String = data[ 2 ];
			AdventureResult item1 = PocketDatabase.parseItem( item1String );
			if ( item1 == null )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has unknown item: " + item1String );
				return null;
			}
			if ( item1.getCount() == 0 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has item count 0: " + item1String );
			}
			return new OneItemPocket( pocketId, type, item1 );
		}
		case ITEM2:
		{
			if ( data.length < 4 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " does not have two items" );
				return null;
			}
			String item1String = data[ 2 ];
			AdventureResult item1 = PocketDatabase.parseItem( item1String );
			if ( item1 == null )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has unknown item: " + item1String );
				return null;
			}
			if ( item1.getCount() == 0 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has item count 0: " + item1String );
			}
			String item2String = data[ 3 ];
			AdventureResult item2 = PocketDatabase.parseItem( item2String );
			if ( item2 == null )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has unknown item: " + item2String );
				return null;
			}
			if ( item2.getCount() == 0 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has item count 0: " + item2String );
			}
			return new TwoItemPocket( pocketId, type, item1, item2 );
		}
		case JOKE:
		{
			if ( data.length < 3 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " must have a text string" );
				return null;
			}
			String text = data[ 2 ];
			return new JokePocket( pocketId, text );
		}
		case MEAT:
		{
			if ( data.length < 4 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " must have an integer and a text string" );
				return null;
			}
			String meatString = data[ 2 ];
			if ( !StringUtilities.isNumeric( meatString ) )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has bad meat value: " + meatString );
				return null;
			}
			int meat = StringUtilities.parseInt( meatString );
			String text = data[ 3 ];
			return new MeatPocket( pocketId, meat, text );
		}
		case POEM:
		{
			if ( data.length < 3 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " must have a text string" );
				return null;
			}
			String text = data[ 2 ];
			return new PoemPocket( pocketId, text );
		}
		case SCRAP:
		{
			if ( data.length < 3 )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " must have a scrap number" );
				return null;
			}
			String scrapString = data[ 2 ];
			if ( !StringUtilities.isNumeric( scrapString ) )
			{
				RequestLogger.printLine( "Pocket " + pocketId + " has bad scrap number: " + scrapString );
				return null;
			}
			int scrap = StringUtilities.parseInt( scrapString );
			return new ScrapPocket( pocketId, scrap );
		}
		case UNKNOWN:
		{
			RequestLogger.printLine( "Pocket " + pocketId + " is unknown" );
			return new Pocket( pocketId, type );
		}
		}

		return new Pocket( pocketId, type );
	}

	private static AdventureResult parseEffect( String effectString )
	{
		String name;
		int duration;
		int lparen = effectString.lastIndexOf( "(" );
		int rparen = effectString.lastIndexOf( ")" );
		if ( lparen < 0 || rparen < 0 )
		{
			name = effectString;
			duration = 0;
		}
		else
		{
			String durationString = effectString.substring( lparen + 1, rparen );
			name = effectString.substring( 0, lparen ).trim();
			duration = StringUtilities.isNumeric( durationString ) ? StringUtilities.parseInt( durationString ) : 0;
		}
		int effectId = EffectDatabase.getEffectId( name, true );
		if ( effectId < 0 )
		{
			return null;
		}
		return EffectPool.get( effectId, duration );
	}

	private static AdventureResult parseItem( String itemString )
	{
		String name;
		int count;
		int lparen = itemString.lastIndexOf( "(" );
		int rparen = itemString.lastIndexOf( ")" );
		if ( lparen < 0 || rparen < 0 )
		{
			name = itemString;
			count = 1;
		}
		else
		{
			String countString = itemString.substring( lparen + 1, rparen );
			name = itemString.substring( 0, lparen ).trim();
			count = StringUtilities.isNumeric( countString ) ? StringUtilities.parseInt( countString ) : 0;
		}
		int itemId = ItemDatabase.getItemId( name, 1, false );
		if ( itemId < 0 )
		{
			return null;
		}
		return ItemPool.get( itemId, count );
	}

	private static boolean addToDatabase( Pocket pocket )
	{
		Integer key = pocket.getPocket();
		if ( PocketDatabase.allPockets.containsKey( key ) )
		{
			RequestLogger.printLine( "Duplicate pocket id: " + key );
			return false;
		}
		PocketDatabase.allPockets.put( key, pocket );
		switch ( pocket.getType() )
		{
		case STATS:
			PocketDatabase.statsPockets.put( key, pocket );
			break;
		case MONSTER:
			PocketDatabase.monsterPockets.put( key, pocket );
			break;
		case EFFECT:
			PocketDatabase.oneEffectPockets.put( key, pocket );
			break;
		case BUFF:
			PocketDatabase.buffPockets.put( key, pocket );
			break;
		case ELEMENT:
			PocketDatabase.elementPockets.put( key, pocket );
			break;
		case JOKE:
			PocketDatabase.jokePockets.put( key, pocket );
			break;
		case CANDY1:
			PocketDatabase.candy1Pockets.put( key, pocket );
			break;
		case CANDY2:
			PocketDatabase.candy2Pockets.put( key, pocket );
			break;
		case CHIPS1:
			PocketDatabase.chips1Pockets.put( key, pocket );
			break;
		case GUM1:
			PocketDatabase.gum1Pockets.put( key, pocket );
			break;
		case LENS1:
			PocketDatabase.lens1Pockets.put( key, pocket );
			break;
		case NEEDLE1:
			PocketDatabase.needle1Pockets.put( key, pocket );
			break;
		case TEETH1:
			PocketDatabase.teeth1Pockets.put( key, pocket );
			break;
		case CANDY:
			PocketDatabase.candyPockets.put( key, pocket );
			break;
		case CHIPS:
			PocketDatabase.chipsPockets.put( key, pocket );
			break;
		case GUM:
			PocketDatabase.gumPockets.put( key, pocket );
			break;
		case LENS:
			PocketDatabase.lensPockets.put( key, pocket );
			break;
		case NEEDLE:
			PocketDatabase.needlePockets.put( key, pocket );
			break;
		case TEETH:
			PocketDatabase.teethPockets.put( key, pocket );
			break;
		case ITEM:
			PocketDatabase.oneItemPockets.put( key, pocket );
			break;
		case ITEM2:
			PocketDatabase.twoItemPockets.put( key, pocket );
			break;
		case AVATAR:
			PocketDatabase.avatarPockets.put( key, pocket );
			break;
		case BELL:
			PocketDatabase.bellPockets.put( key, pocket );
			break;
		case BOOZE:
			PocketDatabase.boozePockets.put( key, pocket );
			break;
		case CASH:
			PocketDatabase.cashPockets.put( key, pocket );
			break;
		case CHESS:
			PocketDatabase.chessPockets.put( key, pocket );
			break;
		case CHOCO:
			PocketDatabase.chocoPockets.put( key, pocket );
			break;
		case FOOD:
			PocketDatabase.foodPockets.put( key, pocket );
			break;
		case FRUIT:
			PocketDatabase.fruitPockets.put( key, pocket );
			break;
		case OYSTER:
			PocketDatabase.oysterPockets.put( key, pocket );
			break;
		case POTION:
			PocketDatabase.potionPockets.put( key, pocket );
			break;
		case YEG:
			PocketDatabase.yegPockets.put( key, pocket );
			break;
		case MEAT:
			PocketDatabase.meatPockets.put( key, pocket );
			break;
		case POEM:
			PocketDatabase.poemPockets.put( key, pocket );
			break;
		case SCRAP:
			PocketDatabase.scrapPockets.put( key, pocket );
			break;
		case UNKNOWN:
			PocketDatabase.unknownPockets.put( key, pocket );
			break;
		default:
			PocketDatabase.uncategorizedPockets.put( key, pocket );
			break;
		}
		return true;
	}

	public static Pocket pocketByNumber( int pocket )
	{
		return PocketDatabase.allPockets.get( IntegerPool.get( pocket ) );
	}

	public static MonsterData monsterByNumber( int pocket )
	{
		Pocket mp = PocketDatabase.monsterPockets.get( IntegerPool.get( pocket ) );
		return mp == null ? null : ((MonsterPocket)mp).getMonster();
	}
}
