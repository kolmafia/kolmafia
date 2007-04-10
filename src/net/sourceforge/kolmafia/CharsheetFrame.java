/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.io.File;
import java.io.BufferedReader;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.tree.DefaultTreeModel;

import javax.swing.filechooser.FileFilter;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.MoodSettings.MoodTrigger;

public class CharsheetFrame extends KoLFrame
{
	private static CharsheetFrame INSTANCE = null;

	private JList locationSelect = null;
	private JComboBox zoneSelect = null;
	private KoLAdventure lastAdventure = null;

	private JTree combatTree;
	private JTextArea combatEditor;
	private DefaultTreeModel combatModel;
	private CardLayout combatCards;
	private JPanel combatPanel;

	private JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect, hpHaltCombatSelect;
	private JCheckBox [] hpRestoreCheckbox;
	private JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect, mpBalanceSelect;
	private JCheckBox [] mpRestoreCheckbox;

	private JLabel avatar;
	private JLabel [] statusLabel;
	private JProgressBar [] tnpDisplay;
	private JProgressBar levelMeter;

	private JList moodList;
	private KoLCharacterAdapter statusRefresher;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 */

	public CharsheetFrame()
	{
		super( "Player Status" );

		INSTANCE = this;
		framePanel.setLayout( new BorderLayout( 20, 20 ) );

		JPanel statusPanel = new JPanel( new BorderLayout( 20, 20 ) );

		this.avatar = new JLabel( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );

		statusPanel.add( createStatusPanel(), BorderLayout.CENTER );
		statusPanel.add( this.avatar, BorderLayout.WEST );

		JPanel statusContainer = new JPanel( new CardLayout( 10, 10 ) );
		statusContainer.add( statusPanel, "" );

		JPanel northPanel = new JPanel( new BorderLayout( 20, 20 ) );
		northPanel.add( statusContainer, BorderLayout.WEST );
		northPanel.add( new ItemManagePanel( tally ), BorderLayout.CENTER );

		framePanel.add( northPanel, BorderLayout.NORTH );
		framePanel.add( getSouthernTabs(), BorderLayout.CENTER );

		statusRefresher = new KoLCharacterAdapter( new StatusRefreshRunnable() );
		KoLCharacter.addCharacterListener( statusRefresher );

		statusRefresher.updateStatus();
		updateSelectedAdventure( AdventureDatabase.getAdventure( StaticEntity.getProperty( "lastAdventure" ) ) );
	}

	public boolean useSidePane()
	{	return true;
	}

	public void dispose()
	{
		KoLCharacter.removeCharacterListener( statusRefresher );
		super.dispose();
	}

	/**
	 * Utility method for creating a panel that displays the given label,
	 * using formatting if the values are different.
	 */

	private JPanel createValuePanel( String title, int displayIndex )
	{
		int index1 = 2 * displayIndex;
		int index2 = index1 + 1;

		statusLabel[ index1 ] = new JLabel( " ", JLabel.LEFT );
		statusLabel[ index1 ].setForeground( Color.BLUE );
		statusLabel[ index2 ] = new JLabel( " ", JLabel.LEFT );

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout( new BoxLayout( headerPanel, BoxLayout.X_AXIS ) );

		headerPanel.add( new JLabel( title + ":  ", JLabel.RIGHT ) );
		headerPanel.add( statusLabel[ index1 ] );
		headerPanel.add( statusLabel[ index2 ] );

		JPanel valuePanel = new JPanel( new BorderLayout( 2, 2 ) );
		valuePanel.add( headerPanel, BorderLayout.EAST );
		valuePanel.add( tnpDisplay[ displayIndex ], BorderLayout.SOUTH );

		return valuePanel;
	}

	/**
	 * Utility method for modifying a panel that displays the given label,
	 * using formatting if the values are different.
	 */

	private void refreshValuePanel( int displayIndex, int baseValue, int adjustedValue, int tillNextPoint )
	{
		int index1 = 2 * displayIndex;
		int index2 = index1 + 1;

		JLabel adjustedLabel = statusLabel[index1];
		JLabel baseLabel = statusLabel[index2];

		adjustedLabel.setText( COMMA_FORMAT.format( adjustedValue ) );
		baseLabel.setText( " (" + COMMA_FORMAT.format( baseValue ) + ")" );

		tnpDisplay[ displayIndex ].setMaximum( 2 * baseValue + 1 );
		tnpDisplay[ displayIndex ].setValue( 2 * baseValue + 1 - tillNextPoint );
		tnpDisplay[ displayIndex ].setString( COMMA_FORMAT.format( tnpDisplay[ displayIndex ].getValue() ) + " / " +
			COMMA_FORMAT.format( tnpDisplay[ displayIndex ].getMaximum() ) );
	}

	/**
	 * Utility method for creating a panel displaying the character's vital
	 * statistics, including a basic stat overview and available turns/meat.
	 *
	 * @return	a <code>JPanel</code> displaying the character's statistics
	 */

	private JPanel createStatusPanel()
	{
		JPanel statusLabelPanel = new JPanel();
		statusLabelPanel.setLayout( new BoxLayout( statusLabelPanel, BoxLayout.Y_AXIS ) );

		this.statusLabel = new JLabel[6];
		for ( int i = 0; i < 6; ++i )
			statusLabel[i] = new JLabel( " ", JLabel.CENTER );

		this.tnpDisplay = new JProgressBar[3];
		for ( int i = 0; i < 3; ++i )
		{
			tnpDisplay[i] = new JProgressBar();
			tnpDisplay[i].setValue( 0 );
			tnpDisplay[i].setStringPainted( true );
		}

		JPanel primeStatPanel = new JPanel( new GridLayout( 3, 1, 5, 5 ) );
		primeStatPanel.add( createValuePanel( "Muscle", 0 ) );
		primeStatPanel.add( createValuePanel( "Mysticality", 1 ) );
		primeStatPanel.add( createValuePanel( "Moxie", 2 ) );
		statusLabelPanel.add( primeStatPanel );

		return statusLabelPanel;
	}

	private class StatusRefreshRunnable implements Runnable
	{
		public void run()
		{
			StaticEntity.getClient().applyEffects();

			refreshValuePanel( 0, KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() );
			refreshValuePanel( 1, KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(), KoLCharacter.getMysticalityTNP() );
			refreshValuePanel( 2, KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() );

			// Set the current avatar
			avatar.setIcon( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );
		}
	}

	public static void updateSelectedAdventure( KoLAdventure location )
	{
		if ( INSTANCE == null || location == null || INSTANCE.zoneSelect == null || INSTANCE.locationSelect == null )
			return;

		if ( !conditions.isEmpty() )
			return;

		INSTANCE.zoneSelect.setSelectedItem( AdventureDatabase.ZONE_DESCRIPTIONS.get( location.getParentZone() ) );
		INSTANCE.locationSelect.setSelectedValue( location, true );
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

		JPanel locationDetails = new JPanel( new BorderLayout( 10, 10 ) );
		locationDetails.add( new AdventureSelectPanel(), BorderLayout.WEST );
		locationDetails.add( new SafetyField( locationSelect ), BorderLayout.CENTER );

		JPanel locationHolder = new JPanel( new CardLayout( 10, 10 ) );
		locationHolder.add( locationDetails, "" );

		tabs.addTab( "Location Details", locationHolder );

		SimpleScrollPane restoreScroller = new SimpleScrollPane( restorePanel );
		JComponentUtilities.setComponentSize( restoreScroller, 560, 300 );

		tabs.addTab( "HP/MP Maintenance", restoreScroller );

		JPanel moodPanel = new JPanel( new BorderLayout() );
		moodPanel.add( new MoodTriggerListPanel(), BorderLayout.CENTER );

		AddTriggerPanel triggers = new AddTriggerPanel();
		moodList.addListSelectionListener( triggers );
		moodPanel.add( triggers, BorderLayout.NORTH );

		tabs.addTab( "Mood Handling", moodPanel );
		tabs.addTab( "Custom Combat", combatPanel );

		return tabs;
	}

	public void requestFocus()
	{
		super.requestFocus();
		locationSelect.requestFocus();
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	private class AdventureSelectPanel extends JPanel implements ChangeListener
	{
		private TreeMap zoneMap;
		private JSpinner countField;

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
			countField.addChangeListener( this );

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
			add( locationPanel, BorderLayout.WEST );
		}

		public void stateChanged( ChangeEvent e )
		{
			int desired = getValue( countField, KoLCharacter.getAdventuresLeft() );
			if ( desired <= 0 )
				countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );
			else if ( desired > KoLCharacter.getAdventuresLeft() )
				countField.setValue( new Integer( 1 ) );
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

		public void requestFocus()
		{	locationSelect.requestFocus();
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
			File location = new File( "settings" + File.separator + CombatSettings.settingsFileName() );
			LogStream writer = LogStream.openStream( location, true );

			writer.println( ((JTextArea)scrollComponent).getText() );
			writer.close();
			writer = null;

			KoLCharacter.battleSkillNames.setSelectedItem( "custom combat script" );
			StaticEntity.setProperty( "battleAction", "custom combat script" );

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
		{
			super( "Custom Combat", "edit", "load", combatTree );
			combatTree.setVisibleRowCount( 8 );
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
		if ( KoLCharacter.baseUserName().equals( "GLOBAL" ) )
			return;

		try
		{
			CombatSettings.restoreDefaults();
			BufferedReader reader = KoLDatabase.getReader( "settings" + File.separator + CombatSettings.settingsFileName() );

			StringBuffer buffer = new StringBuffer();
			String line;

			while ( (line = reader.readLine()) != null )
			{
				buffer.append( line );
				buffer.append( System.getProperty( "line.separator" ) );
			}

			reader.close();
			reader = null;

			// If the buffer is empty, add in the default settings.

			if ( buffer.length() == 0 )
				buffer.append( "[ default ]\n1: attack with weapon" );

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
		CombatSettings.restoreDefaults();
		combatModel.setRoot( CombatSettings.getRoot() );
		combatTree.setRootVisible( false );

		for ( int i = 0; i < combatTree.getRowCount(); ++i )
			combatTree.expandRow( i );
	}

	private void saveRestoreSettings()
	{
		StaticEntity.setProperty( "hpThreshold", String.valueOf( ((float)(hpHaltCombatSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "hpAutoRecovery", String.valueOf( ((float)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "hpAutoRecoveryTarget", String.valueOf( ((float)(hpAutoRecoverTargetSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "hpAutoRecoveryItems", getSettingString( hpRestoreCheckbox ) );

		StaticEntity.setProperty( "mpThreshold", String.valueOf( ((float)(mpBalanceSelect.getSelectedIndex() - 1) / 10.0f) ) );
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
			hpHaltCombatSelect = new JComboBox();
			hpHaltCombatSelect.addItem( "Stop automation if auto-recovery fails" );
			for ( int i = 0; i <= 10; ++i )
				hpHaltCombatSelect.addItem( "Stop automation if health at " + (i*10) + "%" );

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
			add( constructLabelPair( "Stop automation: ", hpHaltCombatSelect ) );
			add( Box.createVerticalStrut( 10 ) );

			add( constructLabelPair( "Restore your health: ", hpAutoRecoverSelect ) );
			add( Box.createVerticalStrut( 5 ) );

			JComponentUtilities.setComponentSize( hpAutoRecoverTargetSelect, 240, 20 );
			add( hpAutoRecoverTargetSelect );
			add( Box.createVerticalStrut( 10 ) );

			add( constructLabelPair( "Use these restores: ", constructScroller( hpRestoreCheckbox = HPRestoreItemList.getCheckboxes() ) ) );

			hpHaltCombatSelect.setSelectedIndex( Math.max( (int)(StaticEntity.getFloatProperty( "hpThreshold" ) * 10) + 1, 0 ) );
			hpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "hpAutoRecovery" ) * 10) + 1 );
			hpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "hpAutoRecoveryTarget" ) * 10) + 1 );

			hpHaltCombatSelect.addActionListener( this );
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
			mpBalanceSelect = new JComboBox();
			mpBalanceSelect.addItem( "Enable manual invocation of bulk recast" );
			for ( int i = 0; i <= 9; ++i )
				mpBalanceSelect.addItem( "Enable conditional recast at " + (i*10) + "%" );

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
			add( constructLabelPair( "Buff balancing: ", mpBalanceSelect ) );
			add( Box.createVerticalStrut( 10 ) );

			add( constructLabelPair( "Restore your mana: ", mpAutoRecoverSelect ) );
			add( Box.createVerticalStrut( 5 ) );

			JComponentUtilities.setComponentSize( mpAutoRecoverTargetSelect, 240, 20 );
			add( mpAutoRecoverTargetSelect );
			add( Box.createVerticalStrut( 10 ) );
			add( constructLabelPair( "Use these restores: ", constructScroller( mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) ) );

			mpBalanceSelect.setSelectedIndex( Math.max( (int)(StaticEntity.getFloatProperty( "mpThreshold" ) * 10) + 1, 0 ) );
			mpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "mpAutoRecovery" ) * 10) + 1 );
			mpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "mpAutoRecoveryTarget" ) * 10) + 1 );

			mpBalanceSelect.addActionListener( this );
			mpAutoRecoverSelect.addActionListener( this );
			mpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
				mpRestoreCheckbox[i].addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	saveRestoreSettings();
		}
	}

	private class AddTriggerPanel extends KoLPanel implements ListSelectionListener
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

		public boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}

		public void valueChanged( ListSelectionEvent e )
		{
			Object selected = moodList.getSelectedValue();
			if ( selected == null )
				return;

			MoodTrigger node = (MoodTrigger) selected;
			String type = node.getType();

			// Update the selected type

			if ( type.equals( "lose_effect" ) )
				typeSelect.setSelectedIndex(0);
			else if ( type.equals( "gain_effect" ) )
				typeSelect.setSelectedIndex(1);
			else if ( type.equals( "unconditional" ) )
				typeSelect.setSelectedIndex(2);

			// Update the selected effect

			valueSelect.setSelectedItem( node.getName() );
			commandField.setText( node.getAction() );
		}

		public void actionConfirmed()
		{
			MoodSettings.addTrigger( (String) typeSelect.getSelectedType(), (String) valueSelect.getSelectedItem(), commandField.getText() );
			MoodSettings.saveSettings();
		}

		public void actionCancelled()
		{
			String [] autoFillTypes = new String [] { "maximal set (all castable buffs)", "minimal set (current active buffs)" };

			String desiredType = (String) JOptionPane.showInputDialog(
				null, "Which kind of buff set would you like to use?", "Decide!",
					JOptionPane.INFORMATION_MESSAGE, null, autoFillTypes, activeEffects.isEmpty() ? autoFillTypes[0] : autoFillTypes[1] );

			if ( desiredType == autoFillTypes[0] )
				MoodSettings.maximalSet();
			else
				MoodSettings.minimalSet();

			MoodSettings.saveSettings();
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		private class ValueComboBox extends MutableComboBox
		{
			public ValueComboBox()
			{	super( EFFECT_MODEL, false );
			}

			public void setSelectedItem( Object anObject )
			{
				commandField.setText( MoodSettings.getDefaultAction( typeSelect.getSelectedType(), (String) anObject ) );
				super.setSelectedItem( anObject );
			}
		}

		private class TypeComboBox extends JComboBox
		{
			public TypeComboBox()
			{
				addItem( "When an effect is lost" );
				addItem( "When an effect is gained" );
				addItem( "Unconditional trigger" );

				addActionListener( new TypeComboBoxListener() );
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

			private class TypeComboBoxListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	valueSelect.setModel( getSelectedIndex() == 2 ? EMPTY_MODEL : EFFECT_MODEL );
				}
			}
		}
	}

	private class MoodTriggerListPanel extends LabeledScrollPanel
	{
		private JComboBox moodSelect;

		public MoodTriggerListPanel()
		{
			super( "", "edit casts", "remove", new JList( MoodSettings.getTriggers() ) );

			moodSelect = new MoodComboBox();

			centerPanel.add( moodSelect, BorderLayout.NORTH );
			moodList = (JList) scrollComponent;

			JPanel extraButtons = new JPanel( new GridLayout( 3, 1, 5, 5 ) );

			extraButtons.add( new NewMoodButton() );
			extraButtons.add( new DeleteMoodButton() );
			extraButtons.add( new CopyMoodButton() );

			buttonPanel.add( extraButtons, BorderLayout.SOUTH );
		}

		public void actionConfirmed()
		{
			String desiredLevel = JOptionPane.showInputDialog( null, "TURN CHANGE!", "15" );
			if ( desiredLevel == null )
				return;

			MoodSettings.addTriggers( moodList.getSelectedValues(), StaticEntity.parseInt( desiredLevel ) );
			MoodSettings.saveSettings();
		}

		public void actionCancelled()
		{
			MoodSettings.removeTriggers( moodList.getSelectedValues() );
			MoodSettings.saveSettings();
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		private class MoodComboBox extends JComboBox
		{
			public MoodComboBox()
			{
				super( MoodSettings.getAvailableMoods() );
				setSelectedItem( StaticEntity.getProperty( "currentMood" ) );
				addActionListener( new MoodComboBoxListener() );
			}

			private class MoodComboBoxListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	MoodSettings.setMood( (String) getSelectedItem() );
				}
			}
		}

		private class NewMoodButton extends ThreadedButton
		{
			public NewMoodButton()
			{	super( "new list" );
			}

			public void run()
			{
				String name = JOptionPane.showInputDialog( "Give your list a name!" );
				if ( name == null )
					return;

				MoodSettings.setMood( name );
				MoodSettings.saveSettings();
			}
		}

		private class DeleteMoodButton extends ThreadedButton
		{
			public DeleteMoodButton()
			{	super( "delete list" );
			}

			public void run()
			{
				MoodSettings.deleteCurrentMood();
				MoodSettings.saveSettings();
			}
		}

		private class CopyMoodButton extends ThreadedButton
		{
			public CopyMoodButton()
			{	super( "copy list" );
			}

			public void run()
			{
				String moodName = JOptionPane.showInputDialog( "Make a copy of current mood list called:" );
				if ( moodName == null )
					return;

				if ( moodName.equals( "default" ) )
					return;

				MoodSettings.copyTriggers( moodName );
				MoodSettings.setMood( moodName );
				MoodSettings.saveSettings();
			}
		}
	}
}
