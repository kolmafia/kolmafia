/**
 * Copyright (c) 2005, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
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
import java.util.StringTokenizer;

public class HermitRequest extends KoLRequest
{
	public HermitRequest( KoLmafia client )
	{
		super( client, "hermit.php" );

		addFormField( "action", "Yep." );
		addFormField( "hermitwants", "43" );
	}

	public void run()
	{
		String item = client.getSettings().getProperty( "hermitTrade" );

		if ( item == null )
		{
			frame.updateDisplay( KoLFrame.ENABLED_STATE, "No hermit trade settings found." );
			client.updateAdventure( false, false );
			return;
		}

		addFormField( "whichitem", item );

		super.run();

		System.out.println( responseCode );
		System.out.println( replyContent );

		if ( isErrorState || responseCode != 200 )
			return;

		if ( !replyContent.contains( "acquire" ) )
		{
			frame.updateDisplay( KoLFrame.ENABLED_STATE, "Ran out of worthless trinkets." );
			client.updateAdventure( false, false );
			return;
		}

		processResults( replyContent );
		client.updateAdventure( true, true );
	}
}