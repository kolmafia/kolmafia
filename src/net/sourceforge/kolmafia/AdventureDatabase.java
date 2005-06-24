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
	private static List [] adventureTable;

	static
	{
		BufferedReader reader = getReader( "adventures.dat" );
		adventureTable = new ArrayList[3];
		for ( int i = 0; i < 3; ++i )
			adventureTable[i] = new ArrayList();

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 3 )
				for ( int i = 0; i < 3; ++i )
					adventureTable[i].add( data[i] );
		}
	}

	public static final LockableListModel getAsLockableListModel( KoLmafia client )
	{
		LockableListModel adventures = new LockableListModel();
		for ( int i = 0; i < adventureTable[0].size(); ++i )
			adventures.add( new KoLAdventure( client, (String) adventureTable[0].get(i),
				(String) adventureTable[1].get(i), (String) adventureTable[2].get(i) ) );

		if ( client != null && client.getSettings().getProperty( "sortAdventures" ).equals( "true" ) )
			java.util.Collections.sort( adventures );

		return adventures;
	}

	public static KoLAdventure getAdventure( KoLmafia client, String adventureName )
	{
		List adventureNames = adventureTable[2];

		for ( int i = 0; i < adventureNames.size(); ++i )
			if ( ((String) adventureNames.get(i)).toLowerCase().indexOf( adventureName.toLowerCase() ) != -1 )
				return new KoLAdventure( client, (String) adventureTable[0].get(i),
					(String) adventureTable[1].get(i), (String) adventureTable[2].get(i) );

		return null;
	}

	public static final boolean contains( String adventureName )
	{
		List adventureNames = adventureTable[2];

		for ( int i = 0; i < adventureNames.size(); ++i )
			if ( ((String) adventureNames.get(i)).toLowerCase().indexOf( adventureName.toLowerCase() ) != -1 )
				return true;

		return false;
	}
}