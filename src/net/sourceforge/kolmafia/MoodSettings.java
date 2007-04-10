/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import net.java.dev.spellcast.utilities.SortedListModel;

public abstract class MoodSettings implements KoLConstants
{
	static
	{
		// Renaming data files to make then easier to find for most
		// people (so they aren't afraid to open them).

		StaticEntity.renameDataFiles( "kms", "moods" );
	}

	private static final AdventureResult PENDANT = new AdventureResult( 1235, 1 );

	private static String lastUsername = "";
	private static AdventureResult songWeapon = null;
	private static int thiefTriggerLimit = 3;

	private static File settingsFile = null;
	private static TreeMap reference = new TreeMap();

	private static boolean isExecuting = false;
	private static ArrayList thiefTriggers = new ArrayList();

	private static SortedListModel mappedList = null;
	private static SortedListModel displayList = new SortedListModel();

	private static SortedListModel availableMoods = new SortedListModel();

	public static final String settingsFileName()
	{	return "moods_" + KoLCharacter.baseUserName() + ".txt";
	}

	public static boolean isExecuting()
	{	return isExecuting;
	}

	public static final void restoreDefaults()
	{
		reference.clear();
		thiefTriggers.clear();
		availableMoods.clear();
		displayList.clear();

		String currentMood = StaticEntity.getProperty( "currentMood" );
		if ( KoLCharacter.baseUserName().equals( "GLOBAL" ) || currentMood.equals( "" ) )
			return;

		settingsFile = new File( SETTINGS_LOCATION, settingsFileName() );
		loadSettings();

		setMood( currentMood );
		((SortedListModel)reference.get( "apathetic" )).clear();

		saveSettings();
	}

	public static SortedListModel getAvailableMoods()
	{	return availableMoods;
	}

	/**
	 * Sets the current mood to be executed to the given
	 * mood.  Also ensures that all defaults are loaded
	 * for the given mood if no data exists.
	 */

	public static void setMood( String mood )
	{
		mood = (mood == null || mood.trim().equals( "" )) ? "default" : mood.toLowerCase().trim();

		StaticEntity.setProperty( "currentMood", mood );

		ensureProperty( mood );
		availableMoods.setSelectedItem( mood );

		mappedList = (SortedListModel) reference.get( mood );

		displayList.clear();
		displayList.addAll( mappedList );
	}

	/**
	 * Retrieves the model associated with the given mood.
	 */

	public static SortedListModel getTriggers()
	{	return displayList;
	}

	public static void addTriggers( Object [] nodes, int duration )
	{
		removeTriggers( nodes );
		StringBuffer newAction = new StringBuffer();

		for ( int i = 0; i < nodes.length; ++i )
		{
			MoodTrigger mt = (MoodTrigger) nodes[i];
			String [] action = mt.getAction().split( " " );

			newAction.setLength(0);
			newAction.append( action[0] );

			if ( action.length > 1 )
			{
				newAction.append( ' ' );
				int startIndex = 2;

				if ( action[1].charAt(0) == '*' )
				{
					newAction.append( '*' );
				}
				else
				{
					if ( !Character.isDigit( action[1].charAt(0) ) )
						startIndex = 1;

					newAction.append( duration );
				}

				for ( int j = startIndex; j < action.length; ++j )
				{
					newAction.append( ' ' );
					newAction.append( action[j] );
				}
			}

			addTrigger( mt.getType(), mt.getName(), newAction.toString() );
		}
	}

	/**
	 * Adds a trigger to the temporary mood settings.
	 */

	public static void addTrigger( String type, String name, String action )
	{	addTrigger( MoodTrigger.constructNode( type + " " + name + " => " + action ) );
	}

	private static void addTrigger( MoodTrigger node )
	{
		if ( node == null )
			return;

		if ( displayList.contains( node ) )
			removeTrigger( node );

		// Check to make sure that there are fewer than three thief
		// displayList if this is a thief trigger.

		thiefTriggerLimit = KoLCharacter.hasEquipped( PENDANT ) ? 4 : 3;

		if ( node.isThiefTrigger() )
		{
			int thiefTriggerCount = 0;
			for ( int i = 0; i < displayList.size(); ++i )
				if ( ((MoodTrigger)displayList.get(i)).isThiefTrigger() )
					++thiefTriggerCount;

			if ( thiefTriggerCount >= thiefTriggerLimit )
				return;
		}

		mappedList.add( node );
		displayList.add( node );

		if ( node.isThiefTrigger() )
			thiefTriggers.add( node );

		SortedListModel apathy = (SortedListModel) reference.get( "apathetic" );
		if ( apathy != null )
			apathy.clear();
	}

	/**
	 * Removes all the current displayList.
	 */

	public static void removeTriggers( Object [] toRemove )
	{
		for ( int i = 0; i < toRemove.length; ++i )
			removeTrigger( (MoodTrigger) toRemove[i] );
	}

	private static void removeTrigger( MoodTrigger toRemove )
	{
		mappedList.remove( toRemove );
		displayList.remove( toRemove );

		if ( thiefTriggers.contains( toRemove ) )
			thiefTriggers.remove( toRemove );
	}

	public static void minimalSet()
	{
		// Beaten-up removal, as a demo of how to handle beaten-up
		// and poisoned statuses.

		addTrigger( "gain_effect", "Hardly Poisoned at All", getDefaultAction( "gain_effect", "Poisoned" ) );
		addTrigger( "gain_effect", "A Little Bit Poisoned", getDefaultAction( "gain_effect", "Poisoned" ) );
		addTrigger( "gain_effect", "Somewhat Poisoned", getDefaultAction( "gain_effect", "Poisoned" ) );
		addTrigger( "gain_effect", "Really Quite Poisoned", getDefaultAction( "gain_effect", "Poisoned" ) );

		String beatenUpAction = getDefaultAction( "gain_effect", "Beaten Up" );
		if ( KoLCharacter.canInteract() || beatenUpAction.startsWith( "cast" ) )
			addTrigger( "gain_effect", "Beaten Up", beatenUpAction );

		// If there's any effects the player currently has and there
		// is a known way to re-acquire it (internally known, anyway),
		// make sure to add those as well.

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			String action = getDefaultAction( "lose_effect", effects[i].getName() );
			if ( action != null && !action.equals( "" ) )
				addTrigger( "lose_effect", effects[i].getName(), action );
		}
	}

	/**
	 * Fills up the trigger list automatically.
	 */

	public static void maximalSet()
	{
		if ( StaticEntity.getProperty( "currentMood" ).equals( "apathetic" ) )
			return;

		UseSkillRequest [] skills = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skills );

		ArrayList thiefSkills = new ArrayList();

		for ( int i = 0; i < skills.length; ++i )
		{
			if ( skills[i].getSkillId() < 1000 )
				continue;

			// Combat rate increasers are not handled by mood
			// autofill, since KoLmafia has a preference for
			// non-combats in the area below.

			if ( skills[i].getSkillId() == 1019 || skills[i].getSkillId() == 6016 )
				continue;

			if ( skills[i].getSkillId() > 6000 && skills[i].getSkillId() < 7000 )
			{
				thiefSkills.add( skills[i].getSkillName() );
				continue;
			}

			String effectName = UneffectRequest.skillToEffect( skills[i].getSkillName() );
			if ( StatusEffectDatabase.contains( effectName ) )
				addTrigger( "lose_effect", effectName, getDefaultAction( "lose_effect", effectName ) );
		}

		thiefTriggerLimit = KoLCharacter.hasEquipped( PENDANT ) ? 4 : 3;

		if ( !thiefSkills.isEmpty() && thiefSkills.size() <= thiefTriggerLimit )
		{
			String [] skillNames = new String[ thiefSkills.size() ];
			thiefSkills.toArray( skillNames );

			for ( int i = 0; i < skillNames.length; ++i )
			{
				String effectName = UneffectRequest.skillToEffect( skillNames[i] );
				addTrigger( "lose_effect", effectName, getDefaultAction( "lose_effect", effectName ) );
			}
		}
		else if ( !thiefSkills.isEmpty() )
		{
			// To make things more convenient for testing, automatically
			// add some of the common accordion thief buffs if they are
			// available skills.

			String [] rankedBuffs = null;

			if ( KoLCharacter.isHardcore() )
			{
				rankedBuffs = new String [] {
					"Fat Leon's Phat Loot Lyric", "The Moxious Madrigal",
					"Aloysius' Antiphon of Aptitude", "The Sonata of Sneakiness",
					"The Psalm of Pointiness", "Ur-Kel's Aria of Annoyance"
				};
			}
			else
			{
				rankedBuffs = new String [] {
					"Fat Leon's Phat Loot Lyric", "Aloysius' Antiphon of Aptitude",
					"Ur-Kel's Aria of Annoyance", "The Sonata of Sneakiness",
					"Jackasses' Symphony of Destruction", "Cletus's Canticle of Celerity"
				};
			}

			int foundSkillCount = 0;
			for ( int i = 0; i < rankedBuffs.length && foundSkillCount < thiefTriggerLimit; ++i )
			{
				if ( KoLCharacter.hasSkill( rankedBuffs[i] ) )
				{
					++foundSkillCount;
					addTrigger( "lose_effect", UneffectRequest.skillToEffect( rankedBuffs[i] ), "cast " + rankedBuffs[i] );
				}
			}
		}

		// Muscle class characters are rather special when it comes
		// to their default displayList.

		if ( NPCStoreDatabase.contains( "cheap wind-up clock" ) )
			addTrigger( "lose_effect", "Ticking Clock", getDefaultAction( "lose_effect", "Ticking Clock" ) );
		if ( NPCStoreDatabase.contains( "blood of the wereseal" ) )
			addTrigger( "lose_effect", "Temporary Lycanthropy", getDefaultAction( "lose_effect", "Temporary Lycanthropy" ) );

		// Now add in all the buffs from the minimal buff set, as those
		// are included here.

		minimalSet();
	}

	/**
	 * Deletes the current mood and sets the current mood
	 * to apathetic.
	 */

	public static void deleteCurrentMood()
	{
		String currentMood = StaticEntity.getProperty( "currentMood" );

		if ( currentMood.equals( "default" ) )
		{
			mappedList.clear();
			displayList.clear();

			setMood( "default" );
			return;
		}

		reference.remove( currentMood );
		availableMoods.remove( currentMood );

		availableMoods.setSelectedItem( "apathetic" );
		setMood( "apathetic" );
	}

	/**
	 * Duplicates the current trigger list into a new list
	 */

	public static void copyTriggers( String newListName )
	{
		String currentMood = StaticEntity.getProperty( "currentMood" );

		if ( newListName == "" )
			return;

		// Can't copy into apathetic list
		if ( currentMood.equals( "apathetic" ) || newListName.equals( "apathetic" ) )
			return;

		// Copy displayList from current list, then
		// create and switch to new list

		SortedListModel oldList = mappedList;
		setMood( newListName );

		mappedList.addAll( oldList );
		displayList.addAll( oldList );
	}

	/**
	 * Executes all the mood displayList for the current mood.
	 */

	public static void execute()
	{	execute( false );
	}

	public static void burnExtraMana( boolean isManualInvocation )
	{
		if ( !isManualInvocation && KoLCharacter.getCurrentMP() < KoLCharacter.getMaximumMP() )
			return;

		String nextBurnCast;

		isExecuting = true;

		while ( (nextBurnCast = getNextBurnCast( isManualInvocation )) != null )
			DEFAULT_SHELL.executeLine( nextBurnCast );

		isExecuting = false;
	}

	public static String getNextBurnCast( boolean isManualInvocation )
	{
		// Rather than keeping a safety for the player, let the player
		// make the mistake of burning below their auto-restore threshold.

		int starting = (int) (StaticEntity.getFloatProperty( "mpThreshold" ) * (float) KoLCharacter.getMaximumMP());
		if ( starting < 0 && !isManualInvocation )
			return null;

		int minimum = Math.max( 0, (int) (StaticEntity.getFloatProperty( "mpAutoRecovery" ) * (float) KoLCharacter.getMaximumMP()) );
		minimum = Math.max( minimum, starting );

		String skillName = null;
		int desiredDuration = 0;

		// Rather than maintain mood-related buffs only, maintain
		// any active effect that the character can auto-cast.  This
		// makes the feature useful even for people who have never
		// defined a mood.

		AdventureResult currentEffect;
		AdventureResult nextEffect;

		for ( int i = 0; i < activeEffects.size() && KoLmafia.permitsContinue(); ++i )
		{
			currentEffect = (AdventureResult) activeEffects.get(i);
			nextEffect = i + 1 >= activeEffects.size() ? null : (AdventureResult) activeEffects.get( i + 1 );

			if ( currentEffect.getCount() >= KoLCharacter.getAdventuresLeft() + 200 )
				return null;

			skillName = UneffectRequest.effectToSkill( currentEffect.getName() );
			if ( !ClassSkillsDatabase.contains( skillName ) || !KoLCharacter.hasSkill( skillName ) )
				continue;

			// If the player only wishes to cast buffs related to their
			// mood, then skip the buff if it's not in the player's moods.

			if ( !StaticEntity.getBooleanProperty( "allowNonMoodBurning" ) )
			{
				boolean shouldIgnore = true;

				for ( int j = 0; j < displayList.size(); ++j )
					shouldIgnore &= !currentEffect.equals( ((MoodTrigger)displayList.get(j)).effect );

				if ( shouldIgnore )
					continue;
			}

			// Only cast if a matching skill was found.  Limit cast count
			// to two in order to ensure that KoLmafia doesn't make the
			// buff counts too far out of balance.

			if ( nextEffect != null )
				desiredDuration = nextEffect.getCount() - currentEffect.getCount();

			int skillId = ClassSkillsDatabase.getSkillId( skillName );

			if ( !StaticEntity.getBooleanProperty( "allowEncounterRateBurning" ) )
				if ( skillId == 1019 || skillId == 5017 || skillId == 6015 || skillId == 6016 )
					continue;

			int castCount = (KoLCharacter.getCurrentMP() - minimum) / ClassSkillsDatabase.getMPConsumptionById( skillId );
			int duration = ClassSkillsDatabase.getEffectDuration( skillId );

			// If the player opts in to allowing breakfast casting to burn
			// off excess MP, rather than using auto-restore, do so.

			if ( currentEffect.getCount() >= 10 )
			{
				String breakfast = executeBreakfastBurning( minimum );
				if ( breakfast != null )
					return breakfast;

				castCount = (KoLCharacter.getCurrentMP() - minimum) / ClassSkillsDatabase.getMPConsumptionById( skillId );
			}

			if ( castCount > 2 && duration > desiredDuration )
				castCount = 2;
			else if ( duration * castCount > desiredDuration )
				castCount = Math.min( 3, castCount );

			if ( castCount > 0 )
				return "cast " + castCount + " " + skillName;
			else
				return executeBreakfastBurning( minimum );
		}

		return executeBreakfastBurning( minimum );
	}

	private static String executeBreakfastBurning( int minimum )
	{
		if ( !StaticEntity.getBooleanProperty( "allowBreakfastBurning" ) )
			return null;

		if ( !StaticEntity.getClient().castBreakfastSkills( true, false ) )
			return null;

		// Cast 'Summon Candy Hearts' if available and your current
		// turn count on your buffs is greater than 10.

		if ( !KoLCharacter.hasSkill( "Summon Candy Hearts" ) )
			return null;

		if ( ClassSkillsDatabase.getMPConsumptionById( 18 ) <= KoLCharacter.getCurrentMP() - minimum )
			return "cast 1 summon candy hearts";

		return null;
	}

	public static void execute( boolean isManualInvocation )
	{
		if ( KoLmafia.refusesContinue() )
			return;

		if ( !willExecute( isManualInvocation ) )
		{
			burnExtraMana( isManualInvocation );
			return;
		}

		if ( StaticEntity.getProperty( "currentMood" ).equals( "apathetic" ) )
			return;

		isExecuting = true;

		AdventureResult initialWeapon = KoLCharacter.getEquipment( KoLCharacter.WEAPON );
		AdventureResult initialOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );

		MoodTrigger current = null;

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		thiefTriggerLimit = KoLCharacter.hasEquipped( PENDANT ) ? 4 : 3;

		// If you have too many accordion thief buffs to execute
		// your displayList, then shrug off your extra buffs, but
		// only if the user allows for this.

		// First we determine which buffs are already affecting the
		// character in question.

		ArrayList thiefBuffs = new ArrayList();
		for ( int i = 0; i < effects.length; ++i )
		{
			String skillName = UneffectRequest.effectToSkill( effects[i].getName() );
			if ( ClassSkillsDatabase.contains( skillName ) )
			{
				int skillId = ClassSkillsDatabase.getSkillId( skillName );
				if ( skillId > 6000 && skillId < 7000 )
					thiefBuffs.add( effects[i] );
			}
		}

		// Then, we determine the displayList which are thief skills, and
		// thereby would be cast at this time.

		ArrayList thiefSkills = new ArrayList();
		for ( int i = 0; i < displayList.size(); ++i )
		{
			current = (MoodTrigger) displayList.get(i);
			if ( current.isThiefTrigger() )
				thiefSkills.add( current.effect );
		}

		// We then remove the displayList which will be used from the pool of
		// effects which could be removed.  Then we compute how many we
		// need to remove and remove them.

		thiefBuffs.removeAll( thiefSkills );
		int buffsToRemove = thiefBuffs.size() + thiefSkills.size() - thiefTriggerLimit;

		for ( int i = 0; i < buffsToRemove; ++i )
			DEFAULT_SHELL.executeLine( "uneffect " + ((AdventureResult)thiefBuffs.get(i)).getName() );

		// Now that everything is prepared, go ahead and execute
		// the displayList which have been set.  First, start out
		// with any skill casting.

		for ( int i = 0; !KoLmafia.refusesContinue() && i < displayList.size(); ++i )
		{
			current = (MoodTrigger) displayList.get(i);
			if ( current.skillId != -1 )
				current.execute( isManualInvocation );
		}

		for ( int i = 0; i < displayList.size(); ++i )
		{
			current = (MoodTrigger) displayList.get(i);
			if ( current.skillId == -1 )
				current.execute( isManualInvocation );
		}

		isExecuting = false;

		if ( !isManualInvocation )
			burnExtraMana( false );

		if ( songWeapon != null )
			UseSkillRequest.untinkerCloverWeapon( UseSkillRequest.ROCKNROLL_LEGEND );
	}

	public static boolean willExecute( boolean isManualInvocation )
	{
		if ( displayList.isEmpty() )
			return false;

		boolean willExecute = false;

		for ( int i = 0; i < displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) displayList.get(i);
			willExecute |= current.shouldExecute( isManualInvocation );
		}

		return willExecute;
	}

	public static ArrayList getMissingEffects()
	{
		ArrayList missing = new ArrayList();
		if ( displayList.isEmpty() )
			return missing;

		for ( int i = 0; i < displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) displayList.get(i);
			if ( current.getType().equals( "lose_effect" ) && !activeEffects.contains( current.effect ) )
				missing.add( current.effect );
		}

		return missing;
	}

	public static int getMaintenanceCost()
	{
		if ( displayList.isEmpty() )
			return 0;

		int runningTally = 0;

		// Iterate over the entire list of applicable triggers,
		// locate the ones which involve spellcasting, and add
		// the MP cost for maintenance to the running tally.

		for ( int i = 0; i < displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) displayList.get(i);
			if ( !current.getType().equals( "lose_effect" ) || !current.shouldExecute( true ) )
				continue;

			String action = current.getAction();
			if ( !action.startsWith( "cast" ) && !action.startsWith( "buff" ) )
				continue;

			int spaceIndex = action.indexOf( " " );
			if ( spaceIndex == -1 )
				continue;

			action = action.substring( spaceIndex + 1 );

			int multiplier = 1;

			if ( Character.isDigit( action.charAt(0) ) )
			{
				spaceIndex = action.indexOf( " " );
				multiplier = StaticEntity.parseInt( action.substring( 0, spaceIndex ) );
				action = action.substring( spaceIndex + 1 );
			}

			String skillName = KoLmafiaCLI.getSkillName( action );
			if ( skillName != null )
				runningTally += ClassSkillsDatabase.getMPConsumptionById( ClassSkillsDatabase.getSkillId( skillName ) ) * multiplier;
		}

		// Running tally calculated, return the amount of
		// MP required to sustain this mood.

		return runningTally;
	}

	/**
	 * Stores the settings maintained in this <code>MoodSettings</code>
	 * object to disk for later retrieval.
	 */

	public static void saveSettings()
	{
		PrintStream writer = LogStream.openStream( settingsFile, true );

		SortedListModel triggerList;
		for ( int i = 0; i < availableMoods.size(); ++i )
		{
			triggerList = (SortedListModel) reference.get( availableMoods.get(i) );
			writer.println( "[ " + availableMoods.get(i) + " ]" );

			for ( int j = 0; j < triggerList.size(); ++j )
				writer.println( ((MoodTrigger)triggerList.get(j)).toSetting() );

			writer.println();
		}

		writer.close();
	}

	/**
	 * Loads the settings located in the given file into this object.
	 * Note that all settings are overridden; if the given file does
	 * not exist, the current global settings will also be rewritten
	 * into the appropriate file.
	 */

	public static void loadSettings()
	{
		reference.clear();
		availableMoods.clear();

		ensureProperty( "default" );
		ensureProperty( "apathetic" );

		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !settingsFile.exists() )
			{
				settingsFile.createNewFile();
				return;
			}

			BufferedReader reader = KoLDatabase.getReader( settingsFile );

			String line;
			String currentKey = "";

			while ( (line = reader.readLine()) != null )
			{
				line = line.trim();
				if ( line.startsWith( "[" ) )
				{
					currentKey = line.substring( 1, line.length() - 1 ).trim().toLowerCase();
					displayList.clear();

					if ( reference.containsKey( currentKey ) )
					{
						mappedList = (SortedListModel) reference.get( currentKey );
					}
					else
					{
						mappedList = new SortedListModel();
						reference.put( currentKey, mappedList );
						availableMoods.add( currentKey );
					}
				}
				else if ( line.length() != 0 )
				{
					addTrigger( MoodTrigger.constructNode( line ) );
				}
			}

			displayList.clear();

			reader.close();
			reader = null;

			setMood( StaticEntity.getProperty( "currentMood" ) );
		}
		catch ( IOException e1 )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e1 );
		}
		catch ( Exception e2 )
		{
			// Somehow, the settings were corrupted; this
			// means that they will have to be created after
			// the current file is deleted.

			StaticEntity.printStackTrace( e2 );
			settingsFile.delete();
			loadSettings();
		}
	}

	public static String getDefaultAction( String type, String name )
	{
		if ( type == null || name == null )
			return "";

		// We can look at the displayList list to see if it matches
		// your current mood.  That way, the "default action" is
		// considered whatever your current mood says it is.

		for ( int i = 0; i < displayList.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) displayList.get(i);
			if ( current.getType().equals( type ) && current.name.equals( name ) )
				return current.action;
		}

		if ( type.equals( "unconditional" ) )
		{
		}
		else if ( type.equals( "gain_effect" ) )
		{
			if ( name.indexOf( "Poisoned" ) != -1 )
				return "use anti-anti-antidote";

			if ( name.equals( "Beaten Up" ) )
			{
				if ( KoLCharacter.hasSkill( "Tongue of the Walrus" ) )
					return "cast Tongue of the Walrus";
				if ( KoLCharacter.hasSkill( "Tongue of the Otter" ) )
					return "cast Tongue of the Otter";
				if ( KoLCharacter.hasItem( UneffectRequest.FOREST_TEARS ) )
					return "use forest tears";
				if ( KoLCharacter.hasItem( UneffectRequest.REMEDY ) )
					return "uneffect beaten up";
				if ( KoLCharacter.hasItem( UneffectRequest.TINY_HOUSE ) || KoLCharacter.canInteract() )
					return "use tiny house";

				return "";
			}

			return "uneffect " + name;
		}
		else if ( type.equals( "lose_effect" ) )
		{
			int tenCount = KoLCharacter.canInteract() ? 6 : 1;

			if ( name.equals( "Butt-Rock Hair" ) )
				return "use 5 can of hair spray";

			if ( name.equals( "Ticking Clock" ) )
				return "use " + tenCount + " cheap wind-up clock";

			if ( name.equals( "Temporary Lycanthropy" ) )
				return "use " + tenCount + " blood of the Wereseal";

			if ( name.equals( "Half-Astral" ) )
				return "use 1 astral mushroom";

			// Tongues require snowcones

			if ( name.endsWith( "Tongue" ) )
				return "use 1 " + name.substring( 0, name.indexOf( " " ) ).toLowerCase() + " snowcone";

			// Cupcake effects require cupcakes

			if ( name.equals( "Cupcake of Choice" ) )
				return "use 1 blue-frosted astral cupcake";
			if ( name.equals( "The Cupcake of Wrath" ) )
				return "use 1 green-frosted astral cupcake";
			if ( name.equals( "Shiny Happy Cupcake" ) )
				return "use 1 orange-frosted astral cupcake";
			if ( name.equals( "Your Cupcake Senses Are Tingling" ) )
				return "use 1 pink-frosted astral cupcake";
			if ( name.equals( "Tiny Bubbles in the Cupcake" ) )
				return "use 1 purple-frosted astral cupcake";

			// Laboratory effects

			if ( name.equals( "Wasabi Sinuses" ) )
				return "use " + tenCount + " Knob Goblin nasal spray";

			if ( name.equals( "Peeled Eyeballs" ) )
				return "use " + tenCount + " Knob Goblin eyedrops";

			if ( name.equals( "Sharp Weapon" ) )
				return "use " + tenCount + " Knob Goblin sharpening spray";

			if ( name.equals( "Heavy Petting" ) )
				return "use " + tenCount + " Knob Goblin pet-buffing spray";

			if ( name.equals( "Big Veiny Brain" ) )
				return "use " + tenCount + " Knob Goblin learning pill";

			// Finally, fall back on skills

			String skillName = UneffectRequest.effectToSkill( name );

			if ( KoLCharacter.hasSkill( skillName ) )
			{
				int skillId = ClassSkillsDatabase.getSkillId( skillName );

				int duration = ClassSkillsDatabase.getEffectDuration( skillId );
				if ( duration == 0 )
					return "";

				int castCount = KoLCharacter.canInteract() ? 10 : 1;
				int mpCost = ClassSkillsDatabase.getMPConsumptionById( skillId );
				if ( mpCost > 0 )
					castCount = Math.min( castCount, KoLCharacter.getMaximumMP() / mpCost );

				return "cast " + castCount + " " + skillName;
			}
		}

		return "";
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private static void ensureProperty( String key )
	{
		if ( !reference.containsKey( key ) )
		{
			SortedListModel defaultList = new SortedListModel();
			reference.put( key, defaultList );
			availableMoods.add( key );
		}
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 *
	 * @param	settingsFile	The file to which the settings will be stored.
	 */

	public static class MoodTrigger implements Comparable
	{
		private int skillId = -1;
		private AdventureResult effect;
		private boolean isThiefTrigger = false;

		private StringBuffer stringForm;

		private String type, name;
		private String action, command, count, parameters;

		public MoodTrigger( String type, AdventureResult effect, String action )
		{
			this.type = type;
			this.effect = effect;
			this.name = effect == null ? null : effect.getName();

			if ( action.startsWith( "use " ) || action.startsWith( "cast " ) )
			{
				// Determine the command, the count amount,
				// and the parameter's unambiguous form.

				int spaceIndex = action.indexOf( " " );

				this.command = action.substring( 0, spaceIndex );
				this.parameters = action.substring( spaceIndex + 1 ).trim();

				if ( this.command.equals( "use" ) )
				{
					AdventureResult item = KoLmafiaCLI.getFirstMatchingItem( this.parameters );

					this.count = String.valueOf( item.getCount() );
					this.parameters = item.getName();
				}
				else
				{
					this.count = "1";

					if ( Character.isDigit( this.parameters.charAt(0) ) )
					{
						spaceIndex = this.parameters.indexOf( " " );
						this.count = this.parameters.substring( 0, spaceIndex );

						this.parameters = this.parameters.substring( spaceIndex ).trim();

						if ( !ClassSkillsDatabase.contains( this.parameters ) )
							this.parameters = KoLmafiaCLI.getSkillName( this.parameters );
					}
				}

				this.action = command + " " + count + " " + parameters;
			}
			else
			{
				this.command = action;
				this.count = "";
				this.parameters = "";

				this.action = action;
			}

			if ( type != null && type.equals( "lose_effect" ) && effect != null )
			{
				String skillName = UneffectRequest.effectToSkill( effect.getName() );
				if ( ClassSkillsDatabase.contains( skillName ) )
				{
					skillId = ClassSkillsDatabase.getSkillId( skillName );
					isThiefTrigger = skillId > 6000 && skillId < 7000;
				}
			}

			this.stringForm = new StringBuffer();
			updateStringForm();
		}

		public String getType()
		{	return type;
		}

		public String getName()
		{	return name;
		}

		public String getAction()
		{	return action;
		}

		public String getCommand()
		{	return command;
		}

		public String toString()
		{	return stringForm.toString();
		}

		public String toSetting()
		{	return effect == null ? type + " => " + action : type + " " + name + " => " + action;
		}

		public boolean equals( Object o )
		{
			if ( o == null || !(o instanceof MoodTrigger) )
				return false;

			MoodTrigger mt = (MoodTrigger) o;
			if ( !type.equals( mt.getType() ) )
				return false;

			if ( name == null )
				return mt.name == null;

			if ( mt.getType() == null )
				return false;

			return name.equals( mt.name );
		}

		public void execute( boolean isManualInvocation )
		{
			if ( shouldExecute( isManualInvocation ) )
			{
				if ( isThiefTrigger() )
				{
					songWeapon = UseSkillRequest.optimizeEquipment( skillId );

					if ( songWeapon == null )
						return;

					if ( songWeapon == UseSkillRequest.ACCORDION || songWeapon == UseSkillRequest.ROCKNROLL_LEGEND )
						songWeapon = null;
				}

				if ( type.equals( "lose_effect" ) && KoLCharacter.canInteract() && (action.startsWith( "use 1" ) || action.startsWith( "cast 1" )) )
					DEFAULT_SHELL.executeLine( getDefaultAction( "lose_effect", effect.getName() ) );
				else
					DEFAULT_SHELL.executeLine( action );
			}
		}

		public boolean shouldExecute( boolean isManualInvocation )
		{
			if ( KoLmafia.refusesContinue() )
				return false;

			boolean shouldExecute = false;

			if ( effect == null )
			{
				shouldExecute = true;
			}
			else if ( type.equals( "gain_effect" ) )
			{
				KoLmafia.applyEffects();
				shouldExecute = activeEffects.contains( effect );
			}
			else if ( type.equals( "lose_effect" ) )
			{
				shouldExecute = action.indexOf( "cupcake" ) != -1 || action.indexOf( "snowcone" ) != -1 || action.indexOf( "mushroom" ) != -1 ?
					!activeEffects.contains( effect ) : effect.getCount( activeEffects ) <= (isManualInvocation ? 5 : 1);

				shouldExecute &= !name.equals( "Temporary Lycanthropy" ) || MoonPhaseDatabase.getMoonlight() > 4;
			}

			return shouldExecute;
		}

		public boolean isThiefTrigger()
		{	return isThiefTrigger;
		}

		public int compareTo( Object o )
		{
			if ( o == null || !(o instanceof MoodTrigger) )
				return -1;

			String othertype = ((MoodTrigger)o).getType();
			String othername = ((MoodTrigger)o).name;
			String otherTriggerAction = ((MoodTrigger)o).action;

			int compareResult = 0;

			if ( type.equals( "unconditional" ) )
			{
				if ( othertype.equals( "unconditional" ) )
					compareResult = action.compareToIgnoreCase( otherTriggerAction );
				else
					compareResult = -1;
			}
			else if ( type.equals( "gain_effect" ) )
			{
				if ( othertype.equals( "unconditional" ) )
					compareResult = 1;
				else if ( othertype.equals( "gain_effect" ) )
					compareResult = name.compareToIgnoreCase( othername );
				else
					compareResult = -1;
			}
			else if ( type.equals( "lose_effect" ) )
			{
				if ( othertype.equals( "lose_effect" ) )
					compareResult = name.compareToIgnoreCase( othername );
				else
					compareResult = 1;
			}

			return compareResult;
		}

		public void updateStringForm()
		{
			stringForm.setLength(0);

			if ( type.equals( "gain_effect" ) )
				stringForm.append( "When I get" );
			else if ( type.equals( "lose_effect" ) )
				stringForm.append( "When I run low on" );
			else
				stringForm.append( "Always" );

			if ( name != null )
			{
				stringForm.append( " " );
				stringForm.append( name );
			}

			if ( type.equals( "lose_effect" ) && name != null && name.equals( "Temporary Lycanthropy" ) )
				stringForm.append( " and there's enough moonlight" );

			stringForm.append( ", " );
			stringForm.append( action );
		}

		public static MoodTrigger constructNode( String line )
		{
			String [] pieces = line.split( " => " );
			if ( pieces.length != 2 )
				return null;

			String type = null;

			if ( pieces[0].startsWith( "gain_effect" ) )
				type = "gain_effect";
			else if ( pieces[0].startsWith( "lose_effect" ) )
				type = "lose_effect";
			else if ( pieces[0].startsWith( "unconditional" ) )
				type = "unconditional";

			if ( type == null )
				return null;

			String name = type.equals( "unconditional" ) ? null :
				pieces[0].substring( pieces[0].indexOf( " " ) ).trim();

			AdventureResult effect = null;
			if ( !type.equals( "unconditional" ) )
			{
				effect = KoLmafiaCLI.getFirstMatchingEffect( name );
				if ( effect == null )
					return null;
			}

			return new MoodTrigger( type, effect, pieces[1].trim() );
		}
	}
}
