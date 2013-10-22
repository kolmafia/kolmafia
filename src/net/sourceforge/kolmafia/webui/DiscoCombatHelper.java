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

package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.combat.MonsterStatusTracker;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DiscoCombatHelper
{
	// Only Disco Bandits can do combos.
	public static boolean canCombo;

	private static final int UNKNOWN = -1;
	public static final int BREAK_IT_ON_DOWN = 0;
	public static final int POP_AND_LOCK_IT = 1;
	public static final int RUN_LIKE_THE_WIND = 2;

	public static final int FIRST_RAVE_SKILL = 0;
	public static final int LAST_RAVE_SKILL = 2;
	public static final int NUM_SKILLS = 3;

	public static final String [] SKILLS = new String[]
	{
		"Break It On Down",
		"Pop and Lock It",
		"Run Like the Wind",
	};

	public static final int [] SKILL_ID = new int[]
	{
		50,
		51,
		52,
	};

	public static final String [] BUTTON_NAME = new String[]
	{
		"Break",
		"Pop",
		"Run",
	};

	public static final boolean [] knownSkill = new boolean[ NUM_SKILLS ];

	public static final int RAVE_CONCENTRATION = 0;
	public static final int RAVE_NIRVANA = 1;
	public static final int RAVE_KNOCKOUT = 2;
	public static final int RAVE_BLEEDING = 3;
	public static final int RAVE_STEAL = 4;
	public static final int RAVE_SUBSTATS = 5;
	public static final int RANDOM_RAVE = 6;

	public static final int FIRST_RAVE_COMBO = 0;
	public static final int NUM_COMBOS = 7;

	public static final String [][] COMBOS =
	{
		{
			"Rave Concentration",
			"Item Drop +30",
		},
		{
			"Rave Nirvana",
			"Meat Drop +50",
		},
		{
			"Rave Knockout",
			"Multi-round stun+damage",
		},
		{
			"Rave Bleeding",
			"Recurring damage",
		},
		{
			"Rave Steal",
			"Steal item",
		},
		{
			"Rave Substats",
			"2-4 substats",
		},
		{
			"Random Rave",
			"Learn a new combo!",
		},
	};

	public static final boolean [] knownCombo = new boolean[ NUM_COMBOS ];

	private static int[][][] COMBO_SKILLS =
	{
		// Rave Concentration
		{
			{ UNKNOWN },
			{ UNKNOWN },
			{ UNKNOWN },
		},
		// Rave Nirvana
		{
			{ UNKNOWN },
			{ UNKNOWN },
			{ UNKNOWN },
		},
		// Rave Knockout
		{
			{ UNKNOWN },
			{ UNKNOWN },
			{ UNKNOWN },
		},
		// Rave Bleeding
		{
			{ UNKNOWN },
			{ UNKNOWN },
			{ UNKNOWN },
		},
		// Rave Steal
		{
			{ UNKNOWN },
			{ UNKNOWN },
			{ UNKNOWN },
		},
		// Rave Substats
		{
			{ UNKNOWN },
			{ UNKNOWN },
			{ UNKNOWN },
		},
		// Random Rave
		{
			{ UNKNOWN },
			{ UNKNOWN },
			{ UNKNOWN },
		},
	};

	// Count of disco skills used in sequence
	private static int counter = 0;
	private static final int [] sequence = new int[3];

	public static final void initialize()
	{
		DiscoCombatHelper.canCombo = KoLCharacter.getClassType().equals( KoLCharacter.DISCO_BANDIT );

		if ( !DiscoCombatHelper.canCombo )
		{
			return;
		}

		for ( int i = 0; i < NUM_SKILLS; ++i )
		{
			String name = SKILLS[ i ];
			knownSkill[ i ] = KoLCharacter.hasSkill( name );
		}

		for ( int i = 0; i < NUM_COMBOS; ++i )
		{
			DiscoCombatHelper.checkCombo( i );
		}

		DiscoCombatHelper.counter = 0;
		DiscoCombatHelper.sequence[ 0 ] = 0;
		DiscoCombatHelper.sequence[ 1 ] = 0;
		DiscoCombatHelper.sequence[ 2 ] = 0;
	}

	private static int skillIdToSkill( final String skill )
	{
		return DiscoCombatHelper.skillIdToSkill( StringUtilities.parseInt( skill ) );
	}

	private static int skillIdToSkill( final int skill )
	{
		for ( int i = 0; i < NUM_SKILLS; ++i )
		{
			if ( skill == SKILL_ID[i] )
			{
				return i;
			}
		}
		return -1;
	}

	private static int skillNameToSkill( final String name )
	{
		for ( int i = 0; i < NUM_SKILLS; ++i )
		{
			if ( SKILLS[i].equals( name ) )
			{
				return i;
			}
		}
		return -1;
	}

	public static boolean canRaveSteal()
	{
		if ( Preferences.getInteger( "_raveStealCount" ) < 30 )
		{
			return true;
		}

		// Rave Steal in the volcano island always works
		String encounter = MonsterStatusTracker.getLastMonsterName();
		if ( encounter.equalsIgnoreCase( "Breakdancing Raver" ) ||
			 encounter.equalsIgnoreCase( "Pop-and-Lock Raver" ) ||
			 encounter.equalsIgnoreCase( "Running Man" ) )
		{
			return true;
		}

		return false;
	}

	public static String disambiguateCombo( String name )
	{
		name = name.trim().toLowerCase();
		for ( int i = 0; i < NUM_COMBOS; ++i )
		{
			if ( COMBOS[ i ][ 0 ].toLowerCase().indexOf( name ) != -1 )
			{
				return COMBOS[ i ][ 0 ];
			}
		}
		return null;
	}

	public static int[] getCombo( String name )
	{
		name = name.trim().toLowerCase();
		for ( int i = 0; i < NUM_COMBOS; ++i )
		{
			if ( COMBOS[ i ][ 0 ].toLowerCase().indexOf( name ) != -1 )
			{
				return getCombo( i );
			}
		}
		return null;
	}

	private static int[] getCombo( final int combo )
	{
		if ( !DiscoCombatHelper.canCombo || !knownCombo[ combo ] )
		{
			return null;
		}

		int[][] data = COMBO_SKILLS[ combo ];
		int[] rv = new int[ data.length ];
		for ( int i = 0; i < data.length; ++i )
		{
			// Some combo allow multiple skills. Pick the first known one.
			int [] skills = data[i];
			for ( int j = 0; j < skills.length; ++j )
			{
				int skill = skills[ j ];
				if ( knownSkill[ skill ] )
				{
					rv[ i ] = SKILL_ID[ skill ];
					break;
				}
			}
		}
		return rv;
	}

	private static final void checkCombo( final int combo )
	{
		int [][] data = COMBO_SKILLS[ combo ];

		// If it's a rave skill, we need to have learned it in battle.
		if ( combo == RANDOM_RAVE )
		{
			int found = 0;

		findUnknownCombo:
			for ( int sel = 0; sel < 27; ++sel )
			{
				int s1 = sel % 3 + FIRST_RAVE_SKILL;
				int s2 = (sel / 3) % 3 + FIRST_RAVE_SKILL;
				int s3 = (sel / 9) % 3 + FIRST_RAVE_SKILL;
				if ( s1 == s2 || s1 == s3 || s2 == s3 )
				{
					continue;
				}

				for ( int test = FIRST_RAVE_COMBO; test < RANDOM_RAVE; ++test )
				{
					int[][] testdata = COMBO_SKILLS[ test ];
					if ( s1 == testdata[ 0 ][ 0 ] &&
					     s2 == testdata[ 1 ][ 0 ] &&
					     s3 == testdata[ 2 ][ 0 ] )
					{
						continue findUnknownCombo;
					}
				}

				// We don't know this combo yet.
				if  ( found == 1 )
				{
					// If we've already found an unknown
					// combo, note that we found more than
					// one and stop looking.
					found = 2;
					break;
				}

				// Save first unknown combo
				data[ 0 ][ 0 ] = s1;
				data[ 1 ][ 0 ] = s2;
				data[ 2 ][ 0 ] = s3;
				found = 1;
			}

			// Check how many unknown combos there are
			switch ( found )
			{
			case 1:
				// If there is only one unknown combo, we can
				// deduce what it is
				for ( int test = FIRST_RAVE_COMBO; test < RANDOM_RAVE; ++test )
				{
					if ( !knownCombo[ test ] )
					{

						KoLmafia.updateDisplay( "All rave combos have been identified!" );
						DiscoCombatHelper.learnRaveCombo( test, data[ 0 ][ 0 ], data[ 1 ][ 0 ], data[ 2 ][ 0 ] );
						break;
					}
				}
				// Fall through
			case 0:
				// We no longer need random rave
				knownCombo[ combo ] = false;
				return;
			}

			// There are at least two unknown combos and the skills
			// for the first one are set up in data
		}
		else if ( combo >= FIRST_RAVE_COMBO )
		{
			String setting = "raveCombo" + String.valueOf( combo - FIRST_RAVE_COMBO + 1 );
			String seq = Preferences.getString( setting );
			String[] skills = seq.split( "," );
			if ( skills.length == 3 )
			{
				for ( int i = 0; i < skills.length; ++i )
				{
					int skill = DiscoCombatHelper.skillNameToSkill( skills[ i ] );
					if ( skill < FIRST_RAVE_SKILL || skill > LAST_RAVE_SKILL )
					{
						knownCombo[ combo ] = false;
						return;
					}
					data[i][0] = skill;
				}
				knownCombo[ combo ] = true;
				return;
			}
			knownCombo[ combo ] = false;
			return;
		}

		// Check that we know all the skills
		for ( int i = 0; i < data.length; ++i )
		{
			// Some combo allow multiple skills. Any will do.
			int [] skills = data[i];
			boolean known = false;
			for ( int j = 0; j < skills.length; ++j )
			{
				int skill = skills[ j ];
				if ( skill != UNKNOWN && knownSkill[ skill ] )
				{
					known = true;
					break;
				}
			}

			// If we don't know a skill, give up.
			if ( !known )
			{
				knownCombo[ combo ] = false;
				return;
			}
		}

		// We know the necessary skills
		knownCombo[ combo ] = true;
	}

	public static final void learnSkill( final String name )
	{
		if ( !DiscoCombatHelper.canCombo )
		{
			return;
		}

		boolean discoSkill = false;
		for ( int i = 0; i < NUM_SKILLS; ++i )
		{
			if ( SKILLS[i].equals( name ) )
			{
				discoSkill = true;
				knownSkill[ i ] = true;
				break;
			};
		}

		// If it's not a Disco Bandit combat skill, no combo
		if ( !discoSkill )
		{
			return;
		}

		for ( int i = 0; i < NUM_COMBOS; ++i )
		{
			DiscoCombatHelper.checkCombo( i );
		}
	}

	public static final void parseFightRound( final String action, final Matcher macroMatcher )
	{
		if ( !DiscoCombatHelper.canCombo )
		{
			return;
		}

		String responseText;
		try
		{
			responseText = macroMatcher.group();
		}
		catch ( IllegalStateException e )
		{	// page structure is botched - should have already been reported
			return;
		}

		// Two of the Rave Combos we can learn show up in the next
		// round of battle, regardless of what we do this round.

		if ( DiscoCombatHelper.counter ==  3 )
		{
			// Your opponent seems to be temporarily unconscious
			if ( responseText.indexOf( "seems to be temporarily unconscious" ) != -1 )
			{
				DiscoCombatHelper.learnRaveCombo( RAVE_KNOCKOUT );
			}
			// He bleeds from various wounds you've inflicted
			if ( responseText.indexOf( "bleeds from various wounds you've inflicted" ) != -1 )
			{
				DiscoCombatHelper.learnRaveCombo( RAVE_BLEEDING );
			}
		}

		if ( action == null || !action.startsWith( "skill" ) )
		{
			DiscoCombatHelper.counter = 0;
			return;
		}

		int skill = DiscoCombatHelper.skillIdToSkill( action.substring( 5 ) );
		if ( skill < 0 )
		{
			DiscoCombatHelper.counter = 0;
			return;
		}

		// Track last three disco skills used in sequence.

		int index = DiscoCombatHelper.counter;
		if ( index == 3 )
		{
			// Shift skills back
			DiscoCombatHelper.sequence[ 0 ] = DiscoCombatHelper.sequence[ 1 ];
			DiscoCombatHelper.sequence[ 1 ] = DiscoCombatHelper.sequence[ 2 ];
			index = 2;
		}

		DiscoCombatHelper.sequence[index++] = skill;
		DiscoCombatHelper.counter = index;

		// If we have completed a known disco or rave combo, reset
		// A combo must have at least two skills.
		int combo = -1;
		for ( int i = 0; DiscoCombatHelper.counter > 1 && i < NUM_COMBOS; ++i )
		{
			if ( !knownCombo[ i ] || i == RANDOM_RAVE )
			{
				continue;
			}

			int [][] data = COMBO_SKILLS[ i ];

			// If we have the correct number of skills to match
			// this sequence, check it.

			if ( DiscoCombatHelper.counter == data.length &&
			     DiscoCombatHelper.checkSequence( data, 0 ) )
			{
				combo = i;
				DiscoCombatHelper.counter = 0;
				break;
			}

			// If we have three skills in a row, we can match
			// either a three-skill combo or a two-skill combo

			if ( DiscoCombatHelper.counter == 3 &&
			     data.length == 2 &&
			     DiscoCombatHelper.checkSequence( data, 1 ) )
			{
				combo = i;
				DiscoCombatHelper.counter = 0;
				break;
			}
		}

		if ( combo >= 0 )
		{
			StringBuilder buffer = new StringBuilder();
			buffer.append( combo < FIRST_RAVE_COMBO ? "Disco" : "Rave" );
			buffer.append( " combo: " );
			buffer.append( COMBOS[ combo ][0] );
			String message = buffer.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		// Track successfull Rave Steal usage
		if ( combo == RAVE_STEAL )
		{
			// Rave Steal in the volcano island shouldn't count
			String encounter = MonsterStatusTracker.getLastMonsterName();
			if ( encounter.equalsIgnoreCase( "Breakdancing Raver" ) ||
			     encounter.equalsIgnoreCase( "Pop-and-Lock Raver" ) ||
			     encounter.equalsIgnoreCase( "Running Man" ) )
			{
			}
			// You're getting tired of this same old song and dance. 
			else if ( responseText.indexOf( "same old song and dance" ) != -1 )
			{
				Preferences.setInteger( "_raveStealCount", 30 );
			}
			else if ( responseText.indexOf( "You acquire an item" ) != -1 )
			{
				Preferences.increment( "_raveStealCount" );
			}
		}

		// If three different rave skills are used in sequence,
		// identify the rave combo

		if ( DiscoCombatHelper.counter == 3 )
		{
			// Your savage beatdown seems to have knocked loose
			// some treasure. Sweet!
			// Your savage beatdown fails to knock loose any treasure. Lame!
			if ( responseText.indexOf( "Your savage beatdown" ) != -1 )
			{
				DiscoCombatHelper.learnRaveCombo( RAVE_STEAL );
			}
			// As your opponent groans in pain, you feel pretty
			// good about the extra dance practice you're
			// getting. You're starting to get tired of beating up
			// on this same dude. Why isn't he dead yet?
			else if ( responseText.indexOf( "extra dance practice" ) != -1 )
			{
				DiscoCombatHelper.learnRaveCombo( RAVE_SUBSTATS );
			}

			// Your dance routine leaves you feeling extra-focused
			// and in the zone. Ooh yeeaah.

			else if ( responseText.indexOf( "extra-focused and in the zone" ) != -1 )
			{
				DiscoCombatHelper.learnRaveCombo( RAVE_CONCENTRATION );
			}

			// Your dance routine leaves you feeling particularly
			// groovy and at one with the universe. It's a little
			// unsettling, but you soon get used to it.

			else if ( responseText.indexOf( "feeling particularly groovy" ) != -1 )
			{
				DiscoCombatHelper.learnRaveCombo( RAVE_NIRVANA );
			}
		}
	}

	private static final void learnRaveCombo( int combo )
	{
		// Sanity check: we used three skills in a row
		if ( DiscoCombatHelper.counter !=  3 )
		{
			return;
		}

		int skill1 = DiscoCombatHelper.sequence[0];
		int skill2 = DiscoCombatHelper.sequence[1];
		int skill3 = DiscoCombatHelper.sequence[2];

		// Sanity check: last three skills must all be different
		if ( skill1 == skill2 || skill1 == skill3 || skill2 == skill3 )
		{
			return;
		}

		// Sanity check: last three skills must all be rave skills
		if ( skill1 < FIRST_RAVE_SKILL || skill1 > LAST_RAVE_SKILL ||
		     skill2 < FIRST_RAVE_SKILL || skill2 > LAST_RAVE_SKILL ||
		     skill3 < FIRST_RAVE_SKILL || skill3 > LAST_RAVE_SKILL )
		{
			return;
		}

		// Clear sequence counter
		DiscoCombatHelper.counter = 0;

		// If we already know this combo, nothing to do
		if ( knownCombo[ combo ] )
		{
			return;
		}

		// We have learned the combo!
		DiscoCombatHelper.learnRaveCombo( combo, skill1, skill2, skill3 );

		// Update the random combo
		DiscoCombatHelper.checkCombo( RANDOM_RAVE );
	}

	private static final void learnRaveCombo( int combo, int skill1, int skill2, int skill3 )
	{
		knownCombo[ combo ] = true;

		// Generate the setting.
		String setting = "raveCombo" + String.valueOf( combo - FIRST_RAVE_COMBO + 1 );
		String value = SKILLS[ skill1 ] + "," + SKILLS[ skill2 ] + "," + SKILLS[ skill3 ];
		Preferences.setString( setting, value );

		// Save the skills in the table
		int [][] data = COMBO_SKILLS[ combo ];
		data[0][0] = skill1;
		data[1][0] = skill2;
		data[2][0] = skill3;

		StringBuilder buffer = new StringBuilder();
		buffer.append( "You learned a new Rave Combo!" );
		buffer.append( KoLConstants.LINE_BREAK );
		buffer.append( SKILLS[ skill1 ] );
		buffer.append( " + " );
		buffer.append( SKILLS[ skill2 ] );
		buffer.append( " + " );
		buffer.append( SKILLS[ skill3 ] );
		buffer.append( " -> " );
		buffer.append( COMBOS[ combo ][0] );
		String message = buffer.toString();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}

	private static final boolean checkSequence( final int[][] data, final int offset )
	{
		// Compare the skill sequence (starting at offset) to a given
		// combo.

		for ( int i = 0; i < data.length; ++i )
		{
			int skill = DiscoCombatHelper.sequence[ i + offset ];
			int [] skills = data[ i ];
			boolean found = false;
			for ( int j = 0; j < skills.length; ++j )
			{
				if ( skill == skills[ j ] )
				{
					found = true;
					break;
				}
			}
			if ( !found )
			{
				return false;
			}
		}
		return true;
	}

	private static final StringBuffer generateTable()
	{
		StringBuffer buffer = new StringBuffer();
		int combos = 0;

		buffer.append( "<table border=2 cols=5>" );
		if ( DiscoCombatHelper.counter > 0 )
		{
			buffer.append( "<caption>" );
			for ( int i = 0; i < DiscoCombatHelper.counter; ++i )
			{
				if ( i > 0 )
				{
					buffer.append( ", " );
				}
				int skill = DiscoCombatHelper.sequence[i];
				buffer.append( SKILLS[ skill ] );
			}
			buffer.append( "</caption>" );
		}

		for ( int i = 0; i < NUM_COMBOS; ++i )
		{
			if ( !knownCombo[ i ] )
			{
				continue;
			}

			// Count this combo
			combos++;

			String [] combo = COMBOS[ i ];
			buffer.append( "<tr>" );

			// Combo name
			DiscoCombatHelper.addComboButton( buffer, combo[ 0 ], getCombo( i ) );

			// Combo effect
			buffer.append( "<td>" );
			buffer.append( combo[ 1 ] );
			buffer.append( "</td>" );

			int [][] data = COMBO_SKILLS[ i ];
			for ( int j = 0; j < 3; ++j )
			{
				if ( j < data.length )
				{
					int [] skills = data[ j ];
					boolean first = true;
					for ( int k = 0; k < skills.length; ++k )
					{
						int skill = skills[ k ];
						String name = SKILLS[ skill ];

						if ( !KoLCharacter.hasSkill( name ) )
						{
							continue;
						}

						if ( first )
						{
							first = false;
						}
						else
						{
							buffer.append( "<br>" );
						}

						// Add the button
						DiscoCombatHelper.addDiscoButton( buffer, skill, true );
					}
				}
				else
				{
					buffer.append( "<td>&nbsp;</td>" );
				}
			}

			buffer.append( "</tr>" );
		}
		buffer.append( "</table>" );

		// If no combos are known, no table.
		if ( combos == 0 )
		{
			buffer.setLength( 0 );
		}

		return buffer;
	}

	private static final void addComboButton( final StringBuffer buffer, final String name, int[] combo )
	{
		buffer.append( "<form method=POST action=\"fight.php\"><td>" );
		buffer.append( "<input type=hidden name=\"action\" value=\"macro\">" );
		buffer.append( "<input type=hidden name=\"macrotext\" value=\"" );

		int cost = 0;
		for ( int i = 0; i < combo.length; ++i )
		{
			int skillId = combo[ i ];
			cost += SkillDatabase.getMPConsumptionById( skillId );
			buffer.append( "skill " );
			buffer.append( skillId );
			buffer.append( ";" );
		}

		buffer.append( "\"><input onclick=\"return killforms(this);\" type=\"submit\" value=\"" );
		buffer.append( name );
		buffer.append( "\"" );
		if ( DiscoCombatHelper.counter > 0 && DiscoCombatHelper.counter < 3 || cost > KoLCharacter.getCurrentMP() )
		{
			buffer.append( " disabled" );
		}

		buffer.append( ">&nbsp;</td></form>" );
	}

	private static final void addDiscoButton( final StringBuffer buffer, final int skill, boolean isEnabled )
	{
		String skillName = SKILLS[ skill ];
		int skillId = SKILL_ID[ skill ];
		String name = BUTTON_NAME[ skill ];

		buffer.append( "<form method=POST action=\"fight.php\"><td>" );
		buffer.append( "<input type=hidden name=\"action\" value=\"skill\">" );
		buffer.append( "<input type=hidden name=\"whichskill\" value=\"" );
		buffer.append( String.valueOf( skillId ) );
		buffer.append( "\"><input onclick=\"return killforms(this);\" type=\"submit\" value=\"" );
		buffer.append( name );
		buffer.append( "\"" );

		// Shouldn't be here if don't have skill, but just in case...
		if ( isEnabled )
		{
			isEnabled &= KoLCharacter.hasSkill( skillName );
		}

		// Make sure we have the MP to use the skill
		if ( isEnabled )
		{
			isEnabled &= SkillDatabase.getMPConsumptionById( skillId ) <= KoLCharacter.getCurrentMP();
		}

		if ( !isEnabled )
		{
			buffer.append( " disabled" );
		}

		buffer.append( ">&nbsp;</td></form>" );
	}

	public static final void decorate( final StringBuffer buffer )
	{
		// If you're not a Disco Bandit, nothing to do.
		if ( !DiscoCombatHelper.canCombo )
		{
			return;
		}

		// If the fight is over, punt
		if ( FightRequest.getCurrentRound() == 0 )
		{
			return;
		}

		// If you are in Birdform, uh-uh
		if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
		{
			return;
		}

		// If you don't want the Disco Helper, you don't have to have it
		if ( !Preferences.getBoolean( "relayAddsDiscoHelper" ) )
		{
			return;
		}

		int index = buffer.lastIndexOf( "</table></center></td>" );
		if ( index != -1 )
		{
			StringBuffer table = DiscoCombatHelper.generateTable();
			table.insert( 0, "<tr>" );
			table.append( "</tr>" );
			buffer.insert( index, table );
		}
	}
}
