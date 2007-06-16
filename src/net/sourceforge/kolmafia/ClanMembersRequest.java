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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClanMembersRequest extends KoLRequest
{
	private static final Pattern CLANID_PATTERN = Pattern.compile( "showclan\\.php\\?whichclan=(\\d+)\">(.*?)</a>" );
	private static final Pattern MEMBER_PATTERN = Pattern.compile( "<a class=nounder href=\"showplayer\\.php\\?who=(\\d+)\">([^<]+)</a></b>&nbsp;</td><td class=small>([^<]*?)</td><td class=small>(\\d+).*?</td>" );

	private String clanId;
	private String clanName;
	private boolean isLookup;

	public ClanMembersRequest()
	{
		super( "showclan.php" );
		this.clanId = "";
		this.clanName = "";
		this.isLookup = true;
	}

	public ClanMembersRequest( Object [] titleChange, Object [] newTitles, Object [] boots )
	{
		super( "clan_members.php" );
		this.isLookup = false;

		this.addFormField( "pwd" );
		this.addFormField( "action", "modify" );

		ArrayList fields = new ArrayList();

		String currentId;
		for ( int i = 0; i < titleChange.length; ++i )
		{
			currentId = KoLmafia.getPlayerId( (String) titleChange[i] );
			this.addFormField( "title" + currentId, (String) newTitles[i] );

			if ( !fields.contains( currentId ) )
				fields.add( currentId );
		}

		for ( int i = 0; i < boots.length; ++i )
		{
			currentId = KoLmafia.getPlayerId( (String) boots[i] );
			ClanManager.unregisterMember( currentId );
			this.addFormField( "boot" + currentId, "on" );

			if ( !fields.contains( currentId ) )
				fields.add( currentId );
		}

		String [] changedIds = new String[ fields.size() ];
		fields.toArray( changedIds );

		for ( int i = 0; i < changedIds.length; ++i )
			this.addFormField( "pids[]", changedIds[i], true );
	}

	public void run()
	{
		if ( this.isLookup )
		{
			// First, you need to know which clan you
			// belong to.  This is done by doing a
			// profile lookup on yourself.

			KoLmafia.updateDisplay( "Determining clan Id..." );
			ProfileRequest clanIdLookup = new ProfileRequest( KoLCharacter.getUserName() );
			clanIdLookup.run();

			Matcher clanIdMatcher = CLANID_PATTERN.matcher( clanIdLookup.responseText );
			if ( !clanIdMatcher.find() )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Your character does not belong to a clan." );
				return;
			}

			// Now that you know which clan you belong
			// to, you can do a clan lookup to get a
			// complete list of clan members in one hit

			this.clanId = clanIdMatcher.group(1);
			this.clanName = clanIdMatcher.group(2);

			this.addFormField( "whichclan", this.clanId );
			KoLmafia.updateDisplay( "Retrieving clan member list..." );
		}

		super.run();
	}

	public void processResults()
	{
		if ( this.isLookup )
		{
			int lastMatchIndex = 0;
			Matcher memberMatcher = MEMBER_PATTERN.matcher( this.responseText );

			while ( memberMatcher.find( lastMatchIndex ) )
			{
				lastMatchIndex = memberMatcher.end();

				String playerId = memberMatcher.group(1);
				String playerName = memberMatcher.group(2);
				String playerLevel = memberMatcher.group(4);

				KoLmafia.registerPlayer( playerName, playerId );
				ClanManager.registerMember( playerName, playerLevel );

				String playerTitle = memberMatcher.group(3);
			}
		}
	}

	public String getClanId()
	{	return this.clanId;
	}

	public String getClanName()
	{	return this.clanName;
	}
}
