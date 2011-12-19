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

import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;

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
			CoinMasterRequest.ITEMID_PATTERN,
			"howmany",
			CoinMasterRequest.HOWMANY_PATTERN,
			"reallybuygifts",
			Crimbo11Request.buyItems,
			Crimbo11Request.buyPrices,
			"tradecandy",
			Crimbo11Request.sellPrices
			);

	public Crimbo11Request()
	{
		super( Crimbo11Request.CRIMBO11 );

		// Visit Uncle Crimbo to get Candy Credit balance
		this.addFormField( "place", "tradeincandy" );
	}

	public Crimbo11Request( final String action )
	{
		super( Crimbo11Request.CRIMBO11, action );
	}

	public Crimbo11Request( final String action, final int itemId, final int quantity )
	{
		super( Crimbo11Request.CRIMBO11, action, itemId, quantity );
	}

	public Crimbo11Request( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public Crimbo11Request( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
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

	public void processResults()
	{
		Crimbo11Request.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=whichitem value=([\\d]+)>.*?descitem.([\\d]+).*?<b>([^<&]*)(?:&nbsp;)*</td>.*?<b>([\\d,]+) credit</b>", Pattern.DOTALL );

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
			if ( place.equals( "tradeincandy" ) || place.equals( "buygifts" ) )
			{
				// Parse current Candy Credits
				CoinMasterRequest.parseBalance( data, responseText );
			}
			return;
		}

		if ( action.equals( "reallybuygifts" ) )
		{
			// Your fingers are writing checks that your Crimbo
			// Credit Balance can't cash.
			if ( responseText.indexOf( "Your fingers are writing checks" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't afford that" );
			}
			// You can't send yourself a present.
			else if ( responseText.indexOf( "You can't send yourself a present" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't send yourself a present" );
			}
			else 
			{
				CoinMasterRequest.completePurchase( data, location );
				CoinmastersFrame.externalUpdate();
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

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "crimbo11.php" ) )
		{
			return false;
		}

		String place = Crimbo11Request.placeString( urlString );
		if ( place != null && urlString.indexOf( "action" ) == -1 )
		{
			String message = "Visiting " + place;
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
			return true;
		}

		CoinmasterData data = Crimbo11Request.CRIMBO11;
		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
