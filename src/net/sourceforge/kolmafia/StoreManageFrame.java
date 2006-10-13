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

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;

import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.table.TableCellRenderer;
import com.sun.java.forums.TableSorter;

// spellcast-related imports
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class StoreManageFrame extends KoLPanelFrame
{
	private JLabel searchLabel;
	private LockableListModel priceSummary;
	private JPanel searchResults;
	private JTable addTable, manageTable;
	private JList resultsDisplay;

	public StoreManageFrame()
	{
		super( "Store Manager" );

		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		tabs.add( "Price Setup", new StoreManagePanel() );
		tabs.add( "Additions", new StoreAddPanel() );
		tabs.add( "Removals", new StoreRemovePanel() );
		tabs.add( "Store Log", new StoreLogPanel() );

		framePanel.add( tabs, BorderLayout.CENTER );
	}

	private class StoreManagePanel extends KoLPanel implements Runnable
	{
		public StoreManagePanel()
		{
			super( "save changes", "auto-undercut", true );

			addTable = new StoreListTable( new LockableListModel() );
			JScrollPane addScroller = new JScrollPane( addTable,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( addScroller, 500, 50 );
			JPanel addPanel = new JPanel( new BorderLayout() );

			addPanel.add( addTable.getTableHeader(), BorderLayout.NORTH );
			addPanel.add( addScroller, BorderLayout.CENTER );

			manageTable = new StoreListTable( StoreManager.getSoldItemList() );
			JScrollPane manageScroller = new JScrollPane( manageTable,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JPanel managePanel = new JPanel( new BorderLayout() );
			managePanel.add( manageTable.getTableHeader(), BorderLayout.NORTH );
			managePanel.add( manageScroller, BorderLayout.CENTER );

			JPanel storePanel = new JPanel( new BorderLayout() );
			storePanel.add( addPanel, BorderLayout.NORTH );
			storePanel.add( managePanel, BorderLayout.CENTER );

			searchResults = new SearchResultsPanel();
			setContent( null, null, searchResults, true, true );
			container.add( storePanel, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{
			if ( !finalizeTable( manageTable ) )
				return;

			KoLmafia.updateDisplay( "Compiling reprice data..." );
			int rowCount = manageTable.getRowCount();

			int [] itemID = new int[ rowCount ];
			int [] prices = new int[ rowCount ];
			int [] limits = new int[ rowCount ];

			for ( int i = 0; i < rowCount; ++i )
			{
				itemID[i] = ((AdventureResult)manageTable.getValueAt( i, 0 )).getItemID();
				prices[i] = ((Integer) manageTable.getValueAt( i, 1 )).intValue();
				limits[i] = ((Boolean)manageTable.getValueAt( i, 4 )).booleanValue() ? 1 : 0;
			}

			(new RequestThread( new StoreManageRequest( itemID, prices, limits ) )).start();
		}

		public void actionCancelled()
		{	(new RequestThread( this )).start();
		}

		public void run()
		{	StaticEntity.getClient().priceItemsAtLowestPrice();
		}
	}

	private class StoreListTable extends TransparentTable
	{
		public StoreListTable( LockableListModel model )
		{
			super( new StoreManageTableModel( model ) );

			if ( model != StoreManager.getSoldItemList() )
			{
				getColumnModel().getColumn(0).setCellEditor(
					new DefaultCellEditor( ((StoreManageTableModel)getModel()).getSellingList() ) );
			}
			else
			{
				setModel( new TableSorter( getModel(), getTableHeader() ) );
			}

			getTableHeader().setReorderingAllowed( false );

			setRowSelectionAllowed( false );

			addMouseListener( new ButtonEventListener( this ) );
			setDefaultRenderer( Integer.class, new IntegerRenderer() );
			setDefaultRenderer( JButton.class, new ButtonRenderer() );

			setOpaque( false );
			setShowGrid( false );

			setRowHeight( 28 );

			getColumnModel().getColumn(0).setMinWidth( 200 );

			getColumnModel().getColumn(4).setMinWidth( 35 );
			getColumnModel().getColumn(4).setMaxWidth( 35 );

			getColumnModel().getColumn(5).setMinWidth( 40 );
			getColumnModel().getColumn(5).setMaxWidth( 40 );

			getColumnModel().getColumn(6).setMinWidth( 40 );
			getColumnModel().getColumn(6).setMaxWidth( 40 );
		}
    }

	private class StoreManageTableModel extends ListWrapperTableModel
	{
		private JComboBox sellingList = null;

		public StoreManageTableModel( LockableListModel model )
		{
			super( new String [] { "Item Name", "Price", model == StoreManager.getSoldItemList() ? "Lowest" : " ", "Qty", "Lim", " ", " " },
					new Class [] { AdventureResult.class, Integer.class, Integer.class, Integer.class, Boolean.class, JButton.class, JButton.class },
					new boolean [] { model != StoreManager.getSoldItemList(), true, false, model != StoreManager.getSoldItemList(), true, false, false }, model );

			if ( model != StoreManager.getSoldItemList() )
			{
				sellingList = new JComboBox( sellables );

				Vector value = new Vector();
				value.add( new AdventureResult( "- select an item -", 1, false ) );
				value.add( new Integer(0) );
				value.add( new Integer(0) );
				value.add( new Integer(0) );
				value.add( new Boolean( false ) );
				model.add( value );
			}
		}

		public JComboBox getSellingList()
		{	return sellingList;
		}

		protected Vector constructVector( Object o )
		{
			Vector value = (Vector) o;
			if ( value.size() < 7 )
			{
				if ( sellingList == null )
				{
					value.add( new RemoveItemButton( ((AdventureResult) value.get(0)).getName() ) );
					value.add( new SearchItemButton( ((AdventureResult) value.get(0)).getName() ) );
				}
				else
				{
					value.add( new AddItemButton() );
					value.add( new SearchItemButton() );
				}
			}

			return value;
		}

		private class AddItemButton extends NestedInsideTableButton
		{
			public AddItemButton()
			{
				super( JComponentUtilities.getImage( "icon_success_sml.gif" ) );
				setToolTipText( "add selected item" );
			}

			public void mouseReleased( MouseEvent e )
			{
				if ( !finalizeTable( addTable ) )
					return;

				AdventureResult soldItem = (AdventureResult) sellingList.getSelectedItem();
				if ( soldItem == null )
					return;

				int price = ((Integer) getValueAt( 0, 1 )).intValue();
				int quantity = ((Integer) getValueAt( 0, 3 )).intValue();

				if ( quantity <= 0 )
					quantity = soldItem.getCount() - quantity;

				int limit = ((Boolean) getValueAt( 0, 4 )).booleanValue() ? 1 : 0;
				soldItem = new AdventureResult( soldItem.getItemID(), quantity );

				setValueAt( new AdventureResult( "-select an item-", 1, false ), 0, 0 );
				(new RequestThread( new AutoSellRequest( soldItem, price, limit ) )).start();
			}
		}

		private class SearchItemButton extends NestedInsideTableButton implements Runnable
		{
			private String itemName;

			public SearchItemButton()
			{	this( null );
			}

			public SearchItemButton( String itemName )
			{
				super( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
				this.itemName = itemName;
				setToolTipText( "price analysis" );
			}

			public void mouseReleased( MouseEvent e )
			{	(new RequestThread( this )).start();
			}

			public void run()
			{
				String searchName = itemName;
				if ( searchName == null )
				{
					AdventureResult item = (AdventureResult) sellingList.getSelectedItem();
					if ( item == null )
						return;

					searchName = item.getName();
				}

				StoreManager.searchMall( searchName, priceSummary, 10, true );
				searchLabel.setText( searchName );
				resultsDisplay.updateUI();

				KoLmafia.updateDisplay( "Price analysis complete." );
			}
		}

		private class RemoveItemButton extends NestedInsideTableButton
		{
			private int itemID;

			public RemoveItemButton( String itemName )
			{
				super( JComponentUtilities.getImage( "icon_error_sml.gif" ) );
				this.itemID = TradeableItemDatabase.getItemID( itemName );
				setToolTipText( "remove item from store" );
			}

			public void mouseReleased( MouseEvent e )
			{	(new RequestThread( new StoreManageRequest( itemID ) )).start();
			}
		}
	}

	private class StoreAddPanel extends ItemManagePanel implements Runnable
	{
		public StoreAddPanel()
		{
			super( "On-Hand Inventory", "add selected", "end of run sale", sellables );
			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
		}

		public void actionConfirmed()
		{
			Object [] items = getDesiredItems( elementList, "Stock up", TAKE_MULTIPLE );
			if ( items == null || items.length == 0 )
				return;

			Runnable [] requests = new Runnable[2];
			requests[0] = new AutoSellRequest( items, AutoSellRequest.AUTOMALL );
			requests[1] = new StoreManageRequest();

			(new RequestThread( requests )).start();
		}

		public void actionCancelled()
		{	(new RequestThread( this )).start();
		}

		public void run()
		{	StaticEntity.getClient().makeEndOfRunSaleRequest();
		}
	}

	private class StoreRemovePanel extends ItemManagePanel
	{
		public StoreRemovePanel()
		{
			super( "Store's Inventory", "remove items", "autosell items", StoreManager.getSortedSoldItemList() );
			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
		}

		public void actionConfirmed()
		{	removeItems( false );
		}

		public void actionCancelled()
		{
			if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
				"Are you sure you'd like to autosell the selected items?", "Clean up!", JOptionPane.YES_NO_OPTION ) )
					return;

			removeItems( true );
		}

		public void removeItems( boolean autoSellAfter )
		{
			Object [] items = elementList.getSelectedValues();
			Runnable [] requests = new Runnable[ autoSellAfter ? items.length + 2 : items.length + 1 ];

			for ( int i = 0; i < items.length; ++i )
			 	requests[i] = new StoreManageRequest( ((StoreManager.SoldItem)items[i]).getItemID() );

			requests[ items.length ] = new StoreManageRequest();

			if ( autoSellAfter )
			{
				AdventureResult [] itemsToSell = new AdventureResult[ items.length ];
				for ( int i = 0; i < items.length; ++i )
					itemsToSell[i] = new AdventureResult( ((StoreManager.SoldItem)items[i]).getItemID(), ((StoreManager.SoldItem)items[i]).getQuantity() );

				requests[ items.length + 1 ] = new AutoSellRequest( itemsToSell, AutoSellRequest.AUTOSELL );
			}

			(new RequestThread( requests )).start();
		}
	}

	/**
	 * An internal class which represents the panel used for tallying the
	 * results of the mall search request.  Note that all of the tallying
	 * functionality is handled by the <code>LockableListModel</code>
	 * provided, so this functions as a container for that list model.
	 */

	private class SearchResultsPanel extends JPanel
	{
		public SearchResultsPanel()
		{
			super( new BorderLayout() );

			JPanel container = new JPanel( new BorderLayout() );
			container.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
			searchLabel = JComponentUtilities.createLabel( "Mall Prices", JLabel.CENTER,
				Color.black, Color.white );

			container.add( searchLabel, BorderLayout.NORTH );
			JComponentUtilities.setComponentSize( searchLabel, 150, 16 );

			resultsDisplay = new JList( priceSummary = new LockableListModel() );
			resultsDisplay.setPrototypeCellValue( "1234567890ABCDEF" );
			resultsDisplay.setVisibleRowCount( 11 );
			resultsDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			JScrollPane scrollArea = new JScrollPane( resultsDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			container.add( scrollArea, BorderLayout.CENTER );
			add( Box.createVerticalStrut( 20 ), BorderLayout.NORTH );
			add( container, BorderLayout.CENTER );
		}
	}

	private class StoreLogPanel extends ItemManagePanel
	{
		public StoreLogPanel()
		{	super( "Transactions Log", "refresh", "resort", StoreManager.getStoreLog() );
		}

		public void actionConfirmed()
		{	(new RequestThread( new StoreManageRequest( true ) )).start();
		}

		public void actionCancelled()
		{	StoreManager.sortStoreLog( true );
		}
	}
}
