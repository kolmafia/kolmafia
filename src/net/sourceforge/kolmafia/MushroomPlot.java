/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MushroomPlot implements KoLConstants
{
	// The player's mushroom plot
	//
	//  1  2  3  4
	//  5  6  7  8
	//  9 10 11 12
	// 13 14 15 16

	private static int [] plot = new int[16];
	private static boolean initialized = false;
	private static boolean ownsPlot = false;

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

	// Spore data
	private static final int [][] SPORE_DATA =
	{
		// Mushroom type, code, cost
		{ SPOOKY, 1, 30 },
		{ KNOB, 2, 40 },
		{ KNOLL, 3, 50 }
	};

	// Public functions to manipulate the mushroom plot

	public static void reset ( KoLmafia client )
	{
		initialized = false;
	}

	public static int [] getMushroomPlot( KoLmafia client )
	{
		initialize( client );
		if ( !client.permitsContinue() )
			return null;

		return plot;
	}

	public static boolean plantMushroom( KoLmafia client, int square, int spore )
	{
		// Validate square
		if ( square < 1 || square > 16 )
		{
			client.updateDisplay( ERROR_STATE, "Squares are numbered from 1 to 16." );
			client.cancelRequest();
			return false;
		}

		// Validate spore
		int [] sporeData = null;
		for ( int i = 0; i < SPORE_DATA.length; ++i )
			if ( SPORE_DATA[i][0] == spore )
			{
				sporeData = SPORE_DATA[i];
				break;
			}

		if ( sporeData == null )
		{
			client.updateDisplay( ERROR_STATE, "You can't plant that." );
			client.cancelRequest();
			return false;
		}

		// Make sure we have enough meat to pay for the spore
		AdventureResult meat = new AdventureResult( AdventureResult.MEAT, sporeData[2] );

		List requirements = new ArrayList();
		requirements.add( meat );

		// Check it
		if ( !client.checkRequirements( requirements ) )
			return false;

		// Make sure we know current state of mushroom plot
		initialize( client );

		// Bail now if can't get plot
		if ( !client.permitsContinue() )
			return false;

		// If the square isn't empty, pick what's there
		if ( plot[ square - 1 ] != EMPTY && !pickMushroom( client, square) )
			return false;

		// Plant the requested spore
		KoLRequest request = new KoLRequest( client, "knoll_mushrooms.php", true);
		request.addFormField( "action", "plant" );
		request.addFormField( "pos", String.valueOf( square - 1 ) );
		request.addFormField( "whichspore", String.valueOf( sporeData[1] ) );

		request.run();

                // If it failed, bail
		if ( !client.permitsContinue() )
			return false;

		// Pay for the spore
                client.processResult( meat.getNegation() );

                // Reset the plot plan
		parsePlot( request.responseText );

		return client.permitsContinue();
	}

	public static boolean pickMushroom( KoLmafia client, int square )
	{
		// Validate square
		if ( square < 1 || square > 16 )
		{
			client.updateDisplay( ERROR_STATE, "Squares are numbered from 1 to 16." );
			client.cancelRequest();
			return false;
		}

		// Make sure we know current state of mushroom plot
		initialize( client );

		// Bail now if can't get plot
		if ( !client.permitsContinue() )
			return false;

		// If the square is empty, bail now.
		if ( plot[ square - 1 ] == EMPTY )
			return true;

		// Pick the requested square
		KoLRequest request = new KoLRequest( client, "knoll_mushrooms.php", true );
		request.addFormField( "action", "click" );
		request.addFormField( "pos", String.valueOf( square - 1 ) );
		request.run();

		// Get the item we picked.
		client.processResults( request.responseText );
		parsePlot( request.responseText );

		return client.permitsContinue();
	}

	public static void parsePlot( String text )
	{
		initialized = true;

		// Clear out plot
		for ( int i = 0; i < plot.length; ++i )
			plot[i] = EMPTY;

		// See if we have a plot to parse
		Matcher plotMatcher = Pattern.compile( "<b>Your Mushroom Plot:</b><p><table>(<tr>.*?</tr><tr>.*></tr><tr>.*?</tr><tr>.*</tr>)</table>" ).matcher( text );
		if ( !plotMatcher.find() )
		{
			ownsPlot = false;
			return;
		}

		// He does own a plot. See what's there.
		ownsPlot = true;
		Matcher squareMatcher = Pattern.compile( "<td>(.*?)</td>" ).matcher( plotMatcher.group(1) );

		for ( int i = 0; i < plot.length; ++i )
		{
			// There should be exactly 16 squares here.  It's an
			// error if we can't match, but handle it
			if ( !squareMatcher.find() )
				break;

			plot[i] = parseSquare( squareMatcher.group(1) );
		}
	}

	private static int parseSquare( String text )
	{
		// We figure out what's there based on the image
		Matcher gifMatcher = Pattern.compile( ".*/(.*).gif" ).matcher( text );
		if ( gifMatcher.find() )
		{
			String gif = gifMatcher.group(1);

			if ( gif.equals( "spooshroom" ) )
				return SPOOKY;
			if ( gif.equals( "mushroom" ) )
				return KNOB;
			if ( gif.equals( "bmushroom" ) )
				return KNOLL;
			if ( gif.equals( "plaidroom" ) )
				return COOL;
			if ( gif.equals( "flatshroom" ) )
				return WARM;
			if ( gif.equals( "tallshroom" ) )
				return POINTY;
			if ( gif.equals( "fireshroom" ) )
				return FLAMING;
			if ( gif.equals( "iceshroom" ) )
				return FROZEN;
			if ( gif.equals( "stinkshroo" ) )
				return STINKY;
			if ( gif.equals( "mushsprout" ) )
				return SPROUT;

			// Else, fall through; it's dirt.
		}

		return EMPTY;
	}

	public static String squareName( int square )
	{
		switch ( square )
		{
		case EMPTY:
			return "__";
		case SPROUT:
			return "oo";
		case SPOOKY:
			return "Sp";
		case KNOB:
			return "Kb";
		case KNOLL:
			return "Kn";
		case WARM:
			return "Wa";
		case COOL:
			return "Co";
		case POINTY:
			return "Po";
		case FLAMING:
			return "Fl";
		case FROZEN:
			return "Fr";
		case STINKY:
			return "St";
		default:
			return "??";
		}
	}

	public static String html( KoLmafia client )
	{
		// Make sure we know current state of mushroom plot
		initialize( client );

		// Bail now if can't get plot
		if ( !client.permitsContinue() )
			return "Can't visit your plot";

		StringBuffer buffer = new StringBuffer();

		buffer.append( "<center><table>" );
		for ( int row = 0; row < 4; ++row )
		{
			buffer.append( "<tr>" );
			for ( int col = 0; col < 4; ++col )
			{
				buffer.append( "<td>" );

				int square = plot [ row * 4 + col ];
				String image = mushroomImage( square );
				String desc = mushroomDesc( square );

				if ( desc != null )
					buffer.append( desc );
				buffer.append( image );
				if ( desc != null )
					buffer.append( "</a>" );

				buffer.append( "</td>" );
			}
			buffer.append( "</tr>" );
		}
		buffer.append( "</table></center>" );

		return buffer.toString();
	}

	public static String mushroomImage( int square )
	{
		String gif;

		switch ( square )
		{
		default:
		case EMPTY:
			gif = "dirt1";
			break;
		case SPROUT:
			gif = "mushsprout";
			break;
		case SPOOKY:
			gif = "spooshroom";
			break;
		case KNOB:
			gif = "bmushroom";
			break;
		case KNOLL:
			gif = "mushroom";
			break;
		case WARM:
			gif = "flatshroom";
			break;
		case COOL:
			gif = "plaidroom";
			break;
		case POINTY:
			gif = "tallshroom";
			break;
		case FLAMING:
			gif = "fireshroom";
			break;
		case FROZEN:
			gif = "iceshroom";
			break;
		case STINKY:
			gif = "stinkshroo";
			break;
		}

		return "<img src=\"http://images.kingdomofloathing.com/itemimages/" + gif + ".gif\" width=30 height=30 border=0>";
	}

	public static String mushroomDesc( int square )
	{
		if ( square == EMPTY || square == SPROUT )
			return null;

		String itemDesc = TradeableItemDatabase.getDescriptionID( square );
		return "<a href=\"desc_item.php?whichitem=" + itemDesc + "\">";
	}

	// Internal functions

	private static void initialize ( KoLmafia client )
	{
		// Clear error state
		client.resetContinueState();

		// Do this only once.
		if ( initialized )
			return;

		initialized = true;

		// If you're not in a Muscle sign, no go.
		if ( !client.getCharacterData().inMuscleSign() )
		{
			client.updateDisplay( ERROR_STATE, "You can't find the mushroom fields." );
			client.cancelRequest();
			return;
		}

		// Otherwise, ask for the state of your plot.
		KoLRequest request = new KoLRequest( client, "knoll_mushrooms.php", true );
		request.run();
		parsePlot( request.responseText );

		// If you don't own a mushroom plot, bail now.
		if ( ownsPlot == false )
		{
			client.updateDisplay( ERROR_STATE, "You haven't bought a mushroom plot yet." );
			client.cancelRequest();
			return;
		}
	}
}
