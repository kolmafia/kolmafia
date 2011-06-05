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
	// <div class="blank">x1</div>
	private static final Pattern PAYOUT_PATTERN = Pattern.compile( "<div class=\"blank\">x(\\d)</div>" );

	private static float [][] matrix = null;
	private static float [] expected = null;

	public static final void visitPorkoChoice( final String responseText )
	{
		// Called when we play Porko

		// Initialize to defaults
		Preferences.setString( "lastPorkoBoard", "" );
		Preferences.setString( "lastPorkoPayouts", "" );
		Preferences.setString( "lastPorkoExpected", "" );

		// You hand Juliedriel your isotope. She takes it with
		// a pair of tongs, and hands you three Porko chips
		if ( responseText.indexOf( "You hand Juliedriel your isotope" ) == -1 )
		{
			SpaaaceRequest.matrix = null;
			SpaaaceRequest.expected = null;
			return;
		}

		ResultProcessor.processItem( ItemPool.LUNAR_ISOTOPE, -1 );

		// Parse the game board.
		StringBuffer buffer = new StringBuffer();
		Matcher matcher = PEG_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			buffer.append( matcher.group(1) );
		}

		String board = buffer.toString();

		buffer.setLength( 0 );
		matcher = PAYOUT_PATTERN.matcher( responseText );
		while ( matcher.find() )
		{
			buffer.append( matcher.group(1) );
		}

		String payouts = buffer.toString();

		// We could presumably figure out the expected value for each
		// starting position. According to Greycat on the Wiki: "Peg
		// style 1 goes right, peg style 2 goes left, and peg style 3
		// is random"

		// Store the 16 rows of pegs in the matrix.
		// Even numbered rows have 9 pegs in columns 0, 2, ... 16
		// Odd numbered rows have 8 pegs in columns 1, 3, ... 15
		// There must be 8 * ( 9 + 8 ) = 136 pegs total
		// There must be 9 payouts total
		if ( board.length() != ( 8 * (9 + 8 ) ) || payouts.length() != 9 )
		{
			SpaaaceRequest.matrix = null;
			SpaaaceRequest.expected = null;
			return;
		}

		Preferences.setString( "lastPorkoBoard", board );
		Preferences.setString( "lastPorkoPayouts", payouts );

		// Make a matrix if we don't already have one
		if ( SpaaaceRequest.matrix == null )
		{
			SpaaaceRequest.matrix = new float[17][17];
		}

		if ( SpaaaceRequest.expected == null )
		{
			SpaaaceRequest.expected = new float[9];
		}

		// Store the pegs
		int index = 0;
		for ( int row = 0; row < 16; ++row )
		{
			int count = ( row % 2 ) == 0 ? 9 : 8;
			int off =  ( row % 2 ) == 0 ? 0 : 1;
			for ( int i = 0; i < count; ++i )
			{
				int peg = Character.getNumericValue( board.charAt( index++ ) );
				SpaaaceRequest.matrix[ row][ i * 2 + off ] = (float) peg;
			}
		}

		// Store the payouts
		for ( int i = 0; i < 9; ++i )
		{
			int payout = Character.getNumericValue( payouts.charAt( i ) );
			SpaaaceRequest.matrix[ 16 ][ i * 2 ] = (float) payout;
		}

		// Iterate up from the bottom of the board calculating payout
		for ( int row = 15; row >= 0; --row )
		{
			int count = ( row % 2 ) == 0 ? 9 : 8;
			int off =  ( row % 2 ) == 0 ? 0 : 1;
			for ( int i = 0; i < count; ++i )
			{
				int col = i * 2 + off;

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
				float value = 0;

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

		// Save and print the payouts
		buffer.setLength( 0 );
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

		// Make the expected payouts be the hover text
		String search = "\"Start Here\"";
		for ( int i = 0; i < 9; ++i )
		{
			float val = SpaaaceRequest.expected[ i ];
			String replace = "\"" + KoLConstants.FLOAT_FORMAT.format( val ) + "\"";

			// Get both alt and title
			StringUtilities.singleStringReplace( buffer, search, replace );
			StringUtilities.singleStringReplace( buffer, search, replace );
		}
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
