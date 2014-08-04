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

package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.TavernRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.WumpusManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;

public class QuestManager
{
	private static final Pattern ORE_PATTERN = Pattern.compile( "(asbestos|linoleum|chrome) ore[\\. ]" );
	private static final Pattern BATHOLE_PATTERN = Pattern.compile( "bathole_(\\d)\\.gif" );
	private static final Pattern DRAWER_PATTERN = Pattern.compile( "search through <b>(\\d+)</b> drawers" );
	private static final Pattern LIGHTER_PATTERN = Pattern.compile( "group of (\\d+) nearby protesters do the same" );
	private static final Pattern TACO_FISH_PATTERN = Pattern.compile( "gain (\\d+) taco fish meat" );

	public static final void handleQuestChange( final String location, final String responseText )
	{
		if ( location.startsWith( "adventure" ) )
		{
			if ( location.contains( AdventurePool.ROAD_TO_WHITE_CITADEL_ID ) )
			{
				handleWhiteCitadelChange( responseText );
			}
			if ( location.contains( AdventurePool.WHITEYS_GROVE_ID ) )
			{
				handleWhiteysGroveChange( responseText );
			}
			else if ( location.contains( AdventurePool.EXTREME_SLOPE_ID ) )
			{
				handleExtremityChange( responseText );
			}
			else if ( location.contains( AdventurePool.AIRSHIP_ID ) ||
					  location.contains( AdventurePool.CASTLE_BASEMENT_ID ) ||
					  location.contains( AdventurePool.CASTLE_GROUND_ID ) ||
					  location.contains( AdventurePool.CASTLE_TOP_ID ) )
			{
				handleBeanstalkChange( location, responseText );
			}
			else if ( location.contains( AdventurePool.ZEPPELIN_PROTESTORS_ID ) )
			{
				handleZeppelinMobChange( responseText );
			}
			else if ( location.contains( AdventurePool.RED_ZEPPELIN_ID ) )
			{
				handleZeppelinChange( responseText );
			}
			else if ( location.contains( AdventurePool.PALINDOME_ID ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.PALINDOME, QuestDatabase.STARTED );
			}
			else if ( location.contains( AdventurePool.HAUNTED_BALLROOM_ID ) )
			{
				handleManorSecondFloorChange( location, responseText );
			}
			else if ( location.contains( AdventurePool.UPPER_CHAMBER_ID ) ||
			          location.contains( AdventurePool.MIDDLE_CHAMBER_ID ) )
			{
				handlePyramidChange( location, responseText );
			}
			else if ( location.contains( AdventurePool.SLOPPY_SECONDS_DINER_ID ) ||
			          location.contains( AdventurePool.FUN_GUY_MANSION_ID ) ||
			          location.contains( AdventurePool.YACHT_ID ) )
			{
				handleAirportChange( location, responseText );
			}
			else if ( location.contains( AdventurePool.MARINARA_TRENCH_ID ) ||
			          location.contains( AdventurePool.ANENOME_MINE_ID ) ||
			          location.contains( AdventurePool.DIVE_BAR_ID ) ||
			          location.contains( AdventurePool.MERKIN_OUTPOST_ID ) ||
			          location.contains( AdventurePool.CALIGINOUS_ABYSS_ID ) )
			{
				handleSeaChange( location, responseText );
			}
			else if ( KoLCharacter.getInebriety() > 25 )
			{
				handleSneakyPeteChange( responseText );
			}
		}
		if ( location.startsWith( "beanstalk" ) )
		{
			if ( responseText.contains( "airship.gif" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.GARBAGE, "step1" );
				KoLCharacter.armBeanstalk();
			}
		}
		else if ( location.startsWith( "barrel" ) )
		{
			BarrelDecorator.parseResponse( location, responseText );
		}
		else if ( location.startsWith( "canadia" ) )
		{
			handleCanadiaChange( location, responseText );
		}
		else if ( location.startsWith( "cobbsknob.php" ) )
		{
			if ( location.indexOf( "action=cell37" ) != -1 )
			{
				handleCell37( responseText );
			}
		}
		else if ( location.startsWith( "council" ) )
		{
			handleCouncilChange( responseText );
		}
		else if ( location.startsWith( "friars" ) )
		{
			handleFriarsChange( responseText );
		}
		else if ( location.startsWith( "guild" ) )
		{
			handleGuildChange( responseText );
		}
		else if ( location.contains( "whichplace=highlands" ) ||
			location.contains( AdventurePool.ABOO_PEAK_ID ) ||
			location.contains( AdventurePool.OIL_PEAK_ID )	)
		{
			handleHighlandsChange( location, responseText );
		}
		else if ( location.startsWith( "inv_use" ) )
		{
			if ( location.contains( "whichitem=" + ItemPool.AWOL_COMMENDATION ) )
			{
				AWOLQuartermasterRequest.parseResponse( location, responseText );
			}
			else if ( location.contains( "whichitem=" + ItemPool.BURT ) )
			{
				BURTRequest.parseResponse( location, responseText );
			}
		}
		else if ( location.startsWith( "lair" ) )
		{
			SorceressLairManager.handleQuestChange( location, responseText );
		}
		else if ( location.startsWith( "manor" ) )
		{
			handleManorFirstFloorChange( responseText );
		}
		else if ( location.startsWith( "monkeycastle" ) )
		{
			handleSeaChange( location, responseText );
		}
		else if ( location.startsWith( "pandamonium" ) )
		{
			// Quest starts the very instant you click on pandamonium.php
			QuestDatabase.setQuestIfBetter( Quest.AZAZEL, QuestDatabase.STARTED );
		}
		else if ( location.startsWith( "place.php" ) )
		{
			if ( location.contains( "whichplace=airport" ) || location.contains( "whichplace=airport_sleaze" ) )
			{
				handleAirportChange( location, responseText );
			}
			else if ( location.contains( "whichplace=desertbeach" ) )
			{
				if ( location.contains( "action=db_pyramid1" ) )
				{
					handlePyramidChange( location, responseText );
				}
				else
				{
					handleBeachChange( responseText );
				}
			}
			else if ( location.contains( "whichplace=manor1" ) )
			{
				handleManorFirstFloorChange( responseText );
			}
			else if ( location.contains( "whichplace=manor2" ) )
			{
				handleManorSecondFloorChange( location, responseText );
			}
			else if ( location.contains( "whichplace=manor3" ) )
			{
				// If here at all, Necklace and Dance quests are complete and second floor open
				QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED );
				QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED );
				// Legacy code support
				Preferences.setInteger( "lastSecondFloorUnlock", KoLCharacter.getAscensions() );
			}
			else if ( location.contains( "whichplace=manor4" ) )
			{
				// If here at all, Necklace and Dance quests are complete and second floor and basement open
				QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED );		
				QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED );		
				QuestDatabase.setQuestIfBetter( Quest.MANOR, "step1" );
				// Legacy code support
				Preferences.setInteger( "lastSecondFloorUnlock", KoLCharacter.getAscensions() );
			}
			else if ( location.contains( "whichplace=marais" ) )
			{
				handleMaraisChange( responseText );
			}
			else if ( location.contains( "whichplace=mclargehuge" ) )
			{
				if ( location.contains( "action=trappercabin" ) )
				{
					handleTrapperChange( responseText );
				}
				else if ( location.contains( "action=cloudypeak" ) )
				{
					handleMcLargehugeChange( responseText );
				}
			}
			else if ( location.contains( "whichplace=orc_chasm" ) )
			{
				handleChasmChange( responseText );
			}
			else if ( location.contains( "whichplace=palindome" ) )
			{
				handlePalindomeChange( location, responseText );
			}
			else if ( location.contains( "whichplace=plains" ) )
			{
				handlePlainsChange( responseText );
			}
			else if ( location.contains( "whichplace=pyramid" ) )
			{
				handlePyramidChange( location, responseText );
			}
			else if ( location.contains( "whichplace=sea_oldman" ) )
			{
				handleSeaChange( location, responseText );
			}
			else if ( location.contains( "whichplace=zeppelin" ) )
			{
				if ( responseText.contains( "zep_mob1.gif" ) )
				{
					QuestDatabase.setQuestIfBetter( Quest.RON, "step2" );
				}
			}
		}
		else if ( location.startsWith( "questlog" ) )
		{
			QuestLogRequest.registerQuests( false, location, responseText );
		}
		else if ( location.startsWith( "seafloor" ) )
		{
			handleSeaChange( location, responseText );
		}
		else if ( location.startsWith( "showplayer" ) )
		{
			ProfileRequest.parseResponse( location, responseText );
		}
		else if ( location.startsWith( "tavern" ) )
		{
			TavernManager.handleTavernChange( responseText );
		}
		else if ( location.startsWith( "town" ) )
		{
			handleTownChange( responseText );
		}
		else if ( location.startsWith( "trickortreat" ) )
		{
			handleTrickOrTreatingChange( responseText );
		}
		else if ( location.startsWith( "woods" ) )
		{
			handleWoodsChange( responseText );
		}
		else if ( location.startsWith( "bathole" ) )
		{
			handleBatholeChange( responseText );
		}
		// Obsolete. Sigh.
		else if ( location.startsWith( "generate15" ) )
		{
			// You slide the last tile into place ...

			if ( AdventureRequest.registerDemonName( "Strange Cube", responseText ) || responseText.indexOf( "slide the last tile" ) != -1 )
			{
				ResultProcessor.processItem( ItemPool.STRANGE_CUBE, -1 );
			}
		}
	}

	private static void handleMcLargehugeChange( String responseText )
	{
		if ( responseText.contains( "you spy a crude stone staircase" ) || responseText.contains( "notice a set of crude carved stairs" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step3" );
			Preferences.setInteger( "currentExtremity", 0 );			
		}
	}

	private static void handleTownChange( String responseText )
	{
		if ( responseText.contains( "town_tower" ) )
		{
			if ( !Preferences.getBoolean( "timeTowerAvailable" ) )
			{
				Preferences.setBoolean( "timeTowerAvailable", true );
				ConcoctionDatabase.setRefreshNeeded( false );
			}
		}
		else
		{
			if ( Preferences.getBoolean( "timeTowerAvailable" ) )
			{
				Preferences.setBoolean( "timeTowerAvailable", false );
				ConcoctionDatabase.setRefreshNeeded( false );
			}
		}
	}

	private static void handleGuildChange( final String responseText )
	{
		if ( responseText.contains( "South of the Border" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.MEATCAR, QuestDatabase.FINISHED );
		}
		if ( responseText.contains( "White Citadel near Whitey's Grove" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.CITADEL, QuestDatabase.STARTED );
		}
	}

	private static void handleWhiteysGroveChange( final String responseText )
	{
		if ( responseText.contains( "It's A Sign!" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step1" );
		}
	}

	private static void handleWhiteCitadelChange( final String responseText )
	{
		if ( responseText.contains( "Cheetahs Never Lose" ) )
		{
			if ( responseText.contains( "further down the Road to the White Citadel" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step3" );
			}
			else
			{
				QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step2" );
			}
		}
		if ( responseText.contains( "Summer Holiday" ) )
		{
			if ( responseText.contains( "unpack the hang glider" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step5" );
			}
			else
			{
				QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step4" );
			}
		}
	}

	private static void handleManorFirstFloorChange( final String responseText )
	{
		// Derive quest status from available rooms
		if ( responseText.contains( "snarfblat=" + AdventurePool.HAUNTED_KITCHEN_ID ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED );
		}
		if ( responseText.contains( "whichplace=manor2" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED );
			// Legacy code support
			Preferences.setInteger( "lastSecondFloorUnlock", KoLCharacter.getAscensions() );
		}
		if ( responseText.contains( "whichplace=manor4" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.MANOR, "step1" );
		}
	}

	private static void handleManorSecondFloorChange( final String location, final String responseText )
	{
		if ( location.contains( "action=manor2_ladys" ) )
		{
			if ( responseText.contains( "just want to dance" ) )
			{
				QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_DANCE, "step1" );
			}
		}
		if ( location.contains( AdventurePool.HAUNTED_BALLROOM_ID ) )
		{
			if ( responseText.contains( "Having a Ball in the Ballroom" ) )
			{
				QuestDatabase.setQuestProgress( Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED );
			}
		}
		// Derive quest status from available rooms
		if ( responseText.contains( "snarfblat=" + AdventurePool.HAUNTED_BATHROOM_ID ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_DANCE, "step1" );
		}
		if ( responseText.contains( "snarfblat=" + AdventurePool.HAUNTED_BALLROOM_ID ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_DANCE, "step3" );
		}
		if ( responseText.contains( "whichplace=manor3" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED );
		}
		// If here at all, Necklace quest is complete
		QuestDatabase.setQuestIfBetter( Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED );
		// Legacy code support
		Preferences.setInteger( "lastSecondFloorUnlock", KoLCharacter.getAscensions() );
	}

	public static final void handlePyramidChange( final String location, final String responseText )
	{
		if ( location.contains( "action=db_pyramid1" ) )
		{
			// Unlock Pyramid
			if ( responseText.contains( "the model bursts into flames and is quickly consumed" ) )
			{
				QuestDatabase.setQuestProgress( Quest.PYRAMID, QuestDatabase.STARTED );
			}
		}
		else if ( location.contains( AdventurePool.UPPER_CHAMBER_ID ) )
		{
			if ( responseText.contains( "Down Dooby-Doo Down Down" ) )
			{
				// Open Middle Chamber
				Preferences.setBoolean( "middleChamberUnlock", true );
				QuestDatabase.setQuestProgress( Quest.PYRAMID, "step1" );
			}
		}
		else if ( location.contains( AdventurePool.MIDDLE_CHAMBER_ID ) )
		{
			if ( responseText.contains( "Further Down Dooby-Doo Down Down" ) )
			{
				// Open Lower Chamber
				Preferences.setBoolean( "lowerChamberUnlock", true );
				QuestDatabase.setQuestProgress( Quest.PYRAMID, "step2" );
			}
			else if ( responseText.contains( "Under Control" ) )
			{
				// Open Control Room
				Preferences.setBoolean( "controlRoomUnlock", true );
				QuestDatabase.setQuestProgress( Quest.PYRAMID, "step3" );
			}
			// If here, must have unlocked middle chamber
			Preferences.setBoolean( "middleChamberUnlock", true );
			QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step1" );
		}
		else if ( location.contains( "whichplace=pyramid" ) )
		{
			// Verify settings based on images displayed, in case above steps were missed
			QuestDatabase.setQuestIfBetter( Quest.PYRAMID, QuestDatabase.STARTED );
			if ( responseText.contains( "pyramid_middle.gif" ) )
			{
				Preferences.setBoolean( "middleChamberUnlock", true );
				QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step1" );
			}
			if ( responseText.contains( "pyramid_bottom" ) )
			{
				Preferences.setBoolean( "lowerChamberUnlock", true );
				QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step2" );
			}
			if ( responseText.contains( "pyramid_controlroom.gif" ) )
			{
				Preferences.setBoolean( "controlRoomUnlock", true );
				QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step3" );
			}
		}
		return;
	}

	public static final void handleAirportChange( final String location, final String responseText )
	{
		// Don't bother if it's always open
		if ( Preferences.getBoolean( "sleazeAirportAlways" ) )
		{
			return;
		}
		// Detect if Airport is open today
		if ( location.contains( AdventurePool.FUN_GUY_MANSION_ID ) || location.contains( AdventurePool.SLOPPY_SECONDS_DINER_ID ) ||
		     location.contains( AdventurePool.YACHT_ID ) || location.contains( "whichplace=airport_sleaze" ) )
		{
			Preferences.setBoolean( "_sleazeAirportToday", true );
		}
		else if ( location.contains( "whichplace=airport" ) )
		{
			if ( responseText.contains( "whichplace=airport_sleaze" ) )
			{
				Preferences.setBoolean( "_sleazeAirportToday", true );
			}
		}
		return;
	}

	private static void handleWoodsChange( final String responseText )
	{
		if ( responseText.contains( "wcroad.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step1" );
		}

		// If we see the Hidden Temple, mark it as unlocked
		if ( responseText.contains( "otherimages/woods/temple.gif" ) )
		{
			Preferences.setInteger( "lastTempleUnlock", KoLCharacter.getAscensions() );
		}

		// If we see the Black Market, update Black Market quest
		if ( responseText.contains( "blackmarket.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.BLACK, "step2" );
			QuestDatabase.setQuestIfBetter( Quest.MACGUFFIN, "step1" );
			Preferences.setInteger( "blackForestProgress", 5 );
		}

		// If we see the link to the empty Black Market, Wu Tang has been defeated
		if ( responseText.indexOf( "action=emptybm" ) != -1 )
		{
			Preferences.setInteger( "lastWuTangDefeated", KoLCharacter.getAscensions() );
		}
	}

	private static void handleBatholeChange( final String responseText )
	{
		Matcher m = BATHOLE_PATTERN.matcher( responseText );

		if ( !m.find() )
		{
			return;
		}

		int image = StringUtilities.parseInt( m.group( 1 ) );
		String status = "";

		if ( image == 1 )
		{
			status = QuestDatabase.STARTED;
		}
		else if ( image == 2 )
		{
			status = "step1";
		}
		else if ( image == 3 )
		{
			status = "step2";
		}
		else if ( image == 4 )
		{
			status = "step3";
		}

		QuestDatabase.setQuestIfBetter( Quest.BAT, status );
	}

	private static final void handleSneakyPeteChange( final String responseText )
	{
		if ( responseText.indexOf( "You hand him your button and take his glowstick" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.NOVELTY_BUTTON );
			return;
		}

		if ( responseText.indexOf( "Ah, man, you dropped your crown back there!" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.TATTERED_PAPER_CROWN );
			return;
		}
	}

	private static final void handleTrickOrTreatingChange( final String responseText )
	{
		if ( responseText.contains( "pull the pumpkin off of your head" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.PUMPKINHEAD_MASK );
			return;
		}
		if ( responseText.contains( "gick all over your mummy costume" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.MUMMY_COSTUME );
			return;
		}
		if ( responseText.contains( "unzipping the mask and throwing it behind you" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.WOLFMAN_MASK );
			return;
		}
		if ( responseText.contains( "Right on, brah. Here, have some gum." ) )
		{
			ResultProcessor.processItem( ItemPool.RUSSIAN_ICE, -1 );
			return;
		}
	}

	private static final void handleCell37( final String responseText )
	{
		// You pass the folder through the little barred window, and hear Subject 37 flipping through the pages
		if ( responseText.contains( "pass the folder through" ) )
		{
			ResultProcessor.processItem( ItemPool.SUBJECT_37_FILE, -1 );
		}
		// You pass the GOTO through the window, and Subject 37 thanks you.
		if ( responseText.contains( "pass the GOTO through" ) )
		{
			ResultProcessor.processItem( ItemPool.GOTO, -1 );
		}
		// You pass the little vial of of weremoose spit through the window.
		if ( responseText.contains( "pass the little vial" ) )
		{
			ResultProcessor.processItem( ItemPool.WEREMOOSE_SPIT, -1 );
		}
		// You hand Subject 37 the glob of abominable blubber.
		if ( responseText.contains( "hand Subject 37 the glob" ) )
		{
			ResultProcessor.processItem( ItemPool.ABOMINABLE_BLUBBER, -1 );
		}
	}

	private static final void handleFriarsChange( final String responseText )
	{
		// "Thank you, Adventurer."

		if ( responseText.indexOf( "Thank you" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.DODECAGRAM, -1 );
			ResultProcessor.processItem( ItemPool.CANDLES, -1 );
			ResultProcessor.processItem( ItemPool.BUTTERKNIFE, -1 );
			int knownAscensions = Preferences.getInteger( "knownAscensions" );
			Preferences.setInteger( "lastFriarCeremonyAscension", knownAscensions );
			QuestDatabase.setQuestProgress( Quest.FRIAR, QuestDatabase.FINISHED );
			if ( KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "Taint cleansed." );
			}
		}
	}

	private static final void handleChasmChange( final String responseText )
	{
		if ( responseText.contains( "Huzzah!  The bridge is finished!" ) )
		{
			ResultProcessor.processItem(
				ItemPool.MORNINGWOOD_PLANK, -1 * InventoryManager.getCount( ItemPool.MORNINGWOOD_PLANK ) );
			ResultProcessor.processItem(
				ItemPool.HARDWOOD_PLANK, -1 * InventoryManager.getCount( ItemPool.HARDWOOD_PLANK ) );
			ResultProcessor.processItem(
				ItemPool.WEIRDWOOD_PLANK, -1 * InventoryManager.getCount( ItemPool.WEIRDWOOD_PLANK ) );
			ResultProcessor.processItem( ItemPool.THICK_CAULK, -1 * InventoryManager.getCount( ItemPool.THICK_CAULK ) );
			ResultProcessor.processItem( ItemPool.LONG_SCREW, -1 * InventoryManager.getCount( ItemPool.LONG_SCREW ) );
			ResultProcessor.processItem( ItemPool.BUTT_JOINT, -1 * InventoryManager.getCount( ItemPool.BUTT_JOINT ) );
			if ( KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "You have bridged the Orc Chasm." );
			}
			QuestDatabase.setQuestProgress( Quest.TOPPING, "step1" );
		}
	}

	private static final void handleHighlandsChange( final String location, final String responseText )
	{
		if ( location.contains( "action=highlands_dude" ) && responseText.contains( "trying to, like, order a pizza" ) )
		{
			QuestDatabase.setQuestProgress( Quest.TOPPING, "step2" );
		}
		if ( location.contains( AdventurePool.ABOO_PEAK_ID ) && responseText.contains( "Come On Ghosty, Light My Pyre" ) ||
			responseText.contains( "orcchasm/fire1.gif" ) )
		{
			Preferences.setInteger( "booPeakProgress", 0 );
		}
		if ( responseText.contains( "orcchasm/fire2.gif" ) )
		{
			Preferences.setInteger( "twinPeakProgress", 15 );
		}
		if ( location.contains( AdventurePool.OIL_PEAK_ID ) && responseText.contains( "Unimpressed with Pressure" ) ||
			responseText.contains( "orcchasm/fire3.gif" ) )
		{
			Preferences.setInteger( "oilPeakProgress", 0 );
		}
	}

	private static final void handleSeaChange( final String location, final String responseText )
	{
		if ( location.contains( "action=oldman_oldman" ) && responseText.contains( "have you found my boot yet?" ) )
		{
			QuestDatabase.setQuestProgress( Quest.SEA_OLD_GUY, QuestDatabase.STARTED );
		}
		// Little Brother
		else if ( location.contains( "who=1" ) )
		{
			if ( responseText.contains( "wish my big brother was here" ) )
			{
				QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, "step1" );
			}
			else if ( responseText.contains( "Wanna help me find Grandpa?" ) )
			{
				QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, "step4" );
			}
			else if ( responseText.contains( "he's been actin' awful weird lately" ) )
			{
				QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, "step10" );
			}
		}
		// Big Brother
		else if ( location.contains( "who=2" ) )
		{
			if ( responseText.contains( "I found this thing" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step11" );
			}
		}
		// Grandpa
		else if ( location.contains( "action=grandpastory" ) )
		{
			if ( responseText.contains( "bet those lousy Mer-kin up and kidnapped her" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step6" );
			}
			else if ( responseText.contains( "Gonna need one of them seahorses" ) )
			{
				Preferences.setBoolean( "corralUnlocked", true );
			}
		}
		else if ( location.contains( AdventurePool.MARINARA_TRENCH_ID ) && responseText.contains( "Show me what you've found, Old Timer" ) )
		{
			QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, "step5" );
		}
		else if ( location.contains( AdventurePool.ANENOME_MINE_ID ) && responseText.contains( "Sure, kid. I can teach you a thing or two" ) )
		{
			QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, "step5" );
		}
		else if ( location.contains( AdventurePool.DIVE_BAR_ID ) && 
				( responseText.contains( "What causes these things to form?" ) || responseText.contains( "what is that divine instrument?" ) ) )
		{
			QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, "step5" );
		}
		else if ( location.contains( AdventurePool.MERKIN_OUTPOST_ID ) && responseText.contains( "Phew, that was a close one" ) )
		{
			QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, "step9" );
			ConcoctionDatabase.setRefreshNeeded( false );
		}
		else if ( location.contains( AdventurePool.CALIGINOUS_ABYSS_ID) && responseText.contains( "I should get dinner on the table for the boys" ) )
		{
			QuestDatabase.setQuestProgress( Quest.SEA_MONKEES, QuestDatabase.FINISHED );
		}
		// Learn about quest progress if visiting sea floor
		else if ( location.startsWith( "seafloor" ) )
		{
			if ( responseText.contains( "abyss" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step12" );
			}
			else if ( responseText.contains( "outpost" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step6" );
			}
			else if ( responseText.contains( "mine" ) && KoLCharacter.isMuscleClass() )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step4" );
			}
			else if ( responseText.contains( "trench" ) && KoLCharacter.isMysticalityClass() )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step4" );
			}
			else if ( responseText.contains( "divebar" ) && KoLCharacter.isMoxieClass() )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step4" );
			}
			else if( responseText.contains( "shipwreck" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step1" );
			}
			if ( responseText.contains( "corral" ) )
			{
				Preferences.setBoolean( "corralUnlocked", true );
			}
		}
		// Learn about quest progress if visiting sea monkey castle
		else if ( location.startsWith( "monkeycastle" ) )
		{
			if ( responseText.contains( "who=4" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, QuestDatabase.FINISHED );
			}
			else if ( responseText.contains( "whichshop=grandma" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step9" );
			}
			else if ( responseText.contains( "who=3" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step5" );
			}
			else if ( responseText.contains( "who=2" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.SEA_MONKEES, "step2" );
			}
		}
	}

	private static final void handlePlainsChange( final String responseText )
	{
		// You stare at the pile of coffee grounds for a minute and it
		// occurs to you that maybe your grandma wasn't so crazy after
		// all. You pull out an enchanted bean and plop it into the
		// pile of grounds. It immediately grows into an enormous
		// beanstalk.

		if ( responseText.indexOf( "immediately grows into an enormous beanstalk" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.ENCHANTED_BEAN, -1 );
			QuestDatabase.setQuestProgress( Quest.GARBAGE, "step1" );
			QuestLogRequest.setBeanstalkPlanted();
			if ( KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "You have planted a beanstalk." );
			}
		}

		if ( responseText.contains( "dome.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, QuestDatabase.STARTED );
		}
	}

	public static final void handleBeanstalkChange( final String location, final String responseText )
	{
		// If you can adventure in areas, it tells us about quests
		if ( location.contains( AdventurePool.AIRSHIP_ID ) )
		{
			// Airship available
			QuestDatabase.setQuestIfBetter( Quest.GARBAGE, "step1" );
			if ( responseText.contains( "we're looking for the Four Immateria" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.GARBAGE, "step2" );
			}
		}
		else if ( location.contains( AdventurePool.CASTLE_BASEMENT_ID ) )
		{
			// Castle basement available
			QuestDatabase.setQuestIfBetter( Quest.GARBAGE, "step7" );
			if ( responseText.contains( "New Area Unlocked" ) && responseText.contains( "The Ground Floor" ) )
			{
				Preferences.setInteger( "lastCastleGroundUnlock", KoLCharacter.getAscensions() );
				QuestDatabase.setQuestProgress( Quest.GARBAGE, "step8" );
			}
		}
		else if ( location.contains( AdventurePool.CASTLE_GROUND_ID ) )
		{
			// Castle Ground floor available
			QuestDatabase.setQuestIfBetter( Quest.GARBAGE, "step8" );
			if ( responseText.contains( "New Area Unlocked" ) && responseText.contains( "The Top Floor" ) )
			{
				Preferences.setInteger( "lastCastleTopUnlock", KoLCharacter.getAscensions() );
				QuestDatabase.setQuestProgress( Quest.GARBAGE, "step9" );
			}
		}
		else if ( location.contains( AdventurePool.CASTLE_TOP_ID ) )
		{
			// Castle Top floor available
			QuestDatabase.setQuestIfBetter( Quest.GARBAGE, "step9" );
		}
	}

	private static final void handleZeppelinMobChange( final String responseText )
	{
		if ( responseText.contains( "mob has cleared out" ) )
		{
			QuestDatabase.setQuestProgress( Quest.RON, "step2" );
		}
		else
		{
			QuestDatabase.setQuestIfBetter( Quest.RON, "step1" );
		}
	}

	private static final void handleZeppelinChange( final String responseText )
	{
		if ( responseText.contains( "sneak aboard the Zeppelin" ) )
		{
			QuestDatabase.setQuestProgress( Quest.RON, "step3" );
		}
		else
		{
			QuestDatabase.setQuestIfBetter( Quest.RON, "step2" );
		}
	}

	private static final void handlePalindomeChange( final String location, final String responseText )
	{
		if ( location.contains( "action=pal_mrlabel" ) )
		{
			if ( responseText.contains( "in the mood for a bowl of wet stunt nut stew" ) )
			{
				QuestDatabase.setQuestProgress( Quest.PALINDOME, "step3" );
			}
		}
	}

	private static final void handleCanadiaChange( final String location, final String responseText )
	{
		if ( location.contains( "action=lc_marty" ) )
		{
			if ( responseText.contains( "All right, Marty, I'll see what I can do" ) )
			{
				QuestDatabase.setQuestProgress( Quest.SWAMP, QuestDatabase.STARTED );
			}
		}
	}

	private static final void handleMaraisChange( final String responseText )
	{
		// Detect unlocked areas
		if ( responseText.contains( "The Edge of the Swamp" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.SWAMP, QuestDatabase.STARTED );
		}
		if ( responseText.contains( "The Dark and Spooky Swamp" ) )
		{
			Preferences.setBoolean( "maraisDarkUnlock", true );
		}
		if ( responseText.contains( "The Wildlife Sanctuarrrrrgh" ) )
		{
			Preferences.setBoolean( "maraisWildlifeUnlock", true );
		}
		if ( responseText.contains( "The Corpse Bog" ) )
		{
			Preferences.setBoolean( "maraisCorpseUnlock", true );
		}
		if ( responseText.contains( "The Ruined Wizard Tower" ) )
		{
			Preferences.setBoolean( "maraisWizardUnlock", true );
		}
		if ( responseText.contains( "Swamp Beaver Territory" ) )
		{
			Preferences.setBoolean( "maraisBeaverUnlock", true );
		}
		if ( responseText.contains( "The Weird Swamp Village" ) )
		{
			Preferences.setBoolean( "maraisVillageUnlock", true );
		}
	}

	private static final Pattern EXP_PATTERN = Pattern.compile( "\\(([\\d]+)%explored\\)" );
	private static final void handleBeachChange( final String responseText )
	{
		String expString = ResponseTextParser.parseDivLabel( "db_l11desertlabel", responseText );
		Matcher matcher = QuestManager.EXP_PATTERN.matcher( expString );
		if ( matcher.find() )
		{
			int explored = StringUtilities.parseInt( matcher.group( 1 ) );
			QuestManager.setDesertExploration( explored );
		}
	}

	private static final void setDesertExploration( final int explored )
	{
		int current = Preferences.getInteger( "desertExploration" );
		QuestManager.setDesertExploration( current, explored - current );
	}

	public static final void incrementDesertExploration( final int increment )
	{
		int current = Preferences.getInteger( "desertExploration" );
		QuestManager.setDesertExploration( current, increment );
	}

	private static final void setDesertExploration( final int current, final int increment )
	{
		// If we've already registered complete desert exploration, we're done
		if ( current == 100 )
		{
			return;
		}

		// Peg new exploration percentage at 100
		int explored = Math.min( current + increment, 100 );

		// Save new exploration percentage
		Preferences.setInteger( "desertExploration", explored );

		// If we are done, update the quest
		if ( explored == 100 )
		{
			QuestDatabase.setQuestProgress( Quest.DESERT, QuestDatabase.FINISHED );
		}
	}

	private static final void handleTrapperChange( final String responseText )
	{
		Matcher oreMatcher = ORE_PATTERN.matcher( responseText );
		if ( oreMatcher.find() )
		{
			Preferences.setString( "trapperOre", oreMatcher.group( 1 ) + " ore" );
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step1" );
		}

		else if ( responseText.contains( "He takes the load of cheese and ore" ) )
		{
			AdventureResult item = new AdventureResult( Preferences.getString( "trapperOre" ), -3, false );
			ResultProcessor.processResult( item );
			ResultProcessor.processResult( new AdventureResult( "goat cheese", -3, false ) );
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step2" );
		}

		// Yeehaw!  I heard the noise and seen them mists dissapatin' from clear down here!  Ya done it!  Ya rightly done it!
		else if ( responseText.contains( "Yeehaw!  I heard the noise" ) )
		{
			Preferences.setInteger( "lastTr4pz0rQuest", KoLCharacter.getAscensions() );
			ResultProcessor.removeItem( ItemPool.GROARS_FUR );
			QuestDatabase.setQuestProgress( Quest.TRAPPER, QuestDatabase.FINISHED );
		}
	}

	private static final void handleExtremityChange( final String responseText )
	{
		if ( responseText.contains( "Discovering Your Extremity" ) ||
			responseText.contains( "2 eXXtreme 4 U" ) ||
			responseText.contains( "3 eXXXtreme 4ever 6pack" ) )
		{
			Preferences.increment( "currentExtremity" );
		}
	}

	private static final void handleCouncilChange( final String responseText )
	{
		Preferences.setInteger( "lastCouncilVisit", KoLCharacter.getLevel() );

		if ( responseText.indexOf( "500" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "mosquito larva", -1, false ) );
		}
		if ( responseText.indexOf( "batskin belt" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "Boss Bat bandana", -1, false ) );
		}
		if ( responseText.indexOf( "dragonbone belt buckle" ) != -1 )
		{
			ResultProcessor.processResult( new AdventureResult( "skull of the bonerdagon", -1, false ) );
		}
		QuestDatabase.handleCouncilText( responseText );
	}

	public static final void unlockGoatlet()
	{
		AdventureRequest goatlet = new AdventureRequest( "Goatlet", "adventure.php", AdventurePool.GOATLET_ID );

		if ( KoLCharacter.inFistcore() )
		{
			// You can actually get here without knowing Worldpunch
			// in Softcore by pulling ores.
			if ( !KoLCharacter.hasSkill( "Worldpunch" ) )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Try again after you learn Worldpunch." );
				return;
			}

			// If you don't have Earthen Fist active, get it.
			if ( !KoLConstants.activeEffects.contains( SorceressLairManager.EARTHEN_FIST ) )
			{
				UseSkillRequest request = UseSkillRequest.getInstance( "Worldpunch" );
				request.setBuffCount( 1 );
				RequestThread.postRequest( request );
			}

			// Perhaps you ran out of MP.
			if ( !KoLmafia.permitsContinue() )
			{
				KoLmafia.updateDisplay( MafiaState.ABORT, "Cast Worldpunch and try again." );
			}

			RequestThread.postRequest( goatlet );
			return;
		}

		if ( KoLCharacter.inAxecore() )
		{
			// No outfit needed; just take option #3
			RequestThread.postRequest( goatlet );
			return;
		}

		if ( !EquipmentManager.hasOutfit( OutfitPool.MINING_OUTFIT ) )
		{
			KoLmafia.updateDisplay( MafiaState.ABORT, "You need a mining outfit to continue." );
			return;
		}

		if ( EquipmentManager.isWearingOutfit( OutfitPool.MINING_OUTFIT ) )
		{
			RequestThread.postRequest( goatlet );
			return;
		}

		SpecialOutfit.createImplicitCheckpoint();
		( new EquipmentRequest( EquipmentDatabase.getOutfit( OutfitPool.MINING_OUTFIT ) ) ).run();
		RequestThread.postRequest( goatlet );
		SpecialOutfit.restoreImplicitCheckpoint();
	}

	/** After we win a fight, some quests may need to be updated.  Centralize handling for it here.
	 * @param responseText The text from (at least) the winning round of the fight
	 * @param monster The monster which <s>died</s>got beaten up.
	 */
	public static void updateQuestData( String responseText, String monster )
	{
		if ( monster.equalsIgnoreCase( "Screambat" ) )
		{
			if ( !QuestDatabase.isQuestLaterThan( Quest.BAT, "step2" ) )
			{
				QuestDatabase.advanceQuest( Quest.BAT );
			}
		}

		else if ( monster.equalsIgnoreCase( "Dirty Thieving Brigand" ) )
		{
			// "Well," you say, "it would really help the war effort if
			// your convent could serve as a hospital for our wounded
			// troops."
			if ( responseText.indexOf( "could serve as a hospital" ) != -1 )
			{
				Preferences.setString( "sidequestNunsCompleted", "hippy" );
			}
			else if ( responseText.indexOf( "could serve as a massage parlor" ) != -1 )
			{
				Preferences.setString( "sidequestNunsCompleted", "fratboy" );
			}
		}
		// oil slick: 6.34
		// oil tycoon: 19.02
		// oil baron: 31.7
		// oil cartel: 63.4
		// dress pants: 6.34
		else if ( monster.equalsIgnoreCase( "Oil Slick" ) )
		{
			double pantsBonus = InventoryManager.getEquippedCount( ItemPool.DRESS_PANTS ) > 0 ? 6.34 : 0;
			float current = Preferences.getFloat( "oilPeakProgress" );

			// normalize
			String setTo = String.format( "%.2f", Math.max( 0, current - 6.34 - pantsBonus ) );

			Preferences.setString( "oilPeakProgress", setTo );
		}
		else if ( monster.equalsIgnoreCase( "Oil Tycoon" ) )
		{
			double pantsBonus = InventoryManager.getEquippedCount( ItemPool.DRESS_PANTS ) > 0 ? 6.34 : 0;
			float current = Preferences.getFloat( "oilPeakProgress" );

			String setTo = String.format( "%.2f", Math.max( 0, current - 19.02 - pantsBonus ) );

			Preferences.setString( "oilPeakProgress", setTo );
		}
		else if ( monster.equalsIgnoreCase( "Oil Baron" ) )
		{
			double pantsBonus = InventoryManager.getEquippedCount( ItemPool.DRESS_PANTS ) > 0 ? 6.34 : 0;
			float current = Preferences.getFloat( "oilPeakProgress" );

			String setTo = String.format( "%.2f", Math.max( 0, current - 31.7 - pantsBonus ) );

			Preferences.setString( "oilPeakProgress", setTo );
		}
		else if ( monster.equalsIgnoreCase( "Oil Cartel" ) )
		{
			double pantsBonus = InventoryManager.getEquippedCount( ItemPool.DRESS_PANTS ) > 0 ? 6.34 : 0;
			float current = Preferences.getFloat( "oilPeakProgress" );

			String setTo = String.format( "%.2f", Math.max( 0, current - 63.4 - pantsBonus ) );

			Preferences.setString( "oilPeakProgress", setTo );
		}

		else if ( monster.equalsIgnoreCase( "Battlie Knight Ghost" ) ||
			monster.equalsIgnoreCase( "Claybender Sorcerer Ghost" ) ||
			monster.equalsIgnoreCase( "Dusken Raider Ghost" ) ||
			monster.equalsIgnoreCase( "Space Tourist Explorer Ghost" ) ||
			monster.equalsIgnoreCase( "Whatsian Commando Ghost" ) )
		{
			Preferences.decrement( "booPeakProgress", 2 );
		}

		else if ( monster.equalsIgnoreCase( "panicking Knott Yeti" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step4" );
		}
		else if ( monster.equalsIgnoreCase( "pygmy witch accountant" ) )
		{
			// If you don't have McClusky File (complete), or McClusky File 5, and accountant doesn't drop file, you must have unlocked office boss
			if ( InventoryManager.getCount( ItemPool.MCCLUSKY_FILE ) == 0 && InventoryManager.getCount( ItemPool.MCCLUSKY_FILE_PAGE5 ) == 0 &&
				Preferences.getInteger( "hiddenOfficeProgress" ) < 6 && responseText.indexOf( "McClusky file" ) == -1 )
			{
				Preferences.setInteger( "hiddenOfficeProgress", 6 );
			}
		}
		else if ( monster.equalsIgnoreCase( "Beer Batter" ) ||
			monster.equalsIgnoreCase( "best-selling novelist" ) ||
			monster.equalsIgnoreCase( "Big Meat Golem" ) ||
			monster.equalsIgnoreCase( "Bowling Cricket" ) ||
			monster.equalsIgnoreCase( "Bronze Chef" ) ||
			monster.equalsIgnoreCase( "concert pianist" ) ||
			monster.equalsIgnoreCase( "the darkness" ) ||
			monster.equalsIgnoreCase( "El Diablo" ) ||
			monster.equalsIgnoreCase( "Electron Submarine" ) ||
			monster.equalsIgnoreCase( "endangered inflatable white tiger" ) ||
			monster.equalsIgnoreCase( "fancy bath slug" ) ||
			monster.equalsIgnoreCase( "Fickle Finger of F8" ) ||
			monster.equalsIgnoreCase( "Flaming Samurai" ) ||
			monster.equalsIgnoreCase( "giant fried egg" ) ||
			monster.equalsIgnoreCase( "Giant Desktop Globe" ) ||
			monster.equalsIgnoreCase( "Ice Cube" ) ||
			monster.equalsIgnoreCase( "malevolent crop circle" ) ||
			monster.equalsIgnoreCase( "possessed pipe-organ" ) ||
			monster.equalsIgnoreCase( "Pretty Fly" ) ||
			monster.equalsIgnoreCase( "Tyrannosaurus Tex" ) ||
			monster.equalsIgnoreCase( "Vicious Easel" ) )
		{
			QuestDatabase.advanceQuest( Quest.FINAL );
		}
		else if ( monster.equalsIgnoreCase( "Your Shadow" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, "step13" );
		}
		else if ( monster.equalsIgnoreCase( "Clancy" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, "step15" );
		}
		else if ( monster.equalsIgnoreCase( "The Naughty Sorceress (3)" ) ||
			monster.equalsIgnoreCase( "The Avatar of Sneaky Pete" ) ||
			monster.equalsIgnoreCase( "The Avatar of Boris" ) ||
			monster.equalsIgnoreCase( "Principal Mooney" ) ||
			monster.equalsIgnoreCase( "Rene C. Corman" ) ||
			monster.equalsIgnoreCase( "The Avatar of Jarlsberg" ) ||
			responseText.contains( "Thwaitgold bee statuette" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, "step16" );
		}
		else if ( monster.equalsIgnoreCase( "Sloppy Seconds Burger" ) )
		{
			if ( responseText.contains( "You consult the list and grab the next ingredient" ) )
			{
				Preferences.increment( "buffJimmyIngredients", 1 );
				if ( Preferences.getInteger( "buffJimmyIngredients" ) >= 15 )
				{
					QuestDatabase.setQuestProgress( Quest.JIMMY_CHEESEBURGER, "step1" );
				}
			}
		}
		else if ( monster.equalsIgnoreCase( "Sloppy Seconds Cocktail" ) )
		{
			if ( responseText.contains( "cocktail sauce bottle" ) || responseText.contains( "defeated foe with your bottle" ) )
			{
				Preferences.increment( "tacoDanCocktailSauce", 1 );
				if ( Preferences.getInteger( "tacoDanCocktailSauce" ) >= 15 )
				{
					QuestDatabase.setQuestProgress( Quest.TACO_DAN_COCKTAIL, "step1" );
				}
			}
		}
		else if ( monster.equalsIgnoreCase( "Sloppy Seconds Sundae" ) )
		{
			if ( responseText.contains( "sprinkles off" ) )
			{
				Preferences.increment( "brodenSprinkles", 1 );
				if ( Preferences.getInteger( "brodenSprinkles" ) >= 15 )
				{
					QuestDatabase.setQuestProgress( Quest.BRODEN_SPRINKLES, "step1" );
				}
			}
		}
		else if ( monster.equalsIgnoreCase( "taco fish" ) )
		{
			Matcher FishMeatMatcher = QuestManager.TACO_FISH_PATTERN.matcher( responseText );
			if ( FishMeatMatcher.find() )
			{
				Preferences.increment( "tacoDanFishMeat", StringUtilities.parseInt( FishMeatMatcher.group( 1 ) ) );
				if ( Preferences.getInteger( "tacoDanFishMeat" ) >= 300 )
				{
					QuestDatabase.setQuestProgress( Quest.TACO_DAN_FISH, "step1" );
				}
			}
		}
		else if ( monster.equalsIgnoreCase( "Fun-Guy Playmate" ) )
		{
			if ( responseText.contains( "hot tub with some more bacteria" ) )
			{
				Preferences.increment( "brodenBacteria", 1 );
				if ( Preferences.getInteger( "brodenBacteria" ) >= 10 )
				{
					QuestDatabase.setQuestProgress( Quest.BRODEN_BACTERIA, "step1" );
				}
			}
		}
		else if ( monster.equalsIgnoreCase( "Wu Tang the Betrayer" ) )
		{
			Preferences.setInteger( "lastWuTangDefeated", KoLCharacter.getAscensions() );
		}
		else if ( monster.equalsIgnoreCase( "Baron Von Ratsworth" ) )
		{
			TavernRequest.addTavernLocation( '6' );
		}
		else if ( monster.equalsIgnoreCase( "Wumpus" ) )
		{
			WumpusManager.reset();
		}

		int adventure = KoLAdventure.lastAdventureId();

		switch ( adventure )
		{
		case AdventurePool.MERKIN_COLOSSEUM:
			// Do not increment round for wandering monsters
			if ( ( monster.equalsIgnoreCase( "Mer-kin balldodger" ) ||
			       monster.equalsIgnoreCase( "Mer-kin netdragger" ) ||
			       monster.equalsIgnoreCase( "Mer-kin bladeswitcher" ) ||
			       monster.equalsIgnoreCase( "Georgepaul, the Balldodger" ) ||
			       monster.equalsIgnoreCase( "Johnringo, the Netdragger" ) ||
			       monster.equalsIgnoreCase( "Ringogeorge, the Bladeswitcher" ) ) &&
			     // Do mark path chosen unless won round 15
			     ( Preferences.increment( "lastColosseumRoundWon", 1 ) == 15 ) )
			{
				Preferences.setString( "merkinQuestPath", "gladiator" );
			}
			break;

		case AdventurePool.THE_DAILY_DUNGEON:
			Preferences.increment( "_lastDailyDungeonRoom", 1 );
			break;

		case AdventurePool.ARID_DESERT:
			int explored = 1;
			if ( KoLCharacter.hasEquipped( ItemPool.UV_RESISTANT_COMPASS, EquipmentManager.OFFHAND ) )
			{
				explored += 1;
			}
			else if ( KoLCharacter.hasEquipped( ItemPool.DOWSING_ROD, EquipmentManager.OFFHAND ) )
			{
				explored += 2;
			}
			if ( Preferences.getString( "peteMotorbikeHeadlight" ).equals( "Blacklight Bulb" ) )
			{
				explored += 2;
			}
			QuestManager.incrementDesertExploration( explored );
			break;

		case AdventurePool.ZEPPELIN_PROTESTORS:
			Matcher LighterMatcher = QuestManager.LIGHTER_PATTERN.matcher( responseText );
			if ( LighterMatcher.find() )
			{
				Preferences.increment( "zeppelinProtestors", StringUtilities.parseInt( LighterMatcher.group( 1 ) ) );
			}
			else
			{
				Preferences.increment( "zeppelinProtestors", 1 );
			}
			break;

		case AdventurePool.RED_ZEPPELIN:
			if ( responseText.contains( "inevitable confrontation with Ron Copperhead" ) )
			{
				QuestDatabase.setQuestProgress( Quest.RON, "step4" );
			}
			break;

		case AdventurePool.HAUNTED_KITCHEN:
			Matcher DrawerMatcher = QuestManager.DRAWER_PATTERN.matcher( responseText );
			if ( DrawerMatcher.find() )
			{
				Preferences.increment( "manorDrawerCount", StringUtilities.parseInt( DrawerMatcher.group( 1 ) ) );
			}
			break;

		case AdventurePool.BLACK_FOREST:
			if ( responseText.contains( "discover the trail leading to the Black Market" ) )
			{
				QuestDatabase.setQuestProgress( Quest.MACGUFFIN, "step1" );
				QuestDatabase.setQuestProgress( Quest.BLACK, "step2" );
				Preferences.setInteger( "blackForestProgress", 5 );
			}
			else
			{
				if ( responseText.contains( "find a row of blackberry bushes so thick" ) )
				{
					Preferences.setInteger( "blackForestProgress", 1 );
				}
				else if ( responseText.contains( "find a cozy black cottage nestled deep" ) )
				{
					Preferences.setInteger( "blackForestProgress", 2 );
				}
				else if ( responseText.contains( "spot a mineshaft sunk deep into the black depths" ) )
				{
					Preferences.setInteger( "blackForestProgress", 3 );
				}
				else if ( responseText.contains( "find a church that would be picturesque if it wasn't so sinister" ) )
				{
					Preferences.setInteger( "blackForestProgress", 4 );
				}
					
				QuestDatabase.setQuestIfBetter( Quest.BLACK, "step1" );
			}
			break;
		}
	}
}
