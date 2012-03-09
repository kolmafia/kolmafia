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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BeerPongRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ConditionsCommand
	extends AbstractCommand
{
	private static final Pattern MEAT_PATTERN = Pattern.compile( "[\\d,]+ meat" );

	public ConditionsCommand()
	{
		this.usage = " clear | check | add <condition> | remove <condition> | set <condition> - modify your adventuring goals.";
	}

	public void run( final String cmd, final String parameters )
	{
		if ( parameters.equals( "" ) )
		{
			RequestLogger.printList( GoalManager.getGoals() );
			return;
		}
	
		String option = parameters.split( " " )[ 0 ];

		if ( option.equals( "list" ) )
		{
			RequestLogger.printList( GoalManager.getGoals() );
			return;
		}

		if ( option.equals( "clear" ) )
		{
			ConditionsCommand.clear();
			return;
		}

		if ( option.equals( "check" ) )
		{
			ConditionsCommand.check();
			return;
		}

		if ( option.equals( "add" ) || option.equals( "remove" ) || option.equals( "set" ) )
		{
			String conditionListString = parameters.substring( option.length() ).toLowerCase().trim();
			ConditionsCommand.update( option, conditionListString );
			return;
		}
	}

	public static void clear()
	{
		GoalManager.clearGoals();
		RequestLogger.printLine( "Conditions list cleared." );
	}

	public static void check()
	{
		KoLmafia.checkRequirements( GoalManager.getGoals() );
		RequestLogger.printLine( "Conditions list validated against available items." );
	}

	public static boolean update( final String option, final String conditionListString )
	{
		String[] conditionList = conditionListString.split( "\\s*,\\s*" );

		boolean hasUpdate = false;

		for ( int i = 0; i < conditionList.length; ++i )
		{
			String conditionString = conditionList[ i ];

			if ( conditionString.equalsIgnoreCase( "castle map items" ) )
			{
				GoalManager.setGoal( ItemPool.get( ItemPool.FURRY_FUR, 1 ) );
				GoalManager.setGoal( ItemPool.get( ItemPool.GIANT_NEEDLE, 1 ) );
				GoalManager.setGoal( ItemPool.get( ItemPool.AWFUL_POETRY_JOURNAL, 1 ) );
				hasUpdate = true;
				continue;
			}

			AdventureResult condition = ConditionsCommand.extractCondition( conditionString );

			if ( condition != null )
			{
				if ( option.equals( "set" ) )
				{
					GoalManager.setGoal( condition );
				}
				else if ( option.equals( "remove" ) )
				{
					GoalManager.addGoal( condition.getNegation() );
				}
				else if ( condition.getCount() > 0 )
				{
					GoalManager.addGoal( condition );
				}

				hasUpdate = true;
			}
		}

		return hasUpdate;
	}

	private static AdventureResult extractCondition( String conditionString )
	{
		if ( conditionString.length() == 0 )
		{
			return null;
		}

		conditionString = conditionString.toLowerCase();

		Matcher meatMatcher = ConditionsCommand.MEAT_PATTERN.matcher( conditionString );
		boolean isMeatCondition = meatMatcher.find() ? meatMatcher.group().length() == conditionString.length() : false;

		if ( isMeatCondition )
		{
			String[] splitCondition = conditionString.split( "\\s+" );
			int amount = StringUtilities.parseInt( splitCondition[ 0 ] );
			return new AdventureResult( AdventureResult.MEAT, amount );
		}

		if ( conditionString.endsWith( "choiceadv" ) || conditionString.endsWith( "choices" ) || conditionString.endsWith( "choice" ) )
		{
			// If it's a choice adventure condition, parse out the
			// number of choice adventures the user wishes to do.

			String[] splitCondition = conditionString.split( "\\s+" );
			int count = splitCondition.length > 1 ? StringUtilities.parseInt( splitCondition[ 0 ] ) : 1;
			return GoalManager.GOAL_CHOICE.getInstance( count );
		}

		if ( conditionString.endsWith( "autostop" ) )
		{
			String[] splitCondition = conditionString.split( "\\s+" );
			int count = splitCondition.length > 1 ? StringUtilities.parseInt( splitCondition[ 0 ] ) : 1;
			return GoalManager.GOAL_AUTOSTOP.getInstance( count );
		}

		if ( conditionString.endsWith( "pirate insult" ) ||
			conditionString.endsWith( "pirate insults" ) )
		{
			String[] splitCondition = conditionString.split( "\\s+" );
			int count = splitCondition.length > 1 ? StringUtilities.parseInt( splitCondition[ 0 ] ) : 1;
			return new AdventureResult( AdventureResult.PSEUDO_ITEM_PRIORITY,
				"pirate insult", count ) {
			
				public int getCount( List list )
				{
					if ( list != KoLConstants.inventory )
					{
						return 0;
					}
					return BeerPongRequest.countPirateInsults();
				}
			};
		}

		if ( conditionString.endsWith( "arena flyer ml" ) )
		{
			String[] splitCondition = conditionString.split( "\\s+" );
			int count = splitCondition.length > 1 ? StringUtilities.parseInt( splitCondition[ 0 ] ) : 1;
			return new AdventureResult( AdventureResult.PSEUDO_ITEM_PRIORITY,
				"Arena flyer ML", count ) {
			
				public int getCount( List list )
				{
					if ( list != KoLConstants.inventory )
					{
						return 0;
					}
					return Preferences.getInteger( "flyeredML" );
				}
			};
		}

		if ( conditionString.startsWith( "level" ) )
		{
			// If the condition is a level, then determine how many
			// substat points are required to the next level and
			// add the substat points as a condition.

			String[] splitCondition = conditionString.split( "\\s+" );
			int level = StringUtilities.parseInt( splitCondition[ 1 ] );

			int primeIndex = KoLCharacter.getPrimeIndex();

			GoalManager.GOAL_SUBSTATS_COUNTS[ primeIndex ] =
				(int) ( KoLCharacter.calculateSubpoints( ( level - 1 ) * ( level - 1 ) + 4, 0 ) - KoLCharacter.getTotalPrime() );

			return GoalManager.GOAL_SUBSTATS;
		}

		if ( conditionString.endsWith( "mus" ) || conditionString.endsWith( "muscle" ) || conditionString.endsWith( "moxie" ) || conditionString.endsWith( "mys" ) || conditionString.endsWith( "myst" ) || conditionString.endsWith( "mox" ) || conditionString.endsWith( "mysticality" ) )
		{
			String[] splitCondition = conditionString.split( "\\s+" );

			int points = StringUtilities.parseInt( splitCondition[ 0 ] );
			int statIndex = conditionString.indexOf( "mus" ) != -1 ? 0 : conditionString.indexOf( "mys" ) != -1 ? 1 : 2;

			GoalManager.GOAL_SUBSTATS_COUNTS[ statIndex ] = (int) KoLCharacter.calculateSubpoints( points, 0 );
			GoalManager.GOAL_SUBSTATS_COUNTS[ statIndex ] =
				Math.max(
					0,
					GoalManager.GOAL_SUBSTATS_COUNTS[ statIndex ] - (int) ( conditionString.indexOf( "mus" ) != -1 ? KoLCharacter.getTotalMuscle() : conditionString.indexOf( "mys" ) != -1 ? KoLCharacter.getTotalMysticality() : KoLCharacter.getTotalMoxie() ) );

			return GoalManager.GOAL_SUBSTATS;
		}

		if ( conditionString.endsWith( "health" ) || conditionString.endsWith( "mana" ) )
		{
			String type;
			int max, current;

			if ( conditionString.endsWith( "health" ) )
			{
				type = AdventureResult.HP;
				max = KoLCharacter.getMaximumHP();
				current = KoLCharacter.getCurrentHP();
			}
			else
			{
				type = AdventureResult.MP;
				max = KoLCharacter.getMaximumMP();
				current = KoLCharacter.getCurrentMP();
			}

			String numberString = conditionString.split( "\\s+" )[ 0 ];
			int points;

			if ( numberString.endsWith( "%" ) )
			{
				int num = StringUtilities.parseInt( numberString.substring( 0, numberString.length() - 1 ) );
				points = (int) ( (float) num * (float) max / 100.0f );
			}
			else
			{
				points = StringUtilities.parseInt( numberString );
			}

			points -= current;

			AdventureResult condition = new AdventureResult( type, points );

			condition = condition.getInstance( condition.getCount() - GoalManager.getGoalCount( condition ));

			return condition;
		}

		if ( conditionString.endsWith( "outfit" ) )
		{
			// Usage: conditions add <location> outfit
			String outfitLocation;

			if ( conditionString.equals( "outfit" ) )
			{
				outfitLocation = Preferences.getString( "lastAdventure" );
			}
			else
			{
				outfitLocation = conditionString.substring( 0, conditionString.length() - 7 );
			}

			// Try to support outfit names by mapping some outfits to their locations
			if ( outfitLocation.equals( "guard" ) || outfitLocation.equals( "elite" ) || outfitLocation.equals( "elite guard" ) )
			{
				outfitLocation = "treasury";
			}

			if ( outfitLocation.equals( "rift" ) )
			{
				outfitLocation = "battlefield";
			}

			if ( outfitLocation.equals( "cloaca-cola" ) || outfitLocation.equals( "cloaca cola" ) )
			{
				outfitLocation = "cloaca";
			}

			if ( outfitLocation.equals( "dyspepsi-cola" ) || outfitLocation.equals( "dyspepsi cola" ) )
			{
				outfitLocation = "dyspepsi";
			}

			KoLAdventure lastAdventure = AdventureDatabase.getAdventure( outfitLocation );

			if ( !EquipmentManager.addOutfitConditions( lastAdventure ) )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ERROR_STATE, "No outfit corresponds to " + lastAdventure.getAdventureName() + "." );
			}

			return null;
		}

		AdventureResult rv = AdventureResult.WildcardResult.getInstance( conditionString );
		return rv != null ? rv : ItemFinder.getFirstMatchingItem( conditionString );
	}
}
