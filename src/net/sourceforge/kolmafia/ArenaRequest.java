/**
 * Copyright (c) 2005-2007, KoLmafia development team
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

package net.sourceforge.kolmafia;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArenaRequest extends KoLRequest
{
	private int option = 0;

	private static final Pattern ID_PATTERN = Pattern.compile( "action=concert.*?option=(\\d+)" );

	public ArenaRequest( int option )
	{
		super( "postwarisland.php" );

		this.addFormField( "action", "concert" );
		this.addFormField( "pwd" );
		if ( option >= 1 && option <= 3 )
		{
			this.option = option;
			this.addFormField( "option", String.valueOf( option ) );
		}
	}

	protected boolean retryOnTimeout()
	{	return true;
	}

	public void run()
	{
		if ( option == 0 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "Decide what to do at the concert." );
			return;
		}

		KoLmafia.updateDisplay( "Visiting the Mysterious Island Arena..." );
		super.run();
	}

	public void processResults()
	{
		if ( this.responseText == null || this.responseText.equals( "")	)
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can't find the Mysterious Island Arena." );
			return;
		}

		if ( this.responseText.indexOf( "You're all rocked out." ) != -1 )
		{
			KoLmafia.updateDisplay( ERROR_STATE, "You can only visit the Mysterious Island Arena once a day." );
			return;
		}

		KoLmafia.updateDisplay( "A music lover is you." );
		RequestFrame.refreshStatus();
	}

	public static final boolean registerRequest( String location )
	{
		if ( !location.startsWith( "postwarisland.php" ) )
			return false;

		Matcher matcher = ID_PATTERN.matcher( location );

		if ( !matcher.find() )
			return true;

		RequestLogger.updateSessionLog( "concert " + matcher.group(1) );
		return true;
	}
}
