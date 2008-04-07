/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemDequeuePanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemEnqueuePanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemPanel;
import net.sourceforge.kolmafia.swingui.panel.CreateItemPanel;
import net.sourceforge.kolmafia.swingui.panel.InventoryPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.panel.OverlapPanel;
import net.sourceforge.kolmafia.swingui.panel.RestorativeItemPanel;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;

public class ItemManageFrame
	extends GenericFrame
{
	private static int pullsRemaining = 0;

	private static final JLabel pullsRemainingLabel1 = new JLabel( " " );
	private static final JLabel pullsRemainingLabel2 = new JLabel( " " );

	/**
	 * Constructs a new <code>ItemManageFrame</code> and inserts all of the necessary panels into a tabular layout for
	 * accessibility.
	 */

	public ItemManageFrame()
	{
		this( true );
	}

	public ItemManageFrame( final boolean useTabs )
	{
		super( "Item Manager" );

		JTabbedPane queueTabs;
		UseItemDequeuePanel dequeuePanel;

		CardLayoutSelectorPanel selectorPanel = new CardLayoutSelectorPanel( "itemManagerIndex" );

		selectorPanel.addPanel( "Usable", new UseItemPanel() );

		JPanel foodPanel = new JPanel( new BorderLayout() );

		queueTabs = null;

		if ( Preferences.getBoolean( "addCreationQueue" ) )
		{
			dequeuePanel = new UseItemDequeuePanel( true, false, false );
			foodPanel.add( dequeuePanel, BorderLayout.NORTH );
			queueTabs = dequeuePanel.getQueueTabs();
		}

		foodPanel.add( new UseItemEnqueuePanel( true, false, false, queueTabs ), BorderLayout.CENTER );

		selectorPanel.addPanel( " - Food", foodPanel );

		JPanel boozePanel = new JPanel( new BorderLayout() );

		queueTabs = null;

		if ( Preferences.getBoolean( "addCreationQueue" ) )
		{
			dequeuePanel = new UseItemDequeuePanel( false, true, false );
			boozePanel.add( dequeuePanel, BorderLayout.NORTH );
			queueTabs = dequeuePanel.getQueueTabs();
		}

		boozePanel.add( new UseItemEnqueuePanel( false, true, false, queueTabs ), BorderLayout.CENTER );

		selectorPanel.addPanel( " - Booze", boozePanel );

		JPanel spleenPanel = new JPanel( new BorderLayout() );

		queueTabs = null;

		if ( Preferences.getBoolean( "addCreationQueue" ) )
		{
			dequeuePanel = new UseItemDequeuePanel( false, false, true );
			spleenPanel.add( dequeuePanel, BorderLayout.NORTH );
			queueTabs = dequeuePanel.getQueueTabs();
		}

		spleenPanel.add( new UseItemEnqueuePanel( false, false, true, queueTabs ), BorderLayout.CENTER );

		selectorPanel.addPanel( " - Spleen", spleenPanel );

		selectorPanel.addPanel( " - Restores", new RestorativeItemPanel() );

		selectorPanel.addSeparator();

		selectorPanel.addPanel( "General", new InventoryPanel( KoLConstants.inventory, false ) );
		selectorPanel.addPanel( " - Recent", new InventoryPanel( KoLConstants.tally, false ) );
		selectorPanel.addPanel( " - Closet", new InventoryPanel( KoLConstants.closet, false ) );
		selectorPanel.addPanel( " - Storage", new HagnkStoragePanel( false ) );

		selectorPanel.addSeparator();

		selectorPanel.addPanel( "Creatable", new CreateItemPanel( true, true, true, true ) );

		selectorPanel.addPanel( " - Cookable", new CreateItemPanel( true, false, false, false ) );
		selectorPanel.addPanel( " - Mixable", new CreateItemPanel( false, true, false, false ) );

		selectorPanel.addSeparator();

		selectorPanel.addPanel( "Equipment", new InventoryPanel( KoLConstants.inventory, true ) );
		selectorPanel.addPanel( " - Storage", new HagnkStoragePanel( true ) );
		selectorPanel.addPanel( " - Create", new CreateItemPanel( false, false, true, false ) );

		// Now a special panel which does nothing more than list
		// some common actions and some descriptions.

		selectorPanel.addSeparator();

		selectorPanel.addPanel( "Item Filters", new JPanel() );
		selectorPanel.addPanel( " - Mementos", new MementoItemsPanel() );
		selectorPanel.addPanel( " - Cleanup", new JunkItemsPanel() );
		selectorPanel.addPanel( " - Keep One", new SingletonItemsPanel() );
		selectorPanel.addPanel( " - Restock", new RestockPanel() );

		selectorPanel.setSelectedIndex( Preferences.getInteger( "itemManagerIndex" ) );

		this.framePanel.add( selectorPanel, BorderLayout.CENTER );
	}

	public static final int getPullsRemaining()
	{
		return ItemManageFrame.pullsRemaining;
	}

	public static final void setPullsRemaining( final int pullsRemaining )
	{
		ItemManageFrame.pullsRemaining = pullsRemaining;

		if ( KoLCharacter.isHardcore() )
		{
			ItemManageFrame.pullsRemainingLabel1.setText( "In Hardcore" );
			ItemManageFrame.pullsRemainingLabel2.setText( "In Hardcore" );
			return;
		}

		switch ( pullsRemaining )
		{
		case 0:
			ItemManageFrame.pullsRemainingLabel1.setText( "No Pulls Left" );
			ItemManageFrame.pullsRemainingLabel2.setText( "No Pulls Left" );
			break;
		case 1:
			ItemManageFrame.pullsRemainingLabel1.setText( "1 Pull Left" );
			ItemManageFrame.pullsRemainingLabel2.setText( "1 Pull Left" );
			break;
		default:
			ItemManageFrame.pullsRemainingLabel1.setText( pullsRemaining + " Pulls Left" );
			ItemManageFrame.pullsRemainingLabel2.setText( pullsRemaining + " Pulls Left" );
		}
	}

	private class JunkItemsPanel
		extends OverlapPanel
	{
		public JunkItemsPanel()
		{
			super( "cleanup", "help", KoLConstants.junkList, true );
		}

		public void actionConfirmed()
		{
			StaticEntity.getClient().makeJunkRemovalRequest();
		}

		public void actionCancelled()
		{
			InputFieldUtilities.alert( "These items have been flagged as \"junk\" because at some point in the past, you've opted to autosell all of the item.  If you use the \"cleanup\" command, KoLmafia will dispose of these items either by pulverizing them (equipment) or autoselling them (non-equipment)." );
		}
	}

	private class SingletonItemsPanel
		extends OverlapPanel
	{
		public SingletonItemsPanel()
		{
			super( "closet", "help", KoLConstants.singletonList, true );
		}

		public void actionConfirmed()
		{
			AdventureResult current;
			AdventureResult[] items = new AdventureResult[ KoLConstants.singletonList.size() ];
			for ( int i = 0; i < KoLConstants.singletonList.size(); ++i )
			{
				current = (AdventureResult) KoLConstants.singletonList.get( i );
				items[ i ] =
					current.getInstance( Math.min( current.getCount( KoLConstants.inventory ), Math.max(
						0, 1 - current.getCount( KoLConstants.closet ) ) ) );
			}

			RequestThread.postRequest( new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, items ) );

		}

		public void actionCancelled()
		{
			InputFieldUtilities.alert( "These items are flagged as \"singletons\".  Using the \"closet\" button, KoLmafia will try to ensure that at least one of the item exists in your closet.\n\nIF THE PLAYER IS STILL IN HARDCORE OR RONIN, these items are treated as a special class of junk items where during the \"cleanup\" routine mentioned in the junk tab, KoLmafia will attempt to leave one of the item in the players inventory.\n\nPlease take note that once the player breaks Ronin, KoLmafia will treat these items as normal junk and ignore the general preservation rule." );
		}
	}

	private class MementoItemsPanel
		extends OverlapPanel
	{
		public MementoItemsPanel()
		{
			super( "closet", "help", KoLConstants.mementoList, true );
		}

		public void actionConfirmed()
		{
			AdventureResult current;
			AdventureResult[] items = new AdventureResult[ KoLConstants.mementoList.size() ];
			for ( int i = 0; i < KoLConstants.mementoList.size(); ++i )
			{
				current = (AdventureResult) KoLConstants.mementoList.get( i );
				items[ i ] = current.getInstance( current.getCount( KoLConstants.inventory ) );
			}

			RequestThread.postRequest( new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, items ) );
		}

		public void actionCancelled()
		{
			InputFieldUtilities.alert( "These items are flagged as \"mementos\".  IF YOU SET A PREFERENCE, KoLmafia will never sell or pulverize these items." );
		}
	}

	private class RestockPanel
		extends OverlapPanel
	{
		public RestockPanel()
		{
			super( "automall", "host sale", KoLConstants.profitableList, true );

			this.filters[ 4 ].setSelected( false );
			this.filters[ 4 ].setEnabled( false );
			this.filterItems();
		}

		public void actionConfirmed()
		{
			if ( !InputFieldUtilities.confirm( "ALL OF THE ITEMS IN THIS LIST, not just the ones you've selected, will be placed into your store.  Are you sure you wish to continue?" ) )
			{
				return;
			}

			StaticEntity.getClient().makeAutoMallRequest();
		}

		public void actionCancelled()
		{
			int selected =
				JOptionPane.showConfirmDialog(
					ItemManageFrame.this,
					StringUtilities.basicTextWrap( "KoLmafia will place all tradeable, autosellable items into your store at 999,999,999 meat. " + StoreManageFrame.UNDERCUT_MESSAGE ),
					"", JOptionPane.YES_NO_CANCEL_OPTION );

			if ( selected != JOptionPane.YES_OPTION && selected != JOptionPane.NO_OPTION )
			{
				return;
			}

			KoLmafia.updateDisplay( "Gathering data..." );
			StaticEntity.getClient().makeEndOfRunSaleRequest( selected == JOptionPane.YES_OPTION );
		}
	}

	private class HagnkStoragePanel
		extends InventoryPanel
	{
		private boolean isPullingForUse = false;

		public HagnkStoragePanel( final boolean isEquipmentOnly )
		{
			super( "pull item", isEquipmentOnly ? "pull & equip" : "closet item", KoLConstants.storage, isEquipmentOnly );

			this.addFilters();
			this.addMovers();

			if ( isEquipmentOnly )
			{
				this.eastPanel.add( ItemManageFrame.pullsRemainingLabel1, BorderLayout.SOUTH );
			}
			else
			{
				this.eastPanel.add( ItemManageFrame.pullsRemainingLabel2, BorderLayout.SOUTH );
			}
		}

		public void addMovers()
		{
			if ( !this.isEquipmentOnly )
			{
				super.addMovers();
			}
		}

		protected int getDesiredItemAmount( final Object item, final String itemName, final int itemCount,
			final String message, final int quantityType )
		{
			if ( !this.isPullingForUse && !this.isEquipmentOnly || quantityType != ItemManagePanel.TAKE_MULTIPLE )
			{
				return super.getDesiredItemAmount( item, itemName, itemCount, message, quantityType );
			}

			int consumptionType = ItemDatabase.getConsumptionType( ( (AdventureResult) item ).getItemId() );
			switch ( consumptionType )
			{
			case KoLConstants.EQUIP_HAT:
			case KoLConstants.EQUIP_PANTS:
			case KoLConstants.EQUIP_SHIRT:
			case KoLConstants.EQUIP_WEAPON:
			case KoLConstants.EQUIP_OFFHAND:
				return 1;

			default:
				return super.getDesiredItemAmount( item, itemName, itemCount, message, quantityType );
			}
		}

		private Object[] pullItems()
		{
			this.isPullingForUse = true;
			Object[] items = this.getDesiredItems( "Pulling" );
			this.isPullingForUse = false;

			if ( items == null )
			{
				return null;
			}

			RequestThread.openRequestSequence();

			if ( items.length == KoLConstants.storage.size() )
			{
				RequestThread.postRequest( new ClosetRequest( ClosetRequest.EMPTY_STORAGE ) );
			}
			else
			{
				RequestThread.postRequest( new ClosetRequest( ClosetRequest.STORAGE_TO_INVENTORY, items ) );
			}

			RequestThread.closeRequestSequence();
			return items;
		}

		public void actionConfirmed()
		{
			this.pullItems();
		}

		public void actionCancelled()
		{
			Object[] items = this.pullItems();
			if ( items == null )
			{
				return;
			}

			RequestThread.closeRequestSequence();

			if ( this.isEquipmentOnly )
			{
				for ( int i = 0; i < items.length; ++i )
				{
					RequestThread.postRequest( new EquipmentRequest( (AdventureResult) items[ i ] ) );
				}
			}
			else
			{
				RequestThread.postRequest( new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, items ) );
			}

			RequestThread.closeRequestSequence();
		}
	}
}
