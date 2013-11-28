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

import java.lang.Integer;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.objectpool.ItemPool;

import net.sourceforge.kolmafia.request.HermitRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HermitCommand
	extends AbstractCommand
{
	public HermitCommand()
	{
		this.usage = "[?] [<item>] - get clover status, or trade for item.";
	}

	@Override
	public void run( final String cmd, String parameters )
	{
		if ( !KoLmafia.permitsContinue() )
		{
			return;
		}

		int cloverCount = HermitRequest.cloverCount();

		if ( parameters.equals( "" ) )
		{
			KoLmafia.updateDisplay( "The Hermit has " + cloverCount + " clover" + ( cloverCount == 1 ? "" : "s" ) + " available today." );
			return;
		}

		int count = 1;

		if ( Character.isDigit( parameters.charAt( 0 ) ) )
		{
			int spaceIndex = parameters.indexOf( " " );
			count = StringUtilities.parseInt( parameters.substring( 0, spaceIndex ) );
			parameters = parameters.substring( spaceIndex );
		}
		else if ( parameters.charAt( 0 ) == '*' )
		{
			int spaceIndex = parameters.indexOf( " " );
			count = Integer.MAX_VALUE;
			parameters = parameters.substring( spaceIndex );
		}

		parameters = parameters.toLowerCase().trim();
		int itemId = -1;

		for ( int i = 0; i < KoLConstants.hermitItems.size(); ++i )
		{
			AdventureResult item = (AdventureResult) KoLConstants.hermitItems.get( i );
			String name = item.getName();
			if ( name.toLowerCase().contains( parameters ) )
			{
				if ( KoLmafiaCLI.isExecutingCheckOnlyCommand )
				{
					RequestLogger.printLine( name );
					return;
				}

				itemId = item.getItemId();
				break;
			}
		}

		if ( itemId == -1 )
		{
			KoLmafia.updateDisplay( MafiaState.ERROR, "You can't get a " + parameters + " from the hermit today." );
			return;
		}

		// "*" for clovers means all the hermit has available today.
		// For any other item, it means as many as you can get with 
		// the worthless items you currently have

		count =
			itemId == ItemPool.TEN_LEAF_CLOVER ?
			Math.min( count, cloverCount ) :
			Math.min( count, HermitRequest.getWorthlessItemCount() );

		if ( count > 0 )
		{
			RequestThread.postRequest( new HermitRequest( itemId, count ) );
		}
	}
}
