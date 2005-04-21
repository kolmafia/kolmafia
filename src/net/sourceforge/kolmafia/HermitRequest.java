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
import java.util.StringTokenizer;

/**
 * An extension of the generic <code>KoLRequest</code> class which handles
 * adventures involving trading with the hermit.
 */

public class HermitRequest extends KoLRequest
{
	private static final AdventureResult TRINKET = new AdventureResult( 43, 0 );
	private static final AdventureResult GEWGAW = new AdventureResult( 44, 0 );
	private static final AdventureResult KNICK_KNACK =  new AdventureResult( 45, 0 );

	private int itemID, quantity;

	/**
	 * Constructs a new <code>HermitRequest</code>.  Note that in order
	 * for the hermit request to successfully run after creation, there
	 * must be <code>KoLSettings</code> specifying the trade that takes
	 * place.
	 *
	 * @param	client	The client to which this request will report errors/results
	 */

	public HermitRequest( KoLmafia client, int itemID, int quantity )
	{
		super( client, "hermit.php" );

		this.itemID = itemID;
		this.quantity = quantity;

		addFormField( "action", "trade" );
		addFormField( "quantity", "" + quantity );
		addFormField( "whichitem", "" + itemID );
		addFormField( "pwd", client.getPasswordHash() );
	}

	/**
	 * Executes the <code>HermitRequest</code>.  This will trade the item
	 * specified in the character's <code>KoLSettings</code> for their
	 * worthless trinket; if the character has no worthless trinkets, this
	 * method will report an error to the client.
	 */

	public void run()
	{
		if ( !client.permitsContinue() )
			return;

		updateDisplay( DISABLED_STATE, "Robbing the hermit..." );
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		if ( replyContent.indexOf( "acquire" ) == -1 )
		{
			// Figure out how many you were REALLY supposed to run,
			// since you clearly didn't have enough trinkets for
			// what you did run. ;)

			int index = replyContent.indexOf( "You have" );
			if ( index == -1 )
			{
				updateDisplay( ERROR_STATE, "Ran out of worthless junk." );
				client.cancelRequest();
				return;
			}

			try
			{
				int actualQuantity = df.parse( replyContent.substring( index + 9 ) ).intValue();

				if ( quantity == actualQuantity )
				{
					updateDisplay( ERROR_STATE, "Today is not a clover day." );
					return;
				}

				(new HermitRequest( client, itemID, actualQuantity )).run();
				return;
			}
			catch ( Exception e )
			{
				// Should not happen.  Theoretically, some weird
				// error message should be logged, but why add an
				// extra line of code?  A row of comment lines is
				// much more interesting.
			}
		}

		processResults( replyContent );

		List inventory = client.getInventory();

		// Subtract the worthless items in order of their priority;
		// as far as we know, the priority is the item ID.

		quantity -= subtractWorthlessItems( TRINKET, inventory, quantity );
		quantity -= subtractWorthlessItems( GEWGAW, inventory, quantity );
		subtractWorthlessItems( KNICK_KNACK, inventory, quantity );

		updateDisplay( ENABLED_STATE, "Hermit successfully looted!" );
	}

	private int subtractWorthlessItems( AdventureResult item, List inventory, int total )
	{
		int index = inventory.indexOf( item );
		if ( index == -1 )
			return 0;

		int count = 0 - Math.min( total, ((AdventureResult)inventory.get( index )).getCount() );
		client.processResult( new AdventureResult( item.getItemID(), count ) );
		return count;
	}
}