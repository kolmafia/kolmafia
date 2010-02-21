/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import java.util.ArrayList;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.request.FightRequest;

import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class DiscoCombatHelper
{
	// Only Disco Bandits can do combos.
	public static boolean canCombo;

	public static final int DISCO_EYE_POKE = 0;
	public static final int DISCO_DANCE_OF_DOOM = 1;
	public static final int DISCO_DANCE_II = 2;
	public static final int DISCO_FACE_STAB = 3;
	public static final int BREAK_IT_ON_DOWN = 4;
	public static final int POP_AND_LOCK_IT = 5;
	public static final int RUN_LIKE_THE_WIND = 6;

	public static final int NUM_SKILLS = 7;

	public static final String [] SKILLS = new String[]
	{
		"Disco Eye-Poke",
		"Disco Dance of Doom",
		"Disco Dance II: Electric Boogaloo",
		"Disco Face Stab",
		"Break It On Down",
		"Pop and Lock It",
		"Run Like the Wind",
	};

	public static final int [] SKILL_ID = new int[]
	{
		5003,
		5005,
		5008,
		5012,
		50,
		51,
		52,
	};

	public static final String [] BUTTON_NAME = new String[]
	{
		"Eye-Poke",
		"Dance",
		"Dance II",
		"Face Stab",
		"Break",
		"Pop",
		"Run",
	};

	public static final boolean [] knownSkill = new boolean[ NUM_SKILLS ];

	public static final int DISCO_CONCENTRATION = 0;
	public static final int DISCO_NIRVANA = 1;
	public static final int DISCO_INFERNO = 2;
	public static final int DISCO_BLEEDING = 3;
	public static final int DISCO_BLINDNESS = 4;

	public static final int RAVE_CONCENTRATION = 5;
	public static final int RAVE_NIRVANA = 6;
	public static final int RAVE_KNOCKOUT = 7;
	public static final int RAVE_BLEEDING = 8;
	public static final int RAVE_STEAL = 9;
	public static final int RAVE_SUBSTATS = 10;

	public static final int FIRST_RAVE_COMBO = 5;
	public static final int NUM_COMBOS = 11;

	public static final String [][] COMBOS =
	{
		{
			"Disco Concentration",
			"Item Drop +20",
		},
		{
			"Disco Nirvana",
			"Meat Drop +30",
		},
		{
			"Disco Inferno",
			"Moxie +5, Hot Damage +3",
		},
		{
			"Disco Bleeding",
			"Recurring damage",
		},
		{
			"Disco Blindness",
			"Monster stunned",
		},
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
			"Auto-steal Outside the Club",
		},
		{
			"Rave Substats",
			"2-4 substats",
		},
	};

	public static final boolean [] knownCombo = new boolean[ NUM_COMBOS ];

	private static int[][][] COMBO_SKILLS =
	{
		// Disco Concentration
		{
			{ DISCO_EYE_POKE },
			{ DISCO_DANCE_OF_DOOM },
			{ DISCO_DANCE_II },
		},
		// Disco Nirvana
		{
			{ DISCO_DANCE_OF_DOOM },
			{ DISCO_DANCE_II },
		},
		// Disco Inferno
		{
			{ DISCO_EYE_POKE },
			{ DISCO_DANCE_II },
		},
		// Disco Bleeding
		{
			{ DISCO_DANCE_OF_DOOM, DISCO_DANCE_II },
			{ DISCO_FACE_STAB },
		},
		// Disco Blindness
		{
			{ DISCO_DANCE_OF_DOOM, DISCO_DANCE_II },
			{ DISCO_EYE_POKE },
		},
		// Rave Concentration
		{
			{ BREAK_IT_ON_DOWN },
			{ POP_AND_LOCK_IT },
			{ RUN_LIKE_THE_WIND },
		},
		// Rave Nirvana
		{
			{ BREAK_IT_ON_DOWN },
			{ POP_AND_LOCK_IT },
			{ RUN_LIKE_THE_WIND },
		},
		// Rave Knockout
		{
			{ BREAK_IT_ON_DOWN },
			{ POP_AND_LOCK_IT },
			{ RUN_LIKE_THE_WIND },
		},
		// Rave Bleeding
		{
			{ BREAK_IT_ON_DOWN },
			{ POP_AND_LOCK_IT },
			{ RUN_LIKE_THE_WIND },
		},
		// Rave Steal
		{
			{ BREAK_IT_ON_DOWN },
			{ POP_AND_LOCK_IT },
			{ RUN_LIKE_THE_WIND },
		},
		// Rave Substats
		{
			{ BREAK_IT_ON_DOWN },
			{ POP_AND_LOCK_IT },
			{ RUN_LIKE_THE_WIND },
		},
	};

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
	}

	private static final void checkCombo( final int combo )
	{
		// If it's a rave skill, we need to have learned it in battle.
		if ( combo >= FIRST_RAVE_COMBO )
		{
			String setting = "raveCombo" + String.valueOf( combo - FIRST_RAVE_COMBO + 1 );
			String seq = Preferences.getString( setting );
			knownCombo[ combo ] = !seq.equals( "" );
			return;
		}

		// Check that we know all the skills
		int [][] data = COMBO_SKILLS[ combo ];
		for ( int i = 0; i < data.length; ++i )
		{
			// Some combo allow multiple skills. Any will do.
			int [] skills = data[i];
			boolean known = false;
			for ( int j = 0; j < skills.length; ++j )
			{
				int skill = skills[ j ];
				if ( knownSkill[ skill ] )
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
	
	private static final StringBuffer generateTable()
	{
		StringBuffer buffer = new StringBuffer();
		int combos = 0;

		buffer.append( "<table border=2 cols=5>" );
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
			buffer.append( "<td>" );
			buffer.append( combo[ 0 ] );
			buffer.append( "</td>" );

			// Combo effect
			buffer.append( "<td>" );
			buffer.append( combo[ 1 ] );
			buffer.append( "</td>" );

			int [][] data = COMBO_SKILLS[ i ];
			for ( int j = 0; j < 3; ++j )
			{
				buffer.append( "<td>" );
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
				buffer.append( "</td>" );
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

	private static final void addDiscoButton( final StringBuffer buffer, final int skill, boolean isEnabled )
	{
		String skillName = SKILLS[ skill ];
		int skillId = SKILL_ID[ skill ];
		String name = BUTTON_NAME[ skill ];

		buffer.append( "<input type=\"button\" onClick=\"document.location.href='" );
		buffer.append( "fight.php?action=skill&whichskill=" );
		buffer.append( String.valueOf( skillId ) );

		buffer.append( "';void(0);\" value=\"" );
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

		buffer.append( ">&nbsp;" );
	}

	public static final void decorate( final StringBuffer buffer )
	{
		// If you're not a Disco Bandit, nothing to do.
		if ( !DiscoCombatHelper.canCombo )
		{
			return;
		}

		// If you've already won the fight, punt
		if ( buffer.indexOf( "WINWINWIN" ) != -1 )
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
