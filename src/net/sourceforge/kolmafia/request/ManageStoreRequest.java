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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;

import net.sourceforge.kolmafia.persistence.ItemDatabase;

import net.sourceforge.kolmafia.session.ResultProcessor;
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
		super( isStoreLog ? "storelog.php" : "manageprices.php" );
		this.requestType = isStoreLog ? ManageStoreRequest.VIEW_STORE_LOG : ManageStoreRequest.PRICE_MANAGEMENT;
	}

	public ManageStoreRequest( final int itemId )
	{
		this( itemId, true );
	}

	public ManageStoreRequest( final int itemId, final boolean takeAll )
	{
		super( "managestore.php" );
		this.addFormField( "action", takeAll ? "takeall" : "take" );
		this.addFormField( "whichitem", String.valueOf( itemId ) );

		this.requestType = ManageStoreRequest.ITEM_REMOVAL;
		this.takenItemId = itemId;

		if ( takeAll )
		{
			AdventureResult item = new AdventureResult( itemId, 1 );
			if ( KoLConstants.profitableList.contains( item ) )
			{
				KoLConstants.profitableList.remove( item );
			}
		}
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

		Matcher takenItemMatcher =
			Pattern.compile( "<option value=\"" + this.takenItemId + "\".*?>.*?\\(([\\d,]+)\\)</option>" ).matcher(
				this.responseText );
		if ( takenItemMatcher.find() )
		{
			ResultProcessor.processResult(
				takenItem.getInstance( StringUtilities.parseInt( takenItemMatcher.group( 1 ) ) - takenItem.getCount( KoLConstants.inventory ) ) );
		}

		StoreManager.update( this.responseText, false );
		KoLmafia.updateDisplay( takenItem.getName() + " removed from your store." );
	}

	@Override
	public void processResults()
	{
	}
}
