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

public class KoLMessenger implements KoLConstants
{
	private static final String MESSENGER_STYLE = "0";
	private static final String TRIVIA_STYLE = "1";

	private KoLmafia client;
	private ContactListFrame contactsFrame;

	private TreeMap instantMessageFrames;
	private TreeMap instantMessageBuffers;
	private SortedListModel onlineContacts;

	private String currentChannel;
	private boolean useTabbedFrame;
	private TabbedChatFrame tabbedFrame;

	public KoLMessenger( KoLmafia client )
	{
		this.client = client;
		this.onlineContacts = new SortedListModel();

		this.instantMessageFrames = new TreeMap();
		this.instantMessageBuffers = new TreeMap();

		contactsFrame = new ContactListFrame( client, onlineContacts );

		String tabsSetting = client.getSettings().getProperty( "useTabbedChat" );
		setTabbedFrameSetting( tabsSetting == null || tabsSetting.equals( "1" ) );
		LimitedSizeChatBuffer.setChatColors( client.getSettings().getProperty( "chatNameColors" ) );
	}

	/**
	 * Notifies the messenger that you should be able to use
	 * tabbed chat (or undo it) by consolidating existing
	 * frames into a single frame or splitting it, as desired.
	 */

	public void setTabbedFrameSetting( boolean useTabbedFrame )
	{
		if ( this.useTabbedFrame != useTabbedFrame )
		{
			Iterator keyIterator = instantMessageBuffers.keySet().iterator();
			String currentKey;  ChatBuffer currentBuffer;  ChatFrame currentFrame;

			if ( useTabbedFrame )
			{
				this.tabbedFrame = new TabbedChatFrame( client, this );
				this.tabbedFrame.setVisible( true );
				if ( currentChannel != null )
					this.tabbedFrame.setTitle( "KoLmafia Chat: You are talking in " + currentChannel );
			}
			else
			{
				this.tabbedFrame.dispose();
				this.tabbedFrame = null;
			}


			while ( keyIterator.hasNext() )
			{
				currentKey = (String) keyIterator.next();
				currentBuffer = (ChatBuffer) instantMessageBuffers.get( currentKey );
				currentFrame = (ChatFrame) instantMessageFrames.get( currentKey );

				if ( useTabbedFrame )
				{
					ChatFrame.ChatPanel panel = tabbedFrame.addTab( currentKey );
					currentBuffer.setChatDisplay( panel.getChatDisplay() );
					currentBuffer.setScrollPane( panel.getScrollPane() );
					currentFrame.setVisible( false );
				}
				else
				{
					currentBuffer.setChatDisplay( currentFrame.getChatDisplay() );
					currentBuffer.setScrollPane( currentFrame.getScrollPane() );
					currentFrame.setVisible( true );
				}

			}

			this.useTabbedFrame = useTabbedFrame;
		}
	}

	/**
	 * Initializes the chat buffer with the provided chat pane.
	 * Note that the chat refresher will also be initialized
	 * by calling this method; to stop the chat refresher, call
	 * the <code>deinitializeChat()</code> method.
	 */

	public void initialize()
	{
		client.updateDisplay( DISABLED_STATE, "Initializing chat..." );
		(new ChatRequest( client, null, "/channel" )).run();

		client.updateDisplay( DISABLED_STATE, "Starting chat..." );
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
		String currentName;  ChatFrame currentFrame;
		while ( names.hasNext() )
		{
			currentName = (String) names.next();
			currentFrame = (ChatFrame) instantMessageFrames.get( currentName );
			if ( currentFrame.isShowing() && currentFrame.hasFocus() )
				return currentName;
		}

		return null;
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

		if ( frameToRemove != null )
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
			client.deinitializeChat();
			currentChannel = null;
			(new ChatRequest( client, currentChannel, "/exit" )).run();
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

			if ( !useTabbedFrame )
			{
				Iterator frames = instantMessageFrames.values().iterator();
				ChatFrame currentFrame;
				while ( frames.hasNext() )
				{
					currentFrame = (ChatFrame) frames.next();
					currentFrame.setVisible( isVisible );
				}
			}
		}
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public void dispose()
	{
		while ( !instantMessageFrames.isEmpty() )
			removeChat( (String) instantMessageFrames.firstKey() );

		if ( contactsFrame != null )
		{
			contactsFrame.setVisible( false );
			contactsFrame.dispose();
		}

		if ( tabbedFrame != null )
		{
			tabbedFrame.setVisible( false );
			tabbedFrame.dispose();
		}
	}

	/**
	 * Replaces the current contact list with the given contact
	 * list.  This is used after every call to /friends.
	 *
	 * @param	currentContacts	A list of the contacts currently online.
	 */

	private void updateContactList( String contactListType, List currentContacts )
	{
		onlineContacts.clear();
		onlineContacts.addAll( currentContacts );

		if ( !contactsFrame.isShowing() )
		{
			contactsFrame.setSize( 200, 500 );
			contactsFrame.setVisible( true );
		}

		contactsFrame.setTitle( contactListType );
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

		// Also, there's a problem with bold and italic tag ordering, as well
		// as bold and font-color ordering.  This needs to be fixed, since
		// the default HTML handler in Java is really rigid about it.  Note
		// that this requires several lines - this just shows you how far
		// behind the default HTML handler is compared to a web browser.

		String orderedTagsContent = originalContent.replaceAll( "<br>&nbsp;&nbsp;", "" ).replaceAll( "<b><i>", "<i><b>" ).replaceAll(
			"<b><font color=green>", "<font color=green><b>" ).replaceAll( "<b><font color=.*?>", "<b>" ).replaceAll(
				"</font></b>", "</b></font>" ).replaceAll( "</b></font></a>", "</b></a>" ).replaceAll( "</?br></b>", "</b><br>" ).replaceAll(
					"</?br></font>", "</font><br>" ).replaceAll( "<b><b>", "" ).replaceAll( "</b></a>", "</a></b>" );

		if ( orderedTagsContent.startsWith( "</font>" ) )
			orderedTagsContent = orderedTagsContent.substring( 7 );

		// Also, there is no such thing as "none" color - though this works in
		// Java 1.4.2 and the latest update of 1.5.0, it shouldn't be here anyway,
		// as it's not a real color.  Also, color bleeding fixes which can't be
		// fixed any other way will be fixed here.

		String validColorsContent = orderedTagsContent.replaceAll( "<font color=\"none\">", "" ).replaceAll(
			"<font color=blue>\\[link\\]<\\/font>", "[link]" );

		// Although it's not necessary, it cleans up the HTML if all of the
		// last seen data is removed.  It makes the buffer smaller (for one),
		// and it gives room to other chat messages, since the buffer is
		// limited in size to begin with.  Also replace the initial text
		// that gives you a description of your channel.

		String noCommentsContent = validColorsContent.replaceAll( "<p><p>", "</font><br><font color=green>" ).replaceAll(
			"<p></p><p>", "<br>" ).replaceAll( "</p>", "" ).replaceAll( "<!--lastseen:[\\d]+-->", "" );

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

			if ( Pattern.compile( "[^<]/" ).matcher( result ).find() )
			{
				result = result.replaceAll( "<.*?>", "\n" ).replaceAll( "[\\n]+", "\n" ).trim();

				// If the user has clicked into a menu, then there's a chance that
				// the active frame will not be recognized - therefore, simply
				// put the messages into the current channel instead.

				String updateChannel = nameOfActiveFrame == null ? currentChannel : nameOfActiveFrame;

				ChatBuffer currentChatBuffer = getChatBuffer( updateChannel );

				// This error should not happen, but it's better to be safe than
				// sorry, so there's a check to make sure that the chat buffer
				// exists before doing anything with the messages.

				if ( currentChatBuffer == null )
				{
					openInstantMessage( updateChannel );
					currentChatBuffer = getChatBuffer( updateChannel );
				}

				StringTokenizer helpString = new StringTokenizer( result, "\n" );
				String currentToken;

				currentChatBuffer.append( "<font color=purple>" );
				while ( helpString.hasMoreTokens() )
				{
					currentToken = helpString.nextToken();
					if ( currentToken.startsWith( "/" ) )
					{
						currentChatBuffer.append( "</font><br><font color=purple><b>" );
						currentChatBuffer.append( currentToken );
						currentChatBuffer.append( "</b>" );
					}
					else if ( !currentToken.endsWith( ":" ) && helpString.hasMoreTokens() )
						currentChatBuffer.append( currentToken );
				}

				currentChatBuffer.append( "</font><br><br>" );
				currentChatBuffer.append( System.getProperty( "line.separator" ) );
			}
			else
			{
				StringTokenizer parsedContactList = new StringTokenizer( result.replaceAll( "<.*?>", "\n" ), "\n" );
				String contactListType = parsedContactList.nextToken();

				List newContactList = new ArrayList();
				while ( parsedContactList.hasMoreTokens() )
				{
					newContactList.add( parsedContactList.nextToken() );

					// The name is usually followed by a comma; the comma is skipped
					// so that you don't have commas appearing in the contact list

					if ( parsedContactList.hasMoreTokens() )
						parsedContactList.nextToken();
				}
				updateContactList( contactListType, newContactList );
			}
		}

		// Extract player IDs for all players who have spoken in chat, or
		// those that were listed on a /friends request.

		Matcher playerIDMatcher = Pattern.compile( "showplayer.php\\?who\\=(\\d+)[\'\"]>(.*?)</a>" ).matcher( originalContent );
		lastFindIndex = 0;
		while( playerIDMatcher.find( lastFindIndex ) )
		{
			lastFindIndex = playerIDMatcher.end();
			String playerID = playerIDMatcher.group(1);
			String playerName = playerIDMatcher.group(2).replaceAll( "<.*?>", "" ).replaceAll( " \\(.*?\\)", "" );

			client.registerPlayer( playerName, playerID );
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
			// Haiku's introduction starts with several lines,
			// so be sure to string the lines together.

			if ( lines[i].startsWith( "<font color=green>Speak" ) )
			{
				processChatMessage( lines[i] + "</font>" );
				lines[i] = null;
				processChatMessage( "<font color=green>" + lines[++i] + "</font>" );
				lines[i] = null;
				processChatMessage( "<font color=green>" + lines[++i] );
				lines[i] = null;
			}
			else if ( lines[i].startsWith( "<font color=green>" ) && !lines[i].startsWith( "<font color=green>[" ) )
			{
				processChatMessage( lines[i] );
				lines[i] = null;
			}
			else if ( lines[i].equals( "</b></font>" ) )
				lines[i] = null;
		}

		// Process the /last command, since it appears in blue,
		// but then the font colors get stripped.

		for ( int i = 0; i < lines.length; ++i )
		{
			if ( lines[i] != null && lines[i].startsWith( "<font color=blue><b>from " ) && lines[i].indexOf( ":</b>" ) != -1 )
			{
				processChannelMessage( currentChannel, lines[i] + "</font>" );
				lines[i++] = null;
				while ( !lines[i].endsWith( "</font>" ) )
				{
					processChannelMessage( currentChannel, "<font color=blue>" + lines[i] + "</font>" );
					lines[i++] = null;
				}
				processChannelMessage( currentChannel, "<font color=blue>" + lines[i] );
				lines[i++] = null;
			}
		}

		// Now, parse the non-green messages and display them
		// to the appropropriate frame.

		for ( int i = 0; i < lines.length; ++i )
		{
			if ( lines[i] != null && lines[i].startsWith( "<font color=green>[haiku]" ) )
				processChatMessage( lines[i] + "<br>" + lines[++i].replaceAll( "<Br>", "<br>" ) );
			else
				processChatMessage( lines[i] );
		}

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
		try
		{
			// Empty messages do not need to be processed; therefore,
			// return if one was retrieved.

			if ( message == null || message.trim().length() == 0 )
				return;

			String noLinksContent = message.replaceAll( "<a target=mainpane .*?>", "" );

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
				ChatFrame frame = (ChatFrame) instantMessageFrames.get( channel );
				if ( frame != null )
					frame.setTitle( "KoLmafia Chat: " + channel + " (listening)" );
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
						ChatFrame frame = (ChatFrame) instantMessageFrames.get( currentChannel );
						if ( frame != null )
							frame.setTitle( "KoLmafia Chat: " + currentChannel + " (inactive)" );
					}
				}

				currentChannel = "/" + noLinksContent.substring( 50, noLinksContent.indexOf( "</font>" ) - 1 );
				processChannelMessage( currentChannel, noLinksContent );

				ChatFrame currentFrame = (ChatFrame) instantMessageFrames.get( currentChannel );

				if ( !useTabbedFrame && currentFrame != null )
				{
					currentFrame.setTitle( "KoLmafia Chat: " + currentChannel + " (talking)" );
					if ( !currentFrame.hasFocus() )
						currentFrame.requestFocus();
				}

				if ( useTabbedFrame && currentChannel != null )
					tabbedFrame.setTitle( "KoLmafia Chat: You are talking in " + currentChannel );
			}
			else if ( message.indexOf( "<font color=blue>" ) == -1 )
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

				boolean isRecipient = message.indexOf( "<a" ) != -1;

				// Next, split the message around the tags so you know
				// how to display the message.

				StringTokenizer splitMessage = new StringTokenizer( message.trim().replaceAll( "<.*?>", "" ), ":", true );
				StringBuffer redoneMessage = new StringBuffer();

				// In traditional instant message style, your name
				// appears in red, and the other person in blue.

				String contactName;

				if ( isRecipient )
				{
					String firstToken = splitMessage.nextToken();
					contactName = firstToken.substring( 0, firstToken.length() - 10 );
					redoneMessage.append( "<font color=blue><b>" );
					redoneMessage.append( contactName );
					redoneMessage.append( "</b></font>: " );
				}
				else
				{
					contactName = splitMessage.nextToken().substring( 11 );
					redoneMessage.append( "<font color=red><b>" );
					redoneMessage.append( client.getLoginName() );
					redoneMessage.append( "</b></font>: " );
				}

				splitMessage.nextToken();

				while ( splitMessage.hasMoreTokens() )
					redoneMessage.append( splitMessage.nextToken() );
				redoneMessage.append( "<br>" );
				redoneMessage.append( System.getProperty( "line.separator" ) );

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
		catch ( Exception e )
		{
			// If an error occurs somewhere in all of this, KoLmafia will
			// stop refreshing.  So, to make things easier, print the
			// error message to the main window. :D

			ChatBuffer messageBuffer = getChatBuffer( currentChannel );
			if ( messageBuffer != null )
			{
				messageBuffer.append( "<br><br><font color=magenta>Unexpected error.</font><br>\n" );
				messageBuffer.append( System.getProperty( "line.separator" ) );
			}
		}
	}

	/**
	 * Private method for handling individual channel methods.
	 * @param	channel	The name of the channel
	 * @param	message	The message that was sent to the channel
	 */

	private void processChannelMessage( String channel, String message )
	{
		try
		{
			ChatBuffer channelBuffer = getChatBuffer( channel );
			if ( useTabbedFrame )
				tabbedFrame.highlightTab( channel );

			// If a channel buffer does not exist, create a new window handling
			// the channel content.  This can be done by opening an "instant message"
			// window for the appropriate channel.

			if ( channelBuffer == null && (message == null || !message.startsWith( "<font color=green>No longer" )) )
			{
				ChatFrame currentFrame = (ChatFrame) instantMessageFrames.get( currentChannel );
				boolean hadFocus = currentFrame != null && currentFrame.hasFocus();

				openInstantMessage( channel );
				channelBuffer = getChatBuffer( channel );

				// Make sure that the current channel doesn't lose focus by opening the
				// instant message.  This can be accomplished by re-requesting focus.

				if ( hadFocus && currentFrame != null )
					currentFrame.requestFocus();

				if ( message == null )
				{
					channelBuffer.append( "<font color=green>You are listening to channel: " );
					channelBuffer.append( channel.substring(1) );
					channelBuffer.append( "</font><br>" );
					channelBuffer.append( System.getProperty( "line.separator" ) );

					ChatFrame newFrame = (ChatFrame) instantMessageFrames.get( channel );
					newFrame.setTitle( "KoLmafia Chat: " + channel + " (listening)" );
				}
			}

			if ( message != null && channelBuffer != null && message.startsWith( "<font color=blue>" ) )
			{
				channelBuffer.append( message );
				channelBuffer.append( "<br>" );
				channelBuffer.append( System.getProperty( "line.separator" ) );
			}
			else if ( message != null && channelBuffer != null )
			{
				String actualMessage = message.trim();

				Matcher nameMatcher = Pattern.compile( "<b>.*?</a>" ).matcher( actualMessage );
				if ( nameMatcher.find() )
				{
					String name = nameMatcher.group();
					name = name.replaceAll( "<.*?>", "" );

					if ( name.indexOf( " (" ) != -1 )
						name = name.substring( 0, name.indexOf( " (" ) );

					int playerID = Integer.parseInt( client.getPlayerID( name ) );

					// In order to make the stylesheet work as intended,
					// the user's player ID is defined with class pid0.

					if ( playerID == client.getUserID() )
						playerID = 0;

					actualMessage = actualMessage.replaceAll( "</font>", "" ).replaceFirst( "<b>",
						"<b class=\"pid" + playerID + "\"><a href=\"" + name + "\">" );
				}

				// Now to replace doubled instances of <font> to 1, and ensure that
				// there's an </font> at the very end.

				int indexRed = actualMessage.indexOf( "<font color=red>" );
				int indexGreen = actualMessage.indexOf( "<font color=green>" );

				actualMessage = actualMessage.replaceAll( "</?font.*?>", "" );

				if ( indexRed != -1 )
					actualMessage = "<font color=red>" + actualMessage.replaceFirst( "<a ", "<a style=\"color:red\"" ) + "</font>";
				else if ( indexGreen != -1 )
					actualMessage = "<font color=green>" + actualMessage.replaceFirst( "<a ", "<a style=\"color:green\"" ) + "</font>";

				channelBuffer.append( actualMessage );
				channelBuffer.append( "<br>" );
				channelBuffer.append( System.getProperty( "line.separator" ) );
			}
		}
		catch ( Exception e )
		{
			// If an error occurs somewhere in all of this, KoLmafia will
			// stop refreshing.  So, to make things easier, print the
			// error message to the main window. :D

			ChatBuffer messageBuffer = getChatBuffer( currentChannel );
			if ( messageBuffer != null )
			{
				messageBuffer.append( "<br><br><font color=magenta>Unexpected error.</font><br>\n" );
				messageBuffer.append( System.getProperty( "line.separator" ) );
			}
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


		// If the window exists, don't open another one as it
		// just confuses the disposal issue

		if ( instantMessageBuffers.containsKey( windowName ) )
			return;

		ChatBuffer newBuffer = new LimitedSizeChatBuffer( client.getLoginName() + ": " + windowName + " - Started " +
			Calendar.getInstance().getTime().toString() );

		ChatFrame newFrame = new ChatFrame( client, this, windowName );

		if ( useTabbedFrame )
		{
			newFrame.setVisible( false );
			ChatFrame.ChatPanel panel = this.tabbedFrame.addTab( windowName );
			newBuffer.setChatDisplay( panel.getChatDisplay() );
			newBuffer.setScrollPane( panel.getScrollPane() );

			if ( currentChannel != null )
				this.tabbedFrame.setTitle( "KoLmafia Chat: You are talking in " + currentChannel );
		}
		else
		{
			newFrame.setVisible( true );
			newBuffer.setChatDisplay( newFrame.getChatDisplay() );
			newBuffer.setScrollPane( newFrame.getScrollPane() );
		}

		instantMessageFrames.put( windowName, newFrame );
		instantMessageBuffers.put( windowName, newBuffer );
	}
}