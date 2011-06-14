/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SpaaaceRequest
	extends GenericRequest
{
	public static final AdventureResult ISOTOPE = ItemPool.get( ItemPool.LUNAR_ISOTOPE, -1 );

	public SpaaaceRequest()
	{
		super( "spaaace.php" );
	}

	// <input type="radio" name="whichitem" value="5156" /></td><td><img style='vertical-align: middle' class=hand src='http://images.kingdomofloathing.com/itemimages/pl_alielf.gif' onclick='descitem(655683821)'></td><td><span onclick="descitem(655683821)" style="font-weight: bold;">plush alielf</span>&nbsp;&nbsp;</td><td>100 lunar isotopes</td>

	private static final Pattern ITEM_PATTERN = Pattern.compile( "name=\"whichitem\" value=\"([\\d]+)\".*?descitem\\(([\\d]+)\\).*?<span.*?>([^<]*)</span>.*?([\\d,]+) lunar isotopes</td>", Pattern.DOTALL );

	public static void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "spaaace.php" ) )
		{
			return;
		}

		if ( urlString.indexOf( "place=shop" ) != -1 )
		{
			// Learn new items by simply visiting a Spaaace shop
			Matcher matcher = ITEM_PATTERN.matcher( responseText );
			while ( matcher.find() )
			{
				int id = StringUtilities.parseInt( matcher.group(1) );
				String desc = matcher.group(2);
				String name = matcher.group(3);
				String data = ItemDatabase.getItemDataName( id );
				// String price = matcher.group(4);
				if ( data == null || !data.equals( name ) )
				{
					ItemDatabase.registerItem( id, name, desc );
				}
			}

			// CoinMasterRequest.parseSpaaaceVisit( urlString, responseText );
			return;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action == null )
		{
			return;
		}
	}

	// title="peg style 3"
	private static final Pattern PEG_PATTERN = Pattern.compile( "title=\"peg style ([123])\"" );

	public static final String parseGameBoard( final String responseText )
	{
		StringBuffer buffer = new StringBuffer();
		Matcher matcher = PEG_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			buffer.append( matcher.group(1) );
		}

		return buffer.toString();
	}

	// <div class="blank">x1</div>
	private static final Pattern PAYOUT_PATTERN = Pattern.compile( "<div class=\"blank\">x(\\d)</div>" );

	public static final String parseGamePayouts( final String responseText )
	{
		StringBuffer buffer = new StringBuffer();
		Matcher matcher = PAYOUT_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			buffer.append( matcher.group(1) );
		}

		return buffer.toString();
	}

	public static final boolean validBoard( final String board, final String payouts )
	{
		// There must be 8 * ( 9 + 8 ) = 136 pegs
		// There must be 9 payouts
		return ( board.length() == ( 8 * (9 + 8 ) ) ) && ( payouts.length() == 9 );
	}

	private static float [][] matrix = null;
	private static float [] expected = null;

	public static final void initializeGameBoard()
	{
		Preferences.setString( "lastPorkoBoard", "" );
		Preferences.setString( "lastPorkoPayouts", "" );
		Preferences.setString( "lastPorkoExpected", "" );
		SpaaaceRequest.matrix = null;
		SpaaaceRequest.expected = null;
	}

	public static final void loadGameBoard( final String board, final String payouts)
	{
		// Store the 16 rows of pegs in the matrix.
		// Even numbered rows have 9 pegs in columns 0, 2, ... 16
		// Odd numbered rows have 8 pegs in columns 1, 3, ... 15

		Preferences.setString( "lastPorkoBoard", board );
		Preferences.setString( "lastPorkoPayouts", payouts );

		// Make a matrix if we don't already have one
		if ( SpaaaceRequest.matrix == null )
		{
			SpaaaceRequest.matrix = new float[18][17];
		}

		// Make expected value array if we don't already have one
		if ( SpaaaceRequest.expected == null )
		{
			SpaaaceRequest.expected = new float[9];
		}

		// Store the pegs
		int index = 0;
		for ( int row = 0, off = 0; row < 16; ++row, off = 1 - off )
		{
			for ( int col = off; col < 17; col += 2 )
			{
				int peg = Character.getNumericValue( board.charAt( index++ ) );
				SpaaaceRequest.matrix[ row][ col ] = (float) peg;
			}
		}

		// Mark unreachable cells
		SpaaaceRequest.calculateUnreachableSquares();

		// Store the payouts
		for ( int col = 0; col < 17; col += 2 )
		{
			// Some payouts are unreachable.
			float payout = SpaaaceRequest.matrix[ 15 ][ col ] == -1 ? -1  :
				(float) Character.getNumericValue( payouts.charAt( col / 2 ) );
			SpaaaceRequest.matrix[ 16 ][ col ] = payout;
			SpaaaceRequest.matrix[ 17 ][ col ] = payout;
		}
	}

	public static final void calculateUnreachableSquares()
	{
		// Algorithm courtesy of clump

		boolean [] reach = new boolean[ 17 ];
		Arrays.fill( reach, true );

		// Work your way down from the top
		for ( int row = 0, off = 0; row < 16; ++row )
		{
			boolean [] reach2 = new boolean[ 17 ];
			Arrays.fill( reach2, false );

			// Examine each peg in the row
			for ( int col = off; col < 17; col += 2 )
			{
				// If we can't get to this peg, skip it
				if ( !reach[ col ] )
				{
					continue;
				}

				float peg = SpaaaceRequest.matrix[ row ][ col ];
				if ( peg == 1.0f )
				{
					if ( col < 16 )
					{
						reach2[ col + 1 ] = true;
					}
					else
					{
						reach2[ col - 1 ] = true;
					}
				}
				else if ( peg == 2.0f )
				{
					if ( col > 0 )
					{
						reach2[ col - 1 ] = true;
					}
					else
					{
						reach2[ col + 1 ] = true;
					}
				}
				else if ( peg == 3.0f )
				{
					if ( col > 0 )
					{
						reach2[ col - 1 ] = true;
					}
					if ( col < 16 )
					{
						reach2[ col + 1 ] = true;
					}
				}
			}

			off = 1 - off;
			reach = reach2;

			for ( int col = off; col < 17; col += 2 )
			{
				if ( !reach[ col ] )
				{
					SpaaaceRequest.matrix[ row ][ col ] = -1.0f;
				}
			}
		}
	}

	public static final void solveGameBoard()
	{
		// According to Greycat on the Wiki: "Peg style 1 goes right,
		// peg style 2 goes left, and peg style 3 is random"

		// We can figure out the expected value for each starting
		// position by calculating expected value for each peg by
		// working up from the bottom row to the top row.
		//
		// The payouts are known for exit slots.
		// For each row from bottom to top
		//   For each peg in row
		//     if the peg is on the left wall or always goes right
		//	 value is right slot of row below
		//     else if the peg is on the right wall or always goes left
		//	 value is left slot of row below
		//     else if the peg goes randomly left or right
		//	 value is average of left and right slots
		//
		// The values of the pegs in the top row is what we need.

		// Iterate up from the bottom of the board calculating payout
		for ( int row = 15, off = 1; row >= 0; --row, off = 1 - off )
		{
			for ( int col = off; col < 17; col += 2 )
			{
				// If this cell is unreachable, skip it
				if ( row > 0 && SpaaaceRequest.matrix[ row - 1 ][ col ] == -1.0 )
				{
					SpaaaceRequest.matrix[ row ][ col ] = -1.0f;
					continue;
				}

				// If we are at the left edge, we must go down and right
				if ( col == 0 )
				{
					SpaaaceRequest.matrix[ row][ col ] = SpaaaceRequest.matrix[ row + 1 ][ 1 ];
					continue;
				}

				// If we are at the right edge, we must go down and left
				if ( col == 16 )
				{
					SpaaaceRequest.matrix[ row][ col ] = SpaaaceRequest.matrix[ row + 1 ][ 15 ];
					continue;
				}

				// Otherwise, what we do depends on peg type
				float peg = SpaaaceRequest.matrix[ row ][ col ];
				float value = -1.0f;

				// Style 1 pegs go right
				if ( peg == 1.0f )
				{
					value = SpaaaceRequest.matrix[ row + 1 ][ col + 1 ];
				}
				// Style 2 pegs go left
				else if ( peg == 2.0f )
				{
					value = SpaaaceRequest.matrix[ row + 1 ][ col - 1 ];
				}
				// Style 3 pegs randomly go right or left
				else if ( peg == 3.0f )
				{
					value = ( SpaaaceRequest.matrix[ row + 1 ][ col + 1 ] + SpaaaceRequest.matrix[ row + 1 ][ col - 1 ] ) / 2.0f;
				}

				// Replace this peg type with the expected value of hitting this peg
				SpaaaceRequest.matrix[ row ][ col ] = value;
			}
		}

		// Save the expected value for each slot in the top row
		StringBuffer buffer = new StringBuffer();
		for ( int i = 0; i < 9; ++i )
		{
			float val = SpaaaceRequest.matrix[ 0 ][ i * 2 ];
			if ( i > 0 )
			{
				buffer.append( ":" );
			}
			buffer.append( KoLConstants.FLOAT_FORMAT.format( val ) );
			SpaaaceRequest.expected[ i ] = val;
		}

		Preferences.setString( "lastPorkoExpected", buffer.toString() );
	}

	public static final void visitPorkoChoice( final String responseText )
	{
		// Called when we play Porko

		// You hand Juliedriel your isotope. She takes it with
		// a pair of tongs, and hands you three Porko chips
		if ( responseText.indexOf( "You hand Juliedriel your isotope" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.LUNAR_ISOTOPE, -1 );
		}

		// Initialize to defaults
		SpaaaceRequest.initializeGameBoard();

		// Parse the game board.
		String board = SpaaaceRequest.parseGameBoard( responseText );
		String payouts = SpaaaceRequest.parseGamePayouts( responseText );

		if ( !SpaaaceRequest.validBoard( board, payouts ) )
		{
			return;
		}

		// Load the board into internal data structures
		SpaaaceRequest.loadGameBoard( board, payouts );

		// Solve the Game Board
		SpaaaceRequest.solveGameBoard();
	}

	private static final Pattern PORKO_BOARD_PATTERN = Pattern.compile( "<div id=\"porko\">(.*?</div>)</div>", Pattern.DOTALL );

	public static final void decoratePorko( final StringBuffer buffer )
	{
		// Make sure we know the expected payouts
		if ( SpaaaceRequest.expected == null )
		{
			return;
		}

		// Make sure the player wants game hints
		if ( !Preferences.getBoolean( "arcadeGameHints" ) )
		{
			return;
		}

		// clump's Greasemonkey Porko Solver script is at:
		// 
		//	http://userscripts.org/scripts/source/104593.user.js
		// 
		// Veracity to clump on June 12, 2011
		//
		// "Would you be willing to let me incorporate your script's
		// look and feel into KoLmafia? Not your code - I have my own,
		// which is Java, rather than Java Script - but the way you
		// decorate the game board. I like it."
		//
		// clump to Veracity on June 12, 2011
		//
		// "Yes, by all means, take any or all of it for whatever
		// purpose you want! I'm glad you like it and am delighted to
		// see anything I make propagate :)"

		// Make the best starting slot be a green arrow
		// Make the expected payouts be the hover text

		Matcher matcher = PORKO_BOARD_PATTERN.matcher( buffer );
		if ( matcher.find() )
		{
			String board = matcher.group( 1 );
			int start = matcher.start( 1 );
			int end = matcher.end( 1 );
			String newBoard = SpaaaceRequest.decoratePorkoBoard( board );

			buffer.replace( start, end, newBoard );
		}
	}

	private static final String IMAGE_ROOT = "http://images.kingdomofloathing.com/itemimages/";
	private static final String LOCAL_ROOT = "/images/itemimages/";

	private static final String ARROW = IMAGE_ROOT + "porko_arrowa.gif";
	private static final String GREEN_ARROW = LOCAL_ROOT + "porko_green_arrowa.gif";

	private static final Pattern DIV_PATTERN = Pattern.compile( "<div.*?class=\"(.*?)\".*?</div>", Pattern.DOTALL );

	private static final String decoratePorkoBoard( final String board )
	{
		StringBuffer buffer = new StringBuffer();

		// Calculate the best expected yield
		float best = 0.0f;
		for ( int i = 0; i < 9; ++i )
		{
			best = Math.max( best, SpaaaceRequest.expected[ i ] );
		}
			
		// Iterate over all the divs in the board.
		// Copy into buffer, decorating to taste

		Matcher matcher = DIV_PATTERN.matcher( board );
		int col = 0;
		int row = 0;
		while ( matcher.find() )
		{
			String div = matcher.group( 0 );
			String type = matcher.group( 1 );

			if ( type.equals( "chip" ) )
			{
				buffer.append( div );
				continue;
			}

			// Get the expected payout for this cell
			float newVal = 0.0f;

			if ( col > 0 && col < 18 )
			{
				newVal = SpaaaceRequest.matrix[ row ][ col - 1 ];
			}

			if ( ++col == 19 )
			{
				col = 0;
				row += 1;
			}

			if ( type.equals( "start" ) )
			{
				// Use green arrow for "best" starting slots 
				if ( newVal == best )
				{
					div = StringUtilities.singleStringReplace( div, ARROW, GREEN_ARROW );
				}

				String search = "Start Here";
				String replace = KoLConstants.FLOAT_FORMAT.format( newVal );
				div = StringUtilities.globalStringReplace( div, search, replace );
			}

			if ( type.equals( "blank" ) )
			{
				// Cells also get annotated
				String search = "class=\"blank\"";
				String color = "";
				String title = "";
				if ( newVal < 0 )
				{
					color = " style=\"background: #EEEEEE\"";
					title = " title=\"Unreachable\"";
				}
				else
				{
					title = " title=\"" + KoLConstants.FLOAT_FORMAT.format( newVal ) + "\"";
				}
				String replace = search + color + title;
				div = StringUtilities.globalStringReplace( div, search, replace );
			}

			buffer.append( div );
		}

		return buffer.toString();
	}

	public static final void visitGeneratorChoice( final String responseText )
	{
		// Called when we visit the Big-Time Generator

		// Initialize to defaults
		SpaaaceRequest.initializeGameBoard();

		// Parse the game board.
		String board = SpaaaceRequest.parseGameBoard( responseText );
		String payouts = "000010000";

		if ( !SpaaaceRequest.validBoard( board, payouts ) )
		{
			return;
		}

		// Load the board into internal data structures
		SpaaaceRequest.loadGameBoard( board, payouts );

		// Solve the Game Board
		SpaaaceRequest.solveGameBoard();
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "spaaace.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		if ( action == null )
		{
			if ( urlString.indexOf( "place=shop1" ) != -1 )
			{
				message = "Visiting The Isotope Smithery";
			}
			else if ( urlString.indexOf( "place=shop2" ) != -1 )
			{
				message = "Visiting Dollhawker's Emporium";
			}
			else if ( urlString.indexOf( "place=shop3" ) != -1 )
			{
				message = "Visiting The Lunar Lunch-o-Mat";
			}
			else if ( urlString.indexOf( "place=porko" ) != -1 )
			{
				message = "Visiting The Porko Palace";
			}
			else if ( urlString.indexOf( "place=grimace" ) != -1 )
			{
				return true;
			}
			else if ( urlString.indexOf( "arrive=1" ) != -1 )
			{
				return true;
			}
		}
		else if ( action.equals( "playporko" ) )
		{
			if ( ISOTOPE.getCount( KoLConstants.inventory ) <= 0 )
			{
				return true;
			}
			message = "[" + KoLAdventure.getAdventureCount() + "] Porko Game";
		}
		else if ( action.equals( "buy" ) )
		{
			// Let CoinmasterRequest claim this
			return false;
		}

		if ( message == null )
		{
			return false;
		}

		RequestLogger.printLine( "" );
		RequestLogger.printLine( message );

		RequestLogger.updateSessionLog();
		RequestLogger.updateSessionLog( message );

		return true;
	}
}
