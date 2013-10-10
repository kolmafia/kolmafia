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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.StoreManager;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ManageStoreRequest
	extends GenericRequest
{
	private static final int ITEM_REMOVAL = 1;
	private static final int PRICE_MANAGEMENT = 2;
	private static final int VIEW_STORE_LOG = 3;

	private int takenItemId;
	private final int requestType;

	public ManageStoreRequest()
	{
		this( false );
	}

	public ManageStoreRequest( final boolean isStoreLog )
	{
		super( isStoreLog ? "backoffice.php" : "manageprices.php" );
		if ( isStoreLog )
		{
			this.addFormField( "which", "3" );
		}

		this.requestType = isStoreLog ? ManageStoreRequest.VIEW_STORE_LOG : ManageStoreRequest.PRICE_MANAGEMENT;
	}

	public ManageStoreRequest( final int itemId, final boolean takeAll )
	{
		this( itemId, takeAll ? Integer.MAX_VALUE : 1 );
	}

	public ManageStoreRequest( final int itemId )
	{
		this( itemId, Integer.MAX_VALUE );
	}

	public ManageStoreRequest( final int itemId, int qty )
	{
		super( "backoffice.php" );
		this.addFormField( "itemid", String.valueOf( itemId ) );
		this.addFormField( "action", "removeitem" );
		qty = Math.min( qty, StoreManager.shopAmount( itemId ) );
		this.addFormField( "qty", String.valueOf( qty ) );
		this.addFormField( "ajax", "1" );

		if ( qty > 1 )
		{
			AdventureResult item = new AdventureResult( itemId, 1 );
			if ( KoLConstants.profitableList.contains( item ) )
			{
				KoLConstants.profitableList.remove( item );
			}
		}

		this.requestType = ManageStoreRequest.ITEM_REMOVAL;
		this.takenItemId = itemId;
	}

	public ManageStoreRequest( final int[] itemId, final int[] prices, final int[] limits )
	{
		super( "manageprices.php" );
		this.addFormField( "action", "update" );
		int formInt;

		this.requestType = ManageStoreRequest.PRICE_MANAGEMENT;
		for ( int i = 0; i < itemId.length; ++i )
		{
			formInt = ( ( i - 1 ) / 100 ); //Group the form fields for every 100 items.
			this.addFormField( "price" + formInt + "[" + itemId[ i ] + "]", prices[ i ] == 0 ? "" : String.valueOf( Math.max(
				prices[ i ], Math.max( ItemDatabase.getPriceById( itemId[ i ] ), 100 ) ) ) );
			this.addFormField( "limit" + formInt + "[" + itemId[ i ] + "]", String.valueOf( limits[ i ] ) );
		}
	}

	@Override
	protected boolean retryOnTimeout()
	{
		return true;
	}

	@Override
	public void run()
	{
		switch ( this.requestType )
		{
		case ITEM_REMOVAL:
			this.removeItem();
			break;

		case PRICE_MANAGEMENT:
			this.managePrices();
			break;

		case VIEW_STORE_LOG:
			this.viewStoreLogs();
			break;
		}
	}

	private void viewStoreLogs()
	{
		KoLmafia.updateDisplay( "Examining store logs..." );

		super.run();

		if ( this.responseText != null )
		{
			StoreManager.parseLog( this.responseText );
		}

		KoLmafia.updateDisplay( "Store purchase logs retrieved." );
	}

	private void managePrices()
	{
		KoLmafia.updateDisplay( "Requesting store inventory..." );

		super.run();

		if ( this.responseText != null )
		{
			StoreManager.update( this.responseText, true );
		}

		KoLmafia.updateDisplay( "Store inventory request complete." );
	}

	private void removeItem()
	{
		KoLmafia.updateDisplay( "Removing " + ItemDatabase.getItemName( this.takenItemId ) + " from store..." );
		AdventureResult takenItem = new AdventureResult( this.takenItemId, 0 );

		super.run();

		Matcher takenItemMatcher = Pattern.compile( "updateInv\\(\\{\"" + this.takenItemId + "\":(\\d*)\\}\\)" ).matcher( this.responseText );
		if ( takenItemMatcher.find() )
		{
			int taken = StringUtilities.parseInt( takenItemMatcher.group( 1 ) );
			StoreManager.removeItem( this.takenItemId, taken );
			KoLmafia.updateDisplay( taken + " " + takenItem.getName() + " removed from your store." );
		}
	}

	@Override
	public void processResults()
	{
	}
}
