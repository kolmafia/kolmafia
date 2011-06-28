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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.request.CoinMasterRequest;
import net.sourceforge.kolmafia.swingui.CoinmastersFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AWOLQuartermasterRequest
	extends CoinMasterRequest
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? A. W. O. L. commendation" );
	public static final AdventureResult COMMENDATION = ItemPool.get( ItemPool.AWOL_COMMENDATION, 1 );
	private static final Pattern TOBUY_PATTERN = Pattern.compile( "tobuy=(\\d+)" );
	public static final CoinmasterData AWOL =
		new CoinmasterData(
			"A. W. O. L. Quartermaster",
			AWOLQuartermasterRequest.class,
			"inv_use.php?whichitem=5116&ajax=1",
			"commendation",
			null,
			false,
			AWOLQuartermasterRequest.TOKEN_PATTERN,
			AWOLQuartermasterRequest.COMMENDATION,
			null,
			"tobuy",
			AWOLQuartermasterRequest.TOBUY_PATTERN,
			"howmany",
			CoinMasterRequest.HOWMANY_PATTERN,
			null,
			CoinmastersDatabase.getCommendationItems(),
			CoinmastersDatabase.commendationBuyPrices(),
			null,
			null
			);

	private static String lastURL = null;

	public AWOLQuartermasterRequest()
	{
		super( AWOLQuartermasterRequest.AWOL );
	}

	public AWOLQuartermasterRequest( final String action )
	{
		super( AWOLQuartermasterRequest.AWOL, action );
	}

	public AWOLQuartermasterRequest( final String action, final int itemId, final int quantity )
	{
		super( AWOLQuartermasterRequest.AWOL, action, itemId, quantity );
		this.addFormField( "doit", "69" );
	}

	public AWOLQuartermasterRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public AWOLQuartermasterRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	private static final Pattern TATTOO_PATTERN = Pattern.compile( "sigils/aol(\\d+).gif" );
	public static void parseAWOLVisit( final String location, final String responseText )
	{
		CoinmasterData data = AWOLQuartermasterRequest.AWOL;

		// If you don't have enough commendations, you are redirected to inventory.php
		if ( responseText.indexOf( "You don't have enough commendations" ) == -1 )
		{
			// inv_use.php?whichitem=5116&pwd&doit=69&tobuy=xxx&howmany=yyy
			CoinMasterRequest.completePurchase( data, location );
		}

		// Check which tattoo - if any - is for sale: sigils/aol3.gif
		Matcher m = TATTOO_PATTERN.matcher( responseText );
		KoLCharacter.AWOLtattoo = m.find() ? StringUtilities.parseInt( m.group( 1 ) ) : 0;

		CoinMasterRequest.parseBalance( data, responseText );
		CoinmastersFrame.externalUpdate();
	}

	public static final boolean registerRequest( final String urlString )
	{
		// inv_use.php?whichitem=5116&pwd&doit=69&tobuy=xxx&howmany=yyy
		if ( urlString.indexOf( "whichitem=5116" ) == -1 )
		{
			return false;
		}

		// Save URL. If request fails, we are redirected to inventory.php
		AWOLQuartermasterRequest.lastURL = urlString;

		if ( urlString.indexOf( "doit=69" ) != -1 && urlString.indexOf( "tobuy" ) != -1	 )
		{
			CoinmasterData data = AWOLQuartermasterRequest.AWOL;
			CoinMasterRequest.buyStuff( data, urlString );
			return true;
		}

		return true;
	}

	public static String accessible()
	{
		int commendations = AWOLQuartermasterRequest.COMMENDATION.getCount( KoLConstants.inventory );
		if ( commendations == 0 )
		{
			return "You don't have any A. W. O. L. commendations";
		}
		return null;
	}
}
