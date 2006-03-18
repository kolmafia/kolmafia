/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class FlowerHunterRequest extends KoLRequest
{
	private static final int ATTACK = 1;
	private static final int PLAYER_SEARCH = 2;
	private static final int CLAN_PROFILER = 3;

	private static final Pattern TARGET_MATCH =
		Pattern.compile( "showplayer\\.php\\?who=(\\d+)\">(.*?)</a></b>  \\(PvP\\)(<br>\\(<a target=mainpane href=\"showclan\\.php\\?whichclan=\\d+\">(.*?)</a>)?.*?<td.*?><td.*?>(\\d+)</td><td.*?>(.*?)</td><td.*?>(\\d+)" );

	private static final Pattern CLAN_MATCH =
		Pattern.compile( "showplayer\\.php\\?who=(\\d+)\">([^<]*?)</a></b>[^<]*?</td><td class=small>[^<]*?</td><td class=small>\\d+ \\(H\\)" );

	private int hunterType;
	private List searchResults = new ArrayList();

	public FlowerHunterRequest( KoLmafia client, String level, String rank )
	{
		super( client, "searchplayer.php" );
		this.hunterType = PLAYER_SEARCH;

		addFormField( "searching", "Yep." );
		addFormField( "searchstring", "" );
		addFormField( "searchlevel", level );
		addFormField( "searchranking", rank );

		addFormField( "pvponly", "on" );
		if ( KoLCharacter.isHardcore() )
			addFormField( "hardcoreonly", "on" );
	}

	public FlowerHunterRequest( KoLmafia client, String opponent, int stance, String mission, String message )
	{
		super( client, "pvp.php" );
		this.hunterType = ATTACK;

		addFormField( "action", "Yep." );
		addFormField( "pwd" );
		addFormField( "who", opponent );
		addFormField( "stance", String.valueOf( stance ) );
		addFormField( "attacktype", mission );
		addFormField( "winmessage", message );
		addFormField( "losemessage", message );
	}

	public FlowerHunterRequest( KoLmafia client, String clanID )
	{
		super( client, "showclan.php" );
		this.hunterType = CLAN_PROFILER;

		addFormField( "whichclan", clanID );
	}

	public List getSearchResults()
	{	return searchResults;
	}

	protected void processResults()
	{
		switch ( hunterType )
		{
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
		Matcher playerMatcher = CLAN_MATCH.matcher( responseText );

		while ( playerMatcher.find() )
		{
			KoLmafia.registerPlayer( playerMatcher.group(2), playerMatcher.group(1) );
			searchResults.add( new ProfileRequest( client, playerMatcher.group(2) ) );
		}
	}

	private void parseSearch()
	{
		if ( responseText.indexOf( "<br>No players found.</center>" ) != -1 )
			return;

		ProfileRequest currentPlayer;
		Matcher playerMatcher = TARGET_MATCH.matcher( responseText );

		while ( playerMatcher.find() )
		{
			KoLmafia.registerPlayer( playerMatcher.group(2), playerMatcher.group(1) );
			currentPlayer = ProfileRequest.getInstance( playerMatcher.group(2), playerMatcher.group(1),
				playerMatcher.group(4), Integer.valueOf( playerMatcher.group(5) ), playerMatcher.group(6),
				Integer.valueOf( playerMatcher.group(7) ) );

			searchResults.add( currentPlayer );
		}
	}

	private void parseAttack()
	{
		// Reset the player's current PvP ranking

		Matcher rankMatcher = Pattern.compile( "Your current PvP Ranking is (\\d+)" ).matcher( responseText );
		if ( rankMatcher.find() )
			KoLCharacter.setPvpRank( Integer.parseInt( rankMatcher.group(1) ) );

		// Trim down the response text so it only includes
		// the information related to the fight.

		int index = responseText.indexOf( "<p>Player to attack" );
		responseText = responseText.substring( 0, index == -1 ? responseText.length() : index );
	}
}
