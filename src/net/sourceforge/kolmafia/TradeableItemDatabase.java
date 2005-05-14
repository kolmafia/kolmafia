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

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Map;
import java.util.TreeMap;
import java.util.StringTokenizer;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import net.java.dev.spellcast.utilities.DataUtilities;

/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows the client to do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class TradeableItemDatabase
{
	private static final String ITEM_DBASE_FILE = "tradeitems.dat";
	public static final int ITEM_COUNT = 1300;

	private static String [] itemByID = new String[ ITEM_COUNT ];
	private static int [] consumptionID = new int[ ITEM_COUNT ];
	private static int [] priceByID = new int[ ITEM_COUNT ];
	private static Map itemByName = new TreeMap();

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader itemdata = DataUtilities.getReaderForSharedDataFile( ITEM_DBASE_FILE );

		try
		{
			String line;
			while ( (line = itemdata.readLine()) != null )
			{
				StringTokenizer strtok = new StringTokenizer( line, "\t" );
				if ( strtok.countTokens() == 4 )
				{
					int itemID = Integer.parseInt( strtok.nextToken() );
					String itemName = strtok.nextToken();

					consumptionID[ itemID ] = Integer.parseInt( strtok.nextToken() );
					priceByID[ itemID ] = Integer.parseInt( strtok.nextToken() );

					itemByID[ itemID ] = itemName;
					itemByName.put( itemName.toLowerCase(), new Integer( itemID ) );
				}
			}
		}
		catch ( IOException e )
		{
			// If an IOException is thrown, that means there was
			// a problem reading in the appropriate data file;
			// that means that no item database exists.  This
			// exception is strange enough that it won't be
			// handled at the current time.
		}
	}

	/**
	 * Temporarily adds an item to the item database.  This
	 * is used whenever KoLmafia encounters an unknown item
	 * in the mall or in the player's inventory.
	 */

	public static void registerItem( int itemID, String itemName )
	{
		consumptionID[ itemID ] = 0;
		priceByID[ itemID ] = 0;

		itemByID[ itemID ] = itemName;
		itemByName.put( itemName.toLowerCase(), new Integer( itemID ) );
	}

	/**
	 * Returns the ID number for an item, given its name.
	 * @param	itemName	The name of the item to lookup
	 * @return	The ID number of the corresponding item
	 */

	public static final int getItemID( String itemName )
	{
		Object itemID = itemByName.get( itemName.toLowerCase().replaceAll( "ñ", "&ntilde;" ).replaceAll( "©", "&trade;" ) );
		return itemID == null ? -1 : ((Integer)itemID).intValue();
	}

	/**
	 * Returns the price for the item with the given ID.
	 * @return	The price associated with the item
	 */

	public static final int getPriceByID( int itemID )
	{	return itemID < 0 ? 0 : priceByID[ itemID ];
	}

	/**
	 * Returns the name for an item, given its ID number.
	 * @param	itemID	The ID number of the item to lookup
	 * @return	The name of the corresponding item
	 */

	public static final String getItemName( int itemID )
	{
		return itemID < 0 || itemID > ITEM_COUNT ? null : itemByID[ itemID ] == null ? null :
			itemByID[ itemID ].replaceAll( "&ntilde;", "ñ" ).replaceAll( "&trade;", "©" );
	}

	/**
	 * Returns a list of all items which contain the given
	 * substring.  This is useful for people who are doing
	 * lookups on items.
	 */

	public static final List getMatchingNames( String substring )
	{
		List substringList = new ArrayList();
		String searchString = substring.toLowerCase().replaceAll( "\"", "" );
		String currentItemName;

		Iterator completeItems = itemByName.keySet().iterator();
		while ( completeItems.hasNext() )
		{
			currentItemName = (String) completeItems.next();
			if ( currentItemName.indexOf( searchString ) != -1 )
				substringList.add( getItemName( getItemID( currentItemName ) ) );
		}

		return substringList;
	}

	/**
	 * Returns whether or not an item with a given name
	 * exists in the database; this is useful in the
	 * event that an item is encountered which is not
	 * tradeable (and hence, should not be displayed).
	 *
	 * @return	<code>true</code> if the item is in the database
	 */

	public static final boolean contains( String itemName )
	{	return itemByName.containsKey( itemName.toLowerCase() );
	}

	/**
	 * Returns whether or not the item with the given name
	 * is usable (this includes edibility).
	 *
	 * @return	<code>true</code> if the item is usable
	 */

	public static final boolean isUsable( String itemName )
	{
		int itemID = getItemID( itemName.replaceAll( "ñ", "&ntilde;" ).replaceAll( "©", "&trade;" ) );
		return itemID == -1 ? false : consumptionID[ itemID ] != ConsumeItemRequest.NO_CONSUME;
	}

	/**
	 * Returns the kind of consumption associated with the
	 * item with the given name.
	 *
	 * @return	The consumption associated with the item
	 */

	public static final int getConsumptionType( String itemName )
	{
		int itemID = getItemID( itemName );
		return itemID == -1 ? ConsumeItemRequest.NO_CONSUME : consumptionID[ itemID ];
	}

	public static void main( String [] args ) throws Exception
	{
		BufferedReader buf = new BufferedReader( new InputStreamReader( System.in ) );
		String line;

		while ( (line = buf.readLine()) != null )
		{
			List matchingNames = getMatchingNames( line );
			for ( int i = 0; i < matchingNames.size(); ++i )
				System.out.println( getItemID( (String) matchingNames.get(i) ) + ": " + matchingNames.get(i) );

			if ( matchingNames.size() == 0 )
				System.out.println( "No matching items found." );
		}
	}
}