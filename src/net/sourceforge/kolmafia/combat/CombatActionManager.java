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

package net.sourceforge.kolmafia.combat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.util.HashSet;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;

import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.DiscoCombatHelper;

public abstract class CombatActionManager
{
	public static final Pattern TRY_TO_RUN_AWAY_PATTERN = Pattern.compile( "run away if (\\d+)% chance of being free" );

	private static final LockableListModel availableLookups = new LockableListModel();
	private static final CustomCombatLookup strategyLookup = new CustomCombatLookup();

	public static final void updateFromPreferences()
	{
		CombatActionManager.loadStrategyLookup( CombatActionManager.getStrategyLookupName() );
	}

	public static final LockableListModel getAvailableLookups()
	{
		String[] list = DataUtilities.list( KoLConstants.CCS_LOCATION );

		for ( int i = 0; i < list.length; ++i )
		{
			if ( list[ i ].endsWith( ".ccs" ) )
			{
				String name = list[ i ].substring( 0, list[ i ].length() - 4 );

				if ( !CombatActionManager.availableLookups.contains( name ) )
				{
					CombatActionManager.availableLookups.add( name );
				}
			}
		}

		if ( !CombatActionManager.availableLookups.contains( "default" ) )
		{
			CombatActionManager.availableLookups.add( "default" );
		}

		return CombatActionManager.availableLookups;
	}

	public static final File getStrategyLookupFile()
	{
		return CombatActionManager.getStrategyLookupFile( CombatActionManager.getStrategyLookupName() );
	}

	public static final File getStrategyLookupFile( String name )
	{
		if ( !name.endsWith( ".ccs" ) )
		{
			name = name + ".ccs";
		}

		return new File( KoLConstants.CCS_LOCATION, name );
	}

	public static void loadStrategyLookup( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			name = "default";
		}

		if ( name.endsWith( ".ccs" ) )
		{
			name = name.substring( 0, name.length() - 4 );
		}

		if ( !CombatActionManager.availableLookups.contains( name ) )
		{
			CombatActionManager.availableLookups.add( name );
		}

		CombatActionManager.strategyLookup.removeAllChildren();
		CombatActionManager.strategyLookup.addEncounterKey( "default" );

		File file = getStrategyLookupFile( name );

		if ( !file.exists() )
		{
			PrintStream ostream = LogStream.openStream( file, true );
			ostream.println( "[ default ]" );
			ostream.println( "special action" );
			ostream.println( "attack with weapon" );
			ostream.close();
		}

		try
		{
			BufferedReader reader = FileUtilities.getReader( file );

			CombatActionManager.strategyLookup.load( reader );

			reader.close();
		}
		catch ( IOException e1 )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e1 );
		}

		Preferences.setString( "customCombatScript", name );

		CombatActionManager.availableLookups.setSelectedItem( name );
	}

	public static final String getStrategyLookupName()
	{
		String script = Preferences.getString( "customCombatScript" );

		if ( script == null || script.length() == 0 )
		{
			return "default";
		}

		return script;
	}

	public static final void saveStrategyLookup( String name )
	{
		PrintStream writer = LogStream.openStream( getStrategyLookupFile( name ), true );

		CombatActionManager.strategyLookup.store( writer );

		writer.close();
	}

	public static final CustomCombatLookup getStrategyLookup()
	{
		return CombatActionManager.strategyLookup;
	}

	public static final void copyStrategyLookup( String name )
	{
		if ( name == null || name.equals( "" ) )
		{
			return;
		}

		File source = getStrategyLookupFile();
		File destination = getStrategyLookupFile( name );

		FileUtilities.copyFile( source, destination );
	}

	public static final String encounterKey( final String line )
	{
		return CombatActionManager.encounterKey( line, true );
	}

	public static final String encounterKey( String line, final boolean changeCase )
	{
		line = StringUtilities.globalStringReplace( line.trim(), "  ", " " );

		String key = StringUtilities.getCanonicalName( line );
		line = StringUtilities.getEntityEncode( line );

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
			// It really is "The Man" or "The Astronomer"
		}
		else if ( key.startsWith( "some " ) )
		{
			key = key.substring( 5 );
			line = line.substring( 5 );
		}

		return changeCase ? key : line;
	}

	public static final void setDefaultAction( final String actionList )
	{
		CombatActionManager.strategyLookup.clearEncounterKey( "default" );

		String[] rounds = actionList.split( "\\s*;\\s*" );

		for ( int i = 0; i < rounds.length; ++i )
		{
			CombatActionManager.strategyLookup.addEncounterAction( "default", i + 1, "", rounds[ i ], false );
		}
	}

	public static final boolean hasGlobalPrefix()
	{
		return CombatActionManager.strategyLookup.getStrategy( "global prefix" ) != null;
	}

	public static final String getEncounterKey( final String encounter )
	{
		return CombatActionManager.strategyLookup.getBestEncounterKey( encounter );
	}

	private static boolean atEndOfStrategy;

	public static final boolean atEndOfStrategy()
	{
		return CombatActionManager.atEndOfStrategy;
	}

	public static final String getCombatAction( final String encounter, final int roundIndex, boolean allowMacro )
	{
		CombatActionManager.atEndOfStrategy = false;

		if ( roundIndex < 0 || roundIndex >= 100 )
		{
			// prevent hang if the combat is somehow not progressing at all
			CombatActionManager.atEndOfStrategy = true;
			return "abort";
		}

		if ( !encounter.equals( "global prefix" ) )
		{
			String action = Preferences.getString( "battleAction" );

			// Custom combat doesn't have a simple action.

			if ( !action.startsWith( "custom" ) )
			{
				// Use the round index to decide what action to return.

				switch ( roundIndex )
				{
				case 0:
					return Preferences.getBoolean( "autoSteal" ) ? "try to steal an item" : "skip";
				case 1:
					return Preferences.getBoolean( "autoSteal" ) && KoLCharacter.hasEquipped( ItemPool.get(
						ItemPool.NEW_WAVE_BLING, 1 ) ) ? "try to steal an item" : "skip";
				case 2:
					if ( KoLCharacter.getClassName().equals( "Seal Clubber") )
					{
						return Preferences.getBoolean( "autoEntangle" ) ? "skill Club Foot" : "skip";
					}
					else
					{
						return Preferences.getBoolean( "autoEntangle" ) ? "skill Entangling Noodles" : "skip";
					}					
				case 3:
					return "special action";
				default:
					CombatActionManager.atEndOfStrategy = true;
					return action;
				}
			}
		}

		String encounterKey = CombatActionManager.getEncounterKey( encounter );
		
		CustomCombatStrategy strategy = CombatActionManager.strategyLookup.getStrategy( encounterKey );
		int actionCount = strategy.getActionCount( strategyLookup, new HashSet() );
		
		if ( roundIndex + 1 >= actionCount )
		{
			CombatActionManager.atEndOfStrategy = true;
		}

		return strategy.getAction( CombatActionManager.strategyLookup, roundIndex, allowMacro );
	}

	public static final boolean isMacroAction( String action )
	{
		return
			action.startsWith( "scrollwhendone" ) ||
			action.startsWith( "mark " ) ||
			action.startsWith( "goto " ) ||
			action.startsWith( "if " ) ||
			action.startsWith( "endif" ) ||
			action.startsWith( "while " ) ||
			action.startsWith( "endwhile" ) ||
			action.startsWith( "sub " ) ||
			action.startsWith( "endsub" ) ||
			action.startsWith( "call " ) ||
			action.startsWith( "#" ) ||
			action.startsWith( "\"" );
	}

	public static final String getLongCombatOptionName( String action )
	{
		if ( action == null )
		{
			return "attack with weapon";
		}

		action = action.trim();

		if ( action.startsWith( "attack" ) || action.length() == 0 )
		{
			return "attack with weapon";
		}

		if ( isMacroAction( action ) )
		{
			return action;
		}

		if ( action.contains( "pick" ) ||
		     ( action.contains( "steal" ) && !action.contains( "stealth" ) &&
		      !action.contains( "combo" ) && !action.contains( "accordion" ) ) )
		{
			return "try to steal an item";
		}

		if ( action.equals( "default" ) )
		{
			return "default";
		}

		if ( action.startsWith( "section" ) )
		{
			return action;
		}

		if ( action.startsWith( "jiggle" ) )
		{
			return "jiggle chefstaff";
		}

		if ( action.startsWith( "special" ) )
		{
			return "special action";
		}

		if ( action.equals( "skip" ) )
		{
			return "skip";
		}

		if ( action.startsWith( "note" ) )
		{
			return action;
		}

		if ( action.startsWith( "summon" ) && action.endsWith( "ghost" ) )
		{
			return "summon pastamancer ghost";
		}

		if ( action.startsWith( "abort" ) )
		{
			if ( action.indexOf( "after" ) != -1 )
			{
				return "abort after this combat";
			}
			return "abort";
		}

		if ( action.startsWith( "consult" ) )
		{
			return action;
		}

		if ( action.startsWith( "custom" ) )
		{
			return "custom combat script";
		}

		if ( action.startsWith( "delevel" ) )
		{
			return "delevel and plink";
		}

		if ( action.startsWith( "twiddle" ) )
		{
			return "twiddle your thumbs";
		}

		if ( action.indexOf( "run" ) != -1 && action.indexOf( "away" ) != -1 )
		{
			Matcher runAwayMatcher = CombatActionManager.TRY_TO_RUN_AWAY_PATTERN.matcher( action );

			int runaway = 0;

			if ( runAwayMatcher.find() )
			{
				runaway = StringUtilities.parseInt( runAwayMatcher.group( 1 ) );
			}

			if ( runaway <= 0 )
			{
				return "try to run away";
			}

			return "run away if " + runaway + "% chance of being free";
		}

		if ( action.startsWith( "combo " ) )
		{
			String combo = DiscoCombatHelper.disambiguateCombo( action.substring( 6 ) );
			if ( combo == null )
			{
				return "note unknown " + action;
			}
			return "combo " + combo;
		}

		if ( action.startsWith( "item" ) || action.startsWith( "use " ) )
		{
			String item = CombatActionManager.getLongItemAction( action.substring( 4 ).trim() );
			return item.startsWith( "attack" ) ? item : "item " + item;
		}

		if ( action.startsWith( "skill" ) )
		{
			String potentialSkill = SkillDatabase.getCombatSkillName( action.substring( 5 ).trim() );
			if ( potentialSkill != null )
			{
				return "skill " + potentialSkill.toLowerCase();
			}
			else
			{
				return "note unknown/ambiguous " + action;
			}
		}

		// Well, it's either a standard skill, or it's an item,
		// or it's something you need to lookup in the tables.

		String potentialSkill = SkillDatabase.getCombatSkillName( action );
		if ( potentialSkill != null )
		{
			return "skill " + potentialSkill.toLowerCase();
		}

		String item = CombatActionManager.getLongItemAction( action );
		return item.startsWith( "attack" ) ? item : "item " + item;
	}

	private static final String getLongItemAction( final String action )
	{
		int commaIndex = action.indexOf( "," );
		if ( commaIndex != -1 )
		{
			String first = CombatActionManager.getLongItemAction( action.substring( 0, commaIndex ) );
			if ( first.startsWith( "attack" ) )
			{
				return CombatActionManager.getLongItemAction( action.substring( commaIndex + 1 ).trim() );
			}

			String second = CombatActionManager.getLongItemAction( action.substring( commaIndex + 1 ).trim() );
			if ( second.startsWith( "attack" ) )
			{
				return first;
			}

			return first + ", " + second;
		}

		if ( action.startsWith( "item" ) )
		{
			return CombatActionManager.getLongItemAction( action.substring( 4 ).trim() );
		}

		int itemId = CombatActionManager.getCombatItem( action );

		if ( itemId <= 0 )
		{
			return "attack with weapon";
		}

		return ItemDatabase.getItemName( itemId );
	}

	public static final String getShortCombatOptionName( String action )
	{
		if ( action == null )
		{
			return "attack";
		}

		if ( action.startsWith( "consult" ) )
		{
			return action;
		}

		if ( action.equals( "default" ) )
		{
			return "default";
		}

		action = action.trim();

		if ( isMacroAction( action ) )
		{
			return action;
		}

		boolean isSkillNumber = true;
		for ( int i = 0; i < action.length() && isSkillNumber; ++i )
		{
			isSkillNumber = Character.isDigit( action.charAt( i ) );
		}

		if ( isSkillNumber )
		{
			return action;
		}

		if ( action.startsWith( "attack" ) || action.length() == 0 )
		{
			return "attack";
		}

		if ( action.startsWith( "abort" ) )
		{
			if ( action.indexOf( "after" ) != -1 )
			{
				return "abort after";
			}
			return "abort";
		}

		if ( action.contains( "pick" ) ||
		     ( action.contains( "steal" ) && !action.contains( "stealth" ) &&
		      !action.contains( "combo" ) && !action.contains( "accordion" ) ) )
		{
			return "steal";
		}

		if ( action.startsWith( "jiggle" ) )
		{
			return "jiggle";
		}

		if ( action.startsWith( "special" ) )
		{
			return "special";
		}

		if ( action.equals( "skip" ) || action.startsWith( "note" ) )
		{
			return "skip";
		}

		if ( action.startsWith( "summon" ) && action.endsWith( "ghost" ) )
		{
			return "summon ghost";
		}

		if ( action.startsWith( "consult" ) )
		{
			return action;
		}

		if ( action.startsWith( "custom" ) )
		{
			return "custom";
		}

		if ( action.startsWith( "delevel" ) )
		{
			return "delevel";
		}

		if ( action.startsWith( "twiddle" ) )
		{
			return "twiddle";
		}

		if ( action.indexOf( "run" ) != -1 && action.indexOf( "away" ) != -1 )
		{
			Matcher runAwayMatcher = CombatActionManager.TRY_TO_RUN_AWAY_PATTERN.matcher( action );
			int runaway = runAwayMatcher.find() ? StringUtilities.parseInt( runAwayMatcher.group( 1 ) ) : 0;
			return runaway <= 0 ? "runaway" : ( "runaway" + runaway );
		}

		if ( action.startsWith( "combo " ) )
		{
			String name = action.substring( 6 );
			String combo = DiscoCombatHelper.disambiguateCombo( name );
			if ( combo == null )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Invalid combo '" + name + "' requested" );
				Macrofier.resetMacroOverride();
				return "skip";
			}
			return "combo " + combo;
		}

		if ( action.startsWith( "item" ) )
		{
			return CombatActionManager.getShortItemAction( action.substring( 4 ).trim() );
		}

		if ( action.startsWith( "skill" ) )
		{
			String name = SkillDatabase.getCombatSkillName( action.substring( 5 ).trim() );
			return name == null ? "attack" : "skill" + SkillDatabase.getSkillId( name );
		}

		String potentialSkill = SkillDatabase.getCombatSkillName( action );
		if ( potentialSkill != null )
		{
			return "skill" + SkillDatabase.getSkillId( potentialSkill );
		}

		return CombatActionManager.getShortItemAction( action );
	}

	private static final String getShortItemAction( final String action )
	{
		int commaIndex = action.indexOf( "," );
		if ( commaIndex != -1 )
		{
			String first = CombatActionManager.getShortItemAction( action.substring( 0, commaIndex ) );
			if ( first.startsWith( "attack" ) )
			{
				return CombatActionManager.getShortItemAction( action.substring( commaIndex + 1 ).trim() );
			}

			String second = CombatActionManager.getShortItemAction( action.substring( commaIndex + 1 ).trim() );
			if ( second.startsWith( "attack" ) )
			{
				return first;
			}

			return first + "," + second;
		}

		if ( action.startsWith( "item" ) )
		{
			return CombatActionManager.getShortItemAction( action.substring( 4 ) );
		}

		int itemId = CombatActionManager.getCombatItem( action );

		if ( itemId <= 0 )
		{
			return "attack";
		}

		if ( itemId == ItemPool.DICTIONARY && InventoryManager.getCount( ItemPool.DICTIONARY ) < 1 )
		{
			itemId = ItemPool.FACSIMILE_DICTIONARY;
		}

		if ( itemId == ItemPool.FACSIMILE_DICTIONARY && InventoryManager.getCount( ItemPool.FACSIMILE_DICTIONARY ) < 1 )
		{
			itemId = ItemPool.DICTIONARY;
		}

		return String.valueOf( itemId );
	}

	public static final int getCombatItem( String action )
	{
		List matchingNames = ItemDatabase.getMatchingNames( action );
		int count = matchingNames.size();

		for ( int i = 0; i < count; ++i )
		{
			String name = (String) matchingNames.get( i );
			int id = ItemDatabase.getItemId( name );
			if ( ItemDatabase.getAttribute( id, ItemDatabase.ATTR_COMBAT | ItemDatabase.ATTR_COMBAT_REUSABLE ) )
			{
				return id;
			}
		}

		return -1;
	}
}
