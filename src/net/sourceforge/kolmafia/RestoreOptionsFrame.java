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
import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

public class RestoreOptionsFrame extends KoLFrame
{
	public RestoreOptionsFrame()
	{
		super( "Auto-Restore" );
		tabs = new JTabbedPane();
		
		tabs.add( "Health", new HealthOptionsPanel() );
		tabs.add( "Mana", new ManaOptionsPanel() );
		
		framePanel.setLayout( new BorderLayout() );
		framePanel.add( tabs, BorderLayout.CENTER );
	}

	private class HealthOptionsPanel extends KoLPanel
	{
		private boolean refreshSoon = false;
		private JTextField betweenBattleScriptField;
		private JComboBox hpAutoRecoverSelect;
		private JTextField hpRecoveryScriptField;

		private JCheckBox [] hpRestoreCheckbox;

		public HealthOptionsPanel()
		{
			super( "save", "reload", new Dimension( 130, 20 ), new Dimension( 260, 20 ) );
			betweenBattleScriptField = new JTextField();

			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover HP" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverSelect.addItem( "Autorecover HP at " + (i * 10) + "%" );

			hpRecoveryScriptField = new JTextField();

			// Add the elements to the panel

			int currentElementCount = 0;
			VerifiableElement [] elements = new VerifiableElement[5];

			elements[ currentElementCount++ ] = new VerifiableElement( "Between Battles: ", new ScriptSelectPanel( betweenBattleScriptField ) );
			elements[ currentElementCount++ ] = new VerifiableElement( "", new JLabel() );

			elements[ currentElementCount++ ] = new VerifiableElement( "HP Auto-Recovery: ", hpAutoRecoverSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "HP Recovery Script: ", new ScriptSelectPanel( hpRecoveryScriptField ) );
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
		{
			setProperty( "betweenBattleScript", betweenBattleScriptField.getText() );
			setProperty( "hpAutoRecover", String.valueOf( ((double)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
			setProperty( "hpRecoveryScript", hpRecoveryScriptField.getText() );
			setProperty( "hpRestores", getSettingString( hpRestoreCheckbox ) );

			JOptionPane.showMessageDialog( null, "Settings have been saved." );
		}

		protected void actionCancelled()
		{
			betweenBattleScriptField.setText( getProperty( "betweenBattleScript" ) );
			hpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "hpAutoRecover" ) ) * 10) + 1 );
			hpRecoveryScriptField.setText( getProperty( "hpRecoveryScript" ) );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}
	}
	
	private class ManaOptionsPanel extends KoLPanel
	{
		private JComboBox mpAutoRecoverSelect;
		private JTextField mpRecoveryScriptField;

		private JCheckBox [] mpRestoreCheckbox;

		public ManaOptionsPanel()
		{
			super( "save", "reload", new Dimension( 130, 20 ), new Dimension( 260, 20 ) );

			mpAutoRecoverSelect = new JComboBox();
			mpAutoRecoverSelect.addItem( "Do not autorecover MP" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverSelect.addItem( "Autorecover MP at " + (i * 10) + "%" );

			mpRecoveryScriptField = new JTextField();

			// Add the elements to the panel

			int currentElementCount = 0;
			VerifiableElement [] elements = new VerifiableElement[3];

			elements[ currentElementCount++ ] = new VerifiableElement( "MP Auto-Recovery: ", mpAutoRecoverSelect );
			elements[ currentElementCount++ ] = new VerifiableElement( "MP Recovery Script: ", new ScriptSelectPanel( mpRecoveryScriptField ) );
			elements[ currentElementCount++ ] = new VerifiableElement( "Use these restores: ", constructScroller( mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) );

			setContent( elements );
			actionCancelled();
		}

		public void setEnabled( boolean isEnabled )
		{
		}

		protected void actionConfirmed()
		{
			setProperty( "mpAutoRecover", String.valueOf( ((double)(mpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0) ) );
			setProperty( "mpRecoveryScript", mpRecoveryScriptField.getText() );
			setProperty( "mpRestores", getSettingString( mpRestoreCheckbox ) );

			JOptionPane.showMessageDialog( null, "Settings have been saved." );
		}

		protected void actionCancelled()
		{
			mpAutoRecoverSelect.setSelectedIndex( (int)(Double.parseDouble( getProperty( "mpAutoRecover" ) ) * 10) + 1 );
			mpRecoveryScriptField.setText( getProperty( "mpRecoveryScript" ) );
		}

		protected boolean shouldAddStatusLabel( VerifiableElement [] elements )
		{	return false;
		}
	}
}
