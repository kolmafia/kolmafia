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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HobopolisDecorator
{
	private static final Pattern HOBOPOLIS_IMG_PATTERN = Pattern.compile(
		"otherimages/hobopolis/[a-z]+(\\d+)" );

	public static final void handleTownSquare( final String responseText )
	{
		// Nothing to do until battle is done
		if ( responseText.indexOf( "WINWINWIN" ) == -1 )
		{
			return;
		}

		String itemName = null;

		// Man, you really hit that hobo hard! So hard, in fact, that
		// you knocked him completely out of his skin, which is now
		// lying in a wrinkly heap on the ground.
		if ( responseText.indexOf( "wrinkly heap on the ground" ) != -1 )
		{
			itemName = "hobo skin";
		}

		// Boy, you sure scorched that hobo good! All that's left is a
		// smoking pair of boots!
		else if ( responseText.indexOf( "smoking pair of boots" ) != -1 )
		{
			itemName = "charred hobo boots";
		}

		// Wow! You froze that hobo so hard that he shattered into a
		// million pieces! All that's left is a pair of frozen eyeballs.
		else if ( responseText.indexOf( "pair of frozen eyeballs" ) != -1 )
		{
			itemName = "frozen hobo eyeballs";
		}

		// Man, you really did a number on that hobo. All that's left
		// of him is a pile of foul-smelling guts.
		else if ( responseText.indexOf( "pile of foul-smelling guts" ) != -1 )
		{
			itemName = "stinking hobo guts";
		}

		// Boy, you sure scared that hobo! He ran off so fast that he
		// left his skull behind!
		else if ( responseText.indexOf( "he left his skull behind" ) != -1 )
		{
			itemName = "creepy hobo skull";
		}

		// Wow. You embarrassed that hobo so thoroughly that he ran off
		// without his crotch!
		else if ( responseText.indexOf( "he ran off without his crotch" ) != -1 )
		{
			itemName = "hobo crotch";
		}

		if ( itemName != null )
		{
			String message = "Richard takes a " + itemName;
			RequestLogger.updateSessionLog( message );
			RequestLogger.printLine( message );

			AdventureResult result = AdventureResult.tallyItem( itemName );
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
	}
	
	public static final void decorate( String location, StringBuffer buffer )
	{
		if ( location.indexOf( "place=1" ) == -1 )
		{
			Matcher m = HOBOPOLIS_IMG_PATTERN.matcher( buffer );
			if ( m.find() )
			{
				StringUtilities.singleStringReplace( buffer, "</b>", 
					"</b> <font size=1>(image " + m.group( 1 ) + ")</font>" );
			}
		}

		if ( location.indexOf( "place=4" ) != -1 )
		{
			int pos = buffer.lastIndexOf( "</body>" );
			String ending = buffer.substring( pos );
			buffer.delete( pos, Integer.MAX_VALUE );
			buffer.append( "<form># tires -> # kills: <select>" );
			for ( int i = 1; i <= 49; ++i )
			{
				float kills = 0.1f * i * i + 0.7f * i + 0.5f;
				buffer.append( "<option>" );
				buffer.append( i );
				buffer.append( " -> " );
				buffer.append( (int) kills );
			}
			buffer.append( "</select></form>" );
			buffer.append( ending );
		}
	}
}
