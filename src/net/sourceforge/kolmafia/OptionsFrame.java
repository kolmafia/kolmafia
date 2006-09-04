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
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// events
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeModel;

// containers
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.JButton;

// utilities
import java.io.File;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.FileOutputStream;

import java.util.Arrays;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * <p>Handles all of the customizable user options in <code>KoLmafia</code>.
 * This class presents all of the options that the user can customize
 * in their adventuring and uses the appropriate <code>KoLSettings</code>
 * in order to display them.  This class also uses <code>KoLSettings</code>
 * to record the user's preferences for upcoming sessions.</p>
 *
 * <p>If this class is accessed before login, it will modify global settings
 * ONLY, and if the character already has settings, any modification of
 * global settings will not modify their own.  Accessing this class after
 * login will result in modification of the character's own settings ONLY,
 * and will not modify any global settings.</p>
 *
 * <p>Proxy settings are a special exception to this rule - because the
 * Java Virtual Machine requires the proxy settings to be specified at
 * a global level, though the settings are changed appropriately on disk,
 * only the most recently loaded settings will be active on the current
 * instance of the JVM.  If separate characters need separate proxies,
 * they cannot be run in the same JVM instance.</p>
 */

public class OptionsFrame extends KoLFrame
{
	private JTree combatTree;
	private JTextArea combatEditor;
	private DefaultTreeModel combatModel;
	private CardLayout combatCards;
	private JPanel combatPanel;

	private JList moodList;

	private JComboBox battleStopSelect;
	private JTextField betweenBattleScriptField;

	private JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect;
	private JCheckBox [] hpRestoreCheckbox;

	private JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect;
	private JCheckBox [] mpRestoreCheckbox;


	/**
	 * Constructs a new <code>OptionsFrame</code> that will be
	 * associated with the given client.  When this frame is
	 * closed, it will attempt to return focus to the currently
	 * active frame; note that if this is done while the client
	 * is shuffling active frames, closing the window will not
	 * properly transfer focus.
	 */

	public OptionsFrame()
	{
		super( "Preferences" );
		tabs = new JTabbedPane();

		// Components of the general tab

		JPanel generalPanel = new JPanel();
		BoxLayout generalLayout = new BoxLayout( generalPanel, BoxLayout.Y_AXIS );
		generalPanel.setLayout( generalLayout );

		generalPanel.add( new GeneralOptionsPanel() );
		generalPanel.add( new ItemOptionsPanel() );
		generalPanel.add( new RelayOptionsPanel() );
		generalPanel.add( new AreaOptionsPanel() );

		// Components of restoration

		JPanel restorePanel = new JPanel();
		restorePanel.setLayout( new BoxLayout( restorePanel, BoxLayout.Y_AXIS ) );

		restorePanel.add( new HealthOptionsPanel() );
		restorePanel.add( new ManaOptionsPanel() );

		// Components of custom combat

		CheckboxListener listener = new CheckboxListener();
		for ( int i = 0; i < hpRestoreCheckbox.length; ++i )
			hpRestoreCheckbox[i].addActionListener( listener );
		for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
			mpRestoreCheckbox[i].addActionListener( listener );

		combatTree = new JTree();
		combatModel = (DefaultTreeModel) combatTree.getModel();

		combatCards = new CardLayout();
		combatPanel = new JPanel( combatCards );
		combatPanel.add( "tree", new CustomCombatTreePanel() );
		combatPanel.add( "editor", new CustomCombatPanel() );

		JPanel moodPanel = new JPanel( new BorderLayout() );
		moodPanel.add( new AddTriggerPanel(), BorderLayout.NORTH );
		moodPanel.add( new MoodTriggerListPanel(), BorderLayout.CENTER );

		addTab( "General", generalPanel );
		tabs.addTab( "Scriptbar", new ScriptButtonPanel() );
		addTab( "Choices", new ChoiceOptionsPanel() );
		addTab( "Restores", restorePanel );
		tabs.addTab( "Combats", combatPanel );
		tabs.addTab( "Moods", moodPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
	}

	private void addTab( String name, JComponent panel )
	{
		JScrollPane scroller = new JScrollPane( panel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
		JComponentUtilities.setComponentSize( scroller, 560, 400 );
		tabs.add( name, scroller );
	}

	private class RelayOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "relayAddsUseLinks", "Add [use] links when acquiring items" },
			{ "relayAddsScriptList", "Add quick scripts list to relay browser" },
			{ "relayAddsCommandLineLinks", "Add gCLI tool links to chat launcher" },
			{ "relayAddsSimulatorLinks", "Add Ayvuir's Simulator of Loathing link" },
			{ "relayAddsPlinking", "Add plinking-friendly button to attack screen" },
			{ "relayAddsShrugOffLinks", "Add shrug off links to effect duration in side pane" },
			{ "makeBrowserDecisions", "Browser modules automatically make decisions" },
			{ "cacheRelayImages", "Cache images seen in relay browser for mini browser" },
			{ "trackLocationChanges", "Adventuring in browser changes selected adventure location" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public RelayOptionsPanel()
		{
			super( "Relay Browser", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );
		}

		protected void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}
	}

	private class GeneralOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "allowGenericUse", "Have use be a generic consumption command" },
			{ "showAllRequests", "Show requests synchronously in mini-browser" },
			{ "defaultToRelayBrowser", "Browser shortcut button loads relay browser" },
			{ "sortAdventures", "Sort adventure list display by moxie evade rating" },
			{ "areaValidation", "Enable stat checks before using adventures" },
			{ "autoSetConditions", "Auto-set conditions field when selecting areas" },
			{ "allowStasisTactics", "Allow stasis-type commands when using combat familiars" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public GeneralOptionsPanel()
		{
			super( "General Options", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			actionCancelled();
			KoLCharacter.refreshCalculatedLists();
			AdventureDatabase.refreshAdventureList();
		}

		protected void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}
	}

	private class ItemOptionsPanel extends OptionsPanel
	{
		private JCheckBox [] optionBoxes;

		private final String [][] options =
		{
			{ "autoCheckpoint", "Allow outfit checkpointing during NPC store purchases" },
			{ "assumeInfiniteNPCItems", "Assume infinite NPC items for item creation" },

			{ "createWithoutBoxServants", "Create without requiring a box servant" },
			{ "autoRepairBoxes", "Create and install new box servant after explosion" },

			{ "autoSatisfyWithMall", "Buy items from the mall whenever needed" },
			{ "autoSatisfyWithNPCs", "Buy items from NPC stores whenever needed" },
			{ "autoSatisfyWithStash", "Take items from the clan stash whenever needed" }
		};

		/**
		 * Constructs a new <code>StartupOptionsPanel</code>, containing a
		 * place for the users to select their desired server and for them
		 * to modify any applicable proxy settings.
		 */

		public ItemOptionsPanel()
		{
			super( "Item Options", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );
			VerifiableElement [] elements = new VerifiableElement[ options.length ];

			optionBoxes = new JCheckBox[ options.length ];
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i] = new JCheckBox();

			for ( int i = 0; i < options.length; ++i )
				elements[i] = new VerifiableElement( options[i][1], JLabel.LEFT, optionBoxes[i] );

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			for ( int i = 0; i < options.length; ++i )
				StaticEntity.setProperty( options[i][0], String.valueOf( optionBoxes[i].isSelected() ) );

			actionCancelled();
			KoLCharacter.refreshCalculatedLists();
		}

		protected void actionCancelled()
		{
			for ( int i = 0; i < options.length; ++i )
				optionBoxes[i].setSelected( StaticEntity.getBooleanProperty( options[i][0] ) );
		}
	}

	private class AreaOptionsPanel extends OptionsPanel
	{
		private String [] zones;
		private JCheckBox [] options;

		public AreaOptionsPanel()
		{
			super( "Adventure List", new Dimension( 370, 16 ), new Dimension( 20, 16 ) );

			zones = new String[ AdventureDatabase.ZONE_NAMES.size() ];
			options = new JCheckBox[ AdventureDatabase.ZONE_NAMES.size() ];

			for ( int i = 0; i < options.length; ++i )
				options[i] = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[ AdventureDatabase.ZONE_NAMES.size() ];
			String [] names = new String[ AdventureDatabase.ZONE_NAMES.keySet().size() ];
			AdventureDatabase.ZONE_NAMES.keySet().toArray( names );

			for ( int i = 0; i < names.length; ++i )
			{
				zones[i] = (String) AdventureDatabase.ZONE_NAMES.get( names[i] );
				elements[i] = new VerifiableElement( "Hide " + AdventureDatabase.ZONE_DESCRIPTIONS.get( names[i] ), JLabel.LEFT, options[i] );
			}

			setContent( elements, false );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			StringBuffer areas = new StringBuffer();

			for ( int i = 0; i < options.length; ++i )
			{
				if ( options[i].isSelected() )
				{
					if ( areas.length() != 0 )
						areas.append( ',' );

					areas.append( zones[i] );
				}
			}

			StaticEntity.setProperty( "zoneExcludeList", areas.toString() );
			super.actionConfirmed();
			AdventureDatabase.refreshAdventureList();
		}

		protected void actionCancelled()
		{
			String excluded = StaticEntity.getProperty( "zoneExcludeList" );
			for ( int i = 0; i < zones.length; ++i )
				options[i].setSelected( excluded.indexOf( zones[i] ) != -1 );
		}
	}

	private class ScriptButtonPanel extends ItemManagePanel implements ListDataListener
	{
		private LockableListModel scriptList;

		public ScriptButtonPanel()
		{
			super( "gCLI Toolbar Buttons", "new script", "new command", new LockableListModel(), true, true );

			scriptList = (LockableListModel) elementList.getModel();
			scriptList.addListDataListener( this );

			((JList)scrollComponent).addKeyListener( new RemoveScriptListener() );

			String [] scriptList = StaticEntity.getProperty( "scriptList" ).split( " \\| " );

			for ( int i = 0; i < scriptList.length; ++i )
				this.scriptList.add( scriptList[i] );
		}

		public void actionConfirmed()
		{
			String rootPath = SCRIPT_DIRECTORY.getAbsolutePath();
			JFileChooser chooser = new JFileChooser( rootPath );
			int returnVal = chooser.showOpenDialog( null );

			if ( chooser.getSelectedFile() == null )
				return;

			if ( returnVal == JFileChooser.APPROVE_OPTION )
			{
				String scriptPath = chooser.getSelectedFile().getAbsolutePath();
				if ( scriptPath.startsWith( rootPath ) )
					scriptPath = scriptPath.substring( rootPath.length() + 1 );

				scriptList.add( "call " + scriptPath );
			}
		}

		public void actionCancelled()
		{
			String currentValue = JOptionPane.showInputDialog( "CLI Command", "" );
			if ( currentValue != null && currentValue.length() != 0 )
				scriptList.add( currentValue );
		}

		private class RemoveScriptListener extends KeyAdapter
		{
			public void keyPressed( KeyEvent e )
			{
				if ( e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE )
				{
					Object [] values = ((JList)scrollComponent).getSelectedValues();
					for ( int i = 0; i < values.length; ++i )
						scriptList.remove( values[i] );
				}
			}
		}

		public void intervalAdded( ListDataEvent e )
		{	saveSettings();
		}

		public void intervalRemoved( ListDataEvent e )
		{	saveSettings();
		}

		public void contentsChanged( ListDataEvent e )
		{	saveSettings();
		}

		private void saveSettings()
		{
			StringBuffer settingString = new StringBuffer();
			if ( scriptList.size() != 0 )
				settingString.append( (String) scriptList.get(0) );

			for ( int i = 1; i < scriptList.size(); ++i )
			{
				settingString.append( " | " );
				settingString.append( (String) scriptList.get(i) );
			}

			StaticEntity.setProperty( "scriptList", settingString.toString() );
		}
	}

	/**
	 * This panel allows the user to select which item they would like
	 * to do for each of the different choice adventures.
	 */

	private class ChoiceOptionsPanel extends KoLPanel
	{
		private JComboBox [] optionSelects;

		private JComboBox cloverProtectSelect;
		private JComboBox castleWheelSelect;
		private JComboBox spookyForestSelect;
		private JComboBox tripTypeSelect;
		private JComboBox violetFogSelect;

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

			cloverProtectSelect = new JComboBox();
			cloverProtectSelect.addItem( "Disassemble ten-leaf clovers" );
			cloverProtectSelect.addItem( "Leave ten-leaf clovers alone" );

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

			VerifiableElement [] elements = new VerifiableElement[ optionSelects.length + 7 ];
			elements[0] = new VerifiableElement( "Clover Protect", cloverProtectSelect );
			elements[1] = new VerifiableElement( "", new JLabel() );
			elements[2] = new VerifiableElement( "Castle Wheel", castleWheelSelect );
			elements[3] = new VerifiableElement( "Forest Corpses", spookyForestSelect );
			elements[4] = new VerifiableElement( "Violet Fog 1", tripTypeSelect );
			elements[5] = new VerifiableElement( "Violet Fog 2", violetFogSelect );
			elements[6] = new VerifiableElement( "Lucky Sewer", optionSelects[0] );

			elements[7] = new VerifiableElement( "", new JLabel() );
			for ( int i = 1; i < optionSelects.length; ++i )
				elements[i+7] = new VerifiableElement( AdventureDatabase.CHOICE_ADVS[i][1][0], optionSelects[i] );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{
			StaticEntity.setProperty( "cloverProtectActive", String.valueOf( cloverProtectSelect.getSelectedIndex() == 0 ) );
			StaticEntity.setProperty( "violetFogGoal", String.valueOf( violetFogSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "choiceAdventure71", String.valueOf( tripTypeSelect.getSelectedIndex() + 1 ) );
			StaticEntity.setProperty( "luckySewerAdventure", (String) optionSelects[0].getSelectedItem() );

			for ( int i = 1; i < optionSelects.length; ++i )
			{
				int index = optionSelects[i].getSelectedIndex();
				String choice = AdventureDatabase.CHOICE_ADVS[i][0][0];
				boolean ignorable = AdventureDatabase.ignoreChoiceOption( choice ) != null;

				if ( ignorable || index != 0 )
					StaticEntity.setProperty( choice, String.valueOf( index ) );
				else
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
		}

		protected void actionCancelled()
		{
			cloverProtectSelect.setSelectedIndex( StaticEntity.getBooleanProperty( "cloverProtectActive" ) ? 0 : 1 );
			violetFogSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "violetFogGoal" ) );

			for ( int i = 1; i < optionSelects.length; ++i )
				optionSelects[i].setSelectedIndex( StaticEntity.getIntegerProperty( AdventureDatabase.CHOICE_ADVS[i][0][0] ) );

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

			int index = 0;

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

			castleWheelSelect.setSelectedIndex( index );

			// Now, determine what is located in choice adventure #26,
			// which shows you which slot (in general) to use.

			index = StaticEntity.getIntegerProperty( "choiceAdventure26" );
			index = index * 2 + StaticEntity.getIntegerProperty( "choiceAdventure" + (26 + index) ) - 3;

			spookyForestSelect.setSelectedIndex( index < 0 ? 5 : index );
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
		StaticEntity.setProperty( "betweenBattleScript", betweenBattleScriptField.getText() );
		StaticEntity.setProperty( "battleStop", String.valueOf( ((float)(battleStopSelect.getSelectedIndex() - 1) / 10.0f) ) );

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
			battleStopSelect.addItem( "Never stop combat" );
			for ( int i = 0; i <= 9; ++i )
				battleStopSelect.addItem( "Autostop at " + (i*10) + "% HP" );

			betweenBattleScriptField = new JTextField();

			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover HP" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverSelect.addItem( "Autorecover HP at " + (i * 10) + "%" );

			hpAutoRecoverTargetSelect = new JComboBox();
			hpAutoRecoverTargetSelect.addItem( "Do not autorecover HP" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverTargetSelect.addItem( "Autorecover HP to " + (i * 10) + "%" );

			// Add the elements to the panel

			int currentElementCount = 0;
			VerifiableElement [] elements = new VerifiableElement[6];

			elements[ currentElementCount++ ] = new VerifiableElement( "Script Command: ", betweenBattleScriptField );
			elements[ currentElementCount++ ] = new VerifiableElement( "Combat Abort: ", battleStopSelect );

			elements[ currentElementCount++ ] = new VerifiableElement( "", new JLabel() );

			elements[ currentElementCount++ ] = new VerifiableElement( "HP Recovery Trigger: ", hpAutoRecoverSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "HP Recovery Target: ", hpAutoRecoverTargetSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "Use these restores: ", constructScroller( hpRestoreCheckbox = HPRestoreItemList.getCheckboxes() ) );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{	saveRestoreSettings();
		}

		protected void actionCancelled()
		{
			betweenBattleScriptField.setText( StaticEntity.getProperty( "betweenBattleScript" ) );
			battleStopSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "battleStop" ) * 10) + 1 );
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
			mpAutoRecoverSelect.addItem( "Do not autorecover MP" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverSelect.addItem( "Autorecover MP at " + (i * 10) + "%" );

			mpAutoRecoverTargetSelect = new JComboBox();
			mpAutoRecoverTargetSelect.addItem( "Do not autorecover MP" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverTargetSelect.addItem( "Autorecover MP to " + (i * 10) + "%" );

			// Add the elements to the panel

			int currentElementCount = 0;
			VerifiableElement [] elements = new VerifiableElement[3];

			elements[ currentElementCount++ ] = new VerifiableElement( "MP Recovery Trigger: ", mpAutoRecoverSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "MP Recovery Target: ", mpAutoRecoverTargetSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "Use these restores: ", constructScroller( mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) );

			setContent( elements );
			actionCancelled();
		}

		protected void actionConfirmed()
		{	saveRestoreSettings();
		}

		protected void actionCancelled()
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

	private class CustomCombatPanel extends LabeledScrollPanel
	{
		public CustomCombatPanel()
		{
			super( "Editor", "save", "help", new JTextArea( 12, 40 ) );
			combatEditor = (JTextArea) scrollComponent;
			refreshCombatSettings();
		}

		protected void actionConfirmed()
		{
			try
			{
				PrintStream writer = new LogStream( DATA_DIRECTORY + CombatSettings.settingsFileName() );
				writer.println( ((JTextArea)scrollComponent).getText() );
				writer.close();
				writer = null;

				int customIndex = KoLCharacter.getBattleSkillIDs().indexOf( "custom" );
				KoLCharacter.getBattleSkillIDs().setSelectedIndex( customIndex );
				KoLCharacter.getBattleSkillNames().setSelectedIndex( customIndex );
				StaticEntity.setProperty( "battleAction", "custom" );
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

		protected void actionCancelled()
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
			JFileChooser chooser = new JFileChooser( (new File( "data" )).getAbsolutePath() );
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
			return !name.startsWith( "." ) && name.endsWith( ".ccs" );
		}

		public String getDescription()
		{	return "Custom Combat Settings (*.ccs)";
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

		protected void actionConfirmed()
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
		private CopyMoodButton moodCopy;

		public MoodTriggerListPanel()
		{

			super( "", "new list", "remove", new JList( MoodSettings.getTriggers() ) );

			moodSelect = new MoodComboBox();
			moodCopy = new CopyMoodButton();

			actualPanel.add( moodSelect, BorderLayout.NORTH );
			moodList = (JList) scrollComponent;
			buttonPanel.add(moodCopy, BorderLayout.SOUTH);
		}

		public void actionConfirmed()
		{
			String name = JOptionPane.showInputDialog( "Give your list a name!" );
			if ( name == null )
				return;

			moodList.setModel( MoodSettings.setMood( name ) );
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
			{	moodList.setModel( MoodSettings.setMood( (String) getSelectedItem() ) );
			}
		}

		private class CopyMoodButton extends JButton implements ActionListener
		{
			public CopyMoodButton()
			{
				super ( "copy list" );
				addActionListener(this);
			}
			public void actionPerformed (ActionEvent e)
			{
				String moodName = JOptionPane.showInputDialog("Make a copy of current mood list called:");
				if (moodName == null)
					return;

				if ( moodName.equals("default"))
					return;

				MoodSettings.copyTriggers(moodName);
				moodList.setModel(MoodSettings.setMood(moodName));
			}
		}
	}
}
