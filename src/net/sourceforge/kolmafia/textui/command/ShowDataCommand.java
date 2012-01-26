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

import java.io.File;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.dev.spellcast.utilities.UtilityConstants;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.LogStream;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;

import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.swingui.CalendarFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShowDataCommand
	extends AbstractCommand
{
	public ShowDataCommand()
	{
		this.usage = " [<param>] - list indicated type of data, possibly filtered by param.";
	}

	public void run( final String cmd, final String parameters )
	{
		ShowDataCommand.show( cmd + " " + parameters );
	}

	public static void show( final String parameters )
	{
		ShowDataCommand.show( parameters, false );
	}

	/**
	 * A special module used specifically for properly printing out data relevant to the current session.
	 */

	public static void show( String parameters, final boolean sessionPrint )
	{
		if ( parameters.length() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Print what?" );
			return;
		}

		parameters = parameters.trim();
		int spaceIndex = parameters.indexOf( " " );

		String list = spaceIndex == -1 ? parameters : parameters.substring( 0, spaceIndex ).trim();
		String filter =
			spaceIndex == -1 ? "" : StringUtilities.getCanonicalName( parameters.substring( spaceIndex ).trim() );

		PrintStream desiredOutputStream = sessionPrint ? RequestLogger.getSessionStream() : RequestLogger.INSTANCE;

		if ( !filter.equals( "" ) && ( parameters.startsWith( "summary" ) || parameters.startsWith( "session" ) || parameters.equals( "status" ) || parameters.startsWith( "equip" ) || parameters.startsWith( "encounters" ) || parameters.startsWith( "locations" ) ) )
		{
			desiredOutputStream = LogStream.openStream( new File( UtilityConstants.ROOT_LOCATION, filter ), false );
			filter = "";
		}

		ShowDataCommand.show( list, filter, desiredOutputStream );

		if ( sessionPrint && RequestLogger.isDebugging() )
		{
			ShowDataCommand.show( list, filter, RequestLogger.getDebugStream() );
		}

		if ( !sessionPrint )
		{
			desiredOutputStream.close();
		}
	}

	/**
	 * A special module used specifically for properly printing out data relevant to the current session. This method is
	 * more specialized than its counterpart and is used when the data to be printed is known, as well as the stream to
	 * print to. Usually called by its counterpart to handle specific instances.
	 */

	private static void show( final String desiredData, String filter, final PrintStream desiredStream )
	{
		desiredStream.println();

		if ( desiredData.startsWith( "moon" ) )
		{
			Date today = new Date();

			desiredStream.println( CalendarFrame.LONG_FORMAT.format( today ) + " - " + HolidayDatabase.getCalendarDayAsString( today ) );
			desiredStream.println();

			desiredStream.println( "Ronald: " + HolidayDatabase.getRonaldPhaseAsString() );
			desiredStream.println( "Grimace: " + HolidayDatabase.getGrimacePhaseAsString() );
			desiredStream.println( "Mini-moon: " + HolidayDatabase.getHamburglarPositionAsString() );
			desiredStream.println();

			String[] holidayPredictions = HolidayDatabase.getHolidayPredictions( today );
			for ( int i = 0; i < holidayPredictions.length; ++i )
			{
				desiredStream.println( holidayPredictions[ i ] );
			}

			desiredStream.println();
			desiredStream.println( HolidayDatabase.getHoliday( today ) );
			desiredStream.println( HolidayDatabase.getMoonEffect() );
			desiredStream.println();
			return;
		}

		if ( desiredData.equals( "session" ) )
		{
			desiredStream.println( "Player: " + KoLCharacter.getUserName() );
			desiredStream.println( "Session Id: " + GenericRequest.serverCookie );
			desiredStream.println( "Password Hash: " + GenericRequest.passwordHash );
			desiredStream.println( "Current Server: " + GenericRequest.KOL_HOST );
			desiredStream.println();
			return;
		}

		if ( desiredData.equals( "status" ) )
		{
			desiredStream.println( "Name: " + KoLCharacter.getUserName() );
			desiredStream.println( "Class: " + KoLCharacter.getClassType() );
			desiredStream.println();

			desiredStream.println( "Lv: " + KoLCharacter.getLevel() );
			desiredStream.println( "HP: " + KoLCharacter.getCurrentHP() + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumHP() ) );
			desiredStream.println( "MP: " + KoLCharacter.getCurrentMP() + " / " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getMaximumMP() ) );

			desiredStream.println();

			desiredStream.println( "Mus: " + ShowDataCommand.getStatString(
				KoLCharacter.getBaseMuscle(), KoLCharacter.getAdjustedMuscle(), KoLCharacter.getMuscleTNP() ) );
			desiredStream.println( "Mys: " + ShowDataCommand.getStatString(
				KoLCharacter.getBaseMysticality(), KoLCharacter.getAdjustedMysticality(),
				KoLCharacter.getMysticalityTNP() ) );
			desiredStream.println( "Mox: " + ShowDataCommand.getStatString(
				KoLCharacter.getBaseMoxie(), KoLCharacter.getAdjustedMoxie(), KoLCharacter.getMoxieTNP() ) );

			desiredStream.println();

			desiredStream.println( "Advs: " + KoLCharacter.getAdventuresLeft() );
			desiredStream.println( "Meat: " + KoLConstants.COMMA_FORMAT.format( KoLCharacter.getAvailableMeat() ) );
			desiredStream.println( "Drunk: " + KoLCharacter.getInebriety() );

			desiredStream.println();
			return;
		}

		if ( desiredData.equals( "modifiers" ) )
		{
			desiredStream.println( "ML: " + KoLConstants.MODIFIER_FORMAT.format( KoLCharacter.getMonsterLevelAdjustment() ) );
			desiredStream.println( "Enc: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getCombatRateAdjustment() ) + "%" );
			desiredStream.println( "Init: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getInitiativeAdjustment() ) + "%" );

			desiredStream.println();

			desiredStream.println( "Exp: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getExperienceAdjustment() ) );
			desiredStream.println( "Meat: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getMeatDropPercentAdjustment() ) + "%" );
			desiredStream.println( "Item: " + KoLConstants.ROUNDED_MODIFIER_FORMAT.format( KoLCharacter.getItemDropPercentAdjustment() ) + "%" );

			desiredStream.println();
			return;
		}

		if ( desiredData.startsWith( "equip" ) )
		{
			AdventureResult hat = EquipmentManager.getEquipment( EquipmentManager.HAT );
			desiredStream.println( "Hat: " + hat );
			if ( hat.getItemId() == ItemPool.HATSEAT )
			{
				desiredStream.println( "Carrying: " + KoLCharacter.getEnthroned() );
			}
			desiredStream.println( "Weapon: " + EquipmentManager.getEquipment( EquipmentManager.WEAPON ) );

			if ( EquipmentManager.getFakeHands() > 0 )
			{
				desiredStream.println( "Fake Hands: " + EquipmentManager.getFakeHands() );
			}

			desiredStream.println( "Off-hand: " + EquipmentManager.getEquipment( EquipmentManager.OFFHAND ) );
			desiredStream.println( "Shirt: " + EquipmentManager.getEquipment( EquipmentManager.SHIRT ) );
			desiredStream.println( "Pants: " + EquipmentManager.getEquipment( EquipmentManager.PANTS ) );

			AdventureResult container = EquipmentManager.getEquipment( EquipmentManager.CONTAINER );
			if ( container != EquipmentRequest.UNEQUIP )
			{
				desiredStream.println( "Container: " + container );
			}

			desiredStream.println();

			desiredStream.println( "Acc. 1: " + EquipmentManager.getEquipment( EquipmentManager.ACCESSORY1 ) );
			desiredStream.println( "Acc. 2: " + EquipmentManager.getEquipment( EquipmentManager.ACCESSORY2 ) );
			desiredStream.println( "Acc. 3: " + EquipmentManager.getEquipment( EquipmentManager.ACCESSORY3 ) );

			desiredStream.println();

			desiredStream.println( "Pet: " + KoLCharacter.getFamiliar() );
			desiredStream.println( "Item: " + EquipmentManager.getFamiliarItem() +
				(EquipmentManager.familiarItemLocked() ? " (locked)" : "") );
			AdventureResult st1 = EquipmentManager.getEquipment( EquipmentManager.STICKER1 );
			AdventureResult st2 = EquipmentManager.getEquipment( EquipmentManager.STICKER2 );
			AdventureResult st3 = EquipmentManager.getEquipment( EquipmentManager.STICKER3 );
			if ( st1 != EquipmentRequest.UNEQUIP || st2 != EquipmentRequest.UNEQUIP || st3 != EquipmentRequest.UNEQUIP )
			{
				desiredStream.println();
				desiredStream.println( "Sticker 1: " + ShowDataCommand.getStickerText(
					st1, EquipmentManager.getTurns( EquipmentManager.STICKER1 ) ) );
				desiredStream.println( "Sticker 2: " + ShowDataCommand.getStickerText(
					st2, EquipmentManager.getTurns( EquipmentManager.STICKER2 ) ) );
				desiredStream.println( "Sticker 3: " + ShowDataCommand.getStickerText(
					st3, EquipmentManager.getTurns( EquipmentManager.STICKER3 ) ) );
			}
			return;
		}

		if ( desiredData.equals( "encounters" ) )
		{
			desiredStream.println( "Encounter Listing: " );

			desiredStream.println();
			RequestLogger.printList( KoLConstants.encounterList, desiredStream );
			return;
		}

		if ( desiredData.equals( "locations" ) )
		{
			desiredStream.println( "Visited Locations: " );
			desiredStream.println();

			RequestLogger.printList( KoLConstants.adventureList, desiredStream );
			return;
		}

		if ( desiredData.equals( "counters" ) )
		{
			desiredStream.println( UseItemRequest.lastSemirareMessage() );
			desiredStream.println();

			String counters = TurnCounter.getUnexpiredCounters();
			if ( counters.equals( "" ) )
			{
				desiredStream.println( "No active counters." );
			}
			else
			{
				desiredStream.println( "Unexpired counters:" );
				desiredStream.println( counters );
			}
			desiredStream.println();
			return;
		}

		List mainList =
			desiredData.equals( "closet" ) ? KoLConstants.closet :
			desiredData.equals( "summary" ) ? KoLConstants.tally :
			desiredData.equals( "storage" ) ? KoLConstants.storage :
			desiredData.equals( "display" ) ? KoLConstants.collection :
			desiredData.equals( "outfits" ) ? EquipmentManager.getOutfits() :
			desiredData.equals( "familiars" ) ? KoLCharacter.getFamiliarList() :
			KoLConstants.inventory;

		if ( desiredData.equals( "effects" ) )
		{
			mainList = KoLConstants.activeEffects;
			AdventureResult[] effects = new AdventureResult[ mainList.size() ];
			mainList.toArray( effects );

			int nBuffs = 0;

			for ( int i = 0; i < effects.length; ++i )
			{
				String skillName = UneffectRequest.effectToSkill( effects[ i ].getName() );
				if ( SkillDatabase.contains( skillName ) )
				{
					int skillId = SkillDatabase.getSkillId( skillName );
					if ( skillId > 6000 && skillId < 7000 )
					{
						++nBuffs;
					}
				}
			}

			desiredStream.println( nBuffs + " of " + UseSkillRequest.songLimit() + " AT buffs active." );
		}

		if ( desiredData.startsWith( "skills" ) )
		{
			mainList = KoLConstants.availableSkills;
			filter = filter.toLowerCase();

			if ( filter.startsWith( "cast" ) )
			{
				mainList = new ArrayList();
				mainList.addAll( KoLConstants.availableSkills );

				List intersect = SkillDatabase.getSkillsByType( SkillDatabase.CASTABLE );
				mainList.retainAll( intersect );
				filter = "";
			}

			if ( filter.startsWith( "pass" ) )
			{
				mainList = new ArrayList();
				mainList.addAll( KoLConstants.availableSkills );

				List intersect = SkillDatabase.getSkillsByType( SkillDatabase.PASSIVE );
				mainList.retainAll( intersect );
				filter = "";
			}

			if ( filter.startsWith( "self" ) )
			{
				mainList = new ArrayList();
				mainList.addAll( KoLConstants.availableSkills );

				List intersect = SkillDatabase.getSkillsByType( SkillDatabase.SELF_ONLY );
				mainList.retainAll( intersect );
				filter = "";
			}

			if ( filter.startsWith( "buff" ) )
			{
				mainList = new ArrayList();
				mainList.addAll( KoLConstants.availableSkills );

				List intersect = SkillDatabase.getSkillsByType( SkillDatabase.BUFF );
				mainList.retainAll( intersect );
				filter = "";
			}

			if ( filter.startsWith( "combat" ) )
			{
				mainList = new ArrayList();
				mainList.addAll( KoLConstants.availableSkills );

				List intersect = SkillDatabase.getSkillsByType( SkillDatabase.COMBAT );
				mainList.retainAll( intersect );
				filter = "";
			}
		}

		if ( filter.equals( "" ) )
		{
			RequestLogger.printList( mainList, desiredStream );
			return;
		}

		String currentItem;
		List resultList = new ArrayList();

		Object[] items = new Object[ mainList.size() ];
		mainList.toArray( items );
		Matcher m = Pattern.compile( "&lt;.*?&gt;" ).matcher( "" );

		for ( int i = 0; i < items.length; ++i )
		{
			currentItem = StringUtilities.getCanonicalName( items[ i ].toString() );
			if ( currentItem.indexOf( filter ) != -1 ||
				m.reset( currentItem ).replaceAll( "" ).indexOf( filter ) != -1 )
			{
				resultList.add( items[ i ] );
			}
		}

		RequestLogger.printList( resultList, desiredStream );
	}

	private static final String getStatString( final int base, final int adjusted, final int tnp )
	{
		StringBuffer statString = new StringBuffer();
		statString.append( KoLConstants.COMMA_FORMAT.format( adjusted ) );

		if ( base != adjusted )
		{
			statString.append( " (" + KoLConstants.COMMA_FORMAT.format( base ) + ")" );
		}

		statString.append( ", tnp = " );
		statString.append( KoLConstants.COMMA_FORMAT.format( tnp ) );

		return statString.toString();
	}

	private static final String getStickerText( AdventureResult item, final int turns )
	{
		if ( !item.equals( EquipmentRequest.UNEQUIP ) )
		{
			item = item.getInstance( turns );
		}
		return item.toString();
	}
}
