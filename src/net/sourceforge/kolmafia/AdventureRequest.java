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
import java.util.StringTokenizer;

/**
 * An extension of a <code>KoLRequest</code> that handles generic adventures,
 * such as those which involve fighting, vacations at the shore, or gambling
 * at the casino.  It will not handle trips to the hermit or to the sewers,
 * as these must be handled differently.
 */

public class AdventureRequest extends KoLRequest
{
	private static final int NEEDED_DELAY = 1000;
	private static final int ACTUAL_DELAY = NEEDED_DELAY - REFRESH_RATE;

	private String formSource;
	private String adventureID;
	private int adventuresUsed;

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

		if ( formSource.equals( "adventure.php" ) )
			addFormField( "adv", adventureID );
		else if ( formSource.equals( "shore.php" ) )
		{
			addFormField( "whichtrip", adventureID );
			addFormField( "pwd", client.getPasswordHash() );
		}
		else if ( formSource.equals( "casino.php" ) )
		{
			addFormField( "action", "slot" );
			addFormField( "whichslot", adventureID );
		}
		else if ( formSource.equals( "dungeon.php" ) )
		{
			addFormField( "action", "Yep" );
			addFormField( "option", "1" );
			addFormField( "pwd", client.getPasswordHash() );
		}
		else
			addFormField( "action", adventureID );

		// If you took a trip to the shore, you would use up 3 adventures
		// for each trip

		this.adventuresUsed = 0;
		if ( formSource.equals( "shore.php" ) )
			this.adventuresUsed = 3;
		else if ( formSource.equals( "casino.php" ) )
		{
			if ( adventureID.equals( "11" ) )
				this.adventuresUsed = 1;
		}
		else
			this.adventuresUsed = 1;
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
		// Before running the next request, you should wait for the
		// refresh rate indicated - this is likely the default rate
		// used for the KoLChat.

		if ( NEEDED_DELAY > 0 )
			delay( NEEDED_DELAY );

		// Prevent the request from happening if the client attempted
		// to cancel in the delay period.

		if ( !client.permitsContinue() )
		{
			isErrorState = true;
			return;
		}

		super.run();

		// In the case of a denim axe (which redirects you to a
		// different URL), you can actually skip the adventure.

		if ( !isErrorState && responseCode == 302 && redirectLocation.equals( "choice.php" ) )
		{
			updateDisplay( NOCHANGE, "Encountered choice adventure.  Retrying..." );
			this.run();
			return;
		}

		// Also, if you're using KoLmafia, you're probably not
		// trying to complete the /haiku subquest, so the subquest
		// will be ignored as well

		if ( !isErrorState && responseCode == 302 && redirectLocation.equals( "haiku.php" ) )
		{
			isErrorState = true;
			updateDisplay( ERROR_STATE, "Encountered haiku subquest." );
			client.cancelRequest();
			return;
		}

		// Update if you're redirected to a page the client does not
		// yet recognize.

		if ( !isErrorState && responseCode == 302 && !redirectLocation.equals( "fight.php" ) )
		{
			isErrorState = true;
			updateDisplay( ERROR_STATE, "Redirected to unknown page: " + redirectLocation );
			client.cancelRequest();
			return;
		}

		// From here on out, there will only be data handling
		// if you've encountered a non-redirect request, and
		// an error hasn't occurred.

		if ( isErrorState || responseCode != 200 )
			return;

		// Sometimes, there's no response from the server.
		// In this case, simply rerun the request.

		if ( responseText.trim().length() == 0 )
		{
			updateDisplay( NOCHANGE, "Empty response from server.  Retrying..." );
			this.run();
			return;
		}

		processResults( responseText );

		// You could be beaten up, which halts adventures.  This is
		// true except for two cases: the casino's standard slot
		// machines and the shore vacations when you don't have
		// enough meat, adventures or are too drunk to continue.

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

					updateDisplay( NOCHANGE, "Server error.  Repeating request..." );
					this.run();
					return;
				}
				else
				{
					// Notify the client of failure by telling it that
					// the adventure did not take place and the client
					// should not continue with the next iteration.
					// Friendly error messages to come later.

					isErrorState = true;
					client.cancelRequest();
					updateDisplay( ERROR_STATE, "Adventures aborted!" );
					return;
				}
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

			isErrorState = true;
			client.cancelRequest();
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

		if ( formSource.equals( "adventure.php" ) && adventureID.equals( "70" ) )
			client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
		if ( formSource.equals( "adventure.php" ) && adventureID.equals( "71" ) )
			client.processResult( new AdventureResult( AdventureResult.MEAT, -30 ) );
		if ( formSource.equals( "adventure.php" ) && adventureID.equals( "72" ) )
			client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );

		if ( formSource.equals( "casino.php" ) )
		{
			if ( adventureID.equals( "1" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -5 ) );
			else if ( adventureID.equals( "2" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
			else if ( adventureID.equals( "11" ) )
				client.processResult( new AdventureResult( AdventureResult.MEAT, -10 ) );
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
	{	return isErrorState ? 0 : adventuresUsed;
	}
}