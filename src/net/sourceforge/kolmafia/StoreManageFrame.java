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
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

// spellcast-related imports
import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class StoreManageFrame extends KoLPanelFrame
{
	private JLabel searchLabel;
	private LockableListModel priceSummary;
	private JPanel searchResults;

	public StoreManageFrame()
	{
		super( "Store Manager" );

		if ( StaticEntity.getClient().shouldMakeConflictingRequest() )
		{
			Runnable [] requests = new Runnable [] {
				new StoreManageRequest( StaticEntity.getClient() ),
				new StoreManageRequest( StaticEntity.getClient(), true )
			};
	
			(new RequestThread( requests )).start();
		}

		tabs = new JTabbedPane();
		tabs.add( "Price Setup", new StoreManagePanel() );
		tabs.add( "Additions", new StoreAddPanel() );
		tabs.add( "Removals", new StoreRemovePanel() );
		tabs.add( "Store Log", new StoreLogPanel() );

		framePanel.add( tabs, BorderLayout.CENTER );
	}

	private class StoreLogPanel extends ItemManagePanel
	{
		public StoreLogPanel()
		{	super( "Transactions Log", "refresh", "resort", StoreManager.getStoreLog() );
		}

		public void actionConfirmed()
		{	(new RequestThread( new StoreManageRequest( StaticEntity.getClient(), true ) )).start();
		}

		public void actionCancelled()
		{	StoreManager.sortStoreLog( true );
		}
	}

	private class StoreAddPanel extends ItemManagePanel implements Runnable
	{
		public StoreAddPanel()
		{
			super( "On-Hand Inventory", "add selected", "end of run sale", KoLCharacter.getSellables() );
			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
		}

		public void actionConfirmed()
		{
			Object [] items = getDesiredItems( elementList, "Stock up", TAKE_MULTIPLE );
			if ( items == null || items.length == 0 )
				return;

			Runnable [] requests = new Runnable[2];
			requests[0] = new AutoSellRequest( StaticEntity.getClient(), items, AutoSellRequest.AUTOMALL );
			requests[1] = new StoreManageRequest( StaticEntity.getClient() );
			
			(new RequestThread( requests )).start();
		}

		public void actionCancelled()
		{	(new RequestThread( this )).start();
		}

		public void run()
		{	StaticEntity.getClient().makeEndOfRunSaleRequest();
		}
	}

	private class StoreRemovePanel extends ItemManagePanel implements Runnable
	{
		public StoreRemovePanel()
		{
			super( "Store's Inventory", "remove selected", "empty out store", StoreManager.getSortedSoldItemList() );
			elementList.setCellRenderer( AdventureResult.getAutoSellCellRenderer() );
		}

		public void actionConfirmed()
		{
			Object [] items = elementList.getSelectedValues();
			StoreManageRequest [] requests = new StoreManageRequest[ items.length + 1 ];

			for ( int i = 0; i < items.length; ++i )
			 	requests[i] = new StoreManageRequest( StaticEntity.getClient(), ((StoreManager.SoldItem)items[i]).getItemID() );

			requests[ items.length ] = new StoreManageRequest( StaticEntity.getClient() );
			(new RequestThread( requests )).start();
		}

		public void actionCancelled()
		{	(new RequestThread( this )).start();
		}

		public void run()
		{	StaticEntity.getClient().removeAllItemsFromStore();
		}
	}

	private class StoreManagePanel extends KoLPanel implements Runnable
	{
		private StoreItemPanelList storeItemList;

		public StoreManagePanel()
		{
			super( "save changes", "auto-undercut", true );

			storeItemList = new StoreItemPanelList();
			JScrollPane storeItemScrollArea = new JScrollPane( storeItemList,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( storeItemScrollArea, 600, 300 );

			JPanel headerPanel = new JPanel( new BorderLayout() );
			headerPanel.add( new AddItemPanel(), BorderLayout.WEST );
			headerPanel.add( Box.createVerticalStrut( 10 ), BorderLayout.SOUTH );
			
			JPanel elementsPanel = new JPanel( new BorderLayout() );
			elementsPanel.add( headerPanel, BorderLayout.NORTH );
			elementsPanel.add( storeItemScrollArea );

			searchResults = new SearchResultsPanel();
			setContent( null, null, searchResults, true, true );
			container.add( elementsPanel, BorderLayout.CENTER );
		}

		public void actionConfirmed()
		{
			DEFAULT_SHELL.updateDisplay( "Compiling reprice data..." );

			java.awt.Component [] components = storeItemList.getPanelListCells();
			int [] itemID = new int[ components.length ];
			int [] prices = new int[ components.length ];
			int [] limits = new int[ components.length ];

			StoreItemPanel currentPanel;
			for ( int i = 0; i < components.length; ++i )
			{
				currentPanel = (StoreItemPanel) components[i];
				itemID[i] = currentPanel.getItemID();
				prices[i] = currentPanel.getPrice();
				limits[i] = 0;
			}

			(new RequestThread( new StoreManageRequest( StaticEntity.getClient(), itemID, prices, limits ) )).start();
		}

		public void actionCancelled()
		{	(new RequestThread( this )).start();
		}

		public void run()
		{	StaticEntity.getClient().priceItemsAtLowestPrice();
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

			JList resultsDisplay = new JList( priceSummary = new LockableListModel() );
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

	private class AddItemPanel extends JPanel
	{
		private JLabel lowestPrice;
		private JComboBox sellingList;
		private JTextField itemPrice, itemQty;
		private JButton addButton, searchButton;

		public AddItemPanel()
		{
			sellingList = new JComboBox( KoLCharacter.getSellables() );
			sellingList.setRenderer( AdventureResult.getAutoSellCellRenderer() );

			itemPrice = new JTextField( "" );
			itemQty = new JTextField( "" );
			lowestPrice = new JLabel( "(unknown)", JLabel.CENTER );

			addButton = new JButton( JComponentUtilities.getImage( "icon_success_sml.gif" ) );
			addButton.addActionListener( new AddButtonListener() );
			addButton.setToolTipText( "Add to Store" );

			searchButton = new JButton( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			searchButton.addActionListener( new SearchButtonListener() );
			searchButton.setToolTipText( "Price Analysis" );

			JComponentUtilities.setComponentSize( sellingList, 240, 20 );
			JComponentUtilities.setComponentSize( itemPrice, 90, 20 );
			JComponentUtilities.setComponentSize( lowestPrice, 90, 20 );
			JComponentUtilities.setComponentSize( itemQty, 50, 20 );
			JComponentUtilities.setComponentSize( addButton, 30, 20 );
			JComponentUtilities.setComponentSize( searchButton, 30, 20 );

			JPanel corePanel = new JPanel();
			corePanel.setLayout( new BoxLayout( corePanel, BoxLayout.X_AXIS ) );
			corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( sellingList ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( itemPrice ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( lowestPrice ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( itemQty ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( addButton ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( searchButton ); corePanel.add( Box.createHorizontalStrut( 30 ) );

			JLabel [] label = new JLabel[5];
			label[0] = new JLabel( "Item Name", JLabel.CENTER );  JComponentUtilities.setComponentSize( label[0], 240, 20 );
			label[1] = new JLabel( "Price", JLabel.CENTER );  JComponentUtilities.setComponentSize( label[1], 90, 20 );
			label[2] = new JLabel( "Lowest", JLabel.CENTER );  JComponentUtilities.setComponentSize( label[2], 90, 20 );
			label[3] = new JLabel( "Qty", JLabel.CENTER );  JComponentUtilities.setComponentSize( label[3], 50, 20 );
			label[4] = new JLabel( "Action", JLabel.CENTER );  JComponentUtilities.setComponentSize( label[4], 70, 20 );

			JPanel labelPanel = new JPanel();
			labelPanel.setLayout( new BoxLayout( labelPanel, BoxLayout.X_AXIS ) );
			labelPanel.add( Box.createHorizontalStrut( 10 ) );
			for ( int i = 0; i < label.length; ++i )
			{
				labelPanel.add( label[i] );
				labelPanel.add( Box.createHorizontalStrut( 10 ) );
			}

			labelPanel.add( Box.createHorizontalStrut( 20 ) );

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( Box.createVerticalStrut( 5 ) );
			add( labelPanel );
			add( Box.createVerticalStrut( 5 ) );
			add( corePanel );
		}

		public void setEnabled( boolean isEnabled )
		{
			sellingList.setEnabled( isEnabled );
			itemPrice.setEnabled( isEnabled );
			itemQty.setEnabled( isEnabled );
			addButton.setEnabled( isEnabled );
			searchButton.setEnabled( isEnabled );
		}

		private class AddButtonListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( !KoLCharacter.hasStore() )
				{
					JOptionPane.showMessageDialog( null, "Sorry, you don't have a store." );
					return;
				}

				AdventureResult soldItem = (AdventureResult) sellingList.getSelectedItem();
				if ( soldItem == null )
					return;

				int price = getValue( itemPrice, StoreManager.getPrice( soldItem.getItemID() ) );
				int limit = 0;
				int qty = getValue( itemQty, soldItem.getCount() );

				soldItem = new AdventureResult( soldItem.getItemID(), qty );

				if ( price > 10 )
					(new RequestThread( new AutoSellRequest( StaticEntity.getClient(), soldItem, price, limit ) )).start();
			}
		}

		private class SearchButtonListener extends ListeningRunnable
		{
			public void run()
			{
				if ( sellingList.getSelectedItem() == null )
					return;

				StoreManager.searchMall( ((AdventureResult)sellingList.getSelectedItem()).getName(), priceSummary, 10, true );
				searchLabel.setText( ((AdventureResult)sellingList.getSelectedItem()).getName() );
			}
		}
	}	

	private class StoreItemPanelList extends PanelList
	{
		public StoreItemPanelList()
		{	super( 16, 600, 24, StoreManager.getSoldItemList() );
		}

		protected PanelListCell constructPanelListCell( Object value, int index )
		{
			StoreItemPanel toConstruct = new StoreItemPanel( (StoreManager.SoldItem) value );
			toConstruct.updateDisplay( this, value, index );
			return toConstruct;
		}
	}

	private class StoreItemPanel extends JPanel implements PanelListCell
	{
		private int itemID;
		private JLabel itemName, itemQuantity, lowestPrice;
		private JTextField itemPrice;
		private JButton takeButton, searchButton;

		public StoreItemPanel( StoreManager.SoldItem value )
		{
			itemID = value.getItemID();
			itemName = new JLabel( TradeableItemDatabase.getItemName( itemID ), JLabel.RIGHT );
			itemQuantity = new JLabel( df.format( value.getQuantity() ), JLabel.CENTER );
			itemPrice = new JTextField( df.format( value.getPrice() ) );
			lowestPrice = new JLabel( value.getLowest() == 0 ? "(unknown)" : df.format( value.getLowest() ), JLabel.CENTER );

			takeButton = new JButton( JComponentUtilities.getImage( "icon_error_sml.gif" ) );
			takeButton.addActionListener( new TakeButtonListener() );
			takeButton.setToolTipText( "Remove Items" );

			searchButton = new JButton( JComponentUtilities.getImage( "icon_warning_sml.gif" ) );
			searchButton.addActionListener( new SearchButtonListener() );
			searchButton.setToolTipText( "Price Analysis" );

			JComponentUtilities.setComponentSize( itemName, 240, 20 );
			JComponentUtilities.setComponentSize( itemPrice, 90, 20 );
			JComponentUtilities.setComponentSize( lowestPrice, 90, 20 );
			JComponentUtilities.setComponentSize( itemQuantity, 50, 20 );
			JComponentUtilities.setComponentSize( takeButton, 30, 20 );
			JComponentUtilities.setComponentSize( searchButton, 30, 20 );

			JPanel corePanel = new JPanel();
			corePanel.setLayout( new BoxLayout( corePanel, BoxLayout.X_AXIS ) );
			corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( itemName ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( itemPrice ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( lowestPrice ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( itemQuantity ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( takeButton ); corePanel.add( Box.createHorizontalStrut( 10 ) );
			corePanel.add( searchButton ); corePanel.add( Box.createHorizontalStrut( 10 ) );

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( Box.createVerticalStrut( 5 ) );
			add( corePanel );
		}

		public void updateDisplay( PanelList list, Object value, int index )
		{
			StoreManager.SoldItem smsi = (StoreManager.SoldItem) value;

			itemName.setText( smsi.getItemName() );
			itemName.setToolTipText( smsi.getItemName() );
			itemQuantity.setText( df.format( smsi.getQuantity() ) );
			itemPrice.setText( df.format( smsi.getPrice() ) );
			lowestPrice.setText( smsi.getLowest() == 0 ? "**" : df.format( smsi.getLowest() ) );
		}

		public void setEnabled( boolean isEnabled )
		{
			itemPrice.setEnabled( isEnabled );
			takeButton.setEnabled( isEnabled );
			searchButton.setEnabled( isEnabled );
		}

		public int getItemID()
		{	return itemID;
		}

		public int getPrice()
		{
			try
			{
				return itemPrice.getText() == null || itemPrice.getText().length() == 0 ? 0 :
					df.parse( itemPrice.getText() ).intValue();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
				return 0;
			}
		}

		private class TakeButtonListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	(new RequestThread( new StoreManageRequest( StaticEntity.getClient(), itemID ) )).start();
			}
		}

		private class SearchButtonListener extends ListeningRunnable
		{
			public void run()
			{
				StoreManager.searchMall( TradeableItemDatabase.getItemName( itemID ), priceSummary, 100, true );
				searchLabel.setText( itemName.getText() );
				searchLabel.setToolTipText( itemName.getText() );
			}
		}
	}
}
