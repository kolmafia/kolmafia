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

import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.ListSelectionModel;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;
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

		JPanel westPanel = new JPanel();
		westPanel.setLayout( new BorderLayout() );
		westPanel.add( storeManager, BorderLayout.NORTH );

		getContentPane().setLayout( new BorderLayout() );
		getContentPane().add( westPanel, BorderLayout.NORTH );
		getContentPane().add( searchResults, BorderLayout.EAST );
	}

	public void setEnabled( boolean isEnabled )
	{
		sellingList.setEnabled( isEnabled );
		priceField.setEnabled( isEnabled );
		storeManager.setEnabled( isEnabled );
		searchResults.setEnabled( isEnabled );
	}

	private class StoreManagePanel extends NonContentPanel
	{
		public StoreManagePanel()
		{
			super( "add item", "search", new Dimension( 100, 20 ), new Dimension( 360, 20 ) );

			priceSummary = new LockableListModel();
			sellingList = new JComboBox( client.getInventory().getMirrorImage() );
			sellingList.setRenderer( AdventureResult.getAutoSellCellRenderer() );

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
		{	(new SearchMallRequestThread()).start();
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

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually make the mall search request.
		 */

		private class SearchMallRequestThread extends Thread
		{
			public SearchMallRequestThread()
			{
				super( "Mall-Search-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				AdventureResult searchItem = (AdventureResult) sellingList.getSelectedItem();
				if ( searchItem == null )
					return;

				String itemName = searchItem.getName();
				ArrayList results = new ArrayList();
				(new SearchMallRequest( client, "\'\'" + itemName + "\'\'", 0, results )).run();

				TreeMap prices = new TreeMap();
				MallPurchaseRequest currentItem;
				Integer currentQuantity, currentPrice;

				Iterator i = results.iterator();
				while ( i.hasNext() )
				{
					currentItem = (MallPurchaseRequest) i.next();
					currentPrice = new Integer( currentItem.getPrice() );

					currentQuantity = (Integer) prices.get( currentPrice );
					if ( currentQuantity == null )
						prices.put( currentPrice, new Integer( currentItem.getQuantity() ) );
					else
						prices.put( currentPrice, new Integer( currentQuantity.intValue() + currentItem.getQuantity() ) );
				}

				priceSummary.clear();
				i = prices.keySet().iterator();

				while ( i.hasNext() )
				{
					currentPrice = (Integer) i.next();
					priceSummary.add( "  " + df.format( ((Integer)prices.get( currentPrice )).intValue() ) + " @ " +
						df.format( currentPrice.intValue() ) + " meat" );
				}
				searchLabel.setText( searchItem.getName() );
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
			searchLabel = JComponentUtilities.createLabel( "Price Summary", JLabel.CENTER,
				Color.black, Color.white );
			add( searchLabel, BorderLayout.NORTH );

			JList resultsDisplay = new JList( priceSummary );
			resultsDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			resultsDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			resultsDisplay.setVisibleRowCount( 11 );

			add( new JScrollPane( resultsDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
		}
	}
}