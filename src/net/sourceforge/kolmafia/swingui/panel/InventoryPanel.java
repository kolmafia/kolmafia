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

import java.awt.event.ActionListener;

import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

public class InventoryPanel
	extends ItemManagePanel
{
	protected boolean isEquipmentOnly;
	private FilterRadioButton[] equipmentFilters;

	public InventoryPanel( final LockableListModel elementModel, final boolean isEquipmentOnly )
	{
		super( elementModel );
		this.isEquipmentOnly = isEquipmentOnly;

		boolean isCloset = elementModel == KoLConstants.closet;
		ActionListener useListener = isEquipmentOnly ?
			(ActionListener) new EquipListener( isCloset ) :
			(ActionListener) new ConsumeListener( isCloset );

		ArrayList listeners = new ArrayList();
		listeners.add( useListener );
		listeners.add( new AutoSellListener( isCloset, true ) );
		listeners.add( new AutoSellListener( isCloset, false ) );
		listeners.add( new PulverizeListener( isCloset ) );
		listeners.add( new PutInClosetListener( isCloset ) );
		listeners.add( new PutOnDisplayListener( isCloset ) );
		listeners.add( new GiveToClanListener( isCloset ) );
		if ( isEquipmentOnly )
			listeners.add( new FamiliarFeedListener() );
		this.setButtons( true, (ActionListener[]) listeners.toArray( new ActionListener[ listeners.size() ] ));

		if ( this.isEquipmentOnly )
		{
			this.elementList.setCellRenderer( ListCellRendererFactory.getEquipmentPowerRenderer() );
		}
		else
		{
			this.elementList.setCellRenderer( ListCellRendererFactory.getDefaultRenderer() );
		}

		if ( this.movers != null )
		{
			this.movers[ 2 ].setSelected( true );
		}
	}

	public InventoryPanel( final String confirmText, final String cancelText, final LockableListModel model,
		final boolean isEquipmentOnly )
	{
		super( confirmText, cancelText, model );
		this.isEquipmentOnly = isEquipmentOnly;

		this.addFilters();

		if ( this.isEquipmentOnly )
		{
			this.elementList.setCellRenderer( ListCellRendererFactory.getEquipmentPowerRenderer() );
		}
		else
		{
			this.elementList.setCellRenderer( ListCellRendererFactory.getDefaultRenderer() );
		}
	}

	public void addFilters()
	{
		if ( !this.isEquipmentOnly )
		{
			super.addFilters();
			return;
		}

		this.equipmentFilters = new FilterRadioButton[ 7 ];
		this.equipmentFilters[ 0 ] = new FilterRadioButton( "weapons", true );
		this.equipmentFilters[ 1 ] = new FilterRadioButton( "offhand" );
		this.equipmentFilters[ 2 ] = new FilterRadioButton( "hats" );
		this.equipmentFilters[ 3 ] = new FilterRadioButton( "shirts" );
		this.equipmentFilters[ 4 ] = new FilterRadioButton( "pants" );
		this.equipmentFilters[ 5 ] = new FilterRadioButton( "accessories" );
		this.equipmentFilters[ 6 ] = new FilterRadioButton( "familiar" );

		ButtonGroup filterGroup = new ButtonGroup();
		JPanel filterPanel = new JPanel();

		for ( int i = 0; i < 7; ++i )
		{
			filterGroup.add( this.equipmentFilters[ i ] );
			filterPanel.add( this.equipmentFilters[ i ] );
		}

		this.northPanel.add( filterPanel, BorderLayout.NORTH );
		this.filterItems();
	}

	public AutoFilterTextField getWordFilter()
	{
		return new EquipmentFilterField();
	}

	private class FilterRadioButton
		extends JRadioButton
	{
		public FilterRadioButton( final String label )
		{
			this( label, false );
		}

		public FilterRadioButton( final String label, final boolean isSelected )
		{
			super( label, isSelected );
			InventoryPanel.this.listenToRadioButton( this );
		}
	}

	private class EquipmentFilterField
		extends FilterItemField
	{
		public boolean isVisible( final Object element )
		{
			if ( InventoryPanel.this.equipmentFilters == null )
			{
				return super.isVisible( element );
			}

			if ( element instanceof AdventureResult && !( (AdventureResult) element ).isItem() )
			{
				return false;
			}

			boolean isVisibleWithFilter = true;

			if ( element == null )
			{
				return false;
			}

			String name =
				element instanceof AdventureResult ? ( (AdventureResult) element ).getName() : element instanceof CreateItemRequest ? ( (CreateItemRequest) element ).getName() : null;

			if ( name == null )
			{
				return true;
			}

			switch ( ItemDatabase.getConsumptionType( name ) )
			{
			case KoLConstants.EQUIP_WEAPON:
				isVisibleWithFilter = InventoryPanel.this.equipmentFilters[ 0 ].isSelected();
				break;

			case KoLConstants.EQUIP_OFFHAND:
				isVisibleWithFilter = InventoryPanel.this.equipmentFilters[ 1 ].isSelected();
				break;

			case KoLConstants.EQUIP_HAT:
				isVisibleWithFilter = InventoryPanel.this.equipmentFilters[ 2 ].isSelected();
				break;

			case KoLConstants.EQUIP_SHIRT:
				isVisibleWithFilter = InventoryPanel.this.equipmentFilters[ 3 ].isSelected();
				break;

			case KoLConstants.EQUIP_PANTS:
				isVisibleWithFilter = InventoryPanel.this.equipmentFilters[ 4 ].isSelected();
				break;

			case KoLConstants.EQUIP_ACCESSORY:
				isVisibleWithFilter = InventoryPanel.this.equipmentFilters[ 5 ].isSelected();
				break;

			case KoLConstants.EQUIP_FAMILIAR:
				isVisibleWithFilter = InventoryPanel.this.equipmentFilters[ 6 ].isSelected();
				break;

			default:
				return false;
			}

			return isVisibleWithFilter && super.isVisible( element );
		}
	}

	private class FamiliarFeedListener
		extends ThreadedListener
	{
		public FamiliarFeedListener()
		{
		}

		protected void execute()
		{
			Object [] items = InventoryPanel.this.getDesiredItems( "Feed" );

			if ( items == null )
			{
				return;
			}

			for ( int i = 0; i < items.length; ++i )
			{
				AdventureResult item = (AdventureResult) items[ i ];

				RequestThread.postRequest( new UseItemRequest( KoLConstants.CONSUME_SLIME, item ) );
			}
		}

		public String toString()
		{
			return "feed slimeling";
		}
	}
}
