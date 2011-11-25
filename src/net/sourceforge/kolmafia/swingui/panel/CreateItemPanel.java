/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.swingui.widget.CreationSettingCheckBox;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

/**
 * Internal class used to handle everything related to creating items; this allows creating of items, which usually
 * get resold in malls.
 */

public class CreateItemPanel
	extends InventoryPanel
{
	public CreateItemPanel( final boolean food, final boolean booze, final boolean equip, boolean other )
	{
		super( "create item", "create & use", ConcoctionDatabase.getCreatables(), equip && !other );

		if ( this.isEquipmentOnly )
		{
			super.addFilters();
		}
		else
		{
			JPanel filterPanel = new JPanel();

			if ( food )
			{
				filterPanel.add( new CreationSettingCheckBox(
					"require in-a-boxes", "requireBoxServants", "Do not cook without chef" ) );
				filterPanel.add( new CreationSettingCheckBox(
					"repair on explosion", "autoRepairBoxServants",
					"Automatically repair chefs on explosion" ) );
			}
			else if ( booze )
			{
				filterPanel.add( new CreationSettingCheckBox(
					"require in-a-boxes", "requireBoxServants", "Do not mix without bartender" ) );
				filterPanel.add( new CreationSettingCheckBox(
					"repair on explosion", "autoRepairBoxServants",
					"Automatically repair bartenders on explosion" ) );
			}

			filterPanel.add( new CreationSettingCheckBox(
						 "use closet", "autoSatisfyWithCloset", "Look in closet for ingredients" ) );

			this.northPanel.add( filterPanel, BorderLayout.NORTH );

			this.setFixedFilter( food, booze, equip, other, true );
		}

		ConcoctionDatabase.getCreatables().updateFilter( false );
	}

	public void addFilters()
	{
	}

	public void actionConfirmed()
	{
		Object[] items = this.elementList.getSelectedValues();
		for ( int i = 0; i < items.length; ++i )
		{
			CreateItemRequest selection = (CreateItemRequest) items[ i ];
			int quantityDesired =
				InputFieldUtilities.getQuantity(
					"Creating multiple " + selection.getName() + ", " + (selection.getQuantityPossible() + selection.getQuantityPullable())
					+ " possible", selection.getQuantityPossible() + selection.getQuantityPullable(), 1 );
			if ( quantityDesired < 1 )
			{
				continue;
			}

			KoLmafia.updateDisplay( "Verifying ingredients..." );
			int pulled = Math.max( 0, quantityDesired - selection.getQuantityPossible() );
			selection.setQuantityNeeded( quantityDesired - pulled );

			SpecialOutfit.createImplicitCheckpoint();
			RequestThread.postRequest( selection );
			SpecialOutfit.restoreImplicitCheckpoint();
			if ( pulled > 0 && KoLmafia.permitsContinue() )
			{
				int newbudget = ConcoctionDatabase.getPullsBudgeted() - pulled;
				RequestThread.postRequest( new StorageRequest(
					StorageRequest.STORAGE_TO_INVENTORY,
					new AdventureResult[] { ItemPool.get( selection.getItemId(), pulled ) } ) );
				ConcoctionDatabase.setPullsBudgeted( newbudget );
			}
		}
	}

	public void actionCancelled()
	{
		Object[] items = this.elementList.getSelectedValues();
		for ( int i = 0; i < items.length; ++i )
		{
			CreateItemRequest selection = (CreateItemRequest) items[ i ];

			int itemId = selection.getItemId();
			int maximum = UseItemRequest.maximumUses( itemId, ItemDatabase.getConsumptionType( itemId ) );
			int quantityDesired =
				maximum < 2 ? maximum : InputFieldUtilities.getQuantity(
					"Creating " + selection.getName() + " for immediate use...", Math.min( maximum,
						selection.getQuantityPossible() + selection.getQuantityPullable() ) );

			if ( quantityDesired < 1 )
			{
				continue;
			}

			KoLmafia.updateDisplay( "Verifying ingredients..." );
			int pulled = Math.max( 0, quantityDesired - selection.getQuantityPossible() );
			selection.setQuantityNeeded( quantityDesired - pulled );

			SpecialOutfit.createImplicitCheckpoint();
			RequestThread.postRequest( selection );
			SpecialOutfit.restoreImplicitCheckpoint();
			if ( pulled > 0 && KoLmafia.permitsContinue() )
			{
				int newbudget = ConcoctionDatabase.getPullsBudgeted() - pulled;
				RequestThread.postRequest( new StorageRequest(
					StorageRequest.STORAGE_TO_INVENTORY,
					new AdventureResult[] { ItemPool.get( selection.getItemId(), pulled ) } ) );
				ConcoctionDatabase.setPullsBudgeted( newbudget );
			}

			RequestThread.postRequest( new UseItemRequest( new AdventureResult(
				selection.getItemId(), quantityDesired ) ) );
		}
	}
}
