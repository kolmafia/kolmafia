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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import tab.CloseTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import javax.swing.border.Border;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class BuffBotFrame extends KoLFrame
{
	private JList buffListDisplay;

	/**
	 * Constructs a new <code>BuffBotFrame</code> and inserts all
	 * of the necessary panels into a tabular layout for accessibility.
	 */

	public BuffBotFrame()
	{
		super( "BuffBot Manager" );

		// Initialize the display log buffer and the file log

		tabs.addTab( "Run Buffbot", new MainBuffPanel() );

		JPanel optionsContainer = new JPanel( new BorderLayout( 10, 10 ) );
		optionsContainer.add( new BuffOptionsPanel(), BorderLayout.NORTH );
		optionsContainer.add( new BuffListPanel(), BorderLayout.CENTER );

		tabs.addTab( "Edit Bufflist", optionsContainer );
		addTab( "Main Settings", new MainSettingsPanel() );
		addTab( "Other Settings", new OtherSettingsPanel() );

		framePanel.add( tabs, BorderLayout.CENTER );
	}

	public boolean useSidePane()
	{	return true;
	}

	/**
	 * Internal class used to handle everything related to
	 * operating the buffbot.
	 */

	private class MainBuffPanel extends LabeledScrollPanel
	{
		public MainBuffPanel()
		{
			super( "BuffBot Activities", "start", "stop", new JList( BuffBotHome.getMessages() ) );

			BuffBotHome.setFrame( BuffBotFrame.this );
			((JList)scrollComponent).setCellRenderer( BuffBotHome.getMessageRenderer() );
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( confirmedButton == null )
				return;

			confirmedButton.setEnabled( isEnabled );
		}

		public void actionConfirmed()
		{
			if ( BuffBotHome.isBuffBotActive() )
				return;

			// Need to make sure everything is up to date.
			// This includes character status, inventory
			// data and current settings.

			BuffBotHome.setBuffBotActive( true );
			RequestThread.postRequest( CharpaneRequest.getInstance() );
			BuffBotManager.runBuffBot( Integer.MAX_VALUE );
		}

		public void actionCancelled()
		{	BuffBotHome.setBuffBotActive( false );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * BuffBot options management
	 */

	private class BuffOptionsPanel extends KoLPanel
	{
		private JCheckBox restrictBox;
		private JCheckBox singletonBox;
		private JComboBox skillSelect;
		private JTextField priceField, countField;

		public BuffOptionsPanel()
		{
			super( "add", "remove", new Dimension( 120, 20 ),  new Dimension( 300, 20 ));
			UseSkillRequest skill;

			LockableListModel skillSet = usableSkills;
			LockableListModel buffSet = new LockableListModel();
			for (int i = 0; (skill = (UseSkillRequest) skillSet.get(i)) != null; ++i )
				if (ClassSkillsDatabase.isBuff( ClassSkillsDatabase.getSkillId( skill.getSkillName() ) ))
					buffSet.add( skill );

			skillSelect = new JComboBox( buffSet );

			priceField = new JTextField();
			countField = new JTextField();
			restrictBox = new JCheckBox();
			singletonBox = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Buff to cast: ", skillSelect );
			elements[1] = new VerifiableElement( "Price (in meat): ", priceField );
			elements[2] = new VerifiableElement( "# of casts: ", countField );
			elements[3] = new VerifiableElement( "White listed?", restrictBox );
			elements[4] = new VerifiableElement( "Once per day?", singletonBox );
			setContent( elements );
		}

		public void actionConfirmed()
		{
			BuffBotManager.addBuff( ((UseSkillRequest) skillSelect.getSelectedItem()).getSkillName(),
				StaticEntity.parseInt( priceField.getText() ), StaticEntity.parseInt( countField.getText() ),
					restrictBox.isSelected(), singletonBox.isSelected() );
		}

		public void actionCancelled()
		{	BuffBotManager.removeBuffs( buffListDisplay.getSelectedValues() );
		}
	}

	private class BuffListPanel extends JPanel
	{
		public BuffListPanel()
		{
			setLayout( new BorderLayout() );
			setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
			add( JComponentUtilities.createLabel( "Active Buffing List", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );

			buffListDisplay = new JList( BuffBotManager.getBuffCostTable() );
			buffListDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			buffListDisplay.setVisibleRowCount( 5 );

			add( new SimpleScrollPane( buffListDisplay ), BorderLayout.CENTER );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * BuffBot White List management
	 */

	private class MainSettingsPanel extends KoLPanel
	{
		private JComboBox messageDisposalSelect;
		private JCheckBox [] mpRestoreCheckbox;

		public MainSettingsPanel()
		{
			super( new Dimension( 120, 20 ),  new Dimension( 300, 20 ) );

			LockableListModel messageDisposalChoices = new LockableListModel();
			messageDisposalChoices.add( "Auto-save non-requests" );
			messageDisposalChoices.add( "Auto-delete non-requests" );
			messageDisposalChoices.add( "Do nothing to non-requests" );
			messageDisposalSelect = new JComboBox( messageDisposalChoices );

			VerifiableElement [] elements = new VerifiableElement[2];
			elements[0] = new VerifiableElement( "Message disposal: ", messageDisposalSelect );
			elements[1] = new VerifiableElement( "Mana restores: ", constructScroller( mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) );

			setContent( elements );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "buffBotMessageDisposal", String.valueOf( messageDisposalSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "mpAutoRecoveryItems", getSettingString( mpRestoreCheckbox ) );
		}

		public void actionCancelled()
		{	messageDisposalSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "buffBotMessageDisposal" ) );
		}
	}

	private class OtherSettingsPanel extends KoLPanel
	{
		private JTextArea whiteListEntry, invalidPriceMessage, thanksMessage;

		public OtherSettingsPanel()
		{
			super( "save", "reset", new Dimension( 120, 20 ),  new Dimension( 300, 20 ) );
			setContent( new VerifiableElement[0] );

			whiteListEntry = new JTextArea();
			invalidPriceMessage = new JTextArea();
			thanksMessage = new JTextArea();

			whiteListEntry.setFont( DEFAULT_FONT );
			whiteListEntry.setLineWrap( true );
			whiteListEntry.setWrapStyleWord( true );

			invalidPriceMessage.setFont( DEFAULT_FONT );
			invalidPriceMessage.setLineWrap( true );
			invalidPriceMessage.setWrapStyleWord( true );

			thanksMessage.setFont( DEFAULT_FONT );
			thanksMessage.setLineWrap( true );
			thanksMessage.setWrapStyleWord( true );

			JPanel settingsTopPanel = new JPanel( new BorderLayout() );
			settingsTopPanel.add( JComponentUtilities.createLabel( "White List (separate names with commas):", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			settingsTopPanel.add( new SimpleScrollPane( whiteListEntry ), BorderLayout.CENTER );

			JPanel settingsMiddlePanel = new JPanel( new BorderLayout() );
			settingsMiddlePanel.add( JComponentUtilities.createLabel( "Invalid Buff Price Message", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			settingsMiddlePanel.add( new SimpleScrollPane( invalidPriceMessage ), BorderLayout.CENTER );

			JPanel settingsBottomPanel = new JPanel( new BorderLayout() );
			settingsBottomPanel.add( JComponentUtilities.createLabel( "Donation Thanks Message", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			settingsBottomPanel.add( new SimpleScrollPane( thanksMessage ), BorderLayout.CENTER );

			JPanel settingsPanel = new JPanel( new GridLayout( 3, 1, 10, 10 ) );

			JComponentUtilities.setComponentSize( settingsTopPanel, 300, 120 );
			JComponentUtilities.setComponentSize( settingsMiddlePanel, 300, 120 );
			JComponentUtilities.setComponentSize( settingsBottomPanel, 300, 120 );

			settingsPanel.add( settingsTopPanel );
			settingsPanel.add( settingsMiddlePanel );
			settingsPanel.add( settingsBottomPanel );

			container.add( settingsPanel, BorderLayout.CENTER );
			actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "whiteList", whiteListEntry.getText() );
			StaticEntity.setProperty( "invalidBuffMessage", invalidPriceMessage.getText() );
			StaticEntity.setProperty( "thanksMessage", thanksMessage.getText() );
		}

		public void actionCancelled()
		{
			whiteListEntry.setText( StaticEntity.getProperty( "whiteList" ) );
			invalidPriceMessage.setText( StaticEntity.getProperty( "invalidBuffMessage" ) );
			thanksMessage.setText( StaticEntity.getProperty( "thanksMessage" ) );
		}
	}
}
