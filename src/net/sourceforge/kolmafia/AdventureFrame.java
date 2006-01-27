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
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

// containers
import javax.swing.JToolBar;
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
import javax.swing.JOptionPane;

// other imports
import java.util.Date;
import java.text.DecimalFormat;
import java.text.ParseException;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.FileOutputStream;

import net.java.dev.spellcast.utilities.SortedListModel;
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
	private JComboBox resultSelect;
	private JPanel resultPanel;
	private CardLayout resultCards;

	private LockableListModel results;
	private JList resultsList;

	private JTabbedPane tabs;
	private AdventureSelectPanel adventureSelect;
	private MallSearchPanel mallSearch;
	private MeatStoragePanel meatStorage;
	private SkillBuffPanel skillBuff;
	private HeroDonationPanel heroDonation;

	public void dispose()
	{
		resultSelect = null;
		resultPanel = null;
		results = null;
		resultsList = null;

		tabs = null;
		adventureSelect = null;
		mallSearch = null;
		meatStorage = null;
		skillBuff = null;
		heroDonation = null;

		super.dispose();
	}

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 *
	 * @param	client	Client/session associated with this frame
	 */

	public AdventureFrame( KoLmafia client )
	{
		super( client, "Main" );
		this.tabs = new JTabbedPane();

		// Construct the adventure select container
		// to hold everything related to adventuring.

		JPanel adventureContainer = new JPanel( new BorderLayout( 10, 10 ) );

		this.adventureSelect = new AdventureSelectPanel();
		JPanel southPanel = new JPanel( new BorderLayout() );

		resultPanel = new JPanel();
		resultCards = new CardLayout( 0, 0 );
		resultPanel.setLayout( resultCards );
		resultSelect = new JComboBox();

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new AdventureResultsPanel( client == null ? new LockableListModel() : client.getSessionTally() ), "0" );

		resultSelect.addItem( "Conditions Left" );
		resultPanel.add( new AdventureResultsPanel( client == null ? new LockableListModel() : client.getConditions() ), "1" );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new AdventureResultsPanel( KoLCharacter.getEffects() ), "2" );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new AdventureResultsPanel( client == null ? new LockableListModel() : client.getAdventureList() ), "3" );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new AdventureResultsPanel( client == null ? new LockableListModel() : client.getEncounterList() ), "4" );

		resultSelect.addActionListener( new ResultSelectListener() );

		southPanel.add( resultSelect, BorderLayout.NORTH );
		southPanel.add( resultPanel, BorderLayout.CENTER );

		adventureContainer.add( adventureSelect, BorderLayout.NORTH );
		adventureContainer.add( southPanel, BorderLayout.CENTER );

		tabs.addTab( "Adventure", adventureContainer );

		// Construct the store purchasing container
		// to hold everything related to purchases.

		JPanel mallContainer = new JPanel( new BorderLayout( 10, 10 ) );
		this.mallSearch = new MallSearchPanel();

		mallContainer.add( mallSearch, BorderLayout.NORTH );
		mallContainer.add( new SearchResultsPanel(), BorderLayout.CENTER );

		tabs.addTab( "Purchases", mallContainer );

		// Construct the panel which holds other
		// commonly-accessed simple features.

		this.meatStorage = new MeatStoragePanel();
		this.skillBuff = new SkillBuffPanel();
		this.heroDonation = new HeroDonationPanel();

		JPanel otherPanel = new JPanel();
		otherPanel.setLayout( new BoxLayout( otherPanel, BoxLayout.Y_AXIS ) );
		otherPanel.add( meatStorage );
		otherPanel.add( skillBuff );
		otherPanel.add( heroDonation );

		tabs.addTab( "Other Activities", otherPanel );

		// Add the automatic restoration and
		// script customization tab.

		JScrollPane restoreScroller = new JScrollPane( new RestoreOptionsPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( restoreScroller, 560, 400 );
		tabs.addTab( "Auto-Restore", restoreScroller );

		addCompactPane();
		framePanel.add( tabs, BorderLayout.CENTER );

		try
		{
			if ( client != null )
			{
				String holiday = MoonPhaseDatabase.getHoliday( sdf.parse( sdf.format( new Date() ) ) );

				if ( holiday.startsWith( "No" ) )
					client.updateDisplay( NORMAL_STATE, MoonPhaseDatabase.getMoonEffect() );
				else
					client.updateDisplay( NORMAL_STATE, holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
			}
		}
		catch ( Exception e )
		{
			// Should not happen - you're having the parser
			// parse something that it formatted.

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}

		// If the user wishes to add toolbars, go ahead
		// and add the toolbar.

		if ( GLOBAL_SETTINGS.getProperty( "useToolbars" ).equals( "true" ) )
		{
			toolbarPanel.add( new DisplayFrameButton( "Council", "council.gif", CouncilFrame.class ) );
			toolbarPanel.add( new MiniBrowserButton() );
			toolbarPanel.add( new DisplayFrameButton( "Graphical CLI", "command.gif", CommandDisplayFrame.class ) );
			toolbarPanel.add( new InvocationButton( "KoLmafia Chat", "chat.gif", KoLMessenger.class, "initialize" ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Item Manager", "inventory.gif", ItemManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Store Manager", "mall.gif", StoreManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Hagnk's Storage", "hagnk.gif", HagnkStorageFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Clan Manager", "clan.gif", ClanManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Run a Buffbot", "buff.gif", FamiliarTrainingFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Familiar Trainer", "arena.gif", FamiliarTrainingFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Player vs. Player", "flower.gif", FlowerHunterFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", OptionsFrame.class ) );
		}
	}

	private class ResultSelectListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	resultCards.show( resultPanel, String.valueOf( resultSelect.getSelectedIndex() ) );
		}
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	private class AdventureSelectPanel extends KoLPanel
	{
		private JComboBox actionSelect;

		private JComboBox locationSelect;
		private JTextField countField;
		private JTextField conditionField;

		public AdventureSelectPanel()
		{
			super( "begin advs", "win game", "stop all", new Dimension( 100, 20 ), new Dimension( 270, 20 ) );

			actionSelect = new JComboBox( KoLCharacter.getBattleSkillNames() );
			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();

			locationSelect = new JComboBox( adventureList );
			countField = new JTextField();
			conditionField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Location: ", locationSelect );
			elements[1] = new VerifiableElement( "# of turnips: ", countField );
			elements[2] = new VerifiableElement( "Combat Tactic: ", actionSelect );
			elements[3] = new VerifiableElement( "Conditions: ", conditionField );

			setContent( elements );

			int actionIndex = KoLCharacter.getBattleSkillIDs().indexOf( getProperty( "battleAction" ) );

			if ( actionIndex == -1 )
			{
				// The character no longer knows this skill
				setProperty( "battleAction", "attack" );
				actionIndex = 0;
			}

			if ( KoLCharacter.getBattleSkillIDs().size() > 0 )
				actionSelect.setSelectedIndex( actionIndex );

			actionSelect.addActionListener( new BattleActionListener() );

			String lastAdventure = getProperty( "lastAdventure" );

			for ( int i = 0; i < adventureList.size(); ++i )
				if ( adventureList.get(i).toString().equals( lastAdventure ) )
					locationSelect.setSelectedItem( adventureList.get(i) );
		}

		private class BattleActionListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( actionSelect.getSelectedIndex() != -1 )
					setProperty( "battleAction", (String) KoLCharacter.getBattleSkillIDs().get( actionSelect.getSelectedIndex() ) );
			}
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );
			setDefaultButton( confirmedButton );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			locationSelect.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
			conditionField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			// Once the stubs are finished, this will notify the
			// client to begin adventuring based on the values
			// placed in the input fields.

			if ( actionSelect.getSelectedItem() == null )
			{
				client.updateDisplay( ERROR_STATE, "Please select a combat option." );
				return;
			}

			Runnable request = (Runnable) locationSelect.getSelectedItem();
			setProperty( "lastAdventure", request.toString() );

			// If it turns out that you have a null client (you are
			// just demoing), stop here.

			if ( client == null )
				return;

			// If there are conditions in the condition field, be
			// sure to process them.

			if ( conditionField.getText().trim().length() > 0 )
			{
				KoLmafiaCLI conditioner = null;

				try
				{
					conditioner = new KoLmafiaCLI( client, System.in );
				}
				catch ( Exception e )
				{
					// While this should not happen, return from the
					// call in the event that there was an error in
					// the initialization process.

					client.updateDisplay( ERROR_STATE, "Unexpected error." );

					e.printStackTrace( KoLmafia.getLogStream() );
					e.printStackTrace();

					return;
				}

				conditioner.executeLine( "conditions clear" );

				boolean verifyConditions = false;
				boolean useDisjunction = false;

				String [] conditions = conditionField.getText().split( "\\s*,\\s*" );

				for ( int i = 0; i < conditions.length; ++i )
				{
					if ( conditions[i].equals( "check" ) )
					{
						// Postpone verification of conditions
						// until all other conditions added.

						verifyConditions = true;
					}
					else if ( conditions[i].startsWith( "conjunction" ) || conditions[i].startsWith( "disjunction" ) )
					{
						// Postpone mode setting until all of
						// the other conditions are added.

						useDisjunction = conditions[i].startsWith( "disjunction" );
					}
					else
					{
						if ( !conditioner.executeConditionsCommand( "add " + conditions[i] ) )
						{
							client.updateDisplay( ERROR_STATE, "Invalid condition: " + conditions[i] );
							return;
						}
					}
				}

				if ( client.getConditions().isEmpty() )
				{
					client.updateDisplay( NORMAL_STATE, "Conditions already satisfied." );
					return;
				}

				if ( verifyConditions )
				{
					conditioner.executeConditionsCommand( "check" );
					if ( client.getConditions().isEmpty() )
					{
						client.updateDisplay( NORMAL_STATE, "Conditions already satisfied." );
						return;
					}
				}

				conditionField.setText( "" );
				conditioner.executeConditionsCommand( useDisjunction ? "mode disjunction" : "mode conjunction" );
			}

			(new RequestThread( request, getValue( countField ) )).start();
		}

		protected void actionCancelled()
		{
			// Once the stubs are finished, this will notify the
			// client to terminate the loop early.  For now, since
			// there's no actual functionality, simply request focus

			if ( isEnabled )
			{
				(new WinGameThread()).start();
			}
			else
			{
				if ( client.getCurrentRequest() instanceof FightRequest )
					client.updateDisplay( DISABLE_STATE, "Completing combat round..." );
				else
					client.updateDisplay( ERROR_STATE, "Adventuring terminated." );

				client.cancelRequest();
				requestFocus();
			}
		}

		private class WinGameThread extends DaemonThread
		{
			private String [][] WIN_GAME_TEXT = new String [][]
			{
				{
					"Petitioning the Seaside Town Council for automatic game completion...",
					"The Seaside Town Council has rejected your petition.  Game incomplete.",
					"You reject the Seaside Town's decision.  Fighting the council...",
					"You have been defeated by the Seaside Town Council."
				},

				{
					"You enter the super-secret code into the Strange Leaflet...",
					"Your ruby W and heavy D fuse to form the mysterious R!",
					"Moxie sign backdoor accessed.  Supertinkering The Ultimate Weapon...",
					"Supertinkering complete.  Executing tower script...",
					"Your RNG spawns an enraged cow on Floors 1-6."
				},

				{
					"You win the game. What, you were expecting more?",
					"You are now standing in an open field to the west of the Kingdom.",
					"You hear a gurgling ocean to the south, and a path leads north into Valhalla.",
					"What now?"
				},

				{
					"You touch your star starfish!  You surge with power!",
					"Accessing tower backdoor.  Fighting Naughty Sorceress...",
					"Connection timed out during post.  Retrying...",
					"Connection timed out during reply.  Retrying...",
					"Your star power has expired.  You have been defeated!"
				},

				{
					"You raise your metallic A to the sky. Victory is yours!",
					"Original game concept by Jick (Asymmetric Publications).",
					"Co-written by Mr. Skullhead, Riff, and the /dev team.",
					"Special thanks to: the Mods, the Ascension testers, and you.",
					"We present you a new quest, which is basically the same thing, only harder.",
					"Crap!  You've been using KoLmafia so long you can't remember how to play.  Game Over."
				},

				{
					"Executing secret trail script...",
					"Crossing first obstacle, admiring landmarks...",
					"Path set to oxygenarian, familiar pace set to grueling...",
					"You have died from KoLera.  Game Over."
				}
			};

			public void run()
			{
				if ( client != null )
				{
					client.resetContinueState();
					displayMessages( WIN_GAME_TEXT[ RNG.nextInt( WIN_GAME_TEXT.length ) ] );
				}
			}

			private void displayMessages( String [] messages )
			{
				for ( int i = 0; i < messages.length - 1 && client.permitsContinue(); ++i )
				{
					client.updateDisplay( DISABLE_STATE, messages[i] );
					KoLRequest.delay( 3000 );
				}

				if ( client.permitsContinue() )
					client.updateDisplay( ERROR_STATE, messages[ messages.length - 1 ] );
			}
		}

		public void requestFocus()
		{	locationSelect.requestFocus();
		}
	}

	/**
	 * An internal class which represents the panel used for tallying the
	 * results in the <code>AdventureFrame</code>.  Note that all of the
	 * tallying functionality is handled by the <code>LockableListModel</code>
	 * provided, so this functions as a container for that list model.
	 */

	private class AdventureResultsPanel extends JPanel
	{
		public AdventureResultsPanel( LockableListModel resultList )
		{
			setLayout( new BorderLayout() );

			ShowDescriptionList tallyDisplay = new ShowDescriptionList( resultList );
			tallyDisplay.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			tallyDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			tallyDisplay.setVisibleRowCount( 11 );

			add( new JScrollPane( tallyDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
		}
	}

	/**
	 * An internal class which represents the panel used for mall
	 * searches in the <code>AdventureFrame</code>.
	 */

	private class MallSearchPanel extends KoLPanel
	{
		private boolean currentlyBuying;

		private JTextField searchField;
		private JTextField countField;
		private JCheckBox forceSortingCheckBox;

		public MallSearchPanel()
		{
			super( "search", "purchase", "cancel", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );
			setDefaultButton( confirmedButton );

			searchField = new JTextField();
			countField = new JTextField();

			forceSortingCheckBox = new JCheckBox();
			results = new LockableListModel();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Item to Find: ", searchField );
			elements[1] = new VerifiableElement( "Search Limit: ", countField );
			elements[2] = new VerifiableElement( "Force Sort: ", forceSortingCheckBox );

			setContent( elements );

			currentlyBuying = false;
			countField.setText( getProperty( "defaultLimit" ) );
			setDefaultButton( confirmedButton );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			searchField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
			forceSortingCheckBox.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			int searchCount = getValue( countField, -1 );
			setProperty( "defaultLimit", countField.getText() );

			if ( searchCount == -1 )
				searchMall( new SearchMallRequest( client, searchField.getText(), results ) );
			else
				searchMall( new SearchMallRequest( client, searchField.getText(), searchCount, results ) );

			if ( results.size() > 0 )
			{
				resultsList.ensureIndexIsVisible( 0 );
				if ( forceSortingCheckBox.isSelected() )
					java.util.Collections.sort( results );
			}
		}

		protected void actionCancelled()
		{
			if ( currentlyBuying )
			{
				client.cancelRequest();
				return;
			}

			Object [] purchases = resultsList.getSelectedValues();
			if ( purchases == null || purchases.length == 0 )
			{
				client.updateDisplay( ERROR_STATE, "Please select a store from which to purchase." );
				return;
			}

			int defaultPurchases = 0;
			for ( int i = 0; i < purchases.length; ++i )
				defaultPurchases += ((MallPurchaseRequest) purchases[i]).getQuantity() == MallPurchaseRequest.MAX_QUANTITY ?
					MallPurchaseRequest.MAX_QUANTITY : ((MallPurchaseRequest) purchases[i]).getLimit();

			int count = getQuantity( "Maximum number of items to purchase?", defaultPurchases, 1 );

			if ( count == 0 )
				return;

			currentlyBuying = true;
			client.makePurchases( results, purchases, count );
			currentlyBuying = false;

			client.enableDisplay();
		}

		public void requestFocus()
		{	searchField.requestFocus();
		}
	}

	public void searchMall( SearchMallRequest request )
	{
		request.run();
		if ( results != request.getResults() )
			results.addAll( request.getResults() );

		client.updateDisplay( ENABLE_STATE, results.size() == 0 ? "No results found." : "Search complete." );
		tabs.setSelectedIndex(1);
		requestFocus();
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

		return df.format( totalPurchases ) + " " + currentPurchase.getItemName() + " for " + df.format( totalPrice ) + " meat";
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

			resultsList = new JList( results );
			resultsList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			resultsList.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			resultsList.setVisibleRowCount( 11 );

			resultsList.addListSelectionListener( new PurchaseSelectListener() );

			add( new JScrollPane( resultsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER ), BorderLayout.CENTER );
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

				client.updateDisplay( NORMAL_STATE, getPurchaseSummary( resultsList.getSelectedValues() ) );
			}
		}
	}

	/**
	 * This panel allows the user to select how they would like to fight
	 * their battles.  Everything from attacks, attack items, recovery items,
	 * retreat, and battle skill usage will be supported when this panel is
	 * finalized.  For now, however, it only customizes attacks.
	 */

	private class RestoreOptionsPanel extends KoLPanel
	{
		private JComboBox battleStopSelect;
		private JComboBox hpAutoRecoverSelect;
		private JComboBox mpAutoRecoverSelect;
		private JTextField hpRecoveryScriptField;
		private JTextField mpRecoveryScriptField;
		private JTextField betweenBattleScriptField;

		/**
		 * Constructs a new <code>RestoreOptionsPanel</code> containing a
		 * way for the users to choose the way they want to recover their
		 * health and mana inbetween battles encountered during adventuring.
		 */

		public RestoreOptionsPanel()
		{
			super( "save", "reload", new Dimension( 130, 20 ), new Dimension( 260, 20 ) );

			battleStopSelect = new JComboBox();
			battleStopSelect.addItem( "Never stop combat" );
			for ( int i = 1; i <= 9; ++i )
				battleStopSelect.addItem( "Autostop at " + (i*10) + "% HP" );

			// Add in the bewteen-adventures field

			betweenBattleScriptField = new JTextField();

			// All the components of autorecovery

			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover HP" );
			for ( int i = 0; i <= 9; ++i )
				hpAutoRecoverSelect.addItem( "Autorecover HP at " + (i * 10) + "%" );

			mpAutoRecoverSelect = new JComboBox();
			mpAutoRecoverSelect.addItem( "Do not autorecover MP" );
			for ( int i = 0; i <= 9; ++i )
				mpAutoRecoverSelect.addItem( "Autorecover MP at " + (i * 10) + "%" );

			hpRecoveryScriptField = new JTextField();
			mpRecoveryScriptField = new JTextField();

			// Add the elements to the panel

			VerifiableElement [] elements = new VerifiableElement[10];
			elements[0] = new VerifiableElement( "Stop Combat: ", battleStopSelect );
			elements[1] = new VerifiableElement( "Between Battles: ", new ScriptSelectPanel( betweenBattleScriptField ) );

			elements[2] = new VerifiableElement( "", new JLabel() );

			elements[3] = new VerifiableElement( "HP Auto-Recovery: ", hpAutoRecoverSelect );
			elements[4] = new VerifiableElement( "HP Recovery Script: ", new ScriptSelectPanel( hpRecoveryScriptField ) );
			elements[5] = new VerifiableElement( "Use these restores: ", HPRestoreItemList.getDisplay() );

			elements[6] = new VerifiableElement( "", new JLabel() );

			elements[7] = new VerifiableElement( "MP Auto-Recovery: ", mpAutoRecoverSelect );
			elements[8] = new VerifiableElement( "MP Recovery Script: ", new ScriptSelectPanel( mpRecoveryScriptField ) );
			elements[9] = new VerifiableElement( "Use these restores: ", MPRestoreItemList.getDisplay() );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "battleStop", String.valueOf( ((double)(battleStopSelect.getSelectedIndex()) / 10.0) ) );
			setProperty( "betweenBattleScript", betweenBattleScriptField.getText() );

			setProperty( "hpAutoRecover", String.valueOf( ((double)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
			setProperty( "hpRecoveryScript", hpRecoveryScriptField.getText() );
			HPRestoreItemList.setProperty();

			setProperty( "mpAutoRecover", String.valueOf( ((double)(mpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
			setProperty( "mpRecoveryScript", mpRecoveryScriptField.getText() );
			MPRestoreItemList.setProperty();

			JOptionPane.showMessageDialog( null, "Settings have been saved." );
		}

		protected void actionCancelled()
		{
			battleStopSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "battleStop" ) ) * 10) );
			betweenBattleScriptField.setText( getProperty( "betweenBattleScript" ) );

			hpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "hpAutoRecover" ) ) * 10) + 1 );
			hpRecoveryScriptField.setText( getProperty( "hpRecoveryScript" ) );

			mpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "mpAutoRecover" ) ) * 10) + 1 );
			mpRecoveryScriptField.setText( getProperty( "mpRecoveryScript" ) );
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
			if ( heroField.getSelectedIndex() != -1 )
				(new RequestThread( new HeroDonationRequest( client, heroField.getSelectedIndex() + 1, getValue( amountField ) ) )).start();

		}

		protected void actionCancelled()
		{
			try
			{
				int increments = df.parse( JOptionPane.showInputDialog( "How many increments?" ) ).intValue();

				if ( increments == 0 )
				{
					client.updateDisplay( ERROR_STATE, "Donation cancelled." );
					return;
				}

				if ( heroField.getSelectedIndex() != -1 )
				{
					int eachAmount = getValue( amountField ) / increments;
					(new RequestThread( new HeroDonationRequest( client, heroField.getSelectedIndex() + 1, eachAmount ), increments )).start();
				}
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
			}
		}
	}

	/**
	 * An internal class which represents the panel used for storing and
	 * removing meat from the closet.
	 */

	private class MeatStoragePanel extends LabeledKoLPanel
	{
		private JComboBox fundSource;
		private JTextField amountField, closetField;

		public MeatStoragePanel()
		{
			super( "Meat Management", "deposit", "withdraw", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			fundSource = new JComboBox();
			fundSource.addItem( "Inventory / Closet" );
			fundSource.addItem( "Hagnk's Storage" );

			amountField = new JTextField();
			closetField = new JTextField( String.valueOf( KoLCharacter.getClosetMeat() ) );
			closetField.setEnabled( false );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Transfer: ", fundSource );
			elements[1] = new VerifiableElement( "Amount: ", amountField );
			elements[2] = new VerifiableElement( "In Closet: ", closetField );
			setContent( elements, true, true );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new ClosetUpdater() ) );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			fundSource.setEnabled( isEnabled );
			amountField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			switch ( fundSource.getSelectedIndex() )
			{
				case 0:
					(new RequestThread( new ItemStorageRequest( client, getValue( amountField ), ItemStorageRequest.MEAT_TO_CLOSET ) )).start();
					return;

				case 1:
					client.updateDisplay( ERROR_STATE, "You cannot deposit into Hagnk's storage." );
					return;
			}
		}

		protected void actionCancelled()
		{
			switch ( fundSource.getSelectedIndex() )
			{
				case 0:
					(new RequestThread( new ItemStorageRequest( client, getValue( amountField ), ItemStorageRequest.MEAT_TO_INVENTORY ) )).start();
					return;

				case 1:
					(new RequestThread( new ItemStorageRequest( client, getValue( amountField ), ItemStorageRequest.PULL_MEAT_FROM_STORAGE ) )).start();
					return;
			}
		}

		private class ClosetUpdater implements Runnable
		{
			public void run()
			{	closetField.setText( String.valueOf( KoLCharacter.getClosetMeat() ) );
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{	(new CreateFrameRunnable( AdventureFrame.class )).run();
	}
}
