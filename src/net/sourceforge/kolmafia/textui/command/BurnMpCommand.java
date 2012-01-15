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
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.moods.ManaBurnManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BurnMpCommand
	extends AbstractCommand
{
	public BurnMpCommand()
	{
		this.usage =
			" extra | * | <num> | -num - use excess/all/specified/all but specified MP for buff extension and summons.";
	}

	public void run( final String cmd, String parameters )
	{
		// Remove extra words. For example, "mana"
		int space = parameters.indexOf( " " );
		if ( space != -1)
		{
			parameters = parameters.substring( 0, space );
		}

		if ( parameters.startsWith( "extra" ) )
		{
			SpecialOutfit.createImplicitCheckpoint();
			RecoveryManager.recoverHP();
			ManaBurnManager.burnExtraMana( true );
			SpecialOutfit.restoreImplicitCheckpoint();
			return;
		}

		int amount;
		if ( parameters.startsWith( "*" ) )
		{
			amount = 0;
		}
		else if ( StringUtilities.isNumeric( parameters ) )
		{
			amount = StringUtilities.parseInt( parameters );
			if ( amount > 0 )
			{
				amount -= KoLCharacter.getCurrentMP();
			}
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Specify how much mana you want to burn" );
			return;
		}

		SpecialOutfit.createImplicitCheckpoint();
		RecoveryManager.recoverHP();
		ManaBurnManager.burnMana( -amount );
		SpecialOutfit.restoreImplicitCheckpoint();
	}
}
