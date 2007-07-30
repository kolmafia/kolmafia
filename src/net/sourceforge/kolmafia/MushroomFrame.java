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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;

import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class MushroomFrame extends KoLFrame
{
	public static final int MAX_FORECAST = 11;

	private static final Color TODAY_COLOR = new Color( 192, 255, 192 );
	private static final Color OTHER_COLOR = new Color( 240, 240, 240 );

	public MushroomFrame()
	{
		super( "Mushroom Plot" );

		JPanel plantPanel = new JPanel();
		plantPanel.add( new ImmediatePlotPanel() );

		this.tabs.addTab( "One Day Planting", plantPanel );

		JPanel planPanel = new JPanel();
		planPanel.add( new ScriptGeneratePanel() );

		this.tabs.addTab( "Script Generator", planPanel );

		this.framePanel.setLayout( new CardLayout( 10, 10 ) );
		this.framePanel.add( this.tabs, "" );
	}

	private class ImmediatePlotPanel extends JPanel
	{
		private boolean doingLayout = false;
		private String [] currentData;
		private String [] layoutData;

		private final MushroomButton [][] currentButtons;
		private final MushroomButton [][] layoutButtons;
		private final MushroomButton [][] forecastButtons;

		public ImmediatePlotPanel()
		{
			JPanel currentPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );
			JPanel layoutPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );
			JPanel forecastPlot = new JPanel( new GridLayout( 4, 4, 0, 0 ) );

			this.currentButtons = new MushroomButton[4][4];
			this.layoutButtons = new MushroomButton[4][4];
			this.forecastButtons = new MushroomButton[4][4];

			for ( int i = 0; i < 4; ++i )
			{
				for ( int j = 0; j < 4; ++j )
				{
					this.currentButtons[i][j] = new MushroomButton( i * 4 + j, false );
					this.layoutButtons[i][j] = new MushroomButton( i * 4 + j, true );
					this.forecastButtons[i][j] = new MushroomButton( i * 4 + j, false );

					currentPlot.add( this.currentButtons[i][j] );
					layoutPlot.add( this.layoutButtons[i][j] );
					forecastPlot.add( this.forecastButtons[i][j] );
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
			buttonPanel.add( new InvocationButton( "Harvest All", MushroomPlot.class, "harvestMushrooms" ) );
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
				if ( !this.currentData[i].equals( this.layoutData[i] ) )
				{
					MushroomPlot.pickMushroom( i + 1, false );
					if ( !this.layoutData[i].endsWith( "/dirt1.gif" ) && !this.layoutData[i].endsWith( "/mushsprout.gif" ) )
						MushroomPlot.plantMushroom( i + 1, MushroomPlot.getMushroomType( this.layoutData[i] ) );
				}
			}

			this.doingLayout = false;
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
				LogStream ostream = LogStream.openStream( output, true );
				ostream.println( "field harvest" );

				for ( int i = 0; i < 16; ++i )
				{
					int mushroomType = MushroomPlot.getMushroomType( this.layoutData[i] );
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
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.

				StaticEntity.printStackTrace( e, "Error saving file <" + output.getAbsolutePath() + ">" );
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
		{	RequestThread.postRequest( new PlotChanger() );
		}

		private class PlotChanger implements Runnable
		{
			public void run()
			{
				// Get the layout state of the field and update

				ImmediatePlotPanel.this.currentData = MushroomPlot.getMushroomPlot( true ).split( ";" );

				// Only update the layout data if you're
				// not currently doing any layouts.

				if ( !ImmediatePlotPanel.this.doingLayout )
					ImmediatePlotPanel.this.layoutData = MushroomPlot.getMushroomPlot( true ).split( ";" );

				// With everything that you need updated,
				// feel free to refresh the layout.

				ImmediatePlotPanel.this.refresh();
			}
		}

		public void refresh()
		{
			// Do nothing if you don't have a plot
			if ( this.layoutData[0].equals( "Your plot is unavailable." ) )
				return;

			// Convert each piece of layout data into the appropriate
			// mushroom plot data.

			String [][] layoutArray = new String[4][4];
			for ( int i = 0; i < 4; ++i )
				for ( int j = 0; j < 4; ++j )
					layoutArray[i][j] = this.layoutData[ i * 4 + j ];

			String [] forecastData = MushroomPlot.getForecastedPlot( true, layoutArray ).split( ";" );

			// What you do is you update each mushroom button based on
			// what is contained in each of the data fields.

			for ( int i = 0; i < 4; ++i )
			{
				for ( int j = 0; j < 4; ++j )
				{
					this.currentButtons[i][j].setIcon( JComponentUtilities.getImage( this.currentData[ i * 4 + j ] ) );
					this.layoutButtons[i][j].setIcon( JComponentUtilities.getImage( this.layoutData[ i * 4 + j ] ) );
					this.forecastButtons[i][j].setIcon( JComponentUtilities.getImage( forecastData[ i * 4 + j ] ) );
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

				this.setOpaque( true );
				this.setBackground( Color.white );
				this.addActionListener( this );
			}

			public void actionPerformed( ActionEvent e )
			{
				if ( !this.canModify )
					return;

				// No mushroom plot
				if ( ImmediatePlotPanel.this.layoutData.length == 1 )
					return;

				// Sprouts transform into dirt because all you can
				// do is pick them.

				if ( ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/mushsprout.gif" ) )
				{
					ImmediatePlotPanel.this.layoutData[ this.index ] = "itemimages/dirt1.gif";
					ImmediatePlotPanel.this.refresh();
					return;
				}

				// Second generation mushrooms transform into dirt
				// because all you can do is pick them.

				if ( ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/flatshroom.gif" ) || ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/plaidroom.gif" ) || ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/tallshroom.gif" ) )
				{
					ImmediatePlotPanel.this.layoutData[ this.index ] = "itemimages/dirt1.gif";
					ImmediatePlotPanel.this.refresh();
					return;
				}

				// Third generation mushrooms transform into dirt
				// because all you can do is pick them.

				if ( ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/fireshroom.gif" ) || ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/iceshroom.gif" ) || ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/stinkshroo.gif" ) )
				{
					ImmediatePlotPanel.this.layoutData[ this.index ] = "itemimages/dirt1.gif";
					ImmediatePlotPanel.this.refresh();
					return;
				}

				// Everything else rotates based on what was there
				// when you clicked on the image.

				if ( ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/dirt1.gif" ) )
				{
					ImmediatePlotPanel.this.layoutData[ this.index ] = "itemimages/mushroom.gif";
					ImmediatePlotPanel.this.refresh();
					return;
				}

				if ( ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/mushroom.gif" ) )
				{
					ImmediatePlotPanel.this.layoutData[ this.index ] = "itemimages/bmushroom.gif";
					ImmediatePlotPanel.this.refresh();
					return;
				}

				if ( ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/bmushroom.gif" ) )
				{
					ImmediatePlotPanel.this.layoutData[ this.index ] = "itemimages/spooshroom.gif";
					ImmediatePlotPanel.this.refresh();
					return;
				}

				if ( ImmediatePlotPanel.this.layoutData[ this.index ].endsWith( "/spooshroom.gif" ) )
				{
					ImmediatePlotPanel.this.layoutData[ this.index ] = ImmediatePlotPanel.this.currentData[ this.index ];
					ImmediatePlotPanel.this.refresh();
					return;
				}
			}
		}
	}

	private class ScriptGeneratePanel extends JPanel
	{
		private String currentLayout = "";

		private JPanel centerPanel;
		private int currentForecast = 2;
		private JButton addToLayoutButton, deleteFromLayoutButton;

		private String [][] planningData;
		private String [][] originalData;

		private JLabel [] headers;
		private JPanel [] planningPanels;
		private MushroomButton [][][] planningButtons;

		public ScriptGeneratePanel()
		{
			this.headers = new JLabel[MAX_FORECAST + 1];

			this.planningData = new String[MAX_FORECAST + 1][16];
			this.originalData = new String[MAX_FORECAST + 1][16];

			for ( int i = 0; i < MAX_FORECAST; ++i )
			{
				for ( int j = 0; j < 16; ++j )
				{
					this.planningData[i][j] = "__";
					this.originalData[i][j] = "__";
				}
			}

			this.centerPanel = new JPanel( new GridLayout( 0, 4, 20, 20 ) );

			// Now add the first panel to the layout so that the person
			// can add more panels as they are needed.

			this.planningPanels = new JPanel[MAX_FORECAST + 1];
			this.planningButtons = new MushroomButton[MAX_FORECAST + 1][4][4];

			for ( int i = 0; i < MAX_FORECAST; ++i )
			{
				this.planningPanels[i] = new JPanel( new GridLayout( 4, 4, 0, 2 ) );
				for ( int j = 0; j < 4; ++j )
				{
					for ( int k = 0; k < 4; ++k )
					{
						this.planningButtons[i][j][k] = new MushroomButton( i, j * 4 + k );
						this.planningPanels[i].add( this.planningButtons[i][j][k] );
					}
				}
			}

			this.centerPanel.add( this.constructPanel( 0, this.planningPanels[0] ) );
			this.centerPanel.add( this.constructPanel( 1, this.planningPanels[1] ) );

			// Dummy buttons for the mushroom plot (just for layout
			// viewing purposes.  To be replaced with real functionality
			// at a later date.

			JPanel buttonPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );

			// Now add the various action buttons.

			this.addToLayoutButton = new InvocationButton( "Add a Day", this, "addToLayout" );
			this.deleteFromLayoutButton = new InvocationButton( "Delete a Day", this, "removeFromLayout" );
			this.deleteFromLayoutButton.setEnabled( false );

			buttonPanel.add( this.addToLayoutButton );
			buttonPanel.add( this.deleteFromLayoutButton );
			buttonPanel.add( new InvocationButton( "Run Layout", this, "runLayout" ) );
			buttonPanel.add( new InvocationButton( "Load Layout", this, "loadLayout" ) );
			buttonPanel.add( new InvocationButton( "Save Layout", this, "saveLayout" ) );
			this.centerPanel.add( buttonPanel );

			this.setLayout( new CardLayout( 40, 40 ) );
			this.add( this.centerPanel, "" );

			this.enableLayout();
			this.currentLayout = StaticEntity.getProperty( "plantingScript" );
			this.initializeLayout();
		}

		private void enableLayout()
		{
			for ( int i = 0; i < this.currentForecast; ++i )
				this.headers[i].setText( "Day " + (i+1) );

			this.headers[ this.currentForecast - 1 ].setText( "Final Day" );

			for ( int i = 0; i < 16; ++i )
			{
				this.planningData[ this.currentForecast ][i] = "__";
				this.originalData[ this.currentForecast ][i] = "__";
			}

			this.updateForecasts( this.currentForecast - 1 );

			this.centerPanel.validate();
			this.centerPanel.repaint();
			MushroomFrame.this.pack();

			this.addToLayoutButton.setEnabled( this.currentForecast != MAX_FORECAST );
			this.deleteFromLayoutButton.setEnabled( this.currentForecast != 2 );
		}

		public void addToLayout()
		{
			this.centerPanel.invalidate();
			this.centerPanel.add( this.constructPanel( this.currentForecast, this.planningPanels[this.currentForecast] ),
				(this.currentForecast < 3) ? this.currentForecast : (this.currentForecast + 1) );

			++this.currentForecast;
			this.enableLayout();
		}

		public void removeFromLayout()
		{
			this.centerPanel.invalidate();
			this.centerPanel.remove( this.currentForecast < 4 ? this.currentForecast - 1 : this.currentForecast );

			--this.currentForecast;
			this.enableLayout();
		}

		public void initializeLayout()
		{
			int plantingLength = 2;
			int indexToHighlight = 0;

			if ( this.currentLayout.equals( "" ) )
			{
				StaticEntity.setProperty( "plantingDay", "-1" );
				StaticEntity.setProperty( "plantingDate", "" );
				StaticEntity.setProperty( "plantingLength", "0" );
			}
			else
			{
				plantingLength = MushroomPlot.loadLayout( this.currentLayout, this.originalData, this.planningData );
				indexToHighlight = StaticEntity.getIntegerProperty( "plantingDay" );
			}

			if ( plantingLength > this.currentForecast )
			{
				this.centerPanel.invalidate();
				for ( int i = this.currentForecast; i < plantingLength; ++i )
					this.centerPanel.add( this.constructPanel( i, this.planningPanels[ i ] ), (i < 3) ? i : (i+1) );

				this.currentForecast = plantingLength;
				this.enableLayout();
			}
			else if ( plantingLength > 1 )
			{
				this.centerPanel.invalidate();
				for ( int i = this.currentForecast; i > plantingLength; --i )
					this.centerPanel.remove( i < 4 ? i - 1 : i );

				this.currentForecast = plantingLength;
				this.enableLayout();
			}


			String today = DATED_FILENAME_FORMAT.format( new Date() );

			if ( !StaticEntity.getProperty( "plantingDate" ).equals( today ) )
				++indexToHighlight;

			for ( int i = 0; i < this.currentForecast; ++i )
				this.headers[i].setBackground( i == indexToHighlight ? TODAY_COLOR : OTHER_COLOR );

			this.updateImages();
		}

		public void runLayout()
		{
			if ( this.currentLayout.equals( "" ) )
				this.saveLayout();

			if ( !this.currentLayout.equals( "" ) )
				DEFAULT_SHELL.executeLine( "call " + PLOTS_DIRECTORY + this.currentLayout + ".ash" );
		}

		public void loadLayout()
		{
			if ( !PLOTS_LOCATION.exists() )
				return;

			File [] layouts = PLOTS_LOCATION.listFiles();
			ArrayList names = new ArrayList();

			for ( int i = 0; i < layouts.length; ++i )
			{
				String name = layouts[i].getName();
				if ( name.endsWith( ".txt" ) )
				{
					name = name.substring( 0, name.length() - 4 );
					if ( !names.contains( name ) )
						names.add( name );
				}
			}

			if ( names.isEmpty() )
				return;

			String layout = (String) input( "Which mushroom plot?", names.toArray() );
			if ( layout != null )
				this.loadLayout( layout );
		}

		public void loadLayout( String layout )
		{
			if ( layout == null || layout.equals( "" ) || this.currentLayout.equals( layout ) )
				return;

			this.currentLayout = layout;
			this.initializeLayout();
		}

		public void saveLayout()
		{
			String location = input( "Name your mushroom plot!" );
			if ( location == null )
				return;

			this.currentLayout = location;

			String [] planned = new String[16];

			for ( int i = 0; i < 16; ++i )
			{
				planned[i] = this.planningData[ this.currentForecast - 1 ][i];
				this.planningData[ this.currentForecast - 1 ][i] = "__";
			}

			MushroomPlot.saveLayout( location, this.originalData, this.planningData );
			for ( int i = 0; i < 16; ++i )
				this.planningData[ this.currentForecast - 1 ][i] = planned[i];
		}

		public void updateForecasts( int startDay )
		{
			for ( int i = startDay; i < MAX_FORECAST; ++i )
			{
				String [][] holdingData = new String[4][4];
				for ( int j = 0; j < 4; ++j )
					for ( int k = 0; k < 4; ++k )
						holdingData[j][k] = this.planningData[ i - 1 ][ j * 4 + k ];

				String [] forecastData = MushroomPlot.getForecastedPlot( true, holdingData ).split( ";" );
				for ( int j = 0; j < 16; ++j )
				{
					this.planningData[i][j] = forecastData[j];
					this.originalData[i][j] = forecastData[j];
				}
			}

			this.updateImages();
		}

		private void updateImages()
		{
			for ( int i = 0; i < MAX_FORECAST; ++i )
				for ( int j = 0; j < 4; ++j )
					for ( int k = 0; k < 4; ++k )
						this.planningButtons[i][j][k].updateImage();
		}

		public JPanel constructPanel( int dayIndex, Component c )
		{
			JPanel panel = new JPanel( new BorderLayout() );
			panel.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );

			this.headers[dayIndex] = new JLabel( "Day " + (dayIndex + 1), JLabel.CENTER );

			panel.add( this.headers[dayIndex], BorderLayout.NORTH );
			panel.add( c, BorderLayout.CENTER );

			return panel;
		}

		private class MushroomButton extends ThreadedButton
		{
			private int dayIndex;
			private int loopIndex;
			private int squareIndex;

			public MushroomButton( int dayIndex, int squareIndex )
			{
				super( JComponentUtilities.getImage( "itemimages/dirt1.gif" ) );

				this.dayIndex = dayIndex;
				this.loopIndex = 4;
				this.squareIndex = squareIndex;

				JComponentUtilities.setComponentSize( this, 30, 30 );
			}

			public void run()
			{
				if ( this.dayIndex == ScriptGeneratePanel.this.currentForecast - 1 )
					return;

				ScriptGeneratePanel.this.planningData[ this.dayIndex ][ this.squareIndex ] = this.toggleMushroom();
				ScriptGeneratePanel.this.updateForecasts( this.dayIndex + 1 );
			}

			public void updateImage()
			{
				String currentMushroom = ScriptGeneratePanel.this.planningData[ this.dayIndex ][ this.squareIndex ];

				if ( currentMushroom.equals( "__" ) )
					this.setIcon( JComponentUtilities.getImage( "itemimages/dirt1.gif" ) );
				else if ( currentMushroom.equals( currentMushroom.toLowerCase() ) )
					this.setIcon( JComponentUtilities.getImage( "itemimages/mushsprout.gif" ) );
				else
					this.setIcon( JComponentUtilities.getImage( MushroomPlot.getMushroomImage( currentMushroom ) ) );

				for ( int i = 0; i < MushroomPlot.MUSHROOMS.length; ++i )
					if ( currentMushroom.equals( MushroomPlot.MUSHROOMS[i][2] ) || currentMushroom.equals( MushroomPlot.MUSHROOMS[i][3] ) )
						this.setToolTipText( (String) MushroomPlot.MUSHROOMS[i][5] );
			}

			private String toggleMushroom()
			{
				ScriptGeneratePanel.this.currentLayout = "";

				// Everything rotates based on what was there
				// when you clicked on the image.

				this.loopIndex = (this.loopIndex + 1) % 5;

				switch ( this.loopIndex )
				{
				// If you loop around, then test to see if the
				// old data was a blank.  If it was, then you
				// have already displayed it, so move on to the
				// next element in the cycle.  If not, return a
				// blank, as that's the next element in the cycle.

				case 0:

					if ( ScriptGeneratePanel.this.originalData[ this.dayIndex ][ this.squareIndex ].equals( "__" ) )
						this.loopIndex = 1;
					else
						return "__";

				// In all other cases, return the next element
				// in the mushroom toggle cycle.

				case 1:  return "kb";
				case 2:  return "kn";
				case 3:  return "sp";
				case 4:  return ScriptGeneratePanel.this.originalData[ this.dayIndex ][ this.squareIndex ];
				}

				return "__";
			}
		}
	}
}
