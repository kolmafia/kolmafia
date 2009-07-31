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

 package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.MoneyMakingGameManager;
import net.sourceforge.kolmafia.swingui.RecentEventsFrame;
import net.sourceforge.kolmafia.swingui.SystemTrayFrame;

public class EventManager
{
	private static final LockableListModel eventHistory = new LockableListModel();

	private static final Pattern EVENT_PATTERN =
		Pattern.compile( "bgcolor=orange><b>New Events:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid orange;\"><center><table><tr><td>(.*?)</td></tr></table>.*?<td height=4></td></tr></table>" );

	public static boolean hasEvents()
	{
		return !EventManager.eventHistory.isEmpty();
	}

	public static void clearEventHistory()
	{
		EventManager.eventHistory.clear();
	}

	public static LockableListModel getEventHistory()
	{
		return EventManager.eventHistory;
	}

	public static boolean addEvent( String eventText, boolean isChatEvent )
	{
		if ( eventText == null )
		{
			return false;
		}

		if ( eventText.indexOf( "logged" ) != -1 )
		{
			return false;
		}

		boolean mmg = eventText.indexOf( "href='bet.php'" ) != -1;

		// The event may be marked up with color and links to
		// user profiles. For example:

		// 04/25/06 12:53:54 PM - New message received from <a target=mainpane href='showplayer.php?who=115875'><font color=green>Brianna</font></a>.
		// 04/25/06 01:06:43 PM - <a class=nounder target=mainpane href='showplayer.php?who=115875'><b><font color=green>Brianna</font></b></a> has played a song (The Polka of Plenty) for you.

		// Add in a player Id so that the events can be handled
		// using a ShowDescriptionList.

		// Remove tags that are not hyperlinks
		eventText = eventText.replaceAll( "</a>", "<a>" ).replaceAll( "<[^a].*?>", "" );
		// Munge links to player profiles
		eventText = eventText.replaceAll( "<a[^>]*showplayer\\.php\\?who=(\\d+)[^>]*>(.*?)<a>", "$2 (#$1)" );
		// Remove all remaining tags.
		eventText = eventText.replaceAll( "<.*?>", "" ).replaceAll( "\\s+", " " );
		if ( mmg )
		{
			MoneyMakingGameManager.processEvent( eventText );
		}

		if ( eventText.indexOf( "/" ) == -1 )
		{
			return false;
		}

		EventManager.eventHistory.add( eventText );

		// Print everything to the default shell; this way, the
		// graphical CLI is also notified of events.

		RequestLogger.printLine( eventText );

		// Balloon messages for whenever the person does not have
		// focus on KoLmafia.

		if ( !isChatEvent )
		{
			if ( StaticEntity.usesSystemTray() )
			{
				SystemTrayFrame.showBalloon( eventText );
			}

			if ( ChatManager.isRunning() )
			{
				int dash = eventText.indexOf( "-" );
				ChatManager.updateChat( "<font color=green>" + eventText.substring( dash + 2 ) + "</font>" );
			}
		}

		return true;
	}

	public static String checkForNewEvents( String responseText )
	{
		if ( responseText == null )
		{
			return null;
		}

		// Capture the entire new events table in order to display the
		// appropriate message.

		Matcher eventMatcher = EventManager.EVENT_PATTERN.matcher( responseText );
		if ( !eventMatcher.find() )
		{
			return responseText;
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

		// Remove the events from the response text

		responseText = eventMatcher.replaceFirst( "" );

		boolean shouldLoadEventFrame = false;

		for ( int i = 0; i < events.length; ++i )
		{
			shouldLoadEventFrame |= EventManager.addEvent( events[i], false );
		}

		shouldLoadEventFrame &= Preferences.getString( "initialFrames" ).indexOf( "RecentEventsFrame" ) != -1;

		// If we're not a GUI and there are no GUI windows open
		// (ie: the GUI loader command wasn't used), quit now.

		if ( StaticEntity.isHeadless() )
		{
			return responseText;
		}

		// If we are not running chat, pop up a RecentEventsFrame to
		// show the events.  Use the standard run method so that you
		// wait for it to finish before calling it again on another
		// event.

		if ( !ChatManager.isRunning() && shouldLoadEventFrame )
		{
			SwingUtilities.invokeLater( new CreateFrameRunnable( RecentEventsFrame.class ) );
		}

		return responseText;
	}
}
