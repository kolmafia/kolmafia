/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.PreferenceListener;
import net.sourceforge.kolmafia.preferences.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.TrendyRequest;

import net.sourceforge.kolmafia.swingui.panel.CardLayoutSelectorPanel;
import net.sourceforge.kolmafia.swingui.panel.CreateItemPanel;
import net.sourceforge.kolmafia.swingui.panel.CreateSpecialPanel;
import net.sourceforge.kolmafia.swingui.panel.InventoryPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.panel.LabeledPanel;
import net.sourceforge.kolmafia.swingui.panel.OverlapPanel;
import net.sourceforge.kolmafia.swingui.panel.PulverizePanel;
import net.sourceforge.kolmafia.swingui.panel.RestorativeItemPanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemDequeuePanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemEnqueuePanel;
import net.sourceforge.kolmafia.swingui.panel.UseItemPanel;

import net.sourceforge.kolmafia.swingui.widget.AutoHighlightSpinner;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.ListCellRendererFactory;

import net.sourceforge.kolmafia.textui.command.AutoMallCommand;
import net.sourceforge.kolmafia.textui.command.CleanupJunkRequest;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ItemManageFrame
	extends GenericFrame
{
	private static final JLabel pullsRemainingLabel1 = new JLabel( " " );
	private static final JLabel pullsRemainingLabel2 = new JLabel( " " );
	private static final PullBudgetSpinner pullBudgetSpinner1 = new PullBudgetSpinner();
	private static final PullBudgetSpinner pullBudgetSpinner2 = new PullBudgetSpinner();

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
		selectorPanel.addPanel( " - Free Pulls", new FreePullsPanel() );

		selectorPanel.addSeparator();

		selectorPanel.addPanel( "Creatable", new CreateItemPanel( true, true, true, true ) );

		selectorPanel.addPanel( " - Cookable", new CreateItemPanel( true, false, false, false ) );
		selectorPanel.addPanel( " - Mixable", new CreateItemPanel( false, true, false, false ) );
		selectorPanel.addPanel( " - Fine Tuning", new CreateSpecialPanel() );

		selectorPanel.addSeparator();

		selectorPanel.addPanel( "Equipment", new InventoryPanel( KoLConstants.inventory, true ) );
		selectorPanel.addPanel( " - Storage", new HagnkStoragePanel( true ) );
		selectorPanel.addPanel( " - Create", new CreateItemPanel( false, false, true, false ) );
		selectorPanel.addPanel( " - Pulverize", new PulverizePanel() );

		// Now a special panel which does nothing more than list
		// some common actions and some descriptions.

		selectorPanel.addSeparator();

		selectorPanel.addPanel( "Item Filters", new ItemFilterPanel() );
		selectorPanel.addPanel( " - Mementos", new MementoItemsPanel() );
		selectorPanel.addPanel( " - Cleanup", new JunkItemsPanel() );
		selectorPanel.addPanel( " - Keep One", new SingletonItemsPanel() );
		selectorPanel.addPanel( " - Restock", new RestockPanel() );

		selectorPanel.setSelectedIndex( Preferences.getInteger( "itemManagerIndex" ) );

		this.setCenterComponent( selectorPanel );
	}

	public static void updatePullsRemaining( final int pullsRemaining )
	{
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

	public static void updatePullsBudgeted( final int pullsBudgeted )
	{
		ItemManageFrame.pullBudgetSpinner1.setValue( new Integer( pullsBudgeted ) );
		ItemManageFrame.pullBudgetSpinner2.setValue( new Integer( pullsBudgeted ) );
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
			CleanupJunkRequest.cleanup();
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

			AutoMallCommand.automall();
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
			this.elementList.setCellRenderer( ListCellRendererFactory.getStorageRenderer() );

			Box box = Box.createVerticalBox();
			JLabel budget = new JLabel( "Budget:" );
			budget.setToolTipText(
				"Sets the number of pulls KoLmafia is allowed to use\n" +
				"to fulfill item consumption and other usage requests" );
			box.add( budget );
			box.add( Box.createVerticalStrut( 5 ) );
			if ( isEquipmentOnly )
			{
				ItemManageFrame.pullBudgetSpinner1.setHorizontalAlignment(
					AutoHighlightTextField.RIGHT );
				JComponentUtilities.setComponentSize(
					ItemManageFrame.pullBudgetSpinner1, 56, 24 );
				box.add( ItemManageFrame.pullBudgetSpinner1 );
				box.add( Box.createVerticalStrut( 5 ) );
				box.add( ItemManageFrame.pullsRemainingLabel1 );
			}
			else
			{
				ItemManageFrame.pullBudgetSpinner2.setHorizontalAlignment(
					AutoHighlightTextField.RIGHT );
				JComponentUtilities.setComponentSize(
					ItemManageFrame.pullBudgetSpinner2, 56, 24 );
				box.add( ItemManageFrame.pullBudgetSpinner2 );
				box.add( Box.createVerticalStrut( 5 ) );
				box.add( ItemManageFrame.pullsRemainingLabel2 );
			}
			this.eastPanel.add( box, BorderLayout.SOUTH );
		}

		public void addMovers()
		{
			if ( !this.isEquipmentOnly )
			{
				super.addMovers();
			}
		}

		protected int getDesiredItemAmount( final Object item, final String itemName, final int itemCount, final String message, final int quantityType )
		{
			if ( !this.isPullingForUse || quantityType != ItemManagePanel.TAKE_MULTIPLE )
			{
				return super.getDesiredItemAmount( item, itemName, itemCount, message, quantityType );
			}

			int consumptionType = ItemDatabase.getConsumptionType( ( (AdventureResult) item ).getItemId() );
			switch ( consumptionType )
			{
			case KoLConstants.EQUIP_HAT:
			case KoLConstants.EQUIP_PANTS:
			case KoLConstants.EQUIP_SHIRT:
			case KoLConstants.EQUIP_CONTAINER:
			case KoLConstants.EQUIP_WEAPON:
			case KoLConstants.EQUIP_OFFHAND:
				return 1;

			default:
				return super.getDesiredItemAmount( item, itemName, itemCount, message, quantityType );
			}
		}

		private Object[] pullItems( final boolean isPullingForUse )
		{
			this.isPullingForUse = isPullingForUse;
			Object[] items = this.getDesiredItems( "Pulling" );

			if ( items == null )
			{
				return null;
			}

			// Trendy characters can't pull untrendy items
			if ( KoLCharacter.isTrendy() )
			{
				for ( int i = 0; i < items.length; ++i )
				{
					AdventureResult item = (AdventureResult) items[ i ];
					String itemName = item.getName();
					if ( !TrendyRequest.isTrendy( "Items", itemName ) )
					{
						items[ i ] = null;
					}
				}
			}

			if ( items.length == KoLConstants.storage.size() )
			{
				RequestThread.postRequest( new StorageRequest( StorageRequest.EMPTY_STORAGE ) );
			}
			else
			{
				RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, items ) );
			}

			return items;
		}

		public void actionConfirmed()
		{
			this.pullItems( false );
		}

		public void actionCancelled()
		{
			Object[] items = this.pullItems( this.isEquipmentOnly );
			if ( items == null )
			{
				return;
			}

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
		}
	}

	private class FreePullsPanel
		extends InventoryPanel
	{
		public FreePullsPanel()
		{
			super( "pull item", "closet item", KoLConstants.freepulls, false );

			this.addFilters();
			this.addMovers();
			this.elementList.setCellRenderer( ListCellRendererFactory.getFreePullsRenderer() );
		}

		public void addMovers()
		{
			super.addMovers();
		}

		private Object[] pullItems()
		{
			Object[] items = this.getDesiredItems( "Pulling" );

			if ( items == null )
			{
				return null;
			}

			RequestThread.postRequest( new StorageRequest( StorageRequest.STORAGE_TO_INVENTORY, items ) );
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

			RequestThread.postRequest( new ClosetRequest( ClosetRequest.INVENTORY_TO_CLOSET, items ) );
		}
	}

	private static class PullBudgetSpinner
		extends AutoHighlightSpinner
		implements ChangeListener
	{
		public PullBudgetSpinner()
		{
			super();
			this.setAlignmentX( 0.0f );
			this.addChangeListener( this );
		}

		public void stateChanged( ChangeEvent e )
		{
			int desired = InputFieldUtilities.getValue( this, 0 );
			ConcoctionDatabase.setPullsBudgeted( desired );
			ConcoctionDatabase.refreshConcoctions( true );
		}
	}

	public static class PrefPopup
		extends JComboBox
		implements ActionListener, PreferenceListener
	{
		private String pref;

		public PrefPopup( String pref )
		{
			this( pref, "1|2|3|4|5" );
		}

		public PrefPopup( String pref, String items )
		{
			super( items.split( "\\|" ) );
			this.pref = pref;
			this.addActionListener( this );
			PreferenceListenerRegistry.registerListener( pref, this );
			this.update();
		}

		public void update()
		{
			this.setSelectedItem( Preferences.getString( this.pref ) );
		}

		public void actionPerformed( ActionEvent e )
		{
			Preferences.setString( this.pref, (String) this.getSelectedItem() );
		}
	}

	private static class ItemFilterPanel
		extends LabeledPanel
	{
		public ItemFilterPanel()
		{
			super( "Number of items retained by \"all but usable\" option",
				"reset to defaults", new Dimension( 100, 20 ), new Dimension( 100, 20 ) );

			VerifiableElement[] elements = new VerifiableElement[ 10 ];
			elements[ 0 ] = new VerifiableElement( "Hats: ",
				new PrefPopup( "usableHats" ) );
			elements[ 1 ] = new VerifiableElement( "1H Weapons: ",
				new PrefPopup( "usable1HWeapons" ) );
			elements[ 2 ] = new VerifiableElement( "2H Weapons: ",
				new PrefPopup( "usable2HWeapons" ) );
			elements[ 3 ] = new VerifiableElement( "3H Weapons: ",
				new PrefPopup( "usable3HWeapons" ) );
			elements[ 4 ] = new VerifiableElement( "Off-Hands: ",
				new PrefPopup( "usableOffhands" ) );
			elements[ 5 ] = new VerifiableElement( "Shirts: ",
				new PrefPopup( "usableShirts" ) );
			elements[ 6 ] = new VerifiableElement( "Pants: ",
				new PrefPopup( "usablePants" ) );
			elements[ 7 ] = new VerifiableElement( "1x-equip Accs.: ",
				new PrefPopup( "usable1xAccs" ) );
			elements[ 8 ] = new VerifiableElement( "Accessories: ",
				new PrefPopup( "usableAccessories" ) );
			elements[ 9 ] = new VerifiableElement( "Other Items: ",
				new PrefPopup( "usableOther" ) );

			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			Preferences.resetToDefault( "usable1HWeapons" );
			Preferences.resetToDefault( "usable1xAccs" );
			Preferences.resetToDefault( "usable2HWeapons" );
			Preferences.resetToDefault( "usable3HWeapons" );
			Preferences.resetToDefault( "usableAccessories" );
			Preferences.resetToDefault( "usableHats" );
			Preferences.resetToDefault( "usableOffhands" );
			Preferences.resetToDefault( "usableOther" );
			Preferences.resetToDefault( "usablePants" );
			Preferences.resetToDefault( "usableShirts" );
		}

		public void actionCancelled()
		{
		}
	}
}
