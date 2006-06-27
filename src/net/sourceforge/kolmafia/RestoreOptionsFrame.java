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

import java.awt.Dimension;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.SpringLayout;
import javax.swing.filechooser.FileFilter;
import javax.swing.JTree;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultTreeModel;

import com.sun.java.forums.SpringUtilities;

import java.io.File;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.FileOutputStream;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RestoreOptionsFrame extends KoLFrame
{
	private JTree displayTree;
	private JTextArea displayEditor;
	private DefaultTreeModel displayModel;
	private CardLayout combatCards;
	private JPanel combatPanel;

	private JComboBox battleStopSelect;
	private JTextField betweenBattleScriptField;

	private JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect;
	private JCheckBox [] hpRestoreCheckbox;

	private JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect;
	private JCheckBox [] mpRestoreCheckbox;

	public RestoreOptionsFrame()
	{
		super( "Combat Configuration" );

		JPanel restorePanel = new JPanel();
		restorePanel.setLayout( new BoxLayout( restorePanel, BoxLayout.Y_AXIS ) );

		restorePanel.add( new HealthOptionsPanel() );
		restorePanel.add( new ManaOptionsPanel() );

		displayTree = new JTree();
		displayModel = (DefaultTreeModel) displayTree.getModel();

		CheckboxListener listener = new CheckboxListener();
		for ( int i = 0; i < hpRestoreCheckbox.length; ++i )
			hpRestoreCheckbox[i].addActionListener( listener );
		for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
			mpRestoreCheckbox[i].addActionListener( listener );

		JScrollPane restoreScroller = new JScrollPane( restorePanel,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( restoreScroller, 600, 300 );
		framePanel.setLayout( new CardLayout( 10, 10 ) );

		tabs = new JTabbedPane();
		tabs.add( "Between Battle", restoreScroller );

		combatCards = new CardLayout();
		combatPanel = new JPanel( combatCards );
		combatPanel.add( "tree", new CustomCombatTreePanel() );
		combatPanel.add( "editor", new CustomCombatPanel() );

		tabs.add( "Custom Combat", combatPanel );

		JScrollPane moodScroller = new JScrollPane( new MoodSwingEditorPanel(),
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( moodScroller, 600, 300 );		
		tabs.add( "Mood Swings", moodScroller );
		
		framePanel.add( tabs, "" );
	}

	private void saveRestoreSettings()
	{
		StaticEntity.setProperty( "betweenBattleScript", betweenBattleScriptField.getText() );
		StaticEntity.setProperty( "battleStop", String.valueOf( ((double)(battleStopSelect.getSelectedIndex() - 1) / 10.0) ) );

		StaticEntity.setProperty( "hpAutoRecovery", String.valueOf( ((double)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
		StaticEntity.setProperty( "hpAutoRecoveryTarget", String.valueOf( ((double)(hpAutoRecoverTargetSelect.getSelectedIndex() - 1) / 10.0) ) );
		StaticEntity.setProperty( "hpAutoRecoveryItems", getSettingString( hpRestoreCheckbox ) );

		StaticEntity.setProperty( "mpAutoRecovery", String.valueOf( ((double)(mpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
		StaticEntity.setProperty( "mpAutoRecoveryTarget", String.valueOf( ((double)(mpAutoRecoverTargetSelect.getSelectedIndex() - 1) / 10.0) ) );
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

		public void setEnabled( boolean isEnabled )
		{
			if ( !isEnabled )
				refreshSoon = true;

			if ( isEnabled && refreshSoon )
			{
				actionCancelled();
				refreshSoon = false;
			}
		}

		protected void actionConfirmed()
		{	saveRestoreSettings();
		}

		protected void actionCancelled()
		{
			betweenBattleScriptField.setText( StaticEntity.getProperty( "betweenBattleScript" ) );
			battleStopSelect.setSelectedIndex( (int)(StaticEntity.parseDouble( getProperty( "battleStop" ) ) * 10) + 1 );
			hpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.parseDouble( StaticEntity.getProperty( "hpAutoRecovery" ) ) * 10) + 1 );
			hpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.parseDouble( StaticEntity.getProperty( "hpAutoRecoveryTarget" ) ) * 10) + 1 );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
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

		public void setEnabled( boolean isEnabled )
		{
		}

		protected void actionConfirmed()
		{	saveRestoreSettings();
		}

		protected void actionCancelled()
		{
			mpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.parseDouble( StaticEntity.getProperty( "mpAutoRecovery" ) ) * 10) + 1 );
			mpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.parseDouble( StaticEntity.getProperty( "mpAutoRecoveryTarget" ) ) * 10) + 1 );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}
	}

	private class CustomCombatPanel extends LabeledScrollPanel
	{
		public CustomCombatPanel()
		{
			super( "Editor", "save", "help", new JTextArea( 12, 40 ) );
			displayEditor = (JTextArea) scrollComponent;
			refreshCombatSettings();
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
			combatCards.show( combatPanel, "tree" );
		}

		protected void actionCancelled()
		{	StaticEntity.openSystemBrowser( "http://kolmafia.sourceforge.net/combat.html" );
		}
	}

	private class CustomCombatTreePanel extends LabeledScrollPanel
	{
		public CustomCombatTreePanel()
		{	super( "Tree View", "edit", "load", displayTree );
		}

		public void actionConfirmed()
		{	combatCards.show( combatPanel, "editor" );
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
	}
	
	private class MoodSwingEditorPanel extends KoLPanel
	{
		private JRadioButton [] activeOptions;
		private JRadioButton [] ignoreOptions;
		private JRadioButton [] inactiveOptions;

		public MoodSwingEditorPanel()
		{
			super( new Dimension( 380, 20 ), new Dimension( 20, 20 ) );

			activeOptions = new JRadioButton[ MoodSettings.EFFECTS.length ];
			ignoreOptions = new JRadioButton[ MoodSettings.EFFECTS.length ];
			inactiveOptions = new JRadioButton[ MoodSettings.EFFECTS.length ];

			JPanel contentPanel = new JPanel( new SpringLayout() );

			for ( int i = 0; i < MoodSettings.EFFECTS.length; ++i )
			{
				activeOptions[i] = new JRadioButton( "active" );
				ignoreOptions[i] = new JRadioButton( "ignore" );
				inactiveOptions[i] = new JRadioButton( "inactive" );

				ButtonGroup holder = new ButtonGroup();
				holder.add( activeOptions[i] );
				holder.add( ignoreOptions[i] );
				holder.add( inactiveOptions[i] );

				contentPanel.add( new JLabel( MoodSettings.EFFECTS[i].getName() + ": ", JLabel.RIGHT ) );
				contentPanel.add( activeOptions[i] );
				contentPanel.add( ignoreOptions[i] );
				contentPanel.add( inactiveOptions[i] );
			}

			setContent( new VerifiableElement[0], false );

			SpringUtilities.makeCompactGrid( contentPanel, MoodSettings.EFFECTS.length, 4, 5, 5, 5, 5 );
			container.add( contentPanel, BorderLayout.CENTER );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			for ( int i = 0; i < MoodSettings.SKILL_NAMES.length; ++i )
			{
				if ( activeOptions[i].isSelected() )
					StaticEntity.setMoodProperty( MoodSettings.SKILL_NAMES[i], "active" );
				else if ( inactiveOptions[i].isSelected() )
					StaticEntity.setMoodProperty( MoodSettings.SKILL_NAMES[i], "inactive" );
				else
					StaticEntity.setMoodProperty( MoodSettings.SKILL_NAMES[i], "ignorea" );
			}
		}

		public void actionCancelled()
		{
			String setting;
			for ( int i = 0; i < MoodSettings.SKILL_NAMES.length; ++i )
			{
				setting = StaticEntity.getMoodProperty( MoodSettings.SKILL_NAMES[i] );

				if ( setting.equals( "active" ) )
					activeOptions[i].setSelected( true );
				else if ( setting.equals( "inactive" ) )
					inactiveOptions[i].setSelected( true );
				else
					ignoreOptions[i].setSelected( true );
			}
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
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
			displayEditor.setText( buffer.toString() );
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
		displayModel.setRoot( CombatSettings.getRoot() );
		displayTree.setRootVisible( false );
	}
}
