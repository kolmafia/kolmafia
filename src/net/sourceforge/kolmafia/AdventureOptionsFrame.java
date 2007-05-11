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
import java.awt.Dimension;
import java.awt.GridLayout;

import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Arrays;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JTree;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import javax.swing.tree.DefaultTreeModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.filechooser.FileFilter;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.MoodSettings.MoodTrigger;


public abstract class AdventureOptionsFrame extends KoLFrame
{
	private ExecuteButton begin;
	private boolean isHandlingConditions = false;

	private JComboBox actionSelect;
	private JComboBox activeMood;

	private TreeMap zoneMap;
	private JSpinner countField;

	protected JTree combatTree;
	protected JTextArea combatEditor;
	protected DefaultTreeModel combatModel;
	protected CardLayout combatCards;
	protected JPanel combatPanel;

	protected JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect, hpHaltCombatSelect;
	protected JCheckBox [] hpRestoreCheckbox;
	protected JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect, mpBalanceSelect;
	protected JCheckBox [] mpRestoreCheckbox;

	protected JList moodList;
	protected JList locationSelect;
	protected JComboBox zoneSelect;

	protected KoLAdventure lastAdventure = null;
	protected boolean updateConditions = true;
	protected JCheckBox autoSetCheckBox = new AutoSetCheckBox();
	protected JTextField conditionField = new JTextField();

	public AdventureOptionsFrame( String title )
	{	super( title );
	}

	public JPanel constructLabelPair( String label, JComponent element1 )
	{	return constructLabelPair( label, element1, null );
	}

	public JPanel constructLabelPair( String label, JComponent element1, JComponent element2 )
	{
		JPanel container = new JPanel();
		container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );

		if ( element1 != null && element1 instanceof JComboBox )
			JComponentUtilities.setComponentSize( element1, 240, 20 );

		if ( element2 != null && element2 instanceof JComboBox )
			JComponentUtilities.setComponentSize( element2, 240, 20 );

		JPanel labelPanel = new JPanel( new GridLayout( 1, 1 ) );
		labelPanel.add( new JLabel( "<html><b>" + label + "</b></html>", JLabel.LEFT ) );

		container.add( labelPanel );

		if ( element1 != null )
		{
			container.add( Box.createVerticalStrut( 5 ) );
			container.add( element1 );
		}

		if ( element2 != null )
		{
			container.add( Box.createVerticalStrut( 5 ) );
			container.add( element2 );
		}

		return container;
	}

	public JTabbedPane getSouthernTabs()
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

		SimpleScrollPane restoreScroller = new SimpleScrollPane( restorePanel );
		JComponentUtilities.setComponentSize( restoreScroller, 560, 300 );

		tabs.addTab( "Auto-Recovery", restoreScroller );

		JPanel moodPanel = new JPanel( new BorderLayout() );
		moodPanel.add( new MoodTriggerListPanel(), BorderLayout.CENTER );

		AddTriggerPanel triggers = new AddTriggerPanel();
		moodList.addListSelectionListener( triggers );
		moodPanel.add( triggers, BorderLayout.NORTH );

		tabs.addTab( "Mood Handling", moodPanel );
		tabs.addTab( "Custom Combat", combatPanel );

		return tabs;
	}

	public class CustomCombatPanel extends LabeledScrollPanel
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
			File location = new File( SETTINGS_LOCATION, CombatSettings.settingsFileName() );
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

	public class CustomCombatTreePanel extends LabeledScrollPanel
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
			JFileChooser chooser = new JFileChooser( SETTINGS_LOCATION.getAbsolutePath() );
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

	public static final FileFilter CCS_FILTER = new FileFilter()
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

	public void refreshCombatSettings()
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

	public void refreshCombatTree()
	{
		CombatSettings.restoreDefaults();
		combatModel.setRoot( CombatSettings.getRoot() );
		combatTree.setRootVisible( false );

		for ( int i = 0; i < combatTree.getRowCount(); ++i )
			combatTree.expandRow( i );
	}

	public void saveRestoreSettings()
	{
		StaticEntity.setProperty( "autoAbortThreshold", getPercentage( hpHaltCombatSelect ) );
		StaticEntity.setProperty( "hpAutoRecovery", getPercentage( hpAutoRecoverSelect ) );
		StaticEntity.setProperty( "hpAutoRecoveryTarget", getPercentage( hpAutoRecoverTargetSelect ) );
		StaticEntity.setProperty( "hpAutoRecoveryItems", getSettingString( hpRestoreCheckbox ) );

		StaticEntity.setProperty( "manaBurningThreshold", getPercentage( mpBalanceSelect ) );
		StaticEntity.setProperty( "mpAutoRecovery", getPercentage( mpAutoRecoverSelect ) );
		StaticEntity.setProperty( "mpAutoRecoveryTarget", getPercentage( mpAutoRecoverTargetSelect ) );
		StaticEntity.setProperty( "mpAutoRecoveryItems", getSettingString( mpRestoreCheckbox ) );
	}

	private static String getPercentage( JComboBox option )
	{	return String.valueOf( (option.getSelectedIndex() - 1) / 20.0f );
	}

	private static void setSelectedIndex( JComboBox option, String property )
	{
		int desiredIndex = (int) ((StaticEntity.getFloatProperty( property ) * 20.0f) + 1);
		option.setSelectedIndex( Math.min( Math.max( desiredIndex, 0 ), option.getItemCount() ) );
	}

	public class CheckboxListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	saveRestoreSettings();
		}
	}

	public class HealthOptionsPanel extends JPanel implements ActionListener
	{
		public boolean refreshSoon = false;

		public HealthOptionsPanel()
		{
			hpHaltCombatSelect = new JComboBox();
			hpHaltCombatSelect.addItem( "Stop automation if auto-recovery fails" );
			for ( int i = 0; i <= 19; ++i )
				hpHaltCombatSelect.addItem( "Stop automation if health at " + (i*5) + "%" );

			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover health" );
			for ( int i = 0; i <= 19; ++i )
				hpAutoRecoverSelect.addItem( "Auto-recover health at " + (i*5) + "%" );

			hpAutoRecoverTargetSelect = new JComboBox();
			hpAutoRecoverTargetSelect.addItem( "Do not automatically recover health" );
			for ( int i = 0; i <= 20; ++i )
				hpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*5) + "% health" );

			// Add the elements to the panel

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( constructLabelPair( "Stop automation: ", hpHaltCombatSelect ) );
			add( Box.createVerticalStrut( 15 ) );

			add( constructLabelPair( "Restore your health: ", hpAutoRecoverSelect, hpAutoRecoverTargetSelect ) );
			add( Box.createVerticalStrut( 15 ) );

			add( constructLabelPair( "Use these restores: ", constructScroller( hpRestoreCheckbox = HPRestoreItemList.getCheckboxes() ) ) );

			setSelectedIndex( hpHaltCombatSelect, "hpThreshold" );
			setSelectedIndex( hpAutoRecoverSelect, "hpAutoRecovery" );
			setSelectedIndex( hpAutoRecoverTargetSelect, "hpAutoRecoveryTarget" );

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

	public class ManaOptionsPanel extends JPanel implements ActionListener
	{
		public ManaOptionsPanel()
		{
			mpBalanceSelect = new JComboBox();
			mpBalanceSelect.addItem( "Do not automatically rebalance buffs" );
			for ( int i = 0; i <= 19; ++i )
				mpBalanceSelect.addItem( "Consider buff rebalancing at " + (i*5) + "%" );

			mpAutoRecoverSelect = new JComboBox();
			mpAutoRecoverSelect.addItem( "Do not automatically recover mana" );
			for ( int i = 0; i <= 19; ++i )
				mpAutoRecoverSelect.addItem( "Auto-recover mana at " + (i*5) + "%" );

			mpAutoRecoverTargetSelect = new JComboBox();
			mpAutoRecoverTargetSelect.addItem( "Do not automatically recover mana" );
			for ( int i = 0; i <= 20; ++i )
				mpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*5) + "% mana" );

			// Add the elements to the panel

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

			add( constructLabelPair( "Mana burning: ", mpBalanceSelect ) );
			add( Box.createVerticalStrut( 15 ) );

			add( constructLabelPair( "Restore your mana: ", mpAutoRecoverSelect, mpAutoRecoverTargetSelect ) );
			add( Box.createVerticalStrut( 15 ) );

			add( constructLabelPair( "Use these restores: ", constructScroller( mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) ) );

			setSelectedIndex( mpBalanceSelect, "manaBurningThreshold" );
			setSelectedIndex( mpAutoRecoverSelect, "mpAutoRecovery" );
			setSelectedIndex( mpAutoRecoverTargetSelect, "mpAutoRecoveryTarget" );

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

	public class AddTriggerPanel extends KoLPanel implements ListSelectionListener
	{
		public LockableListModel EMPTY_MODEL = new LockableListModel();
		public LockableListModel EFFECT_MODEL = new LockableListModel();

		public TypeComboBox typeSelect;
		public ValueComboBox valueSelect;
		public JTextField commandField;

		public AddTriggerPanel()
		{
			super( "add entry", "auto-fill" );

			typeSelect = new TypeComboBox();

			Object [] names = StatusEffectDatabase.values().toArray();

			for ( int i = 0; i < names.length; ++i )
				EFFECT_MODEL.add( names[i].toString() );

			EFFECT_MODEL.sort();

			valueSelect = new ValueComboBox();
			commandField = new JTextField();

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Trigger On: ", typeSelect );
			elements[1] = new VerifiableElement( "Check For: ", valueSelect );
			elements[2] = new VerifiableElement( "Command: ", commandField );

			setContent( elements );
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
			String currentMood = StaticEntity.getProperty( "currentMood" );
			if ( currentMood.equals( "apathetic" ) )
			{
				JOptionPane.showMessageDialog( null, "You cannot add triggers to an apathetic mood." );
				return;
			}

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

		public void addStatusLabel()
		{
		}

		public class ValueComboBox extends MutableComboBox
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

		public class TypeComboBox extends JComboBox
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

			public class TypeComboBoxListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	valueSelect.setModel( getSelectedIndex() == 2 ? EMPTY_MODEL : EFFECT_MODEL );
				}
			}
		}
	}

	public class MoodTriggerListPanel extends LabeledScrollPanel
	{
		public JComboBox availableMoods;

		public MoodTriggerListPanel()
		{
			super( "", "edit casts", "remove", new JList( MoodSettings.getTriggers() ) );

			availableMoods = new MoodComboBox();

			centerPanel.add( availableMoods, BorderLayout.NORTH );
			moodList = (JList) scrollComponent;

			JPanel extraButtons = new JPanel( new GridLayout( 3, 1, 5, 5 ) );

			extraButtons.add( new NewMoodButton() );
			extraButtons.add( new DeleteMoodButton() );
			extraButtons.add( new CopyMoodButton() );

			eastPanel.add( extraButtons, BorderLayout.SOUTH );
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

		public class MoodComboBox extends JComboBox
		{
			public MoodComboBox()
			{
				super( MoodSettings.getAvailableMoods() );
				setSelectedItem( StaticEntity.getProperty( "currentMood" ) );
				addActionListener( new MoodComboBoxListener() );
			}

			public class MoodComboBoxListener implements ActionListener
			{
				public void actionPerformed( ActionEvent e )
				{	MoodSettings.setMood( (String) getSelectedItem() );
				}
			}
		}

		public class NewMoodButton extends ThreadedButton
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

		public class DeleteMoodButton extends ThreadedButton
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

		public class CopyMoodButton extends ThreadedButton
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


	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	public class AdventureSelectPanel extends JPanel implements ChangeListener
	{
		public AdventureSelectPanel( boolean enableAdventures )
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

			if ( enableAdventures )
			{
				countField = new JSpinner();
				countField.addChangeListener( this );
				JComponentUtilities.setComponentSize( countField, 50, 24 );
			}

			JComponentUtilities.setComponentSize( zoneSelect, 200, 24 );
			JPanel zonePanel = new JPanel( new BorderLayout( 5, 5 ) );

			if ( enableAdventures )
				zonePanel.add( countField, BorderLayout.EAST );

			zonePanel.add( zoneSelect, BorderLayout.CENTER );

			zoneSelect.addActionListener( new ZoneChangeListener() );
			locationSelect = new JList( adventureList );
			locationSelect.setVisibleRowCount( 4 );

			JPanel locationPanel = new JPanel( new BorderLayout( 5, 5 ) );
			locationPanel.add( zonePanel, BorderLayout.NORTH );
			locationPanel.add( new SimpleScrollPane( locationSelect ), BorderLayout.CENTER );

			if ( enableAdventures )
			{
				JPanel locationHolder = new JPanel( new CardLayout( 10, 10 ) );
				locationHolder.add( locationPanel, "" );
				add( locationHolder, BorderLayout.WEST );

				add( new ObjectivesPanel(), BorderLayout.CENTER );
				((JSpinner.DefaultEditor)countField.getEditor()).getTextField().addKeyListener( begin );
			}
			else
			{
				add( locationPanel, BorderLayout.WEST );
			}
		}

		public void stateChanged( ChangeEvent e )
		{
			int desired = getValue( countField, KoLCharacter.getAdventuresLeft() );
			if ( desired == KoLCharacter.getAdventuresLeft() + 1 )
				countField.setValue( new Integer( 1 ) );
			else if ( desired <= 0 || desired > KoLCharacter.getAdventuresLeft() )
				countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );

		}

		public void requestFocus()
		{	locationSelect.requestFocus();
		}
	}

	private class ObjectivesPanel extends KoLPanel
	{
		public ObjectivesPanel()
		{
			super( new Dimension( 50, 20 ), new Dimension( 200, 20 ) );

			actionSelect = new MutableComboBox( KoLCharacter.getBattleSkillNames(), false );
			activeMood = new JComboBox( MoodSettings.getAvailableMoods() );

			locationSelect.addListSelectionListener( new ConditionChangeListener() );

			JPanel conditionPanel = new JPanel( new BorderLayout( 5, 5 ) );
			conditionPanel.add( conditionField, BorderLayout.CENTER );
			conditionPanel.add( autoSetCheckBox, BorderLayout.EAST );

			autoSetCheckBox.setSelected( StaticEntity.getBooleanProperty( "autoSetConditions" ) );

			JPanel beginWrapper = new JPanel();
			beginWrapper.add( begin = new ExecuteButton() );

			JPanel stopWrapper = new JPanel();
			stopWrapper.add( new InvocationButton( "Declare World Peace", "stop.gif", RequestThread.class, "declareWorldPeace" ) );

			JPanel buttonWrapper = new JPanel( new BorderLayout() );
			buttonWrapper.add( beginWrapper, BorderLayout.NORTH );
			buttonWrapper.add( stopWrapper, BorderLayout.SOUTH );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Combat:  ", actionSelect );
			elements[1] = new VerifiableElement( "Moods:  ", activeMood );
			elements[2] = new VerifiableElement( "Goals:  ", conditionPanel );

			setContent( elements );
			container.add( buttonWrapper, BorderLayout.EAST );

			JComponentUtilities.addHotKey( this, KeyEvent.VK_ENTER, begin );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "battleAction", (String) actionSelect.getSelectedItem() );
			MoodSettings.setMood( (String) activeMood.getSelectedItem() );
		}

		public void actionCancelled()
		{
		}

		public void addStatusLabel()
		{
		}

		public void setEnabled( boolean isEnabled )
		{	begin.setEnabled( isEnabled );
		}
	}

	private class WorthlessItemRequest implements Runnable
	{
		private int itemCount;

		public WorthlessItemRequest( int itemCount )
		{	this.itemCount = itemCount;
		}

		public void run()
		{	DEFAULT_SHELL.executeLine( "acquire " + itemCount + " worthless item in " + ((Integer)countField.getValue()).intValue() );
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

	private class ConditionChangeListener implements ListSelectionListener, ListDataListener
	{
		public ConditionChangeListener()
		{	conditions.addListDataListener( this );
		}

		public void valueChanged( ListSelectionEvent e )
		{
			if ( updateConditions )
			{
				conditionField.setText( "" );

				if ( !conditions.isEmpty() )
					conditions.clear();

				fillCurrentConditions();
			}
		}

		public void intervalAdded( ListDataEvent e )
		{
			if ( isHandlingConditions )
				return;

			fillCurrentConditions();
		}

		public void intervalRemoved( ListDataEvent e )
		{
			if ( isHandlingConditions )
				return;

			fillCurrentConditions();
		}

		public void contentsChanged( ListDataEvent e )
		{
			if ( isHandlingConditions )
				return;

			fillCurrentConditions();
		}
	}

	private class ExecuteButton extends ThreadedButton
	{
		private boolean isProcessing = false;

		public ExecuteButton()
		{
			super( JComponentUtilities.getImage( "hourglass.gif" ) );
			setToolTipText( "Start Adventuring" );
			JComponentUtilities.setComponentSize( this, 32, 32 );
		}

		public void run()
		{
			if ( isProcessing )
				return;

			KoLmafia.updateDisplay( "Validating adventure sequence..." );

			KoLAdventure request = (KoLAdventure) locationSelect.getSelectedValue();
			if ( request == null )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "No location selected." );
				return;
			}

			isProcessing = true;

			// If there are conditions in the condition field, be
			// sure to process them.

			if ( conditions.isEmpty() || (lastAdventure != null && lastAdventure != request) )
			{
				Object stats = null;
				int substatIndex = conditions.indexOf( tally.get(2) );

				if ( substatIndex != 0 )
					stats = conditions.get( substatIndex );

				conditions.clear();

				if ( stats != null )
					conditions.add( stats );

				lastAdventure = request;

				updateConditions = false;
				isHandlingConditions = true;

				RequestThread.openRequestSequence();
				boolean shouldAdventure = handleConditions( request );
				RequestThread.closeRequestSequence();

				isHandlingConditions = false;
				updateConditions = true;

				if ( !shouldAdventure )
				{
					isProcessing = false;
					return;
				}
			}

			int requestCount = Math.min( getValue( countField, 1 ), KoLCharacter.getAdventuresLeft() );
			countField.setValue( new Integer( requestCount ) );

			boolean resetCount = requestCount == KoLCharacter.getAdventuresLeft();

			StaticEntity.getClient().makeRequest( request, requestCount );

			if ( resetCount )
				countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );

			isProcessing = false;
		}

		private boolean handleConditions( KoLAdventure request )
		{
			if ( KoLmafia.isAdventuring() )
				return false;

			if ( !autoSetCheckBox.isSelected() )
				return true;

			String conditionList = conditionField.getText().trim().toLowerCase();

			if ( conditionList.equalsIgnoreCase( "none" ) )
			{
				conditions.clear();
				return true;
			}

			if ( conditionList.length() == 0 )
				return true;

			conditions.clear();

			int worthlessItemCount = 0;
			String [] splitConditions = conditionList.split( "\\s*,\\s*" );

			// First, figure out whether or not you need to do a disjunction
			// on the conditions, which changes how KoLmafia handles them.

			for ( int i = 0; i < splitConditions.length; ++i )
			{
				if ( splitConditions[i] == null )
					continue;

				if ( splitConditions[i].indexOf( "worthless" ) != -1 )
				{
					// You're looking for some number of
					// worthless items

					worthlessItemCount += Character.isDigit( splitConditions[i].charAt(0) ) ?
						StaticEntity.parseInt( splitConditions[i].split( " " )[0] ) : 1;
				}
				else if ( splitConditions[i].equals( "check" ) )
				{
					// Postpone verification of conditions
					// until all other conditions added.
				}
				else if ( splitConditions[i].equals( "outfit" ) )
				{
					// Determine where you're adventuring and use
					// that to determine which components make up
					// the outfit pulled from that area.

					if ( !(request instanceof KoLAdventure) || !EquipmentDatabase.addOutfitConditions( (KoLAdventure) request ) )
						return true;
				}
				else
				{
					if ( splitConditions[i].startsWith( "+" ) )
					{
						if ( !DEFAULT_SHELL.executeConditionsCommand( "add " + splitConditions[i].substring(1) ) )
							return false;
					}
					else if ( !DEFAULT_SHELL.executeConditionsCommand( "set " + splitConditions[i] ) )
					{
						return false;
					}
				}
			}

			if ( worthlessItemCount > 0 )
			{
				StaticEntity.getClient().makeRequest( new WorthlessItemRequest( worthlessItemCount ) );
				return false;
			}

			if ( conditions.isEmpty() )
			{
				KoLmafia.updateDisplay( "All conditions already satisfied." );
				return false;
			}

			if ( ((Integer)countField.getValue()).intValue() == 0 )
				countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );

			return true;
		}
	}

	private class AutoSetCheckBox extends JCheckBox implements ActionListener
	{
		public AutoSetCheckBox()
		{	addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			StaticEntity.setProperty( "autoSetConditions", String.valueOf( isSelected() ) );

			if ( isSelected() )
			{
				if ( conditionField.getText().equals( "none" ) )
					fillDefaultConditions();
			}
			else
				conditionField.setText( "none" );
		}
	}

	public void fillDefaultConditions()
	{
		if ( !autoSetCheckBox.isSelected() )
		{
			conditionField.setText( "none" );
			return;
		}

		KoLAdventure location = (KoLAdventure) locationSelect.getSelectedValue();
		if ( location == null )
			return;

		conditionField.setText( AdventureDatabase.getCondition( location ) );
	}

	public void fillCurrentConditions()
	{
		StringBuffer conditionString = new StringBuffer();

		for ( int i = 0; i < conditions.size(); ++i )
		{
			if ( i > 0 )
				conditionString.append( ", " );

			conditionString.append( ((AdventureResult)conditions.get(i)).toConditionString() );
		}

		if ( conditionString.length() == 0 )
			fillDefaultConditions();
		else
			conditionField.setText( conditionString.toString() );
	}

	public void requestFocus()
	{
		super.requestFocus();

		if ( locationSelect != null )
			locationSelect.requestFocus();
	}

	protected JPanel getAdventureSummary( String property, JList locationSelect )
	{
		int selectedIndex = StaticEntity.getIntegerProperty( property );

		CardLayout resultCards = new CardLayout();
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		int cardCount = 0;

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new SimpleScrollPane( tally, 4 ), String.valueOf( cardCount++ ) );

		if ( property.startsWith( "default" ) )
		{
			resultSelect.addItem( "Location Details" );
			resultPanel.add( new SafetyField( locationSelect ), String.valueOf( cardCount++ ) );

			resultSelect.addItem( "Conditions Left" );
			resultPanel.add( new SimpleScrollPane( conditions, 4 ), String.valueOf( cardCount++ ) );
		}

		resultSelect.addItem( "Available Skills" );
		resultPanel.add( new SimpleScrollPane( availableSkills, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new SimpleScrollPane( activeEffects, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new SimpleScrollPane( encounterList, 4 ), String.valueOf( cardCount++ ) );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new SimpleScrollPane( adventureList, 4 ), String.valueOf( cardCount++ ) );


		resultSelect.addActionListener( new ResultSelectListener( resultCards, resultPanel, resultSelect, property ) );

		if ( selectedIndex >= cardCount )
			selectedIndex = cardCount - 1;

		resultSelect.setSelectedIndex( selectedIndex );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( resultSelect, BorderLayout.NORTH );
		containerPanel.add( resultPanel, BorderLayout.CENTER );

		return containerPanel;
	}

	private class ResultSelectListener implements ActionListener
	{
		private String property;
		private CardLayout resultCards;
		private JPanel resultPanel;
		private JComboBox resultSelect;

		public ResultSelectListener( CardLayout resultCards, JPanel resultPanel, JComboBox resultSelect, String property )
		{
			this.resultCards = resultCards;
			this.resultPanel = resultPanel;
			this.resultSelect = resultSelect;
			this.property = property;
		}

		public void actionPerformed( ActionEvent e )
		{
			String index = String.valueOf( resultSelect.getSelectedIndex() );
			resultCards.show( resultPanel, index );
			StaticEntity.setProperty( property, index );

		}
	}

	protected class SafetyField extends JPanel implements Runnable, ListSelectionListener
	{
		private JTextPane safetyText;
		private String savedText = " ";
		private JList locationSelect;
		private KoLAdventure lastLocation;

		public SafetyField( JList locationSelect )
		{
			super( new BorderLayout() );

			this.safetyText = new JTextPane();
			this.locationSelect = locationSelect;

			SimpleScrollPane textScroller = new SimpleScrollPane( safetyText );
			JComponentUtilities.setComponentSize( textScroller, 100, 100 );
			add( textScroller, BorderLayout.CENTER );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( this ) );
			locationSelect.addListSelectionListener( this );

			safetyText.setContentType( "text/html" );
			safetyText.setEditable( false );
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

			lastLocation = request;
			AreaCombatData combat = request.getAreaSummary();
			String text = ( combat == null ) ? " " : combat.toString();

			// Avoid rendering and screen flicker if no change.
			// Compare with our own copy of what we set, since
			// getText() returns a modified version.

			if ( !text.equals( savedText ) )
			{
				savedText = text;
				safetyText.setText( text );

				// Change the font for the JEditorPane to the
				// same ones used in a JLabel.

				MutableAttributeSet fonts = safetyText.getInputAttributes();

				StyleConstants.setFontSize( fonts, DEFAULT_FONT.getSize() );
				StyleConstants.setFontFamily( fonts, DEFAULT_FONT.getFamily() );

				StyledDocument html = safetyText.getStyledDocument();
				html.setCharacterAttributes( 0, html.getLength() + 1, fonts, false );

				safetyText.setCaretPosition( 0 );
			}
		}
	}
}
