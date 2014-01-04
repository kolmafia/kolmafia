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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLMailMessage;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.session.BuffBotManager;
import net.sourceforge.kolmafia.session.MailManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MailboxRequest
	extends GenericRequest
{
	private static final Pattern MULTIPAGE_PATTERN =
		Pattern.compile( "Messages: \\w*?, page \\d* \\(\\d* - (\\d*) of (\\d*)\\)</b>" );
	private static final Pattern SINGLEPAGE_PATTERN =
		Pattern.compile( "Messages: \\w*?, page 1 \\((\\d*) messages\\)</b>" );

	private int beginIndex;
	private final String boxname;
	private final String action;

	public MailboxRequest( final String boxname, final KoLMailMessage message, final String action )
	{
		this( boxname, new Object[] { message }, action );
	}

	public MailboxRequest( final String boxname, final Object[] messages, final String action )
	{
		super( "messages.php" );
		this.addFormField( "box", boxname );
		this.addFormField( "the_action", action );

		this.action = action;
		this.boxname = boxname;
		this.beginIndex = 1 ;
		for ( int i = 0; i < messages.length; ++i )
		{
			this.addFormField( ( (KoLMailMessage) messages[ i ] ).getMessageId(), "on" );
		}
	}

	public MailboxRequest( final String boxname )
	{
		this( boxname, 1 );
	}

	public MailboxRequest( final String boxname, final int beginIndex )
	{
		super( "messages.php" );
		this.addFormField( "box", boxname );

		if ( beginIndex != 1 )
		{
			this.addFormField( "begin", String.valueOf( beginIndex ) );
		}

		this.action = null;
		this.boxname = boxname;
		this.beginIndex = beginIndex;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		// Now you know that there is a request in progress, so you
		// reset the variable (to avoid concurrent requests).

		if ( this.action == null )
		{
			KoLmafia.updateDisplay( "Retrieving mail from " + this.boxname + "..." );
		}
		else
		{
			KoLmafia.updateDisplay( "Executing " + this.action + " request for " + this.boxname + "..." );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		// Determine how many messages there are, and how many there
		// are left to go.  This will cause a lot of server load for
		// those with lots of messages.	 But!  This can be fixed by
		// testing the mail manager to see if it thinks all the new
		// messages have been retrieved.

		if ( this.responseText.indexOf( "There are no messages in this mailbox." ) != -1 )
		{
			KoLmafia.updateDisplay( "Your mailbox is empty." );
			return;
		}

		int lastMessageId = 0;
		int totalMessages = Integer.MAX_VALUE;

		try
		{
			Matcher matcher = MailboxRequest.MULTIPAGE_PATTERN.matcher( this.responseText );

			if ( matcher.find() )
			{
				lastMessageId = StringUtilities.parseInt( matcher.group( 1 ) );
				totalMessages = StringUtilities.parseInt( matcher.group( 2 ) );
			}
			else
			{
				matcher = MailboxRequest.SINGLEPAGE_PATTERN.matcher( this.responseText );
				if ( matcher.find() )
				{
					lastMessageId = StringUtilities.parseInt( matcher.group( 1 ) );
					totalMessages = lastMessageId;
				}
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error in mail retrieval" );
			return;
		}

		if ( this.responseText.indexOf( "<td valign=top>" ) == -1 )
		{
			KoLmafia.updateDisplay( "Your mailbox is empty." );
			return;
		}

		this.processMessages();
		KoLmafia.updateDisplay( "Mail retrieved from page " + this.beginIndex + " of " + this.boxname );

		if ( this.boxname.equals( "PvP" ) && lastMessageId != totalMessages )
		{
			( new MailboxRequest( "PvP", this.beginIndex + 1 ) ).run();
		}
	}

	private void processMessages()
	{
		boolean shouldContinueParsing = true;

		int lastMessageIndex = 0;
		int nextMessageIndex = this.responseText.indexOf( "<td valign=top>" );

		String currentMessage;

		do
		{
			lastMessageIndex = nextMessageIndex;
			nextMessageIndex = this.responseText.indexOf( "<td valign=top>", lastMessageIndex + 15 );

			// The last message in the inbox has no "next message
			// index".  In this case, locate the bold X and use
			// that as the next message index.

			if ( nextMessageIndex == -1 )
			{
				nextMessageIndex = this.responseText.indexOf( "<b>X</b>", lastMessageIndex + 15 );
				shouldContinueParsing = false;
			}

			if ( nextMessageIndex == -1 )
			{
				return;
			}

			// If the next message index is still non-positive, that
			// means there aren't any messages left to parse.

			currentMessage = this.responseText.substring( lastMessageIndex, nextMessageIndex );

			// This replaces all of the HTML contained within the message to something
			// that can be rendered with the default RequestPane, and also be subject
			// to the custom font sizes provided by LimitedSizeChatBuffer.

			currentMessage =
				currentMessage.replaceAll( "<br />", "<br>" ).replaceAll( "</?t.*?>", "\n" ).replaceAll(
					"<blockquote>", "<br>" ).replaceAll( "</blockquote>", "" ).replaceAll( "\n", "" ).replaceAll(
					"<center>", "<br><center>" );

			// At this point, the message is registered with the mail manager, which
			// records the message and updates whether or not you should continue.

			if ( BuffBotHome.isBuffBotActive() )
			{
				shouldContinueParsing = BuffBotManager.addMessage( this.boxname, currentMessage ) != null;
			}
			else
			{
				shouldContinueParsing = MailManager.addMessage( this.boxname, currentMessage ) != null;
			}
		}
		while ( shouldContinueParsing && nextMessageIndex != -1 );
	}
}
