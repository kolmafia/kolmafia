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

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class CoinMasterRequest
	extends GenericRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	public static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );

	private final CoinmasterData data;

	private String action = null;
	private int itemId = -1;
	private int quantity = 0;

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
		this.addFormField( itemField, String.valueOf( itemId ) );

		this.quantity = quantity;
		String countField = this.data.getCountField();
		if ( countField != null )
		{
			this.addFormField( countField, String.valueOf( quantity ) );
		}
	}

	public CoinMasterRequest( final CoinmasterData data, final String action, final int itemId )
	{
		this( data, action, itemId, 1 );
	}

	public CoinMasterRequest( final CoinmasterData data, final String action, final AdventureResult ar )
	{
		this( data, action, ar.getItemId(), ar.getCount() );
	}

	public Object run()
	{
		// If we cannot specify the count, we must get 1 at a time.
		CoinmasterData data = this.data;
		int visits = data.getCountField() == null ? this.quantity : 1;
		String master = data.getMaster();

		int i = 1;

		do
		{
			if ( visits > 1 )
			{
				KoLmafia.updateDisplay( "Visiting the " + master + " (" + i + " of " + visits + ")..." );
			}
			else
			{
				KoLmafia.updateDisplay( "Visiting the " + master + "..." );
			}

			super.run();
		}
		while ( KoLmafia.permitsContinue() && ++i <= visits );

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( master + " successfully looted!" );
		}
		return null;
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
	}

	public static final void refundPurchase( final CoinmasterData data, final String urlString )
	{
		if ( data == null )
		{
			return;
		}

		Matcher itemMatcher = data.getItemMatcher( urlString );
		Matcher countMatcher = data.getCountMatcher( urlString );
		Map prices = data.getBuyPrices();

		int cost = getPurchaseCost( itemMatcher, countMatcher, prices );

		String property = data.getProperty();
		if ( property != null )
		{
			Preferences.increment( property, cost );
		}

		AdventureResult item = data.getItem();
		if ( item != null )
		{
			AdventureResult current = item.getInstance( cost );
			ResultProcessor.processResult( current );
		}

		String token = data.getToken();
		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have enough " + token + " to buy that." );
	}

	protected static int getPurchaseCost( final Matcher itemMatcher, final Matcher countMatcher, final Map prices )
	{
		if ( !itemMatcher.find() )
		{
			return 0;
		}

		int count = 1;
		if ( countMatcher != null )
		{
			if ( !countMatcher.find() )
			{
				return 0;
			}
			count = StringUtilities.parseInt( countMatcher.group(1) );
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group(1) );
		String name = ItemDatabase.getItemName( itemId );
		int price = CoinmastersDatabase.getPrice( name, prices );
		return count * price;
	}

	public static final void refundSale( final CoinmasterData data, final String urlString )
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

		int itemId = StringUtilities.parseInt( itemMatcher.group(1) );
		int count = StringUtilities.parseInt( countMatcher.group(1) );

		// Get back the items we failed to turn in
		AdventureResult item = new AdventureResult( itemId, count );
		ResultProcessor.processResult( item );

		// Remove the tokens we failed to receive
		String name = ItemDatabase.getItemName( itemId );
		Map prices = data.getSellPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;

		String property = data.getProperty();
		if ( property != null )
		{
			Preferences.increment( property, -cost );
		}

		String plural = ItemDatabase.getPluralName( itemId );
		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have that many " + plural );
	}

	public static boolean registerRequest( final String urlString )
	{
		if ( urlString.startsWith( "bhh.php" ) )
		{
			return BountyHunterHunterRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "monkeycastle.php" ) )
		{
			return BigBrotherRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "crimbo09.php" ) )
		{
			return CrimboCartelRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "crimbo10.php" ) )
		{
			return CRIMBCOGiftShopRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "arcade.php" ) )
		{
			return TicketCounterRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "gamestore.php" ) )
		{
			return GameShoppeRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "bone_altar.php" ) )
		{
			return AltarOfBonesRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "bigisland.php" ) )
		{
			return IslandDecorator.registerIslandRequest( urlString );
		}

		if ( urlString.startsWith( "inv_use.php" ) )
		{
			return AWOLQuartermasterRequest.registerRequest( urlString );
		}

		if ( urlString.startsWith( "spaaace.php" ) )
		{
			return SpaaaceRequest.registerRequest( urlString );
		}

		return false;
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
			count = StringUtilities.parseInt( countMatcher.group(1) );
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group(1) );
		LockableListModel items = data.getBuyItems();
		AdventureResult item = CoinMasterRequest.findItem( itemId, items );
		String name = item.getName();
		Map prices = data.getBuyPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;

		AdventureResult tokenItem = data.getItem();
		String token = tokenItem == null ? data.getToken() : tokenItem.getName();
		String tokenName = ( cost != 1 ) ? ItemDatabase.getPluralName( token ) : token;
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + cost + " " + tokenName + " for " + count + " " + itemName );

		if ( tokenItem != null )
		{
			AdventureResult current = tokenItem.getInstance( -cost );
			ResultProcessor.processResult( current );
		}

		String property = data.getProperty();
		if ( property != null )
		{
			Preferences.increment( property, -cost );
		}

		CoinmastersFrame.externalUpdate();
	}

	private static AdventureResult findItem( final int itemId, final LockableListModel items )
	{
		Iterator it = items.iterator();
		while ( it.hasNext() )
		{
			AdventureResult item = (AdventureResult)it.next();
			if ( item.getItemId() == itemId )
			{
				return item;
			}
		}
		return null;
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

		int itemId = StringUtilities.parseInt( itemMatcher.group(1) );
		String name = ItemDatabase.getItemName( itemId );
		int count = StringUtilities.parseInt( countMatcher.group(1) );
		Map prices = data.getSellPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;

		AdventureResult tokenItem = data.getItem();
		String token = tokenItem == null ? data.getToken() : tokenItem.getName();
		String tokenName = ( cost != 1 ) ? ItemDatabase.getPluralName( token ) : token;
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + count + " " + itemName + " for " + cost + " " + tokenName );

		AdventureResult item = new AdventureResult( itemId, -count );
		ResultProcessor.processResult( item );

		String property = data.getProperty();
		if ( property != null )
		{
			Preferences.increment( property, cost );
		}

		CoinmastersFrame.externalUpdate();
	}
}
