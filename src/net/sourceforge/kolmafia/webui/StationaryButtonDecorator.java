/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.webui;

import java.util.ArrayList;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;

import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

public class StationaryButtonDecorator
{
	private static final ArrayList combatHotkeys = new ArrayList();

	public static final void decorate( final String urlString, final StringBuffer buffer )
	{
		if ( Preferences.getBoolean( "hideServerDebugText" ) )
		{
			int beginDebug = buffer.indexOf( "<div style='max-height" );
			if ( beginDebug != -1 )
			{
				int endDebug = buffer.indexOf( "</div>", beginDebug ) + 6;
				buffer.delete( beginDebug, endDebug );
			}
		}

		if ( Preferences.getBoolean( "serverAddsCustomCombat" ) )
		{
			int imageIndex = buffer.indexOf( "<td><img src='http://images.kingdomofloathing.com/itemimages/book3.gif' id='skills'>" );
			if ( imageIndex != -1 )
			{
				boolean again = FightRequest.getCurrentRound() == 0;
				String location = again ? getAdventureAgainLocation( buffer ) : "fight.php?action=custom";

				buffer.insert( imageIndex, "<td><a href='" + location + "'><img src='http://images.kingdomofloathing.com/itemimages/plexpock.gif'></td><td class=spacer></td>" );
				imageIndex = buffer.indexOf( "<tr class=label>", imageIndex ) + 16;

				buffer.insert( imageIndex, again ? "<td>again</td><td></td>" : "<td>script</td><td></td>" );
			}

			return;
		}

		if ( !Preferences.getBoolean( "relayAddsCustomCombat" ) )
		{
			return;
		}

		int insertionPoint = buffer.indexOf( "<tr" );
		if ( insertionPoint != -1 )
		{
			StringBuffer actionBuffer = new StringBuffer();

			actionBuffer.append( "<tr><td>" );
			actionBuffer.append( "<table width=\"100%\"><tr><td align=left>" );

			StationaryButtonDecorator.addFightButton( urlString, buffer, actionBuffer, "attack", true );

			if ( KoLCharacter.isMoxieClass() )
			{
				StationaryButtonDecorator.addFightButton(
					urlString, buffer, actionBuffer, "steal", FightRequest.getCurrentRound() == 1 );
			}

			if ( EquipmentManager.usingChefstaff() )
			{
				StationaryButtonDecorator.addFightButton(
					urlString, buffer, actionBuffer, "jiggle", FightRequest.getCurrentRound() > 0 );
			}

			if ( KoLCharacter.hasSkill( "Entangling Noodles" ) )
			{
				StationaryButtonDecorator.addFightButton(
					urlString, buffer, actionBuffer, "3004", FightRequest.getCurrentRound() > 0 );
			}

			StationaryButtonDecorator.addFightButton(
				urlString, buffer, actionBuffer, "script", FightRequest.getCurrentRound() > 0 );

			for ( int i = 1; i <= KoLConstants.STATIONARY_BUTTON_COUNT; ++i )
			{
				String action = Preferences.getString( "stationaryButton" + i );
				if ( action.equals( "" ) || action.equals( "none" ) )
				{
					continue;
				}

				String name = SkillDatabase.getSkillName( Integer.parseInt( action ) );

				if ( !KoLCharacter.hasSkill( name ) )
				{
					for ( int j = i; j < 5; ++j )
					{
						Preferences.setString(
							"stationaryButton" + j, Preferences.getString( "stationaryButton" + ( j + 1 ) ) );
					}

					Preferences.setString( "stationaryButton5", "" );
					continue;
				}

				StationaryButtonDecorator.addFightButton(
					urlString, buffer, actionBuffer, action, FightRequest.getCurrentRound() > 0 );
			}

			if ( StationaryButtonDecorator.combatHotkeys.isEmpty() )
			{
				StationaryButtonDecorator.reloadCombatHotkeyMap();
			}

			actionBuffer.append( "</td><td align=right>" );
			actionBuffer.append( "<select id=\"hotkeyViewer\" onchange=\"updateCombatHotkey();\">" );

			actionBuffer.append( "<option>- update hotkeys -</option>" );

			for ( int i = 0; i < StationaryButtonDecorator.combatHotkeys.size(); ++i )
			{
				actionBuffer.append( "<option>" );
				actionBuffer.append( i );
				actionBuffer.append( ": " );

				actionBuffer.append( StationaryButtonDecorator.combatHotkeys.get( i ) );
				actionBuffer.append( "</option>" );
			}

			actionBuffer.append( "</select>" );

			actionBuffer.append( "</td></tr></table>" );
			actionBuffer.append( "</td></tr>" );
			buffer.insert( insertionPoint, actionBuffer.toString() );

			StringUtilities.singleStringReplace( buffer, "</head>", "<script src=\"/hotkeys.js\"></script></head>" );
			StringUtilities.singleStringReplace(
				buffer,
				"<body",
				"<body onkeyup=\"handleCombatHotkey(event,false);\" onkeydown=\"handleCombatHotkey(event,true);\" " );
		}
	}

	private static final void addFightButton( final String urlString, final StringBuffer response,
		final StringBuffer buffer, final String action, boolean isEnabled )
	{
		boolean forceFocus = action.equals( "attack" );

		String name = StationaryButtonDecorator.getActionName( action );
		buffer.append( "<input type=\"button\" onClick=\"document.location.href='" );

		if ( urlString.startsWith( "choice.php" ) && response.indexOf( "choice.php" ) != -1 )
		{
			if ( forceFocus )
			{
				name = "auto";
			}

			buffer.append( "choice.php?action=auto" );
		}
		else if ( FightRequest.getCurrentRound() == 0 )
		{
			String location = getAdventureAgainLocation( response );
			buffer.append( location );
			isEnabled &= !location.equals( "main.php" );
		}
		else
		{
			buffer.append( "fight.php?" );

			if ( action.equals( "script" ) )
			{
				buffer.append( "action=" );

				if ( urlString.endsWith( "action=script" ) && isEnabled )
				{
					name = "abort";
					buffer.append( "abort" );
				}
				else
				{
					buffer.append( "custom" );
				}
			}
			else if ( action.equals( "attack" ) || action.equals( "steal" ) )
			{
				buffer.append( "action=" );
				buffer.append( action );
			}
			else if ( action.equals( "jiggle" ) )
			{
				buffer.append( "action=chefstaff" );
				isEnabled &= !FightRequest.alreadyJiggled();
			}
			else
			{
				buffer.append( "action=skill&whichskill=" );
				buffer.append( action );
				isEnabled &=
					SkillDatabase.getMPConsumptionById( StringUtilities.parseInt( action ) ) <= KoLCharacter.getCurrentMP();
			}
		}

		buffer.append( "'; void(0);\" value=\"" );
		buffer.append( name );

		buffer.append( "\"" );

		if ( forceFocus )
		{
			buffer.append( " id=\"defaultButton\"" );
		}

		if ( isEnabled )
		{
			buffer.append( ">&nbsp;" );
		}
		else
		{
			buffer.append( " disabled>&nbsp;" );
		}
	}

	private static final String getAdventureAgainLocation( StringBuffer response )
	{
		String location = "main.php";
		String monster = FightRequest.getLastMonsterName();

		if ( monster.equals( "giant sandworm" ) )
		{
			AdventureResult drumMachine = ItemPool.get( UseItemRequest.DRUM_MACHINE, 1 );
			if ( KoLConstants.inventory.contains( drumMachine ) )
			{
				location = "inv_use.php?pwd=" + GenericRequest.passwordHash + "&which=3&whichitem=" + UseItemRequest.DRUM_MACHINE;
			}
			else
			{
				location = "adventure.php?snarfblat=122";
			}
		}
		else if ( monster.equals( "scary pirate" ) )
		{
			location = "inv_use.php?pwd=" + GenericRequest.passwordHash +"&which=3&whichitem=" + UseItemRequest.CURSED_PIECE_OF_THIRTEEN;
		}
		else
		{
			int startIndex = response.indexOf( "<a href=\"" );
			if ( startIndex != -1 )
			{
				location = response.substring( startIndex + 9, response.indexOf( "\"", startIndex + 10 ) );
			}
		}

		return location;
	}

	private static final String getActionName( final String action )
	{
		if ( action.equals( "attack" ) )
		{
			return FightRequest.getCurrentRound() == 0 ? "again" : "attack";
		}

		if ( action.equals( "steal" ) || action.equals( "jiggle" ) || action.equals( "script" ) )
		{
			return action;
		}

		int skillId = StringUtilities.parseInt( action );
		String name = SkillDatabase.getSkillName( skillId ).toLowerCase();

		switch ( skillId )
		{
		case 15: // CLEESH
		case 7002: // Shake Hands
		case 7003: // Hot Breath
		case 7004: // Cold Breath
		case 7005: // Spooky Breath
		case 7006: // Stinky Breath
		case 7007: // Sleazy Breath
			name = StringUtilities.globalStringDelete( name, " " );
			break;

		case 7001: // Give In To Your Vampiric Urges
			name = "bakula";
			break;

		case 7008: // Moxious Maneuver
			name = "moxman";
			break;

		case 7010: // red bottle-rocket
		case 7011: // blue bottle-rocket
		case 7012: // orange bottle-rocket
		case 7013: // purple bottle-rocket
		case 7014: // black bottle-rocket
			name = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( name, "fire " ), "bottle-" );
			break;

		case 2103: // Head + Knee Combo
		case 2105: // Head + Shield Combo
		case 2106: // Knee + Shield Combo
		case 2107: // Head + Knee + Shield Combo
			name = name.substring( 0, name.length() - 6 );
			break;

		case 1003: // thrust-smack
			name = "thrust";
			break;

		case 1004: // lunge-smack
		case 1005: // lunging thrust-smack
			name = "lunge";
			break;

		case 2: // Chronic Indigestion
		case 7009: // Magic Missile
		case 3004: // Entangling Noodles
		case 3009: // Lasagna Bandages
		case 3019: // Fearful Fettucini
		case 19: // Transcendent Olfaction
			name = name.substring( name.lastIndexOf( " " ) + 1 );
			break;

		case 3003: // Minor Ray of Something
		case 3005: // eXtreme Ray of Something
		case 3007: // Cone of Whatever
		case 3008: // Weapon of the Pastalord
		case 3020: // Spaghetti Spear
		case 4003: // Stream of Sauce
		case 4009: // Wave of Sauce
		case 5019: // Tango of Terror
			name = name.substring( 0, name.indexOf( " " ) );
			break;

		case 5003: // Disco Eye-Poke
		case 5012: // Disco Face Stab
			name = StringUtilities.globalStringDelete( StringUtilities.globalStringDelete( name.substring( 6 ), "-" ), " " );
			break;

		case 5005: // Disco Dance of Doom
			name = "dance1";
			break;

		case 5008: // Disco Dance II: Electric Boogaloo
			name = "dance2";
			break;
		}

		return name;
	}

	public static final void reloadCombatHotkeyMap()
	{
		StationaryButtonDecorator.combatHotkeys.clear();

		for ( int i = 0; i <= 9; ++i )
		{
			StationaryButtonDecorator.combatHotkeys.add( Preferences.getString( "combatHotkey" + i ) );
		}
	}
}
