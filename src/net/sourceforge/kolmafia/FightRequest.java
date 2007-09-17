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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.MonsterDatabase.Monster;

public class FightRequest extends KoLRequest
{
	public static final FightRequest INSTANCE = new FightRequest();

	private static final AdventureResult LUCRE = new AdventureResult( 2098, 1 );
	private static final AdventureResult ANTIDOTE = new AdventureResult( 829, 1 );
	private static final AdventureResult SOLDIER = new AdventureResult( 1397, 1 );
	private static final AdventureResult MERCENARY = new AdventureResult( 2139, 1 );
	private static final AdventureResult TEQUILA = new AdventureResult( 1004, -1 );

	public static final int MOSSY_STONE_SPHERE = 2174;
	public static final int SMOOTH_STONE_SPHERE = 2175;
	public static final int CRACKED_STONE_SPHERE = 2176;
	public static final int ROUGH_STONE_SPHERE = 2177;

	private static int lastUserId = 0;
	private static String lostInitiative = "";
	private static String wonInitiative = "";
	private static int preparatoryRounds = 0;

	public static String lastResponseText = "";
	private static boolean isTrackingFights = false;
	private static boolean foundNextRound = false;

	private static boolean isAutomatingFight = false;
	private static boolean isUsingConsultScript = false;

	private static final Pattern SKILL_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern ITEM1_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern ITEM2_PATTERN = Pattern.compile( "whichitem2=(\\d+)" );

	private static final Pattern FAMILIAR_ACT_PATTERN = Pattern.compile( "<table><tr><td align=center.*?</table>", Pattern.DOTALL );
	private static final Pattern FUMBLE_PATTERN = Pattern.compile( "You drop your .*? on your .*?, doing [\\d,]+ damage" );
	private static final Pattern ELEMENTAL_PATTERN = Pattern.compile( "<font color=[\"]?\\w+[\"]?><b>\\+?([\\d,]+)</b></font> (?:damage|points|HP worth)" );

	// NOTE: All of the non-empty patterns that can match in the first group
	// imply that the entire expression should be ignored.  If you add one
	// and this is not the case, then correct the use of this Pattern below.

	private static final Pattern PHYSICAL_PATTERN = Pattern.compile( "(your blood, to the tune of|stabs you for|sown|You lose|You gain|) (\\d[\\d,]*) (\\([^.]*\\) |)(?:\\w+ ){0,2}(?:damage|points?|notch(?:es)?|to your opponent|force damage)");
	private static final Pattern SECONDARY_PATTERN = Pattern.compile( "<b>\\+([\\d,]+)</b>" );
	private static final Pattern MOSQUITO_PATTERN = Pattern.compile( "sucks some blood out of your opponent and injects it into you.*?You gain ([\\d,]+) hit point" );
	private static final Pattern BOSSBAT_PATTERN = Pattern.compile( "until he disengages, two goofy grins on his faces.*?You lose ([\\d,]+)" );
	private static final Pattern GHUOL_HEAL = Pattern.compile( "feasts on a nearby corpse, and looks refreshed\\." );
	private static final Pattern NS_HEAL = Pattern.compile( "The Sorceress pulls a tiny red vial out of the folds of her dress and quickly drinks it" );

	public static final AdventureResult DICTIONARY1 = new AdventureResult( 536, 1 );
	public static final AdventureResult DICTIONARY2 = new AdventureResult( 1316, 1 );

	private static final AdventureResult TOOTH = new AdventureResult( 2, 1 );
	private static final AdventureResult TURTLE = new AdventureResult( 4, 1 );
	private static final AdventureResult SPICES = new AdventureResult( 8, 1 );

	private static final AdventureResult BROKEN_GREAVES = new AdventureResult( 1929, -1 );
	private static final AdventureResult BROKEN_HELMET = new AdventureResult( 1930, -1 );
	private static final AdventureResult BROKEN_SPEAR = new AdventureResult( 1931, -1 );
	private static final AdventureResult BROKEN_SHIELD = new AdventureResult( 1932, -1 );

	private static boolean castCleesh = false;
	private static boolean jiggledChefstaff = false;
	private static int currentRound = 0;
	private static int offenseModifier = 0, defenseModifier = 0;
	private static int healthModifier = 0;

	private static String action1 = null;
	private static String action2 = null;
	private static Monster monsterData = null;
	private static String missingGremlinTool = null;
	private static String encounterLookup = "";

	private static AdventureResult desiredScroll = null;

	private static final AdventureResult SCROLL_334 = new AdventureResult( 547, 1 );
	public static final AdventureResult SCROLL_668 = new AdventureResult( 548, 1 );
	private static final AdventureResult SCROLL_30669 = new AdventureResult( 549, 1 );
	private static final AdventureResult SCROLL_33398 = new AdventureResult( 550, 1 );
	private static final AdventureResult SCROLL_64067 = new AdventureResult( 551, 1 );
	public static final AdventureResult SCROLL_64735 = new AdventureResult( 552, 1 );
	public static final AdventureResult SCROLL_31337 = new AdventureResult( 553, 1 );

	// Ultra-rare monsters
	private static final String [] RARE_MONSTERS =
	{
		"baiowulf", "crazy bastard", "hockey elemental", "hypnotist of hey deze",
		"infinite meat bug", "master of thieves", "temporal bandit"
	};

	/**
	 * Constructs a new <code>FightRequest</code>.  Theprovided will
	 * be used to determine whether or not the fight should be started and/or
	 * continued, and the user settings will be used to determine the kind
	 * of action1 to be taken during the battle.
	 */

	private FightRequest()
	{	super( "fight.php" );
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public static final boolean wonInitiative()
	{	return currentRound == 1 && lastResponseText.indexOf( "You get the jump" ) != -1;
	}

	public void nextRound()
	{
		// When logging in and encountering a fight, always use the
		// attack command to avoid abort problems.

		if ( LoginRequest.isInstanceRunning() )
		{
			action1 = "attack";
			this.addFormField( "action", "attack" );
			return;
		}

		if ( KoLmafia.refusesContinue() )
		{
			action1 = "abort";
			return;
		}

		// First round, KoLmafia does not decide the action.
		// Update accordingly.

		if ( currentRound == 0 )
		{
			action1 = null;
			return;
		}

		// Always let the user see rare monsters

		for ( int i = 0; i < RARE_MONSTERS.length; ++i )
		{
			if ( encounterLookup.indexOf( RARE_MONSTERS[i] ) != -1 )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "You have encountered the " + this.encounter );
				action1 = "abort";
				return;
			}
		}

		action1 = CombatSettings.getShortCombatOptionName( KoLSettings.getUserProperty( "battleAction" ) );
		action2 = null;

		// Adding machine should override custom combat scripts as well,
		// since it's conditions-driven.

		if ( encounterLookup.equals( "rampaging adding machine" ) )
			handleAddingMachine();

		// If the user wants a custom combat script, parse the desired
		// action here.

		if ( action1.equals( "custom" ) )
		{
			action1 = CombatSettings.getSetting( encounterLookup, currentRound - 1 - preparatoryRounds );
		}
		else if ( !KoLCharacter.canInteract() && wonInitiative() && monsterData != null && monsterData.shouldSteal() )
		{
			++preparatoryRounds;
			action1 = "steal";
			this.addFormField( "action", "steal" );
			return;
		}

		// If the person wants to use their own script,
		// then this is where it happens.

		if ( action1.startsWith( "consult" ) )
		{
			isUsingConsultScript = true;
			String scriptName = action1.substring( "consult".length() ).trim();

			KoLmafiaASH interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
			if ( interpreter != null )
			{
				interpreter.execute( "main", new String [] { String.valueOf( currentRound ), encounterLookup, this.responseText } );
				if ( KoLmafia.refusesContinue() )
					action1 = "abort";

				return;
			}

			KoLmafia.updateDisplay( ABORT_STATE, "Consult script '" + scriptName + "' not found." );
			action1 = "abort";
			return;
		}

		// Let the de-level action figure out what
		// should be done, and then re-process.

		if ( action1.startsWith( "delevel" ) )
			action1 = this.getMonsterWeakenAction();

		this.updateCurrentAction();
	}

	public static final String getCurrentKey()
	{	return CombatSettings.encounterKey( encounterLookup );
	}

	private void updateCurrentAction()
	{
		if ( action1.equals( "abort" ) )
		{
			// If the user has chosen to abort combat, flag it.
			action1 = "abort";
			return;
		}

		// User wants to run away
		if ( action1.indexOf( "run" ) != -1 && action1.indexOf( "away" ) != -1 )
		{
			action1 = "runaway";
			this.addFormField( "action", action1 );
			return;
		}

		// User wants a regular attack
		if ( action1.startsWith( "attack" ) )
		{
			action1 = "attack";
			this.addFormField( "action", action1 );
			return;
		}

		if ( action1.startsWith( "twiddle" ) )
		{
			action1 = null;
			return;
		}

		if ( activeEffects.contains( KoLAdventure.AMNESIA ) )
		{
			if ( monsterData == null || !monsterData.willUsuallyMiss( defenseModifier ) )
			{
				action1 = "attack";
				this.addFormField( "action", action1 );
				return;
			}
			else
			{
				action1 = "abort";
				return;
			}
		}

		// Actually steal if the action says to steal

		if ( action1.indexOf( "steal" ) != -1 )
		{
			boolean shouldSteal = wonInitiative();

			if ( CombatSettings.getSettingKey( encounterLookup ).equals( "default" ) )
				shouldSteal &= monsterData != null && monsterData.shouldSteal();

			if ( shouldSteal )
			{
				action1 = "steal";
				this.addFormField( "action", action1 );
				return;
			}

			--preparatoryRounds;
			this.nextRound();
			return;
		}

		// Jiggle chefstaff if the action says to jiggle and we're
		// wielding a chefstaff. Otherwise, skip this action.

		if ( action1.startsWith( "jiggle" ) )
		{
			if ( !jiggledChefstaff && KoLCharacter.wieldingChefstaff() )
			{
				this.addFormField( "action", "chefstaff" );
				return;
			}

			// You can only jiggle once per round.
			--preparatoryRounds;
			this.nextRound();
			return;
		}

		// If the player wants to use an item, make sure he has one
		if ( !action1.startsWith( "skill" ) )
		{
			int item1, item2;

			int commaIndex = action1.indexOf( "," );
			if ( commaIndex != -1 )
			{
				item1 = StaticEntity.parseInt( action1.substring( 0, commaIndex ) );
				item2 = StaticEntity.parseInt( action1.substring( commaIndex + 1 ) );
			}
			else
			{
				item1 = StaticEntity.parseInt( action1 );
				item2 = -1;
			}

			int itemCount = (new AdventureResult( item1, 1 )).getCount( inventory );

			if ( itemCount == 0 )
			{
				item1 = item2;
				item2 = -1;

				itemCount = (new AdventureResult( item1, 1 )).getCount( inventory );

				if ( itemCount == 0 )
				{
					KoLmafia.updateDisplay( ABORT_STATE, "You don't have enough " + TradeableItemDatabase.getItemName( item1 ) );
					action1 = "abort";
					return;
				}
			}

			if ( (item1 == DICTIONARY1.getItemId() || item1 == DICTIONARY2.getItemId()) )
			{
				if ( itemCount < 1 )
				{
					KoLmafia.updateDisplay( ABORT_STATE, "You don't have a dictionary." );
					action1 = "abort";
					return;
				}

				if ( encounterLookup.equals( "rampaging adding machine" ) )
				{
					KoLmafia.updateDisplay( ABORT_STATE, "Dictionaries do not work against adding machines." );
					action1 = "abort";
					return;
				}
			}

			this.addFormField( "action", "useitem" );
			this.addFormField( "whichitem", String.valueOf( item1 ) );

			if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
				return;

			if ( item2 != -1 )
			{
				itemCount = (new AdventureResult( item2, 1 )).getCount( inventory );

				if ( itemCount > 1 || (item1 != item2 && itemCount > 0) )
				{
					action2 = String.valueOf( item2 );
					this.addFormField( "whichitem2", String.valueOf( item2 ) );
					return;
				}

				KoLmafia.updateDisplay( ABORT_STATE, "You don't have enough " + TradeableItemDatabase.getItemName( item2 ) );
				action1 = "abort";
			}

			if ( itemCount >= 2 && item1 != ANTIDOTE.getItemId() && item1 != DICTIONARY1.getItemId() && item1 != DICTIONARY2.getItemId() )
			{
				action2 = action1;
				this.addFormField( "whichitem2", String.valueOf( item1 ) );
			}

			return;
		}

		// Skills use MP. Make sure the character has enough
		if ( KoLCharacter.getCurrentMP() < getActionCost() && passwordHash != null )
		{
			for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
			{
				if ( MPRestoreItemList.CONFIGURES[i].isCombatUsable() && inventory.contains( MPRestoreItemList.CONFIGURES[i].getItem() ) )
				{
					action1 = String.valueOf( MPRestoreItemList.CONFIGURES[i].getItem().getItemId() );

					++preparatoryRounds;
					this.updateCurrentAction();
					return;
				}
			}

			action1 = "abort";
			return;
		}

		// If the player wants to use a skill, make sure he knows it
		String skillName = ClassSkillsDatabase.getSkillName( StaticEntity.parseInt( action1.substring(5) ) );

		if ( KoLmafiaCLI.getCombatSkillName( skillName ) == null )
		{
			if ( this.isAcceptable( 0, 0 ) )
			{
				action1 = "attack";
				this.addFormField( "action", action1 );
				return;
			}

			action1 = "abort";
			return;
		}

		if ( skillName.equals( "CLEESH" ) )
		{
			if ( castCleesh )
			{
				action1 = "attack";
				this.addFormField( "action", action1 );
				return;
			}

			castCleesh = true;
		}

		if ( isInvalidThrustSmack( action1 ) )
		{
			action1 = "abort";
			return;
		}

		this.addFormField( "action", "skill" );
		this.addFormField( "whichskill", action1.substring(5) );
	}

	private static final boolean isInvalidThrustSmack( String action1 )
	{
		if ( !action1.equals( "1003" ) && !action1.equals( "1005" ) )
			return false;

		if ( EquipmentDatabase.isRanged( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getItemId() ) )
		{
			KoLmafia.updateDisplay( ABORT_STATE, "Thrust smacks are useless with ranged weapons." );
			return true;
		}

		return false;
	}

	/**
	 * Executes the single round of the fight.  If the user wins or loses,
	 * thewill be notified; otherwise, the next battle will be run
	 * automatically.  All fighting terminates if thecancels their
	 * request; note that battles are not automatically completed.  However,
	 * the battle's execution will be reported in the statistics for the
	 * requests, and will count against any outstanding requests.
	 */

	public void run()
	{
		RequestThread.openRequestSequence();
		isAutomatingFight = true;

		do
		{
			this.clearDataFields();

			action1 = null;
			action2 = null;
			isUsingConsultScript = false;

			if ( !KoLmafia.refusesContinue() )
				this.nextRound();

			if ( !isUsingConsultScript )
			{
				if ( currentRound == 0 )
				{
					super.run();
				}
				else if ( action1 != null && !action1.equals( "abort" ) )
				{
					delay();
					super.run();
				}
			}

			if ( action1 != null && action1.equals( "abort" ) )
				KoLmafia.updateDisplay( ABORT_STATE, "You're on your own, partner." );
		}
		while ( responseCode == 200 && currentRound != 0 && !KoLmafia.refusesContinue() );

		if ( responseCode == 302 )
			clearInstanceData();

		if ( KoLmafia.refusesContinue() && currentRound != 0 )
			this.showInBrowser( true );

		isAutomatingFight = false;
		RequestThread.closeRequestSequence();
	}

	public static final int getMonsterHealth()
	{
		if ( monsterData == null )
			return 0;

		return monsterData.getAdjustedHP( KoLCharacter.getMonsterLevelAdjustment() ) - healthModifier;
	}

	public static final int getMonsterAttack()
	{
		if ( monsterData == null )
			return 0;

		return monsterData.getAttack() + FightRequest.offenseModifier + KoLCharacter.getMonsterLevelAdjustment();
	}

	public static final int getMonsterDefense()
	{
		if ( monsterData == null )
			return 0;

		return monsterData.getDefense() + FightRequest.defenseModifier + KoLCharacter.getMonsterLevelAdjustment();
	}

	public static final int getMonsterAttackElement()
	{
		if ( monsterData == null )
			return MonsterDatabase.NONE;

		return monsterData.getAttackElement();
	}

	public static final int getMonsterDefenseElement()
	{
		if ( monsterData == null )
			return MonsterDatabase.NONE;

		return monsterData.getDefenseElement();
	}

	public static final boolean willUsuallyMiss()
	{	return willUsuallyMiss(0);
	}

	public static final boolean willUsuallyMiss( int defenseModifier )
	{
		if ( monsterData == null )
			return false;

		return monsterData.willUsuallyMiss( FightRequest.defenseModifier + defenseModifier );
	}

	public static final boolean willUsuallyDodge()
	{	return willUsuallyDodge(0);
	}

	public static final boolean willUsuallyDodge( int offenseModifier )
	{
		if ( monsterData == null )
			return false;

		return monsterData.willUsuallyDodge( FightRequest.offenseModifier + offenseModifier );
	}

	private boolean isAcceptable( int offenseModifier, int defenseModifier )
	{
		if ( monsterData == null )
			return true;

		if ( willUsuallyMiss() || willUsuallyDodge() )
			return false;

		return KoLmafia.getRestoreCount() == 0;
	}

	private void handleAddingMachine()
	{
		if ( desiredScroll != null )
			createAddingScroll( desiredScroll );
		else if ( conditions.contains( SCROLL_64735 ) )
			createAddingScroll( SCROLL_64735 );
		else if ( conditions.contains( SCROLL_64067 ) )
			createAddingScroll( SCROLL_64067 );
		else if ( conditions.contains( SCROLL_31337 ) )
			createAddingScroll( SCROLL_31337 );
		else if ( conditions.contains( SCROLL_668 ) )
			createAddingScroll( SCROLL_668 );
		else if ( KoLSettings.getBooleanProperty( "createHackerSummons" ) )
			createAddingScroll( SCROLL_31337 );
		else
			createAddingScroll( SCROLL_668 );
	}

	private boolean createAddingScroll( AdventureResult scroll )
	{
		AdventureResult part1 = null;
		AdventureResult part2 = null;

		if ( scroll == SCROLL_64735 )
		{
			part1 = SCROLL_668;
			part2 = SCROLL_64067;
		}
		else if ( scroll == SCROLL_64067 )
		{
			if ( !conditions.contains( SCROLL_64067 ) && inventory.contains( SCROLL_64067 ) )
				return false;

			part1 = SCROLL_30669;
			part2 = SCROLL_33398;
		}
		else if ( scroll == SCROLL_668 )
		{
			part1 = SCROLL_334;
			part2 = SCROLL_334;
		}
		else if ( scroll == SCROLL_31337 )
		{
			part1 = SCROLL_668;
			part2 = SCROLL_30669;
		}
		else
		{
			return false;
		}

		if ( desiredScroll != null )
		{
			++preparatoryRounds;
			action1 = String.valueOf( part2.getItemId() );

			desiredScroll = null;
			return true;
		}

		if ( (part1 == part2 && part1.getCount( inventory ) < 2) )
			return false;

		if ( !inventory.contains( part1 ) )
			return createAddingScroll( part1 ) || createAddingScroll( part2 );

		if ( !inventory.contains( part2 ) )
			return createAddingScroll( part2 );

		if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
		{
			++preparatoryRounds;
			action1 = String.valueOf( part1.getItemId() );

			desiredScroll = scroll;
			return true;
		}

		++preparatoryRounds;
		action1 = part1.getItemId() + "," + part2.getItemId();
		return true;
	}

	private static void handleGremlin( String responseText )
	{
		// Batwinged Gremlin has molybdenum hammer OR
		// "It does a bombing run over your head..."

		// Erudite Gremlin has molybdenum crescent wrench OR
		// "He uses the random junk around him to make an automatic
		// eyeball-peeler..."

		// Spider Gremlin has molybdenum pliers OR
		// "It bites you in the fibula with its mandibles..."

		// Vegetable Gremlin has molybdenum screwdriver OR
		// "It picks a <x> off of itself and beats you with it..."

		String text = responseText;
		if ( text.indexOf( "bombing run" ) != -1 )
			missingGremlinTool = "molybdenum hammer";
		else if ( text.indexOf( "eyeball-peeler" ) != -1 )
			missingGremlinTool = "molybdenum crescent wrench";
		else if ( text.indexOf( "fibula" ) != -1 )
			missingGremlinTool = "molybdenum pliers";
		else if ( text.indexOf( "off of itself" ) != -1 )
			missingGremlinTool = "molybdenum screwdriver";
	}

	private String getMonsterWeakenAction()
	{
		if ( this.isAcceptable( 0, 0 ) )
			return "attack";

		int desiredSkill = 0;
		boolean isAcceptable = false;

		// Disco Eye-Poke
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Eye-Poke" ) )
		{
			desiredSkill = 5003;
			isAcceptable = this.isAcceptable( -1, -1 );
		}

		// Disco Dance of Doom
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Dance of Doom" ) )
		{
			desiredSkill = 5005;
			isAcceptable = this.isAcceptable( -3, -3 );
		}

		// Disco Dance II: Electric Boogaloo
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Dance II: Electric Boogaloo" ) )
		{
			desiredSkill = 5008;
			isAcceptable = this.isAcceptable( -5, -5 );
		}

		// Tango of Terror
		if ( !isAcceptable && KoLCharacter.hasSkill( "Tango of Terror" ) )
		{
			desiredSkill = 5019;
			isAcceptable = this.isAcceptable( -6, -6 );
		}

		// Disco Face Stab
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Face Stab" ) )
		{
			desiredSkill = 5012;
			isAcceptable = this.isAcceptable( -7, -7 );
		}

		return desiredSkill == 0 ? "attack" : "skill" + desiredSkill;
	}

	private static final void checkForInitiative( String responseText )
	{
		if ( isAutomatingFight )
			RequestLogger.printLine( "Strategy: " + KoLSettings.getUserProperty( "battleAction" ) );

		if ( lastUserId != KoLCharacter.getUserId() )
		{
			lastUserId = KoLCharacter.getUserId();
			lostInitiative = "Round 0: " + KoLCharacter.getUserName() + " loses initiative!";
			wonInitiative = "Round 0: " + KoLCharacter.getUserName() + " wins initiative!";
		}

		boolean shouldLogAction = KoLSettings.getBooleanProperty( "logBattleAction" );

		// Whether or not you get initiative is easy -- look for the
		// text saying "You get the jump".

		if ( responseText.indexOf( "You get the jump" ) == -1 )
		{
			// If you lose initiative, there's nothing very interesting to
			// print to the session log.

			if ( shouldLogAction )
			{
				RequestLogger.printLine( lostInitiative );
				RequestLogger.updateSessionLog( lostInitiative );
			}

			return;
		}

		// Now that you've won initiative, figure out what actually
		// happened in that first round based on player settings.

		if ( shouldLogAction )
		{
			RequestLogger.printLine( wonInitiative );
			RequestLogger.updateSessionLog( wonInitiative );
		}

		action1 = KoLSettings.getUserProperty( "defaultAutoAttack" );

		// If no default action is made by the player, then the round remains
		// the same.  Simply report winning/losing initiative.

		if ( action1.equals( "" ) || action1.equals( "0" ) )
			return;

		StringBuffer action = new StringBuffer();
		++currentRound;

		if ( KoLSettings.getBooleanProperty( "ignoreAutoAttack" ) )
			++preparatoryRounds;

		if ( shouldLogAction )
			action.append( "Round 1: " + KoLCharacter.getUserName() + " " );

		if ( action1.equals( "1" ) )
		{
			if ( shouldLogAction )
				action.append( "attacks!" );

			action1 = "attack";
		}
		else if ( action1.equals( "3" ) )
		{
			if ( shouldLogAction )
				action.append( "tries to steal an item!" );

			action1 = "steal";
		}
		else if ( shouldLogAction )
		{
			action.append( "casts " +
				ClassSkillsDatabase.getSkillName( Integer.parseInt( action1 ) ).toUpperCase() + "!" );
		}

		if ( shouldLogAction )
		{
			action.append( " (auto-attack)" );
			RequestLogger.printLine( action.toString() );
			RequestLogger.updateSessionLog( action.toString() );
		}
	}

	public static final void updateCombatData( String encounter, String responseText )
	{
		foundNextRound = true;
		lastResponseText = responseText;

		// Round tracker should include this data.

		parseBangPotion( responseText );
		parseStoneSphere( responseText );

		// Spend MP and consume items

		++currentRound;

		if ( !activeEffects.contains( KoLAdventure.CUNCTATITIS ) || responseText.indexOf( "You decide" ) == -1 )
			payActionCost();

		if ( currentRound == 1 )
		{
			// If this is the first round, then register the opponent
			// you are fighting against.

			encounterLookup = CombatSettings.encounterKey( encounter );
			monsterData = MonsterDatabase.findMonster( encounterLookup );

			isTrackingFights = false;
			checkForInitiative( responseText );
		}

		// Quest gremlins - adventure=139 - might have a tool.
		// We should check for adventure #, not monster name

		if ( encounterLookup.indexOf( "gremlin" ) != -1 )
		{
			KoLAdventure location = KoLAdventure.lastVisitedLocation();
			if ( location != null && location.getAdventureId().equals( "139" ) )
				handleGremlin( responseText );
		}

		int blindIndex = responseText.indexOf( "... something.</div>" );

		// Log familiar actions, if the player wishes to include this
		// information in their session logs.

		if ( KoLSettings.getBooleanProperty( "logFamiliarActions" ) )
		{
			Matcher familiarActMatcher = FAMILIAR_ACT_PATTERN.matcher( responseText );
			while ( familiarActMatcher.find() )
			{
				String action = "Round " + currentRound + ": " + ANYTAG_PATTERN.matcher( familiarActMatcher.group() ).replaceAll( "" );
				RequestLogger.printLine( action );
				RequestLogger.updateSessionLog( action );
			}
		}

		// Check for antique breakage; only run the string search if
		// the player is equipped with the applicable item.

		if ( KoLCharacter.getEquipment( KoLCharacter.HAT ).equals( BROKEN_HELMET ) && responseText.indexOf( "Your antique helmet, weakened" ) != -1 )
		{
			KoLCharacter.setEquipment( KoLCharacter.HAT, EquipmentRequest.UNEQUIP );
			KoLCharacter.processResult( BROKEN_HELMET );
		}

		if ( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).equals( BROKEN_SPEAR ) && responseText.indexOf( "sunders your antique spear" ) != -1 )
		{
			KoLCharacter.setEquipment( KoLCharacter.WEAPON, EquipmentRequest.UNEQUIP );
			KoLCharacter.processResult( BROKEN_SPEAR );
		}

		if ( KoLCharacter.getEquipment( KoLCharacter.OFFHAND ).equals( BROKEN_SHIELD ) && responseText.indexOf( "Your antique shield, weakened" ) != -1 )
		{
			KoLCharacter.setEquipment( KoLCharacter.OFFHAND, EquipmentRequest.UNEQUIP );
			KoLCharacter.processResult( BROKEN_SHIELD );
		}

		if ( KoLCharacter.getEquipment( KoLCharacter.PANTS ).equals( BROKEN_GREAVES ) && responseText.indexOf( "Your antique greaves, weakened" ) != -1 )
		{
			KoLCharacter.setEquipment( KoLCharacter.PANTS, EquipmentRequest.UNEQUIP );
			KoLCharacter.processResult( BROKEN_GREAVES );
		}

		updateMonsterHealth( responseText );

		while ( blindIndex != -1 )
		{
			RequestLogger.printLine( "You acquire... something." );
			if ( KoLSettings.getBooleanProperty( "logAcquiredItems" ) )
				RequestLogger.updateSessionLog( "You acquire... something." );

			blindIndex = responseText.indexOf( "... something.</div>", blindIndex + 1 );
		}

		// Reset round information if the battle is complete.
		// This is recognized when fight.php has no data.

		if ( responseText.indexOf( "fight.php" ) == -1 )
		{
			clearInstanceData();
			return;
		}
	}

	private static final Pattern BANG_POTION_PATTERN = Pattern.compile( "You throw the (.*?) potion at your opponent.?.  It shatters against .*?[,\\.] (.*?)\\." );

	private static final void parseBangPotion( String responseText )
	{
		Matcher bangMatcher = BANG_POTION_PATTERN.matcher( responseText );
		while ( bangMatcher.find() )
		{
			int potionId = TradeableItemDatabase.getItemId( bangMatcher.group(1) + " potion" );

			String effectText = bangMatcher.group(2);
			String effectData = null;

			if ( effectText.indexOf( "wino" ) != -1 )
				effectData = "inebriety";
			else if ( effectText.indexOf( "better" ) != -1 )
				effectData = "healing";
			else if ( effectText.indexOf( "confused" ) != -1 )
				effectData = "confusion";
			else if ( effectText.indexOf( "stylish" ) != -1 )
				effectData = "blessing";
			else if ( effectText.indexOf( "blink" ) != -1 )
				effectData = "detection";
			else if ( effectText.indexOf( "yawn" ) != -1 )
				effectData = "sleepiness";
			else if ( effectText.indexOf( "smarter" ) != -1 )
				effectData = "mental acuity";
			else if ( effectText.indexOf( "stronger" ) != -1 )
				effectData = "ettin strength";
			else if ( effectText.indexOf( "disappearing" ) != -1 )
				effectData = "teleportitis";

			ConsumeItemRequest.ensureUpdatedPotionEffects();

			if ( effectData != null )
				KoLSettings.setUserProperty( "lastBangPotion" + potionId, effectData );
		}
	}

		// You hold the rough stone sphere up in the air.
	private static final Pattern STONE_SPHERE_PATTERN = Pattern.compile( "You hold the (.*?) stone sphere up in the air.*?It radiates a (.*?)," );

	private static final void parseStoneSphere( String responseText )
	{
		Matcher sphereMatcher = STONE_SPHERE_PATTERN.matcher( responseText );
		while ( sphereMatcher.find() )
		{
			int sphereId = TradeableItemDatabase.getItemId( sphereMatcher.group(1) + " stone sphere" );

			if ( sphereId == -1 )
				continue;

			String effectText = sphereMatcher.group(2);
			String effectData = null;

			// "It radiates a bright red light, and a gout of flame
			// blasts out of it"
			if ( effectText.equals( "bright red light" ) )
				effectData = "fire";

			// "It radiates a bright yellow light, and a bolt of
			// lightning arcs towards your opponent"
			else if ( effectText.equals( "bright yellow light" ) )
				effectData = "lightning";

			// "It radiates a bright blue light, and an ethereal
			// mist pours out of it"
			else if ( effectText.equals( "bright blue light" ) )
				effectData = "water";

			// "It radiates a bright green light, and vines shoot
			// out of it"
			else if ( effectText.equals( "bright green light" ) )
				effectData = "plants";

			ensureUpdatedSphereEffects();

			if ( effectData != null )
				KoLSettings.setUserProperty( "lastStoneSphere" + sphereId, effectData );
		}
	}

	public static final void ensureUpdatedSphereEffects()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastStoneSphereReset" );
		if ( lastAscension < KoLCharacter.getAscensions() )
		{
			KoLSettings.setUserProperty( "lastStoneSphereReset", String.valueOf( KoLCharacter.getAscensions() ) );
			for ( int i = 2174; i <= 2177; ++i )
				KoLSettings.setUserProperty( "lastStoneSphere" + i, "" );
		}
	}

	public static final String stoneSphereName( int itemId )
	{	return stoneSphereName( itemId, TradeableItemDatabase.getItemName( itemId ) );
	}

	public static final String stoneSphereName( int itemId, String name )
	{
		ensureUpdatedSphereEffects();
		String effect = KoLSettings.getUserProperty( "lastStoneSphere" + itemId );
		if ( effect.equals( "" ) )
			return name;

		return name + " of " + effect;
	}

	private static final void updateMonsterHealth( String responseText )
	{
		// Check if fumbled first, since that causes a special case later.

		boolean fumbled = FUMBLE_PATTERN.matcher( responseText ).find();

		// Monster damage is verbose, so accumulate in a single variable
		// for the entire results and just show the total.

		int damageThisRound = 0;

		Matcher damageMatcher = ELEMENTAL_PATTERN.matcher( responseText );
		while ( damageMatcher.find() )
			damageThisRound += StaticEntity.parseInt( damageMatcher.group(1) );

		damageMatcher = PHYSICAL_PATTERN.matcher( responseText );

		for ( int i = 0; damageMatcher.find(); ++i )
		{
			// In a fumble, the first set of text indicates that there is
			// no actual damage done to the monster.

			if ( i == 0 && fumbled )
				continue;

			// Currently, all of the explicit attack messages that preceed
			// the number all imply that this is not damage against the
			// monster or is damage that should not count (reap/sow X damage.)

			if ( !damageMatcher.group(1).equals( "" ) )
				continue;

			damageThisRound += StaticEntity.parseInt( damageMatcher.group(2) );

			// The last string contains all of the extra damage
			// from dual-wielding or elemental damage, e.g. "(+3) (+10)".

			Matcher secondaryMatcher = SECONDARY_PATTERN.matcher( damageMatcher.group(3) );
			while ( secondaryMatcher.find() )
				damageThisRound += StaticEntity.parseInt( secondaryMatcher.group(1) );
		}

		// Mosquito and Boss Bat can muck with the monster's HP, but
		// they don't have normal text.

		if ( KoLCharacter.getFamiliar().getRace().equals( "Mosquito" ) )
		{
			damageMatcher = MOSQUITO_PATTERN.matcher( responseText );
			if ( damageMatcher.find() )
				damageThisRound += StaticEntity.parseInt( damageMatcher.group(1) );
		}

		damageMatcher = BOSSBAT_PATTERN.matcher( responseText );
		if ( damageMatcher.find() )
			damageThisRound += StaticEntity.parseInt( damageMatcher.group(1) );

		// Done with all processing for monster damage, now handle responseText.

		healthModifier += damageThisRound;
		if ( !KoLSettings.getBooleanProperty( "logMonsterHealth" ) )
			return;

		StringBuffer action = new StringBuffer();

		if ( damageThisRound != 0 )
		{
			action.append( "Round " );
			action.append( currentRound - 1 );
			action.append( ": " );
			action.append( encounterLookup );

			if ( damageThisRound > 0 )
			{
				action.append( " takes " );
				action.append( damageThisRound );
				action.append( " damage." );
			}
			else
			{
				action.append( " heals " );
				action.append( -1 * damageThisRound );
				action.append( " hit points." );
			}

			RequestLogger.printLine( action.toString() );
			RequestLogger.updateSessionLog( action.toString() );
		}

		// Even though we don't have an exact value, at least try to
		// detect if the monster's HP has changed.  Once spaded, we can
		// insert some minimal/maximal values here.

		if ( GHUOL_HEAL.matcher( responseText ).find() || NS_HEAL.matcher( responseText ).find() )
		{
			action.setLength( 0 );
			action.append( "Round " );
			action.append( currentRound - 1 );
			action.append( ": " );
			action.append( encounterLookup );

			action.append( " heals an unspaded amount of hit points." );

			RequestLogger.printLine( action.toString() );
			RequestLogger.updateSessionLog( action.toString() );
		}
	}

	private static final void clearInstanceData()
	{
		missingGremlinTool = null;
		castCleesh = false;
		jiggledChefstaff = false;
		desiredScroll = null;

		offenseModifier = 0;
		defenseModifier = 0;
		healthModifier = 0;

		action1 = null;
		action2 = null;

		currentRound = 0;
		preparatoryRounds = 0;

		if ( encounterLookup.startsWith( "naughty sorceress" ) && KoLSettings.getBooleanProperty( "lucreCoreLeaderboard" ) )
			(new Thread( new LucreCoreUpdater() )).start();
	}

	private static class LucreCoreUpdater implements Runnable
	{
		public void run()
		{
			(new MuseumRequest( new Object [] { LUCRE.getInstance( LUCRE.getCount( inventory ) ) }, true )).run();
			(new GreenMessageRequest( "koldbot", "Completed ascension." )).run();
		}
	}

	private static final int getActionCost()
	{
		if ( action1.equals( "attack" ) )
			return 0;

		if ( action1.startsWith( "item" ) )
			return 0;

		return ClassSkillsDatabase.getMPConsumptionById( StaticEntity.parseInt( action1 ) );
	}

	private static final boolean hasActionCost( int itemId )
	{
		switch ( itemId )
		{
		case 2:		// seal tooth
		case 4:		// turtle totem
		case 8:		// spices
		case 536:	// dictionary
		case 1316:	// facsimile dictionary
		case 2174:	// mossy stone sphere
		case 2175:	// smooth stone sphere
		case 2176:	// cracked stone sphere
		case 2177:	// rough stone sphere
		case 2404:	// jam band flyers
		case 2405:	// rock band flyers
		case 2497:	// molybdenum magnet
		case 2678:	// spectre scepter
			return false;

		case 829:  // Anti-Anti-Antidote

			for ( int i = 0; i < activeEffects.size(); ++i )
				if ( ((AdventureResult)activeEffects.get(i)).getName().indexOf( "Poison" ) != -1 )
					return true;

			return false;

		default:

			return true;
		}
	}

	public static final void payActionCost()
	{
		if ( action1 == null || action1.equals( "" ) )
			return;

		if ( action1.equals( "attack" ) || action1.equals( "runaway" ) || action1.equals( "steal" ) )
			return;

		if ( action1.equals( "jiggle" ) )
		{
			jiggledChefstaff = true;
			return;
		}

		if ( !action1.startsWith( "skill" ) )
		{
			if ( currentRound == 0 )
				return;

			int id1 = StaticEntity.parseInt( action1 );

			if ( hasActionCost( id1 ) )
			{
				if ( id1 == SOLDIER.getItemId() )
				{
					// A toy soldier consumes tequila.

					if ( inventory.contains( TEQUILA ) )
						StaticEntity.getClient().processResult( TEQUILA );

					// Item is not consumed whether or not
					// you can pay the cost.
				}
				else if ( id1 == MERCENARY.getItemId() )
				{
					// A toy mercenary consumes 5-10 meat

					// A sidepane refresh at the end of the
					// battle will re-synch everything.

					// Item is not consumed whether or not
					// you can pay the cost.
				}
				else
				{
					// Anything else uses up the item.
					StaticEntity.getClient().processResult( new AdventureResult( id1, -1 ) );
				}
			}

			if ( action2 == null || action2.equals( "" ) )
				return;

			int id2 = StaticEntity.parseInt( action2 );

			if ( hasActionCost( id2 ) )
			{
				if ( id2 == SOLDIER.getItemId() )
				{
					// A toy soldier consumes tequila.

					if ( inventory.contains( TEQUILA ) )
						StaticEntity.getClient().processResult( TEQUILA );

					// Item is not consumed whether or not
					// you can pay the cost.
				}
				else if ( id2 == MERCENARY.getItemId() )
				{
					// A toy mercenary consumes 5-10 meat.

					// A sidepane refresh at the end of the
					// battle will re-synch everything.

					// Item is not consumed whether or not
					// you can pay the cost.
				}
				else
				{
					// Anything else uses up the item.
					StaticEntity.getClient().processResult( new AdventureResult( id2, -1 ) );
				}
			}

			return;
		}

		int skillId = StaticEntity.parseInt( action1.substring(5) );
		int mpCost = ClassSkillsDatabase.getMPConsumptionById( skillId );

		switch ( skillId )
		{
		case 2005: // Shieldbutt
			offenseModifier -= 5;
			defenseModifier -= 5;
			break;

		case 5003: // Disco Eye-Poke
			offenseModifier -= 1;
			defenseModifier -= 1;
			break;

		case 5005: // Disco Dance of Doom
			offenseModifier -= 3;
			defenseModifier -= 3;
			break;

		case 5008: // Disco Dance II: Electric Boogaloo
			offenseModifier -= 5;
			defenseModifier -= 5;
			break;

		case 5012: // Disco Face Stab
			offenseModifier -= 7;
			defenseModifier -= 7;
			break;

		case 5019: // Tango of Terror
			offenseModifier -= 6;
			defenseModifier -= 6;
		}

		if ( mpCost > 0 )
			StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MP, 0 - mpCost ) );
	}

	public int getAdventuresUsed()
	{	return 0;
	}

	public static final String getNextTrackedRound()
	{
		if ( !isTrackingFights )
			return RequestEditorKit.getFeatureRichHTML( "fight.php", lastResponseText, true );

		while ( !foundNextRound && !KoLmafia.refusesContinue() )
			delay( 200 );

		if ( !foundNextRound || KoLmafia.refusesContinue() )
			isTrackingFights = false;
		else if ( isTrackingFights )
			isTrackingFights = currentRound != 0;

		foundNextRound = false;
		return RequestEditorKit.getFeatureRichHTML( isTrackingFights ? "fight.php?action=script" : "fight.php", lastResponseText, true );
	}

	public static final int getCurrentRound()
	{	return currentRound;
	}

	public static final boolean alreadyJiggled()
	{	return jiggledChefstaff;
	}

	public static final String missingGremlinTool()
	{	return missingGremlinTool;
	}

	public static final void beginTrackingFights()
	{
		isTrackingFights = true;
		foundNextRound = false;
	}

	public static final void stopTrackingFights()
	{
		isTrackingFights = false;
		foundNextRound = false;
	}

	public static final boolean isTrackingFights()
	{	return isTrackingFights;
	}

	public static final String getLastMonsterName()
	{	return encounterLookup;
	}

	public static final Monster getLastMonster()
	{	return monsterData;
	}

	public static final boolean registerRequest( boolean isExternal, String urlString )
	{
		if ( !urlString.startsWith( "fight.php" ) )
			return false;

		ConsumeItemRequest.resetItemUsed();

		action1 = null;
		action2 = null;

		if ( urlString.equals( "fight.php" ) )
			return true;

		boolean shouldLogAction = KoLSettings.getBooleanProperty( "logBattleAction" );
		StringBuffer action = new StringBuffer();

		// Begin logging all the different combat actions and storing
		// relevant data for post-processing.

		if ( shouldLogAction )
			action.append( "Round " + currentRound + ": " + KoLCharacter.getUserName() + " " );

		if ( urlString.indexOf( "runaway" ) != -1 )
		{
			action1 = "runaway";
			if ( shouldLogAction )
				action.append( "casts RETURN!" );
		}
		else if ( urlString.indexOf( "steal" ) != -1 )
		{
			action1 = "steal";
			if ( shouldLogAction )
				action.append( "tries to steal an item!" );
		}
		else if ( urlString.indexOf( "attack" ) != -1 )
		{
			action1 = "attack";
			if ( shouldLogAction )
				action.append( "attacks!" );
		}
		else if ( urlString.indexOf( "chefstaff" ) != -1 )
		{
			action1 = "jiggle";
			if ( shouldLogAction )
			{
				action.append( "jiggles the " );
				action.append( KoLCharacter.getEquipment( KoLCharacter.WEAPON ).getName() );
			}
		}
		else
		{
			Matcher skillMatcher = SKILL_PATTERN.matcher( urlString );
			if ( skillMatcher.find() )
			{
				if ( isInvalidThrustSmack( skillMatcher.group(1) ) )
					return true;

				String skill = ClassSkillsDatabase.getSkillName( StaticEntity.parseInt( skillMatcher.group(1) ) );
				if ( skill == null )
				{
					if ( shouldLogAction )
						action.append( "casts CHANCE!" );
				}
				else
				{
					action1 = CombatSettings.getShortCombatOptionName( "skill " + skill );
					if ( shouldLogAction )
						action.append( "casts " + skill.toUpperCase() + "!" );
				}
			}
			else
			{
				Matcher itemMatcher = ITEM1_PATTERN.matcher( urlString );
				if ( itemMatcher.find() )
				{
					String item = TradeableItemDatabase.getItemName( StaticEntity.parseInt( itemMatcher.group(1) ) );
					if ( item == null )
					{
						if ( shouldLogAction )
							action.append( "plays Garin's Harp" );
					}
					else
					{
						action1 = CombatSettings.getShortCombatOptionName( item );
						if ( shouldLogAction )
							action.append( "uses the " + item );
					}

					itemMatcher = ITEM2_PATTERN.matcher( urlString );
					if ( itemMatcher.find() )
					{
						item = TradeableItemDatabase.getItemName( StaticEntity.parseInt( itemMatcher.group(1) ) );
						if ( item != null )
						{
							action2 = CombatSettings.getShortCombatOptionName( item );
							if ( shouldLogAction )
								action.append( " and uses the " + item );
						}
					}

					if ( shouldLogAction )
						action.append( "!" );
				}
			}
		}

		if ( shouldLogAction )
		{
			RequestLogger.printLine( action.toString() );
			RequestLogger.updateSessionLog( action.toString() );
		}

		return true;
	}
}
