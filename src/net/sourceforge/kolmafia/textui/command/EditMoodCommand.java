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
import net.sourceforge.kolmafia.moods.MoodTrigger;

public class EditMoodCommand
	extends AbstractCommand
{
	public EditMoodCommand()
	{
		this.usage = " list | clear | autofill | [<type>,] <effect> [, <action>] - edit current mood";
		this.flags = KoLmafiaCLI.FULL_LINE_CMD;
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.length() == 0 || parameters.equals( "list" ) )
		{
			RequestLogger.printList( MoodManager.getTriggers() );
			return;
		}

		if ( parameters.equals( "clear" ) )
		{
			MoodManager.removeTriggers( MoodManager.getTriggers().toArray() );
			MoodManager.saveSettings();
			RequestLogger.printLine( "Cleared mood." );
			return;
		}

		if ( parameters.equals( "autofill" ) )
		{
			MoodManager.maximalSet();
			MoodManager.saveSettings();
			RequestLogger.printList( MoodManager.getTriggers() );
			return;
		}

		int start = 0;
		int end = parameters.indexOf( ',' );

		if ( end == -1 )
		{
			RequestLogger.printLine( "Invalid command: " + cmd + " " + parameters );
			return;
		}

		String type = parameters.substring( start, end );
		type = type.trim();

		if ( !type.equals( "lose_effect" ) && !type.equals( "gain_effect" ) && !type.equals( "unconditional" ) )
		{
			type = "lose_effect";
			end = -1;
		}

		start = end + 1;
		end = parameters.indexOf( ',', start );

		String name = ( end != -1 ) ? parameters.substring( start, end ) : parameters.substring( start );
		name = name.trim();

		String action = ( end != -1 ) ? parameters.substring( end + 1 ) : MoodManager.getDefaultAction( type, name );
		action = action.trim();

		MoodTrigger trigger = MoodManager.addTrigger( type, name, action );

		if ( trigger == null )
		{
			RequestLogger.printLine( "Invalid command: " + cmd + " " + parameters );
			return;
		}

		MoodManager.saveSettings();

		RequestLogger.printLine( "Set mood trigger: " + trigger.toString() );
	}
}
