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
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern SNACK_PATTERN = Pattern.compile( "whichsnack=(\\d+)" );
	private static final Pattern TOBUY_PATTERN = Pattern.compile( "tobuy=(\\d+)" );
	private static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany=(\\d+)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );
	private static final Pattern BOUNTY_PATTERN = Pattern.compile( "I'm still waiting for you to bring me (\\d+) (.*?), Bounty Hunter!" );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? (dime|quarter|sand dollar|Crimbux|Game Grid redemption ticket|bone chips|CRIMBCO scrip|store credit|free snack voucher|A. W. O. L. commendation|lunar isotope)" );

	private static String lastURL = null;

	private static final CoinmasterData BHH =
		new CoinmasterData(
			CoinmastersDatabase.BHH,
			"lucre",
			null,
			"bhh.php",
			null,
			CoinmastersFrame.LUCRE,
			"availableLucre",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.HOWMANY_PATTERN,
			CoinmastersDatabase.getLucreItems(),
			CoinmastersDatabase.lucreBuyPrices(),
			null
			);
	private static final CoinmasterData HIPPY =
		new CoinmasterData(
			CoinmastersDatabase.HIPPY,
			"dime",
			"dime",
			"bigisland.php",
			"You don't have any dimes",
			null,
			"availableDimes",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.QUANTITY_PATTERN,
			CoinmastersDatabase.getDimeItems(),
			CoinmastersDatabase.dimeBuyPrices(),
			CoinmastersDatabase.dimeSellPrices()
			);
	private static final CoinmasterData FRATBOY =
		new CoinmasterData(
			CoinmastersDatabase.FRATBOY,
			"quarter",
			"quarter",
			"bigisland.php",
			"You don't have any quarters",
			null,
			"availableQuarters",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.QUANTITY_PATTERN,
			CoinmastersDatabase.getQuarterItems(),
			CoinmastersDatabase.quarterBuyPrices(),
			CoinmastersDatabase.quarterSellPrices()
			);
	private static final CoinmasterData BIGBROTHER =
		new CoinmasterData(
			CoinmastersDatabase.BIGBROTHER,
			"sand dollar",
			"sand dollar",
			"monkeycastle.php",
			"You haven't got any sand dollars",
			CoinmastersFrame.SAND_DOLLAR,
			"availableSandDollars",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.QUANTITY_PATTERN,
			CoinmastersDatabase.getSandDollarItems(),
			CoinmastersDatabase.sandDollarBuyPrices(),
			null
			);
	private static final CoinmasterData TICKETCOUNTER =
		new CoinmasterData(
			CoinmastersDatabase.TICKETCOUNTER,
			"ticket",
			"Game Grid redemption ticket",
			"arcade.php",
			"You currently have no Game Grid redemption tickets",
			CoinmastersFrame.TICKET,
			"availableTickets",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.QUANTITY_PATTERN,
			CoinmastersDatabase.getTicketItems(),
			CoinmastersDatabase.ticketBuyPrices(),
			null
			);
	private static final CoinmasterData GAMESHOPPE =
		new CoinmasterData(
			CoinmastersDatabase.GAMESHOPPE,
			"store credit",
			"store credit",
			"gamestore.php",
			"You currently have no store credit",
			null,
			"availableStoreCredits",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.QUANTITY_PATTERN,
			CoinmastersDatabase.getStoreCreditItems(),
			CoinmastersDatabase.storeCreditBuyPrices(),
			CoinmastersDatabase.storeCreditSellPrices()
			);
	private static final CoinmasterData FREESNACKS =
		new CoinmasterData(
			CoinmastersDatabase.FREESNACKS,
			"snack voucher",
			"free snack voucher",
			"gamestore.php",
			"The teen glances at your snack voucher",
			CoinmastersFrame.VOUCHER,
			"availableSnackVouchers",
			CoinMasterRequest.SNACK_PATTERN,
			null,
			CoinmastersDatabase.getSnackVoucherItems(),
			CoinmastersDatabase.snackVoucherBuyPrices(),
			null
			);
	private static final CoinmasterData CRIMBOCARTEL =
		new CoinmasterData(
			CoinmastersDatabase.CRIMBOCARTEL,
			"Crimbuck",
			"Crimbux",
			"crimbo09.php",
			"You do not currently have any Crimbux",
			CoinmastersFrame.CRIMBUCK,
			"availableCrimbux",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.HOWMANY_PATTERN,
			CoinmastersDatabase.getCrimbuckItems(),
			CoinmastersDatabase.crimbuckBuyPrices(),
			null
			);
	private static final CoinmasterData CRIMBCOGIFTSHOP =
		new CoinmasterData(
			CoinmastersDatabase.CRIMBCOGIFTSHOP,
			"CRIMBCO scrip",
			"CRIMBCO scrip",
			"crimbo10.php",
			"You don't have any CRIMBCO scrip",
			CoinmastersFrame.CRIMBCO_SCRIP,
			"availableCRIMBCOScrip",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.HOWMANY_PATTERN,
			CoinmastersDatabase.getScripItems(),
			CoinmastersDatabase.scripBuyPrices(),
			null
			);
	private static final CoinmasterData ALTAROFBONES =
		new CoinmasterData(
			CoinmastersDatabase.ALTAROFBONES,
			"bone chips",
			"bone chips",
			"bone_altar.php",
			"You have no bone chips",
			CoinmastersFrame.BONE_CHIPS,
			"availableBoneChips",
			CoinMasterRequest.ITEMID_PATTERN,
			null,
			CoinmastersDatabase.getBoneChipItems(),
			CoinmastersDatabase.boneChipBuyPrices(),
			null
			);
	private static final CoinmasterData AWOL =
		new CoinmasterData(
			CoinmastersDatabase.AWOL,
			"commendation",
			"A. W. O. L. commendation",
			"inv_use.php",
			null,
			CoinmastersFrame.AWOL,
			"availableCommendations",
			CoinMasterRequest.TOBUY_PATTERN,
			CoinMasterRequest.HOWMANY_PATTERN,
			CoinmastersDatabase.getCommendationItems(),
			CoinmastersDatabase.commendationBuyPrices(),
			null
			);
	private static final CoinmasterData ISOTOPE_SMITHERY =
		new CoinmasterData(
			CoinmastersDatabase.ISOTOPE_SMITHERY,
			"isotope",
			"lunar isotope",
			"spaaace.php?place=shop1",
			null,
			CoinmastersFrame.ISOTOPE,
			"availableIsotopes",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.QUANTITY_PATTERN,
			CoinmastersDatabase.getIsotope1Items(),
			CoinmastersDatabase.isotope1BuyPrices(),
			null
			);
	private static final CoinmasterData DOLLHAWKER =
		new CoinmasterData(
			CoinmastersDatabase.DOLLHAWKER,
			"isotope",
			"lunar isotope",
			"spaaace.php?place=shop2",
			null,
			CoinmastersFrame.ISOTOPE,
			"availableIsotopes",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.QUANTITY_PATTERN,
			CoinmastersDatabase.getIsotope2Items(),
			CoinmastersDatabase.isotope2BuyPrices(),
			null
			);
	private static final CoinmasterData LUNAR_LUNCH =
		new CoinmasterData(
			CoinmastersDatabase.LUNAR_LUNCH,
			"isotope",
			"lunar isotope",
			"spaaace.php?place=shop3",
			null,
			CoinmastersFrame.ISOTOPE,
			"availableIsotopes",
			CoinMasterRequest.ITEMID_PATTERN,
			CoinMasterRequest.QUANTITY_PATTERN,
			CoinmastersDatabase.getIsotope3Items(),
			CoinmastersDatabase.isotope3BuyPrices(),
			null
			);

	private static final CoinmasterData[] MASTERS = new CoinmasterData[]
	{
		CoinMasterRequest.BHH,
		CoinMasterRequest.HIPPY,
		CoinMasterRequest.FRATBOY,
		CoinMasterRequest.BIGBROTHER,
		CoinMasterRequest.TICKETCOUNTER,
		CoinMasterRequest.GAMESHOPPE,
		CoinMasterRequest.FREESNACKS,
		CoinMasterRequest.CRIMBOCARTEL,
		CoinMasterRequest.CRIMBCOGIFTSHOP,
		CoinMasterRequest.ALTAROFBONES,
		CoinMasterRequest.AWOL,
		CoinMasterRequest.ISOTOPE_SMITHERY,
		CoinMasterRequest.DOLLHAWKER,
		CoinMasterRequest.LUNAR_LUNCH,
	};

	private static String masterToURL( final String master )
	{
		for ( int i = 0; i < MASTERS.length; ++i )
		{
			CoinmasterData data = MASTERS[i];
			if ( master.equals( data.getMaster() ) )
			{
				return data.getURL();
			}
		}
		return "bogus.php";
	}

	private final String master;

	private String action = null;
	private int itemId = -1;
	private int quantity = 0;
	private String itemField = null;
	private boolean single = false;

	public CoinMasterRequest( final String master )
	{
		super( CoinMasterRequest.masterToURL( master ) );

		this.master = master;
		this.itemField = "whichitem";
		this.single = false;

		if ( master == CoinmastersDatabase.HIPPY )
		{
			this.addFormField( "place", "camp" );
			this.addFormField( "whichcamp", "1" );
		}
		else if ( master == CoinmastersDatabase.FRATBOY )
		{
			this.addFormField( "place", "camp" );
			this.addFormField( "whichcamp", "2" );
		}
		else if ( master == CoinmastersDatabase.FREESNACKS )
		{
			this.itemField = "whichsnack";
			this.single = true;
		}
		else if ( master == CoinmastersDatabase.AWOL )
		{
			this.addFormField( "whichitem", "5116" );
			this.addFormField( "which", "3" );
			this.addFormField( "ajax", "1" );
			this.itemField = "tobuy";
		}
	}

	public CoinMasterRequest( final String master, final String action )
	{
		this( master );
		if ( action != null )
		{
			this.action = action;
			this.addFormField( "action", action );
		}
	}

	public CoinMasterRequest( final String master, final String action, final int itemId, final int quantity )
	{
		this( master, action );

		this.itemId = itemId;
		this.addFormField( this.itemField, String.valueOf( itemId ) );
		this.quantity = quantity;

		if ( master == CoinmastersDatabase.HIPPY ||
		     master == CoinmastersDatabase.FRATBOY ||
		     master == CoinmastersDatabase.TICKETCOUNTER ||
		     master == CoinmastersDatabase.GAMESHOPPE ||
		     master == CoinmastersDatabase.ISOTOPE_SMITHERY ||
		     master == CoinmastersDatabase.DOLLHAWKER ||
		     master == CoinmastersDatabase.LUNAR_LUNCH )
		{
			this.addFormField( "quantity", String.valueOf( quantity ) );
		}
		else if ( master == CoinmastersDatabase.BIGBROTHER )
		{
			this.addFormField( "quantity", String.valueOf( quantity ) );
			this.addFormField( "who", "2" );
		}
		else if ( master == CoinmastersDatabase.BHH ||
			  master == CoinmastersDatabase.CRIMBOCARTEL ||
			  master == CoinmastersDatabase.CRIMBCOGIFTSHOP )
		{
			this.addFormField( "howmany", String.valueOf( quantity ) );
		}
		else if ( master == CoinmastersDatabase.AWOL )
		{
			this.removeFormField( "which" );
			this.addFormField( "howmany", String.valueOf( quantity ) );
			this.addFormField( "doit", "69" );
		}
	}

	public CoinMasterRequest( final String master, final String action, final int itemId )
	{
		this( master, action, itemId, 1 );
	}

	public CoinMasterRequest( final String master, final String action, final AdventureResult ar )
	{
		this( master, action, ar.getItemId(), ar.getCount() );
	}

	public Object run()
	{
		int visits = this.single ? this.quantity : 1;

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

	public static void parseCrimboCartelVisit( final String location, final String responseText )
	{
		CoinmasterData data = CoinMasterRequest.CRIMBOCARTEL;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "place=store" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
				CoinmastersFrame.externalUpdate();
			}

			return;
		}

		if ( !action.equals( "buygift" ) )
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

	public static void parseCRIMBCOGiftShopVisit( final String location, final String responseText )
	{
		CoinmasterData data = CoinMasterRequest.CRIMBCOGIFTSHOP;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "place=giftshop" ) != -1 )
			{
				// Parse current coin balances
				CoinMasterRequest.parseBalance( data, responseText );
				CoinmastersFrame.externalUpdate();
			}

			return;
		}

		if ( !action.equals( "buygift" ) )
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

	public static void parseGameShoppeVisit( final String location, final String responseText )
	{
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			if ( location.indexOf( "place=cashier" ) == -1 )
			{
				return;
			}
		}
		else if ( action.equals( "redeem" ) )
		{
			CoinmasterData data = CoinMasterRequest.GAMESHOPPE;
			if ( responseText.indexOf( "You don't have enough" ) != -1 )
			{
				CoinMasterRequest.refundPurchase( data, location );
			}
		}
		else if ( action.equals( "tradein" ) )
		{
			CoinmasterData data = CoinMasterRequest.GAMESHOPPE;
			// The teenager scowls. "You can't trade in cards you don't have."
			if ( responseText.indexOf( "You can't trade in cards you don't have" ) != -1 )
			{
				CoinMasterRequest.refundSale( data, location );
			}
		}
		else if ( action.equals( "buysnack" ) )
		{
			CoinmasterData data = CoinMasterRequest.FREESNACKS;
			if ( responseText.indexOf( "You can't" ) != -1 )
			{
				CoinMasterRequest.refundSale( data, location );
			}
		}
		else
		{
			// Some other action not associated with the cashier
			return;
		}

		// Parse current store credit and free snack balance
		CoinmasterData data = CoinMasterRequest.GAMESHOPPE;
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

	public static void parseSpaaaceVisit( final String location, final String responseText )
	{
		if ( location.indexOf( "place=shop" ) == -1 )
		{
			return;
		}

		CoinmasterData data = findIsotopeMaster( location );
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

		CoinMasterRequest.parseBalance( data, responseText );
		CoinmastersFrame.externalUpdate();
	}

	private static final Pattern SHOP_PATTERN = Pattern.compile( "place=shop(\\d+)" );
	public static CoinmasterData findIsotopeMaster( final String urlString )
	{
		Matcher shopMatcher = CoinMasterRequest.SHOP_PATTERN.matcher( urlString );
		if ( !shopMatcher.find() )
		{
			return null;
		}

		String shop = shopMatcher.group(1);

		if ( shop.equals( "1" ) )
		{
			return CoinMasterRequest.ISOTOPE_SMITHERY;
		}

		if ( shop.equals( "2" ) )
		{
			return CoinMasterRequest.DOLLHAWKER;
		}

		if ( shop.equals( "3" ) )
		{
			return CoinMasterRequest.LUNAR_LUNCH;
		}

		return null;
	}

	public static void parseBalance( final CoinmasterData data, final String responseText )
	{
		if ( data == null )
		{
			return;
		}

		String test = data.getTest();
		if ( test == null )
		{
			return;
		}

		boolean positive = ( data.getMaster() == CoinmastersDatabase.FREESNACKS );
		boolean found = responseText.indexOf( test ) != -1;
		String balance = "0";
		if ( positive == found )
		{
			Matcher matcher = CoinMasterRequest.TOKEN_PATTERN.matcher( responseText );
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

	private static final void refundPurchase( final CoinmasterData data, final String urlString )
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

		if ( urlString.startsWith( "crimbo10.php" ) )
		{
			return registerGiftShopRequest( urlString );
		}

		if ( urlString.startsWith( "arcade.php" ) )
		{
			return registerTicketRequest( urlString );
		}

		if ( urlString.startsWith( "gamestore.php" ) )
		{
			return registerGameStoreRequest( urlString );
		}

		if ( urlString.startsWith( "bone_altar.php" ) )
		{
			return registerBoneChipRequest( urlString );
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
			return registerSpaaaceRequest( urlString );
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

	private static final boolean registerCartelRequest( final String urlString )
	{
		// We only claim crimbo09.php?action=buygift

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		if ( !action.equals( "buygift" ) )
		{
			return false;
		}

		CoinmasterData data = CoinMasterRequest.CRIMBOCARTEL;
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

	private static final boolean registerGameStoreRequest( final String urlString )
	{
		// We only claim action=redeem, action=tradein, action=buysnack

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		if ( action.equals( "redeem" ) )
		{
			CoinmasterData data = CoinMasterRequest.GAMESHOPPE;
			CoinMasterRequest.buyStuff( data, urlString );
			return true;
		}

		if ( action.equals( "tradein" ) )
		{
			CoinmasterData data = CoinMasterRequest.GAMESHOPPE;
			CoinMasterRequest.sellStuff( data, urlString );
			return true;
		}

		if ( action.equals( "buysnack" ) )
		{
			CoinmasterData data = CoinMasterRequest.FREESNACKS;
			CoinMasterRequest.buyStuff( data, urlString );
			return true;
		}

		return false;
	}

	private static final boolean registerBoneChipRequest( final String urlString )
	{
		// We only claim bone_altar.php?action=buy

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		if ( !action.equals( "buy" ) )
		{
			return false;
		}

		CoinmasterData data = CoinMasterRequest.ALTAROFBONES;
		CoinMasterRequest.buyStuff( data, urlString );
		return true;
	}

	private static final boolean registerGiftShopRequest( final String urlString )
	{
		// We only claim crimbo10.php?action=buygift

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return false;
		}

		if ( !action.equals( "buygift" ) )
		{
			return false;
		}

		CoinmasterData data = CoinMasterRequest.CRIMBCOGIFTSHOP;
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

	private static final boolean registerSpaaaceRequest( final String urlString )
	{
		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return true;
		}

		CoinmasterData data = findIsotopeMaster( urlString );
		if ( data == null )
		{
			return false;
		}

		if ( action.equals( "buy" ) )
		{
			CoinMasterRequest.buyStuff( data, urlString );
		}

		return true;
	}

	private static final void buyStuff( final CoinmasterData data, final String urlString )
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

	private static final void sellStuff( final CoinmasterData data, final String urlString )
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
