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
 * adventures in the Kingdom of Loathing sewer.
 */

public class SewerRequest extends KoLRequest
{
	private boolean isLuckySewer;

	/**
	 * Constructs a new <code>SewerRequest</code>.  This method will
	 * also determine what kind of adventure to request, based on
	 * whether or not the character is currently lucky, and whether
	 * or not the desired location is the lucky sewer adventure.
	 *
	 * @param	client	The client associated with this <code>KoLRequest</code>
	 * @param	isLuckySewer	Whether or not the user intends to go the lucky sewer
	 */

	public SewerRequest( KoLmafia client, boolean isLuckySewer )
	{
		super( client, isLuckySewer || client.isLuckyCharacter() ?
			"luckysewer.php" : "sewer.php" );

		this.isLuckySewer = isLuckySewer || client.isLuckyCharacter();
	}

	/**
	 * Runs the <code>SewerRequest</code>.  This method determines
	 * whether or not the lucky sewer adventure will be used and
	 * attempts to run the appropriate adventure.  Note that the
	 * display will be updated in the event of failure.
	 */

	public void run()
	{
		if ( isLuckySewer )
			runLuckySewer();
		else
			runUnluckySewer();
	}

	/**
	 * Utility method which runs the lucky sewer adventure.
	 */

	private void runLuckySewer()
	{
		String items = client.getSettings().getProperty( "luckySewer" );

		if ( items == null )
		{
			frame.updateDisplay( KoLFrame.ENABLED_STATE, "No lucky sewer settings found." );
			client.updateAdventure( false, false );
			return;
		}

		StringTokenizer parsedItems = new StringTokenizer( items, "," );

		addFormField( "action", "take" );
		addFormField( "i" + parsedItems.nextToken(), "on" );
		addFormField( "i" + parsedItems.nextToken(), "on" );
		addFormField( "i" + parsedItems.nextToken(), "on" );

		super.run();

		if ( isErrorState || responseCode != 200 )
			return;

		if ( replyContent.indexOf( "acquire" ) == -1 )
		{
			frame.updateDisplay( KoLFrame.ENABLED_STATE, "Ran out of ten-leaf clovers." );
			client.updateAdventure( false, false );
			return;
		}

		processResults( replyContent );
		client.updateAdventure( true, true );
	}

	/**
	 * Utility method which runs the normal sewer adventure.
	 */

	private void runUnluckySewer()
	{
		super.run();

		if ( isErrorState || responseCode != 200 )
			return;

		if ( replyContent.indexOf( "acquire" ) == -1 )
		{
			frame.updateDisplay( KoLFrame.ENABLED_STATE, "Ran out of chewing gum." );
			client.updateAdventure( false, false );
			return;
		}

		processResults( replyContent );
		client.updateAdventure( true, true );
	}
}