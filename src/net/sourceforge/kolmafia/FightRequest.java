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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.MonsterDatabase.Monster;

public class FightRequest extends KoLRequest
{
	public static final FightRequest INSTANCE = new FightRequest();

	private static final AdventureResult AMNESIA = new AdventureResult( "Amnesia", 1, true );
	private static final AdventureResult CUNCTATITIS = new AdventureResult( "Cunctatitis", 1, true );

	private static final AdventureResult ANTIDOTE = new AdventureResult( 829, 1 );
	private static final AdventureResult SOLDIER = new AdventureResult( 1397, 1 );
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
	private static final AdventureResult MERCENARY = new AdventureResult( 2139, 1 );

	private static final String TOOTH_ACTION = "item" + TOOTH.getItemId();
	private static final String TURTLE_ACTION = "item" + TURTLE.getItemId();
	private static final String SPICES_ACTION = "item" + SPICES.getItemId();
	private static final String MERCENARY_ACTION = "item" + MERCENARY.getItemId();

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
		"baiowulf",
		"count bakula",
		"crazy bastard",
		"hockey elemental",
		"hypnotist of hey deze",
		"infinite meat bug",
		"knott slanding",
		"master of thieves",
		"temporal bandit"
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

		// Fight automation is still considered automation.
		// If the player drops below the threshold, then go
		// ahead and halt the battle.

		if ( !StaticEntity.getClient().runThresholdChecks() )
		{
			action1 = "abort";
			return;
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

		if ( activeEffects.contains( AMNESIA ) )
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
				}
				else
				{
					KoLmafia.updateDisplay( ABORT_STATE, "You don't have enough " + TradeableItemDatabase.getItemName( item2 ) );
					action1 = "abort";
				}

				return;
			}

			if ( item1 == ANTIDOTE.getItemId() || item1 == DICTIONARY1.getItemId() || item2 == DICTIONARY2.getItemId() )
			{
				if ( inventory.contains( MERCENARY ) )
				{
					action2 = MERCENARY_ACTION;
					this.addFormField( "whichitem2", String.valueOf( MERCENARY.getItemId() ) );
				}
				else if ( inventory.contains( TOOTH ) )
				{
					action2 = TOOTH_ACTION;
					this.addFormField( "whichitem2", String.valueOf( TOOTH.getItemId() ) );
				}
				else if ( inventory.contains( TURTLE ) )
				{
					action2 = TURTLE_ACTION;
					this.addFormField( "whichitem2", String.valueOf( TURTLE.getItemId() ) );
				}
				else if ( inventory.contains( SPICES ) )
				{
					action2 = SPICES_ACTION;
					this.addFormField( "whichitem2", String.valueOf( SPICES.getItemId() ) );
				}
			}
			else if ( itemCount >= 2 )
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

	private static final String [][] HIPPY_MESSAGES =
	{
		// 2 total
		{
                        // You see one of your frat brothers take out an
                        // M.C. Escher drawing and show it to a War Hippy
                        // (space) Cadet. The hippy looks at it and runs away
                        // screaming about how he doesn't know which way is
                        // down.
			"M.C. Escher",

                        // You see a hippy loading his didgeridooka, but before
                        // he can fire it, he's dragged off the battlefield by
                        // another hippy protesting the war.
			"protesting the war",

                        // You see a "Baker Company" hippy take one bite too
                        // many from a big plate of brownies, then curl up to
                        // take a nap. Looks like he's out of commission for a
                        // while.
			"Baker Company",

                        // You see a hippy a few paces away suddenly realize
                        // that he's violating his deeply held pacifist
                        // beliefs, scream in horror, and run off the
                        // battlefield.
			"pacifist beliefs",

                        // You look over and see a fellow frat brother
                        // garotting a hippy shaman with the hippy's own
                        // dreadlocks. "Right on, bra!" you shout.
			"garotting",

                        // You glance over and see one of your frat brothers
                        // hosing down a hippy with soapy water. You laugh and
                        // run over for a high-five.
			"soapy water",

                        // You glance out over the battlefield and see a hippy
                        // from the F.R.O.G. division get the hiccups and knock
                        // himself out on his own nasty breath.
			"nasty breath",

                        // You see one of the War Hippy's "Jerry's Riggers"
                        // sneeze midway through making a bomb, inadvertently
                        // turning himself into smoke and dust. In the wind.
			"smoke and dust",

                        // You see a frat boy hose down a hippy Airborne
                        // Commander with sugar water. You applaud as the
                        // Commander gets attacked by her own ferrets.
			"her own ferrets",

                        // You see one of your frat brothers paddling a hippy
                        // who seems to be enjoying it. You say "uh, keep up
                        // the good work... bra... yeah."
			"enjoying it",

                        // As the hippy falls, you see a hippy a few yards away
                        // clutch his chest and fall over, too. Apparently the
                        // hippy you were fighting was just the astral
                        // projection of another hippy several yards
                        // away. Freaky.
			"astral projection",
		},
		// 4 total
		{ 
                        // You see a War Frat Grill Sergeant hose down three
                        // hippies with white-hot chicken wing sauce. You love
                        // the smell of jabañero in the morning. It smells like
                        // victory.
			"three hippies",

                        // As you finish your fight, you see a nearby Wartender
                        // mixing up a cocktail of vodka and pain for a trio of
                        // charging hippies. "Right on, bra!" you shout.
			"trio",

                        // You see one of your frat brothers douse a trio of
                        // nearby hippies in cheap aftershave. They scream and
                        // run off the battlefield to find some incense to
                        // burn.
			// "trio",

                        // You see one of your frat brothers line up three
                        // hippies for simultaneous paddling. Don't bathe --
                        // that's a paddlin'. Light incense -- that's a
                        // paddlin'. Paddlin' a homemade canoe -- oh, you
                        // better believe that's a paddlin'.
			// "three hippies",

                        // You see one of the "Fortunate 500" make a quick call
                        // on his cell phone. Some mercenaries drive up, shove
                        // three hippies into their bitchin' meat car, and
                        // drive away.
			// "three hippies",

                        // As you deliver the finishing blow, you see a frat
                        // boy lob a sake bomb into a trio of nearby
                        // hippies. "Nice work, bra!" you shout.
			// "trio",
		},
		// 8 total
		{ 
                        // You see one of your Beer Bongadier frat brothers use
                        // a complicated beer bong to spray cheap, skunky beer
                        // on a whole squad hippies at once. "Way to go, bra!"
                        // you shout.
			"skunky",

                        // You glance over and see one of the Roaring Drunks
                        // from the 151st Division overturning a mobile sweat
                        // lodge in a berserker rage. Several sweaty, naked
                        // hippies run out and off the battlefield, brushing
                        // burning coals out of their dreadlocks.
			"burning coals",

                        // You see one of your frat brothers punch an
                        // F.R.O.G. in the solar plexus, then aim the
                        // subsequent exhale at a squad of hippies standing
                        // nearby. You watch all of them fall to the ground,
                        // gasping for air.
			"solar plexus",

                        // You see a Grillmaster flinging hot kabobs as fast as
                        // he can make them. He skewers one, two, three, four,
                        // five, six... seven! Seven hippies! Ha ha ha!
			"seven",
		},
		// 16 total
		{ 
                        // A streaking frat boy runs past a nearby funk of
                        // hippies. One look at him makes the hippies have to
                        // go ponder their previous belief that the naked human
                        // body is a beautiful, wholesome thing.
			"naked human body",

                        // You see one of the Fortunate 500 call in an air
                        // strike. His daddy's personal airship flies over and
                        // dumps cheap beer all over a nearby funk of hippies.
			"personal airship",

                        // You look over and see a platoon of frat boys round
                        // up a funk of hippies and take them prisoner. Since
                        // being a POW of the frat boys involves a lot of beer
                        // drinking, you're slightly envious. Since it also
                        // involves a lot of paddling, you're somewhat less so.
			"slightly envious",

                        // You see a kegtank and a mobile sweat lodge facing
                        // off in the distance. Since the kegtank's made of
                        // steel and the sweat lodge is made of wood, you can
                        // guess the outcome.
			"guess the outcome",
		},
		// 32 total
		{ 
                        // You see an entire regiment of hippies throw down
                        // their arms (and their weapons) in disgust and walk
                        // off the battlefield. War! What is it good for?
                        // Absolutely nothing!
			"Absolutely nothing!",

                        // You see a squadron of police cars drive up, and a
                        // squad of policemen arrest a funk of hippies who were
                        // sitting around inhaling smoke from some sort of
                        // glass sculpture.
			"glass sculpture",

                        // You see a kegtank rumble through the battlefield,
                        // firing beer cans out of its top turret. It mows
                        // down, like, 30 hippies in a row, but then runs out
                        // of ammo. They really should have stocked one more
                        // six-pack.
			"one more six-pack",
		},
		// 64 total
		{ 
                        // You see the a couple of frat boys attaching big,
                        // long planks of wood to either side of a
                        // kegtank. Then they drive through the rank hippy
                        // ranks, mass-paddling as they go. Dozens of hippies
                        // flee the battlefield, tears in their filthy, filthy
                        // eyes.
			"planks of wood",

                        // You see one of the "Fortunate 500" hang up his PADL
                        // phone, looking smug. Several SWAT vans of police in
                        // full riot gear pull up, and one of them informs the
                        // hippies through a megaphone that this is not a
                        // "designated free speech zone." The hippies throw
                        // rocks and bottles at the police, but most of them
                        // end up shoved into paddy wagons in chains. Er, the
                        // hippies are the ones in the chains. Not the wagons.
			"SWAT",

                        // You see a couple of frat boys stick a fuse into a
                        // huge wooden barrel, light the fuse, and roll it down
                        // the hill to where the hippy forces are
                        // fighting. Judging by the big bada boom that follows,
                        // that barrel was either full of scotch or gunpowder,
                        // and possibly both.
			"wooden barrel",
		},
	};

	private static final String [][] FRAT_MESSAGES =
	{
		// 2 total
		{
                        // You look over and see a fellow hippy warrior using
                        // his dreadlocks to garotte a frat warrior. "Way to
                        // enforce karmic retribution!" you shout.
			"garotte",

                        // You see a Green Gourmet give a frat boy a plate of
                        // herbal brownies. The frat boy scarfs them all, then
                        // wanders off staring at his hands.
			"herbal brownies",

                        // Elsewhere on the battlefield, you see a fellow hippy
                        // grab a frat warrior's paddle and give the frat boy a
                        // taste of his own medicine. I guess that could count
                        // as homeopathic healing...
			"homeopathic healing",

                        // You see a Wartender pour too much lighter fluid on
                        // his grill and go up in a great ball of
                        // fire. Goodness gracious!
			"Goodness gracious!",

                        // You see a Fire Spinner blow a gout of flame onto a
                        // Wartender's grill, charring all the Wartender's
                        // meaty goodness. The Wartender wanders off crying.
			"meaty goodness",

                        // Nearby, you see one of your sister hippies
                        // explaining the rules of Ultimate Frisbee to a member
                        // of the frat boys' "armchair infantry." His eyes
                        // glaze and he passes out.
			"Ultimate Frisbee",

                        // You see a member of the frat boy's 151st division
                        // pour himself a stiff drink, knock it back, and
                        // finally pass out from alcohol poisoning.
			"alcohol poisoning",

                        // You glance over your shoulder and see a squadron of
                        // winged ferrets descend on a frat warrior, entranced
                        // by the sun glinting off his keg shield.
			"winged ferrets",

                        // You see a hippy shaman casting a Marxist spell over
                        // a member of the "Fortunate 500" division of the frat
                        // boy army. The frat boy gets on his cell phone and
                        // starts redistributing his wealth.
			"Marxist spell",

                        // You see a frat boy warrior pound a beer, smash the
                        // can against his forehead, and pass out. You chuckle
                        // to yourself.
			"smash the can",

                        // You see an F.R.O.G. crunch a bulb of garlic in his
                        // teeth and breathe all over a nearby frat boy, who
                        // turns green and falls over.
			"bulb of garlic",
		},
		// 4 total
		{ 
                        // You hear chanting behind you, and turn to see thick,
                        // ropy (almost anime-esque) vines sprout from a War
                        // Hippy Shaman's dreads and entangle three attacking
                        // frat boy warriors.
			"three attacking",

                        // Nearby, you see an Elite Fire Spinner take down
                        // three frat boys in a whirl of flame and pain.
			"three frat boys",

                        // You look over and see three ridiculously drunk
                        // members of the 151st Division run together for a
                        // three-way congratulatory headbutt, which turns into
                        // a three-way concussion.
			"three-way",

                        // You see a member of the Fortunate 500 take a phone
                        // call, hear him holler something about a stock market
                        // crash, then watch him and two of his fortunate
                        // buddies run off the battlefield in a panic.
			"him and two",

                        // Over the next hill, you see three frat boys abruptly
                        // vanish into a cloud of green smoke. Apparently the
                        // Green Ops Soldiers are on the prowl.
			// "three frat boys",

                        // You hear excited chittering overhead, and look up to
                        // see a squadron of winged ferrets making a
                        // urine-based bombing run over three frat boys. The
                        // frat boys quickly run off the field to find some
                        // cheap aftershave to cover up the smell.
			// "three frat boys",
		},
		// 8 total
		{ 
                        // Nearby, a War Hippy Elder Shaman nods almost
                        // imperceptibly. A Kegtank hits a gopher hole and tips
                        // over. A squad of confused frat boys stumble out and
                        // off the battlefield.
			"squad of",

                        // You leap out of the way of a runaway Mobile Sweat
                        // Lodge, then watch it run over one, two, three, four,
                        // five, six, seven! Seven frat boys! Ha ha ha!
			"seven",

                        // A few yards away, one of the Jerry's Riggers hippies
                        // detonates a bomb underneath a Wartender's grill. An
                        // entire squad of frat boys run from the battlefield
                        // under the onslaught of red-hot coals.
			// "squad of",

                        // You look over and see one of Jerry's Riggers placing
                        // land mines he made out of paperclips, rubber bands,
                        // and psychedelic mushrooms. A charging squad of frat
                        // boys trips them, and is subsequently dragged off the
                        // field ranting about the giant purple squirrels.
			// "squad of",
		},
		// 16 total
		{ 
                        // You turn to see a nearby War Hippy Elder Shaman
                        // making a series of complex hand gestures. A flock of
                        // pigeons swoops down out of the sky and pecks the
                        // living daylights out of a whole platoon of frat
                        // boys.
			"platoon of",

                        // You see a platoon of charging frat boys get mowed
                        // down by a hippy. Remember, kids, a short-range
                        // weapon (like a paddle) usually does poorly against a
                        // long-range weapon (like a didgeridooka).
			// "platoon of",

                        // You look over and see a funk of hippies round up a
                        // bunch of frat boys to take as prisoners of
                        // war. Since being a hippy prisoner involves lounging
                        // around inhaling clouds of smoke and eating brownies,
                        // you're somewhat jealous. Since it also involves
                        // non-stop olfactory assault, you're somewhat less so.
			"funk of hippies",

                        // Nearby, a platoon of frat boys is rocking a mobile
                        // sweat lodge back and forth, trying to tip it
                        // over. When they succeed, they seem surprised by the
                        // hot coals and naked hippies that pour forth, and the
                        // frat boys run away screaming.
			// "platoon of",
		},
		// 32 total
		{ 
                        // A mobile sweat lodge rumbles into a regiment of frat
                        // boys and the hippies inside open all of its vents
                        // simultaneously. Steam that smells like a dozen
                        // baking (and baked) hippies pours out, enveloping the
                        // platoon and sending the frat boys into fits of
                        // nauseated coughing.
			"regiment",

                        // You see a squadron of police cars drive up, and a
                        // squad of policemen arrest an entire regiment of frat
                        // boys. You hear cries of "She told me she was 18,
                        // bra!" and "I told you, I didn't hit her with a
                        // roofing shingle!" as they're dragged off the
                        // battlefield.
			// "regiment",

                        // You see a regiment of frat boys decide they're tired
                        // of drinking non-alcoholic beer and tired of not
                        // hitting on chicks, so they throw down their arms,
                        // and then their weapons, and head back to the frat
                        // house.
			// "regiment",
		},
		// 64 total
		{ 
                        // You see an airborne commander trying out a new
                        // strategy: she mixes a tiny bottle of rum she found
                        // on one of the frat boy casualties with a little of
                        // the frat boy's blood, then adds that to the ferret
                        // bait. A fleet of ferrets swoops down, eats the bait,
                        // and goes berserk with alcohol/bloodlust. The frat
                        // boys scream like schoolgirls as the ferrets decimate
                        // their ranks.
			"scream like schoolgirls",

                        // You see a couple of hippies rigging a mobile sweat
                        // lodge with a public address system. They drive it
                        // through the battlefield, blaring some concept album
                        // about the dark side of Ronald. Frat boys fall asleep
                        // en masse, helpless before music that's horribly
                        // boring if you're not under the influence of
                        // mind-altering drugs.
			"en masse",

                        // You see an elder hippy shaman close her eyes, clench
                        // her fists, and start to chant. She glows with an
                        // eerie green light as storm clouds bubble and roil
                        // overhead. A funnel cloud descends from the
                        // thunderheads and dances through the frat boy ranks,
                        // whisking them up and away like so many miniature
                        // mobile homes.
			"mobile homes",
		},
	};

	private static final boolean findBattlefieldMessage( String responseText, String [] table )
	{
		for ( int i = 0; i < table.length; ++i)
			if ( responseText.indexOf( table[i] ) != -1 )
				return true;
		return false;
	}

	private static final void handleBattlefield( String responseText )
	{
		// Nothing to do until battle is done
		if ( responseText.indexOf( "WINWINWIN" ) != -1 )
			return;

		// Initialize settings if necessary
		ensureUpdatedBattlefield();

		// Figure out how many enemies were defeated
		String [][] table;
		String killCounter;
                String killDelta;
		String questCounter;

		if ( EquipmentDatabase.isWearingOutfit( 33 ) )
		{
			table = HIPPY_MESSAGES;
			killCounter = "hippiesDefeated";
			killDelta = "hippyDelta";
			questCounter = "hippyQuestsCompleted";
		}
		else
		{
			table = FRAT_MESSAGES;
			killCounter = "fratboysDefeated";
			killDelta = "fratboyDelta";
			questCounter = "fratboyQuestsCompleted";
		}

                int quests = 0;
		int delta = 1;
		int test = 2;

		for ( int i = 0; i < table.length; ++i)
		{
			if ( findBattlefieldMessage( responseText, table[i] ) )
			{
                                quests = i + 1;
				delta = test;
				break;
			}
			test *= 2;
		}

		KoLSettings.incrementIntegerProperty( killCounter, delta, 0 );
		KoLSettings.setUserProperty( killDelta, String.valueOf( delta ) );
		KoLSettings.setUserProperty( questCounter, String.valueOf( quests ) );
	}

	public static final void ensureUpdatedBattlefield()
	{
		int lastAscension = KoLSettings.getIntegerProperty( "lastBattlefieldReset" );
		if ( lastAscension == KoLCharacter.getAscensions() )
			return;

		KoLSettings.setUserProperty( "lastBattlefieldReset", String.valueOf( KoLCharacter.getAscensions() ) );
		KoLSettings.setUserProperty( "fratboysDefeated", "0" );
		KoLSettings.setUserProperty( "fratboyDelta", "1" );
		KoLSettings.setUserProperty( "fratboyQuestsCompleted", "0" );
		KoLSettings.setUserProperty( "hippiesDefeated", "0" );
		KoLSettings.setUserProperty( "hippyDelta", "1" );
		KoLSettings.setUserProperty( "hippyQuestsCompleted", "0" );

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

		if ( !activeEffects.contains( CUNCTATITIS ) || responseText.indexOf( "You decide" ) == -1 )
			payActionCost();

		if ( currentRound == 1 )
		{
			// If this is the first round, then register the
			// opponent you are fighting against.

			encounterLookup = CombatSettings.encounterKey( encounter );
			monsterData = MonsterDatabase.findMonster( encounterLookup );

			isTrackingFights = false;
			checkForInitiative( responseText );
		}

		switch ( KoLAdventure.lastAdventureId() )
		{
		case 139:	// Wartime Junkyard
			// Quest gremlins might have a tool.
			handleGremlin( responseText );
			break;

		case 132:	// Battlefield (Frat Uniform)
		case 140:	// Battlefield (Hippy Uniform)
			handleBattlefield( responseText );
			break;
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
		case 2848:	// Gnomitronic Hyperspatial Demodulizer
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
