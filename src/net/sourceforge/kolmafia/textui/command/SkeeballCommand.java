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

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.ArcadeRequest;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SkeeballCommand
	extends AbstractCommand
{
	public SkeeballCommand()
	{
		this.usage = "[<count>] - squander Game Grid tokens at the broken Skeeball machine";
	}

	public void run( final String cmd, String parameters )
	{
		int tokens = ArcadeRequest.TOKEN.getCount( KoLConstants.inventory );
		int count;

		parameters = parameters.trim();
		if ( parameters.equals( "" ) )
		{
			count = 1;
		}
		else if ( parameters.equals( "*" ) )
		{
			count = tokens;
		}
		else if ( StringUtilities.isNumeric( parameters ) )
		{
			count = Math.min( StringUtilities.parseInt( parameters ), tokens );
		}
		else
		{
			KoLmafia.updateDisplay( KoLConstants.ERROR_STATE, "\"" + parameters + "\" doesn't look like a number." );
			return;
		}

		for ( int i = 0; i < count; ++i )
		{
			RequestThread.postRequest( new ArcadeRequest( "skeeball" ) );
		}
	}
}
