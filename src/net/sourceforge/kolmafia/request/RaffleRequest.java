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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RaffleRequest
	extends GenericRequest
{
	private static final Pattern WHERE_PATTERN = Pattern.compile( "where=(\\d+)" );
	private static final Pattern QUANTITY_PATTERN = Pattern.compile( "quantity=(\\d+)" );

	public static final int INVENTORY = 0;
	public static final int STORAGE = 1;

	private final int count;
	private final int source;

	public RaffleRequest( final int count, int source )
	{
		super( "raffle.php" );

		this.count = count;
		this.source = source;

		this.addFormField( "action", "buy" );
		this.addFormField( "where", String.valueOf( source ) );
		this.addFormField( "quantity", String.valueOf( count ) );
	}

	public RaffleRequest( final int count )
	{
		this( count, RaffleRequest.chooseMeatSource() );
	}

	private static int chooseMeatSource()
	{
		if ( KoLCharacter.isHardcore() || KoLCharacter.inRonin() )
		{
			return RaffleRequest.STORAGE;
		}

		return RaffleRequest.INVENTORY;
	}

	@Override
	public void run()
	{
		if ( this.source != RaffleRequest.INVENTORY && this.source != RaffleRequest.STORAGE )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Decide where to take meat from." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting the Raffle House..." );
		super.run();
	}

	@Override
	public void processResults()
	{
		String urlString = this.getURLString();
		String responseText = this.responseText;

		// You cannot afford that many tickets.
		if ( !RaffleRequest.parseResponse( urlString, responseText ) )
		{
			String where = ( this.source == RaffleRequest.INVENTORY ) ? "inventory" : "storage";
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have enough meat in " + where );
			return;
		}
	}

	public static final boolean parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "raffle.php" ) )
		{
			return true;
		}

		if ( responseText.indexOf( "You cannot afford" ) != -1 )
		{
			return false;
		}

		Matcher matcher = RaffleRequest.WHERE_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int where = StringUtilities.parseInt( matcher.group(1) );

		matcher = RaffleRequest.QUANTITY_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		int quantity = StringUtilities.parseInt( matcher.group(1) );
		int cost = 1000 * quantity;

		if ( where == RaffleRequest.STORAGE )
		{
			KoLCharacter.setStorageMeat( KoLCharacter.getStorageMeat() - cost );
		}
		else
		{
			ResultProcessor.processMeat( -cost );
		}

		AdventureResult item = new AdventureResult( ItemPool.RAFFLE_TICKET, quantity );
		ResultProcessor.processItem( false, "You acquire", item, null );

		return true;
	}

	public static final boolean registerRequest( final String location )
	{
		if ( !location.startsWith( "raffle.php" ) )
		{
			return false;
		}


		Matcher matcher = RaffleRequest.WHERE_PATTERN.matcher( location );
		if ( !matcher.find() )
		{
			return true;
		}

		int where = StringUtilities.parseInt( matcher.group(1) );
		String loc = where == RaffleRequest.INVENTORY ? "inventory" : where == RaffleRequest.STORAGE ? "storage" : "nowhere";

		matcher = RaffleRequest.QUANTITY_PATTERN.matcher( location );
		if ( !matcher.find() )
		{
			return true;
		}

		int quantity = StringUtilities.parseInt( matcher.group(1) );

		RequestLogger.updateSessionLog( "raffle " + quantity + " " + loc );

		return true;
	}
}
