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

import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.Color;
import javax.swing.JOptionPane;

import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.DataUtilities;

public abstract class KoLMessenger extends StaticEntity
{
	private static final Pattern CHANNEL_PATTERN = Pattern.compile( "&nbsp;&nbsp;(.*?)<br>" );
	private static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--.*?-->", Pattern.DOTALL );
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "<img.*?>" );
	private static final Pattern EXPAND_PATTERN = Pattern.compile( "(</?p>)+" );
	private static final Pattern COLOR_PATTERN = Pattern.compile( "</?font.*?>" );
	private static final Pattern LINEBREAK_PATTERN = Pattern.compile( "</?br>", Pattern.CASE_INSENSITIVE );
	private static final Pattern TABLE_PATTERN = Pattern.compile( "<table>.*?</table>" );
	private static final Pattern ANYTAG_PATTERN = Pattern.compile( "<.*?>" );
	private static final Pattern TABLECELL_PATTERN = Pattern.compile( "</?[tc].*?>" );
	private static final Pattern PLAYERID_PATTERN = Pattern.compile( "showplayer\\.php\\?who\\=(\\d+)[\'\"][^>]*?>(.*?)</a>" );
	private static final Pattern PARENTHESIS_PATTERN = Pattern.compile( " \\(.*?\\)" );
	private static final Pattern MULTILINE_PATTERN = Pattern.compile( "\n+" );

	private static final SimpleDateFormat EVENT_TIMESTAMP = new SimpleDateFormat( "MM/dd/yy hh:mm a", Locale.US );

	private static final String DEFAULT_TIMESTAMP_COLOR = "#7695B4";
	private static final SimpleDateFormat MESSAGE_TIMESTAMP = new SimpleDateFormat( "[HH:mm]", Locale.US );

	private static TreeMap colors = new TreeMap();

	private static final String [] AVAILABLE_COLORS =
	{
		"#000000", // default (0)
		"#CC9900", // brown (1)
		"#FFCC00", // gold (2)
		"#CC3300", // dark red (3)
		"#FF0033", // red (4)
		"#FF33CC", // hot pink (5)
		"#FF99FF", // soft pink (6)
		"#663399", // dark purple (7)
		"#9933CC", // purple (8)
		"#CC99FF", // light purple (9)
		"#000066", // dark blue (10)
		"#0000CC", // blue (11)
		"#9999FF", // light blue (12)
		"#336600", // dark green (13)
		"#339966", // green (14)
		"#66CC99", // light green (15)
		"#EAEA9A", // mustard (16)
		"#FF9900", // orange (17)
		"#000000", // black (18)
		"#666666", // dark grey (19)
		"#CCCCCC"  // light grey (20)
	};

	private static boolean isRunning = false;
	private static String currentChannel = "";
	private static String updateChannel = "";
	private static ContactListFrame contactsFrame = null;
	private static TabbedChatFrame tabbedFrame = null;

	private static ArrayList currentlyActive = new ArrayList();
	private static TreeMap instantMessageBuffers = new TreeMap();
	private static SortedListModel onlineContacts = new SortedListModel();

	private static final int KOL_STYLE = 6;

	private static int chattingStyle = 0;
	private static boolean enableMonitor = false;
	private static boolean channelsSeparate = false;
	private static boolean privateSeparate = false;

	private static boolean useTabbedChat = false;
	private static boolean highlighting = false;

	public static void reset()
	{
		isRunning = false;
		onlineContacts.clear();
		instantMessageBuffers.clear();

		chattingStyle = parseInt( getProperty( "chatStyle" ) );
		enableMonitor = chattingStyle == 4 || chattingStyle == 5;
		channelsSeparate = chattingStyle == 0 || chattingStyle == 1 || chattingStyle == 4 || chattingStyle == 5;
		privateSeparate = chattingStyle == 0 || chattingStyle == 2 || chattingStyle == 4;

		contactsFrame = new ContactListFrame( onlineContacts );
		useTabbedChat = getProperty( "useTabbedChat" ).equals( "1" );

		if ( useTabbedChat )
		{
			CreateFrameRunnable creator = new CreateFrameRunnable( TabbedChatFrame.class );
			creator.run();
			tabbedFrame = (TabbedChatFrame) creator.getCreation();
		}

		Object [] keys = instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			getChatBuffer( (String) keys[i] ).setActiveLogFile( getChatLogName( (String) keys[i] ) );
	}

	public static String getChatLogName( String key )
	{
		if ( key.startsWith( "/" ) )
			key = "[" + key.substring(1) + "]";

		String filename = "chats/" + DATED_FILENAME_FORMAT.format( new Date() ) + "_" + KoLCharacter.baseUserName();
		return key.equals( "[ALL]" ) ? filename + ".html" : filename + "_" + key + ".html";
	}

	protected static final boolean usingTabbedChat()
	{	return useTabbedChat;
	}

	protected static void setColor( String channel, int colorIndex )
	{
		if ( colorIndex == 0 )
			colors.put( channel, channel.startsWith( "chat" ) ? "black" : "green" );
		else
			colors.put( channel, AVAILABLE_COLORS[ colorIndex ] );
	}

	public static void setUpdateChannel( String channel )
	{
		if ( channel != null && channel.startsWith( "/" ) )
			updateChannel = channel;
	}

	/**
	 * Initializes the chat buffer with the provided chat pane.
	 * Note that the chat refresher will also be initialized
	 * by calling this method; to stop the chat refresher, call
	 * the <code>dispose()</code> method.
	 */

	public static void initialize()
	{
		if ( isRunning )
			return;

		reset();
		isRunning = true;

		openInstantMessage( "[ALL]", enableMonitor );

		// Clear the highlights and add all the ones which
		// were saved from the last session.

		LimitedSizeChatBuffer.clearHighlights();

		String [] highlights = getProperty( "highlightList" ).trim().split( "\n+" );

		if ( highlights.length > 1 )
		{
			LimitedSizeChatBuffer.highlightBuffer = getChatBuffer( "[highs]" );
			LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

			for ( int i = 0; i < highlights.length; ++i )
				LimitedSizeChatBuffer.addHighlight( highlights[i], DataUtilities.toColor( highlights[++i] ) );
		}
	}

	/**
	 * Clears the contents of the chat buffer.  This is called
	 * whenever the user wishes for there to be less text.
	 */

	public static void clearChatBuffer( String contact )
	{
		LimitedSizeChatBuffer bufferToClear = getChatBuffer( contact );
		if ( bufferToClear != null )
			bufferToClear.clearBuffer();
	}

	public static void checkFriends()
	{	RequestThread.postRequest( new ChatRequest( currentChannel, "/friends" ) );
	}

	public static void checkChannel()
	{	RequestThread.postRequest( new ChatRequest( updateChannel, "/who" ) );
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

	public static LimitedSizeChatBuffer getChatBuffer( String contact )
	{	return getChatBuffer( contact, true );
	}

	public static LimitedSizeChatBuffer getChatBuffer( String contact, boolean shouldOpenWindow )
	{
		String neededBufferName = getBufferKey( contact );
		LimitedSizeChatBuffer neededBuffer = (LimitedSizeChatBuffer) instantMessageBuffers.get( neededBufferName );

		// This error should not happen, but it's better to be safe than
		// sorry, so there's a check to make sure that the chat buffer
		// exists before doing anything with the messages.

		if ( neededBuffer == null )
		{
			openInstantMessage( neededBufferName, shouldOpenWindow );
			neededBuffer = (LimitedSizeChatBuffer) instantMessageBuffers.get( neededBufferName );
		}

		return neededBuffer;
	}

	/**
	 * Retrieves the key which will be needed, given the contact
	 * name.  In other words, it translates the contact name to
	 * a key value used by the buffers and frames.
	 */

	private static String getBufferKey( String contact )
	{
		return chattingStyle == KOL_STYLE ? "[ALL]" : contact == null ? currentChannel : contact.startsWith( "[" ) ? contact :
			!privateSeparate && !contact.startsWith( "/" ) ? "[blues]" : !channelsSeparate && contact.startsWith( "/" ) ? "[ALL]" : contact;
	}

	/**
	 * Removes the chat associated with the given contact.  This
	 * method is called whenever a window is closed.
	 */

	public static void removeChat( String contact )
	{
		if ( contact == null || contact.equals( "" ) )
			return;

		// What you're removing is not the channel, but the
		// key associated with that channel.

		contact = getBufferKey( contact );

		// If this key does not exist, then go ahead and try
		// to remove the key.

		LimitedSizeChatBuffer removedBuffer = (LimitedSizeChatBuffer) instantMessageBuffers.remove( contact );

		// Make sure you close any active logging on the channel
		// as well as dispose of the frame so that KoLmafia can
		// return to the login screen properly.

		if ( removedBuffer != null )
			removedBuffer.closeActiveLogFile();

		// If chat is no longer running, you don't have to do
		// anything more.

		if ( !isRunning )
			return;

		// If you're leaving the channel that you are currently
		// talking in, then this is equivalent to exiting chat.

		if ( contact.equals( currentChannel ) )
		{
			// When you exit chat, you go ahead and remove all
			// of the chat pannels from the listener lists.

			String [] channels = new String[ instantMessageBuffers.keySet().size() ];
			instantMessageBuffers.keySet().toArray( channels );

			for ( int i = 0; i < channels.length; ++i )
				removeChat( channels[i] );

			KoLMessenger.dispose();
			return;
		}

		else if ( contact.startsWith( "/" ) && currentlyActive.contains( contact ) )
		{
			currentlyActive.remove( contact );
			RequestThread.postRequest( new ChatRequest( contact, "/listen " + contact.substring(1) ) );
		}
	}

	/**
	 * Returns whether or not the messenger is showing on screen.
	 * @return	<code>true</code> if the messenger is showing.
	 */

	public static boolean isShowing()
	{	return instantMessageBuffers.size() == 0;
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public static void dispose()
	{
		if ( !isRunning )
			return;

		isRunning = false;

		removeChat( currentChannel );
		RequestThread.postRequest( new ChatRequest( currentChannel, "/exit" ) );

		currentChannel = "";
		updateChannel = "";

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
	 * Returns whether or not the messenger is currently running.
	 */

	public static boolean isRunning()
	{	return isRunning;
	}

	/**
	 * Replaces the current contact list with the given contact
	 * list.  This is used after every call to /friends or /who.
	 */

	protected static void updateContactList( String [] contactList )
	{
		onlineContacts.clear();

		for ( int i = 1; i < contactList.length; ++i )
			onlineContacts.add( contactList[i] );

		if ( getProperty( "usePopupContacts" ).equals( "1" ) )
		{
			contactsFrame.setTitle( contactList[0] );
			contactsFrame.setVisible( true );
		}
	}

	private static final String getNormalizedContent( String originalContent )
	{
		String noImageContent = IMAGE_PATTERN.matcher( originalContent ).replaceAll( "" );
		String condensedContent = EXPAND_PATTERN.matcher( noImageContent ).replaceAll( "<br>" );
		String noColorContent = COLOR_PATTERN.matcher( condensedContent ).replaceAll( "" );

		String normalBreaksContent = LINEBREAK_PATTERN.matcher( noColorContent ).replaceAll( "<br>" );
		String normalBoldsContent = StaticEntity.globalStringReplace( normalBreaksContent, "<br></b>", "</b><br>" );
		String colonOrderedContent = StaticEntity.globalStringReplace( normalBoldsContent, ":</b></a>", "</b></a>:" );
		colonOrderedContent = StaticEntity.globalStringReplace( colonOrderedContent, "</a>:</b>", "</a></b>:" );
		String noCommentsContent = COMMENT_PATTERN.matcher( colonOrderedContent ).replaceAll( "" );

		return TABLE_PATTERN.matcher( noCommentsContent ).replaceAll( "" );
	}

	private static void handleTableContent( String content )
	{
		Matcher tableMatcher = TABLE_PATTERN.matcher( content );

		while ( tableMatcher.find() )
		{
			String result = tableMatcher.group();
			String [] contactList = ANYTAG_PATTERN.matcher( result ).replaceAll( "" ).split( "(\\s*,\\s*|\\:)" );

			for ( int i = 0; i < contactList.length; ++i )
			{
				if ( contactList[i].indexOf( "(" ) != -1 )
					contactList[i] = contactList[i].substring( 0, contactList[i].indexOf( "(" ) ).trim();

				contactList[i] = contactList[i].toLowerCase();
			}

			updateContactList( contactList );

			if ( !getProperty( "usePopupContacts" ).equals( "1" ) )
			{
				LimitedSizeChatBuffer currentChatBuffer = getChatBuffer( updateChannel );
				currentChatBuffer.append( StaticEntity.singleStringReplace(
					TABLECELL_PATTERN.matcher( content ).replaceAll( "" ), "</b>", "</b>&nbsp;" ) );
			}
		}
	}

	private static void handlePlayerData( String content )
	{
		Matcher playerMatcher = PLAYERID_PATTERN.matcher( content );

		String playerName, playerId;
		while ( playerMatcher.find() )
		{
			playerName = PARENTHESIS_PATTERN.matcher( ANYTAG_PATTERN.matcher( playerMatcher.group(2) ).replaceAll( "" ) ).replaceAll( "" );
			playerId = playerMatcher.group(1);

			// Handle the new player profile links -- in
			// this case, ignore the registration.

			if ( !playerName.startsWith( "&" ) )
				KoLmafia.registerPlayer( playerName, playerId );
		}
	}

	private static void handleChatData( String content )
	{
		// First, if it's the initial content that lets you
		// know which channels you're listening to, handle it.

		if ( content.startsWith( "<font color=green>Currently listening to channels:" ) )
		{
			String channel, channelKey;
			Matcher channelMatcher = CHANNEL_PATTERN.matcher( content );

			ArrayList channelList = new ArrayList();
			while ( channelMatcher.find() )
			{
				channel = channelMatcher.group(1);
				if ( channel.indexOf( "<b" ) != -1 )
					currentChannel = "/" + ANYTAG_PATTERN.matcher( channel ).replaceAll( "" ).trim();
				else
					channelList.add( channel );
			}

			String [] channels = new String[ channelList.size() ];
			channelList.toArray( channels );

			openInstantMessage( getBufferKey( currentChannel ), true );

			for ( int i = 0; i < channels.length; ++i )
			{
				channelKey = "/" + ANYTAG_PATTERN.matcher( channels[i] ).replaceAll( "" ).trim();
				openInstantMessage( getBufferKey( channelKey ), true );
			}

			return;
		}

		// Now that you know that there was no intent to exit
		// chat, go ahead and split up the lines in chat.

		String [] lines = getNormalizedContent( content ).split( "\\s*<br>\\s*" );

		// First, trim all the lines that were received so
		// that you don't get anything funny-looking, and
		// check to see if there are any messages coming from
		// channel haiku.

		int nextLine = 0;

		for ( int i = 0; i < lines.length; i = nextLine )
		{
			if ( lines[i] == null || lines[i].length() == 0 )
			{
				++nextLine;
				continue;
			}
			else
			{
				while ( ++nextLine < lines.length && lines[ nextLine ].indexOf( "<a" ) == -1 )
					if ( lines[ nextLine ] != null && lines[ nextLine ].length() > 0 )
						lines[i] += "<br>" + lines[ nextLine ];
			}

			processChatMessage( lines[i].trim() );
		}
	}

	/**
	 * Updates the chat with the given information.  This method will
	 * also handle instant message data.
	 *
	 * @param	content	The content with which to update the chat
	 */

	public static void updateChat( String content )
	{
		if ( !isRunning() )
			return;

		// Now, extract the contact list and update KoLMessenger to indicate
		// the contact list found in the last /friends update

		handleTableContent( content );

		if ( !isRunning() )
			return;

		// Extract player Ids from the most recent chat content, since it
		// can prove useful at a later time.

		handlePlayerData( content );

		if ( !isRunning() )
			return;

		// Now, that all the pre-processing is done, go ahead and handle
		// all of the individual chat data.

		handleChatData( content );
	}

	/**
	 * Notifies the chat that the user has stopped talking and
	 * listening to the current channel - this only happens after
	 * the /channel command is used to switch to a different channel.
	 */

	public static void stopConversation()
	{
		if ( !isRunning() )
			return;

		if ( !currentChannel.equals( "" ) )
			currentlyActive.remove( currentChannel );
	}

	/**
	 * Utility method to update the appropriate chat window with the
	 * given message.
	 */

	public static void processChatMessage( String message )
	{
		if ( !isRunning() )
			return;

		// Empty messages do not need to be processed; therefore,
		// return if one was retrieved.

		if ( message == null || message.length() == 0 )
			return;

		if ( message.startsWith( "Invalid password submitted." ) )
		{
			message = StaticEntity.globalStringDelete( message, "Invalid password submitted." ).trim();
			if ( message.length() == 0 )
				return;
		}

		if ( message.startsWith( "[" ) )
		{
			// If the message is coming from a listen channel, you
			// need to place it in that channel.  Otherwise, place
			// it in the current channel.

			int startIndex = message.indexOf( "]" ) + 2;
			String channel = "/" + message.substring( 1, startIndex - 2 );

			processChatMessage( channel, message.substring( startIndex ) );
		}
		else if ( message.startsWith( "No longer listening to channel: " ) )
		{
			int startIndex = message.indexOf( ":" ) + 2;
			String channel = "/" + message.substring( startIndex );

			processChatMessage( channel, message );
			currentlyActive.remove( channel );
		}
		else if ( message.startsWith( "Now listening to channel: " ) )
		{
			int startIndex = message.indexOf( ":" ) + 2;
			String channel = "/" + message.substring( startIndex );

			processChatMessage( channel, message );
		}
		else if ( message.startsWith( "You are now talking in channel: " ) )
		{
			int startIndex = message.indexOf( ":" ) + 2;

			currentChannel = "/" + message.substring( startIndex );
			if ( currentChannel.endsWith( "." ) )
				currentChannel = currentChannel.substring( 0, currentChannel.length() - 1 );

			processChatMessage( currentChannel, message );
		}
		else if ( message.indexOf( "(private)</b></a>:" ) != -1 )
		{
			String sender = ANYTAG_PATTERN.matcher( message.substring( 0, message.indexOf( " (" ) ) ).replaceAll( "" );
			String cleanHTML = "<a target=mainpane href=\"showplayer.php?who=" + KoLmafia.getPlayerId( sender ) + "\"><b><font color=blue>" +
				sender + "</font></b></a>" + message.substring( message.indexOf( ":" ) );

			processChatMessage( sender, cleanHTML );
		}
		else if ( message.startsWith( "<b>private to" ) )
		{
			String sender = KoLCharacter.getUserName();
			String recipient = ANYTAG_PATTERN.matcher( message.substring( 0, message.indexOf( ":" ) ) ).replaceAll( "" ).substring( 11 );

			String cleanHTML = "<a target=mainpane href=\"showplayer.php?who=" + KoLmafia.getPlayerId( sender ) + "\"><b><font color=red>" +
				sender + "</font></b></a>" + message.substring( message.indexOf( ":" ) );

			processChatMessage( recipient, cleanHTML );
		}
		else
		{
			if ( message.indexOf( "href='bet.php'" ) != -1 )
				message = MoneyMakingGameFrame.handleBetResult( message );

			processChatMessage( currentChannel, message );
		}
	}

	/**
	 * static method for handling individual channel methods.
	 * @param	channel	The name of the channel
	 * @param	message	The message that was sent to the channel
	 */

	private static void processChatMessage( String channel, String message )
	{
		if ( !isRunning() || channel == null || message == null || channel.length() == 0 || message.length() == 0 )
			return;

		String bufferKey = getBufferKey( channel );
		if ( message.startsWith( "No longer" ) && !instantMessageBuffers.containsKey( bufferKey ) )
			return;

		processChatMessage( channel, message, bufferKey );
		processChatMessage( channel, message, "[ALL]" );

		// If it's a private message and the buffbot is currently running,
		// then handle it as a buffbot command.

		if ( BuffBotHome.isBuffBotActive() && !channel.startsWith( "/" ) )
		{
			if ( message.endsWith( "help" ) )
			{
				(new ChatRequest( channel, "Please check my profile." )).run();
				return;
			}

			if ( message.endsWith( "restores" ) )
			{
				(new ChatRequest( channel, "I currently have " + KoLmafia.getRestoreCount() + " mana restores at my disposal." )).run();
				return;
			}

			if ( message.endsWith( "logoff" ) )
			{
				BuffBotHome.updateStatus( "Logoff requested by " + channel );
				String [] members = ClanManager.retrieveClanList();
				for ( int i = 0; i < members.length; ++i )
					if ( members[i].equalsIgnoreCase( channel ) )
						System.exit(0);

				BuffBotHome.updateStatus( channel + " added to ignore list" );
				(new ChatRequest( channel, "/baleet" )).run();
			}

		}
	}

	private static void processChatMessage( String channel, String message, String bufferKey )
	{
		try
		{
			LimitedSizeChatBuffer buffer = getChatBuffer( bufferKey );
			String displayHTML = formatChatMessage( channel, message, bufferKey );

			buffer.setActiveLogFile( getChatLogName( bufferKey ) );
			boolean hasEvent = displayHTML.indexOf( "<font color=green>" ) != -1;

			if ( !BuffBotHome.isBuffBotActive() || !hasEvent )
			{
				buffer.append( displayHTML );
				if ( useTabbedChat )
					tabbedFrame.highlightTab( bufferKey );
			}

			if ( hasEvent )
				eventHistory.add( EVENT_TIMESTAMP.format( new Date() ) + " - " + ANYTAG_PATTERN.matcher( message ).replaceAll( "" ) );

		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e, "Error in channel " + channel,
				new String [] { "Channel: " + channel, "Buffer key: " + bufferKey, "Message: " + message, "" } );
		}
	}

	public static String formatChatMessage( String channel, String message, String bufferKey )
	{
		StringBuffer displayHTML = new StringBuffer( message );

		// Figure out what the properly formatted HTML looks like
		// first, based on who sent the message and whether or not
		// there are supposed to be italics.

		// There are a bunch of messages that are supposed to be
		// formatted in green.  These are all handled first.

		if ( message.indexOf( "<a" ) == -1 || message.indexOf( "</a>," ) != -1 || message.startsWith( "<a class=nounder" ) )
		{
			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font>" );
		}
		else if ( message.startsWith( "<a target=mainpane href=\'" ) )
		{
			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font>" );
		}
		else if ( message.startsWith( "<a target=mainpane href=\"messages.php\">" ) )
		{
			if ( StaticEntity.getBooleanProperty( "ignoreGreenEvents" ) )
				return "";

			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font>" );
		}
		else if ( message.indexOf( "has proposed a trade" ) != -1 )
		{
			if ( StaticEntity.getBooleanProperty( "ignoreGreenEvents" ) )
				return "";

			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font>" );
		}
		else if ( message.indexOf( "has cancelled a trade" ) != -1 )
		{
			if ( StaticEntity.getBooleanProperty( "ignoreGreenEvents" ) )
				return "";

			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font>" );
		}
		else if ( message.indexOf( "has responded to a trade" ) != -1 )
		{
			if ( StaticEntity.getBooleanProperty( "ignoreGreenEvents" ) )
				return "";

			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font>" );
		}
		else if ( message.indexOf( "has declined a trade" ) != -1 )
		{
			if ( StaticEntity.getBooleanProperty( "ignoreGreenEvents" ) )
				return "";

			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font>" );
		}
		else if ( message.indexOf( "has accepted a trade" ) != -1 )
		{
			if ( StaticEntity.getBooleanProperty( "ignoreGreenEvents" ) )
				return "";

			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font>" );
		}

		// Then, private messages resulting from a /last command
		// show up in blue.  These are handled next.

		else if ( message.startsWith( "<b>from " ) || message.startsWith( "<b>to " ) )
		{
			displayHTML.insert( 0, "<font color=blue>" );
			displayHTML.append( "</font>" );
		}

		// Mod warnings and system messages turn up in red.  There
		// are kick messages which show up in red as well, but those
		// should also have mod warning attached.

		else if ( message.indexOf( ">Mod Warning<" ) != -1 || message.indexOf( ">System Message<" ) != -1 )
		{
			displayHTML.insert( 0, "<font color=red>" );
			displayHTML.append( "</font>" );
		}
		else
		{
			// Finally, all other messages are treated normally, with
			// no special formatting needed.

			Matcher nameMatcher = Pattern.compile( "<a.*?>(.*?)</a>" ).matcher( message );
			String contactName = nameMatcher.find() ? ANYTAG_PATTERN.matcher( nameMatcher.group(1) ).replaceAll( "" ) : message;

			if ( contactName.indexOf( "*" ) == -1 )
				StaticEntity.singleStringReplace( displayHTML, contactName, "<font color=\"" + getColor( contactName ) + "\">" + contactName + "</font>" );

			// All messages which don't have a colon following the name
			// are italicized messages from actions.

			if ( message.indexOf( "</a>:" ) == -1 && message.indexOf( "</b>:" ) == -1 )
			{
				displayHTML.insert( 0, "<i>" );
				displayHTML.append( "</i>" );
			}
		}

		if ( displayHTML.indexOf( "<font color=green>" ) != -1 && displayHTML.indexOf( " has " ) != -1 )
			CharpaneRequest.getInstance().run();

		// Add the appropriate eSolu scriptlet additions to the
		// processed chat message.

		if ( !getProperty( "eSoluScriptType" ).equals( "0" ) )
		{
			Matcher whoMatcher = Pattern.compile( "showplayer\\.php\\?who=[\\d]+" ).matcher( message );
			if ( whoMatcher.find() )
			{
				String link = whoMatcher.group();
				boolean useColors = getProperty( "eSoluScriptType" ).equals( "1" );

				StringBuffer linkBuffer = new StringBuffer();

				linkBuffer.append( " " );

				linkBuffer.append( "<a href=\"" + link + "_1\" alt=\"send blue message\">" );
				linkBuffer.append( useColors ? "<font color=blue>" : "<font color=gray>" );
				linkBuffer.append( "[p]</font></a>" );

				linkBuffer.append( "<a href=\"" + link + "_4\" alt=\"send trade request\">" );
				linkBuffer.append( useColors ? "<font color=green>" : "<font color=gray>" );
				linkBuffer.append( "[t]</font></a>" );

				linkBuffer.append( "<a href=\"" + link + "_5\" alt=\"search mall store\">" );
				linkBuffer.append( useColors ? "<font color=maroon>" : "<font color=gray>" );
				linkBuffer.append( "[m]</font></a>" );

				linkBuffer.append( "<a href=\"" + link + "_9\" alt=\"put on ignore list\">" );
				linkBuffer.append( useColors ? "<font color=red>" : "<font color=gray>" );
				linkBuffer.append( "[x]</font></a>" );

				int boldIndex = displayHTML.indexOf( "</b>" );
				if ( boldIndex != -1 )
					displayHTML.insert( boldIndex + 4, linkBuffer.toString() );
			}
		}

		// Now, if the person is using LoathingChat style for
		// doing their chatting, then make sure to append the
		// channel with the appropriate color.

		if ( bufferKey.startsWith( "[" ) && channel.startsWith( "/" ) )
			displayHTML.insert( 0, "<font color=\"" + getColor( channel ) + "\">[" + channel.substring(1) + "]</font> " );

		// Now that everything has been properly formatted,
		// show the display HTML.

		StringBuffer timestamp = new StringBuffer();
		timestamp.append( "<font color=\"" );
		timestamp.append( DEFAULT_TIMESTAMP_COLOR );
		timestamp.append( "\">" );
		timestamp.append( MESSAGE_TIMESTAMP.format( new Date() ) );
		timestamp.append( "</font>" );

		displayHTML.insert( 0, timestamp.toString() + "&nbsp;" );
		displayHTML.append( "<br>" );

		return displayHTML.toString().replaceAll( "<([^>]*?<)", "&lt;$1" );
	}

	/**
	 * Utility method which retrieves the color for the given
	 * channel.  Should only be called if the user opted to
	 * use customized colors.
	 */

	private static String getColor( String channel )
	{
		if ( colors.containsKey( channel ) )
			return (String) colors.get( channel );

		if ( channel.startsWith( "/" ) )
			return "green";

		if ( channel.equalsIgnoreCase( KoLCharacter.getUserName() ) )
			return (String) colors.get( "chatcolorself" );

		if ( contactList.contains( channel.toLowerCase() ) )
			return (String) colors.get( "chatcolorcontacts" );

		return (String) colors.get( "chatcolorothers" );
	}

	/**
	 * Opens an instant message window to the character with the
	 * given name so that a private conversation can be started.
	 *
	 * @param	channel	The channel to be opened
	 */

	public static void openInstantMessage( String channel, boolean shouldOpenWindow )
	{
		// If the window exists, don't open another one as it
		// just confuses the disposal issue

		if ( instantMessageBuffers.containsKey( channel ) )
			return;

		try
		{
			if ( !isRunning )
				return;

			LimitedSizeChatBuffer buffer = new LimitedSizeChatBuffer( KoLCharacter.getUserName() + ": " +
				channel + " - Started " + Calendar.getInstance().getTime().toString(), true, true );

			instantMessageBuffers.put( channel, buffer );
			if ( channel.startsWith( "/" ) )
				currentlyActive.add( channel );

			if ( shouldOpenWindow )
			{
				if ( useTabbedChat )
					tabbedFrame.addTab( channel );
				else
					SwingUtilities.invokeLater( new CreateFrameRunnable( ChatFrame.class, new String [] { channel } ) );
			}

			buffer.setActiveLogFile( getChatLogName( channel ) );

			if ( highlighting && !channel.equals( "[highs]" ) )
				buffer.applyHighlights();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			printStackTrace( e );
		}
	}

	/**
	 * Utility method to clear all the chat buffers.  This is used whenever
	 * the user wishes to clear the chat buffer manually due to overflow or
	 * the desire not to log a specific part of a conversation.
	 */

	public static void clearChatBuffers()
	{
		Object [] keys = instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			clearChatBuffer( (String) keys[i] );
	}

	private static Color getRandomColor()
	{
		int [] colors = new int[3];

		do
		{
			for ( int i = 0; i < 3; ++i )
				colors[i] = 40 + RNG.nextInt( 160 );
		}
		while ( colors[0] > 128 && colors[1] > 128 && colors[2] > 128 );

		return new Color( colors[0], colors[1], colors[2] );
	}

	/**
	 * Utility method to add a highlight word to the list of words currently
	 * being handled by the highlighter.  This method will prompt the user
	 * for the word or phrase which is to be highlighted, followed by a prompt
	 * for the color which they would like to use.  Cancellation during any
	 * point of this process results in no chat highlighting being added.
	 */

	public static void addHighlighting()
	{
		String highlight = JOptionPane.showInputDialog( "What word/phrase would you like to highlight?", KoLCharacter.getUserName() );
		if ( highlight == null )
			return;

		highlighting = true;
		Color color = getRandomColor();

		LimitedSizeChatBuffer.highlightBuffer = getChatBuffer( "[highs]" );
		LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

		StringBuffer newSetting = new StringBuffer();

		newSetting.append( getProperty( "highlightList" ) );
		newSetting.append( "\n" );
		newSetting.append( LimitedSizeChatBuffer.addHighlight( highlight, color ) );

		setProperty( "highlightList", newSetting.toString().trim() );

		Object [] keys = instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( !keys[i].equals( "[highs]" ) )
				getChatBuffer( (String) keys[i] ).applyHighlights();
	}

	/**
	 * Utility method to remove a word or phrase from being highlighted.  The
	 * user will be prompted with the highlights which are currently active,
	 * and the user can select which one they would like to remove.  Note
	 * that only one highlight at a time can be removed with this method.
	 */

	public static void removeHighlighting()
	{
		Object [] patterns = LimitedSizeChatBuffer.highlights.toArray();
		if ( patterns.length == 0 )
		{
			JOptionPane.showMessageDialog( null, "No active highlights." );
			highlighting = false;
			return;
		}

		for ( int i = 0; i < patterns.length; ++i )
			patterns[i] = ((Pattern)patterns[i]).pattern();

		String selectedValue = (String) JOptionPane.showInputDialog( null, "Currently highlighting the following terms:",
			"Chat highlights!", JOptionPane.INFORMATION_MESSAGE, null, patterns, patterns[0] );

		if ( selectedValue == null )
			return;

		LimitedSizeChatBuffer.highlightBuffer = getChatBuffer( "[highs]" );
		LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

		for ( int i = 0; i < patterns.length; ++i )
		{
			if ( patterns[i].equals( selectedValue ) )
			{
				String settingString = LimitedSizeChatBuffer.removeHighlight(i);
				LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

				String oldSetting = getProperty( "highlightList" );
				int startIndex = oldSetting.indexOf( settingString );
				int endIndex = startIndex + settingString.length();

				StringBuffer newSetting = new StringBuffer();
				newSetting.append( oldSetting.substring( 0, startIndex ) );

				if ( endIndex < oldSetting.length() )
					newSetting.append( oldSetting.substring( endIndex ) );

				setProperty( "highlightList", MULTILINE_PATTERN.matcher( newSetting.toString() ).replaceAll( "\n" ).trim() );
			}
		}

		Object [] keys = instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( !keys[i].equals( "[highs]" ) )
				getChatBuffer( (String) keys[i] ).applyHighlights();
	}
}
