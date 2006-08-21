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
import java.util.Comparator;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A static class which handles the officially "supported" buffbots.
 *
 * Buffbots can have their buff normalOfferings statically listed in buffs.dat
 *
 * Alternatively, if they keep their display case up-to-date with the current
 * price list - and use a "standard" format for listing prices, the bot is
 * listed in bots.dat and KoLmafia will read and parse the display case
 */

public class BuffBotDatabase extends KoLDatabase
{
	private static boolean isInitialized = false;
	private static TreeMap normalOfferings = new TreeMap();
	private static TreeMap freeOfferings = new TreeMap();

	// Variables to know whether or not the buffbot database
	// has been fully initialized during initialization.

	private static int buffBotsAvailable = 0;
	private static int buffBotsConfigured = 0;

	private static final CaseInsensitiveComparator NAME_COMPARATOR = new CaseInsensitiveComparator();

	public static boolean hasOfferings()
	{
		if ( !isInitialized )
			configureBuffBots();

		return !normalOfferings.isEmpty() || !freeOfferings.isEmpty();
	}

	public static Object [] getCompleteBotList()
	{
		ArrayList completeList = new ArrayList();
		completeList.addAll( normalOfferings.keySet() );

		Object [] philanthropic = freeOfferings.keySet().toArray();
		for ( int i = 0; i < philanthropic.length; ++i )
			if ( !completeList.contains( philanthropic[i] ) )
				completeList.add( philanthropic[i] );

		Collections.sort( completeList, NAME_COMPARATOR );
		completeList.add( 0, "" );

		return completeList.toArray();
	}

	private static class CaseInsensitiveComparator implements Comparator
	{
		public int compare( Object o1, Object o2 )
		{	return ((String)o1).compareToIgnoreCase( (String) o2 );
		}

		public boolean equals( Object o )
		{	return o instanceof CaseInsensitiveComparator;
		}
	}

	public static LockableListModel getStandardOfferings( String botName )
	{	return botName != null && normalOfferings.containsKey( botName ) ? (LockableListModel) normalOfferings.get( botName ) : new LockableListModel();
	}


	public static LockableListModel getPhilanthropicOfferings( String botName )
	{	return botName != null && freeOfferings.containsKey( botName ) ? (LockableListModel) freeOfferings.get( botName ) : new LockableListModel();
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
			BufferedReader reader = KoLDatabase.getReader( location );
			StringBuffer dataBuffer = new StringBuffer();

			try
			{
				String line;
				while ( (line = reader.readLine()) != null )
					dataBuffer.append( line );
			}
			catch ( Exception e )
			{
				// If an exception happens, just catch the
				// exception, incremental the buffbots which
				// have been parsed, and return.

				++buffBotsConfigured;
				return;
			}

			// Now, for the infamous XML parse tree.  Rather than building
			// a tree (which would probably be smarter), simply do regular
			// expression matching and assume we have a properly-structured
			// XML file -- which is assumed because of the XSLT.

			Matcher nodeMatcher = Pattern.compile( "<buffdata>(.*?)</buffdata>", Pattern.DOTALL ).matcher( dataBuffer.toString() );
			Pattern namePattern = Pattern.compile( "<name>(.*?)</name>", Pattern.DOTALL );
			Pattern pricePattern = Pattern.compile( "<price>(.*?)</price>", Pattern.DOTALL );
			Pattern turnPattern = Pattern.compile( "<turns>(.*?)</turns>", Pattern.DOTALL );
			Pattern freePattern = Pattern.compile( "<philanthropic>(.*?)</philanthropic>", Pattern.DOTALL );

			LockableListModel freeBuffs = new LockableListModel();
			LockableListModel normalBuffs = new LockableListModel();

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

					LockableListModel tester = philanthropic ? freeBuffs : normalBuffs;

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
			{
				freeBuffs.sort();
				freeOfferings.put( botName, freeBuffs );
			}

			if ( !normalBuffs.isEmpty() )
			{
				normalBuffs.sort();
				normalOfferings.put( botName, normalBuffs );
			}

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

		private int lowestBuffID;
		private String [] buffs;
		private int [] turns;

		private String stringForm;

		public Offering( String buffName, String botName, int price, int turns, boolean free )
		{
			this.buffs = new String [] { buffName };
			this.turns = new int [] { turns };
			this.lowestBuffID = ClassSkillsDatabase.getSkillID( buffName );

			this.botName = botName;
			this.price = price;
			this.free = free;

			constructStringForm();
		}

		public String getBotName()
		{	return botName;
		}

		public int getPrice()
		{	return price;
		}

		public int [] getTurns()
		{	return turns;
		}

		public int getLowestBuffID()
		{	return lowestBuffID;
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

			int skillID = ClassSkillsDatabase.getSkillID( buffName );
			if ( skillID < lowestBuffID )
				this.lowestBuffID = skillID;

			constructStringForm();
		}

		private void constructStringForm()
		{
			StringBuffer buffer = new StringBuffer();

			buffer.append( "<html>" );

			buffer.append( COMMA_FORMAT.format( price ) );
			buffer.append( " meat for " );

			if ( turns.length == 1 )
			{
				buffer.append( COMMA_FORMAT.format( turns[0] ) );
				buffer.append( " turns of " );
				buffer.append( buffs[0] );
			}
			else
			{
				buffer.append( "a Buff Pack which includes:" );

				for ( int i = 0; i < buffs.length; ++i )
				{
					buffer.append( "<br> - " );
					buffer.append( COMMA_FORMAT.format( turns[i] ) );
					buffer.append( " turns of " );
					buffer.append( buffs[i] );
				}
			}

			buffer.append( "</html>" );
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

			// First, buffpacks should come before standard offerings
			if ( (turns.length == 1 || off.turns.length == 1) && turns.length != off.turns.length )
				return off.turns.length - turns.length;

			// Next, cheaper buffpacks should come before more expensive buffpacks,
			// and philanthropic buffs compare prices as well

			// Philanthropic buffs compare price
			if ( free || turns.length > 1 || off.turns.length > 1 )
				return price - off.price;

			// Compare the ID of the lowest ID buffs
			if ( lowestBuffID != off.lowestBuffID )
				return lowestBuffID - off.lowestBuffID;

			// First compare turns
			if ( turns[0] != off.turns[0] )
				return turns[0] - off.turns[0];

			// Then, compare the names of the bots
			return botName.compareToIgnoreCase( off.botName );
		}
	}
}
