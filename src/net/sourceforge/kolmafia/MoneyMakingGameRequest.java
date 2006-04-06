/**
 * Copyright (c) 2006, KoLmafia development team
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

/**
 * An extension of the generic <code>KoLRequest</code> class which handles
 * placing bets at the Money Making Game
 */

public class MoneyMakingGameRequest extends KoLRequest
{
	private int meat;

	public MoneyMakingGameRequest( KoLmafia client, int meat, boolean fromStorage )
	{
		super( client, "bet.php" );
		
		this.meat = meat;

		addFormField( "action", "makebet" );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "from", fromStorage ? "1" : "0" );
		addFormField( "howmuch", String.valueOf( meat ) );
	}

	public void run()
	{
		
		if (meat < 1000 || meat > 100000000) {
			client.updateDisplay( ABORT_STATE, "Bet must between 1,000 and 100,000,000 meat." );
			return;
		}

		client.updateDisplay( PENDING_STATE, "Placing Bet..." );
		super.run();
		client.updateDisplay( CONTINUE_STATE, "Bet Placed." );
	}

	protected void processResults()
	{
	}
}
