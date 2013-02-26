/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import java.util.ArrayList;

import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.session.DisplayCaseManager;

public class DisplayCaseRequest
	extends TransferItemRequest
{
	public static final Pattern HOWMANY_PATTERN = Pattern.compile( "howmany\\d*=(\\d+)?" );
	private boolean isDeposit;
	private boolean isWithdrawal;

	public DisplayCaseRequest()
	{
		super( "managecollectionshelves.php" );

		this.isDeposit = false;
		this.isWithdrawal = false;
	}

	public DisplayCaseRequest( final Object[] attachments, boolean isDeposit )
	{
		super( "managecollection.php", attachments );
		this.addFormField( "action", isDeposit ? "put" : "take" );

		this.isDeposit = isDeposit;
		this.isWithdrawal = !isDeposit;

		this.source = isDeposit ? KoLConstants.inventory : KoLConstants.collection;
		this.destination = isDeposit ? KoLConstants.collection : KoLConstants.inventory;
	}

	public DisplayCaseRequest( final AdventureResult[] items, final int[] shelves )
	{
		this();
		this.addFormField( "action", "arrange" );

		for ( int i = 0; i < items.length; ++i )
		{
			this.addFormField( "whichshelf" + items[ i ].getItemId(), String.valueOf( shelves[ i ] ) );
		}

		this.isDeposit = false;
		this.isWithdrawal = false;
	}

	public DisplayCaseRequest( final AdventureResult[] items, final int shelf )
	{
		this();
		this.addFormField( "action", "arrange" );

		String shelfString = String.valueOf( shelf );
		for ( int i = 0; i < items.length; ++i )
		{
			this.addFormField( "whichshelf" + items[ i ].getItemId(), shelfString );
		}

		this.isDeposit = false;
		this.isWithdrawal = false;
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return !this.isDeposit && !this.isWithdrawal;
	}

	@Override
	public int getCapacity()
	{
		return 11;
	}

	@Override
	public TransferItemRequest getSubInstance( final Object[] attachments )
	{
		return new DisplayCaseRequest( attachments, this.isDeposit );
	}

	public String getSuccessMessage()
	{
		return "";
	}

	@Override
	public String getItemField()
	{
		return "whichitem";
	}

	@Override
	public String getQuantityField()
	{
		return "howmany";
	}

	@Override
	public String getMeatField()
	{
		return "";
	}

	@Override
	public boolean parseTransfer()
	{
		return DisplayCaseRequest.parseTransfer( this.getURLString(), this.responseText );
	}

	public static final boolean parseTransfer( final String urlString, final String responseText )
	{
		if ( urlString.startsWith( "managecollection.php" ) )
		{
			return DisplayCaseRequest.parseDisplayTransfer( urlString, responseText );
		}

		if ( urlString.startsWith( "managecollectionshelves.php" ) )
		{
			return DisplayCaseRequest.parseDisplayArrangement( urlString, responseText );
		}

		return false;
	}

	public static final Pattern ITEM_PATTERN1 = Pattern.compile( "<b>(([^<]*((?!</b>)<))*[^<]*)</b> moved from inventory to case" );
	public static final Pattern ITEM_PATTERN2 = Pattern.compile( "<b>(([^<]*((?!</b>)<))*[^<]*)</b> moved from case to inventory" );

	public static final boolean parseDisplayTransfer( final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "put" ) != -1 )
		{
			// You haven't got any of that item in your inventory.
			// <b>club necklace (5)</b> moved from inventory to case.
			if ( responseText.indexOf( "moved from inventory to case" ) == -1 )
			{
				return false;
			}

			TransferItemRequest.transferItems( responseText,
				DisplayCaseRequest.ITEM_PATTERN1,
				KoLConstants.inventory,
				KoLConstants.collection );

			return true;
		}

		if ( urlString.indexOf( "take" ) != -1 )
		{
			// You haven't got any of that item in your case.
			// <b>club necklace (5)</b> moved from case to inventory.
			if ( responseText.indexOf( "moved from case to inventory" ) == -1 )
			{
				return false;
			}

			ArrayList itemList = TransferItemRequest.getItemList( responseText,
						ITEM_PATTERN2, TransferItemRequest.ITEM_PATTERN1, (Pattern) null );

			if ( itemList.isEmpty() )
			{
				return false;
			}

			TransferItemRequest.transferItems( itemList,
				KoLConstants.collection,
				KoLConstants.inventory );

			for ( int i = 0; i < itemList.size(); ++i )
			{
				AdventureResult item = ( (AdventureResult) itemList.get( i ) );
				KoLmafia.updateDisplay( "You acquire " + item );
			}

			return true;
		}

		return false;
	}

	public static final boolean parseDisplayArrangement( final String urlString, final String responseText )
	{
		if ( urlString.indexOf( "action=arrange" ) == -1 )
		{
			DisplayCaseManager.update( responseText );
		}

		return true;
	}

	@Override
	public boolean allowMementoTransfer()
	{
		return true;
	}

	@Override
	public boolean allowUntradeableTransfer()
	{
		return true;
	}

	@Override
	public boolean allowUngiftableTransfer()
	{
		return true;
	}

	@Override
	public String getStatusMessage()
	{
		return this.isDeposit ? "Placing items in display case" : this.isWithdrawal ? "Removing items from display case" : "Updating display case";
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "managecollection.php" ) )
		{
			return false;
		}

		if ( urlString.indexOf( "action=take" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"remove from display case", urlString,
				TransferItemRequest.ITEMID_PATTERN,
				TransferItemRequest.HOWMANY_PATTERN,
                                KoLConstants.collection, 0 );
		}

		if ( urlString.indexOf( "action=put" ) != -1 )
		{
			return TransferItemRequest.registerRequest(
				"put in display case", urlString,
				TransferItemRequest.ITEMID_PATTERN,
				TransferItemRequest.HOWMANY_PATTERN,
                                KoLConstants.inventory, 0 );
		}

		return true;
	}
}
