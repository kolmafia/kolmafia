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

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
	private static boolean isInitialized = false;
	private static TreeMap offerings = new TreeMap();
	private static TreeMap freeOfferings = new TreeMap();

	// Variables to know whether or not the buffbot database
	// has been fully initialized during initialization.

	private static int buffBotsAvailable = 0;
	private static int buffBotsConfigured = 0;

	public static Object [] getOfferingList()
	{	return offerings.keySet().toArray();
	}

	public static SortedListModel getOfferings( String buffName )
	{	return buffName != null && offerings.containsKey( buffName ) ? (SortedListModel) offerings.get( buffName ) : new SortedListModel();
	}

	public static boolean hasOfferings()
	{
		if ( !isInitialized )
			configureBuffBots();

		return !offerings.isEmpty() || !freeOfferings.isEmpty();
	}

	public static Object [] getPhilanthropicBotList()
	{	return freeOfferings.keySet().toArray();
	}

	public static SortedListModel getPhilanthropicOfferings( String botName )
	{	return botName != null && freeOfferings.containsKey( botName ) ? (SortedListModel) freeOfferings.get( botName ) : new SortedListModel();
	}

	private static void configureBuffBots()
	{
		if ( isInitialized )
			return;

		KoLmafia.updateDisplay( "Configuring dynamic buff prices..." );

		String [] data = null;
		BufferedReader reader = getReader( "buffbots.dat" );

		while ( (data = readData( reader )) != null )
			if ( data.length == 3 )
				(new Thread( new DynamicBotFetcher( data ) )).start();

		while ( buffBotsAvailable != buffBotsConfigured )
			KoLRequest.delay( 500 );

		KoLmafia.updateDisplay( "Buff prices fetched." );
		isInitialized = true;
	}

	private static class DynamicBotFetcher implements Runnable
	{
		private String botName, location;

		public DynamicBotFetcher( String [] data )
		{
			this.botName = data[0];
			this.location = data[2];

			++buffBotsAvailable;
			KoLmafia.registerPlayer( data[0], data[1] );
		}

		public void run()
		{
			KoLRequest request = new KoLRequest( client, location );
			request.run();

			if ( request.responseText == null )
			{
				++buffBotsConfigured;
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
			Pattern freePattern = Pattern.compile( "<philanthropic>(.*?)</philanthropic>", Pattern.DOTALL );

			SortedListModel freeBuffs = new SortedListModel();
			Matcher nameMatcher, priceMatcher, turnMatcher, freeMatcher;

			while ( nodeMatcher.find() )
			{
				String buffMatch = nodeMatcher.group(1);

				nameMatcher = namePattern.matcher( buffMatch );
				priceMatcher = pricePattern.matcher( buffMatch );
				turnMatcher = turnPattern.matcher( buffMatch );
				freeMatcher = freePattern.matcher( buffMatch );

				if ( nameMatcher.find() && priceMatcher.find() && turnMatcher.find() )
				{
					String name = nameMatcher.group(1).trim();
					int price = StaticEntity.parseInt( priceMatcher.group(1).trim() );
					int turns = StaticEntity.parseInt( turnMatcher.group(1).trim() );
					boolean philanthropic = freeMatcher.find() ? freeMatcher.group(1).trim().equals( "true" ) : false;

					SortedListModel tester = philanthropic && price < 100 ? freeBuffs : (SortedListModel) offerings.get( name );
					if ( tester == null )
					{
						offerings.put( name, new SortedListModel() );
						tester = (SortedListModel) offerings.get( name );
					}

					Offering priceMatch = null;
					Offering currentTest = null;

					for ( int i = 0; i < tester.size(); ++i )
					{
						currentTest = (Offering) tester.get(i);
						if ( currentTest.price == price )
							priceMatch = currentTest;
					}

					if ( priceMatch == null )
						tester.add( new Offering( name, botName, price, turns, philanthropic ) );
					else
						priceMatch.addBuff( name, turns );
				}
			}

			// If the bot offers some philanthropic buffs, then
			// add them to the philanthropic bot list.

			if ( !freeBuffs.isEmpty() )
				freeOfferings.put( botName, freeBuffs );

			// Now that the buffbot is configured, increment
			// the counter to notify the thread that configuration
			// has been completed for this bot.

			++buffBotsConfigured;
		}
	}

	public static class Offering implements Comparable
	{
		private String botName;
		private int price;
		private boolean free;

		private String [] buffs;
		private int [] turns;

		private long rate;
		private String stringForm;

		public Offering( String buffName, String botName, int price, int turns, boolean free )
		{
			this.buffs = new String [] { buffName };
			this.turns = new int [] { turns };

			this.botName = botName;
			this.price = price;
			this.free = free;

			this.rate = (100 * (long)price) / turns;
			constructStringForm();
		}

		public String getBotName()
		{	return botName;
		}

		public String toString()
		{	return stringForm;
		}

		private void addBuff( String buffName, int turns )
		{
			String [] tempNames = new String[ this.buffs.length + 1 ];
			int [] tempTurns = new int[ this.turns.length + 1 ];

			System.arraycopy( this.buffs, 0, tempNames, 0, this.buffs.length );
			System.arraycopy( this.turns, 0, tempTurns, 0, this.buffs.length );

			this.buffs = tempNames;
			this.turns = tempTurns;

			this.buffs[ this.buffs.length - 1 ] = buffName;
			this.turns[ this.turns.length - 1 ] = turns;

			constructStringForm();
		}

		private void constructStringForm()
		{
			StringBuffer buffer = new StringBuffer();

			buffer.append( turns[0] );
			buffer.append( " turns of " );
			buffer.append( buffs[0] );

			for ( int i = 1; i < buffs.length; ++i )
			{
				buffer.append( ", " );
				buffer.append( turns[i] );
				buffer.append( " turns of " );
				buffer.append( buffs[i] );
			}

			buffer.append( " for " );
			buffer.append( price );
			buffer.append( " meat" );

			if ( !free || price >= 100 )
			{
				buffer.append( " from " );
				buffer.append( botName );

				if ( free )
					buffer.append( " (once per day)" );
			}

			this.stringForm = buffer.toString();
		}

		public boolean equals( Object o )
		{
			if ( o == null || !(o instanceof Offering) )
				return false;

			Offering off = (Offering) o;
			return botName.equalsIgnoreCase( off.botName ) && price == off.price && turns == off.turns && free == off.free;
		}

		public GreenMessageRequest toRequest()
		{
			return new GreenMessageRequest( StaticEntity.getClient(), botName, VERSION_NAME,
				new AdventureResult( AdventureResult.MEAT, price ), false );
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof Offering) )
				return -1;

			Offering off = (Offering) o;

			// Philanthropic buffs compare price
			if ( free )
				return price - off.price;

			// First compare turns
			if ( turns[0] != off.turns[0] )
				return off.turns[0] - turns[0];

			// Then compare rates
			if ( rate != off.rate )
				return (int) (rate - off.rate);

			// Then, compare the names of the bots
			return botName.compareToIgnoreCase( off.botName );
		}
	}
}
