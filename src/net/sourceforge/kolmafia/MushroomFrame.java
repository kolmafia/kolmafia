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

import java.awt.Color;
import java.awt.Component;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class MushroomFrame extends KoLFrame
{
	public static final int MAX_FORECAST = 11;

	private final JLabel [] headers;
	private final String [][] planningData;
	private final String [][] originalData;

	private final MushroomButton [][][] planningButtons;

	public MushroomFrame()
	{
		super( "Mushroom Helper" );

		headers = new JLabel[MAX_FORECAST];
		planningData = new String[MAX_FORECAST][16];
		originalData = new String[MAX_FORECAST][16];

		for ( int i = 0; i < MAX_FORECAST; ++i )
		{
			for ( int j = 0; j < 16; ++j )
			{
				planningData[i][j] = "__";
				originalData[i][j] = "__";
			}
		}

		planningButtons = new MushroomButton[MAX_FORECAST][4][4];
		JPanel centerPanel = new JPanel( new GridLayout( 3, 4, 20, 20 ) );

		for ( int i = 0; i < MAX_FORECAST; ++i )
		{
			JPanel currentPlot = new JPanel( new GridLayout( 4, 4, 0, 2 ) );
			for ( int j = 0; j < 4; ++j )
			{
				for ( int k = 0; k < 4; ++k )
				{
					planningButtons[i][j][k] = new MushroomButton( i, j * 4 + k );
					currentPlot.add( planningButtons[i][j][k] );
				}
			}

			centerPanel.add( constructPanel( i, currentPlot ) );
		}

		// Dummy buttons for the mushroom plot (just for layout
		// viewing purposes.  To be replaced with real functionality
		// at a later date.

		JPanel buttonPanel = new JPanel( new GridLayout( 2, 1, 12, 12 ) );

		// Now add the various action buttons.

		buttonPanel.add( new InvocationButton( "Load Layout", this, "loadLayout" ) );
		buttonPanel.add( new InvocationButton( "Save Layout", this, "saveLayout" ) );
		centerPanel.add( buttonPanel );

		framePanel.setLayout( new CardLayout( 40, 40 ) );
		framePanel.add( centerPanel, "" );

		updateForecasts( 1 );
		setResizable( false );
	}

	public void loadLayout()
	{
	}

	public void saveLayout()
	{	MushroomPlot.saveLayout( "mushroom", originalData, planningData );
	}

	public void updateForecasts( int startDay )
	{
		for ( int i = startDay; i < MAX_FORECAST; ++i )
		{
			String [][] holdingData = new String[4][4];
			for ( int j = 0; j < 4; ++j )
				for ( int k = 0; k < 4; ++k )
					holdingData[j][k] = planningData[ i - 1 ][ j * 4 + k ];

			String [] forecastData = MushroomPlot.getForecastedPlot( true, holdingData ).split( ";" );
			for ( int j = 0; j < 16; ++j )
			{
				planningData[i][j] = forecastData[j];
				originalData[i][j] = forecastData[j];
			}
		}

		for ( int i = 0; i < MAX_FORECAST; ++i )
			for ( int j = 0; j < 4; ++j )
				for ( int k = 0; k < 4; ++k )
					planningButtons[i][j][k].updateImage();
	}

	public JPanel constructPanel( int dayIndex, Component c )
	{
		JPanel panel = new JPanel( new BorderLayout() );
		panel.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );

		headers[dayIndex] = new JLabel( "Day " + (dayIndex + 1), JLabel.CENTER );

		panel.add( headers[dayIndex], BorderLayout.NORTH );
		panel.add( c, BorderLayout.CENTER );

		return panel;
	}

	private class MushroomButton extends JButton implements ActionListener
	{
		private int dayIndex;
		private int loopIndex;
		private int squareIndex;

		public MushroomButton( int dayIndex, int squareIndex )
		{
			this.dayIndex = dayIndex;
			this.loopIndex = 4;
			this.squareIndex = squareIndex;

			JComponentUtilities.setComponentSize( this, 30, 30 );

			setOpaque( true );
			setBackground( Color.white );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			planningData[ dayIndex ][ squareIndex ] = toggleMushroom();
			updateForecasts( dayIndex + 1 );
		}

		public void updateImage()
		{
			String currentMushroom = planningData[ dayIndex ][ squareIndex ];

			if ( currentMushroom.equals( "__" ) )
				setIcon( JComponentUtilities.getImage( "itemimages/dirt1.gif" ) );
			else if ( currentMushroom.equals( currentMushroom.toLowerCase() ) )
				setIcon( JComponentUtilities.getImage( "itemimages/mushsprout.gif" ) );
			else
				setIcon( JComponentUtilities.getImage( MushroomPlot.getMushroomImage( currentMushroom ) ) );
		}

		private String toggleMushroom()
		{
			// Everything rotates based on what was there
			// when you clicked on the image.

			loopIndex = (loopIndex + 1) % 5;

			switch ( loopIndex )
			{
				// If you loop around, then test to see if the
				// old data was a blank.  If it was, then you
				// have already displayed it, so move on to the
				// next element in the cycle.  If not, return a
				// blank, as that's the next element in the cycle.

				case 0:

					if ( originalData[ dayIndex ][ squareIndex ].equals( "__" ) )
						loopIndex = 1;
					else
						return "__";

				// In all other cases, return the next element
				// in the mushroom toggle cycle.

				case 1:  return "kb";
				case 2:  return "kn";
				case 3:  return "sp";
				case 4:  return originalData[ dayIndex ][ squareIndex ];
			}

			return "__";
		}
	}
}
