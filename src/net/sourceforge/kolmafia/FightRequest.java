/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An extension of <code>KoLRequest</code> which handles fights
 * and battles.  A new instance is created and started for every
 * round of battle.
 */

public class FightRequest extends KoLRequest
{
	private static boolean isUsingConsultScript = false;
	private static boolean isInstanceRunning = false;

	public static final FightRequest INSTANCE = new FightRequest();

	private static final Pattern SKILL_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern ITEM1_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern ITEM2_PATTERN = Pattern.compile( "whichitem2=(\\d+)" );

	public static final AdventureResult DICTIONARY1 = new AdventureResult( 536, 1 );
	public static final AdventureResult DICTIONARY2 = new AdventureResult( 1316, 1 );

	private static final AdventureResult SEAL_TOOTH = new AdventureResult( 2, 1 );
	private static final AdventureResult TURTLE_SUMMON = new AdventureResult( 4, 1 );
	private static final AdventureResult SPICES = new AdventureResult( 8, 1 );

	private static int currentRound = 0;
	private static int offenseModifier = 0, defenseModifier = 0;

	private static String action1 = null;
	private static String action2 = null;
	private static MonsterDatabase.Monster monsterData = null;
	private static String encounterLookup = "";

	private static final String [] RARE_MONSTERS =
	{
		// Ultra-rare monsters

		"baiowulf",
		"crazy bastard",
		"hockey elemental",
		"hypnotist of hey deze",
		"infinite meat bug",
		"master of thieves"
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

	public void nextRound()
	{
		clearDataFields();
		isUsingConsultScript = false;

		// Now, to test if the user should run away from the
		// battle - this is an HP test.

		int haltTolerance = (int)( StaticEntity.getFloatProperty( "battleStop" ) * (float) KoLCharacter.getMaximumHP() );

		action1 = CombatSettings.getShortCombatOptionName( StaticEntity.getProperty( "battleAction" ) );
		action2 = null;

		for ( int i = 0; i < RARE_MONSTERS.length; ++i )
			if ( encounterLookup.indexOf( RARE_MONSTERS[i] ) != -1 )
				KoLmafia.updateDisplay( ABORT_STATE, "You have encountered the " + encounter );

		if ( currentRound == 0 )
		{
			action1 = StaticEntity.getProperty( "defaultAutoAttack" );
			if ( action1.equals( "" ) || action1.equals( "0" ) )
				action1 = "attack";
		}
		else if ( action1.equals( "custom" ) )
		{
			action1 = CombatSettings.getSetting( encounterLookup, currentRound - 1 );
		}

		// If the person wants to use their own script,
		// then this is where it happens.

		if ( action1.startsWith( "consult" ) )
		{
			isUsingConsultScript = true;
			responseText = StaticEntity.globalStringReplace( responseText, "\"", "\\\"" );

			DEFAULT_SHELL.executeCommand( "call", action1.substring( "consult".length() ).trim() + " (" + currentRound +
				", \"" + encounterLookup + "\", \"" + responseText + "\" )" );

			return;
		}

		// Let the de-level action figure out what
		// should be done, and then re-process.

		if ( action1.equals( "delevel" ) )
			action1 = getMonsterWeakenAction();

		if ( action1 == null || action1.equals( "abort" ) || !KoLmafia.permitsContinue() )
		{
			// If the user has chosen to abort combat, flag it.
			action1 = "abort";
		}
		else if ( currentRound == 0 )
		{
			// If this is the first round, you do not
			// submit extra data.
		}
		else if ( haltTolerance != 0 && KoLCharacter.getCurrentHP() <= haltTolerance )
		{
			// If you plan on halting the battle
			// due to HP loss, then flag it.

			action1 = "abort";
		}
		else if ( action1.startsWith( "run" ) )
		{
			action1 = "runaway";
			addFormField( "action", action1 );
		}
		else if ( action1.startsWith( "attack" ) )
		{
			action1 = "attack";
			addFormField( "action", action1 );
		}

		// If the player wants to use an item, make sure he has one
		else if ( action1.startsWith( "item" ) )
		{
			int itemID = StaticEntity.parseInt( action1.substring( 4 ) );
			int itemCount = (new AdventureResult( itemID, 1 )).getCount( inventory );

			if ( itemCount == 0 )
			{
				action1 = "attack";
				addFormField( "action", action1 );
			}
			else
			{
				addFormField( "action", "useitem" );
				addFormField( "whichitem", String.valueOf( itemID ) );

				if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
				{
					if ( itemCount >= 2 )
					{
						action2 = action1;
						addFormField( "whichitem2", String.valueOf( itemID ) );
					}
					else if ( SEAL_TOOTH.getCount( inventory ) > 0 )
					{
						action2 = "item" + SEAL_TOOTH.getItemID();
						addFormField( "whichitem2", String.valueOf( SEAL_TOOTH.getItemID() ) );
					}
					else if ( TURTLE_SUMMON.getCount( inventory ) > 0 )
					{
						action2 = "item" + TURTLE_SUMMON.getItemID();
						addFormField( "whichitem2", String.valueOf( TURTLE_SUMMON.getItemID() ) );
					}
					else if ( SPICES.getCount( inventory ) > 0 )
					{
						action2 = "item" + SPICES.getItemID();
						addFormField( "whichitem2", String.valueOf( SPICES.getItemID() ) );
					}
				}
			}
		}

		// Skills use MP. Make sure the character has enough
		else if ( KoLCharacter.getCurrentMP() < getActionCost() && passwordHash != null )
		{
			action1 = "attack";
			addFormField( "action", action1 );
		}

		// If the player wants to use a skill, make sure he knows it
		else
		{
			String skillName = ClassSkillsDatabase.getSkillName( StaticEntity.parseInt( action1 ) );

			if ( KoLmafiaCLI.getCombatSkillName( skillName ) == null )
			{
				action1 = "attack";
				addFormField( "action", action1 );
			}
			else
			{
				addFormField( "action", "skill" );
				addFormField( "whichskill", action1 );
			}
		}
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

			nextRound();
			if ( !isUsingConsultScript )
			{
				isInstanceRunning = true;
				super.run();
				isInstanceRunning = false;
			}

			if ( KoLmafia.refusesContinue() || (action1 != null && action1.equals( "abort" )) )
			{
				showInBrowser( true );
				KoLmafia.updateDisplay( ABORT_STATE, "You're on your own, partner." );
				return;
			}
		}
		while ( KoLmafia.permitsContinue() && currentRound != 0 );
	}

	private boolean isAcceptable( int offenseModifier, int defenseModifier )
	{
		if ( monsterData == null )
			return true;

		return monsterData.hasAcceptableDodgeRate( this.offenseModifier + offenseModifier ) &&
			!monsterData.willAlwaysMiss( this.defenseModifier + defenseModifier );
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

		// Entangling Noodles
		if ( !isAcceptable && KoLCharacter.hasSkill( "Entangling Noodles" ) )
		{
			desiredSkill = 3004;
			isAcceptable = isAcceptable( -1 - Math.min( 5, KoLCharacter.getAdjustedMysticality() / 8 ), 0 );
		}

		// Disco Face Stab
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Face Stab" ) )
		{
			desiredSkill = 5012;
			isAcceptable = isAcceptable( -7, -7 );
		}

		return desiredSkill == 0 ? null : String.valueOf( desiredSkill );
	}

	public static void updateCombatData( String encounter, String responseText )
	{
		if ( !isInstanceRunning )
			INSTANCE.responseText = responseText;

		// Spend MP and consume items

		++currentRound;
		payActionCost();

		// If this is the first round, then register the opponent
		// you are fighting against.

		if ( currentRound == 1 )
		{
			encounterLookup = CombatSettings.encounterKey( encounter );
			monsterData = MonsterDatabase.findMonster( encounter );
		}

		if ( responseText.indexOf( "fight.php" ) == -1 )
		{
			encounter = "";
			encounterLookup = "";
			monsterData = null;

			currentRound = 0;
			offenseModifier = 0;
			defenseModifier = 0;

			action1 = null;
			action2 = null;
		}
	}

	private static int getActionCost()
	{
		if ( action1.equals( "attack" ) )
			return 0;

		if ( action1.startsWith( "item" ) )
			return 0;

		return ClassSkillsDatabase.getMPConsumptionByID( StaticEntity.parseInt( action1 ) );
	}

	private static boolean hasActionCost( int itemID )
	{
		switch ( itemID )
		{
		case 2:		// Seal Tooth
		case 4:		// Scroll of Turtle Summoning
		case 8:		// Spices
		case 536:	// Dictionary 1
		case 1316:	// Dictionary 2

			return false;

		default:

			return true;
		}
	}

	public static void payActionCost()
	{
		if ( action1 == null || action1.equals( "" ) )
			return;

		if ( action1.equals( "attack" ) || action1.equals( "runaway" ) )
			return;

		if ( action1.startsWith( "item" ) )
		{
			if ( currentRound == 0 )
				return;

			int id1 = StaticEntity.parseInt( action1.substring( 4 ) );

			if ( hasActionCost( id1 ) )
				StaticEntity.getClient().processResult( new AdventureResult( id1, -1 ) );

			if ( action2 == null || action2.equals( "" ) )
				return;

			int id2 = StaticEntity.parseInt( action2.substring( 4 ) );

			if ( hasActionCost( id2 ) )
				StaticEntity.getClient().processResult( new AdventureResult( id2, -1 ) );

			return;
		}

		int skillID = StaticEntity.parseInt( action1 );
		int mpCost = ClassSkillsDatabase.getMPConsumptionByID( skillID );

		switch ( skillID )
		{
		case 2005: // Shieldbutt
			offenseModifier -= 5;
			defenseModifier -= 5;
			break;

		case 3004: // Entangling Noodles
			offenseModifier -= 6;
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
	{	return responseText == null ? 0 : responseText.indexOf( "fight.php" ) == -1 ? 1 : 0;
	}

	public static boolean processRequest( String urlString )
	{
		if ( urlString.indexOf( "fight.php?" ) == -1 )
			return urlString.indexOf( "fight.php" ) != -1;

		action1 = null;
		action2 = null;

		KoLmafia.getSessionStream().print( " - Round " + currentRound + ": " );

		Matcher skillMatcher = SKILL_PATTERN.matcher( urlString );
		if ( skillMatcher.find() )
		{
			String skill = ClassSkillsDatabase.getSkillName( StaticEntity.parseInt( skillMatcher.group(1) ) );
			if ( skill == null )
			{
				KoLmafia.getSessionStream().println( KoLCharacter.getUsername() + " casts the enchanted spell of CHANCE!" );
			}
			else
			{
				action1 = CombatSettings.getShortCombatOptionName( "skill " + skill );
				KoLmafia.getSessionStream().println( KoLCharacter.getUsername() + " casts the enchanted spell of " + skill.toUpperCase() + "!" );
			}

			return true;
		}

		Matcher itemMatcher = ITEM1_PATTERN.matcher( urlString );
		if ( itemMatcher.find() )
		{
			String item = TradeableItemDatabase.getItemName( StaticEntity.parseInt( itemMatcher.group(1) ) );
			if ( item == null )
			{
				KoLmafia.getSessionStream().print( KoLCharacter.getUsername() + " plays Garin's Harp" );
			}
			else
			{
				action1 = CombatSettings.getShortCombatOptionName( "item " + item );
				KoLmafia.getSessionStream().print( KoLCharacter.getUsername() + " uses the " + item );
			}

			itemMatcher = ITEM2_PATTERN.matcher( urlString );
			if ( itemMatcher.find() )
			{
				KoLmafia.getSessionStream().print( " and " );

				item = TradeableItemDatabase.getItemName( StaticEntity.parseInt( itemMatcher.group(1) ) );
				if ( item == null )
				{
					KoLmafia.getSessionStream().print( "plays the Fairy Flute" );
				}
				else
				{
					action2 = CombatSettings.getShortCombatOptionName( "item " + item );
					KoLmafia.getSessionStream().print( "uses the " + item );
				}
			}

			KoLmafia.getSessionStream().println( "!" );
			return true;
		}

		if ( urlString.indexOf( "runaway" ) != -1 )
		{
			action1 = "runaway";
			KoLmafia.getSessionStream().println( KoLCharacter.getUsername() + " casts the spell of RETURN!" );
		}
		else
		{
			action1 = "attack";
			KoLmafia.getSessionStream().println( KoLCharacter.getUsername() + " attacks with " +
				"fear-inducing body language!" );
		}

		return true;
	}
}
