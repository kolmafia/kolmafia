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
import java.util.List;
import java.util.ArrayList;
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

	private Map instantMessageFrames;
	private Map instantMessageBuffers;

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
		if ( mainChatBuffer != null )
			mainChatBuffer.clearBuffer();
	}

	/**
	 * Retrieves the chat buffer currently used for storing and
	 * saving the currently running chat.
	 *
	 * @return	The current chat buffer for the main chat
	 */

	public ChatBuffer getChatBuffer()
	{	return mainChatBuffer;
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
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public void dispose()
	{
		mainChatFrame.dispose();
		mainChatFrame = null;
		if ( mainChatBuffer != null )
			mainChatBuffer.closeActiveLogFile();
		mainChatBuffer = null;
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

		mainChatBuffer.append( noLinksContent );

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
	 * Opens an instant message window to the character with the
	 * given name so that a private conversation can be started.
	 *
	 * @param	characterName	The name of the person being messaged
	 */

	public void openInstantMessage( String characterName )
	{
		ChatFrame newFrame = new ChatFrame( client, this, characterName );
		ChatBuffer newBuffer = new ChatBuffer( client.getLoginName() + " (Blue Messages to " + characterName + ") : Started " +
			Calendar.getInstance().getTime().toString() );

		newBuffer.setChatDisplay( newFrame.getChatDisplay() );

		newFrame.setVisible( true );
	}
}