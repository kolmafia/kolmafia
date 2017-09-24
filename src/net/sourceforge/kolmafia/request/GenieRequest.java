/**
 * Copyright (c) 2005-2017, KoLmafia development team
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
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EncounterManager;
import net.sourceforge.kolmafia.session.ResultProcessor;

public class GenieRequest
	extends GenericRequest
{
	private static boolean usingPocketWish = false;
	private static final Pattern WISH_PATTERN = Pattern.compile( "You have (\\d) wish" );

	// You are using a pocket wish!
	// You have 2 wishes left today.
	// You have 1 wish left today.

	public GenieRequest()
	{
		super( "choice.php" );
	}

	public static void visitChoice( final String responseText )
	{
		Matcher matcher = GenieRequest.WISH_PATTERN.matcher( responseText );
		if ( matcher.find() )
		{
			int wishesLeft = Integer.parseInt( matcher.group( 1 ) );
			Preferences.setInteger( "_genieWishesUsed", 3 - wishesLeft );
			GenieRequest.usingPocketWish = false;
		}
		else if ( responseText.contains( "You are using a pocket wish!" ) )
		{
			GenieRequest.usingPocketWish = true;
		}
	}

	public static void postChoice( final String responseText )
	{
		if ( responseText.contains( "You acquire" ) ||
		     responseText.contains( "You gain" ) ||
		     responseText.contains( ">Fight!<" ) )
		{
			// Successful wish
			if ( GenieRequest.usingPocketWish )
			{
				ResultProcessor.removeItem( ItemPool.POCKET_WISH );
			}
			else
			{
				Preferences.increment( "_genieWishesUsed" );
			}
		}

		if ( responseText.contains( ">Fight!<" ) )
		{
			EncounterManager.ignoreSpecialMonsters();
		}
	}

}
