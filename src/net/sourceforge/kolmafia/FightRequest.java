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

/**
 * An extension of <code>KoLRequest</code> which handles fights
 * and battles.  A new instance is created and started for every
 * round of battle.
 */

public class FightRequest extends KoLRequest
{
	private int roundCount;

	/**
	 * Constructs a new <code>FightRequest</code>.  The client provided will
	 * be used to determine whether or not the fight should be started and/or
	 * continued, and the user settings will be used to determine the kind
	 * of action to be taken during the battle.
	 */

	public FightRequest( KoLmafia client )
	{	this( client, 0 );
	}

	private FightRequest( KoLmafia client, int roundCount )
	{
		super( client, "fight.php" );
		this.roundCount = roundCount;


		// Now, to test if the user should run away from the
		// battle - this is an HP test.

		String hpAutoFleeSettings = client.getSettings().getProperty( "hpAutoFlee" );
		int fleeTolerance = hpAutoFleeSettings == null ? 0 :
			(int)( Double.parseDouble( hpAutoFleeSettings ) * (double) client.getCharacterData().getMaximumHP() );

		if ( fleeTolerance == 0 )
			fleeTolerance = -1;

		// For now, there will not be any attempts to handle
		// special skills - the user will simply attack, use
		// a moxious maneuver, or run away.

		addFormField( "action",
			client.getCharacterData().getCurrentHP() <= fleeTolerance ? "runaway" :
				client.getCharacterData().getCurrentMP() == 0 ? "attack" :
					client.getSettings().getProperty( "battleAction" ) );
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
		if ( !client.permitsContinue() )
			updateDisplay( DISABLED_STATE, "Completing battle, round " + roundCount + "..." );

		super.run();

		// If there were no problems, then begin fighting the battle,
		// checking for termination conditions

		if ( !isErrorState )
		{
			if ( client.getSettings().getProperty( "battleAction" ).equals( "moxman" ) )
				client.processResult( new AdventureResult( AdventureResult.MP, -1 ) );

			processResults( replyContent );
			int winmsgIndex = replyContent.indexOf( "WINWINWIN" );

			if ( winmsgIndex != -1 )
			{
				// The battle was won!  Therefore, update the display to
				// reflect a victory and notify the client that an adventure
				// was completed.

				if ( !client.permitsContinue() )
					updateDisplay( ENABLED_STATE, "Battle completed, adventures aborted." );
			}
			else if ( roundCount > 30 )
			{
				updateDisplay( ENABLED_STATE, "Battle exceeded 30 rounds." );
				client.cancelRequest();
			}
			else if ( replyContent.indexOf( "You lose." ) != -1 )
			{
				// If you lose the battle, you should update the display to
				// indicate that the battle has been finished; you should
				// also notify the client that an adventure was completed,
				// but that the loop should be halted.

				updateDisplay( ENABLED_STATE, "You were defeated!" );
				client.cancelRequest();
			}
			else if ( replyContent.indexOf( "You run away," ) != -1 )
			{
				// If you successfully run away, then you should update
				// the display to indicate that you ran away and adventuring
				// should terminate.

				updateDisplay( ENABLED_STATE, "Autoflee succeeded." );
				client.cancelRequest();
			}
			else
			{
				// The battle was not lost, but it was not won - therefore there
				// are still a few rounds left to handle; because reopening a
				// connection to an existing URL may cause unforeseen errors,
				// start a new thread and allow this one to die.

				++roundCount;
				this.run();
			}
		}
	}
}