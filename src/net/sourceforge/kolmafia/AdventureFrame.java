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
	private JTabbedPane tabs;
	private KoLMessenger kolchat;

	private CharsheetFrame statusPane;
	private GearChangeFrame gearChanger;
	private ItemManageFrame itemManager;
	private MailboxFrame mailboxDisplay;

	private AdventureSelectPanel adventureSelect;
	private MallSearchPanel mallSearch;
	private ClanBuffPanel clanBuff;
	private RemoveEffectsPanel removeEffects;
	private SkillBuffPanel skillBuff;
	private HeroDonationPanel heroDonation;
	private MeatStoragePanel meatStorage;

	private JMenuItem statusMenuItem;
	private JMenuItem mailMenuItem;

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
		this.isEnabled = true;
		this.tabs = new JTabbedPane();

		this.adventureSelect = new AdventureSelectPanel( adventureList, resultsTally );
		tabs.addTab( "Adventure Select", adventureSelect );

		this.mallSearch = new MallSearchPanel();
		tabs.addTab( "Mall of Loathing", mallSearch );

		this.clanBuff = new ClanBuffPanel();
		this.heroDonation = new HeroDonationPanel();
		this.meatStorage = new MeatStoragePanel();

		JPanel otherStuffPanel = new JPanel();
		otherStuffPanel.setLayout( new GridLayout( 3, 1 ) );
		otherStuffPanel.add( clanBuff, " " );
		otherStuffPanel.add( heroDonation, " " );
		otherStuffPanel.add( meatStorage, " " );
		tabs.addTab( "Other Activities", otherStuffPanel );

		this.removeEffects = new RemoveEffectsPanel();
		this.skillBuff = new SkillBuffPanel();

		JPanel effectsPanel = new JPanel();
		effectsPanel.setLayout( new BorderLayout( 10, 10 ) );
		effectsPanel.add( removeEffects, BorderLayout.NORTH );
		effectsPanel.add( skillBuff, BorderLayout.CENTER );
		tabs.addTab( "Effects & Buffs", effectsPanel );

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
		this.isEnabled = isEnabled;

		if ( statusMenuItem != null )
			statusMenuItem.setEnabled( isEnabled );

		if ( mailMenuItem != null )
			mailMenuItem.setEnabled( isEnabled );

		for ( int i = 0; i < tabs.getTabCount(); ++i )
			tabs.setEnabledAt( i, isEnabled );

		for ( int i = 0; i < existingFrames.size(); ++i )
		{
			KoLFrame currentFrame = (KoLFrame) existingFrames.get(i);
			if ( currentFrame.isShowing() )
				currentFrame.setEnabled( isEnabled );
		}

		if ( adventureSelect != null && adventureSelect.isShowing() )
			adventureSelect.setEnabled( isEnabled );

		if ( mallSearch != null && mallSearch.isShowing() )
			mallSearch.setEnabled( isEnabled );

		if ( clanBuff != null && clanBuff.isShowing() )
			clanBuff.setEnabled( isEnabled );

		if ( heroDonation != null && heroDonation.isShowing() )
			heroDonation.setEnabled( isEnabled );

		if ( meatStorage != null && meatStorage.isShowing() )
			meatStorage.setEnabled( isEnabled );

		if ( skillBuff != null && skillBuff.isShowing() )
			skillBuff.setEnabled( isEnabled );
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

		JMenu statusMenu = new JMenu( "View" );
		statusMenu.setMnemonic( KeyEvent.VK_V );
		menuBar.add( statusMenu );

		this.statusMenuItem = new JMenuItem( "Status Pane", KeyEvent.VK_S );
		statusMenuItem.addActionListener( new ViewStatusPaneListener() );
		statusMenu.add( statusMenuItem );

		JMenuItem gearMenuItem = new JMenuItem( "Gear Changer", KeyEvent.VK_G );
		gearMenuItem.addActionListener( new ViewGearChangerListener() );

		statusMenu.add( gearMenuItem );

		JMenuItem itemMenuItem = new JMenuItem( "Item Manager", KeyEvent.VK_I );
		itemMenuItem.addActionListener( new ViewItemManagerListener() );

		statusMenu.add( itemMenuItem );

		addScriptMenu( menuBar );

		JMenu peopleMenu = new JMenu( "People" );
		peopleMenu.setMnemonic( KeyEvent.VK_P );
		menuBar.add( peopleMenu );

		JMenuItem chatMenuItem = new JMenuItem( "Chat of Loathing", KeyEvent.VK_C );
		chatMenuItem.addActionListener( new ViewChatListener() );

		peopleMenu.add( chatMenuItem );

		JMenuItem composeMenuItem = new JMenuItem( "Green Composer", KeyEvent.VK_G );
		composeMenuItem.addActionListener( new DisplayFrameListener( GreenMessageFrame.class ) );

		peopleMenu.add( composeMenuItem );

		this.mailMenuItem = new JMenuItem( "IcePenguin Express", KeyEvent.VK_I );
		mailMenuItem.addActionListener( new DisplayFrameListener( MailboxFrame.class ) );

		peopleMenu.add( mailMenuItem );

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
			super( "begin", "stop", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );

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

			String lastAdventure = client.getSettings().getProperty( "lastAdventure" );
			if ( lastAdventure != null )
				for ( int i = 0; i < adventureList.size(); ++i )
					if ( adventureList.get(i).toString().equals( lastAdventure ) )
						locationField.setSelectedItem( adventureList.get(i) );
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
		{	countField.setText( "" );
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

					if ( request != null )
					{
						client.getSettings().setProperty( "lastAdventure", request.toString() );
						client.getSettings().saveSettings();

						client.makeRequest( request, count );

						if ( itemManager != null )
							itemManager.refreshConcoctionsList();
					}
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
			maxPerStoreField.setEnabled( isEnabled );
			resultsDisplay.setEnabled( isEnabled );
		}

		public void clear()
		{
			searchField.setText( "" );
			maxPerStoreField.setText( "" );
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
					int storeCount = countField.getText().trim().length() == 0 ? -1 :
						df.parse( countField.getText() ).intValue();

					if ( storeCount == -1 )
						(new SearchMallRequest( client, searchField.getText(), results )).run();
					else
						(new SearchMallRequest( client, searchField.getText(), storeCount, results )).run();

					if ( results.size() > 0 )
						resultsDisplay.ensureIndexIsVisible( 0 );
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
				Object [] purchases = resultsDisplay.getSelectedValues();

				try
				{
					int maxPerStore = maxPerStoreField.getText().trim().length() == 0 ? Integer.MAX_VALUE :
						df.parse( maxPerStoreField.getText() ).intValue();

					MallPurchaseRequest currentRequest;

					client.resetContinueState();
					for ( int i = 0; i < purchases.length && client.permitsContinue(); ++i )
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

				if ( itemManager != null )
					itemManager.refreshConcoctionsList();

				if ( client.permitsContinue() )
					updateDisplay( ENABLED_STATE, "Purchases complete." );
				client.resetContinueState();
			}
		}
	}

	/**
	 * An internal class which represents the panel used for clan
	 * buffs in the <code>AdventureFrame</code>.
	 */

	private class ClanBuffPanel extends LabeledKoLPanel
	{
		private boolean isBuffing;
		private JComboBox buffField;
		private JTextField countField;

		public ClanBuffPanel()
		{
			super( "Make Your Clan 1335", "purchase buffs", "stop purchases", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );
			this.isBuffing = false;

			buffField = new JComboBox( ClanBuffRequest.getRequestList( client ) );
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Clan Buff: ", buffField );
			elements[1] = new VerifiableElement( "# of times: ", countField );

			setContent( elements );
		}

		public void clear()
		{	countField.setText( "" );
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
				client.cancelRequest();
				updateDisplay( ENABLED_STATE, "Purchase attempts cancelled." );
			}
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
					if ( countField.getText().trim().length() == 0 )
						return;

					int buffCount = df.parse( countField.getText() ).intValue();
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

	private class HeroDonationPanel extends LabeledKoLPanel
	{
		private JComboBox heroField;
		private JTextField amountField;

		public HeroDonationPanel()
		{
			super( "Donations to the Greater Good", "lump sum", "increments", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			LockableListModel heroes = new LockableListModel();
			heroes.add( "Statue of Boris" );
			heroes.add( "Statue of Jarlsberg" );
			heroes.add( "Statue of Sneaky Pete" );
			heroes.add( "Future Heroes of Your Clan" );

			heroField = new JComboBox( heroes );
			amountField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Donate To: ", heroField );
			elements[1] = new VerifiableElement( "Amount: ", amountField );

			setContent( elements );
		}

		public void clear()
		{	amountField.setText( "" );
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
			if ( heroField.getSelectedIndex() == 3 )
				(new ClanDonationThread()).start();
			else
				(new HeroDonationThread( false )).start();
		}

		protected void actionCancelled()
		{
			contentPanel = heroDonation;
			if ( heroField.getSelectedIndex() == 3 )
				(new ClanDonationThread()).start();
			else
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
					if ( amountField.getText().trim().length() == 0 )
						return;

					int amountRemaining = df.parse( amountField.getText() ).intValue();
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
						{
							updateDisplay( DISABLED_STATE, "Request " + increments + " in progress..." );
							(new HeroDonationRequest( client, designatedHero, amountRemaining )).run();

							if ( client.permitsContinue() )
								updateDisplay( KoLFrame.ENABLED_STATE, "Requests complete!" );
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

			}
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually donate to the clan.
		 */

		private class ClanDonationThread extends Thread
		{
			public ClanDonationThread()
			{
				super( "Donation-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					if ( amountField.getText().trim().length() == 0 )
						return;

					int amount = df.parse( amountField.getText() ).intValue();
					(new ItemStorageRequest( client, amount, ItemStorageRequest.MEAT_TO_STASH )).run();
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
	 * An internal class which represents the panel used for storing and
	 * removing meat from the closet.
	 */

	private class MeatStoragePanel extends LabeledKoLPanel
	{
		private JTextField amountField;
		private JTextField inClosetField;

		public MeatStoragePanel()
		{
			super( "Meat Management (Closet)", "put in closet", "take from closet", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			amountField = new JTextField();
			inClosetField = new JTextField( df.format( client == null ? 0 : client.getCharacterData().getClosetMeat() ) );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Transaction: ", amountField );
			elements[1] = new VerifiableElement( "Inside Closet: ", inClosetField );

			setContent( elements );
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			inClosetField.setEnabled( false );
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
					if ( amountField.getText().trim().length() == 0 )
						return;

					int amount = df.parse( amountField.getText() ).intValue();
					(new ItemStorageRequest( client, amount, isDeposit ? ItemStorageRequest.MEAT_TO_CLOSET : ItemStorageRequest.MEAT_TO_INVENTORY )).run();

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
	 * An internal class which represents the panel used for removing
	 * effects from the character.
	 */

	private class RemoveEffectsPanel extends LabeledKoLPanel
	{
		private JComboBox effects;

		public RemoveEffectsPanel()
		{
			super( "Uneffective", "uneffect", "description", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			effects = new JComboBox( client == null ? new LockableListModel() :
				client.getCharacterData().getEffects().getMirrorImage() );

			VerifiableElement [] elements = new VerifiableElement[1];
			elements[0] = new VerifiableElement( "Active Effects: ", effects );
			setContent( elements );
		}

		public void clear()
		{
		}

		protected void actionConfirmed()
		{
			contentPanel = removeEffects;
			(new RemoveEffectsThread()).start();
		}

		protected void actionCancelled()
		{
			contentPanel = removeEffects;
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

				RemoveEffectsPanel.this.setEnabled( false );
				(new UneffectRequest( client, effect )).run();
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
			super( "Got Skills?", "cast buff", "description", new Dimension( 100, 20 ), new Dimension( 200, 20 ) );

			skillSelect = new JComboBox( client == null ? new LockableListModel() :
				client.getCharacterData().getAvailableSkills() );
			targetField = new JTextField();
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Using Skill: ", skillSelect );
			elements[1] = new VerifiableElement( "(Blank is YOU!): ", targetField );
			elements[2] = new VerifiableElement( "# of Times: ", countField );
			setContent( elements );
		}

		public void clear()
		{
		}

		protected void actionConfirmed()
		{
			contentPanel = skillBuff;
			(new SkillBuffRequestThread()).start();
		}

		protected void actionCancelled()
		{
			contentPanel = skillBuff;
		}

		/**
		 * In order to keep the user interface from freezing (or at
		 * least appearing to freeze), this internal class is used
		 * to actually do the spellcasting.
		 */

		private class SkillBuffRequestThread extends Thread
		{
			public SkillBuffRequestThread()
			{
				super( "Skill-Buff-Thread" );
				setDaemon( true );
			}

			public void run()
			{
				try
				{
					String buffName = (String) skillSelect.getSelectedItem();
					if ( buffName == null )
						return;

					String target = targetField.getText();
					if ( countField.getText().trim().length() == 0 )
						return;

					int buffCount = df.parse( countField.getText() ).intValue();
					(new UseSkillRequest( client, buffName, target, buffCount )).run();

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
	 * the request for viewing the status pane.
	 */

	private class ViewStatusPaneListener extends DisplayFrameListener
	{
		public ViewStatusPaneListener()
		{	super( CharsheetFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new ViewStatusPaneThread()).start();
		}

		private class ViewStatusPaneThread extends DisplayFrameThread
		{
			public void run()
			{
				if ( statusPane != null )
				{
					statusPane.setVisible( true );
					statusPane.requestFocus();
					statusPane.setEnabled( isEnabled );

					if ( isEnabled )
						statusPane.refreshStatus();
				}
				else
				{
					super.run();
					statusPane = (CharsheetFrame) lastCreatedFrame;
				}
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the gear changer.
	 */

	private class ViewGearChangerListener extends DisplayFrameListener
	{
		public ViewGearChangerListener()
		{	super( GearChangeFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new ViewGearChangerThread()).start();
		}

		private class ViewGearChangerThread extends DisplayFrameThread
		{
			public void run()
			{
				if ( gearChanger != null )
				{
					gearChanger.setVisible( true );
					gearChanger.requestFocus();
					gearChanger.setEnabled( isEnabled );
				}
				else
				{
					super.run();
					gearChanger = (GearChangeFrame) lastCreatedFrame;
				}
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the item manager.
	 */

	private class ViewItemManagerListener extends DisplayFrameListener
	{
		public ViewItemManagerListener()
		{	super( ItemManageFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new ViewItemManagerThread()).start();
		}

		private class ViewItemManagerThread extends DisplayFrameThread
		{
			public void run()
			{
				if ( itemManager != null )
				{
					itemManager.setVisible( true );
					itemManager.requestFocus();
					itemManager.setEnabled( isEnabled );
				}
				else
				{
					super.run();
					itemManager = (ItemManageFrame) lastCreatedFrame;
				}
			}
		}
	}

	/**
	 * In order to keep the user interface from freezing (or at least
	 * appearing to freeze), this internal class is used to process
	 * the request for viewing the item manager.
	 */

	private class DisplayMailListener extends DisplayFrameListener
	{
		public DisplayMailListener()
		{	super( MailboxFrame.class );
		}

		public void actionPerformed( ActionEvent e )
		{	(new DisplayMailThread()).start();
		}

		private class DisplayMailThread extends DisplayFrameThread
		{
			public void run()
			{
				if ( mailboxDisplay != null )
				{
					mailboxDisplay.setVisible( true );
					mailboxDisplay.requestFocus();
					mailboxDisplay.setEnabled( isEnabled );

					if ( isEnabled )
						mailboxDisplay.refreshMailbox();
				}
				else
				{
					super.run();
					mailboxDisplay = (MailboxFrame) lastCreatedFrame;
				}
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

				updateDisplay( NOCHANGE_STATE, " " );
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
				(new LogoutRequest( client )).run();
				client.deinitialize();

				if ( kolchat != null && kolchat.isShowing() )
				{
					kolchat.setVisible( false );
					kolchat.dispose();
					kolchat = null;
				}

				Iterator frames = existingFrames.iterator();
				KoLFrame currentFrame;
				while ( frames.hasNext() )
				{
					currentFrame = (KoLFrame) frames.next();
					currentFrame.setVisible( false );
					currentFrame.dispose();
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