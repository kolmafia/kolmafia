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
import net.java.dev.spellcast.utilities.SortedListModel;
import net.java.dev.spellcast.utilities.LockableListModel;

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
	private static String [] keys;
	private static final File MOODS_FILE = new File( DATA_DIRECTORY + "moods.txt" );
	private static TreeMap reference = new TreeMap();

	private static String mood = "default";
	private static LockableListModel triggers = new LockableListModel();
	private static SortedListModel availableMoods = new SortedListModel();

	static { loadSettings(); }

	public static LockableListModel getAvailableMoods()
	{
		if ( availableMoods.isEmpty() )
			availableMoods.addAll( reference.keySet() );

		return availableMoods;
	}

	/**
	 * Sets the current mood to be executed to the given
	 * mood.  Also ensures that all defaults are loaded
	 * for the given mood if no data exists.
	 */

	public static LockableListModel setMood( String mood )
	{
		MoodSettings.mood = mood;

		ensureProperty( mood );
		StaticEntity.setProperty( "currentMood", mood );
		triggers = (LockableListModel) reference.get( mood );

		return triggers;
	}

	/**
	 * Retrieves the model associated with the given mood.
	 */

	public static LockableListModel getTriggers()
	{	return triggers;
	}

	/**
	 * Adds a trigger to the temporary mood settings.
	 */

	public static void addTrigger( String type, String name, String action )
	{
		triggers.add( MoodTrigger.constructNode( type + " " + name + " => " + action ) );
		saveSettings();
	}

	/**
	 * Removes all the current triggers.
	 */

	public static void removeTriggers( Object [] triggers )
	{
		for ( int i = 0; i < triggers.length; ++i )
			MoodSettings.triggers.remove( triggers[i] );
		saveSettings();
	}

	/**
	 * Fills up the trigger list automatically.
	 */

	public static void autoFillTriggers()
	{
		UseSkillRequest [] skills = new UseSkillRequest[ KoLCharacter.getAvailableSkills().size() ];
		KoLCharacter.getAvailableSkills().toArray( skills );

		for ( int i = 0; i < skills.length; ++i )
		{
			if ( skills[i].getSkillID() < 1000 )
				continue;

			String effectName = UneffectRequest.skillToEffect( skills[i].getSkillName() );
			if ( StatusEffectDatabase.contains( effectName ) )
				addTrigger( "lose_effect", effectName, "cast " + skills[i].getSkillName() );
		}

		if ( KoLCharacter.hasSkill( "Tongue of the Otter" ) )
			addTrigger( "gain_effect", "Beaten Up", "cast Tongue of the Otter" );
		else if ( KoLCharacter.hasSkill( "Tongue of the Walrus" ) )
			addTrigger( "gain_effect", "Beaten Up", "cast Tongue of the Walrus" );
	}

	/**
	 * Executes all the mood triggers for the current mood.
	 */

	public static void execute()
	{
		for ( int i = 0; i < triggers.size(); ++i )
			((MoodTrigger)triggers.get(i)).execute();
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * object to disk for later retrieval.
	 */

	public synchronized static void saveSettings()
	{
		try
		{
			PrintStream writer = new PrintStream( new FileOutputStream( MOODS_FILE ) );

			LockableListModel triggerList;
			for ( int i = 0; i < keys.length; ++i )
			{
				triggerList = (LockableListModel) reference.get( keys[i] );
				writer.println( "[ " + keys[i] + " ]" );

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

			if ( !MOODS_FILE.exists() )
			{
				MOODS_FILE.getParentFile().mkdirs();
				MOODS_FILE.createNewFile();

				ensureProperty( "default" );
				return;
			}

			BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( MOODS_FILE ) ) );

			String line;
			String currentKey = "";
			LockableListModel currentList = new LockableListModel();

			while ( (line = reader.readLine()) != null )
			{
				line = line.trim();
				if ( line.startsWith( "[" ) )
				{
					currentKey = line.substring( 1, line.length() - 1 ).trim().toLowerCase();
					currentList = new LockableListModel();
					reference.put( currentKey, currentList );
				}
				else if ( line.length() != 0 )
				{
					currentList.add( MoodTrigger.constructNode( line ) );
				}
			}

			reader.close();
			reader = null;

			keys = new String[ reference.keySet().size() ];
			reference.keySet().toArray( keys );

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
			MOODS_FILE.delete();
			loadSettings();
		}
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private synchronized static void ensureProperty( String key )
	{
		if ( !reference.containsKey( key ) )
		{
			LockableListModel defaultList = new LockableListModel();
			defaultList.addAll( triggers );

			reference.put( key, defaultList );
			availableMoods.add( key );

			keys = new String[ reference.keySet().size() ];
			reference.keySet().toArray( keys );

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

	private static class MoodTrigger
	{
		private AdventureResult effect;
		private String stringForm, triggerType, triggerName, action;

		public MoodTrigger( String stringForm, String triggerType, String triggerName, String action )
		{
			this.stringForm = stringForm;
			this.triggerType = triggerType;

			this.triggerName = triggerName;
			this.action = action;

			this.effect = triggerName == null ? null : new AdventureResult( triggerName, 1, true );
		}

		public String toString()
		{	return stringForm;
		}

		public String toSetting()
		{	return effect == null ? triggerType + " => " + action : triggerType + " " + triggerName + " => " + action;
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
				DEFAULT_SHELL.executeLine( action );
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
