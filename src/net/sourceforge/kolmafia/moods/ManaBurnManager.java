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

package net.sourceforge.kolmafia.moods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;

import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.BreakfastManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ManaBurnManager
{
	public static final void burnExtraMana( final boolean isManualInvocation )
	{
		String nextBurnCast;
		
		float manaBurnTrigger = Preferences.getFloat( "manaBurningTrigger" );
		if ( !isManualInvocation && KoLCharacter.getCurrentMP() <
			(int) (manaBurnTrigger * KoLCharacter.getMaximumMP()) )
		{
			return;
		}
	
		boolean was = MoodManager.isExecuting;
		MoodManager.isExecuting = true;
	
		int currentMP = -1;
	
		while ( currentMP != KoLCharacter.getCurrentMP() && ( nextBurnCast = ManaBurnManager.getNextBurnCast() ) != null )
		{
			currentMP = KoLCharacter.getCurrentMP();
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( nextBurnCast );
		}
	
		MoodManager.isExecuting = was;
	}

	public static final void burnMana( int minimum )
	{
		String nextBurnCast;
	
		boolean was = MoodManager.isExecuting;
		MoodManager.isExecuting = true;
	
		minimum = Math.max( 0, minimum );
		int currentMP = -1;
	
		while ( currentMP != KoLCharacter.getCurrentMP() && ( nextBurnCast = ManaBurnManager.getNextBurnCast( minimum ) ) != null )
		{
			currentMP = KoLCharacter.getCurrentMP();
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( nextBurnCast );
		}
	
		MoodManager.isExecuting = was;
	}

	public static final String getNextBurnCast()
	{
		// Punt immediately if mana burning is disabled
	
		float manaBurnPreference = Preferences.getFloat( "manaBurningThreshold" );
		if ( manaBurnPreference < 0.0f )
		{
			return null;
		}
	
		float manaRecoverPreference = Preferences.getFloat( "mpAutoRecovery" );
		int minimum = (int) ( Math.max( manaBurnPreference, manaRecoverPreference ) * (float) KoLCharacter.getMaximumMP() ) + 1;
		return ManaBurnManager.getNextBurnCast( minimum );
	}

	private static final String getNextBurnCast( final int minimum )
	{
		// Punt immediately if already burned enough or must recover MP
	
		int allowedMP = KoLCharacter.getCurrentMP() - minimum;
		if ( allowedMP <= 0 )
		{
			return null;
		}
	
		// Pre-calculate possible breakfast/libram skill
	
		boolean onlyMood = !Preferences.getBoolean( "allowNonMoodBurning" );
		String breakfast = Preferences.getBoolean( "allowSummonBurning" ) ?
			ManaBurnManager.considerBreakfastSkill( minimum ) : null;
		int summonThreshold = Preferences.getInteger( "manaBurnSummonThreshold" );
		int durationLimit = Preferences.getInteger( "maxManaBurn" ) + KoLCharacter.getAdventuresLeft();
		ManaBurn chosen = null;
		ArrayList burns = new ArrayList();
	
		// Rather than maintain mood-related buffs only, maintain any
		// active effect that the character can auto-cast. Examine all
		// active effects in order from lowest duration to highest.
	
		for ( int i = 0; i < KoLConstants.activeEffects.size() && KoLmafia.permitsContinue(); ++i )
		{
			AdventureResult currentEffect = (AdventureResult) KoLConstants.activeEffects.get( i );
			String skillName = UneffectRequest.effectToSkill( currentEffect.getName() );
	
			// Only cast if the player knows the skill
	
			if ( !SkillDatabase.contains( skillName ) || !KoLCharacter.hasSkill( skillName ) )
			{
				continue;
			}
	
			int skillId = SkillDatabase.getSkillId( skillName );
			
			int priority = Preferences.getInteger( "skillBurn" + skillId ) + 100;
			// skillBurnXXXX values offset by 100 so that missing prefs read
			// as 100% by default.
			// All skills that were previously hard-coded as unextendable are
			// now indicated by skillBurnXXXX = -100 in defaults.txt, so they
			// can be overridden if desired.
	
			int currentDuration = currentEffect.getCount();
			int currentLimit = durationLimit * Math.min( 100, priority ) / 100;
	
			// If we already have 1000 turns more than the number
			// of turns the player has available, that's enough.
			// Also, if we have more than twice the turns of some
			// more expensive buff, save up for that one instead
			// of extending this one.
	
			if ( currentDuration >= currentLimit )
			{
				continue;
			}
	
			// If the player wants to burn mana on summoning
			// skills, only do so if all potential effects have at
			// least 10 turns remaining.
	
			if ( breakfast != null && currentDuration >= summonThreshold )
			{
				return breakfast;
			}
	
			// If the player only wants to cast buffs related to
			// their mood, then skip the buff if it's not in the
			// any of the player's moods.
	
			if ( onlyMood && !MoodManager.effectInMood( currentEffect ) )
			{
				continue;
			}
	
			// If we don't have enough MP for this skill, consider
			// extending some cheaper effect - but only up to twice
			// the turns of this effect, so that a slow but steady
			// MP gain won't be used exclusively on the cheaper effect.
	
			if ( SkillDatabase.getMPConsumptionById( skillId ) > allowedMP )
			{
				durationLimit = Math.max( 10, Math.min( currentDuration * 2, durationLimit ) );
				continue;
			}
	
			ManaBurn b = new ManaBurn( skillId, skillName, currentDuration, currentLimit );
			if ( chosen == null )
			{
				chosen = b;
			}
	
			burns.add( b );
			breakfast = null; // we're definitely extending an effect
		}
	
		if ( chosen == null )
		{
			// No buff found. Return possible breakfast/libram skill
			if ( breakfast != null ||
				allowedMP < Preferences.getInteger( "lastChanceThreshold" ) )
			{
				return breakfast;
			}
			
			// TODO: find the known but currently unused skill with the
			// highest skillBurnXXXX value (>0), and cast it.
			
			// Last chance: let the user specify something to do with this
			// MP that we can't find any other use for.
			String cmd = Preferences.getString( "lastChanceBurn" );
			if ( cmd.length() == 0 )
			{
				return null;
			}
			
			return StringUtilities.globalStringReplace( cmd, "#",
				String.valueOf( allowedMP ) );
		}
	
		// Simulate casting all of the extendable skills in a balanced
		// manner, to determine the final count of the chosen skill -
		// rather than making multiple server requests.
		Iterator i = burns.iterator();
		while ( i.hasNext() )
		{
			ManaBurn b = (ManaBurn) i.next();
			
			if ( !b.isCastable( allowedMP ) )
			{
				i.remove();
				continue;
			}
			
			allowedMP -= b.simulateCast();
			Collections.sort( burns );
			i = burns.iterator();
		}
	
		return chosen.toString();
	}

	private static final String considerBreakfastSkill( final int minimum )
	{
		for ( int i = 0; i < UseSkillRequest.BREAKFAST_SKILLS.length; ++i )
		{
			if ( !KoLCharacter.hasSkill( UseSkillRequest.BREAKFAST_SKILLS[ i ] ) )
			{
				continue;
			}
			if ( UseSkillRequest.BREAKFAST_SKILLS[ i ].equals( "Pastamastery" ) && !KoLCharacter.canEat() )
			{
				continue;
			}
			if ( UseSkillRequest.BREAKFAST_SKILLS[ i ].equals( "Advanced Cocktailcrafting" ) && !KoLCharacter.canDrink() )
			{
				continue;
			}
	
			UseSkillRequest skill = UseSkillRequest.getInstance( UseSkillRequest.BREAKFAST_SKILLS[ i ] );
	
			int maximumCast = skill.getMaximumCast();
	
			if ( maximumCast == 0 )
			{
				continue;
			}
	
			int availableMP = KoLCharacter.getCurrentMP() - minimum;
			int mpPerUse = SkillDatabase.getMPConsumptionById( skill.getSkillId() );
			int castCount = Math.min( maximumCast, availableMP / mpPerUse );
	
			if ( castCount > 0 )
			{
				return "cast " + castCount + " " + UseSkillRequest.BREAKFAST_SKILLS[ i ];
			}
		}
	
		return ManaBurnManager.considerLibramSummon( minimum );
	}

	private static final String considerLibramSummon( final int minimum )
	{
		int castCount = SkillDatabase.libramSkillCasts( KoLCharacter.getCurrentMP() - minimum );
		if ( castCount <= 0 )
		{
			return null;
		}
	
		List castable =	 BreakfastManager.getBreakfastLibramSkills();
		int skillCount = castable.size();
	
		if ( skillCount == 0 )
		{
			return null;
		}
		
		int nextCast = Preferences.getInteger( "libramSummons" );
		StringBuffer buf = new StringBuffer();
		for ( int i = 0; i < skillCount; ++i )
		{
			int thisCast = (castCount + skillCount - 1 - i) / skillCount;
			if ( thisCast <= 0 ) continue;
			buf.append( "cast " );
			buf.append( thisCast );
			buf.append( " " );
			buf.append( (String) castable.get( (i + nextCast) % skillCount ) );
			buf.append( ";" );
		}
	
		return buf.toString();
	}

}
