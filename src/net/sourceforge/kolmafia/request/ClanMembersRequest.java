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

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ProfileSnapshot;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.ContactManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ClanMembersRequest
	extends GenericRequest
{
	private static final Pattern CLANID_PATTERN = Pattern.compile( "Clan: <b><a class=nounder href=\"showclan\\.php\\?whichclan=(\\d+)\">(.*?)</a>" );
	private static final Pattern MEMBER_PATTERN =
		Pattern.compile( "<a class=nounder href=\"showplayer\\.php\\?who=(\\d+)\">([^<]+)</a></b>&nbsp;</td><td class=small>([^<]*?)&nbsp;</td><td class=small>(\\d+).*?</td>" );

	private static final Pattern RANK_PATTERN = Pattern.compile( "<select name=level.*?</select>" );
	private static final Pattern OPTION_PATTERN = Pattern.compile( "<option.*?>(.*?)</option>" );

	private static final Pattern ROW_PATTERN = Pattern.compile( "<tr>(.*?)</tr>", Pattern.DOTALL );
	private static final Pattern CELL_PATTERN = Pattern.compile( "<td.*?>(.*?)</td>", Pattern.DOTALL );

	private final boolean isLookup;
	private final boolean isDetailLookup;
	private final LockableListModel rankList;

	private String clanId;

	public ClanMembersRequest( final boolean isDetailLookup )
	{
		super( isDetailLookup ? "clan_detailedroster.php" : "showclan.php" );

		this.isLookup = true;
		this.isDetailLookup = isDetailLookup;
		this.rankList = null;

		this.clanId = "";
	}

	public ClanMembersRequest( final LockableListModel rankList )
	{
		super( "clan_members.php" );

		this.isLookup = false;
		this.isDetailLookup = false;
		this.rankList = rankList;
	}

	public ClanMembersRequest( final Object[] titleChange, final Object[] newTitles, final Object[] boots )
	{
		super( "clan_members.php" );

		this.isLookup = false;
		this.isDetailLookup = false;
		this.rankList = new LockableListModel();

		this.addFormField( "action", "modify" );

		ArrayList<String> fields = new ArrayList<String>();

		String currentId;
		for ( int i = 0; i < titleChange.length; ++i )
		{
			currentId = ContactManager.getPlayerId( (String) titleChange[ i ] );
			this.addFormField( "title" + currentId, (String) newTitles[ i ] );

			if ( !fields.contains( currentId ) )
			{
				fields.add( currentId );
			}
		}

		for ( int i = 0; i < boots.length; ++i )
		{
			currentId = ContactManager.getPlayerId( (String) boots[ i ] );
			ClanManager.unregisterMember( currentId );
			this.addFormField( "boot" + currentId, "on" );

			if ( !fields.contains( currentId ) )
			{
				fields.add( currentId );
			}
		}

		String[] changedIds = new String[ fields.size() ];
		fields.toArray( changedIds );

		for ( int i = 0; i < changedIds.length; ++i )
		{
			this.addFormField( "pids[]", changedIds[ i ], true );
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		if ( !this.isLookup || this.isDetailLookup )
		{
			KoLmafia.updateDisplay( "Retrieving clan member list..." );
			super.run();
			return;
		}

		retrieveClanId();

		KoLmafia.updateDisplay( "Retrieving clan member list..." );

		int page = 0;

		do
		{
			this.responseText = null;

			this.constructURLString( "showclan.php?whichclan=" + this.clanId + "&page=" + (page++), false );

			super.run();
		}
		while ( this.responseText != null && this.responseText.indexOf( "next page &gt;&gt;" ) != -1 );
	}

	private void retrieveClanId()
	{
		// First, you need to know which clan you
		// belong to.  This is done by doing a
		// profile lookup on yourself.

		KoLmafia.updateDisplay( "Determining clan id..." );
		ProfileRequest clanIdLookup = new ProfileRequest( KoLCharacter.getUserName() );
		clanIdLookup.run();

		Matcher clanIdMatcher = ClanMembersRequest.CLANID_PATTERN.matcher( clanIdLookup.responseText );
		if ( !clanIdMatcher.find() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Your character does not belong to a clan." );
			return;
		}

		// Now that you know which clan you belong
		// to, you can do a clan lookup to get a
		// complete list of clan members in one hit

		this.clanId = clanIdMatcher.group( 1 );
	}

	@Override
	public void processResults()
	{
		if ( !this.isLookup )
		{
			this.parseRanks();
		}
		else if ( this.isDetailLookup )
		{
			this.parseDetail();
		}
		else
		{
			this.parseSparse();
		}
	}

	private void parseRanks()
	{
        this.rankList.clear();
		Matcher ranklistMatcher = ClanMembersRequest.RANK_PATTERN.matcher( this.responseText );

		if ( ranklistMatcher.find() )
		{
			Matcher rankMatcher = ClanMembersRequest.OPTION_PATTERN.matcher( ranklistMatcher.group() );

			while ( rankMatcher.find() )
			{
				this.rankList.add( rankMatcher.group( 1 ).toLowerCase() );
			}
		}
	}

	private void parseDetail()
	{
		Matcher rowMatcher =
			ClanMembersRequest.ROW_PATTERN.matcher( this.responseText.substring( this.responseText.lastIndexOf( "clan_detailedroster.php" ) ) );

		String currentRow;
		String currentName;
		Matcher dataMatcher;

		while ( rowMatcher.find() )
		{
			currentRow = rowMatcher.group( 1 );

			if ( currentRow.equals( "<td height=4></td>" ) )
			{
				continue;
			}

			dataMatcher = ClanMembersRequest.CELL_PATTERN.matcher( currentRow );

			// The name of the player occurs in the first
			// field of the table.  Use this to index the
			// roster map.

			dataMatcher.find();
			currentName = dataMatcher.group( 1 );
			currentName = KoLConstants.ANYTAG_PATTERN.matcher( currentName ).replaceAll( "" );
			currentName = StringUtilities.globalStringDelete( currentName, "&nbsp;" ).trim();

			ProfileSnapshot.addToRoster( currentName, currentRow );
		}
	}

	private void parseSparse()
	{
		int lastMatchIndex = 0;
		Matcher memberMatcher = ClanMembersRequest.MEMBER_PATTERN.matcher( this.responseText );

		while ( memberMatcher.find( lastMatchIndex ) )
		{
			lastMatchIndex = memberMatcher.end();

			String id = memberMatcher.group( 1 );
			String name = memberMatcher.group( 2 );
			String level = memberMatcher.group( 4 );
			String title = memberMatcher.group( 3 );

			ContactManager.registerPlayerId( name, id );
			ClanManager.registerMember( name, level, title );
		}
	}
}
