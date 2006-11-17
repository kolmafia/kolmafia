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
import java.util.List;

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * donating to the Hall of the Legends of the Times of Old.
 */

public class HeroDonationRequest extends KoLRequest
{
	public static final int BORIS = 1;
	public static final int JARLSBERG = 2;
	public static final int PETE = 3;

	private static final AdventureResult [] STATUE_KEYS =
	{
		null,
		new AdventureResult( "Boris's key", 0 ),
		new AdventureResult( "Jarlsberg's key", 0 ),
		new AdventureResult( "Sneaky Pete's key", 0 )
	};

	private int amount;
	public String statue;
	private boolean hasStatueKey;

	/**
	 * Constructs a new <code>HeroDonationRequest</code>.
	 *
	 * @param	heroId	The identifier for the hero to whom you are donating
	 * @param	amount	The amount you're donating to the given hero
	 */

	public HeroDonationRequest( int heroId, int amount )
	{
		super( "shrines.php" );

		addFormField( "pwd" );
		addFormField( "action", heroId == BORIS ? "boris" : heroId == JARLSBERG ? "jarlsberg" : "sneakypete" );
		addFormField( "howmuch", String.valueOf( amount ) );

		this.amount = amount;
		this.statue = heroId == BORIS ? "boris" : heroId == JARLSBERG ? "jarlsberg" : "pete";
		this.hasStatueKey = inventory.contains( STATUE_KEYS[ heroId ] );
	}

	/**
	 * Runs the request.  Note that this does not report an error if it fails;
	 * it merely parses the results to see if any gains were made.
	 */

	public void run()
	{
		if ( !this.hasStatueKey )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You don't have the appropriate key." );
			return;
		}

		KoLmafia.updateDisplay( "Donating " + amount + " to the shrine..." );
		super.run();

	}

	protected void processResults()
	{
		if ( responseText.indexOf( "You gain" ) == -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, responseText.indexOf( "That's not enough" ) == -1 ?
				"Donation limit exceeded." : "Donation must be larger." );
			return;
		}

		StaticEntity.getClient().processResult( new AdventureResult( AdventureResult.MEAT, 0 - amount ) );
		KoLmafia.updateDisplay( "Donation complete." );
	}

	public String getCommandForm()
	{	return "donate " + amount + " " + statue;
	}
}
