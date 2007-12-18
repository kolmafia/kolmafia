/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import java.util.Map;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.SortedListModel;

public abstract class KoLMailManager
	extends StaticEntity
{
	public static final Map mailboxes = new TreeMap();
	static
	{
		KoLMailManager.mailboxes.put( "Inbox", new SortedListModel() );
		KoLMailManager.mailboxes.put( "PvP", new SortedListModel() );
		KoLMailManager.mailboxes.put( "Outbox", new SortedListModel() );
		KoLMailManager.mailboxes.put( "Saved", new SortedListModel() );
	}

	public static final void clearMailboxes()
	{
		KoLMailManager.getMessages( "Inbox" ).clear();
		KoLMailManager.getMessages( "PvP" ).clear();
		KoLMailManager.getMessages( "Outbox" ).clear();
		KoLMailManager.getMessages( "Saved" ).clear();
	}

	public static final boolean hasNewMessages()
	{
		String oldMessageId = KoLSettings.getUserProperty( "lastMessageId" );

		SortedListModel inbox = KoLMailManager.getMessages( "Inbox" );
		if ( inbox.isEmpty() )
		{
			return false;
		}

		KoLMailMessage latest = (KoLMailMessage) inbox.get( 0 );
		String newMessageId = latest.getMessageId();

		KoLSettings.setUserProperty( "lastMessageId", newMessageId );
		return !oldMessageId.equals( newMessageId );
	}

	/**
	 * Returns a list containing the messages within the specified mailbox.
	 */

	public static final SortedListModel getMessages( final String mailbox )
	{
		return (SortedListModel) KoLMailManager.mailboxes.get( mailbox );
	}

	/**
	 * Adds the given message to the given mailbox. This should be called whenever a new message is found, and should
	 * only be called again if the message indicates that the message was successfully added (it was a new message).
	 * 
	 * @param boxname The name of the mailbox being updated
	 * @param message The message to add to the given mailbox
	 */

	public static KoLMailMessage addMessage( final String boxname, final String message )
	{
		SortedListModel mailbox = (SortedListModel) KoLMailManager.mailboxes.get( boxname );

		KoLMailMessage toadd = new KoLMailMessage( message );

		if ( mailbox.contains( toadd ) )
		{
			return null;
		}

		mailbox.add( toadd );
		return toadd;
	}

	public static final void deleteMessage( final String boxname, final KoLMailMessage message )
	{
		RequestThread.postRequest( new MailboxRequest( boxname, message, "delete" ) );

		SortedListModel mailbox = (SortedListModel) KoLMailManager.mailboxes.get( boxname );
		int messageIndex = mailbox.indexOf( message );
		if ( messageIndex != -1 )
		{
			mailbox.remove( messageIndex );
		}

		KoLSettings.setUserProperty( "lastMessageCount", String.valueOf( KoLMailManager.getMessages( "Inbox" ).size() ) );
	}

	public static final void deleteMessages( final String boxname, final Object[] messages )
	{
		if ( messages.length == 0 )
		{
			return;
		}

		RequestThread.postRequest( new MailboxRequest( boxname, messages, "delete" ) );

		int messageIndex;
		SortedListModel mailbox = (SortedListModel) KoLMailManager.mailboxes.get( boxname );
		for ( int i = 0; i < messages.length; ++i )
		{
			messageIndex = mailbox.indexOf( messages[ i ] );
			if ( messageIndex != -1 )
			{
				mailbox.remove( messageIndex );
			}
		}

		KoLSettings.setUserProperty( "lastMessageCount", String.valueOf( KoLMailManager.getMessages( "Inbox" ).size() ) );
	}

	public static final void saveMessage( final KoLMailMessage message )
	{
		RequestThread.postRequest( new MailboxRequest( "Inbox", message, "save" ) );

		SortedListModel mailbox = (SortedListModel) KoLMailManager.mailboxes.get( "Inbox" );
		int messageIndex = mailbox.indexOf( message );
		if ( messageIndex != -1 )
		{
			mailbox.remove( messageIndex );
		}

		KoLSettings.setUserProperty( "lastMessageCount", String.valueOf( KoLMailManager.getMessages( "Inbox" ).size() ) );
	}

	public static final void saveMessages( final Object[] messages )
	{
		if ( messages.length == 0 )
		{
			return;
		}

		RequestThread.postRequest( new MailboxRequest( "Inbox", messages, "save" ) );

		int messageIndex;
		SortedListModel mailbox = (SortedListModel) KoLMailManager.mailboxes.get( "Inbox" );
		for ( int i = 0; i < messages.length; ++i )
		{
			messageIndex = mailbox.indexOf( messages[ i ] );
			if ( messageIndex != -1 )
			{
				mailbox.remove( messageIndex );
			}
		}

		KoLSettings.setUserProperty( "lastMessageCount", String.valueOf( KoLMailManager.getMessages( "Inbox" ).size() ) );
	}
}
