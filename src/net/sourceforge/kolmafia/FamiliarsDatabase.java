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

import java.util.Map;
import java.util.TreeMap;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;

/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows the client to do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class FamiliarsDatabase implements UtilityConstants
{
	private static final String FAMILIAR_DBASE_FILE = "familiars.dat";

	private static Map familiarByID = new TreeMap();
	private static Map familiarByName = new TreeMap();
	private static Map familiarByLarva = new TreeMap();

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader familiardata = DataUtilities.getReaderForSharedDataFile( FAMILIAR_DBASE_FILE );

		try
		{
			String line;
			while ( (line = familiardata.readLine()) != null )
			{
				StringTokenizer strtok = new StringTokenizer( line, "\t" );
				if ( strtok.countTokens() == 3 )
				{
					Integer familiarID = Integer.valueOf( strtok.nextToken() );
					String familiarLarva = TradeableItemDatabase.getItemName( Integer.parseInt( strtok.nextToken() ) );
					String familiarName = strtok.nextToken();

					familiarByID.put( familiarID, familiarName );
					familiarByName.put( familiarName.toLowerCase(), familiarID );
					familiarByLarva.put( familiarLarva, familiarID );
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
	 * Returns the name for an familiar, given its ID.
	 * @param	familiarID	The ID of the familiar to lookup
	 * @return	The name of the corresponding familiar
	 */

	public static final String getFamiliarName( int familiarID )
	{	return (String) familiarByID.get( new Integer( familiarID ) );
	}

	/**
	 * Returns the ID number for an familiar, given its larval stage.
	 * @param	larvaStage	The larva stage of the familiar to lookup
	 * @return	The ID number of the corresponding familiar
	 */

	public static final int growFamiliarItem( String larvaStage )
	{
		Object familiarID = familiarByLarva.get( larvaStage );
		return familiarID == null ? -1 : ((Integer)familiarID).intValue();
	}

	/**
	 * Returns the ID number for an familiar, given its name.
	 * @param	familiarName	The name of the familiar to lookup
	 * @return	The ID number of the corresponding familiar
	 */

	public static final int getFamiliarID( String familiarName )
	{
		Object familiarID = familiarByName.get( familiarName.toLowerCase() );
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
	{	return familiarByName.containsKey( familiarName.toLowerCase() );
	}
}