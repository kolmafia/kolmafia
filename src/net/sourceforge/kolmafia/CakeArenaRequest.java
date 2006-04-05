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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CakeArenaRequest extends KoLRequest
{
	private boolean isCompetition;

	public CakeArenaRequest( KoLmafia client )
	{
		super( client, "arena.php" );
		this.isCompetition = false;
	}

	public CakeArenaRequest( KoLmafia client, int opponentID, int eventID )
	{
		super( client, "arena.php" );
		addFormField( "action", "go" );
		addFormField( "whichopp", String.valueOf( opponentID ) );
		addFormField( "event", String.valueOf( eventID ) );
		this.isCompetition = true;
	}

	public void run()
	{
		if ( !isCompetition )
			DEFAULT_SHELL.updateDisplay( "Retrieving opponent list..." );

		super.run();
	}

	protected void processResults()
	{
		if ( responseText.indexOf( "You can't" ) != -1 ||
		     responseText.indexOf( "You shouldn't" ) != -1 ||
		     responseText.indexOf( "You don't" ) != -1 ||
		     responseText.indexOf( "You need" ) != -1 ||
		     responseText.indexOf( "You're way too beaten" ) != -1 ||
		     responseText.indexOf( "You're too drunk" ) != -1 )
		{
			// Notify the client of failure by telling it that
			// the adventure did not take place and the client
			// should not continue with the next iteration.
			// Friendly error messages to come later.

			DEFAULT_SHELL.updateDisplay( ERROR_STATE, "Arena battles aborted!" );
			return;
		}

		if ( isCompetition )
		{
			client.processResult( new AdventureResult( AdventureResult.MEAT, -100 ) );
			super.processResults();

			// If the familiar won, increment win count

			if ( responseText.indexOf( "Congratulations!" ) != -1 )
				KoLCharacter.setArenaWins( KoLCharacter.getArenaWins() + 1 );
			return;
		}

		// Retrieve arena wins count

		// "You have won 722 times. Only 8 wins left until your next
		// prize!"

		Matcher winMatcher = Pattern.compile(
			"You have won (\\d*) time" ).matcher( responseText );

		if ( winMatcher.find() )
			KoLCharacter.setArenaWins( Integer.parseInt( winMatcher.group(1) ) );

                // Retrieve list of opponents
		int lastMatchIndex = 0;
		Matcher opponentMatcher = Pattern.compile(
			"<tr><td valign=center><input type=radio .*? name=whichopp value=(\\d+)>.*?<b>(.*?)</b> the (.*?)<br/?>(\\d*).*?</tr>" ).matcher( responseText );

		while ( opponentMatcher.find( lastMatchIndex ) )
		{
			lastMatchIndex = opponentMatcher.end() + 1;
			int id = Integer.parseInt( opponentMatcher.group(1) );
			String name = opponentMatcher.group(2);
			String race = opponentMatcher.group(3);
			int weight = Integer.parseInt( opponentMatcher.group(4) );
			CakeArenaManager.registerOpponent( id, name, race, weight );
		}

		DEFAULT_SHELL.updateDisplay( "Opponent list retrieved." );
	}

	public String toString()
	{	return "Arena Battle";
	}

	/**
	 * An alternative method to doing adventure calculation is determining
	 * how many adventures are used by the given request, and subtract
	 * them after the request is done.
	 *
	 * @return	The number of adventures used by this request.
	 */

	public int getAdventuresUsed()
	{	return isCompetition ? 1 : 0;
	}
}
