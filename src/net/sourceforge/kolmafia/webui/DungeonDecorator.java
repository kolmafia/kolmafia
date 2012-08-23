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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class DungeonDecorator
{
	private static int dungeonRoom = 0;
	private static String dungeonEncounter = "";

	private static final Pattern ROOM_PATTERN = Pattern.compile( "<b>Room ([\\d]+): (.*?)</b>" );

	public static final void decorate( final StringBuffer buffer )
	{
		DungeonDecorator.checkDungeon( buffer.toString() );
	}

	public static final void checkDungeon( final String responseText )
	{
		Matcher roomMatcher = DungeonDecorator.ROOM_PATTERN.matcher( responseText );
		if ( roomMatcher.find() )
		{
			DungeonDecorator.dungeonRoom = StringUtilities.parseInt( roomMatcher.group( 1 ) );
			DungeonDecorator.dungeonEncounter = "Encounter: " + roomMatcher.group( 2 );
		}
		else
		{
			DungeonDecorator.dungeonRoom = 0;
			DungeonDecorator.dungeonEncounter = "";
			Preferences.setBoolean( "dailyDungeonDone", true );
		}
	}

	public static final int getDungeonRoom()
	{
		return DungeonDecorator.dungeonRoom;
	}

	public static final String getDungeonRoomString()
	{
		return "Daily Dungeon (Room " + DungeonDecorator.getDungeonRoom() + ")";
	}

	public static final String getDungeonEncounter()
	{
		// We determined the room number earlier when we visited
		// dungeon.php. We only log the encounter and complete the
		// dungeon when we actual spend the turn to visit the room.
		if ( DungeonDecorator.dungeonRoom == 10 )
		{
			Preferences.setBoolean( "dailyDungeonDone", true );
		}
		return DungeonDecorator.dungeonEncounter;
	}
}
