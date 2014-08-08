/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.Map;
import java.util.Map.Entry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

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
	protected final CoinmasterData data;

	protected String action = null;
	protected AdventureResult[] attachments;

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

	public CoinMasterRequest( final CoinmasterData data, final String action, final AdventureResult [] attachments )
	{
		this( data, action );
		this.attachments = attachments;
	}

	public CoinMasterRequest( final CoinmasterData data, final String action, final AdventureResult attachment )
	{
		this( data, action, new AdventureResult[] { attachment } );
	}

	public CoinMasterRequest( final CoinmasterData data, final String action, final int itemId, final int quantity )
	{		
		this( data, action, new AdventureResult( itemId, quantity ) );
	}

	public CoinMasterRequest( final CoinmasterData data, final String action, final int itemId )
	{
		this( data, action, new AdventureResult( itemId, 1 ) );
	}

	public final void setQuantity( final int quantity )
	{
		// Kludge for the use of CoinmasterPurchaseRequest
		AdventureResult ar = attachments[ 0 ];
		attachments[ 0 ] = ar.getInstance( quantity );
	}

	public static void visit( final CoinmasterData data )
	{
		if ( data == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Visit whom?" );
			return;
		}

		CoinMasterRequest request = data.getRequest();
		request.transact( data );
	}

	public static void buy( final CoinmasterData data, final AdventureResult it )
	{
		if ( data == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Buy from whom?" );
			return;
		}

		String action = data.getBuyAction();
		String itemName = it.getName();
		if ( action == null || !data.canBuyItem( itemName ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't buy " + itemName + " from " + data.getMaster() );
			return;
		}

		String reason = data.canBuy();
		if ( reason != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, reason );
			return;
		}

		CoinMasterRequest request = data.getRequest( action, new AdventureResult[] { it } );
		request.transact( data );
	}

	public static void sell( final CoinmasterData data, final AdventureResult it )
	{
		if ( data == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Sell to whom?" );
			return;
		}

		String action = data.getSellAction();
		String itemName = it.getName();
		if ( action == null || !data.canSellItem( itemName ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't sell " + itemName + " to " + data.getMaster() );
			return;
		}

		String reason = data.canSell();
		if ( reason != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, reason );
			return;
		}

		CoinMasterRequest request = data.getRequest( action, new AdventureResult[] { it } );
		request.transact( data );
	}

	private void transact( final CoinmasterData data )
	{
		String reason = data.accessible();
		if ( reason != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, reason );
			return;
		}

		RequestThread.postRequest( this );
	}

	public void setItem( final AdventureResult item )
	{
		String itemField = this.data.getItemField();
		if ( itemField != null )
		{
			int itemId = item.getItemId();
			this.addFormField( itemField, String.valueOf( this.data.getRow( itemId ) ) );
		}
	}

	public int setCount( final AdventureResult item, final boolean singleton )
	{
		int count = item.getCount();
		if ( singleton )
		{
			count = TransferItemRequest.keepSingleton( item, count );
		}
		String countField = this.data.getCountField();
		if ( countField != null )
		{
			this.addFormField( countField, String.valueOf( count ) );
		}
		return count;
	}

	@Override
	public void run()
	{
		CoinmasterData data = this.data;

		// See if the Coin Master is accessible
		boolean justVisiting = attachments == null;
		if ( !justVisiting )
		{
			String reason = data.accessible();
			if ( reason != null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, reason );
				return;
			}
		}

		try
		{
			// Suit up for a visit
			SpecialOutfit.createImplicitCheckpoint();
			this.equip();

			String master = data.getMaster();

			if ( justVisiting )
			{
				KoLmafia.updateDisplay( "Visiting the " + master + "..." );
				super.run();
			}
			else
			{
				boolean keepSingleton = this.action != null && this.action.equals( data.getSellAction() ) && !KoLCharacter.canInteract();

				for ( int i = 0; i < this.attachments.length && KoLmafia.permitsContinue(); ++i )
				{
					AdventureResult ar = this.attachments[ i ];
					boolean singleton = keepSingleton && KoLConstants.singletonList.contains( ar );

					this.setItem( ar );
					int count = this.setCount( ar, singleton );

					// If we cannot specify the count, we must get 1 at a time.

					int visits = data.getCountField() == null ? count : 1;
					int visit = 0;

					while ( KoLmafia.permitsContinue() && ++visit <= visits )
					{
						if ( visits > 1 )
						{
							KoLmafia.updateDisplay( "Visiting the " + master + " (" + visit + " of " + visits + ")..." );
						}
						else if ( visits == 1 )
						{
							KoLmafia.updateDisplay( "Visiting the " + master + "..." );
						}

						super.run();

						if ( this.responseText.indexOf( "You don't have enough" ) != -1 )
						{
							KoLmafia.updateDisplay( MafiaState.ERROR, "You can't afford that item." );
							break;
						}

						if ( this.responseText.indexOf( "You don't have that many of that item" ) != -1 )
						{
							KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have that many of that item to turn in." );
							break;
						}
					}
				}
			}

			if ( KoLmafia.permitsContinue() && this.action != null )
			{
				KoLmafia.updateDisplay( master + " successfully looted!" );
			}
		}
		finally
		{
			this.unequip();
			SpecialOutfit.restoreImplicitCheckpoint();
		}
	}

	public void equip()
	{
	}

	public void unequip()
	{
	}

	@Override
	public void reconstructFields()
	{
		this.constructURLString( this.getURLString() );
	}

	@Override
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

	public static final int extractItemId( final CoinmasterData data, final String urlString )
	{
		Matcher itemMatcher = data.getItemMatcher( urlString );
		if ( !itemMatcher.find() )
		{
			return -1;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
		if ( data.getRows() != null )
		{
			// itemId above is actually the row
			for ( Entry<String, Integer> entry : data.getRows().entrySet() )
			{
				if ( itemId == entry.getValue() )
				{
					// This is the actual itemId
					return ItemDatabase.getItemId( entry.getKey(), 1 );
				}
			}
			return -1;
		}

		return itemId;
	}

	public static final int extractCount( final CoinmasterData data, final String urlString )
	{
		Matcher countMatcher = data.getCountMatcher( urlString );
		if ( countMatcher != null )
		{
			if ( !countMatcher.find() )
			{
				return 0;
			}
			return StringUtilities.parseInt( countMatcher.group( 1 ) );
		}

		return 1;
	}

	public static final int itemBuyPrice( final CoinmasterData data, final int itemId )
	{
		LockableListModel<AdventureResult> items = data.getBuyItems();
		AdventureResult item = AdventureResult.findItem( itemId, items );
		String name = item.getName();
		Map prices = data.getBuyPrices();
		return CoinmastersDatabase.getPrice( name, prices );
	}

	public static final int itemSellPrice( final CoinmasterData data, final int itemId )
	{
		String name = ItemDatabase.getItemName( itemId );
		Map prices = data.getSellPrices();
		return CoinmastersDatabase.getPrice( name, prices );
	}

	public static final void buyStuff( final CoinmasterData data, final String urlString )
	{
		if ( data == null )
		{
			return;
		}

		int itemId = CoinMasterRequest.extractItemId( data, urlString );
		if ( itemId == -1 )
		{
			return;
		}

		int count = CoinMasterRequest.extractCount( data, urlString );
		if ( count == 0 )
		{
			return;
		}

		String storageAction = data.getStorageAction();
		boolean storage = storageAction != null && urlString.indexOf( storageAction ) != -1;

		CoinMasterRequest.buyStuff( data, itemId, count, storage );
	}

	public static final void buyStuff( final CoinmasterData data, final int itemId, final int count, final boolean storage )
	{
		int price = CoinMasterRequest.itemBuyPrice( data, itemId );
		int cost = count * price;

		String tokenName = ( cost != 1 ) ? data.getPluralToken() : data.getToken();
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : ItemDatabase.getItemName( itemId );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + cost + " " + tokenName + " for " + count + " " + itemName + ( storage ? " from storage" : "" ) );
	}

	public static final void completePurchase( final CoinmasterData data, final String urlString )
	{
		if ( data == null )
		{
			return;
		}

		int itemId = CoinMasterRequest.extractItemId( data, urlString );
		if ( itemId == -1 )
		{
			return;
		}

		String storageAction = data.getStorageAction();
		boolean storage = storageAction != null && urlString.indexOf( storageAction ) != -1;

		int count = CoinMasterRequest.extractCount( data, urlString );
		if ( count == 0 )
		{
			String tradeAll = data.getTradeAllAction();

			if ( tradeAll == null || !urlString.contains( tradeAll ) )
			{
				return;
			}

			AdventureResult tokenItem = data.getItem();
			String property = data.getProperty();

			int available =
				storage ? tokenItem.getCount( KoLConstants.storage ) :
				property != null ? Preferences.getInteger( property ) :
				tokenItem.getCount( KoLConstants.inventory );

			int price = CoinMasterRequest.itemBuyPrice( data, itemId );
			count = available / price;
		}

		CoinMasterRequest.completePurchase( data, itemId, count, storage );
	}

	public static final void completePurchase( final CoinmasterData data, final int itemId, final int count, final boolean storage )
	{
		int price = CoinMasterRequest.itemBuyPrice( data, itemId );
		int cost = count * price;

		String property = data.getProperty();
		if ( property != null && !storage )
		{
			Preferences.increment( property, -cost );
		}

		AdventureResult tokenItem = data.getItem();
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
		int price = CoinMasterRequest.itemSellPrice( data, itemId );
		int cost = count * price;

		String tokenName = ( cost != 1 ) ? data.getPluralToken() : data.getToken();
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : ItemDatabase.getItemName( itemId );

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
		int price = CoinMasterRequest.itemSellPrice( data, itemId );
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
		String action = GenericRequest.getAction( urlString );

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
