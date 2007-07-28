/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AdventureRequest extends KoLRequest
{
	private String adventureName;
	private String formSource;
	private String adventureId;

	private static final AdventureResult HOT_PHIAL = new AdventureResult( 1637, 1 );
	private static final AdventureResult COLD_PHIAL = new AdventureResult( 1638, 1 );
	private static final AdventureResult SPOOKY_PHIAL = new AdventureResult( 1639, 1 );
	private static final AdventureResult STENCH_PHIAL = new AdventureResult( 1640, 1 );
	private static final AdventureResult SLEAZE_PHIAL = new AdventureResult( 1641, 1 );

	private static final AdventureResult HOT_FORM = new AdventureResult( "Hotform", 1, true );
	private static final AdventureResult COLD_FORM = new AdventureResult( "Coldform", 1, true );
	private static final AdventureResult SPOOKY_FORM = new AdventureResult( "Spookyform", 1, true );
	private static final AdventureResult STENCH_FORM = new AdventureResult( "Stenchform", 1, true );
	private static final AdventureResult SLEAZE_FORM = new AdventureResult( "Sleazeform", 1, true );

	private static final AdventureResult [] ELEMENT_FORMS = new AdventureResult [] { HOT_FORM, COLD_FORM, SPOOKY_FORM, STENCH_FORM, SLEAZE_FORM };

	private static final AdventureResult SKELETON_KEY = new AdventureResult( 642, 1 );

	public static final AdventureResult ABRIDGED = new AdventureResult( 534, -1 );
	public static final AdventureResult BRIDGE = new AdventureResult( 535, -1 );
	public static final AdventureResult DODECAGRAM = new AdventureResult( 479, -1 );
	public static final AdventureResult CANDLES = new AdventureResult( 480, -1 );
	public static final AdventureResult BUTTERKNIFE = new AdventureResult( 481, -1 );

	/**
	 * Constructs a new <code>AdventureRequest</code> which executes the
	 * adventure designated by the given Id by posting to the provided form,
	 * notifying the givenof results (or errors).
	 *
	 * @param	adventureName	The name of the adventure location
	 * @param	formSource	The form to which the data will be posted
	 * @param	adventureId	The identifer for the adventure to be executed
	 */

	public AdventureRequest( String adventureName, String formSource, String adventureId )
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
			this.addFormField( "pwd" );
		}
		else if ( formSource.equals( "casino.php" ) )
		{
			this.addFormField( "action", "slot" );
			this.addFormField( "whichslot", adventureId );
		}
		else if ( formSource.equals( "knob.php" ) )
		{
			this.addFormField( "pwd" );
			this.addFormField( "king", "Yep." );
		}
		else if ( formSource.equals( "mountains.php" ) )
		{
			this.addFormField( "pwd" );
			this.addFormField( "orcs", "1" );
		}
		else if ( formSource.equals( "friars.php" ) )
		{
			this.addFormField( "pwd" );
			this.addFormField( "action", "ritual" );
		}
		else if ( formSource.equals( "lair6.php" ) )
		{
			this.addFormField( "place", adventureId );
		}
		else if ( !formSource.equals( "dungeon.php" ) && !formSource.equals( "basement.php" ) && !formSource.equals( "rats.php" ) )
		{
			this.addFormField( "action", adventureId );
		}
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	/**
	 * Executes the <code>AdventureRequest</code>.  All items and stats gained
	 * or lost will be reported to the as well as any errors encountered
	 * through adventuring.  Meat lost due to an adventure (such as those to
	 * the casino, the shore, or the tavern) will also be reported.  Note that
	 * adventure costs are not yet being reported.
	 */

	public void run()
	{
		// Prevent the request from happening if they attempted
		// to cancel in the delay period.

		if ( !KoLmafia.permitsContinue() )
			return;

		if ( this.formSource.equals( "mountains.php" ) )
		{
			KoLAdventure.ZONE_VALIDATOR.constructURLString( "mountains.php" ).run();
			if ( KoLAdventure.ZONE_VALIDATOR.responseText.indexOf( "value=80" ) != -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "The Orc Chasm has already been bridged." );
				return;
			}
		}

		if ( StaticEntity.getBooleanProperty( "cloverProtectActive" ) )
			DEFAULT_SHELL.executeLine( "use * ten-leaf clover" );

		if ( this.formSource.equals( "shore.php" ) )
		{
			if ( KoLCharacter.getAdventuresLeft() < 2 )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "Ran out of adventures." );
				return;
			}
		}

		delay();

		if ( formSource.equals( "dungeon.php" ) || formSource.equals( "basement.php" ) )
			this.data.clear();

		super.run();

		if ( this.formSource.equals( "dungeon.php" ) )
		{
			this.addFormField( "pwd" );
			this.addFormField( "action", "Yep." );

			if ( this.responseText.indexOf( "Locked Door" ) != -1 && SKELETON_KEY.getCount( inventory ) + SKELETON_KEY.getCount( closet ) > 1 )
				this.addFormField( "option", "2" );
			else
				this.addFormField( "option", "1" );

			super.run();
		}

		if ( this.formSource.equals( "basement.php" ) )
			handleBasement();

		if ( StaticEntity.getBooleanProperty( "cloverProtectActive" ) )
			DEFAULT_SHELL.executeLine( "use * ten-leaf clover" );
	}

	private static final Pattern BASEMENT_PATTERN = Pattern.compile( "Level ([\\d,]+)" );

	private void prepareBasementTest( String name )
	{
		boolean foundMatch = false;

		Object currentTest;
		String currentTestString;

		List available = MoodSettings.getAvailableMoods();
		for ( int i = 0; i < available.size() && !foundMatch; ++i )
		{
			currentTest = available.get(i);
			currentTestString = currentTest.toString().toLowerCase();

			if ( currentTestString.indexOf( name ) != -1 )
			{
				foundMatch = true;
				MoodSettings.setMood( currentTestString );
			}
		}

		if ( foundMatch )
			MoodSettings.execute();

		available = KoLCharacter.getCustomOutfits();
		for ( int i = 0; i < available.size() && !foundMatch; ++i )
		{
			currentTest = available.get(i);
			currentTestString = currentTest.toString().toLowerCase();

			if ( currentTestString.indexOf( name ) != -1 )
			{
				RequestThread.postRequest( new EquipmentRequest( (SpecialOutfit) currentTest ) );
				return;
			}
		}
	}

	private boolean canHandleElementTest( int currentLevel, boolean switchedOutfits, int element1, int element2,
		AdventureResult effect1, AdventureResult effect2, AdventureResult desiredPhial )
	{

		float damage1 = (float) Math.pow( currentLevel, 1.52 ) / 2.0f;
		float damage2 = damage1;

		float resistance1 = KoLCharacter.getElementalResistance( element1 );
		float resistance2 = KoLCharacter.getElementalResistance( element2 );

		float expected1 = damage1 / 100.0f * (100.0f - resistance1);
		float expected2 = damage2 / 100.0f * (100.0f - resistance2);

		if ( expected1 + expected2 < KoLCharacter.getCurrentHP() )
			return true;

		if ( expected1 + expected2 < KoLCharacter.getMaximumHP() )
		{
			StaticEntity.getClient().recoverHP( (int) (expected1 + expected2) );
			return KoLmafia.permitsContinue();
		}

		if ( !switchedOutfits )
			return false;

		expected1 = 1.0f;

		if ( expected1 + expected2 >= KoLCharacter.getMaximumHP() )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Insufficient elemental resistance." );
			return false;
		}
		else if ( !activeEffects.contains( effect1 ) && !activeEffects.contains( effect2 ) )
		{
			for ( int i = 0; i < ELEMENT_FORMS.length; ++i )
				if ( activeEffects.contains( ELEMENT_FORMS[i] ) )
					(new UneffectRequest( ELEMENT_FORMS[i] )).run();

			(new ConsumeItemRequest( desiredPhial )).run();
		}

		StaticEntity.getClient().recoverHP( (int) (expected1 + expected2) );
		return KoLmafia.permitsContinue();
	}

	private void handleBasement()
	{
		Matcher levelMatcher = BASEMENT_PATTERN.matcher( responseText );
		if ( !levelMatcher.find() )
			return;

		int currentLevel = StaticEntity.parseInt( levelMatcher.group(1) );
		int element1 = -1, element2 = -1;
		AdventureResult effect1 = null, effect2 = null;

		AdventureResult desiredPhial = null;

		if ( responseText.indexOf( "Hold your nose" ) != -1 )
		{
			element1 = MonsterDatabase.SLEAZE;
			element2 = MonsterDatabase.STENCH;

			desiredPhial = SLEAZE_PHIAL;

			effect1 = SLEAZE_FORM;
			effect2 = STENCH_FORM;
		}
		else if ( responseText.indexOf( "Drink the Drunk's Drink" ) != -1 )
		{
			element1 = MonsterDatabase.COLD;
			element2 = MonsterDatabase.SLEAZE;

			desiredPhial = COLD_PHIAL;

			effect1 = COLD_FORM;
			effect2 = SLEAZE_FORM;
		}
		else if (responseText.indexOf( "Pwn the Cone" ) != -1 )
		{
			element1 = MonsterDatabase.STENCH;
			element2 = MonsterDatabase.HEAT;

			desiredPhial = STENCH_PHIAL;

			effect1 = STENCH_FORM;
			effect2 = HOT_FORM;
		}
		else if (responseText.indexOf( "What's a Typewriter" ) != -1 )
		{
			element1 = MonsterDatabase.HEAT;
			element2 = MonsterDatabase.SPOOKY;

			desiredPhial = HOT_PHIAL;

			effect1 = HOT_FORM;
			effect2 = SPOOKY_FORM;
		}
		else if (responseText.indexOf( "Evade the Vampsicle" ) != -1 )
		{
			element1 = MonsterDatabase.SPOOKY;
			element2 = MonsterDatabase.COLD;

			desiredPhial = SPOOKY_PHIAL;

			effect1 = SPOOKY_FORM;
			effect2 = COLD_FORM;
		}

		// If this is an elemental element check, check to see if
		// additional elements are needed.

		if ( desiredPhial != null )
		{
			if ( !canHandleElementTest( currentLevel, false, element1, element2, effect1, effect2, desiredPhial ) )
			{
				prepareBasementTest( "element" );
				if ( !canHandleElementTest( currentLevel, true, element1, element2, effect1, effect2, desiredPhial ) )
					return;
			}

			this.addFormField( "action", "1" );
		}
		else
		{
			// Stat tests check.  If you fail, do not bother
			// submitting the request.

			float drainRequirement = (float) Math.pow( currentLevel, 1.52 );
			float statRequirement = (float) Math.pow( currentLevel, 1.52 );

			if ( this.responseText.indexOf( "Lift 'em" ) != -1 || this.responseText.indexOf( "Push It Real Good" ) != -1 || this.responseText.indexOf( "Ring That Bell" ) != -1 )
			{
				if ( KoLCharacter.getAdjustedMuscle() < statRequirement )
				{
					prepareBasementTest( "muscle" );
					if ( KoLCharacter.getAdjustedMuscle() < statRequirement )
						KoLmafia.updateDisplay( ABORT_STATE, "Insufficient muscle to continue." );
				}
			}
			else if ( this.responseText.indexOf( "Gathering: The Magic" ) != -1 || this.responseText.indexOf( "Mop the Floor with the Mops" ) != -1 || this.responseText.indexOf( "Do away with the 'doo" ) != -1 )
			{
				if ( KoLCharacter.getAdjustedMysticality() < statRequirement )
				{
					prepareBasementTest( "mysticality" );
					if ( KoLCharacter.getAdjustedMysticality() < statRequirement )
						KoLmafia.updateDisplay( ABORT_STATE, "Insufficient mysticality to continue." );
				}
			}
			else if ( this.responseText.indexOf( "Don't Wake the Baby" ) != -1 || this.responseText.indexOf( "Grab a cue" ) != -1 || this.responseText.indexOf( "Put on the Smooth Moves" ) != -1 )
			{
				if ( KoLCharacter.getAdjustedMoxie() < statRequirement )
				{
					prepareBasementTest( "moxie" );
					if ( KoLCharacter.getAdjustedMoxie() < statRequirement )
						KoLmafia.updateDisplay( ABORT_STATE, "Insufficient moxie to continue." );
				}
			}
			else if ( this.responseText.indexOf( "Grab the Handles" ) != -1 )
			{
				if ( KoLCharacter.getMaximumMP() < drainRequirement )
				{
					prepareBasementTest( "mpdrain" );
					if ( KoLCharacter.getMaximumMP() < drainRequirement )
						KoLmafia.updateDisplay( ABORT_STATE, "Insufficient mana to continue." );
				}

				StaticEntity.getClient().recoverMP( (int) drainRequirement );
			}
			else if ( this.responseText.indexOf( "Run the Gauntlet Gauntlet" ) != -1 )
			{
				float damageAbsorb = 1.0f - (( ((float) Math.sqrt( KoLCharacter.getDamageAbsorption() / 10.0f )) - 1.0f ) / 10.0f);
				float healthRequirement = drainRequirement * damageAbsorb;

				if ( KoLCharacter.getMaximumHP() < healthRequirement )
				{
					prepareBasementTest( "gauntlet" );
					if ( KoLCharacter.getMaximumHP() < healthRequirement )
						KoLmafia.updateDisplay( ABORT_STATE, "Insufficient health to continue." );

					StaticEntity.getClient().recoverHP( (int) healthRequirement );
				}
			}

			if ( KoLmafia.refusesContinue() )
				return;

			// Since everything's safe, choose your option.

			if ( this.responseText.indexOf( "Got Silk?" ) != -1 )
				this.addFormField( "action", KoLCharacter.isMoxieClass() ? "1" : "2" );
			else if ( this.responseText.indexOf( "Save the Dolls" ) != -1 )
				this.addFormField( "action", KoLCharacter.isMysticalityClass() ? "1" : "2" );
			else if ( this.responseText.indexOf( "Take the Red Pill" ) != -1 )
				this.addFormField( "action", KoLCharacter.isMuscleClass() ? "1" : "2" );
			else
				this.addFormField( "action", "1" );
		}

		super.run();

		if ( this.responseCode != 200 )
		{
			if ( FightRequest.INSTANCE.responseCode == 200 && FightRequest.INSTANCE.responseText.indexOf( "<!--WINWINWIN-->" ) != -1 )
				return;

			this.data.clear();
			super.run();
		}

		levelMatcher = BASEMENT_PATTERN.matcher( responseText );
		if ( !levelMatcher.find() || currentLevel != StaticEntity.parseInt( levelMatcher.group(1) ) )
			KoLmafia.updateDisplay( ERROR_STATE, "Failed to pass basement test." );
	}

	public void processResults()
	{
		// Sometimes, there's no response from the server.
		// In this case, skip and continue onto the next one.

		if ( this.responseText == null || this.responseText.trim().length() == 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't get to that area yet." );
			return;
		}

		// The hedge maze should always result in you getting
		// a fight redirect.  If this is not the case, then
		// if the hedge maze is not complete, use up all their
		// pieces first, then go adventuring.

		if ( this.formSource.equals( "lair3.php" ) )
		{
			if ( KoLCharacter.hasItem( SorceressLair.HEDGE_KEY ) && KoLCharacter.hasItem( SorceressLair.PUZZLE_PIECE ) )
				KoLmafia.updateDisplay( PENDING_STATE, "Unexpected hedge maze puzzle state." );

			return;
		}

		if ( this.formSource.equals( "dungeon.php" ) && this.responseText.indexOf( "You have reached the bottom of today's Dungeon" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "Daily dungeon completed." );
			return;
		}

		// The sorceress fight should always result in you getting
		// a fight redirect.

		if ( this.formSource.equals( "lair6.php" ) )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "The sorceress has already been defeated." );
			return;
		}

		if ( this.formSource.equals( "cyrpt.php" ) )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "You shouldn't be here." );
			return;
		}

		// If you haven't unlocked the orc chasm yet,
		// try doing so now.

		if ( this.adventureId.equals( "80" ) && this.responseText.indexOf( "You shouldn't be here." ) != -1 )
		{
			AdventureRequest bridge = new AdventureRequest( "Bridge the Orc Chasm", "mountains.php", "" );
			bridge.run();

			if ( KoLmafia.permitsContinue() )
				this.run();

			return;
		}

		// We're missing an item, haven't been given a quest yet, or otherwise
		// trying to go somewhere not allowed.

		if ( this.responseText.indexOf( "You shouldn't be here" ) != -1 || this.responseText.indexOf( "not yet be accessible" ) != -1 || this.responseText.indexOf( "You can't get there" ) != -1 || this.responseText.indexOf( "Seriously.  It's locked." ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't get to that area yet." );
			return;
		}

		if ( this.responseText.indexOf( "in the regular dimension now" ) != -1 )
		{
			// "You're in the regular dimension now, and don't
			// remember how to get back there."
			KoLmafia.updateDisplay( PENDING_STATE, "You are no longer Half-Astral." );
			return;
		}

		if ( this.responseText.indexOf( "into the spectral mists" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "No one may know of its coming or going." );
			return;
		}

		if ( this.responseText.indexOf( "temporal rift in the plains has closed" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "The temporal rift has closed." );
			return;
		}

		// Cold protection is required for the area.  This only happens at
		// the peak.  Abort and notify.

		if ( this.responseText.indexOf( "need some sort of protection" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You need cold protection." );
			return;
		}

		// Stench protection is required for the area.	This only
		// happens at the Guano Junction.  Abort and notify.

		if ( this.responseText.indexOf( "need stench protection to adventure here" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You need stench protection." );
			return;
		}

		// This is a server error. Hope for the
		// best and repeat the request.

		if ( this.responseText.indexOf( "No adventure data exists for this location" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Server error.  Please wait and try again." );
			return;
		}

		if ( this.responseText.indexOf( "You must have at least" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Your stats are too low for this location." );
			return;
		}

		// Cobb's Knob King's Chamber: if you've already
		// defeated the goblin king, go into pending state.

		if ( this.formSource.equals( "knob.php" ) && this.responseText.indexOf( "You've already slain the Goblin King" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "You already defeated the Goblin King." );
			return;
		}

		// The Haert of the Cyrpt: if you've already defeated
		// the bonerdagon, go into pending state.

		if ( this.formSource.equals( "cyrpt.php" ) && this.responseText.indexOf( "Bonerdagon has been defeated" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "You already defeated the Bonerdagon." );
			return;
		}

		if ( this.responseText.indexOf( "already undefiled" ) != -1 )
		{
			KoLmafia.updateDisplay( PENDING_STATE, "Cyrpt area cleared." );
			return;
		}

		// Nothing more to do in this area

		if ( this.formSource.equals( "adventure.php" ) && this.responseText.indexOf( "adventure.php" ) == -1 && this.responseText.indexOf( "You acquire" ) == -1 )
		{
			if ( !KoLmafia.isAutoStop( this.encounter ) )
				KoLmafia.updateDisplay( PENDING_STATE, "Nothing more to do here." );

			return;
		}

		// The Orc Chasm (pre-bridge)

		if ( this.formSource.equals( "mountains.php" ) )
		{
			// If there's no link to the valley beyond, put down a
			// brIDGE

			if ( this.responseText.indexOf( "value=80" ) == -1 )
			{
				// If you have an unabridged dictionary in your
				// inventory, visit the untinkerer
				// automatically and repeat the request.

				if ( KoLCharacter.hasItem( ABRIDGED ) )
				{
					(new UntinkerRequest( ABRIDGED.getItemId() )).run();
					this.run();
					return;
				}

				// Otherwise, the player is unable to cross the
				// orc chasm at this time.

				KoLmafia.updateDisplay( ERROR_STATE, "You can't cross the Orc Chasm." );
				return;
			}

			if ( this.responseText.indexOf( "the path to the Valley is clear" ) != -1 )
			{
				KoLmafia.updateDisplay( PENDING_STATE, "You have bridged the Orc Chasm." );
				StaticEntity.getClient().processResult( BRIDGE );
			}

			return;
		}

		// If you're at the casino, each of the different slot
		// machines deducts meat from your tally

		if ( this.formSource.equals( "casino.php" ) )
		{
			if ( this.adventureId.equals( "1" ) )
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -5 ) );
			else if ( this.adventureId.equals( "2" ) )
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			else if ( this.adventureId.equals( "11" ) )
				StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
		}

		if ( this.adventureId.equals( "70" ) )
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
		else if ( this.adventureId.equals( "71" ) )
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -30 ) );

		// Shore Trips cost 500 meat each; handle
		// the processing here.

		if ( this.formSource.equals( "shore.php" ) )
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, -500 ) );

		// Trick-or-treating requires a costume;
		// notify the user of this error.

		if ( this.formSource.equals( "trickortreat.php" ) && this.responseText.indexOf( "without a costume" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You must wear a costume." );
			return;
		}
	}

	public static String registerEncounter( KoLRequest request )
	{
		String urlString = request.getURLString();
		if ( !(request instanceof AdventureRequest) && !containsEncounter( urlString, request.responseText ) )
			return "";

		if ( urlString.indexOf( "fight.php" ) != -1 )
		{
			int spanIndex = request.responseText.indexOf( "<span id='monname" ) + 1;
			spanIndex = request.responseText.indexOf( ">", spanIndex ) + 1;

			if ( spanIndex == 0 )
				return "";

			int endSpanIndex = request.responseText.indexOf( "</span>", spanIndex );
			if ( endSpanIndex == -1 )
				return "";

			String encounter = request.responseText.substring( spanIndex, endSpanIndex );
			encounter = CombatSettings.encounterKey( encounter, false );

			RequestLogger.printLine( "Encounter: " + encounter );
			RequestLogger.updateSessionLog( "Encounter: " + encounter );
			StaticEntity.getClient().registerEncounter( encounter, "Combat" );

			return encounter;
		}
		else if ( urlString.startsWith( "dungeon.php" ) || urlString.startsWith( "basement.php" ) )
		{
			return "";
		}
		else
		{
			int boldIndex = request.responseText.indexOf( "Results:</b>" ) + 1;
			boldIndex = request.responseText.indexOf( "<b>", boldIndex ) + 3;

			if ( boldIndex == 2 )
				return "";

			int endBoldIndex = request.responseText.indexOf( "</b>", boldIndex );

			if ( endBoldIndex == -1 )
				return "";

			String encounter = request.responseText.substring( boldIndex, endBoldIndex );
			if ( encounter.equals( "" ) )
				return "";

			RequestLogger.printLine( "Encounter: " + encounter );
			RequestLogger.updateSessionLog( "Encounter: " + encounter );

			if ( !urlString.startsWith( "choice.php" ) || urlString.indexOf( "option" ) == -1 )
				StaticEntity.getClient().registerEncounter( encounter, "Noncombat" );
			else
				StaticEntity.getClient().recognizeEncounter( encounter );

			return encounter;
		}
	}

	private static boolean containsEncounter( String formSource, String responseText )
	{
		// The first round is unique in that there is no
		// data fields.  Therefore, it will equal fight.php
		// exactly every single time.

		if ( formSource.startsWith( "fight.php" ) )
			return FightRequest.getActualRound() == 0;

		// All other adventures can be identified via their
		// form data and the place they point to.

		else if ( formSource.startsWith( "adventure.php" ) )
			return true;
		else if ( formSource.startsWith( "cave.php" ) )
			return formSource.indexOf( "end" ) != -1;
		else if ( formSource.startsWith( "shore.php" ) )
			return formSource.indexOf( "whichtrip" ) != -1;
		else if ( formSource.startsWith( "knob.php" ) )
			return formSource.indexOf( "king" ) != -1;
		else if ( formSource.startsWith( "cyrpt.php" ) )
			return formSource.indexOf( "action" ) != -1;
		else if ( formSource.startsWith( "rats.php" ) )
			return true;
		else if ( formSource.startsWith( "choice.php" ) )
			return responseText.indexOf( "choice.php" ) != -1;
		else if ( formSource.startsWith( "palinshelves.php" ) )
			return responseText.indexOf( "palinshelves.php" ) != -1;

		// It is not a known adventure.  Therefore,
		// do not log the encounter yet.

		return false;
	}

	public int getAdventuresUsed()
	{	return 1;
	}

	public String toString()
	{	return this.adventureName;
	}

	public static void handleServerRedirect( String redirectLocation )
	{
		if ( redirectLocation.indexOf( "main.php" ) != -1 )
			return;

		KoLRequest request = new KoLRequest( redirectLocation );

		if ( redirectLocation.indexOf( "palinshelves.php" ) != -1 )
		{
			request.run();
			request.constructURLString( "palinshelves.php?action=placeitems&whichitem1=2259&whichitem2=2260&whichitem3=493&whichitem4=2261" ).run();
			return;
		}

		if ( redirectLocation.indexOf( "tiles.php" ) != -1 )
		{
			handleDvoraksRevenge( request );
			return;
		}

		FightFrame.showRequest( request );
		KoLmafia.updateDisplay( ABORT_STATE, "Unknown adventure type encountered." );
	}

	public static void handleDvoraksRevenge( KoLRequest request )
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

	public static boolean useMarmotClover( String location, String responseText )
	{
		return StaticEntity.getBooleanProperty( "cloverProtectActive" ) &&
			location.startsWith( "adventure.php" ) && responseText.indexOf( "notice a ten-leaf clover" ) != -1 &&
			responseText.indexOf( "our ten-leaf clover" ) == -1 && responseText.indexOf( "puff of smoke" ) == -1;
	}
}
