/**
 * Copyright (c) 2005-2010, KoLmafia development team
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

package net.sourceforge.kolmafia.chat;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ChannelColorsRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EventManager;
import net.sourceforge.kolmafia.session.RecoveryManager;
import net.sourceforge.kolmafia.swingui.ChatFrame;
import net.sourceforge.kolmafia.swingui.TabbedChatFrame;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.utilities.RollingLinkedList;

public abstract class ChatManager
{
	private static final LinkedList clanMessages = new RollingLinkedList( 20 );
	private static ChatMessage faxbotMessage = null;
	private static final Set validChatReplyRecipients = new HashSet();

	private static final TreeMap instantMessageBuffers = new TreeMap();

	private static boolean isRunning = false;

	private static String currentChannel = null;

	private static List activeWindows = new ArrayList();
	private static List activeChannels = new ArrayList();

	private static TabbedChatFrame tabbedFrame = null;

	public static final void reset()
	{
		ChatManager.dispose();
		ChatPoller.reset();

		ChatManager.clanMessages.clear();
		ChatManager.faxbotMessage = null;
		ChatManager.instantMessageBuffers.clear();
		ChatManager.activeChannels.clear();
		ChatManager.currentChannel = null;
	}

	/**
	 * Initializes the chat buffer with the provided chat pane. Note that the chat refresher will also be initialized by
	 * calling this method; to stop the chat refresher, call the <code>dispose()</code> method.
	 */

	private static final String checkAltar()
	{
		GenericRequest request = new GenericRequest( "chatlaunch.php" );
		RequestThread.postRequest( request );
		return request.responseText;
	}

	public static final void initialize()
	{
		if ( ChatManager.isRunning )
		{
			return;
		}

		if ( !LoginRequest.completedLogin() )
		{
			return;
		}

		if ( ChatManager.checkAltar().indexOf( "altar" ) != -1 )
		{
			KoLmafia.updateDisplay( "You cannot access chat until you complete the Altar of Literacy" );
			return;
		}
		ChatManager.isRunning = true;

		StyledChatBuffer.initializeHighlights();

		Object[] bufferKeys = ChatManager.instantMessageBuffers.keySet().toArray();

		for ( int i = 0; i < bufferKeys.length; ++i )
		{
			String bufferKey = (String) bufferKeys[ i ];

			if ( bufferKey.startsWith( "/" ) )
			{
				ChatManager.openWindow( bufferKey, false );
			}
		}

		new ChatPoller().start();

		RequestThread.postRequest( new ChannelColorsRequest() );
	}

	public static final String getLastFaxBotMessage()
	{
		if ( ChatManager.faxbotMessage != null )
		{
			String message = ChatManager.faxbotMessage.getContent();
			ChatManager.faxbotMessage = null;
			return message;
		}
		return null;
	}

	public static final boolean isValidChatReplyRecipient( String playerName )
	{
		if ( validChatReplyRecipients.contains( playerName ) )
		{
			return true;
		}

		return false;
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public static final void dispose()
	{
		ChatManager.isRunning = false;

		Object[] bufferKeys = ChatManager.activeWindows.toArray();

		for ( int i = 0; i < bufferKeys.length; ++i )
		{
			String bufferKey = (String) bufferKeys[ i ];

			if ( ChatManager.tabbedFrame != null )
			{
				ChatManager.tabbedFrame.removeTab( bufferKey );
			}
			else
			{
				ChatManager.closeWindow( bufferKey );
			}
		}

		ChatManager.tabbedFrame = null;
	}

	public static final boolean isRunning()
	{
		return ChatManager.isRunning;
	}

	public static final String getCurrentChannel()
	{
		return ChatManager.currentChannel;
	}

	public static final StyledChatBuffer getBuffer( final String bufferKey )
	{
		StyledChatBuffer buffer = (StyledChatBuffer) ChatManager.instantMessageBuffers.get( bufferKey );

		if ( buffer != null )
		{
			return buffer;
		}

		buffer = new StyledChatBuffer( bufferKey, !bufferKey.equals( "[high]" ) );

		if ( Preferences.getBoolean( "logChatMessages" ) )
		{
			String fileSuffix = bufferKey;

			if ( fileSuffix.startsWith( "/" ) )
			{
				fileSuffix = "[" + fileSuffix.substring( 1 ) + "]";
			}

			StringBuffer fileName = new StringBuffer();
			fileName.append( KoLConstants.DAILY_FORMAT.format( new Date() ) );
			fileName.append( "_" );
			fileName.append( KoLCharacter.baseUserName() );
			fileName.append( "_" );
			fileName.append( fileSuffix );
			fileName.append( ".html" );

			buffer.setLogFile( new File( KoLConstants.CHATLOG_LOCATION, fileName.toString() ) );
		}

		ChatManager.instantMessageBuffers.put( bufferKey, buffer );

		return buffer;
	}

	public static void processMessages( final List messages )
	{
		Iterator messageIterator = messages.iterator();

		while ( messageIterator.hasNext() )
		{
			ChatMessage message = (ChatMessage) messageIterator.next();

			ChatManager.processMessage( message );
		}
	}

	public static void processMessage( final ChatMessage message )
	{
		if ( message instanceof EventMessage )
		{
			ChatManager.processEvent( (EventMessage) message );
			return;
		}

		if ( message instanceof EnableMessage )
		{
			ChatManager.processChannelEnable( (EnableMessage) message );
			return;
		}

		if ( message instanceof DisableMessage )
		{
			ChatManager.processChannelDisable( (DisableMessage) message );
			return;
		}

		String sender = message.getSender();
		String recipient = message.getRecipient();

		if ( "/clan".equals( recipient ) )
		{
			ChatManager.clanMessages.add( message );
			ChatManager.processCommand( message.getSender(), message.getContent(), "/clan" );
		}

		if ( "FaxBot".equals( sender ) )
		{
			ChatManager.faxbotMessage = message;
		}

		String destination = recipient;

		if ( KoLCharacter.getUserName().equals( recipient ) )
		{
			ChatManager.processCommand( message.getSender(), message.getContent(), "" );
			destination = sender;
		}

		String bufferKey = ChatManager.getBufferKey( destination );

		ChatManager.openWindow( bufferKey, true );

		StyledChatBuffer buffer = ChatManager.getBuffer( bufferKey );

		String displayHTML = ChatFormatter.formatChatMessage( message );
		buffer.append( displayHTML );
	}

	public static final String getBufferKey( String destination )
	{
		String bufferKey = destination.toLowerCase();

		if ( Preferences.getBoolean( "mergeHobopolisChat" ) )
		{
			if ( destination.equals( "/hobopolis" ) || destination.equals( "/slimetube" ) )
			{
				bufferKey = "/clan";
			}
		}

		return bufferKey;
	}

	public static final void processEvent( final EventMessage message )
	{
		if ( Preferences.getBoolean( "greenScreenProtection" ) || BuffBotHome.isBuffBotActive() || message.isHidden() )
		{
			return;
		}

		String content = message.getContent();

		if ( content.indexOf( " has " ) != -1 )
		{
			switch ( KoLmafia.displayState )
			{
			case KoLConstants.ABORT_STATE:
			case KoLConstants.ERROR_STATE:
			case KoLConstants.ENABLE_STATE:
				RequestThread.postRequest( CharPaneRequest.getInstance() );
			}
		}

		EventManager.addChatEvent( content );
		ChatManager.broadcastEvent( message );
	}

	public static final void processChannelEnable( final EnableMessage message )
	{
		String sender = message.getSender();

		if ( !ChatManager.activeChannels.contains( sender ) )
		{
			ChatManager.activeChannels.add( sender );

			String bufferKey = ChatManager.getBufferKey( sender );

			ChatManager.openWindow( bufferKey, false );
		}

		if ( message.isTalkChannel() )
		{
			ChatManager.currentChannel = sender;
		}
	}

	public static final void processChannelDisable( final DisableMessage message )
	{
		String sender = message.getSender();

		if ( ChatManager.activeChannels.contains( sender ) )
		{
			ChatManager.activeChannels.remove( sender );

			String bufferKey = ChatManager.getBufferKey( sender );

			ChatManager.closeWindow( bufferKey );
		}
	}

	public static final void processCommand( final String sender, final String content, final String channel )
	{
		if ( sender == null || content == null )
		{
			return;
		}

		// If a buffbot is running, certain commands become active, such
		// as help, restores, and logoff.

		if ( channel.equals( "" ) && BuffBotHome.isBuffBotActive() )
		{
			if ( content.equalsIgnoreCase( "help" ) )
			{
				ChatSender.sendMessage( sender, "Please check my profile." );
				return;
			}

			if ( content.equalsIgnoreCase( "restores" ) )
			{
				ChatSender.sendMessage(
					sender, "I currently have " + RecoveryManager.getRestoreCount() + " mana restores at my disposal." );

				return;
			}

			if ( content.equalsIgnoreCase( "logoff" ) )
			{
				BuffBotHome.update( BuffBotHome.ERRORCOLOR, "Logoff requested by " + sender );

				if ( ClanManager.isMember( sender ) )
				{
					System.exit( 0 );
				}

				BuffBotHome.update( BuffBotHome.ERRORCOLOR, sender + " added to ignore list" );
				ChatSender.sendMessage( sender, "/baleet" );
				return;
			}
		}

		// Otherwise, sometimes clannies want to take advantage of KoLmafia's
		// automatic chat logging.  In that case...

		if ( channel.equals( "" ) && content.equalsIgnoreCase( "update" ) )
		{
			if ( !ClanManager.isMember( sender ) )
			{
				return;
			}

			StringBuffer mailContent = new StringBuffer();

			Iterator clanMessageIterator = ChatManager.clanMessages.iterator();

			while ( clanMessageIterator.hasNext() )
			{
				ChatMessage message = (ChatMessage) clanMessageIterator.next();
				String cleanMessage = KoLConstants.ANYTAG_PATTERN.matcher( ChatFormatter.formatChatMessage( message ) ).replaceAll( "" );

				mailContent.append( cleanMessage );
				mailContent.append( "\n" );
			}

			RequestThread.postRequest( new SendMailRequest( sender, mailContent.toString() ) );
			return;
		}

		String scriptName = Preferences.getString( "chatbotScript" );
		if ( scriptName.equals( "" ) )
		{
			return;
		}

		Interpreter interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter == null )
		{
			return;
		}

		int parameterCount = interpreter.getParser().getMainMethod().getVariableReferences().size();

		String[] scriptParameters;

		if ( parameterCount == 3 )
		{
			scriptParameters = new String[]
			{
				sender,
				content,
				channel
			};
		}
		else if ( !channel.equals( "" ) )
		{
			return;
		}
		else
		{
			scriptParameters = new String[]
			{
				sender,
				content
			};
		}

		ChatManager.validChatReplyRecipients.add( sender );
		interpreter.execute( "main", scriptParameters );
		ChatManager.validChatReplyRecipients.remove( sender );

		return;
	}

	public static final void broadcastEvent( final EventMessage message )
	{
		String displayHTML = ChatFormatter.formatChatMessage( message );

		Object[] buffers = ChatManager.instantMessageBuffers.values().toArray();

		for ( int i = 0; i < buffers.length; ++i )
		{
			StyledChatBuffer buffer = (StyledChatBuffer) buffers[ i ];

			buffer.append( displayHTML );
		}
	}

	public static final void openWindow( final String bufferKey, boolean highlightOnOpen )
	{
		if ( !ChatManager.isRunning || bufferKey == null )
		{
			return;
		}

		if ( ChatManager.activeWindows.contains( bufferKey ) )
		{
			if ( ChatManager.tabbedFrame != null )
			{
				ChatManager.tabbedFrame.highlightTab( bufferKey );
			}

			return;
		}

		ChatManager.activeWindows.add( bufferKey );

		if ( Preferences.getBoolean( "useTabbedChatFrame" ) )
		{
			if ( ChatManager.tabbedFrame == null )
			{
				ChatManager.tabbedFrame = (TabbedChatFrame) new CreateFrameRunnable( TabbedChatFrame.class ).createFrame();
			}

			ChatManager.tabbedFrame.addTab( bufferKey );

			if ( highlightOnOpen )
			{
				ChatManager.tabbedFrame.highlightTab( bufferKey );
			}

			return;
		}

		ChatFrame frame = new ChatFrame( bufferKey );
		CreateFrameRunnable.decorate( frame );
		frame.setVisible( true );
	}

	public static final void closeWindow( final String bufferKey )
	{
		ChatManager.activeWindows.remove( bufferKey );

		if ( ChatManager.isRunning() && ChatManager.activeChannels.contains( bufferKey ) )
		{
			ChatManager.activeChannels.remove( bufferKey );

			if ( bufferKey.startsWith( "/" ) )
			{
				ChatSender.sendMessage( "/listen " + bufferKey.substring( 1 ) );
			}
		}
	}

	public static final void checkFriends()
	{
		ChatSender.sendMessage( "/friends" );
	}

	public static void applyHighlights()
	{
		Object[] buffers = ChatManager.instantMessageBuffers.values().toArray();

		for ( int i = 0; i < buffers.length; ++i )
		{
			StyledChatBuffer buffer = (StyledChatBuffer) buffers[ i ];

			buffer.applyHighlights();
		}
	}
}
