/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.swingui.CoinmastersFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Crimbo11Request
	extends CoinMasterRequest
{
	public static final String master = "Crimbo 2011"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( Crimbo11Request.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( Crimbo11Request.master );
	private static final Map sellPrices = CoinmastersDatabase.getSellPrices( Crimbo11Request.master );
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You currently have.*?<b>([\\d,]+)</b> Candy Credit", Pattern.DOTALL );
	public static final CoinmasterData CRIMBO11 =
		new CoinmasterData(
			Crimbo11Request.master,
			Crimbo11Request.class,
			"crimbo11.php",
			"Candy Credit",
			null,
			false,
			Crimbo11Request.TOKEN_PATTERN,
			null,
			"availableCandyCredits",
			"whichitem",
			GenericRequest.WHICHITEM_PATTERN,
			"howmany",
			GenericRequest.HOWMANY_PATTERN,
			"reallybuygifts",
			Crimbo11Request.buyItems,
			Crimbo11Request.buyPrices,
			"tradecandy",
			Crimbo11Request.sellPrices,
			null,
			null,
			false,
			null
			);

	public Crimbo11Request()
	{
		super( Crimbo11Request.CRIMBO11 );
	}

	public Crimbo11Request( final String action )
	{
		super( Crimbo11Request.CRIMBO11, action );
	}

	public Crimbo11Request( final String action, final AdventureResult [] attachments )
	{
		super( Crimbo11Request.CRIMBO11, action, attachments );
	}

	public Crimbo11Request( final String action, final AdventureResult attachment )
	{
		super( Crimbo11Request.CRIMBO11, action, attachment );
	}

	public Crimbo11Request( final String action, final int itemId, final int quantity )
	{
		super( Crimbo11Request.CRIMBO11, action, itemId, quantity );
	}

	public Crimbo11Request( final String action, final int itemId )
	{
		super( Crimbo11Request.CRIMBO11, action, itemId );
	}

	private static String placeString( final String urlString )
	{
		String place = GenericRequest.getPlace( urlString );
		if ( place == null )
		{
			return null;
		}
		else if ( place.equals( "tradeincandy" ) )
		{
			return "Uncle Crimbo";
		}
		else if ( place.equals( "yourpresents" ) )
		{
			return "Your Presents";
		}
		else if ( place.equals( "buygifts" ) )
		{
			return "Crimbo Town Toy Factory";
		}
		return null;
	}

	public static String canBuy()
	{
		return null;
	}

	@Override
	public void run()
	{
		this.addFormField( "place", "tradeincandy" );
		super.run();
	}

	@Override
	public void processResults()
	{
		Crimbo11Request.parseResponse( this.getURLString(), this.responseText );
	}

	// <b>Results:</b></td></tr><tr><td style="padding: 5px; border: 1px solid blue;"><center><table><tr><td>Invalid gift selected.  Bah Humbug!</td></tr></table>
	private static final Pattern FAILURE_PATTERN = Pattern.compile( "<b>Results:</b>.*?<table><tr><td>(.*?)</td></tr></table>", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "crimbo11.php" ) )
		{
			return;
		}

		Crimbo11Request.parseCrimbo11Visit( urlString, responseText );
	}

	public static void parseCrimbo11Visit( final String location, final String responseText )
	{
		CoinmasterData data = Crimbo11Request.CRIMBO11;

		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			String place = GenericRequest.getPlace( location );
			if ( place != null && ( place.equals( "tradeincandy" ) || place.equals( "buygifts" ) ) )
			{
				// Parse current Candy Credits
				CoinMasterRequest.parseBalance( data, responseText );
			}
			return;
		}

		if ( action.equals( "reallybuygifts" ) )
		{
			// Good choice, quotid, good choice. My elves will make
			// sure, listen. My elves will make sure that present
			// goes where it's supposed to, okay? Now go trade some
			// more candy. We're dyin' over here.
			//
			// Don't worry, quotid, my elves will, listen. My elves
			// will stuff that stocking just right, okay? Now go
			// get some more, listen. Go get some more candy and
			// trade it in, okay?  else
			if ( responseText.indexOf( "My elves will make sure that present goes where it's supposed to" ) != -1 ||
			     responseText.indexOf( "My elves will stuff that stocking just right" ) != -1 )
			{
				CoinMasterRequest.completePurchase( data, location );
				CoinmastersFrame.externalUpdate();
			}
			// Your fingers are writing checks that your Crimbo
			// Credit Balance can't cash.
			else if ( responseText.indexOf( "Your fingers are writing checks" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't afford that" );
			}
			// You can't send yourself a present.
			else if ( responseText.indexOf( "You can't send yourself a present" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't send yourself a present" );
			}
			// The factory workers inform you that your intended
			// recipient already has one of those.
			else if ( responseText.indexOf( "already has one of those" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "That person already has that gift" );
			}
			// Invalid gift selected.  Bah Humbug!
			else if ( responseText.indexOf( "Invalid gift selected" ) != -1 )
			{
				Matcher itemMatcher = data.getItemMatcher( location );
				String itemId = itemMatcher.find() ?
					itemMatcher.group( 1 ) : "unknown";
				KoLmafia.updateDisplay( MafiaState.ERROR, "Item #" + itemId + " is not a valid gift" );
			}
			else
			{
				Matcher failureMatcher = Crimbo11Request.FAILURE_PATTERN.matcher( responseText );
				String message = failureMatcher.find() ?
					failureMatcher.group( 1 ) :
					"Unknown gifting failure";
				KoLmafia.updateDisplay( MafiaState.ERROR, message );
			}
		}
		else if ( action.equals( "tradecandy" ) )
		{
			// You don't have that much candy!
			if ( responseText.indexOf( "You don't have that much candy" ) == -1 )
			{
				CoinMasterRequest.completeSale( data, location );
				CoinmastersFrame.externalUpdate();
			}
		}
		else
		{
			// Some other action not associated with the cashier
			return;
		}

		// Parse current Candy Credits
		CoinMasterRequest.parseBalance( data, responseText );
	}

	public static final Pattern TOWHO_PATTERN = Pattern.compile( "towho=([^&]*)" );
	private static final boolean registerDonation( final String urlString )
	{
		CoinmasterData data = Crimbo11Request.CRIMBO11;

		Matcher itemMatcher = data.getItemMatcher( urlString );
		if ( !itemMatcher.find() )
		{
			return true;
		}
		String itemIdString = itemMatcher.group( 1 );
		int itemId = StringUtilities.parseInt( itemIdString );

		Matcher countMatcher = data.getCountMatcher( urlString );
		int count = countMatcher.find() ? StringUtilities.parseInt( countMatcher.group( 1 ) ) : 1;

		LockableListModel items = data.getBuyItems();
		AdventureResult item = AdventureResult.findItem( itemId, items );
		String name = item != null ? item.getName() :
			( "item #" + itemIdString );
		Map prices = data.getBuyPrices();
		int price = CoinmastersDatabase.getPrice( name, prices );
		int cost = count * price;

		String tokenName = ( cost != 1 ) ? data.getPluralToken() : data.getToken();
		String itemName = ( count != 1 ) ? ItemDatabase.getPluralName( itemId ) : name;

		Matcher victimMatcher = Crimbo11Request.TOWHO_PATTERN.matcher( urlString );
		String victim = victimMatcher.find() ? GenericRequest.decodeField( victimMatcher.group( 1 ).trim() ) : "0";
		if ( victim.equals( "" ) || victim.equals( "0" ) )
		{
			victim = "the Needy";
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "trading " + cost + " " + tokenName + " for " + count + " " + itemName + " for " + victim );
		return true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "crimbo11.php" ) )
		{
			return false;
		}

		String place = Crimbo11Request.placeString( urlString );
		String action = GenericRequest.getAction( urlString );
		if ( place != null && action == null )
		{
			String message = "Visiting " + place;
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
			return true;
		}
		else if ( action == null )
		{
			return true;
		}
		else if ( action.equals( "buygifts" ) )
		{
			// Transitional form leading to reallybuygifts
			return true;
		}

		CoinmasterData data = Crimbo11Request.CRIMBO11;
		if ( action.equals( data.getBuyAction() ) )
		{
			return Crimbo11Request.registerDonation( urlString );
		}

		return CoinMasterRequest.registerRequest( data, urlString );
	}

	public static String accessible()
	{
		return "Candy Credits are no longer exchangeable";
	}
}
