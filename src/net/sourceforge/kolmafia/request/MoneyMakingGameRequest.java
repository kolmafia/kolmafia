/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.MoneyMakingGameManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoneyMakingGameRequest
	extends GenericRequest
{
	public static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]*)" );
	public static final Pattern FROM_PATTERN = Pattern.compile( "from=(\\d*)" );
	public static final Pattern HOWMUCH_PATTERN = Pattern.compile( "howmuch=(\\d*)" );
	public static final Pattern WHICHBET_PATTERN = Pattern.compile( "whichbet=(\\d*)" );
	public static final Pattern BETID_PATTERN = Pattern.compile( "betid=(\\d*)" );

	public static final String getAction( final String urlString )
	{
		Matcher matcher = ACTION_PATTERN.matcher( urlString );
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

	public static final int getHowMuch( final String urlString )
	{
		return MoneyMakingGameRequest.parseInteger( HOWMUCH_PATTERN, urlString );
	}

	public static final int getWhichBet( final String urlString )
	{
		return MoneyMakingGameRequest.parseInteger( WHICHBET_PATTERN, urlString );
	}

	public static final int getBetId( final String urlString )
	{
		return MoneyMakingGameRequest.parseInteger( BETID_PATTERN, urlString );
	}

	private static final int VISIT = 1;
	private static final int MAKE_BET = 2;
	private static final int RETRACT_BET = 3;
	private static final int TAKE_BET = 4;

	public static final int INVENTORY = 0;
	public static final int STORAGE = 1;

	private final int type;

	public MoneyMakingGameRequest()
	{
		super( "bet.php" );
		this.type = MoneyMakingGameRequest.VISIT;
	}

	public MoneyMakingGameRequest( final int amount, final boolean storage )
	{
		super( "bet.php" );
		this.addFormField( "action", "makebet" );
		this.addFormField( "from", storage ? "1" : "0" );
		this.addFormField( "howmuch", String.valueOf( amount ) );
		this.type = MoneyMakingGameRequest.MAKE_BET;
	}

	public MoneyMakingGameRequest( final int betid )
	{
		super( "bet.php" );
		this.addFormField( "action", "retract" );
		this.addFormField( "betid", String.valueOf( betid ) );
		this.type = MoneyMakingGameRequest.RETRACT_BET;
	}

	public MoneyMakingGameRequest( final int betid, final boolean storage, final boolean dummy )
	{
		super( "bet.php" );
		this.addFormField( "action", "bet" );
		this.addFormField( "whichbet", String.valueOf( betid ) );
		this.addFormField( "from", storage ? "1" : "0" );
		this.addFormField( "confirm", "on" );
		this.type = MoneyMakingGameRequest.TAKE_BET;
	}

	protected boolean shouldFollowRedirect()
	{
		return true;
	}

	public void run()
	{
		super.run();
	}

	public void processResults()
	{
                String responseText = this.responseText;
		MoneyMakingGameRequest.parseResponse( this.getURLString(), responseText );
		switch ( this.type )
		{
		case MoneyMakingGameRequest.VISIT:
			break;
		case MoneyMakingGameRequest.MAKE_BET:
			// You open your wallet and proudly pull out Meat. The
			// old man gives you a funny look, and you start to
			// sweat. After rifling through your pockets for a few
			// awkward moments, you realize that you just don't
			// have enough data. I mean, Meat. To make that wager.
			//
			// After a wait that seems to take forever, the old man
			// turns to you and says, "Sorry, kid, but Hagnk's
			// secretary says that you don't have enough Meat to
			// make that big of a wager. Care to try again?"
			if ( responseText.indexOf( "don't have enough" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have enough meat." );
			}
			break;
		case MoneyMakingGameRequest.RETRACT_BET:
			// You don't have a bet with that ID. Likely, someone
			// already took it.
			if ( responseText.indexOf( "don't have a bet with that ID" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Could not retract bet." );
			}
			break;
		case MoneyMakingGameRequest.TAKE_BET:
			// The old man looks at you quizzically. &quot;There's
			// no bet like that anywhere in our records. Maybe
			// someone else got to it before you could.&quot;
			if ( this.responseText.indexOf( "no bet like that" ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Could not take bet." );
			}
			break;
		}
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "bet.php" ) )
		{
			return;
		}

		// Parse offered bets from responseText
		MoneyMakingGameManager.parseOfferedBets( responseText );

		// Parse my bets from responseText
		MoneyMakingGameManager.parseMyBets( responseText );

		// When you make a bet, you are redirected from the URL you
		// submitted to make it to bet.php
		if ( responseText.indexOf( "You make a bet." ) != -1 )
		{
			MoneyMakingGameManager.makeBet( responseText );
			return;
		}

		String action = getAction( urlString );

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

		String action = getAction( urlString );

		// We have nothing special to do for simple visits.

		if ( action == null )
		{
			return true;
		}

		String message = null;

		if ( action.equals( "makebet" ) )
		{
			String from = getFromString( urlString );
			int howmuch = getHowMuch( urlString );
			if ( from == null || howmuch < 0 )
			{
				return true;
			}

			message = "Betting " + howmuch + " meat from " + from;
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
			// bet.php?action=bet&whichbet=58251231&from=0&confirm=on&pwd
			String from = getFromString( urlString );
			int whichbet = getWhichBet( urlString );
			if ( from == null || whichbet < 0 )
			{
				return true;
			}

			message = "Taking bet " + whichbet + " using meat from " + from;
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
