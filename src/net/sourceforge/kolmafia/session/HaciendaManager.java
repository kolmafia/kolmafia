/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import java.lang.StringBuilder;
import java.util.ArrayList;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.TavernRequest;

import net.sourceforge.kolmafia.swingui.CouncilFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;


public class HaciendaManager
{
	private static final String[][] CLUES = 
	{
		{	"a potato peeler", "0"	},
		{	"an empty sardine can", "1"	},
		{	"an apple core", "2"	},
		{	"a silver pepper-mill", "3"	},
		{	"the lid from a can of sterno", "4"	},
		{	"an empty teacup", "5"	},
		{	"a small crowbar", "6"	},
		{	"a pair of needle-nose pliers", "7"	},
		{	"an empty rifle cartridge", "8"	},
		{	"a long nightcap with a pom-pom on the end", "9"	},
		{	"a dirty sock", "10"	},
		{	"a toothbrush", "11"	},
		{	"an errant cube of chalk from the pool table", "15"	}
	};
	
	private static final String[] REWARDS = 
	{
		"silver cheese-slicer",
		"taco shells",
		"fettucini Inconnu",
		"silver salt-shaker",
		"cans of sterno",
		"silver paté knife",
		"fancy beef jerky",
		"pipe wrench",
		"gun cleaning kit",
		"sleep mask",
		"sock garters",
		"mariachi toothpaste",
		"heavy leather-bound tome",
		"large handful of meat",
		"leather bookmark",
		"ivory cue ball",
		"decanter of fine Scotch",
		"expensive cigar"
	};
	
	public static void parseRoom( final int lastChoice, final int lastDecision, final String text )
	{
		String haciendaLayout = Preferences.getString( "haciendaLayout" );
		StringBuffer newLayout = new StringBuffer( haciendaLayout );
		
		int currentSearch = lastChoice*3 + lastDecision - 1240;
		
		int clueTo = HaciendaManager.returnClue( text );
		
		// If a fight, mark F
		if ( text.contains( "Fight!" ) )
		{
			newLayout.setCharAt( currentSearch, 'F' );
		}
		// If a key, mark K
		else if ( text.contains( "hacienda key" ) )
		{
			newLayout.setCharAt( currentSearch, 'K' );
		}
		// If a clue, mark C, and mark clue location k if not already K
		else if ( clueTo != -1 )
		{
			newLayout.setCharAt( currentSearch, 'C' );
			if ( haciendaLayout.charAt( clueTo ) != 'K' )
			{
				newLayout.setCharAt( clueTo, 'k' );
			}
			KoLmafia.updateDisplay( "You have found a clue: " + HaciendaManager.getClue( text ) );
		}
		else if ( text.contains( "acquire an item" ) || text.contains( "large handful of meat" ) )
		{
			// Verify that it's an reward from here, not elsewhere
			if ( HaciendaManager.verifyReward( text ) )
			{
				newLayout.setCharAt( currentSearch, 'R' );
			}
		}
		else
		{
			// Not sure if this will ever happen, but nothing found, so mark X
			if ( haciendaLayout.charAt( currentSearch ) == '0' )
			{
				newLayout.setCharAt( currentSearch, 'X' );
			}
		}
		
		// Record layout		
		haciendaLayout = newLayout.toString();
		Preferences.setString( "haciendaLayout", haciendaLayout );

		// Check if quest completion was properly completed, and if not, update Hacienda layout
		if ( Preferences.getString( "questG04Nemesis" ).equals( "finished" ) )
		{
			if ( HaciendaManager.countString( haciendaLayout.toLowerCase(), "f" ) > 0 )
			{
				HaciendaManager.questCompleted();
				haciendaLayout = Preferences.getString( "haciendaLayout" );
				newLayout = new StringBuffer( haciendaLayout );
			}
		}

		// See if we can now learn things ?

		for ( int i=0 ; i < 6 ; i++ )
		{
			String room = haciendaLayout.substring( i*3, i*3+3 );

			// Nemesis quest complete, so no fights encountered, and pattern is now two rewards and one Key/clue
			if ( Preferences.getString( "questG04Nemesis" ).equals( "finished" ) )
			{
				for ( int j=0 ; j < 3 ; j++ )
				{
					int currentCheck = i*3+j;
					if ( haciendaLayout.charAt( currentCheck ) == '0' )
					{
						// If we have got a Key or Clue in the Room remaining one is a Reward
						if ( room.contains( "K" ) || room.contains( "C" ) )
						{
							newLayout.setCharAt( currentCheck, 'r' );
						}
						// If there are two Rewards, remaining one is a Clue
						else if ( HaciendaManager.countString( room.toLowerCase(), "r" ) == 2 )
						{
							newLayout.setCharAt( currentCheck, 'C' );
						}
					}
				}
			}
			else
			{
				// Do if Nemesis quest incomplete
				for ( int j=0 ; j < 3 ; j++ )
				{
					int currentCheck = i*3+j;
					if ( haciendaLayout.charAt( currentCheck ) == '0' )
					{
						// If we have got a Key or Clue and a Fight in the Room remaining one is a Reward
						if ( ( room.toLowerCase().contains( "k" ) || room.toLowerCase().contains( "c" ) || room.toLowerCase().contains( "u" ) ) &&
							room.contains( "F" ) )
						{
							newLayout.setCharAt( currentCheck, 'r' );
						}
						// If we have got a Key or Clue and a Reward in the Room remaining one is a Fight
						else if ( ( room.toLowerCase().contains( "k" ) || room.toLowerCase().contains( "c" ) || room.toLowerCase().contains( "u" ) ) &&
							room.contains( "R" ) )
						{
							newLayout.setCharAt( currentCheck, 'f' );
						}
						// If we have got a Fight and a Reward in the Room remaining one is a Key or Clue
						else if ( room.contains( "F" ) && room.contains( "R" ) )
						{
							// If we have found 4 keys, it's a clue
							if( HaciendaManager.countString( haciendaLayout.toLowerCase(), "k" ) == 4 )
							{
								newLayout.setCharAt( currentCheck, 'c' );
							}
							// If we have found 2 clues, it's a key
							else if ( HaciendaManager.countString( haciendaLayout.toLowerCase(), "c" ) == 2 )
							{
								newLayout.setCharAt( currentCheck, 'k' );
							}
							else
							{
								newLayout.setCharAt( currentCheck, 'u' );
							}
						}
					}
				}
			}
		}
		
		// Record layout		
		Preferences.setString( "haciendaLayout", newLayout.toString() );
	}

	public static void questCompleted()
	{
		String haciendaLayout = Preferences.getString( "haciendaLayout" );
		StringBuffer newLayout = new StringBuffer( haciendaLayout );

		for ( int i=0 ; i < 6 ; i++ )
		{
			String room = haciendaLayout.substring( i*3, i*3+3 );

			// Replace any u's with C's (as you must have found all keys), and all F's with R's
			for ( int j=0 ; j < 3 ; j++ )
			{
				int currentCheck = i*3+j;
				if ( haciendaLayout.charAt( currentCheck ) == 'u' )
				{
					newLayout.setCharAt( currentCheck, 'C' );
				}
				else if ( haciendaLayout.charAt( currentCheck ) == 'F' || haciendaLayout.charAt( currentCheck ) == 'f' )
				{
					newLayout.setCharAt( currentCheck, 'R' );
				}
			}

			// Record layout		
			haciendaLayout = newLayout.toString();
			Preferences.setString( "haciendaLayout", haciendaLayout );

			// Make deductions based on any updates 
			for ( int j=0 ; j < 3 ; j++ )
			{
				int currentCheck = i*3+j;
				if ( haciendaLayout.charAt( currentCheck ) == '0' )
				{
					// If we have got a Key or Clue in the Room remaining one is a Reward
					if ( room.contains( "K" ) || room.contains( "C" ) )
					{
						newLayout.setCharAt( currentCheck, 'r' );
					}
					// If there are two Rewards, remaining one is a Clue
					else if ( HaciendaManager.countString( room.toLowerCase(), "r" ) == 2 )
					{
						newLayout.setCharAt( currentCheck, 'C' );
					}
				}
			}
		}
	
		// Record layout		
		Preferences.setString( "haciendaLayout", newLayout.toString() );
	}
	
	private static int returnClue( final String text )
	{
		for ( String[] s : CLUES )
		{
			if ( text.contains( s[ 0 ] ) )
			{
				return Integer.parseInt( s[1] );
			}
		}
		return -1;
	}
	private static String getClue( final String text )
	{
		for ( String[] s : CLUES )
		{
			if ( text.contains( s[ 0 ] ) )
			{
				return s[ 0 ];
			}
		}
		return null;
	}
	
	private static boolean verifyReward( final String text )
	{
		for ( String s : REWARDS )
		{
			if ( text.contains( s ) )
			{
				return true;
			}
		}
		return false;
	}
	
	private static String returnReward( final int location )
	{
		return REWARDS[location];
	}
	
	public static String[] getSpoilers( final int choice )
	{
		String[] result = new String[ 4 ];
		
		switch ( choice )
		{
		case 410:
			// choice of hallways
			result[ 0 ] = HaciendaManager.getWingSpoilers( 0 );
			result[ 1 ] = HaciendaManager.getWingSpoilers( 9 );
			result[ 2 ] = "leave barracks";
			break;
		case 411:
		case 412:
			// choice of rooms
			for ( int i=0 ; i < 3 ; i++ )
			{
				StringBuilder buffer = new StringBuilder();
				buffer.append( HaciendaManager.getSpoiler( choice * 9 + i * 3 - 3699 ) );
				buffer.append( " / " );
				buffer.append( HaciendaManager.getSpoiler( choice * 9 + i * 3 - 3698 ) );
				buffer.append( " / " );
				buffer.append( HaciendaManager.getSpoiler( choice * 9 + i * 3 - 3697 ) );
				result[ i ] = buffer.toString();
			}
			result[ 3 ] = "leave barracks";
			break;
		default:
			// choice of locations in rooms
			for ( int i=0 ; i < 3 ; i++ )
			{
				result[ i ] = HaciendaManager.getSpoiler( choice * 3 + i - 1239 );
			}
			result[ 3 ] = "leave barracks";
			break;
		}
		return result;
	}

	private static String getSpoiler( final int spoiler )
	{
		String haciendaLayout = Preferences.getString( "haciendaLayout" );
		Boolean questComplete = Preferences.getString( "questG04Nemesis" ).equals( "finished" );
		String result = "";
		int roomNumber = spoiler/3;
		
		String room = haciendaLayout.substring( roomNumber*3, roomNumber*3+3 );

		if ( haciendaLayout.charAt( spoiler ) == 'K' )
		{
			result = "empty";
		}
		else if ( haciendaLayout.charAt( spoiler ) == 'u' )
		{
			result = "gain hacienda key or clue";
		}
		else if ( haciendaLayout.charAt( spoiler ) == 'k' )
		{
			result = "gain hacienda key";
		}
		else if ( haciendaLayout.charAt( spoiler ) == 'c' )
		{
			result = "gain clue";
		}
		else if ( haciendaLayout.charAt( spoiler ) == 'F' || haciendaLayout.charAt( spoiler ) == 'f' )
		{
			result = "fight mariachi";
		}
		else if ( haciendaLayout.charAt( spoiler ) == 'R' )
		{
			result = "empty";
		}
		else if ( haciendaLayout.charAt( spoiler ) == 'r' )
		{
			StringBuilder buffer = new StringBuilder();
			buffer.append( "gain " );
			buffer.append( HaciendaManager.returnReward( spoiler ) );
			result = buffer.toString();
		}
		else if ( haciendaLayout.charAt( spoiler ) == 'C' )
		{
			result = "empty";
		}
		else if ( haciendaLayout.charAt( spoiler ) == 'X' )
		{
			result = "empty";
		}
		else if ( haciendaLayout.charAt( spoiler ) == '0' )
		{
			if ( room.contains( "F" ) )
			{
				result = "key, clue or reward";
			}
			else if ( room.toLowerCase().contains( "k" ) || room.toLowerCase().contains( "c" ) || room.toLowerCase().contains( "u" ) )
			{
				result = "fight or reward";
			}
			else if ( room.contains( "R" ) || room.contains( "r" ) )
			{
				result = questComplete ? "reward or clue" : "key, clue or fight";
			}
			else
			{
				result = questComplete ? "reward or clue" : "unknown";
			}
		}
		else
		{
			result = "unknown result";
		}

		if ( questComplete && spoiler == 17 )
		{
			return ( result + " make recordings" );
		}
		return result;
	}
private static String getWingSpoilers( final int spoiler )
	{
		String haciendaLayout = Preferences.getString( "haciendaLayout" );
		Boolean questComplete = Preferences.getString( "questG04Nemesis" ).equals( "finished" );
		String result = "";
		int wingNumber = spoiler/9;
	
		String wing = haciendaLayout.substring( wingNumber*9, wingNumber*9+9 );
	
		int keysFound = HaciendaManager.countString( wing, "K" );
		int keysLocated = HaciendaManager.countString( wing, "k" );
		int cluesLocated = HaciendaManager.countString( wing.toLowerCase(), "c" );
		int rewardsFound = HaciendaManager.countString( wing, "R" );
		int rewardsLocated = HaciendaManager.countString( wing, "r" );
	
		result = questComplete ? ( 6 - rewardsFound ) + " rewards left, " + rewardsLocated + " located." : 
			( 3 - keysFound - cluesLocated ) + " keys or clues left, " + keysLocated + " keys located.";
	
		if ( questComplete && spoiler == 9 )
		{
			return ( result + ", make recordings" );
		}
		return result;
	}

	private static int countString( String inString, String lookFor )
	{
		int result = 0;
		int currentIndex = 0;
		
		currentIndex = inString.indexOf( lookFor );
	
		while ( currentIndex != -1 )
		{
			currentIndex = inString.indexOf( lookFor, currentIndex + 1 );
			result++;
		}
	
		return result;
	}
}