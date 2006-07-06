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
	private static File settingsFile = null;
	private static String characterName = "";
	private static TreeMap reference = new TreeMap();

	private static AdventureResult songWeapon = null;
	public static boolean hasChangedOutfit = false;

	private static ArrayList thiefTriggers = new ArrayList();
	private static SortedListModel triggers = new SortedListModel();
	private static SortedListModel availableMoods = new SortedListModel();

	static { MoodSettings.reset(); }

	public synchronized static final String settingsFileName()
	{	return "~" + KoLCharacter.baseUserName() + ".kms";
	}

	public synchronized static final void reset()
	{
		reference.clear();
		triggers.clear();
		availableMoods.clear();

		MoodSettings.characterName = KoLCharacter.getUsername();
		MoodSettings.settingsFile = new File( DATA_DIRECTORY + settingsFileName() );

		loadSettings();
	}

	public static SortedListModel getAvailableMoods()
	{
		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			MoodSettings.reset();

		return availableMoods;
	}

	/**
	 * Sets the current mood to be executed to the given
	 * mood.  Also ensures that all defaults are loaded
	 * for the given mood if no data exists.
	 */

	public static SortedListModel setMood( String mood )
	{
		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			MoodSettings.reset();

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
		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			MoodSettings.reset();

		if ( triggers.contains( node ) )
			return;

		// Check to make sure that there are fewer than three thief
		// triggers if this is a thief trigger.

		if ( node.isThiefTrigger() )
		{
			int thiefTriggerCount = 0;
			for ( int i = 0; i < triggers.size(); ++i )
				if ( ((MoodTrigger)triggers.get(i)).isThiefTrigger() )
					++thiefTriggerCount;

			if ( thiefTriggerCount == 3 )
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
		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			MoodSettings.reset();

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
		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			MoodSettings.reset();

		UseSkillRequest [] skills = new UseSkillRequest[ KoLCharacter.getAvailableSkills().size() ];
		KoLCharacter.getAvailableSkills().toArray( skills );

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
			{
				if ( skills[i].getSkillID() % 1000 == 0 )
					addTrigger( "lose_effect", effectName, "cast 3 " + skills[i].getSkillName() );
				else
					addTrigger( "lose_effect", effectName, "cast " + skills[i].getSkillName() );
			}
		}

		if ( !thiefSkills.isEmpty() && thiefSkills.size() < 4 )
		{
			String [] skillNames = new String[ thiefSkills.size() ];
			thiefSkills.toArray( skillNames );

			for ( int i = 0; i < skillNames.length; ++i )
				addTrigger( "lose_effect", UneffectRequest.skillToEffect( skillNames[i] ), "cast " + skillNames[i] );
		}

		addTrigger( "lose_effect", "Butt-Rock Hair", "use 5 can of hair spray" );

		// Beaten-up removal, as a demo of how to handle beaten-up
		// and poisoned statuses.

		addTrigger( "gain_effect", "Poisoned", "use anti-anti-antidote" );

		if ( KoLCharacter.hasSkill( "Tongue of the Otter" ) )
			addTrigger( "gain_effect", "Beaten Up", "cast Tongue of the Otter" );
		else if ( KoLCharacter.hasSkill( "Tongue of the Walrus" ) )
			addTrigger( "gain_effect", "Beaten Up", "cast Tongue of the Walrus" );

		// If there's any effects the player currently has and there
		// is a known way to re-acquire it (internally known, anyway),
		// make sure to add those as well.

		AdventureResult [] effects = new AdventureResult[ KoLCharacter.getEffects().size() ];
		KoLCharacter.getEffects().toArray( effects );

		for ( int i = 0; i < effects.length; ++i )
		{
			String action = getDefaultAction( "lose_effect", effects[i].getName() );
			if ( action != null && !action.equals( "" ) && !action.startsWith( "cast" ) )
				addTrigger( "lose_effect", effects[i].getName(), action );
		}
	}

	/**
	 * Executes all the mood triggers for the current mood.
	 */

	public static void execute()
	{
		String initialWeapon = KoLCharacter.getEquipment( KoLCharacter.WEAPON );
		String initialOffhand = KoLCharacter.getEquipment( KoLCharacter.OFFHAND );
		String initialHat = KoLCharacter.getEquipment( KoLCharacter.HAT );

		hasChangedOutfit = false;

		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			MoodSettings.reset();

		MoodTrigger current = null;

		AdventureResult [] effects = new AdventureResult[ KoLCharacter.getEffects().size() ];
		KoLCharacter.getEffects().toArray( effects );

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

		// If you have too many accordion thief buffs to execute
		// your triggers, then shrug off your extra buffs.

		for ( int i = 0; i < triggers.size(); ++i )
		{
			current = (MoodTrigger) triggers.get(i);
			if ( current.isThiefTrigger() )
			{
				if ( thiefBuffs.contains( current.effect ) )
					continue;

				if ( thiefBuffs.size() == 3 )
					DEFAULT_SHELL.executeLine( "uneffect " + ((AdventureResult)thiefBuffs.remove(0)).getName() );

				thiefBuffs.add( current.effect );
			}
		}

		// Now that everything is prepared, go ahead and execute
		// the triggers which have been set.

		for ( int i = 0; i < triggers.size(); ++i )
			((MoodTrigger)triggers.get(i)).execute();

		if ( hasChangedOutfit )
			SpecialOutfit.restoreCheckpoint();

		UseSkillRequest.restoreEquipment( null, null, null, null );
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * object to disk for later retrieval.
	 */

	public synchronized static void saveSettings()
	{
		try
		{
			PrintStream writer = new PrintStream( new FileOutputStream( settingsFile ) );

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

			BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( settingsFile ) ) );

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
		if ( type == null || name == null || !type.equals( "lose_effect" ) )
			return "";

		if ( name.equals( "Butt-Rock Hair" ) )
			return "use 5 can of hair spray";

		// Tongues require snowcones

		if ( name.endsWith( "Tongue" ) )
			return "use " + name.substring( 0, name.indexOf( " " ) ).toLowerCase() + " snowcone";

		// Laboratory effects

		if ( name.equals( "Wasabi Sinuses" ) )
			return "use Knob Goblin nasal spray";

		if ( name.equals( "Peeled Eyeballs" ) )
			return "use Knob Goblin eyedrops";

		if ( name.equals( "Sharp Weapon" ) )
			return "use Knob Goblin sharpening spray";

		if ( name.equals( "Heavy Petting" ) )
			return "use Knob Goblin pet-buffing spray";

		if ( name.equals( "Big Veiny Brain" ) )
			return "use Knob Goblin learning pill";

		// Finally, fall back on skills

		String skillName = UneffectRequest.effectToSkill( name );
		if ( KoLCharacter.hasSkill( skillName ) )
			return ClassSkillsDatabase.getSkillID( skillName ) == 3 ? "" : "cast " + skillName;

		return "";
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private synchronized static void ensureProperty( String key )
	{
		if ( !reference.containsKey( key ) )
		{
			SortedListModel defaultList = new SortedListModel();
			defaultList.addAll( triggers );

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
			return triggerType.equals( mt.triggerType ) && triggerName.equals( mt.triggerName );
		}

		public void execute()
		{
			boolean shouldExecute = false;

			if ( effect == null )
				shouldExecute = true;
			else if ( triggerType.equals( "gain_effect" ) )
				shouldExecute = KoLCharacter.getEffects().contains( effect );
			else if ( triggerType.equals( "lose_effect" ) )
				shouldExecute = !KoLCharacter.getEffects().contains( effect );

			if ( shouldExecute )
			{
				if ( isThiefTrigger() && songWeapon == null )
					UseSkillRequest.optimizeEquipment( skillID );

				DEFAULT_SHELL.executeLine( action );
			}
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

			stringForm.append( ", " );
			stringForm.append( pieces[1] );

			return new MoodTrigger( stringForm.toString(), type, name, pieces[1] );
		}
	}
}
