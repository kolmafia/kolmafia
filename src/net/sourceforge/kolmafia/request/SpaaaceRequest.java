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

import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ResultProcessor;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SpaaaceRequest
	extends GenericRequest
{
	public static final Pattern TOKEN_PATTERN = Pattern.compile( "You have ([\\d,]+) lunar isotope" );
	public static final AdventureResult ISOTOPE = ItemPool.get( ItemPool.LUNAR_ISOTOPE, 1 );
	public static final AdventureResult TRANSPONDER = ItemPool.get( ItemPool.TRANSPORTER_TRANSPONDER, 1 );
	public static final AdventureResult TRANSPONDENT = EffectPool.get( EffectPool.TRANSPONDENT, 1 );

	public static boolean isTranspondent = false;
	public static boolean hasTransponders = false;

	public static void update()
	{
		SpaaaceRequest.isTranspondent = KoLConstants.activeEffects.contains( SpaaaceRequest.TRANSPONDENT );
		SpaaaceRequest.hasTransponders = SpaaaceRequest.TRANSPONDER.getCount( KoLConstants.inventory ) > 0;
	}

	public static boolean immediatelyAccessible()
	{
		SpaaaceRequest.update();
		return SpaaaceRequest.isTranspondent;
	}

	public static String accessible()
	{
		SpaaaceRequest.update();
		if ( SpaaaceRequest.isTranspondent || SpaaaceRequest.hasTransponders )
		{
			return null;
		}
		return "You need a transporter transponder to go there.";
	}

	public static void equip()
	{
		SpaaaceRequest.update();
		if ( !SpaaaceRequest.isTranspondent && SpaaaceRequest.hasTransponders )
		{
			UseItemRequest request = UseItemRequest.getInstance( SpaaaceRequest.TRANSPONDER );
			RequestThread.postRequest( request );
		}
	}

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

		QuestDatabase.setQuestIfBetter( QuestDatabase.GENERATOR, QuestDatabase.STARTED );

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

			SpaaaceRequest.parseShopVisit( urlString, responseText );
			return;
		}
	}

	private static void parseShopVisit( final String location, final String responseText )
	{
		CoinmasterData data = SpaaaceRequest.findIsotopeMaster( location );
		if ( data == null )
		{
			return;
		}

		String action = GenericRequest.getAction( location );
		if ( action == null )
		{
			// Parse current coin balances
			CoinMasterRequest.parseBalance( data, responseText );
			return;
		}

		CoinMasterRequest.parseResponse( data, location, responseText );
	}

	private static final Pattern SHOP_PATTERN = Pattern.compile( "place=shop(\\d+)" );
	private static CoinmasterData findIsotopeMaster( final String urlString )
	{
		Matcher shopMatcher = SpaaaceRequest.SHOP_PATTERN.matcher( urlString );
		if ( !shopMatcher.find() )
		{
			return null;
		}

		String shop = shopMatcher.group(1);

		if ( shop.equals( "1" ) )
		{
			return IsotopeSmitheryRequest.ISOTOPE_SMITHERY;
		}

		if ( shop.equals( "2" ) )
		{
			return DollHawkerRequest.DOLLHAWKER;
		}

		if ( shop.equals( "3" ) )
		{
			return LunarLunchRequest.LUNAR_LUNCH;
		}

		return null;
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

	private static int [][] pegs = null;
	private static String [][] divs = null;
	private static float [] expected = null;
	private static String [] slots = null;

	public static final void initializeGameBoard()
	{
		Preferences.setString( "lastPorkoBoard", "" );
		Preferences.setString( "lastPorkoPayouts", "" );
		Preferences.setString( "lastPorkoExpected", "" );
		SpaaaceRequest.expected = null;
		SpaaaceRequest.pegs = null;
		SpaaaceRequest.divs = null;
		SpaaaceRequest.slots = null;
	}

	static final String UNREACHABLE_CELL =
		"<div class=\"blank\" style=\"background: #EEEEEE\" title=\"Unreachable\"> </div>";

	static final String [] UNREACHABLE_PAYOUTS = {
		"<div class=\"blank\" style=\"background: #EEEEEE\" title=\"Unreachable\">x0</div>",
		"<div class=\"blank\" style=\"background: #EEEEEE\" title=\"Unreachable\">x1</div>",
		"<div class=\"blank\" style=\"background: #EEEEEE\" title=\"Unreachable\">x2</div>",
		"<div class=\"blank\" style=\"background: #EEEEEE\" title=\"Unreachable\">x3</div>",
	};

	static final String [] PAYOUTS = {
		"<div class=\"blank\" style=\"background: LightYellow\" title=\"0\">x0</div>",
		"<div class=\"blank\" style=\"background: PeachPuff\" title=\"1\">x1</div>",
		"<div class=\"blank\" style=\"background: LightSalmon\" title=\"2\">x2</div>",
		"<div class=\"blank\" style=\"background: Tomato\" title=\"3\">x3</div>",
	};

	static final String [] DETERMINISTIC_DOWN = {
		"<div class=\"blank\" style=\"background: LightYellow\" title=\"0\">&darr;</div>",
		"<div class=\"blank\" style=\"background: PeachPuff\" title=\"1\">&darr;</div>",
		"<div class=\"blank\" style=\"background: LightSalmon\" title=\"2\">&darr;</div>",
		"<div class=\"blank\" style=\"background: Tomato\" title=\"3\">&darr;</div>",
	};

	static final String [] DETERMINISTIC_LEFT = {
		"<div class=\"blank\" style=\"background: LightYellow\" title=\"0\">&#8601;</div>",
		"<div class=\"blank\" style=\"background: PeachPuff\" title=\"1\">&#8601;</div>",
		"<div class=\"blank\" style=\"background: LightSalmon\" title=\"2\">&#8601;</div>",
		"<div class=\"blank\" style=\"background: Tomato\" title=\"3\">&#8601;</div>",
	};

	static final String [] DETERMINISTIC_RIGHT = {
		"<div class=\"blank\" style=\"background: LightYellow\" title=\"0\">&#8600;</div>",
		"<div class=\"blank\" style=\"background: PeachPuff\" title=\"1\">&#8600;</div>",
		"<div class=\"blank\" style=\"background: LightSalmon\" title=\"2\">&#8600;</div>",
		"<div class=\"blank\" style=\"background: Tomato\" title=\"3\">&#8600;</div>",
	};

	static final String [] DETERMINISTIC_RANDOM = {
		"<div class=\"blank\" style=\"background: LightYellow\" title=\"0\">.</div>",
		"<div class=\"blank\" style=\"background: PeachPuff\" title=\"1\">.</div>",
		"<div class=\"blank\" style=\"background: LightSalmon\" title=\"2\">.</div>",
		"<div class=\"blank\" style=\"background: Tomato\" title=\"3\">.</div>",
	};

	private static final String makeDiv( int peg, final int min, final int max, final float expected )
	{
		if ( min == max )
		{
			switch ( peg )
			{
			case LEFT:
				return DETERMINISTIC_LEFT[ min ];
			case RIGHT:
				return DETERMINISTIC_RIGHT[ min ];
			case RANDOM:
				return DETERMINISTIC_RANDOM[ min ];
			default:
				// Should not come here
				break;
			}
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append( "<div class=\"blank\" title=\"" );
		if ( min == max )
		{
			buffer.append( String.valueOf( min ) );
		}
		else
		{
			buffer.append( KoLConstants.FLOAT_FORMAT.format( expected ) );
			buffer.append( " (" );
			buffer.append( String.valueOf( min ) );
			buffer.append( "-" );
			buffer.append( String.valueOf( max ) );
			buffer.append( ")" );
		}
		buffer.append( "\">" );
		switch ( peg )
		{
		case LEFT:
			buffer.append( "&#8601;" );
			break;
		case RIGHT:
			buffer.append( "&#8600;" );
			break;
		case RANDOM:
			buffer.append( "." );
			break;
		default:
			// Should not come here
			buffer.append( " " );
			break;
		}
		buffer.append( "</div>" );
		return buffer.toString();
	}

	private static final String makeSlot( final int min, final int max, final float expected )
	{
		if ( min == max )
		{
			return String.valueOf( min );
		}

		StringBuffer buffer = new StringBuffer();
		buffer.append( KoLConstants.FLOAT_FORMAT.format( expected ) );
		buffer.append( " (" );
		buffer.append( String.valueOf( min ) );
		buffer.append( "-" );
		buffer.append( String.valueOf( max ) );
		buffer.append( ")" );
		return buffer.toString();
	}

	// According to Greycat on the Wiki: "Peg style 1 goes right, peg style
	// 2 goes left, and peg style 3 is random"

	static final int RIGHT = 1;
	static final int LEFT = 2;
	static final int RANDOM = 3;

	public static final void loadGameBoard( final String board, final String payouts)
	{
		// Store the 16 rows of pegs in the matrix.
		// Even numbered rows have 9 pegs in columns 0, 2, ... 16
		// Odd numbered rows have 8 pegs in columns 1, 3, ... 15

		Preferences.setString( "lastPorkoBoard", board );
		Preferences.setString( "lastPorkoPayouts", payouts );

		// Make peg matrix
		SpaaaceRequest.pegs = new int[17][17];

		// Make div matrix
		SpaaaceRequest.divs = new String[17][17];

		// Make expected value array
		SpaaaceRequest.expected = new float[9];

		// Make slot title array
		SpaaaceRequest.slots = new String[9];

		// Store the pegs
		int index = 0;
		for ( int row = 0, off = 0; row < 16; ++row, off = 1 - off )
		{
			for ( int col = off; col < 17; col += 2 )
			{
				int peg = Character.getNumericValue( board.charAt( index++ ) );
				SpaaaceRequest.pegs[ row][ col ] = peg;
			}
		}

		// Store the payouts in the last row of the peg array
		for ( int col = 0; col < 17; col += 2 )
		{
			int payout = Character.getNumericValue( payouts.charAt( col / 2 ) );
			SpaaaceRequest.pegs[ 16 ][ col ] = payout;
		}

		// Mark unreachable cells
		SpaaaceRequest.calculateUnreachableCells();

		// Store the payout divs into the bottom row
		for ( int col = 0; col < 17; col += 2 )
		{
			int payout = SpaaaceRequest.pegs[ 16 ][ col ];
			if ( SpaaaceRequest.divs[ 15 ][ col ] == UNREACHABLE_CELL )
			{
				SpaaaceRequest.divs[ 16 ][ col ] = UNREACHABLE_PAYOUTS[ payout ];
			}
			else
			{
				SpaaaceRequest.divs[ 15 ][ col ] = DETERMINISTIC_DOWN[ payout ];
				SpaaaceRequest.divs[ 16 ][ col ] = PAYOUTS[ payout ];
			}
		}
	}

	public static final void calculateUnreachableCells()
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

				switch ( SpaaaceRequest.pegs[ row ][ col ] )
				{
				case RIGHT:
					if ( col < 16 )
					{
						reach2[ col + 1 ] = true;
					}
					else
					{
						reach2[ col - 1 ] = true;
					}
					break;
				case LEFT:
					if ( col > 0 )
					{
						reach2[ col - 1 ] = true;
					}
					else
					{
						reach2[ col + 1 ] = true;
					}
					break;
				case RANDOM:
					if ( col > 0 )
					{
						reach2[ col - 1 ] = true;
					}
					if ( col < 16 )
					{
						reach2[ col + 1 ] = true;
					}
					break;
				}
			}

			off = 1 - off;
			reach = reach2;

			for ( int col = off; col < 17; col += 2 )
			{
				if ( !reach[ col ] )
				{
					SpaaaceRequest.divs[ row ][ col ] = UNREACHABLE_CELL;
				}
			}
		}
	}

	public static final void solveGameBoard()
	{
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

		// Arrays of min, max, and expected values for cells in a row
		int [] min = new int[ 17 ];
		int [] max = new int[ 17 ];
		float [] expected = new float[ 17 ];

		// Initialize the arrays from the payouts at the bottom
		for ( int col = 0; col < 17; col +=2 )
		{
			int val = ( SpaaaceRequest.divs[ 15 ][ col ] == UNREACHABLE_CELL ) ?
				-1 : SpaaaceRequest.pegs[ 16 ][ col ];
			min[ col ] = val;
			max[ col ] = val;
			expected[ col ] = (float) val;
		}

		// Iterate up from the bottom of the board calculating payout
		// Do one extra iteration to end up with the expected values for the entry slots
		for ( int row = 14, off = 1; row >= -1; --row, off = 1 - off )
		{
			for ( int col = off; col < 17; col += 2 )
			{
				// If this cell is unreachable, skip it
				if ( row >= 0 && SpaaaceRequest.divs[ row ][ col ] == UNREACHABLE_CELL )
				{
					continue;
				}

				int peg =
					( col == 0 ) ? RIGHT :
					( col == 16 ) ? LEFT :
					SpaaaceRequest.pegs[ row + 1 ][ col ];
				int minVal;
				int maxVal;
				float eVal;

				// Look at the peg below this cell
				switch ( peg )
				{
				case RIGHT:
					minVal = min[ col + 1 ];
					maxVal = max[ col + 1 ];
					eVal = expected[ col + 1 ];
					break;
				case LEFT:
					minVal = min[ col - 1 ];
					maxVal = max[ col - 1 ];
					eVal = expected[ col - 1 ];
					break;
				case RANDOM:
					minVal = Math.min( min[ col - 1 ], min[ col + 1 ] );
					maxVal = Math.max( max[ col - 1 ], max[ col + 1 ] );
					eVal = ( expected[ col - 1 ] + expected[ col + 1 ] ) / 2.0f;
					break;
				default:
					// Huh?
					minVal = 0;
					maxVal = 0;
					eVal = 0.0f;
					break;
				}

				// Store values of this cell for use by next row
				min[ col ] = minVal;
				max[ col ] = maxVal;
				expected[ col ] = eVal;

				if ( row >= 0 )
				{
					// Calculate the div for this cell
					SpaaaceRequest.divs[ row ][ col ] = SpaaaceRequest.makeDiv( peg, minVal, maxVal, eVal );
				}
			}
		}

		// Save the expected value for each slot in the top row
		StringBuffer buffer = new StringBuffer();
		for ( int col = 0; col < 17; col += 2 )
		{
			int minVal = min[ col ];
			int maxVal = max[ col ];
			float eVal = expected[ col ];
			if ( col > 0 )
			{
				buffer.append( ":" );
			}
			buffer.append( KoLConstants.FLOAT_FORMAT.format( eVal ) );
			SpaaaceRequest.expected[ col / 2 ] = eVal;
			SpaaaceRequest.slots[ col / 2 ] = SpaaaceRequest.makeSlot( minVal, maxVal, eVal );
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

			if ( type.equals( "start" ) )
			{
				// Use green arrow for "best" starting slots 
				if ( SpaaaceRequest.expected[ col / 2 ] == best )
				{
					div = StringUtilities.singleStringReplace( div, ARROW, GREEN_ARROW );
				}

				String search = "Start Here";
				String replace = SpaaaceRequest.slots[ col / 2 ];
				div = StringUtilities.globalStringReplace( div, search, replace );
			}

			if ( type.equals( "blank" ) && row > 0 && col > 0 )
			{
				// Cells also get annotated
				div = SpaaaceRequest.divs[ row - 1 ][ col - 1 ];
			}

			buffer.append( div );

			if ( ++col == 19 )
			{
				col = 0;
				row += 1;
			}
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

		if ( urlString.indexOf( "place=shop" ) != -1 )
		{
			// Let appropriate Coin Master claim this
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		String message = null;

		if ( action == null )
		{
			if ( urlString.indexOf( "place=porko" ) != -1 )
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
