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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;

import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.CreationSettingCheckBox;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class CreateSpecialPanel
	extends InventoryPanel
{
	private static LockableListModel temp;
	static {
		temp = new LockableListModel();
		temp.add( "(reserved for list of manual recipes)");
	}

	public CreateSpecialPanel()
	{
		super( "create item", "create & use", temp, false );

		JPanel filterPanel = new JPanel();

		filterPanel.add( new CreationSettingCheckBox(
			"require in-a-boxes", "requireBoxServants", "Do not cook/mix without -in-the-box" ) );
		filterPanel.add( new CreationSettingCheckBox(
			"repair on explosion", "autoRepairBoxServants",
			"Automatically repair -in-the-box on explosion" ) );
		filterPanel.add( new CreationSettingCheckBox(
			"use closet", "autoSatisfyWithCloset", "Look in closet for ingredients" ) );

		this.northPanel.add( filterPanel, BorderLayout.NORTH );

		this.northPanel.add( new InvSlider(), BorderLayout.EAST );
		Box box = Box.createVerticalBox();
		Box subbox = Box.createHorizontalBox();
		subbox.setAlignmentX( 0.0f );
		subbox.add( Box.createGlue() );
		subbox.add( new JLabel( "Value of ingredients already in inventory:", JLabel.RIGHT ) );
		box.add( subbox );
		box.add( Box.createGlue() );
		box.add( new JLabel( "Value of a turn spent crafting:" ) );
		box.add( new PrefSpinner( "valueOfAdventure" ) );
		box.add( Box.createGlue() );
		box.add( new JLabel( "Value of a Nash Crosby's Still use:" ) );
		box.add( new PrefSpinner( "valueOfStill" ) );
		box.add( Box.createGlue() );
		box.add( new JLabel( "Value of a Tome summon:" ) );
		box.add( new PrefSpinner( "valueOfTome" ) );
		box.add( Box.createGlue() );
		box.add( new JLabel( "List below is not implemented yet:" ) );
		this.northPanel.add( box, BorderLayout.CENTER );

		//this.setFixedFilter( food, booze, equip, other, true );

		//ConcoctionDatabase.getCreatables().updateFilter( false );
	}

	@Override
	public void addFilters()
	{
	}

	@Override
	public void actionConfirmed()
	{
		Object[] items = this.getSelectedValues();
		// Disabled for now
		for ( int i = 0; i < 0*items.length; ++i )
		{
			CreateItemRequest selection = (CreateItemRequest) items[ i ];
			Integer value = 
				InputFieldUtilities.getQuantity(
					"Creating multiple " + selection.getName() + ", " + (selection.getQuantityPossible() + selection.getQuantityPullable())
					+ " possible", selection.getQuantityPossible() + selection.getQuantityPullable(), 1 );
			int quantityDesired = ( value == null ) ? 0 : value.intValue();
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

	@Override
	public void actionCancelled()
	{
		Object[] items = this.getSelectedValues();
		// Disabled for now
		for ( int i = 0; i < 0*items.length; ++i )
		{
			CreateItemRequest selection = (CreateItemRequest) items[ i ];

			int itemId = selection.getItemId();
			int maximum = UseItemRequest.maximumUses( itemId, ItemDatabase.getConsumptionType( itemId ) );
			int quantityDesired = maximum;
			if ( maximum >= 2 )
			{
				Integer value = InputFieldUtilities.getQuantity(
					"Creating " + selection.getName() + " for immediate use...", Math.min( maximum,
						selection.getQuantityPossible() + selection.getQuantityPullable() ) );
				quantityDesired = ( value == null ) ? 0 : value.intValue();
			}

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

			RequestThread.postRequest( UseItemRequest.getInstance( ItemPool.get( selection.getItemId(), quantityDesired ) ) );
		}
	}

	private static class InvSlider
		extends JSlider
		implements ChangeListener, Listener
	{
		public InvSlider()
		{
			super( JSlider.VERTICAL, 0, 30, 18 );
			this.setMinorTickSpacing( 1 );
			this.setMajorTickSpacing( 5 );
			Hashtable h = new Hashtable();
			h.put( IntegerPool.get( 0 ), new JLabel( "Free" ) );
			h.put( IntegerPool.get( 10 ), new JLabel( "Autosell price" ) );
			h.put( IntegerPool.get( 20 ), new JLabel( "Mall (or autosell if min-priced)" ) );
			h.put( IntegerPool.get( 30 ), new JLabel( "Mall price" ) );
			this.setLabelTable( h );
			this.setPaintTicks( true );
			this.setSnapToTicks( true );
			this.setPaintLabels( true );
			this.addChangeListener( this );
			PreferenceListenerRegistry.registerPreferenceListener( "valueOfInventory", this );
			this.update();
		}

		public void stateChanged( ChangeEvent e )
		{
			//if ( this.getValueIsAdjusting() ) return;
			Preferences.setFloat( "valueOfInventory", this.getValue() / 10.0f );
		}

		public void update()
		{
			this.setValue( (int)
				((Preferences.getFloat( "valueOfInventory" ) + 0.05f) * 10.0f) );
		}
	}

	private static class PrefSpinner
		extends AutoHighlightSpinner
		implements ChangeListener, Listener
	{
		private String pref;

		// This spinner is tied to a Preference.
		//
		// Since it is a ChangeListener, whenever the user manipulates
		// the spinner, the "stateChanged" method is called to change
		// the setting, which will write it to the settings file.
		//
		// Since it is a Listener, whenever the setting
		// changes, the "update" method is called to adjust the widget.
		//
		// We do not want to write the settings file when we are
		// adjusting the widget to agree with what the setting has
		// already been changed to.

		boolean updating = false;

		public PrefSpinner( String pref )
		{
			super();
			this.pref = pref;
			this.setAlignmentX( 0.0f );
			JComponentUtilities.setComponentSize( this, 80, -1 );

			// Set the widget from the current value of the setting
			this.update();

			// Register to be informed when the setting changes
			PreferenceListenerRegistry.registerPreferenceListener( pref, this );

			// Register to be informed when the widget changes
			this.addChangeListener( this );
		}

		public void stateChanged( ChangeEvent e )
		{
			// Change the setting to agree with the widget. If we
			// are currently loading the widget from the setting,
			// do not write the setting.
			if ( !updating )
			{
				int val = InputFieldUtilities.getValue( this, 0 );
				Preferences.setInteger( this.pref, val );
			}
		}

		public void update()
		{
			// Change the widget to agree with the setting
			this.updating = true;
			this.setValue( Math.max( 0, Preferences.getInteger( this.pref ) ) );
			this.updating = false;
		}
	}
}
