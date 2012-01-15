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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class MineDecorator
{
	private static final Pattern MINE_PATTERN = Pattern.compile( "mine=(\\d+)" );
	private static final Pattern WHICH_PATTERN = Pattern.compile( "which=(\\d+)" );
	private static final Pattern IMG_PATTERN = Pattern.compile( "<img[^>]+>" );
	private static final Pattern TD_PATTERN = Pattern.compile(
		"<td.*?src=['\"](.*?)['\"].*?alt=['\"](.*?) \\(([\\d+]),([\\d+])\\)['\"].*?</td>" );

	public static final void decorate( final String location, final StringBuffer buffer )
	{
		// Replace difficult to see sparkles with more obvious images
		StringUtilities.globalStringReplace( buffer,
			"http://images.kingdomofloathing.com/otherimages/mine/wallsparkle",
			"/images/otherimages/mine/wallsparkle" );

		if ( buffer.indexOf( "<div id='postload'" ) == -1 )
		{
			return;
		}

		// Determine which mine we are in
		Matcher m = MINE_PATTERN.matcher( location );
		if ( !m.find() )
		{
			return;
		}

		// Fetch explored layout of that mine.
		String data = Preferences.getString( "mineLayout" + m.group( 1 ) );

		// Find the ore squares in the image.
		m = TD_PATTERN.matcher( buffer.toString() );
		if ( !m.find() )
		{
			return;
		}

		buffer.setLength( 0 );
		do
		{
			if ( !m.group( 2 ).equals( "Open Cavern" ) )
			{
				continue;
			}

			// KoL now lists squares as (col,row).
			// Columns go from 0 to 7. Rows go from 0 to 6
			int col = StringUtilities.parseInt( m.group( 3 ) );
			int row = StringUtilities.parseInt( m.group( 4 ) );

			int which = ( row * 8 ) + col;

			Matcher n = Pattern.compile( "#" + which + "(<.*?>)" ).matcher( data );
			if ( !n.find() )
			{
				continue;
			}
			m.appendReplacement( buffer,
				"<td width=50 height=50 background='$1' align=center>" +
				n.group( 1 ) + "</td>" );
		}
		while ( m.find() );
		m.appendTail( buffer );
	}
	
	public static final void parseResponse( final String location, final String responseText )
	{
		if ( Preferences.getInteger( "lastMiningReset" ) != KoLCharacter.getAscensions() )
		{
			for ( int i = 1; i < 10; ++i )
			{
				if ( Preferences.getString( "mineLayout" + i ).length() > 0 )
				{
					Preferences.setString( "mineLayout" + i, "" );
				}
			}
			Preferences.setInteger( "lastMiningReset", KoLCharacter.getAscensions() );
		}
		
		Matcher m = MINE_PATTERN.matcher( location );
		if ( !m.find() )
		{
			return;
		}
		String pref = "mineLayout" + m.group( 1 );
		if ( location.indexOf( "reset=1" ) != -1 )
		{
			Preferences.setString( pref, "" );
			return;
		}
		m = WHICH_PATTERN.matcher( location );
		if ( !m.find() || ( responseText.indexOf( "You acquire" ) == -1 &&
			responseText.indexOf( "An inexpert swing" ) == -1 ) )
		{
			return;
		}
		String which = m.group( 1 );
		m = IMG_PATTERN.matcher( responseText );
		if ( !m.find() )
		{
			return;
		}
		Preferences.setString( pref, Preferences.getString( pref ) + "#" + which + m.group( 0 ) );
	}
}
