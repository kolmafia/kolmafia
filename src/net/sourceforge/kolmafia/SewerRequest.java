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
	private boolean isLuckySewer;
	private static final AdventureResult CLOVER = new AdventureResult( "ten-leaf clover", -1 );
	private static final AdventureResult GUM = new AdventureResult( "chewing gum on a string", -1 );

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
		super( client, isLuckySewer ? "luckysewer.php" : "adventure.php" );
		this.isLuckySewer = isLuckySewer;

		if ( !this.isLuckySewer )
			addFormField( "adv", "12" );
	}

	/**
	 * Runs the <code>SewerRequest</code>.  This method determines
	 * whether or not the lucky sewer adventure will be used and
	 * attempts to run the appropriate adventure.  Note that the
	 * display will be updated in the event of failure.
	 */

	public void run()
	{
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
			updateDisplay( ENABLED_STATE, "Ran out of ten-leaf clovers." );
			client.cancelRequest();
			return;
		}

		String items = client.getSettings().getProperty( "luckySewer" );

		if ( items == null )
		{
			updateDisplay( ENABLED_STATE, "No lucky sewer settings found." );
			client.cancelRequest();
			return;
		}

		StringTokenizer parsedItems = new StringTokenizer( items, "," );

		addFormField( "action", "take" );
		addFormField( "i" + parsedItems.nextToken(), "on" );
		addFormField( "i" + parsedItems.nextToken(), "on" );
		addFormField( "i" + parsedItems.nextToken(), "on" );

		super.run();

		// Update if you're redirected to a page the client does not
		// yet recognize.

		if ( isErrorState )
			return;

		if ( responseCode == 302 && !redirectLocation.equals( "fight.php" ) )
		{
			updateDisplay( ENABLED_STATE, "Redirected to unknown page: " + redirectLocation );
			client.cancelRequest();
			return;
		}

		processResults( replyContent );
		client.addToResultTally( new AdventureResult( AdventureResult.ADV, -1 ) );
		client.addToResultTally( CLOVER );
	}

	/**
	 * Utility method which runs the normal sewer adventure.
	 */

	private void runUnluckySewer()
	{
		if ( client.isLuckyCharacter() )
		{
			updateDisplay( ENABLED_STATE, "You have a ten-leaf clover." );
			client.cancelRequest();
			return;
		}

		if ( !client.getInventory().contains( GUM ) )
		{
			updateDisplay( ENABLED_STATE, "Ran out of chewing gum." );
			client.cancelRequest();
			return;
		}

		super.run();

		if ( isErrorState )
			return;

		// You may have randomly received a clover from some other
		// source - detect this occurence and notify the user

		if ( responseCode == 302 && redirectLocation.equals( "luckysewer.php" ) )
		{
			updateDisplay( ENABLED_STATE, "You have an unaccounted for ten-leaf clover." );
			client.cancelRequest();
			return;
		}

		// Update if you're redirected to a page the client does not
		// yet recognize.

		if ( responseCode == 302 && !redirectLocation.equals( "fight.php" ) )
		{
			updateDisplay( ENABLED_STATE, "Redirected to unknown page: " + redirectLocation );
			client.cancelRequest();
			return;
		}

		processResults( replyContent );
		client.addToResultTally( new AdventureResult( AdventureResult.ADV, -1 ) );
		client.addToResultTally( GUM );
	}
}