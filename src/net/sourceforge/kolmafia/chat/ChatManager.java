/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.ChannelColorsRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
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

	private static final TreeMap instantMessageBuffers = new TreeMap();

	private static boolean isRunning = false;

	private static String currentChannel = null;

	private static List activeWindows = new ArrayList();
	private static List activeChannels = new ArrayList();

	private static TabbedChatFrame tabbedFrame = null;

	public static final void reset()
	{
		ChatManager.isRunning = false;

		ChatManager.clanMessages.clear();
		ChatManager.instantMessageBuffers.clear();
	}

	/**
	 * Initializes the chat buffer with the provided chat pane. Note that the chat refresher will also be initialized by
	 * calling this method; to stop the chat refresher, call the <code>dispose()</code> method.
	 */

	public static final void initialize()
	{
		if ( ChatManager.isRunning )
		{
			return;
		}

		ChatManager.reset();

		// Clear the highlights and add all the ones which
		// were saved from the last session.

		StyledChatBuffer.clearHighlights();

		if ( StyledChatBuffer.highlightBuffer == null )
		{
			StyledChatBuffer.highlightBuffer = ChatManager.getBuffer( "[high]" );
		}

		StyledChatBuffer.highlightBuffer.clear();

		String highlights = Preferences.getString( "highlightList" ).trim();

		if ( highlights.length() > 0 )
		{
			String[] highlightList = highlights.split( "\n+" );

			for ( int i = 0; i < highlightList.length; ++i )
			{
				StyledChatBuffer.addHighlight( highlightList[ i ], DataUtilities.toColor( highlightList[ ++i ] ) );
			}
		}

		ChatManager.isRunning = true;
		ChatManager.currentChannel = null;
		ChatManager.activeChannels.clear();

		new ChatPoller().start();

		RequestThread.postRequest( new ChannelColorsRequest() );
	}

	/**
	 * Disposes the messenger's frames.
	 */

	public static final void dispose()
	{
		ChatManager.isRunning = false;
		
		ChatManager.activeWindows.clear();
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

		if ( buffer == null )
		{
			buffer = new StyledChatBuffer( bufferKey, true );

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
		}

		return buffer;
	}

	public static void processMessages( final List messages, final boolean isInternal )
	{
		Iterator messageIterator = messages.iterator();

		while ( messageIterator.hasNext() )
		{
			ChatMessage message = (ChatMessage) messageIterator.next();

			ChatManager.processMessage( message, isInternal );
		}
	}

	public static void processMessage( final ChatMessage message, final boolean isInternal )
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
		}

		if ( KoLCharacter.getUserName().equals( recipient ) )
		{
			if ( ChatManager.processCommand( message.getSender(), message.getContent() ) )
			{
				return;
			}
		}

		String destination = recipient;

		if ( KoLCharacter.getUserName().equals( recipient ) )
		{
			destination = sender;
		}

		String bufferKey = destination.toLowerCase();

		if ( Preferences.getBoolean( "mergeHobopolisChat" ) )
		{
			if ( destination.equals( "/hobopolis" ) || destination.equals( "/slimetube" ) )
			{
				bufferKey = "/clan";
			}
		}

		ChatManager.openWindow( bufferKey );

		StyledChatBuffer buffer = ChatManager.getBuffer( bufferKey );

		String displayHTML = ChatFormatter.formatChatMessage( message );
		buffer.append( displayHTML );
	}

	public static final void processEvent( final EventMessage message )
	{
		if ( Preferences.getBoolean( "greenScreenProtection" ) || BuffBotHome.isBuffBotActive() )
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
			ChatManager.openWindow( sender );
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
			ChatManager.closeWindow( sender );
		}
	}

	public static final boolean processCommand( final String sender, final String content )
	{
		if ( sender == null || content == null )
		{
			return false;
		}

		// If a buffbot is running, certain commands become active, such
		// as help, restores, and logoff.

		if ( BuffBotHome.isBuffBotActive() )
		{
			if ( content.equalsIgnoreCase( "help" ) )
			{
				ChatSender.sendMessage( sender, "Please check my profile.", true );
				return true;
			}

			if ( content.equalsIgnoreCase( "restores" ) )
			{
				ChatSender.sendMessage(
					sender, "I currently have " + RecoveryManager.getRestoreCount() + " mana restores at my disposal.",
					true );

				return true;
			}

			if ( content.equalsIgnoreCase( "logoff" ) )
			{
				BuffBotHome.update( BuffBotHome.ERRORCOLOR, "Logoff requested by " + sender );

				if ( ClanManager.isMember( sender ) )
				{
					System.exit( 0 );
				}

				BuffBotHome.update( BuffBotHome.ERRORCOLOR, sender + " added to ignore list" );
				ChatSender.sendMessage( sender, "/baleet", true );
				return true;
			}
		}

		// Otherwise, sometimes clannies want to take advantage of KoLmafia's
		// automatic chat logging.  In that case...

		if ( content.equalsIgnoreCase( "update" ) )
		{
			if ( !ClanManager.isMember( sender ) )
			{
				return true;
			}

			StringBuffer mailContent = new StringBuffer();

			Iterator clanMessageIterator = ChatManager.clanMessages.iterator();

			while ( clanMessageIterator.hasNext() )
			{
				ChatMessage message = (ChatMessage) clanMessageIterator.next();
				String cleanMessage = KoLConstants.ANYTAG_PATTERN.matcher( message.getContent() ).replaceAll( "" );

				mailContent.append( cleanMessage );
				mailContent.append( "\n" );
			}

			RequestThread.postRequest( new SendMailRequest( sender, mailContent.toString() ) );
			return true;
		}

		String scriptName = Preferences.getString( "chatbotScript" );
		if ( scriptName.equals( "" ) )
		{
			return false;
		}

		Interpreter interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
		if ( interpreter == null )
		{
			return false;
		}

		String[] scriptParameters = new String[]
		{
			sender,
			content
		};

		interpreter.execute( "main", scriptParameters );

		return true;
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

	public static final void openWindow( final String bufferKey )
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
				ChatManager.tabbedFrame = new TabbedChatFrame();
				new CreateFrameRunnable( TabbedChatFrame.class ).run();
			}

			ChatManager.tabbedFrame.addTab( bufferKey );
			ChatManager.tabbedFrame.highlightTab( bufferKey );
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
			ChatSender.sendMessage( null, "/listen " + bufferKey.substring( 1 ), true );
		}
	}

	public static final void checkFriends()
	{
		ChatSender.sendMessage( null, "/friends", true );
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
