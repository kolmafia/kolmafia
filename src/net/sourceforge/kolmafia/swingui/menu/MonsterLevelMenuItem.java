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

package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.MindControlRequest;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MonsterLevelMenuItem
	extends ThreadedMenuItem
{
	public MonsterLevelMenuItem()
	{
		super( "Monster Level", new MonsterLevelListener() );
	}

	private static class MonsterLevelListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			int maxLevel = 0;

			if ( KoLCharacter.canadiaAvailable() )
			{
				maxLevel = 11;
			}
			else if ( KoLCharacter.knollAvailable() )
			{
				maxLevel = 10;
			}
			else if ( KoLCharacter.gnomadsAvailable() )
			{
				maxLevel = 10;
			}
			else
			{
				return;
			}

			String[] levelArray = new String[ maxLevel + 1 ];

			for ( int i = 0; i <= maxLevel; ++i )
			{
				levelArray[ i ] = "Level " + i;
			}

			int currentLevel = KoLCharacter.getMindControlLevel();

			String selectedLevel =
				(String) InputFieldUtilities.input( "Change monster annoyance from " + currentLevel + "?", levelArray );

			if ( selectedLevel == null )
			{
				return;
			}

			int setting = StringUtilities.parseInt( selectedLevel.split( " " )[ 1 ] );
			RequestThread.postRequest( new MindControlRequest( setting ) );
		}
	}
}
