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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.MoneyMakingGameManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.MoneyMakingGameDecorator;

public class MoneyMakingGameRequest
	extends GenericRequest
{
	public static final Pattern FROM_PATTERN = Pattern.compile( "from=(\\d*)" );
	public static final Pattern WHICHBET_PATTERN = Pattern.compile( "whichbet=(\\d*)" );
	public static final Pattern BETID_PATTERN = Pattern.compile( "betid=(\\d*)" );
	public static final Pattern LOWER_PATTERN = Pattern.compile( "lower=(\\d*)" );
	public static final Pattern HIGHER_PATTERN = Pattern.compile( "higher=(\\d*)" );

	public static final String getLower( final String urlString )
	{
		Matcher matcher = LOWER_PATTERN.matcher( urlString );
		return matcher.find() ? matcher.group(1) : null;
	}

	public static final String getHigher( final String urlString )
	{
		Matcher matcher = HIGHER_PATTERN.matcher( urlString );
		return matcher.find() ? matcher.group(1) : null;
	}

	public static final int parseInteger( final Pattern pattern, final String urlString )
	{
		Matcher matcher = pattern.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group(1) ) : -1;
	}

	public static final int getFrom( final String urlString )
	{
		return MoneyMakingGameRequest.parseInteger( FROM_PATTERN, urlString );
	}

	public static final String getFromString( final String urlString )
	{
		int from = getFrom( urlString );
		return from == 0 ? "inventory" : from == 1 ? "storage" : null;
	}

	public static final int getWhichBet( final String urlString )
	{
		return MoneyMakingGameRequest.parseInteger( WHICHBET_PATTERN, urlString );
	}

	public static final int getBetId( final String urlString )
	{
		return MoneyMakingGameRequest.parseInteger( BETID_PATTERN, urlString );
	}

	public static final int VISIT = 1;
	public static final int SEARCH = 2;
	public static final int MAKE_BET = 3;
	public static final int RETRACT_BET = 4;
	public static final int TAKE_BET = 5;

	public static final int INVENTORY = 0;
	public static final int STORAGE = 1;

	private final int action;

	public MoneyMakingGameRequest()
	{
		super( "bet.php" );
		this.action = MoneyMakingGameRequest.VISIT;
	}

	public MoneyMakingGameRequest( final int action, final int arg1 )
	{
		super( "bet.php" );
		this.action = action;
		switch ( action )
		{
		case MoneyMakingGameRequest.VISIT:
		case MoneyMakingGameRequest.SEARCH:
		case MoneyMakingGameRequest.MAKE_BET:
		case MoneyMakingGameRequest.TAKE_BET:
			// These don't make sense
			break;
		case MoneyMakingGameRequest.RETRACT_BET:
			this.addFormField( "action", "retract" );
			this.addFormField( "betid", String.valueOf( arg1 ) );
			break;
		}
	}

	public MoneyMakingGameRequest( final int action, final int arg1, final int arg2 )
	{
		super( "bet.php" );
		this.action = action;
		switch ( action )
		{
		case MoneyMakingGameRequest.VISIT:
		case MoneyMakingGameRequest.RETRACT_BET:
			// These don't make sense
			break;
		case MoneyMakingGameRequest.SEARCH:
			this.addFormField( "action", "search" );
			this.addFormField( "lower", String.valueOf( arg1 ) );
			this.addFormField( "higher", String.valueOf( arg2 ) );
			break;
		case MoneyMakingGameRequest.MAKE_BET:
			this.addFormField( "action", "makebet" );
			this.addFormField( "howmuch", String.valueOf( arg1 ) );
			this.addFormField( "from", String.valueOf( arg2 ) );
			break;
		case MoneyMakingGameRequest.TAKE_BET:
			this.addFormField( "action", "bet" );
			this.addFormField( "whichbet", String.valueOf( arg1 ) );
			this.addFormField( "from", String.valueOf( arg2 ) );
			this.addFormField( "confirm", "on" );
			break;
		}
	}

	@Override
	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	@Override
	public void processResults()
	{
		String responseText = this.responseText;

		if ( responseText.indexOf( "You can't gamble without a casino pass." ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a casino pass." );
			return;
		}

		MoneyMakingGameRequest.parseResponse( this.getURLString(), responseText, true );

		String error = null;
		switch ( this.action )
		{
		case MoneyMakingGameRequest.VISIT:
		case MoneyMakingGameRequest.SEARCH:
			break;
		case MoneyMakingGameRequest.MAKE_BET:
			error = MoneyMakingGameRequest.makeBetErrorMessage( responseText );
			break;
		case MoneyMakingGameRequest.RETRACT_BET:
			error = MoneyMakingGameRequest.retractBetErrorMessage( responseText );
			break;
		case MoneyMakingGameRequest.TAKE_BET:
			error = MoneyMakingGameRequest.takeBetErrorMessage( responseText );
			break;
		}

		if ( error != null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, error );
		}
	}

	private static String makeBetErrorMessage( final String responseText )
	{
		// You can't make a wager for less than 1,000 Meat. Sad, but
		// true.
		if ( responseText.indexOf( "can't make a wager" ) != -1 )
		{
			return "You must bet at least 1,000 meat.";
		}

		// You open your wallet and proudly pull out Meat. The old man
		// gives you a funny look, and you start to sweat. After
		// rifling through your pockets for a few awkward moments, you
		// realize that you just don't have enough data. I mean,
		// Meat. To make that wager.
		//
		// After a wait that seems to take forever, the old man turns
		// to you and says, "Sorry, kid, but Hagnk's secretary says
		// that you don't have enough Meat to make that big of a
		// wager. Care to try again?"

		if ( responseText.indexOf( "don't have enough" ) != -1 )
		{
			return "You don't have enough meat.";
		}

		// You can't have more than five bets running at one
		// time. Strange, but true.
		if ( responseText.indexOf( "Strange, but true" ) != -1 )
		{
			return "You can only have five bets at a time.";
		}

		return null;
	}

	private static String retractBetErrorMessage( final String responseText )
	{
		// You don't have a bet with that ID. Likely, someone already
		// took it.
		if ( responseText.indexOf( "don't have a bet with that ID" ) != -1 )
		{
			return "Could not retract bet.";
		}

		return null;
	}

	private static String takeBetErrorMessage( final String responseText )
	{
		// The old man looks at you quizzically. &quot;There's no bet
		// like that anywhere in our records. Maybe someone else got to
		// it before you could.&quot;
		if ( responseText.indexOf( "no bet like that" ) != -1 )
		{
			return "Could not take bet.";
		}

		// You rifle through your wallet, but can't seem to fish out
		// enough Meat to afford the wager. You'll have to try
		// something cheaper.
		//
		// &quot;Sorry, kid,&quot; the old man says to
		// you. &quot;Hagnk's secretary says that you don't have enough
		// to take that bet.&quot;

		if ( responseText.indexOf( "fish out enough" ) != -1 ||
		     responseText.indexOf( "don't have enough" ) != -1)
		{
			return "You don't have enough meat.";
		}

		return null;
	}

	public static final void parseResponse( final String urlString, final String responseText, final boolean internal )
	{
		if ( !urlString.startsWith( "bet.php" ) )
		{
			return;
		}

		// In registerRequest, we saved the amount we were betting.
		// Detect if our bet was rejected outright and forget the
		// pending bet, if so.

		if ( MoneyMakingGameManager.makingBet != 0 &&
		     MoneyMakingGameRequest.makeBetErrorMessage( responseText ) != null )
		{
			MoneyMakingGameManager.makingBet = 0;
		}

		// Parse my bets from responseText
		MoneyMakingGameManager.parseMyBets( responseText, internal );

		// Parse offered bets from responseText
		MoneyMakingGameManager.parseOfferedBets( responseText );

		// When you make a bet, you are redirected from the URL you
		// submitted to make it to bet.php
		if ( responseText.indexOf( "You make a bet." ) != -1 )
		{
			MoneyMakingGameManager.makeBet( responseText );
			MoneyMakingGameManager.makingBet = 0;
			return;
		}

		String action = GenericRequest.getAction( urlString );

		// We have nothing else to do for simple visits.
		if ( action == null )
		{
			return;
		}

		if ( action.equals( "retract" ) )
		{
			MoneyMakingGameManager.retractBet( urlString, responseText );
			return;
		}

		if ( action.equals( "bet" ) )
		{
			MoneyMakingGameManager.takeBet( urlString, responseText );
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "bet.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );

		// We have nothing special to do for simple visits.

		if ( action == null )
		{
			return true;
		}

		String message = null;

		if ( action.equals( "makebet" ) )
		{
			String from = getFromString( urlString );
			int howmuch = GenericRequest.getHowMuch( urlString );
			if ( from == null || howmuch < 0 )
			{
				return true;
			}

			// Remember that we're making a bet
			MoneyMakingGameManager.makingBet = howmuch;

			message = "Betting " + KoLConstants.COMMA_FORMAT.format( howmuch ) + " meat from " + from;
		}
		else if ( action.equals( "retract" ) )
		{
			// bet.php?action=retract&betid=58251236&pwd
			int betid = getBetId( urlString );
			if ( betid < 0 )
			{
				return true;
			}

			message = "Retracting bet " + betid;
		}
		else if ( action.equals( "bet" ) )
		{
			// Log when the bet is resolved
			return true;
		}
		else if ( action.equals( "search" ) )
		{
			String minimum = getLower( urlString );
			String maximum = getHigher( urlString );
			MoneyMakingGameDecorator.setLimits( minimum, maximum );
			return true;
		}
		else
		{
			return true;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
