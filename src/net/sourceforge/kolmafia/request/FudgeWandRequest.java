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
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.utilities.IntegerCache;


public class FudgeWandRequest
	extends CoinMasterRequest
{
	public static final String master = "Fudge Wand"; 
	private static final LockableListModel buyItems = CoinmastersDatabase.getBuyItems( FudgeWandRequest.master );
	private static final Map buyPrices = CoinmastersDatabase.getBuyPrices( FudgeWandRequest.master );

	private static final Pattern TOKEN_PATTERN = Pattern.compile( "(?:You've.*?got|You.*? have) (?:<b>)?([\\d,]+)(?:</b>)? fudgecule" );
	public static final AdventureResult FUDGECULE = ItemPool.get( ItemPool.FUDGECULE, 1 );
	public static final AdventureResult FUDGE_WAND = ItemPool.get( ItemPool.FUDGE_WAND, 1 );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "option=(\\d+)" );
	public static final CoinmasterData FUDGEWAND =
		new CoinmasterData(
			FudgeWandRequest.master,
			FudgeWandRequest.class,
			"choice.php?whichchoice=562",
			"fudgecule",
			null,
			false,
			FudgeWandRequest.TOKEN_PATTERN,
			FudgeWandRequest.FUDGECULE,
			null,
			null,
			null,
			null,
			null,
			null,
			FudgeWandRequest.buyItems,
			FudgeWandRequest.buyPrices,
			null,
			null
			);

	private static String lastURL = null;

	private static final Object[][] OPTIONS =
	{
		{ "1", IntegerCache.valueOf( ItemPool.FUDGIE_ROLL ) },
		{ "2", IntegerCache.valueOf( ItemPool.FUDGE_SPORK ) },
		{ "3", IntegerCache.valueOf( ItemPool.FUDGE_CUBE ) },
		{ "4", IntegerCache.valueOf( ItemPool.FUDGE_BUNNY ) },
		{ "5", IntegerCache.valueOf( ItemPool.FUDGECYCLE ) },
	};

	private static String idToOption( final int id )
	{
		for ( int i = 0; i < OPTIONS.length; ++i )
		{
			Object [] option = OPTIONS[ i ];
			if ( ( (Integer) option[ 1 ] ).intValue() == id )
			{
				return (String) option[ 0 ];
			}
		}

		return null;
	}

	private static int optionToId( final String opt )
	{
		for ( int i = 0; i < OPTIONS.length; ++i )
		{
			Object [] option = OPTIONS[ i ];
			if ( opt.equals( (String) option[ 0 ] ) )
			{
				return ( (Integer)option[ 1 ] ).intValue();
			}
		}

		return -1;
	}

	public FudgeWandRequest()
	{
		super( FudgeWandRequest.FUDGEWAND );
	}

	public FudgeWandRequest( final String action )
	{
		super( FudgeWandRequest.FUDGEWAND, action );
	}

	public FudgeWandRequest( final String action, final int itemId, final int quantity )
	{
		super( FudgeWandRequest.FUDGEWAND, action, itemId, quantity );
		String option = FudgeWandRequest.idToOption( itemId );
		this.addFormField( "option", option );
	}

	public FudgeWandRequest( final String action, final int itemId )
	{
		this( action, itemId, 1 );
	}

	public FudgeWandRequest( final String action, final AdventureResult ar )
	{
		this( action, ar.getItemId(), ar.getCount() );
	}

	public void processResults()
	{
		FudgeWandRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static void parseResponse( final String location, final String responseText )
	{
		CoinmasterData data = FudgeWandRequest.FUDGEWAND;

		if ( location.indexOf( "option=6" ) != -1 )
		{
			// We exited the choice adventure.
			return;
		}

		Matcher optionMatcher = FudgeWandRequest.OPTION_PATTERN.matcher( location );
		if ( !optionMatcher.find() )
		{
			return;
		}

		String option = optionMatcher.group( 1 );
		int itemId = FudgeWandRequest.optionToId( option );

		if ( itemId != -1 )
		{
			CoinMasterRequest.completePurchase( data, itemId, 1, false );
		}

		CoinMasterRequest.parseBalance( data, responseText );
		ConcoctionDatabase.setRefreshNeeded( true );
	}

	public static final boolean registerRequest( final String urlString )
	{
		CoinmasterData data = FudgeWandRequest.FUDGEWAND;

		// using the wand is simply a visit
		// inv_use.php?whichitem=5441
		if ( urlString.startsWith( "inv_use.php" ) && urlString.indexOf( "whichitem=5441" ) != -1 )
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "Visiting " + data.getMaster() );
			return true;
		}

		// choice.php?pwd&whichchoice=562&option=3
		if ( !urlString.startsWith( "choice.php" ) || urlString.indexOf( "whichchoice=562" ) == -1 )
		{
			return false;
		}

		if ( urlString.indexOf( "option=6" ) != -1 )
		{
			// We exited the choice adventure.
			return true;
		}

		// Save URL.
		FudgeWandRequest.lastURL = urlString;


		Matcher optionMatcher = FudgeWandRequest.OPTION_PATTERN.matcher( urlString );
		if ( !optionMatcher.find() )
		{
			return true;
		}

		String option = optionMatcher.group( 1 );
		int itemId = FudgeWandRequest.optionToId( option );

		if ( itemId != -1 )
		{
			CoinMasterRequest.buyStuff( data, itemId, 1, false );
		}

		return true;
	}

	public static String accessible()
	{
		int wand = FudgeWandRequest.FUDGE_WAND.getCount( KoLConstants.inventory );
		if ( wand == 0 )
		{
			return "You don't have a wand of fudge control";
		}

		int fugecules = FudgeWandRequest.FUDGECULE.getCount( KoLConstants.inventory );
		if ( fugecules == 0 )
		{
			return "You don't have any fudgecules";
		}
		return null;
	}

	public void equip()
	{
		// Use the wand of fudge control
		RequestThread.postRequest( new GenericRequest( "inv_use.php?whichitem=5441" ) );
	}

	public void unequip()
	{
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=562&option=6" ) );
	}
}
