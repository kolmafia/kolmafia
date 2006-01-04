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
	private static final Color ERROR_COLOR = new Color( 255, 128, 128 );
	private static final Color ENABLED_COLOR = new Color( 128, 255, 128 );
	private static final Color DISABLED_COLOR = null;

	private JTabbedPane tabs;
	private AdventureSelectPanel adventureSelect;
	private MallSearchPanel mallSearch;
	private MeatStoragePanel meatStorage;
	private SkillBuffPanel skillBuff;
	private HeroDonationPanel heroDonation;

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

		this.isEnabled = true;
		this.tabs = new JTabbedPane();

		this.adventureSelect = new AdventureSelectPanel();
		this.mallSearch = new MallSearchPanel();
		this.meatStorage = new MeatStoragePanel();
		this.skillBuff = new SkillBuffPanel();
		this.heroDonation = new HeroDonationPanel();

		tabs.addTab( "Adventure", adventureSelect );
		tabs.addTab( "Purchases", mallSearch );

		JPanel otherPanel = new JPanel();
		otherPanel.setLayout( new BoxLayout( otherPanel, BoxLayout.Y_AXIS ) );
		otherPanel.add( meatStorage );
		otherPanel.add( skillBuff );
		otherPanel.add( heroDonation );
		tabs.addTab( "Other Activities", otherPanel );

		JScrollPane restoreScroller = new JScrollPane( new RestoreOptionsPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( restoreScroller, 560, 400 );
		tabs.addTab( "Auto-Restore", restoreScroller );

		JScrollPane choiceScroller = new JScrollPane( new ChoiceOptionsPanel(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( choiceScroller, 560, 400 );
		tabs.addTab( "Choice Handler", choiceScroller );

		addCompactPane();
		framePanel.add( tabs, BorderLayout.CENTER );
		contentPanel = adventureSelect;

		try
		{
			String holiday = MoonPhaseDatabase.getHoliday( sdf.parse( sdf.format( new Date() ) ) );

			if ( holiday.startsWith( "No" ) )
				client.updateDisplay( NORMAL_STATE, MoonPhaseDatabase.getMoonEffect() );
			else
				client.updateDisplay( NORMAL_STATE, holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
		}
		catch ( Exception e )
		{
			// Should not happen - you're having the parser
			// parse something that it formatted.
		}

		// If the user wishes to add toolbars, go ahead
		// and add the toolbar.

		if ( GLOBAL_SETTINGS.getProperty( "useToolbars" ).equals( "true" ) )
		{
			toolbarPanel.add( new DisplayFrameButton( "Council", "council.gif", CouncilFrame.class ) );
			toolbarPanel.add( new MiniBrowserButton() );
			toolbarPanel.add( new DisplayFrameButton( "Graphical CLI", "command.gif", CommandDisplayFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Mail", "mail.gif", MailboxFrame.class ) );
			toolbarPanel.add( new InvocationButton( "Chat", "chat.gif", KoLMessenger.class, "initialize" ) );
			toolbarPanel.add( new DisplayFrameButton( "Clan", "clan.gif", ClanManageFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Item Manager", "inventory.gif", ItemManageFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Equipment", "equipment.gif", GearChangeFrame.class ) );
			toolbarPanel.add( new KoLPanelFrameButton( "Cast a Buff", "buff.gif", new SkillBuffPanel() ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Familiar Trainer", "arena.gif", FamiliarTrainingFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "Mushroom Plot", "mushroom.gif", MushroomFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "KoL Almanac", "calendar.gif", CalendarFrame.class ) );
			toolbarPanel.add( new DisplayFrameButton( "KoL Encyclopedia", "encyclopedia.gif", ExamineItemsFrame.class ) );

			toolbarPanel.add( new JToolBar.Separator() );

			toolbarPanel.add( new DisplayFrameButton( "Preferences", "preferences.gif", OptionsFrame.class ) );
		}
	}

	public void updateDisplay( int displayState, String message )
	{
		super.updateDisplay( displayState, message );

		if ( contentPanel != adventureSelect )
			adventureSelect.setStatusMessage( displayState, message );
	}

	/**
	 * Auxiliary method used to enable and disable a frame.  By default,
	 * this attempts to toggle the enable/disable status on all tabs
	 * and the view menu item, as well as the item manager if it's
	 * currently visible.
	 *
	 * @param	isEnabled	<code>true</code> if the frame is to be re-enabled
	 */

	public void setEnabled( boolean isEnabled )
	{
		this.isEnabled = isEnabled && (client == null || !BuffBotHome.isBuffBotActive());

		if ( heroDonation != null )
		{
			adventureSelect.setEnabled( this.isEnabled );
			mallSearch.setEnabled( this.isEnabled );
			meatStorage.setEnabled( this.isEnabled );
			skillBuff.setEnabled( this.isEnabled );
			heroDonation.setEnabled( this.isEnabled );
		}
	}

	private class StatusLabel extends JLabel
	{
		public StatusLabel()
		{	super( " ", JLabel.CENTER );
		}

		public void setStatusMessage( int displayState, String s )
		{
			String label = getText();

			// If the current text or the string you're using is
			// null, then do nothing.

			if ( s == null || label == null )
				return;

			// If you're not attempting to time-in the session, but
			// the session has timed out, then ignore all changes
			// to the attempt to time-in the session.

			if ( label.equals( "Session timed out." ) || label.equals( "Nightly maintenance." ) )
				if ( client.inLoginState() && !s.equals( "Timing in session..." ) && (displayState == NORMAL_STATE || displayState == DISABLE_STATE) )
					return;

			// If the string which you're trying to set is blank,
			// then you don't have to update the status message.

			if ( !s.equals( "" ) )
				setText( s );

			// Now, change the background of the frame based on
			// the current display state -- but only if the
			// compact pane has already been constructed.

			if ( compactPane != null )
			{
				switch ( displayState )
				{
					case ERROR_STATE:
						compactPane.setBackground( ERROR_COLOR );
						break;
					case ENABLE_STATE:
						compactPane.setBackground( ENABLED_COLOR );
						break;
					case DISABLE_STATE:
						compactPane.setBackground( DISABLED_COLOR );
						break;
				}
			}
		}
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	private class AdventureSelectPanel extends KoLPanel
	{
		private JComboBox actionSelect;

		private JPanel actionStatusPanel;
		private StatusLabel actionStatusLabel;

		private JComboBox locationSelect;
		private JTextField countField;
		private JTextField conditionField;

		private JComboBox resultSelect;
		private JPanel resultPanel;
		private CardLayout resultCards;

		public AdventureSelectPanel()
		{
			super( "begin advs", "win game", "stop all", new Dimension( 100, 20 ), new Dimension( 270, 20 ) );

			actionSelect = new JComboBox( KoLCharacter.getBattleSkillNames() );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new StatusLabel();
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

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

			String lastAdventure = getProperty( "lastAdventure" );

			for ( int i = 0; i < adventureList.size(); ++i )
				if ( adventureList.get(i).toString().equals( lastAdventure ) )
					locationSelect.setSelectedItem( adventureList.get(i) );
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout( 10, 10 ) );
			centerPanel.add( actionStatusPanel, BorderLayout.NORTH );

			JPanel southPanel = new JPanel();
			southPanel.setLayout( new BorderLayout() );

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

			centerPanel.add( southPanel, BorderLayout.CENTER );
			add( centerPanel, BorderLayout.CENTER );
			setDefaultButton( confirmedButton );
		}

		private class ResultSelectListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{	resultCards.show( resultPanel, String.valueOf( resultSelect.getSelectedIndex() ) );
			}
		}

		public void setStatusMessage( int displayState, String s )
		{	actionStatusLabel.setStatusMessage( displayState, s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			locationSelect.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
			actionSelect.setEnabled( isEnabled );
			conditionField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			// Once the stubs are finished, this will notify the
			// client to begin adventuring based on the values
			// placed in the input fields.

			contentPanel = this;

			if ( actionSelect.getSelectedItem() == null )
			{
				client.updateDisplay( ERROR_STATE, "Please select a combat option." );
				return;
			}

			setProperty( "battleAction", (String) KoLCharacter.getBattleSkillIDs().get( actionSelect.getSelectedIndex() ) );
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

			contentPanel = this;

			if ( confirmedButton.isEnabled() )
				(new WinGameThread()).start();
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
	}

	/**
	 * An internal class which represents the panel used for mall
	 * searches in the <code>AdventureFrame</code>.
	 */

	private class MallSearchPanel extends KoLPanel
	{
		private boolean currentlyBuying;

		private JPanel actionStatusPanel;
		private StatusLabel actionStatusLabel;

		private JTextField searchField;
		private JTextField countField;

		private JCheckBox limitPurchasesCheckBox;
		private JCheckBox forceSortingCheckBox;

		private LockableListModel results;
		private JList resultsList;

		public MallSearchPanel()
		{
			super( "search", "purchase", "cancel", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );
			setDefaultButton( confirmedButton );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new StatusLabel();
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			searchField = new JTextField();
			countField = new JTextField();

			limitPurchasesCheckBox = new JCheckBox();
			limitPurchasesCheckBox.setSelected( true );
			forceSortingCheckBox = new JCheckBox();
			results = new LockableListModel();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Item to Find: ", searchField );
			elements[1] = new VerifiableElement( "Search Limit: ", countField );
			elements[2] = new VerifiableElement( "Buy Limit: ", limitPurchasesCheckBox );
			elements[3] = new VerifiableElement( "Force Sort: ", forceSortingCheckBox );

			setContent( elements );
			currentlyBuying = false;
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements, null, null, true, true );
			countField.setText( getProperty( "defaultLimit" ) );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout( 10, 10 ) );
			centerPanel.add( actionStatusPanel, BorderLayout.NORTH );
			centerPanel.add( new SearchResultsPanel(), BorderLayout.CENTER );
			add( centerPanel, BorderLayout.CENTER );
			setDefaultButton( confirmedButton );
		}

		public void setStatusMessage( int displayState, String s )
		{	actionStatusLabel.setStatusMessage( displayState, s );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			searchField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );

			limitPurchasesCheckBox.setEnabled( isEnabled );
			forceSortingCheckBox.setEnabled( isEnabled );

			resultsList.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			contentPanel = this;

			int searchCount = getValue( countField, -1 );
			setProperty( "defaultLimit", countField.getText() );

			if ( searchCount == -1 )
				(new SearchMallRequest( client, searchField.getText(), results )).run();
			else
				(new SearchMallRequest( client, searchField.getText(), searchCount, results )).run();

			if ( results.size() > 0 )
			{
				resultsList.ensureIndexIsVisible( 0 );
				if ( forceSortingCheckBox.isSelected() )
					java.util.Collections.sort( results );
			}

			client.enableDisplay();
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

			contentPanel = this;

			int count = limitPurchasesCheckBox.isSelected() ?
				getQuantity( "Maximum number of items to purchase?", Integer.MAX_VALUE, 1 ) : Integer.MAX_VALUE;

			if ( count == 0 )
				return;

			currentlyBuying = true;
			client.makePurchases( results, purchases, count );
			currentlyBuying = false;

			client.enableDisplay();
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

					setStatusMessage( NORMAL_STATE, getPurchaseSummary( resultsList.getSelectedValues() ) );
				}
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
			super( new Dimension( 130, 20 ), new Dimension( 260, 20 ) );

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
	 * This panel allows the user to select which item they would like
	 * to do for each of the different choice adventures.
	 */

	private class ChoiceOptionsPanel extends KoLPanel
	{
		private JComboBox [] optionSelects;
		private JComboBox castleWheelSelect;

		/**
		 * Constructs a new <code>ChoiceOptionsPanel</code>.
		 */

		public ChoiceOptionsPanel()
		{
			super( new Dimension( 130, 20 ), new Dimension( 260, 20 ) );

			optionSelects = new JComboBox[ AdventureDatabase.CHOICE_ADVS.length ];
			for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
			{
				optionSelects[i] = new JComboBox();

				boolean ignorable = AdventureDatabase.ignoreChoiceOption( AdventureDatabase.CHOICE_ADVS[i][0][0] ) != null;
				optionSelects[i].addItem( ignorable ?
										  "Ignore this adventure" :
										  "Can't ignore this adventure" );

				for ( int j = 0; j < AdventureDatabase.CHOICE_ADVS[i][2].length; ++j )
					optionSelects[i].addItem( AdventureDatabase.CHOICE_ADVS[i][2][j] );
			}

			castleWheelSelect = new JComboBox();
			castleWheelSelect.addItem( "Turn to map quest position" );
			castleWheelSelect.addItem( "Turn to muscle position" );
			castleWheelSelect.addItem( "Turn to mysticality position" );
			castleWheelSelect.addItem( "Turn to moxie position" );
			castleWheelSelect.addItem( "Ignore this adventure" );

			VerifiableElement [] elements = new VerifiableElement[ optionSelects.length + 1 ];
			elements[0] = new VerifiableElement( "Castle Wheel", castleWheelSelect );

			for ( int i = 1; i < elements.length; ++i )
				elements[i] = new VerifiableElement( AdventureDatabase.CHOICE_ADVS[i-1][1][0], optionSelects[i-1] );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "luckySewerAdventure", (String) optionSelects[0].getSelectedItem() );
			for ( int i = 1; i < optionSelects.length; ++i )
			{
				int index = optionSelects[i].getSelectedIndex();
				String choice = AdventureDatabase.CHOICE_ADVS[i][0][0];
				boolean ignorable = AdventureDatabase.ignoreChoiceOption( choice ) != null;

				if ( ignorable || index != 0 )
					setProperty( choice, String.valueOf( index ) );
				else
					optionSelects[i].setSelectedIndex( Integer.parseInt( getProperty( choice ) ) );
			}

			//              The Wheel:

			//              Muscle
			// Moxie          +         Mysticality
			//            Map Quest

			// Option 1: Turn the wheel counterclockwise
			// Option 2: Turn the wheel clockwise
			// Option 3: Leave the wheel alone

			switch ( castleWheelSelect.getSelectedIndex() )
			{
				case 0: // Map quest position (choice adventure 11)
					setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
					setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
					setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
					setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
					break;

				case 1: // Muscle position (choice adventure 9)
					setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
					setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
					setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
					setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
					break;

				case 2: // Mysticality position (choice adventure 10)
					setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
					setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
					setProperty( "choiceAdventure11", "2" );  // Turn the map quest position counterclockwise
					setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
					break;

				case 3: // Moxie position (choice adventure 12)
					setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
					setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
					setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
					setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
					break;

				case 4: // Ignore this adventure
					setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
					setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
					setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
					setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
					break;
			}
		}

		protected void actionCancelled()
		{
			optionSelects[0].setSelectedItem( getProperty( "luckySewerAdventure" ) );
			for ( int i = 1; i < optionSelects.length; ++i )
				optionSelects[i].setSelectedIndex( Integer.parseInt( getProperty( AdventureDatabase.CHOICE_ADVS[i][0][0] ) ) );

			// Determine the desired wheel position by examining
			// which choice adventure has the "3" value.  If none
			// exists, assume the user wishes to turn it to the map
			// quest.

			// If they are all "3", user wants the wheel left alone

			int option = 11;
			int count = 0;
			for ( int i = 9; i < 13; ++i )
				if ( getProperty( "choiceAdventure" + i ).equals( "3" ) )
				{
					option = i;
					count++;
				}

			switch ( count )
			{
				default:	// Bogus saved options
				case 0:		// Map quest position
					castleWheelSelect.setSelectedIndex(0);
					break;

				case 1:		// One chosen target
					switch ( option )
					{
					case 9: // Muscle position
						castleWheelSelect.setSelectedIndex(1);
						break;

					case 10: // Mysticality position
						castleWheelSelect.setSelectedIndex(2);
						break;

					case 11: // Map quest position
						castleWheelSelect.setSelectedIndex(0);
						break;

					case 12: // Moxie position
						castleWheelSelect.setSelectedIndex(3);
						break;
					}
					break;

				case 4:		// Ignore this adventure
					castleWheelSelect.setSelectedIndex(4);
					break;
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
			contentPanel = this;
			if ( heroField.getSelectedIndex() != -1 )
				(new RequestThread( new HeroDonationRequest( client, heroField.getSelectedIndex() + 1, getValue( amountField ) ) )).start();

		}

		protected void actionCancelled()
		{
			try
			{
				contentPanel = this;
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

			KoLCharacter.addKoLCharacterListener( new KoLCharacterAdapter( new ClosetUpdater() ) );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			fundSource.setEnabled( isEnabled );
			amountField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			contentPanel = this;
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
			contentPanel = this;
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
