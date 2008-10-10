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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LouvreManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.SorceressLairManager;
import net.sourceforge.kolmafia.swingui.RequestSynchFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.BarrelDecorator;
import net.sourceforge.kolmafia.webui.CellarDecorator;
import net.sourceforge.kolmafia.webui.DungeonDecorator;

public class AdventureRequest
	extends GenericRequest
{
	private static final Pattern AREA_PATTERN = Pattern.compile( "(adv|snarfblat)=(\\d*)", Pattern.DOTALL );
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );

	private static final Pattern MONSTER_NAME = Pattern.compile( "<span id='monname'(>| title=\")([^<\"]*?)(</span>|\">)", Pattern.DOTALL );

	private static final GenericRequest ZONE_UNLOCK = new GenericRequest( "" );

	private final String adventureName;
	private final String formSource;
	private final String adventureId;

	private static final AdventureResult SKELETON_KEY = new AdventureResult( 642, 1 );

	public static final AdventureResult ABRIDGED = new AdventureResult( 534, -1 );
	public static final AdventureResult BRIDGE = new AdventureResult( 535, -1 );
	public static final AdventureResult DODECAGRAM = new AdventureResult( 479, -1 );
	public static final AdventureResult CANDLES = new AdventureResult( 480, -1 );
	public static final AdventureResult BUTTERKNIFE = new AdventureResult( 481, -1 );

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
		else if ( formSource.equals( "knob.php" ) )
		{
			this.addFormField( "king", "1" );
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
		else if ( !formSource.equals( "dungeon.php" ) && !formSource.equals( "basement.php" ) && !formSource.equals( "rats.php" ) && !formSource.equals( "barrel.php" ) )
		{
			this.addFormField( "action", adventureId );
		}
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

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
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "The Orc Chasm has already been bridged." );
				return;
			}
		}

		if ( this.formSource.equals( "shore.php" ) )
		{
			if ( KoLCharacter.getAdventuresLeft() < 3 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Ran out of adventures." );
				return;
			}
		}

		if ( this.formSource.equals( "mountains.php" ) )
		{
			if ( KoLConstants.inventory.contains( AdventureRequest.ABRIDGED ) )
			{
				( new UntinkerRequest( AdventureRequest.ABRIDGED.getItemId() ) ).run();
			}

			if ( !KoLConstants.inventory.contains( AdventureRequest.BRIDGE ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't cross the Orc Chasm." );
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
			this.addFormField( "snarfblat", String.valueOf(
				CellarDecorator.recommendCorner() ) );
		}
		else if ( this.formSource.equals( "barrel.php" ) )
		{
			int square = BarrelDecorator.recommendSquare();
			if ( square == 0 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE,
					"All booze in the specified rows has been collected." );
				return;
			}
			this.addFormField( "smash", String.valueOf( square ) );
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

	public void processResults()
	{
		// Sometimes, there's no response from the server.
		// In this case, skip and continue onto the next one.

		if ( this.responseText == null || this.responseText.trim().length() == 0 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't get to that area yet." );
			return;
		}

		// The hedge maze should always result in you getting
		// a fight redirect.  If this is not the case, then
		// if the hedge maze is not complete, use up all their
		// pieces first, then go adventuring.

		if ( this.formSource.equals( "lair3.php" ) )
		{
			if ( InventoryManager.hasItem( SorceressLairManager.HEDGE_KEY ) && InventoryManager.hasItem( SorceressLairManager.PUZZLE_PIECE ) )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Unexpected hedge maze puzzle state." );
			}

			return;
		}

		if ( this.formSource.equals( "dungeon.php" ) && this.responseText.indexOf( "You have reached the bottom of today's Dungeon" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Daily dungeon completed." );
			return;
		}

		// The sorceress fight should always result in you getting
		// a fight redirect.

		if ( this.formSource.equals( "lair6.php" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "The sorceress has already been defeated." );
			return;
		}

		if ( this.formSource.equals( "cyrpt.php" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "You shouldn't be here." );
			return;
		}

		// If you haven't unlocked the orc chasm yet,
		// try doing so now.

		if ( this.adventureId.equals( "80" ) && this.responseText.indexOf( "You shouldn't be here." ) != -1 )
		{
			AdventureRequest bridge = new AdventureRequest( "Bridge the Orc Chasm", "mountains.php", "" );
			bridge.run();

			if ( KoLmafia.permitsContinue() )
			{
				this.run();
			}

			return;
		}

		// We're missing an item, haven't been given a quest yet, or otherwise
		// trying to go somewhere not allowed.

		if ( this.responseText.indexOf( "You shouldn't be here" ) != -1 || this.responseText.indexOf( "not yet be accessible" ) != -1 || this.responseText.indexOf( "You can't get there" ) != -1 || this.responseText.indexOf( "Seriously.  It's locked." ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You can't get to that area yet." );
			return;
		}

		if ( this.responseText.indexOf( "in the regular dimension now" ) != -1 )
		{
			// "You're in the regular dimension now, and don't
			// remember how to get back there."
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "You are no longer Half-Astral." );
			return;
		}

		if ( this.responseText.indexOf( "into the spectral mists" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No one may know of its coming or going." );
			return;
		}

		if ( this.responseText.indexOf( "temporal rift in the plains has closed" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "The temporal rift has closed." );
			return;
		}

		// Cold protection is required for the area.  This only happens at
		// the peak.  Abort and notify.

		if ( this.responseText.indexOf( "need some sort of protection" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need cold protection." );
			return;
		}

		// Stench protection is required for the area.	This only
		// happens at the Guano Junction.  Abort and notify.

		if ( this.responseText.indexOf( "need stench protection to adventure here" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You need stench protection." );
			return;
		}

		// This is a server error. Hope for the
		// best and repeat the request.

		if ( this.responseText.indexOf( "No adventure data exists for this location" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Server error.  Please wait and try again." );
			return;
		}

		if ( this.responseText.indexOf( "You must have at least" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Your stats are too low for this location." );
			return;
		}

		// Cobb's Knob King's Chamber: if you've already
		// defeated the goblin king, go into pending state.

		if ( this.formSource.equals( "knob.php" ) && this.responseText.indexOf( "You've already slain the Goblin King" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "You already defeated the Goblin King." );
			return;
		}

		// The Haert of the Cyrpt: if you've already defeated
		// the bonerdagon, go into pending state.

		if ( this.formSource.equals( "cyrpt.php" ) && this.responseText.indexOf( "Bonerdagon has been defeated" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "You already defeated the Bonerdagon." );
			return;
		}

		if ( this.responseText.indexOf( "already undefiled" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Cyrpt area cleared." );
			return;
		}

		// Nothing more to do in this area

		if ( this.formSource.equals( "adventure.php" ) && this.responseText.indexOf( "adventure.php" ) == -1 && this.responseText.indexOf( "You acquire" ) == -1 )
		{
			if ( !KoLmafia.isAutoStop( this.encounter ) )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Nothing more to do here." );
			}

			return;
		}

		// The Orc Chasm (pre-bridge)

		if ( this.formSource.equals( "mountains.php" ) )
		{
			KoLmafia.updateDisplay( "You have bridged the Orc Chasm." );
			ResultProcessor.processResult( AdventureRequest.BRIDGE );
			return;
		}

		// If you're at the casino, each of the different slot
		// machines deducts meat from your tally

		if ( this.formSource.equals( "casino.php" ) )
		{
			if ( this.adventureId.equals( "1" ) )
			{
				ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, -5 ) );
			}
			else if ( this.adventureId.equals( "2" ) )
			{
				ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			}
			else if ( this.adventureId.equals( "11" ) )
			{
				ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			}
		}

		if ( this.adventureId.equals( "70" ) )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
		}
		else if ( this.adventureId.equals( "71" ) )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, -30 ) );
		}

		// Shore Trips cost 500 meat each; handle
		// the processing here.

		if ( this.formSource.equals( "shore.php" ) )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, -500 ) );
		}

		// Trick-or-treating requires a costume;
		// notify the user of this error.

		if ( this.formSource.equals( "trickortreat.php" ) && this.responseText.indexOf( "without a costume" ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You must wear a costume." );
			return;
		}
	}

	public static final String registerEncounter( final GenericRequest request )
	{
		String urlString = request.getURLString();
		String responseText = request.responseText;

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
			encounter = parseChoiceEncounter( choice, responseText );
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

		RequestLogger.printLine( "Encounter: " + encounter );
		RequestLogger.updateSessionLog( "Encounter: " + encounter );
		AdventureRequest.registerDemonName( encounter, responseText );

		if ( type != null )
		{
			if ( type.equals( "Combat" ) )
			{
				encounter = translateHoboType( encounter );
			}
			StaticEntity.getClient().registerEncounter( encounter, type );
		}

		return encounter;
	}

	private static final String parseCombatEncounter( final String responseText )
	{
		Matcher matcher = MONSTER_NAME.matcher( responseText );
		if ( !matcher.find() )
		{
			return "";
		}
		return CustomCombatManager.encounterKey( matcher.group(2), false );
	}

	private static final String translateHoboType( final String encounter )
	{
		switch ( KoLAdventure.lastAdventureId() )
		{
		case 167:
			// Hobopolis Town Square
			return encounter.startsWith( "Hodgman" ) ? encounter : "Normal Hobo";
		case 168:
			// Burnbarrel Blvd.
			return encounter.equals( "Ol' Scratch" ) ? encounter : "Hot Hobo";
		case 169:
			// Exposure Esplanade
			return encounter.equals( "Frosty" ) ? encounter : "Cold Hobo";
		case 170:
			// The Heap
			return encounter.equals( "Oscus" ) ? encounter : "Stench Hobo";
		case 171:
			// The Ancient Hobo Burial Ground
			return encounter.equals( "Zombo" ) ? encounter : "Spooky Hobo";
		case 172:
			// The Purple Light District
			return encounter.equals( "Chester" ) ? encounter : "Sleaze Hobo";
		}

		return encounter;
	}

	private static final String parseChoiceEncounter( final int choice, final String responseText )
	{
		if ( LouvreManager.louvreChoice( choice ) )
		{
			return LouvreManager.encounterName( choice );
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
		{ "The One-Legged Trouser", "one-pantlegged schnauzer" },
		{ "The Orc With a Spork", "waving a spork" },
		{ "The Slime Puddle", "slime puddle" },
		{ "The Sozzled Old Dragon", "sozzled old dragon" },
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
		// Skip past the Adventure Results
		int boldIndex = responseText.indexOf( "Results:</b>" );
		// ... whether or not they are there.
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
		{ "Summoning Chamber",
		  Pattern.compile( "Did you say your name was (.*?)\\?" ),
		  "delicious-looking pies"
		},
		{ "Hoom Hah",
		  Pattern.compile( "(.*?)! \\1, cooooome to meeeee!" ),
		  "fifty meat"
		},
		{ "Every Seashell Has a Story to Tell If You're Listening",
		  Pattern.compile( "Hello\\? Is (.*?) there\\?" ),
		  "fish-guy"
		},
		{ "Leavesdropping",
		  Pattern.compile( "(.*?), we call you! \\1, come to us!" ),
		  "bullwhip"
		},
		{ "These Pipes... Aren't Clean!",
		  Pattern.compile( "Blurgle. (.*?). Gurgle. By the way," ),
		  "coprodaemon" },
	};

	private static final Pattern NAME_PATTERN = Pattern.compile( "<b>&quot;(.*?)&quot;</b>" );

	public static final void registerDemonName( final String encounter, final String responseText )
	{
		for ( int i = 0; i < AdventureRequest.demons.length; ++i )
		{
			String name = (String) AdventureRequest.demons[ i ][ 0 ];
			if ( name == null || !name.equals( encounter ) )
			{
				continue;
			}

			Pattern pattern = (Pattern) AdventureRequest.demons[ i ][ 1 ];
			Matcher matcher = pattern.matcher( responseText );

			String demon = null;
			int index = -1;

			if ( matcher.find() )
			{
				// We found the name
				demon = matcher.group( 1 );
				index = i + 1;
			}
			else if ( i != 0 )
			{
				// It's not the Summoning Chamber.
				return;
			}
			else
			{
				// It is the Summoning Chamber. If he used a
				// valid demon name, we can deduce which one it
				// is from the result text

				matcher = AdventureRequest.NAME_PATTERN.matcher( responseText );
				if ( !matcher.find() )
				{
					return;
				}

				// Save the name he used.
				demon = matcher.group( 1 );

				// Look for tell-tale string
				for ( int j = 0; j < AdventureRequest.demons.length; ++j )
				{
					String text = (String) AdventureRequest.demons[ j ][ 2 ];
					if ( responseText.indexOf( text ) != -1 )
					{
						index = j + 1;
						break;
					}
				}

				// Couldn't figure out which demon he called.
				if ( index == -1 )
				{
					return;
				}
			}

			String settingName = "demonName" + index;
			String previousName = Preferences.getString( settingName );

			if ( previousName.equals( demon ) )
				return;

			RequestLogger.printLine( "Demon name: " + demon );
			RequestLogger.updateSessionLog( "Demon name: " + demon );
			Preferences.setString( settingName, demon );

			if ( KoLConstants.conditions.isEmpty() )
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, (String) demons[ index ][0] );
		}
	}

	private static final boolean containsEncounter( final String formSource, final String responseText )
	{
		// The first round is unique in that there is no
		// data fields.  Therefore, it will equal fight.php
		// exactly every single time.

		if ( formSource.startsWith( "fight.php" ) )
		{
			return FightRequest.getCurrentRound() == 0;
		}
		else if ( formSource.startsWith( "adventure.php" ) )
		{
			return true;
		}
		else if ( formSource.startsWith( "cave.php" ) )
		{
			return formSource.indexOf( "end" ) != -1;
		}
		else if ( formSource.startsWith( "shore.php" ) )
		{
			return formSource.indexOf( "whichtrip" ) != -1;
		}
		else if ( formSource.startsWith( "knob.php" ) )
		{
			return formSource.indexOf( "king" ) != -1;
		}
		else if ( formSource.startsWith( "cyrpt.php" ) )
		{
			return formSource.indexOf( "action" ) != -1;
		}
		else if ( formSource.startsWith( "rats.php" ) )
		{
			return true;
		}
		else if ( formSource.startsWith( "choice.php" ) )
		{
			return responseText.indexOf( "choice.php" ) != -1;
		}
		else if ( formSource.startsWith( "palinshelves.php" ) )
		{
			return responseText.indexOf( "palinshelves.php" ) != -1;
		}
		else if ( formSource.startsWith( "tiles.php" ) )
		{
			// Only register initial encounter of Dvorak's Revenge
			return responseText.indexOf( "I before E, except after C" ) != -1;
		}
		else if ( formSource.startsWith( "barrel.php?smash" ) )
		{
			return true;
		}

		// It is not a known adventure.  Therefore,
		// do not log the encounter yet.

		return false;
	}

	public int getAdventuresUsed()
	{
		return this.formSource.startsWith( "shore" ) ? 3 : 1;
	}

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
		KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Unknown adventure type encountered." );
	}

	public static final void handleDvoraksRevenge( final GenericRequest request )
	{
		StaticEntity.getClient().registerEncounter( "Dvorak's Revenge", "Noncombat" );
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
