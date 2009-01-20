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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class SuspiciousGuyRequest
	extends GenericRequest
{
	private static final Pattern GOOFBALL_PATTERN = Pattern.compile( "Buy some goofballs \\((\\d+),000 Meat\\)" );

	public SuspiciousGuyRequest( final int itemId )
	{
		super( "town_wrong.php" );
		switch (itemId )
		{
		case ItemPool.GOOFBALLS:
			this.addFormField( "action", "buygoofballs" );
			break;
		case ItemPool.OILY_GOLDEN_MUSHROOM:
			this.addFormField( "sleazy", "1" );
			break;
		default:
			this.addFormField( "place", "goofballs" );

			break;
		}
	}

	public void processResults()
	{
		SuspiciousGuyRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final boolean parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "town_wrong.php" ) )
		{
			return false;
		}

		if ( location.indexOf( "place=goofballs" ) == -1 && location.indexOf( "action=buygoofballs" ) == -1 && location.indexOf( "sleazy=1" ) == -1 )
		{
			return false;
		}

		if ( location.indexOf( "action=buygoofballs" ) != -1 )
		{
			// Here you go, man. If you get caught, you didn't get
			// these from me, man.

			if ( responseText.indexOf( "If you get caught" ) == -1 )
			{
				return true;
			}

			Matcher matcher = GOOFBALL_PATTERN.matcher( responseText );
			if ( !matcher.find() )
			{
				return true;
			}

			int cost = 1000 * Integer.parseInt( matcher.group( 1 ) ) - 1000;
			if ( cost > 0 )
			{
				ResultProcessor.processMeat( -cost );
			}

			return true;
		}

		if ( location.indexOf( "sleazy=1" ) != -1 )
		{
			// The suspicious-looking guy takes your gloomy black
			// mushroom and smiles that unsettling little smile
			// that makes you nervous. "Sweet, man. Here ya go."

			if ( responseText.indexOf ("takes your gloomy black mushroom" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.GLOOMY_BLACK_MUSHROOM, -1 );
			}

			return true;
		}

		return true;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "town_wrong.php" ) )
		{
			return false;
		}

		String message;
		if ( urlString.indexOf( "action=buygoofballs" ) != -1 )
		{
			message = "Buying goofballs from the suspicious looking guy";
		}
		else if ( urlString.indexOf( "sleazy=1" ) != -1 )
		{
			message = "Trading a gloomy black mushroom for an oily golden mushroom";
		}
		else if ( urlString.indexOf( "place=goofballs" ) != -1 )
		{
			RequestLogger.printLine( "" );
			RequestLogger.updateSessionLog();
			message = "Visiting the suspicious looking guy";
		}
		else
		{
			return false;
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
