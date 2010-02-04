/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.util.Collections;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.RecoveryManager;

public class FoldItemCommand
	extends AbstractCommand
{
	public FoldItemCommand()
	{
		this.usage = "[?] <item> - produce item by using another form, repeated as needed.";
	}

	public void run( final String cmd, final String parameters )
	{
		// Determine which item to create
		AdventureResult target = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
		if ( target == null )
		{
			return;
		}

		// If we already have the item in inventory, we're done
		if ( target.getCount( KoLConstants.inventory ) > 0 )
		{
			return;
		}

		// Find the fold group containing this item
		String targetName = target.getName();
		ArrayList group = ItemDatabase.getFoldGroup( targetName );
		if ( group == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "That's not a transformable item!" );
			return;
		}

		// Locate the item in the fold group
		int count = group.size();
		int targetIndex = 0;
		for ( int i = 1; i < count; ++i )
		{
			String form = (String) group.get( i );
			if ( form.equals( targetName ) )
			{
				targetIndex = i;
				break;
			}
		}

		// Iterate backwards to find closest item to transform. Skip index 0.
		int sourceIndex = targetIndex > 1 ? targetIndex - 1 : count - 1;

		while ( sourceIndex != targetIndex )
		{
			String form = (String) group.get( sourceIndex );
			AdventureResult item = new AdventureResult( form, 1, false );

			// If we have this item in inventory, use it to start folding
			if ( item.getCount( KoLConstants.inventory ) > 0 )
			{
				break;
			}

			// Consider the next item. Skip index 0.
			sourceIndex = sourceIndex > 1 ? sourceIndex - 1 : count - 1;
		}

		// Punt now if have nothing foldable
		if ( sourceIndex == targetIndex )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have anything transformable into that item!" );
			return;
		}

		int damage = ( (Integer) group.get( 0 ) ).intValue();
		damage = damage == 0 ? 0 : KoLCharacter.getMaximumHP() * damage / 100 + 2;

		// Fold repeatedly until target is obtained
		while ( sourceIndex != targetIndex )
		{
			String form = (String) group.get( sourceIndex );
			AdventureResult item = new AdventureResult( form, 1, false );

			// Consider the next item. Skip index 0.
			sourceIndex = sourceIndex < count - 1 ? sourceIndex + 1 : 1;

			// If we don't have this item in inventory,  skip
			if ( item.getCount( KoLConstants.inventory ) == 0 )
			{
				continue;
			}

			if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
			{
				RequestLogger.printLine( form + " => " + target );
				break;
			}

			int hp = KoLCharacter.getCurrentHP();
			if ( hp > 0 && hp < damage )
			{
				RecoveryManager.recoverHP( damage );
			}

			RequestThread.postRequest( new UseItemRequest( item ) );
		}
	}
}
