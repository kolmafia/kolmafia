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

package net.sourceforge.kolmafia.request;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinMasterRequest
	extends GenericRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	public static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	public static final Pattern QTY_PATTERN = Pattern.compile( "qty=(\\d+)" );

	private final CoinmasterData data;

	protected String action = null;
	protected int itemId = -1;
	protected int quantity = 0;

	public CoinMasterRequest( final CoinmasterData data )
	{
		super( data.getURL() );
		this.data = data;
	}

	public CoinMasterRequest( final CoinmasterData data, final String action )
	{
		this( data );
		if ( action != null )
		{
			this.action = action;
			this.addFormField( "action", action );
		}
	}

	public CoinMasterRequest( final CoinmasterData data, final String action, final int itemId, final int quantity )
	{
		this( data, action );

		this.itemId = itemId;
		String itemField = this.data.getItemField();
		if ( itemField != null )
		{
			this.addFormField( itemField, String.valueOf( itemId ) );
		}
		this.setQuantity( quantity );
	}

	public CoinMasterRequest( final CoinmasterData data, final String action, final int itemId )
	{
		this( data, action, itemId, 1 );
	}

	public CoinMasterRequest( final CoinmasterData data, final String action, final AdventureResult ar )
	{
		this( data, action, ar.getItemId(), ar.getCount() );
	}

	public void setQuantity( final int quantity )
	{
		this.quantity = quantity;
		String countField = this.data.getCountField();
		if ( countField != null )
		{
			this.addFormField( countField, String.valueOf( quantity ) );
		}
	}

	public static CoinMasterRequest getRequest( final CoinmasterData data )
	{
		Class requestClass = data.getRequestClass();
		Class [] parameters = new Class[ 0 ] ;

		try
		{
			Constructor constructor = requestClass.getConstructor( parameters );
			Object [] initargs = new Object[ 0 ];
			return (CoinMasterRequest) constructor.newInstance( initargs );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public static CoinMasterRequest getRequest( final CoinmasterData data, final String action, final AdventureResult it )
	{
		Class requestClass = data.getRequestClass();
		Class [] parameters = new Class[ 2 ] ;
		parameters[ 0 ] = String.class;
		parameters[ 1 ] = AdventureResult.class;

		try
		{
			Constructor constructor = requestClass.getConstructor( parameters );
			Object [] initargs = new Object[ 2 ];
			initargs[ 0 ] = action;
			initargs[ 1 ] = it;
			return (CoinMasterRequest) constructor.newInstance( initargs );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public static String accessible( final CoinmasterData data )
	{
		// Returns an error reason or null

		Class requestClass = data.getRequestClass();
		Class [] parameters = new Class[ 0 ] ;

		try
		{
			Method method = requestClass.getMethod( "accessible", parameters );
			Object [] args = new Object[ 0 ];
			return (String) method.invoke( null, args );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public static String canSell( final CoinmasterData data )
	{
		// Returns an error reason or null

		Class requestClass = data.getRequestClass();
		Class [] parameters = new Class[ 0 ] ;

		try
		{
			Method method = requestClass.getMethod( "canSell", parameters );
			Object [] args = new Object[ 0 ];
			return (String) method.invoke( null, args );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public static String canBuy( final CoinmasterData data )
	{
		// Returns an error reason or null

		Class requestClass = data.getRequestClass();
		Class [] parameters = new Class[ 0 ] ;

		try
		{
			Method method = requestClass.getMethod( "canBuy", parameters );
			Object [] args = new Object[ 0 ];
			return (String) method.invoke( null, args );
		}
		catch ( Exception e )
		{
			return null;
		}
	}

	public static void visit( final CoinmasterData data )
	{
		if ( data == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Visit whom?" );
			return;
		}

		CoinMasterRequest request = CoinMasterRequest.getRequest( data );
		CoinMasterRequest.transact( data, request );
	}

	public static void buy( final CoinmasterData data, final AdventureResult it )
	{
		if ( data == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Buy from whom?" );
			return;
		}

		String itemName = it.getName();
		if ( !data.canBuyItem( itemName ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't buy " + itemName + " from " + data.getMaster() );
			return;
		}

		String reason = CoinMasterRequest.canBuy( data );
		if ( reason != null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, reason );
			return;
		}

		String action = data.getBuyAction();
		CoinMasterRequest request = CoinMasterRequest.getRequest( data, action, it );
		CoinMasterRequest.transact( data, request );
	}

	public static void sell( final CoinmasterData data, final AdventureResult it )
	{
		if ( data == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Sell to whom?" );
			return;
		}

		String action = data.getSellAction();
		String itemName = it.getName();
		if ( action == null || !data.canSellItem( itemName ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't sell " + itemName + " to " + data.getMaster() );
			return;
		}

		String reason = CoinMasterRequest.canSell( data );
		if ( reason != null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, reason );
			return;
		}

		CoinMasterRequest request = CoinMasterRequest.getRequest( data, action, it );
		CoinMasterRequest.transact( data, request );
	}

	private static void transact( final CoinmasterData data, CoinMasterRequest request )
	{
		String reason = CoinMasterRequest.accessible( data );
		if ( reason != null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, reason );
			return;
		}

		RequestThread.postRequest( request );
	}

	public void run()
	{
		CoinmasterData data = this.data;

		// See if the Coin Master is accessible
		String message = CoinMasterRequest.accessible( data);
		if ( message != null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, message );
			return;
		}

		// Suit up for a visit
		this.equip();

		// If we cannot specify the count, we must get 1 at a time.
		int visits = data.getCountField() == null ? this.quantity : 1;
		String master = data.getMaster();

		int i = 1;

		do
		{
			if ( visits > 1 )
			{
				KoLmafia.updateDisplay( "Visiting the " + master + " (" + i + " of " + visits + ")..." );
			}
			else if ( visits == 1 )
			{
				KoLmafia.updateDisplay( "Visiting the " + master + "..." );
			}

			super.run();

			if ( this.responseText.indexOf( "You don't have enough" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't afford that item.." );
				return;
			}

			if ( this.responseText.indexOf( "You don't have that many of that item" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have that many of that item to turn in." );
				return;
			}
		}
		while ( KoLmafia.permitsContinue() && ++i <= visits );

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( master + " successfully looted!" );
		}

		this.unequip();
	}

	public void equip()
	{
	}

	public void unequip()
	{
	}

	public void processResults()
	{
		CoinMasterRequest.parseResponse( this.data, this.getURLString(), this.responseText );
	}

	/*
	 * A generic response parser for CoinMasterRequests.
	 */

	public static void parseResponse( final CoinmasterData data, final String urlString, final String responseText )
	{
		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			CoinMasterRequest.parseBalance( data, responseText );
			return;
		}

		String buy = data.getBuyAction();
		String sell = data.getSellAction();
		if ( buy != null && action.equals( buy ) &&
		     responseText.indexOf( "You don't have enough" ) == -1 )
		{
			CoinMasterRequest.completePurchase( data, urlString );
		}
		else if ( sell != null && action.equals( sell ) &&
			  responseText.indexOf( "You don't have that many" ) == -1 )
		{
			CoinMasterRequest.completeSale( data, urlString );
		}

		CoinMasterRequest.parseBalance( data, responseText );

		// Coinmaster transactions are now concoctions. If the token is
		// a real item, the Concoction database got refreshed, but not
		// if the token is a pseudo-item
		if ( data.getItem() == null )
		{
			ConcoctionDatabase.setRefreshNeeded( true );
		}
	}

	public static void parseBalance( final CoinmasterData data, final String responseText )
	{
		if ( data == null )
		{
			return;
		}

		// See if this Coin Master will tell us how many tokens we have
		Pattern tokenPattern = data.getTokenPattern();
		if ( tokenPattern == null )
		{
			// If not, we have to depend on inventory tracking
			return;
		}

		// See if there is a special string for having no tokens
		String tokenTest = data.getTokenTest();
		boolean check = true;
		if ( tokenTest != null )
		{
			boolean positive = data.getPositiveTest();
			boolean found = responseText.indexOf( tokenTest ) != -1;
			// If there is a positive check for tokens and we found it
			// or a negative check for tokens and we didn't find it,
			// we can parse the token count on this page
			check = ( positive == found );
		}

		String balance = "0";
		if ( check )
		{
			Matcher matcher = tokenPattern.matcher( responseText );
			if ( !matcher.find() )
			{
				return;
			}
			balance = matcher.group(1);
		}

		// Mr. Store, at least, like to spell out some numbers
		if ( balance.equals( "no" ) )
		{
			balance = "0";
		}
		else if ( balance.equals( "one" ) )
		{
			balance = "1";
		}
		// The Tr4pz0r doesn't give a number if you have 1
		else if ( balance.equals( "" ) )
		{
			balance = "1";
		}

		String property = data.getProperty();
		if ( property != null )
		{
			Preferences.setString( property, balance );
		}

		AdventureResult item = data.getItem();
		if ( item != null )
		{
			// Check and adjust inventory count, just in case
			int count = StringUtilities.parseInt( balance );
			AdventureResult current = item.getInstance( count );
			int icount = item.getCount( KoLConstants.inventory );
			if ( count != icount )
			{
				item = item.getInstance( count - icount );
				AdventureResult.addResultToList( KoLConstants.inventory, item );
			}
		}

		CoinmastersFrame.externalUpdate();
	}

	public static final void buyStuff( final CoinmasterData data, final String urlString )
	{
		if ( data == null )
		{
			return;
		}

		Matcher itemMatcher = data.getItemMatcher( urlString );
		if ( !itemMatcher.find() )
		{
			return;
		}

		Matcher countMatcher = data.getCountMatcher( urlString );
		int count = 1;
		if ( countMatcher != null )
		{
			if ( !countMatcher.find() )
			{
				return;
			}
			count = StringUtilities.parseInt( countMatcher.group( 1 ) );
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		String storageAction = data.getStorageAction();
		boolean storage = storageAction != null && urlString.indexOf( storageAction ) != -1;

		CoinMasterRequest.buyStuff( data, itemId, count, storage );
	}

	public static final void buyStuff( final CoinmasterData data, final int itemId, final int count, final boolean storage )
	{
		LockableListModel items = data.getBuyItems();
		AdventureResult item = AdventureResult.findItem( itemId, items );
		String name = item.getName();
		Map prices = data.getBuyPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;

		AdventureResult tokenItem = data.getItem();
		String token = tokenItem == null ? data.getToken() : tokenItem.getName();
		String tokenName = ( cost != 1 ) ? ItemDatabase.getPluralName( token ) : token;
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + cost + " " + tokenName + " for " + count + " " + itemName + ( storage ? " from storage" : "" ) );
	}

	public static final void completePurchase( final CoinmasterData data, final String urlString )
	{
		if ( data == null )
		{
			return;
		}

		Matcher itemMatcher = data.getItemMatcher( urlString );
		if ( !itemMatcher.find() )
		{
			return;
		}

		AdventureResult tokenItem = data.getItem();
		String tradeAll = data.getTradeAllAction();
		String property = data.getProperty();
		String storageAction = data.getStorageAction();
		boolean storage = storageAction != null && urlString.indexOf( storageAction ) != -1;
		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		LockableListModel items = data.getBuyItems();
		AdventureResult item = AdventureResult.findItem( itemId, items );
		String name = item.getName();
		Map prices = data.getBuyPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );

		int count = 1;

		Matcher countMatcher = data.getCountMatcher( urlString );
		if ( countMatcher != null )
		{
			if ( countMatcher.find() )
			{
				count = StringUtilities.parseInt( countMatcher.group(1) );
			}
			else if ( tradeAll != null && urlString.indexOf( tradeAll ) != -1 )
			{
				int available =
					storage ? tokenItem.getCount( KoLConstants.storage ) :
					property != null ? Preferences.getInteger( property ) :
					tokenItem.getCount( KoLConstants.inventory );
				count = available / price;
			}
			else
			{
				return;
			}
		}

		CoinMasterRequest.completePurchase( data, itemId, count, storage );
	}

	public static final void completePurchase( final CoinmasterData data, final int itemId, final int count, final boolean storage )
	{
		AdventureResult tokenItem = data.getItem();
		String property = data.getProperty();
		LockableListModel items = data.getBuyItems();
		AdventureResult item = AdventureResult.findItem( itemId, items );
		String name = item.getName();
		Map prices = data.getBuyPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );

		int cost = count * price;

		if ( property != null && !storage )
		{
			Preferences.increment( property, -cost );
		}

		if ( tokenItem != null )
		{
			AdventureResult current = tokenItem.getInstance( -cost );
			if ( storage )
			{
				AdventureResult.addResultToList( KoLConstants.storage, current );
			}
			else
			{
				ResultProcessor.processResult( current );
			}
		}
	}

	public static final void sellStuff( final CoinmasterData data, final String urlString )
	{
		if ( data == null )
		{
			return;
		}

		Matcher itemMatcher = data.getItemMatcher( urlString );
		if ( itemMatcher == null )
		{
			return;
		}

		Matcher countMatcher = data.getCountMatcher( urlString );

		if ( countMatcher == null )
		{
			return;
		}

		if ( !itemMatcher.find() || !countMatcher.find() )
		{
			return;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		int count = StringUtilities.parseInt( countMatcher.group( 1 ) );

		CoinMasterRequest.sellStuff( data, itemId, count );
	}

	public static final void sellStuff( final CoinmasterData data, final int itemId, final int count )
	{
		String name = ItemDatabase.getItemName( itemId );
		Map prices = data.getSellPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;

		AdventureResult tokenItem = data.getItem();
		String token = tokenItem == null ? data.getToken() : tokenItem.getName();
		String tokenName = ( cost != 1 ) ? ItemDatabase.getPluralName( token ) : token;
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + count + " " + itemName + " for " + cost + " " + tokenName );
	}

	public static final void completeSale( final CoinmasterData data, final String urlString )
	{
		if ( data == null )
		{
			return;
		}

		Matcher itemMatcher = data.getItemMatcher( urlString );
		if ( itemMatcher == null )
		{
			return;
		}

		Matcher countMatcher = data.getCountMatcher( urlString );
		if ( countMatcher == null )
		{
			return;
		}

		if ( !itemMatcher.find() || !countMatcher.find() )
		{
			return;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		int count = StringUtilities.parseInt( countMatcher.group( 1 ) );

		CoinMasterRequest.completeSale( data, itemId, count );
	}

	public static final void completeSale( final CoinmasterData data, final int itemId, final int count )
	{
		String name = ItemDatabase.getItemName( itemId );
		Map prices = data.getSellPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;

		AdventureResult item = new AdventureResult( itemId, -count );
		ResultProcessor.processResult( item );

		String property = data.getProperty();
		if ( property != null )
		{
			Preferences.increment( property, cost );
		}

		AdventureResult tokenItem = data.getItem();
		if ( tokenItem != null )
		{
			AdventureResult current = tokenItem.getInstance( cost );
			ResultProcessor.processResult( current );
		}
		else
		{
			// Real items get a "You acquire" message logged.
			// Do so here for pseudo-items.
			String message = "You acquire " + cost + " " + data.getToken() + ( cost == 1 ? "" : "s" );
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message	 );
		}
	}

	public static final boolean registerRequest( final CoinmasterData data, final String urlString )
	{
		return CoinMasterRequest.registerRequest( data, urlString, false );
	}

	public static final boolean registerRequest( final CoinmasterData data, final String urlString, final boolean logVisits )
	{
		String action = StringUtilities.getURLDecode( GenericRequest.getAction( urlString ) );

		if ( action == null )
		{
			if ( logVisits )
			{
				RequestLogger.updateSessionLog();
				RequestLogger.updateSessionLog( "Visiting " + data.getMaster() );
			}
			return true;
		}

		String buyAction = data.getBuyAction();
		if ( buyAction != null && action.equals( buyAction ) )
		{
			CoinMasterRequest.buyStuff( data, urlString );
			return true;
		}

		String sellAction = data.getSellAction();
		if ( sellAction != null && action.equals( sellAction ) )
		{
			CoinMasterRequest.sellStuff( data, urlString );
			return true;
		}

		return false;
	}
}
