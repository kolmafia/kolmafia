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

/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
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
 *  [3] Neither the name "Spellcast development team" nor the names of
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

// layout
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;

// event listeners
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// containers
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;

// other imports
import java.text.DecimalFormat;
import java.text.ParseException;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extended <code>KoLFrame</code> which presents the user with the ability to
 * adventure in the Kingdom of Loathing.  As the class is developed, it will also
 * provide other adventure-related functionality, such as inventoryManage management
 * and mall purchases.  Its content panel will also change, pending the activity
 * executed at that moment.
 */

public class AdventureFrame extends KoLFrame
{
	private JTabbedPane tabs;

	private KoLMessenger kolchat;
	private ItemManageFrame isheet;
	private CharsheetFrame csheet;

	private AdventureSelectPanel adventureSelect;
	private MallSearchPanel mallSearch;
	private ClanBuffPanel clanBuff;
	private HeroDonationPanel heroDonation;
	private MeatStoragePanel meatStorage;

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 *
	 * @param	client	Client/session associated with this frame
	 * @param	adventureList	Adventures available to the user
	 * @param	resultsTally	Tally of adventuring results
	 */

	public AdventureFrame( KoLmafia client, LockableListModel adventureList, LockableListModel resultsTally )
	{
		super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()) +
			" (" + KoLRequest.getRootHostName() + ")", client );
		setResizable( false );

		tabs = new JTabbedPane();

		adventureSelect = new AdventureSelectPanel( adventureList, resultsTally );
		tabs.addTab( "Adventure Select", adventureSelect );

		mallSearch = new MallSearchPanel();
		tabs.addTab( "Mall of Loathing", mallSearch );

		clanBuff = new ClanBuffPanel();
		heroDonation = new HeroDonationPanel();
		meatStorage = new MeatStoragePanel();

		JPanel otherStuffPanel = new JPanel();
		otherStuffPanel.setLayout( new GridLayout( 3, 1 ) );
		otherStuffPanel.add( clanBuff, "" );
		otherStuffPanel.add( heroDonation, "" );
		otherStuffPanel.add( meatStorage, "" );
		tabs.addTab( "Other Activities", otherStuffPanel );

		getContentPane().add( tabs, BorderLayout.CENTER );
		contentPanel = adventureSelect;

		updateDisplay( ENABLED_STATE, " " );
		addWindowListener( new LogoutRequestAdapter() );

		addMenuBar();
	}

	/**
	 * Auxilary method used to enable and disable a frame.  By default,
	 * this attempts to toggle the enable/disable status on all tabs
	 * and the view menu item, as well as the item manager if it's
	 * currently visible.
	 *
	 * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		for ( int i = 0; i < tabs.getTabCount(); ++i )
			tabs.setEnabledAt( i, isEnabled );

		if ( isheet != null && isheet.isShowing() )
			isheet.setEnabled( isEnabled );

		adventureSelect.setEnabled( isEnabled );
		mallSearch.setEnabled( isEnabled );
		clanBuff.setEnabled( isEnabled );
		heroDonation.setEnabled( isEnabled );
		meatStorage.setEnabled( isEnabled );
	}

	/**
	 * Utility method used to add a menu bar to the <code>AdventureFrame</code>.
	 * The menu bar contains configuration options and the general license
	 * information associated with <code>KoLmafia</code>.  In addition, the
	 * method adds an item which allows the user to view their character sheet.
	 */

	private void addMenuBar()
	{
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar( menuBar );

		JMenuItem viewMenu = new JMenu("View");
		viewMenu.setMnemonic( KeyEvent.VK_V );
		menuBar.add( viewMenu );

		JMenuItem csheetItem = new JMenuItem( "Status Pane", KeyEvent.VK_S );
		csheetItem.addActionListener( new ViewCharacterSheetListener() );

		viewMenu.add( csheetItem );

		JMenuItem imanageItem = new JMenuItem( "Item Manager", KeyEvent.VK_I );
		imanageItem.addActionListener( new ViewItemManagerListener() );

		viewMenu.add( imanageItem );

		JMenuItem chatItem = new JMenuItem( "Chat of Loathing", KeyEvent.VK_C );
		chatItem.addActionListener( new ViewChatListener() );

		viewMenu.add( chatItem );

		addConfigureMenu( menuBar );
		addHelpMenu( menuBar );
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	private class AdventureSelectPanel extends KoLPanel
	{
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		private JComboBox locationField;
		private JTextField countField;

		public AdventureSelectPanel( LockableListModel adventureList, LockableListModel resultsTally )
		{
			super( "begin", "stop", new Dimension( 100, 20 ), new Dimension( 225, 20 ) );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			locationField = new JComboBox( adventureList );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Location: ", locationField );
			elements[1] = new VerifiableElement( "# of turnips: ", countField );

			setContent( elements, resultsTally );
		}

		protected void setContent( VerifiableElement [] elements, LockableListModel resultsTally )
		{
			super.setContent( elements );

			JPanel southPanel = new JPanel();
			southPanel.setLayout( new BorderLayout( 10, 10 ) );
			southPanel.add( actionStatusPanel, BorderLayout.NORTH );
			southPanel.add( new AdventureResultsPanel( resultsTally ), BorderLayout.SOUTH );
			add( southPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			locationField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
		}

		public void clear()
		{
			countField.setText( "" );
			requestFocus();
		}

		protected void actionConfirmed()
		{
			// Once the stubs are finished, this will notify the
			// client to begin adventuring based on the values
			// placed in the input fields.

			contentPanel = adventureSelect;
			(new AdventureRequestThread()).start();
		}

		protected void actionCancelled()
		{
			// Once the stubs are finished, this will notify the
			// client to terminate the loop early.  For now, since
			// there's no actual functionality, simply request focus

			contentPanel = adventureSelect;
			updateDisplay( ENABLED_STATE, "Adventuring terminated." );
			client.cancelRequest();
			requestFocus();
		}

		public void requestFocus()
		{	locationField.requestFocus();
		}

		/**
		 * An internal class which represents the panel used for tallying the
		 * results in the <code>AdventureFrame</code>.  Note that all of the
		 * tallying functionality is handled by the <code>LockableListModel</code>
		 * provided, so this functions as a container for that list model.
		 */

		private class AdventureResultsPanel extends JPanel
		{
			public AdventureResultsPanel( LockableListModel resultsTally )
			{
				setLayout( new BorderLayout() );
				setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
				add( JComponentUtilities.createLabel( "Session Results Summary", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );

				JList tallyDisplay = new JList( resultsTally );
				tallyDisplay.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
				tallyDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
				tallyDisplay.setVisibleRowCount( 15 );

				add( new JScrollPane( tallyDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually make the adventuring requests.
		 */

		private class AdventureRequestThread extends Thread
		{
			public AdventureRequestThread()
			{
				super( "Adv-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					int count = df.parse( countField.getText() ).intValue();
					Runnable request = (Runnable) locationField.getSelectedItem();
					client.makeRequest( request, count );

					if ( isheet != null )
						isheet.refreshConcoctionsList();
				}
				catch ( ParseException e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}
			}
		}
	}

	/**
	 * An internal class which represents the panel used for mall
	 * searches in the <code>AdventureFrame</code>.
	 */

	private class MallSearchPanel extends KoLPanel
	{
		private boolean currentlyBuying;

		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		private JTextField searchField;
		private JTextField countField;
		private JTextField maxPerStoreField;

		private LockableListModel results;
		private JList resultsDisplay;

		public MallSearchPanel()
		{
			super( "search", "purchase", new Dimension( 100, 20 ), new Dimension( 225, 20 ) );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			searchField = new JTextField();
			countField = new JTextField();
			maxPerStoreField = new JTextField();
			results = new LockableListModel();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Search String: ", searchField );
			elements[1] = new VerifiableElement( "Limit Results: ", countField );
			elements[2] = new VerifiableElement( "Per Store Limit: ", maxPerStoreField );

			setContent( elements );
			currentlyBuying = false;
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements, null, null, null, true, true );

			JPanel southPanel = new JPanel();
			southPanel.setLayout( new BorderLayout( 10, 10 ) );
			southPanel.add( actionStatusPanel, BorderLayout.NORTH );
			southPanel.add( new SearchResultsPanel(), BorderLayout.SOUTH );
			add( southPanel, BorderLayout.SOUTH );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			searchField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
			resultsDisplay.setEnabled( isEnabled );
		}

		public void clear()
		{
			searchField.setText( "" );
			requestFocus();
		}

		protected void actionConfirmed()
		{
			contentPanel = mallSearch;
			(new SearchMallRequestThread()).start();
		}

		protected void actionCancelled()
		{
			if ( currentlyBuying )
				return;

			contentPanel = mallSearch;
			(new PurchaseRequestThread()).start();
		}

		public void requestFocus()
		{	searchField.requestFocus();
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
				add( JComponentUtilities.createLabel( "Search Results", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );

				resultsDisplay = new JList( results );
				resultsDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
				resultsDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
				resultsDisplay.setVisibleRowCount( 15 );

				add( new JScrollPane( resultsDisplay, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
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
				try
				{
					int storeCount = countField.getText().trim().length() == 0 ? 13 :
						df.parse( countField.getText() ).intValue();

					updateDisplay( DISABLED_STATE, "Searching for items..." );
					(new SearchMallRequest( client, searchField.getText(), storeCount, results )).run();
				}
				catch ( Exception e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}
			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually make the mall search request.
		 */

		private class PurchaseRequestThread extends Thread
		{
			public PurchaseRequestThread()
			{
				super( "Purchase-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				updateDisplay( DISABLED_STATE, "Purchasing items..." );
				Object [] purchases = resultsDisplay.getSelectedValues();

				try
				{
					int maxPerStore = maxPerStoreField.getText().trim().length() == 0 ? Integer.MAX_VALUE :
						df.parse( maxPerStoreField.getText() ).intValue();

					MallPurchaseRequest currentRequest;

					for ( int i = 0; i < purchases.length; ++i )
					{
						if ( purchases[i] instanceof MallPurchaseRequest )
						{
							currentRequest = (MallPurchaseRequest) purchases[i];
							currentRequest.setMaximumQuantity( maxPerStore );
							currentRequest.run();
							results.remove( purchases[i] );
						}
					}
				}
				catch ( Exception e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}

				if ( isheet != null )
					isheet.refreshConcoctionsList();

				updateDisplay( ENABLED_STATE, "Purchases complete." );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for clan
	 * buffs in the <code>AdventureFrame</code>.
	 */

	private class ClanBuffPanel extends KoLPanel
	{
		private boolean isBuffing;

		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		private JComboBox buffField;
		private JTextField countField;

		public ClanBuffPanel()
		{
			super( "purchase buffs", "stop purchases", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );
			this.isBuffing = false;

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			buffField = new JComboBox( ClanBuffRequest.getRequestList( client ) );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Clan Buff: ", buffField );
			elements[1] = new VerifiableElement( "# of times: ", countField );

			setContent( elements );
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			add( JComponentUtilities.createLabel( "Make Your Clan 1335", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void clear()
		{	countField.setText( "" );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			buffField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			isBuffing = true;
			contentPanel = clanBuff;
			(new ClanBuffRequestThread()).start();
		}

		protected void actionCancelled()
		{
			if ( isBuffing )
			{
				isBuffing = false;
				contentPanel = clanBuff;
				client.updateAdventure( false, false );
				updateDisplay( ENABLED_STATE, "Purchase attempts cancelled." );
			}
		}

		public void requestFocus()
		{
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually purchase the clan buffs.
		 */

		private class ClanBuffRequestThread extends Thread
		{
			public ClanBuffRequestThread()
			{
				super( "Clan-Buff-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					int buffCount = countField.getText().trim().length() == 0 ? 0 :
						df.parse( countField.getText() ).intValue();
					Runnable buff = (Runnable) buffField.getSelectedItem();

					client.makeRequest( buff, buffCount );
					isBuffing = false;
				}
				catch ( Exception e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}

			}
		}
	}

	/**
	 * An internal class which represents the panel used for donations to
	 * the statues in the shrine.
	 */

	private class HeroDonationPanel extends KoLPanel
	{
		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		private JComboBox heroField;
		private JTextField amountField;

		public HeroDonationPanel()
		{
			super( "worship statue", "blow up statue", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			LockableListModel heroes = new LockableListModel();
			heroes.add( "Statue of Boris" );
			heroes.add( "Statue of Jarlsberg" );
			heroes.add( "Statue of Sneaky Pete" );

			heroField = new JComboBox( heroes );
			amountField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Donate To: ", heroField );
			elements[1] = new VerifiableElement( "Amount: ", amountField );

			setContent( elements );
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			add( JComponentUtilities.createLabel( "The Hall of the Legends of the Times of Old", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void clear()
		{	amountField.setText( "" );
		}

		public void setStatusMessage( String s )
		{	actionStatusLabel.setText( s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			heroField.setEnabled( isEnabled );
			amountField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			contentPanel = heroDonation;
			(new HeroDonationThread()).start();
		}

		protected void actionCancelled()
		{
			if ( AdventureFrame.this.isEnabled() )
			{
				contentPanel = heroDonation;
				if ( heroField.getSelectedIndex() != -1 )
					updateDisplay( NOCHANGE_STATE, "You have killed the Hermit hiding behind the " + heroField.getSelectedItem() );
				else
					updateDisplay( NOCHANGE_STATE, "Blow up which statue?" );
			}
		}

		public void requestFocus()
		{
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually purchase the clan buffs.
		 */

		private class HeroDonationThread extends Thread
		{
			public HeroDonationThread()
			{
				super( "Donation-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					int amount = amountField.getText().trim().length() == 0 ? 0 :
						df.parse( amountField.getText() ).intValue();

					if ( heroField.getSelectedIndex() != -1 )
					{
						updateDisplay( DISABLED_STATE, "Attempting donation..." );
						(new HeroDonationRequest( client, heroField.getSelectedIndex() + 1, amount )).run();
						updateDisplay( ENABLED_STATE, "Donation attempt complete." );
					}
				}
				catch ( Exception e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}

			}
		}
	}

	private class MeatStoragePanel extends KoLPanel
	{
		private JPanel actionStatusPanel;

		private JTextField amountField;
		private JTextField inClosetField;

		public MeatStoragePanel()
		{
			super( "put in closet", "take from closet", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			amountField = new JTextField();
			inClosetField = new JTextField( df.format( client == null ? 0 : client.getCharacterData().getClosetMeat() ) );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Transaction: ", amountField );
			elements[1] = new VerifiableElement( "Inside Closet: ", inClosetField );

			setContent( elements );
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements, null, null, null, true, true );
			inClosetField.setEnabled( false );
			add( JComponentUtilities.createLabel( "Meat Management (Closet)", JLabel.CENTER,
					Color.black, Color.white ), BorderLayout.NORTH );
			add( actionStatusPanel, BorderLayout.SOUTH );
		}

		public void clear()
		{	amountField.setText( "" );
		}

		public void setStatusMessage( String s )
		{	inClosetField.setText( s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			amountField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			contentPanel = meatStorage;
			(new MeatStorageThread( true )).start();
		}

		protected void actionCancelled()
		{
			contentPanel = meatStorage;
			(new MeatStorageThread( false )).start();
		}

		public void requestFocus()
		{
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually purchase the clan buffs.
		 */

		private class MeatStorageThread extends Thread
		{
			private boolean isDeposit;

			public MeatStorageThread( boolean isDeposit )
			{
				super( "Meat-Storage-Thread" );
				setDaemon( true );
				this.isDeposit = isDeposit;
			}

			public void run()
			{
				try
				{
					int amount = amountField.getText().trim().length() == 0 ? 0 :
						df.parse( amountField.getText() ).intValue();

					updateDisplay( DISABLED_STATE, "Executing transaction..." );
					(new ItemStorageRequest( client, amount, isDeposit )).run();
					updateDisplay( ENABLED_STATE, df.format( client == null ? amount : client.getCharacterData().getClosetMeat() ) );

				}
				catch ( Exception e )
				{
					// If the number placed inside of the count list was not
					// an actual integer value, pretend nothing happened.
					// Using exceptions for flow control is bad style, but
					// this will be fixed once we add functionality.
				}

			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing a character sheet.
	 */

	private class ViewCharacterSheetListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			updateDisplay( NOCHANGE_STATE, "Retrieving character data..." );
			(new ViewCharacterSheetThread()).start();
		}

		private class ViewCharacterSheetThread extends Thread
		{
			public ViewCharacterSheetThread()
			{
				super( "CSheet-Display-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				if ( csheet != null && csheet.isShowing() )
				{
					csheet.setVisible( false );
					csheet.dispose();
					csheet = null;
				}

				csheet = new CharsheetFrame( client );
				csheet.pack();  csheet.setVisible( true );

				csheet.requestFocus();
				updateDisplay( NOCHANGE_STATE, "" );
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the chat window.
	 */

	private class ViewChatListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new ViewChatThread()).start();
		}

		private class ViewChatThread extends Thread
		{
			public ViewChatThread()
			{
				super( "Chat-Display-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				if ( client.getMessenger() == null )
				{
					client.initializeChat();
					kolchat = client.getMessenger();
				}

				updateDisplay( NOCHANGE_STATE, "" );
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the item manager.
	 */

	private class ViewItemManagerListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new ViewItemManagerThread()).start();
		}

		private class ViewItemManagerThread extends Thread
		{
			public ViewItemManagerThread()
			{
				super( "Item-Display-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				if ( isheet == null || !isheet.isShowing() )
				{
					isheet = new ItemManageFrame( client );
					isheet.pack();  isheet.setVisible( true );
				}

				isheet.setEnabled( contentPanel.isEnabled() );
				isheet.requestFocus();
			}
		}
	}

	/**
	 * An internal class used to handle logout whenever the window
	 * is closed.  An instance of this class is added to the window
	 * listener list.
	 */

	private class LogoutRequestAdapter extends WindowAdapter
	{
		public void windowClosed( WindowEvent e )
		{
			if ( client != null )
			{
				// Create a new instance of a client, and allow
				// the current client to run down in a separate
				// Thread.

				(new LogoutRequestThread()).start();
				new KoLmafia();
			}
		}

		private class LogoutRequestThread extends Thread
		{
			public LogoutRequestThread()
			{	setDaemon( true );
			}

			public void run()
			{
				(new LogoutRequest( client )).run();
				client.deinitialize();

				if ( csheet != null && csheet.isShowing() )
				{
					csheet.setVisible( false );
					csheet.dispose();
					csheet = null;
				}

				if ( kolchat != null && kolchat.isShowing() )
				{
					kolchat.setVisible( false );
					kolchat.dispose();
					kolchat = null;
				}

				if ( isheet != null && isheet.isShowing() )
				{
					isheet.setVisible( false );
					isheet.dispose();
					isheet = null;
				}
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new AdventureFrame( null, new LockableListModel(), new LockableListModel() );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}