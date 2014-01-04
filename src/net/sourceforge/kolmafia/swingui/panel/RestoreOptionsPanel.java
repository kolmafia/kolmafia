/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.swingui.panel;

import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.moods.HPRestoreItemList;
import net.sourceforge.kolmafia.moods.MPRestoreItemList;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.swingui.widget.GenericScrollPane;

public class RestoreOptionsPanel
	extends JPanel
{
	protected JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect, hpHaltCombatSelect;
	protected JCheckBox[] hpRestoreCheckbox;
	protected JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect, mpBalanceTriggerSelect, mpBalanceSelect;
	protected JCheckBox[] mpRestoreCheckbox;

	private boolean restoring = false;

	public RestoreOptionsPanel()
	{
		super( new GridLayout( 1, 2, 10, 10 ) );

		JPanel healthPanel = new JPanel();
		healthPanel.add( new HealthOptionsPanel() );

		JPanel manaPanel = new JPanel();
		manaPanel.add( new ManaOptionsPanel() );

		this.add( healthPanel );
		this.add( manaPanel );

		CheckboxListener listener = new CheckboxListener();
		for ( int i = 0; i < this.hpRestoreCheckbox.length; ++i )
		{
			this.hpRestoreCheckbox[ i ].addActionListener( listener );
		}
		for ( int i = 0; i < this.mpRestoreCheckbox.length; ++i )
		{
			this.mpRestoreCheckbox[ i ].addActionListener( listener );
		}
	}

	public void updateFromPreferences()
	{
		this.restoreRestoreSettings();
	}

	private GenericScrollPane constructScroller( final JCheckBox[] restoreCheckbox )
	{
		JPanel checkboxPanel = new JPanel( new GridLayout( restoreCheckbox.length, 1 ) );
		for ( int i = 0; i < restoreCheckbox.length; ++i )
		{
			checkboxPanel.add( restoreCheckbox[ i ] );
		}

		return new GenericScrollPane(
			checkboxPanel, GenericScrollPane.VERTICAL_SCROLLBAR_NEVER, GenericScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
	}

	private JPanel constructLabelPair( final String label, final JComponent element1 )
	{
		return this.constructLabelPair( label, element1, null );
	}

	private JPanel constructLabelPair( final String label, final JComponent element1, final JComponent element2 )
	{
		JPanel container = new JPanel();
		container.setLayout( new BoxLayout( container, BoxLayout.Y_AXIS ) );

		if ( element1 != null && element1 instanceof JComboBox )
		{
			JComponentUtilities.setComponentSize( element1, 240, 20 );
		}

		if ( element2 != null && element2 instanceof JComboBox )
		{
			JComponentUtilities.setComponentSize( element2, 240, 20 );
		}

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

	private void saveRestoreSettings()
	{
		if ( this.restoring )
		{
			return;
		}

		Preferences.setFloat(
			"autoAbortThreshold", this.getPercentage( this.hpHaltCombatSelect ) );
		Preferences.setFloat( "hpAutoRecovery", this.getPercentage( this.hpAutoRecoverSelect ) );
		Preferences.setFloat(
			"hpAutoRecoveryTarget", this.getPercentage( this.hpAutoRecoverTargetSelect ) );
		Preferences.setString( "hpAutoRecoveryItems", this.getSettingString( this.hpRestoreCheckbox ) );

		Preferences.setFloat( "manaBurningTrigger", this.getPercentage( this.mpBalanceTriggerSelect ) );
		Preferences.setFloat( "manaBurningThreshold", this.getPercentage( this.mpBalanceSelect ) );
		Preferences.setFloat( "mpAutoRecovery", this.getPercentage( this.mpAutoRecoverSelect ) );
		Preferences.setFloat(
			"mpAutoRecoveryTarget", this.getPercentage( this.mpAutoRecoverTargetSelect ) );
		Preferences.setString( "mpAutoRecoveryItems", this.getSettingString( this.mpRestoreCheckbox ) );
	}

	private String getSettingString( final JCheckBox[] restoreCheckbox )
	{
		StringBuffer restoreSetting = new StringBuffer();

		for ( int i = 0; i < restoreCheckbox.length; ++i )
		{
			if ( restoreCheckbox[ i ].isSelected() )
			{
				if ( restoreSetting.length() != 0 )
				{
					restoreSetting.append( ';' );
				}

				restoreSetting.append( restoreCheckbox[ i ].getText().toLowerCase() );
			}
		}

		return restoreSetting.toString();
	}

	private float getPercentage( final JComboBox option )
	{
		return ( option.getSelectedIndex() - 1 ) / 20.0f;
	}

	private void restoreRestoreSettings()
	{
		this.restoring = true;
		this.setSelectedIndex( this.hpHaltCombatSelect, "autoAbortThreshold" );
		this.setSelectedIndex( this.hpAutoRecoverSelect, "hpAutoRecovery" );
		this.setSelectedIndex( this.hpAutoRecoverTargetSelect, "hpAutoRecoveryTarget" );

		HPRestoreItemList.updateCheckboxes( this.hpRestoreCheckbox );

		this.setSelectedIndex( this.mpBalanceTriggerSelect, "manaBurningTrigger" );
		this.setSelectedIndex( this.mpBalanceSelect, "manaBurningThreshold" );
		this.setSelectedIndex( this.mpAutoRecoverSelect, "mpAutoRecovery" );
		this.setSelectedIndex( this.mpAutoRecoverTargetSelect, "mpAutoRecoveryTarget" );
		MPRestoreItemList.updateCheckboxes( this.mpRestoreCheckbox );
		this.restoring = false;
	}

	private void setSelectedIndex( final JComboBox option, final String property )
	{
		int desiredIndex = (int) ( Preferences.getFloat( property ) * 20.0f + 1 );
		option.setSelectedIndex( Math.min( Math.max( desiredIndex, 0 ), option.getItemCount() ) );
	}

	private class CheckboxListener
		implements ActionListener
	{
		public void actionPerformed( final ActionEvent e )
		{
			RestoreOptionsPanel.this.saveRestoreSettings();
		}
	}

	private class HealthOptionsPanel
		extends JPanel
		implements ActionListener
	{
		public boolean refreshSoon = false;

		public HealthOptionsPanel()
		{
			RestoreOptionsPanel.this.hpHaltCombatSelect = new JComboBox();
			RestoreOptionsPanel.this.hpHaltCombatSelect.addItem( "Stop if auto-recovery fails" );
			for ( int i = 0; i <= 19; ++i )
			{
				RestoreOptionsPanel.this.hpHaltCombatSelect.addItem( "Stop if health at " + i * 5 + "%" );
			}

			RestoreOptionsPanel.this.hpAutoRecoverSelect = new JComboBox();
			RestoreOptionsPanel.this.hpAutoRecoverSelect.addItem( "Do not auto-recover health" );
			for ( int i = 0; i <= 19; ++i )
			{
				RestoreOptionsPanel.this.hpAutoRecoverSelect.addItem( "Auto-recover health at " + i * 5 + "%" );
			}

			RestoreOptionsPanel.this.hpAutoRecoverTargetSelect = new JComboBox();
			RestoreOptionsPanel.this.hpAutoRecoverTargetSelect.addItem( "Do not recover health" );
			for ( int i = 0; i <= 20; ++i )
			{
				RestoreOptionsPanel.this.hpAutoRecoverTargetSelect.addItem( "Try to recover up to " + i * 5 + "% health" );
			}

			// Add the elements to the panel

			this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			this.add( RestoreOptionsPanel.this.constructLabelPair(
				"Stop automation: ", RestoreOptionsPanel.this.hpHaltCombatSelect ) );
			this.add( Box.createVerticalStrut( 25 + 15 ) );

			this.add( RestoreOptionsPanel.this.constructLabelPair(
				"Restore your health: ", RestoreOptionsPanel.this.hpAutoRecoverSelect,
				RestoreOptionsPanel.this.hpAutoRecoverTargetSelect ) );
			this.add( Box.createVerticalStrut( 15 ) );

			this.add( RestoreOptionsPanel.this.constructLabelPair(
				"Use these restores: ",
				RestoreOptionsPanel.this.constructScroller( RestoreOptionsPanel.this.hpRestoreCheckbox = HPRestoreItemList.getCheckboxes() ) ) );

			RestoreOptionsPanel.this.setSelectedIndex( RestoreOptionsPanel.this.hpHaltCombatSelect, "autoAbortThreshold" );
			RestoreOptionsPanel.this.setSelectedIndex( RestoreOptionsPanel.this.hpAutoRecoverSelect, "hpAutoRecovery" );
			RestoreOptionsPanel.this.setSelectedIndex(
				RestoreOptionsPanel.this.hpAutoRecoverTargetSelect, "hpAutoRecoveryTarget" );

			RestoreOptionsPanel.this.hpHaltCombatSelect.addActionListener( this );
			RestoreOptionsPanel.this.hpAutoRecoverSelect.addActionListener( this );
			RestoreOptionsPanel.this.hpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < RestoreOptionsPanel.this.hpRestoreCheckbox.length; ++i )
			{
				RestoreOptionsPanel.this.hpRestoreCheckbox[ i ].addActionListener( this );
			}
		}

		public void actionPerformed( final ActionEvent e )
		{
			RestoreOptionsPanel.this.saveRestoreSettings();
		}
	}

	private class ManaOptionsPanel
		extends JPanel
		implements ActionListener
	{
		public ManaOptionsPanel()
		{
			RestoreOptionsPanel.this.mpBalanceTriggerSelect = new JComboBox();
			RestoreOptionsPanel.this.mpBalanceTriggerSelect.addItem( "Start recasting immediately" );
			for ( int i = 0; i <= 20; ++i )
			{
				RestoreOptionsPanel.this.mpBalanceTriggerSelect.addItem( "Start recasting at " + i * 5 + "%" );
			}

			RestoreOptionsPanel.this.mpBalanceSelect = new JComboBox();
			RestoreOptionsPanel.this.mpBalanceSelect.addItem( "Do not rebalance buffs" );
			for ( int i = 0; i <= 19; ++i )
			{
				RestoreOptionsPanel.this.mpBalanceSelect.addItem( "Recast buffs down to " + i * 5 + "%" );
			}

			RestoreOptionsPanel.this.mpAutoRecoverSelect = new JComboBox();
			RestoreOptionsPanel.this.mpAutoRecoverSelect.addItem( "Do not auto-recover mana" );
			for ( int i = 0; i <= 19; ++i )
			{
				RestoreOptionsPanel.this.mpAutoRecoverSelect.addItem( "Auto-recover mana at " + i * 5 + "%" );
			}

			RestoreOptionsPanel.this.mpAutoRecoverTargetSelect = new JComboBox();
			RestoreOptionsPanel.this.mpAutoRecoverTargetSelect.addItem( "Do not auto-recover mana" );
			for ( int i = 0; i <= 20; ++i )
			{
				RestoreOptionsPanel.this.mpAutoRecoverTargetSelect.addItem( "Try to recover up to " + i * 5 + "% mana" );
			}

			// Add the elements to the panel

			this.setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

			this.add( RestoreOptionsPanel.this.constructLabelPair(
				"Mana burning: ", RestoreOptionsPanel.this.mpBalanceTriggerSelect,
				RestoreOptionsPanel.this.mpBalanceSelect ) );
			this.add( Box.createVerticalStrut( 15 ) );

			this.add( RestoreOptionsPanel.this.constructLabelPair(
				"Restore your mana: ", RestoreOptionsPanel.this.mpAutoRecoverSelect,
				RestoreOptionsPanel.this.mpAutoRecoverTargetSelect ) );
			this.add( Box.createVerticalStrut( 15 ) );

			this.add( RestoreOptionsPanel.this.constructLabelPair(
				"Use these restores: ",
				RestoreOptionsPanel.this.constructScroller( RestoreOptionsPanel.this.mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) ) );

			RestoreOptionsPanel.this.setSelectedIndex( RestoreOptionsPanel.this.mpBalanceTriggerSelect, "manaBurningTrigger" );
			RestoreOptionsPanel.this.setSelectedIndex( RestoreOptionsPanel.this.mpBalanceSelect, "manaBurningThreshold" );
			RestoreOptionsPanel.this.setSelectedIndex( RestoreOptionsPanel.this.mpAutoRecoverSelect, "mpAutoRecovery" );
			RestoreOptionsPanel.this.setSelectedIndex(
				RestoreOptionsPanel.this.mpAutoRecoverTargetSelect, "mpAutoRecoveryTarget" );

			RestoreOptionsPanel.this.mpBalanceTriggerSelect.addActionListener( this );
			RestoreOptionsPanel.this.mpBalanceSelect.addActionListener( this );
			RestoreOptionsPanel.this.mpAutoRecoverSelect.addActionListener( this );
			RestoreOptionsPanel.this.mpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < RestoreOptionsPanel.this.mpRestoreCheckbox.length; ++i )
			{
				RestoreOptionsPanel.this.mpRestoreCheckbox[ i ].addActionListener( this );
			}
		}

		public void actionPerformed( final ActionEvent e )
		{
			RestoreOptionsPanel.this.saveRestoreSettings();
		}
	}
}
