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

package net.sourceforge.kolmafia;

import java.util.List;

import net.sourceforge.kolmafia.CakeArenaManager.ArenaOpponent;

import net.sourceforge.kolmafia.persistence.FamiliarDatabase;

public class FamiliarTool
{
	// Array of current opponents
	private final Opponent[] opponents;

	// Index of best opponent to fight
	private int bestOpponent;

	// Index of best arena match against that opponent
	private int bestMatch;

	// Best weight for own familiar during that match
	private int bestWeight;

	// Difference from "perfect" weight for that match
	private int difference;

	/**
	 * Initializes Familiar Tool with all Arena Data
	 *
	 * @param opponents Array with Ids of all opponents. The index of each opponent will be re-used as a return value
	 */
	public FamiliarTool( final List opponents )
	{
		int opponentCount = opponents.size();
		this.opponents = new Opponent[ opponentCount ];
		for ( int i = 0; i < opponentCount; ++i )
		{
			ArenaOpponent opponent = (ArenaOpponent) opponents.get( i );
			this.opponents[ i ] = new Opponent( opponent );
		}
	}

	/**
	 * Runs all the calculation to determine the best matchup for a familiar
	 *
	 * @param ownFamiliar Id of the familiar to calculate a matchup for
	 * @param possibleOwnWeights Array with all possibilities for familiar weight
	 * @return The Id number of the best opponent. Further information can be collected through other functions
	 */
	public ArenaOpponent bestOpponent( final int ownFamiliar, final int[] possibleOwnWeights )
	{
		int[] ownSkills = FamiliarDatabase.getFamiliarSkills( ownFamiliar );
		return this.bestOpponent( ownSkills, possibleOwnWeights );
	}

	/**
	 * Runs all the calculation to determine the best matchup for a familiar
	 *
	 * @param ownSkills our familiar's skills
	 * @param possibleOwnWeights Array with all possibilities for familiar weight
	 * @return The Id number of the best opponent. Further information can be collected through other functions
	 */
	public ArenaOpponent bestOpponent( final int[] ownSkills, final int[] possibleOwnWeights )
	{
		int opponentCount = this.opponents.length;
		int possibleWeights = possibleOwnWeights.length;

		this.bestMatch = this.bestOpponent = this.bestWeight = -1;
		this.difference = 500; // initialize to worst possible value

		for ( int match = 0; match < 4; match++ )
		{
			int ownSkill = ownSkills[ match ];

			// Skip hopeless contests
			if ( ownSkill == 0 )
			{
				continue;
			}

			for ( int opponent = 0; opponent < opponentCount; ++opponent )
			{
				Opponent opp = this.opponents[ opponent ];
				int opponentWeight = opp.getWeight();

				for ( int weightIndex = 0; weightIndex < possibleWeights; ++weightIndex )
				{
					int ownWeight = possibleOwnWeights[ weightIndex ];
					int ownPower = ownWeight + ownSkill * 3;

					int opponentSkill = opp.getSkill( match );
					int opponentPower;
					if ( opponentSkill == 0 )
					{
						opponentPower = 5;
					}
					else
					{
						opponentPower = opponentWeight + opponentSkill * 3;
					}

					// optimal weight for equal skill is +3
					if ( this.betterWeightDifference( ownPower - ( opponentPower + 3 ), this.difference ) )
					{
						this.difference = ownPower - ( opponentPower + 3 );
						this.bestOpponent = opponent;
						this.bestMatch = match;
						this.bestWeight = ownWeight;
					}
				}
			}
		}

		if ( this.bestOpponent >= 0 )
		{
			return this.opponents[ this.bestOpponent ].getOpponent();
		}

		return null;
	}

	/**
	 * Retrieves match data. Will only supply relevant data for last call to bestOpponent
	 *
	 * @return The Id number of the best match. 1 = 'Ultimate Cage Match', 2 = 'Scavenger Hunt', 3 = 'Obstacle Course',
	 *         4 = 'Hide and Seek'
	 */
	public int bestMatch()
	{
		return this.bestMatch + 1;
	}

	/**
	 * Retrieves weight for matchup. This weight will be a value from the possibleOwnWeights parameter in bestOpponent()
	 *
	 * @return Weight value for chosen matchup
	 */
	public int bestWeight()
	{
		return this.bestWeight;
	}

	/**
	 * Retrieves difference from perfect weight for matchup. Will only supply relevant data for last call to
	 * bestOpponent()
	 *
	 * @return Difference from the perfect weight. 0 = perfect, +X = X pounds too heavy, -X is X pounds too light.
	 */
	public int difference()
	{
		return this.difference;
	}

	private boolean betterWeightDifference( final int newVal, final int oldVal )
	{
		// In order to reduce the probability for accidental loss,
		// do not consider priority values less than -2, but make
		// it lower priority than 3.

		switch ( oldVal )
		{
		case 0:
			return false;

		case 1:
			return newVal == 0;

		case -1:
			return newVal == 0 || newVal == 1;

		case 2:
			return newVal == 0 || newVal == 1 || newVal == -1;

		case 3:
			return newVal == 0 || newVal == 1 || newVal == -1 || newVal == 2;

		case -2:
			return newVal == 0 || newVal == 1 || newVal == -1 || newVal == 2 || newVal == 3;

		default:
			return newVal == 0 || newVal < oldVal && newVal >= -2;
		}
	}

	private class Opponent
	{
		// Cake Arena data structure
		private final ArenaOpponent opponent;

		// Familiar type
		private final int type;

		// Weight
		private final int weight;

		// Arena parameters
		private int[] arena = new int[ 4 ];

		public Opponent( final ArenaOpponent opponent )
		{
			this.opponent = opponent;
			this.type = FamiliarDatabase.getFamiliarId( opponent.getRace() );
			this.weight = opponent.getWeight();
			this.arena = FamiliarDatabase.getFamiliarSkills( this.type );
		}

		public ArenaOpponent getOpponent()
		{
			return this.opponent;
		}

		public int getWeight()
		{
			return this.weight;
		}

		public int getSkill( final int match )
		{
			return this.arena[ match ];
		}
	}
}
