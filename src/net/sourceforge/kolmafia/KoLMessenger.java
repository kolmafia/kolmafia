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

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.StringTokenizer;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.DataUtilities;

public abstract class KoLMessenger extends StaticEntity
{
	private static final SimpleDateFormat EVENT_TIMESTAMP = new SimpleDateFormat( "MM/dd/yy hh:mm a" );

	private static final String DEFAULT_TIMESTAMP_COLOR = "#7695B4";
	private static final SimpleDateFormat MESSAGE_TIMESTAMP = new SimpleDateFormat( "[HH:mm]" );

	private static TreeMap colors = new TreeMap();
	private static String CHATLOG_BASENAME = "";

	private static final Color DEFAULT_HIGHLIGHT = new Color( 128, 0, 128 );

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

	private static int chattingStyle = 0;
	private static boolean useTabbedChat = false;
	private static boolean highlighting = false;

	public static void reset()
	{
		isRunning = false;
		onlineContacts.clear();
		instantMessageBuffers.clear();

		chattingStyle = StaticEntity.parseInt( getProperty( "chatStyle" ) );
		contactsFrame = new ContactListFrame( onlineContacts );
		useTabbedChat = getProperty( "useTabbedChat" ).equals( "1" );

		if ( useTabbedChat )
		{
			CreateFrameRunnable creator = new CreateFrameRunnable( TabbedChatFrame.class );
			creator.run();
			tabbedFrame = (TabbedChatFrame) creator.getCreation();
		}

		initializeChatLogs();
	}

	protected static void setColor( String channel, int colorIndex )
	{	colors.put( channel, AVAILABLE_COLORS[ colorIndex ] );
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

		reset();  isRunning = true;

		// Clear the highlights and add all the ones which
		// were saved from the last session.

		LimitedSizeChatBuffer.clearHighlights();

		String [] highlights = StaticEntity.getProperty( "highlightList" ).replaceAll( "\n\n+", "\n" ).trim().split( "\n" );

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
	{	(new RequestThread( new ChatRequest( client, currentChannel, "/friends" ) )).start();
	}

	public static void checkChannel()
	{	(new RequestThread( new ChatRequest( client, updateChannel, "/who" ) )).start();
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
	 * Retrieves the key which will be needed, given the contact
	 * name.  In other words, it translates the contact name to
	 * a key value used by the buffers and frames.
	 */

	private static String getBufferKey( String contact )
	{
		return contact == null ? currentChannel : contact.startsWith( "[" ) ? contact :
			chattingStyle == 1 && !contact.startsWith( "/" ) ? "[blues]" : chattingStyle == 2 && contact.startsWith( "/" ) ? "[chat]" : contact;
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
			(new RequestThread( new ChatRequest( client, contact, "/listen " + contact.substring(1) ) )).start();
			currentlyActive.remove( contact );
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

		(new Thread( new ChatRequest( client, currentChannel, "/exit" ) )).start();
		removeChat( currentChannel );

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
		String noImageContent = originalContent.replaceAll( "<img.*?>", "" );
		String condensedContent = noImageContent.replaceAll( "(</?p>)+", "<br>" );
		String noColorContent = condensedContent.replaceAll( "</?font.*?>", "" );
		String noItalicsContent = noColorContent.replaceAll( "</?i>", "" );
		String normalBreaksContent = noItalicsContent.replaceAll( "</?[Bb][Rr]>", "<br>" );
		String normalBoldsContent = normalBreaksContent.replaceAll( "<br></b>", "</b><br>" );
		String colonOrderedContent = normalBoldsContent.replaceAll( ":</b></a>", "</b></a>:" ).replaceAll( "</a>:</b>", "</a></b>:" );
		String noCommentsContent = colonOrderedContent.replaceAll( "<!--.*?-->", "" );

		return noCommentsContent.replaceAll( "<table>.*?</table>", "" );
	}

	private static void handleTableContent( String content )
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
			{
				String [] contactList = result.replaceAll( "><", "" ).replaceAll( "<.*?>", "" ).replaceAll( ":", "," ).split( "\\s*,\\s*" );

				for ( int i = 1; i < contactList.length; ++i )
				{
					if ( contactList[i].indexOf( "(" ) != -1 )
						contactList[i] = contactList[i].substring( 0, contactList[i].indexOf( "(" ) ).trim();
					contactList[i] = contactList[i].toLowerCase();
				}

				updateContactList( contactList );
			}
			if ( !getProperty( "usePopupContacts" ).equals( "1" ) )
			{
				LimitedSizeChatBuffer currentChatBuffer = getChatBuffer( updateChannel );
				currentChatBuffer.append( content.replaceAll( "</?[tc].*?>", "" ).replaceFirst( "</b>", "</b><br>" ) );
			}
		}
	}

	private static void handlePlayerData( String content )
	{
		Matcher playerMatcher = Pattern.compile( "showplayer\\.php\\?who\\=(\\d+)[\'\"][^>]*?>(.*?)</a>" ).matcher( content );

		String playerName, playerID;
		while ( playerMatcher.find() )
		{
			playerName = playerMatcher.group(2).replaceAll( "<.*?>", "" ).replaceAll( " \\(.*?\\)", "" ).replaceAll( ":" , "" );
			playerID = playerMatcher.group(1);

			// Handle the new player profile links -- in
			// this case, ignore the registration.

			if ( !playerName.startsWith( "&" ) )
				KoLmafia.registerPlayer( playerName, playerID );
		}
	}

	private static void handleChatData( String content )
	{
		// First, if it's the initial content that lets you
		// know which channels you're listening to, handle it.

		if ( content.startsWith( "<font color=green>Currently listening to channels:" ) )
		{
			String channel, channelKey;
			Matcher channelMatcher = Pattern.compile( "&nbsp;&nbsp;(.*?)<br>" ).matcher( content );

			ArrayList channelList = new ArrayList();
			while ( channelMatcher.find() )
			{
				channel = channelMatcher.group(1);
				if ( channel.indexOf( "<b" ) != -1 )
					currentChannel = "/" + channel.replaceAll( "<.*?>", "" ).trim();
				else
					channelList.add( channel );
			}

			String [] channels = new String[ channelList.size() ];
			channelList.toArray( channels );

			openInstantMessage( getBufferKey( currentChannel ) );
			for ( int i = 0; i < channels.length; ++i )
			{
				channelKey = "/" + channels[i].replaceAll( "<.*?>", "" ).trim();
				openInstantMessage( getBufferKey( channelKey ) );
			}

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
			if ( lines[i] == null )
				continue;

			else if ( lines[i].startsWith( "[haiku]" ) )
				processChatMessage( lines[i].trim() + "<br>" + lines[++i].trim() + "<br>" + lines[++i].trim() + "<br>" + lines[++i].trim() );

			else if ( currentChannel.equals( "/haiku" ) && lines[i].indexOf( "[" ) == -1 && lines[i].indexOf( ":" ) != -1 )
				processChatMessage( lines[i].trim() + "<br>" + lines[++i].trim() + "<br>" + lines[++i].trim() + "<br>" + lines[++i].trim() );

			else
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

		// Extract player IDs from the most recent chat content, since it
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
			message = message.replaceFirst( "Invalid password submitted\\.", "" ).trim();
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

			currentChannel = "/" + message.substring( startIndex ).replaceAll( "\\.", "" );
			processChatMessage( currentChannel, message );
		}
		else if ( message.indexOf( "(private)</b></a>:" ) != -1 )
		{
			String sender = message.substring( 0, message.indexOf( " (" ) ).replaceAll( "<.*?>", "" );
			String cleanHTML = "<a target=mainpane href=\"showplayer.php?who=" + KoLmafia.getPlayerID( sender ) + "\"><b><font color=blue>" +
				sender + "</font></b></a>" + message.substring( message.indexOf( ":" ) );
			processChatMessage( sender, cleanHTML );
		}
		else if ( message.startsWith( "<b>private to" ) )
		{
			String sender = KoLCharacter.getUsername();
			String recipient = message.substring( 0, message.indexOf( ":" ) ).replaceAll( "<.*?>", "" ).substring( 11 );

			String cleanHTML = "<a target=mainpane href=\"showplayer.php?who=" + KoLmafia.getPlayerID( sender ) + "\"><b><font color=red>" +
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

		LimitedSizeChatBuffer buffer = null;
		String bufferKey = getBufferKey( channel );
		String displayHTML = "";

		try
		{

			if ( message.startsWith( "No longer" ) && !instantMessageBuffers.containsKey( bufferKey ) )
				return;

			buffer = getChatBuffer( bufferKey );

			// Figure out what the properly formatted HTML looks like
			// first, based on who sent the message and whether or not
			// there are supposed to be italics.

			// There are a bunch of messages that are supposed to be
			// formatted in green.  These are all handled first.

			if ( message.indexOf( "<a" ) == -1 || message.indexOf( "</a>," ) != -1 || message.startsWith( "<a class=nounder" ) )
				displayHTML = "<font color=green>" + message + "</font>";

			else if ( message.startsWith( "<a target=mainpane href=\'" ) )
				displayHTML = "<font color=green>" + message + "</font>";

			else if ( message.startsWith( "<a target=mainpane href=\"messages.php\">" ) )
				displayHTML = "<font color=green>" + message + "</font>";

			else if ( message.indexOf( "has proposed a trade" ) != -1 )
				displayHTML = "<font color=green>" + message + "</font>";

			else if ( message.indexOf( "has cancelled a trade" ) != -1 )
				displayHTML = "<font color=green>" + message + "</font>";

			else if ( message.indexOf( "has responded to a trade" ) != -1 )
				displayHTML = "<font color=green>" + message + "</font>";

			else if ( message.indexOf( "has declined a trade" ) != -1 )
				displayHTML = "<font color=green>" + message + "</font>";

			else if ( message.indexOf( "has accepted a trade" ) != -1 )
				displayHTML = "<font color=green>" + message + "</font>";

			// Then, private messages resulting from a /last command
			// show up in blue.  These are handled next.

			else if ( message.startsWith( "<b>from " ) || message.startsWith( "<b>to " ) )
				displayHTML = "<font color=blue>" + message + "</font>";

			// Mod warnings and system messages turn up in red.  There
			// are kick messages which show up in red as well, but those
			// should also have mod warning attached.

			else if ( message.indexOf( ">Mod Warning<" ) != -1 || message.indexOf( ">System Message<" ) != -1 )
				displayHTML = "<font color=red>" + message + "</font>";

			else
			{
				// Finally, all other messages are treated normally, with
				// no special formatting needed.

				Matcher nameMatcher = Pattern.compile( "<a.*?>(.*?)</a>" ).matcher( message );
				String contactName = nameMatcher.find() ? nameMatcher.group(1).replaceAll( "<.*?>", "" ) : message;

				if ( contactName.indexOf( "*" ) == -1 )
					displayHTML = message.replaceFirst( contactName, "<font color=\"" + getColor( contactName ) + "\">" + contactName + "</font>" );

				// All messages which don't have a colon following the name
				// are italicized messages from actions.

				if ( message.indexOf( "</a>:" ) == -1 && message.indexOf( "</b>:" ) == -1 )
					displayHTML = "<i>" + displayHTML + "</i>";
			}

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

					linkBuffer.append( "</b> " );

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

					displayHTML = displayHTML.replaceFirst( "</b>", linkBuffer.toString() );
				}
			}

			// Now, if the person is using LoathingChat style for
			// doing their chatting, then make sure to append the
			// channel with the appropriate color.

			if ( bufferKey.startsWith( "[" ) && channel.startsWith( "/" ) )
				displayHTML = "<font color=\"" + getColor( channel.substring(1) ) + "\">[" + channel.substring(1) + "]</font> " + displayHTML;

			// Now that everything has been properly formatted,
			// show the display HTML.

			StringBuffer timestamp = new StringBuffer();
			timestamp.append( "<font color=\"" );
			timestamp.append( DEFAULT_TIMESTAMP_COLOR );
			timestamp.append( "\">" );
			timestamp.append( MESSAGE_TIMESTAMP.format( new Date() ) );
			timestamp.append( "</font>" );

			buffer.append( timestamp.toString() + "&nbsp;" + displayHTML + "<br>" );
			if ( displayHTML.startsWith( "<font color=green>" ) )
				KoLCharacter.getEvents().add( EVENT_TIMESTAMP.format( new Date() ) + " - " + displayHTML.replaceAll( "<.*?>", "" ) );

			// Check to make sure that in the time it took for
			// everything to be processed, chat didn't get closed.

			if ( useTabbedChat )
				tabbedFrame.highlightTab( bufferKey );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error in channel " + channel,
				new String [] { "Channel: " + channel, "Buffer key: " + bufferKey, "Object signature: " + buffer,
					"Use tabs: " + useTabbedChat, "Tabbed frame: " + tabbedFrame, "Collection signature: " + instantMessageBuffers,
					"Message: " + message, "Rendered: " + displayHTML, "" } );
		}
	}

	/**
	 * Utility method which retrieves the color for the given
	 * channel.  Should only be called if the user opted to
	 * use customized colors.
	 */

	private static String getColor( String channel )
	{
		String lowercase = channel.toLowerCase();

		if ( colors.containsKey( lowercase ) )
			return (String) colors.get( lowercase );

		if ( lowercase.equals( KoLCharacter.getUsername().toLowerCase() ) )
			return (String) colors.get( "chatcolorself" );

		if ( client.getContactList().contains( channel ) )
			return (String) colors.get( "chatcolorcontacts" );

		return (String) colors.get( "chatcolorothers" );
	}

	/**
	 * Opens an instant message window to the character with the
	 * given name so that a private conversation can be started.
	 *
	 * @param	channel	The channel to be opened
	 */

	public static void openInstantMessage( String channel )
	{
		// If the window exists, don't open another one as it
		// just confuses the disposal issue

		if ( instantMessageBuffers.containsKey( channel ) )
			return;

		try
		{
			if ( !isRunning )
				return;

			LimitedSizeChatBuffer buffer = new LimitedSizeChatBuffer( KoLCharacter.getUsername() + ": " +
				channel + " - Started " + Calendar.getInstance().getTime().toString(), true, true );

			instantMessageBuffers.put( channel, buffer );
			if ( channel.startsWith( "/" ) )
				currentlyActive.add( channel );

			if ( useTabbedChat )
				tabbedFrame.addTab( channel );
			else
				(new CreateFrameRunnable( ChatFrame.class, new String [] { channel } )).run();

			if ( CHATLOG_BASENAME != null && !CHATLOG_BASENAME.equals( "" ) )
			{
				String filename = CHATLOG_BASENAME + (channel.startsWith( "/" ) ? channel.substring( 1 ) :
					KoLmafia.getPlayerID( channel )) + ".html";
				buffer.setActiveLogFile( filename, "Loathing Chat: " + KoLCharacter.getUsername() +
					" (" + Calendar.getInstance().getTime().toString() + ")" );
			}

			if ( highlighting && !channel.equals( "[highs]" ) )
				buffer.applyHighlights();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Utility method used to initialize all chat logs for all windows.  This
	 * method will prompt the user for the base file name, and the appropriate
	 * channel name will be appended to the end of the chat log name.
	 */

	public static void initializeChatLogs()
	{
		Date currentTime = new Date();
		CHATLOG_BASENAME = "chats" + File.separator + DATED_FILENAME_FORMAT.format( currentTime ) + "_";

		Object [] keys = instantMessageBuffers.keySet().toArray();
		String filename, currentKey;

		for ( int i = 0; i < keys.length; ++i )
		{
			currentKey = (String) keys[i];
			filename = CHATLOG_BASENAME + (currentKey.startsWith( "/" ) ? currentKey.substring( 1 ) : KoLmafia.getPlayerID( currentKey )) + ".html";
			getChatBuffer( currentKey ).setActiveLogFile( filename, "Loathing Chat: " + KoLCharacter.getUsername() + " (" + currentTime + ")", true );
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

	/**
	 * Utility method to add a highlight word to the list of words currently
	 * being handled by the highlighter.  This method will prompt the user
	 * for the word or phrase which is to be highlighted, followed by a prompt
	 * for the color which they would like to use.  Cancellation during any
	 * point of this process results in no chat highlighting being added.
	 */

	public static void addHighlighting()
	{
		String highlight = JOptionPane.showInputDialog( "What word/phrase would you like to highlight?", KoLCharacter.getUsername() );
		if ( highlight == null )
			return;

		int [] colors = new int[3];

		do
		{
			for ( int i = 0; i < 3; ++i )
				colors[i] = 48 + RNG.nextInt( 144 );
		}
		while ( colors[0] > 80 && colors[1] > 80 && colors[2] > 80 );

		Color color = new Color( colors[0], colors[1], colors[2] );
		highlighting = true;

		LimitedSizeChatBuffer.highlightBuffer = getChatBuffer( "[highs]" );
		LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

		StringBuffer newSetting = new StringBuffer();

		newSetting.append( StaticEntity.getProperty( "highlightList" ) );
		newSetting.append( "\n" );
		newSetting.append( LimitedSizeChatBuffer.addHighlight( highlight, color ) );

		StaticEntity.setProperty( "highlightList", newSetting.toString().trim() );

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

				String oldSetting = StaticEntity.getProperty( "highlightList" );
				int startIndex = oldSetting.indexOf( settingString );
				int endIndex = startIndex + settingString.length();

				StringBuffer newSetting = new StringBuffer();
				newSetting.append( oldSetting.substring( 0, startIndex ) );

				if ( endIndex < oldSetting.length() )
					newSetting.append( oldSetting.substring( endIndex ) );

				StaticEntity.setProperty( "highlightList", newSetting.toString().replaceAll( "\n\n+", "\n" ).trim() );
			}
		}

		Object [] keys = instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
			if ( !keys[i].equals( "[highs]" ) )
				getChatBuffer( (String) keys[i] ).applyHighlights();
	}
}
