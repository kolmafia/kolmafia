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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

public class GrandpaRequest
	extends GenericRequest
{
	private static final Pattern WHO_PATTERN = Pattern.compile( "who=(\\d*)" );
	private static final Pattern QUERY_PATTERN = Pattern.compile( "topic=([^&]*)" );

	public GrandpaRequest()
	{
		this(null);
	}

	public GrandpaRequest( final String story)
	{
		super( "monkeycastle.php" );
		this.addFormField( "action", "grandpastory" );
		if ( story != null )
		{
			this.addFormField( "topic", story );
		}
	}

	public void processResults()
	{
		// You can't visit the Sea Monkees without some way of
		// breathing underwater.

		if ( this.responseText.indexOf( "can't visit the Sea Monkees" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You're not equipped to visit the Sea Monkees." );
		}
	}

	public static final String findNPC( final int npc )
	{
		switch ( npc )
		{
		case 1:
			return "Little Brother";
		case 2:
			return "Big Brother";
		case 3:
			return "Grandpa";
		case 4:
			return "Grandma";
		}

		return "Unknown Sea Monkey";
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "monkeycastle.php" ) )
		{
			return false;
		}

		Matcher matcher = WHO_PATTERN.matcher( urlString );
		String action = GenericRequest.getAction( urlString );
		if ( matcher.find() && action == null )
		{
			// Simple visit with no action
			String NPC = GrandpaRequest.findNPC( Integer.parseInt( matcher.group(1) ) );
			RequestLogger.updateSessionLog( "Visiting " + NPC );
			return true;
		}

		if ( action == null )
		{
			return false;
		}

		if ( !action.equals( "grandpastory" ) )
		{
			return false;
		}

		matcher = QUERY_PATTERN.matcher( urlString );
		if ( !matcher.find() )
		{
			return true;
		}

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "grandpa " + GenericRequest.decodeURL( matcher.group( 1 ) ) );

		return true;
	}
}
