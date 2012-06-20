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

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class AdventureCommand
	extends AbstractCommand
{
	public AdventureCommand()
	{
		this.usage = "[?] last | [<count>] <location> - spend your turns.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		int adventureCount;
		KoLAdventure adventure =
			AdventureDatabase.getAdventure( parameters.equalsIgnoreCase( "last" ) ? Preferences.getString( "lastAdventure" ) : parameters );

		if ( adventure != null )
		{
			adventureCount = 1;
		}
		else
		{
			String adventureCountString = parameters.split( " " )[ 0 ];
			adventureCount = adventureCountString.equals( "*" ) ? 0 : StringUtilities.parseInt( adventureCountString );

			if ( adventureCount == 0 && !adventureCountString.equals( "0" ) && !adventureCountString.equals( "*" ) )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR, parameters + " does not exist in the adventure database." );
				return;
			}

			String adventureName = parameters.substring( adventureCountString.length() ).trim();
			adventure = AdventureDatabase.getAdventure( adventureName );

			if ( adventure == null )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR, parameters + " does not exist in the adventure database." );
				return;
			}

			if ( adventureCount <= 0 && adventure.getFormSource().equals( "shore.php" ) )
			{
				adventureCount += (int) Math.floor( KoLCharacter.getAdventuresLeft() / 3 );
			}
			else if ( adventureCount <= 0 )
			{
				adventureCount += KoLCharacter.getAdventuresLeft();
			}
		}

		if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
		{
			RequestLogger.printLine( adventure.toString() );
			return;
		}

		StaticEntity.getClient().makeRequest( adventure, adventureCount );
	}
}
