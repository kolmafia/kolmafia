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
	private String boxname;

	public MailboxRequest( KoLmafia client, String boxname )
	{	this( client, boxname, 0 );
	}

	private MailboxRequest( KoLmafia client, String boxname, int startingIndex )
	{
		super( client, "messages.php" );
		addFormField( "box", boxname );
		addFormField( "begin", "" + startingIndex );

		this.boxname = boxname;
	}

	public void run()
	{
		super.run();

		Matcher messageMatcher = Pattern.compile( "<tr><td valign=top>.*?</tr>" ).matcher( replyContent );
		String currentMessage, currentPlainTextMessage;
		int lastMessageIndex = 0;

		while ( messageMatcher.find( lastMessageIndex ) )
		{
			lastMessageIndex = messageMatcher.end();

			// This replaces all of the HTML contained within the message to something
			// that can be rendered with the default JEditorPane, and also be subject
			// to the custom font sizes provided by LimitedSizeChatBuffer.

			currentMessage = messageMatcher.group().replaceAll( "<br />" , "<br>" ).replaceAll( "</?t.*?>" , "" ).replaceAll(
				"<blockquote>", "<br>" ).replaceAll( "</blockquote>", "" );

			// At this point, a brand new KoLGreenMessage would be created and added
			// to the list of items.  However, because the internal structure of a
			// KoLGreenMessage has not yet been decided, do nothing.
		}

		// Determine how many messages there are, and how many there are left
		// to go.  This will cause a lot of server load for those with lots
		// of messages, though ... hopefully, people don't load things often.

		try
		{
			Matcher messageCountMatcher = Pattern.compile( "[\\d]+" ).matcher(
				replyContent.substring( replyContent.indexOf( " - " ) + 3, replyContent.indexOf( "</b>" ) ) );

			messageCountMatcher.find();
			int lastMessageID = df.parse( messageCountMatcher.group() ).intValue();

			messageCountMatcher.find( 4 );
			int totalMessages = df.parse( messageCountMatcher.group() ).intValue();

			if ( lastMessageID != totalMessages )
				(new MailboxRequest( client, boxname, lastMessageID )).run();
		}
		catch ( Exception e )
		{
			// If an exception is caught, do absolutely nothing because
			// the page has somehow changed (HTML-wise)
		}
	}
}
