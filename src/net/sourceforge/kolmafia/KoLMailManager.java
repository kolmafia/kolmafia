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

import java.util.Map;
import java.util.TreeMap;
import java.util.StringTokenizer;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

import net.java.dev.spellcast.utilities.LockableListModel;

public class KoLMailManager
{
	private static SimpleDateFormat sdf = new SimpleDateFormat( "EEEE, MMMM d, hh:mmaa", Locale.US );

	private Map mailboxes;

	public KoLMailManager( KoLmafia client )
	{
		mailboxes = new TreeMap();
		mailboxes.put( "Inbox", new LockableListModel() );
		mailboxes.put( "Outbox", new LockableListModel() );
		mailboxes.put( "Sent", new LockableListModel() );
	}

	/**
	 * Returns a list containing the messages within the
	 * specified mailbox.
	 */

	public LockableListModel getMessages( String mailbox )
	{	return (LockableListModel) mailboxes.get( mailbox );
	}

	/**
	 * Adds the given message to the given mailbox.  This should be
	 * called whenever a new message is found, and should only
	 * be called again if the message indicates that the message
	 * was successfully added (it was a new message).
	 *
	 * @param	boxname	The name of the mailbox being updated
	 * @param	message	The message to add to the given mailbox
	 */

	public boolean addMessage( String boxname, String message )
	{	return ((LockableListModel)mailboxes.get( boxname )).add( new KoLMailMessage( message ) );
	}

	private class KoLMailMessage
	{
		private String completeMessage;

		private String messageID;
		private String senderID;
		private String senderName;
		private Date messageDate;
		private String messageHTML;

		public KoLMailMessage( String message )
		{
			this.completeMessage = message;
			this.messageID = completeMessage.substring( message.indexOf( "name=" ) + 6, message.indexOf( "\">" ) );
			StringTokenizer messageParser = new StringTokenizer( message, "<>" );

			String lastToken = messageParser.nextToken();
			while ( !lastToken.startsWith( "a " ) )
				lastToken = messageParser.nextToken();

			this.senderID = lastToken.substring( lastToken.indexOf( "who=" ) + 4, lastToken.length() - 2 );
			this.senderName = messageParser.nextToken();

			while ( !messageParser.nextToken().startsWith( "Date" ) );
			messageParser.nextToken();

			try
			{
				// This attempts to parse the date from
				// the given string; note it may throw
				// an exception (but probably not)
				this.messageDate = sdf.parse( messageParser.nextToken() );
			}
			catch ( Exception e )
			{
				// Initialize the date to the current time,
				// since that's about as close as it gets
				this.messageDate = new Date();
			}

			this.messageHTML = message.substring( message.indexOf( "<!-- -->" ) + 8 );
		}

		public String toString()
		{	return "From " + senderName + " @ " + messageDate.toString();
		}
	}
}