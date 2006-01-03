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

import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JOptionPane;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> which handles all the clan
 * management functionality of Kingdom of Loathing.
 */

public class HagnkStorageFrame extends KoLFrame
{
	private JTabbedPane tabs;
	private HagnkStoragePanel all, equip;

	public HagnkStorageFrame( KoLmafia client )
	{
		super( client, "Ancestral Storage" );

		if ( client != null && KoLCharacter.getStorage().isEmpty() )
			(new RequestThread( new ItemStorageRequest( client ) )).start();

		// Finally, add the actual content to the
		// storage frame.

		tabs = new JTabbedPane();
		all = new HagnkStoragePanel( false );
		equip = new HagnkStoragePanel( true );

		addTab( "All Items", all );
		addTab( "Equipment", equip );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	private void addTab( String name, HagnkStoragePanel panel )
	{
		JPanel wrapperPanel = new JPanel();
		wrapperPanel.setLayout( new CardLayout( 10, 10 ) );
		wrapperPanel.add( panel, "" );
		tabs.add( name, wrapperPanel );
	}

	public void setEnabled( boolean isEnabled )
	{
		if ( all != null )
			all.setEnabled( isEnabled );
		if ( equip != null )
			equip.setEnabled( isEnabled );
	}

	private class HagnkStoragePanel extends MultiButtonPanel
	{
		private FilterRadioButton [] filters;

		public HagnkStoragePanel( boolean isEquipment )
		{
			super( "Inside Storage", KoLCharacter.getStorage(), !isEquipment );
			setButtons( new String [] { "put in backpack", "put in closet" },
				new ActionListener [] { new PullFromStorageListener( false ), new PullFromStorageListener( true ) } );

			movers[2].setSelected( true );

			if ( isEquipment )
			{
				filters = new FilterRadioButton[7];
				filters[0] = new FilterRadioButton( "weapons", true );
				filters[1] = new FilterRadioButton( "offhand" );
				filters[2] = new FilterRadioButton( "hats" );
				filters[3] = new FilterRadioButton( "shirts" );
				filters[4] = new FilterRadioButton( "pants" );
				filters[5] = new FilterRadioButton( "accessories" );
				filters[6] = new FilterRadioButton( "familiar" );

				ButtonGroup filterGroup = new ButtonGroup();
				for ( int i = 0; i < 7; ++i )
				{
					filterGroup.add( filters[i] );
					optionPanel.add( filters[i] );
				}

				elementList.setCellRenderer( AdventureResult.getEquipmentCellRenderer( true, false, false, false, false, false, false ) );
			}
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
					filters[0].isSelected(), filters[1].isSelected(), filters[2].isSelected(), filters[3].isSelected(),
					filters[4].isSelected(), filters[5].isSelected(), filters[6].isSelected() ) );
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

				Runnable [] requests = isCloset ? new Runnable[2] : new Runnable[1];
				requests[0] = new ItemStorageRequest( client, ItemStorageRequest.STORAGE_TO_INVENTORY, items );

				if ( isCloset )
					requests[1] = new ItemStorageRequest( client, ItemStorageRequest.INVENTORY_TO_CLOSET, items );

				(new RequestThread( requests )).start();
			}
		}

	}

	public static void main( String [] args )
	{	(new CreateFrameRunnable( HagnkStorageFrame.class )).run();
	}
}
