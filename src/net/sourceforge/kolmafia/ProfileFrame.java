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
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;

import net.java.dev.spellcast.utilities.JComponentUtilities;

public class ProfileFrame extends KoLFrame
{
	private String playerName;
	private ProfileRequest profile;
	private LimitedSizeChatBuffer buffer;
	private JEditorPane profileDisplay;

	public ProfileFrame( KoLmafia client, String playerName )
	{	this( client, playerName, new ProfileRequest( client, playerName ) );
	}

	public ProfileFrame( KoLmafia client, String playerName, ProfileRequest profile )
	{
		super( "KoLmafia: Profile for " + playerName, client );

		this.profile = profile;
		this.playerName = playerName;

		profileDisplay = new JEditorPane();
		profileDisplay.setEditable( false );
		profileDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );
		profileDisplay.setText( "Retrieving profile..." );

		JScrollPane scrollPane = new JScrollPane( profileDisplay, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

		JComponentUtilities.setComponentSize( scrollPane, 400, 300 );
		getContentPane().setLayout( new GridLayout( 1, 1 ) );
		getContentPane().add( scrollPane );

		(new ProfileRequestThread()).start();
	}

	private class ProfileRequestThread extends RequestThread
	{
		public void run()
		{
			profile.initialize();
			buffer = new LimitedSizeChatBuffer( "Profile for " + playerName );
			buffer.setChatDisplay( profileDisplay );

			String profileHTML = profile.responseText.replaceAll( "<td>", "<td>&nbsp;" ).replaceAll(
				"<tr><td height=1 bgcolor=black></td></tr>", "<tr><td><hr></td></tr>" ).replaceAll(
				"<tr><td colspan=2 height=1 bgcolor=black></td></tr>", "<tr><td colspan=2><hr></td></tr>" ).replaceAll(
				"<tr><td colspan=5 height=1 bgcolor=black></td></tr>", "<tr><td colspan=5><hr></td></tr>" );

			profileHTML = profileHTML.substring( 0, profileHTML.lastIndexOf( "</table><a" ) + 8 );
			buffer.append( profileHTML );
		}
	}
}