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

package net.sourceforge.kolmafia.request;

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

public class ChatRequest
	extends GenericRequest
{
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
		this.addFormField( "pwd" );

		String contactId = KoLmafia.getPlayerId( contact );
		String actualMessage = null;

		if ( contact == null || message != null && message.equals( "/exit" ) )
		{
			actualMessage = message;
		}
		else if ( message.equals( "/friend" ) || message.equals( "/ignore" ) || message.equals( "/baleet" ) )
		{
			actualMessage = message + " " + contactId;
		}
		else if ( message.startsWith( "/w " ) || message.startsWith( "/whisper" ) || message.startsWith( "/r" ) || message.startsWith( "/v" ) || message.startsWith( "/conv" ) )
		{
			actualMessage = message;
		}
		else
		{
			boolean foundMacro = false;
			for ( int i = 1; i <= 20; ++i )
			{
				if ( message.startsWith( "/" + i ) )
				{
					foundMacro = true;
				}
			}

			if ( foundMacro || contact.startsWith( "[" ) )
			{
				actualMessage = message;
			}
			else if ( contact.startsWith( "/" ) && ( !message.startsWith( "/" ) || message.startsWith( "/me" ) || message.startsWith( "/em" ) || message.startsWith( "/warn" ) || message.startsWith( "/ann" ) ) )
			{
				actualMessage = contact + " " + message;
			}
			else if ( ( message.equals( "/who" ) || message.equals( "/w" ) ) && contact.startsWith( "/" ) )
			{
				actualMessage = "/who " + contact.substring( 1 );
			}
			else if ( contact.startsWith( "/" ) && message.startsWith( "/" ) )
			{
				actualMessage = message;
			}
			else
			{
				actualMessage = "/msg " + contactId.replaceAll( " ", "_" ) + " " + message;
			}
		}

		this.addFormField( "graf", actualMessage );

		if ( ( actualMessage.equals( "/c" ) || actualMessage.equals( "/channel" ) ) && actualMessage.indexOf( " " ) != -1 )
		{
			ChatManager.stopConversation();
		}

		if ( actualMessage.equals( "/exit" ) )
		{
			ChatManager.dispose();
		}

		this.shouldUpdateChat = shouldUpdateChat;
	}

	public static final String executeChatCommand( String graf, boolean shouldQueue )
	{
		if ( graf == null )
		{
			return null;
		}

		graf = graf.trim();
		String lgraf = graf.toLowerCase();

		if ( lgraf.startsWith( "/msg 0 " ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( graf.substring( 7 ).trim() );
			return KoLmafia.getLastMessage();
		}

		if ( !lgraf.startsWith( "/do" ) && !lgraf.startsWith( "/run" ) && !lgraf.startsWith( "/cli" ) && !lgraf.startsWith( "/wiki" ) && !lgraf.startsWith( "/lookup" ) )
		{
			return null;
		}

		int spaceIndex = graf.indexOf( " " );
		if ( spaceIndex == -1 )
		{
			return null;
		}

		String command = graf.substring( spaceIndex + 1 );

		if ( lgraf.startsWith( "/wiki" ) )
		{
			command = "wiki " + command;
		}
		else if ( lgraf.startsWith( "/lookup" ) )
		{
			command = "lookup " + command;
		}

		if ( shouldQueue )
		{
			CommandDisplayFrame.executeCommand( command );
			return "Command received, processing...";
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

	private class ChatContinuationThread
		extends Thread
	{
		public void run()
		{
			ChatRequest.lastSeen = "";
			ChatRequest request = new ChatRequest( ChatRequest.lastSeen );

			while ( GenericRequest.delay( ChatRequest.CHAT_DELAY ) && ChatManager.isRunning() )
			{
				try
				{
					request.run();
					request.clearDataFields();
					request.addFormField( "lasttime", String.valueOf( ChatRequest.lastSeen ) );
				}
				catch ( Exception e )
				{
					StaticEntity.printStackTrace( e );
				}
			}

			ChatRequest.thread = null;
		}
	}
}
