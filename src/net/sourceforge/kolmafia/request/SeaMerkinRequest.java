/**
 * Copyright (c) 2005-2013, KoLmafia development team
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

import net.sourceforge.kolmafia.objectpool.AdventurePool;

import net.sourceforge.kolmafia.preferences.Preferences;

import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SeaMerkinRequest
	extends GenericRequest
{
	public SeaMerkinRequest()
	{
		super( "sea_merkin.php" );
	}

	public SeaMerkinRequest( final String action )
	{
		this();
		this.addFormField( "action", action );
	}

	@Override
	public void processResults()
	{
		SeaMerkinRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "action.php" ) )
		{
			return;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action.equals( "temple" ) )
		{
			// Normally, this redirects to choice.php?forceoption=0
			// If you have already won, you will come here.

			if ( responseText.contains( "The temple is empty" ) )
			{
				Preferences.setString( "merkinQuestPath", "done" );
			}
		}
	}

	private final static String COLOSSEUM = "snarfblat=" + AdventurePool.MERKIN_COLOSSEUM_ID;
	public static final void parseColosseumResponse( final String urlString, final String responseText )
	{
		if ( !urlString.contains( SeaMerkinRequest.COLOSSEUM ) )
		{
			return;
		}

		// If we have already finished the quest, we don't care what it
		// says when you visit the Colosseum
		if ( Preferences.getString( "merkinQuestPath" ).equals( "done" ) )
		{
			return;
		}

		// The Colosseum is empty -- your crowd of Mer-kin admirers
		// (or, for all you know, your crowd of Mer-kin who totally,
		// totally hate you,) has gone home.

		if ( responseText.contains( "your crowd of Mer-kin admirers" ) )
		{
			Preferences.setString( "merkinQuestPath", "gladiator" );
			Preferences.setInteger( "lastColosseumRoundWon", 15 );
		}

		// As you approach the Colosseum, the guards in the
		// front whisper "Praise be to the High Priest!" and
		// kneel before you. Unfortunately, they kneel in a way
		// that crosses their spears in front of the Colosseum
		// entrance, and you can't get in.

		else if ( responseText.contains( "Praise be to the High Priest" ) )
		{
			Preferences.setString( "merkinQuestPath", "scholar" );
		}
	}

	public static final boolean registerRequest( final String urlString )
	{
		if ( !urlString.startsWith( "sea_merkin.php" ) )
		{
			return false;
		}

		String action = GenericRequest.getAction( urlString );
		if ( action != null && action.equals( "temple" ) )
		{
			// Defer to AdventureDatabase, since it is an adventure
			return false;
		}

		return false;
	}
}
