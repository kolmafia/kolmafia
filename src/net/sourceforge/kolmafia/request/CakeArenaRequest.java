/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.CakeArenaManager;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CakeArenaRequest
	extends GenericRequest
{
	private static final Pattern WINCOUNT_PATTERN = Pattern.compile( "You have won (\\d*) time" );
	private static final Pattern OPPONENT_PATTERN =
		Pattern.compile( "<tr><td valign=center><input type=radio .*? name=whichopp value=(\\d+)>.*?<b>(.*?)</b> the (.*?)<br/?>(\\d*).*?</tr>" );
	private static final Pattern OPP_PATTERN = Pattern.compile( "whichopp=(\\d*)" );
	private static final Pattern EVENT_PATTERN = Pattern.compile( "event=(\\d*)" );

	private boolean isCompetition;
	private int eventId;

	private String [] results;
	private String suckage;

	public CakeArenaRequest()
	{
		super( "arena.php" );
		this.isCompetition = false;
	}

	public CakeArenaRequest( final int opponentId, final int eventId )
	{
		super( "arena.php" );
		this.addFormField( "action", "go" );
		this.addFormField( "whichopp", String.valueOf( opponentId ) );
		this.addFormField( "event", String.valueOf( eventId ) );

		this.isCompetition = true;
		this.eventId = eventId;
	}

	@Override
	public void run()
	{
		if ( !this.isCompetition )
		{
			KoLmafia.updateDisplay( "Retrieving opponent list..." );
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		if ( this.responseText.indexOf( "You can't" ) != -1 ||
		     this.responseText.indexOf( "You shouldn't" ) != -1 ||
		     this.responseText.indexOf( "You don't" ) != -1 ||
		     this.responseText.indexOf( "You need" ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Arena battles aborted!" );
			return;
		}
		else if ( this.responseText.indexOf( "You're way too beaten" ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You're way too beaten up, Arena battles aborted!" );
			return;
		}
		else if ( this.responseText.indexOf( "You're too drunk" ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You're too drunk, Arena battles aborted!" );
			return;
		}

		CakeArenaRequest.parseResponse( this.getURLString(), this.responseText );

		if ( this.isCompetition )
		{
			this.parseMatch();
		}
		else
		{
			KoLmafia.updateDisplay( "Opponent list retrieved." );
		}
	}

	public static boolean parseResults( final String responseText )
	{
		// The Baby Bugged Bugbear might get a free familiar item. If
		// so, it looks like you also get 3 lead necklaces. Nope.
		//
		// Congratulations on your %arenawins arena win. You've earned
		// a prize from the Arena Goodies Sack!

		FamiliarData familiar = KoLCharacter.getFamiliar();
		if ( familiar.getId() == FamiliarPool.BUGBEAR &&
		     responseText.indexOf( "Congratulations on your %arenawins arena win" ) != -1 )
		{
			return ResultProcessor.processItem( ItemPool.BUGGED_BEANIE, 1 );
		}

		return ResultProcessor.processResults( false, responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "action=go" ) != -1 )
		{
			if ( responseText.indexOf( "You don't have enough Meat" ) != -1 )
			{
				return;
			}

			ResultProcessor.processMeat( -100 );

			String suckage = CakeArenaRequest.parseSuckage( CakeArenaRequest.contestLines( responseText ) );
			if ( suckage != null )
			{
				RequestLogger.updateSessionLog( suckage );
			}

			String message = CakeArenaRequest.resultMessage( responseText );
			RequestLogger.updateSessionLog( message );

			if ( message.contains( "lost" ) )
			{
				KoLCharacter.setArenaWins( KoLCharacter.getArenaWins() + 1 );
			}

			return;
		}

		// Retrieve arena wins count

		// "You have won 722 times. Only 8 wins left until your next
		// prize!"

		Matcher winMatcher = CakeArenaRequest.WINCOUNT_PATTERN.matcher( responseText );

		if ( winMatcher.find() )
		{
			KoLCharacter.setArenaWins( StringUtilities.parseInt( winMatcher.group( 1 ) ) );
		}

		// Retrieve list of opponents

		Matcher opponentMatcher = CakeArenaRequest.OPPONENT_PATTERN.matcher( responseText );
		int lastMatchIndex = 0;

		while ( opponentMatcher.find( lastMatchIndex ) )
		{
			lastMatchIndex = opponentMatcher.end() + 1;
			int id = StringUtilities.parseInt( opponentMatcher.group( 1 ) );
			String name = opponentMatcher.group( 2 );
			String race = opponentMatcher.group( 3 );
			int weight = StringUtilities.parseInt( opponentMatcher.group( 4 ) );
			CakeArenaManager.registerOpponent( id, name, race, weight );
		}
	}

	public static final Pattern WIN_PATTERN = Pattern.compile( "is the winner, and gains (\\d+) experience" );

	public final int earnedXP()
	{
		return CakeArenaRequest.earnedXP( this.responseText );
	}

	private static final int earnedXP( final String responseText )
	{
		Matcher matcher = CakeArenaRequest.WIN_PATTERN.matcher( responseText );
		return matcher.find() ? Integer.valueOf( matcher.group( 1 ) ).intValue() : 0;
	}

	// You enter Gorg against Pork Soda in an Ultimate Cage Match.
	// Gorg is too busy being cute to fight very effectively.
	// Pork Soda is too busy being cute to fight very effectively.
	// Gorg struggles for 18 rounds, but is eventually knocked out.
	// Gorg lost.
	//    or
	// Gorg knocks Pork Soda out after 18 rounds.
	// Gorg is the winner, and gains 2 experience!

	// You enter Gonald against Citrus Maximus in a Scavenger Hunt.
	// Gonald has no eyes, and so is not exactly the best choice for this event.
	// Citrus Maximus has no eyes, and so is not exactly the best choice for this event.
	// Gonald finds 12 items from the list.
	// Citrus Maximus finds 12 items.
	// Gonald is the winner, and gains 5 experience!
	// <b>Gonald gains a pound!</b>
	// Congratulations on your 290th arena win.  You've earned a prize from the Arena Goodies Sack!

	// You enter Ton against Mr. Joe Bangles in an Obstacle Course race.
	// Ton is too short to get over most of the obstacles.
	// Mr. Joe Bangles is too short to get over most of the obstacles.
	// Ton makes it through the obstacle course in 199 seconds.
	// Mr. Joe Bangles takes 200 seconds.
	// Ton is the winner, and gains 5 experience!
	// <b>Ton gains a pound!</b>
	// Congratulations!  Only 1 more win until you get a prize from the Arena Goodies Sack!

	// You enter Trort against Pork Soda in a game of Hide and Seek.
	// Trort buzzes incessantly, making it very difficult to remain concealed.
	// Trort manages to stay hidden for 30 seconds.
	// Pork Soda stays hidden for 47 seconds.
	// Trort lost.

	private static final Pattern CONTEST_PATTERN =
		Pattern.compile( "<table><tr><td>(You enter.*?)</td></tr></table>" );

	private static String [] contestLines( final String responseText )
	{
		Matcher contestMatcher = CakeArenaRequest.CONTEST_PATTERN.matcher( responseText );
		return contestMatcher.find() ? contestMatcher.group( 1 ).split( "<p>" ) : null;
	}

	private void parseMatch()
	{
		this.results = CakeArenaRequest.contestLines( this.responseText );
		this.suckage = CakeArenaRequest.parseSuckage( this.results );
	}

	private static final Pattern ENTRY_PATTERN =
		Pattern.compile( "You enter (.*?) against (.*?) in (?:a game of|an|a) (.*?)(?: race)?\\." );

	private static String parseSuckage( String [] lines )
	{
		// Look for special "this familiar sucks" message. Note the
		// familiar can still win, even if such a message is present; a
		// match in which both familiars suck is given to either
		// contestant at random.

		// The first line is always "You enter X against Y in Z
		// The second line might be "your familiar sucks"
		// ... or "the other familiar sucks"
		// ... or "the first line of the contest result"

		// "Familiar suckage" messages do not always include the name
		// of the familiar. Fortunately, for the opponents in the cake
		// arena, they always do, and the message starts with the
		// opponent's name

		// Need at least 2 lines of results
		if ( lines == null || lines.length < 2 )
		{
			return null;
		}

		// Line 1 describes the contest
		String line1 = lines[ 0 ];
		Matcher m = CakeArenaRequest.ENTRY_PATTERN.matcher( line1 );
		if ( !m.find() )
		{
			return null;
		}

		String familiarName = m.group( 1 );
		String opponentName = m.group( 2 );
		int eventId = CakeArenaManager.eventNameToId( m.group( 3 ) );

		String line2 = lines[ 1 ];

		if ( line2.startsWith( opponentName ) )
		{
			// The other familiar sucks but not this one
			return null;
		}

		switch ( eventId )
		{
		case 1: 
			// Gorg struggles for 18 rounds, but is eventually knocked out.
			// Gorg knocks Pork Soda out after 18 rounds.
			if ( line2.contains( "is eventually knocked out" ) ||
			     ( line2.contains( "knocks" ) && line2.contains( "out after" ) ) )
			{
				return null;
			}
			break;
		case 2: 
			// Gonald finds 12 items from the list.
			if ( line2.contains( "items from the list" ) )
			{
				return null;
			}
			break;
		case 3: 
			// Ton makes it through the obstacle course in 199 seconds.
			if ( line2.contains( "makes it through the obstacle course" ) )
			{
				return null;
			}
			break;
		case 4: 
			// Trort manages to stay hidden for 30 seconds.
			if ( line2.contains( "manages to stay hidden for" ) )
			{
				return null;
			}
			break;
		}

		return new String( StringUtilities.globalStringReplace( line2, "<br>", " / " ) );
	}

	public final boolean badContest()
	{
		return this.suckage != null;
	}

	private static String resultMessage( final String responseText )
	{
		FamiliarData familiar = KoLCharacter.getFamiliar();
		int xp = CakeArenaRequest.earnedXP( responseText );

		if ( xp > 0 )
		{
			boolean gain = responseText.indexOf( "gains a pound" ) != -1;
			familiar.addNonCombatExperience( xp );
			return familiar.getName() + " gains " + xp + " experience" + ( gain ? " and a pound." : "." );
		}
		else
		{
			 return familiar.getName() + " lost.";
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "arena.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=go" ) == -1 )
		{
			return true;
		}

		FamiliarData familiar = KoLCharacter.getFamiliar();

		if ( familiar == FamiliarData.NO_FAMILIAR )
		{
			return true;
		}

		if ( KoLCharacter.getAvailableMeat() < 100 )
		{
			return true;
		}

		int opponent = CakeArenaRequest.getOpponent( urlString );
		if ( opponent < 0 )
		{
			return true;
		}

		int event = CakeArenaRequest.getEvent( urlString );
		if ( event < 0 )
		{
			return true;
		}

		CakeArenaManager.ArenaOpponent ao = CakeArenaManager.getOpponent( opponent );
		String eventName = CakeArenaManager.eventIdToName( event );

		String message1 = "[" + KoLAdventure.getAdventureCount() + "] Cake-Shaped Arena";

		String fam1 = familiar.getName() + ", the " + familiar.getModifiedWeight() + " lb. " + familiar.getRace();
		String fam2 = ao == null ? ( "opponent #" + opponent ) : ao.getName() + ", the " + ao.getWeight() + " lb. " + ao.getRace();

		String message2 = "Familiar: " + fam1;
		String message3 = "Opponent: " + fam2;
		String message4 = "Contest: " + eventName;

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message1 );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message1 );
		RequestLogger.updateSessionLog( message2 );
		RequestLogger.updateSessionLog( message3 );
		RequestLogger.updateSessionLog( message4 );

		return true;
	}

	private static int getOpponent( final String urlString )
	{
		Matcher matcher = OPP_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group(1) ) : -1;
	}

	private static int getEvent( final String urlString )
	{
		Matcher matcher = EVENT_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group(1) ) : -1;
	}

	@Override
	public String toString()
	{
		return "Arena Battle";
	}

	@Override
	public int getAdventuresUsed()
	{
		return this.isCompetition ? 1 : 0;
	}
}
