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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
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
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.request.BasementRequest;

import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.GoalManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TavernManager;
import net.sourceforge.kolmafia.session.TurnCounter;

import net.sourceforge.kolmafia.swingui.RequestSynchFrame;

import net.sourceforge.kolmafia.utilities.StringUtilities;

import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.CellarDecorator;
import net.sourceforge.kolmafia.webui.DungeonDecorator;
import net.sourceforge.kolmafia.webui.DvorakDecorator;

public class AdventureRequest
	extends GenericRequest
{
	private static final Pattern AREA_PATTERN = Pattern.compile( "(adv|snarfblat)=(\\d*)", Pattern.DOTALL );
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );

	private static final Pattern MONSTER_NAME = Pattern.compile( "<span id='monname'(>| title=\")([^<]*?)(</span>|\">)", Pattern.DOTALL );
	private static final Pattern HAIKU_MONSTER_NAME = Pattern.compile(
		"<b>.*?<b>([^<]+)<", Pattern.DOTALL );

	private static final Pattern MONSTER_IMAGE = Pattern.compile( "adventureimages/(.*?)\\.gif" );

	private static final GenericRequest ZONE_UNLOCK = new GenericRequest( "" );

	private final String adventureName;
	private final String formSource;
	private final String adventureId;

	private int override = -1;

	private static final AdventureResult SKELETON_KEY = ItemPool.get( ItemPool.SKELETON_KEY, 1 );

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

		if ( formSource.equals( "adventure.php" ) )
		{
			this.addFormField( "snarfblat", adventureId );
		}
		else if ( formSource.equals( "shore.php" ) )
		{
			this.addFormField( "whichtrip", adventureId );
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
		else if ( formSource.equals( "place.php" ) && adventureId.equals( "cloudypeak2" ) )
		{
			this.addFormField( "whichplace", "mclargehuge" );
			this.addFormField( "action", adventureId );
		}
		else if ( !formSource.equals( "dungeon.php" ) &&
			  !formSource.equals( "basement.php" ) &&
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

		else if ( this.formSource.equals( "shore.php" ) )
		{
			int adv = KoLCharacter.inFistcore() ? 5 : 3;
			if ( KoLCharacter.getAdventuresLeft() < adv )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "Ran out of adventures." );
				return;
			}
		}

		if ( this.formSource.equals( "dungeon.php" ) )
		{
			this.data.clear();
		}

		else if ( this.formSource.equals( "adventure.php" ) && this.adventureId.equals( "120" ) )
		{
			// Replace with a (not-so-)randomly chosen corner
			this.removeFormField( "snarfblat" );
			this.addFormField( "snarfblat", String.valueOf( CellarDecorator.recommendCorner() ) );
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
				KoLmafia.updateDisplay( MafiaState.ERROR, "Don't know which square to visit in the Tavern Cellar." );
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

		super.run();

		if ( this.responseCode != 200 )
		{
			return;
		}

		if ( this.formSource.equals( "dungeon.php" ) )
		{
			this.addFormField( "action", "Yep." );

			if ( this.responseText.indexOf( "Locked Door" ) != -1 && AdventureRequest.SKELETON_KEY.getCount( KoLConstants.inventory ) + AdventureRequest.SKELETON_KEY.getCount( KoLConstants.closet ) > 1 )
			{
				ResultProcessor.processResult( AdventureRequest.SKELETON_KEY.getInstance( -1 ) );
				this.addFormField( "option", "2" );
			}
			else if ( this.responseText.indexOf( "\"Move on\"" ) != -1 )
			{
				this.addFormField( "option", "2" );
			}
			else
			{
				this.addFormField( "option", "1" );
			}

			super.run();
		}
	}

	@Override
	public void processResults()
	{
		// Sometimes, there's no response from the server.
		// In this case, skip and continue onto the next one.

		if ( this.responseText == null || this.responseText.trim().length() == 0 )
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

		if ( this.formSource.equals( "dungeon.php" ) && this.responseText.indexOf( "You have reached the bottom of today's Dungeon" ) != -1 )
		{
			KoLmafia.updateDisplay( MafiaState.PENDING, "Daily dungeon completed." );
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

		if ( this.formSource.equals( "adventure.php" ) &&
		     this.responseText.indexOf( "adventure.php" ) == -1 &&
		     this.responseText.indexOf( "You acquire" ) == -1 )
		{
			if ( !KoLmafia.isAutoStop( this.encounter ) )
			{
				KoLmafia.updateDisplay( MafiaState.PENDING, "Nothing more to do here." );
			}

			return;
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

	public static final void handleShoreVisit( final String location, final String responseText )
	{
		if ( location.indexOf( "whichtrip" ) == -1 )
		{
			return;
		}

		// You're too drunk to go on vacation. Which makes sense,
		// somehow. Trust me.
		//
		// What? Where? Huh?
		//
		// You can't afford to go on a vacation.
		//
		// You don't have enough Adventures left

		if ( responseText.indexOf( "You're too drunk" ) != -1 ||
		     responseText.indexOf( "What? Where? Huh?" ) != -1 ||
		     responseText.indexOf( "You can't afford" ) != -1 ||
		     responseText.indexOf( "You don't have enough Adventures left" ) != -1 )
		{
			return;
		}

		// Shore Trips cost 500 meat each
		if ( !KoLCharacter.inFistcore() )
		{
			ResultProcessor.processMeat( -500 );
		}

		// If we did not get a tower item and were already counting
		// down, keep the existing counter.
		if ( TurnCounter.isCounting( "The Shore" ) &&
		     responseText.indexOf( "<b>barbed-wire fence</b>" ) == -1 &&
		     responseText.indexOf( "<b>tropical orchid</b>" ) == -1 &&
		     responseText.indexOf( "<b>stick of dynamite</b>" ) == -1 )
		{
			return;
		}

		// Start a counter to allow people to time getting tower items.
		// We want an interval of 35. This is called before we account
		// for the 3 vacations spend adventuring.
		TurnCounter.stopCounting( "The Shore" );
		int adv = KoLCharacter.inFistcore() ? 5 : 3;
		TurnCounter.startCounting( 35 + adv, "The Shore loc=* shore.php", "dinghy.gif" );
	}

	public static final String registerEncounter( final GenericRequest request )
	{
		String urlString = request.getURLString();
		String responseText = request.responseText;

		// If we were redirected into a fight or a choice through using
		// an item, there will be an encounter in the responseText.
		// Otherwise, if KoLAdventure didn't log the location, there
		// can't be an encounter for us to log.

		if ( !request.isChatRequest && GenericRequest.itemMonster == null && !KoLAdventure.recordToSession( urlString, responseText ) )
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
			Matcher matcher = AdventureRequest.CHOICE_PATTERN.matcher( responseText );
			int choice = 0;
			if ( matcher.find() )
			{
				choice = StringUtilities.parseInt( matcher.group(1) );
			}
			encounter = parseChoiceEncounter( urlString, choice, responseText );
			type = choiceType( choice );
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
			}
			KoLmafia.registerEncounter( encounter, type, responseText );
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

	private static final String parseCombatEncounter( final String responseText )
	{
		String name;
		Matcher matcher = MONSTER_NAME.matcher( responseText );
		if ( !matcher.find() )
		{
			matcher = HAIKU_MONSTER_NAME.matcher( responseText );
			if ( !matcher.find() )
			{
				return "";
			}
			name = matcher.group(1);
		}
		else
		{
			name = matcher.group(2);
		}
		name = CombatActionManager.encounterKey( name, false );
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
				// Hobopolis
				image.startsWith( "nhobo" ) ? "Normal Hobo" :
				image.startsWith( "hothobo" ) ? "Hot Hobo" :
				image.startsWith( "coldhobo" ) ? "Cold Hobo" :
				image.startsWith( "stenchhobo" ) ? "Stench Hobo" :
				image.startsWith( "spookyhobo" ) ? "Spooky Hobo" :
				image.startsWith( "slhobo" ) ? "Sleaze Hobo" :
				// Slime Tube
				image.startsWith( "slime" ) ? "Slime" + image.charAt( 5 ) :
				// Crimbo 2012 wandering elves
				image.equals( "tacoelf_sign" ) ? "Sign-Twirling Crimbo Elf" :
				image.equals( "tacoelf_taco" ) ? "Taco-Clad Crimbo Elf" :
				image.equals( "tacoelf_cart" ) ? "Tacobuilding Crimbo Elf" :
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
				null;
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
		if ( choice == 443 && urlString.indexOf( "xy" ) != -1 )
		{
			return null;
		}

		// No "encounter" for certain arcade games
		if ( ArcadeRequest.arcadeChoice( choice ) )
		{
			return null;
		}

		// No "encounter" for Safety Shelter maps
		if ( choice == 535 || choice == 536 )
		{
			return null;
		}

		// No "encounter" for Vamping Out
		if ( choice == 546 )
		{
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

		// Daily Dungeon
		if ( urlString.startsWith( "dungeon.php" ) )
		{
			DungeonDecorator.checkDungeon( responseText );
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

			case 109:
				// Haunted Ballroom
				// We'll all be flat
				// You make your way to the pipe organ at the end of the ballroom. You straighten your
				// coat, crack your knuckles, and begin to play the sheet music from the diary.
				if ( responseText.indexOf ( "You make your way to the pipe organ" ) != -1 )
				{
					QuestDatabase.setQuestProgress( Quest.MANOR, "step1" );
				}
				break;
			case 113:
				// Haunted Pantry
				// The Manor in Which You're Accustomed
				// At the back of the Haunted Pantry, you encounter a door. You open it, and come to the
				// realization that the Haunted Pantry is actually connected to an entire house.
				//
				// You'd think you would've noticed that sooner!
				if ( responseText.indexOf( "think you would've noticed that sooner" ) != -1 )
				{
					Preferences.setInteger( "lastManorUnlock", KoLCharacter.getAscensions() );
				}
				break;
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
			case 121:
				// Arid desert, ultrahydrated

				// A sietch in time
				// You tramp through the desert for a while, not finding anything that looks like a
				// pyramid. However, you do see what looks like a collection of small buildings off in
				// the distance, and start heading toward them.

				if ( responseText.indexOf( "do see what looks like a collection of small buildings" ) != -1 )
				{
					QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step2" );
				}

				// No Colors Anymore
				// no black paint: "Okay, fine," you sigh, and trudge off to find some black paint.
				else if ( responseText.indexOf( "trudge off to find some black paint" ) != -1 )
				{
					QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step3" );
				}

				// with black paint: "Okay, fine," you sigh, and pull a can of black paint out of your
				// pack. Gnasir lends you a brush, and you paint the door black as night, black as coal.

				// Also occurs in Still No Colors Anymore
				else if ( responseText.indexOf( "you paint the door black as night, black as coal" ) != -1 )
				{
					QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step4" );
				}

				// Walk Without Rhythm

				// no drum machine: Grumbling, you head back toward the oasis.
				else if ( responseText.indexOf( "Grumbling, you head back toward the oasis" ) != -1 )
				{
					QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step5" );
				}

				// with drum machine: ... show your drum machine to
				// Gnasir. He nods approvingly. "Well chosen. This thumper will serve you well.

				// also happens in Walk Without Rhythm 2
				else if ( responseText.indexOf( "This thumper will serve you well" ) != -1 )
				{
					QuestDatabase.setQuestIfBetter( Quest.PYRAMID, "step6" );
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
		int boldIndex = responseText.indexOf( "Results:</b>", index );

		// If there are none, start in the HTML body again.
		if ( boldIndex == -1 )
		{
			boldIndex = index;
		}

		boldIndex = responseText.indexOf( "<b>", boldIndex ) + 3;
		if ( boldIndex == 2 )
		{
			return "";
		}

		int endBoldIndex = responseText.indexOf( "</b>", boldIndex );

		if ( endBoldIndex == -1 )
		{
			return "";
		}

		return responseText.substring( boldIndex, endBoldIndex );
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
		// The first round is unique in that there is no
		// data fields.	 Therefore, it will equal fight.php
		// exactly every single time.

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
			return responseText.indexOf( "choice.php" ) != -1;
		}
		else if ( formSource.startsWith( "hiddencity.php" ) )
		{
			return formSource.indexOf( "which=" ) != -1;
		}
		else if ( formSource.startsWith( "cave.php" ) )
		{
			return formSource.indexOf( "sanctum" ) != -1;
		}
		else if ( formSource.startsWith( "shore.php" ) )
		{
			return formSource.indexOf( "whichtrip" ) != -1;
		}
		else if ( formSource.startsWith( "cobbsknob.php" ) )
		{
			return formSource.indexOf( "throneroom" ) != -1;
		}
		else if ( formSource.startsWith( "cyrpt.php" ) )
		{
			return formSource.indexOf( "action" ) != -1;
		}
		else if ( formSource.startsWith( "cellar.php" ) )
		{
			// Simply visiting the map is not an encounter.
			return !formSource.equals( "cellar.php" );
		}
		else if ( formSource.startsWith( "palinshelves.php" ) )
		{
			return responseText.indexOf( "palinshelves.php" ) != -1;
		}
		else if ( formSource.startsWith( "suburbandis.php" ) )
		{
			return formSource.indexOf( "action=dothis" ) != -1;
		}
		else if ( formSource.startsWith( "tiles.php" ) )
		{
			// Only register initial encounter of Dvorak's Revenge
			DvorakDecorator.saveResponse( responseText );
			return responseText.indexOf( "I before E, except after C" ) != -1;
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
		if ( this.adventureId.equals( "123" ) )
		{	// Desert (Ultrahydrated) may also visit the Oasis
			return KoLConstants.activeEffects.contains(
				EffectPool.get( Effect.HYDRATED ) ) ? 1 : 2;
		}
		String zone = AdventureDatabase.getZone( this.adventureName );
		if ( zone != null && zone.equals( "The Sea" ) )
		{
			return KoLConstants.activeEffects.contains(
				EffectPool.get( Effect.FISHY ) ) ? 1 : 2;
		}
		return this.formSource.startsWith( "shore" ) ? 3 : 1;
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

		RequestSynchFrame.showRequest( AdventureRequest.ZONE_UNLOCK );
		KoLmafia.updateDisplay( MafiaState.ABORT, "Unknown adventure type encountered." );
	}

	public static final void handleDvoraksRevenge( final GenericRequest request )
	{
		KoLmafia.registerEncounter( "Dvorak's Revenge", "Noncombat", null );
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
