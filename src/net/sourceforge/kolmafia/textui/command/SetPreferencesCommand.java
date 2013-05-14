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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.combat.CombatActionManager;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.webui.StationaryButtonDecorator;

public class SetPreferencesCommand
	extends AbstractCommand
{
	public SetPreferencesCommand()
	{
		this.usage = " <preference> [ = <value> ] - show/change preference settings";
		this.flags = KoLmafiaCLI.FULL_LINE_CMD;
	}

	@Override
	public void run( final String cmd, final String parameters )
	{
		int splitIndex = parameters.indexOf( "=" );
		if ( splitIndex == -1 )
		{
			// Allow reading of system properties

			if ( parameters.startsWith( "System." ) )
			{
				RequestLogger.printLine( System.getProperty( parameters.substring( 7 ) ) );
			}
			else if ( Preferences.isUserEditable( parameters ) )
			{
				RequestLogger.printLine( Preferences.getString( parameters ) );
			}

			return;
		}

		String name = parameters.substring( 0, splitIndex ).trim();
		if ( !Preferences.isUserEditable( name ) )
		{
			return;
		}

		String value = parameters.substring( splitIndex + 1 ).trim();
		if ( value.startsWith( "\"" ) )
		{
			value = value.substring( 1, value.endsWith( "\"" ) ? value.length() - 1 : value.length() );
		}

		while ( value.endsWith( ";" ) )
		{
			value = value.substring( 0, value.length() - 1 ).trim();
		}

		if ( name.equals( "battleAction" ) )
		{
			if ( value.indexOf( ";" ) != -1 || value.startsWith( "consult" ) )
			{
				CombatActionManager.setDefaultAction( value );
				value = "custom combat script";
			}
			else
			{
				value = CombatActionManager.getLongCombatOptionName( value );
			}

			// Special handling of the battle action property,
			// such that auto-recovery gets reset as needed.

			if ( name.equals( "battleAction" ) && value != null )
			{
				KoLCharacter.getBattleSkillNames().setSelectedItem( value );
			}
		}

		if ( name.equals( "customCombatScript" ) )
		{
			ChangeCombatScriptCommand.update( value );
			return;
		}

		if ( name.startsWith( "combatHotkey" ) )
		{
			String desiredValue = CombatActionManager.getLongCombatOptionName( value );

			if ( !desiredValue.startsWith( "attack" ) || value.startsWith( "attack" ) )
			{
				value = desiredValue;
			}
		}

		if ( name.equals( "_userMods" ) )
		{
			Modifiers.overrideModifier( "_userMods", value );
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
		}

		if ( Preferences.getString( name ).equals( value ) )
		{
			return;
		}

		// suppress CLI output iff it is a pref that starts with _ AND is defined in defaults.txt
		if ( !name.startsWith( "_" ) || Preferences.containsDefault( name ) )
			RequestLogger.printLine( name + " => " + value );
		Preferences.setString( name, value );

		if ( name.startsWith( "combatHotkey" ) )
		{
			StationaryButtonDecorator.reloadCombatHotkeyMap();
		}
	}
}
