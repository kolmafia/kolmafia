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
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.persistence.ItemFinder;

import net.sourceforge.kolmafia.request.HiddenCityRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HiddenCityCommand
	extends AbstractCommand
{
	public HiddenCityCommand()
	{
		this.usage = " <square> [temple | altar <item>] - set Hidden City square [and perform an action there].";
	}

	public void run( final String cmd, final String parameters )
	{
		String[] split = parameters.split( " ", 3 );

		int square = StringUtilities.parseInt( split[ 0 ] );

		if ( split.length == 1 )
		{
			return;
		}

		HiddenCityRequest request1 = null;
		HiddenCityRequest request2 = null;

		String type = split[ 1 ];

		if ( type.equalsIgnoreCase( "temple" ) )
		{
			request1 = new HiddenCityRequest( square );
			request2 = new HiddenCityRequest( true );
		}
		else if ( type.equalsIgnoreCase( "altar" ) && split.length == 3 )
		{
			AdventureResult result = ItemFinder.getFirstMatchingItem( split[ 2 ], ItemFinder.ANY_MATCH );
			if ( result == null )
			{
				return;
			}
			request1 = new HiddenCityRequest( square );
			request2 = new HiddenCityRequest( true, result.getItemId() );
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "Unknown Hidden City request: " + parameters );
			return;
		}

		RequestThread.postRequest( request1 );
		RequestThread.postRequest( request2 );
	}
}
