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
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION ) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.AutoMallRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;

import net.sourceforge.kolmafia.session.StoreManager;
import net.sourceforge.kolmafia.session.StoreManager.SoldItem;

import net.sourceforge.kolmafia.utilities.AdventureResultArray;
import net.sourceforge.kolmafia.utilities.IntegerArray;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ShopCommand
	extends AbstractCommand
{
	public ShopCommand()
	{
		this.usage = " put <item> [[@] <price> [[limit] <num>]] [, <another>] | take [all|<num>] <item> [, <another>] - sell or remove from Mall..";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		parameters = parameters.toLowerCase();

		if ( !parameters.startsWith( "take " ) && !parameters.startsWith( "put " ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid shop command." );
			return;
		}

		if ( parameters.startsWith( "put " ) )
		{
			ShopCommand.put( parameters.substring( 3 ).trim() );
		}
		else
		{
			ShopCommand.take( parameters.substring( 4 ).trim() );
		}
	}

	public static void put( String parameters )
	{
		String[] itemNames = parameters.split( "\\s*,\\s*" );

		AdventureResultArray items = new AdventureResultArray();
		IntegerArray prices = new IntegerArray();
		IntegerArray limits = new IntegerArray();

		AdventureResult item;
		int price;
		int limit;

		int separatorIndex;
		String description;

		for ( int i = 0; i < itemNames.length; ++i )
		{
			price = 0;
			limit = 0;

			separatorIndex = itemNames[ i ].indexOf( '@' );

			if ( separatorIndex != -1 )
			{
				description = itemNames[ i ].substring( separatorIndex + 1 ).trim();
				itemNames[ i ] = itemNames[ i ].substring( 0, separatorIndex );

				separatorIndex = description.indexOf( "limit" );

				if ( separatorIndex != -1 )
				{
					limit = StringUtilities.parseInt( description.substring( separatorIndex + 5 ).trim() );
					description = description.substring( 0, separatorIndex ).trim();
				}

				price = StringUtilities.parseInt( description );
			}

			// workaround issue: user types in something like "mallsell pail @ 20,000,000
			if ( StringUtilities.isNumeric( itemNames[ i ] ) )
			{
				RequestLogger.printLine( "'" + itemNames[ i ] + "' is not an item.  Did you use a comma in the middle of a number?  Quitting..." );
				return;
			}

			item = ItemFinder.getFirstMatchingItem( itemNames[ i ], true );

			if ( item == null )
			{
				RequestLogger.printLine( "Skipping '" + itemNames[ i ] + "'." );
				continue;
			}

			int inventoryCount = item.getCount( KoLConstants.inventory );

			if ( item.getCount() > inventoryCount )
			{
				item = item.getInstance( inventoryCount );
			}

			if ( item.getCount() == 0 )
			{
				RequestLogger.printLine( "Skipping '" + itemNames[ i ] + "', none found in inventory." );
				continue;
			}

			items.add( item );
			prices.add( price );
			limits.add( limit );
		}

		if ( items.size() > 0 )
		{
			RequestThread.postRequest( new AutoMallRequest( items.toArray(),
									(int[])prices.toArray(),
									(int[])limits.toArray() ) );
		}
	}

	public static void take( String parameters )
	{
		boolean takeAll = false;
		int qty = 1;

		parameters = parameters.trim();

		int space = parameters.indexOf( " " );
		if ( space != -1 )
		{
			String token = parameters.substring( 0, space );
			if ( token.equals( "all" ) )
			{
				takeAll = true;
				parameters = parameters.substring( space + 1 ).trim();
			}
			else if ( StringUtilities.isNumeric( token ) )
			{
				qty = StringUtilities.parseInt( token );
				parameters = parameters.substring( space + 1 ).trim();
			}
		}

		if ( !StoreManager.soldItemsRetrieved )
		{
			RequestThread.postRequest( new ManageStoreRequest() );
		}

		List list = StoreManager.getSoldItemList();

		String[] itemNames = parameters.split( "\\s*,\\s*" );

		for ( int i = 0; i < itemNames.length; ++i )
		{
			AdventureResult item = ItemFinder.getFirstMatchingItem( itemNames[ i ], true );

			if ( item == null )
			{
				RequestLogger.printLine( "Skipping '" + itemNames[ i ] + "'." );
				continue;
			}

			int itemId = item.getItemId();
			
			SoldItem it = new SoldItem( itemId, 0, 0, 0, 0 );
			int index = list.indexOf( it );

			if ( index < 0 )
			{
				RequestLogger.printLine( itemNames[ i ] + " not found in shop." );
				continue;
			}

			SoldItem soldItem = (SoldItem) list.get( index );

			int count = takeAll ? soldItem.getQuantity() : qty;
			RequestThread.postRequest( new ManageStoreRequest( itemId, count ) );
		}
	}
}
