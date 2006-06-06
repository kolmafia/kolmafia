/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan
 * management functionality of Kingdom of Loathing.
 */

public class HagnkStorageFrame extends KoLFrame
{
	private HagnkStoragePanel all, equip;
	private static String pullsRemaining = "";

	public HagnkStorageFrame()
	{
		super( "Ancestral Storage" );
		setTitle( pullsRemaining );

		// Finally, add the actual content to the
		// storage frame.

		tabs = new JTabbedPane();
		all = new HagnkStoragePanel( false );
		equip = new HagnkStoragePanel( true );

		tabs.addTab( "All Items", all );
		tabs.addTab( "Equipment", equip );

		framePanel.add( tabs, BorderLayout.CENTER );

		if ( KoLCharacter.getStorage().isEmpty() && StaticEntity.getClient().shouldMakeConflictingRequest() )
			(new RequestThread( new ItemStorageRequest( StaticEntity.getClient() ) )).start();
	}

	public static void setPullsRemaining( String pullsRemaining )
	{
		HagnkStorageFrame.pullsRemaining = pullsRemaining;

		KoLFrame [] frames = new KoLFrame[ existingFrames.size() ];
		existingFrames.toArray( frames );

		for ( int i = 0; i < frames.length; ++i )
			if ( frames[i] instanceof HagnkStorageFrame )
				frames[i].setTitle( pullsRemaining );
	}

	private class HagnkStoragePanel extends MultiButtonPanel
	{
		private boolean isEquipment;
		private FilterCheckBox [] consumeFilters;
		private FilterRadioButton [] equipmentFilters;

		public HagnkStoragePanel( boolean isEquipment )
		{
			super( "Inside Storage", KoLCharacter.getStorage(), !isEquipment );

			setButtons( new String [] { "put in bag", "put in closet", "take it all" },
				new ActionListener [] { new PullFromStorageListener( false ), new PullFromStorageListener( true ), new EmptyStorageListener() } );

			movers[2].setSelected( true );
			this.isEquipment = isEquipment;

			if ( isEquipment )
			{
				elementList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

				equipmentFilters = new FilterRadioButton[7];
				equipmentFilters[0] = new FilterRadioButton( "weapons", true );
				equipmentFilters[1] = new FilterRadioButton( "offhand" );
				equipmentFilters[2] = new FilterRadioButton( "hats" );
				equipmentFilters[3] = new FilterRadioButton( "shirts" );
				equipmentFilters[4] = new FilterRadioButton( "pants" );
				equipmentFilters[5] = new FilterRadioButton( "accessories" );
				equipmentFilters[6] = new FilterRadioButton( "familiar" );

				ButtonGroup filterGroup = new ButtonGroup();
				for ( int i = 0; i < 7; ++i )
				{
					filterGroup.add( equipmentFilters[i] );
					optionPanel.add( equipmentFilters[i] );
				}

				elementList.setCellRenderer( AdventureResult.getEquipmentCellRenderer( true, false, false, false, false, false, false ) );
			}
			else
			{
				consumeFilters = new FilterCheckBox[3];
				consumeFilters[0] = new FilterCheckBox( consumeFilters, elementList, "Show food", true );
				consumeFilters[1] = new FilterCheckBox( consumeFilters, elementList, "Show drink", true );
				consumeFilters[2] = new FilterCheckBox( consumeFilters, elementList, "Show others", true );

				for ( int i = 0; i < consumeFilters.length; ++i )
					optionPanel.add( consumeFilters[i] );

				elementList.setCellRenderer(
					AdventureResult.getConsumableCellRenderer( true, true, true ) );
			}
		}

		protected Object [] getDesiredItems( String message )
		{
			// Ensure that the selection interval does not include
			// anything that was filtered out by the checkboxes.

			if ( !isEquipment )
			{
				filterSelection( consumeFilters[0].isSelected(), consumeFilters[1].isSelected(),
					consumeFilters[2].isSelected(), true, true );
			}

			return super.getDesiredItems( message );
		}

		private class FilterRadioButton extends JRadioButton implements ActionListener
		{
			public FilterRadioButton( String label )
			{	this( label, false );
			}

			public FilterRadioButton( String label, boolean isSelected )
			{
				super( label, isSelected );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				elementList.setCellRenderer( AdventureResult.getEquipmentCellRenderer(
					equipmentFilters[0].isSelected(), equipmentFilters[1].isSelected(), equipmentFilters[2].isSelected(), equipmentFilters[3].isSelected(),
					equipmentFilters[4].isSelected(), equipmentFilters[5].isSelected(), equipmentFilters[6].isSelected() ) );
				elementList.validate();
			}
		}

		private class PullFromStorageListener implements ActionListener
		{
			private boolean isCloset;

			public PullFromStorageListener( boolean withdraw )
			{	this.isCloset = isCloset;
			}

			public void actionPerformed( ActionEvent e )
			{
				Object [] items = getDesiredItems( "Pulling" );
				if ( items == null )
					return;

				Runnable [] requests = isCloset ? new Runnable[2] : new Runnable[1];
				requests[0] = new ItemStorageRequest( StaticEntity.getClient(), ItemStorageRequest.STORAGE_TO_INVENTORY, items );

				if ( isCloset )
					requests[1] = new ItemStorageRequest( StaticEntity.getClient(), ItemStorageRequest.INVENTORY_TO_CLOSET, items );

				(new RequestThread( requests )).start();
			}
		}

		private class EmptyStorageListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( !KoLCharacter.canInteract() )
				{
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You are not yet out of Ronin." );
					return;
				}

				(new RequestThread( new ItemStorageRequest( StaticEntity.getClient(), ItemStorageRequest.EMPTY_STORAGE ) )).start();
			}
		}
	}
}
