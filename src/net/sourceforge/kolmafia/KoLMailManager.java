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
import net.java.dev.spellcast.utilities.LockableListModel;

public abstract class KoLMailManager extends StaticEntity
{
	protected static Map mailboxes = new TreeMap();
	static
	{
		mailboxes.put( "Inbox", new LockableListModel() );
		mailboxes.put( "PvP", new LockableListModel() );
		mailboxes.put( "Outbox", new LockableListModel() );
		mailboxes.put( "Saved", new LockableListModel() );
	}

	public static void reset()
	{
		getMessages( "Inbox" ).clear();
		getMessages( "PvP" ).clear();
		getMessages( "Outbox" ).clear();
		getMessages( "Saved" ).clear();
	}

	/**
	 * Returns a list containing the messages within the
	 * specified mailbox.
	 */

	public static LockableListModel getMessages( String mailbox )
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

	public static boolean addMessage( String boxname, String message )
	{
		LockableListModel mailbox = (LockableListModel) mailboxes.get(
			boxname.equals( "Inbox" ) && message.indexOf( "Here's a play-by-play report on how it went down" ) != -1 ? "PvP" : boxname );

		KoLMailMessage toadd = new KoLMailMessage( message );

		if ( mailbox.contains( toadd ) )
			return false;

		KoLmafia.registerPlayer( toadd.getSenderName(), toadd.getSenderID() );
		mailbox.add( toadd );
		return true;
	}

	public static void deleteMessage( String boxname, KoLMailMessage message )
	{
		(new RequestThread( new MailboxRequest( client, boxname.equals( "PvP" ) ? "Inbox" : boxname, message, "delete" ) )).start();

		LockableListModel mailbox = (LockableListModel) mailboxes.get( boxname );
		int messageIndex = mailbox.indexOf( message );
		if ( messageIndex != -1 )
			mailbox.remove( messageIndex );
	}

	public static void deleteMessages( String boxname, Object [] messages )
	{
		if ( messages.length == 0 )
			return;

		(new MailboxRequest( client, boxname.equals( "PvP" ) ? "Inbox" : boxname, messages, "delete" )).run();

		int messageIndex;
		LockableListModel mailbox = (LockableListModel) mailboxes.get( boxname );
		for ( int i = 0; i < messages.length; ++i )
		{
			messageIndex = mailbox.indexOf( messages[i] );
			if ( messageIndex != -1 )
				mailbox.remove( messageIndex );
		}
	}

	public static void saveMessage( KoLMailMessage message )
	{
		(new MailboxRequest( client, "Inbox", message, "save" )).run();

		LockableListModel mailbox = (LockableListModel) mailboxes.get( "Inbox" );
		int messageIndex = mailbox.indexOf( message );
		if ( messageIndex != -1 )
			mailbox.remove( messageIndex );
	}

	public static void saveMessages( Object [] messages )
	{
		if ( messages.length == 0 )
			return;

		(new RequestThread( new MailboxRequest( client, "Inbox", messages, "save" ) )).start();

		int messageIndex;
		LockableListModel mailbox = (LockableListModel) mailboxes.get( "Inbox" );
		for ( int i = 0; i < messages.length; ++i )
		{
			messageIndex = mailbox.indexOf( messages[i] );
			if ( messageIndex != -1 )
				mailbox.remove( messageIndex );
		}
	}
}