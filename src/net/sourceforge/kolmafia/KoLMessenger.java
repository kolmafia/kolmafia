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
import java.net.URLEncoder;

import java.awt.Dimension;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.java.dev.spellcast.utilities.SortedListModel;

public class KoLMessenger
{
	private static final String MESSENGER_STYLE = "0";
	private static final String TRIVIA_STYLE = "1";

	private KoLmafia client;
	private ContactListFrame contactsFrame;

	private TreeMap instantMessageFrames;
	private TreeMap instantMessageBuffers;

	private TreeMap seenPlayerIDs;
	private TreeMap seenPlayerNames;
	private SortedListModel onlineContacts;

	private String currentChannel;
	private static final int MAXIMUM_CHATSIZE = 30000;

	public KoLMessenger( KoLmafia client )
	{
		this.client = client;
		this.onlineContacts = new SortedListModel();

		this.instantMessageFrames = new TreeMap();
		this.instantMessageBuffers = new TreeMap();

		seenPlayerIDs = new TreeMap();
		seenPlayerNames = new TreeMap();
		contactsFrame = new ContactListFrame( client, onlineContacts );
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

		activeFrame.updateDisplay( KoLFrame.NOCHANGE_STATE, "Initializing chat..." );
		(new ChatRequest( client, null, "/channel" )).run();

		activeFrame.updateDisplay( KoLFrame.NOCHANGE_STATE, "Starting chat..." );
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
	 * Returns the name of the currently active frame.  This is
	 * used to ensure that messages either (a) get delivered to
	 * the currently active frame, or (b) ensure that focus is
	 * returned to the currently active frame at a later time.
	 */

	public String getNameOfActiveFrame()
	{
		Iterator names = instantMessageBuffers.keySet().iterator();
		String currentName;
		while ( names.hasNext() )
		{
			currentName = (String) names.next();
			if ( ((ChatFrame)instantMessageFrames.get( currentName )).hasFocus() )
				return currentName;
		}

		return currentChannel;
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
		String chatStyle = client.getSettings().getProperty( "chatStyle" );

		if ( contact == null )
			return (ChatBuffer) instantMessageBuffers.get( currentChannel );

		else if ( chatStyle == null )
			return (ChatBuffer) instantMessageBuffers.get( contact );

		else if ( chatStyle.equals( TRIVIA_STYLE ) && !contact.startsWith( "/" ) )
			return (ChatBuffer) instantMessageBuffers.get( "[nsipms]" );

		else
			return (ChatBuffer) instantMessageBuffers.get( contact );
	}

	/**
	 * Removes the chat associated with the given contact.  This
	 * method is called whenever a window is closed.
	 */

	public void removeChat( String contact )
	{
		if ( contact == null )
			return;

		ChatFrame frameToRemove;
		ChatBuffer bufferToRemove;

		frameToRemove = (ChatFrame) instantMessageFrames.remove( contact );
		bufferToRemove = (ChatBuffer) instantMessageBuffers.remove( contact );

		if ( frameToRemove != null && frameToRemove.isShowing() )
			frameToRemove.setVisible( false );

		if ( contact.startsWith( "/" ) && currentChannel != null && !contact.equals( currentChannel ) &&
			frameToRemove != null &&!frameToRemove.getTitle().endsWith( "(inactive)" ) )
				(new ChatRequest( client, contact, "/listen " + contact.substring(1) )).run();

		if ( frameToRemove != null )
			frameToRemove.dispose();

		if ( bufferToRemove != null )
			bufferToRemove.closeActiveLogFile();

		if ( contact.equals( currentChannel ) )
		{
			currentChannel = null;
			client.deinitializeChat();
		}
	}

	/**
	 * Returns whether or not the messenger is showing on screen.
	 * @return	<code>true</code> if the messenger is showing.
	 */

	public boolean isShowing()
	{	return instantMessageBuffers.size() == 0;
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
		while ( !instantMessageFrames.isEmpty() )
			removeChat( (String) instantMessageFrames.firstKey() );

		contactsFrame.setVisible( false );
		contactsFrame.dispose();
	}

	/**
	 * Returns the string form of the player ID associated
	 * with the given player name.
	 *
	 * @param	playerID	The ID of the player
	 * @return	The player's name if it has been seen, or null if it has not
	 *          yet appeared in the chat (not likely, but possible).
	 */

	public String getPlayerName( String playerID )
	{	return (String) seenPlayerNames.get( playerID );
	}

	/**
	 * Returns the string form of the player ID associated
	 * with the given player name.
	 *
	 * @param	playerName	The name of the player
	 * @return	The player's ID if the player has been seen, or the player's name
	 *			with spaces replaced with underscores and other elements encoded
	 *			if the player's ID has not been seen.
	 */

	public String getPlayerID( String playerName )
	{
		if ( playerName == null )
			return null;

		String playerID = (String) seenPlayerIDs.get( playerName );

		try
		{
			return playerID != null ? playerID :
				URLEncoder.encode( playerName.replaceAll( " ", "_" ), "UTF-8" );
		}
		catch ( java.io.UnsupportedEncodingException e )
		{
			// UTF-8 is a very generic encoding scheme; this
			// exception should never be thrown.  But if it
			// is, just ignore it for now.  Better exception
			// handling when it becomes necessary.

			return null;
		}
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
		// First, retrieve the currently active window - that way, the
		// focus can be returned once the chat's been updated.

		String nameOfActiveFrame = getNameOfActiveFrame();

		// There's a lot of bad HTML used in KoL chat; in order to get Java
		// to properly display everything, all of the bad HTML gets replaced
		// with good HTML.

		// First, because linking is not currently handled, all link tags are
		// also removed.

		String noLinksContent = originalContent.replaceAll( "</?a.*?>", "" );

		// Also, there's a problem with bold and italic tag ordering, as well
		// as bold and font-color ordering.  This needs to be fixed, since
		// the default HTML handler in Java is really rigid about it.  Note
		// that this requires several lines - this just shows you how far
		// behind the default HTML handler is compared to a web browser.

		String orderedTagsContent = noLinksContent.replaceAll( "<b><i>", "<i><b>" ).replaceAll(
			"<b><font color=green>", "<font color=green><b>" ).replaceAll( "</font></b>", "</b></font>" ).replaceAll(
				"</?br></b></font>", "</b></font><br>" ).replaceAll( "</?br></font>", "</font><br>" ).replaceAll( "<b><b>", "" );

		// Also, there is no such thing as "none" color - though this works in
		// Java 1.4.2 and the latest update of 1.5.0, it shouldn't be here anyway,
		// as it's not a real color.

		String validColorsContent = orderedTagsContent.replaceAll( "<font color=\"none\">", "" );

		// Although it's not necessary, it cleans up the HTML if all of the
		// last seen data is removed.  It makes the buffer smaller (for one),
		// and it gives room to other chat messages, since the buffer is
		// limited in size to begin with.  Also replace the initial text
		// that gives you a description of your channel.

		String noCommentsContent = validColorsContent.replaceAll( "<p><p>", "</font><br><font color=green>" ).replaceAll( "<!--lastseen:[\\d]+-->", "" );

		// Finally, there's lots of contact list and help file information that
		// needs to get removed - this should be done here.

		String noContactListContent = noCommentsContent.replaceAll( "<table>.*?</table>", "" );

		// Now, extract the contact list and update KoLMessenger to indicate
		// the contact list found in the last /friends update

		Matcher contactListMatcher = Pattern.compile( "<table>.*?</table>" ).matcher( originalContent );

		int lastFindIndex = 0;
		while ( contactListMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = contactListMatcher.end();
			String result = contactListMatcher.group();

			// Ignore the help information, which gets spit out whenever you
			// type /? when looking for the contact list - on the other hand,
			// you can opt to append the result to the window itself.  Also
			// dodge the list that resulted from /who.

			boolean addingHelp = Pattern.compile( "[^<]/" ).matcher( result ).find();
			if ( !addingHelp && result.indexOf( "Contacts Online:" ) != -1 )
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
				result = result.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" ).trim();

				// If the user has clicked into a menu, then there's a chance that
				// the active frame will not be recognized - therefore, simply
				// put the messages into the current channel instead.

				if ( nameOfActiveFrame == null )
					nameOfActiveFrame = currentChannel;

				ChatBuffer currentChatBuffer = getChatBuffer( nameOfActiveFrame );

				// This error should not happen, but it's better to be safe than
				// sorry, so there's a check to make sure that the chat buffer
				// exists before doing anything with the messages.

				if ( currentChatBuffer == null )
				{
					openInstantMessage( nameOfActiveFrame );
					currentChatBuffer = getChatBuffer( nameOfActiveFrame );
				}

				if ( result.startsWith( "Players" ) )
				{
					result = result.replaceAll( "<br>", " " ).replaceAll( " , ", ", " ).replaceFirst( ":", ":</b>" ).trim();

					currentChatBuffer.append( "<font color=teal><b>" );
					currentChatBuffer.append( result.replaceAll( "\n", " " ).replaceAll( " , ", ", " ).replaceFirst( ":", ":</b>" ).trim() );
					currentChatBuffer.append( "</font><br>\n" );
				}
				else
				{
					if ( addingHelp )
					{
						currentChatBuffer.append( "<font color=orange>" );
						currentChatBuffer.append( result.replaceAll( "\n", "<br>" ) );
						currentChatBuffer.append( "</font><br>\n" );
					}
					else
					{
						currentChatBuffer.append( "<font color=teal><b>" );
						currentChatBuffer.append( result.replaceAll( "\n", " " ).replaceAll( " , ", ", " ).replaceFirst( ":", ":</b>" ).trim() );
						currentChatBuffer.append( "</font><br>\n" );
					}
				}
			}
		}

		// Extract player IDs for all players who have spoken in chat, or
		// those that were listed on a /friends request.

		Matcher playerIDMatcher = Pattern.compile( "<a [^>]*?showplayer.php.*?>.*?</a>" ).matcher( originalContent );
		lastFindIndex = 0;
		while( playerIDMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = playerIDMatcher.end();
			StringTokenizer parsedLink = new StringTokenizer( playerIDMatcher.group(), "<>=\'\"" );
			parsedLink.nextToken();  parsedLink.nextToken();
			String playerID = parsedLink.nextToken();
			String playerName = parsedLink.nextToken();
			seenPlayerIDs.put( playerName, playerID );
			seenPlayerNames.put( playerID, playerName );
		}

		// Also extract messages which indicate logon/logoff of players to
		// update the contact list.

		Matcher onlineNoticeMatcher = Pattern.compile( "<font color=green><b>.*?</b></font> logged on.</font>" ).matcher( noContactListContent );
		lastFindIndex = 0;
		while ( onlineNoticeMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = onlineNoticeMatcher.end();
			String [] onlineNotice = onlineNoticeMatcher.group().split( "<.*?>" );
			updateContactList( onlineNotice[2], true );
		}

		Matcher offlineNoticeMatcher = Pattern.compile( "<font color=green><b>.*?</b></font> logged off.</font>" ).matcher( noContactListContent );
		lastFindIndex = 0;
		while ( offlineNoticeMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = offlineNoticeMatcher.end();
			String [] offlineNotice = offlineNoticeMatcher.group().split( "<.*?>" );
			updateContactList( offlineNotice[2], false );
		}

		// Now with all that information parsed, you can properly deal
		// with all of the actual chat messages! :D  Process each line
		// individually, keeping in mind that all the green messages
		// should be processed first after you check to make sure that
		// an exit command was not issued.

		String [] lines = noContactListContent.split( "</?br>" );

		for ( int i = 0; i < lines.length; ++i )
		{
			lines[i] = lines[i].trim();
			if ( lines[i].startsWith( "<img" ) && currentChannel != null )
			{
				removeChat( currentChannel );
				return;
			}

			// Also, while parsing through the messages, fix the HTML
			// on /whois notices.

			if ( lines[i].indexOf( "</b>, the Level" ) != -1 )
				lines[i] += "</font>";

			if ( lines[i].startsWith( "</font>" ) )
				lines[i] = lines[i].substring( 7 );
		}

		// Now begin parsing the green messages, being sure
		// to disable later parsing from parsing it again.

		for ( int i = 0; i < lines.length; ++i )
		{
			if ( lines[i].indexOf( "<font color=green>") != -1 )
			{
				processChatMessage( lines[i] );
				lines[i] = null;
			}
			else if ( lines[i].equals( "</b></font>" ) )
				lines[i] = null;
		}

		// Now, parse the non-green messages and display them
		// to the appropropriate frame.

		for ( int i = 0; i < lines.length; ++i )
			processChatMessage( lines[i] );

		// Now that all the messages have been processed, return
		// the focus to the originally active window (if the window
		// lost focus during any of this).

		if ( nameOfActiveFrame != null )
		{
			ChatFrame activeFrame = (ChatFrame) instantMessageFrames.get( nameOfActiveFrame );
			if ( activeFrame != null && !activeFrame.hasFocus() )
				activeFrame.requestFocus();
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

		String noLinksContent = message.replaceAll( "</?a.*?>", "" );

		// If the message is coming from a listen channel, you
		// need to place it in that channel.  Otherwise, place
		// it in the current channel.

		if ( noLinksContent.startsWith( "<font color=green>[" ) )
		{
			String channel = "/" + noLinksContent.substring( 19, noLinksContent.indexOf( "]" ) );

			int startIndex = noLinksContent.indexOf( "<i>" );
			if ( startIndex == -1 )
				startIndex = noLinksContent.indexOf( "<b>" );

			processChannelMessage( channel, null );
			processChannelMessage( channel, noLinksContent.substring( startIndex ) );
		}
		else if ( noLinksContent.startsWith( "<font color=red><font color=green>[" ) )
		{
			String channel = "/" + noLinksContent.substring( 35, noLinksContent.indexOf( "]" ) );

			int startIndex = noLinksContent.indexOf( "<i>" );
			if ( startIndex == -1 )
				startIndex = noLinksContent.indexOf( "<b>" );

			processChannelMessage( channel, null );
			processChannelMessage( channel, noLinksContent.substring( startIndex ).replaceFirst( "</font>", "" ) );
		}
		else if ( noLinksContent.startsWith( "<font color=green>No longer listening to channel: " ) )
		{
			String channel = "/" + noLinksContent.substring( 50, noLinksContent.indexOf( "</font>" ) );
			processChannelMessage( channel, noLinksContent );
			ChatFrame frame = (ChatFrame) instantMessageFrames.get( channel );
			if ( frame != null )
				frame.setTitle( "KoLmafia Chat: " + channel + " (inactive)" );
		}
		else if ( noLinksContent.startsWith( "<font color=green>Now listening to channel: " ) )
		{
			String channel = "/" + noLinksContent.substring( 44, noLinksContent.indexOf( "</font>" ) );
			processChannelMessage( channel, noLinksContent );
			((ChatFrame) instantMessageFrames.get( channel )).setTitle( "KoLmafia Chat: " + channel + " (listening)" );
		}
		else if ( noLinksContent.startsWith( "<font color=green>You are now talking in channel: " ) )
		{
			// You should notify the channel you're switching away from
			// that you're no longer listening to it.

			if ( currentChannel != null )
			{
				ChatBuffer currentChatBuffer = getChatBuffer( currentChannel );
				if ( currentChatBuffer != null )
				{
					currentChatBuffer.append( "<font color=green>No longer talking in channel: " );
					currentChatBuffer.append( currentChannel.substring(1) );
					currentChatBuffer.append( "." );
					currentChatBuffer.append( "</font><br>\n" );
					((ChatFrame) instantMessageFrames.get( currentChannel )).setTitle( "KoLmafia Chat: " + currentChannel + " (inactive)" );
				}
			}

			currentChannel = "/" + noLinksContent.substring( 50, noLinksContent.indexOf( "</font>" ) - 1 );
			processChannelMessage( currentChannel, noLinksContent );

			ChatFrame currentFrame = (ChatFrame) instantMessageFrames.get( currentChannel );
			currentFrame.setTitle( "KoLmafia Chat: " + currentChannel + " (talking)" );

			if ( !currentFrame.hasFocus() )
				currentFrame.requestFocus();
		}
		else if ( message.indexOf( "<font color=blue>" ) == -1 || noLinksContent.startsWith( "<font color=green>" ) ||
			(message.indexOf( "<b>from " ) != -1 && message.indexOf( "(private)</b>:" ) == -1) )
		{
			// The easy case is if it's a normal chat message.
			// Then, it just gets updated to the main chat buffer,
			// provided that the main chat buffer is not null

			processChannelMessage( currentChannel, noLinksContent );
		}
		else
		{
			// The harder case is if you have a private message.
			// First, you have to determine who sent the message;
			// either the client is the recipient or was the sender.
			// This is determined by where the colon is - in a
			// send, it is not bolded, while in a receive, it is.

			boolean isRecipient = message.indexOf( ":</b>" ) != -1;

			// Next, split the message around the tags so you know
			// how to display the message.

			StringTokenizer splitMessage = new StringTokenizer( message.trim().replaceAll( "<.*?>", "\n" ), "\n" );
			StringBuffer redoneMessage = new StringBuffer();

			// In traditional instant message style, your name
			// appears in red, and the other person in blue.

			String contactName;

			if ( isRecipient )
			{
				String firstToken = splitMessage.nextToken();
				contactName = firstToken.substring( 0, firstToken.length() - 11 );
				redoneMessage.append( "<font color=blue><b>" );
				redoneMessage.append( contactName );
				redoneMessage.append( "</b></font>: " );
			}
			else
			{
				contactName = splitMessage.nextToken().substring( 11 );
				redoneMessage.append( "<font color=red><b>" );
				redoneMessage.append( client.getLoginName() );
				redoneMessage.append( "</b></font>" );
			}

			redoneMessage.append( splitMessage.nextToken() );
			redoneMessage.append( "<br>\n" );

			// Display the message in the appropriate chat
			// buffer, based on who the contact is.

			ChatBuffer messageBuffer = getChatBuffer( contactName );
			if ( messageBuffer == null )
			{
				openInstantMessage( contactName );
				messageBuffer = getChatBuffer( contactName );
			}

			if ( messageBuffer != null )
				messageBuffer.append( redoneMessage.toString() );
		}
	}

	/**
	 * Private method for handling individual channel methods.
	 * @param	channel	The name of the channel
	 * @param	message	The message that was sent to the channel
	 */

	private void processChannelMessage( String channel, String message )
	{
		ChatBuffer channelBuffer = getChatBuffer( channel );

		// If a channel buffer does not exist, create a new window handling
		// the channel content.  This can be done by opening an "instant message"
		// window for the appropriate channel.

		if ( channelBuffer == null && (message == null || !message.startsWith( "<font color=green>No longer" )) )
		{
			openInstantMessage( channel );
			channelBuffer = getChatBuffer( channel );

			// Make sure that the current channel doesn't lose focus by opening the
			// instant message.  This can be accomplished by re-requesting focus.

			((ChatFrame)instantMessageFrames.get( currentChannel )).requestFocus();

			if ( message == null )
			{
				channelBuffer.append( "<font color=green>You are listening to channel: " );
				channelBuffer.append( channel.substring(1) );
				channelBuffer.append( "</font><br>\n" );
				((ChatFrame) instantMessageFrames.get( channel )).setTitle( "KoLmafia Chat: " + channel + " (listening)" );
			}
		}

		if ( message != null && channelBuffer != null )
		{
			String actualMessage = message.trim();
			Matcher nameMatcher = Pattern.compile( "<b>.*?</b>" ).matcher( message );
			if ( nameMatcher.find() )
			{
				String name = nameMatcher.group();
				name = name.substring( 3, name.indexOf( "</b>" ) );

				actualMessage = actualMessage.replaceFirst( "</b>", "</a></b>" ).replaceFirst( "<b>",
					"<b><a style=\"color:black; text-decoration:none;\" href=\"" + name + "\">" );
			}

			channelBuffer.append( actualMessage );
			channelBuffer.append( "<br>\n" );
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
		String chatStyle = client.getSettings().getProperty( "chatStyle" );
		String windowName;

		if ( characterName == null )
			windowName = currentChannel;

		else if ( chatStyle == null )
			windowName = characterName;

		else if ( chatStyle.equals( TRIVIA_STYLE ) && !characterName.startsWith( "/" ) )
			windowName = "[nsipms]";

		else
			windowName = characterName;


		ChatBuffer newBuffer = new LimitedSizeChatBuffer( client.getLoginName() + ": " + windowName + " - Started " +
			Calendar.getInstance().getTime().toString(), MAXIMUM_CHATSIZE );

		ChatFrame newFrame = new ChatFrame( client, this, windowName );
		newFrame.setVisible( true );

		newBuffer.setChatDisplay( newFrame.getChatDisplay() );
		instantMessageFrames.put( windowName, newFrame );
		instantMessageBuffers.put( windowName, newBuffer );
	}
}