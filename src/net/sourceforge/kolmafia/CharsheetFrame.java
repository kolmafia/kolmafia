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
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CharsheetFrame
	extends AdventureOptionsFrame
{
	private static CharsheetFrame INSTANCE = null;
	private static boolean shouldAddExtraTabs = true;

	private final JLabel avatar;
	private JLabel[] statusLabel;
	private JProgressBar[] tnpDisplay;
	private final KoLCharacterAdapter statusRefresher;

	/**
	 * Constructs a new character sheet, using the data located in the provided session.
	 */

	public CharsheetFrame()
	{
		super( "Player Status" );

		CharsheetFrame.INSTANCE = this;
		this.framePanel.setLayout( new BorderLayout( 20, 20 ) );

		JPanel statusPanel = new JPanel( new BorderLayout( 20, 20 ) );

		this.avatar = new JLabel( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );

		statusPanel.add( this.createStatusPanel(), BorderLayout.CENTER );
		statusPanel.add( this.avatar, BorderLayout.WEST );

		JPanel statusContainer = new JPanel( new CardLayout( 10, 10 ) );
		statusContainer.add( statusPanel, "" );

		this.framePanel.add( this.getSouthernTabs(), BorderLayout.CENTER );

		JPanel sessionContainer = new JPanel( new CardLayout( 10, 10 ) );
		sessionContainer.add( this.getAdventureSummary( "charsheetDropdown", this.locationSelect ), "" );

		JPanel northPanel = new JPanel( new BorderLayout() );
		northPanel.add( statusContainer, BorderLayout.WEST );
		northPanel.add( sessionContainer, BorderLayout.CENTER );

		this.framePanel.add( northPanel, BorderLayout.NORTH );

		this.statusRefresher = new KoLCharacterAdapter( new StatusRefreshRunnable() );
		KoLCharacter.addCharacterListener( this.statusRefresher );

		this.statusRefresher.updateStatus();
		CharsheetFrame.updateSelectedAdventure( AdventureDatabase.getAdventure( KoLSettings.getUserProperty( "lastAdventure" ) ) );
	}

	public boolean useSidePane()
	{
		return true;
	}

	public void dispose()
	{
		KoLCharacter.removeCharacterListener( this.statusRefresher );
		super.dispose();
	}

	public static final void removeExtraTabs()
	{
		CharsheetFrame.shouldAddExtraTabs = false;
		if ( CharsheetFrame.INSTANCE == null || CharsheetFrame.INSTANCE.tabs == null )
		{
			return;
		}

		for ( int i = CharsheetFrame.INSTANCE.tabs.getTabCount() - 1; i > 0; --i )
		{
			CharsheetFrame.INSTANCE.tabs.remove( i );
		}
	}

	public UnfocusedTabbedPane getSouthernTabs()
	{
		if ( CharsheetFrame.shouldAddExtraTabs )
		{
			super.getSouthernTabs();
		}

		JPanel locationDetails = new JPanel( new BorderLayout( 10, 10 ) );
		locationDetails.add( new AdventureSelectPanel( false ), BorderLayout.WEST );
		locationDetails.add( new SafetyField( this.locationSelect ), BorderLayout.CENTER );

		JPanel locationHolder = new JPanel( new CardLayout( 10, 10 ) );
		locationHolder.add( locationDetails, "" );

		this.tabs.insertTab( "Overview", null, locationHolder, null, 0 );
		return this.tabs;
	}

	/**
	 * Utility method for creating a panel that displays the given label, using formatting if the values are different.
	 */

	private JPanel createValuePanel( final String title, final int displayIndex )
	{
		int index1 = 2 * displayIndex;
		int index2 = index1 + 1;

		this.statusLabel[ index1 ] = new JLabel( " ", SwingConstants.LEFT );
		this.statusLabel[ index1 ].setForeground( Color.BLUE );
		this.statusLabel[ index2 ] = new JLabel( " ", SwingConstants.LEFT );

		JPanel headerPanel = new JPanel();
		headerPanel.setLayout( new BoxLayout( headerPanel, BoxLayout.X_AXIS ) );

		headerPanel.add( new JLabel( title + ":  ", SwingConstants.RIGHT ) );
		headerPanel.add( this.statusLabel[ index1 ] );
		headerPanel.add( this.statusLabel[ index2 ] );

		JPanel valuePanel = new JPanel( new BorderLayout( 2, 2 ) );
		valuePanel.add( headerPanel, BorderLayout.EAST );
		valuePanel.add( this.tnpDisplay[ displayIndex ], BorderLayout.SOUTH );

		return valuePanel;
	}

	/**
	 * Utility method for modifying a panel that displays the given label, using formatting if the values are different.
	 */

	private void refreshValuePanel( final int displayIndex, final int baseValue, final int adjustedValue,
		final int tillNextPoint )
	{
		int index1 = 2 * displayIndex;
		int index2 = index1 + 1;

		JLabel adjustedLabel = this.statusLabel[ index1 ];
		JLabel baseLabel = this.statusLabel[ index2 ];

		adjustedLabel.setText( KoLConstants.COMMA_FORMAT.format( adjustedValue ) );
		baseLabel.setText( " (" + KoLConstants.COMMA_FORMAT.format( baseValue ) + ")" );

		this.tnpDisplay[ displayIndex ].setMaximum( 2 * baseValue + 1 );
		this.tnpDisplay[ displayIndex ].setValue( 2 * baseValue + 1 - tillNextPoint );
		this.tnpDisplay[ displayIndex ].setString( KoLConstants.COMMA_FORMAT.format( this.tnpDisplay[ displayIndex ].getValue() ) + " / " + KoLConstants.COMMA_FORMAT.format( this.tnpDisplay[ displayIndex ].getMaximum() ) );
	}

	/**
	 * Utility method for creating a panel displaying the character's vital statistics, including a basic stat overview
	 * and available turns/meat.
	 * 
	 * @return a <code>JPanel</code> displaying the character's statistics
	 */

	private JPanel createStatusPanel()
	{
		JPanel statusLabelPanel = new JPanel();
		statusLabelPanel.setLayout( new BoxLayout( statusLabelPanel, BoxLayout.Y_AXIS ) );

		this.statusLabel = new JLabel[ 6 ];
		for ( int i = 0; i < 6; ++i )
		{
			this.statusLabel[ i ] = new JLabel( " ", SwingConstants.CENTER );
		}

		this.tnpDisplay = new JProgressBar[ 3 ];
		for ( int i = 0; i < 3; ++i )
		{
			this.tnpDisplay[ i ] = new JProgressBar();
			this.tnpDisplay[ i ].setValue( 0 );
			this.tnpDisplay[ i ].setStringPainted( true );
		}

		JPanel primeStatPanel = new JPanel( new GridLayout( 3, 1, 5, 5 ) );
		primeStatPanel.add( this.createValuePanel( "Muscle", 0 ) );
		primeStatPanel.add( this.createValuePanel( "Mysticality", 1 ) );
		primeStatPanel.add( this.createValuePanel( "Moxie", 2 ) );
		statusLabelPanel.add( primeStatPanel );

		return statusLabelPanel;
	}

	private class StatusRefreshRunnable
		implements Runnable
	{
		public void run()
		{
			CharsheetFrame.this.refreshValuePanel(
				0, KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() );
			CharsheetFrame.this.refreshValuePanel(
				1, KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(),
				KoLCharacter.getMysticalityTNP() );
			CharsheetFrame.this.refreshValuePanel(
				2, KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() );

			// Set the current avatar
			CharsheetFrame.this.avatar.setIcon( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );
		}
	}

	public static final void updateSelectedAdventure( final KoLAdventure location )
	{
		if ( CharsheetFrame.INSTANCE == null || location == null || CharsheetFrame.INSTANCE.zoneSelect == null || CharsheetFrame.INSTANCE.locationSelect == null )
		{
			return;
		}

		if ( CharsheetFrame.INSTANCE.locationSelect.getSelectedValue() == location || !KoLConstants.conditions.isEmpty() )
		{
			return;
		}

		if ( CharsheetFrame.INSTANCE.zoneSelect instanceof FilterAdventureField )
		{
			( (FilterAdventureField) CharsheetFrame.INSTANCE.zoneSelect ).setText( location.getZone() );
		}
		else
		{
			( (JComboBox) CharsheetFrame.INSTANCE.zoneSelect ).setSelectedItem( location.getParentZoneDescription() );
		}

		CharsheetFrame.INSTANCE.locationSelect.setSelectedValue( location, true );
		CharsheetFrame.INSTANCE.locationSelect.ensureIndexIsVisible( CharsheetFrame.INSTANCE.locationSelect.getSelectedIndex() );
	}
}
