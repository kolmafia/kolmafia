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

package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;

public class ActionBarManager
{
	private static String initialJSONString = "";
	private static String currentJSONString = "";

	public static final void updateJSONString( RelayRequest request )
	{
		String action = request.getFormField( "action" );

		// If there's a fetch, return the JSON object.  Ideally,
		// we would be able to send 304 messages, but that's not
		// possible because 'd' is always the current time.

		if ( action != null && action.equalsIgnoreCase( "fetch" ) )
		{
			request.pseudoResponse( "HTTP/1.1 200 OK", ActionBarManager.currentJSONString );
			return;
		}

		// Otherwise, assume it's a set, and cache the JSON object
		// locally and submit it to the server on logout.

		String bar = request.getFormField( "bar" );
		if ( bar != null )
		{
			ActionBarManager.currentJSONString = bar;
		}

		RequestThread.postRequest( request );
		return;
	}

	public static final void loadJSONString()
	{
		GenericRequest request = new GenericRequest( "actionbar.php?action=fetch" );
		RequestThread.postRequest( request );

		if ( request.responseText != null )
		{
			ActionBarManager.initialJSONString = request.responseText;
			ActionBarManager.currentJSONString = request.responseText;
		}
	}
}
