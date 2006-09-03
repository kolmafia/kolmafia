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

	private boolean hasCost = true;
	private boolean isLuckySewer;

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
	}

	/**
	 * Runs the <code>SewerRequest</code>.  This method determines
	 * whether or not the lucky sewer adventure will be used and
	 * attempts to run the appropriate adventure.  Note that the
	 * display will be updated in the event of failure.
	 */

	public void run()
	{
		AdventureDatabase.retrieveItem( GUM.getNegation() );
		if ( !KoLmafia.permitsContinue() )
			return;

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
			KoLmafia.updateDisplay( ERROR_STATE, "Ran out of ten-leaf clovers." );
			return;
		}

		if ( !(client.getCurrentRequest() instanceof SewerRequest) )
		{
			// First time here.

			// The Sewage Gnomes insist on giving precisely three
			// items, so if you have fewer than three items, report
			// an error.

			int thirdItem = TradeableItemDatabase.getItemID( StaticEntity.getProperty( "luckySewerAdventure" ) );

			if ( thirdItem < 1 || (thirdItem > 12 && thirdItem != 43) )
			{
				KoLmafia.updateDisplay( ERROR_STATE, "You must select a third item from the gnomes." );
				return;
			}

			// Enter the sewer for the first time. For whatever
			// reason, you need to view this page before you can
			// start submitting data.

			hasCost = false;
			super.run();

			if ( responseCode != 200 )
				return;

			// Make a request to use from now on.

			hasCost = true;
			clearDataFields();
			addFormField( "doodit", "1" );

			// Rather than giving people flexibility, it seems like
			// a better idea to assume everyone wants trinkets and
			// spices and let them specify the third item.

			addFormField( "i43", "on" );
			addFormField( "i8", "on" );
			addFormField( "i" + thirdItem, "on" );
		}

		// Enter the sewer

		super.run();
	}

	/**
	 * Utility method which runs the normal sewer adventure.
	 */

	private void runUnluckySewer()
	{
		if ( client.isLuckyCharacter() )
			DEFAULT_SHELL.executeLine( "use * ten-leaf clover" );

		super.run();

		// You may have randomly received a clover from some other
		// source - detect this occurence and notify the user

		if ( responseText.indexOf( "Sewage Gnomes" ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You have an unaccounted for ten-leaf clover." );
			return;
		}
	}

	protected void processResults()
	{
		super.processResults();

		if ( hasCost )
		{
			if ( client.isLuckyCharacter() )
				client.processResult( CLOVER );

			client.processResult( GUM );
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
	{	return hasCost && responseCode == 200 ? 1 : 0;
	}
}
