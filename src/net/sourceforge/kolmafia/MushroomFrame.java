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
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;

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

		tabs.addTab( "One Day Planting", plantPanel );

		JPanel planPanel = new JPanel();
		planPanel.add( new ScriptGeneratePanel() );

		tabs.addTab( "Script Generator", planPanel );

		framePanel.setLayout( new CardLayout( 10, 10 ) );
		framePanel.add( tabs, "" );
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

			setLayout( new CardLayout( 40, 40 ) );
			add( completePanel, "" );

			plotChanged();
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
						MushroomPlot.plantMushroom( i + 1, MushroomPlot.getMushroomType( layoutData[i] ) );
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
				LogStream ostream = LogStream.openStream( output, true );
				ostream.println( "field harvest" );

				for ( int i = 0; i < 16; ++i )
				{
					int mushroomType = MushroomPlot.getMushroomType( layoutData[i] );
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

			String [][] layoutArray = new String[4][4];
			for ( int i = 0; i < 4; ++i )
				for ( int j = 0; j < 4; ++j )
					layoutArray[i][j] = layoutData[ i * 4 + j ];

			String [] forecastData = MushroomPlot.getForecastedPlot( true, layoutArray ).split( ";" );

			// What you do is you update each mushroom button based on
			// what is contained in each of the data fields.

			for ( int i = 0; i < 4; ++i )
			{
				for ( int j = 0; j < 4; ++j )
				{
					currentButtons[i][j].setIcon( JComponentUtilities.getImage( currentData[ i * 4 + j ] ) );
					layoutButtons[i][j].setIcon( JComponentUtilities.getImage( layoutData[ i * 4 + j ] ) );
					forecastButtons[i][j].setIcon( JComponentUtilities.getImage( forecastData[ i * 4 + j ] ) );
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
			headers = new JLabel[MAX_FORECAST + 1];

			planningData = new String[MAX_FORECAST + 1][16];
			originalData = new String[MAX_FORECAST + 1][16];

			for ( int i = 0; i < MAX_FORECAST; ++i )
			{
				for ( int j = 0; j < 16; ++j )
				{
					planningData[i][j] = "__";
					originalData[i][j] = "__";
				}
			}

			centerPanel = new JPanel( new GridLayout( 0, 4, 20, 20 ) );

			// Now add the first panel to the layout so that the person
			// can add more panels as they are needed.

			planningPanels = new JPanel[MAX_FORECAST + 1];
			planningButtons = new MushroomButton[MAX_FORECAST + 1][4][4];

			for ( int i = 0; i < MAX_FORECAST; ++i )
			{
				planningPanels[i] = new JPanel( new GridLayout( 4, 4, 0, 2 ) );
				for ( int j = 0; j < 4; ++j )
				{
					for ( int k = 0; k < 4; ++k )
					{
						planningButtons[i][j][k] = new MushroomButton( i, j * 4 + k );
						planningPanels[i].add( planningButtons[i][j][k] );
					}
				}
			}

			centerPanel.add( constructPanel( 0, planningPanels[0] ) );
			centerPanel.add( constructPanel( 1, planningPanels[1] ) );

			// Dummy buttons for the mushroom plot (just for layout
			// viewing purposes.  To be replaced with real functionality
			// at a later date.

			JPanel buttonPanel = new JPanel( new GridLayout( 0, 1, 5, 5 ) );

			// Now add the various action buttons.

			addToLayoutButton = new InvocationButton( "Add a Day", this, "addToLayout" );
			deleteFromLayoutButton = new InvocationButton( "Delete a Day", this, "removeFromLayout" );
			deleteFromLayoutButton.setEnabled( false );

			buttonPanel.add( addToLayoutButton );
			buttonPanel.add( deleteFromLayoutButton );
			buttonPanel.add( new InvocationButton( "Run Layout", this, "runLayout" ) );
			buttonPanel.add( new InvocationButton( "Load Layout", this, "loadLayout" ) );
			buttonPanel.add( new InvocationButton( "Save Layout", this, "saveLayout" ) );
			centerPanel.add( buttonPanel );

			setLayout( new CardLayout( 40, 40 ) );
			add( centerPanel, "" );

			enableLayout();
			currentLayout = StaticEntity.getProperty( "plantingScript" );
			initializeLayout();
		}

		private void enableLayout()
		{
			for ( int i = 0; i < currentForecast; ++i )
				headers[i].setText( "Day " + (i+1) );

			headers[ currentForecast - 1 ].setText( "Final Day" );

			for ( int i = 0; i < 16; ++i )
			{
				planningData[ currentForecast ][i] = "__";
				originalData[ currentForecast ][i] = "__";
			}

			updateForecasts( currentForecast - 1 );

			centerPanel.validate();
			centerPanel.repaint();
			pack();

			addToLayoutButton.setEnabled( currentForecast != MAX_FORECAST );
			deleteFromLayoutButton.setEnabled( currentForecast != 2 );
		}

		public void addToLayout()
		{
			centerPanel.invalidate();
			centerPanel.add( constructPanel( currentForecast, planningPanels[currentForecast] ),
				(currentForecast < 3) ? currentForecast : (currentForecast + 1) );

			++currentForecast;
			enableLayout();
		}

		public void removeFromLayout()
		{
			centerPanel.invalidate();
			centerPanel.remove( currentForecast < 4 ? currentForecast - 1 : currentForecast );

			--currentForecast;
			enableLayout();
		}

		public void initializeLayout()
		{
			int plantingLength = 2;
			int indexToHighlight = 0;

			if ( currentLayout.equals( "" ) )
			{
				StaticEntity.setProperty( "plantingDay", "-1" );
				StaticEntity.setProperty( "plantingDate", "" );
				StaticEntity.setProperty( "plantingLength", "0" );
			}
			else
			{
				plantingLength = MushroomPlot.loadLayout( currentLayout, originalData, planningData );
				indexToHighlight = StaticEntity.getIntegerProperty( "plantingDay" );
			}

			if ( plantingLength > currentForecast )
			{
				centerPanel.invalidate();
				for ( int i = currentForecast; i < plantingLength; ++i )
					centerPanel.add( constructPanel( i, planningPanels[ i ] ), (i < 3) ? i : (i+1) );

				currentForecast = plantingLength;
				enableLayout();
			}
			else if ( plantingLength > 1 )
			{
				centerPanel.invalidate();
				for ( int i = currentForecast; i > plantingLength; --i )
					centerPanel.remove( i < 4 ? i - 1 : i );

				currentForecast = plantingLength;
				enableLayout();
			}


			String today = DATED_FILENAME_FORMAT.format( new Date() );

			if ( !StaticEntity.getProperty( "plantingDate" ).equals( today ) )
				++indexToHighlight;

			for ( int i = 0; i < currentForecast; ++i )
				headers[i].setBackground( i == indexToHighlight ? TODAY_COLOR : OTHER_COLOR );

			updateImages();
		}

		public void runLayout()
		{
			if ( currentLayout.equals( "" ) )
				saveLayout();

			if ( !currentLayout.equals( "" ) )
				DEFAULT_SHELL.executeLine( "call " + MushroomPlot.PLOT_DIRECTORY.getPath() + "/" + currentLayout + ".ash" );
		}

		public void loadLayout()
		{
			if ( !MushroomPlot.PLOT_DIRECTORY.exists() )
				return;

			File [] layouts = MushroomPlot.PLOT_DIRECTORY.listFiles();
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

			loadLayout( (String) JOptionPane.showInputDialog( null,
				"Which mushroom plot?", "", JOptionPane.OK_OPTION, null, names.toArray(), null ) );
		}

		public void loadLayout( String layout )
		{
			if ( layout == null || layout.equals( "" ) || currentLayout.equals( layout ) )
				return;

			currentLayout = layout;
			initializeLayout();
		}

		public void saveLayout()
		{
			String location = JOptionPane.showInputDialog( "Name your mushroom plot!", "" );
			if ( location == null )
				return;

			currentLayout = location;

			String [] planned = new String[16];

			for ( int i = 0; i < 16; ++i )
			{
				planned[i] = planningData[ currentForecast - 1 ][i];
				planningData[ currentForecast - 1 ][i] = "__";
			}

			MushroomPlot.saveLayout( location, originalData, planningData );
			for ( int i = 0; i < 16; ++i )
				planningData[ currentForecast - 1 ][i] = planned[i];
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

			updateImages();
		}

		private void updateImages()
		{
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
				if ( dayIndex == currentForecast - 1 )
					return;

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

				for ( int i = 0; i < MushroomPlot.MUSHROOMS.length; ++i )
					if ( currentMushroom.equals( MushroomPlot.MUSHROOMS[i][2] ) || currentMushroom.equals( MushroomPlot.MUSHROOMS[i][3] ) )
						setToolTipText( (String) MushroomPlot.MUSHROOMS[i][5] );
			}

			private String toggleMushroom()
			{
				currentLayout = "";

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
}
