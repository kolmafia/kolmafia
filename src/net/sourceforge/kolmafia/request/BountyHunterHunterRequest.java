/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.swingui.AdventureFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BountyHunterHunterRequest
	extends CoinMasterRequest
{
	public static final String master = "Bounty Hunter Hunter"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( BountyHunterHunterRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( BountyHunterHunterRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "You have.*?<b>([\\d,]+)</b> filthy lucre" );
	public static final AdventureResult LUCRE = ItemPool.get( ItemPool.LUCRE, 1 );
	public static final CoinmasterData BHH =
		new CoinmasterData(
			BountyHunterHunterRequest.master,
			BountyHunterHunterRequest.class,
			"bhh.php",
			"lucre",
			"You don't have any filthy lucre",
			false,
			BountyHunterHunterRequest.TOKEN_PATTERN,
			BountyHunterHunterRequest.LUCRE,
			null,
			"whichitem",
			CoinMasterRequest.ITEMID_PATTERN,
			"howmany",
			CoinMasterRequest.HOWMANY_PATTERN,
			"buy",
			BountyHunterHunterRequest.buyItems,
			BountyHunterHunterRequest.buyPrices,
			null,
			null
			);

	public BountyHunterHunterRequest()
	{
		super( BountyHunterHunterRequest.BHH );
	}

	public BountyHunterHunterRequest( final String action )
	{
		super( BountyHunterHunterRequest.BHH, action );
	}

	public BountyHunterHunterRequest( final String action, final int itemId, final int quantity )
	{
		super( BountyHunterHunterRequest.BHH, action, itemId, quantity );
	}

	public BountyHunterHunterRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public BountyHunterHunterRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public void processResults()
	{
		BountyHunterHunterRequest.parseResponse( this.getURLString(), this.responseText );
	}

	private static final Pattern BOUNTY_PATTERN = Pattern.compile( "I'm still waiting for you to bring me (\\d+) (.*?), Bounty Hunter!" );
	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = BountyHunterHunterRequest.BHH;
		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			// I'm still waiting for you to bring me 5 discarded
			// pacifiers, Bounty Hunter!

			Matcher matcher = BountyHunterHunterRequest.BOUNTY_PATTERN.matcher( responseText );
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
				BountyHunterHunterRequest.abandonBounty();
			}
			return;
		}
		
		if ( action.equals( "abandonbounty" ) )
		{
			// Can't hack it, eh? Well, that's okay. I'm sure some other 
			// Bounty Hunter will step up. Maybe you should try something 
			// more appropriate to your Bounty Hunting skills.
			if ( responseText.contains( "Can't hack it, eh?" ) )
			{
				BountyHunterHunterRequest.abandonBounty();
			}
		}
		
		if ( action.equals( "takebounty" ) )
		{
			// All right, then!  Get out there and collect those empty aftershave bottles!
			if ( !responseText.contains( "All right, then!" ) )
			{
				return;
			}
			Matcher idMatcher = CoinMasterRequest.ITEMID_PATTERN.matcher( location );
			if ( !idMatcher.find() )
			{
				return;
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

		CoinMasterRequest.parseResponse( data, location, responseText );
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

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bhh.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting the Bounty Hunter Hunter" );
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
			AdventureResult bounty = AdventureDatabase.getBounty( itemId );
			String plural = ItemDatabase.getPluralName( itemId );
			
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "accept bounty assignment to collect " + bounty.getCount() + " " + plural );
			
			return true;
		}

		if ( action.equals( "abandonbounty" ) )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "abandon bounty assignment" );
			return true;
		}

		CoinmasterData data = BountyHunterHunterRequest.BHH;
		return CoinMasterRequest.registerRequest( data, urlString );
	}
}
