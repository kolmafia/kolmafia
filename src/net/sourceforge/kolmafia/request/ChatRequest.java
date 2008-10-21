/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.ChatManager;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChatRequest
	extends GenericRequest
{
	private static final ArrayList CHAT_COMMANDS = new ArrayList();

	static
	{
		// Basic commands used by KoL.

		CHAT_COMMANDS.add( "/w" );
		CHAT_COMMANDS.add( "/who" );
		CHAT_COMMANDS.add( "/whois" );

		CHAT_COMMANDS.add( "/em" );
		CHAT_COMMANDS.add( "/me" );

		CHAT_COMMANDS.add( "/msg" );
		CHAT_COMMANDS.add( "/whisper" );
		CHAT_COMMANDS.add( "/w" );
		CHAT_COMMANDS.add( "/tell" );
		CHAT_COMMANDS.add( "/r" );
		CHAT_COMMANDS.add( "/reply" );
		CHAT_COMMANDS.add( "/v" );
		CHAT_COMMANDS.add( "/conv" );

		CHAT_COMMANDS.add( "/ignore" );
		CHAT_COMMANDS.add( "/baleet" );
		CHAT_COMMANDS.add( "/friend" );

		CHAT_COMMANDS.add( "/channel" );
		CHAT_COMMANDS.add( "/c" );
		CHAT_COMMANDS.add( "/listen" );
		CHAT_COMMANDS.add( "/l" );

		CHAT_COMMANDS.add( "/friends" );
		CHAT_COMMANDS.add( "/romans" );
		CHAT_COMMANDS.add( "/clannies" );

		CHAT_COMMANDS.add( "/last" );
		CHAT_COMMANDS.add( "/updates" );
		CHAT_COMMANDS.add( "/exit" );

		CHAT_COMMANDS.add( "/warn" );
		CHAT_COMMANDS.add( "/ban" );

		// Chat channels are also effectively commands.

		CHAT_COMMANDS.add( "/newbie" );
		CHAT_COMMANDS.add( "/normal" );
		CHAT_COMMANDS.add( "/trade" );
		CHAT_COMMANDS.add( "/clan" );
		CHAT_COMMANDS.add( "/games" );
		CHAT_COMMANDS.add( "/radio" );
		CHAT_COMMANDS.add( "/pvp" );
		CHAT_COMMANDS.add( "/lounge" );
		CHAT_COMMANDS.add( "/haiku" );
		CHAT_COMMANDS.add( "/foodcourt" );
		CHAT_COMMANDS.add( "/veteran" );
		CHAT_COMMANDS.add( "/valhalla" );
		CHAT_COMMANDS.add( "/hardcore" );
		CHAT_COMMANDS.add( "/mod" );
		CHAT_COMMANDS.add( "/harem" );
		CHAT_COMMANDS.add( "/dev" );
	}

	private static final Pattern LASTSEEN_PATTERN = Pattern.compile( "<!--lastseen:(\\d+)-->" );
	private static final int CHAT_DELAY = 10000;

	private static String rightClickMenu = "";
	private static String lastSeen = "";
	private static ChatContinuationThread thread = null;

	private final boolean shouldUpdateChat;

	/**
	 * Constructs a new <code>ChatRequest</code>.
	 */

	public ChatRequest()
	{
		this( "1" );
	}

	/**
	 * Constructs a new <code>ChatRequest</code> where the given parameter will be passed to the PHP file to indicate
	 * where you left off. Note that this constructor is only available to the <code>ChatRequest</code>; this is done
	 * because only the <code>ChatRequest</code> knows what the appropriate value should be.
	 */

	private ChatRequest( final String lastSeen )
	{
		super( "newchatmessages.php" );
		this.addFormField( "lasttime", lastSeen );
		ChatRequest.lastSeen = lastSeen;

		this.shouldUpdateChat = true;
	}

	/**
	 * Constructs a new <code>ChatRequest</code> that will send the given string to the server.
	 *
	 * @param contact The channel or player to which this message is to be sent
	 * @param message The message to be sent
	 */

	public ChatRequest( final String contact, final String message )
	{
		this( contact, message, true );
	}

	/**
	 * Constructs a new <code>ChatRequest</code> that will send the given string to the server.
	 *
	 * @param contact The channel or player to which this message is to be sent
	 * @param message The message to be sent
	 * @param shouldUpdateChat Whether or not the response from the server should be printed
	 */

	public ChatRequest( final String contact, final String message, final boolean shouldUpdateChat )
	{
		super( "submitnewchat.php" );
		this.addFormField( "playerid", String.valueOf( KoLCharacter.getUserId() ) );

		String contactId = contact == null ? "[none]" :
			StringUtilities.globalStringReplace( KoLmafia.getPlayerId( contact ), " ", "_" ).trim();

		String actualMessage = message == null ? "" : message.trim();

		if ( actualMessage.startsWith( "/c " ) || actualMessage.startsWith( "/channel " ) )
		{
			// The player is switching channels.  Notify the chat client
			// that you're no longer in the original channel.

			ChatManager.stopConversation();
		}
		else if ( actualMessage.startsWith( "/exit" ) )
		{
			// Exiting chat should dispose.  KoLmafia should send the
			// message to be server-friendly.

			ChatManager.dispose();
		}
		else if ( actualMessage.startsWith( "/whois" ) )
		{
			// Leave /whois requests alone, since they always include
			// the name of the player.
		}
		else if ( contactId.startsWith( "[" ) )
		{
			// This is a message coming from an aggregated window, so
			// leave it as is.
		}
		else if ( actualMessage.startsWith( "/" ) && actualMessage.length() > 1 && Character.isDigit( actualMessage.charAt( 1 ) ) )
		{
			// This is a chat macro, and in order to work properly,
			// chat macros can't be disturbed.
		}
		else if ( actualMessage.startsWith( "/msg " ) || actualMessage.startsWith( "/whisper " ) ||
			actualMessage.startsWith( "/w " ) || actualMessage.startsWith( "/tell " ) || actualMessage.startsWith( "/r " ) ||
			actualMessage.startsWith( "/reply " ) || actualMessage.startsWith( "/conv " ) || actualMessage.startsWith( "/v " ) )
		{
			// Explicit requests for private messages should be left
			// alone, since they'd otherwise they'd be accidentally
			// put into a chat channel.
		}
		else if ( !contact.startsWith( "/" ) )
		{
			// Implied requests for a private message should be wrapped
			// in a /msg block.

			actualMessage = "/msg " + contactId + " " + actualMessage;
		}
		else if ( actualMessage.equals( "/w" ) || actualMessage.equals( "/who" ) )
		{
			// Attempts to view the /who list use the name of the channel
			// without the / in front of the channel name.

			actualMessage = "/who " + contact.substring(1);
		}
		else
		{
			// All other messages are directed to a channel.  Append the
			// name of the channel to the beginning of the message so you
			// ensure the message gets there.

			actualMessage = contact + " " + actualMessage;
		}

		this.addFormField( "graf", actualMessage );
		this.shouldUpdateChat = shouldUpdateChat;
	}

	public static final String executeChatCommand( String graf, boolean shouldQueue )
	{
		if ( graf == null )
		{
			return null;
		}

		graf = graf.trim();

		if ( !graf.startsWith( "/" ) )
		{
			return null;
		}

		String lgraf = graf.toLowerCase();

		if ( !lgraf.startsWith( "/" ) )
		{
			return null;
		}

		if ( lgraf.startsWith( "/msg 0 " ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( graf.substring( 7 ).trim() );
			return KoLmafia.getLastMessage();
		}

		int spaceIndex = lgraf.indexOf( " " );

		if ( spaceIndex == -1 )
		{
			return null;
		}

		String baseCommand = lgraf.substring( 0, spaceIndex );
		String parameters = lgraf.substring( spaceIndex ).trim();

		if ( CHAT_COMMANDS.contains( baseCommand ) )
		{
			return null;
		}

		if ( baseCommand.startsWith( "/do " ) || baseCommand.startsWith( "/cli " ) || baseCommand.startsWith( "/run " ) )
		{
			baseCommand = "";
		}
		else
		{
			baseCommand = baseCommand.substring( 1 );
		}

		String command = baseCommand + " " + parameters;
		command = command.trim();

		if ( shouldQueue )
		{
			CommandDisplayFrame.executeCommand( command );
			return " > " + command;
		}

		KoLmafiaCLI.DEFAULT_SHELL.executeLine( command );
		return KoLmafia.getLastMessage();
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void run()
	{
		String commandResult = ChatRequest.executeChatCommand( this.getFormField( "graf" ), true );
		if ( commandResult != null )
		{
			KoLmafia.registerPlayer( KoLConstants.VERSION_NAME, "458968" );
			ChatManager.broadcastMessage(
				"<font color=green>" + commandResult + "</font><br>" );
			return;
		}

		this.responseText = null;
		super.run();
	}

	public void processResults()
	{
		if ( ChatManager.isRunning() && ChatRequest.thread == null )
		{
			ChatRequest.thread = new ChatContinuationThread();
			ChatRequest.thread.start();
		}

		Matcher lastSeenMatcher = ChatRequest.LASTSEEN_PATTERN.matcher( this.responseText );
		if ( lastSeenMatcher.find() )
		{
			ChatRequest.lastSeen = lastSeenMatcher.group( 1 );
		}

		try
		{
			if ( this.shouldUpdateChat && ChatManager.isRunning() )
			{
				ChatManager.updateChat( this.responseText );
			}
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e, "Chat error" );
		}
	}

	public static final String getRightClickMenu()
	{
		if ( ChatRequest.rightClickMenu.equals( "" ) )
		{
			GenericRequest request = new GenericRequest( "lchat.php" );
			RequestThread.postRequest( request );

			int actionIndex = request.responseText.indexOf( "actions = {" );
			if ( actionIndex != -1 )
			{
				ChatRequest.rightClickMenu =
					request.responseText.substring( actionIndex, request.responseText.indexOf( ";", actionIndex ) + 1 );
			}
		}

		return ChatRequest.rightClickMenu;
	}

	/**
	 * An internal class used so that the previous request thread can die and a new one can begin.
	 */

	private static class ChatContinuationThread
		extends Thread
	{
		public void run()
		{
			ChatRequest.lastSeen = "";
			ChatRequest request = new ChatRequest( ChatRequest.lastSeen );
			PauseObject pauser = new PauseObject();

			while ( ChatManager.isRunning() )
			{
				pauser.pause( CHAT_DELAY );

				request.run();
				request.clearDataFields();

				request.addFormField( "lasttime", String.valueOf( ChatRequest.lastSeen ) );
			}

			ChatRequest.thread = null;
		}
	}
}
