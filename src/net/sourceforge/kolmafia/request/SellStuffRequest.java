/**
 * Copyright (c) 2005-2008, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SellStuffRequest
	extends TransferItemRequest
{
	public static final Pattern AUTOSELL_PATTERN = Pattern.compile( "for ([\\d,]+) [Mm]eat" );
	private static final Pattern EMBEDDED_ID_PATTERN = Pattern.compile( "item(\\d+)" );

	private final int sellType;

	private final int[] prices;
	private final int[] limits;

	public static final int AUTOSELL = 1;
	public static final int AUTOMALL = 2;

	public SellStuffRequest( final AdventureResult item )
	{
		this( new AdventureResult[] { item }, SellStuffRequest.AUTOSELL );
	}

	public SellStuffRequest( final AdventureResult item, final int price, final int limit )
	{
		this( new AdventureResult[] { item }, new int[] { price }, new int[] { limit }, SellStuffRequest.AUTOMALL );
	}

	public SellStuffRequest( final Object[] items, final int sellType )
	{
		this( items, new int[ 0 ], new int[ 0 ], sellType );
	}

	public SellStuffRequest( final Object[] items, final int[] prices, final int[] limits, final int sellType )
	{
		super( SellStuffRequest.getSellPage( sellType ), items );

		this.sellType = sellType;
		this.prices = new int[ prices.length ];
		this.limits = new int[ limits.length ];

		if ( sellType == SellStuffRequest.AUTOMALL )
		{
			this.addFormField( "action", "additem" );

			for ( int i = 0; i < prices.length; ++i )
			{
				this.prices[ i ] = prices[ i ];
			}

			for ( int i = 0; i < limits.length; ++i )
			{
				this.limits[ i ] = limits[ i ];
			}
		}
	}

	public String getItemField()
	{
		return "whichitem";
	}

	public String getQuantityField()
	{
		return "qty";
	}

	public String getMeatField()
	{
		return "sendmeat";
	}

	private static final String getSellPage( final int sellType )
	{
		if ( sellType == SellStuffRequest.AUTOMALL )
		{
			return "managestore.php";
		}

		// Get the autosell mode the first time we need it
		if ( KoLCharacter.getAutosellMode().equals( "" ) )
		{
			( new AccountRequest() ).run();
		}

		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
		{
			return "sellstuff_ugly.php";
		}

		return "sellstuff.php";
	}

	public void attachItem( final AdventureResult item, final int index )
	{
		if ( this.sellType == SellStuffRequest.AUTOMALL )
		{
			this.addFormField( "item" + index, String.valueOf( item.getItemId() ) );
			this.addFormField( this.getQuantityField() + index, String.valueOf( item.getCount() ) );

			this.addFormField(
				"price" + index,
				index - 1 >= this.prices.length || this.prices[ index - 1 ] == 0 ? "" : String.valueOf( this.prices[ index - 1 ] ) );
			this.addFormField(
				"limit" + index,
				index - 1 >= this.limits.length || this.limits[ index - 1 ] == 0 ? "" : String.valueOf( this.limits[ index - 1 ] ) );

			return;
		}

		// Autosell: "compact" or "detailed" mode

		this.addFormField( "action", "sell" );

		if ( KoLCharacter.getAutosellMode().equals( "detailed" ) )
		{
			if ( this.getCapacity() == 1 )
			{
				this.addFormField( "quantity", String.valueOf( item.getCount() ) );
			}

			String itemId = String.valueOf( item.getItemId() );
			this.addFormField( "item" + itemId, itemId );
		}
		else
		{
			if ( this.getCapacity() == 1 )
			{
				// If we are doing the requests one at a time,
				// specify the item quantity

				this.addFormField( "type", "quant" );
				this.addFormField( "howmany", String.valueOf( item.getCount() ) );
			}
			else
			{
				// Otherwise, we are selling all.  As of
				// 2/1/2006, must specify a quantity field even
				// for this - but the value is ignored

				this.addFormField( "type", "all" );
				this.addFormField( "howmany", "1" );
			}

			// This is a multiple selection input field.
			// Therefore, you can give it multiple items.

			this.addFormField( "whichitem[]", String.valueOf( item.getItemId() ), true );
		}
	}

	public int getCapacity()
	{
		// If you are attempting to send things to the mall,
		// the capacity is one.

		if ( this.sellType == SellStuffRequest.AUTOMALL )
		{
			return 11;
		}

		// Otherwise, if you are autoselling multiple items,
		// then it depends on which mode you are using.

		int mode = KoLCharacter.getAutosellMode().equals( "detailed" ) ? 1 : 0;

		AdventureResult currentAttachment;
		int inventoryCount, attachmentCount;

		for ( int i = 0; i < this.attachments.length; ++i )
		{
			currentAttachment = (AdventureResult) this.attachments[ i ];

			inventoryCount = currentAttachment.getCount( KoLConstants.inventory );
			if ( inventoryCount == 0 )
			{
				continue;
			}

			attachmentCount = currentAttachment.getCount();

			if ( mode == 0 )
			{
				// We are in compact mode. If we are not
				// selling everything, we must do it one item
				// at a time
				if ( attachmentCount < inventoryCount )
				{
					return 1;
				}

				// Otherwise, look at remaining items
				continue;
			}

			if ( mode == 1 )
			{
				// We are in detailed "sell all" mode.
				if ( attachmentCount >= inventoryCount )
				{
					continue;
				}

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
				this.addFormField( "mode", "3" );
				return 1;
			}

			// We are in detailed "all but one" mode. This item had
			// better also be "all but one"

			if ( attachmentCount != inventoryCount - 1 )
			{
				// Nope. Switch to "quantity" mode
				this.addFormField( "mode", "3" );
				return 1;
			}

			// We continue in "all but one" mode
		}

		// We can sell all the items with the same mode.
		if ( mode > 0 )
		{
			// Add detailed "mode" field
			this.addFormField( "mode", String.valueOf( mode ) );
		}

		return Integer.MAX_VALUE;
	}

	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		int[] prices = new int[ this.prices.length == 0 ? 0 : attachments.length ];
		int[] limits = new int[ this.prices.length == 0 ? 0 : attachments.length ];

		for ( int i = 0; i < prices.length; ++i )
		{
			for ( int j = 0; j < this.attachments.length; ++j )
			{
				if ( attachments[ i ].equals( this.attachments[ j ] ) )
				{
					prices[ i ] = this.prices[ i ];
					limits[ i ] = this.limits[ i ];
				}
			}
		}

		return new SellStuffRequest( attachments, prices, limits, this.sellType );
	}

	public void processResults()
	{
		super.processResults();

		if ( this.sellType == SellStuffRequest.AUTOMALL )
		{
			// We placed stuff in the mall.
			StoreManager.update( this.responseText, false );

			if ( this.responseText.indexOf( "You don't have a store." ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a store." );
				return;
			}

			KoLmafia.updateDisplay( "Items offered up for sale." );
			return;
		}

		SellStuffRequest.parseAutosell( this.getURLString(), this.responseText );

		// Move out of inventory. Process meat gains, if old autosell
		// interface.

		KoLmafia.updateDisplay( "Items sold." );
		KoLCharacter.updateStatus();
	}

	public static final void parseAutosell( final String location, final String responseText )
	{
		if ( !location.startsWith( "sellstuff_ugly.php" ) )
		{
			return;
		}

		// "You sell your 2 disturbing fanfics to an organ
		// grinder's monkey for 264 Meat."

		Matcher matcher = SellStuffRequest.AUTOSELL_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int amount = StringUtilities.parseInt( matcher.group( 1 ) );
			String message = "You gain " + amount + " Meat";
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MEAT, amount ) );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		Pattern itemPattern = null;
		Pattern quantityPattern = null;

		int quantity = 1;

		String sellType = null;

		if ( urlString.startsWith( "sellstuff.php" ) )
		{
			Matcher quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}

			if ( urlString.indexOf( "type=allbutone" ) != -1 )
			{
				quantity = -1;
			}
			else if ( urlString.indexOf( "type=all" ) != -1 )
			{
				quantity = 0;
			}

			itemPattern = TransferItemRequest.ITEMID_PATTERN;
			sellType = "autosell";
		}
		else if ( urlString.startsWith( "sellstuff_ugly.php" ) )
		{
			Matcher quantityMatcher = TransferItemRequest.QUANTITY_PATTERN.matcher( urlString );
			if ( quantityMatcher.find() )
			{
				quantity = StringUtilities.parseInt( quantityMatcher.group( 1 ) );
			}

			if ( urlString.indexOf( "mode=1" ) != -1 )
			{
				quantity = 0;
			}
			else if ( urlString.indexOf( "mode=2" ) != -1 )
			{
				quantity = -1;
			}

			itemPattern = SellStuffRequest.EMBEDDED_ID_PATTERN;
			sellType = "autosell";
		}
		else if ( urlString.startsWith( "managestore.php" ) && urlString.indexOf( "action=additem" ) != -1 )
		{
			itemPattern = TransferItemRequest.ITEMID_PATTERN;
			quantityPattern = TransferItemRequest.QTY_PATTERN;
			sellType = "mallsell";
		}

		if ( itemPattern == null )
		{
			return false;
		}

		return TransferItemRequest.registerRequest(
			sellType, urlString, itemPattern, quantityPattern, KoLConstants.inventory, null, null, quantity );
	}

	public String getSuccessMessage()
	{
		return "";
	}

	public boolean allowMementoTransfer()
	{
		return false;
	}

	public boolean allowSingletonTransfer()
	{
		return KoLCharacter.canInteract();
	}

	public boolean allowUntradeableTransfer()
	{
		return this.sellType == SellStuffRequest.AUTOSELL;
	}

	public boolean allowUngiftableTransfer()
	{
		return this.sellType == SellStuffRequest.AUTOSELL;
	}

	public String getStatusMessage()
	{
		return this.sellType == SellStuffRequest.AUTOMALL ? "Transferring items to store" : "Autoselling items to NPCs";
	}
}
