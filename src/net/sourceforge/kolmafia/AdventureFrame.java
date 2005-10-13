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
import javax.swing.JSeparator;

// other imports
import java.util.Iterator;
import java.text.DecimalFormat;
import java.text.ParseException;
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
		super( client, KoLCharacter.getUsername() + " (" + KoLRequest.getRootHostName() + ")" );

		this.isEnabled = true;
		this.tabs = new JTabbedPane();

		this.adventureSelect = new AdventureSelectPanel();
		this.mallSearch = new MallSearchPanel();
		this.meatStorage = new MeatStoragePanel();
		this.skillBuff = new SkillBuffPanel();
		this.heroDonation = new HeroDonationPanel();

		tabs.addTab( "Adventure Select", adventureSelect );
		tabs.addTab( "Mall of Loathing", mallSearch );

		JPanel otherPanel = new JPanel();
		otherPanel.setLayout( new BoxLayout( otherPanel, BoxLayout.Y_AXIS ) );
		otherPanel.add( meatStorage );
		otherPanel.add( skillBuff );
		otherPanel.add( heroDonation );
		tabs.addTab( "Other Activities", otherPanel );

		addCompactPane();
		getContentPane().add( tabs, BorderLayout.CENTER );
		contentPanel = adventureSelect;

		addWindowListener( new LogoutRequestAdapter() );
		updateDisplay( ENABLED_STATE, MoonPhaseDatabase.getMoonEffect() );

		addMenuBar();
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

		Object [] frames = existingFrames.toArray();

		for ( int i = 0; i < frames.length; ++i )
			if ( frames[i] != this )
				((KoLFrame) frames[i]).setEnabled( isEnabled );
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

		addStatusMenu( menuBar );

		addTravelMenu( menuBar );
		addPeopleMenu( menuBar );
		addScriptMenu( menuBar );

		JMenu optionsMenu = addOptionsMenu( menuBar );
		optionsMenu.add( new InvocationMenuItem( "Clear Results", KeyEvent.VK_C, client, "resetSessionTally" ) );
		optionsMenu.add( new InvocationMenuItem( "Session Time-In", KeyEvent.VK_S, client, "executeTimeInRequest" ) );

		addHelpMenu( menuBar );
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	private class AdventureSelectPanel extends KoLPanel
	{
		private LockableListModel actions;
		private LockableListModel actionNames;
		private JComboBox actionSelect;

		private JPanel actionStatusPanel;
		private JLabel actionStatusLabel;

		private JComboBox locationSelect;
		private JTextField countField;
		private JTextField conditionField;

		private JComboBox resultSelect;
		private JPanel resultPanel;
		private CardLayout resultCards;

		public AdventureSelectPanel()
		{
			super( "begin advs", "win game", "stop all", new Dimension( 100, 20 ), new Dimension( 270, 20 ) );

			actions = new LockableListModel();
			actionNames = new LockableListModel();

			actions.add( "attack" );  actionNames.add( "Normal: Attack with Weapon" );

			// Add in moxious maneuver if the player
			// is of the appropriate class.

			if ( KoLCharacter.getClassType().startsWith( "Ac" ) || KoLCharacter.getClassType().startsWith( "Di" ) )
			{
				actions.add( "moxman" );
				actionNames.add( "Skill: Moxious Maneuver" );
			}

			// Add in dictionary regardless of whether
			// or not the player has a dictionary in
			// their inventory.  Just ignore the option
			// if the person turns out not to have one.

			actions.add( "item0536" );
			actionNames.add( "Item: Use a Dictionary" );

			// Add in all of the player's combat skills;
			// parse the skill list to find out.

			Iterator skills = KoLCharacter.getAvailableSkills().iterator();

			int currentSkillID;
			UseSkillRequest currentSkill;

			while ( skills.hasNext() )
			{
				currentSkill = (UseSkillRequest) skills.next();
				currentSkillID = ClassSkillsDatabase.getSkillID( currentSkill.getSkillName() );

				if ( ClassSkillsDatabase.isCombat( currentSkillID ) )
				{
					actions.add( String.valueOf( currentSkillID ) );
					actionNames.add( "Skill: " + currentSkill.getSkillName() );
				}
			}

			actionNames.setSelectedIndex( actions.indexOf( getProperty( "battleAction" ) ) );
			actionSelect = new JComboBox( actionNames );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
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

			resultPanel.add( new AdventureResultsPanel( client == null ? new LockableListModel() : client.getSessionTally() ), "0" );
			resultPanel.add( new AdventureResultsPanel( client == null ? new LockableListModel() : client.getConditions() ), "1" );
			resultPanel.add( new AdventureResultsPanel( KoLCharacter.getEffects() ), "2" );

			resultSelect = new JComboBox();
			resultSelect.addItem( "Session Results" );
			resultSelect.addItem( "Conditions Left" );
			resultSelect.addItem( "Active Effects" );

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
		{
			String label = actionStatusLabel.getText();
			if ( !s.equals( "Timing in session..." ) && (label.equals( "Session timed out." ) || label.equals( "Nightly maintenance." ) ))
				return;

			if ( !s.equals( "" ) )
				actionStatusLabel.setText( s );

			switch ( displayState )
			{
				case ERROR_STATE:
				case CANCELLED_STATE:
					compactPane.setBackground( ERROR_COLOR );
					break;
				case ENABLED_STATE:
					compactPane.setBackground( ENABLED_COLOR );
					break;
				case DISABLED_STATE:
					compactPane.setBackground( DISABLED_COLOR );
					break;
			}
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

			if ( actionNames.getSelectedIndex() < 0 || actions.getSelectedIndex() >= actions.size() )
				actions.setSelectedIndex( 0 );

			if ( actions.get( actionNames.getSelectedIndex() ).equals( "item0536" ) && FightRequest.DICTIONARY.getCount( KoLCharacter.getInventory() ) < 1 )
			{
				updateDisplay( ERROR_STATE, "Sorry, you don't have a dictionary." );
				return;
			}

			setProperty( "battleAction", (String) actions.get( actionNames.getSelectedIndex() ) );
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

					updateDisplay( ERROR_STATE, "Unexpected error." );
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
					client.updateDisplay( ENABLED_STATE, "Conditions already satisfied." );
					return;
				}

				if ( verifyConditions )
				{
					conditioner.executeConditionsCommand( "check" );
					if ( client.getConditions().isEmpty() )
					{
						client.updateDisplay( ENABLED_STATE, "Conditions already satisfied." );
						return;
					}
				}

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
				client.updateDisplay( CANCELLED_STATE, "Adventuring terminated." );
				client.cancelRequest();
				requestFocus();
			}
		}

		private class WinGameThread extends DaemonThread
		{
			public void run()
			{
				if ( client != null )
					client.resetContinueState();

				switch ( RNG.nextInt(2) )
				{
					case 0:
						fightCouncil();
						break;

					case 1:
						createWeapon();
						break;
				}

			}

			private void fightCouncil()
			{
				updateDisplay( DISABLED_STATE, "Petitioning the Seaside Town Council for automatic game completion..." );
				updateDisplay( DISABLED_STATE, "The Seaside Town Council has rejected your petition.  Game incomplete." );
				updateDisplay( DISABLED_STATE, "You reject the Seaside Town's decision.  Fighting the council..." );
				updateDisplay( ERROR_STATE, "You have been defeated by the Seaside Town Council.  Game Over." );
			}

			private void createWeapon()
			{
				updateDisplay( DISABLED_STATE, "You enter the super-secret code into the Strange Leaflet..." );
				updateDisplay( DISABLED_STATE, "Your ruby W and metallic A fuse to form the mysterious R!" );
				updateDisplay( DISABLED_STATE, "Moxie sign backdoor accessed.  Supertinkering The Ultimate Weapon..." );
				updateDisplay( DISABLED_STATE, "Supertinkering complete.  Executing tower script..." );
				updateDisplay( ERROR_STATE, "Your RNG spawns an enraged cow on Floors 1-6.  Game Over." );
			}

			private void updateDisplay( int state, String message )
			{
				if ( client == null || client.permitsContinue() )
				{
					AdventureFrame.this.updateDisplay( state, message );
					KoLRequest.delay( 3000 );
				}
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
		private JLabel actionStatusLabel;

		private JTextField searchField;
		private JTextField countField;

		private JCheckBox limitPurchasesCheckBox;
		private JCheckBox forceSortingCheckBox;

		private LockableListModel results;
		private JList resultsDisplay;

		public MallSearchPanel()
		{
			super( "search", "purchase", "cancel", new Dimension( 100, 20 ), new Dimension( 250, 20 ) );
			setDefaultButton( confirmedButton );

			actionStatusPanel = new JPanel();
			actionStatusPanel.setLayout( new GridLayout( 2, 1 ) );

			actionStatusLabel = new JLabel( " ", JLabel.CENTER );
			actionStatusPanel.add( actionStatusLabel );
			actionStatusPanel.add( new JLabel( " ", JLabel.CENTER ) );

			searchField = new JTextField();
			countField = new JTextField();

			limitPurchasesCheckBox = new JCheckBox();
			forceSortingCheckBox = new JCheckBox();

			results = new LockableListModel();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Item to Find: ", searchField );
			elements[1] = new VerifiableElement( "Search Limit: ", countField );
			elements[2] = new VerifiableElement( "Limit Purchases: ", limitPurchasesCheckBox );
			elements[3] = new VerifiableElement( "Force Price Sort: ", forceSortingCheckBox );

			setContent( elements );
			currentlyBuying = false;
		}

		protected void setContent( VerifiableElement [] elements )
		{
			super.setContent( elements, null, null, null, true, true );
			countField.setText( getProperty( "defaultLimit" ) );

			JPanel centerPanel = new JPanel();
			centerPanel.setLayout( new BorderLayout( 10, 10 ) );
			centerPanel.add( actionStatusPanel, BorderLayout.NORTH );
			centerPanel.add( new SearchResultsPanel(), BorderLayout.CENTER );
			add( centerPanel, BorderLayout.CENTER );
			setDefaultButton( confirmedButton );
		}

		public void setStatusMessage( int displayState, String s )
		{
			String label = actionStatusLabel.getText();
			if ( !client.inLoginState() && (label.equals( "Session timed out." ) || label.equals( "Nightly maintenance." )) )
				return;

			actionStatusLabel.setText( s );
			switch ( displayState )
			{
				case ERROR_STATE:
				case CANCELLED_STATE:
					compactPane.setBackground( ERROR_COLOR );
					break;
				case ENABLED_STATE:
					compactPane.setBackground( ENABLED_COLOR );
					break;
				case DISABLED_STATE:
					compactPane.setBackground( DISABLED_COLOR );
					break;
			}
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			searchField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );

			limitPurchasesCheckBox.setEnabled( isEnabled );
			forceSortingCheckBox.setEnabled( isEnabled );

			resultsDisplay.setEnabled( isEnabled );
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
				resultsDisplay.ensureIndexIsVisible( 0 );
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

			Object [] purchases = resultsDisplay.getSelectedValues();
			if ( purchases.length == 0 )
			{
				client.updateDisplay( ERROR_STATE, "Please select a store from which to purchase." );
				return;
			}

			contentPanel = this;
			currentlyBuying = true;
			client.makePurchases( results, purchases, limitPurchasesCheckBox.isSelected() ?
				getQuantity( "Maximum number of items to purchase?", Integer.MAX_VALUE, 1 ) : Integer.MAX_VALUE );

			currentlyBuying = false;
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
			super( "Donations to the Greater Good", "to one", "to all", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

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
			contentPanel = this;
			HeroDonationRequest [] requests = new HeroDonationRequest[3];

			for ( int i = 0; i < 3; ++i )
				requests[i] = new HeroDonationRequest( client, i, getValue( amountField ) );

			(new RequestThread( requests )).start();
		}
	}

	/**
	 * An internal class which represents the panel used for storing and
	 * removing meat from the closet.
	 */

	private class MeatStoragePanel extends LabeledKoLPanel
	{
		private JComboBox fundSource;
		private JTextField amountField;

		public MeatStoragePanel()
		{
			super( "Meat Management", "deposit", "withdraw", new Dimension( 80, 20 ), new Dimension( 240, 20 ) );

			fundSource = new JComboBox();
			fundSource.addItem( "Inventory / Closet" );
			fundSource.addItem( "Hagnk's Storage" );

			amountField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Transfer: ", fundSource );
			elements[1] = new VerifiableElement( "Amount: ", amountField );
			setContent( elements, true, true );
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
					contentPanel = this;
					(new RequestThread( new ItemStorageRequest( client, getValue( amountField ), ItemStorageRequest.MEAT_TO_CLOSET ) )).start();
					return;

				case 1:
					contentPanel = this;
					client.updateDisplay( ERROR_STATE, "You cannot deposit into Hagnk's storage." );
					return;
			}
		}

		protected void actionCancelled()
		{
			switch ( fundSource.getSelectedIndex() )
			{
				case 0:
					contentPanel = this;
					(new RequestThread( new ItemStorageRequest( client, getValue( amountField ), ItemStorageRequest.MEAT_TO_INVENTORY ) )).start();
					return;

				case 1:
					contentPanel = this;
					(new RequestThread( new ItemStorageRequest( client, getValue( amountField ), ItemStorageRequest.PULL_MEAT_FROM_STORAGE ) )).start();
					return;
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

			skillSelect = new JComboBox( client == null ? new LockableListModel() : KoLCharacter.getUsableSkills() );

			targetField = new JTextField();
			countField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Skill Name: ", skillSelect );
			elements[1] = new VerifiableElement( "The Victim: ", targetField );
			elements[2] = new VerifiableElement( "# of Times: ", countField );
			setContent( elements, true, true );
		}

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			skillSelect.setEnabled( isEnabled );
			targetField.setEnabled( isEnabled );
			countField.setEnabled( isEnabled );
		}

		protected void actionConfirmed()
		{
			contentPanel = this;
			buff( false );
		}

		protected void actionCancelled()
		{
			contentPanel = this;
			buff( true );
		}

		private void buff( boolean maxBuff )
		{
			String buffName = ((UseSkillRequest) skillSelect.getSelectedItem()).getSkillName();
			if ( buffName == null )
				return;

			String [] targets = targetField.getText().split( "," );
			for ( int i = 0; i < targets.length; ++i )
				targets[i] = targets[i].trim();

			for ( int i = 0; i < targets.length; ++i )
				if ( targets[i] != null )
					for ( int j = i + 1; j < targets.length; ++j )
						if ( targets[j] == null || targets[i].equals( targets[j] ) )
							targets[j] = null;

			int buffCount = maxBuff ?
				(int) ( KoLCharacter.getCurrentMP() /
					ClassSkillsDatabase.getMPConsumptionByID( ClassSkillsDatabase.getSkillID( buffName ) ) ) : getValue( countField, 1 );

			Runnable [] requests;

			if ( targets.length == 0 )
			{
				requests = new Runnable[1];
				requests[0] = new UseSkillRequest( client, buffName, "", buffCount );
			}
			else
			{
				requests = new Runnable[ targets.length ];
				for ( int i = 0; i < requests.length; ++i )
					if ( targets[i] != null )
						requests[i] = new UseSkillRequest( client, buffName, targets[i], buffCount );
			}

			(new RequestThread( requests )).start();
		}
	}

	protected void processWindowEvent( WindowEvent e )
	{
		if ( e.getID() == WindowEvent.WINDOW_CLOSING && !existingFrames.isEmpty() )
		{
			if ( JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog( null,
				"Are you sure you wish to end this KoLmafia session?", "SDUGA Accidental Exit Protection Feature", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE ) )
					return;
		}

		super.processWindowEvent( e );
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		Object [] parameters = new Object[1];
		parameters[0] = null;

		(new CreateFrameRunnable( AdventureFrame.class, parameters )).run();
	}
}
