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

import java.util.List;
import java.util.Calendar;
import javax.swing.JEditorPane;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.SortedListModel;

public class KoLMessenger
{
	private KoLmafia client;
	private ChatFrame channelFrame;
	private ChatBuffer loathingChat;
	private SortedListModel onlineContacts;

	public KoLMessenger( KoLmafia client )
	{
		this.client = client;
		this.onlineContacts = new SortedListModel();

		channelFrame = new ChatFrame( client, this );
		channelFrame.setVisible( true );

		loathingChat = new ChatBuffer( client.getLoginName() + ": Started " +
			Calendar.getInstance().getTime().toString() );

		loathingChat.setChatDisplay( channelFrame.getChatDisplay() );
	}

	/**
	 * Initializes the chat buffer with the provided chat pane.
	 * Note that the chat refresher will also be initialized
	 * by calling this method; to stop the chat refresher, call
	 * the <code>deinitializeChat()</code> method.
	 */

	public void initialize()
	{
		(new ChatRequest( client, "/channel" )).run();
		(new ChatRequest( client, "/friends" )).run();
		(new ChatRequest( client )).run();
	}

	/**
	 * Clears the contents of the chat buffer.  This is called
	 * whenever the user wishes for there to be less text.
	 */

	public void clearChatBuffer()
	{
		if ( loathingChat != null )
			loathingChat.clearBuffer();
	}

	/**
	 * Retrieves the chat buffer currently used for storing and
	 * saving the currently running chat.
	 *
	 * @return	The current chat buffer
	 */

	public ChatBuffer getChatBuffer()
	{	return loathingChat;
	}

	/**
	 * Returns whether or not the messenger is showing on screen.
	 * @return	<code>true</code> if the messenger is showing.
	 */

	public boolean isShowing()
	{	return channelFrame == null ? false : channelFrame.isShowing();
	}

	/**
	 * Sets the messenger's current visibility status.
	 * @param	isVisible	<code>true</code> if the messenger should be visible
	 */

	public void setVisible( boolean isVisible )
	{
		if ( channelFrame != null )
			channelFrame.setVisible( isVisible );
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public void dispose()
	{
		channelFrame.dispose();
		channelFrame = null;
		if ( loathingChat != null )
			loathingChat.closeActiveLogFile();
		loathingChat = null;
	}

	/**
	 * Requests forcus for the messenger's primary window.
	 */

	public void requestFocus()
	{	channelFrame.requestFocus();
	}

	/**
	 * Replaces the current contact list with the given contact
	 * list.  This is used after every call to /friends.
	 *
	 * @param	currentContacts	A list of the contacts currently online.
	 */

	public void updateContactList( List currentContacts )
	{
		onlineContacts.clear();
		onlineContacts.addAll( currentContacts );
	}

	/**
	 * Updates a single online/offline state for a character.
	 * @param	characterName	The character whose state has changed
	 * @param	isOnline	Whether or not they are online
	 */

	public void updateContactList( String characterName, boolean isOnline )
	{
		if ( isOnline && !onlineContacts.contains( characterName ) )
			onlineContacts.add( characterName );
		else if ( !isOnline )
			onlineContacts.remove( characterName );
	}
}