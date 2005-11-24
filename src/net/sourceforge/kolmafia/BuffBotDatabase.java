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
import java.util.ArrayList;


/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows the client to do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class BuffBotDatabase extends KoLDatabase
{
	// List of { "bot", skill, { { price, turns }, ... } }
	private static List buffList;

	static
	{
		// Initialize data
		buffList = new ArrayList();

		// Current bot/skill pair we are working with
		Object [] current = null;

		// Open the data file
		BufferedReader reader = getReader( "buffbots.dat" );

		// Read a line at a time

		String [] data;
		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 4 )
			{
				// Get the fields

				String bot = data[0];

				// Validate the fields

				int skill = ClassSkillsDatabase.getSkillID( data[1] );
				if ( skill < 0 )
				{
					System.out.println( "Unknown buff: " + data[1] );
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

				// Massage the data into a usable form.

				// Make a new entry for each bot/buff pair.
				if ( current == null ||
				     !bot.equals( (String)current[0] ) ||
				     skill != ((Integer)current[1]).intValue() )
				{
					current = new Object[3];
					current[0] = bot;
					current[1] = new Integer( skill );
					current[2] = new ArrayList();

					buffList.add( current );
				}

				// Make a new price/turn pair
				Object [] pair = new Object[2];
				pair[0] = new Integer( price );
				pair[1] = new Integer( turns );

				// Append it to current entry
				ArrayList pairs = (ArrayList)current[2];
				pairs.add( pair );
			}
		}
	}

	public static final int buffCount()
	{	return buffList.size();
	}

	public static Object [] getBuff( int index )
	{	return (Object [])buffList.get( index );
	}

	public static String getBuffBot( int index )
	{
		Object [] buff = getBuff( index );
		String bot = (String)buff[0];
		return bot;
	}

	public static String getBuffName( int index )
	{
		Object [] buff = getBuff( index );
		int skill = ((Integer)buff[1]).intValue();
		String name = ClassSkillsDatabase.getSkillName( skill );
		return name;
	}

	public static int getBuffPrice( int index1, int index2 )
	{
		Object [] buff = getBuff( index1 );
		ArrayList pairs = (ArrayList)buff[2];
		Object [] pair = (Object [])pairs.get( index2 );
		int price = ((Integer)pair[0]).intValue();
		return price;
	}

	public static int getBuffTurns( int index1, int index2 )
	{
		Object [] buff = getBuff( index1 );
		ArrayList pairs = (ArrayList)buff[2];
		Object [] pair = (Object [])pairs.get( index2 );
		int turns = ((Integer)pair[1]).intValue();
		return turns;
	}

	private static void printBuffs()
	{
		int count = buffCount();
		System.out.println( count + " available buffs." );
		for ( int i = 0; i < count; ++i )
		{
			Object [] buff = getBuff( i );
			String bot = (String)buff[0];
			int skill = ((Integer)buff[1]).intValue();
			String name = ClassSkillsDatabase.getSkillName( skill );
			ArrayList pairs = (ArrayList)buff[2];

			System.out.println( "Bot " + bot + " provides " + name );
			for (int j = 0; j < pairs.size(); ++j )
			{
				Object [] pair = (Object [])pairs.get( j );
				int price = ((Integer)pair[0]).intValue();
				int turns = ((Integer)pair[1]).intValue();
				System.out.println( "  " + turns + " turns for " + price + " meat");
			}
		}
	}
}
