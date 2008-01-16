/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseEvent;
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

import com.sun.java.forums.TableSorter;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.SellStuffRequest;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

public class StoreManageFrame
	extends KoLPanelFrame
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
		StoreManageFrame.INSTANCE = this;

		this.tabs.add( "Price Setup", new StoreManagePanel() );
		this.tabs.add( "Additions", new StoreAddPanel() );
		this.tabs.add( "Removals", new StoreRemovePanel() );
		this.tabs.add( "Store Log", new StoreLogPanel() );

		this.framePanel.add( this.tabs, BorderLayout.CENTER );
		StoreManageFrame.updateEarnings( StoreManager.getPotentialEarnings() );
	}

	public static final void updateEarnings( final long potentialEarnings )
	{
		if ( StoreManageFrame.INSTANCE == null || KoLFrame.appearsInTab( "StoreManageFrame" ) )
		{
			return;
		}

		StoreManageFrame.INSTANCE.setTitle( "Store Manager (potential earnings: " + KoLConstants.COMMA_FORMAT.format( potentialEarnings ) + " meat)" );
	}

	public void dispose()
	{
		StoreManageFrame.INSTANCE = null;
		super.dispose();
	}

	private class StoreManagePanel
		extends KoLPanel
	{
		public StoreManagePanel()
		{
			super( "save prices", "auto reprice", true );

			StoreManageFrame.this.addTable = new StoreListTable( null );
			SimpleScrollPane addScroller = new SimpleScrollPane( StoreManageFrame.this.addTable );

			JComponentUtilities.setComponentSize( addScroller, 500, 50 );
			JPanel addPanel = new JPanel( new BorderLayout() );

			addPanel.add( StoreManageFrame.this.addTable.getTableHeader(), BorderLayout.NORTH );
			addPanel.add( addScroller, BorderLayout.CENTER );

			StoreManageFrame.this.manageTable = new StoreListTable( StoreManager.getSoldItemList() );
			SimpleScrollPane manageScroller = new SimpleScrollPane( StoreManageFrame.this.manageTable );

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
			if ( !StoreManageFrame.this.finalizeTable( StoreManageFrame.this.manageTable ) )
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
					StoreManageFrame.this, KoLFrame.basicTextWrap( StoreManageFrame.UNDERCUT_MESSAGE ), "",
					JOptionPane.YES_NO_CANCEL_OPTION );

			if ( selected != JOptionPane.YES_OPTION && selected != JOptionPane.NO_OPTION )
			{
				return;
			}

			KoLmafia.updateDisplay( "Gathering data..." );
			StaticEntity.getClient().priceItemsAtLowestPrice( selected == JOptionPane.YES_OPTION );
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

			this.addMouseListener( new ButtonEventListener( this ) );
			this.setDefaultRenderer( Integer.class, new IntegerRenderer() );
			this.setDefaultRenderer( JButton.class, new ButtonRenderer() );

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
				value.add( new AddItemButton() );
				value.add( new SearchItemButton() );
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
				value.add( new RemoveItemButton( (String) value.get( 0 ) ) );
				value.add( new SearchItemButton( (String) value.get( 0 ) ) );
			}

			return value;
		}
	}

	private class AddItemButton
		extends NestedInsideTableButton
	{
		public AddItemButton()
		{
			super( JComponentUtilities.getImage( "icon_success_sml.gif" ) );
			this.setToolTipText( "add selected item" );
		}

		public void mouseReleased( final MouseEvent e )
		{
			if ( !StoreManageFrame.this.finalizeTable( StoreManageFrame.this.addTable ) )
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

			RequestThread.postRequest( new SellStuffRequest( soldItem, price, limit ) );
		}
	}

	private class SearchItemButton
		extends NestedInsideTableButton
	{
		private final String itemName;

		public SearchItemButton()
		{
			this( null );
		}

		public SearchItemButton( final String itemName )
		{
			super( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			this.itemName = itemName;
			this.setToolTipText( "price analysis" );
		}

		public void mouseReleased( final MouseEvent e )
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
			StoreManager.searchMall( searchName, StoreManageFrame.priceSummary, 10, true );

			StoreManageFrame.this.resultsDisplay.updateUI();

			KoLmafia.updateDisplay( "Price analysis complete." );
			RequestThread.enableDisplayIfSequenceComplete();
		}
	}

	private class RemoveItemButton
		extends NestedInsideTableButton
	{
		private final int itemId;

		public RemoveItemButton( final String itemName )
		{
			super( JComponentUtilities.getImage( "icon_error_sml.gif" ) );
			this.itemId = ItemDatabase.getItemId( itemName );
			this.setToolTipText( "remove item from store" );
		}

		public void mouseReleased( final MouseEvent e )
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
			if ( items == null || items.length == 0 )
			{
				return;
			}

			RequestThread.openRequestSequence();
			RequestThread.postRequest( new SellStuffRequest( items, SellStuffRequest.AUTOMALL ) );
			RequestThread.postRequest( new ManageStoreRequest( false ) );
			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{
			Object[] items = this.getDesiredItems( "Autosell" );
			RequestThread.postRequest( new SellStuffRequest( items, SellStuffRequest.AUTOSELL ) );
		}
	}

	private class StoreRemovePanel
		extends ItemManagePanel
	{
		public StoreRemovePanel()
		{
			super( "take out", "autosell", StoreManager.getSortedSoldItemList() );
			this.addFilters();

			this.filters[ 4 ].setSelected( false );
			this.filters[ 4 ].setEnabled( false );
		}

		public void actionConfirmed()
		{
			this.removeItems( false );
		}

		public void actionCancelled()
		{
			if ( !KoLFrame.confirm( "Are you sure you'd like to autosell the selected items?" ) )
			{
				return;
			}

			this.removeItems( true );
		}

		public void removeItems( final boolean autoSellAfter )
		{
			Object[] items = this.elementList.getSelectedValues();

			for ( int i = 0; i < items.length; ++i )
			{
				RequestThread.postRequest( new ManageStoreRequest( ( (SoldItem) items[ i ] ).getItemId() ) );
			}

			RequestThread.postRequest( new ManageStoreRequest() );

			if ( autoSellAfter )
			{
				AdventureResult[] itemsToSell = new AdventureResult[ items.length ];
				for ( int i = 0; i < items.length; ++i )
				{
					itemsToSell[ i ] =
						new AdventureResult(
							( (SoldItem) items[ i ] ).getItemId(), ( (SoldItem) items[ i ] ).getQuantity() );
				}

				RequestThread.postRequest( new SellStuffRequest( itemsToSell, SellStuffRequest.AUTOSELL ) );
			}
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
			SimpleScrollPane scrollArea = new SimpleScrollPane( StoreManageFrame.this.resultsDisplay );

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
