/**
 * Copyright (c) 2005-2015, KoLmafia development team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;

import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CrossStreamsCommand
	extends AbstractCommand
{
	private static final AdventureResult PROTON_ACCELERATOR = ItemPool.get( ItemPool.PROTON_ACCELERATOR, 1 );

	public CrossStreamsCommand()
	{
		this.usage = " [ <target> ] - Cross streams with the target, default target in preference \"streamCrossDefaultTarget\"";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		Boolean equipped = KoLCharacter.hasEquipped( CrossStreamsCommand.PROTON_ACCELERATOR, EquipmentManager.CONTAINER );
		// Check if Protonic Accelerator Pack equipped or owned
		if ( !InventoryManager.hasItem( CrossStreamsCommand.PROTON_ACCELERATOR ) && !equipped )
		{
			KoLmafia.updateDisplay( "Do not have a Proton Accelerator Pack" );
			return;
		}

		// Check if previously used
		if ( Preferences.getBoolean( "_streamsCrossed" ) )
		{
			KoLmafia.updateDisplay( "Have already crossed streams today" );
			return;
		}

		// Validate target
		String targetName = null;
		String targetId = null;

		parameters = parameters.trim();

		// If no target given, use default
		if ( parameters.equals( "" ) )
		{
			parameters = Preferences.getString( "streamCrossDefaultTarget" );
		}

		if ( StringUtilities.isNumeric( parameters ) )
		{
			// Target ID given, so get Target Name
			targetId = parameters;
			targetName = ContactManager.getPlayerName( targetId, true );
		}
		else
		{
			// Target Name given, so get Target Id
			targetName = parameters;
			targetId = ContactManager.getPlayerId( targetName, true );
		}

		// If names weren't found, they don't exist
		// Contact manager returns Id if looking up Name, and visa versa, so they match
		if ( targetId == targetName )
		{
			KoLmafia.updateDisplay( "Cannot find target " + parameters );
			return;
		}

		// Equip if not equipped
		try
		{
			if ( !equipped )
			{
				SpecialOutfit.createImplicitCheckpoint();
				RequestThread.postRequest( new EquipmentRequest( CrossStreamsCommand.PROTON_ACCELERATOR, EquipmentManager.CONTAINER ) );
			}

			// Cross Streams
			KoLmafia.updateDisplay( "Crossing Streams with " + targetName );
			if ( KoLmafia.permitsContinue() )
			{
				RequestThread.postRequest( new GenericRequest( "showplayer.php?action=crossthestreams&who=" + targetId ) );
			}
		}
		finally
		{
			if ( !equipped )
			{
				SpecialOutfit.restoreImplicitCheckpoint();
			}
		}
	}
}
