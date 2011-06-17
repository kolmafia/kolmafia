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
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinMasterRequest
	extends GenericRequest
{
	public static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern TOBUY_PATTERN = Pattern.compile( "tobuy=(\\d+)" );
	public static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany=(\\d+)" );
	public static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	private static final Pattern BOUNTY_PATTERN = Pattern.compile( "I'm still waiting for you to bring me (\\d+) (.*?), Bounty Hunter!" );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? (dime|quarter|sand dollar|Game Grid redemption ticket|A. W. O. L. commendation)" );

	private static String lastURL = null;

	public static final CoinmasterData BHH =
		new CoinmasterData(
			"Bounty Hunter Hunter",
			"bhh.php",
			"lucre",
			null,
			null,
			false,
			null,
			CoinmastersFrame.LUCRE,
			"availableLucre",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"howmany",
			CoinMasterRequest.HOWMANY_PATTERN,
			"buy",
			CoinmastersDatabase.getLucreItems(),
			CoinmastersDatabase.lucreBuyPrices(),
			null,
			null
			);
	public static final CoinmasterData HIPPY =
		new CoinmasterData(
			"Dimemaster",
			"bigisland.php",
			"dime",
			"dime",
			"You don't have any dimes",
			false,
			CoinMasterRequest.TOKEN_PATTERN,
			null,
			"availableDimes",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"getgear",
			CoinmastersDatabase.getDimeItems(),
			CoinmastersDatabase.dimeBuyPrices(),
			"turnin",
			CoinmastersDatabase.dimeSellPrices()
			);
	public static final CoinmasterData FRATBOY =
		new CoinmasterData(
			"Quartersmaster",
			"bigisland.php",
			"quarter",
			"quarter",
			"You don't have any quarters",
			false,
			CoinMasterRequest.TOKEN_PATTERN,
			null,
			"availableQuarters",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"getgear",
			CoinmastersDatabase.getQuarterItems(),
			CoinmastersDatabase.quarterBuyPrices(),
			"turnin",
			CoinmastersDatabase.quarterSellPrices()
			);
	public static final CoinmasterData BIGBROTHER =
		new CoinmasterData(
			"Big Brother",
			"monkeycastle.php",
			"sand dollar",
			"sand dollar",
			"You haven't got any sand dollars",
			false,
			CoinMasterRequest.TOKEN_PATTERN,
			CoinmastersFrame.SAND_DOLLAR,
			"availableSandDollars",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"buyitem",
			CoinmastersDatabase.getSandDollarItems(),
			CoinmastersDatabase.sandDollarBuyPrices(),
			null,
			null
			);
	public static final CoinmasterData TICKETCOUNTER =
		new CoinmasterData(
			"Arcade Ticket Counter",
			"arcade.php",
			"ticket",
			"Game Grid redemption ticket",
			"You currently have no Game Grid redemption tickets",
			false,
			CoinMasterRequest.TOKEN_PATTERN,
			CoinmastersFrame.TICKET,
			"availableTickets",
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"quantity",
			CoinMasterRequest.QUANTITY_PATTERN,
			"redeem",
			CoinmastersDatabase.getTicketItems(),
			CoinmastersDatabase.ticketBuyPrices(),
			null,
			null
			);
	public static final CoinmasterData AWOL =
		new CoinmasterData(
			"A. W. O. L. Quartermaster",
			"inv_use.php",
			"commendation",
			"A. W. O. L. commendation",
			null,
			false,
			CoinMasterRequest.TOKEN_PATTERN,
			CoinmastersFrame.AWOL,
			"availableCommendations",
			"tobuy",
			CoinMasterRequest.TOBUY_PATTERN,
			"howmany",
			CoinMasterRequest.HOWMANY_PATTERN,
			null,
			CoinmastersDatabase.getCommendationItems(),
			CoinmastersDatabase.commendationBuyPrices(),
			null,
			null
			);

	private final CoinmasterData data;

	private String action = null;
	private int itemId = -1;
	private int quantity = 0;

	public CoinMasterRequest( final CoinmasterData data )
	{
		super( data.getURL() );

		this.data = data;

		if ( data == CoinMasterRequest.HIPPY )
		{
			this.addFormField( "place", "camp" );
			this.addFormField( "whichcamp", "1" );
		}
		else if ( data == CoinMasterRequest.FRATBOY )
		{
			this.addFormField( "place", "camp" );
			this.addFormField( "whichcamp", "2" );
		}
		else if ( data == CoinMasterRequest.AWOL )
		{
			this.addFormField( "whichitem", "5116" );
			this.addFormField( "which", "3" );
			this.addFormField( "ajax", "1" );
		}
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

		if ( data == CoinMasterRequest.BIGBROTHER )
		{
			this.addFormField( "who", "2" );
		}
		else if ( data == CoinMasterRequest.AWOL )
		{
			this.removeFormField( "which" );
			this.addFormField( "doit", "69" );
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

	public static void parseBigBrotherVisit( final String location, final String responseText )
	{
		CoinmasterData data = CoinMasterRequest.BIGBROTHER;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "who=2" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
				CoinmastersFrame.externalUpdate();
			}

			return;
		}

		if ( !action.equals( "buyitem" ) )
		{
			return;
		}

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			CoinMasterRequest.refundPurchase( data, location );
		}

		CoinMasterRequest.parseBalance( data, responseText );
		CoinmastersFrame.externalUpdate();
	}

	public static void parseBountyVisit( final String location, final String responseText )
	{
		CoinmasterData data = CoinMasterRequest.BHH;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			// I'm still waiting for you to bring me 5 discarded
			// pacifiers, Bounty Hunter!

			Matcher matcher = CoinMasterRequest.BOUNTY_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				int count = StringUtilities.parseInt( matcher.group(1) );
				String name = matcher.group(2);
				AdventureResult ar = new AdventureResult( name, count, false );
				Preferences.setInteger( "currentBountyItem", ar.getItemId() );
			}

			if ( responseText.indexOf( "You acquire" ) != -1 )
			{
				// He turned in a bounty for a lucre
				CoinMasterRequest.abandonBounty();
				CoinmastersFrame.externalUpdate();
			}
			return;
		}

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			CoinMasterRequest.refundPurchase( data, location );
			CoinmastersFrame.externalUpdate();
		}
	}

	public static void parseIslandVisit( final String location, final String responseText )
	{
		if ( location.indexOf( "whichcamp" ) == -1 )
		{
			return;
		}

		CoinmasterData data = findCampMaster( location );
		if ( data == null )
		{
			return;
		}

		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			// Parse current coin balances
			CoinMasterRequest.parseBalance( data, responseText );
			CoinmastersFrame.externalUpdate();

			return;
		}

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			CoinMasterRequest.refundPurchase( data, location );
		}
		else if ( responseText.indexOf( "You don't have that many" ) != -1 )
		{
			CoinMasterRequest.refundSale( data, location );
		}

		CoinMasterRequest.parseBalance( data, responseText );
		CoinmastersFrame.externalUpdate();
	}

	private static final Pattern TATTOO_PATTERN = Pattern.compile( "sigils/aol(\\d+).gif" );
	public static void parseAWOLVisit( final String location, final String responseText )
	{
		CoinmasterData data = CoinMasterRequest.AWOL;
		// If you don't have enough commendations, you are redirected to inventory.php
		if ( location.startsWith( "inventory.php" ) )
		{
			if ( responseText.indexOf( "You don't have enough commendations" ) != -1 )
			{
				CoinMasterRequest.refundPurchase( data, CoinMasterRequest.lastURL );
				CoinMasterRequest.parseBalance( data, responseText );
				CoinmastersFrame.externalUpdate();
			}
			return;
		}

		// inv_use.php?whichitem=5116&pwd&doit=69&tobuy=xxx&howmany=yyy
		// You have 50 A. W. O. L. commendations.

		CoinMasterRequest.parseBalance( data, responseText );

		// Check which tattoo - if any - is for sale: sigils/aol3.gif
		Matcher m = TATTOO_PATTERN.matcher( responseText );
		CoinmastersDatabase.AWOLtattoo = m.find() ? StringUtilities.parseInt( m.group( 1 ) ) : 0;

		CoinmastersFrame.externalUpdate();
	}

	private static final Pattern CAMP_PATTERN = Pattern.compile( "whichcamp=(\\d+)" );
	public static CoinmasterData findCampMaster( final String urlString )
	{
		Matcher campMatcher = CoinMasterRequest.CAMP_PATTERN.matcher( urlString );
		if ( !campMatcher.find() )
		{
			return null;
		}

		String camp = campMatcher.group(1);

		if ( camp.equals( "1" ) )
		{
			return CoinMasterRequest.HIPPY;
		}

		if ( camp.equals( "2" ) )
		{
			return CoinMasterRequest.FRATBOY;
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

	protected static final void refundPurchase( final CoinmasterData data, final String urlString )
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
			return registerHunterRequest( urlString );
		}

		if ( urlString.startsWith( "monkeycastle.php" ) )
		{
			return registerSeaRequest( urlString );
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
			return registerTicketRequest( urlString );
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
			return registerIslandRequest( urlString );
		}

		if ( urlString.startsWith( "inv_use.php" ) )
		{
			return registerAWOLRequest( urlString );
		}

		if ( urlString.startsWith( "spaaace.php" ) )
		{
			return SpaaaceRequest.registerRequest( urlString );
		}

		return false;
	}

	private static final boolean registerHunterRequest( final String urlString )
	{
		CoinmasterData data = CoinMasterRequest.BHH;
		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "visit Bounty Hunter Hunter" );
			return true;
		}

		if ( action.equals( "takebounty" ) )
		{
			Matcher idMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			if ( !idMatcher.find() )
			{
				return true;
			}

			int itemId = StringUtilities.parseInt( idMatcher.group( 1 ) );
			Preferences.setInteger( "currentBountyItem", itemId );


			KoLAdventure adventure = AdventureDatabase.getBountyLocation( itemId );
			AdventureFrame.updateSelectedAdventure( adventure );
			AdventureResult bounty = AdventureDatabase.getBounty( adventure );
			String plural = ItemDatabase.getPluralName( itemId );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept bounty assignment to collect " + bounty.getCount() + " " + plural );
		}
		else if ( action.equals( "abandonbounty" ) )
		{
			CoinMasterRequest.abandonBounty();
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon bounty assignment" );
		}
		else if ( action.equals( "buy" ) )
		{
			CoinMasterRequest.buyStuff( data, urlString );
		}

		return true;
	}

	private static final void abandonBounty()
	{
		int itemId = Preferences.getInteger( "currentBountyItem" );
		if ( itemId == 0 )
		{
			return;
		}

		AdventureResult item = new AdventureResult( itemId, 1 );
		ResultProcessor.processResult( item.getInstance( 0 - item.getCount( KoLConstants.inventory ) ) );
		Preferences.setInteger( "currentBountyItem", 0 );
	}

	private static final boolean registerSeaRequest( final String urlString )
	{
		// We only claim monkeycastle.php?action=buyitem
		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		if ( !action.equals( "buyitem" ) )
		{
			return false;
		}

		CoinmasterData data = CoinMasterRequest.BIGBROTHER;
		CoinMasterRequest.buyStuff( data, urlString );
		return true;
	}

	private static final boolean registerTicketRequest( final String urlString )
	{
		// We only claim arcade.php?action=redeem

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		if ( !action.equals( "redeem" ) )
		{
			return false;
		}

		CoinmasterData data = CoinMasterRequest.TICKETCOUNTER;
		CoinMasterRequest.buyStuff( data, urlString );
		return true;
	}

	private static final boolean registerIslandRequest( final String urlString )
	{
		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return true;
		}

		CoinmasterData data = findCampMaster( urlString );
		if ( data == null )
		{
			return false;
		}

		if ( action.equals( "getgear" ) )
		{
			CoinMasterRequest.buyStuff( data, urlString );
		}
		else if ( action.equals( "turnin" ) )
		{
			CoinMasterRequest.sellStuff( data, urlString );
		}

		return true;
	}

	private static final boolean registerAWOLRequest( final String urlString )
	{
		// inv_use.php?whichitem=5116&pwd&doit=69&tobuy=xxx&howmany=yyy
		if ( urlString.indexOf( "whichitem=5116" ) == -1 )
		{
			return false;
		}

		// Save URL. If request fails, we are redirected to inventory.php
		CoinMasterRequest.lastURL = urlString;

		if ( urlString.indexOf( "doit=69" ) != -1 && urlString.indexOf( "tobuy" ) != -1	 )
		{
			CoinmasterData data = CoinMasterRequest.AWOL;
			CoinMasterRequest.buyStuff( data, urlString );
			return true;
		}

		return true;
	}

	protected static final void buyStuff( final CoinmasterData data, final String urlString )
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

		String token = data.getToken();
		String tokenName = ( cost != 1 ) ? ItemDatabase.getPluralName( token ) : token;
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + cost + " " + tokenName + " for " + count + " " + itemName );

		AdventureResult tokenItem = data.getItem();
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

	protected static final void sellStuff( final CoinmasterData data, final String urlString )
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

		String token = data.getToken();
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
