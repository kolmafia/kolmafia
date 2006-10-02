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

// layout
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;

// containers
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

// event listeners
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

// utilities
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> used to display the character
 * sheet for the current user.  Note that this can only be instantiated
 * when the character is logged in; if the character has logged out,
 * this method will contain blank data.
 */

public class CharsheetFrame extends KoLFrame
{
	private JPanel levelPanel;
	private JLabel levelLabel;

	private JLabel avatar;
	private JLabel [] statusLabel;
	private JProgressBar [] tnpDisplay;
	private JProgressBar levelMeter, hpMeter, mpMeter;

	private KoLCharacterAdapter statusRefresher;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 */

	public CharsheetFrame()
	{
		super( "Player Status" );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		JPanel northPanel = new JPanel( new BorderLayout( 20, 20 ) );

		northPanel.add( createStatusPanel(), BorderLayout.CENTER );
		northPanel.add( createImagePanel(), BorderLayout.WEST );

		JScrollPane scroller = new JScrollPane( new JList( activeEffects ),
			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JPanel entirePanel = new JPanel( new BorderLayout( 20, 20 ) );
		entirePanel.add( northPanel, BorderLayout.NORTH );
		entirePanel.add( scroller, BorderLayout.CENTER );

		framePanel.add( entirePanel, "" );
		statusRefresher = new KoLCharacterAdapter( new StatusRefreshRunnable() );
		KoLCharacter.addCharacterListener( statusRefresher );

		statusRefresher.updateStatus();
	}

	public void dispose()
	{
		KoLCharacter.removeCharacterListener( statusRefresher );
		super.dispose();
	}

	/**
	 * Utility method used for creating a panel displaying the character's
	 * avatar.
	 *
	 * @return	a <code>JPanel</code> displaying the class-specific avatar
	 */

	private JPanel createImagePanel()
	{
		JPanel imagePanel = new JPanel( new BorderLayout( 10, 10 ) );
		JPanel namePanel = new JPanel( new GridLayout( 2, 1 ) );
		namePanel.add( new JLabel( KoLCharacter.getUsername() + " (#" + KoLCharacter.getUserID() + ")", JLabel.CENTER ) );

		this.levelLabel = new JLabel( "Level " + KoLCharacter.getLevel() + " " + KoLCharacter.getClassType(), JLabel.CENTER );
		namePanel.add( levelLabel );

		this.levelPanel = new JPanel( new BorderLayout() );
		levelPanel.add( namePanel, BorderLayout.CENTER );

		this.levelMeter = new JProgressBar();
		levelMeter.setValue( 0 );
		levelMeter.setStringPainted( true );
		JComponentUtilities.setComponentSize( levelMeter, 40, 5 );

		levelPanel.add( levelMeter, BorderLayout.SOUTH );
		imagePanel.add( levelPanel, BorderLayout.NORTH );

		this.avatar = new JLabel( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );
		imagePanel.add( avatar, BorderLayout.CENTER );
		imagePanel.add( new RequestButton( "Refresh Status", new CharsheetRequest() ), BorderLayout.SOUTH );


		return imagePanel;
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

		hpMeter = new JProgressBar();
		hpMeter.setValue( 0 );
		hpMeter.setStringPainted( true );

		JComponentUtilities.setComponentSize( hpMeter, 100, 20 );

		mpMeter = new JProgressBar();
		mpMeter.setValue( 0 );
		mpMeter.setStringPainted( true );

		JComponentUtilities.setComponentSize( mpMeter, 100, 20 );

		JPanel hpPanel = new JPanel( new BorderLayout( 5, 5 ) );
		hpPanel.add( new JLabel( JComponentUtilities.getImage( "hp.gif" ), JLabel.CENTER ), BorderLayout.CENTER );
		hpPanel.add( hpMeter, BorderLayout.SOUTH );

		JPanel mpPanel = new JPanel( new BorderLayout( 5, 5 ) );
		mpPanel.add( new JLabel( JComponentUtilities.getImage( "mp.gif" ), JLabel.CENTER ), BorderLayout.CENTER );
		mpPanel.add( mpMeter, BorderLayout.SOUTH );

		JPanel basicPanel = new JPanel();
		basicPanel.add( hpPanel );
		basicPanel.add( Box.createHorizontalStrut( 20 ) );
		basicPanel.add( mpPanel );

		statusLabelPanel.add( basicPanel );
		statusLabelPanel.add( Box.createVerticalStrut( 20 ) );

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
			levelLabel.setText( "Level " + KoLCharacter.getLevel() + " " + KoLCharacter.getClassName() );

			hpMeter.setMaximum( KoLCharacter.getMaximumHP() );
			hpMeter.setValue( KoLCharacter.getCurrentHP() );
			hpMeter.setString( COMMA_FORMAT.format( KoLCharacter.getCurrentHP() ) + " / " + COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );

			mpMeter.setMaximum( KoLCharacter.getMaximumMP() );
			mpMeter.setValue( KoLCharacter.getCurrentMP() );
			mpMeter.setString( COMMA_FORMAT.format( KoLCharacter.getCurrentMP() ) + " / " + COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );

			refreshValuePanel( 0, KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() );
			refreshValuePanel( 1, KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(), KoLCharacter.getMysticalityTNP() );
			refreshValuePanel( 2, KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() );

			int currentLevel = KoLCharacter.calculateLastLevel();
			int nextLevel = KoLCharacter.calculateNextLevel();
			int totalPrime = KoLCharacter.getTotalPrime();

			levelMeter.setMaximum( nextLevel - currentLevel );
			levelMeter.setValue( totalPrime - currentLevel );
			levelMeter.setString( "" );

			levelPanel.setToolTipText( "<html>&nbsp;&nbsp;" + KoLCharacter.getAdvancement() + "&nbsp;&nbsp;<br>&nbsp;&nbsp;(" +
				COMMA_FORMAT.format( nextLevel - totalPrime ) + " subpoints needed)&nbsp;&nbsp;</html>" );

			// Set the current avatar
			avatar.setIcon( JComponentUtilities.getImage( KoLCharacter.getAvatar() ) );
		}
	}
}
