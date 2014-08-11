/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;

public class HatterCommand
	extends AbstractCommand
{
	public HatterCommand()
	{
		this.usage =
			" [hat] - List effects you can get by wearing available hats at the hatter's tea party, or get a buff with a hat.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.length() < 1 )
		{
			RabbitHoleManager.hatCommand();
			return;
		}

		if ( !RabbitHoleManager.teaPartyAvailable() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You have already attended a Tea Party today." );
			return;
		}

		String hat = parameters;

		try
		{
			int len = Integer.parseInt( parameters );

			RabbitHoleManager.getHatBuff( len );
		}
		catch ( NumberFormatException e )
		{
			List hats = EquipmentManager.getEquipmentLists()[ EquipmentManager.HAT ];
			AdventureResult[] matches = ItemFinder.getMatchingItemList( hat, false, hats );

			// TODO: ItemFinder will just return a 0-length array if too many matches.  It would be nice if the "too many matches" error message worked.
			if ( matches.length > 1 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "[" + hat + "] has too many matches." );
				return;
			}

			if ( matches.length == 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a " + hat + " for a hat." );
				return;
			}

			RabbitHoleManager.getHatBuff( AdventureResult.pseudoItem( matches[ 0 ].toString() ) );
		}
	}
}
