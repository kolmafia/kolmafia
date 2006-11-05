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
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows theto do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class TradeableItemDatabase extends KoLDatabase
{
	private static IntegerArray useTypeByID = new IntegerArray();
	private static IntegerArray priceByID = new IntegerArray();

	private static StringArray descByID = new StringArray();

	private static Map nameByItemID = new TreeMap();
	private static Map itemIDByName = new TreeMap();
	private static Map itemIDByPlural = new TreeMap();

	private static Map inebrietyByID = new TreeMap();
	private static BooleanArray tradeableByID = new BooleanArray();
	private static BooleanArray giftableByID = new BooleanArray();

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader reader = getReader( "tradeitems.dat" );

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 5 )
			{
				int itemID = StaticEntity.parseInt( data[0] );
				Integer id = new Integer( itemID );

				useTypeByID.set( itemID, StaticEntity.parseInt( data[2] ) );
				priceByID.set( itemID, StaticEntity.parseInt( data[4] ) );

				itemIDByName.put( getCanonicalName( data[1] ), id );
				nameByItemID.put( id, getDisplayName( data[1] ) );

				tradeableByID.set( itemID, data[3].equals( "all" ) );
				giftableByID.set( itemID, data[3].equals( "all" ) || data[3].equals( "gift" ) );
			}
		}

		// Next, retrieve the description IDs using the data
		// table present in MaxDemian's database.

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

		reader = getReader( "itemdescs.dat" );

		while ( (data = readData( reader )) != null )
		{
			boolean isDescriptionID = true;
			if ( data.length >= 2 && data[1].length() > 0 )
			{
				isDescriptionID = true;
				for ( int i = 0; i < data[1].length() && isDescriptionID; ++i )
					if ( !Character.isDigit( data[1].charAt(i) ) )
						isDescriptionID = false;

				if ( isDescriptionID )
					descByID.set( StaticEntity.parseInt( data[0].trim() ), data[1] );
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

		// Next, retrieve the table of weird pluralizations

		reader = getReader( "plurals.dat" );

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 2 )
			{
				Object itemID = itemIDByName.get( data[0] );
				if ( itemID == null )
				{
					System.out.println( "Bad item name in plurals file: " + data[0] );
					continue;
				}

				itemIDByPlural.put( data[1] , itemID );
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

		// Next, retrieve the table of inebriety

		reader = getReader( "inebriety.dat" );

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 2 )
			{
				Object itemID = itemIDByName.get( getCanonicalName( data[0] ) );
				if ( itemID == null )
				{
					System.out.println( "Bad item name in inebriety file: " + data[0] );
					continue;
				}

				inebrietyByID.put( itemID, Integer.valueOf( data[1] ) );
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

	/**
	 * Temporarily adds an item to the item database.  This
	 * is used whenever KoLmafia encounters an unknown item
	 * in the mall or in the player's inventory.
	 */

	public static void registerItem( int itemID, String itemName )
	{
		if ( itemName == null )
			return;

		KoLmafia.getDebugStream().println( "New item: <" + itemName + "> (#" + itemID + ")" );

		useTypeByID.set( itemID, 0 );
		priceByID.set( itemID, -1 );
		descByID.set( itemID, "" );

		Integer id = new Integer( itemID );

		itemIDByName.put( getCanonicalName( itemName ), id );
		nameByItemID.put( id, getDisplayName( itemName ) );
	}

	/**
	 * Returns the ID number for an item, given its name.
	 * @param	itemName	The name of the item to lookup
	 * @return	The ID number of the corresponding item
	 */

	public static final int getItemID( String itemName )
	{	return getItemID( itemName, 1 );
	}

	/**
	 * Returns the ID number for an item, given its name.
	 * @param	itemName	The name of the item to lookup
	 * @param	count		How many there are
	 * @return	The ID number of the corresponding item
	 */

	public static final int getItemID( String itemName, int count )
	{
		if ( itemName == null || itemName.length() == 0 )
			return -1;

		// Get the canonical name of the item, and attempt
		// to parse based on that.

		String canonicalName = getCanonicalName( itemName );
		Object itemID = itemIDByName.get( canonicalName );

		// If the name, as-is, exists in the item database,
		// then go ahead and return the item ID.

		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// If there's no more than one, don't deal with pluralization
		if ( count < 2 )
			return -1;

		// See if it's a weird pluralization with a pattern we can't
		// guess.

		itemID = itemIDByPlural.get( canonicalName );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// Or maybe it's a standard plural where they just add a letter
		// to the end.

		itemID = itemIDByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// Work around a specific KoL bug: the "less-than-three-shaped
		// box" is sometimes listed as a "less-than-three- shaped box"
		if ( canonicalName.equals( "less-than-three- shaped box" ) )
			return 1168;

		// If it's a snowcone, then reverse the word order
		if ( canonicalName.startsWith( "snowcones" ) )
			return getItemID( canonicalName.split( " " )[1] + " snowcone", count );

		// The word right before the dash may also be pluralized,
		// so make sure the dashed words are recognized.

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "es-", "-" ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "s-", "-" ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// If it's a plural form of "tooth", then make
		// sure that it's handled.  Other things which
		// also have "ee" plural forms should be clumped
		// in as well.

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "ee", "oo" ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// Also handle the plural of vortex, which is
		// "vortices" -- this should only appear in the
		// meat vortex, but better safe than sorry.

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "ices", "ex" ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// Handling of appendices (which is the plural
		// of appendix, not appendex, so it is not caught
		// by the previous test).

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "ices", "ix" ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// Also add in a special handling for knives
		// and other things ending in "ife".

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "ives", "ife" ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// Also add in a special handling for elves
		// and other things ending in "f".

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "ves", "f" ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// Also add in a special handling for staves
		// and other things ending in "aff".

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "aves", "aff" ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// If it's a pluralized form of something that
		// ends with "y", then return the appropriate
		// item ID for the "y" version.

		if ( canonicalName.endsWith( "ies" ) )
		{
			itemID = itemIDByName.get( canonicalName.substring( 0, canonicalName.length() - 3 ) + "y" );
			if ( itemID != null )
				return ((Integer)itemID).intValue();
		}

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "ies ", "y " ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// If it's a pluralized form of something that
		// ends with "o", then return the appropriate
		// item ID for the "o" version.

		if ( canonicalName.endsWith( "es" ) )
		{
			itemID = itemIDByName.get( canonicalName.substring( 0, canonicalName.length() - 2 ) );
			if ( itemID != null )
				return ((Integer)itemID).intValue();
		}

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "es ", " " ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// If it's a pluralized form of something that
		// ends with "an", then return the appropriate
		// item ID for the "en" version.

		itemID = itemIDByName.get( StaticEntity.singleStringReplace( canonicalName, "en ", "an " ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// If it's a standard pluralized forms, then
		// return the appropriate item ID.

		itemID = itemIDByName.get( canonicalName.replaceFirst( "([A-Za-z])s ", "$1 " ) );
		if ( itemID != null )
			return ((Integer)itemID).intValue();

		// If it's something that ends with 'i', then
		// it might be a singular ending with 'us'.

		if ( canonicalName.endsWith( "i" ) )
		{
			itemID = itemIDByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) + "us" );
			if ( itemID != null )
				return ((Integer)itemID).intValue();
		}

		// Attempt to find the item name by brute force
		// by checking every single space location.  Do
		// this recursively for best results.

		int lastSpaceIndex = canonicalName.indexOf( " " );
		return lastSpaceIndex != -1 ? getItemID( canonicalName.substring( lastSpaceIndex ).trim(), count ) : -1;
	}

	public static final int getInebriety( int itemID )
	{
		Integer inebriety = (Integer) inebrietyByID.get( new Integer( itemID ) );
		return inebriety == null ? 0 : inebriety.intValue();
	}

	/**
	 * Returns the price for the item with the given ID.
	 * @return	The price associated with the item
	 */

	public static final int getPriceByID( int itemID )
	{	return priceByID.get( itemID );
	}

	/**
	 * Returns true if the item is tradeable, otherwise false
	 * @return	true if item is tradeable
	 */

	public static final boolean isTradeable( int itemID )
	{	return tradeableByID.get( itemID );
	}

	/**
	 * Returns true if the item is tradeable, otherwise false
	 * @return	true if item is tradeable
	 */

	public static final boolean isGiftable( int itemID )
	{	return giftableByID.get( itemID );
	}

	/**
	 * Returns the name for an item, given its ID number.
	 * @param	itemID	The ID number of the item to lookup
	 * @return	The name of the corresponding item
	 */

	public static final String getItemName( int itemID )
	{	return (String) nameByItemID.get( new Integer( itemID ) );
	}

	/**
	 * Returns a list of all items which contain the given
	 * substring.  This is useful for people who are doing
	 * lookups on items.
	 */

	public static final List getMatchingNames( String substring )
	{	return getMatchingNames( itemIDByName, substring );
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
	{	return getItemID( itemName ) != -1;
	}

	/**
	 * Returns whether or not the item with the given name
	 * is usable (this includes edibility).
	 *
	 * @return	<code>true</code> if the item is usable
	 */

	public static final boolean isUsable( String itemName )
	{
		int itemID = getItemID( itemName );
		if ( itemID <= 0 )
			return false;

		switch ( useTypeByID.get( itemID ) )
		{
		case ConsumeItemRequest.CONSUME_EAT:
		case ConsumeItemRequest.CONSUME_DRINK:
		case ConsumeItemRequest.CONSUME_USE:
		case ConsumeItemRequest.CONSUME_MULTIPLE:
		case ConsumeItemRequest.GROW_FAMILIAR:
		case ConsumeItemRequest.CONSUME_ZAP:
		case ConsumeItemRequest.CONSUME_RESTORE:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns the kind of consumption associated with an item
	 *
	 * @return	The consumption associated with the item
	 */

	public static final int getConsumptionType( int itemID )
	{	return itemID <= 0 ? ConsumeItemRequest.NO_CONSUME : useTypeByID.get( itemID );
	}

	public static final int getConsumptionType( String itemName )
	{	return getConsumptionType( getItemID( itemName ) );
	}

	/**
	 * Returns the item description ID used by the given
	 * item, given its item ID.
	 *
	 * @return	The description ID associated with the item
	 */

	public static final String getDescriptionID( int itemID )
	{	return descByID.get( itemID );
	}

	/**
	 * Returns the set of items keyed by name
	 * @return	The set of items keyed by name
	 */

	public static Set entrySet()
	{	return nameByItemID.entrySet();
	}
}
