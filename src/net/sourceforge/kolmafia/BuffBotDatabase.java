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

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/**
 * A static class which handles the officially "supported" buffbots.
 *
 * Buffbots can have their buff offerings statically listed in buffs.dat
 *
 * Alternatively, if they keep their display case up-to-date with the current
 * price list - and use a "standard" format for listing prices, the bot is
 * listed in bots.dat and KoLmafia will read and parse the display case
 */

public class BuffBotDatabase extends KoLDatabase
{
	// All buffs: skill #, display name, abbreviation
	public static final Object [][] ABBREVIATIONS =
	{
		// Accordion Thief Buffs
		{ new Integer(6003), "Antiphon" },
		{ new Integer(6004), "Madrigal" },
		{ new Integer(6005), "Celerity" },
		{ new Integer(6006), "Polka" },
		{ new Integer(6007), "Melody" },
		{ new Integer(6008), "Ballad" },
		{ new Integer(6009), "Anthem" },
		{ new Integer(6010), "Phat Loot" },
		{ new Integer(6011), "Psalm" },
		{ new Integer(6012), "Symphony" },
		{ new Integer(6013), "Shanty" },
		{ new Integer(6014), "Ode" },
		{ new Integer(6015), "Sneakiness" },
		{ new Integer(6016), "Cantata" },
		{ new Integer(6017), "Aria" },

		// Sauceress Buffs
		{ new Integer(4007), "Elemental" },
		{ new Integer(4008), "Jala" },
		{ new Integer(4011), "Jaba" },

		// Turtle Tamer Buffs
		{ new Integer(2007), "Ghostly" },
		{ new Integer(2008), "Fortitude" },
		{ new Integer(2009), "Empathy" },
		{ new Integer(2010), "Tenacity" },
		{ new Integer(2012), "Astral" },

		// Oddball Buffs
		{ new Integer(3), "Smile" }
	};

	// List of supported buffbots with parsable display cases
	private static ArrayList bots = new ArrayList();

	// buffbots.dat lists dynamically configured buffbots
	static
	{
		// Open the data file
		BufferedReader reader = getReader( "buffbots.dat" );

		// Read a line at a time

		String [] data;
		while ( (data = readData( reader )) != null )
			if ( data.length == 3 )
				bots.add( data );
	}

	// Buffs obtainable from all public buffbots
	private static boolean isInitialized = false;
	private static BuffList allBots = new BuffList();

	public static int buffCount()
	{	return allBots.buffCount();
	}

	public static String getBuffName( int index )
	{	return allBots.getBuffName( index );
	}

	public static String getBuffAbbreviation( int index )
	{	return allBots.getBuffAbbreviation( index );
	}

	public static int getBuffOfferingCount( int index )
	{	return allBots.getBuffOfferingCount( index );
	}

	public static String getBuffBot( int index1, int index2 )
	{	return allBots.getBuffBot( index1, index2 );
	}

	public static int getBuffPrice( int index1, int index2 )
	{	return allBots.getBuffPrice( index1, index2 );
	}

	public static int getBuffTurns( int index1, int index2 )
	{	return allBots.getBuffTurns( index1, index2 );
	}

	public static long getBuffRate( int index1, int index2 )
	{	return allBots.getBuffRate( index1, index2 );
	}

	public static boolean getBuffFree( int index1, int index2 )
	{	return allBots.getBuffFree( index1, index2 );
	}

	public static String getBuffLabel( int index1, int index2 )
	{	return allBots.getBuffLabel( index1, index2 );
	}

	public static void configureBuffBots()
	{
		if ( isInitialized )
			return;

		// List of all bots includes static + dynamic
		allBots = new BuffList();

		KoLmafia.updateDisplay( "Configuring dynamic buff prices..." );

		// Iterate over list of bots and configure each one in a
		// separate thread; since it's all located on separate
		// servers, it's possible to do this.

		int botCount = bots.size();
		DynamicBotFetcher [] botfetches = new DynamicBotFetcher[ botCount ];

		for ( int i = 0; i < botCount; ++i )
		{
			String [] entry = (String []) bots.get(i);

			KoLmafia.registerPlayer( entry[1], entry[2] );
			botfetches[i] = new DynamicBotFetcher( entry[0], entry[2] );
			(new Thread( botfetches[i] )).start();
		}

		// Iterate over the fetching runnables to see if the
		// configuration is complete.  Continue waiting until
		// it is complete.

		boolean configurationComplete = false;
		while ( !configurationComplete )
		{
			KoLRequest.delay( 500 );
			configurationComplete = true;
			for ( int i = 0; i < botCount; ++i )
				configurationComplete &= botfetches[i].completedFetch;
		}

		KoLmafia.updateDisplay( "Buff prices fetched." );
		isInitialized = true;
	}

	private static class DynamicBotFetcher implements Runnable
	{
		private String name, location;
		private boolean completedFetch;

		public DynamicBotFetcher( String name, String location )
		{
			this.name = name;
			this.location = location;
			this.completedFetch = false;
		}

		public void run()
		{
			KoLRequest request = new KoLRequest( client, location );
			request.run();

			if ( request.responseText == null )
			{
				completedFetch = true;
				return;
			}

			// Now, for the infamous XML parse tree.  Rather than building
			// a tree (which would probably be smarter), simply do regular
			// expression matching and assume we have a properly-structured
			// XML file -- which is assumed because of the XSLT.

			Matcher nodeMatcher = Pattern.compile( "<buffdata>(.*?)</buffdata>", Pattern.DOTALL ).matcher( request.responseText );
			Pattern namePattern = Pattern.compile( "<name>(.*?)</name>", Pattern.DOTALL );
			Pattern pricePattern = Pattern.compile( "<price>(.*?)</price>", Pattern.DOTALL );
			Pattern turnPattern = Pattern.compile( "<turns>(.*?)</turns>", Pattern.DOTALL );
			Pattern oncePattern = Pattern.compile( "<philanthropic>(.*?)</philanthropic>", Pattern.DOTALL );

			BuffList buffs = new BuffList();
			Matcher nameMatcher, priceMatcher, turnMatcher, onceMatcher;

			while ( nodeMatcher.find() )
			{
				String buffMatch = nodeMatcher.group(1);

				nameMatcher = namePattern.matcher( buffMatch );
				priceMatcher = pricePattern.matcher( buffMatch );
				turnMatcher = turnPattern.matcher( buffMatch );
				onceMatcher = oncePattern.matcher( buffMatch );

				if ( nameMatcher.find() && priceMatcher.find() && turnMatcher.find() )
				{
					buffs.findAbbreviation( nameMatcher.group(1).trim() ).addOffering(
						new Offering( name, StaticEntity.parseInt( priceMatcher.group(1).trim() ),
						StaticEntity.parseInt( turnMatcher.group(1).trim() ),
						onceMatcher.find() ? onceMatcher.group(1).trim().equals( "true" ) : false ) );
				}
			}

			allBots.addBuffList( buffs );
			completedFetch = true;
		}
	}

/*
	private static void textConfigure( String name, String data )
	{
		// Look for start tag
		int start = data.indexOf( "CONDENSED PRICE LIST" );
		if ( start < 0 )
			return;

		// Look for end tag
		int end = data.indexOf( "</td></tr></table>", start );
		if ( end < 0 )
			return;

		// Focus on the data of interest
		data = data.substring( start, end );

		// Split it into lines
		String lines[] = data.split( "<br>" );

		// Make a BuffList to store what we parse
		BuffList buffs = new BuffList();

		// Parse data and add buffs to list

		Buff current = null;
		for (int i = 0; i < lines.length; ++i )
		{
			String line = lines[i];

			if ( line.length() == 0 )
				continue;

			// If the line doesn't start with a digit, assume this
			// is a buff abbreviation
			if ( !Character.isDigit( line.charAt( 0 ) ) )
			{
				// Look up abbreviated buff name
				current = buffs.findAbbreviation( line.trim() );
				continue;
			}

			// If the line does start with a digit, it's a buff
			// price. Make sure we have a buff.
			if ( current == null )
				continue;

			// Parse standard format: <turns>-<price>(*)?
			int hyphen = line.indexOf( "-" );
			if ( hyphen < 0 )
				continue;

			int star = line.indexOf( "*" );
			String num1 = line.substring( 0, hyphen );
			String num2 = ( star > 0 ) ? line.substring( hyphen + 1, star ) : line.substring( hyphen + 1 );

			int turns = StaticEntity.parseInt( num1 );
			int price = StaticEntity.parseInt( num2 );

			current.addOffering( new Offering( name, price, turns, star > 0 ) );
		}

		// Add this bot's buffs to the global list
		allBots.addBuffList( buffs );
	}
*/
	private static class BuffList
	{
		private ArrayList buffs;

		public BuffList()
		{
			this.buffs = new ArrayList();
			for ( int i = 0; i < ABBREVIATIONS.length; ++i )
				buffs.add( new Buff( i ) );
		}

		public Buff findBuff( String name )
		{
			int skill = ClassSkillsDatabase.getSkillID( name );
			Buff [] buffArray = new Buff[ buffs.size() ];
			buffs.toArray( buffArray );

			for ( int i = 0; i < buffArray.length; ++i )
				if ( skill == buffArray[i].getSkill() )
					return buffArray[i];

			return null;
		}

		public Buff findAbbreviation( String name )
		{

			Buff [] buffArray = new Buff[ buffs.size() ];
			buffs.toArray( buffArray );

			for ( int i = 0; i < buffArray.length; ++i )
				if ( name.indexOf( buffArray[i].getAbbreviation() ) != -1 )
					return buffArray[i];

			return null;
		}

		public int buffCount()
		{	return buffs.size();
		}

		private Buff getBuff( int index )
		{	return (Buff) buffs.get( index );
		}

		public String getBuffName( int index )
		{	return getBuff( index ).getName();
		}

		public String getBuffAbbreviation( int index )
		{	return getBuff( index ).getAbbreviation();
		}

		public int getBuffOfferingCount( int index )
		{	return getBuff( index ).getOfferingCount();
		}

		public Offering getBuffOffering( int index1, int index2 )
		{	return getBuff( index1 ).getOffering( index2 );
		}

		public String getBuffBot( int index1, int index2 )
		{	return getBuff( index1 ).getOfferingBot( index2 );
		}

		public int getBuffPrice( int index1, int index2 )
		{	return getBuff( index1 ).getOfferingPrice( index2 );
		}

		public int getBuffTurns( int index1, int index2 )
		{	return getBuff( index1 ).getOfferingTurns( index2 );
		}

		public long getBuffRate( int index1, int index2 )
		{	return getBuff( index1 ).getOfferingRate( index2 );
		}

		public boolean getBuffFree( int index1, int index2 )
		{	return getBuff( index1 ).getOfferingFree( index2 );
		}

		public String getBuffLabel( int index1, int index2 )
		{	return getBuff( index1 ).getOfferingLabel( index2 );
		}

		public void addBuffList( BuffList bl )
		{
			// All BuffList objects have the same number of
			// Buff structures in the sorted list.

			int buffCount = buffCount();

			for ( int i = 0; i < buffCount; ++i )
			{
				Buff buff = getBuff( i );
				int offerings = bl.getBuffOfferingCount( i );

				for ( int j = 0; j < offerings; ++j )
					buff.addOffering( bl.getBuffOffering( i, j ) );
			}
		}

		private void print()
		{
			int count = buffCount();
			System.out.println( count + " available buffs." );
			for ( int i = 0; i < count; ++i )
			{
				String name = getBuffName( i );
				String abbreviation = getBuffAbbreviation( i );
				System.out.println( name + " (" + abbreviation + ")" );

				int offerings = getBuffOfferingCount( i );
				for (int j = 0; j < offerings; ++j )
					System.out.println( "  " + getBuffLabel( i, j ) );
			}
		}
	}

	private static class Buff
	{
		private int skill;
		private String name;
		private String abbreviation;
		private ArrayList offerings;

		public Buff( int index )
		{
			Object [] data = ABBREVIATIONS[index];
			this.skill = ((Integer)data[0]).intValue();
			this.name = ClassSkillsDatabase.getSkillName( skill );
			this.abbreviation = (String)data[1];
			this.offerings = new ArrayList();
		}

		public void addOffering( Offering off )
		{
			offerings.add( off );
			Collections.sort( offerings );
		}

		public int getSkill()
		{	return skill;
		}

		public String getName()
		{	return name;
		}

		public String getAbbreviation()
		{	return abbreviation;
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
		{	return getOffering( index ).getRate();
		}

		public String getOfferingLabel( int index )
		{	return getOffering( index ).getLabel();
		}

		public boolean equals( Object o )
		{
			if ( !(o instanceof Buff) || o == null )
				return false;

			Buff buff = (Buff) o;
			return abbreviation.equals( buff.abbreviation );
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

		public String getLabel()
		{	return turns + " turns for " + price + " meat from " + bot + (free ? " (once a day)" : "");
		}

		public boolean equals( Object o )
		{
			if ( o == null || !(o instanceof Offering) )
				return false;

			Offering off = (Offering) o;
			return bot.equals( off.bot ) && price == off.price && turns == off.turns && free == off.free;
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof Offering) )
				return -1;

			Offering off = (Offering) o;

			// First compare turns
			if ( turns != off.turns )
				return off.turns - turns;

			// Then compare rates
			if ( rate != off.rate )
				return (int) (rate - off.rate);

			// Then, compare the names of the bots
			return bot.compareToIgnoreCase( off.bot );
		}
	}
}
