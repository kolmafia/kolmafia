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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;


public class DaycareCommand
	extends AbstractCommand
{
	public DaycareCommand()
	{
		this.usage = " [ item | muscle | mysticality | moxie | regen ] - get the item or buff";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( !Preferences.getBoolean( "daycareOpen" ) && !Preferences.getBoolean( "_daycareToday" ) )
		{
			KoLmafia.updateDisplay( "You need a boxing daycare first." );
			return;
		}
		if ( parameters.contains( "item" ) )
		{
			if ( Preferences.getBoolean( "_daycareNap" ) )
			{
				KoLmafia.updateDisplay( "You have already had a Boxing Daydream today" );
				return;
			}				
			RequestThread.postRequest( new GenericRequest( "place.php?whichplace=town_wrong&action=townwrong_boxingdaycare" ) );
			RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1334&option=1" ) );
		}
		else
		{
			int choice = 0;
			if ( parameters.contains ( "mus" ) )
			{
				choice = 1;
			}
			else if ( parameters.contains ( "mox" ) )
			{
				choice = 2;
			}
			else if ( parameters.contains ( "mys" ) )
			{
				choice = 3;
			}
			else if ( parameters.contains ( "regen" ) )
			{
				choice = 4;
			}
			if ( choice == 0 )
			{
				KoLmafia.updateDisplay( "Choice not recognised" );
				return;
			}
			if ( Preferences.getBoolean( "_daycareSpa" ) )
			{
				KoLmafia.updateDisplay( "You have already visited the Boxing Day Spa today" );
				return;
			}				
			RequestThread.postRequest( new GenericRequest( "place.php?whichplace=town_wrong&action=townwrong_boxingdaycare" ) );
			RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1334&option=2" ) );			
			RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1335&option=" + choice ) );			
		}
	}
}
