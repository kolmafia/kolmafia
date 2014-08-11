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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

import net.sourceforge.kolmafia.combat.CombatActionManager;

import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.EffectPool.Effect;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TavernManager;

import net.sourceforge.kolmafia.swingui.RequestSynchFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.DvorakDecorator;

public class AdventureRequest
	extends GenericRequest
{
	private static final Pattern AREA_PATTERN = Pattern.compile( "(adv|snarfblat)=(\\d*)", Pattern.DOTALL );

	private static final Pattern MONSTER_IMAGE = Pattern.compile( "adventureimages/(.*?)\\.gif" );

	private static final GenericRequest ZONE_UNLOCK = new GenericRequest( "" );

	private final String adventureName;
	private final String formSource;
	private final String adventureId;

	private int override = -1;

	/**
	 * Constructs a new <code>AdventureRequest</code> which executes the adventure designated by the given Id by
	 * posting to the provided form, notifying the givenof results (or errors).
	 *
	 * @param adventureName The name of the adventure location
	 * @param formSource The form to which the data will be posted
	 * @param adventureId The identifier for the adventure to be executed
	 */

	public AdventureRequest( final String adventureName, final String formSource, final String adventureId )
	{
		super( formSource );
		this.adventureName = adventureName;
		this.formSource = formSource;
		this.adventureId = adventureId;

		// The adventure Id is all you need to identify the adventure;
		// posting it in the form sent to adventure.php will handle
		// everything for you.
		// Those that change mid session should be added to run() also.

		if ( formSource.equals( "adventure.php" ) )
		{
			this.addFormField( "snarfblat", adventureId );
		}
		else if ( formSource.equals( "casino.php" ) )
		{
			this.addFormField( "action", "slot" );
			this.addFormField( "whichslot", adventureId );
		}
		else if ( formSource.equals( "crimbo10.php" ) )
		{
			this.addFormField( "place", adventureId );
		}
		else if ( formSource.equals( "cobbsknob.php" ) )
		{
			this.addFormField( "action", "throneroom" );
		}
		else if ( formSource.equals( "mountains.php" ) )
		{
			this.addFormField( "orcs", "1" );
		}
		else if ( formSource.equals( "friars.php" ) )
		{
			this.addFormField( "action", "ritual" );
		}
		else if ( formSource.equals( "lair6.php" ) )
		{
			this.addFormField( "place", adventureId );
		}
		else if ( formSource.equals( "invasion.php" ) )
		{
			this.addFormField( "action", adventureId );
		}
		else if ( formSource.equals( "manor3.php" ) )
		{
			this.addFormField( "place", "chamber" );
		}
		else if ( formSource.equals( "mining.php" ) )
		{
			this.addFormField( "mine", adventureId );
		}
		else if ( formSource.equals( "place.php" ) && adventureId.equals( "cloudypeak2" ) )
		{
			this.addFormField( "whichplace", "mclargehuge" );
			this.addFormField( "action", adventureId );
		}
		else if ( this.formSource.equals( "place.php" ) && this.adventureId.equals( "pyramid_state" ) )
		{
			this.addFormField( "whichplace", "pyramid" );
			StringBuffer action = new StringBuffer();
			action.append( adventureId );
			action.append( Preferences.getString( "pyramidPosition" ) );
			if ( Preferences.getBoolean( "pyramidBombUsed" ) )
			{
				action.append( "a" );
			}
			this.addFormField( "action", action.toString() );
		}
		else if ( this.formSource.equals( "place.php" ) && this.adventureId.equals( "manor4_chamber" ) )
		{
			this.addFormField( "whichplace", "manor4" );
			if ( !QuestDatabase.isQuestFinished( Quest.MANOR ) )
			{
				this.addFormField( "action", "manor4_chamberboss" );
			}
			else
			{
				this.addFormField( "action", "manor4_chamber" );
			}
		}
		else if ( !formSource.equals( "basement.php" ) &&
			  !formSource.equals( "cellar.php" ) &&
			  !formSource.equals( "barrel.php" ) )
		{
			this.addFormField( "action", adventureId );
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		// Prevent the request from happening if they attempted
		// to cancel in the delay period.

		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		if ( this.formSource.equals( "mountains.php" ) )
		{
			AdventureRequest.ZONE_UNLOCK.constructURLString( "mountains.php" ).run();
			if ( AdventureRequest.ZONE_UNLOCK.responseText.indexOf( "value=80" ) != -1 )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "The Orc Chasm has already been bridged." );
				return;
			}
		}

		else if ( this.formSource.equals( "adventure.php" ) )
		{
			if ( this.adventureId.equals( AdventurePool.THE_SHORE_ID ) )
			{
				// The Shore
				int adv = KoLCharacter.inFistcore() ? 5 : 3;
				if ( KoLCharacter.getAdventuresLeft() < adv )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Ran out of adventures." );
					return;
				}
			}
		}

		else if ( this.formSource.equals( "barrel.php" ) )
		{
			int square = BarrelDecorator.recommendSquare();
			if ( square == 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR,
					"All booze in the specified rows has been collected." );
				return;
			}
			this.addFormField( "smash", String.valueOf( square ) );
		}

		else if ( this.formSource.equals( "cellar.php" ) )
		{
			int square = TavernManager.recommendSquare();
			if ( square == 0 )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Don't know which square to visit in the Typical Tavern Cellar." );
				return;
			}
			this.addFormField( "whichspot", String.valueOf( square ) );
			this.addFormField( "action", "explore" );
		}

		else if ( this.formSource.equals( "mining.php" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Automated mining is not currently implemented." );
			return;
		}

		else if ( this.formSource.equals( "place.php" ) && this.adventureId.equals( "pyramid_state" ) )
		{
			this.addFormField( "whichplace", "pyramid" );
			StringBuffer action = new StringBuffer();
			action.append( adventureId );
			action.append( Preferences.getString( "pyramidPosition" ) );
			if ( Preferences.getBoolean( "pyramidBombUsed" ) )
			{
				action.append( "a" );
			}
			this.addFormField( "action", action.toString() );
		}

		else if ( this.formSource.equals( "place.php" ) && this.adventureId.equals( "manor4_chamber" ) )
		{
			this.addFormField( "whichplace", "manor4" );
			if ( !QuestDatabase.isQuestFinished( Quest.MANOR ) )
			{
				this.addFormField( "action", "manor4_chamberboss" );
			}
			else
			{
				this.addFormField( "action", "manor4_chamber" );
			}
		}

		super.run();
	}

	@Override
	public void processResults()
	{
		// Sometimes, there's no response from the server.
		// In this case, skip and continue onto the next one.

		if ( this.responseText == null || this.responseText.trim().length() == 0 ||
			this.responseText.contains( "No, that isn't a place yet." ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't get to that area yet." );
			return;
		}

		// The hedge maze should always result in you getting
		// a fight redirect.  If this is not the case, then
		// if the hedge maze is not complete, use up all their
		// pieces first, then go adventuring.

		if ( this.formSource.equals( "lair3.php" ) )
		{
			if ( InventoryManager.hasItem( HedgePuzzleRequest.HEDGE_KEY ) && InventoryManager.hasItem( HedgePuzzleRequest.PUZZLE_PIECE ) )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "Unexpected hedge maze puzzle state." );
			}

			return;
		}

		// The sorceress fight should always result in you getting
		// a fight redirect.

		if ( this.formSource.equals( "lair6.php" ) )
		{
			KoLmafia.updateDisplay( MafiaState.PENDING, "The sorceress has already been defeated." );
			return;
		}

		// If you haven't unlocked the orc chasm yet, try doing so now.

		if ( this.adventureId.equals( AdventurePool.ORC_CHASM_ID ) && this.responseText.indexOf( "You shouldn't be here." ) != -1 )
		{
			AdventureRequest bridge = new AdventureRequest( "Bridge the Orc Chasm", "mountains.php", "" );
			bridge.run();

			if ( KoLmafia.permitsContinue() )
			{
				this.run();
			}

			return;
		}

		// We're missing an item, haven't been given a quest yet, or
		// otherwise trying to go somewhere not allowed.

		int index = KoLAdventure.findAdventureFailure( this.responseText );
		if ( index >= 0 )
		{
			String failure = KoLAdventure.adventureFailureMessage( index );
			MafiaState severity = KoLAdventure.adventureFailureSeverity( index );
			KoLmafia.updateDisplay( severity, failure );
			this.override = 0;
			return;
		}

		// This is a server error. Hope for the best and repeat the
		// request.

		if ( this.responseText.indexOf( "No adventure data exists for this location" ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Server error.  Please wait and try again." );
			return;
		}

		// Nothing more to do in this area

		if ( this.formSource.equals( "adventure.php" ) )
		{
			if ( this.adventureId.equals( AdventurePool.MERKIN_COLOSSEUM_ID ) )
			{
				SeaMerkinRequest.parseColosseumResponse( this.getURLString(), this.responseText );
			}

			if ( !this.responseText.contains( "adventure.php" ) &&
			     !this.responseText.contains( "You acquire" ) )
			{
				if ( !EncounterManager.isAutoStop( this.encounter ) )
				{
					KoLmafia.updateDisplay( MafiaState.PENDING, "Nothing more to do here." );
				}

				return;
			}
		}

		// If you're at the casino, each of the different slot
		// machines deducts meat from your tally

		if ( this.formSource.equals( "casino.php" ) )
		{
			if ( this.adventureId.equals( "1" ) )
			{
				ResultProcessor.processMeat( -5 );
			}
			else if ( this.adventureId.equals( "2" ) )
			{
				ResultProcessor.processMeat( -10 );
			}
			else if ( this.adventureId.equals( "11" ) )
			{
				ResultProcessor.processMeat( -10 );
			}
		}

		if ( this.adventureId.equals( AdventurePool.ROULETTE_TABLES_ID ) )
		{
			ResultProcessor.processMeat( -10 );
		}
		else if ( this.adventureId.equals( String.valueOf( AdventurePool.POKER_ROOM ) ) )
		{
			ResultProcessor.processMeat( -30 );
		}

		// Trick-or-treating requires a costume;
		// notify the user of this error.

		if ( this.formSource.equals( "trickortreat.php" ) && this.responseText.indexOf( "without a costume" ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You must wear a costume." );
			return;
		}
	}

	public static final String registerEncounter( final GenericRequest request )
	{
		String urlString = request.getURLString();
		String responseText = request.responseText;

		// No encounters in chat!
		if ( request.isChatRequest )
		{
			return "";
		}

		// If we were redirected into a fight or a choice through using
		// an item, there will be an encounter in the responseText.
		// Otherwise, if KoLAdventure didn't log the location, there
		// can't be an encounter for us to log.

		if ( GenericRequest.itemMonster == null && !KoLAdventure.recordToSession( urlString, responseText ) )
		{
			return "";
		}

		if ( !( request instanceof AdventureRequest ) && !AdventureRequest.containsEncounter( urlString, responseText ) )
		{
			return "";
		}

		String encounter = null;
		String type = null;

		if ( urlString.startsWith( "fight.php" ) )
		{
			encounter = parseCombatEncounter( responseText );
			type = "Combat";
		}
		else if ( urlString.startsWith( "choice.php" ) )
		{
			int choice = ChoiceManager.extractChoice( responseText );
			encounter = parseChoiceEncounter( urlString, choice, responseText );
			type = choiceType( choice );
			ChoiceManager.registerDeferredChoice( choice, encounter );
		}
		else
		{
			encounter = parseNoncombatEncounter( urlString, responseText );
			type = "Noncombat";
		}

		if ( encounter == null )
		{
			return "";
		}

		Preferences.setString( "lastEncounter", encounter );
		RequestLogger.printLine( "Encounter: " + encounter );
		RequestLogger.updateSessionLog( "Encounter: " + encounter );
		AdventureRequest.registerDemonName( encounter, responseText );

		if ( type != null )
		{
			if ( type.equals( "Combat" ) )
			{
				encounter = AdventureRequest.translateGenericType( encounter, responseText );
				AdventureQueueDatabase.enqueue( KoLAdventure.lastVisitedLocation(), encounter );
			}
			else if ( type.equals( "Noncombat" ) )
			{
				// only log the FIRST choice that we see in a choiceadventure chain.
				if ( !urlString.startsWith( "choice.php" ) || ChoiceManager.getLastChoice() == 0 )
				{
					AdventureQueueDatabase.enqueueNoncombat( KoLAdventure.lastVisitedLocation(), encounter );
				}
			}
			EncounterManager.registerEncounter( encounter, type, responseText );
		}

		return encounter;
	}

	private static String fromName = null;
	private static String toName = null;

	public static final void setNameOverride( final String from, final String to )
	{
		fromName = from;
		toName = to;
	}

	public static final String parseMonsterEncounter( final String responseText )
	{
		String encounter = AdventureRequest.parseCombatEncounter( responseText );
		return AdventureRequest.translateGenericType( encounter, responseText );
	}

	private static final Pattern [] MONSTER_NAME_PATTERNS =
	{
		Pattern.compile( "You're fighting <span id='monname'> *(.*?)</span>", Pattern.DOTALL ),
		// papier weapons can change "fighting" to some other verb
		Pattern.compile( "You're (?:<u>.*?</u>) <span id='monname'>(.*?)</span>", Pattern.DOTALL ),
		Pattern.compile( "<b>.*?(<b>.*?:?</b>.*?)<br>", Pattern.DOTALL ),
	};

	private static final String parseCombatEncounter( final String responseText )
	{
		String name = null;

		for ( int i = 0; i < MONSTER_NAME_PATTERNS.length; ++i )
		{
			Matcher matcher = MONSTER_NAME_PATTERNS[i].matcher( responseText );
			if ( matcher.find() )
			{
				name = matcher.group(1);
				break;
			}
		}

		if ( name == null )
		{
			return "";
		}

		// If the name has bold markup, strip formatting
		name = StringUtilities.globalStringReplace( name, "<b>", "" );
		name = StringUtilities.globalStringReplace( name, "</b>", "" );

		// Canonicalize
		name = CombatActionManager.encounterKey( name, false );

		// Coerce name if needed
		if ( name.equalsIgnoreCase( fromName ) )
		{
			name = CombatActionManager.encounterKey( toName, false );
		}
		fromName = null;

		EquipmentManager.decrementTurns();
		return name;
	}

	private static final String translateGenericType( final String encounter, final String responseText )
	{
		if ( KoLAdventure.lastLocationName != null &&
		     KoLAdventure.lastLocationName.startsWith( "Fernswarthy's Basement" ) )
		{
			return BasementRequest.basementMonster;
		}

		if ( MonsterDatabase.findMonster( encounter, false ) != null )
		{
			return encounter;
		}

		// For monsters that have a randomly-generated name, identify them by the image they use instead

		String override = null;
		String image = null;

		Matcher monster = AdventureRequest.MONSTER_IMAGE.matcher( responseText );
		if ( monster.find() )
		{
			image = monster.group( 1 );
		}

		if ( image != null )
		{
			// Always-available monsters are listed above obsolete monsters
			// to get a quicker match on average.  Obsolete monsters can
			// still be fought due to the Fax Machine.  Due to monster copying,
			// any of these monsters can show up in any zone, or in no zone.
			override =
				// Your Shadow
				image.startsWith( "shadowsealclubber" ) ? "Your Shadow" :
				image.startsWith( "shadowturtletamer" ) ? "Your Shadow" :
				image.startsWith( "shadowpastamancer" ) ? "Your Shadow" :
				image.startsWith( "shadowsauceror" ) ? "Your Shadow" :
				image.startsWith( "shadowdiscobandit" ) ? "Your Shadow" :
				image.startsWith( "shadowaccordionthief" ) ? "Your Shadow" :
				image.startsWith( "shadowsealboris" ) ? "Your Shadow" :
				image.startsWith( "shadowsealjarlsberg" ) ? "Your Shadow" :
				image.startsWith( "shadowsealzombie" ) ? "Your Shadow" :
				image.startsWith( "shadowsealpete" ) ? "Your Shadow" :
				// The Copperhead Club
				image.equals( "coppertender" ) ? "Copperhead Club bartender" :
				// Spookyraven
				image.startsWith( "srpainting" ) ? "ancestral Spookyraven portrait" :
				// Spring Break Beach
				image.equals( "ssd_burger" ) ? "Sloppy Seconds Burger" :
				image.equals( "ssd_cocktail" ) ? "Sloppy Seconds Cocktail" :
				image.equals( "ssd_sundae" ) ? "Sloppy Seconds Sundae" :
				image.startsWith( "fun-gal" ) ? "Fun-Guy Playmate" :
				// The Old Landfill
				image.equals( "js_bender" ) ? "junksprite bender" :
				image.equals( "js_melter" ) ? "junksprite melter" :
				image.equals( "js_sharpener" ) ? "junksprite sharpener" :
				// Dreadsylvania
				image.startsWith( "dvcoldbear" ) ? "cold bugbear" :
				image.startsWith( "dvcoldghost" ) ? "cold ghost" :
				image.startsWith( "dvcoldskel" ) ? "cold skeleton" :
				image.startsWith( "dvcoldvamp" ) ? "cold vampire" :
				image.startsWith( "dvcoldwolf" ) ? "cold werewolf" :
				image.startsWith( "dvcoldzom" ) ? "cold zombie" :
				image.startsWith( "dvhotbear" ) ? "hot bugbear" :
				image.startsWith( "dvhotghost" ) ? "hot ghost" :
				image.startsWith( "dvhotskel" ) ? "hot skeleton" :
				image.startsWith( "dvhotvamp" ) ? "hot vampire" :
				image.startsWith( "dvhotwolf" ) ? "hot werewolf" :
				image.startsWith( "dvhotzom" ) ? "hot zombie" :
				image.startsWith( "dvsleazebear" ) ? "sleaze bugbear" :
				image.startsWith( "dvsleazeghost" ) ? "sleaze ghost" :
				image.startsWith( "dvsleazeskel" ) ? "sleaze skeleton" :
				image.startsWith( "dvsleazevamp" ) ? "sleaze vampire" :
				image.startsWith( "dvsleazewolf" ) ? "sleaze werewolf" :
				image.startsWith( "dvsleazezom" ) ? "sleaze zombie" :
				image.startsWith( "dvspookybear" ) ? "spooky bugbear" :
				image.startsWith( "dvspookyghost" ) ? "spooky ghost" :
				image.startsWith( "dvspookyskel" ) ? "spooky skeleton" :
				image.startsWith( "dvspookyvamp" ) ? "spooky vampire (Dreadsylvanian)" :
				image.startsWith( "dvspookywolf" ) ? "spooky werewolf" :
				image.startsWith( "dvspookyzom" ) ? "spooky zombie" :
				image.startsWith( "dvstenchbear" ) ? "stench bugbear" :
				image.startsWith( "dvstenchghost" ) ? "stench ghost" :
				image.startsWith( "dvstenchskel" ) ? "stench skeleton" :
				image.startsWith( "dvstenchvamp" ) ? "stench vampire" :
				image.startsWith( "dvstenchwolf" ) ? "stench werewolf" :
				image.startsWith( "dvstenchzom" ) ? "stench zombie" :
				// Hobopolis
				image.startsWith( "nhobo" ) ? "Normal Hobo" :
				image.startsWith( "hothobo" ) ? "Hot Hobo" :
				image.startsWith( "coldhobo" ) ? "Cold Hobo" :
				image.startsWith( "stenchhobo" ) ? "Stench Hobo" :
				image.startsWith( "spookyhobo" ) ? "Spooky Hobo" :
				image.startsWith( "slhobo" ) ? "Sleaze Hobo" :
				// Slime Tube
				image.startsWith( "slime" ) ? "Slime" + image.charAt( 5 ) :
				// GamePro Bosses
				image.startsWith( "faq_boss" ) ? "Video Game Boss" :
				image.startsWith( "faq_miniboss" ) ? "Video Game Miniboss" :
				// KOLHS
				image.equals( "shopteacher" ) ? "X-fingered Shop Teacher" :
				// Trick or Treat
				image.equals( "vandalkid" ) ? "vandal kid" :
				image.equals( "paulblart" ) ? "suburban security civilian" :
				image.equals( "tooold" ) ? "kid who is too old to be Trick-or-Treating" :
				// Bugbear Invasion
				image.equals( "bb_caveman" ) ? "Angry Cavebugbear" :
				// Crimbo 2012 wandering elves
				image.equals( "tacoelf_sign" ) ? "Sign-Twirling Crimbo Elf" :
				image.equals( "tacoelf_taco" ) ? "Taco-Clad Crimbo Elf" :
				image.equals( "tacoelf_cart" ) ? "Tacobuilding Crimbo Elf" :
				// Crimbobokutown Toy Factory
				image.equals( "animelf1" ) ? "Tiny-Screwing Animelf" :
				image.equals( "animelf2" ) ? "Plastic-Extruding Animelf" :
				image.equals( "animelf3" ) ? "Circuit-Soldering Animelf" :
				image.equals( "animelf4" ) ? "Quality Control Animelf" :
				image.equals( "animelf5" ) ? "Toy Assembling Animelf" :
				// Elf Alley
				image.startsWith( "elfhobo" ) ? "Hobelf" :
				// Haunted Sorority House
				image.startsWith( "sororeton" ) ? "sexy sorority skeleton" :
				image.startsWith( "sororpire" ) ? "sexy sorority vampire" :
				image.startsWith( "sororwolf" ) ? "sexy sorority werewolf" :
				image.startsWith( "sororeton" ) ? "sexy sorority skeleton" :
				image.startsWith( "sororbie" ) ? "sexy sorority zombie" :
				image.startsWith( "sororghost" ) ? "sexy sorority ghost" :
				// Lord Flameface's Castle Entryway
				image.startsWith( "fireservant" ) ? "Servant Of Lord Flameface" :
				// Abyssal Portals
				image.startsWith( "generalseal" ) ? "general seal" :
				// Crimbo 13
				image.startsWith( "warbear1" ) ? "Warbear Foot Soldier" :
				image.startsWith( "warbear2" ) ? "Warbear Officer" :
				image.startsWith( "warbear3" ) ? "High-Ranking Warbear Officer" :
				// Crashed Space Beast
				image.startsWith( "spacebeast" ) ? "space beast" :
				null;
		}

		// These monsters cannot be identified by their image
		if ( override == null )
		{
			switch ( KoLAdventure.lastAdventureId() )
			{
			// Video Game Minions need to be checked for after checking for Video Game bosses
			case 319:
				override = "Video Game Minion (weak)";
				break;
			case 320:
				override = "Video Game Minion (moderate)";
				break;
			case 321:
				override = "Video Game Minion (strong)";
				break;
			}
		}

		if ( override != null )
		{
			return override;
		}

		return encounter;
	}

	private static final String parseChoiceEncounter( final String urlString, final int choice, final String responseText )
	{
		if ( LouvreManager.louvreChoice( choice ) )
		{
			return LouvreManager.encounterName( choice );
		}

		// No "encounter" when moving on the chessboard
		if ( choice == 443 && urlString.contains( "xy" ) )
		{
		return null;
		}

		// No "encounter" for certain arcade games
		if ( ArcadeRequest.arcadeChoice( choice ) )
		{
			return null;
		}

		if ( ChoiceManager.canWalkFromChoice( choice ) )
		{
			return null;
		}

		switch ( choice )
		{
		case 535: // Deep Inside Ronald, Baby
		case 536: // Deep Inside Grimace, Bow Chick-a Bow Bow	
		case 585: // Screwing Around!
		case 595: // Fire! I... have made... fire!
		case 807: // Breaker Breaker!
			return null;
		}

		return parseEncounter( responseText );
	}

	private static final String choiceType( final int choice )
	{
		if ( LouvreManager.louvreChoice( choice ) )
		{
			return null;
		}

		return "Noncombat";
	}

	private static String[][] LIMERICKS =
	{
		{ "Nantucket Snapper", "ancient old turtle" },
		{ "The Apathetic Lizardman", "lizardman quite apathetic" },
		{ "The Bleary-Eyed Cyclops", "bleary-eyed cyclops" },
		{ "The Crass Goblin", "goblin is crass" },
		{ "The Crotchety Wizard", "crotchety wizard" },
		{ "The Dumb Minotaur", "dumb minotaur" },
		{ "The Fierce-Looking Giant", "fierce-looking giant" },
		{ "The Gelatinous Cube", "gelatinous cube" },
		{ "The Gnome with a Truncheon", "gnome with a truncheon" },
		{ "The Goblin King's Vassal", "Goblin King's vassal" },
		{ "The Insatiable Maiden", "insatiable maiden" },
		{ "The Jewelry Gnoll", "bejeweled it" },
		{ "The Martini Booth", "martini booth" },
		{ "The One-Legged Trouser", "one-pantlegged schnauzer" },
		{ "The Orc With a Spork", "waving a spork" },
		{ "The Slime Puddle", "slime puddle" },
		{ "The Sozzled Old Dragon", "sozzled old dragon" },
		{ "The Superior Ogre", "I am superior" },
		{ "The Unguarded Chest", "chest full of meat" },
		{ "The Unpleasant Satyr", "unpleasant satyr" },
		{ "The Vampire Buffer", "vampire buffer" },
		{ "The Weathered Old Chap", "weathered old chap" },
		{ "The Witch", "A witch" },
		{ "Thud", "hobo glyphs" },
	};

	private static final String parseNoncombatEncounter( final String urlString, final String responseText )
	{
		// Fernswarthy's Basement
		if ( urlString.startsWith( "basement.php" ) )
		{
			return null;
		}

		if ( urlString.startsWith( "adventure.php" ) )
		{
			int area = parseArea( urlString );
			switch ( area )
			{
			case 17:
				// Hidden Temple
				// Dvorak's revenge
				// You jump to the last letter, and put your pom-poms down with a sign of relief --
				// thank goodness that's over. Worst. Spelling bee. Ever.
				if ( responseText.indexOf ( "put your pom-poms down" ) != -1 )
				{
					QuestDatabase.setQuestProgress( Quest.WORSHIP, "step2" );
				}
				break;

			case 19:
				// Limerick Dungeon
				for ( int i = 0; i < LIMERICKS.length; ++i )
				{
					if ( responseText.indexOf( LIMERICKS[i][1] ) != -1 )
					{
						return LIMERICKS[i][0];
					}
				}
				return "Unrecognized Limerick";

			case 114:	// Outskirts of The Knob
				// Unstubbed
				// You go back to the tree where the wounded Knob Goblin guard was resting,
				// and find him just where you left him, continuing to whine about his stubbed toe.
				//
				// "Here you go, tough guy" you say, and hand him the unguent.
				if ( responseText.indexOf( "you say, and hand him the unguent" ) != -1 )
				{
					ResultProcessor.processItem( ItemPool.PUNGENT_UNGUENT, -1 );
				}
				break;
			}
		}
		else if ( urlString.startsWith( "barrel.php" ) )
		{
			// Without this special case, encounter names in the Barrels would
			// be things like "bottle of rum"
			return "Barrel Smash";
		}

		String encounter = parseEncounter( responseText );

		if ( encounter != null )
		{
			return encounter;
		}

		return null;
	}

	private static final String parseEncounter( final String responseText )
	{
		// Look only in HTML body; the header can have scripts with
		// bold text.
		int index = responseText.indexOf( "<body>" );

		// Skip past the Adventure Results
		int brIndex = responseText.indexOf( "Results:</b>", index );
		if ( brIndex != -1 )
		{
			int resultsIndex = responseText.indexOf( "<div id=\"results\">", index );
			if ( resultsIndex != -1 )
			{
				// KoL was nice enough to put results into a div for us
				index = responseText.indexOf( "</div>", resultsIndex );
			}
			else
			{
				// There is no results div, but it doesn't mean that
				// there aren't results. Nothing like consistency. Not.
				index = brIndex;
			}
		}

		int boldIndex = responseText.indexOf( "<b>", index );
		if ( boldIndex == -1 )
		{
			return "";
		}

		int endBoldIndex = responseText.indexOf( "</b>", boldIndex );

		if ( endBoldIndex == -1 )
		{
			return "";
		}

		return responseText.substring( boldIndex + 3, endBoldIndex );
	}

	private static final int parseArea( final String urlString )
	{
		Matcher matcher = AREA_PATTERN.matcher( urlString );
		if ( matcher.find() )
		{
			return StringUtilities.parseInt( matcher.group(2) );
		}

		return -1;
	}

	private static final Object[][] demons =
	{
		{
			"Summoning Chamber",
			Pattern.compile( "Did you say your name was (.*?)\\?" ),
			"delicious-looking pies",
			"demonName1",
		},
		{
			"Hoom Hah",
			Pattern.compile( "(.*?)! \\1, cooooome to meeeee!" ),
			"fifty meat",
			"demonName2",
		},
		{
			"Every Seashell Has a Story to Tell If You're Listening",
			Pattern.compile( "Hello\\? Is (.*?) there\\?" ),
			"fish-guy",
			"demonName3",
		},
		{
			"Leavesdropping",
			Pattern.compile( "(.*?), we call you! \\1, come to us!" ),
			"bullwhip",
			"demonName4",
		},
		{
			"These Pipes... Aren't Clean!",
			Pattern.compile( "Blurgle. (.*?). Gurgle. By the way," ),
			"coprodaemon",
			"demonName5",
		},
		{
			"Flying In Circles",
			// SC: Then his claws slip, and he falls
			// backwards.<p>"<Demon Name>!" he screams as he
			// tumbles backwards. "LORD OF REVENGE! GIVE ME
			// STRENGTH!"
			//
			// TT: With a scrape, her sickle slips from the
			// rock.<p>"<Demon Name>" she shrieks as she plummets
			// toward the lava. "Lord of Revenge! I accept your
			// contract! Give me your power!"
			//
			// PA: Its noodles lose their grip, and the evil
			// pastaspawn falls toward the churning
			// lava.<p><i>"<Demon Name>!"</i> it howls. "<i>Lord of
			// Revenge! Come to my aid!</i>"
			//
			// SA: As it falls, a mouth opens on its surface and
			// howls: "<Demon Name>! Revenge!"
			//
			// DB: His grip slips, and he falls.<p>"<Demon Name>!
			// Lord of Revenge! I call to you!  I pray to you! Help
			// m--"
			//
			// AT: His grip slips, and he tumbles
			// backward.<p>"<Demon Name>!" he screams. "Emperador
			// de la Venganza! Come to my aid!  I beg of you!"

			Pattern.compile( "(?:he falls backwards|her sickle slips from the rock|falls toward the churning lava|a mouth opens on its surface and howls|His grip slips, and he falls|he tumbles backward).*?(?:<i>)?&quot;(.*?)!?&quot;(?:</i>)?(?: he screams| she shrieks| it howls| Revenge| Lord of Revenge)" ),
			"Lord of Revenge",
			"demonName8",
		},
		{
			"Sinister Ancient Tablet",
			Pattern.compile( "<font.*?color=#cccccc>(.*?)</font>" ),
			"flame-wreathed mouth",
			"demonName9",
		},
		{
			"Strange Cube",
			Pattern.compile( "Come to me! Come to (.*?)!" ),
			"writhing and twisting snake",
			"demonName10",
		},
	};

	private static final Pattern NAME_PATTERN = Pattern.compile( "<b>&quot;(.*?)&quot;</b>" );

	public static final boolean registerDemonName( final String encounter, final String responseText )
	{
		String place = null;
		String demon = null;
		String setting = null;

		for ( int i = 0; i < AdventureRequest.demons.length; ++i )
		{
			Object [] demons = AdventureRequest.demons[ i ];
			place = (String) demons[ 0 ];
			if ( place == null || !place.equals( encounter ) )
			{
				continue;
			}

			Pattern pattern = (Pattern) demons[ 1 ];
			Matcher matcher = pattern.matcher( responseText );

			if ( matcher.find() )
			{
				// We found the name
				demon = matcher.group( 1 );
				setting = (String) demons[ 3 ];
			}

			break;
		}

		// If we didn't recognize the demon and he used a valid name in
		// the Summoning Chamber, we can deduce which one it is from
		// the result text

		if ( setting == null && encounter.equals( "Summoning Chamber" ) )
		{
			place = encounter;
			Matcher matcher = AdventureRequest.NAME_PATTERN.matcher( responseText );
			if ( !matcher.find() )
			{
				return false;
			}

			// Save the name he used.
			demon = matcher.group( 1 );

			// Look for tell-tale string
			for ( int i = 0; i < AdventureRequest.demons.length; ++i )
			{
				Object [] demons = AdventureRequest.demons[ i ];
				String text = (String) demons[ 2 ];
				if ( responseText.indexOf( text ) != -1 )
				{
					setting = (String) demons[ 3 ];
					break;
				}
			}
		}

		// Couldn't figure out which demon he called.
		if ( setting == null )
		{
			return false;
		}

		String previousName = Preferences.getString( setting );
		if ( previousName.equals( demon ) )
		{
			// Valid demon name
			return true;
		}

		RequestLogger.printLine( "Demon name: " + demon );
		RequestLogger.updateSessionLog( "Demon name: " + demon );
		Preferences.setString( setting, demon );

		GoalManager.checkAutoStop( place );

		// Valid demon name
		return true;
	}

	private static final boolean containsEncounter( final String formSource, final String responseText )
	{
		if ( formSource.startsWith( "adventure.php" ) )
		{
			return true;
		}
		else if ( formSource.startsWith( "fight.php" ) )
		{
			return FightRequest.getCurrentRound() == 0;
		}
		else if ( formSource.startsWith( "choice.php" ) )
		{
			return responseText.contains( "choice.php" );
		}
		else if ( formSource.startsWith( "cave.php" ) )
		{
			return formSource.contains( "sanctum" );
		}
		else if ( formSource.startsWith( "cobbsknob.php" ) )
		{
			return formSource.contains( "throneroom" );
		}
		else if ( formSource.startsWith( "crypt.php" ) )
		{
			return formSource.contains( "action" );
		}
		else if ( formSource.startsWith( "cellar.php" ) )
		{
			// Simply visiting the map is not an encounter.
			return !formSource.equals( "cellar.php" );
		}
		else if ( formSource.startsWith( "suburbandis.php" ) )
		{
			return formSource.contains( "action=dothis" );
		}
		else if ( formSource.startsWith( "tiles.php" ) )
		{
			// Only register initial encounter of Dvorak's Revenge
			DvorakDecorator.saveResponse( responseText );
			return responseText.contains( "I before E, except after C" );
		}
		else if ( formSource.startsWith( "barrel.php?smash" ) )
		{
			return true;
		}

		// It is not a known adventure.	 Therefore,
		// do not log the encounter yet.

		return false;
	}

	@Override
	public int getAdventuresUsed()
	{
		if ( this.override >= 0 )
		{
			return this.override;
		}
		if ( this.adventureId.equals( AdventurePool.THE_SHORE_ID ) )
		{
			return KoLCharacter.inFistcore() ? 5 : 3;
		}
		String zone = AdventureDatabase.getZone( this.adventureName );
		if ( zone != null && ( zone.equals( "The Sea" ) || this.adventureId.equals( AdventurePool.YACHT_ID ) ) )
		{
			return KoLConstants.activeEffects.contains( EffectPool.get( Effect.FISHY ) ) ? 1 : 2;
		}
		return 1;
	}

	public void overrideAdventuresUsed( int used )
	{
		this.override = used;
	}

	@Override
	public String toString()
	{
		return this.adventureName;
	}

	public static final void handleServerRedirect( final String redirectLocation )
	{
		if ( redirectLocation.indexOf( "main.php" ) != -1 )
		{
			return;
		}

		AdventureRequest.ZONE_UNLOCK.constructURLString( redirectLocation );

		if ( redirectLocation.indexOf( "palinshelves.php" ) != -1 )
		{
			AdventureRequest.ZONE_UNLOCK.run();
			AdventureRequest.ZONE_UNLOCK.constructURLString(
				"palinshelves.php?action=placeitems&whichitem1=2259&whichitem2=2260&whichitem3=493&whichitem4=2261" ).run();
			return;
		}

		if ( redirectLocation.indexOf( "tiles.php" ) != -1 )
		{
			AdventureRequest.handleDvoraksRevenge( AdventureRequest.ZONE_UNLOCK );
			return;
		}

		if ( redirectLocation.contains( "place.php" ) )
		{
			AdventureRequest.ZONE_UNLOCK.run();
			// Don't error out if it's just a redirect to a container zone
			// eg. Using grimstone mask, with choice adventure autoselected
			return;
		}

		RequestSynchFrame.showRequest( AdventureRequest.ZONE_UNLOCK );
		KoLmafia.updateDisplay( MafiaState.ABORT, "Unknown adventure type encountered." );
	}

	public static final void handleDvoraksRevenge( final GenericRequest request )
	{
		EncounterManager.registerEncounter( "Dvorak's Revenge", "Noncombat", null );
		RequestLogger.printLine( "Encounter: Dvorak's Revenge" );
		RequestLogger.updateSessionLog( "Encounter: Dvorak's Revenge" );

		request.run();
		request.constructURLString( "tiles.php?action=jump&whichtile=4" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=6" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=3" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=5" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=7" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=6" ).run();
		request.constructURLString( "tiles.php?action=jump&whichtile=3" ).run();
	}
}
