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

import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

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

		this.instantMessageFrames = new TreeMap();
		this.instantMessageBuffers = new TreeMap();

		contactsFrame = new ContactListFrame( client, onlineContacts );

		mainChatFrame = new ChatFrame( client, this );
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
		KoLFrame activeFrame = client.getActiveFrame();

		activeFrame.updateDisplay( KoLFrame.NOCHANGE_STATE, "Connecting to chat..." );
		(new ChatRequest( client, null, "/channel" )).run();

		activeFrame.updateDisplay( KoLFrame.NOCHANGE_STATE, "Retrieving contact list..." );
		(new ChatRequest( client, null, "/friends" )).run();

		activeFrame.updateDisplay( KoLFrame.NOCHANGE_STATE, "Initializing chat..." );
		(new ChatRequest( client )).run();

		setVisible( true );
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
		if ( contact == null && mainChatBuffer == null )
			return;

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

		if ( contact == null && mainChatBuffer == null && instantMessageBuffers.size() == 0 )
			client.deinitializeChat();
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
	{	(new ResetVisibilityState( isVisible )).run();
	}

	/**
	 * Runnable to ensure that the visibility is reset only
	 * inside of the Swing thread.
	 */

	private class ResetVisibilityState implements Runnable
	{
		private boolean isVisible;

		public ResetVisibilityState( boolean isVisible )
		{	this.isVisible = true;
		}

		public void run()
		{
			if ( !SwingUtilities.isEventDispatchThread() )
			{
				SwingUtilities.invokeLater( this );
				return;
			}

			if ( mainChatFrame != null )
				mainChatFrame.setVisible( isVisible );

			Iterator frames = instantMessageFrames.values().iterator();
			while ( frames.hasNext() )
				((ChatFrame) frames.next()).setVisible( isVisible );

			if ( isVisible && !onlineContacts.isEmpty() )
			{
				contactsFrame.setSize( new Dimension( 150, 500 ) );
				contactsFrame.setVisible( true );
			}
			else
				contactsFrame.setVisible( false );
		}
	}

	/**
	 * Notifies the messenger that the contact list was closed.  In order
	 * to make sure that the GUI doesn't last forever, this is responsible
	 * for understanding that the contact list has been removed and that
	 * a new invisible frame should be created.
	 */

	public void notifyContactListClosed()
	{	contactsFrame = new ContactListFrame( client, onlineContacts );
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public void dispose()
	{
		removeChat( null );
		while ( !instantMessageFrames.isEmpty() )
			removeChat( (String) instantMessageFrames.firstKey() );

		contactsFrame.setVisible( false );
		contactsFrame.dispose();
	}

	/**
	 * Requests forcus for the messenger's primary window.
	 */

	public void requestFocus()
	{
		if ( mainChatFrame != null )
			mainChatFrame.requestFocus();
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

		if ( !onlineContacts.isEmpty() )
		{
			contactsFrame.setSize( new Dimension( 150, 500 ) );
			contactsFrame.setVisible( true );
		}
		else
			contactsFrame.setVisible( false );
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

		if ( !onlineContacts.isEmpty() )
		{
			contactsFrame.setSize( new Dimension( 150, 500 ) );
			contactsFrame.setVisible( true );
		}
		else
			contactsFrame.setVisible( false );
	}

	/**
	 * Updates the chat with the given information.  This method will
	 * also handle instant message data.
	 *
	 * @param	originalContent	The content with which to update the chat
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

		// Process each line individually.

		String [] lines = noLinksContent.split( "<br>" );
		for ( int i = 0; i < lines.length; ++i )
			processChatMessage( lines[i] );

		// Now, extract the contact list and update KoLMessenger to indicate
		// the contact list found in the last /friends update

		Matcher contactListMatcher = Pattern.compile( "<table>.*?</table>" ).matcher( originalContent );
		int lastFindIndex = 0;  boolean addedHelp = false;
		while ( contactListMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = contactListMatcher.end();
			String result = contactListMatcher.group();

			// Ignore the help information, which gets spit out whenever you
			// type /? when looking for the contact list - on the other hand,
			// you can opt to append the result to the window itself.

			if ( !Pattern.compile( "[^<]/" ).matcher( result ).find() )
			{
				StringTokenizer parsedContactList = new StringTokenizer( result.replaceAll( "<.*?>", "\n" ), "\n" );
				parsedContactList.nextToken();

				List newContactList = new ArrayList();
				while ( parsedContactList.hasMoreTokens() )
				{
					newContactList.add( parsedContactList.nextToken() );

					// The name is usually followed by a comma; the comma is skipped
					// so that you don't have commas appearing in the contact list

					if ( parsedContactList.hasMoreTokens() )
						parsedContactList.nextToken();
				}
				updateContactList( newContactList );
			}
			else
			{
				mainChatBuffer.append( result.replaceAll( "><", "" ).replaceAll( "<.*?>", "<br>\n" ) );
				addedHelp = true;
			}

			// Add an extra space inbetween the helper information and the
			// subsequent text to make things easier to read.

			if ( addedHelp )
			{
				mainChatBuffer.append( "<br>\n" );
				addedHelp = false;
			}
		}

		// Also extract messages which indicate logon/logoff of players to
		// update the contact list.

		Matcher onlineNoticeMatcher = Pattern.compile( "<font.*?><b>.*</font>" ).matcher( noLinksContent );
		lastFindIndex = 0;
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
		// Empty messages do not need to be processed; therefore,
		// return if one was retrieved.

		if ( message == null || message.trim().length() == 0 )
			return;

		if ( !message.startsWith( "<font color=blue>" ) )
		{
			// The easy case is if it's a normal chat message.
			// Then, it just gets updated to the main chat buffer,
			// provided that the main chat buffer is not null

			if ( mainChatBuffer != null )
				mainChatBuffer.append( message.trim() + "<br>\n" );
		}
		else
		{
			// The harder case is if you have a private message.
			// First, you have to determine who sent the message;
			// either the client is the recipient or was the sender.
			// This is determined by where the colon is - in a
			// send, it is not bolded, while in a receive, it is.

			boolean isRecipient = message.indexOf( "</b>:" ) != -1;

			// Next, split the message around the tags so you know
			// how to display the message.

			StringTokenizer splitMessage = new StringTokenizer( message.trim().replaceAll( "<.*?>", "\n" ), "\n" );
			StringBuffer redoneMessage = new StringBuffer();

			// In traditional instant message style, your name
			// appears in red, and the other person in blue.

			String contactName;

			if ( isRecipient )
			{
				contactName = splitMessage.nextToken().substring( 11 );
				redoneMessage.append( "<font color=blue><b>" );
				redoneMessage.append( client.getLoginName() );
				redoneMessage.append( "</b></font>" );
			}
			else
			{
				String firstToken = splitMessage.nextToken();
				contactName = firstToken.substring( 0, firstToken.length() - 11 );
				redoneMessage.append( "<font color=red><b>" );
				redoneMessage.append( contactName );
				redoneMessage.append( "</b></font>: " );
			}

			redoneMessage.append( splitMessage.nextToken() );
			redoneMessage.append( "<br>\n" );

			// For now, just display the message inside of the
			// main frame.

			ChatBuffer messageBuffer = getChatBuffer( contactName );
			if ( messageBuffer == null )
			{
				openInstantMessage( contactName );
				messageBuffer = getChatBuffer( contactName );
			}

			messageBuffer.append( redoneMessage.toString() );
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
		ChatBuffer newBuffer = new ChatBuffer( client.getLoginName() + " (Conversation with " + characterName + ") : Started " +
			Calendar.getInstance().getTime().toString() );

		ChatFrame newFrame = new ChatFrame( client, this, characterName );
		newFrame.setVisible( true );

		newBuffer.setChatDisplay( newFrame.getChatDisplay() );
		instantMessageFrames.put( characterName, newFrame );
		instantMessageBuffers.put( characterName, newBuffer );
	}
}