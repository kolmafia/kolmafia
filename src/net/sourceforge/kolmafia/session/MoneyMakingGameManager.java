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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MoneyMakingGameRequest;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoneyMakingGameManager
{
	// To do:
	//
	// Track Meat when you take a bet
	//
	// Make sure mmg_take_bet() works correctly.
	//
	// Don't bother saving bets on resolved unless the bet was submitted
	// via ASH and there is therefore a chance that the event will be
	// picked up by the script.

	public static final Pattern PENDING_BETS_PATTERN = Pattern.compile( "Your Pending Bets:.*?<table>(.*?)</table>" );
	public static final Pattern MY_BET_PATTERN = Pattern.compile( "<tr>.*?([0123456789,]+) Meat.*?betid value='(\\d*)'.*?</tr>" );

	public static final Pattern RECENT_BETS_PATTERN = Pattern.compile( "(Last 20 Bets|Bets Found).*?<table.*?>(.*?)</table>" );
	public static final Pattern OFFERED_BET_PATTERN = Pattern.compile( "<tr>.*?showplayer.*?<b>(.*?)</b> \\(#(\\d*)\\).*?([0123456789,]+) Meat.*?whichbet value='(\\d*)'.*?</tr>" );

	// Babycakes (#311877) took your 1,000 Meat bet, and you lost. Better luck next time.
	// Babycakes (#311877) took your 1,000 Meat bet, and you won, earning you 1,998 Meat.

	public static final Pattern EVENT_PATTERN = Pattern.compile( "- (.*) \\(#(\\d+)\\) took your ([1234567890,]*) Meat bet, and you (won|lost)(, earning you ([0123456789,]*) Meat)?" );

	// Current bets offered by others
	private static ArrayList offered = new ArrayList();

	// The amount I won or lost from the last bet I took
	private static int lastWinnings = 0;

	// Active bets I've made
	private static ArrayList active = new ArrayList();

	// Bets I've made that are taken with no notification yet
	private static LinkedList taken = new LinkedList();

	// The last bet I made
	private static Bet lastBet = null;

	// Events received but not yet matched
	private static LinkedList received = new LinkedList();

	// Events received and matched to taken bets
	private static LinkedList resolved = new LinkedList();

	// The last event handled
	private static Event lastEvent = null;

	// The following are needed to detect bets that are taken before we are
	// able to learn the bet ID and register them.

	// Initial ID for a dummy bet: already gone before KoL returns the list
	// of current bets.
	private static int dummyBetId = -1;

	// The amount of the bet we are in the process of submitting
	public static int makingBet = 0;

	public static void reset()
	{
		MoneyMakingGameManager.offered.clear();
		MoneyMakingGameManager.lastWinnings = 0;
		MoneyMakingGameManager.makingBet = 0;
		MoneyMakingGameManager.dummyBetId = -1;
		MoneyMakingGameManager.active.clear();
		MoneyMakingGameManager.taken.clear();
		MoneyMakingGameManager.lastBet = null;
		MoneyMakingGameManager.received.clear();
		MoneyMakingGameManager.resolved.clear();
		MoneyMakingGameManager.lastEvent = null;
	}

	public static final Bet getLastBet()
	{
		return MoneyMakingGameManager.lastBet;
	}

	public static final int getLastBetId()
	{
		Bet bet = MoneyMakingGameManager.lastBet;
		return bet == null ? 0 : bet.getId();
	}

	private static final int [] getBets( final List list )
	{
		Iterator it = list.iterator();
		int [] bets = new int[ list.size() ];
		int index = 0;
		while ( it.hasNext() )
		{
			Bet bet = (Bet) it.next();
			bets[ index++ ] = bet.getId();
		}
		return bets;
	}

	private static final Bet findBet( final int id, List list )
	{
		int [] bets = new int[ list.size() ];
		Iterator it = list.iterator();
		int index = 0;
		while ( it.hasNext() )
		{
			Bet bet = (Bet) it.next();
			if ( bet.getId() == id )
			{
				return bet;
			}
		}
		return null;
	}

	public static final int [] getOfferedBets()
	{
		return MoneyMakingGameManager.getBets( offered );
	}

	public static final void parseOfferedBets( final String responseText )
	{
		// Find all currently active bets
		MoneyMakingGameManager.offered.clear();
		Matcher recent = RECENT_BETS_PATTERN.matcher( responseText );
		if ( recent.find() )
		{
			Matcher betMatcher = OFFERED_BET_PATTERN.matcher( recent.group( 2 ) );
			while ( betMatcher.find() )
			{
				String player = betMatcher.group( 1 );
				int playerId = StringUtilities.parseInt( betMatcher.group( 2 ) );
				int amount = StringUtilities.parseInt( betMatcher.group( 3 ) );
				int betid = StringUtilities.parseInt( betMatcher.group( 4 ) );
				Bet bet = new Bet( betid, amount, player, playerId );
				// Add to offered list
				MoneyMakingGameManager.offered.add( bet );
			}
		}
	}

	public static final int [] getActiveBets()
	{
		return MoneyMakingGameManager.getBets( active );
	}

	public static final void parseMyBets( final String responseText )
	{
		// Constructed list of currently outstanding bets
		ArrayList current = new ArrayList();

		// Assume there is no newly placed bet
		MoneyMakingGameManager.lastBet = null;

		// Find all currently active bets
		Matcher pending = PENDING_BETS_PATTERN.matcher( responseText );
		if ( pending.find() )
		{
			Matcher betMatcher = MY_BET_PATTERN.matcher( pending.group( 1 ) );
			while ( betMatcher.find() )
			{
				int amount = StringUtilities.parseInt( betMatcher.group( 1 ) );
				int betId = StringUtilities.parseInt( betMatcher.group( 2 ) );

				Bet bet = MoneyMakingGameManager.findBet( betId, MoneyMakingGameManager.active );
				if ( bet == null )
				{
					// This is a new bet
					bet = new Bet( betId, amount );
					MoneyMakingGameManager.lastBet = bet;
				}

				current.add( bet );
			}
		}

		// Move any bets that are gone to taken
		int count = MoneyMakingGameManager.active.size();
		for ( int i = 0; i < count; ++i )
		{
			Bet bet = (Bet) MoneyMakingGameManager.active.get( i );
			if ( current.indexOf( bet) == -1 )
			{
				// Bet is gone. Move to taken
				MoneyMakingGameManager.handleTakenBet( bet );
			}
		}

		// KoL redirects us from the URL that submitted the bet to,
		// simply, bet.php. It is possible for our bet to be taken
		// before we get the response from that page. When this
		// happens, we will not detect a new bet.

		if ( MoneyMakingGameManager.makingBet != 0 && MoneyMakingGameManager.lastBet == null )
		{
			// Make a dummy bet to match with the eventual event -
			// which could have arrived already, too.
			Bet bet = new Bet( MoneyMakingGameManager.dummyBetId--,
					   MoneyMakingGameManager.makingBet );
			MoneyMakingGameManager.lastBet = bet;
			MoneyMakingGameManager.handleTakenBet( bet );
		}

		// Finally, save new list of active bets
		MoneyMakingGameManager.active = current;
	}

	private static final void handleTakenBet( final Bet bet )
	{
		Event ev = MoneyMakingGameManager.findMatchingEvent( bet.getAmount(), MoneyMakingGameManager.received );
		if ( ev != null )
		{
			MoneyMakingGameManager.received.remove( ev );
			MoneyMakingGameManager.resolveEvent( ev, bet );
		}
		else
		{
			MoneyMakingGameManager.taken.add( bet );
		}
	}

	private static final void resolveEvent( final Event ev, final Bet bet )
	{
		ev.setBet( bet );
		synchronized ( MoneyMakingGameManager.resolved )
		{
			MoneyMakingGameManager.resolved.add( ev );
			MoneyMakingGameManager.resolved.notify();
		}
	}

	private static final Bet findBet( final int id )
	{
		Bet bet = MoneyMakingGameManager.findBet( id, offered );
		if ( bet != null )
		{
			return bet;
		}
		bet = MoneyMakingGameManager.findBet( id, active );
		if ( bet != null )
		{
			return bet;
		}
		bet = MoneyMakingGameManager.findBet( id, taken );
		if ( bet != null )
		{
			return bet;
		}
		bet = MoneyMakingGameManager.getLastEventBet();
		if ( bet != null && bet.getId() == id )
		{
			return bet;
		}
		return null;
	}

	public static final String betOwner( final int id )
	{
		Bet bet = MoneyMakingGameManager.findBet( id );
		return bet == null ? "" : bet.getPlayer();
	}

	public static final int betOwnerId( final int id )
	{
		Bet bet = MoneyMakingGameManager.findBet( id );
		return bet == null ? 0 : bet.getPlayerId();
	}

	public static final int betAmount( final int id )
	{
		Bet bet = MoneyMakingGameManager.findBet( id );
		return bet == null ? 0 : bet.getAmount();
	}

	public static final void makeBet( final String responseText )
	{
		Bet bet = MoneyMakingGameManager.lastBet;
		if ( bet == null )
		{
			// Uh oh.
			return;
		}

		int amount = bet.getAmount();
		if ( responseText.indexOf( "Meat has been taken from Hagnk's" ) != -1 )
		{
			bet.setFromStorage( true );
			KoLCharacter.addStorageMeat( -amount );
		}
		else
		{
			ResultProcessor.processMeat( -amount );
		}
	}

	public static final void retractBet( final String urlString, final String responseText )
	{
		// See if we succeeded in retracting the bid
		if ( responseText.indexOf( "You retract your bid" ) == -1 )
		{
			return;
		}

		// Get the bet id
		int betId = MoneyMakingGameRequest.getBetId( urlString );
		if ( betId < 0 )
		{
			return;
		}

		// Find the bet on the "taken" list, since it was moved there
		// when we didn't find it on the list of active bets.
		Bet bet = MoneyMakingGameManager.findBet( betId, MoneyMakingGameManager.taken );
		if ( bet == null )
		{
			// Internal error
			return;
		}

		int amount = bet.getAmount();
		// Put back meat to wherever it came from
		if ( bet.fromStorage() )
		{
			// Add meat to storage
			KoLCharacter.addStorageMeat( amount );
		}
		else
		{
			// Add meat to inventory
			ResultProcessor.processMeat( amount );
		}

		// Remove the bet from the taken list
		int index = MoneyMakingGameManager.taken.indexOf( bet );
		MoneyMakingGameManager.taken.remove( index );
	}

	public static final void takeBet( final String urlString, final String responseText )
	{
		MoneyMakingGameManager.lastWinnings = 0;

		// bet.php?action=bet&whichbet=58251231&from=0&confirm=on&pwd

		// The old man looks at you quizzically. &quot;There's
		// no bet like that anywhere in our records. Maybe
		// someone else got to it before you could.&quot;

		// <td valign=center>You bet 5,000 Meat.</td>
		//
		// &quot;Tough luck, kid. Maybe next time.&quot;
		//    or
		// You gain 15,984 Meat.

		// You take the 1,000 bet from Spunky Monkey. You look
		// nervously at the old man, and he nods at you, indicating
		// that Hagnk's has cleared you for an expenditure of 1,000
		// Meat.
		//
		// &quot;Tough luck, kid. Maybe next time.&quot;
		//
		// &quot;Congratulations,&quot; says the old man. &quot;I'll
		// call Godot up and have him deliver 1,998 Meat to your
		// account immediately.&quot;

		String from = MoneyMakingGameRequest.getFromString( urlString );
		int whichbet = MoneyMakingGameRequest.getWhichBet( urlString );
		if ( from == null || whichbet < 0 )
		{
			return;
		}

		return;
	}

	public static final int getLastWinnings()
	{
		return MoneyMakingGameManager.lastWinnings;
	}

	public static final void processEvent( final String eventText )
	{
		Matcher matcher = EVENT_PATTERN.matcher( eventText );
		if ( !matcher.find() )
		{
			return;
		}

		String player = matcher.group( 1 );
		int playerId = StringUtilities.parseInt( matcher.group( 2 ) );
		int amount = StringUtilities.parseInt( matcher.group( 3 ) );
		boolean won = matcher.group( 4 ).equals( "won" );
		int winnings = matcher.group( 5 ) != null ? StringUtilities.parseInt( matcher.group( 6 ) ) : 0;
		boolean storage = eventText.indexOf( "Hagnk's" ) != -1;

		if ( won )
		{
			// Add meat to wherever it goes
			if ( storage )
			{
				// Add meat to storage
				KoLCharacter.addStorageMeat( winnings );
			}
			else
			{
				// Add meat to inventory
				ResultProcessor.processMeat( winnings );
			}
		}

		Event ev = new Event( player, playerId, amount, winnings );

		// A matching bet on the "taken" list goes with this event. We
		// resolve the first match. There can be more than one and we
		// can't tell which one went with this event, but it doesn't
		// matter.
		Bet bet = MoneyMakingGameManager.findMatchingBet( amount, MoneyMakingGameManager.taken );
		if ( bet != null )
		{
			MoneyMakingGameManager.taken.remove( bet );
			MoneyMakingGameManager.resolveEvent( ev, bet );
			return;
		}

		// A matching bet on the "active" list goes with this
		// event. There can be more than one, but we can't tell which
		// one went with this event - and it does matter. We have to
		// wait until the bet is moved to the "taken" list.
		bet = MoneyMakingGameManager.findMatchingBet( amount, MoneyMakingGameManager.active );
		if ( bet != null )
		{
			MoneyMakingGameManager.received.add( ev );
			return;
		}

		// If chat is active, the event signaling the taking of a bet
		// can arrive before the response from the request that
		// submitted it.
		if ( MoneyMakingGameManager.makingBet != 0 )
		{
			MoneyMakingGameManager.received.add( ev );
			return;
		}

		// No matching active or taken bet. Drop event.
	}

	private static final Event findMatchingEvent( final int amount, final List list )
	{
		Iterator it = list.iterator();
		while ( it.hasNext() )
		{
			Event ev = (Event) it.next();
			if ( amount == ev.getAmount() )
			{
				return ev;
			}
		}
		return null;
	}

	private static final Bet findMatchingBet( final int amount, final List list )
	{
		Iterator it = list.iterator();
		while ( it.hasNext() )
		{
			Bet bet = (Bet) it.next();
			if ( amount == bet.getAmount() )
			{
				return bet;
			}
		}
		return null;
	}

	public static final int getNextEvent( final int ms )
	{
		// Wait up to specified number of seconds to get an event.
		// Return bet ID, or 0 if timeout

		boolean waited = false;
		while ( true )
		{
			// If we have any resolved events, take them first
			synchronized ( MoneyMakingGameManager.resolved )
			{
				if ( !MoneyMakingGameManager.resolved.isEmpty() )
				{
					Event ev = (Event) MoneyMakingGameManager.resolved.remove( 0 );
					MoneyMakingGameManager.lastEvent = ev;
					return ev.getBetId();
				}
			}

			if ( MoneyMakingGameManager.taken.isEmpty() )
			{
				// If we have no taken bets but we do have an
				// unresolved event, visit the bet page to
				// resolve the appropriate active bet.
				if ( !MoneyMakingGameManager.received.isEmpty() )
				{
					RequestThread.postRequest( new MoneyMakingGameRequest() );
					continue;
				}

				// If we have no active bets, no events will
				// come
				if ( MoneyMakingGameManager.active.isEmpty() )
				{
					return 0;
				}
			}

			// If we have already waited and found nothing, punt
			if ( waited )
			{
				return 0;
			}

			// Otherwise, wait
			synchronized ( MoneyMakingGameManager.resolved )
			{
				try
				{
					MoneyMakingGameManager.resolved.wait( ms );
				}
				catch ( InterruptedException e )
				{
				}
			}

			// Mark that we have waited
			waited = true;

			// If we still have no events, it's possible that chat
			// is not active and we just haven't detected any.
			if ( MoneyMakingGameManager.received.isEmpty() &&
			     MoneyMakingGameManager.resolved.isEmpty() )
			{
				RequestThread.postRequest( new GenericRequest( "main.php" ) );
			}
		}
	}

	public static final Event getLastEvent()
	{
		return MoneyMakingGameManager.lastEvent;
	}

	public static final Bet getLastEventBet()
	{
		Event event = MoneyMakingGameManager.lastEvent;
		return event == null ? null : event.getBet();
	}

	public static final int getLastEventBetId()
	{
		Event event = MoneyMakingGameManager.lastEvent;
		return event == null ? 0 : event.getBetId();
	}

	public static final String getLastEventPlayer()
	{
		Event event = MoneyMakingGameManager.lastEvent;
		return event == null ? "" : event.getPlayer();
	}

	public static final int getLastEventPlayerId()
	{
		Event event = MoneyMakingGameManager.lastEvent;
		return event == null ? 0 : event.getPlayerId();
	}

	public static final int getLastEventWinnings()
	{
		Event event = MoneyMakingGameManager.lastEvent;
		return event == null ? 0 : event.getWinnings();
	}

	public static class Bet
		implements Comparable
	{
		private final int betId;
		private final int amount;
		private final String player;
		private final int playerId;
		private boolean fromStorage;

		public Bet( final int betId, final int amount, final String player, final int playerId )
		{
			this.betId = betId;
			this.amount = amount;
			this.player = player;
			this.playerId = playerId;
			this.fromStorage = false;
		}

		public Bet( final int betId, final int amount )
		{
			this( betId, amount, KoLCharacter.getUserName(), KoLCharacter.getUserId() );
		}

		public int getId()
		{
			return this.betId;
		}

		public int getAmount()
		{
			return this.amount;
		}

		public String getPlayer()
		{
			return this.player;
		}

		public int getPlayerId()
		{
			return this.playerId;
		}

		public boolean fromStorage()
		{
			return this.fromStorage;
		}

		public void setFromStorage( final boolean fromStorage )
		{
			this.fromStorage = fromStorage;
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof Bet ) )
			{
				return -1;
			}

			return this.betId - ((Bet) o).betId;
		}

		public String toString()
		{
			return "bet(" + this.betId + ", " + this.player + ", " + this.playerId + ", " + this.amount + ")";
		}
	}

	public static class Event
		implements Comparable
	{
		private Bet bet;
		private final String player;
		private final int playerId;
		private final int amount;
		private final int winnings;

		public Event( final String player, final int playerId, final int amount, final int winnings )
		{
			this.bet = null;
			this.player = player;
			this.playerId = playerId;
			this.amount = amount;
			this.winnings = winnings;
		}

		public Bet getBet()
		{
			return this.bet;
		}

		public int getBetId()
		{
			return this.bet == null ? 0 : this.bet.getId();
		}

		public void setBet( final Bet bet )
		{
			this.bet = bet;
		}

		public String getPlayer()
		{
			return this.player;
		}

		public int getPlayerId()
		{
			return this.playerId;
		}

		public int getAmount()
		{
			return this.amount;
		}

		public int getWinnings()
		{
			return this.winnings;
		}

		public int compareTo( final Object o )
		{
			if ( !( o instanceof Event ) )
			{
				return -1;
			}

			if ( this.bet == null )
			{
				return 1;
			}

			return this.bet.compareTo( ((Event) o).bet );
		}

		public String toString()
		{
			return "event(" + this.getBetId() + ", " + this.player + ", " + this.playerId + ", " + this.winnings + ")";
		}
	}
}
