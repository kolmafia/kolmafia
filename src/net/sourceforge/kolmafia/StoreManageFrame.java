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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

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
import javax.swing.ListSelectionModel;

// spellcast-related imports
import net.java.dev.spellcast.utilities.PanelList;
import net.java.dev.spellcast.utilities.PanelListCell;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class StoreManageFrame extends KoLFrame
{
	private JLabel searchLabel;
	private JComboBox sellingList;
	private JTextField priceField;
	private JTextField limitField;
	private LockableListModel priceSummary;

	private NonContentPanel storeManager;
	private JPanel searchResults;

	public StoreManageFrame( KoLmafia client )
	{
		super( "Watch Out for Dropkicking Prices", client );
		(new StoreManageRequest( client )).run();

		storeManager = new StoreManagePanel();
		searchResults = new SearchResultsPanel();

		JPanel northPanel = new JPanel();
		northPanel.setLayout( new BorderLayout() );
		northPanel.add( storeManager, BorderLayout.NORTH );

		getContentPane().setLayout( new BorderLayout( 20, 20 ) );
		getContentPane().add( northPanel, BorderLayout.NORTH );
		getContentPane().add( searchResults, BorderLayout.EAST );

		JScrollPane scrollArea = new JScrollPane( new StoreItemPanelList(),
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BorderLayout() );
		centerPanel.add( JComponentUtilities.createLabel( "Manage Store", JLabel.CENTER,
			Color.black, Color.white ), BorderLayout.NORTH );
		centerPanel.add( scrollArea, BorderLayout.CENTER );
		getContentPane().add( centerPanel, BorderLayout.CENTER );
	}

	public void setEnabled( boolean isEnabled )
	{
		sellingList.setEnabled( isEnabled );
		priceField.setEnabled( isEnabled );
		limitField.setEnabled( isEnabled );
		storeManager.setEnabled( isEnabled );
		searchResults.setEnabled( isEnabled );
	}

	private class StoreManagePanel extends NonContentPanel
	{
		public StoreManagePanel()
		{
			super( "add item", "search", new Dimension( 100, 20 ), new Dimension( 420, 20 ) );

			priceSummary = new LockableListModel();
			sellingList = new JComboBox( client.getInventory().getMirrorImage() );
			sellingList.setRenderer( AdventureResult.getAutoSellCellRenderer() );

			if ( client.getInventory().size() > 0 )
				sellingList.setSelectedIndex( 0 );

			priceField = new JTextField();
			limitField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Item to Sell: ", sellingList );
			elements[1] = new VerifiableElement( "Desired Price: ", priceField );
			elements[2] = new VerifiableElement( "Desired Limit: ", limitField );
			setContent( elements, null, null, null, true, true );
		}

		public void actionConfirmed()
		{	(new AutoMallRequestThread()).start();
		}

		public void actionCancelled()
		{
			if ( sellingList.getSelectedItem() == null )
				return;

			(new SearchMallRequestThread( ((AdventureResult) sellingList.getSelectedItem()).getName() )).start();
		}

		private class AutoMallRequestThread extends Thread
		{
			public void run()
			{
				try
				{
					AdventureResult soldItem = (AdventureResult) sellingList.getSelectedItem();
					if ( soldItem == null )
						return;

					int price = priceField.getText() == null ? 0 : priceField.getText().length() == 0 ? 0 :
						df.parse( priceField.getText() ).intValue();

					int limit = limitField.getText() == null ? 0 : limitField.getText().length() == 0 ? 0 :
						df.parse( limitField.getText() ).intValue();

					if ( price > 10 )
						client.makeRequest( new AutoSellRequest( client, soldItem, price, limit ), 1 );

					client.updateDisplay( ENABLED_STATE, "" );
				}
				catch ( Exception e )
				{
				}
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
			setLayout( new BorderLayout() );
			setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
			searchLabel = JComponentUtilities.createLabel( "Mall Prices", JLabel.CENTER,
				Color.black, Color.white );

			JComponentUtilities.setComponentSize( searchLabel, 150, 16 );
			add( searchLabel, BorderLayout.NORTH );

			JList resultsDisplay = new JList( priceSummary );
			resultsDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			JScrollPane scrollArea = new JScrollPane( resultsDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

			JComponentUtilities.setComponentSize( scrollArea, 150, 100 );
			add( scrollArea, BorderLayout.CENTER );
		}
	}

	private class StoreItemPanelList extends PanelList
	{
		public StoreItemPanelList()
		{	super( 8, 440, 25, client.getStoreManager().getSoldItemList() );
		}

		protected synchronized PanelListCell constructPanelListCell( Object value, int index )
		{
			StoreItemPanel toConstruct = new StoreItemPanel( (StoreManager.SoldItem) value );
			toConstruct.updateDisplay( this, value, index );
			return toConstruct;
		}

		private class StoreItemPanel extends PanelListCell
		{
			private int itemID;
			private JLabel itemName;
			private JTextField itemPrice, itemLimit;

			public StoreItemPanel( StoreManager.SoldItem value )
			{
				itemID = value.getItemID();
				itemName = new JLabel( TradeableItemDatabase.getItemName( itemID ), JLabel.RIGHT );
				itemPrice = new JTextField( "" + df.format( value.getPrice() ) );
				itemLimit = new JTextField( "" + df.format( value.getLimit() ) );

				JButton takeButton = new JButton( JComponentUtilities.getSharedImage( "icon_error_sml.gif" ) );
				takeButton.addActionListener( new TakeButtonListener() );

				JButton searchButton = new JButton( JComponentUtilities.getSharedImage( "icon_warning_sml.gif" ) );
				searchButton.addActionListener( new SearchButtonListener() );

				JComponentUtilities.setComponentSize( itemName, 210, 20 );
				JComponentUtilities.setComponentSize( itemPrice, 80, 20 );
				JComponentUtilities.setComponentSize( itemLimit, 40, 20 );
				JComponentUtilities.setComponentSize( takeButton, 30, 20 );
				JComponentUtilities.setComponentSize( searchButton, 30, 20 );

				JPanel corePanel = new JPanel();
				corePanel.setLayout( new BoxLayout( corePanel, BoxLayout.X_AXIS ) );
				corePanel.add( itemName ); corePanel.add( Box.createHorizontalStrut( 10 ) );
				corePanel.add( itemPrice ); corePanel.add( Box.createHorizontalStrut( 10 ) );
				corePanel.add( itemLimit ); corePanel.add( Box.createHorizontalStrut( 10 ) );
				corePanel.add( takeButton ); corePanel.add( Box.createHorizontalStrut( 10 ) );
				corePanel.add( searchButton ); corePanel.add( Box.createHorizontalStrut( 10 ) );

				setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
				add( Box.createVerticalStrut( 5 ) );
				add( corePanel );
			}

			public synchronized void updateDisplay( PanelList list, Object value, int index )
			{
				StoreManager.SoldItem smsi = (StoreManager.SoldItem) value;

				itemName.setText( TradeableItemDatabase.getItemName( smsi.getItemID() ) );
				itemPrice.setText( "" + df.format( smsi.getPrice() ) );
				itemLimit.setText( "" + df.format( smsi.getLimit() ) );
			}

			private class TakeButtonListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	(new TakeFromMallRequestThread()).start();
				}

				private class TakeFromMallRequestThread extends Thread
				{
					public void run()
					{	client.getStoreManager().takeItem( itemID );
					}
				}
			}

			private class SearchButtonListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	(new SearchMallRequestThread( itemName.getText() )).start();
				}
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at
	 * least appearing to freeze), this internal class is used
	 * to actually make the mall search request.
	 */

	private class SearchMallRequestThread extends Thread
	{
		private String itemName;

		public SearchMallRequestThread( String itemName )
		{
			super( "Mall-Search-Request-Thread" );
			setDaemon( true );
			this.itemName = itemName;
		}

		public void run()
		{
			client.getStoreManager().searchMall( itemName, priceSummary );
			searchLabel.setText( itemName );
		}
	}
}