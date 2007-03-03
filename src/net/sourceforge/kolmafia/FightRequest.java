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

import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.MonsterDatabase.Monster;

public class FightRequest extends KoLRequest
{
	private static final AdventureResult ANTIDOTE = new AdventureResult( 829, 1 );
	private static final AdventureResult POISON = new AdventureResult( "Poisoned", 1, true );

	private static final AdventureResult SOLDIER = new AdventureResult( 1397, 1 );
	private static final AdventureResult MERCENARY = new AdventureResult( 2139, 1 );
	private static final AdventureResult TEQUILA = new AdventureResult( 1004, -1 );

	private static String lastResult = "";
	private static String lastPlayer = "";

	private static String lostInitiative = "";
	private static String wonInitiative = "";

	private static boolean isTrackingFights = false;
	private static ArrayList trackedRounds = new ArrayList();

	private static boolean isUsingConsultScript = false;
	public static final FightRequest INSTANCE = new FightRequest();

	private static final Pattern SKILL_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern ITEM1_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern ITEM2_PATTERN = Pattern.compile( "whichitem2=(\\d+)" );

	private static final Pattern FAMILIAR_ACT_PATTERN = Pattern.compile( "<table><tr><td align=center.*?</table>", Pattern.DOTALL );

	public static final AdventureResult DICTIONARY1 = new AdventureResult( 536, 1 );
	public static final AdventureResult DICTIONARY2 = new AdventureResult( 1316, 1 );

	private static final AdventureResult TOOTH = new AdventureResult( 2, 1 );
	private static final AdventureResult TURTLE = new AdventureResult( 4, 1 );
	private static final AdventureResult SPICES = new AdventureResult( 8, 1 );

	private static final AdventureResult BROKEN_GREAVES = new AdventureResult( 1929, -1 );
	private static final AdventureResult BROKEN_HELMET = new AdventureResult( 1930, -1 );
	private static final AdventureResult BROKEN_SPEAR = new AdventureResult( 1931, -1 );
	private static final AdventureResult BROKEN_SHIELD = new AdventureResult( 1932, -1 );

	private static final String TOOTH_ACTION = "item" + TOOTH.getItemId();
	private static final String TURTLE_ACTION = "item" + TURTLE.getItemId();
	private static final String SPICES_ACTION = "item" + SPICES.getItemId();
	private static final String MERCENARY_ACTION = "item" + MERCENARY.getItemId();

	private static int currentRound = 0;
	private static int offenseModifier = 0, defenseModifier = 0;

	private static String action1 = null;
	private static String action2 = null;
	private static Monster monsterData = null;
	private static String encounterLookup = "";

	// Ultra-rare monsters
	private static final String [] RARE_MONSTERS = { "baiowulf", "crazy bastard", "hockey elemental", "hypnotist of hey deze", "infinite meat bug", "master of thieves", "temporal bandit" };

	/**
	 * Constructs a new <code>FightRequest</code>.  Theprovided will
	 * be used to determine whether or not the fight should be started and/or
	 * continued, and the user settings will be used to determine the kind
	 * of action1 to be taken during the battle.
	 */

	private FightRequest()
	{	super( "fight.php" );
	}

	private static boolean isLikelyStasisAction( String action )
	{
		if ( action.indexOf( "consult" ) != -1 || action.equals( "7002" ) )
			return true;

		if ( !action.startsWith( "item" ) )
			return false;

		int itemId = StaticEntity.parseInt( action1.substring( 4 ) );

		return itemId == DICTIONARY1.getItemId() || itemId == DICTIONARY2.getItemId() ||
			itemId == TOOTH.getItemId() || itemId == TURTLE.getItemId() || itemId == SPICES.getItemId();
	}

	public void nextRound()
	{
		// When logging in and encountering a fight, always use the
		// attack command to avoid abort problems.

		if ( LoginRequest.isInstanceRunning() )
		{
			action1 = "attack";
			addFormField( "action", "attack" );
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

		action1 = CombatSettings.getShortCombatOptionName( StaticEntity.getProperty( "battleAction" ) );
		action2 = null;

		// Always let the user see rare monsters

		for ( int i = 0; i < RARE_MONSTERS.length; ++i )
		{
			if ( encounterLookup.indexOf( RARE_MONSTERS[i] ) != -1 )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "You have encountered the " + encounter );
				action1 = "abort";
				return;
			}
		}

		// If the user wants a custom combat script, parse the desired
		// action here.

		if ( action1.equals( "custom" ) )
			action1 = CombatSettings.getSetting( encounterLookup, currentRound - 1 );

		// Automatically pickpocket when appropriate, if the
		// player trusts KoLmafia knows the right thing to do.

		if ( currentRound == 1 && responseText != null && responseText.indexOf( "You get the jump" ) != -1 && monsterData != null && monsterData.shouldSteal() )
		{
			action1 = "steal";
			addFormField( "action", action1 );
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
				interpreter.execute( "main", new String [] { String.valueOf( currentRound ), encounterLookup, responseText } );
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
			action1 = getMonsterWeakenAction();

		action1 = CombatSettings.getShortCombatOptionName( action1 );

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
			addFormField( "action", action1 );
			return;
		}

		// User wants a regular attack
		if ( action1.startsWith( "attack" ) )
		{
			action1 = "attack";
			addFormField( "action", action1 );
			return;
		}

		if ( action1.startsWith( "twiddle" ) )
		{
			action1 = null;
			return;
		}

		// If the player wants to use an item, make sure he has one
		if ( action1.startsWith( "item" ) )
		{
			int itemId = StaticEntity.parseInt( action1.substring( 4 ) );
			int itemCount = (new AdventureResult( itemId, 1 )).getCount( inventory );

			if ( (itemId == DICTIONARY1.getItemId() || itemId == DICTIONARY2.getItemId()) && itemCount < 1 )
			{
				KoLmafia.updateDisplay( ABORT_STATE, "Sorry, you don't have a dictionary." );
				action1 = "abort";
				return;
			}


			if ( itemCount == 0 )
			{
				action1 = "attack";
				addFormField( "action", action1 );
				return;
			}

			addFormField( "action", "useitem" );
			addFormField( "whichitem", String.valueOf( itemId ) );

			if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) && !KoLCharacter.getFamiliar().isThiefFamiliar() )
			{
				if ( itemId == ANTIDOTE.getItemId() )
				{
				}
				else if ( itemCount >= 2 && itemId != DICTIONARY1.getItemId() && itemId != DICTIONARY2.getItemId() )
				{
					action2 = action1;
					addFormField( "whichitem2", String.valueOf( itemId ) );
				}
				else if ( MERCENARY.getCount( inventory ) > (action1.equals( MERCENARY_ACTION ) ? 1 : 0) )
				{
					action2 = MERCENARY_ACTION;
					addFormField( "whichitem2", String.valueOf( MERCENARY.getItemId() ) );
				}
				else if ( TOOTH.getCount( inventory ) > (action1.equals( TOOTH_ACTION ) ? 1 : 0) )
				{
					action2 = TOOTH_ACTION;
					addFormField( "whichitem2", String.valueOf( TOOTH.getItemId() ) );
				}
				else if ( TURTLE.getCount( inventory ) > (action1.equals( TURTLE_ACTION ) ? 1 : 0) )
				{
					action2 = TURTLE_ACTION;
					addFormField( "whichitem2", String.valueOf( TURTLE.getItemId() ) );
				}
				else if ( SPICES.getCount( inventory ) > (action1.equals( SPICES_ACTION ) ? 1 : 0) )
				{
					action2 = SPICES_ACTION;
					addFormField( "whichitem2", String.valueOf( SPICES.getItemId() ) );
				}
			}

			return;
		}

		// Skills use MP. Make sure the character has enough
		if ( KoLCharacter.getCurrentMP() < getActionCost() && passwordHash != null )
		{
			action1 = "attack";
			addFormField( "action", action1 );
			return;
		}

		// If the player wants to use a skill, make sure he knows it
		String skillName = ClassSkillsDatabase.getSkillName( StaticEntity.parseInt( action1 ) );

		if ( KoLmafiaCLI.getCombatSkillName( skillName ) == null )
		{
			action1 = "attack";
			addFormField( "action", action1 );
			return;
		}

		if ( skillName.equals( "CLEESH" ) && currentRound != 0 )
		{
			action1 = "attack";
			addFormField( "action", action1 );
			return;
		}

		addFormField( "action", "skill" );
		addFormField( "whichskill", action1 );
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
		do
		{
			clearDataFields();

			action1 = null;
			action2 = null;
			isUsingConsultScript = false;

			nextRound();

			if ( !isUsingConsultScript )
			{
				if ( currentRound == 0 || (action1 != null && !action1.equals( "abort" )) )
					super.run();
			}

			if ( action1 != null && action1.equals( "abort" ) )
				KoLmafia.updateDisplay( ABORT_STATE, "You're on your own, partner." );
		}
		while ( currentRound != 0 && !KoLmafia.refusesContinue() );

		if ( currentRound != 0 )
			showInBrowser( true );
	}

	private boolean isAcceptable( int offenseModifier, int defenseModifier )
	{
		if ( monsterData == null )
			return true;

		return monsterData.hasAcceptableDodgeRate( FightRequest.offenseModifier + offenseModifier ) &&
			!monsterData.willAlwaysMiss( FightRequest.defenseModifier + defenseModifier );
	}

	private String getMonsterWeakenAction()
	{
		if ( isAcceptable( 0, 0 ) )
			return "attack";

		int desiredSkill = 0;
		boolean isAcceptable = false;

		// Disco Eye-Poke
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Eye-Poke" ) )
		{
			desiredSkill = 5003;
			isAcceptable = isAcceptable( -1, -1 );
		}

		// Disco Dance of Doom
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Dance of Doom" ) )
		{
			desiredSkill = 5005;
			isAcceptable = isAcceptable( -3, -3 );
		}

		// Disco Dance II: Electric Boogaloo
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Dance II: Electric Boogaloo" ) )
		{
			desiredSkill = 5008;
			isAcceptable = isAcceptable( -5, -5 );
		}

		// Tango of Terror
		if ( !isAcceptable && KoLCharacter.hasSkill( "Tango of Terror" ) )
		{
			desiredSkill = 5019;
			isAcceptable = isAcceptable( -6, -6 );
		}

		// Disco Face Stab
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Face Stab" ) )
		{
			desiredSkill = 5012;
			isAcceptable = isAcceptable( -7, -7 );
		}

		return desiredSkill == 0 ? "attack" : String.valueOf( desiredSkill );
	}

	private static void checkForInitiative( String responseText )
	{
		if ( !KoLCharacter.getUserName().equals( lastPlayer ) )
		{
			lastPlayer = KoLCharacter.getUserName();
			lostInitiative = "Round 0: " + lastPlayer + " loses initiative!";
			wonInitiative = "Round 0: " + lastPlayer + " wins initiative!";
		}

		boolean shouldLogAction = StaticEntity.getBooleanProperty( "logBattleAction" );

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

		action1 = StaticEntity.getProperty( "defaultAutoAttack" );

		// If no default action is made by the player, then the round remains
		// the same.  Simply report winning/losing initiative.

		if ( action1.equals( "" ) || action1.equals( "0" ) )
			return;

		StringBuffer action = new StringBuffer();

		++currentRound;

		if ( shouldLogAction )
			action.append( "Round 1: " + KoLCharacter.getUserName() + " " );

		if ( action1.equals( "1" ) )
		{
			if ( shouldLogAction )
				action.append( "attacks!" );

			action1 = "attack";
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

	public static void updateCombatData( String encounter, String responseText )
	{
		INSTANCE.responseText = responseText;

		// Round tracker should include this data.

		if ( isTrackingFights )
			trackedRounds.add( responseText );

		// Spend MP and consume items

		++currentRound;
		payActionCost();

		if ( currentRound == 1 )
			checkForInitiative( responseText );

		// Log familiar actions, if the player wishes to include this
		// information in their session logs.

		if ( StaticEntity.getBooleanProperty( "logFamiliarActions" ) )
		{
			Matcher familiarActMatcher = FAMILIAR_ACT_PATTERN.matcher( responseText );
			while ( familiarActMatcher.find() )
			{
				StringTokenizer actions = new StringTokenizer( ANYTAG_PATTERN.matcher( familiarActMatcher.group() ).replaceAll( "\n" ), "\n" );
				while ( actions.hasMoreTokens() )
				{
					String action = "Round " + currentRound + ": " + actions.nextToken();
					RequestLogger.printLine( action );
					RequestLogger.updateSessionLog( action );
				}
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

		// Reset round information if the battle is complete.
		// This is recognized when fight.php has no data.

		if ( responseText.indexOf( "fight.php" ) == -1 )
		{
			clearInstanceData();
			return;
		}

		// If this is the first round, then register the opponent
		// you are fighting against.

		if ( currentRound == 1 )
		{
			encounterLookup = CombatSettings.encounterKey( encounter );
			monsterData = MonsterDatabase.findMonster( encounter );
		}
	}

	private static void clearInstanceData()
	{
		encounterLookup = "";
		monsterData = null;

		currentRound = 0;
		offenseModifier = 0;
		defenseModifier = 0;

		action1 = null;
		action2 = null;
	}

	private static int getActionCost()
	{
		if ( action1.equals( "attack" ) )
			return 0;

		if ( action1.startsWith( "item" ) )
			return 0;

		return ClassSkillsDatabase.getMPConsumptionById( StaticEntity.parseInt( action1 ) );
	}

	private static boolean hasActionCost( int itemId )
	{
		switch ( itemId )
		{
		case 2:		// Seal Tooth
		case 4:		// Scroll of Turtle Summoning
		case 8:		// Spices
		case 536:	// Dictionary 1
		case 1316:	// Dictionary 2

			return false;

		case 829:  // Anti-Anti-Antidote

			return activeEffects.contains( POISON );

		default:

			return true;
		}
	}

	public static void payActionCost()
	{
		if ( action1 == null || action1.equals( "" ) )
			return;

		if ( action1.equals( "attack" ) || action1.equals( "runaway" ) || action1.equals( "steal" ) )
			return;

		if ( action1.startsWith( "item" ) )
		{
			if ( currentRound == 0 )
				return;

			int id1 = StaticEntity.parseInt( action1.substring( 4 ) );

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

			int id2 = StaticEntity.parseInt( action2.substring( 4 ) );

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

		int skillId = StaticEntity.parseInt( action1 );
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

	/**
	 * An alternative method to doing adventure calculation is determining
	 * how many adventures are used by the given request, and subtract
	 * them after the request is done.  This number defaults to <code>zero</code>;
	 * overriding classes should change this value to the appropriate
	 * amount.
	 *
	 * @return	The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{	return responseText == null || responseText.equals( "" ) || responseText.indexOf( "fight.php" ) != -1 ? 0 : 1;
	}

	public static String getNextTrackedRound()
	{
		if ( !isTrackingFights )
			return lastResult;

		for ( int i = 0; trackedRounds.isEmpty() && i < 50; ++i )
			delay( 200 );

		if ( trackedRounds.isEmpty() )
		{
			isTrackingFights = false;
			return lastResult;
		}

		lastResult = (String) trackedRounds.remove(0);

		if ( trackedRounds.isEmpty() && currentRound == 0 )
		{
			isTrackingFights = false;

			StringBuffer resultBuffer = new StringBuffer();
			resultBuffer.append( lastResult );

			try
			{
				RequestEditorKit.getFeatureRichHTML( "fight.php?action=script", resultBuffer, true );
				lastResult = resultBuffer.toString();
			}
			catch ( Exception e )
			{
				StaticEntity.printStackTrace( e );
			}
		}

		return lastResult;
	}

	public static int getCurrentRound()
	{	return currentRound;
	}

	public static void beginTrackingFights()
	{	isTrackingFights = true;
	}

	public static boolean isTrackingFights()
	{	return isTrackingFights;
	}

	public static String getLastMonster()
	{	return encounterLookup == null ? "" : encounterLookup;
	}

	public static boolean registerRequest( String urlString )
	{
		if ( urlString.indexOf( "fight.php" ) == -1 )
			return false;

		action1 = null;
		action2 = null;

		boolean shouldLogAction = StaticEntity.getBooleanProperty( "logBattleAction" );
		StringBuffer action = shouldLogAction ? new StringBuffer() : null;

		if ( urlString.indexOf( "fight.php?" ) == -1 )
		{
			if ( currentRound != 0 )
			{
				if ( shouldLogAction )
				{
					action.append( "Round " + currentRound + ": " + KoLCharacter.getUserName() + " twiddles their thumbs" );
					RequestLogger.printLine( action.toString() );
					RequestLogger.updateSessionLog( action.toString() );
				}
			}

			return true;
		}

		if ( shouldLogAction )
			action.append( "Round " + currentRound + ": " + KoLCharacter.getUserName() + " " );

		Matcher skillMatcher = SKILL_PATTERN.matcher( urlString );
		if ( skillMatcher.find() )
		{
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

			if ( shouldLogAction )
			{
				RequestLogger.printLine( action.toString() );
				RequestLogger.updateSessionLog( action.toString() );
			}

			return true;
		}

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
				action1 = CombatSettings.getShortCombatOptionName( "item " + item );
				if ( shouldLogAction )
					action.append( "uses the " + item );
			}

			itemMatcher = ITEM2_PATTERN.matcher( urlString );
			if ( itemMatcher.find() )
			{
				item = TradeableItemDatabase.getItemName( StaticEntity.parseInt( itemMatcher.group(1) ) );
				if ( item != null )
				{
					action2 = CombatSettings.getShortCombatOptionName( "item " + item );
					if ( shouldLogAction )
						action.append( " and uses the " + item );
				}
			}

			if ( shouldLogAction )
			{
				action.append( "!" );
				RequestLogger.printLine( action.toString() );
				RequestLogger.updateSessionLog( action.toString() );
			}

			return true;
		}

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
		else
		{
			action1 = null;
			if ( shouldLogAction )
				action.append( "casts CHANCE!" );
		}

		if ( shouldLogAction )
		{
			RequestLogger.printLine( action.toString() );
			RequestLogger.updateSessionLog( action.toString() );
		}

		return true;
	}
}
