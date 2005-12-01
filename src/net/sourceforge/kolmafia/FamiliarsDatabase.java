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

import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.io.BufferedReader;

/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows the client to do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class FamiliarsDatabase extends KoLDatabase
{
	private static final String DEFAULT_ITEM = "steaming evil";
	private static final Integer DEFAULT_LARVA = new Integer( 666 );

	private static Map familiarByID = new TreeMap();
	private static Map familiarByName = new TreeMap();
	private static Map familiarByLarva = new TreeMap();
	private static Map familiarItemByID = new TreeMap();
	private static Map familiarByItem = new TreeMap();
	private static Map [] eventSkillByName = new TreeMap[4];

	static
	{
		for ( int i = 0; i < 4; ++i )
			eventSkillByName[i] = new TreeMap();

		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader reader = getReader( "familiars.dat" );

		String [] data;
		Integer familiarID, familiarLarva;
		String familiarName, familiarItemName;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 8 )
			{
				familiarID = Integer.valueOf( data[0] );
				familiarLarva = Integer.valueOf( data[1] );
				familiarName = getDisplayName( data[2] );

				familiarByID.put( familiarID, familiarName );
				familiarByName.put( getCanonicalName( data[2] ), familiarID );
				familiarByLarva.put( familiarLarva, familiarID );
				familiarItemName = getDisplayName( data[3] );
				familiarItemByID.put( familiarID, familiarItemName );
				familiarByItem.put( getCanonicalName( data[3] ), familiarID );

				for ( int i = 0; i < 4; ++i )
					eventSkillByName[i].put( getCanonicalName( data[2] ), Integer.valueOf( data[i+4] ) );
			}
		}
	}

	/**
	 * Temporarily adds a familiar to the familiar database.  This
	 * is used whenever KoLmafia encounters an unknown familiar on
	 * login and is designed to minimize crashing as a result.
	 */

	public static void registerFamiliar( int familiarID, String familiarName )
	{
		KoLmafia.getLogStream().println( "New familiar: \"" + familiarID + "\" (" + familiarName + ")" );

		// Because I'm intelligent, assume that both the familiar item
		// and the familiar larva are the steaming evil (for now).

		Integer dummyID = new Integer( familiarID );

		familiarByID.put( dummyID, familiarName );
		familiarByName.put( getCanonicalName(familiarName), dummyID );
		familiarByLarva.put( DEFAULT_LARVA, dummyID );
		familiarItemByID.put( dummyID, DEFAULT_ITEM );
		familiarByItem.put( getCanonicalName(DEFAULT_ITEM), dummyID );
	}

	/**
	 * Returns the name for an familiar, given its ID.
	 * @param	familiarID	The ID of the familiar to lookup
	 * @return	The name of the corresponding familiar
	 */

	public static final String getFamiliarName( int familiarID )
	{	return (String) familiarByID.get( new Integer( familiarID ) );
	}

	/**
	 * Returns the ID number for an familiar, given its larval stage.
	 * @param	larvaID	The larva stage of the familiar to lookup
	 * @return	The ID number of the corresponding familiar
	 */

	public static final FamiliarData growFamiliarLarva( int larvaID )
	{
		Object familiarID = familiarByLarva.get( new Integer( larvaID ) );
		return familiarID == null ? null : new FamiliarData( ((Integer)familiarID).intValue() );
	}

	/**
	 * Returns the ID number for an familiar, given its name.
	 * @param	substring	The name of the familiar to lookup
	 * @return	The ID number of the corresponding familiar
	 */

	public static final int getFamiliarID( String substring )
	{
		String searchString = substring.toLowerCase();
		String currentFamiliarName;

		Iterator completeFamiliars = familiarByName.keySet().iterator();
		while ( completeFamiliars.hasNext() )
		{
			currentFamiliarName = (String) completeFamiliars.next();
			if ( currentFamiliarName.indexOf( searchString ) != -1 )
			{
				Object familiarID = familiarByName.get( currentFamiliarName );
				return familiarID == null ? -1 : ((Integer)familiarID).intValue();
			}
		}

		return -1;
	}

	public static final String getFamiliarItem( int familiarID )
	{	return (String) familiarItemByID.get( new Integer( familiarID ) );
	}

	public static final int getFamiliarByItem( String item )
	{
		Object familiarID = familiarByItem.get( getCanonicalName( item ) );
		return familiarID == null ? -1 : ((Integer)familiarID).intValue();
	}

	/**
	 * Returns whether or not an item with a given name
	 * exists in the database; this is useful in the
	 * event that an item is encountered which is not
	 * tradeable (and hence, should not be displayed).
	 *
	 * @return	<code>true</code> if the item is in the database
	 */

	public static final boolean contains( String familiarName )
	{	return familiarByName.containsKey( getCanonicalName( familiarName ) );
	}

	public static Integer getFamiliarSkill( String name, int event )
	{	return (Integer) eventSkillByName[ event - 1 ].get( getCanonicalName( name ) );
	}

	/**
	 * Returns an iterator over the entrySet of familiars keyed by name
	 * @return	The Iterator
	 */
	public static Iterator iterator()
	{	return familiarByName.entrySet().iterator();
	}
}
