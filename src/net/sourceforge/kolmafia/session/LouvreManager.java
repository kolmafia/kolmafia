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

import java.util.Arrays;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public abstract class LouvreManager
{
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );

	// Range of choice numbers within the LouvreManager
	private static final int FIRST_CHOICE = 92;
	private static final int LAST_CHOICE = 104;

	// The various locations within the LouvreManager
	private static final String LouvreLocationNames [] =
	{
		"Escher: Relativity",			// 92
		"Escher: House of Stairs",		// 93
		"Escher: Labyrinth",			// 94
		"Escher: Ascending and Descending",	// 95
		"Mondrian: Moxie, Manetwich, Pinot Renoir",				// 96
		"Munch: The Scream: Muscle, Manetwich, Vangoghbitussin",			// 97
		"Botticelli: The Birth of Venus: Mysticality, Vangoghbitussin, Pinot Renoir",	// 98
		"Michelangelo: The Creation of Adam: Moxie, Pinot Renoir",	// 99
		"David: The Death of Socrates: Moxie, Manetwich",		// 100
		"Hopper: Nighthawks: Muscle, Manetwich",			// 101
		"Seurat: Sunday Afternoon on the Island of La Grande Jatte: Muscle, Vangoghbitussin",	// 102
		"Leonardo da Vinci: The Last Supper: Mysticality, Vangoghbitussin",	// 103
		"Dali: The Persistence of Memory: Mysticality, Pinot Renoir",	// 104
	};

	private static final String LouvreShortLocationNames [] =
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

	private static final int LouvreLocationExits [][] =
	{
		{ 96, 97, 98 },			// 92
		{ 96, 97, 98 },			// 93
		{ 96, 97, 98 },			// 94
		{ 96, 97, 98 },			// 95
		{ 0, 99, 100 },			// 96
		{ 0, 101, 102 },		// 97
		{ 0, 103, 104 },		// 98
		{ 0, 6, 3 },			// 99
		{ 0, 6, 1 },			// 100
		{ 0, 1, 4 },			// 101
		{ 0, 4, 2 },			// 102
		{ 0, 2, 5 },			// 103
		{ 0, 5, 3 },			// 104
	};

	public static final String LouvreGoals [] =
	{
		"Manetwich",
		"bottle of Vangoghbitussin",
		"bottle of Pinot Renoir",
		"Muscle",
		"Mysticality",
		"Moxie"
	};
	
	public static final AdventureResult LouvreGoalItems[] =
	{
		ItemPool.get( ItemPool.MANETWICH, 1),
		ItemPool.get( ItemPool.VANGOGHBITUSSIN, 1),
		ItemPool.get( ItemPool.PINOT_RENOIR, 1),
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
	// One row for each LouvreManager location (92 - 104)
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

	private static final int[] routingTuple( final int source, final int goal )
	{
		if ( source < LouvreManager.FIRST_CHOICE || source > LouvreManager.LAST_CHOICE || goal < 1 || goal > 6 )
		{
			return null;
		}
		return LouvreManager.LouvreRoutingTable[ source - LouvreManager.FIRST_CHOICE ][ goal - 1 ];
	}

	// The choice table.
	//
	// One row for each LouvreManager location (92 - 104)
	// Each row contains three values, corresponding to choices 1 - 3
	//
	// 0		Unknown
	// 1 - 6	A goal
	// X		A destination

	private static final int LouvreChoiceTable[][] = new int[ LouvreManager.LAST_CHOICE - LouvreManager.FIRST_CHOICE + 1 ][ 3 ];

	private static final int[] choiceTuple( final int source )
	{
		if ( source < LouvreManager.FIRST_CHOICE || source > LouvreManager.LAST_CHOICE )
		{
			return null;
		}
		return LouvreManager.LouvreChoiceTable[ source - LouvreManager.FIRST_CHOICE ];
	}

	public static final void reset()
	{
		// Reset what we've "learned" about the LouvreManager choices
		for ( int i = 0; i < LouvreManager.LouvreChoiceTable.length; ++i )
		{
			Arrays.fill( LouvreManager.LouvreChoiceTable[ i ], 0 );
		}

		int lastLouvreAscension = Preferences.getInteger( "lastLouvreMap" );
		if ( lastLouvreAscension != KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastLouvreMap", KoLCharacter.getAscensions() );
			Preferences.setString( "louvreLayout", "" );
		}

		String layout = Preferences.getString( "louvreLayout" );
		if ( layout.equals( "" ) )
		{
			return;
		}

		int currentIndex = 0;
		String[] layoutSplit = layout.split( "," );
		for ( int i = 0; i < LouvreManager.LouvreChoiceTable.length; ++i )
		{
			for ( int j = 0; j < LouvreManager.LouvreChoiceTable[ i ].length; ++j )
			{
				int val = ( currentIndex < layoutSplit.length ) ?
					StringUtilities.parseInt( layoutSplit[ currentIndex++ ] ) : 0;
				LouvreManager.LouvreChoiceTable[ i ][ j ] = val;
			}
		}
	}

	public static final void saveMap()
	{
		StringBuffer map = new StringBuffer();

		for ( int i = 0; i < LouvreManager.LouvreChoiceTable.length; ++i )
		{
			for ( int j = 0; j < LouvreManager.LouvreChoiceTable[ i ].length; ++j )
			{
				if ( i != 0 || j != 0 )
				{
					map.append( ',' );
				}
				map.append( LouvreManager.LouvreChoiceTable[ i ][ j ] );
			}
		}

		Preferences.setInteger( "lastLouvreMap", KoLCharacter.getAscensions() );
		Preferences.setString( "louvreLayout", map.toString() );
	}

	public static final boolean louvreChoice( final int choice )
	{
		return choice >= LouvreManager.FIRST_CHOICE && choice <= LouvreManager.LAST_CHOICE;
	}

	public static final void resetDecisions()
	{
		for ( int i = 0; i < LouvreManager.LouvreGoalItems.length; ++i )
		{
			if ( GoalManager.hasGoal( LouvreManager.LouvreGoalItems[ i ] ) )
			{
				Preferences.setInteger( "louvreGoal", i + 1 );
				return;
			}
		}
		
		int goal = Preferences.getInteger( "louvreDesiredGoal" );

		if ( goal == LouvreManager.LouvreGoals.length + 1 )
		{
			if ( KoLCharacter.isMuscleClass() )
			{
				Preferences.setInteger( "louvreGoal", 4 );
			}
			else if ( KoLCharacter.isMysticalityClass() )
			{
				Preferences.setInteger( "louvreGoal", 5 );
			}
			else
			{
				Preferences.setInteger( "louvreGoal", 6 );
			}
		}
		else if ( goal == LouvreManager.LouvreGoals.length + 2 )
		{
			// Compare total subpoints acquired, rather than the
			// non-raw, calculated value for comparing which stat
			// should be chosen next.

			long mus = KoLCharacter.getTotalMuscle();
			long mys = KoLCharacter.getTotalMysticality();
			long mox = KoLCharacter.getTotalMoxie();

			if ( mus <= mys && mus <= mox )
			{
				Preferences.setInteger( "louvreGoal", 4 );
			}
			else if ( mys <= mus && mys <= mox )
			{
				Preferences.setInteger( "louvreGoal", 5 );
			}
			else
			{
				Preferences.setInteger( "louvreGoal", 6 );
			}
		}
		else
		{
			Preferences.setInteger( "louvreGoal", goal );
		}
	}

	private static final String currentGoalString()
	{
		LouvreManager.resetDecisions();
		int goal = Preferences.getInteger( "louvreGoal" );
		if ( goal <= 0 || goal > LouvreManager.LouvreGoals.length )
		{
			return "unknown";
		}
		return LouvreManager.LouvreGoals[ goal - 1 ];
	}

	public static final String handleChoice( final int source, final int stepCount )
	{
		// We only handle LouvreManager choices
		if ( !LouvreManager.louvreChoice( source ) )
		{
			return "";
		}

		String override = Preferences.getString( "louvreOverride" );
		if ( override.indexOf( "," ) != -1 )
		{
			String[] options = override.split( "\\s*,\\s*" );
			if ( options.length > stepCount )
			{
				if ( options[ stepCount ].equalsIgnoreCase( "up" ) )
				{
					return "1";
				}
				else if ( options[ stepCount ].equalsIgnoreCase( "down" ) )
				{
					return "2";
				}
				else
				{
					return "3";
				}
			}
		}

		// Get the routing tuple for this choice/goal
		LouvreManager.resetDecisions();
		int goal = Preferences.getInteger( "louvreGoal" );

		// Pick the best choice
		return LouvreManager.pickNewExit( source, goal );
	}

	// Node marking to prevent loops
	private static final boolean NodeMarks[] = new boolean[ LouvreManager.LAST_CHOICE - LouvreManager.FIRST_CHOICE + 1 ];

	private static final String pickNewExit( final int source, final int goal )
	{
		// Examine destinations and take shortest known path to goal
		int[] choices = LouvreManager.choiceTuple( source );
		int choice = 0;
		int hops = Integer.MAX_VALUE;

		for ( int i = 0; i < choices.length; ++i )
		{
			// Clear marks on nodes
			Arrays.fill( LouvreManager.NodeMarks, false );

			// Mark this node
			LouvreManager.NodeMarks[ source - LouvreManager.FIRST_CHOICE ] = true;

			// Determine how far destination is from goal
			int destination = choices[ i ];
			int dist = LouvreManager.hopsTo( 0, source, i, destination, goal );
			if ( dist < hops )
			{
				choice = i;
				hops = dist;
			}
		}

		return String.valueOf( choice + 1 );
	}

	private static final int hopsTo( int hops, final int source, final int which, int destination, final int goal )
	{
		// If destination is unknown, assume it is the most likely possibility.
		// Add 10 hops so that routes with fewer or no guesses are preferred.
		if ( destination == 0 )
		{
			hops += 10;
			float[] probs = probabilities( source )[ which ];
			float best = -1.0f;
			for ( int i = 0; i < 3; ++ i )
			{
				if ( probs[ i ] > best )
				{
					best = probs[ i ];
					destination = LouvreLocationExits[ source - LouvreManager.FIRST_CHOICE ][ i ];
				}
			}
		}
	
		// If destination is the goal, we're there
		if ( destination == goal )
		{
			return hops;
		}

		// If destination is another goal, can't get there from here
		if ( destination >= 1 && destination <= 6 )
		{
			return Integer.MAX_VALUE;
		}

		// If destination is a predicted but unmapped Escher node (all other
		// possibilities should have been eliminated above), we can reach any
		// goal, but prefer a more direct route.
		if ( !LouvreManager.louvreChoice( destination ) )
		{
			return hops + 100;
		}

		// Known destination. If we've been here before, punt
		if ( LouvreManager.NodeMarks[ destination - LouvreManager.FIRST_CHOICE ] )
		{
			return Integer.MAX_VALUE;
		}

		// Known destination visited for first time
		LouvreManager.NodeMarks[ destination - LouvreManager.FIRST_CHOICE ] = true;

		// Examine destinations and take shortest known path to goal
		int[] choices = LouvreManager.choiceTuple( destination );
		int nextHops = Integer.MAX_VALUE;

		for ( int i = 0; i < choices.length; ++i )
		{
			// Determine how far destination is from goal
			int dist = LouvreManager.hopsTo( hops + 1, destination, i, choices[ i ], goal );
			if ( dist < nextHops )
			{
				nextHops = dist;
			}
		}
		return nextHops;
	}

	public static final boolean mapChoice( final int lastChoice, final int lastDecision, final String text )
	{
		if ( !LouvreManager.louvreChoice( lastChoice ) )
		{
			return false;
		}

		// Punt if bogus decision
		if ( lastDecision < 1 || lastDecision > 3 )
		{
			return true;
		}

		// Return if we've already mapped this decision
		if ( LouvreManager.LouvreChoiceTable[ lastChoice - LouvreManager.FIRST_CHOICE ][ lastDecision - 1 ] != 0 )
		{
			return true;
		}

		Matcher choiceMatcher = LouvreManager.CHOICE_PATTERN.matcher( text );
		if ( choiceMatcher.find() )
		{
			int source = StringUtilities.parseInt( choiceMatcher.group( 1 ) );

			// Sanity check: we must stay within the LouvreManager
			if ( !LouvreManager.louvreChoice( source ) )
			{
				return false;
			}

			// Update the path table
			LouvreManager.mapChoice( lastChoice, lastDecision, source );
			return true;
		}

		// Perhaps we have reached a goal
		for ( int i = 0; i < LouvreManager.LouvreGoalStrings.length; ++i )
		{
			if ( text.indexOf( LouvreManager.LouvreGoalStrings[ i ] ) != -1 )
			{
				LouvreManager.mapChoice( lastChoice, lastDecision, i + 1 );
				return true;
			}
		}

		// Shouldn't get here
		return false;
	}

	private static final void mapChoice( final int choice, final int decision, final int destination )
	{
		int choices[] = LouvreManager.choiceTuple( choice );
		choices[ decision - 1 ] = destination;
		LouvreManager.saveMap();

		// If 2 choices have been discovered, 3rd might be knowable.
		int unknownIndex = -1;
		for ( int i = 0; i < 3; ++i )
		{
			if ( choices[ i ] != 0 )
			{
				continue;
			}
			if ( unknownIndex != -1 )
			{
				return;
			}
			unknownIndex = i;
		}

		// Done if all three destinations are known.
		if ( unknownIndex == -1 )
		{
			return;
		}

		// Find which exit has not been mapped
		int[] exits = LouvreManager.LouvreLocationExits[ choice - LouvreManager.FIRST_CHOICE ];
		for ( int i = 0; i < exits.length; ++i )
		{
			int exit = exits[ i ];

			// Can't deduce exit to one of four Escher drawings
			if ( exit == 0 )
			{
				continue;
			}

			boolean found = false;
			for ( int j = 0; j < choices.length; ++j )
			{
				if ( exit == choices[ j ] )
				{
					found = true;
					break;
				}
			}

			if ( !found )
			{
				choices[ unknownIndex ] = exit;
				LouvreManager.saveMap();
				return;
			}
		}
	}

	public static final String[][] choiceSpoilers( final int choice )
	{
		// We only handle LouvreManager choices
		if ( !LouvreManager.louvreChoice( choice ) )
		{
			return null;
		}

		// Return an array with the same structure as used by built-in
		// choice adventures.
		String[][] result = new String[ 3 ][];

		// The choice option is the first element
		result[ 0 ] = new String[ 1 ];
		result[ 0 ][ 0 ] = "choiceAdventure" + String.valueOf( choice );

		// The name of the choice is second element
		result[ 1 ] = new String[ 1 ];
		result[ 1 ][ 0 ] = LouvreManager.LouvreLocationNames[ choice - LouvreManager.FIRST_CHOICE ];

		// An array of choice spoilers is the third element
		int choices[] = LouvreManager.choiceTuple( choice );
		result[ 2 ] = new String[ 3 ];
		result[ 2 ][ 0 ] = LouvreManager.choiceName( choice, choices[ 0 ], 0 );
		result[ 2 ][ 1 ] = LouvreManager.choiceName( choice, choices[ 1 ], 1 );
		result[ 2 ][ 2 ] = LouvreManager.choiceName( choice, choices[ 2 ], 2 );

		return result;
	}

	public static final String encounterName( final int choice )
	{
		if ( !louvreChoice( choice ) )
		{
			return "";
		}

		String name = LouvreManager.LouvreShortLocationNames[ choice - LouvreManager.FIRST_CHOICE ];
		return "Louvre It or Leave It (" + name + ")";
	}

	private static final String choiceName( final int choice, final int destination, final int which )
	{
		switch ( destination )
		{
		case 0:
			// return "";
			float[] probs = probabilities( choice )[ which ];
			StringBuffer buf = new StringBuffer( "Calculated probabilities:" );
			for ( int i = 0; i < 3; ++i )
			{
				if ( probs[ i ] < 0.01f )
				{	// probably just a round-off error, not a significant chance
					continue;
				}
				buf.append( "<br>" );
				buf.append( Math.round(probs[ i ] * 100.0f) );
				buf.append( "% - " );
				int dest = LouvreManager.LouvreLocationExits[ choice - LouvreManager.FIRST_CHOICE ][ i ];
				if ( dest == 0 )
				{
					buf.append( "return to stairs" );
				}
				else if ( dest <= 6 )
				{
					buf.append( LouvreManager.LouvreGoals[ dest - 1 ] );
				}
				else
				{
					buf.append( LouvreManager.LouvreLocationNames[ dest - LouvreManager.FIRST_CHOICE ] );
				}
			}
			return buf.toString();
			
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
			return LouvreManager.LouvreGoals[ destination - 1 ];
		default:
			return LouvreManager.LouvreLocationNames[ destination - LouvreManager.FIRST_CHOICE ];
		}
	}

	public static final boolean freeAdventure( final String choice, final String decision )
	{
		// "choiceAdventureX"
		int source = StringUtilities.parseInt( choice.substring( 15 ) );

		// Make sure it's a LouvreManager adventure
		if ( !LouvreManager.louvreChoice( source ) )
		{
			return false;
		}

		// It is. If it stays within the LouvreManager, it's free
		int option = StringUtilities.parseInt( decision ) - 1;
		int destination = LouvreManager.LouvreChoiceTable[ source - LouvreManager.FIRST_CHOICE ][ option ];
		return LouvreManager.louvreChoice( destination );
	}
	
	// Derive probabilities for each of the 7 possible permutations.  One permutation (ABC)
	// has two entries, because there are two distinct RNG states that can produce it, and
	// it's sometimes possible to determine that only one of those two states is possible.
	// Values are in the range 0-1, and should always total to 1.
	private static final float[] derive( int location )
	{
		float probs[] = new float[] { 36/81f, 12/81f, 9/81f, 12/81f, 6/81f, 4/81f, 2/81f };
		if ( !LouvreManager.louvreChoice( location ) )
		{	// allow calls for an out-of-range location, to simplify edge cases
			return probs;
		}
		int choices[] = LouvreManager.choiceTuple( location );

		// Find which permutation matches the mapped exits
		int[] baseperm = LouvreManager.LouvreLocationExits[ location - LouvreManager.FIRST_CHOICE ];
		int perm = which( baseperm, choices[ 0 ] ) << 8 |
			which( baseperm, choices[ 1 ] ) << 4 |
			which( baseperm, choices[ 2 ] );
		
		// The first random number may be used twice, both to choose one of the three
		// return-to-stairs locations, and as the first step in scrambling the exits.
		// If we have a mapped Escher link, that can help determine the RNG state.
		boolean asc = any( choices, 95 );
		boolean nonasc = any( choices, 93 ) || any( choices, 94 );
			
		switch ( perm )
		{
		// No exits mapped:
		case 0x000:
			break;
			
		// All 3 exits mapped, or 2 mapped implying the 3rd:
		case 0xCBA:
		case 0x0BA:
		case 0xC0A:
		case 0xCB0:
			probs = new float[] { 1, 0, 0, 0, 0, 0, 0 };
			break;
		
		case 0xABC:
		case 0x0BC:
		case 0xA0C:
		case 0xAB0:
			if ( nonasc )
			{
				probs = new float[] { 0, 1, 0, 0, 0, 0, 0 };
			}
			else if ( asc )
			{
				probs = new float[] { 0, 0, 1, 0, 0, 0, 0 };
			}
			else
			{
				probs = new float[] { 0, 12/21f, 9/21f, 0, 0, 0, 0 };
			}
			break;
			
		case 0xACB:
		case 0x0CB:
		case 0xA0B:
		case 0xAC0:
			probs = new float[] { 0, 0, 0, 1, 0, 0, 0 };
			break;
			
		case 0xCAB:
		case 0x0AB:
		case 0xC0B:
		case 0xCA0:
			probs = new float[] { 0, 0, 0, 0, 1, 0, 0 };
			break;
			
		case 0xBCA:
		case 0x0CA:
		case 0xB0A:
		case 0xBC0:
			probs = new float[] { 0, 0, 0, 0, 0, 1, 0 };
			break;
		
		case 0xBAC:
		case 0x0AC:
		case 0xB0C:
		case 0xBA0:
			probs = new float[] { 0, 0, 0, 0, 0, 0, 1 };
			break;
			
		// Only one exit mapped
		case 0xA00:
			if ( nonasc )
			{
				probs = new float[] { 0, 1, 0, 0, 0, 0, 0 };
			}
			else if ( asc )
			{
				probs = new float[] { 0, 0, 9/21f, 12/21f, 0, 0, 0 };
			}
			else
			{
				probs = new float[] { 0, 12/33f, 9/33f, 12/33f, 0, 0, 0 };
			}
			break;
		
		case 0xB00:
			probs = new float[] { 0, 0, 0, 0, 0, 4/6f, 2/6f };
			break;

		case 0xC00:
			probs = new float[] { 36/42f, 0, 0, 0, 6/42f, 0, 0 };
			break;

		case 0x0A0:
			probs = new float[] { 0, 0, 0, 0, 6/8f, 0, 2/8f };
			break;

		case 0x0B0:
			probs = new float[] { 36/57f, 12/57f, 9/57f, 0, 0, 0, 0 };
			break;

		case 0x0C0:
			probs = new float[] { 0, 0, 0, 12/16f, 0, 4/16f, 0 };
			break;

		case 0x00A:
			probs = new float[] { 36/40f, 0, 0, 0, 0, 4/40f, 0 };
			break;

		case 0x00B:
			probs = new float[] { 0, 0, 0, 12/18f, 6/18f, 0, 0 };
			break;

		case 0x00C:
			probs = new float[] { 0, 12/23f, 9/23f, 0, 0, 0, 2/23f };
			break;
		
		default:
			System.out.println( "Impossible permutation: " +
				Integer.toHexString( perm ) );
		}
		
		return probs;
	}
	
	private static final int which( int[] perm, int dest )
	{
		dest = (dest == 0) ? -1 :
			(dest >= 92 && dest <= 95) ? 0 : dest;
		if ( dest == perm[ 0 ] ) return 0xA;
		if ( dest == perm[ 1 ] ) return 0xB;
		if ( dest == perm[ 2 ] ) return 0xC;
		return 0x0;
	}
	
	private static final boolean any( int[] choices, int dest )
	{
		return choices[ 0 ] == dest || choices[ 1 ] == dest ||
			choices[ 2 ] == dest;
	}
	
	private static final boolean definitive( float val )
	{
		return val == 0.0f || val == 1.0f;
	}

	// Predict values for permutation chances for a location.
	// We can no longer learn anything from adjacent locations,
	// due to the Great RNG Reseeding of '12.
	private static final float[] predict( int location )
	{
		float[] curr = derive( location );
		
		// Normalize the array so that the values add to 1
		float total = 0.0f;
		for ( int p = 0; p < 7; ++p )
		{
			total += curr[ p ];
		}
		if ( total == 1.0f || total == 0.0f ) return curr;
		for ( int p = 0; p < 7; ++p )
		{
			curr[ p ] /= total;
		}
		return curr;
	}
	
	// Calculate probabilities, indexed by <direction>, <base permutation index>
	// for possible exits.
	private static final float[][] probabilities( int location )
	{
		float[][] rv = new float[ 3 ][ 3 ];
		
		float[] probs = predict( location );
		float t;	// remaining probability to distribute
		final int A = 0, B = 1, C = 2;
		
		t = probs[ 0 ];
		rv[ 0 ][ C ] += t;
		rv[ 1 ][ B ] += t;
		rv[ 2 ][ A ] += t;
	
		t = probs[ 1 ] + probs[ 2 ];
		rv[ 0 ][ A ] += t;
		rv[ 1 ][ B ] += t;
		rv[ 2 ][ C ] += t;
	
		t = probs[ 3 ];
		rv[ 0 ][ A ] += t;
		rv[ 1 ][ C ] += t;
		rv[ 2 ][ B ] += t;
	
		t = probs[ 4 ];
		rv[ 0 ][ C ] += t;
		rv[ 1 ][ A ] += t;
		rv[ 2 ][ B ] += t;
	
		t = probs[ 5 ];
		rv[ 0 ][ B ] += t;
		rv[ 1 ][ C ] += t;
		rv[ 2 ][ A ] += t;
	
		t = probs[ 6 ];
		rv[ 0 ][ B ] += t;
		rv[ 1 ][ A ] += t;
		rv[ 2 ][ C ] += t;
	
		return rv;
	}

	// The Wiki has a LouvreManager Map:
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
		4,		// 13
		5,		// 14
		6,		// 15
		1,		// 16
		2,		// 17
		3,		// 18
	};

	private static final int mafiaCode( final int wikiCode )
	{
		if ( wikiCode == LouvreManager.RANDOM )
		{
			return 0;
		}
		return LouvreManager.WikiToMafia[ wikiCode ];
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

	private static final int wikiCode( final int mafiaCode )
	{
		// Map goals through one table
		if ( mafiaCode >= 1 && mafiaCode <= 6 )
		{
			return LouvreManager.MafiaGoalToWiki[ mafiaCode - 1 ];
		}

		// Map destinations through another table
		if ( mafiaCode >= LouvreManager.FIRST_CHOICE && mafiaCode <= LouvreManager.LAST_CHOICE )
		{
			return LouvreManager.MafiaLocationToWiki[ mafiaCode - LouvreManager.FIRST_CHOICE ];
		}

		// Otherwise, just return max value
		return LouvreManager.RANDOM;
	}

	private static int WikiLouvreLocationExits[][];

	static
	{
		LouvreManager.buildWikiExits();
	}

	private static final void buildWikiExits()
	{
		// Get a zeroed array to start things off.
		LouvreManager.WikiLouvreLocationExits = new int[ LouvreManager.LAST_CHOICE - LouvreManager.FIRST_CHOICE + 1 ][ 3 ];

		// Examine each node in Mafia order
		for ( int source = LouvreManager.FIRST_CHOICE; source <= LouvreManager.LAST_CHOICE; ++source )
		{
			// Get the array of exit paths
			int mafiaExits[] = LouvreManager.LouvreLocationExits[ source - LouvreManager.FIRST_CHOICE ];
			int wikiExits[] = LouvreManager.WikiLouvreLocationExits[ LouvreManager.wikiCode( source ) ];

			// Copy translated exit from Mafia exit table to Wiki exit table
			for ( int i = 0; i < mafiaExits.length; ++i )
			{
				wikiExits[ i ] = LouvreManager.wikiCode( mafiaExits[ i ] );
			}

			// Sort the exits in Wiki order
			Arrays.sort( wikiExits );
		}
	}

	// Gemelli has a tool that accepts a code and displays the map
	// corresponding to it:
	//
	// originally: http://www.feesher.com/louvre_mapper.php
	// now: http://louvre.bewarethefgc.com
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

	public static final String gemelliCode()
	{
		int code[] = new int[ 48 ];
		int codeIndex = 0;

		// Examine each node in Wiki order: 0 - 12
		for ( int i = 0; i < LouvreManager.LouvreChoiceTable.length; ++i )
		{
			// Get the choice adventure # corresponding to the Wiki code
			int source = LouvreManager.mafiaCode( i );

			// Get the array of exit paths
			int paths[] = LouvreManager.LouvreChoiceTable[ source - LouvreManager.FIRST_CHOICE ];

			// For each choice in Wiki order
			int exits[] = LouvreManager.WikiLouvreLocationExits[ i ];

			for ( int j = 0; j < exits.length; ++j )
			{
				// Find the exit in the paths
				for ( int index = 0; index < paths.length; ++index )
				{
					int destination = LouvreManager.wikiCode( paths[ index ] );

					// Ignore unmapped paths
					if ( destination == LouvreManager.RANDOM )
					{
						continue;
					}

					// If this is the random exit...
					if ( exits[ j ] == LouvreManager.RANDOM )
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

					if ( exits[ j ] == destination )
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

		for ( int i = 4; i < LouvreManager.LouvreChoiceTable.length; ++i )
		{
			// Get the choice adventure # corresponding to the Wiki code
			int source = LouvreManager.mafiaCode( i );

			// Get the array of exit paths
			int paths[] = LouvreManager.LouvreChoiceTable[ source - LouvreManager.FIRST_CHOICE ];

			// Examine each exit and determine which one is random
			int random = 0;
			for ( int j = 0; j < paths.length; ++j )
			{
				int exit = paths[ j ];
				if ( exit < LouvreManager.FIRST_CHOICE )
				{
					continue;
				}
				exit -= LouvreManager.FIRST_CHOICE;
				if ( exit > 3 )
				{
					continue;
				}
				random = exit;
				break;
			}
			code[ codeIndex ] = random;
			++codeIndex;
		}

		// Convert the 48 element int array into a 24 character character array
		char data[] = new char[ 24 ];
		for ( int i = 0; i < code.length; i += 2 )
		{
			int hexDigit = code[ i ] * 4 + code[ i + 1 ];
			data[ i / 2 ] = Character.forDigit( hexDigit, 16 );
		}

		return String.valueOf( data );
	}

	public static final void showGemelliMap()
	{
		RelayLoader.openSystemBrowser( "http://louvre.bewarethefgc.com/index.php?mapstring=" + LouvreManager.gemelliCode() );
	}

	public static final void addGoalButton( final StringBuffer buffer )
	{
		String goal = LouvreManager.currentGoalString();
		ChoiceManager.addGoalButton( buffer, goal );
	}
}
