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
	private static final LockableListModel<String> eventTexts = new LockableListModel<String>();
	private static final LockableListModel<String> eventHyperTexts = new LockableListModel<String>();

	public static final Pattern EVENT_PATTERN1 =
		Pattern.compile( "<table[^>]*><tr><td[^>]*bgcolor=orange><b>New Events:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid orange;\"><center><table><tr><td>(.*?)</td></tr></table>.*?<td height=4></td></tr></table>" );

	public static final Pattern EVENT_PATTERN2 =
		Pattern.compile( "<table[^>]*><tr><td[^>]*bgcolor=orange><b>New Events:</b></td></tr><tr><td style=\"padding: 5px; border: 1px solid orange;\" align=center>(.*?)</td></tr><tr><td height=4></td></tr></table>" );

	private static final SimpleDateFormat EVENT_TIMESTAMP = new SimpleDateFormat( "MM/dd/yy hh:mm a", Locale.US );

	public static Matcher eventMatcher( final String responseText )
	{
		Matcher matcher = EventManager.EVENT_PATTERN1.matcher( responseText );
		if ( matcher.find() )
		{
			return matcher;
		}

		matcher = EventManager.EVENT_PATTERN2.matcher( responseText );
		if ( matcher.find() )
		{
			return matcher;
		}
		return null;
	}

	public static boolean hasEvents()
	{
		return !EventManager.eventTexts.isEmpty();
	}

	public static void clearEventHistory()
	{
		EventManager.eventTexts.clear();
		EventManager.eventHyperTexts.clear();
	}

	public static LockableListModel<String> getEventTexts()
	{
		return EventManager.eventTexts;
	}

	public static LockableListModel<String> getEventHyperTexts()
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

		// The event may be marked up with color and links to
		// user profiles. For example:

		// 04/25/06 12:53:54 PM - New message received from <a target=mainpane href='showplayer.php?who=115875'><font color=green>Brianna</font></a>.
		// 04/25/06 01:06:43 PM - <a class=nounder target=mainpane href='showplayer.php?who=115875'><b><font color=green>Brianna</font></b></a> has played a song (The Polka of Plenty) for you.

		// Remove tags that are not hyperlinks

		eventHTML = eventHTML.replaceAll( "</[^aA][^>]*>", "" );
		eventHTML = eventHTML.replaceAll( "<[^aA/][^>]*>", "" );

		String eventText = eventHTML.replaceAll( "<a[^>]*showplayer\\.php\\?who=(\\d+)[^>]*>(.*?)</a>", "$2 (#$1)" );

		eventText = eventText.replaceAll( "<.*?>", "" );

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

		Matcher eventMatcher = EventManager.eventMatcher( responseText );
		if ( eventMatcher == null )
		{
			return;
		}

		// Make an array of events
		String allEvents = eventMatcher.group( 1 );
		int para = allEvents.indexOf( "<p>" );
		String normalEvents = para == -1 ? allEvents : allEvents.substring( 0, para );
		String otherEvents = para == -1 ? "" : allEvents.substring( para );

		normalEvents = normalEvents.replaceAll( "<br />", "<br>" );
		normalEvents = normalEvents.replaceAll( "<br>", "\n" );

		String[] events = normalEvents.split( "\n" );

		for ( String event : events )
		{
			if ( !event.contains( "/" ) )
			{
				continue;
			}

			EventManager.addNormalEvent( event );

			if ( ChatManager.isRunning() )
			{
				ChatManager.broadcastEvent( new EventMessage( event, "green" ) );
			}
		}
	}

}
