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

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.moods.ManaBurnManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;

public class NunneryCommand
	extends AbstractCommand
{
	public NunneryCommand()
	{
		this.usage = " [mp] - visit the Nunnery for restoration [but only if MP is restored].";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		NunneryCommand.visit( parameters );
	}

	/**
	 * Attempts to get HP or HP/MP restoration from the Nuns at Our Lady of Perpetual Indecision
	 */

	public static void visit( final String parameters )
	{
		if ( Preferences.getInteger( "nunsVisits" ) >= 3 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Nun of the nuns are available right now." );
			return;
		}
		String side = Preferences.getString( "sidequestNunsCompleted" );
		if ( !side.equals( "fratboy" ) && !side.equals( "hippy" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You have not opened the Nunnery yet." );
			return;
		}
		if ( side.equals( "hippy" ) && parameters.equalsIgnoreCase( "mp" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Only HP restoration is available from the nuns." );
			return;
		}
		else if ( side.equals( "fratboy" ) )
		{
			ManaBurnManager.burnMana( KoLCharacter.getMaximumMP() - 1000 );
		}
		String url =
			Preferences.getString( "warProgress" ).equals( "finished" ) ? "postwarisland.php" : "bigisland.php";

		KoLmafia.updateDisplay( "Get thee to a nunnery!" );
		RequestThread.postRequest( new GenericRequest( url + "?place=nunnery&pwd&action=nuns" ) );
	}
}
