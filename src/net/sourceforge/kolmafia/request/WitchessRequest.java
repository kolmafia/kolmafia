/**
 * Copyright (c) 2005-2016, KoLmafia development team
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

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;

import net.sourceforge.kolmafia.preferences.Preferences;

public class WitchessRequest
	extends GenericRequest
{
	public WitchessRequest()
	{
		super( "choice.php" );
		this.addFormField( "whichchoice", "1183" );
		this.addFormField( "option", "2" );
	}

	@Override
	public void run()
	{
		if ( Preferences.getBoolean( "_witchessBuff" ) )
		{
			KoLmafia.updateDisplay( "You already got your Witchess buff today." );
			return;
		}
		if ( Preferences.getInteger( "puzzleChampBonus") != 20 )
		{
			KoLmafia.updateDisplay( "You cannot automatically get a Witchess buff until all puzzles are solved." );
			return;
		}
		RequestThread.postRequest( new GenericRequest( "campground.php?action=witchess" ) );
		RequestThread.postRequest( new GenericRequest( "choice.php?whichchoice=1181&option=3" ) );
		super.run();
	}

	@Override
	public void processResults()
	{
		WitchessRequest.parseResponse( this.getURLString(), this.responseText );
	}

	public static final void parseResponse( final String urlString, final String responseText )
	{
		if ( !urlString.startsWith( "witchess.php" ) )
		{
			return;
		}

		if ( responseText.contains( "Puzzle Champ" ) )
		{
			Preferences.setBoolean( "_witchessBuff", true );
		}
	}
}
