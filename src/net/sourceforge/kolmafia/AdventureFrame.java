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
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;

// containers
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.JTabbedPane;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JOptionPane;

// utilities
import java.util.Date;
import java.util.Arrays;

import java.io.File;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.FileOutputStream;

// other imports
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
	private JList moodList;
	private JTree combatTree;
	private JTextArea combatEditor;
	private DefaultTreeModel combatModel;
	private CardLayout combatCards;
	private JPanel combatPanel;

	private JComboBox locationSelect;
	private JComboBox dropdown1, dropdown2;
	private AdventureSelectPanel adventureSelect;

	private JComboBox battleStopSelect;

	private JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect;
	private JCheckBox [] hpRestoreCheckbox;
	private JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect;
	private JCheckBox [] mpRestoreCheckbox;

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 */

	public AdventureFrame()
	{
		super( "Adventure" );

		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		// Construct the adventure select container
		// to hold everything related to adventuring.

		JPanel adventureContainer = new JPanel( new BorderLayout( 10, 10 ) );
		this.adventureSelect = new AdventureSelectPanel();

		JPanel southPanel = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
		southPanel.add( getAdventureSummary( StaticEntity.getIntegerProperty( "defaultDropdown1" ) ) );
		southPanel.add( getAdventureSummary( StaticEntity.getIntegerProperty( "defaultDropdown2" ) ) );

		adventureContainer.add( adventureSelect, BorderLayout.NORTH );
		adventureContainer.add( southPanel, BorderLayout.CENTER );

		// Components of auto-restoration

		JPanel restorePanel = new JPanel();
		restorePanel.setLayout( new BoxLayout( restorePanel, BoxLayout.Y_AXIS ) );

		restorePanel.add( new HealthOptionsPanel() );
		restorePanel.add( new ManaOptionsPanel() );

		CheckboxListener listener = new CheckboxListener();
		for ( int i = 0; i < hpRestoreCheckbox.length; ++i )
			hpRestoreCheckbox[i].addActionListener( listener );
		for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
			mpRestoreCheckbox[i].addActionListener( listener );

		JPanel moodPanel = new JPanel( new BorderLayout() );
		moodPanel.add( new AddTriggerPanel(), BorderLayout.NORTH );
		moodPanel.add( new MoodTriggerListPanel(), BorderLayout.CENTER );

		// Components of custom combat

		combatTree = new JTree();
		combatModel = (DefaultTreeModel) combatTree.getModel();

		combatCards = new CardLayout();
		combatPanel = new JPanel( combatCards );
		combatPanel.add( "tree", new CustomCombatTreePanel() );
		combatPanel.add( "editor", new CustomCombatPanel() );

		tabs.addTab( "Turn Burning", adventureContainer );
		addTab( "Choice Handling", new ChoiceOptionsPanel() );
		addTab( "Auto Recovery", restorePanel );
		tabs.addTab( "Mood Swings", moodPanel );
		tabs.addTab( "Custom Combat", combatPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	public boolean useSidePane()
	{	return true;
	}

	private JPanel getAdventureSummary( int selectedIndex )
	{
		CardLayout resultCards = new CardLayout();
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new AdventureResultsPanel( tally ), "0" );

		resultSelect.addItem( "Location Details" );
		resultPanel.add( new SafetyField(), "1" );

		resultSelect.addItem( "Mood Summary" );
		resultPanel.add( new AdventureResultsPanel( MoodSettings.getTriggers() ), "2" );

		resultSelect.addItem( "Conditions Left" );
		resultPanel.add( new AdventureResultsPanel( conditions ), "3" );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new AdventureResultsPanel( activeEffects ), "4" );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new AdventureResultsPanel( adventureList ), "5" );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new AdventureResultsPanel( encounterList ), "6" );

		resultSelect.addActionListener( new ResultSelectListener( resultCards, resultPanel, resultSelect ) );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( resultSelect, BorderLayout.NORTH );
		containerPanel.add( resultPanel, BorderLayout.CENTER );

		if ( dropdown1 == null )
		{
			dropdown1 = resultSelect;
			dropdown1.setSelectedIndex( selectedIndex );
		}
		else
		{
			dropdown2 = resultSelect;
			dropdown2.setSelectedIndex( selectedIndex );
		}

		return containerPanel;
	}

	public void requestFocus()
	{
		super.requestFocus();
		locationSelect.requestFocus();
	}

	private class ResultSelectListener implements ActionListener
	{
		private CardLayout resultCards;
		private JPanel resultPanel;
		private JComboBox resultSelect;

		public ResultSelectListener( CardLayout resultCards, JPanel resultPanel, JComboBox resultSelect )
		{
			this.resultCards = resultCards;
			this.resultPanel = resultPanel;
			this.resultSelect = resultSelect;
		}

		public void actionPerformed( ActionEvent e )
		{
			String index = String.valueOf( resultSelect.getSelectedIndex() );
			resultCards.show( resultPanel, index );
			StaticEntity.setProperty( resultSelect == dropdown1 ? "defaultDropdown1" : "defaultDropdown2", index );

		}
	}

	private class SafetyField extends JPanel implements Runnable, ActionListener
	{
		private JLabel safetyText = new JLabel( " " );
		private String savedText = " ";

		public SafetyField()
		{
			super( new BorderLayout() );
			safetyText.setVerticalAlignment( JLabel.TOP );

			SimpleScrollPane textScroller = new SimpleScrollPane( safetyText, SimpleScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );

			JComponentUtilities.setComponentSize( textScroller, 100, 100 );
			add( textScroller, BorderLayout.CENTER );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( this ) );
			locationSelect.addActionListener( this );

			setSafetyString();
		}

		public void run()
		{	setSafetyString();
		}

		public void actionPerformed( ActionEvent e )
		{	setSafetyString();
		}

		private void setSafetyString()
		{
			KoLAdventure request = (KoLAdventure) locationSelect.getSelectedItem();
			if ( request == null )
				return;

			AreaCombatData combat = request.getAreaSummary();
			String text = ( combat == null ) ? " " : combat.toString();

			// Avoid rendering and screen flicker if no change.
			// Compare with our own copy of what we set, since
			// getText() returns a modified version.

			if ( !text.equals( savedText ) )
			{
				savedText = text;
				safetyText.setText( text );
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
		private JTextField countField;
		private JTextField conditionField;

		public AdventureSelectPanel()
		{
			super( "begin advs", "stop all", new Dimension( 130, 20 ), new Dimension( 270, 20 ) );

			actionSelect = new JComboBox( KoLCharacter.getBattleSkillNames() );
			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();

			locationSelect = new JComboBox( adventureList );

			countField = new JTextField();
			conditionField = new JTextField( "none" );

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Location: ", locationSelect );
			elements[1] = new VerifiableElement( "# of Visits: ", countField );
			elements[2] = new VerifiableElement( "Combat Action: ", actionSelect );
			elements[3] = new VerifiableElement( "Objective(s): ", conditionField );

			setContent( elements );
			actionSelect.addActionListener( new BattleActionListener() );
			locationSelect.addActionListener( new ConditionChangeListener() );
			locationSelect.setSelectedItem( AdventureDatabase.getAdventure( StaticEntity.getProperty( "lastAdventure" ) ) );
		}

		private class ConditionChangeListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( !StaticEntity.getBooleanProperty( "autoSetConditions" ) )
					return;

				KoLAdventure location = (KoLAdventure) locationSelect.getSelectedItem();
				conditionField.setText( AdventureDatabase.getCondition( location ) );
			}
		}

		private class BattleActionListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( actionSelect.getSelectedIndex() != -1 )
				{
					KoLmafia.forceContinue();
					DEFAULT_SHELL.executeLine( "set battleAction=" + actionSelect.getSelectedItem() );
				}
			}
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( actionSelect == null )
				return;

			super.setEnabled( isEnabled );
			actionSelect.setEnabled( true );
		}

		public void actionConfirmed()
		{
			// Once the stubs are finished, this will notify the
			//to begin adventuring based on the values
			// placed in the input fields.

			if ( actionSelect.getSelectedItem() == null )
				DEFAULT_SHELL.executeLine( "set battleAction=attack with weapon" );

			Runnable request = (Runnable) locationSelect.getSelectedItem();
			if ( request == null )
				return;

			// If there are conditions in the condition field, be
			// sure to process them.

			String conditionList = conditionField.getText().trim().toLowerCase();

			if ( conditionList.equalsIgnoreCase( "none" ) )
				conditionList = "";

			if ( !conditions.isEmpty() && StaticEntity.getBooleanProperty( "autoSetConditions" ) &&
				request instanceof KoLAdventure && conditionList.equals( AdventureDatabase.getCondition( (KoLAdventure) request ) ) )
			{
				conditionList = "";
			}

			if ( conditionList.length() > 0 )
			{
				DEFAULT_SHELL.executeConditionsCommand( "clear" );

				boolean verifyConditions = false;
				boolean useDisjunction = false;
				String [] splitConditions = conditionList.split( "\\s*,\\s*" );

				for ( int i = 0; i < splitConditions.length; ++i )
				{
					if ( splitConditions[i].equals( "check" ) )
					{
						// Postpone verification of conditions
						// until all other conditions added.

						verifyConditions = true;
					}
					else if ( splitConditions[i].equals( "outfit" ) )
					{
						// Determine where you're adventuring and use
						// that to determine which components make up
						// the outfit pulled from that area.

						if ( !(request instanceof KoLAdventure) || !EquipmentDatabase.addOutfitConditions( (KoLAdventure) request ) )
						{
							setStatusMessage( "No outfit corresponds to this zone." );
							return;
						}

						verifyConditions = true;
					}
					else if ( splitConditions[i].equals( "or" ) || splitConditions[i].equals( "and" ) || splitConditions[i].startsWith( "conjunction" ) || splitConditions[i].startsWith( "disjunction" ) )
					{
						useDisjunction = splitConditions[i].equals( "or" ) || splitConditions[i].startsWith( "disjunction" );
					}
					else
					{
						if ( !DEFAULT_SHELL.executeConditionsCommand( "add " + splitConditions[i] ) )
						{
							KoLmafia.enableDisplay();
							return;
						}
					}
				}

				if ( verifyConditions )
				{
					DEFAULT_SHELL.executeConditionsCommand( "check" );
					if ( conditions.isEmpty() )
					{
						KoLmafia.updateDisplay( "All conditions already satisfied." );
						KoLmafia.enableDisplay();
						return;
					}
				}

				if ( conditions.size() > 1 )
					DEFAULT_SHELL.executeConditionsCommand( useDisjunction ? "mode disjunction" : "mode conjunction" );

				if ( countField.getText().equals( "" ) )
					countField.setText( String.valueOf( KoLCharacter.getAdventuresLeft() ) );

				if ( !StaticEntity.getBooleanProperty( "autoSetConditions" ) )
					conditionField.setText( "" );
			}

			int requestCount = Math.min( getValue( countField, 1 ), KoLCharacter.getAdventuresLeft() );
			countField.setText( String.valueOf( requestCount ) );

			(new RequestThread( request, requestCount )).start();
		}

		public void actionCancelled()
		{	KoLmafia.declareWorldPeace();
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

			add( new SimpleScrollPane( tallyDisplay ), BorderLayout.CENTER );
		}
	}

	private class CustomCombatPanel extends LabeledScrollPanel
	{
		public CustomCombatPanel()
		{
			super( "Editor", "save", "help", new JTextArea( 12, 40 ) );
			combatEditor = (JTextArea) scrollComponent;
			combatEditor.setFont( DEFAULT_FONT );
			refreshCombatSettings();
		}

		public void actionConfirmed()
		{
			try
			{
				File location = new File( CombatSettings.settingsFileName() );
				if ( !location.exists() )
					CombatSettings.reset();

				LogStream writer = new LogStream( location );
				writer.println( ((JTextArea)scrollComponent).getText() );
				writer.close();
				writer = null;

				KoLCharacter.battleSkillNames.setSelectedItem( "custom combat script" );
				StaticEntity.setProperty( "battleAction", "custom combat script" );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e );
			}

			// After storing all the data on disk, go ahead
			// and reload the data inside of the tree.

			refreshCombatTree();
			combatCards.show( combatPanel, "tree" );
		}

		public void actionCancelled()
		{	StaticEntity.openSystemBrowser( "http://kolmafia.sourceforge.net/combat.html" );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	private class CustomCombatTreePanel extends LabeledScrollPanel
	{
		public CustomCombatTreePanel()
		{	super( "Tree View", "edit", "load", combatTree );
		}

		public void actionConfirmed()
		{
			refreshCombatSettings();
			combatCards.show( combatPanel, "editor" );
		}

		public void actionCancelled()
		{
			JFileChooser chooser = new JFileChooser( (new File( "settings" )).getAbsolutePath() );
			chooser.setFileFilter( CCS_FILTER );

			int returnVal = chooser.showOpenDialog( null );

			if ( chooser.getSelectedFile() == null || returnVal != JFileChooser.APPROVE_OPTION )
				return;

			CombatSettings.loadSettings( chooser.getSelectedFile() );
			refreshCombatSettings();
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	private static final FileFilter CCS_FILTER = new FileFilter()
	{
		public boolean accept( File file )
		{
			String name = file.getName();
			return !name.startsWith( "." ) && name.startsWith( "combat_" );
		}

		public String getDescription()
		{	return "Custom Combat Settings";
		}
	};

	private void refreshCombatSettings()
	{
		try
		{
			CombatSettings.reset();
			BufferedReader reader = KoLDatabase.getReader( CombatSettings.settingsFileName() );

			StringBuffer buffer = new StringBuffer();
			String line;

			while ( (line = reader.readLine()) != null )
			{
				buffer.append( line );
				buffer.append( System.getProperty( "line.separator" ) );
			}

			reader.close();
			reader = null;
			combatEditor.setText( buffer.toString() );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		refreshCombatTree();
	}

	/**
	 * Internal class used to handle everything related to
	 * displaying custom combat.
	 */

	private void refreshCombatTree()
	{
		CombatSettings.reset();
		combatModel.setRoot( CombatSettings.getRoot() );
		combatTree.setRootVisible( false );
	}

	/**
	 * This panel allows the user to select which item they would like
	 * to do for each of the different choice adventures.
	 */

	private class ChoiceOptionsPanel extends KoLPanel
	{
		private JComboBox [] optionSelects;

		private JComboBox castleWheelSelect;
		private JComboBox spookyForestSelect;
		private JComboBox tripTypeSelect;
		private JComboBox violetFogSelect;
		private JComboBox louvreSelect;
		private JComboBox billiardRoomSelect;
		private JComboBox library1Select;
		private JComboBox library2Select;

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
				optionSelects[i].addItem( ignorable ? "Ignore this adventure" : "Make semi-random decision" );

				for ( int j = 0; j < AdventureDatabase.CHOICE_ADVS[i][2].length; ++j )
					optionSelects[i].addItem( AdventureDatabase.CHOICE_ADVS[i][2][j] );
			}

			castleWheelSelect = new JComboBox();
			castleWheelSelect.addItem( "Turn to map quest position (via moxie)" );
			castleWheelSelect.addItem( "Turn to map quest position (via mysticality)" );
			castleWheelSelect.addItem( "Turn to muscle position" );
			castleWheelSelect.addItem( "Turn to mysticality position" );
			castleWheelSelect.addItem( "Turn to moxie position" );
			castleWheelSelect.addItem( "Turn clockwise" );
			castleWheelSelect.addItem( "Turn counterclockwise" );
			castleWheelSelect.addItem( "Ignore this adventure" );

			spookyForestSelect = new JComboBox();
			spookyForestSelect.addItem( "Loot Seal Clubber corpse" );
			spookyForestSelect.addItem( "Loot Turtle Tamer corpse" );
			spookyForestSelect.addItem( "Loot Pastamancer corpse" );
			spookyForestSelect.addItem( "Loot Sauceror corpse" );
			spookyForestSelect.addItem( "Loot Disco Bandit corpse" );
			spookyForestSelect.addItem( "Loot Accordion Thief corpse" );

			tripTypeSelect = new JComboBox();
			tripTypeSelect.addItem( "Take the Bad Trip" );
			tripTypeSelect.addItem( "Take the Mediocre Trip" );
			tripTypeSelect.addItem( "Take the Great Trip" );

			violetFogSelect = new JComboBox();
			for ( int i = 0; i < VioletFog.FogGoals.length; ++i )
				violetFogSelect.addItem( VioletFog.FogGoals[i] );

			louvreSelect = new JComboBox();
			louvreSelect.addItem( "Ignore this adventure" );
			for ( int i = 0; i < Louvre.LouvreGoals.length - 3; ++i )
				louvreSelect.addItem( Louvre.LouvreGoals[i] );
			for ( int i = Louvre.LouvreGoals.length - 3; i < Louvre.LouvreGoals.length; ++i )
				louvreSelect.addItem( "Boost " + Louvre.LouvreGoals[i] );
			louvreSelect.addItem( "Boost Lowest Stat" );

			billiardRoomSelect = new JComboBox();
			billiardRoomSelect.addItem( "Ignore this adventure" );
			billiardRoomSelect.addItem( "Muscle" );
			billiardRoomSelect.addItem( "Mysticality" );
			billiardRoomSelect.addItem( "Moxie" );
			billiardRoomSelect.addItem( "Library Key" );

			library1Select = new JComboBox();
			library1Select.addItem( "Ignore this adventure" );
			library1Select.addItem( "Mysticality" );
			library1Select.addItem( "Moxie" );
			library1Select.addItem( "Mysticality Class Skill" );

			library2Select = new JComboBox();
			library2Select.addItem( "Ignore this adventure" );
			library2Select.addItem( "Muscle" );
			library2Select.addItem( "Gallery Key" );

			VerifiableElement [] elements = new VerifiableElement[ optionSelects.length + 9 ];
			elements[0] = new VerifiableElement( "Castle Wheel", castleWheelSelect );
			elements[1] = new VerifiableElement( "Forest Corpses", spookyForestSelect );
			elements[2] = new VerifiableElement( "Violet Fog 1", tripTypeSelect );
			elements[3] = new VerifiableElement( "Violet Fog 2", violetFogSelect );
			elements[4] = new VerifiableElement( "Haunted Billiard Room", billiardRoomSelect );
			elements[5] = new VerifiableElement( "Haunted Library 1", library1Select );
			elements[6] = new VerifiableElement( "Haunted Library 2", library2Select );
			elements[7] = new VerifiableElement( "Haunted Gallery", louvreSelect );
			elements[8] = new VerifiableElement( "Lucky Sewer", optionSelects[0] );

			elements[9] = new VerifiableElement( "", new JLabel() );
			for ( int i = 1; i < optionSelects.length; ++i )
				elements[i+9] = new VerifiableElement( AdventureDatabase.CHOICE_ADVS[i][1][0], optionSelects[i] );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "violetFogGoal", String.valueOf( violetFogSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "choiceAdventure71", String.valueOf( tripTypeSelect.getSelectedIndex() + 1 ) );
			StaticEntity.setProperty( "luckySewerAdventure", (String) optionSelects[0].getSelectedItem() );

			int louvreGoal = louvreSelect.getSelectedIndex();
			StaticEntity.setProperty( "choiceAdventure91",  String.valueOf( louvreGoal > 0 ? "1" : "2" ) );

			StaticEntity.setProperty( "louvreDesiredGoal", String.valueOf( louvreGoal ) );
			StaticEntity.setProperty( "louvreBoostsLowestStat", String.valueOf( louvreGoal > Louvre.LouvreGoals.length ) );

			for ( int i = 1; i < optionSelects.length; ++i )
			{
				int index = optionSelects[i].getSelectedIndex();
				String choice = AdventureDatabase.CHOICE_ADVS[i][0][0];
				boolean ignorable = AdventureDatabase.ignoreChoiceOption( choice ) != null;

				if ( ignorable || index != 0 )
					StaticEntity.setProperty( choice, String.valueOf( index ) );
				else if ( index >= 0 )
					optionSelects[i].setSelectedIndex( StaticEntity.getIntegerProperty( choice ) );
			}

			//              The Wheel:

			//              Muscle
			// Moxie          +         Mysticality
			//            Map Quest

			// Option 1: Turn the wheel clockwise
			// Option 2: Turn the wheel counterclockwise
			// Option 3: Leave the wheel alone

			switch ( castleWheelSelect.getSelectedIndex() )
			{
			case 0: // Map quest position (choice adventure 11)
									// Muscle goes through moxie
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 1: // Map quest position (choice adventure 11)
									// Muscle goes through mysticality
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 2: // Muscle position (choice adventure 9)
				StaticEntity.setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 3: // Mysticality position (choice adventure 10)
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
				StaticEntity.setProperty( "choiceAdventure11", "2" );  // Turn the map quest position counterclockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 4: // Moxie position (choice adventure 12)
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
				break;

			case 5: // Turn the wheel clockwise
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 6: // Turn the wheel counterclockwise
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "2" );  // Turn the map quest position counterclockwise
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 7: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
				StaticEntity.setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
				break;
			}

			switch ( spookyForestSelect.getSelectedIndex() )
			{
			case 0: // Seal clubber corpse
				StaticEntity.setProperty( "choiceAdventure26", "1" );
				StaticEntity.setProperty( "choiceAdventure27", "1" );
				break;

			case 1: // Turtle tamer corpse
				StaticEntity.setProperty( "choiceAdventure26", "1" );
				StaticEntity.setProperty( "choiceAdventure27", "2" );
				break;

			case 2: // Pastamancer corpse
				StaticEntity.setProperty( "choiceAdventure26", "2" );
				StaticEntity.setProperty( "choiceAdventure28", "1" );
				break;

			case 3: // Sauceror corpse
				StaticEntity.setProperty( "choiceAdventure26", "2" );
				StaticEntity.setProperty( "choiceAdventure28", "2" );
				break;

			case 4: // Disco bandit corpse
				StaticEntity.setProperty( "choiceAdventure26", "3" );
				StaticEntity.setProperty( "choiceAdventure29", "1" );
				break;

			case 5: // Accordion thief corpse
				StaticEntity.setProperty( "choiceAdventure26", "3" );
				StaticEntity.setProperty( "choiceAdventure29", "2" );
				break;
			}

			switch ( billiardRoomSelect.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure77", "3" );
				StaticEntity.setProperty( "choiceAdventure78", "3" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 1: // Muscle
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "2" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 2: // Mysticality
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "1" );
				StaticEntity.setProperty( "choiceAdventure79", "2" );
				break;

			case 3: // Moxie
				StaticEntity.setProperty( "choiceAdventure77", "1" );
				StaticEntity.setProperty( "choiceAdventure78", "3" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 4: // Library Key
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "1" );
				StaticEntity.setProperty( "choiceAdventure79", "1" );
				break;
			}

			switch ( library1Select.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure80", "4" );
				StaticEntity.setProperty( "choiceAdventure88", "1" );
				break;

			case 1: // Mysticality
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "1" );
				break;

			case 2: // Moxie
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "2" );
				break;

			case 3: // Mysticality Class Skill
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "3" );
				break;
			}

			switch ( library2Select.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure81", "4" );
				StaticEntity.setProperty( "choiceAdventure87", "1" );
				break;

			case 1: // Muscle
				StaticEntity.setProperty( "choiceAdventure81", "3" );
				StaticEntity.setProperty( "choiceAdventure87", "2" );
				break;

			case 2: // Gallery Key
				StaticEntity.setProperty( "choiceAdventure81", "1" );
				StaticEntity.setProperty( "choiceAdventure87", "2" );
				break;
			}
		}

		public void actionCancelled()
		{
			int index = StaticEntity.getIntegerProperty( "violetFogGoal" );
			if ( index >= 0 )
				violetFogSelect.setSelectedIndex( index );

			index = StaticEntity.getIntegerProperty( "louvreDesiredGoal" );
			if ( index >= 0 )
				louvreSelect.setSelectedIndex( index );

			for ( int i = 1; i < optionSelects.length; ++i )
			{
				index = StaticEntity.getIntegerProperty( AdventureDatabase.CHOICE_ADVS[i][0][0] );
				if ( index >= 0 )
					optionSelects[i].setSelectedIndex( index );
			}

			// Determine the desired wheel position by examining
			// which choice adventure has the "3" value.
			// If none are "3", may be clockwise or counterclockwise
			// If they are all "3", leave wheel alone

			int [] counts = { 0, 0, 0, 0 };
			int option3 = 11;

			for ( int i = 9; i < 13; ++i )
			{
				int choice = StaticEntity.getIntegerProperty( "choiceAdventure" + i );
				counts[choice]++;

				if ( choice == 3 )
					option3 = i;
			}

			index = 0;

			if ( counts[1] == 4 )
			{
				// All choices say turn clockwise
				index = 5;
			}
			else if ( counts[2] == 4 )
			{
				// All choices say turn counterclockwise
				index = 6;
			}
			else if ( counts[3] == 4 )
			{
				// All choices say leave alone
				index = 7;
			}
			else if ( counts[3] != 1 )
			{
				// Bogus. Assume map quest
				index = 0;
			}
			else if ( option3 == 9)
			{
				// Muscle says leave alone
				index = 2;
			}
			else if ( option3 == 10)
			{
				// Mysticality says leave alone
				index = 3;
			}
			else if ( option3 == 11)
			{
				// Map Quest says leave alone. If we turn
				// clockwise twice, we are going through
				// mysticality. Otherwise, through moxie.
				index = ( counts[1] == 2 ) ? 1 : 0;
			}
			else if ( option3 == 12 )
			{
				// Moxie says leave alone
				index = 4;
			}

			if ( index >= 0 )
				castleWheelSelect.setSelectedIndex( index );

			// Now, determine what is located in choice adventure #26,
			// which shows you which slot (in general) to use.

			index = StaticEntity.getIntegerProperty( "choiceAdventure26" );
			index = index * 2 + StaticEntity.getIntegerProperty( "choiceAdventure" + (26 + index) ) - 3;

			spookyForestSelect.setSelectedIndex( index < 0 ? 5 : index );

			// Figure out what to do in the billiard room

			switch ( StaticEntity.getIntegerProperty( "choiceAdventure77" ) )
			{
			case 1:

				// Moxie
				index = 3;
				break;

			case 2:

				index = StaticEntity.getIntegerProperty( "choiceAdventure78" );

				switch ( index )
				{
				case 1:
					index = StaticEntity.getIntegerProperty( "choiceAdventure79" );
					index = index == 1 ? 4 : index == 2 ? 2 : 0;
					break;
				case 2:
					// Muscle
					index = 1;
					break;
				case 3:
					// Ignore this adventure
					index = 0;
					break;
				}

				break;

			case 3:

				// Ignore this adventure
				index = 0;
				break;
			}

			if ( index >= 0 )
				billiardRoomSelect.setSelectedIndex( index );

			// Figure out what to do at the first bookcase
			index = StaticEntity.getIntegerProperty( "choiceAdventure80" );
			if ( index == 3 )
			{
				index = StaticEntity.getIntegerProperty( "choiceAdventure88" );
				index = ( index < 1 || index > 3 ) ? 0 : index;
			}
			else
			{
				// None of the above. Ignore
				index = 0;
			}

			if ( index >= 0 )
				library1Select.setSelectedIndex( index );

			// Figure out what to do at the second bookcase
			index = StaticEntity.getIntegerProperty( "choiceAdventure81" );
			switch ( index )
			{
			case 1:
				// Check for Gallery Key
				index = StaticEntity.getIntegerProperty( "choiceAdventure87" );
				index = ( index == 2 ) ? 2 : 0;
				break;
			case 3:
				// Muscle
				index = 1;
				break;
			default:
				// Ignore
				index = 0;
				break;
			}

			if ( index >= 0 )
				library2Select.setSelectedIndex( index );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	private void saveRestoreSettings()
	{
		StaticEntity.setProperty( "battleStop", String.valueOf( ((float)(battleStopSelect.getSelectedIndex()) / 10.0f) ) );

		StaticEntity.setProperty( "hpAutoRecovery", String.valueOf( ((float)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "hpAutoRecoveryTarget", String.valueOf( ((float)(hpAutoRecoverTargetSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "hpAutoRecoveryItems", getSettingString( hpRestoreCheckbox ) );

		StaticEntity.setProperty( "mpAutoRecovery", String.valueOf( ((float)(mpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "mpAutoRecoveryTarget", String.valueOf( ((float)(mpAutoRecoverTargetSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "mpAutoRecoveryItems", getSettingString( mpRestoreCheckbox ) );
	}

	private class CheckboxListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	saveRestoreSettings();
		}
	}

	private class HealthOptionsPanel extends KoLPanel
	{
		private boolean refreshSoon = false;

		public HealthOptionsPanel()
		{
			super( new Dimension( 160, 20 ), new Dimension( 300, 20 ) );

			battleStopSelect = new JComboBox();
			for ( int i = 0; i <= 9; ++i )
				battleStopSelect.addItem( "Restorer fails to bring health above " + (i*10) + "%" );

			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover health" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverSelect.addItem( "Auto-recover health at " + (i*10) + "%" );

			hpAutoRecoverTargetSelect = new JComboBox();
			hpAutoRecoverTargetSelect.addItem( "Do not automatically recover health" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*10) + "% health" );

			// Add the elements to the panel

			int currentElementCount = 0;
			VerifiableElement [] elements = new VerifiableElement[5];

			elements[ currentElementCount++ ] = new VerifiableElement( "Abort condition: ", battleStopSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "", new JLabel() );

			elements[ currentElementCount++ ] = new VerifiableElement( "Restore your health: ", hpAutoRecoverSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "", hpAutoRecoverTargetSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "Use these restores: ", constructScroller( hpRestoreCheckbox = HPRestoreItemList.getCheckboxes() ) );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{	saveRestoreSettings();
		}

		public void actionCancelled()
		{
			battleStopSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "battleStop" ) * 10) );
			hpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "hpAutoRecovery" ) * 10) + 1 );
			hpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "hpAutoRecoveryTarget" ) * 10) + 1 );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	private class ManaOptionsPanel extends KoLPanel
	{
		public ManaOptionsPanel()
		{
			super( new Dimension( 160, 20 ), new Dimension( 300, 20 ) );

			mpAutoRecoverSelect = new JComboBox();
			mpAutoRecoverSelect.addItem( "Do not automatically recover mana" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverSelect.addItem( "Auto-recover mana at " + (i*10) + "%" );

			mpAutoRecoverTargetSelect = new JComboBox();
			mpAutoRecoverTargetSelect.addItem( "Do not automatically recover mana" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*10) + "% mana" );

			// Add the elements to the panel

			int currentElementCount = 0;
			VerifiableElement [] elements = new VerifiableElement[3];

			elements[ currentElementCount++ ] = new VerifiableElement( "Restore your mana: ", mpAutoRecoverSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "", mpAutoRecoverTargetSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "Use these restores: ", constructScroller( mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{	saveRestoreSettings();
		}

		public void actionCancelled()
		{
			mpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "mpAutoRecovery" ) * 10) + 1 );
			mpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "mpAutoRecoveryTarget" ) * 10) + 1 );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * BuffBot options management
	 */

	private class AddTriggerPanel extends KoLPanel
	{
		private LockableListModel EMPTY_MODEL = new LockableListModel();
		private LockableListModel EFFECT_MODEL = new LockableListModel();

		private TypeComboBox typeSelect;
		private ValueComboBox valueSelect;
		private JTextField commandField;

		public AddTriggerPanel()
		{
			super( "add entry", "auto-fill" );

			typeSelect = new TypeComboBox();

			Object [] names = StatusEffectDatabase.values().toArray();
			Arrays.sort( names );

			for ( int i = 0; i < names.length; ++i )
				EFFECT_MODEL.add( names[i] );

			valueSelect = new ValueComboBox();
			commandField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Trigger On: ", typeSelect );
			elements[1] = new VerifiableElement( "Check For: ", valueSelect );
			elements[2] = new VerifiableElement( "Command: ", commandField );

			setContent( elements );
		}

		public void actionConfirmed()
		{	MoodSettings.addTrigger( (String) typeSelect.getSelectedType(), (String) valueSelect.getSelectedItem(), commandField.getText() );
		}

		public void actionCancelled()
		{	MoodSettings.autoFillTriggers();
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		private class ValueComboBox extends JComboBox implements ActionListener
		{
			public ValueComboBox()
			{
				super( EFFECT_MODEL );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				commandField.setText( MoodSettings.getDefaultAction( typeSelect.getSelectedType(), (String) getSelectedItem() ) );
			}
		}

		private class TypeComboBox extends JComboBox implements ActionListener
		{
			public TypeComboBox()
			{
				addItem( "When an effect is lost" );
				addItem( "When an effect is gained" );
				addItem( "Unconditional trigger" );

				addActionListener( this );
			}

			public String getSelectedType()
			{
				switch ( getSelectedIndex() )
				{
				case 0:
					return "lose_effect";
				case 1:
					return "gain_effect";
				case 2:
					return "unconditional";
				default:
					return null;
				}
			}

			public void actionPerformed( ActionEvent e )
			{	valueSelect.setModel( getSelectedIndex() == 2 ? EMPTY_MODEL : EFFECT_MODEL );
			}
		}
	}

	private class MoodTriggerListPanel extends LabeledScrollPanel
	{
		private JComboBox moodSelect;

		public MoodTriggerListPanel()
		{

			super( "", "new list", "remove", new JList( MoodSettings.getTriggers() ) );

			moodSelect = new MoodComboBox();

			CopyMoodButton moodCopy = new CopyMoodButton();
			InvocationButton moodRemove = new InvocationButton( "delete list", MoodSettings.class, "deleteCurrentMood" );

			actualPanel.add( moodSelect, BorderLayout.NORTH );
			moodList = (JList) scrollComponent;

			JPanel extraButtons = new JPanel( new BorderLayout( 2, 2 ) );
			extraButtons.add( moodRemove, BorderLayout.NORTH );
			extraButtons.add( moodCopy, BorderLayout.SOUTH );

			buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			String name = JOptionPane.showInputDialog( "Give your list a name!" );
			if ( name == null )
				return;

			MoodSettings.setMood( name );
		}

		public void actionCancelled()
		{	MoodSettings.removeTriggers( moodList.getSelectedValues() );
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		private class MoodComboBox extends JComboBox implements ActionListener
		{
			public MoodComboBox()
			{
				super( MoodSettings.getAvailableMoods() );
				setSelectedItem( StaticEntity.getProperty( "currentMood" ) );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{	MoodSettings.setMood( (String) getSelectedItem() );
			}
		}

		private class CopyMoodButton extends JButton implements ActionListener
		{
			public CopyMoodButton()
			{
				super( "copy list" );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				String moodName = JOptionPane.showInputDialog( "Make a copy of current mood list called:" );
				if ( moodName == null )
					return;

				if ( moodName.equals( "default" ) )
					return;

				MoodSettings.copyTriggers( moodName );
				MoodSettings.setMood( moodName );
			}
		}
	}
}
