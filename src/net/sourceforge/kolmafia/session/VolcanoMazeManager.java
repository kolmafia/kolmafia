/**
 * Copyright (c) 2005-2009, KoLmafia development team
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.VolcanoMazeRequest;
import net.sourceforge.kolmafia.session.NemesisManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class VolcanoMazeManager
{
	private static boolean loaded = false;
	private static int currentMap = 0;
	private static int currentLocation = -1;
	private static VolcanoMap [] maps = new VolcanoMap[5];
	private static int[] squares = new int[ 13 * 13 ];
	private static int found = 1;		// Goal known
	private static int goal = -1;

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
		VolcanoMazeManager.found = 1;
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

	private static final boolean validMap( final String coordinates )
	{
		if ( coordinates == null || coordinates.equals( "" ) )
		{
			return false;
		}

		String[] squares = coordinates.split( "\\s*,\\s*" );
		for ( int i = 0; i < squares.length; ++i )
		{
			String coord = squares[ i];
			if ( !StringUtilities.isNumeric( coord ) )
			{
				return false;
			}
			int val = StringUtilities.parseInt( coord );
			if ( val < 0 || val > 168 )
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
	}

	private static final void addSquares( final int index )
	{
		VolcanoMap map = VolcanoMazeManager.maps[ index ];
		int sequence = index + 1;
		Integer [] platforms = map.getPlatforms();
		for ( int i = 0; i < platforms.length; ++i )
		{
			int square = platforms[ i ].intValue();
			int old = squares[ square ];
			if ( old == 0 )
			{
				VolcanoMazeManager.squares[ square ] = sequence;
				VolcanoMazeManager.found++;
			}
			else if ( old != sequence )
			{
				// Something is wrong: we already found this
				// square elsewhere in the sequence
			}
		}
	}

	private static final String parseCoords( final String responseText )
	{
		if ( responseText.startsWith( "<html>" ) )
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
				else if ( special.equals( "goal" ) )
				{
					VolcanoMazeManager.goal = squint;
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
				int square = row * 13 + col;
				VolcanoMazeManager.currentLocation = square;
			}
		}
		catch ( JSONException e )
		{
			VolcanoMazeManager.currentLocation = -1;
		}

		// (6,6) is the goal
		VolcanoMazeManager.goal = 84;

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

	public static final String currentCoordinates()
	{
		if ( VolcanoMazeManager.currentLocation == -1 )
		{
			return "(unknown)";
		}

		int row = VolcanoMazeManager.currentLocation / 13;
		int col = VolcanoMazeManager.currentLocation % 13;
		return String.valueOf( col ) + "," + String.valueOf( row );
	}

	public static final void printCurrentCoordinates()
	{
		if ( VolcanoMazeManager.currentLocation == -1 )
		{
			RequestLogger.printLine( "I don't know where you are" );
			return;
		}
		RequestLogger.printLine( "Current position: " +
					 VolcanoMazeManager.currentCoordinates() +
					 " on map " +
					 ( VolcanoMazeManager.currentMap + 1 ) );
	}

	// CLI command support

	public static final void visit()
	{
		VolcanoMazeRequest req = new VolcanoMazeRequest();
		RequestThread.postRequest( req );
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
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "We haven't seen the volcanic cave yet" );
			return;
		}

		map.displayHTMLMap( currentLocation );
	}

	public static final void displayMap( final int num )
	{
		if ( num < 1 || num > 5 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Choose map # from 1 - 5" );
			return;
		}

		VolcanoMazeManager.loadCurrentMaps();
		VolcanoMap map = VolcanoMazeManager.maps[ num - 1 ];
		if ( map == null )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "We haven't seen map #" + num );
			return;
		}

		map.displayHTMLMap( -1 );
	}

	public static class VolcanoMap
	{
		public final String coordinates;
		public final Integer [] platforms;
		public final boolean[][] board = new boolean[13][13];

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
				int y = ival.intValue() / 13;
				int x = ival.intValue() % 13;
				board[x][y] = true;
			}

			platforms = (Integer []) list.toArray( new Integer[ list.size() ] );
		}

		public String getCoordinates()
		{
			return this.coordinates;
		}

		public Integer [] getPlatforms()
		{
			return this.platforms;
		}

		public void print( final int player )
		{
			int py = player / 13;
			int px = player % 13;
			StringBuffer buffer = new StringBuffer();
			for ( int y = 0; y < 13; ++y )
			{
				int row = y + 1;
				if ( row < 10 )
				{
					buffer.append( " " );
				}
				buffer.append( String.valueOf( row ) );
				for ( int x = 0; x < 13; ++x )
				{
					buffer.append( " " );
					if ( x == px && y == py )
					{
						buffer.append( "@" );
					}
					else if ( x == 6 && y == 6 )
					{
						buffer.append( "*" );
					}
					else if ( board[x][y] )
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
			int py = player / 13;
			int px = player % 13;
			StringBuffer buffer = new StringBuffer();

			buffer.append( "<table cellpadding=0 cellspacing=0 cols=14>" );
			buffer.append( "<tr><td></td>" );
			for ( int x = 0; x < 13; ++x )
			{
				buffer.append( "<td align=center><b>" );
				buffer.append( String.valueOf( x ) );
				buffer.append( "</b></td>" );
			}
			buffer.append( "<tr>" );
			for ( int y = 0; y < 13; ++y )
			{
				buffer.append( "<td valign=center><b>" );
				buffer.append( String.valueOf( y ) );
				buffer.append( "</b></td>" );
				for ( int x = 0; x < 13; ++x )
				{
					buffer.append( "<td>" );
					buffer.append( "<img src=\"http://images.kingdomofloathing.com/itemimages/" );
					if ( x == px && y == py )
					{
						buffer.append( "platformupyou" );
					}
					else if ( x == 6 && y == 6 )
					{
						buffer.append( "platformgoal" );
					}
					else if ( board[x][y] )
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
}
