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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.RabbitHoleManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HatterCommand
	extends AbstractCommand
{
	public HatterCommand()
	{
		this.usage = " [hat] - List effects you can get by wearing available hats at the hatter's tea party, or get a buff with a hat.";
	}

	public void run( final String cmd, final String parameters )
	{
		if ( parameters.length() < 1 )
		{
			RabbitHoleManager.hatCommand();
			return;
		}
		
		if ( !RabbitHoleManager.teaPartyAvailable() )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You have already attended a Tea Party today." );
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
			List matches = new ArrayList();

			for ( int i = 0; i < hats.size(); ++i )
			{
				if ( StringUtilities.fuzzyMatches( hats.get( i ).toString(), hat ) )
				{
					matches.add( hats.get( i ) );
				}
			}

			if ( matches.size() > 1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + hat + "] has too many matches." );
				return;
			}

			if ( matches.size() == 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a " + hat
					+ " for a hat." );
				return;
			}

			RabbitHoleManager.getHatBuff( (AdventureResult) matches.get( 0 ) );
		}
	}
}
