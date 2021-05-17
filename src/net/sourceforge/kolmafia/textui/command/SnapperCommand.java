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
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Phylum;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;

public class SnapperCommand
	extends AbstractCommand
{
	public SnapperCommand()
	{
		this.usage = " [PHYLUM] - guide your Red Snapper to a certain phylum";
	}

	@Override
	public void run( final String cmd, String parameter )
	{
		FamiliarData current = KoLCharacter.getFamiliar();
		if ( current == null || current.getId() != FamiliarPool.RED_SNAPPER )
		{
			KoLmafia.updateDisplay( "You need to take your Red-Nosed Snapper with you" );
			return;
		}

		parameter = parameter.trim();

		if ( parameter.equals( "" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Which monster phylum do you want?" );
			return;
		}

		Phylum phylum = MonsterDatabase.phylumNumber( parameter );
		if ( phylum == Phylum.NONE )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "What kind of random monster is a " + parameter + "?" );
			return;
		}

		if ( phylum == MonsterDatabase.phylumNumber( Preferences.getString( "redSnapperPhylum" ) ) )
		{
			KoLmafia.updateDisplay( "Your Red-Nosed Snapper is already hot on the tail of any " + phylum.toString() + " it can see" );
			return;
		}

		RequestThread.postRequest( new GenericRequest( "familiar.php?action=guideme" ) );
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1396&option=1&cat=" + phylum.toToken() ) );
		KoLmafia.updateDisplay( "Your Red-Nosed Snapper is now guiding you towards " + phylum.getPlural() );
		KoLCharacter.updateStatus();
	}
}
