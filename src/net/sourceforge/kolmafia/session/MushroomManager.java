/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.MushroomRequest;

import net.sourceforge.kolmafia.swingui.MushroomFrame;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;

import net.sourceforge.kolmafia.webui.RelayLoader;

public abstract class MushroomManager
{
	private static final Pattern PLOT_PATTERN =
		Pattern.compile( "<b>Your Mushroom Plot:</b><p><table>(<tr>.*?</tr><tr>.*></tr><tr>.*?</tr><tr>.*</tr>)</table>" );
	private static final Pattern SQUARE_PATTERN = Pattern.compile( "<td>(.*?)</td>" );
	private static final Pattern IMAGE_PATTERN = Pattern.compile( ".*/((.*)\\.gif)" );

	// The player's mushroom plot
	//
	//  1  2  3  4
	//  5  6  7  8
	//  9 10 11 12
	// 13 14 15 16

	private static final String[][] actualPlot = new String[ 4 ][ 4 ];

	// Empty spot
	public static final int EMPTY = 0;

	// Sprout
	public static final int SPROUT = 1;

	// First generation mushrooms
	public static final int SPOOKY = 724;
	public static final int KNOB = 303;
	public static final int KNOLL = 723;

	// Second generation mushrooms
	public static final int WARM = 749;
	public static final int COOL = 751;
	public static final int POINTY = 753;

	// Third generation mushrooms
	public static final int FLAMING = 755;
	public static final int FROZEN = 756;
	public static final int STINKY = 757;

	// Special mushrooms
	public static final int GLOOMY = 1266;

	private static final int[][] SPORE_DATA =
	{
		{ SPOOKY, 30, 1 },
		{ KNOB, 40, 2 },
		{ KNOLL, 50, 3 }
	};

	// Assocations between the mushroom Ids
	// and the mushroom image.

	public static final Object [][] MUSHROOMS =
	{
		// Sprout and emptiness
		{ IntegerPool.get( EMPTY ), "dirt1.gif", "__", "__", IntegerPool.get( 0 ), "empty" },
		{ IntegerPool.get( SPROUT ), "mushsprout.gif", "..", "..", IntegerPool.get( 0 ), "unknown" },

		// First generation mushrooms
		{ IntegerPool.get( KNOB ), "mushroom.gif", "kb", "KB", IntegerPool.get( 1 ), "knob" },
		{ IntegerPool.get( KNOLL ), "bmushroom.gif", "kn", "KN", IntegerPool.get( 2 ), "knoll" },
		{ IntegerPool.get( SPOOKY ), "spooshroom.gif", "sp", "SP", IntegerPool.get( 3 ), "spooky" },

		// Second generation mushrooms
		{ IntegerPool.get( WARM ), "flatshroom.gif", "wa", "WA", IntegerPool.get( 4 ), "warm" },
		{ IntegerPool.get( COOL ), "plaidroom.gif", "co", "CO", IntegerPool.get( 5 ), "cool" },
		{ IntegerPool.get( POINTY ), "tallshroom.gif", "po", "PO", IntegerPool.get( 6 ), "pointy" },

		// Third generation mushrooms
		{ IntegerPool.get( FLAMING ), "fireshroom.gif", "fl", "FL", IntegerPool.get( 7 ), "flaming" },
		{ IntegerPool.get( FROZEN ), "iceshroom.gif", "fr", "FR", IntegerPool.get( 8 ), "frozen" },
		{ IntegerPool.get( STINKY ), "stinkshroo.gif", "st", "ST", IntegerPool.get( 9 ), "stinky" },

		// Special mushrooms
		{ IntegerPool.get( GLOOMY ), "blackshroo.gif", "gl", "GL", IntegerPool.get( 10 ), "gloomy" },
	};

	public static final int [][] BREEDING =
	{
		// EMPTY,   KNOB,    KNOLL,   SPOOKY,  WARM,    COOL,    POINTY   FLAMING  FROZEN   STINKY   GLOOMY

		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // EMPTY
		{  EMPTY,   KNOB,    COOL,    WARM,    EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // KNOB
		{  EMPTY,   COOL,    KNOLL,   POINTY,  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // KNOLL
		{  EMPTY,   WARM,    POINTY,  SPOOKY,  EMPTY,   EMPTY,   EMPTY,   EMPTY,   GLOOMY,  EMPTY,   EMPTY  },  // SPOOKY
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   WARM,    STINKY,  FLAMING, EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // WARM
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   STINKY,  COOL,    FROZEN,  EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // COOL
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   FLAMING, FROZEN,  POINTY,  EMPTY,   EMPTY,   EMPTY,   EMPTY  },  // POINTY
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   FLAMING, EMPTY,   EMPTY,   EMPTY  },  // FLAMING
		{  EMPTY,   EMPTY,   EMPTY,   GLOOMY,  EMPTY,   EMPTY,   EMPTY,   EMPTY,   FROZEN,  EMPTY,   EMPTY  },  // FROZEN
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   STINKY,  EMPTY  },  // STINKY
		{  EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY,   EMPTY  }   // GLOOMY
	};

	/**
	 * Utility method which returns a two-dimensional array showing the arrangement of the plot.
	 */

	public static final String getMushroomManager( final boolean isDataOnly )
	{
		if ( !MushroomManager.initialize() )
		{
			return "";
		}
		return MushroomManager.getMushroomManager( isDataOnly, MushroomManager.actualPlot );
	}

	/**
	 * Utility method which returns a two-dimensional array showing the arrangement of the forecasted plot (ie: what the
	 * plot will look like tomorrow).
	 */

	public static final String getForecastedPlot( final boolean isDataOnly )
	{
		return MushroomManager.getForecastedPlot( isDataOnly, MushroomManager.actualPlot );
	}

	public static final String getForecastedPlot( final boolean isDataOnly, final String[][] plot )
	{
		// Construct the forecasted plot now.

		boolean[][] changeList = new boolean[ 4 ][ 4 ];
		String[][] forecastPlot = new String[ 4 ][ 4 ];

		for ( int row = 0; row < 4; ++row )
		{
			for ( int col = 0; col < 4; ++col )
			{
				if ( plot[ row ][ col ].equals( "__" ) )
				{
					forecastPlot[ row ][ col ] = MushroomManager.getForecastSquare( row, col, plot );
					changeList[ row ][ col ] = !forecastPlot[ row ][ col ].equals( "__" );
				}
				else if ( plot[ row ][ col ].equals( plot[ row ][ col ].toLowerCase() ) )
				{
					forecastPlot[ row ][ col ] = plot[ row ][ col ].toUpperCase();
					changeList[ row ][ col ] = false;
				}
				else
				{
					forecastPlot[ row ][ col ] = plot[ row ][ col ];
					changeList[ row ][ col ] = false;
				}
			}
		}

		// Whenever the forecasted plot doesn't match the original plot, the
		// surrounding mushrooms are assumed to disappear.  Also forecast the
		// growth of the mushrooms.

		for ( int row = 0; row < 4; ++row )
		{
			for ( int col = 0; col < 4; ++col )
			{
				if ( changeList[ row ][ col ] )
				{
					if ( row != 0 && forecastPlot[ row - 1 ][ col ].equals( forecastPlot[ row - 1 ][ col ].toUpperCase() ) )
					{
						forecastPlot[ row - 1 ][ col ] = "__";
					}

					if ( row != 3 && forecastPlot[ row + 1 ][ col ].equals( forecastPlot[ row + 1 ][ col ].toUpperCase() ) )
					{
						forecastPlot[ row + 1 ][ col ] = "__";
					}

					if ( col != 0 && forecastPlot[ row ][ col - 1 ].equals( forecastPlot[ row ][ col - 1 ].toUpperCase() ) )
					{
						forecastPlot[ row ][ col - 1 ] = "__";
					}

					if ( col != 3 && forecastPlot[ row ][ col + 1 ].equals( forecastPlot[ row ][ col + 1 ].toUpperCase() ) )
					{
						forecastPlot[ row ][ col + 1 ] = "__";
					}
				}
			}
		}

		return MushroomManager.getMushroomManager( isDataOnly, forecastPlot );
	}

	private static final String getForecastSquare( final int row, final int col, final String[][] plot )
	{
		String[] touched = new String[ 4 ];

		// First, determine what kinds of mushrooms
		// touch the square.

		touched[ 0 ] = row == 0 ? "__" : plot[ row - 1 ][ col ];
		touched[ 1 ] = row == 3 ? "__" : plot[ row + 1 ][ col ];
		touched[ 2 ] = col == 0 ? "__" : plot[ row ][ col - 1 ];
		touched[ 3 ] = col == 3 ? "__" : plot[ row ][ col + 1 ];

		// Determine how many adult mushrooms total touch
		// the square.

		int[] touchIndex = new int[ 4 ];
		int touchCount = 0;

		for ( int i = 0; i < 4; ++i )
		{
			if ( !touched[ i ].equals( "__" ) && !touched[ i ].equals( ".." ) )
			{
				for ( int j = 0; j < MushroomManager.MUSHROOMS.length; ++j )
				{
					if ( touched[ i ].equals( MushroomManager.MUSHROOMS[ j ][ 3 ] ) )
					{
						touchIndex[ touchCount ] = ( (Integer) MushroomManager.MUSHROOMS[ j ][ 4 ] ).intValue();
					}
				}

				++touchCount;
			}
		}

		// If exactly two adult mushrooms are touching the
		// square, then return the result of the breed.

		if ( touchCount == 2 && MushroomManager.BREEDING[ touchIndex[ 0 ] ][ touchIndex[ 1 ] ] != MushroomManager.EMPTY )
		{
			return MushroomManager.getShorthand( MushroomManager.BREEDING[ touchIndex[ 0 ] ][ touchIndex[ 1 ] ], false );
		}

		// Otherwise, it'll be the same as whatever is
		// there right now.

		return plot[ row ][ col ];
	}

	private static final String getShorthand( final int mushroomType, final boolean isAdult )
	{
		for ( int i = 0; i < MushroomManager.MUSHROOMS.length; ++i )
		{
			if ( mushroomType == ( (Integer) MushroomManager.MUSHROOMS[ i ][ 0 ] ).intValue() )
			{
				return isAdult ? (String) MushroomManager.MUSHROOMS[ i ][ 3 ] : (String) MushroomManager.MUSHROOMS[ i ][ 2 ];
			}
		}

		return "__";
	}

	private static final String getMushroomManager( boolean isDataOnly, final String[][] plot )
	{
		// Otherwise, you need to construct the string form
		// of the mushroom plot.  Shorthand and hypertext are
		// the only two versions at the moment.

		StringBuffer plotBuffer = new StringBuffer();

		if ( !isDataOnly )
		{
			plotBuffer.append( KoLConstants.LINE_BREAK );
		}

		for ( int row = 0; row < 4; ++row )
		{
			// In a hypertext document, you initialize the
			// row in the table before you start appending
			// the squares.

			for ( int col = 0; col < 4; ++col )
			{
				// Hypertext documents need to have their cells opened before
				// the cell can be printed.

				if ( !isDataOnly )
				{
					plotBuffer.append( "  " );
				}

				String square = plot[ row ][ col ];

				// Mushroom images are used in hypertext documents, while
				// shorthand notation is used in non-hypertext documents.

				plotBuffer.append( square == null ? "__" : square );

				// Hypertext documents need to have their cells closed before
				// another cell can be printed.

				if ( isDataOnly )
				{
					plotBuffer.append( ";" );
				}
			}

			if ( !isDataOnly )
			{
				plotBuffer.append( KoLConstants.LINE_BREAK );
			}
		}

		// Now that the appropriate string has been constructed,
		// return it to the calling method.

		return plotBuffer.toString();
	}

	/**
	 * Utility method which retrieves the image associated with the given mushroom type.
	 */

	public static final String getMushroomImage( final String mushroomType )
	{
		for ( int i = 1; i < MushroomManager.MUSHROOMS.length; ++i )
		{
			if ( mushroomType.equals( MushroomManager.MUSHROOMS[ i ][ 2 ] ) )
			{
				return "itemimages/mushsprout.gif";
			}
			if ( mushroomType.equals( MushroomManager.MUSHROOMS[ i ][ 3 ] ) )
			{
				return "itemimages/" + MushroomManager.MUSHROOMS[ i ][ 1 ];
			}
		}

		return "itemimages/dirt1.gif";
	}

	/**
	 * Utility method which retrieves the mushroom which is associated with the given image.
	 */

	public static final int getMushroomType( final String mushroomImage )
	{
		for ( int i = 0; i < MushroomManager.MUSHROOMS.length; ++i )
		{
			if ( mushroomImage.endsWith( "/" + MushroomManager.MUSHROOMS[ i ][ 1 ] ) )
			{
				return ( (Integer) MushroomManager.MUSHROOMS[ i ][ 0 ] ).intValue();
			}
		}

		return MushroomManager.EMPTY;
	}

	public static final int[] getSporeDataByType( final int spore )
	{
		for ( int i = 0; i < MushroomManager.SPORE_DATA.length; ++i )
		{
                        int [] data = MushroomManager.SPORE_DATA[ i ];
			if ( data[ 0 ] == spore )
			{
                                return data;
			}
		}
                return null;
	}

	public static final int[] getSporeDataByIndex( final int index )
	{
		for ( int i = 0; i < MushroomManager.SPORE_DATA.length; ++i )
		{
                        int [] data = MushroomManager.SPORE_DATA[ i ];
			if ( data[ 2 ] == index )
			{
                                return data;
			}
		}
                return null;
	}

	public static final int getSporeType( final int[] data )
	{
                return data[0];
	}

	public static final String getSporeName( final int[] data )
	{
		return ItemDatabase.getItemName( data[0] );
	}

	public static final int getSporePrice( final int[] data )
	{
                return data[1];
	}

	public static final int getSporeIndex( final int[] data )
	{
                return data[2];
	}

	/**
	 * One of the major functions of the mushroom plot handler, this method plants the given spore into the given
	 * position (or square) of the mushroom plot.
	 */

	public static final boolean plantMushroom( final int square, final int spore )
	{
		// Validate square parameter.  It's possible that
		// the user input the wrong spore number.

		if ( square < 1 || square > 16 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Squares are numbered from 1 to 16." );
			return false;
		}

		// Find the spore that the user wishes to plant

                int [] data = MushroomManager.getSporeDataByType( spore );

		if ( data == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't plant that." );
			return false;
		}

                // Find KoL's internal spore index and the price

		int sporeIndex = MushroomManager.getSporeIndex( data );
                int sporePrice = MushroomManager.getSporePrice( data );

		// Make sure we have enough meat to pay for the spore.
		// Rather than using requirements validation, check the
		// character data.

		if ( KoLCharacter.getAvailableMeat() < sporePrice )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't afford that spore." );
			return false;
		}

		// Make sure we know current state of mushroom plot
		// before we plant the mushroom.  Bail if it fails.

		if ( !MushroomManager.initialize() )
		{
			return false;
		}

		// If the square isn't empty, pick what's there

		int row = ( square - 1 ) / 4;
		int col = ( square - 1 ) % 4;

		if ( !MushroomManager.actualPlot[ row ][ col ].equals( "__" ) && !MushroomManager.pickMushroom( square, true ) )
		{
			return false;
		}

		// Plant the requested spore.

		MushroomRequest request = new MushroomRequest( square, sporeIndex );
		KoLmafia.updateDisplay( "Planting " + ItemDatabase.getItemName( spore ) + " spore in square " + square + "..." );
		RequestThread.postRequest( request );

		// If it failed, bail.

		if ( !KoLmafia.permitsContinue() )
		{
			return false;
		}

		KoLmafia.updateDisplay( "Spore successfully planted." );
		return true;
	}

	public static final void clearField()
	{
		for ( int i = 1; i <= 16; ++i )
		{
			MushroomManager.pickMushroom( i, true );
		}
	}

	/**
	 * Picks all the mushrooms in all squares. This is equivalent to harvesting your mushroom crop, hence the name.
	 */

	public static final void harvestMushrooms()
	{
		for ( int i = 1; i <= 16; ++i )
		{
			MushroomManager.pickMushroom( i, false );
		}
	}

	/**
	 * One of the major functions of the mushroom plot handler, this method picks the mushroom located in the given
	 * square.
	 */

	public static final boolean pickMushroom( final int square, final boolean pickSpores )
	{
		// Validate square parameter.  It's possible that
		// the user input the wrong spore number.

		if ( square < 1 || square > 16 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Squares are numbered from 1 to 16." );
			return false;
		}

		// Make sure we know current state of mushroom plot
		// before we plant the mushroom.  Bail if it fails.

		if ( !MushroomManager.initialize() )
		{
			return false;
		}

		// If the square is not empty, run a request to pick
		// the mushroom in the square.

		int row = ( square - 1 ) / 4;
		int col = ( square - 1 ) % 4;

		boolean shouldPick = !MushroomManager.actualPlot[ row ][ col ].equals( "__" );
		shouldPick &=
			!MushroomManager.actualPlot[ row ][ col ].equals( MushroomManager.actualPlot[ row ][ col ].toLowerCase() ) || pickSpores;

		if ( shouldPick )
		{
			MushroomRequest request = new MushroomRequest( square );
			KoLmafia.updateDisplay( "Picking square " + square + "..." );
			RequestThread.postRequest( request );
			KoLmafia.updateDisplay( "Square picked." );
		}

		return KoLmafia.permitsContinue();
	}

	/**
	 * Utility method used to initialize the state of the plot into the one-dimensional array.
	 */

	public static final boolean ownsPlot()
	{
		return KoLCharacter.getAscensions() == Preferences.getInteger( "lastMushroomPlot" );
	}

	private static final boolean initialize()
	{
		if ( MushroomManager.ownsPlot() )
		{
			return true;
		}

		// If you can't go inside Degrassi Knoll, no go
		if ( !KoLCharacter.knollAvailable() && !KoLCharacter.inZombiecore() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't find the mushroom fields." );
			return false;
		}

		RequestThread.postRequest( new MushroomRequest() );

		if ( !MushroomManager.ownsPlot() )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You haven't bought a mushroom plot yet." );
			return false;
		}

		return true;
	}

	public static final void parsePlot( final String text )
	{
		// Pretend all of the sections on the plot are empty
		// before you begin parsing the plot.

		for ( int row = 0; row < 4; ++row )
		{
			for ( int col = 0; col < 4; ++col )
			{
				MushroomManager.actualPlot[ row ][ col ] = "__";
			}
		}

		Matcher plotMatcher = MushroomManager.PLOT_PATTERN.matcher( text );

		if ( !plotMatcher.find() )
		{
			return;
		}

		// Remember that we have bought a plot this ascension
		Preferences.setInteger( "lastMushroomPlot", KoLCharacter.getAscensions() );

		// Find all of the squares.
		Matcher squareMatcher = MushroomManager.SQUARE_PATTERN.matcher( plotMatcher.group( 1 ) );

		for ( int row = 0; row < 4; ++row )
		{
			for ( int col = 0; col < 4 && squareMatcher.find(); ++col )
			{
				int result = MushroomManager.parseSquare( squareMatcher.group( 1 ) );
				MushroomManager.actualPlot[ row ][ col ] = MushroomManager.getShorthand( result, true );
			}
		}
	}

	private static final int parseSquare( final String text )
	{
		// We figure out what's there based on the image.  This
		// is done by checking the text in the square against
		// the table of square values.

		Matcher gifMatcher = MushroomManager.IMAGE_PATTERN.matcher( text );
		if ( gifMatcher.find() )
		{
			String gif = gifMatcher.group( 1 );
			for ( int i = 0; i < MushroomManager.MUSHROOMS.length; ++i )
			{
				if ( gif.equals( MushroomManager.MUSHROOMS[ i ][ 1 ] ) )
				{
					return ( (Integer) MushroomManager.MUSHROOMS[ i ][ 0 ] ).intValue();
				}
			}
		}

		return MushroomManager.EMPTY;
	}

	public static final int loadLayout( final String filename, final String[][] originalData,
		final String[][] planningData )
	{
		// The easiest file to parse that is already provided is
		// the text file which was generated automatically.

		int dayIndex = 0;
		BufferedReader reader = FileUtilities.getReader( new File( KoLConstants.PLOTS_LOCATION, filename + ".txt" ) );

		try
		{
			String line = "";
			String[][] arrayData = new String[ 4 ][ 4 ];

			while ( line != null )
			{
				if ( dayIndex == 0 )
				{
					for ( int i = 0; i < 16; ++i )
					{
						originalData[ dayIndex ][ i ] = "__";
					}
				}
				else if ( dayIndex < originalData.length )
				{
					for ( int i = 0; i < 4; ++i )
					{
						for ( int j = 0; j < 4; ++j )
						{
							arrayData[ i ][ j ] = planningData[ dayIndex - 1 ][ i * 4 + j ];
						}
					}

					originalData[ dayIndex ] = MushroomManager.getForecastedPlot( true, arrayData ).split( ";" );
				}

				// Skip four lines from the mushroom plot,
				// which only contain header information.

				for ( int i = 0; i < 4 && line != null; ++i )
				{
					line = reader.readLine();
				}

				// Now, split the line into individual units
				// based on whitespace.

				if ( line != null )
				{
					// Get the plot that will result from the
					// previous day's plantings.

					for ( int i = 0; i < 4; ++i )
					{
						if ( ( line = reader.readLine() ) == null )
						{
							break;
						}

						line = line.trim();
						String[] pieces = line.split( "\\*?\\s+" );

						if ( line != null )
						{
							for ( int j = 4; j < 8; ++j )
							{
								planningData[ dayIndex ][ i * 4 + j - 4 ] = pieces[ j ].substring( 0, 2 );
							}
						}
					}

					// Now that you've wrapped up a day, eat
					// an empty line and continue on with the
					// next iteration.

					++dayIndex;
					line = reader.readLine();
				}
			}

			for ( int i = 0; dayIndex > 0 && i < 16; ++i )
			{
				planningData[ dayIndex - 1 ][ i ] = originalData[ dayIndex - 1 ][ i ];
			}
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return Math.max( dayIndex, 2 );
		}

		// Make sure to close the reader after you're done reading
		// all the data in the file.

		try
		{
			reader.close();
			return Math.max( dayIndex, 2 );
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return Math.max( dayIndex, 2 );
		}
	}

	private static final void copyMushroomImage( final String location )
	{
		File source = new File( KoLConstants.IMAGE_LOCATION, location );
		File destination = new File( KoLConstants.PLOTS_LOCATION + "/" + location );

		FileUtilities.copyFile( source, destination );
	}

	public static final void saveLayout( final String filename, final String[][] originalData,
		final String[][] planningData )
	{
		File preview = new File( KoLConstants.PLOTS_LOCATION, filename + ".htm" );

		PrintStream textLayout = LogStream.openStream( new File( KoLConstants.PLOTS_LOCATION, filename + ".txt" ), true );
		PrintStream htmlLayout = LogStream.openStream( preview, true );
		PrintStream plotScript = LogStream.openStream( new File( KoLConstants.PLOTS_LOCATION, filename + ".ash" ), true );

		// The HTML file needs a little bit of header information
		// to make it proper HTML.

		htmlLayout.println( "<html><body>" );

		// Now that we know that data files can be written okay,
		// begin writing layout data.

		ArrayList days = new ArrayList();
		String image = null;
		boolean isTodayEmpty = false;

		for ( int i = 0; i < MushroomFrame.MAX_FORECAST && !isTodayEmpty; ++i )
		{
			textLayout.println();
			textLayout.println( "Day " + ( i + 1 ) + ":" );
			textLayout.println();
			textLayout.println( "Pick                Plant" );

			htmlLayout.println( "<table border=0 cellpadding=0 cellspacing=0>" );
			htmlLayout.println( "<tr><td colspan=9><b>Day " + ( i + 1 ) + ":" + "</b></td></tr>" );
			htmlLayout.println( "<tr><td colspan=4>Pick</td><td width=50>&nbsp;</td><td colspan=4>Plant</td></tr>" );

			// Compile all the commands which are needed for the
			// planting script.

			isTodayEmpty = true;
			ArrayList commands = new ArrayList();

			StringBuffer pickText = new StringBuffer();
			StringBuffer pickHtml = new StringBuffer();
			StringBuffer plantText = new StringBuffer();
			StringBuffer plantHtml = new StringBuffer();

			for ( int j = 0; j < 16; ++j )
			{
				if ( i == 0 )
				{
					commands.add( "field pick " + ( j + 1 ) );
				}

				// If you've reached the end of a row, then you
				// will need to add line breaks.

				if ( j > 0 && j % 4 == 0 )
				{
					textLayout.print( pickText.toString() );
					textLayout.print( "     " );
					textLayout.println( plantText.toString() );

					pickText.setLength( 0 );
					plantText.setLength( 0 );

					htmlLayout.println( "<tr>" );
					htmlLayout.print( "\t" );
					htmlLayout.println( pickHtml.toString() );
					htmlLayout.print( "<td>&nbsp;</td>" );
					htmlLayout.print( "\t" );
					htmlLayout.println( plantHtml.toString() );
					htmlLayout.print( "<td>&nbsp;</td>" );
					htmlLayout.println( "</tr>" );

					pickHtml.setLength( 0 );
					plantHtml.setLength( 0 );
				}

				// If the data in the original is different from
				// the planned, then script commands are needed.
				// Also, the HTML and textual layouts will be a
				// bit different pending on what happened.

				boolean pickRequired =
					!originalData[ i ][ j ].equals( "__" ) && !originalData[ i ][ j ].equals( planningData[ i ][ j ] );

				if ( pickRequired )
				{
					pickText.append( " ***" );
					pickHtml.append( "<td style=\"border: 1px dashed red\"><img src=\"" );
					commands.add( "field pick " + ( j + 1 ) );
				}
				else
				{
					pickText.append( " " + originalData[ i ][ j ] + " " );
					pickHtml.append( "<td><img src=\"" );
				}

				image = MushroomManager.getMushroomImage( originalData[ i ][ j ] );
				MushroomManager.copyMushroomImage( image );

				pickHtml.append( image );
				pickHtml.append( "\"></td>" );

				// Spore additions are a little trickier than looking
				// just at the difference.  Only certain spores can be
				// planted, and only certain

				boolean addedSpore = !originalData[ i ][ j ].equals( planningData[ i ][ j ] );

				if ( addedSpore )
				{
					addedSpore = false;

					if ( planningData[ i ][ j ].startsWith( "kb" ) )
					{
						commands.add( "field plant " + ( j + 1 ) + " knob" );
						addedSpore = true;
					}
					else if ( planningData[ i ][ j ].startsWith( "kn" ) )
					{
						commands.add( "field plant " + ( j + 1 ) + " knoll" );
						addedSpore = true;
					}
					else if ( planningData[ i ][ j ].startsWith( "sp" ) )
					{
						commands.add( "field plant " + ( j + 1 ) + " spooky" );
						addedSpore = true;
					}
				}

				// Now that you know for sure whether or not a spore
				// was added or a breeding result, update the text.

				plantText.append( " " + planningData[ i ][ j ] );

				if ( addedSpore )
				{
					plantText.append( "*" );
					plantHtml.append( "<td style=\"border: 1px dashed blue\"><img src=\"" );

					image = MushroomManager.getMushroomImage( planningData[ i ][ j ].toUpperCase() );
					MushroomManager.copyMushroomImage( image );
					plantHtml.append( image );

					plantHtml.append( "\"></td>" );
				}
				else
				{
					plantText.append( " " );
					plantHtml.append( "<td><img src=\"" );

					image = MushroomManager.getMushroomImage( planningData[ i ][ j ] );
					MushroomManager.copyMushroomImage( image );
					plantHtml.append( image );

					plantHtml.append( "\"></td>" );
				}

				isTodayEmpty &= planningData[ i ][ j ].equals( "__" );
			}

			// Print the data for the last row.

			textLayout.print( pickText.toString() );
			textLayout.print( "     " );
			textLayout.println( plantText.toString() );

			pickText.setLength( 0 );
			plantText.setLength( 0 );

			htmlLayout.println( "<tr>" );
			htmlLayout.print( "\t" );
			htmlLayout.println( pickHtml.toString() );
			htmlLayout.print( "<td>&nbsp;</td>" );
			htmlLayout.print( "\t" );
			htmlLayout.println( plantHtml.toString() );
			htmlLayout.print( "<td>&nbsp;</td>" );
			htmlLayout.println( "</tr>" );

			pickHtml.setLength( 0 );
			plantHtml.setLength( 0 );

			// Print any needed trailing whitespace into the layouts
			// and add the list of commands to be processed later.

			textLayout.println();
			htmlLayout.println( "</table><br /><br />" );

			if ( !isTodayEmpty )
			{
				days.add( commands );
			}
		}

		// All data has been printed.  Add the closing tags to the
		// HTML version and then close the streams.

		try
		{
			textLayout.close();
			htmlLayout.println( "</body></html>" );
			htmlLayout.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return;
		}

		// Now that all of the commands have been compiled, generate
		// the ASH script which will do the layout.

		try
		{
			plotScript.println( "void main()" );
			plotScript.println( "{" );
			plotScript.println();
			plotScript.println( "    if ( !have_mushroom_plot() )" );
			plotScript.println( "    {" );
			plotScript.println( "        print( \"You do not have a mushroom plot.\" );" );
			plotScript.println( "        return;" );
			plotScript.println( "    }" );
			plotScript.println();
			plotScript.println( "    if ( get_property( \"plantingScript\" ) != \"" + filename + "\" )" );
			plotScript.println( "    {" );
			plotScript.println( "        set_property( \"plantingDay\", -1 );" );
			plotScript.println( "        set_property( \"plantingDate\", -1 );" );
			plotScript.println( "        set_property( \"plantingLength\", " + days.size() + " );" );
			plotScript.println( "        set_property( \"plantingScript\", \"" + filename + "\" );" );
			plotScript.println( "    }" );
			plotScript.println();
			plotScript.println( "    if ( get_property( \"plantingDate\" ).string_to_int() == moon_phase() )" );
			plotScript.println( "        return;" );
			plotScript.println();
			plotScript.println( "    set_property( \"plantingDate\", moon_phase() );" );
			plotScript.println( "    int index = (get_property( \"plantingDay\" ).string_to_int() + 1) % " + days.size() + ";" );
			plotScript.println( "    set_property( \"plantingDay\", index );" );

			for ( int i = 0; i < days.size(); ++i )
			{
				ArrayList commands = (ArrayList) days.get( i );

				if ( !commands.isEmpty() )
				{
					plotScript.println();
					plotScript.println( "    if ( index == " + i + " )" );
					plotScript.print( "        cli_execute( \"" );

					for ( int j = 0; j < commands.size(); ++j )
					{
						if ( j != 0 )
						{
							plotScript.print( ";" );
						}
						plotScript.print( commands.get( j ) );
					}

					plotScript.println( "\" );" );
				}
			}

			plotScript.println();
			plotScript.println( "}" );
			plotScript.close();
		}
		catch ( Exception e )
		{
			StaticEntity.printStackTrace( e );
			return;
		}

		// Now that everything has been generated, open the HTML
		// inside of a browser.

		RelayLoader.openSystemBrowser( preview );
	}
}
