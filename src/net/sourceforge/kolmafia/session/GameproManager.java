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

package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.preferences.Preferences;

public class GameproManager
{
	private static final Pattern MAZE_PATTERN =
		  Pattern.compile( "You will start out facing[\\s](\\w+).[\\s]You should go[\\s](\\w+),[\\s](\\w+),[\\s](\\w+),[\\s](\\w+),[\\s](\\w+)," );

	public static void parseGameproMagazine( String responseText )
	{
		if ( !responseText.contains( "Section I: LEGAL STUFF" ) )
		{
			return;
		}

		// How Does a Floating Platform Even Work?
		Preferences.setInteger( "choiceAdventure659",
			responseText.contains( "moving away from you" ) ? 1 :
			responseText.contains( "is approaching" ) ? 2 :
			responseText.contains( "same height as the one you're on" ) ? 3 :
			0 );

		// It's a Place Where Books Are Free
		Preferences.setInteger( "choiceAdventure660",
			responseText.contains( "bookcase" ) ? 1 :
			responseText.contains( "candlesticks" ) ? 2 :
			responseText.contains( "fireplace" ) ? 3 :
			0 );

		// Sphinx For the Memories
		Preferences.setInteger( "choiceAdventure661",
			responseText.contains( "\"time\"" ) ? 1 :
			responseText.contains( "\"a mirror\"" ) ? 2 :
			responseText.contains( "\"hope\"" ) ? 3 :
			0 );

		// Think or Thwim
		Preferences.setInteger( "choiceAdventure662",
			responseText.contains( "swim fins" ) ? 1 :
			responseText.contains( "make a raft" ) ? 2 :
			responseText.contains( "into the water" ) ? 3 : // not verified
			0 );

		// When You're a Stranger
		Preferences.setInteger( "choiceAdventure663",
			responseText.contains( "first door" ) ? 1 :
			responseText.contains( "second door" ) ? 2 :
			responseText.contains( "third door" ) ? 3 :
			0 );

		StringBuilder mazePreference = new StringBuilder( 9 );

		Matcher mazeMatcher = GameproManager.MAZE_PATTERN.matcher( responseText );
		if ( mazeMatcher.find() )
		{
			mazePreference.append( GameproManager.compareDirections( mazeMatcher.group( 1 ), mazeMatcher.group( 2 ) ) );
			mazePreference.append( "," );
			mazePreference.append( GameproManager.compareDirections( mazeMatcher.group( 2 ), mazeMatcher.group( 3 ) ) );
			mazePreference.append( "," );
			mazePreference.append( GameproManager.compareDirections( mazeMatcher.group( 3 ), mazeMatcher.group( 4 ) ) );
			mazePreference.append( "," );
			mazePreference.append( GameproManager.compareDirections( mazeMatcher.group( 4 ), mazeMatcher.group( 5 ) ) );
			mazePreference.append( "," );
			mazePreference.append( GameproManager.compareDirections( mazeMatcher.group( 5 ), mazeMatcher.group( 6 ) ) );

			Preferences.setString( "choiceAdventure665", mazePreference.toString() );
		}

	}

	private static String compareDirections( String direction1, String direction2 )
	{
		if ( direction1.equals( "north" ) )
		{
			if ( direction2.equals( "west" ) )
			{
				return "1";
			}
			else if ( direction2.equals( "north" ) )
			{
				return "2";
			}
			else if ( direction2.equals( "east" ) )
			{
				return "3";
			}
			else
			{
				return "0";
			}
		}

		else if ( direction1.equals( "east" ) )
		{
			if ( direction2.equals( "north" ) )
			{
				return "1";
			}
			else if ( direction2.equals( "east" ) )
			{
				return "2";
			}
			else if ( direction2.equals( "south" ) )
			{
				return "3";
			}
			else
			{
				return "0";
			}
		}

		else if ( direction1.equals( "south" ) )
		{
			if ( direction2.equals( "east" ) )
			{
				return "1";
			}
			else if ( direction2.equals( "south" ) )
			{
				return "2";
			}
			else if ( direction2.equals( "west" ) )
			{
				return "3";
			}
			else
			{
				return "0";
			}
		}

		else if ( direction1.equals( "west" ) )
		{
			if ( direction2.equals( "south" ) )
			{
				return "1";
			}
			else if ( direction2.equals( "west" ) )
			{
				return "2";
			}
			else if ( direction2.equals( "north" ) )
			{
				return "3";
			}
			else
			{
				return "0";
			}
		}

		return "0";
	}

	public static String autoSolve( int stepCount )
	{
		String choices[] = Preferences.getString( "choiceAdventure665" ).split( "," );
		if ( stepCount < 0 || stepCount > choices.length - 1 )
		{
			// Something went wrong, hand it over for manual control
			return "0";
		}
		return choices[ stepCount ];
	}

	public static void addGoalButton( final StringBuffer buffer )
	{
		// Only add the goal button to the first choice
		if ( buffer.indexOf( "swim down the tube" ) != -1 )
		{
			ChoiceManager.addGoalButton( buffer, "Exit the maze" );
		}
	}
}
