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

public class FamiliarRequest extends KoLRequest
{
	private int familiarID;
	private boolean isChangingFamiliar;

	public FamiliarRequest( KoLmafia client )
	{
		super( client, "familiar.php", false );
		this.familiarID = 0;
		this.isChangingFamiliar = false;
	}

	public FamiliarRequest( KoLmafia client, String familiarName )
	{
		super( client, "familiar.php" );
		addFormField( "action", "newfam" );

		this.familiarID = FamiliarsDatabase.getFamiliarID( familiarName );
		addFormField( "newfam", "" + familiarID );
		this.isChangingFamiliar = true;
	}

	public void run()
	{
		super.run();

		// There's nothing to parse if an error was encountered,
		// which could be either an error state or a redirection

		if ( isErrorState || responseCode != 200 )
			return;

		// If there was a change, then make sure that the character
		// has an updated familiar on their display.  Note that all
		// the other familiar data (such as familiar weight) needs
		// to be retrieved as well.  This can actually be done on
		// the reply page.

		if ( isChangingFamiliar )
		{
			KoLCharacter characterData = client.getCharacterData();
			characterData.setFamiliarDescription( FamiliarsDatabase.getFamiliarName( familiarID ), -1 );
		}

		// Otherwise, determine which familiars are present on the
		// frame (since there was no change) and add them.

		else
		{
			KoLCharacter characterData = client.getCharacterData();
			for ( int i = 1; i < 30; ++i )
				if ( replyContent.indexOf( "which=" + i ) != -1 )
					characterData.addFamiliar( i );
		}
	}
}