/**
 * Copyright (c) 2005-2014, KoLmafia development team
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
import java.io.IOException;
import java.io.PrintStream;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.kolmafia.MonsterData;

import net.sourceforge.kolmafia.persistence.MonsterDatabase;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CustomCombatLookup
	extends DefaultMutableTreeNode
{
	private List childKeys = new LinkedList();
	private Map childLookup = new TreeMap();

	public CustomCombatLookup()
	{
		super( "", true );
	}

	public CustomCombatStrategy getStrategy( final String encounterKey )
	{
		return (CustomCombatStrategy) childLookup.get( encounterKey );
	}

	public String getBestEncounterKey( final String encounter )
	{
		MonsterData monsterData = MonsterDatabase.findMonster( encounter, false );

		String encounterKey = getLongestMatch( encounter, monsterData );

		if ( encounterKey != null )
		{
			return encounterKey;
		}

		// If no matches were found, then see if there is a match
		// against the adventure location.

		String location = Preferences.getString( "lastAdventure" ).toLowerCase();

		if ( monsterData == null )
		{
			// An unrecognized monster is likely to be:
			// * something new that the player may want to see personally,
			//	so allow an [unrecognized] section to match them.
			// * something fundamentally different from the known monsters
			//	in the current zone, so don't try to use a [zone name] match.

			location = "unrecognized";
		}

		encounterKey = getLongestMatch( location, monsterData );

		if ( encounterKey != null )
		{
			return encounterKey;
		}

		return "default";
	}

	private String getLongestMatch( final String encounter, MonsterData monsterData )
	{
		String longestMatch = null;
		int longestMatchLength = 0;

		for ( int i = 0; i < childKeys.size(); ++i )
		{
			CombatEncounterKey childKey = (CombatEncounterKey) childKeys.get( i );

			if ( childKey.matches( encounter, monsterData ) )
			{
				String childName = childKey.toString();

				if ( childName.length() > longestMatchLength )
				{
					longestMatch = childKey.toString();
					longestMatchLength = childName.length();
				}
			}
		}

		return longestMatch;
	}

	public void clearEncounterKey( final String encounterKey )
	{
		Iterator strategyIterator = childLookup.values().iterator();

		while ( strategyIterator.hasNext() )
		{
			CustomCombatStrategy strategy = (CustomCombatStrategy) strategyIterator.next();

			if ( strategy.getName().equals( encounterKey ) )
			{
				strategy.removeAllChildren();
			}
			else
			{
				strategy.resetActionCount();
			}
		}
	}

	public void addEncounterKey( String encounterKey )
	{
		if ( childLookup.containsKey( encounterKey ) )
		{
			CustomCombatStrategy strategy = (CustomCombatStrategy) childLookup.get( encounterKey );

			strategy.removeAllChildren();
		}
		else
		{
			CombatEncounterKey combatEncounterKey = new CombatEncounterKey( encounterKey );

			encounterKey = combatEncounterKey.toString();

			CustomCombatStrategy strategy = new CustomCombatStrategy( encounterKey );

			childKeys.add( combatEncounterKey );
			childLookup.put( encounterKey, strategy );

			super.add( strategy );
		}
	}

	public void addEncounterAction( final String encounterKey, final int roundIndex, final String indent,
		final String combatAction, boolean isMacro )
	{
		CustomCombatStrategy strategy = (CustomCombatStrategy) childLookup.get( encounterKey );

		if ( roundIndex < 0 )
		{
			strategy.addCombatAction( strategy.getChildCount() + 1, indent, combatAction, isMacro );
		}
		else
		{
			strategy.addCombatAction( roundIndex, indent, combatAction, isMacro );
		}
	}

	@Override
	public void removeAllChildren()
	{
		childKeys.clear();
		childLookup.clear();

		super.removeAllChildren();
	}

	@Override
	public String toString()
	{
		return "";
	}

	public void load( BufferedReader reader )
		throws IOException
	{
		StringBuffer indent = new StringBuffer();
		String line = null;

		String encounterKey = "default";

		while ( ( line = reader.readLine() ) != null )
		{
			// Skip over any lines with no content.

			line = line.trim();

			if ( line.length() == 0 )
			{
				continue;
			}

			// If we've reached a new encounter key, close out the previous action.

			if ( line.startsWith( "[" ) )
			{
				CustomCombatStrategy strategy = getStrategy( encounterKey );

				if ( strategy.getChildCount() == 0 )
				{
					strategy.addCombatAction( 1, "attack", indent.toString(), false );
				}

				encounterKey = CombatActionManager.encounterKey( line.substring( 1, line.length() - 1 ) );
				addEncounterKey( encounterKey );

				indent.setLength( 0 );
				continue;
			}

			// Check to see if it's a macro action, and create a different kind of node if it is.

			if ( CombatActionManager.isMacroAction( line ) )
			{
				// Strip out leading and trailing quotes.

				if ( line.charAt( 0 ) == '"' )
				{
					line = line.substring( 1 ).trim();

					if ( line.length() == 0 )
					{
						continue;
					}

					if ( line.charAt( line.length() - 1 ) == '"' )
					{
						line = line.substring( 0, line.length() - 1 );
					}
				}

				// Update indentation.

				if ( line.startsWith( "end" ) && indent.length() > 0 )
				{
					indent.delete( 0, 4 );
				}

				addEncounterAction( encounterKey, -1, indent.toString(), line, true );

				// Update indentation again.

				if ( line.startsWith( "if" ) || line.startsWith( "while" ) || line.startsWith( "sub" ) )
				{
					indent.append( "\u0020\u0020\u0020\u0020" );
				}

				continue;
			}

			// If we're currently building a macro, just append the current data to that macro.

			int roundIndex = -1;

			// If it looks like this is a KoLmafia-created settings file,
			// then strip out the round index information.

			if ( Character.isDigit( line.charAt( 0 ) ) )
			{
				int colonIndex = line.indexOf( ":" );
				if ( colonIndex != -1 )
				{
					roundIndex = StringUtilities.parseInt( line.substring( 0, colonIndex ) );
					line = line.substring( colonIndex + 1 ).trim();
				}
			}

			addEncounterAction( encounterKey, roundIndex, indent.toString(), line, false );
		}

		// Make sure that the action is properly closed out.

		CustomCombatStrategy strategy = getStrategy( encounterKey );

		if ( strategy.getChildCount() == 0 )
		{
			strategy.addCombatAction( 1, "attack", indent.toString(), false );
		}
	}

	public void store( PrintStream writer )
	{
		Iterator strategyIterator = childLookup.values().iterator();

		while ( strategyIterator.hasNext() )
		{
			CustomCombatStrategy strategy = (CustomCombatStrategy) strategyIterator.next();

			strategy.store( writer );
		}

	}
}