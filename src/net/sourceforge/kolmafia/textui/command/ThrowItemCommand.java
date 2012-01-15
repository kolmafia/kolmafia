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
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.CurseRequest;

public class ThrowItemCommand
	extends AbstractCommand
{
	public ThrowItemCommand()
	{
		this.usage = "[?] <item> at <player> [ || <message> ] - use item on someone else";
	}

	public void run( String command, String parameters )
	{
		String msg = "";
		int splitPos = parameters.indexOf( "||" );
		if ( splitPos != -1 )
		{
			msg = parameters.substring( splitPos + 2 ).trim();
			parameters = parameters.substring( 0, splitPos ).trim();
			RequestLogger.printLine( "(personalized messages not supported yet)" );
		}
		splitPos = parameters.indexOf( " at " );
		if ( splitPos == -1 )
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "No <s>victim</s>recipient specified." );
			return;
		}
		String target = parameters.substring( splitPos + 4 ).trim();
		parameters = parameters.substring( 0, splitPos ).trim();
		AdventureResult item = ItemFinder.getFirstMatchingItem( parameters, ItemFinder.ANY_MATCH );
		if ( item != null )
		{
			if ( !ItemDatabase.getAttribute( item.getItemId(), ItemDatabase.ATTR_CURSE ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "The " +
					item.getName() + " is not properly balanced for throwing." );
				return;
			}
			RequestThread.postRequest( new CurseRequest( item, target, msg ) );
		}
	}
}
