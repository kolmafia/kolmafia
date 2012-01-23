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

import com.sun.java.forums.TableSorter;

import java.awt.BorderLayout;
import java.awt.Color;

import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.AutoSellRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;
import net.sourceforge.kolmafia.swingui.listener.TableButtonListener;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.panel.ItemManagePanel;
import net.sourceforge.kolmafia.swingui.table.ListWrapperTableModel;
import net.sourceforge.kolmafia.swingui.table.TransparentTable;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class StoreManageFrame
	extends GenericPanelFrame
{
	private static StoreManageFrame INSTANCE = null;
	private static final JLabel searchLabel =
		JComponentUtilities.createLabel( "Mall Prices", SwingConstants.CENTER, Color.black, Color.white );
	private static final LockableListModel priceSummary = new LockableListModel();

	private JComboBox sellingList;
	private JTable addTable, manageTable;
	private JList resultsDisplay;

	public StoreManageFrame()
	{
		super( "Store Manager" );

		this.tabs.add( "Price Setup", new StoreManagePanel() );
		this.tabs.add( "Additions", new StoreAddPanel() );
		this.tabs.add( "Removals", new StoreRemovePanel() );
		this.tabs.add( "Store Log", new StoreLogPanel() );

		this.setCenterComponent( this.tabs );

		StoreManageFrame.INSTANCE = this;

		StoreManageFrame.updateEarnings( StoreManager.getPotentialEarnings() );
	}

	public static final void cancelTableEditing()
	{
		if ( StoreManageFrame.INSTANCE != null )
		{
			InputFieldUtilities.cancelTableEditing( StoreManageFrame.INSTANCE.manageTable );
		}
	}

	public static final void updateEarnings( final long potentialEarnings )
	{
		if ( StoreManageFrame.INSTANCE == null || GenericFrame.appearsInTab( "StoreManageFrame" ) )
		{
			return;
		}

		StoreManageFrame.INSTANCE.setTitle( "Store Manager (potential earnings: " + KoLConstants.COMMA_FORMAT.format( potentialEarnings ) + " meat)" );
	}

	private class StoreManagePanel
		extends GenericPanel
	{
		public StoreManagePanel()
		{
			super( "save prices", "auto reprice", true );

			StoreManageFrame.this.addTable = new StoreListTable( null );
			GenericScrollPane addScroller = new GenericScrollPane( StoreManageFrame.this.addTable );

			JComponentUtilities.setComponentSize( addScroller, 500, 50 );
			JPanel addPanel = new JPanel( new BorderLayout() );

			addPanel.add( StoreManageFrame.this.addTable.getTableHeader(), BorderLayout.NORTH );
			addPanel.add( addScroller, BorderLayout.CENTER );

			StoreManageFrame.this.manageTable = new StoreListTable( StoreManager.getSoldItemList() );
			GenericScrollPane manageScroller = new GenericScrollPane( StoreManageFrame.this.manageTable );

			JPanel managePanel = new JPanel( new BorderLayout() );
			managePanel.add( StoreManageFrame.this.manageTable.getTableHeader(), BorderLayout.NORTH );
			managePanel.add( manageScroller, BorderLayout.CENTER );

			JPanel storePanel = new JPanel( new BorderLayout() );
			storePanel.add( addPanel, BorderLayout.NORTH );
			storePanel.add( managePanel, BorderLayout.CENTER );

			JPanel searchResults = new SearchResultsPanel();

			this.setContent( this.elements, true );
			this.eastContainer.add( searchResults, BorderLayout.CENTER );
			this.container.add( storePanel, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{
			if ( !InputFieldUtilities.finalizeTable( StoreManageFrame.this.manageTable ) )
			{
				return;
			}

			KoLmafia.updateDisplay( "Compiling reprice data..." );
			int rowCount = StoreManageFrame.this.manageTable.getRowCount();

			int[] itemId = new int[ rowCount ];
			int[] prices = new int[ rowCount ];
			int[] limits = new int[ rowCount ];

			SoldItem[] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
			StoreManager.getSoldItemList().toArray( sold );

			for ( int i = 0; i < rowCount; ++i )
			{
				itemId[ i ] =
					ItemDatabase.getItemId( (String) StoreManageFrame.this.manageTable.getValueAt( i, 0 ) );

				prices[ i ] = ( (Integer) StoreManageFrame.this.manageTable.getValueAt( i, 1 ) ).intValue();

				int oldLimit = 0;

				for ( int j = 0; j < sold.length; ++j )
				{
					if ( sold[ j ].getItemId() == itemId[ i ] )
					{
						oldLimit = sold[ j ].getLimit();
						break;
					}
				}

				limits[ i ] =
					( (Boolean) StoreManageFrame.this.manageTable.getValueAt( i, 4 ) ).booleanValue() ? Math.max(
						1, oldLimit ) : 0;
			}

			RequestThread.postRequest( new ManageStoreRequest( itemId, prices, limits ) );
		}

		public void actionCancelled()
		{
			int selected =
				JOptionPane.showConfirmDialog(
					StoreManageFrame.this, StringUtilities.basicTextWrap( StoreManageFrame.UNDERCUT_MESSAGE ), "",
					JOptionPane.YES_NO_CANCEL_OPTION );

			if ( selected != JOptionPane.YES_OPTION && selected != JOptionPane.NO_OPTION )
			{
				return;
			}

			KoLmafia.updateDisplay( "Gathering data..." );
			StoreManager.priceItemsAtLowestPrice( selected == JOptionPane.YES_OPTION );
		}
	}

	public static final String UNDERCUT_MESSAGE =
		"KoLmafia will take items priced at 999,999,999 meat and undercut the current lowest price in the mall.  Would you like KoLmafia to avoid 'minimum possible prices' (100 meat, or twice the autosell value of the item) when doing so?";

	private class StoreListTable
		extends TransparentTable
	{
		public StoreListTable( final LockableListModel model )
		{
			super( model == null ? (ListWrapperTableModel) new StoreAddTableModel() : new StoreManageTableModel() );

			if ( model == null )
			{
				this.getColumnModel().getColumn( 0 ).setCellEditor(
					new DefaultCellEditor( StoreManageFrame.this.sellingList ) );
			}
			else
			{
				this.setModel( new TableSorter( this.getModel(), this.getTableHeader() ) );
			}

			this.getTableHeader().setReorderingAllowed( false );

			this.setRowSelectionAllowed( false );

			this.addMouseListener( new TableButtonListener( this ) );

			this.setOpaque( false );
			this.setShowGrid( false );

			this.setRowHeight( 28 );

			this.getColumnModel().getColumn( 0 ).setMinWidth( 200 );

			this.getColumnModel().getColumn( 4 ).setMinWidth( 35 );
			this.getColumnModel().getColumn( 4 ).setMaxWidth( 35 );

			this.getColumnModel().getColumn( 5 ).setMinWidth( 40 );
			this.getColumnModel().getColumn( 5 ).setMaxWidth( 40 );

			this.getColumnModel().getColumn( 6 ).setMinWidth( 40 );
			this.getColumnModel().getColumn( 6 ).setMaxWidth( 40 );
		}
	}

	private class StoreAddTableModel
		extends ListWrapperTableModel
	{
		public StoreAddTableModel()
		{
			super(
				new String[] { "Item Name", "Price", " ", "Qty", "Lim", " ", " " },
				new Class[] { String.class, Integer.class, Integer.class, Integer.class, Boolean.class, JButton.class, JButton.class },
				new boolean[] { true, true, false, true, true, false, false }, new LockableListModel() );

			LockableListModel dataModel = KoLConstants.inventory.getMirrorImage( new TradeableItemFilter() );
			StoreManageFrame.this.sellingList = new JComboBox( dataModel );

			Vector value = new Vector();
			value.add( "- select an item -" );
			value.add( new Integer( 0 ) );
			value.add( new Integer( 0 ) );
			value.add( new Integer( 0 ) );
			value.add( Boolean.FALSE );

			this.listModel.add( value );
		}

		public Vector constructVector( final Object o )
		{
			Vector value = (Vector) o;
			if ( value.size() < 7 )
			{
				JButton addItemButton = new JButton( JComponentUtilities.getImage( "icon_success_sml.gif" ) );
				addItemButton.setToolTipText( "add selected item" );
				addItemButton.addMouseListener( new AddItemListener() );
				value.add( addItemButton );

				JButton searchItemButton = new JButton( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
				searchItemButton.setToolTipText( "price analysis" );
				searchItemButton.addMouseListener( new SearchItemListener() );
				value.add( searchItemButton );
			}

			return value;
		}
	}

	private class StoreManageTableModel
		extends ListWrapperTableModel
	{
		public StoreManageTableModel()
		{
			super(
				new String[] { "Item Name", "Price", "Lowest", "Qty", "Lim", " ", " " },
				new Class[] { String.class, Integer.class, Integer.class, Integer.class, Boolean.class, JButton.class, JButton.class },
				new boolean[] { false, true, false, false, true, false, false }, StoreManager.getSoldItemList() );
		}

		public Vector constructVector( final Object o )
		{
			Vector value = (Vector) o;
			if ( value.size() < 7 )
			{
				String itemName = (String) value.get( 0 );
				String displayName = StringUtilities.getDisplayName( itemName );
				value.set( 0, displayName );

				JButton removeItemButton = new JButton( JComponentUtilities.getImage( "icon_error_sml.gif" ) );
				removeItemButton.setToolTipText( "remove item from store" );
				removeItemButton.addMouseListener( new RemoveItemListener( itemName ) );
				value.add( removeItemButton );

				JButton searchItemButton = new JButton( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
				searchItemButton.setToolTipText( "price analysis" );
				searchItemButton.addMouseListener( new SearchItemListener( itemName ) );
				value.add( searchItemButton );
			}

			return value;
		}
	}

	private class AddItemListener
		extends ThreadedListener
	{
		protected void execute()
		{
			if ( !InputFieldUtilities.finalizeTable( StoreManageFrame.this.addTable ) )
			{
				return;
			}

			AdventureResult soldItem = (AdventureResult) StoreManageFrame.this.sellingList.getSelectedItem();
			if ( soldItem == null )
			{
				return;
			}

			int price = ( (Integer) StoreManageFrame.this.addTable.getValueAt( 0, 1 ) ).intValue();
			int quantity = ( (Integer) StoreManageFrame.this.addTable.getValueAt( 0, 3 ) ).intValue();

			if ( quantity <= 0 )
			{
				quantity = soldItem.getCount() - quantity;
			}

			int limit = ( (Boolean) StoreManageFrame.this.addTable.getValueAt( 0, 4 ) ).booleanValue() ? 1 : 0;
			soldItem = new AdventureResult( soldItem.getItemId(), quantity );

			StoreManageFrame.this.addTable.setValueAt( new AdventureResult( "-select an item-", 1, false ), 0, 0 );
			StoreManageFrame.this.addTable.setValueAt( new Integer( 0 ), 0, 1 );
			StoreManageFrame.this.addTable.setValueAt( new Integer( 0 ), 0, 3 );

			RequestThread.postRequest( new AutoMallRequest( soldItem, price, limit ) );
		}
	}

	private class SearchItemListener
		extends ThreadedListener
	{
		private final String itemName;

		public SearchItemListener()
		{
			this.itemName = null;
		}

		public SearchItemListener( final String itemName )
		{
			this.itemName = itemName;
		}

		protected void execute()
		{
			String searchName = this.itemName;
			if ( searchName == null )
			{
				AdventureResult item = (AdventureResult) StoreManageFrame.this.sellingList.getSelectedItem();
				if ( item == null )
				{
					return;
				}

				searchName = item.getName();
			}

			StoreManageFrame.searchLabel.setText( searchName );
			StoreManager.searchMall( "\"" + searchName + "\"", StoreManageFrame.priceSummary, 10, true );

			KoLmafia.updateDisplay( "Price analysis complete." );
		}
	}

	private class RemoveItemListener
		extends ThreadedListener
	{
		private final int itemId;

		public RemoveItemListener( final String itemName )
		{
			this.itemId = ItemDatabase.getItemId( itemName );
		}

		protected void execute()
		{
			RequestThread.postRequest( new ManageStoreRequest( this.itemId ) );
		}
	}

	private class StoreAddPanel
		extends ItemManagePanel
	{
		public StoreAddPanel()
		{
			super( "mallsell", "autosell", KoLConstants.inventory );
			this.addFilters();

			this.filters[ 4 ].setSelected( false );
			this.filters[ 4 ].setEnabled( false );
			this.filterItems();
		}

		public void actionConfirmed()
		{
			Object[] items = this.getDesiredItems( "Mallsell" );
			if ( items == null )
			{
				return;
			}

			RequestThread.postRequest( new AutoMallRequest( items ) );
			RequestThread.postRequest( new ManageStoreRequest( false ) );
		}

		public void actionCancelled()
		{
			Object[] items = this.getDesiredItems( "Autosell" );
			if ( items == null )
			{
				return;
			}
			RequestThread.postRequest( new AutoSellRequest( items ) );
		}
	}

	private class StoreRemovePanel
		extends ItemManagePanel
	{
		public StoreRemovePanel()
		{
			super( "take all", "take one", StoreManager.getSortedSoldItemList() );
			this.addFilters();

			this.filters[ 4 ].setSelected( false );
			this.filters[ 4 ].setEnabled( false );
		}

		public void actionConfirmed()
		{
			this.removeItems( true );
		}

		public void actionCancelled()
		{
			this.removeItems( false );
		}

		public void removeItems( final boolean takeAll )
		{
			StoreManageFrame.cancelTableEditing();

			Object[] items = this.elementList.getSelectedValues();

			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new ManageStoreRequest( ( (SoldItem) items[ i ] ).getItemId(), takeAll ) );
			}

			RequestThread.postRequest( new ManageStoreRequest() );
		}
	}

	/**
	 * An internal class which represents the panel used for tallying the results of the mall search request. Note that
	 * all of the tallying functionality is handled by the <code>LockableListModel</code> provided, so this functions
	 * as a container for that list model.
	 */

	private class SearchResultsPanel
		extends JPanel
	{
		public SearchResultsPanel()
		{
			super( new BorderLayout() );

			JPanel container = new JPanel( new BorderLayout() );
			container.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );

			container.add( StoreManageFrame.searchLabel, BorderLayout.NORTH );
			JComponentUtilities.setComponentSize( StoreManageFrame.searchLabel, 150, 16 );

			StoreManageFrame.this.resultsDisplay = new JList( StoreManageFrame.priceSummary );
			StoreManageFrame.this.resultsDisplay.setPrototypeCellValue( "1234567890ABCDEF" );
			StoreManageFrame.this.resultsDisplay.setVisibleRowCount( 11 );
			StoreManageFrame.this.resultsDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			GenericScrollPane scrollArea = new GenericScrollPane( StoreManageFrame.this.resultsDisplay );

			container.add( scrollArea, BorderLayout.CENTER );
			this.add( Box.createVerticalStrut( 20 ), BorderLayout.NORTH );
			this.add( container, BorderLayout.CENTER );
		}
	}

	private class StoreLogPanel
		extends ItemManagePanel
	{
		public StoreLogPanel()
		{
			super( "refresh", "resort", StoreManager.getStoreLog() );
		}

		public void actionConfirmed()
		{
			StoreManager.getStoreLog().clear();
			RequestThread.postRequest( new ManageStoreRequest( true ) );
		}

		public void actionCancelled()
		{
			StoreManager.sortStoreLog( true );
		}
	}

	private class TradeableItemFilter
		implements ListElementFilter
	{
		public boolean isVisible( final Object element )
		{
			if ( !( element instanceof AdventureResult ) )
			{
				return false;
			}

			int itemId = ( (AdventureResult) element ).getItemId();
			return itemId < 1 || ItemDatabase.isTradeable( itemId );
		}
	}
}
