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
import javax.swing.JFileChooser;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class MushroomFrame extends KoLFrame
{
	private boolean doingLayout = false;
	private String [] currentData;
	private String [] layoutData;

	private final MushroomButton [][] currentButtons;
	private final MushroomButton [][] layoutButtons;
	private final MushroomButton [][] forecastButtons;

	public MushroomFrame()
	{
		super( "Mushroom Plot" );

		JPanel currentPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );
		JPanel layoutPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );
		JPanel forecastPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );

		currentButtons = new MushroomButton[4][4];
		layoutButtons = new MushroomButton[4][4];
		forecastButtons = new MushroomButton[4][4];

		for ( int i = 0; i < 4; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				currentButtons[i][j] = new MushroomButton( i * 4 + j, false );
				layoutButtons[i][j] = new MushroomButton( i * 4 + j, true );
				forecastButtons[i][j] = new MushroomButton( i * 4 + j, false );

				currentPlot.add( currentButtons[i][j] );
				layoutPlot.add( layoutButtons[i][j] );
				forecastPlot.add( forecastButtons[i][j] );
			}
		}

		JPanel centerPanel = new JPanel( new GridLayout( 1, 3, 20, 20 ) );
		centerPanel.add( constructPanel( "Current Plot", currentPlot ) );
		centerPanel.add( constructPanel( "Layout Plot", layoutPlot ) );
		centerPanel.add( constructPanel( "Forecasted Plot", forecastPlot ) );

		JPanel completePanel = new JPanel( new BorderLayout( 20, 20 ) );
		completePanel.add( centerPanel, BorderLayout.CENTER );

		// Dummy buttons for the mushroom plot (just for layout
		// viewing purposes.  To be replaced with real functionality
		// at a later date.

		JPanel buttonPanel = new JPanel();
		buttonPanel.add( new InvocationButton( "Harvest All", MushroomPlot.class, "harvestMushrooms" ) );
		buttonPanel.add( new InvocationButton( "Do Layout", this, "executeLayout" ) );
		buttonPanel.add( new InvocationButton( "Script Layout", this, "scriptLayout" ) );
		completePanel.add( buttonPanel, BorderLayout.SOUTH );

		framePanel.setLayout( new CardLayout( 40, 40 ) );
		framePanel.add( completePanel, "" );

		plotChanged();
		setResizable( false );
	}

	public void executeLayout()
	{
		// Change any mushrooms which no longer
		// match the existing plot.

		doingLayout = true;
		for ( int i = 0; i < 16; ++i )
		{
			if ( !currentData[i].equals( layoutData[i] ) )
			{
				MushroomPlot.pickMushroom( i + 1, false );
				if ( !layoutData[i].endsWith( "/dirt1.gif" ) && !layoutData[i].endsWith( "/mushsprout.gif" ) )
					MushroomPlot.plantMushroom( i + 1, MushroomPlot.mushroomType( layoutData[i] ) );
			}
		}

		doingLayout = false;
	}

	public void scriptLayout()
	{
		JFileChooser chooser = new JFileChooser( "scripts" );
		int returnVal = chooser.showSaveDialog( this );

		File output = chooser.getSelectedFile();

		if ( output == null )
			return;

		try
		{
			PrintWriter ostream = new PrintWriter( new FileOutputStream( output, false ), false );
			ostream.println( "field harvest" );

			for ( int i = 0; i < 16; ++i )
			{
				int mushroomType = MushroomPlot.mushroomType( layoutData[i] );
				switch ( mushroomType )
				{
					case MushroomPlot.SPOOKY:
					case MushroomPlot.KNOB:
					case MushroomPlot.KNOLL:
						ostream.println( "field pick " + (i + 1) );
						ostream.println( "field plant " + (i + 1) + " " + TradeableItemDatabase.getItemName( mushroomType ) );
						break;

					case MushroomPlot.EMPTY:
						ostream.println( "field pick " + (i + 1) );
						break;
				}
			}

			ostream.close();
		}
		catch ( Exception ex )
		{
			ex.printStackTrace( KoLmafia.getLogStream() );
			ex.printStackTrace();
		}
	}

	public JPanel constructPanel( String label, Component c )
	{
		JPanel panel = new JPanel( new BorderLayout() );
		panel.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		panel.add( new JLabel( label, JLabel.CENTER ), BorderLayout.NORTH );
		panel.add( c, BorderLayout.CENTER );

		return panel;
	}

	/*
	 * Method invoked by MushroomPlot when the field has changed
	 */

	public void plotChanged()
	{	(new RequestThread( new PlotChanger() )).start();
	}

	private class PlotChanger implements Runnable
	{
		public void run()
		{
			// Get the layout state of the field and update

			currentData = MushroomPlot.getMushroomPlot( true ).split( ";" );

			// Only update the layout data if you're
			// not currently doing any layouts.

			if ( !doingLayout )
				layoutData = MushroomPlot.getMushroomPlot( true ).split( ";" );

			// With everything that you need updated,
			// feel free to refresh the layout.

			refresh();
		}
	}

	public void refresh()
	{
		// Do nothing if you don't have a plot
		if ( layoutData[0].equals( "Your plot is unavailable." ) )
			return;

		// Convert each piece of layout data into the appropriate
		// mushroom plot data.

		int [][] layoutArray = new int[4][4];
		for ( int i = 0; i < 4; ++i )
			for ( int j = 0; j < 4; ++j )
				layoutArray[i][j] = MushroomPlot.mushroomType( layoutData[ i * 4 + j ] );

		String [] forecastData = MushroomPlot.getForecastedPlot( true, layoutArray ).split( ";" );

		// What you do is you update each mushroom button based on
		// what is contained in each of the data fields.

		for ( int i = 0; i < 4; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				currentButtons[i][j].setIcon( JComponentUtilities.getSharedImage( currentData[ i * 4 + j ] ) );
				layoutButtons[i][j].setIcon( JComponentUtilities.getSharedImage( layoutData[ i * 4 + j ] ) );
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

			// No mushroom plot
			if ( layoutData.length == 1 )
				return;

			// Sprouts transform into dirt because all you can
			// do is pick them.

			if ( layoutData[ index ].endsWith( "/mushsprout.gif" ) )
			{
				layoutData[ index ] = "itemimages/dirt1.gif";
				refresh();
				return;
			}

			// Second generation mushrooms transform into dirt
			// because all you can do is pick them.

			if ( layoutData[ index ].endsWith( "/flatshroom.gif" ) || layoutData[ index ].endsWith( "/plaidroom.gif" ) || layoutData[ index ].endsWith( "/tallshroom.gif" ) )
			{
				layoutData[ index ] = "itemimages/dirt1.gif";
				refresh();
				return;
			}

			// Third generation mushrooms transform into dirt
			// because all you can do is pick them.

			if ( layoutData[ index ].endsWith( "/fireshroom.gif" ) || layoutData[ index ].endsWith( "/iceshroom.gif" ) || layoutData[ index ].endsWith( "/stinkshroo.gif" ) )
			{
				layoutData[ index ] = "itemimages/dirt1.gif";
				refresh();
				return;
			}

			// Everything else rotates based on what was there
			// when you clicked on the image.

			if ( layoutData[ index ].endsWith( "/dirt1.gif" ) )
			{
				layoutData[ index ] = "itemimages/mushroom.gif";
				refresh();
				return;
			}

			if ( layoutData[ index ].endsWith( "/mushroom.gif" ) )
			{
				layoutData[ index ] = "itemimages/bmushroom.gif";
				refresh();
				return;
			}

			if ( layoutData[ index ].endsWith( "/bmushroom.gif" ) )
			{
				layoutData[ index ] = "itemimages/spooshroom.gif";
				refresh();
				return;
			}

			if ( layoutData[ index ].endsWith( "/spooshroom.gif" ) )
			{
				layoutData[ index ] = currentData[ index ];
				refresh();
				return;
			}
		}
	}
}
