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

/**
 * An extension of the generic <code>KoLRequest</code> class which handles
 * adventures involving trading with the hermit.
 */

public class HermitRequest extends KoLRequest
{
	/**
	 * Constructs a new <code>HermitRequest</code>.  Note that in order
	 * for the hermit request to successfully run after creation, there
	 * must be <code>KoLSettings</code> specifying the trade that takes
	 * place.
	 *
	 * @param	client	The client to which this request will report errors/results
	 */

	public HermitRequest( KoLmafia client, int quantity )
	{
		super( client, "hermit.php" );

		addFormField( "action", "trade" );
		addFormField( "quantity", "" + quantity );
		addFormField( "pwd", client.getPasswordHash() );
	}

	/**
	 * Executes the <code>HermitRequest</code>.  This will trade the item
	 * specified in the character's <code>KoLSettings</code> for their
	 * worthless trinket; if the character has no worthless trinkets, this
	 * method will report an error to the client.
	 */

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

		// If an error state occurred, return from this
		// request, since there's no content to parse

		if ( isErrorState || responseCode != 200 )
			return;

		if ( replyContent.indexOf( "acquire" ) == -1 )
		{
			// Figure out how many you were REALLY supposed to run,
			// since you clearly didn't have enough trinkets for
			// what you did run. ;)

			int index = replyContent.indexOf( "You have" );
			if ( index == -1 )
			{
				frame.updateDisplay( KoLFrame.ENABLED_STATE, "Ran out of worthless junk." );
				client.updateAdventure( false, false );
				return;
			}

			try
			{
				int actualQuantity = df.parse( replyContent.substring( index + 9 ) ).intValue();
				(new HermitRequest( client, actualQuantity )).run();
			}
			catch ( Exception e )
			{
				// Should not happen.  Theoretically, some weird
				// error message should be logged, but why add an
				// extra line of code?  A row of comment lines is
				// much more interesting.
			}
		}

		processResults( replyContent );
		client.updateAdventure( true, true );
	}
}