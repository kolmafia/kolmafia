/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.VolcanoMazeRequest;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class VolcanoMazeManager
{
	private static boolean loaded = false;

	// The number of maps in the cycle
	public static final int MAPS = 5;
	private static VolcanoMap [] maps = new VolcanoMap[ MAPS ];

	// Which map we are currently on
	private static int currentMap = 0;

	// Where you are on that map
	private static int currentLocation = -1;

	// Constants dictated by size of puzzle
	public static final int NROWS = 13;
	public static final int NCOLS = 13;
	private static final int CELLS = NCOLS * NROWS;
	private static final int MIN_SQUARE = 0;
	private static final int MAX_SQUARE = CELLS - 1;

	// An array, indexed by position, of map # on which this position is
	// above the lava.
	private static final int[] squares = new int[ CELLS ];
	private static final Neighbors[] neighbors = new Neighbors[ CELLS ];

	// The number of know platforms. After MAPS maps are known, this had
	// better be all of them.
	private static int found = 1;		// Goal known

	// Position of the start: (6,12)
	private static final int start = 162;

	// Position of the goal: (6,6)
	private static final int goal = 84;

	private static final String[] IMAGES = new String[]
	{
		"platformupyou.gif",
		"platformgoal.gif",
		"platform3.gif",
		"lava1.gif",
		"lava2.gif",
		"lava3.gif",
		"lava4.gif",
		"lava5.gif",
		"lava6.gif",
		"lava7.gif",
		"lava8.gif",
		"lava9.gif",
		"lava10.gif",
		"lava11.gif",
		"lava12.gif",
	};

	static
	{
		for ( int i = 0; i < VolcanoMazeManager.IMAGES.length; ++i )
		{
			FileUtilities.downloadImage( "http://images.kingdomofloathing.com/itemimages/" + VolcanoMazeManager.IMAGES[i] );
		}
	}

	public static final void reset()
	{
		VolcanoMazeManager.loaded = false;
		VolcanoMazeManager.currentMap = 0;
		VolcanoMazeManager.currentLocation = -1;
		for ( int map = 0; map < VolcanoMazeManager.maps.length; ++map )
		{
			VolcanoMazeManager.maps[ map ] = null;
		}
		Arrays.fill( VolcanoMazeManager.squares, 0 );
		Arrays.fill( VolcanoMazeManager.neighbors, null );
		VolcanoMazeManager.found = 1;
	}

	public static final void clear()
	{
		VolcanoMazeManager.reset();
		for ( int map = 0; map < maps.length; ++map )
		{
			VolcanoMazeManager.clearCurrentMap( map );
		}
	}

	private static final void loadCurrentMaps()
	{
		if ( !VolcanoMazeManager.loaded )
		{
			NemesisManager.ensureUpdatedNemesisStatus();
			for ( int map = 0; map < maps.length; ++map )
			{
				VolcanoMazeManager.loadCurrentMap( map );
			}
			RequestLogger.printLine( VolcanoMazeManager.found + " total platforms seen." );
			VolcanoMazeManager.currentMap = 0;
			VolcanoMazeManager.currentLocation = -1;
			VolcanoMazeManager.loaded = true;
		}
	}

	private static final void loadCurrentMap( final int map )
	{
		String setting = "volcanoMaze" + String.valueOf( map + 1 );
		String coords = Preferences.getString( setting );
		if ( !VolcanoMazeManager.validMap( coords ) )
		{
			Preferences.setString( setting, "" );
			VolcanoMazeManager.maps[ map ] = null;
		}
		else
		{
			VolcanoMazeManager.maps[ map ] = new VolcanoMap( coords );
			VolcanoMazeManager.addSquares( map );
		}
		
	}

	private static final void clearCurrentMap( final int map )
	{
		String setting = "volcanoMaze" + String.valueOf( map + 1 );
		Preferences.setString( setting, "" );
	}

	private static final boolean validMap( final String coordinates )
	{
		if ( coordinates == null || coordinates.equals( "" ) )
		{
			return false;
		}

		String[] platforms = coordinates.split( "\\s*,\\s*" );
		for ( int i = 0; i < platforms.length; ++i )
		{
			String coord = platforms[ i];
			if ( !StringUtilities.isNumeric( coord ) )
			{
				return false;
			}
			int val = StringUtilities.parseInt( coord );
			if ( val < MIN_SQUARE || val > MAX_SQUARE )
			{
				return false;
			}
		}
		return true;
	}

	public static final void decorate( final String location, final StringBuffer buffer )
	{
		if ( !location.startsWith( "volcanomaze.php" ) )
		{
			return;
		}

		// Add a "Solve!" button to the Volcanic Cave which invokes the
		// "volcano solve" command.

		String search = "</form>";
		int index = buffer.lastIndexOf( search );
		if ( index == -1 )
		{
			return;
		}
		index += 7;

		// Build a "Solve!" button
		StringBuffer button = new StringBuffer();

		String url = "/KoLmafia/redirectedCommand?cmd=volcano+solve&pwd=" + GenericRequest.passwordHash;
		button.append( "<form name=solveform action='" + url + "' method=post>" );
		button.append( "<input class=button type=submit value=\"Solve!\">" );
		button.append( "</form>" );

		// Insert it into the page
		buffer.insert( index, button );
	}

	public static final void parseResult( final String responseText )
	{
		// Load current maps, if necessary
		VolcanoMazeManager.loadCurrentMaps();

		// Parse what the server gave us
		String coords = VolcanoMazeManager.parseCoords( responseText );

		// Make sure we got a good map
		if ( !VolcanoMazeManager.validMap( coords ) )
		{
			return;
		}

		// See if we already have this map
		int index = VolcanoMazeManager.currentMap;
		do
		{
			VolcanoMap current = VolcanoMazeManager.maps[ index ];
			// Map not found and empty slot found
			if ( current == null )
			{
				VolcanoMazeManager.currentMap = index;
				VolcanoMazeManager.maps[ index ] = new VolcanoMap( coords );
				break;
			}

			// There is a map. Is it this map?
			if ( coords.equals( current.getCoordinates() ) )
			{
				VolcanoMazeManager.currentMap = index;
				return;
			}

			// No. Skip to next slot
			index = ( index + 1 ) % maps.length;
		} while ( index != VolcanoMazeManager.currentMap );

		// It's a new map. Save the coordinates in user settings
		int sequence = index + 1;
		String setting = "volcanoMaze" + String.valueOf( sequence );
		Preferences.setString( setting, coords );

		// Save the squares
		VolcanoMazeManager.addSquares( currentMap );
		RequestLogger.printLine( VolcanoMazeManager.found + " total platforms seen." );
	}

	private static final void addSquares( final int index )
	{
		VolcanoMap map = VolcanoMazeManager.maps[ index ];
		int seq = index + 1;
		Integer [] platforms = map.getPlatforms();
		int ofound = VolcanoMazeManager.found;
		int pcount = platforms.length;
		RequestLogger.printLine( "Map #" + seq + " has " + pcount + " platforms" );
		for ( int i = 0; i < pcount; ++i )
		{
			int square = platforms[ i ].intValue();
			int old = VolcanoMazeManager.squares[ square];
			if ( old == 0 )
			{
				VolcanoMazeManager.squares[ square ] = seq;
				VolcanoMazeManager.found++;
			}
			else if ( old != seq )
			{
				// Something is wrong: we already found this
				// square elsewhere in the sequence
				RequestLogger.printLine( "Platform " + square + " already seen on map #" + old );
			}
		}
	}

	private static final String parseCoords( final String responseText )
	{
		// move=x,y returns simply "false" if can't move there
		if ( responseText.equals( "false" ) )
		{
			return null;
		}
		else if ( responseText.startsWith( "<html>" ) )
		{
			return VolcanoMazeManager.parseHTMLCoords( responseText );
		}
		else
		{
			return VolcanoMazeManager.parseJSONCoords( responseText );
		}
	}

	private static final Pattern SQUARE_PATTERN = Pattern.compile("<div id=\"sq(\\d+)\" class=\"sq (no|yes)\\s+(you|goal|)\\s*lv(\\d+)\" rel=\"(\\d+),(\\d+)\">");

	private static final String parseHTMLCoords( final String responseText )
	{
		Matcher matcher = VolcanoMazeManager.SQUARE_PATTERN.matcher( responseText );
		StringBuffer buffer = new StringBuffer();
		boolean first = true;
		while ( matcher.find() )
		{
			String square = matcher.group(1);
			String special = matcher.group(3);
			if ( special != null )
			{
				int squint = Integer.parseInt( square );
				if ( special.equals( "you" ) )
				{
					VolcanoMazeManager.currentLocation = squint;
				}

				// Sanity check
				else if ( special.equals( "goal" ) && VolcanoMazeManager.goal != squint )
				{
					RequestLogger.printLine( "Map says goal is on square " + squint + ", not " + VolcanoMazeManager.goal );
				}
			}

			String type = matcher.group(2);
			if ( !type.equals( "yes" ) )
			{
				continue;
			}

			// int column = Integer.parseInt(matcher.group(5));
			// int row = Integer.parseInt(matcher.group(6));

			if ( first )
			{
				first = false;
			}
			else
			{
				buffer.append( "," );
			}
			buffer.append( square );
		}

		return buffer.toString();
	}

	// {"won":"","pos":"6,3","show":["2","6","9","15","20","24","32","34","38","40","47","49","52","56","57","59","64","84","86","92","97","100","105","106","109","111","114","117","127","129","136","144","145","150","153","160","167"]}

	private static final Pattern POS_PATTERN = Pattern.compile("(\\d+),(\\d+)");

	private static final String parseJSONCoords( final String responseText )
	{
		StringBuffer buffer = new StringBuffer();
		JSONObject JSON;

		// Parse the string into a JSON object
		try
		{
			JSON = new JSONObject( responseText );
		}
		catch ( JSONException e )
		{
			return "";
		}

		// "pos" is the player's position
		try
		{
			String pos = JSON.getString( "pos" );
			Matcher matcher = VolcanoMazeManager.POS_PATTERN.matcher( pos );
			if ( matcher.find() )
			{
				int col = Integer.parseInt( matcher.group( 1 ) );
				int row = Integer.parseInt( matcher.group( 2 ) );
				int square = row * NCOLS + col;
				VolcanoMazeManager.currentLocation = square;
			}
		}
		catch ( JSONException e )
		{
			VolcanoMazeManager.currentLocation = -1;
		}

		// "show" is an array of platforms
		JSONArray show;
		try
		{
			show = JSON.getJSONArray( "show" );
		}
		catch ( JSONException e )
		{
			return "";
		}

		// Iterate over the squares
		boolean first = true;
		int count = show.length();
		for ( int index = 0; index < count; ++index )
		{
			String square = show.optString( index, null );

			// Omit the goal square; that is a platform on all maps
			if ( square == null || square.equals( "84" ) )
			{
				continue;
			}

			if ( first )
			{
				first = false;
			}
			else
			{
				buffer.append( "," );
			}
			buffer.append( square );
		}

		return buffer.toString();
	}

	private static final int row( final int pos )
	{
		return ( pos / NCOLS );
	}

	private static final int col( final int pos )
	{
		return ( pos % NCOLS );
	}

	private static final int pos( final int row, final int col )
	{
		return ( row * NCOLS + col );
	}

	public static final String coordinateString( final int pos )
	{
		if ( pos == -1 )
		{
			return "(unknown)";
		}

		int row = row( pos );
		int col = col( pos );

		// Yes, KoL really does display ( column, row )
		return String.valueOf( col ) + "," + String.valueOf( row );
	}

	public static final String coordinateString( final int pos, final int map )
	{
		String cstr = VolcanoMazeManager.coordinateString( pos );
		String mstr = ( map >= 0 ) ? ( "map " + String.valueOf( map + 1 ) ) : "(unknown map )";
		return cstr + " on " + mstr;
	}

	public static final String currentCoordinates()
	{
		return VolcanoMazeManager.coordinateString( currentLocation );
	}

	public static final void printCurrentCoordinates()
	{
		if ( VolcanoMazeManager.currentLocation == -1 )
		{
			RequestLogger.printLine( "I don't know where you are" );
			return;
		}
		RequestLogger.printLine( "Current position: " + VolcanoMazeManager.coordinateString( currentLocation, currentMap ) );
	}

	private static final void discoverMaps()
	{
		VolcanoMazeManager.loadCurrentMaps();
		if ( VolcanoMazeManager.found == CELLS )
		{
			return;
		}

		// Visit the cave to find out where we are
		if ( currentLocation < 0 )
		{
			VolcanoMazeManager.internalVisit();
		}

		// Give up now if we couldn't do that.
		if ( currentLocation < 0 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You couldn't find the lava cave" );
			return;
		}

		VolcanoMazeManager.printCurrentCoordinates();

		while ( VolcanoMazeManager.found < CELLS )
		{
			VolcanoMap map = VolcanoMazeManager.maps[ currentMap ];
			int me = VolcanoMazeManager.currentLocation;
			int next = map.pickNeighbor( me );

			if ( next < 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You seem to be stuck" );
				return;
			}

			RequestLogger.printLine( "Move to: " + VolcanoMazeManager.coordinateString( next, currentMap ) );

			int ofound = VolcanoMazeManager.found;
			VolcanoMazeRequest req = new VolcanoMazeRequest( next );
			RequestThread.postRequest( req );

			if ( ofound >= VolcanoMazeManager.found )
			{
				// This shouldn't happen
				KoLmafia.updateDisplay( MafiaState.ERROR, "Moving did not discover new platforms" );
				return;
			}
		}
	}

	// CLI command support
	public static final void visit()
	{
		VolcanoMazeManager.internalVisit();
		VolcanoMazeManager.printCurrentCoordinates();
	}

	private static final void internalVisit()
	{
		// Must make a new VolcanoMazeRequest every time since that
		// class follows redirects.
		VolcanoMazeRequest VISITOR = new VolcanoMazeRequest();
		RequestThread.postRequest( VISITOR );
	}

	public static final void jump()
	{
		// Must make a new VolcanoMazeRequest every time since that
		// class follows redirects.
		VolcanoMazeRequest JUMP = new VolcanoMazeRequest( true );
		RequestThread.postRequest( JUMP );
		VolcanoMazeManager.printCurrentCoordinates();
	}

	public static final void move( final int x, final int y, final boolean print )
	{
		VolcanoMazeRequest req = new VolcanoMazeRequest( x, y );
		RequestThread.postRequest( req );
		if ( print )
		{
			VolcanoMazeManager.displayMap();
		}
		VolcanoMazeManager.printCurrentCoordinates();
	}

	public static final void displayMap()
	{
		VolcanoMazeManager.loadCurrentMaps();
		VolcanoMap map = VolcanoMazeManager.maps[ VolcanoMazeManager.currentMap ];
		if ( map == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "We haven't seen the volcanic cave yet" );
			return;
		}

		map.displayHTMLMap( currentLocation );
	}

	public static final void displayMap( final int num )
	{
		if ( num < 1 || num > MAPS )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Choose map # from 1 - " + MAPS );
			return;
		}

		VolcanoMazeManager.loadCurrentMaps();
		VolcanoMap map = VolcanoMazeManager.maps[ num - 1 ];
		if ( map == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "We haven't seen map #" + num );
			return;
		}

		map.displayHTMLMap( -1 );
	}

	public static final void platforms()
	{
		VolcanoMazeManager.discoverMaps();
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Make an HTML table to display platform map
		StringBuffer buffer = new StringBuffer();

		buffer.append( "<table border cols=14>" );
		buffer.append( "<tr><td></td>" );
		for ( int col = 0; col < NCOLS; ++col )
		{
			buffer.append( "<td align=center><b>" );
			buffer.append( String.valueOf( col ) );
			buffer.append( "</b></td>" );
		}
		buffer.append( "</tr>" );
		for ( int row = 0; row < NROWS; ++row )
		{
			buffer.append( "<tr>" );
			buffer.append( "<td valign=center><b>" );
			buffer.append( String.valueOf( row ) );
			buffer.append( "</b></td>" );
			for ( int col = 0; col < NCOLS; ++col )
			{
				buffer.append( "<td>" );
				int map = squares[ pos( row, col ) ];
				buffer.append( String.valueOf( map ) );
				buffer.append( "</td>" );
			}
			buffer.append( "</tr>" );
		}
		buffer.append( "</table>" );
		RequestLogger.printLine( buffer.toString() );
		RequestLogger.printLine();
	}

	private static int pathsMade = 0;
	private static int pathsExamined = 0;

	public static final void solve()
	{
		// Save URL to give back to the user's browser
		RelayRequest.redirectedCommandURL = "/volcanomaze.php?start=1";

		VolcanoMazeManager.discoverMaps();
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		// Sanity check
		if ( VolcanoMazeManager.found < CELLS )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "We couldn't discover all the maps" );
			return;
		}

		Path solution = VolcanoMazeManager.solve( currentLocation, currentMap );
                VolcanoMazeManager.printStatistics( solution );

		if ( solution == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't get there from here. Swim to shore and try again." );
			return;
		}

		// Move up next to the goal.
		Iterator it = solution.iterator();
		while ( it.hasNext() )
		{
			Integer next = (Integer) it.next();
			int sq = next.intValue();

			// Quit when we are about to move to the goal
			if ( sq == VolcanoMazeManager.goal )
			{
				break;
			}

			VolcanoMazeRequest req = new VolcanoMazeRequest( sq );
			RequestThread.postRequest( req );
		}
	}

	public static final void test( final int map, final int x, final int y )
	{
		VolcanoMazeManager.loadCurrentMaps();

		// Sanity check
		if ( VolcanoMazeManager.found < CELLS )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't know all the maps" );
			return;
		}

		int location = pos( y, x );
		Path solution = VolcanoMazeManager.solve( location, map - 1 );
                VolcanoMazeManager.printStatistics( solution );

		if ( solution == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't get there from here. Swim to shore and try again." );
			return;
		}

		// Print the solution
		Iterator it = solution.iterator();
		while ( it.hasNext() )
		{
			Integer next = (Integer) it.next();
			int pos = next.intValue();
			RequestLogger.printLine( "Hop to " + VolcanoMazeManager.coordinateString( pos ) );
		}
	}

	private static final void printStatistics( final Path solution )
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append( "Paths examined/made " );
		buffer.append( KoLConstants.COMMA_FORMAT.format( pathsExamined ) );
		buffer.append( "/" );
		buffer.append( KoLConstants.COMMA_FORMAT.format( pathsMade ) );
		buffer.append( " ->" );
		if ( solution != null )
		{
			buffer.append( "solution with " );
			buffer.append( String.valueOf( solution.size() ) );
			buffer.append( " hops." );
		}
		else
		{
			buffer.append( "no solution found." );
		}
		RequestLogger.printLine( buffer.toString() );
	}

	// solve( currentLocation, currentMap ): solve the volcano cave puzzle
	//
	// Inputs:
	//
	// location - starting square
	// map - starting map
	//
	// Global constants:
	//
	// CELLS - number of cells in map: rows & columns
	// MAPS - number of maps in cycle.
	// goal - cell # of goal platform
	//
	// Global input data:
	//
	// VolcanoMap maps[ MAPS ]
	//    Indexed from 0 to MAP
	// int squares[ CELLS ]
	//    Indexed by platform #: map # containing platform
	//
	// Global input/output data:
	//
	// Neighbors neighbors[ CELLS ]
	//    Indexed by platform #: neighbors of platform in same map
	//
	// Global output data:
	//
	// pathsMade - paths generated
	// pathsExamined - paths examined

	private static final Path solve( final int location, final int map )
	{
		// Generate neighbors for every cell
		VolcanoMazeManager.generateNeighbors();

		// The work queue of Paths
		LinkedList queue = new LinkedList();

		// Statistics
		VolcanoMazeManager.pathsMade = 0;
		VolcanoMazeManager.pathsExamined = 0;

		// Find the neighbors for the current location in the current
		// map. These are the first hop for all possible paths.
		VolcanoMap current = VolcanoMazeManager.maps[ map ];
		Neighbors roots = current.neighbors( location );

		// We only need to visit any given cell once.
		boolean [] visited = new boolean[ CELLS ];

		// We have visited the start square
		visited[ location ] = true;

		// Make a path for each root and add it to the queue.
		Integer [] starts = roots.getPlatforms();
		for ( int i = 0; i < starts.length; ++i )
		{
			++VolcanoMazeManager.pathsMade;
			Integer square = starts[ i ];
			queue.addLast( new Path( square ) );
			// We (will) have visited each root
			visited[ square.intValue() ] = true;
		}

		// Perform a breadth-first search of the maze
		while ( !queue.isEmpty() )
		{
			Path path = (Path) queue.removeFirst();
			++VolcanoMazeManager.pathsExamined;
			// System.out.println( "Examining path: " + path );
			
			Integer last = path.getLast();
			Neighbors neighbors = VolcanoMazeManager.neighbors[ last.intValue() ];
			Integer [] platforms = neighbors.getPlatforms();

			// Examine each neighbor
			for ( int i = 0; i < platforms.length; ++i )
			{
				Integer platform = platforms[ i ];
				// If this is a goal, we have the solution
				if ( platform.intValue() == VolcanoMazeManager.goal )
				{
					++VolcanoMazeManager.pathsMade;
					return new Path( path, platform );
				}

				// If neighbor not yet seen, add and search it
				int square = platform.intValue();
				if ( !visited[ square ] )
				{
					++VolcanoMazeManager.pathsMade;
					queue.addLast( new Path( path, platform ) );
					// We (will) have visited this platform
					visited[ square ] = true;
				}
			}
		}

		// No solution found
		return null;
	}

	private static final void generateNeighbors()
	{
		for ( int square = 0; square < CELLS; ++square )
		{
			// Calculate and store neighbors once only
			if ( VolcanoMazeManager.neighbors[ square ] != null )
			{
				continue;
			}

			// The goal appears in every map
			if ( square == goal )
			{
				VolcanoMazeManager.neighbors[ square ] = new Neighbors( square, null );
				continue;
			}

			// Otherwise, get the neighbors relative to the map
			// the square is in.
			int index = VolcanoMazeManager.squares[ square ];
			VolcanoMap pmap = VolcanoMazeManager.maps[ index % MAPS ];
			VolcanoMazeManager.neighbors[ square ] = pmap.neighbors( square );
		}
	}

	private static class VolcanoMap
	{
		public final String coordinates;
		public final Integer [] platforms;
		public final boolean[] board = new boolean[ CELLS ];

		public VolcanoMap( final String coordinates )
		{
			this.coordinates = coordinates; 

			// Make an array of all the platforms
			String[] squares = coordinates.split( "\\s*,\\s*" );
			ArrayList list = new ArrayList();
			for ( int i = 0; i < squares.length; ++i )
			{
				String coord = squares[ i];
				if ( !StringUtilities.isNumeric( coord ) )
				{
					continue;
				}
				Integer ival = new Integer( coord );
				list.add( ival );
				this.board[ ival.intValue() ] = true;
			}
			this.platforms = (Integer []) list.toArray( new Integer[ list.size() ] );

			// Every board has the goal platform
			this.board[ VolcanoMazeManager.goal ] = true;
		}

		public String getCoordinates()
		{
			return this.coordinates;
		}

		public Integer [] getPlatforms()
		{
			return this.platforms;
		}

		public boolean [] getBoard()
		{
			return this.board;
		}

		public boolean inMap( final int row, final int col )
		{
			return this.inMap( pos( row, col ) );
		}

		public boolean inMap( final int square )
		{
			return this.board[ square ];
		}

		public Neighbors neighbors( final int square )
		{
			return new Neighbors( square, this );
		}

		public int pickNeighbor( final int square )
		{
			Neighbors neighbors = this.neighbors( square );
			Integer [] platforms = neighbors.getPlatforms();

			// We might be stuck
			if ( platforms.length == 0 )
			{
				return -1;
			}

			// If there is only one neighbor, that's it
			if ( platforms.length == 1 )
			{
				int next = platforms[ 0 ].intValue();
				// Don't pick the goal!
				return ( next != VolcanoMazeManager.goal ) ? next : -1;
			}

			// Otherwise, pick one at random.
			int next = VolcanoMazeManager.goal;
			while ( next == VolcanoMazeManager.goal )
			{
				int rnd = KoLConstants.RNG.nextInt( platforms.length );
				next = platforms[ rnd ].intValue();
			}
			return next;
		}

		public void print( final int player )
		{
			int prow = row( player );
			int pcol = col( player );
			StringBuffer buffer = new StringBuffer();
			for ( int row = 0; row < NROWS; ++row )
			{
				if ( row < 9 )
				{
					buffer.append( " " );
				}
				buffer.append( String.valueOf( row + 1 ) );
				for ( int col = 0; col < NCOLS; ++col )
				{
					buffer.append( " " );
					if ( row == prow && col == pcol )
					{
						buffer.append( "@" );
					}
					else if ( row == 6 && col == 6 )
					{
						buffer.append( "*" );
					}
					else if ( board[ pos( row, col ) ] )
					{
						buffer.append( "O" );
					}
					else
					{
						buffer.append( "." );
					}
				}
				buffer.append( KoLConstants.LINE_BREAK );
			}

			System.out.println( buffer.toString() );
		}

		public void displayHTMLMap( final int player )
		{
			int prow = row( player );
			int pcol = col( player );
			StringBuffer buffer = new StringBuffer();

			buffer.append( "<table cellpadding=0 cellspacing=0 cols=14>" );
			buffer.append( "<tr><td></td>" );
			for ( int col = 0; col < NCOLS; ++col )
			{
				buffer.append( "<td align=center><b>" );
				buffer.append( String.valueOf( col ) );
				buffer.append( "</b></td>" );
			}
			buffer.append( "</tr>" );
			for ( int row = 0; row < NROWS; ++row )
			{
				buffer.append( "<tr>" );
				buffer.append( "<td valign=center><b>" );
				buffer.append( String.valueOf( row ) );
				buffer.append( "</b></td>" );
				for ( int col = 0; col < NCOLS; ++col )
				{
					buffer.append( "<td>" );
					buffer.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/" );
					if ( row == prow && col == pcol )
					{
						buffer.append( "platformupyou" );
					}
					else if ( row == 6 && col == 6 )
					{
						buffer.append( "platformgoal" );
					}
					else if ( board[ pos( row, col ) ] )
					{
						buffer.append( "platform3" );
					}
					else
					{
						int rnd = KoLConstants.RNG.nextInt( 12 );
						buffer.append( "lava" );
						buffer.append( String.valueOf( rnd + 1 ) );
					}
					buffer.append( ".gif\" width=30 height=30>" );
					buffer.append( "</td>" );
				}
				buffer.append( "</tr>" );
			}
			buffer.append( "</table>" );
			RequestLogger.printLine( buffer.toString() );
			RequestLogger.printLine();
		}
	}

	private static class Neighbors
	{
		public final Integer [] platforms;

		public Neighbors( final int square, final VolcanoMap map )
		{
			int row = row( square );
			int col = col( square );

			ArrayList list = new ArrayList();
			Neighbors.addSquare( list, map, row - 1, col - 1 );
			Neighbors.addSquare( list, map, row - 1, col );
			Neighbors.addSquare( list, map, row - 1, col + 1 );
			Neighbors.addSquare( list, map, row, col - 1 );
			Neighbors.addSquare( list, map, row, col + 1 );
			Neighbors.addSquare( list, map, row + 1, col - 1 );
			Neighbors.addSquare( list, map, row + 1, col );
			Neighbors.addSquare( list, map, row + 1, col + 1 );

			this.platforms = new Integer[ list.size() ];
			list.toArray( this.platforms );
		}

		public Integer [] getPlatforms()
		{
			return this.platforms;
		}

		private static void addSquare( List list, final VolcanoMap map, int row, int col )
		{
			if ( row >= 0 && row < NROWS && col >= 0 && col < NCOLS	 )
			{
				int square = pos( row, col );
				if ( map == null || map.inMap( square ) )
				{
					list.add( new Integer( square ) );
				}
			}
		}
	}

	private static class Path
	{
		private final ArrayList list;

		public Path( final Integer square )
		{
			list = new ArrayList();
			list.add( square );
		}

		public Path( final Path prefix, final Integer square )
		{
			list = (ArrayList) prefix.list.clone();
			list.add( square );
		}

		public boolean contains( final Integer elem )
		{
			return list.contains( elem );
		}

		public Integer get( final int index )
		{
			return (Integer) list.get( index );
		}

		public Integer getLast()
		{
			return (Integer) list.get( list.size() - 1 );
		}

		public Iterator iterator()
		{
			return list.iterator();
		}

		public int size()
		{
			return list.size();
		}

		@Override
		public String toString()
		{
			StringBuffer buffer = new StringBuffer();
			int count = list.size();
			boolean first = true;
			buffer.append( "[" );
			for ( int i = 0; i < count; ++i )
			{
				if ( first )
				{
					first = false;
				}
				else
				{
					buffer.append( "," );
				}
				buffer.append( String.valueOf( list.get( i ) ) );
			}
			buffer.append( "]" );
			return buffer.toString();
		}
	}
}
