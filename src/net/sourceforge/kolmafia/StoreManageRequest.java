/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoreManageRequest extends KoLRequest
{
	private static final int ITEM_REMOVAL = 1;
	private static final int PRICE_MANAGEMENT = 2;
	private static final int VIEW_STORE_LOG = 3;

	private int takenItemId;
	private int requestType;

	public StoreManageRequest()
	{	this( false );
	}

	public StoreManageRequest( boolean isStoreLog )
	{
		super( isStoreLog ? "storelog.php" : "manageprices.php" );
		this.requestType = isStoreLog ? VIEW_STORE_LOG : PRICE_MANAGEMENT;
	}

	public StoreManageRequest( int itemId )
	{
		super( "managestore.php" );
		addFormField( "action", "takeall" );
		addFormField( "whichitem", String.valueOf( itemId ) );

		this.requestType = ITEM_REMOVAL;
		this.takenItemId = itemId;
	}

	public StoreManageRequest( int [] itemId, int [] prices, int [] limits )
	{
		super( "manageprices.php" );
		addFormField( "action", "update" );
		addFormField( "pwd" );

		this.requestType = PRICE_MANAGEMENT;
		for ( int i = 0; i < itemId.length; ++i )
		{
			addFormField( "price" + itemId[i], prices[i] == 0 ? "" :
				String.valueOf( Math.max( prices[i], Math.max( TradeableItemDatabase.getPriceById( itemId[i] ), 100 ) ) ) );
			addFormField( "limit" + itemId[i], String.valueOf( limits[i] ) );
		}
	}

	public void run()
	{
		switch ( requestType )
		{
		case ITEM_REMOVAL:
			removeItem();
			break;

		case PRICE_MANAGEMENT:
			managePrices();
			break;

		case VIEW_STORE_LOG:
			viewStoreLogs();
			break;
		}
	}

	private void viewStoreLogs()
	{
		KoLmafia.updateDisplay( "Examining store logs..." );
		super.run();

		StoreManager.parseLog( responseText );
		KoLmafia.updateDisplay( "Store purchase logs retrieved." );
	}

	private void managePrices()
	{
		KoLmafia.updateDisplay( "Requesting store inventory..." );
		super.run();

		StoreManager.update( responseText, true );
		KoLmafia.updateDisplay( "Store inventory request complete." );
	}

	private void removeItem()
	{
		KoLmafia.updateDisplay( "Removing " + TradeableItemDatabase.getItemName( this.takenItemId ) + " from store..." );
		AdventureResult takenItem = new AdventureResult( takenItemId, 0 );

		super.run();

		Matcher takenItemMatcher = Pattern.compile( "<option value=\"" + takenItemId + "\".*?>.*?\\(([\\d,]+)\\)</option>" ).matcher( responseText );
		if ( takenItemMatcher.find() )
			StaticEntity.getClient().processResult( takenItem.getInstance( StaticEntity.parseInt( takenItemMatcher.group(1) ) - takenItem.getCount( inventory ) ) );

		StoreManager.update( responseText, false );
		KoLmafia.updateDisplay( takenItem.getName() + " removed from your store." );
	}

	public void processResults()
	{
	}
}
