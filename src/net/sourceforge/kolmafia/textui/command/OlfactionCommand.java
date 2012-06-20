/**
 * Copyright (c) 2005-2012, KoLmafia development team
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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.preferences.Preferences;

public class OlfactionCommand
	extends AbstractCommand
{
	public OlfactionCommand()
	{
		this.usage =
			" ( none | monster <name> | [item] <list> | goals ) [abort] - tag next monster [that drops all items in list, or your goals].";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		String pref = cmd.equals( "putty" ) ? "autoPutty" : "autoOlfact";
		parameters = parameters.toLowerCase();
		if ( parameters.equals( "none" ) )
		{
			Preferences.setString( pref, "" );
		}
		else if ( !parameters.equals( "" ) )
		{
			boolean isAbort = false, isItem = false, isMonster = false;
			boolean isGoals = false;
			if ( parameters.endsWith( " abort" ) )
			{
				isAbort = true;
				parameters = parameters.substring( 0, parameters.length() - 6 ).trim();
			}
			if ( parameters.startsWith( "item " ) )
			{
				parameters = parameters.substring( 5 ).trim();
			}
			else if ( parameters.startsWith( "monster " ) )
			{
				isMonster = true;
				parameters = parameters.substring( 8 ).trim();
			}
			else if ( parameters.equals( "goals" ) )
			{
				isGoals = true;
			}
			StringBuffer result = new StringBuffer();
			if ( isGoals )
			{
				result.append( "goals" );
			}
			if ( !isGoals && !isMonster )
			{
				Object[] items = ItemFinder.getMatchingItemList( KoLConstants.inventory, parameters );
				if ( items != null && items.length > 0 )
				{
					result.append( "item " );
					for ( int i = 0; i < items.length; ++i )
					{
						if ( i != 0 )
						{
							result.append( ", " );
						}
						result.append( ( (AdventureResult) items[ i ] ).getName() );
					}
					isItem = true;
				}
			}
			if ( !isGoals && !isItem && parameters.length() >= 1 )
			{
				result.append( "monster " );
				result.append( parameters );
				isMonster = true;
			}
			if ( !isGoals && !isItem && !isMonster )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Unable to interpret your conditions!" );
				return;
			}

			if ( isAbort )
			{
				result.append( " abort" );
			}
			Preferences.setString( pref, result.toString() );
		}
		String option = Preferences.getString( pref );
		if ( option.equals( "" ) )
		{
			KoLmafia.updateDisplay( pref + " is disabled." );
		}
		else
		{
			KoLmafia.updateDisplay( pref + ": " + option.replaceFirst(
				"^goals", "first monster that can drop your remaining goals" ).replaceFirst(
				" abort$", ", and then abort adventuring" ) );
		}
	}
}
