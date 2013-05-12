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

package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.ProfileRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.WineCellarRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;

public class QuestManager
{
	private static final Pattern ORE_PATTERN = Pattern.compile( "(asbestos|linoleum|chrome) ore[\\. ]" );
	private static final Pattern BATHOLE_PATTERN = Pattern.compile( "bathole_(\\d)\\.gif" );

	public static final void handleQuestChange( final String location, final String responseText )
	{
		if ( location.startsWith( "adventure" ) )
		{
			if ( location.indexOf( "216" ) != -1 )
			{
				handleTrickOrTreatingChange( responseText );
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
				KoLCharacter.armBeanstalk();
			}
		}
		else if ( location.startsWith( "barrel" ) )
		{
			BarrelDecorator.parseResponse( location, responseText );
		}
		else if ( location.startsWith( "bigisland" ) )
		{
			IslandDecorator.parseBigIsland( location, responseText );
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
		else if ( location.startsWith( "inv_use" ) )
		{
			if ( location.indexOf( "whichitem=5116" ) != -1 )
			{
				AWOLQuartermasterRequest.parseResponse( location, responseText );
			}
			else if ( location.indexOf( "whichitem=5683" ) != -1 )
			{
				BURTRequest.parseResponse( location, responseText );
			}
		}
		else if ( location.startsWith( "lair" ) )
		{
			SorceressLairManager.handleQuestChange( location, responseText );
		}
		else if ( location.startsWith( "manor3" ) )
		{
			WineCellarRequest.handleCellarChange( responseText );
		}
		else if ( location.startsWith( "pandamonium" ) )
		{
			// Quest starts the very instant you click on pandamonium.php
			QuestDatabase.setQuestIfBetter( Quest.AZAZEL, QuestDatabase.STARTED );
		}
		else if ( location.startsWith( "place.php" ) )
		{
			if ( location.contains( "whichplace=plains" ) )
			{
				handlePlainsChange( responseText );
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
		}
		else if ( location.startsWith( "postwarisland" ) )
		{
			IslandDecorator.parsePostwarIsland( location, responseText );
		}
		else if ( location.startsWith( "questlog" ) )
		{
			QuestLogRequest.registerQuests( false, location, responseText );
		}
		else if ( location.startsWith( "beach.php?action=woodencity" ) )
		{
			parsePyramidChange( responseText );
		}
		else if ( location.startsWith( "showplayer" ) )
		{
			ProfileRequest.parseResponse( location, responseText );
		}
		else if ( location.startsWith( "tavern" ) )
		{
			TavernManager.handleTavernChange( responseText );
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
		}
	}

	private static void handleWoodsChange( final String responseText )
	{
		if ( responseText.contains( "wcroad.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.CITADEL, "step1" );
		}

		// If we see the Hidden Temple, mark it as unlocked
		if ( responseText.indexOf( "otherimages/woods/temple.gif" ) != -1 )
		{
			Preferences.setInteger( "lastTempleUnlock", KoLCharacter.getAscensions() );
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

	private static void parsePyramidChange( String responseText )
	{
		// Suddenly, the model bursts into flames and is quickly consumed, leaving behind a pile of ash and a
		// large hidden trapdoor. You open the trapdoor to find a flight of stone stairs, which appear to
		// descend into an ancient buried pyramid.

		// Well, /that/ wasn't quite what you expected.

		if ( responseText.indexOf( "the model bursts into flames and is quickly consumed" ) != -1 )
		{
			QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step12" );
		}
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
		if ( responseText.indexOf( "pull the pumpkin off of your head" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.PUMPKINHEAD_MASK );
			return;
		}
		if ( responseText.indexOf( "gick all over your mummy costume" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.MUMMY_COSTUME );
			return;
		}
		if ( responseText.indexOf( "unzipping the mask and throwing it behind you" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.WOLFMAN_MASK );
			return;
		}
		if ( responseText.indexOf( "Right on, brah. Here, have some gum." ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.RUSSIAN_ICE, -1 );
			return;
		}
	}

	private static final void handleCell37( final String responseText )
	{
		// You pass the folder through the little barred window, and hear Subject 37 flipping through the pages
		if ( responseText.indexOf( "pass the folder through" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.SUBJECT_37_FILE, -1 );
		}
		// You pass the GOTO through the window, and Subject 37 thanks you.
		if ( responseText.indexOf( "pass the GOTO through" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.GOTO, -1 );
		}
		// You pass the little vial of of weremoose spit through the window.
		if ( responseText.indexOf( "pass the little vial" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.WEREMOOSE_SPIT, -1 );
		}
		// You hand Subject 37 the glob of abominable blubber.
		if ( responseText.indexOf( "hand Subject 37 the glob" ) != -1 )
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
			QuestLogRequest.setBeanstalkPlanted();
			if ( KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "You have planted a beanstalk." );
			}
		}

		if ( responseText.contains( "dome.gif" ) )
		{
			QuestDatabase.setQuestIfBetter( Quest.PALINDOME, "step1" );
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
			QuestDatabase.setQuestProgress( Quest.TRAPPER, QuestDatabase.FINISHED );
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
			if ( !QuestDatabase.isQuestLaterThan( Preferences.getString( Quest.BAT.getPref() ), "step2" ) )
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
	}

}
