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
import net.java.dev.spellcast.utilities.SortedListModel;


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
	private static final Object [][] buffData =
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
		{ new Integer(4008), "Jalapeno" },
		{ new Integer(4011), "Jabanero" },

		// Turtle Tamer Buffs
		{ new Integer(2007), "Ghostly" },
		{ new Integer(2008), "Fortitude" },
		{ new Integer(2009), "Empathy" },
		{ new Integer(2010), "Tenacity" },
		{ new Integer(2012), "Astral" }
	};

	// Buffs obtainable from statically configured buffbots
	private static BuffList staticBots;

	// buffs.dat configures the statically configured buffbots
	static
	{
		// Initialize data.

		staticBots = new BuffList();

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
				Buff buff = staticBots.findBuff( skillName );

				if ( buff == null )
				{
					System.out.println( "Unknown buff: " + skillName );
					continue;
				}

				int price;
				try
				{
					price = df.parse( data[2] ).intValue();
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.
					
					StaticEntity.printStackTrace( e, "Bad price: " + data[2] );
					continue;
				}

				int turns;
				try
				{
					turns = df.parse( data[3] ).intValue();
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.
					
					StaticEntity.printStackTrace( e, "Bad turns: " + data[3] );
					continue;
				}

				int free;
				try
				{
					free = df.parse( data[4] ).intValue();
				}
				catch ( Exception e )
				{
					// This should not happen.  Therefore, print
					// a stack trace for debug purposes.
					
					StaticEntity.printStackTrace( e, "Bad free: " + data[2] );
					continue;
				}

				// Add this offering to the buff
				buff.addOffering( new Offering( bot, price, turns, free != 0 ) );
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

	// List of supported buffbots with parsable display cases
	private static ArrayList bots;

	// buffbots.dat lists dynamically configured buffbots
	static
	{
		// Initialize data.

		bots = new ArrayList();

		// Open the data file
		BufferedReader reader = getReader( "buffbots.dat" );

		// Read a line at a time

		String [] data;
		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 2 )
			{
				// { bot name, bot ID }
				String [] pair = new String[2];

				pair[0] = data[0];
				pair[1] = data[1];

				bots.add( pair );
			}
		}
	}

	// Buffs obtainable from all public buffbots
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
		// List of all bots includes static + dynamic
		allBots = new BuffList();
		allBots.addBuffList( staticBots );

		DEFAULT_SHELL.updateDisplay( "Configuring dynamic buff prices..." );

		// Iterate over list of bots and configure each one
		int botCount = bots.size();

		for ( int i = 0; i < botCount; ++i )
		{
			String [] entry = (String [])bots.get(i);
			String name = entry[0];
			String id = entry[1];

			configureDynamicBot( client, name, id );
		}

		DEFAULT_SHELL.updateDisplay( "Buff prices fetched." );
	}

	private static void configureDynamicBot( KoLmafia client, String name, String id )
	{
		// First, check if the bot is online by sending
		// a chat request.

		KoLRequest request = new ChatRequest( client, "", "/whois " + id );
		request.run();

System.out.println( request.responseText );

		if ( request.responseText.indexOf( "currently online" ) == -1 )
		{
			client.updateDisplay( name + " is not currently online." );
			return;
		}

		DEFAULT_SHELL.updateDisplay( "Fetching buff prices from " + name + "..." );

		request = new KoLRequest( client, "displaycollection.php" );
		request.addFormField( "who", id );
		request.run();

		String data = request.responseText;

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

			try
			{
				int turns = Integer.parseInt( num1 );
				int price = Integer.parseInt( num2 );

				current.addOffering( new Offering( name, price, turns, star > 0 ) );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e );
				continue;
			}
		}

		// Add this bot's buffs to the global list
		allBots.addBuffList( buffs );
	}

	private static class BuffList
	{
		private SortedListModel buffs;

		public BuffList()
		{
			this.buffs = new SortedListModel();
			for ( int i = 0; i < buffData.length; ++i )
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

	private static class Buff implements Comparable
	{
		int skill;
		String name;
		String abbreviation;
		private SortedListModel offerings;

		public Buff( int index )
		{
			Object [] data = buffData[index];
			this.skill = ((Integer)data[0]).intValue();
			this.name = ClassSkillsDatabase.getSkillName( skill );
			this.abbreviation = (String)data[1];
			this.offerings = new SortedListModel();
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

		public int compareTo( Object o )
		{
			if ( !(o instanceof Buff) || o == null )
				return -1;

			Buff buff = (Buff) o;
			return abbreviation.compareTo( buff.abbreviation );
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
