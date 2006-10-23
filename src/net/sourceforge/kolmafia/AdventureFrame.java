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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.SpringLayout;
import com.sun.java.forums.SpringUtilities;

// event listeners
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.AbstractSpinnerModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;

// containers
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.JTabbedPane;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.Box;
import javax.swing.JSpinner;

// utilities
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import java.io.File;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

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
	private JTree combatTree;
	private JTextArea combatEditor;
	private DefaultTreeModel combatModel;
	private CardLayout combatCards;
	private JPanel combatPanel;

	private JComboBox zoneSelect;
	private JList locationSelect;
	private JComboBox dropdown1, dropdown2;
	private AdventureSelectPanel adventureSelect;

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

		// Construct the adventure select container
		// to hold everything related to adventuring.

		this.adventureSelect = new AdventureSelectPanel();

		JPanel adventureDetails = new JPanel( new BorderLayout( 20, 20 ) );
		adventureDetails.add( adventureSelect, BorderLayout.CENTER );

		JPanel meterPanel = new JPanel( new BorderLayout( 10, 10 ) );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.WEST );
		meterPanel.add( KoLmafia.requestMeter, BorderLayout.CENTER );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.EAST );

		adventureDetails.add( meterPanel, BorderLayout.SOUTH );

		framePanel.setLayout( new BorderLayout( 20, 20 ) );
		framePanel.add( adventureDetails, BorderLayout.NORTH );
		framePanel.add( getSouthernTabs(), BorderLayout.CENTER );

		KoLAdventure location = AdventureDatabase.getAdventure( StaticEntity.getProperty( "lastAdventure" ) );
		if ( location != null )
		{
			zoneSelect.setSelectedItem( AdventureDatabase.ZONE_DESCRIPTIONS.get( location.getParentZone() ) );
			locationSelect.setSelectedValue( location, true );
		}

		JComponentUtilities.setComponentSize( framePanel, 640, 480 );
	}

	public boolean useSidePane()
	{	return true;
	}

	private JPanel constructLabelPair( String label, JComponent element )
	{
		JPanel container = new JPanel( new BorderLayout() );

		if ( element instanceof JComboBox )
			JComponentUtilities.setComponentSize( element, 240, 20 );

		container.add( new JLabel( "<html><b>" + label + "</b></html>", JLabel.LEFT ), BorderLayout.NORTH );
		container.add( element, BorderLayout.CENTER );
		return container;
	}

	private JTabbedPane getSouthernTabs()
	{
		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		// Handle everything that might appear inside of the
		// session tally.

		JPanel sessionGrid = new JPanel( new GridLayout( 1, 2, 10, 10 ) );
		sessionGrid.add( getAdventureSummary( StaticEntity.getIntegerProperty( "defaultDropdown1" ) ) );
		sessionGrid.add( getAdventureSummary( StaticEntity.getIntegerProperty( "defaultDropdown2" ) ) );

		tabs.addTab( "Normal Options", sessionGrid );

		// Components of auto-restoration

		JPanel restorePanel = new JPanel( new GridLayout( 1, 2, 10, 10 ) );

		JPanel healthPanel = new JPanel();
		healthPanel.add( new HealthOptionsPanel() );

		JPanel manaPanel = new JPanel();
		manaPanel.add( new ManaOptionsPanel() );

		restorePanel.add( healthPanel );
		restorePanel.add( manaPanel );

		CheckboxListener listener = new CheckboxListener();
		for ( int i = 0; i < hpRestoreCheckbox.length; ++i )
			hpRestoreCheckbox[i].addActionListener( listener );
		for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
			mpRestoreCheckbox[i].addActionListener( listener );

		// Components of custom combat and choice adventuring,
		// combined into one friendly panel.

		combatTree = new JTree();
		combatModel = (DefaultTreeModel) combatTree.getModel();

		combatCards = new CardLayout();
		combatPanel = new JPanel( combatCards );
		combatPanel.add( "tree", new CustomCombatTreePanel() );
		combatPanel.add( "editor", new CustomCombatPanel() );

		addTab( "Choice Adventures", new ChoiceOptionsPanel() );
		tabs.addTab( "Combat Adventures", combatPanel );
		addTab( "Auto Recovery", restorePanel );

		return tabs;
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

	private class SafetyField extends JPanel implements Runnable, ListSelectionListener
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
			locationSelect.addListSelectionListener( this );

			setSafetyString();
		}

		public void run()
		{	setSafetyString();
		}

		public void valueChanged( ListSelectionEvent e )
		{	setSafetyString();
		}

		private void setSafetyString()
		{
			KoLAdventure request = (KoLAdventure) locationSelect.getSelectedValue();
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

	private class AdventureSelectPanel extends JPanel
	{
		private JComboBox moodSelect;
		private JComboBox actionSelect;
		private TreeMap zoneMap;
		private JSpinner countField;
		private JTextField conditionField;

		public AdventureSelectPanel()
		{
			super( new BorderLayout( 10, 10 ) );

			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();

			// West pane is a scroll pane which lists all of the available
			// locations -- to be included is a map on a separate tab.

			Object currentZone;
			zoneMap = new TreeMap();

			zoneSelect = new JComboBox();
			zoneSelect.addItem( "All Locations" );

			Object [] zones = AdventureDatabase.PARENT_LIST.toArray();

			for ( int i = 0; i < zones.length; ++i )
			{
				currentZone = AdventureDatabase.ZONE_DESCRIPTIONS.get( zones[i] );
				zoneMap.put( currentZone, zones[i] );
				zoneSelect.addItem( currentZone );
			}

			countField = new JSpinner();

			JComponentUtilities.setComponentSize( countField, 50, 24 );
			JComponentUtilities.setComponentSize( zoneSelect, 200, 24 );

			JPanel zonePanel = new JPanel( new BorderLayout( 5, 5 ) );
			zonePanel.add( countField, BorderLayout.EAST );
			zonePanel.add( zoneSelect, BorderLayout.CENTER );

			zoneSelect.addActionListener( new ZoneChangeListener() );

			locationSelect = new JList( adventureList );
			locationSelect.setVisibleRowCount( 4 );

			JPanel locationPanel = new JPanel( new BorderLayout( 5, 5 ) );
			locationPanel.add( zonePanel, BorderLayout.NORTH );
			locationPanel.add( new SimpleScrollPane( locationSelect ), BorderLayout.CENTER );

			JPanel westPanel = new JPanel( new CardLayout( 20, 20 ) );
			westPanel.add( locationPanel, "" );

			add( westPanel, BorderLayout.WEST );
			add( new ObjectivesPanel(), BorderLayout.CENTER );
		}

		private class ObjectivesPanel extends KoLPanel
		{
			public ObjectivesPanel()
			{
				super( new Dimension( 70, 20 ), new Dimension( 100, 20 ) );

				actionSelect = new JComboBox( KoLCharacter.getBattleSkillNames() );
				moodSelect = new JComboBox( MoodSettings.getAvailableMoods() );

				conditionField = new JTextField();
				locationSelect.addListSelectionListener( new ConditionChangeListener() );

				JPanel buttonPanel = new JPanel();
				buttonPanel.add( new ExecuteButton() );
				buttonPanel.add( new WorldPeaceButton() );

				JPanel buttonWrapper = new JPanel( new BorderLayout() );
				buttonWrapper.add( buttonPanel, BorderLayout.EAST );

				VerifiableElement [] elements = new VerifiableElement[3];
				elements[0] = new VerifiableElement( "In Combat:  ", actionSelect );
				elements[1] = new VerifiableElement( "Use Mood:  ", moodSelect );
				elements[2] = new VerifiableElement( "Objectives:  ", conditionField );

				setContent( elements );
				container.add( buttonWrapper, BorderLayout.SOUTH );
			}

			public void actionConfirmed()
			{
				if ( actionSelect.getSelectedIndex() != -1 )
				{
					KoLmafia.forceContinue();
					DEFAULT_SHELL.executeLine( "set battleAction=" + actionSelect.getSelectedItem() );
				}

				MoodSettings.setMood( (String) moodSelect.getSelectedItem() );
			}

			public void actionCancelled()
			{
			}

			protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
			{	return false;
			}

			public void setEnabled( boolean isEnabled )
			{
			}
		}

		private class ExecuteButton extends ThreadedActionButton
		{
			public ExecuteButton()
			{	super( "begin" );
			}

			public void executeTask()
			{
				Runnable request = (Runnable) locationSelect.getSelectedValue();
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
								return;

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

					if ( ((Integer)countField.getValue()).intValue() == 0 )
						countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );

					if ( !StaticEntity.getBooleanProperty( "autoSetConditions" ) )
						conditionField.setText( "" );
				}

				int requestCount = Math.min( getValue( countField, 1 ), KoLCharacter.getAdventuresLeft() );
				countField.setValue( new Integer( requestCount ) );

				(new RequestThread( request, requestCount )).start();
			}

			public boolean makesRequest()
			{	return false;
			}
		}

		private class ZoneChangeListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( zoneSelect.getSelectedIndex() == 0 )
				{
					AdventureDatabase.refreshAdventureList();
					return;
				}

				String zone = (String) zoneSelect.getSelectedItem();

				if ( zone == null )
					return;

				zone = (String) zoneMap.get( zone );
				if ( zone == null )
					return;

				AdventureDatabase.refreshAdventureList( zone );
			}
		}

		private class ConditionChangeListener implements ListSelectionListener
		{
			public void valueChanged( ListSelectionEvent e )
			{
				conditions.clear();

				if ( !StaticEntity.getBooleanProperty( "autoSetConditions" ) )
					return;

				KoLAdventure location = (KoLAdventure) locationSelect.getSelectedValue();
				if ( location == null )
					return;

				conditionField.setText( AdventureDatabase.getCondition( location ) );
			}
		}

		private class WorldPeaceButton extends JButton implements ActionListener
		{
			public WorldPeaceButton()
			{
				super( "stop" );
				addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{	KoLmafia.declareWorldPeace();
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
		{	this( null, resultList, 11 );
		}

		public AdventureResultsPanel( String header, LockableListModel resultList, int rowCount )
		{
			setLayout( new BorderLayout() );

			ShowDescriptionList tallyDisplay = new ShowDescriptionList( resultList );
			tallyDisplay.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
			tallyDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			tallyDisplay.setVisibleRowCount( rowCount );

			add( new SimpleScrollPane( tallyDisplay ), BorderLayout.CENTER );

			if ( header != null )
				add( JComponentUtilities.createLabel( header, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
		}
	}

	private class CustomCombatPanel extends LabeledScrollPanel
	{
		public CustomCombatPanel()
		{
			super( "Editor", "save", "help", new JTextArea() );
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
		{	super( "Custom Combat", "edit", "load", combatTree );
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

	private class ChoiceOptionsPanel extends JPanel
	{
		private TreeMap choiceMap;
		private TreeMap selectMap;
		private CardLayout choiceCards;

		private JComboBox [] optionSelects;
		private JComboBox castleWheelSelect;
		private JComboBox spookyForestSelect;
		private JComboBox violetFogSelect;
		private JComboBox louvreSelect;
		private JComboBox billiardRoomSelect;
		private JComboBox librarySelect;

		/**
		 * Constructs a new <code>ChoiceOptionsPanel</code>.
		 */

		public ChoiceOptionsPanel()
		{
			choiceCards = new CardLayout( 10, 10 );

			choiceMap = new TreeMap();
			selectMap = new TreeMap();

			this.setLayout( choiceCards );
			add( new JPanel(), "" );

			optionSelects = new JComboBox[ AdventureDatabase.CHOICE_ADVS.length ];
			for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
			{
				optionSelects[i] = new JComboBox();

				boolean ignorable = AdventureDatabase.ignoreChoiceOption( AdventureDatabase.CHOICE_ADVS[i].getSetting() ) != null;
				optionSelects[i].addItem( ignorable ? "Ignore this adventure" : "Make semi-random decision" );

				String [] options = AdventureDatabase.CHOICE_ADVS[i].getOptions();
				for ( int j = 0; j < options.length; ++j )
					optionSelects[i].addItem( options[j] );
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

			librarySelect = new JComboBox();
			librarySelect.addItem( "Ignore this adventure" );
			librarySelect.addItem( "Muscle" );
			librarySelect.addItem( "Mysticality" );
			librarySelect.addItem( "Moxie" );
			librarySelect.addItem( "Gallery Key" );
			librarySelect.addItem( "Mysticality Class Skill" );

			addChoiceSelect( "Beanstalk", "Castle Wheel", castleWheelSelect );
			addChoiceSelect( "Town", "Lucky Sewer", optionSelects[0] );
			addChoiceSelect( "Woods", "Forest Corpses", spookyForestSelect );
			addChoiceSelect( "Astral", "Violet Fog", violetFogSelect );
			addChoiceSelect( "Manor", "Billiard Room", billiardRoomSelect );
			addChoiceSelect( "Manor", "The Louvre", louvreSelect );

			for ( int i = 1; i < optionSelects.length; ++i )
				addChoiceSelect( AdventureDatabase.CHOICE_ADVS[i].getZone(), AdventureDatabase.CHOICE_ADVS[i].getName(), optionSelects[i] );

			ArrayList options;
			Object [] keys = choiceMap.keySet().toArray();

			for ( int i = 0; i < keys.length; ++i )
			{
				options = (ArrayList) choiceMap.get( keys[i] );
				add( new ChoicePanel( options ), (String) keys[i] );
			}

			actionCancelled();
			locationSelect.addListSelectionListener( new UpdateChoicesListener() );
		}

		private void addChoiceSelect( String zone, String name, JComboBox option )
		{
			if ( !choiceMap.containsKey( zone ) )
				choiceMap.put( zone, new ArrayList() );

			ArrayList options = (ArrayList) choiceMap.get( zone );
			options.add( name );

			selectMap.put( name, option );
		}

		private class ChoicePanel extends KoLPanel
		{
			public ChoicePanel( ArrayList options )
			{
				super( new Dimension( 150, 20 ), new Dimension( 300, 20 ) );

				VerifiableElement [] elements = new VerifiableElement[ options.size() ];

				for ( int i = 0; i < options.size(); ++i )
					elements[i] = new VerifiableElement( options.get(i) + ":  ", (JComboBox) selectMap.get( options.get(i) ) );

				setContent( elements );
			}

			public void actionConfirmed()
			{	ChoiceOptionsPanel.this.actionConfirmed();
			}

			public void actionCancelled()
			{
			}

			protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
			{	return false;
			}
		}

		private class UpdateChoicesListener implements ListSelectionListener
		{
			public void valueChanged( ListSelectionEvent e )
			{
				KoLAdventure location = (KoLAdventure) locationSelect.getSelectedValue();
				if ( location == null )
					return;

				choiceCards.show( ChoiceOptionsPanel.this, choiceMap.containsKey( location.getParentZone() ) ? location.getParentZone() : "" );
			}
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "violetFogGoal", String.valueOf( violetFogSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "luckySewerAdventure", (String) optionSelects[0].getSelectedItem() );

			int louvreGoal = louvreSelect.getSelectedIndex();
			StaticEntity.setProperty( "choiceAdventure91",  String.valueOf( louvreGoal > 0 ? "1" : "2" ) );

			StaticEntity.setProperty( "louvreDesiredGoal", String.valueOf( louvreGoal ) );
			StaticEntity.setProperty( "louvreBoostsLowestStat", String.valueOf( louvreGoal > Louvre.LouvreGoals.length ) );

			for ( int i = 1; i < optionSelects.length; ++i )
			{
				int index = optionSelects[i].getSelectedIndex();
				String choice = AdventureDatabase.CHOICE_ADVS[i].getSetting();
				boolean ignorable = AdventureDatabase.ignoreChoiceOption( choice ) != null;

				if ( ignorable || index != 0 )
					StaticEntity.setProperty( choice, String.valueOf( index ) );
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

			switch ( librarySelect.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure80", "4" );
				StaticEntity.setProperty( "choiceAdventure81", "4" );
				StaticEntity.setProperty( "choiceAdventure87", "1" );
				StaticEntity.setProperty( "choiceAdventure88", "1" );
				break;

			case 1: // Muscle
				StaticEntity.setProperty( "choiceAdventure80", "4" );
				StaticEntity.setProperty( "choiceAdventure81", "3" );
				StaticEntity.setProperty( "choiceAdventure87", "2" );
				StaticEntity.setProperty( "choiceAdventure88", "1" );
				break;

			case 2: // Mysticality
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure81", "4" );
				StaticEntity.setProperty( "choiceAdventure87", "1" );
				StaticEntity.setProperty( "choiceAdventure88", "1" );
				break;

			case 3: // Moxie
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure81", "4" );
				StaticEntity.setProperty( "choiceAdventure87", "1" );
				StaticEntity.setProperty( "choiceAdventure88", "2" );
				break;

			case 4: // Gallery Key
				StaticEntity.setProperty( "choiceAdventure80", "4" );
				StaticEntity.setProperty( "choiceAdventure81", "1" );
				StaticEntity.setProperty( "choiceAdventure87", "2" );
				StaticEntity.setProperty( "choiceAdventure88", "1" );
				break;

			case 5: // Mysticality Class Skill
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure81", "4" );
				StaticEntity.setProperty( "choiceAdventure87", "1" );
				StaticEntity.setProperty( "choiceAdventure88", "3" );
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

			optionSelects[0].setSelectedItem( StaticEntity.getProperty( "luckySewerAdventure" ) );
			for ( int i = 1; i < optionSelects.length; ++i )
			{
				index = StaticEntity.getIntegerProperty( AdventureDatabase.CHOICE_ADVS[i].getSetting() );
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
			else if ( option3 == 11 )
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

			// Figure out what to do at the bookcases

			int library11 = StaticEntity.getIntegerProperty( "choiceAdventure80" );
			int library12 = StaticEntity.getIntegerProperty( "choiceAdventure88" );
			int library21 = StaticEntity.getIntegerProperty( "choiceAdventure81" );
			int library22 = StaticEntity.getIntegerProperty( "choiceAdventure87" );

			if ( library21 == 3 && library22 == 2 )
				index = 1;
			else if ( library21 == 1 && library22 == 2 )
				index = 4;
			else if ( library11 == 3 && library12 == 1 )
				index = 2;
			else if ( library11 == 3 && library12 == 2 )
				index = 3;
			else if ( library11 == 3 && library12 == 3 )
				index = 5;
			else
				index = 0;

			librarySelect.setSelectedIndex( index );
		}
	}

	private void saveRestoreSettings()
	{
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

	private class HealthOptionsPanel extends JPanel implements ActionListener
	{
		private boolean refreshSoon = false;

		public HealthOptionsPanel()
		{
			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover health" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverSelect.addItem( "Auto-recover health at " + (i*10) + "%" );

			hpAutoRecoverTargetSelect = new JComboBox();
			hpAutoRecoverTargetSelect.addItem( "Do not automatically recover health" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*10) + "% health" );

			// Add the elements to the panel

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( constructLabelPair( "Restore your health: ", hpAutoRecoverSelect ) );
			add( Box.createVerticalStrut( 5 ) );

			JComponentUtilities.setComponentSize( hpAutoRecoverTargetSelect, 240, 20 );
			add( hpAutoRecoverTargetSelect );
			add( Box.createVerticalStrut( 10 ) );

			add( constructLabelPair( "Use these restores: ", constructScroller( hpRestoreCheckbox = HPRestoreItemList.getCheckboxes() ) ) );

			hpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "hpAutoRecovery" ) * 10) + 1 );
			hpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "hpAutoRecoveryTarget" ) * 10) + 1 );

			hpAutoRecoverSelect.addActionListener( this );
			hpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < hpRestoreCheckbox.length; ++i )
				hpRestoreCheckbox[i].addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	saveRestoreSettings();
		}
	}

	private class ManaOptionsPanel extends JPanel implements ActionListener
	{
		public ManaOptionsPanel()
		{
			mpAutoRecoverSelect = new JComboBox();
			mpAutoRecoverSelect.addItem( "Do not automatically recover mana" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverSelect.addItem( "Auto-recover mana at " + (i*10) + "%" );

			mpAutoRecoverTargetSelect = new JComboBox();
			mpAutoRecoverTargetSelect.addItem( "Do not automatically recover mana" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*10) + "% mana" );

			// Add the elements to the panel

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( constructLabelPair( "Restore your mana: ", mpAutoRecoverSelect ) );
			add( Box.createVerticalStrut( 5 ) );

			JComponentUtilities.setComponentSize( mpAutoRecoverTargetSelect, 240, 20 );
			add( mpAutoRecoverTargetSelect );
			add( Box.createVerticalStrut( 10 ) );
			add( constructLabelPair( "Use these restores: ", constructScroller( mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) ) );

			mpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "mpAutoRecovery" ) * 10) + 1 );
			mpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "mpAutoRecoveryTarget" ) * 10) + 1 );

			mpAutoRecoverSelect.addActionListener( this );
			mpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
				mpRestoreCheckbox[i].addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	saveRestoreSettings();
		}
	}
}
