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

import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.chat.ChatManager;

public class AltarOfLiteracyRequest
	extends GenericRequest
{
	public AltarOfLiteracyRequest()
	{
		super( "town_altar.php" );
	}

	@Override
	public void processResults()
	{
		AltarOfLiteracyRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "town_altar.php" ) )
		{
			return;
		}

		// Congratulations! You have demonstrated the ability to both
		// read and write! You have been granted access to the Kingdom
		// of Loathing chat.

		if ( responseText.indexOf( "You have been granted access" ) != -1 )
		{
			String message = "You have proven yourself literate.";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			ChatManager.setChatLiteracy( true );
			return;
		}

		// You have already proven yourself literate!
		if ( responseText.indexOf( "You have already proven yourself literate" ) != -1 )
		{
			ChatManager.setChatLiteracy( true );
			return;
		}

		// At this time, you are not allowed to enter the chat.
		if ( responseText.indexOf( "you are not allowed to enter the chat" ) != -1 )
		{
			ChatManager.setChatLiteracy( false );
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "town_altar.php" ) )
		{
			return false;
		}

		String message = null;

		if ( urlString.equals( "town_altar.php" ) )
		{
			message = "Visiting the Altar of Literacy";
		}

		if ( message != null )
		{
			RequestLogger.printLine();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}
}
