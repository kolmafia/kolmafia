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

package net.sourceforge.kolmafia.moods;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.SortedListModel;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class MoodManager
{
	private static final AdventureResult[] AUTO_CLEAR =
	{
		new AdventureResult( "Beaten Up", 1, true ),
		new AdventureResult( "Tetanus", 1, true ),
		new AdventureResult( "Amnesia", 1, true ),
		new AdventureResult( "Cunctatitis", 1, true ),
		new AdventureResult( "Hardly Poisoned at All", 1, true ),
		new AdventureResult( "Majorly Poisoned", 1, true ),
		new AdventureResult( "A Little Bit Poisoned", 1, true ),
		new AdventureResult( "Somewhat Poisoned", 1, true ),
		new AdventureResult( "Really Quite Poisoned", 1, true ),
	};

	public static final AdventureResult TURTLING_ROD = ItemPool.get( ItemPool.TURTLING_ROD, 1 );
	public static final AdventureResult EAU_DE_TORTUE = EffectPool.get( Effect.EAU_DE_TORTUE );

	private static Mood currentMood = null;
	private static final SortedListModel<Mood> availableMoods = new SortedListModel<Mood>();
	private static final SortedListModel<MoodTrigger> displayList = new SortedListModel<MoodTrigger>();
	
	static boolean isExecuting = false;

	public static final File getFile()
	{
		return new File( KoLConstants.SETTINGS_LOCATION, KoLCharacter.baseUserName() + "_moods.txt" );
	}

	public static final boolean isExecuting()
	{
		return MoodManager.isExecuting;
	}

	public static final void updateFromPreferences()
	{
		MoodTrigger.clearKnownSources();
		MoodManager.availableMoods.clear();

		MoodManager.currentMood = null;
		MoodManager.displayList.clear();

		String currentMood = Preferences.getString( "currentMood" );
		MoodManager.loadSettings();

		MoodManager.setMood( currentMood );
		MoodManager.saveSettings();
	}

	public static final LockableListModel<Mood> getAvailableMoods()
	{
		return MoodManager.availableMoods;
	}

	/**
	 * Sets the current mood to be executed to the given mood. Also ensures that all defaults are loaded for the given
	 * mood if no data exists.
	 */

	public static final void setMood( String newMoodName )
	{
		if ( newMoodName == null || newMoodName.trim().equals( "" ) )
		{
			newMoodName = "default";
		}

		if ( newMoodName.equals( "clear" ) || newMoodName.equals( "autofill" ) || newMoodName.startsWith( "exec" ) || newMoodName.startsWith( "repeat" ) )
		{
			return;
		}

		Preferences.setString( "currentMood", newMoodName );

		MoodManager.currentMood = null;
		Mood newMood = new Mood( newMoodName );
		
		for ( Mood mood : MoodManager.availableMoods )
		{
			if ( mood.equals( newMood) )
			{
				MoodManager.currentMood = mood;
				
				if ( newMoodName.indexOf( " extends " ) != -1 || newMoodName.indexOf( "," ) != -1 )
				{
					MoodManager.currentMood.setParentNames( newMood.getParentNames() );
				}
				
				break;
			}
		}

		if ( MoodManager.currentMood == null )
		{
			MoodManager.currentMood = newMood;			
			MoodManager.availableMoods.remove( MoodManager.currentMood );
			MoodManager.availableMoods.add( MoodManager.currentMood );
		}

		MoodManager.displayList.clear();
		MoodManager.displayList.addAll( MoodManager.currentMood.getTriggers() );
		
		MoodManager.availableMoods.setSelectedItem( MoodManager.currentMood );
	}

	/**
	 * Retrieves the model associated with the given mood.
	 */

	public static final LockableListModel<MoodTrigger> getTriggers()
	{
		return MoodManager.displayList;
	}
	
	public static final List<MoodTrigger> getTriggers( String moodName )
	{
		if ( moodName == null || moodName.length() == 0 )
		{
			return Collections.EMPTY_LIST;
		}
		
		for ( Mood mood : MoodManager.availableMoods)
		{
			if ( mood.getName().equals( moodName ) )
			{
				return mood.getTriggers();
			}
		}
		
		return Collections.EMPTY_LIST;
	}

	/**
	 * Adds a trigger to the temporary mood settings.
	 */

	public static final MoodTrigger addTrigger( final String type, final String name, final String action )
	{
		MoodTrigger trigger = MoodTrigger.constructNode( type + " " + name + " => " + action );

		if ( MoodManager.currentMood.addTrigger( trigger ) )
		{
			MoodManager.displayList.remove( trigger );
			MoodManager.displayList.add( trigger );
		}

		return trigger;
	}

	/**
	 * Removes all the current displayList.
	 */

	public static final void removeTriggers( final Object[] triggers )
	{
		for ( int i = 0; i < triggers.length; ++i )
		{
			MoodTrigger trigger = (MoodTrigger) triggers[ i ];

			if ( MoodManager.currentMood.removeTrigger( trigger ) )
			{
				MoodManager.displayList.remove( trigger );
			}
		}
	}

	public static final void removeTriggers( final Collection<MoodTrigger> triggers )
	{
		for ( MoodTrigger trigger : triggers )
		{
			if ( MoodManager.currentMood.removeTrigger( trigger ) )
			{
				MoodManager.displayList.remove( trigger );
			}
		}
	}

	public static final void minimalSet()
	{
		String currentMood = Preferences.getString( "currentMood" );
		if ( currentMood.equals( "apathetic" ) )
		{
			return;
		}

		// If there's any effects the player currently has and there
		// is a known way to re-acquire it (internally known, anyway),
		// make sure to add those as well.

		AdventureResult[] effects = new AdventureResult[ KoLConstants.activeEffects.size() ];
		KoLConstants.activeEffects.toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			String action = MoodManager.getDefaultAction( "lose_effect", effects[ i ].getName() );
			if ( action != null && !action.equals( "" ) )
			{
				MoodManager.addTrigger( "lose_effect", effects[ i ].getName(), action );
			}
		}
	}

	/**
	 * Fills up the trigger list automatically.
	 */

	private static final String [] hardcoreThiefBuffs = new String[]
	{
		"Fat Leon's Phat Loot Lyric",
		"The Moxious Madrigal",
		"Aloysius' Antiphon of Aptitude",
		"The Sonata of Sneakiness",
		"The Psalm of Pointiness",
		"Ur-Kel's Aria of Annoyance"
	};

	private static final String [] softcoreThiefBuffs = new String[]
	{
		"Fat Leon's Phat Loot Lyric",
		"Aloysius' Antiphon of Aptitude",
		"Ur-Kel's Aria of Annoyance",
		"The Sonata of Sneakiness",
		"Jackasses' Symphony of Destruction",
		"Cletus's Canticle of Celerity"
	};

	private static final String [] rankedBorisSongs = new String[]
	{
		"Song of Fortune",
		"Song of Accompaniment",
		// Can't actually pick the following, since it is in the same
		// skill tree as the preceding Songs
		"Song of Solitude",
		"Song of Cockiness",
	};

	public static final void maximalSet()
	{
		String currentMood = Preferences.getString( "currentMood" );
		if ( currentMood.equals( "apathetic" ) )
		{
			return;
		}

		UseSkillRequest[] skills = new UseSkillRequest[ KoLConstants.availableSkills.size() ];
		KoLConstants.availableSkills.toArray( skills );

		ArrayList<String> thiefSkills = new ArrayList<String>();
		ArrayList<String> borisSongs = new ArrayList<String>();

		for ( int i = 0; i < skills.length; ++i )
		{
			int skillId = skills[ i ].getSkillId();

			if ( skillId < 1000 )
			{
				continue;
			}

			// Combat rate increasers are not handled by mood
			// autofill, since KoLmafia has a preference for
			// non-combats in the area below.
			// Musk of the Moose, Carlweather's Cantata of Confrontation,
			// Song of Battle

			if ( skillId == 1019 || skillId == 6016 || skillId == 11019 )
			{
				continue;
			}

			// Skip skills that aren't mood appropriate because they add effects
			// outside of battle.
			// Canticle of Carboloading, The Ode to Booze,
			// Inigo's Incantation of Inspiration, Song of the Glorious Lunch

			if ( skillId == 3024 || skillId == 6014 || skillId == 6028 || skillId == 11023 )
			{
				continue;
			}

			String skillName = skills[ i ].getSkillName();

			if ( SkillDatabase.isAccordionThiefSong( skillId ) )
			{
				thiefSkills.add( skillName );
				continue;
			}

			if ( skillId >= 11000 && skillId < 12000 )
			{
				if ( SkillDatabase.isSong( skillId ) )
				{
					borisSongs.add( skillName );
					continue;
				}
			}

			String effectName = UneffectRequest.skillToEffect( skillName );
			if ( EffectDatabase.contains( effectName ) )
			{
				String action = MoodManager.getDefaultAction( "lose_effect", effectName );
				MoodManager.addTrigger( "lose_effect", effectName, action );
			}
		}

		// If we know Boris Songs, pick one
		if ( !borisSongs.isEmpty() )
		{
			MoodManager.pickSkills( borisSongs, 1, MoodManager.rankedBorisSongs );
		}

		// If we know Accordion Thief Songs, pick some
		if ( !thiefSkills.isEmpty() )
		{
			String[] rankedBuffs =
				KoLCharacter.isHardcore() ?
				MoodManager.hardcoreThiefBuffs :
				MoodManager.softcoreThiefBuffs;
			MoodManager.pickSkills( thiefSkills, UseSkillRequest.songLimit(), rankedBuffs );
		}

		// Now add in all the buffs from the minimal buff set, as those
		// are included here.

		MoodManager.minimalSet();
	}

	private static final void pickSkills( final List<String> skills, final int limit, final String [] rankedBuffs )
	{
		if ( skills.isEmpty() )
		{
			return;
		}

		int skillCount = skills.size();

		// If we know fewer skills than our capacity, add them all

		if ( skillCount <= limit )
		{
			String[] skillNames = new String[ skillCount ];
			skills.toArray( skillNames );

			for ( int i = 0; i < skillNames.length; ++i )
			{
				String effectName = UneffectRequest.skillToEffect( skillNames[ i ] );
				MoodManager.addTrigger( "lose_effect", effectName, "cast " + skillNames[ i ] );
			}

			return;
		}

		// Otherwise, pick from the ranked list of "useful" skills

		int foundSkillCount = 0;
		for ( int i = 0; i < rankedBuffs.length && foundSkillCount < limit; ++i )
		{
			if ( KoLCharacter.hasSkill( rankedBuffs[ i ] ) )
			{
				String effectName =  UneffectRequest.skillToEffect( rankedBuffs[ i ] );
				MoodManager.addTrigger( "lose_effect",effectName, "cast " + rankedBuffs[ i ] );
				++foundSkillCount;
			}
		}
	}

	/**
	 * Deletes the current mood and sets the current mood to apathetic.
	 */

	public static final void deleteCurrentMood()
	{
		MoodManager.displayList.clear();

		Mood current = MoodManager.currentMood;
		if ( current.getName().equals( "default" ) )
		{
			MoodManager.removeTriggers( current.getTriggers() );
			return;
		}

		MoodManager.availableMoods.remove( current );
		MoodManager.setMood( "apathetic" );
	}

	/**
	 * Duplicates the current trigger list into a new list
	 */

	public static final void copyTriggers( final String newMoodName )
	{
		// Copy displayList from current list, then
		// create and switch to new list

		Mood newMood = new Mood( newMoodName );
		newMood.copyFrom( MoodManager.currentMood );
		
		MoodManager.availableMoods.add( newMood );
		MoodManager.setMood( newMoodName );
	}

	/**
	 * Executes all the mood displayList for the current mood.
	 */

	public static final void execute()
	{
		MoodManager.execute( -1 );
	}

	public static final boolean effectInMood( final AdventureResult effect )
	{
		return MoodManager.currentMood.isTrigger( effect );
	}

	public static final void execute( final int multiplicity )
	{
		if ( KoLmafia.refusesContinue() )
		{
			return;
		}

		if ( !MoodManager.willExecute( multiplicity ) )
		{
			return;
		}

		MoodManager.isExecuting = true;

		MoodTrigger current = null;

		AdventureResult[] effects = new AdventureResult[ KoLConstants.activeEffects.size() ];
		KoLConstants.activeEffects.toArray( effects );

		// If you have too many accordion thief buffs to execute
		// your displayList, then shrug off your extra buffs, but
		// only if the user allows for this.

		// First we determine which buffs are already affecting the
		// character in question.

		ArrayList<AdventureResult> thiefBuffs = new ArrayList<AdventureResult>();
		for ( int i = 0; i < effects.length; ++i )
		{
			String skillName = UneffectRequest.effectToSkill( effects[ i ].getName() );
			if ( SkillDatabase.contains( skillName ) )
			{
				int skillId = SkillDatabase.getSkillId( skillName );
				if ( SkillDatabase.isAccordionThiefSong( skillId ) )
				{
					thiefBuffs.add( effects[ i ] );
				}
			}
		}

		// Then, we determine the triggers which are thief skills, and
		// thereby would be cast at this time.

		ArrayList<AdventureResult> thiefKeep = new ArrayList<AdventureResult>();
		ArrayList<AdventureResult> thiefNeed = new ArrayList<AdventureResult>();
		
		List<MoodTrigger> triggers = MoodManager.currentMood.getTriggers();
		for ( MoodTrigger trigger : triggers )
		{
			if ( trigger.isThiefTrigger() )
			{
				AdventureResult effect = trigger.getEffect();
				
				if ( thiefBuffs.remove( effect ) )
				{	// Already have this one
					thiefKeep.add( effect );
				}
				else
				{	// New or completely expired buff - we may
					// need to shrug a buff to make room for it.
					thiefNeed.add( effect );
				}
			}
		}

		int buffsToRemove = thiefNeed.isEmpty() ? 0 :
			thiefBuffs.size() + thiefKeep.size() + thiefNeed.size() - UseSkillRequest.songLimit();

		for ( int i = 0; i < buffsToRemove && i < thiefBuffs.size(); ++i )
		{
			KoLmafiaCLI.DEFAULT_SHELL.executeLine( "uneffect " + thiefBuffs.get( i ).getName() );
		}

		// Now that everything is prepared, go ahead and execute
		// the displayList which have been set.  First, start out
		// with any skill casting.
		
		for ( MoodTrigger trigger : triggers )
		{
			if ( KoLmafia.refusesContinue() )
			{
				break;
			}

			if ( trigger.isSkill() )
			{
				trigger.execute( multiplicity );
			}
		}

		for ( MoodTrigger trigger : triggers )
		{
			if ( !trigger.isSkill() )
			{
				trigger.execute( multiplicity );
			}
		}

		MoodManager.isExecuting = false;
	}

	public static final boolean willExecute( final int multiplicity )
	{
		if ( !MoodManager.currentMood.isExecutable() )
		{
			return false;
		}

		boolean willExecute = false;
		
		List<MoodTrigger> triggers = MoodManager.currentMood.getTriggers();
		for ( MoodTrigger trigger : triggers )
		{
			willExecute |= trigger.shouldExecute( multiplicity );
		}

		return willExecute;
	}

	public static final List<AdventureResult> getMissingEffects()
	{
		List<MoodTrigger> triggers = MoodManager.currentMood.getTriggers();

		if ( triggers.isEmpty() )
		{
			return Collections.EMPTY_LIST;
		}

		ArrayList<AdventureResult> missing = new ArrayList<AdventureResult>();
		for ( MoodTrigger trigger : triggers )
		{
			if ( trigger.getType().equals( "lose_effect" ) && !trigger.matches() )
			{
				missing.add( trigger.getEffect() );
			}
		}

		// Special case: if the character has a turtling rod equipped,
		// assume the Eau de Tortue is a possibility

		if ( KoLCharacter.hasEquipped( MoodManager.TURTLING_ROD, EquipmentManager.OFFHAND ) &&
		     !KoLConstants.activeEffects.contains( MoodManager.EAU_DE_TORTUE ) )
		{
			missing.add( MoodManager.EAU_DE_TORTUE );
		}

		return missing;
	}

	public static final void removeMalignantEffects()
	{
		for ( int i = 0; i < MoodManager.AUTO_CLEAR.length && KoLmafia.permitsContinue(); ++i )
		{
			AdventureResult effect = MoodManager.AUTO_CLEAR[ i ];

			if ( KoLConstants.activeEffects.contains( effect ) )
			{
				RequestThread.postRequest( new UneffectRequest( effect ) );
			}
		}
	}

	public static final int getMaintenanceCost()
	{
		List<MoodTrigger> triggers = MoodManager.currentMood.getTriggers();

		if ( triggers.isEmpty() )
		{
			return 0;
		}

		int runningTally = 0;

		// Iterate over the entire list of applicable triggers,
		// locate the ones which involve spellcasting, and add
		// the MP cost for maintenance to the running tally.

		for ( MoodTrigger trigger : triggers )
		{
			if ( !trigger.getType().equals( "lose_effect" ) || !trigger.shouldExecute( -1 ) )
			{
				continue;
			}

			String action = trigger.getAction();
			if ( !action.startsWith( "cast" ) && !action.startsWith( "buff" ) )
			{
				continue;
			}

			int spaceIndex = action.indexOf( " " );
			if ( spaceIndex == -1 )
			{
				continue;
			}

			action = action.substring( spaceIndex + 1 );

			int multiplier = 1;

			if ( Character.isDigit( action.charAt( 0 ) ) )
			{
				spaceIndex = action.indexOf( " " );
				multiplier = StringUtilities.parseInt( action.substring( 0, spaceIndex ) );
				action = action.substring( spaceIndex + 1 );
			}

			String skillName = SkillDatabase.getSkillName( action );
			if ( skillName != null )
			{
				runningTally +=
					SkillDatabase.getMPConsumptionById( SkillDatabase.getSkillId( skillName ) ) * multiplier;
			}
		}

		// Running tally calculated, return the amount of
		// MP required to sustain this mood.

		return runningTally;
	}

	/**
	 * Stores the settings maintained in this <code>MoodManager</code> object to disk for later retrieval.
	 */

	public static final void saveSettings()
	{
		PrintStream writer = LogStream.openStream( getFile(), true );

		for ( Mood mood : MoodManager.availableMoods )
		{
			writer.println( mood.toSettingString() );
		}

		writer.close();
	}

	/**
	 * Loads the settings located in the given file into this object. Note that all settings are overridden; if the
	 * given file does not exist, the current global settings will also be rewritten into the appropriate file.
	 */

	public static final void loadSettings()
	{
		MoodManager.availableMoods.clear();

		Mood mood = new Mood( "apathetic" );
		MoodManager.availableMoods.add( mood );
		
		mood = new Mood( "default" );
		MoodManager.availableMoods.add( mood );
		
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			BufferedReader reader = FileUtilities.getReader( getFile() );
			
			String line;

			while ( ( line = reader.readLine() ) != null )
			{
				line = line.trim();
				
				if ( line.length() == 0 )
				{
					continue;
				}
				
				if ( !line.startsWith( "[" ) )
				{
					mood.addTrigger( MoodTrigger.constructNode( line ) );
					continue;
				}

				int closeBracketIndex = line.indexOf( "]" );
				
				if ( closeBracketIndex == -1 )
				{
					continue;
				}

				String moodName = line.substring( 1, closeBracketIndex );
				mood = new Mood( moodName );

				MoodManager.availableMoods.remove( mood );
				MoodManager.availableMoods.add( mood );
			}

			reader.close();
			reader = null;

			MoodManager.setMood( Preferences.getString( "currentMood" ) );
		}
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	public static final String getDefaultAction( final String type, final String name )
	{
		if ( type == null || name == null )
		{
			return "";
		}

		// We can look at the displayList list to see if it matches
		// your current mood.  That way, the "default action" is
		// considered whatever your current mood says it is.

		String action = "";

		List<MoodTrigger> triggers = ( MoodManager.currentMood == null ) ? Collections.EMPTY_LIST : MoodManager.currentMood.getTriggers();

		for ( MoodTrigger trigger : triggers )
		{
			if ( trigger.getType().equals( type ) && trigger.getName().equals( name ) )
			{
				action = trigger.getAction();
				break;
			}
		}

		if ( type.equals( "unconditional" ) )
		{
			return action;
		}
		else if ( type.equals( "lose_effect" ) )
		{
			if ( action.equals( "" ) )
			{
				action = EffectDatabase.getDefaultAction( name );

				if ( action == null )
				{
					action = MoodTrigger.getKnownSources( name );
				}
			}

			return action;
		}
		else
		{
			if ( action.equals( "" ) )
			{
				action = "uneffect " + name;
			}

			return action;
		}
	}

	public static final boolean currentlyExecutable( final AdventureResult effect, final String action )
	{
		// It's always OK to boost a stackable effect.
		// Otherwise, it's only OK if it's not active.

		return !MoodManager.unstackableAction( action ) || !KoLConstants.activeEffects.contains( effect );
	}

	public static final boolean unstackableAction( final String action )
	{
		return
			action.indexOf( "absinthe" ) != -1 ||
			action.indexOf( "astral mushroom" ) != -1 ||
			action.indexOf( "oasis" ) != -1 ||
			action.indexOf( "turtle pheromones" ) != -1 ||
			action.indexOf( "gong" ) != -1;
	}

	public static final boolean canMasterTrivia()
	{
		if ( KoLCharacter.canInteract() )
		{
			return true;
		}
		return ( InventoryManager.getAccessibleCount( ItemPool.WHAT_CARD ) > 0 
					&& InventoryManager.getAccessibleCount( ItemPool.WHEN_CARD ) > 0 
					&& InventoryManager.getAccessibleCount( ItemPool.WHERE_CARD ) > 0 
					&& InventoryManager.getAccessibleCount( ItemPool.WHO_CARD ) > 0 );
	}
}
