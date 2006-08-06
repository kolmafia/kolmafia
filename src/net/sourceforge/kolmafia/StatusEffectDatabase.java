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
import java.util.Set;
import java.util.List;
import java.util.Collection;

/**
 * A static class which retrieves all the tradeable items available in
 * the Kingdom of Loathing and allows the client to do item look-ups.
 * The item list being used is a parsed and resorted list found on
 * Ohayou's Kingdom of Loathing website.  In order to decrease server
 * load, this item list is stored within the JAR archive.
 */

public class StatusEffectDatabase extends KoLDatabase
{
	private static Map effectByID = new TreeMap();
	private static Map effectByName = new TreeMap();

	static
	{
		// This begins by opening up the data file and preparing
		// a buffered reader; once this is done, every line is
		// examined and double-referenced: once in the name-lookup,
		// and again in the ID lookup.

		BufferedReader reader = getReader( "statuseffects.dat" );

		String [] data;

		String name;
		Integer effectID;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 2 )
			{
				name = data[1];
				effectID = Integer.valueOf( data[0] );

				effectByID.put( effectID, getDisplayName( name ) );
				effectByName.put( getCanonicalName( name ), effectID );
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
	 * Returns the name for an effect, given its ID.
	 * @param	effectID	The ID of the effect to lookup
	 * @return	The name of the corresponding effect
	 */

	public static final String getEffectName( int effectID )
	{	return effectID == -1 ? "Unknown effect" : getDisplayName( (String) effectByID.get( new Integer( effectID ) ) );
	}

	/**
	 * Returns the ID number for an effect, given its name.
	 * @param	effectName	The name of the effect to lookup
	 * @return	The ID number of the corresponding effect
	 */

	public static final int getEffectID( String effectName )
	{
		Object effectID = effectByName.get( getCanonicalName( effectName ) );
		return effectID == null ? -1 : ((Integer)effectID).intValue();
	}

	/**
	 * Returns the set of status effects keyed by ID
	 * @return	The set of status effects keyed by ID
	 */

	public static Set entrySet()
	{	return effectByID.entrySet();
	}

	public static Collection values()
	{	return effectByID.values();
	}

	/**
	 * Returns whether or not an item with a given name
	 * exists in the database; this is useful in the
	 * event that an item is encountered which is not
	 * tradeable (and hence, should not be displayed).
	 *
	 * @param	effectName	The name of the effect to lookup
	 * @return	<code>true</code> if the item is in the database
	 */

	public static final boolean contains( String effectName )
	{	return effectByName.containsKey( getCanonicalName( effectName ) );
	}

	/**
	 * Returns a list of all items which contain the given
	 * substring.  This is useful for people who are doing
	 * lookups on items.
	 */

	public static final List getMatchingNames( String substring )
	{	return getMatchingNames( effectByName, substring );
	}
}
