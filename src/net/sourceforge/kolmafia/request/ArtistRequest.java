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

package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.session.ResultProcessor;

public class ArtistRequest
	extends GenericRequest
{
	public static final AdventureResult WHISKER = ItemPool.get( ItemPool.RAT_WHISKER, 1 );

	public ArtistRequest()
	{
		this( false );
	}

	public ArtistRequest( boolean whiskers )
	{
		super( "place.php" );
		this.addFormField( "whichplace", "town_wrong" );
		this.addFormField( "action", "townwrong_artist_quest" );
		if ( whiskers )
		{
			this.addFormField( "subaction", "whisker" );
		}
	}

	@Override
	public void processResults()
	{
		ArtistRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		if ( !location.startsWith( "place.php" ) || !location.contains( "action=townwrong_artist_quest" ) )
		{
			return;
		}

		String message = "You have unlocked a new tattoo.";
		if ( responseText.contains( message ) )
		{
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		// First time accepting:
		// Great. If I'm going to work, I'll need my paintbrush, my palette, and my paint.
		if ( responseText.indexOf( "If I'm going to work, I'll need my paintbrush" ) != -1 )
		{
			QuestDatabase.setQuestProgress( Quest.ARTIST, QuestDatabase.STARTED );
		}

		// Subsequent times:
		// You still need to find my tools! Please hurry!
		else if ( responseText.indexOf( "still need to find my tools" ) != -1 )
		{
			QuestDatabase.setQuestProgress( Quest.ARTIST, QuestDatabase.STARTED );
		}

		// The artist pours the pail of paint into a huge barrel, then
		// says "Oh, hey, umm, do you want this empty pail? I don't
		// really have room for it, so if you want it, you can have it.

		if ( responseText.indexOf( "do you want this empty pail" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.PRETENTIOUS_PALETTE, -1 );
			ResultProcessor.processItem( ItemPool.PRETENTIOUS_PAINTBRUSH, -1 );
			ResultProcessor.processItem( ItemPool.PRETENTIOUS_PAIL, -1 );
			QuestDatabase.setQuestProgress( Quest.ARTIST, QuestDatabase.FINISHED );
			return;
		}

		if ( location.contains( "subaction=whiskers" ) &&
		     responseText.contains( "Thanks, Adventurer." ) )
		{
			int count = ArtistRequest.WHISKER.getCount( KoLConstants.inventory );
			ResultProcessor.processItem( ItemPool.RAT_WHISKER, -count );
			return;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "place.php" ) || !urlString.contains( "action=townwrong_artist_quest" ) )
		{
			return false;
		}

		String message;
		if ( urlString.contains( "subaction=whisker" ) )
		{
			int count = ArtistRequest.WHISKER.getCount( KoLConstants.inventory );
			message = "Selling " + count + " rat whisker" + ( count > 1 ? "s" : "" ) + " to the pretentious artist";
		}
		else
		{
			RequestLogger.printLine( "" );
			RequestLogger.updateSessionLog();
			message = "Visiting the pretentious artist";
		}

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
