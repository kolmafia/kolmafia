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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CakeArenaRequest extends KoLRequest
{
	public CakeArenaRequest( KoLmafia client )
	{	super( client, "arena.php" );
	}

	public CakeArenaRequest( KoLmafia client, int opponentID, int eventID )
	{
		super( client, "arena.php" );
		addFormField( "whichopp", "" + opponentID );
		addFormField( "event", "" + eventID );
	}

	public void run()
	{
		super.run();
		processResults( replyContent );

		int lastMatchIndex = 0;
		int [] opponentIDs = new int[4];
		String [] opponents = new String[4];

		Matcher opponentMatcher = Pattern.compile(
			"<tr><td valign=center><input type=radio  checked name=whichopp value=(\\d+)>.*?</tr>" ).matcher( replyContent );

		for ( int i = 0; i < 4; ++i )
		{
			opponentMatcher.find( lastMatchIndex );
			lastMatchIndex = opponentMatcher.end() + 1;

			client.getCakeArenaManager().registerOpponent(
				Integer.parseInt( opponentMatcher.group(1) ), opponentMatcher.group().replaceAll(
					"<br>", "," ).replaceAll( "<.*?>", "" ) );
		}
	}
}