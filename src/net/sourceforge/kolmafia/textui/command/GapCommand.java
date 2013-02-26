/**
 * Copyright (c) 2005-2013, KoLmafia development team
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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

public class GapCommand
	extends AbstractCommand
{
	public GapCommand()
	{
		this.usage = " [skill|structure|vision|speed|accuracy] - get a Greatest American Pants buff.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		GapCommand.visit( parameters );
	}

	public static void visit( final String parameters )
	{
		if ( Preferences.getInteger( "_gapBuffs" ) >= 5 )
		{
			KoLmafia.updateDisplay( "You're out of superpowers." );
			return;
		}

		if ( !KoLCharacter.hasEquipped( ItemPool.get( ItemPool.GREAT_PANTS, 1 ) ) )
		{
			KoLmafia.updateDisplay( "You need to equip your superpants first." );
			return;
		}

		int choicenumber = 0;
		String buffname = "";
		if ( parameters.toLowerCase().indexOf( "skill" ) != -1 || parameters.equals( "1" ) ) {
			choicenumber = 1;
			buffname = "Super Skill";
		}

		else if ( parameters.toLowerCase().indexOf( "structure" ) != -1  || parameters.equals( "2" ) ) {
			choicenumber = 2;
			buffname = "Super Structure";
		}

		else if ( parameters.toLowerCase().indexOf( "vision" ) != -1  || parameters.equals( "3" ) ) {
			choicenumber = 3;
			buffname = "Super Vision";
		}

		else if ( parameters.toLowerCase().indexOf( "speed" ) != -1  || parameters.equals( "4" ) ) {
			choicenumber = 4;
			buffname = "Super Speed";
		}

		else if ( parameters.toLowerCase().indexOf( "accuracy" ) != -1  || parameters.equals( "5" ) ) {
			choicenumber = 5;
			buffname = "Super Accuracy";
		}

		if ( choicenumber == 0 )
		{
			KoLmafia.updateDisplay( (5 - Preferences.getInteger( "_gapBuffs" ) ) + " superbuffs remaining." );
			return;
		}

		KoLmafia.updateDisplay( "Superpower time!" );
		RequestThread.postRequest( new GenericRequest( "inventory.php?action=activatesuperpants" ));
		RequestThread.postRequest( new GenericRequest( "choice.php?pwd&whichchoice=508&option=" + choicenumber
			+ "&choiceform" + choicenumber + "=" + buffname ) );

		Preferences.increment( "_gapBuffs", 1 );
	}
}
