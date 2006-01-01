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
import java.util.Collections;
import net.java.dev.spellcast.utilities.UtilityConstants;

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

public class CombatSettings extends TreeMap implements UtilityConstants
{
	private static String [] keys;
	private static File settingsFile;
	private static String characterName = "";
	private static CombatSettings INSTANCE = null;

	public static final CombatSettings getCurrent()
	{
		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			INSTANCE = new CombatSettings( KoLCharacter.getUsername() );

		return INSTANCE;
	}

	/**
	 * Constructs a settings file for a character with the specified name.
	 * Note that in the data file created, all spaces in the character name
	 * will be replaced with an underscore, and all other punctuation will
	 * be removed.
	 *
	 * @param	characterName	The name of the character this settings file represents
	 */

	private CombatSettings( String characterName )
	{
		this.characterName = characterName;
		String noExtensionName = characterName.replaceAll( "\\/q", "" ).replaceAll( " ", "_" ).toLowerCase();
		this.settingsFile = new File( DATA_DIRECTORY + "~" + noExtensionName + ".ccs" );

		ensureDefaults();
		loadSettings( this.settingsFile );
		saveSettings();
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * object to disk for later retrieval.
	 */

	public void saveSettings()
	{	storeSettings( settingsFile );
	}

	/**
	 * Loads the settings located in the given file into this object.
	 * Note that all settings are overridden; if the given file does
	 * not exist, the current global settings will also be rewritten
	 * into the appropriate file.
	 *
	 * @param	source	The file that contains (or will contain) the character data
	 */

	private void loadSettings( File source )
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !source.exists() )
			{
				source.getParentFile().mkdirs();
				source.createNewFile();

				// Then, store the results into the designated
				// file by calling the appropriate subroutine.

				if ( source != settingsFile )
					storeSettings( source );
			}

			BufferedReader reader = new BufferedReader(
				new InputStreamReader( new FileInputStream( source ) ) );

			String line;
			String currentKey = "";
			ArrayList currentList = new ArrayList();

			while ( (line = reader.readLine()) != null )
			{
				line = line.trim();
				if ( line.startsWith( "[" ) )
				{
					currentKey = line.substring( 1, line.length() - 2 ).trim().toLowerCase();
					currentList = new ArrayList();
					put( currentKey, currentList );
				}
				else if ( line.length() != 0 )
					currentList.add( line );

				keys = new String[ keySet().size() ];
				keySet().toArray( keys );
			}

			reader.close();
			reader = null;
		}
		catch ( IOException e1 )
		{
			// This should not happen, because it should
			// always be possible.  Therefore, no handling
			// will take place at the current time unless a
			// pressing need arises for it.
		}
		catch ( Exception e2 )
		{
			// Somehow, the settings were corrupted; this
			// means that they will have to be created after
			// the current file is deleted.

			source.delete();
			loadSettings( source );
		}
	}

	/**
	 * Ensures that all the default keys are non-null.  This is
	 * used so that there aren't lots of null checks whenever a
	 * key is loaded.
	 */

	private void ensureDefaults()
	{
		// The remaining settings are not related to choice
		// adventures and require no special handling.

		ensureProperty( "default", "attack" );
		ensureProperty( "giant", "skill disco face stab; skill disco face stab; moxman" );
		ensureProperty( "mechamech", "skill disco face stab; skill disco face stab; default" );

		ensureProperty( "me4t begZ0r", "item dictionary" );
		ensureProperty( "spam witch", "item dictionary" );
		ensureProperty( "1335 haxx0r", "item dictionary" );
		ensureProperty( "flaming troll", "item dictionary" );
		ensureProperty( "anime smiley", "item dictionary" );
		ensureProperty( "lamz0r n00b", "item dictionary" );
		ensureProperty( "xxx pr0n", "item dictionary" );

		ensureProperty( "baiowulf", "abort" );
		ensureProperty( "crazy bastard", "abort" );
		ensureProperty( "hockey elemental", "abort" );
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private void ensureProperty( String key, String defaultValue )
	{
		if ( !containsKey( key ) )
		{
			ArrayList defaultList = new ArrayList();
			String [] elements = defaultValue.split( "\\s*;\\s*" );
			for ( int i = 0; i < elements.length; ++i )
				defaultList.add( elements[i] );

			put( key, defaultList );
		}
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 *
	 * @param	destination	The file to which the settings will be stored.
	 */

	private void storeSettings( File destination )
	{
		try
		{
			PrintStream writer = new PrintStream( new FileOutputStream( destination ) );
			
			ArrayList combatOptions;
			String [] combatOptionsArray;
			for ( int i = 0; i < keys.length; ++i )
			{
				writer.println( "[ " + keys[i] + " ]" );

				combatOptions = (ArrayList) get( keys[i] );
				combatOptionsArray = new String[ combatOptions.size() ];
				combatOptions.toArray( combatOptionsArray );
				
				for ( int j = 0; j < combatOptionsArray.length; ++j )
					writer.println( combatOptionsArray[j] );

				writer.println();
			}

			writer.close();
		}
		catch ( IOException e )
		{
			// This should not happen, because it should
			// always be possible.  Therefore, no handling
			// will take place at the current time unless a
			// pressing need arises for it.
		}
	}

	public String getSetting( String encounter, int roundCount )
	{
		if ( encounter.equals( "" ) )
			return getSetting( "default", roundCount );

		for ( int i = 0; i < keys.length; ++i )
		{
			if ( encounter.toLowerCase().indexOf( keys[i] ) != -1 )
			{
				ArrayList match = (ArrayList) get( keys[i] );
				String setting = (String) match.get( roundCount < match.size() ? roundCount : match.size() - 1 );
				return setting.equals( "default" ) ? getSetting( "default", roundCount - match.size() ) : setting;
			}
		}
		
		return "attack";
	}
}
