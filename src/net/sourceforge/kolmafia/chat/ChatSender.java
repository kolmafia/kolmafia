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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.ChatRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChatSender
{
	private static final Pattern DOJAX_PATTERN =
		Pattern.compile( "<!--js\\(\\s*dojax\\(\\s*['\"](.*?)['\"]\\s*\\)\\s*;?\\s*\\)-->" );

	private static final GenericRequest DOJAX_VISITOR = new GenericRequest( "" );

	private static final Pattern PRIVATE_MESSAGE_PATTERN =
		Pattern.compile( "/(?:msg|whisper|w|tell)\\s+(\\S+)\\s+", Pattern.CASE_INSENSITIVE );

	private static final ArrayList CHANNEL_COMMANDS = new ArrayList();

	static
	{
		ChatSender.CHANNEL_COMMANDS.add( "/em" );
		ChatSender.CHANNEL_COMMANDS.add( "/me" );
		ChatSender.CHANNEL_COMMANDS.add( "/ann" );
	}
	
	public static final void sendMessage( String contact, String message )
	{
		String graf = getGraf( contact, message );
		
		if ( graf == null )
		{
			return;
		}
		
		String responseText = ChatSender.sendMessage( graf );
		ChatSender.executeAjaxCommand( responseText );
	}
		
	public static final String sendMessage( String graf )
	{		
		if ( ChatSender.executeCommand( graf ) )
		{
			return "";
		}
		
		ChatRequest request = new ChatRequest( graf );
		
		RequestThread.postRequest( request );

		List chatMessages = new ArrayList();

		if ( graf.equals( "/listen" ) )
		{
			ChatParser.parseChannelList( chatMessages, request.responseText );
		}
		else if ( graf.startsWith( "/l " ) || graf.startsWith( "/listen " ) )
		{
			ChatParser.parseListen( chatMessages, request.responseText );
		}
		else if ( graf.startsWith( "/c " ) || graf.startsWith( "/channel " ) )
		{
			ChatParser.parseChannel( chatMessages, request.responseText );
		}
		else if ( graf.startsWith( "/s " ) || graf.startsWith( "/switch " ) )
		{
			ChatParser.parseSwitch( chatMessages, request.responseText );
		}
		else if ( graf.startsWith( "/who " ) || graf.equals( "/f" ) || graf.equals( "/friends" ) || graf.equals( "/romans" ) )
		{
			ChatParser.parseContacts( chatMessages, request.responseText );
		}
		else
		{
			ChatParser.parseLines( chatMessages, request.responseText );
		}

		ChatManager.processMessages( chatMessages );

		return request.responseText;
	}

	private static final String getGraf( String contact, String message )
	{
		if ( message.startsWith( "/do " ) || message.startsWith( "/run " ) || message.startsWith( "/cli " ) )
		{
			return message;
		}
		
		Matcher privateMessageMatcher = ChatSender.PRIVATE_MESSAGE_PATTERN.matcher( message );

		if ( privateMessageMatcher.find() )
		{
			contact = privateMessageMatcher.group( 1 ).trim();
			message = message.substring( privateMessageMatcher.end() ).trim();
		}

		if ( message.length() > 256 && contact != null && !contact.equals( "/clan" ) )
		{
			// If the message is too long for one message, then
			// divide it into its component pieces.

			String command = "";
			String splitter = " ";
			String prefix = "... ";
			String suffix = " ...";

			if ( message.indexOf( " && " ) != -1 )
			{
				// Assume chained commands, must handle differently

				splitter = " && ";
				prefix = "";
				suffix = "";
			}
			else if ( message.startsWith( "/" ) )
			{
				int spaceIndex = message.indexOf( " " );

				if ( spaceIndex != -1 )
				{
					command = message.substring( 0, spaceIndex ).trim();
					message = message.substring( spaceIndex ).trim();
				}
				else
				{
					command = message.trim();
					message = "";
				}
			}

			int maxPiece = 255 - command.length() - suffix.length();

			while ( message.length() > maxPiece )
			{
				int splitPos = message.lastIndexOf( splitter, maxPiece );
				if ( splitPos <= prefix.length() )
				{
					splitPos = maxPiece;
				}

				ChatSender.sendMessage( contact, command + " " + message.substring( 0, splitPos ) + suffix );

				message = prefix + message.substring( splitPos + splitter.length() );
			}

			ChatSender.sendMessage( contact, command + " " + message );
			return null;
		}

		String contactId = "[none]";

		if ( contact != null )
		{
			contactId = ContactManager.getPlayerId( contact );
			contactId = StringUtilities.globalStringReplace( contactId, " ", "_" ).trim();
		}

		String graf = message == null ? "" : message.trim();

		if ( graf.startsWith( "/exit" ) )
		{
			// Exiting chat should dispose.  KoLmafia should send the
			// message to be server-friendly.

			net.sourceforge.kolmafia.chat.ChatManager.dispose();
			return "";
		}

		if ( contactId.startsWith( "[" ) )
		{
			// This is a message coming from an aggregated window, so
			// leave it as is.
		}
		else if ( !contact.startsWith( "/" ) )
		{
			// Implied requests for a private message should be wrapped
			// in a /msg block.

			graf = "/msg " + contactId + " " + graf;
		}
		else if ( !graf.startsWith( "/" ) )
		{
			// All non-command messages are directed to a channel.  Append the
			// name of the channel to the beginning of the message so you
			// ensure the message gets there.

			graf = contact + " " + graf;
		}
		else
		{
			int spaceIndex = graf.indexOf( " " );
			String baseCommand = spaceIndex == -1 ? graf.toLowerCase() : graf.substring( 0, spaceIndex ).toLowerCase();

			if ( graf.equals( "/w" ) || graf.equals( "/who" ) )
			{
				// Attempts to view the /who list use the name of the channel
				// when the user doesn't specify the channel.

				graf = "/who " + contact.substring( 1 );
			}
			else if ( ChatSender.CHANNEL_COMMANDS.contains( baseCommand ) )
			{
				// Direct the message to a channel.  Append the name of
				// the channel to the beginning of the message so you
				// ensure the message gets there.

				graf = contact + " " + graf;
			}
		}

		if ( graf.startsWith( "/l " ) || graf.startsWith( "/listen " ) )
		{
			String currentChannel = ChatManager.getCurrentChannel();

			if ( currentChannel != null && graf.endsWith( currentChannel ) )
			{
				return null;
			}
		}
		
		return graf;
	}

	private static final boolean executeCommand( String graf )
	{
		if ( graf == null )
		{
			return false;
		}

		if ( !graf.startsWith( "/do " ) && !graf.startsWith( "/run " ) && !graf.startsWith( "/cli " ) )
		{
			return false;
		}
		
		int spaceIndex = graf.indexOf( " " );

		String command = graf.substring( spaceIndex ).trim();
		CommandDisplayFrame.executeCommand( command );
		return true;
	}

	private static void executeAjaxCommand( String responseText )
	{
		Matcher dojax = ChatSender.DOJAX_PATTERN.matcher( responseText );

		while ( dojax.find() )
		{
			ChatSender.DOJAX_VISITOR.constructURLString( dojax.group( 1 ) );
			RequestThread.postRequest( ChatSender.DOJAX_VISITOR );

			if ( ChatSender.DOJAX_VISITOR.responseText == null )
			{
				continue;
			}

			StaticEntity.externalUpdate( ChatSender.DOJAX_VISITOR.getURLString(), ChatSender.DOJAX_VISITOR.responseText );
			EventMessage message = new EventMessage( ChatSender.DOJAX_VISITOR.responseText, null );
			ChatManager.broadcastEvent( message );
		}
	}
}
