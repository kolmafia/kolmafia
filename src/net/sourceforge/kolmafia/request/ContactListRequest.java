/**
 * Copyright (c) 2005-2011, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.session.ContactManager;

public class ContactListRequest
	extends GenericRequest
{
	private static final Pattern LIST_PATTERN = Pattern.compile( "<b>Contact List</b>.*?</table>" );
	private static final Pattern ENTRY_PATTERN =
		Pattern.compile( "<a href=\"showplayer.php\\?who=(\\d+)\".*?<b>(.*?)</b>" );

	public ContactListRequest()
	{
		super( "account_contactlist.php" );
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	public void processResults()
	{
		ContactListRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		ContactManager.addMailContact( KoLCharacter.getUserName(), KoLCharacter.getPlayerId() );

		Matcher listMatcher = ContactListRequest.LIST_PATTERN.matcher( responseText );

		if ( listMatcher.find() )
		{
			Matcher entryMatcher = ContactListRequest.ENTRY_PATTERN.matcher( listMatcher.group() );
			while ( entryMatcher.find() )
			{
				ContactManager.addMailContact( entryMatcher.group( 2 ), entryMatcher.group( 1 ) );
			}
		}
	}
}
