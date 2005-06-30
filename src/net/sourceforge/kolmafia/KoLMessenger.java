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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.StringTokenizer;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;

public class KoLMessenger implements KoLConstants
{
	private static final Color DEFAULT_HIGHLIGHT = new Color( 128, 0, 128 );

	public static final String [] ROOMS =
	{
		"newbie", "normal", "trade", "clan", "games", "villa", "radio", "pvp", "haiku", "foodcourt", "valhalla"
	};

	private KoLmafia client;
	private ContactListFrame contactsFrame;

	private TreeMap instantMessageFrames;
	private TreeMap instantMessageBuffers;
	private SortedListModel onlineContacts;

	private String currentChannel;

	private int chattingStyle;
	private boolean useTabbedFrame;
	private TabbedChatFrame tabbedFrame;

	private String baseLogname;
	private boolean highlighting;

	public KoLMessenger( KoLmafia client )
	{
		this.client = client;
		this.onlineContacts = new SortedListModel();

		this.instantMessageFrames = new TreeMap();
		this.instantMessageBuffers = new TreeMap();

		chattingStyle = Integer.parseInt( client.getSettings().getProperty( "chatStyle" ) );
		contactsFrame = new ContactListFrame( client, onlineContacts );
		setTabbedFrameSetting( client.getSettings().getProperty( "useTabbedChat" ).equals( "1" ) );

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
			Object [] keys = instantMessageBuffers.keySet().toArray();
			String currentKey;  LimitedSizeChatBuffer currentBuffer;  ChatFrame currentFrame;

			if ( useTabbedFrame )
			{
				this.tabbedFrame = new TabbedChatFrame( client, this );
				this.tabbedFrame.setVisible( true );
			}
			else
			{
				this.tabbedFrame.dispose();
				this.tabbedFrame = null;
			}

			for ( int i = 0; i < keys.length; ++i )
			{
				currentKey = (String) keys[i];
				currentBuffer = getChatBuffer( currentKey );
				currentFrame = getChatFrame( currentKey );

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
		(new RequestThread( new ChatRequest( client, null, "/channel" ) )).start();
		LimitedSizeChatBuffer.clearHighlights();
	}

	/**
	 * Clears the contents of the chat buffer.  This is called
	 * whenever the user wishes for there to be less text.
	 */

	public void clearChatBuffer( String contact )
	{
		LimitedSizeChatBuffer bufferToClear = getChatBuffer( contact );
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
		Object [] names = instantMessageBuffers.keySet().toArray();
		String currentName;  ChatFrame currentFrame;
		for ( int i = 0; i < names.length; ++i )
		{
			currentName = (String) names[i];
			currentFrame = getChatFrame( currentName );
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

	public LimitedSizeChatBuffer getChatBuffer( String contact )
	{
		String neededBufferName = getBufferKey( contact );
		LimitedSizeChatBuffer neededBuffer = (LimitedSizeChatBuffer) instantMessageBuffers.get( neededBufferName );

		// This error should not happen, but it's better to be safe than
		// sorry, so there's a check to make sure that the chat buffer
		// exists before doing anything with the messages.

		if ( neededBuffer == null )
		{
			openInstantMessage( neededBufferName );
			neededBuffer = (LimitedSizeChatBuffer) instantMessageBuffers.get( neededBufferName );
		}

		return neededBuffer;
	}

	/**
	 * Retrieves the chat frame associated with the currently
	 * running chat.  These frames are used to track active
	 * and inactive state for the frames.
	 */

	public ChatFrame getChatFrame( String contact )
	{
		String neededFrameName = getBufferKey( contact );
		return (ChatFrame) instantMessageFrames.get( neededFrameName );
	}

	/**
	 * Sets the title of the frame to the given value, if and
	 * only if the given channel name exists as a part of the
	 * title to the frame.
	 */

	public void setChatFrameTitle( String contact, String title )
	{
		ChatFrame neededFrame = getChatFrame( contact );
		if ( neededFrame == null || neededFrame.getTitle().indexOf( contact ) == -1 )
			return;

		neededFrame.setTitle( title );
	}

	/**
	 * Retrieves the key which will be needed, given the contact
	 * name.  In other words, it translates the contact name to
	 * a key value used by the buffers and frames.
	 */

	private String getBufferKey( String contact )
	{
		return contact == null ? currentChannel : contact.startsWith( "[" ) ? contact :
			chattingStyle != 0 && !contact.startsWith( "/" ) ? "/reply" : chattingStyle == 2 && contact.startsWith( "/" ) ? "[chat]" : contact;
	}

	/**
	 * Removes the chat associated with the given contact.  This
	 * method is called whenever a window is closed.
	 */

	public void removeChat( String contact )
	{
		synchronized ( existingFrames )
		{
			if ( contact == null )
				return;

			ChatFrame removedFrame = (ChatFrame) instantMessageFrames.remove( contact );

			if ( removedFrame == null )
				return;

			LimitedSizeChatBuffer removedBuffer = (LimitedSizeChatBuffer) instantMessageBuffers.remove( contact );

			if ( contact.equals( currentChannel ) )
			{
				(new RequestThread( new ChatRequest( client, currentChannel, "/exit" ) )).start();
				currentChannel = null;
				client.deinitializeChat();
				return;
			}

			if ( currentChannel != null && contact.startsWith( "/" ) && !removedFrame.getTitle().endsWith( "(inactive)" ) )
				(new RequestThread( new ChatRequest( client, contact, "/listen " + contact.substring(1) ) )).start();

			removedFrame.setVisible( false );
			removedFrame.dispose();
			removedBuffer.closeActiveLogFile();
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
	 * list.  This is used after every call to /friends or /who.
	 */

	private void updateContactList( String [] contactList )
	{
		onlineContacts.clear();

		for ( int i = 1; i < contactList.length; ++i )
			if ( contactList[i].indexOf( "(" ) != -1 )
				contactList[i] = contactList[i].substring( 0, contactList[i].indexOf( "(" ) ).trim();

		onlineContacts.add( contactList[0].substring( contactList[0].indexOf( ":" ) + 1 ).toLowerCase() );
		for ( int i = 1; i < contactList.length; ++i )
			onlineContacts.add( contactList[i].toLowerCase() );

		if ( !contactsFrame.isShowing() )
		{
			contactsFrame.setSize( 200, 500 );
			contactsFrame.setVisible( true );
		}

		contactsFrame.setTitle( contactList[0].substring( 0, contactList[0].indexOf( ":" ) ) );
	}

	private static final String getNormalizedContent( String originalContent )
	{
		String condensedContent = originalContent.replaceAll( "(</?p>)+", "<br>" );
		String noColorContent = condensedContent.replaceAll( "</?font.*?>", "" );
		String noItalicsContent = noColorContent.replaceAll( "</?i>", "" );
		String normalBreaksContent = noItalicsContent.replaceAll( "</?[Bb][Rr]>", "<br>" );
		String normalBoldsContent = normalBreaksContent.replaceAll( "<br></b>", "</b><br>" );
		String colonOrderedContent = normalBoldsContent.replaceAll( ":</b></a>", "</b></a>:" ).replaceAll( "</a>:</b>", "</a></b>:" );
		String noCommentsContent = colonOrderedContent.replaceAll( "<!--.*?-->", "" );

		return noCommentsContent.replaceAll( "<table>.*?</table>", "" );
	}

	private void handleTableContent( String content, String nameOfActiveFrame )
	{
		Matcher tableMatcher = Pattern.compile( "<table>.*?</table>" ).matcher( content );

		while ( tableMatcher.find() )
		{
			String result = tableMatcher.group();

			// Ignore the help information, which gets spit out whenever you
			// type /? when looking for the contact list - on the other hand,
			// you can opt to append the result to the window itself.  Also
			// dodge the list that resulted from /who.

			if ( Pattern.compile( "[^<]/" ).matcher( result ).find() )
			{
				result = result.replaceAll( "><", "" ).replaceAll( "<.*?>", "\n" ).trim();

				// If the user has clicked into a menu, then there's a chance that
				// the active frame will not be recognized - therefore, simply
				// put the messages into the current channel instead.

				String updateChannel = nameOfActiveFrame == null ? currentChannel : nameOfActiveFrame;

				LimitedSizeChatBuffer currentChatBuffer = getChatBuffer( updateChannel );
				String [] helpdata = result.split( "\n" );
				StringBuffer dataBuffer = new StringBuffer();

				for ( int i = 0; i < helpdata.length; ++i )
				{
					dataBuffer.append( "<font color=orange>" );
					if ( helpdata[i].startsWith( "/" ) )
					{
						dataBuffer.append( "</font><br><font color=orange><b>" );
						dataBuffer.append( helpdata[i] );
						dataBuffer.append( "</b>" );
					}
					else if ( !helpdata[i].endsWith( ":" ) )
						dataBuffer.append( helpdata[i] );
				}

				dataBuffer.append( "</font><br><br>" );
				currentChatBuffer.append( dataBuffer.toString() );
			}
			else
				updateContactList( result.replaceAll( "><", "" ).replaceAll( "<.*?>", "" ).split( "\\s*,\\s*" ) );
		}
	}

	private void handlePlayerData( String content )
	{
		Matcher playerMatcher = Pattern.compile( "showplayer.php\\?who\\=(\\d+)[\'\"]>(.*?)</a>" ).matcher( content );

		String playerName, playerID;
		while ( playerMatcher.find() )
		{
			playerName = playerMatcher.group(2).replaceAll( "<.*?>", "" ).replaceAll( " \\(.*?\\)", "" ).replaceAll( ":" , "" );
			playerID = playerMatcher.group(1);
			client.registerPlayer( playerName, playerID );
		}
	}

	private void handleChatData( String content )
	{
		// If the exit command was issued, then deinitialize
		// the chat.  Exit can be detected by seeing if there
		// were any image tags in the content.

		if ( content.indexOf( "<img" ) != -1 )
		{
			client.deinitializeChat();
			return;
		}

		// Now that you know that there was no intent to exit
		// chat, go ahead and split up the lines in chat.

		String [] lines = getNormalizedContent( content ).split( "<br>" );

		// First, trim all the lines that were received so
		// that you don't get anything funny-looking, and
		// check to see if there are any messages coming from
		// channel haiku.

		for ( int i = 0; i < lines.length; ++i )
		{
			if ( lines[i].startsWith( "[haiku]" ) )
				processChatMessage( lines[i].trim() + "<br>" + lines[++i].trim() + "<br>" + lines[++i].trim() + "<br>" + lines[++i].trim() );

			else if ( currentChannel != null && currentChannel.equals( "/haiku" ) && lines[i].indexOf( "[" ) == -1 && lines[i].indexOf( ":" ) != -1 )
				processChatMessage( lines[i].trim() + "<br>" + lines[++i].trim() + "<br>" + lines[++i].trim() + "<br>" + lines[++i].trim() );

			else
				processChatMessage( lines[i].trim() );
		}

		// Finally, update the title to the frame in which
		// you are currently talking.

		if ( currentChannel != null )
		{
			if ( useTabbedFrame )
				tabbedFrame.setTitle( "KoLmafia Chat: You are talking in " + currentChannel );
			else
				getChatFrame( currentChannel ).setTitle( "KoLmafia Chat: You are talking in " + currentChannel );
		}
	}

	/**
	 * Updates the chat with the given information.  This method will
	 * also handle instant message data.
	 *
	 * @param	content	The content with which to update the chat
	 */

	public void updateChat( String content )
	{
		// First, retrieve the currently active window - that way, the
		// focus can be returned once the chat's been updated.

		String nameOfActiveFrame = getNameOfActiveFrame();

		// Now, extract the contact list and update KoLMessenger to indicate
		// the contact list found in the last /friends update

		handleTableContent( content, nameOfActiveFrame );

		// Extract player IDs from the most recent chat content, since it
		// can prove useful at a later time.

		handlePlayerData( content );

		// Now, that all the pre-processing is done, go ahead and handle
		// all of the individual chat data.

		handleChatData( content );

		// Now that all the messages have been processed, return
		// the focus to the originally active window (if the window
		// lost focus during any of this).

		if ( nameOfActiveFrame != null )
		{
			ChatFrame activeFrame = getChatFrame( nameOfActiveFrame );
			if ( activeFrame != null && !activeFrame.hasFocus() )
				activeFrame.requestFocus();
		}
	}

	/**
	 * Notifies the chat that the user has stopped talking and
	 * listening to the current channel - this only happens after
	 * the /channel command is used to switch to a different channel.
	 */

	public void stopConversation()
	{
		if ( currentChannel != null )
			setChatFrameTitle( currentChannel, "KoLmafia Chat: " + currentChannel + " (inactive)" );
	}

	/**
	 * Notifies the chat that the user has stopped talking but will
	 * still be listening to the current channel - this only happens
	 * after the /switch command is used to switch to a different channel.
	 */

	public void switchConversation()
	{
		if ( currentChannel != null )
			setChatFrameTitle( currentChannel, "KoLmafia Chat: " + currentChannel + " (listening)" );
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

			if ( message == null || message.length() == 0 )
				return;

			if ( message.startsWith( "[" ) )
			{
				// If the message is coming from a listen channel, you
				// need to place it in that channel.  Otherwise, place
				// it in the current channel.

				int startIndex = message.indexOf( "]" ) + 2;
				String channel = "/" + message.substring( 1, startIndex - 2 );

				processChatMessage( channel, message.substring( startIndex ) );
				setChatFrameTitle( channel, "KoLmafia Chat: " + channel + " (listening)" );
			}
			else if ( message.startsWith( "No longer listening to channel: " ) )
			{
				int startIndex = message.indexOf( ":" ) + 2;
				String channel = "/" + message.substring( startIndex );

				processChatMessage( channel, message );
				setChatFrameTitle( channel, "KoLmafia Chat: " + channel + " (inactive)" );
			}
			else if ( message.startsWith( "Now listening to channel: " ) )
			{
				int startIndex = message.indexOf( ":" ) + 2;
				String channel = "/" + message.substring( startIndex );

				processChatMessage( channel, message );
				setChatFrameTitle( channel, "KoLmafia Chat: " + channel + " (listening)" );
			}
			else if ( message.startsWith( "You are now talking in channel: " ) )
			{
				int startIndex = message.indexOf( ":" ) + 2;
				currentChannel = "/" + message.substring( startIndex ).replaceAll( "\\.", "" );
				processChatMessage( currentChannel, message );
			}
			else if ( message.indexOf( "(private)</b></a>:" ) != -1 )
			{
				String sender = message.substring( 0, message.indexOf( " (" ) ).replaceAll( "<.*?>", "" );
				String cleanHTML = "<a target=mainpane href=\"showplayer.php?who=" + client.getPlayerID( sender ) + "\"><b><font color=blue>" +
					sender + "</font></b></a>" + message.substring( message.indexOf( ":" ) );
				processChatMessage( sender, cleanHTML );
			}
			else if ( message.startsWith( "<b>private to" ) )
			{
				String sender = client.getLoginName();
				String recipient = message.substring( 0, message.indexOf( ":" ) ).replaceAll( "<.*?>", "" ).substring( 11 );

				String cleanHTML = "<a target=mainpane href=\"showplayer.php?who=" + client.getPlayerID( sender ) + "\"><b><font color=red>" +
					sender + "</font></b></a>" + message.substring( message.indexOf( ":" ) );
				processChatMessage( recipient, cleanHTML );
			}
			else
			{
				processChatMessage( currentChannel, message );
			}
		}
		catch ( Exception e )
		{
			// If an error occurs somewhere in all of this, KoLmafia will
			// stop refreshing.  So, to make things easier, print the
			// error message to the main window. :D

			e.printStackTrace( client.getLogStream() );
			e.printStackTrace();
		}
	}

	/**
	 * Private method for handling individual channel methods.
	 * @param	channel	The name of the channel
	 * @param	message	The message that was sent to the channel
	 */

	private void processChatMessage( String channel, String message )
	{
		if ( message == null )
			return;

		if ( message != null && message.startsWith( "No longer" ) && !instantMessageBuffers.containsKey( getBufferKey( channel ) ) )
			return;

		LimitedSizeChatBuffer buffer = getChatBuffer( channel );

		// Figure out what the properly formatted HTML looks like
		// first, based on who sent the message and whether or not
		// there are supposed to be italics.

		String displayHTML = null;

		if ( message.indexOf( "<a" ) == -1 || message.indexOf( "</a>," ) != -1 || message.startsWith( "New message received from" ) )
			displayHTML = "<font color=green>" + message + "</font>";

		else if ( message.startsWith( "<b>from " ) || message.startsWith( "<b>to " ) )
			displayHTML = "<font color=blue>" + message + "</font>";

		else if ( message.indexOf( ">Mod Warning<" ) != -1 || message.indexOf( ">System Message<" ) != -1 )
			displayHTML = "<font color=red>" + message + "</font>";

		else if ( message.indexOf( "</a>:" ) == -1 && message.indexOf( "</b>:" ) == -1 )
			displayHTML = "<i>" + message + "</i>";

		else
			displayHTML = message;

		// Now, if the person is using LoathingChat style for
		// doing their chatting, then make sure to append the
		// channel with the appropriate color.

		if ( chattingStyle == 2 )
			displayHTML = "<font color=\"" + getColor( channel.substring(1) ) + "\">[" + channel.substring(1) + "]</font> " + displayHTML;

		// Now that everything has been properly formatted,
		// show the display HTML.

		buffer.append( displayHTML + "<br>" );

		if ( useTabbedFrame )
			tabbedFrame.highlightTab( channel );
	}

	/**
	 * Utility method which retrieves the color for the given
	 * channel.  Should only be called if the user opted to
	 * use customized colors.
	 */

	private String getColor( String channel )
	{
		String [] colors = client.getSettings().getProperty( "channelColors" ).split( "," );

		for ( int i = 0; i < ROOMS.length; ++i )
			if ( ROOMS[i].equals( channel ) )
				return colors.length < i ? colors[i] : "black";

		return "black";
	}

	/**
	 * Opens an instant message window to the character with the
	 * given name so that a private conversation can be started.
	 *
	 * @param	channel	The channel to be opened
	 */

	public void openInstantMessage( String channel )
	{
		// If the window exists, don't open another one as it
		// just confuses the disposal issue

		if ( instantMessageBuffers.containsKey( channel ) )
			return;

		Runnable openMessage = new OpenMessageRunnable( channel );

		try
		{
			if ( SwingUtilities.isEventDispatchThread() )
				openMessage.run();
			else
				SwingUtilities.invokeAndWait( openMessage );
		}
		catch ( Throwable e )
		{
			// Unless the Swing thread is interrupted for some
			// reason (which should never happen), this will
			// not happen.
		}
	}

	/**
	 * This internal class is used to open an instant message or
	 * new channel in a new tab or window.  Because it is only
	 * called internally, and it is always invoked in the Swing
	 * thread, there is no need to use the CreateFrameRunnable
	 * to ensure it gets opened in the Swing thread.
	 */

	private class OpenMessageRunnable implements Runnable
	{
		private String channel;

		public OpenMessageRunnable( String channel )
		{	this.channel = channel;
		}

		public void run()
		{
			LimitedSizeChatBuffer buffer = new LimitedSizeChatBuffer( client.getLoginName() + ": " + channel + " - Started " + Calendar.getInstance().getTime().toString() );
			instantMessageBuffers.put( channel, buffer );
			ChatFrame frame = new ChatFrame( client, KoLMessenger.this, channel );

			if ( useTabbedFrame )
			{
				frame.setVisible( false );
				ChatFrame.ChatPanel panel = tabbedFrame.addTab( channel );
				buffer.setChatDisplay( panel.getChatDisplay() );
				buffer.setScrollPane( panel.getScrollPane() );
			}
			else
			{
				frame.setVisible( true );
				buffer.setChatDisplay( frame.getChatDisplay() );
				buffer.setScrollPane( frame.getScrollPane() );
			}

			instantMessageFrames.put( channel, frame );

			if ( baseLogname != null )
			{
				String fileSuffix = channel.startsWith( "/" ) ? channel.substring( 1 ) : client.getPlayerID( channel );
				buffer.setActiveLogFile( baseLogname + "_" + fileSuffix + ".html",
					"Loathing Chat: " + client.getLoginName() + " (" + Calendar.getInstance().getTime().toString() + ")" );
			}

			if ( highlighting && !channel.equals( "[highs]" ) )
				buffer.applyHighlights();
		}
	}

	/**
	 * Utility method used to initialize all chat logs for all windows.  This
	 * method will prompt the user for the base file name, and the appropriate
	 * channel name will be appended to the end of the chat log name.
	 */

	public void initializeChatLogs()
	{
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showSaveDialog( null );

		if ( chooser.getSelectedFile() == null )
			return;

		baseLogname = chooser.getSelectedFile().getAbsolutePath();

		if ( client != null && returnVal == JFileChooser.APPROVE_OPTION )
		{
			Object [] keys = instantMessageBuffers.keySet().toArray();
			String fileSuffix, currentKey;

			for ( int i = 0; i < keys.length; ++i )
			{
				currentKey = (String) keys[i];
				fileSuffix = currentKey.startsWith( "/" ) ? currentKey.substring( 1 ) : client.getPlayerID( currentKey );

				getChatBuffer( currentKey ).setActiveLogFile( baseLogname + "_" + fileSuffix + ".html",
					"Loathing Chat: " + client.getLoginName() + " (" + Calendar.getInstance().getTime().toString() + ")" );
			}
		}
	}

	/**
	 * Utility method to clear all the chat buffers.  This is used whenever
	 * the user wishes to clear the chat buffer manually due to overflow or
	 * the desire not to log a specific part of a conversation.
	 */

	public void clearChatBuffers()
	{
		Object [] keys = instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			clearChatBuffer( (String) keys[i] );
	}

	/**
	 * Utility method to add a highlight word to the list of words currently
	 * being handled by the highlighter.  This method will prompt the user
	 * for the word or phrase which is to be highlighted, followed by a prompt
	 * for the color which they would like to use.  Cancellation during any
	 * point of this process results in no chat highlighting being added.
	 */

	public void addHighlighting()
	{
		String highlight = JOptionPane.showInputDialog( "What word/phrase would you like to highlight?", client.getLoginName() );

		if ( highlight == null )
			return;

		Color color = JColorChooser.showDialog( null, "Choose highlight color for \"" + highlight + "\"...", DEFAULT_HIGHLIGHT );

		if ( color == null )
			return;

		highlighting = true;

		LimitedSizeChatBuffer.highlightBuffer = getChatBuffer( "[highs]" );
		LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

		LimitedSizeChatBuffer.addHighlight( highlight, color );

		Object [] keys = instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			getChatBuffer( (String) keys[i] ).applyHighlights();
	}

	/**
	 * Utility method to remove a word or phrase from being highlighted.  The
	 * user will be prompted with the highlights which are currently active,
	 * and the user can select which one they would like to remove.  Note
	 * that only one highlight at a time can be removed with this method.
	 */

	public void removeHighlighting()
	{
		Object [] patterns = LimitedSizeChatBuffer.highlights.toArray();
		if ( patterns.length == 0 )
		{
			JOptionPane.showMessageDialog( null, "No active highlights." );
			return;
		}

		for ( int i = 0; i < patterns.length; ++i )
			patterns[i] = ((Pattern)patterns[i]).pattern();

		Object selectedValue = JOptionPane.showInputDialog( null, "Currently highlighting the following terms:",
				"Chat highlights!", JOptionPane.INFORMATION_MESSAGE, null, patterns, patterns[0] );

		if ( selectedValue == null )
			return;

		for ( int i = 0; i < patterns.length; ++i )
			if ( patterns[i].equals( selectedValue ) )
			{
				LimitedSizeChatBuffer.removeHighlight(i);
				LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

				Object [] keys = instantMessageBuffers.keySet().toArray();
				for ( int j = 0; j < keys.length; ++j )
					getChatBuffer( (String) keys[j] ).applyHighlights();

			}
	}
}