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

public class ConsumeItemRequest extends KoLRequest
{
	public static final int NO_CONSUME = 0;
	public static final int CONSUME_EAT = 1;
	public static final int CONSUME_DRINK = 2;
	public static final int CONSUME_USE = 3;

	private AdventureResult itemUsed;
	private RetrieveResultRequest resultRequest;

	/**
	 * Constructs a new <code>ConsumeItemRequest</code>.
	 * @param	client	The client to be notified of the logout
	 */

	public ConsumeItemRequest( KoLmafia client, int consumptionType, AdventureResult item )
	{
		super( client, consumptionType == CONSUME_EAT ? "inv_eat.php" : consumptionType == CONSUME_DRINK ? "inv_booze.php" : "inv_use.php", false );
		addFormField( "whichitem", "" + TradeableItemDatabase.getItemID( item.getName() ) );

		this.itemUsed = new AdventureResult( item.getName(), -1 );
		this.resultRequest = new RetrieveResultRequest( client, consumptionType == CONSUME_USE ? 3 : 1 );
	}

	public void run()
	{
		// Note that requests for bartenders and chefs should
		// not be run if the character already has one

		if ( (itemUsed.getName().startsWith( "chef-in" ) && client.getCharacterData().hasChef()) ||
			itemUsed.getName().startsWith( "bartender-in" ) && client.getCharacterData().hasBartender() )
		{
			client.updateAdventure( false, false );
			return;
		}

		super.run();

		// You know you're successful if the server
		// attempts to redirect you.

		if ( responseCode == 302 && redirectLocation.endsWith( "action=message" ) )
		{
			resultRequest.run();

			if ( itemUsed.getName().startsWith( "chef-in" ) )
				client.getCharacterData().setChef( true );
			else if ( itemUsed.getName().startsWith( "bartender-in" ) )
				client.getCharacterData().setBartender( true );
		}
	}

	private class RetrieveResultRequest extends KoLRequest
	{
		public RetrieveResultRequest( KoLmafia client, int formID )
		{
			super( client, "inventory.php" );
			addFormField( "which", "" + formID );
			addFormField( "action", "message" );
		}

		public void run()
		{
			super.run();

			if ( isErrorState || responseCode != 200 )
				return;

			// Check to make sure that it wasn't a food or drink
			// that was consumed that resulted in nothing.

			if ( replyContent.indexOf( "You're too" ) != -1 )
			{
				client.updateAdventure( false, false );
				client.getActiveFrame().updateDisplay( KoLFrame.ENABLED_STATE, "Consumption limit reached." );
				return;
			}

			// Parse the reply, which can be found before the
			// word "Inventory".  In theory, this could've caused
			// problems in the inventory screen, but since Jick
			// is probably smarter with error-checking after so
			// long, the output/input's probably just fine.

			client.addToResultTally( itemUsed );
			processResults( replyContent.substring( 0, replyContent.indexOf( "Inventory:" ) ) );
		}

	}
}
