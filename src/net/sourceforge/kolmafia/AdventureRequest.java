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

public class AdventureRequest extends KoLRequest
{
	private String formSource;
	private String adventureID;

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
			addFormField( "whichtrip", adventureID );
		else if ( formSource.equals( "casino.php" ) )
		{
			addFormField( "action", "slot" );
			addFormField( "whichslot", adventureID );
		}
	}

	public void run()
	{
		super.run();

		// Most of the time, you run into a battle sequence.  However,
		// if not, you either run into an error state, or there was a
		// special event taking place in that adventure.

		if ( !isErrorState && responseCode != 302 )
		{
			// From preliminary tests, finding out whether or not the
			// adventure was successful is equivalent to whether or
			// not there is centered text in the results.  This is
			// true except for two cases: the casino's standard slot
			// machines and the shore vacations when you don't have
			// enough meat, adventures or are too drunk to continue.

			int resultIndex = replyContent.indexOf( "<p><center>" );

			if ( resultIndex == -1 || replyContent.contains( "You can't afford" ) ||
				replyContent.contains( "You don't have enough" ) || replyContent.contains( "You're too drunk" ) )
			{
				// Notify the client of failure by telling it that
				// the adventure did not take place and the client
				// should not continue with the next iteration.
				// Friendly error messages to come later.

				client.updateAdventure( false, false );
				frame.updateDisplay( KoLFrame.ENABLED_STATE, "Adventures aborted!" );
				return;
			}

			// Also, during the mining adventure, if you lose hit points
			// from a cave in, there are no results to process, so simply
			// update the client without parsing the results.

			if ( !replyContent.contains( "An inexpert swing of your Mattock" ) )
				processResults( replyContent.substring( resultIndex + 12 ) );

			client.updateAdventure( true, true );

			// If you took a trip to the shore, 500 meat should be deducted
			// from your running tally.

			if ( formSource.equals( "shore.php" ) )
				client.addToResultTally( new AdventureResult( AdventureResult.MEAT, -500 ) );

			// If you went to the tavern, 100 meat should be deducted from
			// your running tally.

			if ( formSource.equals( "adventure.php" ) && adventureID.equals( "25" ) )
				client.addToResultTally( new AdventureResult( AdventureResult.MEAT, -100 ) );

			// If you're at the casino, each of the different slot machines
			// deducts meat from your tally

			if ( formSource.equals( "adventure.php" ) && adventureID.equals( "70" ) )
				client.addToResultTally( new AdventureResult( AdventureResult.MEAT, -10 ) );
			if ( formSource.equals( "adventure.php" ) && adventureID.equals( "71" ) )
				client.addToResultTally( new AdventureResult( AdventureResult.MEAT, -30 ) );
			if ( formSource.equals( "adventure.php" ) && adventureID.equals( "72" ) )
				client.addToResultTally( new AdventureResult( AdventureResult.MEAT, -10 ) );

			if ( formSource.equals( "casino.php" ) )
			{
				if ( adventureID.equals( "1" ) )
					client.addToResultTally( new AdventureResult( AdventureResult.MEAT, -5 ) );
				else if ( adventureID.equals( "2" ) )
					client.addToResultTally( new AdventureResult( AdventureResult.MEAT, -10 ) );
				else if ( adventureID.equals( "11" ) )
					client.addToResultTally( new AdventureResult( AdventureResult.MEAT, -10 ) );
			}
		}
		else if ( !isErrorState && redirectLocation.equals( "choice.php" ) )
		{
			// In the case of a denim axe (which redirects you to a
			// different URL), you can actually skip the adventure.

			(new AdventureRequest( client, formSource, adventureID )).start();
		}
	}
}