/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

public class CoinmastersDatabase
	extends KoLDatabase
{
	private static final LockableListModel buyForDimes = new LockableListModel();
	private static final Map dimeSellPriceByName = new TreeMap();
	private static final Map dimeBuyPriceByName = new TreeMap();
	private static final LockableListModel buyForQuarters = new LockableListModel();
	private static final Map quarterSellPriceByName = new TreeMap();
	private static final Map quarterBuyPriceByName = new TreeMap();
	private static final Map lighthouseItems = new TreeMap();

	static
	{
		BufferedReader reader = KoLDatabase.getVersionedReader( "coinmasters.txt", KoLConstants.COINMASTERS_VERSION );

		String[] data;

		while ( ( data = KoLDatabase.readData( reader ) ) != null )
		{
			if ( data.length == 3 )
			{
				String code = data[0];
				int price = StaticEntity.parseInt( data[ 1 ] );
				Integer iprice = new Integer( price );
				String name = KoLDatabase.getCanonicalName( data[2] );
				if ( code.equals( "sd" ) )
				{
					// Something we sell for dimes
					dimeSellPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bd" ) )
				{
					// Something we buy with dimes
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForDimes.add( item );
					dimeBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bdl" ) )
				{
					// Something we buy with dimes if the
					// lighthouse quest is complete
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForDimes.add( item );
					dimeBuyPriceByName.put( name, iprice );
					lighthouseItems.put( name, "" );
				}
				else if ( code.equals( "sq" ) )
				{
					// Something we sell for quarters
					quarterSellPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bq" ) )
				{
					// Something we buy with quarters
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForQuarters.add( item );
					quarterBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bql" ) )
				{
					// Something we buy with quarters if
					// the lighthouse quest is complete
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForQuarters.add( item );
					quarterBuyPriceByName.put( name, iprice );
					lighthouseItems.put( name, "" );
				}
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

	public static final LockableListModel getDimeItems()
	{
		return buyForDimes;
	}

	public static final Map dimeSellPrices()
	{
		return dimeSellPriceByName;
	}

	public static final Map dimeBuyPrices()
	{
		return dimeBuyPriceByName;
	}

	public static final LockableListModel getQuarterItems()
	{
		return buyForQuarters;
	}

	public static final Map quarterSellPrices()
	{
		return quarterSellPriceByName;
	}

	public static final Map quarterBuyPrices()
	{
		return quarterBuyPriceByName;
	}

	public static final Map lighthouseItems()
	{
		return lighthouseItems;
	}
}
