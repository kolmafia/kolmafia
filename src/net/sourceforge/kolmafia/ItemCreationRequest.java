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

/**
 * An extension of <code>KoLRequest</code> designed to handle all the
 * item creation requests.  At the current time, it is only made to
 * handle items which use meat paste and are tradeable in-game.
 */

public class ItemCreationRequest extends KoLRequest
{
	public static final int MEAT_PASTE = 25;
	public static final int MEAT_STACK = 88;
	public static final int DENSE_STACK = 258;

	public static final int COMBINE = 1;
	public static final int COOK = 2;
	public static final int MIX = 3;
	public static final int SMITH = 4;

	private int itemID, quantity, creationType;

	/**
	 * Constructs a new <code>ItemCreationRequest</code> where you create
	 * the given number of items.
	 *
	 * @param	client	The client to be notified of the item creation
	 * @param	itemID	The identifier for the item to be created
	 */

	public ItemCreationRequest( KoLmafia client, int itemID, int creationType, int quantity )
	{
		super( client, "" );
		this.itemID = itemID;
		this.creationType = creationType;
		this.quantity = quantity;
	}

	/**
	 * Runs the item creation request.  Note that if another item needs
	 * to be created for the request to succeed, this method will fail.
	 */

	public void run()
	{
		switch ( itemID )
		{

			// Requests for meat paste are handled separately; the
			// full request is broken into increments of 1000, 100
			// and 10 and then submitted to the server.

			case MEAT_PASTE:
			{
				while ( quantity > 1000 )
				{
					(new MeatPasteRequest( client, 1000 )).run();
					quantity -= 1000;
				}
				while ( quantity > 100 )
				{
					(new MeatPasteRequest( client, 100 )).run();
					quantity -= 100;
				}
				while ( quantity > 10 )
				{
					(new MeatPasteRequest( client, 10 )).run();
					quantity -= 10;
				}
				for ( int i = 0; i < quantity; ++i )
					(new MeatPasteRequest( client, 1 )).run();
				break;
			}

			// Requests for meat stacks are handled separately; the
			// full request must be done one at a time (there is no
			// way to make more than 1 meat stack at a time in KoL).

			case MEAT_STACK:
			case DENSE_STACK:
			{
				boolean isDense = (itemID == DENSE_STACK);
				for ( int i = 0; i < quantity; ++i )
					(new MeatStackRequest( client, isDense )).run();
				break;
			}

			// In order to make indentation cleaner, an internal class
			// a secondary method is called to handle standard item
			// creation requests.  Note that smithing is not currently
			// handled because I don't have a SC and have no idea
			// which page is requested for smithing.

			default:

				switch ( creationType )
				{
					case COMBINE:
						combineItems();
						break;

					case COOK:
					case MIX:
					case SMITH:
						break;
				}

				break;
		}
	}

	/**
	 * Helper routine which actually does the item combination.
	 */

	private void combineItems()
	{
	}

	/**
	 * An internal class made to create meat paste.  This class
	 * takes only values of 10, 100, or 1000; it is the job of
	 * other classes to break up the request to create as much
	 * meat paste as is desired.
	 */

	private class MeatPasteRequest extends KoLRequest
	{
		public MeatPasteRequest( KoLmafia client, int quantity )
		{
			super( client, "inventory.php" );
			addFormField( "which", "3" );
			addFormField( "action", ((quantity == 1) ? "meat" : "" + quantity) + "paste" );
		}
	}

	/**
	 * An internal class made to create meat stacks and dense
	 * meat stacks.  Note that this only creates one meat stack
	 * of the type desired; it is the job of other classes to
	 * break up the request to create as many meat stacks as is
	 * actually desired.
	 */

	private class MeatStackRequest extends KoLRequest
	{
		public MeatStackRequest( KoLmafia client, boolean isDense )
		{
			super( client, "inventory.php" );
			addFormField( "which", "3" );
			addFormField( "action", isDense ? "densestack" : "meatstack" );
		}
	}
}