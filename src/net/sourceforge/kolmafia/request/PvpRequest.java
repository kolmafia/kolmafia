/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.PvpManager;

import net.sourceforge.kolmafia.swingui.ProfileFrame;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PvpRequest
	extends GenericRequest
{
	public static final String[] WIN_MESSAGES =
		new String[] { "50 CHARACTER LIMIT BREAK!", "HERE'S YOUR CHEETO, MOTHER!*$#ER.", "If you want it back, I'll be in my tent.", "PWNED LIKE CRAPSTORM." };

	public static final String[] LOSE_MESSAGES =
		new String[] { "OMG HAX H4X H5X!!", "Please return my pants.", "How do you like my Crotch-To-Your-Foot style?", "PWNED LIKE CRAPSTORM." };

	private static final List searchResults = new ArrayList();

	public static final Pattern VERSUS_PATTERN =
		Pattern.compile( "(.+) initiated a PvP attack against (.+)\\.", Pattern.DOTALL );
	public static final Pattern MINIS_PATTERN =
		Pattern.compile( "\\((\\d+) tattoos, (\\d+) trophies, (\\d+) flowers, (\\d+) white canadians\\)" );

	private static final int RANKVIEW = 0;
	private static final int ATTACK = 1;
	private static final int PLAYER_SEARCH = 2;
	private static final int CLAN_PROFILER = 3;

	private static final Pattern ATTACKS_PATTERN =
		Pattern.compile( "You may participate in (\\d+) more player fights today" );

	private static final Pattern TARGET_PATTERN =
		Pattern.compile( "showplayer\\.php\\?who=(\\d+)\">(.*?)</a></b>  \\(PvP\\)(<br>\\(<a target=mainpane href=\"showclan\\.php\\?whichclan=\\d+\">(.*?)</a>)?.*?<td.*?><td.*?>(\\d+)</td><td.*?>(.*?)</td><td.*?>(\\d+)" );

	private static final Pattern CLAN_PATTERN =
		Pattern.compile( "showplayer\\.php\\?who=(\\d+)\">([^<]*?)</a></b>[^<]*?</td><td class=small>[^<]*?</td><td class=small>\\d+ \\(H\\)" );

	private static final Pattern RANKING_PATTERN = Pattern.compile( "Your current PvP Ranking is (\\d+)" );

	private final int hunterType;

	public PvpRequest()
	{
		super( "pvp.php" );
		this.hunterType = PvpRequest.RANKVIEW;
	}

	public PvpRequest( final String level, final String rank )
	{
		super( "searchplayer.php" );
		this.hunterType = PvpRequest.PLAYER_SEARCH;

		this.addFormField( "searching", "Yep." );
		this.addFormField( "searchstring", "" );
		this.addFormField( "searchlevel", level );
		this.addFormField( "searchranking", rank );

		this.addFormField( "pvponly", "on" );
		this.addFormField( "hardcoreonly", KoLCharacter.isHardcore() ? "1" : "2" );
	}

	public PvpRequest( final String opponent, final int stance, final String mission )
	{
		super( "pvp.php" );
		this.hunterType = PvpRequest.ATTACK;

		this.addFormField( "action", "Yep." );
		this.addFormField( "who", opponent );
		this.addFormField( "stance", String.valueOf( stance ) );
		this.addFormField( "attacktype", mission );

		String win = Preferences.getString( "defaultFlowerWinMessage" );
		String lose = Preferences.getString( "defaultFlowerLossMessage" );

		if ( win.equals( "" ) )
		{
			win =
				PvpRequest.WIN_MESSAGES[ KoLConstants.RNG.nextInt( PvpRequest.WIN_MESSAGES.length ) ];
		}
		if ( lose.equals( "" ) )
		{
			lose =
				PvpRequest.LOSE_MESSAGES[ KoLConstants.RNG.nextInt( PvpRequest.LOSE_MESSAGES.length ) ];
		}

		this.addFormField( "winmessage", win );
		this.addFormField( "losemessage", lose );
	}

	public PvpRequest( final String clanId )
	{
		super( "showclan.php" );
		this.hunterType = PvpRequest.CLAN_PROFILER;

		this.addFormField( "whichclan", clanId );
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void setTarget( final String target )
	{
		this.addFormField( "who", target );
	}

	public static final List getSearchResults()
	{
		return PvpRequest.searchResults;
	}

	public void processResults()
	{
		PvpRequest.searchResults.clear();

		switch ( this.hunterType )
		{
		case RANKVIEW:
			this.parseAttack();
			PvpManager.updateMinis();
			break;

		case ATTACK:
			this.parseAttack();
			break;

		case PLAYER_SEARCH:
			this.parseSearch();
			break;

		case CLAN_PROFILER:
			this.parseClan();
			break;
		}
	}

	private void parseClan()
	{
		Matcher playerMatcher = PvpRequest.CLAN_PATTERN.matcher( this.responseText );

		while ( playerMatcher.find() )
		{
			ContactManager.registerPlayerId( playerMatcher.group( 2 ), playerMatcher.group( 1 ) );
			PvpRequest.searchResults.add( new ProfileRequest( playerMatcher.group( 2 ) ) );
		}
	}

	private void parseSearch()
	{
		if ( this.responseText.indexOf( "<br>No players found.</center>" ) != -1 )
		{
			return;
		}

		ProfileRequest currentPlayer;
		Matcher playerMatcher = PvpRequest.TARGET_PATTERN.matcher( this.responseText );

		while ( playerMatcher.find() )
		{
			ContactManager.registerPlayerId( playerMatcher.group( 2 ), playerMatcher.group( 1 ) );
			currentPlayer =
				ProfileRequest.getInstance(
					playerMatcher.group( 2 ), playerMatcher.group( 1 ), playerMatcher.group( 4 ),
					Integer.valueOf( playerMatcher.group( 5 ) ), playerMatcher.group( 6 ),
					Integer.valueOf( playerMatcher.group( 7 ) ) );

			PvpRequest.searchResults.add( currentPlayer );
		}

		Collections.sort( PvpRequest.searchResults );
	}

	private void parseAttack()
	{
		// Reset the player's current PvP ranking

		Matcher attacksMatcher = PvpRequest.ATTACKS_PATTERN.matcher( this.responseText );
		if ( attacksMatcher.find() )
		{
			KoLCharacter.setAttacksLeft( StringUtilities.parseInt( attacksMatcher.group( 1 ) ) );
		}
		else
		{
			KoLCharacter.setAttacksLeft( 0 );
		}

		Matcher rankMatcher = PvpRequest.RANKING_PATTERN.matcher( this.responseText );
		if ( !rankMatcher.find() )
		{
			if ( !InputFieldUtilities.confirm( "Would you like to break your hippy stone?" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "This feature is not available to hippies." );
				return;
			}

			new GenericRequest( "campground.php?confirm=on&smashstone=Yep." ).run();
			super.run();

			return;
		}

		KoLCharacter.setPvpRank( StringUtilities.parseInt( rankMatcher.group( 1 ) ) );

		// Trim down the response text so it only includes
		// the information related to the fight.

		int index = this.responseText.indexOf( "<p>Player to attack" );
		this.responseText = this.responseText.substring( 0, index == -1 ? this.responseText.length() : index );

		if ( this.hunterType != PvpRequest.RANKVIEW )
		{
			PvpManager.processOffenseContests( this.responseText );
			ProfileFrame.showRequest( this );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "pvp.php" ) )
		{
			return false;
		}

		int whoIndex = urlString.indexOf( "who=" );
		if ( whoIndex == -1 )
		{
			return true;
		}

		String target = urlString.substring( whoIndex + 4 );
		whoIndex = target.indexOf( "&" );

		if ( whoIndex != -1 )
		{
			target = target.substring( 0, whoIndex );
		}

		try
		{
			RequestLogger.updateSessionLog();
			RequestLogger.updateSessionLog( "pvp " + URLDecoder.decode( target, "UTF-8" ) );
		}
		catch ( Exception e )
		{
		}

		return true;
	}
}
