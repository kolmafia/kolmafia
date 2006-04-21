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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StoreManageRequest extends KoLRequest
{
	private static final int ITEM_REMOVAL = 1;
	private static final int PRICE_MANAGEMENT = 2;
	private static final int VIEW_STORE_LOG = 3;

	private int takenItemID;
	private int requestType;

	public StoreManageRequest( KoLmafia client )
	{
		super( client, "manageprices.php" );
		this.requestType = PRICE_MANAGEMENT;
	}

	public StoreManageRequest( KoLmafia client, boolean isStoreLog )
	{
		super( client, isStoreLog ? "storelog.php" : "manageprices.php" );
		this.requestType = VIEW_STORE_LOG;
	}

	public StoreManageRequest( KoLmafia client, int itemID )
	{
		super( client, "managestore.php" );
		addFormField( "action", "takeall" );
		addFormField( "whichitem", String.valueOf( itemID ) );

		this.requestType = ITEM_REMOVAL;
		this.takenItemID = itemID;
	}

	public StoreManageRequest( KoLmafia client, int [] itemID, int [] prices, int [] limits )
	{
		super( client, "manageprices.php" );
		addFormField( "action", "update" );
		addFormField( "pwd" );

		this.requestType = PRICE_MANAGEMENT;
		for ( int i = 0; i < itemID.length; ++i )
		{
			addFormField( "price" + itemID[i], String.valueOf( Math.max( prices[i], Math.max( TradeableItemDatabase.getPriceByID( itemID[i] ), 100 ) ) ) );
			addFormField( "limit" + itemID[i], String.valueOf( limits[i] ) );
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
		DEFAULT_SHELL.updateDisplay( "Examining store logs..." );
		super.run();

		StoreManager.parseLog( responseText );
		DEFAULT_SHELL.updateDisplay( "Store purchase logs retrieved." );
	}

	private void managePrices()
	{
		DEFAULT_SHELL.updateDisplay( "Requesting store inventory..." );
		super.run();

		StoreManager.update( responseText, true );
		DEFAULT_SHELL.updateDisplay( "Store inventory request complete." );
	}

	private void removeItem()
	{
		DEFAULT_SHELL.updateDisplay( "Removing " + TradeableItemDatabase.getItemName( this.takenItemID ) + " from store..." );
		AdventureResult takenItem = new AdventureResult( takenItemID, 0 );

		super.run();

		Matcher takenItemMatcher = Pattern.compile( "<option value=\"" + takenItemID + "\">.*?\\(([\\d,]+)\\)</option>" ).matcher( responseText );
		if ( takenItemMatcher.find() )
			client.processResult( takenItem.getInstance( Integer.parseInt( takenItemMatcher.group(1) ) - takenItem.getCount( KoLCharacter.getInventory() ) ) );

		StoreManager.update( responseText, false );
		DEFAULT_SHELL.updateDisplay( takenItem.getName() + " removed from your store." );
	}

	protected void processResults()
	{
	}
}
