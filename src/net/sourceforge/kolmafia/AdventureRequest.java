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

	public AdventureRequest( KoLmafia client, String formSource, String adventureID )
	{
		super( client, formSource );
		this.formSource = formSource;

		// The adventure ID is all you need to identify the adventure;
		// posting it in the form sent to adventure.php will handle
		// everything for you.

		if ( formSource.equals( "adventure.php" ) )
			addFormField( "adv", adventureID );
		else if ( formSource.equals( "shore.php" ) )
			addFormField( "whichtrip", adventureID );
	}

	public void run()
	{
		super.run();

		// Most of the time, you run into a battle sequence.  However,
		// if not, you either run into an error state, or there was a
		// special event taking place in that adventure.  This is where
		// those events and error states would get parsed.

		if ( responseCode != 302 )
		{
			String formPath = formConnection.getURL().getPath();

			// The first set of stuff to take care of involves
			// generic adventures.

			if ( formPath.equals( "/adventure.php" ) )
			{
				if ( replyContent.contains( "You're out of adventures" ) )
				{
					// The easiest one is if you're out of adventures;
					// then you need to notify the client of failure
					// by telling it that the adventure did not take
					// place and the client should not continue with
					// the next iteration.

					client.updateAdventure( false, false );
					frame.updateDisplay( KoLFrame.LOGGED_IN_STATE, "Adventures aborted!" );
					return;
				}
			}

			// The next set of stuff to take care of involves
			// vacations at the shore.

			if ( formPath.equals( "/shore.php" ) )
			{
				int resultIndex = replyContent.indexOf( "<p><center>" );

				if ( resultIndex == -1 )
				{
					// The easiest one is if there were no results
					// for the trip, which means it failed.  Here,
					// just notify the client like for a standard
					// adventure of the failure.

					client.updateAdventure( false, false );
					frame.updateDisplay( KoLFrame.LOGGED_IN_STATE, "Adventures aborted!" );
					return;
				}

				// If it gets this far, then everything is okay,
				// so you parse the results of the vacation trip.

				processResults( replyContent.substring( resultIndex + 12 ) );
			}
		}
	}
}