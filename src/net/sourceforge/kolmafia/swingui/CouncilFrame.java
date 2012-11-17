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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.swingui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.AdventurePool;

import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;

import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.BURTRequest;
import net.sourceforge.kolmafia.request.CouncilRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.WineCellarRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.session.TavernManager;

import net.sourceforge.kolmafia.webui.IslandDecorator;

public class CouncilFrame
	extends RequestFrame
{
	public static final CouncilRequest COUNCIL_VISIT = new CouncilRequest();

	private static final Pattern ORE_PATTERN = Pattern.compile( "(asbestos|linoleum|chrome) ore[\\. ]" );

	public CouncilFrame()
	{
		super( "Council of Loathing" );
	}

	@Override
	public void setVisible( boolean isVisible )
	{
		super.setVisible( isVisible );

		if ( isVisible )
		{
			CouncilFrame.COUNCIL_VISIT.responseText = null;
			this.displayRequest( CouncilFrame.COUNCIL_VISIT );
		}
	}

	@Override
	public boolean hasSideBar()
	{
		return false;
	}

	@Override
	public String getDisplayHTML( final String responseText )
	{
		return super.getDisplayHTML( responseText )
			.replaceFirst( "<a href=\"town.php\">Back to Seaside Town</a>", "" )
			.replaceFirst( "table width=95%", "table width=100%" );
	}

	public static final void handleQuestChange( final String location, final String responseText )
	{
		if ( location.startsWith( "adventure" ) )
		{
			if ( location.indexOf( "216" ) != -1 )
			{
				CouncilFrame.handleTrickOrTreatingChange( responseText );
			}
			else if ( KoLCharacter.getInebriety() > 25 )
			{
				CouncilFrame.handleSneakyPeteChange( responseText );
			}
		}
		if ( location.startsWith( "beanstalk" ) )
		{
			if ( responseText.contains( "airship.gif" ) )
			{
				KoLCharacter.armBeanstalk();
			}
		}
		else if ( location.startsWith( "bigisland" ) )
		{
			IslandDecorator.parseBigIsland( location, responseText );
		}
		else if ( location.startsWith( "cobbsknob.php" ) )
		{
			if ( location.indexOf( "action=cell37" ) != -1 )
			{
				CouncilFrame.handleCell37( responseText );
			}
		}
		else if ( location.startsWith( "council" ) )
		{
			CouncilFrame.handleCouncilChange( responseText );
		}
		else if ( location.startsWith( "friars" ) )
		{
			CouncilFrame.handleFriarsChange( responseText );
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
				CouncilFrame.handlePlainsChange( responseText );
			}
			else if ( location.contains( "whichplace=mclargehuge" ) )
			{
				if ( location.contains( "action=trappercabin" ) )
				{
					CouncilFrame.handleTrapperChange( responseText );
				}
				else if ( location.contains( "action=cloudypeak" ) )
				{
					CouncilFrame.handleMcLargehugeChange( responseText );
				}
			}
			else if ( location.contains( "whichplace=orc_chasm" ) )
			{
				CouncilFrame.handleChasmChange( responseText );
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
			CouncilFrame.parsePyramidChange( responseText );
		}
		else if ( location.startsWith( "tavern" ) )
		{
			TavernManager.handleTavernChange( responseText );
		}
		else if ( location.startsWith( "trickortreat" ) )
		{
			CouncilFrame.handleTrickOrTreatingChange( responseText );
		}
		else if ( location.startsWith( "woods" ) )
		{
			CouncilFrame.handleWoodsChange( responseText );
		}
		// Obsolete. Sigh.
		else if ( location.startsWith( "generate15" ) )
		{
			// You slide the last tile into place ...

			if ( AdventureRequest.registerDemonName( "Strange Cube", responseText ) ||
			     responseText.indexOf( "slide the last tile" ) != -1 )
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
			ResultProcessor.processItem( ItemPool.MORNINGWOOD_PLANK, -1 * InventoryManager.getCount( ItemPool.MORNINGWOOD_PLANK ) );
			ResultProcessor.processItem( ItemPool.HARDWOOD_PLANK, -1 * InventoryManager.getCount( ItemPool.HARDWOOD_PLANK ) );
			ResultProcessor.processItem( ItemPool.WEIRWOOD_PLANK, -1 * InventoryManager.getCount( ItemPool.WEIRWOOD_PLANK ) );
			ResultProcessor.processItem( ItemPool.THICK_CAULK, -1 * InventoryManager.getCount( ItemPool.THICK_CAULK ) );
			ResultProcessor.processItem( ItemPool.LONG_SCREW, -1 * InventoryManager.getCount( ItemPool.LONG_SCREW ) );
			ResultProcessor.processItem( ItemPool.BUTT_JOINT, -1 * InventoryManager.getCount( ItemPool.BUTT_JOINT ) );
			if ( KoLmafia.isAdventuring() )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "You have bridged the Orc Chasm." );
			}
			QuestDatabase.setQuestProgress( Quest.LOL, "step1" );
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
		Matcher oreMatcher = CouncilFrame.ORE_PATTERN.matcher( responseText );
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
			QuestDatabase.setQuestIfBetter( Quest.TRAPPER, "step3" );
		}

		// Yeehaw!  I heard the noise and seen them mists dissapatin' from clear down here!  Ya done it!  Ya rightly done it!
		else if ( responseText.contains( "Yeehaw!  I heard the noise" ) )
		{
			Preferences.setInteger( "lastTr4pz0rQuest", KoLCharacter.getAscensions() );
			QuestDatabase.setQuestProgress( Quest.TRAPPER, QuestDatabase.FINISHED );
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
}
