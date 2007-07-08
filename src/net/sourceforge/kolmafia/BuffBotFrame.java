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
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

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

		this.tabs.addTab( "Run Buffbot", new MainBuffPanel() );

		JPanel optionsContainer = new JPanel( new BorderLayout( 10, 10 ) );
		optionsContainer.add( new BuffOptionsPanel(), BorderLayout.NORTH );
		optionsContainer.add( new BuffListPanel(), BorderLayout.CENTER );

		this.tabs.addTab( "Edit Bufflist", optionsContainer );
		this.addTab( "Main Settings", new MainSettingsPanel() );
		this.addTab( "Other Settings", new OtherSettingsPanel() );

		this.framePanel.add( this.tabs, BorderLayout.CENTER );
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
			((JList)this.scrollComponent).setCellRenderer( BuffBotHome.getMessageRenderer() );
		}

		public void setEnabled( boolean isEnabled )
		{
			if ( this.confirmedButton == null )
				return;

			this.confirmedButton.setEnabled( isEnabled );
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
			super( "add", "remove", new Dimension( 150, 20 ),  new Dimension( 300, 20 ));
			UseSkillRequest skill;

			LockableListModel skillSet = usableSkills;
			LockableListModel buffSet = new LockableListModel();
			for (int i = 0; (skill = (UseSkillRequest) skillSet.get(i)) != null; ++i )
				if (ClassSkillsDatabase.isBuff( ClassSkillsDatabase.getSkillId( skill.getSkillName() ) ))
					buffSet.add( skill );

			this.skillSelect = new JComboBox( buffSet );

			this.priceField = new JTextField();
			this.countField = new JTextField();
			this.restrictBox = new JCheckBox();
			this.singletonBox = new JCheckBox();

			VerifiableElement [] elements = new VerifiableElement[5];
			elements[0] = new VerifiableElement( "Buff to cast: ", this.skillSelect );
			elements[1] = new VerifiableElement( "Price (in meat): ", this.priceField );
			elements[2] = new VerifiableElement( "# of casts: ", this.countField );
			elements[3] = new VerifiableElement( "Clanmates only?", this.restrictBox );
			elements[4] = new VerifiableElement( "Limit requests?", this.singletonBox );
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			BuffBotManager.addBuff( ((UseSkillRequest) this.skillSelect.getSelectedItem()).getSkillName(),
				StaticEntity.parseInt( this.priceField.getText() ), StaticEntity.parseInt( this.countField.getText() ),
					this.restrictBox.isSelected(), this.singletonBox.isSelected() );
		}

		public void actionCancelled()
		{	BuffBotManager.removeBuffs( BuffBotFrame.this.buffListDisplay.getSelectedValues() );
		}
	}

	private class BuffListPanel extends JPanel
	{
		public BuffListPanel()
		{
			this.setLayout( new BorderLayout() );
			this.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
			this.add( JComponentUtilities.createLabel( "Active Buffing List", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );

			BuffBotFrame.this.buffListDisplay = new JList( BuffBotManager.getBuffCostTable() );
			BuffBotFrame.this.buffListDisplay.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
			BuffBotFrame.this.buffListDisplay.setVisibleRowCount( 5 );

			this.add( new SimpleScrollPane( BuffBotFrame.this.buffListDisplay ), BorderLayout.CENTER );
		}
	}

	/**
	 * Internal class used to handle everything related to
	 * BuffBot White List management
	 */

	private class MainSettingsPanel extends KoLPanel
	{
		private JComboBox philanthropyModeSelect;
		private JComboBox messageDisposalSelect;
		private JCheckBox [] mpRestoreCheckbox;

		public MainSettingsPanel()
		{
			super( new Dimension( 150, 20 ),  new Dimension( 300, 20 ) );

			LockableListModel philanthropyModeChoices = new LockableListModel();
			philanthropyModeChoices.add( "Disabled" );
			philanthropyModeChoices.add( "Once per week" );
			this.philanthropyModeSelect = new JComboBox( philanthropyModeChoices );

			LockableListModel messageDisposalChoices = new LockableListModel();
			messageDisposalChoices.add( "Auto-save non-requests" );
			messageDisposalChoices.add( "Auto-delete non-requests" );
			messageDisposalChoices.add( "Do nothing to non-requests" );
			this.messageDisposalSelect = new JComboBox( messageDisposalChoices );

			VerifiableElement [] elements = new VerifiableElement[3];
			elements[0] = new VerifiableElement( "Philanthropy: ", this.philanthropyModeSelect );
			elements[1] = new VerifiableElement( "Message disposal: ", this.messageDisposalSelect );
			elements[2] = new VerifiableElement( "Mana restores: ", BuffBotFrame.this.constructScroller( this.mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) );

			this.actionCancelled();
			this.setContent( elements );
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "buffBotPhilanthropyType", String.valueOf( this.philanthropyModeSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "buffBotMessageDisposal", String.valueOf( this.messageDisposalSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "mpAutoRecoveryItems", BuffBotFrame.this.getSettingString( this.mpRestoreCheckbox ) );
		}

		public void actionCancelled()
		{
			this.philanthropyModeSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "buffBotPhilanthropyType" ) );
			this.messageDisposalSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "buffBotMessageDisposal" ) );
		}
	}

	private class OtherSettingsPanel extends KoLPanel
	{
		private JTextArea invalidPriceMessage, thanksMessage;

		public OtherSettingsPanel()
		{
			super( "save", "reset", new Dimension( 120, 20 ),  new Dimension( 300, 20 ) );
			this.setContent( new VerifiableElement[0] );

			this.invalidPriceMessage = new JTextArea();
			this.thanksMessage = new JTextArea();

			this.invalidPriceMessage.setFont( DEFAULT_FONT );
			this.invalidPriceMessage.setLineWrap( true );
			this.invalidPriceMessage.setWrapStyleWord( true );

			this.thanksMessage.setFont( DEFAULT_FONT );
			this.thanksMessage.setLineWrap( true );
			this.thanksMessage.setWrapStyleWord( true );

			JPanel settingsMiddlePanel = new JPanel( new BorderLayout() );
			settingsMiddlePanel.add( JComponentUtilities.createLabel( "Invalid Buff Price Message", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			settingsMiddlePanel.add( new SimpleScrollPane( this.invalidPriceMessage ), BorderLayout.CENTER );

			JPanel settingsBottomPanel = new JPanel( new BorderLayout() );
			settingsBottomPanel.add( JComponentUtilities.createLabel( "Donation Thanks Message", JLabel.CENTER,
				Color.black, Color.white ), BorderLayout.NORTH );
			settingsBottomPanel.add( new SimpleScrollPane( this.thanksMessage ), BorderLayout.CENTER );

			JPanel settingsPanel = new JPanel( new GridLayout( 3, 1, 10, 10 ) );

			JComponentUtilities.setComponentSize( settingsMiddlePanel, 300, 120 );
			JComponentUtilities.setComponentSize( settingsBottomPanel, 300, 120 );

			settingsPanel.add( settingsMiddlePanel );
			settingsPanel.add( settingsBottomPanel );

			this.container.add( settingsPanel, BorderLayout.CENTER );
			this.actionCancelled();
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "invalidBuffMessage", this.invalidPriceMessage.getText() );
			StaticEntity.setProperty( "thanksMessage", this.thanksMessage.getText() );
		}

		public void actionCancelled()
		{
			this.invalidPriceMessage.setText( StaticEntity.getProperty( "invalidBuffMessage" ) );
			this.thanksMessage.setText( StaticEntity.getProperty( "thanksMessage" ) );
		}
	}
}
