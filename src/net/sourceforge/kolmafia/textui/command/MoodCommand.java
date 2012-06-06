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

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MoodCommand
	extends AbstractCommand
{
	{
		this.usage = " list | clear | autofill | execute | repeat [<numTimes>] | <moodName> [<numTimes>] - mood management.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.toLowerCase();

		if ( parameters.length() == 0 || parameters.equals( "list" ) )
		{
			RequestLogger.printList( MoodManager.getTriggers() );
			return;
		}
		else if ( parameters.equals( "clear" ) )
		{
			MoodManager.removeTriggers( MoodManager.getTriggers().toArray() );
			MoodManager.saveSettings();
			RequestLogger.printLine( "Cleared mood." );
		}
		else if ( parameters.equals( "autofill" ) )
		{
			MoodManager.maximalSet();
			MoodManager.saveSettings();
			RequestLogger.printList( MoodManager.getTriggers() );
		}
		else if ( parameters.equals( "execute" ) )
		{
			if ( RecoveryManager.isRecoveryActive() || MoodManager.isExecuting() )
			{
				return;
			}

			SpecialOutfit.createImplicitCheckpoint();
			MoodManager.execute( 0 );
			SpecialOutfit.restoreImplicitCheckpoint();
			RequestLogger.printLine( "Mood swing complete." );
		}
		else if ( parameters.startsWith( "repeat" ) )
		{
			if ( RecoveryManager.isRecoveryActive() || MoodManager.isExecuting() )
			{
				return;
			}

			int multiplicity = 0;
			int spaceIndex = parameters.lastIndexOf( " " );

			if ( spaceIndex != -1 )
			{
				multiplicity = StringUtilities.parseInt( parameters.substring( spaceIndex + 1 ) );
			}

			SpecialOutfit.createImplicitCheckpoint();
			MoodManager.execute( multiplicity );
			SpecialOutfit.restoreImplicitCheckpoint();
			RequestLogger.printLine( "Mood swing complete." );
		}
		else
		{
			int multiplicity = 0;
			int spaceIndex = parameters.lastIndexOf( " " );

			if ( spaceIndex != -1 )
			{
				String possibleMultiplicityString = parameters.substring( spaceIndex + 1 );
				
				if ( StringUtilities.isNumeric( possibleMultiplicityString ) )
				{
					multiplicity = StringUtilities.parseInt( possibleMultiplicityString );
					parameters = parameters.substring( 0, spaceIndex );
				}
			}

			String previousMood = Preferences.getString( "currentMood" );
			MoodManager.setMood( parameters );

			if ( multiplicity > 0 )
			{
				this.CLI.executeCommand( "mood", "repeat " + multiplicity );
				MoodManager.setMood( previousMood );
			}
		}
	}
}
