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

import java.util.ArrayList;
import java.util.List;

import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.HermitRequest;

public class GoalManager
{
	public static final AdventureResult GOAL_CHOICE = new AdventureResult( AdventureResult.CHOICE, 1 );
	public static final AdventureResult GOAL_AUTOSTOP = new AdventureResult( AdventureResult.AUTOSTOP, 1 );

	public static final int[] GOAL_SUBSTATS_COUNTS = new int[ 3 ];
	public static final AdventureResult GOAL_SUBSTATS =
		new AdventureResult.AdventureMultiResult( AdventureResult.SUBSTATS, GOAL_SUBSTATS_COUNTS );

	private static final LockableListModel goals = new LockableListModel();

	public static final LockableListModel getGoals()
	{
		return GoalManager.goals;
	}

	public static final String getGoalString()
	{
		StringBuffer conditionString = new StringBuffer();

		for ( int i = 0; i < GoalManager.goals.size(); ++i )
		{
			if ( i > 0 )
			{
				conditionString.append( ", " );
			}

			AdventureResult goal = (AdventureResult) GoalManager.goals.get( i );
			conditionString.append( goal.toConditionString() );
		}

		return conditionString.toString();
	}

	public static final void clearGoals()
	{
		GoalManager.goals.clear();
		
		for ( int i = 0; i < 3; ++i )
		{
			GoalManager.GOAL_SUBSTATS_COUNTS[ i ] = 0;
		}
	}

	public static final boolean hasGoals()
	{
		return !GoalManager.goals.isEmpty();
	}

	public static final boolean hasItemGoal( int itemId )
	{
		return GoalManager.hasGoal( ItemPool.get( itemId, 1 ) );
	}

	public static final boolean hasGoal( AdventureResult goal )
	{
		return GoalManager.goals.contains( goal );
	}

	public static final int getGoalCount( AdventureResult goal )
	{
		return goal.getCount( GoalManager.goals );
	}

	public static final void addItemGoal( int itemId, int count )
	{
		GoalManager.addGoal( ItemPool.get( itemId, count ) );
	}

	public static final void addGoal( AdventureResult goal )
	{
		String goalName = goal.getName();
		
		if ( goalName.equals( AdventureResult.SUBSTATS ) )
		{
			if ( !GoalManager.goals.contains( goal ) )
			{
				GoalManager.goals.add( goal );
			}
		}
		else
		{
			AdventureResult.addResultToList( GoalManager.goals, goal );
		}

		if ( goal.getCount() > 0 )
		{
			RequestLogger.printLine( "Condition added: " + goal );
		}
		else
		{
			RequestLogger.printLine( "Condition removed: " + goal.getNegation() );
		}
	}

	public static final void setItemGoal( int itemId, int count )
	{
		GoalManager.setGoal( ItemPool.get( itemId, count ) );
	}

	public static final void setGoal( AdventureResult goal )
	{
		String goalName = goal.getName();

		if ( goalName.equals( AdventureResult.SUBSTATS ) )
		{
			if ( goal.getCount() == 0 )
			{
				GoalManager.goals.remove( goal );
			}
			else if ( !GoalManager.goals.contains( goal ) )
			{
				GoalManager.addGoal( goal );
			}

			return;
		}
			
		int currentGoalCount = goal.getCount( GoalManager.goals );
		int desiredGoalCount = goal.getCount();

		if ( currentGoalCount >= desiredGoalCount )
		{
			RequestLogger.printLine( "Condition already exists: " + goal );
			return;
		}

		int currentCount = 0;

		if ( goal.isItem() )
		{
			if ( goal.getItemId() == HermitRequest.WORTHLESS_ITEM.getItemId() )
			{
				currentCount = HermitRequest.getWorthlessItemCount( true );
			}
			else
			{
				currentCount = goal.getCount( KoLConstants.inventory );

				if ( Preferences.getBoolean( "autoSatisfyWithCloset" ) )
				{
					currentCount += goal.getCount( KoLConstants.closet );
				}
			}

			for ( int j = 0; j < EquipmentManager.FAMILIAR; ++j )
			{
				if ( EquipmentManager.getEquipment( j ).equals( goal ) )
				{
					++currentCount;
				}
			}
		}

		if ( currentCount >= desiredGoalCount )
		{
			RequestLogger.printLine( "Condition already met: " + goal );
			return;
		}

		goal = goal.getInstance( desiredGoalCount - currentCount );
		GoalManager.addGoal( goal );
	}

	public static final void makeSideTrip( KoLAdventure location, AdventureResult goal )
	{
		List previousGoals = new ArrayList( GoalManager.goals );

		GoalManager.clearGoals();
		GoalManager.addGoal( goal );

		StaticEntity.getClient().makeRequest( location, KoLCharacter.getAdventuresLeft() );

		if ( !GoalManager.goals.isEmpty() )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Unable to obtain " +
				goal );
			GoalManager.clearGoals();
		}

		GoalManager.goals.addAll( previousGoals );
	}

	public static final void checkAutoStop( String message )
	{
		boolean hasOtherGoals = false;
		
		GoalManager.updateProgress( GoalManager.GOAL_AUTOSTOP );

		if ( GoalManager.goals.isEmpty() )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, message );
		}
		else
		{
			RequestLogger.printLine( message );
			RequestLogger.printLine( "There are still unsatisfied conditions." );
		}
	}

	public static final void updateProgress( AdventureResult goal )
	{
		int goalIndex = GoalManager.goals.indexOf( goal );

		if ( goalIndex == -1 )
		{
			return;
		}

		String goalName = goal.getName();

		if ( goalName.equals( AdventureResult.SUBSTATS ) )
		{
			// If the condition is a substat condition, then zero out the
			// appropriate count and remove if all counts dropped to zero.

			for ( int i = 0; i < 3; ++i )
			{
				if ( GoalManager.GOAL_SUBSTATS_COUNTS[ i ] == 0 )
				{
					continue;
				}

				GoalManager.GOAL_SUBSTATS_COUNTS[ i ] =
					Math.max( 0, GoalManager.GOAL_SUBSTATS_COUNTS[ i ] - goal.getCount( i ) );
			}

			if ( GoalManager.GOAL_SUBSTATS.getCount() == 0 )
			{
				GoalManager.goals.remove( goalIndex );
			}
			else
			{
				GoalManager.goals.fireContentsChanged( GoalManager.goals, goalIndex, goalIndex );
			}

			return;
		}

		AdventureResult previousGoal = (AdventureResult) GoalManager.goals.get( goalIndex );

		if ( previousGoal.getCount() <= goal.getCount() )
		{
			GoalManager.goals.remove( goalIndex );
		}
		else
		{
			goal = previousGoal.getInstance( previousGoal.getCount() - goal.getCount() );
			GoalManager.goals.set( goalIndex, goal );
		}
	}

}
