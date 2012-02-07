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

package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.Tr4pz0rRequest;

import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class LootTrapperMenuItem
	extends ThreadedMenuItem
{
	public LootTrapperMenuItem()
	{
		super( "Visit the Trapper", new LootTrapperListener() );
	}

	private static class LootTrapperListener
		extends ThreadedListener
	{
		protected void execute()
		{
			AdventureResult selectedValue =
				(AdventureResult) InputFieldUtilities.input( "I want skins!", Tr4pz0rRequest.buyItems );

			if ( selectedValue == null )
			{
				return;
			}

			int selected = selectedValue.getItemId();
			int maximumValue = Tr4pz0rRequest.YETI_FUR.getCount( KoLConstants.inventory );
			String message = "(You have " + maximumValue + " furs available)";

			Integer value = InputFieldUtilities.getQuantity( "How many " + selectedValue.getName() + " to get?\n" + message, maximumValue, maximumValue );
			int tradeCount = ( value == null ) ? 0 : value.intValue();

			if ( tradeCount == 0 )
			{
				return;
			}

			KoLmafia.updateDisplay( "Visiting the trapper..." );
			RequestThread.postRequest( new Tr4pz0rRequest( selected, tradeCount ) );
		}
	}
}
