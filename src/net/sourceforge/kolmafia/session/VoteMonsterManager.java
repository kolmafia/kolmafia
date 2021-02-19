/*
 * Copyright (c) 2005-2021, KoLmafia development team
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
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;

public class VoteMonsterManager
{
	public static void checkCounter()
	{
		if ( !StandardRequest.isAllowed( "Items", "voter registration form" ) )
		{
			return;
		}

		if ( Preferences.getString( "trackVoteMonster" ).equals( "false" ) )
		{
			return;
		}

		if ( Preferences.getString( "trackVoteMonster" ).equals( "free" ) && Preferences.getInteger( "_voteFreeFights" ) >= 3 )
		{
			return;
		}

		if ( TurnCounter.isCounting( "Vote Monster" ) )
		{
			return;
		}

		int turns = 11 - ( ( KoLCharacter.getTurnsPlayed() - 1 ) % 11 );
		TurnCounter.startCounting( turns, "Vote Monster", "absballot.gif" );
	}

	public static boolean voteMonsterNow()
	{
		int totalTurns = KoLCharacter.getTurnsPlayed();
		return totalTurns % 11 == 1 && Preferences.getInteger( "lastVoteMonsterTurn" ) != totalTurns;
	}
}
