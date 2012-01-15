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

package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GuildRequest;

public class GuildUnlockManager
{
	public static void unlockGuildStore()
	{
		GuildUnlockManager.unlockGuildStore( false );
	}

	public static void unlockGuildStore( final boolean stopAtPaco )
	{
		GenericRequest guildVisit = new GuildRequest();

		// The wiki claims that your prime stats are somehow connected,
		// but the exact procedure is uncertain. Therefore, just allow
		// the person to attempt to unlock their store, regardless of
		// their current stats.

		KoLmafia.updateDisplay( "Entering guild challenge area..." );
		RequestThread.postRequest( guildVisit.constructURLString( "guild.php?place=challenge" ) );

		boolean success =
			stopAtPaco ? guildVisit.responseText.indexOf( "paco" ) != -1 : guildVisit.responseText.indexOf( "\"store.php" ) != -1;

		guildVisit.constructURLString( "guild.php?action=chal" );
		KoLmafia.updateDisplay( "Completing guild tasks..." );

		for ( int i = 0; i < 6 && !success && KoLCharacter.getAdventuresLeft() > 0 && KoLmafia.permitsContinue(); ++i )
		{
			RequestThread.postRequest( guildVisit );

			if ( guildVisit.responseText != null )
			{
				success = stopAtPaco ? guildVisit.responseText.indexOf( "paco" ) != -1 :
					guildVisit.responseText.indexOf( "You've already beaten" ) != -1;
			}
		}

		if ( success )
		{
			RequestThread.postRequest( guildVisit.constructURLString( "guild.php?place=paco" ) );
		}

		if ( success && stopAtPaco )
		{
			KoLmafia.updateDisplay( "You have unlocked the guild meatcar quest." );
		}
		else if ( success )
		{
			KoLmafia.updateDisplay( "Guild store successfully unlocked." );
		}
		else
		{
			KoLmafia.updateDisplay( "Guild store was not unlocked." );
		}
	}

}
