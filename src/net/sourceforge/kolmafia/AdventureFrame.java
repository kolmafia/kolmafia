
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;

// event listeners
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

// containers
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;

// other imports
import java.util.Iterator;
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
	private static final Color ERROR_COLOR = new Color( 255, 128, 128 );
	private static final Color ENABLED_COLOR = new Color( 128, 255, 128 );
	private static final Color DISABLED_COLOR = null;

	private JTabbedPane tabs;
	private JTextField inClosetField;

	private BuffBotFrame buffbotDisplay;

	private AdventureSelectPanel adventureSelect;
	private MallSearchPanel mallSearch;
	private RemoveEffectsPanel removeEffects;
	private SkillBuffPanel skillBuff;
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

		this.isEnabled = true;
		this.tabs = new JTabbedPane();

		this.adventureSelect = new AdventureSelectPanel( adventureList, resultsTally );
		tabs.addTab( "Adventure Select", adventureSelect );

		this.mallSearch = new MallSearchPanel();
		tabs.addTab( "Mall of Loathing", mallSearch );

		this.heroDonation = new HeroDonationPanel();
		this.meatStorage = new MeatStoragePanel();
		this.removeEffects = new RemoveEffectsPanel();
		this.skillBuff = new SkillBuffPanel();

		JPanel otherStuffPanel = new JPanel();
		otherStuffPanel.setLayout( new BoxLayout( otherStuffPanel, BoxLayout.Y_AXIS ) );
		otherStuffPanel.add( skillBuff );
		otherStuffPanel.add( meatStorage );
		otherStuffPanel.add( heroDonation );
		otherStuffPanel.add( removeEffects );

		JScrollPane otherStuffScroller = new JScrollPane( otherStuffPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( otherStuffScroller, 500, 300 );
		tabs.addTab( "Other Activities", otherStuffScroller );

		addCompactPane();
		getContentPane().add( tabs, BorderLayout.CENTER );
		contentPanel = adventureSelect;

		addWindowListener( new LogoutRequestAdapter() );
		addMenuBar();
	}

	public void refreshConcoctionsList()
	{
		if ( itemManager != null )
			itemManager.refreshConcoctionsList();
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
		this.isEnabled = isEnabled && !client.isBuffBotActive();

		if ( mailMenuItem != null )
			mailMenuItem.setEnabled( this.isEnabled );

		for ( int i = 0; i < existingFrames.size(); ++i )
		{
			KoLFrame currentFrame = (KoLFrame) existingFrames.get(i);
			if ( currentFrame.isShowing())
				currentFrame.setEnabled( this.isEnabled );
		}

		if ( adventureSelect != null )
			adventureSelect.setEnabled( this.isEnabled );

		if ( mallSearch != null )
			mallSearch.setEnabled( this.isEnabled );

		if ( heroDonation != null )
			heroDonation.setEnabled( this.isEnabled );

		if ( meatStorage != null )
			meatStorage.setEnabled( this.isEnabled );

		if ( skillBuff != null )
			skillBuff.setEnabled( this.isEnabled );

		if ( removeEffects != null )
			removeEffects.setEnabled( this.isEnabled );

		Iterator framesIterator = existingFrames.iterator();
		KoLFrame currentFrame;

		while ( framesIterator.hasNext() )
		{
			currentFrame = (KoLFrame) framesIterator.next();
			currentFrame.setEnabled( isEnabled );
		}
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

		JMenuItem mallItem = new JMenuItem( "Manipulate Mall", KeyEvent.VK_M );
		mallItem.addActionListener( new DisplayFrameListener( StoreManageFrame.class ) );
		JMenuItem caseItem = new JMenuItem( "Yeti's Museum", KeyEvent.VK_Y );
		caseItem.addActionListener( new DisplayFrameListener( MuseumFrame.class ) );

		JMenu statusMenu = addStatusMenu( menuBar );
		statusMenu.add( mallItem );
		statusMenu.add( caseItem );

		JMenuItem foodItem = new JMenuItem( "Camping Routine", KeyEvent.VK_C );
		foodItem.addActionListener( new GetBreakfastListener() );
		JMenuItem buffbotMenuItem = new JMenuItem( "Evil BuffBot Mode", KeyEvent.VK_E );
		buffbotMenuItem.addActionListener( new ViewBuffBotPanelListener() );

		JMenu scriptMenu = addScriptMenu( menuBar );
		scriptMenu.add( foodItem );
		scriptMenu.add( buffbotMenuItem );

		JMenu visitMenu = new JMenu( "Travel" );
		visitMenu.setMnemonic( KeyEvent.VK_T );

		JMenuItem arenaItem = new JMenuItem( "Eat Cake-Arena", KeyEvent.VK_E );
		arenaItem.addActionListener( new DisplayFrameListener( CakeArenaFrame.class ) );
		JMenuItem hermitItem = new JMenuItem( "Hermit Hideout", KeyEvent.VK_H );
		hermitItem.addActionListener( new HermitRequestListener() );
		JMenuItem trapperItem = new JMenuItem( "Mountain Traps", KeyEvent.VK_M );
		trapperItem.addActionListener( new TrapperRequestListener() );
		JMenuItem hunterItem = new JMenuItem( "Seaside Towels", KeyEvent.VK_S );
		hunterItem.addActionListener( new HunterRequestListener() );

		visitMenu.add( arenaItem );
		visitMenu.add( hermitItem );
		visitMenu.add( trapperItem );
		visitMenu.add( hunterItem );

		menuBar.add( visitMenu );

		JMenuItem clanItem = new JMenuItem( "Manage Your Clan", KeyEvent.VK_M );
		clanItem.addActionListener( new DisplayFrameListener( ClanManageFrame.class ) );

		JMenu peopleMenu = addPeopleMenu( menuBar );
		peopleMenu.add( clanItem );

		JMenuItem resetItem = new JMenuItem( "Reset Session", KeyEvent.VK_R );
		resetItem.addActionListener( new ResetSessionListener() );

		JMenu configMenu = addConfigureMenu( menuBar );
		configMenu.add( resetItem );

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
			super( "begin", "stop", new Dimension( 100, 20 ), new Dimension( 270, 20 ) );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			locationField = new JComboBox( adventureList );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Location: ", locationField );
			elements[1] = new VerifiableElement( "# of turnips: ", countField );
			elements[2] = new VerifiableElement( "Active Effects: ", new JComboBox(
				client == null || client.getCharacterData() == null ? new LockableListModel() :
					client.getCharacterData().getEffects().getMirrorImage() ) );

			setContent( elements, resultsTally );

			String lastAdventure = client == null ? "" : client.getSettings().getProperty( "lastAdventure" );
			if ( lastAdventure != null )
				for ( int i = 0; i < adventureList.size(); ++i )
					if ( adventureList.get(i).toString().equals( lastAdventure ) )
						locationField.setSelectedItem( adventureList.get(i) );
		}

		protected void setContent( VerifiableElement [] elements, LockableListModel resultsTally )
		{
			super.setContent( elements );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout( 10, 10 ) );
			centerPanel.add( actionStatusPanel, BorderLayout.NORTH );
			centerPanel.add( new AdventureResultsPanel( resultsTally ), BorderLayout.CENTER );
			add( centerPanel, BorderLayout.CENTER );
			setDefaultButton( confirmedButton );
		}

		public void setStatusMessage( int displayState, String s )
		{
			if ( !actionStatusLabel.getText().equals( "Session timed out." ) && !actionStatusLabel.getText().equals( "Nightly maintenance." ) )
			{
				actionStatusLabel.setText( s );
				switch ( displayState )
				{
					case ERROR_STATE:
						sidePanel.setBackground( ERROR_COLOR );
						break;
					case ENABLED_STATE:
						if ( !isExecutingScript )
							sidePanel.setBackground( ENABLED_COLOR );
						break;
					case DISABLED_STATE:
						sidePanel.setBackground( DISABLED_COLOR );
						break;
				}
			}
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			locationField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
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
			updateDisplay( ERROR_STATE, "Adventuring terminated." );
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
				tallyDisplay.setVisibleRowCount( 11 );

				add( new JScrollPane( tallyDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
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
				int count = getValue( countField );
				Runnable request = (Runnable) locationField.getSelectedItem();

				if ( request != null )
				{
					client.getSettings().setProperty( "lastAdventure", request.toString() );
					client.getSettings().saveSettings();
					client.makeRequest( request, count );
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
		private JCheckBox limitPurchasesField;

		private LockableListModel results;
		private JList resultsDisplay;

		public MallSearchPanel()
		{
			super( "search", "purchase", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );
			setDefaultButton( confirmedButton );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			searchField = new JTextField();
			countField = new JTextField();
			limitPurchasesField = new JCheckBox();
			results = new LockableListModel();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Search String: ", searchField );
			elements[1] = new VerifiableElement( "Search Limit: ", countField );
			elements[2] = new VerifiableElement( "Limit Purchases: ", limitPurchasesField );

			setContent( elements );
			currentlyBuying = false;
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements, null, null, null, true, true );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout( 10, 10 ) );
			centerPanel.add( actionStatusPanel, BorderLayout.NORTH );
			centerPanel.add( new SearchResultsPanel(), BorderLayout.CENTER );
			add( centerPanel, BorderLayout.CENTER );
			setDefaultButton( confirmedButton );
		}

		public void setStatusMessage( int displayState, String s )
		{
			if ( !actionStatusLabel.getText().equals( "Session timed out." ) && !actionStatusLabel.getText().equals( "Nightly maintenance." ) )
			{
				actionStatusLabel.setText( s );
				switch ( displayState )
				{
					case ERROR_STATE:
						sidePanel.setBackground( ERROR_COLOR );
						break;
					case ENABLED_STATE:
						sidePanel.setBackground( ENABLED_COLOR );
						break;
					case DISABLED_STATE:
						sidePanel.setBackground( DISABLED_COLOR );
						break;
				}
			}
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			searchField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
			limitPurchasesField.setEnabled( isEnabled );
			resultsDisplay.setEnabled( isEnabled );
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
				resultsDisplay.setVisibleRowCount( 11 );

				add( new JScrollPane( resultsDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
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
				int searchCount = getValue( countField, -1 );

				if ( searchCount == -1 )
					(new SearchMallRequest( client, searchField.getText(), results )).run();
				else
					(new SearchMallRequest( client, searchField.getText(), searchCount, results )).run();

				if ( results.size() > 0 )
					resultsDisplay.ensureIndexIsVisible( 0 );
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
				Object [] purchases = resultsDisplay.getSelectedValues();

				try
				{
					MallPurchaseRequest currentRequest;
					client.resetContinueState();

					int maxPurchases = limitPurchasesField.isSelected() ?
						df.parse( JOptionPane.showInputDialog( "Maximum number of items to purchase?" ) ).intValue() : Integer.MAX_VALUE;

					for ( int i = 0; i < purchases.length && maxPurchases > 0 && client.permitsContinue(); ++i )
					{
						if ( purchases[i] instanceof MallPurchaseRequest )
						{
							currentRequest = (MallPurchaseRequest) purchases[i];

							// Keep track of how many of the item you had before
							// you run the purchase request

							AdventureResult oldResult = new AdventureResult( currentRequest.getItemName(), 0 );
							int oldResultIndex = client.getInventory().indexOf( oldResult );
							if ( oldResultIndex != -1 )
								oldResult = (AdventureResult) client.getInventory().get( oldResultIndex );

							currentRequest.setMaximumQuantity( maxPurchases );
							currentRequest.run();

							// Calculate how many of the item you have now after
							// you run the purchase request

							int newResultIndex = client.getInventory().indexOf( oldResult );
							if ( newResultIndex != -1 )
							{
								AdventureResult newResult = (AdventureResult) client.getInventory().get( newResultIndex );
								maxPurchases -= newResult.getCount() - oldResult.getCount();
							}

							// Remove the purchase from the list!  Because you
							// have already made a purchase from the store

							if ( client.permitsContinue() )
								results.remove( purchases[i] );

							refreshConcoctionsList();
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

				if ( client.permitsContinue() )
					updateDisplay( ENABLED_STATE, "Purchases complete." );
				client.resetContinueState();
			}
		}
	}


	/**
	 * An internal class which represents the panel used for donations to
	 * the statues in the shrine.
	 */

	private class HeroDonationPanel extends LabeledKoLPanel
	{
		private JComboBox heroField;
		private JTextField amountField;

		public HeroDonationPanel()
		{
			super( "Donations to the Greater Good", "lump sum", "increments", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			LockableListModel heroes = new LockableListModel();
			heroes.add( "Statue of Boris" );
			heroes.add( "Statue of Jarlsberg" );
			heroes.add( "Statue of Sneaky Pete" );

			heroField = new JComboBox( heroes );
			amountField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Donate To: ", heroField );
			elements[1] = new VerifiableElement( "Amount: ", amountField );

			setContent( elements, true, true );
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
			(new HeroDonationThread( false )).start();
		}

		protected void actionCancelled()
		{
			contentPanel = heroDonation;
			(new HeroDonationThread( true )).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually donate to the statues.
		 */

		private class HeroDonationThread extends Thread
		{
			private boolean useIncrements;

			public HeroDonationThread( boolean useIncrements )
			{
				super( "Donation-Thread" );
				setDaemon( true );
				this.useIncrements = useIncrements;
			}

			public void run()
			{
				try
				{
					int amountRemaining = getValue( amountField );
					int increments = useIncrements ? df.parse( JOptionPane.showInputDialog(
							"How many increments?" ) ).intValue() : 1;

					if ( increments == 0 )
					{
						updateDisplay( ENABLED_STATE, "Donation cancelled." );
						return;
					}

					if ( heroField.getSelectedIndex() != -1 )
					{
						int eachAmount = amountRemaining / increments;
						int designatedHero = heroField.getSelectedIndex() + 1;

						client.makeRequest( new HeroDonationRequest( client, designatedHero, eachAmount ), increments - 1 );
						amountRemaining -= eachAmount * (increments - 1);

						if ( client.permitsContinue() )
							client.makeRequest( new HeroDonationRequest( client, designatedHero, amountRemaining ), 1 );
					}
				}
				catch ( Exception e )
				{
					// If an exception is caught, that means the
					// person did not input a number.  Which means
					// do nothing, which is exactly what would
					// happen at this point.
				}
			}
		}
	}

	/**
	 * An internal class which represents the panel used for storing and
	 * removing meat from the closet.
	 */

	private class MeatStoragePanel extends LabeledKoLPanel
	{
		private JTextField amountField;

		public MeatStoragePanel()
		{
			super( "Meat Management (Closet)", "deposit", "withdraw", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			amountField = new JTextField();
			inClosetField = new JTextField( df.format( client == null ? 0 : client.getCharacterData().getClosetMeat() ) );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Transaction: ", amountField );
			setContent( elements, true, true );
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
				int amount = getValue( amountField );
				client.makeRequest( new ItemStorageRequest( client, amount, isDeposit ?
					ItemStorageRequest.MEAT_TO_CLOSET : ItemStorageRequest.MEAT_TO_INVENTORY ), 1 );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for removing
	 * effects from the character.
	 */

	private class RemoveEffectsPanel extends LabeledKoLPanel
	{
		private JComboBox effects;

		public RemoveEffectsPanel()
		{
			super( "Uneffective", "uneffect", "kill hermit", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			effects = new JComboBox( client == null ? new LockableListModel() :
				client.getCharacterData().getEffects().getMirrorImage() );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Effects: ", effects );
			setContent( elements, true, true );
		}

		protected void actionConfirmed()
		{
			contentPanel = removeEffects;
			(new RemoveEffectsThread()).start();
		}

		protected void actionCancelled()
		{
			contentPanel = removeEffects;
			updateDisplay( ERROR_STATE, "Unfortunately, you do not have a Valuable Trinket Crossbow." );
		}

		private class RemoveEffectsThread extends Thread
		{
			public RemoveEffectsThread()
			{
				super( "Remove-Effects-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				AdventureResult effect = (AdventureResult) effects.getSelectedItem();

				if ( effect == null )
					return;

				client.makeRequest( new UneffectRequest( client, effect ), 1 );
				boolean isEnabled = client.getInventory().contains( UneffectRequest.REMEDY );
				RemoveEffectsPanel.this.setEnabled( isEnabled );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for adding
	 * effects to a character (yourself or others).
	 */

	private class SkillBuffPanel extends LabeledKoLPanel
	{
		private JComboBox skillSelect;
		private JTextField targetField;
		private JTextField countField;

		public SkillBuffPanel()
		{
			super( "Got Skills?", "cast buff", "maxbuff", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			skillSelect = new JComboBox( client == null ? new LockableListModel() :
				client.getCharacterData().getAvailableSkills() );

			targetField = new JTextField();
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Skill Name: ", skillSelect );
			elements[1] = new VerifiableElement( "The Victim: ", targetField );
			elements[2] = new VerifiableElement( "# of Times: ", countField );
			setContent( elements, true, true );
		}

		protected void actionConfirmed()
		{
			contentPanel = skillBuff;
			(new SkillBuffRequestThread( false )).start();
		}

		protected void actionCancelled()
		{
			contentPanel = skillBuff;
			(new SkillBuffRequestThread( true )).start();
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually do the spellcasting.
		 */

		private class SkillBuffRequestThread extends Thread
		{
			private boolean maxBuff;

			public SkillBuffRequestThread( boolean maxBuff )
			{
				super( "Skill-Buff-Thread" );
				setDaemon( true );
				this.maxBuff = maxBuff;
			}

			public void run()
			{
				String buffName = ((UseSkillRequest) skillSelect.getSelectedItem()).getSkillName();
				if ( buffName == null )
					return;

				String target = targetField.getText().trim();

				int buffCount = maxBuff ?
					(int) ( client.getCharacterData().getCurrentMP() /
						ClassSkillsDatabase.getMPConsumptionByID( ClassSkillsDatabase.getSkillID( buffName ) ) ) : getValue( countField, 1 );

				client.makeRequest( new UseSkillRequest( client, buffName, target, buffCount ), 1 );
			}
		}
	}

	private class ResetSessionListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	client.resetSessionTally();
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for fetching breakfast.
	 */

	private class GetBreakfastListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new GetBreakfastThread()).start();
		}

		private class GetBreakfastThread extends Thread
		{
			public GetBreakfastThread()
			{
				super( "Get-Breakfast-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				client.getBreakfast();
				updateDisplay( ENABLED_STATE, "Breakfast retrieved." );
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
		public void windowOpened( WindowEvent e )
		{
			if ( client != null )
			{
				String framesToReloadSetting = client.getSettings().getProperty( "reloadFrames" );
				if ( framesToReloadSetting == null )
					return;

				KoLFrame currentFrame;
				String [] framesToReload = framesToReloadSetting.split( "," );

				for ( int i = 0; i < framesToReload.length; ++i )
				{
					try
					{
						Class [] fields = new Class[1];
						fields[0] = KoLmafia.class;

						KoLmafia [] parameters = new KoLmafia[1];
						parameters[0] = client;

						currentFrame = (KoLFrame) Class.forName( "net.sourceforge.kolmafia." + framesToReload[i] ).getConstructor( fields ).newInstance( parameters );
						currentFrame.pack();
						currentFrame.setVisible( true );
						currentFrame.setEnabled( isEnabled() );

						existingFrames.add( currentFrame );

					}
					catch ( Exception e1 )
					{
					}
				}
			}
		}

		public void windowClosed( WindowEvent e )
		{
			if ( client != null )
			{
				// Create a new instance of a client, and allow
				// the current client to run down in a separate
				// Thread.

				(new LogoutRequestThread()).start();
				new KoLmafiaGUI();
			}
		}

		private class LogoutRequestThread extends Thread
		{
			public LogoutRequestThread()
			{	setDaemon( true );
			}

			public void run()
			{
				if ( kolchat != null && kolchat.isShowing() )
				{
					kolchat.setVisible( false );
					kolchat.dispose();
				}

				Iterator frames = existingFrames.iterator();
				KoLFrame currentFrame;

				StringBuffer framesToReload = new StringBuffer();
				boolean reloadFrame;

				while ( frames.hasNext() )
				{
					currentFrame = (KoLFrame) frames.next();
					reloadFrame = currentFrame.isShowing();
					currentFrame.setVisible( false );
					currentFrame.dispose();

					if ( reloadFrame && framesToReload.indexOf( currentFrame.getFrameName() ) == -1 )
					{
						if ( framesToReload.length() > 0 )
							framesToReload.append( ',' );
						framesToReload.append( currentFrame.getFrameName() );
					}
				}

				client.getSettings().setProperty( "reloadFrames", framesToReload.toString() );
				client.getSettings().saveSettings();

				statusPane = null;
				gearChanger = null;
				itemManager = null;
				mailboxDisplay = null;
				kolchat = null;

				existingFrames.clear();
				client.deinitialize();
				(new LogoutRequest( client )).run();
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the item manager.
	 */

	private class ViewBuffBotPanelListener extends DisplayFrameListener
	{
		private JTabbedPane advTabs;

		public ViewBuffBotPanelListener()
		{	super( BuffBotFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new BuffBotThread()).start();
		}

		private class BuffBotThread extends DisplayFrameThread
		{
			public void run()
			{
				tabs.setSelectedIndex(0);

				if ( buffbotDisplay != null )
				{
					buffbotDisplay.setVisible( true );
					buffbotDisplay.requestFocus();
					buffbotDisplay.setEnabled( isEnabled );
					client.initializeBuffBot();
				}
				else
				{
					super.run();
					buffbotDisplay = (BuffBotFrame) lastCreatedFrame;
				}
			}
		}
	}

	private class HermitRequestListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new HermitRequestThread()).start();
		}

		private class HermitRequestThread extends Thread
		{
			public HermitRequestThread()
			{
				super( "Hermit-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{	client.makeRequest( new KoLAdventure( client, "hermit.php", "", "The Hermitage" ), 1 );
			}
		}
	}

	private class TrapperRequestListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new TrapperRequestThread()).start();
		}

		private class TrapperRequestThread extends Thread
		{
			public TrapperRequestThread()
			{
				super( "Trapper-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{	client.makeRequest( new KoLAdventure( client, "trapper.php", "", "The 1337 Trapper" ), 1 );
			}
		}
	}

	private class HunterRequestListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new HunterRequestThread()).start();
		}

		private class HunterRequestThread extends Thread
		{
			public HunterRequestThread()
			{
				super( "Hunter-Request-Thread" );
				setDaemon( true );
			}

			public void run()
			{	client.makeRequest( new KoLAdventure( client, "town_wrong.php", "bountyhunter", "The Bounty Hunter" ), 1 );
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
		System.setProperty( "SHARED_MODULE_DIRECTORY", "net/sourceforge/kolmafia/" );
		KoLFrame uitest = new AdventureFrame( null, new LockableListModel(), new LockableListModel() );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}