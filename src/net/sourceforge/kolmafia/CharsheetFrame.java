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
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class CharsheetFrame extends KoLFrame
{
	private JLabel avatar;
	private JLabel [] statusLabel;
	private JProgressBar [] tnpDisplay;
	private JProgressBar levelMeter;

	private KoLCharacterAdapter statusRefresher;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 */

	public CharsheetFrame()
	{
		super( "Player Status" );

		framePanel.setLayout( new BorderLayout( 20, 20 ) );

		JPanel statusPanel = new JPanel( new BorderLayout( 20, 20 ) );

		this.avatar = new JLabel( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );

		statusPanel.add( createStatusPanel(), BorderLayout.CENTER );
		statusPanel.add( this.avatar, BorderLayout.WEST );

		JPanel statusContainer = new JPanel( new CardLayout( 10, 10 ) );
		statusContainer.add( statusPanel, "" );

		JPanel northPanel = new JPanel( new BorderLayout( 20, 20 ) );
		northPanel.add( statusContainer, BorderLayout.WEST );
		northPanel.add( new ItemManagePanel( availableSkills ), BorderLayout.CENTER );

		JSplitPane southGrid = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true,
			getAdventureSummary( "charsheetDropdown" ), getAdventureSummary( "charsheetDropdown2" ) );

		southGrid.setDividerLocation( 0.5 );
		southGrid.setResizeWeight( 0.5 );

		framePanel.add( northPanel, BorderLayout.NORTH );
		framePanel.add( southGrid, BorderLayout.CENTER );

		statusRefresher = new KoLCharacterAdapter( new StatusRefreshRunnable() );
		KoLCharacter.addCharacterListener( statusRefresher );

		statusRefresher.updateStatus();
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
}
