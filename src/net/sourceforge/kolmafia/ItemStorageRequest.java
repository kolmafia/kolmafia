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
import java.util.ArrayList;
import java.util.StringTokenizer;

public class ItemStorageRequest extends KoLRequest
{
	private int moveType;
	private Object [] items;
	private List source, destination;

	public static final int MOVE_TO_CLOSET = 1;
	public static final int MOVE_TO_INVENTORY = 2;
	public static final int MOVE_TO_STASH = 3;

	/**
	 * Constructs a new <code>ItemStorageRequest</code>.
	 * @param	client	The client to be notified of the results
	 * @param	moveType	The identifier for the kind of action taking place
	 * @param	items	The list of items involved in the request
	 */

	public ItemStorageRequest( KoLmafia client, int moveType, Object [] items )
	{
		super( client, moveType == MOVE_TO_STASH ? "clan_stash.php" : "closet.php" );

		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "action",
			moveType == MOVE_TO_CLOSET ? "put" :
			moveType == MOVE_TO_INVENTORY ? "take" :
			moveType == MOVE_TO_STASH ? "addgoodies" :
				"" );

		this.items = items;
		this.moveType = moveType;

		switch ( moveType )
		{
			case MOVE_TO_CLOSET:
				source = client.getInventory();
				destination = client.getCloset();
				break;

			case MOVE_TO_INVENTORY:
				source = client.getCloset();
				destination = client.getInventory();
				break;

			case MOVE_TO_STASH:
				source = client.getInventory();
				destination = new ArrayList();
				break;
		}
	}

	/**
	 * Executes the <code>ItemStorageRequest</code>.
	 */

	public void run()
	{
		switch ( moveType )
		{
			case MOVE_TO_CLOSET:
			case MOVE_TO_INVENTORY:
				closet();
				break;

			case MOVE_TO_STASH:
				stash();
				break;
		}
	}

	private void closet()
	{
		// First, check to see how many items are to be
		// placed in the closet - if there's too many,
		// then you'll need to break up the request

		if ( items == null || items.length == 0 )
			return;

		if ( items.length > 11 )
		{
			int currentBaseIndex = 0;
			int remainingItems = items.length;

			Object [] itemHolder = null;

			while ( remainingItems > 0 )
			{
				itemHolder = new Object[ remainingItems < 11 ? remainingItems : 11 ];

				for ( int i = 0; i < itemHolder.length; ++i )
					itemHolder[i] = items[ currentBaseIndex + i ];

				// For each broken-up request, you create a new ItemStorage request
				// which will create the appropriate data to post.

				(new ItemStorageRequest( client, moveType, itemHolder )).run();

				currentBaseIndex += 11;
				remainingItems -= 11;
			}

			// Since all the sub-requests were run, there's nothing left
			// to do - simply return from this method.

			return;
		}

		for ( int i = 0; i < items.length; ++i )
		{
			addFormField( "whichitem" + (i+1), "" + TradeableItemDatabase.getItemID( ((AdventureResult)items[i]).getResultName() ) );
			addFormField( "howmany" + (i+1), "" );
		}

		// Once all the form fields are broken up, this
		// just calls the normal run method from KoLRequest
		// to execute the request.

		super.run();

		// With that done, the items need to be formally
		// removed from the appropriate list and then
		// replaced into the opposing list.

		for ( int i = 0; i < items.length; ++i )
		{
			source.remove( items[i] );
			AdventureResult.addResultToList( destination, (AdventureResult) items[i] );
		}
	}

	private void stash()
	{
	}
}