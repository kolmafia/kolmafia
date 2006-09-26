/**
 * Copyright (c) 2006, KoLmafia development team
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

// utilities
import net.java.dev.spellcast.utilities.UtilityConstants;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Louvre implements UtilityConstants
{
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );

	// Range of choice numbers within the Louvre
	private static final int FIRST_CHOICE = 92;
	private static final int LAST_CHOICE = 104;

	// The various locations within the Louvre
	private static final String LouvreLocationNames [] =
	{
		"Escher: Relativity",			// 92
		"Escher: House of Stairs",		// 93
		"Escher: Labyrinth",			// 94
		"Escher: Ascending and Descending",	// 95
		"Mondrian",				// 96
		"Munch: The Scream",			// 97
		"Botticelli: The Birth of Venus",	// 98
		"Michelangelo: The Creation of Adam",	// 99
		"David: The Death of Socrates",		// 100
		"Hopper: Nighthawks",			// 101
		"Seurat: Sunday Afternoon on the Island of La Grande Jatte",	// 102
		"Leonardo da Vinci: The Last Supper",	// 103
		"Dali: The Persistence of Memory",	// 104
	};

	// 0 = 92, 93, 94, or 95

	private static final int MANETWICH = 1;
	private static final int VANGOGHBITUSSIN = 2;
	private static final int PINOT_RENOIR = 2;
	private static final int MUSCLE = 5;
	private static final int MYSTICALITY = 6;
	private static final int MOXIE = 7;

	private static final int LouvreLocationExits [][] =
	{
		{ 96, 97, 98 },			// 92
		{ 96, 97, 98 },			// 93
		{ 96, 97, 98 },			// 94
		{ 96, 97, 98 },			// 95
		{ 0, 99, 100 },			// 96
		{ 0, 101, 102 },		// 97
		{ 0, 103, 104 },		// 98
		{ 0, 3, 6 },			// 99
		{ 0, 1, 6 },			// 100
		{ 0, 1, 4 },			// 101
		{ 0, 2, 4 },			// 102
		{ 0, 2, 5 },			// 103
		{ 0, 3, 5 },			// 104
	};

	public static final String LouvreGoals [] =
	{
		"Manetwich",
		"bottle of Vangoghbitussin",
		"bottle of Pinot Renoir",
		"Muscle",
		"Mysticality",
		"Moxie",
	};

	// Identifying strings from the response text
	public static final String LouvreGoalStrings [] =
	{
		"Manetwich",
		"bottle of Vangoghbitussin",
		"bottle of Pinot Renoir",
		"a pretty good workout.",
		"new insight as to the nature of the universe.",
		"Moxious!"
	};

	// The choice table.
	//
	// One row for each Louvre location (92 - 104)
	// Each row contains three values, corresponding to choices 1 - 3
	//
	// 0		Unknown
	// 1 - 6	A goal
	// X		A destination

	private static int LouvreChoiceTable [][] = new int [ LAST_CHOICE - FIRST_CHOICE + 1][ 3 ];

	public static void reset()
	{
		// Reset what we've "learned" about the Louvre choices
		for ( int i = FIRST_CHOICE; i <= LAST_CHOICE; ++i )
		{
			int choice[] = LouvreChoiceTable[ i - FIRST_CHOICE ];
			choice[0] = 0;
			choice[1] = 0;
			choice[2] = 0;
		}
	}

	public static boolean louvreChoice( int choice )
	{	return ( choice >= FIRST_CHOICE && choice <= LAST_CHOICE );
	}

	public static String handleChoice( String choice )
	{
		int source = StaticEntity.parseInt( choice );

		// We only handle Louvre choices
		if ( !louvreChoice( source ) )
			return "";

		// Get the user specified goal
		int goal = StaticEntity.getIntegerProperty( "louvreGoal" );

		// If no goal, return "".
		if ( goal == 0 )
			return "";

		// Not implemented yet
		return "";
	}

	public static boolean mapChoice( String text )
	{
		int lastChoice = KoLRequest.getLastChoice();
		if ( !louvreChoice( lastChoice ) )
			return false;

		int lastDecision = KoLRequest.getLastDecision() - 1;
		// Punt if bogus decision
		if ( lastDecision < 0 || lastDecision > 2 )
			return true;

		Matcher choiceMatcher = CHOICE_PATTERN.matcher( text );
		if ( choiceMatcher.find() )
		{
			int source = StaticEntity.parseInt( choiceMatcher.group(1) );

			// Sanity check: we must stay within the Louvre
			if ( !louvreChoice( source ) )
				return false;

			// Update the path table
                        mapChoice( lastChoice, lastDecision, source);
			return true;
		}

		// Perhaps we have reached a goal
		for ( int i = 0; i < LouvreGoalStrings.length; ++i )
		{
			if ( text.indexOf( LouvreGoalStrings[i] ) != -1 )
			{
				mapChoice( lastChoice, lastDecision, i + 1 );
				return true;
			}
		}

		// Shouldn't get here
		return false;
	}

	private static void mapChoice( int choice, int decision, int destination )
	{
		int choices[] = LouvreChoiceTable[ choice - FIRST_CHOICE ];
		choices[ decision ] = destination;

		// If 2 choices have been discovered, 3rd might be knowable.
		int index = -1;
		for ( int i = 0; i < 3; ++i )
		{
			if ( choices[i] == 0)
			{
				if ( index != -1 )
					return;
				index = i;
				continue;
			}
		}

		// Done if all three destinations are known.
		if ( index == -1 )
			return;

		// Find which exit has not been mapped
		int [] exits = LouvreLocationExits[ choice - FIRST_CHOICE ];
		for ( int i = 0; i < 3; ++i )
		{
			int exit = exits[i];

			// Can't deduce exit to one of four Escher drawings
			if ( exit == 0 )
				continue;

			boolean found = false;
			for ( int j = 0; j < 3; ++j )
			{
				if ( choices[j] == exit )
				{
					found = true;
					break;
				}
			}

			if ( !found )
			{
				choices[index] = exit;
				return;
			}
		}
	}

	public static String [][] choiceSpoilers( int choice )
	{
		// We only handle Louvre choices
		if ( !louvreChoice( choice ) )
			return null;

		// Return an array with the same structure as used by built-in
		// choice adventures.
		String [][] result = new String[3][];

		// The choice option is the first element
		result[0] = new String[1];
		result[0][0] = "choiceAdventure" + String.valueOf( choice );

		// The name of the choice is second element
		result[1] = new String[1];
		result[1][0] = LouvreLocationNames[ choice - FIRST_CHOICE ];

		// An array of choice spoilers is the third element
		int choices[] = LouvreChoiceTable[ choice - FIRST_CHOICE ];
		result[2] = new String[3];
		result[2][0] = choiceName( choice, choices[0] );
		result[2][1] = choiceName( choice, choices[1] );
		result[2][2] = choiceName( choice, choices[2] );

		return result;
	}

	private static String choiceName( int choice, int destination )
	{
		switch ( destination )
		{
		case 0:
			return "";
		case 1: case 2: case 3: case 4: case 5: case 6:
			return LouvreGoals[ destination - 1 ];
		default:
			return LouvreLocationNames[ destination - FIRST_CHOICE ];
		}
	}

	public static boolean freeAdventure( String choice, String decision )
	{
		// "choiceAdventureX"
		int source = StaticEntity.parseInt( choice.substring( 15 ) );

		// Make sure it's a Louvre adventure
		if ( !louvreChoice( source ) )
			return false;

		// It is. If it stays within the Louvre, it's free
		int option = StaticEntity.parseInt( decision );
		int destination = LouvreChoiceTable [source][option];
		return ( louvreChoice( destination ) );
	}
}
