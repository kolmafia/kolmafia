/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import net.sourceforge.kolmafia.chat.ChatManager;

public class ChatRequest
	extends GenericRequest
{
	private final String graf;

	/**
	 * Constructs a new <code>ChatRequest</code> where the given parameter
	 * will be passed to the PHP file to indicate where you left off.
	 *
	 * @param lastseen   The timestamp of the last message received
	 * @param tabbedChat true if "modern" chat, false if "older" chat
	 */

	public ChatRequest( final long lastSeen, final boolean tabbedChat, final boolean afk )
	{
		super( "", false );

		// Construct a URL to submit via GET, just like the browser
		StringBuilder newURLString = new StringBuilder( "newchatmessages.php?" );

		if ( tabbedChat )
		{
			newURLString.append( "j=1&" );
		}

		newURLString.append( "lasttime=" );
		newURLString.append( String.valueOf( lastSeen ) );

		if ( !tabbedChat )
		{
			newURLString.append( "&afk=" );
			newURLString.append( afk ? "1" : "0" );
		}

		this.constructURLString( newURLString.toString(), false );

		this.graf = "";
	}

	/**
	 * Constructs a new <code>ChatRequest</code> that will send the given
	 * string to the server.
	 *
	 * @param graf       The message to be sent
	 * @param tabbedChat true if "modern" chat, false if "older" chat
	 */

	public ChatRequest( final String graf, final boolean tabbedChat )
	{
		super( "", false );

		// Construct a URL to submit via GET, just like the browser
		StringBuilder newURLString = new StringBuilder( "submitnewchat.php?" );

		if ( tabbedChat )
		{
			newURLString.append( "j=1&" );
		}

		newURLString.append( "pwd=" );
		newURLString.append( GenericRequest.passwordHash );

		newURLString.append( "&playerid=" );
		newURLString.append( String.valueOf( KoLCharacter.getUserId() ) );

		newURLString.append( "&graf=" );
		newURLString.append( GenericRequest.encodeURL( graf, "ISO-8859-1" ) );

		this.constructURLString( newURLString.toString(), false );

		this.graf = graf;
	}

	public String getGraf()
	{
		return this.graf;
	}

	@Override
	public void run()
	{
		if ( !ChatManager.chatLiterate() )
		{
			return;
		}

		super.run();
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}
}
