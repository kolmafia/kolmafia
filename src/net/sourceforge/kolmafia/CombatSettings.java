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

import java.util.Vector;
import java.util.Hashtable;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;

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

public class CombatSettings extends Hashtable implements UtilityConstants
{
	private static String [] keys;
	private static File settingsFile;
	private static String characterName = "";
	private static CombatSettings INSTANCE = null;
	private final CombatSettingNode root = new CombatSettingNode();

	public static final CombatSettings getCurrent()
	{
		if ( characterName.equals( "" ) || !characterName.equals( KoLCharacter.getUsername() ) )
			INSTANCE = new CombatSettings();

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

	private CombatSettings()
	{
		this.characterName = KoLCharacter.getUsername();
		String noExtensionName = characterName.replaceAll( "\\/q", "" ).replaceAll( " ", "_" ).toLowerCase();
		this.settingsFile = new File( DATA_DIRECTORY + "~" + noExtensionName + ".ccs" );

		ensureDefaults();
		loadSettings();
		saveSettings();
	}

	public final TreeNode getRoot()
	{	return root;
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

	private void loadSettings()
	{
		try
		{
			// First guarantee that a settings file exists with
			// the appropriate Properties data.

			if ( !this.settingsFile.exists() )
			{
				this.settingsFile.getParentFile().mkdirs();
				this.settingsFile.createNewFile();
			}

			BufferedReader reader = new BufferedReader( new InputStreamReader( new FileInputStream( this.settingsFile ) ) );

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

					put( currentKey, currentList );
					root.add( currentList );
				}
				else if ( line.length() != 0 )
					currentList.add( new CombatActionNode( currentList, line ) );
			}

			reader.close();
			reader = null;

			keys = new String[ keySet().size() ];
			keySet().toArray( keys );
		}
		catch ( IOException e1 )
		{
			// This should not happen, because it should
			// always be possible.  Therefore, no handling
			// will take place at the current time unless a
			// pressing need arises for it.

			e1.printStackTrace( KoLmafia.getLogStream() );
			e1.printStackTrace();
		}
		catch ( Exception e2 )
		{
			// Somehow, the settings were corrupted; this
			// means that they will have to be created after
			// the current file is deleted.

			e2.printStackTrace( KoLmafia.getLogStream() );
			e2.printStackTrace();

			this.settingsFile.delete();
			loadSettings();
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

		ensureProperty( "baiowulf", "abort" );
		ensureProperty( "crazy bastard", "abort" );
		ensureProperty( "hockey elemental", "abort" );
		ensureProperty( "hypnotist of hey deze", "abort" );
		ensureProperty( "infinite meat bug", "abort" );
	}

	/**
	 * Ensures that the given property exists, and if it does not exist,
	 * initializes it to the given value.
	 */

	private void ensureProperty( String key, String defaultValue )
	{
		if ( !containsKey( key ) )
		{
			CombatSettingNode defaultList = new CombatSettingNode( key );
			String [] elements = defaultValue.split( "\\s*;\\s*" );
			for ( int i = 0; i < elements.length; ++i )
				defaultList.add( new CombatActionNode( defaultList, elements[i] ) );

			put( key, defaultList );
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

	private void storeSettings( File destination )
	{
		try
		{
			PrintStream writer = new PrintStream( new FileOutputStream( destination ) );

			CombatSettingNode combatOptions;
			CombatActionNode [] combatOptionsArray;
			for ( int i = 0; i < keys.length; ++i )
			{
				writer.println( "[ " + keys[i] + " ]" );

				combatOptions = (CombatSettingNode) get( keys[i] );
				combatOptionsArray = new CombatActionNode[ combatOptions.size() ];
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

			e.printStackTrace( KoLmafia.getLogStream() );
			e.printStackTrace();
		}
	}

	public String getSetting( String encounter, int roundCount )
	{
		if ( encounter.equals( "" ) )
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

		CombatSettingNode match = (CombatSettingNode) get( keys[ longestMatch ] );
		CombatActionNode setting = (CombatActionNode) match.get( roundCount < match.size() ? roundCount : match.size() - 1 );

		return setting.startsWith( "abort" ) || setting.startsWith( "attack" ) || setting.startsWith( "item" ) ||
			setting.startsWith( "skill" ) ? setting.toString() : getSetting( setting.toString(), roundCount - match.size() + 1 );
	}

	private class CombatSettingNode extends Vector implements TreeNode
	{
		private String name;

		public CombatSettingNode()
		{	this.name = "";
		}

		public CombatSettingNode( String name )
		{	this.name = name;
		}

		public Enumeration children()
		{	return elements();
		}

		public boolean getAllowsChildren()
		{	return true;
		}

		public int getChildCount()
		{	return size();
		}

		public TreeNode getChildAt( int childIndex )
		{	return (TreeNode) get( childIndex );
		}

		public int getIndex( TreeNode node )
		{	return indexOf( node );
		}

		public TreeNode getParent()
		{	return root;
		}

		public boolean isLeaf()
		{	return false;
		}

		public String toString()
		{	return name;
		}
	}

	private class CombatActionNode implements TreeNode
	{
		private TreeNode parent;
		private String action;

		public CombatActionNode( TreeNode parent, String action )
		{
			this.parent = parent;
			this.action = action;
		}

		public Enumeration children()
		{	return null;
		}

		public boolean getAllowsChildren()
		{	return false;
		}

		public TreeNode getChildAt( int childIndex )
		{	return null;
		}

		public int getChildCount()
		{	return 0;
		}

		public int getIndex( TreeNode node )
		{	return -1;
		}

		public TreeNode getParent()
		{	return parent;
		}

		public boolean isLeaf()
		{	return true;
		}

		public boolean startsWith( String prefix )
		{	return action.startsWith( prefix );
		}

		public String toString()
		{	return action;
		}
	}
}
