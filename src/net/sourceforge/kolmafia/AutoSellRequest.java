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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AutoSellRequest extends SendMessageRequest
{
	private int sellType;

	private int [] prices;
	private int [] limits;

	public static final int AUTOSELL = 1;
	public static final int AUTOMALL = 2;

	public AutoSellRequest( KoLmafia client, AdventureResult item )
	{	this( client, new AdventureResult [] { item }, AUTOSELL );
	}

	public AutoSellRequest( KoLmafia client, AdventureResult item, int price, int limit )
	{	this( client, new AdventureResult [] { item }, new int [] { price }, new int [] { limit }, AUTOMALL );
	}

	public AutoSellRequest( KoLmafia client, Object [] items, int sellType )
	{	this( client, items, new int[0], new int[0], sellType );
	}

	public AutoSellRequest( KoLmafia client, Object [] items, int [] prices, int [] limits, int sellType )
	{
		super( client, getSellPage( sellType ), items, 0 );
		addFormField( "pwd" );

		this.sellType = sellType;
		this.prices = new int[ prices.length ];
		this.limits = new int[ prices.length ];

		if ( sellType == AUTOMALL )
		{
			addFormField( "action", "additem" );

			this.quantityField = "qty";

			for ( int i = 0; i < prices.length; ++i )
			{
				this.prices[i] = prices[i];
				this.limits[i] = limits[i];
			}
		}
	}

	private static String getSellPage( int sellType )
	{
		if ( sellType == AUTOMALL )
			return "managestore.php";

		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
			return "sellstuff_ugly.php";

		return "sellstuff.php";
	}

	protected void attachItem( AdventureResult item, int index )
	{
		if ( sellType == AUTOMALL )
		{
			addFormField( "item" + index, String.valueOf( item.getItemID() ) );
			addFormField( quantityField + index, String.valueOf( item.getCount() ) );

			if ( prices.length == 0 )
			{
				addFormField( "price" + index, "999999999" );
				addFormField( "limit" + index, "0" );
			}
			else
			{
				addFormField( "price" + index, prices[ index - 1 ] == 0 ? "" : String.valueOf( prices[ index - 1 ] ) );
				addFormField( "limit" + index, limits[ index - 1 ] == 0 ? "" : String.valueOf( limits[ index - 1 ] ) );
			}

			return;
		}

		// Autosell: "compact" or "detailed" mode

		addFormField( "action", "sell" );

		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
		{
			if ( getCapacity() == 1 )
			{
				// If we are doing the requests one at a time,
				// specify the item quantity

				addFormField( "quantity", String.valueOf( item.getCount() ) );
			}

			String itemID = String.valueOf( item.getItemID() );
			addFormField( "item" + itemID, itemID );
		}
		else
		{
			if ( getCapacity() == 1 )
			{
				// If we are doing the requests one at a time,
				// specify the item quantity

				addFormField( "type", "quant" );
				addFormField( "howmany", String.valueOf( item.getCount() ) );
			}
			else
			{
				// Otherwise, we are selling all.  As of
				// 2/1/2006, must specify a quantity field even
				// for this - but the value is ignored

				addFormField( "type", "all" );
				addFormField( "howmany", "1" );
			}

			// This is a multiple selection input field.
			// Therefore, you can give it multiple items.

			addFormField( "whichitem[]", String.valueOf( item.getItemID() ), true );
		}
	}

	protected int getCapacity()
	{
		// If you are attempting to send things to the mall,
		// the capacity is one.

		if ( sellType == AUTOMALL )
			return 11;

		// Otherwise, if you are autoselling multiple items,
		// then it depends on which mode you are using.

		if ( KoLCharacter.getAutosellMode().equals( "" ) )
			(new AccountRequest( client )).run();

		int mode = KoLCharacter.getAutosellMode().equals( "detailed" ) ? 1 : 0;

		AdventureResult currentAttachment;
		int inventoryCount, attachmentCount;

		for ( int i = 0; i < attachments.length; ++i )
		{
			currentAttachment = (AdventureResult) attachments[i];

			inventoryCount = currentAttachment.getCount( KoLCharacter.getInventory() );
			attachmentCount = currentAttachment.getCount();

			if ( mode == 0 )
			{
				// We are in compact mode. If we are not
				// selling everything, we must do it one item
				// at a time
				if ( attachmentCount != inventoryCount )
					return 1;

				// Otherwise, look at remaining items
				continue;
			}

			if ( mode == 1 )
			{
				// We are in detailed "sell all" mode.
				if ( attachmentCount == inventoryCount )
					continue;

				// ...but no longer
				if ( i == 0 && attachmentCount == inventoryCount - 1 )

				{
					// First item and we're selling one
					// less than max. Switch to detailed
					// "all but one" mode
					mode = 2;
					continue;
				}

				// Switch to "quantity" mode
				addFormField( "mode", "3" );
				return 1;
			}

			// We are in detailed "all but one" mode. This item had
			// better also be "all but one"

			if ( attachmentCount != inventoryCount - 1 )
			{
				// Nope. Switch to "quantity" mode
				addFormField( "mode", "3" );
				return 1;
			}

			// We continue in "all but one" mode
		}

		// We can sell all the items with the same mode.
		if ( mode > 0 )
			// Add detailed "mode" field
			addFormField( "mode", String.valueOf( mode ) );

		return Integer.MAX_VALUE;
	}

	protected void repeat( Object [] attachments )
	{
		int [] prices = new int[ this.prices.length == 0 ? 0 : attachments.length ];
		int [] limits = new int[ this.prices.length == 0 ? 0 : attachments.length ];

		for ( int i = 0; i < prices.length; ++i )
		{
			for ( int j = 0; j < this.attachments.length; ++j )
				if ( attachments[i].equals( this.attachments[j] ) )
				{
					prices[i] = this.prices[i];
					limits[i] = this.limits[i];
				}
		}

		(new AutoSellRequest( client, attachments, limits, prices, sellType )).run();
	}

	/**
	 * Executes the <code>AutoSellRequest</code>.  This will automatically
	 * sell the item for its autosell value and update the client with
	 * the needed information.
	 */

	public void run()
	{
		DEFAULT_SHELL.updateDisplay( ( sellType == AUTOSELL ) ? "Autoselling items..." : "Placing items in the mall..." );
		super.run();
	}

	protected void processResults()
	{
		if ( sellType == AUTOMALL )
		{
			// We placed stuff in the mall.
			StoreManager.update( responseText, false );

			if ( responseText.indexOf( "You don't have a store." ) != -1 )
			{
				DEFAULT_SHELL.updateDisplay( ERROR_STATE, "You don't have a store." );
				return;
			}
		}
		else if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
		{
			// New autosell interface.

			// "You sell your 2 disturbing fanfics to an organ
			// grinder's monkey for 264 Meat."

			try
			{
				Matcher matcher = Pattern.compile( "for ([\\d,]+) [Mm]eat" ).matcher( responseText );
				if ( matcher.find() )
					client.processResult( new AdventureResult( AdventureResult.MEAT, df.parse( matcher.group(1) ).intValue() ) );
			}
			catch ( Exception e )
			{
				// This should not happen.  Therefore, print
				// a stack trace for debug purposes.
				
				StaticEntity.printStackTrace( e );
			}
		}

		// Move out of inventory. Process meat gains, if old autosell
		// interface.
		super.processResults();
		DEFAULT_SHELL.updateDisplay( "Items sold." );
	}

	protected String getSuccessMessage()
	{	return "";
	}
}
