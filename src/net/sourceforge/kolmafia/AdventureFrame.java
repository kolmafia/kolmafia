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

// event listeners
import javax.swing.SwingUtilities;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

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
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

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
	private JTree displayTree;
	private DefaultTreeModel displayModel;
	private JComboBox locationSelect;
	private AdventureSelectPanel adventureSelect;

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 */

	public AdventureFrame()
	{
		super( "Adventure" );
		tabs = new JTabbedPane();

		// Construct the adventure select container
		// to hold everything related to adventuring.

		JPanel adventureContainer = new JPanel( new BorderLayout( 10, 10 ) );

		this.adventureSelect = new AdventureSelectPanel();

		JPanel southPanel = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
		southPanel.add( getAdventureSummary(0) );
		southPanel.add( getAdventureSummary(1) );

		adventureContainer.add( adventureSelect, BorderLayout.NORTH );
		adventureContainer.add( southPanel, BorderLayout.CENTER );
		tabs.add( "Adventure", adventureContainer );

		JScrollPane choiceScroller = new JScrollPane( new ChoiceOptionsPanel(),
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( choiceScroller, 560, 400 );
		tabs.add( "Choice Options", choiceScroller );

		displayTree = new JTree();
		displayModel = (DefaultTreeModel) displayTree.getModel();

		JScrollPane treeScroller = new JScrollPane( displayTree, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		tabs.add( "View CCS", treeScroller );
		tabs.add( "Modify CCS", new CustomCombatPanel() );

		try
		{
			String holiday = MoonPhaseDatabase.getHoliday( sdf.parse( sdf.format( new Date() ) ) );

			if ( holiday.startsWith( "No" ) )
				DEFAULT_SHELL.updateDisplay( NULL_STATE, MoonPhaseDatabase.getMoonEffect() );
			else
				DEFAULT_SHELL.updateDisplay( NULL_STATE, holiday + ", " + MoonPhaseDatabase.getMoonEffect() );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.
			
			StaticEntity.printStackTrace( e );
		}

		getContentPane().add( tabs, BorderLayout.CENTER );
	}

	public boolean useSidePane()
	{	return true;
	}

	private JPanel getAdventureSummary( int selectedIndex )
	{
		CardLayout resultCards = new CardLayout( 0, 0 );
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new AdventureResultsPanel( StaticEntity.getClient().getSessionTally() ), "0" );

		resultSelect.addItem( "Location Details" );
		resultPanel.add( new SafetyField(), "1" );

		resultSelect.addItem( "Conditions Left" );
		resultPanel.add( new AdventureResultsPanel( StaticEntity.getClient().getConditions() ), "2" );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new AdventureResultsPanel( KoLCharacter.getEffects() ), "3" );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new AdventureResultsPanel( StaticEntity.getClient().getAdventureList() ), "4" );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new AdventureResultsPanel( StaticEntity.getClient().getEncounterList() ), "5" );

		resultSelect.addActionListener( new ResultSelectListener( resultCards, resultPanel, resultSelect ) );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( resultSelect, BorderLayout.NORTH );
		containerPanel.add( resultPanel, BorderLayout.CENTER );

		resultSelect.setSelectedIndex( selectedIndex );
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
		{	resultCards.show( resultPanel, String.valueOf( resultSelect.getSelectedIndex() ) );
		}
	}

	private class SafetyField extends JPanel implements Runnable, ActionListener
	{
		private JLabel safetyText;

		public SafetyField()
		{
			super( new BorderLayout() );
			safetyText = new JLabel( " " );
			safetyText.setVerticalAlignment( JLabel.TOP );

			JScrollPane textScroller = new JScrollPane( safetyText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
			
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
			Runnable request = (Runnable) locationSelect.getSelectedItem();
			if ( request == null )
				return;

			AreaCombatData combat = AdventureDatabase.getAreaCombatData( request.toString() );
			String text = ( combat == null ) ? "" : combat.toString();
			safetyText.setText( text );
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
			super( "begin advs", "stop all", new Dimension( 100, 20 ), new Dimension( 270, 20 ) );

			actionSelect = new JComboBox( KoLCharacter.getBattleSkillNames() );
			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();

			locationSelect = new JComboBox( adventureList );

			countField = new JTextField();
			conditionField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[4];
			elements[0] = new VerifiableElement( "Location: ", locationSelect );
			elements[1] = new VerifiableElement( "# of Visits: ", countField );
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

		public void setEnabled( boolean isEnabled )
		{
			super.setEnabled( isEnabled );
			actionSelect.setEnabled( true );
		}

		protected void actionConfirmed()
		{
			// Once the stubs are finished, this will notify the
			// client to begin adventuring based on the values
			// placed in the input fields.

			if ( actionSelect.getSelectedItem() == null )
			{
				setStatusMessage( ERROR_STATE, "Please select a combat option." );
				return;
			}

			Runnable request = (Runnable) locationSelect.getSelectedItem();
			if ( request == null )
			{
				setStatusMessage( ERROR_STATE, "Please select an adventure location." );
				return;
			}

			setProperty( "lastAdventure", request.toString() );

			// If there are conditions in the condition field, be
			// sure to process them.

			if ( conditionField.getText().trim().length() > 0 )
			{
				DEFAULT_SHELL.executeLine( "conditions clear" );

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
					else if ( conditions[i].equals( "outfit" ) )
					{
						// Determine where you're adventuring and use
						// that to determine which components make up
						// the outfit pulled from that area.

						if ( !(request instanceof KoLAdventure) || !EquipmentDatabase.addOutfitConditions( (KoLAdventure) request ) )
						{
							DEFAULT_SHELL.updateDisplay( ERROR_STATE, "No outfit corresponds to this zone." );
							return;
						}

						verifyConditions = true;
					}
					else if ( conditions[i].equals( "or" ) || conditions[i].equals( "and" ) || conditions[i].startsWith( "conjunction" ) || conditions[i].startsWith( "disjunction" ) )
					{
						useDisjunction = conditions[i].equals( "or" ) || conditions[i].startsWith( "disjunction" );
					}
					else
					{
						if ( !DEFAULT_SHELL.executeConditionsCommand( "add " + conditions[i] ) )
						{
							DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Invalid condition: " + conditions[i] );
							return;
						}
					}
				}

				if ( StaticEntity.getClient().getConditions().isEmpty() )
				{
					DEFAULT_SHELL.updateDisplay( "Conditions already satisfied." );
					StaticEntity.getClient().enableDisplay();
					return;
				}

				if ( verifyConditions )
				{
					DEFAULT_SHELL.executeConditionsCommand( "check" );
					if ( StaticEntity.getClient().getConditions().isEmpty() )
					{
						DEFAULT_SHELL.updateDisplay( "Conditions already satisfied." );
						StaticEntity.getClient().enableDisplay();
						return;
					}
				}

				DEFAULT_SHELL.executeConditionsCommand( useDisjunction ? "mode disjunction" : "mode conjunction" );
				DEFAULT_SHELL.updateDisplay( "Conditions set.  Preparing for adventuring..." );
				conditionField.setText( "" );
			}

			(new RequestThread( request, getValue( countField ) )).start();
		}

		protected void actionCancelled()
		{
			StaticEntity.getClient().declareWorldPeace();
			locationSelect.requestFocus();
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
	 * This panel allows the user to select which item they would like
	 * to do for each of the different choice adventures.
	 */

	private class ChoiceOptionsPanel extends KoLPanel
	{
		private JComboBox [] optionSelects;

		private JComboBox battleStopSelect;
		private JComboBox cloverProtectSelect;

		private JComboBox castleWheelSelect;
		private JComboBox spookyForestSelect;

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

			battleStopSelect = new JComboBox();
			battleStopSelect.addItem( "Never stop combat" );
			for ( int i = 0; i <= 9; ++i )
				battleStopSelect.addItem( "Autostop at " + (i*10) + "% HP" );
			
			cloverProtectSelect = new JComboBox();
			cloverProtectSelect.addItem( "Disassemble ten-leaf clovers" );
			cloverProtectSelect.addItem( "Leave ten-leaf clovers alone" );
			
			castleWheelSelect = new JComboBox();
			castleWheelSelect.addItem( "Turn to map quest position" );
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

			VerifiableElement [] elements = new VerifiableElement[ optionSelects.length + 6 ];
			elements[0] = new VerifiableElement( "Combat Abort", battleStopSelect );
			elements[1] = new VerifiableElement( "Clover Protect", cloverProtectSelect );

			elements[2] = new VerifiableElement( "", new JLabel() );
			elements[3] = new VerifiableElement( "Castle Wheel", castleWheelSelect );
			elements[4] = new VerifiableElement( "Forest Corpses", spookyForestSelect );
			elements[5] = new VerifiableElement( "Lucky Sewer", optionSelects[0] );

			elements[6] = new VerifiableElement( "", new JLabel() );
			for ( int i = 1; i < optionSelects.length; ++i )
				elements[i+6] = new VerifiableElement( AdventureDatabase.CHOICE_ADVS[i][1][0], optionSelects[i] );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			setProperty( "battleStop", String.valueOf( ((double)(battleStopSelect.getSelectedIndex() - 1) / 10.0) ) );
			setProperty( "cloverProtectActive", String.valueOf( cloverProtectSelect.getSelectedIndex() == 0 ) );
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

			// Option 1: Turn the wheel clockwise
			// Option 2: Turn the wheel counterclockwise
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

				case 4: // Turn the wheel clockwise
					setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
					setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
					setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
					setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
					break;

				case 5: // Turn the wheel counterclockwise
					setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
					setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
					setProperty( "choiceAdventure11", "2" );  // Turn the map quest position counterclockwise
					setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
					break;

				case 6: // Ignore this adventure
					setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
					setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
					setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
					setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
					break;
			}
			
			switch ( spookyForestSelect.getSelectedIndex() )
			{
				case 0: // Seal clubber corpse
					setProperty( "choiceAdventure26", "1" );
					setProperty( "choiceAdventure27", "1" );
					break;

				case 1: // Turtle tamer corpse
					setProperty( "choiceAdventure26", "1" );
					setProperty( "choiceAdventure27", "2" );
					break;

				case 2: // Pastamancer corpse
					setProperty( "choiceAdventure26", "2" );
					setProperty( "choiceAdventure28", "1" );
					break;

				case 3: // Sauceror corpse
					setProperty( "choiceAdventure26", "2" );
					setProperty( "choiceAdventure28", "2" );
					break;

				case 4: // Disco bandit corpse
					setProperty( "choiceAdventure26", "3" );
					setProperty( "choiceAdventure29", "1" );
					break;

				case 5: // Accordion thief corpse
					setProperty( "choiceAdventure26", "3" );
					setProperty( "choiceAdventure29", "2" );
					break;
			}
		}

		protected void actionCancelled()
		{
			battleStopSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "battleStop" ) ) * 10) + 1 );
			cloverProtectSelect.setSelectedIndex( getProperty( "cloverProtectActive" ).equals( "true" ) ? 0 : 1 );

			optionSelects[0].setSelectedItem( getProperty( "luckySewerAdventure" ) );
			for ( int i = 1; i < optionSelects.length; ++i )
				optionSelects[i].setSelectedIndex( Integer.parseInt( getProperty( AdventureDatabase.CHOICE_ADVS[i][0][0] ) ) );

			// Determine the desired wheel position by examining
			// which choice adventure has the "3" value.
			// If none are "3", may be clockwise or counterclockwise
			// If they are all "3", leave wheel alone

			int [] counts = { 0, 0, 0, 0 };
			int option3 = 11;
			for ( int i = 9; i < 13; ++i )
			{
				int choice = Integer.parseInt( getProperty( "choiceAdventure" + i ) );
				counts[choice]++;
				if ( choice == 3 )
					option3 = i;
			}

			int index = 0;

			if ( counts[1] == 4 )
			{
				// All choices say turn clockwise
				index = 4;
			}
			else if ( counts[2] == 4 )
			{
				// All choices say turn counterclockwise
				index = 5;
			}
			else if ( counts[3] == 4 )
			{
				// All choices say leave alone
				index = 6;
			}
			else if ( counts[3] != 1 )
			{
				// Bogus. Assume map quest
				index = 0;
			}
			else if ( option3 == 9)
			{
				// Muscle says leave alone
				index = 1;
			}
			else if ( option3 == 10)
			{
				// Mysticality says leave alone
				index = 2;
			}
			else if ( option3 == 11)
			{
				// Map Quest says leave alone
				index = 0;
			}
			else if ( option3 == 12)
			{
				// Moxie says leave alone
				index = 3;
			}

			castleWheelSelect.setSelectedIndex( index );
			
			// Now, determine what is located in choice adventure #26,
			// which shows you which slot (in general) to use.
			
			index = Integer.parseInt( getProperty( "choiceAdventure26" ) );
			index = index * 2 + Integer.parseInt( getProperty( "choiceAdventure" + (26 + index) ) ) - 3;
			
			spookyForestSelect.setSelectedIndex( index );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}
	}

	private class CustomCombatPanel extends LabeledScrollPanel
	{
		public CustomCombatPanel()
		{
			super( "Custom Combat", "save", "help", new JTextArea( 12, 40 ) );

			try
			{
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
				((JTextArea)scrollComponent).setText( buffer.toString() );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e );
			}

			refreshCombatTree();
		}

		protected void actionConfirmed()
		{
			try
			{
				PrintStream writer = new PrintStream( new FileOutputStream( DATA_DIRECTORY + CombatSettings.settingsFileName() ) );
				writer.println( ((JTextArea)scrollComponent).getText() );
				writer.close();
				writer = null;

				int customIndex = KoLCharacter.getBattleSkillIDs().indexOf( "custom" );
				KoLCharacter.getBattleSkillIDs().setSelectedIndex( customIndex );
				KoLCharacter.getBattleSkillNames().setSelectedIndex( customIndex );
				setProperty( "battleAction", "custom" );
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
			tabs.setSelectedIndex(3);
		}

		protected void actionCancelled()
		{	StaticEntity.openSystemBrowser( "http://kolmafia.sourceforge.net/combat.html" );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * displaying custom combat.
	 */

	private void refreshCombatTree()
	{
		CombatSettings.reset();
		displayModel.setRoot( CombatSettings.getRoot() );
		displayTree.setRootVisible( false );
	}
}
