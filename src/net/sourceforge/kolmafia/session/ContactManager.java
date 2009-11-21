/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.ContactListRequest;
import net.sourceforge.kolmafia.swingui.ContactListFrame;

public class ContactManager
{
	private static final HashMap seenPlayerIds = new HashMap();
	private static final HashMap seenPlayerNames = new HashMap();

	public static final SortedListModel mailContacts = new SortedListModel();
	private static final SortedListModel chatContacts = new SortedListModel();

	private static ContactListFrame contactsFrame = null;

	public static final boolean isMailContact( final String playerName )
	{
		return ContactManager.mailContacts.contains( playerName );
	}

	public static final SortedListModel getMailContacts()
	{
		if ( ContactManager.mailContacts.isEmpty() )
		{
			if ( Preferences.getBoolean( "retrieveContacts" ) )
			{
				RequestThread.postRequest( new ContactListRequest() );
			}

			Preferences.setBoolean( "retrieveContacts", !ContactManager.mailContacts.isEmpty() );
		}

		return ContactManager.mailContacts;
	}

	/**
	 * Replaces the current contact list with the given contact list. This is used after every call to /friends or /who.
	 */

	public static final void updateContactList( final String[] contactList )
	{
		if ( !ChatManager.isRunning() )
		{
			return;
		}

		ContactManager.chatContacts.clear();

		for ( int i = 1; i < contactList.length; ++i )
		{
			ContactManager.chatContacts.add( contactList[ i ] );
		}

		if ( Preferences.getBoolean( "useContactsFrame" ) )
		{
			if ( ContactManager.contactsFrame == null )
			{
				ContactManager.contactsFrame = new ContactListFrame( ContactManager.chatContacts );
			}

			ContactManager.contactsFrame.setTitle( contactList[ 0 ] );
			ContactManager.contactsFrame.setVisible( true );
		}
	}

	public static final void addMailContact( String playerName, final String playerId )
	{
		ContactManager.registerPlayerId( playerName, playerId );

		playerName = playerName.toLowerCase().replaceAll( "[^0-9A-Za-z_ ]", "" );

		if ( !ContactManager.mailContacts.contains( playerName ) )
		{
			ContactManager.mailContacts.add( playerName.toLowerCase() );
		}
	}

	/**
	 * Registers the given player name and player Id with KoLmafia's player name tracker.
	 *
	 * @param playerName The name of the player
	 * @param playerId The player Id associated with this player
	 */

	public static final void registerPlayerId( String playerName, final String playerId )
	{
		playerName = playerName.replaceAll( "[^0-9A-Za-z_ ]", "" );
		String lowercase = playerName.toLowerCase();

		if ( lowercase.equals( "modwarning" ) || ContactManager.seenPlayerIds.containsKey( lowercase ) )
		{
			return;
		}

		ContactManager.seenPlayerIds.put( lowercase, playerId );
		ContactManager.seenPlayerNames.put( playerId, playerName );
	}

	/**
	 * Returns the string form of the player Id associated with the given player name.
	 *
	 * @param playerName The name of the player
	 * @return The player's Id if the player has been seen, or the player's name with spaces replaced with underscores
	 *         and other elements encoded if the player's Id has not been seen.
	 */

	public static final String getPlayerId( final String playerName )
	{
		if ( playerName == null )
		{
			return null;
		}

		String playerId = (String) ContactManager.seenPlayerIds.get( playerName.toLowerCase() );
		return playerId != null ? playerId : playerName;
	}

	/**
	 * Returns the string form of the player Id associated with the given player name.
	 *
	 * @param playerId The Id of the player
	 * @return The player's name if it has been seen, or null if it has not yet appeared in the chat (not likely, but
	 *         possible).
	 */

	public static final String getPlayerName( final String playerId )
	{
		if ( playerId == null )
		{
			return null;
		}

		String playerName = (String) ContactManager.seenPlayerNames.get( playerId );
		return playerName != null ? playerName : playerId;
	}
}
