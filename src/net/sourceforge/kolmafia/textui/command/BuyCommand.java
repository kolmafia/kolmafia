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

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;

import net.sourceforge.kolmafia.request.PurchaseRequest;

import net.sourceforge.kolmafia.session.StoreManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BuyCommand
	extends AbstractCommand
{
	public BuyCommand()
	{
		this.usage = " [using storage] <item> [@ <limit>] [, <another>]... - buy from NPC store or the Mall.";
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		SpecialOutfit.createImplicitCheckpoint();
		BuyCommand.buy( parameters );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	public static void buy( String parameters )
	{
		boolean interact = KoLCharacter.canInteract();
		boolean storage = false;

		String TEST = "using storage ";
		if ( parameters.startsWith( TEST ) )
		{
			storage = true;
			parameters = parameters.substring( TEST.length() ).trim();
		}

		if ( interact && storage )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You cannot purchase using storage unless you are in Hardcore or Ronin" );
			return;
		}

		String[] itemNames = parameters.split( "\\s*,\\s*" );

		for ( String itemName : itemNames )
		{
			String[] pieces = itemName.split( "@" );
			AdventureResult match = ItemFinder.getFirstMatchingItem( pieces[ 0 ] );
			if ( match == null )
			{
				return;
			}

			int priceLimit = pieces.length < 2 ? 0 : StringUtilities.parseInt( pieces[ 1 ] );

			ArrayList<PurchaseRequest> results = 
				// Cheapest from Mall or NPC stores
				interact ? StoreManager.searchMall( match ) :
				// Mall stores only
				storage ? StoreManager.searchOnlyMall( match ) :
				// NPC stores only
				StoreManager.searchNPCs( match );

			KoLmafia.makePurchases( results, results.toArray( new PurchaseRequest[0] ), match.getCount(), false, priceLimit );

			if ( interact && !storage )
			{
				StoreManager.updateMallPrice( match, results );
			}
		}
	}
}
