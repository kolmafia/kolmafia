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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RestoreOptionsFrame extends KoLFrame
{
	private JTextField betweenBattleScriptField;

	private JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect;
	private JCheckBox [] hpRestoreCheckbox;

	private JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect;
	private JCheckBox [] mpRestoreCheckbox;

	public RestoreOptionsFrame()
	{
		super( "Auto-Restore" );

		JPanel restorePanel = new JPanel();
		restorePanel.setLayout( new BoxLayout( restorePanel, BoxLayout.Y_AXIS ) );

		restorePanel.add( new HealthOptionsPanel() );
		restorePanel.add( new ManaOptionsPanel() );

		CheckboxListener listener = new CheckboxListener();
		for ( int i = 0; i < hpRestoreCheckbox.length; ++i )
			hpRestoreCheckbox[i].addActionListener( listener );
		for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
			mpRestoreCheckbox[i].addActionListener( listener );

		JScrollPane restoreScroller = new JScrollPane( restorePanel,
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( restoreScroller, 600, 300 );
		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( restoreScroller, "" );
	}

	private void saveRestoreSettings()
	{
		StaticEntity.setProperty( "betweenBattleScript", betweenBattleScriptField.getText() );

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
			VerifiableElement [] elements = new VerifiableElement[5];

			elements[ currentElementCount++ ] = new VerifiableElement( "Between Battles: ", new ScriptSelectPanel( betweenBattleScriptField ) );
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
			hpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( StaticEntity.getProperty( "hpAutoRecovery" ) ) * 10) + 1 );
			hpAutoRecoverTargetSelect.setSelectedIndex( (int)(Double.parseDouble( StaticEntity.getProperty( "hpAutoRecoveryTarget" ) ) * 10) + 1 );
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
			mpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( StaticEntity.getProperty( "mpAutoRecovery" ) ) * 10) + 1 );
			mpAutoRecoverTargetSelect.setSelectedIndex( (int)(Double.parseDouble( StaticEntity.getProperty( "mpAutoRecoveryTarget" ) ) * 10) + 1 );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}
	}
}
