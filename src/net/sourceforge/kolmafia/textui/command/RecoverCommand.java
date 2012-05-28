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
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.preferences.Preferences;

public class RecoverCommand
	extends AbstractCommand
{
	public RecoverCommand()
	{
		this.usage = " hp | health | mp | mana | both - attempt to regain some HP or MP.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		boolean wasRecoveryActive = RecoveryManager.isRecoveryActive();
		RecoveryManager.setRecoveryActive( true );

		SpecialOutfit.createImplicitCheckpoint();

		int target;

		if ( parameters.equalsIgnoreCase( "hp" ) || parameters.equalsIgnoreCase( "health" ) || parameters.equalsIgnoreCase( "both" ) )
		{
			target = (int) ( Preferences.getFloat( "hpAutoRecoveryTarget" ) * KoLCharacter.getMaximumHP() );
			RecoveryManager.recoverHP( Math.max( target, KoLCharacter.getCurrentHP() + 1 ) );
		}

		if ( parameters.equalsIgnoreCase( "mp" ) || parameters.equalsIgnoreCase( "mana" ) || parameters.equalsIgnoreCase( "both" ) )
		{
			target = (int) ( Preferences.getFloat( "mpAutoRecoveryTarget" ) * KoLCharacter.getMaximumMP() );
			RecoveryManager.recoverMP( Math.max( target, KoLCharacter.getCurrentMP() + 1 ) );
		}

		SpecialOutfit.restoreImplicitCheckpoint();

		RecoveryManager.setRecoveryActive( wasRecoveryActive );
	}
}
