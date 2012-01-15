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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.moods.MoodManager;

public class EditMoodCommand
	extends AbstractCommand
{
	public EditMoodCommand()
	{
		this.usage = " clear | autofill | [<type>,] <effect> [, <action>] - edit current mood";
		this.flags = KoLmafiaCLI.FULL_LINE_CMD;
	}

	public void run( final String cmd, final String parameters )
	{
		if ( parameters.equals( "clear" ) )
		{
			MoodManager.removeTriggers( MoodManager.getTriggers().toArray() );
			MoodManager.saveSettings();
		}
		else if ( parameters.equals( "autofill" ) )
		{
			MoodManager.maximalSet();
			MoodManager.saveSettings();
		}

		String[] split = parameters.split( "\\s*,\\s*" );
		if ( split.length == 3 )
		{
			MoodManager.addTrigger( split[ 0 ], split[ 1 ], split[ 2 ] );
			MoodManager.saveSettings();
		}
		else if ( split.length == 2 )
		{
			MoodManager.addTrigger( split[ 0 ], split[ 1 ], MoodManager.getDefaultAction( split[ 0 ], split[ 1 ] ) );
			MoodManager.saveSettings();
		}
		else if ( split.length == 1 )
		{
			MoodManager.addTrigger( "lose_effect", split[ 0 ], MoodManager.getDefaultAction( "lose_effect", split[ 0 ] ) );
			MoodManager.saveSettings();
		}

		RequestLogger.printList( MoodManager.getTriggers() );
	}
}
