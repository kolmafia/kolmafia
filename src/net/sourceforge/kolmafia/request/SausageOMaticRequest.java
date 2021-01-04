/*
 * Copyright (c) 2005-2021, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.session.InventoryManager;

public class SausageOMaticRequest
	extends CreateItemRequest
{
	public SausageOMaticRequest( final Concoction conc )
	{
		super( "choice.php", conc );
	}

	@Override
	public void run()
	{
		if ( !KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0 )
		{
			return;
		}

		String creation = this.getName();

		if ( !creation.equals( "magical sausage" ) )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "Cannot create " + creation );
			return;
		}

		int quantityNeeded = this.getQuantityNeeded();
		KoLmafia.updateDisplay( "Creating " + this.getQuantityNeeded() + " " + creation + "..." );

		int meatNeeded = 0;
		int count = 0;
		int currentSausages = this.createdItem.getCount( KoLConstants.inventory );
		int sausagesMade = Preferences.getInteger( "_sausagesMade" );
		int grinderUnits = Preferences.getInteger( "sausageGrinderUnits" );

		// Work out total meat cost to make requested sausages
		while ( count < quantityNeeded && KoLmafia.permitsContinue() )
		{
			meatNeeded = meatNeeded + ( sausagesMade + 1 + count ) * 111;
			count++;
		}

		// Work out meat stacks needed to make requested sausages
		int denseStacksNeeded = (int) Math.floor( ( meatNeeded - grinderUnits ) / 1000 );
		int stacksNeeded = (int) Math.floor( ( meatNeeded - grinderUnits - denseStacksNeeded * 1000 ) / 100 );
		int pasteNeeded = (int) Math.ceil( (double) ( meatNeeded - grinderUnits - denseStacksNeeded * 1000 - stacksNeeded * 100 ) / 10 );

		KoLmafia.updateDisplay( "Meat needed: " + Math.max( meatNeeded - grinderUnits, 0 ) +
					", Dense: " + Math.max( denseStacksNeeded, 0 ) +
					", Stacks: " + Math.max( stacksNeeded, 0 ) +
					", Paste: " + Math.max( pasteNeeded, 0 ) );

		InventoryManager.retrieveItem( ItemPool.DENSE_STACK, denseStacksNeeded );
		InventoryManager.retrieveItem( ItemPool.MEAT_STACK, stacksNeeded );
		InventoryManager.retrieveItem( ItemPool.MEAT_PASTE, pasteNeeded );
		InventoryManager.retrieveItem( ItemPool.MAGICAL_SAUSAGE_CASING, quantityNeeded );

		GenericRequest request = new GenericRequest( "inventory.php?action=grind" );
		RequestThread.postRequest( request );

		if ( denseStacksNeeded > 0 )
		{
			String url = "choice.php?whichchoice=1339&option=1&qty=" + denseStacksNeeded + "&iid=" + ItemPool.DENSE_STACK;
			request.constructURLString( url );
			RequestThread.postRequest( request );
		}
		if ( stacksNeeded > 0 )
		{
			String url = "choice.php?whichchoice=1339&option=1&qty=" + stacksNeeded + "&iid=" + ItemPool.MEAT_STACK;
			request.constructURLString( url );
			RequestThread.postRequest( request );
		}
		if ( pasteNeeded > 0 )
		{
			String url = "choice.php?whichchoice=1339&option=1&qty=" + pasteNeeded + "&iid=" + ItemPool.MEAT_PASTE;
			request.constructURLString( url );
			RequestThread.postRequest( request );
		}
		while ( this.getQuantityNeeded() > 0 )
		{
			this.beforeQuantity = this.createdItem.getCount( KoLConstants.inventory );
			String url = "choice.php?whichchoice=1339&option=2";
			request.constructURLString( url );
			RequestThread.postRequest( request );
			int createdQuantity = this.createdItem.getCount( KoLConstants.inventory ) - this.beforeQuantity;
			if ( createdQuantity == 0 )
			{
				if ( KoLmafia.permitsContinue() )
				{
					KoLmafia.updateDisplay( MafiaState.ERROR, "Creation failed, no results detected." );
				}

				return;
			}
			this.quantityNeeded -= createdQuantity;
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "choice.php" ) || !urlString.contains( "whichchoice=1339" ) )
		{
			return false;
		}

		return true;
	}
}
