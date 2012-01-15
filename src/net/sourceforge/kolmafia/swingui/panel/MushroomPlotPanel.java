/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.JComponentUtilities;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.MushroomManager;

import net.sourceforge.kolmafia.swingui.button.InvocationButton;

public class MushroomPlotPanel
	extends JPanel
{
	private boolean doingLayout = false;
	private String[] currentData;
	private String[] layoutData;

	private final MushroomButton[][] currentButtons;
	private final MushroomButton[][] layoutButtons;
	private final MushroomButton[][] forecastButtons;

	public MushroomPlotPanel()
	{
		JPanel currentPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );
		JPanel layoutPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );
		JPanel forecastPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );

		this.currentButtons = new MushroomButton[ 4 ][ 4 ];
		this.layoutButtons = new MushroomButton[ 4 ][ 4 ];
		this.forecastButtons = new MushroomButton[ 4 ][ 4 ];

		for ( int i = 0; i < 4; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				this.currentButtons[ i ][ j ] = new MushroomButton( i * 4 + j, false );
				this.layoutButtons[ i ][ j ] = new MushroomButton( i * 4 + j, true );
				this.forecastButtons[ i ][ j ] = new MushroomButton( i * 4 + j, false );

				currentPlot.add( this.currentButtons[ i ][ j ] );
				layoutPlot.add( this.layoutButtons[ i ][ j ] );
				forecastPlot.add( this.forecastButtons[ i ][ j ] );
			}
		}

		JPanel centerPanel = new JPanel( new GridLayout( 1, 3, 20, 20 ) );
		centerPanel.add( this.constructPanel( "Current Plot", currentPlot ) );
		centerPanel.add( this.constructPanel( "Layout Plot", layoutPlot ) );
		centerPanel.add( this.constructPanel( "Forecasted Plot", forecastPlot ) );

		JPanel completePanel = new JPanel( new BorderLayout( 20, 20 ) );
		completePanel.add( centerPanel, BorderLayout.CENTER );

		// Dummy buttons for the mushroom plot (just for layout
		// viewing purposes.  To be replaced with real functionality
		// at a later date.

		JPanel buttonPanel = new JPanel();
		buttonPanel.add( new InvocationButton( "Harvest All", MushroomManager.class, "harvestMushrooms" ) );
		buttonPanel.add( new InvocationButton( "Do Layout", this, "executeLayout" ) );
		buttonPanel.add( new InvocationButton( "Script Layout", this, "scriptLayout" ) );
		completePanel.add( buttonPanel, BorderLayout.SOUTH );

		this.setLayout( new CardLayout( 40, 40 ) );
		this.add( completePanel, "" );

		this.plotChanged();
	}

	public void executeLayout()
	{
		// Change any mushrooms which no longer
		// match the existing plot.

		this.doingLayout = true;
		for ( int i = 0; i < 16; ++i )
		{
			if ( !this.currentData[ i ].equals( this.layoutData[ i ] ) )
			{
				MushroomManager.pickMushroom( i + 1, false );
				if ( !this.layoutData[ i ].endsWith( "/dirt1.gif" ) && !this.layoutData[ i ].endsWith( "/mushsprout.gif" ) )
				{
					MushroomManager.plantMushroom( i + 1, MushroomManager.getMushroomType( this.layoutData[ i ] ) );
				}
			}
		}

		this.doingLayout = false;
	}

	public void scriptLayout()
	{
		JFileChooser chooser = new JFileChooser( KoLConstants.SCRIPT_LOCATION );
		int returnVal = chooser.showSaveDialog( this );

		File output = chooser.getSelectedFile();

		if ( output == null )
		{
			return;
		}
		
		String outputPath = null;
		
		try
		{
			outputPath = output.getCanonicalPath();
		}
		catch ( IOException e )
		{
			return;
		}

		try
		{
			PrintStream ostream = LogStream.openStream( output, true );
			ostream.println( "field harvest" );

			for ( int i = 0; i < 16; ++i )
			{
				int mushroomType = MushroomManager.getMushroomType( this.layoutData[ i ] );
				switch ( mushroomType )
				{
				case MushroomManager.SPOOKY:
				case MushroomManager.KNOB:
				case MushroomManager.KNOLL:
					ostream.println( "field pick " + ( i + 1 ) );
					ostream.println( "field plant " + ( i + 1 ) + " " + ItemDatabase.getItemName( mushroomType ) );
					break;

				case MushroomManager.EMPTY:
					ostream.println( "field pick " + ( i + 1 ) );
					break;
				}
			}

			ostream.close();
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e, "Error saving file <" + outputPath + ">" );
		}
	}

	public JPanel constructPanel( final String label, final Component c )
	{
		JPanel panel = new JPanel( new BorderLayout() );
		panel.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );
		panel.add( new JLabel( label, SwingConstants.CENTER ), BorderLayout.NORTH );
		panel.add( c, BorderLayout.CENTER );

		return panel;
	}

	/*
	 * Method invoked by MushroomManager when the field has changed
	 */

	public void plotChanged()
	{
		// Get the layout state of the field and update

		this.currentData = MushroomManager.getMushroomManager( true ).split( ";" );

		// Only update the layout data if you're
		// not currently doing any layouts.

		if ( !this.doingLayout )
		{
			this.layoutData = MushroomManager.getMushroomManager( true ).split( ";" );
		}

		// With everything that you need updated,
		// feel free to refresh the layout.

		this.refresh();
	}

	public void refresh()
	{
		// Do nothing if you don't have a plot
		if ( this.layoutData[ 0 ].equals( "Your plot is unavailable." ) )
		{
			return;
		}

		// Convert each piece of layout data into the appropriate
		// mushroom plot data.

		String[][] layoutArray = new String[ 4 ][ 4 ];
		for ( int i = 0; i < 4; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				layoutArray[ i ][ j ] = this.layoutData[ i * 4 + j ];
			}
		}

		String[] forecastData = MushroomManager.getForecastedPlot( true, layoutArray ).split( ";" );

		// What you do is you update each mushroom button based on
		// what is contained in each of the data fields.

		for ( int i = 0; i < 4; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				this.currentButtons[ i ][ j ].setIcon( JComponentUtilities.getImage( this.currentData[ i * 4 + j ] ) );
				this.layoutButtons[ i ][ j ].setIcon( JComponentUtilities.getImage( this.layoutData[ i * 4 + j ] ) );
				this.forecastButtons[ i ][ j ].setIcon( JComponentUtilities.getImage( forecastData[ i * 4 + j ] ) );
			}
		}
	}

	private class MushroomButton
		extends JButton
		implements ActionListener
	{
		private final int index;
		private boolean canModify;

		public MushroomButton( final int index, final boolean canModify )
		{
			this.index = index;
			this.canModify = canModify;

			JComponentUtilities.setComponentSize( this, 30, 30 );

			this.setOpaque( true );
			this.setBackground( Color.white );
			this.addActionListener( this );
		}

		public void actionPerformed( final ActionEvent e )
		{
			if ( !this.canModify )
			{
				return;
			}

			// No mushroom plot
			if ( MushroomPlotPanel.this.layoutData.length == 1 )
			{
				return;
			}

			// Sprouts transform into dirt because all you can
			// do is pick them.

			if ( MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/mushsprout.gif" ) )
			{
				MushroomPlotPanel.this.layoutData[ this.index ] = "itemimages/dirt1.gif";
				MushroomPlotPanel.this.refresh();
				return;
			}

			// Second generation mushrooms transform into dirt
			// because all you can do is pick them.

			if ( MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/flatshroom.gif" ) || MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/plaidroom.gif" ) || MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/tallshroom.gif" ) )
			{
				MushroomPlotPanel.this.layoutData[ this.index ] = "itemimages/dirt1.gif";
				MushroomPlotPanel.this.refresh();
				return;
			}

			// Third generation mushrooms transform into dirt
			// because all you can do is pick them.

			if ( MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/fireshroom.gif" ) || MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/iceshroom.gif" ) || MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/stinkshroo.gif" ) )
			{
				MushroomPlotPanel.this.layoutData[ this.index ] = "itemimages/dirt1.gif";
				MushroomPlotPanel.this.refresh();
				return;
			}

			// Everything else rotates based on what was there
			// when you clicked on the image.

			if ( MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/dirt1.gif" ) )
			{
				MushroomPlotPanel.this.layoutData[ this.index ] = "itemimages/mushroom.gif";
				MushroomPlotPanel.this.refresh();
				return;
			}

			if ( MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/mushroom.gif" ) )
			{
				MushroomPlotPanel.this.layoutData[ this.index ] = "itemimages/bmushroom.gif";
				MushroomPlotPanel.this.refresh();
				return;
			}

			if ( MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/bmushroom.gif" ) )
			{
				MushroomPlotPanel.this.layoutData[ this.index ] = "itemimages/spooshroom.gif";
				MushroomPlotPanel.this.refresh();
				return;
			}

			if ( MushroomPlotPanel.this.layoutData[ this.index ].endsWith( "/spooshroom.gif" ) )
			{
				MushroomPlotPanel.this.layoutData[ this.index ] = MushroomPlotPanel.this.currentData[ this.index ];
				MushroomPlotPanel.this.refresh();
				return;
			}
		}
	}
}

