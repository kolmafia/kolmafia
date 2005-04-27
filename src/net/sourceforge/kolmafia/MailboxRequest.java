/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MailboxRequest extends KoLRequest
{
	private static boolean isRequesting = false;
	private static long lastRequest = System.currentTimeMillis();

	private String boxname;
	private int startingIndex;
	private String action;

	public static boolean isRequesting()
	{	return isRequesting;
	}

	public static long getLastRequest()
	{	return lastRequest;
	}

	public MailboxRequest( KoLmafia client, String boxname )
	{	this( client, boxname, 0 );
	}

	public MailboxRequest( KoLmafia client, String boxname, KoLMailMessage message, String action )
	{
		super( client, "messages.php" );
		addFormField( "box", boxname );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "action", action );

		this.action = action;
		this.boxname = boxname;
		addFormField( message.getMessageID(), "on" );
	}

	public MailboxRequest( KoLmafia client, String boxname, Object [] messages, String action )
	{
		super( client, "messages.php" );
		addFormField( "box", boxname );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "action", action );

		this.action = action;
		this.boxname = boxname;
		for ( int i = 0; i < messages.length; ++i )
			addFormField( ((KoLMailMessage) messages[i]).getMessageID(), "on" );
	}

	private MailboxRequest( KoLmafia client, String boxname, int startingIndex )
	{
		super( client, "messages.php" );
		addFormField( "box", boxname );

		if ( startingIndex != 0 )
			addFormField( "begin", "" + startingIndex );

		this.action = null;
		this.boxname = boxname;
		this.startingIndex = startingIndex;
	}

	public void run()
	{
		// In order to prevent multiple mailbox requests from running,
		// a test is made on a static variable to halt concurrent requests.

		if ( isRequesting )
			return;

		// Now you know that there is a request in progress, so you
		// reset the variable (to avoid concurrent requests).

		if ( action == null )
			updateDisplay( NOCHANGE, "Retrieving mail from " + boxname + "..." );
		else
			updateDisplay( NOCHANGE, "Executing " + action + " request for " + boxname + "..." );

		lastRequest = System.currentTimeMillis();
		isRequesting = true;

		super.run();

		if ( action != null )
		{
			updateDisplay( NOCHANGE, "Selected mail successfully " + action + "d" );
			isRequesting = false;
			return;
		}

		boolean shouldContinueParsing = true;
		KoLMailManager currentMailManager = client.getMailManager();

		int lastMessageIndex = replyContent.indexOf( "<td valign=top>" );
		int nextMessageIndex = lastMessageIndex;

		// Test to see if there weren't any messages; if there
		// weren't any messages, there are no messages to parse,
		// so return from the method without doing anything.

		if ( lastMessageIndex == -1 )
		{
			updateDisplay( NOCHANGE, "Your mailbox is empty." );
			isRequesting = false;
			return;
		}

		String currentMessage;
		while ( shouldContinueParsing )
		{
			lastMessageIndex = nextMessageIndex;
			nextMessageIndex = replyContent.indexOf( "<td valign=top>", lastMessageIndex + 15 );

			// The last message in the inbox has no "next message index".
			// In this case, locate the last index of the link tag and
			// use that as the last message index.

			if ( nextMessageIndex == -1 )
			{
				nextMessageIndex = replyContent.lastIndexOf( "<a" );
				shouldContinueParsing = false;
			}

			currentMessage = replyContent.substring( lastMessageIndex, nextMessageIndex );

			// This replaces all of the HTML contained within the message to something
			// that can be rendered with the default JEditorPane, and also be subject
			// to the custom font sizes provided by LimitedSizeChatBuffer.

			currentMessage = currentMessage.replaceAll( "<br />" , "<br>" ).replaceAll( "</?t.*?>" , "\n" ).replaceAll(
				"<blockquote>", "<br>" ).replaceAll( "</blockquote>", "" ).replaceAll( "\n", "" ).replaceAll( "<center>", "<br><center>" );

			// At this point, the message is registered with the mail manager, which
			// records the message and updates whether or not you should continue.

			shouldContinueParsing &= currentMailManager.addMessage( boxname, currentMessage );
		}

		// Determine how many messages there are, and how many there are left
		// to go.  This will cause a lot of server load for those with lots
		// of messages.  But!  This can be fixed by testing the mail manager
		// to see if it thinks all the new messages have been retrieved.

		isRequesting = false;
		if ( nextMessageIndex != -1 )
		{
			try
			{
				Matcher messageCountMatcher = Pattern.compile( "\\d+" ).matcher(
					replyContent.substring( replyContent.indexOf( " - " ) + 3, replyContent.indexOf( "</b>" ) ) );

				messageCountMatcher.find();
				int lastMessageID = df.parse( messageCountMatcher.group() ).intValue();

				messageCountMatcher.find( 4 );
				int totalMessages = df.parse( messageCountMatcher.group() ).intValue();

				if ( lastMessageID != totalMessages )
					(new MailboxRequest( client, boxname, lastMessageID )).run();
				else
					updateDisplay( NOCHANGE, "Mail retrieved from " + boxname );
			}
			catch ( Exception e )
			{
				// If an exception is caught, do absolutely nothing because
				// the page has somehow changed (HTML-wise)
			}
		}
		else
			updateDisplay( NOCHANGE, "Mail retrieved from " + boxname );
	}
}
