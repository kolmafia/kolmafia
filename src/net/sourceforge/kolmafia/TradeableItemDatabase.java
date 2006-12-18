/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
	private static IntegerArray useTypeById = new IntegerArray();
	private static IntegerArray priceById = new IntegerArray();

	private static StringArray descById = new StringArray();

	private static Map nameByItemId = new TreeMap();
	private static Map itemIdByName = new TreeMap();
	private static Map itemIdByPlural = new TreeMap();

	private static Map inebrietyById = new TreeMap();
	private static BooleanArray tradeableById = new BooleanArray();
	private static BooleanArray giftableById = new BooleanArray();

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and float-referenced: once in the name-lookup,
		// and again in the Id lookup.

		BufferedReader reader = getReader( "tradeitems.txt" );

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 5 )
			{
				int itemId = StaticEntity.parseInt( data[0] );
				Integer id = new Integer( itemId );

				useTypeById.set( itemId, StaticEntity.parseInt( data[2] ) );
				priceById.set( itemId, StaticEntity.parseInt( data[4] ) );

				itemIdByName.put( getCanonicalName( data[1] ), id );
				nameByItemId.put( id, getDisplayName( data[1] ) );

				tradeableById.set( itemId, data[3].equals( "all" ) );
				giftableById.set( itemId, data[3].equals( "all" ) || data[3].equals( "gift" ) );
			}
		}

		// Next, retrieve the description Ids using the data
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

		reader = getReader( "itemdescs.txt" );

		while ( (data = readData( reader )) != null )
		{
			boolean isDescriptionId = true;
			if ( data.length >= 2 && data[1].length() > 0 )
			{
				isDescriptionId = true;
				for ( int i = 0; i < data[1].length() && isDescriptionId; ++i )
					if ( !Character.isDigit( data[1].charAt(i) ) )
						isDescriptionId = false;

				if ( isDescriptionId )
					descById.set( StaticEntity.parseInt( data[0].trim() ), data[1] );
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

		reader = getReader( "plurals.txt" );

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 2 )
			{
				Object itemId = itemIdByName.get( data[0] );
				if ( itemId == null )
				{
					System.out.println( "Bad item name in plurals file: " + data[0] );
					continue;
				}

				itemIdByPlural.put( data[1] , itemId );
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

		reader = getReader( "inebriety.txt" );

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 2 )
			{
				Object itemId = itemIdByName.get( getCanonicalName( data[0] ) );
				if ( itemId == null )
				{
					System.out.println( "Bad item name in inebriety file: " + data[0] );
					continue;
				}

				inebrietyById.put( itemId, Integer.valueOf( data[1] ) );
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

	public static void registerItem( int itemId, String itemName )
	{
		if ( itemName == null )
			return;

		KoLmafia.updateDisplay( "Unknown item found: " + itemName + " (#" + itemId + ")" );

		useTypeById.set( itemId, 0 );
		priceById.set( itemId, -1 );
		descById.set( itemId, "" );

		Integer id = new Integer( itemId );

		itemIdByName.put( getCanonicalName( itemName ), id );
		nameByItemId.put( id, getDisplayName( itemName ) );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 * @param	itemName	The name of the item to lookup
	 * @return	The Id number of the corresponding item
	 */

	public static final int getItemId( String itemName )
	{	return getItemId( itemName, 1 );
	}

	/**
	 * Returns the Id number for an item, given its name.
	 * @param	itemName	The name of the item to lookup
	 * @param	count		How many there are
	 * @return	The Id number of the corresponding item
	 */

	public static final int getItemId( String itemName, int count )
	{
		if ( itemName == null || itemName.length() == 0 )
			return -1;

		// Get the canonical name of the item, and attempt
		// to parse based on that.

		String canonicalName = getCanonicalName( itemName );
		Object itemId = itemIdByName.get( canonicalName );

		// If the name, as-is, exists in the item database,
		// then go ahead and return the item Id.

		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Work around a specific KoL bug: the "less-than-three-shaped
		// box" is sometimes listed as a "less-than-three- shaped box"
		if ( canonicalName.equals( "less-than-three- shaped box" ) )
			return 1168;

		// If there's no more than one, don't deal with pluralization
		if ( count < 2 )
			return -1;

		// See if it's a weird pluralization with a pattern we can't
		// guess.

		itemId = itemIdByPlural.get( canonicalName );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Or maybe it's a standard plural where they just add a letter
		// to the end.

		itemId = itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a snowcone, then reverse the word order
		if ( canonicalName.startsWith( "snowcones" ) )
			return getItemId( canonicalName.split( " " )[1] + " snowcone", count );

		// Lo mein has this odd pluralization where there's a dash
		// introduced into the name when no such dash exists in the
		// singular form.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "-", " " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// The word right before the dash may also be pluralized,
		// so make sure the dashed words are recognized.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "es-", "-" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "s-", "-" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a plural form of "tooth", then make
		// sure that it's handled.  Other things which
		// also have "ee" plural forms should be clumped
		// in as well.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ee", "oo" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Also handle the plural of vortex, which is
		// "vortices" -- this should only appear in the
		// meat vortex, but better safe than sorry.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ices", "ex" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Handling of appendices (which is the plural
		// of appendix, not appendex, so it is not caught
		// by the previous test).

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ices", "ix" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Also add in a special handling for knives
		// and other things ending in "ife".

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ives", "ife" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Also add in a special handling for elves
		// and other things ending in "f".

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ves", "f" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// Also add in a special handling for staves
		// and other things ending in "aff".

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "aves", "aff" ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a pluralized form of something that
		// ends with "y", then return the appropriate
		// item Id for the "y" version.

		if ( canonicalName.endsWith( "ies" ) )
		{
			itemId = itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 3 ) + "y" );
			if ( itemId != null )
				return ((Integer)itemId).intValue();
		}

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "ies ", "y " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a pluralized form of something that
		// ends with "o", then return the appropriate
		// item Id for the "o" version.

		if ( canonicalName.endsWith( "es" ) )
		{
			itemId = itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 2 ) );
			if ( itemId != null )
				return ((Integer)itemId).intValue();
		}

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "es ", " " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a pluralized form of something that
		// ends with "an", then return the appropriate
		// item Id for the "en" version.

		itemId = itemIdByName.get( StaticEntity.singleStringReplace( canonicalName, "en ", "an " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's a standard pluralized forms, then
		// return the appropriate item Id.

		itemId = itemIdByName.get( canonicalName.replaceFirst( "([A-Za-z])s ", "$1 " ) );
		if ( itemId != null )
			return ((Integer)itemId).intValue();

		// If it's something that ends with 'i', then
		// it might be a singular ending with 'us'.

		if ( canonicalName.endsWith( "i" ) )
		{
			itemId = itemIdByName.get( canonicalName.substring( 0, canonicalName.length() - 1 ) + "us" );
			if ( itemId != null )
				return ((Integer)itemId).intValue();
		}

		// Attempt to find the item name by brute force
		// by checking every single space location.  Do
		// this recursively for best results.

		int lastSpaceIndex = canonicalName.indexOf( " " );
		return lastSpaceIndex != -1 ? getItemId( canonicalName.substring( lastSpaceIndex ).trim(), count ) : -1;
	}

	public static final int getInebriety( int itemId )
	{
		Integer inebriety = (Integer) inebrietyById.get( new Integer( itemId ) );
		return inebriety == null ? 0 : inebriety.intValue();
	}

	/**
	 * Returns the price for the item with the given Id.
	 * @return	The price associated with the item
	 */

	public static final int getPriceById( int itemId )
	{	return priceById.get( itemId );
	}

	/**
	 * Returns true if the item is tradeable, otherwise false
	 * @return	true if item is tradeable
	 */

	public static final boolean isTradeable( int itemId )
	{	return tradeableById.get( itemId );
	}

	/**
	 * Returns true if the item is tradeable, otherwise false
	 * @return	true if item is tradeable
	 */

	public static final boolean isGiftable( int itemId )
	{	return giftableById.get( itemId );
	}

	/**
	 * Returns the name for an item, given its Id number.
	 * @param	itemId	The Id number of the item to lookup
	 * @return	The name of the corresponding item
	 */

	public static final String getItemName( int itemId )
	{	return (String) nameByItemId.get( new Integer( itemId ) );
	}

	/**
	 * Returns a list of all items which contain the given
	 * substring.  This is useful for people who are doing
	 * lookups on items.
	 */

	public static final List getMatchingNames( String substring )
	{	return getMatchingNames( itemIdByName, substring );
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
	{	return getItemId( itemName ) != -1;
	}

	/**
	 * Returns whether or not the item with the given name
	 * is usable (this includes edibility).
	 *
	 * @return	<code>true</code> if the item is usable
	 */

	public static final boolean isUsable( String itemName )
	{
		int itemId = getItemId( itemName );
		if ( itemId <= 0 )
			return false;

		switch ( useTypeById.get( itemId ) )
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

	public static final int getConsumptionType( int itemId )
	{	return itemId <= 0 ? ConsumeItemRequest.NO_CONSUME : useTypeById.get( itemId );
	}

	public static final int getConsumptionType( String itemName )
	{	return getConsumptionType( getItemId( itemName ) );
	}

	/**
	 * Returns the item description Id used by the given
	 * item, given its item Id.
	 *
	 * @return	The description Id associated with the item
	 */

	public static final String getDescriptionId( int itemId )
	{	return descById.get( itemId );
	}

	/**
	 * Returns the set of items keyed by name
	 * @return	The set of items keyed by name
	 */

	public static Set entrySet()
	{	return nameByItemId.entrySet();
	}
}
