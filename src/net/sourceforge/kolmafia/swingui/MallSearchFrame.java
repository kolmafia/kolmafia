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
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.swingui.listener.DefaultComponentFocusTraversalPolicy;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterComboBox;
import net.sourceforge.kolmafia.swingui.widget.AutoHighlightTextField;
import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MallSearchFrame
	extends GenericPanelFrame
{
	private static MallSearchFrame INSTANCE = null;
	private static final SortedListModel pastSearches = new SortedListModel();

	private boolean currentlySearching;
	private boolean currentlyBuying;
	private SortedListModel results;
	private ShowDescriptionList resultsList;
	private final MallSearchPanel mallSearch;

	public MallSearchFrame()
	{
		super( "Purchases" );

		this.mallSearch = new MallSearchPanel();

		this.setContentPanel( this.mallSearch );

		MallSearchFrame.INSTANCE = this;
	}

	public JTabbedPane getTabbedPane()
	{
		return null;
	}

	/**
	 * An internal class which represents the panel used for mall searches in the <code>AdventureFrame</code>.
	 */

	private class MallSearchPanel
		extends GenericPanel
	{
		private final JComponent searchField;
		private final AutoHighlightTextField countField;

		private final JCheckBox forceSortingCheckBox;
		private final JCheckBox limitPurchasesCheckBox;

		public MallSearchPanel()
		{
			super( "search", "purchase", "cancel", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );

			this.searchField =
				Preferences.getBoolean( "cacheMallSearches" ) ? (JComponent) new AutoFilterComboBox(
					MallSearchFrame.pastSearches, true ) : (JComponent) new AutoHighlightTextField();

			this.countField = new AutoHighlightTextField();

			this.forceSortingCheckBox = new JCheckBox();
			this.limitPurchasesCheckBox = new JCheckBox();
			MallSearchFrame.this.results = new SortedListModel();

			JPanel checkBoxPanels = new JPanel();
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Force Sort: " ), "" );
			checkBoxPanels.add( this.forceSortingCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			checkBoxPanels.add( new JLabel( "Limit Purchases: " ), "" );
			checkBoxPanels.add( this.limitPurchasesCheckBox );
			checkBoxPanels.add( Box.createHorizontalStrut( 20 ) );
			this.limitPurchasesCheckBox.setSelected( true );

			VerifiableElement[] elements = new VerifiableElement[ 3 ];
			elements[ 0 ] = new VerifiableElement( "Item to Find: ", this.searchField );
			elements[ 1 ] = new VerifiableElement( "Search Limit: ", this.countField );
			elements[ 2 ] = new VerifiableElement( " ", checkBoxPanels, false );

			int searchCount = Preferences.getInteger( "defaultLimit" );
			this.countField.setText( searchCount <= 0 ? "5" : String.valueOf( searchCount ) );

			this.setContent( elements );

			this.add( new SearchResultsPanel(), BorderLayout.CENTER );
			MallSearchFrame.this.currentlySearching = false;
			MallSearchFrame.this.currentlyBuying = false;

			this.setFocusCycleRoot( true );
			this.setFocusTraversalPolicy( new DefaultComponentFocusTraversalPolicy( this.searchField ) );
		}

		public void actionConfirmed()
		{
			int searchCount = InputFieldUtilities.getValue( this.countField, 0 );
			if ( searchCount > 0 )
			{
				Preferences.setInteger( "defaultLimit", searchCount );
			}

			PurchaseRequest.setUsePriceComparison( this.forceSortingCheckBox.isSelected() );

			String searchText = null;

			if ( this.searchField instanceof AutoHighlightTextField )
			{
				searchText = ( (AutoHighlightTextField) this.searchField ).getText();
			}
			else
			{
				( (AutoFilterComboBox) this.searchField ).forceAddition();
				searchText = (String) ( (AutoFilterComboBox) this.searchField ).getSelectedItem();
			}

			MallSearchFrame.this.currentlySearching = true;

			MallSearchFrame.searchMall( new MallSearchRequest(
				searchText, searchCount, MallSearchFrame.this.results, false ) );

			MallSearchFrame.this.currentlySearching = false;

			this.searchField.requestFocus();
		}

		public void actionCancelled()
		{
			if ( MallSearchFrame.this.currentlySearching )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Search stopped." );
				return;
			}

			if ( MallSearchFrame.this.currentlyBuying )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Purchases stopped." );
				return;
			}

			Object[] purchases = MallSearchFrame.this.resultsList.getSelectedValues();
			if ( purchases == null || purchases.length == 0 )
			{
				this.setStatusMessage( "Please select a store from which to purchase." );
				return;
			}

			int defaultPurchases = 0;
			for ( int i = 0; i < purchases.length; ++i )
			{
				defaultPurchases +=
					( (PurchaseRequest) purchases[ i ] ).getQuantity() == PurchaseRequest.MAX_QUANTITY ? PurchaseRequest.MAX_QUANTITY : ( (PurchaseRequest) purchases[ i ] ).getLimit();
			}

			int count =
				this.limitPurchasesCheckBox.isSelected() || defaultPurchases >= 1000 ? InputFieldUtilities.getQuantity(
					"Maximum number of items to purchase?", defaultPurchases, 1 ) : defaultPurchases;

			if ( count == 0 )
			{
				return;
			}

			MallSearchFrame.this.currentlyBuying = true;

			SpecialOutfit.createImplicitCheckpoint();
			StaticEntity.getClient().makePurchases( MallSearchFrame.this.results, purchases, count, false );
			SpecialOutfit.restoreImplicitCheckpoint();

			MallSearchFrame.this.currentlyBuying = false;
		}
	}

	public static final void searchMall( final MallSearchRequest request )
	{
		if ( MallSearchFrame.INSTANCE == null )
		{
			KoLmafiaGUI.constructFrame( "MallSearchFrame" );
		}

		MallSearchFrame.INSTANCE.results.clear();
		request.setResults( MallSearchFrame.INSTANCE.results );

		RequestThread.postRequest( request );
	}

	private String getPurchaseSummary( final Object[] purchases )
	{
		if ( purchases == null || purchases.length == 0 )
		{
			return "";
		}

		long totalPrice = 0;
		int totalPurchases = 0;
		PurchaseRequest currentPurchase = null;

		for ( int i = 0; i < purchases.length; ++i )
		{
			currentPurchase = (PurchaseRequest) purchases[ i ];
			totalPurchases += currentPurchase.getLimit();
			totalPrice += (long) currentPurchase.getLimit() * (long) currentPurchase.getPrice();
		}

		return KoLConstants.COMMA_FORMAT.format( totalPurchases ) + " " + currentPurchase.getItemName() + " for " + KoLConstants.COMMA_FORMAT.format( totalPrice ) + " meat";
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

			JPanel resultsPanel = new JPanel( new BorderLayout() );
			resultsPanel.add( JComponentUtilities.createLabel(
				"Search Results", SwingConstants.CENTER, Color.black, Color.white ), BorderLayout.NORTH );

			MallSearchFrame.this.resultsList = new ShowDescriptionList( MallSearchFrame.this.results );
			MallSearchFrame.this.resultsList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			MallSearchFrame.this.resultsList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			MallSearchFrame.this.resultsList.setVisibleRowCount( 11 );

			MallSearchFrame.this.resultsList.addListSelectionListener( new PurchaseSelectListener() );
			this.add( new GenericScrollPane( MallSearchFrame.this.resultsList ), BorderLayout.CENTER );
		}

		/**
		 * An internal listener class which detects which values are selected in the search results panel.
		 */

		private class PurchaseSelectListener
			implements ListSelectionListener
		{
			public void valueChanged( final ListSelectionEvent e )
			{
				if ( e.getValueIsAdjusting() )
				{
					return;
				}

				// Reset the status message on this panel to
				// show what the current state of the selections
				// is at this time.

				if ( !MallSearchFrame.this.currentlyBuying )
				{
					MallSearchFrame.this.mallSearch.setStatusMessage( MallSearchFrame.this.getPurchaseSummary( MallSearchFrame.this.resultsList.getSelectedValues() ) );
				}
			}
		}
	}
}
