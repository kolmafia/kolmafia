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

		if ( client != null && client.getPasswordHash() != null )
		{
			// Almost all requests use one adventure
			this.adventuresUsed = 1;

			if ( formSource.equals( "adventure.php" ) )
				addFormField( "snarfblat", adventureID );
			else if ( formSource.equals( "shore.php" ) )
			{
				addFormField( "whichtrip", adventureID );
				addFormField( "pwd", client.getPasswordHash() );
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
				addFormField( "pwd", client.getPasswordHash() );
			}
			else if ( formSource.equals( "knob.php" ) )
			{
				addFormField( "pwd", client.getPasswordHash() );
				addFormField( "king", "Yep." );
			}
			else if ( formSource.equals( "mountains.php" ) )
			{
				addFormField( "pwd", client.getPasswordHash() );
				addFormField( "orcs", "1" );
				this.adventuresUsed = 0;
			}
			else if ( formSource.equals( "friars.php" ) )
			{
				addFormField( "pwd", client.getPasswordHash() );
				addFormField( "action", "ritual" );
				this.adventuresUsed = 0;
			}
			else if ( !formSource.equals( "rats.php" ) )
				addFormField( "action", adventureID );
		}

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

		// Handle certain redirections

		if ( responseCode == 302 && !redirectLocation.equals( "maint.php" ) )
		{
			// KoLmafia will not complete the /haiku subquest

			if ( redirectLocation.equals( "haiku.php" ) )
			{
				updateDisplay( ERROR_STATE, "Encountered haiku subquest." );
				client.cancelRequest();
			}

			// Make sure that the daily dungeon allows continues
			// even after a fight.

			else if ( formSource.equals( "dungeon.php" ) )
				client.resetContinueState();

			// Otherwise, the only redirect we understand is
			// fight.php and choice.php.  If it's neither of
			// those, report an error.

			else if ( !redirectLocation.equals( "fight.php" ) && !redirectLocation.equals( "choice.php" ) )
			{
				updateDisplay( ERROR_STATE, "Redirected to unknown page: " + redirectLocation );
				client.cancelRequest();
				return;
			}

			// We're back from a fight, or we completed a choice
			// adventure -- in both cases, adventure usage is zero.

			this.adventuresUsed = 0;
			return;
		}

		// From here on out, there will only be data handling
		// if you've encountered a non-redirect request, and
		// an error hasn't occurred.

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
			updateDisplay( DISABLE_STATE, "Empty response from server.  Retrying..." );
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
			client.cancelRequest();
			updateDisplay( ERROR_STATE, "Ran out of health." );
			return;
		}

		if ( formSource.equals( "adventure.php" ) || formSource.equals( "lair3.php" ) )
		{
			if ( responseText.indexOf( "againform.submit" ) == -1 )
			{
				if ( responseText.indexOf( "No adventure data exists for this location" ) != -1 )
				{
					// In the event that no adventure data existed,
					// this is a server, so KoLmafia should probably
					// repeat the request and notify the client that
					// a server error was encountered.

					updateDisplay( NORMAL_STATE, "Server error.  Repeating request..." );
					this.run();
					return;
				}

				if ( responseText.indexOf( "You shouldn't be here." ) != -1 ||
					  responseText.indexOf( "The Factory has faded back into the spectral mists" ) != -1 )
				{
					// He's missing an item, hasn't been give a quest yet,
					// or otherwise is trying to go somewhere he's not allowed.

					client.cancelRequest();
					updateDisplay( ERROR_STATE, "You can't get to that area." );
					this.adventuresUsed = 0;
					return;
				}

				if ( responseText.indexOf( "This part of the cyrpt is already undefiled" ) != -1 ||
				     responseText.indexOf( "You can't repeat an adventure here." ) != -1 )
				{
					// Nothing more to do in this area

					client.cancelRequest();
					updateDisplay( NORMAL_STATE, "Nothing more to do here." );
					this.adventuresUsed = 0;
					return;
				}

				if ( responseText.indexOf( "You must have at least" ) != -1 )
				{
					client.cancelRequest();
					updateDisplay( ERROR_STATE, "Your stats are too low for this location." );
					this.adventuresUsed = 0;
					return;
				}

				// We can no longer adventure in this area.

				client.cancelRequest();

				// If we gained nothing, assume adventure
				// didn't take place.

				if ( responseText.indexOf( "You acquire an item" ) == -1 && responseText.indexOf( "You gain" ) == -1 )
				{
					updateDisplay( ERROR_STATE, "Adventures aborted!" );
					this.adventuresUsed = 0;
					return;
				}
			}
		}
		else if ( formSource.equals( "knob.php" ) || formSource.equals( "cyrpt.php" ) )
		{
			if ( responseText.indexOf( "You've already slain the Goblin King" ) != -1 ||
			     responseText.indexOf( "Bonerdagon has been defeated" ) != -1 )
			{
				// Nothing more to do in this area

				client.cancelRequest();
				updateDisplay( ERROR_STATE, "You already defeated the Boss." );
				return;
			}
		}
		else if ( formSource.equals( "mountains.php" ) )
		{
			if ( responseText.indexOf( "see no way to cross it" ) != -1 )
				updateDisplay( ERROR_STATE, "You can't cross the Orc Chasm." );
			else if ( responseText.indexOf( "the path to the Valley is clear" ) != -1 )
			{
				updateDisplay( NORMAL_STATE, "You can now cross the Orc Chasm." );
				client.processResult( BRIDGE );
			}
			else
				updateDisplay( ERROR_STATE, "You've already crossed the Orc Chasm." );

			client.cancelRequest();
			return;
		}
		else if ( formSource.equals( "friars.php" ) )
		{
			// "The infernal creatures who have tainted the Friar's
			// copse stream back into the gate, hooting and
			// shrieking."

			if ( responseText.indexOf( "hooting and shrieking" ) != -1 )
			{
				client.processResult( DODECAGRAM );
				client.processResult( CANDLES );
				client.processResult( BUTTERKNIFE );

				KoLCharacter.addAccomplishment( KoLCharacter.FRIARS );
				updateDisplay( NORMAL_STATE, "Taint cleansed." );
			}
			else if ( !KoLCharacter.hasAccomplishment( KoLCharacter.FRIARS ) )
			{
				// Even after you've performed the ritual:
				//   "You don't appear to have all of the
				//   elements necessary to perform the ritual."
				// Detect completion via accomplishments.

				updateDisplay( ERROR_STATE, "You can't perform the ritual." );
			}

			client.cancelRequest();
			return;
		}
		else if ( formSource.equals( "trickortreat.php" ) )
		{
			if ( responseText.indexOf( "You can't go Trick-or-Treating without a costume!" ) != -1 )
			{
				updateDisplay( ERROR_STATE, "Put on a costume and try again!" );
				client.cancelRequest();
				this.adventuresUsed = 0;
				return;
			}
		}
		else if ( responseText.indexOf( "You can't" ) != -1 || responseText.indexOf( "You shouldn't" ) != -1 ||
			responseText.indexOf( "You don't" ) != -1 || responseText.indexOf( "You need" ) != -1 ||
			responseText.indexOf( "You're way too beaten" ) != -1 || responseText.indexOf( "You're too drunk" ) != -1 )
		{
			// Notify the client of failure by telling it that
			// the adventure did not take place and the client
			// should not continue with the next iteration.
			// Friendly error messages to come later.

			client.cancelRequest();
			this.adventuresUsed = 0;
			updateDisplay( ERROR_STATE, "Turn usage aborted!" );
			return;
		}

		// If you took a trip to the shore, 500 meat should be deducted
		// from your running tally.

		if ( formSource.equals( "shore.php" ) )
		{
			client.processResult( new AdventureResult( AdventureResult.MEAT, -500 ) );
			return;
		}

		// If you're at the casino, each of the different slot machines
		// deducts meat from your tally

		if ( formSource.equals( "adventure.php" ) )
		{
			if ( adventureID.equals( "70" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			else if ( adventureID.equals( "71" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -30 ) );
			return;
		}

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
	{	return responseCode == 200 || redirectLocation == null || redirectLocation.equals( "fight.php" ) ? adventuresUsed : 0;
	}
}
