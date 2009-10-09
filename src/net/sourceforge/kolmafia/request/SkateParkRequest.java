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
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.RequestLogger;

public class SkateParkRequest
	extends GenericRequest
{
	public static final Pattern ACTION_PATTERN = Pattern.compile( "action=([^&]*)" );

	public SkateParkRequest()
	{
		super( "sea_skatepark.php" );
	}

	public SkateParkRequest( final String action)
	{
		this();
		this.addFormField( "action", action );
	}

	public void processResults()
	{
		SkateParkRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "sea_skatepark.php" ) )
		{
			return;
		}

		Matcher matcher = ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// Deduce the state of war
		String status = null;

		if ( responseText.indexOf( "ocean/rumble" ) != -1 )
		{
			status = "war";
		}
		else if ( responseText.indexOf( "ocean/roller_territory" ) != -1 )
		{
			status = "roller";
		}
		else if ( responseText.indexOf( "ocean/ice_territory" ) != -1 )
		{
			status = "ice";
		}
		else if ( responseText.indexOf( "ocean/fountain" ) != -1 )
		{
			status = "peace";
		}

		if ( status != null )
		{
			SkateParkRequest.ensureUpdatedSkatePark();
			Preferences.setString( "skateParkStatus", status );
		}
	}

	private static final void ensureUpdatedSkatePark()
	{
		int lastAscension = Preferences.getInteger( "lastSkateParkReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastSkateParkReset", KoLCharacter.getAscensions() );
			Preferences.setString( "skateParkStatus", "war" );
		}
	}

	private static final String actionToPlace( final String action )
	{
                if ( action.equals( "state4buff1" ) )
                {
                        return "the Band Shell";
                }
                if ( action.equals( "state4buff2" ) )
                {
                        return "the Eclectic Eels";
                }
                if ( action.equals( "state4buff3" ) )
                {
                        return "the Merry-Go-Round";
                }
                return null;
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "sea_skatepark.php" ) )
		{
			return false;
		}

		Matcher matcher = ACTION_PATTERN.matcher( urlString );
		String action = matcher.find() ? matcher.group(1) : null;

		// We have nothing special to do for simple visits.

		if ( action == null )
		{
			return true;
		}

		String place = SkateParkRequest.actionToPlace( action );

		if ( place == null )
		{
			return false;
		}

		String message = "Visiting " + place;

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
