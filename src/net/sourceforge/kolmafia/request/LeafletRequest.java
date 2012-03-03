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

import java.io.IOException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestLogger;

public class LeafletRequest
	extends GenericRequest
{
	private static final Pattern COMMAND_PATTERN = Pattern.compile( "command=([^&]*)" );
	private static final Pattern RESPONSE_PATTERN = Pattern.compile( "<td><b>(.*?)</b>" );
	private static final Pattern TCHOTCHKE_PATTERN = Pattern.compile( "A ([a-z ]*?) sits on the mantelpiece" );

	public LeafletRequest()
	{
		this(null);
	}

	public LeafletRequest( final String command )
	{
		super( "leaflet.php" );
		if ( command != null )
		{
			this.addFormField( "command", command );
		}
	}

	public void setCommand( final String command )
	{
		this.clearDataFields();
		this.addFormField( "command", command );
	}

	public void processResults()
	{
		LeafletRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		Matcher matcher = RESPONSE_PATTERN.matcher( responseText );
		if ( !matcher.find() )
		{
			return;
		}

                matcher = TCHOTCHKE_PATTERN.matcher( matcher.group(1) );
		if ( matcher.find() )
		{
			RequestLogger.updateSessionLog( "(You see a " + matcher.group(1) + ")" );
		}
	}

	private static final String getCommand( final String urlString )
	{
		Matcher matcher = COMMAND_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return null;
		}

		return GenericRequest.decodeURL( matcher.group( 1 ) );
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "leaflet.php" ) )
		{
			return false;
		}

		String command = LeafletRequest.getCommand( urlString );
		if ( command == null )
		{
			return true;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "Leaflet " + command );

		return true;
	}
}
