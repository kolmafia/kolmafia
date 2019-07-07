/**
 * Copyright (c) 2005-2018, KoLmafia development team
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

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class BeachManager
{
	// main.php?comb=1
	// choice.php?forceoption=0
	// choice.php?whichchoice=1388&pwd&option=1&minutes=420
	// choice.php?whichchoice=1388&pwd&option=4&coords=8%2C4197
	// choice.php?whichchoice=1388&pwd&option=5

	// You walk for 420 minutes and find a nice stretch of beach.  Now...  Where to comb?

	private static final Pattern MINUTES_PATTERN = Pattern.compile( "You walk for ([\\d,]+) minutes and find a nice stretch of beach" );

	// Beach Layout:
	//
	//     (XXXX minutes down the beach)
	//              columns
	//     0  1  2  3  4  5  6  7  8  9
	// 10  x  x  x  x  x  x  x  x  x  x
	//  9  x  x  x  x  x  x  x  x  x  x
	//  8  x  x  x  x  x  x  x  x  x  x
	//  7  x  x  x  x  x  x  x  x  x  x
	//  6  x  x  x  x  x  x  x  x  x  x
	//  5  x  x  x  x  x  x  x  x  x  x
	//  4  x  x  x  x  x  x  x  x  x  x
	//  3  x  x  x  x  x  x  x  x  x  x
	//  2  x  x  x  x  x  x  x  x  x  x
	// (1) wave washed squares
	//
	// Coordinates: <row>,(minutes*10-column)
	//
	// I've seen a report that the number of rows on the beach changes from day to day. Tides?

	private static final Pattern MAP_PATTERN = Pattern.compile( "name=\"coords\" value=\"([\\d]+),([\\d]+)\".*?title=\"([^\"]*).*?otherimages/beachcomb/(.*?).png", Pattern.DOTALL );

	// Known "title" names
	//
	// rough sand
	// combed sand
	// beach head
	//
	// There are others. whale.png is ... what? a "beached whale"?

	// Settings to hold current map:
	//
	// _beachLayout
	// _beachMinutes
	//
	// Unspaded, as far as I know, whether what's at a particular coordinate changes from day to day.
	// "rough sand" can change to "combed sand" via actions of other players.
	// Presumably all "combed sand" periodically becomes "rough sand" (at rollover?)
	// Presumably, "beach head" squares do not change
	//
	// So, we'll use "_" preferences, for now.

	public static final void parseBeachMap( final String text )
	{
		Matcher matcher = BeachManager.MINUTES_PATTERN.matcher( text );
		if ( !matcher.find() )
		{
			// Print an error?
			return;
		}

		int minutes = StringUtilities.parseInt( matcher.group(1) );

		StringBuilder layout = new StringBuilder();

		// Since number of rows vary, make a map to hold the layout for each row
		Map<Integer, String> rowLayout = new TreeMap<Integer, String>();
		int currentRow = -1;

		matcher = BeachManager.MAP_PATTERN.matcher( text );
		while ( matcher.find() )
		{
			int row = StringUtilities.parseInt( matcher.group(1) );
			int col = StringUtilities.parseInt( matcher.group(2) );
			String type = matcher.group(3);
			String image = matcher.group(4);

			if ( row != currentRow )
			{
				if ( currentRow != -1 )
				{
					rowLayout.put( currentRow, layout.toString() );
				}
				layout.setLength( 0 );
				currentRow = row;
			}

			char ch =
				type.equals( "rough sand" ) ? 'R' :
				type.equals( "combed sand" ) ? 'C' :
				type.equals( "a beach head" ) ? 'H' :
				image.equals( "whale" ) ?  'W':
				'?';

			layout.append( ch );
		}

		if ( currentRow != -1 )
		{
			rowLayout.put( currentRow, layout.toString() );
		}

		layout.setLength( 0 );

		for ( Entry<Integer, String> entry : rowLayout.entrySet() )
		{
			int row = entry.getKey();
			String cols = entry.getValue();
			if ( layout.length() > 0 )
			{
				layout.append( "," );
			}
			layout.append( String.valueOf( row ) );
			layout.append( ':' );
			layout.append( cols );
		}

		Preferences.setInteger( "_beachMinutes", minutes );
		Preferences.setString( "_beachLayout", layout.toString() );
	}

	public static final String beachLayout()
	{
		String layout = Preferences.getString( "beachLayout" );
		if ( layout.length() != 25 )
		{
			layout = "0000000000000000000000000";
			// Preferences.setString( "beachLayout", layout );
		}
		return layout;
	}
}
