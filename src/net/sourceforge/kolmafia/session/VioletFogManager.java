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
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.RelayLoader;

public abstract class VioletFogManager
{
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );

	// Range of choice numbers within the fog

	private static final int FIRST_CHOICE = 48;
	private static final int LAST_CHOICE = 70;

	// The various locations within the violet fog

	private static final String FogLocationNames [] =
	{
		"Violet Fog (Start)",		// 48
		"Man on Bicycle",		// 49
		"Pleasant-faced Man",		// 50
		"Man on Cornflake",		// 51
		"Giant Chessboard",		// 52
		"Mustache",			// 53
		"Birds",			// 54
		"Machine Elves",		// 55
		"Boat on a River",		// 56
		"Man in Sunglasses",		// 57
		"Caterpiller",			// 58
		"This is not a Pipe",		// 59
		"Chorus Girls",			// 60
		"Huge Mountain",		// 61
		"Eye with Hat",			// 62
		"Eye with Weapon",		// 63
		"Eye with Garment",		// 64
		"Muscle Training",		// 65
		"Myst Training",		// 66
		"Moxie Training",		// 67
		"Alcohol Fish",			// 68
		"Food Fish",			// 69
		"Medicine Fish",		// 70
	};

	private static final int FogLocationExits [][] =
	{
		{ 49, 50, 51 },			// 48
		{ 52, 53, 56 },			// 49
		{ 53, 54, 57 },			// 50
		{ 52, 54, 55 },			// 51
		{ 61, 65, 68 },			// 52
		{ 61, 66, 69 },			// 53
		{ 61, 67, 70 },			// 54
		{ 58, 65, 70 },			// 55
		{ 59, 66, 68 },			// 56
		{ 60, 67, 69 },			// 57
		{ 51, 52, 63 },			// 58
		{ 49, 53, 62 },			// 59
		{ 50, 54, 64 },			// 60
		{ 49, 50, 51 },			// 61
		{ 50, 52, 61 },			// 62
		{ 51, 53, 61 },			// 63
		{ 49, 54, 61 },			// 64
		{ 50, 51, 54 },			// 65
		{ 49, 51, 52 },			// 66
		{ 49, 50, 53 },			// 67
		{ 49, 50, 53 },			// 68
		{ 50, 51, 54 },			// 69
		{ 49, 51, 52 },			// 70
	};

	// The routing table.
	//
	// One row for each fog location (48 - 70)
	// Each row contains one tuple for each possible fog destination (49 - 70)
	// Each tuple contains the Next Hop and the Hop Count to get there

	private static int FogRoutingTable[][][];

	private static final int[] routingTuple( final int source, final int destination )
	{
		if ( source < VioletFogManager.FIRST_CHOICE || source > VioletFogManager.LAST_CHOICE || destination < VioletFogManager.FIRST_CHOICE + 1 || destination > VioletFogManager.LAST_CHOICE )
		{
			return null;
		}
		return VioletFogManager.FogRoutingTable[ source - VioletFogManager.FIRST_CHOICE ][ destination - VioletFogManager.FIRST_CHOICE - 1 ];
	}

	private static final int nextHop( final int source, final int destination )
	{
		int[] tuple = VioletFogManager.routingTuple( source, destination );
		return tuple == null ? -1 : tuple[ 0 ];
	}

	static
	{
		VioletFogManager.buildRoutingTable();
		// printRoutingTable();
	}

	private static final void buildRoutingTable()
	{
		// Get a zeroed array to start things off.
		VioletFogManager.FogRoutingTable =
			new int[ VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE + 1 ][ VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE ][ 2 ];
		int unfilled =
			( VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE + 1 ) * ( VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE );

		// Seed it with final destinations: next hop = -1 and hopcount = 0
		for ( int source = VioletFogManager.FIRST_CHOICE + 1; source <= VioletFogManager.LAST_CHOICE; ++source )
		{
			int tuple[] = VioletFogManager.routingTuple( source, source );
			tuple[ 0 ] = -1;
			tuple[ 1 ] = 0;
			--unfilled;
		}

		// Seed it with exit destinations: next hop = destination and hopcount = 1
		for ( int source = VioletFogManager.FIRST_CHOICE; source <= VioletFogManager.LAST_CHOICE; ++source )
		{
			int exits[] = VioletFogManager.FogLocationExits[ source - VioletFogManager.FIRST_CHOICE ];
			for ( int i = 0; i < exits.length; ++i )
			{
				int destination = exits[ i ];
				int tuple[] = VioletFogManager.routingTuple( source, destination );
				tuple[ 0 ] = destination;
				tuple[ 1 ] = 1;
				--unfilled;
			}
		}

		// Now iterate over entire table calculating next hops and hopcounts
		while ( unfilled > 0 )
		{
			int filled = 0;

			for ( int source = VioletFogManager.FIRST_CHOICE; source <= VioletFogManager.LAST_CHOICE; ++source )
			{
				for ( int destination = VioletFogManager.FIRST_CHOICE + 1; destination <= VioletFogManager.LAST_CHOICE; ++destination )
				{
					int tuple[] = VioletFogManager.routingTuple( source, destination );

					// If we've calculated this tuple, skip it
					if ( tuple[ 0 ] != 0 )
					{
						continue;
					}

					// See which of our direct exits can get there fastest
					int nextHop = 0;
					int hopCount = Integer.MAX_VALUE;

					int exits[] = VioletFogManager.FogLocationExits[ source - VioletFogManager.FIRST_CHOICE ];
					for ( int i = 0; i < exits.length; ++i )
					{
						int destTuple[] = VioletFogManager.routingTuple( exits[ i ], destination );
						if ( destTuple[ 0 ] != 0 && destTuple[ 1 ] < hopCount )
						{
							nextHop = exits[ i ];
							hopCount = destTuple[ 1 ];
						}
					}

					// If we found a route, enter it into table
					if ( nextHop != 0 )
					{
						tuple[ 0 ] = nextHop;
						tuple[ 1 ] = hopCount + 1;
						++filled;
					}
				}
			}

			if ( filled == 0 )
			{
				RequestLogger.printLine( "Internal error: " + unfilled + " unreachable nodes in violet fog routing table" );
				break;
			}

			unfilled -= filled;
		}
	}

	// Range of choice numbers with a goal
	private static final int FIRST_GOAL_LOCATION = 62;
	public static final String FogGoals [] =
	{
		"escape from the fog",		// 48-61
		"Cerebral Cloche",		// 62
		"Cerebral Crossbow",		// 63
		"Cerebral Culottes",		// 64
		"Muscle Training",		// 65
		"Mysticality Training",		// 66
		"Moxie Training",		// 67
		"ice stein",			// 68
		"munchies pill",		// 69
		"homeopathic healing powder",	// 70
		"Boost Prime Stat",
		"Boost Lowest Stat"
	};
	private static final AdventureResult FogGoalItems[] =
	{
		null,
		ItemPool.get( ItemPool.C_CLOCHE, 1 ),
		ItemPool.get( ItemPool.C_CROSSBOW, 1 ),
		ItemPool.get( ItemPool.C_CULOTTES, 1 ),
		null,
		null,
		null,
		ItemPool.get( ItemPool.ICE_STEIN, 1 ),
		ItemPool.get( ItemPool.MUNCHIES_PILL, 1 ),
		ItemPool.get( ItemPool.HOMEOPATHIC, 1 ),
	};

	// The choice table.
	//
	// One row for each fog location (48 - 70)
	// Each row contains four values, corresponding to choices 1 - 4
	//
	// -1	The "goal"
	//  0	Unknown
	// xx	A destination

	private static final int FogChoiceTable[][] = new int[ VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE + 1 ][ 4 ];

	public static final void reset()
	{
		// Reset what we've "learned" about the fog choices
		for ( int i = VioletFogManager.FIRST_CHOICE; i <= VioletFogManager.LAST_CHOICE; ++i )
		{
			int choice[] = VioletFogManager.FogChoiceTable[ i - VioletFogManager.FIRST_CHOICE ];
			choice[ 0 ] = i < VioletFogManager.FIRST_GOAL_LOCATION ? 0 : -1;
			choice[ 1 ] = 0;
			choice[ 2 ] = 0;
			choice[ 3 ] = i < VioletFogManager.FIRST_GOAL_LOCATION ? -1 : 0;
		}

		int lastVioletFogAscension = Preferences.getInteger( "lastVioletFogMap" );
		if ( lastVioletFogAscension != KoLCharacter.getAscensions() )
		{
			Preferences.setInteger( "lastVioletFogMap", KoLCharacter.getAscensions() );
			Preferences.setString( "violetFogLayout", "" );
		}

		String layout = Preferences.getString( "violetFogLayout" );
		if ( layout.equals( "" ) )
		{
			return;
		}

		int currentIndex = 0;
		String[] layoutSplit = layout.split( "," );

		for ( int i = 0; i < VioletFogManager.FogChoiceTable.length; ++i )
		{
			for ( int j = 0; j < VioletFogManager.FogChoiceTable[ i ].length; ++j )
			{
				VioletFogManager.FogChoiceTable[ i ][ j ] = StringUtilities.parseInt( layoutSplit[ currentIndex++ ] );
			}
		}
	}

	public static final void saveMap()
	{
		StringBuilder map = new StringBuilder();

		for ( int i = 0; i < VioletFogManager.FogChoiceTable.length; ++i )
		{
			for ( int j = 0; j < VioletFogManager.FogChoiceTable[ i ].length; ++j )
			{
				if ( i != 0 || j != 0 )
				{
					map.append( ',' );
				}
				map.append( VioletFogManager.FogChoiceTable[ i ][ j ] );
			}
		}

		Preferences.setInteger( "lastVioletFogMap", KoLCharacter.getAscensions() );
		Preferences.setString( "violetFogLayout", map.toString() );
	}

	private static final String currentGoalString()
	{
		int goal = Preferences.getInteger( "violetFogGoal" );

		if ( goal < 0 || goal > 11 )
		{
			return "unknown";
		}
		
		if ( goal == 10 )	// Boost Prime Stat
		{
			goal = KoLCharacter.getPrimeIndex() + 4;
		}
		else if ( goal == 11 )	// Boost Lowest Stat
		{
			long mus = KoLCharacter.getTotalMuscle();
			long mys = KoLCharacter.getTotalMysticality();
			long mox = KoLCharacter.getTotalMoxie();

			if ( mus <= mys && mus <= mox )
			{
				goal = 4;
			}
			else if ( mys <= mus && mys <= mox )
			{
				goal = 5;
			}
			else
			{
				goal = 6;
			}
		}

		return VioletFogManager.FogGoals[ goal ];
	}

	public static final boolean fogChoice( final int choice )
	{
		return choice >= VioletFogManager.FIRST_CHOICE && choice <= VioletFogManager.LAST_CHOICE;
	}

	public static final String handleChoice( final int source )
	{
		// We only handle Violet Fog choices
		if ( !VioletFogManager.fogChoice( source ) )
		{
			return "";
		}

		// Get the user specified goal
		int goal = Preferences.getInteger( "violetFogGoal" );
		for ( int i = 0; i < VioletFogManager.FogGoalItems.length; ++i )
		{
			if ( VioletFogManager.FogGoalItems[ i ] != null &&
				GoalManager.hasGoal( VioletFogManager.FogGoalItems[ i ] ) )
			{
				goal = i;
				break;
			}
		}

		// If no goal, return "4".
		// - If we are not at a "goal" location, this will exit the fog
		// - If we are at a "goal" location, this will send us to a non-"goal" location
		if ( goal == 0 )
		{
			return "4";
		}
		
		if ( goal == 10 )	// Boost Prime Stat
		{
			goal = KoLCharacter.getPrimeIndex() + 4;
		}
		else if ( goal == 11 )	// Boost Lowest Stat
		{
			long mus = KoLCharacter.getTotalMuscle();
			long mys = KoLCharacter.getTotalMysticality();
			long mox = KoLCharacter.getTotalMoxie();

			if ( mus <= mys && mus <= mox )
			{
				goal = 4;
			}
			else if ( mys <= mus && mys <= mox )
			{
				goal = 5;
			}
			else
			{
				goal = 6;
			}
		}

		// Find the location we must get to to achieve the goal
		int destination = VioletFogManager.FIRST_GOAL_LOCATION + goal - 1;
		if ( !VioletFogManager.fogChoice( destination ) )
		{
			return "";
		}

		// Are we there yet?
		if ( source == destination )
		{
			// The first decision will get us the goal we seek
			return "1";
		}

		// We haven't reached the goal yet. Find the next hop.
		int nextHop = VioletFogManager.nextHop( source, destination );

		// Choose the path that will take us there
		int path[] = VioletFogManager.FogChoiceTable[ source - VioletFogManager.FIRST_CHOICE ];
		for ( int i = 0; i < path.length; ++i )
		{
			if ( path[ i ] == nextHop )
			{
				return String.valueOf( i + 1 );
			}
		}

		// We don't know how to get there. Pick an unexplored path.
		for ( int i = 0; i < path.length; ++i )
		{
			if ( path[ i ] == 0 )
			{
				// We don't know how to get to the Next Hop
				return String.valueOf( i + 1 );
			}
		}

		// This shouldn't happen
		return "";
	}

	public static final boolean mapChoice( final int lastChoice, final int lastDecision, final String text )
	{
		if ( !VioletFogManager.fogChoice( lastChoice ) )
		{
			return false;
		}

		// Punt if bogus decision
		if ( lastDecision < 1 || lastDecision > 4 )
		{
			return true;
		}

		// Return if we've already mapped this decision
		if ( VioletFogManager.FogChoiceTable[ lastChoice - VioletFogManager.FIRST_CHOICE ][ lastDecision - 1 ] != 0 )
		{
			return true;
		}

		Matcher choiceMatcher = VioletFogManager.CHOICE_PATTERN.matcher( text );
		if ( !choiceMatcher.find() )
		{
			return false;
		}

		int source = StringUtilities.parseInt( choiceMatcher.group( 1 ) );

		// We only handle Violet Fog choices
		if ( !VioletFogManager.fogChoice( source ) )
		{
			return false;
		}

		// Update the path table
		int choices[] = VioletFogManager.FogChoiceTable[ lastChoice - VioletFogManager.FIRST_CHOICE ];
		choices[ lastDecision - 1 ] = source;
		VioletFogManager.saveMap();

		// See if exactly one exit is unknown
		int unknownIndex = -1;
		for ( int i = 0; i < choices.length; ++i )
		{
			if ( choices[ i ] != 0 )
			{
				continue;
			}
			if ( unknownIndex != -1 )
			{
				return true;
			}
			unknownIndex = i;
		}

		// Done if all three destinations are known.
		if ( unknownIndex == -1 )
		{
			return true;
		}

		// Yes. Figure out which one it is
		int exits[] = VioletFogManager.FogLocationExits[ lastChoice - VioletFogManager.FIRST_CHOICE ];
		for ( int i = 0; i < exits.length; ++i )
		{
			int exit = exits[ i ];
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
				VioletFogManager.saveMap();
				return true;
			}
		}

		return true;
	}

	public static final String[][] choiceSpoilers( final int choice )
	{
		// We only handle Violet Fog choices
		if ( !VioletFogManager.fogChoice( choice ) )
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
		result[ 1 ][ 0 ] = VioletFogManager.FogLocationNames[ choice - VioletFogManager.FIRST_CHOICE ];

		// An array of choice spoilers is the third element
		int choices[] = VioletFogManager.FogChoiceTable[ choice - VioletFogManager.FIRST_CHOICE ];
		result[ 2 ] = new String[ 4 ];
		result[ 2 ][ 0 ] = VioletFogManager.choiceName( choice, choices[ 0 ] );
		result[ 2 ][ 1 ] = VioletFogManager.choiceName( choice, choices[ 1 ] );
		result[ 2 ][ 2 ] = VioletFogManager.choiceName( choice, choices[ 2 ] );
		result[ 2 ][ 3 ] = VioletFogManager.choiceName( choice, choices[ 3 ] );

		return result;
	}

	private static final String choiceName( final int choice, final int destination )
	{
		// If it's unknown, no name
		if ( destination == 0 )
		{
			return "";
		}

		// If it's the Goal, pick the goal
		if ( destination == -1 )
		{
			return choice < VioletFogManager.FIRST_GOAL_LOCATION ? VioletFogManager.FogGoals[ 0 ] : VioletFogManager.FogGoals[ choice - VioletFogManager.FIRST_GOAL_LOCATION + 1 ];
		}

		// Otherwise, return the name of the destination
		return VioletFogManager.FogLocationNames[ destination - VioletFogManager.FIRST_CHOICE ];
	}

	public static final boolean freeAdventure( final String choice, final String decision )
	{
		// "choiceAdventureX"
		int source = StringUtilities.parseInt( choice.substring( 15 ) );

		// Journey to the Center of your Mind
		if ( source == 71 )
		{
			// We got diverted from where we thought we were going
			Preferences.setString( "lastAdventure", "A Journey to the Center of Your Mind" );
			// Switch location to the trip of choice.
			String name = "";
			if ( decision.equals( "1" ) )
			{
				name = "An Incredibly Strange Place (Bad Trip)";
			}
			else if ( decision.equals( "2" ) )
			{
				name = "An Incredibly Strange Place (Mediocre Trip)";
			}
			else if ( decision.equals( "3" ) )
			{
				name = "An Incredibly Strange Place (Great Trip)";
			}

			Preferences.setString( "chosenTrip", name );
			return true;
		}

		// Make sure it's a fog adventure
		if ( !VioletFogManager.fogChoice( source ) )
		{
			return false;
		}

		// It is. If it's a "goal" location, decision "1" takes an adventure.
		return source < VioletFogManager.FIRST_GOAL_LOCATION ? true : !decision.equals( "1" );
	}

	// The Wiki has a Violet Fog Map:
	//
	//     http://kol.coldfront.net/thekolwiki/index.php/Violet_Fog_Map
	//
	// The Wiki's numbering scheme mapped to Choice Adventure number:
	//
	//  1 = 61 (Huge Mountain)
	//  2 = 49 (Man on Bicycle)
	//  3 = 52 (Giant Chessboard)
	//  4 = 68 (Alcohol Fish)
	//  5 = 56 (Boat on a River)
	//  6 = 50 (Pleasant-faced Man)
	//  7 = 53 (Mustache)
	//  8 = 51 (Man on Cornflake)
	//  9 = 55 (Machine Elves)
	// 10 = 70 (Medicine Fish)
	// 11 = 54 (Birds)
	// 12 = 67 (Moxie Training)
	// 13 = 57 (Man in Sunglasses)
	// 14 = 60 (Chorus Girls)
	// 15 = 64 (Eye with Garment)
	// 16 = 66 (Mysticality Training)
	// 17 = 65 (Muscle Training)
	// 18 = 69 (Food Fish)
	// 19 = 58 (Caterpillar)
	// 20 = 59 (This is not a Pipe)
	// 21 = 62 (Eye with Hat)
	// 22 = 63 (Eye with Weapon)

	private static final int WikiToMafia [] =
	{
		61,		// 1
		49,		// 2
		52,		// 3
		68,		// 4
		56,		// 5
		50,		// 6
		53,		// 7
		51,		// 8
		55,		// 9
		70,		// 10
		54,		// 11
		67,		// 12
		57,		// 13
		60,		// 14
		64,		// 15
		66,		// 16
		65,		// 17
		69,		// 18
		58,		// 19
		59,		// 20
		62,		// 21
		63,		// 22
	};

	private static final int mafiaCode( final int wikiCode )
	{
		return VioletFogManager.WikiToMafia[ wikiCode - 1 ];
	}

	private static final int MafiaToWiki [] =
	{
		2,		// 49
		6,		// 50
		8,		// 51
		3,		// 52
		7,		// 53
		11,		// 54
		9,		// 55
		5,		// 56
		13,		// 57
		19,		// 58
		20,		// 59
		14,		// 60
		1,		// 61
		21,		// 62
		22,		// 63
		15,		// 64
		17,		// 65
		16,		// 66
		12,		// 67
		4,		// 68
		18,		// 69
		10,		// 70
	};

	private static final int wikiCode( final int mafiaCode )
	{
		return VioletFogManager.MafiaToWiki[ mafiaCode - VioletFogManager.FIRST_CHOICE - 1 ];
	}

	private static int WikiFogLocationExits[][];

	static
	{
		VioletFogManager.buildWikiExits();
	}

	private static final void buildWikiExits()
	{
		// Get a zeroed array to start things off.
		VioletFogManager.WikiFogLocationExits = new int[ VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE ][ 3 ];

		// Examine each node in Mafia order
		for ( int source = VioletFogManager.FIRST_CHOICE + 1; source <= VioletFogManager.LAST_CHOICE; ++source )
		{
			// Get the array of exit paths
			int mafiaExits[] = VioletFogManager.FogLocationExits[ source - VioletFogManager.FIRST_CHOICE ];
			int wikiExits[] = VioletFogManager.WikiFogLocationExits[ VioletFogManager.wikiCode( source ) - 1 ];

			// Copy translated exit from Mafia exit table to Wiki exit table
			for ( int i = 0; i < mafiaExits.length; ++i )
			{
				wikiExits[ i ] = VioletFogManager.wikiCode( mafiaExits[ i ] );
			}

			// Sort the exits in Wiki order
			Arrays.sort( wikiExits );
		}
	}

	// Gemelli has a tool that accepts a code and displays the map
	// corresponding to it:
	//
	// originally: http://www.feesher.com/fog_mapper.php
	// now: http://fog.bewarethefgc.com
	//
	// To get the code, examine the 22 nodes in Wiki order
	// Examine each of the three destinations from each node, again in Wiki order.
	// Generate a digit from 0-3:
	//     0 = unmapped
	//     1 = this way
	//     2 = that way
	//     3 = the other way
	//
	// Take the resulting 66 digit string and convert two digits at a time
	// from base 4 to base 16, resulting in a 33 digit hex string

	public static final String gemelliCode()
	{
		int code[] = new int[ 66 ];
		int codeIndex = 0;

		// Examine each node in Wiki order
		for ( int i = 1; i < VioletFogManager.FogChoiceTable.length; ++i )
		{
			// Get the choice adventure # corresponding to the Wiki code
			int source = VioletFogManager.mafiaCode( i );

			// Get the array of exit paths
			int paths[] = VioletFogManager.FogChoiceTable[ source - VioletFogManager.FIRST_CHOICE ];

			// For each choice in Wiki order
			int exits[] = VioletFogManager.WikiFogLocationExits[ i - 1 ];

			for ( int j = 0; j < exits.length; ++j )
			{
				// Find the exit in the paths
				for ( int index = 0; index < paths.length; ++index )
				{
					if ( paths[ index ] == VioletFogManager.mafiaCode( exits[ j ] ) )
					{
						int choice = source < VioletFogManager.FIRST_GOAL_LOCATION ? index + 1 : index;
						code[ codeIndex ] = choice;
						break;
					}
				}
				++codeIndex;
			}
		}

		// Convert the 66 element int array into a 33 character character array
		char data[] = new char[ 33 ];
		for ( int i = 0; i < code.length; i += 2 )
		{
			int hexDigit = code[ i ] * 4 + code[ i + 1 ];
			data[ i / 2 ] = Character.forDigit( hexDigit, 16 );
		}

		return String.valueOf( data );
	}

	public static final void showGemelliMap()
	{
		RelayLoader.openSystemBrowser( "http://fog.bewarethefgc.com/index.php?mapstring=" + VioletFogManager.gemelliCode() );
	}

	public static final void addGoalButton( final StringBuffer buffer )
	{
		String goal = VioletFogManager.currentGoalString();
		ChoiceManager.addGoalButton( buffer, goal );
	}
}
