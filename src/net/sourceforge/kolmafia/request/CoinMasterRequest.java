/**
 * Copyright (c) 2005-2010, KoLmafia development team
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinMasterRequest
	extends GenericRequest
{
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany=(\\d+)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	private static final Pattern CAMP_PATTERN = Pattern.compile( "whichcamp=(\\d+)" );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You currently have) (?:<b>)?(\\d+)(?:</b>)? (dime|quarter|sand dollar|Crimbux|Game Grid redemption ticket|bone chips)" );
	private static final Pattern BOUNTY_PATTERN = Pattern.compile( "I'm still waiting for you to bring me (\\d+) (.*?), Bounty Hunter!" );

	private static final String BHH = "Bounty Hunter Hunter";
	public static final String HIPPY = "Dimemaster";
	public static final String FRATBOY = "Quartersmaster";
	private static final String BIGBROTHER = "Big Brother";
	private static final String CRIMBOCARTEL = "Crimbo Cartel";
	private static final String TICKETCOUNTER = "Ticket Counter";
	private static final String ALTAROFBONES = "Altar of Bones";

	private final String token;
	private final String master;
	private String action = null;
	private int itemId = -1;
	private int quantity = 0;

	public CoinMasterRequest( final String token )
	{
		super( CoinMasterRequest.chooseURL( token ) );

		this.token = token;

		if ( token.equals( "dime" ) )
		{
			this.addFormField( "place", "camp" );
			this.addFormField( "whichcamp", "1" );
			this.master = HIPPY;
		}
		else if ( token.equals( "quarter" ) )
		{
			this.addFormField( "place", "camp" );
			this.addFormField( "whichcamp", "2" );
			this.master = FRATBOY;
		}
		else if ( token.equals( "lucre" ) )
		{
			this.master = BHH;
		}
		else if ( token.equals( "sand dollar" ) )
		{
			this.master = BIGBROTHER;
		}
		else if ( token.equals( "Crimbuck" ) )
		{
			this.master = CRIMBOCARTEL;
		}
		else if ( token.equals( "ticket" ) )
		{
			this.master = TICKETCOUNTER;
		}
		else if ( token.equals( "bone chips" ) )
		{
			this.master = ALTAROFBONES;
		}
		else
		{
			this.master = "Coinmaster";
		}
	}

	public CoinMasterRequest( final String token, final String action )
	{
		this( token );

		this.action = action;

		this.addFormField( "action", action );
	}

	public CoinMasterRequest( final String token, final String action, final int itemId )
	{
		this( token );

		this.action = action;
		this.itemId = itemId;

		this.addFormField( "action", action );
		this.addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public CoinMasterRequest( final String token, final String action, final int itemId, final int quantity )
	{
		this( token );

		this.action = action;
		this.itemId = itemId;
		this.quantity = quantity;

		this.addFormField( "action", action );
		this.addFormField( "whichitem", String.valueOf( itemId ) );

		if ( master == HIPPY || master == FRATBOY || master == TICKETCOUNTER )
		{
			this.addFormField( "quantity", String.valueOf( quantity ) );
		}
		else if ( master == BIGBROTHER )
		{
			this.addFormField( "quantity", String.valueOf( quantity ) );
			this.addFormField( "who", "2" );
		}
		else if ( master == BHH || master == CRIMBOCARTEL )
		{
			this.addFormField( "howmany", String.valueOf( quantity ) );
		}
	}

	public CoinMasterRequest( final String token, final String action, final AdventureResult ar )
	{
		this( token, action, ar.getItemId(), ar.getCount() );
	}

	private static String chooseURL( final String token )
	{
		if ( token.equals( "lucre" ) )
		{
			return "bhh.php";
		}

		if ( token.equals( "sand dollar" ) )
		{
			return "monkeycastle.php";
		}

		if ( token.equals( "Crimbuck" ) )
		{
			return "crimbo09.php";
		}

		if ( token.equals( "ticket" ) )
		{
			return "arcade.php";
		}

		if ( token.equals( "bone chips" ) )
		{
			return "bone_altar.php";
		}

		if ( token.equals( "dime" ) || token.equals( "quarter" ) )
		{
			return "bigisland.php";
		}

		return "bogus.php";
	}

	public void run()
	{
		KoLmafia.updateDisplay( "Visiting the " + master + "..." );

		super.run();

		if ( KoLmafia.permitsContinue() )
		{
			KoLmafia.updateDisplay( master + " successfully looted!" );
		}
	}

	public static void parseBigBrotherVisit( final String location, final String responseText )
	{
		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( location );
		if ( !actionMatcher.find() )
		{
			if ( location.indexOf( "who=2" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( BIGBROTHER, responseText );
				CoinmastersFrame.externalUpdate();
			}

			return;
		}

		String action = actionMatcher.group(1);
		if ( !action.equals( "buyitem" ) )
		{
			return;
		}

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			CoinMasterRequest.refundPurchase( location, BIGBROTHER );
		}

		CoinMasterRequest.parseBalance( BIGBROTHER, responseText );
		CoinmastersFrame.externalUpdate();
	}

	public static void parseCrimboCartelVisit( final String location, final String responseText )
	{
		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( location );
		if ( !actionMatcher.find() )
		{
			if ( location.indexOf( "place=store" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( CRIMBOCARTEL, responseText );
				CoinmastersFrame.externalUpdate();
			}

			return;
		}

		String action = actionMatcher.group(1);
		if ( !action.equals( "buygift" ) )
		{
			return;
		}

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			CoinMasterRequest.refundPurchase( location, CRIMBOCARTEL );
		}

		CoinMasterRequest.parseBalance( CRIMBOCARTEL, responseText );
		CoinmastersFrame.externalUpdate();
	}

	public static void parseBountyVisit( final String location, final String responseText )
	{
		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( location );
		if ( !actionMatcher.find() )
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
			CoinMasterRequest.refundPurchase( location, BHH );
			CoinmastersFrame.externalUpdate();
		}
	}

	public static void parseIslandVisit( final String location, final String responseText )
	{
		if ( location.indexOf( "whichcamp" ) == -1 )
		{
			return;
		}

		String master = findCampMaster( location );
		if ( master == null )
		{
			return;
		}

		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( location );
		if ( !actionMatcher.find() )
		{
			// Parse current coin balances
			CoinMasterRequest.parseBalance( master, responseText );
			CoinmastersFrame.externalUpdate();

			return;
		}

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			CoinMasterRequest.refundPurchase( location, master );
		}
		else if ( responseText.indexOf( "You don't have that many" ) != -1 )
		{
			CoinMasterRequest.refundSale( location, master );
		}

		CoinMasterRequest.parseBalance( master, responseText );
		CoinmastersFrame.externalUpdate();
	}

	public static String findCampMaster( final String urlString )
	{
		Matcher campMatcher = CoinMasterRequest.CAMP_PATTERN.matcher( urlString );
		if ( !campMatcher.find() )
		{
			return null;
		}

		String camp = campMatcher.group(1);

		if ( camp.equals( "1" ) )
		{
			return HIPPY;
		}

		if ( camp.equals( "2" ) )
		{
			return FRATBOY;
		}

		return null;
	}

	public static void parseBalance( final String master, final String responseText )
	{
		String test;

		if ( master == HIPPY )
		{
			test = "You don't have any dimes";
		}
		else if ( master == FRATBOY )
		{
			test = "You don't have any quarters";
		}
		else if ( master == BIGBROTHER )
		{
			test = "You haven't got any sand dollars";
		}
		else if ( master == CRIMBOCARTEL )
		{
			test = "You do not currently have any Crimbux";
		}
		else if ( master == TICKETCOUNTER )
		{
			test = "You currently have no Game Grid redemption tickets";
		}
		else if ( master == ALTAROFBONES )
		{
			test = "You have no bone chips";
		}
		else
		{
			return;
		}

		String balance = "0";
		if ( responseText.indexOf( test ) == -1 )
		{
			Matcher matcher = CoinMasterRequest.TOKEN_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				balance = matcher.group(1);
			}
		}

		if ( master == HIPPY )
		{
			Preferences.setString( "availableDimes", balance );
		}
		else if ( master == FRATBOY )
		{
			Preferences.setString( "availableQuarters", balance );
		}
		else if ( master == BIGBROTHER )
		{
			// Check and adjust inventory count, just in case
			int count = StringUtilities.parseInt( balance );
			AdventureResult item = ItemPool.get( ItemPool.SAND_DOLLAR, count );
			int icount = item.getCount( KoLConstants.inventory );
			if ( count != icount )
			{
				item = item.getInstance( count - icount );
				AdventureResult.addResultToList( KoLConstants.inventory, item );
			}
		}
		else if ( master == CRIMBOCARTEL )
		{
			// Check and adjust inventory count, just in case
			int count = StringUtilities.parseInt( balance );
			AdventureResult item = ItemPool.get( ItemPool.CRIMBUCK, count );
			int icount = item.getCount( KoLConstants.inventory );
			if ( count != icount )
			{
				item = item.getInstance( count - icount );
				AdventureResult.addResultToList( KoLConstants.inventory, item );
			}
		}
		else if ( master == TICKETCOUNTER )
		{
			// Check and adjust inventory count, just in case
			int count = StringUtilities.parseInt( balance );
			AdventureResult item = ItemPool.get( ItemPool.GG_TICKET, count );
			int icount = item.getCount( KoLConstants.inventory );
			if ( count != icount )
			{
				item = item.getInstance( count - icount );
				AdventureResult.addResultToList( KoLConstants.inventory, item );
			}
		}
		else if ( master == ALTAROFBONES )
		{
			// Check and adjust inventory count, just in case
			int count = StringUtilities.parseInt( balance );
			AdventureResult item = ItemPool.get( ItemPool.BONE_CHIPS, count );
			int icount = item.getCount( KoLConstants.inventory );
			if ( count != icount )
			{
				item = item.getInstance( count - icount );
				AdventureResult.addResultToList( KoLConstants.inventory, item );
			}
		}
	}

	private static final void refundPurchase( final String urlString, final String master )
	{
		Matcher itemMatcher;
		Matcher countMatcher;
		Map prices;
		String property;
		String token;

		if ( master == BHH )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.HOWMANY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.lucreBuyPrices();
			property = "availableLucre";
			token = "lucre";
		}
		else if ( master == BIGBROTHER )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.sandDollarBuyPrices();
			property = "availableSandDollars";
			token = "sand dollars";
		}
		else if ( master == CRIMBOCARTEL )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.HOWMANY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.crimbuckBuyPrices();
			property = "availableCrimbux";
			token = "Crimbuck";
		}
		else if ( master == TICKETCOUNTER )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.ticketBuyPrices();
			property = "availableTickets";
			token = "tickets";
		}
		else if ( master == ALTAROFBONES )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = null;
			prices = CoinmastersDatabase.boneChipBuyPrices();
			property = "availableBoneChips";
			token = "bone chips";
		}
		else if ( master == HIPPY )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.dimeBuyPrices();
			property = "availableDimes";
			token = "dimes";
		}
		else if ( master == FRATBOY )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.quarterBuyPrices();
			property = "availableQuarters";
			token = "quarters";
		}
		else
		{
			return;
		}

		int cost = getPurchaseCost( itemMatcher, countMatcher, prices );
		Preferences.increment( property, cost );

		if ( master == BHH )
		{
			AdventureResult lucres = CoinmastersFrame.LUCRE.getInstance( cost );
			ResultProcessor.processResult( lucres );
		}
		else if ( master == BIGBROTHER )
		{
			AdventureResult sandDollars = CoinmastersFrame.SAND_DOLLAR.getInstance( cost );
			ResultProcessor.processResult( sandDollars );
		}
		else if ( master == CRIMBOCARTEL )
		{
			AdventureResult crimbux = CoinmastersFrame.CRIMBUCK.getInstance( cost );
			ResultProcessor.processResult( crimbux );
		}
		else if ( master == TICKETCOUNTER )
		{
			AdventureResult tickets = CoinmastersFrame.TICKET.getInstance( cost );
			ResultProcessor.processResult( tickets );
		}
		else if ( master == ALTAROFBONES )
		{
			AdventureResult bone_chips = CoinmastersFrame.BONE_CHIPS.getInstance( cost );
			ResultProcessor.processResult( bone_chips );
		}

		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have enough " + token + " to buy that." );
	}

	private static int getPurchaseCost( final Matcher itemMatcher, final Matcher countMatcher, final Map prices )
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

	public static final void refundSale( final String urlString, final String master )
	{
		Map prices;
		String property;

		if ( master == HIPPY )
		{
			prices = CoinmastersDatabase.dimeSellPrices();
			property = "availableDimes";
		}
		else if ( master == FRATBOY )
		{
			prices = CoinmastersDatabase.quarterSellPrices();
			property = "availableQuarters";
		}
		else
		{
			return;
		}

		Matcher itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
		Matcher countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
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
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;

		Preferences.increment( property, -cost );

		String plural = ItemDatabase.getPluralName( itemId );
		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have that many " + plural );
	}

	public static final boolean registerRequest( final String urlString )
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
			return registerCartelRequest( urlString );
		}

		if ( urlString.startsWith( "arcade.php" ) )
		{
			return registerTicketRequest( urlString );
		}

		if ( urlString.startsWith( "bone_altar.php" ) )
		{
			return registerBoneChipRequest( urlString );
		}

		if ( urlString.startsWith( "bigisland.php" ) )
		{
			return registerIslandRequest( urlString );
		}

		return false;
	}

	private static final boolean registerHunterRequest( final String urlString )
	{
		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !actionMatcher.find() )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "visit Bounty Hunter Hunter" );
			return true;
		}

		String action = actionMatcher.group(1);

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
			CoinMasterRequest.buyStuff( urlString, BHH );
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

		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !actionMatcher.find() )
		{
			return false;
		}

		String action = actionMatcher.group(1);

		if ( !action.equals( "buyitem" ) )
		{
			return false;
		}

		CoinMasterRequest.buyStuff( urlString, BIGBROTHER );
		return true;
	}

	private static final boolean registerCartelRequest( final String urlString )
	{
		// We only claim crimbo09.php?action=buygift

		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !actionMatcher.find() )
		{
			return false;
		}

		String action = actionMatcher.group(1);

		if ( !action.equals( "buygift" ) )
		{
			return false;
		}

		CoinMasterRequest.buyStuff( urlString, CRIMBOCARTEL );
		return true;
	}

	private static final boolean registerTicketRequest( final String urlString )
	{
		// We only claim arcade.php?action=redeem

		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !actionMatcher.find() )
		{
			return false;
		}

		String action = actionMatcher.group(1);

		if ( !action.equals( "redeem" ) )
		{
			return false;
		}

		CoinMasterRequest.buyStuff( urlString, TICKETCOUNTER );
		return true;
	}

	private static final boolean registerBoneChipRequest( final String urlString )
	{
		// We only claim bone_altar.php?action=buy

		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !actionMatcher.find() )
		{
			return false;
		}

		String action = actionMatcher.group(1);

		if ( !action.equals( "buy" ) )
		{
			return false;
		}

		CoinMasterRequest.buyStuff( urlString, ALTAROFBONES );
		return true;
	}

	private static final boolean registerIslandRequest( final String urlString )
	{
		String master = findCampMaster( urlString );
		if ( master == null )
		{
			return false;
		}

		Matcher actionMatcher = GenericRequest.ACTION_PATTERN.matcher( urlString );
		if ( !actionMatcher.find() )
		{
			return true;
		}

		String action = actionMatcher.group(1);

		if ( action.equals( "getgear" ) )
		{
			CoinMasterRequest.buyStuff( urlString, master );
		}
		else if ( action.equals( "turnin" ) )
		{
			CoinMasterRequest.sellStuff( urlString, master );
		}

		return true;
	}

	private static final void buyStuff( final String urlString, final String master )
	{
		Matcher itemMatcher;
		Matcher countMatcher;
		Map prices;
		String token;
		String property;

		if ( master == BHH )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.HOWMANY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.lucreBuyPrices();
			token = "filthy lucre";
			property = "availableLucre";
		}
		else if ( master == BIGBROTHER )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.sandDollarBuyPrices();
			token = "sand dollar";
			property = "availableSandDollars";
		}
		else if ( master == CRIMBOCARTEL )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.HOWMANY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.crimbuckBuyPrices();
			token = "Crimbuck";
			property = "availableCrimbux";
		}
		else if ( master == TICKETCOUNTER )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.ticketBuyPrices();
			token = "ticket";
			property = "availableTickets";
		}
		else if ( master == ALTAROFBONES )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = null;
			prices = CoinmastersDatabase.boneChipBuyPrices();
			token = "bone chips";
			property = "availableBoneChips";
		}
		else if ( master == HIPPY )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.dimeBuyPrices();
			token = "dime";
			property = "availableDimes";
		}
		else if ( master == FRATBOY )
		{
			itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
			countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.quarterBuyPrices();
			token = "quarter";
			property = "availableQuarters";
		}
		else
		{
			return;
		}

		if ( !itemMatcher.find() )
		{
			return;
		}

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
		String name = ItemDatabase.getItemName( itemId );
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;
		String tokenName = ( cost != 1 ) ? ItemDatabase.getPluralName( token ) : token;
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + cost + " " + tokenName + " for " + count + " " + itemName );

		if ( master == BHH )
		{
			AdventureResult lucres = CoinmastersFrame.LUCRE.getInstance( -cost );
			ResultProcessor.processResult( lucres );
		}
		else if ( master == BIGBROTHER )
		{
			AdventureResult sandDollars = CoinmastersFrame.SAND_DOLLAR.getInstance( -cost );
			ResultProcessor.processResult( sandDollars );
		}
		else if ( master == CRIMBOCARTEL )
		{
			AdventureResult crimbux = CoinmastersFrame.CRIMBUCK.getInstance( -cost );
			ResultProcessor.processResult( crimbux );
		}
		else if ( master == TICKETCOUNTER )
		{
			AdventureResult tickets = CoinmastersFrame.TICKET.getInstance( -cost );
			ResultProcessor.processResult( tickets );
		}
		else if ( master == ALTAROFBONES )
		{
			AdventureResult boneChips = CoinmastersFrame.BONE_CHIPS.getInstance( -cost );
			ResultProcessor.processResult( boneChips );
		}

		Preferences.increment( property, -cost );
		CoinmastersFrame.externalUpdate();
	}

	private static final void sellStuff( final String urlString, final String master )
	{
		Matcher itemMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( urlString );
		Matcher countMatcher = CoinMasterRequest.QUANTITY_PATTERN.matcher( urlString );

		if ( !itemMatcher.find() || !countMatcher.find() )
		{
			return;
		}

		Map prices;
		String token;
		String property;

		if ( master == HIPPY )
		{
			prices = CoinmastersDatabase.dimeSellPrices();
			token = "dime";
			property = "availableDimes";
		}
		else if ( master == FRATBOY )
		{
			prices = CoinmastersDatabase.quarterSellPrices();
			token = "quarter";
			property = "availableQuarters";
		}
		else
		{
			return;
		}

		int itemId = StringUtilities.parseInt( itemMatcher.group(1) );
		String name = ItemDatabase.getItemName( itemId );
		int count = StringUtilities.parseInt( countMatcher.group(1) );
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;
		String tokenName = ( cost != 1 ) ? ItemDatabase.getPluralName( token ) : token;
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + count + " " + itemName + " for " + cost + " " + tokenName );

		AdventureResult item = new AdventureResult( itemId, -count );
		ResultProcessor.processResult( item );

		Preferences.increment( property, cost );
		CoinmastersFrame.externalUpdate();
	}
}
