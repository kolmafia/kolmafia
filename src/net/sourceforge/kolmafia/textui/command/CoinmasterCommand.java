/**
 * Copyright (c) 2005-2014, KoLmafia development team
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

import java.util.List;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.CoinmasterRegistry;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.CoinMasterRequest;

import net.sourceforge.kolmafia.persistence.ItemFinder;

public class CoinmasterCommand
	extends AbstractCommand
{
	public CoinmasterCommand()
	{
		this.usage = " (buy|sell) <nickname> <item>... - buy or sell items to specified coinmaster.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		boolean isBuy;
		if ( parameters.startsWith( "buy" ) )
		{
			parameters = parameters.substring( 3 ).trim();
			isBuy = true;
		}
		else if ( parameters.startsWith( "sell" ) )
		{
			parameters = parameters.substring( 4 ).trim();
			isBuy = false;
		}
		else
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid coinmaster command." );
			return;
		}

		// Identify the coinmaster

		int spaceIndex = parameters.indexOf( " " );
		if ( spaceIndex == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Invalid coinmaster command." );
			return;
		}

		String nickname = parameters.substring( 0, spaceIndex );
		parameters = parameters.substring( spaceIndex + 1).trim();

		CoinmasterData data = CoinmasterRegistry.findCoinmasterByNickname( nickname );
		if ( data == null )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Which coinmaster is " + nickname + "?" );
			return;
		}

		List source = isBuy ? null : KoLConstants.inventory;
		AdventureResult[] itemList = ItemFinder.getMatchingItemList( parameters, source );

		if ( itemList.length == 0 )
		{
			return;
		}

		String action;

		if ( isBuy )
		{
			action = data.getBuyAction();
			if ( action == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't buy from " + data.getMaster() );
				return;
			}

			for ( int i = 0; i < itemList.length; ++i )
			{
				AdventureResult item = itemList[ i ];
				String itemName = item.getName();
				if ( !data.canBuyItem( itemName ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You can't buy " + itemName + " from " + data.getMaster() );
					return;
				}
			}

			String reason = data.canBuy();
			if ( reason != null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, reason );
				return;
			}
		}
		else
		{
			action = data.getSellAction();
			if ( action == null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, "You can't sell to " + data.getMaster() );
				return;
			}

			for ( int i = 0; i < itemList.length; ++i )
			{
				AdventureResult item = itemList[ i ];
				String itemName = item.getName();
				if ( !data.canSellItem( itemName ) )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "You can't sell " + itemName + " to " + data.getMaster() );
					return;
				}
			}

			String reason = data.canSell();
			if ( reason != null )
			{
				KoLmafia.updateDisplay( MafiaState.ERROR, reason );
				return;
			}
		}
			
		CoinMasterRequest request = data.getRequest( action, itemList );

		RequestThread.postRequest( request );
	}
}
