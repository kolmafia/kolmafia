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

	private String action;
	private int roundCount;
	private int turnsUsed = 0;
	private static String encounter = "";

	private static final String [] RARE_MONSTERS =
	{ "baiowulf", "crazy bastard", "hockey elemental", "hypnotist of hey deze", "infinite meat bug" };

	/**
	 * Constructs a new <code>FightRequest</code>.  The client provided will
	 * be used to determine whether or not the fight should be started and/or
	 * continued, and the user settings will be used to determine the kind
	 * of action to be taken during the battle.
	 */

	public FightRequest( KoLmafia client )
	{
		super( client, "fight.php" );
		this.roundCount = 0;
		this.turnsUsed = 0;
		nextRound();
	}

	public void nextRound()
	{
		clearDataFields();
		++roundCount;

		// Now, to test if the user should run away from the
		// battle - this is an HP test.

		int haltTolerance = (int)( Double.parseDouble( getProperty( "battleStop" ) ) * (double) KoLCharacter.getMaximumHP() );

		action = getProperty( "battleAction" );

		for ( int i = 0; i < RARE_MONSTERS.length; ++i )
			if ( encounter.indexOf( RARE_MONSTERS[i] ) != -1 )
				client.updateDisplay( ABORT_STATE, "You have encountered the " + encounter );

		if ( roundCount > 1 && action.equals( "custom" ) )
		{
			action = CombatSettings.getSetting( encounter, roundCount - 2 );
			if ( action.startsWith( "item" ) )
			{
				String name = (String) TradeableItemDatabase.getMatchingNames( action.substring(4).trim() ).get(0);
				action = name == null ? "attack" : "item" + TradeableItemDatabase.getItemID( name );
			}
			else if ( action.startsWith( "skill" ) )
			{
				String name = KoLmafiaCLI.getCombatSkillName( action.substring(5).trim() );
				action = name == null ? "attack" : String.valueOf( ClassSkillsDatabase.getSkillID( name ) );
			}
		}

		if ( roundCount == 1 )
		{
			// If this is the first round, you
			// actually wind up submitting no
			// extra data.

			action = "attack";
		}
		else if ( action.equals( "abort" ) || !client.permitsContinue() )
		{
			// If the user has chosen to abort
			// combat, flag it.

			action = "...";
		}
		else if ( haltTolerance != 0 && KoLCharacter.getCurrentHP() <= haltTolerance )
		{
			// If you plan on halting the battle
			// due to HP loss, then flag it.

			action = "...";
		}
		else if ( action.startsWith( "run" ) )
		{
			action = "runaway";
			addFormField( "action", action );
		}
		else if ( action.equals( "attack" ) )
		{
			action = "attack";
			addFormField( "action", action );
		}

		// If the player wants to use an item, make sure he has one
		else if ( action.startsWith( "item" ) )
		{
			int itemID = Integer.parseInt( action.substring( 4 ) );

			if ( (new AdventureResult( itemID, 1 )).getCount( KoLCharacter.getInventory() ) == 0 )
			{
				action = "attack";
				addFormField( "action", action );
			}
			else
			{
				addFormField( "action", "useitem" );
				addFormField( "whichitem", String.valueOf( itemID ) );

				if ( KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) && !hasActionCost( itemID ) )
					addFormField( "whichitem2", String.valueOf( itemID ) );
			}
		}

		// Skills use MP. Make sure the character has enough
		else if ( KoLCharacter.getCurrentMP() < getActionCost() )
		{
			action = "attack";
			addFormField( "action", action );
		}
		else if ( action.equals( "moxman" ) )
		{
			action = "moxman";
			addFormField( "action", action );
		}

		// If the player wants to use a skill, make sure he knows it
		else
		{
			String skillName = ClassSkillsDatabase.getSkillName( Integer.parseInt( action ) );

			if ( KoLmafiaCLI.getCombatSkillName( skillName ) == null )
			{
				action = "attack";
				addFormField( "action", action );
			}
			else
			{
				addFormField( "action", "skill" );
				addFormField( "whichskill", action );
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
		if ( action.equals( "..." ) || !client.permitsContinue() )
		{
			DEFAULT_SHELL.updateDisplay( ABORT_STATE, "Battle stopped.  Please finish in-browser." );

			// Finish in browser if requested
			if ( getProperty( "synchronizeFightFrame" ).equals( "false" ) )
				showInBrowser( true );

			return;
		}

		super.run();

		// Spend MP and consume items
		payActionCost();

		// If this is the first round, then register the opponent
		// you are fighting against.

		if ( roundCount == 1 )
		{
			Matcher encounterMatcher = Pattern.compile( "<span id='monname'>(.*?)</span>" ).matcher( responseText );

			if ( encounterMatcher.find() )
			{
				FightRequest.encounter = encounterMatcher.group(1).toLowerCase();
				client.registerEncounter( encounter );
			}
		}

		if ( responseText.indexOf( "fight.php" ) != -1 )
		{
			nextRound();
			run();
		}
		else if ( responseText.indexOf( "WINWINWIN" ) != -1 )
		{
			// The battle was won!  If the user canceled, say
			// it's complete.

			if ( !client.permitsContinue() )
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Battle completed, adventures aborted." );

			// If you can't battle again in this location,
			// cancel future iterations.

			else if ( responseText.indexOf( "againform.submit" ) == -1 && responseText.indexOf( "lair3.php" ) == -1 && responseText.indexOf( "lair4.php" ) == -1 && responseText.indexOf( "lair5.php" ) == -1 && responseText.indexOf( "lair6.php" ) == -1 )
				DEFAULT_SHELL.updateDisplay( PENDING_STATE, "Nothing left to do here." );

			this.turnsUsed = 1;
		}
		else if ( responseText.indexOf( "You lose." ) != -1 )
		{
			// If you lose the battle, you should update the display to
			// indicate that the battle has been finished; you should
			// also notify the client that an adventure was completed,
			// but that the loop should be halted.

			if ( KoLCharacter.getCurrentHP() == 0 )
			{
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You were defeated!" );
				this.turnsUsed = 1;
			}
			else
			{
				// Sometimes you hit the thirty round limit.  Here, print
				// the error to the debug log and continue adventuring
				// as normal.

				DEFAULT_SHELL.updateDisplay( "Thirty combat round limit exceeded." );
				this.turnsUsed = 1;
			}
		}
		else
		{
			// Otherwise, you still have more rounds to fight.
			// move onto the next round and then rerun the
			// request.

			nextRound();
			run();
		}
	}

	public int getCombatRound()
	{	return roundCount;
	}

	private int getActionCost()
	{
		if ( action.equals( "attack" ) )
			return 0;

		if ( action.startsWith( "item" ) )
			return 0;

		if ( action.equals( "moxman" ) )
			return KoLCharacter.getLevel();

		return ClassSkillsDatabase.getMPConsumptionByID( Integer.parseInt( action ) );
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
		if ( action.equals( "attack" ) || action.equals( "runaway" ) )
			return;

		if ( action.startsWith( "item" ) )
		{
			int itemID = Integer.parseInt( action.substring( 4 ) );

			if ( hasActionCost( itemID ) )
				client.processResult( new AdventureResult( itemID, -1 ) );

			return;
		}

		int mp = action.equals( "moxman" ) ? KoLCharacter.getLevel() :
			ClassSkillsDatabase.getMPConsumptionByID( Integer.parseInt( action ) );

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
