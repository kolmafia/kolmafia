/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Locale;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.EventMessage;

import net.sourceforge.kolmafia.request.LoginRequest;

import net.sourceforge.kolmafia.swingui.SystemTrayFrame;

public class EventManager
{
	private static final LockableListModel eventTexts = new LockableListModel();
	private static final LockableListModel eventHyperTexts = new LockableListModel();

	public static final Pattern EVENT_PATTERN =
		Pattern.compile( "<table[^>]*><tr><td[^>]*bgcolor=orange><b>New Events:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid orange;\"><center><table><tr><td>(.*?)</td></tr></table>.*?<td height=4></td></tr></table>" );

	private static final SimpleDateFormat EVENT_TIMESTAMP = new SimpleDateFormat( "MM/dd/yy hh:mm a", Locale.US );

	public static boolean hasEvents()
	{
		return !EventManager.eventTexts.isEmpty();
	}

	public static void clearEventHistory()
	{
		EventManager.eventTexts.clear();
		EventManager.eventHyperTexts.clear();
	}

	public static LockableListModel getEventTexts()
	{
		return EventManager.eventTexts;
	}

	public static LockableListModel getEventHyperTexts()
	{
		return EventManager.eventHyperTexts;
	}

	public static void addChatEvent( final String eventHTML )
	{
		EventManager.addNormalEvent( eventHTML, true );
	}

	public static boolean addNormalEvent( String eventHTML )
	{
		return EventManager.addNormalEvent( eventHTML, false );
	}

	public static boolean addNormalEvent( String eventHTML, boolean addTimestamp )
	{
		if ( eventHTML == null )
		{
			return false;
		}

		if ( eventHTML.indexOf( "logged" ) != -1 || eventHTML.indexOf( "has left the building" ) != -1 )
		{
			return false;
		}

		if ( addTimestamp )
		{
			EventManager.eventHyperTexts.add( EventManager.EVENT_TIMESTAMP.format( new Date() ) + " - " + eventHTML );
		}
		else
		{
			EventManager.eventHyperTexts.add( eventHTML );
		}

		if ( !LoginRequest.isInstanceRunning() )
		{
			// Print everything to the default shell; this way, the
			// graphical CLI is also notified of events.

			RequestLogger.printLine( eventHTML );
		}

		boolean moneyMakingGameEvent = eventHTML.indexOf( "href='bet.php'" ) != -1;

		// The event may be marked up with color and links to
		// user profiles. For example:

		// 04/25/06 12:53:54 PM - New message received from <a target=mainpane href='showplayer.php?who=115875'><font color=green>Brianna</font></a>.
		// 04/25/06 01:06:43 PM - <a class=nounder target=mainpane href='showplayer.php?who=115875'><b><font color=green>Brianna</font></b></a> has played a song (The Polka of Plenty) for you.

		// Remove tags that are not hyperlinks

		eventHTML = eventHTML.replaceAll( "</[^aA][^>]*>", "" );
		eventHTML = eventHTML.replaceAll( "<[^aA/][^>]*>", "" );

		String eventText = eventHTML.replaceAll( "<a[^>]*showplayer\\.php\\?who=(\\d+)[^>]*>(.*?)</a>", "$2 (#$1)" );

		eventText = eventText.replaceAll( "<.*?>", "" );

		if ( moneyMakingGameEvent )
		{
			MoneyMakingGameManager.processEvent( eventText );
		}

		if ( addTimestamp )
		{
			EventManager.eventTexts.add( EventManager.EVENT_TIMESTAMP.format( new Date() ) + " - " + eventText );
		}
		else
		{
			EventManager.eventTexts.add( eventText );
		}

		if ( !LoginRequest.isInstanceRunning() )
		{
			// Balloon messages for whenever the person does not have
			// focus on KoLmafia.

			if ( StaticEntity.usesSystemTray() )
			{
				SystemTrayFrame.showBalloon( eventText );
			}
		}

		return true;
	}

	public static void checkForNewEvents( String responseText )
	{
		if ( responseText == null )
		{
			return;
		}

		// Capture the entire new events table in order to display the
		// appropriate message.

		Matcher eventMatcher = EventManager.EVENT_PATTERN.matcher( responseText );
		if ( !eventMatcher.find() )
		{
			return;
		}

		// Make an array of events

		String[] events = eventMatcher.group( 1 ).replaceAll( "<br>", "\n" ).split( "\n" );

		for ( int i = 0; i < events.length; ++i )
		{
			if ( events[ i ].indexOf( "/" ) == -1 )
			{
				events[ i ] = null;
			}
		}

		for ( int i = 0; i < events.length; ++i )
		{
			EventManager.addNormalEvent( events[ i ] );

			if ( ChatManager.isRunning() )
			{
				ChatManager.broadcastEvent( new EventMessage( events[i], "green" ) );
			}
		}
	}

}
