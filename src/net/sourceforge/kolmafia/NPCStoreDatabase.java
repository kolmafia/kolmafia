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
 * A static class which retrieves all the NPC stores currently
 * available to <code>KoLmafia</code>.
 */

public class NPCStoreDatabase extends KoLDatabase
{
	private static List [] npcstoreTable;

	static
	{
		BufferedReader reader = getReader( "npcstores.dat" );

		npcstoreTable = new ArrayList[5];
		for ( int i = 0; i < 5; ++i )
			npcstoreTable[i] = new ArrayList();

		String [] data;

		while ( (data = readData( reader )) != null )
		{
			if ( data.length == 4 )
			{
				for ( int i = 0; i < 4; ++i )
					npcstoreTable[i].add( data[i] );

				npcstoreTable[4].add( Integer.valueOf( data[2] ) );
			}
		}

		try
		{
			reader.close();
		}
		catch ( Exception e )
		{
			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	public static final MallPurchaseRequest getPurchaseRequest( String itemName )
	{
		int itemIndex = npcstoreTable[4].indexOf( new Integer( TradeableItemDatabase.getItemID( itemName ) ) );

		// If the person is not in a muscle sign, then items from the
		// Degrassi Knoll are not available.

		if ( npcstoreTable[0].get(itemIndex).equals( "5" ) && !KoLCharacter.inMuscleSign() )
			return null;

		// If the person is not in a mysticality sign, then items from the
		// Canadia Jewelers are not available.

		if ( npcstoreTable[0].get(itemIndex).equals( "j" ) && !KoLCharacter.inMysticalitySign() )
			return null;

		return itemIndex == -1 ? null :
			new MallPurchaseRequest( client, (String) npcstoreTable[1].get(itemIndex), (String) npcstoreTable[0].get(itemIndex),
				Integer.parseInt( (String) npcstoreTable[2].get(itemIndex) ), Integer.parseInt( (String) npcstoreTable[3].get(itemIndex) ) );
	}

	public static final boolean contains( String itemName )
	{	return npcstoreTable[4].contains( new Integer( TradeableItemDatabase.getItemID( itemName ) ) ) && getPurchaseRequest( itemName ) != null;
	}

	public static final int getNPCStorePrice( String itemName )
	{
		int itemIndex = npcstoreTable[4].indexOf( new Integer( TradeableItemDatabase.getItemID( itemName ) ) );
		return itemIndex == -1 ? 0 : Integer.parseInt( (String) npcstoreTable[3].get(itemIndex) );
	}
}
