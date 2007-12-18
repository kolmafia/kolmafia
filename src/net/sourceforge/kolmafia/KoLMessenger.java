/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.Color;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;

public abstract class KoLMessenger
	extends StaticEntity
{
	private static final Pattern CHANNEL_PATTERN = Pattern.compile( "&nbsp;&nbsp;(.*?)<br>" );
	private static final Pattern COMMENT_PATTERN = Pattern.compile( "<!--.*?-->", Pattern.DOTALL );
	private static final Pattern IMAGE_PATTERN = Pattern.compile( "<img.*?>" );
	private static final Pattern EXPAND_PATTERN = Pattern.compile( "</?p>" );
	private static final Pattern COLOR_PATTERN = Pattern.compile( "</?font.*?>" );
	private static final Pattern LINEBREAK_PATTERN = Pattern.compile( "</?br>", Pattern.CASE_INSENSITIVE );
	private static final Pattern TABLE_PATTERN = Pattern.compile( "<table>.*?</table>" );
	private static final Pattern TABLECELL_PATTERN = Pattern.compile( "</?[tc].*?>" );
	private static final Pattern PLAYERID_PATTERN =
		Pattern.compile( "showplayer\\.php\\?who\\=(\\d+)[\'\"][^>]*?>(.*?)</a>" );
	private static final Pattern PARENTHESIS_PATTERN = Pattern.compile( " \\(.*?\\)" );
	private static final Pattern MULTILINE_PATTERN = Pattern.compile( "\n+" );

	private static final Pattern GREEN_PATTERN =
		Pattern.compile( "<font color=green><b>(.*?)</font></a></b> (.*?)</font>" );
	private static final Pattern NESTED_LINKS_PATTERN =
		Pattern.compile( "<a target=mainpane href=\"([^<]*?)\"><font color=green>(.*?) <a[^>]+><font color=green>([^<]*?)</font></a>.</font></a>" );
	private static final Pattern WHOIS_PATTERN =
		Pattern.compile( "(<a [^>]*?>)<b><font color=green>([^>]*? \\(#\\d+\\))</b></a>([^<]*?)<br>" );

	private static final SimpleDateFormat EVENT_TIMESTAMP = new SimpleDateFormat( "MM/dd/yy hh:mm a", Locale.US );

	private static final String VERSION_ID = ">" + KoLConstants.VERSION_NAME + " (private)<";
	private static final String DEFAULT_TIMESTAMP_COLOR = "#7695B4";
	private static final SimpleDateFormat MESSAGE_TIMESTAMP = new SimpleDateFormat( "[HH:mm]", Locale.US );

	private static final int ROLLING_LIMIT = 32;
	private static final ArrayList clanMessages = new ArrayList();

	private static int rollingIndex = 0;
	private static String lastBlueMessage = "";

	static
	{
		for ( int i = 0; i < KoLMessenger.ROLLING_LIMIT; ++i )
		{
			KoLMessenger.clanMessages.add( "" );
		}
	}

	private static final TreeMap colors = new TreeMap();

	private static final String[] AVAILABLE_COLORS = { "#000000", // default (0)
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
	"#CCCCCC" // light grey (20)
	};

	private static boolean isRunning = false;
	private static String currentChannel = "/clan";
	private static String updateChannel = "/clan";
	private static ContactListFrame contactsFrame = null;
	private static TabbedChatFrame tabbedFrame = null;

	private static ArrayList currentlyActive = new ArrayList();
	private static TreeMap instantMessageBuffers = new TreeMap();
	private static SortedListModel onlineContacts = new SortedListModel();

	private static boolean enableMonitor = false;
	private static boolean channelsSeparate = false;
	private static boolean eventsIgnored = false;

	private static boolean useTabbedChat = false;
	private static boolean highlighting = false;

	public static final void updateFontSize()
	{
		Object[] buffers = KoLMessenger.instantMessageBuffers.values().toArray();
		for ( int i = 0; i < buffers.length; ++i )
		{
			( (LimitedSizeChatBuffer) buffers[ i ] ).fireBufferChanged();
		}
	}

	public static final void reset()
	{
		KoLMessenger.isRunning = false;
		KoLMessenger.onlineContacts.clear();
		KoLMessenger.instantMessageBuffers.clear();

		KoLMessenger.contactsFrame = new ContactListFrame( KoLMessenger.onlineContacts );
		KoLMessenger.useTabbedChat = KoLSettings.getBooleanProperty( "useTabbedChatFrame" );

		if ( KoLMessenger.useTabbedChat )
		{
			( new CreateFrameRunnable( TabbedChatFrame.class ) ).run();
		}
	}

	public static final void setTabbedFrame( final TabbedChatFrame tabbedFrame )
	{
		KoLMessenger.tabbedFrame = tabbedFrame;
	}

	private static final void updateSettings()
	{
		KoLMessenger.enableMonitor = KoLSettings.getBooleanProperty( "useChatMonitor" );
		KoLMessenger.channelsSeparate = KoLSettings.getBooleanProperty( "useSeparateChannels" );
		KoLMessenger.eventsIgnored = KoLSettings.getBooleanProperty( "greenScreenProtection" );
	}

	public static final String getChatLogName( String key )
	{
		if ( key.startsWith( "/" ) )
		{
			key = "[" + key.substring( 1 ) + "]";
		}

		String filename = KoLConstants.DAILY_FORMAT.format( new Date() ) + "_" + KoLCharacter.baseUserName();
		return key.equals( "[main]" ) ? filename + ".html" : filename + "_" + key + ".html";
	}

	public static final boolean usingTabbedChat()
	{
		return KoLMessenger.useTabbedChat;
	}

	public static final void setColor( final String channel, final int colorIndex )
	{
		if ( colorIndex == 0 )
		{
			KoLMessenger.colors.put( channel, channel.startsWith( "chat" ) ? "black" : "green" );
		}
		else
		{
			KoLMessenger.colors.put( channel, KoLMessenger.AVAILABLE_COLORS[ colorIndex ] );
		}
	}

	public static final void setUpdateChannel( final String channel )
	{
		if ( channel != null && channel.startsWith( "/" ) )
		{
			KoLMessenger.updateChannel = channel;
		}
	}

	public static final String getUpdateChannel()
	{
		return KoLMessenger.updateChannel;
	}

	/**
	 * Initializes the chat buffer with the provided chat pane. Note that the chat refresher will also be initialized by
	 * calling this method; to stop the chat refresher, call the <code>dispose()</code> method.
	 */

	public static final void initialize()
	{
		if ( KoLMessenger.isRunning )
		{
			return;
		}

		KoLMessenger.reset();
		KoLMessenger.openInstantMessage( "[main]", KoLMessenger.enableMonitor );

		// Clear the highlights and add all the ones which
		// were saved from the last session.

		LimitedSizeChatBuffer.clearHighlights();

		String[] highlights = KoLSettings.getUserProperty( "highlightList" ).trim().split( "\n+" );

		if ( highlights.length > 1 )
		{
			LimitedSizeChatBuffer.highlightBuffer = KoLMessenger.getChatBuffer( "[high]" );
			LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

			for ( int i = 0; i < highlights.length; ++i )
			{
				LimitedSizeChatBuffer.addHighlight( highlights[ i ], DataUtilities.toColor( highlights[ ++i ] ) );
			}
		}

		KoLMessenger.isRunning = true;

		RequestThread.postRequest( new ChatRequest( null, "/listen" ) );
		RequestThread.postRequest( new ChannelColorsRequest() );
	}

	/**
	 * Clears the contents of the chat buffer. This is called whenever the user wishes for there to be less text.
	 */

	public static final void clearChatBuffer( final String contact )
	{
		LimitedSizeChatBuffer bufferToClear = KoLMessenger.getChatBuffer( contact );
		if ( bufferToClear != null )
		{
			bufferToClear.clearBuffer();
		}
	}

	public static final void checkFriends()
	{
		RequestThread.postRequest( new ChatRequest( KoLMessenger.currentChannel, "/friends" ) );
	}

	public static final void checkChannel()
	{
		RequestThread.postRequest( new ChatRequest( KoLMessenger.updateChannel, "/who" ) );
	}

	/**
	 * Retrieves the chat buffer currently used for storing and saving the currently running chat associated with the
	 * given contact. If the contact is <code>null</code>, this method returns the main chat.
	 * 
	 * @param contact Name of the contact
	 * @return The chat buffer for the given contact
	 */

	public static final LimitedSizeChatBuffer getChatBuffer( final String contact )
	{
		return KoLMessenger.getChatBuffer( contact, true );
	}

	public static final LimitedSizeChatBuffer getChatBuffer( final String contact, final boolean shouldOpenWindow )
	{
		String neededBufferName = KoLMessenger.getBufferKey( contact );
		LimitedSizeChatBuffer neededBuffer =
			(LimitedSizeChatBuffer) KoLMessenger.instantMessageBuffers.get( neededBufferName );

		// This error should not happen, but it's better to be safe than
		// sorry, so there's a check to make sure that the chat buffer
		// exists before doing anything with the messages.

		if ( neededBuffer == null )
		{
			KoLMessenger.openInstantMessage( neededBufferName, shouldOpenWindow );
			neededBuffer = (LimitedSizeChatBuffer) KoLMessenger.instantMessageBuffers.get( neededBufferName );
		}

		return neededBuffer;
	}

	/**
	 * Retrieves the key which will be needed, given the contact name. In other words, it translates the contact name to
	 * a key value used by the buffers and frames.
	 */

	private static final String getBufferKey( final String contact )
	{
		return contact == null ? KoLMessenger.currentChannel : contact.startsWith( "[" ) ? contact : !KoLMessenger.channelsSeparate && contact.startsWith( "/" ) ? "[main]" : contact;
	}

	/**
	 * Removes the chat associated with the given contact. This method is called whenever a window is closed.
	 */

	public static final void removeChat( String contact )
	{
		if ( contact == null || contact.equals( "" ) )
		{
			return;
		}

		// What you're removing is not the channel, but the
		// key associated with that channel.

		contact = KoLMessenger.getBufferKey( contact );

		// If this key does not exist, then go ahead and try
		// to remove the key.

		LimitedSizeChatBuffer removedBuffer =
			(LimitedSizeChatBuffer) KoLMessenger.instantMessageBuffers.remove( contact );

		// Make sure you close any active logging on the channel
		// as well as dispose of the frame so that KoLmafia can
		// return to the login screen properly.

		if ( removedBuffer != null )
		{
			removedBuffer.closeActiveLogFile();
		}

		// If chat is no longer running, you don't have to do
		// anything more.

		if ( !KoLMessenger.isRunning )
		{
			return;
		}

		// If you're leaving the channel that you are currently
		// talking in, then this is equivalent to exiting chat.

		if ( contact.equals( KoLMessenger.currentChannel ) )
		{
			// When you exit chat, you go ahead and remove all
			// of the chat pannels from the listener lists.

			String[] channels = new String[ KoLMessenger.instantMessageBuffers.size() ];
			KoLMessenger.instantMessageBuffers.keySet().toArray( channels );

			for ( int i = 0; i < channels.length; ++i )
			{
				KoLMessenger.removeChat( channels[ i ] );
			}

			KoLMessenger.dispose();
			return;
		}

		else if ( contact.startsWith( "/" ) && KoLMessenger.currentlyActive.contains( contact ) )
		{
			KoLMessenger.currentlyActive.remove( contact );
			RequestThread.postRequest( new ChatRequest( contact, "/listen " + contact.substring( 1 ) ) );
		}
	}

	/**
	 * Returns whether or not the messenger is showing on screen.
	 * 
	 * @return <code>true</code> if the messenger is showing.
	 */

	public static final boolean isShowing()
	{
		return KoLMessenger.instantMessageBuffers.size() == 0;
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public static final void dispose()
	{
		if ( !KoLMessenger.isRunning )
		{
			return;
		}

		KoLMessenger.isRunning = false;

		KoLMessenger.removeChat( KoLMessenger.currentChannel );
		RequestThread.postRequest( new ChatRequest( KoLMessenger.currentChannel, "/exit" ) );

		KoLMessenger.currentChannel = "/clan";
		KoLMessenger.updateChannel = "/clan";

		if ( KoLMessenger.contactsFrame != null )
		{
			KoLMessenger.contactsFrame.setVisible( false );
			KoLMessenger.contactsFrame.dispose();
		}

		if ( KoLMessenger.tabbedFrame != null )
		{
			KoLMessenger.tabbedFrame.setVisible( false );
			KoLMessenger.tabbedFrame.dispose();
		}
	}

	/**
	 * Returns whether or not the messenger is currently running.
	 */

	public static final boolean isRunning()
	{
		return KoLMessenger.isRunning;
	}

	/**
	 * Replaces the current contact list with the given contact list. This is used after every call to /friends or /who.
	 */

	public static final void updateContactList( final String[] contactList )
	{
		if ( KoLMessenger.contactsFrame == null || !KoLMessenger.isRunning )
		{
			return;
		}

		KoLMessenger.onlineContacts.clear();

		for ( int i = 1; i < contactList.length; ++i )
		{
			KoLMessenger.onlineContacts.add( contactList[ i ] );
		}

		if ( KoLSettings.getBooleanProperty( "useContactsFrame" ) )
		{
			KoLMessenger.contactsFrame.setTitle( contactList[ 0 ] );
			KoLMessenger.contactsFrame.setVisible( true );
		}
	}

	public static final String getNormalizedContent( final String originalContent )
	{
		return KoLMessenger.getNormalizedContent( originalContent, true );
	}

	public static final String getNormalizedContent( final String originalContent, boolean isInternal )
	{
		String noImageContent = KoLMessenger.IMAGE_PATTERN.matcher( originalContent ).replaceAll( "" );
		String normalBreaksContent = KoLMessenger.LINEBREAK_PATTERN.matcher( noImageContent ).replaceAll( "<br>" );
		String condensedContent = KoLMessenger.EXPAND_PATTERN.matcher( normalBreaksContent ).replaceAll( "<br>" );

		String normalBoldsContent = StaticEntity.globalStringReplace( condensedContent, "<br></b>", "</b><br>" );
		String colonOrderedContent = StaticEntity.globalStringReplace( normalBoldsContent, ":</b></a>", "</a></b>:" );
		colonOrderedContent = StaticEntity.globalStringReplace( colonOrderedContent, "</a>:</b>", "</a></b>:" );
		colonOrderedContent = StaticEntity.globalStringReplace( colonOrderedContent, "</b></a>:", "</a></b>:" );

		String italicOrderedContent = StaticEntity.globalStringReplace( colonOrderedContent, "<b><i>", "<i><b>" );
		italicOrderedContent =
			StaticEntity.globalStringReplace( italicOrderedContent, "</b></font></a>", "</font></a></b>" );

		String fixedGreenContent =
			KoLMessenger.GREEN_PATTERN.matcher( italicOrderedContent ).replaceAll(
				"<font color=green><b>$1</b></font></a> $2</font>" );
		fixedGreenContent =
			KoLMessenger.NESTED_LINKS_PATTERN.matcher( fixedGreenContent ).replaceAll(
				"<a target=mainpane href=\"$1\"><font color=green>$2 $3</font></a>" );
		fixedGreenContent =
			KoLMessenger.WHOIS_PATTERN.matcher( fixedGreenContent ).replaceAll(
				"$1<b><font color=green>$2</font></b></a><font color=green>$3</font><br>" );

		String leftAlignContent = StaticEntity.globalStringDelete( fixedGreenContent, "<center>" );
		leftAlignContent = StaticEntity.globalStringReplace( leftAlignContent, "</center>", "<br>" );

		if ( !isInternal )
		{
			String normalPrivateContent =
				StaticEntity.globalStringReplace(
					leftAlignContent, "<font color=blue>private to ", "<font color=blue>private to</font></b> <b>" );
			normalPrivateContent =
				StaticEntity.globalStringReplace(
					normalPrivateContent, "(private)</a></b>", "(private)</b></font></a><font color=blue>" );
			return normalPrivateContent;
		}

		String noColorContent = KoLMessenger.COLOR_PATTERN.matcher( leftAlignContent ).replaceAll( "" );
		String noCommentsContent = KoLMessenger.COMMENT_PATTERN.matcher( noColorContent ).replaceAll( "" );
		return KoLMessenger.TABLE_PATTERN.matcher( noCommentsContent ).replaceAll( "" );
	}

	private static final void handleTableContent( final String content )
	{
		Matcher tableMatcher = KoLMessenger.TABLE_PATTERN.matcher( content );

		while ( tableMatcher.find() )
		{
			String result = tableMatcher.group();
			String[] contactList =
				KoLConstants.ANYTAG_PATTERN.matcher( result ).replaceAll( "" ).split( "(\\s*,\\s*|\\:)" );

			for ( int i = 0; i < contactList.length; ++i )
			{
				if ( contactList[ i ].indexOf( "(" ) != -1 )
				{
					contactList[ i ] = contactList[ i ].substring( 0, contactList[ i ].indexOf( "(" ) ).trim();
				}

				contactList[ i ] = contactList[ i ].toLowerCase();
			}

			KoLMessenger.updateContactList( contactList );

			if ( !KoLSettings.getBooleanProperty( "useContactsFrame" ) )
			{
				LimitedSizeChatBuffer currentChatBuffer = KoLMessenger.getChatBuffer( KoLMessenger.updateChannel );
				currentChatBuffer.append( StaticEntity.singleStringReplace( KoLMessenger.TABLECELL_PATTERN.matcher(
					content ).replaceAll( "" ), "</b>", "</b>&nbsp;" ) );
			}
		}
	}

	private static final void handlePlayerData( final String content )
	{
		Matcher playerMatcher = KoLMessenger.PLAYERID_PATTERN.matcher( content );

		String playerName, playerId;
		while ( playerMatcher.find() )
		{
			playerName =
				KoLMessenger.PARENTHESIS_PATTERN.matcher(
					KoLConstants.ANYTAG_PATTERN.matcher( playerMatcher.group( 2 ) ).replaceAll( "" ) ).replaceAll( "" );
			playerId = playerMatcher.group( 1 );

			// Handle the new player profile links -- in
			// this case, ignore the registration.

			if ( !playerName.startsWith( "&" ) )
			{
				KoLmafia.registerPlayer( playerName, playerId );
			}
		}
	}

	private static final void handleChatData( final String content )
	{
		// First, if it's the initial content that lets you
		// know which channels you're listening to, handle it.

		if ( content.startsWith( "<font color=green>Currently listening to channels:" ) )
		{
			String channel, channelKey;
			Matcher channelMatcher = KoLMessenger.CHANNEL_PATTERN.matcher( content );

			ArrayList channelList = new ArrayList();
			while ( channelMatcher.find() )
			{
				channel = channelMatcher.group( 1 );
				if ( channel.indexOf( "<b" ) != -1 )
				{
					KoLMessenger.currentChannel =
						"/" + KoLConstants.ANYTAG_PATTERN.matcher( channel ).replaceAll( "" ).trim();
				}
				else
				{
					channelList.add( channel );
				}
			}

			String[] channels = new String[ channelList.size() ];
			channelList.toArray( channels );

			KoLMessenger.openInstantMessage( KoLMessenger.getBufferKey( KoLMessenger.currentChannel ), true );

			for ( int i = 0; i < channels.length; ++i )
			{
				channelKey = "/" + KoLConstants.ANYTAG_PATTERN.matcher( channels[ i ] ).replaceAll( "" ).trim();
				KoLMessenger.openInstantMessage( KoLMessenger.getBufferKey( channelKey ), true );
			}

			return;
		}

		// Now that you know that there was no intent to exit
		// chat, go ahead and split up the lines in chat.

		String[] lines = KoLMessenger.getNormalizedContent( content ).split( "\\s*<br>\\s*" );

		// First, trim all the lines that were received so
		// that you don't get anything funny-looking, and
		// check to see if there are any messages coming from
		// channel haiku.

		int nextLine = 0;

		for ( int i = 0; i < lines.length; i = nextLine )
		{
			if ( lines[ i ] == null || lines[ i ].length() == 0 )
			{
				++nextLine;
				continue;
			}
			else
			{
				while ( ++nextLine < lines.length && lines[ nextLine ].indexOf( "<a" ) == -1 )
				{
					if ( lines[ nextLine ] != null && lines[ nextLine ].length() > 0 )
					{
						lines[ i ] += "<br>" + lines[ nextLine ];
					}
				}
			}

			KoLMessenger.processChatMessage( lines[ i ].trim() );
		}
	}

	/**
	 * Updates the chat with the given information. This method will also handle instant message data.
	 * 
	 * @param content The content with which to update the chat
	 */

	public static final void updateChat( final String content )
	{
		KoLMessenger.updateSettings();

		// Now, extract the contact list and update KoLMessenger to indicate
		// the contact list found in the last /friends update

		KoLMessenger.handleTableContent( content );

		// Extract player Ids from the most recent chat content, since it
		// can prove useful at a later time.

		KoLMessenger.handlePlayerData( content );

		// Now, that all the pre-processing is done, go ahead and handle
		// all of the individual chat data.

		KoLMessenger.handleChatData( content );
	}

	/**
	 * Notifies the chat that the user has stopped talking and listening to the current channel - this only happens
	 * after the /channel command is used to switch to a different channel.
	 */

	public static final void stopConversation()
	{
		if ( !KoLMessenger.currentChannel.equals( "" ) )
		{
			KoLMessenger.currentlyActive.remove( KoLMessenger.currentChannel );
		}
	}

	/**
	 * Utility method to update the appropriate chat window with the given message.
	 */

	public static final void processChatMessage( String message )
	{
		// Empty messages do not need to be processed; therefore,
		// return if one was retrieved.

		if ( message == null || message.length() == 0 )
		{
			return;
		}

		if ( message.startsWith( "Invalid password submitted." ) )
		{
			message = StaticEntity.globalStringDelete( message, "Invalid password submitted." ).trim();
			if ( message.length() == 0 )
			{
				return;
			}
		}

		if ( message.startsWith( "[" ) )
		{
			// If the message is coming from a listen channel, you
			// need to place it in that channel.  Otherwise, place
			// it in the current channel.

			int startIndex = message.indexOf( "]" ) + 2;
			String channel = "/" + message.substring( 1, startIndex - 2 );

			KoLMessenger.processChatMessage( channel, message.substring( startIndex ) );
		}
		else if ( message.startsWith( "No longer listening to channel: " ) )
		{
			int startIndex = message.indexOf( ":" ) + 2;
			String channel = "/" + message.substring( startIndex );

			KoLMessenger.processChatMessage( channel, message );
			KoLMessenger.currentlyActive.remove( channel );
		}
		else if ( message.startsWith( "Now listening to channel: " ) )
		{
			int startIndex = message.indexOf( ":" ) + 2;
			int dotIndex = message.indexOf( "." );
			String channel = "/" + message.substring( startIndex, dotIndex == -1 ? message.length() : dotIndex );

			KoLMessenger.processChatMessage( channel, message );
		}
		else if ( message.startsWith( "You are now talking in channel: " ) )
		{
			int startIndex = message.indexOf( ":" ) + 2;
			int dotIndex = message.indexOf( "." );
			String channel = "/" + message.substring( startIndex, dotIndex == -1 ? message.length() : dotIndex );

			KoLMessenger.processChatMessage( channel, message );
		}
		else if ( message.indexOf( "(private)<" ) != -1 )
		{
			String sender =
				KoLConstants.ANYTAG_PATTERN.matcher( message.substring( 0, message.indexOf( " (" ) ) ).replaceAll( "" );
			String text = message.substring( message.indexOf( ":" ) + 1 ).trim();

			if ( KoLMessenger.handleSpecialRequest( sender, text ) )
			{
				return;
			}

			KoLMessenger.processChatMessage(
				sender,
				"<a target=mainpane href=\"showplayer.php?who=" + KoLmafia.getPlayerId( sender ) + "\"><b><font color=blue>" + sender + "</font></b></a>: " + text );
		}
		else if ( message.startsWith( "<b>private to" ) )
		{
			String sender = KoLCharacter.getUserName();
			String recipient =
				KoLConstants.ANYTAG_PATTERN.matcher( message.substring( 0, message.indexOf( ":" ) ) ).replaceAll( "" ).substring(
					11 );

			String cleanHTML =
				"<a target=mainpane href=\"showplayer.php?who=" + KoLmafia.getPlayerId( sender ) + "\"><b><font color=red>" + sender + "</font></b></a>" + message.substring( message.indexOf( ":" ) );

			KoLMessenger.processChatMessage( recipient, cleanHTML );
		}
		else
		{
			KoLMessenger.processChatMessage( KoLMessenger.currentChannel, message );
		}
	}

	/**
	 * static final method for handling individual channel methods.
	 * 
	 * @param channel The name of the channel
	 * @param message The message that was sent to the channel
	 */

	public static final void processChatMessage( final String channel, final String message )
	{
		if ( channel == null || message == null || channel.length() == 0 || message.length() == 0 )
		{
			return;
		}

		String bufferKey = KoLMessenger.getBufferKey( channel );
		if ( message.startsWith( "No longer" ) && !KoLMessenger.instantMessageBuffers.containsKey( bufferKey ) )
		{
			return;
		}

		KoLMessenger.processChatMessage( channel, message, bufferKey, KoLMessenger.eventsIgnored );
		if ( !bufferKey.equals( "[main]" ) )
		{
			KoLMessenger.processChatMessage( channel, message, "[main]", true );
		}

		// If it's a private message, then it's possible the player wishes
		// to run some command.

		if ( channel.equals( "/clan" ) && !message.startsWith( "<b>from <a" ) && !message.startsWith( "<b>to <a" ) )
		{
			if ( KoLMessenger.rollingIndex == KoLMessenger.ROLLING_LIMIT )
			{
				KoLMessenger.rollingIndex = 0;
			}

			if ( !KoLMessenger.isGreenMessage( message ) )
			{
				KoLMessenger.clanMessages.set( KoLMessenger.rollingIndex++ , message );
			}
		}
	}

	private static final boolean handleSpecialRequest( final String channel, final String message )
	{
		// If a buffbot is running, certain commands become active, such
		// as help, restores, and logoff.

		if ( BuffBotHome.isBuffBotActive() )
		{
			if ( message.equalsIgnoreCase( "help" ) )
			{
				RequestThread.postRequest( new ChatRequest( channel, "Please check my profile.", false ) );
				return true;
			}

			if ( message.equalsIgnoreCase( "restores" ) )
			{
				RequestThread.postRequest( new ChatRequest(
					channel, "I currently have " + KoLmafia.getRestoreCount() + " mana restores at my disposal.", false ) );
				return true;
			}

			if ( message.equalsIgnoreCase( "logoff" ) )
			{
				BuffBotHome.update( BuffBotHome.ERRORCOLOR, "Logoff requested by " + channel );

				if ( ClanManager.isMember( channel ) )
				{
					System.exit( 0 );
				}

				BuffBotHome.update( BuffBotHome.ERRORCOLOR, channel + " added to ignore list" );
				RequestThread.postRequest( new ChatRequest( channel, "/baleet", false ) );
				return true;
			}
		}

		// Otherwise, sometimes clannies want to take advantage of KoLmafia's
		// automatic chat logging.  In that case...

		if ( message.equalsIgnoreCase( "update" ) )
		{
			if ( !ClanManager.isMember( channel ) )
			{
				return true;
			}

			StringBuffer data = new StringBuffer();
			for ( int i = 0; i < KoLMessenger.ROLLING_LIMIT; ++i )
			{
				data.append( KoLConstants.ANYTAG_PATTERN.matcher(
					(String) KoLMessenger.clanMessages.get( ( KoLMessenger.rollingIndex + i ) % KoLMessenger.ROLLING_LIMIT ) ).replaceAll(
					"" ) );
				data.append( "\n" );
			}

			String toSend = RequestEditorKit.getUnicode( data.toString().trim() );
			RequestThread.postRequest( new GreenMessageRequest( channel, toSend ) );
			return true;
		}

		String scriptName = KoLSettings.getUserProperty( "chatbotScript" );
		if ( scriptName.equals( "" ) )
		{
			return false;
		}

		KoLmafiaASH interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter == null )
		{
			return false;
		}

		KoLMessenger.lastBlueMessage = channel;
		KoLSettings.setUserProperty( "chatbotScriptExecuted", "false" );
		interpreter.execute( "main", new String[] { channel, message } );
		return KoLSettings.getBooleanProperty( "chatbotScriptExecuted" );
	}

	public static final String lastBlueMessage()
	{
		return KoLMessenger.lastBlueMessage;
	}

	private static final void processChatMessage( final String channel, final String message, String bufferKey,
		final boolean ignoreEvents )
	{
		try
		{
			boolean isGreenMessage = KoLMessenger.isGreenMessage( message );
			String displayHTML = KoLMessenger.formatChatMessage( channel, message, bufferKey, isGreenMessage );

			if ( isGreenMessage )
			{
				if ( ignoreEvents || BuffBotHome.isBuffBotActive() )
				{
					return;
				}

				if ( displayHTML.indexOf( " has " ) != -1 )
				{
					RequestThread.postRequest( CharpaneRequest.getInstance() );
				}

				KoLConstants.eventHistory.add( KoLMessenger.EVENT_TIMESTAMP.format( new Date() ) + " - " + KoLConstants.ANYTAG_PATTERN.matcher(
					displayHTML ).replaceAll( "" ) );
				bufferKey = KoLMessenger.updateChannel;
			}

			LimitedSizeChatBuffer buffer = KoLMessenger.getChatBuffer( bufferKey );

			if ( KoLSettings.getBooleanProperty( "logChatMessages" ) )
			{
				buffer.setActiveLogFile( new File(
					KoLConstants.CHATLOG_LOCATION, KoLMessenger.getChatLogName( bufferKey ) ) );
			}

			buffer.append( displayHTML );

			if ( KoLMessenger.isRunning && KoLMessenger.useTabbedChat )
			{
				KoLMessenger.tabbedFrame.highlightTab( bufferKey );
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, message );
		}
	}

	private static final boolean isGreenMessage( final String message )
	{
		if ( message.indexOf( ":" ) != -1 )
		{
			return false;
		}

		return message.indexOf( "logged on" ) != -1 || message.indexOf( "logged off" ) != -1 || message.startsWith( "<a target=mainpane href=\"messages.php\">" ) || message.indexOf( "have been attacked" ) != -1 || message.indexOf( "has proposed a trade" ) != -1 || message.indexOf( "has cancelled a trade" ) != -1 || message.indexOf( "has responded to a trade" ) != -1 || message.indexOf( "has declined a trade" ) != -1 || message.indexOf( "has accepted a trade" ) != -1 || message.indexOf( "has given you" ) != -1 || message.indexOf( "has played" ) != -1 || message.indexOf( "has littered toilet paper" ) != -1 || message.indexOf( "with a brick" ) != -1 || message.indexOf( "has hit you in the face" ) != -1;
	}

	public static final String formatChatMessage( final String channel, final String message, final String bufferKey,
		final boolean isGreenMessage )
	{
		StringBuffer displayHTML = new StringBuffer( message );
		StaticEntity.singleStringDelete( displayHTML, "target=mainpane " );

		// There are a bunch of messages that are supposed to be
		// formatted in green.  These are all handled first.

		boolean isWhoMessage =
			message.indexOf( "<a" ) == -1 || message.indexOf( "</a>," ) != -1 || message.startsWith( "<a class=nounder" ) || message.startsWith( "<a target=mainpane href=\'" );

		if ( isWhoMessage || isGreenMessage )
		{
			displayHTML.insert( 0, "<font color=green>" );
			displayHTML.append( "</font><br>" );

			return displayHTML.toString();
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
		else if ( message.indexOf( KoLMessenger.VERSION_ID ) != -1 )
		{
			displayHTML.insert( 0, "<font color=blue>" );
			displayHTML.append( "</font>" );
		}
		else
		{
			// Finally, all other messages are treated normally, with
			// no special formatting needed.

			Matcher nameMatcher = Pattern.compile( "<a.*?>(.*?)</a>" ).matcher( message );
			String contactName =
				nameMatcher.find() ? KoLConstants.ANYTAG_PATTERN.matcher( nameMatcher.group( 1 ) ).replaceAll( "" ) : message;

			if ( contactName.indexOf( "*" ) == -1 )
			{
				StaticEntity.singleStringReplace(
					displayHTML, contactName,
					"<font color=\"" + KoLMessenger.getColor( contactName ) + "\">" + contactName + "</font>" );
			}

			// All messages which don't have a colon following the name
			// are italicized messages from actions.

			if ( message.indexOf( "</a>:" ) == -1 && message.indexOf( "</b>:" ) == -1 )
			{
				displayHTML.insert( 0, "<i>" );
				displayHTML.append( "</i>" );
			}
		}

		// Add the appropriate eSolu scriptlet additions to the
		// processed chat message.

		if ( !KoLSettings.getUserProperty( "eSoluScriptType" ).equals( "0" ) )
		{
			Matcher whoMatcher = Pattern.compile( "showplayer\\.php\\?who=[\\d]+" ).matcher( message );
			if ( whoMatcher.find() )
			{
				String link = whoMatcher.group();
				boolean useColors = KoLSettings.getUserProperty( "eSoluScriptType" ).equals( "1" );

				StringBuffer linkBuffer = new StringBuffer();

				linkBuffer.append( " " );

				linkBuffer.append( "<a href=\"" + link + "_1\" alt=\"send blue message\">" );
				linkBuffer.append( useColors ? "<font color=blue>" : "<font color=gray>" );
				linkBuffer.append( "[p]</font></a>" );

				linkBuffer.append( "<a href=\"" + link + "_3\" alt=\"send trade request\">" );
				linkBuffer.append( useColors ? "<font color=green>" : "<font color=gray>" );
				linkBuffer.append( "[t]</font></a>" );

				linkBuffer.append( "<a href=\"" + link + "_4\" alt=\"search mall store\">" );
				linkBuffer.append( useColors ? "<font color=maroon>" : "<font color=gray>" );
				linkBuffer.append( "[m]</font></a>" );

				linkBuffer.append( "<a href=\"" + link + "_8\" alt=\"put on ignore list\">" );
				linkBuffer.append( useColors ? "<font color=red>" : "<font color=gray>" );
				linkBuffer.append( "[x]</font></a>" );

				int boldIndex = displayHTML.indexOf( "</b>" );
				if ( boldIndex != -1 )
				{
					displayHTML.insert( boldIndex + 4, linkBuffer.toString() );
				}
			}
		}

		// Now, if the person is using LoathingChat style for
		// doing their chatting, then make sure to append the
		// channel with the appropriate color.

		if ( bufferKey.startsWith( "[" ) && channel.startsWith( "/" ) )
		{
			displayHTML.insert(
				0, "<font color=\"" + KoLMessenger.getColor( channel ) + "\">[" + channel.substring( 1 ) + "]</font> " );
		}

		// Now that everything has been properly formatted,
		// show the display HTML.

		if ( !KoLMessenger.enableMonitor || !bufferKey.equals( "[main]" ) )
		{
			StringBuffer timestamp = new StringBuffer();
			timestamp.append( "<font color=\"" );
			timestamp.append( KoLMessenger.DEFAULT_TIMESTAMP_COLOR );
			timestamp.append( "\">" );
			timestamp.append( KoLMessenger.MESSAGE_TIMESTAMP.format( new Date() ) );
			timestamp.append( "</font>" );

			displayHTML.insert( 0, timestamp.toString() + "&nbsp;" );
		}

		displayHTML.append( "<br>" );

		return displayHTML.toString().replaceAll( "<([^>]*?<)", "&lt;$1" );
	}

	/**
	 * Utility method which retrieves the color for the given channel. Should only be called if the user opted to use
	 * customized colors.
	 */

	private static final String getColor( final String channel )
	{
		if ( KoLMessenger.colors.containsKey( channel ) )
		{
			return (String) KoLMessenger.colors.get( channel );
		}

		if ( channel.startsWith( "/" ) )
		{
			return "green";
		}

		if ( channel.equalsIgnoreCase( KoLCharacter.getUserName() ) && KoLMessenger.colors.containsKey( "chatcolorself" ) )
		{
			return (String) KoLMessenger.colors.get( "chatcolorself" );
		}

		if ( KoLConstants.contactList.contains( channel.toLowerCase() ) && KoLMessenger.colors.containsKey( "chatcolorcontacts" ) )
		{
			return (String) KoLMessenger.colors.get( "chatcolorcontacts" );
		}

		if ( KoLMessenger.colors.containsKey( "chatcolorothers" ) )
		{
			return (String) KoLMessenger.colors.get( "chatcolorothers" );
		}

		return "black";
	}

	/**
	 * Opens an instant message window to the character with the given name so that a private conversation can be
	 * started.
	 * 
	 * @param channel The channel to be opened
	 */

	public static final void openInstantMessage( final String channel, boolean shouldOpenWindow )
	{
		if ( channel == null )
		{
			return;
		}

		shouldOpenWindow &= KoLMessenger.isRunning;

		// If the window exists, don't open another one as it
		// just confuses the disposal issue

		if ( KoLMessenger.instantMessageBuffers.containsKey( channel ) )
		{
			return;
		}

		try
		{
			LimitedSizeChatBuffer buffer =
				new LimitedSizeChatBuffer(
					KoLCharacter.getUserName() + ": " + channel + " - Started " + Calendar.getInstance().getTime().toString(),
					true,
					KoLMessenger.isRunning && ( !channel.equals( "[main]" ) || KoLSettings.getBooleanProperty( "useSeparateChannels" ) ) );

			KoLMessenger.instantMessageBuffers.put( channel, buffer );
			if ( channel.startsWith( "/" ) )
			{
				KoLMessenger.currentlyActive.add( channel );
			}

			if ( shouldOpenWindow )
			{
				try
				{
					if ( KoLMessenger.useTabbedChat )
					{
						KoLMessenger.tabbedFrame.addTab( channel );
					}
					else
					{
						CreateFrameRunnable creator =
							new CreateFrameRunnable( ChatFrame.class, new String[] { channel } );
						if ( SwingUtilities.isEventDispatchThread() )
						{
							creator.run();
						}
						else
						{
							SwingUtilities.invokeAndWait( creator );
						}
					}
				}
				catch ( Exception e )
				{
					// Whoo, exception occurred.  Pretend nothing
					// happened and skip the exception.
				}
			}

			if ( KoLSettings.getBooleanProperty( "logChatMessages" ) )
			{
				buffer.setActiveLogFile( new File( KoLConstants.CHATLOG_LOCATION, KoLMessenger.getChatLogName( channel ) ) );
			}

			if ( KoLMessenger.highlighting && !channel.equals( "[high]" ) )
			{
				buffer.applyHighlights();
			}
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Utility method to clear all the chat buffers. This is used whenever the user wishes to clear the chat buffer
	 * manually due to overflow or the desire not to log a specific part of a conversation.
	 */

	public static final void clearChatBuffers()
	{
		Object[] keys = KoLMessenger.instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			KoLMessenger.clearChatBuffer( (String) keys[ i ] );
		}
	}

	private static final Color getRandomColor()
	{
		int[] colors = new int[ 3 ];

		do
		{
			for ( int i = 0; i < 3; ++i )
			{
				colors[ i ] = 40 + KoLConstants.RNG.nextInt( 160 );
			}
		}
		while ( colors[ 0 ] > 128 && colors[ 1 ] > 128 && colors[ 2 ] > 128 );

		return new Color( colors[ 0 ], colors[ 1 ], colors[ 2 ] );
	}

	/**
	 * Utility method to add a highlight word to the list of words currently being handled by the highlighter. This
	 * method will prompt the user for the word or phrase which is to be highlighted, followed by a prompt for the color
	 * which they would like to use. Cancellation during any point of this process results in no chat highlighting being
	 * added.
	 */

	public static final void addHighlighting()
	{
		String highlight = KoLFrame.input( "What word/phrase would you like to highlight?", KoLCharacter.getUserName() );
		if ( highlight == null )
		{
			return;
		}

		KoLMessenger.highlighting = true;
		Color color = KoLMessenger.getRandomColor();

		LimitedSizeChatBuffer.highlightBuffer = KoLMessenger.getChatBuffer( "[high]" );
		LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

		StringBuffer newSetting = new StringBuffer();

		newSetting.append( KoLSettings.getUserProperty( "highlightList" ) );
		newSetting.append( "\n" );
		newSetting.append( LimitedSizeChatBuffer.addHighlight( highlight, color ) );

		KoLSettings.setUserProperty( "highlightList", newSetting.toString().trim() );

		Object[] keys = KoLMessenger.instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			if ( !keys[ i ].equals( "[high]" ) )
			{
				KoLMessenger.getChatBuffer( (String) keys[ i ] ).applyHighlights();
			}
		}
	}

	/**
	 * Utility method to remove a word or phrase from being highlighted. The user will be prompted with the highlights
	 * which are currently active, and the user can select which one they would like to remove. Note that only one
	 * highlight at a time can be removed with this method.
	 */

	public static final void removeHighlighting()
	{
		Object[] patterns = LimitedSizeChatBuffer.highlights.toArray();
		if ( patterns.length == 0 )
		{
			KoLFrame.alert( "No active highlights." );
			KoLMessenger.highlighting = false;
			return;
		}

		for ( int i = 0; i < patterns.length; ++i )
		{
			patterns[ i ] = ( (Pattern) patterns[ i ] ).pattern();
		}

		String selectedValue = (String) KoLFrame.input( "Currently highlighting the following terms:", patterns );

		if ( selectedValue == null )
		{
			return;
		}

		LimitedSizeChatBuffer.highlightBuffer = KoLMessenger.getChatBuffer( "[high]" );
		LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

		for ( int i = 0; i < patterns.length; ++i )
		{
			if ( patterns[ i ].equals( selectedValue ) )
			{
				String settingString = LimitedSizeChatBuffer.removeHighlight( i );
				LimitedSizeChatBuffer.highlightBuffer.clearBuffer();

				String oldSetting = KoLSettings.getUserProperty( "highlightList" );
				int startIndex = oldSetting.indexOf( settingString );
				int endIndex = startIndex + settingString.length();

				StringBuffer newSetting = new StringBuffer();
				newSetting.append( oldSetting.substring( 0, startIndex ) );

				if ( endIndex < oldSetting.length() )
				{
					newSetting.append( oldSetting.substring( endIndex ) );
				}

				KoLSettings.setUserProperty( "highlightList", KoLMessenger.MULTILINE_PATTERN.matcher(
					newSetting.toString() ).replaceAll( "\n" ).trim() );
			}
		}

		Object[] keys = KoLMessenger.instantMessageBuffers.keySet().toArray();
		for ( int i = 0; i < keys.length; ++i )
		{
			if ( !keys[ i ].equals( "[high]" ) )
			{
				KoLMessenger.getChatBuffer( (String) keys[ i ] ).applyHighlights();
			}
		}
	}

	private static final Pattern GENERAL_PATTERN =
		Pattern.compile( "<td>([^<]*?)&nbsp;&nbsp;&nbsp;&nbsp;</td>.*?<option value=(\\d+) selected>" );
	private static final Pattern SELF_PATTERN =
		Pattern.compile( "<select name=chatcolorself>.*?<option value=(\\d+) selected>" );
	private static final Pattern CONTACTS_PATTERN =
		Pattern.compile( "<select name=chatcolorcontacts>.*?<option value=(\\d+) selected>" );
	private static final Pattern OTHER_PATTERN =
		Pattern.compile( "<select name=chatcolorothers>.*?<option value=(\\d+) selected>" );

	private static class ChannelColorsRequest
		extends KoLRequest
	{
		public ChannelColorsRequest()
		{
			super( "account_chatcolors.php" );
		}

		public void run()
		{
			super.run();

			// First, add in all the colors for all of the
			// channel tags (for people using standard KoL
			// chatting mode).

			Matcher colorMatcher = KoLMessenger.GENERAL_PATTERN.matcher( this.responseText );
			while ( colorMatcher.find() )
			{
				KoLMessenger.setColor(
					"/" + colorMatcher.group( 1 ).toLowerCase(), StaticEntity.parseInt( colorMatcher.group( 2 ) ) );
			}

			// Add in other custom colors which are available
			// in the chat options.

			colorMatcher = KoLMessenger.SELF_PATTERN.matcher( this.responseText );
			if ( colorMatcher.find() )
			{
				KoLMessenger.setColor( "chatcolorself", StaticEntity.parseInt( colorMatcher.group( 1 ) ) );
			}

			colorMatcher = KoLMessenger.CONTACTS_PATTERN.matcher( this.responseText );
			if ( colorMatcher.find() )
			{
				KoLMessenger.setColor( "chatcolorcontacts", StaticEntity.parseInt( colorMatcher.group( 1 ) ) );
			}

			colorMatcher = KoLMessenger.OTHER_PATTERN.matcher( this.responseText );
			if ( colorMatcher.find() )
			{
				KoLMessenger.setColor( "chatcolorothers", StaticEntity.parseInt( colorMatcher.group( 1 ) ) );
			}
		}
	}
}
