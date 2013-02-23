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

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EnthroneCommand
	extends AbstractCommand
{
	private static AdventureResult HATSEAT = ItemPool.get( ItemPool.HATSEAT, 1 );
	
	public EnthroneCommand()
	{
		this.usage = "[?] <species> - place a familiar in the Crown of Thrones.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( parameters.length() == 0 )
		{
			ShowDataCommand.show( "familiars" );
			return;
		}
		else if ( parameters.equalsIgnoreCase( "none" ) || parameters.equalsIgnoreCase( "unequip" ) )
		{
			if ( KoLCharacter.getEnthroned().equals( FamiliarData.NO_FAMILIAR ) )
			{
				return;
			}

			RequestThread.postRequest( FamiliarRequest.enthroneRequest(
				FamiliarData.NO_FAMILIAR ) );
			return;
		}
		else if ( parameters.indexOf( "(no change)" ) != -1 )
		{
			return;
		}

		List familiarList = KoLCharacter.getFamiliarList();

		String[] familiars = new String[ familiarList.size() ];
		for ( int i = 0; i < familiarList.size(); ++i )
		{
			FamiliarData familiar = (FamiliarData) familiarList.get( i );
			familiars[ i ] = StringUtilities.getCanonicalName( familiar.getRace() );
		}

		List matchList = StringUtilities.getMatchingNames( familiars, parameters );

		if ( matchList.size() > 1 )
		{
			RequestLogger.printList( matchList );
			RequestLogger.printLine();

			KoLmafia.updateDisplay( MafiaState.ERROR, "[" + parameters + "] has too many matches." );
		}
		else if ( matchList.size() == 1 )
		{
			String race = (String) matchList.get( 0 );
			FamiliarData change = null;
			for ( int i = 0; i < familiars.length; ++i )
			{
				if ( race.equals( familiars[ i ] ) )
				{
					change = (FamiliarData) familiarList.get( i );
					break;
				}
			}

			if ( change == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't enthrone an unknown familiar!" );
				return;
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( change.toString() );
				return;
			}

			if ( !change.enthroneable() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't enthrone a " + change.getRace() + "!" );
				return;
			}

			if ( KoLCharacter.getFamiliar().equals( change ) )
			{
				RequestThread.postRequest( new FamiliarRequest(
					FamiliarData.NO_FAMILIAR ) );
			}
			RequestThread.postRequest( new EquipmentRequest( HATSEAT,
				EquipmentManager.HAT ) );
			if ( KoLmafia.permitsContinue() &&
				!KoLCharacter.getEnthroned().equals( change ) )
			{
				RequestThread.postRequest(
					FamiliarRequest.enthroneRequest( change ) );
			}
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't have a " + parameters + " for a familiar." );
		}
	}
}
