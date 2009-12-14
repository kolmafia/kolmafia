/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.request.SellStuffRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MallSellCommand
	extends AbstractCommand
{
	public MallSellCommand()
	{
		this.usage = " <item> [[@] <price> [[limit] <num>]] [, <another>]... - sell in Mall.";
	}

	public void run( final String cmd, final String parameters )
	{
		String[] itemNames = parameters.split( "\\s*,\\s*" );

		AdventureResult[] items = new AdventureResult[ itemNames.length ];
		int[] prices = new int[ itemNames.length ];
		int[] limits = new int[ itemNames.length ];

		int separatorIndex;
		String description;

		for ( int i = 0; i < itemNames.length; ++i )
		{
			separatorIndex = itemNames[ i ].indexOf( "@" );

			if ( separatorIndex != -1 )
			{
				description = itemNames[ i ].substring( separatorIndex + 1 ).trim();
				itemNames[ i ] = itemNames[ i ].substring( 0, separatorIndex );

				separatorIndex = description.indexOf( "limit" );

				if ( separatorIndex != -1 )
				{
					limits[ i ] = StringUtilities.parseInt( description.substring( separatorIndex + 5 ).trim() );
					description = description.substring( 0, separatorIndex ).trim();
				}

				prices[ i ] = StringUtilities.parseInt( description );
			}

			items[ i ] = ItemFinder.getFirstMatchingItem( itemNames[ i ], false );

			if ( items[ i ] == null )
			{
				int spaceIndex = itemNames[ i ].lastIndexOf( " " );
				if ( spaceIndex == -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + itemNames[ i ] + "] has no matches." );
					return;
				}

				prices[ i ] = StringUtilities.parseInt( itemNames[ i ].substring( spaceIndex + 1 ) );
				itemNames[ i ] = itemNames[ i ].substring( 0, spaceIndex ).trim();
				items[ i ] = ItemFinder.getFirstMatchingItem( itemNames[ i ], false );
			}

			if ( items[ i ] == null )
			{
				int spaceIndex = itemNames[ i ].lastIndexOf( " " );
				if ( spaceIndex == -1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + itemNames[ i ] + "] has no matches." );
					return;
				}

				limits[ i ] = prices[ i ];
				prices[ i ] = StringUtilities.parseInt( itemNames[ i ].substring( spaceIndex + 1 ) );
				itemNames[ i ] = itemNames[ i ].substring( 0, spaceIndex ).trim();

				items[ i ] = ItemFinder.getFirstMatchingItem( itemNames[ i ], false );
			}

			if ( items[ i ] == null )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "[" + itemNames[ i ] + "] has no matches." );
				return;
			}

			int inventoryCount = items[ i ].getCount( KoLConstants.inventory );

			if ( items[ i ].getCount() > inventoryCount )
			{
				items[ i ] = items[ i ].getInstance( inventoryCount );
			}
		}

		RequestThread.postRequest( new SellStuffRequest( items, prices, limits, SellStuffRequest.AUTOMALL ) );
	}
}