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
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

// event listeners
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;

// utilities
import java.net.URL;
import java.net.MalformedURLException;
import java.util.StringTokenizer;
import net.java.dev.spellcast.utilities.JComponentUtilities;

/**
 * An extension of <code>KoLFrame</code> used to display the character
 * sheet for the current user.  Note that this can only be instantiated
 * when the character is logged in; if the character has logged out,
 * this method will contain blank data.  Note also that the avatar that
 * is currently displayed will be the default avatar from the class and
 * will not reflect outfits or customizations.
 */

public class CharsheetFrame extends KoLFrame
{
	private KoLCharacter characterData;

	private JLabel [] statusLabel;
	private JPanel [] statpointPanel;
	private JButton refreshButton;

	/**
	 * Constructs a new character sheet, using the data located
	 * in the provided session.
	 *
	 * @param	client	The client containing the data associated with the character
	 */

	public CharsheetFrame( KoLmafia client )
	{
		super( "KoLmafia: " + ((client == null) ? "UI Test" : client.getLoginName()) +
			" (Character Sheet)", client );

		// For now, because character listeners haven't been implemented
		// yet, re-request the character sheet from the server

		setResizable( false );
		contentPanel = null;

		CardLayout cards = new CardLayout( 10, 10 );
		getContentPane().setLayout( cards );

		JPanel entirePanel = new JPanel();
		entirePanel.setLayout( new BorderLayout( 20, 20 ) );

		entirePanel.add( createStatusPanel(), BorderLayout.CENTER );
		entirePanel.add( createImagePanel(), BorderLayout.WEST );

		getContentPane().add( entirePanel, "" );
		addWindowListener( new ReturnFocusAdapter() );
		setDefaultCloseOperation( HIDE_ON_CLOSE );
	}

	public void setEnabled( boolean isEnabled )
	{
		super.setEnabled( isEnabled );
		if ( refreshButton != null )
			refreshButton.setEnabled( isEnabled );
	}

	/**
	 * Utility method used for creating a panel displaying the character's avatar.
	 * Because image retrieval has not been implemented, this method displays
	 * only the default avatar for the character's class.
	 *
	 * @return	a <code>JPanel</code> displaying the class-specific avatar
	 */

	private JPanel createImagePanel()
	{
		JPanel imagePanel = new JPanel();
		imagePanel.setLayout( new BorderLayout( 10, 10 ) );

		JPanel namePanel = new JPanel();
		namePanel.setLayout( new GridLayout( 2, 1 ) );
		namePanel.add( new JLabel( characterData.getUsername() + " (#" + characterData.getUserID() + ")", JLabel.CENTER ) );
		namePanel.add( new JLabel( "Level " + characterData.getLevel() + " " + characterData.getClassName(), JLabel.CENTER ) );

		imagePanel.add( namePanel, BorderLayout.NORTH );

		StringTokenizer parsedName = new StringTokenizer( characterData.getClassType() );
		StringBuffer imagename = new StringBuffer();
		while ( parsedName.hasMoreTokens() )
			imagename.append( parsedName.nextToken().toLowerCase() );

		imagePanel.add( new JLabel( JComponentUtilities.getSharedImage( imagename.toString() + ".gif" ) ), BorderLayout.CENTER );

		this.refreshButton = new JButton( "Refresh Status" );
		refreshButton.addActionListener( new StatusRefreshListener() );
		imagePanel.add( refreshButton, BorderLayout.SOUTH );
		return imagePanel;
	}

	/**
	 * Utility method for creating a panel that displays the given statusLabel,
	 * using formatting if the values are different.
	 */

	private JPanel createValuePanel( int displayIndex )
	{
		statpointPanel[ displayIndex ] = new JPanel();
		statpointPanel[ displayIndex ].setLayout( new BorderLayout( 0, 0 ) );

		int index1 = ((displayIndex + 1) << 1);
		int index2 = index1 + 1;

		statusLabel[ index1 ] = new JLabel( "", JLabel.LEFT );
		statusLabel[ index1 ].setForeground( Color.BLUE );
		statpointPanel[ displayIndex ].add( statusLabel[ index1 ], BorderLayout.WEST );

		statusLabel[ index2 ] = new JLabel( "", JLabel.LEFT );
		statpointPanel[ displayIndex ].add( statusLabel[ index2 ], BorderLayout.CENTER );

		return statpointPanel[ displayIndex ];
	}

	/**
	 * Utility method for modifying a panel that displays the given statusLabel,
	 * using formatting if the values are different.
	 */

	private void refreshValuePanel( int displayIndex, int baseValue, int adjustedValue, int tillNextPoint )
	{
		int index1 = ((displayIndex + 1) << 1);
		int index2 = index1 + 1;

		JLabel adjustedLabel = statusLabel[index1];
		JLabel baseLabel = statusLabel[index2];

		if ( baseValue != adjustedValue )
		{
			adjustedLabel.setText( "" + adjustedValue );
			baseLabel.setText( " (" + baseValue + ")" );
		}
		else
		{
			adjustedLabel.setText( "" );
			baseLabel.setText( "" + baseValue );
		}

		statpointPanel[ displayIndex ].setToolTipText( "" + tillNextPoint + " until " + (baseValue + 1) );
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

		statusLabelPanel.add( new JLabel( " " ) );

		this.statusLabel = new JLabel[11];
		for ( int i = 0; i < 11; ++i )
		{
			statusLabel[i] = new JLabel( "", JLabel.CENTER );
			if ( i == 1 )  i = 7;
		}

		statusLabelPanel.add( statusLabel[0], "");
		statusLabelPanel.add( statusLabel[1], "" );
		statusLabelPanel.add( new JLabel( " " ) );

		JPanel primeStatLabels = new JPanel();
		primeStatLabels.setLayout( new GridLayout( 3, 1 ) );
		primeStatLabels.add( new JLabel( "Mus: ", JLabel.RIGHT ) );
		primeStatLabels.add( new JLabel( "Mys: ", JLabel.RIGHT ) );
		primeStatLabels.add( new JLabel( "Mox: ", JLabel.RIGHT ) );

		this.statpointPanel = new JPanel[3];
		JPanel primeStatValues = new JPanel();
		primeStatValues.setLayout( new GridLayout( 3, 1 ) );
		primeStatValues.add( createValuePanel( 0 ) );
		primeStatValues.add( createValuePanel( 1 ) );
		primeStatValues.add( createValuePanel( 2 ) );

		JPanel primeStatPanel = new JPanel();
		primeStatPanel.setLayout( new BoxLayout( primeStatPanel, BoxLayout.X_AXIS ) );
		primeStatPanel.add( primeStatLabels, BorderLayout.WEST );
		primeStatPanel.add( primeStatValues, BorderLayout.CENTER );
		statusLabelPanel.add( primeStatPanel, "" );

		statusLabelPanel.add( new JLabel( " " ), "" );
		statusLabelPanel.add( statusLabel[8], "" );
		statusLabelPanel.add( statusLabel[9], "" );
		statusLabelPanel.add( statusLabel[10], "" );

		statusLabelPanel.add( new JLabel( " " ), "" );
		refreshStatus();

		return statusLabelPanel;
	}

	/**
	 * Utility method used to refresh the status of the pane.  This
	 * method is made public so that the same frame can be hidden and
	 * reused at a later time.
	 */

	public void refreshStatus()
	{
		this.setEnabled( false );
		if ( client != null )
		{
			characterData = client.getCharacterData();
			(new CharsheetRequest( client )).run();
			client.applyRecentEffects();
		}
		else
			characterData = new KoLCharacter( "UI Test" );

		statusLabel[0].setText( characterData.getCurrentHP() + " / " + characterData.getMaximumHP() + " (HP)" );
		statusLabel[1].setText( characterData.getCurrentMP() + " / " + characterData.getMaximumMP() + " (MP)" );

		refreshValuePanel( 0, characterData.getBaseMuscle(), characterData.getAdjustedMuscle(), characterData.getMuscleTNP() );
		refreshValuePanel( 1, characterData.getBaseMysticality(), characterData.getAdjustedMysticality(), characterData.getMysticalityTNP() );
		refreshValuePanel( 2, characterData.getBaseMoxie(), characterData.getAdjustedMoxie(), characterData.getMoxieTNP() );

		statusLabel[8].setText( characterData.getAvailableMeat() + " meat" );
		statusLabel[9].setText( characterData.getInebriety() + " drunkenness" );
		statusLabel[10].setText( characterData.getAdventuresLeft() + " adventures left" );

		client.updateDisplay( ENABLED_STATE, " " );
		this.setEnabled( true );
	}

	private class StatusRefreshListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	(new StatusRefreshThread()).start();
		}

		private class StatusRefreshThread extends Thread
		{
			public StatusRefreshThread()
			{
				super( "Status-Refresh-Thread" );
				setDaemon( true );
			}

			public void run()
			{	refreshStatus();
			}
		}
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{
		KoLFrame uitest = new CharsheetFrame( null );
		uitest.pack();  uitest.setVisible( true );  uitest.requestFocus();
	}
}
