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

/**
 * An extension of a <code>KoLRequest</code> which specifically handles
 * exercising in the clan gym.
 */

public class ClanGymRequest extends KoLRequest
{
	public static final int HOBOFLEX = 1;
	public static final int BIGBOOK = 2;
	public static final int TANULOTS = 3;

	/**
	 * Constructs a new <code>ClanGymRequest</code>.
	 *
	 * @param	client	The client to be notified in the event of error
	 * @param	equipmentID	The identifier for the equipment you're using
	 * @param	turnCount	The number of turns you're spending on the equipment
	 */

	public ClanGymRequest( KoLmafia client, int equipmentID, int turnCount )
	{
		super( client, "clan_gym.php" );
		addFormField( "action", equipmentID == HOBOFLEX ? "hoboflex" : equipmentID == BIGBOOK ? "bigbook" : "tanningbed" );
		addFormField( "numturns", "" + turnCount );
	}

	/**
	 * Runs the request.  Note that this does not report an error if it fails;
	 * it merely parses the results to see if any gains were made.
	 */

	public void run()
	{
		super.run();

		// All the gains will be found before the first </center> tag;
		// therefore, you can parse just that small segment.

		processResults( replyContent.substring( 0, replyContent.indexOf( "</center>" ) ) );
	}
}
