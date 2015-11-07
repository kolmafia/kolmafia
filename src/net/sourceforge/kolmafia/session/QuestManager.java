/**
 * Copyright (c) 2005-2015, KoLmafia development team
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
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
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
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.OrcChasmRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.TavernRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
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
	private static final Pattern LOWER_CHAMBER_PATTERN = Pattern.compile( "action=pyramid_state(\\d+)" );
	private static final Pattern GORE_PATTERN = Pattern.compile( "(\\d+) pounds of (?:the gore|gore)" );
	private static final Pattern TOURIST_PATTERN = Pattern.compile( "and the (\\d+) tourists in front" );
	private static final Pattern WALFORD_PATTERN = Pattern.compile( "\\(Walford's bucket filled by (\\d+)%\\)" );

	public static final void handleQuestChange( GenericRequest request )
	{
		String location = request.getURLString();
		String responseText = request.responseText;
		String locationId = request.getFormField( "snarfblat" );
		if ( locationId == null )
		{
			locationId = "";
		}
		if ( location.startsWith( "adventure" ) )
		{
			if ( locationId.equals( AdventurePool.ROAD_TO_WHITE_CITADEL_ID ) )
			{
				handleWhiteCitadelChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.WHITEYS_GROVE_ID ) )
			{
				handleWhiteysGroveChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.BARROOM_BRAWL_ID ) )
			{
				handleBarroomBrawlChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.KNOB_SHAFT_ID ) )
			{
				handleKnobShaftChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.EXTREME_SLOPE_ID ) )
			{
				handleExtremityChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.ICY_PEAK_ID ) )
			{
				handleIcyPeakChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.AIRSHIP_ID ) ||
			          locationId.equals( AdventurePool.CASTLE_BASEMENT_ID ) ||
			          locationId.equals( AdventurePool.CASTLE_GROUND_ID ) ||
			          locationId.equals( AdventurePool.CASTLE_TOP_ID ) )
			{
				handleBeanstalkChange( location, responseText );
			}
			else if ( locationId.equals( AdventurePool.ZEPPELIN_PROTESTORS_ID ) )
			{
				handleZeppelinMobChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.RED_ZEPPELIN_ID ) )
			{
				handleZeppelinChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.PALINDOME_ID ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.PALINDOME, QuestDatabase.STARTED );
			}
			else if ( locationId.equals( AdventurePool.POOP_DECK_ID ) )
			{
				handlePoopDeckChange( responseText );
			}
			else if ( locationId.equals( AdventurePool.HAUNTED_BALLROOM_ID ) )
			{
				handleManorSecondFloorChange( location, responseText );
			}
			else if ( locationId.equals( AdventurePool.UPPER_CHAMBER_ID ) ||
			          locationId.equals( AdventurePool.MIDDLE_CHAMBER_ID ) )
			{
				handlePyramidChange( location, responseText );
			}
			else if ( locationId.equals( AdventurePool.SLOPPY_SECONDS_DINER_ID ) ||
			          locationId.equals( AdventurePool.FUN_GUY_MANSION_ID ) ||
			          locationId.equals( AdventurePool.YACHT_ID ) ||
			          locationId.equals( AdventurePool.DR_WEIRDEAUX_ID ) ||
			          locationId.equals( AdventurePool.SECRET_GOVERNMENT_LAB_ID ) ||
			          locationId.equals( AdventurePool.DEEP_DARK_JUNGLE_ID ) ||
			          locationId.equals( AdventurePool.BARF_MOUNTAIN_ID ) ||
			          locationId.equals( AdventurePool.GARBAGE_BARGES_ID ) ||
			          locationId.equals( AdventurePool.TOXIC_TEACUPS_ID ) ||
			          locationId.equals( AdventurePool.LIQUID_WASTE_SLUICE_ID ) ||
			          locationId.equals( AdventurePool.SMOOCH_ARMY_HQ_ID ) ||
			          locationId.equals( AdventurePool.VELVET_GOLD_MINE_ID ) ||
			          locationId.equals( AdventurePool.LAVACO_LAMP_FACTORY_ID ) ||
			          locationId.equals( AdventurePool.BUBBLIN_CALDERA_ID ) ||
			          locationId.equals( AdventurePool.ICE_HOTEL_ID ) ||
			          locationId.equals( AdventurePool.VYKEA_ID ) ||
			          locationId.equals( AdventurePool.ICE_HOLE_ID ) )
			{
				handleAirportChange( location, responseText );
			}
			else if ( locationId.equals( AdventurePool.MARINARA_TRENCH_ID ) ||
			          locationId.equals( AdventurePool.ANENOME_MINE_ID ) ||
			          locationId.equals( AdventurePool.DIVE_BAR_ID ) ||
			          locationId.equals( AdventurePool.MERKIN_OUTPOST_ID ) ||
			          locationId.equals( AdventurePool.CALIGINOUS_ABYSS_ID ) )
			{
				handleSeaChange( location, responseText );
			}
			else if ( KoLCharacter.getInebriety() > 25 )
			{
				handleSneakyPeteChange( responseText );
			}
		}
		else if ( location.startsWith( "beanstalk" ) )
		{
			if ( responseText.contains( "airship.gif" ) )
			{
				QuestDatabase.setQuestIfBetter( Quest.GARBAGE, "step1" );
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
			if ( location.contains( "action=cell37" ) )
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
		          locationId.equals( AdventurePool.ABOO_PEAK_ID ) ||
		          locationId.equals( AdventurePool.OIL_PEAK_ID ) )
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
			if ( location.contains( "whichplace=airport" ) )
			{
				handleAirportChange( location, responseText );
			}
			else if ( location.contains( "whichplace=bathole" ) )
			{
				handleBatholeChange( responseText );
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
				if ( responseText.contains( "Cold as ice and twice as smooth" ) )
				{
					QuestDatabase.setQuestProgress( Quest.MANOR, QuestDatabase.FINISHED );
					ResultProcessor.removeItem( ItemPool.ED_EYE );
					if ( InventoryManager.getCount( ItemPool.ED_FATS_STAFF ) == 0 && InventoryManager.getCount( ItemPool.ED_AMULET ) == 0 )
					{
						QuestDatabase.setQuestProgress( Quest.MACGUFFIN, QuestDatabase.FINISHED );
					}
				}
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
			else if ( location.contains( "whichplace=town" ) )
			{
				handleTownChange( responseText );
			}
			else if ( location.contains( "whichplace=woods" ) )
			{
				handleWoodsChange( location, responseText );
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
		else if ( location.startsWith( "tavern" ) )
		{
			TavernManager.handleTavernChange( responseText );
		}
		else if ( location.startsWith( "trickortreat" ) )
		{
			handleTrickOrTreatingChange( responseText );
		}
		// Obsolete. Sigh.
		else if ( location.startsWith( "generate15" ) )
		{
			// You slide the last tile into place ...

			if ( AdventureRequest.registerDemonName( "Strange Cube", responseText ) || responseText.contains( "slide the last tile" ) )
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

	private static void handleBarroomBrawlChange( String responseText )
	{
		if ( responseText.contains( "Jackin' the Jukebox" ) )
		{
			QuestDatabase.setQuestProgress( Quest.CLANCY, "step1" );
		}
	}

	private static void handleKnobShaftChange( String responseText )
	{
		if ( responseText.contains( "A Miner Variation" ) )
		{
			QuestDatabase.setQuestProgress( Quest.CLANCY, "step3" );
		}
	}

	private static void handleIcyPeakChange( String responseText )
	{
		if ( responseText.contains( "Mercury Rising" ) )
		{
			QuestDatabase.setQuestProgress( Quest.CLANCY, "step7" );
		}
	}

	private static void handleTownChange( String responseText )
	{
		boolean oldTimeTower = Preferences.getBoolean( "timeTowerAvailable" );
		boolean newTimeTower = responseText.contains( "town_tower" );
		if ( oldTimeTower != newTimeTower )
		{
			Preferences.setBoolean( "timeTowerAvailable", newTimeTower );
			ConcoctionDatabase.setRefreshNeeded( false );
		}
	}

	private static void handleGuildChange( final String responseText )
	{
		if ( responseText.contains( "South of the Border" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.MEATCAR, QuestDatabase.FINISHED );
		}
		if ( responseText.contains( "not recovered the Epic Weapon yet" ) ||
		     responseText.contains( "The Tomb is within the Misspelled Cemetery" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, QuestDatabase.STARTED );
		}
		if ( responseText.contains( "Clownlord Beelzebozo" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step5" );
		}
		if ( responseText.contains( "Meatsmithing hammer" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step7" );
		}
		if ( QuestDatabase.isQuestStep( Quest.NEMESIS, "step8" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step9" );
		}
		if ( responseText.contains( "in the Big Mountains" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step10" );
		}
		if ( QuestDatabase.isQuestStep( Quest.NEMESIS, "step16" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step17" );
		}
	}

	private static void handlePoopDeckChange( final String responseText )
	{
		if ( responseText.contains( "unlocks a padlock on a trap door" ) )
		{
			QuestDatabase.setQuestProgress( Quest.PIRATE, QuestDatabase.FINISHED );
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
		if ( responseText.contains( "I Guess They Were the Existential Blues Brothers" ) )
		{
			QuestDatabase.setQuestProgress( Quest.CITADEL, "step3" );
		}
		else
		{
			QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step2" );
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
				Preferences.setInteger( "pyramidPosition", 1 );
				QuestDatabase.setQuestProgress( Quest.PYRAMID, "step3" );
			}
			else if ( responseText.contains( "Don't You Know Who I Am?" ) )
			{
				QuestDatabase.setQuestProgress( Quest.CLANCY, QuestDatabase.FINISHED );
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
			Matcher LowerChamberMatcher = QuestManager.LOWER_CHAMBER_PATTERN.matcher( responseText );
			if ( LowerChamberMatcher.find() )
			{
				Preferences.setInteger( "pyramidPosition", StringUtilities.parseInt( LowerChamberMatcher.group( 1 ) ) );
			}
			if ( responseText.contains( "action=pyramid_state1a" ) )
			{
				Preferences.setBoolean( "pyramidBombUsed", true );
			}
			// Lower chamber parsing
			if ( location.contains( "action=pyramid_state" ) )
			{
				if ( responseText.contains( "the rubble is gone" ) )
				{
					Preferences.setBoolean( "pyramidBombUsed", true );
					ResultProcessor.processItem( ItemPool.ANCIENT_BOMB, -1 );
				}
			}
		}
		return;
	}

	public static final void handleAirportChange( final String location, final String responseText )
	{
		// Check Cold settings
		if ( !Preferences.getBoolean( "coldAirportAlways" ) )
		{
			// Detect if Airport is open today
			if ( location.contains( AdventurePool.ICE_HOTEL_ID ) ||
			     location.contains( AdventurePool.VYKEA_ID ) ||
			     location.contains( AdventurePool.ICE_HOLE_ID ) ||
			     location.contains( "whichplace=airport_cold" ) )
			{
				Preferences.setBoolean( "_coldAirportToday", true );
			}
			else if ( location.contains( "whichplace=airport" ) )
			{
				if ( responseText.contains( "whichplace=airport_cold" ) )
				{
					Preferences.setBoolean( "_coldAirportToday", true );
				}
			}
		}

		// Check Hot settings
		if ( !Preferences.getBoolean( "hotAirportAlways" ) )
		{
			// Detect if Airport is open today
			if ( location.contains( AdventurePool.SMOOCH_ARMY_HQ_ID ) ||
			     location.contains( AdventurePool.VELVET_GOLD_MINE_ID ) ||
			     location.contains( AdventurePool.LAVACO_LAMP_FACTORY_ID ) ||
			     location.contains( AdventurePool.BUBBLIN_CALDERA_ID ) ||
			     location.contains( "whichplace=airport_hot" ) )
			{
				Preferences.setBoolean( "_hotAirportToday", true );
			}
			else if ( location.contains( "whichplace=airport" ) )
			{
				if ( responseText.contains( "whichplace=airport_hot" ) )
				{
					Preferences.setBoolean( "_hotAirportToday", true );
				}
			}
		}

		// Check Sleaze settings
		if ( !Preferences.getBoolean( "sleazeAirportAlways" ) )
		{
			// Detect if Airport is open today
			if ( location.contains( AdventurePool.FUN_GUY_MANSION_ID ) ||
			     location.contains( AdventurePool.SLOPPY_SECONDS_DINER_ID ) ||
			     location.contains( AdventurePool.YACHT_ID ) ||
			     location.contains( "whichplace=airport_sleaze" ) )
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
		}

		// Check Spooky settings
		if ( !Preferences.getBoolean( "spookyAirportAlways" ) )
		{
			// Detect if Airport is open today
			if ( location.contains( AdventurePool.DR_WEIRDEAUX_ID ) ||
			     location.contains( AdventurePool.SECRET_GOVERNMENT_LAB_ID ) ||
			     location.contains( AdventurePool.DEEP_DARK_JUNGLE_ID ) ||
			     location.contains( "whichplace=airport_spooky" ) )
			{
				Preferences.setBoolean( "_spookyAirportToday", true );
			}
			else if ( location.contains( "whichplace=airport" ) )
			{
				if ( responseText.contains( "whichplace=airport_spooky" ) )
				{
					Preferences.setBoolean( "_spookyAirportToday", true );
				}
			}
		}

		// Check Stench settings
		if ( !Preferences.getBoolean( "stenchAirportAlways" ) )
		{
			// Detect if Airport is open today
			if ( location.contains( AdventurePool.BARF_MOUNTAIN_ID ) ||
			     location.contains( AdventurePool.GARBAGE_BARGES_ID ) ||
			     location.contains( AdventurePool.TOXIC_TEACUPS_ID ) ||
			     location.contains( AdventurePool.LIQUID_WASTE_SLUICE_ID ) ||
			     location.contains( "whichplace=airport_stench" ) )
			{
				Preferences.setBoolean( "_stenchAirportToday", true );
			}
			else if ( location.contains( "whichplace=airport" ) )
			{
				if ( responseText.contains( "whichplace=airport_stench" ) )
				{
					Preferences.setBoolean( "_stenchAirportToday", true );
				}
			}
		}

		// Detect Bunker state
		if ( location.contains( "whichplace=airport_spooky_bunker" ) )
		{
			if ( responseText.contains( "action=si_shop1locked" ) )
			{
				Preferences.setBoolean( "SHAWARMAInitiativeUnlocked", false );
			}
			if ( responseText.contains( "action=si_shop2locked" ) )
			{
				Preferences.setBoolean( "canteenUnlocked", false );
			}
			if ( responseText.contains( "action=si_shop3locked" ) )
			{
				Preferences.setBoolean( "armoryUnlocked", false );
			}
			if ( responseText.contains( "whichshop=si_shop1" ) )
			{
				Preferences.setBoolean( "SHAWARMAInitiativeUnlocked", true );
			}
			if ( responseText.contains( "whichshop=si_shop2" ) )
			{
				Preferences.setBoolean( "canteenUnlocked", true );
			}
			if ( responseText.contains( "whichshop=si_shop3" ) )
			{
				Preferences.setBoolean( "armoryUnlocked", true );
			}
			if ( responseText.contains( "find the door to the secret government sandwich shop and use the keycard" ) )
			{
				Preferences.setBoolean( "SHAWARMAInitiativeUnlocked", true );
				ResultProcessor.removeItem( ItemPool.SHAWARMA_KEYCARD );
			}
			if ( responseText.contains( "find a door with a bottle-shaped icon on it, zip the keycard through the reader" ) )
			{
				Preferences.setBoolean( "canteenUnlocked", true );
				ResultProcessor.removeItem( ItemPool.BOTTLE_OPENER_KEYCARD );
			}
			if ( responseText.contains( "insert the keycard and the door slides open" ) )
			{
				Preferences.setBoolean( "armoryUnlocked", true );
				ResultProcessor.removeItem( ItemPool.ARMORY_KEYCARD );
			}
		}
		return;
	}

	private static void handleWoodsChange( final String location, final String responseText )
	{
		if ( responseText.contains( "wcroad.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step1" );
		}

		if ( location.contains( "action=woods_dakota" ) )
		{
			if ( responseText.contains( "need you to pick up a couple things for me" ) )
			{
				QuestDatabase.setQuestProgress( Quest.TEMPLE, QuestDatabase.STARTED );
			}
			else if ( responseText.contains( "make a note of the temple's location" ) )
			{
				QuestDatabase.setQuestProgress( Quest.TEMPLE, QuestDatabase.FINISHED );
				Preferences.setInteger( "lastTempleUnlock", KoLCharacter.getAscensions() );
			}
		}
		// If we see the Hidden Temple, mark it as unlocked
		if ( responseText.contains( "temple.gif" ) )
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
		if ( responseText.contains( "action=emptybm" ) )
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
		else if ( image == 5 )
		{
			status = "step4";
		}

		QuestDatabase.setQuestIfBetter( Quest.BAT, status );
	}

	private static final void handleSneakyPeteChange( final String responseText )
	{
		if ( responseText.contains( "You hand him your button and take his glowstick" ) )
		{
			EquipmentManager.discardEquipment( ItemPool.NOVELTY_BUTTON );
			return;
		}

		if ( responseText.contains( "Ah, man, you dropped your crown back there!" ) )
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
		if ( responseText.contains( "scientists should have a file on me" ) ||
			responseText.contains( "Did you find that file yet" ) )
		{
			QuestDatabase.setQuestProgress( Quest.ESCAPE, QuestDatabase.STARTED );
		}
		// You pass the folder through the little barred window, and hear Subject 37 flipping through the pages
		if ( responseText.contains( "pass the folder through" ) )
		{
			ResultProcessor.processItem( ItemPool.SUBJECT_37_FILE, -1 );
			// You may already have the item you are sent to get
			if ( InventoryManager.getCount( ItemPool.GOTO ) > 0 )
			{
				QuestDatabase.setQuestProgress( Quest.ESCAPE, "step3" );
			}
			else
			{
				QuestDatabase.setQuestProgress( Quest.ESCAPE, "step2" );
			}
		}
		// You pass the GOTO through the window, and Subject 37 thanks you.
		if ( responseText.contains( "pass the GOTO through" ) )
		{
			ResultProcessor.processItem( ItemPool.GOTO, -1 );
			// You may already have the item you are sent to get
			if ( InventoryManager.getCount( ItemPool.WEREMOOSE_SPIT ) > 0 )
			{
				QuestDatabase.setQuestProgress( Quest.ESCAPE, "step5" );
			}
			else
			{
				QuestDatabase.setQuestProgress( Quest.ESCAPE, "step4" );
			}
		}
		// You pass the little vial of of weremoose spit through the window.
		if ( responseText.contains( "pass the little vial" ) )
		{
			ResultProcessor.processItem( ItemPool.WEREMOOSE_SPIT, -1 );
			// You may already have the item you are sent to get
			if ( InventoryManager.getCount( ItemPool.ABOMINABLE_BLUBBER ) > 0 )
			{
				QuestDatabase.setQuestProgress( Quest.ESCAPE, "step7" );
			}
			else
			{
				QuestDatabase.setQuestProgress( Quest.ESCAPE, "step6" );
			}
		}
		// You hand Subject 37 the glob of abominable blubber.
		if ( responseText.contains( "hand Subject 37 the glob" ) )
		{
			ResultProcessor.processItem( ItemPool.ABOMINABLE_BLUBBER, -1 );
			QuestDatabase.setQuestProgress( Quest.ESCAPE, QuestDatabase.FINISHED );
		}
	}

	private static final void handleFriarsChange( final String responseText )
	{
		// "Thank you, Adventurer."

		if ( responseText.contains( "Thank you" ) || responseText.contains( "Please return to us if there's ever anything we can do for you in return" ) )
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
		if ( location.contains( "action=highlands_dude" ) &&
			( responseText.contains( "trying to, like, order a pizza" ) || responseText.contains( "trying to order a pizza" ) ) )
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

		if ( responseText.contains( "immediately grows into an enormous beanstalk" ) )
		{
			ResultProcessor.processItem( ItemPool.ENCHANTED_BEAN, -1 );
			QuestDatabase.setQuestProgress( Quest.GARBAGE, "step1" );
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
		else if ( location.contains( AdventurePool.CASTLE_TOP_ID ) &&
		          !responseText.contains( "You have to learn to walk" ) &&
		          !responseText.contains( "You'll have to figure out some other way" ) )
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
		if ( location.contains( "action=pal_mr" ) )
		{
			if ( responseText.contains( "in the mood for a bowl of wet stunt nut stew" ) )
			{
				QuestDatabase.setQuestProgress( Quest.PALINDOME, "step3" );
				ResultProcessor.removeItem( ItemPool.PALINDROME_BOOK_2 );
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

		else if ( responseText.contains( "He takes the load of cheese and ore" ) ||
				responseText.contains( "haul your load of ore and cheese" ) )
		{
			AdventureResult item = ItemPool.get( Preferences.getString( "trapperOre" ), -3 );
			ResultProcessor.processResult( item );
			ResultProcessor.processResult( ItemPool.get( ItemPool.GOAT_CHEESE, -3 ) );
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step2" );
		}

		// Yeehaw!  I heard the noise and seen them mists dissapatin' from clear down here!  Ya done it!  Ya rightly done it!
		else if ( responseText.contains( "Yeehaw!  I heard the noise" ) )
		{
			Preferences.setInteger( "lastTr4pz0rQuest", KoLCharacter.getAscensions() );
			ResultProcessor.removeItem( ItemPool.GROARS_FUR );
			QuestDatabase.setQuestProgress( Quest.TRAPPER, QuestDatabase.FINISHED );
		}

		// "Yeehaw!" John exclaims as you drag the huge yeti pelt into his shack.
		else if ( responseText.contains( "drag the huge yeti pelt into his shack" ) )
		{
			Preferences.setInteger( "lastTr4pz0rQuest", KoLCharacter.getAscensions() );
			ResultProcessor.removeItem( ItemPool.WINGED_YETI_FUR );
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

		if ( responseText.contains( "500" ) )
		{
			ResultProcessor.removeItem( ItemPool.MOSQUITO_LARVA );
		}
		if ( responseText.contains( "dragonbone belt buckle" ) )
		{
			ResultProcessor.removeItem( ItemPool.BONERDAGON_SKULL );
		}
		QuestDatabase.handleCouncilText( responseText );
		if ( QuestDatabase.isQuestLaterThan( Quest.MACGUFFIN, QuestDatabase.UNSTARTED ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.BLACK, QuestDatabase.STARTED );
		}
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
			if ( !KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.EARTHEN_FIST ) ) )
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
	public static void updateQuestData( String responseText, String monsterName )
	{
		String adventureId = KoLAdventure.lastAdventureIdString();
		String counter =
			adventureId.equals( "ns_01_crowd1" ) ?
			"nsContestants1" :
			adventureId.equals( "ns_01_crowd2" ) ?
			"nsContestants2" :
			adventureId.equals( "ns_01_crowd3" ) ?
			"nsContestants3" :
			null;

		if ( counter != null )
		{
			int crowd = Preferences.getInteger( counter );
			if ( crowd > 0 )
			{
				Preferences.setInteger( counter, crowd - 1 );
			}
			if ( Preferences.getInteger( "nsContestants1" ) == 0 &&
				Preferences.getInteger( "nsContestants2" ) == 0 &&
				Preferences.getInteger( "nsContestants3" ) == 0 )
			{
				QuestDatabase.setQuestProgress( Quest.FINAL, "step2" );
			}
			return;
		}

		if ( monsterName.equals( "screambat" ) )
		{
			if ( !QuestDatabase.isQuestLaterThan( Quest.BAT, "step2" ) )
			{
				QuestDatabase.advanceQuest( Quest.BAT );
			}
		}

		else if ( monsterName.equals( "dirty thieving brigand" ) )
		{
			// "Well," you say, "it would really help the war effort if
			// your convent could serve as a hospital for our wounded
			// troops."
			if ( responseText.contains( "could serve as a hospital" ) )
			{
				Preferences.setString( "sidequestNunsCompleted", "hippy" );
			}
			else if ( responseText.contains( "could serve as a massage parlor" ) )
			{
				Preferences.setString( "sidequestNunsCompleted", "fratboy" );
			}
		}
		// oil slick: 6.34
		// oil tycoon: 19.02
		// oil baron: 31.7
		// oil cartel: 63.4
		// dress pants: 6.34
		else if ( monsterName.equals( "oil slick" ) )
		{
			double pantsBonus = InventoryManager.getEquippedCount( ItemPool.DRESS_PANTS ) > 0 ? 6.34 : 0;
			float current = Preferences.getFloat( "oilPeakProgress" );
			double lovebug = responseText.contains( "love oil beetle trundles up" ) ? 6.34 : 0;

			// normalize
			String setTo = String.format( "%.2f", Math.max( 0, current - 6.34 - pantsBonus - lovebug ) );

			Preferences.setString( "oilPeakProgress", setTo );
		}
		else if ( monsterName.equals( "oil tycoon" ) )
		{
			double pantsBonus = InventoryManager.getEquippedCount( ItemPool.DRESS_PANTS ) > 0 ? 6.34 : 0;
			float current = Preferences.getFloat( "oilPeakProgress" );
			double lovebug = responseText.contains( "love oil beetle trundles up" ) ? 6.34 : 0;

			String setTo = String.format( "%.2f", Math.max( 0, current - 19.02 - pantsBonus - lovebug ) );

			Preferences.setString( "oilPeakProgress", setTo );
		}
		else if ( monsterName.equals( "oil baron" ) )
		{
			double pantsBonus = InventoryManager.getEquippedCount( ItemPool.DRESS_PANTS ) > 0 ? 6.34 : 0;
			float current = Preferences.getFloat( "oilPeakProgress" );
			double lovebug = responseText.contains( "love oil beetle trundles up" ) ? 6.34 : 0;

			String setTo = String.format( "%.2f", Math.max( 0, current - 31.7 - pantsBonus - lovebug ) );

			Preferences.setString( "oilPeakProgress", setTo );
		}
		else if ( monsterName.equals( "oil cartel" ) )
		{
			double pantsBonus = InventoryManager.getEquippedCount( ItemPool.DRESS_PANTS ) > 0 ? 6.34 : 0;
			float current = Preferences.getFloat( "oilPeakProgress" );
			double lovebug = responseText.contains( "love oil beetle trundles up" ) ? 6.34 : 0;

			String setTo = String.format( "%.2f", Math.max( 0, current - 63.4 - pantsBonus - lovebug ) );

			Preferences.setString( "oilPeakProgress", setTo );
		}

		else if ( monsterName.equals( "Battlie Knight Ghost" ) ||
			  monsterName.equals( "Claybender Sorcerer Ghost" ) ||
			  monsterName.equals( "Dusken Raider Ghost" ) ||
			  monsterName.equals( "Space Tourist Explorer Ghost" ) ||
			  monsterName.equals( "Whatsian Commando Ghost" ) )
		{
			Preferences.decrement( "booPeakProgress", 2 );
		}

		else if ( monsterName.equals( "panicking Knott Yeti" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step4" );
		}
		else if ( monsterName.equals( "pygmy witch accountant" ) )
		{
			// If you don't have McClusky File (complete), or
			// McClusky File 5, and accountant doesn't drop file,
			// you must have unlocked office boss
			if ( InventoryManager.getCount( ItemPool.MCCLUSKY_FILE ) == 0 &&
			     InventoryManager.getCount( ItemPool.MCCLUSKY_FILE_PAGE5 ) == 0 &&
			     Preferences.getInteger( "hiddenOfficeProgress" ) < 6 &&
			     !responseText.contains( "McClusky file" ) )
			{
				Preferences.setInteger( "hiddenOfficeProgress", 6 );
			}
		}
		else if ( monsterName.equals( "topiary gopher" ) ||
			  monsterName.equals( "topiary chihuahua herd" ) ||
			  monsterName.equals( "topiary duck" ) ||
			  monsterName.equals( "topiary kiwi" ) )
		{
			// We are still in the Hedge Maze
			QuestDatabase.setQuestProgress( Quest.FINAL, "step4" );
		}
		else if ( monsterName.equals( "wall of skin" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, "step7" );
		}
		else if ( monsterName.equals( "wall of meat" ) &&
			  responseText.contains( "the stairs to the next floor are clear" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, "step8" );
		}
		else if ( monsterName.equals( "wall of bones" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, "step9" );
		}
		else if ( monsterName.equals( "Your Shadow" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, "step11" );
		}
		else if ( monsterName.equals( "Clancy" ) )
		{
			// We do not currently have a distinct step for Clancy
			QuestDatabase.setQuestProgress( Quest.FINAL, "step11" );
		}
		else if ( monsterName.equals( "Naughty Sorceress (3)" ) ||
			  monsterName.equals( "The Avatar of Sneaky Pete" ) ||
			  monsterName.equals( "The Avatar of Boris" ) ||
			  monsterName.equals( "Principal Mooney" ) ||
			  monsterName.equals( "Rene C. Corman" ) ||
			  monsterName.equals( "The Avatar of Jarlsberg" ) ||
			  monsterName.equals( "The Rain King" ) ||
			  responseText.contains( "Thwaitgold bee statuette" ) )
		{
			QuestDatabase.setQuestProgress( Quest.FINAL, "step12" );
		}
		else if ( monsterName.equals( "The Unknown Seal Clubber" ) ||
			  monsterName.equals( "The Unknown Turtle Tamer" ) ||
			  monsterName.equals( "The Unknown Pastamancer" ) ||
			  monsterName.equals( "The Unknown Sauceror" ) ||
			  monsterName.equals( "The Unknown Disco Bandit" ) ||
			  monsterName.equals( "The Unknown Accordion Thief" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step2" );
		}
		else if ( monsterName.equals( "The Clownlord Beelzebozo" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step6" );
		}
		else if ( monsterName.equals( "menacing thug" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step19" );
		}
		else if ( monsterName.equals( "Mob Penguin hitman" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step21" );
		}
		else if ( monsterName.equals( "hunting seal" ) ||
			  monsterName.equals( "turtle trapper" ) ||
			  monsterName.equals( "evil spaghetti cult assassin" ) ||
			  monsterName.equals( "b&eacute;arnaise zombie" ) ||
			  monsterName.equals( "flock of seagulls" ) ||
			  monsterName.equals( "mariachi bandolero" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step23" );
		}
		else if ( monsterName.equals( "Gorgolok, the Infernal Seal (Volcanic Cave)" ) ||
			  monsterName.equals( "Stella, the Turtle Poacher (Volcanic Cave)" ) ||
			  monsterName.equals( "Spaghetti Elemental (Volcanic Cave)" ) ||
			  monsterName.equals( "Lumpy, the Sinister Sauceblob (Volcanic Cave)" ) ||
			  monsterName.equals( "Spirit of New Wave (Volcanic Cave)" ) ||
			  monsterName.equals( "Somerset Lopez, Dread Mariachi (Volcanic Cave)" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step29" );
		}
		else if ( monsterName.equals( "Sloppy Seconds Burger" ) )
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
		else if ( monsterName.equals( "Sloppy Seconds Cocktail" ) )
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
		else if ( monsterName.equals( "Sloppy Seconds Sundae" ) )
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
		else if ( monsterName.equals( "taco fish" ) )
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
		else if ( monsterName.equals( "Fun-Guy Playmate" ) )
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
		else if ( monsterName.equals( "Wu Tang the Betrayer" ) )
		{
			Preferences.setInteger( "lastWuTangDefeated", KoLCharacter.getAscensions() );
		}
		else if ( monsterName.equals( "Baron Von Ratsworth" ) )
		{
			TavernRequest.addTavernLocation( '6' );
		}
		else if ( monsterName.equals( "wumpus" ) )
		{
			WumpusManager.reset();
		}
		else if ( monsterName.equals( "pair of burnouts" ) )
		{
			int increment = responseText.contains( "throw the opium grenade" ) ? 3 : 1;
			Preferences.increment( "burnoutsDefeated", increment, 30, false );
			if ( Preferences.getInteger( "burnoutsDefeated" ) == 30 )
			{
				QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step4" );
			}
		}
		else if ( monsterName.equals( "biclops" ) )
		{
			QuestDatabase.setQuestProgress( Quest.CITADEL, "step5" );
		}
		else if ( monsterName.equals( "surprised and annoyed witch" ) ||
			  monsterName.equals( "extremely annoyed witch" ) )
		{
			QuestDatabase.setQuestProgress( Quest.CITADEL, "step7" );
		}
		else if ( monsterName.equals( "Elp&iacute;zo & Crosybdis" ) )
		{
			QuestDatabase.setQuestProgress( Quest.CITADEL, "step10" );
		}
		else if ( monsterName.equals( "hulking bridge troll" ) )
		{
			OrcChasmRequest.setChasmProgress( 0 );
		}
		else if ( monsterName.equals( "warehouse guard" ) ||
				monsterName.equals( "warehouse janitor" ) ||
				monsterName.equals( "warehouse clerk" ) )
		{
			Preferences.increment( "warehouseProgress", 1 );
		}
		else if ( monsterName.equals( "E.V.E., the robot zombie" ) )
		{
			QuestDatabase.setQuestProgress( Quest.EVE, "step1" );
		}
		else if ( monsterName.equals( "writing desk" ) )
		{
			if ( responseText.contains( "You open" ) )
			{
				Preferences.increment( "writingDesksDefeated", 1, 5, false );
			}
		}
		else if ( monsterName.equals( "nasty bear" ) )
		{
			Preferences.increment( "dinseyNastyBearsDefeated", 1, 8, false );
			QuestDatabase.setQuestProgress( Quest.NASTY_BEARS, 
				( Preferences.getInteger( "dinseyNastyBearsDefeated" ) == 8 ? "step2" : "step1" ) );
		}
		else if ( monsterName.equals( "Wart Dinsey" ) )
		{
			Preferences.setInteger( "lastWartDinseyDefeated", KoLCharacter.getAscensions() );
		}
		else if ( monsterName.equals( "Cake Lord" ) )
		{
			QuestDatabase.setQuestProgress( Quest.ARMORER, "step3" );
		}

		int adventure = KoLAdventure.lastAdventureId();

		switch ( adventure )
		{
		case AdventurePool.MERKIN_COLOSSEUM:
			// Do not increment round for wandering monsters
			if ( ( monsterName.equals( "Mer-kin balldodger" ) ||
			       monsterName.equals( "Mer-kin netdragger" ) ||
			       monsterName.equals( "Mer-kin bladeswitcher" ) ||
			       monsterName.equals( "Georgepaul, the Balldodger" ) ||
			       monsterName.equals( "Johnringo, the Netdragger" ) ||
			       monsterName.equals( "Ringogeorge, the Bladeswitcher" ) ) &&
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
			// clingy monsters do not increment exploration
			if ( !responseText.contains( "Desert exploration" ) )
			{
				break;
			}
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
				int flamingProtesters = StringUtilities.parseInt( LighterMatcher.group( 1 ) );
				Preferences.increment( "zeppelinProtestors", flamingProtesters );
				RequestLogger.printLine( "Set fire to " + flamingProtesters + " protesters" );			}
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
			if ( !InventoryManager.hasItem( ItemPool.BILLIARDS_KEY ) )
			{
				Matcher drawerMatcher = QuestManager.DRAWER_PATTERN.matcher( responseText );
				if ( drawerMatcher.find() )
				{
					Preferences.increment( "manorDrawerCount", StringUtilities.parseInt( drawerMatcher.group( 1 ) ) );
				}
				else
				{
					Preferences.increment( "manorDrawerCount", 1 );
				}
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

		case AdventurePool.DEEP_DARK_JUNGLE:
			if ( responseText.contains( "jungle pun occurs to you" ) )
			{
				Preferences.increment( "junglePuns", 1 );
				if ( Preferences.getInteger( "junglePuns" ) >= 11 )
				{
					QuestDatabase.setQuestProgress( Quest.JUNGLE_PUN, "step2" );
				}
			}
			break;

		case AdventurePool.SECRET_GOVERNMENT_LAB:
			Matcher GoreMatcher = QuestManager.GORE_PATTERN.matcher( responseText );
			if ( GoreMatcher.find() )
			{
				Preferences.increment( "goreCollected", StringUtilities.parseInt( GoreMatcher.group( 1 ) ) );
				if ( Preferences.getInteger( "goreCollected" ) >= 100 )
				{
					QuestDatabase.setQuestProgress( Quest.GORE, "step2" );
				}
			}
			if ( responseText.contains( "The gore sloshes around nauseatingly in your bucket" ) )
			{
				QuestDatabase.setQuestProgress( Quest.GORE, "step2" );
			}
			break;

		case AdventurePool.BARF_MOUNTAIN:
			if ( responseText.contains( "made it to the front of the line" ) )
			{
				Preferences.setBoolean( "dinseyRollercoasterNext", true );
			}
			break;

		case AdventurePool.GARBAGE_BARGES:
			if ( QuestDatabase.isQuestLaterThan( Quest.SOCIAL_JUSTICE_I, QuestDatabase.UNSTARTED ) )
			{
				Preferences.increment( "dinseySocialJusticeIProgress", 1 );
			}
			else if ( responseText.contains( "probably not embarrassingly sexist anymore" ) )
			{
				Preferences.setInteger( "dinseySocialJusticeIProgress", 15 );
				QuestDatabase.setQuestProgress( Quest.SOCIAL_JUSTICE_I, "step1" );
			}
			if ( responseText.contains( "at least the barges aren't getting hung up on it anymore" ) )
			{
				Preferences.setInteger( "dinseyFilthLevel", 0 );
				QuestDatabase.setQuestProgress( Quest.FISH_TRASH, "step2" );
			}
			else if ( responseText.contains( "larger chunks of garbage out of the waterway" ) )
			{
				Preferences.decrement( "dinseyFilthLevel", 5, 0 );
				QuestDatabase.setQuestProgress( Quest.FISH_TRASH, "step1" );
			}
			break;

		case AdventurePool.TOXIC_TEACUPS:
			if ( responseText.contains( "pretend to be having a good time" ) )
			{
				Preferences.increment( "dinseyFunProgress", 1 );
			}
			else if ( responseText.contains( "surrounding crowd seems to be pretty excited about the ride" ) )
			{
				Preferences.setInteger( "dinseyFunProgress", 15 );
				QuestDatabase.setQuestProgress( Quest.ZIPPITY_DOO_DAH, "step2" );
			}
			break;

		case AdventurePool.LIQUID_WASTE_SLUICE:
			if ( responseText.contains( "probably not unacceptably racist anymore" ) )
			{
				Preferences.setInteger( "dinseySocialJusticeIIProgress", 15 );
				QuestDatabase.setQuestProgress( Quest.SOCIAL_JUSTICE_II, "step1" );
			}
			else if ( QuestDatabase.isQuestLaterThan( Quest.SOCIAL_JUSTICE_II, QuestDatabase.UNSTARTED ) )
			{
				Preferences.increment( "dinseySocialJusticeIIProgress", 1 );
			}
			break;

		case AdventurePool.PALINDOME:
			if ( monsterName.equals( "Drab Bard" ) ||
			     monsterName.equals( "Bob Racecar" ) ||
			     monsterName.equals( "Racecar Bob" ) )
			{
				Preferences.increment( "palindomeDudesDefeated", 1, 5, false );
			}
			break;

		case AdventurePool.ICE_HOTEL:
		case AdventurePool.VYKEA:
		case AdventurePool.ICE_HOLE:
			Matcher WalfordMatcher = QuestManager.WALFORD_PATTERN.matcher( responseText );
			while ( WalfordMatcher.find() )
			{
				Preferences.increment( "walfordBucketProgress", StringUtilities.parseInt( WalfordMatcher.group( 1 ) ) );
				if ( Preferences.getInteger( "walfordBucketProgress" ) >= 100 )
				{
					QuestDatabase.setQuestProgress( Quest.BUCKET, "step2" );
				}
			}
			break;
		}
	}

	/** After we lose a fight, some quests may need to be updated.  Centralize handling for it here.
	 * @param responseText The text from (at least) the losing round of the fight
	 * @param monster The monster which beat us up.
	 */
	public static void updateQuestFightLost( String responseText, String monsterName )
	{
		if ( monsterName.equals( "menacing thug" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step18" );
		}
		else if ( monsterName.equals( "Mob Penguin hitman" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step20" );
		}
		else if ( monsterName.equals( "hunting seal" ) ||
			  monsterName.equals( "turtle trapper" ) ||
			  monsterName.equals( "evil spaghetti cult assassin" ) ||
			  monsterName.equals( "b&eacute;arnaise zombie" ) ||
			  monsterName.equals( "flock of seagulls" ) ||
			  monsterName.equals( "mariachi bandolero" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step22" );
		}
		else if ( monsterName.equals( "Argarggagarg the Dire Hellseal" ) ||
			  monsterName.equals( "Safari Jack, Small-Game Hunter" ) ||
			  monsterName.equals( "Yakisoba the Executioner" ) ||
			  monsterName.equals( "Heimandatz, Nacho Golem" ) ||
			  monsterName.equals( "Jocko Homo" ) ||
			  monsterName.equals( "The Mariachi With No Name" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step24" );
		}
		else if ( monsterName.equals( "mother hellseal" ) )
		{
			Preferences.decrement( "_sealScreeches", 1, 0 );
		}
	}

	/** After we start a fight, some quests may need to be updated.  Centralize handling for it here.
	 * @param responseText The text from (at least) the first round of the fight
	 * @param monster The monster
	 */
	public static void updateQuestFightStarted( final String responseText, final String monsterName )
	{
		if ( monsterName.equals( "Gorgolok, the Infernal Seal (Volcanic Cave)" ) ||
		     monsterName.equals( "Stella, the Turtle Poacher (Volcanic Cave)" ) ||
		     monsterName.equals( "Spaghetti Elemental (Volcanic Cave)" ) ||
		     monsterName.equals( "Lumpy, the Sinister Sauceblob (Volcanic Cave)" ) ||
		     monsterName.equals( "Spirit of New Wave (Volcanic Cave)" ) ||
		     monsterName.equals( "Somerset Lopez, Dread Mariachi (Volcanic Cave)" ) )
		{
			QuestDatabase.setQuestProgress( Quest.NEMESIS, "step28" );
		}
		else if ( monsterName.equals( "Cake Lord" ) )
		{
			QuestDatabase.setQuestProgress( Quest.ARMORER, "step2" );
		}
	}
	
	public static void updateQuestItemUsed( final int itemId, final String responseText )
	{
		switch ( itemId )
		{
		case ItemPool.FINGERNAIL_CLIPPERS:
			if ( responseText.contains( "little sliver of something fingernail-like" ) )
			{
				Preferences.increment( "fingernailsClipped", 1 );
				if ( Preferences.getInteger( "fingernailsClipped" ) >= 23 )
				{
					QuestDatabase.setQuestProgress( Quest.CLIPPER, "step1" );
				}
			}
			break;

		case ItemPool.DINSEY_REFRESHMENTS:
			if ( responseText.contains( "realize that the box of refreshments is empty" ) ||
				responseText.contains( "box of snacks is empty" ) )
			{
				QuestDatabase.setQuestProgress( Quest.WORK_WITH_FOOD, "step1" );
				Preferences.setInteger( "dinseyTouristsFed", 30 );
			}
			else if ( responseText.contains( "hand out snacks to your opponent" ) )
			{
				int count = 1;
				if ( responseText.contains( "and the tourist in front" ) )
				{
					count++;
				}
				else
				{
					Matcher touristMatcher = QuestManager.TOURIST_PATTERN.matcher( responseText );
					if ( touristMatcher.find() )
					{
						count+=StringUtilities.parseInt( touristMatcher.group( 1 ) );
					}
				}
				Preferences.increment( "dinseyTouristsFed", count, 30, false );
			}
			break;
		}
	}
	
	public static void updateQuestItemEquipped( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.GORE_BUCKET:
			QuestDatabase.setQuestIfBetter( Quest.GORE, "step1" );
			break;

		case ItemPool.MINI_CASSETTE_RECORDER:
			QuestDatabase.setQuestIfBetter( Quest.JUNGLE_PUN, "step1" );
			break;

		case ItemPool.GPS_WATCH:
			QuestDatabase.setQuestIfBetter( Quest.OUT_OF_ORDER, "step1" );
			break;

		case ItemPool.TRASH_NET:
			QuestDatabase.setQuestIfBetter( Quest.FISH_TRASH, "step1" );
			break;

		case ItemPool.LUBE_SHOES:
			QuestDatabase.setQuestIfBetter( Quest.SUPER_LUBER, "step1" );
			break;

		case ItemPool.MASCOT_MASK:
			QuestDatabase.setQuestIfBetter( Quest.ZIPPITY_DOO_DAH, "step1" );
			break;

		case ItemPool.WALFORDS_BUCKET:
			QuestDatabase.setQuestIfBetter( Quest.BUCKET, "step1" );
			break;
		}
	}
}
