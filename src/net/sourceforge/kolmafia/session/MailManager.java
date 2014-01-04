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

package net.sourceforge.kolmafia.session;

import java.util.Map;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.KoLMailMessage;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.MailboxRequest;

public abstract class MailManager
{
	public static final Map mailboxes = new TreeMap();
	static
	{
		MailManager.mailboxes.put( "Inbox", new SortedListModel() );
		MailManager.mailboxes.put( "PvP", new SortedListModel() );
		MailManager.mailboxes.put( "Pen Pal", new SortedListModel() );
		MailManager.mailboxes.put( "Outbox", new SortedListModel() );
		MailManager.mailboxes.put( "Saved", new SortedListModel() );
	}

	public static final void clearMailboxes()
	{
		MailManager.getMessages( "Inbox" ).clear();
		MailManager.getMessages( "PvP" ).clear();
		MailManager.getMessages( "Pen Pal" ).clear();
		MailManager.getMessages( "Outbox" ).clear();
		MailManager.getMessages( "Saved" ).clear();
	}

	public static final boolean hasNewMessages()
	{
		String oldMessageId = Preferences.getString( "lastMessageId" );

		SortedListModel inbox = MailManager.getMessages( "Inbox" );
		if ( inbox.isEmpty() )
		{
			return false;
		}

		KoLMailMessage latest = (KoLMailMessage) inbox.get( 0 );
		String newMessageId = latest.getMessageId();

		Preferences.setString( "lastMessageId", newMessageId );
		return !oldMessageId.equals( newMessageId );
	}

	/**
	 * Returns a list containing the messages within the specified mailbox.
	 */

	public static final SortedListModel getMessages( final String mailbox )
	{
		return (SortedListModel) MailManager.mailboxes.get( mailbox );
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
		SortedListModel mailbox = (SortedListModel) MailManager.mailboxes.get( boxname );

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

		SortedListModel mailbox = (SortedListModel) MailManager.mailboxes.get( boxname );
		int messageIndex = mailbox.indexOf( message );
		if ( messageIndex != -1 )
		{
			mailbox.remove( messageIndex );
		}

		Preferences.setInteger( "lastMessageCount", MailManager.getMessages( "Inbox" ).size() );
	}

	public static final void deleteMessages( final String boxname, final Object[] messages )
	{
		if ( messages.length == 0 )
		{
			return;
		}

		RequestThread.postRequest( new MailboxRequest( boxname, messages, "delete" ) );

		int messageIndex;
		SortedListModel mailbox = (SortedListModel) MailManager.mailboxes.get( boxname );
		for ( int i = 0; i < messages.length; ++i )
		{
			messageIndex = mailbox.indexOf( messages[ i ] );
			if ( messageIndex != -1 )
			{
				mailbox.remove( messageIndex );
			}
		}

		Preferences.setInteger( "lastMessageCount", MailManager.getMessages( "Inbox" ).size() );
	}

	public static final void saveMessage( final String boxname, final KoLMailMessage message )
	{
		RequestThread.postRequest( new MailboxRequest( boxname, message, "save" ) );

		SortedListModel mailbox = (SortedListModel) MailManager.mailboxes.get( boxname );
		int messageIndex = mailbox.indexOf( message );
		if ( messageIndex != -1 )
		{
			mailbox.remove( messageIndex );
		}

		Preferences.setInteger( "lastMessageCount", MailManager.getMessages( "Inbox" ).size() );
	}

	public static final void saveMessages( final String boxname, final Object[] messages )
	{
		if ( messages.length == 0 )
		{
			return;
		}

		RequestThread.postRequest( new MailboxRequest( boxname, messages, "save" ) );

		int messageIndex;
		SortedListModel mailbox = (SortedListModel) MailManager.mailboxes.get( boxname );
		for ( int i = 0; i < messages.length; ++i )
		{
			messageIndex = mailbox.indexOf( messages[ i ] );
			if ( messageIndex != -1 )
			{
				mailbox.remove( messageIndex );
			}
		}

		Preferences.setInteger( "lastMessageCount", MailManager.getMessages( "Inbox" ).size() );
	}
}
