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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.request.ClosetRequest;

public class ClosetCommand
	extends AbstractCommand
{
	public ClosetCommand()
	{
		this.usage = " list <filter> | empty | put <item>... | take <item>... - list or manipulate your closet.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( parameters.startsWith( "list" ) )
		{
			ShowDataCommand.show( "closet " + parameters.substring( 4 ).trim() );
			return;
		}
		else if ( parameters.length() == 0 )
		{
			ShowDataCommand.show( "closet" );
			return;
		}

		if ( parameters.length() <= 4 )
		{
			RequestLogger.printList( KoLConstants.closet );
			return;
		}

		if ( parameters.equals( "empty" ) )
		{
			RequestThread.postRequest( new ClosetRequest( ClosetRequest.EMPTY_CLOSET ) );
			return;
		}

		if ( !parameters.startsWith( "take" ) && !parameters.startsWith( "put" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid closet command." );
			return;
		}

		boolean isTake = parameters.startsWith( "take" );
		AdventureResult[] itemList =
			ItemFinder.getMatchingItemList(
				isTake ? KoLConstants.closet : KoLConstants.inventory, parameters.substring( 4 ).trim() );

		if ( itemList.length == 0 )
		{
			return;
		}

		int meatAttachmentCount = 0;

		for ( int i = 0; i < itemList.length; ++i )
		{
			AdventureResult item = itemList[ i ];
			if ( item.getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new ClosetRequest(
					isTake ? ClosetRequest.MEAT_TO_INVENTORY : ClosetRequest.MEAT_TO_CLOSET,
					item.getCount() ) );

				itemList[ i ] = null;
				++meatAttachmentCount;
			}
		}

		if ( meatAttachmentCount == itemList.length )
		{
			return;
		}

		RequestThread.postRequest( new ClosetRequest(
			parameters.startsWith( "take" ) ? ClosetRequest.CLOSET_TO_INVENTORY : ClosetRequest.INVENTORY_TO_CLOSET,
			itemList ) );
		
		for ( int i = 0; i < itemList.length; ++i )
		{
			// update "Hatter" daily deed
			if ( EquipmentDatabase.isHat( itemList[ i ] ) )
			{
				PreferenceListenerRegistry.firePreferenceChanged( "(hats)" );
			}
		}
	}
}
