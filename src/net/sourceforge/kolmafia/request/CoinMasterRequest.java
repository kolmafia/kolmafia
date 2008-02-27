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

package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.swingui.AdventureFrame;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinMasterRequest
	extends GenericRequest
{
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]+)" );
	private static final Pattern CAMP_PATTERN = Pattern.compile( "whichcamp=(\\d+)" );
	private static final Pattern BHH_BUY_PATTERN = Pattern.compile( "whichitem=(\\d+).*?howmany=(\\d+)" );
	private static final Pattern CAMP_TRADE_PATTERN = Pattern.compile( "whichitem=(\\d+).*?quantity=(\\d+)" );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You've.*?got (\\d+) (dime|quarter)" );

	private static final String BHH = "Bounty Hunter Hunter";
	private static final String HIPPY = "Dimemaster";
	private static final String FRATBOY = "Quartersmaster";

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
		this.addFormField( "pwd" );
	}

	public CoinMasterRequest( final String token, final String action, final int itemId )
	{
		this( token );

		this.action = action;
		this.itemId = itemId;

		this.addFormField( "action", action );
		this.addFormField( "pwd" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public CoinMasterRequest( final String token, final String action, final int itemId, final int quantity )
	{
		this( token );

		this.action = action;
		this.itemId = itemId;
		this.quantity = quantity;

		this.addFormField( "action", action );
		this.addFormField( "pwd" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );

		if ( master == HIPPY || master == FRATBOY )
		{
			this.addFormField( "quantity", String.valueOf( quantity ) );
		}
		else if ( master == BHH )
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

	public static void parseBountyVisit( final String location, final String responseText )
	{
		Matcher actionMatcher = CoinMasterRequest.ACTION_PATTERN.matcher( location );
		if ( !actionMatcher.find() )
		{
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

		Matcher actionMatcher = CoinMasterRequest.ACTION_PATTERN.matcher( location );
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
		String balance = "0";
		String property;
		String test;

		if ( master == HIPPY )
		{
			property = "availableDimes";
			test = "You don't have any dimes";
		}
		else if ( master == FRATBOY )
		{
			property = "availableQuarters";
			test = "You don't have any quarters";
		}
		else
		{
			return;
		}

		if ( responseText.indexOf( test ) == -1 )
		{
			Matcher matcher = CoinMasterRequest.TOKEN_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				balance = matcher.group(1);
			}
		}

		Preferences.setString( property, balance );
	}

	private static final void refundPurchase( final String urlString, final String master )
	{
		Matcher matcher;
		Map prices;
		String property;
		String token;

		if ( master == BHH )
		{
			matcher = CoinMasterRequest.BHH_BUY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.lucreBuyPrices();
			property = "availableLucre";
			token = "lucre";
		}
		else if ( master == HIPPY )
		{
			matcher = CoinMasterRequest.CAMP_TRADE_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.dimeBuyPrices();
			property = "availableDimes";
			token = "dimes";
		}
		else if ( master == FRATBOY )
		{
			matcher = CoinMasterRequest.CAMP_TRADE_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.quarterBuyPrices();
			property = "availableQuarters";
			token = "quarters";
		}
		else
		{
			return;
		}

		int cost = getPurchaseCost( matcher, prices );
		Preferences.increment( property, cost );

		if ( master == BHH )
		{
			AdventureResult lucres = CoinmastersFrame.LUCRE.getInstance( cost );
			StaticEntity.getClient().processResult( lucres );
		}

		KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have enough " + token + " to buy that." );
	}

	private static int getPurchaseCost( final Matcher matcher, final Map prices )
	{
		if ( !matcher.find() )
		{
			return 0;
		}

		int itemId = StringUtilities.parseInt( matcher.group(1) );
		String name = ItemDatabase.getItemName( itemId );
		int count = StringUtilities.parseInt( matcher.group(2) );
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

		Matcher matcher = CoinMasterRequest.CAMP_TRADE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return;
		}

		int itemId = StringUtilities.parseInt( matcher.group(1) );
		int count = StringUtilities.parseInt( matcher.group(2) );

		// Get back the items we failed to turn in
		AdventureResult item = new AdventureResult( itemId, count );
		StaticEntity.getClient().processResult( item );

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

		if ( urlString.startsWith( "bigisland.php" ) )
		{
			return registerIslandRequest( urlString );
		}

		return false;
	}

	private static final boolean registerHunterRequest( final String urlString )
	{
		Matcher actionMatcher = CoinMasterRequest.ACTION_PATTERN.matcher( urlString );
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

			AdventureFrame.updateSelectedAdventure( AdventureDatabase.getBountyLocation( itemId ) );
			AdventureResult bounty = AdventureDatabase.getBounty( itemId );
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
		StaticEntity.getClient().processResult( item.getInstance( 0 - item.getCount( KoLConstants.inventory ) ) );
		Preferences.setInteger( "currentBountyItem", 0 );
	}

	private static final boolean registerIslandRequest( final String urlString )
	{
		String master = findCampMaster( urlString );
		if ( master == null )
		{
			return false;
		}

		Matcher actionMatcher = CoinMasterRequest.ACTION_PATTERN.matcher( urlString );
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
		Matcher tradeMatcher;
		Map prices;
		String token;
		String property;

		if ( master == BHH )
		{
			tradeMatcher = CoinMasterRequest.BHH_BUY_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.lucreBuyPrices();
			token = "filthy lucre";
			property = "availableLucre";
		}
		else if ( master == HIPPY )
		{
			tradeMatcher = CoinMasterRequest.CAMP_TRADE_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.dimeBuyPrices();
			token = "dime";
			property = "availableDimes";
		}
		else if ( master == FRATBOY )
		{
			tradeMatcher = CoinMasterRequest.CAMP_TRADE_PATTERN.matcher( urlString );
			prices = CoinmastersDatabase.quarterBuyPrices();
			token = "quarter";
			property = "availableQuarters";
		}
		else
		{
			return;
		}

		if ( !tradeMatcher.find() )
		{
			return;
		}

		int itemId = StringUtilities.parseInt( tradeMatcher.group(1) );
		String name = ItemDatabase.getItemName( itemId );
		int count = StringUtilities.parseInt( tradeMatcher.group(2) );
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;
		String tokenName = ( cost > 1 ) ? ( token + "s" ) : "token";
		String itemName = ( count > 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + cost + " " + tokenName + " for " + count + " " + itemName );

		if ( master == BHH )
		{
			AdventureResult lucres = CoinmastersFrame.LUCRE.getInstance( -cost );
			StaticEntity.getClient().processResult( lucres );
		}

		Preferences.increment( property, -cost );
		CoinmastersFrame.externalUpdate();
	}

	private static final void sellStuff( final String urlString, final String master )
	{
		Matcher tradeMatcher = CoinMasterRequest.CAMP_TRADE_PATTERN.matcher( urlString );
		if ( !tradeMatcher.find() )
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

		int itemId = StringUtilities.parseInt( tradeMatcher.group(1) );
		String name = ItemDatabase.getItemName( itemId );
		int count = StringUtilities.parseInt( tradeMatcher.group(2) );
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;
		String tokenName = ( cost > 1 ) ? ( token + "s" ) : "token";
		String itemName = ( count > 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + count + " " + itemName + " for " + cost + " " + tokenName );

		AdventureResult item = new AdventureResult( itemId, -count );
		StaticEntity.getClient().processResult( item );

		Preferences.increment( property, cost );
		CoinmastersFrame.externalUpdate();
	}
}
