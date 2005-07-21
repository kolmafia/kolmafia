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

import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A static class which retrieves all the adventures currently
 * available to <code>KoLmafia</code>.
 */

public class AdventureDatabase extends KoLDatabase
{
	public static final String [][] ZONES =
	{
		{ "Shore", "vacations at the shore" }, { "Camp", "campground resting" }, { "Gym", "clan gym equipment" },
		{ "Town", "Seaside Town areas" }, { "Casino", "Seaside Town's casino games" }, { "Plains", "general plains areas" },
		{ "Knob", "Cobb's knob areas" }, { "Bat", "Bat Hole areas" }, { "Cyrpt", "the defiled cyrpt quest" },
		{ "Woods", "general woods areas" }, { "Friars", "deep fat friar's quest" }, { "Mount", "general mountain areas" },
		{ "Mclarge", "Mt. McLargeHuge areas" }, { "Island", "the mysterious island areas" }, { "Stalk", "the areas beyond the beanstalk" },
		{ "Beach", "the desert beach areas" }, { "Tower", "the Sorceress Tower maze" }, { "Signed", "sign-restricted areas" },
		{ "Special", "special areas" }
	};


	private static List [] adventureTable;

	static
	{
		BufferedReader reader = getReader( "adventures.dat" );
		adventureTable = new ArrayList[4];
		for ( int i = 0; i < 4; ++i )
			adventureTable[i] = new ArrayList();

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 4 )
			{
				adventureTable[0].add( ZONES[ Integer.parseInt( data[0] ) ][0] );
				for ( int i = 1; i < 4; ++i )
					adventureTable[i].add( data[i] );
			}
		}
	}

	/**
	 * Returns the complete list of adventures available to the character
	 * based on the information provided by the given client.  Each element
	 * in this list is a <code>KoLAdventure</code> object.
	 */

	public static final LockableListModel getAsLockableListModel( KoLmafia client )
	{
		KoLSettings settings = client == null ? new KoLSettings() : client.getSettings();

		String [] zones = settings.getProperty( "zoneExcludeList" ).split( "," );
		if ( zones.length == 1 && zones[0].length() == 0 )
			zones[0] = "-";

		boolean shouldAdd = true;
		String zoneName;
		LockableListModel adventures = new LockableListModel();

		for ( int i = 0; i < adventureTable[1].size(); ++i )
		{
			shouldAdd = true;
			zoneName = (String) adventureTable[0].get(i);

			for ( int j = 0; j < zones.length && shouldAdd; ++j )
				if ( zoneName.equals( zones[j] ) )
					shouldAdd = false;

			if ( shouldAdd )
				adventures.add( new KoLAdventure( client, zoneName,
					(String) adventureTable[1].get(i), (String) adventureTable[2].get(i), (String) adventureTable[3].get(i) ) );
		}

		if ( client != null && settings.getProperty( "sortAdventures" ).equals( "true" ) )
			java.util.Collections.sort( adventures );

		return adventures;
	}

	/**
	 * Returns the first adventure in the database which contains the given
	 * substring in part of its name.
	 */

	public static KoLAdventure getAdventure( KoLmafia client, String adventureName )
	{
		List adventureNames = adventureTable[3];

		for ( int i = 0; i < adventureNames.size(); ++i )
			if ( ((String) adventureNames.get(i)).toLowerCase().indexOf( adventureName.toLowerCase() ) != -1 )
				return new KoLAdventure( client, (String) adventureTable[0].get(i), (String) adventureTable[1].get(i),
					(String) adventureTable[2].get(i), (String) adventureTable[3].get(i) );

		return null;
	}
}
