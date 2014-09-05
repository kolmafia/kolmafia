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

package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;

public class LightsOutManager
{
	public static void checkCounter()
	{
		if ( !Preferences.getBoolean( "trackLightsOut" ) )
		{
			return;
		}

		if ( TurnCounter.isCounting( "Spookyraven Lights Out" ) )
		{
			return;
		}

		if ( Preferences.getString( "nextSpookyravenElizabethRoom").equals( "none" ) &&
		     Preferences.getString( "nextSpookyravenStephenRoom" ).equals( "none" ) )
		{
			return;
		}

		int turns = 37 - ( KoLCharacter.getTurnsPlayed() % 37 );
		TurnCounter.startCounting( turns, "Spookyraven Lights Out", "bulb.gif" );
	}

	public static boolean lightsOutNow()
	{
		int totalTurns = KoLCharacter.getTurnsPlayed();
		return totalTurns % 37 == 0 && Preferences.getInteger( "lastLightsOutTurn" ) != totalTurns;
	}

	public static void report()
	{
		String elizabethRoom = Preferences.getString( "nextSpookyravenElizabethRoom" );
		if ( elizabethRoom.equals( "none" ) )
		{
			RequestLogger.printLine( "You have defeated Elizabeth Spookyraven" );
		}
		else
		{
			RequestLogger.printLine( "Elizabeth will next show up in " + elizabethRoom );
		}

		String stephenRoom = Preferences.getString( "nextSpookyravenStephenRoom" );
		if ( stephenRoom.equals( "none" ) )
		{
			RequestLogger.printLine( "You have defeated Stephen Spookyraven" );
		}
		else
		{
			RequestLogger.printLine( "Stephen will next show up in " + stephenRoom );
		}
	}

	public static String message()
	{
		String msg = "";
		String elizabethRoom = Preferences.getString( "nextSpookyravenElizabethRoom" );
		String stephenRoom = Preferences.getString( "nextSpookyravenStephenRoom" );
		if ( !elizabethRoom.equals( "none" ) )
		{
			msg += "Elizabeth can be found in " + elizabethRoom + ".  ";
		}
		if ( !stephenRoom.equals( "none" ) )
		{
			msg += "Stephen can be found in " + stephenRoom + ".  ";
		}

		return msg;
	}

}
