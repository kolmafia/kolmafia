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

public class ItemCreationRequest extends KoLRequest
{
	public static final int MEAT_PASTE = 1;
	public static final int MEAT_STACK = 2;
	public static final int DENSE_STACK = 3;

	private int item, quantity;

	public ItemCreationRequest( KoLmafia client, int item, int quantity )
	{
		super( client, "" );
		this.item = item;
		this.quantity = quantity;
	}

	public void run()
	{
		switch ( item )
		{
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

			case MEAT_STACK:
			case DENSE_STACK:
			{
				boolean isDense = (item == DENSE_STACK);
				for ( int i = 0; i < quantity; ++i )
					(new MeatStackRequest( client, isDense )).run();
				break;
			}

			default:
				break;
		}
	}

	private class MeatPasteRequest extends KoLRequest
	{
		public MeatPasteRequest( KoLmafia client, int quantity )
		{
			super( client, "inventory.php" );
			addFormField( "which", "3" );

			addFormField( "action", ((quantity == 1) ? "meat" : "" + quantity) + "paste" );
		}
	}

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