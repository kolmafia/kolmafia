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
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import com.sun.java.forums.TableSorter;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;

import net.sourceforge.kolmafia.KoLFrame.OverlapPanel;
import net.sourceforge.kolmafia.StoreManager.SoldItem;

public class StoreManageFrame extends KoLPanelFrame
{
	private static StoreManageFrame INSTANCE = null;
	private static final JLabel searchLabel = JComponentUtilities.createLabel(
		"Mall Prices", JLabel.CENTER, Color.black, Color.white );
	private static final LockableListModel priceSummary = new LockableListModel();

	private JComboBox sellingList;
	private JTable addTable, manageTable;
	private JList resultsDisplay;

	public StoreManageFrame()
	{
		super( "Store Manager" );
		INSTANCE = this;

		this.tabs.add( "Price Setup", new StoreManagePanel() );
		this.tabs.add( "Additions", new StoreAddPanel() );
		this.tabs.add( "Restocker", new ProfitableItemsPanel() );
		this.tabs.add( "End of Run", new EndOfRunSalePanel() );
		this.tabs.add( "Removals", new StoreRemovePanel() );
		this.tabs.add( "Store Log", new StoreLogPanel() );

		this.framePanel.add( this.tabs, BorderLayout.CENTER );
		updateEarnings( StoreManager.getPotentialEarnings() );
	}

	public static final void updateEarnings( long potentialEarnings )
	{
		if ( INSTANCE == null || StaticEntity.getGlobalProperty( "initialDesktop" ).indexOf( "StoreManageFrame" ) != -1 )
			return;

		INSTANCE.setTitle( "Store Manager (potential earnings: " + COMMA_FORMAT.format( potentialEarnings ) + " meat)" );
	}

	public void dispose()
	{
		INSTANCE = null;
		super.dispose();
	}

	private class StoreManagePanel extends KoLPanel
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

			this.setContent( elements, true );
			this.eastContainer.add( searchResults, BorderLayout.CENTER );
			this.container.add( storePanel, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{
			if ( !StoreManageFrame.this.finalizeTable( StoreManageFrame.this.manageTable ) )
				return;

			KoLmafia.updateDisplay( "Compiling reprice data..." );
			int rowCount = StoreManageFrame.this.manageTable.getRowCount();

			int [] itemId = new int[ rowCount ];
			int [] prices = new int[ rowCount ];
			int [] limits = new int[ rowCount ];

			SoldItem [] sold = new SoldItem[ StoreManager.getSoldItemList().size() ];
			StoreManager.getSoldItemList().toArray( sold );

			for ( int i = 0; i < rowCount; ++i )
			{
				itemId[i] = TradeableItemDatabase.getItemId( (String) StoreManageFrame.this.manageTable.getValueAt( i, 0 ) );
				prices[i] = ((Integer) StoreManageFrame.this.manageTable.getValueAt( i, 1 )).intValue();

				int oldLimit = 0;

				for ( int j = 0; j < sold.length; ++j )
				{
					if ( sold[j].getItemId() == itemId[i] )
					{
						oldLimit = sold[j].getLimit();
						break;
					}
				}

				limits[i] = ((Boolean) StoreManageFrame.this.manageTable.getValueAt( i, 4 )).booleanValue() ? Math.max( 1, oldLimit ) : 0;
			}

			RequestThread.postRequest( new StoreManageRequest( itemId, prices, limits ) );
		}

		public void actionCancelled()
		{
			if ( !confirm( UNDERCUT_MESSAGE + "Are you sure you wish to continue with this repricing?" ) )
				return;

			StaticEntity.getClient().priceItemsAtLowestPrice();
		}
	}

	public static final String UNDERCUT_MESSAGE =
		"KoLmafia will take items priced at 999,999,999 meat and attempt to reprice them. " +
		"In this attempt, it will match or undercut the current lowest price in the mall. " +
		"As a warning, if someone holds an \"anti-raffle,\" KoLmafia will price based on that price.  ";

	private class StoreListTable extends TransparentTable
	{
		public StoreListTable( LockableListModel model )
		{
			super( model == null ? (ListWrapperTableModel) new StoreAddTableModel() : new StoreManageTableModel() );

			if ( model == null )
				this.getColumnModel().getColumn(0).setCellEditor( new DefaultCellEditor( StoreManageFrame.this.sellingList ) );
			else
				this.setModel( new TableSorter( this.getModel(), this.getTableHeader() ) );

			this.getTableHeader().setReorderingAllowed( false );

			this.setRowSelectionAllowed( false );

			this.addMouseListener( new ButtonEventListener( this ) );
			this.setDefaultRenderer( Integer.class, new IntegerRenderer() );
			this.setDefaultRenderer( JButton.class, new ButtonRenderer() );

			this.setOpaque( false );
			this.setShowGrid( false );

			this.setRowHeight( 28 );

			this.getColumnModel().getColumn(0).setMinWidth( 200 );

			this.getColumnModel().getColumn(4).setMinWidth( 35 );
			this.getColumnModel().getColumn(4).setMaxWidth( 35 );

			this.getColumnModel().getColumn(5).setMinWidth( 40 );
			this.getColumnModel().getColumn(5).setMaxWidth( 40 );

			this.getColumnModel().getColumn(6).setMinWidth( 40 );
			this.getColumnModel().getColumn(6).setMaxWidth( 40 );
		}
    }

	private class StoreAddTableModel extends ListWrapperTableModel
	{
		public StoreAddTableModel()
		{
			super( new String [] { "Item Name", "Price", " ", "Qty", "Lim", " ", " " },
					new Class [] { String.class, Integer.class, Integer.class, Integer.class, Boolean.class, JButton.class, JButton.class },
					new boolean [] { true, true, false, true, true, false, false }, new LockableListModel() );

			LockableListModel dataModel = inventory.getMirrorImage( new TradeableItemFilter() );
			StoreManageFrame.this.sellingList = new JComboBox( dataModel );

			Vector value = new Vector();
			value.add( "- select an item -" );
			value.add( new Integer(0) );
			value.add( new Integer(0) );
			value.add( new Integer(0) );
			value.add( Boolean.FALSE );

			this.listModel.add( value );
		}

		public Vector constructVector( Object o )
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

	private class StoreManageTableModel extends ListWrapperTableModel
	{
		public StoreManageTableModel()
		{
			super( new String [] { "Item Name", "Price", "Lowest", "Qty", "Lim", " ", " " },
					new Class [] { String.class, Integer.class, Integer.class, Integer.class, Boolean.class, JButton.class, JButton.class },
					new boolean [] { false, true, false, false, true, false, false }, StoreManager.getSoldItemList() );
		}

		public Vector constructVector( Object o )
		{
			Vector value = (Vector) o;
			if ( value.size() < 7 )
			{
				value.add( new RemoveItemButton( (String) value.get(0) ) );
				value.add( new SearchItemButton( (String) value.get(0) ) );
			}

			return value;
		}
	}

	private class AddItemButton extends NestedInsideTableButton
	{
		public AddItemButton()
		{
			super( JComponentUtilities.getImage( "icon_success_sml.gif" ) );
			this.setToolTipText( "add selected item" );
		}

		public void mouseReleased( MouseEvent e )
		{
			if ( !StoreManageFrame.this.finalizeTable( StoreManageFrame.this.addTable ) )
				return;

			AdventureResult soldItem = (AdventureResult) StoreManageFrame.this.sellingList.getSelectedItem();
			if ( soldItem == null )
				return;

			int price = ((Integer) StoreManageFrame.this.addTable.getValueAt( 0, 1 )).intValue();
			int quantity = ((Integer) StoreManageFrame.this.addTable.getValueAt( 0, 3 )).intValue();

			if ( quantity <= 0 )
				quantity = soldItem.getCount() - quantity;

			int limit = ((Boolean) StoreManageFrame.this.addTable.getValueAt( 0, 4 )).booleanValue() ? 1 : 0;
			soldItem = new AdventureResult( soldItem.getItemId(), quantity );

			StoreManageFrame.this.addTable.setValueAt( new AdventureResult( "-select an item-", 1, false ), 0, 0 );
			StoreManageFrame.this.addTable.setValueAt( new Integer(0), 0, 1 );
			StoreManageFrame.this.addTable.setValueAt( new Integer(0), 0, 3 );

			RequestThread.postRequest( new AutoSellRequest( soldItem, price, limit ) );
		}
	}

	private class SearchItemButton extends NestedInsideTableButton
	{
		private String itemName;

		public SearchItemButton()
		{	this( null );
		}

		public SearchItemButton( String itemName )
		{
			super( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			this.itemName = itemName;
			this.setToolTipText( "price analysis" );
		}

		public void mouseReleased( MouseEvent e )
		{
			String searchName = this.itemName;
			if ( searchName == null )
			{
				AdventureResult item = (AdventureResult) StoreManageFrame.this.sellingList.getSelectedItem();
				if ( item == null )
					return;

				searchName = item.getName();
			}

			searchLabel.setText( searchName );
			StoreManager.searchMall( searchName, priceSummary, 10, true );

			StoreManageFrame.this.resultsDisplay.updateUI();

			KoLmafia.updateDisplay( "Price analysis complete." );
			RequestThread.enableDisplayIfSequenceComplete();
		}
	}

	private class RemoveItemButton extends NestedInsideTableButton
	{
		private int itemId;

		public RemoveItemButton( String itemName )
		{
			super( JComponentUtilities.getImage( "icon_error_sml.gif" ) );
			this.itemId = TradeableItemDatabase.getItemId( itemName );
			this.setToolTipText( "remove item from store" );
		}

		public void mouseReleased( MouseEvent e )
		{	RequestThread.postRequest( new StoreManageRequest( this.itemId ) );
		}
	}

	private class StoreAddPanel extends ItemManagePanel
	{
		public StoreAddPanel()
		{
			super( "put in", "auto sell", inventory );
			this.addFilters();

			this.filters[4].setSelected( false );
			this.filters[4].setEnabled( false );
			this.filterItems();
		}

		public void actionConfirmed()
		{
			Object [] items = this.getDesiredItems( "Automall" );
			if ( items == null || items.length == 0 )
				return;

			RequestThread.openRequestSequence();
			RequestThread.postRequest( new AutoSellRequest( items, AutoSellRequest.AUTOMALL ) );
			RequestThread.postRequest( new StoreManageRequest( false ) );
			RequestThread.closeRequestSequence();
		}

		public void actionCancelled()
		{
			Object [] items = this.getDesiredItems( "Autosell" );
			RequestThread.postRequest( new AutoSellRequest( items, AutoSellRequest.AUTOSELL ) );
		}
	}

	private class ProfitableItemsPanel extends OverlapPanel
	{
		public ProfitableItemsPanel()
		{
			super( "automall", "help", profitableList, true );

			this.filters[4].setSelected( false );
			this.filters[4].setEnabled( false );
			this.filterItems();
		}

		public void actionConfirmed()
		{	StaticEntity.getClient().makeAutoMallRequest();
		}

		public void actionCancelled()
		{	alert( "These items have been flagged as \"profitable\" because at some point in the past, you've opted to place them in the mall.  If you use the \"automall\" command, KoLmafia will place all of these items in the mall." );
		}
	}

	private class EndOfRunSalePanel extends OverlapPanel
	{
		public EndOfRunSalePanel()
		{
			super( "host sale", "help", mementoList, false );

			this.filters[4].setSelected( false );
			this.filters[4].setEnabled( false );
			this.filterItems();
		}

		public void actionConfirmed()
		{
			KoLmafia.updateDisplay( "Gathering data..." );
			StaticEntity.getClient().makeEndOfRunSaleRequest();
		}

		public void actionCancelled()
		{	alert( "KoLmafia will place all items which are not already in your store into your store. " + StoreManageFrame.UNDERCUT_MESSAGE );
		}
	}

	private class StoreRemovePanel extends ItemManagePanel
	{
		public StoreRemovePanel()
		{
			super( "take out", "auto sell", StoreManager.getSortedSoldItemList() );
			this.addFilters();

			this.filters[4].setSelected( false );
			this.filters[4].setEnabled( false );
		}

		public void actionConfirmed()
		{	this.removeItems( false );
		}

		public void actionCancelled()
		{
			if ( !confirm( "Are you sure you'd like to autosell the selected items?" ) )
					return;

			this.removeItems( true );
		}

		public void removeItems( boolean autoSellAfter )
		{
			Object [] items = this.elementList.getSelectedValues();

			for ( int i = 0; i < items.length; ++i )
			 	RequestThread.postRequest( new StoreManageRequest( ((SoldItem)items[i]).getItemId() ) );

			RequestThread.postRequest( new StoreManageRequest() );

			if ( autoSellAfter )
			{
				AdventureResult [] itemsToSell = new AdventureResult[ items.length ];
				for ( int i = 0; i < items.length; ++i )
					itemsToSell[i] = new AdventureResult( ((SoldItem)items[i]).getItemId(), ((SoldItem)items[i]).getQuantity() );

				RequestThread.postRequest( new AutoSellRequest( itemsToSell, AutoSellRequest.AUTOSELL ) );
			}
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

			container.add( searchLabel, BorderLayout.NORTH );
			JComponentUtilities.setComponentSize( searchLabel, 150, 16 );

			StoreManageFrame.this.resultsDisplay = new JList( priceSummary );
			StoreManageFrame.this.resultsDisplay.setPrototypeCellValue( "1234567890ABCDEF" );
			StoreManageFrame.this.resultsDisplay.setVisibleRowCount( 11 );
			StoreManageFrame.this.resultsDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			SimpleScrollPane scrollArea = new SimpleScrollPane( StoreManageFrame.this.resultsDisplay );

			container.add( scrollArea, BorderLayout.CENTER );
			this.add( Box.createVerticalStrut( 20 ), BorderLayout.NORTH );
			this.add( container, BorderLayout.CENTER );
		}
	}

	private class StoreLogPanel extends ItemManagePanel
	{
		public StoreLogPanel()
		{	super( "refresh", "resort", StoreManager.getStoreLog() );
		}

		public void actionConfirmed()
		{	RequestThread.postRequest( new StoreManageRequest( true ) );
		}

		public void actionCancelled()
		{	StoreManager.sortStoreLog( true );
		}
	}

	private class TradeableItemFilter extends ListElementFilter
	{
		public boolean isVisible( Object element )
		{
			if ( !(element instanceof AdventureResult) )
				return false;

			int itemId = ((AdventureResult)element).getItemId();
			return itemId < 1 || TradeableItemDatabase.isTradeable( itemId );
		}
	}
}
