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

	private void resetAdventuresUsed()
	{
		this.adventuresUsed = formSource.equals( "shore.php" ) ? 3 :
			formSource.equals( "casino.php" ) && !adventureID.equals( "11" ) ? 0 :
			formSource.equals( "mountains.php" ) || formSource.equals( "friars.php" ) ? 0 : 1;
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
		resetAdventuresUsed();
		if ( hasLuckyVersion && client.isLuckyCharacter() && getProperty( "cloverProtectActive" ).equals( "true" ) )
			(new ItemStorageRequest( client, ItemStorageRequest.CLOSET_YOUR_CLOVERS )).run();

		// Prevent the request from happening if the client attempted
		// to cancel in the delay period.

		if ( !client.permitsContinue() )
			return;

		super.run();

		// Handle certain redirections, because they can change the
		// current continue state.

		if ( responseCode == 302 && !redirectLocation.equals( "maint.php" ) )
		{
			// KoLmafia will not complete the /haiku subquest

			if ( redirectLocation.equals( "haiku.php" ) )
			{
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Encountered haiku subquest." );
			}

			// Make sure that the daily dungeon allows continues
			// even after a fight.

			else if ( formSource.equals( "dungeon.php" ) )
				DEFAULT_SHELL.updateDisplay( CONTINUE_STATE, "" );

			// Otherwise, the only redirect we understand is
			// fight.php and choice.php.  If it's neither of
			// those, report an error.

			else if ( !redirectLocation.equals( "fight.php" ) && !redirectLocation.equals( "choice.php" ) )
			{
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Redirected to unknown page: " + redirectLocation );
				return;
			}

			// We're back from a fight, or we completed a choice
			// adventure -- in both cases, adventure usage was
			// calculated when that page was processed

			this.adventuresUsed = 0;
			return;
		}
	}

	protected void processResults()
	{
		super.processResults();

		// Don't look at results for errors or redirections; fights and
		// choices are handled elsewhere.
		if ( responseCode != 200 )
			return;

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

		// You could be beaten up, which halts adventures.  This is
		// true except for two cases: the casino's standard slot
		// machines and the shore vacations when you don't have
		// enough meat, adventures or are too drunk to continue.

		if ( KoLCharacter.getCurrentHP() == 0 )
		{
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Ran out of health." );
			return;
		}

		// Do special processing for each KoL PHP file we connect to.
		// fight.php and choice.php are handled via redirection.

		// A regular adventure location or the Sorceress's Hedge Maze
		if ( formSource.equals( "adventure.php" ) || formSource.equals( "lair3.php" ) )
		{
			// Look more closely if no "adventure again" link
			if ( responseText.indexOf( "againform.submit" ) == -1 )
			{
				if ( responseText.indexOf( "No adventure data exists for this location" ) != -1 )
				{
					// This is a server error. Hope for the
					// best and repeat the request.
					DEFAULT_SHELL.updateDisplay( "Server error.  Repeating request..." );
					this.run();
					return;
				}

				// If you haven't unlocked the orc chasm yet,
				// try doing so now.
				if ( adventureID.equals( "80" ) && responseText.indexOf( "You shouldn't be here." ) != -1 )
				{
					(new AdventureRequest( client, "mountains.php", "" )).run();

					if ( client.permitsContinue() )
						this.run();

					return;
				}

				if ( responseText.indexOf( "You shouldn't be here." ) != -1 ||
				     responseText.indexOf( "The Factory has faded back into the spectral mists" ) != -1 )
				{
					// We're missing an item, haven't been
					// give a quest yet, or otherwise
					// trying to go somewhere not allowed.

					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You can't get to that area." );
					this.adventuresUsed = 0;
					return;
				}

				if ( responseText.indexOf( "This part of the cyrpt is already undefiled" ) != -1 ||
				     responseText.indexOf( "You can't repeat an adventure here." ) != -1 )
				{
					// Nothing more to do in this area

					DEFAULT_SHELL.updateDisplay( PENDING_STATE, "Nothing more to do here." );
					this.adventuresUsed = 0;
					return;
				}

				if ( responseText.indexOf( "You must have at least" ) != -1 )
				{
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Your stats are too low for this location." );
					this.adventuresUsed = 0;
					return;
				}

				// If we gained nothing, assume adventure
				// didn't take place.

				if ( responseText.indexOf( "You acquire an item" ) == -1 && responseText.indexOf( "You gain" ) == -1 )
				{
					DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Adventures aborted (empty response from server)." );
					this.adventuresUsed = 0;
					return;
				}
				return;
			}

			// The "adventure again" link is present, so no problem
			// repeating an adventure in this location.

			// See if we really did anything
			if ( responseText.indexOf( "You can't" ) != -1 ||
			     responseText.indexOf( "You shouldn't" ) != -1 ||
			     responseText.indexOf( "You don't" ) != -1 ||
			     responseText.indexOf( "You need" ) != -1 ||
			     responseText.indexOf( "You're way too beaten" ) != -1 ||
			     responseText.indexOf( "You're too drunk" ) != -1 )
			{
				// Give a generic message for now and abort
				// further adventuring.

				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Turn usage aborted!" );
				this.adventuresUsed = 0;
				return;
			}

			// Certain one-time adventures do not cost a turn.
			if ( AdventureDatabase.freeAdventure( responseText ) )
				this.adventuresUsed = 0;

			// If you're at the casino, each of the different slot
			// machines deducts meat from your tally
			if ( adventureID.equals( "70" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			else if ( adventureID.equals( "71" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -30 ) );

			return;
		}

		// Cobb's Knob King's Chamber
		if ( formSource.equals( "knob.php" ) )
		{
			if ( responseText.indexOf( "You've already slain the Goblin King" ) != -1 )
				// Nothing more to do in this area

				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You already defeated the Goblin King." );
			return;
		}

		// The Haert of the Cyrpt
		if ( formSource.equals( "cyrpt.php" ) )
		{
			if ( responseText.indexOf( "Bonerdagon has been defeated" ) != -1 )
				// Nothing more to do in this area

				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You already defeated the Bonerdagon." );
			return;
		}

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
				DEFAULT_SHELL.updateDisplay( "You can now cross the Orc Chasm." );
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

				DEFAULT_SHELL.updateDisplay( "Taint cleansed." );
				return;
			}

			// Even after you've performed the ritual:
			// "You don't appear to have all of the elements
			// necessary to perform the ritual."

			this.adventuresUsed = 0;
			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You can't perform the ritual." );
			return;
		}

		// Shore Trips cost 500 meat each
		if ( formSource.equals( "shore.php" ) )
		{
			client.processResult( new AdventureResult( AdventureResult.MEAT, -500 ) );
			return;
		}

		// Trick-or-treating
		if ( formSource.equals( "trickortreat.php" ) )
		{
			if ( responseText.indexOf( "You can't go Trick-or-Treating without a costume!" ) != -1 )
			{
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Put on a costume and try again!" );
				this.adventuresUsed = 0;
			}
			return;
		}

		// Casino games cost meat to play
		if ( formSource.equals( "casino.php" ) )
		{
			if ( adventureID.equals( "1" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -5 ) );
			else if ( adventureID.equals( "2" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			else if ( adventureID.equals( "11" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			return;
		}
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
	{
		if ( responseCode == 200 || redirectLocation == null )
			return adventuresUsed;

		// Fights and choices take an adventure
		if ( redirectLocation.equals( "fight.php" ) || redirectLocation.equals( "choice.php" ) )
			return adventuresUsed;

		// Other redirections don't use an adventure.
		return 0;
	}
}
