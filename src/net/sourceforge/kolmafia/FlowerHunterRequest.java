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

package net.sourceforge.kolmafia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlowerHunterRequest extends KoLRequest
{
	public static final String [] WIN_MESSAGES = new String []
	{
		"50 CHARACTER LIMIT BREAK!",
		"HERE'S YOUR CHEETO, MOTHER!*$#ER.",
		"If you want it back, I'll be in my tent.",
		"PWNED LIKE CRAPSTORM."
	};

	public static final String [] LOSE_MESSAGES = new String []
	{
		"OMG HAX H4X H5X!!",
		"Please return my pants.",
		"How do you like my Crotch-To-Your-Foot style?",
		"PWNED LIKE CRAPSTORM."
	};

	private static int tattooCount = -1;
	private static int trophyCount = -1;
	private static int flowerCount = -1;
	private static int canadaCount = -1;

	private static final Pattern TATTOO_PATTERN = Pattern.compile( "You have unlocked (\\d+) <a class=nounder href=\"account_tattoos.php\">" );
	private static final Pattern TROPHY_PATTERN = Pattern.compile( "You have earned (\\d+) <a class=nounder href=\"trophies.php\">" );
	private static final Pattern FLOWER_PATTERN = Pattern.compile( "You have picked ([\\d,]+) pretty flower" );
	private static final Pattern CANADA_PATTERN = Pattern.compile( "white Canadian</a>&nbsp;&nbsp;&nbsp;</td><td>([\\d,]+)</td>" );

	private static final int RANKVIEW = 0;
	private static final int ATTACK = 1;
	private static final int PLAYER_SEARCH = 2;
	private static final int CLAN_PROFILER = 3;

	private static final Pattern ATTACKS_PATTERN = Pattern.compile( "You may participate in (\\d+) more player fights today" );

	private static final Pattern TARGET_PATTERN =
		Pattern.compile( "showplayer\\.php\\?who=(\\d+)\">(.*?)</a></b>  \\(PvP\\)(<br>\\(<a target=mainpane href=\"showclan\\.php\\?whichclan=\\d+\">(.*?)</a>)?.*?<td.*?><td.*?>(\\d+)</td><td.*?>(.*?)</td><td.*?>(\\d+)" );

	private static final Pattern CLAN_PATTERN =
		Pattern.compile( "showplayer\\.php\\?who=(\\d+)\">([^<]*?)</a></b>[^<]*?</td><td class=small>[^<]*?</td><td class=small>\\d+ \\(H\\)" );

	private static final Pattern RANKING_PATTERN = Pattern.compile( "Your current PvP Ranking is (\\d+)" );

	private int hunterType;
	private List searchResults = new ArrayList();

	public FlowerHunterRequest()
	{
		super( "pvp.php" );
		this.hunterType = RANKVIEW;
	}

	public FlowerHunterRequest( String level, String rank )
	{
		super( "searchplayer.php" );
		this.hunterType = PLAYER_SEARCH;

		addFormField( "searching", "Yep." );
		addFormField( "searchstring", "" );
		addFormField( "searchlevel", level );
		addFormField( "searchranking", rank );

		addFormField( "pvponly", "on" );
		addFormField( "hardcoreonly", KoLCharacter.isHardcore() ? "1" : "2" );
	}

	public FlowerHunterRequest( String opponent, int stance, String mission, String win, String lose )
	{
		super( "pvp.php" );
		this.hunterType = ATTACK;

		addFormField( "action", "Yep." );
		addFormField( "pwd" );
		addFormField( "who", opponent );
		addFormField( "stance", String.valueOf( stance ) );
		addFormField( "attacktype", mission );

		if ( win.equals( "" ) )
			win = WIN_MESSAGES[ RNG.nextInt( WIN_MESSAGES.length ) ];
		if ( lose.equals( "" ) )
			lose = LOSE_MESSAGES[ RNG.nextInt( LOSE_MESSAGES.length ) ];

		addFormField( "winmessage", win );
		addFormField( "losemessage", lose );

		StaticEntity.setProperty( "defaultFlowerWinMessage", win );
		StaticEntity.setProperty( "defaultFlowerLossMessage", lose );
	}

	public FlowerHunterRequest( String clanId )
	{
		super( "showclan.php" );
		this.hunterType = CLAN_PROFILER;

		addFormField( "whichclan", clanId );
	}

	public void setTarget( String target )
	{	addFormField( "who", target );
	}

	public List getSearchResults()
	{	return searchResults;
	}

	public void processResults()
	{
		switch ( hunterType )
		{
		case RANKVIEW:

			parseAttack();

			KoLRequest miniRequest = new KoLRequest( "questlog.php?which=3" );
			miniRequest.run();

			Matcher miniMatcher = TATTOO_PATTERN.matcher( miniRequest.responseText );
			if ( miniMatcher.find() )
				tattooCount = StaticEntity.parseInt( miniMatcher.group(1) );
			else
				tattooCount = 0;

			miniMatcher = TROPHY_PATTERN.matcher( miniRequest.responseText );
			if ( miniMatcher.find() )
				trophyCount = StaticEntity.parseInt( miniMatcher.group(1) );
			else
				trophyCount = 0;

			miniMatcher = FLOWER_PATTERN.matcher( miniRequest.responseText );
			if ( miniMatcher.find() )
				flowerCount = StaticEntity.parseInt( miniMatcher.group(1) );
			else
				flowerCount = 0;

			miniRequest = new KoLRequest( "showconsumption.php" );
			miniRequest.run();

			miniMatcher = CANADA_PATTERN.matcher( miniRequest.responseText );
			if ( miniMatcher.find() )
				canadaCount = StaticEntity.parseInt( miniMatcher.group(1) );
			else
				canadaCount = 0;

			break;

		case ATTACK:
			parseAttack();
			break;

		case PLAYER_SEARCH:
			parseSearch();
			break;

		case CLAN_PROFILER:
			parseClan();
			break;
		}
	}

	private void parseClan()
	{
		ProfileRequest currentPlayer;
		Matcher playerMatcher = CLAN_PATTERN.matcher( responseText );

		while ( playerMatcher.find() )
		{
			KoLmafia.registerPlayer( playerMatcher.group(2), playerMatcher.group(1) );
			searchResults.add( new ProfileRequest( playerMatcher.group(2) ) );
		}
	}

	private void parseSearch()
	{
		if ( responseText.indexOf( "<br>No players found.</center>" ) != -1 )
			return;

		ProfileRequest currentPlayer;
		Matcher playerMatcher = TARGET_PATTERN.matcher( responseText );

		while ( playerMatcher.find() )
		{
			KoLmafia.registerPlayer( playerMatcher.group(2), playerMatcher.group(1) );
			currentPlayer = ProfileRequest.getInstance( playerMatcher.group(2), playerMatcher.group(1),
				playerMatcher.group(4), Integer.valueOf( playerMatcher.group(5) ), playerMatcher.group(6),
				Integer.valueOf( playerMatcher.group(7) ) );

			searchResults.add( currentPlayer );
		}

		Collections.sort( searchResults );
	}

	private void parseAttack()
	{
		// Reset the player's current PvP ranking

		Matcher attacksMatcher = ATTACKS_PATTERN.matcher( responseText );
		if ( attacksMatcher.find() )
			KoLCharacter.setAttacksLeft( StaticEntity.parseInt( attacksMatcher.group(1) ) );
		else
			KoLCharacter.setAttacksLeft( 0 );

		Matcher rankMatcher = RANKING_PATTERN.matcher( responseText );
		if ( !rankMatcher.find() )
		{
			(new KoLRequest( "campground.php?pwd&confirm=on&smashstone=Yep." )).run();
			super.run();
			return;
		}

		KoLCharacter.setPvpRank( StaticEntity.parseInt( rankMatcher.group(1) ) );

		// Trim down the response text so it only includes
		// the information related to the fight.

		int index = responseText.indexOf( "<p>Player to attack" );
		responseText = responseText.substring( 0, index == -1 ? responseText.length() : index );

		if ( hunterType != RANKVIEW )
		{
			processOffenseContests( responseText );
			StaticEntity.getClient().showHTML( responseText, null );
		}
		else
		{
		}
	}

	public static void processDefenseContests( String responseText )
	{
	}

	public static void processOffenseContests( String responseText )
	{
		String resultText = StaticEntity.globalStringReplace( responseText.substring(
			responseText.indexOf( "<td>" ) + 4, responseText.indexOf( "Your PvP Ranking" ) ), "<p>", LINE_BREAK );

		resultText = ANYTAG_PATTERN.matcher( resultText.substring( 0, resultText.lastIndexOf( "<b>" ) ) ).replaceAll( "" );

		String [] fightData = resultText.split( "\n" );
		String target = null;

		for ( int i = 0; i < fightData.length; ++i )
		{
			if ( fightData[i].startsWith( "You call" ) )
			{
				target = fightData[i].substring( 9, fightData[i].indexOf( " out," ) );
				fightData[i] = null;
				break;
			}

			fightData[i] = null;
		}

		LogStream pvpResults = LogStream.openStream( "attacks/" + "rawdata.txt", false );

		pvpResults.println();
		pvpResults.println( new Date() );
		pvpResults.println( KoLCharacter.getUserName() + " initiated a PvP attack against " + target + "." );
		pvpResults.println( "(" + tattooCount + " tattoos, " + trophyCount + " trophies, " +
			flowerCount + " flowers, " + canadaCount + " white canadians)" );

		pvpResults.println();

		for ( int i = 0; i < fightData.length; ++i )
			if ( fightData[i] != null )
				processOffenseContest( target, fightData[i], pvpResults );

		pvpResults.println();
		pvpResults.println();
		pvpResults.close();
	}

	public static void processOffenseContest( String target, String line, LogStream ostream )
	{
		String contest = null;

		// Messages for the battle stance that the player selected
		// for their attack.

		if ( line.startsWith( "You attempt to Burninate" ) )
			contest = "Buffed Mysticality";
		else if ( line.startsWith( "You challenge your opponent to a game of Telekinetic Ping-Pong" ) )
			contest = "Unbuffed Mysticality";
		else if ( line.startsWith( "You try to embarrrass" ) )
			contest = "Buffed Moxie";
		else if ( line.startsWith( "You challenge your opponent to an insult contest" ) )
			contest = "Unbuffed Moxie";

		// Now the messages you get for the remaining five minis
		// that are randomly selected by KoL.  Start with the three
		// stat minis that KoL randomly selects.

		else if ( line.indexOf( "challenges you to an armwrestling match" ) != -1 )
			contest = "Buffed Muscle";
		else if ( line.indexOf( "challenges you to a game of Wizard's Croquet" ) != -1 )
			contest = "Buffed Mysticality";
		else if ( line.indexOf( "challenges you to a dancing contest" ) != -1 )
			contest = "Buffed Moxie";

		// There's a giant list for the remaining minis.  Go ahead
		// and list them here.

		else if ( line.indexOf( "challenges you to a diet balance contest" ) != -1 )
			contest = "Balanced Diet";
		else if ( line.indexOf( "challenges you to a bleeding contest" ) != -1 )
			contest = "Bleeding Contest";
		else if ( line.indexOf( "challenges you to a burping contest" ) != -1 )
			contest = "Burping Contest";
		else if ( line.indexOf( "challenges you to a Canadianity contest" ) != -1 )
			contest = "Canadianity Contest";
		else if ( line.indexOf( "challenges you to a familiar show" ) != -1 )
			contest = "Familiar Weight";
		else if ( line.indexOf( "arranges an impromptu fashion show" ) != -1 )
			contest = "Fashion Show";
		else if ( line.indexOf( "challenges you to a flower-picking contest" ) != -1 )
			contest = "Flower Picking Contest";
		else if ( line.indexOf( "challenges you to a &quot;How Hung Over are You?&quot; competition" ) != -1 )
			contest = "\"How Hung Over are You?\"";
		else if ( line.indexOf( "challenges you to a pie-eating competition" ) != -1 )
			contest = "Pie-Eating Contest";
		else if ( line.indexOf( "challenges you to a popularity contest" ) != -1 )
			contest = "Popularity Contest";
		else if ( line.indexOf( "challenges you to a purity test" ) != -1 )
			contest = "Purity Test";
		else if ( line.indexOf( "challenges you to a tattoo contest" ) != -1 )
			contest = "Tattoo Contest";
		else if ( line.indexOf( "challenges you to a trophy-stacking contest" ) != -1 )
			contest = "Trophy Contest";
		else if ( line.indexOf( "challenges you to a wine tasting contest" ) != -1 )
			contest = "Wine Tasting Contest";
		else if ( line.indexOf( "challenges you to a work ethic contest" ) != -1 )
			contest = "Work Ethic Contest";

		// If it's not one of the above, then just note it as an
		// unknown contest for later.

		else
			contest = "Unknown Contest";

		String lastMessage = line.substring( line.lastIndexOf( " " ) + 1, line.length() - 2 );
		boolean isWinner = lastMessage.toUpperCase().equals( lastMessage );

		String result = contest + ": You " + (isWinner ? "won." : "lost.");
		ostream.println( result );

		recordContestResult( target, result );
	}

	public static void recordContestResult( String opponent, String result )
	{
	}

	public static boolean registerRequest( String urlString )
	{
		if ( !urlString.startsWith( "pvp.php" ) )
			return false;

		int whoIndex = urlString.indexOf( "who=" );
		if ( whoIndex == -1 )
			return true;

		String target = urlString.substring( whoIndex + 4 );
		whoIndex = target.indexOf( "&" );

		if ( whoIndex != -1 )
			target = target.substring( 0, whoIndex );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( "pvp " + target );
		return true;
	}
}
