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
	private String [] currentData;
	private final MushroomButton [][] currentButtons;
	private final MushroomButton [][] forecastButtons;

	public MushroomFrame( KoLmafia client )
	{
		super( client, "Mushroom Fields" );

		JPanel currentPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );
		JPanel forecastPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );

		currentButtons = new MushroomButton[4][4];
		forecastButtons = new MushroomButton[4][4];

		for ( int i = 0; i < 4; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				currentButtons[i][j] = new MushroomButton( i * 4 + j, true );
				forecastButtons[i][j] = new MushroomButton( i * 4 + j, false );

				currentPlot.add( currentButtons[i][j] );
				forecastPlot.add( forecastButtons[i][j] );
			}
		}

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new GridLayout( 1, 2, 20, 20 ) );
		centerPanel.add( constructPanel( "Current Plot", currentPlot ) );
		centerPanel.add( constructPanel( "Forecasted Plot", forecastPlot ) );

		framePanel.setLayout( new CardLayout( 40, 40 ) );
		framePanel.add( centerPanel, "" );

		plotChanged();
		setResizable( false );
	}

	public JPanel constructPanel( String label, Component c )
	{
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );
		panel.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		panel.add( new JLabel( label, JLabel.CENTER ), BorderLayout.NORTH );
		panel.add( c, BorderLayout.CENTER );

		return panel;
	}

	/*
	 * Method invoked by MushroomPlot when the field has changed
	 */

	public void plotChanged()
	{
		// Get the current state of the field and update
		(new UpdateMushroomThread()).start();
	}

	/**
	 * Special thread which allows the current page to be updated outside
	 * of the Swing thread -- this means images can be downloaded without
	 * locking the UI.
	 */

	private class UpdateMushroomThread extends DaemonThread
	{
		public void run()
		{
			synchronized( MushroomFrame.class )
			{
				currentData = MushroomPlot.getMushroomPlot( true ).split( ";" );
				refresh();
			}
		}
	}

	public void refresh()
	{
		// Convert each piece of current data into the appropriate
		// mushroom plot data.

		int [][] currentArray = new int[4][4];
		for ( int i = 0; i < 4; ++i )
			for ( int j = 0; j < 4; ++j )
				currentArray[i][j] = MushroomPlot.mushroomType( currentData[ i * 4 + j ] );

		String [] forecastData = MushroomPlot.getForecastedPlot( true, currentArray ).split( ";" );

		// What you do is you update each mushroom button based on
		// what is contained in each of the data fields.

		for ( int i = 0; i < 4; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				currentButtons[i][j].setIcon( JComponentUtilities.getSharedImage( currentData[ i * 4 + j ] ) );
				forecastButtons[i][j].setIcon( JComponentUtilities.getSharedImage( forecastData[ i * 4 + j ] ) );
			}
		}
	}

	private class MushroomButton extends JButton implements ActionListener
	{
		private int index;
		private boolean canModify;

		public MushroomButton( int index, boolean canModify )
		{
			this.index = index;
			this.canModify = canModify;

			JComponentUtilities.setComponentSize( this, 30, 30 );

			setOpaque( true );
			setBackground( Color.white );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( !canModify )
				return;

			// Sprouts transform into dirt because all you can
			// do is pick them.

			if ( currentData[ index ].endsWith( "/mushsprout.gif" ) )
			{
				currentData[ index ] = "itemimages/dirt1.gif";
				refresh();
				return;
			}

			// Second generation mushrooms transform into dirt
			// because all you can do is pick them.

			if ( currentData[ index ].endsWith( "/flatshroom.gif" ) || currentData[ index ].endsWith( "/plaidroom.gif" ) || currentData[ index ].endsWith( "/tallshroom.gif" ) )
			{
				currentData[ index ] = "itemimages/dirt1.gif";
				refresh();
				return;
			}

			// Third generation mushrooms transform into dirt
			// because all you can do is pick them.

			if ( currentData[ index ].endsWith( "/fireshroom.gif" ) || currentData[ index ].endsWith( "/iceshroom.gif" ) || currentData[ index ].endsWith( "/stinkshroo.gif" ) )
			{
				currentData[ index ] = "itemimages/dirt1.gif";
				refresh();
				return;
			}

			// Everything else rotates based on what was there
			// when you clicked on the image.

			if ( currentData[ index ].endsWith( "/dirt1.gif" ) )
			{
				currentData[ index ] = "itemimages/mushroom.gif";
				refresh();
				return;
			}

			if ( currentData[ index ].endsWith( "/mushroom.gif" ) )
			{
				currentData[ index ] = "itemimages/bmushroom.gif";
				refresh();
				return;
			}

			if ( currentData[ index ].endsWith( "/bmushroom.gif" ) )
			{
				currentData[ index ] = "itemimages/spooshroom.gif";
				refresh();
				return;
			}

			if ( currentData[ index ].endsWith( "/spooshroom.gif" ) )
			{
				currentData[ index ] = "itemimages/dirt1.gif";
				refresh();
				return;
			}
		}
	}

	public static void main( String [] args )
	{	(new CreateFrameRunnable( MushroomFrame.class )).run();
	}
}
