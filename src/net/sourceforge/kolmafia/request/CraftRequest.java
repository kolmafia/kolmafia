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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.request;

import java.util.EnumSet;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.session.InventoryManager;

public class CraftRequest
	extends GenericRequest
{
	private CraftingType mixingMethod;
	private int quantity;
	private AdventureResult item1;
	private AdventureResult item2;
	private int remaining;
	private int created;

	public CraftRequest( final String mode, final int quantity, final int itemId1, final int itemId2 )
	{
		super( "craft.php" );

		this.addFormField( "action", "craft" );
		this.setMixingMethod( mode );
		this.addFormField( "a", String.valueOf( itemId1 ) );
		this.addFormField( "b", String.valueOf( itemId2 ) );

		this.quantity = quantity;
		this.remaining = quantity;
		this.item1 = ItemPool.get( itemId1, quantity );
		this.item2 = ItemPool.get( itemId2, quantity );
	}

	private void setMixingMethod( final String mode )
	{
		if ( mode.equals( "combine" ) )
		{
			this.mixingMethod = CraftingType.COMBINE;
		}
		else if ( mode.equals( "cocktail" ) )
		{
			this.mixingMethod = CraftingType.MIX;
		}
		else if ( mode.equals( "cook" ) )
		{
			this.mixingMethod = CraftingType.COOK;
		}
		else if ( mode.equals( "smith" ) )
		{
			this.mixingMethod = CraftingType.SMITH;
		}
		else if ( mode.equals( "jewelry" ) )
		{
			this.mixingMethod = CraftingType.JEWELRY;
		}
		else
		{
			this.mixingMethod = CraftingType.NOCREATE;
			return;
		}

		this.addFormField( "mode", mode );
	}

	public int created()
	{
		return this.quantity - this.remaining;
	}

	@Override
	public void run()
	{
		if ( this.mixingMethod == CraftingType.NOCREATE ||
		     this.quantity <= 0 ||
		     !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Get all the ingredients up front

		if ( !InventoryManager.retrieveItem( this.item1 ) ||
		     !InventoryManager.retrieveItem( this.item2 ) )
		{
			return;
		}

		this.remaining = this.quantity;

		while ( this.remaining > 0 && KoLmafia.permitsContinue() )
		{
			if ( !CreateItemRequest.autoRepairBoxServant( this.mixingMethod, EnumSet.noneOf(CraftingRequirements.class) ) )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Auto-repair was unsuccessful." );
				return;
			}

			this.addFormField( "qty", String.valueOf( this.remaining ) );
			this.created = 0;

			super.run();

			if ( this.responseCode == 302 && this.redirectLocation.startsWith( "inventory" ) )
			{
				CreateItemRequest.REDIRECT_REQUEST.constructURLString( this.redirectLocation ).run();
			}

			if ( this.created == 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Creation failed, no results detected." );
				return;
			}

			this.remaining -= this.created;
		}
	}

	@Override
	public void processResults()
	{
		this.created = CreateItemRequest.parseCrafting( this.getURLString(), this.responseText );

		if ( this.responseText.indexOf( "Smoke" ) != -1 )
		{
			KoLmafia.updateDisplay( "Your box servant has escaped!" );
		}
	}
}
