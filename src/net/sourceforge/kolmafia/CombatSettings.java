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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public abstract class CombatSettings implements KoLConstants
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

	public static final void restoreDefaults()
	{
		root.removeAllChildren();
		reference.clear();

		if ( KoLCharacter.baseUserName().equals( "GLOBAL" ) )
			return;

		settingsFile = new File( SETTINGS_LOCATION, settingsFileName() );

		loadSettings();
		ensureProperty( "default", "attack with weapon" );
		saveSettings();
	}

	public static final String settingsFileName()
	{	return "combat_" + KoLCharacter.baseUserName() + ".txt";
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

				keys = new String[ reference.size() ];
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
					continue;
				}

				if ( line.length() == 0 )
					continue;

				int desiredIndex = currentList.getChildCount() + 1;

				// If it looks like this is a KoLmafia-created settings file,
				// then parse it accordingly.

				if ( Character.isDigit( line.charAt(0) ) )
				{
					int colonIndex = line.indexOf( ":" );
					if ( colonIndex != -1 )
					{
						desiredIndex = StaticEntity.parseInt( line.substring( 0, colonIndex ) );
						line = line.substring( colonIndex + 1 );
					}
				}

				if ( desiredIndex >= currentList.getChildCount() )
				{
					String action = currentList.getChildCount() > 0 ?
						((CombatActionNode) currentList.getLastChild()).action : "attack with weapon";

					while ( currentList.getChildCount() < desiredIndex - 1 )
						currentList.add( new CombatActionNode( currentList.getChildCount() + 1, action ) );

					currentList.add( new CombatActionNode( desiredIndex, line ) );
				}
			}

			if ( currentList != root && currentList.getChildCount() == 0 )
				currentList.add( new CombatActionNode( 1, "delevel and plink" ) );

			reader.close();
			reader = null;

			keys = new String[ reference.size() ];
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
		else if ( key.startsWith( "some " ) )
		{
			key = key.substring( 5 );
			line = line.substring( 5 );
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
		PrintStream writer = LogStream.openStream( settingsFile, true );

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

	public static String getSettingKey( String encounter )
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
			return "default";

		return keys[ longestMatch ];
	}

	public static String getSetting( String encounter, int roundCount )
	{
		// Otherwise, you have a tactic for this round against
		// the given monster.  Return that tactic.

		CombatSettingNode match = (CombatSettingNode) reference.get( getSettingKey( encounter ) );
		if ( match.getChildCount() == 0 )
			return "attack";

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
			if ( this.willDelevel )
				return;

			this.willDelevel |= node.getAction().equalsIgnoreCase( "delevel" );
			super.add( node );
		}

		public String toString()
		{	return this.name;
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
		{	return this.action.startsWith( prefix );
		}

		public String getAction()
		{	return this.action;
		}

		public String toString()
		{	return this.index + ": " + this.action;
		}
	}

	public static String getLongCombatOptionName( String action )
	{
		if ( action == null )
			return "attack with weapon";

		action = action.trim();

		if ( action.startsWith( "attack" ) || action.length() == 0 )
			return "attack with weapon";

		if ( action.indexOf( "steal" ) != -1 || action.indexOf( "pick" ) != -1 )
			return "try to steal an item";

		if ( action.startsWith( "default" ) || action.startsWith( "abort" ) || action.startsWith( "consult" ) )
			return action;

		if ( action.startsWith( "custom" ) )
			return "custom combat script";

		if ( action.startsWith( "delevel" ) )
			return "delevel and plink";

		if ( action.startsWith( "twiddle" ) )
			return "twiddle your thumbs";

		if ( action.indexOf( "run" ) != -1 && action.indexOf( "away" ) != -1 )
			return "try to run away";

		if ( action.startsWith( "item" ) )
		{
			String item = getLongItemAction( action.substring(4) );
			return item.startsWith( "attack" ) ? item : "item " + item;
		}

		if ( action.startsWith( "skill" ) )
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

		String item = getLongItemAction( action );
		return item.startsWith( "attack" ) ? item : "item " + item;
	}

	private static String getLongItemAction( String action )
	{
		int commaIndex = action.indexOf( "," );
		if ( commaIndex != -1 )
		{
			String first = getLongItemAction( action.substring( 0, commaIndex ) );
			if ( first.startsWith( "attack" ) )
				return getLongItemAction( action.substring( commaIndex + 1 ).trim() );

			String second = getLongItemAction( action.substring( commaIndex + 1 ).trim() );
			if ( second.startsWith( "attack" ) )
				return first;

			return first + ", " + second;
		}

		if ( action.startsWith( "item" ) )
			return getLongItemAction( action.substring(4).trim() );

		int itemId = KoLmafiaCLI.getFirstMatchingItemId( TradeableItemDatabase.getMatchingNames( action ) );
		if ( itemId <= 0 )
			return "attack with weapon";

		return TradeableItemDatabase.getItemName( itemId );
	}

	public static String getShortCombatOptionName( String action )
	{
		if ( action == null )
			return "attack";

		action = action.trim();

		boolean isSkillNumber = true;
		for ( int i = 0; i < action.length(); ++i )
			isSkillNumber &= Character.isDigit( action.charAt(i) );

		if ( isSkillNumber )
			return action;

		if ( action.startsWith( "attack" ) || action.length() == 0 )
			return "attack";

		if ( action.startsWith( "abort" ) )
			return "abort";

		if ( action.indexOf( "steal" ) != -1 || action.indexOf( "pick" ) != -1 )
			return "steal";

		if ( action.startsWith( "consult" ) )
			return action;

		if ( action.startsWith( "custom" ) )
			return "custom";

		if ( action.startsWith( "delevel" ) )
			return "delevel";

		if ( action.startsWith( "twiddle" ) )
			return "twiddle";

		if ( action.indexOf( "run" ) != -1 && action.indexOf( "away" ) != -1 )
			return "runaway";

		if ( action.startsWith( "item" ) )
			return getShortItemAction( action.substring(4).trim() );

		if ( action.startsWith( "skill" ) )
		{
			String name = KoLmafiaCLI.getCombatSkillName( action.substring(5).trim() );
			return name == null ? "attack with weapon" : "skill" + ClassSkillsDatabase.getSkillId( name );
		}

		String potentialSkill = KoLmafiaCLI.getCombatSkillName( action );
		if ( potentialSkill != null )
			return "skill" + ClassSkillsDatabase.getSkillId( potentialSkill );

		return getShortItemAction( action );
	}

	private static String getShortItemAction( String action )
	{
		int commaIndex = action.indexOf( "," );
		if ( commaIndex != -1 )
		{
			String first = getShortItemAction( action.substring( 0, commaIndex ) );
			if ( first.startsWith( "attack" ) )
				return getShortItemAction( action.substring( commaIndex + 1 ).trim() );

			String second = getShortItemAction( action.substring( commaIndex + 1 ).trim() );
			if ( second.startsWith( "attack" ) )
				return first;

			return first + "," + second;
		}

		if ( action.startsWith( "item" ) )
			return getShortItemAction( action.substring(4) );

		int itemId = TradeableItemDatabase.getItemId( action );
		if ( itemId == FightRequest.DICTIONARY1.getItemId() && !inventory.contains( FightRequest.DICTIONARY1 ) )
			itemId = FightRequest.DICTIONARY2.getItemId();

		if ( itemId == FightRequest.DICTIONARY2.getItemId() && !inventory.contains( FightRequest.DICTIONARY2 ) )
			itemId = FightRequest.DICTIONARY1.getItemId();

		return itemId <= 0 ? "attack" : String.valueOf( itemId );
	}
}
