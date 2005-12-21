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
 * An extension of the generic <code>KoLRequest</code> class which handles
 * adventures in the Kingdom of Loathing sewer.
 */

public class SewerRequest extends KoLRequest
{
	public static final AdventureResult CLOVER = new AdventureResult( "ten-leaf clover", -1 );
	public static final AdventureResult GUM = new AdventureResult( "chewing gum on a string", -1 );

	private boolean isLuckySewer;
	private KoLRequest request;

	/**
	 * Constructs a new <code>SewerRequest</code>.  This method will
	 * also determine what kind of adventure to request, based on
	 * whether or not the character is currently lucky, and whether
	 * or not the desired location is the lucky sewer adventure.
	 *
	 * @param	client	The client associated with this <code>KoLRequest</code>
	 * @param	isLuckySewer	Whether or not the user intends to go the lucky sewer
	 */

	public SewerRequest( KoLmafia client, boolean isLuckySewer )
	{
		super( client, "sewer.php" );
		this.isLuckySewer = isLuckySewer;
		request = null;
	}

	/**
	 * Set up this request before a series of run() calls
	 */
	public void startRun()
	{
		request = null;
	}

	/**
	 * Runs the <code>SewerRequest</code>.  This method determines
	 * whether or not the lucky sewer adventure will be used and
	 * attempts to run the appropriate adventure.  Note that the
	 * display will be updated in the event of failure.
	 */

	public void run()
	{
		isErrorState = false;

		if ( isLuckySewer )
			runLuckySewer();
		else
			runUnluckySewer();
	}

	/**
	 * Utility method which runs the lucky sewer adventure.
	 */

	private void runLuckySewer()
	{
		if ( !client.isLuckyCharacter() )
		{
			isErrorState = true;
			updateDisplay( ERROR_STATE, "Ran out of ten-leaf clovers." );
			client.cancelRequest();
			return;
		}

		if ( !KoLCharacter.getInventory().contains( GUM ) )
		{
			isErrorState = true;
			updateDisplay( ERROR_STATE, "Ran out of chewing gum." );
			client.cancelRequest();
			return;
		}

		if ( request == null )
		{
			// First time here.

                        // The Sewage Gnomes insist on giving precisely three
			// items, so if you have fewer than three items, report
			// an error.

			String [] items = getProperty( "luckySewer" ).split( "," );

			if ( items.length != 3 )
			{
				isErrorState = true;
				updateDisplay( ERROR_STATE, "You must select three items to get from the Sewage Gnomes." );
				client.cancelRequest();
				return;
			}

			// Enter the sewer for the first time. For whatever
			// reason, you need to view this page before you can
			// start submitting data.

			super.run();

			if ( isErrorState )
				return;

			// Make a request to use from now on.

			request = new KoLRequest( client, "sewer.php", false );
			request.addFormField( "doodit", "1" );

			for ( int i = 0; i < 3; i++)
			{
				// Values are now item IDs. Indices 1-12 in the
				// options correspond correctly to item IDs,
				// but index 13 corresponds to item ID 43.

				if ( items[i].equals( "13" ) )
					items[i] = "43";

				request.addFormField( "i" + items[i], "on" );
			}
		}

		// Enter the sewer

		request.run();

		if ( request.isErrorState )
			return;

		client.processResult( CLOVER );
		client.processResult( GUM );
	}

	/**
	 * Utility method which runs the normal sewer adventure.
	 */

	private void runUnluckySewer()
	{
		if ( client.isLuckyCharacter() )
		{
			isErrorState = true;
			updateDisplay( ERROR_STATE, "You have a ten-leaf clover." );
			client.cancelRequest();
			return;
		}

		// The unlucky sewer adventure consumes one piece of gum per
		// invocation.

		if ( !KoLCharacter.getInventory().contains( GUM ) )
		{
			isErrorState = true;
			updateDisplay( ERROR_STATE, "Ran out of chewing gum." );
			client.cancelRequest();
			return;
		}

		super.run();

		if ( isErrorState )
			return;

		// You may have randomly received a clover from some other
		// source - detect this occurence and notify the user

		if ( responseText.indexOf( "Sewage Gnomes" ) != -1 )
		{
			isErrorState = true;
			updateDisplay( ERROR_STATE, "You have an unaccounted for ten-leaf clover." );
			client.cancelRequest();
			return;
		}

		// Consume the gum
		client.processResult( GUM );
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
	{	return isErrorState ? 0 : 1;
	}
}
