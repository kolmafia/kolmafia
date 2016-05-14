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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.session;

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;

import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.StorageRequest;

import net.sourceforge.kolmafia.utilities.AdventureResultArray;

public abstract class FamiliarManager
{
	public static void equipAllFamiliars()
	{
		KoLmafia.updateDisplay( "Equipping familiars..." );

		FamiliarData current = KoLCharacter.getFamiliar();

		AdventureResultArray closetItems = new AdventureResultArray();
		AdventureResultArray storageItems = new AdventureResultArray();
		ArrayList<GenericRequest> requests = new ArrayList<GenericRequest>();

		for ( FamiliarData familiar : KoLCharacter.getFamiliarList() )
		{
			int itemId = FamiliarDatabase.getFamiliarItemId( familiar.getId() );

			// If this familiar has no specific item of its own, skip it
			if (itemId == -1 )
			{
				continue;
			}

			// If this familiar is already wearing its item, skip it
			AdventureResult currentItem = familiar.getItem();
			if ( currentItem.getItemId() == itemId )
			{
				continue;
			}

			AdventureResult item = ItemPool.get( itemId, 1 );
			if ( item.getCount( KoLConstants.inventory ) > 0 )
			{
				// Use one from inventory
			}
			else if ( item.getCount( KoLConstants.closet ) > 0 )
			{
				// Use one from the closet
				closetItems.add( item );
			}
			else if ( KoLCharacter.canInteract() && item.getCount( KoLConstants.storage ) > 0 )
			{
				// Use one from storage
				storageItems.add( item );
			}
			else
			{
				continue;
			}

			GenericRequest req =
				familiar.equals( current ) ?
				new EquipmentRequest( item ) :
				new FamiliarRequest( familiar, item );

			requests.add( req );
		}

		// If nothing to do, do nothing!

		if ( requests.size() == 0 )
		{
			return;
		}

		// Pull all items that are in storage but not inventory or the closet
		if ( storageItems.size() > 0 )
		{
			RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, storageItems.toArray(), true ) );
		}

		// If you do a "pull all", some items can end up in the
		// closet. Can that happen with a normal "pull"?
		//
		// Just in case, if the item is now in the closet but not
		// inventory, add it to closetItems

		for ( AdventureResult item : storageItems )
		{
			if ( item.getCount( KoLConstants.inventory ) == 0 &&
			     item.getCount( KoLConstants.closet ) > 0 )
			{
				// Use one from the closet
				closetItems.add( item );
			}
		}

		// Move all items that are in the closet into inventory
		if ( closetItems.size() > 0 )
		{
			// *** We'd like to do this transfer without adding the
			// *** items to the session tally
			RequestThread.postRequest( new ClosetRequest( ClosetRequest.CLOSET_TO_INVENTORY, closetItems.toArray() ) );
		}

		// Equip all familiars with equipment from inventory
		for ( GenericRequest request : requests )
		{
			RequestThread.postRequest( request );
		}

		// Leave original familiar as current familiar
		RequestThread.postRequest( new FamiliarRequest( current ) );

		KoLmafia.updateDisplay( "Familiars equipped." );
	}
}
