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
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MPRestoreItemList;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Monster;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.HobopolisDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;

public class FightRequest
	extends GenericRequest
{
	private static final PauseObject PAUSER = new PauseObject();
	public static final FightRequest INSTANCE = new FightRequest();

	private static final AdventureResult AMNESIA = new AdventureResult( "Amnesia", 1, true );
	private static final AdventureResult CUNCTATITIS = new AdventureResult( "Cunctatitis", 1, true );

	public static final AdventureResult DICTIONARY1 = ItemPool.get( ItemPool.DICTIONARY, 1 );
	public static final AdventureResult DICTIONARY2 = ItemPool.get( ItemPool.FACSIMILE_DICTIONARY, 1 );
	private static final AdventureResult SOLDIER = ItemPool.get( ItemPool.TOY_SOLDIER, 1 );
	private static final AdventureResult TEQUILA = ItemPool.get( ItemPool.TEQUILA, -1 );

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

	private static final Pattern FAMILIAR_ACT_PATTERN =
		Pattern.compile( "<table><tr><td align=center.*?</table>", Pattern.DOTALL );
	private static final Pattern FUMBLE_PATTERN =
		Pattern.compile( "You drop your .*? on your .*?, doing [\\d,]+ damage" );
	private static final Pattern ELEMENTAL_PATTERN =
		Pattern.compile( "<font color=[\"]?\\w+[\"]?><b>\\+?([\\d,]+)</b></font> (?:damage|points|HP worth)" );
	private static final Pattern CLEESH_PATTERN =
		Pattern.compile( "You cast CLEESH at your opponent.*?turns into a (\\w*)", Pattern.DOTALL );

	// NOTE: All of the non-empty patterns that can match in the first group
	// imply that the entire expression should be ignored.	If you add one
	// and this is not the case, then correct the use of this Pattern below.

	private static final Pattern PHYSICAL_PATTERN =
		Pattern.compile( "(your blood, to the tune of|stabs you for|sown|You lose|You gain|) (\\d[\\d,]*) (\\([^.]*\\) |)((?:\\w+ ){0,2})(?:damage|points?|notch(?:es)?|to your opponent|force damage)" );

	private static final Pattern SECONDARY_PATTERN = Pattern.compile( "<b>\\+([\\d,]+)</b>" );
	private static final Pattern MOSQUITO_PATTERN =
		Pattern.compile( "sucks some blood out of your opponent and injects it into you.*?You gain ([\\d,]+) hit point" );
	private static final Pattern BOSSBAT_PATTERN =
		Pattern.compile( "until he disengages, two goofy grins on his faces.*?You lose ([\\d,]+)" );
	private static final Pattern GHUOL_HEAL = Pattern.compile( "feasts on a nearby corpse, and looks refreshed\\." );
	private static final Pattern NS_HEAL =
		Pattern.compile( "The Sorceress pulls a tiny red vial out of the folds of her dress and quickly drinks it" );


	private static final AdventureResult TOOTH = ItemPool.get( ItemPool.SEAL_TOOTH, 1);
	private static final AdventureResult TURTLE = ItemPool.get( ItemPool.TURTLE_TOTEM, 1);
	private static final AdventureResult SPICES = ItemPool.get( ItemPool.SPICES, 1);
	private static final AdventureResult MERCENARY = ItemPool.get( ItemPool.TOY_MERCENARY, 1);
	private static final AdventureResult STOMPER = ItemPool.get( ItemPool.MINIBORG_STOMPER, 1);
	private static final AdventureResult LASER = ItemPool.get( ItemPool.MINIBORG_LASER, 1);
	private static final AdventureResult DESTROYER = ItemPool.get( ItemPool.MINIBORG_DESTROYOBOT, 1);
	private static final AdventureResult SHURIKEN = ItemPool.get( ItemPool.PAPER_SHURIKEN, 1);

	private static final String TOOTH_ACTION = "item" + ItemPool.SEAL_TOOTH;
	private static final String TURTLE_ACTION = "item" + ItemPool.TURTLE_TOTEM;
	private static final String SPICES_ACTION = "item" + ItemPool.SPICES;
	private static final String MERCENARY_ACTION = "item" + ItemPool.TOY_MERCENARY;
	private static final String STOMPER_ACTION = "item" + ItemPool.MINIBORG_STOMPER;
	private static final String LASER_ACTION = "item" + ItemPool.MINIBORG_LASER;
	private static final String DESTROYER_ACTION = "item" + ItemPool.MINIBORG_DESTROYOBOT;
	private static final String SHURIKEN_ACTION = "item" + ItemPool.PAPER_SHURIKEN;

	private static final AdventureResult BROKEN_GREAVES = ItemPool.get( ItemPool.ANTIQUE_GREAVES, -1 );
	private static final AdventureResult BROKEN_HELMET = ItemPool.get( ItemPool.ANTIQUE_HELMET, -1 );
	private static final AdventureResult BROKEN_SPEAR = ItemPool.get( ItemPool.ANTIQUE_SPEAR, -1 );
	private static final AdventureResult BROKEN_SHIELD = ItemPool.get( ItemPool.ANTIQUE_SHIELD, -1 );

	private static boolean castCleesh = false;
	private static boolean jiggledChefstaff = false;
	private static int currentRound = 0;
	private static int levelModifier = 0;
	private static int healthModifier = 0;

	private static String action1 = null;
	private static String action2 = null;
	private static Monster monsterData = null;
	private static String encounterLookup = "";

	private static AdventureResult desiredScroll = null;

	private static final AdventureResult SCROLL_334 = ItemPool.get( ItemPool.SCROLL_334, 1);
	public static final AdventureResult SCROLL_668 = ItemPool.get( ItemPool.SCROLL_668, 1);
	private static final AdventureResult SCROLL_30669 = ItemPool.get( ItemPool.SCROLL_30669, 1);
	private static final AdventureResult SCROLL_33398 = ItemPool.get( ItemPool.SCROLL_33398, 1);
	private static final AdventureResult SCROLL_64067 = ItemPool.get( ItemPool.SCROLL_64067, 1);
	public static final AdventureResult SCROLL_64735 = ItemPool.get( ItemPool.GATES_SCROLL, 1);
	public static final AdventureResult SCROLL_31337 = ItemPool.get( ItemPool.ELITE_SCROLL, 1);

	// Ultra-rare monsters
	private static final String[] RARE_MONSTERS =
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
	 * Constructs a new <code>FightRequest</code>. Theprovided will be used to determine whether or not the fight
	 * should be started and/or continued, and the user settings will be used to determine the kind of action1 to be
	 * taken during the battle.
	 */

	private FightRequest()
	{
		super( "fight.php" );
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public static final boolean wonInitiative()
	{
		if ( FightRequest.currentRound != 1 )
			return false;

		return FightRequest.wonInitiative( FightRequest.lastResponseText );
	}

	public static final boolean wonInitiative( String text )
	{
		// Regular encounter
		if ( text.indexOf( "You get the jump" ) != -1 )
			return true;

		// Can Has Cyborger
		if ( text.indexOf( "The Jump: you gets it." ) != -1 )
			return true;

		// Haiku dungeon

		//    Before he sees you,
		//    you're already attacking.
		//    You're sneaky like that.

		if ( text.indexOf( "You're sneaky like that." ) != -1 )
			return true;

		//    You leap at your foe,
		//    throwing caution to the wind,
		//    and get the first strike.

		if ( text.indexOf( "and get the first strike." ) != -1 )
			return true;

		//    You jump at your foe
		//    and strike before he's ready.
		//    Nice and sportsmanlike.

		if ( text.indexOf( "Nice and sportsmanlike." ) != -1 )
			return true;

		return false;
	}

	public void nextRound()
	{
		// When logging in and encountering a fight, always use the
		// attack command to avoid abort problems.

		if ( LoginRequest.isInstanceRunning() )
		{
			FightRequest.action1 = "attack";
			this.addFormField( "action", "attack" );
			return;
		}

		if ( KoLmafia.refusesContinue() )
		{
			FightRequest.action1 = "abort";
			return;
		}

		// First round, KoLmafia does not decide the action.
		// Update accordingly.

		if ( FightRequest.currentRound == 0 )
		{
			FightRequest.action1 = null;
			return;
		}

		// Always let the user see rare monsters

		for ( int i = 0; i < FightRequest.RARE_MONSTERS.length; ++i )
		{
			if ( FightRequest.encounterLookup.indexOf( FightRequest.RARE_MONSTERS[ i ] ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "You have encountered the " + this.encounter );
				FightRequest.action1 = "abort";
				return;
			}
		}

		// Fight automation is still considered automation.
		// If the player drops below the threshold, then go
		// ahead and halt the battle.

		if ( !StaticEntity.getClient().runThresholdChecks() )
		{
			FightRequest.action1 = "abort";
			return;
		}

		this.nextRound( null );
	}

	public void nextRound( String desiredAction )
	{
		if ( desiredAction == null )
		{
			desiredAction = Preferences.getString( "battleAction" );
		}

		FightRequest.action1 = CustomCombatManager.getShortCombatOptionName( desiredAction );
		FightRequest.action2 = null;

		// Adding machine should override custom combat scripts as well,
		// since it's conditions-driven.

		if ( FightRequest.encounterLookup.equals( "rampaging adding machine" ) )
		{
			this.handleAddingMachine();
		}

		// Hulking Constructs also require special handling

		else if ( FightRequest.encounterLookup.equals( "hulking construct" ) )
		{
			this.handleHulkingConstruct();
		}

		// If the user wants a custom combat script, parse the desired
		// action here.

		if ( FightRequest.action1.equals( "custom" ) )
		{
			FightRequest.action1 =
				CustomCombatManager.getSetting(
					FightRequest.encounterLookup, FightRequest.currentRound - 1 - FightRequest.preparatoryRounds );
		}

		// If it is appropriate to try pickpocketing, make the attempt

		else if ( FightRequest.wonInitiative() &&
			  FightRequest.monsterData != null &&
			  FightRequest.monsterData.shouldSteal() )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.action1 = "steal";
			this.addFormField( "action", "steal" );
			return;
		}

		// If the person wants to use their own script,
		// then this is where it happens.

		if ( FightRequest.action1.startsWith( "consult" ) )
		{
			FightRequest.isUsingConsultScript = true;
			String scriptName = FightRequest.action1.substring( "consult".length() ).trim();

			Interpreter interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
			if ( interpreter != null )
			{
				int initialRound = FightRequest.currentRound;
				interpreter.execute( "main", new String[]
				{
					String.valueOf( FightRequest.currentRound ),
					FightRequest.encounterLookup,
					FightRequest.lastResponseText
				} );

				if ( KoLmafia.refusesContinue() || initialRound == FightRequest.currentRound )
				{
					FightRequest.action1 = "abort";
				}

				return;
			}

			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Consult script '" + scriptName + "' not found." );
			FightRequest.action1 = "abort";
			return;
		}

		// Let the de-level action figure out what
		// should be done, and then re-process.

		if ( FightRequest.action1.startsWith( "delevel" ) )
		{
			FightRequest.action1 = this.getMonsterWeakenAction();
		}

		this.updateCurrentAction();
	}

	public static final String getCurrentKey()
	{
		return CustomCombatManager.encounterKey( FightRequest.encounterLookup );
	}

	private void updateCurrentAction()
	{
		if ( FightRequest.action1.equals( "abort" ) )
		{
			// If the user has chosen to abort combat, flag it.
			FightRequest.action1 = "abort";
			return;
		}

		// User wants to run away
		if ( FightRequest.action1.indexOf( "run" ) != -1 && FightRequest.action1.indexOf( "away" ) != -1 )
		{
			FightRequest.action1 = "runaway";
			this.addFormField( "action", FightRequest.action1 );
			return;
		}

		// User wants a regular attack
		if ( FightRequest.action1.startsWith( "attack" ) )
		{
			FightRequest.action1 = "attack";
			this.addFormField( "action", FightRequest.action1 );
			return;
		}

		if ( FightRequest.action1.startsWith( "twiddle" ) )
		{
			FightRequest.action1 = null;
			return;
		}

		if ( KoLConstants.activeEffects.contains( FightRequest.AMNESIA ) )
		{
			if ( FightRequest.monsterData == null || !FightRequest.monsterData.willUsuallyMiss( FightRequest.levelModifier ) )
			{
				FightRequest.action1 = "attack";
				this.addFormField( "action", FightRequest.action1 );
				return;
			}

			FightRequest.action1 = "abort";
			return;
		}

		// Actually steal if the action says to steal

		if ( FightRequest.action1.indexOf( "steal" ) != -1 )
		{
			if ( FightRequest.wonInitiative() &&
                             FightRequest.monsterData != null &&
                             FightRequest.monsterData.shouldSteal() )
			{
				FightRequest.action1 = "steal";
				this.addFormField( "action", "steal" );
				return;
			}

			--FightRequest.preparatoryRounds;
			this.nextRound();
			return;
		}

		// Jiggle chefstaff if the action says to jiggle and we're
		// wielding a chefstaff. Otherwise, skip this action.

		if ( FightRequest.action1.startsWith( "jiggle" ) )
		{
			if ( !FightRequest.jiggledChefstaff && EquipmentManager.usingChefstaff() )
			{
				this.addFormField( "action", "chefstaff" );
				return;
			}

			// You can only jiggle once per round.
			--FightRequest.preparatoryRounds;
			this.nextRound();
			return;
		}

		// If the player wants to use an item, make sure he has one
		if ( !FightRequest.action1.startsWith( "skill" ) )
		{
			int item1, item2;

			int commaIndex = FightRequest.action1.indexOf( "," );
			if ( commaIndex != -1 )
			{
				item1 = StringUtilities.parseInt( FightRequest.action1.substring( 0, commaIndex ) );
				item2 = StringUtilities.parseInt( FightRequest.action1.substring( commaIndex + 1 ) );
			}
			else
			{
				item1 = StringUtilities.parseInt( FightRequest.action1 );
				item2 = -1;
			}

			int itemCount = ( new AdventureResult( item1, 1 ) ).getCount( KoLConstants.inventory );

			if ( itemCount == 0 && item2 != -1)
			{
				item1 = item2;
				item2 = -1;

				itemCount = ( new AdventureResult( item1, 1 ) ).getCount( KoLConstants.inventory );
			}

			if ( itemCount == 0 )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ABORT_STATE, "You don't have enough " + ItemDatabase.getItemName( item1 ) );
				FightRequest.action1 = "abort";
				return;
			}

			if ( item1 == ItemPool.DICTIONARY || item1 == ItemPool.FACSIMILE_DICTIONARY )
			{
				if ( itemCount < 1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "You don't have a dictionary." );
					FightRequest.action1 = "abort";
					return;
				}

				if ( FightRequest.encounterLookup.equals( "rampaging adding machine" ) )
				{
					FightRequest.action1 = "attack";
					return;
				}
			}

			this.addFormField( "action", "useitem" );
			this.addFormField( "whichitem", String.valueOf( item1 ) );

			if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
			{
				return;
			}

			if ( item2 != -1 )
			{
				itemCount = ( new AdventureResult( item2, 1 ) ).getCount( KoLConstants.inventory );

				if ( itemCount > 1 || item1 != item2 && itemCount > 0 )
				{
					FightRequest.action2 = String.valueOf( item2 );
					this.addFormField( "whichitem2", String.valueOf( item2 ) );
				}
				else
				{
					KoLmafia.updateDisplay(
						KoLConstants.ABORT_STATE, "You don't have enough " + ItemDatabase.getItemName( item2 ) );
					FightRequest.action1 = "abort";
				}

				return;
			}

			if ( item1 == ItemPool.ANTIDOTE || item1 == ItemPool.DICTIONARY || item1 == ItemPool.FACSIMILE_DICTIONARY )
			{
				if ( KoLConstants.inventory.contains( FightRequest.MERCENARY ) )
				{
					FightRequest.action2 = FightRequest.MERCENARY_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.MERCENARY.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.DESTROYER ) )
				{
					FightRequest.action2 = FightRequest.DESTROYER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.DESTROYER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.LASER ) )
				{
					FightRequest.action2 = FightRequest.LASER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.LASER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.STOMPER ) )
				{
					FightRequest.action2 = FightRequest.STOMPER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.STOMPER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.TOOTH ) )
				{
					FightRequest.action2 = FightRequest.TOOTH_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.TOOTH.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.TURTLE ) )
				{
					FightRequest.action2 = FightRequest.TURTLE_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.TURTLE.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.SPICES ) )
				{
					FightRequest.action2 = FightRequest.SPICES_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.SPICES.getItemId() ) );
				}
			}
			else if ( itemCount >= 2 )
			{
				FightRequest.action2 = FightRequest.action1;
				this.addFormField( "whichitem2", String.valueOf( item1 ) );
			}

			return;
		}

		// If the player wants to use a skill, make sure he knows it
		String skillName =
			SkillDatabase.getSkillName( StringUtilities.parseInt( FightRequest.action1.substring( 5 ) ) );

		if ( KoLmafiaCLI.getCombatSkillName( skillName ) == null )
		{
			if ( this.isAcceptable( 0, 0 ) )
			{
				FightRequest.action1 = "attack";
				this.addFormField( "action", FightRequest.action1 );
				return;
			}

			FightRequest.action1 = "abort";
			return;
		}

		// You can only sniff if you are not on the trail
		if ( skillName.equals( "Transcendent Olfaction" ) )
		{
			if ( KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.ON_THE_TRAIL ) ) )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound();
				return;
			}
		}

		// Skills use MP. Make sure the character has enough
		if ( KoLCharacter.getCurrentMP() < FightRequest.getActionCost() && GenericRequest.passwordHash != null )
		{
			for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
			{
				if ( MPRestoreItemList.CONFIGURES[ i ].isCombatUsable() && KoLConstants.inventory.contains( MPRestoreItemList.CONFIGURES[ i ].getItem() ) )
				{
					FightRequest.action1 = String.valueOf( MPRestoreItemList.CONFIGURES[ i ].getItem().getItemId() );

					++FightRequest.preparatoryRounds;
					this.updateCurrentAction();
					return;
				}
			}

			FightRequest.action1 = "abort";
			return;
		}

		if ( skillName.equals( "CLEESH" ) )
		{
			if ( FightRequest.castCleesh )
			{
				FightRequest.action1 = "attack";
				this.addFormField( "action", FightRequest.action1 );
				return;
			}

			FightRequest.castCleesh = true;
		}

		if ( FightRequest.isInvalidThrustSmack( FightRequest.action1 ) )
		{
			FightRequest.action1 = "abort";
			return;
		}

		this.addFormField( "action", "skill" );
		this.addFormField( "whichskill", FightRequest.action1.substring( 5 ) );
	}

	public static final boolean isInvalidThrustSmack( final String action )
	{
		boolean isThrustSmack = action.equals( "skill thrust-smack" ) || action.equals( "skill lunging thrust-smack" ) ||
			action.equals( "1003" ) && action.equals( "1005" );

		if ( !isThrustSmack )
		{
			return false;
		}

		int weaponId = EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId();

		if ( EquipmentDatabase.getWeaponType( weaponId ) == KoLConstants.MOXIE )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Thrust smacks are useless with ranged weapons." );
			return true;
		}

		return false;
	}

	public void runOnce( final String desiredAction )
	{
		this.clearDataFields();

		FightRequest.action1 = null;
		FightRequest.action2 = null;
		FightRequest.isUsingConsultScript = false;

		if ( !KoLmafia.refusesContinue() )
		{
			if ( desiredAction == null )
			{
				this.nextRound();
			}
			else
			{
				this.nextRound( desiredAction );
			}
		}

		if ( !FightRequest.isUsingConsultScript )
		{
			if ( FightRequest.currentRound == 0 )
			{
				super.run();
			}
			else if ( FightRequest.action1 != null && !FightRequest.action1.equals( "abort" ) )
			{
				super.run();
			}
		}

		if ( FightRequest.action1 != null && FightRequest.action1.equals( "abort" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "You're on your own, partner." );
		}
	}

	public void run()
	{
		RequestThread.openRequestSequence();
		FightRequest.isAutomatingFight = true;

		do
		{
			this.runOnce( null );
		}
		while ( this.responseCode == 200 && FightRequest.currentRound != 0 && !KoLmafia.refusesContinue() );

		if ( this.responseCode == 302 )
		{
			FightRequest.clearInstanceData();
		}

		if ( KoLmafia.refusesContinue() && FightRequest.currentRound != 0 )
		{
			this.showInBrowser( true );
		}

		FightRequest.isAutomatingFight = false;
		RequestThread.closeRequestSequence();
	}

	public static final int getMonsterLevelModifier()
	{
		if ( FightRequest.monsterData == null )
		{
			return 0;
		}

		return FightRequest.levelModifier;
	}

	public static final int getMonsterHealth()
	{
		if ( FightRequest.monsterData == null )
		{
			return 0;
		}

		return FightRequest.monsterData.getAdjustedHP( KoLCharacter.getMonsterLevelAdjustment() ) - FightRequest.healthModifier;
	}

	public static final int getMonsterAttack()
	{
		if ( FightRequest.monsterData == null )
		{
			return 0;
		}

		return FightRequest.monsterData.getAttack() + FightRequest.levelModifier + KoLCharacter.getMonsterLevelAdjustment();
	}

	public static final int getMonsterDefense()
	{
		if ( FightRequest.monsterData == null )
		{
			return 0;
		}

		return FightRequest.monsterData.getDefense() + FightRequest.levelModifier + KoLCharacter.getMonsterLevelAdjustment();
	}

	public static final int getMonsterAttackElement()
	{
		if ( FightRequest.monsterData == null )
		{
			return MonsterDatabase.NONE;
		}

		return FightRequest.monsterData.getAttackElement();
	}

	public static final int getMonsterDefenseElement()
	{
		if ( FightRequest.monsterData == null )
		{
			return MonsterDatabase.NONE;
		}

		return FightRequest.monsterData.getDefenseElement();
	}

	public static final boolean willUsuallyMiss()
	{
		return FightRequest.willUsuallyMiss( 0 );
	}

	public static final boolean willUsuallyMiss( final int defenseModifier )
	{
		if ( FightRequest.monsterData == null )
		{
			return false;
		}

		return FightRequest.monsterData.willUsuallyMiss( FightRequest.levelModifier + defenseModifier );
	}

	public static final boolean willUsuallyDodge()
	{
		return FightRequest.willUsuallyDodge( 0 );
	}

	public static final boolean willUsuallyDodge( final int offenseModifier )
	{
		if ( FightRequest.monsterData == null )
		{
			return false;
		}

		return FightRequest.monsterData.willUsuallyDodge( FightRequest.levelModifier + offenseModifier );
	}

	private boolean isAcceptable( final int offenseModifier, final int defenseModifier )
	{
		if ( FightRequest.monsterData == null )
		{
			return true;
		}

		if ( FightRequest.willUsuallyMiss() || FightRequest.willUsuallyDodge() )
		{
			return false;
		}

		return KoLmafia.getRestoreCount() == 0;
	}

	private void handleAddingMachine()
	{
		if ( FightRequest.desiredScroll != null )
		{
			this.createAddingScroll( FightRequest.desiredScroll );
		}
		else if ( KoLConstants.conditions.contains( FightRequest.SCROLL_64735 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_64735 );
		}
		else if ( KoLConstants.conditions.contains( FightRequest.SCROLL_64067 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_64067 );
		}
		else if ( KoLConstants.conditions.contains( FightRequest.SCROLL_31337 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_31337 );
		}
		else if ( KoLConstants.conditions.contains( FightRequest.SCROLL_668 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_668 );
		}
		else if ( Preferences.getBoolean( "createHackerSummons" ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_31337 );
		}
		else
		{
			this.createAddingScroll( FightRequest.SCROLL_668 );
		}
	}

	private boolean createAddingScroll( final AdventureResult scroll )
	{
		AdventureResult part1 = null;
		AdventureResult part2 = null;

		if ( scroll == FightRequest.SCROLL_64735 )
		{
			part2 = FightRequest.SCROLL_64067;
			part1 = FightRequest.SCROLL_668;
		}
		else if ( scroll == FightRequest.SCROLL_64067 )
		{
			if ( !KoLConstants.conditions.contains( FightRequest.SCROLL_64067 ) && KoLConstants.inventory.contains( FightRequest.SCROLL_64067 ) )
			{
				return false;
			}

			part1 = FightRequest.SCROLL_30669;
			part2 = FightRequest.SCROLL_33398;
		}
		else if ( scroll == FightRequest.SCROLL_668 )
		{
			part1 = FightRequest.SCROLL_334;
			part2 = FightRequest.SCROLL_334;
		}
		else if ( scroll == FightRequest.SCROLL_31337 )
		{
			part1 = FightRequest.SCROLL_30669;
			part2 = FightRequest.SCROLL_668;
		}
		else
		{
			return false;
		}

		if ( FightRequest.desiredScroll != null )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.action1 = String.valueOf( part2.getItemId() );

			FightRequest.desiredScroll = null;
			return true;
		}

		if ( part1 == part2 && part1.getCount( KoLConstants.inventory ) < 2 )
		{
			return false;
		}

		if ( !KoLConstants.inventory.contains( part1 ) )
		{
			return this.createAddingScroll( part1 ) || this.createAddingScroll( part2 );
		}

		if ( !KoLConstants.inventory.contains( part2 ) )
		{
			return this.createAddingScroll( part2 );
		}

		if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.action1 = String.valueOf( part1.getItemId() );

			FightRequest.desiredScroll = scroll;
			return true;
		}

		++FightRequest.preparatoryRounds;
		FightRequest.action1 = part1.getItemId() + "," + part2.getItemId();
		return true;
	}

	private void handleHulkingConstruct()
	{
		if ( FightRequest.currentRound > 1 )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.action1 = "3155";
			return;
		}

		AdventureResult card1 = ItemPool.get( ItemPool.PUNCHCARD_ATTACK, 1 );
		AdventureResult card2 = ItemPool.get( ItemPool.PUNCHCARD_WALL, 1 );

		if ( !KoLConstants.inventory.contains( card1 ) ||
		     !KoLConstants.inventory.contains( card2 ) )
		{
			FightRequest.action1 = "runaway";
			return;
		}

		++FightRequest.preparatoryRounds;
		if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
		{
			FightRequest.action1 = "3146";
		}
		else
		{
			FightRequest.action1 = "3146,3155";
		}
	}

	private String getMonsterWeakenAction()
	{
		if ( this.isAcceptable( 0, 0 ) )
		{
			return "attack";
		}

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

	private static final void checkForInitiative( final String responseText )
	{
		if ( FightRequest.isAutomatingFight )
		{
			String action = Preferences.getString( "battleAction" );

			if ( action.startsWith( "custom" ) )
			{
				action = CustomCombatManager.getSettingsFileLocation();
			}

			RequestLogger.printLine( "Strategy: " + action );
		}

		if ( FightRequest.lastUserId != KoLCharacter.getUserId() )
		{
			FightRequest.lastUserId = KoLCharacter.getUserId();
			FightRequest.lostInitiative = "Round 0: " + KoLCharacter.getUserName() + " loses initiative!";
			FightRequest.wonInitiative = "Round 0: " + KoLCharacter.getUserName() + " wins initiative!";
		}

		boolean shouldLogAction = Preferences.getBoolean( "logBattleAction" );

		// The response tells you if you won initiative.

		if ( !FightRequest.wonInitiative( responseText ) )
		{
			// If you lose initiative, there's nothing very
			// interesting to print to the session log.

			if ( shouldLogAction )
			{
				RequestLogger.printLine( FightRequest.lostInitiative );
				RequestLogger.updateSessionLog( FightRequest.lostInitiative );
			}

			return;
		}

		// Now that you've won initiative, figure out what actually
		// happened in that first round based on player settings.

		if ( shouldLogAction )
		{
			RequestLogger.printLine( FightRequest.wonInitiative );
			RequestLogger.updateSessionLog( FightRequest.wonInitiative );
		}

		FightRequest.action1 = Preferences.getString( "defaultAutoAttack" );

		// If no default action is made by the player, then the round remains
		// the same.  Simply report winning/losing initiative.

		if ( FightRequest.action1.equals( "" ) || FightRequest.action1.equals( "0" ) )
		{
			return;
		}

		StringBuffer action = new StringBuffer();

		++FightRequest.currentRound;
		++FightRequest.preparatoryRounds;

		if ( shouldLogAction )
		{
			action.append( "Round 1: " + KoLCharacter.getUserName() + " " );
		}

		if ( FightRequest.action1.equals( "1" ) )
		{
			if ( shouldLogAction )
			{
				action.append( "attacks!" );
			}

			FightRequest.action1 = "attack";
		}
		else if ( FightRequest.action1.equals( "3" ) )
		{
			if ( shouldLogAction )
			{
				action.append( "tries to steal an item!" );
			}

			FightRequest.action1 = "steal";
		}
		else if ( shouldLogAction )
		{
			action.append( "casts " + SkillDatabase.getSkillName( Integer.parseInt( FightRequest.action1 ) ).toUpperCase() + "!" );
		}

		if ( shouldLogAction )
		{
			action.append( " (auto-attack)" );
			RequestLogger.printLine( action.toString() );
			RequestLogger.updateSessionLog( action.toString() );
		}
	}

	public static final void updateCombatData( final String encounter, final String responseText )
	{
		FightRequest.foundNextRound = true;
		FightRequest.lastResponseText = responseText;

		// Round tracker should include this data.

		FightRequest.parseBangPotion( responseText );
		FightRequest.parseStoneSphere( responseText );
		FightRequest.parsePirateInsult( responseText );

		// Spend MP and consume items

		++FightRequest.currentRound;

		if ( !KoLConstants.activeEffects.contains( FightRequest.CUNCTATITIS ) || responseText.indexOf( "You decide" ) == -1 )
		{
			FightRequest.payActionCost();
		}

		if ( FightRequest.currentRound == 1 )
		{
			// If this is the first round, then register the
			// opponent you are fighting against.

			FightRequest.encounterLookup = CustomCombatManager.encounterKey( encounter );
			FightRequest.monsterData = MonsterDatabase.findMonster( FightRequest.encounterLookup, false );

			FightRequest.isTrackingFights = false;
			FightRequest.checkForInitiative( responseText );
		}
		else
		{
			// Otherwise, the player can change the monster
			Matcher matcher = CLEESH_PATTERN.matcher( responseText );
			if ( matcher.find() )
			{
				FightRequest.encounterLookup = CustomCombatManager.encounterKey( matcher.group(1) );
				FightRequest.monsterData = MonsterDatabase.findMonster( FightRequest.encounterLookup, false );
				FightRequest.healthModifier = 0;
			}
		}

		switch ( KoLAdventure.lastAdventureId() )
		{
		case 182: // Barrel with Something Burning in it
		case 183: // Near an Abandoned Refrigerator
		case 184: // Over Where the Old Tires Are
		case 185: // Out by that Rusted-Out Car
			// Quest gremlins might have a tool.
			IslandDecorator.handleGremlin( responseText );
			break;

		case 132: // Battlefield (Frat Uniform)
		case 140: // Battlefield (Hippy Uniform)
			IslandDecorator.handleBattlefield( responseText );
			break;

		case 167: // Hobopolis Town Square
			HobopolisDecorator.handleTownSquare( responseText );
			break;
		}

		int blindIndex = responseText.indexOf( "... something.</div>" );

		// Log familiar actions, if the player wishes to include this
		// information in their session logs.

		if ( Preferences.getBoolean( "logFamiliarActions" ) )
		{
			Matcher familiarActMatcher = FightRequest.FAMILIAR_ACT_PATTERN.matcher( responseText );
			while ( familiarActMatcher.find() )
			{
				String action =
					"Round " + FightRequest.currentRound + ": " + KoLConstants.ANYTAG_PATTERN.matcher(
						familiarActMatcher.group() ).replaceAll( "" );
				RequestLogger.printLine( action );
				RequestLogger.updateSessionLog( action );
			}
		}

		// Check for antique breakage; only run the string search if
		// the player is equipped with the applicable item.

		if ( EquipmentManager.getEquipment( EquipmentManager.HAT ).equals( FightRequest.BROKEN_HELMET ) && responseText.indexOf( "Your antique helmet, weakened" ) != -1 )
		{
			EquipmentManager.setEquipment( EquipmentManager.HAT, EquipmentRequest.UNEQUIP );
			ResultProcessor.tallyResult( FightRequest.BROKEN_HELMET, true );
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Your antique helmet broke." );
		}

		if ( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).equals( FightRequest.BROKEN_SPEAR ) && responseText.indexOf( "sunders your antique spear" ) != -1 )
		{
			EquipmentManager.setEquipment( EquipmentManager.WEAPON, EquipmentRequest.UNEQUIP );
			ResultProcessor.tallyResult( FightRequest.BROKEN_SPEAR, true );
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Your antique spear broke." );
		}

		if ( EquipmentManager.getEquipment( EquipmentManager.OFFHAND ).equals( FightRequest.BROKEN_SHIELD ) && responseText.indexOf( "Your antique shield, weakened" ) != -1 )
		{
			EquipmentManager.setEquipment( EquipmentManager.OFFHAND, EquipmentRequest.UNEQUIP );
			ResultProcessor.tallyResult( FightRequest.BROKEN_SHIELD, true );
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Your antique shield broke." );
		}

		if ( EquipmentManager.getEquipment( EquipmentManager.PANTS ).equals( FightRequest.BROKEN_GREAVES ) && responseText.indexOf( "Your antique greaves, weakened" ) != -1 )
		{
			EquipmentManager.setEquipment( EquipmentManager.PANTS, EquipmentRequest.UNEQUIP );
			ResultProcessor.tallyResult( FightRequest.BROKEN_GREAVES, true );
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Your antique greaves broke." );
		}

		FightRequest.updateMonsterHealth( responseText );

		while ( blindIndex != -1 )
		{
			RequestLogger.printLine( "You acquire... something." );
			if ( Preferences.getBoolean( "logAcquiredItems" ) )
			{
				RequestLogger.updateSessionLog( "You acquire... something." );
			}

			blindIndex = responseText.indexOf( "... something.</div>", blindIndex + 1 );
		}

		// Reset round information if the battle is complete.
		// This is recognized when fight.php has no data.

		if ( Preferences.getBoolean( "serverAddsCustomCombat" ) )
		{
			if ( responseText.indexOf( "(show old combat form)" ) != -1 )
			{
				return;
			}
		}
		else
		{
			if ( responseText.indexOf( "fight.php" ) != -1 )
			{
				return;
			}
		}

		// Check for bounty item not dropping from a monster
		// that is known to drop the item.

		int bountyItemId = Preferences.getInteger( "currentBountyItem" );
		if ( monsterData != null && bountyItemId != 0 )
		{
			AdventureResult bountyItem = new AdventureResult( bountyItemId, 1 );
			String bountyItemName = bountyItem.getName();

			if ( monsterData.getItems().contains( bountyItem ) && responseText.indexOf( bountyItemName ) == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Bounty item failed to drop from expected monster." );
			}
		}

		FightRequest.clearInstanceData();
	}

	private static final Pattern BANG_POTION_PATTERN =
		Pattern.compile( "You throw the (.*?) potion at your opponent.?.  It shatters against .*?[,\\.] (.*?)\\." );

	private static final void parseBangPotion( final String responseText )
	{
		Matcher bangMatcher = FightRequest.BANG_POTION_PATTERN.matcher( responseText );
		while ( bangMatcher.find() )
		{
			int potionId = ItemDatabase.getItemId( bangMatcher.group( 1 ) + " potion" );

			String effectText = bangMatcher.group( 2 );
			String[][] strings = ItemPool.bangPotionStrings;

			for ( int i = 0; i < strings.length; ++i )
			{
				if ( effectText.indexOf( strings[i][1] ) != -1 )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
									    potionId,
									    819, 827,
									    "lastBangPotion" ) )
					{
						KoLmafia.updateDisplay( "All bang potions have been identified!" );
					}
					break;
				}
			}
		}
	}

	// You hold the rough stone sphere up in the air.
	private static final Pattern STONE_SPHERE_PATTERN =
		Pattern.compile( "You hold the (.*?) stone sphere up in the air.*?It radiates a (.*?)," );

	private static final void parseStoneSphere( final String responseText )
	{
		Matcher sphereMatcher = FightRequest.STONE_SPHERE_PATTERN.matcher( responseText );
		while ( sphereMatcher.find() )
		{
			int sphereId = ItemDatabase.getItemId( sphereMatcher.group( 1 ) + " stone sphere" );

			if ( sphereId == -1 )
			{
				continue;
			}

			String effectText = sphereMatcher.group( 2 );
			String[][] strings = ItemPool.stoneSphereStrings;

			for ( int i = 0; i < strings.length; ++i )
			{
				if ( effectText.indexOf( strings[i][1] ) != -1 )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
									    sphereId,
									    2174, 2177,
									    "lastStoneSphere" ) )
					{
						KoLmafia.updateDisplay( "All stone spheres have been identified!" );
					}
					break;
				}
			}
		}
	}

	public static final String stoneSphereEffectToId( final String effect )
	{
		for ( int i = 2174; i <= 2177; ++i )
		{
			String itemId = String.valueOf( i );
			String value = Preferences.getString( "lastStoneSphere" + itemId );

			if ( value.equals( "plants" ) )
			{
				value = "nature";
			}

			if ( effect.equals( value ) )
			{
				return itemId;
			}
		}

		return null;
	}

	// The pirate sneers at you and replies &quot;<insult>&quot;

	private static final Pattern PIRATE_INSULT_PATTERN =
		Pattern.compile( "The pirate sneers at you and replies &quot;(.*?)&quot;" );

	// The first string is an insult you hear from Rickets.
	// The second string is the insult you must use in reply.

	private static final String [][] PIRATE_INSULTS =
	{
		{
			"Arrr, the power of me serve'll flay the skin from yer bones!",
			"Obviously neither your tongue nor your wit is sharp enough for the job."
		},
		{
			"Do ye hear that, ye craven blackguard?  It be the sound of yer doom!",
			"It can't be any worse than the smell of your breath!"
		},
		{
			"Suck on <i>this</i>, ye miserable, pestilent wretch!",
			"That reminds me, tell your wife and sister I had a lovely time last night."
		},
		{
			"The streets will run red with yer blood when I'm through with ye!",
			"I'd've thought yellow would be more your color."
		},
		{
			"Yer face is as foul as that of a drowned goat!",
			"I'm not really comfortable being compared to your girlfriend that way."
		},
		{
			"When I'm through with ye, ye'll be crying like a little girl!",
			"It's an honor to learn from such an expert in the field."
		},
		{
			"In all my years I've not seen a more loathsome worm than yerself!",
			"Amazing!  How do you manage to shave without using a mirror?"
		},
		{
			"Not a single man has faced me and lived to tell the tale!",
			"It only seems that way because you haven't learned to count to one."
		},
	};

	private static final void parsePirateInsult( final String responseText )
	{
		Matcher insultMatcher = FightRequest.PIRATE_INSULT_PATTERN.matcher( responseText );
		if ( insultMatcher.find() )
		{
			int insult = FightRequest.findPirateInsult( insultMatcher.group( 1 ) );
			if ( insult > 0 )
			{
				KoLCharacter.ensureUpdatedPirateInsults();
				Preferences.setBoolean( "lastPirateInsult" + insult, true );
			}
		}
	}

	private static final int findPirateInsult( final String insult )
	{
		for ( int i = 0; i < PIRATE_INSULTS.length; ++i )
		{
			if ( insult.equals( PIRATE_INSULTS[i][1] ) )
			{
				return i + 1;
			}
		}
		return 0;
	}

	public static final int findPirateRetort( final String insult )
	{
		for ( int i = 0; i < PIRATE_INSULTS.length; ++i )
		{
			if ( insult.equals( PIRATE_INSULTS[i][0] ) )
			{
				return i + 1;
			}
		}
		return 0;
	}

	public static final String findPirateRetort( final int insult )
	{
		KoLCharacter.ensureUpdatedPirateInsults();
		if ( Preferences.getBoolean( "lastPirateInsult" + insult ) )
		{
			return PIRATE_INSULTS[insult - 1][1];
		}
		return null;
	}

	public static final float pirateInsultOdds()
	{
		KoLCharacter.ensureUpdatedPirateInsults();

		int count = 0;
		for ( int i = 1; i <= 8; ++i )
		{
			if ( Preferences.getBoolean( "lastPirateInsult" + i ) )
			{
				count += 1;
			}
		}

		return FightRequest.pirateInsultOdds( count );
	}

	public static final float pirateInsultOdds( int count )
	{
		// If you know less than three insults, you can't possibly win.
		if ( count < 3 )
		{
			return 0.0f;
		}

		// Otherwise, your probability of winning is:
		//   ( count ) / 8	the first contest
		//   ( count - 1 ) / 8	the second contest
		//   ( count - 2 ) / 6	the third contest

		float odds = 1.0f;

		odds *= ( count * 1.0f ) / 8;
		odds *= ( count * 1.0f - 1 ) / 7;
		odds *= ( count * 1.0f - 2 ) / 6;

		return odds;
	}

	private static final void updateMonsterHealth( final String responseText )
	{
		// Check if fumbled first, since that causes a special case later.

		boolean fumbled = FightRequest.FUMBLE_PATTERN.matcher( responseText ).find();

		// Monster damage is verbose, so accumulate in a single variable
		// for the entire results and just show the total.

		int damageThisRound = 0;

		Matcher damageMatcher = FightRequest.ELEMENTAL_PATTERN.matcher( responseText );
		while ( damageMatcher.find() )
		{
			damageThisRound += StringUtilities.parseInt( damageMatcher.group( 1 ) );
		}

		damageMatcher = FightRequest.PHYSICAL_PATTERN.matcher( responseText );

		for ( int i = 0; damageMatcher.find(); ++i )
		{
			// In a fumble, the first set of text indicates that there is
			// no actual damage done to the monster.

			if ( i == 0 && fumbled )
			{
				continue;
			}

			// Currently, all of the explicit attack messages that preceed
			// the number all imply that this is not damage against the
			// monster or is damage that should not count (reap/sow X damage.)

			if ( !damageMatcher.group( 1 ).equals( "" ) )
			{
				continue;
			}

			// "shambles up to your opponent" following a number is most
			// likely a familiar naming problem, so it should not count.

			if ( damageMatcher.group( 4 ).equals( "shambles up " ) )
			{
				continue;
			}

			damageThisRound += StringUtilities.parseInt( damageMatcher.group( 2 ) );

			// The last string contains all of the extra damage
			// from dual-wielding or elemental damage, e.g. "(+3) (+10)".

			Matcher secondaryMatcher = FightRequest.SECONDARY_PATTERN.matcher( damageMatcher.group( 3 ) );
			while ( secondaryMatcher.find() )
			{
				damageThisRound += StringUtilities.parseInt( secondaryMatcher.group( 1 ) );
			}
		}

		// Mosquito and Boss Bat can muck with the monster's HP, but
		// they don't have normal text.

		if ( KoLCharacter.getFamiliar().getRace().equals( "Mosquito" ) )
		{
			damageMatcher = FightRequest.MOSQUITO_PATTERN.matcher( responseText );
			if ( damageMatcher.find() )
			{
				damageThisRound += StringUtilities.parseInt( damageMatcher.group( 1 ) );
			}
		}

		damageMatcher = FightRequest.BOSSBAT_PATTERN.matcher( responseText );
		if ( damageMatcher.find() )
		{
			damageThisRound += StringUtilities.parseInt( damageMatcher.group( 1 ) );
		}

		// Done with all processing for monster damage, now handle responseText.

		FightRequest.healthModifier += damageThisRound;
		if ( !Preferences.getBoolean( "logMonsterHealth" ) )
		{
			return;
		}

		StringBuffer action = new StringBuffer();

		if ( damageThisRound != 0 )
		{
			action.append( "Round " );
			action.append( FightRequest.currentRound - 1 );
			action.append( ": " );
			action.append( FightRequest.encounterLookup );

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

		if ( FightRequest.GHUOL_HEAL.matcher( responseText ).find() || FightRequest.NS_HEAL.matcher( responseText ).find() )
		{
			action.setLength( 0 );
			action.append( "Round " );
			action.append( FightRequest.currentRound - 1 );
			action.append( ": " );
			action.append( FightRequest.encounterLookup );

			action.append( " heals an unspaded amount of hit points." );

			RequestLogger.printLine( action.toString() );
			RequestLogger.updateSessionLog( action.toString() );
		}
	}

	private static final void clearInstanceData()
	{
		IslandDecorator.startFight();
		FightRequest.castCleesh = false;
		FightRequest.jiggledChefstaff = false;
		FightRequest.desiredScroll = null;

		FightRequest.levelModifier = 0;
		FightRequest.healthModifier = 0;

		FightRequest.action1 = null;
		FightRequest.action2 = null;

		FightRequest.currentRound = 0;
		FightRequest.preparatoryRounds = 0;
	}

	private static final int getActionCost()
	{
		if ( FightRequest.action1.equals( "attack" ) )
		{
			return 0;
		}

		if ( FightRequest.action1.startsWith( "item" ) )
		{
			return 0;
		}

		return SkillDatabase.getMPConsumptionById( StringUtilities.parseInt( FightRequest.action1 ) );
	}

	public static void addItemActionsWithNoCost()
	{
		KoLCharacter.battleSkillNames.add( "item seal tooth" );
		KoLCharacter.battleSkillNames.add( "item turtle totem" );
		KoLCharacter.battleSkillNames.add( "item spices" );

		KoLCharacter.battleSkillNames.add( "item dictionary" );
		KoLCharacter.battleSkillNames.add( "item jam band flyers" );
		KoLCharacter.battleSkillNames.add( "item rock band flyers" );

		KoLCharacter.battleSkillNames.add( "item toy soldier" );
		KoLCharacter.battleSkillNames.add( "item toy mercenary" );

		KoLCharacter.battleSkillNames.add( "item Miniborg stomper" );
		KoLCharacter.battleSkillNames.add( "item Miniborg laser" );
		KoLCharacter.battleSkillNames.add( "item Miniborg Destroy-O-Bot" );

		KoLCharacter.battleSkillNames.add( "item naughty paper shuriken" );
	}

	private static final boolean isItemConsumed( final int itemId )
	{
		switch ( itemId )
		{
		case ItemPool.SEAL_TOOTH:
		case ItemPool.TURTLE_TOTEM:
		case ItemPool.SPICES:
		case ItemPool.DICTIONARY:
		case ItemPool.FACSIMILE_DICTIONARY:
		case ItemPool.TOY_SOLDIER:
		case ItemPool.TOY_MERCENARY:
		case ItemPool.MOSSY_STONE_SPHERE:
		case ItemPool.SMOOTH_STONE_SPHERE:
		case ItemPool.CRACKED_STONE_SPHERE:
		case ItemPool.ROUGH_STONE_SPHERE:
		case ItemPool.JAM_BAND_FLYERS:
		case ItemPool.ROCK_BAND_FLYERS:
		case ItemPool.MOLYBDENUM_MAGNET:
		case ItemPool.SPECTRE_SCEPTER:
		case ItemPool.GNOME_DEMODULIZER:
		case ItemPool.PIRATE_INSULT_BOOK:
		case ItemPool.MINIBORG_STOMPER:
		case ItemPool.MINIBORG_STRANGLER:
		case ItemPool.MINIBORG_LASER:
		case ItemPool.MINIBORG_BEEPER:
		case ItemPool.MINIBORG_HIVEMINDER:
		case ItemPool.MINIBORG_DESTROYOBOT:
		case ItemPool.PAPER_SHURIKEN:
		case ItemPool.EMPTY_EYE:
		case ItemPool.ICEBALL:
			return false;

		case ItemPool.ANTIDOTE: // Anti-Anti-Antidote

			for ( int i = 0; i < KoLConstants.activeEffects.size(); ++i )
			{
				if ( ( (AdventureResult) KoLConstants.activeEffects.get( i ) ).getName().indexOf( "Poison" ) != -1 )
				{
					return true;
				}
			}

			return false;

		default:

			return true;
		}
	}

	public static final void payActionCost()
	{
		if ( FightRequest.action1 == null || FightRequest.action1.equals( "" ) )
		{
			return;
		}

		if ( FightRequest.action1.equals( "attack" ) || FightRequest.action1.equals( "runaway" ) || FightRequest.action1.equals( "steal" ) )
		{
			return;
		}

		if ( FightRequest.action1.equals( "jiggle" ) )
		{
			FightRequest.jiggledChefstaff = true;
			return;
		}

		if ( !FightRequest.action1.startsWith( "skill" ) )
		{
			if ( FightRequest.currentRound == 0 )
			{
				return;
			}

			FightRequest.payItemCost( StringUtilities.parseInt( FightRequest.action1 ) );

			if ( FightRequest.action2 == null || FightRequest.action2.equals( "" ) )
			{
				return;
			}

			FightRequest.payItemCost( StringUtilities.parseInt( FightRequest.action2 ) );

			return;
		}

		int skillId = StringUtilities.parseInt( FightRequest.action1.substring( 5 ) );
		int mpCost = SkillDatabase.getMPConsumptionById( skillId );

		switch ( skillId )
		{
		case 2005: // Shieldbutt
		case 2105: // Head + Shield Combo
		case 2106: // Knee + Shield Combo
		case 2107: // Head + Knee + Shield Combo
			FightRequest.levelModifier -= 5;
			break;

		case 5003: // Disco Eye-Poke
			FightRequest.levelModifier -= 1;
			break;

		case 5005: // Disco Dance of Doom
			FightRequest.levelModifier -= 3;
			break;

		case 5008: // Disco Dance II: Electric Boogaloo
			FightRequest.levelModifier -= 5;
			break;

		case 5012: // Disco Face Stab
			FightRequest.levelModifier -= 7;
			break;

		case 5019: // Tango of Terror
			FightRequest.levelModifier -= 6;
		}

		if ( mpCost > 0 )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MP, 0 - mpCost ) );
		}
	}

	public static final void payItemCost( final int itemId )
	{
		if ( FightRequest.isItemConsumed( itemId ) )
		{
			ResultProcessor.processResult( new AdventureResult( itemId, -1 ) );
			return;
		}

		switch ( itemId )
		{
		case ItemPool.TOY_SOLDIER:
			// A toy soldier consumes tequila.

			if ( KoLConstants.inventory.contains( FightRequest.TEQUILA ) )
			{
				ResultProcessor.processResult( FightRequest.TEQUILA );
			}
			break;

		case ItemPool.TOY_MERCENARY:
			// A toy mercenary consumes 5-10 meat

			// A sidepane refresh at the end of the battle will
			// re-synch everything.
			break;
		}
	}

	public int getAdventuresUsed()
	{
		return 0;
	}

	public static final String getNextTrackedRound()
	{
		while ( FightRequest.isTrackingFights && !FightRequest.foundNextRound && !KoLmafia.refusesContinue() )
		{
			PAUSER.pause( 200 );
		}

		if ( !FightRequest.foundNextRound || KoLmafia.refusesContinue() )
		{
			FightRequest.isTrackingFights = false;
		}
		else if ( FightRequest.isTrackingFights )
		{
			FightRequest.isTrackingFights = FightRequest.currentRound != 0;
		}

		FightRequest.foundNextRound = false;
		return RequestEditorKit.getFeatureRichHTML(
			FightRequest.isTrackingFights ? "fight.php?action=script" : "fight.php", FightRequest.lastResponseText,
			true );
	}

	public static final int getCurrentRound()
	{
		return FightRequest.currentRound;
	}

	public static final boolean alreadyJiggled()
	{
		return FightRequest.jiggledChefstaff;
	}

	public static final void beginTrackingFights()
	{
		FightRequest.isTrackingFights = true;
		FightRequest.foundNextRound = false;
	}

	public static final void stopTrackingFights()
	{
		FightRequest.isTrackingFights = false;
		FightRequest.foundNextRound = false;
	}

	public static final boolean isTrackingFights()
	{
		return FightRequest.isTrackingFights;
	}

	public static final String getLastMonsterName()
	{
		return FightRequest.encounterLookup;
	}

	public static final Monster getLastMonster()
	{
		return FightRequest.monsterData;
	}

	public static final boolean registerRequest( final boolean isExternal, final String urlString )
	{
		if ( !urlString.startsWith( "fight.php" ) )
		{
			return false;
		}

		FightRequest.action1 = null;
		FightRequest.action2 = null;

		if ( urlString.equals( "fight.php" ) )
		{
			return true;
		}

		boolean shouldLogAction = Preferences.getBoolean( "logBattleAction" );
		StringBuffer action = new StringBuffer();

		// Begin logging all the different combat actions and storing
		// relevant data for post-processing.

		if ( shouldLogAction )
		{
			action.append( "Round " + FightRequest.currentRound + ": " + KoLCharacter.getUserName() + " " );
		}

		if ( urlString.indexOf( "runaway" ) != -1 )
		{
			FightRequest.action1 = "runaway";
			if ( shouldLogAction )
			{
				action.append( "casts RETURN!" );
			}
		}
		else if ( urlString.indexOf( "steal" ) != -1 )
		{
			FightRequest.action1 = "steal";
			if ( shouldLogAction )
			{
				action.append( "tries to steal an item!" );
			}
		}
		else if ( urlString.indexOf( "attack" ) != -1 )
		{
			FightRequest.action1 = "attack";
			if ( shouldLogAction )
			{
				action.append( "attacks!" );
			}
		}
		else if ( urlString.indexOf( "chefstaff" ) != -1 )
		{
			FightRequest.action1 = "jiggle";
			if ( shouldLogAction )
			{
				action.append( "jiggles the " );
				action.append( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getName() );
			}
		}
		else
		{
			Matcher skillMatcher = FightRequest.SKILL_PATTERN.matcher( urlString );
			if ( skillMatcher.find() )
			{
				if ( FightRequest.isInvalidThrustSmack( skillMatcher.group( 1 ) ) )
				{
					return true;
				}

				String skill = SkillDatabase.getSkillName( StringUtilities.parseInt( skillMatcher.group( 1 ) ) );
				if ( skill == null )
				{
					if ( shouldLogAction )
					{
						action.append( "casts CHANCE!" );
					}
				}
				else
				{
					if ( skill.equalsIgnoreCase( "Transcendent Olfaction" ) )
					{
						Preferences.setString( "olfactedMonster",
							FightRequest.encounterLookup );
					}
					FightRequest.action1 = CustomCombatManager.getShortCombatOptionName( "skill " + skill );
					if ( shouldLogAction )
					{
						action.append( "casts " + skill.toUpperCase() + "!" );
					}
				}
			}
			else
			{
				Matcher itemMatcher = FightRequest.ITEM1_PATTERN.matcher( urlString );
				if ( itemMatcher.find() )
				{
					String item = ItemDatabase.getItemName( StringUtilities.parseInt( itemMatcher.group( 1 ) ) );
					if ( item == null )
					{
						if ( shouldLogAction )
						{
							action.append( "plays Garin's Harp" );
						}
					}
					else
					{
						if ( item.equalsIgnoreCase( "odor extractor" ) )
						{
							Preferences.setString( "olfactedMonster",
								FightRequest.encounterLookup );
						}
						FightRequest.action1 = CustomCombatManager.getShortCombatOptionName( item );
						if ( shouldLogAction )
						{
							action.append( "uses the " + item );
						}
					}

					itemMatcher = FightRequest.ITEM2_PATTERN.matcher( urlString );
					if ( itemMatcher.find() )
					{
						item = ItemDatabase.getItemName( StringUtilities.parseInt( itemMatcher.group( 1 ) ) );
						if ( item != null )
						{
							if ( item.equalsIgnoreCase( "odor extractor" ) )
							{
								Preferences.setString( "olfactedMonster",
									FightRequest.encounterLookup );
							}
							FightRequest.action2 = CustomCombatManager.getShortCombatOptionName( item );
							if ( shouldLogAction )
							{
								action.append( " and uses the " + item );
							}
						}
					}

					if ( shouldLogAction )
					{
						action.append( "!" );
					}
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
