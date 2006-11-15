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
import java.util.ArrayList;
import java.util.TreeMap;
import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultMutableTreeNode;

import net.java.dev.spellcast.utilities.UtilityConstants;

public abstract class CombatSettings implements UtilityConstants, KoLConstants
{
	static
	{
		// Renaming data files to make then easier to find for most
		// people (so they aren't afraid to open them).

		StaticEntity.renameDataFiles( "ccs", "combat" );
	}

	private static String [] keys = new String[0];
	private static File settingsFile = null;
	private static TreeMap reference = new TreeMap();
	private static CombatSettingNode root = new CombatSettingNode();

	public static final void reset()
	{
		settingsFile = new File( settingsFileName() );
		root.removeAllChildren();
		reference.clear();

		loadSettings();
		ensureProperty( "default", "attack with weapon" );
		saveSettings();
	}

	public static final String settingsFileName()
	{	return "settings/combat_" + KoLCharacter.baseUserName() + ".txt";
	}

	public static final TreeNode getRoot()
	{	return root;
	}

	public static void loadSettings( File source )
	{
		if ( source == null || settingsFile == null )
			return;

		if ( settingsFile.getAbsolutePath().equals( source.getAbsolutePath() ) )
			return;

		settingsFile = source;

		loadSettings();
		ensureProperty( "default", "attack with weapon" );
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
				settingsFile.createNewFile();

				keys = new String[ reference.keySet().size() ];
				reference.keySet().toArray( keys );

				return;
			}

			BufferedReader reader = KoLDatabase.getReader( settingsFile );
			String line;
			CombatSettingNode currentList = root;

			while ( (line = reader.readLine()) != null )
			{
				line = line.trim();
				if ( line.startsWith( "[" ) )
				{
					if ( currentList != root && currentList.getChildCount() == 0 )
						currentList.add( new CombatActionNode( 1, "attack with weapon" ) );

					String currentKey = encounterKey( line.substring( 1, line.length() - 1 ) );
					currentList = new CombatSettingNode( currentKey );

					reference.put( currentKey, currentList );
					root.add( currentList );
				}
				else if ( line.length() != 0 )
				{
					// If it looks like this is a KoLmafia-created settings file,
					// then parse it accordingly.

					if ( Character.isDigit( line.charAt(0) ) )
					{
						String [] pieces = line.split( "\\s*:\\s*" );
						int desiredIndex = StaticEntity.parseInt( pieces[0] );

						if ( pieces.length == 2 && desiredIndex >= currentList.getChildCount() )
						{
							String action = currentList.getChildCount() > 0 ?
								((CombatActionNode) currentList.getLastChild()).action : "attack with weapon";

							while ( currentList.getChildCount() < desiredIndex - 1 )
								currentList.add( new CombatActionNode( currentList.getChildCount() + 1, action ) );

							currentList.add( new CombatActionNode( desiredIndex, pieces[1].trim() ) );
						}
					}

					// Otherwise, handle it as though the person is using the old
					// style format.

					else
					{
						currentList.add( new CombatActionNode( currentList.getChildCount() + 1, line ) );
					}
				}
			}

			if ( currentList != root && currentList.getChildCount() == 0 )
				currentList.add( new CombatActionNode( 1, "attack with weapon" ) );

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

	public static String encounterKey( String line )
	{	return encounterKey( line, true );
	}

	public static String encounterKey( String line, boolean changeCase )
	{
		line = line.trim();
		String key = line.toLowerCase();

		if ( key.startsWith( "a " ) )
		{
			key = key.substring( 2 );
			line = line.substring( 2 );
		}
		else if ( key.startsWith( "an " ) )
		{
			key = key.substring( 3 );
			line = line.substring( 3 );
		}
		else if ( key.startsWith( "the " ) )
		{
			key = key.substring( 4 );
			line = line.substring( 4 );
		}

		return changeCase ? key : line;
    }

	public static void setDefaultAction( String actionList )
	{
		CombatSettingNode currentList = (CombatSettingNode) reference.get( "default" );
		currentList.removeAllChildren();

		String [] rounds = actionList.split( "\\s*;\\s*" );
		for ( int i = 0; i < rounds.length; ++i )
			currentList.add( new CombatActionNode( i + 1, rounds[i] ) );
	}

	public static List getDefaultAction()
	{
		ArrayList nodeList = new ArrayList();
		CombatSettingNode currentList = (CombatSettingNode) reference.get( "default" );
		for ( int i = 0; i < currentList.getChildCount(); ++i )
			nodeList.add( currentList.getChildAt(i) );

		return nodeList;
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
				defaultList.add( new CombatActionNode( i + 1, elements[i] ) );

			reference.put( key, defaultList );
			root.add( defaultList );
		}
	}

	/**
	 * Stores the settings maintained in this <code>KoLSettings</code>
	 * to the noted file.  Note that this method ALWAYS overwrites
	 * the given file.
	 */

	public static void saveSettings()
	{
		try
		{
			if ( settingsFile.exists() )
				settingsFile.delete();

			settingsFile.createNewFile();
			PrintStream writer = new LogStream( settingsFile );

			CombatSettingNode combatOptions;
			for ( int i = 0; i < keys.length; ++i )
			{
				writer.println( "[ " + keys[i] + " ]" );

				combatOptions = (CombatSettingNode) reference.get( keys[i] );
				String action = null, newAction = null;

				for ( int j = 0; j < combatOptions.getChildCount(); ++j )
				{
					if ( action == null )
					{
						action = ((CombatActionNode)combatOptions.getChildAt(j)).getAction();
						writer.println( combatOptions.getChildAt(j) );
					}
					else
					{
						newAction = ((CombatActionNode)combatOptions.getChildAt(j)).getAction();
						if ( !action.equals( newAction ) )
						{
							action = newAction;
							writer.println( combatOptions.getChildAt(j) );
						}
					}
				}

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
		String location = StaticEntity.getProperty( "lastAdventure" ).toLowerCase();

		// Allow for longer matches (closer to exact matches)
		// by tracking the length of the match.

		int longestMatch = -1;
		int longestMatchLength = 0;

		if ( encounter != null && !encounter.equals( "" ) )
		{
			for ( int i = 0; i < keys.length; ++i )
			{
				if ( encounter.indexOf( keys[i] ) != -1 )
				{
					if ( keys[i].length() > longestMatchLength )
					{
						longestMatch = i;
						longestMatchLength = keys[i].length();
					}
				}
			}
		}

		// If no matches were found, then see if there is a match
		// against the adventure location.

		if ( longestMatch == -1 && location != null )
		{
			for ( int i = 0; i < keys.length; ++i )
			{
				if ( location.indexOf( keys[i] ) != -1 )
				{
					if ( keys[i].length() > longestMatchLength )
					{
						longestMatch = i;
						longestMatchLength = keys[i].length();
					}
				}
			}
		}

		if ( longestMatch == -1 )
			return encounter == null || encounter.equals( "default" ) ? "attack with weapon" : getSetting( "default", roundCount );

		// Otherwise, you have a tactic for this round against
		// the given monster.  Return that tactic.

		CombatSettingNode match = (CombatSettingNode) reference.get( keys[ longestMatch ] );
		if ( match.getChildCount() == 0 )
			return "attack with weapon";

		CombatActionNode setting = (CombatActionNode) match.getChildAt(
			roundCount < match.getChildCount() ? roundCount : match.getChildCount() - 1 );

		return getShortCombatOptionName( setting.getAction() );
	}

	private static class CombatSettingNode extends DefaultMutableTreeNode
	{
		private String name;
		private boolean willDelevel = false;

		public CombatSettingNode()
		{	this( "" );
		}

		public CombatSettingNode( String name )
		{
			super( name, true );
			this.name = name;
		}

		public void add( CombatActionNode node )
		{
			if ( willDelevel )
				return;

			willDelevel |= node.getAction().equalsIgnoreCase( "delevel" );
			super.add( node );
		}

		public String toString()
		{	return name;
		}
	}

	private static class CombatActionNode extends DefaultMutableTreeNode
	{
		private int index;
		private String action;

		public CombatActionNode( int index, String action )
		{
			super( action, false );

			this.index = index;
			this.action = getLongCombatOptionName( action );
		}

		public boolean startsWith( String prefix )
		{	return action.startsWith( prefix );
		}

		public String getAction()
		{	return action;
		}

		public String toString()
		{	return index + ": " + action;
		}
	}

	public static String getLongCombatOptionName( String action )
	{
		if ( action == null || action.length() == 0 || action.startsWith( "attack" ) )
			return "attack with weapon";

		action = action.trim();

		if ( action.startsWith( "default" ) || action.startsWith( "abort" ) || action.startsWith( "consult" ) )
			return action;

		else if ( action.startsWith( "delevel" ) )
			return "delevel and plink";

		else if ( action.indexOf( "run" ) != -1 && action.indexOf( "away" ) != -1 )
			return "try to run away";

		else if ( action.startsWith( "custom" ) )
			return "custom combat script";

		else if ( action.startsWith( "item" ) )
		{
			AdventureResult item = DEFAULT_SHELL.getFirstMatchingItem( action.substring(4).trim() );
			if ( item != null )
				return "item " + KoLDatabase.getCanonicalName( item.getName() );
		}

		else if ( action.startsWith( "skill" ) )
		{
			String potentialSkill = KoLmafiaCLI.getCombatSkillName( action.substring(5).trim() );
			if ( potentialSkill != null )
				return "skill " + potentialSkill.toLowerCase();
		}

		// Well, it's either a standard skill, or it's an item,
		// or it's something you need to lookup in the tables.

		String potentialSkill = KoLmafiaCLI.getCombatSkillName( action );
		if ( potentialSkill != null )
			return "skill " + potentialSkill.toLowerCase();

		int itemId = action.equals( "" ) ? -1 :
			KoLmafiaCLI.getFirstMatchingItemId( TradeableItemDatabase.getMatchingNames( action ) );

		if ( itemId > 0 )
		{
			String potentialItem = TradeableItemDatabase.getItemName( itemId );
			if ( potentialItem != null )
				return "item " + potentialItem.toLowerCase();
		}

		return "attack with weapon";
	}

	public static String getShortCombatOptionName( String action )
	{
		if ( action == null || action.length() == 0 || action.startsWith( "attack" ) )
			return "attack";

		action = action.trim();

		if ( action.startsWith( "consult" ) )
			return action;

		if ( action.startsWith( "attack" ) )
			return "attack";

		boolean isSkillNumber = true;
		for ( int i = 0; i < action.length(); ++i )
			isSkillNumber &= Character.isDigit( action.charAt(i) );

		if ( isSkillNumber )
			return action;

		if ( action.startsWith( "custom" ) )
			return "custom";

		if ( action.startsWith( "delevel" ) )
			return "delevel";

		if ( action.startsWith( "abort" ) )
			return "abort";

		if ( action.startsWith( "attack with weapon" ) )
			return "attack";

		if ( action.indexOf( "run" ) != -1 && action.indexOf( "away" ) != -1 )
			return "runaway";

		if ( action.startsWith( "skill" ) )
		{
			String name = KoLmafiaCLI.getCombatSkillName( action.substring(5).trim() );
			return name == null ? "attack with weapon" : String.valueOf( ClassSkillsDatabase.getSkillId( name ) );
		}

		if ( action.startsWith( "item" ) )
		{
			String name = action.substring(4).trim();
			for ( int i = 0; i < name.length(); ++i )
				if ( !Character.isDigit( name.charAt(i) ) )
				{
					int itemId = TradeableItemDatabase.getItemId( name );
					if ( itemId == FightRequest.DICTIONARY1.getItemId() && !inventory.contains( FightRequest.DICTIONARY1 ) )
						itemId = FightRequest.DICTIONARY2.getItemId();

					if ( itemId == FightRequest.DICTIONARY2.getItemId() && !inventory.contains( FightRequest.DICTIONARY2 ) )
						itemId = FightRequest.DICTIONARY1.getItemId();

					return "item" + itemId;
				}

			return action;
		}

		String potentialSkill = KoLmafiaCLI.getCombatSkillName( action );
		if ( potentialSkill != null )
			return String.valueOf( ClassSkillsDatabase.getSkillId( potentialSkill ) );

		int itemId = action.equals( "" ) ? -1 :
			KoLmafiaCLI.getFirstMatchingItemId( TradeableItemDatabase.getMatchingNames( action ) );

		if ( itemId != -1 )
			return "item" + itemId;

		return "attack";
	}
}
