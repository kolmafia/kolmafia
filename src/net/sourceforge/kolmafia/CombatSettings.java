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

import java.util.List;
import java.util.Vector;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;

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

public abstract class CombatSettings implements UtilityConstants
{
	private static String [] keys;
	private static File settingsFile;
	private static String characterName = "";
	private static TreeMap reference = new TreeMap();
	private static CombatSettingNode root = new CombatSettingNode();

	static { CombatSettings.reset(); }

	public static final void reset()
	{
		CombatSettings.characterName = KoLCharacter.getUsername();
		CombatSettings.settingsFile = new File( DATA_DIRECTORY + settingsFileName() );

		root.removeAllChildren();
		reference.clear();

		loadSettings();
		saveSettings();
	}

	public static final String settingsFileName()
	{	return "~" + KoLCharacter.getUsername().replaceAll( "\\/q", "" ).replaceAll( " ", "_" ).toLowerCase() + ".ccs";
	}

	public static final TreeNode getRoot()
	{
		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			CombatSettings.reset();

		return root;
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * object to disk for later retrieval.
	 */

	public static void saveSettings()
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

	private static void loadSettings()
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !settingsFile.exists() )
			{
				settingsFile.getParentFile().mkdirs();
				settingsFile.createNewFile();

				ensureProperty( "default", "attack" );

				keys = new String[ reference.keySet().size() ];
				reference.keySet().toArray( keys );

				return;
			}

			BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( settingsFile ) ) );

			String line;
			String currentKey = "";
			CombatSettingNode currentList = root;

			while ( (line = reader.readLine()) != null )
			{
				line = line.trim();
				if ( line.startsWith( "[" ) )
				{
					currentKey = line.substring( 1, line.length() - 2 ).trim().toLowerCase();
					currentList = new CombatSettingNode( currentKey );

					reference.put( currentKey, currentList );
					root.add( currentList );
				}
				else if ( line.length() != 0 )
					currentList.add( new CombatActionNode( line ) );
			}

			reader.close();
			reader = null;

			keys = new String[ reference.keySet().size() ];
			reference.keySet().toArray( keys );
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

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private static void ensureProperty( String key, String defaultValue )
	{
		if ( !reference.containsKey( key ) )
		{
			CombatSettingNode defaultList = new CombatSettingNode( key );
			String [] elements = defaultValue.split( "\\s*;\\s*" );
			for ( int i = 0; i < elements.length; ++i )
				defaultList.add( new CombatActionNode( elements[i] ) );

			reference.put( key, defaultList );
			root.add( defaultList );
		}
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 *
	 * @param	destination	The file to which the settings will be stored.
	 */

	private static void storeSettings( File destination )
	{
		try
		{
			PrintStream writer = new PrintStream( new FileOutputStream( destination ) );

			CombatSettingNode combatOptions;
			for ( int i = 0; i < keys.length; ++i )
			{
				writer.println( "[ " + keys[i] + " ]" );

				combatOptions = (CombatSettingNode) reference.get( keys[i] );
				for ( int j = 0; j < combatOptions.getChildCount(); ++j )
					writer.println( combatOptions.getChildAt(j) );

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

	public static String getSetting( String encounter, int roundCount )
	{
		if ( !characterName.equals( KoLCharacter.getUsername() ) )
			CombatSettings.reset();

		if ( encounter == null || encounter.equals( "" ) )
			return getSetting( "default", roundCount );

		// Allow for longer matches (closer to exact matches)
		// by tracking the length of the match.

		int longestMatch = -1;
		int longestMatchLength = 0;

		for ( int i = 0; i < keys.length; ++i )
		{
			if ( encounter.toLowerCase().indexOf( keys[i] ) != -1 )
			{
				if ( keys[i].length() > longestMatchLength )
				{
					longestMatch = i;
					longestMatchLength = keys[i].length();
				}
			}
		}

		// If no matches were found, then resort to the normal
		// default routine -- because default is stored, there
		// will definitely be a match.

		if ( longestMatch == -1 )
			return getSetting( "default", roundCount );

		// Otherwise, you have a tactic for this round against
		// the given monster.  Return that tactic.

		CombatSettingNode match = (CombatSettingNode) reference.get( keys[ longestMatch ] );
		if ( match.getChildCount() == 0 )
			return "attack";

		CombatActionNode setting = (CombatActionNode) match.getChildAt(
			roundCount < match.getChildCount() ? roundCount : match.getChildCount() - 1 );

		if ( setting.startsWith( "abort" ) || setting.startsWith( "attack" ) || setting.startsWith( "moxman" ) || setting.startsWith( "item" ) || setting.startsWith( "skill" ) || setting.startsWith( "run" ) )
			return setting.toString();

		// Well, it's either a standard skill, or it's an item,
		// or it's something you need to lookup in the tables.

		String potentialSkill = KoLmafiaCLI.getCombatSkillName( setting.toString() );
		if ( potentialSkill != null )
			return "skill " + potentialSkill;

		int itemID = setting.toString().equals( "" ) ? -1 : KoLmafiaCLI.getFirstMatchingItemID(
			TradeableItemDatabase.getMatchingNames( setting.toString() ), KoLmafiaCLI.NOWHERE );

		if ( itemID != -1 )
			return "item " + TradeableItemDatabase.getItemName( itemID );

		return getSetting( setting.toString(), roundCount - match.getChildCount() + 1 );
	}

	private static class CombatSettingNode extends DefaultMutableTreeNode
	{
		private String name;

		public CombatSettingNode()
		{	this( "" );
		}

		public CombatSettingNode( String name )
		{
			super( name, true );
			this.name = name;
		}

		public String toString()
		{	return name;
		}
	}

	private static class CombatActionNode extends DefaultMutableTreeNode
	{
		private String action;

		public CombatActionNode( String action )
		{
			super( action, false );
			this.action = action;
		}

		public boolean startsWith( String prefix )
		{	return action.startsWith( prefix );
		}

		public String toString()
		{	return action;
		}
	}
}
