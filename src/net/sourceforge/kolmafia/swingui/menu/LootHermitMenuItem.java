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

package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.HermitRequest;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class LootHermitMenuItem
	extends ThreadedMenuItem
{
	public LootHermitMenuItem()
	{
		super( "Loot the Hermit", new LootHermitListener() );
	}

	private static class LootHermitListener
		extends ThreadedListener
	{
		@Override
		protected void execute()
		{
			// See how many clovers are available today. This visits the
			// Hermit, if necessary, and sets the AdventureResult in
			// KoLConstants.hermitItems.
			int cloverCount = HermitRequest.cloverCount();

			AdventureResult selectedValue =
				(AdventureResult) InputFieldUtilities.input( "I have worthless items!", KoLConstants.hermitItems );

			if ( selectedValue == null )
			{
				return;
			}

			int selected = selectedValue.getItemId();
			int maximumValue = HermitRequest.getWorthlessItemCount( true );

			String message = "(You have " + maximumValue + " worthless items retrievable)";

			if ( selected == ItemPool.TEN_LEAF_CLOVER )
			{
				if ( cloverCount <= maximumValue )
				{
					message = "(There are " + cloverCount + " clovers still available)";
					maximumValue = cloverCount;
				}
			}

			Integer value = InputFieldUtilities.getQuantity( "How many " + selectedValue.getName() + " to get?\n" + message, maximumValue, 1 );
			int tradeCount = ( value == null ) ? 0 : value.intValue();

			if ( tradeCount == 0 )
			{
				return;
			}

			RequestThread.postRequest( new HermitRequest( selected, tradeCount ) );
		}
	}
}
