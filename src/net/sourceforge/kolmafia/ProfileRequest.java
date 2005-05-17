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

public class ProfileRequest extends KoLRequest
{
	public ProfileRequest( KoLmafia client, String playerName )
	{
		super( client, "showplayer.php" );
		addFormField( "who", client.getPlayerID( playerName ) );
	}

	public void run()
	{
		updateDisplay( NOCHANGE, "Retrieving player profile..." );
		super.run();

		int secondTableIndex = replyContent.indexOf( "</table><table>" );

		// This is a massive replace which makes the profile easier to
		// parse and re-represent inside of editor panes.

		replyContent = replyContent.substring( replyContent.indexOf( "</b>" ), secondTableIndex ).replaceAll(
			"<td", " <td" ).replaceAll( "<tr", "<br><tr" ).replaceAll( "</?[ctplhi].*?>", "" ).replaceAll(
			"[ ]+", " " ).replaceAll( "(<br> )+", "<br> " ) + "<br>" +
				replyContent.substring( secondTableIndex, replyContent.lastIndexOf( "send" ) ).replaceAll(
				"<td", " <td" ).replaceAll( "<tr", "<br><tr" ).replaceAll( "</?[tplh].*?>", "" ).replaceAll(
				"[ ]+", " " ).replaceAll( "(<br> )+", "<br> " ).replaceAll( "<[cC]enter>.*?</center>", "" ).replaceAll(
				"onClick=\'.*?\'", "" ).replaceFirst( "<br> Familiar:", "" ).replaceFirst(
				"</b>,", "</b><br>" ).replaceFirst( "<b>\\(</b>.*?<b>\\)</b>", "<br>" ).replaceFirst(
				"<b>Ranking:", "<b>PVP Ranking:" ).replaceFirst( "<br>", "" );

		// This completes the retrieval of the player profile.
		// Fairly straightforward, but really ugly-looking.
		// Now, just update the display.

		updateDisplay( NOCHANGE, "Profile successfully retrieved." );
	}
}
