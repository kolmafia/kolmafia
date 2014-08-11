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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.Type69Request;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.AdventureResultArray;

public class StorageCommand
	extends AbstractCommand
{
	public StorageCommand()
	{
		this.usage = " all | outfit <name> | <item> [, <item>]... - pull items from Hagnk's storage.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		if ( KoLCharacter.isHardcore() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot pull things from storage when you are in Hardcore." );
			return;
		}

		if ( parameters.trim().equals( "all" ) )
		{
			if ( !KoLCharacter.canInteract() )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot pull everything while your pulls are limited." );
				return;
			}

			RequestThread.postRequest( new StorageRequest( StorageRequest.EMPTY_STORAGE ) );
			return;
		}

		AdventureResult[] items;

		if ( parameters.startsWith( "outfit " ) )
		{
			String name = parameters.substring( 7 ).trim();
			SpecialOutfit outfit = EquipmentManager.getMatchingOutfit( name );
			if ( outfit == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "No such outfit." );
				return;
			}

			AdventureResultArray have = new AdventureResultArray();
			AdventureResultArray missing = new AdventureResultArray();
			AdventureResultArray needed = new AdventureResultArray();

			AdventureResult[] pieces = outfit.getPieces();
			for ( int i = 0; i < pieces.length; ++i )
			{
				AdventureResult piece = pieces[ i ];

				// Count of item from all "autoSatisfy" source
				int availableCount = InventoryManager.getAccessibleCount( piece );

				// Count of item in storage
				int storageCount = piece.getCount( KoLConstants.storage );

				if ( KoLCharacter.canInteract() && Preferences.getBoolean( "autoSatisfyWithStorage" ) )
				{
					availableCount -= storageCount;
				}

				if ( availableCount > 0 )
				{
					// Don't need to pull; it's in inventory or closet or equipped
					KoLmafia.updateDisplay( piece.getName() + " is available without pulling." );
					have.add( piece );
					continue;
				}

				if ( storageCount == 0 )
				{
					// None available outside of storage - and none in storage
					KoLmafia.updateDisplay( piece.getName() + " is not in storage." );
					missing.add( piece );
					continue;
				}

				needed.add( piece );
			}

			if ( missing.size() > 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You are missing " + missing.size() + " pieces of outfit '" + outfit.getName() + "'; pull aborted." );
				return;
			}

			if ( needed.size() == 0 )
			{
				KoLmafia.updateDisplay( "All pieces of outfit '" + outfit.getName() + "' are available without pulling." );
				return;
			}

			if ( have.size() > 0 )
			{
				KoLmafia.updateDisplay( have.size() + " pieces of outfit '" + outfit.getName() + "' are available without pulling; the remaining " + needed.size() + " will be pulled." );
			}

			items = needed.toArray();
		}
		else
		{
			items = ItemFinder.getMatchingItemList( parameters, KoLConstants.storage );
		}

		if ( items.length == 0 )
		{
			return;
		}

		int meatAttachmentCount = 0;

		for ( int i = 0; i < items.length; ++i )
		{
			AdventureResult item = items[ i ];
			if ( item.getName().equals( AdventureResult.MEAT ) )
			{
				RequestThread.postRequest( new StorageRequest( StorageRequest.PULL_MEAT_FROM_STORAGE, item.getCount() ) );

				items[ i ] = null;
				++meatAttachmentCount;
			}
		}

		if ( meatAttachmentCount == items.length )
		{
			return;
		}

		// Double check to make sure you have all items on hand
		// since a failure to get something from Hagnk's is bad.

		for ( int i = 0; i < items.length; ++i )
		{
			AdventureResult item = items[ i ];
			if ( item == null )
			{
				continue;
			}

			String itemName = item.getName();
			int storageCount = item.getCount( KoLConstants.storage ) + item.getCount( KoLConstants.freepulls );

			if ( storageCount < item.getCount() )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR,
					"You only have " + storageCount + " " + itemName + " in storage (you wanted " + item.getCount() + ")" );
			}

			if ( !Type69Request.isAllowed( "Items", itemName ) )
			{
				KoLmafia.updateDisplay(
					MafiaState.ERROR,
					itemName + " is not allowed right now." );
			}
		}

		RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, items ) );

		if ( !KoLCharacter.canInteract() )
		{
			int pulls = ConcoctionDatabase.getPullsRemaining();
			if ( pulls >= 0 && KoLmafia.permitsContinue() )
			{
				KoLmafia.updateDisplay( pulls + ( pulls == 1 ? " pull" : " pulls" ) + " remaining," + ConcoctionDatabase.getPullsBudgeted() + " budgeted for automatic use." );
			}
		}
	}
}
