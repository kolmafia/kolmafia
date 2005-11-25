/**
 * Copyright (c) 2005, KoLmafia development team
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

import java.io.BufferedReader;
import java.util.List;
import java.util.Iterator;
import net.java.dev.spellcast.utilities.SortedListModel;


/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows the client to do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class BuffBotDatabase extends KoLDatabase
{
	// All buffs: skill #, display name, abbreviation
	private static final Object [][] buffs =
	{
		// Accordion Thief Buffs
		{ new Integer(6003), "Antiphon of Aptitude", "Antiphon" },
		{ new Integer(6004), "Moxious Madrigal", "Madrigal" },
		{ new Integer(6005), "Canticle of Celerity", "Celerity" },
		{ new Integer(6006), "Polka of Plenty", "Polka" },
		{ new Integer(6007), "Magical Mojomuscular Melody", "MMMelody" },
		{ new Integer(6008), "Power Ballad of the Arrowsmith", "Power Ballad" },
		{ new Integer(6009), "Anthem of Absorption", "Anthem" },
		{ new Integer(6010), "Phat Loot Lyric", "Phat Loot" },
		{ new Integer(6011), "Psalm of Pointiness", "Psalm" },
		{ new Integer(6012), "Symphony of Destruction", "Symphony" },
		{ new Integer(6013), "Shanty of Superiority", "Shanty" },
		{ new Integer(6014), "Ode to Booze", "Ode" },

		// Sauceress Buffs
		{ new Integer(4007), "Elemental Saucesphere", "Elemental" },
		{ new Integer(4008), "Jalape&ntilde;o Saucesphere", "Jalapeno" },
		{ new Integer(4011), "Jaba&ntilde;ero Saucesphere", "Jabanero" },

		// Turtle Tamer Buffs
		{ new Integer(2007), "Ghostly Shell", "Ghostly" },
		{ new Integer(2008), "Reptilian Fortitude", "Fortitude" },
		{ new Integer(2009), "Empathy of the Newt", "Empathy" },
		{ new Integer(2010), "Tenacity of the Snapper", "Tenacity" },
		{ new Integer(2012), "Astral Shell", "Astral" }
	};

	// List of all Buffs
	private static SortedListModel buffList;

	static
	{
		// Initialize data.
		// Make the array
		buffList = new SortedListModel();

		// Fill it with Buff objects
		for ( int i = 0; i < buffs.length; ++i )
			buffList.add( new Buff( ((Integer)buffs[i][0]).intValue() ) );

		// Open the data file
		BufferedReader reader = getReader( "buffs.dat" );

		// Read a line at a time

		String [] data;
		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 5 )
			{
				// Get the fields

				String bot = data[0];

				// Validate the fields

				String skillName = data[1];
				Buff buff = findBuff( skillName );

				if ( buff == null )
				{
					System.out.println( "Unknown buff: " + skillName );
					continue;
				}

				int price;
				try
				{
					price = Integer.parseInt( data[2] );
				}
				catch ( Exception e )
				{
					System.out.println( "Bad price: " + data[2] );
					continue;
				}

				int turns;
				try
				{
					turns = Integer.parseInt( data[3] );
				}
				catch ( Exception e )
				{
					System.out.println( "Bad turns: " + data[3] );
					continue;
				}

				int free;
				try
				{
					free = Integer.parseInt( data[4] );
				}
				catch ( Exception e )
				{
					System.out.println( "Bad free: " + data[2] );
					continue;
				}

				// Add this offering to the buff
				buff.addOffering( new Offering( bot, price, turns, free != 0 ) );
			}
		}
	}

	private static Buff findBuff( String name )
	{
		int skill = ClassSkillsDatabase.getSkillID( name );
		Iterator iterator = buffList.iterator();

		while ( iterator.hasNext() )
		{
			Buff buff = (Buff)iterator.next();
			if ( skill == buff.getSkill() )
			     return buff;
		}

		return null;
	}

	public static final int buffCount()
	{	return buffList.size();
	}

	private static Buff getBuff( int index )
	{	return (Buff)buffList.get( index );
	}

	public static String getBuffName( int index )
	{	return getBuff( index ).getName();
	}

	public static int getBuffOfferingCount( int index )
	{	return getBuff( index ).getOfferingCount();
	}

	public static String getBuffBot( int index1, int index2 )
	{	return getBuff( index1 ).getOfferingBot( index2 );
	}

	public static int getBuffPrice( int index1, int index2 )
	{	return getBuff( index1 ).getOfferingPrice( index2 );
	}

	public static int getBuffTurns( int index1, int index2 )
	{	return getBuff( index1 ).getOfferingTurns( index2 );
	}

	public static long getBuffRate( int index1, int index2 )
	{	return getBuff( index1 ).getOfferingRate( index2 );
	}

	public static boolean getBuffFree( int index1, int index2 )
	{	return getBuff( index1 ).getOfferingFree( index2 );
	}

	public static String getBuffLabel( int index1, int index2, boolean compact )
                {	return getBuff( index1 ).getOfferingLabel( index2, compact );
	}

	private static void printBuffs()
	{
		int count = buffCount();
		System.out.println( count + " available buffs." );
		for ( int i = 0; i < count; ++i )
		{
			String skill = getBuffName( i );
			System.out.println( skill );

			int offerings = getBuffOfferingCount( i );
			for (int j = 0; j < offerings; ++j )
				System.out.println( "  " + getBuffLabel( i, j, false ) );
		}
	}

	private static class Buff implements Comparable
	{
		int skill;
		private SortedListModel offerings;
		String name;

		public Buff( int skill )
		{
			this.skill = skill;
			this.offerings = new SortedListModel();
			this.name = ClassSkillsDatabase.getSkillName( skill );
		}

		public void addOffering( Offering off )
		{
			offerings.add( off );
		}

		public int getSkill()
		{	return skill;
		}

		public String getName()
		{	return name;
		}

		public int getOfferingCount()
		{	return offerings.size();
		}

		public Offering getOffering( int index )
		{	return (Offering)offerings.get( index );
		}

		public String getOfferingBot( int index )
		{	return getOffering( index).getBot();
		}

		public int getOfferingPrice( int index )
		{	return getOffering( index).getPrice();
		}

		public int getOfferingTurns( int index )
		{	return getOffering( index).getTurns();
		}

		public boolean getOfferingFree( int index )
		{	return getOffering( index).getFree();
		}

		public long getOfferingRate( int index )
		{	return getOffering( index).getRate();
		}

		public String getOfferingLabel( int index, boolean compact )
		{	return getOffering( index).getLabel( compact );
		}

		public boolean equals( Object o )
		{
			if ( !(o instanceof Buff) || o == null )
				return false;

			Buff buff = (Buff) o;
			return name.equals( buff.name );
		}

		public int compareTo( Object o )
		{
			if ( !(o instanceof Buff) || o == null )
				return -1;

			Buff buff = (Buff) o;
			return name.compareTo( buff.name );
		}
	}

	private static class Offering implements Comparable
	{
		String bot;
		int price;
		int turns;
		boolean free;
		long rate;

		public Offering( String bot, int price, int turns, boolean free )
		{
			this.bot = bot;
			this.price = price;
			this.turns = turns;
			this.free = free;
			this.rate = (100 * (long)price) / turns;
		}

		public String getBot()
		{	return bot;
		}

		public int getPrice()
		{	return price;
		}

		public int getTurns()
		{	return turns;
		}

		public boolean getFree()
		{	return free;
		}

		public long getRate()
		{	return rate;
		}

		public String getLabel( boolean compact )
		{
			String rate = ff.format( (double)(this.rate / 100.0) );

			if ( compact )
				return turns + " for " + price + " (" + rate + " M/T) from " + bot;

			String isFree = free ? " (once a day)" : "";
			return turns + " turns for " + price + " meat (" + rate + " meat/turn) from " + bot + isFree;
		}

		public boolean equals( Object o )
		{
			if ( o == null || !(o instanceof Offering) )
				return false;

			Offering off = (Offering) o;
			return bot.equals( off.bot ) &&
				price == off.price &&
				turns == off.turns &&
				free == off.free;
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof Offering) )
				return -1;

			Offering off = (Offering) o;

			// First compare rates
			if ( rate < off.rate )
				return -1;

			if ( rate > off.rate )
				return 1;

			// If rates are equal compare turns

			if ( turns < off.turns )
				return -1;

			if ( turns > off.turns )
				return 1;

			return 0;
		}
	}
}
