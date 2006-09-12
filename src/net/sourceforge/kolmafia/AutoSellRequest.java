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
		super( client, getSellPage( client, sellType ), items, 0 );
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

	private static String getSellPage( KoLmafia client, int sellType )
	{
		if ( sellType == AUTOMALL )
			return "managestore.php";

		// Get the autosell mode the first time we need it
		if ( KoLCharacter.getAutosellMode().equals( "" ) )
			(new AccountRequest( client )).run();

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
				addFormField( "price" + index, "0" );
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
		KoLmafia.updateDisplay( ( sellType == AUTOSELL ) ? "Autoselling items..." : "Placing items in the mall..." );
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
				KoLmafia.updateDisplay( ERROR_STATE, "You don't have a store." );
				return;
			}
		}
		else if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
			StaticEntity.externalUpdate( "sellstuff_ugly.php", responseText );

		// Move out of inventory. Process meat gains, if old autosell
		// interface.

		super.processResults();
		KoLmafia.updateDisplay( "Items sold." );
	}

	public static boolean processRequest( String urlString )
	{
		Matcher itemMatcher = null;
		int quantity = 1;

		if ( urlString.startsWith( "sellstuff.php" ) )
		{
			Matcher quantityMatcher = Pattern.compile( "howmany=([\\d,]+)" ).matcher( urlString );
			if ( quantityMatcher.find() )
				quantity = StaticEntity.parseInt( quantityMatcher.group(1) );

			if ( urlString.indexOf( "type=allbutone" ) != -1 )
				quantity = -1;
			else if ( urlString.indexOf( "type=all" ) != -1 )
				quantity = 0;

			itemMatcher = Pattern.compile( "whichitem%5B%5d=(\\d+)" ).matcher( urlString );
		}
		else if ( urlString.startsWith( "sellstuff_ugly.php" ) )
		{
			Matcher quantityMatcher = Pattern.compile( "howmany=([\\d,]+)" ).matcher( urlString );
			if ( quantityMatcher.find() )
				quantity = StaticEntity.parseInt( quantityMatcher.group(1) );

			if ( urlString.indexOf( "mode=1" ) != -1 )
				quantity = 0;
			else if ( urlString.indexOf( "mode=2" ) != -1 )
				quantity = -1;

			itemMatcher = Pattern.compile( "item(\\d+)" ).matcher( urlString );
		}

		if ( itemMatcher == null )
			return false;

		StringBuffer buffer = new StringBuffer();
		while ( itemMatcher.find() )
		{
			buffer.append( buffer.length() == 0 ? "autosell " : ", " );
			buffer.append( quantity == 0 ? "*" : String.valueOf( quantity ) );
			buffer.append( " " );

			AdventureResult item = new AdventureResult( StaticEntity.parseInt( itemMatcher.group(1) ), 1 );
			int inventoryAmount = item.getCount( KoLCharacter.getInventory() );

			if ( quantity < 1 )
				quantity += inventoryAmount;
			else
				quantity = Math.min( quantity, inventoryAmount );

			StaticEntity.getClient().processResult( item.getInstance( 0 - quantity ) );
			buffer.append( item.getName() );
		}

		if ( buffer.length() != 0 )
		{
			KoLmafia.getSessionStream().println();
			KoLmafia.getSessionStream().println( buffer.toString() );
		}

		return true;
	}

	protected String getSuccessMessage()
	{	return "";
	}

	protected boolean tallyItemTransfer()
	{	return false;
	}
}
