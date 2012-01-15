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

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

public class PastamancerEntityCommand
	extends AbstractCommand
{
	public PastamancerEntityCommand()
	{
		this.usage = " - give details of your current pasta guardians.";
	}

	public void run( final String cmd, final String parameters )
	{
		if ( KoLCharacter.getClassType() != KoLCharacter.PASTAMANCER )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Only Pastamancers can summon a combat entity" );
			return;
		}

		KoLCharacter.ensureUpdatedPastaGuardians();

		int summons = Preferences.getInteger( "pastamancerGhostSummons" );
		RequestLogger.printLine( "You've summoned a pasta guardian " + summons + " time" + ( summons != 1 ? "s" : "" ) + " today." );

		String name = Preferences.getString( "pastamancerGhostName" );
		String type = Preferences.getString( "pastamancerGhostType" );
		int experience = Preferences.getInteger( "pastamancerGhostExperience" );
		if ( name.equals( "" ) )
		{
			RequestLogger.printLine( "You do not currently have an active pasta guardian." );
		}
		else
		{
			RequestLogger.printLine( "Your active pasta guardian is " + name + " the " + type + " (" + experience + " exp). " );
		}

		name = Preferences.getString( "pastamancerOrbedName" );
		if ( name.equals( "" ) )
		{
			return;
		}

		type = Preferences.getString( "pastamancerOrbedType" );
		experience = Preferences.getInteger( "pastamancerOrbedExperience" );
		RequestLogger.printLine( "Your crystal orb contains " + name + " the " + type + " (" + experience + " exp)." );
	}
}
