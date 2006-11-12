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
import java.awt.CardLayout;
import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class MallSearchFrame extends KoLPanelFrame
{
	private static MallSearchFrame INSTANCE = null;

	private boolean currentlyBuying;
	private SortedListModel results;
	private JList resultsList;
	private MallSearchPanel mallSearch;

	public MallSearchFrame()
	{
		super( "Purchases" );

		INSTANCE = this;
		this.mallSearch = new MallSearchPanel();

		setContentPanel( mallSearch );
	}

	public void dispose()
	{
		INSTANCE = null;
		super.dispose();
	}

	/**
	 * An internal class which represents the panel used for mall
	 * searches in the <code>AdventureFrame</code>.
	 */

	private class MallSearchPanel extends KoLPanel
	{
		private MutableComboBox searchField;
		private JTextField countField;

		private JCheckBox forceSortingCheckBox;
		private JCheckBox limitPurchasesCheckBox;

		public MallSearchPanel()
		{
			super( "search", "purchase", "cancel", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );

			searchField = new MutableComboBox( new SortedListModel(), true, true );
			countField = new JTextField();

			forceSortingCheckBox = new JCheckBox();
			limitPurchasesCheckBox = new JCheckBox();
			results = new SortedListModel();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Force Sort: " ), "" );
			checkBoxPanels.add( forceSortingCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Limit Purchases: " ), "" );
			checkBoxPanels.add( limitPurchasesCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			limitPurchasesCheckBox.setSelected( true );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Item to Find: ", searchField );
			elements[1] = new VerifiableElement( "Search Limit: ", countField );
			elements[2] = new VerifiableElement( " ", checkBoxPanels, false );

			setContent( elements );

			int searchCount = StaticEntity.getIntegerProperty( "defaultLimit" );
			countField.setText( searchCount <= 0 ? "5" : String.valueOf( searchCount ) );

			add( new SearchResultsPanel(), BorderLayout.CENTER );

			currentlyBuying = false;
			countField.setText( StaticEntity.getProperty( "defaultLimit" ) );
		}

		public void actionConfirmed()
		{
			int searchCount = getValue( countField, 0 );
			if ( searchCount > 0 )
				StaticEntity.setProperty( "defaultLimit", String.valueOf( searchCount ) );

			MallPurchaseRequest.setUsePriceComparison( forceSortingCheckBox.isSelected() );
			searchMall( new SearchMallRequest( (String) searchField.getSelectedItem(), searchCount, results, false ) );
		}

		public void actionCancelled()
		{	(new RequestThread( new MallPurchaseRunnable() )).start();
		}

		private class MallPurchaseRunnable implements Runnable
		{
			public void run()
			{
				if ( currentlyBuying )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "Purchases stopped." );
					return;
				}

				Object [] purchases = resultsList.getSelectedValues();
				if ( purchases == null || purchases.length == 0 )
				{
					setStatusMessage( "Please select a store from which to purchase." );
					return;
				}

				int defaultPurchases = 0;
				for ( int i = 0; i < purchases.length; ++i )
					defaultPurchases += ((MallPurchaseRequest) purchases[i]).getQuantity() == MallPurchaseRequest.MAX_QUANTITY ?
						MallPurchaseRequest.MAX_QUANTITY : ((MallPurchaseRequest) purchases[i]).getLimit();

				int count = limitPurchasesCheckBox.isSelected() || defaultPurchases >= 1000 ?
					getQuantity( "Maximum number of items to purchase?", defaultPurchases, 1 ) : defaultPurchases;

				if ( count == 0 )
					return;

				currentlyBuying = true;

				KoLmafia.forceContinue();
				StaticEntity.getClient().makePurchases( results, purchases, count );
				currentlyBuying = false;

				resultsList.updateUI();
			}
		}

		public void requestFocus()
		{	searchField.requestFocus();
		}
	}

	public static void searchMall( SearchMallRequest request )
	{
		if ( INSTANCE == null )
			KoLmafiaGUI.constructFrame( "MallSearchFrame" );

		INSTANCE.results.clear();
		request.setResults( INSTANCE.results );
		(new RequestThread( request )).start();
	}

	private String getPurchaseSummary( Object [] purchases )
	{
		if ( purchases == null || purchases.length == 0 )
			return "";

		long totalPrice = 0;
		int totalPurchases = 0;
		MallPurchaseRequest currentPurchase = null;

		for ( int i = 0; i < purchases.length; ++i )
		{
			currentPurchase = (MallPurchaseRequest) purchases[i];
			totalPurchases += currentPurchase.getLimit();
			totalPrice += ((long)currentPurchase.getLimit()) * ((long)currentPurchase.getPrice());
		}

		return COMMA_FORMAT.format( totalPurchases ) + " " + currentPurchase.getItemName() + " for " + COMMA_FORMAT.format( totalPrice ) + " meat";
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
			setLayout( new CardLayout( 10, 10 ) );

			JPanel resultsPanel = new JPanel( new BorderLayout() );
			resultsPanel.add( JComponentUtilities.createLabel( "Search Results", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );

			resultsList = new JList( results );
			resultsList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			resultsList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			resultsList.setVisibleRowCount( 11 );

			resultsList.addListSelectionListener( new PurchaseSelectListener() );
			resultsPanel.add( new SimpleScrollPane( resultsList ), BorderLayout.CENTER );

			add( resultsPanel, "" );
		}

		/**
		 * An internal listener class which detects which values are selected
		 * in the search results panel.
		 */

		private class PurchaseSelectListener implements ListSelectionListener
		{
			public void valueChanged( ListSelectionEvent e )
			{
				if ( e.getValueIsAdjusting() )
					return;

				// Reset the status message on this panel to
				// show what the current state of the selections
				// is at this time.

				if ( !currentlyBuying )
					mallSearch.setStatusMessage( getPurchaseSummary( resultsList.getSelectedValues() ) );
			}
		}
	}
}
