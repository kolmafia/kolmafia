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

package net.sourceforge.kolmafia;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class KoLMailMessage
	implements Comparable<KoLMailMessage>
{
	private static final SimpleDateFormat TIMESTAMP_FORMAT =
		new SimpleDateFormat( "EEEE, MMMM dd, yyyy, hh:mmaa", Locale.US );

	private final String messageId;
	private final String senderId;
	private final String senderName;
	private String messageDate;
	private Date timestamp;
	private final String messageHTML;

	private String completeHTML;

	public KoLMailMessage( final String message )
	{
		// Blank lines are not displayed correctly
		this.completeHTML = StringUtilities.globalStringReplace( message, "<br><br>", "<br>&nbsp;<br>" );

		// Extract message ID
		this.messageId = message.substring( message.indexOf( "name=" ) + 6, message.indexOf( "\">" ) );

		// Tokenize message
		StringTokenizer messageParser = new StringTokenizer( message, "<>" );
		String lastToken = messageParser.nextToken();

		// Trim off message ID
		this.completeHTML = this.completeHTML.substring( this.completeHTML.indexOf( ">" ) + 1 );


		// Messages from pseudo-characters do not have a [reply] link
		int replyLink = this.completeHTML.indexOf( "reply</a>]" );
		if ( replyLink > 0 )
		{
			// Real sender. Trim message, parse and register sender
			while ( !lastToken.startsWith( "a " ) )
			{
				lastToken = messageParser.nextToken();
			}

			this.senderId = lastToken.substring( lastToken.indexOf( "who=" ) + 4, lastToken.length() - 1 );
			this.senderName = messageParser.nextToken();

			ContactManager.registerPlayerId( this.senderName, this.senderId );
		}
		else
		{
			// Pseudo player.
			this.senderId = "";

			while ( !lastToken.startsWith( "/b" ) )
			{
				lastToken = messageParser.nextToken();
			}

			String name = messageParser.nextToken();
			int sp = name.indexOf( "&nbsp;" );
			if ( sp > 0 )
			{
				name = name.substring( 0, sp );
			}
			this.senderName = name.trim();
		}

		while ( !messageParser.nextToken().startsWith( "Date" ) )
		{
			;
		}
		messageParser.nextToken();

		this.messageDate = messageParser.nextToken().trim();
		this.messageHTML = message.substring( message.indexOf( this.messageDate ) + this.messageDate.length() + 4 );

		try
		{
			// This attempts to parse the date from
			// the given string; note it may throw
			// an exception (but probably not)

			this.timestamp = KoLMailMessage.TIMESTAMP_FORMAT.parse( this.messageDate );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Could not parse date \"" + this.messageDate + "\"" );

			// Initialize the date to the current time,
			// since that's about as close as it gets

			this.timestamp = new Date();
			this.messageDate = KoLMailMessage.TIMESTAMP_FORMAT.format( this.timestamp );
		}
	}

	@Override
	public String toString()
	{
		return this.senderName + " @ " + this.messageDate;
	}

	public int compareTo( final KoLMailMessage o )
	{
		return o == null || !( o instanceof KoLMailMessage ) ? -1 : this.messageId.compareTo( ( (KoLMailMessage) o ).messageId );
	}

	@Override
	public boolean equals( final Object o )
	{
		return o == null || !( o instanceof KoLMailMessage ) ? false : this.messageId.equals( ( (KoLMailMessage) o ).messageId );
	}

	@Override
	public int hashCode()
	{
		return this.messageId != null ? this.messageId.hashCode() : 0;
	}

	public String getMessageId()
	{
		return this.messageId;
	}

	public Date getTimestamp()
	{
		return this.timestamp;
	}

	public String getCompleteHTML()
	{
		return this.completeHTML;
	}

	public String getMessageHTML()
	{
		return this.messageHTML;
	}

	public String getSenderName()
	{
		return this.senderName;
	}

	public String getSenderId()
	{
		return this.senderId;
	}
}
