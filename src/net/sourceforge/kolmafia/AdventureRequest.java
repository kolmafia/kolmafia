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
import java.util.StringTokenizer;

/**
 * An extension of a <code>KoLRequest</code> that handles generic adventures,
 * such as those which involve fighting, vacations at the shore, or gambling
 * at the casino.  It will not handle trips to the hermit or to the sewers,
 * as these must be handled differently.
 */

public class AdventureRequest extends KoLRequest
{
	private String formSource;
	private String adventureID;
	private boolean hasLuckyVersion;
	protected int adventuresUsed;

	public static final AdventureResult ABRIDGED = new AdventureResult( 534, -1 );
	public static final AdventureResult BRIDGE = new AdventureResult( 535, -1 );
	public static final AdventureResult DODECAGRAM = new AdventureResult( 479, -1 );
	public static final AdventureResult CANDLES = new AdventureResult( 480, -1 );
	public static final AdventureResult BUTTERKNIFE = new AdventureResult( 481, -1 );

	/**
	 * Constructs a new <code>AdventureRequest</code> which executes the
	 * adventure designated by the given ID by posting to the provided form,
	 * notifying the given client of results (or errors).
	 *
	 * @param	client	The client to which results will be reported
	 * @param	formSource	The form to which the data will be posted
	 * @param	adventureID	The identifer for the adventure to be executed
	 */

	public AdventureRequest( KoLmafia client, String formSource, String adventureID )
	{
		super( client, formSource );
		this.formSource = formSource;
		this.adventureID = adventureID;

		// The adventure ID is all you need to identify the adventure;
		// posting it in the form sent to adventure.php will handle
		// everything for you.

		// Almost all requests use one adventure
		this.adventuresUsed = 1;

		if ( formSource.equals( "adventure.php" ) )
			addFormField( "snarfblat", adventureID );
		else if ( formSource.equals( "cave.php" ) )
		{
			addFormField( "action", adventureID );
			this.adventuresUsed = adventureID.equals( "end" ) ? 1 : 0;
		}
		else if ( formSource.equals( "shore.php" ) )
		{
			addFormField( "whichtrip", adventureID );
			addFormField( "pwd" );
			this.adventuresUsed = 3;
		}
		else if ( formSource.equals( "casino.php" ) )
		{
			addFormField( "action", "slot" );
			addFormField( "whichslot", adventureID );
			if ( !adventureID.equals( "11" ) )
				this.adventuresUsed = 0;
		}
		else if ( formSource.equals( "dungeon.php" ) )
		{
			addFormField( "action", "Yep" );
			addFormField( "option", "1" );
			addFormField( "pwd" );
		}
		else if ( formSource.equals( "knob.php" ) )
		{
			addFormField( "pwd" );
			addFormField( "king", "Yep." );
		}
		else if ( formSource.equals( "mountains.php" ) )
		{
			addFormField( "pwd" );
			addFormField( "orcs", "1" );
			this.adventuresUsed = 0;
		}
		else if ( formSource.equals( "friars.php" ) )
		{
			addFormField( "pwd" );
			addFormField( "action", "ritual" );
			this.adventuresUsed = 0;
		}
		else if ( formSource.equals( "lair6.php" ) )
			addFormField( "place", adventureID );
		else if ( !formSource.equals( "rats.php" ) )
			addFormField( "action", adventureID );

		hasLuckyVersion = hasLuckyVersion( adventureID );
	}

	public static final boolean hasLuckyVersion( String adventureID )
	{
		for ( int i = 0; i < AdventureDatabase.CLOVER_ADVS.length; ++i )
			if ( AdventureDatabase.CLOVER_ADVS[i].equals( adventureID ) )
				return true;

		return false;
	}

	/**
	 * Executes the <code>AdventureRequest</code>.  All items and stats gained
	 * or lost will be reported to the client, as well as any errors encountered
	 * through adventuring.  Meat lost due to an adventure (such as those to
	 * the casino, the shore, or the tavern) will also be reported.  Note that
	 * adventure costs are not yet being reported.
	 */

	public void run()
	{
		if ( hasLuckyVersion && client.isLuckyCharacter() && getProperty( "cloverProtectActive" ).equals( "true" ) )
			(new ItemStorageRequest( client, ItemStorageRequest.CLOSET_YOUR_CLOVERS )).run();

		// Prevent the request from happening if the client attempted
		// to cancel in the delay period.

		if ( !client.permitsContinue() )
			return;

		super.run();
	}

	protected void processResults()
	{
		// If this is a lucky adventure, then remove a clover
		// from the player's inventory.

		if ( hasLuckyVersion && client.isLuckyCharacter() )
			client.processResult( SewerRequest.CLOVER );

		// Sometimes, there's no response from the server.
		// In this case, simply rerun the request.

		if ( responseText.trim().length() == 0 )
		{
			DEFAULT_SHELL.updateDisplay( "Empty response from server.  Retrying..." );
			this.run();
			return;
		}

		Matcher encounterMatcher = Pattern.compile( "<center><b>(.*?)</b>" ).matcher( responseText );
		if ( encounterMatcher.find() )
			client.registerEncounter( encounterMatcher.group(1) );

		// If you haven't unlocked the orc chasm yet,
		// try doing so now.

		if ( adventureID.equals( "80" ) && responseText.indexOf( "You shouldn't be here." ) != -1 )
		{
			(new AdventureRequest( client, "mountains.php", "" )).run();
			if ( client.permitsContinue() )
				this.run();

			return;
		}

		// We're missing an item, haven't been given a quest yet, or otherwise
		// trying to go somewhere not allowed.

		if ( responseText.indexOf( "You shouldn't be here." ) != -1 || responseText.indexOf( "into the spectral mists" ) != -1 )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You can't get to that area." );
			return;
		}

		// This is a server error. Hope for the
		// best and repeat the request.

		if ( responseText.indexOf( "No adventure data exists for this location" ) != -1 )
		{
			DEFAULT_SHELL.updateDisplay( "Server error.  Repeating request..." );
			this.run();
			return;
		}

		// Nothing more to do in this area

		if ( responseText.indexOf( "This part of the cyrpt is already undefiled" ) != -1 ||
		     responseText.indexOf( "You can't repeat an adventure here." ) != -1 )
		{
			DEFAULT_SHELL.updateDisplay( PENDING_STATE, "Nothing more to do here." );
			return;
		}

		if ( responseText.indexOf( "You must have at least" ) != -1 )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Your stats are too low for this location." );
			return;
		}

		// Cobb's Knob King's Chamber: if you've already
		// defeated the goblin king, go into pending state.

		if ( formSource.equals( "knob.php" ) && responseText.indexOf( "You've already slain the Goblin King" ) != -1 )
			DEFAULT_SHELL.updateDisplay( PENDING_STATE, "You already defeated the Goblin King." );

		// The Haert of the Cyrpt: if you've already defeated
		// the bonerdagon, go into pending state.

		if ( formSource.equals( "cyrpt.php" ) && responseText.indexOf( "Bonerdagon has been defeated" ) != -1 )
			DEFAULT_SHELL.updateDisplay( PENDING_STATE, "You already defeated the Bonerdagon." );

		// The Orc Chasm (pre-bridge)

		if ( formSource.equals( "mountains.php" ) )
		{
			// If there's no link to the valley beyond, put down a
			// bridge

			if ( responseText.indexOf( "value=80" ) == -1 )
			{
				// If you have an unabridged dictionary in your
				// inventory, visit the untinkerer
				// automatically and repeat the request.

				if ( KoLCharacter.hasItem( ABRIDGED, false ) )
				{
					AdventureDatabase.retrieveItem( ABRIDGED.getNegation() );
					(new UntinkerRequest( client, ABRIDGED.getItemID() )).run();

					this.run();
					return;
				}

				// Otherwise, the player is unable to cross the
				// orc chasm at this time.

				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You can't cross the Orc Chasm." );
				return;
			}

			if ( responseText.indexOf( "the path to the Valley is clear" ) != -1 )
			{
				DEFAULT_SHELL.updateDisplay( PENDING_STATE, "You can now cross the Orc Chasm." );
				client.processResult( BRIDGE );
				return;
			}

			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You've already crossed the Orc Chasm." );
			return;
		}

		// The Deep Fat Friars' Ceremony Location

		if ( formSource.equals( "friars.php" ) )
		{
			// "The infernal creatures who have tainted the Friar's
			// copse stream back into the gate, hooting and
			// shrieking."

			if ( responseText.indexOf( "hooting and shrieking" ) != -1 )
			{
				client.processResult( DODECAGRAM );
				client.processResult( CANDLES );
				client.processResult( BUTTERKNIFE );

				Matcher learnedMatcher = Pattern.compile( "You emerge with a (.*?) of Steel" ).matcher( responseText );
				if ( learnedMatcher.find() )
					KoLCharacter.addAvailableSkill( new UseSkillRequest( client, learnedMatcher.group(1) + " of Steel", "", 1 ) );
				
				DEFAULT_SHELL.updateDisplay( PENDING_STATE, "Taint cleansed." );
				return;
			}
                        // Even after you've performed the ritual:
                        // "You don't appear to have all of the elements
                        // necessary to perform the ritual."

                        DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You can't perform the ritual." );
                        return;
		}

		// If we gained nothing, assume adventure didn't take place.

		if ( !formSource.equals( "dungeons.php" ) && responseText.indexOf( "You lose" ) == -1 && responseText.indexOf( "You acquire" ) == -1 && responseText.indexOf( "You gain" ) == -1 )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "No results detected." );
			return;
		}

		// If you're at the casino, each of the different slot
		// machines deducts meat from your tally

		if ( formSource.equals( "casino.php" ) )
		{
			if ( adventureID.equals( "1" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -5 ) );
			else if ( adventureID.equals( "2" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			else if ( adventureID.equals( "11" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
		}

		if ( adventureID.equals( "70" ) )
			client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
		else if ( adventureID.equals( "71" ) )
			client.processResult( new AdventureResult( AdventureResult.MEAT, -30 ) );

		// Shore Trips cost 500 meat each; handle
		// the processing here.

		if ( formSource.equals( "shore.php" ) )
			client.processResult( new AdventureResult( AdventureResult.MEAT, -500 ) );

		// Trick-or-treating requires a costume;
		// notify the user of this error.

		if ( formSource.equals( "trickortreat.php" ) && responseText.indexOf( "without a costume" ) != -1 )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You must wear a costume." );
			return;
		}

		super.processResults();
	}

	public int getAdventuresUsed()
	{	return client.permitsContinue() ? adventuresUsed : 0;
	}
}
