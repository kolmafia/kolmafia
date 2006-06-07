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
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An extension of <code>KoLRequest</code> which handles fights
 * and battles.  A new instance is created and started for every
 * round of battle.
 */

public class FightRequest extends KoLRequest
{
	public static final AdventureResult DICTIONARY1 = new AdventureResult( 536, 1 );
	public static final AdventureResult DICTIONARY2 = new AdventureResult( 1316, 1 );

	private int roundCount;
	private int turnsUsed = 0;
	private String action1, action2;

	private static String encounter = "";
	private static final String [] RARE_MONSTERS =
	{
		// Ultra-rare monsters

		"baiowulf",
		"crazy bastard",
		"hockey elemental",
		"hypnotist of hey deze",
		"infinite meat bug",

		// Monsters that scale with player level and can't be defeated
		// with normal tactics

		"candied yam golem",
		"malevolent tofurkey",
		"possessed can of cranberry sauce",
		"stuffing golem"
	};

	/**
	 * Constructs a new <code>FightRequest</code>.  The client provided will
	 * be used to determine whether or not the fight should be started and/or
	 * continued, and the user settings will be used to determine the kind
	 * of action1 to be taken during the battle.
	 */

	public FightRequest( KoLmafia client )
	{
		super( client, "fight.php" );
		this.roundCount = 0;

		FightRequest.encounter = "";
		this.turnsUsed = 0;
	}

	public void nextRound()
	{
		clearDataFields();
		++roundCount;

		// Now, to test if the user should run away from the
		// battle - this is an HP test.

		int haltTolerance = (int)( Double.parseDouble( getProperty( "battleStop" ) ) * (double) KoLCharacter.getMaximumHP() );

		action1 = getProperty( "battleAction" );
		action2 = null;

		for ( int i = 0; i < RARE_MONSTERS.length; ++i )
			if ( encounter.indexOf( RARE_MONSTERS[i] ) != -1 )
				client.updateDisplay( ABORT_STATE, "You have encountered the " + encounter );

		if ( roundCount > 1 && action1.equals( "custom" ) )
		{
			action1 = CombatSettings.getSetting( encounter, roundCount - 2 );
			if ( action1.startsWith( "item" ) )
			{
				String name = (String) TradeableItemDatabase.getMatchingNames( action1.substring(4).trim() ).get(0);
				action1 = name == null ? "attack" : "item" + TradeableItemDatabase.getItemID( name );
			}
			else if ( action1.startsWith( "skill" ) )
			{
				String name = KoLmafiaCLI.getCombatSkillName( action1.substring(5).trim() );
				action1 = name == null ? "attack" : String.valueOf( ClassSkillsDatabase.getSkillID( name ) );
			}
		}

		if ( roundCount == 1 )
		{
			// If this is the first round, you
			// actually wind up submitting no
			// extra data.

			action1 = "attack";
		}
		else if ( action1.equals( "abort" ) || !KoLmafia.permitsContinue() )
		{
			// If the user has chosen to abort
			// combat, flag it.

			action1 = "...";
		}
		else if ( haltTolerance != 0 && KoLCharacter.getCurrentHP() <= haltTolerance )
		{
			// If you plan on halting the battle
			// due to HP loss, then flag it.

			action1 = "...";
		}
		else if ( action1.startsWith( "run" ) )
		{
			action1 = "runaway";
			addFormField( "action", action1 );
		}
		else if ( action1.equals( "attack" ) )
		{
			action1 = "attack";
			addFormField( "action", action1 );
		}

		// If the player wants to use an item, make sure he has one
		else if ( action1.startsWith( "item" ) )
		{
			int itemID = Integer.parseInt( action1.substring( 4 ) );
			int itemCount = (new AdventureResult( itemID, 1 )).getCount( KoLCharacter.getInventory() );

			if ( itemCount == 0 )
			{
				action1 = "attack";
				addFormField( "action", action1 );
			}
			else
			{
				addFormField( "action", "useitem" );
				addFormField( "whichitem", String.valueOf( itemID ) );

				if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) && itemCount >= 2 )
				{
					action2 = action1;
					addFormField( "whichitem2", String.valueOf( itemID ) );
				}
			}
		}

		// Skills use MP. Make sure the character has enough
		else if ( KoLCharacter.getCurrentMP() < getActionCost() )
		{
			action1 = "attack";
			addFormField( "action", action1 );
		}
		else if ( action1.equals( "moxman" ) )
		{
			action1 = "moxman";
			addFormField( "action", action1 );
		}

		// If the player wants to use a skill, make sure he knows it
		else
		{
			String skillName = ClassSkillsDatabase.getSkillName( Integer.parseInt( action1 ) );

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
	 * the client will be notified; otherwise, the next battle will be run
	 * automatically.  All fighting terminates if the client cancels their
	 * request; note that battles are not automatically completed.  However,
	 * the battle's execution will be reported in the statistics for the
	 * requests, and will count against any outstanding requests.
	 */

	public void run()
	{
		nextRound();

		if ( KoLmafia.refusesContinue() || action1.equals( "..." ) )
		{
			action1 = null;
			action2 = null;

			clearDataFields();
			super.run();

			if ( turnsUsed == 0 )
				showInBrowser( true );

			return;
		}

		super.run();
	}

	protected void processResults()
	{
		// Spend MP and consume items

		payActionCost();

		// If this is the first round, then register the opponent
		// you are fighting against.

		if ( roundCount == 1 )
			FightRequest.encounter = AdventureRequest.registerEncounter( this );

		if ( responseText.indexOf( "fight.php" ) != -1 )
		{
			// This is a fall-through state.  This means that
			// you need to run another fight.
		}
		else if ( responseText.indexOf( "WINWINWIN" ) != -1 )
		{
			// The battle was won!  If the user canceled, say
			// it's complete.

			if ( !KoLmafia.permitsContinue() )
				KoLmafia.updateDisplay( ERROR_STATE, "Battle completed, adventures aborted." );

			this.turnsUsed = 1;
		}
		else if ( responseText.indexOf( "You run away" ) != -1 )
		{
			if ( !KoLmafia.permitsContinue() )
				KoLmafia.updateDisplay( ERROR_STATE, "Battle completed, adventures aborted." );

			this.turnsUsed = 1;
		}
		else if ( responseText.indexOf( "You lose." ) != -1 )
		{
			// If you lose the battle, you should update the display to
			// indicate that the battle has been finished; you should
			// also notify the client that an adventure was completed,
			// but that the loop should be halted.

			if ( KoLmafia.refusesContinue() )
				KoLmafia.updateDisplay( ABORT_STATE, "Battle completed, adventures aborted." );
			else if ( KoLCharacter.getCurrentHP() == 0 )
				KoLmafia.updateDisplay( ERROR_STATE, "You were defeated!" );

			this.turnsUsed = 1;
		}

		// Otherwise, you still have more rounds to fight.
		// move onto the next round and then rerun the
		// request.

		super.processResults();

		if ( turnsUsed == 0 )
			run();
	}

	public int getCombatRound()
	{	return roundCount;
	}

	private int getActionCost()
	{
		if ( action1.equals( "attack" ) )
			return 0;

		if ( action1.startsWith( "item" ) )
			return 0;

		if ( action1.equals( "moxman" ) )
			return KoLCharacter.getLevel();

		return ClassSkillsDatabase.getMPConsumptionByID( Integer.parseInt( action1 ) );
	}

	private boolean hasActionCost( int itemID )
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

	private void payActionCost()
	{
		if ( action1 == null || action1.equals( "" ) || action1.equals( "..." ) )
			return;

		if ( action1.equals( "attack" ) || action1.equals( "runaway" ) )
			return;

		if ( action1.startsWith( "item" ) )
		{
			int id1 = Integer.parseInt( action1.substring( 4 ) );

			if ( hasActionCost( id1 ) )
				client.processResult( new AdventureResult( id1, -1 ) );

			if ( action2 == null || action2.equals( "" ) || action2.equals( "..." ) )
				return;

			int id2 = Integer.parseInt( action2.substring( 4 ) );

			if ( hasActionCost( id2 ) )
				client.processResult( new AdventureResult( id2, -1 ) );

			return;
		}

		int mp = action1.equals( "moxman" ) ? KoLCharacter.getLevel() :
			ClassSkillsDatabase.getMPConsumptionByID( Integer.parseInt( action1 ) );

		if ( mp > 0 )
			client.processResult( new AdventureResult( AdventureResult.MP, 0 - mp ) );
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
	{	return turnsUsed;
	}
}
