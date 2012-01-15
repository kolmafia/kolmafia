/**
 * Copyright (c) 2005-2012, KoLmafia development team
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

import java.util.Arrays;

import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;

import net.sourceforge.kolmafia.session.StoreManager;

public class AutoMallRequest
	extends TransferItemRequest
{
	private final int[] prices;
	private final int[] limits;

	public AutoMallRequest( final AdventureResult item, final int price, final int limit )
	{
		this( new AdventureResult[] { item },
		      new int[] { price },
		      new int[] { limit } );
	}

	public AutoMallRequest( final Object[] items )
	{
		this( items, new int[ 0 ], new int[ 0 ] );
	}

	public AutoMallRequest( final Object[] items, final int[] prices, final int[] limits )
	{
		super( "managestore.php", items );

		this.prices = new int[ prices.length ];
		this.limits = new int[ limits.length ];

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

	public void attachItem( final AdventureResult item, final int index )
	{
		this.addFormField( "item" + index, String.valueOf( item.getItemId() ) );
		this.addFormField( this.getQuantityField() + index, String.valueOf( item.getCount() ) );

		int pos = Arrays.asList( this.attachments ).indexOf( item ) & 0xFFFF;

		this.addFormField(
				"price" + index,
				pos >= this.prices.length || this.prices[ pos ] == 0 ? "" : String.valueOf( this.prices[ pos ] ) );
		this.addFormField(
				"limit" + index,
				pos >= this.limits.length || this.limits[ pos ] == 0 ? "" : String.valueOf( this.limits[ pos ] ) );
	}

	public int getCapacity()
	{
		return 11;
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
					prices[ i ] = this.prices[ j ];
					limits[ i ] = this.limits[ j ];
				}
			}
		}

		return new AutoMallRequest( attachments, prices, limits );
	}

	public void processResults()
	{
		super.processResults();

		// We placed stuff in the mall.
		if ( this.responseText.indexOf( "You don't have a store." ) != -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "You don't have a store." );
			return;
		}

		KoLmafia.updateDisplay( "Items offered up for sale." );
	}

	public boolean parseTransfer()
	{
		return AutoMallRequest.parseTransfer( this.getURLString(), this.responseText );
    }

	public static final boolean parseTransfer( final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "action=additem" ) == -1 )
		{
			return false;
		}

		if ( responseText.indexOf( "You don't have a store." ) != -1 )
		{
			return false;
		}

		TransferItemRequest.transferItems( urlString,
			TransferItemRequest.ITEMID_PATTERN,
			TransferItemRequest.QTY_PATTERN,
			KoLConstants.inventory, null, 1 );

		StoreManager.update( responseText, false );
		return true;
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
		return false;
	}

	public boolean allowUndisplayableTransfer()
	{
		return false;
	}

	public boolean allowUngiftableTransfer()
	{
		return false;
	}

	public String getStatusMessage()
	{
		return "Transferring items to store";
	}

	public static final boolean registerRequest( final String urlString )
	{
		Pattern itemPattern = null;
		Pattern quantityPattern = null;

		int quantity = 1;

		if ( urlString.startsWith( "managestore.php" ) && urlString.indexOf( "action=additem" ) != -1 )
		{
			itemPattern = TransferItemRequest.ITEMID_PATTERN;
			quantityPattern = TransferItemRequest.QTY_PATTERN;
		}
		else
		{
			return false;
		}

		return TransferItemRequest.registerRequest(
			"mallsell", urlString, itemPattern, quantityPattern, KoLConstants.inventory, quantity );
	}
}
