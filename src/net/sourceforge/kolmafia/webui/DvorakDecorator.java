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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;


public abstract class DvorakDecorator
{
	public static final void decorate( final StringBuffer buffer )
	{
		String search = "</div></center></td></tr>";
		int index = buffer.indexOf( search );
		if ( index == -1 )
		{
			return;
		}
		index += 6;

		// Build a "Solve!" button
		StringBuffer button = new StringBuffer();

		button.append( "<form name=solveform action='" );
		button.append( "/KoLmafia/specialCommand?cmd=dvorak&pwd=" );
		button.append( GenericRequest.passwordHash );
		button.append( "' method=post>" );
		button.append( "<input class=button type=submit value=\"Solve!\">" );
		button.append( "</form>" );

		// Insert it into the page
		buffer.insert( index, button );
	}

	private static String lastResponse = "";

	public static final void saveResponse( final String responseText )
	{
		DvorakDecorator.lastResponse = responseText;
	}

        // <td class='cell greyed'><img
        // src="http://images.kingdomofloathing.com/itemimages/tilek.gif"
        // width=30 height=30 border=0 alt='Tile labeled "K"'></td>
        //
        // <td class='cell'><a class=nounder
        // href='tiles.php?action=jump&whichtile=8'><img
        // src="http://images.kingdomofloathing.com/itemimages/tilep.gif"
        // width=30 height=30 border=0 alt='Tile labeled "P"'></a></td>


	private static final Pattern TILE_PATTERN = Pattern.compile( "<td class='(cell|cell greyed)'.*?'Tile labeled \"(.)\"'>(</a>)?</td>" );

	public static final void solve()
	{
		if ( DvorakDecorator.lastResponse == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You don't appear to be at the tiles puzzle" );
			return;
		}

		// Examine the tiles and figure out which row we are on.
		KoLmafia.updateDisplay( "Examining tiles..." );

		char [][] tiles = new char[7][9];
		int currentRow = 0;

		Matcher matcher = DvorakDecorator.TILE_PATTERN.matcher( DvorakDecorator.lastResponse );
		int count = 0;
		while ( matcher.find() )
		{
			int row = count / 9;
			int column = count % 9;
			if ( row > 7 )
			{
				KoLmafia.updateDisplay( "Too many rows!" );
				return;
			}
			if ( matcher.group(1).equals( "cell" ) )
			{
				currentRow = row;
			}
			tiles[ row ][ column ] = matcher.group(2).charAt( 0 );
			count++;
		}

		if ( count != (7 * 9 ) )
		{
			KoLmafia.updateDisplay( "Wrong number of cells!" );
			return;
		}

		/*
		StringBuilder buffer = new StringBuilder();
		for ( int row = 0; row < 7; ++row )
		{
			buffer.setLength( 0 );
			buffer.append( "Row " );
			buffer.append( String.valueOf( row + 1 ) );
			buffer.append( ": " );
			for ( int col = 0; col < 9; ++col )
			{
				buffer.append( tiles[ row ][ col ] );
				buffer.append( " " );
			}
			if ( row == currentRow )
			{
				buffer.append( " ---you are here" );
			}
			RequestLogger.printLine( buffer.toString() );
		}
		*/

		// Execute requests to hop from tile to tile to the end.
		String solution = "BANANAS";
		String message;

		GenericRequest request = new GenericRequest( "" );
		for ( int row = currentRow; row >= 0; --row )
		{
			char match = solution.charAt( 6 - row );
			int found = -1;
			for ( int col = 0; col < 9; ++col )
			{
				char tile = tiles[ row ][ col ];
				if ( match == tile )
				{
					found = col;
					break;
				}
			}
			if ( found == -1 )
			{
				KoLmafia.updateDisplay( "Could not find '" + match + "' in row " + ( row + 1 ) );
				return;
			}

			message = "Give me a" + ( match == 'A' ? "n " : " " ) + match + "!";

			KoLmafia.updateDisplay( message );
			RequestLogger.updateSessionLog( message );

			String url = "tiles.php?action=jump&whichtile=" + found;
			request.constructURLString( url );
			request.run();
		}

		message = "What's that spell? " + solution + "!";
		KoLmafia.updateDisplay( message );
		RequestLogger.updateSessionLog( message );

		KoLmafia.updateDisplay( "Tile puzzle completed." );

		StringBuffer buffer = new StringBuffer( request.responseText );
		RequestEditorKit.getFeatureRichHTML( request.getURLString(), buffer );
		RelayRequest.specialCommandResponse = buffer.toString();
		RelayRequest.specialCommandIsAdventure = true;
		DvorakDecorator.lastResponse = null;
	}
}
