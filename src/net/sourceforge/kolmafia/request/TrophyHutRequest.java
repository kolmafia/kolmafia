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

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class TrophyHutRequest
	extends GenericRequest
{
	private static final Pattern WHICHTROPHY_PATTERN = Pattern.compile( "whichtrophy=(\\d*)" );

	public TrophyHutRequest()
	{
		super( "trophy.php" );
	}

	public void processResults()
	{
		TrophyHutRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "trophy.php" ) )
		{
			return;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null || !action.equals( "buytrophy" ) )
		{
			return;
		}

		// You can't afford to have a trophy installed.
		// Your trophy has been installed at your campsite.
		if ( responseText.indexOf( "Your trophy has been installed at your campsite" ) != -1 )
		{
			RequestLogger.updateSessionLog( "You spent 10,000 Meat" );
			ResultProcessor.processMeat( -10000 );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "trophy.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			// Don't log simple visits
			return true;
		}

		if ( !action.equals( "buytrophy" ) )
		{
			// Don't claim unknown actions
			return false;
		}

		Matcher matcher= TrophyHutRequest.WHICHTROPHY_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			// Missing trophy field
			return true;
		}

		String message = "Buying trophy #" + matcher.group(1) + " at the Trophy Hut";

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
