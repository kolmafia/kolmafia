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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemStorageRequest extends KoLRequest
{
	private int moveType;
	private Object [] items;
	private List source, destination;

	public static final int INVENTORY_TO_CLOSET = 1;
	public static final int CLOSET_TO_INVENTORY = 2;
	public static final int INVENTORY_TO_STASH = 3;

	public static final int MEAT_TRANSACTION = 4;

	/**
	 * Constructs a new <code>ItemStorageRequest</code>.
	 * @param	client	The client to be notified of the results
	 * @param	amount	The amount of meat involved in this transaction
	 * @param	isDeposit	Whether or not this is a deposit or withdrawal
	 */

	public ItemStorageRequest( KoLmafia client, int amount, boolean isDeposit )
	{
		super( client, "closet.php" );
		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "amt", "" + amount );
		addFormField( "action", isDeposit ? "addmeat" : "takemeat" );

		this.items = null;
		this.moveType = MEAT_TRANSACTION;
	}

	/**
	 * Constructs a new <code>ItemStorageRequest</code>.
	 * @param	client	The client to be notified of the results
	 * @param	moveType	The identifier for the kind of action taking place
	 * @param	items	The list of items involved in the request
	 */

	public ItemStorageRequest( KoLmafia client, int moveType, Object [] items )
	{
		super( client, moveType == INVENTORY_TO_STASH ? "clan_stash.php" : "closet.php" );

		addFormField( "pwd", client.getPasswordHash() );
		addFormField( "action",
			moveType == INVENTORY_TO_CLOSET ? "put" :
			moveType == CLOSET_TO_INVENTORY ? "take" :
			moveType == INVENTORY_TO_STASH ? "addgoodies" : "" );

		this.items = items;
		this.moveType = moveType;

		switch ( moveType )
		{
			case INVENTORY_TO_CLOSET:
				source = client.getInventory();
				destination = client.getCloset();
				break;

			case CLOSET_TO_INVENTORY:
				source = client.getCloset();
				destination = client.getInventory();
				break;

			case INVENTORY_TO_STASH:
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
			case INVENTORY_TO_CLOSET:
			case CLOSET_TO_INVENTORY:
				closet();
				break;

			case INVENTORY_TO_STASH:
				stash();
				break;

			case MEAT_TRANSACTION:
				meat();
				break;
		}
	}

	private void meat()
	{
		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		// Now, determine how much is left in your closet
		// by locating "Your closet contains x meat" and
		// update the display with that information.

		Matcher meatInClosetMatcher = Pattern.compile( "[\\d,]+ meat\\.</b>" ).matcher( replyContent );

		if ( meatInClosetMatcher.find() )
		{
			try
			{
				String meatInCloset = meatInClosetMatcher.group();
				client.getCharacterData().setClosetMeat( df.parse( meatInCloset ).intValue() );
			}
			catch ( Exception e )
			{
			}
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
			AdventureResult result = (AdventureResult) items[i];
			int itemID = TradeableItemDatabase.getItemID( result.getName() );

			if ( itemID != -1 )
			{
				addFormField( "whichitem" + (i+1), "" + itemID );
				addFormField( "howmany" + (i+1), "" + result.getCount() );
			}
		}

		// Once all the form fields are broken up, this
		// just calls the normal run method from KoLRequest
		// to execute the request.

		super.run();

		// With that done, the items need to be formally
		// removed from the appropriate list and then
		// replaced into the opposing list.

		AdventureResult currentResult, negatedResult;
		for ( int i = 0; i < items.length; ++i )
		{
			currentResult = (AdventureResult) items[i];
			if ( currentResult.isItem() )
			{
				negatedResult = new AdventureResult( currentResult.getName(), 0 - currentResult.getCount() );

				if ( moveType == INVENTORY_TO_CLOSET )
				{
					client.addToResultTally( negatedResult );
					AdventureResult.addResultToList( destination, currentResult );
				}
				else
				{
					AdventureResult.addResultToList( source, negatedResult );
					client.addToResultTally( currentResult );
				}
			}
		}
	}

	private void stash()
	{
		// First, check to see how many items are to be
		// placed in the closet - if there's too many,
		// then you'll need to break up the request

		if ( items == null || items.length == 0 )
			return;

		if ( items.length > 1 )
		{
			Object [] itemHolder = new Object[1];
			for ( int i = 0; i < items.length; ++i )
			{
				itemHolder[0] = items[i];
				(new ItemStorageRequest( client, INVENTORY_TO_STASH, itemHolder )).run();
			}

			return;
		}

		AdventureResult result = (AdventureResult) items[0];
		int itemID = TradeableItemDatabase.getItemID( result.getName() );

		if ( itemID != -1 )
		{
			addFormField( "whichitem", "" + itemID );
			addFormField( "quantity", "" + result.getCount() );

			super.run();

			AdventureResult negatedResult = new AdventureResult( result.getName(), 0 - result.getCount() );
			client.addToResultTally( negatedResult );
		}
	}
}