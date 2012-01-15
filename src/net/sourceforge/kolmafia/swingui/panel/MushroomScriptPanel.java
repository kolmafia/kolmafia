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
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.MushroomManager;
import net.sourceforge.kolmafia.swingui.MushroomFrame;
import net.sourceforge.kolmafia.swingui.button.InvocationButton;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class MushroomScriptPanel
	extends JPanel
{
	private static final Color TODAY_COLOR = new Color( 192, 255, 192 );
	private static final Color OTHER_COLOR = new Color( 240, 240, 240 );

	private String currentLayout = "";

	private final JPanel centerPanel;
	private int currentForecast = 2;
	private final JButton addToLayoutButton, deleteFromLayoutButton;

	private final String[][] planningData;
	private final String[][] originalData;

	private final JLabel[] headers;
	private final JPanel[] planningPanels;
	private final MushroomButton[][][] planningButtons;

	public MushroomScriptPanel()
	{
		this.headers = new JLabel[ MushroomFrame.MAX_FORECAST + 1 ];

		this.planningData = new String[ MushroomFrame.MAX_FORECAST + 1 ][ 16 ];
		this.originalData = new String[ MushroomFrame.MAX_FORECAST + 1 ][ 16 ];

		for ( int i = 0; i < MushroomFrame.MAX_FORECAST; ++i )
		{
			for ( int j = 0; j < 16; ++j )
			{
				this.planningData[ i ][ j ] = "__";
				this.originalData[ i ][ j ] = "__";
			}
		}

		this.centerPanel = new JPanel( new GridLayout( 0, 4, 20, 20 ) );

		// Now add the first panel to the layout so that the person
		// can add more panels as they are needed.

		this.planningPanels = new JPanel[ MushroomFrame.MAX_FORECAST + 1 ];
		this.planningButtons = new MushroomButton[ MushroomFrame.MAX_FORECAST + 1 ][ 4 ][ 4 ];

		for ( int i = 0; i < MushroomFrame.MAX_FORECAST; ++i )
		{
			this.planningPanels[ i ] = new JPanel( new GridLayout( 4, 4, 0, 2 ) );
			for ( int j = 0; j < 4; ++j )
			{
				for ( int k = 0; k < 4; ++k )
				{
					this.planningButtons[ i ][ j ][ k ] = new MushroomButton( i, j * 4 + k );
					this.planningPanels[ i ].add( this.planningButtons[ i ][ j ][ k ] );
				}
			}
		}

		this.centerPanel.add( this.constructPanel( 0, this.planningPanels[ 0 ] ) );
		this.centerPanel.add( this.constructPanel( 1, this.planningPanels[ 1 ] ) );

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
		this.currentLayout = Preferences.getString( "plantingScript" );
		this.initializeLayout();
	}

	private void enableLayout()
	{
		for ( int i = 0; i < this.currentForecast; ++i )
		{
			this.headers[ i ].setText( "Day " + ( i + 1 ) );
		}

		this.headers[ this.currentForecast - 1 ].setText( "Final Day" );

		for ( int i = 0; i < 16; ++i )
		{
			this.planningData[ this.currentForecast ][ i ] = "__";
			this.originalData[ this.currentForecast ][ i ] = "__";
		}

		this.updateForecasts( this.currentForecast - 1 );

		this.centerPanel.validate();
		this.centerPanel.repaint();

		this.addToLayoutButton.setEnabled( this.currentForecast != MushroomFrame.MAX_FORECAST );
		this.deleteFromLayoutButton.setEnabled( this.currentForecast != 2 );
	}

	public void addToLayout()
	{
		this.centerPanel.invalidate();
		this.centerPanel.add(
			this.constructPanel( this.currentForecast, this.planningPanels[ this.currentForecast ] ),
			this.currentForecast < 3 ? this.currentForecast : this.currentForecast + 1 );

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
			Preferences.setInteger( "plantingDay", -1 );
			Preferences.setString( "plantingDate", "" );
			Preferences.setInteger( "plantingLength", 0 );
		}
		else
		{
			plantingLength = MushroomManager.loadLayout( this.currentLayout, this.originalData, this.planningData );
			indexToHighlight = Preferences.getInteger( "plantingDay" );
		}

		if ( plantingLength > this.currentForecast )
		{
			this.centerPanel.invalidate();
			for ( int i = this.currentForecast; i < plantingLength; ++i )
			{
				this.centerPanel.add( this.constructPanel( i, this.planningPanels[ i ] ), i < 3 ? i : i + 1 );
			}

			this.currentForecast = plantingLength;
			this.enableLayout();
		}
		else if ( plantingLength > 1 )
		{
			this.centerPanel.invalidate();
			for ( int i = this.currentForecast; i > plantingLength; --i )
			{
				this.centerPanel.remove( i < 4 ? i - 1 : i );
			}

			this.currentForecast = plantingLength;
			this.enableLayout();
		}

		String today = KoLConstants.DAILY_FORMAT.format( new Date() );

		if ( !Preferences.getString( "plantingDate" ).equals( today ) )
		{
			++indexToHighlight;
		}

		for ( int i = 0; i < this.currentForecast; ++i )
		{
			this.headers[ i ].setBackground( i == indexToHighlight ? MushroomScriptPanel.TODAY_COLOR : MushroomScriptPanel.OTHER_COLOR );
		}

		this.updateImages();
	}

	public void runLayout()
	{
		if ( this.currentLayout.equals( "" ) )
		{
			this.saveLayout();
		}

		if ( !this.currentLayout.equals( "" ) )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "call " + KoLConstants.PLOTS_DIRECTORY + this.currentLayout + ".ash" );
		}
	}

	public void loadLayout()
	{
		File[] layouts = DataUtilities.listFiles( KoLConstants.PLOTS_LOCATION );
		ArrayList names = new ArrayList();

		for ( int i = 0; i < layouts.length; ++i )
		{
			String name = layouts[ i ].getName();
			if ( name.endsWith( ".txt" ) )
			{
				name = name.substring( 0, name.length() - 4 );
				if ( !names.contains( name ) )
				{
					names.add( name );
				}
			}
		}

		if ( names.isEmpty() )
		{
			return;
		}

		String layout = (String) InputFieldUtilities.input( "Which mushroom plot?", names.toArray() );
		if ( layout != null )
		{
			this.loadLayout( layout );
		}
	}

	public void loadLayout( final String layout )
	{
		if ( layout == null || layout.equals( "" ) || this.currentLayout.equals( layout ) )
		{
			return;
		}

		this.currentLayout = layout;
		this.initializeLayout();
	}

	public void saveLayout()
	{
		String location = InputFieldUtilities.input( "Name your mushroom plot!" );
		if ( location == null )
		{
			return;
		}

		this.currentLayout = location;

		String[] planned = new String[ 16 ];

		for ( int i = 0; i < 16; ++i )
		{
			planned[ i ] = this.planningData[ this.currentForecast - 1 ][ i ];
			this.planningData[ this.currentForecast - 1 ][ i ] = "__";
		}

		MushroomManager.saveLayout( location, this.originalData, this.planningData );
		for ( int i = 0; i < 16; ++i )
		{
			this.planningData[ this.currentForecast - 1 ][ i ] = planned[ i ];
		}
	}

	public void updateForecasts( final int startDay )
	{
		for ( int i = startDay; i < MushroomFrame.MAX_FORECAST; ++i )
		{
			String[][] holdingData = new String[ 4 ][ 4 ];
			for ( int j = 0; j < 4; ++j )
			{
				for ( int k = 0; k < 4; ++k )
				{
					holdingData[ j ][ k ] = this.planningData[ i - 1 ][ j * 4 + k ];
				}
			}

			String[] forecastData = MushroomManager.getForecastedPlot( true, holdingData ).split( ";" );
			for ( int j = 0; j < 16; ++j )
			{
				this.planningData[ i ][ j ] = forecastData[ j ];
				this.originalData[ i ][ j ] = forecastData[ j ];
			}
		}

		this.updateImages();
	}

	private void updateImages()
	{
		for ( int i = 0; i < MushroomFrame.MAX_FORECAST; ++i )
		{
			for ( int j = 0; j < 4; ++j )
			{
				for ( int k = 0; k < 4; ++k )
				{
					this.planningButtons[ i ][ j ][ k ].updateImage();
				}
			}
		}
	}

	public JPanel constructPanel( final int dayIndex, final Component c )
	{
		JPanel panel = new JPanel( new BorderLayout() );
		panel.setBorder( BorderFactory.createLineBorder( Color.black, 1 ) );

		this.headers[ dayIndex ] = new JLabel( "Day " + ( dayIndex + 1 ), SwingConstants.CENTER );

		panel.add( this.headers[ dayIndex ], BorderLayout.NORTH );
		panel.add( c, BorderLayout.CENTER );

		return panel;
	}

	private class MushroomButton
		extends JButton
		implements ActionListener
	{
		private final int dayIndex;
		private int loopIndex;
		private final int squareIndex;

		public MushroomButton( final int dayIndex, final int squareIndex )
		{
			super( JComponentUtilities.getImage( "itemimages/dirt1.gif" ) );

			this.dayIndex = dayIndex;
			this.loopIndex = 4;
			this.squareIndex = squareIndex;

			JComponentUtilities.setComponentSize( this, 30, 30 );
			this.addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( this.dayIndex == MushroomScriptPanel.this.currentForecast - 1 )
			{
				return;
			}

			MushroomScriptPanel.this.planningData[ this.dayIndex ][ this.squareIndex ] = this.toggleMushroom();
			MushroomScriptPanel.this.updateForecasts( this.dayIndex + 1 );
		}

		public void updateImage()
		{
			String currentMushroom = MushroomScriptPanel.this.planningData[ this.dayIndex ][ this.squareIndex ];

			if ( currentMushroom.equals( "__" ) )
			{
				this.setIcon( JComponentUtilities.getImage( "itemimages/dirt1.gif" ) );
			}
			else if ( currentMushroom.equals( currentMushroom.toLowerCase() ) )
			{
				this.setIcon( JComponentUtilities.getImage( "itemimages/mushsprout.gif" ) );
			}
			else
			{
				this.setIcon( JComponentUtilities.getImage( MushroomManager.getMushroomImage( currentMushroom ) ) );
			}

			for ( int i = 0; i < MushroomManager.MUSHROOMS.length; ++i )
			{
				if ( currentMushroom.equals( MushroomManager.MUSHROOMS[ i ][ 2 ] ) || currentMushroom.equals( MushroomManager.MUSHROOMS[ i ][ 3 ] ) )
				{
					this.setToolTipText( (String) MushroomManager.MUSHROOMS[ i ][ 5 ] );
				}
			}
		}

		private String toggleMushroom()
		{
			MushroomScriptPanel.this.currentLayout = "";

			// Everything rotates based on what was there
			// when you clicked on the image.

			this.loopIndex = ( this.loopIndex + 1 ) % 5;

			switch ( this.loopIndex )
			{
			// If you loop around, then test to see if the
			// old data was a blank.  If it was, then you
			// have already displayed it, so move on to the
			// next element in the cycle.  If not, return a
			// blank, as that's the next element in the cycle.

			case 0:

				if ( MushroomScriptPanel.this.originalData[ this.dayIndex ][ this.squareIndex ].equals( "__" ) )
				{
					this.loopIndex = 1;
				}
				else
				{
					return "__";
				}

				// In all other cases, return the next element
				// in the mushroom toggle cycle.

			case 1:
				return "kb";
			case 2:
				return "kn";
			case 3:
				return "sp";
			case 4:
				return MushroomScriptPanel.this.originalData[ this.dayIndex ][ this.squareIndex ];
			}

			return "__";
		}
	}
}
