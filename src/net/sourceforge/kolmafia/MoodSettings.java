/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStreamReader;

import java.util.TreeMap;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.SortedListModel;

/**
 * An extension of {@link java.util.Properties} which handles all the
 * user settings of <code>KoLmafia</code>.  In order to maintain issues
 * involving compatibility (J2SE 1.4 does not support XML output directly),
 * all data is written using {@link java.util.Properties#store(OutputStream,String)}.
 * Files are named according to the following convention: a tilde (<code>~</code>)
 * preceeds the name of the character whose settings this object represents,
 * with the 'kcs' extension (KoLmafia Character Settings).  All global settings
 * are stored in <code>~.kcs</code>.
 */

public abstract class MoodSettings implements KoLConstants
{
	static
	{
		// Renaming data files to make then easier to find for most
		// people (so they aren't afraid to open them).

		StaticEntity.renameDataFiles( "kms", "moods" );
	}

	private static int thiefTriggerLimit = 3;
	private static final AdventureResult PENDANT = new AdventureResult( 1235, 1 );

	private static File settingsFile = null;
	private static TreeMap reference = new TreeMap();

	private static AdventureResult songWeapon = null;
	public static boolean hasChangedOutfit = false;

	private static ArrayList thiefTriggers = new ArrayList();
	private static SortedListModel triggers = new SortedListModel();
	private static SortedListModel availableMoods = new SortedListModel();

	static { MoodSettings.reset(); }

	public synchronized static final String settingsFileName()
	{	return DATA_DIRECTORY + "settings/moods_" + KoLCharacter.baseUserName() + ".txt";
	}

	public synchronized static final void reset()
	{
		reference.clear();
		triggers.clear();
		availableMoods.clear();

		MoodSettings.settingsFile = new File( settingsFileName() );
		loadSettings();
		ensureProperty( "default" );
		ensureProperty( "apathetic" );
	}

	public static SortedListModel getAvailableMoods()
	{	return availableMoods;
	}

	/**
	 * Sets the current mood to be executed to the given
	 * mood.  Also ensures that all defaults are loaded
	 * for the given mood if no data exists.
	 */

	public static SortedListModel setMood( String mood )
	{
		mood = mood == null || mood.trim().equals( "" ) ? "default" : mood.toLowerCase().trim();
		ensureProperty( mood );

		StaticEntity.setProperty( "currentMood", mood );
		triggers = (SortedListModel) reference.get( mood );

		return triggers;
	}

	/**
	 * Retrieves the model associated with the given mood.
	 */

	public static SortedListModel getTriggers()
	{	return triggers;
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

		if ( StaticEntity.getProperty( "currentMood" ).equals( "apathetic" ) || triggers.contains( node ) )
			return;

		// Check to make sure that there are fewer than three thief
		// triggers if this is a thief trigger.

		thiefTriggerLimit = KoLCharacter.hasEquipped( PENDANT ) ? 4 : 3;

		if ( node.isThiefTrigger() )
		{
			int thiefTriggerCount = 0;
			for ( int i = 0; i < triggers.size(); ++i )
				if ( ((MoodTrigger)triggers.get(i)).isThiefTrigger() )
					++thiefTriggerCount;

			if ( thiefTriggerCount >= thiefTriggerLimit )
				return;
		}

		triggers.add( node );
		if ( node.isThiefTrigger() )
			thiefTriggers.add( node );

		saveSettings();
	}

	/**
	 * Removes all the current triggers.
	 */

	public static void removeTriggers( Object [] triggers )
	{
		for ( int i = 0; i < triggers.length; ++i )
		{
			MoodSettings.triggers.remove( triggers[i] );
			if ( thiefTriggers.contains( triggers[i] ) )
				thiefTriggers.remove( triggers[i] );
		}

		saveSettings();
	}

	/**
	 * Fills up the trigger list automatically.
	 */

	public static void autoFillTriggers()
	{
		if ( StaticEntity.getProperty( "currentMood" ).equals( "apathetic" ) )
			return;

		UseSkillRequest [] skills = new UseSkillRequest[ availableSkills.size() ];
		availableSkills.toArray( skills );

		ArrayList thiefSkills = new ArrayList();

		for ( int i = 0; i < skills.length; ++i )
		{
			if ( skills[i].getSkillID() < 1000 )
				continue;

			if ( skills[i].getSkillID() > 6000 && skills[i].getSkillID() < 7000 )
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
					"Aloysius' Antiphon of Aptitude", "Ur-Kel's Aria of Annoyance",
					"Fat Leon's Phat Loot Lyric", "Cletus's Canticle of Celerity",
					"The Psalm of Pointiness", "The Moxious Madrigal"
				};
			}
			else
			{
				rankedBuffs = new String [] {
					"Fat Leon's Phat Loot Lyric", "Aloysius' Antiphon of Aptitude",
					"Ur-Kel's Aria of Annoyance", "The Sonata of Sneakiness",
					"Cletus's Canticle of Celerity", "Jackasses' Symphony of Destruction"
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

		if ( KoLCharacter.isHardcore() )
			addTrigger( "lose_effect", "Butt-Rock Hair", getDefaultAction( "lose_effect", "Butt-Rock Hair" ) );


		// Muscle class characters are rather special when it comes
		// to their default triggers.

		if ( NPCStoreDatabase.contains( "cheap wind-up clock" ) )
			addTrigger( "lose_effect", "Ticking Clock", getDefaultAction( "lose_effect", "Ticking Clock" ) );
		if ( NPCStoreDatabase.contains( "blood of the wereseal" ) )
			addTrigger( "lose_effect", "Temporary Lycanthropy", getDefaultAction( "lose_effect", "Temporary Lycanthropy" ) );

		// Beaten-up removal, as a demo of how to handle beaten-up
		// and poisoned statuses.

		addTrigger( "gain_effect", "Poisoned", getDefaultAction( "gain_effect", "Poisoned" ) );
		addTrigger( "gain_effect", "Beaten Up", getDefaultAction( "gain_effect", "Beaten Up" ) );


		// If there's any effects the player currently has and there
		// is a known way to re-acquire it (internally known, anyway),
		// make sure to add those as well.

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			String action = getDefaultAction( "lose_effect", effects[i].getName() );
			if ( action != null && !action.equals( "" ) && !action.startsWith( "cast" ) )
				addTrigger( "lose_effect", effects[i].getName(), action );
		}
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
			triggers.clear();
			saveSettings();
			return;
		}

		availableMoods.setSelectedItem( "apathetic" );
		setMood( "apathetic" );

		reference.remove( currentMood );
		availableMoods.remove( currentMood );

		saveSettings();
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

		// Copy triggers from current list, then
		// create and switch to new list

		SortedListModel oldTriggers = getTriggers();
		setMood( newListName );

		triggers.clear();
		triggers.addAll( oldTriggers );

		saveSettings();
	}
	/**
	 * Executes all the mood triggers for the current mood.
	 */

	public static void execute()
	{
		if ( KoLmafia.refusesContinue() || !willExecute() )
			return;

		AdventureResult initialWeapon = KoLCharacter.getEquipment( KoLCharacter.WEAPON );
		AdventureResult initialOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );
		AdventureResult initialHat = KoLCharacter.getEquipment( KoLCharacter.HAT );

		hasChangedOutfit = false;
		MoodTrigger current = null;

		AdventureResult [] effects = new AdventureResult[ activeEffects.size() ];
		activeEffects.toArray( effects );

		thiefTriggerLimit = KoLCharacter.hasEquipped( PENDANT ) ? 4 : 3;

		ArrayList thiefBuffs = new ArrayList();
		for ( int i = 0; i < effects.length; ++i )
		{
			String skillName = UneffectRequest.effectToSkill( effects[i].getName() );
			if ( ClassSkillsDatabase.contains( skillName ) )
			{
				int skillID = ClassSkillsDatabase.getSkillID( skillName );
				if ( skillID > 6000 && skillID < 7000 )
					thiefBuffs.add( effects[i] );
			}
		}

		ArrayList thiefSkills = new ArrayList();
		for ( int i = 0; i < triggers.size(); ++i )
		{
			current = (MoodTrigger) triggers.get(i);
			if ( current.isThiefTrigger() && !thiefBuffs.contains( current.effect ) )
				thiefSkills.add( current.effect );
		}

		// If you have too many accordion thief buffs to execute
		// your triggers, then shrug off your extra buffs.

		boolean allowThiefShrugOff = StaticEntity.getBooleanProperty( "allowThiefShrugOff" );
		boolean shouldExecuteThiefSkills = allowThiefShrugOff || thiefBuffs.size() + thiefSkills.size() <= thiefTriggerLimit;

		if ( allowThiefShrugOff && thiefBuffs.size() + thiefSkills.size() > thiefTriggerLimit )
		{
			for ( int i = 0; i < thiefBuffs.size() && thiefBuffs.size() + thiefSkills.size() > thiefTriggerLimit; ++i )
				if ( !thiefSkills.contains( thiefBuffs.get(i) ) )
					DEFAULT_SHELL.executeLine( "uneffect " + ((AdventureResult)thiefBuffs.remove(i--)).getName() );
		}

		// Now that everything is prepared, go ahead and execute
		// the triggers which have been set.  First, start out
		// with any skill casting.

		for ( int i = 0; i < triggers.size(); ++i )
		{
			current = (MoodTrigger) triggers.get(i);
			if ( current.skillID != -1 && (!current.isThiefTrigger() || shouldExecuteThiefSkills) )
				current.execute();
		}

		for ( int i = 0; i < triggers.size(); ++i )
		{
			current = (MoodTrigger) triggers.get(i);
			if ( current.skillID == -1 )
				current.execute();
		}

		if ( !hasChangedOutfit )
			UseSkillRequest.restoreEquipment( songWeapon, initialWeapon, initialOffhand, initialHat );
	}

	public static boolean willExecute()
	{
		if ( triggers.isEmpty() )
			return false;

		boolean willExecute = false;

		for ( int i = 0; i < triggers.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) triggers.get(i);
			willExecute |= current.shouldExecute();
		}

		return willExecute;
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * object to disk for later retrieval.
	 */

	public synchronized static void saveSettings()
	{
		try
		{
			PrintStream writer = new LogStream( settingsFile );

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
		catch ( IOException e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}
	}

	/**
	 * Loads the settings located in the given file into this object.
	 * Note that all settings are overridden; if the given file does
	 * not exist, the current global settings will also be rewritten
	 * into the appropriate file.
	 *
	 * @param	source	The file that contains (or will contain) the character data
	 */

	public synchronized static void loadSettings()
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !settingsFile.exists() )
			{
				settingsFile.getParentFile().mkdirs();
				settingsFile.createNewFile();

				setMood( "default" );
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
					triggers = new SortedListModel();

					reference.put( currentKey, triggers );
					availableMoods.add( currentKey );
				}
				else if ( line.length() != 0 )
				{
					addTrigger( MoodTrigger.constructNode( line ) );
				}
			}

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

		// We can look at the triggers list to see if it matches
		// your current mood.  That way, the "default action" is
		// considered whatever your current mood says it is.

		for ( int i = 0; i < triggers.size(); ++i )
		{
			MoodTrigger current = (MoodTrigger) triggers.get(i);
			if ( current.triggerType.equals( type ) && current.triggerName.equals( name ) )
				return current.action;
		}

		if ( type.equals( "unconditional" ) )
		{
		}
		else if ( type.equals( "gain_effect" ) )
		{
			if ( name.equals( "Poisoned" ) )
				return "use anti-anti-antidote";

			if ( name.equals( "Beaten Up" ) )
			{
				if ( KoLCharacter.hasSkill( "Tongue of the Otter" ) )
					return "cast Tongue of the Otter";
				if ( KoLCharacter.hasSkill( "Tongue of the Walrus" ) )
					return "cast Tongue of the Walrus";
				if ( KoLCharacter.hasItem( UneffectRequest.FOREST_TEARS, false ) )
					return "use forest tears";
				if ( KoLCharacter.hasItem( UneffectRequest.TINY_HOUSE, false ) )
					return "use tiny house";
				if ( KoLCharacter.hasItem( UneffectRequest.REMEDY, false ) || KoLCharacter.canInteract() )
					return "uneffect beaten up";

				return "use unguent; adventure 3 unlucky sewer";
			}

		}
		else if ( type.equals( "lose_effect" ) )
		{
			if ( name.equals( "Butt-Rock Hair" ) )
				return "use 5 can of hair spray";

			if ( name.equals( "Ticking Clock" ) )
				return "use 1 cheap wind-up clock";

			if ( name.equals( "Temporary Lycanthropy" ) )
				return "use 1 blood of the Wereseal";

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
				return "use 1 Knob Goblin nasal spray";

			if ( name.equals( "Peeled Eyeballs" ) )
				return "use 1 Knob Goblin eyedrops";

			if ( name.equals( "Sharp Weapon" ) )
				return "use 1 Knob Goblin sharpening spray";

			if ( name.equals( "Heavy Petting" ) )
				return "use 1 Knob Goblin pet-buffing spray";

			if ( name.equals( "Big Veiny Brain" ) )
				return "use 1 Knob Goblin learning pill";

			// Finally, fall back on skills

			String skillName = UneffectRequest.effectToSkill( name );
			if ( KoLCharacter.hasSkill( skillName ) )
			{
				int skillID = ClassSkillsDatabase.getSkillID( skillName );

				if ( skillID % 1000 == 0 || skillID == 1015 )
					return "cast 3 " + skillName;
				else if ( skillID != 3 )
					return "cast " + skillName;
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

			availableMoods.setSelectedItem( key );
			saveSettings();
		}
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 *
	 * @param	destination	The file to which the settings will be stored.
	 */

	private static class MoodTrigger implements Comparable
	{
		private int skillID = -1;
		private AdventureResult effect;
		private boolean isThiefTrigger = false;
		private String stringForm, triggerType, triggerName, action;

		public MoodTrigger( String stringForm, String triggerType, String triggerName, String action )
		{
			this.stringForm = stringForm;
			this.triggerType = triggerType;

			this.triggerName = triggerName;
			this.action = action;

			this.effect = triggerName == null ? null : new AdventureResult( triggerName, 1, true );

			if ( triggerType != null && triggerType.equals( "lose_effect" ) && effect != null )
			{
				String skillName = UneffectRequest.effectToSkill( effect.getName() );
				if ( ClassSkillsDatabase.contains( skillName ) )
				{
					skillID = ClassSkillsDatabase.getSkillID( skillName );
					isThiefTrigger = skillID > 6000 && skillID < 7000;
				}
			}
		}

		public String toString()
		{	return stringForm;
		}

		public String toSetting()
		{	return effect == null ? triggerType + " => " + action : triggerType + " " + triggerName + " => " + action;
		}

		public boolean equals( Object o )
		{
			if ( o == null || !(o instanceof MoodTrigger) )
				return false;

			MoodTrigger mt = (MoodTrigger) o;
			if ( !triggerType.equals( mt.triggerType ) )
				return false;

			if ( triggerName == null )
				return mt.triggerName == null;

			if ( mt.triggerType == null )
				return false;

			return triggerName.equals( mt.triggerName );
		}

		public void execute()
		{
			if ( shouldExecute() )
			{
				if ( skillID != -1 && songWeapon == null )
					songWeapon = UseSkillRequest.optimizeEquipment( skillID );

				if ( isThiefTrigger() && songWeapon == null )
					return;

				DEFAULT_SHELL.executeLine( action );
			}
		}

		public boolean shouldExecute()
		{
			if ( KoLmafia.refusesContinue() )
				return false;

			boolean shouldExecute = false;

			if ( effect == null )
			{
				shouldExecute = true;
			}
			else if ( triggerType.equals( "gain_effect" ) )
			{
				shouldExecute = activeEffects.contains( effect );
			}
			else if ( triggerType.equals( "lose_effect" ) )
			{
				shouldExecute = action.indexOf( "cupcake" ) != -1 || action.indexOf( "snowcone" ) != -1 ?
					!activeEffects.contains( effect ) : effect.getCount( activeEffects ) < 2;

				shouldExecute &= !triggerName.equals( "Temporary Lycanthropy" ) || MoonPhaseDatabase.getMoonlight() > 4;
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

			String otherTriggerType = ((MoodTrigger)o).triggerType;
			String otherTriggerName = ((MoodTrigger)o).triggerName;
			String otherTriggerAction = ((MoodTrigger)o).action;

			int compareResult = 0;

			if ( triggerType.equals( "unconditional" ) )
			{
				if ( otherTriggerType.equals( "unconditional" ) )
					compareResult = action.compareToIgnoreCase( otherTriggerAction );
				else
					compareResult = -1;
			}
			else if ( triggerType.equals( "gain_effect" ) )
			{
				if ( otherTriggerType.equals( "unconditional" ) )
				{
					compareResult = 1;
				}
				else if ( otherTriggerType.equals( "gain_effect" ) )
				{
					compareResult = triggerName.compareToIgnoreCase( otherTriggerName );
					if ( compareResult == 0 )
						compareResult = action.compareToIgnoreCase( otherTriggerAction );
				}
				else
				{
					compareResult = -1;
				}
			}
			else if ( triggerType.equals( "lose_effect" ) )
			{
				if ( otherTriggerType.equals( "lose_effect" ) )
				{
					compareResult = triggerName.compareToIgnoreCase( otherTriggerName );
					if ( compareResult == 0 )
						compareResult = action.compareToIgnoreCase( otherTriggerAction );
				}
				else
				{
					compareResult = 1;
				}
			}

			return compareResult;
		}

		public static MoodTrigger constructNode( String line )
		{
			String [] pieces = line.split( " => " );
			if ( pieces.length != 2 )
				return null;

			StringBuffer stringForm = new StringBuffer();
			String type = null;

			if ( pieces[0].startsWith( "gain_effect" ) )
			{
				type = "gain_effect";
				stringForm.append( "When I am affected by" );
			}
			else if ( pieces[0].startsWith( "lose_effect" ) )
			{
				type = "lose_effect";
				stringForm.append( "When I run out of" );
			}
			else if ( pieces[0].startsWith( "unconditional" ) )
			{
				type = "unconditional";
				stringForm.append( "No matter what" );
			}

			if ( type == null )
				return null;

			String name = type.equals( "unconditional" ) ? null :
				pieces[0].substring( pieces[0].indexOf( " " ) ).trim();

			if ( name != null )
			{
				stringForm.append( " " );
				stringForm.append( name );
			}

			if ( type.equals( "lose_effect" ) && name != null && name.equals( "Temporary Lycanthropy" ) )
				stringForm.append( " and there's enough moonlight" );

			stringForm.append( ", " );
			stringForm.append( pieces[1] );

			return new MoodTrigger( stringForm.toString(), type, name, pieces[1] );
		}
	}
}
