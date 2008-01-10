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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoinmasterRequest
	extends KoLRequest
{
	private static final Pattern ITEMID_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]+)" );
	private static final Pattern BHH_BUY_PATTERN = Pattern.compile( "whichitem=(\\d+).*?howmany=(\\d+)" );
			// bhh.php?pwd&action=buy&whichitem=xxx&howmany=yyy 

	private static final String BHH = "Bounty Hunter Hunter";
	private static final String HIPPY = "Dimemaster";
	private static final String FRATBOY = "Quartersmaster";

	private final String token;
	private final String master;
	private String action = null;
	private int itemId = -1;
	private int quantity = 0;

	public CoinmasterRequest( final String token )
	{
		super( CoinmasterRequest.chooseURL( token ) );

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

	public CoinmasterRequest( final String token, final String action )
	{
		this( token );

		this.action = action;

		this.addFormField( "action", action );
		this.addFormField( "pwd" );
	}

	public CoinmasterRequest( final String token, final String action, final int itemId )
	{
		this( token );

		this.action = action;
		this.itemId = itemId;

		this.addFormField( "action", action );
		this.addFormField( "pwd" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );
	}

	public CoinmasterRequest( final String token, final String action, final int itemId, final int quantity )
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
	}

	public void processResults()
	{
		CoinmasterRequest.parseVisit( this.getURLString(), this.responseText );
		KoLmafia.updateDisplay( master + " successfully looted!" );
	}

	public static void parseVisit( final String location, final String responseText )
	{
		if ( location.startsWith( "bhh.php" ) )
		{
			parseBountyVisit( location, responseText );
		}
		else if ( location.startsWith( "bigisland.php" ) )
		{
			parseIslandVisit( location, responseText );
		}
	}

	public static void parseBountyVisit( final String location, final String responseText )
	{
		Matcher actionMatcher = CoinmasterRequest.ACTION_PATTERN.matcher( location );
		if ( !actionMatcher.find() )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "visit bounty hunter hunter" );

			if ( responseText.indexOf( "You acquire" ) != -1 )
			{
				// He turned in a bounty for a lucre
				CoinmasterRequest.abandonBounty();
				CoinmastersFrame.externalUpdate();
			}
			return;
		}

		if ( responseText.indexOf( "You don't have enough" ) != -1 )
		{
			// Get those lucres back.

			Matcher matcher = CoinmasterRequest.BHH_BUY_PATTERN.matcher( location );
			Map prices = CoinmastersDatabase.lucreBuyPrices();
			int cost = getPurchaseCost( matcher, prices );

			AdventureResult lucres = CoinmastersFrame.LUCRE.getInstance( cost );
			StaticEntity.getClient().processResult( lucres );
		}
	}

	private static int getPurchaseCost( final Matcher matcher, final Map prices )
	{
		if ( !matcher.find() )
		{
			return 0;
		}

		int itemId = StaticEntity.parseInt( matcher.group(1) );
		String name = TradeableItemDatabase.getItemName( itemId );
		int count = StaticEntity.parseInt( matcher.group(2) );
		int price = CoinmastersDatabase.getPrice( name, prices );
		return count * price;
	}

	public static void parseIslandVisit( final String location, final String responseText )
	{
		if ( location.indexOf( "whichcamp" ) == -1 )
		{
			return;
		}

                System.out.println( "Visiting coinmaster at location = \"" + location + "\"" );

		CoinmastersFrame.externalUpdate();
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
		Matcher actionMatcher = CoinmasterRequest.ACTION_PATTERN.matcher( urlString );
		if ( !actionMatcher.find() )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "visit bounty hunter hunter" );
			return true;
		}

		String action = actionMatcher.group(1);

		if ( action.equals( "takebounty" ) )
		{
			Matcher idMatcher = CoinmasterRequest.ITEMID_PATTERN.matcher( urlString );
			if ( !idMatcher.find() )
			{
				return true;
			}

			KoLSettings.setUserProperty( "currentBountyItem", idMatcher.group( 1 ) );
			int itemId = StaticEntity.parseInt( idMatcher.group( 1 ) );
			AdventureFrame.updateSelectedAdventure( AdventureDatabase.getBountyLocation( itemId ) );

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept bounty assignment to collect " + AdventureDatabase.getBounty( itemId ) );
		}
		else if ( action.equals( "abandonbounty" ) )
		{
			CoinmasterRequest.abandonBounty();
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon bounty assignment" );
		}
		else if ( action.equals( "buy" ) )
		{
			Matcher buyMatcher = CoinmasterRequest.BHH_BUY_PATTERN.matcher( urlString );
			if ( !buyMatcher.find() )
			{
				return true;
			}

			int itemId = StaticEntity.parseInt( buyMatcher.group(1) );
			String name = TradeableItemDatabase.getItemName( itemId );
			int count = StaticEntity.parseInt( buyMatcher.group(2) );
			Map prices = CoinmastersDatabase.lucreBuyPrices();
			int price = CoinmastersDatabase.getPrice( name, prices );
			int cost = count * price;

			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "trading " + cost + " filthy lucre for " + count + " " + name );

			AdventureResult lucres = CoinmastersFrame.LUCRE.getInstance( -1 * cost );
			StaticEntity.getClient().processResult( lucres );
		}
		else
		{
			// Unknown action
			return false;
		}

		return true;
	}

	private static final void abandonBounty()
	{
		int itemId = KoLSettings.getIntegerProperty( "currentBountyItem" );
		if ( itemId == 0 )
		{
			return;
		}

		AdventureResult item = new AdventureResult( itemId, 1 );
		StaticEntity.getClient().processResult( item.getInstance( 0 - item.getCount( KoLConstants.inventory ) ) );
		KoLSettings.setUserProperty( "currentBountyItem", "0" );
	}

	private static final boolean registerIslandRequest( final String urlString )
	{
		return false;
	}
}
