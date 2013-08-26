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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Crimbo09Request
	extends GenericRequest
{
	public static final AdventureResult BUTTON = ItemPool.get( ItemPool.ELF_RESISTANCE_BUTTON, 1 );
	private static final Pattern CRIMBUX_PATTERN = Pattern.compile( "You currently have ([0123456789,]) Crimbu(x|ck)" );

	private static int bet = 0;

	public Crimbo09Request()
	{
		super( "crimbo09.php" );
	}

	@Override
	public void processResults()
	{
		Crimbo09Request.parseResponse( this.getURLString(), this.responseText );
	}

	public static final int getTurnsUsed( GenericRequest request )
	{
		String action = request.getFormField( "action" );
		if ( action != null )
		{
			if ( action.equals( "new11" ) )
			{
				return 1;
			}

			if ( !action.equals( "slotmachine" ) )
			{
				return 0;
			}

			String howmany = request.getFormField( "howmany" );
			return howmany == null ? 0 : StringUtilities.parseInt( howmany );
		}

		return 0;
	}

	private static final int getHowmany( final String urlString )
	{
		Matcher matcher = GenericRequest.HOWMANY_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group(1) ) : 0;
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "crimbo09.php" ) )
		{
			return;
		}

		String action = GenericRequest.getAction( location );
		if ( action == null || action.equals( "buygift" ) )
		{
			CrimboCartelRequest.parseResponse( location, responseText );
			return;
		}

		// Trade elf resistance buttons for Crimbux
		if ( action.equals( "tradearmbands" ) )
		{
			int count = BUTTON.getCount( KoLConstants.inventory );
			ResultProcessor.processItem( ItemPool.ELF_RESISTANCE_BUTTON, -count );
			return;
		}

		// Start a new 11 game
		if ( action.equals( "new11" ) )
		{
			// Deduct Crimbux from inventory
			ResultProcessor.processItem( ItemPool.CRIMBUCK, -bet );
			return;
		}

		// Double down
		if ( action.equals( "double11" ) )
		{
			// Double down - deduct an additional bet
			ResultProcessor.processItem( ItemPool.CRIMBUCK, -bet );
			return;
		}

		// Play the slot machine
		if ( action.equals( "slotmachine" ) )
		{
			// Deduct Crimbux from inventory
			ResultProcessor.processItem( ItemPool.CRIMBUCK, -bet );
			return;
		}
	}

	public static String locationName( final String urlString )
	{
		if ( urlString.indexOf( "place=don" ) != -1 )
		{
			return "Don Crimbo";
		}
		if ( urlString.indexOf( "place=store" ) != -1 )
		{
			return "the Crimbo Cartel";
		}
		return null;
	}

	private static String visitLocation( final String urlString )
	{
		String name = Crimbo09Request.locationName( urlString );
		if ( name != null )
		{
			return "Visiting " + name + " in Crimbo Town";
		}
		return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "crimbo09.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		// We want to log certain simple visits
		if ( action == null )
		{
			message = Crimbo09Request.visitLocation( urlString );
		}

		// Buy stuff in the Crimbo Cartel
		else if ( action.equals( "buygift" ) )
		{
			return CrimboCartelRequest.registerRequest( urlString );
		}

		// Trade elf resistance buttons for Crimbux
		else if ( action.equals( "tradearmbands" ) )
		{
			int count = BUTTON.getCount( KoLConstants.inventory );
			message = "Giving " + count + " elf resistance button" + ( count > 1 ? "s" : "" ) + " to Don Crimbo";
		}

		// Start a new 11 game
		else if ( action.equals( "new11" ) )
		{
			Crimbo09Request.bet = getHowmany( urlString );
			message = "[" + KoLAdventure.getAdventureCount() + "] Crimbo 11 Table" + KoLConstants.LINE_BREAK + "You bet " + Crimbo09Request.bet + " Crimbu" + ( Crimbo09Request.bet == 1 ? "ck" : "x" );
		}

		// Double down in a new 11 game
		else if ( action.equals( "double11" ) )
		{
			message = "You bet an additional " + Crimbo09Request.bet + " Crimbu" + ( Crimbo09Request.bet == 1 ? "ck" : "x" );
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			return true;
		}
		// Play the slot machine
		else if ( action.equals( "slotmachine" ) )
		{
			Crimbo09Request.bet = getHowmany( urlString );
			message = "[" + KoLAdventure.getAdventureCount() + "] Crimbo Casino Slot Machine";
		}

		// Buy Crimbux at the change machine
		else if ( action.equals( "buycrimbux" ) )
		{
			message = "Buying Crimbux at the Crimbo Casino Change Machine";
		}

		// Unknown action
		else
		{
			return false;
		}

		if ( message == null )
		{
			return true;
		}

		RequestLogger.printLine();
		RequestLogger.updateSessionLog();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
