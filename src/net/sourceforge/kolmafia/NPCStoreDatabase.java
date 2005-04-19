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

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.UtilityConstants;
import net.java.dev.spellcast.utilities.LockableListModel;

/**
 * A static class which retrieves all the NPC stores currently
 * available to <code>KoLmafia</code>.
 */

public class NPCStoreDatabase implements UtilityConstants
{
	private static final String NPC_DBASE_FILE = "npcstores.dat";
	private static List [] npcstoreTable;

	static
	{
		BufferedReader npcdata = DataUtilities.getReaderForSharedDataFile( NPC_DBASE_FILE );

		npcstoreTable = new ArrayList[5];
		for ( int i = 0; i < 5; ++i )
			npcstoreTable[i] = new ArrayList();

		try
		{
			String line;
			while ( (line = npcdata.readLine()) != null )
			{
				StringTokenizer strtok = new StringTokenizer( line, "\t" );
				if ( strtok.countTokens() == 4 )
				{
					for ( int i = 0; i < 4; ++i )
						npcstoreTable[i].add( strtok.nextToken() );

					npcstoreTable[4].add( TradeableItemDatabase.getItemName( Integer.parseInt( (String) npcstoreTable[2].get(
						npcstoreTable[2].size() - 1 ) ) ).replaceAll( "ñ", "&ntilde;" ).replaceAll( "©", "&trade;" ) );
				}
			}
		}
		catch ( IOException e )
		{
			// If an IOException is thrown, that means there was
			// a problem reading in the appropriate data file;
			// that means that no npcstores can be done.  However,
			// the npcstores data file should always be present.

			// The exception is strange enough that it won't be
			// handled at the current time.
		}
	}

	public static final MallPurchaseRequest getPurchaseRequest( KoLmafia client, String itemName )
	{
		List itemNames = npcstoreTable[4];
		int itemIndex = itemNames.indexOf( itemName );

		return itemIndex == -1 ? null :
			new MallPurchaseRequest( client, (String) npcstoreTable[1].get(itemIndex), (String) npcstoreTable[0].get(itemIndex),
				Integer.parseInt( (String) npcstoreTable[2].get(itemIndex) ), Integer.parseInt( (String) npcstoreTable[3].get(itemIndex) ) );
	}

	public static final boolean contains( String itemName )
	{	return npcstoreTable[4].contains( itemName );
	}

	public static final int getNPCStorePrice( String itemName )
	{
		List itemNames = npcstoreTable[4];
		int itemIndex = itemNames.indexOf( itemName );

		return itemIndex == -1 ? 0 : Integer.parseInt( (String) npcstoreTable[3].get(itemIndex) );
	}
}