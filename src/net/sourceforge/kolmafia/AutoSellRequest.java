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
	{
		this( client, items, new int[0], new int[0], sellType );
	}

	public AutoSellRequest( KoLmafia client, Object [] items, int [] prices, int [] limits, int sellType )
	{
		super( client, sellPage( sellType ), items, 0 );
		addFormField( "pwd", client.getPasswordHash() );

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

	private static String sellPage ( int sellType )
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
				addFormField( "mode", "3" );
				addFormField( "quantity", String.valueOf( item.getCount() ) );
			}
			else
			{
				// Otherwise, we are selling all.
				addFormField( "mode", "1" );
				addFormField( "howmany", "1" );
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
		// but for one of them, you're not selling the maximum
		// amount, then set the capacity to 1.

		AdventureResult currentAttachment;

		for ( int i = 0; i < attachments.length; ++i )
		{
			currentAttachment = (AdventureResult) attachments[i];
			if ( currentAttachment.getCount( KoLCharacter.getInventory() ) != currentAttachment.getCount() )
				return 1;
		}

		// Otherwise, if all items are the maximum amount,
		// then autosell everything in one request.

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
		updateDisplay( DISABLE_STATE, ( sellType == AUTOSELL ) ? "Autoselling items..." : "Placing items in the mall..." );

		super.run();

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( responseCode != 200 )
			return;

		// Otherwise, update the client with the information stating that you
		// sold all the items of the given time, and acquired a certain amount
		// of meat from the recipient.

		if ( sellType == AUTOSELL )
		{
			try
			{
				Matcher matcher = Pattern.compile( "for ([\\d,]+) [Mm]eat" ).matcher( responseText );
				if ( matcher.find() )
					client.processResult( new AdventureResult( AdventureResult.MEAT, df.parse( matcher.group(1) ).intValue() ) );
			}
			catch ( Exception e )
			{
				e.printStackTrace( KoLmafia.getLogStream() );
				e.printStackTrace();
			}
		}
		else
			StoreManager.update( responseText, false );

		updateDisplay( NORMAL_STATE, "Items sold." );
	}

	protected String getSuccessMessage()
	{	return "";
	}
}
