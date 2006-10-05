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
import java.util.Arrays;
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
	private static final int PINOT_RENOIR = 3;
	private static final int MUSCLE = 4;
	private static final int MYSTICALITY = 5;
	private static final int MOXIE = 6;

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

	// The routing table.
	//
	// One row for each Louvre location (92 - 104)
	// Each row contains one tuple for each goal
	// Each tuple contains a directly accessible destination from the
	// current location that leads efficiently to the destination

	// 0		92, 93, 94, or 95
	// 1 - 6	A goal
	// X		A destination

	private static final int LouvreRoutingTable [][][] =
	{
		{ { 96, 97 }, { 97, 98 }, { 96, 98 },	// 92
		  { 97 }, { 98 }, { 96 } },
		{ { 96, 97 }, { 97, 98 }, { 96, 98 },	// 93
		  { 97 }, { 98 }, { 96 } },
		{ { 96, 97 }, { 97, 98 }, { 96, 98 },	// 94
		  { 97 }, { 98 }, { 96 } },
		{ { 96, 97 }, { 97, 98 }, { 96, 98 },	// 95
		  { 97 }, { 98 }, { 96 } },
		{ { 100 }, { 0 }, { 99 },		// 96
		  { 0 }, { 0 }, { 99, 100 } },
		{ { 101 }, { 102 }, { 0 },		// 97
		  { 101, 102 }, { 0 }, { 0 } },
		{ { 0 }, { 103 }, { 104 },		// 98
		  { 0 }, { 103, 104 }, { 0 } },
		{ { 0 }, { 0 }, { 3 },			// 99
		  { 0 }, { 0 }, { 6 } },
		{ { 1 }, { 0 }, { 0 },			// 100
		  { 0 }, { 0 }, { 6 } },
		{ { 1 }, { 0 }, { 0 },			// 101
		  { 4 }, { 0 }, { 0 } },
		{ { 0 }, { 2 }, { 0 },			// 102
		  { 4 }, { 0 }, { 0 } },
		{ { 0 }, { 2 }, { 0 },			// 103
		  { 0 }, { 5 }, { 0 } },
		{ { 0 }, { 0 }, { 3 },			// 104
		  { 0 }, { 5 }, { 0 } },
	};

	private static int [] routingTuple( int source, int goal )
	{
		if ( source < FIRST_CHOICE || source > LAST_CHOICE || goal < 1 || goal > 6 )
			return null;
		return LouvreRoutingTable[ source - FIRST_CHOICE ][ goal - 1];
	}

	// The choice table.
	//
	// One row for each Louvre location (92 - 104)
	// Each row contains three values, corresponding to choices 1 - 3
	//
	// 0		Unknown
	// 1 - 6	A goal
	// X		A destination

	private static int LouvreChoiceTable [][] = new int [ LAST_CHOICE - FIRST_CHOICE + 1][ 3 ];

	private static int [] choiceTuple( int source )
	{
		if ( source < FIRST_CHOICE || source > LAST_CHOICE )
			return null;
		return LouvreChoiceTable[ source - FIRST_CHOICE ];
	}

	public static void reset()
	{
		// Reset what we've "learned" about the Louvre choices
		for ( int i = 0; i < LouvreChoiceTable.length; ++i )
			for ( int j = 0; j < LouvreChoiceTable[i].length; ++j )
				LouvreChoiceTable[i][j] = 0;

		int lastLouvreAscension = StaticEntity.getIntegerProperty( "lastLouvreMap" );
		if ( lastLouvreAscension < KoLCharacter.getAscensions() )
		{
			StaticEntity.setProperty( "lastLouvreMap", String.valueOf( KoLCharacter.getAscensions() ) );
			StaticEntity.setProperty( "louvreLayout", "" );
		}

		String layout = StaticEntity.getProperty( "louvreLayout" );
		if ( layout.equals( "" ) )
			return;

		int currentIndex = 0;
		String [] layoutSplit = layout.split( "," );
		for ( int i = 0; i < LouvreChoiceTable.length; ++i )
			for ( int j = 0; j < LouvreChoiceTable[i].length; ++j )
				LouvreChoiceTable[i][j] = StaticEntity.parseInt( layoutSplit[currentIndex++] );
	}

	public static void saveMap()
	{
		StringBuffer map = new StringBuffer();

		for ( int i = 0; i < LouvreChoiceTable.length; ++i )
		{
			for ( int j = 0; j < LouvreChoiceTable[i].length; ++j )
			{
				if ( i != 0 || j != 0 )  map.append( ',' );
				map.append( LouvreChoiceTable[i][j] );
			}
		}

		StaticEntity.setProperty( "lastLouvreMap", String.valueOf( KoLCharacter.getAscensions() ) );
		StaticEntity.setProperty( "louvreLayout", map.toString() );
	}

	public static boolean louvreChoice( int choice )
	{	return ( choice >= FIRST_CHOICE && choice <= LAST_CHOICE );
	}

	public static String handleChoice( String choice )
	{
		// We only handle Louvre choices
		int source = StaticEntity.parseInt( choice );
		if ( !louvreChoice( source ) )
			return "";

		// Get the routing tuple for this choice/gaol
		int goal = StaticEntity.getIntegerProperty( "louvreGoal" );
		int [] tuple = routingTuple( source, goal );
		if ( tuple == null )
			return "";

		// Examine destinations and take one that we know about
		int [] choices = choiceTuple( source );
		for ( int i = 0; i < tuple.length; ++i )
		{
			int option = tuple[i];
			for ( int j = 0; j < choices.length; ++j )
			{
				int destination = choices[j];
				// If routing table destination is 0, we any of
				// 92 - 95 will do. Otherwise, need exact match
				if ( ( option == 0 && ( destination >= 92 && destination <= 95 ) ) ||
				     ( option != 0 && option == destination ) )
					return String.valueOf( j + 1 );
			}
		}

		// We don't know how to get to the destination. Pick one we
		// haven't explored.
		for ( int j = 0; j < choices.length; ++j )
		{
			if ( choices[j] == 0 )
				return String.valueOf( j + 1 );
		}

		// Shouldn't get here
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

		// Return if we've already mapped this decision
		if ( LouvreChoiceTable[ lastChoice - FIRST_CHOICE ][ lastDecision] != 0 )
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
		int choices[] = choiceTuple( choice );
		choices[ decision ] = destination;

		// If 2 choices have been discovered, 3rd might be knowable.
		int unknownIndex = -1;
		for ( int i = 0; i < 3; ++i )
		{
			if ( choices[i] != 0)
				continue;
			if ( unknownIndex != -1 )
				return;
			unknownIndex = i;
		}

		// Done if all three destinations are known.
		if ( unknownIndex == -1 )
			return;

		// Find which exit has not been mapped
		int [] exits = LouvreLocationExits[ choice - FIRST_CHOICE ];
		for ( int i = 0; i < exits.length; ++i )
		{
			int exit = exits[i];

			// Can't deduce exit to one of four Escher drawings
			if ( exit == 0 )
				continue;

			boolean found = false;
			for ( int j = 0; j < choices.length; ++j )
			{
				if ( exit == choices[j] )
				{
					found = true;
					break;
				}
			}

			if ( !found )
			{
				choices[ unknownIndex ] = exit;
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
		int choices[] = choiceTuple( choice );
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
		int option = StaticEntity.parseInt( decision ) - 1;
		int destination = LouvreChoiceTable[ source - FIRST_CHOICE ][option];
		return ( louvreChoice( destination ) );
	}

	// The Wiki has a Louvre Map:
	//
	//     http://kol.coldfront.net/thekolwiki/index.php/Louvre_Map
	//
	// The Wiki's numbering scheme mapped to Choice Adventure number:
	//
	//  0 = 92 (Escher: Relativity)
	//  1 = 93 (Escher: House of Stairs)
	//  2 = 94 (Escher: Labyrinth)
	//  3 = 95 (Escher: Ascending and Descending)
	//  4 = 97 (Munch: The Scream)
	//  5 = 98 (Botticelli: The Birth of Venus)
	//  6 = 96 (Mondrian)
	//  7 = 101 (Hopper: Nighthawks)
	//  8 = 102 (Seurat: Sunday Afternoon on the Island of La Grande Jatte)
	//  9 = 103 (Leonardo da Vinci: The Last Supper)
	// 10 = 104 (Dali: The Persistence of Memory)
	// 11 = 99 (Michelangelo: The Creation of Adam)
	// 12 = 100 (David: The Death of Socrates)
	//
	// Additionally, Gemelli has numbered the rewards for use in his
	// mapping tool:
	//
	// 13 = Muscle
	// 14 = Mysticality
	// 15 = Moxie
	// 16 = Manetwich
	// 17 = bottle of Vangoghbitussin
	// 18 = bottle of Pinot Renoir

	// The "random" exit - value chosen so it sorts at the end
	static final int RANDOM = Integer.MAX_VALUE;

	private static final int WikiToMafia [] =
	{
		92,		// 0
		93,		// 1
		94,		// 2
		95,		// 3
		97,		// 4
		98,		// 5
		96,		// 6
		101,		// 7
		102,		// 8
		103,		// 9
		104,		// 10
		99,		// 11
		100,		// 12
		5,		// 13
		6,		// 14
		7,		// 15
		1,		// 16
		2,		// 17
		3,		// 18
	};

	private static int mafiaCode( int wikiCode )
	{
		if ( wikiCode == RANDOM )
			return 0;
		return WikiToMafia[ wikiCode ];
	}

	private static final int MafiaLocationToWiki [] =
	{
		0,		// 92
		1,		// 93
		2,		// 94
		3,		// 95
		6,		// 96
		4,		// 97
		5,		// 98
		11,		// 99
		12,		// 100
		7,		// 101
		8,		// 102
		9,		// 103
		10,		// 104
	};

	private static final int MafiaGoalToWiki [] =
	{
		16,		// 1
		17,		// 2
		18,		// 3
		13,		// 4
		14,		// 5
		15,		// 6
	};

	private static int wikiCode( int mafiaCode )
	{
		// Map goals through one table
		if ( mafiaCode >= 1 && mafiaCode <= 6 )
			return MafiaGoalToWiki[ mafiaCode - 1 ];

		// Map destinations through another table
		if ( mafiaCode >= FIRST_CHOICE && mafiaCode <= LAST_CHOICE )
			return MafiaLocationToWiki[ mafiaCode - FIRST_CHOICE ];

		// Otherwise, just return max value
		return RANDOM;
	}

	private static int WikiLouvreLocationExits [][];

	static
	{
		buildWikiExits();
	}

	private static void buildWikiExits()
	{
		// Get a zeroed array to start things off.
		WikiLouvreLocationExits = new int [ LAST_CHOICE - FIRST_CHOICE + 1][ 3 ];

		// Examine each node in Mafia order
		for ( int source = FIRST_CHOICE; source <= LAST_CHOICE; ++source )
		{
			// Get the array of exit paths
			int mafiaExits[] = LouvreLocationExits[ source - FIRST_CHOICE ];
			int wikiExits[] = WikiLouvreLocationExits[ wikiCode( source) ];

			// Copy translated exit from Mafia exit table to Wiki exit table
			for ( int i = 0; i < mafiaExits.length; ++i )
				wikiExits[i] = wikiCode( mafiaExits[i] );

			// Sort the exits in Wiki order
			Arrays.sort( wikiExits );
		}
	}

	// Gemelli has a tool that accepts a code and displays the map
	// corresponding to it:
	//
	//     http://www.feesher.com/louvre_mapper.php
	//
        // Gemelli says:
        //
        // Just like the Fog Mapper, each digit transforms from a base-16
        // character to two base-4 characters. So after this transformation,
        // you'll end up with a 48-character string containing digits from 0 to
        // 3.
        //
        // The first 39 characters represent the paths from locations 0 through
        // 12. 0=unmapped, 1=up, 2=down, and 3=sideways. The target locations
        // are listed in numeric order. So the first three characters represent
        // the paths from location 0 to locations 4, 5, and 6 in that order.
        //
        // For locations 4-12, the first two characters are used to capture the
        // paths to the absolute locations, and the third character captures
        // the path to the randomized location. Example: for location 4, the
        // first character represents the path to location 7, the second to
        // location 8, and the third to the randomized location.
        //
        // So that covers the first 39 characters. The final 9 characters then
        // tell you the randomized locations accessible from locations 4-12. So
        // let's say those characters are 021310021 ... this means:
        //
        // * the randomized paths from locations 4, 9, and 10 are unmapped
        // * the randomized paths from locations 6, 8, and 12 connect to location 1
        // * the randomized paths from locations 5 and 11 connect to location 2
        // * the randomized path from location 7 connects to location 3

	public static String gemelliCode()
	{
		int code[] = new int[48];
		int codeIndex = 0;

		// Examine each node in Wiki order: 0 - 12
		for ( int i = 0; i < LouvreChoiceTable.length; ++i )
		{
			// Get the choice adventure # corresponding to the Wiki code
			int source = mafiaCode( i );

			// Get the array of exit paths
			int paths[] = LouvreChoiceTable[ source - FIRST_CHOICE ];

			// For each choice in Wiki order
			int exits[] = WikiLouvreLocationExits[ i ];

			for ( int j = 0; j < exits.length; ++j )
			{
				// Find the exit in the paths
				for ( int index = 0; index < paths.length; ++index )
				{
					int destination = wikiCode( paths[index] );

					// Ignore unmapped paths
					if ( destination == RANDOM )
						continue;

					// If this is the random exit...
					if ( exits[j] == RANDOM )
					{
						// ...destination must be an
						// Escher location
						if ( destination < 4 )
						{
							code[ codeIndex ] = index + 1;
							break;
						}
						continue;
					}

					if ( exits[j] == destination )
					{
						code[ codeIndex ] = index + 1;
						break;
					}
				}
				++codeIndex;
			}
		}

		// Look at choices 4-12 and determine where the "random" exit
		// goes. 0 = unmapped, 1 = 93, 2 = 94, 3 = 95

		for ( int i = 4; i < LouvreChoiceTable.length; ++i )
		{
			// Get the choice adventure # corresponding to the Wiki code
			int source = mafiaCode( i );

			// Get the array of exit paths
			int paths[] = LouvreChoiceTable[ source - FIRST_CHOICE ];

			// Examine each exit and determine which one is random
			int random = 0;
			for ( int j = 0; j < paths.length; ++j )
			{
				int exit = paths[j];
				if ( exit < FIRST_CHOICE )
					continue;
				exit -= FIRST_CHOICE;
				if ( exit > 3 )
					continue;
				random = exit;
				break;
			}
			code[ codeIndex ] = random;
			++codeIndex;
		}

		// Convert the 48 element int array into a 24 character character array
		char data[] = new char[24];
		for ( int i = 0; i < code.length; i += 2 )
		{
			int hexDigit = code[i] * 4 + code[i+1];
			data[i/2] = Character.forDigit( hexDigit, 16 );
		}

		return String.valueOf( data );
	}

	public static void showGemelliMap()
	{	StaticEntity.openSystemBrowser( "http://www.feesher.com/louvre_mapper.php?mapstring=" + gemelliCode() );
	}
}
