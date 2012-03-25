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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HedgePuzzleRequest
	extends GenericRequest
{
	private static final GenericRequest HEDGE_REQUEST = new GenericRequest( "lair3.php?action=hedge" );
	private static final HedgePuzzleRequest PUZZLE_REQUEST = new HedgePuzzleRequest();
	private static final HedgePuzzleRequest ROTATE_REQUEST = new HedgePuzzleRequest();

	private static final Pattern ACTION_PATTERN = Pattern.compile( "action=([\\d]+)" );
	public static final AdventureResult PUZZLE_PIECE = ItemPool.get( ItemPool.PUZZLE_PIECE, -1 );
	public static final AdventureResult HEDGE_KEY = ItemPool.get( ItemPool.HEDGE_KEY, 1 );

	private static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;
	private static final String[] DIRECTIONS = new String[] { "north", "east", "south", "west" };

	private static final String[] TILES = new String[]
	{
		"Upper-Left",
		"Upper-Middle",
		"Upper-Right",
		"Middle-Left",
		"Center",
		"Middle-Right",
		"Lower-Left",
		"Lower-Middle",
		"Lower-Right",
	};

	private static final Pattern[][] PATTERNS = new Pattern[][]
	{
		{
			Pattern.compile( "alt=\"Upper-Left Tile: (.*?)\"", Pattern.DOTALL ),
			Pattern.compile( "alt=\"Middle-Left Tile: (.*?)\"", Pattern.DOTALL ),
			Pattern.compile( "alt=\"Lower-Left Tile: (.*?)\"", Pattern.DOTALL ),
		},
		{
			Pattern.compile( "alt=\"Upper-Middle Tile: (.*?)\"", Pattern.DOTALL ),
			Pattern.compile( "alt=\"Center Tile: (.*?)\"", Pattern.DOTALL ),
			Pattern.compile( "alt=\"Lower-Middle Tile: (.*?)\"", Pattern.DOTALL ),
		},
		{
			Pattern.compile( "alt=\"Upper-Right Tile: (.*?)\"", Pattern.DOTALL ),
			Pattern.compile( "alt=\"Middle-Right Tile: (.*?)\"", Pattern.DOTALL ),
			Pattern.compile( "alt=\"Lower-Right Tile: (.*?)\"", Pattern.DOTALL ),
		}
	};

	private static final String[] EXITS = new String[]
	{
		"exit of the hedge maze is accessible when the Upper-Left",
		"exit of the hedge maze is accessible when the Upper-Middle",
		"exit of the hedge maze is accessible when the Upper-Right",
	};

	private static final String[] ENTRANCES = new String[]
	{
		"entrance to this hedge maze is accessible when the Lower-Left",
		"entrance to this hedge maze is accessible when the Lower-Middle",
		"entrance to this hedge maze is accessible when the Lower-Right",
	};

	// Current state of the puzzle

	private static int[][] interest = new int[ 3 ][ 2 ];
	private static boolean[][][][] exits = new boolean[ 3 ][ 3 ][ 4 ][ 4 ];

	public static String lastResponseText = null;

	public HedgePuzzleRequest()
	{
		super( "hedgepuzzle.php" );
	}

	public HedgePuzzleRequest( final int tile )
	{
		this();
		this.addFormField( "action", String.valueOf( tile ) );
	}

	public void processResults()
	{
		HedgePuzzleRequest.parseResponse( this.getURLString(), this.responseText );
		if ( HedgePuzzleRequest.PUZZLE_PIECE.getCount( KoLConstants.inventory ) == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of puzzle pieces." );
			return;
		}
	}

	public static final void parseResponse( final String location, final String responseText )
	{
		HedgePuzzleRequest.lastResponseText = responseText;

		// You don't have a hedge puzzle. 
		if ( responseText.indexOf( "You don't have a hedge puzzle." ) != -1 )
		{
			return;
		}

		// If the topiary golem stole one of your hedge puzzles, take it away.
		if ( responseText.indexOf( "Topiary Golem" ) != -1 )
		{
			ResultProcessor.processResult( HedgePuzzleRequest.PUZZLE_PIECE );
			return;
		}

		// Otherwise, look at the puzzle and save its configuration
		HedgePuzzleRequest.parseConfiguration( responseText );
		HedgePuzzleRequest.generateMazeConfigurations();
	}

	private static final void parseConfiguration( final String responseText )
	{
		for ( int x = 0; x < 3; ++x )
		{
			if ( responseText.indexOf( ENTRANCES[ x ] ) != -1 )
			{
				interest[ 0 ][ 0 ] = x;
				interest[ 0 ][ 1 ] = 2;
			}
			else if ( responseText.indexOf( EXITS[ x ]  ) != -1 )
			{
				interest[ 2 ][ 0 ] = x;
				interest[ 2 ][ 1 ] = -1;
			}
		}

		for ( int x = 0; x < 3; ++x )
		{
			for ( int y = 0; y < 3; ++y )
			{
				Matcher squareMatcher = PATTERNS[ x ][ y ].matcher( responseText );

				if ( !squareMatcher.find() )
				{
					// This should not happen
					continue;
				}

				String squareData = squareMatcher.group( 1 );

				for ( int i = 0; i < HedgePuzzleRequest.DIRECTIONS.length; ++i )
				{
					exits[ x ][ y ][ 0 ][ i ] = squareData.indexOf( HedgePuzzleRequest.DIRECTIONS[ i ] ) != -1;
				}

				if ( squareData.indexOf( "key" ) != -1 )
				{
					interest[ 1 ][ 0 ] = x;
					interest[ 1 ][ 1 ] = y;
				}
			}
		}
	}

	private static final void generateMazeConfigurations()
	{
		for ( int x = 0; x < 3; ++x )
		{
			for ( int y = 0; y < 3; ++y )
			{
				for ( int config = 3; config >= 0; --config ) // For all possible maze configurations
				{
					for ( int direction = 0; direction < 4; ++direction )
					{
						exits[ x ][ y ][ config ][ ( direction + config ) % 4 ] = exits[ x ][ y ][ 0 ][ direction ];
					}

					boolean allowConfig = true;

					for ( int direction = 0; direction < 4; ++direction )
					{
						if ( exits[ x ][ y ][ config ][ direction ] &&
						     !HedgePuzzleRequest.isExitPermitted( direction, x, y ) )
						{
							allowConfig = false;
							break;
						}
					}

					if ( !allowConfig )
					{
						for ( int direction = 0; direction < 4; ++direction )
						{
							exits[ x ][ y ][ config ][ direction ] = false;
						}
					}
				}
			}
		}
	}

	private static final boolean isExitPermitted( final int direction, final int x, final int y )
	{
		switch ( direction )
		{
		case NORTH:
			return y > 0 || x == interest[ 2 ][ 0 ];
		case EAST:
			return x < 2;
		case SOUTH:
			return y < 2 || x == interest[ 0 ][ 0 ];
		case WEST:
			return x > 0;
		default:
			return false;
		}
	}

	public static final void completeHedgeMaze( boolean visit )
	{
		// Check state relative to the hedge maze, and begin!

		if ( visit )
		{
			HedgePuzzleRequest.initializeMaze();
		}

		HedgePuzzleRequest.completeHedgeMaze();

		RelayRequest.specialCommandResponse = HedgePuzzleRequest.lastResponseText;
		RelayRequest.specialCommandIsAdventure = true;
		HedgePuzzleRequest.lastResponseText = null;
	}

	private static final void completeHedgeMaze()
	{
		// If we couldn't look at a puzzle, we canceled.

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// First mission -- retrieve the key from the hedge maze

		HedgePuzzleRequest.retrieveHedgeKey();

		// Retrieving the key after rotating the puzzle pieces uses an
		// adventure. If we ran out of puzzle pieces or adventures, we
		// canceled.

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Look at the puzzle again if we just retrieved the key

		HedgePuzzleRequest.initializeMaze();

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Second mission -- rotate the hedge maze until the hedge path
		// leads to the hedge door.

		HedgePuzzleRequest.finalizeHedgeMaze();

		// Navigating up to the tower door after rotating the puzzle
		// pieces requires an adventure. If we ran out of puzzle pieces
		// or adventures, we canceled.
	}

	private static final void initializeMaze()
	{
		KoLmafia.updateDisplay( "Retrieving maze status..." );
		RequestThread.postRequest( HedgePuzzleRequest.PUZZLE_REQUEST );
	}

	private static final void retrieveHedgeKey()
	{
		// Check to see if the hedge maze has already been solved for
		// the key.

		if ( KoLConstants.inventory.contains( HedgePuzzleRequest.HEDGE_KEY ) ||
		     HedgePuzzleRequest.lastResponseText.indexOf( "There is a key here." ) == -1 )
		{
			return;
		}

		int[][] solution = HedgePuzzleRequest.computeSolution( interest[ 0 ], interest[ 1 ] );

		if ( solution == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to compute maze solution." );
			return;
		}

		HedgePuzzleRequest.executeSolution( "Retrieving hedge key...", solution );
	}

	private static final void finalizeHedgeMaze()
	{
		int[][] solution = HedgePuzzleRequest.computeSolution( interest[ 0 ], interest[ 2 ] );

		if ( solution == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unable to compute maze solution." );
			return;
		}

		HedgePuzzleRequest.executeSolution( "Executing final rotations...", solution );
	}

	private static final int[][] computeSolution( final int[] start, final int[] destination )
	{
		KoLmafia.updateDisplay( "Computing maze solution..." );

		boolean[][] visited = new boolean[ 3 ][ 3 ];
		int[][] currentSolution = new int[ 3 ][ 3 ];
		int[][] optimalSolution = new int[ 3 ][ 3 ];

		for ( int i = 0; i < 3; ++i )
		{
			for ( int j = 0; j < 3; ++j )
			{
				optimalSolution[ i ][ j ] = -1;
			}
		}

		HedgePuzzleRequest.computeSolution(
			visited, currentSolution, optimalSolution, exits,
			start[ 0 ], start[ 1 ],
			destination[ 0 ], destination[ 1 ],
			HedgePuzzleRequest.SOUTH );

		for ( int i = 0; i < 3; ++i )
		{
			for ( int j = 0; j < 3; ++j )
			{
				if ( optimalSolution[ i ][ j ] == -1 )
				{
					return null;
				}
			}
		}

		return optimalSolution;
	}

	private static final void computeSolution( final boolean[][] visited, final int[][] currentSolution,
		final int[][] optimalSolution, final boolean[][][][] exits, final int currentX, final int currentY,
		final int destinationX, final int destinationY, final int incomingDirection )
	{
		// If the destination has already been reached, replace the
		// optimum value, if this involves fewer rotations.

		if ( currentX == destinationX && currentY == destinationY )
		{
			// First, determine the minimum number of spins needed
			// for the destination square.

			if ( currentY != -1 )
			{
				for ( int i = 0; i < 4; ++i )
				{
					if ( exits[ currentX ][ currentY ][ i ][ incomingDirection ] )
					{
						currentSolution[ currentX ][ currentY ] = i;
						break;
					}
				}
			}

			int currentSum = 0;
			for ( int i = 0; i < 3; ++i )
			{
				for ( int j = 0; j < 3; ++j )
				{
					if ( visited[ i ][ j ] )
					{
						currentSum += currentSolution[ i ][ j ];
					}
				}
			}

			int optimalSum = 0;
			for ( int i = 0; i < 3; ++i )
			{
				for ( int j = 0; j < 3; ++j )
				{
					optimalSum += optimalSolution[ i ][ j ];
				}
			}

			if ( optimalSum >= 0 && currentSum > optimalSum )
			{
				return;
			}

			if ( currentY != -1 )
			{
				visited[ currentX ][ currentY ] = true;
			}

			for ( int i = 0; i < 3; ++i )
			{
				for ( int j = 0; j < 3; ++j )
				{
					optimalSolution[ i ][ j ] = visited[ i ][ j ] ? currentSolution[ i ][ j ] : 0;
				}
			}

			if ( currentY != -1 )
			{
				visited[ currentX ][ currentY ] = false;
			}

			return;
		}

		if ( currentY == -1 || visited[ currentX ][ currentY ] )
		{
			return;
		}

		int nextX = -1, nextY = -1;
		visited[ currentX ][ currentY ] = true;

		for ( int config = 0; config < 4; ++config )
		{
			if ( !exits[ currentX ][ currentY ][ config ][ incomingDirection ] )
			{
				continue;
			}

			for ( int i = 0; i < 4; ++i )
			{
				if ( i == incomingDirection || !exits[ currentX ][ currentY ][ config ][ i ] )
				{
					continue;
				}

				currentSolution[ currentX ][ currentY ] = config;
				switch ( i )
				{
				case NORTH:
					nextX = currentX;
					nextY = currentY - 1;
					break;
				case EAST:
					nextX = currentX + 1;
					nextY = currentY;
					break;
				case SOUTH:
					nextX = currentX;
					nextY = currentY + 1;
					break;
				case WEST:
					nextX = currentX - 1;
					nextY = currentY;
					break;
				}

				HedgePuzzleRequest.computeSolution(
					visited, currentSolution, optimalSolution, exits, nextX, nextY, destinationX, destinationY,
					i > 1 ? i - 2 : i + 2 );
			}
		}

		visited[ currentX ][ currentY ] = false;
	}

	private static final void executeSolution( final String message, int [][] solution )
	{
		KoLmafia.updateDisplay( message );

		for ( int x = 0; x < 3 && KoLmafia.permitsContinue(); ++x )
		{
			for ( int y = 0; y < 3 && KoLmafia.permitsContinue(); ++y )
			{
				HedgePuzzleRequest.rotateHedgePiece( x, y, solution[ x ][ y ] );
			}
		}

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// The hedge maze has been properly rotated! Visit the maze again.

		RequestThread.postRequest( HedgePuzzleRequest.HEDGE_REQUEST );
		HedgePuzzleRequest.lastResponseText = HedgePuzzleRequest.HEDGE_REQUEST.responseText;

		if ( HedgePuzzleRequest.lastResponseText.indexOf( "You're out of adventures." ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of adventures." );
		}

	}

	private static final void rotateHedgePiece( final int x, final int y, final int rotations )
	{
		String action = String.valueOf( 1 + y * 3 + x );
		HedgePuzzleRequest.ROTATE_REQUEST.addFormField( "action", action );

		for ( int i = 0; i < rotations && KoLmafia.permitsContinue(); ++i )
		{
			// We're out of puzzles unless the response says:
			// "Click one of the puzzle sections to rotate that
			// section 90 degrees to the right."

			if ( HedgePuzzleRequest.lastResponseText.indexOf( "Click one" ) == -1 )
			{
				return;
			}

			RequestThread.postRequest( HedgePuzzleRequest.ROTATE_REQUEST );
		}
	}

	public static final void decorate( final StringBuffer buffer )
	{
		if ( buffer.indexOf( "You don't have a hedge puzzle" ) != -1 )
		{
			return;
		}

		String search = "<p><center><a href=\"inventory.php\">";
		int index = buffer.indexOf( search );
		if ( index == -1 )
		{
			return;
		}
		index += 11;

		// Build a "Solve!" button
		StringBuffer button = new StringBuffer();

		button.append( "<form name=solveform action='" );
		button.append( "/KoLmafia/specialCommand?cmd=hedge+solve&pwd=" );
		button.append( GenericRequest.passwordHash );
		button.append( "' method=post>" );
		button.append( "<input class=button type=submit value=\"Solve!\">" );
		button.append( "</form>" );
		button.append( "<p>" );

		// Insert it into the page
		buffer.insert( index, button );
	}

	private static int getTile( final String urlString )
	{
		Matcher matcher = HedgePuzzleRequest.ACTION_PATTERN.matcher( urlString );
		return matcher.find() ? StringUtilities.parseInt( matcher.group( 1 ) ) : 0;
	}

	public static String getTileName( final String urlString )
	{
		int tile = HedgePuzzleRequest.getTile ( urlString );
		return	( tile < 1 || tile > 9 ) ? null : TILES[ tile - 1 ];
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "hedgepuzzle.php" ) )
		{
			return false;
		}

		String tile = HedgePuzzleRequest.getTileName( urlString );
		if ( tile == null )
		{
			return true;
		}

		String message = "Rotate the " + tile + " tile of the hedge maze puzzle";;

		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
