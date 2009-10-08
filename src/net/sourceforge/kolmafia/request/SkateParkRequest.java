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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.Preferences;

public class SkateParkRequest
	extends GenericRequest
{
	public SkateParkRequest()
	{
		super( "sea_skatepark.php" );
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

		SkateParkRequest.ensureUpdatedSkatePark();
		if ( responseText.indexOf( "ocean/rumble" ) != -1 )
		{
			Preferences.setString( "skateParkStatus", "war" );
		}
		else if ( responseText.indexOf( "ocean/roller_territory" ) != -1 )
		{
			Preferences.setString( "skateParkStatus", "roller" );
		}
		else if ( responseText.indexOf( "ocean/ice_territory" ) != -1 )
		{
			Preferences.setString( "skateParkStatus", "ice" );
		}
		else if ( responseText.indexOf( "ocean/ocean_fountain" ) != -1 )
		{
			Preferences.setString( "skateParkStatus", "peace" );
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

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "sea_skatepark.php" ) )
		{
			return false;
		}

		return false;
	}
}
