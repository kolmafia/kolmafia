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

import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.StringTokenizer;

import javax.swing.JEditorPane;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.SortedListModel;

public class KoLMessenger
{
	private KoLmafia client;
	private ContactListFrame contactsFrame;

	private ChatFrame mainChatFrame;
	private ChatBuffer mainChatBuffer;

	private TreeMap instantMessageFrames;
	private TreeMap instantMessageBuffers;

	private SortedListModel onlineContacts;

	public KoLMessenger( KoLmafia client )
	{
		this.client = client;
		this.onlineContacts = new SortedListModel();

		mainChatFrame = new ChatFrame( client, this );
		mainChatFrame.setVisible( true );

		this.instantMessageFrames = new TreeMap();
		this.instantMessageBuffers = new TreeMap();

		mainChatBuffer = new ChatBuffer( client.getLoginName() + ": Started " +
			Calendar.getInstance().getTime().toString() );

		mainChatBuffer.setChatDisplay( mainChatFrame.getChatDisplay() );
	}

	/**
	 * Initializes the chat buffer with the provided chat pane.
	 * Note that the chat refresher will also be initialized
	 * by calling this method; to stop the chat refresher, call
	 * the <code>deinitializeChat()</code> method.
	 */

	public void initialize()
	{
		(new ChatRequest( client, null, "/channel" )).run();
		(new ChatRequest( client, null, "/friends" )).run();
		(new ChatRequest( client )).run();
	}

	/**
	 * Clears the contents of the chat buffer.  This is called
	 * whenever the user wishes for there to be less text.
	 */

	public void clearChatBuffer( String contact )
	{
		ChatBuffer bufferToClear = getChatBuffer( contact );
		if ( bufferToClear != null )
			bufferToClear.clearBuffer();
	}

	/**
	 * Retrieves the chat buffer currently used for storing and
	 * saving the currently running chat associated with the
	 * given contact.  If the contact is <code>null</code>, this
	 * method returns the main chat.
	 *
	 * @param	contact	Name of the contact
	 * @return	The chat buffer for the given contact
	 */

	public ChatBuffer getChatBuffer( String contact )
	{
		return contact == null ? mainChatBuffer :
			(ChatBuffer) instantMessageBuffers.get( contact );
	}

	/**
	 * Removes the chat associated with the given contact.  This
	 * method is called whenever a window is closed.
	 */

	public void removeChat( String contact )
	{
		ChatFrame frameToRemove;
		ChatBuffer bufferToRemove;

		if ( contact == null )
		{
			frameToRemove = mainChatFrame;
			mainChatFrame = null;

			bufferToRemove = mainChatBuffer;
			mainChatBuffer = null;
		}
		else
		{
			frameToRemove = (ChatFrame) instantMessageFrames.remove( contact );
			bufferToRemove = (ChatBuffer) instantMessageBuffers.remove( contact );
		}

		if ( frameToRemove != null )
		{
			frameToRemove.setVisible( false );
			frameToRemove.dispose();
		}

		if ( bufferToRemove != null )
			bufferToRemove.closeActiveLogFile();
	}

	/**
	 * Returns whether or not the messenger is showing on screen.
	 * @return	<code>true</code> if the messenger is showing.
	 */

	public boolean isShowing()
	{	return mainChatFrame == null ? false : mainChatFrame.isShowing();
	}

	/**
	 * Sets the messenger's current visibility status.
	 * @param	isVisible	<code>true</code> if the messenger should be visible
	 */

	public void setVisible( boolean isVisible )
	{
		if ( mainChatFrame != null )
			mainChatFrame.setVisible( isVisible );

		Iterator frames = instantMessageFrames.values().iterator();
		while ( frames.hasNext() )
			((ChatFrame) frames.next()).setVisible( isVisible );
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public void dispose()
	{
		removeChat( null );
		while ( !instantMessageFrames.isEmpty() )
			removeChat( (String) instantMessageFrames.firstKey() );
	}

	/**
	 * Requests forcus for the messenger's primary window.
	 */

	public void requestFocus()
	{	mainChatFrame.requestFocus();
	}

	/**
	 * Replaces the current contact list with the given contact
	 * list.  This is used after every call to /friends.
	 *
	 * @param	currentContacts	A list of the contacts currently online.
	 */

	private void updateContactList( List currentContacts )
	{
		onlineContacts.clear();
		onlineContacts.addAll( currentContacts );
	}

	/**
	 * Updates a single online/offline state for a character.
	 * @param	characterName	The character whose state has changed
	 * @param	isOnline	Whether or not they are online
	 */

	private void updateContactList( String characterName, boolean isOnline )
	{
		if ( isOnline && !onlineContacts.contains( characterName ) )
			onlineContacts.add( characterName );
		else if ( !isOnline )
			onlineContacts.remove( characterName );
	}

	/**
	 * Updates the chat with the given information.  This method will
	 * also handle instant message data.
	 *
	 * @param	content	The content with which to update the chat
	 */

	public void updateChat( String originalContent )
	{
		String orderedTagsContent = originalContent.replaceAll( "<b><i>", "<i><b>" );
		String correctedTagsContent = orderedTagsContent.replaceAll( "</br>", "<br>" );
		String validColorsContent = correctedTagsContent.replaceAll( "<font color=\"none\">", "" ).replaceAll( "</b></font>", "</b>" );
		String noDoubleTagsContent = validColorsContent.replaceAll( "<font.*?><font.*?>", "<font color=green>" );
		String noCommentsContent = noDoubleTagsContent.replaceAll( "<!.*?>", "" );
		String noContactListContent = noCommentsContent.replaceAll( "<table>.*?</table>", "" );
		String noLinksContent = noContactListContent.replaceAll( "</?a.*?>", "" );

		// For now, update the main chat buffer with the entire
		// content.  Instant message changes will follow.

		if ( mainChatBuffer != null )
		{
			String [] lines = noLinksContent.split( "<br>" );
			for ( int i = 0; i < lines.length; ++i )
				processChatMessage( lines[i] );
		}

		// Now, extract the contact list and update KoLMessenger to indicate
		// the contact list found in the last /friends update

		Matcher contactListMatcher = Pattern.compile( "<table>.*?</table>" ).matcher( originalContent );
		if ( contactListMatcher.find() )
		{
			StringTokenizer parsedContactList = new StringTokenizer( contactListMatcher.group().replaceAll( "<.*?>", "\n" ), "\n" );
			parsedContactList.nextToken();

			List newContactList = new ArrayList();
			while ( parsedContactList.hasMoreTokens() )
				newContactList.add( parsedContactList.nextToken() );
			updateContactList( newContactList );
		}

		// Also extract messages which indicate logon/logoff of players to
		// update the contact list.

		Matcher onlineNoticeMatcher = Pattern.compile( "<font.*?><b>.*</font>" ).matcher( noLinksContent );
		int lastFindIndex = 0;
		while ( onlineNoticeMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = onlineNoticeMatcher.end();
			String [] onlineNotice = onlineNoticeMatcher.group().split( "<.*?>" );
			updateContactList( onlineNotice[2], onlineNotice[3].endsWith( "on." ) );
		}
	}

	/**
	 * Utility method to update the appropriate chat window with the
	 * given message.
	 */

	public void processChatMessage( String message )
	{
		if ( !message.startsWith( "<font color=blue>" ) )
		{
			// The easy case is if it's a normal chat message.
			// Then, it just gets updated to the main chat buffer,
			// provided that the main chat buffer is not null

			if ( mainChatBuffer != null )
				mainChatBuffer.append( message + "<br>\n" );
		}
		else
		{
			// The harder case is if you have a private message.
			// First, you have to determine who sent the message;
			// either the client is the recipient or was the sender.
			// This is determined by where the colon is - in a
			// send, it is not bolded, while in a receive, it is.

			boolean isRecipient = message.indexOf( "</b>:" ) == -1;

			// Next, split the message around the tags so you know
			// how to display the message.

			StringTokenizer splitMessage = new StringTokenizer( message.replaceAll( "<.*?>", "\n" ), "\n" );

			// For now, just display the message inside of the
			// main frame.

			ChatBuffer messageBuffer = mainChatBuffer;

			if ( messageBuffer != null )
				messageBuffer.append( message + "<br>\n" );
		}
	}

	/**
	 * Opens an instant message window to the character with the
	 * given name so that a private conversation can be started.
	 *
	 * @param	characterName	The name of the person being messaged
	 */

	public void openInstantMessage( String characterName )
	{
		ChatFrame newFrame = new ChatFrame( client, this, characterName );
		ChatBuffer newBuffer = new ChatBuffer( client.getLoginName() + " (Conversation with " + characterName + ") : Started " +
			Calendar.getInstance().getTime().toString() );

		newBuffer.setChatDisplay( newFrame.getChatDisplay() );
		newFrame.setVisible( true );
	}
}