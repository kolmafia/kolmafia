/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLDatabase;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.CoinMasterPurchaseRequest;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.HashMultimap;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmastersDatabase
	extends KoLDatabase
{
	public static final HashMap COINMASTER_ITEMS = new HashMap();

	private static final LockableListModel buyForDimes = new LockableListModel();
	private static final Map dimeSellPriceByName = new TreeMap();
	private static final Map dimeBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForQuarters = new LockableListModel();
	private static final Map quarterSellPriceByName = new TreeMap();
	private static final Map quarterBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForLucre = new LockableListModel();
	private static final Map lucreBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForSandDollars = new LockableListModel();
	private static final Map sandDollarBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForCrimbux = new LockableListModel();
	private static final Map crimbuckBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForTickets = new LockableListModel();
	private static final Map ticketBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForBoneChips = new LockableListModel();
	private static final Map boneChipBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForScrip = new LockableListModel();
	private static final Map scripBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForStoreCredit = new LockableListModel();
	private static final Map storeCreditSellPriceByName = new TreeMap();
	private static final Map storeCreditBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForSnackVouchers = new LockableListModel();
	private static final Map snackVoucherBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForCommendations = new LockableListModel();
	private static final Map commendationBuyPriceByName = new TreeMap();

	private static final LockableListModel buyForIsotopes1 = new LockableListModel();
	private static final Map isotope1BuyPriceByName = new TreeMap();

	private static final LockableListModel buyForIsotopes2 = new LockableListModel();
	private static final Map isotope2BuyPriceByName = new TreeMap();

	private static final LockableListModel buyForIsotopes3 = new LockableListModel();
	private static final Map isotope3BuyPriceByName = new TreeMap();

	private static final LockableListModel buyForMrAccessory = new LockableListModel();
	private static final Map MrAccessoryBuyPriceByName = new TreeMap();

	private static final LockableListModel buyFromTraveler = new LockableListModel();
	private static final Map TravelerBuyPriceByName = new TreeMap();

	private static final Map lighthouseItems = new TreeMap();

	static
	{
		BufferedReader reader = FileUtilities.getVersionedReader( "coinmasters.txt", KoLConstants.COINMASTERS_VERSION );

		String[] data;

		while ( ( data = FileUtilities.readData( reader ) ) != null )
		{
			if ( data.length >= 3 )
			{
				String code = data[0];
				int price = StringUtilities.parseInt( data[ 1 ] );
				Integer iprice = new Integer( price );
				String rname = data[2];
				String name = StringUtilities.getCanonicalName( rname );
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
				else if ( code.equals( "bl" ) )
				{
					// Something we buy with lucre
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForLucre.add( item );
					lucreBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bs" ) )
				{
					// Something we buy with sand dollars
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForSandDollars.add( item );
					sandDollarBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bc" ) )
				{
					// Something we buy with Crimbux
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForCrimbux.add( item );
					crimbuckBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bt" ) )
				{
					// Something we buy with Game Grid tickets
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForTickets.add( item );
					ticketBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bb" ) )
				{
					// Something we buy with bone chips
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForBoneChips.add( item );
					boneChipBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bcs" ) )
				{
					// Something we buy with CRIMBCO Scrip
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForScrip.add( item );
					scripBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "ssc" ) )
				{
					// Something we sell for store credit
					storeCreditSellPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bsc" ) )
				{
					// Something we buy with store credit
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForStoreCredit.add( item );
					storeCreditBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bsv" ) )
				{
					// Something we buy with snack vouchers
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForSnackVouchers.add( item );
					snackVoucherBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bac" ) )
				{
					// Something we buy with A. W. O. L. commendations
					int itemId = ( data.length > 3 ) ?
						StringUtilities.parseInt( data[ 3 ] ) :
						ItemDatabase.getItemId( name, 1 );
					AdventureResult item = AdventureResult.tallyItem( rname, itemId );
					buyForCommendations.add( item );
					commendationBuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bli1" ) )
				{
					// Something we buy with lunar isotopes
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForIsotopes1.add( item );
					isotope1BuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bli2" ) )
				{
					// Something we buy with lunar isotopes
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForIsotopes2.add( item );
					isotope2BuyPriceByName.put( name, iprice );
				}
				else if ( code.equals( "bli3" ) )
				{
					// Something we buy with lunar isotopes
					AdventureResult item = new AdventureResult( name, 0, false );
					buyForIsotopes3.add( item );
					isotope3BuyPriceByName.put( name, iprice );
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

	public static final int getPrice( final String name, final Map prices )
	{
		if ( name == null )
		{
			return 0;
		}
		Integer price = (Integer) prices.get( StringUtilities.getCanonicalName( name ) );
		return ( price == null ) ? 0 : price.intValue();
	}

	public static final boolean availableItem( final String name )
	{
		if ( name.equals( "a crimbo carol, ch. 1" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.SEAL_CLUBBER );
		}
		if ( name.equals( "a crimbo carol, ch. 2" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.TURTLE_TAMER );
		}
		if ( name.equals( "a crimbo carol, ch. 3" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.PASTAMANCER );
		}
		if ( name.equals( "a crimbo carol, ch. 4" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.SAUCEROR );
		}
		if ( name.equals( "a crimbo carol, ch. 5" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.DISCO_BANDIT );
		}
		if ( name.equals( "a crimbo carol, ch. 6" ) )
		{
			return KoLCharacter.getClassType().equals( KoLCharacter.ACCORDION_THIEF );
		}
		if ( name.equals( "a. w. o. l. tattoo #1" ) )
		{
			return KoLCharacter.AWOLtattoo == 1;
		}
		if ( name.equals( "a. w. o. l. tattoo #2" ) )
		{
			return KoLCharacter.AWOLtattoo == 2;
		}
		if ( name.equals( "a. w. o. l. tattoo #3" ) )
		{
			return KoLCharacter.AWOLtattoo == 3;
		}
		if ( name.equals( "a. w. o. l. tattoo #4" ) )
		{
			return KoLCharacter.AWOLtattoo == 4;
		}
		if ( name.equals( "a. w. o. l. tattoo #5" ) )
		{
			return KoLCharacter.AWOLtattoo == 5;
		}

		return true;
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

	public static final LockableListModel getLucreItems()
	{
		return buyForLucre;
	}

	public static final Map lucreBuyPrices()
	{
		return lucreBuyPriceByName;
	}

	public static final LockableListModel getSandDollarItems()
	{
		return buyForSandDollars;
	}

	public static final Map sandDollarBuyPrices()
	{
		return sandDollarBuyPriceByName;
	}

	public static final LockableListModel getCrimbuckItems()
	{
		return buyForCrimbux;
	}

	public static final Map crimbuckBuyPrices()
	{
		return crimbuckBuyPriceByName;
	}

	public static final LockableListModel getTicketItems()
	{
		return buyForTickets;
	}

	public static final Map ticketBuyPrices()
	{
		return ticketBuyPriceByName;
	}

	public static final LockableListModel getBoneChipItems()
	{
		return buyForBoneChips;
	}

	public static final Map boneChipBuyPrices()
	{
		return boneChipBuyPriceByName;
	}

	public static final LockableListModel getScripItems()
	{
		return buyForScrip;
	}

	public static final Map scripBuyPrices()
	{
		return scripBuyPriceByName;
	}

	public static final LockableListModel getStoreCreditItems()
	{
		return buyForStoreCredit;
	}

	public static final Map storeCreditSellPrices()
	{
		return storeCreditSellPriceByName;
	}

	public static final Map storeCreditBuyPrices()
	{
		return storeCreditBuyPriceByName;
	}

	public static final LockableListModel getSnackVoucherItems()
	{
		return buyForSnackVouchers;
	}

	public static final Map snackVoucherBuyPrices()
	{
		return snackVoucherBuyPriceByName;
	}

	public static final LockableListModel getCommendationItems()
	{
		return buyForCommendations;
	}

	public static final Map commendationBuyPrices()
	{
		return commendationBuyPriceByName;
	}

	public static final LockableListModel getIsotope1Items()
	{
		return buyForIsotopes1;
	}

	public static final Map isotope1BuyPrices()
	{
		return isotope1BuyPriceByName;
	}

	public static final LockableListModel getIsotope2Items()
	{
		return buyForIsotopes2;
	}

	public static final Map isotope2BuyPrices()
	{
		return isotope2BuyPriceByName;
	}

	public static final LockableListModel getIsotope3Items()
	{
		return buyForIsotopes3;
	}

	public static final Map isotope3BuyPrices()
	{
		return isotope3BuyPriceByName;
	}

	public static final LockableListModel getMrAItems()
	{
		return buyForMrAccessory;
	}

	public static final Map MrABuyPrices()
	{
		return MrAccessoryBuyPriceByName;
	}

	public static final LockableListModel getTravelerItems()
	{
		return buyFromTraveler;
	}

	public static final Map TravelerBuyPrices()
	{
		return TravelerBuyPriceByName;
	}

	public static final Map lighthouseItems()
	{
		return lighthouseItems;
	}

	public static final void clearPurchaseRequests( CoinmasterData data )
	{
		// Clear all purchase requests for a particular Coin Master
		Iterator it = CoinmastersDatabase.COINMASTER_ITEMS.values().iterator();
		while ( it.hasNext() )
		{
			CoinMasterPurchaseRequest request = (CoinMasterPurchaseRequest) it.next();
			if ( request.getData() == data )
			{
				it.remove();
			}
		}
	}

	public static final void registerPurchaseRequest( CoinmasterData data, int itemId, int price )
	{
		// Register a purchase request
		CoinMasterPurchaseRequest request = new CoinMasterPurchaseRequest( data, itemId, price );
		CoinmastersDatabase.COINMASTER_ITEMS.put( new Integer( itemId ), request );
	}

	public static final CoinMasterPurchaseRequest getPurchaseRequest( final String itemName )
	{
		if ( CoinmastersDatabase.COINMASTER_ITEMS.isEmpty() )
		{
			CoinmastersFrame.initializePurchaseRequests();
		}

		Integer id = new Integer( ItemDatabase.getItemId( itemName, 1, false ) );
		CoinMasterPurchaseRequest request =  (CoinMasterPurchaseRequest) CoinmastersDatabase.COINMASTER_ITEMS.get(  id );

		if ( request == null )
		{
			return null;
		}

		request.setCanPurchase( CoinmastersDatabase.canPurchase( request ) );

		return request;
	}

	private static final boolean canPurchase( final CoinMasterPurchaseRequest request )
	{
		// If the Coin Master is "accessible" - which is up to the Coin
		// Master to determine - we can purchase from it.
		if ( CoinMasterRequest.accessible( request.getData() ) != null )
		{
			return false;
		}

		// See if we can afford the item
		CoinmasterData data = request.getData();
		int tokens = data.availableTokens();
		int price = request.getPrice();
		return price <= tokens;
	}

	public static final boolean contains( final String itemName )
	{
		return CoinmastersDatabase.contains( itemName, true );
	}

	public static final int price( final String itemName )
	{
		PurchaseRequest request = CoinmastersDatabase.getPurchaseRequest( itemName );
		return request == null ? 0 : request.getPrice();
	}

	public static final boolean contains( final String itemName, boolean validate )
	{
		PurchaseRequest item = CoinmastersDatabase.getPurchaseRequest( itemName );
		return item != null && ( !validate || item.canPurchase() );
	}
}
